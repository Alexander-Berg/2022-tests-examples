package ru.yandex.solomon.alert.rule.cache;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.monlib.metrics.MetricType;
import ru.yandex.monlib.metrics.registry.MetricRegistry;
import ru.yandex.solomon.alert.rule.stubs.DcMetricsClientFindOnlyStub;
import ru.yandex.solomon.labels.LabelsFormat;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.labels.shard.ShardKey;
import ru.yandex.solomon.metabase.api.protobuf.EMetabaseStatusCode;
import ru.yandex.solomon.metrics.client.CrossDcMetricsClient;
import ru.yandex.solomon.metrics.client.FindResponse;
import ru.yandex.solomon.metrics.client.MetabaseStatus;
import ru.yandex.solomon.metrics.client.MetricsClient;
import ru.yandex.solomon.metrics.client.cache.FindCacheOptions;
import ru.yandex.solomon.metrics.client.cache.MetabaseFindCacheImpl;
import ru.yandex.solomon.model.MetricKey;
import ru.yandex.solomon.model.StockpileKey;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.stockpile.client.shard.StockpileLocalId;
import ru.yandex.stockpile.client.shard.StockpileShardId;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class MetabaseFindCacheImplTest {
    @Rule
    public Timeout timeoutRule = Timeout.builder()
        .withLookingForStuckThread(true)
        .withTimeout(15, TimeUnit.SECONDS)
        .build();

    private DcMetricsClientFindOnlyStub metricsClientSas;
    private DcMetricsClientFindOnlyStub metricsClientVla;
    private DcMetricsClientFindOnlyStub metricsClientMan;

    private MetricsClient crossDcClient;

    private MetabaseFindCacheImpl findCache;

    private ManualClock clock;

    private final int limit = 100;
    private final long softDeadline = 0;

    @Before
    public void setUp() {
        metricsClientSas = new DcMetricsClientFindOnlyStub("sas");
        metricsClientVla = new DcMetricsClientFindOnlyStub("vla");
        metricsClientMan = new DcMetricsClientFindOnlyStub("man");

        crossDcClient = new CrossDcMetricsClient(Map.of(
            "sas", metricsClientSas,
            "vla", metricsClientVla,
            "man", metricsClientMan
        ));

        clock = new ManualClock();

        var key = new ShardKey("project-test", "cluster-test", "service-test");
        metricsClientSas.addShard(key);
        metricsClientMan.addShard(key);
        metricsClientVla.addShard(key);
    }

    private long deadlineMillis() {
        return Instant.now().plusSeconds(30).toEpochMilli();
    }

    private static class ManualResponse {
        private final CompletableFuture<FindResponse> future;
        private final FindResponse response;

        private ManualResponse(FindResponse response) {
            this.response = response;
            this.future = new CompletableFuture<>();
        }

        CompletableFuture<FindResponse> promise() {
            return future;
        }

        void complete() {
            future.complete(response);
        }
    }

    private void makeCacheForClient(MetricsClient client) {
        makeCacheForClient(client, null);
    }

    private void makeCacheForClient(MetricsClient client, @Nullable Consumer<FindResponse> onComplete) {
        FindCacheOptions options = FindCacheOptions.newBuilder()
            .setMaxSize(1000)
            .setExpireTtl(5, TimeUnit.MINUTES)
            .setRefreshInterval(10, TimeUnit.MINUTES)
            .build();
        findCache = new MetabaseFindCacheImpl(client, new MetricRegistry(), options, clock, clock.asTicker(), onComplete);
    }

    @Test
    public void removeObsolete() {
        makeCacheForClient(metricsClientSas);

        ManualResponse secondResponse = new ManualResponse(
            new FindResponse(MetabaseStatus.fromCode(EMetabaseStatusCode.INTERNAL_ERROR, "bye")));
        metricsClientSas.addResponse(new FindResponse(MetabaseStatus.OK));
        metricsClientSas.addResponse(secondResponse.promise());

        FindResponse response = findCache.find(Selectors.of(), limit, softDeadline, deadlineMillis()).join();
        assertTrue(response.isOk());

        clock.passedTime(1, TimeUnit.MINUTES);

        FindResponse responseAgain = findCache.find(Selectors.of(), limit, softDeadline, deadlineMillis()).join();
        assertTrue(responseAgain.isOk());

        clock.passedTime(5, TimeUnit.MINUTES);

        var key = new MetabaseFindCacheImpl.FindRequestKey("sas", Selectors.of(), limit);
        assertNull(findCache.getCache().getIfPresent(key));
    }

    @Test
    public void manyRequestsJoin() {
        makeCacheForClient(metricsClientSas);

        ManualResponse response = new ManualResponse(new FindResponse(MetabaseStatus.OK));
        ManualResponse secondResponse = new ManualResponse(
            new FindResponse(MetabaseStatus.fromCode(EMetabaseStatusCode.INTERNAL_ERROR, "bye")));
        metricsClientSas.addResponse(response.promise());
        metricsClientSas.addResponse(secondResponse.promise());

        List<CompletableFuture<FindResponse>> responses = IntStream.range(0, 10).parallel()
            .mapToObj(ignore -> findCache.find(Selectors.of(), limit, softDeadline, deadlineMillis()))
            .collect(Collectors.toList());

        assertThat(findCache.activeCount(), equalTo(1L));
        response.complete();

        for (var resp : responses) {
            assertTrue(resp.join().isOk());
        }
    }

    @Test
    public void cachedFindInBackground() throws InterruptedException {
        Semaphore sync = new Semaphore(0);
        Consumer<FindResponse> onComplete = ignore -> sync.release();
        makeCacheForClient(metricsClientSas, onComplete);

        ManualResponse first = new ManualResponse(new FindResponse(MetabaseStatus.fromCode(EMetabaseStatusCode.OK, "first")));
        ManualResponse second = new ManualResponse(new FindResponse(MetabaseStatus.fromCode(EMetabaseStatusCode.OK, "second")));

        metricsClientSas.addResponse(first.promise());
        metricsClientSas.addResponse(second.promise());

        first.complete();
        findCache.find(Selectors.of(), limit, softDeadline, deadlineMillis()).join();
        sync.acquire();

        for (int i = 0; i < 20; i++) {
            findCache.find(Selectors.of(), limit, softDeadline, deadlineMillis()).join();
            clock.passedTime(1, TimeUnit.MINUTES);
        }

        FindResponse response = findCache.find(Selectors.of(), limit, softDeadline, deadlineMillis()).join();
        assertThat(response.getStatus().getDescription(), equalTo("first"));
        assertThat(findCache.activeCount(), equalTo(1L));

        second.complete();
        sync.acquire();

        FindResponse responseNext = findCache.find(Selectors.of(), limit, softDeadline, deadlineMillis()).join();
        assertThat(responseNext.getStatus().getDescription(), equalTo("second"));
    }


    @Test
    public void immediateFailSlowOK() {
        makeCacheForClient(crossDcClient);

        ManualResponse fastSas = new ManualResponse(new FindResponse(MetabaseStatus.fromCode(EMetabaseStatusCode.NODE_UNAVAILABLE, "CircuitBreaker")));
        ManualResponse slowVla = new ManualResponse(new FindResponse(MetabaseStatus.OK));

        fastSas.complete();

        metricsClientSas.addResponse(fastSas.promise());
        metricsClientVla.addResponse(slowVla.promise());

        CompletableFuture<FindResponse> responseFuture = findCache.find(Selectors.of(), limit, softDeadline, deadlineMillis());

        clock.passedTime(1, TimeUnit.SECONDS);
        slowVla.complete();

        FindResponse response = responseFuture.join();

        assertTrue(response.isOk());
        assertFalse(response.isAllDestSuccess());
    }

    @Test
    public void crossDcTest() {
        makeCacheForClient(crossDcClient);

        ManualResponse fastSas = new ManualResponse(new FindResponse(MetabaseStatus.OK));
        ManualResponse slowVla = new ManualResponse(new FindResponse(MetabaseStatus.OK));
        ManualResponse downMan = new ManualResponse(new FindResponse(MetabaseStatus.OK));

        metricsClientSas.addResponse(fastSas.promise());
        metricsClientVla.addResponse(slowVla.promise());
        metricsClientMan.addResponse(downMan.promise());

        CompletableFuture<FindResponse> responseFuture = findCache.find(Selectors.of(), limit, softDeadline, deadlineMillis());

        fastSas.complete();
        clock.passedTime(10, TimeUnit.SECONDS);
        slowVla.complete();
        clock.passedTime(20, TimeUnit.SECONDS);
        downMan.complete();

        FindResponse response = responseFuture.join();

        assertTrue(response.isOk());
        assertTrue(response.isAllDestSuccess());
    }

    @Test
    public void crossDcTestAllFail() {
        makeCacheForClient(crossDcClient);

        ManualResponse fastSas = new ManualResponse(new FindResponse(MetabaseStatus.fromCode(EMetabaseStatusCode.NODE_UNAVAILABLE, "")));
        ManualResponse slowVla = new ManualResponse(new FindResponse(MetabaseStatus.fromCode(EMetabaseStatusCode.NODE_UNAVAILABLE, "")));
        ManualResponse downMan = new ManualResponse(new FindResponse(MetabaseStatus.fromCode(EMetabaseStatusCode.NODE_UNAVAILABLE, "")));

        metricsClientSas.addResponse(fastSas.promise());
        metricsClientVla.addResponse(slowVla.promise());
        metricsClientMan.addResponse(downMan.promise());

        CompletableFuture<FindResponse> responseFuture = findCache.find(Selectors.of(), limit, softDeadline, deadlineMillis());

        fastSas.complete();
        clock.passedTime(10, TimeUnit.SECONDS);
        slowVla.complete();
        clock.passedTime(20, TimeUnit.SECONDS);
        downMan.complete();

        FindResponse response = responseFuture.join();

        assertFalse(response.isOk());
        assertFalse(response.isAllDestSuccess());
    }

    @Test
    public void dcIsDown() throws InterruptedException {
        Semaphore sync = new Semaphore(0);
        Consumer<FindResponse> onComplete = response -> sync.release();
        makeCacheForClient(crossDcClient, onComplete);

        ManualResponse fastSas = new ManualResponse(new FindResponse(MetabaseStatus.OK));
        ManualResponse slowVla = new ManualResponse(new FindResponse(MetabaseStatus.OK));
        ManualResponse downMan = new ManualResponse(new FindResponse(
            MetabaseStatus.fromCode(EMetabaseStatusCode.DEADLINE_EXCEEDED, "Deadline exceeded")
        ));

        ManualResponse nextSas = new ManualResponse(new FindResponse(MetabaseStatus.fromCode(EMetabaseStatusCode.RESOURCE_EXHAUSTED, "should be not requsted")));
        ManualResponse nextVla = new ManualResponse(new FindResponse(MetabaseStatus.fromCode(EMetabaseStatusCode.RESOURCE_EXHAUSTED, "should be not requsted")));
        List<ManualResponse> unavailableMan = IntStream.range(0, 3).mapToObj(
            i -> new ManualResponse(new FindResponse(
                MetabaseStatus.fromCode(EMetabaseStatusCode.NODE_UNAVAILABLE, "node is down " + i)
            ))
        ).collect(Collectors.toList());
        ManualResponse okMan = new ManualResponse(new FindResponse(MetabaseStatus.OK));

        metricsClientSas.addResponse(fastSas.promise());
        metricsClientSas.addResponse(nextSas.promise());

        metricsClientVla.addResponse(slowVla.promise());
        metricsClientVla.addResponse(nextVla.promise());

        metricsClientMan.addResponse(downMan.promise());

        CompletableFuture<FindResponse> responseFuture = findCache.find(Selectors.of(), limit, softDeadline, deadlineMillis());

        fastSas.complete();
        clock.passedTime(10, TimeUnit.SECONDS);
        slowVla.complete();
        clock.passedTime(20, TimeUnit.SECONDS);
        downMan.complete();

        sync.acquire(3);

        FindResponse response = responseFuture.join();

        assertTrue(response.isOk());
        assertFalse(response.isAllDestSuccess());

        nextSas.complete();
        nextVla.complete();

        for (var resp : unavailableMan) {
            metricsClientMan.addResponse(resp.promise());
            resp.complete();
        }

        metricsClientMan.addResponse(okMan.promise());
        okMan.complete();

        for (int i = 0; i < 4; i++) {
            findCache.find(Selectors.of(), limit, softDeadline, deadlineMillis());
            clock.passedTime(1, TimeUnit.MINUTES);
            sync.acquire();
        }

        FindResponse finalResponse = findCache.find(Selectors.of(), limit, softDeadline, deadlineMillis()).join();
        assertTrue(finalResponse.isOk());
        assertTrue(finalResponse.isAllDestSuccess());

        assertThat(metricsClientSas.requestCount(), equalTo(1));
        assertThat(metricsClientVla.requestCount(), equalTo(1));
        assertThat(metricsClientMan.requestCount(), equalTo(5));
    }

    private MetricKey metric(String dest, String labels) {
        var stockpileKey = new StockpileKey(dest, StockpileShardId.random(42), StockpileLocalId.random());
        return new MetricKey(MetricType.DGAUGE, LabelsFormat.parse(labels), stockpileKey);
    }
}
