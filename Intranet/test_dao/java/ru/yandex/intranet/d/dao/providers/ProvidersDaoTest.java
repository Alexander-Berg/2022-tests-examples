package ru.yandex.intranet.d.dao.providers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.datasource.impl.YdbRetry;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.metrics.YdbMetrics;
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel;
import ru.yandex.intranet.d.model.providers.AggregationAlgorithm;
import ru.yandex.intranet.d.model.providers.AggregationQuotaQueryType;
import ru.yandex.intranet.d.model.providers.AggregationSettings;
import ru.yandex.intranet.d.model.providers.BillingMeta;
import ru.yandex.intranet.d.model.providers.FreeProvisionAggregationMode;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.model.providers.RelatedCoefficient;
import ru.yandex.intranet.d.model.providers.RelatedResourceMapping;
import ru.yandex.intranet.d.model.providers.UsageMode;

import static java.util.Collections.emptySet;

/**
 * Providers DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ProvidersDaoTest {

    @Autowired
    private ProvidersDao providersDao;
    @Autowired
    private YdbTableClient ydbTableClient;
    @Autowired
    private YdbMetrics ydbMetrics;
    @Value("${ydb.transactionRetries}")
    private long transactionRetries;
    private static final long DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE = 1000L;

    @Test
    public void testGetById() {
        Optional<ProviderModel> ypProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TestProviders.YP_ID, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(ypProvider);
        Assertions.assertTrue(ypProvider.isPresent());
    }

    @Test
    public void testGetByIdMissing() {
        Optional<ProviderModel> ypProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(ypProvider);
        Assertions.assertFalse(ypProvider.isPresent());
    }

    @Test
    public void testGetBySourceTvmId() {
        List<ProviderModel> ypProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TestProviders.YP_SOURCE_TVM_ID, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(ypProvider);
        Assertions.assertFalse(ypProvider.isEmpty());
    }

    @Test
    public void testGetBySourceTvmIdMissing() {
        List<ProviderModel> ypProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        -1L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(ypProvider);
        Assertions.assertTrue(ypProvider.isEmpty());
    }

    @Test
    public void testGetBySourceTvmIdImmediate() {
        List<ProviderModel> ypProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.STALE_READ_ONLY),
                        TestProviders.YP_SOURCE_TVM_ID, Tenants.DEFAULT_TENANT_ID)).block();
        Assertions.assertNotNull(ypProvider);
        Assertions.assertFalse(ypProvider.isEmpty());
    }

    @Test
    public void testGetBySourceTvmIdMissingImmediate() {
        List<ProviderModel> ypProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.STALE_READ_ONLY),
                        -1L, Tenants.DEFAULT_TENANT_ID)).block();
        Assertions.assertNotNull(ypProvider);
        Assertions.assertTrue(ypProvider.isEmpty());
    }

    @Test
    public void testGetByIds() {
        List<ProviderModel> oneProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getByIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(TestProviders.YP_ID, Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> twoProviders = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getByIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(TestProviders.YP_ID, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TestProviders.YDB_ID, Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(oneProvider);
        Assertions.assertNotNull(twoProviders);
        Assertions.assertEquals(1, oneProvider.size());
        Assertions.assertEquals(2, twoProviders.size());
    }

    @Test
    public void testGetByIdsMissing() {
        List<ProviderModel> noProviders = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getByIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(noProviders);
        Assertions.assertTrue(noProviders.isEmpty());
    }

    @Test
    public void testGetBySourceTvmIds() {
        List<ProviderModel> oneProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(TestProviders.YP_SOURCE_TVM_ID, Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> twoProviders = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(TestProviders.YP_SOURCE_TVM_ID, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TestProviders.YDB_SOURCE_TVM_ID, Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(oneProvider);
        Assertions.assertNotNull(twoProviders);
        Assertions.assertEquals(1, oneProvider.size());
        Assertions.assertEquals(2, twoProviders.size());
    }

    @Test
    public void testGetBySourceTvmIdsMissing() {
        List<ProviderModel> noProviders = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(-1L, Tenants.DEFAULT_TENANT_ID)))
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(noProviders);
        Assertions.assertTrue(noProviders.isEmpty());
    }

    @Test
    public void testGetBySourceTvmIdsImmediate() {
        List<ProviderModel> oneProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmIds(session.asTxCommitRetryable(TransactionMode.STALE_READ_ONLY),
                        List.of(Tuples.of(TestProviders.YP_SOURCE_TVM_ID, Tenants.DEFAULT_TENANT_ID)))).block();
        List<ProviderModel> twoProviders = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(Tuples.of(TestProviders.YP_SOURCE_TVM_ID, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(TestProviders.YDB_SOURCE_TVM_ID, Tenants.DEFAULT_TENANT_ID)))).block();
        Assertions.assertNotNull(oneProvider);
        Assertions.assertNotNull(twoProviders);
        Assertions.assertEquals(1, oneProvider.size());
        Assertions.assertEquals(2, twoProviders.size());
    }

    @Test
    public void testGetBySourceTvmIdsMissingImmediate() {
        List<ProviderModel> noProviders = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmIds(session.asTxCommitRetryable(TransactionMode.STALE_READ_ONLY),
                        List.of(Tuples.of(-1L, Tenants.DEFAULT_TENANT_ID)))).block();
        Assertions.assertNotNull(noProviders);
        Assertions.assertTrue(noProviders.isEmpty());
    }

    @Test
    public void testUpsertProvider() {
        ProviderModel provider = new ProviderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                0L, "Test-1", "Тест-1", "Test description-1", "Тестовое описание-1",
                null, null, 999L, 998L, 1L, false, false, false, false, true, "test",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                Map.of("key", RelatedResourceMapping.builder()
                        .relatedCoefficientMap(Map.of(
                                "key2", RelatedCoefficient.builder().numerator(1L).denominator(2L).build(),
                                "key3", RelatedCoefficient.builder().numerator(1L).denominator(1L).build()
                        ))
                        .build()), 1L, null, false, false, null, null, null);
        ydbTableClient.usingSessionMonoRetryable(session -> providersDao.upsertProviderRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), provider)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<ProviderModel> newProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        provider.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> newProviderBySourceTvmId = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        999L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newProvider);
        Assertions.assertTrue(newProvider.isPresent());
        Assertions.assertEquals(provider, newProvider.get());
        Assertions.assertNotNull(newProviderBySourceTvmId);
        Assertions.assertFalse(newProviderBySourceTvmId.isEmpty());
        Assertions.assertEquals(provider, newProviderBySourceTvmId.get(0));
    }

    @Test
    public void testUpsertProviderFull() {
        ProviderModel provider = new ProviderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                0L, "Test-1", "Тест-1", "Test description-1", "Тестовое описание-1",
                "https://test.yandex-team.ru", "grpc.yandex-team.ru",
                999L, 998L, 9999L, false, false, false, false, true, "test",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), true),
                true, true, true, true, new BillingMeta(null),
                Map.of("key", RelatedResourceMapping.builder()
                        .relatedCoefficientMap(Map.of(
                                "key2", RelatedCoefficient.builder().numerator(1L).denominator(2L).build(),
                                "key3", RelatedCoefficient.builder().numerator(1L).denominator(1L).build()
                        ))
                        .build()), 1L, null, false, false,
                new AggregationSettings(FreeProvisionAggregationMode.NONE, UsageMode.UNDEFINED, 300L),
                new AggregationAlgorithm(false, 1000L, AggregationQuotaQueryType.PAGINATE, false, true, 3L, true,
                        false), null);
        ydbTableClient.usingSessionMonoRetryable(session -> providersDao.upsertProviderRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), provider)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<ProviderModel> newProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        provider.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> newProviderBySourceTvmId = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        999L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newProvider);
        Assertions.assertTrue(newProvider.isPresent());
        Assertions.assertEquals(provider, newProvider.get());
        Assertions.assertNotNull(newProviderBySourceTvmId);
        Assertions.assertFalse(newProviderBySourceTvmId.isEmpty());
        Assertions.assertEquals(provider, newProviderBySourceTvmId.get(0));
    }

    @Test
    public void testUpsertProviderMinimal() {
        ProviderModel provider = new ProviderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                0L, "Test-1", "Тест-1", "Test description-1", "Тестовое описание-1",
                null, null, 1L, 2L, 1L, false, false, false, false, true, "test",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                null, 1L, null, false, false, null, null, null);
        ydbTableClient.usingSessionMonoRetryable(session -> providersDao.upsertProviderRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), provider)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<ProviderModel> newProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        provider.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newProvider);
        Assertions.assertTrue(newProvider.isPresent());
        Assertions.assertEquals(provider, newProvider.get());
    }

    @Test
    public void testUpsertProviders() {
        ProviderModel providerOne = new ProviderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                0L, "Test-1", "Тест-1", "Test description-1", "Тестовое описание-1",
                null, null, 999L, 998L, 1L, false, false, false, false, true, "test-1",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                Map.of("key", RelatedResourceMapping.builder()
                        .relatedCoefficientMap(Map.of(
                                "key2", RelatedCoefficient.builder().numerator(1L).denominator(2L).build(),
                                "key3", RelatedCoefficient.builder().numerator(1L).denominator(1L).build()
                        ))
                        .build()), 1L, null, false, false,
                new AggregationSettings(FreeProvisionAggregationMode.NONE, UsageMode.UNDEFINED, null),
                new AggregationAlgorithm(false, 1000L, AggregationQuotaQueryType.PAGINATE, false, true, 3L, true,
                        false), null);
        ProviderModel providerTwo = new ProviderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                0L, "Test-2", "Тест-2", "Test description-2", "Тестовое описание-2",
                null, null, 9999L, 9998L, 1L, false, false, false, false, true, "test-2",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                null, 1L, null, false, false,
                new AggregationSettings(FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE, UsageMode.TIME_SERIES,
                        300L),
                new AggregationAlgorithm(true, 1000L, AggregationQuotaQueryType.PAGINATE, true, false, 3L, true,
                        false), null);
        ydbTableClient.usingSessionMonoRetryable(session -> providersDao.upsertProvidersRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), List.of(providerOne, providerTwo))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<ProviderModel> newProviderOne = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        providerOne.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> newProviderOneBySourceTvmId = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        999L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<ProviderModel> newProviderTwo = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        providerTwo.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> newProviderTwoBySourceTvmId = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        9999L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newProviderOne);
        Assertions.assertTrue(newProviderOne.isPresent());
        Assertions.assertEquals(providerOne, newProviderOne.get());
        Assertions.assertNotNull(newProviderOneBySourceTvmId);
        Assertions.assertFalse(newProviderOneBySourceTvmId.isEmpty());
        Assertions.assertEquals(providerOne, newProviderOneBySourceTvmId.get(0));
        Assertions.assertNotNull(newProviderTwo);
        Assertions.assertTrue(newProviderTwo.isPresent());
        Assertions.assertEquals(providerTwo, newProviderTwo.get());
        Assertions.assertNotNull(newProviderTwoBySourceTvmId);
        Assertions.assertFalse(newProviderTwoBySourceTvmId.isEmpty());
        Assertions.assertEquals(providerTwo, newProviderTwoBySourceTvmId.get(0));
    }

    @Test
    public void testUpsertProvidersFull() {
        ProviderModel providerOne = new ProviderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                0L, "Test-1", "Тест-1", "Test description-1", "Тестовое описание-1",
                "https://test-1.yandex-team.ru", "grpc-1.yandex-team.ru",
                999L, 998L, 9999L, false, false, false, false, true, "test-1",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                Map.of("key", RelatedResourceMapping.builder()
                        .relatedCoefficientMap(Map.of(
                                "key2", RelatedCoefficient.builder().numerator(1L).denominator(2L).build(),
                                "key3", RelatedCoefficient.builder().numerator(1L).denominator(1L).build()
                        ))
                        .build()), 1L, null, false, false,
                new AggregationSettings(FreeProvisionAggregationMode.NONE, UsageMode.UNDEFINED, 300L),
                new AggregationAlgorithm(false, 1000L, AggregationQuotaQueryType.PAGINATE, false, true, 3L, true,
                        false), null);
        ProviderModel providerTwo = new ProviderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                0L, "Test-2", "Тест-2", "Test description-2", "Тестовое описание-2",
                "https://test-2.yandex-team.ru", "grpc-2.yandex-team.ru",
                9999L, 9998L, 99999L, false, false, false, false, true, "test-2",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                Map.of("key", RelatedResourceMapping.builder()
                                .relatedCoefficientMap(Map.of(
                                        "key4", RelatedCoefficient.builder().numerator(3L).denominator(4L).build(),
                                        "key5", RelatedCoefficient.builder().numerator(5L).denominator(6L).build()
                                ))
                                .build(),
                        "key6", RelatedResourceMapping.builder()
                                .relatedCoefficientMap(Map.of(
                                        "key7", RelatedCoefficient.builder().numerator(7L).denominator(8L).build(),
                                        "key8", RelatedCoefficient.builder().numerator(9L).denominator(10L).build()
                                ))
                                .build()), 1L, null, false, false,
                new AggregationSettings(FreeProvisionAggregationMode.NONE, UsageMode.UNDEFINED, 300L),
                new AggregationAlgorithm(false, 1000L, AggregationQuotaQueryType.PAGINATE, false, true, 3L, true,
                        false), null);
        ydbTableClient.usingSessionMonoRetryable(session -> providersDao.upsertProvidersRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), List.of(providerOne, providerTwo))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<ProviderModel> newProviderOne = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        providerOne.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> newProviderOneBySourceTvmId = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        999L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<ProviderModel> newProviderTwo = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        providerTwo.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> newProviderTwoBySourceTvmId = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        9999L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newProviderOne);
        Assertions.assertTrue(newProviderOne.isPresent());
        Assertions.assertEquals(providerOne, newProviderOne.get());
        Assertions.assertNotNull(newProviderOneBySourceTvmId);
        Assertions.assertFalse(newProviderOneBySourceTvmId.isEmpty());
        Assertions.assertEquals(providerOne, newProviderOneBySourceTvmId.get(0));
        Assertions.assertNotNull(newProviderTwo);
        Assertions.assertTrue(newProviderTwo.isPresent());
        Assertions.assertEquals(providerTwo, newProviderTwo.get());
        Assertions.assertNotNull(newProviderTwoBySourceTvmId);
        Assertions.assertFalse(newProviderTwoBySourceTvmId.isEmpty());
        Assertions.assertEquals(providerTwo, newProviderTwoBySourceTvmId.get(0));
    }

    @Test
    public void testUpsertProvidersMinimal() {
        ProviderModel providerOne = new ProviderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                0L, "Test-1", "Тест-1", "Test description-1", "Тестовое описание-1",
                null, null, 1L, 2L, 1L, false, false, false, false, true, "test-1",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                null, 1L, null, false, false, null, null, null);
        ProviderModel providerTwo = new ProviderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                0L, "Test-2", "Тест-2", "Test description-2", "Тестовое описание-2",
                null, null, 3L, 4L, 1L, false, false, false, false, true, "test-2",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                null, 1L, null, false, false, null, null, null);
        ydbTableClient.usingSessionMonoRetryable(session -> providersDao.upsertProvidersRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), List.of(providerOne, providerTwo))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<ProviderModel> newProviderOne = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        providerOne.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<ProviderModel> newProviderTwo = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        providerTwo.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newProviderOne);
        Assertions.assertTrue(newProviderOne.isPresent());
        Assertions.assertEquals(providerOne, newProviderOne.get());
        Assertions.assertNotNull(newProviderTwo);
        Assertions.assertTrue(newProviderTwo.isPresent());
        Assertions.assertEquals(providerTwo, newProviderTwo.get());
    }

    @Test
    public void testUpdateProviderFull() {
        ProviderModel provider = new ProviderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                0L, "Test-1", "Тест-1", "Test description-1", "Тестовое описание-1",
                "https://test.yandex-team.ru", "grpc.yandex-team.ru",
                999L, 998L, 9999L, false, false, false, false, true, "test",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                Map.of("key", RelatedResourceMapping.builder()
                                .relatedCoefficientMap(Map.of(
                                        "key4", RelatedCoefficient.builder().numerator(3L).denominator(4L).build(),
                                        "key5", RelatedCoefficient.builder().numerator(5L).denominator(6L).build()
                                ))
                                .build(),
                        "key6", RelatedResourceMapping.builder()
                                .relatedCoefficientMap(Map.of(
                                        "key7", RelatedCoefficient.builder().numerator(7L).denominator(8L).build(),
                                        "key8", RelatedCoefficient.builder().numerator(9L).denominator(10L).build()
                                ))
                                .build()), 1L, null, false, false, null, null, null);
        ProviderModel updatedProvider = new ProviderModel(provider.getId(), Tenants.DEFAULT_TENANT_ID,
                1L, "Test-1-up", "Тест-1-up", "Test description-1-up", "Тестовое описание-1-up",
                "https://test-up.yandex-team.ru", "grpc-up.yandex-team.ru",
                899L, 898L, 9998L, true, false, false, false, true, "test-up",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                Map.of("key", RelatedResourceMapping.builder()
                        .relatedCoefficientMap(Map.of(
                                "key4", RelatedCoefficient.builder().numerator(3L).denominator(4L).build(),
                                "key5", RelatedCoefficient.builder().numerator(5L).denominator(6L).build()
                        ))
                        .build()), 1L, null, false, false,
                new AggregationSettings(FreeProvisionAggregationMode.NONE, UsageMode.UNDEFINED, 300L),
                new AggregationAlgorithm(false, 1000L, AggregationQuotaQueryType.PAGINATE, false, true, 3L, true,
                        false), null);
        ydbTableClient.usingSessionMonoRetryable(session -> providersDao.upsertProviderRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), provider)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<ProviderModel> newProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        provider.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> newProviderBySourceTvmId = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        999L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newProvider);
        Assertions.assertTrue(newProvider.isPresent());
        Assertions.assertEquals(provider, newProvider.get());
        Assertions.assertNotNull(newProviderBySourceTvmId);
        Assertions.assertFalse(newProviderBySourceTvmId.isEmpty());
        Assertions.assertEquals(provider, newProviderBySourceTvmId.get(0));
        ydbTableClient.usingSessionMonoRetryable(session -> providersDao.updateProviderRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), updatedProvider)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<ProviderModel> updatedNewProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        provider.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> updatedNewProviderBySourceTvmId = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        899L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> previousNewProviderBySourceTvmId = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        999L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(updatedNewProvider);
        Assertions.assertTrue(updatedNewProvider.isPresent());
        Assertions.assertEquals(updatedProvider, updatedNewProvider.get());
        Assertions.assertNotNull(updatedNewProviderBySourceTvmId);
        Assertions.assertFalse(updatedNewProviderBySourceTvmId.isEmpty());
        Assertions.assertEquals(updatedProvider, updatedNewProviderBySourceTvmId.get(0));
        Assertions.assertNotNull(previousNewProviderBySourceTvmId);
        Assertions.assertTrue(previousNewProviderBySourceTvmId.isEmpty());
    }

    @Test
    public void testUpdateProviderNotUpdated() {
        ProviderModel provider = new ProviderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                0L, "Test-1", "Тест-1", "Test description-1", "Тестовое описание-1",
                "https://test.yandex-team.ru", "grpc.yandex-team.ru",
                999L, 998L, 9999L, false, false, false, false, true, "test",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                null, 1L, null, false, false, null, null, null);
        ProviderModel updatedProvider = new ProviderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                1L, "Test-1-up", "Тест-1-up", "Test description-1-up", "Тестовое описание-1-up",
                "https://test-up.yandex-team.ru", "grpc-up.yandex-team.ru",
                899L, 898L, 9998L, true, false, false, false, true, "test-up",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                null, 1L, null, false, false, null, null, null);
        ydbTableClient.usingSessionMonoRetryable(session -> providersDao.upsertProviderRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), provider)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<ProviderModel> newProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        provider.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> newProviderBySourceTvmId = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        999L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newProvider);
        Assertions.assertTrue(newProvider.isPresent());
        Assertions.assertEquals(provider, newProvider.get());
        Assertions.assertNotNull(newProviderBySourceTvmId);
        Assertions.assertFalse(newProviderBySourceTvmId.isEmpty());
        Assertions.assertEquals(provider, newProviderBySourceTvmId.get(0));
        ydbTableClient.usingSessionMonoRetryable(session -> providersDao.updateProviderRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), updatedProvider)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<ProviderModel> updatedNewProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        updatedProvider.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> updatedNewProviderBySourceTvmId = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        899L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> previousNewProviderBySourceTvmId = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        999L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(updatedNewProvider);
        Assertions.assertFalse(updatedNewProvider.isPresent());
        Assertions.assertNotNull(updatedNewProviderBySourceTvmId);
        Assertions.assertTrue(updatedNewProviderBySourceTvmId.isEmpty());
        Assertions.assertNotNull(previousNewProviderBySourceTvmId);
        Assertions.assertFalse(previousNewProviderBySourceTvmId.isEmpty());
        Assertions.assertEquals(provider, previousNewProviderBySourceTvmId.get(0));
    }

    @Test
    public void testUpdateProviderNoSourceTvmIdChange() {
        ProviderModel provider = new ProviderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                0L, "Test-1", "Тест-1", "Test description-1", "Тестовое описание-1",
                "https://test.yandex-team.ru", "grpc.yandex-team.ru",
                999L, 998L, 9999L, false, false, false, false, true, "test",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                null, 1L, null, false, false, null, null, null);
        ProviderModel updatedProvider = new ProviderModel(provider.getId(), Tenants.DEFAULT_TENANT_ID,
                1L, "Test-1-up", "Тест-1-up", "Test description-1-up", "Тестовое описание-1-up",
                "https://test-up.yandex-team.ru", "grpc-up.yandex-team.ru",
                999L, 898L, 9998L, true, false, false, false, true, "test-up",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                null, 1L, null, false, false, null, null, null);
        ydbTableClient.usingSessionMonoRetryable(session -> providersDao.upsertProviderRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), provider)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<ProviderModel> newProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        provider.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> newProviderBySourceTvmId = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        999L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newProvider);
        Assertions.assertTrue(newProvider.isPresent());
        Assertions.assertEquals(provider, newProvider.get());
        Assertions.assertNotNull(newProviderBySourceTvmId);
        Assertions.assertFalse(newProviderBySourceTvmId.isEmpty());
        Assertions.assertEquals(provider, newProviderBySourceTvmId.get(0));
        ydbTableClient.usingSessionMonoRetryable(session -> providersDao.updateProviderRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), updatedProvider)
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<ProviderModel> updatedNewProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        provider.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> updatedNewProviderBySourceTvmId = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        999L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(updatedNewProvider);
        Assertions.assertTrue(updatedNewProvider.isPresent());
        Assertions.assertEquals(updatedProvider, updatedNewProvider.get());
        Assertions.assertNotNull(updatedNewProviderBySourceTvmId);
        Assertions.assertFalse(updatedNewProviderBySourceTvmId.isEmpty());
        Assertions.assertEquals(updatedProvider, updatedNewProviderBySourceTvmId.get(0));
    }

    @Test
    public void testUpdateProvidersFull() {
        ProviderModel providerOne = new ProviderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                0L, "Test-1", "Тест-1", "Test description-1", "Тестовое описание-1",
                "https://test.yandex-team.ru", "grpc.yandex-team.ru",
                999L, 998L, 9999L, false, false, false, false, true, "test-1",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                null, 1L, null, false, false, null, null, null);
        ProviderModel providerTwo = new ProviderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                0L, "Test-2", "Тест-2", "Test description-2", "Тестовое описание-2",
                "https://test-2.yandex-team.ru", "grpc-2.yandex-team.ru",
                9999L, 9998L, 99999L, false, false, false, false, true, "test-2",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                Map.of("key", RelatedResourceMapping.builder()
                                .relatedCoefficientMap(Map.of(
                                        "key4", RelatedCoefficient.builder().numerator(3L).denominator(4L).build(),
                                        "key5", RelatedCoefficient.builder().numerator(5L).denominator(6L).build()
                                ))
                                .build(),
                        "key6", RelatedResourceMapping.builder()
                                .relatedCoefficientMap(Map.of(
                                        "key7", RelatedCoefficient.builder().numerator(7L).denominator(8L).build(),
                                        "key8", RelatedCoefficient.builder().numerator(9L).denominator(10L).build()
                                ))
                                .build()), 1L, null, false, false, null, null, null);
        ProviderModel updatedProviderOne = new ProviderModel(providerOne.getId(), Tenants.DEFAULT_TENANT_ID,
                1L, "Test-1-up", "Тест-1-up", "Test description-1-up", "Тестовое описание-1-up",
                "https://test-up.yandex-team.ru", "grpc-up.yandex-team.ru",
                899L, 898L, 9998L, true, true, true, true, false, "test-1-up",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                Map.of("key", RelatedResourceMapping.builder()
                                .relatedCoefficientMap(Map.of(
                                        "key4", RelatedCoefficient.builder().numerator(3L).denominator(4L).build(),
                                        "key5", RelatedCoefficient.builder().numerator(5L).denominator(6L).build()
                                ))
                                .build(),
                        "key6", RelatedResourceMapping.builder()
                                .relatedCoefficientMap(Map.of(
                                        "key7", RelatedCoefficient.builder().numerator(7L).denominator(8L).build(),
                                        "key8", RelatedCoefficient.builder().numerator(9L).denominator(10L).build()
                                ))
                                .build()), 1L, null, false, false,
                new AggregationSettings(FreeProvisionAggregationMode.NONE, UsageMode.UNDEFINED, 300L),
                new AggregationAlgorithm(false, 1000L, AggregationQuotaQueryType.PAGINATE, false, true, 3L, true,
                        false), null);
        ProviderModel updatedProviderTwo = new ProviderModel(providerTwo.getId(), Tenants.DEFAULT_TENANT_ID,
                1L, "Test-2-up", "Тест-2-up", "Test description-2-up", "Тестовое описание-2-up",
                "https://test-2-up.yandex-team.ru", "grpc-2-up.yandex-team.ru",
                8999L, 8998L, 99998L, true, true, true, true, false, "test-2-up",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                null, 1L, null, false, false,
                new AggregationSettings(FreeProvisionAggregationMode.NONE, UsageMode.UNDEFINED, 300L),
                new AggregationAlgorithm(false, 1000L, AggregationQuotaQueryType.PAGINATE, false, true, 3L, true,
                        false), null);
        ydbTableClient.usingSessionMonoRetryable(session -> providersDao.upsertProvidersRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), List.of(providerOne, providerTwo))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<ProviderModel> newProviderOne = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        providerOne.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<ProviderModel> newProviderTwo = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        providerTwo.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> newProviderOneBySourceTvmId = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        999L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> newProviderTwoBySourceTvmId = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        9999L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newProviderOne);
        Assertions.assertTrue(newProviderOne.isPresent());
        Assertions.assertEquals(providerOne, newProviderOne.get());
        Assertions.assertNotNull(newProviderOneBySourceTvmId);
        Assertions.assertFalse(newProviderOneBySourceTvmId.isEmpty());
        Assertions.assertEquals(providerOne, newProviderOneBySourceTvmId.get(0));
        Assertions.assertNotNull(newProviderTwo);
        Assertions.assertTrue(newProviderTwo.isPresent());
        Assertions.assertEquals(providerTwo, newProviderTwo.get());
        Assertions.assertNotNull(newProviderTwoBySourceTvmId);
        Assertions.assertFalse(newProviderTwoBySourceTvmId.isEmpty());
        Assertions.assertEquals(providerTwo, newProviderTwoBySourceTvmId.get(0));
        ydbTableClient.usingSessionMonoRetryable(session -> providersDao.updateProvidersRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), List.of(updatedProviderOne,
                updatedProviderTwo)).retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics)))
                .block();
        Optional<ProviderModel> updatedNewProviderOne = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        providerOne.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> updatedNewProviderOneBySourceTvmId = ydbTableClient
                .usingSessionMonoRetryable(session ->
                        providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                899L, Tenants.DEFAULT_TENANT_ID)
                                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics)))
                .block();
        List<ProviderModel> previousNewProviderOneBySourceTvmId = ydbTableClient
                .usingSessionMonoRetryable(session ->
                        providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                999L, Tenants.DEFAULT_TENANT_ID)
                                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics)))
                .block();
        Optional<ProviderModel> updatedNewProviderTwo = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        providerTwo.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> updatedNewProviderTwoBySourceTvmId = ydbTableClient
                .usingSessionMonoRetryable(session ->
                        providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                8999L, Tenants.DEFAULT_TENANT_ID)
                                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics)))
                .block();
        List<ProviderModel> previousNewProviderTwoBySourceTvmId = ydbTableClient
                .usingSessionMonoRetryable(session ->
                        providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                9999L, Tenants.DEFAULT_TENANT_ID)
                                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics)))
                .block();
        Assertions.assertNotNull(updatedNewProviderOne);
        Assertions.assertTrue(updatedNewProviderOne.isPresent());
        Assertions.assertEquals(updatedProviderOne, updatedNewProviderOne.get());
        Assertions.assertNotNull(updatedNewProviderOneBySourceTvmId);
        Assertions.assertFalse(updatedNewProviderOneBySourceTvmId.isEmpty());
        Assertions.assertEquals(updatedProviderOne, updatedNewProviderOneBySourceTvmId.get(0));
        Assertions.assertNotNull(previousNewProviderOneBySourceTvmId);
        Assertions.assertTrue(previousNewProviderOneBySourceTvmId.isEmpty());
        Assertions.assertNotNull(updatedNewProviderTwo);
        Assertions.assertTrue(updatedNewProviderTwo.isPresent());
        Assertions.assertEquals(updatedProviderTwo, updatedNewProviderTwo.get());
        Assertions.assertNotNull(updatedNewProviderTwoBySourceTvmId);
        Assertions.assertFalse(updatedNewProviderTwoBySourceTvmId.isEmpty());
        Assertions.assertEquals(updatedProviderTwo, updatedNewProviderTwoBySourceTvmId.get(0));
        Assertions.assertNotNull(previousNewProviderTwoBySourceTvmId);
        Assertions.assertTrue(previousNewProviderTwoBySourceTvmId.isEmpty());
    }

    @Test
    public void testUpdateProvidersNoSourceTvmIdChange() {
        ProviderModel providerOne = new ProviderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                0L, "Test-1", "Тест-1", "Test description-1", "Тестовое описание-1",
                "https://test.yandex-team.ru", "grpc.yandex-team.ru",
                999L, 998L, 9999L, false, false, false, false, true, "test-1",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                Map.of("key", RelatedResourceMapping.builder()
                                .relatedCoefficientMap(Map.of(
                                        "key4", RelatedCoefficient.builder().numerator(3L).denominator(4L).build(),
                                        "key5", RelatedCoefficient.builder().numerator(5L).denominator(6L).build()
                                ))
                                .build(),
                        "key6", RelatedResourceMapping.builder()
                                .relatedCoefficientMap(Map.of(
                                        "key7", RelatedCoefficient.builder().numerator(7L).denominator(8L).build(),
                                        "key8", RelatedCoefficient.builder().numerator(9L).denominator(10L).build()
                                ))
                                .build()), 1L, null, false, false, null, null, null);
        ProviderModel providerTwo = new ProviderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                0L, "Test-2", "Тест-2", "Test description-2", "Тестовое описание-2",
                "https://test-2.yandex-team.ru", "grpc-2.yandex-team.ru",
                9999L, 9998L, 99999L, false, false, false, false, true, "test-2",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                null, 1L, null, false, false, null, null, null);
        ProviderModel updatedProviderOne = new ProviderModel(providerOne.getId(), Tenants.DEFAULT_TENANT_ID,
                1L, "Test-1-up", "Тест-1-up", "Test description-1-up", "Тестовое описание-1-up",
                "https://test-up.yandex-team.ru", "grpc-up.yandex-team.ru",
                999L, 898L, 9998L, true, true, true, true, false, "test-1-up",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                null, 1L, null, false, false, null, null, null);
        ProviderModel updatedProviderTwo = new ProviderModel(providerTwo.getId(), Tenants.DEFAULT_TENANT_ID,
                1L, "Test-2-up", "Тест-2-up", "Test description-2-up", "Тестовое описание-2-up",
                "https://test-2-up.yandex-team.ru", "grpc-2-up.yandex-team.ru",
                9999L, 8998L, 99998L, true, true, true, true, false, "test-2-up",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_SYNC_ACCOUNTS_PAGE_SIZE, false, emptySet(), null),
                true, true, true, true, new BillingMeta(null),
                Map.of("key", RelatedResourceMapping.builder()
                                .relatedCoefficientMap(Map.of(
                                        "key4", RelatedCoefficient.builder().numerator(3L).denominator(4L).build(),
                                        "key5", RelatedCoefficient.builder().numerator(5L).denominator(6L).build()
                                ))
                                .build(),
                        "key6", RelatedResourceMapping.builder()
                                .relatedCoefficientMap(Map.of(
                                        "key7", RelatedCoefficient.builder().numerator(7L).denominator(8L).build(),
                                        "key8", RelatedCoefficient.builder().numerator(9L).denominator(10L).build()
                                ))
                                .build()), 1L, null, false, false, null, null, null);
        ydbTableClient.usingSessionMonoRetryable(session -> providersDao.upsertProvidersRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), List.of(providerOne, providerTwo))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<ProviderModel> newProviderOne = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        providerOne.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Optional<ProviderModel> newProviderTwo = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        providerTwo.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> newProviderOneBySourceTvmId = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        999L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> newProviderTwoBySourceTvmId = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        9999L, Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(newProviderOne);
        Assertions.assertTrue(newProviderOne.isPresent());
        Assertions.assertEquals(providerOne, newProviderOne.get());
        Assertions.assertNotNull(newProviderOneBySourceTvmId);
        Assertions.assertFalse(newProviderOneBySourceTvmId.isEmpty());
        Assertions.assertEquals(providerOne, newProviderOneBySourceTvmId.get(0));
        Assertions.assertNotNull(newProviderTwo);
        Assertions.assertTrue(newProviderTwo.isPresent());
        Assertions.assertEquals(providerTwo, newProviderTwo.get());
        Assertions.assertNotNull(newProviderTwoBySourceTvmId);
        Assertions.assertFalse(newProviderTwoBySourceTvmId.isEmpty());
        Assertions.assertEquals(providerTwo, newProviderTwoBySourceTvmId.get(0));
        ydbTableClient.usingSessionMonoRetryable(session -> providersDao.updateProvidersRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), List.of(updatedProviderOne,
                updatedProviderTwo)).retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics)))
                .block();
        Optional<ProviderModel> updatedNewProviderOne = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        providerOne.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> updatedNewProviderOneBySourceTvmId = ydbTableClient
                .usingSessionMonoRetryable(session ->
                        providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                999L, Tenants.DEFAULT_TENANT_ID)
                                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics)))
                .block();
        Optional<ProviderModel> updatedNewProviderTwo = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        providerTwo.getId(), Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        List<ProviderModel> updatedNewProviderTwoBySourceTvmId = ydbTableClient
                .usingSessionMonoRetryable(session ->
                        providersDao.getBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                9999L, Tenants.DEFAULT_TENANT_ID)
                                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics)))
                .block();
        Assertions.assertNotNull(updatedNewProviderOne);
        Assertions.assertTrue(updatedNewProviderOne.isPresent());
        Assertions.assertEquals(updatedProviderOne, updatedNewProviderOne.get());
        Assertions.assertNotNull(updatedNewProviderOneBySourceTvmId);
        Assertions.assertFalse(updatedNewProviderOneBySourceTvmId.isEmpty());
        Assertions.assertEquals(updatedProviderOne, updatedNewProviderOneBySourceTvmId.get(0));
        Assertions.assertNotNull(updatedNewProviderTwo);
        Assertions.assertTrue(updatedNewProviderTwo.isPresent());
        Assertions.assertEquals(updatedProviderTwo, updatedNewProviderTwo.get());
        Assertions.assertNotNull(updatedNewProviderTwoBySourceTvmId);
        Assertions.assertFalse(updatedNewProviderTwoBySourceTvmId.isEmpty());
        Assertions.assertEquals(updatedProviderTwo, updatedNewProviderTwoBySourceTvmId.get(0));
    }

    @Test
    public void testExistsByKey() {
        Boolean exists = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.existsByKey(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID, "yp", true)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(exists);
        Assertions.assertTrue(exists);
    }

    @Test
    public void testExistsByKeyNotExists() {
        Boolean exists = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.existsByKey(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID, "not-exists", true)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(exists);
        Assertions.assertFalse(exists);
    }
}
