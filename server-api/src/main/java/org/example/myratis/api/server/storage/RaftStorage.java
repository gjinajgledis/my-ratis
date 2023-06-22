package org.example.myratis.api.server.storage;


import java.io.Closeable;
import java.io.IOException;


/** The storage of a raft server. */
public interface RaftStorage extends Closeable {
  /** Initialize the storage. */
  void initialize() throws IOException;

  /** @return the storage directory. */
  RaftStorageDirectory getStorageDir();

  /** @return the metadata file. */
  RaftStorageMetadataFile getMetadataFile();


  enum StartupOption {
    /** Format the storage. */
    FORMAT,
    RECOVER
  }

}