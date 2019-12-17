
package com.github.jad.utils.dto;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;


public class CustomThreadFactory implements ThreadFactory {
    private final AtomicLong threadCounter;

    private final String namingPattern;

    private final Boolean daemon;

    public CustomThreadFactory(String namingPattern, Boolean daemon) {
        this.namingPattern = namingPattern;
        this.daemon = daemon;
        threadCounter = new AtomicLong();
    }



    public final String getNamingPattern() {
        return namingPattern;
    }

    public final Boolean getDaemonFlag() {
        return daemon;
    }



    @Override
    public Thread newThread(final Runnable runnable) {
        final Thread thread = Executors.defaultThreadFactory().newThread(runnable);
        initializeThread(thread);

        return thread;
    }

    private void initializeThread(final Thread thread) {

        if (getNamingPattern() != null) {
            final Long count = threadCounter.incrementAndGet();
            thread.setName(String.format(getNamingPattern(), count));
        }

        if (getDaemonFlag() != null) {
            thread.setDaemon(getDaemonFlag());
        }
    }


}
