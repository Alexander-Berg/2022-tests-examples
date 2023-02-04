package ru.yandex.intranet.d.web.front.accounts;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.protobuf.Timestamp;
import com.yandex.ydb.table.transaction.TransactionMode;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.LogCollector;
import ru.yandex.intranet.d.TestAccounts;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.backend.service.provider_proto.Account;
import ru.yandex.intranet.d.backend.service.provider_proto.AccountsPageToken;
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
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasOperationsDao;
import ru.yandex.intranet.d.dao.accounts.OperationsInProgressDao;
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao;
import ru.yandex.intranet.d.dao.providers.ProvidersDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse;
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService;
import ru.yandex.intranet.d.loaders.providers.ProvidersLoader;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel;
import ru.yandex.intranet.d.model.accounts.OperationInProgressModel;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
import ru.yandex.intranet.d.model.folders.FolderOperationType;
import ru.yandex.intranet.d.model.folders.OperationPhase;
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.AccountDto;
import ru.yandex.intranet.d.web.model.AvailableResourcesDto;
import ru.yandex.intranet.d.web.model.CreateAccountExpandedAnswerDto;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.ProviderDto;
import ru.yandex.intranet.d.web.model.SortOrderDto;
import ru.yandex.intranet.d.web.model.folders.FrontAccountInputDto;
import ru.yandex.intranet.d.web.model.folders.front.history.FrontFolderOperationLogDto;
import ru.yandex.intranet.d.web.model.folders.front.history.FrontFolderOperationLogPageDto;

import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_1;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_1_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_RESERVE_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_IN_CLOSING_SERVICE;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_IN_NON_EXPORTABLE_SERVICE;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_IN_RENAMING_SERVICE;
import static ru.yandex.intranet.d.TestProviders.CLAUD2_ID;
import static ru.yandex.intranet.d.TestProviders.MDB_ID;
import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestResources.YP_CPU_MAN;
import static ru.yandex.intranet.d.UnitIds.CORES;

/**
 * Front Accounts API Test
 *
 * @author Denis Blokhin <denblo@yandex-team.ru>
 */
@IntegrationTest
public class FrontAccountsApiTest {
    @Autowired
    private WebTestClient webClient;

    @Autowired
    private ProvidersDao providersDao;

    @Autowired
    private YdbTableClient ydbTableClient;

    @Autowired
    private StubProviderService stubProviderService;

    @Autowired
    private OperationsInProgressDao operationsInProgressDao;

    @Autowired
    private AccountsQuotasOperationsDao quotasOperationsDao;

    @Autowired
    private FolderOperationLogDao folderOperationLogDao;

    @Autowired
    private ProvidersLoader providersLoader;

    @BeforeEach
    void setUp() {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse.success(Account.newBuilder()
                .setAccountId(UUID.randomUUID().toString())
                .setDeleted(false)
                .setFolderId(TEST_FOLDER_1_ID)
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
    }

    @Test
    public void accountsShouldBeCreated() {
        final AccountDto result = LogCollector.collectLogs(() -> webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_ID,
                        YP_ID,
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
                .getResponseBody());

        Assertions.assertNotNull(result.getId());
        Assertions.assertEquals(result.getFolderId(), TEST_FOLDER_1_ID);

        LogCollector.Logs logs = LogCollector.takeLogs();
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Provider request \"{}\", opId: {}, requestId: {}"));
        Assertions.assertTrue(logs.contains(Level.INFO,
                "opId: {}, requestId: {}, request: {}"));
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Success on provider \"{}\", opId: {}, requestId: {}"));
        Assertions.assertTrue(logs.contains(Level.INFO,
                "Success on provider \"{}\", opId: {}, requestId: {}, result: {}"));
    }

    @Test
    public void expandedTest() {
        final CreateAccountExpandedAnswerDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts/_expanded")
                .bodyValue(
                        new FrontAccountInputDto(
                                TEST_FOLDER_1_ID,
                                YP_ID,
                                "account_name",
                                "account_key",
                                TEST_ACCOUNT_SPACE_1_ID,
                                null, null
                        )
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(CreateAccountExpandedAnswerDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        final ProviderDto provider = result.getProvider();
        Assertions.assertNotNull(provider);
        Assertions.assertEquals(YP_ID, provider.getId());
        Assertions.assertNotNull(result.getExpandedProvider().getAccounts().get(0));
        final AccountDto account = result.getExpandedProvider().getAccounts().get(0).getAccount();
        Assertions.assertNotNull(account);
        Assertions.assertEquals(TEST_FOLDER_1_ID, account.getFolderId());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1_ID, account.getAccountsSpacesId());
    }

    @Test
    public void providerIdAndFolderIdShouldBeValidated() {
        ErrorCollectionDto errors = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_ID,
                        MDB_ID + "bug",
                        null,
                        null,
                        null, null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(Set.of("Provider not found."), errors.getFieldErrors().get("providerId"));

        errors = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_ID + "invalid",
                        YP_ID,
                        null,
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(Set.of("Folder not found."), errors.getFieldErrors().get("folderId"));
    }

    @Test
    public void providersWithAccountNameAndKeySupportCanAcceptAccountNameAndKey() {

        ProviderModel ypProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        YP_ID, Tenants.DEFAULT_TENANT_ID)).block().get();

        ProviderModel updateYp = ProviderModel.builder(ypProvider)
                .accountsSettings(AccountsSettingsModel.builder(ypProvider.getAccountsSettings())
                        .displayNameSupported(false)
                        .keySupported(false)
                        .build())
                .build();

        ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.updateProviderRetryable(session
                                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), updateYp)).block();
        providersLoader.refreshCache();

        ErrorCollectionDto errors = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_ID,
                        YP_ID,
                        "ac_name",
                        null,
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .value(statusCode -> Assertions.assertEquals(statusCode, 422))
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(Collections.singleton("Account name is not supported in provider."),
                errors.getFieldErrors().get("accountName"));

        errors = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_ID,
                        YP_ID,
                        null,
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .value(statusCode -> Assertions.assertEquals(statusCode, 422))
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(Collections.singleton("Account key is not supported in provider."),
                errors.getFieldErrors().get("accountKey"));

        AccountDto accountDto = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_ID,
                        YP_ID,
                        null,
                        null,
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AccountDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertTrue(accountDto.getDisplayName() != null && !accountDto.getDisplayName().isEmpty());

        ProviderModel updateYp2 = ProviderModel.builder(ypProvider)
                .accountsSettings(AccountsSettingsModel.builder(ypProvider.getAccountsSettings())
                        .displayNameSupported(false)
                        .keySupported(true)
                        .build())
                .build();

        ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.updateProviderRetryable(session
                                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), updateYp2)).block();
        providersLoader.refreshCache();

        accountDto = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_ID,
                        YP_ID,
                        null,
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

        Assertions.assertEquals("outerKey", accountDto.getDisplayName());

        ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.updateProviderRetryable(session
                                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), ypProvider)).block();
        providersLoader.refreshCache();

        accountDto = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_ID,
                        YP_ID,
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

        Assertions.assertEquals("outerDisplayName", accountDto.getDisplayName());
    }

    @Test
    public void providersWithAccountsSpaceCanAcceptAccountsSpaceKeyForAccount() {
        ProviderModel ypProvider = ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        YP_ID, Tenants.DEFAULT_TENANT_ID)).block().get();

        ProviderModel updateYp = ProviderModel.builder(ypProvider)
                .accountsSpacesSupported(false)
                .build();

        ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.updateProviderRetryable(session
                                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), updateYp)).block();
        providersLoader.refreshCache();

        ErrorCollectionDto errors = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_ID,
                        YP_ID,
                        null,
                        null,
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .value(statusCode -> Assertions.assertEquals(statusCode, 422))
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(Collections.singleton("Accounts spaces is not supported in provider."),
                errors.getFieldErrors().get("accountsSpaceId"));

        ydbTableClient.usingSessionMonoRetryable(session ->
                providersDao.updateProviderRetryable(session
                                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), ypProvider)).block();
        providersLoader.refreshCache();

        errors = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_ID,
                        YP_ID,
                        "ac_name",
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId() + "invalid", null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(Set.of("Accounts space not found."), errors.getFieldErrors().get("accountsSpaceId"));

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_ID,
                        YP_ID,
                        "ac_name",
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AccountDto.class)
                .returnResult();
    }

    @Test
    public void accountResourcesCanBeFetched() {
        AvailableResourcesDto resources = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .get()
                .uri("/front/accounts/{accountId}/available-resources", TestAccounts.TEST_ACCOUNT_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AvailableResourcesDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(resources);
        Optional<AvailableResourcesDto.Resource> ypCpuMan =
                resources.getResources().stream().filter(resource -> resource.getId().equals(YP_CPU_MAN)).findFirst();
        Assertions.assertTrue(ypCpuMan.isPresent());
        Set<String> allowedUnitIds = ypCpuMan.get().getResourceUnits().getAllowedUnitIds();
        Assertions.assertTrue(allowedUnitIds.contains(CORES));
    }

    @Test
    public void accountResourcesMustFailOnInvalidAcccountId() {
        ErrorCollectionDto errorCollectionDto = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/accounts/{accountId}/available-resources", TestAccounts.TEST_ACCOUNT_1_ID + "invalid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(Collections.singleton("Account not found."), errorCollectionDto.getErrors());
    }

    @Test
    public void operationInProgressShouldBeKeeptIfErrorRetryible() {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse.failure(
                new StatusRuntimeException(Status.DEADLINE_EXCEEDED))));

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_ID,
                        YP_ID,
                        "ac_name",
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorCollectionDto.class);

        FolderOperationLogModel folderLog = ydbTableClient.usingSessionMonoRetryable(session -> folderOperationLogDao
                .getFirstPageByFolder(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID, TEST_FOLDER_1_ID, SortOrderDto.ASC, 100)).block()
                .stream()
                .filter(f -> f.getOperationType() == FolderOperationType.CREATE_ACCOUNT)
                .sorted(Comparator.comparing(FolderOperationLogModel::getOrder).reversed()).findFirst().get();

        String opId = folderLog.getAccountsQuotasOperationsId().orElse(null);
        Assertions.assertNotNull(opId);

        AccountsQuotasOperationsModel op = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasOperationsDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        opId, Tenants.DEFAULT_TENANT_ID)).block().get();

        AccountsQuotasOperationsModel.RequestStatus opStatus = op.getRequestStatus().get();
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.WAITING, opStatus);
        Assertions.assertTrue(op.getErrorMessage().isPresent());


        Optional<OperationInProgressModel> opInP = ydbTableClient.usingSessionMonoRetryable(session ->
                operationsInProgressDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        new OperationInProgressModel.Key(opId, TEST_FOLDER_1_ID),
                        Tenants.DEFAULT_TENANT_ID)).block();

        Assertions.assertTrue(opInP.isPresent());
    }

    private void operationInProgressShouldBeRemovedIfErrorNotRetryable(Status status) {
        stubProviderService.setCreateAccountResponses(List.of(GrpcResponse.failure(
                new StatusRuntimeException(status))));

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_ID,
                        YP_ID,
                        "ac_name",
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorCollectionDto.class);

        FolderOperationLogModel folderLog = ydbTableClient.usingSessionMonoRetryable(session -> folderOperationLogDao
                .getFirstPageByFolder(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID, TEST_FOLDER_1_ID, SortOrderDto.ASC, 100)).block()
                .stream()
                .filter(f -> f.getOperationType() == FolderOperationType.CREATE_ACCOUNT)
                .sorted(Comparator.comparing(FolderOperationLogModel::getOrder).reversed()).findFirst().get();

        String opId = folderLog.getAccountsQuotasOperationsId().orElse(null);
        Assertions.assertNotNull(opId);

        AccountsQuotasOperationsModel op = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasOperationsDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        opId, Tenants.DEFAULT_TENANT_ID)).block().get();

        AccountsQuotasOperationsModel.RequestStatus opStatus = op.getRequestStatus().get();
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.ERROR, opStatus);

        Optional<OperationInProgressModel> opInP = ydbTableClient.usingSessionMonoRetryable(session ->
                operationsInProgressDao.getById(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        new OperationInProgressModel.Key(opId, TEST_FOLDER_1_ID),
                        Tenants.DEFAULT_TENANT_ID)).block();

        Assertions.assertFalse(opInP.isPresent());
    }

    @Test
    public void operationInProgressShouldBeRemovedIfErrorNotRetryable1() {
        operationInProgressShouldBeRemovedIfErrorNotRetryable(Status.PERMISSION_DENIED);
    }

    @Test
    public void operationInProgressShouldBeRemovedIfErrorNotRetryable2() {
        operationInProgressShouldBeRemovedIfErrorNotRetryable(Status.UNKNOWN);
    }

    @Test
    public void accountCreationShouldBeSuccessIfWasErrorAndAfterDuplicateException() {
        stubProviderService.setCreateAccountResponses(List.of(
                GrpcResponse.failure(new StatusRuntimeException(Status.DEADLINE_EXCEEDED)),
                GrpcResponse.failure(new StatusRuntimeException(Status.ALREADY_EXISTS))
        ));

        stubProviderService.setListAccountsByFolderResponses(List.of(
                GrpcResponse.success(ListAccountsByFolderResponse.newBuilder()
                        .setNextPageToken(AccountsPageToken.newBuilder().setToken("test").build())
                        .addAccounts(Account.newBuilder()
                                .setAccountId("test")
                                .setDisplayName("ac_name")
                                .setDeleted(false)
                                .setFolderId(TEST_FOLDER_1_ID)
                                .setKey("ac_key")
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
                                                .setStaffLogin(StaffLogin.newBuilder()
                                                        .setStaffLogin(TestUsers.USER_1_LOGIN)
                                                        .build())
                                                .setPassportUid(PassportUID.newBuilder()
                                                        .setPassportUid(TestUsers.USER_1_UID)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build()),
                GrpcResponse.success(ListAccountsByFolderResponse.newBuilder().build())
        ));


        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_ID,
                        YP_ID,
                        "ac_name",
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void accountCreationRetryWithDefaultQuotasTest() {
        stubProviderService.setCreateAccountResponses(List.of(
                GrpcResponse.failure(new StatusRuntimeException(Status.DEADLINE_EXCEEDED)),
                GrpcResponse.failure(new StatusRuntimeException(Status.ALREADY_EXISTS))
        ));

        stubProviderService.setListAccountsByFolderResponses(List.of(
                GrpcResponse.success(ListAccountsByFolderResponse.newBuilder()
                        .setNextPageToken(AccountsPageToken.newBuilder().setToken("test").build())
                        .addAccounts(Account.newBuilder()
                                .setAccountId("test")
                                .setDisplayName("ac_name")
                                .setDeleted(false)
                                .setFolderId(TEST_FOLDER_1_ID)
                                .setKey("ac_key")
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
                                                .setStaffLogin(StaffLogin.newBuilder()
                                                        .setStaffLogin(TestUsers.USER_1_LOGIN)
                                                        .build())
                                                .setPassportUid(PassportUID.newBuilder()
                                                        .setPassportUid(TestUsers.USER_1_UID)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build()),
                GrpcResponse.success(ListAccountsByFolderResponse.newBuilder().build())
        ));


        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_ID,
                        YP_ID,
                        "ac_name",
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void accountCreationShouldCreateHistory() {
        final AccountDto account = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_ID,
                        YP_ID,
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

        final FrontFolderOperationLogPageDto history = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}", TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderOperationLogPageDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(history.getPage());

        List<FrontFolderOperationLogDto> items = history.getPage().getItems();

        FrontFolderOperationLogDto closeHistory = items.get(0);

        Assertions.assertEquals(OperationPhase.CLOSE, closeHistory.getOperationPhase());
        Assertions.assertNull(closeHistory.getOldAccounts());
        Assertions.assertEquals(1, closeHistory.getNewAccounts().getAccounts().size());
        FrontFolderOperationLogDto.Account logAccount = closeHistory.getNewAccounts()
                .getAccounts().get(account.getId());
        Assertions.assertNotNull(logAccount.getOuterAccountKeyInProvider());

        FrontFolderOperationLogDto submitHistory = items.get(1);

        Assertions.assertEquals(OperationPhase.SUBMIT, submitHistory.getOperationPhase());
        Assertions.assertNull(submitHistory.getOldAccounts());
        Assertions.assertEquals(1, submitHistory.getNewAccounts().getAccounts().size());
        logAccount = submitHistory.getNewAccounts().getAccounts().get(account.getId());
        Assertions.assertEquals("ac_key", logAccount.getOuterAccountKeyInProvider());

        Assertions.assertNotNull(account.getAccountsSpacesId());
        Assertions.assertEquals(account.getAccountsSpacesId(), logAccount.getAccountsSpaceId());
        Assertions.assertNotNull(history.getAccountsSpacesById().get(account.getAccountsSpacesId()));
    }

    @Test
    public void createAccountInClosingServiceTest() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_IN_CLOSING_SERVICE,
                        YP_ID,
                        "ac_name",
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("folderId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Current service status is not allowed."));
                });
    }

    @Test
    public void createAccountInNonExportableServiceTest() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_IN_NON_EXPORTABLE_SERVICE,
                        YP_ID,
                        "ac_name",
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("folderId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Services in the sandbox are not allowed."));
                });
    }

    @Test
    public void createAccountInRenamingServiceTest() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_IN_RENAMING_SERVICE,
                        YP_ID,
                        "ac_name",
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void createAccountFailOnEmptyAccountNameTest() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_IN_RENAMING_SERVICE,
                        YP_ID,
                        "",
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    Assertions.assertNotNull(result.getResponseBody());
                    Assertions.assertNotNull(result.getResponseBody().getErrors());
                    Assertions.assertTrue(
                            result.getResponseBody()
                                    .getFieldErrors()
                                    .getOrDefault("accountName", Set.of())
                                    .contains("Account name is required.")
                    );
                });
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_IN_RENAMING_SERVICE,
                        YP_ID,
                        "   ",
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    Assertions.assertNotNull(result.getResponseBody());
                    Assertions.assertNotNull(result.getResponseBody().getErrors());
                    Assertions.assertTrue(
                            result.getResponseBody()
                                    .getFieldErrors()
                                    .getOrDefault("accountName", Set.of())
                                    .contains("Account name is required.")
                    );
                });
    }

    @Test
    public void createAccountFailOnNullAccountNameTest() {
        // provider supports account names;
        // 4xx is expected if account == null
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_IN_RENAMING_SERVICE,
                        YP_ID,
                        null,
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(),
                        null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    Assertions.assertNotNull(result.getResponseBody());
                    Assertions.assertNotNull(result.getResponseBody().getErrors());
                    Assertions.assertTrue(
                            result.getResponseBody()
                                    .getFieldErrors()
                                    .getOrDefault("accountName", Set.of())
                                    .contains("Account name is required.")
                    );
                });
    }

    @Test
    public void createAccountSuccessOnNullAccountNameTest() {
        // provider doesn't support account names;
        // no errors expected if accountName == null
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_ID,
                        CLAUD2_ID,
                        null,
                        "ac_key",
                        null,
                        null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void accountShouldBeCreatedForProviderAdminTest() {
        AccountDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_ID,
                        YP_ID,
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
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
        Assertions.assertEquals(result.getFolderId(), TEST_FOLDER_1_ID);
    }

    @Test
    public void accountShouldNotBeCreatedWithoutPermissionsTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_ID,
                        YP_ID,
                        "ac_name",
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getErrors().isEmpty());
    }

    @Test
    public void accountShouldNotBeCreatedInReserveFolder() {
        final ErrorCollectionDto errors = LogCollector.collectLogs(() -> webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/accounts")
                .bodyValue(new FrontAccountInputDto(
                        TEST_FOLDER_1_RESERVE_ID,
                        YP_ID,
                        "ac_name",
                        "ac_key",
                        TEST_ACCOUNT_SPACE_1.getId(), null, null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody());
        Assertions.assertNotNull(errors);
        Assertions.assertTrue(errors.getFieldErrors()
                .get("folderId").contains("Account can not be created in a reserve folder."));
    }
}
