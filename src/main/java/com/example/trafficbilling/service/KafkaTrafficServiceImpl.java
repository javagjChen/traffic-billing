package com.example.trafficbilling.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaTrafficServiceImpl implements ITrafficService{

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;


    @Override
    public String handleRequest(String userId, String apiKey) {
        String topic = "api-requests";
        String message = userId + "," + apiKey;
        kafkaTemplate.send(topic, message);
        return "just receive";
    }
}
