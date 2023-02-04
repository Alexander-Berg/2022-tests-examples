package ru.yandex.qe.dispenser.ws.logic;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiQuotaState;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.domain.Quota;
import ru.yandex.qe.dispenser.domain.QuotaView;
import ru.yandex.qe.dispenser.domain.dao.DiJdbcTemplate;
import ru.yandex.qe.dispenser.domain.dao.quota.QuotaDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class QuotaSyncHierarchyTest extends BusinessLogicTestBase {

    @Autowired(required = false)
    private DiJdbcTemplate jdbcTemplate;

    @Autowired
    private QuotaDao quotaDao;

    @Test
    public void notSyncedQuotaCanBeUpdated() {
        Assumptions.assumeFalse(jdbcTemplate == null, "Only for sql dao");

        final AtomicReference<String> query = new AtomicReference<>();

        final BiConsumer<String, Object> listener = (sql, params) -> {
            if (sql.startsWith("SELECT * FROM quota ")) {
                if (!query.compareAndSet(null, sql)) {
                    fail("more than one quota request");
                }
            }
        };
        jdbcTemplate.addQueryListener(listener);
        hierarchy.update();
        jdbcTemplate.removeQueryListener(listener);

        final Set<Long> loadedQuotaIds = jdbcTemplate.queryForSet(query.get(), (r, e) -> r.getLong("id"));

        assertFalse(loadedQuotaIds.isEmpty());

        final Set<Quota> allQuotas = quotaDao.getAll();
        assertTrue(allQuotas.size() > loadedQuotaIds.size());

        assertTrue(allQuotas.stream()
                .filter(q -> loadedQuotaIds.contains(q.getId()))
                .allMatch(q -> q.getOwnActual() > 0 || q.getOwnMax() > 0 || q.getMax() > 0));
    }

    @Test
    public void actualValuesShouldBeCalculatedOncePerCacheUpdateForSqlHierarhcy() {
        Assumptions.assumeFalse(jdbcTemplate == null, "Only for sql dao");

        dispenser()
                .service(NIRVANA)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject(SEARCH)
                        .withActual(DiAmount.of(56, DiUnit.COUNT))
                        .build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));

        updateHierarchy();

        final Set<QuotaView> allQuotas = Hierarchy.get().getQuotaCache().getAll();

        final boolean hasAggregated = allQuotas.stream()
                .anyMatch(q -> q.getTotalActual() > 0);

        assertTrue(hasAggregated, "Reader should have aggregated value");

        final long rootYtCpuActual = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .forResource(YT_CPU)
                .ofProject(YANDEX)
                .perform().getFirst().getActual(DiUnit.COUNT);


        final QuotaView cachedQuota = Hierarchy.get().getQuotaCache().getAll()
                .stream()
                .filter(q -> q.getProject().getPublicKey().equals(INFRA)
                        && q.getResource().getPublicKey().equals(YT_CPU))
                .findFirst().get();

        dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .forResource(YT_CPU)
                .ofProject(YANDEX)
                .perform().getFirst().getActual(DiUnit.COUNT);

        final QuotaView afterChangeCachedQuota = Hierarchy.get().getQuotaCache().getAll()
                .stream()
                .filter(q -> q.getProject().getPublicKey().equals(INFRA)
                        && q.getResource().getPublicKey().equals(YT_CPU))
                .findFirst().get();

        assertSame(cachedQuota, afterChangeCachedQuota);
        assertEquals(cachedQuota.getOwnActual(), afterChangeCachedQuota.getOwnActual());

        dispenser()
                .service(NIRVANA)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject(INFRA)
                        .withActual(DiAmount.of(100, DiUnit.COUNT))
                        .build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));

        updateHierarchy();

        final QuotaView afterResetCachedQuota = Hierarchy.get().getQuotaCache().getAll()
                .stream()
                .filter(q -> q.getProject().getPublicKey().equals(INFRA)
                        && q.getResource().getPublicKey().equals(YT_CPU))
                .findFirst().get();

        assertNotSame(cachedQuota, afterResetCachedQuota);
        assertEquals(cachedQuota.getOwnActual() + 100_000, afterResetCachedQuota.getOwnActual());
    }
}
