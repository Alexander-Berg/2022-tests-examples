package ru.yandex.intranet.d.dao.accounts;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.folders.FolderDao;
import ru.yandex.intranet.d.datasource.impl.YdbRetry;
import ru.yandex.intranet.d.datasource.model.WithTxId;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.metrics.YdbMetrics;
import ru.yandex.intranet.d.model.WithTenant;
import ru.yandex.intranet.d.model.accounts.AccountModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderType;
import ru.yandex.intranet.d.model.quotas.QuotaAggregationModel;

import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_1_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_1_QUOTA_1;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_2_QUOTA_1;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_5_ID;
import static ru.yandex.intranet.d.dao.Tenants.DEFAULT_TENANT_ID;

/**
 * AccountsQuotasDaoTest.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 21.10.2020
 */
@IntegrationTest
class AccountsQuotasDaoTest {
    @Autowired
    private AccountsQuotasDao accountsQuotasDao;
    @Autowired
    private AccountsDao accountsDao;
    @Autowired
    private FolderDao folderDao;
    @Autowired
    private ProviderReserveAccountsDao providerReserveAccountsDao;
    @Autowired
    private YdbTableClient ydbTableClient;
    @Autowired
    private YdbMetrics ydbMetrics;
    @Value("${ydb.transactionRetries}")
    private long transactionRetries;

    @Test
    void getByIdStartTx() {
        WithTxId<Optional<AccountsQuotasModel>> res = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.getByIdStartTx(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        new AccountsQuotasModel.Identity(TEST_ACCOUNT_1_ID, TestResources.YP_HDD_MAN),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(res);
        Assertions.assertTrue(res.get().isPresent());
        Assertions.assertEquals(TEST_ACCOUNT_1_QUOTA_1, res.get().get());
    }

    @Test
    void getByIds() {
        List<AccountsQuotasModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(
                                TEST_ACCOUNT_1_QUOTA_1.getIdentity(),
                                TEST_ACCOUNT_2_QUOTA_1.getIdentity()
                        ),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(2, models.size());
        Assertions.assertTrue(models.contains(TEST_ACCOUNT_1_QUOTA_1));
        Assertions.assertTrue(models.contains(TEST_ACCOUNT_2_QUOTA_1));
    }

    @Test
    void upsertOneTx() {
        AccountsQuotasModel newAccountQuota = new AccountsQuotasModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setAccountId(TEST_ACCOUNT_1_ID)
                .setResourceId(TestResources.YP_SSD_VLA)
                .setProvidedQuota(33L)
                .setAllocatedQuota(22L)
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setProviderId(TestProviders.YP_ID)
                .setLastProvisionUpdate(Instant.parse("2020-12-02T00:00:00.000Z"))
                .setLastReceivedProvisionVersion(1L)
                .setLatestSuccessfulProvisionOperationId("9f69de2a-7a27-423b-adc8-f3453b5ae4a6")
                .build();

        ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.upsertOneTxRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        newAccountQuota
                )
        ).block();

        WithTxId<Optional<AccountsQuotasModel>> res = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.getByIdStartTx(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        newAccountQuota.getIdentity(),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(res);
        Assertions.assertTrue(res.get().isPresent());
        Assertions.assertEquals(newAccountQuota, res.get().get());
    }

    @Test
    void upsertAll() {
        AccountsQuotasModel newAccountQuota1 = new AccountsQuotasModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setAccountId(TEST_ACCOUNT_1_ID)
                .setResourceId(TestResources.YP_SSD_VLA)
                .setProvidedQuota(33L)
                .setAllocatedQuota(22L)
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setProviderId(TestProviders.YP_ID)
                .setLastProvisionUpdate(Instant.parse("2020-12-02T00:00:00.000Z"))
                .setLastReceivedProvisionVersion(1L)
                .setLatestSuccessfulProvisionOperationId("018c53d7-79fa-423e-9e15-35d5a5a8c2d8")
                .build();
        AccountsQuotasModel newAccountQuota2 = new AccountsQuotasModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setAccountId(TEST_ACCOUNT_1_ID)
                .setResourceId(TestResources.YP_SSD_VLA)
                .setProvidedQuota(55L)
                .setAllocatedQuota(44L)
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setProviderId(TestProviders.YP_ID)
                .setLastProvisionUpdate(Instant.parse("2020-12-02T00:00:00.000Z"))
                .setLastReceivedProvisionVersion(1L)
                .setLatestSuccessfulProvisionOperationId("5e55b407-2d09-47d4-8994-adb0ce89af0c")
                .build();

        ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.upsertAllRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        List.of(newAccountQuota1, newAccountQuota2)
                )
        ).block();

        List<AccountsQuotasModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(newAccountQuota1.getIdentity(), newAccountQuota2.getIdentity()),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(1, models.size());
        Assertions.assertTrue(models.contains(newAccountQuota2));
    }

    @Test
    void remove() {
        ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.removeRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        TEST_ACCOUNT_1_QUOTA_1.getIdentity(),
                        DEFAULT_TENANT_ID,
                        2
                )
        ).block();
        List<AccountsQuotasModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TEST_ACCOUNT_1_QUOTA_1.getIdentity(), TEST_ACCOUNT_2_QUOTA_1.getIdentity()),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(1, models.size());
        Assertions.assertTrue(models.contains(TEST_ACCOUNT_2_QUOTA_1));
    }

    @Test
    void removeAll() {
        List<AccountsQuotasModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TEST_ACCOUNT_1_QUOTA_1.getIdentity(), TEST_ACCOUNT_2_QUOTA_1.getIdentity()),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(2, models.size());
        Assertions.assertTrue(models.contains(TEST_ACCOUNT_2_QUOTA_1));

        ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.removeAllRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        List.of(TEST_ACCOUNT_1_QUOTA_1.getIdentity()),
                        DEFAULT_TENANT_ID
                )
        ).block();

        models = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TEST_ACCOUNT_1_QUOTA_1.getIdentity(), TEST_ACCOUNT_2_QUOTA_1.getIdentity()),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(1, models.size());
        Assertions.assertTrue(models.contains(TEST_ACCOUNT_2_QUOTA_1));
    }

    @Test
    public void testGetAllByResourceIds() {
        int count = 1100;
        List<AccountsQuotasModel> quotasToUpsert = new ArrayList<>(count);
        for (int i = 0; i < count / 2; i++) {
            quotasToUpsert.add(new AccountsQuotasModel.Builder()
                    .setResourceId(TestResources.YP_SSD_VLA)
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setAccountId(UUID.randomUUID().toString())
                    .setFolderId(UUID.randomUUID().toString())
                    .setProviderId(TestProviders.YP_ID)
                    .setProvidedQuota(0L)
                    .setAllocatedQuota(0L)
                    .setLastProvisionUpdate(Instant.now())
                    .build());
            quotasToUpsert.add(new AccountsQuotasModel.Builder()
                    .setResourceId(TestResources.YP_SSD_MAN)
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setAccountId(UUID.randomUUID().toString())
                    .setFolderId(UUID.randomUUID().toString())
                    .setProviderId(TestProviders.YP_ID)
                    .setProvidedQuota(0L)
                    .setAllocatedQuota(0L)
                    .setLastProvisionUpdate(Instant.now())
                    .build());
        }

        WithTxId<List<AccountsQuotasModel>> quotaModelsBeforeUpsert = ydbTableClient.usingSessionMonoRetryable(
                session -> accountsQuotasDao.getAllByResourceIds(
                        session.asTxCommit(TransactionMode.ONLINE_READ_ONLY), Tenants.DEFAULT_TENANT_ID,
                        List.of(TestResources.YP_SSD_VLA, TestResources.YP_SSD_MAN)
                ).retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotaModelsBeforeUpsert);
        Assertions.assertNotNull(quotaModelsBeforeUpsert.get());
        count += quotaModelsBeforeUpsert.get().size();

        ydbTableClient.usingSessionMonoRetryable(session -> accountsQuotasDao.upsertAllRetryable(
                session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE), quotasToUpsert)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        WithTxId<List<AccountsQuotasModel>> quotaModels = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.getAllByResourceIds(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID,
                        List.of(TestResources.YP_SSD_VLA, TestResources.YP_SSD_MAN)
                ).retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotaModels);
        Assertions.assertNotNull(quotaModels.get());
        Assertions.assertEquals(count, quotaModels.get().size());
    }

    @Test
    public void testGetAllByFolderIds() {
        int count = 1100;
        List<AccountsQuotasModel> quotasToUpsert = new ArrayList<>(count);
        String folderId1 = UUID.randomUUID().toString();
        String folderId2 = UUID.randomUUID().toString();
        Set<String> folderIds = Set.of(folderId1, folderId2);
        for (int i = 0; i < count / 2; i++) {
            quotasToUpsert.add(new AccountsQuotasModel.Builder()
                    .setResourceId(TestResources.YP_SSD_VLA)
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setAccountId(UUID.randomUUID().toString())
                    .setFolderId(folderId1)
                    .setProviderId(TestProviders.YP_ID)
                    .setProvidedQuota(0L)
                    .setAllocatedQuota(0L)
                    .setLastProvisionUpdate(Instant.now())
                    .build());
            quotasToUpsert.add(new AccountsQuotasModel.Builder()
                    .setResourceId(TestResources.YP_SSD_VLA)
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setAccountId(UUID.randomUUID().toString())
                    .setFolderId(folderId2)
                    .setProviderId(TestProviders.YP_ID)
                    .setProvidedQuota(0L)
                    .setAllocatedQuota(0L)
                    .setLastProvisionUpdate(Instant.now())
                    .build());
        }

        WithTxId<List<AccountsQuotasModel>> quotaModelsBeforeUpsert = ydbTableClient.usingSessionMonoRetryable(
                session -> accountsQuotasDao.getAllByFolderIds(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID, folderIds)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotaModelsBeforeUpsert);
        Assertions.assertNotNull(quotaModelsBeforeUpsert.get());
        count += quotaModelsBeforeUpsert.get().size();

        ydbTableClient.usingSessionMonoRetryable(session -> accountsQuotasDao.upsertAllRetryable(
                session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE), quotasToUpsert)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        WithTxId<List<AccountsQuotasModel>> quotaModels = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.getAllByFolderIds(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID, folderIds)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotaModels);
        Assertions.assertNotNull(quotaModels.get());
        Assertions.assertEquals(count, quotaModels.get().size());
        Assertions.assertTrue(quotaModels.get().stream().allMatch(quota -> folderIds.contains(quota.getFolderId())));
    }

    @Test
    public void testGetByTenantFolderProviderResource() {
        int count = 2200;
        List<AccountsQuotasModel> quotasToUpsert = new ArrayList<>(count);
        List<AccountModel> accountsToUpsert = new ArrayList<>(count);
        Set<WithTenant<AccountsQuotasDao.FolderProviderAccountsSpaceResource>> paramsSet = new HashSet<>();
        String folderId1 = UUID.randomUUID().toString();
        String folderId2 = UUID.randomUUID().toString();
        String accountsSpaceId = UUID.randomUUID().toString();
        Set<String> folderIds = Set.of(folderId1, folderId2);
        for (int i = 0; i < count / 2; i++) {
            AccountsQuotasModel firstQuota = new AccountsQuotasModel.Builder()
                    .setResourceId(TestResources.YP_SSD_VLA)
                    .setTenantId(DEFAULT_TENANT_ID)
                    .setAccountId(UUID.randomUUID().toString())
                    .setFolderId(folderId1)
                    .setProviderId(TestProviders.YP_ID)
                    .setProvidedQuota(0L)
                    .setAllocatedQuota(0L)
                    .setLastProvisionUpdate(Instant.now())
                    .build();
            AccountModel firstAccount = new AccountModel.Builder()
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setId(firstQuota.getAccountId())
                    .setVersion(1)
                    .setDeleted(false)
                    .setDisplayName("новый аккаунт")
                    .setProviderId(TestProviders.YP_ID)
                    .setOuterAccountIdInProvider("42")
                    .setOuterAccountKeyInProvider("new")
                    .setFolderId(folderId1)
                    .setAccountsSpacesId(accountsSpaceId)
                    .setLastAccountUpdate(Instant.now())
                    .build();
            quotasToUpsert.add(firstQuota);
            accountsToUpsert.add(firstAccount);
            paramsSet.add(new WithTenant<>(DEFAULT_TENANT_ID,
                    new AccountsQuotasDao.FolderProviderAccountsSpaceResource(folderId1, TestProviders.YP_ID,
                            firstAccount.getAccountsSpacesId().orElse(null), TestResources.YP_SSD_VLA)));
            AccountsQuotasModel secondQuota = new AccountsQuotasModel.Builder()
                    .setResourceId(TestResources.YP_SSD_VLA)
                    .setTenantId(DEFAULT_TENANT_ID)
                    .setAccountId(UUID.randomUUID().toString())
                    .setFolderId(folderId2)
                    .setProviderId(TestProviders.YP_ID)
                    .setProvidedQuota(0L)
                    .setAllocatedQuota(0L)
                    .setLastProvisionUpdate(Instant.now())
                    .build();
            AccountModel secondAccount = new AccountModel.Builder()
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setId(secondQuota.getAccountId())
                    .setVersion(1)
                    .setDeleted(false)
                    .setDisplayName("новый аккаунт")
                    .setProviderId(TestProviders.YP_ID)
                    .setOuterAccountIdInProvider("42")
                    .setOuterAccountKeyInProvider("new")
                    .setFolderId(folderId2)
                    .setLastAccountUpdate(Instant.now())
                    .build();
            quotasToUpsert.add(secondQuota);
            accountsToUpsert.add(secondAccount);
            paramsSet.add(new WithTenant<>(DEFAULT_TENANT_ID,
                    new AccountsQuotasDao.FolderProviderAccountsSpaceResource(folderId2, TestProviders.YP_ID,
                            secondAccount.getAccountsSpacesId().orElse(null), TestResources.YP_SSD_VLA)));
        }
        WithTxId<List<AccountsQuotasModel>> quotaModelsBeforeUpsert = ydbTableClient.usingSessionMonoRetryable(
                session -> accountsQuotasDao.getAllByFolderIds(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                                Tenants.DEFAULT_TENANT_ID, folderIds)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotaModelsBeforeUpsert);
        Assertions.assertNotNull(quotaModelsBeforeUpsert.get());
        count += quotaModelsBeforeUpsert.get().size();
        ydbTableClient.usingSessionMonoRetryable(session -> accountsDao.upsertAllRetryable(
                        session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE), accountsToUpsert)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        ydbTableClient.usingSessionMonoRetryable(session -> accountsQuotasDao.upsertAllRetryable(
                        session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE), quotasToUpsert)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<AccountsQuotasModel> quotaModels = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.getByTenantFolderProviderResource(session
                                        .asTxCommit(TransactionMode.ONLINE_READ_ONLY), new ArrayList<>(paramsSet))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotaModels);
        Assertions.assertEquals(count, quotaModels.size());
        Assertions.assertTrue(quotaModels.stream().allMatch(quota -> folderIds.contains(quota.getFolderId())));
    }

    @Test
    void getAllByAccountIdsPageTest() {
        // insert 3000 quotas
        ArrayList<AccountsQuotasModel> quotas = new ArrayList<>();
        Set<String> resourceIds = new HashSet<>();
        for (int i = 0; i < 3000; i++) {
            String resourceId = String.valueOf(i);
            quotas.add(new AccountsQuotasModel.Builder()
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setAccountId(TEST_ACCOUNT_5_ID)
                    .setResourceId(resourceId)
                    .setProvidedQuota(33L)
                    .setAllocatedQuota(22L)
                    .setFolderId(TestFolders.TEST_FOLDER_4_ID)
                    .setProviderId(TestProviders.YDB_ID)
                    .setLastProvisionUpdate(Instant.parse("2020-12-02T00:00:00.000Z"))
                    .setLastReceivedProvisionVersion(1L)
                    .setLatestSuccessfulProvisionOperationId("018c53d7-79fa-423e-9e15-35d5a5a8c2d8")
                    .build()
            );
            resourceIds.add(resourceId);
        }
        ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.upsertAllRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        quotas
                )
        ).block();

        // load and check
        List<AccountsQuotasModel> res = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.getAllByAccountIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        DEFAULT_TENANT_ID,
                        Set.of(TEST_ACCOUNT_5_ID)
                )
        ).block();
        Assertions.assertNotNull(res);
        Assertions.assertEquals(3000, res.size());
        Assertions.assertEquals(
                resourceIds,
                res.stream().map(AccountsQuotasModel::getResourceId).collect(Collectors.toSet())
        );
    }

    @Test
    public void getForAggregationByProviderAndResources() {
        String providerId = UUID.randomUUID().toString();
        String resourceIdOne = UUID.randomUUID().toString();
        String resourceIdTwo = UUID.randomUUID().toString();
        List<AccountsQuotasModel> quotas = new ArrayList<>();
        List<FolderModel> folders = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            String folderId = UUID.randomUUID().toString();
            folders.add(new FolderModel(folderId, Tenants.DEFAULT_TENANT_ID,
                    42L, 0, "test", "test", false,
                    FolderType.COMMON_DEFAULT_FOR_SERVICE, Set.of(), 0));
            quotas.add(new AccountsQuotasModel.Builder()
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setAccountId(UUID.randomUUID().toString())
                    .setResourceId(resourceIdOne)
                    .setProvidedQuota(5L)
                    .setAllocatedQuota(3L)
                    .setFolderId(folderId)
                    .setProviderId(providerId)
                    .setLastProvisionUpdate(Instant.now())
                    .setFrozenProvidedQuota(0L)
                    .build());
        }
        for (int i = 0; i < 2000; i++) {
            String folderId = UUID.randomUUID().toString();
            folders.add(new FolderModel(folderId, Tenants.DEFAULT_TENANT_ID,
                    42L, 0, "test", "test", false,
                    FolderType.COMMON_DEFAULT_FOR_SERVICE, Set.of(), 0));
            quotas.add(new AccountsQuotasModel.Builder()
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setAccountId(UUID.randomUUID().toString())
                    .setResourceId(resourceIdTwo)
                    .setProvidedQuota(5L)
                    .setAllocatedQuota(3L)
                    .setFolderId(folderId)
                    .setProviderId(providerId)
                    .setLastProvisionUpdate(Instant.now())
                    .setFrozenProvidedQuota(0L)
                    .build());
        }
        ydbTableClient.usingSessionMonoRetryable(session -> folderDao.upsertAllRetryable(
                        session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE), folders)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        ydbTableClient.usingSessionMonoRetryable(session -> accountsQuotasDao.upsertAllRetryable(
                        session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE), quotas)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<QuotaAggregationModel> result = ydbTableClient.usingSessionMonoRetryable(session -> accountsQuotasDao
                .getForAggregationByProviderAndResources(session.asTxCommit(TransactionMode.STALE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID, providerId, List.of(resourceIdOne, resourceIdTwo))).block();
        Assertions.assertEquals(4000, result.size());
        Assertions.assertEquals(quotas.stream()
                .map(m -> new QuotaAggregationModel(m.getResourceId(),
                        null, null, m.getProvidedQuota(), m.getAllocatedQuota(), 42L,
                        m.getFolderId(), m.getAccountId(), FolderType.COMMON_DEFAULT_FOR_SERVICE))
                .collect(Collectors.toSet()), new HashSet<>(result));
    }

    @Test
    public void getForAggregationByProvider() {
        String providerId = UUID.randomUUID().toString();
        String resourceIdOne = UUID.randomUUID().toString();
        String resourceIdTwo = UUID.randomUUID().toString();
        List<AccountsQuotasModel> quotas = new ArrayList<>();
        List<FolderModel> folders = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            String folderId = UUID.randomUUID().toString();
            folders.add(new FolderModel(folderId, Tenants.DEFAULT_TENANT_ID,
                    42L, 0, "test", "test", false,
                    FolderType.COMMON_DEFAULT_FOR_SERVICE, Set.of(), 0));
            quotas.add(new AccountsQuotasModel.Builder()
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setAccountId(UUID.randomUUID().toString())
                    .setResourceId(resourceIdOne)
                    .setProvidedQuota(5L)
                    .setAllocatedQuota(3L)
                    .setFolderId(folderId)
                    .setProviderId(providerId)
                    .setLastProvisionUpdate(Instant.now())
                    .setFrozenProvidedQuota(0L)
                    .build());
        }
        for (int i = 0; i < 2000; i++) {
            String folderId = UUID.randomUUID().toString();
            folders.add(new FolderModel(folderId, Tenants.DEFAULT_TENANT_ID,
                    42L, 0, "test", "test", false,
                    FolderType.COMMON_DEFAULT_FOR_SERVICE, Set.of(), 0));
            quotas.add(new AccountsQuotasModel.Builder()
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setAccountId(UUID.randomUUID().toString())
                    .setResourceId(resourceIdTwo)
                    .setProvidedQuota(5L)
                    .setAllocatedQuota(3L)
                    .setFolderId(folderId)
                    .setProviderId(providerId)
                    .setLastProvisionUpdate(Instant.now())
                    .setFrozenProvidedQuota(0L)
                    .build());
        }
        ydbTableClient.usingSessionMonoRetryable(session -> folderDao.upsertAllRetryable(
                        session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE), folders)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        ydbTableClient.usingSessionMonoRetryable(session -> accountsQuotasDao.upsertAllRetryable(
                        session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE), quotas)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<QuotaAggregationModel> result = ydbTableClient.usingSessionMonoRetryable(session -> accountsQuotasDao
                .getForAggregationByProvider(session.asTxCommit(TransactionMode.STALE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID, providerId)).block();
        Assertions.assertEquals(4000, result.size());
        Assertions.assertEquals(quotas.stream()
                .map(m -> new QuotaAggregationModel(m.getResourceId(),
                        null, null, m.getProvidedQuota(), m.getAllocatedQuota(), 42L,
                        m.getFolderId(), m.getAccountId(), FolderType.COMMON_DEFAULT_FOR_SERVICE))
                .collect(Collectors.toSet()), new HashSet<>(result));
    }

}
