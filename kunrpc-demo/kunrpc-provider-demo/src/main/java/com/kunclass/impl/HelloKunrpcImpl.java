package com.kunclass.impl;
import com.kunapi.HelloKunrpc;
import com.kunclass.annotation.KunrpcApi;

//@KunrpcApi
public class HelloKunrpcImpl implements  HelloKunrpc{
    @Override
    public String sayHi(String msh) {
        return "Hi, consumer:" + msh;
    }
}
