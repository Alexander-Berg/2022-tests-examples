package ru.auto.tests.publicapi.provider;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import org.apache.commons.codec.binary.Hex;
import ru.auto.tests.commons.restassured.AllureFailureListener;
import ru.auto.tests.publicapi.ra.AllureLoggerFilter;
import ru.auto.tests.publicapi.ra.InternalServerErrorListener;

import java.net.URI;
import java.util.Random;

import static io.restassured.config.FailureConfig.failureConfig;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static io.restassured.http.ContentType.JSON;
import static ru.auto.tests.publicapi.GsonObjectMapper.gson;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.UBER_TRACE_ID;

class RequestSpecSupplier {

    private RequestSpecSupplier() {

    }

    static RequestSpecBuilder baseReqSpecBuilder(URI baseUri, String basePath) {
        return new RequestSpecBuilder().setContentType(JSON)
                .setBaseUri(baseUri)
                .setBasePath(basePath)
                .addFilter(new AllureLoggerFilter())
                .addHeader(UBER_TRACE_ID, getUberTraceId());
    }

    private static String getUberTraceId() {
        byte[] bytes = new byte[8];
        new Random().nextBytes(bytes);
        String traceId = Hex.encodeHexString(bytes);
        return String.format("%s:%s:0:1", traceId, traceId);
    }

    static LogConfig getLogConfig(boolean loggerEnabled) {
        if (loggerEnabled) {
            return logConfig().enableLoggingOfRequestAndResponseIfValidationFails();
        }
        return logConfig();
    }

    static RestAssuredConfig baseRestAssuredConfig() {
        return config().objectMapperConfig(objectMapperConfig().defaultObjectMapper(gson()))
                .failureConfig(failureConfig().failureListeners(new AllureFailureListener(), new InternalServerErrorListener()));
    }
}
