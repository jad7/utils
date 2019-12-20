package com.github.jad.utils.dto;

@FunctionalInterface
public interface SupplierEx<T> {
    T get() throws Exception;
}
