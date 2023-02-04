package com.yandex.runtime.test_support;

import java.lang.Integer;
import java.lang.NullPointerException;


/** @exclude */
public class TestClass {
    public class ReturnClass {}

    public int getIntValue() {
        return 42;
    }

    public Integer getObjectValue() {
        return new Integer(42);
    }

    public int throwException() throws NullPointerException {
        throw new NullPointerException();
    }

    public ReturnClass objectField = new ReturnClass();
}
