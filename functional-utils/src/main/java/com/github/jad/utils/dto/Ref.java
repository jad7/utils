package com.github.jad.utils.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public  class Ref<T> {
    private T obj;
}