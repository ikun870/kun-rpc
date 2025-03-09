package com.kunclass.Serialize;

import com.kunclass.config.ObjectWrapper;

/**
 * 序列化接口
 * 用于序列化和反序列化
 *
 */

public interface Serializer {
    /**
     * 序列化
     * @param object 要序列化的对象
     * @return 序列化后的字节数组
     */
    //序列化
    byte[] serialize(Object object);

    //反序列化

    /**
     * 反序列化
     * @param bytes 字节数组
     * @param clazz 要反序列化成的类
     * @return 反序列化后的对象
     * @param <T> 泛型
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);


}
