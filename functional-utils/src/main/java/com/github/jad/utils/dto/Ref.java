package com.github.jad.utils.dto;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Ref<T> { //TODO interface
    private T obj;

    public T get() {
        return obj;
    }

    public void set(T obj) {
        this.obj = obj;
    }
}