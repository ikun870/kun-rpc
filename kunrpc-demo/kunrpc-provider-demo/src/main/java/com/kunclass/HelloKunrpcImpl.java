package com.kunclass;
import com.kunapi.HelloKunrpc;
public class HelloKunrpcImpl implements  HelloKunrpc{
    @Override
    public String sayHi(String msh) {
        return "Hi, consumer:" + msh;
    }
}
