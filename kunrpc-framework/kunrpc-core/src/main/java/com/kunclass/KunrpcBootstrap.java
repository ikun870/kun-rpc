package com.kunclass;

import com.kunclass.discovery.Registry;
import com.kunclass.discovery.RegistryConfig;
import lombok.extern.slf4j.Slf4j;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class KunrpcBootstrap {



    //KunrpcBootstrap是一个单例，我们希望每个应用程序都只有一个实例
    private static final KunrpcBootstrap kunrpcBootstrap = new KunrpcBootstrap();

    //定义一些相关的基础配置
    private String appName = "default";
    private RegistryConfig registryConfig;
    private ProtocolConfig protocolConfig;
    private int port = 8088;
    private Registry registry;

    //维护已经发布且暴露的服务列表 ，key--》interface的全限定名，value--》serviceConfig
    private static final Map<String,ServiceConfig<?>> SERVICES_LIST = new ConcurrentHashMap<>(16);

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
        this.appName = appName;
        return  kunrpcBootstrap;
    }

    /**
     * 用来配置一个注册中心
     * @param registryConfig 注册中心配置
     * @return this 当前实例
     */
    public KunrpcBootstrap registry(RegistryConfig registryConfig) {
        //这里维护一个zookeeper实例，但是这样写就会将zooKeeper和当前工程耦合
        //我们更希望以后可以扩展更多种不同的实现

        //尝试使用registryConfig来获取注册中心，类似于工厂模式
        this.registry = registryConfig.getRegistry();
        return this;
       // return kunrpcBootstrap;
    }

    /**
     * 用来配置一个协议
     * @param protocolConfig
     * @return
     */
    public KunrpcBootstrap protocol(ProtocolConfig protocolConfig) {
        this.protocolConfig = protocolConfig;
        log.debug("protocolConfig:{}", protocolConfig.toString());
        return this;
    }
    /**
     * ——————————————————服务提供方的api————————————————————————————————
     */


    /**
     * 用来发布一个服务,将接口以及实现，注册到服务中心
     * @param service 独立封装的需要发布的服务
     * @return
     */
    public KunrpcBootstrap publish(ServiceConfig<?> service) {
        //创建zookeeper
        //注册service，我们抽象了注册中心的概念，使用注册中心的一个实现完成注册
        //这里难道不是强耦合了吗？是的

        registry.register(service);

        //1.当服务调用方通过接口、方法名、具体的方法参数列表发起调用时，服务提供方怎么根据这些信息找到对应的服务实现类，然后调用对应的方法
        //（1）new一个  （2）spring beanFactory.getBean(Class)  （3）自己维护映射关系 ✔️✔️
        SERVICES_LIST.put(service.getInterface().getName(),service);
        return this;
    }

    /**
     * 用来发布多个服务
     * @param services 独立封装的需要发布的服务
     * @return
     */
    public KunrpcBootstrap publish(List<ServiceConfig<?>> services) {
        for(ServiceConfig<?> service : services) {
            publish(service);
        }
        return this;
    }

    /**
     * 启动引导程序
     */
    public void start() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
