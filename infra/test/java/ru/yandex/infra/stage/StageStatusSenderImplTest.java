package ru.yandex.infra.stage;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.metrics.MapGaugeRegistry;
import ru.yandex.infra.controller.testutil.FutureUtils;
import ru.yandex.infra.controller.yp.DummyYpObjectRepository;
import ru.yandex.infra.stage.concurrent.SerialExecutor;
import ru.yandex.infra.stage.util.AdaptiveRateLimiter;
import ru.yandex.infra.stage.util.AdaptiveRateLimiterImpl;
import ru.yandex.infra.stage.util.SettableClock;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;
import ru.yandex.yp.model.YpError;
import ru.yandex.yp.model.YpErrorCodes;
import ru.yandex.yp.model.YpException;

import static com.spotify.hamcrest.future.FutureMatchers.futureWillCompleteWithValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static ru.yandex.infra.controller.testutil.FutureUtils.get1s;

class StageStatusSenderImplTest {
    private static final String STAGE_ID = "stageId";

    private DummyYpObjectRepository<StageMeta, TStageSpec, TStageStatus> ypRepository;
    private SerialExecutor executor;
    private StageStatusSenderImpl statusSender;
    private MapGaugeRegistry gaugeRegistry;

    @BeforeEach
    void before() {
        ypRepository = new DummyYpObjectRepository<>();
        executor = new SerialExecutor(getClass().getSimpleName());
        gaugeRegistry = new MapGaugeRegistry();
        statusSender = new StageStatusSenderImpl(ypRepository, executor, gaugeRegistry, TestData.CONVERTER,
                AdaptiveRateLimiter.EMPTY, Duration.ofMillis(1), Duration.ofSeconds(1));
    }

    @AfterEach
    void after() {
        executor.shutdown();
    }

    @Test
    void cancelScheduledStatusUpdateTest() {
        statusSender.save(STAGE_ID, TestData.STAGE_STATUS);
        statusSender.cancelScheduledStatusUpdate(STAGE_ID);
        assertThat(statusSender.getInProgressFutures().size(), equalTo(1));
        var saveStatusCompletedFuture = statusSender.getInProgressFutures().get(STAGE_ID);
        ypRepository.saveStatusResponse.completeExceptionally(new RuntimeException());

        get1s(saveStatusCompletedFuture);
        assertThat(statusSender.getInProgressFutures().size(), equalTo(0));
        assertFailedMetric(0);
    }

    @Test
    void removeFromFailedOnCanceling() throws InterruptedException {
        assertFailedMetric(0);
        ypRepository.saveStatusResponse.completeExceptionally(new RuntimeException());

        statusSender.save(STAGE_ID, TestData.STAGE_STATUS);
        while (0 == (int) gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_FAILED_SEND_STATUS_COUNT)) {
            Thread.sleep(10);
        }

        assertFailedMetric(1);
        var saveStatusCompletedFuture = statusSender.getInProgressFutures().get(STAGE_ID);

        statusSender.cancelScheduledStatusUpdate(STAGE_ID);

        get1s(saveStatusCompletedFuture);
        assertThat(statusSender.getInProgressFutures().size(), equalTo(0));
        assertFailedMetric(0);
    }

    @Test
    void requestsCountMetricsTest() {
        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_SEND_STATUS_REQUESTS_TOTAL), equalTo(0L));

        statusSender.save(STAGE_ID, TestData.STAGE_STATUS);
        doReceive();
        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_SEND_STATUS_REQUESTS_TOTAL), equalTo(1L));

        statusSender.save(STAGE_ID, TestData.STAGE_STATUS);
        doReceive();
        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_SEND_STATUS_REQUESTS_TOTAL), equalTo(2L));

        sendAfterSeveralFailures(1);
        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_SEND_STATUS_REQUESTS_TOTAL), equalTo(4L));
        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_SEND_STATUS_REQUESTS_FAILED), equalTo(1L));

        statusSender.save(STAGE_ID, TestData.STAGE_STATUS);
        doReceive();
        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_SEND_STATUS_REQUESTS_TOTAL), equalTo(5L));
    }

    @Test
    void statusUpdatedBeforeRequestCompleted() {
        statusSender.save(STAGE_ID, TestData.STAGE_STATUS);

        assertThat(ypRepository.lastSavedStatuses.get(STAGE_ID).getRevision(),
                equalTo(TestData.STAGE_STATUS.getRevision()));

        int newRevision = 555;
        statusSender.save(STAGE_ID, TestData.STAGE_STATUS.withRevision(newRevision));

        doReceive();
        assertThat(ypRepository.lastSavedStatuses.get(STAGE_ID).getRevision(), equalTo(newRevision));
        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_SEND_STATUS_REQUESTS_TOTAL), equalTo(2L));
    }

    @Test
    void shouldAlwaysSaveTheLatestStatus() {
        for (int i = 0; i < 50; i++) {
            statusSender.save(STAGE_ID, TestData.STAGE_STATUS);
            switch (i % 3) {
                case 0:
                    ypRepository.saveStatusResponse.complete(true);
                    ypRepository.saveStatusResponse = new CompletableFuture<>();
                    break;
                case 1:
                    ypRepository.saveStatusResponse.completeExceptionally(new RuntimeException());
                    ypRepository.saveStatusResponse = new CompletableFuture<>();
                    break;
                case 2:
                    statusSender.cancelScheduledStatusUpdate(STAGE_ID);
            }
        }

        int newRevision = 555;
        statusSender.save(STAGE_ID, TestData.STAGE_STATUS.withRevision(newRevision));
        doReceive();

        assertThat(ypRepository.lastSavedStatuses.get(STAGE_ID).getRevision(), equalTo(newRevision));
    }

    @Test
    void notResendStatus() {
        statusSender.save(STAGE_ID, TestData.STAGE_STATUS);
        doReceive();
        assertThat(statusSender.getInProgressFutures(), anEmptyMap());
        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_SEND_STATUS_REQUESTS_TOTAL), equalTo(1L));
    }

    @Test
    void retryOnFailureTest() {
        long requiredFailures = 3;
        sendAfterSeveralFailures(requiredFailures);

        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_SEND_STATUS_REQUESTS_TOTAL), equalTo(requiredFailures+1));
        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_SEND_STATUS_REQUESTS_FAILED), equalTo(requiredFailures));
        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_FAILED_SEND_STATUS_COUNT), equalTo(0));
    }

    @Test
    void rateLimiterAfterThrottlingException() throws InterruptedException {
        SettableClock CLOCK = new SettableClock();
        AdaptiveRateLimiterImpl limiter = AdaptiveRateLimiterImplTest.getLimiter(ImmutableMap.of("default_rps", "2",
                                                                            "min_rps","2",
                                                                            "max_rps", "2"), CLOCK);

        StageStatusSenderImpl statusSenderWithLimiter = new StageStatusSenderImpl(ypRepository,
                                                                executor,
                                                                gaugeRegistry,
                                                                TestData.CONVERTER,
                                                                limiter,
                                                                Duration.ofMillis(1),
                                                                Duration.ofSeconds(1));

        for (int i = 0; i < 10; i++) {
            statusSenderWithLimiter.save(STAGE_ID + i, TestData.STAGE_STATUS);
        }

        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_SEND_STATUS_REQUESTS_TOTAL), equalTo(10L));
        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_SEND_STATUS_RPS_LIMIT), equalTo(Double.NaN));

        var future = ypRepository.saveStatusResponse;
        ypRepository.saveStatusResponse = new CompletableFuture<>();

        future.completeExceptionally(new YpException("limits...", "", new YpError(YpErrorCodes.REQUEST_THROTTLED, null, null, null), null));
        waitForFirstFailure();

        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_SEND_STATUS_RPS_LIMIT), equalTo(2.0));

        ypRepository.saveStatusResponse.complete(true);

        //Expecting only 2 out 10 requests should be completed according to rate limit = 2 rps
        while(8 != statusSenderWithLimiter.getInProgressFutures().size()) {
            Thread.sleep(1);
        }

        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_SEND_STATUS_REQUESTS_TOTAL), equalTo(12L));

        //another 2 requests should complete after switching to next rate limiter interval
        CLOCK.incrementSecond();
        while(6 != statusSenderWithLimiter.getInProgressFutures().size()) {
            Thread.sleep(1);
        }
        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_SEND_STATUS_REQUESTS_TOTAL), equalTo(14L));

        Thread.sleep(200);
        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_SEND_STATUS_REQUESTS_TOTAL), equalTo(14L));
        assertThat(statusSenderWithLimiter.getInProgressFutures().size(), equalTo(6));
    }

    @Test
    void rateLimiterIgnoresNonThrottlingException() {
        SettableClock CLOCK = new SettableClock();
        AdaptiveRateLimiterImpl limiter = AdaptiveRateLimiterImplTest.getLimiter(ImmutableMap.of("default_rps", "2",
                "min_rps","2",
                "max_rps", "2"), CLOCK);

        statusSender = new StageStatusSenderImpl(ypRepository,
                executor,
                gaugeRegistry,
                TestData.CONVERTER,
                limiter,
                Duration.ofSeconds(1),
                Duration.ofMinutes(1));

        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_SEND_STATUS_RPS_LIMIT), equalTo(Double.NaN));


        ypRepository.saveStatusResponse.completeExceptionally(new YpException("limits...", "", new YpError(YpErrorCodes.AUTHENTICATION_ERROR, null, null, null), null));
        statusSender.save(STAGE_ID, TestData.STAGE_STATUS);
        waitForFirstFailure();

        assertThat((long)gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_SEND_STATUS_REQUESTS_TOTAL), greaterThanOrEqualTo(1L));
        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_SEND_STATUS_RPS_LIMIT), equalTo(Double.NaN));
    }

    private void doReceive() {
        var future = statusSender.getInProgressFutures().get(STAGE_ID);
        ypRepository.saveStatusResponse.complete(true);
        assertThat(future, futureWillCompleteWithValue());
        ypRepository.saveStatusResponse = new CompletableFuture<>();
    }

    private void waitForFirstFailure() {
        while (0 == (int) gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_FAILED_SEND_STATUS_COUNT)) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignore) {}
        }
    }

    private void assertFailedMetric(int expectedValue) {
        assertThat(gaugeRegistry.getGaugeValue(StageStatusSenderImpl.METRIC_FAILED_SEND_STATUS_COUNT), equalTo(expectedValue));
    }

    private void sendAfterSeveralFailures(long requiredFailures) {
        AtomicInteger calls = new AtomicInteger();
        ypRepository.saveStatusResponseSupplier = () -> {
            if (calls.incrementAndGet() <= requiredFailures) {
                return CompletableFuture.failedFuture(new RuntimeException("yp save status error"));
            }
            return CompletableFuture.completedFuture(true);
        };

        statusSender.save(STAGE_ID, TestData.STAGE_STATUS);
        statusSender.getInProgressFutures().values().forEach(FutureUtils::get5s);
    }
}
