package ru.auto.tests.coverage;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.RestAssured;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Paths;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class SwaggerCoverageTest {

    private static final String BODY_STRING = "Hello world!";

    @Rule
    public WireMockRule mock = new WireMockRule(options().dynamicPort());

    @Before
    public void setUp() {
        configureFor(mock.port());
        stubFor(get(urlEqualTo("/hello"))
                .willReturn(aResponse().withStatus(HttpStatus.SC_OK)
                        .withBody(BODY_STRING)));
    }

    @Test
    public void test() {
        RestAssured.given().filter(new RequestLoggerFilter(Paths.get("swagger-result.txt"))).get(mock.url("/hello")).then().statusCode(200);
    }
}
