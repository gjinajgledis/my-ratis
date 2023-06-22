package org.example.myratis.api.server.storage;

import java.io.File;

/** The directory of a {@link RaftStorage}. */
public interface RaftStorageDirectory {

  String CURRENT_DIR_NAME = "current";
  String STATE_MACHINE_DIR_NAME = "sm"; // directory containing state machine snapshots
  String TMP_DIR_NAME = "tmp";

  /** @return the root directory of this storage */
  File getRoot();

  /** @return the current directory. */
  default File getCurrentDir() {
    return new File(getRoot(), CURRENT_DIR_NAME);
  }

  /** @return the state machine directory. */
  default File getStateMachineDir() {
    return new File(getRoot(), STATE_MACHINE_DIR_NAME);
  }

  /** @return the temporary directory. */
  default File getTmpDir() {
    return new File(getRoot(), TMP_DIR_NAME);
  }

  /** Is this storage healthy? */
  boolean isHealthy();
}