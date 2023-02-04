package ru.auto.tests.aspect;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Label;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.Objects;

@SuppressWarnings("all")
@Aspect
public class JsonUnitAspectJ {

    private static AllureLifecycle lifecycle;

    @Around("execution(* ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals(..))")
    public Object compareLabel(final ProceedingJoinPoint joinPoint) throws Throwable {
        Object returnObject;
        getLifecycle().updateTestCase(t -> {
            t.withLabels(new Label().withName("tag").withValue("compare"));
        });
        returnObject = joinPoint.proceed();
        return returnObject;
    }


    static AllureLifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = Allure.getLifecycle();
        }
        return lifecycle;
    }
}
