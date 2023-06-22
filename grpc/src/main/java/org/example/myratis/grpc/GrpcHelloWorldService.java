package org.example.myratis.grpc;

import io.grpc.stub.StreamObserver;
import org.example.myratis.proto.HelloProtos.HelloRequest;
import org.example.myratis.proto.HelloProtos.HelloReplay;
import org.example.myratis.proto.grpc.HelloWorldServiceGrpc.HelloWorldServiceImplBase;
public class GrpcHelloWorldService extends HelloWorldServiceImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReplay> responseObserver) {
        HelloReplay replay = HelloReplay.newBuilder().setId(request.getId()).setHello("Hello!!").build();
        responseObserver.onNext(replay);
        responseObserver.onCompleted();
    }
}
