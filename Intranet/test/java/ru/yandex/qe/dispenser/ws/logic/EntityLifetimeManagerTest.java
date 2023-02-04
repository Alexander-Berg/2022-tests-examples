package ru.yandex.qe.dispenser.ws.logic;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.domain.entity.EntityLifetimeManager;
import ru.yandex.qe.dispenser.domain.entity.EntityLifetimeManagerImpl;
import ru.yandex.qe.dispenser.solomon.SolomonHolder;
import ru.yandex.qe.dispenser.utils.DiEntityIdSupplier;
import ru.yandex.monlib.metrics.histogram.HistogramSnapshot;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.monlib.metrics.primitives.Histogram;
import ru.yandex.monlib.metrics.primitives.LazyGaugeInt64;
import ru.yandex.monlib.metrics.primitives.Rate;
import ru.yandex.monlib.metrics.registry.MetricId;
import ru.yandex.monlib.metrics.registry.MetricRegistry;


public class EntityLifetimeManagerTest extends BusinessLogicTestBase {
    private static final long TTL = TimeUnit.SECONDS.toMillis(1);

    @Autowired(required = false)
    private EntityLifetimeManagerImpl entityLifetimeManager;

    @BeforeAll
    public void configureTtl() {
        if (entityLifetimeManager != null) {
            entityLifetimeManager.removeAllTtls();
            entityLifetimeManager.setTtl(NIRVANA, YT_FILE, TTL);
        }
    }

    @AfterAll
    public void cleanTtl() {
        if (entityLifetimeManager != null) {
            entityLifetimeManager.removeAllTtls();
        }
    }

    // TODO Declarative annotation-based exclusion?
    private void skipIfNeeded() {
        Assumptions.assumeFalse(entityLifetimeManager == null, "No entityLifetimeManager found");
    }

    @Test
    public void createdEntitiesShouldBeDeletedAfterTtl() {
        skipIfNeeded();

        final Set<DiEntity> entitiesToCreate = generateNirvanaYtFiles(150).collect(Collectors.toSet());
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntities(entitiesToCreate, LYADZHIN.chooses(INFRA))
                .perform();

        sleep(TTL * 2);
        entityLifetimeManager.removeOld();

        final DiListResponse<DiEntity> entities = dispenser().getEntities().inService(NIRVANA).perform();
        Assertions.assertTrue(entities.isEmpty());
    }

    @Test
    public void canSetTtlPerExpirableEntity() {
        skipIfNeeded();

        final List<DiEntity> entitiesWithTtl = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final DiEntity e = DiEntity.withKey(entitiesWithTtl.size() + "")
                    .bySpecification(TASK)
                    .occupies(TELLURIUM_PARALLEL, DiAmount.of(1, DiUnit.COUNT))
                    .withTtl(Duration.ofMillis(TTL))
                    .build();
            entitiesWithTtl.add(e);
        }
        for (int i = 0; i < 10; i++) {
            final DiEntity e = DiEntity.withKey(entitiesWithTtl.size() + "")
                    .bySpecification(TASK)
                    .occupies(TELLURIUM_PARALLEL, DiAmount.of(1, DiUnit.COUNT))
                    .withTtl(Duration.ofMillis(100 * TTL))
                    .build();
            entitiesWithTtl.add(e);
        }
        dispenser().quotas()
                .changeInService(SCRAPER)
                .createEntities(entitiesWithTtl, LYADZHIN.chooses(INFRA))
                .perform();

        sleep(TTL * 2);
        entityLifetimeManager.removeOld();

        final DiListResponse<DiEntity> entities = dispenser().getEntities().inService(SCRAPER).perform();
        Assertions.assertEquals(10, entities.size());
    }

    @Test
    public void canProlongTtlForExpirableEntity() {
        skipIfNeeded();

        final DiEntity e = DiEntity.withKey("1")
                .bySpecification(TASK)
                .occupies(TELLURIUM_PARALLEL, DiAmount.of(1, DiUnit.COUNT))
                .withTtl(Duration.ofMillis(TTL * 5))
                .build();

        dispenser().quotas()
                .changeInService(SCRAPER)
                .createEntity(e, LYADZHIN.chooses(INFRA))
                .perform();

        sleep(TTL * 3);
        entityLifetimeManager.removeOld();

        DiListResponse<DiEntity> entities = dispenser().getEntities().inService(SCRAPER).perform();
        Assertions.assertEquals(1, entities.size());

        dispenser()
                .updateEntities()
                .inService(SCRAPER)
                .bySpecification(TASK)
                .prolong("1", Duration.ofMillis(TTL * 5))
                .perform();

        sleep(TTL * 3);
        entityLifetimeManager.removeOld();

        entities = dispenser().getEntities().inService(SCRAPER).perform();
        Assertions.assertEquals(1, entities.size());

        sleep(TTL * 5);
        entityLifetimeManager.removeOld();

        entities = dispenser().getEntities().inService(SCRAPER).perform();
        Assertions.assertTrue(entities.isEmpty());
    }

    // TODO stabilize
    @Disabled
    @Test
    public void onlyOldEntitiesShouldBeDeleted() {
        skipIfNeeded();

        final DiEntityIdSupplier idSupplier = new DiEntityIdSupplier();
        final Set<DiEntity> entitiesToBeDeleted = generateNirvanaYtFiles(10, idSupplier).collect(Collectors.toSet());
        final Set<DiEntity> entitiesToBeLeft = generateNirvanaYtFiles(10, idSupplier).collect(Collectors.toSet());

        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntities(entitiesToBeDeleted, LYADZHIN.chooses(INFRA))
                .perform();

        sleep(TTL);

        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntities(entitiesToBeLeft, LYADZHIN.chooses(INFRA))
                .perform();

        entityLifetimeManager.removeOld();

        final Set<DiEntity> returnedEntities = dispenser().getEntities()
                .inService(NIRVANA)
                .perform()
                .stream()
                .collect(Collectors.toSet());
        Assertions.assertEquals(returnedEntities, entitiesToBeLeft);
    }

    @Test
    public void solomonMetricsShouldWorkCorrectly() {
        final SolomonHolder solomonHolder = new SolomonHolder(42);
        final MetricRegistry rootRegistry = solomonHolder.getRootRegistry();
        Assertions.assertNull(rootRegistry.getMetric(new MetricId(EntityLifetimeManagerImpl.ELAPSED_TIME_SENSOR, Labels.of())));
        Assertions.assertNull(rootRegistry.getMetric(new MetricId(EntityLifetimeManagerImpl.ERROR_RATE_SENSOR, Labels.of())));
        Assertions.assertNull(rootRegistry.getMetric(new MetricId(EntityLifetimeManagerImpl.LAST_START_SENSOR, Labels.of())));

        final EntityLifetimeManager manager = new EntityLifetimeManagerImpl(solomonHolder);

        final Histogram elapsedTime = (Histogram) rootRegistry.getMetric(new MetricId(EntityLifetimeManagerImpl.ELAPSED_TIME_SENSOR, Labels.of()));
        final Rate errorRate = (Rate) rootRegistry.getMetric(new MetricId(EntityLifetimeManagerImpl.ERROR_RATE_SENSOR, Labels.of()));
        final LazyGaugeInt64 timeSinceLastStart = (LazyGaugeInt64) rootRegistry.getMetric(new MetricId(EntityLifetimeManagerImpl.LAST_START_SENSOR, Labels.of()));
        Assertions.assertThrows(Exception.class, manager::removeOld);
        Assertions.assertEquals(1, errorRate.get());
        final HistogramSnapshot snapshot = elapsedTime.snapshot();
        Assertions.assertTrue(IntStream.range(0, snapshot.count())
                .mapToLong(snapshot::value)
                .anyMatch(x -> x > 0)
        );
        Assertions.assertTrue(timeSinceLastStart.get() > 0L);

    }
}
