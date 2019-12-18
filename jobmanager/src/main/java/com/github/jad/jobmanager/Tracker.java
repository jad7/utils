package com.github.jad.jobmanager;

import com.github.jad.utils.CommonUtils;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.jad.utils.FunctionalUtils.curry2;
import static com.github.jad.utils.FunctionalUtils.getOrDefault;


/**
 * Thread safe implementation
 */

@Slf4j
public class Tracker {
    private static InheritableThreadLocal<Tracker> threadLocal = new InheritableThreadLocal<>();

    private static PeriodFormatter formatter = new PeriodFormatterBuilder()
            .appendDays()
            .appendSuffix("d ")
            .appendHours()
            .appendSuffix("h ")
            .appendMinutes()
            .appendSuffix("m ")
            .appendSeconds()
            .appendSuffix("s")
            .toFormatter();

    private static final int TIME_UPDATE_TIME_MS = 1000;
    private static final int PRINT_INTERVAL_TIME_MS = 5000;

    public static int FIRST_PRIORITY = 0;
    public static int LAST_PRIORITY = Integer.MAX_VALUE - 1;

    private volatile Integer totalObjectsRowCount;
    @Setter
    private volatile Function<Integer, Integer> percentCalculator = curry2(Tracker::percentCalc, (Supplier<Integer>) () -> totalObjectsRowCount);
    @Setter
    private volatile BiFunction<ConcurrentMap<String, LongAdder>,
            SortedSet<Step>, Integer> calcProcessed = getOrDefault(Tracker::getProcessed, 0);


    private final ConcurrentMap<String, LongAdder> steps = new ConcurrentHashMap<>();

    private AtomicInteger priorityProvider = new AtomicInteger(100);

    private SortedSet<Step> stepsOrder = Collections.synchronizedSortedSet(new TreeSet<>(Comparator.comparingInt(Tracker.Step::getPriority)));

    private long jobStartTime = System.currentTimeMillis();
    private Queue<TimeCount> fifo = new CircularFifoQueue<>(5); //data for last 5 sec
    private volatile long lastPrinted = 0;
    @Setter
    private String objectsName = "documents";

    private volatile String lastCollect;

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1,
            CommonUtils.threadFactory("controller-thread-%d"));

    public Tracker() {
        executorService.scheduleAtFixedRate(this::print, 1000, TIME_UPDATE_TIME_MS, TimeUnit.MILLISECONDS);
        threadLocal.set(this);
    }

    public static Tracker getCurrentTracker() {
        return threadLocal.get();
    }

    public static void processTracker(String stepName) {
        final Tracker tracker = threadLocal.get();
        if (tracker != null) {
            tracker.processed(stepName);
        }
    }

    public static void processTracker(String stepName, int add) {
        final Tracker tracker = threadLocal.get();
        if (tracker != null) {
            tracker.processed(stepName, add);
        }
    }

    private LongAdder getStep(String name) {
        return steps.computeIfAbsent(name, n -> {
            stepsOrder.add(new Tracker.Step(priorityProvider.getAndIncrement(), n));
            return new LongAdder();
        });
    }

    private LongAdder getStep(String name, int priority) {
        return steps.computeIfAbsent(name, n -> {
            stepsOrder.add(new Tracker.Step(priority, n));
            return new LongAdder();
        });
    }

    public void processed(String stepName) {
        getStep(stepName).add(1);
    }
    public void processed(String stepName, int count) {
        getStep(stepName).add(count);
    }
    public void processed(String stepName, int count, int priority) {
        getStep(stepName, priority).add(count);
    }

    public void setProcessed(String stepName, int setValue) {
        final LongAdder step = getStep(stepName);
        final int now = step.intValue();
        processed(stepName, setValue - now);
    }

    public void removeStep(String stepName) {
        steps.computeIfPresent(stepName, (name, adder) -> {
            stepsOrder.removeIf(step -> name.equals(step.getName())); //TODO
            return null;
        });
    }

    private void print() {
        try {
            final String collect = stepsOrder.stream().map(Tracker.Step::getName).map(n -> {
                final LongAdder longAdder = steps.get(n);
                if (longAdder != null) {
                    return n + ": " + longAdder.sum();
                } else {
                    return "";
                }
            }).collect(Collectors.joining(", "));

            if (!Objects.equals(lastCollect, collect)) {
                lastCollect = collect;
                int processed = calcProcessed.apply(steps, stepsOrder);
                fifo.add(new Tracker.TimeCount(System.currentTimeMillis(), processed));
                if ((System.currentTimeMillis() - lastPrinted) < PRINT_INTERVAL_TIME_MS) {
                    return;
                }
                lastPrinted = System.currentTimeMillis();
                Integer totalPercent = percentCalculator.apply(processed);
                //calculateTotalPercent(processed);

                MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
                String throughput = calcThroughput();
                String eta = calcETA(processed);
                long heapUsed = heap.getUsed() / 1024 / 1024;
                String infoString = Stream.of(
                        totalPercent == null ? "" : "Total: " + totalPercent + "%",
                        "Throughput: " + throughput + " obj/s",
                        objectsName,
                        eta == null ? "" : "ETA:" + eta,
                        "HeapUsage: " + heapUsed + "Mb",
                        collect
                )
                    .filter(CommonUtils::isNonEmpty)
                    .collect(Collectors.joining(", "));

                log.info(infoString);
            }
        } catch (Exception e) {
            log.warn("Controller exception", e);
        }
    }



    private String calcETA(long objectIndexed) {
        if (objectIndexed == 0 || totalObjectsRowCount == null) {
            return null;
        }
        long timeDelta = Math.max(1, System.currentTimeMillis() - jobStartTime);
        long etaMS = totalObjectsRowCount * timeDelta / Math.max(1, objectIndexed);
        return formatter.print(new Period(etaMS - timeDelta));
    }

    private String calcThroughput() {
        List<TimeCount> timeCounts = new ArrayList<>(fifo);
        if (timeCounts.isEmpty() || timeCounts.size() == 1) {
            return "N/A";
        }
        Tracker.TimeCount oldest = timeCounts.get(0);
        Tracker.TimeCount newest = timeCounts.get(timeCounts.size() - 1);
        long deltaTime = Math.max(1, newest.getTime() - oldest.getTime());
        long numDelta = newest.getCount() - oldest.getCount();
        return String.valueOf(numDelta * 1000 / deltaTime);
    }

    private String calculateTotalPercent(long processed) {
        if (totalObjectsRowCount == null || totalObjectsRowCount == 0) {
            return null;
        }
        //return pe
        return String.valueOf((int) (processed * 100 / totalObjectsRowCount));
    }

    public synchronized void summaryAndStop() {
        if (executorService.isShutdown()) {
            return;
        }
        executorService.shutdown();
        long deltaTime = Math.max(1, System.currentTimeMillis() - jobStartTime);
        int processed = calcProcessed.apply(steps, stepsOrder);
        log.info("Summary. Total indexed: {}. Time: {} Avg throughput: {} doc/sec",
                processed,
                formatter.print(new Period(deltaTime)),
                (processed * 1000 / deltaTime)
        );
        threadLocal.remove();
    }


    public void setTotal(int total) {
        totalObjectsRowCount = total;
    }


    @Data
    @AllArgsConstructor
    private static class TimeCount {
        private long time;
        private long count;
    }


    private static Integer percentCalc(Integer current, Integer total) {
        if (current == null || total == null || total == 0) {
            return null;
        }
        return current * 100 / total;
    }

    private static int getProcessed(ConcurrentMap<String, LongAdder> steps,
                                   SortedSet<Step> stepsOrder) {
        if (!stepsOrder.isEmpty()) {
            return Optional.ofNullable(steps.get(stepsOrder.last().getName()))
                    .map(LongAdder::intValue).orElse(0);
        } else {
            return 0;
        }
    }

    @AllArgsConstructor @ToString
    public static class Step {
        @Getter @Setter private int priority;
        @Getter @Setter private String name;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tracker.Step step = (Tracker.Step) o;
            return Objects.equals(name, step.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }


}
