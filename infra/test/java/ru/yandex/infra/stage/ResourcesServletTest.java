package ru.yandex.infra.stage;

import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.asynchttpclient.Response;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.controller.testutil.FutureUtils;
import ru.yandex.infra.controller.testutil.LocalHttpServerBasedTest;
import ru.yandex.infra.stage.podspecs.FixedResourceSupplier;
import ru.yandex.infra.stage.rest.ResourcesServlet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ResourcesServletTest extends LocalHttpServerBasedTest {
    private static final String RESOURCES_PATH = "/resources";

    @Override
    protected Map<String, HttpServlet> getServlets() {
        return ImmutableMap.of(RESOURCES_PATH, new ResourcesServlet(ImmutableMap.of(
                "resource1",
                new FixedResourceSupplier(TestData.DOWNLOADABLE_RESOURCE, Optional.of(TestData.RESOURCE_META), null),
                "resource2", new FixedResourceSupplier(TestData.DOWNLOADABLE_RESOURCE2, Optional.empty(), null)
        )));
    }

    @Test
    void testResponse() throws Exception {
        Response response =
                FutureUtils.get1s(getClient().executeRequest(getClient().prepareGet(getUrl() + RESOURCES_PATH).build())
                        .toCompletableFuture());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(response.getResponseBodyAsBytes());

        JsonNode node1 = rootNode.get("resource1");
        assertThat(node1.get("state").asText(), equalTo("resolved"));
        assertThat(node1.get("resource_id").asLong(), equalTo(TestData.RESOURCE_META.getResourceId()));
        assertThat(node1.get("task_id").asLong(), equalTo(TestData.RESOURCE_META.getTaskId()));

        assertThat(rootNode.get("resource2").get("state").asText(), equalTo("unresolved"));
    }
}
