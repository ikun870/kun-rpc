package com.kunclass;

import com.kunclass.discovery.Registry;
import com.kunclass.discovery.RegistryConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;


import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class KunrpcBootstrap {



    //KunrpcBootstrap是一个单例，我们希望每个应用程序都只有一个实例
    private static final KunrpcBootstrap kunrpcBootstrap = new KunrpcBootstrap();

    //定义一些相关的基础配置
    private String appName = "default";
    private RegistryConfig registryConfig;
    private ProtocolConfig protocolConfig;
    private int port = 8088;
    private Registry registry;

    //连接的缓存，InetSocketAddress做key时，一定要注意是否重写了equals和toString方法
    public static final Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);


    //维护已经发布且暴露的服务列表 ，key--》interface的全限定名，value--》serviceConfig
    private static final Map<String,ServiceConfig<?>> SERVICES_LIST = new ConcurrentHashMap<>(16);

    //定义全局的对外挂起的completableFuture
    public static final Map<Long, CompletableFuture<Object>> PENDING_REQUEST = new ConcurrentHashMap<>(128);

    //构造函数私有化
    private KunrpcBootstrap() {
    //构造启动引导程序，需要做一些初始化工作

    }

    public static KunrpcBootstrap getInstance() {

        return kunrpcBootstrap;
    }

    /**
     * 用来定义当前应用的名字
     * @param appName 应用名
     * @return this
     */
    public KunrpcBootstrap application(String appName) {
        this.appName = appName;
        return  kunrpcBootstrap;
    }

    /**
     * 用来配置一个注册中心
     * @param registryConfig 注册中心配置
     * @return this 当前实例
     */
    public KunrpcBootstrap registry(RegistryConfig registryConfig) {
        //这里维护一个zookeeper实例，但是这样写就会将zooKeeper和当前工程耦合
        //我们更希望以后可以扩展更多种不同的实现

        //尝试使用registryConfig来获取注册中心，类似于工厂模式
        this.registry = registryConfig.getRegistry();
        return this;
       // return kunrpcBootstrap;
    }

    /**
     * 用来配置一个协议
     * @param protocolConfig
     * @return
     */
    public KunrpcBootstrap protocol(ProtocolConfig protocolConfig) {
        this.protocolConfig = protocolConfig;
        log.debug("protocolConfig:{}", protocolConfig.toString());
        return this;
    }
    /**
     * ——————————————————服务提供方的api————————————————————————————————
     */


    /**
     * 用来发布一个服务,将接口以及实现，注册到服务中心
     * @param service 独立封装的需要发布的服务
     * @return
     */
    public KunrpcBootstrap publish(ServiceConfig<?> service) {
        //创建zookeeper
        //注册service，我们抽象了注册中心的概念，使用注册中心的一个实现完成注册
        //这里难道不是强耦合了吗？是的

        registry.register(service);

        //1.当服务调用方通过接口、方法名、具体的方法参数列表发起调用时，服务提供方怎么根据这些信息找到对应的服务实现类，然后调用对应的方法
        //（1）new一个  （2）spring beanFactory.getBean(Class)  （3）自己维护映射关系 ✔️✔️
        SERVICES_LIST.put(service.getInterface().getName(),service);
        return this;
    }

    /**
     * 用来发布多个服务
     * @param services 独立封装的需要发布的服务
     * @return
     */
    public KunrpcBootstrap publish(List<ServiceConfig<?>> services) {
        for(ServiceConfig<?> service : services) {
            publish(service);
        }
        return this;
    }

    /**
     * 启动netty服务引导程序
     */
    public void start() {
        //定义线程池，EventLoopGroup是一个线程组，它包含了一组NIO线程，专门用于网络事件的处理
        //bossGroup用于接收连接，workerGroup用于具体的处理
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        //启动一个服务端需要一个启动类
        ServerBootstrap bootstrap = new ServerBootstrap();
        //配置启动类
        bootstrap = bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        //添加处理器
                        //这里是核心，我们需要添加很多入站和出站的处理器handler
                        socketChannel.pipeline().addLast(new SimpleChannelInboundHandler<>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
                                ByteBuf byteBuf = (ByteBuf) msg;
                                log.info("byteBf-->{}: " , byteBuf.toString(Charset.defaultCharset()));

                                //可以就此不管了，也可以写回去
                                channelHandlerContext.channel().writeAndFlush(Unpooled.copiedBuffer("Kunrpc:Hello,Client!", Charset.defaultCharset()));
                            }
                        });
                    }
                });

        //绑定端口，同步等待成功
        try {
            ChannelFuture channelFuture = bootstrap.bind(port).sync();
            System.out.println("Server started on port " + port);
            //等待服务端监听端口关闭
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //关闭线程池
            try {
                bossGroup.shutdownGracefully().sync();
                workerGroup.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }


    /**
     * ——————————————————服务调用方的api————————————————————————————————
     */
    public KunrpcBootstrap reference(ReferenceConfig<?> referenceConfig) {
        //在这个方法里，我们是否可以拿到相关的配置项-注册中心
        //配置reference,将来调用get方法时，方便生成代理对象

        //1.reference需要一个注册中心
        //这里是值传递，但是传递的是引用，所以我们可以直接设置
        referenceConfig.setRegistry(registry);

        return this;
    }

}
