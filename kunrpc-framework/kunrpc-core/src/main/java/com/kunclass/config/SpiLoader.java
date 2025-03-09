package com.kunclass.config;

import com.kunclass.Compress.Compressor;
import com.kunclass.Compress.CompressorFactory;
import com.kunclass.Serialize.Serializer;
import com.kunclass.Serialize.SerializerFactory;
import com.kunclass.loadBalancer.LoadBalancer;
import com.kunclass.spi.SpiHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 这个主要是处理configuration中是类的成员变量，并且类是要实现了某一个接口的
 * 所以就只写了一个LoadBalancer
 */
@Slf4j
public class SpiLoader {

    /**
     * 从 SPI 中读取配置项
     * spi文件中配置了很多实现（自由定义，只能配置一个实现，还是多个）
     * @param configuration 配置类
     */
    public void loadFromSpi(Configuration configuration) {

        List<ObjectWrapper<LoadBalancer>> loadBalancerWrappers = SpiHandler.getList(LoadBalancer.class);
        if (loadBalancerWrappers == null ||loadBalancerWrappers.isEmpty()) {
            log.info("LoadBalancer is null, use default LoadBalancer:" + configuration.getLoadBalancer() + "!");
        }
        else configuration.setLoadBalancer(loadBalancerWrappers.get(0).getObjectImpl());

        // 其他配置项
        // 这里可以添加其他的 SPI 配置项
        // 例如：Compressor、IdGenerator 等等
        List<ObjectWrapper<Compressor>> compressorWrappers = SpiHandler.getList(Compressor.class);
        if (compressorWrappers == null || compressorWrappers.isEmpty()) {
            log.info("Compressor is null, use default Compressor:" + configuration.getCompressType() + "!");
        }
        else {
            compressorWrappers.forEach(CompressorFactory::addCompressor);
        }

        // 序列化器
        List<ObjectWrapper<Serializer>> serializerWrappers = SpiHandler.getList(Serializer.class);
        if (serializerWrappers == null || serializerWrappers.isEmpty()) {
            log.info("Serializer is null, use default Serializer:" + configuration.getSerializeType() + "!");
        }
        else {
            serializerWrappers.forEach(SerializerFactory::addSerializer);
        }

    }

}
