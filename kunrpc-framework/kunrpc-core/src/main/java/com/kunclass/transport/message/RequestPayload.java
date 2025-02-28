package com.kunclass.transport.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用来描述请求调用方所请求的接口方法的描述
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestPayload {

    //1.接口的名字 com.kunapi.HelloKunrpc
    private String interfaceName;

    //2.方法名 sayHi
    private String methodName;

    //3.方法的参数列表：参数类型和具体的参数
    //参数类型用来确定重载方法，具体的参数用来确定调用哪个方法
    private Class<?>[] parameterTypes;// --{java.lang.String}
    private Object[] parameters;//-- "你好“

    //4.返回值的封装
    private Class<?> returnType;//--java.lang.String

}
