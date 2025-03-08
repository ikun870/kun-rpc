package com.kunclass.loadBalancer.impl;

import com.kunclass.KunrpcBootstrap;
import com.kunclass.loadBalancer.AbstractLoadBalancer;
import com.kunclass.loadBalancer.Selector;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

@Slf4j
public class MinimumResponseTimeLoadBalancer extends AbstractLoadBalancer {
    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceAddressesList) {
        return new MinimumResponseTimeSelector(serviceAddressesList);
    }


    private static class MinimumResponseTimeSelector implements Selector {

        public MinimumResponseTimeSelector(List<InetSocketAddress> serviceAddressesList) {
        }

        @Override
        public InetSocketAddress getNext() {
            //firstEntry() returns the first (lowest) entry in the map, or null if the map is empty.
            Map.Entry<Long, Channel> entry = KunrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.firstEntry();
            if (entry == null) {
                //直接从缓存中获取一个可用的地址就行;

                return (InetSocketAddress) KunrpcBootstrap.CHANNEL_CACHE.entrySet().iterator().next().getValue().remoteAddress();
            }
            else {
                if(log.isDebugEnabled()) {
                    log.debug("选取了心跳检测耗时最短【{}ms】的服务提供方{}", entry.getKey(), entry.getValue().remoteAddress());
                }
                // 返回最小响应时间的地址
                return (InetSocketAddress) entry.getValue().remoteAddress();
            }

        }

    }
}
