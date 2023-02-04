package ru.yandex.intranet.d.web.api.delivery

import com.yandex.ydb.table.transaction.TransactionMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_1
import ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_1_ID
import ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_ID
import ru.yandex.intranet.d.TestProviders.YP_ID
import ru.yandex.intranet.d.TestResources.YP_HDD_IVA
import ru.yandex.intranet.d.TestUsers.USER_1_ID
import ru.yandex.intranet.d.TestUsers.USER_1_UID
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasOperationsDao
import ru.yandex.intranet.d.dao.delivery.DeliveriesAndProvidedRequestsDao
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao
import ru.yandex.intranet.d.datasource.model.YdbSession
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel.RequestStatus
import ru.yandex.intranet.d.model.accounts.OperationChangesModel
import ru.yandex.intranet.d.model.accounts.OperationOrdersModel
import ru.yandex.intranet.d.model.accounts.OperationSource
import ru.yandex.intranet.d.model.delivery.DeliverableDeltaModel
import ru.yandex.intranet.d.model.delivery.DeliverableFolderOperationModel
import ru.yandex.intranet.d.model.delivery.DeliverableMetaHistoryModel
import ru.yandex.intranet.d.model.delivery.DeliverableMetaRequestModel
import ru.yandex.intranet.d.model.delivery.provide.DeliveryAndProvideDestinationModel
import ru.yandex.intranet.d.model.delivery.provide.DeliveryAndProvideModel
import ru.yandex.intranet.d.model.delivery.provide.DeliveryAndProvideOperationListModel
import ru.yandex.intranet.d.model.delivery.provide.DeliveryAndProvideOperationModel
import ru.yandex.intranet.d.model.delivery.provide.DeliveryAndProvideRequestModel
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel
import ru.yandex.intranet.d.model.folders.FolderOperationType
import ru.yandex.intranet.d.model.folders.QuotasByAccount
import ru.yandex.intranet.d.model.folders.QuotasByResource
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.ErrorCollectionDto
import ru.yandex.intranet.d.web.model.delivery.status.DeliveryStatusResponseDto
import ru.yandex.intranet.d.web.model.operations.OperationStatusDto
import ru.yandex.intranet.d.web.model.resources.ResourceSegmentationSegmentDto
import java.time.Instant
import java.util.*

@IntegrationTest
class DeliveryStatusesTests(
    @Autowired private var webClient: WebTestClient,
    @Autowired private var deliveriesDao: DeliveriesAndProvidedRequestsDao,
    @Autowired private var accountsQuotasOperationsDao: AccountsQuotasOperationsDao,
    @Autowired private var folderOperationLogDao: FolderOperationLogDao,
    @Autowired private var ydbTableClient: YdbTableClient,
    @Autowired @Value("\${hardwareOrderService.tvmSourceId}")
    private val dispenserTvmSourceId: Long
) {
    @Test
    fun deliveryToAccountStatusTest() {
        val firstDelivery = createDelivery()
        val secondDelivery = createDelivery()
        val firstDeliveryOperations = createAccountOperations(toOperationIds(firstDelivery))
        val secondDeliveryOperations = createAccountOperations(toOperationIds(secondDelivery))
        val firstFolderOperations = createFolderOperations(firstDelivery)
        val secondFolderOperations = createFolderOperations(secondDelivery)
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            folderOperationLogDao.upsertAllRetryable(
                session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                firstFolderOperations.plus(secondFolderOperations))
        }.block()
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            accountsQuotasOperationsDao.upsertAllRetryable(
                session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                firstDeliveryOperations.plus(secondDeliveryOperations))
        }.block()
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            deliveriesDao.upsertAllRetryable(
                session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                listOf(firstDelivery, secondDelivery))
        }.block()

        val result = webClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .post()
            .uri("/api/v1/delivery/_status")
            .bodyValue(listOf(firstDelivery.deliveryId, secondDelivery.deliveryId))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(DeliveryStatusResponseDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(2, result.deliveries.size)
        assertEquals(2, result.deliveries[0].operations.size)
        assertEquals(1, result.deliveries[0].accounts.size)
        assertEquals(1, result.deliveries[0].providers.size)
        assertEquals(1, result.deliveries[0].resources.size)

        val accountResponseDto = result.deliveries[0].accounts[0]
        assertEquals(TEST_ACCOUNT_1.id, accountResponseDto.accountId)
        assertEquals(TEST_ACCOUNT_1.providerId, accountResponseDto.providerId)
        assertEquals(TEST_ACCOUNT_1.displayName.orElseThrow(), accountResponseDto.displayName)
        assertTrue(accountResponseDto.segments!!.contains(ResourceSegmentationSegmentDto(
            "7fbd778f-d803-44c8-831a-c1de5c05885c", "Location", 1,
            "8691406c-2e5f-4873-8bdf-f0bf99ed9bea", "MAN", false)))
        assertTrue(accountResponseDto.segments!!.contains(ResourceSegmentationSegmentDto(
            "4654c7c8-cb87-4a73-8af4-0b8d4a92f16a", "Segment", 0,
            "e9552be0-7b24-4c70-a1e4-dd842299a802", "Default", false)))

        val providerResponseDto = result.deliveries[0].providers[0]
        assertEquals(YP_ID, providerResponseDto.providerId)
        assertEquals("YP", providerResponseDto.displayName)

        val operationResponseDto = result.deliveries[0].operations[0]
        assertEquals(OperationStatusDto.IN_PROGRESS, operationResponseDto.status)
        assertEquals(TEST_ACCOUNT_1.id, operationResponseDto.accountId)
        assertEquals(YP_ID, operationResponseDto.providerId)
        assertEquals(YP_HDD_IVA, operationResponseDto.requestedQuotas[0].resourceId)
        assertEquals("1234567", operationResponseDto.requestedQuotas[0].amount.rawAmount)
        assertEquals(TEST_FOLDER_1_ID, operationResponseDto.folderOperationLogs!!.stream().findFirst().orElseThrow()
            .folderId)
        assertNotNull(operationResponseDto.folderOperationLogs!!.stream().findFirst().orElseThrow().id)
        assertNotNull(operationResponseDto.folderOperationLogs!!.stream().findFirst().orElseThrow().timestamp)

        val resourceResponseDto = result.deliveries[0].resources[0]
        assertEquals(YP_HDD_IVA, resourceResponseDto.resourceId)
        assertEquals("YP-HDD-IVA", resourceResponseDto.displayName)
        assertTrue(resourceResponseDto.segments.contains(ResourceSegmentationSegmentDto(
            "7fbd778f-d803-44c8-831a-c1de5c05885c", "Location", 1,
            "de536d75-04fc-4fa2-bf1c-a382616a4f0c", "IVA", false)))
        assertTrue(resourceResponseDto.segments.contains(ResourceSegmentationSegmentDto(
            "4654c7c8-cb87-4a73-8af4-0b8d4a92f16a", "Segment", 0,
            "e9552be0-7b24-4c70-a1e4-dd842299a802", "Default", false)))

    }

    @Test
    fun deliveryToAccountStatusMappingTest() {
        val firstDelivery = createDelivery()
        val secondDelivery = createDelivery()
        val thirdDelivery = createDelivery()
        val firstDeliveryOperations = createAccountOperations(toOperationIds(firstDelivery),
            requestStatus = RequestStatus.OK)
        val secondDeliveryOperations = createAccountOperations(toOperationIds(secondDelivery))
        val thirdDeliveryOperations = createAccountOperations(toOperationIds(thirdDelivery),
            requestStatus = RequestStatus.ERROR)
        val firstFolderOperations = createFolderOperations(firstDelivery)
        val secondFolderOperations = createFolderOperations(secondDelivery)
        val thirdFolderOperations = createFolderOperations(thirdDelivery)
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            folderOperationLogDao.upsertAllRetryable(
                session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                firstFolderOperations.plus(secondFolderOperations).plus(thirdFolderOperations))
        }.block()
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            accountsQuotasOperationsDao.upsertAllRetryable(
                session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                firstDeliveryOperations.plus(secondDeliveryOperations).plus(thirdDeliveryOperations))
        }.block()
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            deliveriesDao.upsertAllRetryable(
                session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                listOf(firstDelivery, secondDelivery, thirdDelivery))
        }.block()

        val result = webClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .post()
            .uri("/api/v1/delivery/_status")
            .bodyValue(listOf(firstDelivery.deliveryId, secondDelivery.deliveryId, thirdDelivery.deliveryId))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(DeliveryStatusResponseDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(3, result.deliveries.size)
        val deliveriesById = result.deliveries.associateBy { it.deliveryId }
        deliveriesById[firstDelivery.deliveryId]!!.operations.forEach {
            assertEquals(OperationStatusDto.SUCCESS, it.status)
        }
        deliveriesById[secondDelivery.deliveryId]!!.operations.forEach {
            assertEquals(OperationStatusDto.IN_PROGRESS, it.status)
        }
        deliveriesById[thirdDelivery.deliveryId]!!.operations.forEach {
            assertEquals(OperationStatusDto.FAILURE, it.status)
        }
    }

    @Test
    fun nonExistentDeliveryIdTest() {
        val delivery = createDelivery()
        val operations = createAccountOperations(toOperationIds(delivery))
        val folderOperations = createFolderOperations(delivery)
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            folderOperationLogDao.upsertAllRetryable(
                session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                folderOperations)
        }.block()
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            accountsQuotasOperationsDao.upsertAllRetryable(
                session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), operations)
        }.block()
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            deliveriesDao.upsertOneTxRetryable(
                session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), delivery)
        }.block()

        val badId = UUID.randomUUID().toString()

        val result = webClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .post()
            .uri("/api/v1/delivery/_status")
            .bodyValue(listOf(delivery.deliveryId, badId))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(DeliveryStatusResponseDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(1, result.deliveries.size)
        val deliveryResponse = result.deliveries.first { d -> d.deliveryId == delivery.deliveryId }
        assertTrue(deliveryResponse.providers.isNotEmpty())
        assertTrue(deliveryResponse.resources.isNotEmpty())
        assertTrue(deliveryResponse.operations.isNotEmpty())
        assertTrue(deliveryResponse.accounts.isNotEmpty())
    }

    @Test
    fun emptyIdsListTest() {
        val result = webClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .post()
            .uri("/api/v1/delivery/_status")
            .bodyValue(listOf<String>())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(1, result.errors.size)
        assertTrue(result.errors.contains("Non-empty list is required."))
    }

    @Test
    fun invalidDeliveryIdTest() {
        val badId = "invalid uuid"
        val result = webClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .post()
            .uri("/api/v1/delivery/_status")
            .bodyValue(listOf(badId))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(1, result.errors.size)
        assertTrue(result.errors.contains("'$badId' is invalid UUID."))
    }

    @Test
    fun deliveryWithoutOperationTest() {
        val deliveryWithoutOperationId = UUID.randomUUID().toString()
        val deliveryWithoutOperation = DeliveryAndProvideModel.Builder()
            .deliveryId(deliveryWithoutOperationId)
            .request(DeliveryAndProvideRequestModel(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                setOf()))
            .tenantId(Tenants.DEFAULT_TENANT_ID)
            .build()
        val deliveryWithOperations = createDelivery()
        val operations = createAccountOperations(toOperationIds(deliveryWithOperations))
        val folderOperations = createFolderOperations(deliveryWithOperations)
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            folderOperationLogDao.upsertAllRetryable(
                session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                folderOperations)
        }.block()
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            accountsQuotasOperationsDao.upsertAllRetryable(
                session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), operations)
        }.block()
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            deliveriesDao.upsertAllRetryable(
                session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                listOf(deliveryWithoutOperation, deliveryWithOperations))
        }.block()

        val onlyDeliveryWithoutOperationsResult = webClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .post()
            .uri("/api/v1/delivery/_status")
            .bodyValue(listOf(deliveryWithoutOperationId))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(DeliveryStatusResponseDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(1, onlyDeliveryWithoutOperationsResult.deliveries.size)
        assertTrue(onlyDeliveryWithoutOperationsResult.deliveries[0].providers.isEmpty())
        assertTrue(onlyDeliveryWithoutOperationsResult.deliveries[0].operations.isEmpty())
        assertTrue(onlyDeliveryWithoutOperationsResult.deliveries[0].accounts.isEmpty())
        assertTrue(onlyDeliveryWithoutOperationsResult.deliveries[0].resources.isEmpty())

        val bothDeliveriesResult = webClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .post()
            .uri("/api/v1/delivery/_status")
            .bodyValue(listOf(deliveryWithoutOperationId, deliveryWithOperations.deliveryId))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(DeliveryStatusResponseDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(2, bothDeliveriesResult.deliveries.size)
        val emptyDelivery = bothDeliveriesResult.deliveries.first { d -> d.deliveryId == deliveryWithoutOperationId }
        val normalDelivery = bothDeliveriesResult.deliveries.first { d ->
            d.deliveryId == deliveryWithOperations.deliveryId }
        assertTrue(emptyDelivery.providers.isEmpty())
        assertTrue(emptyDelivery.resources.isEmpty())
        assertTrue(emptyDelivery.operations.isEmpty())
        assertTrue(emptyDelivery.accounts.isEmpty())

        assertTrue(normalDelivery.providers.isNotEmpty())
        assertTrue(normalDelivery.resources.isNotEmpty())
        assertTrue(normalDelivery.operations.isNotEmpty())
        assertTrue(normalDelivery.accounts.isNotEmpty())
    }

    @Test
    fun operationWithoutAccountIdTest() {
        val delivery = createDelivery()
        val operationIdsList = ArrayList(delivery.operations)
        val badOperation = AccountsQuotasOperationsModel.builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setOperationId(operationIdsList[0].operations[0].operationId)
            .setCreateDateTime(Instant.now())
            .setOperationSource(OperationSource.USER)
            .setOperationType(AccountsQuotasOperationsModel.OperationType.UPDATE_PROVISION)
            .setAuthorUserId(UUID.randomUUID().toString())
            .setProviderId(YP_ID)
            .setRequestedChanges(OperationChangesModel.builder()
                .frozenProvisions(listOf(
                    OperationChangesModel.Provision(YP_HDD_IVA, 1_234_567)))
                .build())
            .setOrders(OperationOrdersModel.builder().submitOrder(1).build())
            .build()
        val secondOperation = createAccountOperations(operationIdsList[1].operations.map { op -> op.operationId })

        val folderOperations = createFolderOperations(delivery)
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            folderOperationLogDao.upsertAllRetryable(
                session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                folderOperations)
        }.block()
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            accountsQuotasOperationsDao.upsertAllRetryable(
                session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                mutableListOf(badOperation).plus(secondOperation))
        }.block()
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            deliveriesDao.upsertAllRetryable(
                session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                mutableListOf(delivery))
        }.block()

        val result = webClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .post()
            .uri("/api/v1/delivery/_status")
            .bodyValue(listOf(delivery.deliveryId))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(DeliveryStatusResponseDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(1, result.deliveries.size)
        assertTrue(result.deliveries[0].operations.stream().anyMatch { it.accountId == null })
    }

    private fun createDelivery(): DeliveryAndProvideModel {
        val deliveryId = UUID.randomUUID().toString()
        val destination = DeliveryAndProvideDestinationModel.Builder()
            .serviceId(1L)
            .providerId(YP_ID)
            .folderId(TEST_FOLDER_1_ID)
            .accountId(TEST_ACCOUNT_1_ID)
            .resourceId(YP_HDD_IVA)
            .delta(DeliverableDeltaModel.builder()
                .amount(1_234_567L)
                .unitKey("bytes")
                .build())
            .meta(DeliverableMetaRequestModel.builder()
                .bigOrderId(42L)
                .campaignId(1L)
                .quotaRequestId(80L)
                .build())
            .folderOperationModel(DeliverableFolderOperationModel.builder()
                .id(UUID.randomUUID().toString())
                .folderId(TEST_FOLDER_1_ID)
                .operationDateTime(Instant.now())
                .build())
            .build()
        return DeliveryAndProvideModel.Builder()
            .deliveryId(deliveryId)
            .tenantId(Tenants.DEFAULT_TENANT_ID)
            .request(DeliveryAndProvideRequestModel.Builder()
                .deliveryId(deliveryId)
                .authorUid(USER_1_UID)
                .addDeliverable(destination)
                .build())
            .addOperations(setOf(createOperation(UUID.randomUUID().toString(), destination),
                createOperation(UUID.randomUUID().toString(), destination)))
            .build()
    }

    private fun createOperation(operationId: String, destination: DeliveryAndProvideDestinationModel,
                                operationList: MutableList<DeliveryAndProvideOperationModel> = mutableListOf())
        : DeliveryAndProvideOperationListModel {
        return DeliveryAndProvideOperationListModel.Builder()
            .serviceId(destination.serviceId)
            .folderId(destination.folderId)
            .accountId(destination.accountId)
            .addOperations(operationList)
            .addOperation(createOperationList(operationId))
            .build()
    }

    private fun createOperationList(operationId: String, version: Int = 0): DeliveryAndProvideOperationModel {
        return DeliveryAndProvideOperationModel.Builder()
            .version(version)
            .operationId(operationId)
            .build()
    }

    private fun createAccountOperations(ids: Collection<String>,
                                        requestStatus: RequestStatus = RequestStatus.WAITING
    ) = ids.map { id ->
        AccountsQuotasOperationsModel.builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setOperationId(id)
            .setCreateDateTime(Instant.now())
            .setOperationSource(OperationSource.USER)
            .setOperationType(AccountsQuotasOperationsModel.OperationType.DELIVER_AND_UPDATE_PROVISION)
            .setAuthorUserId(UUID.randomUUID().toString())
            .setProviderId(YP_ID)
            .setRequestedChanges(OperationChangesModel.builder()
                .accountId(TEST_ACCOUNT_1_ID)
                .frozenProvisions(listOf(
                    OperationChangesModel.Provision(YP_HDD_IVA, 1_234_567)))
                .build())
            .setOrders(OperationOrdersModel.builder().submitOrder(1).build())
            .setRequestStatus(requestStatus)
            .build()
    }

    private fun createFolderOperations(delivery: DeliveryAndProvideModel) = delivery.request.deliverables
        .map { d ->
            FolderOperationLogModel.builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setFolderId(d.folderOperationModel!!.folderId)
                .setOperationDateTime(d.folderOperationModel!!.operationDateTime)
                .setId(d.folderOperationModel!!.id)
                .setProviderRequestId(null)
                .setOperationType(FolderOperationType.QUOTA_DELIVERY)
                .setAuthorUserId(USER_1_ID)
                .setAuthorUserUid(USER_1_UID)
                .setAuthorProviderId(null)
                .setSourceFolderOperationsLogId(null)
                .setDestinationFolderOperationsLogId(null)
                .setOldFolderFields(null)
                .setOldQuotas(QuotasByResource(mapOf(YP_HDD_IVA to 0L)))
                .setOldBalance(QuotasByResource(mapOf()))
                .setOldProvisions(QuotasByAccount(mapOf()))
                .setOldAccounts(null)
                .setNewFolderFields(null)
                .setNewQuotas(QuotasByResource(mapOf(YP_HDD_IVA to 1_234_567L)))
                .setNewBalance(QuotasByResource(mapOf()))
                .setNewProvisions(QuotasByAccount(mapOf()))
                .setActuallyAppliedProvisions(null)
                .setNewAccounts(null)
                .setAccountsQuotasOperationsId(null)
                .setQuotasDemandsId(null)
                .setOperationPhase(null)
                .setOrder(1L)
                .setCommentId(null)
                .setDeliveryMeta(
                    DeliverableMetaHistoryModel.builder()
                        .deliveryId(delivery.deliveryId)
                        .quotaRequestId(d.meta.quotaRequestId)
                        .campaignId(d.meta.campaignId)
                        .bigOrderIds(d.meta.bigOrderId)
                        .build())
                .build()
        }

    private fun toOperationIds(firstDelivery: DeliveryAndProvideModel) =
        firstDelivery.operations.flatMap { operations ->
            operations.operations.map { op -> op.operationId }
        }
}
