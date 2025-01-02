package com.example.trafficbilling.consumer;

import jakarta.annotation.PostConstruct;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Properties;

@Component
public class KafkaTrafficProcessor {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.streams.input-topic}")
    private String inputTopic;

    @Value("${maxRequestPerMin}")
    private Long maxRequestPerMin;

    @PostConstruct
    public void startStreamProcessor() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "rate-limiter-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();

        // 消费请求日志
        KStream<String, String> requestStream = builder.stream(inputTopic);

        // 按用户和 API 分组，统计每分钟请求数
        requestStream
                .mapValues(value -> {
                    String[] parts = value.split(",");
                    return parts[0] + ":" + parts[1]; // userId:apiKey
                })
                .groupBy((key, value) -> value)
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
                .count(Materialized.as("rate-limiting-store"))
                .toStream()
                .foreach((key, count) -> {
                    String userApiKey = key.key();
                    String[] split = userApiKey.split(":");
                    String userId = split[0];
                    String apiKey = split[1];
                    // 转换为分钟数
                    if (count > maxRequestPerMin){
                        System.out.println("Kafka Rate limit exceeded for user " + userId + " on " + apiKey);
                    }else {
                        System.out.println("Kafka Rate limit Access for user " + userId + " on " + apiKey);
                    }
        });

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();
    }
}
