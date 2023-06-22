package org.example.myratis.api.server.storage;

import org.example.myratis.common.io.MD5Hash;

import java.nio.file.Path;


/**
 * Metadata about a file.
 *
 * The objects of this class are immutable.
 */
public class FileInfo {
  private final Path path;
  private final MD5Hash fileDigest;
  private final long fileSize;

  public FileInfo(Path path, MD5Hash fileDigest) {
    this.path = path;
    this.fileDigest = fileDigest;
    this.fileSize = path.toFile().length();
  }

  @Override
  public String toString() {
    return path.toString();
  }

  /** @return the path of the file. */
  public Path getPath() {
    return path;
  }

  /** @return the MD5 file digest of the file. */
  public MD5Hash getFileDigest() {
    return fileDigest;
  }

  /** @return the size of the file. */
  public long getFileSize() {
    return fileSize;
  }
}