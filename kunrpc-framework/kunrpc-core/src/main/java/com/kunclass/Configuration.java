package com.kunclass;

import com.kunclass.discovery.Registry;
import com.kunclass.discovery.RegistryConfig;
import com.kunclass.loadBalancer.LoadBalancer;
import com.kunclass.loadBalancer.impl.RoundRobinLoadBalancer;
import lombok.Data;

/**
 * 全局的配置类：几种配置方式
 * 代码配置
 * xml配置文件
 * spi配置
 * 默认项
 */
@Data
public class Configuration {
    //定义一些相关的基础配置

    //应用程序名称
    private String appName = "default";
    //端口号
    private  final int port = 8089;
    //注册中心
    private RegistryConfig registryConfig;
    //序列化协议
    private ProtocolConfig protocolConfig;
    //id生成器
    private  final IdGenerator idGenerator = new IdGenerator(1, 2);
    //序列化协议
    private  String serializeType = "jdk";
    //压缩方式
    private  String compressType = "gzip";
    //负载均衡器
    private  LoadBalancer loadBalancer = new RoundRobinLoadBalancer();

    //读xml
    public Configuration() {
        //读取xml配置文件
    }

    //代码配置由引导程序完成

    //读properties

}
