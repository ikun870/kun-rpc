package com.kunclass.loadBalancer;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 负载均衡器的接口
 */
public interface LoadBalancer {

    /**
     * 从服务列表中选择一个服务地址
     * @param serviceName 服务名称
     * @return 服务地址
     */
    //可以根据服务列表找到一个可用的服务地址
    InetSocketAddress selectServiceAddress(String serviceName);

    /**
     * 当感知节点发生了动态上下线，需要重新进行负载均衡
     * @param serviceName 服务名称
     */
    void reLoadBalance(String serviceName,List<InetSocketAddress> serviceAddresses);

}
