package ru.auto.tests.publicapi.aspect;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;

/**
 * Created by vicdev on 01.11.17.
 */
@SuppressWarnings("all")
@Aspect
public class RestAssuredAspectJ {

    private static AllureLifecycle lifecycle;

    @Around("execution(* io.restassured.builder.ResponseSpecBuilder.expectStatusCode(..))" +
            " || execution(* io.restassured.builder.ResponseSpecBuilder.expectContentType(..))" +
            " || execution(* io.restassured.builder.ResponseSpecBuilder.expectBody(..))" +
            " || execution(* io.restassured.builder.ResponseSpecBuilder.expectHeader(..))" +
            " || execution(* io.restassured.builder.ResponseSpecBuilder.expectCookie(..))")
    public Object step(final ProceedingJoinPoint joinPoint) throws Throwable {

        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final String name = joinPoint.getArgs().length > 0
                ? String.format("%s \'%s\'", methodSignature.getName(), arrayToString(joinPoint.getArgs()))
                : methodSignature.getName();


        final String uuid = UUID.randomUUID().toString();
        final StepResult result = new StepResult()
                .withName(name);
        getLifecycle().startStep(uuid, result);
        try {
            final Object proceed = joinPoint.proceed();
            getLifecycle().updateStep(uuid, s -> s.withStatus(Status.PASSED));
            return proceed;
        } catch (Throwable e) {
            getLifecycle().updateStep(uuid, s -> s
                    .withStatus(getStatus(e).orElse(Status.BROKEN))
                    .withStatusDetails(getStatusDetails(e).orElse(null)));
            throw e;
        } finally {
            getLifecycle().stopStep(uuid);
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

    private static String arrayToString(final Object... array) {
        return Stream.of(array)
                .map(object -> {
                    if (object.getClass().isArray()) {
                        return arrayToString((Object[]) object);
                    }
                    return Objects.toString(object);
                })
                .collect(Collectors.joining(" "));
    }
}
