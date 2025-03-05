package com.kunclass.loadBalancer.impl;

import com.kunclass.KunrpcBootstrap;
import com.kunclass.loadBalancer.Selector;
import com.kunclass.loadBalancer.AbstractLoadBalancer;
import com.kunclass.transport.message.KunrpcRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.server.quorum.QuorumCnxManager;

import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ConsistentHashBalancer extends AbstractLoadBalancer {
    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceAddressesList) {
        return new ConsistentHashSelector(serviceAddressesList,128);
    }

    /**
     * 一致性哈希选择器
     * 包括一致性哈希算法的实现
     */

    private static class ConsistentHashSelector implements Selector {
        // hash环用来存放服务器地址节点和虚拟节点
        private SortedMap<Integer, InetSocketAddress> circle = new TreeMap<>();
        //虚拟节点数
        private int virtualNodeNum ;

        public ConsistentHashSelector(List<InetSocketAddress> serviceAddressesList,int virtualNodeNum) {
            //我们应该尝试将虚拟节点均匀的分布在hash环上
            this.virtualNodeNum = virtualNodeNum;
            for (InetSocketAddress address : serviceAddressesList) {
                ////为每一个节点生成匹配的虚拟节点进行挂载
                for (int i = 0; i < virtualNodeNum; i++) {
                    String virtualNodeName = address.toString() + "&&VN" + i;
                    int hash = hash(virtualNodeName);
                    //将虚拟节点放入hash环中
                    circle.put(hash, address);
                    if(log.isDebugEnabled()) {
                        log.debug("虚拟节点[{}]被添加到Circle，hash值为{}", virtualNodeName, hash);
                    }
                }
            }

        }

        /**
         * 从hash环中选择一个服务地址
         * @return
         */

        @Override
        public InetSocketAddress getNext() {
            //1.hash环已经建立好了，我们需要根据key找到对应的服务地址
            /// 有没有办法可以获取到具体的请求内容KunrpcRequest呢？使用ThreadLocal
            KunrpcRequest request = KunrpcBootstrap.REQUEST_THREAD_LOCAL.get();
            //利用的是请求的id特征
            String requestId = String.valueOf(request.getRequestId());
            //2.根据请求的id找到对应的hash值，因为String的hashcode和分配的内存地址有关，如果多个requestId的内存地址连续，那么hash值也是连续的
            //连续的hash值，会导致负载不均衡，因此我们使用md5等算法进行hash
            int hash = hash(requestId);

            //判断hash环中是否有等于hash的节点
            //如果没有，需要去寻找大于hash的最小节点
            if(!circle.containsKey(hash)){
                //返回大于hash的最小节点
                SortedMap<Integer, InetSocketAddress> tailMap = circle.tailMap(hash);
                //如果tailMap为空，说明hash值在hash环上的位置是最后一截，我们需要返回hash环的第一个节点circle.firstKey()
                //tailMap.firstKey()是大于hash的最小节点
                hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();

            }
            //返回对应的服务地址
            return circle.get(hash);
        }

        /**
         * 为每一个节点生成匹配的虚拟节点进行挂载
         * @param address
         */
        private void addNode(InetSocketAddress address){
            for (int i = 0; i < virtualNodeNum; i++) {
                String virtualNodeName = address.toString() + "&&VN" + i;
                int hash = hash(virtualNodeName);
                //将虚拟节点放入hash环中
                circle.put(hash, address);
                if(log.isDebugEnabled()){
                    log.debug("虚拟节点[{}]被添加到Circle，hash值为{}",virtualNodeName,hash);
                }
            }
        }

        /**
         * 从hash环中删除一个服务地址
         * @param address
         */
        private void removeNode(InetSocketAddress address) {
            for (int i = 0; i < virtualNodeNum; i++) {
                String virtualNodeName = address.toString() + "&&VN" + i;
                int hash = hash(virtualNodeName);
                //将虚拟节点从hash环中删除
                circle.remove(hash);
            }
        }

        @Override
        public void reBalance(List<InetSocketAddress> serviceAddressesList) {

        }


        /**
         * 具体的hash算法  生成的hash值都比较大，其实也不太均匀
         * @param key
         * @return
         */
        private int hash (String key) {
            MessageDigest md5 = null;
            try {
                md5 = MessageDigest.getInstance("MD5");

            } catch (Exception e) {
                throw new RuntimeException("md5 error", e);
            }
            byte[] bytes = key.getBytes();
            //将key的字节数组放入md5中
            md5.update(bytes);
            //获取md5的hash值
            byte[] digest = md5.digest();
            //将hash值转换为int类型，4个字节
            //& 0xFF（八位1）是为了将byte转换为int，因为byte是有符号的，& 0xFF是为了将byte的符号位清零
            //& 0xFF就会导致结果出现很多的负数
            return ((digest[3] & 0xFF) << 24)
                    | ((digest[2] & 0xFF) << 16)
                    | ((digest[1] & 0xFF) << 8)
                    | (digest[0] & 0xFF);
        }
    }
}
