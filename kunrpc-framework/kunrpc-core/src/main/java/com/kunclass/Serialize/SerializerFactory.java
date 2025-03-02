package com.kunclass.Serialize;

import com.kunclass.Serialize.impl.HessianSerializer;
import com.kunclass.Serialize.impl.JdkSerializer;
import com.kunclass.Serialize.impl.JsonSerializer;

import java.util.concurrent.ConcurrentHashMap;

public class SerializerFactory {

    private final static ConcurrentHashMap<String,SerializerWrapper> SERIALIZER_CACHE = new ConcurrentHashMap<>(8);
    private final static ConcurrentHashMap<Byte,SerializerWrapper> SERIALIZER_CACHE_CODE = new ConcurrentHashMap<>(8);

    static {
        SerializerWrapper jdk = new SerializerWrapper((byte) 1, "jdk", new JdkSerializer());
        SerializerWrapper json = new SerializerWrapper((byte) 2, "json", new JsonSerializer());
        SerializerWrapper hessian = new SerializerWrapper((byte) 3, "hessian", new HessianSerializer());
        SERIALIZER_CACHE.put("jdk",jdk);
        SERIALIZER_CACHE.put("json",json);
        SERIALIZER_CACHE.put("hessian",hessian);
        SERIALIZER_CACHE_CODE.put((byte)1,jdk);
        SERIALIZER_CACHE_CODE.put((byte)2,json);
        SERIALIZER_CACHE_CODE.put((byte)3,hessian);
    }

    /**
     * 使用工厂方法SerializerWrapper
     * @param serializerName
     * @return SerializerWrapper 序列化器包装类
     */
    public static SerializerWrapper getSerializerWrapper(String serializerName) {
        return SERIALIZER_CACHE.get(serializerName);
    }

    public static SerializerWrapper getSerializerWrapper(byte code) {
        return SERIALIZER_CACHE_CODE.get(code);
    }
}
