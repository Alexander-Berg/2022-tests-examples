package ru.yandex.intranet.d.dao.transfers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.WithTenant;
import ru.yandex.intranet.d.model.transfers.TransferRequestByServiceModel;
import ru.yandex.intranet.d.model.transfers.TransferRequestStatus;

/**
 * Transfer request by service DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class TransferRequestByServiceDaoTest {

    @Autowired
    private TransferRequestByServiceDao dao;
    @Autowired
    private YdbTableClient ydbTableClient;

    @Test
    public void upsertAndGetById() {
        TransferRequestByServiceModel model = TransferRequestByServiceModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .serviceId(1L)
                .status(TransferRequestStatus.PENDING)
                .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .transferRequestId(UUID.randomUUID().toString())
                .build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertOneRetryable(txSession, model)))
                .block();
        Optional<TransferRequestByServiceModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getById(txSession, model.getIdentity(), model.getTenantId())))
                .block();
        Assertions.assertEquals(Optional.of(model), result);
    }

    @Test
    public void upsertAndGetByIds() {
        TransferRequestByServiceModel modelOne = TransferRequestByServiceModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .serviceId(1L)
                .status(TransferRequestStatus.PENDING)
                .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .transferRequestId(UUID.randomUUID().toString())
                .build();
        TransferRequestByServiceModel modelTwo = TransferRequestByServiceModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .serviceId(1L)
                .status(TransferRequestStatus.PENDING)
                .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .transferRequestId(UUID.randomUUID().toString())
                .build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertAllRetryable(txSession, List.of(modelOne, modelTwo))))
                .block();
        List<TransferRequestByServiceModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getByIds(txSession, List.of(modelOne.getIdentity(), modelTwo.getIdentity()),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Set.of(modelOne, modelTwo), new HashSet<>(result));
    }

    @Test
    public void deleteOne() {
        TransferRequestByServiceModel model = TransferRequestByServiceModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .serviceId(1L)
                .status(TransferRequestStatus.PENDING)
                .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .transferRequestId(UUID.randomUUID().toString())
                .build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertOneRetryable(txSession, model)))
                .block();
        Optional<TransferRequestByServiceModel> upsertResult = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getById(txSession, model.getIdentity(), model.getTenantId())))
                .block();
        Assertions.assertEquals(Optional.of(model), upsertResult);
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.deleteOneRetryable(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                model.getIdentity()))))
                .block();
        Optional<TransferRequestByServiceModel> deleteResult = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getById(txSession, model.getIdentity(), model.getTenantId())))
                .block();
        Assertions.assertEquals(Optional.empty(), deleteResult);
    }

    @Test
    public void deleteMany() {
        TransferRequestByServiceModel modelOne = TransferRequestByServiceModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .serviceId(1L)
                .status(TransferRequestStatus.PENDING)
                .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .transferRequestId(UUID.randomUUID().toString())
                .build();
        TransferRequestByServiceModel modelTwo = TransferRequestByServiceModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .serviceId(1L)
                .status(TransferRequestStatus.PENDING)
                .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .transferRequestId(UUID.randomUUID().toString())
                .build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertAllRetryable(txSession, List.of(modelOne, modelTwo))))
                .block();
        List<TransferRequestByServiceModel> upsertResult = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getByIds(txSession, List.of(modelOne.getIdentity(), modelTwo.getIdentity()),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(upsertResult);
        Assertions.assertEquals(Set.of(modelOne, modelTwo), new HashSet<>(upsertResult));
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.deleteManyRetryable(txSession, List.of(new WithTenant<>(
                                Tenants.DEFAULT_TENANT_ID, modelOne.getIdentity()), new WithTenant<>(
                                        Tenants.DEFAULT_TENANT_ID, modelTwo.getIdentity())))))
                .block();
        List<TransferRequestByServiceModel> deleteResult = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getByIds(txSession, List.of(modelOne.getIdentity(), modelTwo.getIdentity()),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(deleteResult);
        Assertions.assertTrue(deleteResult.isEmpty());
    }

}
