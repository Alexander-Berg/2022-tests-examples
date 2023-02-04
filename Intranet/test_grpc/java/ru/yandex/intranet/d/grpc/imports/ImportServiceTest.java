package ru.yandex.intranet.d.grpc.imports;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.yandex.ydb.table.transaction.TransactionMode;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.backend.service.proto.Account;
import ru.yandex.intranet.d.backend.service.proto.AccountSegmentKey;
import ru.yandex.intranet.d.backend.service.proto.AccountSegments;
import ru.yandex.intranet.d.backend.service.proto.AccountsSpaceIdentity;
import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;
import ru.yandex.intranet.d.backend.service.proto.FieldError;
import ru.yandex.intranet.d.backend.service.proto.FolderQuotas;
import ru.yandex.intranet.d.backend.service.proto.ImportAmount;
import ru.yandex.intranet.d.backend.service.proto.ImportQuotasRequest;
import ru.yandex.intranet.d.backend.service.proto.ImportQuotasResponse;
import ru.yandex.intranet.d.backend.service.proto.Provision;
import ru.yandex.intranet.d.backend.service.proto.QuotasImportsServiceGrpc;
import ru.yandex.intranet.d.backend.service.proto.ResourceQuota;
import ru.yandex.intranet.d.backend.service.proto.TargetFolder;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsDao;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao;
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao;
import ru.yandex.intranet.d.dao.quotas.QuotasDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
import ru.yandex.intranet.d.model.WithTenant;
import ru.yandex.intranet.d.model.accounts.AccountModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel;
import ru.yandex.intranet.d.model.folders.AccountHistoryModel;
import ru.yandex.intranet.d.model.folders.AccountsHistoryModel;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
import ru.yandex.intranet.d.model.folders.FolderOperationType;
import ru.yandex.intranet.d.model.folders.ProvisionHistoryModel;
import ru.yandex.intranet.d.model.folders.ProvisionsByResource;
import ru.yandex.intranet.d.model.folders.QuotasByAccount;
import ru.yandex.intranet.d.model.folders.QuotasByResource;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.utils.ErrorsHelper;
import ru.yandex.intranet.d.web.model.SortOrderDto;

/**
 * Import GRPC API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ImportServiceTest {

    @SuppressWarnings("unused")
    @GrpcClient("inProcess")
    private QuotasImportsServiceGrpc.QuotasImportsServiceBlockingStub importService;
    @Autowired
    private AccountsDao accountsDao;
    @Autowired
    private QuotasDao quotasDao;
    @Autowired
    private AccountsQuotasDao accountsQuotasDao;
    @Autowired
    private FolderOperationLogDao folderOperationLogDao;
    @Autowired
    private YdbTableClient tableClient;

    @Test
    public void simpleImportTest() {
        ImportQuotasRequest providerRequest = newImportQuotasRequest(List.of(
                AccountSegmentKey.newBuilder()
                        .setSegmentationKey("location")
                        .setSegmentKey("sas")
                        .build(),
                AccountSegmentKey.newBuilder()
                        .setSegmentationKey("segment")
                        .setSegmentKey("default")
                        .build()
        ));
        try {
            ImportQuotasResponse result = importService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .importQuotas(providerRequest);
            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.getImportFailuresList().isEmpty());
            Assertions.assertEquals(1, result.getSuccessfullyImportedList().size());
            Assertions.assertTrue(result.getSuccessfullyImportedList().get(0).hasTargetFolder());
            Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1",
                    result.getSuccessfullyImportedList().get(0).getTargetFolder().getFolderId());
            Assertions.assertFalse(result.getSuccessfullyImportedList().get(0).hasTargetService());
            Optional<AccountModel> newAccount = tableClient.usingSessionMonoRetryable(session ->
                    session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                            accountsDao.getAllByExternalId(ts, new WithTenant<>(Tenants.DEFAULT_TENANT_ID,
                                    new AccountModel.ExternalId("96e779cf-7d3f-4e74-ba41-c2acc7f04235", "1",
                                            "978bd75a-cf67-44ac-b944-e8ca949bdf7e")))))
                    .block();
            Assertions.assertNotNull(newAccount);
            Assertions.assertTrue(newAccount.isPresent());
            Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, newAccount.get().getTenantId());
            Assertions.assertEquals(0L, newAccount.get().getVersion());
            Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", newAccount.get().getProviderId());
            Assertions.assertEquals("1", newAccount.get().getOuterAccountIdInProvider());
            Assertions.assertTrue(newAccount.get().getOuterAccountKeyInProvider().isPresent());
            Assertions.assertEquals("test", newAccount.get().getOuterAccountKeyInProvider().get());
            Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", newAccount.get().getFolderId());
            Assertions.assertTrue(newAccount.get().getDisplayName().isPresent());
            Assertions.assertEquals("Test", newAccount.get().getDisplayName().get());
            Assertions.assertFalse(newAccount.get().isDeleted());
            List<QuotaModel> newQuotas = tableClient.usingSessionMonoRetryable(session ->
                    session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                            quotasDao.getByFolders(ts, List.of("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1"),
                                    Tenants.DEFAULT_TENANT_ID)))
                    .block();
            Assertions.assertNotNull(newQuotas);
            Assertions.assertEquals(1, newQuotas.size());
            Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", newQuotas.get(0).getFolderId());
            Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", newQuotas.get(0).getProviderId());
            Assertions.assertEquals("45e40bbb-e70c-4963-876c-f3e2b5db5403", newQuotas.get(0).getResourceId());
            Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, newQuotas.get(0).getTenantId());
            Assertions.assertEquals(100000L, newQuotas.get(0).getQuota());
            Assertions.assertEquals(50000L, newQuotas.get(0).getBalance());
            Optional<AccountsQuotasModel> accountQuota = tableClient.usingSessionMonoRetryable(session ->
                    session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                            accountsQuotasDao.getById(ts, new AccountsQuotasModel.Identity(newAccount.get().getId(),
                                    "45e40bbb-e70c-4963-876c-f3e2b5db5403"), Tenants.DEFAULT_TENANT_ID)))
                    .block();
            Assertions.assertNotNull(accountQuota);
            Assertions.assertTrue(accountQuota.isPresent());
            Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, accountQuota.get().getTenantId());
            Assertions.assertEquals(newAccount.get().getId(), accountQuota.get().getAccountId());
            Assertions.assertEquals("45e40bbb-e70c-4963-876c-f3e2b5db5403", accountQuota.get().getResourceId());
            Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", accountQuota.get().getFolderId());
            Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", accountQuota.get().getProviderId());
            Assertions.assertEquals(50000L, accountQuota.get().getProvidedQuota());
            Assertions.assertEquals(25000L, accountQuota.get().getAllocatedQuota());
            List<FolderOperationLogModel> log = tableClient.usingSessionMonoRetryable(session ->
                    session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts -> folderOperationLogDao
                            .getFirstPageByFolder(ts, Tenants.DEFAULT_TENANT_ID, "2e8f9b1c-7b3a-41ba-9067-2dee22b046a1",
                                    SortOrderDto.ASC, 100))).block();
            Assertions.assertNotNull(log);
            Assertions.assertEquals(1, log.size());
            Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, log.get(0).getTenantId());
            Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", log.get(0).getFolderId());
            Assertions.assertEquals(FolderOperationType.QUOTA_IMPORT, log.get(0).getOperationType());
            Assertions.assertTrue(log.get(0).getAuthorProviderId().isPresent());
            Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", log.get(0).getAuthorProviderId().get());
            Assertions.assertTrue(log.get(0).getOldFolderFields().isEmpty());
            Assertions.assertTrue(log.get(0).getNewFolderFields().isEmpty());
            Assertions.assertEquals(new QuotasByResource(Map.of("45e40bbb-e70c-4963-876c-f3e2b5db5403", 0L)),
                    log.get(0).getOldQuotas());
            Assertions.assertEquals(new QuotasByResource(Map.of("45e40bbb-e70c-4963-876c-f3e2b5db5403", 100000L)),
                    log.get(0).getNewQuotas());
            Assertions.assertEquals(new QuotasByResource(Map.of("45e40bbb-e70c-4963-876c-f3e2b5db5403", 0L)),
                    log.get(0).getOldBalance());
            Assertions.assertEquals(new QuotasByResource(Map.of("45e40bbb-e70c-4963-876c-f3e2b5db5403", 50000L)),
                    log.get(0).getNewBalance());
            Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                    new ProvisionsByResource(Map.of("45e40bbb-e70c-4963-876c-f3e2b5db5403",
                            new ProvisionHistoryModel(0L, null))))), log.get(0).getOldProvisions());
            Assertions.assertEquals(new QuotasByAccount(Map.of(newAccount.get().getId(),
                    new ProvisionsByResource(Map.of("45e40bbb-e70c-4963-876c-f3e2b5db5403",
                            new ProvisionHistoryModel(50000L, null))))), log.get(0).getNewProvisions());
            Assertions.assertEquals(0L, log.get(0).getOrder());
            Assertions.assertTrue(log.get(0).getOldAccounts().isEmpty());
            Assertions.assertEquals(Optional.of(new AccountsHistoryModel(Map.of(newAccount.get().getId(),
                    new AccountHistoryModel(0L, "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "1", "test",
                            "2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", "Test", false, null,
                            "978bd75a-cf67-44ac-b944-e8ca949bdf7e", null, null)))),
                    log.get(0).getNewAccounts());
            Assertions.assertTrue(log.get(0).getActuallyAppliedProvisions().isEmpty());
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(ErrorsHelper.extractErrorDetails(e).toString(), e);
        }
    }

    @Test
    public void testAccountsSpaceNotFoundError() {
        ImportQuotasRequest providerRequest = newImportQuotasRequest(List.of(
                AccountSegmentKey.newBuilder()
                        .setSegmentationKey("a")
                        .setSegmentKey("b")
                        .build()
        ));
        try {
            //noinspection ResultOfMethodCallIgnored
            importService.withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .importQuotas(providerRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertEquals(1, details.get().getFieldErrorsCount());
            FieldError fieldError = details.get().getFieldErrors(0);
            Assertions.assertEquals("quotas.0.accounts.0.accountSpaceIdentity", fieldError.getKey());
            Assertions.assertEquals(1, fieldError.getErrorsCount());
            Assertions.assertEquals("Accounts space not found.", fieldError.getErrors(0));
            return;
        }
        Assertions.fail();
    }

    @Test
    public void testAccountsSpaceNotMatchError() {
        ImportQuotasRequest providerRequest = newImportQuotasRequest(List.of(
                AccountSegmentKey.newBuilder()
                        .setSegmentationKey("location")
                        .setSegmentKey("vla")
                        .build(),
                AccountSegmentKey.newBuilder()
                        .setSegmentationKey("segment")
                        .setSegmentKey("default")
                        .build()
        ));
        try {
            //noinspection ResultOfMethodCallIgnored
            importService.withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .importQuotas(providerRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertEquals(1, details.get().getFieldErrorsCount());
            FieldError fieldError = details.get().getFieldErrors(0);
            Assertions.assertEquals("quotas.0.accounts.0.provisions.0", fieldError.getKey());
            Assertions.assertEquals(1, fieldError.getErrorsCount());
            Assertions.assertEquals("Provided resource account space does not match account.",
                    fieldError.getErrors(0));
            return;
        }
        Assertions.fail();
    }

    @Test
    public void testAmbiguousTvmIds() {
        try {
            ImportQuotasRequest providerRequest = ImportQuotasRequest.newBuilder().addAllQuotas(List.of(
                    FolderQuotas.newBuilder()
                            .setTargetFolder(TargetFolder.newBuilder()
                                    .setFolderId(TestFolders.TEST_IMPORT_FOLDER_ID).build())
                            .addAllResourceQuotas(List.of(
                                    ResourceQuota.newBuilder()
                                            .setResourceId(TestResources.CLAUD1_RAM)
                                            .setProviderId(TestProviders.CLAUD1_ID)
                                            .setQuota(ImportAmount.newBuilder()
                                                    .setValue(100L)
                                                    .setUnitKey("bytes")
                                                    .build())
                                            .setBalance(ImportAmount.newBuilder()
                                                    .setValue(100L)
                                                    .setUnitKey("bytes")
                                                    .build())
                                            .build()
                            )).build()
            )).build();
            ImportQuotasResponse result = importService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.CLAUD_TVM_ID))
                    .importQuotas(providerRequest);

            Assertions.assertEquals(
                    TestFolders.TEST_IMPORT_FOLDER_ID,
                    result.getSuccessfullyImported(0).getTargetFolder().getFolderId()
            );

            List<FolderOperationLogModel> log = tableClient.usingSessionMonoRetryable(session ->
                    session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts -> folderOperationLogDao
                            .getFirstPageByFolder(ts, Tenants.DEFAULT_TENANT_ID, TestFolders.TEST_IMPORT_FOLDER_ID,
                                    SortOrderDto.ASC, 100))).block();
            Assertions.assertNotNull(log);
            Assertions.assertEquals(FolderOperationType.QUOTA_IMPORT, log.get(0).getOperationType());
            Assertions.assertTrue(log.get(0).getAuthorProviderId().isPresent());
            Assertions.assertEquals(TestProviders.CLAUD1_ID, log.get(0).getAuthorProviderId().get());
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(ErrorsHelper.extractErrorDetails(e).toString(), e);
        }
    }

    private ImportQuotasRequest newImportQuotasRequest(List<AccountSegmentKey> segments) {
        return ImportQuotasRequest.newBuilder()
                .addAllQuotas(List.of(
                        FolderQuotas.newBuilder()
                                .setTargetFolder(TargetFolder.newBuilder()
                                        .setFolderId(TestFolders.TEST_IMPORT_FOLDER_ID).build())
                                .addAllResourceQuotas(List.of(
                                        ResourceQuota.newBuilder()
                                                .setResourceId(TestResources.YP_CPU_SAS)
                                                .setProviderId(TestProviders.YP_ID)
                                                .setQuota(ImportAmount.newBuilder()
                                                        .setValue(100L)
                                                        .setUnitKey("cores")
                                                        .build())
                                                .setBalance(ImportAmount.newBuilder()
                                                        .setValue(50L)
                                                        .setUnitKey("cores")
                                                        .build())
                                                .build()
                                ))
                                .addAllAccounts(List.of(
                                        Account.newBuilder()
                                                .setAccountId("1")
                                                .setKey("test")
                                                .setDisplayName("Test")
                                                .setDeleted(false)
                                                .setProviderId(TestProviders.YP_ID)
                                                .setAccountsSpace(AccountsSpaceIdentity.newBuilder()
                                                        .setSegments(AccountSegments.newBuilder()
                                                                .addAllKey(segments).build()).build())
                                                .addAllProvisions(List.of(
                                                        Provision.newBuilder()
                                                                .setResourceId(TestResources.YP_CPU_SAS)
                                                                .setProvided(ImportAmount.newBuilder()
                                                                        .setValue(50L)
                                                                        .setUnitKey("cores")
                                                                        .build())
                                                                .setAllocated(ImportAmount.newBuilder()
                                                                        .setValue(25L)
                                                                        .setUnitKey("cores")
                                                                        .build())
                                                                .build()
                                                )).build()
                                )).build()
                        )
                ).build();
    }

    @Test
    public void importToFolderInProviderWithDisplayNameUnsupportedTest() {
        ImportQuotasRequest providerRequest = ImportQuotasRequest.newBuilder()
                .addAllQuotas(List.of(
                        FolderQuotas.newBuilder()
                                .setTargetFolder(TargetFolder.newBuilder()
                                        .setFolderId(TestFolders.TEST_FOLDER_1_ID).build())
                                .addAllAccounts(List.of(
                                        Account.newBuilder()
                                                .setAccountId(UUID.randomUUID().toString())
                                                .setKey("test-account")
                                                .setDeleted(false)
                                                .setProviderId(TestProviders.CLAUD2_ID)
                                                .setAccountsSpace(AccountsSpaceIdentity.newBuilder()
                                                        .setAccountsSpaceId("b74ecfec-842f-4669-955a-3a7112fd8387")
                                                        .build()).build()
                                )).build()
                        )
                ).build();
        try {
            ImportQuotasResponse result = importService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.CLAUD_TVM_ID))
                    .importQuotas(providerRequest);
            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.getImportFailuresList().isEmpty());
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(ErrorsHelper.extractErrorDetails(e).toString(), e);
        }
    }
}
