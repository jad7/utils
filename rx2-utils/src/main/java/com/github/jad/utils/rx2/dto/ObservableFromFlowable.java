package com.github.jad.utils.rx2.dto;

import lombok.Getter;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public final class ObservableFromFlowable<T> extends Observable<T> {

    final Publisher<? extends T> source;
    @Getter PublisherSubscriber<T> subscriber;


    public ObservableFromFlowable(Publisher<? extends T> publisher) {
        this.source = publisher;
    }

    long requestOnSubscribe = 0;

    public void request(long l) {
        if (subscriber == null || subscriber.upstream == null) {
            requestOnSubscribe += l;
        } else {
            subscriber.upstream.request(l);
        }
    }

    @Override
    protected void subscribeActual(final Observer<? super T> o) {
        subscriber = new PublisherSubscriber<>(o);
        source.subscribe(subscriber);
    }

    public final class PublisherSubscriber<T>
        implements FlowableSubscriber<T>, Disposable {

        final Observer<? super T> downstream;
        @Getter Subscription upstream;

        PublisherSubscriber(Observer<? super T> o) {
            this.downstream = o;
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
                if (requestOnSubscribe > 0) {
                    s.request(requestOnSubscribe);
                }
            }
        }

        @Override
        public void dispose() {
            upstream.cancel();
            upstream = SubscriptionHelper.CANCELLED;
        }

        @Override
        public boolean isDisposed() {
            return upstream == SubscriptionHelper.CANCELLED;
        }
    }
}