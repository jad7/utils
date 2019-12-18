package com.github.jad.jobmanager;

import lombok.Data;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import rx.Scheduler;

import java.io.PrintStream;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author Illia Krokhmalov <jad7kii@gmail.com>
 * @since 12/2018
 */
@Data
public class Context {
    private Tracker tracker;
    private JobManager jobManager;
    private ApplicationContext applicationContext;
    private Environment environment;
    private String name;
    private PrintStream errorStream;
    private AtomicLong fails;
    private Config config;
    private AbstractFlow abstractFlow;
    private final ConcurrentMap<String, Object> state = new ConcurrentHashMap<>();

    public void addFails(long count) {
        fails.addAndGet(count);
    }

    public int threads(String threads) {
        Objects.requireNonNull(abstractFlow, "For using this method your flow should extends AbstractFlow");
        return abstractFlow.threads(threads);
    }

    public int buffer(String buffer) {
        Objects.requireNonNull(abstractFlow, "For using this method your flow should extends AbstractFlow");
        return abstractFlow.buffer(buffer);
    }

    public Scheduler scheduler(String name) {
        Objects.requireNonNull(abstractFlow, "For using this method your flow should extends AbstractFlow");
        return abstractFlow.scheduler(name);
    }
}
