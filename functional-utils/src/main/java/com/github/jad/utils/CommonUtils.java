package com.github.jad.utils;

import com.github.jad.utils.dto.CustomThreadFactory;
import com.github.jad.utils.dto.FunctionEx;
import com.github.jad.utils.dto.SupplierEx;
import com.github.jad.utils.dto.Ref;

import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
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

    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static boolean isNonEmpty(final CharSequence cs) {
        return !isEmpty(cs);
    }

    //Copy-paste org.apache.commons.lang3.StringUtils
    public static String uncapitalize(final String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }

        final int firstCodepoint = str.codePointAt(0);
        final int newCodePoint = Character.toLowerCase(firstCodepoint);
        if (firstCodepoint == newCodePoint) {
            // already capitalized
            return str;
        }

        final int newCodePoints[] = new int[strLen]; // cannot be longer than the char array
        int outOffset = 0;
        newCodePoints[outOffset++] = newCodePoint; // copy the first codepoint
        for (int inOffset = Character.charCount(firstCodepoint); inOffset < strLen; ) {
            final int codepoint = str.codePointAt(inOffset);
            newCodePoints[outOffset++] = codepoint; // copy the remaining ones
            inOffset += Character.charCount(codepoint);
        }
        return new String(newCodePoints, 0, outOffset);
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

    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }
}
