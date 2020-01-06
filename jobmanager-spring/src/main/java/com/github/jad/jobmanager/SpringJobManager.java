package com.github.jad.jobmanager;

import com.github.jad.utils.CommonUtils;
import com.github.jad.utils.FunctionalUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

public class SpringJobManager extends JobManager {

    public static final String SPRING_CONTEXT = "__SPRING_CONTEXT__";
    public static final String SPRING_ENVIRONMENT = "__SPRING_ENVIRONMENT__";
    private final ApplicationContext applicationContext;
    private final Environment environment;

    public SpringJobManager(ApplicationContext applicationContext, Environment environment,
                            String prefix) {
        super(new PropertySource() {
            private Supplier<String> lazyPrefix = FunctionalUtils.lazy(() -> {
                if (CommonUtils.isEmpty(prefix) || prefix.equals("__default__")) {
                    return "spring.jobmanager.";
                } else {
                    return prefix.endsWith(".") ? prefix : prefix + ".";
                }
            });

            @Override
            public <T> T getProperty(String key, Class<T> clazz) {
                return environment.getProperty(lazyPrefix.get() + key, clazz);
            }

            @Override
            public <T> T getProperty(String key, Class<T> clazz, T defaultValue) {
                return environment.getProperty(lazyPrefix.get() + key, clazz, defaultValue);
            }
        });
        this.applicationContext = applicationContext;
        this.environment = environment;
    }

    @Override
    protected Context createContext(String jobName) {
        Context context = super.createContext(jobName);
        context.getVariablesMap().put(SPRING_CONTEXT, applicationContext);
        context.getVariablesMap().put(SPRING_ENVIRONMENT, environment);
        return context;
    }




}
