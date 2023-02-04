package ru.yandex.spirit.it_tests;

import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.val;
import ru.yandex.whitespirit.it_tests.templates.TemplatesManager;

import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static ru.yandex.spirit.it_tests.Utils.*;

public class StartrekClient {
    private final String baseUrl = "https://st-api.test.yandex-team.ru";
    private final String TOKEN;
    private final TemplatesManager templatesManager = new TemplatesManager();

    StartrekClient(String token) {
        TOKEN = token;
    }


    public Response closeTicket(String ticket, String resolution) {
        val body = templatesManager.processTemplate("close_ticket.json.flth",
                Map.of("resolution", resolution));

        return execute(
                Method.POST, String.format("/v2/issues/%s/transitions/close/_execute", ticket),
                emptyMap(), of(body)
        );
    }

    public Response getIssue(String id) {
        return execute(Method.GET, String.format("/v2/issues/%s", id), emptyMap(), empty());
    }

    public Response postAttachment(String id, String attachmentFileName) {
        Map<String, Object> parts = Map.of("file",  getFile(attachmentFileName));

        return execute(Method.POST, String.format("/v2/issues/%s/attachments", id), parts, empty());
    }

    public Response removeTag(String id, String tag) {
        val body = templatesManager.processTemplate(
                "remove_tag_ticket.json.flth", Map.of("tag", tag)
        );

        return execute(Method.PATCH, String.format("/v2/issues/%s", id), emptyMap(), of(body));
    }

    public Response findTicket(String query) {
        val body = templatesManager.processTemplate(
                "find_by_filters_ticket.json.flth", Map.of("query", query)
        );
        return execute(Method.POST, "/v2/issues/_search", emptyMap(), of(body));
    }

    private Response execute(Method method, String path, Map<String, Object> multiparts, Optional<String> body) {
        RequestSpecification request = prepare_request_specification(baseUrl, multiparts, body);
        request.header("Authorization", String.format("OAuth %s", TOKEN));
        return request.request(method, path);
    }
}
