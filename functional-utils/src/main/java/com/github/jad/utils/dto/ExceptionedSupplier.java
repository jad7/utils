package com.github.jad.utils.dto;

@FunctionalInterface
public interface ExceptionedSupplier<T> {
    T get() throws Exception;
}
