package com.kunclass.utils.zookeeper;

import com.kunclass.Constant;
import com.kunclass.exceptions.ZookeeperException;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class ZookeeperUtils {

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
    /**
     * 判断节点是否存在
     * @param zooKeeper
     * @param node
     * @param watcher
     * @return
     */
    public static boolean exists(ZooKeeper zooKeeper,String node,Watcher watcher){
        try {
            return zooKeeper.exists(node, watcher)!=null;
        } catch (KeeperException |InterruptedException e) {
            log.error("节点{}存在异常：",node,e);
            throw new ZookeeperException(e);
        }

    }


    /**
     * 查询一个节点的子元素
     * @param zooKeeper
     * @param parentNode 服务节点
     * @return 子节点
     */
    public static List<String> getChildren(ZooKeeper zooKeeper, String parentNode,Watcher watcher) {
        try {
           return zooKeeper.getChildren(parentNode, watcher);
        } catch (KeeperException | InterruptedException e) {
            log.error("查询服务{}时获取子节点异常：",parentNode,e);
            throw new ZookeeperException(e);
        }
    }

}
