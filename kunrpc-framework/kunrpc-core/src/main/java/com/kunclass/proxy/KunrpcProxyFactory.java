package com.kunclass.proxy;

import com.kunclass.KunrpcBootstrap;
import com.kunclass.ReferenceConfig;
import com.kunclass.discovery.RegistryConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KunrpcProxyFactory {

    public static Map<Class<?>,Object> cache  = new ConcurrentHashMap<>(16);

    public static <T> T getProxy(Class<T> interfaceClass) {

        Object bean = cache.get(interfaceClass);
        if (bean != null) {
            return (T) bean;
        }

        ReferenceConfig<T> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterfaceRef(interfaceClass);

        KunrpcBootstrap.getInstance()
                .application("first-kunrpc-consumer")
                .registry(new RegistryConfig("zookeeper://localhost:2181"))
                .serializer("Hessian")//Hessian:222对比jdk:390的序列化后的字节长度
                .compressor("Gzip")//Gzip:222对比不压缩:390的序列化后的字节长度
                .group("primary")
                .reference(referenceConfig);

        T t = referenceConfig.get();
        cache.put(interfaceClass,t);

        return t;
    }
}
