package ru.yandex.whitespirit.it_tests.whitespirit.client;

import java.util.Collections;
import java.util.Map;

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import io.restassured.response.Response;
import lombok.val;

import static ru.yandex.whitespirit.it_tests.whitespirit.client.HttpPathTemplate.GET_DOCUMENT;
import static ru.yandex.whitespirit.it_tests.whitespirit.client.HttpPathTemplate.GET_DOCUMENT_SCHEMALESS;

public class HudsuckerClient extends WhiteSpiritClient {
    public HudsuckerClient(String baseUrl, String whitespiritUrl) {
        super(baseUrl, new OpenApiValidationFilter(whitespiritUrl + "/swagger.yaml"));
    }

    public Response hudsucker() {
        return doGET(HttpPathTemplate.HUDSUCKER, Collections.emptyMap());
    }

    public Response status(String kktSerialNumber) {
        return doGET(HttpPathTemplate.STATUS, Map.of("kktSN", kktSerialNumber));
    }
}
