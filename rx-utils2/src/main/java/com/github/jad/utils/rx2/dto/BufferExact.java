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
import java.util.function.Predicate;

public class BufferExact<T, C extends Collection<? super T>>
        implements FlowableSubscriber<T>, Subscription {

    final Subscriber<? super C> downstream;

    final Callable<C> bufferSupplier;

    final int size;

    C buffer;

    Subscription upstream;

    boolean done;

    int index;

    final Predicate<T> isSignal;
    private boolean lastWasASignal;

    BufferExact(Subscriber<? super C> actual, int size, Callable<C> bufferSupplier, Predicate<T> isSignal) {
        this.downstream = actual;
        this.size = size;
        this.bufferSupplier = bufferSupplier;
        this.isSignal = isSignal;
    }

    @Override
    public void request(long n) {
        if (SubscriptionHelper.validate(n)) {
            upstream.request(BackpressureHelper.multiplyCap(n, size));
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
        }
    }

    @Override
    public void onNext(T t) {
        if (done) {
            return;
        }

        C b = buffer;

        if (isSignal.test(t)) {
            if (lastWasASignal) {
                if (b != null) {
                    buffer = null;
                    downstream.onNext(b);
                }
            } else {
                lastWasASignal = true;
            }
            request(1);
        } else {
            lastWasASignal = false;
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

            b.add(t);

            int i = index + 1;
            if (i == size) {
                index = 0;
                buffer = null;
                downstream.onNext(b);
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
