package com.example.trafficbilling.config;

import com.example.trafficbilling.service.ITrafficService;
import com.example.trafficbilling.service.JVMTrafficServiceImpl;
import com.example.trafficbilling.service.KafkaTrafficServiceImpl;
import com.example.trafficbilling.service.RedisTrafficServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ServiceConfig {

    @Bean
    public Map<String, ITrafficService> serviceMap(KafkaTrafficServiceImpl kafkaTrafficService,
                                                   RedisTrafficServiceImpl redisTrafficService,
                                                   JVMTrafficServiceImpl jvmTrafficService) {
        Map<String, ITrafficService> map = new HashMap<>();
        map.put("kafka", kafkaTrafficService);
        map.put("redis", redisTrafficService);
        map.put("jvm", jvmTrafficService);
        return map;
    }
}
