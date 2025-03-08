package com.kunclass.core;

import com.kunclass.Compress.CompressorFactory;
import com.kunclass.KunrpcBootstrap;
import com.kunclass.Serialize.SerializerFactory;
import com.kunclass.discovery.NettyBootstrapInitializer;
import com.kunclass.discovery.Registry;
import com.kunclass.enumeration.RequestType;
import com.kunclass.exceptions.DiscoveryException;
import com.kunclass.exceptions.NetworkException;
import com.kunclass.transport.message.KunrpcRequest;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 心跳检测器
 * 核心目的：检测服务提供方的心跳，以便在服务提供方宕机时，及时剔除该服务
 */
@Slf4j
public class HeartbeatDetector {

    public static void detectHeartbeat(String serviceName) {
        //1.从注册中心拉取服务列表并建立连接
        Registry registry = KunrpcBootstrap.getInstance().getConfiguration().getRegistryConfig().getRegistry();
        List<InetSocketAddress> lookupResult = registry.lookup(serviceName);

        //2.将连接进行缓存
        for (InetSocketAddress inetSocketAddress : lookupResult) {
            try {
                //
                if(!KunrpcBootstrap.CHANNEL_CACHE.containsKey(inetSocketAddress)){
                    //这里应该是指的那些刚刚上线没有来得及建立连接的服务提供方节点
                Channel channel = NettyBootstrapInitializer.getInstance().connect(inetSocketAddress).sync().channel();
                KunrpcBootstrap.CHANNEL_CACHE.put(inetSocketAddress, channel);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }


        //3.任务、定期发送消息
            Thread thread = new Thread(() -> new Timer().scheduleAtFixedRate(new MyTimerTask(), 0, 4000),"heartbeat-thread");
            //设置为守护线程,主线程结束后，守护线程也会结束
            thread.setDaemon(true);
            thread.start();

        }

    }

        //
    private static class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            //在每次心跳检测之前，清空上一次响应时间的缓存
            KunrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.clear();
            //4.发送心跳消息
            Map<InetSocketAddress, Channel> channelCache = KunrpcBootstrap.CHANNEL_CACHE;
            for (Map.Entry<InetSocketAddress, Channel> entry : channelCache.entrySet()) {
                int tryTimes = 0;
                //对于每一个服务提供方，我们都会进行心跳检测
                while (true) {
                    Channel channel = entry.getValue();
                    InetSocketAddress inetSocketAddress = entry.getKey();

                    long startTime = System.currentTimeMillis();

                    //构建一个心跳请求
                    KunrpcRequest kunrpcRequest = KunrpcRequest.builder()
                            .requestId(KunrpcBootstrap.getInstance().getConfiguration().getIdGenerator().getId())
                            //.requestPayload(null)
                            .requestType(RequestType.HEARTBEAT.getId())
                            .compressType(CompressorFactory.getCompressorWrapper(KunrpcBootstrap.getInstance().getConfiguration().getCompressType()).getCode())
                            .serializeType(SerializerFactory.getSerializerWrapper(KunrpcBootstrap.getInstance().getConfiguration().getSerializeType()).getCode())
                            .timeStamp(startTime)
                            .build();

                    CompletableFuture<Object> completableFuture = new CompletableFuture<>();
                    KunrpcBootstrap.PENDING_REQUEST.put(kunrpcRequest.getRequestId(), completableFuture);

                    channel.writeAndFlush(kunrpcRequest).addListener((ChannelFutureListener) future -> {
                        if (!future.isSuccess()) {
                            completableFuture.completeExceptionally(future.cause());
                        }
                    });
                    //
                    Long endTime = null;
                    try {
                        //阻塞等待结果
                        //我们这里设置了一个超时时间，如果超过了这个时间，我们就认为这个服务提供方已经宕机
                        completableFuture.get(1000, TimeUnit.MILLISECONDS);
                        endTime = System.currentTimeMillis();
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        log.error("服务提供方{}【第{}次】心跳检测失败", inetSocketAddress,++tryTimes);
                        //从缓存中移除
                        //当心跳检测3次失败时，我们认为这个服务提供方已经宕机
                        if (tryTimes >= 3) {
                            log.error("服务提供方{}已经宕机", inetSocketAddress);
                            KunrpcBootstrap.CHANNEL_CACHE.remove(inetSocketAddress);
                            break;
                        }
                        //等待后重试
                        try {
                            Thread.sleep(10*(new Random().nextInt(1,5)));
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }
                        continue;
                    }

                    Long Time = endTime - startTime;
                    //将心跳耗时放入缓存(缓存的结构是TreeMap，自动排序)
                    KunrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.put(Time, channel);
                    log.info("服务提供方{}的心跳耗时为：{}ms", inetSocketAddress, Time);
                    //KunrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.firstEntry();
                    break;
                }
            }
            log.info("begin------------------响应时间的treemap------------------");
            for(Map.Entry<Long, Channel> entry : KunrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.entrySet()){
                if(log.isDebugEnabled()){
                    log.debug("响应时间：{}ms,服务提供方：{}",entry.getKey(),entry.getValue().remoteAddress());
                }
            }
            log.info("end------------------响应时间的treemap------------------");
        }
    }



}
