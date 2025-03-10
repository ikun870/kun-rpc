package com.kunclass;

import com.kunapi.HelloKunrpc;
import com.kunclass.core.HeartbeatDetector;
import com.kunclass.discovery.RegistryConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;


@Slf4j
public class ConsumerApplication {
    public static void main(String[] args) {
        // 想尽一切办法获取代理对象，使用referenceConfig进行封装
        //referenceConfig一定有生成代理对象的方法，get()
        ReferenceConfig<HelloKunrpc> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterfaceRef(HelloKunrpc.class);


        //代理做了什么
        //1.连接注册中心 2.拉取服务列表 3.选择一个服务并建立连接 4.发送请求，携带一些信息（接口名、参数列表、方法的名字）、获得结果
        KunrpcBootstrap.getInstance()
                .application("first-kunrpc-consumer")
                .registry(new RegistryConfig("zookeeper://localhost:2181"))
                .serializer("Hessian")//Hessian:222对比jdk:390的序列化后的字节长度
                .compressor("Gzip")//Gzip:222对比不压缩:390的序列化后的字节长度
                .reference(referenceConfig);

        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        // 获取一个代理对象
        HelloKunrpc helloKunrpc = referenceConfig.get();
        for(int i = 0; i < 10; i++) {
            String sayHi = helloKunrpc.sayHi("kunrpc");
            log.info("sayHi:{}", sayHi);
        }
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        for(int i = 0; i < 10; i++) {
            String sayHi = helloKunrpc.sayHi("kunrpc");
            log.info("sayHi:{}", sayHi);
        }
//        String sayHi = helloKunrpc.sayHi("kunrpc");
//        log.info("sayHi:{}", sayHi);


    }
}
