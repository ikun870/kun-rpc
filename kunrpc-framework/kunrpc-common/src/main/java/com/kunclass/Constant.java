package com.kunclass;

public class Constant {
    //zookeeper的默认连接地址
    public static final String DEFAULT_Zk_CONNECT = "localhost:2181";

    //zookeeper的默认超时时间
    public static final int DEFAULT_Zk_TIMEOUT = 10000;

    //服务提供方和调用方的基础路径
    public static final String BASE_PROVIDERS_PATH = "/kunrpc-metadata/providers";
    public static final String BASE_CONSUMERS_PATH = "/kunrpc-metadata/consumers";
}
