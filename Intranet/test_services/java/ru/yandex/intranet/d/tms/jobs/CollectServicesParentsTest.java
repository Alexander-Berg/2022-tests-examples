package ru.yandex.intranet.d.tms.jobs;

import java.util.Set;

import javax.annotation.Nullable;

import com.yandex.ydb.table.transaction.TransactionMode;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.services.ServicesDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for cron job to collect all parents for each service.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 04.11.2020
 */
@IntegrationTest
class CollectServicesParentsTest {
    @Autowired
    private CollectServicesParents collectServicesParentsTask;
    @Autowired
    private ServicesDao servicesDao;
    @Autowired
    private YdbTableClient tableClient;

    @Test
    void executeTest() {
        collectServicesParentsTask.execute();

        assertEquals(new LongOpenHashSet(Set.of()), getAllParents(1L));
        assertEquals(new LongOpenHashSet(Set.of(8L)), getAllParents(9L));
        assertEquals(new LongOpenHashSet(Set.of(8L)), getAllParents(10L));
        assertEquals(new LongOpenHashSet(Set.of(8L, 10L)), getAllParents(11L));
    }

    @Nullable
    private LongSet getAllParents(long serviceId) {
        LongSet parents;
        parents = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        servicesDao.getAllParents(ts, serviceId, Tenants.DEFAULT_TENANT_ID)
                ))
                .block();
        return parents;
    }
}
