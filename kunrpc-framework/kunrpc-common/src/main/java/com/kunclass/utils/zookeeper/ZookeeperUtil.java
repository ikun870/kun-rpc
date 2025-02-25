package com.kunclass.utils.zookeeper;

import com.kunclass.Constant;
import com.kunclass.exceptions.ZookeeperException;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class ZookeeperUtil {

    /**
     * 使用默认配置创建zookeeper实例
     * @return ZooKeeper实例
     */
    public static ZooKeeper createZooKeeper() {
        //定义连接参数
        String connectString = Constant.DEFAULT_Zk_CONNECT;
        //定义超时时间
        int timeout = Constant.DEFAULT_Zk_TIMEOUT;

        return createZooKeeper(connectString, timeout);
    }
    public static ZooKeeper createZooKeeper(String connectString, int timeout) {

        CountDownLatch countDownLatch = new CountDownLatch(1);


        try {
            // 创建zookeeper实例，建立连接
            final ZooKeeper zooKeeper = new ZooKeeper(connectString, timeout, event ->
            {
                // 只有连接成功才放行
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    log.info("客户端已经连接成功。");
                    countDownLatch.countDown();
                }
            });

            countDownLatch.await();
            //定义节点和数据
            return zooKeeper;
        }
        catch (IOException | InterruptedException e) {
            log.error("创建zookeeper实例失败", e);
            throw new ZookeeperException();
        }
        //return null;
    }

    /**
     * 创建节点
     * @param zooKeeper
     * @param node
     * @param watcher
     * @param createMode 创建模式
     * @return
     */
    public static Boolean createNode(ZooKeeper zooKeeper,ZookeeperNode node,Watcher watcher,CreateMode createMode) {
        try {
            if (zooKeeper.exists(node.getNodePath(), watcher) == null) {
                String res = zooKeeper.create(node.getNodePath(), node.getData(), ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
                log.info("创建节点成功：{}", res);
                return true;
            }
            else {
                if(log.isDebugEnabled())
                    log.debug("节点已存在：{}", node.getNodePath());
            }
        }
        catch (InterruptedException | KeeperException e) {
            log.error("创建zookeeper实例时发生异常：",e);
            throw new ZookeeperException();
        }
        //已经存在或者抛出异常
        return false;
    }

    /**
     * 关闭zookeeper实例
     * @param zookeeper
     */
    public static void close(ZooKeeper zookeeper){
        try {
            zookeeper.close();
        }
        catch (InterruptedException e) {
            log.error("关闭zookeeper实例时发生异常：",e);
            throw new ZookeeperException();
        }

    }


}
