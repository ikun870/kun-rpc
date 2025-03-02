package com.kunclass;

import com.kunclass.utils.DateUtils;

import java.util.Date;
import java.util.concurrent.atomic.LongAdder;

/**
 * 请求Id生成器
 */

public class IdGenerator {

    /**
     * 单机版本的线程安全的id发号器，一旦变成集群状态，就不行了
     */
    /*
    public static LongAdder longAdder = new LongAdder();
    //不同的线程，生成的id是不一样的
    public static long getId() {
        longAdder.increment();
        return longAdder.sum();
    }
     */

    /**
     * 雪花算法，生成全局唯一的id
     * 1.机房id（数据中心id）：5位
     * 2.机器id：5位
     * 3.时间戳：原本64位表示时间，是一个很大的数，我们可以用当前时间戳减去一个起始时间点的时间戳，这样就可以用42位表示时间
     * 4.序列号：12位（需要序列号是因为在同一毫秒内，可能会有多个请求）
     * 5位机房id+5位机器id+42位时间戳+12位序列号=64位
     */

    //起始的时间戳
    public static final long START_STAMP = DateUtils.get("2025-01-01").getTime();
    //private final static long START_STAMP = 1609459200000L; //2021-01-01 00:00:00
    //每一部分占用的位数
    public static final long DATA_CENTER_ID_BITS = 5L;
    public static final long MACHINE_ID_BITS = 5L;
    public static final long SEQUENCE_BITS = 12L;

    //最大值
    public static final long MAX_DATA_CENTER_ID = ~(-1 << DATA_CENTER_ID_BITS);
    public static final long MAX_MACHINE_ID = ~(-1 << MACHINE_ID_BITS);
    public static final long MAX_SEQUENCE = ~(-1 << SEQUENCE_BITS);

    //每一部分的偏移量
    //时间戳42--机房号5--机器号5--序列号12
    //例如：一个64的结构为：101010101010101010101010101010101010101 0101010 101010 101010101010  64位
    public static final long TIMESTAMP_LEFT_SHIFT = DATA_CENTER_ID_BITS + MACHINE_ID_BITS + SEQUENCE_BITS;
    public static final long DATA_CENTER_ID_SHIFT = MACHINE_ID_BITS + SEQUENCE_BITS;
    public static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;

    //数据中心
    private long dataCenterId;
    //机器标识
    private long machineId;
    //序列号
    private LongAdder sequenceId = new LongAdder();
    //时钟回拨的问题，需要处理
    private long lastStamp = -1L;

    public IdGenerator(long dataCenterId, long machineId) {
        //检查机房号和机器号是否超过了最大值
        if(dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0 || machineId > MAX_MACHINE_ID || machineId < 0) {
            throw new IllegalArgumentException("机房id或机器id不合法");
        }
        this.dataCenterId = dataCenterId;
        this.machineId = machineId;
    }

    public long getId() {
        //1.处理时间戳
        long currentStamp = System.currentTimeMillis();
        //如果当前时间小于上一次的时间戳，说明时钟回拨了
        if (currentStamp < lastStamp) {
            throw new RuntimeException("服务器出现了时钟回拨问题!");
        }
        //并发量大，如果是同一时间戳内，需要处理序列号
        if (currentStamp == lastStamp) {
            //线程安全的自增
            sequenceId.increment();
            //一瞬间的并发量很大，如果序列号甚至超过了最大值
            if (sequenceId.sum() > MAX_SEQUENCE) {
                //等待下一毫秒
                while (currentStamp == lastStamp) {
                    //之所以使用while循环，是因为有可能在这个循环中，执行得很快，导致仍然currentStamp == lastStamp
                    currentStamp = System.currentTimeMillis();
                }

            }
        }
        if(currentStamp != lastStamp) {
            //这里不用else是因为，sequenceId.sum() > MAX_SEQUENCE的情况下，也需要重新设置sequenceId
            //如果是不同时间戳
            sequenceId.reset();
        }
        lastStamp = currentStamp;
        //2.生成id
        return (currentStamp - START_STAMP) << TIMESTAMP_LEFT_SHIFT
                | dataCenterId << DATA_CENTER_ID_SHIFT
                | machineId << MACHINE_ID_SHIFT
                | sequenceId.sum();
    }

    public static void main(String[] args) {
        IdGenerator idGenerator = new IdGenerator(1, 2);
        for (int i = 0; i < 1000; i++) {
            new Thread(()  ->{System.out.println(idGenerator.getId());}).start();
        }

    }


}
