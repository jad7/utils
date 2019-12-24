package com.github.jad.utils.dto;

import io.reactivex.FlowableOperator;
import org.reactivestreams.Subscriber;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;


public  class SmartBuffer<T> implements FlowableOperator<List<T>, T> {
        final int count;
        final Predicate<T> isSignal;

        /**
         * @param count
         *            the number of elements a buffer should have before being emitted
         * @param isSignal TODO
         */
        public SmartBuffer(int count, Predicate<T> isSignal) {
            if (count <= 0) {
                throw new IllegalArgumentException("count must be greater than 0");
            }
            Objects.requireNonNull(isSignal, "isSignal predicate can not be null");
            this.count = count;
            this.isSignal = isSignal;
        }



    @Override
    public Subscriber<? super T> apply(Subscriber<? super List<T>> subscriber) throws Exception {
            //TODO
        throw new UnsupportedOperationException("Not implemented");
        /*BufferExact<T> parent = new BufferExact<T>(subscriber, count, isSignal);

        subscriber.add(parent);
        subscriber.setProducer(parent.createProducer());

        return parent;*/
    }
}
