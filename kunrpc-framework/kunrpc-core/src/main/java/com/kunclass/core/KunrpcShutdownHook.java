package com.kunclass.core;

import java.util.concurrent.TimeUnit;

/**
 * 停机时的钩子函数
 */
public class KunrpcShutdownHook extends Thread{

    @Override
    public void run() {
        //1.打开挡板    boolean需要线程安全
        ShutdownHolder.BAFFLE.set(true);

        //2.等待计数器归零，所有的请求都处理完毕 AtomicInteger
        //最多等10s
        long start = System.currentTimeMillis();
        while (true){
            try {
                //等待归零，继续执行，countDownLatch
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if(ShutdownHolder.REQUEST_COUNTER.sum() == 0 || System.currentTimeMillis() - start > 10000){
                break;
            }
        }



        //3.阻塞结束后，释放资源
    }
}
