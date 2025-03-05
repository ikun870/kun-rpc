package com.kunclass.channelHandler.handler;

import com.kunclass.KunrpcBootstrap;
import com.kunclass.transport.message.KunrpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<KunrpcResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, KunrpcResponse kunrpcResponse) throws Exception {
        //TODO 处理响应码
//        if(kunrpcResponse.getCode() == 0) {
//            log.error("服务提供方处理请求失败，失败原因：{}",kunrpcResponse.getMessage());
//            return;
//        }
        //服务提供方发送过来的结果
        Object returnValue = kunrpcResponse.getBody();
        returnValue = returnValue == null ? new Object() : returnValue;
        //从全局挂起的请求中获取到对应的completableFuture
        CompletableFuture<Object> completableFuture = KunrpcBootstrap.PENDING_REQUEST.get(kunrpcResponse.getRequestId());
        //将结果设置到completableFuture中
        completableFuture.complete(returnValue);

        if(log.isDebugEnabled()) {
            log.debug("在调用方找到了请求id为{}的completableFuture，处理响应结果",kunrpcResponse.getRequestId());
        }

    }
}
