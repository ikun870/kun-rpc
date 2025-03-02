package com.kunclass.Serialize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 序列化器包装类
 * 是为了将序列化器映射到一个byte类型的code上
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SerializerWrapper {
    private byte code;
    private String type;
    private Serializer serializer;
}
