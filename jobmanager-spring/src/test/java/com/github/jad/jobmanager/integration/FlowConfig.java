package com.github.jad.jobmanager.integration;

import com.github.jad.jobmanager.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Semaphore;

@Configuration
public class FlowConfig {

    @Bean
    public Flow test1() {
        return new AbstractSpringFlow(true) {
            @Autowired
            private Semaphore test1Semaphore;
            @Override
            public void safeRun(Context context, Tracker tracker, Config config) {
                test1Semaphore.release();
            }
        };
    }

    @Bean
    public Semaphore test1Semaphore() {
        return new Semaphore(0);
    }
}
