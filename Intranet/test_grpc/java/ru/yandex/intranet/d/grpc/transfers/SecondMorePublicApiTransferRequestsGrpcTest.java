package ru.yandex.intranet.d.grpc.transfers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.yandex.ydb.table.transaction.TransactionMode;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuple8;
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.backend.service.proto.CancelTransferRequest;
import ru.yandex.intranet.d.backend.service.proto.CreateTransferRequest;
import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;
import ru.yandex.intranet.d.backend.service.proto.GetTransferRequest;
import ru.yandex.intranet.d.backend.service.proto.ReserveResourceTransfer;
import ru.yandex.intranet.d.backend.service.proto.ReserveTransferParameters;
import ru.yandex.intranet.d.backend.service.proto.Transfer;
import ru.yandex.intranet.d.backend.service.proto.TransferAmount;
import ru.yandex.intranet.d.backend.service.proto.TransferParameters;
import ru.yandex.intranet.d.backend.service.proto.TransferStatus;
import ru.yandex.intranet.d.backend.service.proto.TransferSubtype;
import ru.yandex.intranet.d.backend.service.proto.TransferType;
import ru.yandex.intranet.d.backend.service.proto.TransferUser;
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
import ru.yandex.intranet.d.dao.users.AbcServiceMemberDao;
import ru.yandex.intranet.d.dao.users.UsersDao;
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
import ru.yandex.intranet.d.model.transfers.PendingTransferRequestsModel;
import ru.yandex.intranet.d.model.users.UserModel;
import ru.yandex.intranet.d.services.notifications.NotificationMailSenderStub;
import ru.yandex.intranet.d.util.Long2LongMultimap;
import ru.yandex.intranet.d.utils.ErrorsHelper;
import ru.yandex.intranet.d.web.model.SortOrderDto;

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
import static ru.yandex.intranet.d.grpc.transfers.MorePublicApiTransferRequestsGrpcTest.serviceBuilder;
import static ru.yandex.intranet.d.grpc.transfers.PublicApiTransferRequestsGrpcTest.folderModel;
import static ru.yandex.intranet.d.grpc.transfers.PublicApiTransferRequestsGrpcTest.providerModel;
import static ru.yandex.intranet.d.grpc.transfers.PublicApiTransferRequestsGrpcTest.quotaModel;
import static ru.yandex.intranet.d.grpc.transfers.PublicApiTransferRequestsGrpcTest.resourceModel;
import static ru.yandex.intranet.d.grpc.transfers.PublicApiTransferRequestsGrpcTest.resourceSegmentModel;
import static ru.yandex.intranet.d.grpc.transfers.PublicApiTransferRequestsGrpcTest.resourceSegmentationModel;
import static ru.yandex.intranet.d.grpc.transfers.PublicApiTransferRequestsGrpcTest.resourceTypeModel;
import static ru.yandex.intranet.d.grpc.transfers.ThirdMorePublicApiTransferRequestsGrpcTest.membershipBuilder;
import static ru.yandex.intranet.d.grpc.transfers.ThirdMorePublicApiTransferRequestsGrpcTest.reserveFolderModel;
import static ru.yandex.intranet.d.model.users.UserServiceRoles.QUOTA_MANAGER;
import static ru.yandex.intranet.d.model.users.UserServiceRoles.RESPONSIBLE_OF_PROVIDER;

/**
 * Transfer requests GRPC public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class SecondMorePublicApiTransferRequestsGrpcTest {

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
    public static final long FOLDER_SERVICE_ID = 7765L;
    public static final long PROVIDER_SERVICE_ID = 7766L;

    public SecondMorePublicApiTransferRequestsGrpcTest(
            @Value("${abc.roles.quotaManager}") long quotaManagerRoleId,
            @Value("${abc.roles.responsibleOfProvider}") long responsibleOfProvider) {
        this.quotaManagerRoleId = quotaManagerRoleId;
        this.responsibleOfProvide = responsibleOfProvider;
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void createTransferRequestFailOnBadResourceTest() {
        boolean firstFailure = false;
        try {
            transfersService
                .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                .createTransfer(CreateTransferRequest.newBuilder()
                        .setType(TransferType.RESERVE_TRANSFER)
                        .setAddConfirmation(false)
                        .setParameters(TransferParameters.newBuilder()
                                .setReserveTransfer(ReserveTransferParameters.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setFolderId(TEST_FOLDER_SERVICE_D)
                                        .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                .setDelta(TransferAmount.newBuilder()
                                                        .setValue(100L)
                                                        .setUnitKey("bytes")
                                                        .build())
                                                .setResourceId(UUID.randomUUID().toString())
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
        boolean secondFailure = false;
        try {
            transfersService
                    .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                    .createTransfer(CreateTransferRequest.newBuilder()
                            .setType(TransferType.RESERVE_TRANSFER)
                            .setAddConfirmation(false)
                            .setParameters(TransferParameters.newBuilder()
                                    .setReserveTransfer(ReserveTransferParameters.newBuilder()
                                            .setProviderId(YP_ID)
                                            .setFolderId(TEST_FOLDER_SERVICE_D)
                                            .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                    .setDelta(TransferAmount.newBuilder()
                                                            .setValue(100L)
                                                            .setUnitKey("bytes")
                                                            .build())
                                                    .setResourceId(YDB_RAM_SAS)
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

        boolean thirdFailure = false;
        try {
            transfersService
                    .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                    .createTransfer(CreateTransferRequest.newBuilder()
                            .setType(TransferType.RESERVE_TRANSFER)
                            .setAddConfirmation(false)
                            .setParameters(TransferParameters.newBuilder()
                                    .setReserveTransfer(ReserveTransferParameters.newBuilder()
                                            .setProviderId(YP_ID)
                                            .setFolderId(TEST_FOLDER_SERVICE_D)
                                            .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                    .setDelta(TransferAmount.newBuilder()
                                                            .setValue(100L)
                                                            .setUnitKey("bytes")
                                                            .build())
                                                    .setResourceId(YP_HDD_UNMANAGED)
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
    public void createTransferRequestFailOnBadUnitTest() {
        try {
            transfersService
                    .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                    .createTransfer(CreateTransferRequest.newBuilder()
                            .setType(TransferType.RESERVE_TRANSFER)
                            .setAddConfirmation(false)
                            .setParameters(TransferParameters.newBuilder()
                                    .setReserveTransfer(ReserveTransferParameters.newBuilder()
                                            .setProviderId(YP_ID)
                                            .setFolderId(TEST_FOLDER_SERVICE_D)
                                            .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                    .setDelta(TransferAmount.newBuilder()
                                                            .setValue(100L)
                                                            .setUnitKey("millicores")
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
    public void createTransferRequestFailOnLowBalanceTest() {
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepare();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();

        try {
            transfersService
                    .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                    .createTransfer(CreateTransferRequest.newBuilder()
                            .setType(TransferType.RESERVE_TRANSFER)
                            .setAddConfirmation(false)
                            .setParameters(TransferParameters.newBuilder()
                                    .setReserveTransfer(ReserveTransferParameters.newBuilder()
                                            .setProviderId(providerModel.getId())
                                            .setFolderId(folderOne.getId())
                                            .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                    .setDelta(TransferAmount.newBuilder()
                                                            .setValue(60000L)
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
        try {
            transfersService
                    .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
                    .createTransfer(CreateTransferRequest.newBuilder()
                            .setType(TransferType.RESERVE_TRANSFER)
                            .setAddConfirmation(false)
                            .setParameters(TransferParameters.newBuilder()
                                    .setReserveTransfer(ReserveTransferParameters.newBuilder()
                                            .setProviderId(providerModel.getId())
                                            .setFolderId(folderOne.getId())
                                            .addResourceTransfers(ReserveResourceTransfer.newBuilder()
                                                    .setDelta(TransferAmount.newBuilder()
                                                            .setValue(60000L)
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
    public void createTransferRequestFailOnWithoutQuorumTest() {
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepare();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();

        try {
            transfersService
                    .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
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
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
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
        Transfer result = transfersService
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
                                                        .setValue(50000L)
                                                        .setUnitKey("bytes")
                                                        .build())
                                                .setResourceId(resourceOne.getId())
                                                .build()))
                                .build())
                        .build());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(TransferType.RESERVE_TRANSFER, result.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_RESERVE_TRANSFER, result.getTransferSubtype());
        Assertions.assertEquals(TransferStatus.PENDING, result.getStatus());
        Assertions.assertEquals(USER_2_UID, result.getCreatedBy().getUid());
        Assertions.assertTrue(result.getVotes().getVotersList().isEmpty());
        Assertions.assertEquals(1, result.getResponsible().getGroupedList().size());
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId())));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID)
                                && r.getServiceIdsList().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(result.getResponsible().getProviderReserveResponsibleList().isEmpty());
        Assertions.assertEquals(result.getResponsible().getProviderReserveResponsibleList().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, result.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().size());
        Assertions.assertTrue(result.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().stream().map(TransferUser::getUid)
                .collect(Collectors.toList()).contains(USER_1_UID));
        Assertions.assertEquals(PROVIDER_SERVICE_ID, result.getResponsible()
                .getProviderReserveResponsibleList().get(0).getServiceId());
        Assertions.assertEquals(folderTwo.getId(), result.getResponsible().getProviderReserveResponsibleList()
                .get(0).getFolderId());
        Assertions.assertFalse(result.getParameters().getReserveTransfer().getResourceTransfersList().isEmpty());
        Assertions.assertEquals(folderOne.getId(), result.getParameters().getReserveTransfer().getFolderId());
        Assertions.assertTrue(result.getParameters().getReserveTransfer().getResourceTransfersList()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().getValue() == 50000L
                        && r.getDelta().getUnitKey().equals("bytes")));
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
        Transfer result = transfersService
                .withCallCredentials(MockGrpcUser.uid(USER_1_UID))
                .createTransfer(CreateTransferRequest.newBuilder()
                        .setType(TransferType.RESERVE_TRANSFER)
                        .setAddConfirmation(true)
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
        Assertions.assertEquals(TransferType.RESERVE_TRANSFER, result.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_RESERVE_TRANSFER, result.getTransferSubtype());
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
        Assertions.assertEquals(USER_1_UID, result.getCreatedBy().getUid());
        Assertions.assertEquals(1, result.getVotes().getVotersList().size());
        Assertions.assertEquals(USER_1_UID,
                result.getVotes().getVotersList().get(0).getVoter().getUid());
        Assertions.assertEquals(VoteType.CONFIRM,
                result.getVotes().getVotersList().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId(), folderTwo.getId()),
                new HashSet<>(result.getVotes().getVotersList().get(0).getFolderIdsList()));
        Assertions.assertTrue(result.getVotes().getVotersList().get(0).getServiceIdsList().isEmpty());
        Assertions.assertEquals(0, result.getResponsible().getGroupedList().size());
        Assertions.assertTrue(result.getResponsible().getProviderReserveResponsibleList().isEmpty());
        Assertions.assertEquals(1, result.getResponsible().getProviderSuperResponsibleList().size());
        Assertions.assertEquals(USER_1_UID, result.getResponsible().getProviderSuperResponsibleList()
                .get(0).getResponsible().getUid());
        Assertions.assertEquals(1, result.getResponsible().getProviderSuperResponsibleList()
                .get(0).getProviderIdsList().size());
        Assertions.assertTrue(result.getResponsible().getProviderSuperResponsibleList()
                .get(0).getProviderIdsList().contains(providerModel.getId()));
        Assertions.assertFalse(result.getParameters().getReserveTransfer().getResourceTransfersList().isEmpty());
        Assertions.assertEquals(folderOne.getId(), result.getParameters().getReserveTransfer().getFolderId());
        Assertions.assertTrue(result.getParameters().getReserveTransfer().getResourceTransfersList()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().getValue() == 50000L
                        && r.getDelta().getUnitKey().equals("bytes")));
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
        Transfer result = transfersService
                .withCallCredentials(MockGrpcUser.uid(USER_2_UID))
                .createTransfer(CreateTransferRequest.newBuilder()
                        .setType(TransferType.RESERVE_TRANSFER)
                        .setAddConfirmation(true)
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
        Assertions.assertEquals(TransferType.RESERVE_TRANSFER, result.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_RESERVE_TRANSFER, result.getTransferSubtype());
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
        Assertions.assertEquals(1, result.getVotes().getVotersList().size());
        Assertions.assertEquals(USER_2_UID,
                result.getVotes().getVotersList().get(0).getVoter().getUid());
        Assertions.assertEquals(VoteType.CONFIRM,
                result.getVotes().getVotersList().get(0).getVoteType());
        Assertions.assertEquals(Set.of(folderOne.getId()),
                new HashSet<>(result.getVotes().getVotersList().get(0).getFolderIdsList()));
        Assertions.assertEquals(Set.of(FOLDER_SERVICE_ID),
                new HashSet<>(result.getVotes().getVotersList().get(0).getServiceIdsList()));
        Assertions.assertEquals(1, result.getResponsible().getGroupedList().size());
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId())));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID)
                                && r.getServiceIdsList().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(result.getResponsible().getProviderReserveResponsibleList().isEmpty());
        Assertions.assertEquals(providerModel.getId(), result.getResponsible()
                .getProviderReserveResponsibleList().get(0).getProviderId());
        Assertions.assertEquals(1, result.getResponsible().getProviderReserveResponsibleList().get(0)
                .getResponsibleUsersList().size());
        Assertions.assertTrue(result.getResponsible().getProviderReserveResponsibleList().get(0)
                .getResponsibleUsersList().stream().map(TransferUser::getUid)
                .collect(Collectors.toList()).contains(USER_1_UID));
        Assertions.assertFalse(result.getParameters().getReserveTransfer().getResourceTransfersList().isEmpty());
        Assertions.assertEquals(folderOne.getId(), result.getParameters().getReserveTransfer().getFolderId());
        Assertions.assertTrue(result.getParameters().getReserveTransfer().getResourceTransfersList()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().getValue() == 50000L
                        && r.getDelta().getUnitKey().equals("bytes")));
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
        Transfer result = transfersService
                .withCallCredentials(MockGrpcUser.uid(USER_3_UID))
                .createTransfer(CreateTransferRequest.newBuilder()
                        .setType(TransferType.RESERVE_TRANSFER)
                        .setAddConfirmation(true)
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
        Assertions.assertEquals(TransferType.RESERVE_TRANSFER, result.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_RESERVE_TRANSFER, result.getTransferSubtype());
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
        Assertions.assertEquals(USER_3_UID, result.getCreatedBy().getUid());
        Assertions.assertTrue(result.getVotes().getVotersList().isEmpty());
        Assertions.assertEquals(1, result.getResponsible().getGroupedList().size());
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId())));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID)
                                && r.getServiceIdsList().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(result.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(result.getResponsible().getProviderReserveResponsibleList().isEmpty());
        Assertions.assertEquals(result.getResponsible().getProviderReserveResponsibleList().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, result.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().size());
        Assertions.assertTrue(result.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().stream().map(TransferUser::getUid)
                .collect(Collectors.toList()).contains(USER_1_UID));
        Assertions.assertFalse(result.getParameters().getReserveTransfer().getResourceTransfersList().isEmpty());
        Assertions.assertEquals(folderOne.getId(), result.getParameters().getReserveTransfer().getFolderId());
        Assertions.assertTrue(result.getParameters().getReserveTransfer().getResourceTransfersList()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().getValue() == 50000L
                        && r.getDelta().getUnitKey().equals("bytes")));
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

        Transfer result = transfersService
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
                                                        .setValue(50000L)
                                                        .setUnitKey("bytes")
                                                        .build())
                                                .setResourceId(resourceOne.getId())
                                                .build()))
                                .build())
                        .build());
        Assertions.assertNotNull(result);
        Transfer getResult = transfersService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
                .getTransfer(GetTransferRequest.newBuilder()
                        .setTransferId(result.getTransferId())
                        .build());
        Assertions.assertNotNull(getResult);
        Assertions.assertEquals(TransferType.RESERVE_TRANSFER, getResult.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_RESERVE_TRANSFER, result.getTransferSubtype());
        Assertions.assertEquals(TransferStatus.PENDING, getResult.getStatus());
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
        Assertions.assertEquals(USER_2_UID, getResult.getCreatedBy().getUid());
        Assertions.assertTrue(getResult.getVotes().getVotersList().isEmpty());
        Assertions.assertEquals(1, getResult.getResponsible().getGroupedList().size());
        Assertions.assertTrue(getResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId())));
        Assertions.assertTrue(getResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID))));
        Assertions.assertTrue(getResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(getResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(getResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID)
                                && r.getServiceIdsList().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(getResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(getResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(getResult.getResponsible().getProviderReserveResponsibleList().isEmpty());
        Assertions.assertEquals(getResult.getResponsible().getProviderReserveResponsibleList().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, getResult.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().size());
        Assertions.assertTrue(getResult.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().stream().map(TransferUser::getUid)
                .collect(Collectors.toList()).contains(USER_1_UID));
        Assertions.assertFalse(getResult.getParameters().getReserveTransfer().getResourceTransfersList().isEmpty());
        Assertions.assertEquals(folderOne.getId(), getResult.getParameters().getReserveTransfer().getFolderId());
        Assertions.assertTrue(getResult.getParameters().getReserveTransfer().getResourceTransfersList()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().getValue() == 50000L
                        && r.getDelta().getUnitKey().equals("bytes")));
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
        Transfer result = transfersService
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
                                                        .setValue(50000L)
                                                        .setUnitKey("bytes")
                                                        .build())
                                                .setResourceId(resourceOne.getId())
                                                .build()))
                                .build())
                        .build());
        Assertions.assertNotNull(result);
        Transfer cancelResult = transfersService
                .withCallCredentials(MockGrpcUser.uid(USER_2_UID))
                .cancelTransfer(CancelTransferRequest.newBuilder()
                        .setTransferId(result.getTransferId())
                        .setVersion(result.getVersion())
                        .build());
        Assertions.assertNotNull(cancelResult);
        Assertions.assertEquals(TransferType.RESERVE_TRANSFER, cancelResult.getType());
        Assertions.assertEquals(TransferSubtype.DEFAULT_RESERVE_TRANSFER, cancelResult.getTransferSubtype());
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
        Assertions.assertEquals(USER_2_UID, cancelResult.getCreatedBy().getUid());
        Assertions.assertTrue(cancelResult.getVotes().getVotersList().isEmpty());
        Assertions.assertEquals(1, cancelResult.getResponsible().getGroupedList().size());
        Assertions.assertTrue(cancelResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId())));
        Assertions.assertTrue(cancelResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID))));
        Assertions.assertTrue(cancelResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID))));
        Assertions.assertTrue(cancelResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID))));
        Assertions.assertTrue(cancelResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(USER_2_UID)
                                && r.getServiceIdsList().contains(FOLDER_SERVICE_ID))));
        Assertions.assertTrue(cancelResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertTrue(cancelResult.getResponsible().getGroupedList().stream()
                .anyMatch(g -> g.getFolderIdsList().contains(folderOne.getId()) && g.getResponsibleSetList().stream()
                        .anyMatch(r -> r.getResponsible().getUid().equals(SERVICE_1_QUOTA_MANAGER_2_UID)
                                && r.getServiceIdsList().contains(TEST_SERVICE_ID_DISPENSER))));
        Assertions.assertFalse(cancelResult.getResponsible().getProviderReserveResponsibleList().isEmpty());
        Assertions.assertEquals(cancelResult.getResponsible().getProviderReserveResponsibleList().get(0)
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, cancelResult.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().size());
        Assertions.assertTrue(cancelResult.getResponsible().getProviderReserveResponsibleList()
                .get(0).getResponsibleUsersList().stream().map(TransferUser::getUid)
                .collect(Collectors.toList()).contains(USER_1_UID));
        Assertions.assertFalse(cancelResult.getParameters().getReserveTransfer().getResourceTransfersList().isEmpty());
        Assertions.assertEquals(folderOne.getId(),
                cancelResult.getParameters().getReserveTransfer().getFolderId());
        Assertions.assertTrue(cancelResult.getParameters().getReserveTransfer().getResourceTransfersList()
                .stream().anyMatch(r -> r.getResourceId().equals(resourceOne.getId())
                        && r.getDelta().getValue() == 50000L
                        && r.getDelta().getUnitKey().equals("bytes")));
    }

    private Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel,
            QuotaModel, QuotaModel> prepareWithRoles() {
        return prepareWithRoles(tableClient, servicesDao, abcServiceMemberDao, responsibleOfProvide, quotaManagerRoleId,
                usersDao, providersDao, resourceTypesDao, resourceSegmentationsDao, resourceSegmentsDao, resourcesDao,
                folderDao, quotasDao);
    }

    @SuppressWarnings("ParameterNumber")
    public static Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel,
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
