package com.github.jad.utils;

import com.github.jad.utils.dto.Ref;
import rx.Observable;
import rx.Scheduler;
import com.github.jad.utils.dto.SmartBuffer;
import com.github.jad.utils.dto.ValWrapper;
import rx.internal.operators.OperatorGroupByEvicting;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static rx.Observable.interval;

public class RxUtils {

    public static <P1, P2> Observable.Transformer<P1, P2> toTransformer(Function<Observable<P1>, Observable<P2>> function) {
        return function::apply;
    }

    public static <R> Observable.Transformer<R, List<R>> smartBuffer(int bufferSize, int time, TimeUnit unit, Scheduler scheduler) {
        return tObservable -> {
            Ref<Boolean> terminationFlag = new Ref<>(true);
            Observable<ValWrapper<R>> interval = interval(time, unit, scheduler).map(ValWrapper::signal);
            return  Observable.merge(Arrays.asList(
                            tObservable.doOnTerminate(() -> terminationFlag.set(false)).map(ValWrapper::val),
                            interval.takeWhile(r -> terminationFlag.get()).onBackpressureDrop()
                        ), 2)
                    .lift(new SmartBuffer<ValWrapper<R>>(bufferSize, ValWrapper::isSignal))
                    .map(list -> list.stream().map(ValWrapper::getVal).collect(Collectors.toList()));
        };
    }

    public static <R> Observable.Transformer<R, List<R>> smartBuffer(int bufferSize, int time, TimeUnit unit) {
        return smartBuffer(bufferSize, time, unit, Schedulers.computation());
    }

    public static <T,R> Observable.Transformer<T, R> parallel(int threadCount,
                                                              Function<T, Integer> threadDistribution,
                                                              Scheduler scheduler,
                                                              Function<Observable<T>, Observable<R>> map) {
        return rObservable -> rObservable
                //.groupBy(r -> threadDistribution.apply(r) % threadCount)
                .lift(new OperatorGroupByEvicting<>(r -> threadDistribution.apply(r) % threadCount, v -> v, threadCount * 2, false, null))
                .flatMap(item -> item.observeOn(scheduler, threadCount * 2)
                                .compose(map::apply),
                        threadCount);
    }

    public static <T,R> Observable.Transformer<T, R> parallel(int threadCount,
                                                              Function<T, Integer> threadDistribution,
                                                              Scheduler scheduler,
                                                              int bufferSize,
                                                              Function<Observable<T>, Observable<R>> map) {
        return rObservable -> rObservable
                //.groupBy(r -> threadDistribution.apply(r) % threadCount)
                .lift(new OperatorGroupByEvicting<>(r -> threadDistribution.apply(r) % threadCount, v -> v, bufferSize, false, null))
                .flatMap(item -> item.observeOn(scheduler, bufferSize)
                                .compose(map::apply).rebatchRequests(1),
                        threadCount);
    }

    public static <T,R> Observable.Transformer<T, R> parallel(int threadCount,
                                                              Scheduler scheduler,
                                                              Function<Observable<T>, Observable<R>> map) {
        return parallel(threadCount, e -> (int)(Math.random() * threadCount), scheduler, map);
    }
}
