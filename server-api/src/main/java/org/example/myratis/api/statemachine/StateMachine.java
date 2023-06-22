package org.example.myratis.api.statemachine;


import com.google.protobuf.InvalidProtocolBufferException;
import org.example.myratis.api.server.RaftServer;
import org.example.myratis.api.server.protocol.TermIndex;
import org.example.myratis.api.server.storage.RaftStorage;
import org.example.myratis.common.protocol.ClientInvocationId;
import org.example.myratis.common.protocol.Message;
import org.example.myratis.common.protocol.RaftGroupId;
import org.example.myratis.common.util.JavaUtils;
import org.example.myratis.common.util.LifeCycle;
import org.example.myratis.common.util.ReferenceCountedObject;
import org.example.myratis.proto.RaftProtos.StateMachineLogEntryProto;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * StateMachine is the entry point for the custom implementation of replicated state as defined in
 * the "State Machine Approach" in the literature
 * (see https://en.wikipedia.org/wiki/State_machine_replication).
 *
 *  A {@link StateMachine} implementation must be threadsafe.
 *  For example, the {@link #applyTransaction(TransactionContext)} method and the {@link #query(Message)} method
 *  can be invoked in parallel.
 */
public interface StateMachine extends Closeable {

  /** A registry to support different state machines in multi-raft environment. */
  interface Registry extends Function<RaftGroupId, StateMachine> {
  }


  /**
   * For write state machine data.
   */
  interface DataChannel extends WritableByteChannel {
    /**
     * This method is the same as {@link WritableByteChannel#write(ByteBuffer)}.
     *
     * If the implementation has overridden {@link #write(ReferenceCountedObject)},
     * then it does not have to override this method.
     */
    @Override
    default int write(ByteBuffer buffer) throws IOException {
      throw new UnsupportedOperationException();
    }

    /**
     * Similar to {@link #write(ByteBuffer)}
     * except that the parameter is a {@link ReferenceCountedObject}.
     *
     * This is an optional method.
     * The default implementation is the same as write(referenceCountedBuffer.get()).
     *
     * The implementation may choose to override this method in order to retain the buffer for later use.
     *
     * - If the buffer is retained, it must be released afterward.
     *   Otherwise, the buffer will not be returned, and it will cause a memory leak.
     *
     * - If the buffer is retained multiple times, it must be released the same number of time.
     *
     * - It is safe to access the buffer before this method returns with or without retaining it.
     *
     * - If the buffer is not retained but is accessed after this method returns,
     *   the content of the buffer could possibly be changed unexpectedly, and it will cause data corruption.
     */
    default int write(ReferenceCountedObject<ByteBuffer> referenceCountedBuffer) throws IOException {
      return write(referenceCountedBuffer.get());
    }

    /**
     * Similar to {@link java.nio.channels.FileChannel#force(boolean)},
     * the underlying implementation should force writing the data and/or metadata to the underlying storage.
     *
     * @param metadata Should the metadata be forced?
     * @throws IOException If there are IO errors.
     */
    void force(boolean metadata) throws IOException;
  }


  /**
   * Initializes the State Machine with the given parameter.
   * The state machine must, if there is any, read the latest snapshot.
   */
  void initialize(RaftServer raftServer, RaftGroupId raftGroupId, RaftStorage storage) throws IOException;

  /**
   * Returns the lifecycle state for this StateMachine.
   * @return the lifecycle state.
   */
  LifeCycle.State getLifeCycleState();

  /**
   * Pauses the state machine. On return, the state machine should have closed all open files so
   * that a new snapshot can be installed.
   */
  void pause();

  /**
   * Re-initializes the State Machine in PAUSED state. The
   * state machine is responsible reading the latest snapshot from the file system (if any) and
   * initialize itself with the latest term and index there including all the edits.
   */
  void reinitialize() throws IOException;

  /**
   * Dump the in-memory state into a snapshot file in the RaftStorage. The
   * StateMachine implementation can decide 1) its own snapshot format, 2) when
   * a snapshot is taken, and 3) how the snapshot is taken (e.g., whether the
   * snapshot blocks the state machine, and whether to purge log entries after
   * a snapshot is done).
   *
   * In the meanwhile, when the size of raft log outside of the latest snapshot
   * exceeds certain threshold, the RaftServer may choose to trigger a snapshot
   * if {@link org.apache.ratis.server.RaftServerConfigKeys.Snapshot#AUTO_TRIGGER_ENABLED_KEY} is enabled.
   *
   * The snapshot should include the latest raft configuration.
   *
   * @return the largest index of the log entry that has been applied to the
   *         state machine and also included in the snapshot. Note the log purge
   *         should be handled separately.
   */
  // TODO: refactor this
  long takeSnapshot() throws IOException;

  /**
   * @return StateMachineStorage to interact with the durability guarantees provided by the
   * state machine.
   */
  StateMachineStorage getStateMachineStorage();

  /**
   * Returns the information for the latest durable snapshot.
   */
  SnapshotInfo getLatestSnapshot();

  /**
   * Query the state machine. The request must be read-only.
   */
  CompletableFuture<Message> query(Message request);

  /**
   * Query the state machine, provided minIndex <= commit index.
   * The request must be read-only.
   * Since the commit index of this server may lag behind the Raft service,
   * the returned result may possibly be stale.
   *
   * When minIndex > {@link #getLastAppliedTermIndex()},
   * the state machine may choose to either
   * (1) return exceptionally, or
   * (2) wait until minIndex <= {@link #getLastAppliedTermIndex()} before running the query.
   */
  CompletableFuture<Message> queryStale(Message request, long minIndex);


  /**
   * This is called before the transaction passed from the StateMachine is appended to the raft log.
   * This method is called with the same strict serial order as the transaction order in the raft log.
   * Since this is called serially in the critical path of log append,
   * it is important to do only required operations here.
   * @return The Transaction context.
   */
  TransactionContext preAppendTransaction(TransactionContext trx) throws IOException;

  /**
   * Called to notify the state machine that the Transaction passed cannot be appended (or synced).
   * The exception field will indicate whether there was an exception or not.
   * @param trx the transaction to cancel
   * @return cancelled transaction
   */
  TransactionContext cancelTransaction(TransactionContext trx) throws IOException;

  /**
   * Called for transactions that have been committed to the RAFT log. This step is called
   * sequentially in strict serial order that the transactions have been committed in the log.
   * The SM is expected to do only necessary work, and leave the actual apply operation to the
   * applyTransaction calls that can happen concurrently.
   * @param trx the transaction state including the log entry that has been committed to a quorum
   *            of the raft peers
   * @return The Transaction context.
   */
  TransactionContext applyTransactionSerial(TransactionContext trx) throws InvalidProtocolBufferException;

  /**
   * Apply a committed log entry to the state machine. This method is called sequentially in
   * strict serial order that the transactions have been committed in the log. Note that this
   * method, which returns a future, is asynchronous. The state machine implementation may
   * choose to apply the log entries in parallel. In that case, the order of applying entries to
   * state machine could possibly be different from the log commit order.
   *
   * The implementation must be deterministic so that the raft log can be replayed in any raft peers.
   * Note that, if there are three or more servers,
   * the Raft algorithm makes sure the that log remains consistent
   * even if there are hardware errors in one machine (or less than the majority number of machines).
   *
   * Any exceptions thrown in this method are treated as unrecoverable errors (such as hardware errors).
   * The server will be shut down when it occurs.
   * Administrators should manually fix the underlying problem and then restart the machine.
   *
   * @param trx the transaction state including the log entry that has been replicated to a majority of the raft peers.
   *
   * @return a future containing the result message of the transaction,
   *         where the result message will be replied to the client.
   *         When there is an application level exception (e.g. access denied),
   *         the state machine may complete the returned future exceptionally.
   *         The exception will be wrapped in an {@link org.apache.ratis.protocol.exceptions.StateMachineException}
   *         and then replied to the client.
   */
  CompletableFuture<Message> applyTransaction(TransactionContext trx);

  /** @return the last term-index applied by this {@link StateMachine}. */
  TermIndex getLastAppliedTermIndex();

  /**
   * Converts the given proto to a string.
   *
   * @param proto state machine proto
   * @return the string representation of the proto.
   */
  default String toStateMachineLogEntryString(StateMachineLogEntryProto proto) {
    return JavaUtils.getClassSimpleName(proto.getClass()) +  ":" + ClientInvocationId.valueOf(proto);
  }
}