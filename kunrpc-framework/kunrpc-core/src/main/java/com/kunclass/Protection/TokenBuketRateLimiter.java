package com.kunclass.Protection;

/**
 * 基于令牌桶算法的限流器
 * 令牌桶的解释：
 * 1. 令牌桶算法是一种流量控制算法，用于限制请求的速率。
 * 2. 令牌桶中有一个固定数量的令牌，表示可以处理的请求数量。
 * 3. 当请求到达时，检查令牌桶中是否有令牌，如果有，则允许请求通过，并从桶中取出一个令牌。
 * 4. 如果没有令牌，则拒绝请求或等待一段时间后重试。
 * 5. 令牌桶会以固定的速率向桶中添加令牌，直到达到桶的最大容量。
 *
 */
public class TokenBuketRateLimiter {

    private int tokens; // 当前令牌桶中的令牌数量,>0代表有令牌，可以放行并将令牌数量减1，==0代表没有令牌，不能放行
    private int maxTokens; // 令牌桶的最大容量
    private long lastRefillTime; // 上次添加令牌的时间
    private long refillInterval; // 添加令牌的时间间隔
    private int refillTokens; // 每次添加的令牌数量，不能超过maxTokens

    public TokenBuketRateLimiter(int maxTokens, long refillInterval, int refillTokens) {
        this.maxTokens = maxTokens;
        this.refillInterval = refillInterval;
        this.refillTokens = refillTokens;
        this.tokens = maxTokens;
        this.lastRefillTime = System.currentTimeMillis();
    }

    /**
     * 尝试获取令牌
     * @return true表示获取成功，false表示获取失败
     */
    public synchronized boolean tryAcquire() {
        // 令牌桶算法的实现

        //1.将令牌添加到令牌桶
        long currentTime = System.currentTimeMillis();
        // 计算自上次添加令牌以来经过的时间
        long elapsedTime = currentTime - lastRefillTime;
        //这里必须判断，让elapsedTime累计起来，不然(elapsedTime / refillInterval)一直为0
        if (elapsedTime > refillInterval) {
            // 计算可以添加的令牌数量
            int tokensToAdd = (int) (elapsedTime / refillInterval) * refillTokens;
            // 更新令牌桶中的令牌数量
            tokens = Math.min(tokens + tokensToAdd, maxTokens);
            lastRefillTime = currentTime;
        }

        //2.检查令牌桶中是否有令牌
        if (tokens > 0) {
            // 如果有令牌，则允许请求通过，并从桶中取出一个令牌
            tokens--;
            return true;
        } else {
            // 如果没有令牌，则拒绝请求或等待一段时间后重试
            return false;
        }

    }

    public static void main(String[] args) throws InterruptedException {
        TokenBuketRateLimiter rateLimiter = new TokenBuketRateLimiter(10, 100, 1);
        for (int i = 0; i < 2000; i++) {
            Thread.sleep(10);
            if (rateLimiter.tryAcquire()) {
                System.out.println("Request " + i + " allowed");
            } else {
                System.out.println("Request " + i + " denied");
            }
        }
    }

}
