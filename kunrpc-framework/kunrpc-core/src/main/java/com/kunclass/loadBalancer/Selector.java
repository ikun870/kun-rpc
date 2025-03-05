package com.kunclass.loadBalancer;

import java.net.InetSocketAddress;
import java.util.List;

public interface Selector {
    /**
     * 从服务列表中选择一个服务地址
     * @return 具体的一个服务地址
     */
    InetSocketAddress getNext();

    //TODO: 服务上下线重新平衡负载
    void reBalance(List<InetSocketAddress> serviceAddressesList);

}
