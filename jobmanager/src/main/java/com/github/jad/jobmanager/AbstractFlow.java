package com.github.jad.jobmanager;

import com.github.jad.utils.Memoizer;
import lombok.experimental.Delegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.github.jad.utils.CommonUtils.threadFactory;
import static com.github.jad.utils.FunctionalUtils.curry2;
import static com.github.jad.utils.FunctionalUtils.supToFun;


/**
 *
 * @author Illia Krokhmalov <jad7kii@gmail.com>
 * @since 12/2018
 */
public abstract class AbstractFlow implements Flow, DisposableBean {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final static BiFunction<String, String, String> WHOLE_PATTERN
            = (step, type) -> "steps." + step + "." + type;
    private static final Function<String, String> threadPattern = curry2(WHOLE_PATTERN, "threads");
    private static final Function<String, String> bufferPattern = curry2(WHOLE_PATTERN, "buffer");

    protected JobManager jobManager;
    protected String name;
    protected Context context;
    @Delegate
    protected Config config;
    private final Runnable mDoFinalizers = Memoizer.memoize(this::doFinalizers);

    protected AbstractFlow() {
    }

    @Override
    public String getName() {
        if (name == null) {
            return Flow.super.getName();
        }
        return name;
    }


    protected Scheduler scheduler(String name) {
        return getSchedulers().computeIfAbsent(name, k ->
            Schedulers.from(getExecutors().computeIfAbsent(k, k1 -> {
                    log.debug("For {} creating executionService", name);
                    return Executors.newCachedThreadPool(threadFactory(k1));
                }
            )));
    }


    protected Scheduler scheduler(String name, int threads) {
        return getSchedulers().computeIfAbsent(name, k ->
            Schedulers.from(getExecutors().computeIfAbsent(k, k1 -> {
                    log.debug("For {} creating {} fixed executionService", name, threads);
                    return Executors.newFixedThreadPool(threads, threadFactory(k1));
                }
            )));
    }

    protected synchronized int registerFinalizer(Runnable finalizer) {
        int num = getFinalizers().size();
        getFinalizers().add(finalizer);
        return num;
    }

    protected synchronized void unregisterFinalizer(int num) {
        getFinalizers().remove(num);
    }

    protected int threads(String stepName) {
        String property = threadPattern.apply(stepName);
        Integer threads = getProp(property, Integer.TYPE);
        if (threads == null) {
            throw new IllegalArgumentException("Property \"" + property + "\" is not defined");
        }
        return threads;
    }

    protected int buffer(String stepName) {
        String property = bufferPattern.apply(stepName);
        Integer bufferSize = getProp(property, Integer.TYPE);
        if (bufferSize == null) {
            throw new IllegalArgumentException("Property \"" + property + "\" is not defined");
        }
        return bufferSize;
    }

    @Override
    public void run(Context context) {
        this.name = context.getName();
        this.jobManager = context.getJobManager();
        this.config = context.getConfig();
        this.config.setLogger(log);
        this.context = context;
        context.setAbstractFlow(this);
        registerFinalizer(() -> getExecutors().values().forEach(ExecutorService::shutdown));
        registerFinalizer(() -> context.getTracker().summaryAndStop());
        AutowireCapableBeanFactory factory = context.getApplicationContext().getAutowireCapableBeanFactory();
        factory.autowireBean(this);
        //factory.initializeBean( bean, "bean" ); TODO Maybe
        try {
            safeRun(context, context.getTracker(), config);
        } finally {
            mDoFinalizers.run();
            this.context = null;
        }
    }



    @Override
    public boolean stop() {
        //TODO improve
        boolean success = true;
        for (ExecutorService executorService : getExecutors().values()) {
            try {
                executorService.shutdownNow();
            } catch (Exception e) {
                success = false;
                log.warn("Stopping execution job {}", getName(), e);
            }
        }
        mDoFinalizers.run();
        return success;
    }

    private void doFinalizers() {
        getFinalizers().forEach(runnable -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("Error on finalizer", e);
            }
        });
    }

    private List<Runnable> getFinalizers() {
        if (context != null) {
            return (List<Runnable>) context.getState().computeIfAbsent("__FINALIZERS__", supToFun(ArrayList::new));
        } else {
            return Collections.emptyList();
        }
    }

    private Map<String, ExecutorService> getExecutors() {
        return (Map<String, ExecutorService>) context.getState().computeIfAbsent("__EXECUTORS__", supToFun(HashMap::new));
    }

    private Map<String, Scheduler> getSchedulers() {
        return (Map<String, Scheduler>) context.getState().computeIfAbsent("__SCHEDULERS__", supToFun(HashMap::new));
    }


    @Override
    public void destroy() throws Exception {
        try {
            mDoFinalizers.run();
        } catch (Exception e) {
            log.warn("Exception on destroy flow {}", getName(), e);
        }
    }


    public abstract void safeRun(Context context, Tracker tracker, Config config);
}
