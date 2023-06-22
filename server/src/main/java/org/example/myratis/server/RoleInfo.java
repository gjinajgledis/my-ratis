package org.example.myratis.server;


import org.example.myratis.common.protocol.RaftPeerId;
import org.example.myratis.common.util.Preconditions;
import org.example.myratis.common.util.Timestamp;
import org.example.myratis.proto.RaftProtos.RaftPeerRole;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Maintain the Role of a Raft Peer.
 */
class RoleInfo {
  private final RaftPeerId id;
  private volatile RaftPeerRole role;
  /** Used when the peer is follower, to monitor election timeout */
  private final AtomicReference<FollowerState> followerState = new AtomicReference<>();

  private final AtomicReference<Timestamp> transitionTime;

  RoleInfo(RaftPeerId id) {
    this.id = id;
    this.transitionTime = new AtomicReference<>(Timestamp.currentTime());
  }

  void transitionRole(RaftPeerRole newRole) {
    this.role = newRole;
    this.transitionTime.set(Timestamp.currentTime());
  }

  long getRoleElapsedTimeMs() {
    return transitionTime.get().elapsedTimeMs();
  }

  RaftPeerRole getCurrentRole() {
    return role;
  }

  Optional<FollowerState> getFollowerState() {
    return Optional.ofNullable(followerState.get());
  }

  void startFollowerState(RaftServerImpl server, Object reason) {
    updateAndGet(followerState, new FollowerState(server, reason)).start();
  }

  void shutdownFollowerState() {
    final FollowerState follower = followerState.getAndSet(null);
    if (follower != null) {
      LOG.info("{}: shutdown {}", id, follower);
      follower.stopRunning();
      follower.interrupt();
    }
  }

  private <T> T updateAndGet(AtomicReference<T> ref, T current) {
    final T updated = ref.updateAndGet(previous -> previous != null? previous: current);
    Preconditions.assertTrue(updated == current, "previous != null");
    return updated;
  }

  @Override
  public String toString() {
    return String.format("%9s", role);
  }
}