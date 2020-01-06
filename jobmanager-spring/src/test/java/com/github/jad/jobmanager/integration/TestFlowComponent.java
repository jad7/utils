package com.github.jad.jobmanager.integration;

import com.github.jad.jobmanager.AbstractFlow;
import com.github.jad.jobmanager.Config;
import com.github.jad.jobmanager.Context;
import com.github.jad.jobmanager.Tracker;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

@Component
public class TestFlowComponent extends AbstractFlow {
    public static Semaphore semaphore = new Semaphore(0);

    @Override
    public void safeRun(Context context, Tracker tracker, Config config) {
        semaphore.release();
    }
}
