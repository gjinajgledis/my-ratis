package org.example.myratis;

import org.example.myratis.common.Constants;
import org.example.myratis.common.config.RaftProperties;
import org.example.myratis.common.protocol.RaftPeer;
import org.example.myratis.api.server.RaftServer;
import org.example.myratis.common.util.NetUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;

/**
 * Hello world!
 *
 */
public final class App implements Closeable {

    private final RaftServer server;

    public App(RaftPeer peer) throws IOException {
        //create a property object
        final RaftProperties properties = new RaftProperties();

        //set the storage directory (different for each peer) in the RaftProperty object
        //RaftServerConfigKeys.setStorageDir(properties, Collections.singletonList(storageDir));

        //set the read policy to Linearizable Read.
        //the Default policy will route read-only requests to leader and directly query leader statemachine.
        //Linearizable Read allows to route read-only requests to any group member
        //and uses ReadIndex to guarantee strong consistency.
        //RaftServerConfigKeys.Read.setOption(properties, RaftServerConfigKeys.Read.Option.LINEARIZABLE);
        //set the linearizable read timeout
        //RaftServerConfigKeys.Read.setTimeout(properties, TimeDuration.ONE_MINUTE);

        //set the port (different for each peer) in RaftProperty object
        final int port = NetUtils.createSocketAddr(peer.getAddress()).getPort();
        //GrpcConfigKeys.Server.setPort(properties, port);

        //create the counter state machine which holds the counter value
        final StateMachineExample stateMachineExample = new StateMachineExample();

        //build the Raft server
        this.server = RaftServer.newBuilder()
                .setGroup(Constants.RAFT_GROUP)
                //.setProperties(properties)
                .setServerId(peer.getId())
                .setStateMachine(stateMachineExample)
                .build();
    }

    public void start() throws IOException {
        server.start();
    }

    @Override
    public void close() throws IOException {
        server.close();
    }

    public static void main(String[] args) {
        //get peerIndex from the arguments
        if (args.length != 1) {
            throw new IllegalArgumentException("Invalid argument number: expected to be 1 but actual is " + args.length);
        }
        final int peerIndex = Integer.parseInt(args[0]);
        if (peerIndex < 0 || peerIndex > 2) {
            throw new IllegalArgumentException("The server index must be 0, 1 or 2: peerIndex=" + peerIndex);
        }
        final RaftPeer currentPeer = Constants.PEERS.get(peerIndex);
        try(App app = new App(currentPeer)) {
            app.start();
        } catch(Throwable e) {
            e.printStackTrace();
            System.err.println();
            System.err.println("args = " + Arrays.toString(args));
            System.err.println();
            System.err.println("Usage: java org.apache.ratis.examples.counter.server.CounterServer peer_index");
            System.err.println();
            System.err.println("peer_index must be 0, 1 or 2");
            System.exit(1);
        }
    }
}
