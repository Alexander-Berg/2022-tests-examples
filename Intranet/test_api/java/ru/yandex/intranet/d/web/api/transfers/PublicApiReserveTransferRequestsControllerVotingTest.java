package ru.yandex.intranet.d.web.api.transfers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuple8;

import ru.yandex.intranet.d.IntegrationTest;
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
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.model.resources.ResourceModel;
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.SortOrderDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestStatusDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestSubtypeDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestVoteTypeDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateReserveResourceTransferDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateReserveTransferDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateTransferRequestParametersDto;
import ru.yandex.intranet.d.web.model.transfers.api.TransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.api.TransferRequestVoterDto;
import ru.yandex.intranet.d.web.model.transfers.api.TransferRequestVotingDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestHistoryEventsPageDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestSearchDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestsPageDto;

import static ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_DISPENSER;
import static ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER_2_UID;
import static ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER_UID;
import static ru.yandex.intranet.d.TestUsers.USER_1_UID;
import static ru.yandex.intranet.d.TestUsers.USER_2_UID;
import static ru.yandex.intranet.d.web.api.transfers.SecondMorePublicApiTransferRequestsControllerTest.FOLDER_SERVICE_ID;
import static ru.yandex.intranet.d.web.api.transfers.SecondMorePublicApiTransferRequestsControllerTest.PROVIDER_SERVICE_ID;

/**
 * Reserve transfer requests voting public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class PublicApiReserveTransferRequestsControllerVotingTest {
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

    public PublicApiReserveTransferRequestsControllerVotingTest(
            @Value("${abc.roles.quotaManager}") long quotaManagerRoleId,
            @Value("${abc.roles.responsibleOfProvider}") long responsibleOfProvide) {
        this.quotaManagerRoleId = quotaManagerRoleId;
        this.responsibleOfProvide = responsibleOfProvide;
    }

    private Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel,
            QuotaModel, QuotaModel> prepareWithRoles() {
        return SecondMorePublicApiTransferRequestsControllerTest.prepareWithRoles(tableClient, servicesDao,
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

        TransferRequestDto result = webClient
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
                                                .delta(50000L)
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
                .isOk()
                .expectBody(TransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        TransferRequestDto voteResult = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/api/v1/transfers/{id}/_vote", result.getId())
                .accept(MediaType.APPLICATION_JSON)
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
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, voteResult.getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                voteResult.getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, voteResult.getStatus());
        Assertions.assertEquals(USER_2_UID, voteResult.getCreatedBy());
        Assertions.assertEquals(1, voteResult.getVotes().getVoters().size());
        Assertions.assertEquals(USER_2_UID, voteResult.getVotes().getVoters().get(0).getVoter());
        Assertions.assertEquals(TransferRequestVoteTypeDto.CONFIRM,
                voteResult.getVotes().getVoters().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()),
                new HashSet<>(voteResult.getVotes().getVoters().get(0).getFolderIds()));
        Assertions.assertEquals(Set.of(FOLDER_SERVICE_ID),
                new HashSet<>(voteResult.getVotes().getVoters().get(0).getServiceIds()));
        Assertions.assertEquals(1, voteResult.getTransferResponsible().getGrouped().size());
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId())));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID)
                                && r.getServiceIds().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(voteResult.getTransferResponsible().getProviderReserveResponsible().isEmpty());
        Assertions.assertEquals(voteResult.getTransferResponsible().getProviderReserveResponsible().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, voteResult.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().size());
        Assertions.assertTrue(voteResult.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().contains(USER_1_UID));
        Assertions.assertEquals(PROVIDER_SERVICE_ID, voteResult.getTransferResponsible()
                .getProviderReserveResponsible().get(0).getServiceId());
        Assertions.assertEquals(folderTwo.getId(), voteResult.getTransferResponsible()
                .getProviderReserveResponsible().get(0).getFolderId());
        Assertions.assertTrue(voteResult.getParameters().getReserveTransfer().isPresent());
        Assertions.assertEquals(folderOne.getId(), voteResult.getParameters().getReserveTransfer().get().getFolderId());
        Assertions.assertTrue(voteResult.getParameters().getReserveTransfer().get().getResourceTransfers()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta() == 50000L
                        && r.getDeltaUnitKey().equals("bytes")));

        TransferRequestDto voteResult2 = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/api/v1/transfers/{id}/_vote", voteResult.getId())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(TransferRequestVotingDto.builder()
                        .voteType(TransferRequestVoteTypeDto.CONFIRM)
                        .version(voteResult.getVersion())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(voteResult2);
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, voteResult2.getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                voteResult2.getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, voteResult2.getStatus());
        Assertions.assertEquals(USER_2_UID, voteResult2.getCreatedBy());
        Assertions.assertEquals(2, voteResult2.getVotes().getVoters().size());
        Map<String, TransferRequestVoterDto> voteByUser =
                voteResult2.getVotes().getVoters().stream()
                        .collect(Collectors.toMap(TransferRequestVoterDto::getVoter, Function.identity()));
        TransferRequestVoterDto vote = voteByUser.get(USER_1_UID);
        Assertions.assertNotNull(vote);
        Assertions.assertEquals(TransferRequestVoteTypeDto.CONFIRM, vote.getVoteType());
        Assertions.assertEquals(Set.of(folderTwo.getId()), new HashSet<>(vote.getFolderIds()));
        Assertions.assertEquals(Set.of(PROVIDER_SERVICE_ID), new HashSet<>(vote.getServiceIds()));
        Assertions.assertEquals(1, voteResult2.getTransferResponsible().getGrouped().size());
        Assertions.assertTrue(voteResult2.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId())));
        Assertions.assertTrue(voteResult2.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID))));
        Assertions.assertTrue(voteResult2.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(voteResult2.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(voteResult2.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID)
                                && r.getServiceIds().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(voteResult2.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(voteResult2.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(voteResult2.getTransferResponsible().getProviderReserveResponsible().isEmpty());
        Assertions.assertEquals(voteResult2.getTransferResponsible().getProviderReserveResponsible().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, voteResult2.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().size());
        Assertions.assertTrue(voteResult2.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().contains(USER_1_UID));
        Assertions.assertEquals(PROVIDER_SERVICE_ID, voteResult2.getTransferResponsible()
                .getProviderReserveResponsible().get(0).getServiceId());
        Assertions.assertEquals(folderTwo.getId(), voteResult2.getTransferResponsible()
                .getProviderReserveResponsible().get(0).getFolderId());
        Assertions.assertTrue(voteResult2.getParameters().getReserveTransfer().isPresent());
        Assertions.assertEquals(folderOne.getId(),
                voteResult2.getParameters().getReserveTransfer().get().getFolderId());
        Assertions.assertTrue(voteResult2.getParameters().getReserveTransfer().get().getResourceTransfers()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta() == 50000L
                        && r.getDeltaUnitKey().equals("bytes")));
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
                .uri("/front/transfers/{id}/history", voteResult.getId())
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

        TransferRequestDto result = webClient
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
                                                .delta(50000L)
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
                .isOk()
                .expectBody(TransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        TransferRequestDto voteResult = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/api/v1/transfers/{id}/_vote", result.getId())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(TransferRequestVotingDto.builder()
                        .voteType(TransferRequestVoteTypeDto.REJECT)
                        .version(result.getVersion())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(voteResult);
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, voteResult.getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                voteResult.getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.REJECTED, voteResult.getStatus());
        Assertions.assertEquals(USER_2_UID, voteResult.getCreatedBy());
        Assertions.assertEquals(1, voteResult.getVotes().getVoters().size());
        Assertions.assertEquals(USER_2_UID,
                voteResult.getVotes().getVoters().get(0).getVoter());
        Assertions.assertEquals(TransferRequestVoteTypeDto.REJECT,
                voteResult.getVotes().getVoters().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()),
                new HashSet<>(voteResult.getVotes().getVoters().get(0).getFolderIds()));
        Assertions.assertEquals(Set.of(FOLDER_SERVICE_ID),
                new HashSet<>(voteResult.getVotes().getVoters().get(0).getServiceIds()));
        Assertions.assertEquals(1, voteResult.getTransferResponsible().getGrouped().size());
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId())));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID)
                                && r.getServiceIds().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(voteResult.getTransferResponsible().getProviderReserveResponsible().isEmpty());
        Assertions.assertEquals(voteResult.getTransferResponsible().getProviderReserveResponsible().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, voteResult.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().size());
        Assertions.assertTrue(voteResult.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().contains(USER_1_UID));
        Assertions.assertEquals(PROVIDER_SERVICE_ID, voteResult.getTransferResponsible()
                .getProviderReserveResponsible().get(0).getServiceId());
        Assertions.assertEquals(folderTwo.getId(), voteResult.getTransferResponsible()
                .getProviderReserveResponsible().get(0).getFolderId());
        Assertions.assertFalse(voteResult.getParameters().getReserveTransfer().isEmpty());
        Assertions.assertEquals(folderOne.getId(), voteResult.getParameters().getReserveTransfer().get().getFolderId());
        Assertions.assertTrue(voteResult.getParameters().getReserveTransfer().get().getResourceTransfers()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta() == 50000L
                        && r.getDeltaUnitKey().equals("bytes")));
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

        TransferRequestDto result = webClient
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
                                                .delta(50000L)
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
                .isOk()
                .expectBody(TransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        TransferRequestDto voteResult = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/api/v1/transfers/{id}/_vote", result.getId())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(TransferRequestVotingDto.builder()
                        .voteType(TransferRequestVoteTypeDto.ABSTAIN)
                        .version(result.getVersion())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(voteResult);
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, voteResult.getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                voteResult.getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, voteResult.getStatus());
        Assertions.assertEquals(USER_2_UID, voteResult.getCreatedBy());
        Assertions.assertEquals(1, voteResult.getVotes().getVoters().size());
        Assertions.assertEquals(USER_2_UID,
                voteResult.getVotes().getVoters().get(0).getVoter());
        Assertions.assertEquals(TransferRequestVoteTypeDto.ABSTAIN,
                voteResult.getVotes().getVoters().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()),
                new HashSet<>(voteResult.getVotes().getVoters().get(0).getFolderIds()));
        Assertions.assertEquals(Set.of(FOLDER_SERVICE_ID),
                new HashSet<>(voteResult.getVotes().getVoters().get(0).getServiceIds()));
        Assertions.assertEquals(1, voteResult.getTransferResponsible().getGrouped().size());
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId())));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID)
                                && r.getServiceIds().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(voteResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(voteResult.getTransferResponsible().getProviderReserveResponsible().isEmpty());
        Assertions.assertEquals(voteResult.getTransferResponsible().getProviderReserveResponsible().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, voteResult.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().size());
        Assertions.assertTrue(voteResult.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().contains(USER_1_UID));
        Assertions.assertEquals(PROVIDER_SERVICE_ID, voteResult.getTransferResponsible()
                .getProviderReserveResponsible().get(0).getServiceId());
        Assertions.assertEquals(folderTwo.getId(), voteResult.getTransferResponsible()
                .getProviderReserveResponsible().get(0).getFolderId());
        Assertions.assertTrue(voteResult.getParameters().getReserveTransfer().isPresent());
        Assertions.assertEquals(folderOne.getId(), voteResult.getParameters().getReserveTransfer().get().getFolderId());
        Assertions.assertTrue(voteResult.getParameters().getReserveTransfer().get().getResourceTransfers()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta() == 50000L
                        && r.getDeltaUnitKey().equals("bytes")));
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

        TransferRequestDto result = webClient
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
                                                .delta(50000L)
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
                .isOk()
                .expectBody(TransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        TransferRequestDto voteResultOne = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/api/v1/transfers/{id}/_vote", result.getId())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(TransferRequestVotingDto.builder()
                        .voteType(TransferRequestVoteTypeDto.ABSTAIN)
                        .version(result.getVersion())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(voteResultOne);
        TransferRequestDto voteResultTwo = webClient
                .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_UID))
                .post()
                .uri("/api/v1/transfers/{id}/_vote", voteResultOne.getId())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(TransferRequestVotingDto.builder()
                        .voteType(TransferRequestVoteTypeDto.ABSTAIN)
                        .version(voteResultOne.getVersion())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(voteResultTwo);
        TransferRequestDto voteResultThree = webClient
                .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_2_UID))
                .post()
                .uri("/api/v1/transfers/{id}/_vote", voteResultTwo.getId())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(TransferRequestVotingDto.builder()
                        .voteType(TransferRequestVoteTypeDto.ABSTAIN)
                        .version(voteResultTwo.getVersion())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(voteResultThree);
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, voteResultThree
                .getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                voteResultThree.getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.REJECTED, voteResultThree.getStatus());
        Assertions.assertEquals(USER_2_UID, voteResultThree.getCreatedBy());
        Assertions.assertEquals(3, voteResultThree.getVotes().getVoters().size());
        Map<String, TransferRequestVoterDto> voteByUser =
                voteResultThree.getVotes().getVoters().stream()
                        .collect(Collectors.toMap(TransferRequestVoterDto::getVoter, Function.identity()));
        TransferRequestVoterDto voteOne = voteByUser.get(USER_2_UID);
        Assertions.assertNotNull(voteOne);
        Assertions.assertEquals(TransferRequestVoteTypeDto.ABSTAIN, voteOne.getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()), new HashSet<>(voteOne.getFolderIds()));
        Assertions.assertEquals(Set.of(FOLDER_SERVICE_ID), new HashSet<>(voteOne.getServiceIds()));
        TransferRequestVoterDto voteTwo = voteByUser.get(SERVICE_1_QUOTA_MANAGER_UID);
        Assertions.assertNotNull(voteTwo);
        Assertions.assertEquals(TransferRequestVoteTypeDto.ABSTAIN, voteTwo.getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()), new HashSet<>(voteTwo.getFolderIds()));
        Assertions.assertEquals(Set.of(TEST_SERVICE_ID_DISPENSER),
                new HashSet<>(voteTwo.getServiceIds()));
        TransferRequestVoterDto voteThree = voteByUser.get(SERVICE_1_QUOTA_MANAGER_2_UID);
        Assertions.assertNotNull(voteThree);
        Assertions.assertEquals(TransferRequestVoteTypeDto.ABSTAIN, voteThree.getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()), new HashSet<>(voteThree.getFolderIds()));
        Assertions.assertEquals(Set.of(TEST_SERVICE_ID_DISPENSER),
                new HashSet<>(voteThree.getServiceIds()));
        Assertions.assertEquals(1, voteResultThree.getTransferResponsible()
                .getGrouped().size());
        Assertions.assertTrue(voteResultThree.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId())));
        Assertions.assertTrue(voteResultThree.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID))));
        Assertions.assertTrue(voteResultThree.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(voteResultThree.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(voteResultThree.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID)
                                && r.getServiceIds().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(voteResultThree.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(voteResultThree.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(voteResultThree.getTransferResponsible().getProviderReserveResponsible()
                .isEmpty());
        Assertions.assertEquals(voteResultThree.getTransferResponsible().getProviderReserveResponsible().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, voteResultThree.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().size());
        Assertions.assertTrue(voteResultThree.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().contains(USER_1_UID));
        Assertions.assertEquals(PROVIDER_SERVICE_ID, voteResultThree
                .getTransferResponsible()
                .getProviderReserveResponsible().get(0).getServiceId());
        Assertions.assertEquals(folderTwo.getId(), voteResultThree.getTransferResponsible()
                .getProviderReserveResponsible().get(0).getFolderId());
        Assertions.assertFalse(voteResultThree.getParameters().getReserveTransfer().isEmpty());
        Assertions.assertEquals(folderOne.getId(),
                voteResultThree.getParameters().getReserveTransfer().get().getFolderId());
        Assertions.assertTrue(voteResultThree.getParameters().getReserveTransfer().get()
                .getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta() == 50000L
                        && r.getDeltaUnitKey().equals("bytes")));
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
                .uri("/front/transfers/{id}/history", voteResultThree.getId())
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

        TransferRequestDto result = webClient
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
                                                .delta(50000L)
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
                .isOk()
                .expectBody(TransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        TransferRequestDto voteResultOne = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/api/v1/transfers/{id}/_vote", result.getId())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(TransferRequestVotingDto.builder()
                        .voteType(TransferRequestVoteTypeDto.ABSTAIN)
                        .version(result.getVersion())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(voteResultOne);
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, voteResultOne
                .getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                voteResultOne.getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.REJECTED, voteResultOne.getStatus());
        Assertions.assertEquals(USER_2_UID, voteResultOne.getCreatedBy());
        Assertions.assertEquals(1, voteResultOne.getVotes().getVoters().size());
        Map<String, TransferRequestVoterDto> voteByUser =
                voteResultOne.getVotes().getVoters().stream()
                        .collect(Collectors.toMap(TransferRequestVoterDto::getVoter, Function.identity()));
        TransferRequestVoterDto voteOne = voteByUser.get(USER_1_UID);
        Assertions.assertNotNull(voteOne);
        Assertions.assertEquals(TransferRequestVoteTypeDto.ABSTAIN, voteOne.getVoteType());
        Assertions.assertEquals(Set.of(folderTwo.getId()), new HashSet<>(voteOne.getFolderIds()));
        Assertions.assertEquals(Set.of(PROVIDER_SERVICE_ID), new HashSet<>(voteOne.getServiceIds()));
        Assertions.assertEquals(1, voteResultOne.getTransferResponsible()
                .getGrouped().size());
        Assertions.assertTrue(voteResultOne.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId())));
        Assertions.assertTrue(voteResultOne.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID))));
        Assertions.assertTrue(voteResultOne.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(voteResultOne.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(voteResultOne.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID)
                                && r.getServiceIds().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(voteResultOne.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(voteResultOne.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(voteResultOne.getTransferResponsible().getProviderReserveResponsible()
                .isEmpty());
        Assertions.assertEquals(voteResultOne.getTransferResponsible().getProviderReserveResponsible().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, voteResultOne.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().size());
        Assertions.assertTrue(voteResultOne.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().contains(USER_1_UID));
        Assertions.assertEquals(PROVIDER_SERVICE_ID, voteResultOne
                .getTransferResponsible()
                .getProviderReserveResponsible().get(0).getServiceId());
        Assertions.assertEquals(folderTwo.getId(), voteResultOne.getTransferResponsible()
                .getProviderReserveResponsible().get(0).getFolderId());
        Assertions.assertTrue(voteResultOne.getParameters().getReserveTransfer().isPresent());
        Assertions.assertEquals(folderOne.getId(),
                voteResultOne.getParameters().getReserveTransfer().get().getFolderId());
        Assertions.assertTrue(voteResultOne.getParameters().getReserveTransfer().get().getResourceTransfers()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta() == 50000L
                        && r.getDeltaUnitKey().equals("bytes")));
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
                .uri("/front/transfers/{id}/history", voteResultOne.getId())
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
