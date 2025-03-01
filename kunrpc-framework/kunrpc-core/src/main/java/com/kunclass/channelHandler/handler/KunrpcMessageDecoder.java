package com.kunclass.channelHandler.handler;

import com.kunclass.transport.message.KunrpcRequest;
import com.kunclass.transport.message.MessageFormatConstant;
import com.kunclass.transport.message.RequestPayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.Arrays;

@Slf4j
public class KunrpcMessageDecoder extends LengthFieldBasedFrameDecoder {
    public KunrpcMessageDecoder() {
        super(
        //找到当前报文的总长度，截取报文，截取出来的报文我们可以进行解析
        //1.最大帧的长度maxFrameLength，超过这个长度就会被丢弃
        MessageFormatConstant.MAX_FRAME_LENGTH,
        //2.长度域的偏移量lengthFieldOffset，表示长度域在报文中的位置
        MessageFormatConstant.MAGIC_NUMBER.length+MessageFormatConstant.VERSION_LENGTH+MessageFormatConstant.HEAD_FIELD_LENGTH,
        //3.长度域长度lengthFieldLength，表示长度域的长度
        MessageFormatConstant.FULL_FIELD_LENGTH,
        //4.负载的偏移量lengthAdjustment，表示负载在报文中的位置TODO
        -(MessageFormatConstant.MAGIC_NUMBER.length+MessageFormatConstant.VERSION_LENGTH+MessageFormatConstant.HEAD_FIELD_LENGTH+MessageFormatConstant.FULL_FIELD_LENGTH),
        //5.初始值，表示初始的长度
        0);

    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object decode = super.decode(ctx, in);
        if(decode instanceof ByteBuf byteBuf) {
            return decodeFrame(byteBuf);
        }
        return null;
    }

    private Object decodeFrame(ByteBuf byteBuf) {
        //1.解析magicNumber
        byte[] magicNumber = new byte[MessageFormatConstant.MAGIC_NUMBER.length];
        byteBuf.readBytes(magicNumber);
        //检测魔数是否正确
        if(!Arrays.equals(MessageFormatConstant.MAGIC_NUMBER, magicNumber)) {
            throw new RuntimeException("魔数magicNumber不匹配");
        }

        //2.解析version
        byte version = byteBuf.readByte();
        if(version > MessageFormatConstant.VERSION) {
            throw new RuntimeException("版本version不支持");
        }

        //3.解析headLength
        short headLength = byteBuf.readShort();

        //4.解析fullLength
        int fullLength = byteBuf.readInt();

        //5.请求类型
        byte requestType = byteBuf.readByte();

        //6.序列化方式
        byte serializeType = byteBuf.readByte();

        //7.压缩方式
        byte compressType = byteBuf.readByte();

        //8.请求id
        long requestId = byteBuf.readLong();

        //我们需要封装
        KunrpcRequest kunrpcRequest = KunrpcRequest.builder()
                .requestId(requestId)
                .requestType(requestType)
                .serializeType(serializeType)
                .compressType(compressType)
                .build();

        //TODO 心跳请求没有负载，此处可以判断是否有负载
        if(requestType==2) {
            return kunrpcRequest;
        }
        //9.解析body
        int payloadLength = fullLength - headLength;
        byte[] payload = new byte[payloadLength];
        byteBuf.readBytes(payload);

        //有了字节数组之后就可以解压缩，反序列化
        //TODO 解压缩
        //TODO 反序列化
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(payload);
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            RequestPayload requestPayload = (RequestPayload) objectInputStream.readObject();
            kunrpcRequest.setRequestPayload(requestPayload);
        }
        catch (IOException|ClassNotFoundException e) {
            log.error("请求{}反序列化时发生异常",requestId,e);
            throw new RuntimeException(e);
        }
        return kunrpcRequest;

    }
}
