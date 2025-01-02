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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class JVMTrafficTests {

    @Autowired
    private ApiController apiController;

    /**
     * 模拟高并发
     * @throws InterruptedException
     */
    @Test
    public void testJVMHighConcurrency() throws InterruptedException {
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
                        response = apiController.getApi1(userId,"jvm");
                    }else if (randomKey == 2){
                        response = apiController.putApi3(userId,"jvm");
                    }else {
                        response = apiController.postApi2(userId,"jvm");
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
        System.out.println("失败次数:" + failureCount.get());
        System.out.println("耗时:" + sw.prettyPrint());
        assertThat(successCount.get()).isGreaterThan(0);
        assertThat(failureCount.get()).isGreaterThan(0);
    }

    /**
     * 简单模拟限制内的请求
     */
    @Test
    void testAccessWithinLimit() {
        String userId = "user1";
        StopWatch sw = new StopWatch();
        sw.start("user1mock jvm");
        for (int j = 0; j < 100; j++) {
            String response;
            response = apiController.getApi1(userId,"jvm");
            assertEquals("JVM Rate limit Access for user user1 on api1", response);
        }
        sw.stop();
        System.out.println("耗时:" + sw.prettyPrint());
    }

    /**
     * 简单模拟限制外的请求
     */
    @Test
    void testAccessExceedingLimit() {
        String userId = "user2";
        StopWatch sw = new StopWatch();
        sw.start("user2mock jvm");
        for (int j = 0; j < 100; j++) {
            String response;
            response = apiController.getApi1(userId,"jvm");
            assertEquals("JVM Rate limit Access for user user2 on api1", response);
        }

        String response = apiController.getApi1(userId,"jvm");
        assertEquals("JVM Rate limit exceeded for user user2 on api1", response);
        sw.stop();
        System.out.println("耗时:" + sw.prettyPrint());
    }
}
