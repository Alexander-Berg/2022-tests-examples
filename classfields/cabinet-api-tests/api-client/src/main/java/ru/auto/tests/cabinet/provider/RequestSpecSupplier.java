package ru.auto.tests.cabinet.provider;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import ru.auto.tests.commons.restassured.AllureFailureListener;
import ru.auto.tests.commons.restassured.AllureLoggerFilter;

import java.net.URI;

import static io.restassured.config.FailureConfig.failureConfig;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static io.restassured.http.ContentType.JSON;
import static ru.auto.tests.cabinet.GsonObjectMapper.gson;

class RequestSpecSupplier {

    private RequestSpecSupplier() {

    }
    static RequestSpecBuilder baseReqSpecBuilder(URI baseUri, String basePath) {
        return new RequestSpecBuilder().setContentType(JSON)
                .setBaseUri(baseUri)
                .setBasePath(basePath)
                .addFilter(new AllureLoggerFilter());
    }

    static LogConfig getLogConfig(boolean loggerEnabled) {
        if (loggerEnabled) {
            return logConfig().enableLoggingOfRequestAndResponseIfValidationFails();
        }
        return logConfig();
    }

    static RestAssuredConfig baseRestAssuredConfig() {
        return config().objectMapperConfig(objectMapperConfig().defaultObjectMapper(gson()))
                .failureConfig(failureConfig().failureListeners(new AllureFailureListener()));
    }
}
