package org.example.myratis.api.server;



import org.example.myratis.api.server.protocol.RaftServerProtocol;
import org.example.myratis.common.protocol.RaftGroupId;
import org.example.myratis.common.protocol.RaftPeerId;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * An server-side interface for supporting different RPC implementations
 * such as Netty, gRPC and Hadoop.
 */
public interface RaftServerRpc extends RaftServerProtocol, Closeable {
  /** Start the RPC service. */
  void start() throws IOException;

  /** @return the address where this RPC server is listening */
  InetSocketAddress getInetSocketAddress();

  /** @return the address where this RPC server is listening for client requests */
  default InetSocketAddress getClientServerAddress() {
    return getInetSocketAddress();
  }
  /** @return the address where this RPC server is listening for admin requests */
  default InetSocketAddress getAdminServerAddress() {
    return getInetSocketAddress();
  }

  /** Handle the given exception.  For example, try reconnecting. */
  void handleException(RaftPeerId serverId, Exception e, boolean reconnect);

  /** The server role changes from leader to a non-leader role. */
  default void notifyNotLeader(RaftGroupId groupId) {
  }

}