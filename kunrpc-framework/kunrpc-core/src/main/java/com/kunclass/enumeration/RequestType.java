package com.kunclass.enumeration;

import lombok.Getter;

/**
 * 用来标记请求的类型
 */
@Getter
public enum RequestType {

    REQUEST((byte) 1, "Normal request"),
    HEARTBEAT((byte) 2, "heartbeat");

    private byte id;
    private String type;

    RequestType(byte b, String request) {
        this.id = b;
        this.type = request;
    }
}
