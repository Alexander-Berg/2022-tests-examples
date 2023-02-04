package ru.yandex.intranet.d.grpc.transfers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.yandex.ydb.table.transaction.TransactionMode;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuple8;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.backend.service.proto.CreateTransferRequest;
import ru.yandex.intranet.d.backend.service.proto.ReserveResourceTransfer;
import ru.yandex.intranet.d.backend.service.proto.ReserveTransferParameters;
import ru.yandex.intranet.d.backend.service.proto.Transfer;
import ru.yandex.intranet.d.backend.service.proto.TransferAmount;
import ru.yandex.intranet.d.backend.service.proto.TransferParameters;
import ru.yandex.intranet.d.backend.service.proto.TransferStatus;
import ru.yandex.intranet.d.backend.service.proto.TransferSubtype;
import ru.yandex.intranet.d.backend.service.proto.TransferType;
import ru.yandex.intranet.d.backend.service.proto.TransferUser;
import ru.yandex.intranet.d.backend.service.proto.TransferVoter;
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
import ru.yandex.intranet.d.dao.users.AbcServiceMemberDao;
import ru.yandex.intranet.d.dao.users.UsersDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.model.resources.ResourceModel;
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.SortOrderDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestStatusDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestHistoryEventsPageDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestSearchDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestsPageDto;

import static ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_DISPENSER;
import static ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER_2_UID;
import static ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER_UID;
import static ru.yandex.intranet.d.TestUsers.USER_1_UID;
import static ru.yandex.intranet.d.TestUsers.USER_2_UID;
import static ru.yandex.intranet.d.grpc.transfers.SecondMorePublicApiTransferRequestsGrpcTest.FOLDER_SERVICE_ID;
import static ru.yandex.intranet.d.grpc.transfers.SecondMorePublicApiTransferRequestsGrpcTest.PROVIDER_SERVICE_ID;

/**
 * Reserve transfer requests voting GRPC public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class PublicApiReserveTransferRequestsVotingGrpcTest {

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

    private final long quotaManagerRoleId;
    private final long responsibleOfProvide;

    public PublicApiReserveTransferRequestsVotingGrpcTest(
            @Value("${abc.roles.quotaManager}") long quotaManagerRoleId,
            @Value("${abc.roles.responsibleOfProvider}") long responsibleOfProvide) {
        this.quotaManagerRoleId = quotaManagerRoleId;
        this.responsibleOfProvide = responsibleOfProvide;
    }

    private Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel,
            QuotaModel, QuotaModel> prepareWithRoles() {
        return SecondMorePublicApiTransferRequestsGrpcTest.prepareWithRoles(tableClient, servicesDao,
                abcServiceMemberDao, responsibleOfProvide, quotaManagerRoleId, usersDao, providersDao, resourceTypesDao,
                resourceSegmentationsDao, resourceSegmentsDao, resourcesDao, folderDao, quotasDao);
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void voteConfirmReserveTransferRequestTest() {
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepareWithRoles();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();
        FolderModel folderTwo = prepare.getT6();
        Transfer result = transfersService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
                .createTransfer(CreateTransferRequest.newBuilder()
                        .setType(TransferType.RESERVE_TRANSFER)
                        .setAddConfirmation(false)
                        .setParameters(TransferParameters.newBuilder()
                                .setReserveTransfer(ReserveTransferParameters.newBuilder()
                                        .setProviderId(providerModel.getId())
                                        .setFolderId(folderOne.getId())
                                        .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                .setDelta(TransferAmount.newBuilder()
                                                        .setValue(50000L)
                                                        .setUnitKey("bytes")
                                                        .build())
                                                .setResourceId(resourceOne.getId())
                                                .build()))
                                .build())
                        .build());
        Assertions.assertNotNull(result);
        Transfer voteResult = transfersService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
                .voteForTransfer(VoteForTransferRequest.newBuilder()
                        .setTransferId(result.getTransferId())
                        .setVersion(result.getVersion())
                        .setVote(VoteType.CONFIRM)
                        .build());
        Assertions.assertNotNull(voteResult);
        Assertions.assertEquals(TransferType.RESERVE_TRANSFER, voteResult.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_RESERVE_TRANSFER, voteResult.getTransferSubtype());
        Assertions.assertEquals(TransferStatus.PENDING, voteResult.getStatus());
        Assertions.assertEquals(USER_2_UID, voteResult.getCreatedBy().getUid());
        Assertions.assertEquals(1, voteResult.getVotes().getVotersList().size());
        Assertions.assertEquals(USER_2_UID, voteResult.getVotes().getVotersList().get(0).getVoter().getUid());
        Assertions.assertEquals(VoteType.CONFIRM,
                voteResult.getVotes().getVotersList().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()),
                new HashSet<>(voteResult.getVotes().getVotersList().get(0).getFolderIdsList()));
        Assertions.assertEquals(Set.of(FOLDER_SERVICE_ID),
                new HashSet<>(voteResult.getVotes().getVotersList().get(0).getServiceIdsList()));
        Assertions.assertEquals(1, voteResult.getResponsible().getGroupedList().size());
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId())));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID)
                                && r.getServiceIdsList().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(voteResult.getResponsible().getProviderReserveResponsibleList().isEmpty());
        Assertions.assertEquals(voteResult.getResponsible().getProviderReserveResponsibleList().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, voteResult.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().size());
        Assertions.assertTrue(voteResult.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().stream().map(TransferUser::getUid)
                .collect(Collectors.toList()).contains(USER_1_UID));
        Assertions.assertEquals(PROVIDER_SERVICE_ID, voteResult.getResponsible()
                .getProviderReserveResponsibleList().get(0).getServiceId());
        Assertions.assertEquals(folderTwo.getId(), voteResult.getResponsible()
                .getProviderReserveResponsibleList().get(0).getFolderId());
        Assertions.assertFalse(voteResult.getParameters().getReserveTransfer().getResourceTransfersList().isEmpty());
        Assertions.assertEquals(folderOne.getId(), voteResult.getParameters().getReserveTransfer().getFolderId());
        Assertions.assertTrue(voteResult.getParameters().getReserveTransfer().getResourceTransfersList()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().getValue() == 50000L
                        && r.getDelta().getUnitKey().equals("bytes")));
        Transfer voteResult2 = transfersService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .voteForTransfer(VoteForTransferRequest.newBuilder()
                        .setTransferId(voteResult.getTransferId())
                        .setVersion(voteResult.getVersion())
                        .setVote(VoteType.CONFIRM)
                        .build());
        Assertions.assertNotNull(voteResult2);
        Assertions.assertEquals(TransferType.RESERVE_TRANSFER, voteResult2.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_RESERVE_TRANSFER, voteResult2.getTransferSubtype());
        Assertions.assertEquals(TransferStatus.APPLIED, voteResult2.getStatus());
        Assertions.assertEquals(USER_2_UID, voteResult2.getCreatedBy().getUid());
        Assertions.assertEquals(2, voteResult2.getVotes().getVotersList().size());
        Map<String, TransferVoter> voteByUser =
                voteResult2.getVotes().getVotersList().stream()
                        .collect(Collectors.toMap(v -> v.getVoter().getUid(), Function.identity()));
        TransferVoter vote = voteByUser.get(USER_1_UID);
        Assertions.assertNotNull(vote);
        Assertions.assertEquals(VoteType.CONFIRM, vote.getVoteType());
        Assertions.assertEquals(Set.of(folderTwo.getId()), new HashSet<>(vote.getFolderIdsList()));
        Assertions.assertEquals(Set.of(PROVIDER_SERVICE_ID), new HashSet<>(vote.getServiceIdsList()));
        Assertions.assertEquals(1, voteResult2.getResponsible().getGroupedList().size());
        Assertions.assertTrue(voteResult2.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId())));
        Assertions.assertTrue(voteResult2.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID))));
        Assertions.assertTrue(voteResult2.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(voteResult2.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(voteResult2.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID)
                                && r.getServiceIdsList().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(voteResult2.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(voteResult2.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(voteResult2.getResponsible().getProviderReserveResponsibleList().isEmpty());
        Assertions.assertEquals(voteResult2.getResponsible().getProviderReserveResponsibleList().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, voteResult2.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().size());
        Assertions.assertTrue(voteResult2.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().stream().map(TransferUser::getUid)
                .collect(Collectors.toList()).contains(USER_1_UID));
        Assertions.assertEquals(PROVIDER_SERVICE_ID, voteResult2.getResponsible()
                .getProviderReserveResponsibleList().get(0).getServiceId());
        Assertions.assertEquals(folderTwo.getId(), voteResult2.getResponsible()
                .getProviderReserveResponsibleList().get(0).getFolderId());
        Assertions.assertFalse(voteResult2.getParameters().getReserveTransfer().getResourceTransfersList().isEmpty());
        Assertions.assertEquals(folderOne.getId(),
                voteResult2.getParameters().getReserveTransfer().getFolderId());
        Assertions.assertTrue(voteResult2.getParameters().getReserveTransfer().getResourceTransfersList()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().getValue() == 50000L
                        && r.getDelta().getUnitKey().equals("bytes")));
        List<QuotaModel> quotasOne = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderOne.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        List<QuotaModel> quotasTwo = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderTwo.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(quotasOne);
        Assertions.assertNotNull(quotasTwo);
        Map<String, QuotaModel> quotaByResourceOne = quotasOne.stream()
                .collect(Collectors.toMap(QuotaModel::getResourceId, Function.identity()));
        Map<String, QuotaModel> quotaByResourceTwo = quotasTwo.stream()
                .collect(Collectors.toMap(QuotaModel::getResourceId, Function.identity()));
        QuotaModel quotaOne = quotaByResourceOne.get(resourceOne.getId());
        QuotaModel quotaTwo = quotaByResourceTwo.get(resourceOne.getId());
        Assertions.assertEquals(60000, quotaOne.getQuota());
        Assertions.assertEquals(60000, quotaOne.getBalance());
        Assertions.assertEquals(0, quotaTwo.getQuota());
        Assertions.assertEquals(0, quotaTwo.getBalance());
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
        Assertions.assertNotNull(logsOne);
        Assertions.assertNotNull(logsTwo);
        Assertions.assertEquals(1, logsOne.size());
        Assertions.assertEquals(1, logsTwo.size());
        FrontTransferRequestHistoryEventsPageDto historyResult = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
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
        Assertions.assertEquals(3, historyResult.getEvents().size());
        FrontTransferRequestsPageDto searchResultByCurrentUser = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
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
                .mutateWith(MockUser.uid(USER_2_UID))
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
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers/_search")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestSearchDto.builder()
                        .filterByServiceId(String.valueOf(FOLDER_SERVICE_ID))
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
    public void voteRejectReserveTransferRequestTest() {
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepareWithRoles();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();
        FolderModel folderTwo = prepare.getT6();
        Transfer result = transfersService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
                .createTransfer(CreateTransferRequest.newBuilder()
                        .setType(TransferType.RESERVE_TRANSFER)
                        .setAddConfirmation(false)
                        .setParameters(TransferParameters.newBuilder()
                                .setReserveTransfer(ReserveTransferParameters.newBuilder()
                                        .setProviderId(providerModel.getId())
                                        .setFolderId(folderOne.getId())
                                        .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                .setDelta(TransferAmount.newBuilder()
                                                        .setValue(50000L)
                                                        .setUnitKey("bytes")
                                                        .build())
                                                .setResourceId(resourceOne.getId())
                                                .build()))
                                .build())
                        .build());
        Assertions.assertNotNull(result);
        Transfer voteResult = transfersService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
                .voteForTransfer(VoteForTransferRequest.newBuilder()
                        .setTransferId(result.getTransferId())
                        .setVersion(result.getVersion())
                        .setVote(VoteType.REJECT)
                        .build());
        Assertions.assertNotNull(voteResult);
        Assertions.assertEquals(TransferType.RESERVE_TRANSFER, voteResult.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_RESERVE_TRANSFER, voteResult.getTransferSubtype());
        Assertions.assertEquals(TransferStatus.REJECTED, voteResult.getStatus());
        Assertions.assertEquals(USER_2_UID, voteResult.getCreatedBy().getUid());
        Assertions.assertEquals(1, voteResult.getVotes().getVotersList().size());
        Assertions.assertEquals(USER_2_UID,
                voteResult.getVotes().getVotersList().get(0).getVoter().getUid());
        Assertions.assertEquals(VoteType.REJECT,
                voteResult.getVotes().getVotersList().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()),
                new HashSet<>(voteResult.getVotes().getVotersList().get(0).getFolderIdsList()));
        Assertions.assertEquals(Set.of(FOLDER_SERVICE_ID),
                new HashSet<>(voteResult.getVotes().getVotersList().get(0).getServiceIdsList()));
        Assertions.assertEquals(1, voteResult.getResponsible().getGroupedList().size());
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId())));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID)
                                && r.getServiceIdsList().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(voteResult.getResponsible().getProviderReserveResponsibleList().isEmpty());
        Assertions.assertEquals(voteResult.getResponsible().getProviderReserveResponsibleList().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, voteResult.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().size());
        Assertions.assertTrue(voteResult.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().stream().map(TransferUser::getUid)
                .collect(Collectors.toList()).contains(USER_1_UID));
        Assertions.assertEquals(PROVIDER_SERVICE_ID, voteResult.getResponsible()
                .getProviderReserveResponsibleList().get(0).getServiceId());
        Assertions.assertEquals(folderTwo.getId(), voteResult.getResponsible()
                .getProviderReserveResponsibleList().get(0).getFolderId());
        Assertions.assertFalse(voteResult.getParameters().getReserveTransfer().getResourceTransfersList().isEmpty());
        Assertions.assertEquals(folderOne.getId(), voteResult.getParameters().getReserveTransfer().getFolderId());
        Assertions.assertTrue(voteResult.getParameters().getReserveTransfer().getResourceTransfersList()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().getValue() == 50000L
                        && r.getDelta().getUnitKey().equals("bytes")));
        List<QuotaModel> quotasOne = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderOne.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        List<QuotaModel> quotasTwo = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderTwo.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(quotasOne);
        Assertions.assertNotNull(quotasTwo);
        Map<String, QuotaModel> quotaByResourceOne = quotasOne.stream()
                .collect(Collectors.toMap(QuotaModel::getResourceId, Function.identity()));
        Map<String, QuotaModel> quotaByResourceTwo = quotasTwo.stream()
                .collect(Collectors.toMap(QuotaModel::getResourceId, Function.identity()));
        QuotaModel quotaOne = quotaByResourceOne.get(resourceOne.getId());
        QuotaModel quotaTwo = quotaByResourceTwo.get(resourceOne.getId());
        Assertions.assertEquals(10000, quotaOne.getQuota());
        Assertions.assertEquals(10000, quotaOne.getBalance());
        Assertions.assertEquals(50000, quotaTwo.getQuota());
        Assertions.assertEquals(50000, quotaTwo.getBalance());
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
        Assertions.assertNotNull(logsOne);
        Assertions.assertNotNull(logsTwo);
        Assertions.assertEquals(0, logsOne.size());
        Assertions.assertEquals(0, logsTwo.size());
        FrontTransferRequestHistoryEventsPageDto historyResult = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
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
                .mutateWith(MockUser.uid(USER_2_UID))
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
                .mutateWith(MockUser.uid(USER_2_UID))
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
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers/_search")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestSearchDto.builder()
                        .filterByServiceId(String.valueOf(FOLDER_SERVICE_ID))
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
    public void voteAbstainReserveTransferRequestTest() {
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepareWithRoles();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();
        FolderModel folderTwo = prepare.getT6();
        Transfer result = transfersService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
                .createTransfer(CreateTransferRequest.newBuilder()
                        .setType(TransferType.RESERVE_TRANSFER)
                        .setAddConfirmation(false)
                        .setParameters(TransferParameters.newBuilder()
                                .setReserveTransfer(ReserveTransferParameters.newBuilder()
                                        .setProviderId(providerModel.getId())
                                        .setFolderId(folderOne.getId())
                                        .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                .setDelta(TransferAmount.newBuilder()
                                                        .setValue(50000L)
                                                        .setUnitKey("bytes")
                                                        .build())
                                                .setResourceId(resourceOne.getId())
                                                .build()))
                                .build())
                        .build());
        Assertions.assertNotNull(result);
        Transfer voteResult = transfersService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
                .voteForTransfer(VoteForTransferRequest.newBuilder()
                        .setTransferId(result.getTransferId())
                        .setVersion(result.getVersion())
                        .setVote(VoteType.ABSTAIN)
                        .build());
        Assertions.assertNotNull(voteResult);
        Assertions.assertEquals(TransferType.RESERVE_TRANSFER, voteResult.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_RESERVE_TRANSFER, voteResult.getTransferSubtype());
        Assertions.assertEquals(TransferStatus.PENDING, voteResult.getStatus());
        Assertions.assertEquals(USER_2_UID, voteResult.getCreatedBy().getUid());
        Assertions.assertEquals(1, voteResult.getVotes().getVotersList().size());
        Assertions.assertEquals(USER_2_UID,
                voteResult.getVotes().getVotersList().get(0).getVoter().getUid());
        Assertions.assertEquals(VoteType.ABSTAIN,
                voteResult.getVotes().getVotersList().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()),
                new HashSet<>(voteResult.getVotes().getVotersList().get(0).getFolderIdsList()));
        Assertions.assertEquals(Set.of(FOLDER_SERVICE_ID),
                new HashSet<>(voteResult.getVotes().getVotersList().get(0).getServiceIdsList()));
        Assertions.assertEquals(1, voteResult.getResponsible().getGroupedList().size());
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId())));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID)
                                && r.getServiceIdsList().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(voteResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(voteResult.getResponsible().getProviderReserveResponsibleList().isEmpty());
        Assertions.assertEquals(voteResult.getResponsible().getProviderReserveResponsibleList().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, voteResult.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().size());
        Assertions.assertTrue(voteResult.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().stream().map(TransferUser::getUid)
                .collect(Collectors.toList()).contains(USER_1_UID));
        Assertions.assertEquals(PROVIDER_SERVICE_ID, voteResult.getResponsible()
                .getProviderReserveResponsibleList().get(0).getServiceId());
        Assertions.assertEquals(folderTwo.getId(), voteResult.getResponsible()
                .getProviderReserveResponsibleList().get(0).getFolderId());
        Assertions.assertFalse(voteResult.getParameters().getReserveTransfer().getResourceTransfersList().isEmpty());
        Assertions.assertEquals(folderOne.getId(), voteResult.getParameters().getReserveTransfer().getFolderId());
        Assertions.assertTrue(voteResult.getParameters().getReserveTransfer().getResourceTransfersList()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().getValue() == 50000L
                        && r.getDelta().getUnitKey().equals("bytes")));
        List<QuotaModel> quotasOne = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderOne.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        List<QuotaModel> quotasTwo = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderTwo.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(quotasOne);
        Assertions.assertNotNull(quotasTwo);
        Map<String, QuotaModel> quotaByResourceOne = quotasOne.stream()
                .collect(Collectors.toMap(QuotaModel::getResourceId, Function.identity()));
        Map<String, QuotaModel> quotaByResourceTwo = quotasTwo.stream()
                .collect(Collectors.toMap(QuotaModel::getResourceId, Function.identity()));
        QuotaModel quotaOne = quotaByResourceOne.get(resourceOne.getId());
        QuotaModel quotaTwo = quotaByResourceTwo.get(resourceOne.getId());
        Assertions.assertEquals(10000, quotaOne.getQuota());
        Assertions.assertEquals(10000, quotaOne.getBalance());
        Assertions.assertEquals(50000, quotaTwo.getQuota());
        Assertions.assertEquals(50000, quotaTwo.getBalance());
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
        Assertions.assertNotNull(logsOne);
        Assertions.assertNotNull(logsTwo);
        Assertions.assertEquals(0, logsOne.size());
        Assertions.assertEquals(0, logsTwo.size());
        FrontTransferRequestHistoryEventsPageDto historyResult = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
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
        Assertions.assertNotNull(searchResultByCurrentUser);
        Assertions.assertEquals(1, searchResultByCurrentUser.getTransfers().size());
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
        Assertions.assertNotNull(searchResultByFolder);
        Assertions.assertEquals(1, searchResultByFolder.getTransfers().size());
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
        Assertions.assertNotNull(searchResultByService);
        Assertions.assertEquals(1, searchResultByService.getTransfers().size());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void voteAllAbstainReserveTransferRequestTest() {
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepareWithRoles();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();
        FolderModel folderTwo = prepare.getT6();
        Transfer result = transfersService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
                .createTransfer(CreateTransferRequest.newBuilder()
                        .setType(TransferType.RESERVE_TRANSFER)
                        .setAddConfirmation(false)
                        .setParameters(TransferParameters.newBuilder()
                                .setReserveTransfer(ReserveTransferParameters.newBuilder()
                                        .setProviderId(providerModel.getId())
                                        .setFolderId(folderOne.getId())
                                        .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                .setDelta(TransferAmount.newBuilder()
                                                        .setValue(50000L)
                                                        .setUnitKey("bytes")
                                                        .build())
                                                .setResourceId(resourceOne.getId())
                                                .build()))
                                .build())
                        .build());
        Assertions.assertNotNull(result);
        Transfer voteResultOne = transfersService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
                .voteForTransfer(VoteForTransferRequest.newBuilder()
                        .setTransferId(result.getTransferId())
                        .setVersion(result.getVersion())
                        .setVote(VoteType.ABSTAIN)
                        .build());
        Assertions.assertNotNull(voteResultOne);
        Transfer voteResultTwo = transfersService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
                .voteForTransfer(VoteForTransferRequest.newBuilder()
                        .setTransferId(voteResultOne.getTransferId())
                        .setVersion(voteResultOne.getVersion())
                        .setVote(VoteType.ABSTAIN)
                        .build());
        Assertions.assertNotNull(voteResultTwo);
        Transfer voteResultThree = transfersService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_2_UID))
                .voteForTransfer(VoteForTransferRequest.newBuilder()
                        .setTransferId(voteResultTwo.getTransferId())
                        .setVersion(voteResultTwo.getVersion())
                        .setVote(VoteType.ABSTAIN)
                        .build());
        Assertions.assertNotNull(voteResultThree);
        Assertions.assertEquals(TransferType.RESERVE_TRANSFER, voteResultThree.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_RESERVE_TRANSFER, voteResultThree.getTransferSubtype());
        Assertions.assertEquals(TransferStatus.REJECTED, voteResultThree.getStatus());
        Assertions.assertEquals(USER_2_UID, voteResultThree.getCreatedBy().getUid());
        Assertions.assertEquals(3, voteResultThree.getVotes().getVotersList().size());
        Map<String, TransferVoter> voteByUser =
                voteResultThree.getVotes().getVotersList().stream()
                        .collect(Collectors.toMap(v -> v.getVoter().getUid(), Function.identity()));
        TransferVoter voteOne = voteByUser.get(USER_2_UID);
        Assertions.assertNotNull(voteOne);
        Assertions.assertEquals(VoteType.ABSTAIN, voteOne.getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()), new HashSet<>(voteOne.getFolderIdsList()));
        Assertions.assertEquals(Set.of(FOLDER_SERVICE_ID), new HashSet<>(voteOne.getServiceIdsList()));
        TransferVoter voteTwo = voteByUser.get(SERVICE_1_QUOTA_MANAGER_UID);
        Assertions.assertNotNull(voteTwo);
        Assertions.assertEquals(VoteType.ABSTAIN, voteTwo.getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()), new HashSet<>(voteTwo.getFolderIdsList()));
        Assertions.assertEquals(Set.of(TEST_SERVICE_ID_DISPENSER),
                new HashSet<>(voteTwo.getServiceIdsList()));
        TransferVoter voteThree = voteByUser.get(SERVICE_1_QUOTA_MANAGER_2_UID);
        Assertions.assertNotNull(voteThree);
        Assertions.assertEquals(VoteType.ABSTAIN, voteThree.getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()), new HashSet<>(voteThree.getFolderIdsList()));
        Assertions.assertEquals(Set.of(TEST_SERVICE_ID_DISPENSER),
                new HashSet<>(voteThree.getServiceIdsList()));
        Assertions.assertEquals(1, voteResultThree.getResponsible()
                .getGroupedList().size());
        Assertions.assertTrue(voteResultThree.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId())));
        Assertions.assertTrue(voteResultThree.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID))));
        Assertions.assertTrue(voteResultThree.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(voteResultThree.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(voteResultThree.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID)
                                && r.getServiceIdsList().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(voteResultThree.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(voteResultThree.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(voteResultThree.getResponsible().getProviderReserveResponsibleList()
                .isEmpty());
        Assertions.assertEquals(voteResultThree.getResponsible().getProviderReserveResponsibleList().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, voteResultThree.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().size());
        Assertions.assertTrue(voteResultThree.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().stream().map(TransferUser::getUid)
                .collect(Collectors.toList()).contains(USER_1_UID));
        Assertions.assertEquals(PROVIDER_SERVICE_ID, voteResultThree.getResponsible()
                .getProviderReserveResponsibleList().get(0).getServiceId());
        Assertions.assertEquals(folderTwo.getId(), voteResultThree.getResponsible()
                .getProviderReserveResponsibleList().get(0).getFolderId());
        Assertions.assertFalse(voteResultThree.getParameters().getReserveTransfer()
                .getResourceTransfersList().isEmpty());
        Assertions.assertEquals(folderOne.getId(),
                voteResultThree.getParameters().getReserveTransfer().getFolderId());
        Assertions.assertTrue(voteResultThree.getParameters().getReserveTransfer()
                .getResourceTransfersList().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().getValue() == 50000L
                        && r.getDelta().getUnitKey().equals("bytes")));
        List<QuotaModel> quotasOne = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderOne.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        List<QuotaModel> quotasTwo = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderTwo.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(quotasOne);
        Assertions.assertNotNull(quotasTwo);
        Map<String, QuotaModel> quotaByResourceOne = quotasOne.stream()
                .collect(Collectors.toMap(QuotaModel::getResourceId, Function.identity()));
        Map<String, QuotaModel> quotaByResourceTwo = quotasTwo.stream()
                .collect(Collectors.toMap(QuotaModel::getResourceId, Function.identity()));
        QuotaModel quotaOne = quotaByResourceOne.get(resourceOne.getId());
        QuotaModel quotaTwo = quotaByResourceTwo.get(resourceOne.getId());
        Assertions.assertEquals(10000, quotaOne.getQuota());
        Assertions.assertEquals(10000, quotaOne.getBalance());
        Assertions.assertEquals(50000, quotaTwo.getQuota());
        Assertions.assertEquals(50000, quotaTwo.getBalance());
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
        Assertions.assertNotNull(logsOne);
        Assertions.assertNotNull(logsTwo);
        Assertions.assertEquals(0, logsOne.size());
        Assertions.assertEquals(0, logsTwo.size());
        FrontTransferRequestHistoryEventsPageDto historyResult = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .get()
                .uri("/front/transfers/{id}/history", voteResultThree.getTransferId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontTransferRequestHistoryEventsPageDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(historyResult);
        Assertions.assertEquals(4, historyResult.getEvents().size());
        FrontTransferRequestsPageDto searchResultByCurrentUser = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
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
                .mutateWith(MockUser.uid(USER_2_UID))
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
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers/_search")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestSearchDto.builder()
                        .filterByServiceId(String.valueOf(FOLDER_SERVICE_ID))
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
    public void voteAllAbstainByProviderReserveTransferRequestTest() {
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepareWithRoles();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();
        FolderModel folderTwo = prepare.getT6();
        Transfer result = transfersService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
                .createTransfer(CreateTransferRequest.newBuilder()
                        .setType(TransferType.RESERVE_TRANSFER)
                        .setAddConfirmation(false)
                        .setParameters(TransferParameters.newBuilder()
                                .setReserveTransfer(ReserveTransferParameters.newBuilder()
                                        .setProviderId(providerModel.getId())
                                        .setFolderId(folderOne.getId())
                                        .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                .setDelta(TransferAmount.newBuilder()
                                                        .setValue(50000L)
                                                        .setUnitKey("bytes")
                                                        .build())
                                                .setResourceId(resourceOne.getId())
                                                .build()))
                                .build())
                        .build());
        Assertions.assertNotNull(result);
        Transfer voteResultOne = transfersService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .voteForTransfer(VoteForTransferRequest.newBuilder()
                        .setTransferId(result.getTransferId())
                        .setVersion(result.getVersion())
                        .setVote(VoteType.ABSTAIN)
                        .build());
        Assertions.assertNotNull(voteResultOne);
        Assertions.assertEquals(TransferType.RESERVE_TRANSFER, voteResultOne.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_RESERVE_TRANSFER, voteResultOne.getTransferSubtype());
        Assertions.assertEquals(TransferStatus.REJECTED, voteResultOne.getStatus());
        Assertions.assertEquals(USER_2_UID, voteResultOne.getCreatedBy().getUid());
        Assertions.assertEquals(1, voteResultOne.getVotes().getVotersList().size());
        Map<String, TransferVoter> voteByUser =
                voteResultOne.getVotes().getVotersList().stream()
                        .collect(Collectors.toMap(v -> v.getVoter().getUid(), Function.identity()));
        TransferVoter voteOne = voteByUser.get(USER_1_UID);
        Assertions.assertNotNull(voteOne);
        Assertions.assertEquals(VoteType.ABSTAIN, voteOne.getVoteType());
        Assertions.assertEquals(Set.of(folderTwo.getId()), new HashSet<>(voteOne.getFolderIdsList()));
        Assertions.assertEquals(Set.of(PROVIDER_SERVICE_ID), new HashSet<>(voteOne.getServiceIdsList()));
        Assertions.assertEquals(1, voteResultOne.getResponsible().getGroupedList().size());
        Assertions.assertTrue(voteResultOne.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId())));
        Assertions.assertTrue(voteResultOne.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID))));
        Assertions.assertTrue(voteResultOne.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(voteResultOne.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(voteResultOne.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID)
                                && r.getServiceIdsList().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(voteResultOne.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(voteResultOne.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(voteResultOne.getResponsible().getProviderReserveResponsibleList()
                .isEmpty());
        Assertions.assertEquals(voteResultOne.getResponsible().getProviderReserveResponsibleList().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, voteResultOne.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().size());
        Assertions.assertTrue(voteResultOne.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().stream().map(TransferUser::getUid)
                .collect(Collectors.toList()).contains(USER_1_UID));
        Assertions.assertEquals(PROVIDER_SERVICE_ID, voteResultOne.getResponsible()
                .getProviderReserveResponsibleList().get(0).getServiceId());
        Assertions.assertEquals(folderTwo.getId(), voteResultOne.getResponsible()
                .getProviderReserveResponsibleList().get(0).getFolderId());
        Assertions.assertFalse(voteResultOne.getParameters().getReserveTransfer().getResourceTransfersList().isEmpty());
        Assertions.assertEquals(folderOne.getId(),
                voteResultOne.getParameters().getReserveTransfer().getFolderId());
        Assertions.assertTrue(voteResultOne.getParameters().getReserveTransfer().getResourceTransfersList()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().getValue() == 50000L
                        && r.getDelta().getUnitKey().equals("bytes")));
        List<QuotaModel> quotasOne = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderOne.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        List<QuotaModel> quotasTwo = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderTwo.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(quotasOne);
        Assertions.assertNotNull(quotasTwo);
        Map<String, QuotaModel> quotaByResourceOne = quotasOne.stream()
                .collect(Collectors.toMap(QuotaModel::getResourceId, Function.identity()));
        Map<String, QuotaModel> quotaByResourceTwo = quotasTwo.stream()
                .collect(Collectors.toMap(QuotaModel::getResourceId, Function.identity()));
        QuotaModel quotaOne = quotaByResourceOne.get(resourceOne.getId());
        QuotaModel quotaTwo = quotaByResourceTwo.get(resourceOne.getId());
        Assertions.assertEquals(10000, quotaOne.getQuota());
        Assertions.assertEquals(10000, quotaOne.getBalance());
        Assertions.assertEquals(50000, quotaTwo.getQuota());
        Assertions.assertEquals(50000, quotaTwo.getBalance());
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
        Assertions.assertNotNull(logsOne);
        Assertions.assertNotNull(logsTwo);
        Assertions.assertEquals(0, logsOne.size());
        Assertions.assertEquals(0, logsTwo.size());
        FrontTransferRequestHistoryEventsPageDto historyResult = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .get()
                .uri("/front/transfers/{id}/history", voteResultOne.getTransferId())
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
                .mutateWith(MockUser.uid(USER_2_UID))
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
                .mutateWith(MockUser.uid(USER_2_UID))
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
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers/_search")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestSearchDto.builder()
                        .filterByServiceId(String.valueOf(FOLDER_SERVICE_ID))
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
}
