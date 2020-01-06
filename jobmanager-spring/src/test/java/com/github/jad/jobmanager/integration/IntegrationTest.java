package com.github.jad.jobmanager.integration;

import com.github.jad.jobmanager.JobManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
        classes = AutoconfigurationApplication.class)
public class IntegrationTest {

    @Autowired
    JobManager jobManager;

    @Autowired
    Semaphore test1Semaphore;

    @Test
    public void positive() throws InterruptedException {
        jobManager.start(TestFlowComponent.class.getSimpleName());
        boolean released = TestFlowComponent.semaphore.tryAcquire(10, TimeUnit.SECONDS);
        Assert.assertTrue(released);
    }
}
