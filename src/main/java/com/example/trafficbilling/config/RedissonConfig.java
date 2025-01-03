package com.example.trafficbilling.config;


import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // 配置 Redis 单节点模式
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379") // 替换为你的 Redis 地址
                .setPassword(null)                  // 如果 Redis 设置了密码，填入密码；否则设置为 null
                .setConnectionPoolSize(10)          // 连接池大小，可根据需求调整
                .setConnectionMinimumIdleSize(2);   // 最小空闲连接数

        return Redisson.create(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericToStringSerializer<>(Object.class));
        return template;
    }
}

