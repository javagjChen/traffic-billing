package com.example.trafficbilling.limiter;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RateLimit {
    String key();

    String extKey() default "";

    int period();

    int count();

    boolean disable() default false;

    String message() default "";

    LimitType limitType() default LimitType.CUSTOMER;
}
