package ru.auto.tests.commons.restassured;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;

import java.util.function.Function;

import static io.restassured.http.ContentType.JSON;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

/**
 * Created by vicdev on 01.03.17.
 */
public class ResponseSpecBuilders {

    public static Function<Response, Response> validatedWith(ResponseSpecBuilder respSpec) {
        return response -> response.then().spec(respSpec.build()).extract().response();
    }

    public static Function<Response, Response> validatedWith(ResponseSpecification respSpec) {
        return response -> response.then().spec(respSpec).extract().response();
    }

    /**
     * @param code use org.apache.http.HttpStatus for status codes
     * @return ResponseSpecBuilder
     */
    public static ResponseSpecBuilder shouldBeCode(int code) {
        return new ResponseSpecBuilder().expectStatusCode(code);
    }

    public static ResponseSpecBuilder shouldBe200Ok() {
        return shouldBeCode(HttpStatus.SC_OK);
    }

    public static ResponseSpecBuilder shouldBe200OkJSON() {
        return shouldBe200Ok().expectContentType(JSON);
    }

    public static ResponseSpecBuilder shouldBe200OkJSONAndMatchSchema(String schemaPath) {
        return shouldBe200OkJSON().expectBody(matchesJsonSchemaInClasspath(schemaPath));
    }

}
