package com.kunclass.channelHandler.handler;

import com.kunclass.KunrpcBootstrap;
import com.kunclass.Protection.CircuitBreaker;
import com.kunclass.enumeration.ResponseCode;
import com.kunclass.exceptions.ResponseException;
import com.kunclass.loadBalancer.LoadBalancer;
import com.kunclass.transport.message.KunrpcRequest;
import com.kunclass.transport.message.KunrpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<KunrpcResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, KunrpcResponse kunrpcResponse) throws Exception {
        //从全局挂起的请求中获取到对应的completableFuture
        CompletableFuture<Object> completableFuture = KunrpcBootstrap.PENDING_REQUEST.get(kunrpcResponse.getRequestId());

        SocketAddress socketAddress = ctx.channel().remoteAddress();
        Map<InetSocketAddress, CircuitBreaker> ipCircuitBreaker = KunrpcBootstrap.getInstance().getConfiguration().getIpCircuitBreaker();
        CircuitBreaker circuitBreaker = ipCircuitBreaker.get(socketAddress);

        byte code = kunrpcResponse.getCode();
        if(code == ResponseCode.SERVER_ERROR.getCode()) {
            circuitBreaker.recordRequest(false);
            completableFuture.complete(null);
            log.error("服务端处理请求【{}】失败，服务端内部错误【{}】", kunrpcResponse.getRequestId(), code);
            throw new ResponseException(ResponseCode.SERVER_ERROR.getDesc(), code);
        }
        else if(code == ResponseCode.RATE_LIMIT.getCode()) {
            circuitBreaker.recordRequest(false);
            completableFuture.complete(null);
            log.error("服务端处理请求【{}】失败，限流【{}】", kunrpcResponse.getRequestId(), code);

            throw new ResponseException(ResponseCode.RATE_LIMIT.getDesc(), code);
        }
        else if(code == ResponseCode.NOT_FOUND.getCode()) {
            circuitBreaker.recordRequest(false);
            completableFuture.complete(null);
            log.error("服务端处理请求【{}】失败，服务未找到【{}】", kunrpcResponse.getRequestId(), code);

            throw new ResponseException(ResponseCode.NOT_FOUND.getDesc(), code);
        }
        else if(code == ResponseCode.HEARTBEAT_SUCCESS.getCode()){
            completableFuture.complete(null);
            if(log.isDebugEnabled()) {
                log.debug("心跳检测成功，服务端地址为{}", socketAddress);
            }
        }
        else if(code == ResponseCode.SUCCESS.getCode()){//如果是正常的响应

            //服务提供方发送过来的结果
            Object returnValue = kunrpcResponse.getBody();
           // returnValue = returnValue == null ? new Object() : returnValue;

            //将结果设置到completableFuture中
            completableFuture.complete(returnValue);

            if(log.isDebugEnabled()) {
                log.debug("在调用方找到了请求id为{}的completableFuture，处理响应结果",kunrpcResponse.getRequestId());
            }
        }
        else if(code == ResponseCode.CLOSING.getCode()) {
            completableFuture.complete(null);
            if(log.isDebugEnabled()) {
                log.debug("服务端正在关闭~~~~~，请求失败了，服务端地址为{}", socketAddress);
            }
            //修正负载均衡器  
            // 先将这个服务地址从健康的列表中移除
            KunrpcBootstrap.CHANNEL_CACHE.remove(socketAddress);
            LoadBalancer loadBalancer = KunrpcBootstrap.getInstance().getConfiguration().getLoadBalancer();
            //重新进行负载均衡reLoadBalance
            KunrpcRequest kunrpcRequest = KunrpcBootstrap.REQUEST_THREAD_LOCAL.get();
            loadBalancer.reLoadBalance(kunrpcRequest.getRequestPayload().getInterfaceName(), KunrpcBootstrap.CHANNEL_CACHE.keySet().stream().toList());

            throw new ResponseException(ResponseCode.CLOSING.getDesc(), code);
        }
        else {
            circuitBreaker.recordRequest(false);
            completableFuture.complete(null);
            log.error("服务端处理请求【{}】失败，未知错误【{}】", kunrpcResponse.getRequestId(), code);

            throw new ResponseException("服务端处理请求失败", code);
        }

    }
}
