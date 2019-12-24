package com.github.jad.utils.rx2.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ValWrapper<T> {
    private T val;
    private boolean signal;

    public static <T> ValWrapper<T> signal(Long l) {
        return new ValWrapper<>(null, true);
    }

    public static <T> ValWrapper<T> val(T id) {
        return new ValWrapper<>(id, false);
    }
}