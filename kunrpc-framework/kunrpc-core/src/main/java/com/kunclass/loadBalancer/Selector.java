package com.kunclass.loadBalancer;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 通过某种算法，选择器选择一个服务地址
 */
public interface Selector {
    /**
     * 从服务列表中选择一个服务地址
     * @return 具体的一个服务地址
     */
    InetSocketAddress getNext();

}
