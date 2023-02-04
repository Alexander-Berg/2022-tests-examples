package ru.auto.tests.realtyapi.aspect;

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
import ru.auto.tests.realtyapi.config.RealtyApiConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.qameta.allure.util.ResultsUtils.EPIC_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.FEATURE_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.STORY_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.TAG_LABEL_NAME;
import static java.lang.String.format;
import static java.lang.System.getProperties;
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

        Supplier<RequestSpecBuilder> builderSupplier = (Supplier<RequestSpecBuilder>)
                FieldUtils.readDeclaredField(joinPoint.getThis(), "reqSpecSupplier", true);

        RequestSpecBuilder builder = builderSupplier.get();

        RequestSpecificationImpl spec = (RequestSpecificationImpl)
                FieldUtils.readDeclaredField(builder, "spec", true);

        String apiVersion = spec.getBasePath();
        String requestUri = spec.getURI();
        String prodUri = provideRealtyApiConfig().getRealtyApiProdURI().resolve(format("/%s", apiVersion)).toString();

        boolean isNotProdRequest = !requestUri.equals(prodUri);

        try {
            if (isNotProdRequest) {

                Set<AllureLabel> labels = new HashSet<>();
                getLifecycle().updateTestCase(testCase -> labels.addAll(
                        (List<AllureLabel>) (Object) testCase.getLabels()));

                List<String> tags = asList(apiOperation.tags());
                if (!asList(tags).isEmpty()) {
                    getLifecycle().updateTestCase(testCase ->
                            labels.addAll((Collection<AllureLabel>) tags.stream()
                                    .map(tag -> new AllureLabel().setName(TAG_LABEL_NAME).setValue(tag))
                                    .collect(Collectors.toList()))
                    );
                }

                if (!apiOperation.value().isEmpty()) {
                    getLifecycle().updateTestCase(testCase ->
                            labels.add(new AllureLabel().setName(STORY_LABEL_NAME)
                                    .setValue(apiOperation.value()))
                    );
                }

                String feature = joinPoint.getThis().getClass().getAnnotation(Api.class).value();
                if (!feature.isEmpty()) {
                    getLifecycle().updateTestCase(testCase ->
                            labels.add(new AllureLabel().setName(FEATURE_LABEL_NAME)
                                    .setValue(feature)));
                }

                if (!apiVersion.isEmpty()) {
                    getLifecycle().updateTestCase(testCase ->
                            labels.add(new AllureLabel().setName(EPIC_LABEL_NAME)
                                    .setValue(apiVersion))
                    );
                }

                if (!labels.isEmpty()) {
                    getLifecycle().updateTestCase(testResult ->
                            testResult.setLabels(new ArrayList<>(labels)));
                }
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

    private RealtyApiConfig provideRealtyApiConfig() {
        return ConfigFactory.create(RealtyApiConfig.class, getProperties(), System.getenv());
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
