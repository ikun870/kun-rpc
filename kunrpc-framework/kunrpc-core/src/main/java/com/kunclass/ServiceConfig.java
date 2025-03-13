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

    //服务分组
    private String group = "default";

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

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
