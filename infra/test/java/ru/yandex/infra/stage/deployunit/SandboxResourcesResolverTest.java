package ru.yandex.infra.stage.deployunit;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.cache.DummyCache;
import ru.yandex.infra.stage.concurrent.SerialExecutor;
import ru.yandex.infra.stage.dto.DeployUnitSpec;
import ru.yandex.infra.stage.dto.DownloadableResource;
import ru.yandex.infra.stage.dto.ReplicaSetUnitSpec;
import ru.yandex.infra.stage.util.AdaptiveRateLimiter;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.api.TReplicaSetSpec;
import ru.yandex.yp.client.pods.TLayer;
import ru.yandex.yp.client.pods.TPodAgentSpec;
import ru.yandex.yp.client.pods.TResource;
import ru.yandex.yp.client.pods.TResourceGang;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static ru.yandex.infra.stage.TestData.POD_AGENT_CONFIG_EXTRACTOR;

public class SandboxResourcesResolverTest {

    SerialExecutor serialExecutor;
    String deployUnitId = "test-unit";
    String deployUnitId0 = deployUnitId + "0";
    String deployUnitId1 = deployUnitId + "1";
    String deployUnitId2 = deployUnitId + "2";

    DeployUnitSpec spec;

    private static final String FIRST_RESOURCE_ID = "sbrTest1";
    private static final String SECOND_RESOURCE_ID = "sbrTest2";
    private static final String LAYER_ID = "sbrTest3";

    private static final String SANDBOX_ID_1 = "111";
    private static final String SANDBOX_ID_2 = "222";
    private static final String SANDBOX_ID_3 = "333";

    private static final String SBR_PREFIX = "sbr:";


    private static final TResource.Builder resource1 = TResource.newBuilder().setUrl(getSbrUrl(SANDBOX_ID_1)).setId(FIRST_RESOURCE_ID);
    private static final TResource.Builder resource2 = TResource.newBuilder().setUrl(getSbrUrl(SANDBOX_ID_2)).setId(SECOND_RESOURCE_ID);
    private static final TLayer.Builder layer1 = TLayer.newBuilder().setUrl(getSbrUrl(SANDBOX_ID_3)).setId(LAYER_ID);

    private static final String RESOLVED_STATUS = "rbtorrent:resolved";
    private static final RuntimeException FAILED_TO_RESOLVE_EXCEPTION = new RuntimeException("Failed to resolve");

    private static final SandboxResourcesResolveResultHandler handler = new DummySandboxResourcesResolveResultHandler();

    private static class CountSandboxResourcesGetter implements SandboxResourcesGetter {
        private int requests = 1;

        @Override
        public CompletableFuture<String> get(String resourceId) {
            requests++;
            CompletableFuture<String> result = new CompletableFuture<>();
            result.completeExceptionally(FAILED_TO_RESOLVE_EXCEPTION);
            return result;
        }
    }

    private static class SuccessfulSandboxResourceGetter implements SandboxResourcesGetter {

        private final String resolvedStatus;
        public SuccessfulSandboxResourceGetter(String resolvedStatus) {
            this.resolvedStatus = resolvedStatus;
        }

        @Override
        public CompletableFuture<String> get(String resourceId) {
            return CompletableFuture.completedFuture(resolvedStatus);
        }
    }

    private static class CustomFuturesSandboxResourcesGetter implements SandboxResourcesGetter {
        private final Map<String, Integer> resourceIdToRequests = new ConcurrentHashMap<>();
        private final Map<String, Supplier<CompletableFuture<String>>> resourceIdToFutures;

        public CustomFuturesSandboxResourcesGetter(Map<String, Supplier<CompletableFuture<String>>> resourceIdToFutures) {
            this.resourceIdToFutures = resourceIdToFutures;
        }

        @Override
        public CompletableFuture<String> get(String resourceId) {
            resourceIdToRequests.put(resourceId, resourceIdToRequests.getOrDefault(resourceId, 0) + 1);
            return resourceIdToFutures.get(resourceId).get();
        }

        public Map<String, Integer> getResourceIdToRequests() {
            return resourceIdToRequests;
        }

        public Integer getRequests(String resourceUrl) {
            return resourceIdToRequests.get(getSandboxId(resourceUrl));
        }
    }

    private static String getSbrUrl(String sandboxId) {
        return SBR_PREFIX + sandboxId;
    }

    private static String getSandboxId(String url) {
        return url.substring(SBR_PREFIX.length());
    }

    @BeforeEach
    void initExecutor() {
        spec = getDeployUnitSpecWithResources(List.of(resource1, resource2), List.of(layer1));
        serialExecutor = new SerialExecutor("test_thread");
    }

    void waitForState(Supplier<Boolean> supplier) {
        try {
            for (int i = 0; i < 20; i++) {
                if (supplier.get())
                    return;
                Thread.sleep(100);
            }
        } catch (InterruptedException ex)
        {}
    }

    @Test
    void successfulResolveTest() {
        SuccessfulSandboxResourceGetter succeededGetter = new SuccessfulSandboxResourceGetter(RESOLVED_STATUS);
        SandboxResourcesResolver resolver = new SandboxResourcesResolverImpl(
                serialExecutor, succeededGetter, new DummyCache<>(), Duration.ZERO, Duration.ofMinutes(1), AdaptiveRateLimiter.EMPTY
        );

        resolver.registerResultHandlerAndTryGet(deployUnitId, handler, spec, emptyList());
        waitForState(() -> resolver.getResolveStatus(deployUnitId).isReady());

        checkResolvedResources(resolver, deployUnitId, List.of(FIRST_RESOURCE_ID, SECOND_RESOURCE_ID, LAYER_ID),
                succeededGetter.resolvedStatus);
    }

    @Test
    void failureResolveTest() {
        CountSandboxResourcesGetter getter = new CountSandboxResourcesGetter();
        SandboxResourcesResolver resolver = new SandboxResourcesResolverImpl(
                serialExecutor, getter, new DummyCache<>(), Duration.ZERO, Duration.ofMinutes(1), AdaptiveRateLimiter.EMPTY
        );

        resolver.registerResultHandlerAndTryGet(deployUnitId, handler, spec, emptyList());
        waitForState(() -> resolver.getResolveStatus(deployUnitId).getFailureDetails().isPresent());

        assertThat(resolver.getResolveStatus(deployUnitId).isReady(), equalTo(false));
        assertThat(resolver.get(deployUnitId).get(FIRST_RESOURCE_ID), equalTo(null));
        assertThat(resolver.get(deployUnitId).get(SECOND_RESOURCE_ID), equalTo(null));
        assertThat(resolver.get(deployUnitId).get(LAYER_ID), equalTo(null));
    }

    @Test
    void urlValidationTest() {
        SuccessfulSandboxResourceGetter succeededGetter = new SuccessfulSandboxResourceGetter("resolved");
        SandboxResourcesResolver resolver = new SandboxResourcesResolverImpl(
                serialExecutor, succeededGetter, new DummyCache<>(), Duration.ZERO, Duration.ofMinutes(1), AdaptiveRateLimiter.EMPTY
        );

        resolver.registerResultHandlerAndTryGet(deployUnitId, handler, spec, emptyList());
        waitForState(() -> resolver.getResolveStatus(deployUnitId).getFailureDetails().isPresent());
    }

    @Test
    void takeFromCacheTest() {
        SuccessfulSandboxResourceGetter succeededGetter = new SuccessfulSandboxResourceGetter(RESOLVED_STATUS);
        final DummyCache<DownloadableResource> cache = new DummyCache<>();
        cache.put(SANDBOX_ID_1, new DownloadableResource("rbtorrent:cache1", null));
        cache.put(SANDBOX_ID_2, new DownloadableResource("invalidURL", null));
        SandboxResourcesResolver resolver = new SandboxResourcesResolverImpl(
                serialExecutor, succeededGetter, cache, Duration.ZERO, Duration.ofMinutes(1), AdaptiveRateLimiter.EMPTY
        );

        resolver.registerResultHandlerAndTryGet(deployUnitId, handler, spec, emptyList());
        waitForState(() -> resolver.get(deployUnitId).get(LAYER_ID) != null);

        assertThat(resolver.getResolveStatus(deployUnitId).isReady(), equalTo(false));
        assertThat(resolver.get(deployUnitId).get(FIRST_RESOURCE_ID), equalTo("rbtorrent:cache1"));
        assertThat(resolver.get(deployUnitId).get(SECOND_RESOURCE_ID), equalTo(null));
        assertThat(resolver.get(deployUnitId).get(LAYER_ID), equalTo(succeededGetter.resolvedStatus));
    }

    @Test
    void firstFailsSandboxResourcesTest() {
        Map<String, Supplier<CompletableFuture<String>>> resourceIdToFutures = Map.of(
                SANDBOX_ID_1, getResolvedFuturesSince(4),
                SANDBOX_ID_2, getResolvedFuturesSince(3),
                SANDBOX_ID_3, getResolvedFuturesSince(2)
        );

        CustomFuturesSandboxResourcesGetter sandboxResourceGetter =
                new CustomFuturesSandboxResourcesGetter(resourceIdToFutures);

        SandboxResourcesResolver resolver = new SandboxResourcesResolverImpl(
                serialExecutor, sandboxResourceGetter, new DummyCache<>(),
                Duration.ZERO, Duration.ofMinutes(1), AdaptiveRateLimiter.EMPTY
        );

        resolver.registerResultHandlerAndTryGet(deployUnitId0, handler, spec, emptyList());
        resolver.registerResultHandlerAndTryGet(deployUnitId1, handler, spec, emptyList());
        resolver.registerResultHandlerAndTryGet(deployUnitId2, handler, spec, emptyList());

        waitForState(() -> resolver.getResolveStatus(deployUnitId0).isReady());
        waitForState(() -> resolver.getResolveStatus(deployUnitId1).isReady());
        waitForState(() -> resolver.getResolveStatus(deployUnitId2).isReady());

        checkResolvedResources(resolver, deployUnitId0, List.of(FIRST_RESOURCE_ID, SECOND_RESOURCE_ID, LAYER_ID),
                RESOLVED_STATUS);
        checkResolvedResources(resolver, deployUnitId1, List.of(FIRST_RESOURCE_ID, SECOND_RESOURCE_ID, LAYER_ID),
                RESOLVED_STATUS);
        checkResolvedResources(resolver, deployUnitId2, List.of(FIRST_RESOURCE_ID, SECOND_RESOURCE_ID, LAYER_ID),
                RESOLVED_STATUS);

        Map<String, Integer> resourceIdToRequests = sandboxResourceGetter.getResourceIdToRequests();
        assertThat(resourceIdToRequests.get(SANDBOX_ID_1), equalTo(4));
        assertThat(resourceIdToRequests.get(SANDBOX_ID_2), equalTo(3));
        assertThat(resourceIdToRequests.get(SANDBOX_ID_3), equalTo(2));
    }

    private Supplier<CompletableFuture<String>> getResolvedFuturesSince(int firstResolved) {
        AtomicInteger attempts = new AtomicInteger(0);
        return () -> {
            CompletableFuture<String> future = new CompletableFuture<>();
            if (attempts.incrementAndGet() < firstResolved) {
                future.completeExceptionally(FAILED_TO_RESOLVE_EXCEPTION);
            } else {
                future.complete(RESOLVED_STATUS);
            }
            return future;
        };
    }

    @Test
    void registerInvalidResourcesAndThenDeleteAllTest() {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(FAILED_TO_RESOLVE_EXCEPTION);
        Supplier<CompletableFuture<String>> getFailedFutures = () -> future;

        Map<String, Supplier<CompletableFuture<String>>> resourceIdToFutures = Map.of(
                SANDBOX_ID_1, getFailedFutures,
                SANDBOX_ID_2, getResolvedFuturesSince(1)
        );

        CustomFuturesSandboxResourcesGetter sandboxResourceGetter =
                new CustomFuturesSandboxResourcesGetter(resourceIdToFutures);

        SandboxResourcesResolver resolver = new SandboxResourcesResolverImpl(
                serialExecutor, sandboxResourceGetter, new DummyCache<>(),
                Duration.ZERO, Duration.ofMinutes(1), AdaptiveRateLimiter.EMPTY
        );

        registerWithResources(resolver, deployUnitId0, List.of(resource1), List.of());
        registerWithResources(resolver, deployUnitId0, List.of(resource2), List.of());

        int resource1Requests = sandboxResourceGetter.getRequests(resource1.getUrl());

        waitForState(() -> resolver.getResolveStatus(deployUnitId0).isReady());

        checkResolvedResources(resolver, deployUnitId0, List.of(SECOND_RESOURCE_ID), RESOLVED_STATUS);

        int resource1RequestsAfterResolved = sandboxResourceGetter.getRequests(resource1.getUrl());

        assertThat(resource1Requests + 1, greaterThanOrEqualTo(resource1RequestsAfterResolved));
        // one additional request can be parallel to the second register call
    }

    private void registerWithResources(SandboxResourcesResolver resolver,
                                       String duId,
                                       List<TResource.Builder> resources,
                                       List<TLayer.Builder> layers) {
        resolver.registerResultHandlerAndTryGet(
                duId, handler,
                getDeployUnitSpecWithResources(resources, layers),
                emptyList()
        );
    }

    private void checkResolvedResources(SandboxResourcesResolver resolver, String fullDeployUnitId,
                                        List<String> resourceIds, String resolvedStatus) {

        assertThat(resolver.getResolveStatus(fullDeployUnitId).isReady(), equalTo(true));
        resourceIds.forEach(resourceId ->
                assertThat(resolver.get(fullDeployUnitId).get(resourceId), equalTo(resolvedStatus)));
    }

    private static DeployUnitSpec getDeployUnitSpecWithResources(List<TResource.Builder> resources, List<TLayer.Builder> layers) {
        var resourceGang = TResourceGang.newBuilder();
        resources.forEach(resourceGang::addStaticResources);
        layers.forEach(resourceGang::addLayers);

        var podAgentSpec = TPodAgentSpec.newBuilder().setResources(resourceGang);
        var podAgentPayload = DataModel.TPodSpec.TPodAgentPayload.newBuilder().setSpec(podAgentSpec);
        var podSpec = DataModel.TPodSpec.newBuilder().setPodAgentPayload(podAgentPayload);
        var podTemplateSpec = TPodTemplateSpec.newBuilder().setSpec(podSpec);
        var replicaSetSpec = TReplicaSetSpec.newBuilder().setPodTemplateSpec(podTemplateSpec);

        var specDetails = new ReplicaSetUnitSpec(replicaSetSpec.build(), emptyMap(), POD_AGENT_CONFIG_EXTRACTOR);
        return TestData.DEPLOY_UNIT_SPEC.withDetails(specDetails);
    }

    @AfterEach
    void destroyExecutor() {
        serialExecutor.shutdown();
    }
}
