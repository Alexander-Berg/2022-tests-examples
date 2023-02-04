package ru.yandex.intranet.d.dao.quotas;

import java.time.Duration;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import reactor.util.function.Tuple2;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao;
import ru.yandex.intranet.d.dao.accounts.ProviderReserveAccountsDao;
import ru.yandex.intranet.d.dao.folders.FolderDao;
import ru.yandex.intranet.d.datasource.impl.YdbRetry;
import ru.yandex.intranet.d.datasource.model.WithTxId;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.metrics.YdbMetrics;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderType;
import ru.yandex.intranet.d.model.quotas.QuotaAggregationModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;

import static ru.yandex.intranet.d.TestResources.YP_HDD_MAN;

/**
 * @author Nikita Minin <spasitel@yandex-team.ru>
 */
@IntegrationTest
class QuotasDaoTest {

    public static final String TEST_FOLDER_ID_3 = "test-folder-id3";
    public static final String TEST_FOLDER_ID_4 = "test-folder-id4"; //SERVICE_ID_2
    public static final String TEST_FOLDER_ID_5 = "test-folder-id5"; //SERVICE_ID_2
    public static final long SERVICE_ID = 1;
    public static final long SERVICE_ID_2 = 2;
    @Autowired
    private QuotasDao quotasDao;
    @Autowired
    private AccountsQuotasDao accountsQuotasDao;
    @Autowired
    private ProviderReserveAccountsDao providerReserveAccountsDao;
    @Autowired
    private FolderDao folderDao;
    @Autowired
    private YdbTableClient ydbTableClient;
    @Autowired
    private YdbMetrics ydbMetrics;
    @Value("${ydb.transactionRetries}")
    private long transactionRetries;

    @BeforeEach
    public void setUp() {
        ydbTableClient.usingSessionMonoRetryable(session -> quotasDao.upsertOneRetryable(
                session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE),
                new QuotaModel(
                        Tenants.DEFAULT_TENANT_ID,
                        TestFolders.TEST_FOLDER_2_ID,
                        TestProviders.YDB_ID,
                        TestResources.YDB_RAM_SAS,
                        100000002,
                        -2000022,
                        0))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        ydbTableClient.usingSessionMonoRetryable(session -> quotasDao.upsertOneRetryable(
                session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE),
                new QuotaModel(
                        Tenants.DEFAULT_TENANT_ID,
                        TEST_FOLDER_ID_3,
                        TestProviders.YDB_ID,
                        TestResources.YDB_RAM_SAS,
                        1003,
                        3,
                        0))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        ydbTableClient.usingSessionMonoRetryable(session -> quotasDao.upsertOneRetryable(
                session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE),
                new QuotaModel(
                        Tenants.DEFAULT_TENANT_ID,
                        TEST_FOLDER_ID_4,
                        TestProviders.YP_ID,
                        YP_HDD_MAN,
                        104,
                        14,
                        0))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        ydbTableClient.usingSessionMonoRetryable(session -> quotasDao.upsertOneRetryable(
                session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE),
                new QuotaModel(
                        Tenants.DEFAULT_TENANT_ID,
                        TEST_FOLDER_ID_5,
                        TestProviders.YP_ID,
                        YP_HDD_MAN,
                        10005,
                        0,
                        0))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        ydbTableClient.usingSessionMonoRetryable(session -> quotasDao.upsertOneRetryable(
                session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE),
                new QuotaModel(
                        Tenants.DEFAULT_TENANT_ID,
                        TEST_FOLDER_ID_5,
                        TestProviders.YP_ID,
                        TestResources.YP_SSD_VLA,
                        1000005,
                        1000002,
                        0))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
    }

    @Test
    public void testGetByFolders() {
        List<QuotaModel> quotasInFirstFolder = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByFolders(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TestFolders.TEST_FOLDER_1_ID), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotasInFirstFolder);
        Assertions.assertEquals(4, quotasInFirstFolder.size());
        QuotaModel quotaOne = quotasInFirstFolder.stream()
                .filter(q -> q.getResourceId().equals(YP_HDD_MAN))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(1000000000000L, quotaOne.getQuota());
        Assertions.assertEquals(800000000000L, quotaOne.getBalance());

        List<QuotaModel> quotasInSecondFolder = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByFolders(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TestFolders.TEST_FOLDER_2_ID), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotasInSecondFolder);
        Assertions.assertEquals(4, quotasInSecondFolder.size());

        List<QuotaModel> quotasInSeveralFolders = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByFolders(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TEST_FOLDER_ID_3, TEST_FOLDER_ID_4, TEST_FOLDER_ID_5), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotasInSeveralFolders);
        Assertions.assertEquals(4, quotasInSeveralFolders.size());

        List<QuotaModel> quotasInFakeFolder = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByFolders(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        List.of("fake_folder_id"), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotasInFakeFolder);
        Assertions.assertEquals(0, quotasInFakeFolder.size());
    }

    @Test
    public void testGetByFoldersNotIncludeCompletelyZeroQuotas() {
        List<QuotaModel> quotas = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByFolders(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TestFolders.TEST_FOLDER_7_ID), Tenants.DEFAULT_TENANT_ID, false)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotas);
        Assertions.assertEquals(1, quotas.size());
        QuotaModel ypHddMan = quotas.get(0);
        Assertions.assertEquals("ef333da9-b076-42f5-b7f5-84cd04ab7fcc", ypHddMan.getResourceId());
        Assertions.assertEquals(1000000000000L, ypHddMan.getQuota());
        Assertions.assertEquals(800000000000L, ypHddMan.getBalance());
    }

    @Test
    public void testGetByFoldersAndProvider() {
        List<QuotaModel> quota1 = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByFoldersAndProvider(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TestFolders.TEST_FOLDER_1_ID), Tenants.DEFAULT_TENANT_ID, TestProviders.YP_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quota1);
        Assertions.assertEquals(4, quota1.size());
        QuotaModel quotaOne = quota1.stream()
                .filter(q -> q.getResourceId().equals(YP_HDD_MAN))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(1000000000000L, quotaOne.getQuota());
        Assertions.assertEquals(800000000000L, quotaOne.getBalance());

        List<QuotaModel> quotaWithWrongProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByFoldersAndProvider(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TestFolders.TEST_FOLDER_1_ID), Tenants.DEFAULT_TENANT_ID, TestProviders.YDB_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotaWithWrongProvider);
        Assertions.assertEquals(quotaWithWrongProvider.size(), 0);

        List<QuotaModel> quotasWithFilterByProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByFoldersAndProvider(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TestFolders.TEST_FOLDER_2_ID, TEST_FOLDER_ID_3, TEST_FOLDER_ID_4, TEST_FOLDER_ID_5),
                        Tenants.DEFAULT_TENANT_ID, TestProviders.YDB_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotasWithFilterByProvider);
        Assertions.assertEquals(quotasWithFilterByProvider.size(), 2);
    }

    @Test
    public void testGetByFoldersAndProviderNotIncludeCompletelyZeroQuotas() {
        List<QuotaModel> quotas = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByFoldersAndProvider(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TestFolders.TEST_FOLDER_7_ID), Tenants.DEFAULT_TENANT_ID, TestProviders.YP_ID, false)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotas);
        Assertions.assertEquals(1, quotas.size());
        QuotaModel ypHddMan = quotas.get(0);
        Assertions.assertEquals("ef333da9-b076-42f5-b7f5-84cd04ab7fcc", ypHddMan.getResourceId());
        Assertions.assertEquals(1000000000000L, ypHddMan.getQuota());
        Assertions.assertEquals(800000000000L, ypHddMan.getBalance());
    }

    @Test
    public void testGetByServiceAndProviderAndResource() {
        Tuple2<List<FolderModel>, List<QuotaModel>> quotasAndFolders =
                ydbTableClient.usingSessionMonoRetryable(session -> quotasDao.getByServiceAndProviderAndResource(
                        session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        SERVICE_ID, TestProviders.YP_ID, YP_HDD_MAN, Tenants.DEFAULT_TENANT_ID, 10, null)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        Assertions.assertNotNull(quotasAndFolders);
        Assertions.assertEquals(2, quotasAndFolders.getT1().size());
        Assertions.assertEquals(2, quotasAndFolders.getT2().size());

        Tuple2<List<FolderModel>, List<QuotaModel>> quotasAndFoldersPage2 =
                ydbTableClient.usingSessionMonoRetryable(session ->
                        quotasDao.getByServiceAndProviderAndResource(
                                session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                                SERVICE_ID, TestProviders.YP_ID, YP_HDD_MAN,
                                Tenants.DEFAULT_TENANT_ID, 10, quotasAndFolders.getT1().get(0).getId())
                                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics)))
                        .block();

        Assertions.assertNotNull(quotasAndFoldersPage2);
        Assertions.assertEquals(1, quotasAndFoldersPage2.getT1().size());
        Assertions.assertEquals(1, quotasAndFoldersPage2.getT2().size());

        Tuple2<List<FolderModel>, List<QuotaModel>> quotasAndFolders2 =
                ydbTableClient.usingSessionMonoRetryable(session -> quotasDao.getByServiceAndProviderAndResource(
                        session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        SERVICE_ID, TestProviders.YP_ID, TestResources.YP_SSD_VLA, Tenants.DEFAULT_TENANT_ID, 5, null)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        Assertions.assertNotNull(quotasAndFolders2);
        Assertions.assertEquals(1, quotasAndFolders2.getT1().size());
        Assertions.assertEquals(1, quotasAndFolders2.getT2().size());
        QuotaModel quotaOne = quotasAndFolders2.getT2().get(0);
        Assertions.assertEquals(1002, quotaOne.getQuota());
        Assertions.assertEquals(-202, quotaOne.getBalance());

    }

    @Test
    public void testRemoveQuota() {
        List<QuotaModel> quotas = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByFolders(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TestFolders.TEST_FOLDER_2_ID), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotas);
        Assertions.assertEquals(4, quotas.size());
        Assertions.assertEquals(TestFolders.TEST_FOLDER_2_ID, quotas.get(0).getFolderId());
        Assertions.assertEquals(TestFolders.TEST_FOLDER_2_ID, quotas.get(1).getFolderId());

        ydbTableClient.usingSessionMonoRetryable(session -> quotasDao.deleteQuotaRetryable(
                session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE),
                TestFolders.TEST_FOLDER_2_ID,
                quotas.get(0).getProviderId(),
                quotas.get(0).getResourceId(),
                Tenants.DEFAULT_TENANT_ID)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        List<QuotaModel> quotas2 = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByFolders(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TestFolders.TEST_FOLDER_2_ID), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotas2);
        Assertions.assertEquals(3, quotas2.size());

        ydbTableClient.usingSessionMonoRetryable(session -> quotasDao.deleteQuotaRetryable(
                session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE),
                TestFolders.TEST_FOLDER_2_ID,
                quotas2.get(0).getProviderId(),
                quotas2.get(0).getResourceId(),
                Tenants.DEFAULT_TENANT_ID)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        List<QuotaModel> quotas3 = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByFolders(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TestFolders.TEST_FOLDER_2_ID), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotas3);
        Assertions.assertEquals(2, quotas3.size());

        ydbTableClient.usingSessionMonoRetryable(session -> quotasDao.deleteQuotaRetryable(
                session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE),
                TestFolders.TEST_FOLDER_2_ID,
                quotas3.get(0).getProviderId(),
                quotas3.get(0).getResourceId(),
                Tenants.DEFAULT_TENANT_ID)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        List<QuotaModel> quotas4 = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByFolders(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TestFolders.TEST_FOLDER_2_ID), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotas4);
        Assertions.assertEquals(1, quotas4.size());

        ydbTableClient.usingSessionMonoRetryable(session -> quotasDao.deleteQuotaRetryable(
                session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE),
                TestFolders.TEST_FOLDER_2_ID,
                quotas4.get(0).getProviderId(),
                quotas4.get(0).getResourceId(),
                Tenants.DEFAULT_TENANT_ID)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        List<QuotaModel> quotas5 = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByFolders(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TestFolders.TEST_FOLDER_2_ID), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotas5);
        Assertions.assertEquals(0, quotas5.size());
    }

    @Test
    public void testGetAllByResourceIds() {
        int count = 1100;
        List<QuotaModel> quotasToUpsert = new ArrayList<>(count);
        for (int i = 0; i < count / 2; i++) {
            quotasToUpsert.add(new QuotaModel(
                    Tenants.DEFAULT_TENANT_ID,
                    UUID.randomUUID().toString(),
                    TestProviders.YP_ID,
                    TestResources.YP_SSD_VLA,
                    0,
                    0,
                    0));
            quotasToUpsert.add(new QuotaModel(
                    Tenants.DEFAULT_TENANT_ID,
                    UUID.randomUUID().toString(),
                    TestProviders.YP_ID,
                    TestResources.YP_SSD_MAN,
                    0,
                    0,
                    0));
        }

        WithTxId<List<QuotaModel>> quotaModelsBeforeUpsert = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getAllByResourceIds(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID, List.of(TestResources.YP_SSD_VLA, TestResources.YP_SSD_MAN)
                ).retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotaModelsBeforeUpsert);
        Assertions.assertNotNull(quotaModelsBeforeUpsert.get());
        count += quotaModelsBeforeUpsert.get().size();

        ydbTableClient.usingSessionMonoRetryable(session -> quotasDao.upsertAllRetryable(
                session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE), quotasToUpsert)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        WithTxId<List<QuotaModel>> quotaModels = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getAllByResourceIds(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID, List.of(TestResources.YP_SSD_VLA, TestResources.YP_SSD_MAN)
                ).retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotaModels);
        Assertions.assertNotNull(quotaModels.get());
        Assertions.assertEquals(count, quotaModels.get().size());
    }

    @Test
    public void testGetOneQuota() {
        Optional<QuotaModel> quota = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getOneQuota(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        TestFolders.TEST_FOLDER_2_ID,
                        TestProviders.YDB_ID,
                        TestResources.YDB_RAM_SAS,
                        Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quota);
        Assertions.assertTrue(quota.isPresent());
        Assertions.assertEquals(100000002, quota.get().getQuota());
        Assertions.assertEquals(-2000022, quota.get().getBalance());

        Optional<QuotaModel> quotaForFakeResource = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getOneQuota(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        TestFolders.TEST_FOLDER_2_ID,
                        TestProviders.YDB_ID,
                        "fake-resource-id",
                        Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        Assertions.assertNotNull(quotaForFakeResource);
        Assertions.assertTrue(quotaForFakeResource.isEmpty());
    }

    @Test
    public void testGetById() {
        Optional<QuotaModel> quota = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getById(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        new QuotaModel.Key(TestFolders.TEST_FOLDER_2_ID, TestProviders.YDB_ID,
                                TestResources.YDB_RAM_SAS),
                        Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quota);
        Assertions.assertTrue(quota.isPresent());
        Assertions.assertEquals(100000002, quota.get().getQuota());
        Assertions.assertEquals(-2000022, quota.get().getBalance());

        Optional<QuotaModel> quotaForFakeResource = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getById(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        new QuotaModel.Key(TestFolders.TEST_FOLDER_2_ID, TestProviders.YDB_ID,
                                "fake-resource-id"),
                        Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        Assertions.assertNotNull(quotaForFakeResource);
        Assertions.assertTrue(quotaForFakeResource.isEmpty());
    }

    @Test
    public void testGetByIds() {
        List<QuotaModel> quota = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByIds(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        List.of(new QuotaModel.Key(TestFolders.TEST_FOLDER_2_ID, TestProviders.YDB_ID,
                                        TestResources.YDB_RAM_SAS),
                                new QuotaModel.Key(TEST_FOLDER_ID_3, TestProviders.YDB_ID,
                                        TestResources.YDB_RAM_SAS),
                                new QuotaModel.Key(TestFolders.TEST_FOLDER_2_ID, TestProviders.YDB_ID,
                                        "fake-resource-id")),
                        Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quota);
        Assertions.assertEquals(2, quota.size());
        Assertions.assertTrue(quota.stream().anyMatch(q -> q.getFolderId().equals(TestFolders.TEST_FOLDER_2_ID)
                && q.getBalance() == -2000022));
        Assertions.assertTrue(quota.stream().anyMatch(q -> q.getFolderId().equals(TEST_FOLDER_ID_3)
                && q.getBalance() == 3));
    }

    @Test
    public void testUpsertMany() {
        List<QuotaModel> quotasUpsert = List.of(
                new QuotaModel(
                        Tenants.DEFAULT_TENANT_ID,
                        TestFolders.TEST_FOLDER_4_ID,
                        TestProviders.YP_ID,
                        TestResources.YP_CPU_SAS,
                        100,
                        50,
                        0),
                new QuotaModel(
                        Tenants.DEFAULT_TENANT_ID,
                        TestFolders.TEST_FOLDER_4_ID,
                        TestProviders.YP_ID,
                        TestResources.YP_CPU_MAN,
                        200,
                        150,
                        0));

        ydbTableClient.usingSessionMonoRetryable(session -> quotasDao.upsertAllRetryable(
                session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE), quotasUpsert)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        List<QuotaModel> quotasGet = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByIds(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        quotasUpsert.stream().map(QuotaModel::toKey).collect(Collectors.toList()),
                        Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertEquals(quotasUpsert, quotasGet);
    }

    @Test
    public void testGetByServiceAndProvider() {
        Tuple2<List<FolderModel>, List<QuotaModel>> quotas = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByServiceAndProvider(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        SERVICE_ID, TestProviders.YP_ID, Tenants.DEFAULT_TENANT_ID, 5, null)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        Assertions.assertNotNull(quotas);
        Assertions.assertEquals(11, quotas.getT2().size());

        Tuple2<List<FolderModel>, List<QuotaModel>> quotas2 = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByServiceAndProvider(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        SERVICE_ID, TestProviders.YDB_ID, Tenants.DEFAULT_TENANT_ID, 5, null)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        Assertions.assertNotNull(quotas2);
        Assertions.assertEquals(1, quotas2.getT2().size());
        QuotaModel quotaOne = quotas2.getT2().get(0);
        Assertions.assertEquals(100000002, quotaOne.getQuota());
        Assertions.assertEquals(-2000022, quotaOne.getBalance());
    }

    @Test
    public void testGetByServiceAndProviderWithPage() {
        Tuple2<List<FolderModel>, List<QuotaModel>> quotasAndFolders =
                ydbTableClient.usingSessionMonoRetryable(session ->
                        quotasDao.getByServiceAndProvider(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                                SERVICE_ID, TestProviders.YP_ID, Tenants.DEFAULT_TENANT_ID, 1, null)
                                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics)))
                        .block();

        Assertions.assertNotNull(quotasAndFolders);
        Assertions.assertEquals(1, quotasAndFolders.getT1().size());
        Assertions.assertFalse(quotasAndFolders.getT2().isEmpty());

        int totalQuotas = quotasAndFolders.getT2().size();

        Tuple2<List<FolderModel>, List<QuotaModel>> quotasAndFolders2 =
                ydbTableClient.usingSessionMonoRetryable(session ->
                        quotasDao.getByServiceAndProvider(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                                SERVICE_ID,
                                TestProviders.YP_ID,
                                Tenants.DEFAULT_TENANT_ID,
                                3,
                                quotasAndFolders.getT2().get(quotasAndFolders.getT2().size() - 1).getFolderId())
                                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics)))
                        .block();

        Assertions.assertNotNull(quotasAndFolders2);
        Assertions.assertEquals(2, quotasAndFolders2.getT1().size());
        Assertions.assertFalse(quotasAndFolders2.getT2().isEmpty());

        totalQuotas += quotasAndFolders2.getT2().size();

        Tuple2<List<FolderModel>, List<QuotaModel>> quotasAndFolders3 =
                ydbTableClient.usingSessionMonoRetryable(session ->
                        quotasDao.getByServiceAndProvider(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                                SERVICE_ID,
                                TestProviders.YP_ID,
                                Tenants.DEFAULT_TENANT_ID,
                                1,
                                quotasAndFolders2.getT2().get(quotasAndFolders2.getT2().size() - 1).getFolderId())
                                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics)))
                        .block();

        Assertions.assertNotNull(quotasAndFolders3);
        Assertions.assertEquals(0, quotasAndFolders3.getT2().size());
        Assertions.assertEquals(0, quotasAndFolders3.getT1().size());
        Assertions.assertEquals(11, totalQuotas);

    }

    @Test
    public void testGetManyQuotasByFolders() {
        final String providerId = "provider-id-many";
        for (int i = 0;
             i < 2500;
             i++) {
            int finalI = i;
            ydbTableClient.usingSessionMonoRetryable(session -> quotasDao.upsertOneRetryable(
                    session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE),
                    new QuotaModel(
                            Tenants.DEFAULT_TENANT_ID,
                            TestFolders.TEST_FOLDER_1_ID,
                            providerId,
                            "resource-id-" + finalI,
                            10000 + finalI,
                            100 + finalI,
                            0))
                    .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        }

        List<QuotaModel> quotas = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByFoldersAndProvider(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TestFolders.TEST_FOLDER_1_ID), Tenants.DEFAULT_TENANT_ID, providerId)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        Assertions.assertNotNull(quotas);
        Assertions.assertEquals(2500, quotas.size());
        for (int i = 0;
             i < 2499;
             i++) {
            Assertions.assertTrue(quotas.get(i).getResourceId().compareTo(quotas.get(i + 1).getResourceId()) < 0);

        }
    }

    @Test
    public void testGetByProviderFoldersResources() {
        ydbTableClient.usingSessionMonoRetryable(session -> quotasDao.upsertAllRetryable(
                session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE),
                List.of(new QuotaModel(
                        Tenants.DEFAULT_TENANT_ID,
                        "1",
                        TestProviders.YDB_ID,
                        "1",
                        0,
                        0,
                        0),
                        new QuotaModel(
                        Tenants.DEFAULT_TENANT_ID,
                        "1",
                        TestProviders.YDB_ID,
                        "2",
                        0,
                        0,
                        0),
                        new QuotaModel(
                        Tenants.DEFAULT_TENANT_ID,
                        "1",
                        TestProviders.YDB_ID,
                        "3",
                        0,
                        0,
                        0),
                        new QuotaModel(
                        Tenants.DEFAULT_TENANT_ID,
                        "2",
                        TestProviders.YDB_ID,
                        "1",
                        0,
                        0,
                        0),
                        new QuotaModel(
                        Tenants.DEFAULT_TENANT_ID,
                        "3",
                        TestProviders.YDB_ID,
                        "1",
                        0,
                        0,
                        0),
                        new QuotaModel(
                        Tenants.DEFAULT_TENANT_ID,
                        "3",
                        TestProviders.YDB_ID,
                        "2",
                        0,
                        0,
                        0)))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<QuotaModel> quotas = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByProviderFoldersResources(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID, Set.of("1", "2", "3"), TestProviders.YDB_ID, Set.of("1", "2", "3"),
                        1).retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(quotas);
        Assertions.assertEquals(6, quotas.size());
    }

    @Test
    public void scanQuotasAggregationSubset() {
        List<FolderModel> folders = List.of(
                new FolderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                42L, 0, "test", "test", false, FolderType.COMMON_DEFAULT_FOR_SERVICE, Set.of(), 0),
                new FolderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                        69L, 0, "test", "test", false, FolderType.COMMON_DEFAULT_FOR_SERVICE, Set.of(), 0));
        ydbTableClient.usingSessionMonoRetryable(session -> folderDao.upsertAllRetryable(
                        session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE), folders)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        String providerId = UUID.randomUUID().toString();
        String resourceIdOne = UUID.randomUUID().toString();
        String resourceIdTwo = UUID.randomUUID().toString();
        List<QuotaModel> quotas = List.of(
                new QuotaModel(Tenants.DEFAULT_TENANT_ID, folders.get(0).getId(), providerId,
                    resourceIdOne, 10L, 5L, 0L),
                new QuotaModel(Tenants.DEFAULT_TENANT_ID, folders.get(1).getId(),
                    providerId, resourceIdTwo, 20L, 10L, 0L));
        ydbTableClient.usingSessionMonoRetryable(session -> quotasDao.upsertAllRetryable(
                        session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE), quotas)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<AccountsQuotasModel> provisions = List.of(
                new AccountsQuotasModel.Builder()
                        .setTenantId(Tenants.DEFAULT_TENANT_ID)
                        .setAccountId(UUID.randomUUID().toString())
                        .setResourceId(resourceIdOne)
                        .setProvidedQuota(5L)
                        .setAllocatedQuota(3L)
                        .setFolderId(folders.get(0).getId())
                        .setProviderId(providerId)
                        .setLastProvisionUpdate(Instant.now())
                        .setFrozenProvidedQuota(0L)
                        .build(),
                new AccountsQuotasModel.Builder()
                        .setTenantId(Tenants.DEFAULT_TENANT_ID)
                        .setAccountId(UUID.randomUUID().toString())
                        .setResourceId(resourceIdTwo)
                        .setProvidedQuota(10L)
                        .setAllocatedQuota(7L)
                        .setFolderId(folders.get(1).getId())
                        .setProviderId(providerId)
                        .setLastProvisionUpdate(Instant.now())
                        .setFrozenProvidedQuota(0L)
                        .build());
        ydbTableClient.usingSessionMonoRetryable(session -> accountsQuotasDao.upsertAllRetryable(
                        session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE), provisions)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        List<QuotaAggregationModel> result = ydbTableClient.usingSessionMonoRetryable(session -> quotasDao
                .scanQuotasAggregationSubset(session, Tenants.DEFAULT_TENANT_ID, providerId,
                        Set.of(resourceIdOne, resourceIdTwo), Duration.ofMinutes(5L))).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(4, result.size());
    }

    @Test
    public void getForAggregationByProviderAndResources() {
        String providerId = UUID.randomUUID().toString();
        String resourceIdOne = UUID.randomUUID().toString();
        String resourceIdTwo = UUID.randomUUID().toString();
        List<QuotaModel> quotas = new ArrayList<>();
        List<FolderModel> folders = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            String folderId = UUID.randomUUID().toString();
            folders.add(new FolderModel(folderId, Tenants.DEFAULT_TENANT_ID,
                    42L, 0, "test", "test", false,
                    FolderType.COMMON_DEFAULT_FOR_SERVICE, Set.of(), 0));
            quotas.add(new QuotaModel(Tenants.DEFAULT_TENANT_ID, folderId, providerId,
                    resourceIdOne, 10L, 5L, 0L));
        }
        for (int i = 0; i < 2000; i++) {
            String folderId = UUID.randomUUID().toString();
            folders.add(new FolderModel(folderId, Tenants.DEFAULT_TENANT_ID,
                    42L, 0, "test", "test", false,
                    FolderType.COMMON_DEFAULT_FOR_SERVICE, Set.of(), 0));
            quotas.add(new QuotaModel(Tenants.DEFAULT_TENANT_ID, folderId, providerId,
                    resourceIdTwo, 10L, 5L, 0L));
        }
        ydbTableClient.usingSessionMonoRetryable(session -> folderDao.upsertAllRetryable(
                        session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE), folders)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        ydbTableClient.usingSessionMonoRetryable(session -> quotasDao.upsertAllRetryable(
                        session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE), quotas)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<QuotaAggregationModel> result = ydbTableClient.usingSessionMonoRetryable(session -> quotasDao
                .getForAggregationByProviderAndResources(session.asTxCommit(TransactionMode.STALE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID, providerId, List.of(resourceIdOne, resourceIdTwo))).block();
        Assertions.assertEquals(quotas.stream().map(m -> new QuotaAggregationModel(m.getResourceId(),
                        m.getQuota(), m.getBalance(), null, null, 42L,
                        m.getFolderId(), null, FolderType.COMMON_DEFAULT_FOR_SERVICE))
                        .collect(Collectors.toSet()), new HashSet<>(result));
    }

    @Test
    public void getForAggregationByProvider() {
        String providerId = UUID.randomUUID().toString();
        String resourceIdOne = UUID.randomUUID().toString();
        String resourceIdTwo = UUID.randomUUID().toString();
        List<QuotaModel> quotas = new ArrayList<>();
        List<FolderModel> folders = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            String folderId = UUID.randomUUID().toString();
            folders.add(new FolderModel(folderId, Tenants.DEFAULT_TENANT_ID,
                    42L, 0, "test", "test", false,
                    FolderType.COMMON_DEFAULT_FOR_SERVICE, Set.of(), 0));
            quotas.add(new QuotaModel(Tenants.DEFAULT_TENANT_ID, folderId, providerId,
                    resourceIdOne, 10L, 5L, 0L));
        }
        for (int i = 0; i < 2000; i++) {
            String folderId = UUID.randomUUID().toString();
            folders.add(new FolderModel(folderId, Tenants.DEFAULT_TENANT_ID,
                    42L, 0, "test", "test", false,
                    FolderType.COMMON_DEFAULT_FOR_SERVICE, Set.of(), 0));
            quotas.add(new QuotaModel(Tenants.DEFAULT_TENANT_ID, folderId, providerId,
                    resourceIdTwo, 10L, 5L, 0L));
        }
        ydbTableClient.usingSessionMonoRetryable(session -> folderDao.upsertAllRetryable(
                        session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE), folders)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        ydbTableClient.usingSessionMonoRetryable(session -> quotasDao.upsertAllRetryable(
                        session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE), quotas)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<QuotaAggregationModel> result = ydbTableClient.usingSessionMonoRetryable(session -> quotasDao
                .getForAggregationByProvider(session.asTxCommit(TransactionMode.STALE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID, providerId)).block();
        Assertions.assertEquals(quotas.stream().map(m -> new QuotaAggregationModel(m.getResourceId(),
                        m.getQuota(), m.getBalance(), null, null, 42L,
                        m.getFolderId(), null, FolderType.COMMON_DEFAULT_FOR_SERVICE))
                        .collect(Collectors.toSet()), new HashSet<>(result));
    }

}
