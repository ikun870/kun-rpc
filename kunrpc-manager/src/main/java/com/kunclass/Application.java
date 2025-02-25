package com.kunclass;

import com.kunclass.exceptions.ZookeeperException;
import com.kunclass.utils.zookeeper.ZookeeperNode;
import com.kunclass.utils.zookeeper.ZookeeperUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.List;

/**
 * 注册中心的管理页面
 */
@Slf4j
public class Application {
    public static void main(String[] args) {
        //帮我们创建基础目录
        // 帮我们创建基础目录
        // kunrpc-metadata   (持久节点)
        //  └─ providers （持久节点）
        //  		└─ service1  （持久节点，接口的全限定名）
        //  		    ├─ node1 [data]     /ip:port
        //  		    ├─ node2 [data]
        //            └─ node3 [data]
        //  └─ consumers
        //        └─ service1
        //             ├─ node1 [data]
        //             ├─ node2 [data]
        //             └─ node3 [data]
        //  └─ config

        // 创建zookeeper实例，建立连接
        ZooKeeper zooKeeper = ZookeeperUtil.createZooKeeper();

        //定义节点和数据
        String basePath = "/kunrpc-metadata";
        String providersPath = basePath + "/providers";
        String consumersPath = basePath + "/consumers";

        ZookeeperNode baseNode = new ZookeeperNode("/kunrpc-metadata", null);
        ZookeeperNode providerNode = new ZookeeperNode(providersPath, null);
        ZookeeperNode consumerNode = new ZookeeperNode(consumersPath, null);
        //创建节点
        List.of(baseNode, providerNode, consumerNode).forEach(node -> {
            ZookeeperUtil.createNode(zooKeeper, node, null, CreateMode.PERSISTENT);
        });

        ZookeeperUtil.close(zooKeeper);
    }
}
