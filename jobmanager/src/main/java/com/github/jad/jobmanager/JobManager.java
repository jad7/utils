package com.github.jad.jobmanager;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static com.github.jad.utils.CommonUtils.isNotBlank;
import static com.github.jad.utils.CommonUtils.threadFactory;
import static java.lang.String.format;
import static java.util.concurrent.Executors.newCachedThreadPool;

/**
 *
 * @author Illia Krokhmalov <jad7kii@gmail.com>
 * @since 12/2018
 */

@Slf4j
public class JobManager {

    private final Map<String, JobRunner> jobRunnerMap = new ConcurrentHashMap<>();

    private ExecutorService executorService = newCachedThreadPool(threadFactory("jobManager"));

    protected PropertySource propertySource;

    public JobManager(PropertySource propertySource) {
        this.propertySource = propertySource;
    }

    public void addFlowFactory(String name, Supplier<? extends Flow> flowSupplier) {
        Objects.requireNonNull(name);
        if (jobRunnerMap.putIfAbsent(name.toLowerCase(), new JobRunner(flowSupplier, this)) != null) {
            throw new IllegalArgumentException(format("Job with name %s already exist", name));
        }
        log.info("Job with name {} has been registered", name);
    }

    public void addFlow(Flow flow) {
        String name = flow.getName();
        Objects.requireNonNull(name);
        if (jobRunnerMap.putIfAbsent(name.toLowerCase(), new JobRunner(() -> flow, this)) != null) {
            throw new IllegalArgumentException(format("Job with name %s already exist", name));
        }
        log.info("Job with name {} has been registered", name);
    }

    public void start(String jobName) {
        Objects.requireNonNull(jobName);
        final JobRunner jobRunner = jobRunnerMap.get(jobName.toLowerCase());
        if (jobRunner == null) {
            throw new IllegalArgumentException(format("Job with name %s not registered", jobName));
        }
        final JobRunner.Wrapper<JobRunner.Status> statusWrapper = jobRunner.getStatus();
        JobRunner.Status status = statusWrapper.getObject();
        if (!status.isTerminated()) {
            throw new IllegalStateException(format("You trying to initSchedule job %s with status %s", jobName, status.name()));
        }
        final Exchanger<String> startingStatus = new Exchanger<>();
        executorService.submit(() -> {
            if (!jobRunner.setStatus(statusWrapper, JobRunner.Status.IN_PROGRESS)) {
                try {
                    startingStatus.exchange("Job status has been changed");
                } catch (InterruptedException e) {
                    log.warn("Thread {} interrupted", Thread.currentThread().getName(), e);
                }
                return;
            }
            try {
                startingStatus.exchange("");
            } catch (InterruptedException e) {
                log.warn("Thread {} interrupted", Thread.currentThread().getName(), e);
                return;
            }
            jobRunner.run(createContext(jobName));
        });
        try {
            String exchange = startingStatus.exchange("");
            if (isNotBlank(exchange)) {
                throw new IllegalStateException(format("Can not initSchedule job %s because: %s", jobName, exchange));
            }
        } catch (InterruptedException e) {
            log.warn("Thread {} interrupted", Thread.currentThread().getName(), e);

        }
    }

    protected Context createContext(String jobName) {
        Context context = new Context();
        context.setTracker(new Tracker());
        context.setJobManager(this);
        context.setName(jobName);
        Config config = new Config( jobName + ".", propertySource);
        context.setConfig(config);
        return context;
    }

    public JobRunner getJob(String jobName) {
        return jobRunnerMap.get(jobName);
    }

    public List<String> getJobNames() {
        return new ArrayList<>(jobRunnerMap.keySet());
    }

    public void stopAll() {
        executorService.shutdownNow();
    }
}
