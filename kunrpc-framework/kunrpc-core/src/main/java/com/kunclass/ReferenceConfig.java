package com.kunclass;

import com.kunclass.discovery.Registry;
import com.kunclass.discovery.RegistryConfig;
import com.kunclass.exceptions.NetworkException;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

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
                //定义线程池，EventLoopGroup是一个线程组，它包含了一组NIO线程，专门用于网络事件的处理
                //Q:整个连接过程放在这里合适吗？也就意味着每次调用都会产生一个新的netty连接。A:不合适，我们应该缓存连接
                //也就意味着每次在此处建立一个新的连接是不合适的

                //解决方案：缓存channel，尝试从缓存中获取channel，如果没有，再建立连接，建立连接后，放入缓存
                //1.尝试从全局的缓存中获取一个通道
                Channel channel = KunrpcBootstrap.CHANNEL_CACHE.get(inetSocketAddress);
                if(channel == null ) {
                    //新建一个连接
                    EventLoopGroup group = new NioEventLoopGroup();

                    //启动一个客户端需要一个启动类
                    Bootstrap bootstrap = new Bootstrap();
                    bootstrap= bootstrap.group(group)
                            //指定channel类型
                            .channel(NioSocketChannel.class)
                            .handler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel socketChannel) throws Exception {
                                    //添加处理器
                                    socketChannel.pipeline().addLast(null);
                                }
                            });
                    //尝试连接服务器
                    try {
                        channel = bootstrap.connect(inetSocketAddress).sync().channel();
                        //加入缓存
                        KunrpcBootstrap.CHANNEL_CACHE.put(inetSocketAddress,channel);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if(channel == null)
                    throw new NetworkException("获取通道时发生异常");

                ChannelFuture channelFuture = channel.writeAndFlush(new Object());

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
