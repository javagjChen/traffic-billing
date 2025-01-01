package com.example.trafficbilling.service;

import io.micrometer.common.util.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisTrafficStorage {

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;

    public RedisTrafficStorage(StringRedisTemplate redisTemplate, RedissonClient redissonClient) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
    }

    public void storeRequestCount(String userApiKey, String windowStart, Long count) {
        String key = "rate_limiter:" + userApiKey + ":" + windowStart;
        String lockKey = "lock:" + userApiKey + ":" + windowStart;

        // 获取分布式锁
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁，设置等待时间和超时时间
            if (lock.tryLock(5, 2, TimeUnit.SECONDS)) {
                redisTemplate.opsForValue().set(key, String.valueOf(count), 1, TimeUnit.MINUTES);
            } else {
                throw new RuntimeException("Rate limiter lock acquisition failed");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Rate limiter interrupted while acquiring lock", e);
        } finally {
            // 释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }




    public Long getAndIncrementRequestCount(String userApiKey, String windowStart, int maxRequestsPerMinute) {
        String key = "rate_limiter:" + userApiKey + ":" + windowStart;
        String lockKey = "lock:" + userApiKey + ":" + windowStart;

        // 获取分布式锁
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁，设置等待时间和超时时间
            if (lock.tryLock(5, 2, TimeUnit.SECONDS)) {
                // 获取当前计数
                Long count = this.getRequestCount(userApiKey, windowStart);
                if (count >= maxRequestsPerMinute) {
                    return count; // 超过限流，不增加计数
                }

                // 更新计数并设置过期时间
                redisTemplate.opsForValue().increment(key);
                redisTemplate.expire(key, 1, TimeUnit.MINUTES);
                return count + 1;
            } else {
                throw new RuntimeException("Rate limiter lock acquisition failed");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Rate limiter interrupted while acquiring lock", e);
        } finally {
            // 释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }




    public Long getRequestCount(String userApiKey, String windowStart) {
        String key = "rate_limiter:" + userApiKey + ":" + windowStart;
        String value = redisTemplate.opsForValue().get(key);
        if (StringUtils.isBlank(value)){
            return 0L;
        }
        System.out.println("当前线程:" + Thread.currentThread().getName() + ",key:" + key + ",count:" + value);
        return Long.parseLong(value);
    }
}
