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

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class KafkaTrafficTests {

    @Autowired
    private ApiController apiController;

    @Test
    public void testKafkaHighConcurrency() throws InterruptedException {
        String userId = "user1";
        int requestsPerSecond = 1500;
        int testDurationSeconds = 60;

        ExecutorService executorService = Executors.newFixedThreadPool(50);
        StopWatch sw = new StopWatch();
        sw.start("user1mock高并发");
        for (int i = 0; i < testDurationSeconds; i++) {
            for (int j = 0; j < requestsPerSecond; j++) {
                executorService.submit(() -> {
                    String response;
                    int randomKey = ((int) (Math.random() * 3) + 1);
                    if (randomKey == 1){
                        apiController.getApi1(userId,"kafka");
                    }else if (randomKey == 2){
                        apiController.putApi3(userId,"kafka");
                    }else {
                        apiController.postApi2(userId,"kafka");
                    }

                });
            }
            Thread.sleep(1000);
        }
        sw.stop();
        executorService.shutdown();

        System.out.println("耗时:" + sw.prettyPrint());
    }

    @Test
    void testAccessWithinLimit() {
        String userId = "user1";
        StopWatch sw = new StopWatch();
        sw.start("user1mock kafka");
        for (int j = 0; j < 100; j++) {
            apiController.getApi1(userId,"kafka");
        }
        sw.stop();
        System.out.println("耗时:" + sw.prettyPrint());
    }

    @Test
    void testAccessExceedingLimit() {
        String userId = "user2";
        StopWatch sw = new StopWatch();
        sw.start("user2mock kafka");
        for (int j = 0; j < 100; j++) {
            apiController.getApi1(userId,"kafka");
        }

        apiController.getApi1(userId,"kafka");
        sw.stop();
        System.out.println("耗时:" + sw.prettyPrint());
    }
}
