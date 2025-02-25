package com.kunclass;


import com.kunapi.HelloKunrpc;
import com.kunclass.impl.HelloKunrpcImpl;

public class Application {
    public static void main(String[] args) {
        //服务提供方需要注册服务，启动服务
        //1.封装要发布的服务
        ServiceConfig<HelloKunrpc> service = new ServiceConfig<>();
        service.setInterface(HelloKunrpc.class);
        service.setRef(new HelloKunrpcImpl());
        //2.定义注册中心

        //3.通过启动引导程序，启动服务提供方
          //1.配置 ---应用的名称 --注册中心的地址 --（序列化协议） --（压缩方式）
          //2.发布服务
        KunrpcBootstrap.getInstance()
                .application("first-kunrpc-provider")
                //配置注册中心
                .registry(new RegistryConfig("zookeeper://localhost:2181"))
                //协议
                .protocol(new ProtocolConfig("jdk"))
                //发布服务
                .publish(service)
                .start();
    }
}
