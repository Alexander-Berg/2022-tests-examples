package ru.yandex.intranet.d.web.api.delivery

import com.yandex.ydb.table.transaction.TransactionMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.EntityExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.util.function.Tuple2
import reactor.util.function.Tuples
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestUsers.*
import ru.yandex.intranet.d.UnitIds.*
import ru.yandex.intranet.d.UnitsEnsembleIds.CPU_UNITS_ID
import ru.yandex.intranet.d.UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID
import ru.yandex.intranet.d.backend.service.provider_proto.*
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.accounts.*
import ru.yandex.intranet.d.dao.delivery.DeliveriesAndProvidedRequestsDao
import ru.yandex.intranet.d.dao.folders.FolderDao
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao
import ru.yandex.intranet.d.dao.providers.ProvidersDao
import ru.yandex.intranet.d.dao.quotas.QuotasDao
import ru.yandex.intranet.d.dao.resources.ResourcesDao
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao
import ru.yandex.intranet.d.datasource.model.YdbSession
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.datasource.model.YdbTxSession
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService
import ru.yandex.intranet.d.model.WithTenant
import ru.yandex.intranet.d.model.accounts.*
import ru.yandex.intranet.d.model.delivery.DeliverableMetaHistoryModel
import ru.yandex.intranet.d.model.folders.*
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel
import ru.yandex.intranet.d.model.providers.ProviderModel
import ru.yandex.intranet.d.model.quotas.QuotaModel
import ru.yandex.intranet.d.model.resources.ResourceModel
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel
import ru.yandex.intranet.d.utils.DummyModels
import ru.yandex.intranet.d.utils.failOnNull
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.AmountDto
import ru.yandex.intranet.d.web.model.ErrorCollectionDto
import ru.yandex.intranet.d.web.model.SortOrderDto
import ru.yandex.intranet.d.web.model.delivery.DeliverableDeltaDto
import ru.yandex.intranet.d.web.model.delivery.DeliverableFolderOperationDto
import ru.yandex.intranet.d.web.model.delivery.DeliverableMetaRequestDto
import ru.yandex.intranet.d.web.model.delivery.provide.DeliveryAndProvideDestinationDto
import ru.yandex.intranet.d.web.model.delivery.provide.DeliveryAndProvideMetaResponseDto
import ru.yandex.intranet.d.web.model.delivery.provide.DeliveryAndProvideRequestDto
import ru.yandex.intranet.d.web.model.delivery.status.DeliveryStatusDto
import ru.yandex.intranet.d.web.model.delivery.status.DeliveryStatusRequestedQuotasDto
import ru.yandex.intranet.d.web.model.operations.OperationStatusDto
import java.time.Instant
import java.util.*
import java.util.stream.Collectors

@IntegrationTest
class DeliveryAndProvideApiTest(
    @Autowired private val webClient: WebTestClient,
    @Autowired private val deliveriesDao: DeliveriesAndProvidedRequestsDao,
    @Autowired private val providersDao: ProvidersDao,
    @Autowired private val accountsDao: AccountsDao,
    @Autowired private val folderOperationLogDao: FolderOperationLogDao,
    @Autowired private val folderDao: FolderDao,
    @Autowired private val accountsQuotasOperationsDao: AccountsQuotasOperationsDao,
    @Autowired private val operationsInProgressDao: OperationsInProgressDao,
    @Autowired private val resourceTypesDao: ResourceTypesDao,
    @Autowired private val resourceSegmentationsDao: ResourceSegmentationsDao,
    @Autowired private val resourceSegmentsDao: ResourceSegmentsDao,
    @Autowired private val resourcesDao: ResourcesDao,
    @Autowired private val quotasDao: QuotasDao,
    @Autowired private val accountsQuotasDao: AccountsQuotasDao,
    @Autowired private val stubProviderService: StubProviderService,
    @Autowired private val accountsSpacesDao: AccountsSpacesDao,
    @Autowired private val tableClient: YdbTableClient,
    @Value("\${hardwareOrderService.tvmSourceId}") private val dispenserTvmSourceId: Long
) {
    companion object {
        private const val TEST_DELIVERY_ID = "1421ae7c-9b76-44bc-87c7-e18d998778b3"
        private const val GRPC_URI = "in-process:test"
    }

    @Test
    fun testReturnDeliveryWithIdempotency() {
        val deliveryO = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            deliveriesDao.getById(
                session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                TEST_DELIVERY_ID,
                Tenants.DEFAULT_TENANT_ID
            )
        }
            .block()
        Assertions.assertNotNull(deliveryO)
        Assertions.assertTrue(deliveryO!!.isPresent)
        Assertions.assertNotNull(deliveryO.get().request)
        Assertions.assertNotNull(deliveryO.get().request.deliverables)
        Assertions.assertEquals(1, deliveryO.get().request.deliverables.size)
        val (deliveryId, authorUid, deliverables) = deliveryO.get().request
        val (serviceId, providerId, folderId, accountId, resourceId, delta, meta) = deliverables.elementAt(0)
        val body = DeliveryAndProvideRequestDto.Builder()
            .deliveryId(deliveryId)
            .authorUid(authorUid)
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(serviceId)
                    .providerId(providerId)
                    .folderId(folderId)
                    .accountId(accountId)
                    .resourceId(resourceId)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(delta.amount)
                            .unitKey(delta.unitKey)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(meta.quotaRequestId)
                            .campaignId(meta.campaignId)
                            .bigOrderId(meta.bigOrderId)
                            .build()
                    )
                    .build()
            )
            .build()
        webClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .post()
            .uri("/api/v1/delivery/_provide")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.OK)
            .expectBody(DeliveryStatusDto::class.java)
            .consumeWith { result: EntityExchangeResult<DeliveryStatusDto> ->
                Assertions.assertNotNull(result)
                Assertions.assertNotNull(result.responseBody)
                Assertions.assertEquals(TEST_DELIVERY_ID, result.responseBody!!.deliveryId)
            }
    }

    @Test
    fun testWrongRequestWithIdempotency() {
        val deliveryO = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            deliveriesDao.getById(
                session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                TEST_DELIVERY_ID,
                Tenants.DEFAULT_TENANT_ID
            )
        }
            .block()
        Assertions.assertNotNull(deliveryO)
        Assertions.assertTrue(deliveryO!!.isPresent)
        Assertions.assertNotNull(deliveryO.get().request)
        Assertions.assertNotNull(deliveryO.get().request.deliverables)
        Assertions.assertEquals(1, deliveryO.get().request.deliverables.size)
        val (deliveryId, _, deliverables) = deliveryO.get().request
        val (serviceId, providerId, folderId, accountId, resourceId, delta, meta) = deliverables.elementAt(0)
        val body = DeliveryAndProvideRequestDto.Builder()
            .deliveryId(deliveryId)
            .authorUid("Not correct author uid")
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(serviceId)
                    .providerId(providerId)
                    .folderId(folderId)
                    .accountId(accountId)
                    .resourceId(resourceId)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(delta.amount)
                            .unitKey(delta.unitKey)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(meta.quotaRequestId)
                            .campaignId(meta.campaignId)
                            .bigOrderId(meta.bigOrderId)
                            .build()
                    )
                    .build()
            )
            .build()
        webClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .post()
            .uri("/api/v1/delivery/_provide")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
            .expectBody(ErrorCollectionDto::class.java)
            .consumeWith { result: EntityExchangeResult<ErrorCollectionDto> ->
                Assertions.assertNotNull(result)
                val errorCollection = result.responseBody
                Assertions.assertNotNull(errorCollection)
                Assertions.assertNotNull(errorCollection!!.errors)
                Assertions.assertTrue(errorCollection.errors.contains("Delivery request mismatch."))
            }
    }

    @Test
    fun testSuccessDeliverAndProvideWithoutAccountSpace() {
        val provider = providerModel(GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true)
        val firstFolder = folderModel(1L, "First folder", "First folder description")
        val secondFolder = folderModel(1L, "Second folder", "Second folder description")
        val storageResourceType = resourceTypeModel(provider.id, "storage", STORAGE_UNITS_DECIMAL_ID)
        val memoryResourceType = resourceTypeModel(provider.id, "memory", STORAGE_UNITS_DECIMAL_ID)
        val cpuResourceType = resourceTypeModel(provider.id, "cpu", CPU_UNITS_ID)
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val sasSegment = resourceSegmentModel(locationSegmentation.id, "SAS")
        val hddSasResource = resourceModel(
            provider.id,
            "hddSas",
            storageResourceType.id,
            setOf(Tuples.of(locationSegmentation.id, sasSegment.id)),
            STORAGE_UNITS_DECIMAL_ID,
            setOf(
                BYTES,
                KILOBYTES,
                MEGABYTES,
                GIGABYTES
            ),
            GIGABYTES,
            BYTES,
            null
        )
        val hddVlaResource = resourceModel(
            provider.id,
            "hddVla",
            storageResourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            STORAGE_UNITS_DECIMAL_ID,
            setOf(
                BYTES,
                KILOBYTES,
                MEGABYTES,
                GIGABYTES
            ),
            GIGABYTES,
            BYTES,
            null
        )
        val ramSasResource = resourceModel(
            provider.id,
            "ramSas",
            memoryResourceType.id,
            setOf(Tuples.of(locationSegmentation.id, sasSegment.id)),
            STORAGE_UNITS_DECIMAL_ID,
            setOf(
                BYTES,
                KILOBYTES,
                MEGABYTES,
                GIGABYTES
            ),
            GIGABYTES,
            BYTES,
            null
        )
        val ramVlaResource = resourceModel(
            provider.id,
            "ramVla",
            memoryResourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            STORAGE_UNITS_DECIMAL_ID,
            setOf(
                BYTES,
                KILOBYTES,
                MEGABYTES,
                GIGABYTES
            ),
            GIGABYTES,
            BYTES,
            null
        )
        val cpuSasResource = resourceModel(
            provider.id,
            "cpuSas",
            cpuResourceType.id,
            setOf(Tuples.of(locationSegmentation.id, sasSegment.id)),
            CPU_UNITS_ID,
            setOf(
                MILLICORES,
                CORES
            ),
            CORES,
            MILLICORES,
            null
        )
        val cpuVlaResource = resourceModel(
            provider.id,
            "cpuVla",
            cpuResourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            CPU_UNITS_ID,
            setOf(
                MILLICORES,
                CORES
            ),
            CORES,
            MILLICORES,
            null
        )
        val hddSasQuotaFF = quotaModel(provider.id, hddSasResource.id, firstFolder.id, 300L, 200L, 0L)
        val hddVlaQuotaFF = quotaModel(provider.id, hddVlaResource.id, firstFolder.id, 100L, 90L, 0L)
        val ramSasQuotaFF = quotaModel(provider.id, ramSasResource.id, firstFolder.id, 200L, 100L, 0L)
        val ramVlaQuotaFF = quotaModel(provider.id, ramVlaResource.id, firstFolder.id, 200L, 200L, 0L)
        val cpuSasQuotaFF = quotaModel(provider.id, cpuSasResource.id, firstFolder.id, 1000L, 900L, 0L)
        val cpuVlaQuotaFF = quotaModel(provider.id, cpuVlaResource.id, firstFolder.id, 2000L, 2000L, 0L)
        val firstAccountFF = accountModel(
            provider.id, null, "first-account-first-folder",
            "first-account-first-folder", firstFolder.id, "first-account-first-folder", null, null
        )
        val secondAccountFF = accountModel(
            provider.id, null, "second-account-first-folder",
            "second-account-first-folder", firstFolder.id, "second-account-first-folder", null, null
        )
        val accountSF = accountModel(
            provider.id, null, "account-second-folder",
            "account-second-folder", secondFolder.id, "account-second-folder", null, null
        )
        val hddSasProvisionFFFA = accountQuotaModel(
            provider.id, hddSasResource.id, firstFolder.id, firstAccountFF.id,
            50L, 50L
        )
        val hddSasProvisionFFSA = accountQuotaModel(
            provider.id, hddSasResource.id, firstFolder.id, secondAccountFF.id,
            50L, 10L
        )
        val hddVlaProvisionFFFA = accountQuotaModel(
            provider.id, hddVlaResource.id, firstFolder.id, firstAccountFF.id,
            10L, 0L
        )
        val hddVlaProvisionFFSA = accountQuotaModel(
            provider.id, hddVlaResource.id, firstFolder.id, secondAccountFF.id,
            0L, 0L
        )
        val ramSasProvisionFFFA = accountQuotaModel(
            provider.id, ramSasResource.id, firstFolder.id, firstAccountFF.id,
            100L, 0L
        )
        val cpuSasProvisionFFFA = accountQuotaModel(
            provider.id, cpuSasResource.id, firstFolder.id, secondAccountFF.id,
            100L, 0L
        )
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertAllRetryable(txSession, listOf(firstFolder, secondFolder))
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypesRetryable(
                    txSession,
                    listOf(storageResourceType, memoryResourceType, cpuResourceType)
                )
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession, listOf(vlaSegment, sasSegment))
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourcesRetryable(
                    txSession, listOf(
                        hddSasResource, hddVlaResource, ramSasResource,
                        ramVlaResource, cpuSasResource, cpuVlaResource
                    )
                )
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertAllRetryable(txSession, listOf(firstAccountFF, secondAccountFF, accountSF))
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertAllRetryable(
                    txSession, listOf(
                        hddSasQuotaFF, hddVlaQuotaFF, ramSasQuotaFF, ramVlaQuotaFF, cpuSasQuotaFF, cpuVlaQuotaFF
                    )
                )
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertAllRetryable(
                    txSession, listOf(
                        hddSasProvisionFFFA, hddSasProvisionFFSA, hddVlaProvisionFFFA, hddVlaProvisionFFSA,
                        ramSasProvisionFFFA, cpuSasProvisionFFFA
                    )
                )
            }
        }.block()
        val requestId = 143L
        val campaignId = 40L
        val bigOrderId = 44L
        val body = DeliveryAndProvideRequestDto.Builder()
            .deliveryId(UUID.randomUUID().toString())
            .authorUid(DISPENSER_QUOTA_MANAGER_UID)
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(firstAccountFF.id)
                    .resourceId(hddSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(400)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(firstAccountFF.id)
                    .resourceId(hddVlaResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(500)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(firstAccountFF.id)
                    .resourceId(ramSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(1000)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(firstAccountFF.id)
                    .resourceId(ramVlaResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(2000)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(firstAccountFF.id)
                    .resourceId(cpuSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(2000)
                            .unitKey(MILLICORES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(firstAccountFF.id)
                    .resourceId(cpuVlaResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(3000)
                            .unitKey(MILLICORES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(secondAccountFF.id)
                    .resourceId(hddSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(1400)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(secondAccountFF.id)
                    .resourceId(hddVlaResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(1500)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(secondAccountFF.id)
                    .resourceId(ramSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(2000)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(secondAccountFF.id)
                    .resourceId(ramVlaResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(3000)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(secondFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(secondFolder.id)
                    .accountId(accountSF.id)
                    .resourceId(hddSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(400)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(secondFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(secondFolder.id)
                    .accountId(accountSF.id)
                    .resourceId(hddVlaResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(500)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(secondFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(secondFolder.id)
                    .accountId(accountSF.id)
                    .resourceId(ramSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(1000)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(secondFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(secondFolder.id)
                    .accountId(accountSF.id)
                    .resourceId(ramVlaResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(2000)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(secondFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(secondFolder.id)
                    .accountId(accountSF.id)
                    .resourceId(cpuSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(2000)
                            .unitKey(MILLICORES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(secondFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(secondFolder.id)
                    .accountId(accountSF.id)
                    .resourceId(cpuVlaResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(3000)
                            .unitKey(MILLICORES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .build()
        stubProviderService.setUpdateProvisionResponseByMappingFunction(true)
        stubProviderService.setUpdateProvisionRequestToKeyFunction { r: UpdateProvisionRequest? -> r!!.accountId }
        stubProviderService.addAllToUpdateProvisionResponsesMap(
            mapOf(
                firstAccountFF.outerAccountIdInProvider to
                    GrpcResponse.success(
                        UpdateProvisionResponse.newBuilder()
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("storage")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("SAS")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(450L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(50L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("storage")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("VLA")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(510L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("memory")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("SAS")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(1100L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("memory")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("VLA")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(2000L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("cpu")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("SAS")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(2000L).setUnitKey("millicores").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("millicores").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("cpu")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("VLA")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(3000L).setUnitKey("millicores").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("millicores").build()
                                ).build()
                            )
                            .build()
                    ),
                secondAccountFF.outerAccountIdInProvider to
                    GrpcResponse.success(
                        UpdateProvisionResponse.newBuilder()
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("storage")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("SAS")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(1450L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(10L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("storage")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("VLA")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(1500L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("memory")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("SAS")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(2000L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("memory")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("VLA")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(3000L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .build()
                    ),
                accountSF.outerAccountIdInProvider to
                    GrpcResponse.success(
                        UpdateProvisionResponse.newBuilder()
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("storage")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("SAS")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(400L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("storage")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("VLA")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(500L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("memory")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("SAS")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(1000L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("memory")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("VLA")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(2000L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("cpu")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("SAS")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(2000L).setUnitKey("millicores").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("millicores").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("cpu")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("VLA")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(3000L).setUnitKey("millicores").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("millicores").build()
                                ).build()
                            )
                            .build()
                    ),
            )
        )

        val responseBody = webClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .post()
            .uri("/api/v1/delivery/_provide")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.OK)
            .expectBody(DeliveryStatusDto::class.java)
            .returnResult()
            .responseBody

        responseBody ?: failOnNull()
        Assertions.assertEquals(body.deliveryId, responseBody.deliveryId)
        val operations = responseBody.operations
        Assertions.assertEquals(3, operations.size)
        operations.forEach { Assertions.assertEquals(OperationStatusDto.SUCCESS, it.status) }
        Assertions.assertEquals(3, stubProviderService.updateProvisionCallCount)
        val updateProvisionRequests = stubProviderService.updateProvisionRequests
        Assertions.assertEquals(3, updateProvisionRequests.size)
        val updateProvisionByAccountId = updateProvisionRequests.associateBy { it.t1.accountId }
        val firstAccountUpdateRequest = updateProvisionByAccountId[firstAccountFF.outerAccountIdInProvider]!!.t1
        Assertions.assertEquals(firstFolder.id, firstAccountUpdateRequest.folderId)
        Assertions.assertEquals(firstFolder.serviceId, firstAccountUpdateRequest.abcServiceId)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, firstAccountUpdateRequest.author.passportUid.passportUid)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_LOGIN, firstAccountUpdateRequest.author.staffLogin.staffLogin)
        Assertions.assertEquals(mapOf(
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(storageResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(sasSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(450L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(storageResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(vlaSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(510L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(memoryResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(sasSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(1100L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(memoryResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(vlaSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(2000L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(cpuResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(sasSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(2000L)
                .setUnitKey(MILLICORES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(cpuResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(vlaSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(3000L)
                .setUnitKey(MILLICORES_KEY)
                .build()
        ), firstAccountUpdateRequest.updatedProvisionsList.associate { it.resourceKey to it.provided })
        Assertions.assertEquals(mapOf(
            firstAccountFF.outerAccountIdInProvider to
                mapOf(
                    ResourceKey.newBuilder()
                        .setCompoundKey(
                            CompoundResourceKey.newBuilder()
                                .setResourceTypeKey(storageResourceType.key)
                                .addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey(locationSegmentation.key)
                                        .setResourceSegmentKey(sasSegment.key)
                                        .build()
                                )
                                .build()
                        ).build() to Amount.newBuilder()
                        .setValue(50L)
                        .setUnitKey(BYTES_KEY)
                        .build(),
                    ResourceKey.newBuilder()
                        .setCompoundKey(
                            CompoundResourceKey.newBuilder()
                                .setResourceTypeKey(storageResourceType.key)
                                .addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey(locationSegmentation.key)
                                        .setResourceSegmentKey(vlaSegment.key)
                                        .build()
                                )
                                .build()
                        ).build() to Amount.newBuilder()
                        .setValue(10L)
                        .setUnitKey(BYTES_KEY)
                        .build(),
                    ResourceKey.newBuilder()
                        .setCompoundKey(
                            CompoundResourceKey.newBuilder()
                                .setResourceTypeKey(memoryResourceType.key)
                                .addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey(locationSegmentation.key)
                                        .setResourceSegmentKey(sasSegment.key)
                                        .build()
                                )
                                .build()
                        ).build() to Amount.newBuilder()
                        .setValue(100L)
                        .setUnitKey(BYTES_KEY)
                        .build(),
                    ResourceKey.newBuilder()
                        .setCompoundKey(
                            CompoundResourceKey.newBuilder()
                                .setResourceTypeKey(memoryResourceType.key)
                                .addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey(locationSegmentation.key)
                                        .setResourceSegmentKey(vlaSegment.key)
                                        .build()
                                )
                                .build()
                        ).build() to Amount.newBuilder()
                        .setValue(0L)
                        .setUnitKey(BYTES_KEY)
                        .build(),
                    ResourceKey.newBuilder()
                        .setCompoundKey(
                            CompoundResourceKey.newBuilder()
                                .setResourceTypeKey(cpuResourceType.key)
                                .addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey(locationSegmentation.key)
                                        .setResourceSegmentKey(sasSegment.key)
                                        .build()
                                )
                                .build()
                        ).build() to Amount.newBuilder()
                        .setValue(0L)
                        .setUnitKey(MILLICORES_KEY)
                        .build(),
                    ResourceKey.newBuilder()
                        .setCompoundKey(
                            CompoundResourceKey.newBuilder()
                                .setResourceTypeKey(cpuResourceType.key)
                                .addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey(locationSegmentation.key)
                                        .setResourceSegmentKey(vlaSegment.key)
                                        .build()
                                )
                                .build()
                        ).build() to Amount.newBuilder()
                        .setValue(0L)
                        .setUnitKey(MILLICORES_KEY)
                        .build()
                ),
            secondAccountFF.outerAccountIdInProvider to mapOf(
                ResourceKey.newBuilder()
                    .setCompoundKey(
                        CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(storageResourceType.key)
                            .addResourceSegmentKeys(
                                ResourceSegmentKey.newBuilder()
                                    .setResourceSegmentationKey(locationSegmentation.key)
                                    .setResourceSegmentKey(sasSegment.key)
                                    .build()
                            )
                            .build()
                    ).build() to Amount.newBuilder()
                    .setValue(50L)
                    .setUnitKey(BYTES_KEY)
                    .build(),
                ResourceKey.newBuilder()
                    .setCompoundKey(
                        CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(storageResourceType.key)
                            .addResourceSegmentKeys(
                                ResourceSegmentKey.newBuilder()
                                    .setResourceSegmentationKey(locationSegmentation.key)
                                    .setResourceSegmentKey(vlaSegment.key)
                                    .build()
                            )
                            .build()
                    ).build() to Amount.newBuilder()
                    .setValue(0L)
                    .setUnitKey(BYTES_KEY)
                    .build(),
                ResourceKey.newBuilder()
                    .setCompoundKey(
                        CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(memoryResourceType.key)
                            .addResourceSegmentKeys(
                                ResourceSegmentKey.newBuilder()
                                    .setResourceSegmentationKey(locationSegmentation.key)
                                    .setResourceSegmentKey(sasSegment.key)
                                    .build()
                            )
                            .build()
                    ).build() to Amount.newBuilder()
                    .setValue(0L)
                    .setUnitKey(BYTES_KEY)
                    .build(),
                ResourceKey.newBuilder()
                    .setCompoundKey(
                        CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(memoryResourceType.key)
                            .addResourceSegmentKeys(
                                ResourceSegmentKey.newBuilder()
                                    .setResourceSegmentationKey(locationSegmentation.key)
                                    .setResourceSegmentKey(vlaSegment.key)
                                    .build()
                            )
                            .build()
                    ).build() to Amount.newBuilder()
                    .setValue(0L)
                    .setUnitKey(BYTES_KEY)
                    .build(),
                ResourceKey.newBuilder()
                    .setCompoundKey(
                        CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(cpuResourceType.key)
                            .addResourceSegmentKeys(
                                ResourceSegmentKey.newBuilder()
                                    .setResourceSegmentationKey(locationSegmentation.key)
                                    .setResourceSegmentKey(sasSegment.key)
                                    .build()
                            )
                            .build()
                    ).build() to Amount.newBuilder()
                    .setValue(100L)
                    .setUnitKey(MILLICORES_KEY)
                    .build(),
                ResourceKey.newBuilder()
                    .setCompoundKey(
                        CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(cpuResourceType.key)
                            .addResourceSegmentKeys(
                                ResourceSegmentKey.newBuilder()
                                    .setResourceSegmentationKey(locationSegmentation.key)
                                    .setResourceSegmentKey(vlaSegment.key)
                                    .build()
                            )
                            .build()
                    ).build() to Amount.newBuilder()
                    .setValue(0L)
                    .setUnitKey(MILLICORES_KEY)
                    .build()
            )
        ), firstAccountUpdateRequest.knownProvisionsList.associate {
            it.accountId to
                it.knownProvisionsList.associate { provision -> provision.resourceKey to provision.provided }
        })
        Assertions.assertEquals(firstAccountFF.providerId, firstAccountUpdateRequest.providerId)
        Assertions.assertFalse(firstAccountUpdateRequest.accountsSpaceKey.hasCompoundKey())

        val secondAccountUpdateRequest = updateProvisionByAccountId[secondAccountFF.outerAccountIdInProvider]!!.t1
        Assertions.assertEquals(firstFolder.id, firstAccountUpdateRequest.folderId)
        Assertions.assertEquals(firstFolder.serviceId, secondAccountUpdateRequest.abcServiceId)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, secondAccountUpdateRequest.author.passportUid.passportUid)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_LOGIN, secondAccountUpdateRequest.author.staffLogin.staffLogin)
        Assertions.assertEquals(mapOf(
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(storageResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(sasSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(1450L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(storageResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(vlaSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(1500L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(memoryResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(sasSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(2000L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(memoryResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(vlaSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(3000L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(cpuResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(sasSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(100L)
                .setUnitKey(MILLICORES_KEY)
                .build()
        ), secondAccountUpdateRequest.updatedProvisionsList.associate { it.resourceKey to it.provided })
        Assertions.assertEquals(mapOf(
            firstAccountFF.outerAccountIdInProvider to
                mapOf(
                    ResourceKey.newBuilder()
                        .setCompoundKey(
                            CompoundResourceKey.newBuilder()
                                .setResourceTypeKey(storageResourceType.key)
                                .addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey(locationSegmentation.key)
                                        .setResourceSegmentKey(sasSegment.key)
                                        .build()
                                )
                                .build()
                        ).build() to Amount.newBuilder()
                        .setValue(450L)
                        .setUnitKey(BYTES_KEY)
                        .build(),
                    ResourceKey.newBuilder()
                        .setCompoundKey(
                            CompoundResourceKey.newBuilder()
                                .setResourceTypeKey(storageResourceType.key)
                                .addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey(locationSegmentation.key)
                                        .setResourceSegmentKey(vlaSegment.key)
                                        .build()
                                )
                                .build()
                        ).build() to Amount.newBuilder()
                        .setValue(510L)
                        .setUnitKey(BYTES_KEY)
                        .build(),
                    ResourceKey.newBuilder()
                        .setCompoundKey(
                            CompoundResourceKey.newBuilder()
                                .setResourceTypeKey(memoryResourceType.key)
                                .addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey(locationSegmentation.key)
                                        .setResourceSegmentKey(sasSegment.key)
                                        .build()
                                )
                                .build()
                        ).build() to Amount.newBuilder()
                        .setValue(1100L)
                        .setUnitKey(BYTES_KEY)
                        .build(),
                    ResourceKey.newBuilder()
                        .setCompoundKey(
                            CompoundResourceKey.newBuilder()
                                .setResourceTypeKey(memoryResourceType.key)
                                .addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey(locationSegmentation.key)
                                        .setResourceSegmentKey(vlaSegment.key)
                                        .build()
                                )
                                .build()
                        ).build() to Amount.newBuilder()
                        .setValue(2000L)
                        .setUnitKey(BYTES_KEY)
                        .build(),
                    ResourceKey.newBuilder()
                        .setCompoundKey(
                            CompoundResourceKey.newBuilder()
                                .setResourceTypeKey(cpuResourceType.key)
                                .addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey(locationSegmentation.key)
                                        .setResourceSegmentKey(sasSegment.key)
                                        .build()
                                )
                                .build()
                        ).build() to Amount.newBuilder()
                        .setValue(2000L)
                        .setUnitKey(MILLICORES_KEY)
                        .build(),
                    ResourceKey.newBuilder()
                        .setCompoundKey(
                            CompoundResourceKey.newBuilder()
                                .setResourceTypeKey(cpuResourceType.key)
                                .addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey(locationSegmentation.key)
                                        .setResourceSegmentKey(vlaSegment.key)
                                        .build()
                                )
                                .build()
                        ).build() to Amount.newBuilder()
                        .setValue(3000L)
                        .setUnitKey(MILLICORES_KEY)
                        .build()
                ),
            secondAccountFF.outerAccountIdInProvider to mapOf(
                ResourceKey.newBuilder()
                    .setCompoundKey(
                        CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(storageResourceType.key)
                            .addResourceSegmentKeys(
                                ResourceSegmentKey.newBuilder()
                                    .setResourceSegmentationKey(locationSegmentation.key)
                                    .setResourceSegmentKey(sasSegment.key)
                                    .build()
                            )
                            .build()
                    ).build() to Amount.newBuilder()
                    .setValue(50L)
                    .setUnitKey(BYTES_KEY)
                    .build(),
                ResourceKey.newBuilder()
                    .setCompoundKey(
                        CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(storageResourceType.key)
                            .addResourceSegmentKeys(
                                ResourceSegmentKey.newBuilder()
                                    .setResourceSegmentationKey(locationSegmentation.key)
                                    .setResourceSegmentKey(vlaSegment.key)
                                    .build()
                            )
                            .build()
                    ).build() to Amount.newBuilder()
                    .setValue(0L)
                    .setUnitKey(BYTES_KEY)
                    .build(),
                ResourceKey.newBuilder()
                    .setCompoundKey(
                        CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(memoryResourceType.key)
                            .addResourceSegmentKeys(
                                ResourceSegmentKey.newBuilder()
                                    .setResourceSegmentationKey(locationSegmentation.key)
                                    .setResourceSegmentKey(sasSegment.key)
                                    .build()
                            )
                            .build()
                    ).build() to Amount.newBuilder()
                    .setValue(0L)
                    .setUnitKey(BYTES_KEY)
                    .build(),
                ResourceKey.newBuilder()
                    .setCompoundKey(
                        CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(memoryResourceType.key)
                            .addResourceSegmentKeys(
                                ResourceSegmentKey.newBuilder()
                                    .setResourceSegmentationKey(locationSegmentation.key)
                                    .setResourceSegmentKey(vlaSegment.key)
                                    .build()
                            )
                            .build()
                    ).build() to Amount.newBuilder()
                    .setValue(0L)
                    .setUnitKey(BYTES_KEY)
                    .build(),
                ResourceKey.newBuilder()
                    .setCompoundKey(
                        CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(cpuResourceType.key)
                            .addResourceSegmentKeys(
                                ResourceSegmentKey.newBuilder()
                                    .setResourceSegmentationKey(locationSegmentation.key)
                                    .setResourceSegmentKey(sasSegment.key)
                                    .build()
                            )
                            .build()
                    ).build() to Amount.newBuilder()
                    .setValue(100L)
                    .setUnitKey(MILLICORES_KEY)
                    .build()
            )
        ), secondAccountUpdateRequest.knownProvisionsList.associate {
            it.accountId to
                it.knownProvisionsList.associate { provision -> provision.resourceKey to provision.provided }
        })
        Assertions.assertEquals(secondAccountFF.providerId, secondAccountUpdateRequest.providerId)
        Assertions.assertFalse(secondAccountUpdateRequest.accountsSpaceKey.hasCompoundKey())

        val thirdAccountUpdateRequest = updateProvisionByAccountId[accountSF.outerAccountIdInProvider]!!.t1
        Assertions.assertEquals(secondFolder.id, thirdAccountUpdateRequest.folderId)
        Assertions.assertEquals(secondFolder.serviceId, thirdAccountUpdateRequest.abcServiceId)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, thirdAccountUpdateRequest.author.passportUid.passportUid)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_LOGIN, thirdAccountUpdateRequest.author.staffLogin.staffLogin)
        Assertions.assertEquals(mapOf(
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(storageResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(sasSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(400L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(storageResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(vlaSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(500L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(memoryResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(sasSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(1000L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(memoryResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(vlaSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(2000L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(cpuResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(sasSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(2000L)
                .setUnitKey(MILLICORES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(cpuResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(vlaSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(3000L)
                .setUnitKey(MILLICORES_KEY)
                .build()
        ), thirdAccountUpdateRequest.updatedProvisionsList.associate { it.resourceKey to it.provided })
        Assertions.assertTrue(thirdAccountUpdateRequest.knownProvisionsList.associate {
            it.accountId to
                it.knownProvisionsList.associate { provision -> provision.resourceKey to provision.provided }
        }.isEmpty())
        Assertions.assertEquals(accountSF.providerId, thirdAccountUpdateRequest.providerId)
        Assertions.assertFalse(thirdAccountUpdateRequest.accountsSpaceKey.hasCompoundKey())

        val folderOperationDtoMap = operations.associate { it.accountId!! to it.folderOperationLogs }
        val operationDtos = folderOperationDtoMap.values.stream().flatMap { it!!.stream() }
            .collect(Collectors.toSet())
        Assertions.assertEquals(2, operationDtos.size)
        val folderOperationLogModels = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getByIds(txSession,
                    operationDtos
                        .map {
                            WithTenant(
                                Tenants.DEFAULT_TENANT_ID,
                                FolderOperationLogModel.Identity(it!!.folderId, it.timestamp, it.id)
                            )
                        }
                )
            }
        }.block()
        folderOperationLogModels ?: failOnNull()
        Assertions.assertEquals(2, folderOperationLogModels.size)
        val folderOperationLogModelMap = folderOperationLogModels.associateBy { it.folderId }
        val logModelFF = folderOperationLogModelMap[firstFolder.id]
        logModelFF ?: failOnNull()
        Assertions.assertEquals(FolderOperationType.QUOTA_DELIVERY, logModelFF.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, logModelFF.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, logModelFF.authorUserUid.get())
        Assertions.assertTrue(logModelFF.authorProviderId.isEmpty)
        Assertions.assertTrue(logModelFF.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(logModelFF.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(logModelFF.oldFolderFields.isEmpty)
        Assertions.assertEquals(
            QuotasByResource(
                mapOf(
                    hddSasResource.id to 300L,
                    hddVlaResource.id to 100L,
                    ramSasResource.id to 200L,
                    ramVlaResource.id to 200L,
                    cpuSasResource.id to 1000L,
                    cpuVlaResource.id to 2000L
                )
            ), logModelFF.oldQuotas
        )
        Assertions.assertEquals(QuotasByResource(mapOf()), logModelFF.oldBalance)
        Assertions.assertEquals(QuotasByAccount(mapOf()), logModelFF.oldProvisions)
        Assertions.assertTrue(logModelFF.oldAccounts.isEmpty)
        Assertions.assertTrue(logModelFF.newFolderFields.isEmpty)
        Assertions.assertEquals(
            QuotasByResource(
                mapOf(
                    hddSasResource.id to 300L + 400L + 1400L,
                    hddVlaResource.id to 100L + 500L + 1500L,
                    ramSasResource.id to 200L + 1000L + 2000L,
                    ramVlaResource.id to 200L + 2000L + 3000L,
                    cpuSasResource.id to 1000L + 2000L,
                    cpuVlaResource.id to 2000L + 3000L
                )
            ), logModelFF.newQuotas
        )
        Assertions.assertEquals(QuotasByResource(mapOf()), logModelFF.newBalance)
        Assertions.assertEquals(QuotasByAccount(mapOf()), logModelFF.newProvisions)
        Assertions.assertTrue(logModelFF.actuallyAppliedProvisions.isEmpty)
        Assertions.assertTrue(logModelFF.newAccounts.isEmpty)
        Assertions.assertTrue(logModelFF.accountsQuotasOperationsId.isEmpty)
        Assertions.assertTrue(logModelFF.quotasDemandsId.isEmpty)
        Assertions.assertTrue(logModelFF.operationPhase.isEmpty)
        Assertions.assertTrue(logModelFF.commentId.isEmpty)
        Assertions.assertEquals(2, logModelFF.order)
        Assertions.assertEquals(
            DeliverableMetaHistoryModel.builder()
                .quotaRequestId(requestId)
                .campaignId(campaignId)
                .bigOrderIds(bigOrderId)
                .deliveryId(body.deliveryId)
                .build(), logModelFF.deliveryMeta.get()
        )
        Assertions.assertTrue(logModelFF.transferMeta.isEmpty)
        val logModelSF = folderOperationLogModelMap[secondFolder.id]
        logModelSF ?: failOnNull()
        Assertions.assertEquals(FolderOperationType.QUOTA_DELIVERY, logModelSF.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, logModelSF.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, logModelSF.authorUserUid.get())
        Assertions.assertTrue(logModelSF.authorProviderId.isEmpty)
        Assertions.assertTrue(logModelSF.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(logModelSF.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(logModelSF.oldFolderFields.isEmpty)
        Assertions.assertEquals(
            QuotasByResource(
                mapOf(
                    hddSasResource.id to 0L,
                    hddVlaResource.id to 0L,
                    ramSasResource.id to 0L,
                    ramVlaResource.id to 0L,
                    cpuSasResource.id to 0L,
                    cpuVlaResource.id to 0L
                )
            ), logModelSF.oldQuotas
        )
        Assertions.assertEquals(QuotasByResource(mapOf()), logModelSF.oldBalance)
        Assertions.assertEquals(QuotasByAccount(mapOf()), logModelSF.oldProvisions)
        Assertions.assertTrue(logModelSF.oldAccounts.isEmpty)
        Assertions.assertTrue(logModelSF.newFolderFields.isEmpty)
        Assertions.assertEquals(
            QuotasByResource(
                mapOf(
                    hddSasResource.id to 400L,
                    hddVlaResource.id to 500L,
                    ramSasResource.id to 1000L,
                    ramVlaResource.id to 2000L,
                    cpuSasResource.id to 2000L,
                    cpuVlaResource.id to 3000L
                )
            ), logModelSF.newQuotas
        )
        Assertions.assertEquals(QuotasByResource(mapOf()), logModelSF.newBalance)
        Assertions.assertEquals(QuotasByAccount(mapOf()), logModelSF.newProvisions)
        Assertions.assertTrue(logModelSF.actuallyAppliedProvisions.isEmpty)
        Assertions.assertTrue(logModelSF.newAccounts.isEmpty)
        Assertions.assertTrue(logModelSF.accountsQuotasOperationsId.isEmpty)
        Assertions.assertTrue(logModelSF.quotasDemandsId.isEmpty)
        Assertions.assertTrue(logModelSF.operationPhase.isEmpty)
        Assertions.assertTrue(logModelSF.commentId.isEmpty)
        Assertions.assertEquals(2, logModelSF.order)
        Assertions.assertEquals(
            DeliverableMetaHistoryModel.builder()
                .quotaRequestId(requestId)
                .campaignId(campaignId)
                .bigOrderIds(bigOrderId)
                .deliveryId(body.deliveryId)
                .build(), logModelSF.deliveryMeta.get()
        )
        Assertions.assertTrue(logModelSF.transferMeta.isEmpty)

        val operationsByFolderLogIdentity = operations.stream()
            .flatMap { it.folderOperationLogs!!.stream() }
            .collect(Collectors.groupingBy {
                FolderOperationLogModel.Identity(
                    it.folderId, it.timestamp, it.id
                )
            })
        Assertions.assertEquals(2, operationsByFolderLogIdentity[logModelFF.identity]!!.size)
        Assertions.assertEquals(1, operationsByFolderLogIdentity[logModelSF.identity]!!.size)

        val operationDtoByAccount = operations.associateBy { it.accountId }
        val firstAccountFFOpp = operationDtoByAccount[firstAccountFF.id]
        firstAccountFFOpp ?: failOnNull()
        Assertions.assertEquals(firstAccountFF.providerId, firstAccountFFOpp.providerId)
        firstAccountFFOpp.updateDateTime ?: failOnNull()
        Assertions.assertNull(firstAccountFFOpp.errorMessage)
        Assertions.assertNull(firstAccountFFOpp.errorKind)
        var requestedQuota = listOf(
            DeliveryStatusRequestedQuotasDto(
                hddSasResource.id, AmountDto(
                    "400",
                    "B",
                    "400",
                    "B",
                    "400",
                    BYTES,
                    "400",
                    BYTES
                )
            ),
            DeliveryStatusRequestedQuotasDto(
                hddVlaResource.id, AmountDto(
                    "500",
                    "B",
                    "500",
                    "B",
                    "500",
                    BYTES,
                    "500",
                    BYTES
                )
            ),
            DeliveryStatusRequestedQuotasDto(
                ramSasResource.id, AmountDto(
                    "1",
                    "KB",
                    "1000",
                    "B",
                    "1",
                    KILOBYTES,
                    "1000",
                    BYTES
                )
            ),
            DeliveryStatusRequestedQuotasDto(
                ramVlaResource.id, AmountDto(
                    "2",
                    "KB",
                    "2000",
                    "B",
                    "2",
                    KILOBYTES,
                    "2000",
                    BYTES
                )
            ),
            DeliveryStatusRequestedQuotasDto(
                cpuSasResource.id, AmountDto(
                    "2",
                    "cores",
                    "2000",
                    "mCores",
                    "2",
                    CORES,
                    "2000",
                    MILLICORES
                )
            ),
            DeliveryStatusRequestedQuotasDto(
                cpuVlaResource.id, AmountDto(
                    "3",
                    "cores",
                    "3000",
                    "mCores",
                    "3",
                    CORES,
                    "3000",
                    MILLICORES
                )
            )
        )
        Assertions.assertTrue(requestedQuota.size == firstAccountFFOpp.requestedQuotas.size
            && requestedQuota.containsAll(firstAccountFFOpp.requestedQuotas)
            && firstAccountFFOpp.requestedQuotas.containsAll(requestedQuota))
        Assertions.assertEquals(
            DeliveryAndProvideMetaResponseDto.Builder()
                .quotaRequestId(requestId)
                .campaignId(campaignId)
                .addBigOrderId(bigOrderId)
                .build(), firstAccountFFOpp.meta!!
        )
        Assertions.assertEquals(
            DeliverableFolderOperationDto.builder()
                .folderId(logModelFF.identity.folderId)
                .timestamp(logModelFF.identity.operationDateTime)
                .id(logModelFF.identity.id)
                .build(), firstAccountFFOpp.folderOperationLogs!!.stream().findFirst().orElseThrow()
        )

        val secondAccountFFOpp = operationDtoByAccount[secondAccountFF.id]
        secondAccountFFOpp ?: failOnNull()
        Assertions.assertEquals(firstAccountFF.providerId, secondAccountFFOpp.providerId)
        secondAccountFFOpp.updateDateTime ?: failOnNull()
        Assertions.assertNull(secondAccountFFOpp.errorMessage)
        Assertions.assertNull(secondAccountFFOpp.errorKind)
        requestedQuota = listOf(
            DeliveryStatusRequestedQuotasDto(
                hddSasResource.id, AmountDto(
                    "1.4",
                    "KB",
                    "1400",
                    "B",
                    "1.4",
                    KILOBYTES,
                    "1400",
                    BYTES
                )
            ),
            DeliveryStatusRequestedQuotasDto(
                hddVlaResource.id, AmountDto(
                    "1.5",
                    "KB",
                    "1500",
                    "B",
                    "1.5",
                    KILOBYTES,
                    "1500",
                    BYTES
                )
            ),
            DeliveryStatusRequestedQuotasDto(
                ramSasResource.id, AmountDto(
                    "2",
                    "KB",
                    "2000",
                    "B",
                    "2",
                    KILOBYTES,
                    "2000",
                    BYTES
                )
            ),
            DeliveryStatusRequestedQuotasDto(
                ramVlaResource.id, AmountDto(
                    "3",
                    "KB",
                    "3000",
                    "B",
                    "3",
                    KILOBYTES,
                    "3000",
                    BYTES
                )
            )
        )
        Assertions.assertTrue(requestedQuota.size == secondAccountFFOpp.requestedQuotas.size
            && requestedQuota.containsAll(secondAccountFFOpp.requestedQuotas)
            && secondAccountFFOpp.requestedQuotas.containsAll(requestedQuota))
        Assertions.assertEquals(
            DeliveryAndProvideMetaResponseDto.Builder()
                .quotaRequestId(requestId)
                .campaignId(campaignId)
                .addBigOrderId(bigOrderId)
                .build(), secondAccountFFOpp.meta!!
        )
        Assertions.assertEquals(
            DeliverableFolderOperationDto.builder()
                .folderId(logModelFF.identity.folderId)
                .timestamp(logModelFF.identity.operationDateTime)
                .id(logModelFF.identity.id)
                .build(), secondAccountFFOpp.folderOperationLogs!!.stream().findFirst().orElseThrow()
        )

        val accountSFOpp = operationDtoByAccount[accountSF.id]
        accountSFOpp ?: failOnNull()
        Assertions.assertEquals(accountSF.providerId, accountSFOpp.providerId)
        accountSFOpp.updateDateTime ?: failOnNull()
        Assertions.assertNull(accountSFOpp.errorMessage)
        Assertions.assertNull(accountSFOpp.errorKind)
        requestedQuota = listOf(
            DeliveryStatusRequestedQuotasDto(
                hddSasResource.id, AmountDto(
                    "400",
                    "B",
                    "400",
                    "B",
                    "400",
                    BYTES,
                    "400",
                    BYTES
                )
            ),
            DeliveryStatusRequestedQuotasDto(
                hddVlaResource.id, AmountDto(
                    "500",
                    "B",
                    "500",
                    "B",
                    "500",
                    BYTES,
                    "500",
                    BYTES
                )
            ),
            DeliveryStatusRequestedQuotasDto(
                ramSasResource.id, AmountDto(
                    "1",
                    "KB",
                    "1000",
                    "B",
                    "1",
                    KILOBYTES,
                    "1000",
                    BYTES
                )
            ),
            DeliveryStatusRequestedQuotasDto(
                ramVlaResource.id, AmountDto(
                    "2",
                    "KB",
                    "2000",
                    "B",
                    "2",
                    KILOBYTES,
                    "2000",
                    BYTES
                )
            ),
            DeliveryStatusRequestedQuotasDto(
                cpuSasResource.id, AmountDto(
                    "2",
                    "cores",
                    "2000",
                    "mCores",
                    "2",
                    CORES,
                    "2000",
                    MILLICORES
                )
            ),
            DeliveryStatusRequestedQuotasDto(
                cpuVlaResource.id, AmountDto(
                    "3",
                    "cores",
                    "3000",
                    "mCores",
                    "3",
                    CORES,
                    "3000",
                    MILLICORES
                )
            )
        )
        Assertions.assertTrue(requestedQuota.size == accountSFOpp.requestedQuotas.size
            && requestedQuota.containsAll(accountSFOpp.requestedQuotas)
            && accountSFOpp.requestedQuotas.containsAll(requestedQuota))
        Assertions.assertEquals(
            DeliveryAndProvideMetaResponseDto.Builder()
                .quotaRequestId(requestId)
                .campaignId(campaignId)
                .addBigOrderId(bigOrderId)
                .build(), accountSFOpp.meta!!
        )
        Assertions.assertEquals(
            DeliverableFolderOperationDto.builder()
                .folderId(logModelSF.identity.folderId)
                .timestamp(logModelSF.identity.operationDateTime)
                .id(logModelSF.identity.id)
                .build(), accountSFOpp.folderOperationLogs!!.stream().findFirst().orElseThrow()
        )

        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(
                    txSession, listOf(firstFolder.id, secondFolder.id), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        updatedQuotas ?: failOnNull()
        val quotaByResourceIdByFolder = mutableMapOf<String, MutableMap<String, QuotaModel>>()
        for (quota in updatedQuotas) {
            val orDefault = quotaByResourceIdByFolder.getOrPut(quota.folderId) { mutableMapOf() }
            orDefault[quota.resourceId] = quota
        }

        Assertions.assertEquals(
            300L + 400L + 1400L,
            quotaByResourceIdByFolder[firstFolder.id]!![hddSasResource.id]!!.quota
        )
        Assertions.assertEquals(
            100L + 500L + 1500L,
            quotaByResourceIdByFolder[firstFolder.id]!![hddVlaResource.id]!!.quota
        )
        Assertions.assertEquals(
            200L + 1000L + 2000L,
            quotaByResourceIdByFolder[firstFolder.id]!![ramSasResource.id]!!.quota
        )
        Assertions.assertEquals(
            200L + 2000L + 3000L,
            quotaByResourceIdByFolder[firstFolder.id]!![ramVlaResource.id]!!.quota
        )
        Assertions.assertEquals(
            1000L + 2000L,
            quotaByResourceIdByFolder[firstFolder.id]!![cpuSasResource.id]!!.quota
        )
        Assertions.assertEquals(
            2000L + 3000L,
            quotaByResourceIdByFolder[firstFolder.id]!![cpuVlaResource.id]!!.quota
        )

        Assertions.assertEquals(
            200L,
            quotaByResourceIdByFolder[firstFolder.id]!![hddSasResource.id]!!.balance
        )
        Assertions.assertEquals(
            90L,
            quotaByResourceIdByFolder[firstFolder.id]!![hddVlaResource.id]!!.balance
        )
        Assertions.assertEquals(
            100L,
            quotaByResourceIdByFolder[firstFolder.id]!![ramSasResource.id]!!.balance
        )
        Assertions.assertEquals(
            200L,
            quotaByResourceIdByFolder[firstFolder.id]!![ramVlaResource.id]!!.balance
        )
        Assertions.assertEquals(
            900L,
            quotaByResourceIdByFolder[firstFolder.id]!![cpuSasResource.id]!!.balance
        )
        Assertions.assertEquals(
            2000L,
            quotaByResourceIdByFolder[firstFolder.id]!![cpuVlaResource.id]!!.balance
        )

        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[firstFolder.id]!![hddSasResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[firstFolder.id]!![hddVlaResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[firstFolder.id]!![ramSasResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[firstFolder.id]!![ramVlaResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[firstFolder.id]!![cpuSasResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[firstFolder.id]!![cpuVlaResource.id]!!.frozenQuota
        )

        Assertions.assertEquals(
            400L,
            quotaByResourceIdByFolder[secondFolder.id]!![hddSasResource.id]!!.quota
        )
        Assertions.assertEquals(
            500L,
            quotaByResourceIdByFolder[secondFolder.id]!![hddVlaResource.id]!!.quota
        )
        Assertions.assertEquals(
            1000L,
            quotaByResourceIdByFolder[secondFolder.id]!![ramSasResource.id]!!.quota
        )
        Assertions.assertEquals(
            2000L,
            quotaByResourceIdByFolder[secondFolder.id]!![ramVlaResource.id]!!.quota
        )
        Assertions.assertEquals(
            2000L,
            quotaByResourceIdByFolder[secondFolder.id]!![cpuSasResource.id]!!.quota
        )
        Assertions.assertEquals(
            3000L,
            quotaByResourceIdByFolder[secondFolder.id]!![cpuVlaResource.id]!!.quota
        )

        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![hddSasResource.id]!!.balance
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![hddVlaResource.id]!!.balance
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![ramSasResource.id]!!.balance
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![ramVlaResource.id]!!.balance
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![cpuSasResource.id]!!.balance
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![cpuVlaResource.id]!!.balance
        )

        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![hddSasResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![hddVlaResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![ramSasResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![ramVlaResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![cpuSasResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![cpuVlaResource.id]!!.frozenQuota
        )

        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(
                    txSession, Tenants.DEFAULT_TENANT_ID, setOf(firstAccountFF.id, secondAccountFF.id, accountSF.id)
                )
            }
        }.block()
        updatedProvisions ?: failOnNull()
        val provisionByResourceIdByAccount = mutableMapOf<String, MutableMap<String, AccountsQuotasModel>>()
        for (provision in updatedProvisions) {
            val orDefault = provisionByResourceIdByAccount.getOrPut(provision.accountId) { mutableMapOf() }
            orDefault[provision.resourceId] = provision
        }

        var provisionByResourceId = provisionByResourceIdByAccount[firstAccountFF.id]
        provisionByResourceId ?: failOnNull()
        Assertions.assertEquals(
            50L + 400L,
            provisionByResourceId[hddSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            50L,
            provisionByResourceId[hddSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[hddSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[firstAccountFF.id]!!.operationId,
            provisionByResourceId[hddSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddSasResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            10L + 500L,
            provisionByResourceId[hddVlaResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddVlaResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[hddVlaResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[firstAccountFF.id]!!.operationId,
            provisionByResourceId[hddVlaResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddVlaResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            100L + 1000L,
            provisionByResourceId[ramSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[ramSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[firstAccountFF.id]!!.operationId,
            provisionByResourceId[ramSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramSasResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            2000,
            provisionByResourceId[ramVlaResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramVlaResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[ramVlaResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[firstAccountFF.id]!!.operationId,
            provisionByResourceId[ramVlaResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramVlaResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            2000,
            provisionByResourceId[cpuSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[cpuSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[cpuSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[firstAccountFF.id]!!.operationId,
            provisionByResourceId[cpuSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[cpuSasResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            3000,
            provisionByResourceId[cpuVlaResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[cpuVlaResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[cpuVlaResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[firstAccountFF.id]!!.operationId,
            provisionByResourceId[cpuVlaResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[cpuVlaResource.id]!!.frozenProvidedQuota
        )

        provisionByResourceId = provisionByResourceIdByAccount[secondAccountFF.id]
        provisionByResourceId ?: failOnNull()
        Assertions.assertEquals(
            50L + 1400L,
            provisionByResourceId[hddSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            10L,
            provisionByResourceId[hddSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[hddSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[secondAccountFF.id]!!.operationId,
            provisionByResourceId[hddSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddSasResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            1500L,
            provisionByResourceId[hddVlaResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddVlaResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[hddVlaResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[secondAccountFF.id]!!.operationId,
            provisionByResourceId[hddVlaResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddVlaResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            2000L,
            provisionByResourceId[ramSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[ramSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[secondAccountFF.id]!!.operationId,
            provisionByResourceId[ramSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramSasResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            3000,
            provisionByResourceId[ramVlaResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramVlaResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[ramVlaResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[secondAccountFF.id]!!.operationId,
            provisionByResourceId[ramVlaResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramVlaResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            100,
            provisionByResourceId[cpuSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[cpuSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[cpuSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertTrue(
            provisionByResourceId[cpuSasResource.id]!!.latestSuccessfulProvisionOperationId.isEmpty
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[cpuSasResource.id]!!.frozenProvidedQuota
        )

        provisionByResourceId = provisionByResourceIdByAccount[accountSF.id]
        provisionByResourceId ?: failOnNull()
        Assertions.assertEquals(
            400L,
            provisionByResourceId[hddSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[hddSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[accountSF.id]!!.operationId,
            provisionByResourceId[hddSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddSasResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            500L,
            provisionByResourceId[hddVlaResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddVlaResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[hddVlaResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[accountSF.id]!!.operationId,
            provisionByResourceId[hddVlaResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddVlaResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            1000L,
            provisionByResourceId[ramSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[ramSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[accountSF.id]!!.operationId,
            provisionByResourceId[ramSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramSasResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            2000,
            provisionByResourceId[ramVlaResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramVlaResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[ramVlaResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[accountSF.id]!!.operationId,
            provisionByResourceId[ramVlaResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramVlaResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            2000,
            provisionByResourceId[cpuSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[cpuSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[cpuSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[accountSF.id]!!.operationId,
            provisionByResourceId[cpuSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[cpuSasResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            3000,
            provisionByResourceId[cpuVlaResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[cpuVlaResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[cpuVlaResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[accountSF.id]!!.operationId,
            provisionByResourceId[cpuVlaResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[cpuVlaResource.id]!!.frozenProvidedQuota
        )

        val updatedFolders = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.getByIds(
                    txSession, listOf(firstFolder.id, secondFolder.id), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()

        updatedFolders ?: failOnNull()
        val updatedFoldersById = updatedFolders.associateBy { it.id }
        Assertions.assertEquals(7, updatedFoldersById[firstFolder.id]!!.nextOpLogOrder)
        Assertions.assertEquals(5, updatedFoldersById[secondFolder.id]!!.nextOpLogOrder)

        val operationInProgress = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenantFolders(
                    txSession, Tenants.DEFAULT_TENANT_ID, setOf(firstFolder.id, secondFolder.id)
                )
            }
        }.block()
        operationInProgress ?: failOnNull()
        Assertions.assertTrue(operationInProgress.isEmpty())

        val accountQuotaOperations = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getByIds(
                    txSession, operations.map { it.operationId }.toList(), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        accountQuotaOperations ?: failOnNull()
        Assertions.assertEquals(3, accountQuotaOperations.size)
        val accountsOperationsByAccountId = accountQuotaOperations.associateBy { it.requestedChanges.accountId.get() }
        var accountOperation = accountsOperationsByAccountId[firstAccountFF.id]
        accountOperation ?: failOnNull()
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.OperationType.DELIVER_AND_UPDATE_PROVISION,
            accountOperation.operationType
        )
        Assertions.assertEquals(OperationSource.USER, accountOperation.operationSource)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, accountOperation.authorUserId)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, accountOperation.authorUserUid.get())
        Assertions.assertEquals(firstAccountFF.providerId, accountOperation.providerId)
        Assertions.assertTrue(accountOperation.accountsSpaceId.isEmpty)
        Assertions.assertTrue(accountOperation.updateDateTime.isPresent)
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.OK, accountOperation.requestStatus.get())
        Assertions.assertTrue(accountOperation.errorMessage.isEmpty)
        Assertions.assertTrue(accountOperation.fullErrorMessage.isEmpty)
        Assertions.assertTrue(accountOperation.errorKind.isEmpty)
        assertEquals(OperationChangesModel.builder()
            .accountId(firstAccountFF.id)
            .deliveryId(body.deliveryId)
            .updatedProvisions(
                listOf(
                    OperationChangesModel.Provision(hddSasResource.id, 50L + 400L),
                    OperationChangesModel.Provision(hddVlaResource.id, 10L + 500L),
                    OperationChangesModel.Provision(ramSasResource.id, 100L + 1000L),
                    OperationChangesModel.Provision(ramVlaResource.id, 2000L),
                    OperationChangesModel.Provision(cpuSasResource.id, 2000L),
                    OperationChangesModel.Provision(cpuVlaResource.id, 3000L)
                )
            )
            .frozenProvisions(
                listOf(
                    OperationChangesModel.Provision(hddSasResource.id, 400L),
                    OperationChangesModel.Provision(hddVlaResource.id, 500L),
                    OperationChangesModel.Provision(ramSasResource.id, 1000L),
                    OperationChangesModel.Provision(ramVlaResource.id, 2000L),
                    OperationChangesModel.Provision(cpuSasResource.id, 2000L),
                    OperationChangesModel.Provision(cpuVlaResource.id, 3000L)
                )
            )
            .build(), accountOperation.requestedChanges)
        val submitOrder = accountOperation.orders.submitOrder
        val closeOrder = accountOperation.orders.closeOrder.get()
        Assertions.assertTrue(submitOrder == 3L || submitOrder == 4L)
        Assertions.assertTrue(closeOrder == 5L || closeOrder == 6L)

        accountOperation = accountsOperationsByAccountId[secondAccountFF.id]
        accountOperation ?: failOnNull()
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.OperationType.DELIVER_AND_UPDATE_PROVISION,
            accountOperation.operationType
        )
        Assertions.assertEquals(OperationSource.USER, accountOperation.operationSource)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, accountOperation.authorUserId)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, accountOperation.authorUserUid.get())
        Assertions.assertEquals(secondAccountFF.providerId, accountOperation.providerId)
        Assertions.assertTrue(accountOperation.accountsSpaceId.isEmpty)
        Assertions.assertTrue(accountOperation.updateDateTime.isPresent)
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.OK, accountOperation.requestStatus.get())
        Assertions.assertTrue(accountOperation.errorMessage.isEmpty)
        Assertions.assertTrue(accountOperation.fullErrorMessage.isEmpty)
        Assertions.assertTrue(accountOperation.errorKind.isEmpty)
        assertEquals(            OperationChangesModel.builder()
            .accountId(secondAccountFF.id)
            .deliveryId(body.deliveryId)
            .updatedProvisions(
                listOf(
                    OperationChangesModel.Provision(hddSasResource.id, 50L + 1400L),
                    OperationChangesModel.Provision(hddVlaResource.id, 0L + 1500L),
                    OperationChangesModel.Provision(ramSasResource.id, 2000L),
                    OperationChangesModel.Provision(ramVlaResource.id, 3000L)
                )
            )
            .frozenProvisions(
                listOf(
                    OperationChangesModel.Provision(hddSasResource.id, 1400L),
                    OperationChangesModel.Provision(hddVlaResource.id, 1500L),
                    OperationChangesModel.Provision(ramSasResource.id, 2000L),
                    OperationChangesModel.Provision(ramVlaResource.id, 3000L)
                )
            )
            .build(), accountOperation.requestedChanges)
        Assertions.assertTrue(
            (accountOperation.orders.submitOrder == 3L
                && accountOperation.orders.submitOrder != submitOrder)
                || (accountOperation.orders.submitOrder == 4L
                && accountOperation.orders.submitOrder != submitOrder)
        )
        Assertions.assertTrue(
            (accountOperation.orders.closeOrder.get() == 5L
                && accountOperation.orders.closeOrder.get() != closeOrder)
                || (accountOperation.orders.closeOrder.get() == 6L
                && accountOperation.orders.closeOrder.get() != closeOrder)
        )

        accountOperation = accountsOperationsByAccountId[accountSF.id]
        accountOperation ?: failOnNull()
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.OperationType.DELIVER_AND_UPDATE_PROVISION,
            accountOperation.operationType
        )
        Assertions.assertEquals(OperationSource.USER, accountOperation.operationSource)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, accountOperation.authorUserId)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, accountOperation.authorUserUid.get())
        Assertions.assertEquals(accountSF.providerId, accountOperation.providerId)
        Assertions.assertTrue(accountOperation.accountsSpaceId.isEmpty)
        Assertions.assertTrue(accountOperation.updateDateTime.isPresent)
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.OK, accountOperation.requestStatus.get())
        Assertions.assertTrue(accountOperation.errorMessage.isEmpty)
        Assertions.assertTrue(accountOperation.fullErrorMessage.isEmpty)
        Assertions.assertTrue(accountOperation.errorKind.isEmpty)
        assertEquals(
            OperationChangesModel.builder()
                .accountId(accountSF.id)
                .deliveryId(body.deliveryId)
                .updatedProvisions(
                    listOf(
                        OperationChangesModel.Provision(hddSasResource.id, 400L),
                        OperationChangesModel.Provision(hddVlaResource.id, 500L),
                        OperationChangesModel.Provision(ramSasResource.id, 1000L),
                        OperationChangesModel.Provision(ramVlaResource.id, 2000L),
                        OperationChangesModel.Provision(cpuSasResource.id, 2000L),
                        OperationChangesModel.Provision(cpuVlaResource.id, 3000L)
                    )
                )
                .frozenProvisions(
                    listOf(
                        OperationChangesModel.Provision(hddSasResource.id, 400L),
                        OperationChangesModel.Provision(hddVlaResource.id, 500L),
                        OperationChangesModel.Provision(ramSasResource.id, 1000L),
                        OperationChangesModel.Provision(ramVlaResource.id, 2000L),
                        OperationChangesModel.Provision(cpuSasResource.id, 2000L),
                        OperationChangesModel.Provision(cpuVlaResource.id, 3000L)
                    )
                )
                .build(), accountOperation.requestedChanges
        )
        Assertions.assertEquals(
            OperationOrdersModel.builder()
                .submitOrder(3L)
                .closeOrder(4L)
                .build(), accountOperation.orders
        )

        val firstFolderOppLogs = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, firstFolder.id, SortOrderDto.ASC, 10
                )
            }
        }.block()
        firstFolderOppLogs ?: failOnNull()
        Assertions.assertEquals(5, firstFolderOppLogs.size)
        val firstFolderLogsByAccountByPhase =
            mutableMapOf<String, MutableMap<OperationPhase, FolderOperationLogModel>>()
        for (log in firstFolderOppLogs) {
            if (log.operationType == FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT) {
                val orPut =
                    firstFolderLogsByAccountByPhase.getOrPut(log.accountsQuotasOperationsId.get()) { mutableMapOf() }
                orPut[log.operationPhase.get()] = log
            }
        }
        val firstAccountFFOperation = accountsOperationsByAccountId[firstAccountFF.id]
        firstAccountFFOperation ?: failOnNull()
        var folderLog = firstFolderLogsByAccountByPhase[firstAccountFFOperation.operationId]!![OperationPhase.SUBMIT]
        folderLog ?: failOnNull()
        Assertions.assertEquals(firstAccountFFOperation.lastRequestId.get(), folderLog.providerRequestId.get())
        Assertions.assertEquals(FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, folderLog.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, folderLog.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, folderLog.authorUserUid.get())
        Assertions.assertTrue(folderLog.authorProviderId.isEmpty)
        Assertions.assertTrue(folderLog.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.oldFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    firstAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(50L, null),
                            hddVlaResource.id to ProvisionHistoryModel(10L, null),
                            ramSasResource.id to ProvisionHistoryModel(100L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L, null),
                            cpuVlaResource.id to ProvisionHistoryModel(0L, null)
                        )
                    )
                )
            ), folderLog.oldProvisions
        )
        Assertions.assertTrue(folderLog.oldAccounts.isEmpty)
        Assertions.assertTrue(folderLog.newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    firstAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(50L + 400L, 0L),
                            hddVlaResource.id to ProvisionHistoryModel(10L + 500L, 0L),
                            ramSasResource.id to ProvisionHistoryModel(100L + 1000L, 0L),
                            ramVlaResource.id to ProvisionHistoryModel(0L + 2000L, 0L),
                            cpuSasResource.id to ProvisionHistoryModel(0L + 2000L, 0L),
                            cpuVlaResource.id to ProvisionHistoryModel(0L + 3000L, 0L)
                        )
                    )
                )
            ), folderLog.newProvisions
        )
        Assertions.assertTrue(folderLog.actuallyAppliedProvisions.isEmpty)
        Assertions.assertTrue(folderLog.newAccounts.isEmpty)
        Assertions.assertTrue(folderLog.quotasDemandsId.isEmpty)
        Assertions.assertTrue(folderLog.commentId.isEmpty)
        Assertions.assertEquals(DeliverableMetaHistoryModel.builder()
            .quotaRequestId(requestId)
            .campaignId(campaignId)
            .bigOrderIds(bigOrderId)
            .deliveryId(body.deliveryId)
            .build(), folderLog.deliveryMeta.get())
        Assertions.assertTrue(folderLog.transferMeta.isEmpty)
        Assertions.assertEquals(firstAccountFFOperation.orders.submitOrder, folderLog.order)

        folderLog = firstFolderLogsByAccountByPhase[firstAccountFFOperation.operationId]!![OperationPhase.CLOSE]
        folderLog ?: failOnNull()
        Assertions.assertTrue(folderLog.providerRequestId.isEmpty)
        Assertions.assertEquals(FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, folderLog.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, folderLog.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, folderLog.authorUserUid.get())
        Assertions.assertTrue(folderLog.authorProviderId.isEmpty)
        Assertions.assertTrue(folderLog.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.oldFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    firstAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(50L, null),
                            hddVlaResource.id to ProvisionHistoryModel(10L, null),
                            ramSasResource.id to ProvisionHistoryModel(100L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L, null),
                            cpuVlaResource.id to ProvisionHistoryModel(0L, null)
                        )
                    )
                )
            ), folderLog.oldProvisions
        )
        Assertions.assertTrue(folderLog.oldAccounts.isEmpty)
        Assertions.assertTrue(folderLog.newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    firstAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(50L + 400L, null),
                            hddVlaResource.id to ProvisionHistoryModel(10L + 500L, null),
                            ramSasResource.id to ProvisionHistoryModel(100L + 1000L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L + 2000L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L + 2000L, null),
                            cpuVlaResource.id to ProvisionHistoryModel(0L + 3000L, null)
                        )
                    )
                )
            ), folderLog.newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    firstAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(50L + 400L, null),
                            hddVlaResource.id to ProvisionHistoryModel(10L + 500L, null),
                            ramSasResource.id to ProvisionHistoryModel(100L + 1000L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L + 2000L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L + 2000L, null),
                            cpuVlaResource.id to ProvisionHistoryModel(0L + 3000L, null)
                        )
                    )
                )
            ), folderLog.actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(folderLog.newAccounts.isEmpty)
        Assertions.assertTrue(folderLog.quotasDemandsId.isEmpty)
        Assertions.assertTrue(folderLog.commentId.isEmpty)
        Assertions.assertTrue(folderLog.deliveryMeta.isEmpty)
        Assertions.assertTrue(folderLog.transferMeta.isEmpty)
        Assertions.assertEquals(firstAccountFFOperation.orders.closeOrder.get(), folderLog.order)

        val secondAccountFFOperation = accountsOperationsByAccountId[secondAccountFF.id]
        secondAccountFFOperation ?: failOnNull()
        folderLog = firstFolderLogsByAccountByPhase[secondAccountFFOperation.operationId]!![OperationPhase.SUBMIT]
        folderLog ?: failOnNull()
        Assertions.assertEquals(secondAccountFFOperation.lastRequestId.get(), folderLog.providerRequestId.get())
        Assertions.assertEquals(FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, folderLog.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, folderLog.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, folderLog.authorUserUid.get())
        Assertions.assertTrue(folderLog.authorProviderId.isEmpty)
        Assertions.assertTrue(folderLog.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.oldFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    secondAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(50L, null),
                            hddVlaResource.id to ProvisionHistoryModel(0L, null),
                            ramSasResource.id to ProvisionHistoryModel(0L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L, null)
                        )
                    )
                )
            ), folderLog.oldProvisions
        )
        Assertions.assertTrue(folderLog.oldAccounts.isEmpty)
        Assertions.assertTrue(folderLog.newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    secondAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(50L + 1400L, 0L),
                            hddVlaResource.id to ProvisionHistoryModel(0L + 1500L, 0L),
                            ramSasResource.id to ProvisionHistoryModel(0L + 2000L, 0L),
                            ramVlaResource.id to ProvisionHistoryModel(0L + 3000L, 0L)
                        )
                    )
                )
            ), folderLog.newProvisions
        )
        Assertions.assertTrue(folderLog.actuallyAppliedProvisions.isEmpty)
        Assertions.assertTrue(folderLog.newAccounts.isEmpty)
        Assertions.assertTrue(folderLog.quotasDemandsId.isEmpty)
        Assertions.assertTrue(folderLog.commentId.isEmpty)
        Assertions.assertEquals(DeliverableMetaHistoryModel.builder()
            .quotaRequestId(requestId)
            .campaignId(campaignId)
            .bigOrderIds(bigOrderId)
            .deliveryId(body.deliveryId)
            .build(), folderLog.deliveryMeta.get())
        Assertions.assertTrue(folderLog.transferMeta.isEmpty)
        Assertions.assertEquals(secondAccountFFOperation.orders.submitOrder, folderLog.order)

        folderLog = firstFolderLogsByAccountByPhase[secondAccountFFOperation.operationId]!![OperationPhase.CLOSE]
        folderLog ?: failOnNull()
        Assertions.assertTrue(folderLog.providerRequestId.isEmpty)
        Assertions.assertEquals(FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, folderLog.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, folderLog.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, folderLog.authorUserUid.get())
        Assertions.assertTrue(folderLog.authorProviderId.isEmpty)
        Assertions.assertTrue(folderLog.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.oldFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    secondAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(50L, null),
                            hddVlaResource.id to ProvisionHistoryModel(0L, null),
                            ramSasResource.id to ProvisionHistoryModel(0L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L, null)
                        )
                    )
                )
            ), folderLog.oldProvisions
        )
        Assertions.assertTrue(folderLog.oldAccounts.isEmpty)
        Assertions.assertTrue(folderLog.newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    secondAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(50L + 1400L, null),
                            hddVlaResource.id to ProvisionHistoryModel(0L + 1500L, null),
                            ramSasResource.id to ProvisionHistoryModel(0L + 2000L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L + 3000L, null)
                        )
                    )
                )
            ), folderLog.newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    secondAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(50L + 1400L, null),
                            hddVlaResource.id to ProvisionHistoryModel(0L + 1500L, null),
                            ramSasResource.id to ProvisionHistoryModel(0L + 2000L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L + 3000L, null)
                        )
                    )
                )
            ), folderLog.actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(folderLog.newAccounts.isEmpty)
        Assertions.assertTrue(folderLog.quotasDemandsId.isEmpty)
        Assertions.assertTrue(folderLog.commentId.isEmpty)
        Assertions.assertTrue(folderLog.deliveryMeta.isEmpty)
        Assertions.assertTrue(folderLog.transferMeta.isEmpty)
        Assertions.assertEquals(secondAccountFFOperation.orders.closeOrder.get(), folderLog.order)

        val secondFolderOppLogs = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, secondFolder.id, SortOrderDto.ASC, 10
                )
            }
        }.block()
        secondFolderOppLogs ?: failOnNull()
        Assertions.assertEquals(3, secondFolderOppLogs.size)
        val secondFolderLogsByAccountByPhase =
            mutableMapOf<String, MutableMap<OperationPhase, FolderOperationLogModel>>()
        for (log in secondFolderOppLogs) {
            if (log.operationType == FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT) {
                val orPut =
                    secondFolderLogsByAccountByPhase.getOrPut(log.accountsQuotasOperationsId.get()) { mutableMapOf() }
                orPut[log.operationPhase.get()] = log
            }
        }

        val accountSFOperation = accountsOperationsByAccountId[accountSF.id]
        accountSFOperation ?: failOnNull()
        folderLog = secondFolderLogsByAccountByPhase[accountSFOperation.operationId]!![OperationPhase.SUBMIT]
        folderLog ?: failOnNull()
        Assertions.assertEquals(accountSFOperation.lastRequestId.get(), folderLog.providerRequestId.get())
        Assertions.assertEquals(FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, folderLog.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, folderLog.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, folderLog.authorUserUid.get())
        Assertions.assertTrue(folderLog.authorProviderId.isEmpty)
        Assertions.assertTrue(folderLog.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.oldFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    accountSF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(0L, null),
                            hddVlaResource.id to ProvisionHistoryModel(0L, null),
                            ramSasResource.id to ProvisionHistoryModel(0L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L, null),
                            cpuVlaResource.id to ProvisionHistoryModel(0L, null)
                        )
                    )
                )
            ), folderLog.oldProvisions
        )
        Assertions.assertTrue(folderLog.oldAccounts.isEmpty)
        Assertions.assertTrue(folderLog.newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    accountSF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(0L + 400L, 0L),
                            hddVlaResource.id to ProvisionHistoryModel(0L + 500L, 0L),
                            ramSasResource.id to ProvisionHistoryModel(0L + 1000L, 0L),
                            ramVlaResource.id to ProvisionHistoryModel(0L + 2000L, 0L),
                            cpuSasResource.id to ProvisionHistoryModel(0L + 2000L, 0L),
                            cpuVlaResource.id to ProvisionHistoryModel(0L + 3000L, 0L)
                        )
                    )
                )
            ), folderLog.newProvisions
        )
        Assertions.assertTrue(folderLog.actuallyAppliedProvisions.isEmpty)
        Assertions.assertTrue(folderLog.newAccounts.isEmpty)
        Assertions.assertTrue(folderLog.quotasDemandsId.isEmpty)
        Assertions.assertTrue(folderLog.commentId.isEmpty)
        Assertions.assertEquals(DeliverableMetaHistoryModel.builder()
            .quotaRequestId(requestId)
            .campaignId(campaignId)
            .bigOrderIds(bigOrderId)
            .deliveryId(body.deliveryId)
            .build(), folderLog.deliveryMeta.get())
        Assertions.assertTrue(folderLog.transferMeta.isEmpty)
        Assertions.assertEquals(accountSFOperation.orders.submitOrder, folderLog.order)

        folderLog = secondFolderLogsByAccountByPhase[accountSFOperation.operationId]!![OperationPhase.CLOSE]
        folderLog ?: failOnNull()
        Assertions.assertTrue(folderLog.providerRequestId.isEmpty)
        Assertions.assertEquals(FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, folderLog.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, folderLog.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, folderLog.authorUserUid.get())
        Assertions.assertTrue(folderLog.authorProviderId.isEmpty)
        Assertions.assertTrue(folderLog.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.oldFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    accountSF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(0L, null),
                            hddVlaResource.id to ProvisionHistoryModel(0L, null),
                            ramSasResource.id to ProvisionHistoryModel(0L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L, null),
                            cpuVlaResource.id to ProvisionHistoryModel(0L, null)
                        )
                    )
                )
            ), folderLog.oldProvisions
        )
        Assertions.assertTrue(folderLog.oldAccounts.isEmpty)
        Assertions.assertTrue(folderLog.newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    accountSF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(0L + 400L, null),
                            hddVlaResource.id to ProvisionHistoryModel(0L + 500L, null),
                            ramSasResource.id to ProvisionHistoryModel(0L + 1000L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L + 2000L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L + 2000L, null),
                            cpuVlaResource.id to ProvisionHistoryModel(0L + 3000L, null)
                        )
                    )
                )
            ), folderLog.newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    accountSF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(0L + 400L, null),
                            hddVlaResource.id to ProvisionHistoryModel(0L + 500L, null),
                            ramSasResource.id to ProvisionHistoryModel(0L + 1000L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L + 2000L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L + 2000L, null),
                            cpuVlaResource.id to ProvisionHistoryModel(0L + 3000L, null)
                        )
                    )
                )
            ), folderLog.actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(folderLog.newAccounts.isEmpty)
        Assertions.assertTrue(folderLog.quotasDemandsId.isEmpty)
        Assertions.assertTrue(folderLog.commentId.isEmpty)
        Assertions.assertTrue(folderLog.deliveryMeta.isEmpty)
        Assertions.assertTrue(folderLog.transferMeta.isEmpty)
        Assertions.assertEquals(accountSFOperation.orders.closeOrder.get(), folderLog.order)
    }

    @Test
    fun testSuccessDeliverAndProvideWithAccountSpace() {
        val provider = providerModel(
            GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true,
            accountsSpacesSupported = true
        )
        val firstFolder = folderModel(1L, "First folder", "First folder description")
        val secondFolder = folderModel(1L, "Second folder", "Second folder description")
        val storageResourceType = resourceTypeModel(provider.id, "storage", STORAGE_UNITS_DECIMAL_ID)
        val memoryResourceType = resourceTypeModel(provider.id, "memory", STORAGE_UNITS_DECIMAL_ID)
        val cpuResourceType = resourceTypeModel(provider.id, "cpu", CPU_UNITS_ID)
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val sasSegment = resourceSegmentModel(locationSegmentation.id, "SAS")
        val vlaAccountsSpace: AccountSpaceModel = DummyModels.accountSpaceModel(
            provider.id, "test", setOf(Tuples.of(locationSegmentation.id, vlaSegment.id))
        )
        val sasAccountsSpace: AccountSpaceModel = DummyModels.accountSpaceModel(
            provider.id, "test", setOf(Tuples.of(locationSegmentation.id, sasSegment.id))
        )

        val hddSasResource = resourceModel(
            provider.id,
            "hddSas",
            storageResourceType.id,
            setOf(Tuples.of(locationSegmentation.id, sasSegment.id)),
            STORAGE_UNITS_DECIMAL_ID,
            setOf(
                BYTES,
                KILOBYTES,
                MEGABYTES,
                GIGABYTES
            ),
            GIGABYTES,
            BYTES,
            sasAccountsSpace.id
        )
        val hddVlaResource = resourceModel(
            provider.id,
            "hddVla",
            storageResourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            STORAGE_UNITS_DECIMAL_ID,
            setOf(
                BYTES,
                KILOBYTES,
                MEGABYTES,
                GIGABYTES
            ),
            GIGABYTES,
            BYTES,
            vlaAccountsSpace.id
        )
        val ramSasResource = resourceModel(
            provider.id,
            "ramSas",
            memoryResourceType.id,
            setOf(Tuples.of(locationSegmentation.id, sasSegment.id)),
            STORAGE_UNITS_DECIMAL_ID,
            setOf(
                BYTES,
                KILOBYTES,
                MEGABYTES,
                GIGABYTES
            ),
            GIGABYTES,
            BYTES,
            sasAccountsSpace.id
        )
        val ramVlaResource = resourceModel(
            provider.id,
            "ramVla",
            memoryResourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            STORAGE_UNITS_DECIMAL_ID,
            setOf(
                BYTES,
                KILOBYTES,
                MEGABYTES,
                GIGABYTES
            ),
            GIGABYTES,
            BYTES,
            vlaAccountsSpace.id
        )
        val cpuSasResource = resourceModel(
            provider.id,
            "cpuSas",
            cpuResourceType.id,
            setOf(Tuples.of(locationSegmentation.id, sasSegment.id)),
            CPU_UNITS_ID,
            setOf(
                MILLICORES,
                CORES
            ),
            CORES,
            MILLICORES,
            sasAccountsSpace.id
        )
        val cpuVlaResource = resourceModel(
            provider.id,
            "cpuVla",
            cpuResourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            CPU_UNITS_ID,
            setOf(
                MILLICORES,
                CORES
            ),
            CORES,
            MILLICORES,
            vlaAccountsSpace.id
        )
        val hddSasQuotaFF = quotaModel(provider.id, hddSasResource.id, firstFolder.id, 300L, 200L, 0L)
        val hddVlaQuotaFF = quotaModel(provider.id, hddVlaResource.id, firstFolder.id, 100L, 90L, 0L)
        val ramSasQuotaFF = quotaModel(provider.id, ramSasResource.id, firstFolder.id, 200L, 100L, 0L)
        val ramVlaQuotaFF = quotaModel(provider.id, ramVlaResource.id, firstFolder.id, 200L, 200L, 0L)
        val cpuSasQuotaFF = quotaModel(provider.id, cpuSasResource.id, firstFolder.id, 1000L, 1000L, 0L)
        val cpuVlaQuotaFF = quotaModel(provider.id, cpuVlaResource.id, firstFolder.id, 2000L, 2000L, 0L)
        val firstAccountFF = accountModel(
            provider.id, sasAccountsSpace.id, "first-account-first-folder",
            "first-account-first-folder", firstFolder.id, "first-account-first-folder", null, null
        )
        val secondAccountFF = accountModel(
            provider.id, vlaAccountsSpace.id, "second-account-first-folder",
            "second-account-first-folder", firstFolder.id, "second-account-first-folder", null, null
        )
        val accountSF = accountModel(
            provider.id, sasAccountsSpace.id, "account-second-folder",
            "account-second-folder", secondFolder.id, "account-second-folder", null, null
        )
        val hddSasProvisionFFFA = accountQuotaModel(
            provider.id, hddSasResource.id, firstFolder.id, firstAccountFF.id,
            100L, 50L
        )
        val hddVlaProvisionFFSA = accountQuotaModel(
            provider.id, hddVlaResource.id, firstFolder.id, secondAccountFF.id,
            10L, 0L
        )
        val ramSasProvisionFFFA = accountQuotaModel(
            provider.id, ramSasResource.id, firstFolder.id, firstAccountFF.id,
            100L, 0L
        )
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertAllRetryable(txSession, listOf(firstFolder, secondFolder))
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypesRetryable(
                    txSession,
                    listOf(storageResourceType, memoryResourceType, cpuResourceType)
                )
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession, listOf(vlaSegment, sasSegment))
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsSpacesDao.upsertAllRetryable(
                    txSession, listOf(
                        sasAccountsSpace, vlaAccountsSpace
                    )
                )
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourcesRetryable(
                    txSession, listOf(
                        hddSasResource, hddVlaResource, ramSasResource,
                        ramVlaResource, cpuSasResource, cpuVlaResource
                    )
                )
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertAllRetryable(txSession, listOf(firstAccountFF, secondAccountFF, accountSF))
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertAllRetryable(
                    txSession, listOf(
                        hddSasQuotaFF, hddVlaQuotaFF, ramSasQuotaFF, ramVlaQuotaFF, cpuSasQuotaFF, cpuVlaQuotaFF
                    )
                )
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertAllRetryable(
                    txSession, listOf(
                        hddSasProvisionFFFA, hddVlaProvisionFFSA, ramSasProvisionFFFA
                    )
                )
            }
        }.block()
        val requestId = 143L
        val campaignId = 40L
        val bigOrderId = 44L
        val body = DeliveryAndProvideRequestDto.Builder()
            .deliveryId(UUID.randomUUID().toString())
            .authorUid(DISPENSER_QUOTA_MANAGER_UID)
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(firstAccountFF.id)
                    .resourceId(hddSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(400)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(firstAccountFF.id)
                    .resourceId(ramSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(1000)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(firstAccountFF.id)
                    .resourceId(cpuSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(2000)
                            .unitKey(MILLICORES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(secondAccountFF.id)
                    .resourceId(hddVlaResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(1500)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(secondAccountFF.id)
                    .resourceId(ramVlaResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(3000)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(secondFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(secondFolder.id)
                    .accountId(accountSF.id)
                    .resourceId(hddSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(400)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(secondFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(secondFolder.id)
                    .accountId(accountSF.id)
                    .resourceId(ramSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(1000)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(secondFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(secondFolder.id)
                    .accountId(accountSF.id)
                    .resourceId(cpuSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(2000)
                            .unitKey(MILLICORES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderId)
                            .build()
                    )
                    .build()
            )
            .build()
        stubProviderService.setUpdateProvisionResponseByMappingFunction(true)
        stubProviderService.setUpdateProvisionRequestToKeyFunction { r: UpdateProvisionRequest? -> r!!.accountId }
        stubProviderService.addAllToUpdateProvisionResponsesMap(
            mapOf(
                firstAccountFF.outerAccountIdInProvider to
                    GrpcResponse.success(
                        UpdateProvisionResponse.newBuilder()
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("storage")
                                            .build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(500L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(50L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("memory")
                                            .build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(1100L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("cpu")
                                            .build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(2000L).setUnitKey("millicores").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("millicores").build()
                                ).build()
                            )
                            .setAccountsSpaceKey(
                                AccountsSpaceKey.newBuilder().setCompoundKey(
                                    CompoundAccountsSpaceKey.newBuilder().addResourceSegmentKeys(
                                        ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                            .setResourceSegmentKey("SAS").build()
                                    ).build()
                                ).build()
                            )
                            .build()
                    ),
                secondAccountFF.outerAccountIdInProvider to
                    GrpcResponse.success(
                        UpdateProvisionResponse.newBuilder()
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("storage")
                                            .build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(1510L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("memory")
                                            .build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(3000L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .setAccountsSpaceKey(
                                AccountsSpaceKey.newBuilder().setCompoundKey(
                                    CompoundAccountsSpaceKey.newBuilder().addResourceSegmentKeys(
                                        ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                            .setResourceSegmentKey("VLA").build()
                                    ).build()
                                ).build()
                            )
                            .build()
                    ),
                accountSF.outerAccountIdInProvider to
                    GrpcResponse.success(
                        UpdateProvisionResponse.newBuilder()
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("storage")
                                            .build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(400L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("memory")
                                            .build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(1000L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("cpu")
                                            .build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(2000L).setUnitKey("millicores").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("millicores").build()
                                ).build()
                            )
                            .setAccountsSpaceKey(
                                AccountsSpaceKey.newBuilder().setCompoundKey(
                                    CompoundAccountsSpaceKey.newBuilder().addResourceSegmentKeys(
                                        ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                            .setResourceSegmentKey("SAS").build()
                                    ).build()
                                ).build()
                            )
                            .build()
                    ),
            )
        )
        val responseBody = webClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .post()
            .uri("/api/v1/delivery/_provide")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.OK)
            .expectBody(DeliveryStatusDto::class.java)
            .returnResult()
            .responseBody

        responseBody ?: failOnNull()
        Assertions.assertEquals(body.deliveryId, responseBody.deliveryId)
        val operations = responseBody.operations
        Assertions.assertEquals(3, operations.size)
        operations.forEach { Assertions.assertEquals(OperationStatusDto.SUCCESS, it.status) }
        Assertions.assertEquals(3, stubProviderService.updateProvisionCallCount)
        val updateProvisionRequests = stubProviderService.updateProvisionRequests
        Assertions.assertEquals(3, updateProvisionRequests.size)
        val updateProvisionByAccountId = updateProvisionRequests.associateBy { it.t1.accountId }
        val firstAccountUpdateRequest = updateProvisionByAccountId[firstAccountFF.outerAccountIdInProvider]!!.t1
        Assertions.assertEquals(firstFolder.id, firstAccountUpdateRequest.folderId)
        Assertions.assertEquals(firstFolder.serviceId, firstAccountUpdateRequest.abcServiceId)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, firstAccountUpdateRequest.author.passportUid.passportUid)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_LOGIN, firstAccountUpdateRequest.author.staffLogin.staffLogin)
        Assertions.assertEquals(mapOf(
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(storageResourceType.key)
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(500L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(memoryResourceType.key)
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(1100L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(cpuResourceType.key)
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(2000L)
                .setUnitKey(MILLICORES_KEY)
                .build()
        ), firstAccountUpdateRequest.updatedProvisionsList.associate { it.resourceKey to it.provided })
        Assertions.assertEquals(mapOf(
            firstAccountFF.outerAccountIdInProvider to
                mapOf(
                    ResourceKey.newBuilder()
                        .setCompoundKey(
                            CompoundResourceKey.newBuilder()
                                .setResourceTypeKey(storageResourceType.key)
                                .build()
                        ).build() to Amount.newBuilder()
                        .setValue(100L)
                        .setUnitKey(BYTES_KEY)
                        .build(),
                    ResourceKey.newBuilder()
                        .setCompoundKey(
                            CompoundResourceKey.newBuilder()
                                .setResourceTypeKey(memoryResourceType.key)
                                .build()
                        ).build() to Amount.newBuilder()
                        .setValue(100L)
                        .setUnitKey(BYTES_KEY)
                        .build(),
                    ResourceKey.newBuilder()
                        .setCompoundKey(
                            CompoundResourceKey.newBuilder()
                                .setResourceTypeKey(cpuResourceType.key)
                                .build()
                        ).build() to Amount.newBuilder()
                        .setValue(0L)
                        .setUnitKey(MILLICORES_KEY)
                        .build()
                )
        ), firstAccountUpdateRequest.knownProvisionsList.associate {
            it.accountId to
                it.knownProvisionsList.associate { provision -> provision.resourceKey to provision.provided }
        })
        Assertions.assertEquals(firstAccountFF.providerId, firstAccountUpdateRequest.providerId)
        Assertions.assertEquals(
            AccountsSpaceKey.newBuilder()
                .setCompoundKey(
                    CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(sasSegment.key)
                                .build()
                        )
                        .build()
                )
                .build(), firstAccountUpdateRequest.accountsSpaceKey
        )

        val secondAccountUpdateRequest = updateProvisionByAccountId[secondAccountFF.outerAccountIdInProvider]!!.t1
        Assertions.assertEquals(firstFolder.id, firstAccountUpdateRequest.folderId)
        Assertions.assertEquals(firstFolder.serviceId, secondAccountUpdateRequest.abcServiceId)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, secondAccountUpdateRequest.author.passportUid.passportUid)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_LOGIN, secondAccountUpdateRequest.author.staffLogin.staffLogin)
        Assertions.assertEquals(mapOf(
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(storageResourceType.key)
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(1510L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(memoryResourceType.key)
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(3000L)
                .setUnitKey(BYTES_KEY)
                .build()
        ), secondAccountUpdateRequest.updatedProvisionsList.associate { it.resourceKey to it.provided })
        Assertions.assertEquals(mapOf(
            secondAccountFF.outerAccountIdInProvider to mapOf(
                ResourceKey.newBuilder()
                    .setCompoundKey(
                        CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(storageResourceType.key)
                            .build()
                    ).build() to Amount.newBuilder()
                    .setValue(10L)
                    .setUnitKey(BYTES_KEY)
                    .build(),
                ResourceKey.newBuilder()
                    .setCompoundKey(
                        CompoundResourceKey.newBuilder()
                            .setResourceTypeKey(memoryResourceType.key)
                            .build()
                    ).build() to Amount.newBuilder()
                    .setValue(0L)
                    .setUnitKey(BYTES_KEY)
                    .build()
            )
        ), secondAccountUpdateRequest.knownProvisionsList.associate {
            it.accountId to
                it.knownProvisionsList.associate { provision -> provision.resourceKey to provision.provided }
        })
        Assertions.assertEquals(secondAccountFF.providerId, secondAccountUpdateRequest.providerId)
        Assertions.assertEquals(
            AccountsSpaceKey.newBuilder()
                .setCompoundKey(
                    CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(vlaSegment.key)
                                .build()
                        )
                        .build()
                )
                .build(), secondAccountUpdateRequest.accountsSpaceKey
        )

        val thirdAccountUpdateRequest = updateProvisionByAccountId[accountSF.outerAccountIdInProvider]!!.t1
        Assertions.assertEquals(secondFolder.id, thirdAccountUpdateRequest.folderId)
        Assertions.assertEquals(secondFolder.serviceId, thirdAccountUpdateRequest.abcServiceId)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, thirdAccountUpdateRequest.author.passportUid.passportUid)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_LOGIN, thirdAccountUpdateRequest.author.staffLogin.staffLogin)
        Assertions.assertEquals(mapOf(
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(storageResourceType.key)
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(400L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(memoryResourceType.key)
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(1000L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(cpuResourceType.key)
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(2000L)
                .setUnitKey(MILLICORES_KEY)
                .build()
        ), thirdAccountUpdateRequest.updatedProvisionsList.associate { it.resourceKey to it.provided })
        Assertions.assertTrue(thirdAccountUpdateRequest.knownProvisionsList.associate {
            it.accountId to
                it.knownProvisionsList.associate { provision -> provision.resourceKey to provision.provided }
        }.isEmpty())
        Assertions.assertEquals(accountSF.providerId, thirdAccountUpdateRequest.providerId)
        Assertions.assertEquals(
            AccountsSpaceKey.newBuilder()
                .setCompoundKey(
                    CompoundAccountsSpaceKey.newBuilder()
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(sasSegment.key)
                                .build()
                        )
                        .build()
                )
                .build(), thirdAccountUpdateRequest.accountsSpaceKey
        )

        val folderOperationDtoMap = operations.associate { it.accountId!! to it.folderOperationLogs }
        Assertions.assertEquals(2, folderOperationDtoMap.values.distinct().size)
        val folderOperationLogModels = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getByIds(txSession,
                    folderOperationDtoMap.values.stream()
                        .flatMap { it!!.stream() }
                        .distinct()
                        .map {
                            WithTenant(
                                Tenants.DEFAULT_TENANT_ID,
                                FolderOperationLogModel.Identity(it!!.folderId, it.timestamp, it.id)
                            )
                        }
                        .collect(Collectors.toList())
                )
            }
        }.block()
        folderOperationLogModels ?: failOnNull()
        Assertions.assertEquals(2, folderOperationLogModels.size)
        val folderOperationLogModelMap = folderOperationLogModels.associateBy { it.folderId }
        val logModelFF = folderOperationLogModelMap[firstFolder.id]
        logModelFF ?: failOnNull()
        Assertions.assertEquals(FolderOperationType.QUOTA_DELIVERY, logModelFF.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, logModelFF.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, logModelFF.authorUserUid.get())
        Assertions.assertTrue(logModelFF.authorProviderId.isEmpty)
        Assertions.assertTrue(logModelFF.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(logModelFF.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(logModelFF.oldFolderFields.isEmpty)
        Assertions.assertEquals(
            QuotasByResource(
                mapOf(
                    hddSasResource.id to 300L,
                    hddVlaResource.id to 100L,
                    ramSasResource.id to 200L,
                    ramVlaResource.id to 200L,
                    cpuSasResource.id to 1000L
                )
            ), logModelFF.oldQuotas
        )
        Assertions.assertEquals(QuotasByResource(mapOf()), logModelFF.oldBalance)
        Assertions.assertEquals(QuotasByAccount(mapOf()), logModelFF.oldProvisions)
        Assertions.assertTrue(logModelFF.oldAccounts.isEmpty)
        Assertions.assertTrue(logModelFF.newFolderFields.isEmpty)
        Assertions.assertEquals(
            QuotasByResource(
                mapOf(
                    hddSasResource.id to 300L + 400L,
                    hddVlaResource.id to 100L + 1500L,
                    ramSasResource.id to 200L + 1000L,
                    ramVlaResource.id to 200L + 3000L,
                    cpuSasResource.id to 1000L + 2000L
                )
            ), logModelFF.newQuotas
        )
        Assertions.assertEquals(QuotasByResource(mapOf()), logModelFF.newBalance)
        Assertions.assertEquals(QuotasByAccount(mapOf()), logModelFF.newProvisions)
        Assertions.assertTrue(logModelFF.actuallyAppliedProvisions.isEmpty)
        Assertions.assertTrue(logModelFF.newAccounts.isEmpty)
        Assertions.assertTrue(logModelFF.accountsQuotasOperationsId.isEmpty)
        Assertions.assertTrue(logModelFF.quotasDemandsId.isEmpty)
        Assertions.assertTrue(logModelFF.operationPhase.isEmpty)
        Assertions.assertTrue(logModelFF.commentId.isEmpty)
        Assertions.assertEquals(2, logModelFF.order)
        Assertions.assertEquals(
            DeliverableMetaHistoryModel.builder()
                .quotaRequestId(requestId)
                .campaignId(campaignId)
                .bigOrderIds(bigOrderId)
                .deliveryId(body.deliveryId)
                .build(), logModelFF.deliveryMeta.get()
        )
        Assertions.assertTrue(logModelFF.transferMeta.isEmpty)
        val logModelSF = folderOperationLogModelMap[secondFolder.id]
        logModelSF ?: failOnNull()
        Assertions.assertEquals(FolderOperationType.QUOTA_DELIVERY, logModelSF.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, logModelSF.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, logModelSF.authorUserUid.get())
        Assertions.assertTrue(logModelSF.authorProviderId.isEmpty)
        Assertions.assertTrue(logModelSF.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(logModelSF.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(logModelSF.oldFolderFields.isEmpty)
        Assertions.assertEquals(
            QuotasByResource(
                mapOf(
                    hddSasResource.id to 0L,
                    ramSasResource.id to 0L,
                    cpuSasResource.id to 0L
                )
            ), logModelSF.oldQuotas
        )
        Assertions.assertEquals(QuotasByResource(mapOf()), logModelSF.oldBalance)
        Assertions.assertEquals(QuotasByAccount(mapOf()), logModelSF.oldProvisions)
        Assertions.assertTrue(logModelSF.oldAccounts.isEmpty)
        Assertions.assertTrue(logModelSF.newFolderFields.isEmpty)
        Assertions.assertEquals(
            QuotasByResource(
                mapOf(
                    hddSasResource.id to 400L,
                    ramSasResource.id to 1000L,
                    cpuSasResource.id to 2000L
                )
            ), logModelSF.newQuotas
        )
        Assertions.assertEquals(QuotasByResource(mapOf()), logModelSF.newBalance)
        Assertions.assertEquals(QuotasByAccount(mapOf()), logModelSF.newProvisions)
        Assertions.assertTrue(logModelSF.actuallyAppliedProvisions.isEmpty)
        Assertions.assertTrue(logModelSF.newAccounts.isEmpty)
        Assertions.assertTrue(logModelSF.accountsQuotasOperationsId.isEmpty)
        Assertions.assertTrue(logModelSF.quotasDemandsId.isEmpty)
        Assertions.assertTrue(logModelSF.operationPhase.isEmpty)
        Assertions.assertTrue(logModelSF.commentId.isEmpty)
        Assertions.assertEquals(2, logModelSF.order)
        Assertions.assertEquals(
            DeliverableMetaHistoryModel.builder()
                .quotaRequestId(requestId)
                .campaignId(campaignId)
                .bigOrderIds(bigOrderId)
                .deliveryId(body.deliveryId)
                .build(), logModelSF.deliveryMeta.get()
        )
        Assertions.assertTrue(logModelSF.transferMeta.isEmpty)

        val operationsByFolderLogIdentity = operations.stream()
            .flatMap { it.folderOperationLogs!!.stream() }
            .collect(Collectors.groupingBy {
                FolderOperationLogModel.Identity(
                    it.folderId, it.timestamp, it.id
                )
            })
        Assertions.assertEquals(2, operationsByFolderLogIdentity[logModelFF.identity]!!.size)
        Assertions.assertEquals(1, operationsByFolderLogIdentity[logModelSF.identity]!!.size)

        val operationDtoByAccount = operations.associateBy { it.accountId }
        val firstAccountFFOpp = operationDtoByAccount[firstAccountFF.id]
        firstAccountFFOpp ?: failOnNull()
        Assertions.assertEquals(firstAccountFF.providerId, firstAccountFFOpp.providerId)
        firstAccountFFOpp.updateDateTime ?: failOnNull()
        Assertions.assertNull(firstAccountFFOpp.errorMessage)
        Assertions.assertNull(firstAccountFFOpp.errorKind)
        var requestedQuota = listOf(
            DeliveryStatusRequestedQuotasDto(
                hddSasResource.id, AmountDto(
                    "400",
                    "B",
                    "400",
                    "B",
                    "400",
                    BYTES,
                    "400",
                    BYTES
                )
            ),
            DeliveryStatusRequestedQuotasDto(
                ramSasResource.id, AmountDto(
                    "1",
                    "KB",
                    "1000",
                    "B",
                    "1",
                    KILOBYTES,
                    "1000",
                    BYTES
                )
            ),
            DeliveryStatusRequestedQuotasDto(
                cpuSasResource.id, AmountDto(
                    "2",
                    "cores",
                    "2000",
                    "mCores",
                    "2",
                    CORES,
                    "2000",
                    MILLICORES
                )
            )
        )
        Assertions.assertTrue(requestedQuota.size == firstAccountFFOpp.requestedQuotas.size
            && requestedQuota.containsAll(firstAccountFFOpp.requestedQuotas)
            && firstAccountFFOpp.requestedQuotas.containsAll(requestedQuota))
        Assertions.assertEquals(
            DeliveryAndProvideMetaResponseDto.Builder()
                .quotaRequestId(requestId)
                .campaignId(campaignId)
                .addBigOrderId(bigOrderId)
                .build(), firstAccountFFOpp.meta!!
        )
        Assertions.assertEquals(
            DeliverableFolderOperationDto.builder()
                .folderId(logModelFF.identity.folderId)
                .timestamp(logModelFF.identity.operationDateTime)
                .id(logModelFF.identity.id)
                .build(), firstAccountFFOpp.folderOperationLogs!!.stream().findFirst().orElseThrow()
        )

        val secondAccountFFOpp = operationDtoByAccount[secondAccountFF.id]
        secondAccountFFOpp ?: failOnNull()
        Assertions.assertEquals(firstAccountFF.providerId, secondAccountFFOpp.providerId)
        secondAccountFFOpp.updateDateTime ?: failOnNull()
        Assertions.assertNull(secondAccountFFOpp.errorMessage)
        Assertions.assertNull(secondAccountFFOpp.errorKind)
        requestedQuota =             listOf(
            DeliveryStatusRequestedQuotasDto(
                hddVlaResource.id, AmountDto(
                    "1.5",
                    "KB",
                    "1500",
                    "B",
                    "1.5",
                    KILOBYTES,
                    "1500",
                    BYTES
                )
            ),
            DeliveryStatusRequestedQuotasDto(
                ramVlaResource.id, AmountDto(
                    "3",
                    "KB",
                    "3000",
                    "B",
                    "3",
                    KILOBYTES,
                    "3000",
                    BYTES
                )
            )
        )
        Assertions.assertTrue(requestedQuota.size == secondAccountFFOpp.requestedQuotas.size
            && requestedQuota.containsAll(secondAccountFFOpp.requestedQuotas)
            && secondAccountFFOpp.requestedQuotas.containsAll(requestedQuota))
        Assertions.assertEquals(
            DeliveryAndProvideMetaResponseDto.Builder()
                .quotaRequestId(requestId)
                .campaignId(campaignId)
                .addBigOrderId(bigOrderId)
                .build(), secondAccountFFOpp.meta!!
        )
        Assertions.assertEquals(
            DeliverableFolderOperationDto.builder()
                .folderId(logModelFF.identity.folderId)
                .timestamp(logModelFF.identity.operationDateTime)
                .id(logModelFF.identity.id)
                .build(), secondAccountFFOpp.folderOperationLogs!!.stream().findFirst().orElseThrow()
        )

        val accountSFOpp = operationDtoByAccount[accountSF.id]
        accountSFOpp ?: failOnNull()
        Assertions.assertEquals(accountSF.providerId, accountSFOpp.providerId)
        accountSFOpp.updateDateTime ?: failOnNull()
        Assertions.assertNull(accountSFOpp.errorMessage)
        Assertions.assertNull(accountSFOpp.errorKind)
        requestedQuota = listOf(
            DeliveryStatusRequestedQuotasDto(
                hddSasResource.id, AmountDto(
                    "400",
                    "B",
                    "400",
                    "B",
                    "400",
                    BYTES,
                    "400",
                    BYTES
                )
            ),
            DeliveryStatusRequestedQuotasDto(
                ramSasResource.id, AmountDto(
                    "1",
                    "KB",
                    "1000",
                    "B",
                    "1",
                    KILOBYTES,
                    "1000",
                    BYTES
                )
            ),
            DeliveryStatusRequestedQuotasDto(
                cpuSasResource.id, AmountDto(
                    "2",
                    "cores",
                    "2000",
                    "mCores",
                    "2",
                    CORES,
                    "2000",
                    MILLICORES
                )
            ),
        )
        Assertions.assertTrue(requestedQuota.size == accountSFOpp.requestedQuotas.size
            && requestedQuota.containsAll(accountSFOpp.requestedQuotas)
            && accountSFOpp.requestedQuotas.containsAll(requestedQuota))
        Assertions.assertEquals(
            DeliveryAndProvideMetaResponseDto.Builder()
                .quotaRequestId(requestId)
                .campaignId(campaignId)
                .addBigOrderId(bigOrderId)
                .build(), accountSFOpp.meta!!
        )
        Assertions.assertEquals(
            DeliverableFolderOperationDto.builder()
                .folderId(logModelSF.identity.folderId)
                .timestamp(logModelSF.identity.operationDateTime)
                .id(logModelSF.identity.id)
                .build(), accountSFOpp.folderOperationLogs!!.stream().findFirst().orElseThrow()
        )

        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(
                    txSession, listOf(firstFolder.id, secondFolder.id), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        updatedQuotas ?: failOnNull()
        val quotaByResourceIdByFolder = mutableMapOf<String, MutableMap<String, QuotaModel>>()
        for (quota in updatedQuotas) {
            val orDefault = quotaByResourceIdByFolder.getOrPut(quota.folderId) { mutableMapOf() }
            orDefault[quota.resourceId] = quota
        }

        Assertions.assertEquals(
            300L + 400L,
            quotaByResourceIdByFolder[firstFolder.id]!![hddSasResource.id]!!.quota
        )
        Assertions.assertEquals(
            100L + 1500L,
            quotaByResourceIdByFolder[firstFolder.id]!![hddVlaResource.id]!!.quota
        )
        Assertions.assertEquals(
            200L + 1000L,
            quotaByResourceIdByFolder[firstFolder.id]!![ramSasResource.id]!!.quota
        )
        Assertions.assertEquals(
            200L + 3000L,
            quotaByResourceIdByFolder[firstFolder.id]!![ramVlaResource.id]!!.quota
        )
        Assertions.assertEquals(
            1000L + 2000L,
            quotaByResourceIdByFolder[firstFolder.id]!![cpuSasResource.id]!!.quota
        )
        Assertions.assertEquals(
            2000L,
            quotaByResourceIdByFolder[firstFolder.id]!![cpuVlaResource.id]!!.quota
        )

        Assertions.assertEquals(
            200L,
            quotaByResourceIdByFolder[firstFolder.id]!![hddSasResource.id]!!.balance
        )
        Assertions.assertEquals(
            90L,
            quotaByResourceIdByFolder[firstFolder.id]!![hddVlaResource.id]!!.balance
        )
        Assertions.assertEquals(
            100L,
            quotaByResourceIdByFolder[firstFolder.id]!![ramSasResource.id]!!.balance
        )
        Assertions.assertEquals(
            200L,
            quotaByResourceIdByFolder[firstFolder.id]!![ramVlaResource.id]!!.balance
        )
        Assertions.assertEquals(
            1000L,
            quotaByResourceIdByFolder[firstFolder.id]!![cpuSasResource.id]!!.balance
        )
        Assertions.assertEquals(
            2000L,
            quotaByResourceIdByFolder[firstFolder.id]!![cpuVlaResource.id]!!.balance
        )

        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[firstFolder.id]!![hddSasResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[firstFolder.id]!![hddVlaResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[firstFolder.id]!![ramSasResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[firstFolder.id]!![ramVlaResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[firstFolder.id]!![cpuSasResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[firstFolder.id]!![cpuVlaResource.id]!!.frozenQuota
        )

        Assertions.assertEquals(
            400L,
            quotaByResourceIdByFolder[secondFolder.id]!![hddSasResource.id]!!.quota
        )
        Assertions.assertEquals(
            1000L,
            quotaByResourceIdByFolder[secondFolder.id]!![ramSasResource.id]!!.quota
        )
        Assertions.assertEquals(
            2000L,
            quotaByResourceIdByFolder[secondFolder.id]!![cpuSasResource.id]!!.quota
        )

        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![hddSasResource.id]!!.balance
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![ramSasResource.id]!!.balance
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![cpuSasResource.id]!!.balance
        )

        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![hddSasResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![ramSasResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![cpuSasResource.id]!!.frozenQuota
        )

        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(
                    txSession, Tenants.DEFAULT_TENANT_ID, setOf(firstAccountFF.id, secondAccountFF.id, accountSF.id)
                )
            }
        }.block()
        updatedProvisions ?: failOnNull()
        val provisionByResourceIdByAccount = mutableMapOf<String, MutableMap<String, AccountsQuotasModel>>()
        for (provision in updatedProvisions) {
            val orDefault = provisionByResourceIdByAccount.getOrPut(provision.accountId) { mutableMapOf() }
            orDefault[provision.resourceId] = provision
        }

        var provisionByResourceId = provisionByResourceIdByAccount[firstAccountFF.id]
        provisionByResourceId ?: failOnNull()
        Assertions.assertEquals(
            100L + 400L,
            provisionByResourceId[hddSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            50L,
            provisionByResourceId[hddSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[hddSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[firstAccountFF.id]!!.operationId,
            provisionByResourceId[hddSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddSasResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            100L + 1000L,
            provisionByResourceId[ramSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[ramSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[firstAccountFF.id]!!.operationId,
            provisionByResourceId[ramSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramSasResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            2000,
            provisionByResourceId[cpuSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[cpuSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[cpuSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[firstAccountFF.id]!!.operationId,
            provisionByResourceId[cpuSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[cpuSasResource.id]!!.frozenProvidedQuota
        )

        provisionByResourceId = provisionByResourceIdByAccount[secondAccountFF.id]
        provisionByResourceId ?: failOnNull()
        Assertions.assertEquals(
            1510L,
            provisionByResourceId[hddVlaResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddVlaResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[hddVlaResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[secondAccountFF.id]!!.operationId,
            provisionByResourceId[hddVlaResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddVlaResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            3000,
            provisionByResourceId[ramVlaResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramVlaResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[ramVlaResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[secondAccountFF.id]!!.operationId,
            provisionByResourceId[ramVlaResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramVlaResource.id]!!.frozenProvidedQuota
        )

        provisionByResourceId = provisionByResourceIdByAccount[accountSF.id]
        provisionByResourceId ?: failOnNull()
        Assertions.assertEquals(
            400L,
            provisionByResourceId[hddSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[hddSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[accountSF.id]!!.operationId,
            provisionByResourceId[hddSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddSasResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            1000L,
            provisionByResourceId[ramSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[ramSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[accountSF.id]!!.operationId,
            provisionByResourceId[ramSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramSasResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            2000,
            provisionByResourceId[cpuSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[cpuSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[cpuSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[accountSF.id]!!.operationId,
            provisionByResourceId[cpuSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[cpuSasResource.id]!!.frozenProvidedQuota
        )

        val updatedFolders = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.getByIds(
                    txSession, listOf(firstFolder.id, secondFolder.id), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()

        updatedFolders ?: failOnNull()
        val updatedFoldersById = updatedFolders.associateBy { it.id }
        Assertions.assertEquals(7, updatedFoldersById[firstFolder.id]!!.nextOpLogOrder)
        Assertions.assertEquals(5, updatedFoldersById[secondFolder.id]!!.nextOpLogOrder)

        val operationInProgress = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenantFolders(
                    txSession, Tenants.DEFAULT_TENANT_ID, setOf(firstFolder.id, secondFolder.id)
                )
            }
        }.block()
        operationInProgress ?: failOnNull()
        Assertions.assertTrue(operationInProgress.isEmpty())

        val accountQuotaOperations = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getByIds(
                    txSession, operations.map { it.operationId }.toList(), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        accountQuotaOperations ?: failOnNull()
        Assertions.assertEquals(3, accountQuotaOperations.size)
        val accountsOperationsByAccountId = accountQuotaOperations.associateBy { it.requestedChanges.accountId.get() }
        var accountOperation = accountsOperationsByAccountId[firstAccountFF.id]
        accountOperation ?: failOnNull()
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.OperationType.DELIVER_AND_UPDATE_PROVISION,
            accountOperation.operationType
        )
        Assertions.assertEquals(OperationSource.USER, accountOperation.operationSource)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, accountOperation.authorUserId)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, accountOperation.authorUserUid.get())
        Assertions.assertEquals(firstAccountFF.providerId, accountOperation.providerId)
        Assertions.assertEquals(sasAccountsSpace.id, accountOperation.accountsSpaceId.get())
        Assertions.assertTrue(accountOperation.updateDateTime.isPresent)
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.OK, accountOperation.requestStatus.get())
        Assertions.assertTrue(accountOperation.errorMessage.isEmpty)
        Assertions.assertTrue(accountOperation.fullErrorMessage.isEmpty)
        Assertions.assertTrue(accountOperation.errorKind.isEmpty)
        assertEquals(
            OperationChangesModel.builder()
                .accountId(firstAccountFF.id)
                .deliveryId(body.deliveryId)
                .updatedProvisions(
                    listOf(
                        OperationChangesModel.Provision(hddSasResource.id, 100L + 400L),
                        OperationChangesModel.Provision(ramSasResource.id, 100L + 1000L),
                        OperationChangesModel.Provision(cpuSasResource.id, 2000L)
                    )
                )
                .frozenProvisions(
                    listOf(
                        OperationChangesModel.Provision(hddSasResource.id, 400L),
                        OperationChangesModel.Provision(ramSasResource.id, 1000L),
                        OperationChangesModel.Provision(cpuSasResource.id, 2000L)
                    )
                )
                .build(), accountOperation.requestedChanges
        )
        val submitOrder = accountOperation.orders.submitOrder
        val closeOrder = accountOperation.orders.closeOrder.get()
        Assertions.assertTrue(submitOrder == 3L || submitOrder == 4L)
        Assertions.assertTrue(closeOrder == 5L || closeOrder == 6L)

        accountOperation = accountsOperationsByAccountId[secondAccountFF.id]
        accountOperation ?: failOnNull()
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.OperationType.DELIVER_AND_UPDATE_PROVISION,
            accountOperation.operationType
        )
        Assertions.assertEquals(OperationSource.USER, accountOperation.operationSource)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, accountOperation.authorUserId)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, accountOperation.authorUserUid.get())
        Assertions.assertEquals(secondAccountFF.providerId, accountOperation.providerId)
        Assertions.assertEquals(vlaAccountsSpace.id, accountOperation.accountsSpaceId.get())
        Assertions.assertTrue(accountOperation.updateDateTime.isPresent)
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.OK, accountOperation.requestStatus.get())
        Assertions.assertTrue(accountOperation.errorMessage.isEmpty)
        Assertions.assertTrue(accountOperation.fullErrorMessage.isEmpty)
        Assertions.assertTrue(accountOperation.errorKind.isEmpty)
        assertEquals(
            OperationChangesModel.builder()
                .accountId(secondAccountFF.id)
                .deliveryId(body.deliveryId)
                .updatedProvisions(
                    listOf(
                        OperationChangesModel.Provision(hddVlaResource.id, 10L + 1500L),
                        OperationChangesModel.Provision(ramVlaResource.id, 3000L)
                    )
                )
                .frozenProvisions(
                    listOf(
                        OperationChangesModel.Provision(hddVlaResource.id, 1500L),
                        OperationChangesModel.Provision(ramVlaResource.id, 3000L)
                    )
                )
                .build(), accountOperation.requestedChanges
        )
        Assertions.assertTrue(
            (accountOperation.orders.submitOrder == 3L
                && accountOperation.orders.submitOrder != submitOrder)
                || (accountOperation.orders.submitOrder == 4L
                && accountOperation.orders.submitOrder != submitOrder)
        )
        Assertions.assertTrue(
            (accountOperation.orders.closeOrder.get() == 5L
                && accountOperation.orders.closeOrder.get() != closeOrder)
                || (accountOperation.orders.closeOrder.get() == 6L
                && accountOperation.orders.closeOrder.get() != closeOrder)
        )

        accountOperation = accountsOperationsByAccountId[accountSF.id]
        accountOperation ?: failOnNull()
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.OperationType.DELIVER_AND_UPDATE_PROVISION,
            accountOperation.operationType
        )
        Assertions.assertEquals(OperationSource.USER, accountOperation.operationSource)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, accountOperation.authorUserId)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, accountOperation.authorUserUid.get())
        Assertions.assertEquals(accountSF.providerId, accountOperation.providerId)
        Assertions.assertEquals(sasAccountsSpace.id, accountOperation.accountsSpaceId.get())
        Assertions.assertTrue(accountOperation.updateDateTime.isPresent)
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.OK, accountOperation.requestStatus.get())
        Assertions.assertTrue(accountOperation.errorMessage.isEmpty)
        Assertions.assertTrue(accountOperation.fullErrorMessage.isEmpty)
        Assertions.assertTrue(accountOperation.errorKind.isEmpty)
        assertEquals(
            OperationChangesModel.builder()
                .accountId(accountSF.id)
                .deliveryId(body.deliveryId)
                .updatedProvisions(
                    listOf(
                        OperationChangesModel.Provision(hddSasResource.id, 400L),
                        OperationChangesModel.Provision(ramSasResource.id, 1000L),
                        OperationChangesModel.Provision(cpuSasResource.id, 2000L)
                    )
                )
                .frozenProvisions(
                    listOf(
                        OperationChangesModel.Provision(hddSasResource.id, 400L),
                        OperationChangesModel.Provision(ramSasResource.id, 1000L),
                        OperationChangesModel.Provision(cpuSasResource.id, 2000L)
                    )
                )
                .build(), accountOperation.requestedChanges
        )
        Assertions.assertEquals(
            OperationOrdersModel.builder()
                .submitOrder(3L)
                .closeOrder(4L)
                .build(), accountOperation.orders
        )

        val firstFolderOppLogs = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, firstFolder.id, SortOrderDto.ASC, 10
                )
            }
        }.block()
        firstFolderOppLogs ?: failOnNull()
        Assertions.assertEquals(5, firstFolderOppLogs.size)
        val firstFolderLogsByAccountByPhase =
            mutableMapOf<String, MutableMap<OperationPhase, FolderOperationLogModel>>()
        for (log in firstFolderOppLogs) {
            if (log.operationType == FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT) {
                val orPut =
                    firstFolderLogsByAccountByPhase.getOrPut(log.accountsQuotasOperationsId.get()) { mutableMapOf() }
                orPut[log.operationPhase.get()] = log
            }
        }
        val firstAccountFFOperation = accountsOperationsByAccountId[firstAccountFF.id]
        firstAccountFFOperation ?: failOnNull()
        var folderLog = firstFolderLogsByAccountByPhase[firstAccountFFOperation.operationId]!![OperationPhase.SUBMIT]
        folderLog ?: failOnNull()
        Assertions.assertEquals(firstAccountFFOperation.lastRequestId.get(), folderLog.providerRequestId.get())
        Assertions.assertEquals(FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, folderLog.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, folderLog.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, folderLog.authorUserUid.get())
        Assertions.assertTrue(folderLog.authorProviderId.isEmpty)
        Assertions.assertTrue(folderLog.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.oldFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    firstAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(100L, null),
                            ramSasResource.id to ProvisionHistoryModel(100L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L, null)
                        )
                    )
                )
            ), folderLog.oldProvisions
        )
        Assertions.assertTrue(folderLog.oldAccounts.isEmpty)
        Assertions.assertTrue(folderLog.newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    firstAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(100L + 400L, 0L),
                            ramSasResource.id to ProvisionHistoryModel(100L + 1000L, 0L),
                            cpuSasResource.id to ProvisionHistoryModel(0L + 2000L, 0L)
                        )
                    )
                )
            ), folderLog.newProvisions
        )
        Assertions.assertTrue(folderLog.actuallyAppliedProvisions.isEmpty)
        Assertions.assertTrue(folderLog.newAccounts.isEmpty)
        Assertions.assertTrue(folderLog.quotasDemandsId.isEmpty)
        Assertions.assertTrue(folderLog.commentId.isEmpty)
        Assertions.assertEquals(DeliverableMetaHistoryModel.builder()
            .quotaRequestId(requestId)
            .campaignId(campaignId)
            .bigOrderIds(bigOrderId)
            .deliveryId(body.deliveryId)
            .build(), folderLog.deliveryMeta.get())
        Assertions.assertTrue(folderLog.transferMeta.isEmpty)
        Assertions.assertEquals(firstAccountFFOperation.orders.submitOrder, folderLog.order)

        folderLog = firstFolderLogsByAccountByPhase[firstAccountFFOperation.operationId]!![OperationPhase.CLOSE]
        folderLog ?: failOnNull()
        Assertions.assertTrue(folderLog.providerRequestId.isEmpty)
        Assertions.assertEquals(FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, folderLog.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, folderLog.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, folderLog.authorUserUid.get())
        Assertions.assertTrue(folderLog.authorProviderId.isEmpty)
        Assertions.assertTrue(folderLog.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.oldFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    firstAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(100L, null),
                            ramSasResource.id to ProvisionHistoryModel(100L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L, null)
                        )
                    )
                )
            ), folderLog.oldProvisions
        )
        Assertions.assertTrue(folderLog.oldAccounts.isEmpty)
        Assertions.assertTrue(folderLog.newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    firstAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(100L + 400L, null),
                            ramSasResource.id to ProvisionHistoryModel(100L + 1000L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L + 2000L, null)
                        )
                    )
                )
            ), folderLog.newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    firstAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(100L + 400L, null),
                            ramSasResource.id to ProvisionHistoryModel(100L + 1000L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L + 2000L, null)
                        )
                    )
                )
            ), folderLog.actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(folderLog.newAccounts.isEmpty)
        Assertions.assertTrue(folderLog.quotasDemandsId.isEmpty)
        Assertions.assertTrue(folderLog.commentId.isEmpty)
        Assertions.assertTrue(folderLog.deliveryMeta.isEmpty)
        Assertions.assertTrue(folderLog.transferMeta.isEmpty)
        Assertions.assertEquals(firstAccountFFOperation.orders.closeOrder.get(), folderLog.order)

        val secondAccountFFOperation = accountsOperationsByAccountId[secondAccountFF.id]
        secondAccountFFOperation ?: failOnNull()
        folderLog = firstFolderLogsByAccountByPhase[secondAccountFFOperation.operationId]!![OperationPhase.SUBMIT]
        folderLog ?: failOnNull()
        Assertions.assertEquals(secondAccountFFOperation.lastRequestId.get(), folderLog.providerRequestId.get())
        Assertions.assertEquals(FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, folderLog.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, folderLog.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, folderLog.authorUserUid.get())
        Assertions.assertTrue(folderLog.authorProviderId.isEmpty)
        Assertions.assertTrue(folderLog.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.oldFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    secondAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddVlaResource.id to ProvisionHistoryModel(10L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L, null)
                        )
                    )
                )
            ), folderLog.oldProvisions
        )
        Assertions.assertTrue(folderLog.oldAccounts.isEmpty)
        Assertions.assertTrue(folderLog.newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    secondAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddVlaResource.id to ProvisionHistoryModel(10L + 1500L, 0L),
                            ramVlaResource.id to ProvisionHistoryModel(0L + 3000L, 0L)
                        )
                    )
                )
            ), folderLog.newProvisions
        )
        Assertions.assertTrue(folderLog.actuallyAppliedProvisions.isEmpty)
        Assertions.assertTrue(folderLog.newAccounts.isEmpty)
        Assertions.assertTrue(folderLog.quotasDemandsId.isEmpty)
        Assertions.assertTrue(folderLog.commentId.isEmpty)
        Assertions.assertEquals(DeliverableMetaHistoryModel.builder()
            .quotaRequestId(requestId)
            .campaignId(campaignId)
            .bigOrderIds(bigOrderId)
            .deliveryId(body.deliveryId)
            .build(), folderLog.deliveryMeta.get())
        Assertions.assertTrue(folderLog.transferMeta.isEmpty)
        Assertions.assertEquals(secondAccountFFOperation.orders.submitOrder, folderLog.order)

        folderLog = firstFolderLogsByAccountByPhase[secondAccountFFOperation.operationId]!![OperationPhase.CLOSE]
        folderLog ?: failOnNull()
        Assertions.assertTrue(folderLog.providerRequestId.isEmpty)
        Assertions.assertEquals(FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, folderLog.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, folderLog.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, folderLog.authorUserUid.get())
        Assertions.assertTrue(folderLog.authorProviderId.isEmpty)
        Assertions.assertTrue(folderLog.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.oldFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    secondAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddVlaResource.id to ProvisionHistoryModel(10L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L, null)
                        )
                    )
                )
            ), folderLog.oldProvisions
        )
        Assertions.assertTrue(folderLog.oldAccounts.isEmpty)
        Assertions.assertTrue(folderLog.newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    secondAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddVlaResource.id to ProvisionHistoryModel(10L + 1500L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L + 3000L, null)
                        )
                    )
                )
            ), folderLog.newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    secondAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddVlaResource.id to ProvisionHistoryModel(10L + 1500L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L + 3000L, null)
                        )
                    )
                )
            ), folderLog.actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(folderLog.newAccounts.isEmpty)
        Assertions.assertTrue(folderLog.quotasDemandsId.isEmpty)
        Assertions.assertTrue(folderLog.commentId.isEmpty)
        Assertions.assertTrue(folderLog.deliveryMeta.isEmpty)
        Assertions.assertTrue(folderLog.transferMeta.isEmpty)
        Assertions.assertEquals(secondAccountFFOperation.orders.closeOrder.get(), folderLog.order)

        val secondFolderOppLogs = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, secondFolder.id, SortOrderDto.ASC, 10
                )
            }
        }.block()
        secondFolderOppLogs ?: failOnNull()
        Assertions.assertEquals(3, secondFolderOppLogs.size)
        val secondFolderLogsByAccountByPhase =
            mutableMapOf<String, MutableMap<OperationPhase, FolderOperationLogModel>>()
        for (log in secondFolderOppLogs) {
            if (log.operationType == FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT) {
                val orPut =
                    secondFolderLogsByAccountByPhase.getOrPut(log.accountsQuotasOperationsId.get()) { mutableMapOf() }
                orPut[log.operationPhase.get()] = log
            }
        }

        val accountSFOperation = accountsOperationsByAccountId[accountSF.id]
        accountSFOperation ?: failOnNull()
        folderLog = secondFolderLogsByAccountByPhase[accountSFOperation.operationId]!![OperationPhase.SUBMIT]
        folderLog ?: failOnNull()
        Assertions.assertEquals(accountSFOperation.lastRequestId.get(), folderLog.providerRequestId.get())
        Assertions.assertEquals(FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, folderLog.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, folderLog.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, folderLog.authorUserUid.get())
        Assertions.assertTrue(folderLog.authorProviderId.isEmpty)
        Assertions.assertTrue(folderLog.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.oldFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    accountSF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(0L, null),
                            ramSasResource.id to ProvisionHistoryModel(0L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L, null)
                        )
                    )
                )
            ), folderLog.oldProvisions
        )
        Assertions.assertTrue(folderLog.oldAccounts.isEmpty)
        Assertions.assertTrue(folderLog.newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    accountSF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(0L + 400L, 0L),
                            ramSasResource.id to ProvisionHistoryModel(0L + 1000L, 0L),
                            cpuSasResource.id to ProvisionHistoryModel(0L + 2000L, 0L)
                        )
                    )
                )
            ), folderLog.newProvisions
        )
        Assertions.assertTrue(folderLog.actuallyAppliedProvisions.isEmpty)
        Assertions.assertTrue(folderLog.newAccounts.isEmpty)
        Assertions.assertTrue(folderLog.quotasDemandsId.isEmpty)
        Assertions.assertTrue(folderLog.commentId.isEmpty)
        Assertions.assertEquals(DeliverableMetaHistoryModel.builder()
            .quotaRequestId(requestId)
            .campaignId(campaignId)
            .bigOrderIds(bigOrderId)
            .deliveryId(body.deliveryId)
            .build(), folderLog.deliveryMeta.get())
        Assertions.assertTrue(folderLog.transferMeta.isEmpty)
        Assertions.assertEquals(accountSFOperation.orders.submitOrder, folderLog.order)

        folderLog = secondFolderLogsByAccountByPhase[accountSFOperation.operationId]!![OperationPhase.CLOSE]
        folderLog ?: failOnNull()
        Assertions.assertTrue(folderLog.providerRequestId.isEmpty)
        Assertions.assertEquals(FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, folderLog.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, folderLog.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, folderLog.authorUserUid.get())
        Assertions.assertTrue(folderLog.authorProviderId.isEmpty)
        Assertions.assertTrue(folderLog.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.oldFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    accountSF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(0L, null),
                            ramSasResource.id to ProvisionHistoryModel(0L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L, null)
                        )
                    )
                )
            ), folderLog.oldProvisions
        )
        Assertions.assertTrue(folderLog.oldAccounts.isEmpty)
        Assertions.assertTrue(folderLog.newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    accountSF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(0L + 400L, null),
                            ramSasResource.id to ProvisionHistoryModel(0L + 1000L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L + 2000L, null)
                        )
                    )
                )
            ), folderLog.newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    accountSF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(0L + 400L, null),
                            ramSasResource.id to ProvisionHistoryModel(0L + 1000L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L + 2000L, null)
                        )
                    )
                )
            ), folderLog.actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(folderLog.newAccounts.isEmpty)
        Assertions.assertTrue(folderLog.quotasDemandsId.isEmpty)
        Assertions.assertTrue(folderLog.commentId.isEmpty)
        Assertions.assertTrue(folderLog.deliveryMeta.isEmpty)
        Assertions.assertTrue(folderLog.transferMeta.isEmpty)
        Assertions.assertEquals(accountSFOperation.orders.closeOrder.get(), folderLog.order)
    }

    @Test
    fun testSuccessDeliverAndProvideWithoutAccountSpaceDifferentBigOrders() {
        val provider = providerModel(GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true)
        val firstFolder = folderModel(1L, "First folder", "First folder description")
        val secondFolder = folderModel(1L, "Second folder", "Second folder description")
        val storageResourceType = resourceTypeModel(provider.id, "storage", STORAGE_UNITS_DECIMAL_ID)
        val memoryResourceType = resourceTypeModel(provider.id, "memory", STORAGE_UNITS_DECIMAL_ID)
        val cpuResourceType = resourceTypeModel(provider.id, "cpu", CPU_UNITS_ID)
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val sasSegment = resourceSegmentModel(locationSegmentation.id, "SAS")
        val hddSasResource = resourceModel(
            provider.id,
            "hddSas",
            storageResourceType.id,
            setOf(Tuples.of(locationSegmentation.id, sasSegment.id)),
            STORAGE_UNITS_DECIMAL_ID,
            setOf(
                BYTES,
                KILOBYTES,
                MEGABYTES,
                GIGABYTES
            ),
            GIGABYTES,
            BYTES,
            null
        )
        val hddVlaResource = resourceModel(
            provider.id,
            "hddVla",
            storageResourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            STORAGE_UNITS_DECIMAL_ID,
            setOf(
                BYTES,
                KILOBYTES,
                MEGABYTES,
                GIGABYTES
            ),
            GIGABYTES,
            BYTES,
            null
        )
        val ramSasResource = resourceModel(
            provider.id,
            "ramSas",
            memoryResourceType.id,
            setOf(Tuples.of(locationSegmentation.id, sasSegment.id)),
            STORAGE_UNITS_DECIMAL_ID,
            setOf(
                BYTES,
                KILOBYTES,
                MEGABYTES,
                GIGABYTES
            ),
            GIGABYTES,
            BYTES,
            null
        )
        val ramVlaResource = resourceModel(
            provider.id,
            "ramVla",
            memoryResourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            STORAGE_UNITS_DECIMAL_ID,
            setOf(
                BYTES,
                KILOBYTES,
                MEGABYTES,
                GIGABYTES
            ),
            GIGABYTES,
            BYTES,
            null
        )
        val cpuSasResource = resourceModel(
            provider.id,
            "cpuSas",
            cpuResourceType.id,
            setOf(Tuples.of(locationSegmentation.id, sasSegment.id)),
            CPU_UNITS_ID,
            setOf(
                MILLICORES,
                CORES
            ),
            CORES,
            MILLICORES,
            null
        )
        val cpuVlaResource = resourceModel(
            provider.id,
            "cpuVla",
            cpuResourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            CPU_UNITS_ID,
            setOf(
                MILLICORES,
                CORES
            ),
            CORES,
            MILLICORES,
            null
        )
        val firstAccountFF = accountModel(
            provider.id, null, "first-account-first-folder",
            "first-account-first-folder", firstFolder.id, "first-account-first-folder", null, null
        )
        val secondAccountFF = accountModel(
            provider.id, null, "second-account-first-folder",
            "second-account-first-folder", firstFolder.id, "second-account-first-folder", null, null
        )
        val accountSF = accountModel(
            provider.id, null, "account-second-folder",
            "account-second-folder", secondFolder.id, "account-second-folder", null, null
        )
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertAllRetryable(txSession, listOf(firstFolder, secondFolder))
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypesRetryable(
                    txSession,
                    listOf(storageResourceType, memoryResourceType, cpuResourceType)
                )
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession, listOf(vlaSegment, sasSegment))
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourcesRetryable(
                    txSession, listOf(
                        hddSasResource, hddVlaResource, ramSasResource,
                        ramVlaResource, cpuSasResource, cpuVlaResource
                    )
                )
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertAllRetryable(txSession, listOf(firstAccountFF, secondAccountFF, accountSF))
            }
        }.block()
        val requestId = 143L
        val campaignId = 40L
        val bigOrderOneId = 44L
        val bigOrderTwoId = 45L
        val bigOrderThreeId = 46L
        val body = DeliveryAndProvideRequestDto.Builder()
            .deliveryId(UUID.randomUUID().toString())
            .authorUid(DISPENSER_QUOTA_MANAGER_UID)
            // FF FA bigOrderOne
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(firstAccountFF.id)
                    .resourceId(hddSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(100)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderOneId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(firstAccountFF.id)
                    .resourceId(hddVlaResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(100)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderOneId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(firstAccountFF.id)
                    .resourceId(ramSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(100)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderOneId)
                            .build()
                    )
                    .build()
            )
            // FF FA bigOrderTwo
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(firstAccountFF.id)
                    .resourceId(hddSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(200)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderTwoId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(firstAccountFF.id)
                    .resourceId(hddVlaResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(200)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderTwoId)
                            .build()
                    )
                    .build()
            )
            // FF FA bigOrderThree
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(firstAccountFF.id)
                    .resourceId(hddVlaResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(300)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderThreeId)
                            .build()
                    )
                    .build()
            )
            // FF SA bigOrderOne
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(secondAccountFF.id)
                    .resourceId(ramSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(100)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderOneId)
                            .build()
                    )
                    .build()
            )
            // FF SA bigOrderTwo
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(secondAccountFF.id)
                    .resourceId(ramSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(100)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderTwoId)
                            .build()
                    )
                    .build()
            )
            // FF SA bigOrderThree
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(firstFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(firstFolder.id)
                    .accountId(secondAccountFF.id)
                    .resourceId(ramSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(100)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderThreeId)
                            .build()
                    )
                    .build()
            )
            // SF bigOrderOne
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(secondFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(secondFolder.id)
                    .accountId(accountSF.id)
                    .resourceId(hddSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(400)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderOneId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(secondFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(secondFolder.id)
                    .accountId(accountSF.id)
                    .resourceId(hddVlaResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(500)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderOneId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(secondFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(secondFolder.id)
                    .accountId(accountSF.id)
                    .resourceId(ramSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(1000)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderOneId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(secondFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(secondFolder.id)
                    .accountId(accountSF.id)
                    .resourceId(ramVlaResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(2000)
                            .unitKey(BYTES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderOneId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(secondFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(secondFolder.id)
                    .accountId(accountSF.id)
                    .resourceId(cpuSasResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(2000)
                            .unitKey(MILLICORES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderOneId)
                            .build()
                    )
                    .build()
            )
            .addDeliverable(
                DeliveryAndProvideDestinationDto.Builder()
                    .serviceId(secondFolder.serviceId)
                    .providerId(provider.id)
                    .folderId(secondFolder.id)
                    .accountId(accountSF.id)
                    .resourceId(cpuVlaResource.id)
                    .delta(
                        DeliverableDeltaDto.builder()
                            .amount(3000)
                            .unitKey(MILLICORES_KEY)
                            .build()
                    )
                    .meta(
                        DeliverableMetaRequestDto.builder()
                            .quotaRequestId(requestId)
                            .campaignId(campaignId)
                            .bigOrderId(bigOrderOneId)
                            .build()
                    )
                    .build()
            )
            .build()
        stubProviderService.setUpdateProvisionResponseByMappingFunction(true)
        stubProviderService.setUpdateProvisionRequestToKeyFunction { r: UpdateProvisionRequest? -> r!!.accountId }
        stubProviderService.addAllToUpdateProvisionResponsesMap(
            mapOf(
                firstAccountFF.outerAccountIdInProvider to
                    GrpcResponse.success(
                        UpdateProvisionResponse.newBuilder()
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("storage")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("SAS")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(300L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("storage")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("VLA")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(600L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("memory")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("SAS")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(100L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .build()
                    ),
                secondAccountFF.outerAccountIdInProvider to
                    GrpcResponse.success(
                        UpdateProvisionResponse.newBuilder()
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("memory")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("SAS")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(300L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .build()
                    ),
                accountSF.outerAccountIdInProvider to
                    GrpcResponse.success(
                        UpdateProvisionResponse.newBuilder()
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("storage")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("SAS")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(400L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("storage")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("VLA")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(500L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("memory")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("SAS")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(1000L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("memory")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("VLA")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(2000L).setUnitKey("bytes").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("bytes").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("cpu")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("SAS")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(2000L).setUnitKey("millicores").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("millicores").build()
                                ).build()
                            )
                            .addProvisions(
                                Provision.newBuilder().setResourceKey(
                                    ResourceKey.newBuilder().setCompoundKey(
                                        CompoundResourceKey.newBuilder()
                                            .setResourceTypeKey("cpu")
                                            .addResourceSegmentKeys(
                                                ResourceSegmentKey.newBuilder()
                                                    .setResourceSegmentationKey("location")
                                                    .setResourceSegmentKey("VLA")
                                                    .build()
                                            ).build()
                                    ).build()
                                ).setProvided(
                                    Amount.newBuilder().setValue(3000L).setUnitKey("millicores").build()
                                ).setAllocated(
                                    Amount.newBuilder().setValue(0L).setUnitKey("millicores").build()
                                ).build()
                            )
                            .build()
                    ),
            )
        )

        val responseBody = webClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .post()
            .uri("/api/v1/delivery/_provide")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.OK)
            .expectBody(DeliveryStatusDto::class.java)
            .returnResult()
            .responseBody

        responseBody ?: failOnNull()
        Assertions.assertEquals(body.deliveryId, responseBody.deliveryId)
        val operations = responseBody.operations
        Assertions.assertEquals(3, operations.size)
        operations.forEach { Assertions.assertEquals(OperationStatusDto.SUCCESS, it.status) }
        Assertions.assertEquals(3, stubProviderService.updateProvisionCallCount)
        val updateProvisionRequests = stubProviderService.updateProvisionRequests
        Assertions.assertEquals(3, updateProvisionRequests.size)
        val updateProvisionByAccountId = updateProvisionRequests.associateBy { it.t1.accountId }
        val firstAccountUpdateRequest = updateProvisionByAccountId[firstAccountFF.outerAccountIdInProvider]!!.t1
        Assertions.assertEquals(firstFolder.id, firstAccountUpdateRequest.folderId)
        Assertions.assertEquals(firstFolder.serviceId, firstAccountUpdateRequest.abcServiceId)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, firstAccountUpdateRequest.author.passportUid.passportUid)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_LOGIN, firstAccountUpdateRequest.author.staffLogin.staffLogin)
        Assertions.assertEquals(mapOf(
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(storageResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(sasSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(300L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(storageResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(vlaSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(600L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(memoryResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(sasSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(100L)
                .setUnitKey(BYTES_KEY)
                .build()
        ), firstAccountUpdateRequest.updatedProvisionsList.associate { it.resourceKey to it.provided })
        Assertions.assertTrue(
            firstAccountUpdateRequest.knownProvisionsList.associate {
                it.accountId to
                    it.knownProvisionsList.associate { provision -> provision.resourceKey to provision.provided }
            }.isEmpty()
        )
        Assertions.assertEquals(firstAccountFF.providerId, firstAccountUpdateRequest.providerId)
        Assertions.assertFalse(firstAccountUpdateRequest.accountsSpaceKey.hasCompoundKey())

        val secondAccountUpdateRequest = updateProvisionByAccountId[secondAccountFF.outerAccountIdInProvider]!!.t1
        Assertions.assertEquals(firstFolder.id, firstAccountUpdateRequest.folderId)
        Assertions.assertEquals(firstFolder.serviceId, secondAccountUpdateRequest.abcServiceId)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, secondAccountUpdateRequest.author.passportUid.passportUid)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_LOGIN, secondAccountUpdateRequest.author.staffLogin.staffLogin)
        Assertions.assertEquals(mapOf(
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(memoryResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(sasSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(300L)
                .setUnitKey(BYTES_KEY)
                .build()
        ), secondAccountUpdateRequest.updatedProvisionsList.associate { it.resourceKey to it.provided })
        Assertions.assertEquals(mapOf(
            firstAccountFF.outerAccountIdInProvider to
                mapOf(
                    ResourceKey.newBuilder()
                        .setCompoundKey(
                            CompoundResourceKey.newBuilder()
                                .setResourceTypeKey(storageResourceType.key)
                                .addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey(locationSegmentation.key)
                                        .setResourceSegmentKey(sasSegment.key)
                                        .build()
                                )
                                .build()
                        ).build() to Amount.newBuilder()
                        .setValue(300L)
                        .setUnitKey(BYTES_KEY)
                        .build(),
                    ResourceKey.newBuilder()
                        .setCompoundKey(
                            CompoundResourceKey.newBuilder()
                                .setResourceTypeKey(storageResourceType.key)
                                .addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey(locationSegmentation.key)
                                        .setResourceSegmentKey(vlaSegment.key)
                                        .build()
                                )
                                .build()
                        ).build() to Amount.newBuilder()
                        .setValue(600L)
                        .setUnitKey(BYTES_KEY)
                        .build(),
                    ResourceKey.newBuilder()
                        .setCompoundKey(
                            CompoundResourceKey.newBuilder()
                                .setResourceTypeKey(memoryResourceType.key)
                                .addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey(locationSegmentation.key)
                                        .setResourceSegmentKey(sasSegment.key)
                                        .build()
                                )
                                .build()
                        ).build() to Amount.newBuilder()
                        .setValue(100L)
                        .setUnitKey(BYTES_KEY)
                        .build()
                )
        ), secondAccountUpdateRequest.knownProvisionsList.associate {
            it.accountId to
                it.knownProvisionsList.associate { provision -> provision.resourceKey to provision.provided }
        })
        Assertions.assertEquals(secondAccountFF.providerId, secondAccountUpdateRequest.providerId)
        Assertions.assertFalse(secondAccountUpdateRequest.accountsSpaceKey.hasCompoundKey())

        val thirdAccountUpdateRequest = updateProvisionByAccountId[accountSF.outerAccountIdInProvider]!!.t1
        Assertions.assertEquals(secondFolder.id, thirdAccountUpdateRequest.folderId)
        Assertions.assertEquals(secondFolder.serviceId, thirdAccountUpdateRequest.abcServiceId)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, thirdAccountUpdateRequest.author.passportUid.passportUid)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_LOGIN, thirdAccountUpdateRequest.author.staffLogin.staffLogin)
        Assertions.assertEquals(mapOf(
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(storageResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(sasSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(400L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(storageResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(vlaSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(500L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(memoryResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(sasSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(1000L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(memoryResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(vlaSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(2000L)
                .setUnitKey(BYTES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(cpuResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(sasSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(2000L)
                .setUnitKey(MILLICORES_KEY)
                .build(),
            ResourceKey.newBuilder()
                .setCompoundKey(
                    CompoundResourceKey.newBuilder()
                        .setResourceTypeKey(cpuResourceType.key)
                        .addResourceSegmentKeys(
                            ResourceSegmentKey.newBuilder()
                                .setResourceSegmentationKey(locationSegmentation.key)
                                .setResourceSegmentKey(vlaSegment.key)
                                .build()
                        )
                        .build()
                ).build() to Amount.newBuilder()
                .setValue(3000L)
                .setUnitKey(MILLICORES_KEY)
                .build()
        ), thirdAccountUpdateRequest.updatedProvisionsList.associate { it.resourceKey to it.provided })
        Assertions.assertTrue(thirdAccountUpdateRequest.knownProvisionsList.associate {
            it.accountId to
                it.knownProvisionsList.associate { provision -> provision.resourceKey to provision.provided }
        }.isEmpty())
        Assertions.assertEquals(accountSF.providerId, thirdAccountUpdateRequest.providerId)
        Assertions.assertFalse(thirdAccountUpdateRequest.accountsSpaceKey.hasCompoundKey())

        val folderOperationDtoMap = operations.associate { it.accountId!! to it.folderOperationLogs }
        Assertions.assertEquals(2, folderOperationDtoMap.values.distinct().size)
        val folderOperationLogModels = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getByIds(txSession,
                    folderOperationDtoMap.values.stream()
                        .flatMap { it!!.stream() }
                        .distinct()
                        .map {
                            WithTenant(
                                Tenants.DEFAULT_TENANT_ID,
                                FolderOperationLogModel.Identity(it!!.folderId, it.timestamp, it.id)
                            )
                        }
                        .collect(Collectors.toList())
                )
            }
        }.block()
        folderOperationLogModels ?: failOnNull()
        Assertions.assertEquals(4, folderOperationLogModels.size)
        val folderOperations = folderOperationLogModels.groupBy { it.folderId }

        val folderIdentitySet = folderOperationDtoMap[firstAccountFF.id]!!.stream().map {
            FolderOperationLogModel.Identity(it.folderId, it.timestamp, it.id) }
            .collect(Collectors.toSet())

        val oldQuotasSet = setOf(
            QuotasByResource(
                mapOf(
                    hddSasResource.id to 0L,
                    hddVlaResource.id to 0L,
                    ramSasResource.id to 0L
                )
            ),
            QuotasByResource(
                mapOf(
                    hddSasResource.id to 100L,
                    hddVlaResource.id to 100L,
                    ramSasResource.id to 200L
                )
            ),
            QuotasByResource(
                mapOf(
                    hddVlaResource.id to 300L,
                    ramSasResource.id to 300L
                )
            )
        ).toHashSet()

        val newQuotasSet = setOf(
            QuotasByResource(
                mapOf(
                    hddSasResource.id to 100L,
                    hddVlaResource.id to 100L,
                    ramSasResource.id to 200L
                )
            ),
            QuotasByResource(
                mapOf(
                    hddSasResource.id to 300L,
                    hddVlaResource.id to 300L,
                    ramSasResource.id to 300L
                )
            ),
            QuotasByResource(
                mapOf(
                    hddVlaResource.id to 600L,
                    ramSasResource.id to 400L
                )
            )
        ).toHashSet()

        val orderSet = setOf(2L,3L,4L).toHashSet()

        val deliveryMetaSet = setOf(
            DeliverableMetaHistoryModel.builder()
                .quotaRequestId(requestId)
                .campaignId(campaignId)
                .bigOrderIds(bigOrderOneId)
                .deliveryId(body.deliveryId)
                .build(),
            DeliverableMetaHistoryModel.builder()
                .quotaRequestId(requestId)
                .campaignId(campaignId)
                .bigOrderIds(bigOrderTwoId)
                .deliveryId(body.deliveryId)
                .build(),
            DeliverableMetaHistoryModel.builder()
                .quotaRequestId(requestId)
                .campaignId(campaignId)
                .bigOrderIds(bigOrderThreeId)
                .deliveryId(body.deliveryId)
                .build()
        ).toHashSet()

        folderOperations[firstFolder.id]!!.forEach { op ->
            Assertions.assertEquals(Tenants.DEFAULT_TENANT_ID, op.tenantId)
            Assertions.assertTrue(folderIdentitySet.remove(op.identity))
            Assertions.assertEquals(FolderOperationType.QUOTA_DELIVERY, op.operationType)
            Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, op.authorUserId.get())
            Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, op.authorUserUid.get())
            Assertions.assertTrue(op.authorProviderId.isEmpty)
            Assertions.assertTrue(op.sourceFolderOperationsLogId.isEmpty)
            Assertions.assertTrue(op.destinationFolderOperationsLogId.isEmpty)
            Assertions.assertTrue(op.oldFolderFields.isEmpty)
            Assertions.assertTrue(oldQuotasSet.remove(op.oldQuotas))
            Assertions.assertEquals(QuotasByResource(mapOf()), op.oldBalance)
            Assertions.assertEquals(QuotasByAccount(mapOf()), op.oldProvisions)
            Assertions.assertTrue(op.oldAccounts.isEmpty)
            Assertions.assertTrue(op.newFolderFields.isEmpty)
            Assertions.assertTrue(newQuotasSet.remove(op.newQuotas))
            Assertions.assertEquals(QuotasByResource(mapOf()), op.newBalance)
            Assertions.assertEquals(QuotasByAccount(mapOf()), op.newProvisions)
            Assertions.assertTrue(op.actuallyAppliedProvisions.isEmpty)
            Assertions.assertTrue(op.newAccounts.isEmpty)
            Assertions.assertTrue(op.accountsQuotasOperationsId.isEmpty)
            Assertions.assertTrue(op.quotasDemandsId.isEmpty)
            Assertions.assertTrue(op.operationPhase.isEmpty)
            Assertions.assertTrue(op.commentId.isEmpty)
            Assertions.assertTrue(orderSet.remove(op.order))
            Assertions.assertTrue(deliveryMetaSet.remove(op.deliveryMeta.get()))
            Assertions.assertTrue(op.transferMeta.isEmpty)

        }

        val logModelSF = folderOperations[secondFolder.id]!![0]
        logModelSF ?: failOnNull()
        Assertions.assertEquals(FolderOperationType.QUOTA_DELIVERY, logModelSF.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, logModelSF.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, logModelSF.authorUserUid.get())
        Assertions.assertTrue(logModelSF.authorProviderId.isEmpty)
        Assertions.assertTrue(logModelSF.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(logModelSF.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(logModelSF.oldFolderFields.isEmpty)
        Assertions.assertEquals(
            QuotasByResource(
                mapOf(
                    hddSasResource.id to 0L,
                    hddVlaResource.id to 0L,
                    ramSasResource.id to 0L,
                    ramVlaResource.id to 0L,
                    cpuSasResource.id to 0L,
                    cpuVlaResource.id to 0L
                )
            ), logModelSF.oldQuotas
        )
        Assertions.assertEquals(QuotasByResource(mapOf()), logModelSF.oldBalance)
        Assertions.assertEquals(QuotasByAccount(mapOf()), logModelSF.oldProvisions)
        Assertions.assertTrue(logModelSF.oldAccounts.isEmpty)
        Assertions.assertTrue(logModelSF.newFolderFields.isEmpty)
        Assertions.assertEquals(
            QuotasByResource(
                mapOf(
                    hddSasResource.id to 400L,
                    hddVlaResource.id to 500L,
                    ramSasResource.id to 1000L,
                    ramVlaResource.id to 2000L,
                    cpuSasResource.id to 2000L,
                    cpuVlaResource.id to 3000L
                )
            ), logModelSF.newQuotas
        )
        Assertions.assertEquals(QuotasByResource(mapOf()), logModelSF.newBalance)
        Assertions.assertEquals(QuotasByAccount(mapOf()), logModelSF.newProvisions)
        Assertions.assertTrue(logModelSF.actuallyAppliedProvisions.isEmpty)
        Assertions.assertTrue(logModelSF.newAccounts.isEmpty)
        Assertions.assertTrue(logModelSF.accountsQuotasOperationsId.isEmpty)
        Assertions.assertTrue(logModelSF.quotasDemandsId.isEmpty)
        Assertions.assertTrue(logModelSF.operationPhase.isEmpty)
        Assertions.assertTrue(logModelSF.commentId.isEmpty)
        Assertions.assertEquals(2, logModelSF.order)
        Assertions.assertEquals(
            DeliverableMetaHistoryModel.builder()
                .quotaRequestId(requestId)
                .campaignId(campaignId)
                .bigOrderIds(bigOrderOneId)
                .deliveryId(body.deliveryId)
                .build(), logModelSF.deliveryMeta.get()
        )
        Assertions.assertTrue(logModelSF.transferMeta.isEmpty)

        val operationsByFolderId = operations.stream()
            .flatMap { it.folderOperationLogs!!.stream() }
            .collect(Collectors.groupingBy {
                it.folderId
            })

        Assertions.assertEquals(6, operationsByFolderId[firstFolder.id]!!.size)
        Assertions.assertEquals(1, operationsByFolderId[secondFolder.id]!!.size)

        val operationDtoByAccount = operations.associateBy { it.accountId }
        val firstAccountFFOpp = operationDtoByAccount[firstAccountFF.id]
        firstAccountFFOpp ?: failOnNull()
        Assertions.assertEquals(firstAccountFF.providerId, firstAccountFFOpp.providerId)
        firstAccountFFOpp.updateDateTime ?: failOnNull()
        Assertions.assertNull(firstAccountFFOpp.errorMessage)
        Assertions.assertNull(firstAccountFFOpp.errorKind)
        assertEquals(
            listOf(
                DeliveryStatusRequestedQuotasDto(
                    hddSasResource.id, AmountDto(
                        "300",
                        "B",
                        "300",
                        "B",
                        "300",
                        BYTES,
                        "300",
                        BYTES
                    )
                ),
                DeliveryStatusRequestedQuotasDto(
                    hddVlaResource.id, AmountDto(
                        "600",
                        "B",
                        "600",
                        "B",
                        "600",
                        BYTES,
                        "600",
                        BYTES
                    )
                ),
                DeliveryStatusRequestedQuotasDto(
                    ramSasResource.id, AmountDto(
                        "100",
                        "B",
                        "100",
                        "B",
                        "100",
                        BYTES,
                        "100",
                        BYTES
                    )
                )
            ), firstAccountFFOpp.requestedQuotas
        )
        Assertions.assertEquals(
            DeliveryAndProvideMetaResponseDto.Builder()
                .quotaRequestId(requestId)
                .campaignId(campaignId)
                .addBigOrderId(bigOrderOneId)
                .addBigOrderId(bigOrderTwoId)
                .addBigOrderId(bigOrderThreeId)
                .build(), firstAccountFFOpp.meta!!
        )
        Assertions.assertEquals(
            folderOperationDtoMap[firstAccountFF.id], firstAccountFFOpp.folderOperationLogs!!
        )

        val secondAccountFFOpp = operationDtoByAccount[secondAccountFF.id]
        secondAccountFFOpp ?: failOnNull()
        Assertions.assertEquals(firstAccountFF.providerId, secondAccountFFOpp.providerId)
        secondAccountFFOpp.updateDateTime ?: failOnNull()
        Assertions.assertNull(secondAccountFFOpp.errorMessage)
        Assertions.assertNull(secondAccountFFOpp.errorKind)
        Assertions.assertEquals(
            listOf(
                DeliveryStatusRequestedQuotasDto(
                    ramSasResource.id, AmountDto(
                        "300",
                        "B",
                        "300",
                        "B",
                        "300",
                        BYTES,
                        "300",
                        BYTES
                    )
                )
            ), secondAccountFFOpp.requestedQuotas
        )
        Assertions.assertEquals(
            DeliveryAndProvideMetaResponseDto.Builder()
                .quotaRequestId(requestId)
                .campaignId(campaignId)
                .addBigOrderId(bigOrderOneId)
                .addBigOrderId(bigOrderTwoId)
                .addBigOrderId(bigOrderThreeId)
                .build(), secondAccountFFOpp.meta!!
        )
        Assertions.assertEquals(
            folderOperationDtoMap[secondAccountFF.id], secondAccountFFOpp.folderOperationLogs!!
        )

        val accountSFOpp = operationDtoByAccount[accountSF.id]
        accountSFOpp ?: failOnNull()
        Assertions.assertEquals(accountSF.providerId, accountSFOpp.providerId)
        accountSFOpp.updateDateTime ?: failOnNull()
        Assertions.assertNull(accountSFOpp.errorMessage)
        Assertions.assertNull(accountSFOpp.errorKind)
        assertEquals(
            listOf(
                DeliveryStatusRequestedQuotasDto(
                    hddSasResource.id, AmountDto(
                        "400",
                        "B",
                        "400",
                        "B",
                        "400",
                        BYTES,
                        "400",
                        BYTES
                    )
                ),
                DeliveryStatusRequestedQuotasDto(
                    hddVlaResource.id, AmountDto(
                        "500",
                        "B",
                        "500",
                        "B",
                        "500",
                        BYTES,
                        "500",
                        BYTES
                    )
                ),
                DeliveryStatusRequestedQuotasDto(
                    ramSasResource.id, AmountDto(
                        "1",
                        "KB",
                        "1000",
                        "B",
                        "1",
                        KILOBYTES,
                        "1000",
                        BYTES
                    )
                ),
                DeliveryStatusRequestedQuotasDto(
                    ramVlaResource.id, AmountDto(
                        "2",
                        "KB",
                        "2000",
                        "B",
                        "2",
                        KILOBYTES,
                        "2000",
                        BYTES
                    )
                ),
                DeliveryStatusRequestedQuotasDto(
                    cpuSasResource.id, AmountDto(
                        "2",
                        "cores",
                        "2000",
                        "mCores",
                        "2",
                        CORES,
                        "2000",
                        MILLICORES
                    )
                ),
                DeliveryStatusRequestedQuotasDto(
                    cpuVlaResource.id, AmountDto(
                        "3",
                        "cores",
                        "3000",
                        "mCores",
                        "3",
                        CORES,
                        "3000",
                        MILLICORES
                    )
                )
            ), accountSFOpp.requestedQuotas
        )
        Assertions.assertEquals(
            DeliveryAndProvideMetaResponseDto.Builder()
                .quotaRequestId(requestId)
                .campaignId(campaignId)
                .addBigOrderId(bigOrderOneId)
                .build(), accountSFOpp.meta!!
        )
        Assertions.assertEquals(
            DeliverableFolderOperationDto.builder()
                .folderId(logModelSF.identity.folderId)
                .timestamp(logModelSF.identity.operationDateTime)
                .id(logModelSF.identity.id)
                .build(), accountSFOpp.folderOperationLogs!!.stream().findFirst().orElseThrow()
        )

        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(
                    txSession, listOf(firstFolder.id, secondFolder.id), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        updatedQuotas ?: failOnNull()
        val quotaByResourceIdByFolder = mutableMapOf<String, MutableMap<String, QuotaModel>>()
        for (quota in updatedQuotas) {
            val orDefault = quotaByResourceIdByFolder.getOrPut(quota.folderId) { mutableMapOf() }
            orDefault[quota.resourceId] = quota
        }

        Assertions.assertEquals(
            300L,
            quotaByResourceIdByFolder[firstFolder.id]!![hddSasResource.id]!!.quota
        )
        Assertions.assertEquals(
            600L,
            quotaByResourceIdByFolder[firstFolder.id]!![hddVlaResource.id]!!.quota
        )
        Assertions.assertEquals(
            400L,
            quotaByResourceIdByFolder[firstFolder.id]!![ramSasResource.id]!!.quota
        )

        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[firstFolder.id]!![hddSasResource.id]!!.balance
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[firstFolder.id]!![hddVlaResource.id]!!.balance
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[firstFolder.id]!![ramSasResource.id]!!.balance
        )

        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[firstFolder.id]!![hddSasResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[firstFolder.id]!![hddVlaResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[firstFolder.id]!![ramSasResource.id]!!.frozenQuota
        )

        Assertions.assertEquals(
            400L,
            quotaByResourceIdByFolder[secondFolder.id]!![hddSasResource.id]!!.quota
        )
        Assertions.assertEquals(
            500L,
            quotaByResourceIdByFolder[secondFolder.id]!![hddVlaResource.id]!!.quota
        )
        Assertions.assertEquals(
            1000L,
            quotaByResourceIdByFolder[secondFolder.id]!![ramSasResource.id]!!.quota
        )
        Assertions.assertEquals(
            2000L,
            quotaByResourceIdByFolder[secondFolder.id]!![ramVlaResource.id]!!.quota
        )
        Assertions.assertEquals(
            2000L,
            quotaByResourceIdByFolder[secondFolder.id]!![cpuSasResource.id]!!.quota
        )
        Assertions.assertEquals(
            3000L,
            quotaByResourceIdByFolder[secondFolder.id]!![cpuVlaResource.id]!!.quota
        )

        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![hddSasResource.id]!!.balance
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![hddVlaResource.id]!!.balance
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![ramSasResource.id]!!.balance
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![ramVlaResource.id]!!.balance
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![cpuSasResource.id]!!.balance
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![cpuVlaResource.id]!!.balance
        )

        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![hddSasResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![hddVlaResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![ramSasResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![ramVlaResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![cpuSasResource.id]!!.frozenQuota
        )
        Assertions.assertEquals(
            0L,
            quotaByResourceIdByFolder[secondFolder.id]!![cpuVlaResource.id]!!.frozenQuota
        )

        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(
                    txSession, Tenants.DEFAULT_TENANT_ID, setOf(firstAccountFF.id, secondAccountFF.id, accountSF.id)
                )
            }
        }.block()
        updatedProvisions ?: failOnNull()
        val provisionByResourceIdByAccount = mutableMapOf<String, MutableMap<String, AccountsQuotasModel>>()
        for (provision in updatedProvisions) {
            val orDefault = provisionByResourceIdByAccount.getOrPut(provision.accountId) { mutableMapOf() }
            orDefault[provision.resourceId] = provision
        }

        var provisionByResourceId = provisionByResourceIdByAccount[firstAccountFF.id]
        provisionByResourceId ?: failOnNull()
        Assertions.assertEquals(
            300L,
            provisionByResourceId[hddSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[hddSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[firstAccountFF.id]!!.operationId,
            provisionByResourceId[hddSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddSasResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            600L,
            provisionByResourceId[hddVlaResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddVlaResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[hddVlaResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[firstAccountFF.id]!!.operationId,
            provisionByResourceId[hddVlaResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddVlaResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            100L,
            provisionByResourceId[ramSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[ramSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[firstAccountFF.id]!!.operationId,
            provisionByResourceId[ramSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramSasResource.id]!!.frozenProvidedQuota
        )

        provisionByResourceId = provisionByResourceIdByAccount[secondAccountFF.id]
        provisionByResourceId ?: failOnNull()
        Assertions.assertEquals(
            300L,
            provisionByResourceId[ramSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[ramSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[secondAccountFF.id]!!.operationId,
            provisionByResourceId[ramSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramSasResource.id]!!.frozenProvidedQuota
        )

        provisionByResourceId = provisionByResourceIdByAccount[accountSF.id]
        provisionByResourceId ?: failOnNull()
        Assertions.assertEquals(
            400L,
            provisionByResourceId[hddSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[hddSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[accountSF.id]!!.operationId,
            provisionByResourceId[hddSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddSasResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            500L,
            provisionByResourceId[hddVlaResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddVlaResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[hddVlaResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[accountSF.id]!!.operationId,
            provisionByResourceId[hddVlaResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[hddVlaResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            1000L,
            provisionByResourceId[ramSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[ramSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[accountSF.id]!!.operationId,
            provisionByResourceId[ramSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramSasResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            2000,
            provisionByResourceId[ramVlaResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramVlaResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[ramVlaResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[accountSF.id]!!.operationId,
            provisionByResourceId[ramVlaResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[ramVlaResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            2000,
            provisionByResourceId[cpuSasResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[cpuSasResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[cpuSasResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[accountSF.id]!!.operationId,
            provisionByResourceId[cpuSasResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[cpuSasResource.id]!!.frozenProvidedQuota
        )

        Assertions.assertEquals(
            3000,
            provisionByResourceId[cpuVlaResource.id]!!.providedQuota
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[cpuVlaResource.id]!!.allocatedQuota
        )
        Assertions.assertTrue(
            provisionByResourceId[cpuVlaResource.id]!!.lastReceivedProvisionVersion.isEmpty
        )
        Assertions.assertEquals(
            operationDtoByAccount[accountSF.id]!!.operationId,
            provisionByResourceId[cpuVlaResource.id]!!.latestSuccessfulProvisionOperationId.get()
        )
        Assertions.assertEquals(
            0L,
            provisionByResourceId[cpuVlaResource.id]!!.frozenProvidedQuota
        )

        val updatedFolders = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.getByIds(
                    txSession, listOf(firstFolder.id, secondFolder.id), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()

        updatedFolders ?: failOnNull()
        val updatedFoldersById = updatedFolders.associateBy { it.id }
        Assertions.assertEquals(9, updatedFoldersById[firstFolder.id]!!.nextOpLogOrder)
        Assertions.assertEquals(5, updatedFoldersById[secondFolder.id]!!.nextOpLogOrder)

        val operationInProgress = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenantFolders(
                    txSession, Tenants.DEFAULT_TENANT_ID, setOf(firstFolder.id, secondFolder.id)
                )
            }
        }.block()
        operationInProgress ?: failOnNull()
        Assertions.assertTrue(operationInProgress.isEmpty())

        val accountQuotaOperations = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getByIds(
                    txSession, operations.map { it.operationId }.toList(), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        accountQuotaOperations ?: failOnNull()
        Assertions.assertEquals(3, accountQuotaOperations.size)
        val accountsOperationsByAccountId = accountQuotaOperations.associateBy { it.requestedChanges.accountId.get() }
        var accountOperation = accountsOperationsByAccountId[firstAccountFF.id]
        accountOperation ?: failOnNull()
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.OperationType.DELIVER_AND_UPDATE_PROVISION,
            accountOperation.operationType
        )
        Assertions.assertEquals(OperationSource.USER, accountOperation.operationSource)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, accountOperation.authorUserId)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, accountOperation.authorUserUid.get())
        Assertions.assertEquals(firstAccountFF.providerId, accountOperation.providerId)
        Assertions.assertTrue(accountOperation.accountsSpaceId.isEmpty)
        Assertions.assertTrue(accountOperation.updateDateTime.isPresent)
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.OK, accountOperation.requestStatus.get())
        Assertions.assertTrue(accountOperation.errorMessage.isEmpty)
        Assertions.assertTrue(accountOperation.fullErrorMessage.isEmpty)
        Assertions.assertTrue(accountOperation.errorKind.isEmpty)
        Assertions.assertEquals(firstAccountFF.id, accountOperation.requestedChanges.accountId.get())
        Assertions.assertEquals(body.deliveryId, accountOperation.requestedChanges.deliveryId.get())
        assertEquals(                    listOf(
            OperationChangesModel.Provision(hddSasResource.id, 300L),
            OperationChangesModel.Provision(hddVlaResource.id, 600L),
            OperationChangesModel.Provision(ramSasResource.id, 100L)
        ), accountOperation.requestedChanges.updatedProvisions.get())
        assertEquals(                    listOf(
            OperationChangesModel.Provision(hddSasResource.id, 300L),
            OperationChangesModel.Provision(hddVlaResource.id, 600L),
            OperationChangesModel.Provision(ramSasResource.id, 100L)
        ), accountOperation.requestedChanges.frozenProvisions.get())
        val submitOrder = accountOperation.orders.submitOrder
        val closeOrder = accountOperation.orders.closeOrder.get()
        Assertions.assertTrue(submitOrder == 5L || submitOrder == 6L)
        Assertions.assertTrue(closeOrder == 7L || closeOrder == 8L)

        accountOperation = accountsOperationsByAccountId[secondAccountFF.id]
        accountOperation ?: failOnNull()
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.OperationType.DELIVER_AND_UPDATE_PROVISION,
            accountOperation.operationType
        )
        Assertions.assertEquals(OperationSource.USER, accountOperation.operationSource)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, accountOperation.authorUserId)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, accountOperation.authorUserUid.get())
        Assertions.assertEquals(secondAccountFF.providerId, accountOperation.providerId)
        Assertions.assertTrue(accountOperation.accountsSpaceId.isEmpty)
        Assertions.assertTrue(accountOperation.updateDateTime.isPresent)
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.OK, accountOperation.requestStatus.get())
        Assertions.assertTrue(accountOperation.errorMessage.isEmpty)
        Assertions.assertTrue(accountOperation.fullErrorMessage.isEmpty)
        Assertions.assertTrue(accountOperation.errorKind.isEmpty)
        Assertions.assertEquals(secondAccountFF.id, accountOperation.requestedChanges.accountId.get())
        Assertions.assertEquals(body.deliveryId, accountOperation.requestedChanges.deliveryId.get())
        assertEquals(listOf(
            OperationChangesModel.Provision(ramSasResource.id, 300L)
        ), accountOperation.requestedChanges.updatedProvisions.get())
        assertEquals(listOf(
            OperationChangesModel.Provision(ramSasResource.id, 300L)
        ), accountOperation.requestedChanges.frozenProvisions.get())
        Assertions.assertTrue(
            (accountOperation.orders.submitOrder == 5L
                && accountOperation.orders.submitOrder != submitOrder)
                || (accountOperation.orders.submitOrder == 6L
                && accountOperation.orders.submitOrder != submitOrder)
        )
        Assertions.assertTrue(
            (accountOperation.orders.closeOrder.get() == 7L
                && accountOperation.orders.closeOrder.get() != closeOrder)
                || (accountOperation.orders.closeOrder.get() == 8L
                && accountOperation.orders.closeOrder.get() != closeOrder)
        )

        accountOperation = accountsOperationsByAccountId[accountSF.id]
        accountOperation ?: failOnNull()
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.OperationType.DELIVER_AND_UPDATE_PROVISION,
            accountOperation.operationType
        )
        Assertions.assertEquals(OperationSource.USER, accountOperation.operationSource)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, accountOperation.authorUserId)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, accountOperation.authorUserUid.get())
        Assertions.assertEquals(accountSF.providerId, accountOperation.providerId)
        Assertions.assertTrue(accountOperation.accountsSpaceId.isEmpty)
        Assertions.assertTrue(accountOperation.updateDateTime.isPresent)
        Assertions.assertEquals(AccountsQuotasOperationsModel.RequestStatus.OK, accountOperation.requestStatus.get())
        Assertions.assertTrue(accountOperation.errorMessage.isEmpty)
        Assertions.assertTrue(accountOperation.fullErrorMessage.isEmpty)
        Assertions.assertTrue(accountOperation.errorKind.isEmpty)

        Assertions.assertEquals(accountSF.id, accountOperation.requestedChanges.accountId.get())
        Assertions.assertEquals(body.deliveryId, accountOperation.requestedChanges.deliveryId.get())
        assertEquals(listOf(
            OperationChangesModel.Provision(hddSasResource.id, 400L),
            OperationChangesModel.Provision(hddVlaResource.id, 500L),
            OperationChangesModel.Provision(ramSasResource.id, 1000L),
            OperationChangesModel.Provision(ramVlaResource.id, 2000L),
            OperationChangesModel.Provision(cpuSasResource.id, 2000L),
            OperationChangesModel.Provision(cpuVlaResource.id, 3000L)
        ), accountOperation.requestedChanges.updatedProvisions.get())
        assertEquals(listOf(
            OperationChangesModel.Provision(hddSasResource.id, 400L),
            OperationChangesModel.Provision(hddVlaResource.id, 500L),
            OperationChangesModel.Provision(ramSasResource.id, 1000L),
            OperationChangesModel.Provision(ramVlaResource.id, 2000L),
            OperationChangesModel.Provision(cpuSasResource.id, 2000L),
            OperationChangesModel.Provision(cpuVlaResource.id, 3000L)
        ), accountOperation.requestedChanges.frozenProvisions.get())
        Assertions.assertEquals(
            OperationOrdersModel.builder()
                .submitOrder(3L)
                .closeOrder(4L)
                .build(), accountOperation.orders
        )

        val firstFolderOppLogs = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, firstFolder.id, SortOrderDto.ASC, 10
                )
            }
        }.block()
        firstFolderOppLogs ?: failOnNull()
        Assertions.assertEquals(7, firstFolderOppLogs.size)
        val firstFolderLogsByAccountByPhase =
            mutableMapOf<String, MutableMap<OperationPhase, FolderOperationLogModel>>()
        for (log in firstFolderOppLogs) {
            if (log.operationType == FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT) {
                val orPut =
                    firstFolderLogsByAccountByPhase.getOrPut(log.accountsQuotasOperationsId.get()) { mutableMapOf() }
                orPut[log.operationPhase.get()] = log
            }
        }
        val firstAccountFFOperation = accountsOperationsByAccountId[firstAccountFF.id]
        firstAccountFFOperation ?: failOnNull()
        var folderLog = firstFolderLogsByAccountByPhase[firstAccountFFOperation.operationId]!![OperationPhase.SUBMIT]
        folderLog ?: failOnNull()
        Assertions.assertEquals(firstAccountFFOperation.lastRequestId.get(), folderLog.providerRequestId.get())
        Assertions.assertEquals(FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, folderLog.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, folderLog.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, folderLog.authorUserUid.get())
        Assertions.assertTrue(folderLog.authorProviderId.isEmpty)
        Assertions.assertTrue(folderLog.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.oldFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    firstAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(0L, null),
                            hddVlaResource.id to ProvisionHistoryModel(0L, null),
                            ramSasResource.id to ProvisionHistoryModel(0L, null)
                        )
                    )
                )
            ), folderLog.oldProvisions
        )
        Assertions.assertTrue(folderLog.oldAccounts.isEmpty)
        Assertions.assertTrue(folderLog.newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    firstAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(300L, 0L),
                            hddVlaResource.id to ProvisionHistoryModel(600L, 0L),
                            ramSasResource.id to ProvisionHistoryModel(100L, 0L)
                        )
                    )
                )
            ), folderLog.newProvisions
        )
        Assertions.assertTrue(folderLog.actuallyAppliedProvisions.isEmpty)
        Assertions.assertTrue(folderLog.newAccounts.isEmpty)
        Assertions.assertTrue(folderLog.quotasDemandsId.isEmpty)
        Assertions.assertTrue(folderLog.commentId.isEmpty)
        Assertions.assertEquals(DeliverableMetaHistoryModel.builder()
            .deliveryId(body.deliveryId)
            .quotaRequestId(requestId)
            .campaignId(campaignId)
            .bigOrderIds(bigOrderOneId)
            .bigOrderIds(bigOrderTwoId)
            .bigOrderIds(bigOrderThreeId)
            .build(), folderLog.deliveryMeta.get())
        Assertions.assertTrue(folderLog.transferMeta.isEmpty)
        Assertions.assertEquals(firstAccountFFOperation.orders.submitOrder, folderLog.order)

        folderLog = firstFolderLogsByAccountByPhase[firstAccountFFOperation.operationId]!![OperationPhase.CLOSE]
        folderLog ?: failOnNull()
        Assertions.assertTrue(folderLog.providerRequestId.isEmpty)
        Assertions.assertEquals(FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, folderLog.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, folderLog.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, folderLog.authorUserUid.get())
        Assertions.assertTrue(folderLog.authorProviderId.isEmpty)
        Assertions.assertTrue(folderLog.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.oldFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    firstAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(0L, null),
                            hddVlaResource.id to ProvisionHistoryModel(0L, null),
                            ramSasResource.id to ProvisionHistoryModel(0L, null)
                        )
                    )
                )
            ), folderLog.oldProvisions
        )
        Assertions.assertTrue(folderLog.oldAccounts.isEmpty)
        Assertions.assertTrue(folderLog.newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    firstAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(300L, null),
                            hddVlaResource.id to ProvisionHistoryModel(600L, null),
                            ramSasResource.id to ProvisionHistoryModel(100L, null)
                        )
                    )
                )
            ), folderLog.newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    firstAccountFF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(300L, null),
                            hddVlaResource.id to ProvisionHistoryModel(600L, null),
                            ramSasResource.id to ProvisionHistoryModel(100L, null)
                        )
                    )
                )
            ), folderLog.actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(folderLog.newAccounts.isEmpty)
        Assertions.assertTrue(folderLog.quotasDemandsId.isEmpty)
        Assertions.assertTrue(folderLog.commentId.isEmpty)
        Assertions.assertTrue(folderLog.deliveryMeta.isEmpty)
        Assertions.assertTrue(folderLog.transferMeta.isEmpty)
        Assertions.assertEquals(firstAccountFFOperation.orders.closeOrder.get(), folderLog.order)

        val secondAccountFFOperation = accountsOperationsByAccountId[secondAccountFF.id]
        secondAccountFFOperation ?: failOnNull()
        folderLog = firstFolderLogsByAccountByPhase[secondAccountFFOperation.operationId]!![OperationPhase.SUBMIT]
        folderLog ?: failOnNull()
        Assertions.assertEquals(secondAccountFFOperation.lastRequestId.get(), folderLog.providerRequestId.get())
        Assertions.assertEquals(FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, folderLog.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, folderLog.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, folderLog.authorUserUid.get())
        Assertions.assertTrue(folderLog.authorProviderId.isEmpty)
        Assertions.assertTrue(folderLog.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.oldFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    secondAccountFF.id to ProvisionsByResource(
                        mapOf(
                            ramSasResource.id to ProvisionHistoryModel(0L, null)
                        )
                    )
                )
            ), folderLog.oldProvisions
        )
        Assertions.assertTrue(folderLog.oldAccounts.isEmpty)
        Assertions.assertTrue(folderLog.newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    secondAccountFF.id to ProvisionsByResource(
                        mapOf(
                            ramSasResource.id to ProvisionHistoryModel(300L, 0L)
                        )
                    )
                )
            ), folderLog.newProvisions
        )
        Assertions.assertTrue(folderLog.actuallyAppliedProvisions.isEmpty)
        Assertions.assertTrue(folderLog.newAccounts.isEmpty)
        Assertions.assertTrue(folderLog.quotasDemandsId.isEmpty)
        Assertions.assertTrue(folderLog.commentId.isEmpty)
        Assertions.assertEquals(DeliverableMetaHistoryModel.builder()
            .deliveryId(body.deliveryId)
            .quotaRequestId(requestId)
            .campaignId(campaignId)
            .bigOrderIds(bigOrderOneId)
            .bigOrderIds(bigOrderTwoId)
            .bigOrderIds(bigOrderThreeId)
            .build(), folderLog.deliveryMeta.get())
        Assertions.assertTrue(folderLog.transferMeta.isEmpty)
        Assertions.assertEquals(secondAccountFFOperation.orders.submitOrder, folderLog.order)

        folderLog = firstFolderLogsByAccountByPhase[secondAccountFFOperation.operationId]!![OperationPhase.CLOSE]
        folderLog ?: failOnNull()
        Assertions.assertTrue(folderLog.providerRequestId.isEmpty)
        Assertions.assertEquals(FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, folderLog.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, folderLog.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, folderLog.authorUserUid.get())
        Assertions.assertTrue(folderLog.authorProviderId.isEmpty)
        Assertions.assertTrue(folderLog.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.oldFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    secondAccountFF.id to ProvisionsByResource(
                        mapOf(
                            ramSasResource.id to ProvisionHistoryModel(0L, null)
                        )
                    )
                )
            ), folderLog.oldProvisions
        )
        Assertions.assertTrue(folderLog.oldAccounts.isEmpty)
        Assertions.assertTrue(folderLog.newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    secondAccountFF.id to ProvisionsByResource(
                        mapOf(
                            ramSasResource.id to ProvisionHistoryModel(300L, null)
                        )
                    )
                )
            ), folderLog.newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    secondAccountFF.id to ProvisionsByResource(
                        mapOf(
                            ramSasResource.id to ProvisionHistoryModel(300L, null)
                        )
                    )
                )
            ), folderLog.actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(folderLog.newAccounts.isEmpty)
        Assertions.assertTrue(folderLog.quotasDemandsId.isEmpty)
        Assertions.assertTrue(folderLog.commentId.isEmpty)
        Assertions.assertTrue(folderLog.deliveryMeta.isEmpty)
        Assertions.assertTrue(folderLog.transferMeta.isEmpty)
        Assertions.assertEquals(secondAccountFFOperation.orders.closeOrder.get(), folderLog.order)

        val secondFolderOppLogs = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, secondFolder.id, SortOrderDto.ASC, 10
                )
            }
        }.block()
        secondFolderOppLogs ?: failOnNull()
        Assertions.assertEquals(3, secondFolderOppLogs.size)
        val secondFolderLogsByAccountByPhase =
            mutableMapOf<String, MutableMap<OperationPhase, FolderOperationLogModel>>()
        for (log in secondFolderOppLogs) {
            if (log.operationType == FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT) {
                val orPut =
                    secondFolderLogsByAccountByPhase.getOrPut(log.accountsQuotasOperationsId.get()) { mutableMapOf() }
                orPut[log.operationPhase.get()] = log
            }
        }

        val accountSFOperation = accountsOperationsByAccountId[accountSF.id]
        accountSFOperation ?: failOnNull()
        folderLog = secondFolderLogsByAccountByPhase[accountSFOperation.operationId]!![OperationPhase.SUBMIT]
        folderLog ?: failOnNull()
        Assertions.assertEquals(accountSFOperation.lastRequestId.get(), folderLog.providerRequestId.get())
        Assertions.assertEquals(FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, folderLog.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, folderLog.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, folderLog.authorUserUid.get())
        Assertions.assertTrue(folderLog.authorProviderId.isEmpty)
        Assertions.assertTrue(folderLog.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.oldFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    accountSF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(0L, null),
                            hddVlaResource.id to ProvisionHistoryModel(0L, null),
                            ramSasResource.id to ProvisionHistoryModel(0L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L, null),
                            cpuVlaResource.id to ProvisionHistoryModel(0L, null)
                        )
                    )
                )
            ), folderLog.oldProvisions
        )
        Assertions.assertTrue(folderLog.oldAccounts.isEmpty)
        Assertions.assertTrue(folderLog.newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    accountSF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(0L + 400L, 0L),
                            hddVlaResource.id to ProvisionHistoryModel(0L + 500L, 0L),
                            ramSasResource.id to ProvisionHistoryModel(0L + 1000L, 0L),
                            ramVlaResource.id to ProvisionHistoryModel(0L + 2000L, 0L),
                            cpuSasResource.id to ProvisionHistoryModel(0L + 2000L, 0L),
                            cpuVlaResource.id to ProvisionHistoryModel(0L + 3000L, 0L)
                        )
                    )
                )
            ), folderLog.newProvisions
        )
        Assertions.assertTrue(folderLog.actuallyAppliedProvisions.isEmpty)
        Assertions.assertTrue(folderLog.newAccounts.isEmpty)
        Assertions.assertTrue(folderLog.quotasDemandsId.isEmpty)
        Assertions.assertTrue(folderLog.commentId.isEmpty)
        Assertions.assertEquals(DeliverableMetaHistoryModel.builder()
            .deliveryId(body.deliveryId)
            .quotaRequestId(requestId)
            .campaignId(campaignId)
            .bigOrderIds(bigOrderOneId)
            .build(), folderLog.deliveryMeta.get())
        Assertions.assertTrue(folderLog.transferMeta.isEmpty)
        Assertions.assertEquals(accountSFOperation.orders.submitOrder, folderLog.order)

        folderLog = secondFolderLogsByAccountByPhase[accountSFOperation.operationId]!![OperationPhase.CLOSE]
        folderLog ?: failOnNull()
        Assertions.assertTrue(folderLog.providerRequestId.isEmpty)
        Assertions.assertEquals(FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, folderLog.operationType)
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_ID, folderLog.authorUserId.get())
        Assertions.assertEquals(DISPENSER_QUOTA_MANAGER_UID, folderLog.authorUserUid.get())
        Assertions.assertTrue(folderLog.authorProviderId.isEmpty)
        Assertions.assertTrue(folderLog.sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.destinationFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(folderLog.oldFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.oldBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    accountSF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(0L, null),
                            hddVlaResource.id to ProvisionHistoryModel(0L, null),
                            ramSasResource.id to ProvisionHistoryModel(0L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L, null),
                            cpuVlaResource.id to ProvisionHistoryModel(0L, null)
                        )
                    )
                )
            ), folderLog.oldProvisions
        )
        Assertions.assertTrue(folderLog.oldAccounts.isEmpty)
        Assertions.assertTrue(folderLog.newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), folderLog.newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    accountSF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(0L + 400L, null),
                            hddVlaResource.id to ProvisionHistoryModel(0L + 500L, null),
                            ramSasResource.id to ProvisionHistoryModel(0L + 1000L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L + 2000L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L + 2000L, null),
                            cpuVlaResource.id to ProvisionHistoryModel(0L + 3000L, null)
                        )
                    )
                )
            ), folderLog.newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    accountSF.id to ProvisionsByResource(
                        mapOf(
                            hddSasResource.id to ProvisionHistoryModel(0L + 400L, null),
                            hddVlaResource.id to ProvisionHistoryModel(0L + 500L, null),
                            ramSasResource.id to ProvisionHistoryModel(0L + 1000L, null),
                            ramVlaResource.id to ProvisionHistoryModel(0L + 2000L, null),
                            cpuSasResource.id to ProvisionHistoryModel(0L + 2000L, null),
                            cpuVlaResource.id to ProvisionHistoryModel(0L + 3000L, null)
                        )
                    )
                )
            ), folderLog.actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(folderLog.newAccounts.isEmpty)
        Assertions.assertTrue(folderLog.quotasDemandsId.isEmpty)
        Assertions.assertTrue(folderLog.commentId.isEmpty)
        Assertions.assertTrue(folderLog.deliveryMeta.isEmpty)
        Assertions.assertTrue(folderLog.transferMeta.isEmpty)
        Assertions.assertEquals(accountSFOperation.orders.closeOrder.get(), folderLog.order)
    }

    fun providerModel(
        grpcUri: String? = null,
        restUri: String? = null,
        accountsSpacesSupported: Boolean = false,
        softDeleteSupported: Boolean = false,
        accountKeySupported: Boolean = false,
        accountDisplayNameSupported: Boolean = false,
        perAccountVersionSupported: Boolean = false,
        perProvisionVersionSupported: Boolean = false,
        perAccountLastUpdateSupported: Boolean = false,
        operationIdDeduplicationSupported: Boolean = false,
        perProvisionLastUpdateSupported: Boolean = true
    ): ProviderModel {
        return ProviderModel.builder().id(UUID.randomUUID().toString()).grpcApiUri(grpcUri).restApiUri(restUri)
            .destinationTvmId(42L).tenantId(Tenants.DEFAULT_TENANT_ID).version(0L).nameEn("Test").nameRu("Test")
            .descriptionEn("Test").descriptionRu("Test").sourceTvmId(42L).serviceId(69L).deleted(false).readOnly(false)
            .multipleAccountsPerFolder(true).accountTransferWithQuota(true).managed(true).key("test")
            .trackerComponentId(1L).accountsSettings(
                AccountsSettingsModel.builder().displayNameSupported(accountDisplayNameSupported)
                    .keySupported(accountKeySupported).deleteSupported(true).softDeleteSupported(softDeleteSupported)
                    .moveSupported(true).renameSupported(true).perAccountVersionSupported(perAccountVersionSupported)
                    .perProvisionVersionSupported(perProvisionVersionSupported)
                    .perAccountLastUpdateSupported(perAccountLastUpdateSupported)
                    .perProvisionLastUpdateSupported(perProvisionLastUpdateSupported)
                    .operationIdDeduplicationSupported(operationIdDeduplicationSupported).syncCoolDownDisabled(false)
                    .retryCoolDownDisabled(false).accountsSyncPageSize(1000L).build()
            ).importAllowed(true).accountsSpacesSupported(accountsSpacesSupported).syncEnabled(true).grpcTlsOn(true)
            .build()
    }

    fun folderModel(serviceId: Long, displayName: String = "Test", description: String = "Test"): FolderModel {
        return FolderModel.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setServiceId(serviceId)
            .setVersion(0L)
            .setDisplayName(displayName)
            .setDescription(description)
            .setDeleted(false)
            .setFolderType(FolderType.COMMON)
            .setTags(emptySet())
            .setNextOpLogOrder(2L)
            .build()
    }

    fun resourceSegmentationModel(providerId: String, key: String): ResourceSegmentationModel {
        return ResourceSegmentationModel.builder().id(UUID.randomUUID().toString()).tenantId(Tenants.DEFAULT_TENANT_ID)
            .providerId(providerId).version(0L).key(key).nameEn("Test").nameRu("Test").descriptionEn("Test")
            .descriptionRu("Test").deleted(false).build()
    }

    fun resourceSegmentModel(segmentationId: String, key: String): ResourceSegmentModel {
        return ResourceSegmentModel.builder().id(UUID.randomUUID().toString()).tenantId(Tenants.DEFAULT_TENANT_ID)
            .segmentationId(segmentationId).version(0L).key(key).nameEn("Test").nameRu("Test").descriptionEn("Test")
            .descriptionRu("Test").deleted(false).build()
    }

    fun resourceTypeModel(providerId: String, key: String, unitsEnsembleId: String): ResourceTypeModel {
        return ResourceTypeModel.builder().id(UUID.randomUUID().toString()).tenantId(Tenants.DEFAULT_TENANT_ID)
            .providerId(providerId).version(0L).key(key).nameEn("Test").nameRu("Test").descriptionEn("Test")
            .descriptionRu("Test").deleted(false).unitsEnsembleId(unitsEnsembleId).build()
    }

    fun resourceModel(
        providerId: String,
        key: String,
        resourceTypeId: String,
        segments: Set<Tuple2<String, String>>,
        unitsEnsembleId: String,
        allowedUnitIds: Set<String>,
        defaultUnitId: String,
        baseUnitId: String,
        accountsSpaceId: String?,
        providerApiUnitId: String? = null
    ): ResourceModel {
        return ResourceModel.builder().id(UUID.randomUUID().toString()).tenantId(Tenants.DEFAULT_TENANT_ID).version(0L)
            .key(key).nameEn("Test").nameRu("Test").descriptionEn("Test").descriptionRu("Test").deleted(false)
            .unitsEnsembleId(unitsEnsembleId).providerId(providerId).resourceTypeId(resourceTypeId)
            .segments(segments.mapTo(HashSet()) { t: Tuple2<String, String> ->
                ResourceSegmentSettingsModel(t.t1, t.t2)
            }).resourceUnits(ResourceUnitsModel(allowedUnitIds, defaultUnitId, providerApiUnitId)).managed(true)
            .orderable(true).readOnly(false).baseUnitId(baseUnitId).accountsSpacesId(accountsSpaceId).build()
    }

    fun quotaModel(
        providerId: String, resourceId: String, folderId: String, quota: Long, balance: Long, frozen: Long
    ): QuotaModel {
        return QuotaModel.builder().tenantId(Tenants.DEFAULT_TENANT_ID).providerId(providerId).resourceId(resourceId)
            .folderId(folderId).quota(quota).balance(balance).frozenQuota(frozen).build()
    }

    fun accountModel(
        providerId: String,
        accountsSpaceId: String?,
        externalId: String,
        externalKey: String,
        folderId: String,
        displayName: String,
        lastReceivedVersion: Long?,
        lastOpId: String?,
        deleted: Boolean = false
    ): AccountModel {
        return AccountModel.Builder().setTenantId(Tenants.DEFAULT_TENANT_ID).setId(UUID.randomUUID().toString())
            .setVersion(0L).setProviderId(providerId).setAccountsSpacesId(accountsSpaceId)
            .setOuterAccountIdInProvider(externalId).setOuterAccountKeyInProvider(externalKey).setFolderId(folderId)
            .setDisplayName(displayName).setDeleted(deleted).setLastAccountUpdate(Instant.now())
            .setLastReceivedVersion(lastReceivedVersion).setLatestSuccessfulAccountOperationId(lastOpId).build()
    }

    private fun accountQuotaModel(
        providerId: String,
        resourceId: String,
        folderId: String,
        accountId: String,
        provided: Long,
        allocated: Long,
        lastReceivedVersion: Long? = null,
        lastOpId: String? = null
    ): AccountsQuotasModel {
        return AccountsQuotasModel.Builder().setTenantId(Tenants.DEFAULT_TENANT_ID).setProviderId(providerId)
            .setResourceId(resourceId).setFolderId(folderId).setAccountId(accountId).setProvidedQuota(provided)
            .setAllocatedQuota(allocated).setLastProvisionUpdate(Instant.now())
            .setLastReceivedProvisionVersion(lastReceivedVersion).setLatestSuccessfulProvisionOperationId(lastOpId)
            .build()
    }

    private fun assertEquals(firstOp: OperationChangesModel, secondOp: OperationChangesModel) {
        Assertions.assertEquals(firstOp.accountId, secondOp.accountId)
        assertEquals(firstOp.updatedProvisions, secondOp.updatedProvisions)
        assertEquals(firstOp.frozenProvisions, secondOp.frozenProvisions)
        Assertions.assertEquals(firstOp.accountCreateParams, secondOp.accountCreateParams)
        Assertions.assertEquals(firstOp.accountRenameParams, secondOp.accountRenameParams)
        Assertions.assertEquals(firstOp.destinationFolderId, secondOp.destinationFolderId)
        Assertions.assertEquals(firstOp.destinationAccountId, secondOp.destinationAccountId)
        assertEquals(firstOp.updatedDestinationProvisions, secondOp.updatedDestinationProvisions)
        assertEquals(firstOp.frozenDestinationProvisions, secondOp.frozenDestinationProvisions)
        Assertions.assertEquals(firstOp.transferRequestId, secondOp.transferRequestId)
        Assertions.assertEquals(firstOp.deliveryId, secondOp.deliveryId)
    }

    private fun <T> assertEquals(o1: Optional<List<T>>, o2: Optional<List<T>>) {
        Assertions.assertTrue(o1.isEmpty == o2.isEmpty)
        if (o1.isPresent) {
            assertEquals(o1.get(), o2.get())
        }
    }

    private fun <T> assertEquals(l1: List<T>, l2: List<T>) {
        Assertions.assertTrue(l1.size == l2.size
            && l1.containsAll(l2)
            && l2.containsAll(l1))
    }
}
