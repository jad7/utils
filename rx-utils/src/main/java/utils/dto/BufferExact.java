package utils.dto;

import rx.Producer;
import rx.Subscriber;
import rx.internal.operators.BackpressureUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class BufferExact<T> extends Subscriber<T> {
    final Subscriber<? super List<T>> actual;
    final int count;
    final Predicate<T> isSignal;
    private boolean lastWasASignal;

    List<T> buffer;

    public BufferExact(Subscriber<? super List<T>> actual, int count, Predicate<T> isSignal) {
        this.actual = actual;
        this.count = count;
        this.isSignal = isSignal;
        this.request(0L);
    }

    @Override
    public void onNext(T t) {
        List<T> b = buffer;
        if (isSignal.test(t)) {
            if (lastWasASignal) {
                if (b != null) {
                    buffer = null;
                    actual.onNext(b);
                }
            } else {
                lastWasASignal = true;
            }
            request(1);
        } else {
            lastWasASignal = false;
            if (b == null) {
                b = new ArrayList<T>(count);
                buffer = b;
            }

            b.add(t);

            if (b.size() == count) {
                buffer = null;
                actual.onNext(b);
            }
        }
    }


    @Override
    public void onError(Throwable e) {
        buffer = null;
        actual.onError(e);
    }

    @Override
    public void onCompleted() {
        List<T> b = buffer;
        if (b != null) {
            actual.onNext(b);
        }
        actual.onCompleted();
    }

    Producer createProducer() {
        return n -> {
            if (n < 0L) {
                throw new IllegalArgumentException("n >= required but it was " + n);
            }
            if (n != 0L) {
                long u = BackpressureUtils.multiplyCap(n, count);
                BufferExact.this.request(u);
            }
        };
    }
}
