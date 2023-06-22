package org.example.myratis.api.statemachine;

import org.example.myratis.api.server.storage.RaftStorage;

import java.io.File;
import java.io.IOException;

public interface StateMachineStorage {

  void init(RaftStorage raftStorage) throws IOException;

  /**
   * Returns the information for the latest durable snapshot.
   */
  SnapshotInfo getLatestSnapshot();

  // TODO: StateMachine can decide to compact the files independently of concurrent install snapshot
  // etc requests. We should have ref counting for the SnapshotInfo with a release mechanism
  // so that raft server will release the files after the snapshot file copy in case a compaction
  // is waiting for deleting these files.

  void format() throws IOException;


  /** @return the state machine directory. */
  default File getSnapshotDir() {
    return null;
  }

  /** @return the temporary directory. */
  default File getTmpDir() {
    return null;
  }
}