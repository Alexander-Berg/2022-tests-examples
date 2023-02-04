package ru.yandex.intranet.d.dao.accounts;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.folders.FolderDao;
import ru.yandex.intranet.d.datasource.Ydb;
import ru.yandex.intranet.d.datasource.model.WithTxId;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.accounts.AccountModel;
import ru.yandex.intranet.d.model.accounts.AccountReserveType;
import ru.yandex.intranet.d.model.accounts.ServiceAccountKeys;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderType;

import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_1;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_1_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_2;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_2_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_3;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_3_ID;
import static ru.yandex.intranet.d.dao.Tenants.DEFAULT_TENANT_ID;

/**
 * AccountsDaoTest.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 19.10.2020
 */
@IntegrationTest
class AccountsDaoTest {
    @Autowired
    private AccountsDao accountsDao;
    @Autowired
    private FolderDao folderDao;
    @Autowired
    private YdbTableClient ydbTableClient;

    @Test
    void getByIdStartTx() {
        WithTxId<Optional<AccountModel>> res = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.getByIdStartTx(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_ACCOUNT_1_ID,
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(res);
        Assertions.assertTrue(res.get().isPresent());
        Assertions.assertEquals(TEST_ACCOUNT_1, res.get().get());
    }

    @Test
    void getByIds() {
        List<AccountModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TEST_ACCOUNT_1_ID, TEST_ACCOUNT_2_ID),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(2, models.size());
        Assertions.assertTrue(models.contains(TEST_ACCOUNT_1));
        Assertions.assertTrue(models.contains(TEST_ACCOUNT_2));
    }

    @Test
    void upsertOneTx() {
        String newAccountID = "0dbd459b-b7ea-4f0c-9450-5ecba65becec";
        AccountModel newAccount = new AccountModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setId(newAccountID)
                .setVersion(1)
                .setDeleted(false)
                .setDisplayName("новый аккаунт")
                .setProviderId(TestProviders.YP_ID)
                .setOuterAccountIdInProvider("555")
                .setOuterAccountKeyInProvider("new")
                .setFolderId(TestFolders.TEST_FOLDER_2_ID)
                .setLastAccountUpdate(Instant.parse("2020-12-02T00:00:00.000Z"))
                .setLastReceivedVersion(1L)
                .setLatestSuccessfulAccountOperationId("023dd916-f17e-4939-a068-b9b32c9ba316")
                .build();

        ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.upsertOneTxRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        newAccount
                )
        ).block();

        WithTxId<Optional<AccountModel>> res = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.getByIdStartTx(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        newAccountID,
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(res);
        Assertions.assertTrue(res.get().isPresent());
        Assertions.assertEquals(newAccount, res.get().get());
    }

    @Test
    void upsertAll() {
        String newAccount1Id = "0dbd459b-b7ea-4f0c-9450-5ecba65becec";
        AccountModel newAccount1 = new AccountModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setId(newAccount1Id)
                .setVersion(1)
                .setDeleted(false)
                .setDisplayName("новый аккаунт")
                .setProviderId(TestProviders.YP_ID)
                .setOuterAccountIdInProvider("555")
                .setOuterAccountKeyInProvider("new")
                .setFolderId(TestFolders.TEST_FOLDER_2_ID)
                .setLastAccountUpdate(Instant.parse("2020-12-02T00:00:00.000Z"))
                .setLastReceivedVersion(1L)
                .setLatestSuccessfulAccountOperationId("0df504f2-9790-445e-adb4-7dd170ea13d4")
                .build();

        String newAccount2Id = "8a3a68f2-031a-4512-a7c5-a4442c11b4bc";
        AccountModel newAccount2 = new AccountModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setId(newAccount2Id)
                .setVersion(1)
                .setDeleted(false)
                .setDisplayName("новый аккаунт 2")
                .setProviderId(TestProviders.YP_ID)
                .setOuterAccountIdInProvider("777")
                .setOuterAccountKeyInProvider("new-new")
                .setFolderId(TestFolders.TEST_FOLDER_2_ID)
                .setLastAccountUpdate(Instant.parse("2020-12-02T00:00:00.000Z"))
                .setLastReceivedVersion(1L)
                .setLatestSuccessfulAccountOperationId("7571f4c5-a319-4d7f-8746-3f618e476022")
                .setReserveType(AccountReserveType.PROVIDER)
                .build();

        ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.upsertAllRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        List.of(newAccount1, newAccount2)
                )
        ).block();

        List<AccountModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(newAccount1Id, newAccount2Id),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(2, models.size());
        Assertions.assertTrue(models.contains(newAccount1));
        Assertions.assertTrue(models.contains(newAccount2));
    }

    @Test
    void remove() {
        ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.removeRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        TEST_ACCOUNT_1_ID,
                        DEFAULT_TENANT_ID,
                        2
                )
        ).block();
        List<AccountModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TEST_ACCOUNT_1_ID, TEST_ACCOUNT_2_ID),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(1, models.size());
        Assertions.assertTrue(models.contains(TEST_ACCOUNT_2));
    }

    @Test
    void removeAll() {
        ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.removeAllRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        List.of(TEST_ACCOUNT_1_ID),
                        DEFAULT_TENANT_ID
                )
        ).block();

        List<AccountModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TEST_ACCOUNT_1_ID, TEST_ACCOUNT_2_ID),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(1, models.size());
        Assertions.assertTrue(models.contains(TEST_ACCOUNT_2));
    }

    @Test
    public void getAllNonDeletedByAccountsSpaceExcludingEmpty() {
        List<AccountModel> accounts = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.getAllNonDeletedByAccountsSpaceExcluding(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        DEFAULT_TENANT_ID,
                        UUID.randomUUID().toString(),
                        "",
                        Set.of())
        ).block();
        Assertions.assertNotNull(accounts);
        Assertions.assertTrue(accounts.isEmpty());
    }

    @Test
    public void getAllNonDeletedByAccountsSpaceExcludingOne() {
        AccountModel newAccount = new AccountModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setId(UUID.randomUUID().toString())
                .setVersion(1)
                .setDeleted(false)
                .setDisplayName("Test")
                .setProviderId(UUID.randomUUID().toString())
                .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                .setOuterAccountKeyInProvider(null)
                .setFolderId(UUID.randomUUID().toString())
                .setLastAccountUpdate(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .setLastReceivedVersion(1L)
                .setLatestSuccessfulAccountOperationId(UUID.randomUUID().toString())
                .build();
        ydbTableClient.usingSessionMonoRetryable(session -> accountsDao
                .upsertOneTxRetryable(session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), newAccount)
        ).block();
        List<AccountModel> accounts = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.getAllNonDeletedByAccountsSpaceExcluding(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        DEFAULT_TENANT_ID,
                        newAccount.getProviderId(),
                        "",
                        Set.of())
        ).block();
        Assertions.assertNotNull(accounts);
        Assertions.assertEquals(1, accounts.size());
        Assertions.assertEquals(Set.of(newAccount), new HashSet<>(accounts));
    }

    @Test
    public void getAllNonDeletedByAccountsSpaceExcludingOnePage() {
        List<AccountModel> accounts = new ArrayList<>();
        String providerId = UUID.randomUUID().toString();
        for (int i = 0; i < Ydb.MAX_RESPONSE_ROWS; i++) {
            accounts.add(new AccountModel.Builder()
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setId(UUID.randomUUID().toString())
                    .setVersion(1)
                    .setDeleted(false)
                    .setDisplayName("Test")
                    .setProviderId(providerId)
                    .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                    .setOuterAccountKeyInProvider(null)
                    .setFolderId(UUID.randomUUID().toString())
                    .setLastAccountUpdate(Instant.now().truncatedTo(ChronoUnit.MICROS))
                    .setLastReceivedVersion(1L)
                    .setLatestSuccessfulAccountOperationId(UUID.randomUUID().toString())
                    .build());
        }
        ydbTableClient.usingSessionMonoRetryable(session -> accountsDao
                .upsertAllRetryable(session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), accounts)
        ).block();
        List<AccountModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.getAllNonDeletedByAccountsSpaceExcluding(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        DEFAULT_TENANT_ID,
                        providerId,
                        "",
                        Set.of())
        ).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Ydb.MAX_RESPONSE_ROWS, result.size());
        Assertions.assertEquals(new HashSet<>(result), new HashSet<>(accounts));
    }

    @Test
    public void getAllNonDeletedByAccountsSpaceExcludingOnePageAndOne() {
        List<AccountModel> accounts = new ArrayList<>();
        String providerId = UUID.randomUUID().toString();
        for (int i = 0; i < Ydb.MAX_RESPONSE_ROWS + 1; i++) {
            accounts.add(new AccountModel.Builder()
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setId(UUID.randomUUID().toString())
                    .setVersion(1)
                    .setDeleted(false)
                    .setDisplayName("Test")
                    .setProviderId(providerId)
                    .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                    .setOuterAccountKeyInProvider(null)
                    .setFolderId(UUID.randomUUID().toString())
                    .setLastAccountUpdate(Instant.now().truncatedTo(ChronoUnit.MICROS))
                    .setLastReceivedVersion(1L)
                    .setLatestSuccessfulAccountOperationId(UUID.randomUUID().toString())
                    .build());
        }
        ydbTableClient.usingSessionMonoRetryable(session -> accountsDao
                .upsertAllRetryable(session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), accounts)
        ).block();
        List<AccountModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.getAllNonDeletedByAccountsSpaceExcluding(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        DEFAULT_TENANT_ID,
                        providerId,
                        "",
                        Set.of())
        ).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Ydb.MAX_RESPONSE_ROWS + 1, result.size());
        Assertions.assertEquals(new HashSet<>(result), new HashSet<>(accounts));
    }

    @Test
    public void getAllNonDeletedByAccountsSpaceExcludingTwoPages() {
        List<AccountModel> accounts = new ArrayList<>();
        String providerId = UUID.randomUUID().toString();
        for (int i = 0; i < 2 * Ydb.MAX_RESPONSE_ROWS; i++) {
            accounts.add(new AccountModel.Builder()
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setId(UUID.randomUUID().toString())
                    .setVersion(1)
                    .setDeleted(false)
                    .setDisplayName("Test")
                    .setProviderId(providerId)
                    .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                    .setOuterAccountKeyInProvider(null)
                    .setFolderId(UUID.randomUUID().toString())
                    .setLastAccountUpdate(Instant.now().truncatedTo(ChronoUnit.MICROS))
                    .setLastReceivedVersion(1L)
                    .setLatestSuccessfulAccountOperationId(UUID.randomUUID().toString())
                    .build());
        }
        ydbTableClient.usingSessionMonoRetryable(session -> accountsDao
                .upsertAllRetryable(session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), accounts)
        ).block();
        List<AccountModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.getAllNonDeletedByAccountsSpaceExcluding(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        DEFAULT_TENANT_ID,
                        providerId,
                        "",
                        Set.of())
        ).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2 * Ydb.MAX_RESPONSE_ROWS, result.size());
        Assertions.assertEquals(new HashSet<>(result), new HashSet<>(accounts));
    }

    @Test
    public void getAllNonDeletedByAccountsSpaceExcludingTwoPagesAndOne() {
        List<AccountModel> accounts = new ArrayList<>();
        String providerId = UUID.randomUUID().toString();
        for (int i = 0; i < 2 * Ydb.MAX_RESPONSE_ROWS + 1; i++) {
            accounts.add(new AccountModel.Builder()
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setId(UUID.randomUUID().toString())
                    .setVersion(1)
                    .setDeleted(false)
                    .setDisplayName("Test")
                    .setProviderId(providerId)
                    .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                    .setOuterAccountKeyInProvider(null)
                    .setFolderId(UUID.randomUUID().toString())
                    .setLastAccountUpdate(Instant.now().truncatedTo(ChronoUnit.MICROS))
                    .setLastReceivedVersion(1L)
                    .setLatestSuccessfulAccountOperationId(UUID.randomUUID().toString())
                    .build());
        }
        ydbTableClient.usingSessionMonoRetryable(session -> accountsDao
                .upsertAllRetryable(session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), accounts)
        ).block();
        List<AccountModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.getAllNonDeletedByAccountsSpaceExcluding(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        DEFAULT_TENANT_ID,
                        providerId,
                        "",
                        Set.of())
        ).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2 * Ydb.MAX_RESPONSE_ROWS + 1, result.size());
        Assertions.assertEquals(new HashSet<>(result), new HashSet<>(accounts));
    }

    @Test
    public void getAllNonDeletedByAccountsSpaceExcludingTwoPagesFilter() {
        List<AccountModel> accounts = new ArrayList<>();
        String providerId = UUID.randomUUID().toString();
        for (int i = 0; i < 2 * Ydb.MAX_RESPONSE_ROWS; i++) {
            accounts.add(new AccountModel.Builder()
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setId(UUID.randomUUID().toString())
                    .setVersion(1)
                    .setDeleted(false)
                    .setDisplayName("Test")
                    .setProviderId(providerId)
                    .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                    .setOuterAccountKeyInProvider(null)
                    .setFolderId(UUID.randomUUID().toString())
                    .setLastAccountUpdate(Instant.now().truncatedTo(ChronoUnit.MICROS))
                    .setLastReceivedVersion(1L)
                    .setLatestSuccessfulAccountOperationId(UUID.randomUUID().toString())
                    .build());
        }
        ydbTableClient.usingSessionMonoRetryable(session -> accountsDao
                .upsertAllRetryable(session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), accounts)
        ).block();
        List<AccountModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.getAllNonDeletedByAccountsSpaceExcluding(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        DEFAULT_TENANT_ID,
                        providerId,
                        "",
                        Set.of(accounts.get(0).getOuterAccountIdInProvider(),
                                accounts.get((int) Ydb.MAX_RESPONSE_ROWS).getOuterAccountIdInProvider()))
        ).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2 * Ydb.MAX_RESPONSE_ROWS - 2, result.size());
    }

    @Test
    public void getAllByFoldersProvidersAccountsSpaces() {
        List<AccountModel> accounts = new ArrayList<>();
        String providerIdOne = UUID.randomUUID().toString();
        String folderIdOne = UUID.randomUUID().toString();
        String accountsSpaceIdOne = UUID.randomUUID().toString();
        String providerIdTwo = UUID.randomUUID().toString();
        String folderIdTwo = UUID.randomUUID().toString();
        String accountsSpaceIdTwo = UUID.randomUUID().toString();
        List<FolderProviderAccountsSpace> filter = Stream.of(new FolderProviderAccountsSpace(DEFAULT_TENANT_ID,
                folderIdOne, providerIdOne, accountsSpaceIdOne), new FolderProviderAccountsSpace(DEFAULT_TENANT_ID,
                folderIdTwo, providerIdTwo, accountsSpaceIdTwo)).sorted(FolderProviderAccountsSpace.COMPARATOR)
                .collect(Collectors.toList());
        for (int i = 0; i < Ydb.MAX_RESPONSE_ROWS + 1; i++) {
            accounts.add(new AccountModel.Builder()
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setId(UUID.randomUUID().toString())
                    .setVersion(1)
                    .setDeleted(false)
                    .setDisplayName("Test")
                    .setProviderId(filter.get(0).getProviderId())
                    .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                    .setOuterAccountKeyInProvider(null)
                    .setFolderId(filter.get(0).getFolderId())
                    .setAccountsSpacesId(filter.get(0).getAccountsSpaceId().orElse(null))
                    .setLastAccountUpdate(Instant.now().truncatedTo(ChronoUnit.MICROS))
                    .setLastReceivedVersion(1L)
                    .setLatestSuccessfulAccountOperationId(UUID.randomUUID().toString())
                    .build());
        }
        for (int i = 0; i < Ydb.MAX_RESPONSE_ROWS + 1; i++) {
            accounts.add(new AccountModel.Builder()
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setId(UUID.randomUUID().toString())
                    .setVersion(1)
                    .setDeleted(false)
                    .setDisplayName("Test")
                    .setProviderId(filter.get(1).getProviderId())
                    .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                    .setOuterAccountKeyInProvider(null)
                    .setFolderId(filter.get(1).getFolderId())
                    .setAccountsSpacesId(filter.get(1).getAccountsSpaceId().orElse(null))
                    .setLastAccountUpdate(Instant.now().truncatedTo(ChronoUnit.MICROS))
                    .setLastReceivedVersion(1L)
                    .setLatestSuccessfulAccountOperationId(UUID.randomUUID().toString())
                    .build());
        }
        ydbTableClient.usingSessionMonoRetryable(session -> accountsDao
                .upsertAllRetryable(session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), accounts)
        ).block();
        List<AccountModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.getAllByFoldersProvidersAccountsSpaces(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        new HashSet<>(filter), false)
        ).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2 * Ydb.MAX_RESPONSE_ROWS + 2, result.size());
    }

    @Test
    void getAllByIdsWithDeleted() {
        List<AccountModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.getAllByIdsWithDeleted(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TEST_ACCOUNT_1_ID, TEST_ACCOUNT_2_ID, TEST_ACCOUNT_3_ID),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(3, models.size());
        Assertions.assertTrue(models.contains(TEST_ACCOUNT_1));
        Assertions.assertTrue(models.contains(TEST_ACCOUNT_2));
        Assertions.assertTrue(models.contains(TEST_ACCOUNT_3));
    }

    @Test
    public void getNonDeletedServiceAccountKeysByProviderAccountsSpaces() {
        FolderModel folder = new FolderModel(UUID.randomUUID().toString(), DEFAULT_TENANT_ID, 42L, 0, "test",
                "test", false, FolderType.COMMON_DEFAULT_FOR_SERVICE, Set.of(), 0L);
        ydbTableClient.usingSessionMonoRetryable(session -> folderDao.upsertOneRetryable(
                session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), folder)).block();
        List<AccountModel> accounts = new ArrayList<>();
        List<ServiceAccountKeys> expectedResult = new ArrayList<>();
        String providerId = UUID.randomUUID().toString();
        String accountsSpaceOne = UUID.randomUUID().toString();
        String accountsSpaceTwo = UUID.randomUUID().toString();
        for (int i = 0; i < Ydb.MAX_RESPONSE_ROWS + 1; i++) {
            AccountModel account = new AccountModel.Builder()
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setId(UUID.randomUUID().toString())
                    .setVersion(1)
                    .setDeleted(false)
                    .setDisplayName("Test")
                    .setProviderId(providerId)
                    .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                    .setOuterAccountKeyInProvider(null)
                    .setFolderId(folder.getId())
                    .setLastAccountUpdate(Instant.now().truncatedTo(ChronoUnit.MICROS))
                    .setLastReceivedVersion(1L)
                    .setLatestSuccessfulAccountOperationId(UUID.randomUUID().toString())
                    .setAccountsSpacesId(accountsSpaceOne)
                    .build();
            accounts.add(account);
            expectedResult.add(new ServiceAccountKeys(account.getTenantId(),
                    account.getId(),
                    account.getProviderId(),
                    account.getOuterAccountIdInProvider(),
                    account.getOuterAccountKeyInProvider().orElse(null),
                    account.getAccountsSpacesId().orElse(null),
                    42L,
                    folder.getId()));
        }
        for (int i = 0; i < 10; i++) {
            AccountModel account = new AccountModel.Builder()
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setId(UUID.randomUUID().toString())
                    .setVersion(1)
                    .setDeleted(true)
                    .setDisplayName("Test")
                    .setProviderId(providerId)
                    .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                    .setOuterAccountKeyInProvider(null)
                    .setFolderId(folder.getId())
                    .setLastAccountUpdate(Instant.now().truncatedTo(ChronoUnit.MICROS))
                    .setLastReceivedVersion(1L)
                    .setLatestSuccessfulAccountOperationId(UUID.randomUUID().toString())
                    .setAccountsSpacesId(accountsSpaceOne)
                    .build();
            accounts.add(account);
        }
        for (int i = 0; i < Ydb.MAX_RESPONSE_ROWS + 1; i++) {
            AccountModel account = new AccountModel.Builder()
                    .setTenantId(Tenants.DEFAULT_TENANT_ID)
                    .setId(UUID.randomUUID().toString())
                    .setVersion(1)
                    .setDeleted(false)
                    .setDisplayName("Test")
                    .setProviderId(providerId)
                    .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                    .setOuterAccountKeyInProvider(null)
                    .setFolderId(folder.getId())
                    .setLastAccountUpdate(Instant.now().truncatedTo(ChronoUnit.MICROS))
                    .setLastReceivedVersion(1L)
                    .setLatestSuccessfulAccountOperationId(UUID.randomUUID().toString())
                    .setAccountsSpacesId(accountsSpaceTwo)
                    .build();
            accounts.add(account);
            expectedResult.add(new ServiceAccountKeys(account.getTenantId(),
                    account.getId(),
                    account.getProviderId(),
                    account.getOuterAccountIdInProvider(),
                    account.getOuterAccountKeyInProvider().orElse(null),
                    account.getAccountsSpacesId().orElse(null),
                    42L,
                    folder.getId()));
        }
        ydbTableClient.usingSessionMonoRetryable(session -> accountsDao
                .upsertAllRetryable(session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), accounts)
        ).block();
        List<ServiceAccountKeys> result = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.getAllNonDeletedServiceAccountKeysByProviderAccountsSpaces(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        DEFAULT_TENANT_ID,
                        providerId,
                        Set.of(accountsSpaceOne, accountsSpaceTwo))
        ).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2 * (Ydb.MAX_RESPONSE_ROWS + 1), result.size());
        Assertions.assertEquals(new HashSet<>(expectedResult), new HashSet<>(result));
    }

}
