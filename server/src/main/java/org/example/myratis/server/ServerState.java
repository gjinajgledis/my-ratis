package org.example.myratis.server;


import org.example.myratis.api.server.RaftServer;
import org.example.myratis.api.server.protocol.TermIndex;
import org.example.myratis.api.server.storage.RaftStorage;
import org.example.myratis.api.statemachine.StateMachine;
import org.example.myratis.common.config.RaftProperties;
import org.example.myratis.common.protocol.RaftGroup;
import org.example.myratis.common.protocol.RaftPeer;
import org.example.myratis.common.protocol.RaftPeerId;
import org.example.myratis.common.util.TimeDuration;
import org.example.myratis.common.util.Timestamp;
import org.example.myratis.proto.RaftProtos.RaftPeerRole;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


/**
 * Common states of a raft peer. Protected by RaftServer's lock.
 */
class ServerState {
  private final RaftServer server;

  /** local storage for log and snapshot */
  private final MemoizedCheckedSupplier<RaftStorage, IOException> raftStorage;

  private volatile Timestamp lastNoLeaderTime;
  private final TimeDuration noLeaderTimeout;
  private final RaftPeerId id;

  /**
   * Latest term server has seen.
   * Initialized to 0 on first boot, increases monotonically.
   */
  private final AtomicLong currentTerm = new AtomicLong();
  /**
   * The server ID of the leader for this term. Null means either there is
   * no leader for this term yet or this server does not know who it is yet.
   */
  private final AtomicReference<RaftPeerId> leaderId = new AtomicReference<>();

  /**
   * Latest installed snapshot for this server. This maybe different than StateMachine's latest
   * snapshot. Once we successfully install a snapshot, the SM may not pick it up immediately.
   * Further, this will not get updated when SM does snapshots itself.
   */
  private final AtomicReference<TermIndex> latestInstalledSnapshot = new AtomicReference<>();

  ServerState(RaftPeerId id, RaftGroup group, StateMachine stateMachine, RaftServer server,
              RaftStorage.StartupOption option, RaftProperties prop) {

    this.server = server;
    this.id =id;

    Collection<RaftPeer> followerPeers = group.getPeers().stream()
        .filter(peer -> peer.getStartupRole() == RaftPeerRole.FOLLOWER).toList();
    Collection<RaftPeer> listenerPeers = group.getPeers().stream()
        .filter(peer -> peer.getStartupRole() == RaftPeerRole.LISTENER).toList();
    final String storageDirName = group.getGroupId().getUuid().toString();

    this.raftStorage = MemoizedCheckedSupplier.valueOf(
        () -> StorageImplUtils.initRaftStorage(storageDirName, option, prop));


    // On start the leader is null, start the clock now
    this.lastNoLeaderTime = Timestamp.currentTime();
    this.noLeaderTimeout = TimeDuration.ONE_MILLISECOND;

  }

  void initialize(StateMachine stateMachine) throws IOException {
    // initialize raft storage
    final RaftStorage storage =null;
    // read configuration from the storage
    Optional.ofNullable(storage.readRaftConfiguration()).ifPresent(this::setRaftConf);

    stateMachine.initialize(server.getRaftServer(), getMemberId().getGroupId(), storage);

  }


  void start() {
    //stateMachineUpdater.get().start();
  }

  long getCurrentTerm() {
    return currentTerm.get();
  }


}