package ru.yandex.intranet.d.web.admin.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao;
import ru.yandex.intranet.d.dao.folders.FolderDao;
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao;
import ru.yandex.intranet.d.dao.providers.ProvidersDao;
import ru.yandex.intranet.d.dao.quotas.QuotasDao;
import ru.yandex.intranet.d.dao.resources.ResourcesDao;
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao;
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao;
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao;
import ru.yandex.intranet.d.datasource.impl.YdbRetry;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.metrics.YdbMetrics;
import ru.yandex.intranet.d.model.WithTenant;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
import ru.yandex.intranet.d.model.folders.FolderOperationType;
import ru.yandex.intranet.d.model.folders.QuotasByAccount;
import ru.yandex.intranet.d.model.folders.QuotasByResource;
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel;
import ru.yandex.intranet.d.model.providers.BillingMeta;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.model.resources.ResourceModel;
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel;
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel;
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel;
import ru.yandex.intranet.d.model.users.UserModel;
import ru.yandex.intranet.d.services.providers.ProvidersService;
import ru.yandex.intranet.d.util.result.Result;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.SortOrderDto;
import ru.yandex.intranet.d.web.model.quotas.QuotaSetDto;
import ru.yandex.intranet.d.web.security.model.YaUserDetails;

import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestUsers.USER_1_ID;
import static ru.yandex.intranet.d.TestUsers.USER_1_UID;
import static ru.yandex.intranet.d.web.admin.providers.AdminProvidersApiTest.accountsQuotasModel;
import static ru.yandex.intranet.d.web.admin.providers.AdminProvidersApiTest.quotaModel;
import static ru.yandex.intranet.d.web.admin.providers.AdminProvidersApiTest.resourceModel;
import static ru.yandex.intranet.d.web.admin.providers.AdminProvidersApiTest.resourceSegmentModel;
import static ru.yandex.intranet.d.web.admin.providers.AdminProvidersApiTest.resourceSegmentationModel;
import static ru.yandex.intranet.d.web.admin.providers.AdminProvidersApiTest.resourceTypeModel;

/**
 * Tests for {@link ru.yandex.intranet.d.web.controllers.admin.providers.AdminProvidersController#putProvisionsAsQuota}
 *
 * @author Evgenii Serov <evserov@yandex-team.ru>
 */
@IntegrationTest
public class PutProvisionAsQuotasTest {
    private static final Random RANDOM = new Random();

    @Autowired
    private WebTestClient webClient;
    @Autowired
    private QuotasDao quotasDao;
    @Autowired
    private AccountsQuotasDao accountsQuotasDao;
    @Autowired
    private FolderOperationLogDao folderOperationLogDao;
    @Autowired
    private YdbTableClient ydbTableClient;
    @Autowired
    private ProvidersDao providersDao;
    @Autowired
    private ResourceTypesDao resourceTypesDao;
    @Autowired
    private ResourceSegmentationsDao resourceSegmentationsDao;
    @Autowired
    private ResourceSegmentsDao resourceSegmentsDao;
    @Autowired
    private ResourcesDao resourcesDao;
    @Autowired
    private FolderDao folderDao;
    @Autowired
    private YdbTableClient tableClient;
    @Autowired
    private YdbMetrics ydbMetrics;
    @Value("${ydb.transactionRetries}")
    private long transactionRetries;
    @Autowired
    private ProvidersService providersService;

    @Test
    public void setQuotasProviderValidationTest() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/admin/providers/{id}/_putProvisionsAsQuotas", "fake_provider_id")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertTrue(errorCollection.getErrors().contains("Provider not found."));
                });

        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/admin/providers/{id}/_putProvisionsAsQuotas", UUID.randomUUID().toString())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertTrue(errorCollection.getErrors().contains("Provider not found."));
                });

        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/admin/providers/{id}/_putProvisionsAsQuotas", YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertTrue(errorCollection.getErrors().contains("Provider is managed."));
                });
    }

    @Test
    public void setQuotasForbiddenForNonDAdminTest() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.NOT_D_ADMIN_UID))
                .post()
                .uri("/admin/providers/{id}/_putProvisionsAsQuotas", YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void setQuotasShouldWorkTest() {
        ProviderModel provider = getProvider();
        ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.upsertProviderRetryable(session.asTxCommitRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE), provider)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        Result<QuotaSetDto> quotaSetDto = providersService.putProvisionsAsQuotaBlocking(Tenants.DEFAULT_TENANT_ID,
                provider.getId(), getUser());
        Assertions.assertTrue(quotaSetDto.isSuccess());
        quotaSetDto.doOnSuccess(quota -> Assertions.assertEquals(0L, quota.getQuotaChangeCount()));
        ResourceTypeModel resourceType = resourceTypeModel(provider.getId(), "test",
                "b02344bf-96af-4cc5-937c-66a479989ce8");
        ResourceSegmentationModel locationSegmentation = resourceSegmentationModel(provider.getId(), "location");
        ResourceSegmentModel vlaSegment = resourceSegmentModel(locationSegmentation.getId(), "VLA");
        ResourceModel resourceOne = resourceModel(provider.getId(), "test", resourceType.getId(),
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())),
                "b02344bf-96af-4cc5-937c-66a479989ce8",
                Set.of("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                        "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", null);
        ResourceModel resourceTwo = resourceModel(provider.getId(), "test", resourceType.getId(),
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())),
                "b02344bf-96af-4cc5-937c-66a479989ce8",
                Set.of("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                        "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", null);

        AtomicInteger atomicInteger = new AtomicInteger();
        List<FolderModel> folders = Stream.generate(atomicInteger::incrementAndGet)
                .limit(300)
                .map(AdminProvidersApiTest::folderModel)
                .collect(Collectors.toList());
        List<QuotaModel> quotaModels = folders.stream()
                .flatMap(f -> Stream.of(quotaModel(provider.getId(), resourceOne.getId(), f.getId(), 0,
                        -RANDOM.nextInt(50000) - 1),
                        quotaModel(provider.getId(), resourceTwo.getId(), f.getId(), 0, -RANDOM.nextInt(50000) - 1)))
                .collect(Collectors.toList());
        Map<String, Map<String, Long>> balanceByResourceByFolder = quotaModels.stream()
                .collect(Collectors.groupingBy(QuotaModel::getFolderId,
                        Collectors.groupingBy(QuotaModel::getResourceId, Collectors.mapping(QuotaModel::getBalance,
                                Collectors.summingLong(l -> -l)))));
        List<AccountsQuotasModel> accountsQuotasModels = folders.stream()
                .flatMap(f -> {
                    Map<String, Long> balanceByResource = balanceByResourceByFolder.get(f.getId());
                    long quotaOne = balanceByResource.get(resourceOne.getId());
                    long providedAccountOneResourceOne = quotaOne - RANDOM.nextInt((int) quotaOne);
                    long providedAccountTwoResourceOne = quotaOne - providedAccountOneResourceOne;
                    long quotaTwo = balanceByResource.get(resourceTwo.getId());
                    long providedAccountOneResourceTwo = quotaTwo - RANDOM.nextInt((int) quotaTwo);
                    long providedAccountTwoResourceTwo = quotaTwo - providedAccountOneResourceTwo;
                    String accountOneId = UUID.randomUUID().toString();
                    String accountTwoId = UUID.randomUUID().toString();
                    return Stream.of(
                            accountsQuotasModel(provider.getId(), resourceOne.getId(), f.getId(), accountOneId, 0,
                                    providedAccountOneResourceOne),
                            accountsQuotasModel(provider.getId(), resourceOne.getId(), f.getId(), accountTwoId, 0,
                                    providedAccountTwoResourceOne),
                            accountsQuotasModel(provider.getId(), resourceTwo.getId(), f.getId(), accountOneId, 0,
                                    providedAccountOneResourceTwo),
                            accountsQuotasModel(provider.getId(), resourceTwo.getId(), f.getId(), accountTwoId, 0,
                                    providedAccountTwoResourceTwo)
                    );
                })
                .collect(Collectors.toList());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                providersDao.upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourceTypesDao
                        .upsertResourceTypeRetryable(txSession, resourceType)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourceSegmentationsDao
                        .upsertResourceSegmentationRetryable(txSession, locationSegmentation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourceSegmentsDao
                        .upsertResourceSegmentRetryable(txSession, vlaSegment)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourcesDao
                        .upsertResourceRetryable(txSession, resourceOne)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourcesDao
                        .upsertResourceRetryable(txSession, resourceTwo)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertAllRetryable(txSession, folders)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .upsertAllRetryable(txSession, quotaModels)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                        .upsertAllRetryable(txSession, accountsQuotasModels)))
                .block();

        Result<QuotaSetDto> quotaSetDtoTwo = providersService.putProvisionsAsQuotaBlocking(Tenants.DEFAULT_TENANT_ID,
                provider.getId(), getUser());
        Assertions.assertTrue(quotaSetDtoTwo.isSuccess());
        quotaSetDtoTwo.doOnSuccess(quota -> Assertions.assertEquals(quotaModels.stream()
                .filter(quotaModel -> quotaModel.getBalance() != 0)
                .count(), quota.getQuotaChangeCount()));
        Set<WithTenant<QuotaModel.Key>> keys = quotaModels.stream()
                .map(QuotaModel::toKey)
                .map(k -> new WithTenant<>(Tenants.DEFAULT_TENANT_ID, k))
                .collect(Collectors.toSet());
        Assertions.assertEquals(quotaModels.size(), keys.size());
        List<QuotaModel> updatedQuotaModels = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao.getByKeys(txSession,
                                new ArrayList<>(keys)))).block();
        Assertions.assertNotNull(updatedQuotaModels);
        Set<WithTenant<QuotaModel.Key>> updatedKeys = updatedQuotaModels.stream()
                .map(QuotaModel::toKey)
                .map(k -> new WithTenant<>(Tenants.DEFAULT_TENANT_ID, k))
                .collect(Collectors.toSet());
        Assertions.assertEquals(updatedQuotaModels.size(), updatedKeys.size());
        Assertions.assertEquals(keys, updatedKeys);
        Map<QuotaModel.Key, Long> quotaModelsMap = quotaModels.stream()
                .collect(Collectors.toMap(QuotaModel::toKey, qm -> -qm.getBalance()));
        Assertions.assertEquals(0, (int) updatedQuotaModels.stream()
                .filter(uqm -> !quotaModelsMap.get(uqm.toKey()).equals(uqm.getQuota())).count());
        Map<String, Map<String, Long>> oldQuotasBalanceByFolder = quotaModels.stream()
                .collect(Collectors.groupingBy(QuotaModel::getFolderId, Collectors.groupingBy(QuotaModel::getResourceId,
                        Collectors.mapping(QuotaModel::getBalance, Collectors.summingLong(l -> l)))));
        folders.forEach(f -> {
            List<FolderOperationLogModel> operationLogModels =
                    tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                            TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
                                    .getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, f.getId(),
                                            SortOrderDto.DESC, 2)))
                            .block();
            Assertions.assertNotNull(operationLogModels);
            Assertions.assertEquals(1, operationLogModels.size());
            FolderOperationLogModel folderOperationLogModel = operationLogModels.get(0);
            Assertions.assertEquals(FolderOperationType.PROVISION_AS_QUOTA_BY_ADMIN,
                    folderOperationLogModel.getOperationType());
            Assertions.assertEquals(USER_1_UID, folderOperationLogModel.getAuthorUserUid().orElseThrow());
            Assertions.assertEquals(USER_1_ID, folderOperationLogModel.getAuthorUserId().orElseThrow());
            QuotasByResource oldQuota =
                    new QuotasByResource(oldQuotasBalanceByFolder.get(f.getId()).entrySet().stream().
                            collect(Collectors.toMap(Map.Entry::getKey, v -> 0L)));
            Assertions.assertEquals(oldQuota,
                    folderOperationLogModel.getOldQuotas());
            Assertions.assertEquals(new QuotasByResource(oldQuotasBalanceByFolder.get(f.getId())),
                    folderOperationLogModel.getOldBalance());
            Assertions.assertEquals(new QuotasByAccount(Map.of()), folderOperationLogModel.getOldProvisions());
            Assertions.assertEquals(new QuotasByResource(oldQuotasBalanceByFolder.get(f.getId()).entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, v -> -v.getValue()))),
                    folderOperationLogModel.getNewQuotas());
            Assertions.assertEquals(oldQuota, folderOperationLogModel.getNewBalance());
            Assertions.assertEquals(new QuotasByAccount(Map.of()), folderOperationLogModel.getNewProvisions());
            Assertions.assertEquals(f.getNextOpLogOrder(), folderOperationLogModel.getOrder());
        });
    }

    @Test
    public void setQuotasWithoutProvisionsTest() {
        ProviderModel provider = getProvider(true);
        ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.upsertProviderRetryable(session.asTxCommitRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE), provider)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        ResourceTypeModel resourceType = resourceTypeModel(provider.getId(), "test",
                "b02344bf-96af-4cc5-937c-66a479989ce8");
        ResourceSegmentationModel locationSegmentation = resourceSegmentationModel(provider.getId(), "location");
        ResourceSegmentModel vlaSegment = resourceSegmentModel(locationSegmentation.getId(), "VLA");
        ResourceModel resourceOne = resourceModel(provider.getId(), "test", resourceType.getId(),
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())),
                "b02344bf-96af-4cc5-937c-66a479989ce8",
                Set.of("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                        "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", null);
        ResourceModel resourceTwo = resourceModel(provider.getId(), "test", resourceType.getId(),
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())),
                "b02344bf-96af-4cc5-937c-66a479989ce8",
                Set.of("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                        "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", null);

        AtomicInteger atomicInteger = new AtomicInteger();
        List<FolderModel> folders = Stream.generate(atomicInteger::incrementAndGet)
                .limit(4)
                .map(AdminProvidersApiTest::folderModel)
                .collect(Collectors.toList());
        List<QuotaModel> quotaModels = folders.stream()
                .flatMap(f -> Stream.of(quotaModel(provider.getId(), resourceOne.getId(), f.getId(), 0,
                        -RANDOM.nextInt(50000) - 1),
                        quotaModel(provider.getId(), resourceTwo.getId(), f.getId(), 0, -RANDOM.nextInt(50000) - 1)))
                .collect(Collectors.toList());
        Map<String, Map<String, Long>> balanceByResourceByFolder = quotaModels.stream()
                .collect(Collectors.groupingBy(QuotaModel::getFolderId,
                        Collectors.groupingBy(QuotaModel::getResourceId, Collectors.mapping(QuotaModel::getBalance,
                                Collectors.summingLong(l -> -l)))));
        List<AccountsQuotasModel> accountsQuotasModels = new ArrayList<>();
        for (int i = 2; i < folders.size(); i++) {
            FolderModel f = folders.get(i);
            Map<String, Long> balanceByResource = balanceByResourceByFolder.get(f.getId());
            long quotaOne = balanceByResource.get(resourceOne.getId());
            long providedAccountOneResourceOne = quotaOne - RANDOM.nextInt((int) quotaOne);
            long providedAccountTwoResourceOne = quotaOne - providedAccountOneResourceOne;
            long quotaTwo = balanceByResource.get(resourceTwo.getId());
            long providedAccountOneResourceTwo = quotaTwo - RANDOM.nextInt((int) quotaTwo);
            long providedAccountTwoResourceTwo = quotaTwo - providedAccountOneResourceTwo;
            String accountOneId = UUID.randomUUID().toString();
            String accountTwoId = UUID.randomUUID().toString();
            if (i % 2 == 0) {
                accountsQuotasModels.addAll(List.of(accountsQuotasModel(provider.getId(), resourceOne.getId(),
                        f.getId(), accountOneId, 0, providedAccountOneResourceOne),
                        accountsQuotasModel(provider.getId(), resourceOne.getId(), f.getId(), accountTwoId, 0,
                                providedAccountTwoResourceOne)));
            }
            accountsQuotasModels.addAll(List.of(
                    accountsQuotasModel(provider.getId(), resourceTwo.getId(), f.getId(), accountOneId, 0,
                            providedAccountOneResourceTwo),
                    accountsQuotasModel(provider.getId(), resourceTwo.getId(), f.getId(), accountTwoId, 0,
                            providedAccountTwoResourceTwo)
            ));
        }
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourceTypesDao
                        .upsertResourceTypeRetryable(txSession, resourceType)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourceSegmentationsDao
                        .upsertResourceSegmentationRetryable(txSession, locationSegmentation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourceSegmentsDao
                        .upsertResourceSegmentRetryable(txSession, vlaSegment)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourcesDao
                        .upsertResourceRetryable(txSession, resourceOne)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourcesDao
                        .upsertResourceRetryable(txSession, resourceTwo)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertAllRetryable(txSession, folders)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .upsertAllRetryable(txSession, quotaModels)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                        .upsertAllRetryable(txSession, accountsQuotasModels)))
                .block();
        Result<QuotaSetDto> quotaSetDto = providersService.putProvisionsAsQuotaBlocking(Tenants.DEFAULT_TENANT_ID,
                provider.getId(), getUser());
        Assertions.assertTrue(quotaSetDto.isSuccess());
        quotaSetDto.doOnSuccess(quota -> Assertions.assertEquals(3L, quota.getQuotaChangeCount()));
    }

    private YaUserDetails getUser() {
        return new YaUserDetails(TestUsers.D_ADMIN_UID, null, null, null,
                Set.of(), UserModel.builder().id(USER_1_ID).passportUid(USER_1_UID).roles(Map.of()).build(),
                List.of(), Set.of());
    }

    private ProviderModel getProvider() {
        return getProvider(false);
    }

    private ProviderModel getProvider(boolean readOnly) {
        return ProviderModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .nameEn("Test provider")
                .nameRu("Тестовый провайдер")
                .descriptionEn("Test provider")
                .descriptionRu("Тестовый провайдер")
                .grpcApiUri("url")
                .sourceTvmId(1L)
                .destinationTvmId(2L)
                .serviceId(42L)
                .deleted(false)
                .readOnly(readOnly)
                .multipleAccountsPerFolder(true)
                .accountTransferWithQuota(false)
                .managed(false)
                .key("test_provider_42")
                .accountsSettings(AccountsSettingsModel.builder()
                        .displayNameSupported(true)
                        .keySupported(true)
                        .deleteSupported(true)
                        .softDeleteSupported(true)
                        .moveSupported(true)
                        .renameSupported(true)
                        .perAccountVersionSupported(true)
                        .perProvisionVersionSupported(true)
                        .perAccountVersionSupported(true)
                        .perProvisionLastUpdateSupported(true)
                        .operationIdDeduplicationSupported(true)
                        .syncCoolDownDisabled(true)
                        .retryCoolDownDisabled(true)
                        .perAccountLastUpdateSupported(true)
                        .accountsSyncPageSize(1000L)
                        .build())
                .importAllowed(true)
                .accountsSpacesSupported(false)
                .syncEnabled(true)
                .grpcTlsOn(false)
                .billingMeta(BillingMeta.builder().build())
                .trackerComponentId(1L)
                .relatedResourcesByResourceId(null)
                .build();
    }
}
