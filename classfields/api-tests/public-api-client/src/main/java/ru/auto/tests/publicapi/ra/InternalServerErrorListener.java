package ru.auto.tests.publicapi.ra;

import io.restassured.listener.ResponseValidationFailureListener;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;

public class InternalServerErrorListener implements ResponseValidationFailureListener {

    @Override
    public void onFailure(RequestSpecification requestSpecification, ResponseSpecification responseSpecification, Response response) {
        if (response.getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            throw new AssertionError(String.format("%s.\n%s", response.getStatusLine(),
                    response.getBody().prettyPrint()));
        }
    }
}
