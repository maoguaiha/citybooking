package com.citybooking.server.lock;

import com.citybooking.server.common.BizException;
import com.citybooking.server.common.ResultCode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
@Profile("!prod")
public class LocalLock implements DistributedLock {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T withLock(String key, long leaseSeconds, Supplier<T> supplier) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new BizException(ResultCode.CONFLICT, "操作冲突，请重试");
        }
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean tryLock(String key, long leaseSeconds) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        return lock.tryLock();
    }

    @Override
    public void unlock(String key) {
        ReentrantLock lock = locks.get(key);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
