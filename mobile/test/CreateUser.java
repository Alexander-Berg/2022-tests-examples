package ru.yandex.autotests.mobile.disk.android.rules.annotations.test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates that user will be created for test, not picked from TUS
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface CreateUser {
    String login() default "";
    String password() default "";
    String firsName() default "";
    String lastName() default "";
    String language() default "";
    String country() default "";
    String tags() default "";
}
