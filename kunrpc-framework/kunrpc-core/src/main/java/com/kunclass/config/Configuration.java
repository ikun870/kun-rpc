package com.kunclass.config;

import com.kunclass.IdGenerator;
import com.kunclass.discovery.RegistryConfig;
import com.kunclass.loadBalancer.LoadBalancer;
import com.kunclass.loadBalancer.impl.RoundRobinLoadBalancer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * 全局的配置类：几种配置方式
 * 代码配置
 * KunrpcBootstrap.getInstance()
 *                 .application("first-kunrpc-consumer")
 *                 .registry(new RegistryConfig("zookeeper://localhost:2181"))
 *                 .serializer("Hessian")
 *                 .compressor("Gzip")
 *                 .reference(referenceConfig);
 *
 * xml配置文件
 * configuration.setAppName(appName);
 * configuration.setPort(port);
 * configuration.setRegistryConfig(parseConfig("com.kunclass.discovery.RegistryConfig",new Class[]{String.class},registryUrl)
 * configuration.setIdGenerator(parseConfig("com.kunclass.IdGenerator",new Class[]{long.class,long.class},params));
 * configuration.setSerializeType(serializeType);
 * configuration.setCompressType(compressType);
 *
 * spi配置
 * configuration.setLoadBalancer(loadBalancer);
 *
 * 默认项
 * configuration中的成员变量设置
 *
 * spi 特殊的格式 code+type+xml --》objectWrapper ---》统一放入工厂
 * xml
 *
 */
@Data
@Slf4j
public class Configuration {
    //定义一些相关的基础配置

    //应用程序名称
    private String appName = "default";
    //端口号
    private int port = 8089;
    //注册中心
    private RegistryConfig registryConfig = new RegistryConfig("zookeeper://localhost:2181");
    //id生成器
    private IdGenerator idGenerator = new IdGenerator(1, 2);
    //序列化协议
    private  String serializeType = "jdk";
    //压缩方式
    private  String compressType = "gzip";
    //负载均衡器
    private  LoadBalancer loadBalancer = new RoundRobinLoadBalancer();


    //读xml，dom4j
    public Configuration() {
        //使用默认配置项

        //spi机制发现相关配置项
        //new SpiLoader().loadFromSpi(this);


        //读取xml配置文件
        new XmlLoader().loadFromXml(this);

        //编程配置项，kunrpcBootstrap提供

    }

    public static void main(String[] args) {
        Configuration configuration = new Configuration();
        System.out.println(configuration.loadBalancer.getClass().getName());
    }



}
