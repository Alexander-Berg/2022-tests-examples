package ru.yandex.intranet.d.grpc.transfers;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.yandex.ydb.table.transaction.TransactionMode;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.backend.service.proto.CancelTransferRequest;
import ru.yandex.intranet.d.backend.service.proto.CreateTransferRequest;
import ru.yandex.intranet.d.backend.service.proto.GetTransferRequest;
import ru.yandex.intranet.d.backend.service.proto.QuotaTransfer;
import ru.yandex.intranet.d.backend.service.proto.QuotaTransferParameters;
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
import ru.yandex.intranet.d.dao.transfers.PendingTransferRequestsDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
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
import ru.yandex.intranet.d.web.model.SortOrderDto;

import static ru.yandex.intranet.d.TestUsers.USER_2_UID;

/**
 * Transfer requests GRPC public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class PublicApiTransferRequestsGrpcTest {

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
        Transfer getResult = transfersService
                .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                .getTransfer(GetTransferRequest.newBuilder()
                .setTransferId(result.getTransferId())
                .build());
        Assertions.assertNotNull(getResult);
        Assertions.assertEquals(TransferType.QUOTA_TRANSFER, getResult.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_QUOTA_TRANSFER, getResult.getTransferSubtype());
        Assertions.assertEquals(TransferStatus.PENDING, getResult.getStatus());
        Assertions.assertEquals("1120000000000010", getResult.getCreatedBy().getUid());
        Assertions.assertTrue(getResult.getVotes().getVotersList().isEmpty());
        Assertions.assertEquals(2, getResult.getResponsible().getGroupedList().size());
        Assertions.assertTrue(getResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId())));
        Assertions.assertTrue(getResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId())));
        Assertions.assertTrue(getResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010"))));
        Assertions.assertTrue(getResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012"))));
        Assertions.assertTrue(getResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010"))));
        Assertions.assertTrue(getResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012"))));
        Assertions.assertTrue(getResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010")
                                && r.getServiceIdsList().contains(1L))));
        Assertions.assertTrue(getResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012")
                                && r.getServiceIdsList().contains(1L))));
        Assertions.assertTrue(getResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010")
                                && r.getServiceIdsList().contains(2L))));
        Assertions.assertTrue(getResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012")
                                && r.getServiceIdsList().contains(2L))));
        Assertions.assertEquals(2, getResult.getParameters().getQuotaTransfer().getQuotaTransfersList().size());
        Assertions.assertTrue(getResult.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())));
        Assertions.assertTrue(getResult.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderTwo.getId())));
        Assertions.assertTrue(getResult.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())
                        && t.getResourceTransfersList().stream()
                        .anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().getValue() == 100L
                        && r.getDelta().getUnitKey().equals("bytes"))));
        Assertions.assertTrue(getResult.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfersList().stream()
                        .anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().getValue() == -100L
                        && r.getDelta().getUnitKey().equals("bytes"))));
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
        Transfer result = transfersService
                .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                .createTransfer(CreateTransferRequest.newBuilder()
                        .setType(TransferType.QUOTA_TRANSFER)
                        .setAddConfirmation(true)
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
        Assertions.assertEquals("1120000000000010", result.getCreatedBy().getUid());
        Assertions.assertEquals(1, result.getVotes().getVotersList().size());
        Assertions.assertEquals("1120000000000010",
                result.getVotes().getVotersList().get(0).getVoter().getUid());
        Assertions.assertEquals(VoteType.CONFIRM,
                result.getVotes().getVotersList().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId(), folderTwo.getId()),
                new HashSet<>(result.getVotes().getVotersList().get(0).getFolderIdsList()));
        Assertions.assertEquals(Set.of(1L, 2L),
                new HashSet<>(result.getVotes().getVotersList().get(0).getServiceIdsList()));
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
        Transfer result = transfersService
                .withCallCredentials(MockGrpcUser.uid(USER_2_UID))
                .createTransfer(CreateTransferRequest.newBuilder()
                        .setType(TransferType.QUOTA_TRANSFER)
                        .setAddConfirmation(true)
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
        Assertions.assertEquals(USER_2_UID, result.getCreatedBy().getUid());
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
        Transfer cancelResult = transfersService
                .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
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
