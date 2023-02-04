package ru.auto.tests.moisha.ra;

import io.restassured.builder.ResponseSpecBuilder;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;

/**
 * Created by vicdev on 18.09.17.
 */
public class ResponseSpecBuilders {

    private ResponseSpecBuilders() {
    }

    public static ResponseSpecBuilder shouldBeEmptyJson() {
        return shouldBe200OkJSON().expectBody(equalTo("{}"));
    }
}
