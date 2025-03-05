package com.kunclass.channelHandler.handler;

import com.kunclass.Compress.Compressor;
import com.kunclass.Compress.CompressorFactory;
import com.kunclass.KunrpcBootstrap;
import com.kunclass.Serialize.Serializer;
import com.kunclass.Serialize.SerializerFactory;
import com.kunclass.transport.message.KunrpcRequest;
import com.kunclass.transport.message.MessageFormatConstant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;


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
        byteBuf.writeByte(kunrpcRequest.getSerializeType());
        byteBuf.writeByte(kunrpcRequest.getCompressType());
        byteBuf.writeByte(kunrpcRequest.getRequestType());
        //请求id--用8字节表示
        byteBuf.writeLong(kunrpcRequest.getRequestId());
        byteBuf.writeLong(kunrpcRequest.getTimeStamp());
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
        //1.根据配置的序列化方式进行序列化
        //怎么实现序列化
        //1.使用工具类 耦合性高 不方便替换序列化的方式
        byte[] bodyBytes = null;
        if(kunrpcRequest.getRequestPayload()!=null){
            Serializer serializer = SerializerFactory.getSerializerWrapper(kunrpcRequest.getSerializeType()).getSerializer();
            bodyBytes =serializer.serialize(kunrpcRequest.getRequestPayload());

            //2.根据配置的压缩方式进行压缩

            Compressor compressor = CompressorFactory.getCompressorWrapper(kunrpcRequest.getCompressType()).getCompressor();
            bodyBytes = compressor.compress(bodyBytes);
        }
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
}
