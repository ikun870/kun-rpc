package com.kunclass.discovery;

import com.kunclass.KunrpcBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
//TODO：这里会有问题
public class NettyBootstrapInitializer {

    private static Bootstrap bootstrap = new Bootstrap();

    //这段代码没有放在getInstance方法中，是因为这段代码只需要执行一次
    //避免了当很多个线程同时调用getInstance方法时，重复创建连接channel
    static {
        EventLoopGroup group = new NioEventLoopGroup();
        //新建一个连接channel
        bootstrap = bootstrap.group(group)
                //指定channel类型
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        //添加处理器
                        socketChannel.pipeline().addLast(null);
                    }
                });
    }

    private NettyBootstrapInitializer() {
    }
    public static Bootstrap getInstance() {
        return bootstrap;
    }
}
