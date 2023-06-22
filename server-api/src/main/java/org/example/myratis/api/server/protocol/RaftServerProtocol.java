package org.example.myratis.api.server.protocol;

import org.example.myratis.proto.RaftProtos.RequestVoteReplyProto;
import org.example.myratis.proto.RaftProtos.RequestVoteRequestProto;

import java.io.IOException;

public interface RaftServerProtocol {
    enum Op {REQUEST_VOTE, APPEND_ENTRIES, INSTALL_SNAPSHOT}

    RequestVoteReplyProto requestVote(RequestVoteRequestProto request) throws IOException;

}
