package com.kunclass.impl;
import com.kunapi.HelloKunrpc;
import com.kunapi.HelloKunrpc2;

//@KunrpcApi
public class HelloKunrpcImpl2 implements HelloKunrpc2 {
    @Override
    public String sayHi(String msh) {
        return "Hi, consumer:" + msh;
    }
}
