package com.kunclass.Compress;

public interface Compressor {

    /**
     * 对字节数组进行压缩
     * @param bytes
     * @return 压缩后的字节数组
     */
    byte[] compress(byte[] bytes) ;


    /**
     * 对字节数组进行解压
     * @param bytes
     * @return 解压后的字节数组
     */
    byte[] decompress(byte[] bytes) ;

}
