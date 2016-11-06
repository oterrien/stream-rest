package com.ote.test;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Aspect
public class TraceableAspect {

    private static final AtomicLong COUNT = new AtomicLong(0);

    @Around("execution(* *(..)) && @annotation(com.ote.test.Traceable)")
    public Object execute(ProceedingJoinPoint point) throws Throwable {

        final Logger logger = LoggerFactory.getLogger(point.getTarget().getClass());

        if (logger.isTraceEnabled()) {
            return executeWithTrace(point, logger);
        } else {
            return point.proceed();
        }
    }

    private Object executeWithTrace(ProceedingJoinPoint point, Logger logger) throws Throwable {
        long start = System.currentTimeMillis();
        long count = COUNT.incrementAndGet();
        String methodName = point.getSignature().getName();
        String parameters = Stream.of(point.getArgs()).map(Object::toString).collect(Collectors.joining(","));
        try {
            logger.trace(String.format("####-%d-START- %s(%s) ", count, methodName, parameters));
            return point.proceed();
        } catch (Throwable e) {
            logger.error(String.format("####-%d-ERROR- %s(%s) ", count, methodName, parameters), e);
            throw e;
        } finally {
            long end = System.currentTimeMillis();
            logger.trace(String.format("####-%d-END  - %s(%s) in %sms", count, methodName, parameters, (end - start)));
        }
    }
}