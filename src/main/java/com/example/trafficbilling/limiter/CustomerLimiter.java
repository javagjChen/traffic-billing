package com.example.trafficbilling.limiter;


import java.util.Collections;
import java.util.List;
import java.util.UUID;

import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("customerLimiter")
public class CustomerLimiter extends Limiter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public CustomerLimiter() {
    }

    public boolean shouldBeLimited(String prefixKey, int limitCount, int limitPeriod) {
        if (StringUtils.isBlank(prefixKey)) {
            this.logger.error("自定义限流发生错误,缓存key不能为空");
        } else {
            List<String> keys = Collections.singletonList(prefixKey);
            Long result = (Long) this.redisTemplate.execute(this.redisScript, keys, new Object[]{limitCount, limitPeriod, System.currentTimeMillis(), UUID.randomUUID().toString()});
            if (result == 0L) {
                this.logger.warn("缓存key:[{}]触发自定义限流：限流周期:[{}], 限流阈值:[{}]", prefixKey, limitPeriod, limitCount);
                return true;
            }
        }

        return false;
    }
}
