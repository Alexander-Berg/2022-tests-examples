package ru.auto.tests.commons.restassured;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.util.UUID;

import static io.qameta.allure.model.Status.PASSED;

/**
 * Created by vicdev on 27.03.17.
 */
public class AllureLoggerFilter extends AllureRestAssured {

    @Override
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext filterContext) {
        AllureLifecycle lifecycle = Allure.getLifecycle();
        lifecycle.startStep(
                UUID.randomUUID().toString(),
                new StepResult().setStatus(PASSED).setName(String.format("%s: %s", requestSpec.getMethod(), requestSpec.getURI()))
        );
        Response response;
        try {
            response = super.filter(requestSpec, responseSpec, filterContext);
        } finally {
            lifecycle.stopStep();
        }
        return response;
    }
}
