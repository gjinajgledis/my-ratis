package org.example.myratis.api.server.protocol;


import org.example.myratis.proto.RaftProtos.LogEntryProto;
import org.example.myratis.proto.RaftProtos.TermIndexProto;

import java.util.Comparator;
import java.util.Optional;

/** The term and the log index defined in the Raft consensus algorithm. */
public interface TermIndex extends Comparable<TermIndex> {
  TermIndex[] EMPTY_ARRAY = {};

  /** @return the term. */
  long getTerm();

  /** @return the index. */
  long getIndex();

  /** @return the {@link TermIndexProto}. */
  default TermIndexProto toProto() {
    return TermIndexProto.newBuilder()
        .setTerm(getTerm())
        .setIndex(getIndex())
        .build();
  }

  @Override
  default int compareTo(TermIndex that) {
    return Comparator.comparingLong(TermIndex::getTerm)
        .thenComparingLong(TermIndex::getIndex)
        .compare(this, that);
  }

  /** @return a {@link TermIndex} object from the given proto. */
  static TermIndex valueOf(TermIndexProto proto) {
    return Optional.ofNullable(proto).map(p -> valueOf(p.getTerm(), p.getIndex())).orElse(null);
  }

  /** @return a {@link TermIndex} object from the given proto. */
  static TermIndex valueOf(LogEntryProto proto) {
    return Optional.ofNullable(proto).map(p -> valueOf(p.getTerm(), p.getIndex())).orElse(null);
  }

  /** @return a {@link TermIndex} object. */
  static TermIndex valueOf(long term, long index) {
    return new TermIndex() {
      @Override
      public long getTerm() {
        return term;
      }

      @Override
      public long getIndex() {
        return index;
      }

      @Override
      public boolean equals(Object obj) {
        if (obj == this) {
          return true;
        } else if (!(obj instanceof TermIndex)) {
          return false;
        }

        final TermIndex that = (TermIndex) obj;
        return this.getTerm() == that.getTerm()
            && this.getIndex() == that.getIndex();
      }

      @Override
      public int hashCode() {
        return Long.hashCode(term) ^ Long.hashCode(index);
      }

      private String longToString(long n) {
        return n >= 0L? String.valueOf(n) : "~";
      }

      @Override
      public String toString() {
        return String.format("(t:%s, i:%s)", longToString(term), longToString(index));
      }
    };
  }
}