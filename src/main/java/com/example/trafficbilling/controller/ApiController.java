package com.example.trafficbilling.controller;

import com.example.trafficbilling.service.RedisTrafficStorage;
import com.example.trafficbilling.service.RequestEventProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private RedisTrafficStorage redisStorage;
    @Autowired
    private RequestEventProducer producer;


    @GetMapping("/api1")
    public String getApi1(@RequestParam String userId) {
        return handleRequest(userId, "api1");
    }

    @PostMapping("/api2")
    public String postApi2(@RequestParam String userId) {
        return handleRequest(userId, "api2");
    }

    @PutMapping("/api3")
    public String putApi3(@RequestParam String userId) {
        return handleRequest(userId, "api3");
    }

    private String handleRequest(String userId, String apiKey) {
        String userApiKey = userId + ":" + apiKey;
        long windowStartMillis = System.currentTimeMillis();
        // 转换为分钟数
        long minutesSinceEpoch = Duration.ofMillis(windowStartMillis).toMinutes();

        // 每分钟最大处理10000个请求
        int maxRequestsPerMinute = 100;
        Long requestCount = redisStorage.getAndIncrementRequestCount(userApiKey, String.valueOf(minutesSinceEpoch),maxRequestsPerMinute);
        if (requestCount >= maxRequestsPerMinute) {
            return "Rate limit exceeded for user " + userId + " on " + apiKey;
        }
        producer.sendRequestEvent(userId, apiKey);
        return "Access granted for user " + userId + " on " + apiKey;
    }
}
