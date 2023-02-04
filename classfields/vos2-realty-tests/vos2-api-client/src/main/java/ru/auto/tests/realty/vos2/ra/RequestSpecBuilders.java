package ru.auto.tests.realty.vos2.ra;

import com.google.gson.JsonObject;
import io.restassured.builder.RequestSpecBuilder;

import java.util.function.Consumer;

import static io.restassured.http.ContentType.JSON;

public class RequestSpecBuilders {

    private RequestSpecBuilders() {}

    public static Consumer<RequestSpecBuilder> jsonBody(String body) {
        return requestSpec -> requestSpec.setContentType(JSON).setBody(body);
    }

    public static Consumer<RequestSpecBuilder> jsonBody(JsonObject body) {
        return requestSpec -> requestSpec.setContentType(JSON).setBody(body);
    }
}
