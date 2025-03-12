package com.kunclass.channelHandler.handler;

import com.kunclass.KunrpcBootstrap;
import com.kunclass.ServiceConfig;
import com.kunclass.enumeration.RequestType;
import com.kunclass.enumeration.ResponseCode;
import com.kunclass.transport.message.KunrpcRequest;
import com.kunclass.transport.message.KunrpcResponse;
import com.kunclass.transport.message.RequestPayload;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

@Slf4j
public class MethodCallHandler extends SimpleChannelInboundHandler<KunrpcRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, KunrpcRequest kunrpcRequest) throws Exception {
        //1.获取负载内容
        RequestPayload payload = kunrpcRequest.getRequestPayload();
        //2.根据负载内容，调用对应的方法
        Object result = null;
        if(kunrpcRequest.getRequestType()!= RequestType.HEARTBEAT.getId()){
            result = callTargetMethod(payload);

            if(log.isDebugEnabled()) {
                log.debug("服务端调用{}服务的{}方法成功",payload.getInterfaceName(),payload.getMethodName());
            }
        }


        //3.封装响应结果，其中压缩、序列化的格式和kunrpcRequest一样
        KunrpcResponse response = KunrpcResponse.builder()
                .requestId(kunrpcRequest.getRequestId())
                .code(ResponseCode.SUCCESS.getCode())
                .compressType(kunrpcRequest.getCompressType())
                .serializeType(kunrpcRequest.getSerializeType())
                .body(result)
                .timeStamp(System.currentTimeMillis())
                .build();
        //4.写出发送响应
        ctx.channel().writeAndFlush(response);
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
