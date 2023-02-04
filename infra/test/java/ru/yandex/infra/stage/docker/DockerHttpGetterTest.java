package ru.yandex.infra.stage.docker;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.servlet.http.HttpServlet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.infra.controller.testutil.DummyServlet;
import ru.yandex.infra.controller.testutil.FutureUtils;
import ru.yandex.infra.controller.testutil.LocalHttpServerBasedTest;
import ru.yandex.infra.controller.util.ResourceUtils;
import ru.yandex.infra.stage.HttpServiceMetrics;
import ru.yandex.infra.stage.HttpServiceMetricsImpl;
import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.dto.DockerImageContents;
import ru.yandex.infra.stage.dto.DockerImageDescription;
import ru.yandex.infra.stage.util.JsonHttpGetter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static ru.yandex.infra.stage.TestData.DEFAULT_REGISTRY_HOST;
import static ru.yandex.infra.stage.TestData.DOCKER_IMAGE_DESCRIPTION;
import static ru.yandex.infra.stage.TestData.DOCKER_IMAGE_WITHOUT_HOST_DESCRIPTION;

class DockerHttpGetterTest extends LocalHttpServerBasedTest {
    private DummyServlet servlet;

    @Override
    protected Map<String, HttpServlet> getServlets() {
        servlet = new DummyServlet(ResourceUtils.readResource("docker/docker_info.json"));
        return ImmutableMap.of("/api/docker/resolve", servlet);
    }

    static Stream<Arguments> dockerGetterParameters() {
        return Stream.of(
                Arguments.of(null, DOCKER_IMAGE_DESCRIPTION),
                Arguments.of("http://null.dev.yandex-team.ru", DOCKER_IMAGE_DESCRIPTION),
                Arguments.of(DEFAULT_REGISTRY_HOST, DOCKER_IMAGE_WITHOUT_HOST_DESCRIPTION)
        );
    }

    @ParameterizedTest
    @MethodSource("dockerGetterParameters")
    void dockerGetterTest(String defaultRegistryHost, DockerImageDescription image) throws Exception {
        JsonHttpGetter jsonHttpGetter = new JsonHttpGetter(HttpServiceMetrics.Source.DOCKER, getClient(),
                Mockito.mock(HttpServiceMetricsImpl.class));
        DockerGetter getter = new DockerHttpGetterImpl(getUrl(), defaultRegistryHost, jsonHttpGetter);
        DockerImageContents result = getter.get(image).get(3, TimeUnit.SECONDS);
        assertThat(result.getLayers().size(), equalTo(10));
        assertThat(result.getCommand(), equalTo(TestData.DOCKER_IMAGE_CONTENTS.getCommand()));
        assertThat(result.getEntryPoint(), equalTo(ImmutableList
                .of("java", "-Xmx1g", "-Xbootclasspath/a:/etc/aero", "-Dyandex.environment=production",
                        "-Dcamelot.client.endpoints.file=/etc/aerostat/endpoints.xml",
                        "-Djava.net.preferIPv6Addresses=true",
                        "-jar", "/usr/share/aero/aero-agent/aero-agent-deb.jar")));
        assertThat(result.getUser(), equalTo(TestData.DOCKER_IMAGE_CONTENTS.getUser()));
        assertThat(result.getLayers().get(0), equalTo(DockerResolverTest.DOWNLOADABLE_RESOURCE));
        assertThat(result.getWorkingDir(), equalTo(TestData.DOCKER_IMAGE_CONTENTS.getWorkingDir()));
        assertThat(result.getEnvironment(), equalTo(TestData.DOCKER_IMAGE_CONTENTS.getEnvironment()));
        Map<String, String> requestParameters = FutureUtils.get1s(servlet.getRequestParameters());
        assertThat(requestParameters, hasEntry("registryUrl",
                DEFAULT_REGISTRY_HOST + '/' + DOCKER_IMAGE_DESCRIPTION.getName()));
        assertThat(requestParameters, hasEntry("tag", DOCKER_IMAGE_DESCRIPTION.getTag()));
    }
}
