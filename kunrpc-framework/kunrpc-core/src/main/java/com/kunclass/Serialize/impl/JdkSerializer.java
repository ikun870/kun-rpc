package com.kunclass.Serialize.impl;
import com.kunclass.Serialize.Serializer;
import com.kunclass.exceptions.DeserializerException;
import com.kunclass.exceptions.SerializerException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class JdkSerializer implements Serializer {
    @Override
    public byte[] serialize(Object object) {
        //针对不同的消息类型需要做不同的处理，心跳的请求，没有payload
        if(object==null){
            return null;
        }
        //希望可以通过一些设计模式，面向对象的编程，让我们可以配置修改序列化和压缩的方式
        //对象变成一个字节数组 序列化 压缩
        try(
            //将流的定义放在try中，可以自动关闭流
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream);){

            outputStream.writeObject(object);

            if(log.isDebugEnabled()){
                log.debug("序列化{}对象成功,序列化后的字节数为{}", object.getClass().getSimpleName(), byteArrayOutputStream.toByteArray().length);
            }

            return byteArrayOutputStream.toByteArray();
        }
        catch (IOException e){
            log.error("序列化{}时发生异常", object.getClass().getSimpleName());
            throw new SerializerException("序列化时发生异常");
        }

    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if(bytes==null||clazz==null){
            return null;
        }

        try(ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            ObjectInputStream inputStream = new ObjectInputStream(byteArrayInputStream);){

            if(log.isDebugEnabled()){
                log.debug("反序列化{}类成功", clazz.getName());
            }

            return (T) inputStream.readObject();
        }
        catch (IOException | ClassNotFoundException e){
            log.error("反序列化{}时发生异常", clazz.getName());
            throw new DeserializerException("反序列化时发生异常");
        }
    }
}
