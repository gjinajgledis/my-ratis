package org.example.myratis.api.server;

import org.example.myratis.api.server.protocol.RaftServerProtocol;
import org.example.myratis.common.config.RaftProperties;
import org.example.myratis.common.protocol.RaftGroup;
import org.example.myratis.common.protocol.RaftGroupId;
import org.example.myratis.common.protocol.RaftPeer;
import org.example.myratis.common.protocol.RaftPeerId;
import org.example.myratis.common.util.LifeCycle;

import java.io.Closeable;
import java.io.IOException;

public interface RaftServer extends RaftServerProtocol, Closeable {

    /** @return the server ID. */
    RaftPeerId getId();

    /**
     * @return the general {@link RaftPeer} for this server.
     *         To obtain a specific {@link RaftPeer} for a {@link RaftGroup}, use {@link Division#getPeer()}.
     */
    RaftPeer getPeer();

    /** @return the group IDs the server is part of. */
    Iterable<RaftGroupId> getGroupIds();

    /** @return the groups the server is part of. */
    Iterable<RaftGroup> getGroups() throws IOException;

    /** @return the server properties. */
    RaftProperties getProperties();

    /** @return the rpc service. */
    RaftServerRpc getServerRpc();

    /** Start this server. */
    void start() throws IOException;

    LifeCycle.State getLifeCycleState();
}
