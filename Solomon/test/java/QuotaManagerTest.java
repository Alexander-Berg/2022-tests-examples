package ru.yandex.solomon.quotas.manager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.core.db.dao.QuotasDao;
import ru.yandex.solomon.core.db.dao.memory.InMemoryQuotasDao;
import ru.yandex.solomon.quotas.manager.fetcher.ManualAlertingQuotaFetcherStub;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class QuotaManagerTest {

    private QuotasDao dao;
    private QuotaManager manager;
    private ManualAlertingQuotaFetcherStub fetcher;

    private static final Scope ALERT_PROJECT_DEFAULTS = Scope.defaultOf("alerting", "project");
    private static final Scope SOLOMON = Scope.of("alerting", "project", "solomon");
    private static final Scope YT = Scope.of("alerting", "project", "yt");

    private Instant now;

    @Before
    public void setUp() {
        dao = new InMemoryQuotasDao();
        fetcher = new ManualAlertingQuotaFetcherStub();
        manager = new QuotaManager(dao, fetcher);
        now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        dao.createSchemaForTests();
    }

    @After
    public void tearDown() {
        dao.dropSchemaForTests();
    }

    @Test
    public void empty() {
        var result = manager.getUsageWithLimits(Scope.of("alerting", "project", "solomon")).join();

        assertThat(result, emptyIterable());
    }

    @Test
    public void singleLimit() {
        manager.updateLimit(Scope.defaultOf("alerting", "project"), "alert.count", 100, "uranix", now).join();

        {
            var result = manager.getUsageWithLimits(Scope.of("alerting", "project", "solomon")).join();
            assertThat(result, contains(new QuotaValueWithLimit("alert.count", 0, 100)));
        }

        fetcher.getAlertCounter("solomon").incrementAndGet();
        fetcher.getSubalertCounter("solomon").addAndGet(100);

        {
            var result = manager.getUsageWithLimits(Scope.of("alerting", "project", "solomon")).join();
            assertThat(result, contains(new QuotaValueWithLimit("alert.count", 1, 100)));
        }
    }

    @Test
    public void multiLimit() {
        manager.updateLimit(ALERT_PROJECT_DEFAULTS, "alert.count", 100, "uranix", now).join();
        manager.updateLimit(ALERT_PROJECT_DEFAULTS, "subalert.count", 10000, "gordiychuk", now.plusSeconds(5)).join();
        manager.updateLimit(YT, "subalert.count", 1000, "guschin", now.plusSeconds(100500)).join();

        {
            var result = manager.getDefaultLimits("alerting", "project").join();
            assertThat(result, containsInAnyOrder(
                new QuotaLimit("alert.count", 100, "uranix", now),
                new QuotaLimit("subalert.count", 10000, "gordiychuk", now.plusSeconds(5))
            ));
        }

        {
            var result = manager.getUsageWithLimits(SOLOMON).join();
            assertThat(result, containsInAnyOrder(
                new QuotaValueWithLimit("alert.count", 0, 100),
                new QuotaValueWithLimit("subalert.count", 0, 10000)
            ));
        }

        fetcher.getAlertCounter("solomon").incrementAndGet();
        fetcher.getSubalertCounter("solomon").addAndGet(100);

        {
            var result = manager.getUsageWithLimits(SOLOMON).join();
            assertThat(result, containsInAnyOrder(
                new QuotaValueWithLimit("alert.count", 1, 100),
                new QuotaValueWithLimit("subalert.count", 100, 10000)
            ));
        }

        {
            var result = manager.getUsageWithLimits(YT).join();
            assertThat(result, containsInAnyOrder(
                new QuotaValueWithLimit("alert.count", 0, 100),
                new QuotaValueWithLimit("subalert.count", 0, 1000)
            ));
        }
    }

    @Test
    public void updateLimit() {
        manager.updateLimit(ALERT_PROJECT_DEFAULTS, "alert.count", 100, "uranix", now).join();
        manager.updateLimit(ALERT_PROJECT_DEFAULTS, "subalert.count", 10000, "uranix", now).join();
        manager.updateLimit(YT, "subalert.count", 1000, "uranix", now).join();

        fetcher.getAlertCounter("yt").addAndGet(10);
        fetcher.getSubalertCounter("yt").addAndGet(10000);

        {
            var result = manager.getUsageWithLimits(YT).join();
            assertThat(result, containsInAnyOrder(
                new QuotaValueWithLimit("alert.count", 10, 100),
                new QuotaValueWithLimit("subalert.count", 10000, 1000)
            ));
        }

        manager.updateLimit(YT, "subalert.count", 50000, "babenko", now.plusSeconds(100));

        {
            var result = manager.getUsageWithLimits(YT).join();
            assertThat(result, containsInAnyOrder(
                new QuotaValueWithLimit("alert.count", 10, 100),
                new QuotaValueWithLimit("subalert.count", 10000, 50000)
            ));
        }
    }

    @Test
    public void resetLimit() {
        manager.updateLimit(ALERT_PROJECT_DEFAULTS, "alert.count", 100, "uranix", now).join();
        manager.updateLimit(ALERT_PROJECT_DEFAULTS, "subalert.count", 10000, "uranix", now).join();
        manager.updateLimit(SOLOMON, "alert.count", 200, "uranix", now).join();

        fetcher.getAlertCounter("solomon").addAndGet(10);
        fetcher.getSubalertCounter("solomon").addAndGet(500);

        {
            var result = manager.getUsageWithLimits(SOLOMON).join();
            assertThat(result, containsInAnyOrder(
                new QuotaValueWithLimit("alert.count", 10, 200),
                new QuotaValueWithLimit("subalert.count", 500, 10000)
            ));
        }

        manager.resetLimitToDefault(SOLOMON, "alert.count").join();

        {
            var result = manager.getUsageWithLimits(SOLOMON).join();
            assertThat(result, containsInAnyOrder(
                new QuotaValueWithLimit("alert.count", 10, 100),
                new QuotaValueWithLimit("subalert.count", 500, 10000)
            ));
        }
    }

    @Test
    public void forgetIndicator() {
        manager.updateLimit(ALERT_PROJECT_DEFAULTS, "alert.count", 100, "uranix", now).join();
        manager.updateLimit(ALERT_PROJECT_DEFAULTS, "subalert.count", 10000, "uranix", now).join();
        manager.updateLimit(SOLOMON, "alert.count", 200, "uranix", now).join();

        fetcher.getAlertCounter("solomon").addAndGet(10);
        fetcher.getSubalertCounter("solomon").addAndGet(500);

        {
            var result = manager.getUsageWithLimits(SOLOMON).join();
            assertThat(result, containsInAnyOrder(
                new QuotaValueWithLimit("alert.count", 10, 200),
                new QuotaValueWithLimit("subalert.count", 500, 10000)
            ));
        }

        manager.deleteIndicator("alerting", "project", "subalert.count").join();

        {
            var result = manager.getUsageWithLimits(SOLOMON).join();
            assertThat(result, containsInAnyOrder(
                new QuotaValueWithLimit("alert.count", 10, 200)
            ));
        }
    }
}
