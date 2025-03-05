package com.kunclass.discovery.impl;

import com.kunclass.Constant;
import com.kunclass.KunrpcBootstrap;
import com.kunclass.ServiceConfig;
import com.kunclass.discovery.AbstractRegistry;
import com.kunclass.exceptions.DiscoveryException;
import com.kunclass.exceptions.NetworkException;
import com.kunclass.utils.NetUtils;
import com.kunclass.utils.zookeeper.ZookeeperNode;
import com.kunclass.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class ZookeeperRegistry extends AbstractRegistry {

    private ZooKeeper zooKeeper ;
    //创建zookeeper实例
    public ZookeeperRegistry() {
        this.zooKeeper = ZookeeperUtils.createZooKeeper();
    }
    public ZookeeperRegistry(String connectString,int timeout) {
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
        String node = parentNode+"/"+ NetUtils.getIp()+":"+ KunrpcBootstrap.PORT;
        if(!ZookeeperUtils.exists(zooKeeper,node,null)) {
            ZookeeperUtils.createNode(zooKeeper,new ZookeeperNode(node,null),null, CreateMode.EPHEMERAL);
        }
        log.debug("serviceConfig:服务{}已经被注册", serviceConfig.getInterface().getName());

    }

    /**
     *
     * @param serviceName
     * @return 服务的地址列表
     */
    @Override
    public List<InetSocketAddress> lookup(String serviceName) {
        //1.从zookeeper上获取服务的地址
        //服务名称的节点,使用路径来表示
        String parentNode = Constant.BASE_PROVIDERS_PATH+"/"+serviceName;
        //获取子节点,比如192.168.12.123：2151
        List<String> childNodes = ZookeeperUtils.getChildren(zooKeeper,parentNode,null);
        //将所有子节点转换为InetSocketAddress（获取了所有的可用的服务列表）
        List<InetSocketAddress> inetSocketAddresses = childNodes.stream().map(this::parseNode).toList();
        if(inetSocketAddresses.isEmpty()) {
            throw new DiscoveryException("没有可用的服务（主机）");
        }
        return inetSocketAddresses;
    }

    private InetSocketAddress parseNode(String s) {
        String[] split = s.split(":");
        return new InetSocketAddress(split[0],Integer.parseInt(split[1]));
    }
}
