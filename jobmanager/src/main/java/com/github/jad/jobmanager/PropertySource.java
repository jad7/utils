package com.github.jad.jobmanager;

public interface PropertySource {
    <T> T getProperty(String key, Class<T> clazz);

    <T> T getProperty(String s, Class<T> clazz, T defaultValue);
}
