package com.github.jad.utils.rx2.dto;

import com.github.jad.utils.rx2.dto.BufferExact;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.internal.fuseable.HasUpstreamPublisher;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;


public class SmartBuffer<T, R> extends Flowable<List<? super R>> implements HasUpstreamPublisher<T> {
    protected final Flowable<T> source;
    private final Function<T, R> mapper;
    private final long msInterval;
    private final Consumer<Long> of;
    final int count;
    final Predicate<T> isSignal;

        /**
         * @param count
         *            the number of elements a buffer should have before being emitted
         * @param isSignal TODO
         */
    public SmartBuffer(Flowable<T> source, int count, Predicate<T> isSignal,
                       Function<T, R> mapper, long msInterval, Consumer<Long> of) {
        this.source = source;
        this.mapper = mapper;
        this.msInterval = msInterval;
        this.of = of;
        if (count <= 0) {
            throw new IllegalArgumentException("count must be greater than 0");
        }
        Objects.requireNonNull(isSignal, "isSignal predicate can not be null");
        this.count = count;
        this.isSignal = isSignal;
    }

    @Override
    protected void subscribeActual(Subscriber<? super List<? super R>> s) {
        source.subscribe(new BufferExact<>(s, of, count, () -> new ArrayList<>(count), isSignal, mapper, Math.max(1, msInterval) / 2));

    }

    @Override
    public Publisher<T> source() {
        return source;
    }

}
