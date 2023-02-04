package ru.yandex.intranet.d.services.operations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.backend.service.provider_proto.KnownProvision;
import ru.yandex.intranet.d.backend.service.provider_proto.ProvisionRequest;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsDao;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasOperationsDao;
import ru.yandex.intranet.d.dao.accounts.OperationsInProgressDao;
import ru.yandex.intranet.d.dao.folders.FolderDao;
import ru.yandex.intranet.d.dao.providers.ProvidersDao;
import ru.yandex.intranet.d.dao.quotas.QuotasDao;
import ru.yandex.intranet.d.dao.resources.ResourcesDao;
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao;
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao;
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService;
import ru.yandex.intranet.d.i18n.Locales;
import ru.yandex.intranet.d.model.accounts.AccountModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel;
import ru.yandex.intranet.d.model.accounts.OperationChangesModel;
import ru.yandex.intranet.d.model.accounts.OperationInProgressModel;
import ru.yandex.intranet.d.model.accounts.OperationOrdersModel;
import ru.yandex.intranet.d.model.accounts.OperationSource;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderType;
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.model.resources.ResourceModel;
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel;
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel;
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel;
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel;
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel;

/**
 * Update provision operations retry service test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@SuppressWarnings({"SameParameterValue"})
@IntegrationTest
public class YetAnotherUpdateProvisionOperationsRetryServiceTest {

    private static final String GRPC_URI = "in-process:test";

    @Autowired
    private ProvidersDao providersDao;
    @Autowired
    private AccountsDao accountsDao;
    @Autowired
    private FolderDao folderDao;
    @Autowired
    private AccountsQuotasOperationsDao accountsQuotasOperationsDao;
    @Autowired
    private OperationsInProgressDao operationsInProgressDao;
    @Autowired
    private ResourceTypesDao resourceTypesDao;
    @Autowired
    private ResourceSegmentationsDao resourceSegmentationsDao;
    @Autowired
    private ResourceSegmentsDao resourceSegmentsDao;
    @Autowired
    private ResourcesDao resourcesDao;
    @Autowired
    private QuotasDao quotasDao;
    @Autowired
    private AccountsQuotasDao accountsQuotasDao;
    @Autowired
    private YdbTableClient tableClient;
    @Autowired
    private StubProviderService stubProviderService;
    @Autowired
    private OperationsRetryService operationsRetryService;

    @Test
    public void testUpdateProvisionRetryProviderApiUnitIsSet() {
        ProviderModel provider = providerModel(GRPC_URI, null, false, false, true, true, false, false, false, false);
        FolderModel folder = folderModel(1L);
        ResourceTypeModel resourceType = resourceTypeModel(provider.getId(), "test",
                "b02344bf-96af-4cc5-937c-66a479989ce8");
        ResourceSegmentationModel locationSegmentation = resourceSegmentationModel(provider.getId(), "location");
        ResourceSegmentModel vlaSegment = resourceSegmentModel(locationSegmentation.getId(), "VLA");
        ResourceModel resource = resourceModel(provider.getId(), "test", resourceType.getId(),
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())),
                "b02344bf-96af-4cc5-937c-66a479989ce8",
                Set.of("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                        "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", null,
                "bb6b1e08-49a7-4cf8-b1b2-e8e71871d6d3");
        QuotaModel quota = quotaModel(provider.getId(), resource.getId(), folder.getId(), 300000L, 200000L, 40000L);
        AccountModel account = accountModel(provider.getId(), null, "test-id", "test",
                folder.getId(), "Test", null, null);
        AccountsQuotasModel provision = accountQuotaModel(provider.getId(), resource.getId(), folder.getId(),
                account.getId(), 60000L, 30000L, null, null);
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), null, account.getId(),
                Map.of(resource.getId(), 100000L), Map.of(resource.getId(), 40000L));
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                providersDao.upsertProviderRetryable(txSession, provider)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                                .upsertOneRetryable(txSession, folder)))
                .block();
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
                                .upsertResourceRetryable(txSession, resource)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                                .upsertOneRetryable(txSession, account)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                                .upsertOneRetryable(txSession, quota)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                                .upsertOneRetryable(txSession, provision)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                                .upsertOneRetryable(txSession, operation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> operationsInProgressDao
                                .upsertOneRetryable(txSession, inProgress)))
                .block();
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Assertions.assertNotNull(stubProviderService.getUpdateProvisionRequests());
        Assertions.assertEquals(1, stubProviderService.getUpdateProvisionRequests().size());
        Assertions.assertEquals(1, stubProviderService.getUpdateProvisionRequests()
                .getFirst().getT1().getUpdatedProvisionsList().size());
        Assertions.assertEquals(1, stubProviderService.getUpdateProvisionRequests()
                .getFirst().getT1().getKnownProvisionsList().size());
        Assertions.assertEquals(1, stubProviderService.getUpdateProvisionRequests()
                .getFirst().getT1().getKnownProvisions(0).getKnownProvisionsList().size());
        ProvisionRequest updatedProvisionsRequest = stubProviderService.getUpdateProvisionRequests()
                .getFirst().getT1().getUpdatedProvisions(0);
        KnownProvision knownProvisionsRequest = stubProviderService.getUpdateProvisionRequests()
                .getFirst().getT1().getKnownProvisions(0).getKnownProvisions(0);
        Assertions.assertNotNull(updatedProvisionsRequest);
        Assertions.assertNotNull(knownProvisionsRequest);
        Assertions.assertEquals(100L, updatedProvisionsRequest.getProvided().getValue());
        Assertions.assertEquals("kilobytes", updatedProvisionsRequest.getProvided().getUnitKey());
        Assertions.assertEquals(60L, knownProvisionsRequest.getProvided().getValue());
        Assertions.assertEquals("kilobytes", knownProvisionsRequest.getProvided().getUnitKey());
        stubProviderService.reset();
    }

    @Test
    public void testUpdateProvisionRetryProviderApiUnitIsNotSet() {
        ProviderModel provider = providerModel(GRPC_URI, null, false, false, true, true, false, false, false, false);
        FolderModel folder = folderModel(1L);
        ResourceTypeModel resourceType = resourceTypeModel(provider.getId(), "test",
                "b02344bf-96af-4cc5-937c-66a479989ce8");
        ResourceSegmentationModel locationSegmentation = resourceSegmentationModel(provider.getId(), "location");
        ResourceSegmentModel vlaSegment = resourceSegmentModel(locationSegmentation.getId(), "VLA");
        ResourceModel resource = resourceModel(provider.getId(), "test", resourceType.getId(),
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())),
                "b02344bf-96af-4cc5-937c-66a479989ce8",
                Set.of("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                        "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", null, null);
        QuotaModel quota = quotaModel(provider.getId(), resource.getId(), folder.getId(), 300000L, 200000L, 40000L);
        AccountModel account = accountModel(provider.getId(), null, "test-id", "test",
                folder.getId(), "Test", null, null);
        AccountsQuotasModel provision = accountQuotaModel(provider.getId(), resource.getId(), folder.getId(),
                account.getId(), 60000L, 30000L, null, null);
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), null, account.getId(),
                Map.of(resource.getId(), 100000L), Map.of(resource.getId(), 40000L));
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                providersDao.upsertProviderRetryable(txSession, provider)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                                .upsertOneRetryable(txSession, folder)))
                .block();
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
                                .upsertResourceRetryable(txSession, resource)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                                .upsertOneRetryable(txSession, account)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                                .upsertOneRetryable(txSession, quota)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                                .upsertOneRetryable(txSession, provision)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                                .upsertOneRetryable(txSession, operation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> operationsInProgressDao
                                .upsertOneRetryable(txSession, inProgress)))
                .block();
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Assertions.assertNotNull(stubProviderService.getUpdateProvisionRequests());
        Assertions.assertEquals(1, stubProviderService.getUpdateProvisionRequests().size());
        Assertions.assertEquals(1, stubProviderService.getUpdateProvisionRequests()
                .getFirst().getT1().getUpdatedProvisionsList().size());
        Assertions.assertEquals(1, stubProviderService.getUpdateProvisionRequests()
                .getFirst().getT1().getKnownProvisionsList().size());
        Assertions.assertEquals(1, stubProviderService.getUpdateProvisionRequests()
                .getFirst().getT1().getKnownProvisions(0).getKnownProvisionsList().size());
        ProvisionRequest updatedProvisionsRequest = stubProviderService.getUpdateProvisionRequests()
                .getFirst().getT1().getUpdatedProvisions(0);
        KnownProvision knownProvisionsRequest = stubProviderService.getUpdateProvisionRequests()
                .getFirst().getT1().getKnownProvisions(0).getKnownProvisions(0);
        Assertions.assertNotNull(updatedProvisionsRequest);
        Assertions.assertNotNull(knownProvisionsRequest);
        Assertions.assertEquals(100000L, updatedProvisionsRequest.getProvided().getValue());
        Assertions.assertEquals("bytes", updatedProvisionsRequest.getProvided().getUnitKey());
        Assertions.assertEquals(60000L, knownProvisionsRequest.getProvided().getValue());
        Assertions.assertEquals("bytes", knownProvisionsRequest.getProvided().getUnitKey());
        stubProviderService.reset();
    }

    @SuppressWarnings("ParameterNumber")
    private ProviderModel providerModel(String grpcUri, String restUri, boolean accountsSpacesSupported,
                                        boolean softDeleteSupported, boolean accountKeySupported,
                                        boolean accountDisplayNameSupported, boolean perAccountVersionSupported,
                                        boolean perProvisionVersionSupported, boolean perAccountLastUpdateSupported,
                                        boolean operationIdDeduplicationSupported) {
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
                        .displayNameSupported(accountDisplayNameSupported)
                        .keySupported(accountKeySupported)
                        .deleteSupported(true)
                        .softDeleteSupported(softDeleteSupported)
                        .moveSupported(true)
                        .renameSupported(true)
                        .perAccountVersionSupported(perAccountVersionSupported)
                        .perProvisionVersionSupported(perProvisionVersionSupported)
                        .perAccountLastUpdateSupported(perAccountLastUpdateSupported)
                        .perProvisionLastUpdateSupported(true)
                        .operationIdDeduplicationSupported(operationIdDeduplicationSupported)
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
                .setNextOpLogOrder(2L)
                .build();
    }

    private AccountsQuotasOperationsModel operationModel(String providerId,
                                                         String accountsSpaceId,
                                                         String accountId,
                                                         Map<String, Long> updatedProvisionsByResourceId,
                                                         Map<String, Long> frozenProvisionsByResourceId) {
        return AccountsQuotasOperationsModel.builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setOperationId(UUID.randomUUID().toString())
                .setLastRequestId(null)
                .setCreateDateTime(Instant.now().minus(Duration.ofMinutes(2)))
                .setOperationSource(OperationSource.USER)
                .setOperationType(AccountsQuotasOperationsModel.OperationType.UPDATE_PROVISION)
                .setAuthorUserId("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                .setAuthorUserUid("1120000000000001")
                .setProviderId(providerId)
                .setAccountsSpaceId(accountsSpaceId)
                .setUpdateDateTime(null)
                .setRequestStatus(AccountsQuotasOperationsModel.RequestStatus.WAITING)
                .setErrorMessage(null)
                .setRequestedChanges(OperationChangesModel.builder()
                        .accountId(accountId)
                        .updatedProvisions(updatedProvisionsByResourceId.entrySet().stream()
                                .map(v -> new OperationChangesModel.Provision(v.getKey(), v.getValue()))
                                .collect(Collectors.toList()))
                        .frozenProvisions(frozenProvisionsByResourceId.entrySet().stream()
                                .map(v -> new OperationChangesModel.Provision(v.getKey(), v.getValue()))
                                .collect(Collectors.toList()))
                        .build())
                .setOrders(OperationOrdersModel.builder()
                        .submitOrder(1L)
                        .build())
                .build();
    }

    private OperationInProgressModel inProgressModel(String operationId, String folderId) {
        return OperationInProgressModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .operationId(operationId)
                .folderId(folderId)
                .accountId(null)
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

    @SuppressWarnings("ParameterNumber")
    private ResourceModel resourceModel(String providerId, String key, String resourceTypeId,
                                        Set<Tuple2<String, String>> segments, String unitsEnsembleId,
                                        Set<String> allowedUnitIds, String defaultUnitId,
                                        String baseUnitId, String accountsSpaceId, String providerApiUnitId) {
        return ResourceModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .key(key)
                .nameEn("Test")
                .nameRu("Test")
                .descriptionEn("Test")
                .descriptionRu("Test")
                .deleted(false)
                .unitsEnsembleId(unitsEnsembleId)
                .providerId(providerId)
                .resourceTypeId(resourceTypeId)
                .segments(segments.stream().map(t -> new ResourceSegmentSettingsModel(t.getT1(), t.getT2()))
                        .collect(Collectors.toSet()))
                .resourceUnits(new ResourceUnitsModel(allowedUnitIds, defaultUnitId, providerApiUnitId))
                .managed(true)
                .orderable(true)
                .readOnly(false)
                .baseUnitId(baseUnitId)
                .accountsSpacesId(accountsSpaceId)
                .build();
    }

    private QuotaModel quotaModel(String providerId, String resourceId, String folderId, long quota, long balance,
                                  long frozen) {
        return QuotaModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .providerId(providerId)
                .resourceId(resourceId)
                .folderId(folderId)
                .quota(quota)
                .balance(balance)
                .frozenQuota(frozen)
                .build();
    }

    @SuppressWarnings("ParameterNumber")
    private AccountModel accountModel(String providerId, String accountsSpaceId, String externalId, String externalKey,
                                      String folderId, String displayName, Long lastReceivedVersion,
                                      String lastOpId) {
        return new AccountModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setId(UUID.randomUUID().toString())
                .setVersion(0L)
                .setProviderId(providerId)
                .setAccountsSpacesId(accountsSpaceId)
                .setOuterAccountIdInProvider(externalId)
                .setOuterAccountKeyInProvider(externalKey)
                .setFolderId(folderId)
                .setDisplayName(displayName)
                .setDeleted(false)
                .setLastAccountUpdate(Instant.now())
                .setLastReceivedVersion(lastReceivedVersion)
                .setLatestSuccessfulAccountOperationId(lastOpId)
                .build();
    }

    @SuppressWarnings("ParameterNumber")
    private AccountsQuotasModel accountQuotaModel(String providerId, String resourceId, String folderId,
                                                  String accountId, long provided, long allocated,
                                                  Long lastReceivedVersion, String lastOpId) {
        return new AccountsQuotasModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setProviderId(providerId)
                .setResourceId(resourceId)
                .setFolderId(folderId)
                .setAccountId(accountId)
                .setProvidedQuota(provided)
                .setAllocatedQuota(allocated)
                .setLastProvisionUpdate(Instant.now())
                .setLastReceivedProvisionVersion(lastReceivedVersion)
                .setLatestSuccessfulProvisionOperationId(lastOpId)
                .build();
    }

}
