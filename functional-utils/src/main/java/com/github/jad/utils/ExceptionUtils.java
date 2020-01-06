package com.github.jad.utils;

import com.github.jad.utils.dto.FunctionEx;
import com.github.jad.utils.dto.RunnableEx;
import com.github.jad.utils.dto.SupplierEx;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class ExceptionUtils {
    
    private ExceptionUtils() {
    }



    public static <X extends Throwable> void propagateIfInstanceOf(Throwable throwable, Class<X> declaredType) throws X {
        if (declaredType.isInstance(throwable)) {
            throw declaredType.cast(throwable);
        }
    }

    public static void propagateIfPossible(Throwable throwable) {
        propagateIfInstanceOf(throwable, Error.class);
        propagateIfInstanceOf(throwable, RuntimeException.class);
    }

    public static <X extends Throwable> void propagateIfPossible(Throwable throwable, Class<X> declaredType) throws X {
        propagateIfInstanceOf(throwable, declaredType);
        propagateIfPossible(throwable);
    }

    public static <X1 extends Throwable, X2 extends Throwable> void propagateIfPossible(Throwable throwable, Class<X1> declaredType1, Class<X2> declaredType2) throws X1, X2 {
        Objects.requireNonNull(declaredType2);
        propagateIfInstanceOf(throwable, declaredType1);
        propagateIfPossible(throwable, declaredType2);
    }

    public static RuntimeException propagate(Throwable throwable) {
        propagateIfPossible(Objects.requireNonNull(throwable));
        throw new RuntimeException(throwable);
    }

    public static <T> T propagateException(SupplierEx<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw propagate(e);
        }
    }

    public static <T> Supplier<T> supplierEx(SupplierEx<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw propagate(e);
            }
        };
    }

    public static <T> Runnable runEx(RunnableEx runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw propagate(e);
            }
        };
    }

    public static void propagate(RunnableEx runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw propagate(e);
        }
    }

    //TODO BiFunction

    public static <P, R> Function<P, R> exFun(FunctionEx<P, R> functionEx) {
        return p -> {
            try {
                return functionEx.apply(p);
            } catch (Exception e) {
                throw propagate(e);
            }
        };
    }
}
