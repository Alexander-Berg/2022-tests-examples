package ru.yandex.infra.stage.docker;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.infra.controller.metrics.MapGaugeRegistry;
import ru.yandex.infra.controller.testutil.FutureUtils;
import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.cache.DummyCache;
import ru.yandex.infra.stage.concurrent.SerialExecutor;
import ru.yandex.infra.stage.dto.Checksum;
import ru.yandex.infra.stage.dto.DockerImageContents;
import ru.yandex.infra.stage.dto.DockerImageDescription;
import ru.yandex.infra.stage.dto.DownloadableResource;
import ru.yandex.infra.stage.util.AdaptiveRateLimiter;

import static com.spotify.hamcrest.future.FutureMatchers.futureCompletedWithValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.not;
import static ru.yandex.infra.stage.docker.DummyDockerResolveResultHandlerFactory.handler;
import static ru.yandex.infra.stage.docker.DummyDockerResolveResultHandlerFactory.trueHandler;

public class DockerResolverTest {
    public static final DockerImageDescription DESCRIPTION = new DockerImageDescription("registry.yandex.net", "rtc-base/bionic", "stable");
    public static final Duration INITIAL_RETRY_INTERVAL = Duration.ofMillis(1);
    public static final Duration MAX_RETRY_INTERVAL = Duration.ofMinutes(1);
    public static final Checksum CHECKSUM = new Checksum("8aa60d32a74eff26e16d82ac8fe969948c092dec8a799f2ba4f4a8e8a459a31c",
            Checksum.Type.SHA256);
    public static final DownloadableResource DOWNLOADABLE_RESOURCE = new DownloadableResource(
            "http://storage-int.mds.yandex.net:80/get-docker-registry/263079/06ec78bc-e91f-4fd4-af29-dcc554b59669", CHECKSUM);
    private static final boolean DEFAULT_ALLOW_FORCE_RESOLVE = true;
    private MapGaugeRegistry metricsRegistry;

    private static class FailDockerGetter implements DockerGetter {
        private boolean doFail = true;
        private final boolean alwaysFail;

        public FailDockerGetter() {
            this(false);
        }

        public FailDockerGetter(boolean alwaysFail) {
            this.alwaysFail = alwaysFail;
        }

        @Override
        public CompletableFuture<DockerImageContents> get(DockerImageDescription description) {
            CompletableFuture<DockerImageContents> result = new CompletableFuture<>();
            if (doFail) {
                result.completeExceptionally(new RuntimeException("always error"));
                if (!alwaysFail) {
                    doFail = false;
                }
            } else {
                result.complete(TestData.DOCKER_IMAGE_CONTENTS);
            }
            return result;
        }
    }

    private static class CountRequestDockerGetter implements DockerGetter {
        private int requestCount = 0;
        private final CompletableFuture<DockerImageContents> futureResult;

        public CountRequestDockerGetter(CompletableFuture<DockerImageContents> futureResult) {
            this.futureResult = futureResult;
        }

        @Override
        public CompletableFuture<DockerImageContents> get(DockerImageDescription description) {
            ++requestCount;
            return futureResult;
        }
    }

    private SerialExecutor serialExecutor;
    private CompletableFuture<DockerImageContents> futureResult;
    private DockerImagesResolver resolver;
    private DummyCache<DockerImageContents> persistent;
    private CountRequestDockerGetter getter;

    @BeforeEach
    void initExecutor() {
        serialExecutor = new SerialExecutor("test_thread");
        futureResult = new CompletableFuture<>();
        persistent = new DummyCache<>();
        metricsRegistry = new MapGaugeRegistry();
        getter = new CountRequestDockerGetter(futureResult);
        resolver = new DockerImagesResolverImpl(
                serialExecutor,
                getter,
                INITIAL_RETRY_INTERVAL,
                MAX_RETRY_INTERVAL,
                persistent,
                DEFAULT_ALLOW_FORCE_RESOLVE,
                metricsRegistry,
                AdaptiveRateLimiter.EMPTY
        );
    }

    @AfterEach
    void destroyExecutor() {
        serialExecutor.shutdown();
    }

    @Test
    void dockerRetryTest() {
        DockerImagesResolver resolver = new DockerImagesResolverImpl(
                serialExecutor,
                new FailDockerGetter(),
                INITIAL_RETRY_INTERVAL,
                MAX_RETRY_INTERVAL,
                persistent,
                DEFAULT_ALLOW_FORCE_RESOLVE,
                metricsRegistry,
                AdaptiveRateLimiter.EMPTY
        );
        CompletableFuture<Void> futureOk = new CompletableFuture<>();
        CompletableFuture<Void> futureFail = new CompletableFuture<>();
        resolver.registerResultHandler("id", handler(
                () -> futureOk.complete(null),
                () -> futureFail.complete(null)));
        resolver.addResolve(TestData.DOCKER_IMAGE_DESCRIPTION, "id");
        assertThat(metricsRegistry.getGaugeValue(DockerImagesResolverImpl.METRIC_SCHEDULED_REQUESTS_COUNT), equalTo(1L));
        FutureUtils.get1s(futureOk);
        FutureUtils.get1s(futureFail);
        assertThat(metricsRegistry.getGaugeValue(DockerImagesResolverImpl.METRIC_SCHEDULED_REQUESTS_COUNT), equalTo(0L));
        assertThat(metricsRegistry.getGaugeValue(DockerImagesResolverImpl.METRIC_SEND_REQUESTS_COUNT), equalTo(2L));
        assertThat(metricsRegistry.getGaugeValue(DockerImagesResolverImpl.METRIC_FORCE_RESOLVE_COUNT), equalTo(0L));
        assertThat(metricsRegistry.getGaugeValue(DockerImagesResolverImpl.METRIC_FAILED_REQUESTS_COUNT), equalTo(1L));
        assertThat(metricsRegistry.getGaugeValue(DockerImagesResolverImpl.METRIC_SUCCEED_REQUESTS_COUNT), equalTo(1L));
        assertThat(metricsRegistry.getGaugeValue(DockerImagesResolverImpl.METRIC_IMAGES_COUNT), equalTo(1));
    }

    @Test
    void dockerStopRetryAfterRemovingSubscriberTest() throws InterruptedException {
        DockerImagesResolver resolver = new DockerImagesResolverImpl(
                serialExecutor,
                new FailDockerGetter(true),
                INITIAL_RETRY_INTERVAL,
                MAX_RETRY_INTERVAL,
                persistent,
                DEFAULT_ALLOW_FORCE_RESOLVE,
                metricsRegistry,
                AdaptiveRateLimiter.EMPTY
        );
        AtomicInteger failuresCounter = new AtomicInteger();
        resolver.registerResultHandler("id", handler(null, () -> {
            if (failuresCounter.incrementAndGet() == 3) {
                resolver.removeResolve(TestData.DOCKER_IMAGE_DESCRIPTION, "id");
            }
        }));
        resolver.addResolve(TestData.DOCKER_IMAGE_DESCRIPTION, "id");
        Thread.sleep(1000);

        assertThat(metricsRegistry.getGaugeValue(DockerImagesResolverImpl.METRIC_SCHEDULED_REQUESTS_COUNT), equalTo(0L));
        assertThat((Long)metricsRegistry.getGaugeValue(DockerImagesResolverImpl.METRIC_SEND_REQUESTS_COUNT), greaterThan(3L));
        assertThat((Long)metricsRegistry.getGaugeValue(DockerImagesResolverImpl.METRIC_FAILED_REQUESTS_COUNT), greaterThan(3L));
    }

    @Test
    void dockerStatusTest() {
        CompletableFuture<DockerImageContents> futureOk = new CompletableFuture<>();
        resolver.registerResultHandler("id", trueHandler((x, result) -> futureOk.complete(result), null));
        resolver.addResolve(TestData.DOCKER_IMAGE_DESCRIPTION, "id");
        futureResult.complete(TestData.DOCKER_IMAGE_CONTENTS);
        assertThat(FutureUtils.get1s(futureOk), equalTo(TestData.DOCKER_IMAGE_CONTENTS));
        assertThat(resolver.getResolveStatus(TestData.DOCKER_IMAGE_DESCRIPTION).getResult().get(), equalTo(
                TestData.DOCKER_IMAGE_CONTENTS));
    }

    @Test
    void dockerSubUnsubTest() {
        CompletableFuture<Void> future1 = new CompletableFuture<>();
        CompletableFuture<Void> future2 = new CompletableFuture<>();
        CompletableFuture<Void> future3 = new CompletableFuture<>();
        resolver.registerResultHandler("id1", handler(() -> future1.complete(null), null));
        resolver.registerResultHandler("id2", handler(() -> future2.complete(null), null));
        resolver.registerResultHandler("id3", handler(() -> future3.complete(null), null));
        resolver.addResolve(TestData.DOCKER_IMAGE_DESCRIPTION, "id1");
        resolver.addResolve(TestData.DOCKER_IMAGE_DESCRIPTION, "id2");
        resolver.unregisterResultHandler("id2");
        resolver.addResolve(TestData.DOCKER_IMAGE_DESCRIPTION, "id3");
        resolver.removeResolve(TestData.DOCKER_IMAGE_DESCRIPTION, "id3");
        futureResult.complete(TestData.DOCKER_IMAGE_CONTENTS);
        FutureUtils.get1s(future1);
        assertThat(future2, not(futureCompletedWithValue()));
        assertThat(future3, not(futureCompletedWithValue()));
    }

    @Test
    void imageSharingTest() {
        List<CompletableFuture<Void>> futures = ImmutableList.of(new CompletableFuture<>(), new CompletableFuture<>());
        List<String> ids = ImmutableList.of("id1", "id2");
        for (int i = 0; i < ids.size(); ++i) {
            int index = i;
            String id = ids.get(i);
            resolver.registerResultHandler(id, handler(
                    () -> futures.get(index).complete(null),
                    null));
        }
        ids.forEach((id) -> resolver.addResolve(TestData.DOCKER_IMAGE_DESCRIPTION, id));
        futureResult.complete(TestData.DOCKER_IMAGE_CONTENTS);
        futures.forEach(FutureUtils::get1s);
        assertThat(resolver.getResolveStatus(TestData.DOCKER_IMAGE_DESCRIPTION),
                equalTo(DockerResolveStatus.getReadyStatus(TestData.DOCKER_IMAGE_CONTENTS)));
    }

    @Test
    void restoreFromStateWithSavedEntryPoint() {
        DummyCache<DockerImageContents> cache = new DummyCache<>();
        cache.map.put(DockerImagesResolverImpl.getImageKeyForCache(TestData.DOCKER_IMAGE_DESCRIPTION), TestData.DOCKER_IMAGE_CONTENTS);
        DockerImagesResolverImpl resolver = new DockerImagesResolverImpl(
                serialExecutor,
                getter,
                INITIAL_RETRY_INTERVAL,
                MAX_RETRY_INTERVAL,
                cache,
                DEFAULT_ALLOW_FORCE_RESOLVE,
                metricsRegistry,
                AdaptiveRateLimiter.EMPTY
        );
        assertThat(resolver.getResolveStatus(TestData.DOCKER_IMAGE_DESCRIPTION),
                equalTo(DockerResolveStatus.getReadyStatus(TestData.DOCKER_IMAGE_CONTENTS)));
    }

    @Test
    void removeFromStateAfterAllReferencesRemoved() {
        String id1 = "unit1";
        String id2 = "unit2";
        DockerImagesResolverImpl resolver = new DockerImagesResolverImpl(
                serialExecutor,
                getter,
                INITIAL_RETRY_INTERVAL,
                MAX_RETRY_INTERVAL,
                persistent,
                DEFAULT_ALLOW_FORCE_RESOLVE,
                metricsRegistry,
                AdaptiveRateLimiter.EMPTY
        );

        // TODO: merge register + resolve into one method
        resolver.registerResultHandler(id1, handler(null, null));
        resolver.registerResultHandler(id2, handler(null, null));
        resolver.addResolve(TestData.DOCKER_IMAGE_DESCRIPTION, id1);
        resolver.addResolve(TestData.DOCKER_IMAGE_DESCRIPTION, id2);

        futureResult.complete(TestData.DOCKER_IMAGE_CONTENTS);
        FutureUtils.get1s(persistent.putCallFuture);

        assertThat(persistent.putCallsCount, equalTo(1));
        assertThat(persistent.removeCallsCount, equalTo(0));

        resolver.removeResolve(TestData.DOCKER_IMAGE_DESCRIPTION, id1);
        resolver.unregisterResultHandler(id1);
        assertThat(persistent.map, hasEntry(
                DockerImagesResolverImpl.getImageKeyForCache(TestData.DOCKER_IMAGE_DESCRIPTION), TestData.DOCKER_IMAGE_CONTENTS));

        resolver.removeResolve(TestData.DOCKER_IMAGE_DESCRIPTION, id2);
        resolver.unregisterResultHandler(id2);
        assertThat(persistent.removeCallsCount, equalTo(1));
        assertThat(persistent.map.size(), equalTo(0));
    }

    @Test
    void forceResolveIgnoresReadyStatusToPerformOneMoreRequestTest() {
        String subscriber = "subscriberId";
        resolver.addResolve(DESCRIPTION, subscriber);
        resolver.addResolve(DESCRIPTION, subscriber);
        resolver.addResolve(DESCRIPTION, subscriber);

        assertThat("Should perform only one resolve request for the same description", getter.requestCount, equalTo(1));

        resolver.forceResolve(DESCRIPTION);

        assertThat(getter.requestCount, equalTo(2));

        resolver.forceResolve(DESCRIPTION);
        resolver.forceResolve(DESCRIPTION);
        resolver.forceResolve(DESCRIPTION);

        assertThat(getter.requestCount, equalTo(5));
    }

    @Test
    void forceResolveShouldNotBreakAddRequestLogicTest() {
        String subscriber = "subscriberId";

        resolver.addResolve(DESCRIPTION, subscriber);
        resolver.forceResolve(DESCRIPTION);

        assertThat(getter.requestCount, equalTo(2));

        resolver.addResolve(DESCRIPTION, subscriber);
        resolver.addResolve(DESCRIPTION, subscriber);

        assertThat("Checking that addResolve don't perform new requests after forceResolve", getter.requestCount, equalTo(2));
    }

    @Test
    void getImageKeyForCacheTest() {
        assertThat(DockerImagesResolverImpl.getImageKeyForCache(new DockerImageDescription("registry.yandex.net", "rtc-base/bionic", "stable")), equalTo("registry.yandex.net/rtc-base/bionic:stable"));
    }

    @Test
    void forceResolveWithEmptyCacheTest() {
        resolver.forceResolve(DESCRIPTION);

        assertThat(getter.requestCount, equalTo(1));
        assertThat(metricsRegistry.getGaugeValue(DockerImagesResolverImpl.METRIC_FORCE_RESOLVE_COUNT), equalTo(1L));
    }

    @ParameterizedTest
    @CsvSource({
            "false, 0",
            "true, 2"
    })
    void allowForceResolveConfigOptionTest(boolean allowForceResolve, int expectedRequestsCount) {
        DockerImagesResolver resolver = new DockerImagesResolverImpl(
                serialExecutor,
                getter,
                INITIAL_RETRY_INTERVAL,
                MAX_RETRY_INTERVAL,
                persistent,
                allowForceResolve,
                metricsRegistry,
                AdaptiveRateLimiter.EMPTY
        );

        resolver.forceResolve(DESCRIPTION);
        resolver.forceResolve(DESCRIPTION);

        assertThat(getter.requestCount, equalTo(expectedRequestsCount));
        assertThat(metricsRegistry.getGaugeValue(DockerImagesResolverImpl.METRIC_FORCE_RESOLVE_COUNT),
                equalTo((long)expectedRequestsCount));
        assertThat(metricsRegistry.getGaugeValue(DockerImagesResolverImpl.METRIC_SEND_REQUESTS_COUNT), equalTo((long)expectedRequestsCount));
    }

    @ParameterizedTest
    @CsvSource({
            "valid-name,valid-tag,true",
            "valid-name,invalid tag,false",
            "invalid name,valid-tag,false",
            "invalid name,invalid tag,false",
            "name,tagWithCapitals,true",
            "nameWithCapitals,tag,false",
            "name1,tag:unstable,true",
            "name/registry/dots:,21-01-20,true",
            "name^,tag,false",
            "name=,tag,false",
            "+name,tag,false",
            "name-with-dashes,valid-like-name,true",
            "dir/name.01_2:,Tag_10-10-21.9:12:57,true"
    })
    void validateDockerImageDescriptionTest(String imageName, String imageTag, boolean isValid) {
        DockerImageDescription description = new DockerImageDescription("registry.yandex.net", imageName, imageTag);
        Assertions.assertEquals(isValid, DockerImagesResolverImpl.validateDescription(description).isEmpty(),
                String.format("Image name '%s', image tag '%s'", imageName, imageTag));
    }
}
