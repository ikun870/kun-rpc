package com.kunapi;

import com.kunclass.annotation.KunrpcApi;


public interface HelloKunrpc2 {

    /**
     * 通用接口，server和client都需要依赖
     * @param msh
     * @return
     */
    String sayHi(String msh);

}
