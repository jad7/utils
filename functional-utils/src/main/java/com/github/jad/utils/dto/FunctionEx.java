package com.github.jad.utils.dto;

public interface FunctionEx<P ,R> {
    R apply(P param) throws Exception;
}
