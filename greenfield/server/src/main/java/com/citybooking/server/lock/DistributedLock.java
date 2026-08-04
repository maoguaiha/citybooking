package com.citybooking.server.lock;

import java.util.function.Supplier;

/**
 * 分布式锁抽象。开发/测试用本地实现，生产切换 Redisson 实现（见 LockConfig）。
 */
public interface DistributedLock {

    /**
     * 获取锁并执行任务；获取失败（被占用）抛出 BizException(CONFLICT)。
     */
    <T> T withLock(String key, long leaseSeconds, Supplier<T> supplier);

    boolean tryLock(String key, long leaseSeconds);

    void unlock(String key);
}
