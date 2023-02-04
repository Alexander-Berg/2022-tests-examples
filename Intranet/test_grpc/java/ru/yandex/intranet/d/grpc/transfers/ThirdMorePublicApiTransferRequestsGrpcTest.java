package ru.yandex.intranet.d.grpc.transfers;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.yandex.ydb.table.transaction.TransactionMode;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuple8;
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.backend.service.proto.CancelTransferRequest;
import ru.yandex.intranet.d.backend.service.proto.CreateTransferRequest;
import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;
import ru.yandex.intranet.d.backend.service.proto.QuotaTransfer;
import ru.yandex.intranet.d.backend.service.proto.QuotaTransferParameters;
import ru.yandex.intranet.d.backend.service.proto.ReserveResourceTransfer;
import ru.yandex.intranet.d.backend.service.proto.ReserveTransferParameters;
import ru.yandex.intranet.d.backend.service.proto.ResourceTransfer;
import ru.yandex.intranet.d.backend.service.proto.Transfer;
import ru.yandex.intranet.d.backend.service.proto.TransferAmount;
import ru.yandex.intranet.d.backend.service.proto.TransferParameters;
import ru.yandex.intranet.d.backend.service.proto.TransferStatus;
import ru.yandex.intranet.d.backend.service.proto.TransferSubtype;
import ru.yandex.intranet.d.backend.service.proto.TransferType;
import ru.yandex.intranet.d.backend.service.proto.TransfersServiceGrpc;
import ru.yandex.intranet.d.backend.service.proto.VoteForTransferRequest;
import ru.yandex.intranet.d.backend.service.proto.VoteType;
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
import ru.yandex.intranet.d.grpc.MockGrpcUser;
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
import ru.yandex.intranet.d.utils.ErrorsHelper;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.SortOrderDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestStatusDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestHistoryEventsPageDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestSearchDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestsPageDto;

import static ru.yandex.intranet.d.TestUsers.USER_2_UID;
import static ru.yandex.intranet.d.grpc.transfers.PublicApiTransferRequestsGrpcTest.folderModel;
import static ru.yandex.intranet.d.grpc.transfers.PublicApiTransferRequestsGrpcTest.providerModel;
import static ru.yandex.intranet.d.grpc.transfers.PublicApiTransferRequestsGrpcTest.quotaModel;
import static ru.yandex.intranet.d.grpc.transfers.PublicApiTransferRequestsGrpcTest.resourceModel;
import static ru.yandex.intranet.d.grpc.transfers.PublicApiTransferRequestsGrpcTest.resourceSegmentModel;
import static ru.yandex.intranet.d.grpc.transfers.PublicApiTransferRequestsGrpcTest.resourceSegmentationModel;
import static ru.yandex.intranet.d.grpc.transfers.PublicApiTransferRequestsGrpcTest.resourceTypeModel;

/**
 * Transfer requests GRPC public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ThirdMorePublicApiTransferRequestsGrpcTest {

    @GrpcClient("inProcess")
    private TransfersServiceGrpc.TransfersServiceBlockingStub transfersService;
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

    public ThirdMorePublicApiTransferRequestsGrpcTest(
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
        return SecondMorePublicApiTransferRequestsGrpcTest.prepareWithRoles(tableClient, servicesDao,
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
        try {
            transfersService
                    .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                    .createTransfer(CreateTransferRequest.newBuilder()
                            .setType(TransferType.QUOTA_TRANSFER)
                            .setAddConfirmation(false)
                            .setParameters(TransferParameters.newBuilder()
                                    .setQuotaTransfer(QuotaTransferParameters.newBuilder()
                                            .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                    .setFolderId(folderOne.getId())
                                                    .addResourceTransfers(ResourceTransfer.newBuilder()
                                                            .setDelta(TransferAmount.newBuilder()
                                                                    .setValue(0L)
                                                                    .setUnitKey("bytes")
                                                                    .build())
                                                            .setResourceId(resource.getId())
                                                            .build())
                                                    .build())
                                            .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                    .setFolderId(folderTwo.getId())
                                                    .addResourceTransfers(ResourceTransfer.newBuilder()
                                                            .setDelta(TransferAmount.newBuilder()
                                                                    .setValue(0L)
                                                                    .setUnitKey("bytes")
                                                                    .build())
                                                            .setResourceId(resource.getId())
                                                            .build())
                                                    .build()))
                                    .build())
                            .build());
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getFieldErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createReserveTransferRequestZeroDeltaTest() {
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepareWithRoles();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();
        try {
            transfersService
                    .withCallCredentials(MockGrpcUser.uid(USER_2_UID))
                    .createTransfer(CreateTransferRequest.newBuilder()
                            .setType(TransferType.RESERVE_TRANSFER)
                            .setAddConfirmation(false)
                            .setParameters(TransferParameters.newBuilder()
                                    .setReserveTransfer(ReserveTransferParameters.newBuilder()
                                            .setProviderId(providerModel.getId())
                                            .setFolderId(folderOne.getId())
                                            .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                    .setDelta(TransferAmount.newBuilder()
                                                            .setValue(0L)
                                                            .setUnitKey("bytes")
                                                            .build())
                                                    .setResourceId(resourceOne.getId())
                                                    .build()))
                                    .build())
                            .build());
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getFieldErrorsCount() > 0);
            return;
        }
        Assertions.fail();
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
        Metadata extraHeaders = new Metadata();
        Metadata.Key<String> headerKey = Metadata.Key.of("Idempotency-Key", Metadata.ASCII_STRING_MARSHALLER);
        extraHeaders.put(headerKey, idempotencyKey);
        Transfer result = transfersService
                .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders))
                .createTransfer(CreateTransferRequest.newBuilder()
                        .setType(TransferType.QUOTA_TRANSFER)
                        .setAddConfirmation(false)
                        .setParameters(TransferParameters.newBuilder()
                                .setQuotaTransfer(QuotaTransferParameters.newBuilder()
                                        .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                .setFolderId(folderOne.getId())
                                                .addResourceTransfers(ResourceTransfer.newBuilder()
                                                        .setDelta(TransferAmount.newBuilder()
                                                                .setValue(100L)
                                                                .setUnitKey("bytes")
                                                                .build())
                                                        .setResourceId(resource.getId())
                                                        .build())
                                                .build())
                                        .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                .setFolderId(folderTwo.getId())
                                                .addResourceTransfers(ResourceTransfer.newBuilder()
                                                        .setDelta(TransferAmount.newBuilder()
                                                                .setValue(-100L)
                                                                .setUnitKey("bytes")
                                                                .build())
                                                        .setResourceId(resource.getId())
                                                        .build())
                                                .build()))
                                .build())
                        .build());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(TransferType.QUOTA_TRANSFER, result.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_QUOTA_TRANSFER, result.getTransferSubtype());
        Assertions.assertEquals(TransferStatus.PENDING, result.getStatus());
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                                TransactionMode.SERIALIZABLE_READ_WRITE,
                                txSession -> pendingTransferRequestsDao.getById(txSession,
                                        result.getTransferId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Assertions.assertNotNull(pendingTransferRequest);
        Assertions.assertEquals(
                Optional.of(result.getTransferId()),
                pendingTransferRequest.map(PendingTransferRequestsModel::getRequestId));
        Assertions.assertEquals("1120000000000010", result.getCreatedBy().getUid());
        Assertions.assertTrue(result.getVotes().getVotersList().isEmpty());
        Assertions.assertEquals(2, result.getResponsible().getGroupedList().size());
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId())));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId())));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010"))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012"))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010"))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012"))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010")
                                && r.getServiceIdsList().contains(1L))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012")
                                && r.getServiceIdsList().contains(1L))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010")
                                && r.getServiceIdsList().contains(2L))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012")
                                && r.getServiceIdsList().contains(2L))));
        Assertions.assertEquals(2, result.getParameters().getQuotaTransfer().getQuotaTransfersList().size());
        Assertions.assertTrue(result.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())));
        Assertions.assertTrue(result.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderTwo.getId())));
        Assertions.assertTrue(result.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())
                        && t.getResourceTransfersList().stream()
                        .anyMatch(r -> r.getResourceId().equals(resource.getId())
                                && r.getDelta().getValue() == 100L
                                && r.getDelta().getUnitKey().equals("bytes"))));
        Assertions.assertTrue(result.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfersList().stream()
                        .anyMatch(r -> r.getResourceId().equals(resource.getId())
                                && r.getDelta().getValue() == -100L
                                && r.getDelta().getUnitKey().equals("bytes"))));
        long updatedMailCounter = mailSender.getCounter();
        Assertions.assertTrue(updatedMailCounter - initialMailCounter > 0);
        Transfer idempotencyResult = transfersService
                .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders))
                .createTransfer(CreateTransferRequest.newBuilder()
                        .setType(TransferType.QUOTA_TRANSFER)
                        .setAddConfirmation(false)
                        .setParameters(TransferParameters.newBuilder()
                                .setQuotaTransfer(QuotaTransferParameters.newBuilder()
                                        .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                .setFolderId(folderOne.getId())
                                                .addResourceTransfers(ResourceTransfer.newBuilder()
                                                        .setDelta(TransferAmount.newBuilder()
                                                                .setValue(100L)
                                                                .setUnitKey("bytes")
                                                                .build())
                                                        .setResourceId(resource.getId())
                                                        .build())
                                                .build())
                                        .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                .setFolderId(folderTwo.getId())
                                                .addResourceTransfers(ResourceTransfer.newBuilder()
                                                        .setDelta(TransferAmount.newBuilder()
                                                                .setValue(-100L)
                                                                .setUnitKey("bytes")
                                                                .build())
                                                        .setResourceId(resource.getId())
                                                        .build())
                                                .build()))
                                .build())
                        .build());
        Assertions.assertNotNull(idempotencyResult);
        Assertions.assertEquals(result.getTransferId(), idempotencyResult.getTransferId());
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
        Transfer result = transfersService
                .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                .createTransfer(CreateTransferRequest.newBuilder()
                        .setType(TransferType.QUOTA_TRANSFER)
                        .setAddConfirmation(false)
                        .setParameters(TransferParameters.newBuilder()
                                .setQuotaTransfer(QuotaTransferParameters.newBuilder()
                                        .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                .setFolderId(folderOne.getId())
                                                .addResourceTransfers(ResourceTransfer.newBuilder()
                                                        .setDelta(TransferAmount.newBuilder()
                                                                .setValue(100L)
                                                                .setUnitKey("bytes")
                                                                .build())
                                                        .setResourceId(resource.getId())
                                                        .build())
                                                .build())
                                        .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                .setFolderId(folderTwo.getId())
                                                .addResourceTransfers(ResourceTransfer.newBuilder()
                                                        .setDelta(TransferAmount.newBuilder()
                                                                .setValue(-100L)
                                                                .setUnitKey("bytes")
                                                                .build())
                                                        .setResourceId(resource.getId())
                                                        .build())
                                                .build()))
                                .build())
                        .build());
        Assertions.assertNotNull(result);
        String idempotencyKey = UUID.randomUUID().toString();
        Metadata extraHeaders = new Metadata();
        Metadata.Key<String> headerKey = Metadata.Key.of("Idempotency-Key", Metadata.ASCII_STRING_MARSHALLER);
        extraHeaders.put(headerKey, idempotencyKey);
        Transfer cancelResult = transfersService
                .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders))
                .cancelTransfer(CancelTransferRequest.newBuilder()
                        .setTransferId(result.getTransferId())
                        .setVersion(result.getVersion())
                        .build());
        Assertions.assertNotNull(cancelResult);
        Assertions.assertEquals(TransferType.QUOTA_TRANSFER, cancelResult.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_QUOTA_TRANSFER, cancelResult.getTransferSubtype());
        Assertions.assertEquals(TransferStatus.CANCELLED, cancelResult.getStatus());
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                                TransactionMode.SERIALIZABLE_READ_WRITE,
                                txSession -> pendingTransferRequestsDao.getById(txSession,
                                        result.getTransferId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Assertions.assertNotNull(pendingTransferRequest);
        Assertions.assertEquals(
                Optional.empty(),
                pendingTransferRequest.map(PendingTransferRequestsModel::getRequestId));
        Assertions.assertEquals("1120000000000010", cancelResult.getCreatedBy().getUid());
        Assertions.assertTrue(cancelResult.getVotes().getVotersList().isEmpty());
        Assertions.assertEquals(2, cancelResult.getResponsible().getGroupedList().size());
        Assertions.assertTrue(cancelResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId())));
        Assertions.assertTrue(cancelResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId())));
        Assertions.assertTrue(cancelResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010"))));
        Assertions.assertTrue(cancelResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012"))));
        Assertions.assertTrue(cancelResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010"))));
        Assertions.assertTrue(cancelResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012"))));
        Assertions.assertTrue(cancelResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010")
                                && r.getServiceIdsList().contains(1L))));
        Assertions.assertTrue(cancelResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012")
                                && r.getServiceIdsList().contains(1L))));
        Assertions.assertTrue(cancelResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010")
                                && r.getServiceIdsList().contains(2L))));
        Assertions.assertTrue(cancelResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012")
                                && r.getServiceIdsList().contains(2L))));
        Assertions.assertEquals(2, cancelResult.getParameters().getQuotaTransfer()
                .getQuotaTransfersList().size());
        Assertions.assertTrue(cancelResult.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())));
        Assertions.assertTrue(cancelResult.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderTwo.getId())));
        Assertions.assertTrue(cancelResult.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())
                        && t.getResourceTransfersList().stream()
                        .anyMatch(r -> r.getResourceId().equals(resource.getId())
                                && r.getDelta().getValue() == 100L
                                && r.getDelta().getUnitKey().equals("bytes"))));
        Assertions.assertTrue(cancelResult.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfersList().stream()
                        .anyMatch(r -> r.getResourceId().equals(resource.getId())
                                && r.getDelta().getValue() == -100L
                                && r.getDelta().getUnitKey().equals("bytes"))));
        Transfer idempotencyCancelResult = transfersService
                .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders))
                .cancelTransfer(CancelTransferRequest.newBuilder()
                        .setTransferId(result.getTransferId())
                        .setVersion(result.getVersion())
                        .build());
        Assertions.assertNotNull(idempotencyCancelResult);
        Assertions.assertEquals(TransferStatus.CANCELLED, idempotencyCancelResult.getStatus());
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
        Transfer result = transfersService
                .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                .createTransfer(CreateTransferRequest.newBuilder()
                        .setType(TransferType.QUOTA_TRANSFER)
                        .setAddConfirmation(false)
                        .setParameters(TransferParameters.newBuilder()
                                .setQuotaTransfer(QuotaTransferParameters.newBuilder()
                                        .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                .setFolderId(folderOne.getId())
                                                .addResourceTransfers(ResourceTransfer.newBuilder()
                                                        .setDelta(TransferAmount.newBuilder()
                                                                .setValue(100L)
                                                                .setUnitKey("bytes")
                                                                .build())
                                                        .setResourceId(resource.getId())
                                                        .build())
                                                .build())
                                        .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                .setFolderId(folderTwo.getId())
                                                .addResourceTransfers(ResourceTransfer.newBuilder()
                                                        .setDelta(TransferAmount.newBuilder()
                                                                .setValue(-100L)
                                                                .setUnitKey("bytes")
                                                                .build())
                                                        .setResourceId(resource.getId())
                                                        .build())
                                                .build()))
                                .build())
                        .build());
        Assertions.assertNotNull(result);
        String idempotencyKey = UUID.randomUUID().toString();
        Metadata extraHeaders = new Metadata();
        Metadata.Key<String> headerKey = Metadata.Key.of("Idempotency-Key", Metadata.ASCII_STRING_MARSHALLER);
        extraHeaders.put(headerKey, idempotencyKey);
        Transfer voteResult = transfersService
                .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders))
                .voteForTransfer(VoteForTransferRequest.newBuilder()
                        .setTransferId(result.getTransferId())
                        .setVersion(result.getVersion())
                        .setVote(VoteType.CONFIRM)
                        .build());
        Assertions.assertNotNull(voteResult);
        Assertions.assertEquals(TransferType.QUOTA_TRANSFER, voteResult.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_QUOTA_TRANSFER, voteResult.getTransferSubtype());
        Assertions.assertEquals(TransferStatus.APPLIED, voteResult.getStatus());
        Assertions.assertEquals("1120000000000010", voteResult.getCreatedBy().getUid());
        Assertions.assertEquals(1, voteResult.getVotes().getVotersList().size());
        Assertions.assertEquals("1120000000000010",
                voteResult.getVotes().getVotersList().get(0).getVoter().getUid());
        Assertions.assertEquals(VoteType.CONFIRM,
                voteResult.getVotes().getVotersList().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId(), folderTwo.getId()),
                new HashSet<>(voteResult.getVotes().getVotersList().get(0).getFolderIdsList()));
        Assertions.assertEquals(Set.of(1L, 2L),
                new HashSet<>(voteResult.getVotes().getVotersList().get(0).getServiceIdsList()));
        Assertions.assertEquals(2, voteResult.getResponsible().getGroupedList().size());
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId())));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId())));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010"))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012"))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010"))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012"))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010")
                                && r.getServiceIdsList().contains(1L))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012")
                                && r.getServiceIdsList().contains(1L))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010")
                                && r.getServiceIdsList().contains(2L))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012")
                                && r.getServiceIdsList().contains(2L))));
        Assertions.assertEquals(2, voteResult.getParameters().getQuotaTransfer().getQuotaTransfersList().size());
        Assertions.assertTrue(voteResult.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())));
        Assertions.assertTrue(voteResult.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderTwo.getId())));
        Assertions.assertTrue(voteResult.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())
                        && t.getResourceTransfersList().stream()
                        .anyMatch(r -> r.getResourceId().equals(resource.getId())
                                && r.getDelta().getValue() == 100L
                                && r.getDelta().getUnitKey().equals("bytes"))));
        Assertions.assertTrue(voteResult.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfersList().stream()
                        .anyMatch(r -> r.getResourceId().equals(resource.getId())
                                && r.getDelta().getValue() == -100L
                                && r.getDelta().getUnitKey().equals("bytes"))));
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
                .uri("/front/transfers/{id}/history", voteResult.getTransferId())
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
        Transfer idempotencyVoteResult = transfersService
                .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(extraHeaders))
                .voteForTransfer(VoteForTransferRequest.newBuilder()
                        .setTransferId(result.getTransferId())
                        .setVersion(result.getVersion())
                        .setVote(VoteType.CONFIRM)
                        .build());
        Assertions.assertNotNull(idempotencyVoteResult);
        Assertions.assertEquals(TransferStatus.APPLIED, idempotencyVoteResult.getStatus());
    }

}
