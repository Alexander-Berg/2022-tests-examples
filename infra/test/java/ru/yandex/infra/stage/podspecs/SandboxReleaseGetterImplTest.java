package ru.yandex.infra.stage.podspecs;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServlet;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.infra.controller.testutil.DummyServlet;
import ru.yandex.infra.controller.testutil.FutureUtils;
import ru.yandex.infra.controller.testutil.LocalHttpServerBasedTest;
import ru.yandex.infra.controller.util.ResourceUtils;
import ru.yandex.infra.stage.HttpServiceMetrics;
import ru.yandex.infra.stage.HttpServiceMetricsImpl;
import ru.yandex.infra.stage.cache.DummyCache;
import ru.yandex.infra.stage.dto.Checksum;
import ru.yandex.infra.stage.dto.DownloadableResource;
import ru.yandex.infra.stage.util.DummyHttpServiceMetrics;
import ru.yandex.infra.stage.util.JsonHttpGetter;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.infra.controller.util.ConfigUtils.token;

class SandboxReleaseGetterImplTest extends LocalHttpServerBasedTest {
    private static final String RESOURCE_TYPE = "POD_AGENT_BINARY";
    private static final String ATTRIBUTE_KEY = "cthulhu";
    private static final String ATTRIBUTE_VALUE = "fhtagn";
    private static final long RESOURCE_ID = 1145024159;
    private static final String RESOURCE_URL = "rbtorrent:80e028bbb0ed816aa4690a8b081917879b3a0ba8";
    private static final long INVALID_RESOURCE_ID = 1586612985;
    private static final String INVALID_RESOURCE_URL = "rbtorrent:12d764cd6b2f70d39bb0522246762d3f24aad4f5";
    private DummyServlet servlet;

    @Override
    protected Map<String, HttpServlet> getServlets() {
        servlet = new DummyServlet(ResourceUtils.readResource("sandbox_release.json"));
        return ImmutableMap.of(
                "/api/v1.0/resource",
                servlet,
                "/api/v1.0/resource/" + RESOURCE_ID,
                new DummyServlet(ResourceUtils.readResource("sandbox_resource_" + RESOURCE_ID + ".json")),
                "/api/v1.0/resource/" + INVALID_RESOURCE_ID,
                new DummyServlet(ResourceUtils.readResource("sandbox_resource_" + INVALID_RESOURCE_ID + ".json"))
        );
    }

    @Test
    void retrieveReleaseFromServer() {
        String token = "token";
        SandboxReleaseGetterImpl getter = new SandboxReleaseGetterImpl(getUrl(), token,
                ImmutableMap.of(ATTRIBUTE_KEY, ATTRIBUTE_VALUE), new DummyCache<>(), getJsonHttpGetter());
        ResourceWithMeta resource = FutureUtils.get1s(getter.getLatestRelease(RESOURCE_TYPE, true));
        ResourceWithMeta expectedResource = getExpectedResource("True");
        assertThat(resource, equalTo(expectedResource));
        Map<String, String> parameters = FutureUtils.get1s(servlet.getRequestParameters());
        assertThat(parameters, hasEntry("limit", "1"));
        assertThat(parameters, hasEntry("type", RESOURCE_TYPE));
        assertThat(parameters, hasEntry("attrs", String.format("{\"%s\":\"%s\"}", ATTRIBUTE_KEY, ATTRIBUTE_VALUE)));
        assertThat(parameters, hasEntry("state", "READY"));
        assertThat(parameters, hasEntry("order", "-id"));

        Map<String, String> headers = FutureUtils.get1s(servlet.getHeaders());
        assertThat(headers, hasEntry("Authorization", "OAuth " + token));
    }

    @Test
    void retrieveReleaseFromServerById() {
        String token = "token";
        SandboxReleaseGetterImpl getter = new SandboxReleaseGetterImpl(getUrl(), token,
                ImmutableMap.of(ATTRIBUTE_KEY, ATTRIBUTE_VALUE), new DummyCache<>(), getJsonHttpGetter());
        ResourceWithMeta resource = FutureUtils.get1s(getter.getReleaseByResourceId(RESOURCE_ID, true));
        ResourceWithMeta expectedResource = getExpectedResource("531709712");
        assertThat(resource, equalTo(expectedResource));
    }

    private ResourceWithMeta getExpectedResource(String backupTask) {
        Map<String, String> attributes = new TreeMap<>();
        attributes.put("released_xdc_acceptance", "True");
        attributes.put("released_xdc", "True");
        attributes.put("backup_task", backupTask);
        attributes.put("released", "stable");
        attributes.put("released_man_pre", "True");
        attributes.put("released_sas_test", "True");
        attributes.put("ttl", "inf");

        return new ResourceWithMeta(
                new DownloadableResource(RESOURCE_URL,
                        new Checksum("1efefea312f633a3efef7f48799b69ed", Checksum.Type.MD5)),
                Optional.of(new SandboxResourceMeta(518490877, RESOURCE_ID, attributes))
        );
    }

    @Test
    void validateResource() {
        String token = "token";
        SandboxReleaseGetterImpl getter = new SandboxReleaseGetterImpl(getUrl(), token, emptyMap(),
                new DummyCache<>(), getJsonHttpGetter());

        FutureUtils.get1s(getter.validateSandboxResource(RESOURCE_ID, RESOURCE_URL));

        assertThrows(RuntimeException.class,
                () -> FutureUtils.get1s(getter.validateSandboxResource(RESOURCE_ID,  INVALID_RESOURCE_URL)));

        assertThrows(RuntimeException.class,
                () -> FutureUtils.get1s(getter.validateSandboxResource(INVALID_RESOURCE_ID, INVALID_RESOURCE_URL)));
    }

    @Test
    void cacheTest() {
        DummyCache<ResourceWithMeta> cache = new DummyCache<>();
        ResourceWithMeta expectedResource = getExpectedResource("task");
        var cachedResourceId = 111L;
        cache.put(Long.toString(cachedResourceId), expectedResource);

        var metrics = new DummyHttpServiceMetrics();
        var jsonHttpGetter = new JsonHttpGetter(HttpServiceMetrics.Source.SANDBOX,
                getClient(),
                metrics);

        SandboxReleaseGetterImpl getter = new SandboxReleaseGetterImpl(getUrl(), "token", emptyMap(),
                cache, jsonHttpGetter);

        assertThat(cache.putCallsCount, equalTo(1));

        //Take from cache
        ResourceWithMeta resource = FutureUtils.get1s(getter.getReleaseByResourceId(cachedResourceId, true));
        assertThat(resource, equalTo(expectedResource));
        assertThat(metrics.requestsCount, equalTo(0));
        assertThat(cache.putCallsCount, equalTo(1));

        //absent in cache, do request
        ResourceWithMeta resource2 = FutureUtils.get1s(getter.getReleaseByResourceId(RESOURCE_ID, true));
        assertThat(resource2.getMeta().get().getResourceId(), equalTo(RESOURCE_ID));
        assertThat(metrics.requestsCount, equalTo(1));
        assertThat(cache.putCallsCount, equalTo(2));

        //absent in cache, do request
        ResourceWithMeta resource3 = FutureUtils.get1s(getter.getReleaseByResourceId(RESOURCE_ID, true));
        assertThat(resource3, equalTo(resource2));
        assertThat(metrics.requestsCount, equalTo(1));
        assertThat(cache.putCallsCount, equalTo(2));
    }

    @Test
    void doSingleHttpRequestInParallelTest() {
        var metrics = new DummyHttpServiceMetrics();
        var jsonHttpGetter = new JsonHttpGetter(HttpServiceMetrics.Source.SANDBOX,
                getClient(),
                metrics);

        SandboxReleaseGetterImpl getter = new SandboxReleaseGetterImpl(getUrl(), "token", emptyMap(),
                new DummyCache<>(), jsonHttpGetter);

        var futures = new ArrayList<CompletableFuture<ResourceWithMeta>>();

        for (int i = 0; i < 100; i++) {
            futures.add(getter.getReleaseByResourceId(RESOURCE_ID, true));
        }

        futures.forEach(FutureUtils::get1s);

        assertThat(metrics.requestsCount, equalTo(1));
    }

    @Test
    @Disabled
    void productionSandboxManualTest() throws Exception {
        SandboxReleaseGetterImpl getter = new SandboxReleaseGetterImpl("https://sandbox.yandex-team.ru",
                token(System.getenv("TOKEN_PATH")), ImmutableMap.of("released_xdc", "True"),
                new DummyCache<>(), getJsonHttpGetter());
        System.out.println(getter.getLatestRelease(RESOURCE_TYPE, true).get(20, TimeUnit.SECONDS));
    }

    private JsonHttpGetter getJsonHttpGetter() {
        return new JsonHttpGetter(HttpServiceMetrics.Source.SANDBOX,
                getClient(),
                Mockito.mock(HttpServiceMetricsImpl.class));
    }
}
