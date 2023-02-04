package ru.yandex.intranet.d.dao.sync;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.sync.Errors;
import ru.yandex.intranet.d.model.sync.ProvidersSyncErrorsModel;

import static ru.yandex.intranet.d.dao.Tenants.DEFAULT_TENANT_ID;

/**
 * ProvidersSyncErrorsDaoTest.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @see ProvidersSyncErrorsDao
 * @since 05-04-2021
 */
@SuppressWarnings("ConstantConditions")
@IntegrationTest
class ProvidersSyncErrorsDaoTest {
    @Autowired
    private ProvidersSyncErrorsDao dao;
    @Autowired
    private YdbTableClient ydbTableClient;

    @Test
    public void testUpsertAndGetById() {
        ProvidersSyncErrorsModel modelToWrite = new ProvidersSyncErrorsModel.Builder()
                .setTenantId(DEFAULT_TENANT_ID)
                .setProviderId(TestProviders.YP_ID)
                .setAccountSpaceId("abcd")
                .setSyncId("456")
                .setErrorId("789")
                .setSyncStart(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .setRequestTimestamp(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .setRequest("{\"q\":\"q?\"}")
                .setErrors(new Errors("Error!", Map.of("id", "111"), "{\"re\":\"q!\"}"))
                .build();

        ydbTableClient.usingSessionMonoRetryable(session ->
                dao.upsertOneRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        modelToWrite
                )
        ).block();

        Optional<ProvidersSyncErrorsModel> modelToRead = ydbTableClient.usingSessionMonoRetryable(session ->
                dao.getById(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        modelToWrite.getIdentity(), DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(modelToRead);
        Assertions.assertTrue(modelToRead.isPresent());
        Assertions.assertEquals(modelToWrite, modelToRead.get());
    }

    @Test
    void clearOldErrors() {
        ProvidersSyncErrorsModel oldError = new ProvidersSyncErrorsModel.Builder()
                .setTenantId(DEFAULT_TENANT_ID)
                .setProviderId(TestProviders.YP_ID)
                .setAccountSpaceId("abcd")
                .setSyncId("1")
                .setErrorId("789")
                .setSyncStart(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .setRequestTimestamp(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .setRequest("{\"q\":\"q?\"}")
                .setErrors(new Errors("Error!", Map.of("id", "111"), "{\"re\":\"q!\"}"))
                .build();
        ProvidersSyncErrorsModel newError = new ProvidersSyncErrorsModel.Builder()
                .setTenantId(DEFAULT_TENANT_ID)
                .setProviderId(TestProviders.YP_ID)
                .setAccountSpaceId("abcd")
                .setSyncId("2")
                .setErrorId("789")
                .setSyncStart(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .setRequestTimestamp(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .setRequest("{\"q\":\"q?\"}")
                .setErrors(new Errors("Error!", Map.of("id", "111"), "{\"re\":\"q!\"}"))
                .build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                dao.upsertAllRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        List.of(oldError, newError)
                )
        ).block();

        List<ProvidersSyncErrorsModel> errors = ydbTableClient.usingSessionMonoRetryable(session ->
                dao.getAllByProvider(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        DEFAULT_TENANT_ID,
                        TestProviders.YP_ID
                )
        ).block().get();
        Assertions.assertTrue(errors.contains(oldError));
        Assertions.assertTrue(errors.contains(newError));

        ydbTableClient.usingSessionMonoRetryable(session ->
                dao.clearOldErrorsRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        newError.getTenantId(),
                        newError.getProviderId(),
                        newError.getSyncId()
                )
        ).block();

        errors = ydbTableClient.usingSessionMonoRetryable(session ->
                dao.getAllByProvider(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        DEFAULT_TENANT_ID,
                        TestProviders.YP_ID
                )
        ).block().get();
        Assertions.assertFalse(errors.contains(oldError));
        Assertions.assertTrue(errors.contains(newError));
    }
}
