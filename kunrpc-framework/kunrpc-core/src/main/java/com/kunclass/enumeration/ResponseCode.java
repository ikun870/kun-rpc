package com.kunclass.enumeration;

import lombok.Data;
import lombok.Getter;

@Getter
public enum ResponseCode {

    SUCCESS((byte) 1, "Success"),
    FAIL((byte) 2, "Fail");

    private byte code;
    private String desc;

    ResponseCode(byte code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
