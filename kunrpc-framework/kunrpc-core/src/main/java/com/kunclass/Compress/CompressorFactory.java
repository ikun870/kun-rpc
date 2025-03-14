package com.kunclass.Compress;

import com.kunclass.Compress.impl.GzipCompressor;
import com.kunclass.config.ObjectWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 压缩器工厂类
 * 用于获取压缩器包装类
 */
@Slf4j
public class CompressorFactory {

    private final static ConcurrentHashMap<String, ObjectWrapper<Compressor>> COMPRESSOR_CACHE = new ConcurrentHashMap<>(8);
    private final static ConcurrentHashMap<Byte, ObjectWrapper<Compressor>> COMPRESSOR_CACHE_CODE = new ConcurrentHashMap<>(8);

    static {
        ObjectWrapper<Compressor> gzip = new ObjectWrapper<>((byte) 1, "gzip", new GzipCompressor());
        COMPRESSOR_CACHE.put("gzip",gzip);
        COMPRESSOR_CACHE_CODE.put((byte)1,gzip);
    }

    /**
     * 使用工厂方法获取一个CompressorWrapper
     * @param compressorName
     * @return CompressorWrapper 压缩器包装类
     */
    public static ObjectWrapper<Compressor> getCompressorWrapper(String compressorName) {

        if(COMPRESSOR_CACHE.get(compressorName)==null){
            log.info("compressorName:{} is not found, use default compressor:GZIP",compressorName);
            return COMPRESSOR_CACHE.get("gzip");
        }

        return COMPRESSOR_CACHE.get(compressorName);
    }

    public static ObjectWrapper<Compressor> getCompressorWrapper(byte code) {

        if(COMPRESSOR_CACHE_CODE.get(code)==null){
            log.info("code:{} is not found, use default compressor:GZIP",code);
            return COMPRESSOR_CACHE_CODE.get((byte)1);
        }

        return COMPRESSOR_CACHE_CODE.get(code);
    }

    /**
     * 添加压缩器
     * @param compressorWrapper 压缩器包装类
     */
    public static void addCompressor(ObjectWrapper<Compressor> compressorWrapper) {
        COMPRESSOR_CACHE.put(compressorWrapper.getName(),compressorWrapper);
        COMPRESSOR_CACHE_CODE.put(compressorWrapper.getCode(),compressorWrapper);
    }
}
