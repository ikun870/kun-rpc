package com.kunclass.Serialize;

import com.kunclass.Serialize.impl.HessianSerializer;
import com.kunclass.Serialize.impl.JdkSerializer;
import com.kunclass.Serialize.impl.JsonSerializer;
import com.kunclass.config.ObjectWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SerializerFactory {

    private final static ConcurrentHashMap<String, ObjectWrapper<Serializer>> SERIALIZER_CACHE = new ConcurrentHashMap<>(8);
    private final static ConcurrentHashMap<Byte,ObjectWrapper<Serializer>> SERIALIZER_CACHE_CODE = new ConcurrentHashMap<>(8);

    static {
        ObjectWrapper<Serializer> jdk = new ObjectWrapper<>((byte) 1, "jdk", new JdkSerializer());
        ObjectWrapper<Serializer> json = new ObjectWrapper<>((byte) 2, "json", new JsonSerializer());
        ObjectWrapper<Serializer> hessian = new ObjectWrapper<>((byte) 3, "hessian", new HessianSerializer());
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
    public static ObjectWrapper<Serializer> getSerializerWrapper(String serializerName) {
        if(SERIALIZER_CACHE.get(serializerName)==null){
            if(log.isInfoEnabled()){
                log.info("serializerName:{} is not found, use default serializer:JDK",serializerName);
            }
            return SERIALIZER_CACHE.get("jdk");
        }

        return SERIALIZER_CACHE.get(serializerName);
    }

    public static ObjectWrapper<Serializer> getSerializerWrapper(byte code) {
        if(SERIALIZER_CACHE_CODE.get(code)==null){
            if(log.isInfoEnabled()){
                log.info("code:{} is not found, use default serializer:JDK",code);
            }
            return SERIALIZER_CACHE_CODE.get((byte)1);
        }

        return SERIALIZER_CACHE_CODE.get(code);
    }

    /**
     * 添加序列化器
     * @param serializerWrapper 序列化器包装类
     */
    public static void addSerializer(ObjectWrapper<Serializer> serializerWrapper) {
        SERIALIZER_CACHE.put(serializerWrapper.getName(),serializerWrapper );
        SERIALIZER_CACHE_CODE.put(serializerWrapper.getCode(), serializerWrapper);
    }
}
