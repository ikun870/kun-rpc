package com.kunclass;

import com.kunclass.discovery.NettyBootstrapInitializer;
import com.kunclass.discovery.Registry;
import com.kunclass.discovery.RegistryConfig;
import com.kunclass.exceptions.NetworkException;
import com.kunclass.proxy.handler.RpcConsumerInvocationHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ReferenceConfig<T> {
    //保存需要代理的服务接口类型。
    private Class<T> interfaceRef;
    //通过KunrpcBootstrap的reference方法直接拿到registry，用于后续的服务发现。
    private Registry  registry;



    /**
     * 获取一个api接口的代理对象
     * 代理设计模式
     * @return
     */
    public T get() {
        //使用动态代理
        //1.获取当前线程的类加载器
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        //2.获取当前接口的类对象
        Class<T>[] classes = new Class[]{interfaceRef};

        InvocationHandler invocationHandler = new RpcConsumerInvocationHandler(interfaceRef, registry);

        //使用动态代理生成代理对象
        Object helloProxy = Proxy.newProxyInstance(classLoader, classes, invocationHandler);
        return (T) helloProxy;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public Class<T> getInterfaceRef() {
        return interfaceRef;
    }

    public void setInterfaceRef(Class<T> interfaceRef) {
        this.interfaceRef = interfaceRef;
    }
}
