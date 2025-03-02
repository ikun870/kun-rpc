package com.kunclass.enumeration;

/**
 * 用来标记请求的类型
 */
public enum RequestType {

    REQUEST((byte) 1, "Normal request"),
    HEARTBEAT((byte) 2, "heartbeat");

    private byte id;
    private String type;

    public byte getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    RequestType(byte b, String request) {
    }
}
