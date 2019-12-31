package com.github.jad.utils.dto;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public  class VolatileRef<T> {
    private volatile T obj;

    public T get() {
        return obj;
    }

    public void set(T obj) {
        this.obj = obj;
    }
}