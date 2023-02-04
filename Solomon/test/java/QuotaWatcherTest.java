package ru.yandex.solomon.quotas.watcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.solomon.core.db.dao.QuotasDao;
import ru.yandex.solomon.core.db.dao.memory.InMemoryQuotasDao;
import ru.yandex.solomon.quotas.watcher.pumpkin.PumpkinQuotas;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;
import ru.yandex.solomon.util.file.SimpleFileStorage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class QuotaWatcherTest {
    @Rule
    public Timeout timeout = Timeout.seconds(15);

    private ManualClock clock;
    private ScheduledExecutorService timer;

    @Before
    public void setUp() {
        clock = new ManualClock();
        timer = new ManualScheduledExecutorService(1, clock);
    }

    @Test
    public void serialize() {
        {
            var key = QuotaWatcher.Key.ofDefaults("project", "alert.count");
            String serialized = QuotaWatcher.serialize(Map.entry(key, 42L));
            assertThat(serialized, equalTo("project\t\talert.count\t42"));
            var back = QuotaWatcher.deserialize(serialized);
            assertThat(back.getKey(), equalTo(key));
            assertThat(back.getValue(), equalTo(42L));
        }

        {
            var key = QuotaWatcher.Key.of("shard", "solomon\tsolomon_production_coremon", "fileSensors");
            String serialized = QuotaWatcher.serialize(Map.entry(key, 100500L));
            assertThat(serialized, equalTo("shard\tsolomon\\tsolomon_production_coremon\tfileSensors\t100500"));
            var back = QuotaWatcher.deserialize(serialized);
            assertThat(back.getKey(), equalTo(key));
            assertThat(back.getValue(), equalTo(100500L));
        }
    }

    @Test
    public void noFile() {
        QuotasDao quotasDao = new FailingQuotasDao();
        QuotaWatcher quotaWatcher = new QuotaWatcher(null, quotasDao, "alerting", timer);

        long maxAlertCount = quotaWatcher.getLimit("project", "solomon", "alerts.count")
                .orElse(Long.MAX_VALUE);

        long pumpkinValue = PumpkinQuotas.get("alerting")
                .get(QuotaWatcher.Key.ofDefaults("project", "alerts.count"));

        assertThat(maxAlertCount, equalTo(pumpkinValue));
    }

    @Test
    public void fromDb() throws InterruptedException {
        InMemoryQuotasDao quotasDao = new InMemoryQuotasDao();
        quotasDao.createSchemaForTests().join();
        Semaphore semaphore = new Semaphore(0);
        QuotaWatcher quotaWatcher = new QuotaWatcher(null, quotasDao, "alerting", timer);
        quotaWatcher.registerOnLoad(ignore -> semaphore.release());

        clock.passedTime(80, TimeUnit.SECONDS);
        semaphore.acquire();

        {
            long maxAlertCount = quotaWatcher.getLimit("project", "solomon", "alert.count").orElse(Long.MAX_VALUE);
            assertThat(maxAlertCount, equalTo(Long.MAX_VALUE));
        }

        quotasDao.upsert("alerting", "project", null, "alert.count", 100500, "uranix", Instant.now()).join();
        clock.passedTime(80, TimeUnit.SECONDS);
        semaphore.acquire();

        {
            long maxAlertCount = quotaWatcher.getLimit("project", "solomon", "alert.count").orElse(Long.MAX_VALUE);
            assertThat(maxAlertCount, equalTo(100500L));
        }
    }

    @Test
    public void failingDb() throws InterruptedException {
        FailingQuotasDao quotasDao = new FailingQuotasDao();
        Semaphore semaphore = new Semaphore(0);
        QuotaWatcher quotaWatcher = new QuotaWatcher(null, quotasDao, "alerting", timer);
        quotaWatcher.registerOnLoad(ignore -> semaphore.release());

        clock.passedTime(80, TimeUnit.SECONDS);
        semaphore.acquire();

        {
            long maxAlertCount = quotaWatcher.getLimit("project", "solomon", "alerts.count").orElse(Long.MAX_VALUE);
            long pumpkinValue = PumpkinQuotas.get("alerting")
                    .get(QuotaWatcher.Key.ofDefaults("project", "alerts.count"));
            assertThat(maxAlertCount, equalTo(pumpkinValue));
        }
    }

    @Test
    public void missingCacheDir() throws IOException {
        var missingDir = Path.of("missingDir");
        var storage = new SimpleFileStorage(missingDir);
        Files.delete(missingDir);

        QuotasDao quotasDao = new FailingQuotasDao();
        QuotaWatcher quotaWatcher = new QuotaWatcher(storage, quotasDao, "alerting", timer);

        OptionalLong limit = quotaWatcher.getLimit("project", "solomon", "alerts.count");
        long pumpkinValue = PumpkinQuotas.get("alerting")
                .get(QuotaWatcher.Key.ofDefaults("project", "alerts.count"));
        assertThat(limit, equalTo(OptionalLong.of(pumpkinValue)));
    }

    @Test
    public void loadFromDisk() throws IOException {
        Path cacheDir = Path.of("cache");
        if (!Files.exists(cacheDir)) {
            Files.createDirectory(cacheDir);
        }
        Files.write(cacheDir.resolve(QuotaWatcher.STATE_FILE), List.of(QuotaWatcher.serialize(Map.entry(
                QuotaWatcher.Key.of("project", "solomon", "alert.count"), 42L
        ))), StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        QuotasDao quotasDao = new FailingQuotasDao();
        QuotaWatcher quotaWatcher = new QuotaWatcher(new SimpleFileStorage(cacheDir), quotasDao, "alerting", timer);

        var limit = quotaWatcher.getLimit("project", "solomon", "alert.count");
        assertThat(limit, equalTo(OptionalLong.of(42L)));
    }

}
