package com.example.trafficbilling.limiter;


import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.micrometer.common.util.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Order(10)
public class RateLimitInterceptor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private Map<String, Map<String, Object>> rateLimitConfig = new HashMap<>();

    private final String limitTipMsg = "操作过于频繁，请稍后再试";
    private static final String LIMIT_PERIOD = "period";
    private static final String LIMIT_COUNT = "count";
    private static final String LIMIT_DISABLE = "disable";
    @Autowired
    private Map<String, Limiter> limiterMap;

    private static final ExpressionParser parser = new SpelExpressionParser();

    public RateLimitInterceptor() {
    }

    @Around("execution(public * *(..)) && @annotation(com.example.trafficbilling.limiter.RateLimit)")
    public Object interceptor(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature)pjp.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + ":" + method.getName();
        RateLimit limitAnnotation = method.getAnnotation(RateLimit.class);
        LimitType limitType = limitAnnotation.limitType();
        String key = limitAnnotation.key();
        int period = limitAnnotation.period();
        int count = limitAnnotation.count();
        if (StringUtils.isBlank(key)) {
            this.logger.error("限流切面发生错误,限流缓存key为空, 所在方法:{}", methodName);
            return pjp.proceed();
        } else {
            boolean disable = limitAnnotation.disable();
            if (this.rateLimitConfig != null && this.rateLimitConfig.containsKey(key)) {
                Map<String, Object> configDetailMap = this.rateLimitConfig.get(key);
                if (configDetailMap.containsKey("period")) {
                    period = (Integer)configDetailMap.get("period");
                }

                if (configDetailMap.containsKey("count")) {
                    count = (Integer)configDetailMap.get("count");
                }

                if (configDetailMap.containsKey("disable")) {
                    disable = (Boolean)configDetailMap.get("disable");
                }
            }

            if (disable) {
                return pjp.proceed();
            } else {
                Limiter limiter = null;
                switch (limitType) {
                    case IP:
                        limiter = this.limiterMap.get("ipLimiter");
                        break;
                    case CUSTOMER:
                        limiter = this.limiterMap.get("customerLimiter");
                }

                if (limiter == null) {
                    this.logger.error("限流切面发生错误,获取不到类型:[{}]的限流器, 限流key:{}, 所在方法:{}", new Object[]{limitType, key, methodName});
                    return pjp.proceed();
                } else {
                    boolean shouldBeLimited = false;

                    String msg;
                    try {
                        msg = this.getLimitKey(pjp, signature, key, limitAnnotation.extKey());
                        shouldBeLimited = limiter.shouldBeLimited(msg, count, period);
                    } catch (Exception e) {
                        this.logger.error("限流切面发生异常, 限流key:{}, 所在方法:{}, e:{}", key, methodName, e.getMessage());
                    }

                    if (shouldBeLimited) {
                        msg = this.limitTipMsg;
                        if (StringUtils.isNotBlank(limitAnnotation.message())) {
                            msg = limitAnnotation.message();
                        }

                        throw new RuntimeException(msg);
                    } else {
                        return pjp.proceed();
                    }
                }
            }
        }
    }

    private String getLimitKey(ProceedingJoinPoint joinPoint, MethodSignature signature, String key, String extKey) {
        String limitKey = "rateLimit:";
        if (StringUtils.isNotBlank(extKey)) {
            String[] parameterNamesArr = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            Map<String, Object> variables = new HashMap(args.length);

            for(int i = 0; i < args.length; ++i) {
                variables.put(parameterNamesArr[i], args[i]);
            }

            limitKey = limitKey + key + ":" + this.evaluate(extKey, null, variables, String.class);
        } else {
            limitKey = limitKey + key;
        }

        return limitKey;
    }

    public <T> T evaluate(String expressionString, Object rootObject, Map<String, Object> variables, Class<T> resultType) {
        StandardEvaluationContext context = new StandardEvaluationContext(rootObject);
        if (variables != null) {
            Iterator var5 = variables.entrySet().iterator();

            while(var5.hasNext()) {
                Map.Entry<String, Object> entry = (Map.Entry)var5.next();
                context.setVariable(entry.getKey(), entry.getValue());
            }
        }

        Expression expression = parser.parseExpression(expressionString);
        return expression.getValue(context, resultType);
    }
}

