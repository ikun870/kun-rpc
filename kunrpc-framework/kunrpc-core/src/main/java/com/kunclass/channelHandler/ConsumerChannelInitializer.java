package com.kunclass.channelHandler;

import com.kunclass.channelHandler.handler.KunrpcRequestEncoder;
import com.kunclass.channelHandler.handler.KunrpcResponseDecoder;
import com.kunclass.channelHandler.handler.MySimpleChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * 消费者端的Channel初始化器
 * 这里handler与channel的关系是一对多的关系，一个channel可以有多个handler，handler之间是有顺序的
 * handler是处理消息的，所处理的消息是channel中的消息
 * channel是处理连接的
 */
public class ConsumerChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {

        socketChannel.pipeline()
                //netty默认的自带处理器
                .addLast(new LoggingHandler(LogLevel.DEBUG))
                //出站消息编码器
                .addLast(new KunrpcRequestEncoder())
                //入站消息解码器
                .addLast(new KunrpcResponseDecoder())
                //处理结果
                .addLast(new MySimpleChannelInboundHandler());
    }
}
