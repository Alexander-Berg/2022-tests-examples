package ru.yandex.intranet.d.web.api.transfers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.model.resources.ResourceModel;
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel;
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel;
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel;
import ru.yandex.intranet.d.model.services.ServiceRecipeModel;
import ru.yandex.intranet.d.model.transfers.PendingTransferRequestsModel;
import ru.yandex.intranet.d.model.users.UserModel;
import ru.yandex.intranet.d.services.notifications.NotificationMailSenderStub;
import ru.yandex.intranet.d.util.Long2LongMultimap;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.SortOrderDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestStatusDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestSubtypeDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestVoteTypeDto;
import ru.yandex.intranet.d.web.model.transfers.api.CancelTransferRequestInputDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateReserveResourceTransferDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateReserveTransferDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.api.CreateTransferRequestParametersDto;
import ru.yandex.intranet.d.web.model.transfers.api.TransferRequestDto;

import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_SERVICE_D;
import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestResources.YDB_RAM_SAS;
import static ru.yandex.intranet.d.TestResources.YP_HDD_MAN;
import static ru.yandex.intranet.d.TestResources.YP_HDD_UNMANAGED;
import static ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_D;
import static ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_DISPENSER;
import static ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER_2_UID;
import static ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER_UID;
import static ru.yandex.intranet.d.TestUsers.USER_1_ID;
import static ru.yandex.intranet.d.TestUsers.USER_1_STAFF_ID;
import static ru.yandex.intranet.d.TestUsers.USER_1_UID;
import static ru.yandex.intranet.d.TestUsers.USER_2_ID;
import static ru.yandex.intranet.d.TestUsers.USER_2_STAFF_ID;
import static ru.yandex.intranet.d.TestUsers.USER_2_UID;
import static ru.yandex.intranet.d.TestUsers.USER_3_UID;
import static ru.yandex.intranet.d.model.users.UserServiceRoles.QUOTA_MANAGER;
import static ru.yandex.intranet.d.model.users.UserServiceRoles.RESPONSIBLE_OF_PROVIDER;
import static ru.yandex.intranet.d.web.api.transfers.MorePublicApiTransferRequestsControllerTest.containsError;
import static ru.yandex.intranet.d.web.api.transfers.MorePublicApiTransferRequestsControllerTest.serviceBuilder;
import static ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.folderModel;
import static ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.providerModel;
import static ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.quotaModel;
import static ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.resourceModel;
import static ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.resourceSegmentModel;
import static ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.resourceSegmentationModel;
import static ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.resourceTypeModel;
import static ru.yandex.intranet.d.web.api.transfers.ThirdMorePublicApiTransferRequestsControllerTest.membershipBuilder;
import static ru.yandex.intranet.d.web.api.transfers.ThirdMorePublicApiTransferRequestsControllerTest.reserveFolderModel;

/**
 * Transfer requests public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class SecondMorePublicApiTransferRequestsControllerTest {
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
    public static final long FOLDER_SERVICE_ID = 7765L;
    public static final long PROVIDER_SERVICE_ID = 7766L;

    public SecondMorePublicApiTransferRequestsControllerTest(
            @Value("${abc.roles.quotaManager}") long quotaManagerRoleId,
            @Value("${abc.roles.responsibleOfProvider}") long responsibleOfProvider) {
        this.quotaManagerRoleId = quotaManagerRoleId;
        this.responsibleOfProvide = responsibleOfProvider;
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createTransferRequestFailOnBadResourceTest() {
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
                                                .resourceId(UUID.randomUUID().toString())
                                                .deltaUnitKey("bytes")
                                                .build())
                                        .folderId(TEST_FOLDER_SERVICE_D)
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
                    containsError(errorCollection, "parameters.reserveTransfer.resourceTransfers.0.resourceId",
                            "Resource not found.");
                });
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
                                                .resourceId(YDB_RAM_SAS)
                                                .deltaUnitKey("bytes")
                                                .build())
                                        .folderId(TEST_FOLDER_SERVICE_D)
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
                    containsError(errorCollection, "parameters.reserveTransfer.resourceTransfers.0.resourceId",
                            "Resource belong to other provider.");
                });
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
                                                .resourceId(YP_HDD_UNMANAGED)
                                                .deltaUnitKey("bytes")
                                                .build())
                                        .folderId(TEST_FOLDER_SERVICE_D)
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
                    containsError(errorCollection, "parameters.reserveTransfer.resourceTransfers.0.resourceId",
                            "Resource is not managed.");
                });
    }

    @Test
    public void createTransferRequestFailOnBadUnitTest() {
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
                                                .deltaUnitKey("millicores")
                                                .build())
                                        .folderId(TEST_FOLDER_SERVICE_D)
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
                    containsError(errorCollection, "parameters.reserveTransfer.resourceTransfers.0.deltaUnitKey",
                            "Unit not found.");
                });
    }

    @Test
    public void createTransferRequestFailOnLowBalanceTest() {
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepare();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();

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
                                                .delta(60000L)
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
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    containsError(errorCollection, "parameters.reserveTransfer.resourceTransfers.0.delta",
                            "Not enough balance for quota transfer."
                    + "\nThe balance is 50000 B.");
                    Set<Object> suggestedAmountPromptSet = errorCollection.getDetails().get("suggestedAmountPrompt");
                    Assertions.assertNotNull(suggestedAmountPromptSet);
                    Assertions.assertTrue(suggestedAmountPromptSet.contains("Transfer all quota"));
                });
    }

    @Test
    public void createTransferRequestFailOnMaxLongBalanceTest() {
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepare();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();

        QuotaModel quotaModel = quotaModel(providerModel.getId(), resourceOne.getId(), folderOne.getId(),
                Long.MAX_VALUE, Long.MAX_VALUE);

        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .upsertAllRetryable(txSession, List.of(quotaModel))))
                .block();

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
                                                .delta(60000L)
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
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    containsError(errorCollection, "parameters.reserveTransfer.resourceTransfers.0.delta",
                            "Number is out of accepted range.");
                });
    }

    @Test
    public void createTransferRequestFailOnWithoutQuorumTest() {
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepare();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();

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
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getErrors());
                    Assertions.assertTrue(errorCollection.getErrors()
                            .contains("Can not collect quorum for transfer request confirmation."));
                });
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createReserveTransferRequestTest() {
        long initialMailCounter = mailSender.getCounter();
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
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, result.getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER, result.getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, result.getStatus());
        Assertions.assertEquals(USER_2_UID, result.getCreatedBy());
        Assertions.assertTrue(result.getVotes().getVoters().isEmpty());
        Assertions.assertEquals(1, result.getTransferResponsible().getGrouped().size());
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId())));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID)
                                && r.getServiceIds().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(result.getTransferResponsible().getProviderReserveResponsible().isEmpty());
        Assertions.assertEquals(result.getTransferResponsible().getProviderReserveResponsible().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, result.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().size());
        Assertions.assertTrue(result.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().contains(USER_1_UID));
        Assertions.assertEquals(PROVIDER_SERVICE_ID, result.getTransferResponsible()
                .getProviderReserveResponsible().get(0).getServiceId());
        Assertions.assertEquals(folderTwo.getId(), result.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getFolderId());
        Assertions.assertFalse(result.getParameters().getReserveTransfer().isEmpty());
        Assertions.assertEquals(folderOne.getId(), result.getParameters().getReserveTransfer().get().getFolderId());
        Assertions.assertTrue(result.getParameters().getReserveTransfer().get().getResourceTransfers()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta() == 50000L
                        && r.getDeltaUnitKey().equals("bytes")));
        long updatedMailCounter = mailSender.getCounter();
        Assertions.assertTrue(updatedMailCounter - initialMailCounter > 0);
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createAndConfirmByProviderResponsibleReserveTransferRequestTest() {
        long initialMailCounter = mailSender.getCounter();
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepareWithRoles();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();
        FolderModel folderTwo = prepare.getT6();

        TransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(CreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(true)
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
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, result.getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER, result.getRequestSubtype());
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
        Assertions.assertEquals(USER_1_UID, result.getCreatedBy());
        Assertions.assertEquals(1, result.getVotes().getVoters().size());
        Assertions.assertEquals(USER_1_UID,
                result.getVotes().getVoters().get(0).getVoter());
        Assertions.assertEquals(TransferRequestVoteTypeDto.CONFIRM,
                result.getVotes().getVoters().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId(), folderTwo.getId()),
                new HashSet<>(result.getVotes().getVoters().get(0).getFolderIds()));
        Assertions.assertTrue(result.getVotes().getVoters().get(0).getServiceIds().isEmpty());
        Assertions.assertEquals(0, result.getTransferResponsible().getGrouped().size());
        Assertions.assertTrue(result.getTransferResponsible().getProviderReserveResponsible().isEmpty());
        Assertions.assertEquals(1, result.getTransferResponsible().getProviderSuperResponsible().size());
        Assertions.assertEquals(USER_1_UID, result.getTransferResponsible().getProviderSuperResponsible()
                .get(0).getResponsible());
        Assertions.assertEquals(1, result.getTransferResponsible().getProviderSuperResponsible()
                .get(0).getProviderIds().size());
        Assertions.assertTrue(result.getTransferResponsible().getProviderSuperResponsible()
                .get(0).getProviderIds().contains(providerModel.getId()));
        Assertions.assertFalse(result.getParameters().getReserveTransfer().isEmpty());
        Assertions.assertEquals(folderOne.getId(), result.getParameters().getReserveTransfer().get().getFolderId());
        Assertions.assertTrue(result.getParameters().getReserveTransfer().get().getResourceTransfers()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta() == 50000L
                        && r.getDeltaUnitKey().equals("bytes")));
        List<QuotaModel> block = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderOne.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(block);
        QuotaModel quotasOne = block.stream().filter(q -> q.getResourceId().equals(resourceOne.getId())).findFirst()
                .orElseThrow();
        List<QuotaModel> block1 = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderTwo.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(block1);
        QuotaModel quotasTwo = block1.stream().filter(q -> q.getResourceId().equals(resourceOne.getId())).findFirst()
                .orElseThrow();
        Assertions.assertEquals(60000, quotasOne.getQuota());
        Assertions.assertEquals(60000, quotasOne.getBalance());
        Assertions.assertEquals(0, quotasTwo.getQuota());
        Assertions.assertEquals(0, quotasTwo.getBalance());
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
        long updatedMailCounter = mailSender.getCounter();
        Assertions.assertEquals(0L, updatedMailCounter - initialMailCounter);
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createAndConfirmReserveTransferRequestTest() {
        long initialMailCounter = mailSender.getCounter();
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
                        .addConfirmation(true)
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
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, result.getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER, result.getRequestSubtype());
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
        Assertions.assertEquals(USER_2_UID, result.getCreatedBy());
        Assertions.assertEquals(1, result.getVotes().getVoters().size());
        Assertions.assertEquals(USER_2_UID,
                result.getVotes().getVoters().get(0).getVoter());
        Assertions.assertEquals(TransferRequestVoteTypeDto.CONFIRM,
                result.getVotes().getVoters().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()),
                new HashSet<>(result.getVotes().getVoters().get(0).getFolderIds()));
        Assertions.assertEquals(Set.of(FOLDER_SERVICE_ID),
                new HashSet<>(result.getVotes().getVoters().get(0).getServiceIds()));
        Assertions.assertEquals(1, result.getTransferResponsible().getGrouped().size());
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId())));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID)
                                && r.getServiceIds().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(result.getTransferResponsible().getProviderReserveResponsible().isEmpty());
        Assertions.assertEquals(providerModel.getId(), result.getTransferResponsible()
                .getProviderReserveResponsible().get(0).getProviderId());
        Assertions.assertEquals(1, result.getTransferResponsible().getProviderReserveResponsible().get(0)
                .getResponsibleUsers().size());
        Assertions.assertTrue(result.getTransferResponsible().getProviderReserveResponsible().get(0)
                .getResponsibleUsers().contains(USER_1_UID));
        Assertions.assertFalse(result.getParameters().getReserveTransfer().isEmpty());
        Assertions.assertEquals(folderOne.getId(), result.getParameters().getReserveTransfer().get().getFolderId());
        Assertions.assertTrue(result.getParameters().getReserveTransfer().get().getResourceTransfers()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta() == 50000L
                        && r.getDeltaUnitKey().equals("bytes")));
        long updatedMailCounter = mailSender.getCounter();
        Assertions.assertTrue(updatedMailCounter - initialMailCounter > 0);
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createAndConfirmWithoutRightsReserveTransferRequestTest() {
        long initialMailCounter = mailSender.getCounter();
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepareWithRoles();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();
        FolderModel folderTwo = prepare.getT6();
        TransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_3_UID))
                .post()
                .uri("/api/v1/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(CreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(true)
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
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, result.getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER, result.getRequestSubtype());
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
        Assertions.assertEquals(USER_3_UID, result.getCreatedBy());
        Assertions.assertTrue(result.getVotes().getVoters().isEmpty());
        Assertions.assertEquals(1, result.getTransferResponsible().getGrouped().size());
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId())));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID)
                                && r.getServiceIds().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(result.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(result.getTransferResponsible().getProviderReserveResponsible().isEmpty());
        Assertions.assertEquals(result.getTransferResponsible().getProviderReserveResponsible().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, result.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().size());
        Assertions.assertTrue(result.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().contains(USER_1_UID));
        Assertions.assertFalse(result.getParameters().getReserveTransfer().isEmpty());
        Assertions.assertEquals(folderOne.getId(), result.getParameters().getReserveTransfer().get().getFolderId());
        Assertions.assertTrue(result.getParameters().getReserveTransfer().get().getResourceTransfers()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta() == 50000L
                        && r.getDeltaUnitKey().equals("bytes")));
        long updatedMailCounter = mailSender.getCounter();
        Assertions.assertTrue(updatedMailCounter - initialMailCounter > 0);
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void getReserveTransferRequestTest() {
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
        TransferRequestDto getResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .get()
                .uri("/api/v1/transfers/{id}", result.getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(getResult);
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, getResult.getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER, result.getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, getResult.getStatus());
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
        Assertions.assertEquals(USER_2_UID, getResult.getCreatedBy());
        Assertions.assertTrue(getResult.getVotes().getVoters().isEmpty());
        Assertions.assertEquals(1, getResult.getTransferResponsible().getGrouped().size());
        Assertions.assertTrue(getResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId())));
        Assertions.assertTrue(getResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID))));
        Assertions.assertTrue(getResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(getResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(getResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID)
                                && r.getServiceIds().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(getResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(getResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(getResult.getTransferResponsible().getProviderReserveResponsible().isEmpty());
        Assertions.assertEquals(getResult.getTransferResponsible().getProviderReserveResponsible().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, getResult.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().size());
        Assertions.assertTrue(getResult.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().contains(USER_1_UID));
        Assertions.assertFalse(getResult.getParameters().getReserveTransfer().isEmpty());
        Assertions.assertEquals(folderOne.getId(), getResult.getParameters().getReserveTransfer().get().getFolderId());
        Assertions.assertTrue(getResult.getParameters().getReserveTransfer().get().getResourceTransfers()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta() == 50000L
                        && r.getDeltaUnitKey().equals("bytes")));
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void cancelReserveTransferRequestTest() {
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
        TransferRequestDto cancelResult = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/api/v1/transfers/{id}/_cancel", result.getId())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new CancelTransferRequestInputDto(result.getVersion()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(cancelResult);
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, cancelResult.getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER, result.getRequestSubtype());
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
        Assertions.assertEquals(USER_2_UID, cancelResult.getCreatedBy());
        Assertions.assertTrue(cancelResult.getVotes().getVoters().isEmpty());
        Assertions.assertEquals(1, cancelResult.getTransferResponsible().getGrouped().size());
        Assertions.assertTrue(cancelResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId())));
        Assertions.assertTrue(cancelResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID))));
        Assertions.assertTrue(cancelResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(cancelResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(cancelResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(USER_2_UID)
                                && r.getServiceIds().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(cancelResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(cancelResult.getTransferResponsible().getGrouped().stream()
                .anyMatch(g -> g.getFolderIds().contains(folderOne.getId()) && g.getResponsibleSet().stream()
                        .anyMatch(r -> r.getResponsible().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIds().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(cancelResult.getTransferResponsible().getProviderReserveResponsible().isEmpty());
        Assertions.assertEquals(cancelResult.getTransferResponsible().getProviderReserveResponsible().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, cancelResult.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().size());
        Assertions.assertTrue(cancelResult.getTransferResponsible().getProviderReserveResponsible()
                .get(0).getResponsibleUsers().contains(USER_1_UID));
        Assertions.assertFalse(cancelResult.getParameters().getReserveTransfer().isEmpty());
        Assertions.assertEquals(folderOne.getId(),
                cancelResult.getParameters().getReserveTransfer().get().getFolderId());
        Assertions.assertTrue(cancelResult.getParameters().getReserveTransfer().get().getResourceTransfers()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta() == 50000L
                        && r.getDeltaUnitKey().equals("bytes")));
    }

    private Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel,
            QuotaModel, QuotaModel> prepareWithRoles() {
        return prepareWithRoles(tableClient, servicesDao, abcServiceMemberDao, responsibleOfProvide, quotaManagerRoleId,
                usersDao, providersDao, resourceTypesDao, resourceSegmentationsDao, resourceSegmentsDao, resourcesDao,
                folderDao, quotasDao);
    }

    @SuppressWarnings("ParameterNumber")
    static Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel,
            QuotaModel, QuotaModel> prepareWithRoles(
            YdbTableClient tableClient, ServicesDao servicesDao, AbcServiceMemberDao abcServiceMemberDao,
            long responsibleOfProvide, long quotaManagerRoleId, UsersDao usersDao, ProvidersDao providersDao,
            ResourceTypesDao resourceTypesDao, ResourceSegmentationsDao resourceSegmentationsDao,
            ResourceSegmentsDao resourceSegmentsDao, ResourcesDao resourcesDao, FolderDao folderDao,
            QuotasDao quotasDao) {
        ServiceRecipeModel providerService = serviceBuilder().id(PROVIDER_SERVICE_ID).name("Provider service")
                .nameEn("Provider service").slug("provider_service").parentId(TEST_SERVICE_ID_D).build();
        ServiceRecipeModel folderService = serviceBuilder().id(FOLDER_SERVICE_ID).name("Folder service")
                .nameEn("Folder service").slug("folder_service").parentId(TEST_SERVICE_ID_DISPENSER).build();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> servicesDao.upsertRecipeRetryable(txSession,
                        providerService)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> servicesDao.upsertRecipeRetryable(txSession,
                        folderService)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> servicesDao.upsertAllParentsRetryable(txSession,
                        new Long2LongMultimap().put(FOLDER_SERVICE_ID, TEST_SERVICE_ID_DISPENSER)
                                .put(PROVIDER_SERVICE_ID, TEST_SERVICE_ID_D), Tenants.DEFAULT_TENANT_ID)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> abcServiceMemberDao.upsertManyRetryable(txSession,
                                List.of(membershipBuilder(Long.MAX_VALUE - 1, PROVIDER_SERVICE_ID, USER_1_STAFF_ID,
                                responsibleOfProvide),
                        membershipBuilder(Long.MAX_VALUE - 2, FOLDER_SERVICE_ID, USER_2_STAFF_ID,
                                quotaManagerRoleId)))))
                .block();
        List<UserModel> userModels = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> usersDao.getByIds(txSession,
                        List.of(Tuples.of(USER_1_ID, Tenants.DEFAULT_TENANT_ID),
                                Tuples.of(USER_2_ID, Tenants.DEFAULT_TENANT_ID)))))
                .block();
        Assertions.assertNotNull(userModels);
        Map<String, UserModel> usersByIdMap = userModels.stream()
                .collect(Collectors.toMap(UserModel::getId, Function.identity()));
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> usersDao.updateUsersRetryable(txSession,
                        List.of(usersByIdMap.get(USER_1_ID).copyBuilder().roles(
                                Map.of(RESPONSIBLE_OF_PROVIDER, Set.of(PROVIDER_SERVICE_ID))).build(),
                                usersByIdMap.get(USER_2_ID).copyBuilder().roles(
                                        Map.of(QUOTA_MANAGER, Set.of(FOLDER_SERVICE_ID))
                                ).build()))))
                .block();
        return prepare(PROVIDER_SERVICE_ID, FOLDER_SERVICE_ID, tableClient, providersDao, resourceTypesDao,
                resourceSegmentationsDao, resourceSegmentsDao, resourcesDao, folderDao, quotasDao);
    }

    private Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel,
            QuotaModel, QuotaModel> prepare() {
        return prepare(12L, 1L, tableClient, providersDao, resourceTypesDao,
                resourceSegmentationsDao, resourceSegmentsDao, resourcesDao, folderDao, quotasDao);
    }

    @SuppressWarnings("ParameterNumber")
    static Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel,
            QuotaModel, QuotaModel> prepare(
            long providerServiceId, long folderServiceId, YdbTableClient tableClient, ProvidersDao providersDao,
            ResourceTypesDao resourceTypesDao, ResourceSegmentationsDao resourceSegmentationsDao,
            ResourceSegmentsDao resourceSegmentsDao, ResourcesDao resourcesDao, FolderDao folderDao,
            QuotasDao quotasDao) {
        ProviderModel provider = providerModel("in-process:test", null, false, providerServiceId);
        ResourceTypeModel resourceType = resourceTypeModel(provider.getId(), "test",
                "b02344bf-96af-4cc5-937c-66a479989ce8");
        ResourceSegmentationModel locationSegmentation = resourceSegmentationModel(provider.getId(), "location");
        ResourceSegmentModel vlaSegment = resourceSegmentModel(locationSegmentation.getId(), "VLA");
        ResourceModel resourceOne = resourceModel(provider.getId(), "test", resourceType.getId(),
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())),
                "b02344bf-96af-4cc5-937c-66a479989ce8",
                Set.of("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                        "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", null);
        ResourceModel resourceTwo = resourceModel(provider.getId(), "test", resourceType.getId(),
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())),
                "b02344bf-96af-4cc5-937c-66a479989ce8",
                Set.of("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                        "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", null);
        FolderModel folderOne = folderModel(folderServiceId);
        FolderModel folderTwo = reserveFolderModel(providerServiceId);
        QuotaModel quotaOne = quotaModel(provider.getId(), resourceOne.getId(), folderTwo.getId(), 50000, 50000);
        QuotaModel quotaTwo = quotaModel(provider.getId(), resourceTwo.getId(), folderTwo.getId(), 150000, 150000);
        QuotaModel quotaThree = quotaModel(provider.getId(), resourceOne.getId(), folderOne.getId(), 10000, 10000);
        QuotaModel quotaFour = quotaModel(provider.getId(), resourceTwo.getId(), folderOne.getId(), 500, 500);
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
                        .upsertResourceRetryable(txSession, resourceOne)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourcesDao
                        .upsertResourceRetryable(txSession, resourceTwo)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertAllRetryable(txSession, List.of(folderOne, folderTwo))))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .upsertAllRetryable(txSession, List.of(quotaOne, quotaTwo, quotaThree, quotaFour))))
                .block();
        return Tuples.of(provider, resourceType, resourceOne, resourceTwo, folderOne, folderTwo, quotaOne, quotaTwo);
    }
}
