package ru.yandex.intranet.d.dao.accounts;

import java.util.ArrayList;
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
import ru.yandex.intranet.d.datasource.Ydb;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.WithTenant;
import ru.yandex.intranet.d.model.accounts.OperationInProgressModel;

/**
 * Operations in progress DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class OperationsInProgressDaoTest {

    @Autowired
    private OperationsInProgressDao dao;
    @Autowired
    private YdbTableClient ydbTableClient;

    @Test
    public void upsertAndGetById() {
        OperationInProgressModel operation = OperationInProgressModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .operationId(UUID.randomUUID().toString())
                .folderId(UUID.randomUUID().toString())
                .build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertOneRetryable(txSession, operation)))
                .block();
        Optional<OperationInProgressModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getById(txSession, operation.getKey(), operation.getTenantId())))
                .block();
        Assertions.assertEquals(Optional.of(operation), result);
    }

    @Test
    public void upsertAndGetByIds() {
        OperationInProgressModel operationOne = OperationInProgressModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .operationId(UUID.randomUUID().toString())
                .folderId(UUID.randomUUID().toString())
                .build();
        OperationInProgressModel operationTwo = OperationInProgressModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .operationId(UUID.randomUUID().toString())
                .folderId(UUID.randomUUID().toString())
                .build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertAllRetryable(txSession, List.of(operationOne, operationTwo))))
                .block();
        List<OperationInProgressModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getByIds(txSession, List.of(operationOne.getKey(), operationTwo.getKey()),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Set.of(operationOne, operationTwo), new HashSet<>(result));
    }

    @Test
    public void deleteOne() {
        OperationInProgressModel operation = OperationInProgressModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .operationId(UUID.randomUUID().toString())
                .folderId(UUID.randomUUID().toString())
                .build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertOneRetryable(txSession, operation)))
                .block();
        Optional<OperationInProgressModel> upsertResult = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getById(txSession, operation.getKey(), operation.getTenantId())))
                .block();
        Assertions.assertEquals(Optional.of(operation), upsertResult);
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.deleteOneRetryable(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                operation.getKey()))))
                .block();
        Optional<OperationInProgressModel> deleteResult = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getById(txSession, operation.getKey(), operation.getTenantId())))
                .block();
        Assertions.assertEquals(Optional.empty(), deleteResult);
    }

    @Test
    public void deleteMany() {
        OperationInProgressModel operationOne = OperationInProgressModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .operationId(UUID.randomUUID().toString())
                .folderId(UUID.randomUUID().toString())
                .build();
        OperationInProgressModel operationTwo = OperationInProgressModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .operationId(UUID.randomUUID().toString())
                .folderId(UUID.randomUUID().toString())
                .build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertAllRetryable(txSession, List.of(operationOne, operationTwo))))
                .block();
        List<OperationInProgressModel> upsertResult = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getByIds(txSession, List.of(operationOne.getKey(), operationTwo.getKey()),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(upsertResult);
        Assertions.assertEquals(Set.of(operationOne, operationTwo), new HashSet<>(upsertResult));
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.deleteManyRetryable(txSession, List.of(new WithTenant<>(
                                Tenants.DEFAULT_TENANT_ID, operationOne.getKey()), new WithTenant<>(
                                        Tenants.DEFAULT_TENANT_ID, operationTwo.getKey())))))
                .block();
        List<OperationInProgressModel> deleteResult = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getByIds(txSession, List.of(operationOne.getKey(), operationTwo.getKey()),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(deleteResult);
        Assertions.assertTrue(deleteResult.isEmpty());
    }

    @Test
    public void getAllByTenant() {
        List<OperationInProgressModel> operations = new ArrayList<>();
        for (int i = 0; i < Ydb.MAX_RESPONSE_ROWS * 4; i++) {
            operations.add(OperationInProgressModel.builder()
                    .tenantId(Tenants.DEFAULT_TENANT_ID)
                    .operationId(UUID.randomUUID().toString())
                    .folderId(UUID.randomUUID().toString())
                    .build());
        }
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertAllRetryable(txSession, operations)))
                .block();
        List<OperationInProgressModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getAllByTenant(txSession, Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new HashSet<>(operations), new HashSet<>(result));
    }

    @Test
    public void getAllByTenantAccounts() {
        List<String> accountIds = List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString());
        List<OperationInProgressModel> operations = new ArrayList<>();
        for (int i = 0; i < Ydb.MAX_RESPONSE_ROWS * 4; i++) {
            operations.add(OperationInProgressModel.builder()
                    .tenantId(Tenants.DEFAULT_TENANT_ID)
                    .operationId(UUID.randomUUID().toString())
                    .folderId(UUID.randomUUID().toString())
                    .accountId(accountIds.get(i % accountIds.size()))
                    .build());
        }
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertAllRetryable(txSession, operations)))
                .block();
        List<OperationInProgressModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getAllByTenantAccounts(txSession, Tenants.DEFAULT_TENANT_ID,
                                new HashSet<>(accountIds))))
                .block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new HashSet<>(operations), new HashSet<>(result));
    }

    @Test
    public void getAllByTenantFolders() {
        List<String> folderIds = List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString());
        List<OperationInProgressModel> operations = new ArrayList<>();
        for (int i = 0; i < Ydb.MAX_RESPONSE_ROWS * 4; i++) {
            operations.add(OperationInProgressModel.builder()
                    .tenantId(Tenants.DEFAULT_TENANT_ID)
                    .operationId(UUID.randomUUID().toString())
                    .folderId(folderIds.get(i % folderIds.size()))
                    .accountId(i % 2 == 0 ? UUID.randomUUID().toString() : null)
                    .build());
        }
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertAllRetryable(txSession, operations)))
                .block();
        List<OperationInProgressModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getAllByTenantFolders(txSession, Tenants.DEFAULT_TENANT_ID,
                                new HashSet<>(folderIds))))
                .block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new HashSet<>(operations), new HashSet<>(result));
    }

}
