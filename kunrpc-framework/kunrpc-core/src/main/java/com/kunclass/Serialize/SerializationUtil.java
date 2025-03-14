package com.kunclass.Serialize;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

@Slf4j
public class SerializationUtil {

    public static byte[] serialize(Object object) {
        //针对不同的消息类型需要做不同的处理，心跳的请求，没有payload
        if(object==null){
            return null;
        }
        //希望可以通过一些设计模式，面向对象的编程，让我们可以配置修改序列化和压缩的方式
        //对象变成一个字节数组 序列化 压缩
        try{
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream);
            outputStream.writeObject(object);
            byte[] bytes = byteArrayOutputStream.toByteArray();
            return bytes;
        }
        catch (IOException e){
            log.error("序列化时发生异常");
            throw new RuntimeException("序列化时发生异常");
        }

    }
}
