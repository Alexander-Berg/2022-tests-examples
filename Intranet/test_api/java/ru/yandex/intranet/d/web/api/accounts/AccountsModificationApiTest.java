package ru.yandex.intranet.d.web.api.accounts;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestUsers;
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
import ru.yandex.intranet.d.dao.accounts.AccountsDao;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasOperationsDao;
import ru.yandex.intranet.d.dao.accounts.OperationsInProgressDao;
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse;
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.accounts.AccountOperationDto;
import ru.yandex.intranet.d.web.model.accounts.AccountOperationStatusDto;
import ru.yandex.intranet.d.web.model.accounts.CreateAccountDto;
import ru.yandex.intranet.d.web.model.operations.OperationDto;
import ru.yandex.intranet.d.web.model.operations.OperationStatusDto;

import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_1;

/**
 * Accounts modification API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class AccountsModificationApiTest {

    @Autowired
    private WebTestClient webClient;
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
        CreateAccountDto request = CreateAccountDto.builder()
                .providerId(TestProviders.YP_ID)
                .externalKey("outerKey")
                .displayName("outerDisplayName")
                .accountsSpaceId(TEST_ACCOUNT_SPACE_1.getId())
                .freeTier(false)
                .build();
        AccountOperationDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(AccountOperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getResult().isPresent());
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.getResult().get().getFolderId());
        Assertions.assertEquals(TestProviders.YP_ID, result.getResult().get().getProviderId());
        Assertions.assertEquals("outerKey", result.getResult().get().getExternalKey().orElseThrow());
        Assertions.assertEquals("outerDisplayName", result.getResult().get().getDisplayName().orElseThrow());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(),
                result.getResult().get().getAccountsSpaceId().orElseThrow());
        Assertions.assertNotEquals("", result.getResult().get().getId());
        Assertions.assertNotEquals("", result.getResult().get().getExternalId());
        Assertions.assertNotEquals("", result.getOperationId());
        Assertions.assertFalse(result.getResult().get().isFreeTier());
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.getOperationStatus());
        OperationDto operationResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/operations/{id}", result.getOperationId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(operationResult);
        Assertions.assertEquals(result.getOperationId(), operationResult.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(), operationResult.getAccountsSpaceId().orElseThrow());
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.getStatus());
        Assertions.assertFalse(operationResult.getFailure().isPresent());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateAccountNonRetryableFailure() {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Test failure")))));
        CreateAccountDto request = CreateAccountDto.builder()
                .providerId(TestProviders.YP_ID)
                .externalKey("outerKey")
                .displayName("outerDisplayName")
                .accountsSpaceId(TEST_ACCOUNT_SPACE_1.getId())
                .freeTier(false)
                .build();
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Set.of("Error on account creation, status code: INVALID_ARGUMENT Test failure. " +
                "Account may be created on next provider sync."), result.getErrors());
        Map<String, ?> operationMeta = (Map<String, ?>) ((Set<?>) result.getDetails().get("operationMeta"))
                .iterator().next();
        Assertions.assertTrue(operationMeta.containsKey("operationId"));
        String operationId = (String) operationMeta.get("operationId");
        OperationDto operationResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/operations/{id}", operationId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(operationResult);
        Assertions.assertEquals(operationId, operationResult.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(), operationResult.getAccountsSpaceId().orElseThrow());
        Assertions.assertEquals(OperationStatusDto.FAILURE, operationResult.getStatus());
        Assertions.assertTrue(operationResult.getFailure().isPresent());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateAccountConflictNoMatch() {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.ALREADY_EXISTS.withDescription("Test failure")))));
        stubProviderService.setListAccountsByFolderResponses(List.of(GrpcResponse.success(ListAccountsByFolderResponse
                .newBuilder().build())));
        CreateAccountDto request = CreateAccountDto.builder()
                .providerId(TestProviders.YP_ID)
                .externalKey("outerKey")
                .displayName("outerDisplayName")
                .accountsSpaceId(TEST_ACCOUNT_SPACE_1.getId())
                .freeTier(false)
                .build();
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Set.of("Error on account creation, status code: ALREADY_EXISTS Test failure. " +
                "Account may be created on next provider sync."), result.getErrors());
        Map<String, ?> operationMeta = (Map<String, ?>) ((Set<?>) result.getDetails().get("operationMeta"))
                .iterator().next();
        Assertions.assertTrue(operationMeta.containsKey("operationId"));
        String operationId = (String) operationMeta.get("operationId");
        OperationDto operationResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/operations/{id}", operationId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(operationResult);
        Assertions.assertEquals(operationId, operationResult.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(), operationResult.getAccountsSpaceId().orElseThrow());
        Assertions.assertEquals(OperationStatusDto.FAILURE, operationResult.getStatus());
        Assertions.assertTrue(operationResult.getFailure().isPresent());
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
        CreateAccountDto request = CreateAccountDto.builder()
                .providerId(TestProviders.YP_ID)
                .externalKey("outerKey")
                .displayName("outerDisplayName")
                .accountsSpaceId(TEST_ACCOUNT_SPACE_1.getId())
                .freeTier(false)
                .build();
        AccountOperationDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(AccountOperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getResult().isPresent());
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.getResult().get().getFolderId());
        Assertions.assertEquals(TestProviders.YP_ID, result.getResult().get().getProviderId());
        Assertions.assertEquals("outerKey", result.getResult().get().getExternalKey().orElseThrow());
        Assertions.assertEquals("outerDisplayName", result.getResult().get().getDisplayName().orElseThrow());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(),
                result.getResult().get().getAccountsSpaceId().orElseThrow());
        Assertions.assertNotEquals("", result.getResult().get().getId());
        Assertions.assertNotEquals("", result.getResult().get().getExternalId());
        Assertions.assertNotEquals("", result.getOperationId());
        Assertions.assertFalse(result.getResult().get().isFreeTier());
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.getOperationStatus());
        OperationDto operationResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/operations/{id}", result.getOperationId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(operationResult);
        Assertions.assertEquals(result.getOperationId(), operationResult.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(), operationResult.getAccountsSpaceId().orElseThrow());
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.getStatus());
        Assertions.assertFalse(operationResult.getFailure().isPresent());
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
        CreateAccountDto request = CreateAccountDto.builder()
                .providerId(TestProviders.YP_ID)
                .externalKey("outerKey")
                .displayName("outerDisplayName")
                .accountsSpaceId(TEST_ACCOUNT_SPACE_1.getId())
                .freeTier(false)
                .build();
        AccountOperationDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(AccountOperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getResult().isPresent());
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.getResult().get().getFolderId());
        Assertions.assertEquals(TestProviders.YP_ID, result.getResult().get().getProviderId());
        Assertions.assertEquals("outerKey", result.getResult().get().getExternalKey().orElseThrow());
        Assertions.assertEquals("outerDisplayName", result.getResult().get().getDisplayName().orElseThrow());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(),
                result.getResult().get().getAccountsSpaceId().orElseThrow());
        Assertions.assertNotEquals("", result.getResult().get().getId());
        Assertions.assertNotEquals("", result.getResult().get().getExternalId());
        Assertions.assertNotEquals("", result.getOperationId());
        Assertions.assertFalse(result.getResult().get().isFreeTier());
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.getOperationStatus());
        OperationDto operationResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/operations/{id}", result.getOperationId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(operationResult);
        Assertions.assertEquals(result.getOperationId(), operationResult.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(), operationResult.getAccountsSpaceId().orElseThrow());
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.getStatus());
        Assertions.assertFalse(operationResult.getFailure().isPresent());
    }

    @Test
    public void testCreateAccountRetryableFailureFailedInlineRetry() {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Test failure")))));
        CreateAccountDto request = CreateAccountDto.builder()
                .providerId(TestProviders.YP_ID)
                .externalKey("outerKey")
                .displayName("outerDisplayName")
                .accountsSpaceId(TEST_ACCOUNT_SPACE_1.getId())
                .freeTier(false)
                .build();
        AccountOperationDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isAccepted()
                .expectBody(AccountOperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getResult().isEmpty());
        Assertions.assertNotEquals("", result.getOperationId());
        Assertions.assertEquals(AccountOperationStatusDto.IN_PROGRESS, result.getOperationStatus());
        OperationDto operationResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/operations/{id}", result.getOperationId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(operationResult);
        Assertions.assertEquals(result.getOperationId(), operationResult.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(), operationResult.getAccountsSpaceId().orElseThrow());
        Assertions.assertEquals(OperationStatusDto.IN_PROGRESS, operationResult.getStatus());
        Assertions.assertFalse(operationResult.getFailure().isEmpty());
    }

    @Test
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
        CreateAccountDto request = CreateAccountDto.builder()
                .providerId(TestProviders.YP_ID)
                .externalKey("outerKey")
                .displayName("outerDisplayName")
                .accountsSpaceId(TEST_ACCOUNT_SPACE_1.getId())
                .freeTier(false)
                .build();
        String idempotencyKey = UUID.randomUUID().toString();
        AccountOperationDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(AccountOperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getResult().isPresent());
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, result.getResult().get().getFolderId());
        Assertions.assertEquals(TestProviders.YP_ID, result.getResult().get().getProviderId());
        Assertions.assertEquals("outerKey", result.getResult().get().getExternalKey().orElseThrow());
        Assertions.assertEquals("outerDisplayName", result.getResult().get().getDisplayName().orElseThrow());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(),
                result.getResult().get().getAccountsSpaceId().orElseThrow());
        Assertions.assertNotEquals("", result.getResult().get().getId());
        Assertions.assertNotEquals("", result.getResult().get().getExternalId());
        Assertions.assertNotEquals("", result.getOperationId());
        Assertions.assertFalse(result.getResult().get().isFreeTier());
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, result.getOperationStatus());
        OperationDto operationResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/operations/{id}", result.getOperationId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(operationResult);
        Assertions.assertEquals(result.getOperationId(), operationResult.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(), operationResult.getAccountsSpaceId().orElseThrow());
        Assertions.assertEquals(OperationStatusDto.SUCCESS, operationResult.getStatus());
        Assertions.assertFalse(operationResult.getFailure().isPresent());

        AccountOperationDto idempotencyResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(AccountOperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(idempotencyResult);
        Assertions.assertTrue(idempotencyResult.getResult().isPresent());
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, idempotencyResult.getResult().get().getFolderId());
        Assertions.assertEquals(TestProviders.YP_ID, idempotencyResult.getResult().get().getProviderId());
        Assertions.assertEquals("outerKey", idempotencyResult.getResult().get().getExternalKey().orElseThrow());
        Assertions.assertEquals("outerDisplayName", idempotencyResult.getResult().get().getDisplayName()
                .orElseThrow());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(),
                idempotencyResult.getResult().get().getAccountsSpaceId().orElseThrow());
        Assertions.assertNotEquals("", idempotencyResult.getResult().get().getId());
        Assertions.assertNotEquals("", idempotencyResult.getResult().get().getExternalId());
        Assertions.assertNotEquals("", idempotencyResult.getOperationId());
        Assertions.assertFalse(idempotencyResult.getResult().get().isFreeTier());
        Assertions.assertEquals(AccountOperationStatusDto.SUCCESS, idempotencyResult.getOperationStatus());
    }

    @Test
    public void testCreateAccountRetryableFailureFailedInlineRetryIdempotency() {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Test failure")))));
        CreateAccountDto request = CreateAccountDto.builder()
                .providerId(TestProviders.YP_ID)
                .externalKey("outerKey")
                .displayName("outerDisplayName")
                .accountsSpaceId(TEST_ACCOUNT_SPACE_1.getId())
                .freeTier(false)
                .build();
        String idempotencyKey = UUID.randomUUID().toString();
        AccountOperationDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isAccepted()
                .expectBody(AccountOperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getResult().isEmpty());
        Assertions.assertNotEquals("", result.getOperationId());
        Assertions.assertEquals(AccountOperationStatusDto.IN_PROGRESS, result.getOperationStatus());
        OperationDto operationResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/operations/{id}", result.getOperationId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(operationResult);
        Assertions.assertEquals(result.getOperationId(), operationResult.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(), operationResult.getAccountsSpaceId().orElseThrow());
        Assertions.assertEquals(OperationStatusDto.IN_PROGRESS, operationResult.getStatus());
        Assertions.assertFalse(operationResult.getFailure().isEmpty());
        AccountOperationDto idempotencyResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isAccepted()
                .expectBody(AccountOperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(idempotencyResult);
        Assertions.assertTrue(idempotencyResult.getResult().isEmpty());
        Assertions.assertNotEquals("", idempotencyResult.getOperationId());
        Assertions.assertEquals(AccountOperationStatusDto.IN_PROGRESS, idempotencyResult.getOperationStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateAccountConflictNoMatchIdempotency() {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.ALREADY_EXISTS.withDescription("Test failure")))));
        stubProviderService.setListAccountsByFolderResponses(List.of(GrpcResponse.success(ListAccountsByFolderResponse
                .newBuilder().build())));
        CreateAccountDto request = CreateAccountDto.builder()
                .providerId(TestProviders.YP_ID)
                .externalKey("outerKey")
                .displayName("outerDisplayName")
                .accountsSpaceId(TEST_ACCOUNT_SPACE_1.getId())
                .freeTier(false)
                .build();
        String idempotencyKey = UUID.randomUUID().toString();
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Set.of("Error on account creation, status code: ALREADY_EXISTS Test failure. " +
                "Account may be created on next provider sync."), result.getErrors());
        Map<String, ?> operationMeta = (Map<String, ?>) ((Set<?>) result.getDetails().get("operationMeta"))
                .iterator().next();
        Assertions.assertTrue(operationMeta.containsKey("operationId"));
        String operationId = (String) operationMeta.get("operationId");
        OperationDto operationResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/operations/{id}", operationId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(operationResult);
        Assertions.assertEquals(operationId, operationResult.getId());
        Assertions.assertEquals(TestProviders.YP_ID, operationResult.getProviderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1.getId(), operationResult.getAccountsSpaceId().orElseThrow());
        Assertions.assertEquals(OperationStatusDto.FAILURE, operationResult.getStatus());
        Assertions.assertTrue(operationResult.getFailure().isPresent());
        ErrorCollectionDto idempotencyResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/folders/{folderId}/accounts", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(idempotencyResult);
        Assertions.assertEquals(Set.of("Error on account creation, status code: ALREADY_EXISTS Test failure. " +
                "Account may be created on next provider sync."), idempotencyResult.getErrors());
        Map<String, ?> idempotencyOperationMeta = (Map<String, ?>) ((Set<?>) idempotencyResult.getDetails()
                .get("operationMeta")).iterator().next();
        Assertions.assertTrue(idempotencyOperationMeta.containsKey("operationId"));
        String idempotencyOperationId = (String) idempotencyOperationMeta.get("operationId");
        Assertions.assertEquals(idempotencyOperationId, operationResult.getId());
    }

}
