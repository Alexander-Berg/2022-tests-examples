package ru.yandex.intranet.d.web.front.transfer;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.folders.FolderDao;
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao;
import ru.yandex.intranet.d.dao.providers.ProvidersDao;
import ru.yandex.intranet.d.dao.quotas.QuotasDao;
import ru.yandex.intranet.d.dao.resources.ResourcesDao;
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao;
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao;
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao;
import ru.yandex.intranet.d.dao.transfers.PendingTransferRequestsDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
import ru.yandex.intranet.d.model.folders.FolderType;
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.model.resources.ResourceModel;
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel;
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel;
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel;
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel;
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel;
import ru.yandex.intranet.d.model.transfers.PendingTransferRequestsModel;
import ru.yandex.intranet.d.services.notifications.NotificationMailSenderStub;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.SortOrderDto;
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
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestSearchDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestsPageDto;

import static ru.yandex.intranet.d.TestUsers.USER_2_ID;
import static ru.yandex.intranet.d.TestUsers.USER_2_UID;

/**
 * Front transfer requests API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class FrontTransferRequestsControllerTest {

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
    private FolderOperationLogDao folderOperationLogDao;
    @Autowired
    private FolderDao folderDao;
    @Autowired
    private YdbTableClient tableClient;
    @Autowired
    private NotificationMailSenderStub mailSender;
    @Autowired
    private PendingTransferRequestsDao pendingTransferRequestsDao;

    @Test
    @SuppressWarnings("MethodLength")
    public void getTransferRequestTest() {
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
        Assertions.assertNotNull(result);
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
        Assertions.assertNotNull(getResult);
        Assertions.assertEquals(resourceType.getId(),
                getResult.getResources().get(resource.getId()).getResourceTypeId());
        Assertions.assertEquals(resourceType.getNameEn(),
                getResult.getResourceTypes().get(resourceType.getId()).getName());
        Assertions.assertEquals(vlaSegment.getId(),
                Objects.requireNonNull(getResult.getResources().get(resource.getId()).getSegments())
                        .get(locationSegmentation.getId()));
        Assertions.assertEquals(vlaSegment.getNameEn(), getResult.getSegments().get(vlaSegment.getId()).getName());
        Assertions.assertEquals(locationSegmentation.getNameEn(),
                getResult.getSegmentations().get(locationSegmentation.getId()).getName());
        Assertions.assertEquals(locationSegmentation.getGroupingOrder(),
                getResult.getSegmentations().get(locationSegmentation.getId()).getGroupingOrder());
        Assertions.assertEquals(resourceType.getNameEn(),
                getResult.getResourceTypes().get(resourceType.getId()).getName());
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, getResult.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER,
                getResult.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, getResult.getTransfer().getStatus());
        Assertions.assertEquals("193adb36-7db2-4542-875f-ef93cddbd52d", getResult.getTransfer().getCreatedBy());
        Assertions.assertTrue(getResult.getTransfer().getTransferVotes().getVotes().isEmpty());
        Assertions.assertEquals(2, getResult.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(getResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(getResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId())));
        Assertions.assertTrue(getResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        Assertions.assertTrue(getResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        Assertions.assertTrue(getResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        Assertions.assertTrue(getResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        Assertions.assertTrue(getResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(getResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(getResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("2"))));
        Assertions.assertTrue(getResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("2"))));
        Assertions.assertEquals(2, getResult.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(getResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals("1")));
        Assertions.assertTrue(getResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals("2")));
        Assertions.assertTrue(getResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
        Assertions.assertTrue(getResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("-100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createTransferRequestTest() {
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
        Assertions.assertNotNull(result);
        Assertions.assertEquals(resourceType.getId(),
                result.getResources().get(resource.getId()).getResourceTypeId());
        Assertions.assertEquals(resourceType.getNameEn(),
                result.getResourceTypes().get(resourceType.getId()).getName());
        Assertions.assertEquals(vlaSegment.getId(),
                Objects.requireNonNull(result.getResources().get(resource.getId()).getSegments())
                        .get(locationSegmentation.getId()));
        Assertions.assertEquals(vlaSegment.getNameEn(), result.getSegments().get(vlaSegment.getId()).getName());
        Assertions.assertEquals(locationSegmentation.getNameEn(),
                result.getSegmentations().get(locationSegmentation.getId()).getName());
        Assertions.assertEquals(locationSegmentation.getGroupingOrder(),
                result.getSegmentations().get(locationSegmentation.getId()).getGroupingOrder());
        Assertions.assertEquals(resourceType.getNameEn(),
                result.getResourceTypes().get(resourceType.getId()).getName());
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, result.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER,
                result.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, result.getTransfer().getStatus());
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
        Assertions.assertEquals("193adb36-7db2-4542-875f-ef93cddbd52d", result.getTransfer().getCreatedBy());
        Assertions.assertTrue(result.getTransfer().getTransferVotes().getVotes().isEmpty());
        Assertions.assertEquals(2, result.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId())));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("2"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("2"))));
        Assertions.assertEquals(2, result.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals("1")));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals("2")));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("-100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
        long updatedMailCounter = mailSender.getCounter();
        Assertions.assertTrue(updatedMailCounter - initialMailCounter > 0);
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createAndConfirmTransferRequestTest() {
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
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                        .addConfirmation(true)
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
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, result.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER,
                result.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, result.getTransfer().getStatus());
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> pendingTransferRequestsDao.getById(txSession,
                                result.getTransfer().getId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Assertions.assertNotNull(pendingTransferRequest);
        Assertions.assertEquals(
                Optional.empty(),
                pendingTransferRequest.map(PendingTransferRequestsModel::getRequestId));
        Assertions.assertEquals("193adb36-7db2-4542-875f-ef93cddbd52d", result.getTransfer().getCreatedBy());
        Assertions.assertEquals(1, result.getTransfer().getTransferVotes().getVotes().size());
        Assertions.assertEquals("193adb36-7db2-4542-875f-ef93cddbd52d",
                result.getTransfer().getTransferVotes().getVotes().get(0).getUserId());
        Assertions.assertEquals(TransferRequestVoteTypeDto.CONFIRM,
                result.getTransfer().getTransferVotes().getVotes().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId(), folderTwo.getId()),
                new HashSet<>(result.getTransfer().getTransferVotes().getVotes().get(0).getFolderIds()));
        Assertions.assertEquals(Set.of("1", "2"),
                new HashSet<>(result.getTransfer().getTransferVotes().getVotes().get(0).getServiceIds()));
        Assertions.assertEquals(2, result.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId())));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("2"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("2"))));
        Assertions.assertEquals(2, result.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals("1")));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals("2")));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("-100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
        List<QuotaModel> quotasOne = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderOne.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        List<QuotaModel> quotasTwo = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderTwo.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertEquals(150, quotasOne.get(0).getQuota());
        Assertions.assertEquals(150, quotasOne.get(0).getBalance());
        Assertions.assertEquals(50, quotasTwo.get(0).getQuota());
        Assertions.assertEquals(50, quotasTwo.get(0).getBalance());
        List<FolderOperationLogModel> logsOne = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        folderOperationLogDao.getPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folderOne.getId(),
                                null, SortOrderDto.ASC, 100)))
                .block();
        List<FolderOperationLogModel> logsTwo = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        folderOperationLogDao.getPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folderTwo.getId(),
                                null, SortOrderDto.ASC, 100))).block();
        Assertions.assertEquals(1, logsOne.size());
        Assertions.assertEquals(1, logsTwo.size());
        long updatedMailCounter = mailSender.getCounter();
        Assertions.assertEquals(0L, updatedMailCounter - initialMailCounter);
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createAndConfirmWithoutRightsTransferRequestTest() {
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
                        .addConfirmation(true)
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
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, result.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER,
                result.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, result.getTransfer().getStatus());
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
        Assertions.assertEquals(USER_2_ID, result.getTransfer().getCreatedBy());
        Assertions.assertTrue(result.getTransfer().getTransferVotes().getVotes().isEmpty());
        Assertions.assertEquals(2, result.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId())));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("2"))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("2"))));
        Assertions.assertEquals(2, result.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals("1")));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals("2")));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("-100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void putTransferRequestTest() {
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
        Assertions.assertNotNull(result);
        FrontSingleTransferRequestDto putResult = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .put()
                .uri("/front/transfers/{id}?version={version}", result.getTransfer().getId(),
                        result.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontPutTransferRequestDto.builder()
                        .addConfirmation(false)
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
        Assertions.assertEquals(vlaSegment.getId(),
                Objects.requireNonNull(putResult.getResources().get(resource.getId()).getSegments())
                        .get(locationSegmentation.getId()));
        Assertions.assertEquals(vlaSegment.getNameEn(), putResult.getSegments().get(vlaSegment.getId()).getName());
        Assertions.assertEquals(locationSegmentation.getNameEn(),
                putResult.getSegmentations().get(locationSegmentation.getId()).getName());
        Assertions.assertEquals(locationSegmentation.getGroupingOrder(),
                putResult.getSegmentations().get(locationSegmentation.getId()).getGroupingOrder());
        Assertions.assertEquals(resourceType.getNameEn(),
                putResult.getResourceTypes().get(resourceType.getId()).getName());
        Assertions.assertEquals(resourceType.getNameEn(),
                putResult.getResourceTypes().get(resourceType.getId()).getName());
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
        Assertions.assertEquals("193adb36-7db2-4542-875f-ef93cddbd52d", putResult.getTransfer().getCreatedBy());
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

    @Test
    @SuppressWarnings("MethodLength")
    public void putAndConfirmTransferRequestTest() {
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
        Assertions.assertNotNull(result);
        FrontSingleTransferRequestDto putResult = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, putResult.getTransfer().getStatus());
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> pendingTransferRequestsDao.getById(txSession,
                                result.getTransfer().getId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Assertions.assertNotNull(pendingTransferRequest);
        Assertions.assertEquals(
                Optional.empty(),
                pendingTransferRequest.map(PendingTransferRequestsModel::getRequestId));
        Assertions.assertEquals("193adb36-7db2-4542-875f-ef93cddbd52d", putResult.getTransfer().getCreatedBy());
        Assertions.assertEquals(1, putResult.getTransfer().getTransferVotes().getVotes().size());
        Assertions.assertEquals("193adb36-7db2-4542-875f-ef93cddbd52d",
                putResult.getTransfer().getTransferVotes().getVotes().get(0).getUserId());
        Assertions.assertEquals(TransferRequestVoteTypeDto.CONFIRM,
                putResult.getTransfer().getTransferVotes().getVotes().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId(), folderTwo.getId()),
                new HashSet<>(putResult.getTransfer().getTransferVotes().getVotes().get(0).getFolderIds()));
        Assertions.assertEquals(Set.of("1", "2"),
                new HashSet<>(putResult.getTransfer().getTransferVotes().getVotes().get(0).getServiceIds()));
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
        List<QuotaModel> quotasOne = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderOne.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        List<QuotaModel> quotasTwo = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderTwo.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertEquals(140, quotasOne.get(0).getQuota());
        Assertions.assertEquals(140, quotasOne.get(0).getBalance());
        Assertions.assertEquals(60, quotasTwo.get(0).getQuota());
        Assertions.assertEquals(60, quotasTwo.get(0).getBalance());
        List<FolderOperationLogModel> logsOne = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        folderOperationLogDao.getPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folderOne.getId(),
                                null, SortOrderDto.ASC, 100)))
                .block();
        List<FolderOperationLogModel> logsTwo = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        folderOperationLogDao.getPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folderTwo.getId(),
                                null, SortOrderDto.ASC, 100)))
                .block();
        Assertions.assertEquals(1, logsOne.size());
        Assertions.assertEquals(1, logsTwo.size());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void cancelTransferRequestTest() {
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
        Assertions.assertNotNull(result);
        FrontSingleTransferRequestDto cancelResult = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
        Assertions.assertNotNull(cancelResult);
        Assertions.assertEquals(vlaSegment.getId(),
                Objects.requireNonNull(cancelResult.getResources().get(resource.getId()).getSegments())
                        .get(locationSegmentation.getId()));
        Assertions.assertEquals(vlaSegment.getNameEn(), cancelResult.getSegments().get(vlaSegment.getId()).getName());
        Assertions.assertEquals(locationSegmentation.getNameEn(),
                cancelResult.getSegmentations().get(locationSegmentation.getId()).getName());
        Assertions.assertEquals(locationSegmentation.getGroupingOrder(),
                cancelResult.getSegmentations().get(locationSegmentation.getId()).getGroupingOrder());
        Assertions.assertEquals(resourceType.getNameEn(),
                cancelResult.getResourceTypes().get(resourceType.getId()).getName());
        Assertions.assertEquals(resourceType.getNameEn(),
                cancelResult.getResourceTypes().get(resourceType.getId()).getName());
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, cancelResult.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER,
                result.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.CANCELLED, cancelResult.getTransfer().getStatus());
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> pendingTransferRequestsDao.getById(txSession,
                                result.getTransfer().getId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Assertions.assertNotNull(pendingTransferRequest);
        Assertions.assertEquals(
                Optional.empty(),
                pendingTransferRequest.map(PendingTransferRequestsModel::getRequestId));
        Assertions.assertEquals("193adb36-7db2-4542-875f-ef93cddbd52d", cancelResult.getTransfer().getCreatedBy());
        Assertions.assertTrue(cancelResult.getTransfer().getTransferVotes().getVotes().isEmpty());
        Assertions.assertEquals(2, cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId())));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("2"))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("2"))));
        Assertions.assertEquals(2, cancelResult.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(cancelResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals("1")));
        Assertions.assertTrue(cancelResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals("2")));
        Assertions.assertTrue(cancelResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
        Assertions.assertTrue(cancelResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("-100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void searchTransferRequestTest() {
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
        Assertions.assertNotNull(result);
        FrontTransferRequestsPageDto searchResultByCurrentUser = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
        Assertions.assertNotNull(searchResultByCurrentUser);
        FrontTransferRequestsPageDto searchResultByFolder = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
        Assertions.assertNotNull(searchResultByFolder);
        FrontTransferRequestsPageDto searchResultByService = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers/_search")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestSearchDto.builder()
                        .filterByServiceId("1")
                        .addFilterByStatus(TransferRequestStatusDto.PENDING)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontTransferRequestsPageDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(searchResultByService);
        Assertions.assertEquals(vlaSegment.getId(),
                Objects.requireNonNull(searchResultByService.getResources().get(resource.getId()).getSegments())
                        .get(locationSegmentation.getId()));
        Assertions.assertEquals(vlaSegment.getNameEn(),
                searchResultByService.getSegments().get(vlaSegment.getId()).getName());
        Assertions.assertEquals(locationSegmentation.getNameEn(),
                searchResultByService.getSegmentations().get(locationSegmentation.getId()).getName());
        Assertions.assertEquals(locationSegmentation.getGroupingOrder(),
                searchResultByService.getSegmentations().get(locationSegmentation.getId()).getGroupingOrder());
        Assertions.assertEquals(resourceType.getNameEn(),
                searchResultByService.getResourceTypes().get(resourceType.getId()).getName());
        FrontTransferRequestDto resultOne = searchResultByCurrentUser.getTransfers().get(0);
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, resultOne.getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER,
                result.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, resultOne.getStatus());
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
        Assertions.assertEquals("193adb36-7db2-4542-875f-ef93cddbd52d", resultOne.getCreatedBy());
        Assertions.assertTrue(resultOne.getTransferVotes().getVotes().isEmpty());
        Assertions.assertEquals(2, resultOne.getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(resultOne.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(resultOne.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId())));
        Assertions.assertTrue(resultOne.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        Assertions.assertTrue(resultOne.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        Assertions.assertTrue(resultOne.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        Assertions.assertTrue(resultOne.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        Assertions.assertTrue(resultOne.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(resultOne.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(resultOne.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("2"))));
        Assertions.assertTrue(resultOne.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("2"))));
        Assertions.assertEquals(2, resultOne.getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(resultOne.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals("1")));
        Assertions.assertTrue(resultOne.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals("2")));
        Assertions.assertTrue(resultOne.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
        Assertions.assertTrue(resultOne.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("-100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
        FrontTransferRequestDto resultTwo = searchResultByFolder.getTransfers().get(0);
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, resultTwo.getRequestType());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, resultTwo.getStatus());
        Assertions.assertEquals("193adb36-7db2-4542-875f-ef93cddbd52d", result.getTransfer().getCreatedBy());
        Assertions.assertTrue(resultTwo.getTransferVotes().getVotes().isEmpty());
        Assertions.assertEquals(2, resultTwo.getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(resultTwo.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(resultTwo.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId())));
        Assertions.assertTrue(resultTwo.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        Assertions.assertTrue(resultTwo.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        Assertions.assertTrue(resultTwo.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        Assertions.assertTrue(resultTwo.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        Assertions.assertTrue(resultTwo.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(resultTwo.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(resultTwo.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("2"))));
        Assertions.assertTrue(resultTwo.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("2"))));
        Assertions.assertEquals(2, resultTwo.getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(resultTwo.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals("1")));
        Assertions.assertTrue(resultTwo.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals("2")));
        Assertions.assertTrue(resultTwo.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
        Assertions.assertTrue(resultTwo.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("-100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
        FrontTransferRequestDto resultThree = searchResultByService.getTransfers().get(0);
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, resultThree.getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER,
                result.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, resultThree.getStatus());
        Assertions.assertEquals("193adb36-7db2-4542-875f-ef93cddbd52d", resultThree.getCreatedBy());
        Assertions.assertTrue(resultThree.getTransferVotes().getVotes().isEmpty());
        Assertions.assertEquals(2, resultThree.getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(resultThree.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(resultThree.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId())));
        Assertions.assertTrue(resultThree.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        Assertions.assertTrue(resultThree.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        Assertions.assertTrue(resultThree.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d"))));
        Assertions.assertTrue(resultThree.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd"))));
        Assertions.assertTrue(resultThree.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(resultThree.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("1"))));
        Assertions.assertTrue(resultThree.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("193adb36-7db2-4542-875f-ef93cddbd52d")
                                && r.getServiceIds().contains("2"))));
        Assertions.assertTrue(resultThree.getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderTwo.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals("d50e7f1a-5e87-4ec5-8e28-7b9c8caab4bd")
                                && r.getServiceIds().contains("2"))));
        Assertions.assertEquals(2, resultThree.getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(resultThree.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals("1")));
        Assertions.assertTrue(resultThree.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals("2")));
        Assertions.assertTrue(resultThree.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
        Assertions.assertTrue(resultThree.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("-100") && r.getDeltaUnit().equals("B")
                        && r.getDeltaUnitId().equals("b15101c2-da50-4d6f-9a8e-b90160871b0a"))));
    }

    static ProviderModel providerModel(String grpcUri, String restUri, boolean accountsSpacesSupported) {
        return providerModel(grpcUri, restUri, accountsSpacesSupported, 69L);
    }

    static ProviderModel providerModel(String grpcUri, String restUri, boolean accountsSpacesSupported,
                                       long serviceId) {
        return providerModelBuilder(grpcUri, restUri, accountsSpacesSupported, serviceId).build();
    }

    static ProviderModel.Builder providerModelBuilder(String grpcUri, String restUri, boolean accountsSpacesSupported,
                                                      long serviceId) {
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
                .serviceId(serviceId)
                .deleted(false)
                .readOnly(false)
                .multipleAccountsPerFolder(true)
                .accountTransferWithQuota(true)
                .managed(true)
                .key("test")
                .trackerComponentId(1L)
                .accountsSettings(AccountsSettingsModel.builder()
                        .displayNameSupported(true)
                        .keySupported(true)
                        .deleteSupported(true)
                        .softDeleteSupported(true)
                        .moveSupported(true)
                        .renameSupported(true)
                        .perAccountVersionSupported(true)
                        .perProvisionVersionSupported(true)
                        .perAccountLastUpdateSupported(true)
                        .perProvisionLastUpdateSupported(true)
                        .operationIdDeduplicationSupported(true)
                        .syncCoolDownDisabled(false)
                        .retryCoolDownDisabled(false)
                        .accountsSyncPageSize(1000L)
                        .build())
                .importAllowed(true)
                .allocatedSupported(true)
                .accountsSpacesSupported(accountsSpacesSupported).syncEnabled(true).grpcTlsOn(true);
    }

    public static ResourceTypeModel resourceTypeModel(String providerId, String key, String unitsEnsembleId) {
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

    public static ResourceSegmentationModel resourceSegmentationModel(String providerId, String key) {
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

    public static ResourceSegmentModel resourceSegmentModel(String segmentationId, String key) {
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

    @SuppressWarnings("ParameterNumber")
    public static ResourceModel resourceModel(String providerId, String key, String resourceTypeId,
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

    public static FolderModel folderModel(long serviceId) {
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
                .setNextOpLogOrder(1L)
                .build();
    }

    public static QuotaModel quotaModel(String providerId, String resourceId, String folderId, long quota,
                                        long balance) {
        return QuotaModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .providerId(providerId)
                .resourceId(resourceId)
                .folderId(folderId)
                .quota(quota)
                .balance(balance)
                .frozenQuota(0L)
                .build();
    }
}
