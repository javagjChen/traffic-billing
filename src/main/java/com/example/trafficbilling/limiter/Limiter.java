package com.example.trafficbilling.limiter;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

public class Limiter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected RedisScript<Long> redisScript;
    @Autowired
    protected RedisTemplate<String, Object> redisTemplate;

    public Limiter() {
    }

    @PostConstruct
    public void init() {
        this.redisScript = new DefaultRedisScript(buildLuaScript(), Long.class);
    }


    public static String buildLuaScript() {
        return "-- KEYS[1] 是服务唯一标识\n-- ARGV[1] 是限流阈值（请求次数）\n-- ARGV[2] 是窗口大小（秒）\n-- ARGV[3] 是当前时间戳（毫秒）\n-- ARGV[4] UUID\nlocal window_start_time = ARGV[3] - ARGV[2]*1000\nredis.call('zremrangebyscore',KEYS[1],'-inf',window_start_time)\nlocal now_request = redis.call('zcard',KEYS[1])\nif now_request < tonumber(ARGV[1]) then\n redis.call('zadd',KEYS[1],ARGV[3],ARGV[4])\n redis.call('expire', KEYS[1], ARGV[2])\n return 1\nelse\n return 0\nend";
    }

    public boolean shouldBeLimited(String prefixKey, int limitCount, int limitPeriod) {
        this.logger.error("未定义的方法 shouldBeLimited");
        return false;
    }
}

