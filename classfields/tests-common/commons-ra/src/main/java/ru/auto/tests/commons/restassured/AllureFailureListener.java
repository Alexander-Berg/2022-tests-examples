package ru.auto.tests.commons.restassured;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.StepResult;
import io.restassured.listener.ResponseValidationFailureListener;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import java.util.UUID;

import static io.qameta.allure.model.Status.FAILED;

public class AllureFailureListener implements ResponseValidationFailureListener {

    @Override
    public void onFailure(RequestSpecification requestSpecification, ResponseSpecification responseSpecification, Response response) {
        AllureLifecycle lifecycle = Allure.getLifecycle();
        String uuid = UUID.randomUUID().toString();
        lifecycle.startStep(uuid, new StepResult().setStatus(FAILED)
                        .setName("Response validation fails"));
        lifecycle.stopStep(uuid);
    }
}
