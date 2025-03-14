package com.kunclass.discovery;

import com.kunclass.Constant;
import com.kunclass.discovery.impl.NacosRegistry;
import com.kunclass.discovery.impl.ZookeeperRegistry;
import com.kunclass.exceptions.DiscoveryException;

//@Data
public class RegistryConfig {
    //定义注册中心的地址： zookeeper://localhost:2181  redis://192.168.12.125:6379
    private String connectString;

    public RegistryConfig(String connectString) {
        this.connectString = connectString;
    }

    //可以使用简单工厂来完成
    public Registry getRegistry() {
        //1.获取注册中心的类型
        String registryType = getRegistryType(connectString,true).toLowerCase().trim();
        //2.根据注册中心的类型，创建对应的注册中心实例（其中需要创建zookeeper实例）
        if(registryType.equals("zookeeper")) {
            String host = getRegistryType(connectString,false);
            return new ZookeeperRegistry(host, Constant.DEFAULT_Zk_TIMEOUT);
        } else if (registryType.equals("nacos")) {
            //return new RedisRegistry();
            String host = getRegistryType(connectString,false);
            return new NacosRegistry(host, Constant.DEFAULT_Zk_TIMEOUT);
        }
        throw new DiscoveryException("不支持的注册中心类型!");
    }

    private String getRegistryType(String connectString,boolean getType) {
        //zookeeper://localhost:2181
        //redis://
        String[] typeAndHost = connectString.split("://");
        if(typeAndHost.length !=2) {
            throw new IllegalArgumentException("注册中心地址不合法");
        }
        if(getType) {
            return typeAndHost[0];
        }
        return typeAndHost[1];
    }
}
