package com.example.trafficbilling;

import com.example.trafficbilling.controller.ApiController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.StopWatch;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class RedisLimitTests {

    @Autowired
    private ApiController apiController;

    @Test
    public void testRedisHighConcurrency() throws InterruptedException {
        String userId = "user1";
        int requestsPerSecond = 1500;
        int testDurationSeconds = 60;

        ExecutorService executorService = Executors.newFixedThreadPool(50);
        AtomicInteger successCount = new AtomicInteger();
        StopWatch sw = new StopWatch();
        sw.start("基于注解+redis的限流");
        for (int i = 0; i < testDurationSeconds; i++) {
            for (int j = 0; j < requestsPerSecond; j++) {
                executorService.submit(() -> {
                    String response = apiController.handleRequest(userId);
                    if (response.contains("Access")) {
                        successCount.getAndIncrement();
                    }
                });
            }
            Thread.sleep(1000);
        }
        sw.stop();
        executorService.shutdown();

        System.out.println("成功次数:" + successCount.get());
        System.out.println("总次数:" + testDurationSeconds * requestsPerSecond);
        System.out.println("耗时:" + sw.prettyPrint());
        assertThat(successCount.get()).isGreaterThan(0);
    }

}
