package ru.yandex.partner.testapi;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.web.reactive.function.client.WebClient;

import ru.yandex.partner.libs.memcached.MemcachedService;
import ru.yandex.partner.test.db.MysqlTestConfiguration;
import ru.yandex.partner.test.utils.TestUtils;
import ru.yandex.partner.testapi.configuration.MemcachedTestApiTestConfiguration;
import ru.yandex.partner.testapi.configuration.TestApiServiceTestConfiguration;
import ru.yandex.partner.testapi.configuration.TusServiceTestConfiguration;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true"},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextHierarchy({
        @ContextConfiguration(classes = {
                MysqlTestConfiguration.class,
                TestApiServiceTestConfiguration.class,
                TusServiceTestConfiguration.class,
                MemcachedTestApiTestConfiguration.class}),
})
// Способ сделать методы @BeforeAll и @AfterAll не статичными
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestApiTest {

    @LocalServerPort
    private int port;

    private Set<String> responseResources;

    @Autowired
    MemcachedService memcachedServiceMock;

    private Integer testcasesCounter = 0;

    @BeforeAll
    void beforeAll() throws IOException, URISyntaxException {
        responseResources = paths(this.getClass(), "/responses").collect(Collectors.toSet());
    }

    @ParameterizedTest
    @ArgumentsSource(TestcasesArgumentProvider.class)
    void requestResponseTest(String resourcePath) throws IOException {
        var testcase = resourcePath.replace(".json", "").replace("/testcases/", "");
        var response = performRequest(testcase);
        responseResources.remove(convertToResponseResource(testcase));
        testcasesCounter++;
        // self update
        if (TestUtils.needSelfUpdate()) {
            selfUpdate(testcase, response);
            Assertions.fail("self_updated");
        } else {
            assertResponse(testcase, response);
            verify(memcachedServiceMock, times(testcasesCounter)).flush();
        }
    }

    @AfterAll
    void afterAll() throws IOException {
        if (TestUtils.needSelfUpdate()) {
            for (String resource : responseResources) {
                Files.delete(Path.of(TestUtils.getAbsolutePath(resource)));
            }
        } else {
            if (!responseResources.isEmpty()) {
                Assertions.fail("Missed testcases: " + Arrays.toString(responseResources.toArray()));
            }
        }
    }

    private String performRequest(String testcase) throws JsonProcessingException {
        var response = WebClient.create("http://127.0.0.1:" + port + "/testapi/wait/" + testcase)
                .post()
                .retrieve()
                .toEntity(String.class)
                .block()
                .getBody();

        return TestUtils.getObjectMapper().readValue(response, JsonNode.class).toPrettyString() + "\n";
    }


    private void assertResponse(String testcase, String response) throws IOException {
        try (var inputStream = this.getClass().getResourceAsStream(convertToResponseResource(testcase))) {
            if (inputStream == null) {
                Assertions.fail("Not found response resource for testcase = " + testcase);
            }
            Assertions.assertEquals(IOUtils.toString(inputStream), response);
        }
    }

    private void selfUpdate(String testcase, String response) throws IOException {
        var path = TestUtils.getAbsolutePath(convertToResponseResource(testcase));
        TestUtils.prepareFile(path);
        try (var writer = new FileWriter(path)) {
            writer.append(response);
            writer.flush();
        }
    }

    private String convertToResponseResource(String testcase) {
        return "/responses/" + testcase + ".json";
    }

    private static class TestcasesArgumentProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            var relativePath = "/testcases";
            return paths(this.getClass(), relativePath)
                    .sorted()
                    .map(Arguments::of);
        }
    }

    private static Stream<String> paths(Class<?> clazz, String relativePath) throws IOException, URISyntaxException {
        var resource = clazz.getResource(relativePath);
        if (resource == null) {
            return Stream.of();
        }
        var sourceDir = Path.of(resource.toURI());

        return Files.walk(sourceDir)
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .map(file -> Paths.get(relativePath, sourceDir.toUri().relativize(file.toURI()).getPath()).toString());
    }
}
