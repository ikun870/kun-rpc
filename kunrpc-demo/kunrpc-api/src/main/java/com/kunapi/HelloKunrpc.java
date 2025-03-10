package com.kunapi;

import com.kunclass.annotation.KunrpcApi;
import com.kunclass.annotation.TryTimes;

@KunrpcApi
public interface HelloKunrpc {

    /**
     * 通用接口，server和client都需要依赖
     * @param msh
     * @return
     */
    @TryTimes(tryTimes = 3, interval = 3000)
    String sayHi(String msh);

}
