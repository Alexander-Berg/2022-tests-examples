package ru.yandex.intranet.d.web.front.transfer;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuple8;
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
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
import ru.yandex.intranet.d.dao.users.AbcServiceMemberDao;
import ru.yandex.intranet.d.dao.users.UsersDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderType;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.model.resources.ResourceModel;
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel;
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel;
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel;
import ru.yandex.intranet.d.model.transfers.PendingTransferRequestsModel;
import ru.yandex.intranet.d.model.users.AbcServiceMemberModel;
import ru.yandex.intranet.d.model.users.AbcServiceMemberState;
import ru.yandex.intranet.d.services.notifications.NotificationMailSenderStub;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestHistoryEventTypeDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestStatusDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestSubtypeDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaResourceTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateReserveTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestParametersDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontPutTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontSingleTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestHistoryEventsPageDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestSearchDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestsPageDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_RESERVE_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_SERVICE_ID;
import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestResources.YP_CPU_MAN;
import static ru.yandex.intranet.d.TestResources.YP_HDD_MAN;
import static ru.yandex.intranet.d.TestResources.YP_SSD_MAN;
import static ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_DISPENSER;
import static ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER;
import static ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER_2;
import static ru.yandex.intranet.d.TestUsers.USER_1_ID;
import static ru.yandex.intranet.d.TestUsers.USER_2_ID;
import static ru.yandex.intranet.d.TestUsers.USER_2_UID;
import static ru.yandex.intranet.d.UnitIds.BYTES;
import static ru.yandex.intranet.d.UnitIds.CORES;
import static ru.yandex.intranet.d.UnitIds.GIGABYTES;
import static ru.yandex.intranet.d.UnitIds.KILOBYTES;
import static ru.yandex.intranet.d.UnitIds.MEGABYTES;
import static ru.yandex.intranet.d.utils.TestUtil.assertPresent;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.folderModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.providerModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.quotaModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.resourceModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.resourceSegmentModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.resourceSegmentationModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.resourceTypeModel;

/**
 * Front transfer requests API test.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@IntegrationTest
public class ThirdMoreFrontTransferRequestsControllerTest {
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

    private final long quotaManagerRoleId;
    private final long responsibleOfProvide;
    public static final long FOLDER_SERVICE_ID = 7765L;
    public static final long PROVIDER_SERVICE_ID = 7766L;

    public ThirdMoreFrontTransferRequestsControllerTest(
            @Value("${abc.roles.quotaManager}") long quotaManagerRoleId,
            @Value("${abc.roles.responsibleOfProvider}") long responsibleOfProvider) {
        this.quotaManagerRoleId = quotaManagerRoleId;
        this.responsibleOfProvide = responsibleOfProvider;
    }

    static FolderModel reserveFolderModel(long serviceId) {
        return FolderModel.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setServiceId(serviceId)
                .setVersion(0L)
                .setDisplayName("Test")
                .setDescription("Test")
                .setDeleted(false)
                .setFolderType(FolderType.PROVIDER_RESERVE)
                .setTags(Collections.emptySet())
                .setNextOpLogOrder(1L)
                .build();
    }

    static AbcServiceMemberModel membershipBuilder(long id, long serviceId, long staffId, long roleId) {
        return AbcServiceMemberModel.newBuilder()
                .id(id)
                .serviceId(serviceId)
                .staffId(staffId)
                .roleId(roleId)
                .state(AbcServiceMemberState.ACTIVE)
                .build();
    }

    private Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel,
            QuotaModel, QuotaModel> prepareWithRoles() {
        return SecondMoreFrontTransferRequestsControllerTest.prepareWithRoles(tableClient, servicesDao,
                abcServiceMemberDao, responsibleOfProvide, quotaManagerRoleId, usersDao, providersDao, resourceTypesDao,
                resourceSegmentationsDao, resourceSegmentsDao, resourcesDao, folderDao, quotasDao);
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void getTransferRequestHistoryPutTest() {
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepareWithRoles();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();

        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("50000")
                                                .resourceId(resourceOne.getId())
                                                .deltaUnitId(BYTES)
                                                .build())
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId(String.valueOf(folderOne.getServiceId()))
                                        .providerId(providerModel.getId())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(result);
        FrontSingleTransferRequestDto putResult = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .put()
                .uri("/front/transfers/{id}?version={version}", result.getTransfer().getId(),
                        result.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("40000")
                                                .resourceId(resourceOne.getId())
                                                .deltaUnitId(BYTES)
                                                .build())
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId(String.valueOf(folderOne.getServiceId()))
                                        .providerId(providerModel.getId())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(putResult);
        FrontTransferRequestHistoryEventsPageDto historyResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .get()
                .uri("/front/transfers/{id}/history", result.getTransfer().getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontTransferRequestHistoryEventsPageDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(historyResult);
        assertEquals(2, historyResult.getEvents().size());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void searchTransferRequestTest() {
        FrontTransferRequestsPageDto searchResultByClosedService = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers/_search")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestSearchDto.builder()
                        .filterByServiceId("20")
                        .addFilterByStatus(TransferRequestStatusDto.PENDING)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontTransferRequestsPageDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(searchResultByClosedService);
        assertTrue(searchResultByClosedService.getTransfers().isEmpty());
    }


    @Test
    @SuppressWarnings("MethodLength")
    public void getTransferRequestHistoryTest() {
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
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourceTypesDao
                        .upsertResourceTypeRetryable(txSession, resourceType))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourceSegmentationsDao
                        .upsertResourceSegmentationRetryable(txSession, locationSegmentation))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourceSegmentsDao
                        .upsertResourceSegmentRetryable(txSession, vlaSegment))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourcesDao
                        .upsertResourceRetryable(txSession, resource))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertAllRetryable(txSession, List.of(folderOne, folderTwo)))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .upsertAllRetryable(txSession, List.of(quotaOne, quotaTwo)))).block();
        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
        assertNotNull(result);
        FrontTransferRequestHistoryEventsPageDto historyResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/transfers/{id}/history", result.getTransfer().getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontTransferRequestHistoryEventsPageDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(historyResult);
        assertEquals(1, historyResult.getEvents().size());
        assertEquals("193adb36-7db2-4542-875f-ef93cddbd52d",
                historyResult.getEvents().get(0).getAuthorId().get());
        assertEquals(TransferRequestHistoryEventTypeDto.CREATED,
                historyResult.getEvents().get(0).getEventType());
        assertEquals(TransferRequestStatusDto.PENDING,
                historyResult.getEvents().get(0).getNewFields().get().getStatus().get());
        assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER,
                historyResult.getEvents().get(0).getNewFields().get().getType().get());
        assertEquals(2,
                historyResult.getEvents().get(0).getNewTransferResponsible().get().getResponsibleGroups().size());
        assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible().get()
                .getResponsibleGroups().stream().anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible().get()
                .getResponsibleGroups().stream().anyMatch(g -> g.getFolders().contains(folderTwo.getId())));
        assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible().get()
                .getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible().get()
                .getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible().get()
                .getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible().get()
                .getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible().get()
                .getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("1"))));
        assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible().get()
                .getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("1"))));
        assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible().get()
                .getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("2"))));
        assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible().get()
                .getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("2"))));
        assertEquals(2,
                historyResult.getEvents().get(0).getNewParameters().get().getQuotaTransfers().size());
        assertTrue(historyResult.getEvents().get(0).getNewParameters().get().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals("1")));
        assertTrue(historyResult.getEvents().get(0).getNewParameters().get().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals("2")));
        assertTrue(historyResult.getEvents().get(0).getNewParameters().get().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
        assertTrue(historyResult.getEvents().get(0).getNewParameters().get().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("-100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void searchReserveTransferRequestTest() {
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepareWithRoles();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();
        FolderModel folderTwo = prepare.getT6();

        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("50000")
                                                .resourceId(resourceOne.getId())
                                                .deltaUnitId(BYTES)
                                                .build())
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId(String.valueOf(folderOne.getServiceId()))
                                        .providerId(providerModel.getId())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(result);
        FrontTransferRequestsPageDto searchResultByCurrentUser = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers/_search")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestSearchDto.builder()
                        .filterByCurrentUser(true)
                        .addFilterByStatus(TransferRequestStatusDto.PENDING)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontTransferRequestsPageDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(searchResultByCurrentUser);
        FrontTransferRequestsPageDto searchResultByFolder = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers/_search")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestSearchDto.builder()
                        .filterByFolderId(folderOne.getId())
                        .addFilterByStatus(TransferRequestStatusDto.PENDING)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontTransferRequestsPageDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(searchResultByFolder);
        FrontTransferRequestsPageDto searchResultByService = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers/_search")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestSearchDto.builder()
                        .filterByServiceId(String.valueOf(FOLDER_SERVICE_ID))
                        .addFilterByStatus(TransferRequestStatusDto.PENDING)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontTransferRequestsPageDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(searchResultByService);
        FrontTransferRequestDto resultOne = searchResultByCurrentUser.getTransfers().get(0);
        assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, resultOne.getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                resultOne.getRequestSubtype());
        assertEquals(TransferRequestStatusDto.PENDING, resultOne.getStatus());
        assertEquals(USER_2_ID, resultOne.getCreatedBy());
        assertTrue(resultOne.getTransferVotes().getVotes().isEmpty());
        assertEquals(1, resultOne.getTransferResponsible().getResponsibleGroups().size());
        assertTrue(resultOne.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        assertTrue(resultOne.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        assertTrue(resultOne.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        assertTrue(resultOne.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        assertTrue(resultOne.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        assertTrue(resultOne.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        assertTrue(resultOne.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        assertTrue(resultOne.getTransferResponsible().getReserveResponsible().isPresent());
        assertEquals(resultOne.getTransferResponsible().getReserveResponsible().get()
                .getProviderId(), providerModel.getId());
        assertEquals(1, resultOne.getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().size());
        assertTrue(resultOne.getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().contains(USER_1_ID));
        assertEquals(String.valueOf(PROVIDER_SERVICE_ID), resultOne.getTransferResponsible()
                .getReserveResponsible().get().getServiceId());
        assertEquals(folderTwo.getId(), resultOne.getTransferResponsible().getReserveResponsible()
                .get().getFolderId());
        assertEquals(2, resultOne.getParameters().getQuotaTransfers().size());
        assertTrue(resultOne.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        assertTrue(resultOne.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        assertTrue(resultOne.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        assertTrue(resultOne.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        FrontTransferRequestDto resultTwo = searchResultByFolder.getTransfers().get(0);
        assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, resultTwo.getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                resultTwo.getRequestSubtype());
        assertEquals(TransferRequestStatusDto.PENDING, resultTwo.getStatus());
        assertEquals(USER_2_ID, resultTwo.getCreatedBy());
        assertTrue(resultTwo.getTransferVotes().getVotes().isEmpty());
        assertEquals(1, resultTwo.getTransferResponsible().getResponsibleGroups().size());
        assertTrue(resultTwo.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        assertTrue(resultTwo.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        assertTrue(resultTwo.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        assertTrue(resultTwo.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        assertTrue(resultTwo.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        assertTrue(resultTwo.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        assertTrue(resultTwo.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        assertTrue(resultTwo.getTransferResponsible().getReserveResponsible().isPresent());
        assertEquals(resultTwo.getTransferResponsible().getReserveResponsible().get()
                .getProviderId(), providerModel.getId());
        assertEquals(1, resultTwo.getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().size());
        assertTrue(resultTwo.getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().contains(USER_1_ID));
        assertEquals(String.valueOf(PROVIDER_SERVICE_ID), resultTwo.getTransferResponsible()
                .getReserveResponsible().get().getServiceId());
        assertEquals(folderTwo.getId(), resultTwo.getTransferResponsible().getReserveResponsible()
                .get().getFolderId());
        assertEquals(2, resultTwo.getParameters().getQuotaTransfers().size());
        assertTrue(resultTwo.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        assertTrue(resultTwo.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        assertTrue(resultTwo.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        assertTrue(resultTwo.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        FrontTransferRequestDto resultThree = searchResultByService.getTransfers().get(0);
        assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, resultThree.getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                resultThree.getRequestSubtype());
        assertEquals(TransferRequestStatusDto.PENDING, resultThree.getStatus());
        assertEquals(USER_2_ID, resultThree.getCreatedBy());
        assertTrue(resultThree.getTransferVotes().getVotes().isEmpty());
        assertEquals(1, resultThree.getTransferResponsible().getResponsibleGroups().size());
        assertTrue(resultThree.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        assertTrue(resultThree.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        assertTrue(resultThree.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        assertTrue(resultThree.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        assertTrue(resultThree.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        assertTrue(resultThree.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        assertTrue(resultThree.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        assertTrue(resultThree.getTransferResponsible().getReserveResponsible().isPresent());
        assertEquals(resultThree.getTransferResponsible().getReserveResponsible().get()
                .getProviderId(), providerModel.getId());
        assertEquals(1, resultThree.getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().size());
        assertTrue(resultThree.getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().contains(USER_1_ID));
        assertEquals(String.valueOf(PROVIDER_SERVICE_ID), resultThree.getTransferResponsible()
                .getReserveResponsible().get().getServiceId());
        assertEquals(folderTwo.getId(), resultThree.getTransferResponsible().getReserveResponsible()
                .get().getFolderId());
        assertEquals(2, resultThree.getParameters().getQuotaTransfers().size());
        assertTrue(resultThree.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        assertTrue(resultThree.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        assertTrue(resultThree.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        assertTrue(resultThree.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createTransferRequestZeroDeltaTest() {
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
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider)))
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
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
                                                .delta("0")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("0")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createTransferRequestTooSmallDeltaTest() {
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
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider)))
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
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
                                                .delta("0.5")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("0.5")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createTransferRequestRoundingTest() {
        long initialMailCounter = mailSender.getCounter();
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
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider)))
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
        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
                                                .delta("100.5")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("-100.5")
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
        assertNotNull(result);
        assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, result.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER,
                result.getTransfer().getRequestSubtype());
        assertEquals(TransferRequestStatusDto.PENDING, result.getTransfer().getStatus());
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                                TransactionMode.SERIALIZABLE_READ_WRITE,
                                txSession -> pendingTransferRequestsDao.getById(txSession,
                                        result.getTransfer().getId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        assertNotNull(pendingTransferRequest);
        assertEquals(
                Optional.of(result.getTransfer().getId()),
                pendingTransferRequest.map(PendingTransferRequestsModel::getRequestId));
        assertEquals("193adb36-7db2-4542-875f-ef93cddbd52d", result.getTransfer().getCreatedBy());
        assertTrue(result.getTransfer().getTransferVotes().getVotes().isEmpty());
        assertEquals(2, result.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId())));
        assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("1"))));
        assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("1"))));
        assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("2"))));
        assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("2"))));
        assertEquals(2, result.getTransfer().getParameters().getQuotaTransfers().size());
        assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals("1")));
        assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals("2")));
        assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
        assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("-100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
        long updatedMailCounter = mailSender.getCounter();
        assertTrue(updatedMailCounter - initialMailCounter > 0);
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createReserveTransferRequestRoundingTest() {
        long initialMailCounter = mailSender.getCounter();
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepareWithRoles();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();
        FolderModel folderTwo = prepare.getT6();

        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("50000.5")
                                                .resourceId(resourceOne.getId())
                                                .deltaUnitId(BYTES)
                                                .build())
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId(String.valueOf(folderOne.getServiceId()))
                                        .providerId(providerModel.getId())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, result.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                result.getTransfer().getRequestSubtype());
        assertEquals(TransferRequestStatusDto.PENDING, result.getTransfer().getStatus());
        assertEquals(USER_2_ID, result.getTransfer().getCreatedBy());
        assertTrue(result.getTransfer().getTransferVotes().getVotes().isEmpty());
        assertEquals(1, result.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        assertTrue(result.getTransfer().getTransferResponsible().getReserveResponsible().isPresent());
        assertEquals(result.getTransfer().getTransferResponsible().getReserveResponsible().get()
                .getProviderId(), providerModel.getId());
        assertEquals(1, result.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().size());
        assertTrue(result.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().contains(USER_1_ID));
        assertEquals(String.valueOf(PROVIDER_SERVICE_ID), result.getTransfer().getTransferResponsible()
                .getReserveResponsible().get().getServiceId());
        assertEquals(folderTwo.getId(), result.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getFolderId());
        assertEquals(2, result.getTransfer().getParameters().getQuotaTransfers().size());
        assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        long updatedMailCounter = mailSender.getCounter();
        assertTrue(updatedMailCounter - initialMailCounter > 0);
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createReserveTransferRequestZeroDeltaTest() {
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepareWithRoles();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();

        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("0")
                                                .resourceId(resourceOne.getId())
                                                .deltaUnitId(BYTES)
                                                .build())
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId(String.valueOf(folderOne.getServiceId()))
                                        .providerId(providerModel.getId())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void getTransferRequestAmountDtoTest() {
        ProviderModel provider = providerModel("in-process:test", null, false);
        ResourceTypeModel resourceType = resourceTypeModel(provider.getId(), "test",
                "b02344bf-96af-4cc5-937c-66a479989ce8");
        ResourceSegmentationModel locationSegmentation = resourceSegmentationModel(provider.getId(), "location");
        ResourceSegmentModel vlaSegment = resourceSegmentModel(locationSegmentation.getId(), "VLA");
        ResourceModel resource = resourceModel(provider.getId(), "test", resourceType.getId(),
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())),
                "b02344bf-96af-4cc5-937c-66a479989ce8",
                Set.of("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                        "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6",
                        "bb6b1e08-49a7-4cf8-b1b2-e8e71871d6d3"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", null);
        FolderModel folderOne = folderModel(1L);
        FolderModel folderTwo = folderModel(2L);
        QuotaModel quotaOne = quotaModel(provider.getId(), resource.getId(), folderOne.getId(), 500000000, 500000000);
        QuotaModel quotaTwo = quotaModel(provider.getId(), resource.getId(), folderTwo.getId(), 1500000000, 1500000000);
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> providersDao.upsertProviderRetryable(txSession, provider)))
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
        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
                                                .delta("123456000")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a") //B
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("-123456")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("bb6b1e08-49a7-4cf8-b1b2-e8e71871d6d3") //KB
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
        assertNotNull(result);
        FrontSingleTransferRequestDto getResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/transfers/{id}", result.getTransfer().getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(getResult);
        assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, getResult.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER,
                getResult.getTransfer().getRequestSubtype());
        assertEquals(TransferRequestStatusDto.PENDING, getResult.getTransfer().getStatus());
        assertEquals("193adb36-7db2-4542-875f-ef93cddbd52d", getResult.getTransfer().getCreatedBy());
        assertTrue(getResult.getTransfer().getTransferVotes().getVotes().isEmpty());
        assertEquals(2, getResult.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        assertPresent(getResult.getTransfer().getParameters().getQuotaTransfers().stream().filter(t ->
                t.getDestinationFolderId().equals(folderOne.getId())
        ).findFirst().flatMap(t -> t.getResourceTransfers().stream().filter(r ->
                r.getResourceId().equals(resource.getId())
        ).findFirst()), r -> {
            assertEquals("123456000", r.getDeltaAmount().getRawAmount());
            assertEquals("B", r.getDeltaAmount().getRawUnit());
            assertEquals("123.46", r.getDeltaAmount().getReadableAmount());
            assertEquals("MB", r.getDeltaAmount().getReadableUnit());
            assertEquals("123456000", r.getDeltaAmount().getAmountInMinAllowedUnit());
            assertEquals(BYTES, r.getDeltaAmount().getMinAllowedUnit());
            assertEquals("123.45", r.getDeltaAmount().getForEditAmount());
            assertEquals(MEGABYTES, r.getDeltaAmount().getForEditUnitId());
        });
        assertPresent(getResult.getTransfer().getParameters().getQuotaTransfers().stream().filter(t ->
                t.getDestinationFolderId().equals(folderTwo.getId())
        ).findFirst().flatMap(t -> t.getResourceTransfers().stream().filter(r ->
                r.getResourceId().equals(resource.getId())
        ).findFirst()), r -> {
            assertEquals("-123456000", r.getDeltaAmount().getRawAmount());
            assertEquals("B", r.getDeltaAmount().getRawUnit());
            assertEquals("-123.46", r.getDeltaAmount().getReadableAmount());
            assertEquals("MB", r.getDeltaAmount().getReadableUnit());
            assertEquals("-123456000", r.getDeltaAmount().getAmountInMinAllowedUnit());
            assertEquals(BYTES, r.getDeltaAmount().getMinAllowedUnit());
            assertEquals("-123.46", r.getDeltaAmount().getForEditAmount());
            assertEquals(MEGABYTES, r.getDeltaAmount().getForEditUnitId());
        });
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createReserveTransferRequestTooSmallDeltaTest() {
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepareWithRoles();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();

        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("0.5")
                                                .resourceId(resourceOne.getId())
                                                .deltaUnitId(BYTES)
                                                .build())
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId(String.valueOf(folderOne.getServiceId()))
                                        .providerId(providerModel.getId())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
    }

    @Test
    public void identicalSuggestedAmountsForDifferentResourcesNotOmittedTest() {
        QuotaModel quotaOne = quotaModel(YP_ID, YP_HDD_MAN, TEST_FOLDER_1_RESERVE_ID, 1, 1);
        QuotaModel quotaTwo = quotaModel(YP_ID, YP_SSD_MAN, TEST_FOLDER_1_RESERVE_ID, 1, 1);
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                                .upsertAllRetryable(txSession, List.of(quotaOne, quotaTwo))))
                .block();
        ErrorCollectionDto errorCollectionDto = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("1")
                                                .resourceId(YP_HDD_MAN)
                                                .deltaUnitId(GIGABYTES)
                                                .build())
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("1")
                                                .resourceId(YP_SSD_MAN)
                                                .deltaUnitId(GIGABYTES)
                                                .build())
                                        .destinationFolderId(TEST_FOLDER_1_ID)
                                        .destinationServiceId(String.valueOf(TEST_FOLDER_1_SERVICE_ID))
                                        .providerId(YP_ID)
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(errorCollectionDto);
        assertNotNull(errorCollectionDto.getDetails());

        Set<Object> ypHddSuggestedAmount = errorCollectionDto.getDetails().get(YP_HDD_MAN + ".suggestedAmount");
        Set<Object> ypSsdSuggestedAmount = errorCollectionDto.getDetails().get(YP_SSD_MAN + ".suggestedAmount");
        assertEquals(1, ypHddSuggestedAmount.size());
        assertEquals(1, ypSsdSuggestedAmount.size());
    }

    @Test
    public void reserveTransferNotShowSuggestedAmountIfNegativeBalanceTest() {
        QuotaModel quotaNegative = quotaModel(YP_ID, YP_HDD_MAN, TEST_FOLDER_1_RESERVE_ID, 0, -10);
        QuotaModel quotaZero = quotaModel(YP_ID, YP_SSD_MAN, TEST_FOLDER_1_RESERVE_ID, 0, 0);
        QuotaModel quotaPositive = quotaModel(YP_ID, YP_CPU_MAN, TEST_FOLDER_1_RESERVE_ID, 10, 10);
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .upsertAllRetryable(txSession, List.of(quotaNegative, quotaZero, quotaPositive))))
                .block();
        ErrorCollectionDto errorCollectionDto = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("100")
                                                .resourceId(YP_HDD_MAN)
                                                .deltaUnitId(GIGABYTES)
                                                .build())
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("100")
                                                .resourceId(YP_SSD_MAN)
                                                .deltaUnitId(GIGABYTES)
                                                .build())
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("100")
                                                .resourceId(YP_CPU_MAN)
                                                .deltaUnitId(CORES)
                                                .build())
                                        .destinationFolderId(TEST_FOLDER_1_ID)
                                        .destinationServiceId(String.valueOf(TEST_FOLDER_1_SERVICE_ID))
                                        .providerId(YP_ID)
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(errorCollectionDto);
        assertNotNull(errorCollectionDto.getDetails());

        assertNull(errorCollectionDto.getDetails().get(YP_HDD_MAN + ".suggestedAmount"));
        assertNull(errorCollectionDto.getDetails().get(YP_SSD_MAN + ".suggestedAmount"));

        assertNotNull(errorCollectionDto.getDetails().get(YP_CPU_MAN + ".suggestedAmount"));
        assertEquals(1, errorCollectionDto.getDetails().get(YP_CPU_MAN + ".suggestedAmount").size());
    }

    @Test
    public void quotaTransferNotShowSuggestedAmountIfNegativeBalanceTest() {
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
        QuotaModel quotaTwo = quotaModel(provider.getId(), resource.getId(), folderTwo.getId(), 150, 0);
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourceTypesDao
                        .upsertResourceTypeRetryable(txSession, resourceType))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourceSegmentationsDao
                        .upsertResourceSegmentationRetryable(txSession, locationSegmentation))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourceSegmentsDao
                        .upsertResourceSegmentRetryable(txSession, vlaSegment))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourcesDao
                        .upsertResourceRetryable(txSession, resource))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertAllRetryable(txSession, List.of(folderOne, folderTwo)))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .upsertAllRetryable(txSession, List.of(quotaOne, quotaTwo)))).block();
        ErrorCollectionDto errorCollectionDto = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
                                                .delta("200")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("-200")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        assertNull(errorCollectionDto.getDetails().get(resource.getId() + ".suggestedAmount"));

        ErrorCollectionDto errorCollectionDto2 = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
                                                .delta("-200")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("200")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(errorCollectionDto2.getDetails().get(resource.getId() + ".suggestedAmount"));
        assertEquals(1, errorCollectionDto2.getDetails().get(resource.getId() + ".suggestedAmount").size());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void putAndConfirmWithoutRightsTransferRequestTest() {
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
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                        .upsertProviderRetryable(txSession, provider))).block();
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
        Assertions.assertNotNull(putResult);
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, putResult.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER,
                result.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, putResult.getTransfer().getStatus());
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                                TransactionMode.SERIALIZABLE_READ_WRITE,
                                txSession -> pendingTransferRequestsDao.getById(txSession,
                                        result.getTransfer().getId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Assertions.assertNotNull(pendingTransferRequest);
        Assertions.assertEquals(
                Optional.of(result.getTransfer().getId()),
                pendingTransferRequest.map(PendingTransferRequestsModel::getRequestId));
        Assertions.assertEquals(USER_2_ID, putResult.getTransfer().getCreatedBy());
        Assertions.assertTrue(putResult.getTransfer().getTransferVotes().getVotes().isEmpty());
        Assertions.assertEquals(2, putResult.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId())));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("2"))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
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
}
