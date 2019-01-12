package com.tinfig.wsosc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpChunkAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

public class BridgeThread extends Thread {
    private static final int WEBSOCKET_PORT = 5000;
    private final List<OscTarget> oscTargets;
    private final Runnable onMessageProcessed;
    private final Runnable onShutdown;
    private final Logger logger;

    private volatile ServerBootstrap serverBootstrap;
    private volatile boolean crashed;

    public BridgeThread(List<OscTarget> oscTargets, Runnable onMessageProcessed, Runnable onShutdown, Logger logger) {
        super("BridgeThread");
        this.oscTargets = Collections.unmodifiableList(oscTargets);
        this.onMessageProcessed = onMessageProcessed;
        this.onShutdown = onShutdown;
        this.logger = logger;

        setDaemon(false);
    }

    @Override
    public void run() {
        serverBootstrap = new ServerBootstrap();
        try {
            serverBootstrap.group(new NioEventLoopGroup(), new NioEventLoopGroup())
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(WEBSOCKET_PORT))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(final SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    new HttpRequestDecoder(),
                                    new HttpChunkAggregator(65536),
                                    new HttpResponseEncoder(),
                                    new WebSocketServerProtocolHandler("/faces"),
                                    new FaceMessageHandler(oscTargets, onMessageProcessed));
                        }
                    });

            final Channel ch = serverBootstrap.bind().sync().channel();

            logger.log("Starting bridge thread on TCP " + WEBSOCKET_PORT + " with " + oscTargets.size() + " targets:");
            oscTargets.forEach(t -> logger.log("  " + t.address + ":" + t.port));
            logger.log("Bridge thread running!");

            ch.closeFuture().sync();
        } catch (Exception e) {
            logger.log("Bridge thread error: " + e.getMessage());
            logger.log(Utils.toString(e));
            this.crashed = true;
        } finally {
            serverBootstrap.shutdown();
            logger.log("Bridge thread stopped.");
            onShutdown.run();
        }
    }

    public void shutdownBridge() {
        if (serverBootstrap != null) {
            logger.log("Stopping bridge thread...");
            serverBootstrap.shutdown();
        }
    }

    public boolean isCrashed() {
        return crashed;
    }
}
