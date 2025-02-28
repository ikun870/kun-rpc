package com.kunclass.transport.message;

public class MessageFormatConstant {
    public static final byte[] MAGIC_NUMBER = "krpc".getBytes();
    public static final byte VERSION = 1;
    //头长度= 4 + 1 + 2 + 4 + 1 + 1 + 1 + 8 = 22,因为
    public static final short HEAD_LENGTH = (byte) (MAGIC_NUMBER.length + 1 + 2 + 4 + 1 + 1 + 1 + 8);
    public static final int FULL_LENGTH = 4 + 1 + 2 + 4 + 1 + 1 + 1 + 8;

}
