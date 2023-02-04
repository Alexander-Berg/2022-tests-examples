package ru.yandex.intranet.d.dao.accounts;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.datasource.model.WithTxId;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel;
import ru.yandex.intranet.d.model.accounts.OperationChangesModel;
import ru.yandex.intranet.d.model.accounts.OperationErrorKind;
import ru.yandex.intranet.d.model.accounts.OperationOrdersModel;
import ru.yandex.intranet.d.model.accounts.OperationSource;

import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNTS_QUOTAS_OPERATIONS_1;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNTS_QUOTAS_OPERATIONS_2;
import static ru.yandex.intranet.d.dao.Tenants.DEFAULT_TENANT_ID;

/**
 * AccountsQuotasOperationsDaoTest.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 22.10.2020
 */
@IntegrationTest
class AccountsQuotasOperationsDaoTest {
    @Autowired
    private AccountsQuotasOperationsDao accountsQuotasOperationsDao;
    @Autowired
    private YdbTableClient ydbTableClient;

    @Test
    void getByIdStartTx() {
        WithTxId<Optional<AccountsQuotasOperationsModel>> res = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasOperationsDao.getByIdStartTx(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_ACCOUNTS_QUOTAS_OPERATIONS_1.getOperationId(),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(res);
        Assertions.assertTrue(res.get().isPresent());
        Assertions.assertEquals(TEST_ACCOUNTS_QUOTAS_OPERATIONS_1, res.get().get());
    }

    @Test
    void getByIds() {
        List<AccountsQuotasOperationsModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasOperationsDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(
                                TEST_ACCOUNTS_QUOTAS_OPERATIONS_1.getOperationId(),
                                TEST_ACCOUNTS_QUOTAS_OPERATIONS_2.getOperationId()
                        ),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(2, models.size());
        Assertions.assertTrue(models.contains(TEST_ACCOUNTS_QUOTAS_OPERATIONS_1));
        Assertions.assertTrue(models.contains(TEST_ACCOUNTS_QUOTAS_OPERATIONS_2));
    }

    @Test
    void upsertOneTx() {
        AccountsQuotasOperationsModel newAccountsQuotasOperations = new AccountsQuotasOperationsModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setOperationId("273c0fee-5cf1-4083-b9dc-8ec0e855e150")
                .setLastRequestId("3223343")
                .setCreateDateTime(Instant.ofEpochSecond(1603385085))
                .setOperationSource(OperationSource.USER)
                .setOperationType(AccountsQuotasOperationsModel.OperationType.DELETE_ACCOUNT)
                .setAuthorUserId(TestUsers.USER_1_ID)
                .setAuthorUserUid(null)
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setAccountsSpaceId(null)
                .setUpdateDateTime(Instant.ofEpochSecond(1603385085))
                .setRequestStatus(AccountsQuotasOperationsModel.RequestStatus.OK)
                .setErrorMessage(null)
                .setFullErrorMessage(null)
                .setRequestedChanges(OperationChangesModel.builder()
                        .accountId("56a41608-84df-41c4-9653-89106462e0ce")
                        .build())
                .setOrders(OperationOrdersModel.builder().submitOrder(1).build())
                .setErrorKind(OperationErrorKind.UNKNOWN)
                .build();

        ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasOperationsDao.upsertOneTxRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        newAccountsQuotasOperations
                )
        ).block();

        WithTxId<Optional<AccountsQuotasOperationsModel>> res = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasOperationsDao.getByIdStartTx(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        newAccountsQuotasOperations.getOperationId(),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(res);
        Assertions.assertTrue(res.get().isPresent());
        Assertions.assertEquals(newAccountsQuotasOperations, res.get().get());
    }

    @Test
    void upsertAll() {
        AccountsQuotasOperationsModel newAccountQuotaOperations1 = new AccountsQuotasOperationsModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setOperationId("273c0fee-5cf1-4083-b9dc-8ec0e855e150")
                .setLastRequestId("3223343")
                .setCreateDateTime(Instant.ofEpochSecond(1603385085))
                .setOperationSource(OperationSource.USER)
                .setOperationType(AccountsQuotasOperationsModel.OperationType.DELETE_ACCOUNT)
                .setAuthorUserId(TestUsers.USER_1_ID)
                .setAuthorUserUid(null)
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setAccountsSpaceId(null)
                .setUpdateDateTime(Instant.ofEpochSecond(1603385085))
                .setRequestStatus(AccountsQuotasOperationsModel.RequestStatus.OK)
                .setErrorMessage(null)
                .setFullErrorMessage(null)
                .setRequestedChanges(OperationChangesModel.builder()
                        .accountId("56a41608-84df-41c4-9653-89106462e0ce")
                        .build())
                .setOrders(OperationOrdersModel.builder().submitOrder(1).build())
                .setErrorKind(OperationErrorKind.UNKNOWN)
                .build();
        AccountsQuotasOperationsModel newAccountQuotaOperations2 = new AccountsQuotasOperationsModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setOperationId("273c0fee-5cf1-4083-b9dc-8ec0e855e150")
                .setLastRequestId("3223343")
                .setCreateDateTime(Instant.ofEpochSecond(1603385085))
                .setOperationSource(OperationSource.USER)
                .setOperationType(AccountsQuotasOperationsModel.OperationType.DELETE_ACCOUNT)
                .setAuthorUserId(TestUsers.USER_1_ID)
                .setAuthorUserUid(null)
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setAccountsSpaceId(null)
                .setUpdateDateTime(Instant.ofEpochSecond(1603385085))
                .setRequestStatus(AccountsQuotasOperationsModel.RequestStatus.OK)
                .setErrorMessage(null)
                .setFullErrorMessage(null)
                .setRequestedChanges(OperationChangesModel.builder()
                        .accountId("56a41608-84df-41c4-9653-89106462e0ce")
                        .build())
                .setOrders(OperationOrdersModel.builder().submitOrder(1).build())
                .build();

        ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasOperationsDao.upsertAllRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        List.of(newAccountQuotaOperations1, newAccountQuotaOperations2)
                )
        ).block();

        List<AccountsQuotasOperationsModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasOperationsDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(newAccountQuotaOperations1.getOperationId(),
                                newAccountQuotaOperations2.getOperationId()),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(1, models.size());
        Assertions.assertTrue(models.contains(newAccountQuotaOperations2));
    }

    @Test
    void remove() {
        ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasOperationsDao.removeRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        TEST_ACCOUNTS_QUOTAS_OPERATIONS_1.getOperationId(),
                        DEFAULT_TENANT_ID,
                        2
                )
        ).block();
        List<AccountsQuotasOperationsModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasOperationsDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TEST_ACCOUNTS_QUOTAS_OPERATIONS_1.getOperationId(),
                                TEST_ACCOUNTS_QUOTAS_OPERATIONS_2.getOperationId()),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(1, models.size());
        Assertions.assertTrue(models.contains(TEST_ACCOUNTS_QUOTAS_OPERATIONS_2));
    }

    @Test
    void removeAll() {
        ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasOperationsDao.removeAllRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        List.of(TEST_ACCOUNTS_QUOTAS_OPERATIONS_1.getOperationId()),
                        DEFAULT_TENANT_ID
                )
        ).block();

        List<AccountsQuotasOperationsModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasOperationsDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TEST_ACCOUNTS_QUOTAS_OPERATIONS_1.getOperationId(),
                                TEST_ACCOUNTS_QUOTAS_OPERATIONS_2.getOperationId()),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(1, models.size());
        Assertions.assertTrue(models.contains(TEST_ACCOUNTS_QUOTAS_OPERATIONS_2));
    }

    @Test
    void getAllByIdsWithPaginationTest() {
        List<AccountsQuotasOperationsModel> models = Objects.requireNonNull(
                ydbTableClient.usingSessionMonoRetryable(session ->
                        accountsQuotasOperationsDao.getAllByIds(
                                session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                DEFAULT_TENANT_ID,
                                List.of(
                                        TEST_ACCOUNTS_QUOTAS_OPERATIONS_1.getOperationId(),
                                        TEST_ACCOUNTS_QUOTAS_OPERATIONS_2.getOperationId()
                                )
                        )
                ).block());

        Assertions.assertNotNull(models);
        Assertions.assertEquals(2, models.size());
        Assertions.assertTrue(models.contains(TEST_ACCOUNTS_QUOTAS_OPERATIONS_1));
        Assertions.assertTrue(models.contains(TEST_ACCOUNTS_QUOTAS_OPERATIONS_2));
    }
}
