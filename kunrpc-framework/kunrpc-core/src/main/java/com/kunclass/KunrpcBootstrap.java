package com.kunclass;

import com.kunclass.annotation.KunrpcApi;
import com.kunclass.channelHandler.handler.KunrpcRequestDecoder;
import com.kunclass.channelHandler.handler.KunrpcResponseEncoder;
import com.kunclass.channelHandler.handler.MethodCallHandler;
import com.kunclass.config.Configuration;
import com.kunclass.core.HeartbeatDetector;
import com.kunclass.discovery.RegistryConfig;
import com.kunclass.loadBalancer.LoadBalancer;
import com.kunclass.transport.message.KunrpcRequest;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class KunrpcBootstrap {


    //KunrpcBootstrap是一个单例，我们希望每个应用程序都只有一个实例
    private static final KunrpcBootstrap kunrpcBootstrap = new KunrpcBootstrap();



    //全局的配置中心
    @Getter
    private Configuration configuration;

    //连接的缓存，InetSocketAddress做key时，一定要注意是否重写了equals和toString方法
    public static final Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);
    public static final TreeMap<Long, Channel> ANSWER_TIME_CHANNEL_CACHE = new TreeMap<>();


    //维护已经发布且暴露的服务列表 ，key--》interface的全限定名，value--》serviceConfig
    public static final Map<String, ServiceConfig<?>> SERVICES_LIST = new ConcurrentHashMap<>(16);

    //定义全局的对外挂起的completableFuture
    public static final Map<Long, CompletableFuture<Object>> PENDING_REQUEST = new ConcurrentHashMap<>(128);

    //保存request对象，可以在当前线程随时获取
    public static ThreadLocal<KunrpcRequest> REQUEST_THREAD_LOCAL = new ThreadLocal<>();

    //构造函数私有化
    private KunrpcBootstrap() {
        //构造启动引导程序，需要做一些初始化工作
        configuration = new Configuration();

    }

    public static KunrpcBootstrap getInstance() {

        return kunrpcBootstrap;
    }

    /**
     * 用来定义当前应用的名字
     *
     * @param appName 应用名
     * @return this
     */
    public KunrpcBootstrap application(String appName) {
        configuration.setAppName (appName );
        return kunrpcBootstrap;
    }

    /**
     * 用来配置一个注册中心
     *
     * @param registryConfig 注册中心配置
     * @return this 当前实例
     */
    public KunrpcBootstrap registry(RegistryConfig registryConfig) {
        //这里维护一个zookeeper实例，但是这样写就会将zooKeeper和当前工程耦合
        //我们更希望以后可以扩展更多种不同的实现

        //尝试使用registryConfig来获取注册中心，类似于工厂模式
        configuration.setRegistryConfig (registryConfig );

        return this;
    }

    /**
     * 用来配置一个负载均衡器
     * @param loadBalancer
     * @return
     */
    public KunrpcBootstrap loadBalancer(LoadBalancer loadBalancer) {
        configuration.setLoadBalancer (loadBalancer );
        return this;
    }

    /**
     * ——————————————————服务提供方的api————————————————————————————————
     */


    /**
     * 用来发布一个服务,将接口以及实现，注册到服务中心
     *
     * @param service 独立封装的需要发布的服务
     * @return
     */
    public KunrpcBootstrap publish(ServiceConfig<?> service) {
        //创建zookeeper
        //注册service，我们抽象了注册中心的概念，使用注册中心的一个实现完成注册
        //这里难道不是强耦合了吗？是的

        configuration.getRegistryConfig().getRegistry().register(service);

        //1.当服务调用方通过接口、方法名、具体的方法参数列表发起调用时，服务提供方怎么根据这些信息找到对应的服务实现类，然后调用对应的方法
        //（1）new一个  （2）spring beanFactory.getBean(Class)  （3）自己维护映射关系 ✔️✔️
        SERVICES_LIST.put(service.getInterface().getName(), service);
        return this;
    }

    /**
     * 用来发布多个服务
     *
     * @param services 独立封装的需要发布的服务
     * @return
     */
    public KunrpcBootstrap publish(List<ServiceConfig<?>> services) {
        for (ServiceConfig<?> service : services) {
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
                        socketChannel.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG))
                                //消息解码器
                                .addLast(new KunrpcRequestDecoder())
                                //根据请求类型，进行方法调用
                                .addLast(new MethodCallHandler())
                                .addLast(new KunrpcResponseEncoder());
                    }
                });

        //绑定端口，同步等待成功
        try {
            ChannelFuture channelFuture = bootstrap.bind(configuration.getPort()).sync();
            System.out.println("Server started on port " + configuration.getPort());


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
        //开启对这个服务的心跳检测
        HeartbeatDetector.detectHeartbeat(referenceConfig.getInterfaceRef().getName());

        //在这个方法里，我们是否可以拿到相关的配置项-注册中心
        //配置reference,将来调用get方法时，方便生成代理对象

        //1.reference需要一个注册中心
        //这里是值传递，但是传递的是引用，所以我们可以直接设置
        referenceConfig.setRegistry(configuration.getRegistryConfig().getRegistry());

        return this;
    }

    /**
     * 用来配置序列化器
     *
     * @param serializer_type 序列化器类型
     * @return
     */
    public KunrpcBootstrap serializer(String serializer_type) {
        serializer_type = serializer_type.toLowerCase();
        configuration.setSerializeType(serializer_type);
        if (!log.isDebugEnabled()) {
            log.debug("序列化器类型:{}", serializer_type);
        }
        return this;
    }

    /**
     * 用来配置压缩器
     *
     * @param compressor_type 压缩器类型
     * @return
     */
    public KunrpcBootstrap compressor(String compressor_type) {
        configuration.setCompressType(compressor_type.toLowerCase());
        if (!log.isDebugEnabled()) {
            log.debug("压缩器类型:{}", compressor_type);
        }
        return this;
    }

    /**
     * 通过packageName扫描包下的所有类，发布服务
     * @param packageName
     * @return
     */
    public KunrpcBootstrap scan(String packageName) {
        //扫描包下的所有类,获取类名
        List<String> classNames = getAllClassNames(packageName);
        //遍历类名，获取类的class对象
        for (String className : classNames) {
            //通过反射获取class对象
            try {
                Class<?> clazz = Class.forName(className);

                // 获取接口，
                Class<?>[] interfaces = clazz.getInterfaces();
                //有接口才执行
                for (Class<?> anInterface : interfaces) {
                    //过滤掉没有标记KunrpcApi的接口
                    if (!anInterface.isAnnotationPresent(KunrpcApi.class)) {
                        continue;
                    }

                    //创建serviceConfig
                    ServiceConfig<?> serviceConfig = new ServiceConfig<>();
                    serviceConfig.setInterface(anInterface);
                    serviceConfig.setRef(clazz.getConstructor().newInstance());
                    //发布服务
                    publish(serviceConfig);
                    if(log.isDebugEnabled()) {
                        log.debug("扫描到服务[{}]，已经发布！", anInterface.getName());
                    }
                }

            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException |InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        //返回当前实例
        return this;
    }

    private static List<String> getAllClassNames(String packageName) {
        //通过packageName获取包下的所有类名
        //1.通过packageName获取绝对路径
        String packagePath = packageName.replace(".", "\\");
        //2.通过相对路径获取文件
        URL systemResource = ClassLoader.getSystemResource(packagePath);
        //3.获取文件的绝对路径
        if(systemResource == null) {
            throw new RuntimeException("包路径不存在");
        }
        String absolutePath = systemResource.getPath();

        //先对absolutePath的路径进行中文解码，比如D:\%e6%89%be%e5%ae%9e%e4%b9%a0转换为D:\找实习
        absolutePath = URLDecoder.decode(absolutePath, StandardCharsets.UTF_8);

        List<String> classNames = new ArrayList<>();
        //递归查找所有的class文件
        recursionFindClassNames(absolutePath,classNames);

        return classNames;
    }

    private static void recursionFindClassNames(String absolutePath,List<String> classNames) {
        //获取文件

        File file = new File(absolutePath);
        if(file.isDirectory()) {
            //处理文件夹下的所有文件，包括文件夹
            File[] files = file.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    //只处理class文件和文件夹，避免出现其他后缀文件
                    return file.getPath().endsWith(".class") || file.isDirectory();
                }
            });
            if(files == null) {
                return;
            }
            //递归处理
            for (File file1 : files) {
                recursionFindClassNames(file1.getAbsolutePath(),classNames);
            }
        } else {

            String className = absolutePath.substring(absolutePath.indexOf("com"), absolutePath.indexOf(".class"));
            //将绝对路径转换为类名
            className = className.replace("\\", ".");
            classNames.add(className);

        }
    }

}
