package com.kunclass.Serialize.impl;

import com.alibaba.fastjson2.JSON;
import com.kunclass.Serialize.Serializer;
import com.kunclass.exceptions.DeserializerException;
import com.kunclass.exceptions.SerializerException;
import com.kunclass.transport.message.RequestPayload;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * Json序列化器
 * 具有局限性，只能序列化字符串
 */
@Slf4j
public class JsonSerializer implements Serializer {
    @Override
    public byte[] serialize(Object object) {
        //针对不同的消息类型需要做不同的处理，心跳的请求，没有payload
        if(object==null){
            return null;
        }
        byte[] jsonBytes = JSON.toJSONBytes(object);

        if(log.isDebugEnabled()){
            log.debug("序列化{}对象成功,序列化后的字节数为{}", object.getClass().getSimpleName(), jsonBytes.length);
        }

            return jsonBytes;
        }


    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if(bytes==null||clazz==null){
            return null;
        }
        T t = JSON.parseObject(bytes, clazz);
        if(log.isDebugEnabled()){
            log.debug("反序列化{}类成功", clazz.getName());
        }
        return t;

    }

    public static void main(String[] args) {
        Serializer serializer = new JsonSerializer();
        RequestPayload requestPayload = new RequestPayload();

        requestPayload.setInterfaceName("com.kunclass.service.HelloService");
        requestPayload.setMethodName("sayHi");
        //Json只能处理字符串

        byte[] bytes = serializer.serialize(requestPayload);
        RequestPayload requestPayload1 = serializer.deserialize(bytes, RequestPayload.class);
        System.out.println(requestPayload1);
    }

}


