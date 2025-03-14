package com.kunclass.channelHandler.handler;

import com.kunclass.KunrpcBootstrap;
import com.kunclass.Protection.RateLimiter;
import com.kunclass.Protection.TokenBuketRateLimiter;
import com.kunclass.ServiceConfig;
import com.kunclass.core.ShutdownHolder;
import com.kunclass.enumeration.RequestType;
import com.kunclass.enumeration.ResponseCode;
import com.kunclass.transport.message.KunrpcRequest;
import com.kunclass.transport.message.KunrpcResponse;
import com.kunclass.transport.message.RequestPayload;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

@Slf4j
public class MethodCallHandler extends SimpleChannelInboundHandler<KunrpcRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, KunrpcRequest kunrpcRequest) throws Exception {
        Channel channel = ctx.channel();
        //-3.先封装部分响应
        KunrpcResponse response = KunrpcResponse.builder()
                .requestId(kunrpcRequest.getRequestId())
                .compressType(kunrpcRequest.getCompressType())
                .serializeType(kunrpcRequest.getSerializeType())
                .body(null)
                //TODO 这里不设置会产生BUG
                .timeStamp(System.currentTimeMillis())
                .build();

        //-2.如果挡板打开，直接返回
        if(ShutdownHolder.BAFFLE.get()) {
            response.setCode(ResponseCode.CLOSING.getCode());
            channel.writeAndFlush(response);
            return;
        }

        //-1.先进行请求计数器加一
        ShutdownHolder.REQUEST_COUNTER.increment();

        //0.完成限流相关的操作
        SocketAddress socketAddress = channel.remoteAddress();
        Map<InetSocketAddress, RateLimiter> ipRateLimiter = KunrpcBootstrap.getInstance().getConfiguration().getIpRateLimiter();

        RateLimiter rateLimiter = ipRateLimiter.get((InetSocketAddress)socketAddress);
        if (rateLimiter == null) {
            rateLimiter = new TokenBuketRateLimiter(5, 1000,5);
            ipRateLimiter.put((InetSocketAddress)socketAddress, rateLimiter);
        }
        boolean tryAcquire = rateLimiter.tryAcquire();

        //限流
        if (!tryAcquire) {
            //如果没有获取到令牌，说明限流了,这时需要封装响应，进行返回！
            response.setCode(ResponseCode.RATE_LIMIT.getCode());
            log.warn("ip:{}限流了", socketAddress);

        }
        //处理心跳
        else if (kunrpcRequest.getRequestType() == RequestType.HEARTBEAT.getId()) {
            response.setCode(ResponseCode.HEARTBEAT_SUCCESS.getCode());
        }
        else {
        //正常的具体的调用过程

            /// --------------------------正常的具体的调用过程---------------------------------
            //1.获取负载内容
            RequestPayload payload = kunrpcRequest.getRequestPayload();
            //2.根据负载内容，调用对应的方法
            try {
                Object result = callTargetMethod(payload);

                if (log.isDebugEnabled()) {
                    log.debug("服务端调用{}服务的{}方法成功", payload.getInterfaceName(), payload.getMethodName());
                }

                //3.继续封装响应结果，其中压缩、序列化的格式和kunrpcRequest一样
                response.setCode(ResponseCode.SUCCESS.getCode());
                response.setBody(result);

            }catch (Exception e) {
                //如果调用失败了，设置响应的code
                response.setCode(ResponseCode.SERVER_ERROR.getCode());
                log.error("服务端调用{}服务的{}方法失败", payload.getInterfaceName(), payload.getMethodName(), e);
            }
        }
        //4.写出发送响应
        channel.writeAndFlush(response);

        //5.计数器减一
        ShutdownHolder.REQUEST_COUNTER.decrement();
    }

    private Object callTargetMethod(RequestPayload payload) {
        String methodName = payload.getMethodName();
        String interfaceName = payload.getInterfaceName();
        Class<?>[] parameterTypes = payload.getParameterTypes();
        Object[] parameterNames = payload.getParameterNames();

        //这里我们需要根据interfaceName获取到对应的服务实现类，然后调用对应的方法
        ServiceConfig<?> serviceConfig = KunrpcBootstrap.SERVICES_LIST.get(interfaceName);
        //具体的实现类
        Object refImpl = serviceConfig.getRef();
        //通过反射调用对应的方法
        //1.获取方法对象 2.执行invoke
        Object invoke = null;
        try {
            Class<?> aClass = refImpl.getClass();
            Method method = aClass.getMethod(methodName, parameterTypes);
            invoke = method.invoke(refImpl, parameterNames);
        }
        catch (Exception e) {
            log.error("调用{}服务的{}方法时反射发送失败",interfaceName,methodName, e);
            throw new RuntimeException(e);
        }
        return invoke;
    }
}
