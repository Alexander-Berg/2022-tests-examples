package ru.auto.tests.realtyapi.ra;


import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import ru.auto.tests.realtyapi.config.RealtyApiConfig;

import java.util.UUID;

import static io.qameta.allure.model.Status.PASSED;
import static java.lang.System.getProperties;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.UBER_TRACE_ID;

public class AllureLoggerFilter extends AllureRestAssured {

    @Override
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext filterContext) {
        String uberTraceId = requestSpec.getHeaders().getValue(UBER_TRACE_ID);
        String jaegerUri = ConfigFactory.create(RealtyApiConfig.class, getProperties(), System.getenv()).getJaegerUri().toString();

        AllureLifecycle lifecycle = Allure.getLifecycle();
        lifecycle.startStep(
                UUID.randomUUID().toString(),
                new StepResult().setStatus(PASSED).setName(String.format("%s: %s", requestSpec.getMethod(), requestSpec.getURI()))
        );
        lifecycle.addAttachment("Jaeger", "text/html", "html", getUberTraceIdAttachment(jaegerUri, uberTraceId));
        Response response;
        try {
            response = super.filter(requestSpec, responseSpec, filterContext);
        } finally {
            lifecycle.stopStep();
        }
        return response;
    }

    private byte[] getUberTraceIdAttachment(String jaegerUri, String uberTraceId) {
        String link = String.format("%s/trace/%s", jaegerUri, StringUtils.substringBefore(uberTraceId, ":"));
        return String.format("<html><body><a href=\"%s\">%s</a></body></html>", link, link).getBytes();
    }
}
