package com.example.trafficbilling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class TrafficBillingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrafficBillingApplication.class, args);
    }

}
