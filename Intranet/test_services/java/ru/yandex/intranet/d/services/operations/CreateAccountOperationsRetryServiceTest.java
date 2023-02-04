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
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundAccountsSpaceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.LastUpdate;
import ru.yandex.intranet.d.backend.service.provider_proto.ListAccountsByFolderResponse;
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceSegmentKey;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsDao;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasOperationsDao;
import ru.yandex.intranet.d.dao.accounts.AccountsSpacesDao;
import ru.yandex.intranet.d.dao.accounts.OperationsInProgressDao;
import ru.yandex.intranet.d.dao.folders.FolderDao;
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao;
import ru.yandex.intranet.d.dao.providers.ProvidersDao;
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao;
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse;
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService;
import ru.yandex.intranet.d.i18n.Locales;
import ru.yandex.intranet.d.model.WithTenant;
import ru.yandex.intranet.d.model.accounts.AccountModel;
import ru.yandex.intranet.d.model.accounts.AccountSpaceModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel;
import ru.yandex.intranet.d.model.accounts.OperationChangesModel;
import ru.yandex.intranet.d.model.accounts.OperationInProgressModel;
import ru.yandex.intranet.d.model.accounts.OperationOrdersModel;
import ru.yandex.intranet.d.model.accounts.OperationSource;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
import ru.yandex.intranet.d.model.folders.FolderOperationType;
import ru.yandex.intranet.d.model.folders.FolderType;
import ru.yandex.intranet.d.model.folders.QuotasByAccount;
import ru.yandex.intranet.d.model.folders.QuotasByResource;
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel;
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel;
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel;
import ru.yandex.intranet.d.web.model.SortOrderDto;

/**
 * Create account operations retry service test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class CreateAccountOperationsRetryServiceTest {

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
    private ResourceSegmentationsDao resourceSegmentationsDao;
    @Autowired
    private ResourceSegmentsDao resourceSegmentsDao;
    @Autowired
    private YdbTableClient tableClient;
    @Autowired
    private StubProviderService stubProviderService;
    @Autowired
    private OperationsRetryService operationsRetryService;

    @Test
    @SuppressWarnings("MethodLength")
    public void testSuccessAfterRefreshNoAccountsSpace() {
        ProviderModel provider = providerModel(GRPC_URI, null, false, false, true, true, false, false, false, false);
        FolderModel folder = folderModel(1L);
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), null, folder.getId());
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                        .upsertOneRetryable(txSession, operation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> operationsInProgressDao
                        .upsertOneRetryable(txSession, inProgress)))
                .block();
        stubProviderService.setListAccountsByFolderResponses(
                List.of(
                        GrpcResponse.success(
                                ListAccountsByFolderResponse.newBuilder()
                                        .addAccounts(Account.newBuilder()
                                                .setAccountId("test-id")
                                                .setKey("test")
                                                .setDisplayName("Test")
                                                .setFolderId(folder.getId())
                                                .setDeleted(false)
                                                .build())
                                        .build()
                        )
                )
        );
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Optional<AccountModel> createdAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "test-id", null)))))
                .block();
        Assertions.assertNotNull(createdAccount);
        Assertions.assertTrue(createdAccount.isPresent());
        Assertions.assertEquals(provider.getId(), createdAccount.get().getProviderId());
        Assertions.assertEquals("test-id", createdAccount.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("test", createdAccount.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), createdAccount.get().getFolderId());
        Assertions.assertEquals("Test", createdAccount.get().getDisplayName().get());
        Assertions.assertEquals(0L, createdAccount.get().getVersion());
        Assertions.assertFalse(createdAccount.get().isDeleted());
        Assertions.assertFalse(createdAccount.get().getLastReceivedVersion().isPresent());
        Assertions.assertEquals(operation.getOperationId(),
                createdAccount.get().getLatestSuccessfulAccountOperationId().get());
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
        List<FolderOperationLogModel> newOpLog = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
                        .getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folder.getId(), SortOrderDto.ASC,
                                100)))
                .block();
        Assertions.assertNotNull(newOpLog);
        Assertions.assertEquals(1, newOpLog.size());
        Assertions.assertEquals(folder.getId(), newOpLog.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.CREATE_ACCOUNT, newOpLog.get(0).getOperationType());
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
        Assertions.assertEquals(new QuotasByAccount(Map.of()), newOpLog.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of()), newOpLog.get(0).getNewProvisions());
        Assertions.assertTrue(newOpLog.get(0).getOldAccounts().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().isPresent());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .containsKey(createdAccount.get().getId()));
        Assertions.assertEquals(provider.getId(), newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getProviderId().get());
        Assertions.assertEquals("test-id", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getOuterAccountIdInProvider().get());
        Assertions.assertEquals("test", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getFolderId().get());
        Assertions.assertEquals("Test", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getDisplayName().get());
        Assertions.assertFalse(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getDeleted().get());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getLastReceivedVersion().isEmpty());
        Assertions.assertEquals(2L, newOpLog.get(0).getOrder());
        Assertions.assertEquals(operation.getOperationId(), newOpLog.get(0).getAccountsQuotasOperationsId().get());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testSuccessAfterRetryNoAccountsSpace() {
        ProviderModel provider = providerModel(GRPC_URI, null, false, false, true, true, false, false, false, false);
        FolderModel folder = folderModel(1L);
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), null, folder.getId());
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                        .upsertOneRetryable(txSession, operation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> operationsInProgressDao
                        .upsertOneRetryable(txSession, inProgress)))
                .block();
        stubProviderService.setListAccountsByFolderResponses(
                List.of(
                        GrpcResponse.success(
                                ListAccountsByFolderResponse.newBuilder().build()
                        )
                )
        );
        stubProviderService.setCreateAccountResponses(
                List.of(
                        GrpcResponse.success(
                                Account.newBuilder()
                                        .setAccountId("test-id")
                                        .setKey("test")
                                        .setDisplayName("Test")
                                        .setFolderId(folder.getId())
                                        .setDeleted(false)
                                        .build()
                        )
                )
        );
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Optional<AccountModel> createdAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "test-id", null)))))
                .block();
        Assertions.assertNotNull(createdAccount);
        Assertions.assertTrue(createdAccount.isPresent());
        Assertions.assertEquals(provider.getId(), createdAccount.get().getProviderId());
        Assertions.assertEquals("test-id", createdAccount.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("test", createdAccount.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), createdAccount.get().getFolderId());
        Assertions.assertEquals("Test", createdAccount.get().getDisplayName().get());
        Assertions.assertEquals(0L, createdAccount.get().getVersion());
        Assertions.assertFalse(createdAccount.get().isDeleted());
        Assertions.assertFalse(createdAccount.get().getLastReceivedVersion().isPresent());
        Assertions.assertEquals(operation.getOperationId(),
                createdAccount.get().getLatestSuccessfulAccountOperationId().get());
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
        List<FolderOperationLogModel> newOpLog = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
                        .getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folder.getId(), SortOrderDto.ASC,
                                100)))
                .block();
        Assertions.assertNotNull(newOpLog);
        Assertions.assertEquals(1, newOpLog.size());
        Assertions.assertEquals(folder.getId(), newOpLog.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.CREATE_ACCOUNT, newOpLog.get(0).getOperationType());
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
        Assertions.assertEquals(new QuotasByAccount(Map.of()), newOpLog.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of()), newOpLog.get(0).getNewProvisions());
        Assertions.assertTrue(newOpLog.get(0).getOldAccounts().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().isPresent());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .containsKey(createdAccount.get().getId()));
        Assertions.assertEquals(provider.getId(), newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getProviderId().get());
        Assertions.assertEquals("test-id", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getOuterAccountIdInProvider().get());
        Assertions.assertEquals("test", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getFolderId().get());
        Assertions.assertEquals("Test", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getDisplayName().get());
        Assertions.assertFalse(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getDeleted().get());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getLastReceivedVersion().isEmpty());
        Assertions.assertEquals(2L, newOpLog.get(0).getOrder());
        Assertions.assertEquals(operation.getOperationId(), newOpLog.get(0).getAccountsQuotasOperationsId().get());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testSuccessAfterConflictNoAccountsSpace() {
        ProviderModel provider = providerModel(GRPC_URI, null, false, false, true, true, false, false, false, false);
        FolderModel folder = folderModel(1L);
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), null, folder.getId());
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                        .upsertOneRetryable(txSession, operation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> operationsInProgressDao
                        .upsertOneRetryable(txSession, inProgress)))
                .block();
        stubProviderService.setListAccountsByFolderResponses(
                List.of(
                        GrpcResponse.success(
                                ListAccountsByFolderResponse.newBuilder().build()
                        ),
                        GrpcResponse.success(
                                ListAccountsByFolderResponse.newBuilder()
                                        .addAccounts(Account.newBuilder()
                                                .setAccountId("test-id")
                                                .setKey("test")
                                                .setDisplayName("Test")
                                                .setFolderId(folder.getId())
                                                .setDeleted(false)
                                                .build())
                                        .build()
                        )
                )
        );
        stubProviderService.setCreateAccountResponses(
                List.of(
                        GrpcResponse.failure(new StatusRuntimeException(Status.ALREADY_EXISTS, new Metadata())),
                        GrpcResponse.failure(new StatusRuntimeException(Status.ALREADY_EXISTS, new Metadata())),
                        GrpcResponse.failure(new StatusRuntimeException(Status.ALREADY_EXISTS, new Metadata()))
                )
        );
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Optional<AccountModel> createdAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "test-id", null)))))
                .block();
        Assertions.assertNotNull(createdAccount);
        Assertions.assertTrue(createdAccount.isPresent());
        Assertions.assertEquals(provider.getId(), createdAccount.get().getProviderId());
        Assertions.assertEquals("test-id", createdAccount.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("test", createdAccount.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), createdAccount.get().getFolderId());
        Assertions.assertEquals("Test", createdAccount.get().getDisplayName().get());
        Assertions.assertEquals(0L, createdAccount.get().getVersion());
        Assertions.assertFalse(createdAccount.get().isDeleted());
        Assertions.assertFalse(createdAccount.get().getLastReceivedVersion().isPresent());
        Assertions.assertEquals(operation.getOperationId(),
                createdAccount.get().getLatestSuccessfulAccountOperationId().get());
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
        List<FolderOperationLogModel> newOpLog = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
                        .getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folder.getId(), SortOrderDto.ASC,
                                100)))
                .block();
        Assertions.assertNotNull(newOpLog);
        Assertions.assertEquals(1, newOpLog.size());
        Assertions.assertEquals(folder.getId(), newOpLog.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.CREATE_ACCOUNT, newOpLog.get(0).getOperationType());
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
        Assertions.assertEquals(new QuotasByAccount(Map.of()), newOpLog.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of()), newOpLog.get(0).getNewProvisions());
        Assertions.assertTrue(newOpLog.get(0).getOldAccounts().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().isPresent());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .containsKey(createdAccount.get().getId()));
        Assertions.assertEquals(provider.getId(), newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getProviderId().get());
        Assertions.assertEquals("test-id", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getOuterAccountIdInProvider().get());
        Assertions.assertEquals("test", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getFolderId().get());
        Assertions.assertEquals("Test", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getDisplayName().get());
        Assertions.assertFalse(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getDeleted().get());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getLastReceivedVersion().isEmpty());
        Assertions.assertEquals(2L, newOpLog.get(0).getOrder());
        Assertions.assertEquals(operation.getOperationId(), newOpLog.get(0).getAccountsQuotasOperationsId().get());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testSuccessAfterRefreshAccountsSpace() {
        ProviderModel provider = providerModel(GRPC_URI, null, true, false, true, true, false, false, false, false);
        FolderModel folder = folderModel(1L);
        ResourceSegmentationModel locationSegmentation = resourceSegmentationModel(provider.getId(), "location");
        ResourceSegmentModel vlaSegment = resourceSegmentModel(locationSegmentation.getId(), "VLA");
        AccountSpaceModel accountsSpace = accountSpaceModel(provider.getId(), "test",
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())));
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), accountsSpace.getId(),
                folder.getId());
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
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
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                        .upsertOneRetryable(txSession, operation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> operationsInProgressDao
                        .upsertOneRetryable(txSession, inProgress)))
                .block();
        stubProviderService.setListAccountsByFolderResponses(
                List.of(
                        GrpcResponse.success(
                                ListAccountsByFolderResponse.newBuilder()
                                        .addAccounts(Account.newBuilder()
                                                .setAccountId("test-id")
                                                .setKey("test")
                                                .setDisplayName("Test")
                                                .setFolderId(folder.getId())
                                                .setDeleted(false)
                                                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                                        .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                                                .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                                                        .setResourceSegmentationKey("location")
                                                                        .setResourceSegmentKey("VLA")
                                                                        .build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .build()
                        )
                )
        );
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Optional<AccountModel> createdAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "test-id",
                                        accountsSpace.getId())))))
                .block();
        Assertions.assertNotNull(createdAccount);
        Assertions.assertTrue(createdAccount.isPresent());
        Assertions.assertEquals(provider.getId(), createdAccount.get().getProviderId());
        Assertions.assertEquals("test-id", createdAccount.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("test", createdAccount.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), createdAccount.get().getFolderId());
        Assertions.assertEquals("Test", createdAccount.get().getDisplayName().get());
        Assertions.assertEquals(0L, createdAccount.get().getVersion());
        Assertions.assertFalse(createdAccount.get().isDeleted());
        Assertions.assertFalse(createdAccount.get().getLastReceivedVersion().isPresent());
        Assertions.assertEquals(operation.getOperationId(),
                createdAccount.get().getLatestSuccessfulAccountOperationId().get());
        Assertions.assertEquals(accountsSpace.getId(), createdAccount.get().getAccountsSpacesId().get());
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
        List<FolderOperationLogModel> newOpLog = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
                        .getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folder.getId(), SortOrderDto.ASC,
                                100)))
                .block();
        Assertions.assertNotNull(newOpLog);
        Assertions.assertEquals(1, newOpLog.size());
        Assertions.assertEquals(folder.getId(), newOpLog.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.CREATE_ACCOUNT, newOpLog.get(0).getOperationType());
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
        Assertions.assertEquals(new QuotasByAccount(Map.of()), newOpLog.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of()), newOpLog.get(0).getNewProvisions());
        Assertions.assertTrue(newOpLog.get(0).getOldAccounts().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().isPresent());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .containsKey(createdAccount.get().getId()));
        Assertions.assertEquals(provider.getId(), newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getProviderId().get());
        Assertions.assertEquals("test-id", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getOuterAccountIdInProvider().get());
        Assertions.assertEquals("test", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getFolderId().get());
        Assertions.assertEquals("Test", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getDisplayName().get());
        Assertions.assertFalse(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getDeleted().get());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getLastReceivedVersion().isEmpty());
        Assertions.assertEquals(2L, newOpLog.get(0).getOrder());
        Assertions.assertEquals(operation.getOperationId(), newOpLog.get(0).getAccountsQuotasOperationsId().get());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testSuccessAfterRetryAccountsSpace() {
        ProviderModel provider = providerModel(GRPC_URI, null, true, false, true, true, false, false, false, false);
        FolderModel folder = folderModel(1L);
        ResourceSegmentationModel locationSegmentation = resourceSegmentationModel(provider.getId(), "location");
        ResourceSegmentModel vlaSegment = resourceSegmentModel(locationSegmentation.getId(), "VLA");
        AccountSpaceModel accountsSpace = accountSpaceModel(provider.getId(), "test",
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())));
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), accountsSpace.getId(),
                folder.getId());
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
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
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                        .upsertOneRetryable(txSession, operation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> operationsInProgressDao
                        .upsertOneRetryable(txSession, inProgress)))
                .block();
        stubProviderService.setListAccountsByFolderResponses(
                List.of(
                        GrpcResponse.success(
                                ListAccountsByFolderResponse.newBuilder().build()
                        )
                )
        );
        stubProviderService.setCreateAccountResponses(
                List.of(
                        GrpcResponse.success(
                                Account.newBuilder()
                                        .setAccountId("test-id")
                                        .setKey("test")
                                        .setDisplayName("Test")
                                        .setFolderId(folder.getId())
                                        .setDeleted(false)
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
        Optional<AccountModel> createdAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "test-id", accountsSpace.getId())))))
                .block();
        Assertions.assertNotNull(createdAccount);
        Assertions.assertTrue(createdAccount.isPresent());
        Assertions.assertEquals(provider.getId(), createdAccount.get().getProviderId());
        Assertions.assertEquals("test-id", createdAccount.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("test", createdAccount.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), createdAccount.get().getFolderId());
        Assertions.assertEquals("Test", createdAccount.get().getDisplayName().get());
        Assertions.assertEquals(0L, createdAccount.get().getVersion());
        Assertions.assertFalse(createdAccount.get().isDeleted());
        Assertions.assertFalse(createdAccount.get().getLastReceivedVersion().isPresent());
        Assertions.assertEquals(operation.getOperationId(),
                createdAccount.get().getLatestSuccessfulAccountOperationId().get());
        Assertions.assertEquals(accountsSpace.getId(), createdAccount.get().getAccountsSpacesId().get());
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
        List<FolderOperationLogModel> newOpLog = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
                        .getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folder.getId(), SortOrderDto.ASC,
                                100)))
                .block();
        Assertions.assertNotNull(newOpLog);
        Assertions.assertEquals(1, newOpLog.size());
        Assertions.assertEquals(folder.getId(), newOpLog.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.CREATE_ACCOUNT, newOpLog.get(0).getOperationType());
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
        Assertions.assertEquals(new QuotasByAccount(Map.of()), newOpLog.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of()), newOpLog.get(0).getNewProvisions());
        Assertions.assertTrue(newOpLog.get(0).getOldAccounts().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().isPresent());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .containsKey(createdAccount.get().getId()));
        Assertions.assertEquals(provider.getId(), newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getProviderId().get());
        Assertions.assertEquals("test-id", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getOuterAccountIdInProvider().get());
        Assertions.assertEquals("test", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getFolderId().get());
        Assertions.assertEquals("Test", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getDisplayName().get());
        Assertions.assertFalse(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getDeleted().get());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getLastReceivedVersion().isEmpty());
        Assertions.assertEquals(2L, newOpLog.get(0).getOrder());
        Assertions.assertEquals(operation.getOperationId(), newOpLog.get(0).getAccountsQuotasOperationsId().get());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testSuccessAfterConflictAccountsSpace() {
        ProviderModel provider = providerModel(GRPC_URI, null, true, false, true, true, false, false, false, false);
        FolderModel folder = folderModel(1L);
        ResourceSegmentationModel locationSegmentation = resourceSegmentationModel(provider.getId(), "location");
        ResourceSegmentModel vlaSegment = resourceSegmentModel(locationSegmentation.getId(), "VLA");
        AccountSpaceModel accountsSpace = accountSpaceModel(provider.getId(), "test",
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())));
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), accountsSpace.getId(),
                folder.getId());
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
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
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                        .upsertOneRetryable(txSession, operation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> operationsInProgressDao
                        .upsertOneRetryable(txSession, inProgress)))
                .block();
        stubProviderService.setListAccountsByFolderResponses(
                List.of(
                        GrpcResponse.success(
                                ListAccountsByFolderResponse.newBuilder().build()
                        ),
                        GrpcResponse.success(
                                ListAccountsByFolderResponse.newBuilder()
                                        .addAccounts(Account.newBuilder()
                                                .setAccountId("test-id")
                                                .setKey("test")
                                                .setDisplayName("Test")
                                                .setFolderId(folder.getId())
                                                .setDeleted(false)
                                                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                                        .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                                                .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                                                        .setResourceSegmentationKey("location")
                                                                        .setResourceSegmentKey("VLA")
                                                                        .build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .build()
                        )
                )
        );
        stubProviderService.setCreateAccountResponses(
                List.of(
                        GrpcResponse.failure(new StatusRuntimeException(Status.ALREADY_EXISTS, new Metadata())),
                        GrpcResponse.failure(new StatusRuntimeException(Status.ALREADY_EXISTS, new Metadata())),
                        GrpcResponse.failure(new StatusRuntimeException(Status.ALREADY_EXISTS, new Metadata()))
                )
        );
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Optional<AccountModel> createdAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "test-id", accountsSpace.getId())))))
                .block();
        Assertions.assertNotNull(createdAccount);
        Assertions.assertTrue(createdAccount.isPresent());
        Assertions.assertEquals(provider.getId(), createdAccount.get().getProviderId());
        Assertions.assertEquals("test-id", createdAccount.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("test", createdAccount.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), createdAccount.get().getFolderId());
        Assertions.assertEquals("Test", createdAccount.get().getDisplayName().get());
        Assertions.assertEquals(0L, createdAccount.get().getVersion());
        Assertions.assertFalse(createdAccount.get().isDeleted());
        Assertions.assertFalse(createdAccount.get().getLastReceivedVersion().isPresent());
        Assertions.assertEquals(operation.getOperationId(),
                createdAccount.get().getLatestSuccessfulAccountOperationId().get());
        Assertions.assertEquals(accountsSpace.getId(), createdAccount.get().getAccountsSpacesId().get());
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
        List<FolderOperationLogModel> newOpLog = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
                        .getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folder.getId(), SortOrderDto.ASC,
                                100)))
                .block();
        Assertions.assertNotNull(newOpLog);
        Assertions.assertEquals(1, newOpLog.size());
        Assertions.assertEquals(folder.getId(), newOpLog.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.CREATE_ACCOUNT, newOpLog.get(0).getOperationType());
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
        Assertions.assertEquals(new QuotasByAccount(Map.of()), newOpLog.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of()), newOpLog.get(0).getNewProvisions());
        Assertions.assertTrue(newOpLog.get(0).getOldAccounts().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().isPresent());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .containsKey(createdAccount.get().getId()));
        Assertions.assertEquals(provider.getId(), newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getProviderId().get());
        Assertions.assertEquals("test-id", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getOuterAccountIdInProvider().get());
        Assertions.assertEquals("test", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getFolderId().get());
        Assertions.assertEquals("Test", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getDisplayName().get());
        Assertions.assertFalse(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getDeleted().get());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getLastReceivedVersion().isEmpty());
        Assertions.assertEquals(2L, newOpLog.get(0).getOrder());
        Assertions.assertEquals(operation.getOperationId(), newOpLog.get(0).getAccountsQuotasOperationsId().get());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testNoRetryNoRefresh() {
        ProviderModel provider = providerModel(GRPC_URI, null, false, false, false, true, false, false, false, false);
        FolderModel folder = folderModel(1L);
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), null, folder.getId());
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                        .upsertOneRetryable(txSession, operation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> operationsInProgressDao
                        .upsertOneRetryable(txSession, inProgress)))
                .block();
        stubProviderService.setListAccountsByFolderResponses(
                List.of(
                        GrpcResponse.success(
                                ListAccountsByFolderResponse.newBuilder()
                                        .addAccounts(Account.newBuilder()
                                                .setAccountId("test-id")
                                                .setDisplayName("Test")
                                                .setFolderId(folder.getId())
                                                .setDeleted(false)
                                                .build())
                                        .build()
                        )
                )
        );
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Optional<AccountModel> createdAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "test-id", null)))))
                .block();
        Assertions.assertNotNull(createdAccount);
        Assertions.assertFalse(createdAccount.isPresent());
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
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testCompleteFailure() {
        ProviderModel provider = providerModel(GRPC_URI, null, false, false, true, true, false, false, false, true);
        FolderModel folder = folderModel(1L);
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), null, folder.getId());
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                        .upsertOneRetryable(txSession, operation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> operationsInProgressDao
                        .upsertOneRetryable(txSession, inProgress)))
                .block();
        stubProviderService.setListAccountsByFolderResponses(
                List.of(
                        GrpcResponse.failure(new StatusRuntimeException(Status.INVALID_ARGUMENT)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.INVALID_ARGUMENT)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.INVALID_ARGUMENT)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.INVALID_ARGUMENT)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.INVALID_ARGUMENT)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.INVALID_ARGUMENT))
                )
        );
        stubProviderService.setCreateAccountResponses(
                List.of(
                        GrpcResponse.failure(new StatusRuntimeException(Status.INVALID_ARGUMENT)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.INVALID_ARGUMENT)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.INVALID_ARGUMENT))
                )
        );
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Optional<AccountModel> createdAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "test-id", null)))))
                .block();
        Assertions.assertNotNull(createdAccount);
        Assertions.assertFalse(createdAccount.isPresent());
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
        List<OperationInProgressModel> inProgressAfter = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        operationsInProgressDao.getAllByTenant(txSession, Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(inProgressAfter);
        Assertions.assertTrue(inProgressAfter.isEmpty());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testCompleteFailureOnUnknownError() {
        ProviderModel provider = providerModel(GRPC_URI, null, false, false, true, true, false, false, false, true);
        FolderModel folder = folderModel(1L);
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), null, folder.getId());
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                        .upsertOneRetryable(txSession, operation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> operationsInProgressDao
                        .upsertOneRetryable(txSession, inProgress)))
                .block();
        stubProviderService.setListAccountsByFolderResponses(
                List.of(
                        GrpcResponse.failure(new StatusRuntimeException(Status.UNKNOWN))
                )
        );
        stubProviderService.setCreateAccountResponses(
                List.of(
                        GrpcResponse.failure(new StatusRuntimeException(Status.UNKNOWN))
                )
        );
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Optional<AccountModel> createdAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "test-id", null)))))
                .block();
        Assertions.assertNotNull(createdAccount);
        Assertions.assertFalse(createdAccount.isPresent());
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
        List<OperationInProgressModel> inProgressAfter = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        operationsInProgressDao.getAllByTenant(txSession, Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(inProgressAfter);
        Assertions.assertTrue(inProgressAfter.isEmpty());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testSuccessAfterRefreshOperationIdMatch() {
        ProviderModel provider = providerModel(GRPC_URI, null, false, false, false, true, false, false, true, false);
        FolderModel folder = folderModel(1L);
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), null, folder.getId());
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                        .upsertOneRetryable(txSession, operation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> operationsInProgressDao
                        .upsertOneRetryable(txSession, inProgress)))
                .block();
        stubProviderService.setListAccountsByFolderResponses(
                List.of(
                        GrpcResponse.success(
                                ListAccountsByFolderResponse.newBuilder()
                                        .addAccounts(Account.newBuilder()
                                                .setAccountId("test-id")
                                                .setDisplayName("Test")
                                                .setFolderId(folder.getId())
                                                .setDeleted(false)
                                                .setLastUpdate(LastUpdate.newBuilder()
                                                        .setOperationId(operation.getOperationId())
                                                        .build())
                                                .build())
                                        .build()
                        )
                )
        );
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Optional<AccountModel> createdAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "test-id", null)))))
                .block();
        Assertions.assertNotNull(createdAccount);
        Assertions.assertTrue(createdAccount.isPresent());
        Assertions.assertEquals(provider.getId(), createdAccount.get().getProviderId());
        Assertions.assertEquals("test-id", createdAccount.get().getOuterAccountIdInProvider());
        Assertions.assertTrue(createdAccount.get().getOuterAccountKeyInProvider().isEmpty());
        Assertions.assertEquals(folder.getId(), createdAccount.get().getFolderId());
        Assertions.assertEquals("Test", createdAccount.get().getDisplayName().get());
        Assertions.assertEquals(0L, createdAccount.get().getVersion());
        Assertions.assertFalse(createdAccount.get().isDeleted());
        Assertions.assertFalse(createdAccount.get().getLastReceivedVersion().isPresent());
        Assertions.assertEquals(operation.getOperationId(),
                createdAccount.get().getLatestSuccessfulAccountOperationId().get());
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
        List<FolderOperationLogModel> newOpLog = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
                        .getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folder.getId(), SortOrderDto.ASC,
                                100)))
                .block();
        Assertions.assertNotNull(newOpLog);
        Assertions.assertEquals(1, newOpLog.size());
        Assertions.assertEquals(folder.getId(), newOpLog.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.CREATE_ACCOUNT, newOpLog.get(0).getOperationType());
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
        Assertions.assertEquals(new QuotasByAccount(Map.of()), newOpLog.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of()), newOpLog.get(0).getNewProvisions());
        Assertions.assertTrue(newOpLog.get(0).getOldAccounts().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().isPresent());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .containsKey(createdAccount.get().getId()));
        Assertions.assertEquals(provider.getId(), newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getProviderId().get());
        Assertions.assertEquals("test-id", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getOuterAccountIdInProvider().get());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getOuterAccountKeyInProvider().isEmpty());
        Assertions.assertEquals(folder.getId(), newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getFolderId().get());
        Assertions.assertEquals("Test", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getDisplayName().get());
        Assertions.assertFalse(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getDeleted().get());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getLastReceivedVersion().isEmpty());
        Assertions.assertEquals(2L, newOpLog.get(0).getOrder());
        Assertions.assertEquals(operation.getOperationId(), newOpLog.get(0).getAccountsQuotasOperationsId().get());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testTwoFailures() {
        ProviderModel provider = providerModel(GRPC_URI, null, false, false, true, true, false, false, false, false);
        FolderModel folder = folderModel(1L);
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), null, folder.getId());
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                        .upsertOneRetryable(txSession, operation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> operationsInProgressDao
                        .upsertOneRetryable(txSession, inProgress)))
                .block();
        stubProviderService.setListAccountsByFolderResponses(
                List.of(
                        GrpcResponse.success(
                                ListAccountsByFolderResponse.newBuilder()
                                        .build()
                        ),
                        GrpcResponse.failure(new StatusRuntimeException(Status.INVALID_ARGUMENT)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.INVALID_ARGUMENT)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.INVALID_ARGUMENT))
                )
        );
        stubProviderService.setCreateAccountResponses(
                List.of(
                        GrpcResponse.failure(new StatusRuntimeException(Status.INVALID_ARGUMENT)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.INVALID_ARGUMENT)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.INVALID_ARGUMENT))
                )
        );
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Optional<AccountModel> createdAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "test-id", null)))))
                .block();
        Assertions.assertNotNull(createdAccount);
        Assertions.assertFalse(createdAccount.isPresent());
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
        List<OperationInProgressModel> inProgressAfter = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        operationsInProgressDao.getAllByTenant(txSession, Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(inProgressAfter);
        Assertions.assertTrue(inProgressAfter.isEmpty());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testNextIterationAfterRefresh() {
        ProviderModel provider = providerModel(GRPC_URI, null, false, false, true, true, false, false, false, false);
        FolderModel folder = folderModel(1L);
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), null, folder.getId());
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                        .upsertOneRetryable(txSession, operation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> operationsInProgressDao
                        .upsertOneRetryable(txSession, inProgress)))
                .block();
        stubProviderService.setListAccountsByFolderResponses(
                List.of(
                        GrpcResponse.failure(new StatusRuntimeException(Status.UNAVAILABLE)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.UNAVAILABLE)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.UNAVAILABLE))
                )
        );
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Optional<AccountModel> createdAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "test-id", null)))))
                .block();
        Assertions.assertNotNull(createdAccount);
        Assertions.assertFalse(createdAccount.isPresent());
        Optional<AccountsQuotasOperationsModel> updatedOperation = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        accountsQuotasOperationsDao.getById(txSession, operation.getOperationId(),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(updatedOperation);
        Assertions.assertTrue(updatedOperation.isPresent());
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.WAITING,
                updatedOperation.get().getRequestStatus().get());
        Assertions.assertTrue(updatedOperation.get().getErrorMessage().isEmpty());
        List<OperationInProgressModel> inProgressAfter = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        operationsInProgressDao.getAllByTenant(txSession, Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(inProgressAfter);
        Assertions.assertEquals(1, inProgressAfter.size());
        Assertions.assertEquals(1, inProgressAfter.get(0).getRetryCounter());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testNextIterationAfterRetry() {
        ProviderModel provider = providerModel(GRPC_URI, null, false, false, true, true, false, false, false, false);
        FolderModel folder = folderModel(1L);
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), null, folder.getId());
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                        .upsertOneRetryable(txSession, operation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> operationsInProgressDao
                        .upsertOneRetryable(txSession, inProgress)))
                .block();
        stubProviderService.setListAccountsByFolderResponses(
                List.of(
                        GrpcResponse.success(
                                ListAccountsByFolderResponse.newBuilder()
                                        .build()
                        )
                )
        );
        stubProviderService.setCreateAccountResponses(
                List.of(
                        GrpcResponse.failure(new StatusRuntimeException(Status.UNAVAILABLE)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.UNAVAILABLE)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.UNAVAILABLE))
                )
        );
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Optional<AccountModel> createdAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "test-id", null)))))
                .block();
        Assertions.assertNotNull(createdAccount);
        Assertions.assertFalse(createdAccount.isPresent());
        Optional<AccountsQuotasOperationsModel> updatedOperation = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        accountsQuotasOperationsDao.getById(txSession, operation.getOperationId(),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(updatedOperation);
        Assertions.assertTrue(updatedOperation.isPresent());
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.WAITING,
                updatedOperation.get().getRequestStatus().get());
        Assertions.assertTrue(updatedOperation.get().getErrorMessage().isEmpty());
        List<OperationInProgressModel> inProgressAfter = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        operationsInProgressDao.getAllByTenant(txSession, Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(inProgressAfter);
        Assertions.assertEquals(1, inProgressAfter.size());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testNextIterationAfterPostRetryRefresh() {
        ProviderModel provider = providerModel(GRPC_URI, null, false, false, true, true, false, false, false, false);
        FolderModel folder = folderModel(1L);
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), null, folder.getId());
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                        .upsertOneRetryable(txSession, operation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> operationsInProgressDao
                        .upsertOneRetryable(txSession, inProgress)))
                .block();
        stubProviderService.setListAccountsByFolderResponses(
                List.of(
                        GrpcResponse.success(
                                ListAccountsByFolderResponse.newBuilder()
                                        .build()
                        ),
                        GrpcResponse.failure(new StatusRuntimeException(Status.UNAVAILABLE)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.UNAVAILABLE)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.UNAVAILABLE))
                )
        );
        stubProviderService.setCreateAccountResponses(
                List.of(
                        GrpcResponse.failure(new StatusRuntimeException(Status.ALREADY_EXISTS)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.ALREADY_EXISTS)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.ALREADY_EXISTS))
                )
        );
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Optional<AccountModel> createdAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "test-id", null)))))
                .block();
        Assertions.assertNotNull(createdAccount);
        Assertions.assertFalse(createdAccount.isPresent());
        Optional<AccountsQuotasOperationsModel> updatedOperation = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        accountsQuotasOperationsDao.getById(txSession, operation.getOperationId(),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(updatedOperation);
        Assertions.assertTrue(updatedOperation.isPresent());
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.WAITING,
                updatedOperation.get().getRequestStatus().get());
        Assertions.assertTrue(updatedOperation.get().getErrorMessage().isEmpty());
        List<OperationInProgressModel> inProgressAfter = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        operationsInProgressDao.getAllByTenant(txSession, Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(inProgressAfter);
        Assertions.assertEquals(1, inProgressAfter.size());
        stubProviderService.reset();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testRefreshNotSupported() {
        ProviderModel provider = providerModel(GRPC_URI, null, false, false, true, true, false, false, false, true);
        FolderModel folder = folderModel(1L);
        AccountsQuotasOperationsModel operation = operationModel(provider.getId(), null, folder.getId());
        OperationInProgressModel inProgress = inProgressModel(operation.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                        .upsertOneRetryable(txSession, operation)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> operationsInProgressDao
                        .upsertOneRetryable(txSession, inProgress)))
                .block();
        stubProviderService.setListAccountsByFolderResponses(
                List.of(
                        GrpcResponse.failure(new StatusRuntimeException(Status.UNIMPLEMENTED)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.UNIMPLEMENTED)),
                        GrpcResponse.failure(new StatusRuntimeException(Status.UNIMPLEMENTED))
                )
        );
        stubProviderService.setCreateAccountResponses(
                List.of(
                        GrpcResponse.success(
                                Account.newBuilder()
                                        .setAccountId("test-id")
                                        .setKey("test")
                                        .setDisplayName("Test")
                                        .setFolderId(folder.getId())
                                        .setDeleted(false)
                                        .build()
                        )
                )
        );
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block();
        Optional<AccountModel> createdAccount = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .getAllByExternalId(txSession, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                new AccountModel.ExternalId(provider.getId(), "test-id", null)))))
                .block();
        Assertions.assertNotNull(createdAccount);
        Assertions.assertTrue(createdAccount.isPresent());
        Assertions.assertEquals(provider.getId(), createdAccount.get().getProviderId());
        Assertions.assertEquals("test-id", createdAccount.get().getOuterAccountIdInProvider());
        Assertions.assertEquals("test", createdAccount.get().getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), createdAccount.get().getFolderId());
        Assertions.assertEquals("Test", createdAccount.get().getDisplayName().get());
        Assertions.assertEquals(0L, createdAccount.get().getVersion());
        Assertions.assertFalse(createdAccount.get().isDeleted());
        Assertions.assertFalse(createdAccount.get().getLastReceivedVersion().isPresent());
        Assertions.assertEquals(operation.getOperationId(),
                createdAccount.get().getLatestSuccessfulAccountOperationId().get());
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
        List<FolderOperationLogModel> newOpLog = tableClient.usingSessionMonoRetryable(session -> session
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderOperationLogDao
                        .getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folder.getId(), SortOrderDto.ASC,
                                100)))
                .block();
        Assertions.assertNotNull(newOpLog);
        Assertions.assertEquals(1, newOpLog.size());
        Assertions.assertEquals(folder.getId(), newOpLog.get(0).getFolderId());
        Assertions.assertEquals(FolderOperationType.CREATE_ACCOUNT, newOpLog.get(0).getOperationType());
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
        Assertions.assertEquals(new QuotasByAccount(Map.of()), newOpLog.get(0).getOldProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of()), newOpLog.get(0).getNewProvisions());
        Assertions.assertTrue(newOpLog.get(0).getOldAccounts().isEmpty());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().isPresent());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .containsKey(createdAccount.get().getId()));
        Assertions.assertEquals(provider.getId(), newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getProviderId().get());
        Assertions.assertEquals("test-id", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getOuterAccountIdInProvider().get());
        Assertions.assertEquals("test", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getOuterAccountKeyInProvider().get());
        Assertions.assertEquals(folder.getId(), newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getFolderId().get());
        Assertions.assertEquals("Test", newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getDisplayName().get());
        Assertions.assertFalse(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getDeleted().get());
        Assertions.assertTrue(newOpLog.get(0).getNewAccounts().get().getAccounts()
                .get(createdAccount.get().getId()).getLastReceivedVersion().isEmpty());
        Assertions.assertEquals(2L, newOpLog.get(0).getOrder());
        Assertions.assertEquals(operation.getOperationId(), newOpLog.get(0).getAccountsQuotasOperationsId().get());
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

    private AccountsQuotasOperationsModel operationModel(String providerId, String accountsSpaceId, String folderId) {
        return AccountsQuotasOperationsModel.builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setOperationId(UUID.randomUUID().toString())
                .setLastRequestId(UUID.randomUUID().toString())
                .setCreateDateTime(Instant.now().minus(Duration.ofMinutes(2)))
                .setOperationSource(OperationSource.USER)
                .setOperationType(AccountsQuotasOperationsModel.OperationType.CREATE_ACCOUNT)
                .setAuthorUserId("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                .setAuthorUserUid("1120000000000001")
                .setProviderId(providerId)
                .setAccountsSpaceId(accountsSpaceId)
                .setUpdateDateTime(null)
                .setRequestStatus(AccountsQuotasOperationsModel.RequestStatus.WAITING)
                .setErrorMessage(null)
                .setRequestedChanges(OperationChangesModel.builder()
                        .accountCreateParams(new OperationChangesModel
                                .AccountCreateParams("test", "Test", folderId,
                                    UUID.randomUUID().toString(), false, null))
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

}
