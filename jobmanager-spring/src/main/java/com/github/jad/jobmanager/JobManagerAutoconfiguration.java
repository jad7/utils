package com.github.jad.jobmanager;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Map;

@Configuration
public class JobManagerAutoconfiguration {


    @Bean(destroyMethod = "stopAll")
    @ConditionalOnMissingBean
    public JobManager jobManager(@Value("${spring.jobmanager.prefix:__default__}") String prefix,
                                 ApplicationContext applicationContext, Environment environment) {
        return new SpringJobManager(applicationContext, environment, prefix);
    }

    @Autowired
    public void registerJobs(JobManager jobManager, @Autowired(required = false) Map<String, Flow> flows) {
        if (flows != null) {
            for (Map.Entry<String, Flow> flow : flows.entrySet()) {
                Flow flowValue = flow.getValue();
                if (flowValue instanceof AbstractSpringFlow) {
                    ((AbstractSpringFlow) flowValue).setStringBeanName(flow.getKey());
                }
                jobManager.addFlow(flowValue);
            }
        }
    }

}
