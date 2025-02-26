package com.kunclass.discovery;
//思考注册中心应该具有什么样的能力

import com.kunclass.ServiceConfig;

import java.net.InetSocketAddress;

public interface Registry {

    /**
     * 注册服务
     * @param serviceConfig 服务的配置内容
     */
    void register(ServiceConfig<?> serviceConfig);

    /**
     * 从注册中心拉取一个可用的服务地址
     * @param serviceName
     * @return 服务地址（ip+port）
     */
    InetSocketAddress lookup(String serviceName);
}
