package ru.yandex.payments.yasmgen.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ru.yandex.payments.yasmgen.WarnThreshold;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WarnThreshold(lower = "0", upper = "${warn-upper-threshold}")
public @interface PredefinedWarnThreshold {
}
