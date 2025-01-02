package com.example.trafficbilling.service;

import io.micrometer.common.util.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class RedisTrafficServiceImpl implements ITrafficService{

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    //每分钟最大处理请求数
    @Value("${maxRequestPerMin}")
    private Long maxRequestPerMin;

    public Long getAndIncrementRequestCount(String userApiKey, String windowStart, long maxRequestsPerMinute) {
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
                    return ++count;
                }
                // 更新计数并设置过期时间
                redisTemplate.opsForValue().increment(key);
                redisTemplate.expire(key, 60, TimeUnit.SECONDS);
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
        //System.out.println("当前线程:" + Thread.currentThread().getName() + ",key:" + key + ",count:" + value);
        return Long.parseLong(value);
    }

    @Override
    public String handleRequest(String userId, String apiKey) {
        String userApiKey = userId + ":" + apiKey;
        long windowStartMillis = System.currentTimeMillis();
        // 转换为分钟数
        long minutesSinceEpoch = Duration.ofMillis(windowStartMillis).toMinutes();

//        long currentCount = this.getAndIncrementRequestCount(userApiKey, String.valueOf(minutesSinceEpoch), maxRequestPerMin);
        String key = "rate_limit:" + userId + ":" + apiKey;
        Long currentCount = redisTemplate.opsForValue().increment(key);
        long TIME_WINDOW_SECONDS = 60;
        if (currentCount == 1) {
            redisTemplate.expire(key, TIME_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        if (currentCount > maxRequestPerMin) {
            return "Redis Rate limit exceeded for user " + userId + " on " + apiKey;
        }
        return "Redis Rate limit Access for user " + userId + " on " + apiKey;
    }
}
