package com.kunclass;

import lombok.extern.slf4j.Slf4j;

import java.rmi.registry.Registry;
import java.util.List;
import java.util.logging.Handler;

@Slf4j
public class KunrpcBootstrap {



    //KunrpcBootstrap是一个单例，我们希望每个应用程序都只有一个实例
    private static KunrpcBootstrap kunrpcBootstrap = new KunrpcBootstrap();

    //构造函数私有化
    private KunrpcBootstrap() {
    //构造启动引导程序，需要做一些初始化工作

    }

    public static KunrpcBootstrap getInstance() {

        return kunrpcBootstrap;
    }

    /**
     * 用来定义当前应用的名字
     * @param appName 应用名
     * @return this
     */
    public KunrpcBootstrap application(String appName) {

        return  kunrpcBootstrap;
    }

    /**
     * 用来配置一个注册中心
     * @param registryConfig 注册中心配置
     * @return this 当前实例
     */
    public KunrpcBootstrap registry(RegistryConfig registryConfig) {
        return this;
       // return kunrpcBootstrap;
    }

    /**
     * 用来配置一个协议
     * @param protocolConfig
     * @return
     */
    public KunrpcBootstrap protocol(ProtocolConfig protocolConfig) {
        log.debug("protocolConfig:{}", protocolConfig.toString());
        return this;
    }
    /**
     * ——————————————————服务提供方的api————————————————————————————————
     */


    /**
     * 用来发布一个服务,将接口以及实现，注册到服务中心
     * @param serviceConfig 独立封装的需要发布的服务
     * @return
     */
    public KunrpcBootstrap publish(ServiceConfig<?> serviceConfig) {
        log.debug("serviceConfig:{}", serviceConfig.toString());
        return this;
    }

    /**
     * 用来发布多个服务
     * @param serviceConfig 独立封装的需要发布的服务
     * @return
     */
    public KunrpcBootstrap publish(List<ServiceConfig<?>> serviceConfig) {
        return this;
    }

    /**
     * 启动引导程序
     * @return this
     */
    public KunrpcBootstrap start() {
        return this;
    }


    /**
     * ——————————————————服务调用方的api————————————————————————————————
     */
    public KunrpcBootstrap reference(ReferenceConfig<?> referenceConfig) {
        //在这个方法里，我们是否可以拿到相关的配置项-注册中心
        //配置reference,将来调用get方法时，方便生成代理对象

        return this;
    }

}
