package com.kunclass.channelHandler.handler;

import com.kunclass.Serialize.Serializer;
import com.kunclass.Serialize.SerializerFactory;
import com.kunclass.enumeration.RequestType;
import com.kunclass.transport.message.KunrpcRequest;
import com.kunclass.transport.message.MessageFormatConstant;
import com.kunclass.transport.message.RequestPayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * 服务提供方请求解码器
 */

@Slf4j
public class KunrpcRequestDecoder extends LengthFieldBasedFrameDecoder {
    public KunrpcRequestDecoder() {
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

    /**
     * 解码，将字节数组转换为KunrpcRequest对象
     * @param byteBuf
     * @return
     */

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

        //5.序列化方式
        byte serializeType = byteBuf.readByte();

        //6.压缩方式
        byte compressType = byteBuf.readByte();

        //7.请求类型
        byte requestType = byteBuf.readByte();

        //8.请求id
        long requestId = byteBuf.readLong();

        //我们需要封装
        KunrpcRequest kunrpcRequest = KunrpcRequest.builder()
                .requestId(requestId)
                .requestType(requestType)
                .serializeType(serializeType)
                .compressType(compressType)
                .build();


        if(requestType == RequestType.HEARTBEAT.getId()) {
            return kunrpcRequest;
        }
        //9.解析body
        int payloadLength = fullLength - headLength;
        byte[] payload = new byte[payloadLength];
        byteBuf.readBytes(payload);

        //有了字节数组之后就可以解压缩，反序列化
        //TODO 解压缩

        //反序列化
        //1--->jdk   2--->json
        Serializer serializer = SerializerFactory.getSerializerWrapper(serializeType).getSerializer();
        RequestPayload requestPayload = serializer.deserialize(payload, RequestPayload.class);

        kunrpcRequest.setRequestPayload(requestPayload);

        if(log.isDebugEnabled()){
            log.debug("服务提供方请求{}的报文解码完成",requestId);
        }

        return kunrpcRequest;

    }
}
