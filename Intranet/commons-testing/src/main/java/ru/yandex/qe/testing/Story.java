package ru.yandex.qe.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * User: bgleb
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.SOURCE)
public @interface Story {
    String value();
}
