package org.example.myratis.common.protocol;

import com.google.protobuf.ByteString;

import java.util.Optional;
import java.util.UUID;

/**
 * Id of Raft client. Should be globally unique so that raft peers can use it
 * to correctly identify retry requests from the same client.
 */
public final class ClientId extends RaftId {
  private static final ClientId EMPTY_CLIENT_ID = new ClientId(ZERO_UUID_BYTESTRING);

  public static ClientId emptyClientId() {
    return EMPTY_CLIENT_ID;
  }

  public static ClientId randomId() {
    return new ClientId(UUID.randomUUID());
  }

  public static ClientId valueOf(ByteString data) {
    return Optional.ofNullable(data).filter(d -> !d.isEmpty()).map(ClientId::new).orElse(EMPTY_CLIENT_ID);
  }

  public static ClientId valueOf(UUID uuid) {
    return new ClientId(uuid);
  }

  private ClientId(ByteString data) {
    super(data);
  }

  private ClientId(UUID uuid) {
    super(uuid);
  }

  @Override
  String createUuidString(UUID uuid) {
    return "client-" + super.createUuidString(uuid);
  }
}