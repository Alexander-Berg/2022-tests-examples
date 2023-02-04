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
import ru.yandex.intranet.d.model.transfers.TransferRequestByFolderModel;
import ru.yandex.intranet.d.model.transfers.TransferRequestStatus;

/**
 * Transfer request by folder DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class TransferRequestByFolderDaoTest {

    @Autowired
    private TransferRequestByFolderDao dao;
    @Autowired
    private YdbTableClient ydbTableClient;

    @Test
    public void upsertAndGetById() {
        TransferRequestByFolderModel model = TransferRequestByFolderModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .folderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                .status(TransferRequestStatus.PENDING)
                .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .transferRequestId(UUID.randomUUID().toString())
                .build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertOneRetryable(txSession, model)))
                .block();
        Optional<TransferRequestByFolderModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getById(txSession, model.getIdentity(), model.getTenantId())))
                .block();
        Assertions.assertEquals(Optional.of(model), result);
    }

    @Test
    public void upsertAndGetByIds() {
        TransferRequestByFolderModel modelOne = TransferRequestByFolderModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .folderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                .status(TransferRequestStatus.PENDING)
                .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .transferRequestId(UUID.randomUUID().toString())
                .build();
        TransferRequestByFolderModel modelTwo = TransferRequestByFolderModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .folderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                .status(TransferRequestStatus.PENDING)
                .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .transferRequestId(UUID.randomUUID().toString())
                .build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertAllRetryable(txSession, List.of(modelOne, modelTwo))))
                .block();
        List<TransferRequestByFolderModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getByIds(txSession, List.of(modelOne.getIdentity(), modelTwo.getIdentity()),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Set.of(modelOne, modelTwo), new HashSet<>(result));
    }

    @Test
    public void deleteOne() {
        TransferRequestByFolderModel model = TransferRequestByFolderModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .folderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                .status(TransferRequestStatus.PENDING)
                .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .transferRequestId(UUID.randomUUID().toString())
                .build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertOneRetryable(txSession, model)))
                .block();
        Optional<TransferRequestByFolderModel> upsertResult = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getById(txSession, model.getIdentity(), model.getTenantId())))
                .block();
        Assertions.assertEquals(Optional.of(model), upsertResult);
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.deleteOneRetryable(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                model.getIdentity()))))
                .block();
        Optional<TransferRequestByFolderModel> deleteResult = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getById(txSession, model.getIdentity(), model.getTenantId())))
                .block();
        Assertions.assertEquals(Optional.empty(), deleteResult);
    }

    @Test
    public void deleteMany() {
        TransferRequestByFolderModel modelOne = TransferRequestByFolderModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .folderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                .status(TransferRequestStatus.PENDING)
                .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .transferRequestId(UUID.randomUUID().toString())
                .build();
        TransferRequestByFolderModel modelTwo = TransferRequestByFolderModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .folderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                .status(TransferRequestStatus.PENDING)
                .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .transferRequestId(UUID.randomUUID().toString())
                .build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertAllRetryable(txSession, List.of(modelOne, modelTwo))))
                .block();
        List<TransferRequestByFolderModel> upsertResult = ydbTableClient.usingSessionMonoRetryable(session ->
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
        List<TransferRequestByFolderModel> deleteResult = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getByIds(txSession, List.of(modelOne.getIdentity(), modelTwo.getIdentity()),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(deleteResult);
        Assertions.assertTrue(deleteResult.isEmpty());
    }

}
