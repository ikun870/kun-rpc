package com.kunclass.Serialize.impl;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.kunclass.Serialize.Serializer;
import com.kunclass.exceptions.DeserializerException;
import com.kunclass.exceptions.SerializerException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * Hessian序列化器
 */
@Slf4j
public class HessianSerializer implements Serializer {
    @Override
    public byte[] serialize(Object object) {
        //针对不同的消息类型需要做不同的处理，心跳的请求，没有payload
        if(object==null){
            return null;
        }

        try(
            //放在try中，可以自动关闭流
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();)
        {

            Hessian2Output outputStream = new Hessian2Output(byteArrayOutputStream);
            outputStream.writeObject(object);
            outputStream.flush();

            if(log.isDebugEnabled()){
                log.debug("使用Hessian序列化{}对象成功,序列化后的字节长度为{}", object.getClass().getSimpleName(),byteArrayOutputStream.toByteArray().length);
            }

            return byteArrayOutputStream.toByteArray();
        }
        catch (IOException e){
            log.error("使用Hessian序列化{}时发生异常", object.getClass().getSimpleName());
            throw new SerializerException("序列化时发生异常");
        }

    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if(bytes==null||clazz==null){
            return null;
        }

        try(ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes))
        {
             Hessian2Input inputStream = new Hessian2Input(byteArrayInputStream);

            if(log.isDebugEnabled()){
                log.debug("使用Hessian反序列化{}类成功", clazz.getName());
            }

            return (T) inputStream.readObject();
        }
        catch (IOException e){
            log.error("使用Hessian反序列化{}时发生异常", clazz.getName());
            throw new DeserializerException("反序列化时发生异常");
        }
    }
}
