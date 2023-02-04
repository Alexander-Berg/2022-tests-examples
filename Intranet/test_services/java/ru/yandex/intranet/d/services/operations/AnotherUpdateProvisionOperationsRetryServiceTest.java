package ru.yandex.intranet.d.services.operations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.yandex.ydb.table.transaction.TransactionMode;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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
import ru.yandex.intranet.d.backend.service.provider_proto.Provision;
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceSegmentKey;
import ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionResponse;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsDao;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasOperationsDao;
import ru.yandex.intranet.d.dao.accounts.AccountsSpacesDao;
import ru.yandex.intranet.d.dao.accounts.OperationsInProgressDao;
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
import ru.yandex.intranet.d.model.accounts.OperationInProgressModel;
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
import ru.yandex.intranet.d.utils.DbHelper;
import ru.yandex.intranet.d.utils.DummyModels;
import ru.yandex.intranet.d.web.model.SortOrderDto;

import static ru.yandex.intranet.d.UnitIds.BYTES;
import static ru.yandex.intranet.d.UnitIds.GIGABYTES;
import static ru.yandex.intranet.d.UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID;

/**
 * Update provision operations retry service test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@SuppressWarnings({"SameParameterValue", "OptionalGetWithoutIsPresent"})
@IntegrationTest
public class AnotherUpdateProvisionOperationsRetryServiceTest {

    private static final String GRPC_URI = "in-process:test";

    @Autowired
    private ProvidersDao providersDao;
    @Autowired
    private AccountsDao accountsDao;
    @Autowired
    private FolderOperationLogDao folderOperationLogDao;
    @Autowired
    private FolderDao folderDao;
    @Autowired
    private AccountsQuotasOperationsDao accountsQuotasOperationsDao;
    @Autowired
    private OperationsInProgressDao operationsInProgressDao;
    @Autowired
    private AccountsSpacesDao accountsSpacesDao;
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
    @Autowired
    private DbHelper dbHelper;

    @Test
    @SuppressWarnings("MethodLength")
    public void testSuccessAfterRefreshPush() {
        ProviderModel provider = providerModel(GRPC_URI, null, true, false, true, true, false, false, false, false);
        FolderModel folder = folderModel(1L);
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
        QuotaModel quota = quotaModel(provider.getId(), resource.getId(), folder.getId(), 300L, 200L, 40L);
        AccountModel account = accountModel(provider.getId(), accountsSpace.getId(), "test-id", "test",
                folder.getId(), "Test", null, null, false);
        AccountsQuotasModel provision = accountQuotaModel(provider.getId(), resource.getId(), folder.getId(),
                account.getId(), 60L, 30L, null, null);
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), accountsSpace.getId(),
                account.getId(), Map.of(resource.getId(), 100L), Map.of(resource.getId(), 40L));
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
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
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsSpacesDao
                        .upsertOneRetryable(txSession, accountsSpace)))
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
        stubProviderService.setGetAccountResponses(
                List.of(
                        GrpcResponse.success(
                                Account.newBuilder()
                                        .setAccountId("test-id")
                                        .setKey("test")
                                        .setDisplayName("Test")
                                        .setFolderId(folder.getId())
                                        .setDeleted(false)
                                        .addProvisions(Provision.newBuilder()
                                                .setResourceKey(ResourceKey.newBuilder()
                                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                                .setResourceTypeKey("test")
                                                                .build())
                                                        .build())
                                                .setProvided(Amount.newBuilder()
                                                        .setValue(100L)
                                                        .setUnitKey("bytes")
                                                        .build())
                                                .setAllocated(Amount.newBuilder()
                                                        .setValue(70L)
                                                        .setUnitKey("bytes")
                                                        .build())
                                                .build())
                                        .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                                                .setResourceSegmentationKey("location")
                                                                .setResourceSegmentKey("VLA")
                                                                .build())
                                                        .build())
                                                .build())
                                        .build()
                        )
                )
        );
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Optional<AccountModel> updatedAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "test-id", accountsSpace.getId())))))
                .block();
        Assertions.assertNotNull(updatedAccount);
        Assertions.assertTrue(updatedAccount.isPresent());
        Assertions.assertEquals(provider.getId(), updatedAccount.get().getProviderId());
        Assertions.assertEquals("test-id", updatedAccount.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("test", updatedAccount.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), updatedAccount.get().getFolderId());
        Assertions.assertEquals("Test", updatedAccount.get().getDisplayName().get());
        Assertions.assertEquals(0L, updatedAccount.get().getVersion());
        Assertions.assertFalse(updatedAccount.get().isDeleted());
        Assertions.assertFalse(updatedAccount.get().getLastReceivedVersion().isPresent());
        Assertions.assertTrue(updatedAccount.get().getLatestSuccessfulAccountOperationId().isEmpty());
        Assertions.assertEquals(accountsSpace.getId(), updatedAccount.get().getAccountsSpacesId().get());
        Optional<AccountsQuotasOperationsModel> updatedOperation = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        accountsQuotasOperationsDao.getById(txSession, operation.getOperationId(),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(updatedOperation);
        Assertions.assertTrue(updatedOperation.isPresent());
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.OK,
                updatedOperation.get().getRequestStatus().get());
        Assertions.assertEquals(2L, updatedOperation.get().getOrders().getCloseOrder().get());
        List<OperationInProgressModel> inProgressAfter = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        operationsInProgressDao.getAllByTenant(txSession, Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(inProgressAfter);
        Assertions.assertTrue(inProgressAfter.isEmpty());
        List<QuotaModel> updatedQuotas = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folder.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(updatedQuotas);
        Assertions.assertEquals(1, updatedQuotas.size());
        Assertions.assertEquals(folder.getId(), updatedQuotas.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), updatedQuotas.get(0).getProviderId());
        Assertions.assertEquals(resource.getId(), updatedQuotas.get(0).getResourceId());
        Assertions.assertEquals(300L, updatedQuotas.get(0).getQuota());
        Assertions.assertEquals(200L, updatedQuotas.get(0).getBalance());
        Assertions.assertEquals(0L, updatedQuotas.get(0).getFrozenQuota());
        List<AccountsQuotasModel> updatedProvisions = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                        .getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID, Set.of(account.getId()))))
                .block();
        Assertions.assertNotNull(updatedProvisions);
        Assertions.assertEquals(1, updatedProvisions.size());
        Assertions.assertEquals(account.getId(), updatedProvisions.get(0).getAccountId());
        Assertions.assertEquals(resource.getId(), updatedProvisions.get(0).getResourceId());
        Assertions.assertEquals(folder.getId(), updatedProvisions.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), updatedProvisions.get(0).getProviderId());
        Assertions.assertEquals(100L, updatedProvisions.get(0).getProvidedQuota());
        Assertions.assertEquals(70L, updatedProvisions.get(0).getAllocatedQuota());
        Assertions.assertTrue(updatedProvisions.get(0).getLastReceivedProvisionVersion().isEmpty());
        Assertions.assertTrue(updatedProvisions.get(0).getLatestSuccessfulProvisionOperationId().isPresent());
        Assertions.assertEquals(operation.getOperationId(), updatedProvisions.get(0)
                .getLatestSuccessfulProvisionOperationId().get());
        List<FolderOperationLogModel> newOpLog = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
                        .getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folder.getId(), SortOrderDto.ASC,
                                100)))
                .block();
        Assertions.assertNotNull(newOpLog);
        Assertions.assertEquals(1, newOpLog.size());
        Assertions.assertEquals(folder.getId(), newOpLog.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.PROVIDE_REVOKE_QUOTAS_TO_ACCOUNT,
                newOpLog.get(0).getOperationType());
        Assertions.assertEquals("0b204534-d0ec-452d-99fe-a3d1da5a49a9", newOpLog.get(0).getAuthorUserId().get());
        Assertions.assertEquals("1120000000000001", newOpLog.get(0).getAuthorUserUid().get());
        Assertions.assertTrue(newOpLog.get(0).getAuthorProviderId().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getSourceFolderOperationsLogId().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getOldFolderFields().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getNewFolderFields().isEmpty());
        Assertions.assertEquals(new QuotasByResource(Map.of()), newOpLog.get(0).getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of()), newOpLog.get(0).getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of()), newOpLog.get(0).getOldBalance());
        Assertions.assertEquals(new QuotasByResource(Map.of()), newOpLog.get(0).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(account.getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(60L, null))))),
                newOpLog.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(account.getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(100L, null))))),
                newOpLog.get(0).getNewProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(account.getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(100L, null))))),
                newOpLog.get(0).getActuallyAppliedProvisions().get());
        Assertions.assertTrue(newOpLog.get(0).getOldAccounts().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().isEmpty());
        Assertions.assertEquals(2L, newOpLog.get(0).getOrder());
        Assertions.assertEquals(operation.getOperationId(), newOpLog.get(0).getAccountsQuotasOperationsId().get());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testSuccessAfterRetryPush() {
        ProviderModel provider = providerModel(GRPC_URI, null, true, false, true, true, false, false, false, false);
        FolderModel folder = folderModel(1L);
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
        QuotaModel quota = quotaModel(provider.getId(), resource.getId(), folder.getId(), 300L, 200L, 40L);
        AccountModel account = accountModel(provider.getId(), accountsSpace.getId(), "test-id", "test",
                folder.getId(), "Test", null, null, false);
        AccountsQuotasModel provision = accountQuotaModel(provider.getId(), resource.getId(), folder.getId(),
                account.getId(), 60L, 30L, null, null);
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), accountsSpace.getId(),
                account.getId(), Map.of(resource.getId(), 100L), Map.of(resource.getId(), 40L));
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
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
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsSpacesDao
                        .upsertOneRetryable(txSession, accountsSpace)))
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
        stubProviderService.setGetAccountResponses(
                List.of(
                        GrpcResponse.success(
                                Account.newBuilder()
                                        .setAccountId("test-id")
                                        .setKey("test")
                                        .setDisplayName("Test")
                                        .setFolderId(folder.getId())
                                        .setDeleted(false)
                                        .addProvisions(Provision.newBuilder()
                                                .setResourceKey(ResourceKey.newBuilder()
                                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                                .setResourceTypeKey("test")
                                                                .build())
                                                        .build())
                                                .setProvided(Amount.newBuilder()
                                                        .setValue(60L)
                                                        .setUnitKey("bytes")
                                                        .build())
                                                .setAllocated(Amount.newBuilder()
                                                        .setValue(30L)
                                                        .setUnitKey("bytes")
                                                        .build())
                                                .build())
                                        .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                                                .setResourceSegmentationKey("location")
                                                                .setResourceSegmentKey("VLA")
                                                                .build())
                                                        .build())
                                                .build())
                                        .build()
                        )
                )
        );
        stubProviderService.setUpdateProvisionResponses(
                List.of(
                        GrpcResponse.success(
                                UpdateProvisionResponse.newBuilder()
                                        .addProvisions(Provision.newBuilder()
                                                .setResourceKey(ResourceKey.newBuilder()
                                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                                .setResourceTypeKey("test")
                                                                .build())
                                                        .build())
                                                .setProvided(Amount.newBuilder()
                                                        .setValue(100L)
                                                        .setUnitKey("bytes")
                                                        .build())
                                                .setAllocated(Amount.newBuilder()
                                                        .setValue(70L)
                                                        .setUnitKey("bytes")
                                                        .build())
                                                .build())
                                        .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                                                .setResourceSegmentationKey("location")
                                                                .setResourceSegmentKey("VLA")
                                                                .build())
                                                        .build())
                                                .build())
                                        .build()
                        )
                ));
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Optional<AccountModel> updatedAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "test-id", accountsSpace.getId())))))
                .block();
        Assertions.assertNotNull(updatedAccount);
        Assertions.assertTrue(updatedAccount.isPresent());
        Assertions.assertEquals(provider.getId(), updatedAccount.get().getProviderId());
        Assertions.assertEquals("test-id", updatedAccount.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("test", updatedAccount.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), updatedAccount.get().getFolderId());
        Assertions.assertEquals("Test", updatedAccount.get().getDisplayName().get());
        Assertions.assertEquals(0L, updatedAccount.get().getVersion());
        Assertions.assertFalse(updatedAccount.get().isDeleted());
        Assertions.assertFalse(updatedAccount.get().getLastReceivedVersion().isPresent());
        Assertions.assertTrue(updatedAccount.get().getLatestSuccessfulAccountOperationId().isEmpty());
        Assertions.assertEquals(accountsSpace.getId(), updatedAccount.get().getAccountsSpacesId().get());
        Optional<AccountsQuotasOperationsModel> updatedOperation = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        accountsQuotasOperationsDao.getById(txSession, operation.getOperationId(),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(updatedOperation);
        Assertions.assertTrue(updatedOperation.isPresent());
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.OK,
                updatedOperation.get().getRequestStatus().get());
        Assertions.assertEquals(2L, updatedOperation.get().getOrders().getCloseOrder().get());
        List<OperationInProgressModel> inProgressAfter = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        operationsInProgressDao.getAllByTenant(txSession, Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(inProgressAfter);
        Assertions.assertTrue(inProgressAfter.isEmpty());
        List<QuotaModel> updatedQuotas = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folder.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(updatedQuotas);
        Assertions.assertEquals(1, updatedQuotas.size());
        Assertions.assertEquals(folder.getId(), updatedQuotas.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), updatedQuotas.get(0).getProviderId());
        Assertions.assertEquals(resource.getId(), updatedQuotas.get(0).getResourceId());
        Assertions.assertEquals(300L, updatedQuotas.get(0).getQuota());
        Assertions.assertEquals(200L, updatedQuotas.get(0).getBalance());
        Assertions.assertEquals(0L, updatedQuotas.get(0).getFrozenQuota());
        List<AccountsQuotasModel> updatedProvisions = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                        .getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID, Set.of(account.getId()))))
                .block();
        Assertions.assertNotNull(updatedProvisions);
        Assertions.assertEquals(1, updatedProvisions.size());
        Assertions.assertEquals(account.getId(), updatedProvisions.get(0).getAccountId());
        Assertions.assertEquals(resource.getId(), updatedProvisions.get(0).getResourceId());
        Assertions.assertEquals(folder.getId(), updatedProvisions.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), updatedProvisions.get(0).getProviderId());
        Assertions.assertEquals(100L, updatedProvisions.get(0).getProvidedQuota());
        Assertions.assertEquals(70L, updatedProvisions.get(0).getAllocatedQuota());
        Assertions.assertTrue(updatedProvisions.get(0).getLastReceivedProvisionVersion().isEmpty());
        Assertions.assertTrue(updatedProvisions.get(0).getLatestSuccessfulProvisionOperationId().isPresent());
        Assertions.assertEquals(operation.getOperationId(), updatedProvisions.get(0)
                .getLatestSuccessfulProvisionOperationId().get());
        List<FolderOperationLogModel> newOpLog = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
                        .getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folder.getId(), SortOrderDto.ASC,
                                100)))
                .block();
        Assertions.assertNotNull(newOpLog);
        Assertions.assertEquals(1, newOpLog.size());
        Assertions.assertEquals(folder.getId(), newOpLog.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.PROVIDE_REVOKE_QUOTAS_TO_ACCOUNT,
                newOpLog.get(0).getOperationType());
        Assertions.assertEquals("0b204534-d0ec-452d-99fe-a3d1da5a49a9", newOpLog.get(0).getAuthorUserId().get());
        Assertions.assertEquals("1120000000000001", newOpLog.get(0).getAuthorUserUid().get());
        Assertions.assertTrue(newOpLog.get(0).getAuthorProviderId().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getSourceFolderOperationsLogId().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getOldFolderFields().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getNewFolderFields().isEmpty());
        Assertions.assertEquals(new QuotasByResource(Map.of()), newOpLog.get(0).getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of()), newOpLog.get(0).getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of()), newOpLog.get(0).getOldBalance());
        Assertions.assertEquals(new QuotasByResource(Map.of()), newOpLog.get(0).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(account.getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(60L, null))))),
                newOpLog.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(account.getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(100L, null))))),
                newOpLog.get(0).getNewProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(account.getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(100L, null))))),
                newOpLog.get(0).getActuallyAppliedProvisions().get());
        Assertions.assertTrue(newOpLog.get(0).getOldAccounts().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().isEmpty());
        Assertions.assertEquals(2L, newOpLog.get(0).getOrder());
        Assertions.assertEquals(operation.getOperationId(), newOpLog.get(0).getAccountsQuotasOperationsId().get());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testSuccessAfterConflictPush() {
        ProviderModel provider = providerModel(GRPC_URI, null, true, false, true, true, false, false, false, false);
        FolderModel folder = folderModel(1L);
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
        QuotaModel quota = quotaModel(provider.getId(), resource.getId(), folder.getId(), 300L, 200L, 40L);
        AccountModel account = accountModel(provider.getId(), accountsSpace.getId(), "test-id", "test",
                folder.getId(), "Test", null, null, false);
        AccountsQuotasModel provision = accountQuotaModel(provider.getId(), resource.getId(), folder.getId(),
                account.getId(), 60L, 30L, null, null);
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), accountsSpace.getId(),
                account.getId(), Map.of(resource.getId(), 100L), Map.of(resource.getId(), 40L));
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
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
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsSpacesDao
                        .upsertOneRetryable(txSession, accountsSpace)))
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
        stubProviderService.setGetAccountResponses(
                List.of(
                        GrpcResponse.success(
                                Account.newBuilder()
                                        .setAccountId("test-id")
                                        .setKey("test")
                                        .setDisplayName("Test")
                                        .setFolderId(folder.getId())
                                        .setDeleted(false)
                                        .addProvisions(Provision.newBuilder()
                                                .setResourceKey(ResourceKey.newBuilder()
                                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                                .setResourceTypeKey("test")
                                                                .build())
                                                        .build())
                                                .setProvided(Amount.newBuilder()
                                                        .setValue(60L)
                                                        .setUnitKey("bytes")
                                                        .build())
                                                .setAllocated(Amount.newBuilder()
                                                        .setValue(30L)
                                                        .setUnitKey("bytes")
                                                        .build())
                                                .build())
                                        .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                                                .setResourceSegmentationKey("location")
                                                                .setResourceSegmentKey("VLA")
                                                                .build())
                                                        .build())
                                                .build())
                                        .build()
                        ),
                        GrpcResponse.success(
                                Account.newBuilder()
                                        .setAccountId("test-id")
                                        .setKey("test")
                                        .setDisplayName("Test")
                                        .setFolderId(folder.getId())
                                        .setDeleted(false)
                                        .addProvisions(Provision.newBuilder()
                                                .setResourceKey(ResourceKey.newBuilder()
                                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                                .setResourceTypeKey("test")
                                                                .build())
                                                        .build())
                                                .setProvided(Amount.newBuilder()
                                                        .setValue(100L)
                                                        .setUnitKey("bytes")
                                                        .build())
                                                .setAllocated(Amount.newBuilder()
                                                        .setValue(70L)
                                                        .setUnitKey("bytes")
                                                        .build())
                                                .build())
                                        .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                                                .setResourceSegmentationKey("location")
                                                                .setResourceSegmentKey("VLA")
                                                                .build())
                                                        .build())
                                                .build())
                                        .build()
                        )
                )
        );
        stubProviderService.setUpdateProvisionResponses(
                List.of(
                        GrpcResponse.failure(new StatusRuntimeException(Status.FAILED_PRECONDITION, new Metadata())),
                        GrpcResponse.failure(new StatusRuntimeException(Status.FAILED_PRECONDITION, new Metadata())),
                        GrpcResponse.failure(new StatusRuntimeException(Status.FAILED_PRECONDITION, new Metadata()))
                ));
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Optional<AccountModel> updatedAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "test-id", accountsSpace.getId())))))
                .block();
        Assertions.assertNotNull(updatedAccount);
        Assertions.assertTrue(updatedAccount.isPresent());
        Assertions.assertEquals(provider.getId(), updatedAccount.get().getProviderId());
        Assertions.assertEquals("test-id", updatedAccount.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("test", updatedAccount.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), updatedAccount.get().getFolderId());
        Assertions.assertEquals("Test", updatedAccount.get().getDisplayName().get());
        Assertions.assertEquals(0L, updatedAccount.get().getVersion());
        Assertions.assertFalse(updatedAccount.get().isDeleted());
        Assertions.assertFalse(updatedAccount.get().getLastReceivedVersion().isPresent());
        Assertions.assertTrue(updatedAccount.get().getLatestSuccessfulAccountOperationId().isEmpty());
        Assertions.assertEquals(accountsSpace.getId(), updatedAccount.get().getAccountsSpacesId().get());
        Optional<AccountsQuotasOperationsModel> updatedOperation = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        accountsQuotasOperationsDao.getById(txSession, operation.getOperationId(),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(updatedOperation);
        Assertions.assertTrue(updatedOperation.isPresent());
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.OK,
                updatedOperation.get().getRequestStatus().get());
        Assertions.assertEquals(2L, updatedOperation.get().getOrders().getCloseOrder().get());
        List<OperationInProgressModel> inProgressAfter = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        operationsInProgressDao.getAllByTenant(txSession, Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(inProgressAfter);
        Assertions.assertTrue(inProgressAfter.isEmpty());
        List<QuotaModel> updatedQuotas = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folder.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(updatedQuotas);
        Assertions.assertEquals(1, updatedQuotas.size());
        Assertions.assertEquals(folder.getId(), updatedQuotas.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), updatedQuotas.get(0).getProviderId());
        Assertions.assertEquals(resource.getId(), updatedQuotas.get(0).getResourceId());
        Assertions.assertEquals(300L, updatedQuotas.get(0).getQuota());
        Assertions.assertEquals(200L, updatedQuotas.get(0).getBalance());
        Assertions.assertEquals(0L, updatedQuotas.get(0).getFrozenQuota());
        List<AccountsQuotasModel> updatedProvisions = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                        .getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID, Set.of(account.getId()))))
                .block();
        Assertions.assertNotNull(updatedProvisions);
        Assertions.assertEquals(1, updatedProvisions.size());
        Assertions.assertEquals(account.getId(), updatedProvisions.get(0).getAccountId());
        Assertions.assertEquals(resource.getId(), updatedProvisions.get(0).getResourceId());
        Assertions.assertEquals(folder.getId(), updatedProvisions.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), updatedProvisions.get(0).getProviderId());
        Assertions.assertEquals(100L, updatedProvisions.get(0).getProvidedQuota());
        Assertions.assertEquals(70L, updatedProvisions.get(0).getAllocatedQuota());
        Assertions.assertTrue(updatedProvisions.get(0).getLastReceivedProvisionVersion().isEmpty());
        Assertions.assertTrue(updatedProvisions.get(0).getLatestSuccessfulProvisionOperationId().isPresent());
        Assertions.assertEquals(operation.getOperationId(), updatedProvisions.get(0)
                .getLatestSuccessfulProvisionOperationId().get());
        List<FolderOperationLogModel> newOpLog = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
                        .getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folder.getId(), SortOrderDto.ASC,
                                100)))
                .block();
        Assertions.assertNotNull(newOpLog);
        Assertions.assertEquals(1, newOpLog.size());
        Assertions.assertEquals(folder.getId(), newOpLog.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.PROVIDE_REVOKE_QUOTAS_TO_ACCOUNT,
                newOpLog.get(0).getOperationType());
        Assertions.assertEquals("0b204534-d0ec-452d-99fe-a3d1da5a49a9", newOpLog.get(0).getAuthorUserId().get());
        Assertions.assertEquals("1120000000000001", newOpLog.get(0).getAuthorUserUid().get());
        Assertions.assertTrue(newOpLog.get(0).getAuthorProviderId().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getSourceFolderOperationsLogId().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getOldFolderFields().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getNewFolderFields().isEmpty());
        Assertions.assertEquals(new QuotasByResource(Map.of()), newOpLog.get(0).getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of()), newOpLog.get(0).getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(Map.of()), newOpLog.get(0).getOldBalance());
        Assertions.assertEquals(new QuotasByResource(Map.of()), newOpLog.get(0).getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(account.getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(60L, null))))),
                newOpLog.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(account.getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(100L, null))))),
                newOpLog.get(0).getNewProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(account.getId(),
                new ProvisionsByResource(Map.of(resource.getId(), new ProvisionHistoryModel(100L, null))))),
                newOpLog.get(0).getActuallyAppliedProvisions().get());
        Assertions.assertTrue(newOpLog.get(0).getOldAccounts().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().isEmpty());
        Assertions.assertEquals(2L, newOpLog.get(0).getOrder());
        Assertions.assertEquals(operation.getOperationId(), newOpLog.get(0).getAccountsQuotasOperationsId().get());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testFailuresNotFoundDeletedAccountPush() {
        ProviderModel provider = providerModel(GRPC_URI, null, true, false, true, true, false, false, false, false);
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
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", null);
        QuotaModel quota = quotaModel(provider.getId(), resource.getId(), folder.getId(), 300L, 200L, 40L);
        AccountModel account = accountModel(provider.getId(), null, "test-id", "test",
                folder.getId(), "Test", null, null, true);
        AccountsQuotasModel provision = accountQuotaModel(provider.getId(), resource.getId(), folder.getId(),
                account.getId(), 60L, 30L, null, null);
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), null, account.getId(),
                Map.of(resource.getId(), 100L), Map.of(resource.getId(), 40L));
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
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
        stubProviderService.setGetAccountResponses(
                List.of(
                        GrpcResponse.failure(new StatusRuntimeException(Status.NOT_FOUND, new Metadata())),
                        GrpcResponse.failure(new StatusRuntimeException(Status.NOT_FOUND, new Metadata())),
                        GrpcResponse.failure(new StatusRuntimeException(Status.NOT_FOUND, new Metadata()))
                ));
        stubProviderService.setUpdateProvisionResponses(
                List.of(
                        GrpcResponse.failure(new StatusRuntimeException(Status.NOT_FOUND, new Metadata())),
                        GrpcResponse.failure(new StatusRuntimeException(Status.NOT_FOUND, new Metadata())),
                        GrpcResponse.failure(new StatusRuntimeException(Status.NOT_FOUND, new Metadata()))
                ));
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();


        Optional<AccountsQuotasOperationsModel> updatedOperation = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        accountsQuotasOperationsDao.getById(txSession, operation.getOperationId(),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(updatedOperation);
        Assertions.assertTrue(updatedOperation.isPresent());
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.ERROR,
                updatedOperation.get().getRequestStatus().get());
        Assertions.assertTrue(updatedOperation.get().getOrders().getCloseOrder().isEmpty());
        Assertions.assertTrue(updatedOperation.get().getErrorMessage().isPresent());
        List<OperationInProgressModel> inProgressAfter = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        operationsInProgressDao.getAllByTenant(txSession, Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(inProgressAfter);
        Assertions.assertTrue(inProgressAfter.isEmpty());
        List<QuotaModel> updatedQuotas = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folder.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(updatedQuotas);
        Assertions.assertEquals(1, updatedQuotas.size());
        Assertions.assertEquals(folder.getId(), updatedQuotas.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), updatedQuotas.get(0).getProviderId());
        Assertions.assertEquals(resource.getId(), updatedQuotas.get(0).getResourceId());
        Assertions.assertEquals(300L, updatedQuotas.get(0).getQuota());
        Assertions.assertEquals(240L, updatedQuotas.get(0).getBalance());
        Assertions.assertEquals(0L, updatedQuotas.get(0).getFrozenQuota());
        List<AccountsQuotasModel> updatedProvisions = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                        .getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID, Set.of(account.getId()))))
                .block();
        Assertions.assertNotNull(updatedProvisions);
        Assertions.assertEquals(1, updatedProvisions.size());
        Assertions.assertEquals(account.getId(), updatedProvisions.get(0).getAccountId());
        Assertions.assertEquals(resource.getId(), updatedProvisions.get(0).getResourceId());
        Assertions.assertEquals(folder.getId(), updatedProvisions.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), updatedProvisions.get(0).getProviderId());
        Assertions.assertEquals(60L, updatedProvisions.get(0).getProvidedQuota());
        Assertions.assertEquals(30L, updatedProvisions.get(0).getAllocatedQuota());
        Assertions.assertTrue(updatedProvisions.get(0).getLastReceivedProvisionVersion().isEmpty());
        Assertions.assertFalse(updatedProvisions.get(0).getLatestSuccessfulProvisionOperationId().isPresent());
        List<FolderOperationLogModel> newOpLog = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
                        .getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folder.getId(), SortOrderDto.ASC,
                                100)))
                .block();
        Assertions.assertNotNull(newOpLog);
        Assertions.assertTrue(newOpLog.isEmpty());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testCompleteFailureNoAccountsSpacePull() {
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
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", null);
        QuotaModel quota = quotaModel(provider.getId(), resource.getId(), folder.getId(), 300L, 100L, 0L);
        AccountModel account = accountModel(provider.getId(), null, "test-id", "test",
                folder.getId(), "Test", null, null, false);
        AccountsQuotasModel provision = accountQuotaModel(provider.getId(), resource.getId(), folder.getId(),
                account.getId(), 200L, 150L, null, null);
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), null, account.getId(),
                Map.of(resource.getId(), 120L), Map.of(resource.getId(), 0L));
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
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
        stubProviderService.setGetAccountResponses(
                List.of(
                        GrpcResponse.failure(new StatusRuntimeException(Status.UNKNOWN))
                )
        );
        stubProviderService.setUpdateProvisionResponses(
                List.of(
                        GrpcResponse.failure(new StatusRuntimeException(Status.UNKNOWN))
                ));
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Optional<AccountModel> updatedAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "test-id", null)))))
                .block();
        Assertions.assertNotNull(updatedAccount);
        Assertions.assertTrue(updatedAccount.isPresent());
        Assertions.assertEquals(provider.getId(), updatedAccount.get().getProviderId());
        Assertions.assertEquals("test-id", updatedAccount.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("test", updatedAccount.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), updatedAccount.get().getFolderId());
        Assertions.assertEquals("Test", updatedAccount.get().getDisplayName().get());
        Assertions.assertEquals(0L, updatedAccount.get().getVersion());
        Assertions.assertFalse(updatedAccount.get().isDeleted());
        Assertions.assertFalse(updatedAccount.get().getLastReceivedVersion().isPresent());
        Assertions.assertTrue(updatedAccount.get().getLatestSuccessfulAccountOperationId().isEmpty());
        Optional<AccountsQuotasOperationsModel> updatedOperation = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        accountsQuotasOperationsDao.getById(txSession, operation.getOperationId(),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(updatedOperation);
        Assertions.assertTrue(updatedOperation.isPresent());
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.ERROR,
                updatedOperation.get().getRequestStatus().get());
        Assertions.assertTrue(updatedOperation.get().getErrorMessage().isPresent());
        Assertions.assertTrue(updatedOperation.get().getOrders().getCloseOrder().isEmpty());
        List<OperationInProgressModel> inProgressAfter = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        operationsInProgressDao.getAllByTenant(txSession, Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(inProgressAfter);
        Assertions.assertTrue(inProgressAfter.isEmpty());
        List<QuotaModel> updatedQuotas = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folder.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(updatedQuotas);
        Assertions.assertEquals(1, updatedQuotas.size());
        Assertions.assertEquals(folder.getId(), updatedQuotas.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), updatedQuotas.get(0).getProviderId());
        Assertions.assertEquals(resource.getId(), updatedQuotas.get(0).getResourceId());
        Assertions.assertEquals(300L, updatedQuotas.get(0).getQuota());
        Assertions.assertEquals(100L, updatedQuotas.get(0).getBalance());
        Assertions.assertEquals(0L, updatedQuotas.get(0).getFrozenQuota());
        List<AccountsQuotasModel> updatedProvisions = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                        .getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID, Set.of(account.getId()))))
                .block();
        Assertions.assertNotNull(updatedProvisions);
        Assertions.assertEquals(1, updatedProvisions.size());
        Assertions.assertEquals(account.getId(), updatedProvisions.get(0).getAccountId());
        Assertions.assertEquals(resource.getId(), updatedProvisions.get(0).getResourceId());
        Assertions.assertEquals(folder.getId(), updatedProvisions.get(0).getFolderId());
        Assertions.assertEquals(provider.getId(), updatedProvisions.get(0).getProviderId());
        Assertions.assertEquals(200L, updatedProvisions.get(0).getProvidedQuota());
        Assertions.assertEquals(150L, updatedProvisions.get(0).getAllocatedQuota());
        Assertions.assertTrue(updatedProvisions.get(0).getLastReceivedProvisionVersion().isEmpty());
        Assertions.assertTrue(updatedProvisions.get(0).getLatestSuccessfulProvisionOperationId().isEmpty());
        List<FolderOperationLogModel> newOpLog = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
                        .getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folder.getId(), SortOrderDto.ASC,
                                100)))
                .block();
        Assertions.assertNotNull(newOpLog);
        Assertions.assertTrue(newOpLog.isEmpty());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testRetryCounter() {
        ProviderModel provider = DummyModels.providerModel();
        dbHelper.upsertProvider(provider);

        ResourceTypeModel resourceType = resourceTypeModel(provider.getId(), "default", STORAGE_UNITS_DECIMAL_ID);
        dbHelper.upsertResourceType(resourceType);

        ResourceModel resource = DummyModels.resourceModel(provider.getId(), "default", resourceType.getId(),
                STORAGE_UNITS_DECIMAL_ID, Set.of(GIGABYTES, BYTES), GIGABYTES, BYTES);
        dbHelper.upsertResource(resource);

        FolderModel folder = folderModel(1L);
        dbHelper.upsertFolder(folder);

        QuotaModel quota = quotaModel(provider.getId(), resource.getId(), folder.getId(), 300L, 200L, 40L);
        dbHelper.upsertQuota(quota);

        AccountModel account = accountModel(provider.getId(), null, "test-id", "test",
                folder.getId(), "Test", null, null, false);
        dbHelper.upsertAccount(account);

        AccountsQuotasModel provision = accountQuotaModel(provider.getId(), resource.getId(), folder.getId(),
                account.getId(), 60L, 30L, null, null);
        dbHelper.upsertAccountsQuota(provision);

        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), null, account.getId(),
                Map.of(resource.getId(), 100L), Map.of(resource.getId(), 40L));
        dbHelper.upsertAccountsQuotasOperation(operation);

        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        dbHelper.upsertOperationsInProgress(inProgress);

        stubProviderService.setGetAccountResponses(List.of(
                GrpcResponse.failure(new StatusRuntimeException(Status.UNKNOWN)),
                GrpcResponse.failure(new StatusRuntimeException(Status.ABORTED))
        ));
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        var newInProgress = dbHelper.readDb(tx -> operationsInProgressDao.getById(tx,
                inProgress.getKey(), inProgress.getTenantId()));
        Assertions.assertEquals(1, newInProgress.get().getRetryCounter());

        stubProviderService.reset();
        stubProviderService.setGetAccountResponses(List.of(
                GrpcResponse.failure(new StatusRuntimeException(Status.UNKNOWN)),
                GrpcResponse.failure(new StatusRuntimeException(Status.ABORTED))
        ));
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        newInProgress = dbHelper.readDb(tx -> operationsInProgressDao.getById(tx,
                inProgress.getKey(), inProgress.getTenantId()));
        Assertions.assertEquals(2, newInProgress.get().getRetryCounter());

        stubProviderService.reset();
        stubProviderService.setGetAccountResponses(List.of(
                GrpcResponse.failure(new StatusRuntimeException(Status.UNKNOWN)),
                GrpcResponse.failure(new StatusRuntimeException(Status.ABORTED))
        ));
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        newInProgress = dbHelper.readDb(tx -> operationsInProgressDao.getById(tx,
                inProgress.getKey(), inProgress.getTenantId()));
        Assertions.assertEquals(3, newInProgress.get().getRetryCounter());

        stubProviderService.reset();
        stubProviderService.setGetAccountResponses(List.of(
                GrpcResponse.failure(new StatusRuntimeException(Status.UNKNOWN)),
                GrpcResponse.failure(new StatusRuntimeException(Status.ABORTED))
        ));
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        newInProgress = dbHelper.readDb(tx -> operationsInProgressDao.getById(tx,
                inProgress.getKey(), inProgress.getTenantId()));
        Assertions.assertTrue(newInProgress.isEmpty());

        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testRetryCounterNonFatalError() {
        ProviderModel provider = DummyModels.providerModel();
        dbHelper.upsertProvider(provider);

        ResourceTypeModel resourceType = resourceTypeModel(provider.getId(), "default", STORAGE_UNITS_DECIMAL_ID);
        dbHelper.upsertResourceType(resourceType);

        ResourceModel resource = DummyModels.resourceModel(provider.getId(), "default", resourceType.getId(),
                STORAGE_UNITS_DECIMAL_ID, Set.of(GIGABYTES, BYTES), GIGABYTES, BYTES);
        dbHelper.upsertResource(resource);

        FolderModel folder = folderModel(1L);
        dbHelper.upsertFolder(folder);

        QuotaModel quota = quotaModel(provider.getId(), resource.getId(), folder.getId(), 300L, 200L, 40L);
        dbHelper.upsertQuota(quota);

        AccountModel account = accountModel(provider.getId(), null, "test-id", "test",
                folder.getId(), "Test", null, null, false);
        dbHelper.upsertAccount(account);

        AccountsQuotasModel provision = accountQuotaModel(provider.getId(), resource.getId(), folder.getId(),
                account.getId(), 60L, 30L, null, null);
        dbHelper.upsertAccountsQuota(provision);

        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), null, account.getId(),
                Map.of(resource.getId(), 100L), Map.of(resource.getId(), 40L));
        dbHelper.upsertAccountsQuotasOperation(operation);

        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        dbHelper.upsertOperationsInProgress(inProgress);

        stubProviderService.setGetAccountResponses(List.of(
                GrpcResponse.failure(new StatusRuntimeException(Status.ABORTED))
        ));
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        var newInProgress = dbHelper.readDb(tx -> operationsInProgressDao.getById(tx,
                inProgress.getKey(), inProgress.getTenantId()));
        Assertions.assertEquals(1, newInProgress.get().getRetryCounter());

        stubProviderService.reset();
        stubProviderService.setGetAccountResponses(List.of(
                GrpcResponse.failure(new StatusRuntimeException(Status.ABORTED))
        ));
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        newInProgress = dbHelper.readDb(tx -> operationsInProgressDao.getById(tx,
                inProgress.getKey(), inProgress.getTenantId()));
        Assertions.assertEquals(2, newInProgress.get().getRetryCounter());

        stubProviderService.reset();
        stubProviderService.setGetAccountResponses(List.of(
                GrpcResponse.failure(new StatusRuntimeException(Status.ABORTED))
        ));
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        newInProgress = dbHelper.readDb(tx -> operationsInProgressDao.getById(tx,
                inProgress.getKey(), inProgress.getTenantId()));
        Assertions.assertEquals(3, newInProgress.get().getRetryCounter());

        stubProviderService.reset();
        stubProviderService.setGetAccountResponses(List.of(
                GrpcResponse.failure(new StatusRuntimeException(Status.ABORTED))
        ));
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        newInProgress = dbHelper.readDb(tx -> operationsInProgressDao.getById(tx,
                inProgress.getKey(), inProgress.getTenantId()));
        Assertions.assertTrue(newInProgress.isEmpty());

        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testLastRequestUpdateOnRetry() {
        ProviderModel provider = DummyModels.providerModel();
        dbHelper.upsertProvider(provider);

        ResourceTypeModel resourceType = resourceTypeModel(provider.getId(), "default", STORAGE_UNITS_DECIMAL_ID);
        dbHelper.upsertResourceType(resourceType);

        ResourceModel resource = DummyModels.resourceModel(provider.getId(), "default", resourceType.getId(),
                STORAGE_UNITS_DECIMAL_ID, Set.of(GIGABYTES, BYTES), GIGABYTES, BYTES);
        dbHelper.upsertResource(resource);

        FolderModel folder = folderModel(1L);
        dbHelper.upsertFolder(folder);

        QuotaModel quota = quotaModel(provider.getId(), resource.getId(), folder.getId(), 300L, 200L, 40L);
        dbHelper.upsertQuota(quota);

        AccountModel account = accountModel(provider.getId(), null, "test-id", "test",
                folder.getId(), "Test", null, null, false);
        dbHelper.upsertAccount(account);

        AccountsQuotasModel provision = accountQuotaModel(provider.getId(), resource.getId(), folder.getId(),
                account.getId(), 60L, 30L, null, null);
        dbHelper.upsertAccountsQuota(provision);

        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), null, account.getId(),
                Map.of(resource.getId(), 100L), Map.of(resource.getId(), 40L));
        dbHelper.upsertAccountsQuotasOperation(operation);

        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        dbHelper.upsertOperationsInProgress(inProgress);

        stubProviderService.setGetAccountResponses(List.of(
                GrpcResponse.failure(new StatusRuntimeException(Status.UNKNOWN)),
                GrpcResponse.failure(new StatusRuntimeException(Status.ABORTED))
        ));
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        var newInProgress = dbHelper.readDb(tx -> operationsInProgressDao.getById(tx,
                inProgress.getKey(), inProgress.getTenantId()));
        Assertions.assertEquals(1, newInProgress.get().getRetryCounter());
        var currentOperation = dbHelper.readDb(tx -> accountsQuotasOperationsDao.getById(tx,
                inProgress.getOperationId(), inProgress.getTenantId()));
        Assertions.assertNotNull(currentOperation.get());
        Assertions.assertNotEquals(currentOperation.get().getLastRequestId(), operation.getLastRequestId());
        operation = currentOperation.get();

        stubProviderService.reset();
        stubProviderService.setGetAccountResponses(List.of(
                GrpcResponse.failure(new StatusRuntimeException(Status.UNKNOWN)),
                GrpcResponse.failure(new StatusRuntimeException(Status.ABORTED))
        ));
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        newInProgress = dbHelper.readDb(tx -> operationsInProgressDao.getById(tx,
                inProgress.getKey(), inProgress.getTenantId()));
        Assertions.assertEquals(2, newInProgress.get().getRetryCounter());
        currentOperation = dbHelper.readDb(tx -> accountsQuotasOperationsDao.getById(tx,
                inProgress.getOperationId(), inProgress.getTenantId()));
        Assertions.assertNotNull(currentOperation.get());
        Assertions.assertNotEquals(currentOperation.get().getLastRequestId(), operation.getLastRequestId());

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
                                      String lastOpId, boolean deleted) {
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
                .setDeleted(deleted)
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
