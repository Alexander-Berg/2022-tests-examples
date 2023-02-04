package ru.yandex.payments.testing.micronaut_cucumber;

import javax.inject.Singleton;

import lombok.Getter;
import lombok.Setter;

@Singleton
public class TestBean {
    @Getter
    @Setter
    private int state = 0;
}
