package ru.yandex.intranet.d.web.front.folders;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.protobuf.util.Timestamps;
import com.yandex.ydb.table.transaction.TransactionMode;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuple2;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.LogCollector;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.TestSegmentations;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.backend.service.provider_proto.Account;
import ru.yandex.intranet.d.backend.service.provider_proto.AccountsSpaceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.Amount;
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundAccountsSpaceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundResourceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.KnownAccountProvisions;
import ru.yandex.intranet.d.backend.service.provider_proto.LastUpdate;
import ru.yandex.intranet.d.backend.service.provider_proto.PassportUID;
import ru.yandex.intranet.d.backend.service.provider_proto.Provision;
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceSegmentKey;
import ru.yandex.intranet.d.backend.service.provider_proto.StaffLogin;
import ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionRequest;
import ru.yandex.intranet.d.backend.service.provider_proto.UserID;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasOperationsDao;
import ru.yandex.intranet.d.dao.quotas.QuotasDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse;
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.services.integration.providers.rest.model.UpdateProvisionRequestDto;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.operations.OperationDto;
import ru.yandex.intranet.d.web.model.operations.OperationRequestLogDto;
import ru.yandex.intranet.d.web.model.quotas.AccountsQuotasOperationsDto;
import ru.yandex.intranet.d.web.model.quotas.ProvisionLiteDto;
import ru.yandex.intranet.d.web.model.quotas.UpdateProvisionsAnswerDto;

import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_ID;
import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestResources.YP_HDD_MAN;
import static ru.yandex.intranet.d.TestResources.YP_SSD_MAN;
import static ru.yandex.intranet.d.UnitIds.GIGABYTES;
import static ru.yandex.intranet.d.web.front.folders.FrontQuotasApiTest.getBody;
import static ru.yandex.intranet.d.web.front.folders.FrontQuotasApiTest.prepareFailedPreconditionResponseTest;
import static ru.yandex.intranet.d.web.front.folders.FrontQuotasApiTest.prepareUpdateProvisionsOkResponseTest;
import static ru.yandex.intranet.d.web.front.folders.FrontQuotasApiTest.prepareUpdateProvisionsUnknownResponseTest;

/**
 * Front quotas API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class FrontQuotasApiTest4 {
    @Autowired
    private YdbTableClient ydbTableClient;
    @Autowired
    private StubProviderService stubProviderService;
    @Autowired
    private WebTestClient webClient;
    @Autowired
    private AccountsQuotasOperationsDao operationsDao;
    @Autowired
    private QuotasDao quotasDao;

    @Test
    public void updateProvisionsOkResponseIdempotencyTest() {
        prepareUpdateProvisionsOkResponseTest(stubProviderService);
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES),
                new ProvisionLiteDto(YP_SSD_MAN, // resourceId
                        "10", // provided amount
                        GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        GIGABYTES));
        String idempotencyKey = UUID.randomUUID().toString();
        UpdateProvisionsAnswerDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionsAnswerDto.class)
                .returnResult()
                .getResponseBody();

        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();

        Assertions.assertNotNull(responseBody);
        AccountsQuotasOperationsDto accountsQuotasOperationsDto = responseBody.getAccountsQuotasOperationsDto();
        Assertions.assertNotNull(accountsQuotasOperationsDto);
        AccountsQuotasOperationsDto expectedAccountsQuotasOperationsDto = new AccountsQuotasOperationsDto(operationId,
                AccountsQuotasOperationsModel.RequestStatus.OK);
        Assertions.assertEquals(expectedAccountsQuotasOperationsDto, accountsQuotasOperationsDto);

        UpdateProvisionsAnswerDto idempotencyResponseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionsAnswerDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(idempotencyResponseBody);
        AccountsQuotasOperationsDto idempotencyAccountsQuotasOperationsDto = idempotencyResponseBody
                .getAccountsQuotasOperationsDto();
        Assertions.assertNotNull(idempotencyAccountsQuotasOperationsDto);
        AccountsQuotasOperationsDto idempotencyExpectedAccountsQuotasOperationsDto = new AccountsQuotasOperationsDto(
                operationId, AccountsQuotasOperationsModel.RequestStatus.OK);
        Assertions.assertEquals(idempotencyExpectedAccountsQuotasOperationsDto, idempotencyAccountsQuotasOperationsDto);

        String expectedUrlKey = TestSegmentations.YP_SEGMENT_DEFAULT + "." +
                TestSegmentations.YP_LOCATION_MAN + ":Yandex Deploy";
        String expectedUrl = "https://deploy.yandex-team.ru/yp/man/pod-sets?accountId=abc:service:1&segments=default";
        var expandedAccount = idempotencyResponseBody.getExpandedProvider().getAccounts().get(0);

        Assertions.assertNotNull(expandedAccount.getExternalAccountUrls());
        Assertions.assertFalse(expandedAccount.getExternalAccountUrls().isEmpty());
        Assertions.assertTrue(expandedAccount.getUrlsForSegments());
        Assertions.assertTrue(expandedAccount.getExternalAccountUrls().containsKey(expectedUrlKey));
        Assertions.assertEquals(expectedUrl, expandedAccount.getExternalAccountUrls().get(expectedUrlKey));
    }

    @Test
    public void testRetryableFailureFailedInlineRetryIdempotency() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Test failure")))));
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES),
                new ProvisionLiteDto(YP_SSD_MAN, // resourceId
                        "10", // provided amount
                        GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        GIGABYTES));
        String idempotencyKey = UUID.randomUUID().toString();
        ErrorCollectionDto responseBody = LogCollector.collectLogs(() -> webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody());

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(Set.of("Can't update right now, update job was scheduled."),
                responseBody.getErrors());

        ErrorCollectionDto idempotencyResponseBody = LogCollector.collectLogs(() -> webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody());

        Assertions.assertNotNull(idempotencyResponseBody);
        Assertions.assertEquals(Set.of("Can't update right now, update job was scheduled."),
                idempotencyResponseBody.getErrors());
    }

    @Test
    public void testConflictNoMatchIdempotency() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Test failure")))));
        stubProviderService.setGetAccountResponses(List.of(GrpcResponse.success(Account
                .newBuilder()
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                        .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                .addAllResourceSegmentKeys(List.of(
                                        ResourceSegmentKey.newBuilder()
                                                .setResourceSegmentationKey("location")
                                                .setResourceSegmentKey("man")
                                                .build(),
                                        ResourceSegmentKey.newBuilder()
                                                .setResourceSegmentationKey("segment")
                                                .setResourceSegmentKey("default")
                                                .build()
                                ))
                                .build())
                        .build())
                .setAccountId("123")
                .setDeleted(false)
                .setFolderId(TEST_FOLDER_1_ID)
                .setDisplayName("тестовый аккаунт")
                .setKey("dummy")
                .setFreeTier(false)
                .addAllProvisions(List.of())
                .build())));
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES),
                new ProvisionLiteDto(YP_SSD_MAN, // resourceId
                        "10", // provided amount
                        GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        GIGABYTES));
        String idempotencyKey = UUID.randomUUID().toString();
        ErrorCollectionDto responseBody = LogCollector.collectLogs(() -> webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody());

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(Set.of("Error occurred while performing the operation on the provider side."),
                responseBody.getErrors());
        Assertions.assertNotNull(responseBody.getDetails());
        Assertions.assertEquals(1, responseBody.getDetails().get("errorFromProvider").size());
        String errorFromProvider = (String) responseBody.getDetails().get("errorFromProvider")
                .stream().findFirst().get();
        Assertions.assertTrue(errorFromProvider.startsWith("FAILED_PRECONDITION Test failure\nRequest id:"));

        ErrorCollectionDto idempotencyResponseBody = LogCollector.collectLogs(() -> webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody());

        Assertions.assertNotNull(idempotencyResponseBody);
        Assertions.assertEquals(
                Set.of("Error occurred while performing the operation on the provider side."),
                idempotencyResponseBody.getErrors());
        Assertions.assertNotNull(idempotencyResponseBody.getDetails());
        Assertions.assertEquals(1, idempotencyResponseBody.getDetails().get("errorFromProvider").size());
        String idempotencyErrorFromProvider = (String) idempotencyResponseBody.getDetails().get("errorFromProvider")
                .stream().findFirst().get();
        Assertions.assertTrue(idempotencyErrorFromProvider.startsWith("FAILED_PRECONDITION Test failure\nRequest id:"));
    }

    @Test
    public void testOvercommitProhibitedErrorMessage() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Test failure")))));
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "50", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES),
                new ProvisionLiteDto(YP_SSD_MAN, // resourceId
                        "10", // provided amount
                        GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        GIGABYTES));
        ErrorCollectionDto responseBody = LogCollector.collectLogs(() -> webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody());

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(
                Set.of("Error when changed quota: the provided quota cannot be less than the allocated quota"),
                responseBody.getErrors());
        Assertions.assertNotNull(responseBody.getDetails());
        Assertions.assertEquals(1, responseBody.getDetails().get("errorFromProvider").size());
        String errorFromProvider = (String) responseBody.getDetails().get("errorFromProvider")
                .stream().findFirst().get();
        Assertions.assertTrue(errorFromProvider.startsWith("Test failure\nRequest id:"));
    }

    @Test
    public void testOvercommitProhibitedOnConflictErrorMessage() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .failure(new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Test failure")))));
        stubProviderService.setGetAccountResponses(List.of(GrpcResponse.success(Account
                .newBuilder()
                .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                        .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                .addAllResourceSegmentKeys(List.of(
                                        ResourceSegmentKey.newBuilder()
                                                .setResourceSegmentationKey("location")
                                                .setResourceSegmentKey("man")
                                                .build(),
                                        ResourceSegmentKey.newBuilder()
                                                .setResourceSegmentationKey("segment")
                                                .setResourceSegmentKey("default")
                                                .build()
                                ))
                                .build())
                        .build())
                .setAccountId("123")
                .setDeleted(false)
                .setFolderId(TEST_FOLDER_1_ID)
                .setDisplayName("тестовый аккаунт")
                .setKey("dummy")
                .setFreeTier(false)
                .addAllProvisions(List.of())
                .build())));
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "50", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES),
                new ProvisionLiteDto(YP_SSD_MAN, // resourceId
                        "10", // provided amount
                        GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        GIGABYTES));
        String idempotencyKey = UUID.randomUUID().toString();
        ErrorCollectionDto responseBody = LogCollector.collectLogs(() -> webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody());

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(
                Set.of("Error when changed quota: the provided quota cannot be less than the allocated quota"),
                responseBody.getErrors());
        Assertions.assertNotNull(responseBody.getDetails());
        Assertions.assertEquals(1, responseBody.getDetails().get("errorFromProvider").size());
        String errorFromProvider = (String) responseBody.getDetails().get("errorFromProvider")
                .stream().findFirst().get();
        Assertions.assertTrue(errorFromProvider.startsWith("FAILED_PRECONDITION Test failure\nRequest id:"));
    }

    @Test
    public void overcommitProhibitedErrorMessageShouldBeComputeUsingProviderResponseTest() {
        prepareFailedPreconditionResponseTest(stubProviderService);
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES));
        ErrorCollectionDto responseBody = LogCollector.collectLogs(() -> webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody());

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(
                Set.of("Error occurred while performing the operation on the provider side."),
                responseBody.getErrors());
        Assertions.assertNotNull(responseBody.getDetails());
        Assertions.assertEquals(1, responseBody.getDetails().get("errorFromProvider").size());
        String errorFromProvider = (String) responseBody.getDetails().get("errorFromProvider")
                .stream().findFirst().get();
        Assertions.assertTrue(errorFromProvider.startsWith("""
                FAILED_PRECONDITION Test error Test error
                test: Test error description
                Request id:"""));
    }

    @Test
    public void updateProvisionsIncorrectResponseValidationTest() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .success(ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionResponse.newBuilder()
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("hdd")
                                                .build())
                                        .build())
                                .setLastUpdate(LastUpdate.newBuilder()
                                        .setAuthor(UserID.newBuilder()
                                                .setPassportUid(PassportUID.newBuilder()
                                                        .setPassportUid("1")
                                                        .build())
                                                .setStaffLogin(StaffLogin.newBuilder()
                                                        .setStaffLogin("test")
                                                        .build())
                                                .build())
                                        .setOperationId(UUID.randomUUID().toString())
                                        .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(100)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(100)
                                        .setUnitKey("") // wrong value
                                        .build())
                                .build())
                        .build())));

        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES));
        ErrorCollectionDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Map<String, Set<String>> errors = responseBody.getFieldErrors();
        Assertions.assertNotNull(errors);
        Assertions.assertTrue(errors.containsKey("provisions.0.allocatedAmountUnitKey"));
        Assertions.assertEquals(
                errors.get("provisions.0.allocatedAmountUnitKey"),
                Set.of("The value can not be converted to base unit.")
        );

        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();

        AccountsQuotasOperationsModel operation = Objects.requireNonNull(ydbTableClient.usingSessionMonoRetryable(
                session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> operationsDao.getById(txSession, operationId, Tenants.DEFAULT_TENANT_ID)
                )
        ).block()).orElseThrow();
        AccountsQuotasOperationsModel.RequestStatus requestStatus = operation.getRequestStatus().orElse(null);
        Assertions.assertEquals(requestStatus, AccountsQuotasOperationsModel.RequestStatus.WAITING);

        List<QuotaModel> quotaModels = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> quotasDao.getByIds(txSession,
                                List.of(new QuotaModel.Key(TestFolders.TEST_FOLDER_1_ID, TestProviders.YP_ID,
                                        TestResources.YP_HDD_MAN)),
                                Tenants.DEFAULT_TENANT_ID
                        )
                )
        ).block();
        Assertions.assertEquals(List.of(
                QuotaModel.builder()
                        .tenantId(Tenants.DEFAULT_TENANT_ID)
                        .folderId(TEST_FOLDER_1_ID)
                        .providerId(YP_ID)
                        .resourceId(YP_HDD_MAN)
                        .quota(1000000000000L)
                        .balance(800000000000L)
                        .frozenQuota(0L)
                        .build()
        ), quotaModels);
    }

    @Test
    public void updateProvisionsOkProviderResponseRequestLogTest() throws IOException {
        prepareUpdateProvisionsOkResponseTest(stubProviderService);
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES));
        UpdateProvisionsAnswerDto responseBody = LogCollector.collectLogs(() -> webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionsAnswerDto.class)
                .returnResult()
                .getResponseBody());
        Assertions.assertNotNull(responseBody);

        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest updateProvisionRequest = next.getT1();
        Assertions.assertNotNull(updateProvisionRequest);
        String operationId = updateProvisionRequest.getOperationId();
        Assertions.assertNotNull(operationId);
        List<KnownAccountProvisions> knownProvisionsList = updateProvisionRequest.getKnownProvisionsList();
        Assertions.assertNotNull(knownProvisionsList);

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
        ObjectReader updateProvisionRequestReader = objectMapper.readerFor(UpdateProvisionRequestDto.class);
        UpdateProvisionRequestDto operationRequestLogData = updateProvisionRequestReader.readValue(
                operationRequestLog.getRequestData());
        Assertions.assertEquals(updateProvisionRequest.getAbcServiceId(), operationRequestLogData.getAbcServiceId());
        Assertions.assertEquals(updateProvisionRequest.getFolderId(), operationRequestLogData.getFolderId());

        Assertions.assertNotNull(operationRequestLog.getResponseData());
        JsonNode provisionsNodes = operationRequestLog.getResponseData().findValue("provisions");
        Assertions.assertNotNull(provisionsNodes);
    }

    @Test
    public void updateProvisionsUnknownProviderResponseRequestLogTest() throws IOException {
        prepareUpdateProvisionsUnknownResponseTest(stubProviderService);
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES));
        ErrorCollectionDto responseBody = LogCollector.collectLogs(() -> webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody());
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(Set.of("Error occurred while performing the operation on the provider side."),
                responseBody.getErrors());

        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest updateProvisionRequest = next.getT1();
        Assertions.assertNotNull(updateProvisionRequest);
        String operationId = updateProvisionRequest.getOperationId();
        Assertions.assertNotNull(operationId);
        List<KnownAccountProvisions> knownProvisionsList = updateProvisionRequest.getKnownProvisionsList();
        Assertions.assertNotNull(knownProvisionsList);

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
        ObjectReader updateProvisionRequestReader = objectMapper.readerFor(UpdateProvisionRequestDto.class);
        UpdateProvisionRequestDto operationRequestLogData = updateProvisionRequestReader.readValue(
                operationRequestLog.getRequestData());
        Assertions.assertEquals(updateProvisionRequest.getAbcServiceId(), operationRequestLogData.getAbcServiceId());
        Assertions.assertEquals(updateProvisionRequest.getFolderId(), operationRequestLogData.getFolderId());
        Assertions.assertNotNull(operationRequestLog.getRequestData());
        Assertions.assertNotNull(operationRequestLog.getResponseData());
        JsonNode errorMessage = operationRequestLog.getResponseData().findValue("message");
        Assertions.assertEquals("UNKNOWN: Test error", errorMessage.asText());
    }
}
