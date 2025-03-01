package com.kunclass.proxy.handler;

import com.kunclass.KunrpcBootstrap;
import com.kunclass.discovery.NettyBootstrapInitializer;
import com.kunclass.discovery.Registry;
import com.kunclass.exceptions.DiscoveryException;
import com.kunclass.exceptions.NetworkException;
import com.kunclass.transport.message.KunrpcRequest;
import com.kunclass.transport.message.RequestPayload;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 该类封装了客户端通信的基础逻辑，每一个代理对象的远程调用过程都封装在了invoke方法中
 * 1.发现服务，从注册中心，寻找一个可用的服务
 * 2.使用netty连接服务器，发送 调用的 服务的名字+方法名字+参数列表，得到结果
 * 3.将结果返回给调用方
 */

@Slf4j
public class RpcConsumerInvocationHandler implements InvocationHandler {

    private final Class<?> interfaceRef;

    private final Registry  registry;

    public RpcConsumerInvocationHandler(Class<?> interfaceRef, Registry registry) {
        this.interfaceRef = interfaceRef;
        this.registry = registry;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //我们调用sayHi方法，事实上会走进这个代码段中
        //我们已经知道method(具体的方法)、args（参数列表）
//        log.info("method:{}", method.getName());
//        log.info("args:{}", args);

//1.发现服务，从注册中心，寻找一个可用的服务
        //reference_arg.setRegistry(registry);
        //registry
        //传入服务接口的名字，从注册中心获取服务的地址
        //TODO：我们每次调用都会去注册中心获取服务地址，这个是不是有点浪费？是的，我们可以做一个缓存+watcher
        //TODO：我们如何合理得选择一个可用的服务地址呢？而不是只获取第一个。我们可以使用负载均衡算法
        InetSocketAddress inetSocketAddress = registry.lookup(interfaceRef.getName());
        log.info("服务调用方{}得到了服务地址inetSocketAddress:{}", interfaceRef.getName(),inetSocketAddress);

        //使用netty连接服务器，发送 调用的 服务的名字+方法名字+参数列表，得到结果
        //定义线程池，EventLoopGroup是一个线程组，它包含了一组NIO线程，专门用于网络事件的处理
        //Q:整个连接过程放在这里合适吗？也就意味着每次调用都会产生一个新的netty连接。A:不合适，我们应该缓存连接
        //也就意味着每次在此处建立一个新的连接是不合适的

        //解决方案：缓存channel，尝试从缓存中获取channel，如果没有，再建立连接，建立连接后，放入缓存
// 2.尝试获取一个通道
        Channel channel = getAvailableChannel(inetSocketAddress);
        log.info("服务调用方{}得到了通道channel:{}", interfaceRef.getName(),channel);

        /**
         * ————————————————————————3.封装报文————————————————————————
         */
        RequestPayload requestPayload = RequestPayload.builder()
                .interfaceName(interfaceRef.getName())
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .parameterNames(args)
                .returnType(method.getReturnType())
                .build();
        //TODO 需要对各种请求id和各种类型进行处理
        KunrpcRequest kunrpcRequest = KunrpcRequest.builder()
                .requestId(1L)
                .compressType((byte) 1)
                .serializeType((byte) 1)
                .requestType((byte) 1)

                .requestPayload(requestPayload)
                .build();


        /**
         * ————————————————————————同步的策略————————————————————————
         */
                /*
                ChannelFuture channelFuture = channel.writeAndFlush(new Object());
                //学习channelFuture的使用， get()方法是阻塞的 getNow()是非阻塞的 都是获取结果
                if(channelFuture.isDone()){
                    Object object = channelFuture.getNow();
                } else if (!channelFuture.isSuccess()){
                    //如果发送失败，我们应该怎么处理？
                    //1.重试 2.抛出异常 3.记录日志
                    //我们这里选择捕获异步任务中的异常并抛出异常
                    Throwable cause = channelFuture.cause();
                    throw new NetworkException(cause);

                }
                */

        /**
         * ————————————————————————异步的策略————————————————————————
         */
//4.写出报文
        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
        KunrpcBootstrap.PENDING_REQUEST.put(1L,completableFuture);

        //这里写出一个请求，这个请求实例会进入pipline执行出站的一系列操作
        //我们可以想象得到，第一个出站程序一定是将kunrpcRequest--》二进制报文
        channel.writeAndFlush(kunrpcRequest).addListener((ChannelFutureListener) future -> {
            //当前future的返回结果是writeAndFlush的返回结果
            //一旦数据被写入，future就结束了
            //我们想要的是服务端给我们的返回结果，所以这里处理completeFuture是不合适的
            //应该将completeFuture挂起并且暴露，并且在得到服务提供方的响应的时候调用complete方法
//                    if(future.isDone()){
//                       completableFuture.complete(future.getNow());
//                    } else
            //所以只处理一下异常就可以了
            if(!future.isSuccess()){
                completableFuture.completeExceptionally(future.cause());
            }
        });
        //如果没有地方处理这个 completeFuture，那么这个请求就会一直挂起阻塞，等待complete的执行
        //q：我们需要在哪里调用complete方法呢？a：在服务提供方的处理器中调用
//5.获得响应的结果
        return completableFuture.get(3, TimeUnit.SECONDS);
    }

    /**
     * 根据地址尝试从全局的缓存中获取一个通道
     * @param inetSocketAddress
     * @return
     */
    private Channel getAvailableChannel(InetSocketAddress inetSocketAddress) {
        //1.尝试从全局的缓存中获取一个通道
        Channel channel = KunrpcBootstrap.CHANNEL_CACHE.get(inetSocketAddress);
        //2.如果没有，再建立连接，建立连接后，放入缓存
        if(channel == null ) {
            //await()方法是同步的，会阻塞当前线程，直到连接建立成功再返回，netty还提供了异步处理的逻辑
            //sync和await都是阻塞当前线程，获取channel（连接的过程是异步的，发送数据的过程是同步的）
            //如果发生了异常，sync会抛出异常，await会返回一个future对象，通过future对象可以获取异常
            /**
             * ————————————————————————同步的策略————————————————————————
             */
//                   channel = NettyBootstrapInitializer.getInstance().connect(inetSocketAddress)
//                            .await().channel();
            /**
             * ————————————————————————异步的策略————————————————————————
             */
            CompletableFuture<Channel> channelFuture = new CompletableFuture<>();
            NettyBootstrapInitializer.getInstance().connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
                if(future.isDone()){
                    //异步的，我们已经完成
                    if(log.isDebugEnabled()){
                        log.debug("已经和{}连接成功",inetSocketAddress);
                    }
                    channelFuture.complete(future.channel());
                } else if(!future.isSuccess()){
                    //失败
                    channelFuture.completeExceptionally(future.cause());
                }
            });
            //阻塞等待channel
            try {
                channel = channelFuture.get(3,TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("获取通道时发生异常1",e);
                throw new DiscoveryException(e);
            }
            //将channel放入缓存
            KunrpcBootstrap.CHANNEL_CACHE.put(inetSocketAddress,channel);
        }
        if(channel == null){
            log.error("获取{}通道失败",inetSocketAddress);
            throw new NetworkException("获取通道时发生异常2");
        }
        return channel;
    }

}
