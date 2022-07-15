package com.dc.piglet.rtmp.server;

import com.dc.piglet.rtmp.conf.ServerConfig;
import com.dc.piglet.rtmp.handler.HandShakeHandler;
import com.dc.piglet.rtmp.handler.RtmpDecoder;
import com.dc.piglet.rtmp.handler.RtmpEncoder;
import com.dc.piglet.rtmp.handler.ServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 启动器
 */
public class Server {

    static {
        APPLICATIONS = new ConcurrentHashMap<String, ServerApplication>();
        TIMER = new HashedWheelTimer(10, TimeUnit.MILLISECONDS);
    }

    protected static final Map<String,ServerApplication> APPLICATIONS;
    public static final Timer TIMER;
    public static String serverPort = ServerConfig.config.getProperty("server.port");

    /**
     *  header -- handshake(in) -- rtmpDecoder(in) -- rtmpEncoder(out) -- serverHandler(all) -- tail
     *
     */
    public static void start(int port){
        NioEventLoopGroup boosGroup = new NioEventLoopGroup();
        NioEventLoopGroup workGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(boosGroup,workGroup).
                    channel(NioServerSocketChannel.class).
                    childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new HandShakeHandler());
                            socketChannel.pipeline().addLast(new RtmpDecoder());
                            socketChannel.pipeline().addLast(new RtmpEncoder());
                            socketChannel.pipeline().addLast(new ServerHandler());
                        }
                    });

            Channel ch = serverBootstrap.bind(port).sync().channel();
            ch.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            boosGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        start(Integer.parseInt(serverPort));
    }
}
