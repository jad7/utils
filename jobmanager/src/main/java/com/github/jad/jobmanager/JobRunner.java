package com.github.jad.jobmanager;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 *
 * @author Illia Krokhmalov <jad7kii@gmail.com>
 * @since 12/2018
 */

@Slf4j
public class JobRunner {

    private final AtomicReference<Wrapper<Status>> status = new AtomicReference<>(new WrapperImpl<>(Status.NOT_STARTED));

    private final Supplier<? extends Flow> flowSupplier;
    private final JobManager jobManager;
    private volatile Context context;
    private Flow currentFlow;


    public JobRunner(Supplier<? extends Flow> flowSupplier, JobManager jobManager) {
        this.flowSupplier = flowSupplier;
        this.jobManager = jobManager;
    }

    public Wrapper<Status> getStatus() {
        return status.get();
    }

    public boolean setStatus(Wrapper<Status> prev, Status newStatus) {
        requireNonNull(prev, "Prev status can not be null");
        requireNonNull(newStatus, "New status can not be null");
        return status.compareAndSet(prev, new WrapperImpl<>(newStatus));
    }

    public void run(Context context) {
        requireNonNull(context, "Context can not be null");
        this.context = context;
        try {
            currentFlow = flowSupplier.get();
            requireNonNull(currentFlow, "Supplier can not produce 'null' flow");
            currentFlow.run(context);
            Wrapper<Status> resultStatus = getStatus();
            if (resultStatus.getObject() == Status.IN_PROGRESS) {
                if (!setStatus(resultStatus, Status.SUCCESS)) {
                    log.warn("Can not set status 'SUCCESS' for job {}, because of some concurrent process changed it from IN_PROGRESS to {}"
                            , context.getName(), getStatus().getObject());
                }
            }
        } catch (Exception e) {
            JobRunner.Wrapper<JobRunner.Status> status1 = getStatus();
            if (status1.getObject() == Status.IN_PROGRESS) {
                if (!setStatus(status1, Status.FAILED)) {
                    log.warn("Can not set status 'FAILED' for job {}, because of some concurrent process", context.getName());
                }
            }
            log.warn("Job {} failed.", context.getName(), e);
        }

    }

    public void pause() throws PausingJobException { //TODO async (Future)
        if (currentFlow.isPausable()) {
            String name = context == null ? "{NOT_INITIALIZED}" : context.getName();
            PausableFlow pausableFlow = (PausableFlow) currentFlow;
            Wrapper<Status> status = getStatus();
            if (status.getObject() == Status.IN_PROGRESS) {
                if (!setStatus(status, Status.PAUSING)) {

                    throw new PausingJobException(format("Can not set status 'PAUSING' for job %s, because of some concurrent process changed it from IN_PROGRESS to %s"
                            , name, getStatus().getObject().name()));

                }
                pausableFlow.pause();
                status = getStatus();
                if (!setStatus(status, Status.PAUSED)) {
                    throw new PausingJobException(format("Can not pause job %s, because of some concurrent process changed it from 'PAUSING' to %s"
                            , name, getStatus().getObject().name()));
                }
            } else {
                throw new PausingJobException(format("Can not pause job %s from state: %s", name, status.getObject()));
            }
        } else {
            throw new IllegalStateException("You trying to pause unpausable flow");
        }
    }

    public void resume() {
        if (currentFlow.isPausable()) {
            PausableFlow pausableFlow = (PausableFlow) currentFlow;
            Wrapper<Status> status = getStatus();
            String name = context.getName();
            if (status.getObject() == Status.PAUSED) {
                pausableFlow.resume();
                status = getStatus();
                if (!setStatus(status, Status.IN_PROGRESS)) {
                    throw new PausingJobException(format("Can not resume job %s, because of some concurrent process changed it from 'PAUSED' to %s"
                            , name, getStatus().getObject().name()));
                }
            } else {
                throw new PausingJobException(format("Can not resume job %s, which is not paused (from state: %s)", name, status.getObject()));
            }
        } else {
            throw new IllegalStateException("You trying to pause unpausable flow");
        }

    }

    public enum Status {
        NOT_STARTED(true),
        IN_PROGRESS,
        PAUSING,
        PAUSED,
        STOPPED(true),
        FAILED(true),
        SUCCESS(true)
        ;
        @Getter
        private final boolean terminated;

        Status() {
            terminated = false;
        }

        Status(boolean terminated) {
            this.terminated = terminated;
        }
    }

    public interface Wrapper<T> {
        T getObject();
    }

    //Solution for A->B->A problem
    @AllArgsConstructor
    private class WrapperImpl<T> implements Wrapper<T> {
        @Getter
        final T object;
    }
}
