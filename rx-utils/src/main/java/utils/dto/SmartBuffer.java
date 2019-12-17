package utils.dto;

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
         *            the interval with which chunks have to be created. Note that when {@code skip == count}
         *            the operator will produce non-overlapping chunks. If
         *            {@code skip < count}, this buffer operation will produce overlapping chunks and if
         *            {@code skip > count} non-overlapping chunks will be created and some values will not be pushed
         *            into a buffer at all!
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
