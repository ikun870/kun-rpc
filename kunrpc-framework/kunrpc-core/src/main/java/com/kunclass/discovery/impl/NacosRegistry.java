package com.kunclass.discovery.impl;

import com.kunclass.Constant;
import com.kunclass.ServiceConfig;
import com.kunclass.discovery.AbstractRegistry;
import com.kunclass.utils.NetUtils;
import com.kunclass.utils.zookeeper.ZookeeperNode;
import com.kunclass.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.net.InetSocketAddress;

@Slf4j
public class NacosRegistry extends AbstractRegistry {

    private ZooKeeper zooKeeper ;

    public NacosRegistry() {
        this.zooKeeper = ZookeeperUtils.createZooKeeper();
    }
    public NacosRegistry(String connectString, int timeout) {
        this.zooKeeper = ZookeeperUtils.createZooKeeper(connectString,timeout);
    }

    @Override
    public void register(ServiceConfig<?> serviceConfig) {
        //1.将服务的信息注册到zookeeper上
        //服务名称的节点,使用路径来表示
        String parentNode = Constant.BASE_PROVIDERS_PATH+"/"+serviceConfig.getInterface().getName();
        //这个节点应该是一个持久节点
        if(!ZookeeperUtils.exists(zooKeeper,parentNode,null)) {
            ZookeeperUtils.createNode(zooKeeper,new ZookeeperNode(parentNode,null),null, CreateMode.PERSISTENT);
        }
        //在这个节点下，创建一个临时节点，节点的数据是当前服务的地址.服务提供方的端口一般自己设定，我们还需要一个获取ip的方法。ip通常是一个局域网ip（也非ipv6）
        //TODO:这里的端口号应该是从配置文件中获取
        String node = parentNode+"/"+ NetUtils.getIp()+":"+8088;
        if(!ZookeeperUtils.exists(zooKeeper,node,null)) {
            ZookeeperUtils.createNode(zooKeeper,new ZookeeperNode(node,null),null, CreateMode.EPHEMERAL);
        }
        log.debug("serviceConfig:服务{}已经被注册", serviceConfig.getInterface().getName());

    }

    @Override
    public InetSocketAddress lookup(String name) {
        return null;
    }
}
