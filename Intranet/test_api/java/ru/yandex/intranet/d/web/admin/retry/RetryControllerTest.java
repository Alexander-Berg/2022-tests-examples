package ru.yandex.intranet.d.web.admin.retry;

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
import ru.yandex.intranet.d.backend.service.provider_proto.ListAccountsByFolderResponse;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsDao;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasOperationsDao;
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
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel;
import ru.yandex.intranet.d.model.accounts.OperationChangesModel;
import ru.yandex.intranet.d.model.accounts.OperationErrorCollections;
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
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.model.resources.ResourceModel;
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel;
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel;
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel;
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel;
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel;
import ru.yandex.intranet.d.util.result.ErrorCollection;
import ru.yandex.intranet.d.util.result.TypedError;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.controllers.admin.retry.model.OperationDto;
import ru.yandex.intranet.d.web.controllers.admin.retry.model.OperationsDto;
import ru.yandex.intranet.d.web.model.SortOrderDto;

/**
 * Retry admin controller test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class RetryControllerTest {

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
    private WebTestClient webClient;

    @Test
    @SuppressWarnings("MethodLength")
    public void abortCreateAccountTest() {
        ProviderModel provider = providerModel(GRPC_URI, null, false, false, true, true, false, false, false, false);
        FolderModel folder = folderModel(1L);
        AccountsQuotasOperationsModel operation = createAccountOperationModel(provider.getId(), null, folder.getId());
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
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/retry/{id}/_abort?comment={comment}", operation.getOperationId(), "Test")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();
        Optional<AccountsQuotasOperationsModel> updatedOperation = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        accountsQuotasOperationsDao.getById(txSession, operation.getOperationId(),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(updatedOperation);
        Assertions.assertTrue(updatedOperation.isPresent());
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.ERROR,
                updatedOperation.get().getRequestStatus().get());
        Assertions.assertEquals("Test",
                updatedOperation.get().getErrorMessage().get());
        List<OperationInProgressModel> inProgressAfter = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        operationsInProgressDao.getAllByTenant(txSession, Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(inProgressAfter);
        Assertions.assertTrue(inProgressAfter.isEmpty());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void retryCreateAccountTest() {
        ProviderModel provider = providerModel(GRPC_URI, null, false, false, true, true, false, false, false, false);
        FolderModel folder = folderModel(1L);
        AccountsQuotasOperationsModel operation = createAccountOperationModel(provider.getId(), null, folder.getId());
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
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/retry/_execute")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();
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
    public void abortUpdateProvisionTest() {
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
        QuotaModel quota = quotaModel(provider.getId(), resource.getId(), folder.getId(), 300L, 200L, 40L);
        AccountModel account = accountModel(provider.getId(), null, "test-id", "test",
                folder.getId(), "Test", null, null);
        AccountsQuotasModel provision = accountQuotaModel(provider.getId(), resource.getId(), folder.getId(),
                account.getId(), 60L, 30L, null, null);
        AccountsQuotasOperationsModel operation = updateProvisionOperationModel(provider.getId(), null, account.getId(),
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
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/retry/{id}/_abort?comment={comment}", operation.getOperationId(), "Test")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();
        Optional<AccountsQuotasOperationsModel> updatedOperation = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        accountsQuotasOperationsDao.getById(txSession, operation.getOperationId(),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(updatedOperation);
        Assertions.assertTrue(updatedOperation.isPresent());
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.ERROR,
                updatedOperation.get().getRequestStatus().get());
        Assertions.assertEquals("Test",
                updatedOperation.get().getErrorMessage().get());
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
    }

    @Test
    public void getOperationsInProgressTest() {
        ProviderModel provider = providerModel(GRPC_URI, null, false, false, true, true, false, false, false, false);
        FolderModel folder = folderModel(1L);
        AccountsQuotasOperationsModel operation = createAccountOperationModel(provider.getId(), null, folder.getId());
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
        OperationsDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/admin/retry/_inProgress", operation.getOperationId(), "Test")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationsDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertEquals(1, result.getOperations().size());
    }

    @Test
    public void getOperationsWithErrorInProgressTest() {
        ProviderModel provider = providerModel(GRPC_URI, null, false, false, true, true, false, false, false, false);
        FolderModel folder = folderModel(1L);

        AccountsQuotasOperationsModel operationWithError = AccountsQuotasOperationsModel.builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setOperationId(UUID.randomUUID().toString())
                .setLastRequestId(null)
                .setCreateDateTime(Instant.now().minus(Duration.ofMinutes(2)))
                .setOperationSource(OperationSource.USER)
                .setOperationType(AccountsQuotasOperationsModel.OperationType.CREATE_ACCOUNT)
                .setAuthorUserId("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                .setAuthorUserUid("1120000000000001")
                .setProviderId(provider.getId())
                .setAccountsSpaceId(null)
                .setUpdateDateTime(null)
                .setRequestStatus(AccountsQuotasOperationsModel.RequestStatus.WAITING)
                .setErrorMessage("Test error")
                .setFullErrorMessage(null)
                .setRequestedChanges(OperationChangesModel.builder()
                        .accountCreateParams(new OperationChangesModel
                                .AccountCreateParams("test", "Test",
                                folder.getId(), UUID.randomUUID().toString(), false, null))
                        .build())
                .setOrders(OperationOrdersModel.builder()
                        .submitOrder(1L)
                        .build())
                .build();
        OperationInProgressModel inProgressWithError =
                inProgressModel(operationWithError.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                        .upsertOneRetryable(txSession, operationWithError)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> operationsInProgressDao
                        .upsertOneRetryable(txSession, inProgressWithError)))
                .block();

        AccountsQuotasOperationsModel operationWithFullError = AccountsQuotasOperationsModel.builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setOperationId(UUID.randomUUID().toString())
                .setLastRequestId(null)
                .setCreateDateTime(Instant.now().minus(Duration.ofMinutes(2)))
                .setOperationSource(OperationSource.USER)
                .setOperationType(AccountsQuotasOperationsModel.OperationType.CREATE_ACCOUNT)
                .setAuthorUserId("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                .setAuthorUserUid("1120000000000001")
                .setProviderId(provider.getId())
                .setAccountsSpaceId(null)
                .setUpdateDateTime(null)
                .setRequestStatus(AccountsQuotasOperationsModel.RequestStatus.WAITING)
                .setErrorMessage(null)
                .setFullErrorMessage(OperationErrorCollections.builder()
                        .addErrorCollection(
                                Locales.ENGLISH,
                                ErrorCollection.builder().addError(TypedError.badRequest("Test error")).build())
                        .addErrorCollection(
                                Locales.RUSSIAN,
                                ErrorCollection.builder().addError(TypedError.badRequest("Тестовая ошибка")).build())
                        .build())
                .setRequestedChanges(OperationChangesModel.builder()
                        .accountCreateParams(new OperationChangesModel
                                .AccountCreateParams("test", "Test",
                                folder.getId(), UUID.randomUUID().toString(), false, null))
                        .build())
                .setOrders(OperationOrdersModel.builder()
                        .submitOrder(1L)
                        .build())
                .build();
        OperationInProgressModel inProgressWithFullError =
                inProgressModel(operationWithFullError.getOperationId(), folder.getId());
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasOperationsDao
                        .upsertOneRetryable(txSession, operationWithFullError)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> operationsInProgressDao
                        .upsertOneRetryable(txSession, inProgressWithFullError)))
                .block();

        OperationsDto resultWithError = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/admin/retry/_inProgress", operationWithError.getOperationId(), "Test")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationsDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(resultWithError.getOperations());
        Assertions.assertEquals(2, resultWithError.getOperations().size());

        OperationDto operationWithErrorDto = resultWithError.getOperations().get(0);
        Assertions.assertTrue(operationWithErrorDto.getErrorMessage().isPresent());
        Assertions.assertEquals("Test error", operationWithErrorDto.getErrorMessage().get());

        OperationDto operationWithFullErrorDto = resultWithError.getOperations().get(1);
        Assertions.assertTrue(operationWithFullErrorDto.getErrorMessage().isPresent());
        Assertions.assertEquals("Test error", operationWithFullErrorDto.getErrorMessage().get());
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

    private AccountsQuotasOperationsModel createAccountOperationModel(String providerId,
                                                                      String accountsSpaceId,
                                                                      String folderId) {
        return AccountsQuotasOperationsModel.builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setOperationId(UUID.randomUUID().toString())
                .setLastRequestId(null)
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

    private AccountsQuotasOperationsModel updateProvisionOperationModel(
            String providerId,
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
