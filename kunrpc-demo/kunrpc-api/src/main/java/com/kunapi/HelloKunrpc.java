package com.kunapi;

public interface HelloKunrpc {

    /**
     * 通用接口，server和client都需要依赖
     * @param msh
     * @return
     */
    String sayHi(String msh);

}
