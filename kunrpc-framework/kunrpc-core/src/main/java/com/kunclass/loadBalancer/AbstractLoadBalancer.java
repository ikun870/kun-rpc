package com.kunclass.loadBalancer;

import com.kunclass.KunrpcBootstrap;
import com.kunclass.discovery.Registry;
import com.kunclass.exceptions.LoadBalancerException;
import com.kunclass.loadBalancer.impl.RoundRobinLoadBalancer;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 负载均衡器的接口，这是一个模板方法模式
 * 可以使得负载均衡器具有多种实现
 */
public abstract class AbstractLoadBalancer implements LoadBalancer{


    //一个服务会匹配一个
    private Map<String,Selector> cache = new ConcurrentHashMap<>();

    @Override
    public InetSocketAddress selectServiceAddress(String serviceName) {
        //1.先从缓存中找到服务对应的选择器
        Selector selector = cache.get(serviceName);
        if(selector == null){
            //2.如果没有找到，就从注册中心拉取服务列表
            //3.将选择器放入缓存
            //负载均衡器内部维护服务列表的缓存,根据服务名称找到服务列表
            List<InetSocketAddress> serviceAddressesList = KunrpcBootstrap.getInstance().getRegistry().lookup(serviceName);

            //通过轮询选择器选择一个服务地址
            selector = getSelector(serviceAddressesList);

            cache.put(serviceName,selector);
        }
        return selector.getNext();
    }

    /**
     * 选择器的获取方法，由子类实现
     * @param serviceAddressesList 服务地址列表
     * @return 负载均衡算法选择器
     */
    protected  abstract Selector getSelector(List<InetSocketAddress> serviceAddressesList);


}
