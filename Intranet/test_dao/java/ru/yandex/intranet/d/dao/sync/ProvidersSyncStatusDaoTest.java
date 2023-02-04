package ru.yandex.intranet.d.dao.sync;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.sync.ProvidersSyncStatusModel;
import ru.yandex.intranet.d.model.sync.ProvidersSyncStatusModel.SyncStatuses;
import ru.yandex.intranet.d.model.sync.SyncStats;

import static ru.yandex.intranet.d.dao.Tenants.DEFAULT_TENANT_ID;

/**
 * ProvidersSyncStatusDaoTest.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @see ProvidersSyncStatusDao
 * @since 02-04-2021
 */
@IntegrationTest
class ProvidersSyncStatusDaoTest {
    @Autowired
    private ProvidersSyncStatusDao dao;
    @Autowired
    private YdbTableClient ydbTableClient;

    @Test
    public void testUpsertAndGetById() {
        ProvidersSyncStatusModel modelToWrite = new ProvidersSyncStatusModel.Builder()
                .setTenantId(DEFAULT_TENANT_ID)
                .setProviderId(TestProviders.YP_ID)
                .setLastSyncStart(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .setLastSyncId("123")
                .setLastSyncFinish(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .setLastSyncStatus(SyncStatuses.DONE_OK)
                .setLastSyncStats(new SyncStats(1L, 10L, Duration.ofSeconds(5)))
                .setNewSyncStart(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .setNewSyncId("456")
                .setNewSyncStatus(SyncStatuses.RUNNING)
                .build();

        ydbTableClient.usingSessionMonoRetryable(session ->
                dao.upsertOneRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        modelToWrite
                )
        ).block();

        Optional<ProvidersSyncStatusModel> modelToRead = ydbTableClient.usingSessionMonoRetryable(session ->
                dao.getById(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        modelToWrite.getProviderId(), DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(modelToRead);
        Assertions.assertTrue(modelToRead.isPresent());
        Assertions.assertEquals(modelToWrite, modelToRead.get());
    }

    @Test
    void testUpsertNewSync() {
        ProvidersSyncStatusModel modelToWrite = new ProvidersSyncStatusModel.Builder()
                .setTenantId(DEFAULT_TENANT_ID)
                .setProviderId(TestProviders.MDB_ID)
                .setNewSyncStart(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .setNewSyncId("456")
                .setNewSyncStatus(SyncStatuses.RUNNING)
                .build();

        ydbTableClient.usingSessionMonoRetryable(session ->
                dao.upsertNewSyncRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        modelToWrite.getTenantId(), // tenantId,
                        modelToWrite.getProviderId(), // providerId
                        modelToWrite.getNewSyncStart(), // newSyncStart
                        modelToWrite.getNewSyncId(), // newSyncId
                        modelToWrite.getNewSyncStatus() // newSyncStatus
                )
        ).block();

        Optional<ProvidersSyncStatusModel> modelToRead = ydbTableClient.usingSessionMonoRetryable(session ->
                dao.getById(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        modelToWrite.getProviderId(), DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(modelToRead);
        Assertions.assertTrue(modelToRead.isPresent());
        Assertions.assertEquals(modelToWrite, modelToRead.get());
    }
}
