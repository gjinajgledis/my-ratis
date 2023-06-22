package org.example.myratis.server;

import org.example.myratis.api.server.DivisionProperties;
import org.example.myratis.api.server.RaftServerRpc;
import org.example.myratis.api.server.protocol.RaftServerProtocol;
import org.example.myratis.api.server.protocol.TermIndex;
import org.example.myratis.api.statemachine.StateMachine;
import org.example.myratis.common.config.RaftProperties;
import org.example.myratis.common.protocol.RaftGroup;
import org.example.myratis.common.protocol.RaftGroupId;
import org.example.myratis.common.protocol.RaftPeer;
import org.example.myratis.common.protocol.RaftPeerId;
import org.example.myratis.common.util.JavaUtils;
import org.example.myratis.common.util.LifeCycle;
import org.example.myratis.common.util.TimeDuration;
import org.example.myratis.grpc.server.GrpcService;
import org.example.myratis.proto.RaftProtos.RaftPeerRole;
import org.example.myratis.proto.RaftProtos.RaftRpcRequestProto;
import org.example.myratis.proto.RaftProtos.RequestVoteRequestProto;
import org.example.myratis.proto.RaftProtos.RequestVoteReplyProto;
import org.example.myratis.api.server.RaftServer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.example.myratis.common.util.LifeCycle.State.*;

public class RaftServerImpl implements RaftServer {

    private final RaftPeerId id;
    private final Supplier<RaftPeer> peerSupplier = JavaUtils.memoize(this::buildRaftPeer);
    private final RaftProperties properties;
    private final DivisionProperties divisionProperties;
    private final StateMachine stateMachine;
    private final LifeCycle lifeCycle;
    private final ServerState state;
    private  RoleInfo role;
    private  RaftServerRpc serverRpc;
    private  ExecutorService executor;
    private  ThreadGroup threadGroup;
    private final AtomicBoolean firstElectionSinceStartup = new AtomicBoolean(true);

    RaftServerImpl(RaftPeerId id, StateMachine stateMachine, RaftProperties properties, ThreadGroup threadGroup) {
        this.id = id;
        this.properties = properties;
        this.stateMachine = stateMachine;
        this.state = new ServerState(id, group, stateMachine, this, option, properties);

        this.serverRpc = new GrpcService();

        this.lifeCycle = new LifeCycle(this.id + "-" + JavaUtils.getClassSimpleName(getClass()));

        this.executor = Executors.newCachedThreadPool();
        this.threadGroup = threadGroup == null ? new ThreadGroup(this.id.toString()) : threadGroup;
    }

    private RaftServerImpl(RaftPeerId id, ){
       this.id = id;

    }
    RaftServerImpl(RaftGroup group, StateMachine stateMachine, RaftServerProxy proxy, RaftStorage.StartupOption option)
            throws IOException {
        final RaftPeerId id = proxy.getId();
        LOG.info("{}: new RaftServerImpl for {} with {}", id, group, stateMachine);
        this.lifeCycle = new LifeCycle(id);
        this.stateMachine = stateMachine;
        this.role = new RoleInfo(id);

        final RaftProperties properties = proxy.getProperties();
        this.divisionProperties = new DivisionPropertiesImpl(properties);
        leaderStepDownWaitTime = RaftServerConfigKeys.LeaderElection.leaderStepDownWaitTime(properties);
        this.sleepDeviationThreshold = RaftServerConfigKeys.sleepDeviationThreshold(properties);
        this.proxy = proxy;

        this.state = new ServerState(id, group, stateMachine, this, option, properties);
        this.retryCache = new RetryCacheImpl(properties);
        this.dataStreamMap = new DataStreamMapImpl(id);
        this.readOption = RaftServerConfigKeys.Read.option(properties);

        this.jmxAdapter = new RaftServerJmxAdapter();
        this.leaderElectionMetrics = LeaderElectionMetrics.getLeaderElectionMetrics(
                getMemberId(), state::getLastLeaderElapsedTimeMs);
        this.raftServerMetrics = RaftServerMetricsImpl.computeIfAbsentRaftServerMetrics(
                getMemberId(), () -> commitInfoCache::get, retryCache::getStatistics);

        this.startComplete = new AtomicBoolean(false);
        this.threadGroup = new ThreadGroup(proxy.getThreadGroup(), getMemberId().toString());

        this.transferLeadership = new TransferLeadership(this, properties);
        this.snapshotRequestHandler = new SnapshotManagementRequestHandler(this);
        this.snapshotInstallationHandler = new SnapshotInstallationHandler(this, properties);

        this.serverExecutor = ConcurrentUtils.newThreadPoolWithMax(
                RaftServerConfigKeys.ThreadPool.serverCached(properties),
                RaftServerConfigKeys.ThreadPool.serverSize(properties),
                id + "-server");
        this.clientExecutor = ConcurrentUtils.newThreadPoolWithMax(
                RaftServerConfigKeys.ThreadPool.clientCached(properties),
                RaftServerConfigKeys.ThreadPool.clientSize(properties),
                id + "-client");
    }

    @Override
    public RequestVoteReplyProto requestVote(RequestVoteRequestProto r) throws IOException {
        final RaftRpcRequestProto request = r.getServerRequest();
        return requestVote(r.getPreVote() ? Phase.PRE_VOTE : Phase.ELECTION,
                RaftPeerId.valueOf(request.getRequestorId()),
                ProtoUtils.toRaftGroupId(request.getRaftGroupId()),
                r.getCandidateTerm(),
                TermIndex.valueOf(r.getCandidateLastEntry()));
    }

    private RequestVoteReplyProto requestVote(Phase phase,
                                              RaftPeerId candidateId, RaftGroupId candidateGroupId,
                                              long candidateTerm, TermIndex candidateLastEntry) throws IOException {

        assertLifeCycleState(LifeCycle.States.RUNNING);
        assertGroup(candidateId, candidateGroupId);

        boolean shouldShutdown = false;
        final RequestVoteReplyProto reply;
        synchronized (this) {
            // Check life cycle state again to avoid the PAUSING/PAUSED state.
            assertLifeCycleState(LifeCycle.States.RUNNING);

            final VoteContext context = new VoteContext(this, phase, candidateId);
            final RaftPeer candidate = context.recognizeCandidate(candidateTerm);
            final boolean voteGranted = context.decideVote(candidate, candidateLastEntry);
            if (candidate != null && phase == Phase.ELECTION) {
                // change server state in the ELECTION phase
                final boolean termUpdated =
                        changeToFollower(candidateTerm, true, false, "candidate:" + candidateId);
                if (voteGranted) {
                    state.grantVote(candidate.getId());
                }
                if (termUpdated || voteGranted) {
                    state.persistMetadata(); // sync metafile
                }
            }
            if (voteGranted) {
                role.getFollowerState().ifPresent(fs -> fs.updateLastRpcTime(FollowerState.UpdateType.REQUEST_VOTE));
            } else if(shouldSendShutdown(candidateId, candidateLastEntry)) {
                shouldShutdown = true;
            }
            reply = ServerProtoUtils.toRequestVoteReplyProto(candidateId, getMemberId(),
                    voteGranted, state.getCurrentTerm(), shouldShutdown);
            if (LOG.isInfoEnabled()) {
                LOG.info("{} replies to {} vote request: {}. Peer's state: {}",
                        getMemberId(), phase, ServerStringUtils.toRequestVoteReplyString(reply), state);
            }
        }
        return reply;
    }


    @Override
    public RaftPeerId getId() {
        return id;
    }

    @Override
    public RaftPeer getPeer() {
        return peerSupplier.get();
    }

    @Override
    public Iterable<RaftGroupId> getGroupIds() {
        return null;
    }

    @Override
    public Iterable<RaftGroup> getGroups() throws IOException {
        return null;
    }

    @Override
    public RaftProperties getProperties() {
        return properties;
    }

    @Override
    public RaftServerRpc getServerRpc() {
        return serverRpc;
    }

    @Override
    public void start() throws IOException {
        if (!lifeCycle.compareAndTransition(NEW, STARTING)) {
            return ;
        }
        state.initialize(stateMachine);

        startAsPeer(RaftPeerRole.FOLLOWER);

        state.start();
    }

    /**
     * The peer belongs to the current configuration, should start as a follower or listener
     */
    private void startAsPeer(RaftPeerRole newRole) {
        Object reason = "";
        if (newRole == RaftPeerRole.FOLLOWER) {
            reason = "startAsFollower";
            setRole(RaftPeerRole.FOLLOWER, reason);
        } else if (newRole == RaftPeerRole.LISTENER) {
            reason = "startAsListener";
            setRole(RaftPeerRole.LISTENER, reason);
        } else {
            throw new IllegalArgumentException("Unexpected role " + newRole);
        }
        role.startFollowerState(this, reason);

        lifeCycle.transition(RUNNING);
    }

    private void setRole(RaftPeerRole newRole, Object reason) {
        this.role.transitionRole(newRole);
    }

    TimeDuration getRandomElectionTimeout() {
        if (firstElectionSinceStartup.get()) {
            return getFirstRandomElectionTimeout();
        }
        final int min = divisionProperties.minRpcTimeoutMs();
        final int millis = min + ThreadLocalRandom.current().nextInt(divisionProperties.maxRpcTimeoutMs() - min + 1);
        return TimeDuration.valueOf(millis, TimeUnit.MILLISECONDS);
    }

    private TimeDuration getFirstRandomElectionTimeout() {
        final RaftProperties properties = getProperties();
        final int min = 0;
        final int max = 1;
        final int mills = min + ThreadLocalRandom.current().nextInt(max - min + 1);
        return TimeDuration.valueOf(mills, TimeUnit.MILLISECONDS);
    }

    @Override
    public LifeCycle.State getLifeCycleState() {
        return lifeCycle.getCurrentState();
    }

    @Override
    public void close() throws IOException {

    }

    static class Builder {
        private final RaftPeerId id;
        private final Supplier<RaftPeer> peerSupplier = JavaUtils.memoize(this::buildRaftPeer);
        private final RaftProperties properties;
        private final DivisionProperties divisionProperties;
        private final StateMachine.Registry stateMachineRegistry;
        private final LifeCycle lifeCycle;
        private final ServerState state;
        private final RoleInfo role;
        private final RaftServerRpc serverRpc;
        private final ExecutorService executor;
        private final ThreadGroup threadGroup;

        public RaftServer build(){
            return new RaftServerImpl();
        }
    }

}
