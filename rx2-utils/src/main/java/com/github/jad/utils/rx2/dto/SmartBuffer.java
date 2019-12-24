package com.github.jad.utils.rx2.dto;

import com.github.jad.utils.rx2.dto.BufferExact;
import io.reactivex.Flowable;
import io.reactivex.internal.fuseable.HasUpstreamPublisher;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;


public class SmartBuffer<T> extends Flowable<List<? extends T>> implements HasUpstreamPublisher<T> {
        protected final Flowable<T> source;
        final int count;
        final Predicate<T> isSignal;

        /**
         * @param count
         *            the number of elements a buffer should have before being emitted
         * @param isSignal TODO
         */
        public SmartBuffer(Flowable<T> source, int count, Predicate<T> isSignal) {
            this.source = source;
            if (count <= 0) {
                throw new IllegalArgumentException("count must be greater than 0");
            }
            Objects.requireNonNull(isSignal, "isSignal predicate can not be null");
            this.count = count;
            this.isSignal = isSignal;
        }

    @Override
    protected void subscribeActual(Subscriber<? super List<? extends T>> s) {
        source.subscribe(new BufferExact<>(s, count, () -> new ArrayList<>(count), isSignal));

    }

    @Override
    public Publisher<T> source() {
        return source;
    }

}
