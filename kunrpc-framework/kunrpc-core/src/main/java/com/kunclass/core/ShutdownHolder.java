package com.kunclass.core;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * 挡板用来控制停机时的请求处理
 */
public class ShutdownHolder {
    public static final AtomicBoolean BAFFLE = new AtomicBoolean(false);//挡板默认不打开

    public static final LongAdder REQUEST_COUNTER = new LongAdder();//用于请求的计数器，默认值为1

}
