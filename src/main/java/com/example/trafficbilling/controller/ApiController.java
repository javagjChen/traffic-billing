package com.example.trafficbilling.controller;

import com.example.trafficbilling.limiter.LimitType;
import com.example.trafficbilling.limiter.RateLimit;
import com.example.trafficbilling.service.ITrafficService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private Map<String, ITrafficService> serviceMap;


    @GetMapping("/api1/{type}")
    public String getApi1(@RequestParam String userId,@PathVariable String type) {
        ITrafficService iTrafficService = serviceMap.get(type);
        return iTrafficService.handleRequest(userId, "api1");
    }

    @PostMapping("/api2{type}")
    public String postApi2(@RequestParam String userId,@PathVariable String type) {
        ITrafficService iTrafficService = serviceMap.get(type);
        return iTrafficService.handleRequest(userId, "api2");
    }

    @PutMapping("/api3{type}")
    public String putApi3(@RequestParam String userId,@PathVariable String type) {
        ITrafficService iTrafficService = serviceMap.get(type);
        return iTrafficService.handleRequest(userId, "api3");
    }

    @RateLimit(limitType = LimitType.CUSTOMER, key = "test", extKey = "#userId",
            period = 60, count = 10000, message = "该用户操作过于频繁，请稍后再试")
    public String handleRequest(String userId) {
        return "Access granted for user " + userId;
    }
}
