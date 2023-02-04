package ru.yandex.intranet.d.web.api.transfers;

import java.util.Collections;
import java.util.HashSet;
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
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
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
import ru.yandex.intranet.d.web.model.SortOrderDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestStatusDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestSubtypeDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestVoteTypeDto;
import ru.yandex.intranet.d.web.model.transfers.api.CancelTransferRequestInputDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateQuotaResourceTransferDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateQuotaTransferDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateReserveResourceTransferDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateReserveTransferDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateTransferRequestParametersDto;
import ru.yandex.intranet.d.web.model.transfers.api.TransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.api.TransferRequestVotingDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestHistoryEventsPageDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestSearchDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestsPageDto;

import static ru.yandex.intranet.d.TestUsers.USER_2_UID;
import static ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.folderModel;
import static ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.providerModel;
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
public class ThirdMorePublicApiTransferRequestsControllerTest {
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

    public ThirdMorePublicApiTransferRequestsControllerTest(
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
        return SecondMorePublicApiTransferRequestsControllerTest.prepareWithRoles(tableClient, servicesDao,
                abcServiceMemberDao, responsibleOfProvide, quotaManagerRoleId, usersDao, providersDao, resourceTypesDao,
                resourceSegmentationsDao, resourceSegmentsDao, resourcesDao, folderDao, quotasDao);
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
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(CreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                        .addConfirmation(false)
                        .parameters(CreateTransferRequestParametersDto.builder()
                                .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                        .folderId(folderOne.getId())
                                        .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                                .delta(0L)
                                                .resourceId(resource.getId())
                                                .deltaUnitKey("bytes")
                                                .build())
                                        .build())
                                .addQuotaTransfer(CreateQuotaTransferDto.builder()
                                        .folderId(folderTwo.getId())
                                        .addResourceTransfer(CreateQuotaResourceTransferDto.builder()
                                                .delta(0L)
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
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
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
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(CreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(CreateTransferRequestParametersDto.builder()
                                .reserveTransfer(CreateReserveTransferDto.builder()
                                        .addResourceTransfer(CreateReserveResourceTransferDto.builder()
                                                .delta(0L)
                                                .resourceId(resourceOne.getId())
                                                .deltaUnitKey("bytes")
                                                .build())
                                        .folderId(folderOne.getId())
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
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createTransferRequestIdempotencyTest() {
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
        String idempotencyKey = UUID.randomUUID().toString();
        TransferRequestDto result = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
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
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER, result.getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, result.getStatus());
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                                TransactionMode.SERIALIZABLE_READ_WRITE,
                                txSession -> pendingTransferRequestsDao.getById(txSession,
                                        result.getId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Assertions.assertNotNull(pendingTransferRequest);
        Assertions.assertEquals(
                Optional.of(result.getId()),
                pendingTransferRequest.map(PendingTransferRequestsModel::getRequestId));
        Assertions.assertEquals("1120000000000010", result.getCreatedBy());
        Assertions.assertTrue(result.getVotes().getVoters().isEmpty());
        Assertions.assertEquals(2, result.getTransferResponsible().getGrouped().size());
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId())));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderTwo.getId())));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000010"))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000012"))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderTwo.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000010"))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderTwo.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000012"))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000010")
                                && r.getServiceIds().contains(1L))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000012")
                                && r.getServiceIds().contains(1L))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderTwo.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000010")
                                && r.getServiceIds().contains(2L))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderTwo.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000012")
                                && r.getServiceIds().contains(2L))));
        Assertions.assertEquals(2, result.getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(result.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())));
        Assertions.assertTrue(result.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getFolderId().equals(folderTwo.getId())));
        Assertions.assertTrue(result.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta() == 100L
                        && r.getDeltaUnitKey().equals("bytes"))));
        Assertions.assertTrue(result.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta() == -100L
                        && r.getDeltaUnitKey().equals("bytes"))));
        long updatedMailCounter = mailSender.getCounter();
        Assertions.assertTrue(updatedMailCounter - initialMailCounter > 0);
        TransferRequestDto idempotencyResult = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
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
        Assertions.assertNotNull(idempotencyResult);
        Assertions.assertEquals(result.getId(), idempotencyResult.getId());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void cancelTransferRequestIdempotencyTest() {
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
        TransferRequestDto result = webClient
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
        String idempotencyKey = UUID.randomUUID().toString();
        TransferRequestDto cancelResult = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/api/v1/transfers/{id}/_cancel", result.getId())
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(new CancelTransferRequestInputDto(result.getVersion()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(cancelResult);
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, cancelResult.getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER, result.getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.CANCELLED, cancelResult.getStatus());
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
        Assertions.assertEquals("1120000000000010", cancelResult.getCreatedBy());
        Assertions.assertTrue(cancelResult.getVotes().getVoters().isEmpty());
        Assertions.assertEquals(2, cancelResult.getTransferResponsible().getGrouped().size());
        Assertions.assertTrue(cancelResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId())));
        Assertions.assertTrue(cancelResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderTwo.getId())));
        Assertions.assertTrue(cancelResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000010"))));
        Assertions.assertTrue(cancelResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000012"))));
        Assertions.assertTrue(cancelResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderTwo.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000010"))));
        Assertions.assertTrue(cancelResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderTwo.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000012"))));
        Assertions.assertTrue(cancelResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000010")
                                && r.getServiceIds().contains(1L))));
        Assertions.assertTrue(cancelResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000012")
                                && r.getServiceIds().contains(1L))));
        Assertions.assertTrue(cancelResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderTwo.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000010")
                                && r.getServiceIds().contains(2L))));
        Assertions.assertTrue(cancelResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderTwo.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000012")
                                && r.getServiceIds().contains(2L))));
        Assertions.assertEquals(2, cancelResult.getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(cancelResult.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())));
        Assertions.assertTrue(cancelResult.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getFolderId().equals(folderTwo.getId())));
        Assertions.assertTrue(cancelResult.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta() == 100L
                        && r.getDeltaUnitKey().equals("bytes"))));
        Assertions.assertTrue(cancelResult.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta() == -100L
                        && r.getDeltaUnitKey().equals("bytes"))));
        TransferRequestDto idempotencyCancelResult = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/api/v1/transfers/{id}/_cancel", result.getId())
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(new CancelTransferRequestInputDto(result.getVersion()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(idempotencyCancelResult);
        Assertions.assertEquals(TransferRequestStatusDto.CANCELLED, idempotencyCancelResult.getStatus());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void voteConfirmTransferRequestIdempotencyTest() {
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
        TransferRequestDto result = webClient
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
        String idempotencyKey = UUID.randomUUID().toString();
        TransferRequestDto voteResult = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/api/v1/transfers/{id}/_vote", result.getId())
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(TransferRequestVotingDto.builder()
                        .voteType(TransferRequestVoteTypeDto.CONFIRM)
                        .version(result.getVersion())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(voteResult);
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, voteResult.getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_QUOTA_TRANSFER, voteResult.getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, voteResult.getStatus());
        Assertions.assertEquals("1120000000000010", voteResult.getCreatedBy());
        Assertions.assertEquals(1, voteResult.getVotes().getVoters().size());
        Assertions.assertEquals("1120000000000010",
                voteResult.getVotes().getVoters().get(0).getVoter());
        Assertions.assertEquals(TransferRequestVoteTypeDto.CONFIRM,
                voteResult.getVotes().getVoters().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId(), folderTwo.getId()),
                new HashSet<>(voteResult.getVotes().getVoters().get(0).getFolderIds()));
        Assertions.assertEquals(Set.of(1L, 2L),
                new HashSet<>(voteResult.getVotes().getVoters().get(0).getServiceIds()));
        Assertions.assertEquals(2, voteResult.getTransferResponsible().getGrouped().size());
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId())));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderTwo.getId())));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000010"))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000012"))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderTwo.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000010"))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderTwo.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000012"))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000010")
                                && r.getServiceIds().contains(1L))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000012")
                                && r.getServiceIds().contains(1L))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderTwo.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000010")
                                && r.getServiceIds().contains(2L))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderTwo.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals("1120000000000012")
                                && r.getServiceIds().contains(2L))));
        Assertions.assertEquals(2, voteResult.getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(voteResult.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())));
        Assertions.assertTrue(voteResult.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getFolderId().equals(folderTwo.getId())));
        Assertions.assertTrue(voteResult.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta() == 100L
                        && r.getDeltaUnitKey().equals("bytes"))));
        Assertions.assertTrue(voteResult.getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta() == -100L
                        && r.getDeltaUnitKey().equals("bytes"))));
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
                                folderOperationLogDao.getPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID,
                                        folderOne.getId(), null, SortOrderDto.ASC, 100)))
                .block();
        List<FolderOperationLogModel> logsTwo = tableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                                folderOperationLogDao.getPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID,
                                        folderTwo.getId(), null, SortOrderDto.ASC, 100)))
                .block();
        Assertions.assertEquals(1, logsOne.size());
        Assertions.assertEquals(1, logsTwo.size());
        FrontTransferRequestHistoryEventsPageDto historyResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/transfers/{id}/history", voteResult.getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontTransferRequestHistoryEventsPageDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(historyResult);
        Assertions.assertEquals(2, historyResult.getEvents().size());
        FrontTransferRequestsPageDto searchResultByCurrentUser = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers/_search")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestSearchDto.builder()
                        .filterByCurrentUser(true)
                        .addFilterByStatus(TransferRequestStatusDto.APPLIED)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontTransferRequestsPageDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(searchResultByCurrentUser);
        Assertions.assertEquals(1, searchResultByCurrentUser.getTransfers().size());
        FrontTransferRequestsPageDto searchResultByFolder = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers/_search")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestSearchDto.builder()
                        .filterByFolderId(folderOne.getId())
                        .addFilterByStatus(TransferRequestStatusDto.APPLIED)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontTransferRequestsPageDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(searchResultByFolder);
        Assertions.assertEquals(1, searchResultByFolder.getTransfers().size());
        FrontTransferRequestsPageDto searchResultByService = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers/_search")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestSearchDto.builder()
                        .filterByServiceId("1")
                        .addFilterByStatus(TransferRequestStatusDto.APPLIED)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontTransferRequestsPageDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(searchResultByService);
        Assertions.assertEquals(1, searchResultByService.getTransfers().size());
        TransferRequestDto idempotencyVoteResult = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/api/v1/transfers/{id}/_vote", result.getId())
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(TransferRequestVotingDto.builder()
                        .voteType(TransferRequestVoteTypeDto.CONFIRM)
                        .version(result.getVersion())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(idempotencyVoteResult);
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, idempotencyVoteResult.getStatus());
    }

}
