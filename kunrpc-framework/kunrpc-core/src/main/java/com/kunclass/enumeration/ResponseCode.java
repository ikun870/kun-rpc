package com.kunclass.enumeration;

import lombok.Data;
import lombok.Getter;

/**
 * 初始化方式：枚举常量SUCCESS/FAIL在类加载时自动实例化，
 * 通过构造参数(byte)1,"Success"初始化字段
 */
@Getter
public enum ResponseCode {

    SUCCESS((byte) 1, "Success"),
    FAIL((byte) 2, "Fail");

    private byte code;
    private String desc;

    //因为没有在这里写构造方法，导致取到的RequestType始终为0
    ResponseCode(byte code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
