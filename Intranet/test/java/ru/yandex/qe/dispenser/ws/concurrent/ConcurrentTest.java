package ru.yandex.qe.dispenser.ws.concurrent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiPerformer;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiResourceAmount;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.client.v1.builder.BatchQuotaChangeRequestBuilder;
import ru.yandex.qe.dispenser.domain.dao.quota.SqlQuotaDao;
import ru.yandex.qe.dispenser.domain.util.MathUtils;
import ru.yandex.qe.dispenser.ws.logic.BusinessLogicTestBase;

public final class ConcurrentTest extends BusinessLogicTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(ConcurrentTest.class);

    /**
     * {@link SqlQuotaDao#changeAll}
     * DISPENSER-496: Написать конкурентный тест на изменение квот
     */
    @Test
    public void acquireQuotaMustBeThreadSafe() throws InterruptedException {
        // InMemoryRequestManager is broken for this test
        Assumptions.assumeFalse(isStubMode(), "ConcurrentTest.acquireQuotaMustBeThreadSafe is skipped in stub configuration");

        final int concurrentThreads = 7;
        final int requests = 1000;
        final int batchSize = 4;

        final ExecutorService requestPerformers = Executors.newFixedThreadPool(concurrentThreads);

        final Map<String, AtomicLong> expectedActuals = new ConcurrentHashMap<>();

        final String[] persons = {LYADZHIN.getLogin(), WHISTLER.getLogin(), AMOSOV_F.getLogin(), "bendyna", "welvet"};
        final DiPerformer[] allPerformers = Arrays.stream(persons).flatMap(login -> {
            return dispenser().projects().get()
                    .avaliableFor(login)
                    .perform()
                    .stream()
                    .map(project -> DiPerformer.login(login).chooses(project.getKey()));
        }).toArray(DiPerformer[]::new);

        for (int i = 0; i < requests; i++) {
            requestPerformers.submit(() -> {
                final Map<String, Long> diffs = new HashMap<>();
                final BatchQuotaChangeRequestBuilder rb = dispenser().quotas().changeInService(SCRAPER);
                for (int j = 0; j < batchSize; j++) {
                    final DiPerformer performer = allPerformers[new Random().nextInt(allPerformers.length)];
                    final long diff = new Random().nextInt(1000);
                    final DiResourceAmount amount = DiResourceAmount.ofResource(DOWNLOADS)
                            .withAmount(DiAmount.of(diff, DiUnit.PERMILLE))
                            .build();

                    rb.acquireResource(amount, performer);
                    MathUtils.increment(diffs, performer.getProjectKey(), diff);
                }

                //noinspection OverlyBroadCatchBlock
                try {
                    rb.perform();
                } catch (Exception e) {
                    LOG.error("Quota acquire error!", e);
                    return;
                }
                diffs.forEach((key, diff) -> {
                    expectedActuals.computeIfAbsent(key, k -> new AtomicLong()).addAndGet(diff);
                });
            });
        }
        final ScheduledExecutorService hierarchyUpdater = Executors.newSingleThreadScheduledExecutor();
        hierarchyUpdater.scheduleAtFixedRate(this::updateHierarchy, 1, 1, TimeUnit.SECONDS);

        requestPerformers.shutdown();
        requestPerformers.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        hierarchyUpdater.shutdown();
        hierarchyUpdater.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        updateHierarchy();

        final DiQuotaGetResponse quotas = dispenser().quotas().get().inService(SCRAPER).inLeafs().perform();

        assertActualEquals(quotas, DOWNLOADS, p -> expectedActuals.getOrDefault(p, new AtomicLong()).get());
    }
}
