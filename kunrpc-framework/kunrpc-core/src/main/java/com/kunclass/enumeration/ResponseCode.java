package com.kunclass.enumeration;

import lombok.Getter;

/**
 *服务提供方返回的结果
 * 响应码需要做统一的处理 <255
 * 成功码 20--方法成功调用 21--心跳成功返回
 * 错误码 44--请求的方法不存在  50--服务器内部错误
 * 负载码 31--服务器负载过高，被限流
 */
@Getter
public enum ResponseCode {

    SUCCESS((byte) 20, "~~~~Success!~~~"),
    HEARTBEAT_SUCCESS((byte) 21, "~~~Heartbeat Success!~~~"),
    RATE_LIMIT((byte) 31, "~~~Rate Limit!~~~"),
    NOT_FOUND((byte) 44, "~~~Not Found!~~~"),
    SERVER_ERROR((byte) 50, "~~~Server Error!~~~"),
    CLOSING((byte) 60, "~~~Server is closing!~~~");

    private byte code;
    private String desc;

    //因为没有在这里写构造方法，导致取到的RequestType始终为0
    ResponseCode(byte code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
