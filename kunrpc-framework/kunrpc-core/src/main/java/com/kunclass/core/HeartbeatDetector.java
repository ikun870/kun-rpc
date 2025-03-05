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
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class HeartbeatDetector {

    public static void detectHeartbeat(String serviceName) {
        //1.从注册中心拉取服务列表并建立连接
        Registry registry = KunrpcBootstrap.getInstance().getRegistry();
        List<InetSocketAddress> lookupResult = registry.lookup(serviceName);

        //2.将连接进行缓存
        for (InetSocketAddress inetSocketAddress : lookupResult) {
            try {
                if(KunrpcBootstrap.CHANNEL_CACHE.containsKey(inetSocketAddress)){
                Channel channel = NettyBootstrapInitializer.getInstance().connect(inetSocketAddress).sync().channel();
                KunrpcBootstrap.CHANNEL_CACHE.put(inetSocketAddress, channel);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }


        //3.任务、定期发送消息
            Thread thread = new Thread(() -> new Timer().scheduleAtFixedRate(new MyTimerTask(), 2000, 2000),"heartbeat-thread");
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
                Channel channel = entry.getValue();
                InetSocketAddress inetSocketAddress = entry.getKey();

                long startTime = System.currentTimeMillis();

                //构建一个心跳请求
                KunrpcRequest kunrpcRequest = KunrpcRequest.builder()
                        .requestId(KunrpcBootstrap.ID_GENERATOR.getId())
                        //.requestPayload(null)
                        .requestType(RequestType.HEARTBEAT.getId())
                        .compressType(CompressorFactory.getCompressorWrapper(KunrpcBootstrap.COMPRESSOR_TYPE).getCode())
                        .serializeType(SerializerFactory.getSerializerWrapper(KunrpcBootstrap.SERIALIZER_TYPE).getCode())
                        .timeStamp(startTime)
                        .build();

                CompletableFuture<Object> completableFuture = new CompletableFuture<>();
                KunrpcBootstrap.PENDING_REQUEST.put(kunrpcRequest.getRequestId(), completableFuture);

                channel.writeAndFlush(kunrpcRequest).addListener((ChannelFutureListener) future -> {
                    if(!future.isSuccess()){
                        completableFuture.completeExceptionally(future.cause());
                    }
                });
                //
                Long endTime = null;
                try {
                    completableFuture.get();
                    endTime = System.currentTimeMillis();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }

                Long Time = endTime - startTime;
                //将心跳耗时放入缓存(缓存的结构是TreeMap，自动排序)
                KunrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.put(Time,channel);
                log.info("服务提供方{}的心跳耗时为：{}ms",inetSocketAddress,Time);
                //KunrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.firstEntry();

            }

        }
    }



}
