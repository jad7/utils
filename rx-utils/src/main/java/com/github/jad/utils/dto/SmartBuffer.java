package com.github.jad.utils.dto;

import rx.Observable;
import rx.Subscriber;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;


public  class SmartBuffer<T> implements Observable.Operator<List<T>, T> {
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
        public Subscriber<? super T> call(final Subscriber<? super List<T>> child) {
            BufferExact<T> parent = new BufferExact<T>(child, count, isSignal);

            child.add(parent);
            child.setProducer(parent.createProducer());

            return parent;

        }

}
