package ru.auto.tests.realtyapi.provider;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import org.apache.commons.codec.binary.Hex;
import ru.auto.tests.commons.restassured.AllureFailureListener;
import ru.auto.tests.realtyapi.ra.AllureLoggerFilter;

import java.net.URI;
import java.util.Random;

import static io.restassured.RestAssured.config;
import static io.restassured.config.FailureConfig.failureConfig;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static io.restassured.http.ContentType.JSON;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.X_REAL_IP_HEADER_NAME;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.X_REAL_IP_HEADER_VALUE;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.UBER_TRACE_ID;
import static ru.auto.tests.realtyapi.v1.GsonObjectMapper.gson;

class ReqSpecSupplier {

    private ReqSpecSupplier() {
    }

    static RequestSpecBuilder baseReqSpecBuilder(URI baseUri, String version) {
        return new RequestSpecBuilder()
                .setContentType(JSON)
                .setBaseUri(baseUri)
                .setBasePath(version)
                .addFilter(new AllureLoggerFilter())
                .addHeader(X_REAL_IP_HEADER_NAME, X_REAL_IP_HEADER_VALUE)
                .addHeader(UBER_TRACE_ID, getUberTraceId());
    }

    static RequestSpecBuilder baseReqSpecBuilder(String baseUri, int port, String version) {
        return new RequestSpecBuilder()
            .setContentType(JSON)
            .setBaseUri(baseUri)
            .setPort(port)
            .setBasePath(version)
            .addFilter(new AllureLoggerFilter())
            .addHeader(X_REAL_IP_HEADER_NAME, X_REAL_IP_HEADER_VALUE)
            .addHeader(UBER_TRACE_ID, getUberTraceId());
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

    private static String getUberTraceId() {
        byte[] bytes = new byte[8];
        new Random().nextBytes(bytes);
        String traceId = Hex.encodeHexString(bytes);
        return String.format("%s:%s:0:1", traceId, traceId);
    }
}
