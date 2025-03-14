package com.kunclass.Compress.impl;

import com.kunclass.Compress.Compressor;
import com.kunclass.exceptions.CompressException;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class GzipCompressor implements Compressor {
    /**
     * 使用gzip算法对字节数组进行压缩
     * @param origin
     * @return 压缩后的字节数组
     */
    @Override
    public byte[] compress(byte[] origin) {

        try(ByteArrayOutputStream out = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(out))
        {
            gzip.write(origin);
            gzip.finish();
            byte[] compressed = out.toByteArray();
            if(log.isDebugEnabled()){
                log.debug("使用Gzip算法对字节数组进行压缩成功,压缩前的字节数组长度为{},压缩后的字节数组长度为{}",origin.length,compressed.length);
            }
            return compressed;
        } catch (IOException e) {
            log.error("对字节数组进行压缩时发生异常",e);
            throw new CompressException(e);
        }

    }

    /**
     * 使用gzip算法对字节数组进行解压缩
     * @param compressed
     * @return 解压缩后的字节数组
     */
    @Override
    public byte[] decompress(byte[] compressed) {
        try(ByteArrayInputStream in = new ByteArrayInputStream(compressed);
            GZIPInputStream gzipInputStream = new GZIPInputStream(in))
        {
            byte[] origin = gzipInputStream.readAllBytes();
            if(log.isDebugEnabled()){
               log.debug("使用Gzip算法对字节数组进行解压缩成功,解压缩前的字节数组长度为{},解压缩后的字节数组长度为{}",compressed.length,origin.length);
            }
            return origin;
        } catch (IOException e) {
            log.error("对字节数组进行解压缩时发生异常",e);
            throw new CompressException(e);
        }
    }
}
