package ru.yandex.infra.stage.podspecs;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.cache.Cache;
import ru.yandex.infra.stage.cache.DummyCache;
import ru.yandex.infra.stage.concurrent.SerialExecutor;
import ru.yandex.infra.stage.dto.Checksum;
import ru.yandex.infra.stage.dto.DownloadableResource;
import ru.yandex.infra.stage.util.SettableClock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.controller.testutil.FutureUtils.get1s;

class SandboxResourceSupplierTest {
    private static final String RESOURCE_TYPE = "POD_AGENT_BINARY";
    private static final String RESOURCE_NAME = "234234";
    private static final ResourceWithMeta OLD_RESOURCE =
            new ResourceWithMeta(new DownloadableResource("url1", Checksum.EMPTY), Optional.of(TestData.RESOURCE_META));
    private static final ResourceWithMeta NEW_RESOURCE =
            new ResourceWithMeta(new DownloadableResource("url2", Checksum.EMPTY), Optional.of(TestData.RESOURCE_META));
    private static final Duration RETRY_INTERVAL = Duration.ofMillis(100);

    private SerialExecutor executor;
    private SettableClock clock;
    private DummyReleaseGetter releaseGetter;
    private volatile CompletableFuture<?> updateFuture;
    private DummyCache<ResourceWithMeta> cache;

    @BeforeEach
    void before() {
        executor = new SerialExecutor(getClass().getName());
        clock = new SettableClock();
        releaseGetter = new DummyReleaseGetter();
        cache = new DummyCache<>();
    }

    @AfterEach
    void after() {
        executor.shutdown();
    }

    @Test
    void updateResource() {
        setResource(OLD_RESOURCE);
        SandboxResourceSupplier supplier = defaultSupplier(cache);
        get1s(supplier.start());
        assertThat(supplier.get(), equalTo(OLD_RESOURCE));
        setResource(NEW_RESOURCE);
        waitForUpdateAttempt();
        assertThat(supplier.get(), equalTo(NEW_RESOURCE));
    }

    @Test
    void updateResourceAfterError() {
        setResource(OLD_RESOURCE);
        SandboxResourceSupplier supplier = defaultSupplier(cache);
        get1s(supplier.start());
        assertThat(supplier.get(), equalTo(OLD_RESOURCE));
        setError(new RuntimeException("error"));
        waitForUpdateAttempt();
        assertThat(supplier.get(), equalTo(OLD_RESOURCE));
        clock.incrementSecond();
        assertThat(supplier.timeSinceLastUpdate(), equalTo(Duration.ofSeconds(1)));
        setResource(NEW_RESOURCE);
        waitForUpdateAttempt();
        assertThat(supplier.get(), equalTo(NEW_RESOURCE));
        assertThat(supplier.timeSinceLastUpdate(), equalTo(Duration.ZERO));
    }

    @Test
    void retryErrorOnStart() {
        setError(new RuntimeException("error"));
        SandboxResourceSupplier supplier = new SandboxResourceSupplier(executor, releaseGetter, RESOURCE_TYPE, true,
                Duration.ofDays(1), RETRY_INTERVAL, clock, RESOURCE_NAME, cache, this::notifyOnUpdateAttempt);
        CompletableFuture<?> started = supplier.start();
        waitForUpdateAttempt();
        assertThat("Getter should not be able to start", !started.isDone());
        setResource(OLD_RESOURCE);
        waitForUpdateAttempt();
        assertThat(supplier.get(), equalTo(OLD_RESOURCE));
    }

    @Test
    void startFromPersistIfSandboxNotWorking() {
        setError(new RuntimeException("error"));
        SandboxResourceSupplier supplier = defaultSupplier(new DummyCache<>(Map.of(RESOURCE_NAME, OLD_RESOURCE)));
        get1s(supplier.start());
        assertThat(supplier.get(), equalTo(OLD_RESOURCE));
    }

    @Test
    void returnOldResourceUntilNewPersisted() {
        DummyCache<ResourceWithMeta> persistent = new DummyCache<>(Map.of(RESOURCE_NAME, OLD_RESOURCE));
        SandboxResourceSupplier supplier = defaultSupplier(persistent);
        setResource(NEW_RESOURCE);
        persistent.putResult = new CompletableFuture<>();
        get1s(supplier.start());
        get1s(persistent.putCallFuture);
        assertThat(supplier.get(), equalTo(OLD_RESOURCE));
        persistent.putResult.complete(null);
        waitForUpdateAttempt();
        assertThat(supplier.get(), equalTo(NEW_RESOURCE));
    }

    @Test
    void ignoreResourcePersistFailure() {
        DummyCache<ResourceWithMeta> persistent = new DummyCache<>(Map.of(RESOURCE_NAME, OLD_RESOURCE));
        SandboxResourceSupplier supplier = defaultSupplier(persistent);
        setResource(NEW_RESOURCE);
        get1s(supplier.start());
        get1s(persistent.putCallFuture);
        persistent.putResult = new CompletableFuture<>();
        persistent.putResult.completeExceptionally(new RuntimeException("error"));
        waitForUpdateAttempt();
        assertThat(supplier.get(), equalTo(NEW_RESOURCE));
    }

    @Test
    void persistOnInitialGet() {
        persistFromGetterTestTemplate(Optional.empty());
    }

    @Test
    void persistOnUpdate() {
        persistFromGetterTestTemplate(Optional.of(OLD_RESOURCE));
    }

    @Test
    void notPersistIfUnchanged() {
        setResource(OLD_RESOURCE);
        DummyCache<ResourceWithMeta> persistent = new DummyCache<>(Map.of(RESOURCE_NAME, OLD_RESOURCE));
        SandboxResourceSupplier supplier = defaultSupplier(persistent);
        get1s(supplier.start());
        waitForUpdateAttempt();
        assertThat("Persist should not be called on unchanged", persistent.putCallsCount == 0);
    }

    @Test
    void zeroDelayOnStartWithRestored() {
        DummyCache<ResourceWithMeta> persistent = new DummyCache<>(Map.of(RESOURCE_NAME, OLD_RESOURCE));
        SandboxResourceSupplier supplier = defaultSupplier(persistent);
        supplier.start();
        assertThat(supplier.timeSinceLastUpdate(), equalTo(Duration.ZERO));
    }

    private void persistFromGetterTestTemplate(Optional<ResourceWithMeta> persistedValue) {
        setResource(NEW_RESOURCE);
        DummyCache<ResourceWithMeta> persistent = persistedValue.map(resourceWithMeta -> new DummyCache<>(Map.of(RESOURCE_NAME, resourceWithMeta))).orElseGet(DummyCache::new);
        SandboxResourceSupplier supplier = defaultSupplier(persistent);
        supplier.start();
        get1s(persistent.putCallFuture);
        assertThat(persistent.get(RESOURCE_NAME), equalTo(Optional.of(NEW_RESOURCE)));
    }

    private SandboxResourceSupplier defaultSupplier(Cache<ResourceWithMeta> persistent) {
        return new SandboxResourceSupplier(executor, releaseGetter, RESOURCE_TYPE, true,
                RETRY_INTERVAL, RETRY_INTERVAL, clock, RESOURCE_NAME, persistent, this::notifyOnUpdateAttempt);
    }

    private void setResource(ResourceWithMeta resource) {
        updateFuture = new CompletableFuture<>();
        releaseGetter.setResource(resource);
    }

    private void setError(Exception error) {
        updateFuture = new CompletableFuture<>();
        releaseGetter.setError(error);
    }

    private void waitForUpdateAttempt() {
        get1s(updateFuture);
    }

    private void notifyOnUpdateAttempt() {
        updateFuture.complete(null);
    }
}
