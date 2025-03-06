package com.kunclass.loadBalancer.impl;

import com.kunclass.KunrpcBootstrap;
import com.kunclass.loadBalancer.AbstractLoadBalancer;
import com.kunclass.loadBalancer.Selector;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

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
                // 返回最小响应时间的地址
                return (InetSocketAddress) entry.getValue().remoteAddress();
            }

        }

        @Override
        public void reBalance(List<InetSocketAddress> serviceAddressesList) {

        }
    }
}
