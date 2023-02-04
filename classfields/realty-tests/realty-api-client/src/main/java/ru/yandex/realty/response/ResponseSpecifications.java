package ru.yandex.realty.response;

import io.restassured.specification.ResponseSpecification;
import ru.auto.tests.commons.restassured.ResponseSpecBuilders;

import static org.hamcrest.CoreMatchers.equalTo;

/**
 * Created by vicdev on 07.04.17.
 */
public class ResponseSpecifications {

    public static ResponseSpecification shouldBeStatusOk() {
        return ResponseSpecBuilders.shouldBe200OkJSON()
                .expectBody("status", equalTo("OK")).build();
    }
}
