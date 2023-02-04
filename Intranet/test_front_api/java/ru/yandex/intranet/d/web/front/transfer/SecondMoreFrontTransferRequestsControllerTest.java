package ru.yandex.intranet.d.web.front.transfer;

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
import ru.yandex.intranet.d.web.model.transfers.TransferRequestHistoryEventTypeDto;
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

import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_SERVICE_D;
import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestResources.YDB_RAM_SAS;
import static ru.yandex.intranet.d.TestResources.YP_HDD_MAN;
import static ru.yandex.intranet.d.TestResources.YP_HDD_UNMANAGED;
import static ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_D;
import static ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_DISPENSER;
import static ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER;
import static ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER_2;
import static ru.yandex.intranet.d.TestUsers.USER_1_ID;
import static ru.yandex.intranet.d.TestUsers.USER_1_STAFF_ID;
import static ru.yandex.intranet.d.TestUsers.USER_1_UID;
import static ru.yandex.intranet.d.TestUsers.USER_2_ID;
import static ru.yandex.intranet.d.TestUsers.USER_2_STAFF_ID;
import static ru.yandex.intranet.d.TestUsers.USER_2_UID;
import static ru.yandex.intranet.d.TestUsers.USER_3_ID;
import static ru.yandex.intranet.d.TestUsers.USER_3_UID;
import static ru.yandex.intranet.d.UnitIds.BYTES;
import static ru.yandex.intranet.d.UnitIds.KILOBYTES;
import static ru.yandex.intranet.d.UnitIds.MILLICORES;
import static ru.yandex.intranet.d.model.users.UserServiceRoles.QUOTA_MANAGER;
import static ru.yandex.intranet.d.model.users.UserServiceRoles.RESPONSIBLE_OF_PROVIDER;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.folderModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.providerModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.quotaModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.resourceModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.resourceSegmentModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.resourceSegmentationModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.resourceTypeModel;
import static ru.yandex.intranet.d.web.front.transfer.MoreFrontTransferRequestsControllerTest.containsError;
import static ru.yandex.intranet.d.web.front.transfer.MoreFrontTransferRequestsControllerTest.serviceBuilder;
import static ru.yandex.intranet.d.web.front.transfer.ThirdMoreFrontTransferRequestsControllerTest.membershipBuilder;
import static ru.yandex.intranet.d.web.front.transfer.ThirdMoreFrontTransferRequestsControllerTest.reserveFolderModel;

/**
 * Front transfer requests API test.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@IntegrationTest
public class SecondMoreFrontTransferRequestsControllerTest {
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

    public SecondMoreFrontTransferRequestsControllerTest(
            @Value("${abc.roles.quotaManager}") long quotaManagerRoleId,
            @Value("${abc.roles.responsibleOfProvider}") long responsibleOfProvider) {
        this.quotaManagerRoleId = quotaManagerRoleId;
        this.responsibleOfProvide = responsibleOfProvider;
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createTransferRequestFailOnBadFolderTest() {
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("100")
                                                .resourceId(YP_HDD_MAN)
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .destinationFolderId(UUID.randomUUID().toString())
                                        .destinationServiceId(String.valueOf(TEST_SERVICE_ID_D))
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
                    containsError(errorCollection, "parameters.reserveTransfer.destinationFolderId",
                            "Folder not found.");
                });
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("100")
                                                .resourceId(YP_HDD_MAN)
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .destinationFolderId(TEST_FOLDER_1_ID)
                                        .destinationServiceId(String.valueOf(TEST_SERVICE_ID_D))
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
                    containsError(errorCollection, "parameters.reserveTransfer.destinationFolderId",
                            "Folder and service does not match.");
                });
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createTransferRequestFailOnBadResourceTest() {
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("100")
                                                .resourceId(UUID.randomUUID().toString())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .destinationFolderId(TEST_FOLDER_SERVICE_D)
                                        .destinationServiceId(String.valueOf(TEST_SERVICE_ID_D))
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
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("100")
                                                .resourceId(YDB_RAM_SAS)
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .destinationFolderId(TEST_FOLDER_SERVICE_D)
                                        .destinationServiceId(String.valueOf(TEST_SERVICE_ID_D))
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
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("100")
                                                .resourceId(YP_HDD_UNMANAGED)
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .destinationFolderId(TEST_FOLDER_SERVICE_D)
                                        .destinationServiceId(String.valueOf(TEST_SERVICE_ID_D))
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
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("100")
                                                .resourceId(YP_HDD_MAN)
                                                .deltaUnitId(MILLICORES)
                                                .build())
                                        .destinationFolderId(TEST_FOLDER_SERVICE_D)
                                        .destinationServiceId(String.valueOf(TEST_SERVICE_ID_D))
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
                    containsError(errorCollection, "parameters.reserveTransfer.resourceTransfers.0.deltaUnitId",
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
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("60000")
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
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("60000")
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
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, result.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                result.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, result.getTransfer().getStatus());
        Assertions.assertEquals(USER_2_ID, result.getTransfer().getCreatedBy());
        Assertions.assertTrue(result.getTransfer().getTransferVotes().getVotes().isEmpty());
        Assertions.assertEquals(1, result.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getReserveResponsible().isPresent());
        Assertions.assertEquals(result.getTransfer().getTransferResponsible().getReserveResponsible().get()
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, result.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().size());
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().contains(USER_1_ID));
        Assertions.assertEquals(String.valueOf(PROVIDER_SERVICE_ID), result.getTransfer().getTransferResponsible()
                .getReserveResponsible().get().getServiceId());
        Assertions.assertEquals(folderTwo.getId(), result.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getFolderId());
        Assertions.assertEquals(2, result.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
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

        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(true)
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
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, result.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                result.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.APPLIED, result.getTransfer().getStatus());
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> pendingTransferRequestsDao.getById(txSession,
                                result.getTransfer().getId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Assertions.assertNotNull(pendingTransferRequest);
        Assertions.assertEquals(
                Optional.empty(),
                pendingTransferRequest.map(PendingTransferRequestsModel::getRequestId));
        Assertions.assertEquals(USER_1_ID, result.getTransfer().getCreatedBy());
        Assertions.assertEquals(1, result.getTransfer().getTransferVotes().getVotes().size());
        Assertions.assertEquals(USER_1_ID,
                result.getTransfer().getTransferVotes().getVotes().get(0).getUserId());
        Assertions.assertEquals(TransferRequestVoteTypeDto.CONFIRM,
                result.getTransfer().getTransferVotes().getVotes().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId(), folderTwo.getId()),
                new HashSet<>(result.getTransfer().getTransferVotes().getVotes().get(0).getFolderIds()));
        Assertions.assertTrue(result.getTransfer().getTransferVotes().getVotes().get(0).getServiceIds().isEmpty());
        Assertions.assertEquals(0, result.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getReserveResponsible().isEmpty());
        Assertions.assertEquals(1, result.getTransfer().getTransferResponsible().getProviderResponsible().size());
        Assertions.assertEquals(USER_1_ID, result.getTransfer().getTransferResponsible().getProviderResponsible()
                .get(0).getResponsibleId());
        Assertions.assertEquals(1, result.getTransfer().getTransferResponsible().getProviderResponsible()
                .get(0).getProviderIds().size());
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getProviderResponsible()
                .get(0).getProviderIds().contains(providerModel.getId()));
        Assertions.assertEquals(2, result.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        List<QuotaModel> block = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderOne.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(block);
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
        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(true)
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
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, result.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                result.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, result.getTransfer().getStatus());
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> pendingTransferRequestsDao.getById(txSession,
                                result.getTransfer().getId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Assertions.assertNotNull(pendingTransferRequest);
        Assertions.assertEquals(
                Optional.of(result.getTransfer().getId()),
                pendingTransferRequest.map(PendingTransferRequestsModel::getRequestId));
        Assertions.assertEquals(USER_2_ID, result.getTransfer().getCreatedBy());
        Assertions.assertEquals(1, result.getTransfer().getTransferVotes().getVotes().size());
        Assertions.assertEquals(USER_2_ID,
                result.getTransfer().getTransferVotes().getVotes().get(0).getUserId());
        Assertions.assertEquals(TransferRequestVoteTypeDto.CONFIRM,
                result.getTransfer().getTransferVotes().getVotes().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()),
                new HashSet<>(result.getTransfer().getTransferVotes().getVotes().get(0).getFolderIds()));
        Assertions.assertEquals(Set.of(String.valueOf(FOLDER_SERVICE_ID)),
                new HashSet<>(result.getTransfer().getTransferVotes().getVotes().get(0).getServiceIds()));
        Assertions.assertEquals(1, result.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getReserveResponsible().isPresent());
        Assertions.assertEquals(providerModel.getId(), result.getTransfer().getTransferResponsible()
                .getReserveResponsible().get().getProviderId());
        Assertions.assertEquals(1, result.getTransfer().getTransferResponsible().getReserveResponsible().get()
                .getResponsibleIds().size());
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getReserveResponsible().get()
                .getResponsibleIds().contains(USER_1_ID));
        Assertions.assertEquals(2, result.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
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
        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_3_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(true)
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
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, result.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                result.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, result.getTransfer().getStatus());
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> pendingTransferRequestsDao.getById(txSession,
                                result.getTransfer().getId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Assertions.assertNotNull(pendingTransferRequest);
        Assertions.assertEquals(
                Optional.of(result.getTransfer().getId()),
                pendingTransferRequest.map(PendingTransferRequestsModel::getRequestId));
        Assertions.assertEquals(USER_3_ID, result.getTransfer().getCreatedBy());
        Assertions.assertTrue(result.getTransfer().getTransferVotes().getVotes().isEmpty());
        Assertions.assertEquals(1, result.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getReserveResponsible().isPresent());
        Assertions.assertEquals(result.getTransfer().getTransferResponsible().getReserveResponsible().get()
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, result.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().size());
        Assertions.assertTrue(result.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().contains(USER_1_ID));
        Assertions.assertEquals(2, result.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        Assertions.assertTrue(result.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        long updatedMailCounter = mailSender.getCounter();
        Assertions.assertTrue(updatedMailCounter - initialMailCounter > 0);
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void putTransferReserveRequestTest() {
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
        FrontSingleTransferRequestDto putResult = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .put()
                .uri("/front/transfers/{id}?version={version}", result.getTransfer().getId(),
                        result.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(false)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("40000")
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
        Assertions.assertNotNull(putResult);
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, putResult.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                putResult.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, putResult.getTransfer().getStatus());
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> pendingTransferRequestsDao.getById(txSession,
                                result.getTransfer().getId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Assertions.assertNotNull(pendingTransferRequest);
        Assertions.assertEquals(
                Optional.of(result.getTransfer().getId()),
                pendingTransferRequest.map(PendingTransferRequestsModel::getRequestId));
        Assertions.assertEquals(USER_2_ID, putResult.getTransfer().getCreatedBy());
        Assertions.assertTrue(putResult.getTransfer().getTransferVotes().getVotes().isEmpty());
        Assertions.assertEquals(1, putResult.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(putResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertEquals(2, putResult.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(putResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(putResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        Assertions.assertTrue(putResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("40") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        Assertions.assertTrue(putResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-40") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void putAndConfirmWithoutRightsReserveTransferRequestTest() {
        long initialMailCounter = mailSender.getCounter();
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepareWithRoles();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();
        FolderModel folderTwo = prepare.getT6();
        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(USER_3_UID))
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
        FrontSingleTransferRequestDto put = webClient
                .mutateWith(MockUser.uid(USER_3_UID))
                .put()
                .uri("/front/transfers/{id}?version={version}", result.getTransfer().getId(),
                        result.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(true)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("40000")
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
        Assertions.assertNotNull(put);
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, put.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                put.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, put.getTransfer().getStatus());
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> pendingTransferRequestsDao.getById(txSession,
                                result.getTransfer().getId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Assertions.assertNotNull(pendingTransferRequest);
        Assertions.assertEquals(
                Optional.of(result.getTransfer().getId()),
                pendingTransferRequest.map(PendingTransferRequestsModel::getRequestId));
        Assertions.assertEquals(USER_3_ID, put.getTransfer().getCreatedBy());
        Assertions.assertTrue(put.getTransfer().getTransferVotes().getVotes().isEmpty());
        Assertions.assertEquals(1, put.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(put.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(put.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        Assertions.assertTrue(put.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(put.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(put.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        Assertions.assertTrue(put.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(put.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(put.getTransfer().getTransferResponsible().getReserveResponsible().isPresent());
        Assertions.assertEquals(put.getTransfer().getTransferResponsible().getReserveResponsible().get()
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, put.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().size());
        Assertions.assertTrue(put.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().contains(USER_1_ID));
        Assertions.assertEquals(2, put.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(put.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(put.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        Assertions.assertTrue(put.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("40") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        Assertions.assertTrue(put.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-40") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        long updatedMailCounter = mailSender.getCounter();
        Assertions.assertTrue(updatedMailCounter - initialMailCounter > 0);
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void putAndConfirmReserveTransferRequestTest() {
        long initialMailCounter = mailSender.getCounter();
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
        FrontSingleTransferRequestDto put = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .put()
                .uri("/front/transfers/{id}?version={version}", result.getTransfer().getId(),
                        result.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontCreateTransferRequestDto.builder()
                        .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                        .addConfirmation(true)
                        .parameters(FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("40000")
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
        Assertions.assertNotNull(put);
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, put.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                put.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, put.getTransfer().getStatus());
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> pendingTransferRequestsDao.getById(txSession,
                                result.getTransfer().getId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Assertions.assertNotNull(pendingTransferRequest);
        Assertions.assertEquals(
                Optional.of(result.getTransfer().getId()),
                pendingTransferRequest.map(PendingTransferRequestsModel::getRequestId));
        Assertions.assertEquals(USER_2_ID, put.getTransfer().getCreatedBy());
        Assertions.assertEquals(1, put.getTransfer().getTransferVotes().getVotes().size());
        Assertions.assertEquals(USER_2_ID,
                put.getTransfer().getTransferVotes().getVotes().get(0).getUserId());
        Assertions.assertEquals(TransferRequestVoteTypeDto.CONFIRM,
                put.getTransfer().getTransferVotes().getVotes().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()),
                new HashSet<>(put.getTransfer().getTransferVotes().getVotes().get(0).getFolderIds()));
        Assertions.assertEquals(Set.of(String.valueOf(FOLDER_SERVICE_ID)),
                new HashSet<>(put.getTransfer().getTransferVotes().getVotes().get(0).getServiceIds()));
        Assertions.assertEquals(1, put.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(put.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(put.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        Assertions.assertTrue(put.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(put.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(put.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        Assertions.assertTrue(put.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(put.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(put.getTransfer().getTransferResponsible().getReserveResponsible().isPresent());
        Assertions.assertEquals(providerModel.getId(), put.getTransfer().getTransferResponsible()
                .getReserveResponsible().get().getProviderId());
        Assertions.assertEquals(1, put.getTransfer().getTransferResponsible().getReserveResponsible().get()
                .getResponsibleIds().size());
        Assertions.assertTrue(put.getTransfer().getTransferResponsible().getReserveResponsible().get()
                .getResponsibleIds().contains(USER_1_ID));
        Assertions.assertEquals(2, put.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(put.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(put.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        Assertions.assertTrue(put.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("40") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        Assertions.assertTrue(put.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-40") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
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
        FrontSingleTransferRequestDto getResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .get()
                .uri("/front/transfers/{id}", result.getTransfer().getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(getResult);
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, getResult.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                getResult.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, getResult.getTransfer().getStatus());
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> pendingTransferRequestsDao.getById(txSession,
                                result.getTransfer().getId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Assertions.assertNotNull(pendingTransferRequest);
        Assertions.assertEquals(
                Optional.of(result.getTransfer().getId()),
                pendingTransferRequest.map(PendingTransferRequestsModel::getRequestId));
        Assertions.assertEquals(USER_2_ID, getResult.getTransfer().getCreatedBy());
        Assertions.assertTrue(getResult.getTransfer().getTransferVotes().getVotes().isEmpty());
        Assertions.assertEquals(1, getResult.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(getResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(getResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        Assertions.assertTrue(getResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(getResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(getResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        Assertions.assertTrue(getResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(getResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(getResult.getTransfer().getTransferResponsible().getReserveResponsible().isPresent());
        Assertions.assertEquals(getResult.getTransfer().getTransferResponsible().getReserveResponsible().get()
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, getResult.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().size());
        Assertions.assertTrue(getResult.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().contains(USER_1_ID));
        Assertions.assertEquals(2, getResult.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(getResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(getResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        Assertions.assertTrue(getResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        Assertions.assertTrue(getResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
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
        FrontSingleTransferRequestDto cancelResult = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers/{id}/_cancel?version={version}", result.getTransfer().getId(),
                        result.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(cancelResult);
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, cancelResult.getTransfer().getRequestType());
        Assertions.assertEquals(TransferRequestSubtypeDto.DEFAULT_RESERVE_TRANSFER,
                cancelResult.getTransfer().getRequestSubtype());
        Assertions.assertEquals(TransferRequestStatusDto.CANCELLED, cancelResult.getTransfer().getStatus());
        Optional<PendingTransferRequestsModel> pendingTransferRequest =
                tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> pendingTransferRequestsDao.getById(txSession,
                                result.getTransfer().getId(), Tenants.DEFAULT_TENANT_ID)))
                        .block();
        Assertions.assertNotNull(pendingTransferRequest);
        Assertions.assertEquals(
                Optional.empty(),
                pendingTransferRequest.map(PendingTransferRequestsModel::getRequestId));
        Assertions.assertEquals(USER_2_ID, cancelResult.getTransfer().getCreatedBy());
        Assertions.assertTrue(cancelResult.getTransfer().getTransferVotes().getVotes().isEmpty());
        Assertions.assertEquals(1, cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().size());
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getReserveResponsible().isPresent());
        Assertions.assertEquals(cancelResult.getTransfer().getTransferResponsible().getReserveResponsible().get()
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, cancelResult.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().size());
        Assertions.assertTrue(cancelResult.getTransfer().getTransferResponsible().getReserveResponsible()
                .get().getResponsibleIds().contains(USER_1_ID));
        Assertions.assertEquals(2, cancelResult.getTransfer().getParameters().getQuotaTransfers().size());
        Assertions.assertTrue(cancelResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(cancelResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        Assertions.assertTrue(cancelResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        Assertions.assertTrue(cancelResult.getTransfer().getParameters().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void getReserveTransferRequestHistoryTest() {
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
        FrontTransferRequestHistoryEventsPageDto historyResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .get()
                .uri("/front/transfers/{id}/history", result.getTransfer().getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontTransferRequestHistoryEventsPageDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(historyResult);
        Assertions.assertEquals(1, historyResult.getEvents().size());
        Assertions.assertTrue(historyResult.getEvents().get(0).getAuthorId().isPresent());
        Assertions.assertEquals(USER_2_ID,
                historyResult.getEvents().get(0).getAuthorId().get());
        Assertions.assertEquals(TransferRequestHistoryEventTypeDto.CREATED,
                historyResult.getEvents().get(0).getEventType());
        Assertions.assertTrue(historyResult.getEvents().get(0).getNewFields().isPresent());
        Assertions.assertTrue(historyResult.getEvents().get(0).getNewFields().get().getStatus().isPresent());
        Assertions.assertTrue(historyResult.getEvents().get(0).getNewFields().get().getType().isPresent());
        Assertions.assertEquals(TransferRequestStatusDto.PENDING,
                historyResult.getEvents().get(0).getNewFields().get().getStatus().get());
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER,
                historyResult.getEvents().get(0).getNewFields().get().getType().get());
        Assertions.assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible().isPresent());
        Assertions.assertEquals(1,
                historyResult.getEvents().get(0).getNewTransferResponsible().get().getResponsibleGroups().size());
        Assertions.assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible().get()
                .getResponsibleGroups().stream().anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible().get()
                .getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        Assertions.assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible().get()
                .getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible().get()
                .getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible().get()
                .getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        Assertions.assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible().get()
                .getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible().get()
                .getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible().get()
                .getReserveResponsible().isPresent());
        Assertions.assertEquals(providerModel.getId(), historyResult.getEvents().get(0).getNewTransferResponsible()
                .get().getReserveResponsible().get().getProviderId());
        Assertions.assertEquals(1, historyResult.getEvents().get(0).getNewTransferResponsible()
                .get().getReserveResponsible().get().getResponsibleIds().size());
        Assertions.assertEquals(String.valueOf(PROVIDER_SERVICE_ID), historyResult.getEvents().get(0)
                .getNewTransferResponsible().get().getReserveResponsible().get().getServiceId());
        Assertions.assertEquals(folderTwo.getId(), historyResult.getEvents().get(0).getNewTransferResponsible()
                .get().getReserveResponsible().get().getFolderId());
        Assertions.assertTrue(historyResult.getEvents().get(0).getNewTransferResponsible()
                .get().getReserveResponsible().get().getResponsibleIds().contains(USER_1_ID));
        Assertions.assertTrue(historyResult.getEvents().get(0).getNewParameters().isPresent());
        Assertions.assertEquals(2,
                historyResult.getEvents().get(0).getNewParameters().get().getQuotaTransfers().size());
        Assertions.assertTrue(historyResult.getEvents().get(0).getNewParameters().get().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(historyResult.getEvents().get(0).getNewParameters().get().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getDestinationServiceId().equals(String.valueOf(PROVIDER_SERVICE_ID))));
        Assertions.assertTrue(historyResult.getEvents().get(0).getNewParameters().get().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderOne.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
        Assertions.assertTrue(historyResult.getEvents().get(0).getNewParameters().get().getQuotaTransfers().stream()
                .anyMatch(t -> t.getDestinationFolderId().equals(folderTwo.getId())
                        && t.getResourceTransfers().stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().equals("-50") && r.getDeltaUnit().equals("KB")
                        && r.getDeltaUnitId().equals(KILOBYTES))));
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
