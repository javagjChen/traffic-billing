package com.example.trafficbilling.service;

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

    @Value("${spring.kafka.streams.output-topic}")
    private String outputTopic;

    private final RedisTrafficStorage redisStorage;

    public KafkaTrafficProcessor(RedisTrafficStorage redisStorage) {
        this.redisStorage = redisStorage;
    }

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
                .count(Materialized.as("request-counts-store"))
                .toStream().foreach((key, count) -> {
                    // 将统计结果写入 Redis
                    String userApiKey = key.key();
                    // 获取窗口起始时间（毫秒）
                    long windowStartMillis = key.window().start();
                    // 转换为分钟数
                    long minutesSinceEpoch = Duration.ofMillis(windowStartMillis).toMinutes();
                    redisStorage.storeRequestCount(userApiKey, String.valueOf(minutesSinceEpoch), count);
        });

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();
    }
}
