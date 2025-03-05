package com.kunclass.loadBalancer.impl;

import com.kunclass.KunrpcBootstrap;
import com.kunclass.discovery.Registry;
import com.kunclass.exceptions.LoadBalancerException;
import com.kunclass.loadBalancer.AbstractLoadBalancer;
import com.kunclass.loadBalancer.LoadBalancer;
import com.kunclass.loadBalancer.Selector;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡器
 * 区分均衡器和选择器
 */
@Slf4j
public class RoundRobinLoadBalancer extends AbstractLoadBalancer {


    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceAddressesList) {
        return new RoundRobinSelector(serviceAddressesList);
    }

    /**
     * 轮询选择器,放在轮询负载均衡器的内部类中
     */
    private static class RoundRobinSelector implements Selector {

        private List<InetSocketAddress> serviceAddressesList;
        private AtomicInteger index ;


        public RoundRobinSelector(List<InetSocketAddress> serviceAddressesList) {
            this.serviceAddressesList = serviceAddressesList;
            index = new AtomicInteger(0);
        }

        /**
         * 从服务列表中选择一个服务地址
         * @return
         */
        @Override
        public InetSocketAddress getNext() {
            if(serviceAddressesList == null || serviceAddressesList.isEmpty()){
                log.error("进行轮询负载均衡时，服务列表为空");
                throw new LoadBalancerException("服务列表为空");

            }

            //轮询算法
            InetSocketAddress address = serviceAddressesList.get(index.getAndIncrement() % serviceAddressesList.size());
            //防止溢出
            if(index.get() == Integer.MAX_VALUE){
                index.set(0);
            }
            //返回一个服务地址
            return address;
        }

        @Override
        public void reBalance(List<InetSocketAddress> serviceAddressesList) {

        }
    }
}
