package com.github.jad.utils.dto;

import lombok.AllArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
public  class VolatileRef<T> implements Serializable {
    private volatile T obj;

    public T get() {
        return obj;
    }

    public void set(T obj) {
        this.obj = obj;
    }
}