package com.github.jad.utils.rx2;

import com.github.jad.utils.dto.Ref;
import com.github.jad.utils.rx2.dto.SmartBuffer;
import com.github.jad.utils.rx2.dto.ValWrapper;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.reactivex.Flowable.interval;


public class RxUtils {

    public static <P1, P2> FlowableTransformer<P1, P2> toTransformer(Function<Flowable<P1>, Flowable<P2>> function) {
        return function::apply;
    }

    public static <R> FlowableTransformer<R, List<R>> smartBuffer(int bufferSize, int time, TimeUnit unit, Scheduler scheduler) {
        return tFlowable -> {
            Ref<Boolean> terminationFlag = new Ref<>(true);
            Flowable<ValWrapper<R>> interval = interval(time, unit, scheduler).map(ValWrapper::signal);
            return  Flowable.merge(Arrays.asList(
                            tFlowable.doOnTerminate(() -> terminationFlag.set(false)).map(ValWrapper::val),
                            interval.takeWhile(r -> terminationFlag.get()).onBackpressureDrop()
                        ), 2)
                    .compose(src -> new SmartBuffer<>(src, bufferSize, ValWrapper::isSignal))
                    .map(list -> list.stream().map(ValWrapper::getVal).collect(Collectors.toList()));
        };
    }

    public static <R> FlowableTransformer<R, List<R>> smartBuffer(int bufferSize, int time, TimeUnit unit) {
        return smartBuffer(bufferSize, time, unit, Schedulers.computation());
    }

    public static <T, R> FlowableTransformer<T, R> parallel(int threadCount,
                                                            Function<T, Integer> threadDistribution,
                                                            Scheduler scheduler,
                                                            Function<Flowable<T>, Flowable<R>> map) {
        return rObservable -> rObservable
                .groupBy(r -> threadDistribution.apply(r) % threadCount)
                //.lift(new OperatorGroupByEvicting<>(r -> threadDistribution.apply(r) % threadCount, v -> v, threadCount * 2, false, null))
                .flatMap(item -> item.observeOn(scheduler, false, 1)
                                .compose(map::apply), false, threadCount, 1);
    }

    public static <T,R> FlowableTransformer<T, R> parallel(int threadCount,
                                                           Function<T, Integer> threadDistribution,
                                                           Scheduler scheduler,
                                                           int bufferSize,
                                                           Function<Flowable<T>, Flowable<R>> map) {
        return rObservable -> rObservable
                .groupBy(r -> threadDistribution.apply(r) % threadCount, t -> t, false, bufferSize)
                .flatMap(item -> item.observeOn(scheduler, false, bufferSize)
                                .compose(map::apply), false, threadCount, bufferSize);
    }

    public static <T,R> FlowableTransformer<T, R> parallel(int threadCount,
                                                              Scheduler scheduler,
                                                              Function<Flowable<T>, Flowable<R>> map) {
        return parallel(threadCount, e -> (int)(Math.random() * threadCount), scheduler, map);
    }
}
