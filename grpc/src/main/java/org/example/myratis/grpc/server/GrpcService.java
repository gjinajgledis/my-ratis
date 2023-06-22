package org.example.myratis.grpc.server;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;
import org.example.myratis.api.server.RaftServerRpc;
import org.example.myratis.common.util.JavaUtils;
import org.example.myratis.common.util.MemoizedSupplier;
import org.example.myratis.common.util.SizeInBytes;
import org.example.myratis.api.server.RaftServer;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class GrpcService implements RaftServerRpc {

    private Map<String, Server> servers = new HashMap<>();
    private Supplier<InetSocketAddress> addressSupplier;
    public GrpcService(RaftServer raftServer,
                       String serverHost, int serverPort,
                       SizeInBytes grpcMessageSizeMax,
                       SizeInBytes flowControlWindow) {

        final NettyServerBuilder serverBuilder = startBuildingNettyServer(serverHost, serverPort, grpcMessageSizeMax, flowControlWindow);
        serverBuilder.addService(new GrpcServerProtocolService(raftServer));
        final Server server = serverBuilder.build();
        servers.put(GrpcServerProtocolService.class.getSimpleName(), server);
        addressSupplier = newAddressSupplier(serverPort, server);

    }
    private MemoizedSupplier<InetSocketAddress> newAddressSupplier(int port, Server server) {
        return JavaUtils.memoize(() -> new InetSocketAddress(port != 0 ? port : server.getPort()));
    }

    private static NettyServerBuilder startBuildingNettyServer(String hostname, int port, SizeInBytes grpcMessageSizeMax, SizeInBytes flowControlWindow) {
        InetSocketAddress address = hostname == null || hostname.isEmpty() ? new InetSocketAddress(port) : new InetSocketAddress(hostname, port);
        NettyServerBuilder nettyServerBuilder = NettyServerBuilder.forAddress(address)
                .withChildOption(ChannelOption.SO_REUSEADDR, true)
                .maxInboundMessageSize(grpcMessageSizeMax.getSizeInt())
                .flowControlWindow(flowControlWindow.getSizeInt());

        return nettyServerBuilder;
    }
}
