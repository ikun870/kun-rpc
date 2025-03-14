package com.rpcclass.netty;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MyCompletableFuture {
    public static void main(String[] args) throws ExecutionException, InterruptedException, TimeoutException {

        /*
        可以获取子线程的返回值，过程中的结果可以在主线程中阻塞等待其完成
         */
        CompletableFuture<Integer> future = new CompletableFuture<>();

        new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int i = 0;

            future.complete(i);

        }).start();
        //怎么在子线程中获取i的值

        //这里的get方法是阻塞的，会等待future完成
        Integer result = future.get(3, TimeUnit.SECONDS);
        System.out.println(result);

    }
}