package com.kunclass.channelHandler.handler;

import com.kunclass.enumeration.RequestType;
import com.kunclass.transport.message.KunrpcRequest;
import com.kunclass.transport.message.KunrpcResponse;
import com.kunclass.transport.message.MessageFormatConstant;
import com.kunclass.transport.message.RequestPayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;

@Slf4j
public class KunrpcResponseDecoder extends LengthFieldBasedFrameDecoder {
    public KunrpcResponseDecoder() {
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

        //5.响应码
        byte reponseCode = byteBuf.readByte();

        //6.序列化方式
        byte serializeType = byteBuf.readByte();

        //7.压缩方式
        byte compressType = byteBuf.readByte();

        //8.请求id
        long requestId = byteBuf.readLong();

        //我们需要封装
        KunrpcResponse kunrpcResponse = KunrpcResponse.builder()
                .requestId(requestId)
                .compressType(compressType)
                .serializeType(serializeType)
                .code(reponseCode)
                .build();

        //TODO 心跳？
//        if(requestType== RequestType.HEARTBEAT.getId()) {
//            return kunrpcRequest;
//        }
        //9.解析body
        int bodyLength = fullLength - headLength;
        byte[] body = new byte[bodyLength];
        byteBuf.readBytes(body);

        //有了字节数组之后就可以解压缩，反序列化
        //TODO 解压缩
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body);
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            Object object  =  objectInputStream.readObject();
            kunrpcResponse.setBody(object);
        }
        catch (IOException|ClassNotFoundException e) {
            log.error("响应{}反序列化时发生异常",requestId,e);
            throw new RuntimeException(e);
        }

        if(log.isDebugEnabled()){
            log.debug("响应报文在调用方解码成功，请求id：{}，响应码：{}，序列化方式：{}，压缩方式：{}，body长度：{}",
                    kunrpcResponse.getRequestId(),kunrpcResponse.getCode(),kunrpcResponse.getSerializeType(),kunrpcResponse.getCompressType(),bodyLength);
        }

        return kunrpcResponse;

    }
}
