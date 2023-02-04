package ru.yandex.intranet.d.services.sync;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.protobuf.util.Timestamps;
import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.backend.service.provider_proto.Account;
import ru.yandex.intranet.d.backend.service.provider_proto.AccountsSpaceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.Amount;
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundAccountsSpaceKey;
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
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasOperationsDao;
import ru.yandex.intranet.d.dao.accounts.AccountsSpacesDao;
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
import ru.yandex.intranet.d.i18n.Locales;
import ru.yandex.intranet.d.model.WithTenant;
import ru.yandex.intranet.d.model.accounts.AccountModel;
import ru.yandex.intranet.d.model.accounts.AccountSpaceModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel;
import ru.yandex.intranet.d.model.accounts.OperationChangesModel;
import ru.yandex.intranet.d.model.accounts.OperationOrdersModel;
import ru.yandex.intranet.d.model.accounts.OperationSource;
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
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel;
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel;
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel;
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel;
import ru.yandex.intranet.d.web.model.SortOrderDto;

/**
 * Tests fo service to sync providers accounts and quotas.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class MoreAccountsSyncServiceTest {

    private static final String GRPC_URI = "in-process:test";

    @Autowired
    private AccountsSyncService accountsSyncService;
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
    private AccountsSpacesDao accountsSpacesDao;
    @Autowired
    private AccountsQuotasOperationsDao accountsQuotasOperationsDao;
    @Autowired
    private YdbTableClient tableClient;
    @Autowired
    private StubProviderService stubProviderService;

    @Test
    @SuppressWarnings("MethodLength")
    public void testBasicSyncCreateThenHardDeleteTwoAccountsInFolder() {
        Clock baseClock = Clock.systemUTC();
        ProviderModel provider = providerModel(GRPC_URI, null, false, false);
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
                        .upsertResourceRetryable(txSession, resource)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
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
                                .addAccounts(Account.newBuilder()
                                        .setAccountId("sync-test-2")
                                        .setDeleted(false)
                                        .setDisplayName("sync-test-2")
                                        .setFolderId(folder.getId())
                                        .setKey("sync-test-2")
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
                                .build()),
                GrpcResponse.success(ListAccountsResponse.newBuilder()
                        .addAccounts(Account.newBuilder()
                                .setAccountId("sync-test-2")
                                .setDeleted(false)
                                .setDisplayName("sync-test-2")
                                .setFolderId(folder.getId())
                                .setKey("sync-test-2")
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
                                .build()).build())));
        accountsSyncService.syncOneProvider(provider, Locales.ENGLISH, baseClock).block();
        Optional<AccountModel> syncedAccountOne = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "sync-test-1", null)))))
                .block();
        Optional<AccountModel> syncedAccountTwo = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "sync-test-2", null)))))
                .block();
        Assertions.assertNotNull(syncedAccountOne);
        Assertions.assertTrue(syncedAccountOne.isPresent());
        Assertions.assertEquals(provider.getId(), syncedAccountOne.get().getProviderId());
        Assertions.assertEquals("sync-test-1", syncedAccountOne.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("sync-test-1", syncedAccountOne.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), syncedAccountOne.get().getFolderId());
        Assertions.assertEquals("sync-test-1", syncedAccountOne.get().getDisplayName().get());
        Assertions.assertFalse(syncedAccountOne.get().isDeleted());
        Assertions.assertEquals(0L, syncedAccountOne.get().getLastReceivedVersion().get());
        Assertions.assertTrue(syncedAccountOne.get().getLatestSuccessfulAccountOperationId().isEmpty());
        Assertions.assertNotNull(syncedAccountTwo);
        Assertions.assertTrue(syncedAccountTwo.isPresent());
        Assertions.assertEquals(provider.getId(), syncedAccountTwo.get().getProviderId());
        Assertions.assertEquals("sync-test-2", syncedAccountTwo.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("sync-test-2", syncedAccountTwo.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), syncedAccountTwo.get().getFolderId());
        Assertions.assertEquals("sync-test-2", syncedAccountTwo.get().getDisplayName().get());
        Assertions.assertFalse(syncedAccountTwo.get().isDeleted());
        Assertions.assertEquals(0L, syncedAccountTwo.get().getLastReceivedVersion().get());
        Assertions.assertTrue(syncedAccountTwo.get().getLatestSuccessfulAccountOperationId().isEmpty());
        List<QuotaModel> syncedQuotas = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folder.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(syncedQuotas);
        Assertions.assertEquals(1, syncedQuotas.size());
        Assertions.assertEquals(folder.getId(), syncedQuotas.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), syncedQuotas.get(0).getProviderId());
        Assertions.assertEquals(resource.getId(), syncedQuotas.get(0).getResourceId());
        Assertions.assertEquals(0L, syncedQuotas.get(0).getQuota());
        Assertions.assertEquals(-4_000_000_000L, syncedQuotas.get(0).getBalance());
        Assertions.assertEquals(0L, syncedQuotas.get(0).getFrozenQuota());
        List<AccountsQuotasModel> syncedProvisionsOne = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                        .getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID,
                                Set.of(syncedAccountOne.get().getId()))))
                .block();
        List<AccountsQuotasModel> syncedProvisionsTwo = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                        .getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID,
                                Set.of(syncedAccountTwo.get().getId()))))
                .block();
        Assertions.assertNotNull(syncedProvisionsOne);
        Assertions.assertEquals(1, syncedProvisionsOne.size());
        Assertions.assertEquals(syncedAccountOne.get().getId(), syncedProvisionsOne.get(0).getAccountId());
        Assertions.assertEquals(resource.getId(), syncedProvisionsOne.get(0).getResourceId());
        Assertions.assertEquals(folder.getId(), syncedProvisionsOne.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), syncedProvisionsOne.get(0).getProviderId());
        Assertions.assertEquals(2_000_000_000L, syncedProvisionsOne.get(0).getProvidedQuota());
        Assertions.assertEquals(1_000_000_000L, syncedProvisionsOne.get(0).getAllocatedQuota());
        Assertions.assertEquals(0L, syncedProvisionsOne.get(0).getLastReceivedProvisionVersion().get());
        Assertions.assertTrue(syncedProvisionsOne.get(0).getLatestSuccessfulProvisionOperationId().isEmpty());
        Assertions.assertNotNull(syncedProvisionsTwo);
        Assertions.assertEquals(1, syncedProvisionsTwo.size());
        Assertions.assertEquals(syncedAccountTwo.get().getId(), syncedProvisionsTwo.get(0).getAccountId());
        Assertions.assertEquals(resource.getId(), syncedProvisionsTwo.get(0).getResourceId());
        Assertions.assertEquals(folder.getId(), syncedProvisionsTwo.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), syncedProvisionsTwo.get(0).getProviderId());
        Assertions.assertEquals(2_000_000_000L, syncedProvisionsTwo.get(0).getProvidedQuota());
        Assertions.assertEquals(1_000_000_000L, syncedProvisionsTwo.get(0).getAllocatedQuota());
        Assertions.assertEquals(0L, syncedProvisionsTwo.get(0).getLastReceivedProvisionVersion().get());
        Assertions.assertTrue(syncedProvisionsTwo.get(0).getLatestSuccessfulProvisionOperationId().isEmpty());
        List<FolderOperationLogModel> syncedOpLog = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
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
        Assertions.assertEquals(new QuotasByResource(Map.of(resource.getId(), -4_000_000_000L)),
                syncedOpLog.get(0).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(syncedAccountOne.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(0L, null))),
                syncedAccountTwo.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(0L, null))))),
                syncedOpLog.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(syncedAccountOne.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(2_000_000_000L, 0L))),
                syncedAccountTwo.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(2_000_000_000L, 0L))))),
                syncedOpLog.get(0).getNewProvisions());
        Assertions.assertTrue(syncedOpLog.get(0).getOldAccounts().isEmpty());
        Assertions.assertTrue(syncedOpLog.get(0).getNewAccounts().isPresent());
        Assertions.assertTrue(syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .containsKey(syncedAccountOne.get().getId()));
        Assertions.assertTrue(syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .containsKey(syncedAccountTwo.get().getId()));
        Assertions.assertEquals(provider.getId(), syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountOne.get().getId()).getProviderId().get());
        Assertions.assertEquals("sync-test-1", syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountOne.get().getId()).getOuterAccountIdInProvider().get());
        Assertions.assertEquals("sync-test-1", syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountOne.get().getId()).getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountOne.get().getId()).getFolderId().get());
        Assertions.assertEquals("sync-test-1", syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountOne.get().getId()).getDisplayName().get());
        Assertions.assertFalse(syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountOne.get().getId()).getDeleted().get());
        Assertions.assertEquals(0L, syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountOne.get().getId()).getLastReceivedVersion().get());
        Assertions.assertEquals(provider.getId(), syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountTwo.get().getId()).getProviderId().get());
        Assertions.assertEquals("sync-test-2", syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountTwo.get().getId()).getOuterAccountIdInProvider().get());
        Assertions.assertEquals("sync-test-2", syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountTwo.get().getId()).getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountTwo.get().getId()).getFolderId().get());
        Assertions.assertEquals("sync-test-2", syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountTwo.get().getId()).getDisplayName().get());
        Assertions.assertFalse(syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountTwo.get().getId()).getDeleted().get());
        Assertions.assertEquals(0L, syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountTwo.get().getId()).getLastReceivedVersion().get());
        Assertions.assertEquals(1L, syncedOpLog.get(0).getOrder());
        Assertions.assertTrue(syncedOpLog.get(0).getAccountsQuotasOperationsId().isEmpty());
        accountsSyncService.syncOneProvider(provider, Locales.ENGLISH, Clock.offset(baseClock, Duration.ofMinutes(10)))
                .block();
        Optional<AccountModel> syncedAgainAccountOne = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "sync-test-1", null)))))
                .block();
        Optional<AccountModel> syncedAgainAccountTwo = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "sync-test-2", null)))))
                .block();
        Assertions.assertNotNull(syncedAgainAccountOne);
        Assertions.assertTrue(syncedAgainAccountOne.isPresent());
        Assertions.assertEquals(provider.getId(), syncedAgainAccountOne.get().getProviderId());
        Assertions.assertEquals("sync-test-1", syncedAgainAccountOne.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("sync-test-1", syncedAgainAccountOne.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), syncedAgainAccountOne.get().getFolderId());
        Assertions.assertEquals("sync-test-1", syncedAgainAccountOne.get().getDisplayName().get());
        Assertions.assertTrue(syncedAgainAccountOne.get().isDeleted());
        Assertions.assertEquals(0L, syncedAgainAccountOne.get().getLastReceivedVersion().get());
        Assertions.assertTrue(syncedAgainAccountOne.get().getLatestSuccessfulAccountOperationId().isEmpty());
        Assertions.assertNotNull(syncedAgainAccountTwo);
        Assertions.assertTrue(syncedAgainAccountTwo.isPresent());
        Assertions.assertEquals(provider.getId(), syncedAgainAccountTwo.get().getProviderId());
        Assertions.assertEquals("sync-test-2", syncedAgainAccountTwo.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("sync-test-2", syncedAgainAccountTwo.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), syncedAgainAccountTwo.get().getFolderId());
        Assertions.assertEquals("sync-test-2", syncedAgainAccountTwo.get().getDisplayName().get());
        Assertions.assertFalse(syncedAgainAccountTwo.get().isDeleted());
        Assertions.assertEquals(0L, syncedAgainAccountTwo.get().getLastReceivedVersion().get());
        Assertions.assertTrue(syncedAgainAccountTwo.get().getLatestSuccessfulAccountOperationId().isEmpty());
        List<QuotaModel> syncedAgainQuotas = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folder.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(syncedAgainQuotas);
        Assertions.assertEquals(1, syncedAgainQuotas.size());
        Assertions.assertEquals(folder.getId(), syncedAgainQuotas.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), syncedAgainQuotas.get(0).getProviderId());
        Assertions.assertEquals(resource.getId(), syncedAgainQuotas.get(0).getResourceId());
        Assertions.assertEquals(0L, syncedAgainQuotas.get(0).getQuota());
        Assertions.assertEquals(-2_000_000_000L, syncedAgainQuotas.get(0).getBalance());
        Assertions.assertEquals(0L, syncedAgainQuotas.get(0).getFrozenQuota());
        List<AccountsQuotasModel> syncedAgainProvisionsOne = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                        .getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID,
                                Set.of(syncedAccountOne.get().getId()))))
                .block();
        List<AccountsQuotasModel> syncedAgainProvisionsTwo = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                        .getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID,
                                Set.of(syncedAccountTwo.get().getId()))))
                .block();
        Assertions.assertNotNull(syncedAgainProvisionsOne);
        Assertions.assertEquals(1, syncedAgainProvisionsOne.size());
        Assertions.assertEquals(syncedAccountOne.get().getId(), syncedAgainProvisionsOne.get(0).getAccountId());
        Assertions.assertEquals(resource.getId(), syncedAgainProvisionsOne.get(0).getResourceId());
        Assertions.assertEquals(folder.getId(), syncedAgainProvisionsOne.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), syncedAgainProvisionsOne.get(0).getProviderId());
        Assertions.assertEquals(0L, syncedAgainProvisionsOne.get(0).getProvidedQuota());
        Assertions.assertEquals(0L, syncedAgainProvisionsOne.get(0).getAllocatedQuota());
        Assertions.assertTrue(syncedAgainProvisionsOne.get(0).getLastReceivedProvisionVersion().isEmpty());
        Assertions.assertTrue(syncedAgainProvisionsOne.get(0).getLatestSuccessfulProvisionOperationId().isEmpty());
        Assertions.assertNotNull(syncedAgainProvisionsTwo);
        Assertions.assertEquals(1, syncedAgainProvisionsTwo.size());
        Assertions.assertEquals(syncedAccountTwo.get().getId(), syncedAgainProvisionsTwo.get(0).getAccountId());
        Assertions.assertEquals(resource.getId(), syncedAgainProvisionsTwo.get(0).getResourceId());
        Assertions.assertEquals(folder.getId(), syncedAgainProvisionsTwo.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), syncedAgainProvisionsTwo.get(0).getProviderId());
        Assertions.assertEquals(2_000_000_000L, syncedAgainProvisionsTwo.get(0).getProvidedQuota());
        Assertions.assertEquals(1_000_000_000L, syncedAgainProvisionsTwo.get(0).getAllocatedQuota());
        Assertions.assertEquals(0L, syncedAgainProvisionsTwo.get(0).getLastReceivedProvisionVersion().get());
        Assertions.assertTrue(syncedAgainProvisionsTwo.get(0).getLatestSuccessfulProvisionOperationId().isEmpty());
        List<FolderOperationLogModel> syncedAgainOpLog = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
                        .getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folder.getId(), SortOrderDto.ASC,
                                100)))
                .block();
        Assertions.assertNotNull(syncedAgainOpLog);
        Assertions.assertEquals(2, syncedAgainOpLog.size());
        syncedAgainOpLog.sort(Comparator.comparing(FolderOperationLogModel::getOrder));
        Assertions.assertEquals(folder.getId(), syncedAgainOpLog.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.SYNC_WITH_PROVIDER, syncedAgainOpLog.get(0).getOperationType());
        Assertions.assertEquals("0b204534-d0ec-452d-99fe-a3d1da5a49a9",
                syncedAgainOpLog.get(0).getAuthorUserId().get());
        Assertions.assertEquals("1120000000000001", syncedAgainOpLog.get(0).getAuthorUserUid().get());
        Assertions.assertTrue(syncedAgainOpLog.get(0).getAuthorProviderId().isEmpty());
        Assertions.assertTrue(syncedAgainOpLog.get(0).getSourceFolderOperationsLogId().isEmpty());
        Assertions.assertTrue(syncedAgainOpLog.get(0).getOldFolderFields().isEmpty());
        Assertions.assertTrue(syncedAgainOpLog.get(0).getNewFolderFields().isEmpty());
        Assertions.assertEquals(new QuotasByResource(Map.of()), syncedAgainOpLog.get(0).getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of()), syncedAgainOpLog.get(0).getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of(resource.getId(), 0L)),
                syncedAgainOpLog.get(0).getOldBalance());
        Assertions.assertEquals(new QuotasByResource(Map.of(resource.getId(), -4_000_000_000L)),
                syncedAgainOpLog.get(0).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(syncedAgainAccountOne.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(0L, null))),
                syncedAgainAccountTwo.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(0L, null))))),
                syncedAgainOpLog.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(syncedAgainAccountOne.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(2_000_000_000L, 0L))),
                syncedAgainAccountTwo.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(2_000_000_000L, 0L))))),
                syncedAgainOpLog.get(0).getNewProvisions());
        Assertions.assertTrue(syncedAgainOpLog.get(0).getOldAccounts().isEmpty());
        Assertions.assertTrue(syncedAgainOpLog.get(0).getNewAccounts().isPresent());
        Assertions.assertTrue(syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .containsKey(syncedAgainAccountOne.get().getId()));
        Assertions.assertTrue(syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .containsKey(syncedAgainAccountTwo.get().getId()));
        Assertions.assertEquals(provider.getId(), syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getProviderId().get());
        Assertions.assertEquals("sync-test-1", syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getOuterAccountIdInProvider().get());
        Assertions.assertEquals("sync-test-1", syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getFolderId().get());
        Assertions.assertEquals("sync-test-1", syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getDisplayName().get());
        Assertions.assertFalse(syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getDeleted().get());
        Assertions.assertEquals(0L, syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getLastReceivedVersion().get());
        Assertions.assertEquals(provider.getId(), syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountTwo.get().getId()).getProviderId().get());
        Assertions.assertEquals("sync-test-2", syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountTwo.get().getId()).getOuterAccountIdInProvider().get());
        Assertions.assertEquals("sync-test-2", syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountTwo.get().getId()).getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountTwo.get().getId()).getFolderId().get());
        Assertions.assertEquals("sync-test-2", syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountTwo.get().getId()).getDisplayName().get());
        Assertions.assertFalse(syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountTwo.get().getId()).getDeleted().get());
        Assertions.assertEquals(0L, syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountTwo.get().getId()).getLastReceivedVersion().get());
        Assertions.assertEquals(1L, syncedAgainOpLog.get(0).getOrder());
        Assertions.assertTrue(syncedAgainOpLog.get(0).getAccountsQuotasOperationsId().isEmpty());
        Assertions.assertEquals(folder.getId(), syncedAgainOpLog.get(1).getFolderId());
        Assertions.assertEquals(FolderOperationType.SYNC_WITH_PROVIDER, syncedAgainOpLog.get(1).getOperationType());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getAuthorUserId().isEmpty());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getAuthorUserUid().isEmpty());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getAuthorProviderId().isPresent());
        Assertions.assertEquals(provider.getId(), syncedAgainOpLog.get(1).getAuthorProviderId().get());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getSourceFolderOperationsLogId().isEmpty());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getOldFolderFields().isEmpty());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getNewFolderFields().isEmpty());
        Assertions.assertEquals(new QuotasByResource(Map.of()), syncedAgainOpLog.get(1).getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of()), syncedAgainOpLog.get(1).getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of(resource.getId(), -4_000_000_000L)),
                syncedAgainOpLog.get(1).getOldBalance());
        Assertions.assertEquals(new QuotasByResource(Map.of(resource.getId(), -2_000_000_000L)),
                syncedAgainOpLog.get(1).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(syncedAgainAccountOne.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(2_000_000_000L, 0L))))),
                syncedAgainOpLog.get(1).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(syncedAgainAccountOne.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(0L, null))))),
                syncedAgainOpLog.get(1).getNewProvisions());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getOldAccounts().isPresent());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getOldAccounts().get().getAccounts()
                .containsKey(syncedAgainAccountOne.get().getId()));
        Assertions.assertFalse(syncedAgainOpLog.get(1).getOldAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getDeleted().get());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getNewAccounts().isPresent());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getNewAccounts().get().getAccounts()
                .containsKey(syncedAgainAccountOne.get().getId()));
        Assertions.assertTrue(syncedAgainOpLog.get(1).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getDeleted().get());
        Assertions.assertEquals(2L, syncedAgainOpLog.get(1).getOrder());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getAccountsQuotasOperationsId().isEmpty());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testBasicSyncCreateThenSoftDeleteTwoAccountsInFolder() {
        Clock baseClock = Clock.systemUTC();
        ProviderModel provider = providerModel(GRPC_URI, null, false, true);
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
                        .upsertResourceRetryable(txSession, resource)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
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
                                .addAccounts(Account.newBuilder()
                                        .setAccountId("sync-test-2")
                                        .setDeleted(false)
                                        .setDisplayName("sync-test-2")
                                        .setFolderId(folder.getId())
                                        .setKey("sync-test-2")
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
                                .build()),
                GrpcResponse.success(ListAccountsResponse.newBuilder()
                        .addAccounts(Account.newBuilder()
                                .setAccountId("sync-test-1")
                                .setDeleted(true)
                                .setDisplayName("sync-test-1")
                                .setFolderId(folder.getId())
                                .setKey("sync-test-1")
                                .setVersion(CurrentVersion.newBuilder().setVersion(1L).build())
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
                                        .setTimestamp(Timestamps.fromMillis(now.plusSeconds(5).toEpochMilli()))
                                        .build())
                                .build())
                        .addAccounts(Account.newBuilder()
                                .setAccountId("sync-test-2")
                                .setDeleted(false)
                                .setDisplayName("sync-test-2")
                                .setFolderId(folder.getId())
                                .setKey("sync-test-2")
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
                                .build()).build())));
        accountsSyncService.syncOneProvider(provider, Locales.ENGLISH, baseClock).block();
        Optional<AccountModel> syncedAccountOne = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "sync-test-1", null)))))
                .block();
        Optional<AccountModel> syncedAccountTwo = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "sync-test-2", null)))))
                .block();
        Assertions.assertNotNull(syncedAccountOne);
        Assertions.assertTrue(syncedAccountOne.isPresent());
        Assertions.assertEquals(provider.getId(), syncedAccountOne.get().getProviderId());
        Assertions.assertEquals("sync-test-1", syncedAccountOne.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("sync-test-1", syncedAccountOne.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), syncedAccountOne.get().getFolderId());
        Assertions.assertEquals("sync-test-1", syncedAccountOne.get().getDisplayName().get());
        Assertions.assertFalse(syncedAccountOne.get().isDeleted());
        Assertions.assertEquals(0L, syncedAccountOne.get().getLastReceivedVersion().get());
        Assertions.assertTrue(syncedAccountOne.get().getLatestSuccessfulAccountOperationId().isEmpty());
        Assertions.assertNotNull(syncedAccountTwo);
        Assertions.assertTrue(syncedAccountTwo.isPresent());
        Assertions.assertEquals(provider.getId(), syncedAccountTwo.get().getProviderId());
        Assertions.assertEquals("sync-test-2", syncedAccountTwo.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("sync-test-2", syncedAccountTwo.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), syncedAccountTwo.get().getFolderId());
        Assertions.assertEquals("sync-test-2", syncedAccountTwo.get().getDisplayName().get());
        Assertions.assertFalse(syncedAccountTwo.get().isDeleted());
        Assertions.assertEquals(0L, syncedAccountTwo.get().getLastReceivedVersion().get());
        Assertions.assertTrue(syncedAccountTwo.get().getLatestSuccessfulAccountOperationId().isEmpty());
        List<QuotaModel> syncedQuotas = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folder.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(syncedQuotas);
        Assertions.assertEquals(1, syncedQuotas.size());
        Assertions.assertEquals(folder.getId(), syncedQuotas.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), syncedQuotas.get(0).getProviderId());
        Assertions.assertEquals(resource.getId(), syncedQuotas.get(0).getResourceId());
        Assertions.assertEquals(0L, syncedQuotas.get(0).getQuota());
        Assertions.assertEquals(-4_000_000_000L, syncedQuotas.get(0).getBalance());
        Assertions.assertEquals(0L, syncedQuotas.get(0).getFrozenQuota());
        List<AccountsQuotasModel> syncedProvisionsOne = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                        .getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID,
                                Set.of(syncedAccountOne.get().getId()))))
                .block();
        List<AccountsQuotasModel> syncedProvisionsTwo = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                        .getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID,
                                Set.of(syncedAccountTwo.get().getId()))))
                .block();
        Assertions.assertNotNull(syncedProvisionsOne);
        Assertions.assertEquals(1, syncedProvisionsOne.size());
        Assertions.assertEquals(syncedAccountOne.get().getId(), syncedProvisionsOne.get(0).getAccountId());
        Assertions.assertEquals(resource.getId(), syncedProvisionsOne.get(0).getResourceId());
        Assertions.assertEquals(folder.getId(), syncedProvisionsOne.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), syncedProvisionsOne.get(0).getProviderId());
        Assertions.assertEquals(2_000_000_000L, syncedProvisionsOne.get(0).getProvidedQuota());
        Assertions.assertEquals(1_000_000_000L, syncedProvisionsOne.get(0).getAllocatedQuota());
        Assertions.assertEquals(0L, syncedProvisionsOne.get(0).getLastReceivedProvisionVersion().get());
        Assertions.assertTrue(syncedProvisionsOne.get(0).getLatestSuccessfulProvisionOperationId().isEmpty());
        Assertions.assertNotNull(syncedProvisionsTwo);
        Assertions.assertEquals(1, syncedProvisionsTwo.size());
        Assertions.assertEquals(syncedAccountTwo.get().getId(), syncedProvisionsTwo.get(0).getAccountId());
        Assertions.assertEquals(resource.getId(), syncedProvisionsTwo.get(0).getResourceId());
        Assertions.assertEquals(folder.getId(), syncedProvisionsTwo.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), syncedProvisionsTwo.get(0).getProviderId());
        Assertions.assertEquals(2_000_000_000L, syncedProvisionsTwo.get(0).getProvidedQuota());
        Assertions.assertEquals(1_000_000_000L, syncedProvisionsTwo.get(0).getAllocatedQuota());
        Assertions.assertEquals(0L, syncedProvisionsTwo.get(0).getLastReceivedProvisionVersion().get());
        Assertions.assertTrue(syncedProvisionsTwo.get(0).getLatestSuccessfulProvisionOperationId().isEmpty());
        List<FolderOperationLogModel> syncedOpLog = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
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
        Assertions.assertEquals(new QuotasByResource(Map.of(resource.getId(), -4_000_000_000L)),
                syncedOpLog.get(0).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(syncedAccountOne.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(0L, null))),
                syncedAccountTwo.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(0L, null))))),
                syncedOpLog.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(syncedAccountOne.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(2_000_000_000L, 0L))),
                syncedAccountTwo.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(2_000_000_000L, 0L))))),
                syncedOpLog.get(0).getNewProvisions());
        Assertions.assertTrue(syncedOpLog.get(0).getOldAccounts().isEmpty());
        Assertions.assertTrue(syncedOpLog.get(0).getNewAccounts().isPresent());
        Assertions.assertTrue(syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .containsKey(syncedAccountOne.get().getId()));
        Assertions.assertTrue(syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .containsKey(syncedAccountTwo.get().getId()));
        Assertions.assertEquals(provider.getId(), syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountOne.get().getId()).getProviderId().get());
        Assertions.assertEquals("sync-test-1", syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountOne.get().getId()).getOuterAccountIdInProvider().get());
        Assertions.assertEquals("sync-test-1", syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountOne.get().getId()).getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountOne.get().getId()).getFolderId().get());
        Assertions.assertEquals("sync-test-1", syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountOne.get().getId()).getDisplayName().get());
        Assertions.assertFalse(syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountOne.get().getId()).getDeleted().get());
        Assertions.assertEquals(0L, syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountOne.get().getId()).getLastReceivedVersion().get());
        Assertions.assertEquals(provider.getId(), syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountTwo.get().getId()).getProviderId().get());
        Assertions.assertEquals("sync-test-2", syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountTwo.get().getId()).getOuterAccountIdInProvider().get());
        Assertions.assertEquals("sync-test-2", syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountTwo.get().getId()).getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountTwo.get().getId()).getFolderId().get());
        Assertions.assertEquals("sync-test-2", syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountTwo.get().getId()).getDisplayName().get());
        Assertions.assertFalse(syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountTwo.get().getId()).getDeleted().get());
        Assertions.assertEquals(0L, syncedOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAccountTwo.get().getId()).getLastReceivedVersion().get());
        Assertions.assertEquals(1L, syncedOpLog.get(0).getOrder());
        Assertions.assertTrue(syncedOpLog.get(0).getAccountsQuotasOperationsId().isEmpty());
        accountsSyncService.syncOneProvider(provider, Locales.ENGLISH, Clock.offset(baseClock, Duration.ofMinutes(10)))
                .block();
        Optional<AccountModel> syncedAgainAccountOne = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "sync-test-1", null)))))
                .block();
        Optional<AccountModel> syncedAgainAccountTwo = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "sync-test-2", null)))))
                .block();
        Assertions.assertNotNull(syncedAgainAccountOne);
        Assertions.assertTrue(syncedAgainAccountOne.isPresent());
        Assertions.assertEquals(provider.getId(), syncedAgainAccountOne.get().getProviderId());
        Assertions.assertEquals("sync-test-1", syncedAgainAccountOne.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("sync-test-1", syncedAgainAccountOne.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), syncedAgainAccountOne.get().getFolderId());
        Assertions.assertEquals("sync-test-1", syncedAgainAccountOne.get().getDisplayName().get());
        Assertions.assertTrue(syncedAgainAccountOne.get().isDeleted());
        Assertions.assertEquals(1L, syncedAgainAccountOne.get().getLastReceivedVersion().get());
        Assertions.assertTrue(syncedAgainAccountOne.get().getLatestSuccessfulAccountOperationId().isEmpty());
        Assertions.assertNotNull(syncedAgainAccountTwo);
        Assertions.assertTrue(syncedAgainAccountTwo.isPresent());
        Assertions.assertEquals(provider.getId(), syncedAgainAccountTwo.get().getProviderId());
        Assertions.assertEquals("sync-test-2", syncedAgainAccountTwo.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("sync-test-2", syncedAgainAccountTwo.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), syncedAgainAccountTwo.get().getFolderId());
        Assertions.assertEquals("sync-test-2", syncedAgainAccountTwo.get().getDisplayName().get());
        Assertions.assertFalse(syncedAgainAccountTwo.get().isDeleted());
        Assertions.assertEquals(0L, syncedAgainAccountTwo.get().getLastReceivedVersion().get());
        Assertions.assertTrue(syncedAgainAccountTwo.get().getLatestSuccessfulAccountOperationId().isEmpty());
        List<QuotaModel> syncedAgainQuotas = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folder.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(syncedAgainQuotas);
        Assertions.assertEquals(1, syncedAgainQuotas.size());
        Assertions.assertEquals(folder.getId(), syncedAgainQuotas.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), syncedAgainQuotas.get(0).getProviderId());
        Assertions.assertEquals(resource.getId(), syncedAgainQuotas.get(0).getResourceId());
        Assertions.assertEquals(0L, syncedAgainQuotas.get(0).getQuota());
        Assertions.assertEquals(-2_000_000_000L, syncedAgainQuotas.get(0).getBalance());
        Assertions.assertEquals(0L, syncedAgainQuotas.get(0).getFrozenQuota());
        List<AccountsQuotasModel> syncedAgainProvisionsOne = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                        .getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID,
                                Set.of(syncedAccountOne.get().getId()))))
                .block();
        List<AccountsQuotasModel> syncedAgainProvisionsTwo = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                        .getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID,
                                Set.of(syncedAccountTwo.get().getId()))))
                .block();
        Assertions.assertNotNull(syncedAgainProvisionsOne);
        Assertions.assertEquals(1, syncedAgainProvisionsOne.size());
        Assertions.assertEquals(syncedAccountOne.get().getId(), syncedAgainProvisionsOne.get(0).getAccountId());
        Assertions.assertEquals(resource.getId(), syncedAgainProvisionsOne.get(0).getResourceId());
        Assertions.assertEquals(folder.getId(), syncedAgainProvisionsOne.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), syncedAgainProvisionsOne.get(0).getProviderId());
        Assertions.assertEquals(0L, syncedAgainProvisionsOne.get(0).getProvidedQuota());
        Assertions.assertEquals(0L, syncedAgainProvisionsOne.get(0).getAllocatedQuota());
        Assertions.assertTrue(syncedAgainProvisionsOne.get(0).getLastReceivedProvisionVersion().isEmpty());
        Assertions.assertTrue(syncedAgainProvisionsOne.get(0).getLatestSuccessfulProvisionOperationId().isEmpty());
        Assertions.assertNotNull(syncedAgainProvisionsTwo);
        Assertions.assertEquals(1, syncedAgainProvisionsTwo.size());
        Assertions.assertEquals(syncedAccountTwo.get().getId(), syncedAgainProvisionsTwo.get(0).getAccountId());
        Assertions.assertEquals(resource.getId(), syncedAgainProvisionsTwo.get(0).getResourceId());
        Assertions.assertEquals(folder.getId(), syncedAgainProvisionsTwo.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), syncedAgainProvisionsTwo.get(0).getProviderId());
        Assertions.assertEquals(2_000_000_000L, syncedAgainProvisionsTwo.get(0).getProvidedQuota());
        Assertions.assertEquals(1_000_000_000L, syncedAgainProvisionsTwo.get(0).getAllocatedQuota());
        Assertions.assertEquals(0L, syncedAgainProvisionsTwo.get(0).getLastReceivedProvisionVersion().get());
        Assertions.assertTrue(syncedAgainProvisionsTwo.get(0).getLatestSuccessfulProvisionOperationId().isEmpty());
        List<FolderOperationLogModel> syncedAgainOpLog = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
                        .getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folder.getId(), SortOrderDto.ASC,
                                100)))
                .block();
        Assertions.assertNotNull(syncedAgainOpLog);
        Assertions.assertEquals(2, syncedAgainOpLog.size());
        syncedAgainOpLog.sort(Comparator.comparing(FolderOperationLogModel::getOrder));
        Assertions.assertEquals(folder.getId(), syncedAgainOpLog.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.SYNC_WITH_PROVIDER, syncedAgainOpLog.get(0).getOperationType());
        Assertions.assertEquals("0b204534-d0ec-452d-99fe-a3d1da5a49a9",
                syncedAgainOpLog.get(0).getAuthorUserId().get());
        Assertions.assertEquals("1120000000000001", syncedAgainOpLog.get(0).getAuthorUserUid().get());
        Assertions.assertTrue(syncedAgainOpLog.get(0).getAuthorProviderId().isEmpty());
        Assertions.assertTrue(syncedAgainOpLog.get(0).getSourceFolderOperationsLogId().isEmpty());
        Assertions.assertTrue(syncedAgainOpLog.get(0).getOldFolderFields().isEmpty());
        Assertions.assertTrue(syncedAgainOpLog.get(0).getNewFolderFields().isEmpty());
        Assertions.assertEquals(new QuotasByResource(Map.of()), syncedAgainOpLog.get(0).getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of()), syncedAgainOpLog.get(0).getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of(resource.getId(), 0L)),
                syncedAgainOpLog.get(0).getOldBalance());
        Assertions.assertEquals(new QuotasByResource(Map.of(resource.getId(), -4_000_000_000L)),
                syncedAgainOpLog.get(0).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(syncedAgainAccountOne.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(0L, null))),
                syncedAgainAccountTwo.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(0L, null))))),
                syncedAgainOpLog.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(syncedAgainAccountOne.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(2_000_000_000L, 0L))),
                syncedAgainAccountTwo.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(2_000_000_000L, 0L))))),
                syncedAgainOpLog.get(0).getNewProvisions());
        Assertions.assertTrue(syncedAgainOpLog.get(0).getOldAccounts().isEmpty());
        Assertions.assertTrue(syncedAgainOpLog.get(0).getNewAccounts().isPresent());
        Assertions.assertTrue(syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .containsKey(syncedAgainAccountOne.get().getId()));
        Assertions.assertTrue(syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .containsKey(syncedAgainAccountTwo.get().getId()));
        Assertions.assertEquals(provider.getId(), syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getProviderId().get());
        Assertions.assertEquals("sync-test-1", syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getOuterAccountIdInProvider().get());
        Assertions.assertEquals("sync-test-1", syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getFolderId().get());
        Assertions.assertEquals("sync-test-1", syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getDisplayName().get());
        Assertions.assertFalse(syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getDeleted().get());
        Assertions.assertEquals(0L, syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getLastReceivedVersion().get());
        Assertions.assertEquals(provider.getId(), syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountTwo.get().getId()).getProviderId().get());
        Assertions.assertEquals("sync-test-2", syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountTwo.get().getId()).getOuterAccountIdInProvider().get());
        Assertions.assertEquals("sync-test-2", syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountTwo.get().getId()).getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountTwo.get().getId()).getFolderId().get());
        Assertions.assertEquals("sync-test-2", syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountTwo.get().getId()).getDisplayName().get());
        Assertions.assertFalse(syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountTwo.get().getId()).getDeleted().get());
        Assertions.assertEquals(0L, syncedAgainOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountTwo.get().getId()).getLastReceivedVersion().get());
        Assertions.assertEquals(1L, syncedAgainOpLog.get(0).getOrder());
        Assertions.assertTrue(syncedAgainOpLog.get(0).getAccountsQuotasOperationsId().isEmpty());
        Assertions.assertEquals(folder.getId(), syncedAgainOpLog.get(1).getFolderId());
        Assertions.assertEquals(FolderOperationType.SYNC_WITH_PROVIDER, syncedAgainOpLog.get(1).getOperationType());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getAuthorUserId().isEmpty());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getAuthorUserUid().isEmpty());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getAuthorProviderId().isPresent());
        Assertions.assertEquals(provider.getId(), syncedAgainOpLog.get(1).getAuthorProviderId().get());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getSourceFolderOperationsLogId().isEmpty());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getOldFolderFields().isEmpty());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getNewFolderFields().isEmpty());
        Assertions.assertEquals(new QuotasByResource(Map.of()), syncedAgainOpLog.get(1).getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of()), syncedAgainOpLog.get(1).getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of(resource.getId(), -4_000_000_000L)),
                syncedAgainOpLog.get(1).getOldBalance());
        Assertions.assertEquals(new QuotasByResource(Map.of(resource.getId(), -2_000_000_000L)),
                syncedAgainOpLog.get(1).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(syncedAgainAccountOne.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(2_000_000_000L, 0L))))),
                syncedAgainOpLog.get(1).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(syncedAgainAccountOne.get().getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(0L, null))))),
                syncedAgainOpLog.get(1).getNewProvisions());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getOldAccounts().isPresent());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getOldAccounts().get().getAccounts()
                .containsKey(syncedAgainAccountOne.get().getId()));
        Assertions.assertFalse(syncedAgainOpLog.get(1).getOldAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getDeleted().get());
        Assertions.assertEquals(0L, syncedAgainOpLog.get(1).getOldAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getLastReceivedVersion().get());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getNewAccounts().isPresent());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getNewAccounts().get().getAccounts()
                .containsKey(syncedAgainAccountOne.get().getId()));
        Assertions.assertTrue(syncedAgainOpLog.get(1).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getDeleted().get());
        Assertions.assertEquals(1L, syncedAgainOpLog.get(1).getNewAccounts().get().getAccounts()
                .get(syncedAgainAccountOne.get().getId()).getLastReceivedVersion().get());
        Assertions.assertEquals(2L, syncedAgainOpLog.get(1).getOrder());
        Assertions.assertTrue(syncedAgainOpLog.get(1).getAccountsQuotasOperationsId().isEmpty());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testBasicSyncAccountsSpaces() {
        ProviderModel provider = providerModel(GRPC_URI, null, true, true);
        ResourceTypeModel resourceType = resourceTypeModel(provider.getId(), "test",
                "b02344bf-96af-4cc5-937c-66a479989ce8");
        ResourceSegmentationModel locationSegmentation = resourceSegmentationModel(provider.getId(), "location");
        ResourceSegmentModel vlaSegment = resourceSegmentModel(locationSegmentation.getId(), "VLA");
        AccountSpaceModel accountsSpace = accountSpaceModel(provider.getId(), "test",
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())));
        ResourceModel resource = resourceModel(provider.getId(), "test", resourceType.getId(),
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())),
                "b02344bf-96af-4cc5-937c-66a479989ce8",
                Set.of("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                        "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                accountsSpace.getId());
        FolderModel folder = folderModel(1L);
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
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsSpacesDao
                        .upsertOneRetryable(txSession, accountsSpace)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourcesDao
                        .upsertResourceRetryable(txSession, resource)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
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
                                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                        .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                                .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("location")
                                                        .setResourceSegmentKey("VLA")
                                                        .build())
                                                .build())
                                        .build())
                                .addProvisions(Provision.newBuilder()
                                        .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("test")
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
        accountsSyncService.syncOneProvider(provider, Locales.ENGLISH, Clock.systemUTC()).block();
        Optional<AccountModel> syncedAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "sync-test-1", accountsSpace.getId())))))
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
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
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
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
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
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
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
    @SuppressWarnings("MethodLength")
    public void testBasicSyncSkipCreationInProgress() {
        ProviderModel provider = providerModel(GRPC_URI, null, false, true);
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
        AccountsQuotasOperationsModel operation = createAccountOperationModel(null, provider.getId(),
                AccountsQuotasOperationsModel.RequestStatus.WAITING,
                "sync-test-1", folder.getId(), 1L, null, Clock.systemUTC());
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
                        .upsertResourceRetryable(txSession, resource)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                        .upsertOneRetryable(txSession, operation)))
                .block();
        String accountOperationId = operation.getOperationId();
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
        accountsSyncService.syncOneProvider(provider, Locales.ENGLISH, Clock.systemUTC()).block();
        Optional<AccountModel> syncedAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "sync-test-1", null)))))
                .block();
        Assertions.assertNotNull(syncedAccount);
        Assertions.assertFalse(syncedAccount.isPresent());
        List<QuotaModel> syncedQuotas = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folder.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(syncedQuotas);
        Assertions.assertTrue(syncedQuotas.isEmpty());
        List<FolderOperationLogModel> syncedOpLog = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
                        .getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folder.getId(), SortOrderDto.ASC,
                                100)))
                .block();
        Assertions.assertNotNull(syncedOpLog);
        Assertions.assertTrue(syncedOpLog.isEmpty());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testBasicSyncAccountsSpacesTimedOutCreation() {
        ProviderModel provider = providerModel(GRPC_URI, null, true, true);
        ResourceTypeModel resourceType = resourceTypeModel(provider.getId(), "test",
                "b02344bf-96af-4cc5-937c-66a479989ce8");
        ResourceSegmentationModel locationSegmentation = resourceSegmentationModel(provider.getId(), "location");
        ResourceSegmentModel vlaSegment = resourceSegmentModel(locationSegmentation.getId(), "VLA");
        AccountSpaceModel accountsSpace = accountSpaceModel(provider.getId(), "test",
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())));
        ResourceModel resource = resourceModel(provider.getId(), "test", resourceType.getId(),
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())),
                "b02344bf-96af-4cc5-937c-66a479989ce8",
                Set.of("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                        "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                accountsSpace.getId());
        FolderModel folder = folderModel(1L);
        AccountsQuotasOperationsModel operation = createAccountOperationModel(null, provider.getId(),
                AccountsQuotasOperationsModel.RequestStatus.ERROR,
                "sync-test-1", folder.getId(), 1L, null, Clock.systemUTC());
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
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsSpacesDao
                        .upsertOneRetryable(txSession, accountsSpace)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourcesDao
                        .upsertResourceRetryable(txSession, resource)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                        .upsertOneRetryable(txSession, operation)))
                .block();
        String accountOperationId = operation.getOperationId();
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
                                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                        .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                                .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("location")
                                                        .setResourceSegmentKey("VLA")
                                                        .build())
                                                .build())
                                        .build())
                                .addProvisions(Provision.newBuilder()
                                        .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("test")
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
        accountsSyncService.syncOneProvider(provider, Locales.ENGLISH, Clock.systemUTC()).block();
        Optional<AccountModel> syncedAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "sync-test-1", accountsSpace.getId())))))
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
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
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
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
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
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
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

    private ProviderModel providerModel(String grpcUri, String restUri, boolean accountsSpacesSupported,
                                        boolean softDeleteSupported) {
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
                        .softDeleteSupported(softDeleteSupported)
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

    private AccountSpaceModel accountSpaceModel(String providerId, String key, Set<Tuple2<String, String>> segments) {
        return AccountSpaceModel.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setVersion(0L)
                .setOuterKeyInProvider(key)
                .setNameEn("Test")
                .setNameRu("Test")
                .setDescriptionEn("Test")
                .setDescriptionRu("Test")
                .setDeleted(false)
                .setProviderId(providerId)
                .setSegments(segments.stream().map(t -> new ResourceSegmentSettingsModel(t.getT1(), t.getT2()))
                        .collect(Collectors.toSet()))
                .build();
    }

    @SuppressWarnings("ParameterNumber")
    private ResourceModel resourceModel(String providerId, String key, String resourceTypeId,
                                        Set<Tuple2<String, String>> segments, String unitsEnsembleId,
                                        Set<String> allowedUnitIds, String defaultUnitId,
                                        String baseUnitId, String accountsSpaceId) {
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
                .resourceUnits(new ResourceUnitsModel(allowedUnitIds, defaultUnitId, null))
                .managed(true)
                .orderable(true)
                .readOnly(false)
                .baseUnitId(baseUnitId)
                .accountsSpacesId(accountsSpaceId)
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
                .setNextOpLogOrder(1L)
                .build();
    }

    @SuppressWarnings("ParameterNumber")
    private AccountsQuotasOperationsModel createAccountOperationModel(
            String accountsSpaceId,
            String providerId,
            AccountsQuotasOperationsModel.RequestStatus requestStatus,
            String accountKey,
            String folderId,
            long submitOrder,
            Long closeOrder,
            Clock clock) {
        Instant now = Instant.now(clock);
        return new AccountsQuotasOperationsModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setOperationId(UUID.randomUUID().toString())
                .setLastRequestId(UUID.randomUUID().toString())
                .setCreateDateTime(now)
                .setOperationSource(OperationSource.USER)
                .setOperationType(AccountsQuotasOperationsModel.OperationType.CREATE_ACCOUNT)
                .setAuthorUserId("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                .setAuthorUserUid("1120000000000001")
                .setProviderId(providerId)
                .setAccountsSpaceId(accountsSpaceId)
                .setUpdateDateTime(now)
                .setRequestStatus(requestStatus)
                .setErrorMessage(null)
                .setRequestedChanges(OperationChangesModel.builder()
                        .accountCreateParams(new OperationChangesModel
                                .AccountCreateParams(accountKey, "test", folderId,
                                    UUID.randomUUID().toString(), false, null))
                        .build())
                .setOrders(OperationOrdersModel.builder()
                        .submitOrder(submitOrder)
                        .closeOrder(closeOrder)
                        .build())
                .build();
    }

}
