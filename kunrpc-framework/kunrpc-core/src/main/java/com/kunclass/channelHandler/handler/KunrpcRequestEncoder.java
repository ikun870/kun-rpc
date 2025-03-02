package com.kunclass.channelHandler.handler;

import com.kunclass.transport.message.KunrpcRequest;
import com.kunclass.transport.message.MessageFormatConstant;
import com.kunclass.transport.message.RequestPayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;


/**
 * 报文格式：
 * magicNumber（魔数）: 4字节   ---》kunrpc.getBytes()
 * version（版本）: 1字节
 * headLength（头长度）: 2字节
 * fulllength（总长度）: 4字节
 * serialize 序列化方式: 1字节
 * compress 压缩方式: 1字节
 * requestType: 1字节
 * requestId: 8字节
 * body
 * headLength = 4 + 1 + 2 + 4 + 1 + 1 + 1 + 8 = 22
 *这是报文的示意图：
 * 0                4                 5                7               11               12               13              14                 22
 * +----------------+----------------+----------------+----------------+----------------+----------------+----------------+----------------+
 * | magicNumber     | version        | headLength     | fulllength     | serialize      | compress       | requestType    | requestId      |
 * +----------------+----------------+----------------+----------------+----------------+----------------++----------------+----------------+
 * |                                                                                                                                        |
 * |                                                        body                                                                         |
 * ——------------------------------------------------------------------------------------------------------------------------------------——--
 *
 *
 *出站时第一个经过的处理器
 */
@Slf4j
public class KunrpcRequestEncoder extends MessageToByteEncoder<KunrpcRequest> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, KunrpcRequest kunrpcRequest, ByteBuf byteBuf) throws Exception {
        //6b 72 70 63 --用4字节表示
        //Bytes代表写入的是多个字节（数据的形式），数据的值是一个魔数
        byteBuf.writeBytes(MessageFormatConstant.MAGIC_NUMBER);
        //版本号--用1字节表示
        byteBuf.writeByte(MessageFormatConstant.VERSION);
        //头长度--用2字节表示
        byteBuf.writeShort(MessageFormatConstant.HEAD_LENGTH);
        //总长度--用4字节表示但是还不知道body的长度
        //byteBuf.writeInt(0);
        byteBuf.writerIndex(byteBuf.writerIndex() + MessageFormatConstant.FULL_FIELD_LENGTH);
        //序列化方式--用1字节表示
        //压缩方式--用1字节表示
        //请求类型--用1字节表示
        byteBuf.writeByte(kunrpcRequest.getRequestType());
        byteBuf.writeByte(kunrpcRequest.getSerializeType());
        byteBuf.writeByte(kunrpcRequest.getCompressType());
        //请求id--用8字节表示
        byteBuf.writeLong(kunrpcRequest.getRequestId());
//        //判断请求类型，是否是心跳请求
//        if(kunrpcRequest.getRequestType()== RequestType.HEARTBEAT.getId()){
//            //先保存当前写指针的位置
//            int i = byteBuf.writerIndex();
//            byteBuf.writerIndex(MessageFormatConstant.MAGIC_NUMBER.length+MessageFormatConstant.VERSION_LENGTH+MessageFormatConstant.HEAD_FIELD_LENGTH);
//            int fullLength = MessageFormatConstant.HEAD_LENGTH;
//            byteBuf.writeInt(fullLength);
//            //将写指针的位置移动到正常的位置（返回写完请求体的位置）
//            byteBuf.writerIndex(i);
//            return;
//        }
//
        //写入请求体（requestPayload）

        byte[] bodyBytes = getBodyBytes(kunrpcRequest.getRequestPayload());
        if(bodyBytes!=null){
            byteBuf.writeBytes(bodyBytes);
        }
        int bodyLength = bodyBytes==null?0:bodyBytes.length;

        //处理报文总长度
        int fullLength = MessageFormatConstant.HEAD_LENGTH + bodyLength;
        //先保存当前写指针的位置
        int i = byteBuf.writerIndex();
        //将写指针的位置移动到写总长度的位置上（回退）
        byteBuf.writerIndex(MessageFormatConstant.MAGIC_NUMBER.length+MessageFormatConstant.VERSION_LENGTH+MessageFormatConstant.HEAD_FIELD_LENGTH);
        //写入总长度
        byteBuf.writeInt(fullLength);
        //将写指针的位置移动到正常的位置（返回写完请求体的位置）
        byteBuf.writerIndex(i);

        if(log.isDebugEnabled()){
            log.debug("请求{}的报文编码完成",kunrpcRequest.getRequestId());
        }

    }

    private byte[] getBodyBytes(RequestPayload requestPayload) {
        //针对不同的消息类型需要做不同的处理，心跳的请求，没有payload
        if(requestPayload==null){
            return null;
        }
        //希望可以通过一些设计模式，面向对象的编程，让我们可以配置修改序列化和压缩的方式
        //对象变成一个字节数组 序列化 压缩
        try{
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream);
            outputStream.writeObject(requestPayload);
            byte[] bytes = byteArrayOutputStream.toByteArray();
            return bytes;
        }
        catch (IOException e){
            log.error("序列化时发生异常");
            throw new RuntimeException("序列化时发生异常");
        }

    }
}
