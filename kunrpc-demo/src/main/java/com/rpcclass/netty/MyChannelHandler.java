package com.rpcclass.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.Charset;

public class MyChannelHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
        System.out.println("Server received: " + byteBuf.toString(Charset.defaultCharset()));
        //回写数据
        //可以通过ctx获取channel，然后写出数据
        ctx.write(Unpooled.copiedBuffer("Hello,Client!", Charset.defaultCharset()));
        ctx.flush();
        //ctx.writeAndFlush("Hello,Client!");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
