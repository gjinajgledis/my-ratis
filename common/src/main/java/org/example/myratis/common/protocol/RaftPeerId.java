package org.example.myratis.common.protocol;

import com.google.protobuf.ByteString;
import org.example.myratis.common.util.JavaUtils;
import org.example.myratis.common.util.Preconditions;
import org.example.myratis.proto.RaftProtos.RaftPeerIdProto;


import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Id of Raft Peer which is globally unique.
 *
 * This is a value-based class.
 */
public final class RaftPeerId {
  private static final Map<ByteString, RaftPeerId> BYTE_STRING_MAP = new ConcurrentHashMap<>();
  private static final Map<String, RaftPeerId> STRING_MAP = new ConcurrentHashMap<>();

  public static RaftPeerId valueOf(ByteString id) {
    return BYTE_STRING_MAP.computeIfAbsent(id, RaftPeerId::new);
  }

  public static RaftPeerId valueOf(String id) {
    return STRING_MAP.computeIfAbsent(id, RaftPeerId::new);
  }

  public static RaftPeerId getRaftPeerId(String id) {
    return id == null || id.isEmpty() ? null : RaftPeerId.valueOf(id);
  }

  /** UTF-8 string as id */
  private final String idString;
  /** The corresponding bytes of {@link #idString}. */
  private final ByteString id;

  private final Supplier<RaftPeerIdProto> raftPeerIdProto;

  private RaftPeerId(String id) {
    this.idString = Objects.requireNonNull(id, "id == null");
    this.id = ByteString.copyFrom(idString, StandardCharsets.UTF_8);
    this.raftPeerIdProto = JavaUtils.memoize(this::buildRaftPeerIdProto);
  }

  private RaftPeerId(ByteString id) {
    this.id = Objects.requireNonNull(id, "id == null");
    Preconditions.assertTrue(id.size() > 0, "id is empty.");
    this.idString = id.toString(StandardCharsets.UTF_8);
    this.raftPeerIdProto = JavaUtils.memoize(this::buildRaftPeerIdProto);
  }

  private RaftPeerIdProto buildRaftPeerIdProto() {
    final RaftPeerIdProto.Builder builder = RaftPeerIdProto.newBuilder().setId(id);
    return builder.build();
  }

  public RaftPeerIdProto getRaftPeerIdProto() {
    return raftPeerIdProto.get();
  }

  /**
   * @return id in {@link ByteString}.
   */
  public ByteString toByteString() {
    return id;
  }

  @Override
  public String toString() {
    return idString;
  }

  @Override
  public boolean equals(Object other) {
    return other == this ||
        (other instanceof RaftPeerId && idString.equals(((RaftPeerId)other).idString));
  }

  @Override
  public int hashCode() {
    return idString.hashCode();
  }
}