package com.github.jad.utils.rx2.dto;


import io.reactivex.FlowableSubscriber;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class BufferExact<T, R, C extends Collection<? super R>>
        implements FlowableSubscriber<T>, Subscription {

    private final Subscriber<? super C> downstream;

    private final Callable<C> bufferSupplier;

    private final Consumer<Long> realUpstreamConsumer;
    private final int size;

    private C buffer;

    private Subscription upstream;

    boolean done;

    private int index;

    private final Predicate<T> isSignal;

    private final Function<T, R> mapper;
    private final long msInterval;

    long totalRequested = 0;
    long upstreamRequested = 0;
    long totalEmitted = 0;

    long lastEmitted = 0;

    BufferExact(Subscriber<? super C> actual, Consumer<Long> of, int size,
                Callable<C> bufferSupplier, Predicate<T> isSignal, Function<T, R> mapper, long msInterval) {
        this.downstream = actual;
        this.realUpstreamConsumer = of;
        this.size = size;
        this.bufferSupplier = bufferSupplier;
        this.isSignal = isSignal;
        this.mapper = mapper;
        this.msInterval = msInterval;
    }

    @Override
    public void request(long n) {
        if (SubscriptionHelper.validate(n)) {
            totalRequested += n;
            if (n > 0 && index >= size) { //TODO maybe remove
                C b = buffer;
                index = 0;
                buffer = null;
                totalEmitted++;
                lastEmitted = System.currentTimeMillis();
                downstream.onNext(b);
                n--;
            }
            long num = BackpressureHelper.multiplyCap(n, size);
            long toRequest = Math.max(0, num - upstreamRequested);
            if (toRequest > 0) {
                this.upstreamRequested += toRequest;
                realUpstreamConsumer.accept(toRequest);
            }
        }
    }

    @Override
    public void cancel() {
        upstream.cancel();
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.validate(this.upstream, s)) {
            this.upstream = s;
            downstream.onSubscribe(this);
            upstream.request(Long.MAX_VALUE);
        }
    }

    @Override
    public void onNext(T t) {
        if (done) {
            return;
        }

        C b = buffer;

        if (isSignal.test(t)) {

            if (b != null && !b.isEmpty()) { //
                long currentTimeMillis = System.currentTimeMillis();
                if (totalRequested > totalEmitted && (currentTimeMillis - lastEmitted) >= msInterval)  {
                   buffer = null;
                   index = 0;
                   totalEmitted++;
                   lastEmitted = currentTimeMillis;
                   downstream.onNext(b);
               }
                /*else { //DEBUG backpressure case
                   System.out.println("Requested:" + totalRequested + " Emitted:" + totalEmitted + " UpstreamRequested:"
                           + upstreamRequested + " CurrentBuf" + (buffer == null ? "null" : buffer.size()));
               }*/
            }

        } else {
            upstreamRequested--;
            if (b == null) {
                try {
                    b = ObjectHelper.requireNonNull(bufferSupplier.call(), "The bufferSupplier returned a null buffer");
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    cancel();
                    onError(e);
                    return;
                }

                buffer = b;
            }

            b.add(mapper.apply(t));

            int i = index + 1;
            if (i >= size) {
                if (totalRequested > totalEmitted)  {
                    index = 0;
                    buffer = null;
                    totalEmitted++;
                    lastEmitted = System.currentTimeMillis();
                    downstream.onNext(b);
                }
            } else {
                index = i;
            }
        }
    }

    @Override
    public void onError(Throwable t) {
        if (done) {
            RxJavaPlugins.onError(t);
            return;
        }
        done = true;
        downstream.onError(t);
    }

    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        done = true;

        C b = buffer;

        if (b != null && !b.isEmpty()) {
            downstream.onNext(b);
        }
        downstream.onComplete();
    }

}
