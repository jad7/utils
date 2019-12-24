package com.github.jad.utils;

import com.github.jad.utils.dto.FunctionEx;
import com.github.jad.utils.dto.RunnableEx;
import com.github.jad.utils.dto.SupplierEx;
import com.github.jad.utils.dto.TriFunction;

import java.util.function.*;

import static com.github.jad.utils.CommonUtils.propagate;

public class FunctionalUtils {

    public static <P1, V> Supplier<V> curry(Function<P1, V> function, P1 p1) {
        return () -> function.apply(p1);
    }

    public static <P1, P2> Consumer<P2> curry1(BiConsumer<P1, P2> consumer, final P1 p1) {
        return (p2) -> consumer.accept(p1, p2);
    }

    public static <P1, P2, V> Function<P2, V> curry1(BiFunction<P1, P2, V> function, P1 p1) {
        return (p2) -> function.apply(p1, p2);
    }

    public static <P1, P2, P3, V> BiFunction<P2, P3, V> curry1(TriFunction<P1, P2, P3, V> function, P1 p1) {
        return (p2, p3) -> function.apply(p1, p2, p3);
    }

    public static <P1, P2, V> Function<P1, V> curry2(BiFunction<P1, P2, V> function, P2 p2) {
        return (p1) -> function.apply(p1, p2);
    }

    public static <P1, P2, P3, V> BiFunction<P1, P3, V> curry2(TriFunction<P1, P2, P3, V> function, P2 p2) {
        return (p1, p3) -> function.apply(p1, p2, p3);
    }

    public static <P1, P2, V> Function<P1, V> curry2(BiFunction<P1, P2, V> function, Supplier<P2> p2) {
        return (p1) -> function.apply(p1, p2.get());
    }

    public static <P1, P2, P3, V> BiFunction<P1, P2, V> curry3(TriFunction<P1, P2, P3, V> function, P3 p3) {
        return (p1, p2) -> function.apply(p1, p2, p3);
    }

    public static <A,B,C> BiFunction<A, B, C> getOrDefault(BiFunction<A, B, C> function, C defaultVal) {
        return function.andThen(curry2(CommonUtils::getOrDefault, defaultVal));
    }

    public static <A, B>  Function<A, B> getOrDefault(Function<A, B> function, B defaultVal) {
        return function.andThen(curry2(CommonUtils::getOrDefault, defaultVal));
    }

    public static <V> V setAndReturn(Consumer<? super V> consumer, V obj) {
        consumer.accept(obj);
        return obj;
    }

    public static <A, B> Function<A, B> supToFun(Supplier<B> supplier) {
        return a -> supplier.get();
    }

    public static <T> T propagateException(SupplierEx<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw CommonUtils.propagate(e);
        }
    }

    public static <T> Supplier<T> supplierEx(SupplierEx<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw CommonUtils.propagate(e);
            }
        };
    }

    public static <T> Runnable runEx(RunnableEx runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw CommonUtils.propagate(e);
            }
        };
    }

    public static void propagate(RunnableEx runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw CommonUtils.propagate(e);
        }
    }

    //TODO BiFunction

    public static <P, R> Function<P, R> exFun(FunctionEx<P, R> functionEx) {
        return p -> {
            try {
                return functionEx.apply(p);
            } catch (Exception e) {
                throw CommonUtils.propagate(e);
            }
        };
    }

}
