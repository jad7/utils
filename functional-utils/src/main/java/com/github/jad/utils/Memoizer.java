package com.github.jad.utils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Memoizer<T, U> {

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
