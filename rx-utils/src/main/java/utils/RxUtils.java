package utils;

import rx.Observable;
import rx.Scheduler;
import utils.dto.SmartBuffer;
import utils.dto.ValWrapper;

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

    public static <R> Observable.Transformer<R, List<R>> smartBuffer(int bufferSize, int time, TimeUnit unit) {
        return tObservable -> {
            AtomicBoolean atomicBoolean = new AtomicBoolean(true);
            Observable<ValWrapper<R>> interval = interval(time, unit).map(ValWrapper::signal);
            return tObservable
                    .doOnTerminate(() -> atomicBoolean.set(false))
                    .map(ValWrapper::val)
                    .mergeWith(interval.takeWhile(r -> atomicBoolean.get()).onBackpressureDrop())
                    .lift(new SmartBuffer<ValWrapper<R>>(bufferSize, ValWrapper::isSignal))
                    .map(list -> list.stream().map(ValWrapper::getVal).collect(Collectors.toList()));
        };
    }

    public static <T,R> Observable.Transformer<T, R> parallel(int threadCount,
                                                              Function<T, Integer> keyExtractor,
                                                              Scheduler scheduler,
                                                              Function<Observable<T>, Observable<R>> map) {
        return rObservable -> rObservable.groupBy(r -> keyExtractor.apply(r) % threadCount)
                .flatMap(item -> item.observeOn(scheduler)
                                .compose(map::apply),
                        threadCount);
    }

    public static <T,R> Observable.Transformer<T, R> parallel(int threadCount,
                                                              Scheduler scheduler,
                                                              Function<Observable<T>, Observable<R>> map) {
        return parallel(threadCount, e -> (int)(Math.random() * threadCount), scheduler, map);
    }
}
