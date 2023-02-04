package ru.yandex.intranet.d.web.front.transfer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.folders.FolderDao;
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
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.model.resources.ResourceModel;
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel;
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel;
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel;
import ru.yandex.intranet.d.model.services.ServiceRecipeModel;
import ru.yandex.intranet.d.model.users.UserModel;
import ru.yandex.intranet.d.util.FrontStringUtil;
import ru.yandex.intranet.d.util.Long2LongMultimap;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaResourceTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateReserveTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestParametersDto;
import ru.yandex.intranet.d.web.model.transfers.front.dryrun.FrontDryRunCreateTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.dryrun.FrontDryRunSingleTransferResultDto;
import ru.yandex.intranet.d.web.model.transfers.front.dryrun.FrontDryRunTransferFolderWarningsDto;
import ru.yandex.intranet.d.web.model.transfers.front.dryrun.FrontDryRunTransferPermissionsDto;
import ru.yandex.intranet.d.web.model.transfers.front.dryrun.FrontDryRunTransferQuotaDto;
import ru.yandex.intranet.d.web.model.transfers.front.dryrun.FrontDryRunTransferQuotasDto;
import ru.yandex.intranet.d.web.model.transfers.front.dryrun.FrontDryRunTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.dryrun.FrontDryRunTransferWarningsDto;

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
import static ru.yandex.intranet.d.UnitIds.BYTES;
import static ru.yandex.intranet.d.model.users.UserServiceRoles.QUOTA_MANAGER;
import static ru.yandex.intranet.d.model.users.UserServiceRoles.RESPONSIBLE_OF_PROVIDER;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.folderModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.providerModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.quotaModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.resourceModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.resourceSegmentModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.resourceSegmentationModel;
import static ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest.resourceTypeModel;
import static ru.yandex.intranet.d.web.front.transfer.MoreFrontTransferRequestsControllerTest.serviceBuilder;
import static ru.yandex.intranet.d.web.front.transfer.SecondMoreFrontTransferRequestsControllerTest.FOLDER_SERVICE_ID;
import static ru.yandex.intranet.d.web.front.transfer.SecondMoreFrontTransferRequestsControllerTest.PROVIDER_SERVICE_ID;
import static ru.yandex.intranet.d.web.front.transfer.ThirdMoreFrontTransferRequestsControllerTest.membershipBuilder;
import static ru.yandex.intranet.d.web.front.transfer.ThirdMoreFrontTransferRequestsControllerTest.reserveFolderModel;

/**
 * FrontDryRUnTransferRequestsControllerTest
 *
 * @author Denis Blokhin <denblo@yandex-team.ru>
 */
@IntegrationTest
public class FrontDryRunTransferRequestsControllerTest {

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
    private YdbTableClient tableClient;
    @Autowired
    private ServicesDao servicesDao;
    @Autowired
    private AbcServiceMemberDao abcServiceMemberDao;
    @Autowired
    private UsersDao usersDao;

    private final long quotaManagerRoleId;
    private final long responsibleOfProvide;

    public FrontDryRunTransferRequestsControllerTest(
            @Value("${abc.roles.quotaManager}") long quotaManagerRoleId,
            @Value("${abc.roles.responsibleOfProvider}") long responsibleOfProvider) {
        this.quotaManagerRoleId = quotaManagerRoleId;
        this.responsibleOfProvide = responsibleOfProvider;
    }


    @Test
    public void dryRunShouldWork() {
        ProviderModel provider = providerModel("in-process:test", null, false);
        ResourceTypeModel resourceType = resourceTypeModel(provider.getId(), "test",
                "b02344bf-96af-4cc5-937c-66a479989ce8");
        ResourceSegmentationModel locationSegmentation = resourceSegmentationModel(provider.getId(), "location");
        ResourceSegmentModel vlaSegment = resourceSegmentModel(locationSegmentation.getId(), "VLA");
        ResourceSegmentModel sasSegment = resourceSegmentModel(locationSegmentation.getId(), "SAS");
        ResourceModel resource = resourceModel(provider.getId(), "test", resourceType.getId(),
                Set.of(Tuples.of(locationSegmentation.getId(), vlaSegment.getId())),
                "b02344bf-96af-4cc5-937c-66a479989ce8",
                Set.of("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                        "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", null);
        ResourceModel sasResource = resourceModel(provider.getId(), "test", resourceType.getId(),
                Set.of(Tuples.of(locationSegmentation.getId(), sasSegment.getId())),
                "b02344bf-96af-4cc5-937c-66a479989ce8",
                Set.of("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                        "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", null);
        FolderModel folderOne = folderModel(1L);
        FolderModel folderTwo = folderModel(2L);
        QuotaModel quotaOne = quotaModel(provider.getId(), resource.getId(), folderOne.getId(), 50, 50);
        QuotaModel quotaTwo = quotaModel(provider.getId(), resource.getId(), folderTwo.getId(), 150, 150);
        QuotaModel quotaThree = quotaModel(provider.getId(), sasResource.getId(), folderOne.getId(), 200, 200);
        QuotaModel quotaFour = quotaModel(provider.getId(), sasResource.getId(), folderTwo.getId(), 300, 300);
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
                        .upsertResourceSegmentsRetryable(txSession, List.of(vlaSegment, sasSegment))))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> resourcesDao
                        .upsertResourcesRetryable(txSession, List.of(resource, sasResource))))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertAllRetryable(txSession, List.of(folderOne, folderTwo))))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .upsertAllRetryable(txSession, List.of(quotaOne, quotaTwo, quotaThree, quotaFour))))
                .block();
        FrontDryRunSingleTransferResultDto result = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers/_dryRun")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new FrontDryRunCreateTransferRequestDto(
                        TransferRequestTypeDto.QUOTA_TRANSFER,
                        FrontCreateTransferRequestParametersDto.builder()
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId("1")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("100")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("-100")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .build(),
                        false,
                        false,
                        null
                ))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontDryRunSingleTransferResultDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(vlaSegment.getId(),
                Objects.requireNonNull(result.getResources().get(resource.getId()).getSegments())
                        .get(locationSegmentation.getId()));
        Assertions.assertEquals(vlaSegment.getNameEn(), result.getSegments().get(vlaSegment.getId()).getName());
        Assertions.assertEquals(locationSegmentation.getNameEn(),
                result.getSegmentations().get(locationSegmentation.getId()).getName());
        Assertions.assertEquals(locationSegmentation.getGroupingOrder(),
                result.getSegmentations().get(locationSegmentation.getId()).getGroupingOrder());
        Assertions.assertEquals(resourceType.getNameEn(),
                result.getResourceTypes().get(resourceType.getId()).getName());
        FrontDryRunTransferRequestDto transfer = result.getTransfer();
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, transfer.getRequestType());
        Map<String, Map<String, String>> oldQuotas = transfer.getQuotasOld().stream()
                .collect(Collectors.toMap(FrontDryRunTransferQuotasDto::getFolderId, q -> q.getQuotas().stream()
                        .collect(Collectors.toMap(FrontDryRunTransferQuotaDto::getResourceId,
                                FrontDryRunTransferQuotaDto::getBalance))));

        Assertions.assertEquals(Map.of(
                folderOne.getId(), Map.of(resource.getId(), FrontStringUtil.toString(BigDecimal.valueOf(5000, 2))),
                folderTwo.getId(), Map.of(resource.getId(), FrontStringUtil.toString(BigDecimal.valueOf(15000, 2)))
        ), oldQuotas);

        Map<String, Map<String, String>> newQuotas = transfer.getQuotasNew().stream()
                .collect(Collectors.toMap(FrontDryRunTransferQuotasDto::getFolderId, q -> q.getQuotas().stream()
                        .collect(Collectors.toMap(FrontDryRunTransferQuotaDto::getResourceId,
                                FrontDryRunTransferQuotaDto::getBalance))));

        Assertions.assertEquals(Map.of(
                folderOne.getId(), Map.of(resource.getId(), FrontStringUtil.toString(BigDecimal.valueOf(15000, 2))),
                folderTwo.getId(), Map.of(resource.getId(), FrontStringUtil.toString(BigDecimal.valueOf(5000, 2)))
        ), newQuotas);

        Assertions.assertEquals(newQuotas.keySet(), oldQuotas.keySet());
    }

    @Test
    public void dryRunResponsibleShouldBeReturned() {
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
        FrontDryRunSingleTransferResultDto result = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers/_dryRun")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new FrontDryRunCreateTransferRequestDto(
                        TransferRequestTypeDto.QUOTA_TRANSFER,
                        FrontCreateTransferRequestParametersDto.builder()
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId("1")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("100")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("-100")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .build(),
                        true,
                        false,
                        null
                ))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontDryRunSingleTransferResultDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getTransferResponsible().isPresent());

        Assertions.assertTrue(result.getTransferResponsible().get()
                .getResponsibleGroups().stream()
                .noneMatch(x -> x.getResponsibles().isEmpty()));
    }

    @Test
    public void dryRunPermissionsShouldBeReturned() {
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
        FrontDryRunSingleTransferResultDto result = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers/_dryRun")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new FrontDryRunCreateTransferRequestDto(
                        TransferRequestTypeDto.QUOTA_TRANSFER,
                        FrontCreateTransferRequestParametersDto.builder()
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId("1")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("100")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("-100")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .build(),
                        true,
                        false,
                        null
                ))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontDryRunSingleTransferResultDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getPermissions().isPresent());
        FrontDryRunTransferPermissionsDto permissions = result.getPermissions().get();
        Assertions.assertTrue(permissions.isCanVote());
        Assertions.assertTrue(permissions.isCanConfirmSingleHandedly());
        Assertions.assertFalse(permissions.isCanAutoConfirmAsProviderResponsible());
    }

    @Test
    public void dryRunWarningsShouldBeReturned() {
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
        FrontDryRunSingleTransferResultDto result = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers/_dryRun")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new FrontDryRunCreateTransferRequestDto(
                        TransferRequestTypeDto.QUOTA_TRANSFER,
                        FrontCreateTransferRequestParametersDto.builder()
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId("1")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("1000")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("-1000")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .build(),
                        false,
                        false,
                        null
                ))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontDryRunSingleTransferResultDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);

        FrontDryRunTransferRequestDto transfer = result.getTransfer();
        Assertions.assertEquals(TransferRequestTypeDto.QUOTA_TRANSFER, transfer.getRequestType());
        Map<String, Map<String, String>> oldQuotas = transfer.getQuotasOld().stream()
                .collect(Collectors.toMap(FrontDryRunTransferQuotasDto::getFolderId, q -> q.getQuotas().stream()
                        .collect(Collectors.toMap(FrontDryRunTransferQuotaDto::getResourceId,
                                FrontDryRunTransferQuotaDto::getBalance))));

        Assertions.assertEquals(Map.of(
                folderOne.getId(), Map.of(resource.getId(), FrontStringUtil.toString(BigDecimal.valueOf(5000, 2))),
                folderTwo.getId(), Map.of(resource.getId(), FrontStringUtil.toString(BigDecimal.valueOf(15000, 2)))
        ), oldQuotas);

        Map<String, Map<String, String>> newQuotas = transfer.getQuotasNew().stream()
                .collect(Collectors.toMap(FrontDryRunTransferQuotasDto::getFolderId, q -> q.getQuotas().stream()
                        .collect(Collectors.toMap(FrontDryRunTransferQuotaDto::getResourceId,
                                FrontDryRunTransferQuotaDto::getBalance))));

        Assertions.assertEquals(Map.of(
                folderOne.getId(), Map.of(resource.getId(), FrontStringUtil.toString(BigDecimal.valueOf(1.05))),
                folderTwo.getId(), Map.of(resource.getId(), FrontStringUtil.toString(BigDecimal.valueOf(-85000, 2)))
        ), newQuotas);

        FrontDryRunTransferWarningsDto warnings = result.getWarnings();
        List<String> messages = List.of("Not enough balance for quota transfer.\n" +
                "The balance is 150 B.");
        Assertions.assertTrue(warnings.getGeneral().isEmpty());
        Assertions.assertEquals(messages, warnings.getPerResource().get(resource.getId()));
        Assertions.assertEquals("Transfer all quota",
                warnings.getDetailsPerResource().get(resource.getId()).get("suggestedAmountPrompt"));
        FrontDryRunTransferFolderWarningsDto folderWarnings = warnings.getPerFolder().get(folderTwo.getId());
        Assertions.assertEquals(messages, folderWarnings.getMessages());

        Object suggestedAmount = warnings.getDetailsPerResource().get(resource.getId()).get("suggestedAmount");
        Assertions.assertTrue(Map.class.isAssignableFrom(suggestedAmount.getClass()));
        //noinspection unchecked
        Map<String, String> amountDto = (Map<String, String>) suggestedAmount;
        Assertions.assertEquals("150", amountDto.get("rawAmount"));

        Object suggestedAmountPerFolder = warnings.getPerFolder().get(folderTwo.getId()).getDetailsPerResource()
                .get(resource.getId()).get("suggestedAmount");
        Assertions.assertTrue(Map.class.isAssignableFrom(suggestedAmountPerFolder.getClass()));
        //noinspection unchecked
        Map<String, String> amountDtoPerFolder = (Map<String, String>) suggestedAmountPerFolder;
        Assertions.assertEquals("150", amountDtoPerFolder.get("rawAmount"));
    }

    @Test
    public void dryRunWarningsWithoutSuggestedAmount() {
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
        QuotaModel quotaTwo = quotaModel(provider.getId(), resource.getId(), folderTwo.getId(), 150, -100);
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
        FrontDryRunSingleTransferResultDto result = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers/_dryRun")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new FrontDryRunCreateTransferRequestDto(
                        TransferRequestTypeDto.QUOTA_TRANSFER,
                        FrontCreateTransferRequestParametersDto.builder()
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId("1")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("1000")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                        .destinationFolderId(folderTwo.getId())
                                        .destinationServiceId("2")
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("-1000")
                                                .resourceId(resource.getId())
                                                .deltaUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                                                .build())
                                        .build())
                                .build(),
                        false,
                        false,
                        null
                ))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontDryRunSingleTransferResultDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);

        FrontDryRunTransferWarningsDto warnings = result.getWarnings();
        List<String> messages = List.of("Not enough balance for quota transfer.\n" +
                "250 B has been provided to the provider, it must be lifted to the balance to transfer it.\n" +
                "The balance is -100 B.");
        Assertions.assertTrue(warnings.getGeneral().isEmpty());
        Assertions.assertEquals(messages, warnings.getPerResource().get(resource.getId()));

        FrontDryRunTransferFolderWarningsDto folderWarnings = warnings.getPerFolder().get(folderTwo.getId());
        Assertions.assertEquals(messages, folderWarnings.getMessages());
        Assertions.assertNull(warnings.getDetailsPerResource().get(resource.getId()));
        Assertions.assertNull(warnings.getPerFolder().get(folderTwo.getId()).getDetailsPerResource()
                .get(resource.getId()));
    }

    private Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel,
            QuotaModel, QuotaModel> prepareWithRoles() {
        return SecondMoreFrontTransferRequestsControllerTest.prepareWithRoles(tableClient, servicesDao,
                abcServiceMemberDao, responsibleOfProvide, quotaManagerRoleId, usersDao, providersDao, resourceTypesDao,
                resourceSegmentationsDao, resourceSegmentsDao, resourcesDao, folderDao, quotasDao);
    }

    @Test
    public void reserveTransferDryRunTest() {
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepareWithRoles();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();
        FolderModel folderTwo = prepare.getT6();

        FrontDryRunSingleTransferResultDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers/_dryRun")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new FrontDryRunCreateTransferRequestDto(
                        TransferRequestTypeDto.RESERVE_TRANSFER,
                        FrontCreateTransferRequestParametersDto.builder()
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
                                .build(),
                        false,
                        false,
                        null
                ))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontDryRunSingleTransferResultDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);

        FrontDryRunTransferRequestDto transfer = result.getTransfer();
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, transfer.getRequestType());
        Map<String, Map<String, String>> oldQuotas = transfer.getQuotasOld().stream()
                .collect(Collectors.toMap(FrontDryRunTransferQuotasDto::getFolderId, q -> q.getQuotas().stream()
                        .collect(Collectors.toMap(FrontDryRunTransferQuotaDto::getResourceId,
                                FrontDryRunTransferQuotaDto::getBalance))));

        Assertions.assertEquals(Map.of(
                folderOne.getId(), Map.of(resourceOne.getId(), FrontStringUtil.toString(new BigDecimal("10.00"))),
                folderTwo.getId(), Map.of(resourceOne.getId(), FrontStringUtil.toString(new BigDecimal("50.00")))
        ), oldQuotas);

        Map<String, Map<String, String>> newQuotas = transfer.getQuotasNew().stream()
                .collect(Collectors.toMap(FrontDryRunTransferQuotasDto::getFolderId, q -> q.getQuotas().stream()
                        .collect(Collectors.toMap(FrontDryRunTransferQuotaDto::getResourceId,
                                FrontDryRunTransferQuotaDto::getBalance))));

        Assertions.assertEquals(Map.of(
                folderOne.getId(), Map.of(resourceOne.getId(), FrontStringUtil.toString(new BigDecimal("60.00"))),
                folderTwo.getId(), Map.of(resourceOne.getId(), FrontStringUtil.toString(new BigDecimal("0.00")))
        ), newQuotas);

        Assertions.assertEquals(newQuotas.keySet(), oldQuotas.keySet());
    }

    @Test
    public void dryRunReserveTransferResponsibleShouldBeReturnedTest() {
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepareWithRoles();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();
        FolderModel folderTwo = prepare.getT6();

        FrontDryRunSingleTransferResultDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers/_dryRun")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new FrontDryRunCreateTransferRequestDto(
                        TransferRequestTypeDto.RESERVE_TRANSFER,
                        FrontCreateTransferRequestParametersDto.builder()
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
                                .build(),
                        true,
                        false,
                        null
                ))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontDryRunSingleTransferResultDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getTransferResponsible().isPresent());

        Assertions.assertEquals(1, result.getTransferResponsible().get().getResponsibleGroups().size());
        Assertions.assertTrue(result.getTransferResponsible().get().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId())));
        Assertions.assertTrue(result.getTransferResponsible().get().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID))));
        Assertions.assertTrue(result.getTransferResponsible().get().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER))));
        Assertions.assertTrue(result.getTransferResponsible().get().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2))));
        Assertions.assertTrue(result.getTransferResponsible().get().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(USER_2_ID)
                                && r.getServiceIds().contains(String.valueOf(FOLDER_SERVICE_ID)))));
        Assertions.assertTrue(result.getTransferResponsible().get().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(result.getTransferResponsible().get().getResponsibleGroups().stream()
                .anyMatch(g -> g.getFolders().contains(folderOne.getId()) && g.getResponsibles().stream()
                        .anyMatch(r -> r.getResponsibleId().equals(SERVICE_1_QUOTA_MANAGER_2)
                                && r.getServiceIds().contains(String.valueOf(TEST_SERVICE_ID_DISPENSER)))));
        Assertions.assertTrue(result.getTransferResponsible().get().getReserveResponsible().isPresent());
        Assertions.assertEquals(result.getTransferResponsible().get().getReserveResponsible().get()
                .getProviderId(), providerModel.getId());
        Assertions.assertEquals(1, result.getTransferResponsible().get().getReserveResponsible()
                .get().getResponsibleIds().size());
        Assertions.assertTrue(result.getTransferResponsible().get().getReserveResponsible()
                .get().getResponsibleIds().contains(USER_1_ID));
        Assertions.assertEquals(String.valueOf(PROVIDER_SERVICE_ID), result.getTransferResponsible().get()
                .getReserveResponsible().get().getServiceId());
        Assertions.assertEquals(folderTwo.getId(), result.getTransferResponsible().get().getReserveResponsible()
                .get().getFolderId());
    }

    @Test
    public void dryRunReserveTransferPermissionsShouldBeReturnedTest() {
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepareWithRoles();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();

        FrontDryRunSingleTransferResultDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers/_dryRun")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new FrontDryRunCreateTransferRequestDto(
                        TransferRequestTypeDto.RESERVE_TRANSFER,
                        FrontCreateTransferRequestParametersDto.builder()
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
                                .build(),
                        false,
                        true,
                        null
                ))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontDryRunSingleTransferResultDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);

        Assertions.assertTrue(result.getPermissions().isPresent());
        FrontDryRunTransferPermissionsDto permissions = result.getPermissions().get();
        Assertions.assertTrue(permissions.isCanVote());
        Assertions.assertFalse(permissions.isCanConfirmSingleHandedly());
        Assertions.assertFalse(permissions.isCanAutoConfirmAsProviderResponsible());

        result = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/front/transfers/_dryRun")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new FrontDryRunCreateTransferRequestDto(
                        TransferRequestTypeDto.RESERVE_TRANSFER,
                        FrontCreateTransferRequestParametersDto.builder()
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
                                .build(),
                        false,
                        true,
                        null
                ))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontDryRunSingleTransferResultDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);

        Assertions.assertTrue(result.getPermissions().isPresent());
        permissions = result.getPermissions().get();
        Assertions.assertFalse(permissions.isCanVote());
        Assertions.assertFalse(permissions.isCanConfirmSingleHandedly());
        Assertions.assertTrue(permissions.isCanAutoConfirmAsProviderResponsible());
    }

    @Test
    public void dryRunReserveTransferWarningsShouldBeReturned() {
        Tuple8<ProviderModel, ResourceTypeModel, ResourceModel, ResourceModel, FolderModel, FolderModel, QuotaModel,
                QuotaModel> prepare = prepareWithRoles();
        ProviderModel providerModel = prepare.getT1();
        ResourceModel resourceOne = prepare.getT3();
        FolderModel folderOne = prepare.getT5();
        FolderModel folderTwo = prepare.getT6();

        FrontDryRunSingleTransferResultDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers/_dryRun")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new FrontDryRunCreateTransferRequestDto(
                        TransferRequestTypeDto.RESERVE_TRANSFER,
                        FrontCreateTransferRequestParametersDto.builder()
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
                                .build(),
                        false,
                        false,
                        null
                ))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontDryRunSingleTransferResultDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);

        FrontDryRunTransferRequestDto transfer = result.getTransfer();
        Assertions.assertEquals(TransferRequestTypeDto.RESERVE_TRANSFER, transfer.getRequestType());
        Map<String, Map<String, String>> oldQuotas = transfer.getQuotasOld().stream()
                .collect(Collectors.toMap(FrontDryRunTransferQuotasDto::getFolderId, q -> q.getQuotas().stream()
                        .collect(Collectors.toMap(FrontDryRunTransferQuotaDto::getResourceId,
                                FrontDryRunTransferQuotaDto::getBalance))));

        Assertions.assertEquals(Map.of(
                folderOne.getId(), Map.of(resourceOne.getId(), FrontStringUtil.toString(new BigDecimal("10.00"))),
                folderTwo.getId(), Map.of(resourceOne.getId(), FrontStringUtil.toString(new BigDecimal("50.00")))
        ), oldQuotas);

        Map<String, Map<String, String>> newQuotas = transfer.getQuotasNew().stream()
                .collect(Collectors.toMap(FrontDryRunTransferQuotasDto::getFolderId, q -> q.getQuotas().stream()
                        .collect(Collectors.toMap(FrontDryRunTransferQuotaDto::getResourceId,
                                FrontDryRunTransferQuotaDto::getBalance))));

        Assertions.assertEquals(Map.of(
                folderOne.getId(), Map.of(resourceOne.getId(), FrontStringUtil.toString(new BigDecimal("70.00"))),
                folderTwo.getId(), Map.of(resourceOne.getId(), FrontStringUtil.toString(new BigDecimal("-10.00")))
        ), newQuotas);

        FrontDryRunTransferWarningsDto warnings = result.getWarnings();
        List<String> messages = List.of("Not enough balance for quota transfer.\n" +
                "The balance is 50000 B.");
        Assertions.assertTrue(warnings.getGeneral().isEmpty());
        Assertions.assertEquals(messages, warnings.getPerResource().get(resourceOne.getId()));
        Assertions.assertEquals("Transfer all quota",
                warnings.getDetailsPerResource().get(resourceOne.getId()).get("suggestedAmountPrompt"));
        FrontDryRunTransferFolderWarningsDto folderWarnings = warnings.getPerFolder().get(folderTwo.getId());
        Assertions.assertEquals(messages, folderWarnings.getMessages());

        Object suggestedAmount = warnings.getDetailsPerResource().get(resourceOne.getId()).get("suggestedAmount");
        Assertions.assertTrue(Map.class.isAssignableFrom(suggestedAmount.getClass()));
        //noinspection unchecked
        Map<String, String> amountDto = (Map<String, String>) suggestedAmount;
        Assertions.assertEquals("50000", amountDto.get("rawAmount"));

        Object suggestedAmountPerFolder = warnings.getPerFolder().get(folderTwo.getId()).getDetailsPerResource()
                .get(resourceOne.getId()).get("suggestedAmount");
        Assertions.assertTrue(Map.class.isAssignableFrom(suggestedAmountPerFolder.getClass()));
        //noinspection unchecked
        Map<String, String> amountDtoPerFolder = (Map<String, String>) suggestedAmountPerFolder;
        Assertions.assertEquals("50000", amountDtoPerFolder.get("rawAmount"));
    }

    @Test
    public void dryRunReserveTransferWarningsWithoutSuggestedAmount() {
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

        ProviderModel provider = providerModel("in-process:test", null, false, PROVIDER_SERVICE_ID);
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
        FolderModel folderOne = folderModel(FOLDER_SERVICE_ID);
        FolderModel folderTwo = reserveFolderModel(PROVIDER_SERVICE_ID);
        QuotaModel quotaOne = quotaModel(provider.getId(), resourceOne.getId(), folderTwo.getId(), 50000, -50000);
        QuotaModel quotaThree = quotaModel(provider.getId(), resourceOne.getId(), folderOne.getId(), 10000, 10000);
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
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertAllRetryable(txSession, List.of(folderOne, folderTwo))))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .upsertAllRetryable(txSession, List.of(quotaOne, quotaThree))))
                .block();

        FrontDryRunSingleTransferResultDto result = webClient
                .mutateWith(MockUser.uid(USER_2_UID))
                .post()
                .uri("/front/transfers/_dryRun")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new FrontDryRunCreateTransferRequestDto(
                        TransferRequestTypeDto.RESERVE_TRANSFER,
                        FrontCreateTransferRequestParametersDto.builder()
                                .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                        .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                                .delta("60000")
                                                .resourceId(resourceOne.getId())
                                                .deltaUnitId(BYTES)
                                                .build())
                                        .destinationFolderId(folderOne.getId())
                                        .destinationServiceId(String.valueOf(folderOne.getServiceId()))
                                        .providerId(provider.getId())
                                        .build())
                                .build(),
                        false,
                        false,
                        null
                ))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontDryRunSingleTransferResultDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);

        FrontDryRunTransferWarningsDto warnings = result.getWarnings();
        List<String> messages = List.of("Not enough balance for quota transfer.\n" +
                "100000 B has been provided to the provider, it must be lifted to the balance to transfer it.\n" +
                "The balance is -50000 B.");
        Assertions.assertTrue(warnings.getGeneral().isEmpty());
        Assertions.assertEquals(messages, warnings.getPerResource().get(resourceOne.getId()));

        FrontDryRunTransferFolderWarningsDto folderWarnings = warnings.getPerFolder().get(folderTwo.getId());
        Assertions.assertEquals(messages, folderWarnings.getMessages());
        Assertions.assertNull(warnings.getDetailsPerResource().get(resourceOne.getId()));
        Assertions.assertNull(warnings.getPerFolder().get(folderTwo.getId()).getDetailsPerResource()
                .get(resourceOne.getId()));
    }
}
