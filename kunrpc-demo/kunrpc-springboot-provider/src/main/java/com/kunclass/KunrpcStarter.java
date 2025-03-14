package com.kunclass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 *KunrpcStarter 类实现了 CommandLineRunner 接口，这意味着它会在 Spring Boot 应用启动后执行。
 * 具体来说，它会在应用启动后延迟 5 秒，
 * 然后启动 KunrpcBootstrap 实例，并扫描指定包中的类。
 */
@Component
@Slf4j
public class KunrpcStarter implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        //延迟5秒启动
        Thread.sleep(5000);
        log.info("kunrpc启动…………");
        KunrpcBootstrap.getInstance()
                //.application("first-kunrpc-provider")
                //配置注册中心，包含创建zookeeper实例
                //.registry(new RegistryConfig("zookeeper://localhost:2181"))
                //协议
                //.serializer("jdk")
                //发布服务到对应的zookeeper上
                //.publish(service)
                //扫包批量发布
                //注释的这些东西没有必要写在这里，都在配置Configuration类里面了
                .scan("com.kunclass.impl")
                .start();
    }
}
