package com.yandex.launcher;

/**
 * Use it when you need to make sure,that some object has been created
 */
public interface TestProvider<T> {

    T get();
}
