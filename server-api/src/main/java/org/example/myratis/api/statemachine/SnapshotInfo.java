package org.example.myratis.api.statemachine;

import java.util.List;

import org.example.myratis.api.server.protocol.TermIndex;
import org.example.myratis.api.server.storage.FileInfo;

/**
 * The information of a state machine snapshot,
 * where a snapshot captures the states at a particular {@link TermIndex}.
 * Each state machine implementation must define its snapshot format and persist snapshots in a durable storage.
 */
public interface SnapshotInfo {

  /**
   * @return The term and index corresponding to this snapshot.
   */
  TermIndex getTermIndex();

  /**
   * @return The term corresponding to this snapshot.
   */
  default long getTerm() {
    return getTermIndex().getTerm();
  }

  /**
   * @return The index corresponding to this snapshot.
   */
  default long getIndex() {
    return getTermIndex().getIndex();
  }

  /**
   * @return a list of underlying files of this snapshot.
   */
  List<FileInfo> getFiles();
}