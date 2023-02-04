package ru.yandex.intranet.d.web.front.transfer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestServices;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.folders.FolderDao;
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao;
import ru.yandex.intranet.d.dao.providers.ProvidersDao;
import ru.yandex.intranet.d.dao.quotas.QuotasDao;
import ru.yandex.intranet.d.dao.resources.ResourcesDao;
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao;
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao;
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao;
import ru.yandex.intranet.d.dao.services.ServicesDao;
import ru.yandex.intranet.d.dao.transfers.PendingTransferRequestsDao;
import ru.yandex.intranet.d.dao.transfers.TransferRequestsDao;
import ru.yandex.intranet.d.dao.users.AbcServiceMemberDao;
import ru.yandex.intranet.d.dao.users.UsersDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.model.resources.ResourceModel;
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel;
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel;
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel;
import ru.yandex.intranet.d.model.transfers.PendingTransferRequestsModel;
import ru.yandex.intranet.d.model.transfers.TransferNotified;
import ru.yandex.intranet.d.model.transfers.TransferRequestModel;
import ru.yandex.intranet.d.model.transfers.TransferRequestStatus;
import ru.yandex.intranet.d.model.users.AbcServiceMemberModel;
import ru.yandex.intranet.d.model.users.AbcServiceMemberState;
import ru.yandex.intranet.d.model.users.StaffAffiliation;
import ru.yandex.intranet.d.model.users.UserModel;
import ru.yandex.intranet.d.model.users.UserServiceRoles;
import ru.yandex.intranet.d.services.notifications.NotificationMailSenderStub;
import ru.yandex.intranet.d.services.tracker.TrackerClient;
import ru.yandex.intranet.d.services.transfer.TransferRequestResponsibleAndNotifyService;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestStatusDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestSubtypeDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestVoteTypeDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaResourceTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestParametersDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontPutTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontSingleTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestVotingDto;

import static ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER;
import static ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER_2;
import static ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER_UID;
import static ru.yandex.intranet.d.TestUsers.USER_2_ID;
import static ru.yandex.intranet.d.TestUsers.USER_2_UID;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.folderModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.providerModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.quotaModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.resourceModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.resourceSegmentModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.resourceSegmentationModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.resourceTypeModel;

/**
 * Front Transfer Requests Refresh API Test
 * */
@IntegrationTest
public class FrontTransferRequestsControllerRefreshTest {
    @Autowired
    private WebTestClient webClient;
    @Autowired
    private ProvidersDao providersDao;
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
    private FolderDao folderDao;
    @Autowired
    private ServicesDao servicesDao;
    @Autowired
    private FolderOperationLogDao folderOperationLogDao;
    @Autowired
    private AbcServiceMemberDao abcServiceMemberDao;
    @Autowired
    private YdbTableClient tableClient;
    @Autowired
    private UsersDao usersDao;
    @Autowired
    private NotificationMailSenderStub mailSender;
    @Autowired
    private PendingTransferRequestsDao pendingTransferRequestsDao;
    @Autowired
    private TransferRequestsDao transferRequestsDao;
    @SpyBean
    private TrackerClient trackerClient;

    private final long quotaManagerRoleId;
    private final long responsibleOfProvider;
    public static final long FOLDER_SERVICE_ID = 7765L;
    public static final long PROVIDER_SERVICE_ID = 7766L;

    public FrontTransferRequestsControllerRefreshTest(
            @Value("${abc.roles.quotaManager}") long quotaManagerRoleId,
            @Value("${abc.roles.responsibleOfProvider}") long responsibleOfProvider) {
        this.quotaManagerRoleId = quotaManagerRoleId;
        this.responsibleOfProvider = responsibleOfProvider;
    }

    public static UserModel.Builder userModelBuilder(long uid) {
        return UserModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .passportUid(Long.toString(uid))
                .passportLogin("test-login-" + uid)
                .staffId(uid)
                .staffDismissed(false)
                .staffRobot(false)
                .staffAffiliation(StaffAffiliation.YANDEX)
                .roles(Map.of())
                .gender("M")
                .firstNameEn("test")
                .firstNameRu("test")
                .lastNameEn("test")
                .lastNameRu("test")
                .dAdmin(false)
                .deleted(false)
                .workEmail("login-" + uid + "@yandex-team.ru")
                .langUi("ru")
                .timeZone("Europe/Moscow");
    }

    public static AbcServiceMemberModel.Builder abcServiceMemberModelBuilder(long staffId,
                                                                             long serviceId,
                                                                             long roleId) {
        return AbcServiceMemberModel.newBuilder()
                .id(staffId)
                .staffId(staffId)
                .serviceId(serviceId)
                .roleId(roleId)
                .state(AbcServiceMemberState.ACTIVE);
    }

    private static Stream<Arguments> voteTypeWithExpectedStatus() {
        return Stream.of(
            Arguments.of(TransferRequestVoteTypeDto.REJECT, TransferRequestStatus.REJECTED),
            Arguments.of(TransferRequestVoteTypeDto.CONFIRM, TransferRequestStatus.APPLIED),
            Arguments.of(TransferRequestVoteTypeDto.ABSTAIN, TransferRequestStatus.PENDING)
        );
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void putTransferRequestRefreshAddAndRemoveResponsibleTest() {
        ProviderModel provider = providerModel("in-process:test", null, false);
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
        FolderModel folderOne = folderModel(1L);
        FolderModel folderTwo = folderModel(2L);
        QuotaModel quotaOne = quotaModel(provider.getId(), resource.getId(), folderOne.getId(), 50, 50);
        QuotaModel quotaTwo = quotaModel(provider.getId(), resource.getId(), folderTwo.getId(), 150, 150);
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                providersDao.upsertProviderRetryable(txSession, provider)))
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
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                                .upsertAllRetryable(txSession, List.of(folderOne, folderTwo))))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                                .upsertAllRetryable(txSession, List.of(quotaOne, quotaTwo))))
                .block();
        final UserModel newQuotaManager = userModelBuilder(1001L)
                .roles(Map.of(UserServiceRoles.QUOTA_MANAGER, Set.of(TestServices.TEST_SERVICE_ID_D)))
                .build();
        final AbcServiceMemberModel newQuotaManagerServiceModel = abcServiceMemberModelBuilder(1001L,
                TestServices.TEST_SERVICE_ID_D, quotaManagerRoleId)
                .build();
        final UserModel newQuotaManager2 = userModelBuilder(1002L)
                .roles(Map.of(UserServiceRoles.QUOTA_MANAGER, Set.of(TestServices.TEST_SERVICE_ID_D)))
                .build();
        final AbcServiceMemberModel newQuotaManager2ServiceModel = abcServiceMemberModelBuilder(1002L,
                TestServices.TEST_SERVICE_ID_D, quotaManagerRoleId)
                .build();
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                usersDao.upsertUserRetryable(txSession, newQuotaManager)))
                .block();
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                abcServiceMemberDao.upsertManyRetryable(txSession,
                                        List.of(newQuotaManagerServiceModel))))
                .block();
        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId("1")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("100")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("-100")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(newQuotaManager.getId()))));
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                usersDao.upsertUserRetryable(txSession, newQuotaManager2)))
                .block();
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                abcServiceMemberDao.upsertManyRetryable(txSession,
                                        List.of(newQuotaManager2ServiceModel))))
                .block();
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                abcServiceMemberDao.depriveRoleRetryable(txSession,
                                        newQuotaManagerServiceModel.getId())))
                .block();
        long counter = mailSender.getCounter();
        Mockito.verify(trackerClient, Mockito.times(0)).updateTicket(Mockito.any(), Mockito.any());
        FrontSingleTransferRequestDto putResult = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .put()
                .uri("/front/transfers/{id}?version={version}", result.getTransfer().getId(),
                        result.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontPutTransferRequestDto.builder()
                        .addConfirmation(true)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId("1")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("90")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("-90")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Mockito.verify(trackerClient, Mockito.times(1)).updateTicket(Mockito.any(), Mockito.any());
        Assertions.assertNotNull(putResult);
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, putResult.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER,
                putResult.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, putResult.getTransfer().getStatus());
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                                TransactionMode.SERIALIZABLE_READ_WRITE,
                                txSession -> pendingTransferRequestsDao.getById(txSession,
                                        result.getTransfer().getId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Optional<TransferRequestModel> loadedRequestO = tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                                txSession -> transferRequestsDao.getById(txSession, result.getTransfer().getId(),
                                        Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(loadedRequestO);
        Assertions.assertTrue(loadedRequestO.isPresent());
        TransferRequestModel transferRequestModel = loadedRequestO.get();
        Assertions.assertNotNull(pendingTransferRequest);
        Assertions.assertEquals(1, mailSender.getCounter() - counter);
        Assertions.assertEquals(
                Optional.of(result.getTransfer().getId()),
                pendingTransferRequest.map(PendingTransferRequestsModel::getRequestId));
        Assertions.assertEquals(USER_2_ID, putResult.getTransfer().getCreatedBy());
        Assertions.assertTrue(putResult.getTransfer().getTransferVotes().getVotes().isEmpty());
        Assertions.assertTrue(transferRequestModel.getTransferNotified().isPresent());
        TransferNotified transferNotified = transferRequestModel.getTransferNotified().get();
        Assertions.assertTrue(transferNotified.getNotifiedUserIds()
                .containsAll(Set.of(SERVICE_1_QUOTA_MANAGER, SERVICE_1_QUOTA_MANAGER_2, newQuotaManager2.getId())));
        Assertions.assertFalse(transferNotified.getNotifiedUserIds().contains(newQuotaManager.getId()));

        Assertions.assertEquals(2, putResult.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId())));

        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(newQuotaManager2.getId()))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .allMatch(g -> g.getResponsibles().stream()
                        .noneMatch(r -> r.getResponsibleId().equals(newQuotaManager.getId()))));

        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains("2"))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains("2"))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(newQuotaManager2.getId())
                                && r.getServiceIds().contains("2"))));

        Assertions.assertEquals(2, putResult.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(putResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals("1")));
        Assertions.assertTrue(putResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals("2")));
        Assertions.assertTrue(putResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("90") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
        Assertions.assertTrue(putResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("-90") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void putTransferRequestFailedRefreshAddAndRemoveResponsibleTest() {
        ProviderModel provider = providerModel("in-process:test", null, false);
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
        FolderModel folderOne = folderModel(1L);
        FolderModel folderTwo = folderModel(2L);
        QuotaModel quotaOne = quotaModel(provider.getId(), resource.getId(), folderOne.getId(), 50, 50);
        QuotaModel quotaTwo = quotaModel(provider.getId(), resource.getId(), folderTwo.getId(), 150, 150);
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                providersDao.upsertProviderRetryable(txSession, provider)))
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
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                                .upsertAllRetryable(txSession, List.of(folderOne, folderTwo))))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                                .upsertAllRetryable(txSession, List.of(quotaOne, quotaTwo))))
                .block();
        final UserModel newQuotaManager = userModelBuilder(1001L)
                .roles(Map.of(UserServiceRoles.QUOTA_MANAGER, Set.of(TestServices.TEST_SERVICE_ID_D)))
                .build();
        final AbcServiceMemberModel newQuotaManagerServiceModel = abcServiceMemberModelBuilder(1001L,
                TestServices.TEST_SERVICE_ID_D, quotaManagerRoleId)
                .build();
        final UserModel newQuotaManager2 = userModelBuilder(1002L)
                .roles(Map.of(UserServiceRoles.QUOTA_MANAGER, Set.of(TestServices.TEST_SERVICE_ID_D)))
                .build();
        final AbcServiceMemberModel newQuotaManager2ServiceModel = abcServiceMemberModelBuilder(1002L,
                TestServices.TEST_SERVICE_ID_D, quotaManagerRoleId)
                .build();
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                usersDao.upsertUsersRetryable(txSession, List.of(newQuotaManager, newQuotaManager2))))
                .block();
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                abcServiceMemberDao.upsertManyRetryable(txSession,
                                        List.of(newQuotaManagerServiceModel))))
                .block();
        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId("1")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("100")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("-100")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                abcServiceMemberDao.depriveRoleRetryable(txSession,
                                        newQuotaManagerServiceModel.getId())))
                .block();
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                abcServiceMemberDao.upsertManyRetryable(txSession,
                                        List.of(newQuotaManager2ServiceModel))))
                .block();
        Optional<TransferRequestModel> loadedRequestO = tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                                txSession -> transferRequestsDao.getById(txSession, result.getTransfer().getId(),
                                        Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(loadedRequestO);
        Assertions.assertTrue(loadedRequestO.isPresent());
        TransferRequestModel transferRequestModel = loadedRequestO.get();
        Set<String> responsibleIds = transferRequestModel.getResponsible().getResponsible()
                .stream()
                .flatMap(f -> f.getResponsible().stream().flatMap(sr -> sr.getResponsibleIds().stream()))
                .collect(Collectors.toSet());
        Assertions.assertTrue(responsibleIds.contains(newQuotaManager.getId()));
        long counter = mailSender.getCounter();
        Mockito.verify(trackerClient, Mockito.times(0)).updateTicket(Mockito.any(), Mockito.any());
        ErrorCollectionDto error = webClient
                .mutateWith(MockUser.uid(newQuotaManager.getPassportUid().get()))
                .put()
                .uri("/front/transfers/{id}?version={version}", result.getTransfer().getId(),
                        result.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontPutTransferRequestDto.builder()
                        .addConfirmation(true)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId("1")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("90")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("-90")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Mockito.verify(trackerClient, Mockito.times(1)).updateTicket(Mockito.any(), Mockito.any());
        Assertions.assertNotNull(error);
        Assertions.assertEquals(counter + 1, mailSender.getCounter());
        Assertions.assertTrue(mailSender.getMail().stream()
                .anyMatch(mn -> mn.getActualTo()
                        .map(at -> at.equals(newQuotaManager2.getWorkEmail().get()))
                        .orElse(false)));
        loadedRequestO = tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                                txSession -> transferRequestsDao.getById(txSession, result.getTransfer().getId(),
                                        Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(loadedRequestO);
        Assertions.assertTrue(loadedRequestO.isPresent());
        transferRequestModel = loadedRequestO.get();
        responsibleIds = TransferRequestResponsibleAndNotifyService.getResponsibleIds(transferRequestModel);
        Assertions.assertFalse(responsibleIds.contains(newQuotaManager.getId()));
        Assertions.assertTrue(responsibleIds.containsAll(Set.of(SERVICE_1_QUOTA_MANAGER, SERVICE_1_QUOTA_MANAGER_2,
                newQuotaManager2.getId())));
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void cancelTransferRequestRefreshAddAndRemoveResponsibleTest() {
        ProviderModel provider = providerModel("in-process:test", null, false);
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
        FolderModel folderOne = folderModel(1L);
        FolderModel folderTwo = folderModel(2L);
        QuotaModel quotaOne = quotaModel(provider.getId(), resource.getId(), folderOne.getId(), 50, 50);
        QuotaModel quotaTwo = quotaModel(provider.getId(), resource.getId(), folderTwo.getId(), 150, 150);
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                providersDao.upsertProviderRetryable(txSession, provider)))
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
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                                .upsertAllRetryable(txSession, List.of(folderOne, folderTwo))))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                                .upsertAllRetryable(txSession, List.of(quotaOne, quotaTwo))))
                .block();
        final UserModel newQuotaManager = userModelBuilder(1001L)
                .roles(Map.of(UserServiceRoles.QUOTA_MANAGER, Set.of(TestServices.TEST_SERVICE_ID_D)))
                .build();
        final AbcServiceMemberModel newQuotaManagerServiceModel = abcServiceMemberModelBuilder(1001L,
                TestServices.TEST_SERVICE_ID_D, quotaManagerRoleId)
                .build();
        final UserModel newQuotaManager2 = userModelBuilder(1002L)
                .roles(Map.of(UserServiceRoles.QUOTA_MANAGER, Set.of(TestServices.TEST_SERVICE_ID_D)))
                .build();
        final AbcServiceMemberModel newQuotaManager2ServiceModel = abcServiceMemberModelBuilder(1002L,
                TestServices.TEST_SERVICE_ID_D, quotaManagerRoleId)
                .build();
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                usersDao.upsertUsersRetryable(txSession, List.of(newQuotaManager, newQuotaManager2))))
                .block();
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                abcServiceMemberDao.upsertManyRetryable(txSession,
                                        List.of(newQuotaManagerServiceModel))))
                .block();
        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId("1")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("100")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("-100")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                abcServiceMemberDao.upsertManyRetryable(txSession,
                                        List.of(newQuotaManager2ServiceModel))))
                .block();
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                abcServiceMemberDao.depriveRoleRetryable(txSession,
                                        newQuotaManagerServiceModel.getId())))
                .block();
        long counter = mailSender.getCounter();
        Mockito.verify(trackerClient, Mockito.times(0)).updateTicket(Mockito.any(), Mockito.any());
        Mockito.verify(trackerClient, Mockito.times(0)).closeTicket(Mockito.any(), Mockito.any(), Mockito.any());
        FrontSingleTransferRequestDto cancelResult = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers/{id}/_cancel?version={version}", result.getTransfer().getId(),
                        result.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Mockito.verify(trackerClient, Mockito.times(0)).updateTicket(Mockito.any(), Mockito.any());
        Mockito.verify(trackerClient, Mockito.times(1)).closeTicket(Mockito.any(), Mockito.any(), Mockito.any());
        Assertions.assertNotNull(cancelResult);
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, cancelResult.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER,
                cancelResult.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.CANCELLED, cancelResult.getTransfer().getStatus());
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                                TransactionMode.SERIALIZABLE_READ_WRITE,
                                txSession -> pendingTransferRequestsDao.getById(txSession,
                                        result.getTransfer().getId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Optional<TransferRequestModel> loadedRequestO = tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                                txSession -> transferRequestsDao.getById(txSession, result.getTransfer().getId(),
                                        Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(loadedRequestO);
        Assertions.assertTrue(loadedRequestO.isPresent());
        TransferRequestModel transferRequestModel = loadedRequestO.get();
        Assertions.assertNotNull(pendingTransferRequest);
        Assertions.assertFalse(pendingTransferRequest.isPresent());
        Assertions.assertEquals(0, mailSender.getCounter() - counter);
        Assertions.assertEquals(USER_2_ID, cancelResult.getTransfer().getCreatedBy());
        Assertions.assertTrue(cancelResult.getTransfer().getTransferVotes().getVotes().isEmpty());
        Assertions.assertTrue(transferRequestModel.getTransferNotified().isPresent());
        TransferNotified transferNotified = transferRequestModel.getTransferNotified().get();
        Assertions.assertTrue(transferNotified.getNotifiedUserIds()
                .containsAll(Set.of(SERVICE_1_QUOTA_MANAGER, SERVICE_1_QUOTA_MANAGER_2, newQuotaManager.getId())));
        Assertions.assertFalse(transferNotified.getNotifiedUserIds().contains(newQuotaManager2.getId()));

        Assertions.assertEquals(2, cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId())));

        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(newQuotaManager2.getId()))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .allMatch(g -> g.getResponsibles().stream()
                        .noneMatch(r -> r.getResponsibleId().equals(newQuotaManager.getId()))));

        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains("2"))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains("2"))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(newQuotaManager2.getId())
                                && r.getServiceIds().contains("2"))));

        Assertions.assertEquals(2, cancelResult.getTransfer().getParameters().getQuotaTransfers().size());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void cancelTransferRequestFailedRefreshAddAndRemoveResponsibleTest() {
        ProviderModel provider = providerModel("in-process:test", null, false);
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
        FolderModel folderOne = folderModel(1L);
        FolderModel folderTwo = folderModel(2L);
        QuotaModel quotaOne = quotaModel(provider.getId(), resource.getId(), folderOne.getId(), 50, 50);
        QuotaModel quotaTwo = quotaModel(provider.getId(), resource.getId(), folderTwo.getId(), 150, 150);
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                providersDao.upsertProviderRetryable(txSession, provider)))
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
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                                .upsertAllRetryable(txSession, List.of(folderOne, folderTwo))))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                                .upsertAllRetryable(txSession, List.of(quotaOne, quotaTwo))))
                .block();
        final UserModel newQuotaManager = userModelBuilder(1001L)
                .roles(Map.of(UserServiceRoles.QUOTA_MANAGER, Set.of(TestServices.TEST_SERVICE_ID_D)))
                .build();
        final AbcServiceMemberModel newQuotaManagerServiceModel = abcServiceMemberModelBuilder(1001L,
                TestServices.TEST_SERVICE_ID_D, quotaManagerRoleId)
                .build();
        final UserModel newQuotaManager2 = userModelBuilder(1002L)
                .roles(Map.of(UserServiceRoles.QUOTA_MANAGER, Set.of(TestServices.TEST_SERVICE_ID_D)))
                .build();
        final AbcServiceMemberModel newQuotaManager2ServiceModel = abcServiceMemberModelBuilder(1002L,
                TestServices.TEST_SERVICE_ID_D, quotaManagerRoleId)
                .build();
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                usersDao.upsertUsersRetryable(txSession, List.of(newQuotaManager, newQuotaManager2))))
                .block();
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                abcServiceMemberDao.upsertManyRetryable(txSession,
                                        List.of(newQuotaManagerServiceModel))))
                .block();
        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId("1")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("100")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("-100")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                abcServiceMemberDao.depriveRoleRetryable(txSession,
                                        newQuotaManagerServiceModel.getId())))
                .block();
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                abcServiceMemberDao.upsertManyRetryable(txSession,
                                        List.of(newQuotaManager2ServiceModel))))
                .block();
        long counter = mailSender.getCounter();
        Mockito.verify(trackerClient, Mockito.times(0)).updateTicket(Mockito.any(), Mockito.any());
        Mockito.verify(trackerClient, Mockito.times(0)).closeTicket(Mockito.any(), Mockito.any(), Mockito.any());
        ErrorCollectionDto cancelErrors = webClient
                .mutateWith(MockUser.uid(newQuotaManager.getPassportUid().get()))
                .post()
                .uri("/front/transfers/{id}/_cancel?version={version}", result.getTransfer().getId(),
                        result.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Mockito.verify(trackerClient, Mockito.times(1)).updateTicket(Mockito.any(), Mockito.any());
        Mockito.verify(trackerClient, Mockito.times(0)).closeTicket(Mockito.any(), Mockito.any(), Mockito.any());
        Assertions.assertNotNull(cancelErrors);
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                                TransactionMode.SERIALIZABLE_READ_WRITE,
                                txSession -> pendingTransferRequestsDao.getById(txSession,
                                        result.getTransfer().getId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Optional<TransferRequestModel> loadedRequestO = tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                                txSession -> transferRequestsDao.getById(txSession, result.getTransfer().getId(),
                                        Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(loadedRequestO);
        Assertions.assertTrue(loadedRequestO.isPresent());
        TransferRequestModel transferRequestModel = loadedRequestO.get();
        Assertions.assertNotNull(pendingTransferRequest);
        Assertions.assertTrue(pendingTransferRequest.isPresent());
        Assertions.assertEquals(TransferRequestStatus.PENDING, transferRequestModel.getStatus());
        Assertions.assertEquals(1, mailSender.getCounter() - counter);
        Assertions.assertTrue(mailSender.getMail().stream()
                .anyMatch(mn -> mn.getActualTo()
                        .map(at -> at.equals(newQuotaManager2.getWorkEmail().get()))
                        .orElse(false)));
        Assertions.assertTrue(transferRequestModel.getTransferNotified().isPresent());
        TransferNotified transferNotified = transferRequestModel.getTransferNotified().get();
        Assertions.assertEquals(transferNotified.getNotifiedUserIds(),
                Set.of(SERVICE_1_QUOTA_MANAGER, SERVICE_1_QUOTA_MANAGER_2, newQuotaManager2.getId()));
        Set<String> responsibleIds = TransferRequestResponsibleAndNotifyService.getResponsibleIds(transferRequestModel);
        Assertions.assertTrue(responsibleIds.containsAll(Set.of(SERVICE_1_QUOTA_MANAGER, SERVICE_1_QUOTA_MANAGER_2,
                newQuotaManager2.getId())));
        Assertions.assertFalse(responsibleIds.contains(newQuotaManager.getId()));
    }

    @MethodSource("voteTypeWithExpectedStatus")
    @ParameterizedTest(name = "[{index}] {displayName} ({arguments})")
    @SuppressWarnings("MethodLength")
    public void voteTransferRequestRefreshAddAndRemoveResponsibleTest(TransferRequestVoteTypeDto votingType,
                                                                      TransferRequestStatus status) {
        ProviderModel provider = providerModel("in-process:test", null, false);
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
        FolderModel folderOne = folderModel(1L);
        FolderModel folderTwo = folderModel(2L);
        QuotaModel quotaOne = quotaModel(provider.getId(), resource.getId(), folderOne.getId(), 50, 50);
        QuotaModel quotaTwo = quotaModel(provider.getId(), resource.getId(), folderTwo.getId(), 150, 150);
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                providersDao.upsertProviderRetryable(txSession, provider)))
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
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                                .upsertAllRetryable(txSession, List.of(folderOne, folderTwo))))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                                .upsertAllRetryable(txSession, List.of(quotaOne, quotaTwo))))
                .block();
        final UserModel newQuotaManager = userModelBuilder(1001L)
                .roles(Map.of(UserServiceRoles.QUOTA_MANAGER, Set.of(TestServices.TEST_SERVICE_ID_D)))
                .build();
        final AbcServiceMemberModel newQuotaManagerServiceModel = abcServiceMemberModelBuilder(1001L,
                TestServices.TEST_SERVICE_ID_D, quotaManagerRoleId)
                .build();
        final UserModel newQuotaManager2 = userModelBuilder(1002L)
                .roles(Map.of(UserServiceRoles.QUOTA_MANAGER, Set.of(TestServices.TEST_SERVICE_ID_D)))
                .build();
        final AbcServiceMemberModel newQuotaManager2ServiceModel = abcServiceMemberModelBuilder(1002L,
                TestServices.TEST_SERVICE_ID_D, quotaManagerRoleId)
                .build();
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                usersDao.upsertUsersRetryable(txSession, List.of(newQuotaManager, newQuotaManager2))))
                .block();
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                abcServiceMemberDao.upsertManyRetryable(txSession,
                                        List.of(newQuotaManagerServiceModel))))
                .block();
        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId("1")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("100")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("-100")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                abcServiceMemberDao.upsertManyRetryable(txSession,
                                        List.of(newQuotaManager2ServiceModel))))
                .block();
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                abcServiceMemberDao.depriveRoleRetryable(txSession,
                                        newQuotaManagerServiceModel.getId())))
                .block();
        long counter = mailSender.getCounter();
        Mockito.verify(trackerClient, Mockito.times(0)).updateTicket(Mockito.any(), Mockito.any());
        Mockito.verify(trackerClient, Mockito.times(0)).closeTicket(Mockito.any(), Mockito.any(), Mockito.any());
        FrontSingleTransferRequestDto cancelResult = webClient
                .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_UID))
                .post()
                .uri("/front/transfers/{id}/_vote?version={version}", result.getTransfer().getId(),
                        result.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestVotingDto.builder()
                        .voteType(votingType)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(cancelResult);
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, cancelResult.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER,
                cancelResult.getTransfer().getRequestSubtype());
        Optional<TransferRequestModel> loadedRequestO = tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                                txSession -> transferRequestsDao.getById(txSession, result.getTransfer().getId(),
                                        Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(loadedRequestO);
        Assertions.assertTrue(loadedRequestO.isPresent());
        TransferRequestModel transferRequestModel = loadedRequestO.get();
        Assertions.assertEquals(status, transferRequestModel.getStatus());
        Mockito.verify(trackerClient, Mockito.times(1)).updateTicket(Mockito.any(), Mockito.any());
        TransferNotified transferNotified = transferRequestModel.getTransferNotified().get();
        if (votingType == TransferRequestVoteTypeDto.ABSTAIN) {
            Mockito.verify(trackerClient, Mockito.times(0)).closeTicket(Mockito.any(), Mockito.any(), Mockito.any());
            Assertions.assertEquals(1, mailSender.getCounter() - counter);
            Assertions.assertTrue(mailSender.getMail().stream()
                .anyMatch(mn -> mn.getActualTo()
                        .map(at -> at.equals(newQuotaManager2.getWorkEmail().get()))
                        .orElse(false)));
            Assertions.assertTrue(transferNotified.getNotifiedUserIds()
                    .containsAll(Set.of(SERVICE_1_QUOTA_MANAGER, SERVICE_1_QUOTA_MANAGER_2, newQuotaManager2.getId())));
            Assertions.assertFalse(transferNotified.getNotifiedUserIds().contains(newQuotaManager.getId()));
        } else {
            Assertions.assertEquals(0, mailSender.getCounter() - counter);
            Mockito.verify(trackerClient, Mockito.times(1)).closeTicket(Mockito.any(), Mockito.any(), Mockito.any());
            Assertions.assertTrue(transferNotified.getNotifiedUserIds()
                    .containsAll(Set.of(SERVICE_1_QUOTA_MANAGER, SERVICE_1_QUOTA_MANAGER_2, newQuotaManager.getId())));
            Assertions.assertFalse(transferNotified.getNotifiedUserIds().contains(newQuotaManager2.getId()));
        }
        Assertions.assertEquals(USER_2_ID, cancelResult.getTransfer().getCreatedBy());
        Assertions.assertFalse(cancelResult.getTransfer().getTransferVotes().getVotes().isEmpty());
        Assertions.assertTrue(transferRequestModel.getTransferNotified().isPresent());


        Assertions.assertEquals(2, cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId())));

        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(newQuotaManager2.getId()))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .allMatch(g -> g.getResponsibles().stream()
                        .noneMatch(r -> r.getResponsibleId().equals(newQuotaManager.getId()))));

        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains("2"))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains("2"))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(newQuotaManager2.getId())
                                && r.getServiceIds().contains("2"))));

        Assertions.assertEquals(2, cancelResult.getTransfer().getParameters().getQuotaTransfers().size());
    }

    @MethodSource("voteTypeWithExpectedStatus")
    @ParameterizedTest(name = "[{index}] {displayName} ({arguments})")
    @SuppressWarnings("MethodLength")
    public void voteTransferRequestRejectRefreshAddAndRemoveResponsibleTest(TransferRequestVoteTypeDto votingType,
                                                                            TransferRequestStatus status) {
        ProviderModel provider = providerModel("in-process:test", null, false);
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
        FolderModel folderOne = folderModel(1L);
        FolderModel folderTwo = folderModel(2L);
        QuotaModel quotaOne = quotaModel(provider.getId(), resource.getId(), folderOne.getId(), 50, 50);
        QuotaModel quotaTwo = quotaModel(provider.getId(), resource.getId(), folderTwo.getId(), 150, 150);
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                providersDao.upsertProviderRetryable(txSession, provider)))
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
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                                .upsertAllRetryable(txSession, List.of(folderOne, folderTwo))))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                                .upsertAllRetryable(txSession, List.of(quotaOne, quotaTwo))))
                .block();
        final UserModel newQuotaManager = userModelBuilder(1001L)
                .roles(Map.of(UserServiceRoles.QUOTA_MANAGER, Set.of(TestServices.TEST_SERVICE_ID_D)))
                .build();
        final AbcServiceMemberModel newQuotaManagerServiceModel = abcServiceMemberModelBuilder(1001L,
                TestServices.TEST_SERVICE_ID_D, quotaManagerRoleId)
                .build();
        final UserModel newQuotaManager2 = userModelBuilder(1002L)
                .roles(Map.of(UserServiceRoles.QUOTA_MANAGER, Set.of(TestServices.TEST_SERVICE_ID_D)))
                .build();
        final AbcServiceMemberModel newQuotaManager2ServiceModel = abcServiceMemberModelBuilder(1002L,
                TestServices.TEST_SERVICE_ID_D, quotaManagerRoleId)
                .build();
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                usersDao.upsertUsersRetryable(txSession, List.of(newQuotaManager, newQuotaManager2))))
                .block();
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                abcServiceMemberDao.upsertManyRetryable(txSession,
                                        List.of(newQuotaManagerServiceModel))))
                .block();
        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId("1")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("100")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("-100")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                abcServiceMemberDao.upsertManyRetryable(txSession,
                                        List.of(newQuotaManager2ServiceModel))))
                .block();
        tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                abcServiceMemberDao.depriveRoleRetryable(txSession,
                                        newQuotaManagerServiceModel.getId())))
                .block();
        long counter = mailSender.getCounter();
        Mockito.verify(trackerClient, Mockito.times(0)).updateTicket(Mockito.any(), Mockito.any());
        Mockito.verify(trackerClient, Mockito.times(0)).closeTicket(Mockito.any(), Mockito.any(), Mockito.any());
        ErrorCollectionDto cancelResult = webClient
                .mutateWith(MockUser.uid(newQuotaManager.getPassportUid().get()))
                .post()
                .uri("/front/transfers/{id}/_vote?version={version}", result.getTransfer().getId(),
                        result.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestVotingDto.builder()
                        .voteType(votingType)
                        .build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(cancelResult);
        Optional<TransferRequestModel> loadedRequestO = tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                                txSession -> transferRequestsDao.getById(txSession, result.getTransfer().getId(),
                                        Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(loadedRequestO);
        Assertions.assertTrue(loadedRequestO.isPresent());
        TransferRequestModel transferRequestModel = loadedRequestO.get();
        Assertions.assertEquals(TransferRequestStatus.PENDING, transferRequestModel.getStatus());
        Mockito.verify(trackerClient, Mockito.times(1)).updateTicket(Mockito.any(), Mockito.any());
        Mockito.verify(trackerClient, Mockito.times(0)).closeTicket(Mockito.any(), Mockito.any(), Mockito.any());
        Assertions.assertEquals(1, mailSender.getCounter() - counter);
        Assertions.assertTrue(mailSender.getMail().stream()
                .anyMatch(mn -> mn.getActualTo()
                        .map(at -> at.equals(newQuotaManager2.getWorkEmail().get()))
                        .orElse(false)));
        Assertions.assertTrue(transferRequestModel.getVotes().getVotes().isEmpty());
        Assertions.assertTrue(transferRequestModel.getTransferNotified().isPresent());
        TransferNotified transferNotified = transferRequestModel.getTransferNotified().get();
        Assertions.assertTrue(transferNotified.getNotifiedUserIds()
                .containsAll(Set.of(SERVICE_1_QUOTA_MANAGER, SERVICE_1_QUOTA_MANAGER_2, newQuotaManager2.getId())));
        Assertions.assertFalse(transferNotified.getNotifiedUserIds().contains(newQuotaManager.getId()));
        Set<String> responsibleIds = TransferRequestResponsibleAndNotifyService.getResponsibleIds(transferRequestModel);
        Assertions.assertEquals(Set.of(SERVICE_1_QUOTA_MANAGER, SERVICE_1_QUOTA_MANAGER_2, newQuotaManager2.getId()),
                responsibleIds);
    }
}
