package com.kunclass;

import com.kunclass.discovery.Registry;
import com.kunclass.discovery.RegistryConfig;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;

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
        Class<?>[] classes = new Class[]{interfaceRef};

        //使用动态代理生成代理对象
        Object helloProxy = Proxy.newProxyInstance(classLoader, classes, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                //我们调用sayHi方法，事实上会走进这个代码段中
                //我们已经知道method(具体的方法)、args（参数列表）
                log.info("method:{}", method.getName());
                log.info("args:{}", args);

                //1.发现服务，从注册中心，寻找一个可用的服务
                //reference_arg.setRegistry(registry);
                //registry
                //传入服务接口的名字，从注册中心获取服务的地址
                //TODO：我们每次调用都会去注册中心获取服务地址，这个是不是有点浪费？是的，我们可以做一个缓存+watcher
                //TODO：我们如何合理得选择一个可用的服务地址呢？而不是只获取第一个。我们可以使用负载均衡算法
                InetSocketAddress inetSocketAddress = registry.lookup(interfaceRef.getName());
                log.info("服务调用方{}得到了服务地址inetSocketAddress:{}", interfaceRef.getName(),inetSocketAddress);

                //2.使用netty连接服务器，发送 调用的 服务的名字+方法名字+参数列表，得到结果

                System.out.println("代理对象执行了");


                return null;
            }
        });
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
