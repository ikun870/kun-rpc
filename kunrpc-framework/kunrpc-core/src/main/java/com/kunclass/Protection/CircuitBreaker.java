package com.kunclass.Protection;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 标准的断路器应该有3个状态：(这里我们简单起见，只选取前两种)
 * 1. 关闭状态：正常工作，所有请求都被允许通过。
 * 2. 打开状态：所有请求都被拒绝，直接返回错误。
 * 3. 半开状态：允许部分请求通过，用于测试服务是否恢复。
 * 判断的指标：失败的比例
 */
public class CircuitBreaker {

    private volatile boolean isOpen =false; // 断路器是否打开
    private AtomicInteger requestCount = new AtomicInteger(0); // 请求总数
    private AtomicInteger failureCount = new AtomicInteger(0); // 失败请求数
    private int failureThreshold; // 失败阈值，超过这个值就打开断路器
    private float failureRateThreshold; // 失败率阈值，超过这个值就打开断路器

    public CircuitBreaker(float failureRateThreshold, int failureThreshold) {
        this.failureRateThreshold = failureRateThreshold;
        this.failureThreshold = failureThreshold;
    }

    //判断断路器是否打开
    public boolean isOpen() {
        return isOpen;
    }

    //每次发送请求，获取发生异常的次数
    public void recordRequest(boolean isSuccess) {
        requestCount.incrementAndGet();
        if (!isSuccess) {
            failureCount.incrementAndGet();
        }
        //判断是否打开断路器
        if (isOpen) {
            // 如果断路器已经打开，则不需要再判断了
            return;
        }
        //计算失败率
        float failureRate = (float) failureCount.get() / requestCount.get();
        if (failureRate > failureRateThreshold || failureCount.get() > failureThreshold) {
            isOpen = true;
        }
    }

    //重置断路器
    public void reset() {
        isOpen = false;
        requestCount.set(0);
        failureCount.set(0);
    }

    public static void main(String[] args) throws InterruptedException {
        CircuitBreaker circuitBreaker = new CircuitBreaker(0.5f, 3);
        new Thread(() -> {
            try {
                while (true){
                    Thread.sleep(9000);
                    circuitBreaker.reset();
                    System.out.println("Circuit breaker reset");
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }).start();
        for (int i = 0; i < 30; i++) {
            Thread.sleep(1000);
            circuitBreaker.recordRequest(i % 2 == 0); // 偶数请求成功，奇数请求失败
            System.out.println("Request " + i + ": " + (circuitBreaker.isOpen() ? "Open" : "Closed"));
        }

    }
}
