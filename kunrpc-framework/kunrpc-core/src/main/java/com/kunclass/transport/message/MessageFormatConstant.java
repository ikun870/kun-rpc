package com.kunclass.transport.message;

public class MessageFormatConstant {
    //value，存的值
    public static final byte[] MAGIC_NUMBER = "krpc".getBytes();
    public static final byte VERSION = 1;
        //头长度= 4 + 1 + 2 + 4 + 1 + 1 + 1 + 8 = 22,因为
    public static final short HEAD_LENGTH = (byte) (MAGIC_NUMBER.length + 1 + 2 + 4 + 1 + 1 + 1 + 8);

    //length,参考报文格式
    public static final int MAX_FRAME_LENGTH = 1024*1024;
    public static final int VERSION_LENGTH = 1;
    public static final int HEAD_FIELD_LENGTH = 2;
    public static final int FULL_FIELD_LENGTH = 4;
}
