package ru.yandex.infra.sidecars_updater.sandbox;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.sidecars_updater.sidecars.Sidecar;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.sidecars_updater.TestData.DEFAULT_HTTP_PROXY_URL;
import static ru.yandex.infra.sidecars_updater.TestData.DEFAULT_LAYER_1_REVISION;
import static ru.yandex.infra.sidecars_updater.TestData.DEFAULT_LAYER_ATTRIBUTES;
import static ru.yandex.infra.sidecars_updater.TestData.DEFAULT_LAYER_INFO;
import static ru.yandex.infra.sidecars_updater.TestData.DEFAULT_LAYER_TYPE;
import static ru.yandex.infra.sidecars_updater.TestData.DEFAULT_SKYNET_ID_URL;
import static ru.yandex.infra.sidecars_updater.TestData.HTTP_URL_1;
import static ru.yandex.infra.sidecars_updater.TestData.HTTP_URL_2;
import static ru.yandex.infra.sidecars_updater.util.Utils.getOtherValue;

public class SandboxInfoGetterImplTest {
    private static final List<String> ALL_REAL_URLS = List.of(
            "sbr:" + DEFAULT_LAYER_1_REVISION,
            DEFAULT_SKYNET_ID_URL,
            DEFAULT_HTTP_PROXY_URL,
            HTTP_URL_1,
            HTTP_URL_2
    );
    private static final String REAL_URL = "real_url";
    private static final String FAKE_URL = "fake_url";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(1);
    private static final int DEFAULT_RESOURCES_SIZE_LIMIT = 1000;
    private static final boolean DEFAULT_HIDDEN = true;

    private static Stream<Arguments> refreshTypeCacheTestParameters() {
        ConcurrentHashMap<Object, Object> cacheWithDefaultInfo = new ConcurrentHashMap<>();
        ALL_REAL_URLS.forEach(url -> cacheWithDefaultInfo.put(url, DEFAULT_LAYER_INFO));

        ConcurrentHashMap<Object, Object> cacheWithOtherInfo = new ConcurrentHashMap<>();
        SandboxResourceInfo otherInfo = DEFAULT_LAYER_INFO.withRevision(DEFAULT_LAYER_1_REVISION + 1)
                .withType(getOtherValue(Sidecar.Type.values(), DEFAULT_LAYER_TYPE).toString());
        ALL_REAL_URLS.forEach(url -> cacheWithOtherInfo.put(url, otherInfo));

        return Stream.of(
                Arguments.of(new ConcurrentHashMap<>(), cacheWithDefaultInfo),
                Arguments.of(cacheWithOtherInfo, cacheWithOtherInfo)
        );
    }

    @ParameterizedTest
    @MethodSource("refreshTypeCacheTestParameters")
    void refreshTypeCacheTest(ConcurrentMap<String, SandboxResourceInfo> infoByUrlCache,
                              ConcurrentMap<String, SandboxResourceInfo> expectedCache) {
        SandboxClient sandboxClient = mock(SandboxClient.class);
        Sidecar.Type type = DEFAULT_LAYER_TYPE;
        CompletableFuture<List<SandboxResourceInfo>> getResourcesCalled = sandboxClient.getResources(
                eq(type.toString()),
                anyMap(),
                anyBoolean(),
                anyLong()
        );
        when(getResourcesCalled).thenReturn(CompletableFuture.completedFuture(List.of(DEFAULT_LAYER_INFO)));

        SandboxInfoGetterImpl sandboxInfoGetterImpl = new SandboxInfoGetterImpl(
                sandboxClient, DEFAULT_TIMEOUT, DEFAULT_RESOURCES_SIZE_LIMIT, infoByUrlCache
        );
        sandboxInfoGetterImpl.refreshTypeCache(type.toString(), DEFAULT_LAYER_ATTRIBUTES, DEFAULT_HIDDEN);

        ConcurrentMap<String, SandboxResourceInfo> actualCache = sandboxInfoGetterImpl.getInfoByUrlCache();
        assertThat(actualCache, equalTo(expectedCache));
    }

    private static Stream<Arguments> getSandboxResourceInfoTestParameters() {
        return Stream.of(
                Arguments.of(REAL_URL, Optional.of(DEFAULT_LAYER_INFO)),
                Arguments.of(FAKE_URL, Optional.empty())
        );
    }

    @ParameterizedTest
    @MethodSource("getSandboxResourceInfoTestParameters")
    void getSandboxResourceInfoTest(String url, Optional<SandboxResourceInfo> expectedInfo) {
        SandboxClient sandboxClient = mock(SandboxClient.class);
        SandboxInfoGetter sandboxInfoGetter = new SandboxInfoGetterImpl(
                sandboxClient, DEFAULT_TIMEOUT, DEFAULT_RESOURCES_SIZE_LIMIT,
                new ConcurrentHashMap<>(Map.of(REAL_URL, DEFAULT_LAYER_INFO))
        );

        Optional<SandboxResourceInfo> actualInfo = sandboxInfoGetter.getSandboxResourceInfoByUrl(url);
        assertThat(actualInfo, equalTo(expectedInfo));
        verify(sandboxClient, never()).getResources(any(), anyMap(), anyBoolean(), anyLong());
    }
}
