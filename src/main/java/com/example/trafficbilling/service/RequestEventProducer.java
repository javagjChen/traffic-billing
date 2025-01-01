package com.example.trafficbilling.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class RequestEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public RequestEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendRequestEvent(String userId, String apiKey) {
        String topic = "api-requests";
        String message = userId + "," + apiKey;
        kafkaTemplate.send(topic, message);
    }
}
