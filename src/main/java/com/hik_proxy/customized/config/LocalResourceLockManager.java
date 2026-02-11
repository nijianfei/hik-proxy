package com.hik_proxy.customized.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class LocalResourceLockManager {

    private static final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    /**
     * 尝试获取锁（等待指定时间）
     * @param resourceKey 资源标识
     * @param waitTime 等待时间（秒）
     * @return 锁标识，null表示获取失败
     */
    public boolean tryLock(String resourceKey, long waitTime) {
        ReentrantLock lock = lockMap.computeIfAbsent(resourceKey, k -> new ReentrantLock());

        try {
            if (!lock.tryLock(waitTime, TimeUnit.SECONDS)) {
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }


    /**
     * 释放锁
     */
    public void unlock(String resourceKey) {
        if (StringUtils.isBlank(resourceKey)) {
            return;
        }
        ReentrantLock lock = lockMap.get(resourceKey);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            lockMap.remove(resourceKey);
        }
    }
}
