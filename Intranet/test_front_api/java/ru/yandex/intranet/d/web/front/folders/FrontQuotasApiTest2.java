package ru.yandex.intranet.d.web.front.folders;

import java.time.Instant;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.protobuf.util.Timestamps;
import com.yandex.ydb.table.transaction.TransactionMode;
import io.grpc.Metadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuple2;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestGrpcResponses;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.backend.service.provider_proto.Account;
import ru.yandex.intranet.d.backend.service.provider_proto.AccountsSpaceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.Amount;
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundAccountsSpaceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundResourceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.KnownAccountProvisions;
import ru.yandex.intranet.d.backend.service.provider_proto.KnownProvision;
import ru.yandex.intranet.d.backend.service.provider_proto.LastUpdate;
import ru.yandex.intranet.d.backend.service.provider_proto.PassportUID;
import ru.yandex.intranet.d.backend.service.provider_proto.Provision;
import ru.yandex.intranet.d.backend.service.provider_proto.ProvisionRequest;
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceSegmentKey;
import ru.yandex.intranet.d.backend.service.provider_proto.StaffLogin;
import ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionRequest;
import ru.yandex.intranet.d.backend.service.provider_proto.UserID;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsDao;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasOperationsDao;
import ru.yandex.intranet.d.dao.folders.FolderDao;
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao;
import ru.yandex.intranet.d.dao.quotas.QuotasDao;
import ru.yandex.intranet.d.dao.resources.ResourcesDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse;
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService;
import ru.yandex.intranet.d.model.accounts.AccountModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
import ru.yandex.intranet.d.model.folders.FolderOperationType;
import ru.yandex.intranet.d.model.folders.FolderType;
import ru.yandex.intranet.d.model.folders.OperationPhase;
import ru.yandex.intranet.d.model.folders.ProvisionHistoryModel;
import ru.yandex.intranet.d.model.folders.ProvisionsByResource;
import ru.yandex.intranet.d.model.folders.QuotasByAccount;
import ru.yandex.intranet.d.model.folders.QuotasByResource;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.model.resources.ResourceModel;
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel;
import ru.yandex.intranet.d.services.quotas.ProvisionService;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.SortOrderDto;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedAccount;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedAccountResource;
import ru.yandex.intranet.d.web.model.quotas.ProvisionLiteDto;
import ru.yandex.intranet.d.web.model.quotas.UpdateProvisionsAnswerDto;

import static java.util.Locale.ENGLISH;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_1;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_1_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_5_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_6;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_6_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_2_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_4_ID;
import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestResources.YDB_RAM_SAS;
import static ru.yandex.intranet.d.TestResources.YP_HDD_MAN;
import static ru.yandex.intranet.d.TestResources.YP_SSD_MAN;
import static ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_DISPENSER;
import static ru.yandex.intranet.d.TestUsers.USER_1_ID;
import static ru.yandex.intranet.d.TestUsers.USER_1_UID;
import static ru.yandex.intranet.d.UnitIds.GIGABYTES;
import static ru.yandex.intranet.d.UnitIds.MEBIBYTES;
import static ru.yandex.intranet.d.UnitIds.TERABYTES;
import static ru.yandex.intranet.d.web.front.folders.FrontQuotasApiTest.getBody;
import static ru.yandex.intranet.d.web.front.folders.FrontQuotasApiTest.prepareFailedPreconditionResponseTest;
import static ru.yandex.intranet.d.web.front.folders.FrontQuotasApiTest.prepareUpdateProvisionsOkResponseTest;
import static ru.yandex.intranet.d.web.front.folders.FrontQuotasApiTest.setUpUpdateAnswer;

/**
 * Tests for page /front/quotas
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@IntegrationTest
public class FrontQuotasApiTest2 {
    @Autowired
    private WebTestClient webClient;
    @Autowired
    private StubProviderService stubProviderService;
    @Autowired
    private FolderOperationLogDao folderOperationLogDao;
    @Autowired
    private YdbTableClient ydbTableClient;
    @Autowired
    private ProvisionService provisionService;
    @Autowired
    ResourcesDao resourcesDao;
    @Autowired
    private AccountsDao accountsDao;
    @Autowired
    private QuotasDao quotasDao;
    @Autowired
    private FolderDao folderDao;
    @Autowired
    @Qualifier("messageSource")
    private MessageSource messages;
    @Autowired
    private AccountsQuotasOperationsDao operationsDao;

    @Test
    public void updateProvisionsOkResponseWithoutAccountSpaceTest() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .success(ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionResponse.newBuilder()
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("ram")
                                                .addAllResourceSegmentKeys(List.of(
                                                        ResourceSegmentKey.newBuilder()
                                                                .setResourceSegmentationKey("segment")
                                                                .setResourceSegmentKey("default")
                                                                .build(),
                                                        ResourceSegmentKey.newBuilder()
                                                                .setResourceSegmentationKey("location")
                                                                .setResourceSegmentKey("sas")
                                                                .build()
                                                ))
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("mebibytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("mebibytes")
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .build())));

        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YDB_RAM_SAS, // resourceId
                        "1", // provided amount
                        MEBIBYTES, // provided amount unit key
                        "0", // old provided amount
                        MEBIBYTES));

        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody()
                        .setServiceId(12L)
                        .setFolderId(TEST_FOLDER_4_ID)
                        .setAccountId(TEST_ACCOUNT_5_ID)
                        .setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void updateProvisionsConvertProvisionsToProviderApiUnitTest() {
        setUpUpdateAnswer(stubProviderService);
        webClient
                .mutateWith(MockUser.uid(TestUsers.DISPENSER_QUOTA_MANAGER_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(Collections.singletonList(
                        new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                                "1", // provided amount
                                TERABYTES, // provided amount unit key
                                "200", // old provided amount
                                GIGABYTES))).build())
                .exchange()
                .expectStatus()
                .isOk();

        Deque<Tuple2<UpdateProvisionRequest, Metadata>> updateProvisionRequests =
                stubProviderService.getUpdateProvisionRequests();
        Assertions.assertEquals(1, updateProvisionRequests.size());
        Tuple2<UpdateProvisionRequest, Metadata> first = updateProvisionRequests.getFirst();
        Assertions.assertNotNull(first);

        UpdateProvisionRequest updateProvisionRequest = first.getT1();
        Assertions.assertNotNull(updateProvisionRequest);

        Assertions.assertEquals(TEST_ACCOUNT_1.getOuterAccountIdInProvider(), updateProvisionRequest.getAccountId());

        AccountsSpaceKey accountsSpaceKey = updateProvisionRequest.getAccountsSpaceKey();
        Assertions.assertNotNull(accountsSpaceKey);
        CompoundAccountsSpaceKey compoundKey = accountsSpaceKey.getCompoundKey();
        Assertions.assertNotNull(compoundKey);
        List<ResourceSegmentKey> resourceSegmentKeysList = compoundKey.getResourceSegmentKeysList();
        Assertions.assertNotNull(resourceSegmentKeysList);
        Assertions.assertEquals(2, resourceSegmentKeysList.size());
        Assertions.assertTrue(resourceSegmentKeysList.stream().anyMatch(
                s -> s.getResourceSegmentationKey().equals("segment")
                        && s.getResourceSegmentKey().equals("default")));
        Assertions.assertTrue(resourceSegmentKeysList.stream().anyMatch(
                s -> s.getResourceSegmentationKey().equals("location")
                        && s.getResourceSegmentKey().equals("man")));

        List<ProvisionRequest> updatedProvisionsList = updateProvisionRequest.getUpdatedProvisionsList();
        Assertions.assertNotNull(updatedProvisionsList);
        Assertions.assertEquals(Map.of(
                "hdd", ProvisionRequest.newBuilder()
                        .setResourceKey(ResourceKey.newBuilder().setCompoundKey(CompoundResourceKey.newBuilder()
                                .setResourceTypeKey("hdd").build()).build())
                        .setProvided(Amount.newBuilder().setValue(1000000000L).setUnitKey("kilobytes").build())
                        .build(),
                "ram", ProvisionRequest.newBuilder()
                        .setResourceKey(ResourceKey.newBuilder().setCompoundKey(CompoundResourceKey.newBuilder()
                                .setResourceTypeKey("ram").build()).build())
                        .setProvided(Amount.newBuilder().setValue(80L).setUnitKey("gigabytes").build())
                        .build()
        ), updatedProvisionsList.stream()
                .collect(Collectors.toMap(k -> k.getResourceKey().getCompoundKey().getResourceTypeKey(),
                        Function.identity())));

        List<KnownAccountProvisions> knownProvisionsList = updateProvisionRequest.getKnownProvisionsList();
        Assertions.assertNotNull(knownProvisionsList);
        Assertions.assertEquals(1, knownProvisionsList.size());
        Map<String, List<KnownAccountProvisions>> knownProvisionsByAccountMap = knownProvisionsList.stream()
                .collect(Collectors.groupingBy(KnownAccountProvisions::getAccountId, Collectors.toList()));
        List<KnownAccountProvisions> knownAccountProvisions1 = knownProvisionsByAccountMap.get(
                TEST_ACCOUNT_1.getOuterAccountIdInProvider());
        Assertions.assertEquals(1, knownAccountProvisions1.size());
        KnownAccountProvisions knownAccountProvisions = knownAccountProvisions1.get(0);
        Assertions.assertNotNull(knownAccountProvisions);
        Assertions.assertEquals(TEST_ACCOUNT_1.getOuterAccountIdInProvider(), knownAccountProvisions.getAccountId());
        List<KnownProvision> knownProvisionsList1 = knownAccountProvisions.getKnownProvisionsList();
        Assertions.assertNotNull(knownProvisionsList1);
        Assertions.assertEquals(Map.of(
                "hdd", KnownProvision.newBuilder()
                        .setResourceKey(ResourceKey.newBuilder().setCompoundKey(CompoundResourceKey.newBuilder()
                                .setResourceTypeKey("hdd").build()).build())
                        .setProvided(Amount.newBuilder().setValue(200000000L).setUnitKey("kilobytes").build())
                        .build(),
                "ram", KnownProvision.newBuilder()
                        .setResourceKey(ResourceKey.newBuilder().setCompoundKey(CompoundResourceKey.newBuilder()
                                .setResourceTypeKey("ram").build()).build())
                        .setProvided(Amount.newBuilder().setValue(80L).setUnitKey("gigabytes").build())
                        .build()
        ), knownProvisionsList1.stream()
                .collect(Collectors.toMap(k -> k.getResourceKey().getCompoundKey().getResourceTypeKey(),
                        Function.identity())));
    }

    @Test
    public void updateProvisionsConflictResponseGetAccountFailTest() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(TestGrpcResponses.alreadyExistsTestResponse()));

        stubProviderService.setGetAccountResponses(List.of(TestGrpcResponses.alreadyExistsTestResponse()));

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
        ErrorCollectionDto errorCollectionDto = webClient
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

        Assertions.assertNotNull(errorCollectionDto);
        Set<String> errors = errorCollectionDto.getErrors();
        Assertions.assertNotNull(errors);
        Assertions.assertEquals(1, errors.size());
        Assertions.assertEquals(messages.getMessage("errors.provision.update.scheduled", null, ENGLISH),
                errors.iterator().next());

        long updateProvisionCallCount = stubProviderService.getUpdateProvisionCallCount();
        Assertions.assertEquals(1, updateProvisionCallCount);

        long getAccountCallCount = stubProviderService.getGetAccountCallCount();
        Assertions.assertEquals(1, getAccountCallCount);
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void updateProvisionsOkResponseHistoryTest() {
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
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk();

        List<FolderOperationLogModel> folderOperationLogModels = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> folderOperationLogDao.getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID,
                                TestFolders.TEST_FOLDER_1_ID, SortOrderDto.ASC, 100)
                )
        ).block();
        AccountsQuotasOperationsModel operation = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> operationsDao.getById(txSession, folderOperationLogModels.get(1)
                                .getAccountsQuotasOperationsId().orElseThrow(), Tenants.DEFAULT_TENANT_ID)
                )
        ).block().orElseThrow();

        Assertions.assertNotNull(folderOperationLogModels);
        Assertions.assertEquals(3, folderOperationLogModels.size());

        FolderOperationLogModel folderOperationLogModel = folderOperationLogModels.get(1);
        Assertions.assertNotNull(folderOperationLogModel);

        Assertions.assertEquals(operation.getCreateDateTime(),
                folderOperationLogModel.getOperationDateTime());
        Assertions.assertEquals(operation.getLastRequestId(),
                folderOperationLogModel.getProviderRequestId());
        Assertions.assertEquals(FolderOperationType.PROVIDE_REVOKE_QUOTAS_TO_ACCOUNT,
                folderOperationLogModel.getOperationType());
        Assertions.assertEquals(USER_1_ID, folderOperationLogModel.getAuthorUserId().orElseThrow());
        Assertions.assertEquals(USER_1_UID, folderOperationLogModel.getAuthorUserUid().orElseThrow());
        Assertions.assertNull(folderOperationLogModel.getAuthorProviderId().orElse(null));
        Assertions.assertNull(folderOperationLogModel.getSourceFolderOperationsLogId().orElse(null));
        Assertions.assertNull(folderOperationLogModel.getDestinationFolderOperationsLogId().orElse(null));
        Assertions.assertNull(folderOperationLogModel.getOldFolderFields().orElse(null));
        Assertions.assertEquals(new QuotasByResource(
                Map.of(
                        YP_HDD_MAN, 1000000000000L,
                        YP_SSD_MAN, 2000000000000L
                )
        ), folderOperationLogModel.getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(
                Map.of(
                        YP_HDD_MAN, 800000000000L,
                        YP_SSD_MAN, 2000000000000L
                )
        ), folderOperationLogModel.getOldBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(
                TEST_ACCOUNT_1_ID, new ProvisionsByResource(Map.of(
                        YP_HDD_MAN, new ProvisionHistoryModel(200000000000L, 1L)))
        )), folderOperationLogModel.getOldProvisions());
        Assertions.assertNull(folderOperationLogModel.getOldAccounts().orElse(null));
        Assertions.assertNull(folderOperationLogModel.getNewFolderFields().orElse(null));
        Assertions.assertEquals(new QuotasByResource(
                Map.of(
                        YP_HDD_MAN, 1000000000000L,
                        YP_SSD_MAN, 2000000000000L
                )
        ), folderOperationLogModel.getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(
                Map.of(
                        YP_HDD_MAN, 800000000000L,
                        YP_SSD_MAN, 1990000000000L
                )
        ), folderOperationLogModel.getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(TEST_ACCOUNT_1_ID, new ProvisionsByResource(
                Map.of(
                        YP_HDD_MAN, new ProvisionHistoryModel(100000000000L, 2L),
                        YP_SSD_MAN, new ProvisionHistoryModel(10000000000L, 0L)
                )))
        ), folderOperationLogModel.getNewProvisions());
        Assertions.assertNull(folderOperationLogModel.getActuallyAppliedProvisions().orElse(null));
        Assertions.assertNull(folderOperationLogModel.getNewAccounts().orElse(null));
        Assertions.assertEquals(operation.getOperationId(),
                folderOperationLogModel.getAccountsQuotasOperationsId().orElse(null));
        Assertions.assertNull(folderOperationLogModel.getQuotasDemandsId().orElse(null));
        Assertions.assertEquals(OperationPhase.SUBMIT, folderOperationLogModel.getOperationPhase().orElseThrow());
        Assertions.assertEquals(operation.getOrders().getSubmitOrder(),
                folderOperationLogModel.getOrder());

        folderOperationLogModel = folderOperationLogModels.get(2);
        Assertions.assertNotNull(folderOperationLogModel);

        Assertions.assertEquals(operation.getUpdateDateTime().orElseThrow(),
                folderOperationLogModel.getOperationDateTime());
        Assertions.assertEquals(operation.getLastRequestId(),
                folderOperationLogModel.getProviderRequestId());
        Assertions.assertEquals(FolderOperationType.PROVIDE_REVOKE_QUOTAS_TO_ACCOUNT,
                folderOperationLogModel.getOperationType());
        Assertions.assertEquals(USER_1_ID, folderOperationLogModel.getAuthorUserId().orElseThrow());
        Assertions.assertEquals(USER_1_UID, folderOperationLogModel.getAuthorUserUid().orElseThrow());
        Assertions.assertNull(folderOperationLogModel.getAuthorProviderId().orElse(null));
        Assertions.assertNull(folderOperationLogModel.getSourceFolderOperationsLogId().orElse(null));
        Assertions.assertNull(folderOperationLogModel.getDestinationFolderOperationsLogId().orElse(null));
        Assertions.assertNull(folderOperationLogModel.getOldFolderFields().orElse(null));
        Assertions.assertEquals(new QuotasByResource(
                Map.of(
                        YP_HDD_MAN, 1000000000000L,
                        YP_SSD_MAN, 2000000000000L
                )
        ), folderOperationLogModel.getOldQuotas());
        Assertions.assertEquals(new QuotasByResource(
                Map.of(
                        YP_HDD_MAN, 800000000000L,
                        YP_SSD_MAN, 2000000000000L
                )
        ), folderOperationLogModel.getOldBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(
                TEST_ACCOUNT_1_ID, new ProvisionsByResource(Map.of(
                        YP_HDD_MAN, new ProvisionHistoryModel(200000000000L, 1L)
                ))
        )), folderOperationLogModel.getOldProvisions());
        Assertions.assertNull(folderOperationLogModel.getOldAccounts().orElse(null));
        Assertions.assertNull(folderOperationLogModel.getNewFolderFields().orElse(null));
        Assertions.assertEquals(new QuotasByResource(
                Map.of(
                        YP_HDD_MAN, 1000000000000L,
                        YP_SSD_MAN, 2000000000000L
                )
        ), folderOperationLogModel.getNewQuotas());
        Assertions.assertEquals(new QuotasByResource(
                Map.of(
                        YP_HDD_MAN, 900000000000L,
                        YP_SSD_MAN, 1990000000000L
                )
        ), folderOperationLogModel.getNewBalance());
        Assertions.assertEquals(new QuotasByAccount(Map.of(TEST_ACCOUNT_1_ID, new ProvisionsByResource(
                Map.of(
                        YP_HDD_MAN, new ProvisionHistoryModel(100000000000L, 2L),
                        YP_SSD_MAN, new ProvisionHistoryModel(10000000000L, 0L)
                )))
        ), folderOperationLogModel.getNewProvisions());
        Assertions.assertEquals(new QuotasByAccount(Map.of(TEST_ACCOUNT_1_ID, new ProvisionsByResource(
                Map.of(YP_HDD_MAN, new ProvisionHistoryModel(100000000000L, null),
                        YP_SSD_MAN, new ProvisionHistoryModel(10000000000L, null)
                )))
        ), folderOperationLogModel.getActuallyAppliedProvisions().orElseThrow());
        Assertions.assertNull(folderOperationLogModel.getNewAccounts().orElse(null));
        Assertions.assertEquals(operation.getOperationId(),
                folderOperationLogModel.getAccountsQuotasOperationsId().orElse(null));
        Assertions.assertNull(folderOperationLogModel.getQuotasDemandsId().orElse(null));
        Assertions.assertEquals(OperationPhase.CLOSE, folderOperationLogModel.getOperationPhase().orElseThrow());
        Assertions.assertEquals(operation.getOrders().getCloseOrder().orElseThrow(),
                folderOperationLogModel.getOrder());
    }

    @Test
    public void updateProvisionsKnownProvisionAndUpdateProvisionTest() {
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
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk();

        Deque<Tuple2<UpdateProvisionRequest, Metadata>> updateProvisionRequests =
                stubProviderService.getUpdateProvisionRequests();
        Assertions.assertEquals(1, updateProvisionRequests.size());
        Tuple2<UpdateProvisionRequest, Metadata> first = updateProvisionRequests.getFirst();
        Assertions.assertNotNull(first);

        UpdateProvisionRequest updateProvisionRequest = first.getT1();
        Assertions.assertNotNull(updateProvisionRequest);

        Assertions.assertEquals(TEST_ACCOUNT_1.getOuterAccountIdInProvider(), updateProvisionRequest.getAccountId());

        AccountsSpaceKey accountsSpaceKey = updateProvisionRequest.getAccountsSpaceKey();
        Assertions.assertNotNull(accountsSpaceKey);
        CompoundAccountsSpaceKey compoundKey = accountsSpaceKey.getCompoundKey();
        Assertions.assertNotNull(compoundKey);
        List<ResourceSegmentKey> resourceSegmentKeysList = compoundKey.getResourceSegmentKeysList();
        Assertions.assertNotNull(resourceSegmentKeysList);
        Assertions.assertEquals(2, resourceSegmentKeysList.size());
        Assertions.assertTrue(resourceSegmentKeysList.stream().anyMatch(
                s -> s.getResourceSegmentationKey().equals("segment")
                        && s.getResourceSegmentKey().equals("default")));
        Assertions.assertTrue(resourceSegmentKeysList.stream().anyMatch(
                s -> s.getResourceSegmentationKey().equals("location")
                        && s.getResourceSegmentKey().equals("man")));

        List<ProvisionRequest> updatedProvisionsList = updateProvisionRequest.getUpdatedProvisionsList();
        Assertions.assertNotNull(updatedProvisionsList);
        Assertions.assertEquals(3, updatedProvisionsList.size());
        Assertions.assertEquals(
                List.of(
                        ProvisionRequest.newBuilder()
                                .setProvided(Amount.newBuilder().setValue(80L).setUnitKey("gigabytes").build())
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder().setResourceTypeKey("ram")
                                                .build())
                                        .build())
                                .build(),
                        ProvisionRequest.newBuilder()
                                .setProvided(Amount.newBuilder().setValue(100000000L).setUnitKey("kilobytes").build())
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder().setResourceTypeKey("hdd")
                                                .build())
                                        .build())
                                .build(),
                        ProvisionRequest.newBuilder()
                                .setProvided(Amount.newBuilder().setValue(10L).setUnitKey("gigabytes").build())
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder().setResourceTypeKey("ssd")
                                                .build())
                                        .build())
                                .build()
                ),
                updatedProvisionsList);

        List<KnownAccountProvisions> knownProvisionsList = updateProvisionRequest.getKnownProvisionsList();
        Assertions.assertNotNull(knownProvisionsList);
        Assertions.assertEquals(1, knownProvisionsList.size());
        Map<String, List<KnownAccountProvisions>> knownProvisionsByAccountMap = knownProvisionsList.stream()
                .collect(Collectors.groupingBy(KnownAccountProvisions::getAccountId, Collectors.toList()));
        List<KnownAccountProvisions> knownAccountProvisions1 = knownProvisionsByAccountMap.get(
                TEST_ACCOUNT_1.getOuterAccountIdInProvider());
        Assertions.assertEquals(1, knownAccountProvisions1.size());
        KnownAccountProvisions knownAccountProvisions = knownAccountProvisions1.get(0);
        Assertions.assertNotNull(knownAccountProvisions);
        Assertions.assertEquals(TEST_ACCOUNT_1.getOuterAccountIdInProvider(), knownAccountProvisions.getAccountId());
        List<KnownProvision> knownProvisionsList1 = knownAccountProvisions.getKnownProvisionsList();
        Assertions.assertNotNull(knownProvisionsList1);
        Assertions.assertEquals(3, knownProvisionsList1.size());
        Assertions.assertEquals(List.of(
                KnownProvision.newBuilder()
                        .setResourceKey(ResourceKey.newBuilder().setCompoundKey(CompoundResourceKey.newBuilder()
                                .setResourceTypeKey("ram").build()).build())
                        .setProvided(Amount.newBuilder().setValue(80L).setUnitKey("gigabytes").build())
                        .build(),
                KnownProvision.newBuilder()
                        .setResourceKey(ResourceKey.newBuilder().setCompoundKey(CompoundResourceKey.newBuilder()
                                .setResourceTypeKey("hdd").build()).build())
                        .setProvided(Amount.newBuilder().setValue(200000000L).setUnitKey("kilobytes").build())
                        .build(),
                KnownProvision.newBuilder()
                        .setResourceKey(ResourceKey.newBuilder().setCompoundKey(CompoundResourceKey.newBuilder()
                                .setResourceTypeKey("ssd").build()).build())
                        .setProvided(Amount.newBuilder().setValue(0L).setUnitKey("gigabytes").build())
                        .build()
        ), knownProvisionsList1);
    }

    public void prepareUpdateProvisionsForKnownProvisionTest() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .success(ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionResponse.newBuilder()
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("hdd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(100)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("gigabytes")
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())

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
                        .build())));
    }

    @Test
    public void updateProvisionsKnownProvisionOnEmptyAccountTest() {
        prepareUpdateProvisionsForKnownProvisionTest();
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setFolderId(TEST_FOLDER_2_ID)
                        .setAccountId(TEST_ACCOUNT_6_ID).setUpdatedProvisions(Collections.singletonList(
                                new ProvisionLiteDto(YP_HDD_MAN,
                                        "1",
                                        GIGABYTES,
                                        "0",
                                        GIGABYTES))).build())
                .exchange()
                .expectStatus()
                .isOk();

        Deque<Tuple2<UpdateProvisionRequest, Metadata>> updateProvisionRequests =
                stubProviderService.getUpdateProvisionRequests();
        Assertions.assertEquals(1, updateProvisionRequests.size());
        Tuple2<UpdateProvisionRequest, Metadata> first = updateProvisionRequests.getFirst();
        Assertions.assertNotNull(first);

        UpdateProvisionRequest updateProvisionRequest = first.getT1();
        Assertions.assertNotNull(updateProvisionRequest);

        Assertions.assertEquals(TEST_ACCOUNT_6.getOuterAccountIdInProvider(), updateProvisionRequest.getAccountId());

        AccountsSpaceKey accountsSpaceKey = updateProvisionRequest.getAccountsSpaceKey();
        Assertions.assertNotNull(accountsSpaceKey);
        CompoundAccountsSpaceKey compoundKey = accountsSpaceKey.getCompoundKey();
        Assertions.assertNotNull(compoundKey);
        List<ResourceSegmentKey> resourceSegmentKeysList = compoundKey.getResourceSegmentKeysList();
        Assertions.assertNotNull(resourceSegmentKeysList);
        Assertions.assertEquals(2, resourceSegmentKeysList.size());
        Assertions.assertTrue(resourceSegmentKeysList.stream().anyMatch(
                s -> s.getResourceSegmentationKey().equals("segment")
                        && s.getResourceSegmentKey().equals("default")));
        Assertions.assertTrue(resourceSegmentKeysList.stream().anyMatch(
                s -> s.getResourceSegmentationKey().equals("location")
                        && s.getResourceSegmentKey().equals("man")));

        List<ProvisionRequest> updatedProvisionsList = updateProvisionRequest.getUpdatedProvisionsList();
        Assertions.assertNotNull(updatedProvisionsList);
        Assertions.assertEquals(1, updatedProvisionsList.size());
        Assertions.assertEquals(List.of(ProvisionRequest.newBuilder()
                        .setProvided(Amount.newBuilder().setValue(1000000L).setUnitKey("kilobytes").build())
                        .setResourceKey(ResourceKey.newBuilder()
                                .setCompoundKey(CompoundResourceKey.newBuilder().setResourceTypeKey("hdd")
                                        .build())
                                .build())
                        .build()),
                updatedProvisionsList);

        List<KnownAccountProvisions> knownProvisionsList = updateProvisionRequest.getKnownProvisionsList();
        Assertions.assertNotNull(knownProvisionsList);
        Assertions.assertEquals(1, knownProvisionsList.size());
        KnownAccountProvisions knownAccountProvisions = knownProvisionsList.get(0);
        Assertions.assertNotNull(knownAccountProvisions);
        Assertions.assertEquals(TEST_ACCOUNT_6.getOuterAccountIdInProvider(), knownAccountProvisions.getAccountId());
        List<KnownProvision> knownProvisionsList1 = knownAccountProvisions.getKnownProvisionsList();
        Assertions.assertNotNull(knownProvisionsList1);
        Assertions.assertEquals(1, knownProvisionsList1.size());
        Assertions.assertEquals(List.of(KnownProvision.newBuilder()
                .setResourceKey(ResourceKey.newBuilder().setCompoundKey(CompoundResourceKey.newBuilder()
                        .setResourceTypeKey("hdd").build()).build())
                .setProvided(Amount.newBuilder().setValue(0L).setUnitKey("kilobytes").build())
                .build()
        ), knownProvisionsList1);
    }

    public void prepareUpdateProvisionsForResponseWithoutTimestampTest() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .success(ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionResponse.newBuilder()
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("hdd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(100)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .build())
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
                        .build())));
    }

    @Test
    public void updateProvisionsResponseWithoutTimestampTest() {
        prepareUpdateProvisionsForResponseWithoutTimestampTest();
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setFolderId(TEST_FOLDER_2_ID)
                        .setAccountId(TEST_ACCOUNT_6_ID).setUpdatedProvisions(Collections.singletonList(
                                new ProvisionLiteDto(YP_HDD_MAN,
                                        "1",
                                        GIGABYTES,
                                        "0",
                                        GIGABYTES))).build())
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void updateProvisionsFreezingQuotaCalculatingTest() {
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
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk();

        List<QuotaModel> quotaModels = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> quotasDao.getByIds(txSession,
                                List.of(new QuotaModel.Key(TestFolders.TEST_FOLDER_1_ID, TestProviders.YP_ID,
                                                TestResources.YP_HDD_MAN),
                                        new QuotaModel.Key(TestFolders.TEST_FOLDER_1_ID, TestProviders.YP_ID,
                                                TestResources.YP_SSD_MAN)),
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
                        .balance(900000000000L)
                        .frozenQuota(0L)
                        .build(),
                QuotaModel.builder()
                        .tenantId(Tenants.DEFAULT_TENANT_ID)
                        .folderId(TEST_FOLDER_1_ID)
                        .providerId(YP_ID)
                        .resourceId(YP_SSD_MAN)
                        .quota(2000000000000L)
                        .balance(1990000000000L)
                        .frozenQuota(0L)
                        .build()
        ), quotaModels);
    }

    public void prepareUpdateProvisionsResponseWithDifferentAnswerTest() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .success(ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionResponse.newBuilder()
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("hdd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(120)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(100)
                                        .setUnitKey("gigabytes")
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("ssd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(8)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("gigabytes")
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("cpu")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("millicores")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("millicores")
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
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
                        .build())));
    }

    @Test
    public void updateProvisionsFreezingQuotaCalculatingOnDifferentProviderAnswerTest() {
        prepareUpdateProvisionsResponseWithDifferentAnswerTest();
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
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk();

        List<QuotaModel> quotaModels = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> quotasDao.getByIds(txSession,
                                List.of(new QuotaModel.Key(TestFolders.TEST_FOLDER_1_ID, TestProviders.YP_ID,
                                                TestResources.YP_HDD_MAN),
                                        new QuotaModel.Key(TestFolders.TEST_FOLDER_1_ID, TestProviders.YP_ID,
                                                TestResources.YP_SSD_MAN)),
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
                        .balance(880000000000L)
                        .frozenQuota(0L)
                        .build(),
                QuotaModel.builder()
                        .tenantId(Tenants.DEFAULT_TENANT_ID)
                        .folderId(TEST_FOLDER_1_ID)
                        .providerId(YP_ID)
                        .resourceId(YP_SSD_MAN)
                        .quota(2000000000000L)
                        .balance(1992000000000L)
                        .frozenQuota(0L)
                        .build()
        ), quotaModels);
    }

    @Test
    public void updateProvisionsFreezingQuotaCalculatingOnFailureProviderAnswerTest() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(TestGrpcResponses.alreadyExistsTestResponse()));

        stubProviderService.setGetAccountResponses(List.of(GrpcResponse
                .success(Account.newBuilder()
                        .setAccountId(TEST_ACCOUNT_1_ID)
                        .setDeleted(false)
                        .setDisplayName("test")
                        .setFolderId(TEST_FOLDER_1_ID)
                        .setKey("test")
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
                                        .setValue(200)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(100)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .build())
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
                                                        .build())
                                        ))
                                .build())
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
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .is4xxClientError();

        List<QuotaModel> quotaModels = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> quotasDao.getByIds(txSession,
                                List.of(new QuotaModel.Key(TestFolders.TEST_FOLDER_1_ID, TestProviders.YP_ID,
                                                TestResources.YP_HDD_MAN),
                                        new QuotaModel.Key(TestFolders.TEST_FOLDER_1_ID, TestProviders.YP_ID,
                                                TestResources.YP_SSD_MAN)),
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
                        .build(),
                QuotaModel.builder()
                        .tenantId(Tenants.DEFAULT_TENANT_ID)
                        .folderId(TEST_FOLDER_1_ID)
                        .providerId(YP_ID)
                        .resourceId(YP_SSD_MAN)
                        .quota(2000000000000L)
                        .balance(2000000000000L)
                        .frozenQuota(0L)
                        .build()
        ), quotaModels);
    }

    @Test
    public void updateProvisionsActualAccountQuotaCalculatingTest() {
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
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk();
    }

    /**
     * Update provisions balance calculating test.
     *
     * @see ru.yandex.intranet.d.web.controllers.front.FrontQuotasController#updateProvision
     */
    @Test
    public void updateProvisionsBalanceCalculatingTest() {
        prepareUpdateProvisionsOkResponseTest(stubProviderService);
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_SSD_MAN, // resourceId
                        "10", // provided amount
                        GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        GIGABYTES));
        UpdateProvisionsAnswerDto answer = webClient
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
                .getResponseBody();
        Assertions.assertNotNull(answer);
        Optional<ExpandedAccount> answerAccount = answer.getExpandedProvider().getAccounts().stream()
                .filter(expandedAccount -> expandedAccount.getAccount().getId().equals(TEST_ACCOUNT_1_ID))
                .findFirst();
        Assertions.assertTrue(answerAccount.isPresent());
        Optional<ExpandedAccountResource> answerYpSsdMan = answerAccount.get().getResources().stream()
                .filter(expandedAccountResource -> expandedAccountResource.getResourceId().equals(YP_SSD_MAN))
                .findFirst();
        Assertions.assertTrue(answerYpSsdMan.isPresent());
        Optional<ExpandedAccountResource> answerYpHddMan = answerAccount.get().getResources().stream()
                .filter(expandedAccountResource -> expandedAccountResource.getResourceId().equals(YP_HDD_MAN))
                .findFirst();
        Assertions.assertTrue(answerYpHddMan.isEmpty(), "Unmodified resources should not be returned");
    }

    @Test
    public void updateProvisionsConflictResponseWithNewResourcesCompletedTest() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(TestGrpcResponses.alreadyExistsTestResponse()));

        stubProviderService.setGetAccountResponses(List.of(GrpcResponse
                .success(Account.newBuilder()
                        .setAccountId(TEST_ACCOUNT_1_ID)
                        .setDeleted(false)
                        .setDisplayName("test")
                        .setFolderId(TEST_FOLDER_1_ID)
                        .setKey("test")
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
                                        .setUnitKey("gigabytes")
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("ssd")
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
                                        .setValue(10)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("cpu_new")
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
                                        .setValue(1)
                                        .setUnitKey("millicores")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("millicores")
                                        .build())
                                .build())
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
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk();

        long updateProvisionCallCount = stubProviderService.getUpdateProvisionCallCount();
        Assertions.assertEquals(1, updateProvisionCallCount);

        long getAccountCallCount = stubProviderService.getGetAccountCallCount();
        Assertions.assertEquals(1, getAccountCallCount);
    }

    @Test
    public void updateProvisionsUnknownResourceResponseTest() {
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
                                        .setUnitKey("gigabytes")
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("ssd")
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
                                        .setValue(10)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("cpu_new")
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
                                        .setValue(1)
                                        .setUnitKey("millicores")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("millicores")
                                        .build())
                                .build())
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
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk();

        long updateProvisionCallCount = stubProviderService.getUpdateProvisionCallCount();
        Assertions.assertEquals(1, updateProvisionCallCount);
    }

    @SuppressWarnings("SameParameterValue")
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

    @Test
    @SuppressWarnings("MethodLength")
    public void updateProvisionsWithNotUniqueUnitKeyTest() {
        FolderModel folder = FolderModel.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setServiceId(TEST_SERVICE_ID_DISPENSER)
                .setVersion(0L)
                .setDisplayName("Test")
                .setDescription("Test")
                .setDeleted(false)
                .setFolderType(FolderType.COMMON)
                .setTags(Collections.emptySet())
                .setNextOpLogOrder(2L)
                .build();

        ResourceModel resource = ResourceModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .key("yp_hdd")
                .nameEn("Test")
                .nameRu("Test")
                .descriptionEn("Test")
                .descriptionRu("Test")
                .deleted(false)
                .unitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                .providerId(YP_ID)
                .resourceTypeId("44f93060-e367-44e6-b069-98c20d03dd81")
                .segments(null)
                .resourceUnits(new ResourceUnitsModel(Set.of("b15101c2-da50-4d6f-9a8e-b90160871b0a",
                        "74fe1983-144a-4156-8839-aa791cc2deb6", "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5",
                        "d1456370-c8bb-442f-8ded-b2c96afacb3c"), "b15101c2-da50-4d6f-9a8e-b90160871b0a", null))
                .managed(true)
                .orderable(true)
                .readOnly(false)
                .baseUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                .accountsSpacesId(null)
                .build();

        AccountModel account = new AccountModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setId(UUID.randomUUID().toString())
                .setVersion(0L)
                .setProviderId(YP_ID)
                .setAccountsSpacesId(null)
                .setOuterAccountIdInProvider("test-id")
                .setOuterAccountKeyInProvider("test")
                .setFolderId(folder.getId())
                .setDisplayName("Test")
                .setDeleted(false)
                .setLastAccountUpdate(Instant.now())
                .setLastReceivedVersion(null)
                .setLatestSuccessfulAccountOperationId(null)
                .setDeleted(false)
                .build();

        QuotaModel quota1 = quotaModel(YP_ID, resource.getId(), folder.getId(), 300L, 300L, 0L);
        QuotaModel quota2 = quotaModel(YP_ID, "c778d9f1-489d-4100-a057-309bdfab81d0", folder.getId(), 300000000L,
                300000000L, 0L);

        ydbTableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
                .block();
        ydbTableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourcesDao
                        .upsertResourceRetryable(txSession, resource)))
                .block();
        Optional<ResourceModel> block =
                ydbTableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourcesDao
                                .getById(txSession, "c778d9f1-489d-4100-a057-309bdfab81d0", Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Assertions.assertTrue(block != null && block.isPresent());
        ydbTableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourcesDao
                        .upsertResourceRetryable(txSession, ResourceModel.builder(block.get())
                                .deleted(false)
                                .build())))
                .block();
        ydbTableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .upsertOneRetryable(txSession, account)))
                .block();
        ydbTableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .upsertAllRetryable(txSession, List.of(quota1, quota2))))
                .block();

        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .success(ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionResponse.newBuilder()
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("hdd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("bytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("bytes")
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("ram")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("mebibytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("mebibytes")
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
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .build())));

        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(resource.getId(), // resourceId
                        "1", // provided amount
                        "b15101c2-da50-4d6f-9a8e-b90160871b0a", // provided amount unit key
                        "0", // old provided amount
                        "b15101c2-da50-4d6f-9a8e-b90160871b0a"),
                new ProvisionLiteDto("c778d9f1-489d-4100-a057-309bdfab81d0", // resourceId
                        "1", // provided amount
                        "68705acb-bc62-43bc-b54f-fba490776fb9", // provided amount unit key
                        "0", // old provided amount
                        "68705acb-bc62-43bc-b54f-fba490776fb9")
        );

        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody()
                        .setAccountId(account.getId())
                        .setFolderId(folder.getId())
                        .setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk();
    }

    //    @Test
    public void updateProvisions5xxResponseTest() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(TestGrpcResponses.internalTestResponse()));

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
        ErrorCollectionDto errorCollectionDto = webClient
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

        Assertions.assertNotNull(errorCollectionDto);
        System.out.println(errorCollectionDto);
        Set<String> errors = errorCollectionDto.getErrors();
        Assertions.assertNotNull(errors);
        Assertions.assertEquals(1, errors.size());
        Assertions.assertEquals(messages.getMessage("errors.provision.update.scheduled", null, ENGLISH),
                errors.iterator().next());

        long updateProvisionCallCount = stubProviderService.getUpdateProvisionCallCount();
        Assertions.assertEquals(4 * 3, updateProvisionCallCount);
    }

    //    @Test
    public void updateProvisions5xxRestoredResponseTest() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(
                TestGrpcResponses.internalTestResponse(),
                TestGrpcResponses.internalTestResponse(),
                TestGrpcResponses.internalTestResponse(),
                GrpcResponse
                        .success(ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionResponse
                                .newBuilder()
                                .addProvisions(Provision.newBuilder()
                                        .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("hdd")
                                                        .build())
                                                .build())
                                        .setProvided(Amount.newBuilder()
                                                .setValue(100)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .setAllocated(Amount.newBuilder()
                                                .setValue(100)
                                                .setUnitKey("gigabytes")
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
                                                .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                                .build())
                                        .build())
                                .addProvisions(Provision.newBuilder()
                                        .setResourceKey(ResourceKey.newBuilder()
                                                .setCompoundKey(CompoundResourceKey.newBuilder()
                                                        .setResourceTypeKey("ssd")
                                                        .build())
                                                .build())
                                        .setProvided(Amount.newBuilder()
                                                .setValue(10)
                                                .setUnitKey("gigabytes")
                                                .build())
                                        .setAllocated(Amount.newBuilder()
                                                .setValue(0)
                                                .setUnitKey("gigabytes")
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
                                                .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                                .build())
                                        .build())
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
                                .build())
        ));

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
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk();

        long updateProvisionCallCount = stubProviderService.getUpdateProvisionCallCount();
        Assertions.assertEquals(4, updateProvisionCallCount);
    }

    @Test
    public void updateProvisionsFailedPreconditionAndProvideLessThanAllocatedResponseTest() {
        prepareFailedPreconditionResponseTest(stubProviderService);
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "0", // provided amount
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
                .isBadRequest()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(
                Set.of("Error when changed quota: the provided quota cannot be less than the allocated quota"),
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

}
