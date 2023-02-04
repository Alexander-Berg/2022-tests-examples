package ru.yandex.intranet.d.web.api.transfers;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

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
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.model.resources.ResourceModel;
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel;
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel;
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel;
import ru.yandex.intranet.d.model.services.ServiceRecipeModel;
import ru.yandex.intranet.d.model.services.ServiceState;
import ru.yandex.intranet.d.model.transfers.PendingTransferRequestsModel;
import ru.yandex.intranet.d.services.notifications.NotificationMailSenderStub;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.SortOrderDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestStatusDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestSubtypeDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestVoteTypeDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateQuotaResourceTransferDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateQuotaTransferDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateReserveResourceTransferDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateReserveTransferDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateTransferRequestParametersDto;
import ru.yandex.intranet.d.web.model.transfers.api.TransferRequestDto;

import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_3_ID;
import static ru.yandex.intranet.d.TestProviders.CLAUD2_ID;
import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestResources.YP_HDD_MAN;
import static ru.yandex.intranet.d.TestResources.YP_HDD_SAS;
import static ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.folderModel;
import static ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.providerModel;
import static ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.providerModelBuilder;
import static ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.quotaModel;
import static ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.resourceModel;
import static ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.resourceSegmentModel;
import static ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.resourceSegmentationModel;
import static ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.resourceTypeModel;

/**
 * Transfer requests public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class MorePublicApiTransferRequestsControllerTest {
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
        TransferRequestDto result = webClient
                .mutateWith(MockUser.uid("1120000000000003"))
                .post()
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(CreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                        .addConfirmation(false)
                        .parameters(CreateTransferRequestParametersDto.builder()
                                .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                        .folderId(folderOne.getId())
                                        .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                                .delta(100L)
                                                .resourceId(resource.getId())
                                                .deltaUnitKey("bytes")
                                                .build())
                                        .build())
                                .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                        .folderId(folderTwo.getId())
                                        .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                                .delta(-100L)
                                                .resourceId(resource.getId())
                                                .deltaUnitKey("bytes")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, result.getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER,
                result.getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, result.getStatus());
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> pendingTransferRequestsDao.getById(txSession,
                                result.getId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Assertions.assertNotNull(pendingTransferRequest);
        Assertions.assertEquals(
                Optional.empty(),
                pendingTransferRequest.map(PendingTransferRequestsModel::getRequestId));
        Assertions.assertEquals("1120000000000003", result.getCreatedBy());
        Assertions.assertEquals(1, result.getVotes().getVoters().size());
        Assertions.assertEquals("1120000000000003",
                result.getVotes().getVoters().get(0).getVoter());
        Assertions.assertEquals(TransferRequestVoteTypeDto.CONFIRM,
                result.getVotes().getVoters().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId(), folderTwo.getId()),
                new HashSet<>(result.getVotes().getVoters().get(0).getFolderIds()));
        Assertions.assertEquals(Set.of(),
                new HashSet<>(result.getVotes().getVoters().get(0).getServiceIds()));
        Assertions.assertEquals(Set.of(provider.getId()),
                new HashSet<>(result.getVotes().getVoters().get(0).getProviderIds()));
        Assertions.assertEquals(0, result.getTransferResponsible().getGrouped().size());
        Assertions.assertEquals(1, result.getTransferResponsible().getProviderSuperResponsible().size());
        Assertions.assertTrue(result.getTransferResponsible().getProviderSuperResponsible().stream()
                .anyMatch(g -> g.getResponsible().equals("1120000000000003")));
        Assertions.assertTrue(result.getTransferResponsible().getProviderSuperResponsible().stream()
                .anyMatch(g -> g.getProviderIds().contains(provider.getId())));
        Assertions.assertEquals(2, result.getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(result.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())));
        Assertions.assertTrue(result.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getFolderId().equals(folderTwo.getId())));
        Assertions.assertTrue(result.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta() == 100L && r.getDeltaUnitKey().equals("bytes"))));
        Assertions.assertTrue(result.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta() == -100L && r.getDeltaUnitKey().equals("bytes"))));
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
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(CreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                        .addConfirmation(false)
                        .parameters(CreateTransferRequestParametersDto.builder()
                                .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                        .folderId(folderOne.getId())
                                        .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                                .delta(160L)
                                                .resourceId(resource.getId())
                                                .deltaUnitKey("bytes")
                                                .build())
                                        .build())
                                .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                        .folderId(folderTwo.getId())
                                        .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                                .delta(-160L)
                                                .resourceId(resource.getId())
                                                .deltaUnitKey("bytes")
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
        CreateTransferRequestDto body = CreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .addConfirmation(true)
                .parameters(CreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                .folderId(TestFolders.TEST_FOLDER_IN_CLOSING_SERVICE)
                                .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                        .delta(1L)
                                        .resourceId(YP_HDD_MAN)
                                        .deltaUnitKey("gigabytes")
                                        .build())
                                .build())
                        .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                .folderId(TEST_FOLDER_1_ID)
                                .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                        .delta(-1L)
                                        .resourceId(YP_HDD_MAN)
                                        .deltaUnitKey("gigabytes")
                                        .build())
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(expectedResult("parameters.quotaTransfers.0",
                        "Current service status is not allowed."));
    }

    @Test
    public void createTransferRequestFromClosingServiceTest() {
        CreateTransferRequestDto body = CreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .addConfirmation(true)
                .parameters(CreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                .folderId(TestFolders.TEST_FOLDER_IN_CLOSING_SERVICE)
                                .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                        .delta(-1L)
                                        .resourceId(YP_HDD_SAS)
                                        .deltaUnitKey("gigabytes")
                                        .build())
                                .build())
                        .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                .folderId(TestFolders.TEST_FOLDER_2_ID)
                                .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                        .delta(1L)
                                        .resourceId(YP_HDD_SAS)
                                        .deltaUnitKey("gigabytes")
                                        .build())
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void createTransferRequestToNonExportableServiceTest() {
        CreateTransferRequestDto body = CreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .addConfirmation(true)
                .parameters(CreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                .folderId(TestFolders.TEST_FOLDER_IN_NON_EXPORTABLE_SERVICE)
                                .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                        .delta(1L)
                                        .resourceId(YP_HDD_MAN)
                                        .deltaUnitKey("gigabytes")
                                        .build())
                                .build())
                        .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                .folderId(TEST_FOLDER_1_ID)
                                .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                        .delta(-1L)
                                        .resourceId(YP_HDD_MAN)
                                        .deltaUnitKey("gigabytes")
                                        .build())
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(expectedResult("parameters.quotaTransfers.0",
                        "Services in the sandbox are not allowed."));
    }

    @Test
    public void createTransferRequestFromNonExportableServiceTest() {
        CreateTransferRequestDto body = CreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .addConfirmation(true)
                .parameters(CreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                .folderId(TestFolders.TEST_FOLDER_IN_NON_EXPORTABLE_SERVICE)
                                .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                        .delta(-1L)
                                        .resourceId(YP_HDD_SAS)
                                        .deltaUnitKey("gigabytes")
                                        .build())
                                .build())
                        .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                .folderId(TestFolders.TEST_FOLDER_2_ID)
                                .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                        .delta(1L)
                                        .resourceId(YP_HDD_SAS)
                                        .deltaUnitKey("gigabytes")
                                        .build())
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void createTransferRequestToRenamingServiceTest() {
        CreateTransferRequestDto body = CreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .addConfirmation(true)
                .parameters(CreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                .folderId(TestFolders.TEST_FOLDER_IN_RENAMING_SERVICE)
                                .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                        .delta(1L)
                                        .resourceId(YP_HDD_MAN)
                                        .deltaUnitKey("gigabytes")
                                        .build())
                                .build())
                        .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                .folderId(TEST_FOLDER_1_ID)
                                .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                        .delta(-1L)
                                        .resourceId(YP_HDD_MAN)
                                        .deltaUnitKey("gigabytes")
                                        .build())
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void createTransferRequestFromRenamingServiceTest() {
        CreateTransferRequestDto body = CreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .addConfirmation(true)
                .parameters(CreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                .folderId(TestFolders.TEST_FOLDER_IN_RENAMING_SERVICE)
                                .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                        .delta(-1L)
                                        .resourceId(YP_HDD_SAS)
                                        .deltaUnitKey("gigabytes")
                                        .build())
                                .build())
                        .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                .folderId(TestFolders.TEST_FOLDER_2_ID)
                                .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                        .delta(1L)
                                        .resourceId(YP_HDD_SAS)
                                        .deltaUnitKey("gigabytes")
                                        .build())
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void createTransferRequestFailOnQuotaTransfersAndReserveTransferInOneRequestTest() {
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/api/v1/transfers")
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
                .uri("/api/v1/transfers")
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
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(CreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(CreateTransferRequestParametersDto.builder()
                                .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                        .folderId(TEST_FOLDER_1_ID)
                                        .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                                .delta(100L)
                                                .resourceId(YP_HDD_MAN)
                                                .deltaUnitKey("bytes")
                                                .build())
                                        .build())
                                .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                        .folderId(TEST_FOLDER_3_ID)
                                        .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                                .delta(-100L)
                                                .resourceId(YP_HDD_MAN)
                                                .deltaUnitKey("bytes")
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
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(CreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(CreateTransferRequestParametersDto.builder()
                                .reserveTransfer(CreateReserveTransferDto.builder()
                                        .addResourceTransfer(CreateReserveResourceTransferDto.builder()
                                                .delta(100L)
                                                .resourceId(YP_HDD_MAN)
                                                .deltaUnitKey("bytes")
                                                .build())
                                        .folderId(TEST_FOLDER_1_ID)
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
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(CreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(CreateTransferRequestParametersDto.builder()
                                .reserveTransfer(CreateReserveTransferDto.builder()
                                        .addResourceTransfer(CreateReserveResourceTransferDto.builder()
                                                .build())
                                        .folderId(TEST_FOLDER_1_ID)
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
                    containsError(errorCollection, "parameters.reserveTransfer.resourceTransfers.0.deltaUnitKey",
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
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(CreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(CreateTransferRequestParametersDto.builder()
                                .reserveTransfer(CreateReserveTransferDto.builder()
                                        .addResourceTransfer(CreateReserveResourceTransferDto.builder()
                                                .delta(100L)
                                                .resourceId(YP_HDD_MAN)
                                                .deltaUnitKey("bytes")
                                                .build())
                                        .addResourceTransfer(CreateReserveResourceTransferDto.builder()
                                                .delta(200L)
                                                .resourceId(YP_HDD_MAN)
                                                .deltaUnitKey("bytes")
                                                .build())
                                        .folderId(TEST_FOLDER_1_ID)
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
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(CreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(CreateTransferRequestParametersDto.builder()
                                .reserveTransfer(CreateReserveTransferDto.builder()
                                        .addResourceTransfer(CreateReserveResourceTransferDto.builder()
                                                .delta(-100L)
                                                .resourceId(YP_HDD_MAN)
                                                .deltaUnitKey("bytes")
                                                .build())
                                        .addResourceTransfer(CreateReserveResourceTransferDto.builder()
                                                .delta(200L)
                                                .resourceId(YP_HDD_SAS)
                                                .deltaUnitKey("bytes")
                                                .build())
                                        .folderId(TEST_FOLDER_1_ID)
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
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(CreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(CreateTransferRequestParametersDto.builder()
                                .reserveTransfer(CreateReserveTransferDto.builder()
                                        .addResourceTransfer(CreateReserveResourceTransferDto.builder()
                                                .delta(100L)
                                                .resourceId(YP_HDD_MAN)
                                                .deltaUnitKey("bytes")
                                                .build())
                                        .folderId(TEST_FOLDER_1_ID)
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
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(CreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(CreateTransferRequestParametersDto.builder()
                                .reserveTransfer(CreateReserveTransferDto.builder()
                                        .addResourceTransfer(CreateReserveResourceTransferDto.builder()
                                                .delta(100L)
                                                .resourceId(YP_HDD_MAN)
                                                .deltaUnitKey("bytes")
                                                .build())
                                        .folderId(TEST_FOLDER_1_ID)
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
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(CreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(CreateTransferRequestParametersDto.builder()
                                .reserveTransfer(CreateReserveTransferDto.builder()
                                        .addResourceTransfer(CreateReserveResourceTransferDto.builder()
                                                .delta(100L)
                                                .resourceId(YP_HDD_MAN)
                                                .deltaUnitKey("bytes")
                                                .build())
                                        .folderId(TEST_FOLDER_1_ID)
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
    public void createTransferRequestFailOnProviderWithoutReserveFolderTest() {
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(CreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(CreateTransferRequestParametersDto.builder()
                                .reserveTransfer(CreateReserveTransferDto.builder()
                                        .addResourceTransfer(CreateReserveResourceTransferDto.builder()
                                                .delta(100L)
                                                .resourceId(YP_HDD_MAN)
                                                .deltaUnitKey("bytes")
                                                .build())
                                        .folderId(TEST_FOLDER_1_ID)
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

    private CreateTransferRequestDto getBody(TransferRequestTypeDto quotaTransfer) {
        return CreateTransferRequestDto.builder()
                .requestType(quotaTransfer)
                .addConfirmation(false)
                .parameters(CreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                .folderId(TEST_FOLDER_1_ID)
                                .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                        .delta(100L)
                                        .resourceId(YP_HDD_MAN)
                                        .deltaUnitKey("bytes")
                                        .build())
                                .build())
                        .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                .folderId(TEST_FOLDER_3_ID)
                                .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                        .delta(-100L)
                                        .resourceId(YP_HDD_MAN)
                                        .deltaUnitKey("bytes")
                                        .build())
                                .build())
                        .reserveTransfer(CreateReserveTransferDto.builder()
                                .addResourceTransfer(CreateReserveResourceTransferDto.builder()
                                        .delta(100L)
                                        .resourceId(YP_HDD_MAN)
                                        .deltaUnitKey("bytes")
                                        .build())
                                .folderId(TEST_FOLDER_1_ID)
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
