package ru.auto.tests.publicapi.aspect;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Label;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.internal.RequestSpecificationImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.log4j.Log4j;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import ru.auto.tests.publicapi.config.PublicApiConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import static io.qameta.allure.util.ResultsUtils.FEATURE_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.STORY_LABEL_NAME;

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

        Supplier<RequestSpecBuilder> builderSupplier = (Supplier<RequestSpecBuilder>)
                FieldUtils.readDeclaredField(joinPoint.getThis(), "reqSpecSupplier", true);

        RequestSpecBuilder builder = builderSupplier.get();

        RequestSpecificationImpl spec = (RequestSpecificationImpl)
                FieldUtils.readDeclaredField(builder, "spec", true);

        String requestUri = spec.getURI();
        String prodUri = provideAutoruApiConfig().getPublicApiProdURI().toString();

        boolean isNotProdRequest = !requestUri.equals(prodUri);

        try {
            if (isNotProdRequest) {
                Set<AllureLabel> labels = new HashSet<>();
                getLifecycle().updateTestCase(testCase ->
                        labels.addAll((List<AllureLabel>) (Object) testCase.getLabels()));

                if (!labels.isEmpty()) {
                    getLifecycle().updateTestCase(testResult -> testResult.setLabels(new ArrayList<>(labels)));
                }
            }
            returnObject = joinPoint.proceed();
        } catch (Throwable throwable) {
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

    public PublicApiConfig provideAutoruApiConfig() {
        return ConfigFactory.create(PublicApiConfig.class, System.getProperties(), System.getenv());
    }

    class AllureLabel extends Label {
        @Override
        public AllureLabel setName(String value) {
            return (AllureLabel) super.setName(value);
        }

        @Override
        public AllureLabel setValue(String value) {
            return (AllureLabel) super.setValue(value);
        }

        @Override
        public boolean equals(Object obj) {
            AllureLabel label = (AllureLabel) obj;
            return name.equals(label.name) && value.equals(label.value);
        }

        @Override
        public int hashCode() {
            return 31 + name.hashCode() + value.hashCode();
        }
    }
}
