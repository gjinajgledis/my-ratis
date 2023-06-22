package org.example.myratis.common.protocol;


import com.google.protobuf.ByteString;

import java.util.UUID;

/**
 * The id of a raft group.
 *
 * This is a value-based class.
 */
public final class RaftGroupId extends RaftId {
  private static final RaftGroupId EMPTY_GROUP_ID = new RaftGroupId(ZERO_UUID_BYTESTRING);

  public static RaftGroupId emptyGroupId() {
    return EMPTY_GROUP_ID;
  }

  public static RaftGroupId randomId() {
    return new RaftGroupId(UUID.randomUUID());
  }

  public static RaftGroupId valueOf(UUID uuid) {
    return new RaftGroupId(uuid);
  }

  public static RaftGroupId valueOf(ByteString data) {
    return new RaftGroupId(data);
  }

  private RaftGroupId(UUID id) {
    super(id);
  }

  private RaftGroupId(ByteString data) {
    super(data);
  }

  @Override
  String createUuidString(UUID uuid) {
    return "group-" + super.createUuidString(uuid);
  }
}