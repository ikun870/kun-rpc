package com.kunclass.watch;

import com.kunclass.KunrpcBootstrap;
import com.kunclass.discovery.NettyBootstrapInitializer;
import com.kunclass.discovery.Registry;
import com.kunclass.discovery.RegistryConfig;
import com.kunclass.loadBalancer.LoadBalancer;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class OnlineAndOfflineWatcher implements Watcher {
    @Override
    public void process(WatchedEvent watchedEvent) {

        //判断事件类型
        //先判断是否是连接类型的事件
        if (watchedEvent.getType() == Event.EventType.NodeChildrenChanged) {
            //获取服务名
            String serviceName = watchedEvent.getPath().substring(watchedEvent.getPath().lastIndexOf("/") + 1);

            if(log.isDebugEnabled()) {
                log.debug("服务{}：检测到节点事件类型：{}", serviceName,watchedEvent.getType());
            }
            //节点发生变化，重新获取服务列表
            Registry registry = KunrpcBootstrap.getInstance().getConfiguration().getRegistryConfig().getRegistry();
            List<InetSocketAddress> addresses = registry.lookup(serviceName);
            for(InetSocketAddress address : addresses) {
                //todo
                //新增的节点 会在addresses中，不在CHANNEL_CACHE中，但是可能会被心跳检测的lookup查询到刚好放入CHANNEL_CACHE
                //放入CHANNEL_CACHE的两种情况：1.lookup查询到的但是没有在CHANNEL_CACHE中 2.新上线的节点，在建立channel连接后放入
                if(!KunrpcBootstrap.CHANNEL_CACHE.containsKey(address)) {
                    //根据地址建立连接，连接成功后会放入CHANNEL_CACHE中
                    try {
                        //每一个上线的服务节点都需要通过NettyBootstrapInitializer先建立和channel的连接，然后放入缓存中
                        Channel channel = NettyBootstrapInitializer.getInstance().connect(address).sync().channel();
                        KunrpcBootstrap.CHANNEL_CACHE.put(address,channel);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    log.info("服务{}：节点{}上线", serviceName,address);
                }
                //下线的节点 可能会在CHANNEL_CACHE中，不在addresses中
                for(InetSocketAddress key : KunrpcBootstrap.CHANNEL_CACHE.keySet()) {
                    if(!addresses.contains(key)) {
                        //下线的节点，从CHANNEL_CACHE中移除
                        KunrpcBootstrap.CHANNEL_CACHE.remove(key);
                        log.info("服务{}：节点{}下线", serviceName,key);
                    }
                }

            }
            //获取负载均衡器，重新进行负载均衡
            LoadBalancer loadBalancer = KunrpcBootstrap.getInstance().getConfiguration().getLoadBalancer();
            loadBalancer.reLoadBalance(serviceName,addresses);

        }


    }
}
