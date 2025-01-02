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
class RedisTrafficTests {

    @Autowired
    private ApiController apiController;

    @Test
    public void testRedisHighConcurrency() throws InterruptedException {
        String userId = "user1";
        int requestsPerSecond = 1500;
        int testDurationSeconds = 60;

        ExecutorService executorService = Executors.newFixedThreadPool(50);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();
        StopWatch sw = new StopWatch();
        sw.start("user1mock高并发");
        for (int i = 0; i < testDurationSeconds; i++) {
            for (int j = 0; j < requestsPerSecond; j++) {
                executorService.submit(() -> {
                    String response;
                    int randomKey = ((int) (Math.random() * 3) + 1);
                    if (randomKey == 1){
                        response = apiController.getApi1(userId,"redis");
                    }else if (randomKey == 2){
                        response = apiController.putApi3(userId,"redis");
                    }else {
                        response = apiController.postApi2(userId,"redis");
                    }
                    if (response.contains("Access")) {
                        successCount.getAndIncrement();
                    } else {
                        failureCount.getAndIncrement();
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
        assertThat(failureCount.get()).isEqualTo(0);
    }

}
