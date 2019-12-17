package com.github.jad.utils;

import com.github.jad.utils.dto.CustomThreadFactory;
import com.github.jad.utils.dto.ExceptionedSupplier;
import com.github.jad.utils.dto.Ref;

import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.github.jad.utils.FunctionalUtils.setAndReturn;

public class CommonUtils {

    private static final Object THBNL = new Object();

    public static boolean isEmpty(Collection<?> c) {
        return c == null || c.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> c) {
        return c == null || c.isEmpty();
    }

    public static boolean isNonEmpty(Collection<?> c) {
        return !isEmpty(c);
    }

    public static boolean isNonEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }


    @SuppressWarnings("unchecked")
    public static <T> Supplier<T> lazy(Supplier<T> supplier) {
        final Ref<T> res = new Ref(THBNL);
        return () -> (T) (res.getObj() == THBNL ? setAndReturn(res::setObj, supplier.get()) : res.getObj());
    }


    public static <T> T getOrDefault(T val, T def){
        return Optional.ofNullable(val).orElse(def);
    }



    public static ThreadFactory threadFactory(String namingPattern) {
        Objects.requireNonNull(namingPattern, "Thread naming patten can not be null");
        if (!namingPattern.contains("%d")) {
            namingPattern += "-%d";
        }
        return new CustomThreadFactory(namingPattern, true);
    }

    public static ThreadFactory threadFactory(String namingPattern, boolean daemon) {
        Objects.requireNonNull(namingPattern, "Thread naming patten can not be null");
        if (!namingPattern.contains("%d")) {
            namingPattern += "-%d";
        }
        return new CustomThreadFactory(namingPattern, daemon);
    }

    public static <T> T propagateException(ExceptionedSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw propagate(e);
        }
    }

    public static <T> Supplier<T> propagateSupplierException(ExceptionedSupplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw propagate(e);
            }
        };
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

    public static <T> Stream<T> concat(Stream<T> ... streams) {
        Stream<T> concat = Stream.empty();
        for (Stream<T> stream : streams) {
            concat = Stream.concat(concat, stream);
        }
        return concat;
    }

    @SuppressWarnings("uncheked")
    public static  <K, V> Map<K, V> mapOf(Object... objects) {
        final HashMap<K, V> result = new HashMap<>(objects.length / 2 + 1);
        for (int i = 0; i < objects.length; i += 2) {
            result.put((K)objects[i], (V)objects[i + 1]);
        }
        return result;
    }
}
