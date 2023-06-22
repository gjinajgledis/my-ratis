package org.example.myratis.api.statemachine;


import com.google.protobuf.ByteString;
import org.example.myratis.common.util.Preconditions;
import org.example.myratis.proto.RaftProtos.RaftPeerRole;
import org.example.myratis.proto.RaftProtos.StateMachineLogEntryProto;
import org.example.myratis.proto.RaftProtos.LogEntryProto;

import java.io.IOException;
import java.util.Objects;

/**
 * Context for a transaction.
 * The transaction might have originated from a client request, or it
 * maybe coming from another replica of the state machine through the RAFT log.
 * {@link TransactionContext} can be created from
 * either the {@link StateMachine} or the state machine updater.
 *
 * In the first case, the {@link StateMachine} is a leader. When it receives
 * a {@link StateMachine#startTransaction(RaftClientRequest)} request, it returns
 * a {@link TransactionContext} with the changes from the {@link StateMachine}.
 * The same context will be passed back to the {@link StateMachine}
 * via the {@link StateMachine#applyTransaction(TransactionContext)} call.
 *
 * In the second case, the {@link StateMachine} is a follower.
 * The {@link TransactionContext} will be a committed entry coming from
 * the RAFT log from the leader.
 */
public interface TransactionContext {
  /** @return the role of the server when this context is created. */
  RaftPeerRole getServerRole();


  /**
   * Returns the data from the {@link StateMachine}
   * @return the data from the {@link StateMachine}
   */
  StateMachineLogEntryProto getStateMachineLogEntry();

  /** Set exception in case of failure. */
  TransactionContext setException(Exception exception);

  /**
   * Returns the exception from the {@link StateMachine} or the log
   * @return the exception from the {@link StateMachine} or the log
   */
  Exception getException();

  /**
   * Sets the {@link StateMachine} the {@link TransactionContext} is specific to, the method would
   * not create a new transaction context, it updates the {@link StateMachine} it associates with
   * @param stateMachineContext state machine context
   * @return transaction context specific to the given {@link StateMachine}
   */
  TransactionContext setStateMachineContext(Object stateMachineContext);

  /**
   * Returns the {@link StateMachine} the current {@link TransactionContext} specific to
   * @return the {@link StateMachine} the current {@link TransactionContext} specific to
   */
  Object getStateMachineContext();

  /**
   * Initialize {@link LogEntryProto} using the internal {@link StateMachineLogEntryProto}.
   * @param term The current term.
   * @param index The index of the log entry.
   * @return the result {@link LogEntryProto}
   */
  LogEntryProto initLogEntry(long term, long index);

  /**
   * Returns the committed log entry
   * @return the committed log entry
   */
  LogEntryProto getLogEntry();

  /**
   * Sets whether to commit the transaction to the RAFT log or not
   * @param shouldCommit true if the transaction is supposed to be committed to the RAFT log
   * @return the current {@link TransactionContext} itself
   */
  TransactionContext setShouldCommit(boolean shouldCommit);

  /**
   * It indicates if the transaction should be committed to the RAFT log
   * @return true if it commits the transaction to the RAFT log, otherwise, false
   */
  boolean shouldCommit();

  // proxy StateMachine methods. We do not want to expose the SM to the RaftLog

  /**
   * This is called before the transaction passed from the StateMachine is appended to the raft log.
   * This method will be called from log append and having the same strict serial order that the
   * Transactions will have in the RAFT log. Since this is called serially in the critical path of
   * log append, it is important to do only required operations here.
   * @return The Transaction context.
   */
  TransactionContext preAppendTransaction() throws IOException;

  /**
   * Called to notify the state machine that the Transaction passed cannot be appended (or synced).
   * The exception field will indicate whether there was an exception or not.
   * @return cancelled transaction
   */
  TransactionContext cancelTransaction() throws IOException;


}