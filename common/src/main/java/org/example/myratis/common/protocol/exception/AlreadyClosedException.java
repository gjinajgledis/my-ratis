
package org.example.myratis.common.protocol.exception;

/**
 * The corresponding object is already closed.
 */
public class AlreadyClosedException extends RaftException {
  public AlreadyClosedException(String message) {
    super(message);
  }

  public AlreadyClosedException(String message, Throwable t) {
    super(message, t);
  }
}