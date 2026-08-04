package com.citybooking.server.lock;

import com.citybooking.server.common.BizException;
import com.citybooking.server.common.ResultCode;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@Profile("prod")
public class RedissonLock implements DistributedLock {

    private final RedissonClient redissonClient;

    public RedissonLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> T withLock(String key, long leaseSeconds, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock("lock:" + key);
        try {
            if (!lock.tryLock(0, leaseSeconds, TimeUnit.SECONDS)) {
                throw new BizException(ResultCode.CONFLICT, "操作冲突，请重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.INTERNAL, "获取锁被中断");
        }
        try {
            return supplier.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public boolean tryLock(String key, long leaseSeconds) {
        RLock lock = redissonClient.getLock("lock:" + key);
        try {
            return lock.tryLock(0, leaseSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(String key) {
        RLock lock = redissonClient.getLock("lock:" + key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
