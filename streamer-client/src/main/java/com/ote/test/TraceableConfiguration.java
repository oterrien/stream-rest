package com.ote.test;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;

@Profile("with-trace-aop")
@Configuration
public class TraceableConfiguration extends TraceableAspect {

    @PostConstruct
    public void init() {

        LoggerFactory.getLogger(this.getClass()).warn("####- TraceableConfiguration is enabled");

    }
}
