package ru.yandex.intranet.d.grpc.accounts;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.google.protobuf.Timestamp;
import com.yandex.ydb.table.transaction.TransactionMode;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.backend.service.proto.AccountKey;
import ru.yandex.intranet.d.backend.service.proto.AccountName;
import ru.yandex.intranet.d.backend.service.proto.AccountOperationStatus;
import ru.yandex.intranet.d.backend.service.proto.AccountsServiceGrpc;
import ru.yandex.intranet.d.backend.service.proto.CreateAccountRequest;
import ru.yandex.intranet.d.backend.service.proto.CreateAccountResponse;
import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;
import ru.yandex.intranet.d.backend.service.proto.GetOperationStateRequest;
import ru.yandex.intranet.d.backend.service.proto.OperationState;
import ru.yandex.intranet.d.backend.service.proto.OperationStatus;
import ru.yandex.intranet.d.backend.service.proto.OperationsServiceGrpc;
import ru.yandex.intranet.d.backend.service.proto.ProviderAccountsSpace;
import ru.yandex.intranet.d.backend.service.provider_proto.Account;
import ru.yandex.intranet.d.backend.service.provider_proto.AccountsSpaceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundAccountsSpaceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.CurrentVersion;
import ru.yandex.intranet.d.backend.service.provider_proto.LastUpdate;
import ru.yandex.intranet.d.backend.service.provider_proto.ListAccountsByFolderResponse;
import ru.yandex.intranet.d.backend.service.provider_proto.PassportUID;
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceSegmentKey;
import ru.yandex.intranet.d.backend.service.provider_proto.StaffLogin;
import ru.yandex.intranet.d.backend.service.provider_proto.UserID;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsDao;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasOperationsDao;
import ru.yandex.intranet.d.dao.accounts.OperationsInProgressDao;
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse;
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel;
import ru.yandex.intranet.d.model.accounts.OperationInProgressModel;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
import ru.yandex.intranet.d.model.folders.FolderOperationType;
import ru.yandex.intranet.d.utils.ErrorsHelper;
import ru.yandex.intranet.d.web.model.SortOrderDto;

import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_1;

/**
 * Accounts modification GRPC API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class AccountsModificationServiceTest {

    @GrpcClient("inProcess")
    private AccountsServiceGrpc.AccountsServiceBlockingStub accountsService;
    @GrpcClient("inProcess")
    private OperationsServiceGrpc.OperationsServiceBlockingStub operationsService;
    @Autowired
    private StubProviderService stubProviderService;
    @Autowired
    private AccountsDao accountsDao;
    @Autowired
    private YdbTableClient tableClient;
    @Autowired
    private OperationsInProgressDao operationsInProgressDao;
    @Autowired
    private AccountsQuotasOperationsDao quotasOperationsDao;
    @Autowired
    private FolderOperationLogDao folderOperationLogDao;

    @Test
    public void testCreateAccountSuccess() {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse.success(Account.newBuilder()
                .setAccountId(UUID.randomUUID().toString())
                .setDeleted(false)
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setDisplayName("outerDisplayName")
                .setKey("outerKey")
                .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                        .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey("location")
                                        .setResourceSegmentKey("vla")
                                        .build())
                                .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey("segment")
                                        .setResourceSegmentKey("default")
                                        .build())
                                .build())
                        .build())
                .setLastUpdate(LastUpdate.newBuilder()
                        .setAuthor(UserID.newBuilder()
                                .setPassportUid(PassportUID.newBuilder()
                                        .setPassportUid(TestUsers.USER_1_UID).build())
                                .setStaffLogin(StaffLogin.newBuilder()
                                        .setStaffLogin(TestUsers.USER_1_LOGIN).build())
                                .build())
                        .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
                        .build()).build())));
        CreateAccountRequest request = CreateAccountRequest.newBuilder()
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setProviderId(TestProviders.YP_ID)
                .setExternalKey(AccountKey.newBuilder().setValue("outerKey").build())
                .setDisplayName(AccountName.newBuilder().setValue("outerDisplayName").build())
                .setAccountsSpace(ProviderAccountsSpace.newBuilder().setId(TEST_ACCOUNT_SPACE_1.getId()).build())
                .setFreeTier(false)
                .build();
        CreateAccountResponse response = accountsService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .createAccount(request);
        Assertions.assertTrue(response.hasAccount());
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, response.getAccount().getFolderId());
        Assertions.assertEquals(TestProviders.YP_ID, response.getAccount().getProviderId());
        Assertions.assertEquals("outerKey", response.getAccount().getExternalKey().getValue());
        Assertions.assertEquals("outerDisplayName", response.getAccount().getDisplayName().getValue());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(), response.getAccount().getAccountsSpace().getId());
        Assertions.assertNotEquals("", response.getAccount().getId());
        Assertions.assertNotEquals("", response.getAccount().getExternalId());
        Assertions.assertNotEquals("", response.getOperationId());
        Assertions.assertFalse(response.getAccount().getFreeTier());
        Assertions.assertEquals(AccountOperationStatus.ACCOUNT_OPERATION_SUCCESS, response.getOperationStatus());
        GetOperationStateRequest operationStateRequest = GetOperationStateRequest.newBuilder()
                .setOperationId(response.getOperationId())
                .build();
        OperationState operationState = operationsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getOperationState(operationStateRequest);
        Assertions.assertNotNull(operationState);
        Assertions.assertEquals(response.getOperationId(), operationState.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationState.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(), operationState.getAccountsSpaceId().getId());
        Assertions.assertEquals(OperationStatus.SUCCESS, operationState.getStatus());
        Assertions.assertFalse(operationState.hasFailure());
    }

    @Test
    public void testCreateAccountNonRetryableFailure() {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Test failure")))));
        CreateAccountRequest request = CreateAccountRequest.newBuilder()
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setProviderId(TestProviders.YP_ID)
                .setExternalKey(AccountKey.newBuilder().setValue("outerKey").build())
                .setDisplayName(AccountName.newBuilder().setValue("outerDisplayName").build())
                .setAccountsSpace(ProviderAccountsSpace.newBuilder().setId(TEST_ACCOUNT_SPACE_1.getId()).build())
                .setFreeTier(false)
                .build();
        try {
            accountsService
                    .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                    .createAccount(request);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            Assertions.assertEquals("Error on account creation, status code: INVALID_ARGUMENT Test failure. " +
                    "Account may be created on next provider sync.", details.get().getErrorsList().get(0));
            FolderOperationLogModel folderLog = tableClient.usingSessionMonoRetryable(session -> folderOperationLogDao
                            .getFirstPageByFolder(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                    Tenants.DEFAULT_TENANT_ID, TestFolders.TEST_FOLDER_1_ID, SortOrderDto.ASC, 100))
                    .block().stream()
                    .filter(f -> f.getOperationType() == FolderOperationType.CREATE_ACCOUNT)
                    .sorted(Comparator.comparing(FolderOperationLogModel::getOrder).reversed()).findFirst().get();
            String opId = folderLog.getAccountsQuotasOperationsId().orElse(null);
            Assertions.assertNotNull(opId);
            AccountsQuotasOperationsModel op = tableClient.usingSessionMonoRetryable(session ->
                    quotasOperationsDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                            opId, Tenants.DEFAULT_TENANT_ID)).block().get();
            AccountsQuotasOperationsModel.RequestStatus opStatus = op.getRequestStatus().get();
            Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.ERROR, opStatus);
            Assertions.assertTrue(op.getErrorMessage().isPresent());
            Optional<OperationInProgressModel> opInP = tableClient.usingSessionMonoRetryable(session ->
                    operationsInProgressDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                            new OperationInProgressModel.Key(opId, TestFolders.TEST_FOLDER_1_ID),
                            Tenants.DEFAULT_TENANT_ID)).block();
            Assertions.assertTrue(opInP.isEmpty());
            return;
        }
        Assertions.fail();
    }

    @Test
    public void testCreateAccountConflictNoMatch() {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.ALREADY_EXISTS.withDescription("Test failure")))));
        stubProviderService.setListAccountsByFolderResponses(List.of(GrpcResponse.success(ListAccountsByFolderResponse
                .newBuilder().build())));
        CreateAccountRequest request = CreateAccountRequest.newBuilder()
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setProviderId(TestProviders.YP_ID)
                .setExternalKey(AccountKey.newBuilder().setValue("outerKey").build())
                .setDisplayName(AccountName.newBuilder().setValue("outerDisplayName").build())
                .setAccountsSpace(ProviderAccountsSpace.newBuilder().setId(TEST_ACCOUNT_SPACE_1.getId()).build())
                .setFreeTier(false)
                .build();
        try {
            accountsService
                    .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                    .createAccount(request);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.ALREADY_EXISTS, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            Assertions.assertEquals("Error on account creation, status code: ALREADY_EXISTS Test failure. " +
                    "Account may be created on next provider sync.", details.get().getErrorsList().get(0));
            FolderOperationLogModel folderLog = tableClient.usingSessionMonoRetryable(session -> folderOperationLogDao
                            .getFirstPageByFolder(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                    Tenants.DEFAULT_TENANT_ID, TestFolders.TEST_FOLDER_1_ID, SortOrderDto.ASC, 100))
                    .block().stream()
                    .filter(f -> f.getOperationType() == FolderOperationType.CREATE_ACCOUNT)
                    .sorted(Comparator.comparing(FolderOperationLogModel::getOrder).reversed()).findFirst().get();
            String opId = folderLog.getAccountsQuotasOperationsId().orElse(null);
            Assertions.assertNotNull(opId);
            AccountsQuotasOperationsModel op = tableClient.usingSessionMonoRetryable(session ->
                    quotasOperationsDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                            opId, Tenants.DEFAULT_TENANT_ID)).block().get();
            AccountsQuotasOperationsModel.RequestStatus opStatus = op.getRequestStatus().get();
            Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.ERROR, opStatus);
            Assertions.assertTrue(op.getErrorMessage().isPresent());
            Optional<OperationInProgressModel> opInP = tableClient.usingSessionMonoRetryable(session ->
                    operationsInProgressDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                            new OperationInProgressModel.Key(opId, TestFolders.TEST_FOLDER_1_ID),
                            Tenants.DEFAULT_TENANT_ID)).block();
            Assertions.assertTrue(opInP.isEmpty());
            return;
        }
        Assertions.fail();
    }

    @Test
    public void testCreateAccountConflictHasMatch() {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.ALREADY_EXISTS.withDescription("Test failure")))));
        stubProviderService.setListAccountsByFolderResponses(List.of(GrpcResponse.success(ListAccountsByFolderResponse
                .newBuilder()
                .addAccounts(Account.newBuilder()
                        .setAccountId(UUID.randomUUID().toString())
                        .setDeleted(false)
                        .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                        .setDisplayName("outerDisplayName")
                        .setKey("outerKey")
                        .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
                        .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                                .setResourceSegmentationKey("location")
                                                .setResourceSegmentKey("vla")
                                                .build())
                                        .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                                .setResourceSegmentationKey("segment")
                                                .setResourceSegmentKey("default")
                                                .build())
                                        .build())
                                .build())
                        .setLastUpdate(LastUpdate.newBuilder()
                                .setAuthor(UserID.newBuilder()
                                        .setPassportUid(PassportUID.newBuilder()
                                                .setPassportUid(TestUsers.USER_1_UID).build())
                                        .setStaffLogin(StaffLogin.newBuilder()
                                                .setStaffLogin(TestUsers.USER_1_LOGIN).build())
                                        .build())
                                .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
                                .build()).build())
                .build())));
        CreateAccountRequest request = CreateAccountRequest.newBuilder()
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setProviderId(TestProviders.YP_ID)
                .setExternalKey(AccountKey.newBuilder().setValue("outerKey").build())
                .setDisplayName(AccountName.newBuilder().setValue("outerDisplayName").build())
                .setAccountsSpace(ProviderAccountsSpace.newBuilder().setId(TEST_ACCOUNT_SPACE_1.getId()).build())
                .setFreeTier(false)
                .build();
        CreateAccountResponse response = accountsService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .createAccount(request);
        Assertions.assertTrue(response.hasAccount());
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, response.getAccount().getFolderId());
        Assertions.assertEquals(TestProviders.YP_ID, response.getAccount().getProviderId());
        Assertions.assertEquals("outerKey", response.getAccount().getExternalKey().getValue());
        Assertions.assertEquals("outerDisplayName", response.getAccount().getDisplayName().getValue());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(), response.getAccount().getAccountsSpace().getId());
        Assertions.assertNotEquals("", response.getAccount().getId());
        Assertions.assertNotEquals("", response.getAccount().getExternalId());
        Assertions.assertNotEquals("", response.getOperationId());
        Assertions.assertFalse(response.getAccount().getFreeTier());
        Assertions.assertEquals(AccountOperationStatus.ACCOUNT_OPERATION_SUCCESS, response.getOperationStatus());
        GetOperationStateRequest operationStateRequest = GetOperationStateRequest.newBuilder()
                .setOperationId(response.getOperationId())
                .build();
        OperationState operationState = operationsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getOperationState(operationStateRequest);
        Assertions.assertNotNull(operationState);
        Assertions.assertEquals(response.getOperationId(), operationState.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationState.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(), operationState.getAccountsSpaceId().getId());
        Assertions.assertEquals(OperationStatus.SUCCESS, operationState.getStatus());
        Assertions.assertFalse(operationState.hasFailure());
    }

    @Test
    public void testCreateAccountRetryableFailureSuccessfulInlineRetry() {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Test failure"))),
                GrpcResponse.success(Account.newBuilder()
                .setAccountId(UUID.randomUUID().toString())
                .setDeleted(false)
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setDisplayName("outerDisplayName")
                .setKey("outerKey")
                .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                        .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey("location")
                                        .setResourceSegmentKey("vla")
                                        .build())
                                .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey("segment")
                                        .setResourceSegmentKey("default")
                                        .build())
                                .build())
                        .build())
                .setLastUpdate(LastUpdate.newBuilder()
                        .setAuthor(UserID.newBuilder()
                                .setPassportUid(PassportUID.newBuilder()
                                        .setPassportUid(TestUsers.USER_1_UID).build())
                                .setStaffLogin(StaffLogin.newBuilder()
                                        .setStaffLogin(TestUsers.USER_1_LOGIN).build())
                                .build())
                        .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
                        .build()).build())));
        CreateAccountRequest request = CreateAccountRequest.newBuilder()
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setProviderId(TestProviders.YP_ID)
                .setExternalKey(AccountKey.newBuilder().setValue("outerKey").build())
                .setDisplayName(AccountName.newBuilder().setValue("outerDisplayName").build())
                .setAccountsSpace(ProviderAccountsSpace.newBuilder().setId(TEST_ACCOUNT_SPACE_1.getId()).build())
                .setFreeTier(false)
                .build();
        CreateAccountResponse response = accountsService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .createAccount(request);
        Assertions.assertTrue(response.hasAccount());
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, response.getAccount().getFolderId());
        Assertions.assertEquals(TestProviders.YP_ID, response.getAccount().getProviderId());
        Assertions.assertEquals("outerKey", response.getAccount().getExternalKey().getValue());
        Assertions.assertEquals("outerDisplayName", response.getAccount().getDisplayName().getValue());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(), response.getAccount().getAccountsSpace().getId());
        Assertions.assertNotEquals("", response.getAccount().getId());
        Assertions.assertNotEquals("", response.getAccount().getExternalId());
        Assertions.assertNotEquals("", response.getOperationId());
        Assertions.assertFalse(response.getAccount().getFreeTier());
        Assertions.assertEquals(AccountOperationStatus.ACCOUNT_OPERATION_SUCCESS, response.getOperationStatus());
        GetOperationStateRequest operationStateRequest = GetOperationStateRequest.newBuilder()
                .setOperationId(response.getOperationId())
                .build();
        OperationState operationState = operationsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getOperationState(operationStateRequest);
        Assertions.assertNotNull(operationState);
        Assertions.assertEquals(response.getOperationId(), operationState.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationState.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(), operationState.getAccountsSpaceId().getId());
        Assertions.assertEquals(OperationStatus.SUCCESS, operationState.getStatus());
        Assertions.assertFalse(operationState.hasFailure());
    }

    @Test
    public void testCreateAccountRetryableFailureFailedInlineRetry() {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse
                        .failure(new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Test failure")))));
        CreateAccountRequest request = CreateAccountRequest.newBuilder()
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setProviderId(TestProviders.YP_ID)
                .setExternalKey(AccountKey.newBuilder().setValue("outerKey").build())
                .setDisplayName(AccountName.newBuilder().setValue("outerDisplayName").build())
                .setAccountsSpace(ProviderAccountsSpace.newBuilder().setId(TEST_ACCOUNT_SPACE_1.getId()).build())
                .setFreeTier(false)
                .build();
        CreateAccountResponse response = accountsService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .createAccount(request);
        Assertions.assertFalse(response.hasAccount());
        Assertions.assertNotEquals("", response.getOperationId());
        Assertions.assertEquals(AccountOperationStatus.ACCOUNT_OPERATION_IN_PROGRESS, response.getOperationStatus());
        GetOperationStateRequest operationStateRequest = GetOperationStateRequest.newBuilder()
                .setOperationId(response.getOperationId())
                .build();
        OperationState operationState = operationsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getOperationState(operationStateRequest);
        Assertions.assertNotNull(operationState);
        Assertions.assertEquals(response.getOperationId(), operationState.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationState.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(), operationState.getAccountsSpaceId().getId());
        Assertions.assertEquals(OperationStatus.IN_PROGRESS, operationState.getStatus());
        Assertions.assertTrue(operationState.hasFailure());
        Assertions.assertEquals("Error on account creation, status code: UNAVAILABLE Test failure. " +
                "Account may be created on next provider sync.", operationState.getFailure().getErrorsList().get(0));
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testCreateAccountSuccessIdempotency() {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse.success(Account.newBuilder()
                .setAccountId(UUID.randomUUID().toString())
                .setDeleted(false)
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setDisplayName("outerDisplayName")
                .setKey("outerKey")
                .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                        .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey("location")
                                        .setResourceSegmentKey("vla")
                                        .build())
                                .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey("segment")
                                        .setResourceSegmentKey("default")
                                        .build())
                                .build())
                        .build())
                .setLastUpdate(LastUpdate.newBuilder()
                        .setAuthor(UserID.newBuilder()
                                .setPassportUid(PassportUID.newBuilder()
                                        .setPassportUid(TestUsers.USER_1_UID).build())
                                .setStaffLogin(StaffLogin.newBuilder()
                                        .setStaffLogin(TestUsers.USER_1_LOGIN).build())
                                .build())
                        .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
                        .build()).build())));
        CreateAccountRequest request = CreateAccountRequest.newBuilder()
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setProviderId(TestProviders.YP_ID)
                .setExternalKey(AccountKey.newBuilder().setValue("outerKey").build())
                .setDisplayName(AccountName.newBuilder().setValue("outerDisplayName").build())
                .setAccountsSpace(ProviderAccountsSpace.newBuilder().setId(TEST_ACCOUNT_SPACE_1.getId()).build())
                .setFreeTier(false)
                .build();
        String idempotencyKey = UUID.randomUUID().toString();
        Metadata extraHeaders = new Metadata();
        Metadata.Key<String> headerKey = Metadata.Key.of("Idempotency-Key", Metadata.ASCII_STRING_MARSHALLER);
        extraHeaders.put(headerKey, idempotencyKey);
        CreateAccountResponse response = accountsService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders))
                .createAccount(request);
        Assertions.assertTrue(response.hasAccount());
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, response.getAccount().getFolderId());
        Assertions.assertEquals(TestProviders.YP_ID, response.getAccount().getProviderId());
        Assertions.assertEquals("outerKey", response.getAccount().getExternalKey().getValue());
        Assertions.assertEquals("outerDisplayName", response.getAccount().getDisplayName().getValue());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(), response.getAccount().getAccountsSpace().getId());
        Assertions.assertNotEquals("", response.getAccount().getId());
        Assertions.assertNotEquals("", response.getAccount().getExternalId());
        Assertions.assertNotEquals("", response.getOperationId());
        Assertions.assertFalse(response.getAccount().getFreeTier());
        Assertions.assertEquals(AccountOperationStatus.ACCOUNT_OPERATION_SUCCESS, response.getOperationStatus());
        GetOperationStateRequest operationStateRequest = GetOperationStateRequest.newBuilder()
                .setOperationId(response.getOperationId())
                .build();
        OperationState operationState = operationsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getOperationState(operationStateRequest);
        Assertions.assertNotNull(operationState);
        Assertions.assertEquals(response.getOperationId(), operationState.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationState.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(), operationState.getAccountsSpaceId().getId());
        Assertions.assertEquals(OperationStatus.SUCCESS, operationState.getStatus());
        Assertions.assertFalse(operationState.hasFailure());
        CreateAccountResponse idempotencyResponse = accountsService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders))
                .createAccount(request);
        Assertions.assertTrue(idempotencyResponse.hasAccount());
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, idempotencyResponse.getAccount().getFolderId());
        Assertions.assertEquals(TestProviders.YP_ID, idempotencyResponse.getAccount().getProviderId());
        Assertions.assertEquals("outerKey", idempotencyResponse.getAccount().getExternalKey().getValue());
        Assertions.assertEquals("outerDisplayName",
                idempotencyResponse.getAccount().getDisplayName().getValue());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(),
                idempotencyResponse.getAccount().getAccountsSpace().getId());
        Assertions.assertNotEquals("", idempotencyResponse.getAccount().getId());
        Assertions.assertNotEquals("", idempotencyResponse.getAccount().getExternalId());
        Assertions.assertNotEquals("", idempotencyResponse.getOperationId());
        Assertions.assertFalse(idempotencyResponse.getAccount().getFreeTier());
        Assertions.assertEquals(AccountOperationStatus.ACCOUNT_OPERATION_SUCCESS,
                idempotencyResponse.getOperationStatus());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testCreateAccountRetryableFailureFailedInlineRetryIdempotency() {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Test failure")))));
        CreateAccountRequest request = CreateAccountRequest.newBuilder()
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setProviderId(TestProviders.YP_ID)
                .setExternalKey(AccountKey.newBuilder().setValue("outerKey").build())
                .setDisplayName(AccountName.newBuilder().setValue("outerDisplayName").build())
                .setAccountsSpace(ProviderAccountsSpace.newBuilder().setId(TEST_ACCOUNT_SPACE_1.getId()).build())
                .setFreeTier(false)
                .build();
        String idempotencyKey = UUID.randomUUID().toString();
        Metadata extraHeaders = new Metadata();
        Metadata.Key<String> headerKey = Metadata.Key.of("Idempotency-Key", Metadata.ASCII_STRING_MARSHALLER);
        extraHeaders.put(headerKey, idempotencyKey);
        CreateAccountResponse response = accountsService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders))
                .createAccount(request);
        Assertions.assertFalse(response.hasAccount());
        Assertions.assertNotEquals("", response.getOperationId());
        Assertions.assertEquals(AccountOperationStatus.ACCOUNT_OPERATION_IN_PROGRESS, response.getOperationStatus());
        GetOperationStateRequest operationStateRequest = GetOperationStateRequest.newBuilder()
                .setOperationId(response.getOperationId())
                .build();
        OperationState operationState = operationsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getOperationState(operationStateRequest);
        Assertions.assertNotNull(operationState);
        Assertions.assertEquals(response.getOperationId(), operationState.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationState.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(), operationState.getAccountsSpaceId().getId());
        Assertions.assertEquals(OperationStatus.IN_PROGRESS, operationState.getStatus());
        Assertions.assertTrue(operationState.hasFailure());
        Assertions.assertEquals("Error on account creation, status code: UNAVAILABLE Test failure. " +
                "Account may be created on next provider sync.", operationState.getFailure().getErrorsList().get(0));
        CreateAccountResponse idempotencyResponse = accountsService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders))
                .createAccount(request);
        Assertions.assertFalse(idempotencyResponse.hasAccount());
        Assertions.assertNotEquals("", idempotencyResponse.getOperationId());
        Assertions.assertEquals(AccountOperationStatus.ACCOUNT_OPERATION_IN_PROGRESS,
                idempotencyResponse.getOperationStatus());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testCreateAccountConflictNoMatchIdempotency() {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.ALREADY_EXISTS.withDescription("Test failure")))));
        stubProviderService.setListAccountsByFolderResponses(List.of(GrpcResponse.success(ListAccountsByFolderResponse
                .newBuilder().build())));
        CreateAccountRequest request = CreateAccountRequest.newBuilder()
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setProviderId(TestProviders.YP_ID)
                .setExternalKey(AccountKey.newBuilder().setValue("outerKey").build())
                .setDisplayName(AccountName.newBuilder().setValue("outerDisplayName").build())
                .setAccountsSpace(ProviderAccountsSpace.newBuilder().setId(TEST_ACCOUNT_SPACE_1.getId()).build())
                .setFreeTier(false)
                .build();
        String idempotencyKey = UUID.randomUUID().toString();
        Metadata extraHeaders = new Metadata();
        Metadata.Key<String> headerKey = Metadata.Key.of("Idempotency-Key", Metadata.ASCII_STRING_MARSHALLER);
        extraHeaders.put(headerKey, idempotencyKey);
        boolean error = false;
        try {
            accountsService
                    .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                    .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders))
                    .createAccount(request);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.ALREADY_EXISTS, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            Assertions.assertEquals("Error on account creation, status code: ALREADY_EXISTS Test failure. " +
                    "Account may be created on next provider sync.", details.get().getErrorsList().get(0));
            FolderOperationLogModel folderLog = tableClient.usingSessionMonoRetryable(session -> folderOperationLogDao
                            .getFirstPageByFolder(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                    Tenants.DEFAULT_TENANT_ID, TestFolders.TEST_FOLDER_1_ID, SortOrderDto.ASC, 100))
                    .block().stream()
                    .filter(f -> f.getOperationType() == FolderOperationType.CREATE_ACCOUNT)
                    .sorted(Comparator.comparing(FolderOperationLogModel::getOrder).reversed()).findFirst().get();
            String opId = folderLog.getAccountsQuotasOperationsId().orElse(null);
            Assertions.assertNotNull(opId);
            AccountsQuotasOperationsModel op = tableClient.usingSessionMonoRetryable(session ->
                    quotasOperationsDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                            opId, Tenants.DEFAULT_TENANT_ID)).block().get();
            AccountsQuotasOperationsModel.RequestStatus opStatus = op.getRequestStatus().get();
            Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.ERROR, opStatus);
            Assertions.assertTrue(op.getErrorMessage().isPresent());
            Optional<OperationInProgressModel> opInP = tableClient.usingSessionMonoRetryable(session ->
                    operationsInProgressDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                            new OperationInProgressModel.Key(opId, TestFolders.TEST_FOLDER_1_ID),
                            Tenants.DEFAULT_TENANT_ID)).block();
            Assertions.assertTrue(opInP.isEmpty());
            error = true;
        }
        Assertions.assertTrue(error);

        boolean idempotencyError = false;
        try {
            accountsService
                    .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                    .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders))
                    .createAccount(request);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.ALREADY_EXISTS, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            Assertions.assertEquals("Error on account creation, status code: ALREADY_EXISTS Test failure. " +
                    "Account may be created on next provider sync.", details.get().getErrorsList().get(0));
            idempotencyError = true;
        }
        Assertions.assertTrue(idempotencyError);
    }

}
