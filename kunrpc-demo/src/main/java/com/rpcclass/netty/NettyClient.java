package com.rpcclass.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;


public class NettyClient {

    public void run(){
        //定义线程池，EventLoopGroup是一个线程组，它包含了一组NIO线程，专门用于网络事件的处理
        EventLoopGroup group = new NioEventLoopGroup();

        //启动一个客户端需要一个启动类
        Bootstrap bootstrap = new Bootstrap();
        bootstrap= bootstrap.group(group)
                .remoteAddress(new InetSocketAddress(8080))
                //指定channel类型
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        //添加处理器
                        socketChannel.pipeline().addLast(new MyChannelHandler2());
                    }
                });
        //尝试连接服务器
        try {
            ChannelFuture channelFuture = bootstrap.connect().sync();
            System.out.println("Client connected to server");
            //获取channel，并且写出数据
            channelFuture.channel().writeAndFlush(Unpooled.copiedBuffer("hello,Netty!", Charset.defaultCharset()));
            //等待连接关闭.阻塞程序，等到接收消息，channel关闭
            channelFuture.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            //关闭线程池
            try {
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static void main(String[] args) {
        new NettyClient().run();
    }

}
