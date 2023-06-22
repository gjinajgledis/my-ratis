package org.example.myratis.server;


import org.example.myratis.api.server.RaftServer;
import org.example.myratis.common.util.TimeDuration;
import org.example.myratis.common.util.Timestamp;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;

/**
 * Used when the peer is a follower. Used to track the election timeout.
 */
class FollowerState extends Thread {
  enum UpdateType {
    APPEND_START(AtomicInteger::incrementAndGet),
    APPEND_COMPLETE(AtomicInteger::decrementAndGet),
    INSTALL_SNAPSHOT_START(AtomicInteger::incrementAndGet),
    INSTALL_SNAPSHOT_COMPLETE(AtomicInteger::decrementAndGet),
    INSTALL_SNAPSHOT_NOTIFICATION(AtomicInteger::get),
    REQUEST_VOTE(AtomicInteger::get);

    private final ToIntFunction<AtomicInteger> updateFunction;

    UpdateType(ToIntFunction<AtomicInteger> updateFunction) {
      this.updateFunction = updateFunction;
    }

    int update(AtomicInteger outstanding) {
      return updateFunction.applyAsInt(outstanding);
    }
  }
  private final Object reason;
  private final RaftServer server;

  private final Timestamp creationTime = Timestamp.currentTime();
  private volatile Timestamp lastRpcTime = creationTime;
  private volatile boolean isRunning = true;
  private final AtomicInteger outstandingOp = new AtomicInteger();

  FollowerState(RaftServer server, Object reason) {
    this.server = server;
    this.reason = reason;
  }

  void updateLastRpcTime(UpdateType type) {
    lastRpcTime = Timestamp.currentTime();
    type.update(outstandingOp);

  }

  Timestamp getLastRpcTime() {
    return lastRpcTime;
  }

  int getOutstandingOp() {
    return outstandingOp.get();
  }

  void stopRunning() {
    this.isRunning = false;
  }


  private boolean shouldRun() {
    return true;
  }

  @Override
  public  void run() {
    final TimeDuration sleepDeviationThreshold = server.getSleepDeviationThreshold();
    while (shouldRun()) {
      final TimeDuration electionTimeout = server.getRandomElectionTimeout();
      try {
        final TimeDuration extraSleep = electionTimeout.sleep();
        if (extraSleep.compareTo(sleepDeviationThreshold) > 0) {
          continue;
        }

        if (!shouldRun()) {
          break;
        }
        synchronized (server) {
          if (outstandingOp.get() == 0 && lastRpcTime.elapsedTime().compareTo(electionTimeout) >= 0) {
            break;
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (Exception e) {

      }
    }
  }

  @Override
  public String toString() {
    return getName();
  }
}