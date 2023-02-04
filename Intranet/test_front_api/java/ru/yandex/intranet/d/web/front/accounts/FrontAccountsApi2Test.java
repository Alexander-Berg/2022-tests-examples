package ru.yandex.intranet.d.web.front.accounts;

import java.io.IOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.protobuf.Timestamp;
import com.yandex.ydb.table.transaction.TransactionMode;
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
import ru.yandex.intranet.d.backend.service.provider_proto.CurrentVersion;
import ru.yandex.intranet.d.backend.service.provider_proto.LastUpdate;
import ru.yandex.intranet.d.backend.service.provider_proto.ListAccountsByFolderResponse;
import ru.yandex.intranet.d.backend.service.provider_proto.PassportUID;
import ru.yandex.intranet.d.backend.service.provider_proto.StaffLogin;
import ru.yandex.intranet.d.backend.service.provider_proto.UserID;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse;
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
import ru.yandex.intranet.d.model.folders.FolderOperationType;
import ru.yandex.intranet.d.services.integration.providers.rest.model.CreateAccountRequestDto;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.AccountDto;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.SortOrderDto;
import ru.yandex.intranet.d.web.model.folders.FrontAccountInputDto;
import ru.yandex.intranet.d.web.model.operations.OperationDto;
import ru.yandex.intranet.d.web.model.operations.OperationRequestLogDto;

import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_1;

/**
 * Front accounts API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class FrontAccountsApi2Test {

    @Autowired
    private WebTestClient webClient;
    @Autowired
    private StubProviderService stubProviderService;
    @Autowired
    private YdbTableClient ydbTableClient;
    @Autowired
    private FolderOperationLogDao folderOperationLogDao;

    @Test
    public void accountsShouldBeCreatedIdempotency() {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse.success(Account.newBuilder()
                .setAccountId(UUID.randomUUID().toString())
                .setDeleted(false)
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setDisplayName("outerDisplayName")
                .setKey("outerKey")
                .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
                .setLastUpdate(LastUpdate.newBuilder()
                        .setAuthor(UserID.newBuilder()
                                .setPassportUid(PassportUID.newBuilder().setPassportUid("uuid").build())
                                .setStaffLogin(StaffLogin.newBuilder().setStaffLogin("login").build())
                                .build())
                        .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
                        .setOperationId(UUID.randomUUID().toString())
                        .build()).build())));
        String idempotencyKey = UUID.randomUUID().toString();
        AccountDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TestFolders.TEST_FOLDER_1_ID,
                        TestProviders.YP_ID,
                        "ac_name",
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AccountDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result.getId());
        Assertions.assertEquals(result.getFolderId(), TestFolders.TEST_FOLDER_1_ID);
        AccountDto idempotencyResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TestFolders.TEST_FOLDER_1_ID,
                        TestProviders.YP_ID,
                        null,
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AccountDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(idempotencyResult.getId());
        Assertions.assertEquals(idempotencyResult.getFolderId(), TestFolders.TEST_FOLDER_1_ID);
    }

    @Test
    public void testCreateAccountRetryableFailureFailedInlineRetryIdempotency() {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Test failure")))));
        String idempotencyKey = UUID.randomUUID().toString();
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TestFolders.TEST_FOLDER_1_ID,
                        TestProviders.YP_ID,
                        "ac_name",
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Set.of("Error on account creation, status code: UNAVAILABLE Test failure. " +
                "Account may be created on next provider sync."), result.getErrors());
        ErrorCollectionDto idempotencyResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TestFolders.TEST_FOLDER_1_ID,
                        TestProviders.YP_ID,
                        "ac_name",
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(),
                        null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(idempotencyResult);
        Assertions.assertEquals(Set.of("Error on account creation. Account may be created on next provider sync."),
                idempotencyResult.getErrors());
    }

    @Test
    public void testCreateAccountConflictNoMatchIdempotency() {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.ALREADY_EXISTS.withDescription("Test failure")))));
        stubProviderService.setListAccountsByFolderResponses(List.of(GrpcResponse.success(ListAccountsByFolderResponse
                .newBuilder().build())));
        String idempotencyKey = UUID.randomUUID().toString();
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TestFolders.TEST_FOLDER_1_ID,
                        TestProviders.YP_ID,
                        "ac_name",
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(),
                        null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Set.of("Error on account creation, status code: ALREADY_EXISTS Test failure. " +
                "Account may be created on next provider sync."), result.getErrors());
        ErrorCollectionDto idempotencyResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TestFolders.TEST_FOLDER_1_ID,
                        TestProviders.YP_ID,
                        null,
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(idempotencyResult);
        Assertions.assertEquals(Set.of("Error on account creation, status code: ALREADY_EXISTS Test failure. " +
                        "Account may be created on next provider sync."),
                idempotencyResult.getErrors());
    }

    @Test
    public void createAccountsProviderRequestLogTest() throws IOException {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse.success(Account.newBuilder()
                .setAccountId(UUID.randomUUID().toString())
                .setDeleted(false)
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setDisplayName("outerDisplayName")
                .setKey("outerKey")
                .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
                .setLastUpdate(LastUpdate.newBuilder()
                        .setAuthor(UserID.newBuilder()
                                .setPassportUid(PassportUID.newBuilder().setPassportUid("uuid").build())
                                .setStaffLogin(StaffLogin.newBuilder().setStaffLogin("login").build())
                                .build())
                        .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
                        .setOperationId(UUID.randomUUID().toString())
                        .build()).build())));

        AccountDto accountDto = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TestFolders.TEST_FOLDER_1_ID,
                        TestProviders.YP_ID,
                        "ac_name",
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AccountDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(accountDto);
        Assertions.assertNotNull(accountDto.getId());
        Assertions.assertEquals(accountDto.getFolderId(), TestFolders.TEST_FOLDER_1_ID);

        List<FolderOperationLogModel> folderLogs = ydbTableClient.usingSessionMonoRetryable(session ->
                folderOperationLogDao.getFirstPageByFolder(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID,
                        TestFolders.TEST_FOLDER_1_ID,
                        SortOrderDto.ASC,
                        100)
        ).block();
        Assertions.assertNotNull(folderLogs);
        folderLogs = folderLogs.stream().filter(f -> f.getOperationType() == FolderOperationType.CREATE_ACCOUNT)
            .sorted(Comparator.comparing(FolderOperationLogModel::getOrder).reversed()).collect(Collectors.toList());
        Assertions.assertTrue(folderLogs.size() > 0);
        FolderOperationLogModel folderLog = folderLogs.get(0);

        String operationId = folderLog.getAccountsQuotasOperationsId().orElse(null);
        Assertions.assertNotNull(operationId);

        OperationDto operation = webClient
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
        Assertions.assertNotNull(operation);
        Assertions.assertTrue(operation.getRequestLogs().isPresent());
        Assertions.assertTrue(operation.getRequestLogs().get().size() > 0);
        OperationRequestLogDto operationRequestLog = operation.getRequestLogs().get().get(0);

        Assertions.assertNotNull(operationRequestLog.getRequestData());
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectReader createAccountRequestReader = objectMapper.readerFor(CreateAccountRequestDto.class);
        CreateAccountRequestDto operationRequestLogData = createAccountRequestReader.readValue(
                operationRequestLog.getRequestData());
        Assertions.assertNotNull(operationRequestLogData);
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, operationRequestLogData.getFolderId());

        Assertions.assertNotNull(operationRequestLog.getResponseData());
        JsonNode folderIdNode = operationRequestLog.getResponseData().findValue("folderId");
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, folderIdNode.asText());
    }

    @Test
    public void createAccountsProviderFailRequestLogTest() throws IOException {
        stubProviderService.setCreateAccountResponses(List.of(
                GrpcResponse.failure(new StatusRuntimeException(Status.ALREADY_EXISTS))
        ));

        ErrorCollectionDto errors = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TestFolders.TEST_FOLDER_1_ID,
                        TestProviders.YP_ID,
                        "ac_name",
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(errors);

        List<FolderOperationLogModel> folderLogs = ydbTableClient.usingSessionMonoRetryable(session ->
                folderOperationLogDao.getFirstPageByFolder(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID,
                        TestFolders.TEST_FOLDER_1_ID,
                        SortOrderDto.ASC,
                        100)
        ).block();
        Assertions.assertNotNull(folderLogs);
        folderLogs = folderLogs.stream().filter(f -> f.getOperationType() == FolderOperationType.CREATE_ACCOUNT)
            .sorted(Comparator.comparing(FolderOperationLogModel::getOrder).reversed()).collect(Collectors.toList());
        Assertions.assertTrue(folderLogs.size() > 0);
        FolderOperationLogModel folderLog = folderLogs.get(0);

        String operationId = folderLog.getAccountsQuotasOperationsId().orElse(null);
        Assertions.assertNotNull(operationId);

        OperationDto operation = webClient
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
        Assertions.assertNotNull(operation);
        Assertions.assertTrue(operation.getRequestLogs().isPresent());
        Assertions.assertTrue(operation.getRequestLogs().get().size() > 0);
        OperationRequestLogDto operationRequestLog = operation.getRequestLogs().get().get(0);

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectReader createAccountRequestReader = objectMapper.readerFor(CreateAccountRequestDto.class);
        CreateAccountRequestDto operationRequestLogData = createAccountRequestReader.readValue(
                operationRequestLog.getRequestData());
        Assertions.assertNotNull(operationRequestLogData);
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, operationRequestLogData.getFolderId());

        Assertions.assertNotNull(operationRequestLog.getRequestData());
        JsonNode responseMessage = operationRequestLog.getResponseData().findValue("message");
        Assertions.assertNotNull(responseMessage);
        Assertions.assertEquals("ALREADY_EXISTS", responseMessage.asText());
    }
}
