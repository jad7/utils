package com.github.jad.utils;

import com.github.jad.utils.dto.TriFunction;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class FunctionalUtils {

    public static <P1, V> Supplier<V> curry(Function<P1, V> function, P1 p1) {
        return () -> function.apply(p1);
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

    /*public static <P1, P2, P3, V> BiFunction<P1, P3, V> curry2(TriFunction<P1, P2, P3, V> function, P2 p2) {
        return (p1, p3) -> function.apply(p1, p2, p3);
    }*/

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

    public static class Memoizer<T, U> {

        private final Map<T, U> cache = new ConcurrentHashMap<>();

        private Memoizer() {}

        private Function<T, U> doMemoize(final Function<T, U> function) {
            return input -> cache.computeIfAbsent(input, function::apply);
        }

        public static <T, U> Function<T, U> memoize(final Function<T, U> function) {
            return new Memoizer<T, U>().doMemoize(function);
        }

        public static Runnable memoize(final Runnable runnable) {
            UUID uuid = UUID.randomUUID();
            Memoizer<UUID, Class<Void>> uuidVoidMemoizer = new Memoizer<>();
            Function<UUID, Class<Void>> uuidClassFunction = uuidVoidMemoizer.doMemoize(id -> {
                runnable.run();
                return Void.TYPE;
            });
            return () -> uuidClassFunction.apply(uuid);
        }
    }
}
