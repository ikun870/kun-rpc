package com.kunclass.discovery;
//思考注册中心应该具有什么样的能力

import com.kunclass.ServiceConfig;

public interface Registry {

    /**
     * 注册服务
     * @param serviceConfig 服务的配置内容
     */
    void register(ServiceConfig<?> serviceConfig);
}
