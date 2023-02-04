package ru.auto.tests.cabinet.aspect;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Label;
import io.swagger.annotations.ApiOperation;
import lombok.extern.log4j.Log4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * Created by vicdev on 01.11.17.
 */
@SuppressWarnings("all")
@Aspect
@Log4j
public class SwaggerAnnotationsAspectJ {

    private static AllureLifecycle lifecycle;

    @Around("@annotation(apiOperation) && execution(* *(..))")
    public Object afterApiOperation(final ProceedingJoinPoint joinPoint, final ApiOperation apiOperation)
            throws Throwable {
        Object returnObject = null;
        try {
            if (!asList(apiOperation.tags()).isEmpty()) {
                getLifecycle().updateTestCase(testResult -> {
                    testResult.withLabels((Collection<Label>) asList(apiOperation.tags()).stream()
                            .map(tag -> new Label().withName("tag").withValue(tag))
                            .collect(Collectors.toList()));
                });
            }

            if (!apiOperation.value().isEmpty()) {
                getLifecycle().updateTestCase(t -> {
                    t.withLabels(new Label().withName("story")
                            .withValue(apiOperation.value()));
                });
            }
            returnObject = joinPoint.proceed();
        } catch (
                Throwable throwable) {
            throw throwable;
        } finally {
            return returnObject;
        }

    }

    /**
     * For tests only.
     *
     * @param allure allure lifecycle to set.
     */
    public static void setLifecycle(final AllureLifecycle allure) {
        lifecycle = allure;
    }

    public static AllureLifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = Allure.getLifecycle();
        }
        return lifecycle;
    }
}
