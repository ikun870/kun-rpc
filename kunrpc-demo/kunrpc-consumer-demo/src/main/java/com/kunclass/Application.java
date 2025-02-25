package com.kunclass;

import com.kunapi.HelloKunrpc;


import java.lang.ref.Reference;

public class Application {
    public static void main(String[] args) {
        // 想尽一切办法获取代理对象，使用referenceConfig进行封装
        //referenceConfig一定有生成代理对象的方法，get()
        ReferenceConfig<HelloKunrpc> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(HelloKunrpc.class);


        //代理做了什么
        //1.连接注册中心 2.拉取服务列表 3.选择一个服务并建立连接 4.发送请求，携带一些信息（接口名、参数列表、方法的名字）、获得结果
        KunrpcBootstrap.getInstance()
                .application("first-kunrpc-consumer")
                .registry(new RegistryConfig("zookeeper://localhost:2181"))
                .reference(referenceConfig);

        // 获取一个代理对象
        HelloKunrpc helloKunrpc = referenceConfig.get();
        helloKunrpc.sayHi("kunrpc");

    }
}
