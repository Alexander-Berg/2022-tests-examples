package ru.yandex.intranet.d.web.admin.maintenance;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsDao;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao;
import ru.yandex.intranet.d.dao.folders.FolderDao;
import ru.yandex.intranet.d.dao.providers.ProvidersDao;
import ru.yandex.intranet.d.dao.quotas.QuotasDao;
import ru.yandex.intranet.d.dao.resources.ResourcesDao;
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao;
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao;
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.accounts.AccountModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderType;
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.model.resources.ResourceModel;
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel;
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel;
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel;
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel;
import ru.yandex.intranet.d.utils.DummyModels;
import ru.yandex.intranet.d.web.MockUser;

import static com.yandex.ydb.table.transaction.TransactionMode.SERIALIZABLE_READ_WRITE;

/**
 * Maintenance admin API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class MaintenanceAdminApiTest {

    @Autowired
    private QuotasDao quotasDao;
    @Autowired
    private AccountsDao accountsDao;
    @Autowired
    private AccountsQuotasDao accountsQuotasDao;
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
    private WebTestClient webClient;

    @Test
    public void runGCTest() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/maintenance/_gcQuotasProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/maintenance/_gcQuotasProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void runGCExpectedTest() {
        ProviderModel provider = providerModel("in-process:test", null, false);
        ResourceTypeModel resourceType = resourceTypeModel(provider.getId(), "test",
                "b02344bf-96af-4cc5-937c-66a479989ce8");
        ResourceSegmentationModel locationSegmentation = resourceSegmentationModel(provider.getId(), "location");
        ResourceSegmentModel vlaSegment = resourceSegmentModel(locationSegmentation.getId(), "VLA");
        ResourceModel resource = resourceModel(provider.getId(), "test", resourceType.getId(),
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())),
                "b02344bf-96af-4cc5-937c-66a479989ce8",
                Set.of("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                        "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", null);
        FolderModel folderOne = folderModel(1L);
        FolderModel folderTwo = folderModel(1L);
        FolderModel folderThree = folderModel(1L);
        FolderModel folderFour = folderModel(1L);
        AccountModel accountOne = accountModel(provider.getId(), folderOne.getId());
        AccountModel accountTwo = accountModel(provider.getId(), folderTwo.getId());
        AccountModel accountThree = accountModel(provider.getId(), folderThree.getId());
        AccountModel accountFour = accountModel(provider.getId(), folderFour.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(SERIALIZABLE_READ_WRITE,
                txSession -> providersDao.upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        SERIALIZABLE_READ_WRITE, txSession -> resourceTypesDao
                                .upsertResourceTypeRetryable(txSession, resourceType)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        SERIALIZABLE_READ_WRITE, txSession -> resourceSegmentationsDao
                                .upsertResourceSegmentationRetryable(txSession, locationSegmentation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        SERIALIZABLE_READ_WRITE, txSession -> resourceSegmentsDao
                                .upsertResourceSegmentRetryable(txSession, vlaSegment)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        SERIALIZABLE_READ_WRITE, txSession -> resourcesDao
                                .upsertResourceRetryable(txSession, resource)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        SERIALIZABLE_READ_WRITE, txSession -> folderDao
                                .upsertAllRetryable(txSession, List.of(folderOne, folderTwo, folderThree, folderFour))))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        SERIALIZABLE_READ_WRITE, txSession -> accountsDao.upsertAllRetryable(txSession,
                                List.of(accountOne, accountTwo, accountThree, accountFour)))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                                .upsertOneRetryable(txSession, QuotaModel.builder()
                                        .tenantId(Tenants.DEFAULT_TENANT_ID)
                                        .providerId(provider.getId())
                                        .resourceId(resource.getId())
                                        .folderId(folderOne.getId())
                                        .quota(0L)
                                        .balance(0L)
                                        .frozenQuota(0L)
                                        .build())))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                                .upsertOneRetryable(txSession, QuotaModel.builder()
                                        .tenantId(Tenants.DEFAULT_TENANT_ID)
                                        .providerId(provider.getId())
                                        .resourceId(resource.getId())
                                        .folderId(folderTwo.getId())
                                        .quota(1L)
                                        .balance(0L)
                                        .frozenQuota(0L)
                                        .build())))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                                .upsertOneRetryable(txSession, QuotaModel.builder()
                                        .tenantId(Tenants.DEFAULT_TENANT_ID)
                                        .providerId(provider.getId())
                                        .resourceId(resource.getId())
                                        .folderId(folderThree.getId())
                                        .quota(0L)
                                        .balance(1L)
                                        .frozenQuota(0L)
                                        .build())))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                                .upsertOneRetryable(txSession, QuotaModel.builder()
                                        .tenantId(Tenants.DEFAULT_TENANT_ID)
                                        .providerId(provider.getId())
                                        .resourceId(resource.getId())
                                        .folderId(folderFour.getId())
                                        .quota(0L)
                                        .balance(0L)
                                        .frozenQuota(1L)
                                        .build())))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                                .upsertOneRetryable(txSession, new AccountsQuotasModel.Builder()
                                        .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                        .setProviderId(provider.getId())
                                        .setResourceId(resource.getId())
                                        .setFolderId(folderOne.getId())
                                        .setAccountId(accountOne.getId())
                                        .setProvidedQuota(0L)
                                        .setAllocatedQuota(0L)
                                        .setLastProvisionUpdate(Instant.now().minus(Duration.ofDays(2)))
                                        .build())))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                                .upsertOneRetryable(txSession, new AccountsQuotasModel.Builder()
                                        .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                        .setProviderId(provider.getId())
                                        .setResourceId(resource.getId())
                                        .setFolderId(folderTwo.getId())
                                        .setAccountId(accountTwo.getId())
                                        .setProvidedQuota(1L)
                                        .setAllocatedQuota(0L)
                                        .setLastProvisionUpdate(Instant.now().minus(Duration.ofDays(2)))
                                        .build())))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                                .upsertOneRetryable(txSession, new AccountsQuotasModel.Builder()
                                        .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                        .setProviderId(provider.getId())
                                        .setResourceId(resource.getId())
                                        .setFolderId(folderThree.getId())
                                        .setAccountId(accountThree.getId())
                                        .setProvidedQuota(0L)
                                        .setAllocatedQuota(1L)
                                        .setLastProvisionUpdate(Instant.now().minus(Duration.ofDays(2)))
                                        .build())))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                                .upsertOneRetryable(txSession, new AccountsQuotasModel.Builder()
                                        .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                        .setProviderId(provider.getId())
                                        .setResourceId(resource.getId())
                                        .setFolderId(folderFour.getId())
                                        .setAccountId(accountFour.getId())
                                        .setProvidedQuota(0L)
                                        .setAllocatedQuota(0L)
                                        .setLastProvisionUpdate(Instant.now())
                                        .build())))
                .block();
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/maintenance/_gcQuotasProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();
        Optional<QuotaModel> quotaOne = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        SERIALIZABLE_READ_WRITE, txSession -> quotasDao.getOneQuota(txSession, folderOne.getId(),
                                provider.getId(), resource.getId(), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Optional<QuotaModel> quotaTwo = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        SERIALIZABLE_READ_WRITE, txSession -> quotasDao.getOneQuota(txSession, folderTwo.getId(),
                                provider.getId(), resource.getId(), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Optional<QuotaModel> quotaThree = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        SERIALIZABLE_READ_WRITE, txSession -> quotasDao.getOneQuota(txSession, folderThree.getId(),
                                provider.getId(), resource.getId(), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Optional<QuotaModel> quotaFour = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        SERIALIZABLE_READ_WRITE, txSession -> quotasDao.getOneQuota(txSession, folderFour.getId(),
                                provider.getId(), resource.getId(), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Optional<AccountsQuotasModel> accountQuotaOne = tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(SERIALIZABLE_READ_WRITE, txSession ->
                                accountsQuotasDao.getById(txSession, new AccountsQuotasModel
                                        .Identity(accountOne.getId(), resource.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Optional<AccountsQuotasModel> accountQuotaTwo = tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(SERIALIZABLE_READ_WRITE, txSession ->
                                accountsQuotasDao.getById(txSession, new AccountsQuotasModel
                                        .Identity(accountTwo.getId(), resource.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Optional<AccountsQuotasModel> accountQuotaThree = tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(SERIALIZABLE_READ_WRITE, txSession ->
                                accountsQuotasDao.getById(txSession, new AccountsQuotasModel
                                        .Identity(accountThree.getId(), resource.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Optional<AccountsQuotasModel> accountQuotaFour = tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(SERIALIZABLE_READ_WRITE, txSession ->
                                accountsQuotasDao.getById(txSession, new AccountsQuotasModel
                                        .Identity(accountFour.getId(), resource.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertTrue(quotaOne.isEmpty());
        Assertions.assertTrue(quotaTwo.isPresent());
        Assertions.assertTrue(quotaThree.isPresent());
        Assertions.assertTrue(quotaFour.isPresent());
        Assertions.assertTrue(accountQuotaOne.isEmpty());
        Assertions.assertTrue(accountQuotaTwo.isPresent());
        Assertions.assertTrue(accountQuotaThree.isPresent());
        Assertions.assertTrue(accountQuotaFour.isPresent());
    }

    private ProviderModel providerModel(String grpcUri, String restUri, boolean accountsSpacesSupported) {
        return ProviderModel.builder()
                .id(UUID.randomUUID().toString())
                .grpcApiUri(grpcUri)
                .restApiUri(restUri)
                .destinationTvmId(42L)
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .nameEn("Test")
                .nameRu("Test")
                .descriptionEn("Test")
                .descriptionRu("Test")
                .sourceTvmId(42L)
                .serviceId(69L)
                .deleted(false)
                .readOnly(false)
                .multipleAccountsPerFolder(true)
                .accountTransferWithQuota(true)
                .managed(true)
                .key("test")
                .trackerComponentId(1L)
                .accountsSettings(AccountsSettingsModel.builder()
                        .displayNameSupported(true)
                        .keySupported(true)
                        .deleteSupported(true)
                        .softDeleteSupported(true)
                        .moveSupported(true)
                        .renameSupported(true)
                        .perAccountVersionSupported(true)
                        .perProvisionVersionSupported(true)
                        .perAccountLastUpdateSupported(true)
                        .perProvisionLastUpdateSupported(true)
                        .operationIdDeduplicationSupported(true)
                        .syncCoolDownDisabled(false)
                        .retryCoolDownDisabled(false)
                        .accountsSyncPageSize(1000L)
                        .build())
                .importAllowed(true)
                .accountsSpacesSupported(accountsSpacesSupported)
                .syncEnabled(true)
                .grpcTlsOn(true)
                .build();
    }

    private ResourceTypeModel resourceTypeModel(String providerId, String key, String unitsEnsembleId) {
        return ResourceTypeModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .providerId(providerId)
                .version(0L)
                .key(key)
                .nameEn("Test")
                .nameRu("Test")
                .descriptionEn("Test")
                .descriptionRu("Test")
                .deleted(false)
                .unitsEnsembleId(unitsEnsembleId)
                .build();
    }

    private ResourceSegmentationModel resourceSegmentationModel(String providerId, String key) {
        return ResourceSegmentationModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .providerId(providerId)
                .version(0L)
                .key(key)
                .nameEn("Test")
                .nameRu("Test")
                .descriptionEn("Test")
                .descriptionRu("Test")
                .deleted(false)
                .build();
    }

    private ResourceSegmentModel resourceSegmentModel(String segmentationId, String key) {
        return ResourceSegmentModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .segmentationId(segmentationId)
                .version(0L)
                .key(key)
                .nameEn("Test")
                .nameRu("Test")
                .descriptionEn("Test")
                .descriptionRu("Test")
                .deleted(false)
                .build();
    }

    @SuppressWarnings("ParameterNumber")
    private ResourceModel resourceModel(String providerId, String key, String resourceTypeId,
                                        Set<Tuple2<String, String>> segments, String unitsEnsembleId,
                                        Set<String> allowedUnitIds, String defaultUnitId,
                                        String baseUnitId, String accountsSpaceId) {
        return DummyModels.resourceModel(providerId, key, resourceTypeId, unitsEnsembleId, allowedUnitIds,
                defaultUnitId, baseUnitId, builder -> builder
                        .segments(segments.stream().map(t -> new ResourceSegmentSettingsModel(t.getT1(), t.getT2()))
                                .collect(Collectors.toSet()))
                        .accountsSpacesId(accountsSpaceId)
        );
    }

    private FolderModel folderModel(long serviceId) {
        return FolderModel.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setServiceId(serviceId)
                .setVersion(0L)
                .setDisplayName("Test")
                .setDescription("Test")
                .setDeleted(false)
                .setFolderType(FolderType.COMMON)
                .setTags(Collections.emptySet())
                .setNextOpLogOrder(1L)
                .build();
    }

    private AccountModel accountModel(String providerId, String folderId) {
        return new AccountModel.Builder()
                .setId(UUID.randomUUID().toString())
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setVersion(0L)
                .setDisplayName("Test")
                .setDeleted(false)
                .setProviderId(providerId)
                .setFolderId(folderId)
                .setOuterAccountIdInProvider("test")
                .setOuterAccountKeyInProvider("test")
                .setLastAccountUpdate(Instant.now())
                .setFreeTier(false)
                .build();
    }

}
