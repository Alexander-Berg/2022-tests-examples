package ru.yandex.intranet.d.web.front.transfer;

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
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaResourceTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateReserveTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestParametersDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontSingleTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestHistoryEventsPageDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestSearchDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestVoteDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestVotingDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestsPageDto;

import static ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_DISPENSER;
import static ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER;
import static ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER_2;
import static ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER_2_UID;
import static ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER_UID;
import static ru.yandex.intranet.d.TestUsers.USER_1_ID;
import static ru.yandex.intranet.d.TestUsers.USER_1_UID;
import static ru.yandex.intranet.d.TestUsers.USER_2_ID;
import static ru.yandex.intranet.d.TestUsers.USER_2_UID;
import static ru.yandex.intranet.d.UnitIds.BYTES;
import static ru.yandex.intranet.d.UnitIds.KILOBYTES;
import static ru.yandex.intranet.d.web.front.transfer.SecondMoreFrontTransferRequestsControllerTest.FOLDER_SERVICE_ID;
import static ru.yandex.intranet.d.web.front.transfer.SecondMoreFrontTransferRequestsControllerTest.PROVIDER_SERVICE_ID;

/**
 * Front reserve transfer requests voting API test.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@IntegrationTest
public class FrontReserveTransferRequestsControllerVotingTest {
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

    public FrontReserveTransferRequestsControllerVotingTest(
            @Value("${abc.roles.quotaManager}") long quotaManagerRoleId,
            @Value("${abc.roles.responsibleOfProvider}") long responsibleOfProvide) {
        this.quotaManagerRoleId = quotaManagerRoleId;
        this.responsibleOfProvide = responsibleOfProvide;
    }

    private Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel,
            QuotaModel, QuotaModel> prepareWithRoles() {
        return SecondMoreFrontTransferRequestsControllerTest.prepareWithRoles(tableClient, servicesDao,
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

        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("50000")
                                                .resourceId(resourceOne.getId())
                                                .deltaUnitId(BYTES)
                                                .build())
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId(String.valueOf(folderOne.getServiceId()))
                                        .providerId(providerModel.getId())
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
        FrontSingleTransferRequestDto voteResult = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers/{id}/_vote?version={version}",
                        result.getTransfer().getId(), result.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestVotingDto.builder()
                        .voteType(TransferRequestVoteTypeDto.CONFIRM)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(voteResult);
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, voteResult.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                voteResult.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, voteResult.getTransfer().getStatus());
        Assertions.assertEquals(USER_2_ID, voteResult.getTransfer().getCreatedBy());
        Assertions.assertEquals(1, voteResult.getTransfer().getTransferVotes().getVotes().size());
        Assertions.assertEquals(USER_2_ID,
                voteResult.getTransfer().getTransferVotes().getVotes().get(0).getUserId());
        Assertions.assertEquals(TransferRequestVoteTypeDto.CONFIRM,
                voteResult.getTransfer().getTransferVotes().getVotes().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()),
                new HashSet<>(voteResult.getTransfer().getTransferVotes().getVotes().get(0).getFolderIds()));
        Assertions.assertEquals(Set.of(String.valueOf(FOLDER_SERVICE_ID)),
                new HashSet<>(voteResult.getTransfer().getTransferVotes().getVotes().get(0).getServiceIds()));
        Assertions.assertEquals(1, voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getReserveResponsible().isPresent());
        Assertions.assertEquals(voteResult.getTransfer().getTransferResponsible().getReserveResponsible().get()
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, voteResult.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().size());
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().contains(USER_1_ID));
        Assertions.assertEquals(String.valueOf(PROVIDER_SERVICE_ID), voteResult.getTransfer().getTransferResponsible()
                .getReserveResponsible().get().getServiceId());
        Assertions.assertEquals(folderTwo.getId(), voteResult.getTransfer().getTransferResponsible()
                .getReserveResponsible().get().getFolderId());
        Assertions.assertEquals(2, voteResult.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(voteResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(voteResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        Assertions.assertTrue(voteResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        Assertions.assertTrue(voteResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        Assertions.assertFalse(voteResult.getTransfer().isCanVote());

        FrontSingleTransferRequestDto voteResult2 = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/front/transfers/{id}/_vote?version={version}",
                        voteResult.getTransfer().getId(), voteResult.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestVotingDto.builder()
                        .voteType(TransferRequestVoteTypeDto.CONFIRM)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(voteResult2);
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, voteResult2.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                voteResult2.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, voteResult2.getTransfer().getStatus());
        Assertions.assertEquals(USER_2_ID, voteResult2.getTransfer().getCreatedBy());
        Assertions.assertEquals(2, voteResult2.getTransfer().getTransferVotes().getVotes().size());
        Map<String, FrontTransferRequestVoteDto> voteByUser =
                voteResult2.getTransfer().getTransferVotes().getVotes().stream()
                        .collect(Collectors.toMap(FrontTransferRequestVoteDto::getUserId, Function.identity()));
        FrontTransferRequestVoteDto vote = voteByUser.get(USER_1_ID);
        Assertions.assertNotNull(vote);
        Assertions.assertEquals(TransferRequestVoteTypeDto.CONFIRM, vote.getVoteType());
        Assertions.assertEquals(Set.of(folderTwo.getId()), new HashSet<>(vote.getFolderIds()));
        Assertions.assertEquals(Set.of(String.valueOf(PROVIDER_SERVICE_ID)), new HashSet<>(vote.getServiceIds()));
        Assertions.assertEquals(1, voteResult2.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(voteResult2.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(voteResult2.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        Assertions.assertTrue(voteResult2.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(voteResult2.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(voteResult2.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        Assertions.assertTrue(voteResult2.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(voteResult2.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(voteResult2.getTransfer().getTransferResponsible().getReserveResponsible().isPresent());
        Assertions.assertEquals(voteResult2.getTransfer().getTransferResponsible().getReserveResponsible().get()
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, voteResult2.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().size());
        Assertions.assertTrue(voteResult2.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().contains(USER_1_ID));
        Assertions.assertEquals(String.valueOf(PROVIDER_SERVICE_ID), voteResult2.getTransfer().getTransferResponsible()
                .getReserveResponsible().get().getServiceId());
        Assertions.assertEquals(folderTwo.getId(), voteResult2.getTransfer().getTransferResponsible()
                .getReserveResponsible().get().getFolderId());
        Assertions.assertEquals(2, voteResult2.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(voteResult2.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(voteResult2.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        Assertions.assertTrue(voteResult2.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        Assertions.assertTrue(voteResult2.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        Assertions.assertFalse(voteResult2.getTransfer().isCanVote());
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
                .uri("/front/transfers/{id}/history", voteResult.getTransfer().getId())
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

        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("50000")
                                                .resourceId(resourceOne.getId())
                                                .deltaUnitId(BYTES)
                                                .build())
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId(String.valueOf(folderOne.getServiceId()))
                                        .providerId(providerModel.getId())
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
        FrontSingleTransferRequestDto voteResult = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers/{id}/_vote?version={version}",
                        result.getTransfer().getId(), result.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestVotingDto.builder()
                        .voteType(TransferRequestVoteTypeDto.REJECT)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(voteResult);
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, voteResult.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                voteResult.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.REJECTED, voteResult.getTransfer().getStatus());
        Assertions.assertEquals(USER_2_ID, voteResult.getTransfer().getCreatedBy());
        Assertions.assertEquals(1, voteResult.getTransfer().getTransferVotes().getVotes().size());
        Assertions.assertEquals(USER_2_ID,
                voteResult.getTransfer().getTransferVotes().getVotes().get(0).getUserId());
        Assertions.assertEquals(TransferRequestVoteTypeDto.REJECT,
                voteResult.getTransfer().getTransferVotes().getVotes().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()),
                new HashSet<>(voteResult.getTransfer().getTransferVotes().getVotes().get(0).getFolderIds()));
        Assertions.assertEquals(Set.of(String.valueOf(FOLDER_SERVICE_ID)),
                new HashSet<>(voteResult.getTransfer().getTransferVotes().getVotes().get(0).getServiceIds()));
        Assertions.assertEquals(1, voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getReserveResponsible().isPresent());
        Assertions.assertEquals(voteResult.getTransfer().getTransferResponsible().getReserveResponsible().get()
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, voteResult.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().size());
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().contains(USER_1_ID));
        Assertions.assertEquals(String.valueOf(PROVIDER_SERVICE_ID), voteResult.getTransfer().getTransferResponsible()
                .getReserveResponsible().get().getServiceId());
        Assertions.assertEquals(folderTwo.getId(), voteResult.getTransfer().getTransferResponsible()
                .getReserveResponsible().get().getFolderId());
        Assertions.assertEquals(2, voteResult.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(voteResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(voteResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        Assertions.assertTrue(voteResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        Assertions.assertTrue(voteResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
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
                .uri("/front/transfers/{id}/history", voteResult.getTransfer().getId())
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

        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("50000")
                                                .resourceId(resourceOne.getId())
                                                .deltaUnitId(BYTES)
                                                .build())
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId(String.valueOf(folderOne.getServiceId()))
                                        .providerId(providerModel.getId())
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
        FrontSingleTransferRequestDto voteResult = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers/{id}/_vote?version={version}",
                        result.getTransfer().getId(), result.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestVotingDto.builder()
                        .voteType(TransferRequestVoteTypeDto.ABSTAIN)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(voteResult);
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, voteResult.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                voteResult.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, voteResult.getTransfer().getStatus());
        Assertions.assertEquals(USER_2_ID, voteResult.getTransfer().getCreatedBy());
        Assertions.assertEquals(1, voteResult.getTransfer().getTransferVotes().getVotes().size());
        Assertions.assertEquals(USER_2_ID,
                voteResult.getTransfer().getTransferVotes().getVotes().get(0).getUserId());
        Assertions.assertEquals(TransferRequestVoteTypeDto.ABSTAIN,
                voteResult.getTransfer().getTransferVotes().getVotes().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()),
                new HashSet<>(voteResult.getTransfer().getTransferVotes().getVotes().get(0).getFolderIds()));
        Assertions.assertEquals(Set.of(String.valueOf(FOLDER_SERVICE_ID)),
                new HashSet<>(voteResult.getTransfer().getTransferVotes().getVotes().get(0).getServiceIds()));
        Assertions.assertEquals(1, voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getReserveResponsible().isPresent());
        Assertions.assertEquals(voteResult.getTransfer().getTransferResponsible().getReserveResponsible().get()
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, voteResult.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().size());
        Assertions.assertTrue(voteResult.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().contains(USER_1_ID));
        Assertions.assertEquals(String.valueOf(PROVIDER_SERVICE_ID), voteResult.getTransfer().getTransferResponsible()
                .getReserveResponsible().get().getServiceId());
        Assertions.assertEquals(folderTwo.getId(), voteResult.getTransfer().getTransferResponsible()
                .getReserveResponsible().get().getFolderId());
        Assertions.assertEquals(2, voteResult.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(voteResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(voteResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        Assertions.assertTrue(voteResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        Assertions.assertTrue(voteResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
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
                .uri("/front/transfers/{id}/history", voteResult.getTransfer().getId())
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

        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("50000")
                                                .resourceId(resourceOne.getId())
                                                .deltaUnitId(BYTES)
                                                .build())
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId(String.valueOf(folderOne.getServiceId()))
                                        .providerId(providerModel.getId())
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
        FrontSingleTransferRequestDto voteResultOne = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers/{id}/_vote?version={version}",
                        result.getTransfer().getId(), result.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestVotingDto.builder()
                        .voteType(TransferRequestVoteTypeDto.ABSTAIN)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(voteResultOne);
        FrontSingleTransferRequestDto voteResultTwo = webClient
                .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_UID))
                .post()
                .uri("/front/transfers/{id}/_vote?version={version}",
                        voteResultOne.getTransfer().getId(), voteResultOne.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestVotingDto.builder()
                        .voteType(TransferRequestVoteTypeDto.ABSTAIN)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(voteResultTwo);
        FrontSingleTransferRequestDto voteResultThree = webClient
                .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_2_UID))
                .post()
                .uri("/front/transfers/{id}/_vote?version={version}",
                        voteResultTwo.getTransfer().getId(), voteResultTwo.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestVotingDto.builder()
                        .voteType(TransferRequestVoteTypeDto.ABSTAIN)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(voteResultThree);
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, voteResultThree.getTransfer()
                .getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                voteResultThree.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.REJECTED, voteResultThree.getTransfer().getStatus());
        Assertions.assertEquals(USER_2_ID, voteResultThree.getTransfer().getCreatedBy());
        Assertions.assertEquals(3, voteResultThree.getTransfer().getTransferVotes().getVotes().size());
        Map<String, FrontTransferRequestVoteDto> voteByUser =
                voteResultThree.getTransfer().getTransferVotes().getVotes().stream()
                        .collect(Collectors.toMap(FrontTransferRequestVoteDto::getUserId, Function.identity()));
        FrontTransferRequestVoteDto voteOne = voteByUser.get(USER_2_ID);
        Assertions.assertNotNull(voteOne);
        Assertions.assertEquals(TransferRequestVoteTypeDto.ABSTAIN, voteOne.getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()), new HashSet<>(voteOne.getFolderIds()));
        Assertions.assertEquals(Set.of(String.valueOf(FOLDER_SERVICE_ID)), new HashSet<>(voteOne.getServiceIds()));
        FrontTransferRequestVoteDto voteTwo = voteByUser.get(SERVICE_1_QUOTA_MANAGER);
        Assertions.assertNotNull(voteTwo);
        Assertions.assertEquals(TransferRequestVoteTypeDto.ABSTAIN, voteTwo.getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()), new HashSet<>(voteTwo.getFolderIds()));
        Assertions.assertEquals(Set.of(String.valueOf(TEST_SERVICE_ID_DISPENSER)),
                new HashSet<>(voteTwo.getServiceIds()));
        FrontTransferRequestVoteDto voteThree = voteByUser.get(SERVICE_1_QUOTA_MANAGER_2);
        Assertions.assertNotNull(voteThree);
        Assertions.assertEquals(TransferRequestVoteTypeDto.ABSTAIN, voteThree.getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()), new HashSet<>(voteThree.getFolderIds()));
        Assertions.assertEquals(Set.of(String.valueOf(TEST_SERVICE_ID_DISPENSER)),
                new HashSet<>(voteThree.getServiceIds()));
        Assertions.assertEquals(1, voteResultThree.getTransfer().getTransferResponsible()
                .getResponsibleGroups().size());
        Assertions.assertTrue(voteResultThree.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(voteResultThree.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        Assertions.assertTrue(voteResultThree.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(voteResultThree.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(voteResultThree.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        Assertions.assertTrue(voteResultThree.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(voteResultThree.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(voteResultThree.getTransfer().getTransferResponsible().getReserveResponsible()
                .isPresent());
        Assertions.assertEquals(voteResultThree.getTransfer().getTransferResponsible().getReserveResponsible().get()
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, voteResultThree.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().size());
        Assertions.assertTrue(voteResultThree.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().contains(USER_1_ID));
        Assertions.assertEquals(String.valueOf(PROVIDER_SERVICE_ID), voteResultThree.getTransfer()
                .getTransferResponsible()
                .getReserveResponsible().get().getServiceId());
        Assertions.assertEquals(folderTwo.getId(), voteResultThree.getTransfer().getTransferResponsible()
                .getReserveResponsible().get().getFolderId());
        Assertions.assertEquals(2, voteResultThree.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(voteResultThree.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(voteResultThree.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        Assertions.assertTrue(voteResultThree.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        Assertions.assertTrue(voteResultThree.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
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
                .uri("/front/transfers/{id}/history", voteResultThree.getTransfer().getId())
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

        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("50000")
                                                .resourceId(resourceOne.getId())
                                                .deltaUnitId(BYTES)
                                                .build())
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId(String.valueOf(folderOne.getServiceId()))
                                        .providerId(providerModel.getId())
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
        FrontSingleTransferRequestDto voteResultOne = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/front/transfers/{id}/_vote?version={version}",
                        result.getTransfer().getId(), result.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestVotingDto.builder()
                        .voteType(TransferRequestVoteTypeDto.ABSTAIN)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(voteResultOne);
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, voteResultOne.getTransfer()
                .getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                voteResultOne.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.REJECTED, voteResultOne.getTransfer().getStatus());
        Assertions.assertEquals(USER_2_ID, voteResultOne.getTransfer().getCreatedBy());
        Assertions.assertEquals(1, voteResultOne.getTransfer().getTransferVotes().getVotes().size());
        Map<String, FrontTransferRequestVoteDto> voteByUser =
                voteResultOne.getTransfer().getTransferVotes().getVotes().stream()
                        .collect(Collectors.toMap(FrontTransferRequestVoteDto::getUserId, Function.identity()));
        FrontTransferRequestVoteDto voteOne = voteByUser.get(USER_1_ID);
        Assertions.assertNotNull(voteOne);
        Assertions.assertEquals(TransferRequestVoteTypeDto.ABSTAIN, voteOne.getVoteType());
        Assertions.assertEquals(Set.of(folderTwo.getId()), new HashSet<>(voteOne.getFolderIds()));
        Assertions.assertEquals(Set.of(String.valueOf(PROVIDER_SERVICE_ID)), new HashSet<>(voteOne.getServiceIds()));
        Assertions.assertEquals(1, voteResultOne.getTransfer().getTransferResponsible()
                .getResponsibleGroups().size());
        Assertions.assertTrue(voteResultOne.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(voteResultOne.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        Assertions.assertTrue(voteResultOne.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(voteResultOne.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(voteResultOne.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        Assertions.assertTrue(voteResultOne.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(voteResultOne.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(voteResultOne.getTransfer().getTransferResponsible().getReserveResponsible()
                .isPresent());
        Assertions.assertEquals(voteResultOne.getTransfer().getTransferResponsible().getReserveResponsible().get()
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, voteResultOne.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().size());
        Assertions.assertTrue(voteResultOne.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().contains(USER_1_ID));
        Assertions.assertEquals(String.valueOf(PROVIDER_SERVICE_ID), voteResultOne.getTransfer()
                .getTransferResponsible()
                .getReserveResponsible().get().getServiceId());
        Assertions.assertEquals(folderTwo.getId(), voteResultOne.getTransfer().getTransferResponsible()
                .getReserveResponsible().get().getFolderId());
        Assertions.assertEquals(2, voteResultOne.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(voteResultOne.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(voteResultOne.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        Assertions.assertTrue(voteResultOne.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        Assertions.assertTrue(voteResultOne.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
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
                .uri("/front/transfers/{id}/history", voteResultOne.getTransfer().getId())
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
