package ru.yandex.autotests.mobile.disk.android.rules.annotations.test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates tag that will be used for request user from TUS
 */
@Target({TYPE, METHOD})
@Retention(RUNTIME)
public @interface UserTags {
    String[] value();
}
