package ru.yandex.intranet.d.web.api.delivery;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.folders.FolderDao;
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao;
import ru.yandex.intranet.d.dao.providers.ProvidersDao;
import ru.yandex.intranet.d.dao.quotas.QuotasDao;
import ru.yandex.intranet.d.dao.resources.ResourcesDao;
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao;
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao;
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao;
import ru.yandex.intranet.d.datasource.model.WithTxId;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
import ru.yandex.intranet.d.model.folders.FolderOperationType;
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
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.SortOrderDto;
import ru.yandex.intranet.d.web.model.delivery.DeliverableDeltaDto;
import ru.yandex.intranet.d.web.model.delivery.DeliverableMetaRequestDto;
import ru.yandex.intranet.d.web.model.delivery.DeliverableRequestDto;
import ru.yandex.intranet.d.web.model.delivery.DeliveryRequestDto;
import ru.yandex.intranet.d.web.model.delivery.DeliveryResponseDto;

/**
 * Delivery API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class DeliveryApiTest {

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
    @Value("${hardwareOrderService.tvmSourceId}")
    private long dispenserTvmSourceId;

    @Test
    public void testSuccessfulDeliveryToService() {
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
        FolderModel folderOne = defaultFolderModel(12L);
        QuotaModel quotaOne = quotaModel(provider.getId(), resource.getId(), folderOne.getId(), 50, 40);
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
                        .upsertAllRetryable(txSession, List.of(folderOne))))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .upsertAllRetryable(txSession, List.of(quotaOne))))
                .block();
        DeliveryResponseDto result = webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_deliver")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(DeliveryRequestDto.builder()
                        .deliveryId(UUID.randomUUID().toString())
                        .authorUid("1120000000000010")
                        .addDeliverable(DeliverableRequestDto.builder()
                                .serviceId(12L)
                                .resourceId(resource.getId())
                                .delta(DeliverableDeltaDto.builder()
                                        .amount(100L)
                                        .unitKey("bytes")
                                        .build())
                                .meta(DeliverableMetaRequestDto.builder()
                                        .quotaRequestId(69L)
                                        .campaignId(1L)
                                        .bigOrderId(42L)
                                        .build())
                                .build())
                        .addDeliverable(DeliverableRequestDto.builder()
                                .serviceId(12L)
                                .resourceId(resource.getId())
                                .delta(DeliverableDeltaDto.builder()
                                        .amount(200L)
                                        .unitKey("bytes")
                                        .build())
                                .meta(DeliverableMetaRequestDto.builder()
                                        .quotaRequestId(69L)
                                        .campaignId(1L)
                                        .bigOrderId(43L)
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(DeliveryResponseDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.getDeliverables().size());
        Assertions.assertEquals(2L,
                result.getDeliverables().stream().filter(d -> d.getServiceId().get().equals(12L)).count());
        Assertions.assertEquals(2L,
                result.getDeliverables().stream().filter(d -> d.getFolderId().isEmpty()).count());
        Assertions.assertEquals(2L, result.getDeliverables().stream()
                .filter(d -> d.getResourceId().equals(resource.getId())).count());
        Assertions.assertEquals(2L,
                result.getDeliverables().stream().filter(d -> d.getMeta().getQuotaRequestId() == 69L).count());
        Assertions.assertEquals(2L,
                result.getDeliverables().stream().filter(d -> d.getMeta().getCampaignId() == 1L).count());
        Assertions.assertEquals(1L,
                result.getDeliverables().stream().filter(d -> d.getMeta().getBigOrderId() == 42L).count());
        Assertions.assertEquals(1L,
                result.getDeliverables().stream().filter(d -> d.getMeta().getBigOrderId() == 43L).count());
        List<QuotaModel> quotasOne = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderOne.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertEquals(350, quotasOne.get(0).getQuota());
        Assertions.assertEquals(340, quotasOne.get(0).getBalance());
        List<FolderOperationLogModel> logsOne = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        folderOperationLogDao.getPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folderOne.getId(),
                                null, SortOrderDto.ASC, 100)))
                .block();
        Assertions.assertEquals(2, logsOne.size());
        Assertions.assertEquals(2L, logsOne.stream().filter(log ->
                log.getOperationType().equals(FolderOperationType.QUOTA_DELIVERY)).count());
    }

    @Test
    public void testSuccessfulDeliveryToServiceNewDefaultFolder() {
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
        DeliveryResponseDto result = webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_deliver")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(DeliveryRequestDto.builder()
                        .deliveryId(UUID.randomUUID().toString())
                        .authorUid("1120000000000010")
                        .addDeliverable(DeliverableRequestDto.builder()
                                .serviceId(12L)
                                .resourceId(resource.getId())
                                .delta(DeliverableDeltaDto.builder()
                                        .amount(100L)
                                        .unitKey("bytes")
                                        .build())
                                .meta(DeliverableMetaRequestDto.builder()
                                        .quotaRequestId(69L)
                                        .campaignId(1L)
                                        .bigOrderId(42L)
                                        .build())
                                .build())
                        .addDeliverable(DeliverableRequestDto.builder()
                                .serviceId(12L)
                                .resourceId(resource.getId())
                                .delta(DeliverableDeltaDto.builder()
                                        .amount(200L)
                                        .unitKey("bytes")
                                        .build())
                                .meta(DeliverableMetaRequestDto.builder()
                                        .quotaRequestId(69L)
                                        .campaignId(1L)
                                        .bigOrderId(43L)
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(DeliveryResponseDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.getDeliverables().size());
        Assertions.assertEquals(2L,
                result.getDeliverables().stream().filter(d -> d.getServiceId().get().equals(12L)).count());
        Assertions.assertEquals(2L,
                result.getDeliverables().stream().filter(d -> d.getFolderId().isEmpty()).count());
        Assertions.assertEquals(2L, result.getDeliverables().stream()
                .filter(d -> d.getResourceId().equals(resource.getId())).count());
        Assertions.assertEquals(2L,
                result.getDeliverables().stream().filter(d -> d.getMeta().getQuotaRequestId() == 69L).count());
        Assertions.assertEquals(2L,
                result.getDeliverables().stream().filter(d -> d.getMeta().getCampaignId() == 1L).count());
        Assertions.assertEquals(1L,
                result.getDeliverables().stream().filter(d -> d.getMeta().getBigOrderId() == 42L).count());
        Assertions.assertEquals(1L,
                result.getDeliverables().stream().filter(d -> d.getMeta().getBigOrderId() == 43L).count());
        FolderModel folderOne = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .getDefaultFolderTx(txSession, Tenants.DEFAULT_TENANT_ID, 12L)))
                .map(WithTxId::get)
                .block().get();
        List<QuotaModel> quotasOne = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderOne.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertEquals(300, quotasOne.get(0).getQuota());
        Assertions.assertEquals(300, quotasOne.get(0).getBalance());
        List<FolderOperationLogModel> logsOne = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        folderOperationLogDao.getPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folderOne.getId(),
                                null, SortOrderDto.ASC, 100)))
                .block();
        Assertions.assertEquals(3, logsOne.size());
        Assertions.assertEquals(1L, logsOne.stream().filter(log ->
                log.getOperationType().equals(FolderOperationType.FOLDER_CREATE)).count());
        Assertions.assertEquals(2L, logsOne.stream().filter(log ->
                log.getOperationType().equals(FolderOperationType.QUOTA_DELIVERY)).count());
    }

    @Test
    public void testSuccessfulDeliveryToFolder() {
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
        FolderModel folderOne = folderModel(12L);
        QuotaModel quotaOne = quotaModel(provider.getId(), resource.getId(), folderOne.getId(), 50, 40);
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
                        .upsertAllRetryable(txSession, List.of(folderOne))))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .upsertAllRetryable(txSession, List.of(quotaOne))))
                .block();
        DeliveryResponseDto result = webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_deliver")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(DeliveryRequestDto.builder()
                        .deliveryId(UUID.randomUUID().toString())
                        .authorUid("1120000000000010")
                        .addDeliverable(DeliverableRequestDto.builder()
                                .folderId(folderOne.getId())
                                .resourceId(resource.getId())
                                .delta(DeliverableDeltaDto.builder()
                                        .amount(100L)
                                        .unitKey("bytes")
                                        .build())
                                .meta(DeliverableMetaRequestDto.builder()
                                        .quotaRequestId(69L)
                                        .campaignId(1L)
                                        .bigOrderId(42L)
                                        .build())
                                .build())
                        .addDeliverable(DeliverableRequestDto.builder()
                                .folderId(folderOne.getId())
                                .resourceId(resource.getId())
                                .delta(DeliverableDeltaDto.builder()
                                        .amount(200L)
                                        .unitKey("bytes")
                                        .build())
                                .meta(DeliverableMetaRequestDto.builder()
                                        .quotaRequestId(69L)
                                        .campaignId(1L)
                                        .bigOrderId(43L)
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(DeliveryResponseDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.getDeliverables().size());
        Assertions.assertEquals(2L,
                result.getDeliverables().stream().filter(d -> d.getServiceId().isEmpty()).count());
        Assertions.assertEquals(2L,
                result.getDeliverables().stream().filter(d -> d.getFolderId().get().equals(folderOne.getId())).count());
        Assertions.assertEquals(2L, result.getDeliverables().stream()
                .filter(d -> d.getResourceId().equals(resource.getId())).count());
        Assertions.assertEquals(2L,
                result.getDeliverables().stream().filter(d -> d.getMeta().getQuotaRequestId() == 69L).count());
        Assertions.assertEquals(2L,
                result.getDeliverables().stream().filter(d -> d.getMeta().getCampaignId() == 1L).count());
        Assertions.assertEquals(1L,
                result.getDeliverables().stream().filter(d -> d.getMeta().getBigOrderId() == 42L).count());
        Assertions.assertEquals(1L,
                result.getDeliverables().stream().filter(d -> d.getMeta().getBigOrderId() == 43L).count());
        List<QuotaModel> quotasOne = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderOne.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertEquals(350, quotasOne.get(0).getQuota());
        Assertions.assertEquals(340, quotasOne.get(0).getBalance());
        List<FolderOperationLogModel> logsOne = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        folderOperationLogDao.getPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folderOne.getId(),
                                null, SortOrderDto.ASC, 100)))
                .block();
        Assertions.assertEquals(2, logsOne.size());
        Assertions.assertEquals(2L, logsOne.stream().filter(log ->
                log.getOperationType().equals(FolderOperationType.QUOTA_DELIVERY)).count());
    }

    @Test
    public void testSuccessfulDeliveryToServiceNoPermissions() {
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
        FolderModel folderOne = defaultFolderModel(12L);
        QuotaModel quotaOne = quotaModel(provider.getId(), resource.getId(), folderOne.getId(), 50, 40);
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
                        .upsertAllRetryable(txSession, List.of(folderOne))))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .upsertAllRetryable(txSession, List.of(quotaOne))))
                .block();
        webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/api/v1/delivery/_deliver")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(DeliveryRequestDto.builder()
                        .deliveryId(UUID.randomUUID().toString())
                        .authorUid("1120000000000010")
                        .addDeliverable(DeliverableRequestDto.builder()
                                .serviceId(12L)
                                .resourceId(resource.getId())
                                .delta(DeliverableDeltaDto.builder()
                                        .amount(100L)
                                        .unitKey("bytes")
                                        .build())
                                .meta(DeliverableMetaRequestDto.builder()
                                        .quotaRequestId(69L)
                                        .campaignId(1L)
                                        .bigOrderId(42L)
                                        .build())
                                .build())
                        .addDeliverable(DeliverableRequestDto.builder()
                                .serviceId(12L)
                                .resourceId(resource.getId())
                                .delta(DeliverableDeltaDto.builder()
                                        .amount(200L)
                                        .unitKey("bytes")
                                        .build())
                                .meta(DeliverableMetaRequestDto.builder()
                                        .quotaRequestId(69L)
                                        .campaignId(1L)
                                        .bigOrderId(43L)
                                        .build())
                                .build())
                        .build())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void testSuccessfulDeliveryToServiceRepeat() {
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
        FolderModel folderOne = defaultFolderModel(12L);
        QuotaModel quotaOne = quotaModel(provider.getId(), resource.getId(), folderOne.getId(), 50, 40);
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
                        .upsertAllRetryable(txSession, List.of(folderOne))))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .upsertAllRetryable(txSession, List.of(quotaOne))))
                .block();
        DeliveryRequestDto requestBody = DeliveryRequestDto.builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid("1120000000000010")
                .addDeliverable(DeliverableRequestDto.builder()
                        .serviceId(12L)
                        .resourceId(resource.getId())
                        .delta(DeliverableDeltaDto.builder()
                                .amount(100L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(69L)
                                .campaignId(1L)
                                .bigOrderId(42L)
                                .build())
                        .build())
                .addDeliverable(DeliverableRequestDto.builder()
                        .serviceId(12L)
                        .resourceId(resource.getId())
                        .delta(DeliverableDeltaDto.builder()
                                .amount(200L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(69L)
                                .campaignId(1L)
                                .bigOrderId(43L)
                                .build())
                        .build())
                .build();
        DeliveryResponseDto result = webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_deliver")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(DeliveryResponseDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.getDeliverables().size());
        Assertions.assertEquals(2L,
                result.getDeliverables().stream().filter(d -> d.getServiceId().get().equals(12L)).count());
        Assertions.assertEquals(2L,
                result.getDeliverables().stream().filter(d -> d.getFolderId().isEmpty()).count());
        Assertions.assertEquals(2L, result.getDeliverables().stream()
                .filter(d -> d.getResourceId().equals(resource.getId())).count());
        Assertions.assertEquals(2L,
                result.getDeliverables().stream().filter(d -> d.getMeta().getQuotaRequestId() == 69L).count());
        Assertions.assertEquals(2L,
                result.getDeliverables().stream().filter(d -> d.getMeta().getCampaignId() == 1L).count());
        Assertions.assertEquals(1L,
                result.getDeliverables().stream().filter(d -> d.getMeta().getBigOrderId() == 42L).count());
        Assertions.assertEquals(1L,
                result.getDeliverables().stream().filter(d -> d.getMeta().getBigOrderId() == 43L).count());
        DeliveryResponseDto resultAgain = webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_deliver")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(DeliveryResponseDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(resultAgain);
        Assertions.assertEquals(2, resultAgain.getDeliverables().size());
        Assertions.assertEquals(2L,
                resultAgain.getDeliverables().stream().filter(d -> d.getServiceId().get().equals(12L)).count());
        Assertions.assertEquals(2L,
                resultAgain.getDeliverables().stream().filter(d -> d.getFolderId().isEmpty()).count());
        Assertions.assertEquals(2L, resultAgain.getDeliverables().stream()
                .filter(d -> d.getResourceId().equals(resource.getId())).count());
        Assertions.assertEquals(2L,
                resultAgain.getDeliverables().stream().filter(d -> d.getMeta().getQuotaRequestId() == 69L).count());
        Assertions.assertEquals(2L,
                resultAgain.getDeliverables().stream().filter(d -> d.getMeta().getCampaignId() == 1L).count());
        Assertions.assertEquals(1L,
                resultAgain.getDeliverables().stream().filter(d -> d.getMeta().getBigOrderId() == 42L).count());
        Assertions.assertEquals(1L,
                resultAgain.getDeliverables().stream().filter(d -> d.getMeta().getBigOrderId() == 43L).count());
        List<QuotaModel> quotasOne = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .getByFolders(txSession, List.of(folderOne.getId()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertEquals(350, quotasOne.get(0).getQuota());
        Assertions.assertEquals(340, quotasOne.get(0).getBalance());
        List<FolderOperationLogModel> logsOne = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        folderOperationLogDao.getPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folderOne.getId(),
                                null, SortOrderDto.ASC, 100)))
                .block();
        Assertions.assertEquals(2, logsOne.size());
        Assertions.assertEquals(2L, logsOne.stream().filter(log ->
                log.getOperationType().equals(FolderOperationType.QUOTA_DELIVERY)).count());
    }

    @Test
    public void testDeliveryToClosingService() {
        DeliveryRequestDto body = DeliveryRequestDto.builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid("1120000000000010")
                .addDeliverable(DeliverableRequestDto.builder()
                        .serviceId(13L)
                        .resourceId(TestResources.YP_SSD_VLA)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(100L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(69L)
                                .campaignId(1L)
                                .bigOrderId(42L)
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_deliver")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("deliverables.0.serviceId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Current service status is not allowed."));
                });
    }

    @Test
    public void testDeliveryToNonExportableService() {
        DeliveryRequestDto body = DeliveryRequestDto.builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid("1120000000000010")
                .addDeliverable(DeliverableRequestDto.builder()
                        .serviceId(15L)
                        .resourceId(TestResources.YP_SSD_VLA)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(100L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(69L)
                                .campaignId(1L)
                                .bigOrderId(42L)
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_deliver")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("deliverables.0.serviceId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Services in the sandbox are not allowed."));
                });
    }

    @Test
    public void testDeliveryToRenamingService() {
        DeliveryRequestDto body = DeliveryRequestDto.builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid("1120000000000010")
                .addDeliverable(DeliverableRequestDto.builder()
                        .serviceId(16L)
                        .resourceId(TestResources.YP_SSD_VLA)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(100L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(69L)
                                .campaignId(1L)
                                .bigOrderId(42L)
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_deliver")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk();
    }

    private static ProviderModel providerModel(String grpcUri, String restUri, boolean accountsSpacesSupported) {
        return providerModel(grpcUri, restUri, accountsSpacesSupported, 69L);
    }

    private static ProviderModel providerModel(String grpcUri, String restUri, boolean accountsSpacesSupported,
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
                .accountsSpacesSupported(accountsSpacesSupported)
                .syncEnabled(true)
                .grpcTlsOn(true)
                .build();
    }

    private static ResourceTypeModel resourceTypeModel(String providerId, String key, String unitsEnsembleId) {
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

    private static ResourceSegmentationModel resourceSegmentationModel(String providerId, String key) {
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

    private static ResourceSegmentModel resourceSegmentModel(String segmentationId, String key) {
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
    private static ResourceModel resourceModel(String providerId, String key, String resourceTypeId,
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

    private static FolderModel folderModel(long serviceId) {
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

    private static FolderModel defaultFolderModel(long serviceId) {
        return FolderModel.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setServiceId(serviceId)
                .setVersion(0L)
                .setDisplayName("default")
                .setDescription("default")
                .setDeleted(false)
                .setFolderType(FolderType.COMMON_DEFAULT_FOR_SERVICE)
                .setTags(Collections.emptySet())
                .setNextOpLogOrder(1L)
                .build();
    }

    private static QuotaModel quotaModel(String providerId, String resourceId, String folderId, long quota,
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
