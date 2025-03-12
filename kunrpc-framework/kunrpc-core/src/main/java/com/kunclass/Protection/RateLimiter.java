package com.kunclass.Protection;

public interface RateLimiter {

    /**
     * 尝试获取令牌
     * @return true表示获取成功,允许请求进入，false表示获取失败，拒绝请求
     */
    boolean tryAcquire();
}
