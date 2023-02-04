package ru.yandex.intranet.d.grpc.transfers;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.yandex.ydb.table.transaction.TransactionMode;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.backend.service.proto.CreateTransferRequest;
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
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.SortOrderDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestStatusDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestHistoryEventsPageDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestSearchDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestsPageDto;

/**
 * Transfer requests voting GRPC public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class PublicApiTransferRequestsVotingGrpcTest {

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

    @Test
    @SuppressWarnings("MethodLength")
    public void voteConfirmTransferRequestTest() {
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
        Transfer voteResult = transfersService
                .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
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
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void voteRejectTransferRequestTest() {
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
        Transfer voteResult = transfersService
                .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                .voteForTransfer(VoteForTransferRequest.newBuilder()
                        .setTransferId(result.getTransferId())
                        .setVersion(result.getVersion())
                        .setVote(VoteType.REJECT)
                        .build());
        Assertions.assertNotNull(voteResult);
        Assertions.assertEquals(TransferType.QUOTA_TRANSFER, voteResult.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_QUOTA_TRANSFER, voteResult.getTransferSubtype());
        Assertions.assertEquals(TransferStatus.REJECTED, voteResult.getStatus());
        Assertions.assertEquals("1120000000000010", voteResult.getCreatedBy().getUid());
        Assertions.assertEquals(1, voteResult.getVotes().getVotersList().size());
        Assertions.assertEquals("1120000000000010",
                voteResult.getVotes().getVotersList().get(0).getVoter().getUid());
        Assertions.assertEquals(VoteType.REJECT,
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
        Assertions.assertEquals(2, voteResult.getParameters().getQuotaTransfer()
                .getQuotaTransfersList().size());
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
        Assertions.assertEquals(50, quotasOne.get(0).getQuota());
        Assertions.assertEquals(50, quotasOne.get(0).getBalance());
        Assertions.assertEquals(150, quotasTwo.get(0).getQuota());
        Assertions.assertEquals(150, quotasTwo.get(0).getBalance());
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
        Assertions.assertEquals(0, logsOne.size());
        Assertions.assertEquals(0, logsTwo.size());
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
                        .addFilterByStatus(TransferRequestStatusDto.REJECTED)
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
                        .addFilterByStatus(TransferRequestStatusDto.REJECTED)
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
                        .addFilterByStatus(TransferRequestStatusDto.REJECTED)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontTransferRequestsPageDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(searchResultByService);
        Assertions.assertEquals(1, searchResultByService.getTransfers().size());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void voteAbstainTransferRequestTest() {
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
        Transfer voteResult = transfersService
                .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                .voteForTransfer(VoteForTransferRequest.newBuilder()
                        .setTransferId(result.getTransferId())
                        .setVersion(result.getVersion())
                        .setVote(VoteType.ABSTAIN)
                        .build());
        Assertions.assertNotNull(voteResult);
        Assertions.assertEquals(TransferType.QUOTA_TRANSFER, voteResult.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_QUOTA_TRANSFER, voteResult.getTransferSubtype());
        Assertions.assertEquals(TransferStatus.PENDING, voteResult.getStatus());
        Assertions.assertEquals("1120000000000010", voteResult.getCreatedBy().getUid());
        Assertions.assertEquals(1, voteResult.getVotes().getVotersList().size());
        Assertions.assertEquals("1120000000000010",
                voteResult.getVotes().getVotersList().get(0).getVoter().getUid());
        Assertions.assertEquals(VoteType.ABSTAIN,
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
        Assertions.assertEquals(2, voteResult.getParameters().getQuotaTransfer()
                .getQuotaTransfersList().size());
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
        Assertions.assertEquals(50, quotasOne.get(0).getQuota());
        Assertions.assertEquals(50, quotasOne.get(0).getBalance());
        Assertions.assertEquals(150, quotasTwo.get(0).getQuota());
        Assertions.assertEquals(150, quotasTwo.get(0).getBalance());
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
        Assertions.assertEquals(0, logsOne.size());
        Assertions.assertEquals(0, logsTwo.size());
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
                        .addFilterByStatus(TransferRequestStatusDto.PENDING)
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
                        .addFilterByStatus(TransferRequestStatusDto.PENDING)
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
                        .addFilterByStatus(TransferRequestStatusDto.PENDING)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontTransferRequestsPageDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(searchResultByService);
        Assertions.assertEquals(1, searchResultByService.getTransfers().size());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void voteAllAbstainTransferRequestTest() {
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
        Transfer firstVoteResult = transfersService
                .withCallCredentials(MockGrpcUser.uid("1120000000000012"))
                .voteForTransfer(VoteForTransferRequest.newBuilder()
                        .setTransferId(result.getTransferId())
                        .setVersion(result.getVersion())
                        .setVote(VoteType.ABSTAIN)
                        .build());
        Assertions.assertNotNull(firstVoteResult);
        Transfer secondVoteResult = transfersService
                .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                .voteForTransfer(VoteForTransferRequest.newBuilder()
                        .setTransferId(firstVoteResult.getTransferId())
                        .setVersion(firstVoteResult.getVersion())
                        .setVote(VoteType.ABSTAIN)
                        .build());
        Assertions.assertNotNull(secondVoteResult);
        Assertions.assertEquals(TransferType.QUOTA_TRANSFER, secondVoteResult.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_QUOTA_TRANSFER, secondVoteResult.getTransferSubtype());
        Assertions.assertEquals(TransferStatus.REJECTED, secondVoteResult.getStatus());
        Assertions.assertEquals("1120000000000010", secondVoteResult.getCreatedBy().getUid());
        Assertions.assertEquals(2, secondVoteResult.getVotes().getVotersList().size());
        Assertions.assertTrue(secondVoteResult.getVotes().getVotersList().stream()
                .anyMatch(vote -> vote.getVoter().getUid().equals("1120000000000010")
                        && vote.getVoteType().equals(VoteType.ABSTAIN)
                        && new HashSet<>(vote.getFolderIdsList()).equals(Set.of(folderOne.getId(), folderTwo.getId()))
                        && new HashSet<>(vote.getServiceIdsList()).equals(Set.of(1L, 2L))));
        Assertions.assertTrue(secondVoteResult.getVotes().getVotersList().stream()
                .anyMatch(vote -> vote.getVoter().getUid().equals("1120000000000012")
                        && vote.getVoteType().equals(VoteType.ABSTAIN)
                        && new HashSet<>(vote.getFolderIdsList()).equals(Set.of(folderOne.getId(), folderTwo.getId()))
                        && new HashSet<>(vote.getServiceIdsList()).equals(Set.of(1L, 2L))));
        Assertions.assertEquals(2,
                secondVoteResult.getResponsible().getGroupedList().size());
        Assertions.assertTrue(secondVoteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId())));
        Assertions.assertTrue(secondVoteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId())));
        Assertions.assertTrue(secondVoteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010"))));
        Assertions.assertTrue(secondVoteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012"))));
        Assertions.assertTrue(secondVoteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010"))));
        Assertions.assertTrue(secondVoteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012"))));
        Assertions.assertTrue(secondVoteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010")
                                && r.getServiceIdsList().contains(1L))));
        Assertions.assertTrue(secondVoteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012")
                                && r.getServiceIdsList().contains(1L))));
        Assertions.assertTrue(secondVoteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000010")
                                && r.getServiceIdsList().contains(2L))));
        Assertions.assertTrue(secondVoteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderTwo.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals("1120000000000012")
                                && r.getServiceIdsList().contains(2L))));
        Assertions.assertEquals(2, secondVoteResult.getParameters().getQuotaTransfer()
                .getQuotaTransfersList().size());
        Assertions.assertTrue(secondVoteResult.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())));
        Assertions.assertTrue(secondVoteResult.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderTwo.getId())));
        Assertions.assertTrue(secondVoteResult.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
                .anyMatch(t -> t.getFolderId().equals(folderOne.getId())
                        && t.getResourceTransfersList().stream()
                        .anyMatch(r -> r.getResourceId().equals(resource.getId())
                        && r.getDelta().getValue() == 100L
                        && r.getDelta().getUnitKey().equals("bytes"))));
        Assertions.assertTrue(secondVoteResult.getParameters().getQuotaTransfer().getQuotaTransfersList().stream()
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
        Assertions.assertEquals(50, quotasOne.get(0).getQuota());
        Assertions.assertEquals(50, quotasOne.get(0).getBalance());
        Assertions.assertEquals(150, quotasTwo.get(0).getQuota());
        Assertions.assertEquals(150, quotasTwo.get(0).getBalance());
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
        Assertions.assertEquals(0, logsOne.size());
        Assertions.assertEquals(0, logsTwo.size());
        FrontTransferRequestHistoryEventsPageDto historyResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/transfers/{id}/history", secondVoteResult.getTransferId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontTransferRequestHistoryEventsPageDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(historyResult);
        Assertions.assertEquals(3, historyResult.getEvents().size());
        FrontTransferRequestsPageDto searchResultByCurrentUser = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers/_search")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestSearchDto.builder()
                        .filterByCurrentUser(true)
                        .addFilterByStatus(TransferRequestStatusDto.REJECTED)
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
                        .addFilterByStatus(TransferRequestStatusDto.REJECTED)
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
                        .addFilterByStatus(TransferRequestStatusDto.REJECTED)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontTransferRequestsPageDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(searchResultByService);
        Assertions.assertEquals(1, searchResultByService.getTransfers().size());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void voteConfirmFailureTransferRequestTest() {
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
        QuotaModel mismatchedQuotaOne = quotaModel(provider.getId(), resource.getId(), folderOne.getId(), 50, 50);
        QuotaModel mismatchedQuotaTwo = quotaModel(provider.getId(), resource.getId(), folderTwo.getId(), 50, 50);
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
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .upsertAllRetryable(txSession, List.of(mismatchedQuotaOne, mismatchedQuotaTwo))))
                .block();
        Transfer voteResult = transfersService
                .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                .voteForTransfer(VoteForTransferRequest.newBuilder()
                        .setTransferId(result.getTransferId())
                        .setVersion(result.getVersion())
                        .setVote(VoteType.CONFIRM)
                        .build());
        Assertions.assertNotNull(voteResult);
        Assertions.assertEquals(TransferType.QUOTA_TRANSFER, voteResult.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_QUOTA_TRANSFER, voteResult.getTransferSubtype());
        Assertions.assertEquals(TransferStatus.FAILED, voteResult.getStatus());
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
        Assertions.assertEquals(2, voteResult.getParameters().getQuotaTransfer()
                .getQuotaTransfersList().size());
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
        Assertions.assertEquals(50, quotasOne.get(0).getQuota());
        Assertions.assertEquals(50, quotasOne.get(0).getBalance());
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
        Assertions.assertEquals(0, logsOne.size());
        Assertions.assertEquals(0, logsTwo.size());
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
                        .addFilterByStatus(TransferRequestStatusDto.FAILED)
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
                        .addFilterByStatus(TransferRequestStatusDto.FAILED)
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
                        .addFilterByStatus(TransferRequestStatusDto.FAILED)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontTransferRequestsPageDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(searchResultByService);
        Assertions.assertEquals(1, searchResultByService.getTransfers().size());
    }

    private ProviderModel providerModel(String grpcUri, String restUri, boolean accountsSpacesSupported) {
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
                .serviceId(69L)
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
                .accountsSpacesSupported(accountsSpacesSupported)
                .syncEnabled(true)
                .grpcTlsOn(true)
                .build();
    }

    private ResourceTypeModel resourceTypeModel(String providerId, String key, String unitsEnsembleId) {
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

    private ResourceSegmentationModel resourceSegmentationModel(String providerId, String key) {
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

    private ResourceSegmentModel resourceSegmentModel(String segmentationId, String key) {
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
    private ResourceModel resourceModel(String providerId, String key, String resourceTypeId,
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

    private FolderModel folderModel(long serviceId) {
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

    private QuotaModel quotaModel(String providerId, String resourceId, String folderId, long quota, long balance) {
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
