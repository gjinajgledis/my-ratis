package org.example.myratis.grpc.server;

import io.grpc.stub.StreamObserver;
import org.example.myratis.proto.RaftProtos.RequestVoteReplyProto;
import org.example.myratis.proto.RaftProtos.RequestVoteRequestProto;
import org.example.myratis.proto.grpc.RaftServerProtocolServiceGrpc.RaftServerProtocolServiceImplBase;
import org.example.myratis.api.server.RaftServer;

public class GrpcServerProtocolService extends RaftServerProtocolServiceImplBase {

    private RaftServer raftServer;

    public GrpcServerProtocolService(RaftServer raftServer) {
        this.raftServer = raftServer;
    }

    @Override
    public void requestVote(RequestVoteRequestProto request, StreamObserver<RequestVoteReplyProto> responseObserver){
       try{
           final RequestVoteReplyProto replyProto = raftServer.requestVote(request);
           responseObserver.onNext(replyProto);
           responseObserver.onCompleted();
       }catch (Exception e){
           responseObserver.onError(e);
       }
    }

}
