package com.rpcclass.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.Arrays;

public class NettyServer {

    private int port;

    public NettyServer(int port) {
        this.port = port;
    }

    public void run() {
        //定义线程池，EventLoopGroup是一个线程组，它包含了一组NIO线程，专门用于网络事件的处理
        //bossGroup用于接收连接，workerGroup用于具体的处理
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        //启动一个服务端需要一个启动类
        ServerBootstrap bootstrap = new ServerBootstrap();
        //配置启动类
        bootstrap = bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        //添加处理器
                        socketChannel.pipeline().addLast(new MyChannelHandler());
                    }
                });

        //绑定端口，同步等待成功
        try {
            ChannelFuture channelFuture = bootstrap.bind(port).sync();
            System.out.println("Server started on port " + port);
            //等待服务端监听端口关闭
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //关闭线程池
            try {
                bossGroup.shutdownGracefully().sync();
                workerGroup.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public static void main(String[] args) {
        //new NettyServer(8080).run();

        byte[] bytes = "krpc".getBytes();
        //将bytes转为十六进制字符串
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        System.out.println(sb.toString());


    }
}
