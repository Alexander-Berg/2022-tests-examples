package ru.yandex.payments.testing.micronaut_cucumber;

import javax.inject.Singleton;

@Singleton
public class TestMockBean {
    public int getNumber() {
        return 42;
    }
}
