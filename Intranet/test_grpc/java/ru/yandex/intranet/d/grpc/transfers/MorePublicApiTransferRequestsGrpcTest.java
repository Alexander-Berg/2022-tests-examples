package ru.yandex.intranet.d.grpc.transfers;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import com.yandex.ydb.table.transaction.TransactionMode;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestUsers;
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
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
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
import ru.yandex.intranet.d.utils.ErrorsHelper;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.SortOrderDto;

import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_3_ID;
import static ru.yandex.intranet.d.TestProviders.CLAUD2_ID;
import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestResources.YP_HDD_MAN;
import static ru.yandex.intranet.d.TestResources.YP_HDD_SAS;
import static ru.yandex.intranet.d.grpc.transfers.PublicApiTransferRequestsGrpcTest.folderModel;
import static ru.yandex.intranet.d.grpc.transfers.PublicApiTransferRequestsGrpcTest.providerModel;
import static ru.yandex.intranet.d.grpc.transfers.PublicApiTransferRequestsGrpcTest.providerModelBuilder;
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
public class MorePublicApiTransferRequestsGrpcTest {
    private static final Random RANDOM = new Random();

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
        Transfer result = transfersService
                .withCallCredentials(MockGrpcUser.uid("1120000000000003"))
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
        Assertions.assertEquals(TransferStatus.APPLIED, result.getStatus());
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
        Assertions.assertEquals("1120000000000003", result.getCreatedBy().getUid());
        Assertions.assertEquals(1, result.getVotes().getVotersList().size());
        Assertions.assertEquals("1120000000000003",
                result.getVotes().getVotersList().get(0).getVoter().getUid());
        Assertions.assertEquals(VoteType.CONFIRM, result.getVotes().getVotersList().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId(), folderTwo.getId()),
                new HashSet<>(result.getVotes().getVotersList().get(0).getFolderIdsList()));
        Assertions.assertEquals(Set.of(),
                new HashSet<>(result.getVotes().getVotersList().get(0).getServiceIdsList()));
        Assertions.assertEquals(Set.of(provider.getId()),
                new HashSet<>(result.getVotes().getVotersList().get(0).getProviderIdsList()));
        Assertions.assertEquals(0, result.getResponsible().getGroupedList().size());
        Assertions.assertEquals(1, result.getResponsible().getProviderSuperResponsibleList().size());
        Assertions.assertTrue(result.getResponsible().getProviderSuperResponsibleList().stream()
                .anyMatch(g -> g.getResponsible().getUid().equals("1120000000000003")));
        Assertions.assertTrue(result.getResponsible().getProviderSuperResponsibleList().stream()
                .anyMatch(g -> g.getProviderIdsList().contains(provider.getId())));
        Assertions.assertEquals(2, result.getParameters().getQuotaTransfer().getQuotaTransfersList().size());
        Assertions.assertTrue(result.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())));
        Assertions.assertTrue(result.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderTwo.getId())));
        Assertions.assertTrue(result.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())
                        && t.getResourceTransfersList().stream()
                                .anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().getValue() == 100L && r.getDelta().getUnitKey().equals("bytes"))));
        Assertions.assertTrue(result.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfersList().stream()
                                .anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().getValue() == -100L && r.getDelta().getUnitKey().equals("bytes"))));
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
        try {
            transfersService
                    .withCallCredentials(MockGrpcUser.uid("1120000000000003"))
                    .createTransfer(CreateTransferRequest.newBuilder()
                            .setType(TransferType.QUOTA_TRANSFER)
                            .setAddConfirmation(false)
                            .setParameters(TransferParameters.newBuilder()
                                    .setQuotaTransfer(QuotaTransferParameters.newBuilder()
                                            .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                    .setFolderId(folderOne.getId())
                                                    .addResourceTransfers(ResourceTransfer.newBuilder()
                                                            .setDelta(TransferAmount.newBuilder()
                                                                    .setValue(160L)
                                                                    .setUnitKey("bytes")
                                                                    .build())
                                                            .setResourceId(resource.getId())
                                                            .build())
                                                    .build())
                                            .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                    .setFolderId(folderTwo.getId())
                                                    .addResourceTransfers(ResourceTransfer.newBuilder()
                                                            .setDelta(TransferAmount.newBuilder()
                                                                    .setValue(-160L)
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
    public void createTransferRequestToClosingServiceTest() {
        try {
            transfersService
                    .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                    .createTransfer(CreateTransferRequest.newBuilder()
                            .setType(TransferType.QUOTA_TRANSFER)
                            .setAddConfirmation(false)
                            .setParameters(TransferParameters.newBuilder()
                                    .setQuotaTransfer(QuotaTransferParameters.newBuilder()
                                            .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                    .setFolderId(TestFolders.TEST_FOLDER_IN_CLOSING_SERVICE)
                                                    .addResourceTransfers(ResourceTransfer.newBuilder()
                                                            .setDelta(TransferAmount.newBuilder()
                                                                    .setValue(1L)
                                                                    .setUnitKey("gigabytes")
                                                                    .build())
                                                            .setResourceId(YP_HDD_MAN)
                                                            .build())
                                                    .build())
                                            .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                    .setFolderId(TEST_FOLDER_1_ID)
                                                    .addResourceTransfers(ResourceTransfer.newBuilder()
                                                            .setDelta(TransferAmount.newBuilder()
                                                                    .setValue(-1L)
                                                                    .setUnitKey("gigabytes")
                                                                    .build())
                                                            .setResourceId(YP_HDD_MAN)
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
    public void createTransferRequestFromClosingServiceTest() {
        transfersService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .createTransfer(CreateTransferRequest.newBuilder()
                        .setType(TransferType.QUOTA_TRANSFER)
                        .setAddConfirmation(false)
                        .setParameters(TransferParameters.newBuilder()
                                .setQuotaTransfer(QuotaTransferParameters.newBuilder()
                                        .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                .setFolderId(TestFolders.TEST_FOLDER_IN_CLOSING_SERVICE)
                                                .addResourceTransfers(ResourceTransfer.newBuilder()
                                                        .setDelta(TransferAmount.newBuilder()
                                                                .setValue(-1L)
                                                                .setUnitKey("gigabytes")
                                                                .build())
                                                        .setResourceId(YP_HDD_MAN)
                                                        .build())
                                                .build())
                                        .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                .setFolderId(TEST_FOLDER_1_ID)
                                                .addResourceTransfers(ResourceTransfer.newBuilder()
                                                        .setDelta(TransferAmount.newBuilder()
                                                                .setValue(1L)
                                                                .setUnitKey("gigabytes")
                                                                .build())
                                                        .setResourceId(YP_HDD_MAN)
                                                        .build())
                                                .build()))
                                .build())
                        .build());
    }

    @Test
    public void createTransferRequestToNonExportableServiceTest() {
        try {
            transfersService
                    .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                    .createTransfer(CreateTransferRequest.newBuilder()
                            .setType(TransferType.QUOTA_TRANSFER)
                            .setAddConfirmation(false)
                            .setParameters(TransferParameters.newBuilder()
                                    .setQuotaTransfer(QuotaTransferParameters.newBuilder()
                                            .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                    .setFolderId(TestFolders.TEST_FOLDER_IN_NON_EXPORTABLE_SERVICE)
                                                    .addResourceTransfers(ResourceTransfer.newBuilder()
                                                            .setDelta(TransferAmount.newBuilder()
                                                                    .setValue(1L)
                                                                    .setUnitKey("gigabytes")
                                                                    .build())
                                                            .setResourceId(YP_HDD_MAN)
                                                            .build())
                                                    .build())
                                            .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                    .setFolderId(TEST_FOLDER_1_ID)
                                                    .addResourceTransfers(ResourceTransfer.newBuilder()
                                                            .setDelta(TransferAmount.newBuilder()
                                                                    .setValue(-1L)
                                                                    .setUnitKey("gigabytes")
                                                                    .build())
                                                            .setResourceId(YP_HDD_MAN)
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
    public void createTransferRequestFromNonExportableServiceTest() {
        transfersService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .createTransfer(CreateTransferRequest.newBuilder()
                        .setType(TransferType.QUOTA_TRANSFER)
                        .setAddConfirmation(false)
                        .setParameters(TransferParameters.newBuilder()
                                .setQuotaTransfer(QuotaTransferParameters.newBuilder()
                                        .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                .setFolderId(TestFolders.TEST_FOLDER_IN_NON_EXPORTABLE_SERVICE)
                                                .addResourceTransfers(ResourceTransfer.newBuilder()
                                                        .setDelta(TransferAmount.newBuilder()
                                                                .setValue(-1L)
                                                                .setUnitKey("gigabytes")
                                                                .build())
                                                        .setResourceId(YP_HDD_MAN)
                                                        .build())
                                                .build())
                                        .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                .setFolderId(TEST_FOLDER_1_ID)
                                                .addResourceTransfers(ResourceTransfer.newBuilder()
                                                        .setDelta(TransferAmount.newBuilder()
                                                                .setValue(1L)
                                                                .setUnitKey("gigabytes")
                                                                .build())
                                                        .setResourceId(YP_HDD_MAN)
                                                        .build())
                                                .build()))
                                .build())
                        .build());
    }

    @Test
    public void createTransferRequestToRenamingServiceTest() {
        transfersService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .createTransfer(CreateTransferRequest.newBuilder()
                        .setType(TransferType.QUOTA_TRANSFER)
                        .setAddConfirmation(false)
                        .setParameters(TransferParameters.newBuilder()
                                .setQuotaTransfer(QuotaTransferParameters.newBuilder()
                                        .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                .setFolderId(TestFolders.TEST_FOLDER_IN_RENAMING_SERVICE)
                                                .addResourceTransfers(ResourceTransfer.newBuilder()
                                                        .setDelta(TransferAmount.newBuilder()
                                                                .setValue(1L)
                                                                .setUnitKey("gigabytes")
                                                                .build())
                                                        .setResourceId(YP_HDD_MAN)
                                                        .build())
                                                .build())
                                        .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                .setFolderId(TEST_FOLDER_1_ID)
                                                .addResourceTransfers(ResourceTransfer.newBuilder()
                                                        .setDelta(TransferAmount.newBuilder()
                                                                .setValue(-1L)
                                                                .setUnitKey("gigabytes")
                                                                .build())
                                                        .setResourceId(YP_HDD_MAN)
                                                        .build())
                                                .build()))
                                .build())
                        .build());
    }

    @Test
    public void createTransferRequestFromRenamingServiceTest() {
        transfersService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .createTransfer(CreateTransferRequest.newBuilder()
                        .setType(TransferType.QUOTA_TRANSFER)
                        .setAddConfirmation(false)
                        .setParameters(TransferParameters.newBuilder()
                                .setQuotaTransfer(QuotaTransferParameters.newBuilder()
                                        .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                .setFolderId(TestFolders.TEST_FOLDER_IN_RENAMING_SERVICE)
                                                .addResourceTransfers(ResourceTransfer.newBuilder()
                                                        .setDelta(TransferAmount.newBuilder()
                                                                .setValue(-1L)
                                                                .setUnitKey("gigabytes")
                                                                .build())
                                                        .setResourceId(YP_HDD_MAN)
                                                        .build())
                                                .build())
                                        .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                .setFolderId(TEST_FOLDER_1_ID)
                                                .addResourceTransfers(ResourceTransfer.newBuilder()
                                                        .setDelta(TransferAmount.newBuilder()
                                                                .setValue(1L)
                                                                .setUnitKey("gigabytes")
                                                                .build())
                                                        .setResourceId(YP_HDD_MAN)
                                                        .build())
                                                .build()))
                                .build())
                        .build());
    }

    @Test
    public void createTransferRequestFailOnAbsentReserveTransferTest() {
        try {
            transfersService
                    .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                    .createTransfer(CreateTransferRequest.newBuilder()
                            .setType(TransferType.RESERVE_TRANSFER)
                            .setAddConfirmation(false)
                            .setParameters(TransferParameters.newBuilder()
                                    .setQuotaTransfer(QuotaTransferParameters.newBuilder()
                                            .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                    .setFolderId(TEST_FOLDER_1_ID)
                                                    .addResourceTransfers(ResourceTransfer.newBuilder()
                                                            .setDelta(TransferAmount.newBuilder()
                                                                    .setValue(100L)
                                                                    .setUnitKey("bytes")
                                                                    .build())
                                                            .setResourceId(YP_HDD_MAN)
                                                            .build())
                                                    .build())
                                            .addQuotaTransfers(QuotaTransfer.newBuilder()
                                                    .setFolderId(TEST_FOLDER_3_ID)
                                                    .addResourceTransfers(ResourceTransfer.newBuilder()
                                                            .setDelta(TransferAmount.newBuilder()
                                                                    .setValue(-100L)
                                                                    .setUnitKey("bytes")
                                                                    .build())
                                                            .setResourceId(YP_HDD_MAN)
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
    public void createTransferRequestFailOnDuplicateResourceIdTest() {
        try {
            transfersService
                    .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                    .createTransfer(CreateTransferRequest.newBuilder()
                            .setType(TransferType.RESERVE_TRANSFER)
                            .setAddConfirmation(false)
                            .setParameters(TransferParameters.newBuilder()
                                    .setReserveTransfer(ReserveTransferParameters.newBuilder()
                                            .setProviderId(YP_ID)
                                            .setFolderId(TEST_FOLDER_1_ID)
                                            .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                    .setDelta(TransferAmount.newBuilder()
                                                            .setValue(100L)
                                                            .setUnitKey("bytes")
                                                            .build())
                                                    .setResourceId(YP_HDD_MAN)
                                                    .build())
                                            .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                    .setDelta(TransferAmount.newBuilder()
                                                            .setValue(200L)
                                                            .setUnitKey("bytes")
                                                            .build())
                                                    .setResourceId(YP_HDD_MAN)
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
    public void createTransferRequestFailOnNotPositiveDeltaTest() {
        try {
            transfersService
                    .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                    .createTransfer(CreateTransferRequest.newBuilder()
                            .setType(TransferType.RESERVE_TRANSFER)
                            .setAddConfirmation(false)
                            .setParameters(TransferParameters.newBuilder()
                                    .setReserveTransfer(ReserveTransferParameters.newBuilder()
                                            .setProviderId(YP_ID)
                                            .setFolderId(TEST_FOLDER_1_ID)
                                            .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                    .setDelta(TransferAmount.newBuilder()
                                                            .setValue(-100L)
                                                            .setUnitKey("bytes")
                                                            .build())
                                                    .setResourceId(YP_HDD_MAN)
                                                    .build())
                                            .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                    .setDelta(TransferAmount.newBuilder()
                                                            .setValue(200L)
                                                            .setUnitKey("bytes")
                                                            .build())
                                                    .setResourceId(YP_HDD_SAS)
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
    public void createTransferRequestFailOnBadProviderTest() {
        boolean firstFailure = false;
        try {
            transfersService
                    .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                    .createTransfer(CreateTransferRequest.newBuilder()
                            .setType(TransferType.RESERVE_TRANSFER)
                            .setAddConfirmation(false)
                            .setParameters(TransferParameters.newBuilder()
                                    .setReserveTransfer(ReserveTransferParameters.newBuilder()
                                            .setProviderId(UUID.randomUUID().toString())
                                            .setFolderId(TEST_FOLDER_1_ID)
                                            .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                    .setDelta(TransferAmount.newBuilder()
                                                            .setValue(100L)
                                                            .setUnitKey("bytes")
                                                            .build())
                                                    .setResourceId(YP_HDD_MAN)
                                                    .build()))
                                    .build())
                            .build());
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getFieldErrorsCount() > 0);
            firstFailure = true;
        }
        Assertions.assertTrue(firstFailure);
        ProviderModel provider = providerBuilder()
                .deleted(true)
                .build();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, provider))).block();
        boolean secondFailure = false;
        try {
            transfersService
                    .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                    .createTransfer(CreateTransferRequest.newBuilder()
                            .setType(TransferType.RESERVE_TRANSFER)
                            .setAddConfirmation(false)
                            .setParameters(TransferParameters.newBuilder()
                                    .setReserveTransfer(ReserveTransferParameters.newBuilder()
                                            .setProviderId(provider.getId())
                                            .setFolderId(TEST_FOLDER_1_ID)
                                            .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                    .setDelta(TransferAmount.newBuilder()
                                                            .setValue(100L)
                                                            .setUnitKey("bytes")
                                                            .build())
                                                    .setResourceId(YP_HDD_MAN)
                                                    .build()))
                                    .build())
                            .build());
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getFieldErrorsCount() > 0);
            secondFailure = true;
        }
        Assertions.assertTrue(secondFailure);
        ProviderModel pr2 = providerBuilder()
                .managed(false)
                .build();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                                .upsertProviderRetryable(txSession, pr2))).block();
        boolean thirdFailure = false;
        try {
            transfersService
                    .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                    .createTransfer(CreateTransferRequest.newBuilder()
                            .setType(TransferType.RESERVE_TRANSFER)
                            .setAddConfirmation(false)
                            .setParameters(TransferParameters.newBuilder()
                                    .setReserveTransfer(ReserveTransferParameters.newBuilder()
                                            .setProviderId(pr2.getId())
                                            .setFolderId(TEST_FOLDER_1_ID)
                                            .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                    .setDelta(TransferAmount.newBuilder()
                                                            .setValue(100L)
                                                            .setUnitKey("bytes")
                                                            .build())
                                                    .setResourceId(YP_HDD_MAN)
                                                    .build()))
                                    .build())
                            .build());
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getFieldErrorsCount() > 0);
            thirdFailure = true;
        }
        Assertions.assertTrue(thirdFailure);
    }

    @Test
    public void createTransferRequestFailOnProviderWithoutReserveFolderTest() {
        try {
            transfersService
                    .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                    .createTransfer(CreateTransferRequest.newBuilder()
                            .setType(TransferType.RESERVE_TRANSFER)
                            .setAddConfirmation(false)
                            .setParameters(TransferParameters.newBuilder()
                                    .setReserveTransfer(ReserveTransferParameters.newBuilder()
                                            .setProviderId(CLAUD2_ID)
                                            .setFolderId(TEST_FOLDER_1_ID)
                                            .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                    .setDelta(TransferAmount.newBuilder()
                                                            .setValue(100L)
                                                            .setUnitKey("bytes")
                                                            .build())
                                                    .setResourceId(YP_HDD_MAN)
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
