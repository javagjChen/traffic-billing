package com.example.trafficbilling.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class JVMTrafficServiceImpl implements ITrafficService{

    @Value("${maxRequestPerMin}")
    private Long maxRequestPerMin;
    private static final long TIME_WINDOW_MS = 60 * 1000; // 时间窗口（毫秒）
    private final Map<String, UserRateLimit> userRateLimits = new ConcurrentHashMap<>();

    @Override
    public String handleRequest(String userId, String apiKey) {
        String userApiKey = userId + ":" + apiKey;
        UserRateLimit rateLimit = userRateLimits.computeIfAbsent(userApiKey, UserRateLimit::new);
        synchronized (rateLimit) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - rateLimit.startTime > TIME_WINDOW_MS) {
                rateLimit.reset(currentTime); // 超过时间窗口，重置计数器
            }
            if (rateLimit.requestCount.incrementAndGet() > maxRequestPerMin) {
                return "JVM Rate limit exceeded for user " + userId + " on " + apiKey;
            }
        }
        return "JVM Rate limit Access for user " + userId + " on " + apiKey;
    }

    static class UserRateLimit {
        long startTime; // 当前窗口的起始时间
        AtomicInteger requestCount; // 请求计数器

        UserRateLimit(String userApiKey) {
            this.startTime = System.currentTimeMillis();
            this.requestCount = new AtomicInteger(0);
        }

        void reset(long newStartTime) {
            this.startTime = newStartTime;
            this.requestCount.set(0);
        }
    }
}
