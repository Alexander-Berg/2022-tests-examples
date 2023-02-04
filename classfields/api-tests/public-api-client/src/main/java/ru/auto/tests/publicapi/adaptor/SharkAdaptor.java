package ru.auto.tests.publicapi.adaptor;

import com.google.inject.Inject;
import groovy.lang.Singleton;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import ru.auto.tests.commons.restassured.AllureLoggerFilter;
import ru.auto.tests.publicapi.config.PublicApiConfig;

@Singleton
public class SharkAdaptor {

    @Inject
    private PublicApiConfig config;

    public Response updateClaim(String creditApplicationId, String claimId, String body) {
         return RestAssured.given().filter(new AllureLoggerFilter())
                .baseUri(config.getSharkApiURI().toString())
                .header("X-Yandex-Uid", "slider5")
                .header("Accept", "application/json")
                .contentType("application/json")
                .pathParam("credit_application_id", creditApplicationId)
                .pathParam("claim_id", claimId)
                .body(body)
                .when()
                .post("/credit-application/update-claim/{credit_application_id}/{claim_id}");
    }
}
