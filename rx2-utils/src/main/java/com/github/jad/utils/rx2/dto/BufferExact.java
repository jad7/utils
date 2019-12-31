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
import java.util.function.Function;
import java.util.function.Predicate;

public class BufferExact<T, R, C extends Collection<? super R>>
        implements FlowableSubscriber<T>, Subscription {

    private final Subscriber<? super C> downstream;

    private final Callable<C> bufferSupplier;

    private final ObservableFromFlowable of;
    private final int size;

    private C buffer;

    private Subscription upstream;

    boolean done;

    private int index;

    private final Predicate<T> isSignal;

    private final Function<T, R> mapper;
    long totalRequested = 0;
    long upstreamRequested = 0;
    long totalEmitted = 0;
    long signalRequested = 0;

    BufferExact(Subscriber<? super C> actual, ObservableFromFlowable of, int size, Callable<C> bufferSupplier, Predicate<T> isSignal, Function<T, R> mapper) {
        this.downstream = actual;
        this.of = of;
        this.size = size;
        this.bufferSupplier = bufferSupplier;
        this.isSignal = isSignal;
        this.mapper = mapper;
    }

    @Override
    public void request(long n) {
        if (SubscriptionHelper.validate(n)) {
            totalRequested += n;
            if (n > 0 && index >= size) {
                C b = buffer;
                index = 0;
                buffer = null;
                totalEmitted++;
                downstream.onNext(b);
                n--;
            }
            long num = BackpressureHelper.multiplyCap(n + 1, size);
            long toRequest = Math.max(0, num - upstreamRequested);
            if (toRequest > 0) {
                this.upstreamRequested += toRequest;
                //upstream.request(toRequest);
                of.request(toRequest);
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
               if (totalRequested > totalEmitted)  {
                   buffer = null;
                   totalEmitted++;
                   downstream.onNext(b);
               } else {
                   System.out.println("Requested:" + totalRequested + " Emitted:" + totalEmitted + " UpstreamRequested:"
                           + upstreamRequested + " CurrentBuf" + (buffer == null ? "null" : buffer.size()));
               }
            }
            //request(1);
            //upstream.request(1);
            signalRequested++;

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
