package org.example.myratis.api.server;



import org.example.myratis.common.util.TimeDuration;

import java.util.concurrent.TimeUnit;

/**
 * The properties set for a server division.
 *
 * @see RaftServerConfigKeys
 */
public interface DivisionProperties {

  /** @return the minimum rpc timeout. */
  TimeDuration minRpcTimeout();

  /** @return the minimum rpc timeout in milliseconds. */
  default int minRpcTimeoutMs() {
    return minRpcTimeout().toIntExact(TimeUnit.MILLISECONDS);
  }

  /** @return the maximum rpc timeout. */
  TimeDuration maxRpcTimeout();

  /** @return the maximum rpc timeout in milliseconds. */
  default int maxRpcTimeoutMs() {
    return maxRpcTimeout().toIntExact(TimeUnit.MILLISECONDS);
  }

  /** @return the rpc sleep time period. */
  TimeDuration rpcSleepTime();

  /** @return the rpc slowness timeout. */
  TimeDuration rpcSlownessTimeout();
}