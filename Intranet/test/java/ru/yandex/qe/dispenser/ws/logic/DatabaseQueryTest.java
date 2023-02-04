package ru.yandex.qe.dispenser.ws.logic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTimeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiPerformer;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.domain.dao.DiJdbcTemplate;
import ru.yandex.qe.dispenser.domain.dao.quota.SqlQuotaDao;
import ru.yandex.qe.dispenser.domain.hierarchy.CachingHierarchySupplier;
import ru.yandex.qe.dispenser.domain.lots.LotsManager;
import ru.yandex.qe.dispenser.domain.util.MathUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DatabaseQueryTest extends BusinessLogicTestBase {
    @Autowired(required = false)
    private DiJdbcTemplate jdbcTemplate;
    @Autowired(required = false)
    private LotsManager lotsManager;

    @BeforeEach
    public void init() {
        skipIfNeeded();
    }

    @Test
    public void thereShouldBeNoExtraQueriesAfterHierarchyUpdate() {
        final DiPerformer bendynaDefault = DiPerson.login("bendyna").chooses(DEFAULT);
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(randomUnitEntity(), bendynaDefault)
                .perform();

        updateHierarchy();

        final String[] tableBlackList = {
                "person", "dispenser_admin", "yandex_group", "project",
                "person_membership", "yandex_group_membership",
                "service", "service_admin", "resource", "quota_spec", "quota",
                "entity_spec", "entity_spec_resource", "entity_meta_type"
        };

        final BiConsumer<String, Object> failOnBlackListTableQuery = (sql, params) -> {
            for (final String tableName : tableBlackList) {
                Assertions.assertFalse(sql.contains("FROM " + tableName), "There is query to '" + tableName + "' table!"); // TODO: more intelligent code
            }
        };
        try {
            dispenser().quotas()
                    .changeInService(NIRVANA)
                    .createEntity(randomUnitEntity(), bendynaDefault)
                    .perform();
        } finally {
            jdbcTemplate.removeQueryListener(failOnBlackListTableQuery);
        }
    }

    /**
     * {@link CachingHierarchySupplier#setStubQuotaDao}
     * DISPENSER-481: Добавить квоты в иерархию
     */
    @Test
    public void gettingQuotasV1ShouldNotQueryQuotaTableIfHierarchyIsRelevant() throws NoSuchFieldException, IllegalAccessException {
        final BiConsumer<String, Object> failOnQuotaTableQuery = (sql, params) -> {
            Assertions.assertFalse(sql.contains("FROM quota"), "There is no query to 'quota' table!"); // TODO: more intelligent code
        };
        jdbcTemplate.addQueryListener(failOnQuotaTableQuery);
        try {
            createLocalClient()
                    .path("/v1/quotas")
                    .query("project", DEFAULT)
                    .query("service", CLUSTER_API)
                    .get(DiQuotaGetResponse.class);
        } finally {
            jdbcTemplate.removeQueryListener(failOnQuotaTableQuery);
        }

        // scroll time to future
        DateTimeUtils.setCurrentMillisOffset(10 * 60_000);

        final AtomicBoolean wasQueryAboutQuotas = new AtomicBoolean();
        final BiConsumer<String, Object> quotaQueryListener = (sql, params) -> wasQueryAboutQuotas.compareAndSet(false, sql.contains("FROM quota"));
        jdbcTemplate.addQueryListener(quotaQueryListener);
        try {
            createLocalClient()
                    .path("/v1/quotas")
                    .query("project", DEFAULT)
                    .query("service", CLUSTER_API)
                    .get(DiQuotaGetResponse.class);
        } finally {
            jdbcTemplate.removeQueryListener(quotaQueryListener);
            DateTimeUtils.setCurrentMillisOffset(0);
        }

        Assertions.assertFalse(wasQueryAboutQuotas.get(), "There is no query to 'quota' table!");
    }

    @Test
    public void lotsManagerShouldOnlySelectQuotaAfterHierarchyUpdate() {
        final QueryCounter queryCounter = new QueryCounter();
        jdbcTemplate.addQueryListener(queryCounter);
        long counter = queryCounter.getCounter(SqlQuotaDao.GET_ALL_QUOTAS_QUERY);

        lotsManager.update();
        counter++;

        assertEquals(queryCounter.getCounter(SqlQuotaDao.GET_ALL_QUOTAS_QUERY), counter);
        jdbcTemplate.removeQueryListener(queryCounter);
    }

    private void skipIfNeeded() {
        Assumptions.assumeFalse(jdbcTemplate == null, "No jdbcTemplate found");
        Assumptions.assumeTrue(isHierarchyEnabled(), "Hierarchy disabled");
        Assumptions.assumeFalse(lotsManager == null, "No lots manager sound");
    }

    private static class QueryCounter implements BiConsumer<String, Object> {

        private final Map<String, Long> counters = new ConcurrentHashMap<>();

        @Override
        public void accept(final String sql, final Object params) {
            MathUtils.increment(counters, sql, 1);
        }


        public long getCounter(@NotNull final String sql) {
            return getCounters().getOrDefault(sql, 0L);
        }

        @NotNull
        public Map<String, Long> getCounters() {
            return counters;
        }
    }
}
