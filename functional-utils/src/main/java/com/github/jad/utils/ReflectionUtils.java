package com.github.jad.utils;

import java.lang.reflect.*;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

import static java.security.AccessController.doPrivileged;

public class ReflectionUtils {

    private static final Permission[] PERMISSIONS = {
            new RuntimePermission("accessDeclaredMembers"),
            new ReflectPermission("suppressAccessChecks")
    };

    //private static BiConsumer<Field, Object> makeAccessible = getJavaVersion() >= 9 ? ReflectionUtils::makeAccessible9 : ReflectionUtils::makeAccessible8;

    private static ConcurrentMap<FieldKey, FiledExtractor> extractorCache = new ConcurrentHashMap<>();


    @SuppressWarnings("unchecked")
    public static  <T> T extractFieldValue(String fieldName, Class<T> objectClass, Object extractFrom) {
        return extractFieldValue(fieldName, extractFrom);
    }

    @SuppressWarnings("unchecked")
    public static  <T> T extractFieldValue(String fieldName, Object extractFrom) {
        FiledExtractor extractor = extractorCache.computeIfAbsent(new FieldKey(fieldName, extractFrom.getClass()), key -> {

            Field fieldRes = doPrivileged((PrivilegedAction<Field>) () -> {
                Field field;
                try {
                    field = key.objectClass.getDeclaredField(key.fieldName);
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("Can not extract field '" + key.fieldName + "' from clazz " + key.objectClass.getName());
                }
                //makeAccessible.accept(field, extractFrom);
                makeAccessible8(field, extractFrom); //TODO support Java 9
                return field;
            }, null, PERMISSIONS);
            return obj -> getFieldValue(fieldRes, obj);

        });
        return (T) extractor.extract(extractFrom);
    }

    private static <T extends AccessibleObject & Member> boolean isAccessible(final T member) {
        Objects.requireNonNull(member, "No member provided");
        return Modifier.isPublic(member.getModifiers()) && Modifier.isPublic(member.getDeclaringClass().getModifiers());
    }

    private static void makeAccessible8(final Field field, Object extractFrom) {
        Objects.requireNonNull(field, "No field provided");
        if ((!isAccessible(field) || Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
            field.setAccessible(true);
        }
    }

    /*@SuppressWarnings("all")
    private static void makeAccessible9(final Field field, Object extractFrom) {
        Objects.requireNonNull(field, "No field provided");
        if ((!isAccessible(field) || Modifier.isFinal(field.getModifiers())) && !field.canAccess(extractFrom)) {
            field.setAccessible(true);
        }
    }*/

    public static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        int dotPos = version.indexOf('.');
        int dashPos = version.indexOf('-');
        return Integer.parseInt(version.substring(0,
                dotPos > -1 ? dotPos : dashPos > -1 ? dashPos : 1));
    }

    private static Object getFieldValue(final Field field, final Object instance) {
        try {
            return field.get(instance);
        } catch (final IllegalAccessException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    @FunctionalInterface
    private interface FiledExtractor {
        Object extract(Object obj);
    }

    private static class FieldKey {
        final String fieldName;
        final Class<?> objectClass;

        FieldKey(String fieldName, Class<?> objectClass) {
            this.fieldName = fieldName;
            this.objectClass = objectClass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FieldKey fieldKey = (FieldKey) o;
            return Objects.equals(fieldName, fieldKey.fieldName) &&
                    Objects.equals(objectClass, fieldKey.objectClass);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fieldName, objectClass);
        }
    }
}
