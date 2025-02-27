package com.kunclass.channelHandler.handler;

import com.kunclass.KunrpcBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        //服务提供方发送过来的结果
        String result = msg.toString(Charset.defaultCharset());
        //从全局挂起的请求中获取到对应的completableFuture
        CompletableFuture<Object> completableFuture = KunrpcBootstrap.PENDING_REQUEST.get(1L);
        //将结果设置到completableFuture中
        completableFuture.complete(result);
    }
}
