package com.kunclass;

/**
 * 服务配置
 * 用于封装服务的接口和实现
 * @param <T>
 */
public class ServiceConfig<T> {
    //服务接口 .class 对象
    private Class<?> interfaceProvider;
    //服务实现类
    private Object ref;

    public Class<?> getInterface() {
        return interfaceProvider;
    }

    public void setInterface(Class<?> interfaceProvider) {
        this.interfaceProvider = interfaceProvider;
    }

    public Object getRef() {
        return ref;
    }

    public void setRef(Object ref) {
        this.ref = ref;
    }
}
