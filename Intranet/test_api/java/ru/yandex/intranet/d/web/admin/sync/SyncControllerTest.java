package ru.yandex.intranet.d.web.admin.sync;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.protobuf.util.Timestamps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.backend.service.provider_proto.Account;
import ru.yandex.intranet.d.backend.service.provider_proto.Amount;
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundResourceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.CurrentVersion;
import ru.yandex.intranet.d.backend.service.provider_proto.LastUpdate;
import ru.yandex.intranet.d.backend.service.provider_proto.ListAccountsResponse;
import ru.yandex.intranet.d.backend.service.provider_proto.PassportUID;
import ru.yandex.intranet.d.backend.service.provider_proto.Provision;
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceSegmentKey;
import ru.yandex.intranet.d.backend.service.provider_proto.StaffLogin;
import ru.yandex.intranet.d.backend.service.provider_proto.UserID;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsDao;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao;
import ru.yandex.intranet.d.dao.folders.FolderDao;
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao;
import ru.yandex.intranet.d.dao.providers.ProvidersDao;
import ru.yandex.intranet.d.dao.quotas.QuotasDao;
import ru.yandex.intranet.d.dao.resources.ResourcesDao;
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao;
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao;
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse;
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService;
import ru.yandex.intranet.d.model.WithTenant;
import ru.yandex.intranet.d.model.accounts.AccountModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
import ru.yandex.intranet.d.model.folders.FolderOperationType;
import ru.yandex.intranet.d.model.folders.FolderType;
import ru.yandex.intranet.d.model.folders.ProvisionHistoryModel;
import ru.yandex.intranet.d.model.folders.ProvisionsByResource;
import ru.yandex.intranet.d.model.folders.QuotasByAccount;
import ru.yandex.intranet.d.model.folders.QuotasByResource;
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.model.resources.ResourceModel;
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel;
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel;
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel;
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel;
import ru.yandex.intranet.d.utils.DbHelper;
import ru.yandex.intranet.d.utils.DummyModels;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.SortOrderDto;

import static com.yandex.ydb.table.transaction.TransactionMode.SERIALIZABLE_READ_WRITE;
import static ru.yandex.intranet.d.UnitIds.BYTES;
import static ru.yandex.intranet.d.UnitIds.GIGABYTES;
import static ru.yandex.intranet.d.UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID;

/**
 * Sync admin controller test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "SameParameterValue"})
@IntegrationTest
public class SyncControllerTest {

    private static final String GRPC_URI = "in-process:test";

    @Autowired
    private WebTestClient webClient;
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
    private QuotasDao quotasDao;
    @Autowired
    private AccountsDao accountsDao;
    @Autowired
    private AccountsQuotasDao accountsQuotasDao;
    @Autowired
    private FolderOperationLogDao folderOperationLogDao;
    @Autowired
    private FolderDao folderDao;
    @Autowired
    private YdbTableClient tableClient;
    @Autowired
    private StubProviderService stubProviderService;
    @Autowired
    private DbHelper dbHelper;

    @Test
    @SuppressWarnings("MethodLength")
    public void syncTest() {
        ProviderModel provider = providerModel(GRPC_URI, null, false);
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
        FolderModel folder = folderModel(1L);
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                SERIALIZABLE_READ_WRITE, txSession -> providersDao.upsertProviderRetryable(txSession, provider)))
                .block();
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
                        .upsertOneRetryable(txSession, folder)))
                .block();
        String accountOperationId = UUID.randomUUID().toString();
        String provisionOperationId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        stubProviderService.setListAccountsResponses(List.of(GrpcResponse
                .success(ListAccountsResponse.newBuilder()
                        .addAccounts(Account.newBuilder()
                                .setAccountId("sync-test-1")
                                .setDeleted(false)
                                .setDisplayName("sync-test-1")
                                .setFolderId(folder.getId())
                                .setKey("sync-test-1")
                                .setVersion(CurrentVersion.newBuilder().setVersion(0L).build())
                                .setLastUpdate(LastUpdate.newBuilder()
                                        .setAuthor(UserID.newBuilder()
                                                .setPassportUid(PassportUID.newBuilder()
                                                        .setPassportUid("1120000000000001")
                                                        .build())
                                                .setStaffLogin(StaffLogin.newBuilder()
                                                        .setStaffLogin("login-1")
                                                        .build())
                                                .build())
                                        .setOperationId(accountOperationId)
                                        .setTimestamp(Timestamps.fromMillis(now.toEpochMilli()))
                                        .build())
                                .addProvisions(Provision.newBuilder()
                                        .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("test")
                                                        .addAllResourceSegmentKeys(List.of(
                                                                ResourceSegmentKey.newBuilder()
                                                                        .setResourceSegmentationKey("location")
                                                                        .setResourceSegmentKey("VLA")
                                                                        .build()
                                                        ))
                                                        .build())
                                                .build())
                                        .setLastUpdate(LastUpdate.newBuilder()
                                                .setAuthor(UserID.newBuilder()
                                                        .setPassportUid(PassportUID.newBuilder()
                                                                .setPassportUid("1120000000000001")
                                                                .build())
                                                        .setStaffLogin(StaffLogin.newBuilder()
                                                                .setStaffLogin("login-1")
                                                                .build())
                                                        .build())
                                                .setOperationId(provisionOperationId)
                                                .setTimestamp(Timestamps.fromMillis(now.toEpochMilli()))
                                                .build())
                                        .setProvided(Amount.newBuilder()
                                                .setValue(2)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .setAllocated(Amount.newBuilder()
                                                .setValue(1)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .setVersion(CurrentVersion.newBuilder().setVersion(0L).build())
                                        .build())
                                .build())
                        .build())));
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/sync/_execute")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();
        Optional<AccountModel> syncedAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "sync-test-1", null)))))
                .block();
        Assertions.assertNotNull(syncedAccount);
        Assertions.assertTrue(syncedAccount.isPresent());
        Assertions.assertEquals(provider.getId(), syncedAccount.get().getProviderId());
        Assertions.assertEquals("sync-test-1", syncedAccount.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("sync-test-1", syncedAccount.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), syncedAccount.get().getFolderId());
        Assertions.assertEquals("sync-test-1", syncedAccount.get().getDisplayName().get());
        Assertions.assertFalse(syncedAccount.get().isDeleted());
        Assertions.assertEquals(0L, syncedAccount.get().getLastReceivedVersion().get());
        Assertions.assertTrue(syncedAccount.get().getLatestSuccessfulAccountOperationId().isEmpty());
        List<QuotaModel> syncedQuotas = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folder.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(syncedQuotas);
        Assertions.assertEquals(1, syncedQuotas.size());
        Assertions.assertEquals(folder.getId(), syncedQuotas.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), syncedQuotas.get(0).getProviderId());
        Assertions.assertEquals(resource.getId(), syncedQuotas.get(0).getResourceId());
        Assertions.assertEquals(0L, syncedQuotas.get(0).getQuota());
        Assertions.assertEquals(-2_000_000_000L, syncedQuotas.get(0).getBalance());
        Assertions.assertEquals(0L, syncedQuotas.get(0).getFrozenQuota());
        List<AccountsQuotasModel> syncedProvisions = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                        .getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID, Set.of(syncedAccount.get().getId()))))
                .block();
        Assertions.assertNotNull(syncedProvisions);
        Assertions.assertEquals(1, syncedProvisions.size());
        Assertions.assertEquals(syncedAccount.get().getId(), syncedProvisions.get(0).getAccountId());
        Assertions.assertEquals(resource.getId(), syncedProvisions.get(0).getResourceId());
        Assertions.assertEquals(folder.getId(), syncedProvisions.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), syncedProvisions.get(0).getProviderId());
        Assertions.assertEquals(2_000_000_000L, syncedProvisions.get(0).getProvidedQuota());
        Assertions.assertEquals(1_000_000_000L, syncedProvisions.get(0).getAllocatedQuota());
        Assertions.assertEquals(0L, syncedProvisions.get(0).getLastReceivedProvisionVersion().get());
        Assertions.assertTrue(syncedProvisions.get(0).getLatestSuccessfulProvisionOperationId().isEmpty());
        List<FolderOperationLogModel> syncedOpLog = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
                        .getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folder.getId(), SortOrderDto.ASC,
                                100)))
                .block();
        Assertions.assertNotNull(syncedOpLog);
        Assertions.assertEquals(1, syncedOpLog.size());
        Assertions.assertEquals(folder.getId(), syncedOpLog.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.SYNC_WITH_PROVIDER, syncedOpLog.get(0).getOperationType());
        Assertions.assertEquals("0b204534-d0ec-452d-99fe-a3d1da5a49a9", syncedOpLog.get(0).getAuthorUserId().get());
        Assertions.assertEquals("1120000000000001", syncedOpLog.get(0).getAuthorUserUid().get());
        Assertions.assertTrue(syncedOpLog.get(0).getAuthorProviderId().isEmpty());
        Assertions.assertTrue(syncedOpLog.get(0).getSourceFolderOperationsLogId().isEmpty());
        Assertions.assertTrue(syncedOpLog.get(0).getOldFolderFields().isEmpty());
        Assertions.assertTrue(syncedOpLog.get(0).getNewFolderFields().isEmpty());
        Assertions.assertEquals(new QuotasByResource(Map.of()), syncedOpLog.get(0).getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of()), syncedOpLog.get(0).getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of(resource.getId(), 0L)),
                syncedOpLog.get(0).getOldBalance());
        Assertions.assertEquals(new QuotasByResource(Map.of(resource.getId(), -2_000_000_000L)),
                syncedOpLog.get(0).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(syncedAccount.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(0L, null))))),
                syncedOpLog.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(syncedAccount.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(2_000_000_000L, 0L))))),
                syncedOpLog.get(0).getNewProvisions());
        Assertions.assertTrue(syncedOpLog.get(0).getOldAccounts().isEmpty());
        Assertions.assertTrue(syncedOpLog.get(0).getNewAccounts().isPresent());
        Assertions.assertTrue(syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .containsKey(syncedAccount.get().getId()));
        Assertions.assertEquals(provider.getId(), syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccount.get().getId()).getProviderId().get());
        Assertions.assertEquals("sync-test-1", syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccount.get().getId()).getOuterAccountIdInProvider().get());
        Assertions.assertEquals("sync-test-1", syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccount.get().getId()).getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccount.get().getId()).getFolderId().get());
        Assertions.assertEquals("sync-test-1", syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccount.get().getId()).getDisplayName().get());
        Assertions.assertFalse(syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccount.get().getId()).getDeleted().get());
        Assertions.assertEquals(0L, syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccount.get().getId()).getLastReceivedVersion().get());
        Assertions.assertEquals(1L, syncedOpLog.get(0).getOrder());
        Assertions.assertTrue(syncedOpLog.get(0).getAccountsQuotasOperationsId().isEmpty());
        stubProviderService.reset();
    }

    @Test
    public void syncTestDefaultQuotas() {
        long defaultQuota = 1048576L;
        ProviderModel provider = providerModel(GRPC_URI, null, false);
        ResourceTypeModel resourceType = resourceTypeModel(provider.getId(), "default", STORAGE_UNITS_DECIMAL_ID);
        ResourceModel resource = DummyModels.resourceModel(provider.getId(), "default", resourceType.getId(),
                STORAGE_UNITS_DECIMAL_ID, Set.of(GIGABYTES, BYTES), GIGABYTES, BYTES, builder -> builder
                        .defaultQuota(defaultQuota));
        FolderModel folder = folderModel(1L);
        dbHelper.upsertProvider(provider);
        dbHelper.upsertResourceType(resourceType);
        dbHelper.upsertResource(resource);
        dbHelper.upsertFolder(folder);

        LastUpdate lastUpdate = LastUpdate.newBuilder()
                .setAuthor(UserID.newBuilder()
                        .setPassportUid(PassportUID.newBuilder()
                                .setPassportUid("1120000000000001")
                                .build())
                        .setStaffLogin(StaffLogin.newBuilder()
                                .setStaffLogin("login-1")
                                .build())
                        .build())
                .setOperationId(UUID.randomUUID().toString())
                .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                .build();
        stubProviderService.setListAccountsResponses(List.of(GrpcResponse
                .success(ListAccountsResponse.newBuilder()
                        .addAccounts(Account.newBuilder()
                                .setAccountId("sync-test-1")
                                .setDeleted(false)
                                .setDisplayName("sync-test-1")
                                .setFolderId(folder.getId())
                                .setKey("sync-test-1")
                                .setVersion(CurrentVersion.newBuilder().setVersion(0L))
                                .setLastUpdate(lastUpdate)
                                .addProvisions(Provision.newBuilder()
                                        .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("default")
                                                        .addAllResourceSegmentKeys(List.of()))) // ?
                                        .setLastUpdate(lastUpdate)
                                        .setProvided(Amount.newBuilder()
                                                .setValue(defaultQuota + 1).setUnitKey("bytes"))
                                        .setAllocated(Amount.newBuilder()
                                                .setValue(1).setUnitKey("bytes"))
                                        .setVersion(CurrentVersion.newBuilder().setVersion(0L))))
                        .build())));
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/sync/_execute")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();

        List<QuotaModel> syncedQuotas = dbHelper.readDb(tx -> quotasDao
                .getByFolders(tx, List.of(folder.getId()), Tenants.DEFAULT_TENANT_ID));
        Assertions.assertNotNull(syncedQuotas);
        Assertions.assertEquals(1, syncedQuotas.size());
        Assertions.assertEquals(defaultQuota, syncedQuotas.get(0).getQuota());
        Assertions.assertEquals(-1L, syncedQuotas.get(0).getBalance());

        List<FolderOperationLogModel> syncedOpLog = dbHelper.readDb(tx -> folderOperationLogDao
                .getFirstPageByFolder(tx, folder.getTenantId(), folder.getId(), SortOrderDto.ASC, 100));

        Assertions.assertNotNull(syncedOpLog);
        Assertions.assertEquals(1, syncedOpLog.size());
        Assertions.assertEquals(new QuotasByResource(Map.of(resource.getId(), -1L)),
                syncedOpLog.get(0).getNewBalance());
        Assertions.assertEquals(new QuotasByResource(Map.of(resource.getId(), defaultQuota)),
                syncedOpLog.get(0).getNewQuotas());

        stubProviderService.reset();
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

}
