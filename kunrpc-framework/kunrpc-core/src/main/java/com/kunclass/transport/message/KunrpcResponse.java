package com.kunclass.transport.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *服务提供方返回的结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KunrpcResponse {

    // 请求id
    private long requestId;

    //压缩的类型，序列化的方式
    private byte compressType;
    private byte serializeType;

    private Long timeStamp;

    //1,成功    2，异常
    private byte code;

    //具体的消息体
    private Object body;

}
