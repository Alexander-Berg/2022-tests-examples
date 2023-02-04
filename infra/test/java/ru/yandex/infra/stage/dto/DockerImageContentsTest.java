package ru.yandex.infra.stage.dto;

import java.util.Collections;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.controller.util.ResourceUtils;
import ru.yandex.infra.stage.TestData;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class DockerImageContentsTest {
    @Test
    void handle_nulls_in_optional_fields() throws Exception {
        String json = ResourceUtils.readResource("docker/nulls.json");
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = objectMapper.readTree(json);
        assertThat(DockerImageContents.fromJson(node, TestData.DOCKER_IMAGE_DESCRIPTION), equalTo(new DockerImageContents(
                TestData.DOCKER_IMAGE_DESCRIPTION, emptyList(), emptyList(), emptyList(), empty(), empty(), empty(), Collections.emptyMap(), Optional.of(TestData.DEFAULT_IMAGE_HASH)
        )));
    }

    @Test
    void order_of_environment_variables_must_be_stable() throws Exception {
        String json = ResourceUtils.readResource("docker/nulls_with_environment.json");
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = objectMapper.readTree(json);
        assertThat(DockerImageContents.fromJson(node, TestData.DOCKER_IMAGE_DESCRIPTION),
                equalTo(new DockerImageContents(TestData.DOCKER_IMAGE_DESCRIPTION, emptyList(), emptyList(), emptyList(), empty(), empty(), empty(),
                        ImmutableSortedMap.of("a", "a", "b", "b", "c", "c"), Optional.of(TestData.DEFAULT_IMAGE_HASH))));
    }

    @Test
    void order_of_environment_variables_must_be_stable_when_consrtucting() {
        DockerImageContents dockerImageContents = new DockerImageContents(TestData.DOCKER_IMAGE_DESCRIPTION, emptyList(), emptyList(), emptyList(),
                empty(), empty(), empty(),
                ImmutableMap.of("b", "b", "a", "a", "c", "c"), empty());
        assertThat(dockerImageContents.getEnvironment().toString(),
                equalTo( ImmutableSortedMap.of("a", "a", "b", "b", "c", "c").toString()));
    }

}
