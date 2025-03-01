package com.kunclass.channelHandler;

import com.kunclass.channelHandler.handler.KunrpcMessageEncoder;
import com.kunclass.channelHandler.handler.MySimpleChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class ConsumerChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {

        socketChannel.pipeline()
                //netty默认的自带处理器
                .addLast(new LoggingHandler(LogLevel.DEBUG))
                //消息编码器
                .addLast(new KunrpcMessageEncoder())
                .addLast(new MySimpleChannelInboundHandler());
    }
}
