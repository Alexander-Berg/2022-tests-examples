package ru.yandex.intranet.d.web.front.transfer;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import com.google.common.collect.Sets;
import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestServices;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.UnitIds;
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
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.model.resources.ResourceModel;
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel;
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel;
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel;
import ru.yandex.intranet.d.model.services.ServiceReadOnlyState;
import ru.yandex.intranet.d.model.services.ServiceRecipeModel;
import ru.yandex.intranet.d.model.services.ServiceState;
import ru.yandex.intranet.d.model.transfers.PendingTransferRequestsModel;
import ru.yandex.intranet.d.services.notifications.NotificationMailSenderStub;
import ru.yandex.intranet.d.services.validators.AbcServiceValidator;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.SortOrderDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestStatusDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestSubtypeDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestVoteTypeDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaResourceTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateReserveTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestParametersDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontPutTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontSingleTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestHistoryEventsPageDto;

import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_3_ID;
import static ru.yandex.intranet.d.TestProviders.CLAUD2_ID;
import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestResources.YP_HDD_MAN;
import static ru.yandex.intranet.d.TestResources.YP_HDD_SAS;
import static ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_D;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.folderModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.providerModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.providerModelBuilder;
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
public class MoreFrontTransferRequestsControllerTest {
    private static final Random RANDOM = new Random();

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
    private YdbTableClient tableClient;
    @Autowired
    private NotificationMailSenderStub mailSender;
    @Autowired
    private PendingTransferRequestsDao pendingTransferRequestsDao;

    @Test
    @SuppressWarnings("MethodLength")
    public void getTransferRequestHistoryPutTest() {
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
        Assertions.assertNotNull(historyResult);
        Assertions.assertEquals(vlaSegment.getId(),
                Objects.requireNonNull(historyResult.getResources().get(resource.getId()).getSegments())
                        .get(locationSegmentation.getId()));
        Assertions.assertEquals(vlaSegment.getNameEn(), historyResult.getSegments().get(vlaSegment.getId()).getName());
        Assertions.assertEquals(locationSegmentation.getNameEn(),
                historyResult.getSegmentations().get(locationSegmentation.getId()).getName());
        Assertions.assertEquals(locationSegmentation.getGroupingOrder(),
                historyResult.getSegmentations().get(locationSegmentation.getId()).getGroupingOrder());
        Assertions.assertEquals(resourceType.getNameEn(),
                historyResult.getResourceTypes().get(resourceType.getId()).getName());
        Assertions.assertEquals(resourceType.getNameEn(),
                historyResult.getResourceTypes().get(resourceType.getId()).getName());
        Assertions.assertEquals(2, historyResult.getEvents().size());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createTransferRequestProviderResponsibleAutoConfirmTest() {
        long initialMailCounter = mailSender.getCounter();
        ProviderModel provider = providerModel("in-process:test", null, false, 1L);
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
                .mutateWith(MockUser.uid("1120000000000003"))
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
        Assertions.assertEquals("0491ab05-a324-4d5d-9a25-0988b39060cd", result.getTransfer().getCreatedBy());
        Assertions.assertEquals(1, result.getTransfer().getTransferVotes().getVotes().size());
        Assertions.assertEquals("0491ab05-a324-4d5d-9a25-0988b39060cd",
                result.getTransfer().getTransferVotes().getVotes().get(0).getUserId());
        Assertions.assertEquals(TransferRequestVoteTypeDto.CONFIRM,
                result.getTransfer().getTransferVotes().getVotes().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId(), folderTwo.getId()),
                new HashSet<>(result.getTransfer().getTransferVotes().getVotes().get(0).getFolderIds()));
        Assertions.assertEquals(Set.of(),
                new HashSet<>(result.getTransfer().getTransferVotes().getVotes().get(0).getServiceIds()));
        Assertions.assertEquals(Set.of(provider.getId()),
                new HashSet<>(result.getTransfer().getTransferVotes().getVotes().get(0).getProviderIds()));
        Assertions.assertEquals(0, result.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertEquals(1, result.getTransfer().getTransferResponsible().getProviderResponsible().size());
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getProviderResponsible().stream()
                .anyMatch(g -> g.getResponsibleId().equals("0491ab05-a324-4d5d-9a25-0988b39060cd")));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getProviderResponsible().stream()
                .anyMatch(g -> g.getProviderIds().contains(provider.getId())));
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
                                null, SortOrderDto.ASC, 100)))
                .block();
        Assertions.assertEquals(1, logsOne.size());
        Assertions.assertEquals(1, logsTwo.size());
        long updatedMailCounter = mailSender.getCounter();
        Assertions.assertEquals(0L, updatedMailCounter - initialMailCounter);
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createTransferRequestInvalidBalanceTest() {
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
                                                .delta("160")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("-160")
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
        Assertions.assertNotNull(result);

    }

    @Test
    public void createTransferRequestToClosingServiceTest() {
        FrontCreateTransferRequestDto body = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .addConfirmation(true)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_IN_CLOSING_SERVICE)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_CLOSING))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TEST_FOLDER_1_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-1")
                                        .resourceId(YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(expectedResult("parameters.quotaTransfers.0.destinationServiceId",
                        "Current service status is not allowed."));
    }

    @Test
    public void createTransferRequestFromClosingServiceTest() {
        FrontCreateTransferRequestDto body = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .addConfirmation(true)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_IN_CLOSING_SERVICE)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_CLOSING))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-1")
                                        .resourceId(YP_HDD_SAS)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(YP_HDD_SAS)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void createTransferRequestToNonExportableServiceTest() {
        FrontCreateTransferRequestDto body = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .addConfirmation(true)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_IN_NON_EXPORTABLE_SERVICE)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_NON_EXPORTABLE))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TEST_FOLDER_1_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-1")
                                        .resourceId(YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(expectedResult("parameters.quotaTransfers.0.destinationServiceId",
                        "Services in the sandbox are not allowed."));
    }

    @Test
    public void createTransferRequestFromNonExportableServiceTest() {
        FrontCreateTransferRequestDto body = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .addConfirmation(true)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_IN_NON_EXPORTABLE_SERVICE)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_NON_EXPORTABLE))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-1")
                                        .resourceId(YP_HDD_SAS)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(YP_HDD_SAS)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void createTransferRequestToRenamingServiceTest() {
        FrontCreateTransferRequestDto body = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .addConfirmation(true)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_IN_RENAMING_SERVICE)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_RENAMING))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TEST_FOLDER_1_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-1")
                                        .resourceId(YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void createTransferRequestFromRenamingServiceTest() {
        FrontCreateTransferRequestDto body = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .addConfirmation(true)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_IN_RENAMING_SERVICE)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_RENAMING))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-1")
                                        .resourceId(YP_HDD_SAS)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(YP_HDD_SAS)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void createTransferRequestWithDecimalDeltasRuTest() {
        testCreationForLang("ru");
    }

    @Test
    public void createTransferRequestWithDecimalDeltasEnTest() {
        testCreationForLang("en");
    }

    @SuppressWarnings("MethodLength")
    private void testCreationForLang(String lang) {
        ProviderModel provider = providerModel("in-process:test", null, false);
        ResourceTypeModel resourceType = resourceTypeModel(provider.getId(), "test",
                "9c8fcba0-a6b7-4fcc-a736-7ebdec1fb595");
        ResourceSegmentationModel locationSegmentation = resourceSegmentationModel(provider.getId(), "location");
        ResourceSegmentModel vlaSegment = resourceSegmentModel(locationSegmentation.getId(), "VLA");
        ResourceModel resource = resourceModel(provider.getId(), "test", resourceType.getId(),
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())),
                "9c8fcba0-a6b7-4fcc-a736-7ebdec1fb595",
                Set.of("8056ee12-2222-4e6c-a9da-4653f14ecc71", "b897b313-6d68-4a44-bf61-7e228458c5f5",
                        "e6b442ab-84a3-43e9-bfb6-50692c4252ed", "1750b8fa-ef06-47a1-be67-72122e7db33d",
                        "b4606b30-1999-465f-adfc-c7b990028dd6"),
                "b897b313-6d68-4a44-bf61-7e228458c5f5", "b4606b30-1999-465f-adfc-c7b990028dd6", null);
        FolderModel folderOne = folderModel(1L);
        FolderModel folderTwo = folderModel(2L);
        QuotaModel quotaOne = quotaModel(provider.getId(), resource.getId(), folderOne.getId(), 0, 0);
        QuotaModel quotaTwo = quotaModel(provider.getId(), resource.getId(), folderTwo.getId(), 1025, 1025);
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
        String json = "{\n" +
                "    \"requestType\": \"QUOTA_TRANSFER\",\n" +
                "    \"parameters\":\n" +
                "    {\n" +
                "        \"quotaTransfers\":\n" +
                "        [\n" +
                "            {\n" +
                "                \"destinationServiceId\": 2,\n" +
                "                \"destinationFolderId\": \"" + folderTwo.getId() + "\",\n" +
                "                \"resourceTransfers\":\n" +
                "                [\n" +
                "                    {\n" +
                "                        \"resourceId\": \"" + resource.getId() + "\",\n" +
                "                        \"delta\": \"-0.5\",\n" +
                "                        \"deltaUnitId\": \"b897b313-6d68-4a44-bf61-7e228458c5f5\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"destinationServiceId\": 1,\n" +
                "                \"destinationFolderId\": \"" + folderOne.getId() + "\",\n" +
                "                \"resourceTransfers\":\n" +
                "                [\n" +
                "                    {\n" +
                "                        \"resourceId\": \"" + resource.getId() + "\",\n" +
                "                        \"delta\": \"0.5\",\n" +
                "                        \"deltaUnitId\": \"b897b313-6d68-4a44-bf61-7e228458c5f5\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            }\n" +
                "        ]\n" +
                "    },\n" +
                "    \"addConfirmation\": false,\n" +
                "    \"summary\": \"test\",\n" +
                "    \"description\": \"\"\n" +
                "}";
        testCreation(json, folderOne, folderTwo, resource, mailSender.getCounter(), lang);

        json = "{\n" +
                "    \"requestType\": \"QUOTA_TRANSFER\",\n" +
                "    \"parameters\":\n" +
                "    {\n" +
                "        \"quotaTransfers\":\n" +
                "        [\n" +
                "            {\n" +
                "                \"destinationServiceId\": 2,\n" +
                "                \"destinationFolderId\": \"" + folderTwo.getId() + "\",\n" +
                "                \"resourceTransfers\":\n" +
                "                [\n" +
                "                    {\n" +
                "                        \"resourceId\": \"" + resource.getId() + "\",\n" +
                "                        \"delta\": \"-0,5\",\n" +
                "                        \"deltaUnitId\": \"b897b313-6d68-4a44-bf61-7e228458c5f5\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"destinationServiceId\": 1,\n" +
                "                \"destinationFolderId\": \"" + folderOne.getId() + "\",\n" +
                "                \"resourceTransfers\":\n" +
                "                [\n" +
                "                    {\n" +
                "                        \"resourceId\": \"" + resource.getId() + "\",\n" +
                "                        \"delta\": \"0,5\",\n" +
                "                        \"deltaUnitId\": \"b897b313-6d68-4a44-bf61-7e228458c5f5\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            }\n" +
                "        ]\n" +
                "    },\n" +
                "    \"addConfirmation\": false,\n" +
                "    \"summary\": \"test\",\n" +
                "    \"description\": \"\"\n" +
                "}";
        testCreation(json, folderOne, folderTwo, resource, mailSender.getCounter(), lang);
    }

    @SuppressWarnings("MethodLength")
    private void testCreation(String json, FolderModel folderOne, FolderModel folderTwo, ResourceModel resource,
                              long initialMailCounter, String lang) {
        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers?lang=" + lang)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
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
                        && r.getDelta().equals("512") && r.getDeltaUnit().equals("MiB")
                        && r.getDeltaUnitId().equals("b4606b30-1999-465f-adfc-c7b990028dd6"))));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("-512") && r.getDeltaUnit().equals("MiB")
                        && r.getDeltaUnitId().equals("b4606b30-1999-465f-adfc-c7b990028dd6"))));
        long updatedMailCounter = mailSender.getCounter();
        Assertions.assertTrue(updatedMailCounter - initialMailCounter > 0);

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
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, getResult.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER,
                getResult.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, getResult.getTransfer().getStatus());

        Assertions.assertTrue(getResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("512") && r.getDeltaUnit().equals("MiB")
                        && r.getDeltaUnitId().equals("b4606b30-1999-465f-adfc-c7b990028dd6"))));
        Assertions.assertTrue(getResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("-512") && r.getDeltaUnit().equals("MiB")
                        && r.getDeltaUnitId().equals("b4606b30-1999-465f-adfc-c7b990028dd6"))));
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void putTransferRequestWithDeltasRuTest() {
        testUpdateForLang("ru");
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void putTransferRequestWithDeltasEnTest() {
        testUpdateForLang("en");
    }

    @SuppressWarnings("MethodLength")
    private void testUpdateForLang(String lang) {
        ProviderModel provider = providerModel("in-process:test", null, false);
        ResourceTypeModel resourceType = resourceTypeModel(provider.getId(), "test",
                "9c8fcba0-a6b7-4fcc-a736-7ebdec1fb595");
        ResourceSegmentationModel locationSegmentation = resourceSegmentationModel(provider.getId(), "location");
        ResourceSegmentModel vlaSegment = resourceSegmentModel(locationSegmentation.getId(), "VLA");
        ResourceModel resource = resourceModel(provider.getId(), "test", resourceType.getId(),
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())),
                "9c8fcba0-a6b7-4fcc-a736-7ebdec1fb595",
                Set.of("8056ee12-2222-4e6c-a9da-4653f14ecc71", "b897b313-6d68-4a44-bf61-7e228458c5f5",
                        "e6b442ab-84a3-43e9-bfb6-50692c4252ed", "1750b8fa-ef06-47a1-be67-72122e7db33d",
                        "b4606b30-1999-465f-adfc-c7b990028dd6"),
                "b897b313-6d68-4a44-bf61-7e228458c5f5", "b4606b30-1999-465f-adfc-c7b990028dd6", null);
        FolderModel folderOne = folderModel(1L);
        FolderModel folderTwo = folderModel(2L);
        QuotaModel quotaOne = quotaModel(provider.getId(), resource.getId(), folderOne.getId(), 0, 0);
        QuotaModel quotaTwo = quotaModel(provider.getId(), resource.getId(), folderTwo.getId(), 1025, 1025);
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
        String json = "{\n" +
                "    \"requestType\": \"QUOTA_TRANSFER\",\n" +
                "    \"parameters\":\n" +
                "    {\n" +
                "        \"quotaTransfers\":\n" +
                "        [\n" +
                "            {\n" +
                "                \"destinationServiceId\": 2,\n" +
                "                \"destinationFolderId\": \"" + folderTwo.getId() + "\",\n" +
                "                \"resourceTransfers\":\n" +
                "                [\n" +
                "                    {\n" +
                "                        \"resourceId\": \"" + resource.getId() + "\",\n" +
                "                        \"delta\": \"-0.5\",\n" +
                "                        \"deltaUnitId\": \"b897b313-6d68-4a44-bf61-7e228458c5f5\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"destinationServiceId\": 1,\n" +
                "                \"destinationFolderId\": \"" + folderOne.getId() + "\",\n" +
                "                \"resourceTransfers\":\n" +
                "                [\n" +
                "                    {\n" +
                "                        \"resourceId\": \"" + resource.getId() + "\",\n" +
                "                        \"delta\": \"0.5\",\n" +
                "                        \"deltaUnitId\": \"b897b313-6d68-4a44-bf61-7e228458c5f5\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            }\n" +
                "        ]\n" +
                "    },\n" +
                "    \"addConfirmation\": false,\n" +
                "    \"summary\": \"test\",\n" +
                "    \"description\": \"\"\n" +
                "}";
        String jsonUpdate = "{\n" +
                "    \"requestType\": \"QUOTA_TRANSFER\",\n" +
                "    \"parameters\":\n" +
                "    {\n" +
                "        \"quotaTransfers\":\n" +
                "        [\n" +
                "            {\n" +
                "                \"destinationServiceId\": 2,\n" +
                "                \"destinationFolderId\": \"" + folderTwo.getId() + "\",\n" +
                "                \"resourceTransfers\":\n" +
                "                [\n" +
                "                    {\n" +
                "                        \"resourceId\": \"" + resource.getId() + "\",\n" +
                "                        \"delta\": \"-0.75\",\n" +
                "                        \"deltaUnitId\": \"b897b313-6d68-4a44-bf61-7e228458c5f5\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"destinationServiceId\": 1,\n" +
                "                \"destinationFolderId\": \"" + folderOne.getId() + "\",\n" +
                "                \"resourceTransfers\":\n" +
                "                [\n" +
                "                    {\n" +
                "                        \"resourceId\": \"" + resource.getId() + "\",\n" +
                "                        \"delta\": \"0.75\",\n" +
                "                        \"deltaUnitId\": \"b897b313-6d68-4a44-bf61-7e228458c5f5\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            }\n" +
                "        ]\n" +
                "    },\n" +
                "    \"addConfirmation\": false,\n" +
                "    \"summary\": \"test\",\n" +
                "    \"description\": \"\"\n" +
                "}";
        testUpdate(json, jsonUpdate, folderOne, folderTwo, resource, lang);
        jsonUpdate = "{\n" +
                "    \"requestType\": \"QUOTA_TRANSFER\",\n" +
                "    \"parameters\":\n" +
                "    {\n" +
                "        \"quotaTransfers\":\n" +
                "        [\n" +
                "            {\n" +
                "                \"destinationServiceId\": 2,\n" +
                "                \"destinationFolderId\": \"" + folderTwo.getId() + "\",\n" +
                "                \"resourceTransfers\":\n" +
                "                [\n" +
                "                    {\n" +
                "                        \"resourceId\": \"" + resource.getId() + "\",\n" +
                "                        \"delta\": \"-0,75\",\n" +
                "                        \"deltaUnitId\": \"b897b313-6d68-4a44-bf61-7e228458c5f5\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"destinationServiceId\": 1,\n" +
                "                \"destinationFolderId\": \"" + folderOne.getId() + "\",\n" +
                "                \"resourceTransfers\":\n" +
                "                [\n" +
                "                    {\n" +
                "                        \"resourceId\": \"" + resource.getId() + "\",\n" +
                "                        \"delta\": \"0,75\",\n" +
                "                        \"deltaUnitId\": \"b897b313-6d68-4a44-bf61-7e228458c5f5\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            }\n" +
                "        ]\n" +
                "    },\n" +
                "    \"addConfirmation\": false,\n" +
                "    \"summary\": \"test\",\n" +
                "    \"description\": \"\"\n" +
                "}";
        testUpdate(json, jsonUpdate, folderOne, folderTwo, resource, lang);
    }

    private void testUpdate(String json, String jsonUpdate, FolderModel folderOne, FolderModel folderTwo,
                            ResourceModel resource, String lang) {
        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers?lang={lang}", lang)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
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
                .uri("/front/transfers/{id}?version={version}&lang={lang}", result.getTransfer().getId(),
                        result.getTransfer().getVersion(), lang)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(jsonUpdate)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(putResult);
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, putResult.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER,
                putResult.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, putResult.getTransfer().getStatus());
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
                        && r.getDelta().equals("768") && r.getDeltaUnit().equals("MiB")
                        && r.getDeltaUnitId().equals("b4606b30-1999-465f-adfc-c7b990028dd6"))));
        Assertions.assertTrue(putResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().equals("-768") && r.getDeltaUnit().equals("MiB")
                        && r.getDeltaUnitId().equals("b4606b30-1999-465f-adfc-c7b990028dd6"))));
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createTransferRequestFractionalAmountsTest() {
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
        Assertions.assertNotNull(result);
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, result.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER,
                result.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, result.getTransfer().getStatus());
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
    public void createTransferRequestFailOnQuotaTransfersAndReserveTransferInOneRequestTest() {
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody(TransferRequestTypeDto.QUOTA_TRANSFER))
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(expectedResult("parameters", "Only one type of parameters allowed in one request."));

        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody(TransferRequestTypeDto.RESERVE_TRANSFER))
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(expectedResult("parameters", "Only one type of parameters allowed in one request."));
    }

    @Test
    public void createTransferRequestFailOnAbsentReserveTransferTest() {
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(TEST_FOLDER_1_ID)
                                        .destinationServiceId("1")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("100")
                                                .resourceId(YP_HDD_MAN)
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(TEST_FOLDER_3_ID)
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("-100")
                                                .resourceId(YP_HDD_MAN)
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(expectedResult("parameters.reserveTransfer", "Field is required."));
    }

    @Test
    public void createTransferRequestFailOnAbsentProviderIdTest() {
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .destinationFolderId(TEST_FOLDER_1_ID)
                                        .destinationServiceId("1")
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(expectedResult("parameters.reserveTransfer.providerId", "Field is required."));
    }

    @Test
    public void createTransferRequestFailOnAbsentResourceFieldsTest() {
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .build())
                                        .destinationFolderId(TEST_FOLDER_1_ID)
                                        .destinationServiceId("1")
                                        .providerId(YP_ID)
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    containsError(errorCollection, "parameters.reserveTransfer.resourceTransfers.0.delta",
                            "Field is required.");
                    containsError(errorCollection, "parameters.reserveTransfer.resourceTransfers.0.deltaUnitId",
                            "Field is required.");
                    containsError(errorCollection, "parameters.reserveTransfer.resourceTransfers.0.resourceId",
                            "Field is required.");
                });
    }

    @Test
    public void createTransferRequestFailOnDuplicateResourceIdTest() {
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("200")
                                                .resourceId(YP_HDD_MAN)
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .destinationFolderId(TEST_FOLDER_1_ID)
                                        .destinationServiceId("1")
                                        .providerId(YP_ID)
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    containsError(errorCollection, "parameters.reserveTransfer.resourceTransfers",
                            "Duplicate resource ids are not allowed.");
                });
    }

    @Test
    public void createTransferRequestFailOnNotPositiveDeltaTest() {
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("-100")
                                                .resourceId(YP_HDD_MAN)
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("200")
                                                .resourceId(YP_HDD_SAS)
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .destinationFolderId(TEST_FOLDER_1_ID)
                                        .destinationServiceId("1")
                                        .providerId(YP_ID)
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    containsError(errorCollection, "parameters.reserveTransfer.resourceTransfers.0.delta",
                            "Number must be positive.");
                });
    }

    @Test
    public void createTransferRequestFailOnBadProviderTest() {
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .destinationFolderId(TEST_FOLDER_1_ID)
                                        .destinationServiceId("1")
                                        .providerId(UUID.randomUUID().toString())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    containsError(errorCollection, "parameters.reserveTransfer.providerId",
                            "Provider not found.");
                    containsError(errorCollection, "parameters.reserveTransfer.resourceTransfers.0.resourceId",
                            "Resource belong to other provider.");
                });
        ProviderModel provider = providerBuilder()
                .deleted(true)
                .build();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .destinationFolderId(TEST_FOLDER_1_ID)
                                        .destinationServiceId("1")
                                        .providerId(provider.getId())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    containsError(errorCollection, "parameters.reserveTransfer.providerId",
                            "Provider not found.");
                    containsError(errorCollection, "parameters.reserveTransfer.resourceTransfers.0.resourceId",
                            "Resource belong to other provider.");
                });
        ProviderModel pr2 = providerBuilder()
                .managed(false)
                .build();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, pr2))).block();
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .destinationFolderId(TEST_FOLDER_1_ID)
                                        .destinationServiceId("1")
                                        .providerId(pr2.getId())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    containsError(errorCollection, "parameters.reserveTransfer.providerId",
                            "Provider is non managed.");
                    containsError(errorCollection, "parameters.reserveTransfer.resourceTransfers.0.resourceId",
                            "Resource belong to other provider.");
                });
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createTransferRequestFailOnBadServiceTest() {
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .destinationFolderId(TEST_FOLDER_1_ID)
                                        .destinationServiceId(UUID.randomUUID().toString())
                                        .providerId(YP_ID)
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    containsError(errorCollection, "parameters.reserveTransfer.destinationServiceId",
                            "Service not found.");
                });
        ServiceRecipeModel service1 = serviceBuilder().id(7766L).exportable(false).build();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> servicesDao
                                .upsertRecipeRetryable(txSession, service1))).block();
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .destinationFolderId(TEST_FOLDER_1_ID)
                                        .destinationServiceId(Long.toString(service1.getId()))
                                        .providerId(YP_ID)
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    containsError(errorCollection, "parameters.reserveTransfer.destinationServiceId",
                            "Services in the sandbox are not allowed.");
                });


        ServiceRecipeModel service2 = serviceBuilder()
                .id(7765L)
                .state(Sets.difference(EnumSet.allOf(ServiceState.class),
                        Sets.union(AbcServiceValidator.ALLOWED_SERVICE_STATES, Set.of(ServiceState.UNKNOWN)))
                        .iterator().next())
                .build();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> servicesDao
                                .upsertRecipeRetryable(txSession, service2))).block();
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .destinationFolderId(TEST_FOLDER_1_ID)
                                        .destinationServiceId(Long.toString(service2.getId()))
                                        .providerId(YP_ID)
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    containsError(errorCollection, "parameters.reserveTransfer.destinationServiceId",
                            "Current service status is not allowed.");
                });

        ServiceRecipeModel service3 = serviceBuilder()
                .id(7764L)
                .readOnlyState(Sets.difference(EnumSet.allOf(ServiceReadOnlyState.class),
                        Sets.union(AbcServiceValidator.ALLOWED_SERVICE_READONLY_STATES,
                                Set.of(ServiceReadOnlyState.UNKNOWN)))
                        .iterator().next())
                .build();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> servicesDao
                                .upsertRecipeRetryable(txSession, service3))).block();
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .destinationFolderId(TEST_FOLDER_1_ID)
                                        .destinationServiceId(Long.toString(service3.getId()))
                                        .providerId(YP_ID)
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    containsError(errorCollection, "parameters.reserveTransfer.destinationServiceId",
                            "Current service status is not allowed.");
                });
    }

    @Test
    public void createTransferRequestFailOnProviderWithoutReserveFolderTest() {
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
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
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .destinationFolderId(TEST_FOLDER_1_ID)
                                        .destinationServiceId(String.valueOf(TEST_SERVICE_ID_D))
                                        .providerId(CLAUD2_ID)
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    containsError(errorCollection, "parameters.reserveTransfer.providerId",
                            "Provider reserve folder not found.");
                });
    }

    private ProviderModel.Builder providerBuilder() {
        return providerModelBuilder("in-process:test", null, false, 69L);
    }

    static void containsError(ErrorCollectionDto errorCollection, String s, String s2) {
        Set<String> errors = errorCollection.getFieldErrors()
                .get(s);
        Assertions.assertNotNull(errors);
        Assertions.assertTrue(errors.contains(s2));
    }

    private Consumer<EntityExchangeResult<ErrorCollectionDto>> expectedResult(String parameters, String s) {
        return result -> {
            Assertions.assertNotNull(result);
            ErrorCollectionDto errorCollection = result.getResponseBody();
            Assertions.assertNotNull(errorCollection);
            Assertions.assertNotNull(errorCollection.getFieldErrors());
            containsError(errorCollection, parameters, s);
        };
    }

    private FrontCreateTransferRequestDto getBody(TransferRequestTypeDto quotaTransfer) {
        return FrontCreateTransferRequestDto.builder()
                .requestType(quotaTransfer)
                .addConfirmation(false)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TEST_FOLDER_1_ID)
                                .destinationServiceId("1")
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("100")
                                        .resourceId(YP_HDD_MAN)
                                        .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TEST_FOLDER_3_ID)
                                .destinationServiceId("2")
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-100")
                                        .resourceId(YP_HDD_MAN)
                                        .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                        .build())
                                .build())
                        .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("100")
                                        .resourceId(YP_HDD_MAN)
                                        .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                        .build())
                                .destinationFolderId(TEST_FOLDER_1_ID)
                                .destinationServiceId("1")
                                .providerId(YP_ID)
                                .build())
                        .build())
                .build();
    }

    static ServiceRecipeModel.Builder serviceBuilder() {
        return ServiceRecipeModel.builder()
                .id(RANDOM.nextLong())
                .name("Test name")
                .nameEn("Test name")
                .slug("test_service")
                .state(ServiceState.DEVELOP)
                .readOnlyState(null)
                .exportable(true)
                .parentId(RANDOM.nextLong());
    }
}
