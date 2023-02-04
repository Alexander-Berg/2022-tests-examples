package ru.yandex.intranet.d.web.front.aggregation

import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.UnitIds
import ru.yandex.intranet.d.UnitsEnsembleIds
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.accounts.AccountsDao
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao
import ru.yandex.intranet.d.dao.aggregates.ServiceDenormalizedAggregatesDao
import ru.yandex.intranet.d.dao.folders.FolderDao
import ru.yandex.intranet.d.dao.providers.ProvidersDao
import ru.yandex.intranet.d.dao.quotas.QuotasDao
import ru.yandex.intranet.d.dao.resources.ResourcesDao
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao
import ru.yandex.intranet.d.dao.services.ServicesDao
import ru.yandex.intranet.d.dao.usage.ServiceUsageDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.accounts.AccountModel
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel
import ru.yandex.intranet.d.model.aggregates.AggregateBundle
import ru.yandex.intranet.d.model.aggregates.ServiceDenormalizedAggregateAmounts
import ru.yandex.intranet.d.model.aggregates.ServiceDenormalizedAggregateKey
import ru.yandex.intranet.d.model.aggregates.ServiceDenormalizedAggregateModel
import ru.yandex.intranet.d.model.folders.FolderModel
import ru.yandex.intranet.d.model.folders.FolderType
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel
import ru.yandex.intranet.d.model.providers.AggregationSettings
import ru.yandex.intranet.d.model.providers.FreeProvisionAggregationMode
import ru.yandex.intranet.d.model.providers.ProviderModel
import ru.yandex.intranet.d.model.providers.UsageMode
import ru.yandex.intranet.d.model.quotas.QuotaModel
import ru.yandex.intranet.d.model.resources.ResourceModel
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel
import ru.yandex.intranet.d.model.services.ServiceRecipeModel
import ru.yandex.intranet.d.model.services.ServiceState
import ru.yandex.intranet.d.model.usage.ServiceUsageModel
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.serviceUsageModel
import ru.yandex.intranet.d.web.model.aggregation.FindServiceTotalsRequestDto
import ru.yandex.intranet.d.web.model.aggregation.FindServiceTotalsRequestFilterDto
import ru.yandex.intranet.d.web.model.aggregation.FindServiceTotalsRequestFilterSegmentDto
import ru.yandex.intranet.d.web.model.aggregation.FindServiceTotalsResponseDto
import ru.yandex.intranet.d.web.model.aggregation.FindSubtreeTotalRequestDto
import ru.yandex.intranet.d.web.model.aggregation.FindSubtreeTotalResponseDto
import ru.yandex.intranet.d.web.model.aggregation.RankSubtreeAmountsRequestDto
import ru.yandex.intranet.d.web.model.aggregation.RankSubtreeAmountsResponseDto
import ru.yandex.intranet.d.web.model.aggregation.RankSubtreeSortingField
import ru.yandex.intranet.d.web.model.aggregation.RankSubtreeSortingOrder
import ru.yandex.intranet.d.web.model.aggregation.RankSubtreeSortingParamsDto
import java.math.BigInteger
import java.time.Instant
import java.util.*

/**
 * Front aggregation with usage API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class FrontAggregationWithUsageTest(
    @Autowired private val serviceDenormalizedAggregatesDao: ServiceDenormalizedAggregatesDao,
    @Autowired private val providersDao: ProvidersDao,
    @Autowired private val resourceTypesDao: ResourceTypesDao,
    @Autowired private val resourcesDao: ResourcesDao,
    @Autowired private val servicesDao: ServicesDao,
    @Autowired private val folderDao: FolderDao,
    @Autowired private val quotasDao: QuotasDao,
    @Autowired private val accountsDao: AccountsDao,
    @Autowired private val accountsQuotasDao: AccountsQuotasDao,
    @Autowired private val resourceSegmentationsDao: ResourceSegmentationsDao,
    @Autowired private val resourceSegmentsDao: ResourceSegmentsDao,
    @Autowired private val serviceUsageDao: ServiceUsageDao,
    @Autowired private val tableClient: YdbTableClient,
    @Autowired private val webClient: WebTestClient,
    @Autowired private val helper: FrontAggregationTestHelper
) {

    @Test
    fun testEmpty(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne, childTwo, grandchildOne, grandchildTwo))
                .awaitSingleOrNull()
        }}
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne, folderChildTwo,
                folderGrandchildOne, folderGrandchildTwo)).awaitSingleOrNull()
        }}
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProviderRetryable(txSession, provider)
            .awaitSingleOrNull() }}
        val resourceType = resourceTypeModel(provider.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            .awaitSingleOrNull() }}
        val resource = resourceModel(provider.id, "test", resourceType.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourceRetryable(txSession, resource)
            .awaitSingleOrNull() }}
        val resultRoot = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findSubtreeTotal")
            .bodyValue(FindSubtreeTotalRequestDto(root.id, resource.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindSubtreeTotalResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultRoot.aggregates.size)
        val resultSubtreeRoot = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsRequestDto(root.id, resource.id, from = null, limit = 100, sortingParams = null, unitId = null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(0, resultSubtreeRoot.aggregates.size)
    }

    @Test
    fun testSubtreeTotal(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne, childTwo, grandchildOne, grandchildTwo))
                .awaitSingleOrNull()
        }}
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne, folderChildTwo,
                folderGrandchildOne, folderGrandchildTwo)).awaitSingleOrNull()
        }}
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProviderRetryable(txSession, provider)
            .awaitSingleOrNull() }}
        val resourceType = resourceTypeModel(provider.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            .awaitSingleOrNull() }}
        val resource = resourceModel(provider.id, "test", resourceType.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourceRetryable(txSession, resource)
            .awaitSingleOrNull() }}
        val accountRoot = accountModel(provider.id, null, folderRoot.id, "test")
        val accountChildOne = accountModel(provider.id, null, folderChildOne.id, "test")
        val accountChildTwo = accountModel(provider.id, null, folderChildTwo.id, "test")
        val accountGrandchildOne = accountModel(provider.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwo = accountModel(provider.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRoot, accountChildOne, accountChildTwo,
                accountGrandchildOne, accountGrandchildTwo)).awaitSingleOrNull()
        }}
        val quotaRoot = quotaModel(provider.id, resource.id, folderRoot.id, 10L, 5L)
        val quotaChildOne = quotaModel(provider.id, resource.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwo = quotaModel(provider.id, resource.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOne = quotaModel(provider.id, resource.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwo = quotaModel(provider.id, resource.id, folderGrandchildTwo.id, 60L, 30L)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.upsertAllRetryable(txSession, listOf(quotaRoot, quotaChildOne, quotaChildTwo,
                quotaGrandchildOne, quotaGrandchildTwo)).awaitSingleOrNull()
        }}
        val provisionRoot = accountQuotaModel(provider.id, resource.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOne = accountQuotaModel(provider.id, resource.id, folderChildOne.id, accountChildOne.id,
            15L, 6L)
        val provisionChildTwo = accountQuotaModel(provider.id, resource.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L)
        val provisionGrandchildOne = accountQuotaModel(provider.id, resource.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L)
        val provisionGrandchildTwo = accountQuotaModel(provider.id, resource.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.upsertAllRetryable(txSession, listOf(provisionRoot, provisionChildOne, provisionChildTwo,
                provisionGrandchildOne, provisionGrandchildTwo)).awaitSingleOrNull()
        }}
        val now = Instant.now()
        val usageRoot = serviceUsageModel(root.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L))
        val usageChildOne = serviceUsageModel(childOne.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L))
        val usageChildTwo = serviceUsageModel(childTwo.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L))
        val usageGrandchildOne = serviceUsageModel(grandchildOne.id, provider.id, resource.id, now,
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null)
        val usageGrandchildTwo = serviceUsageModel(grandchildTwo.id, provider.id, resource.id, now,
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, listOf(usageRoot, usageChildOne, usageChildTwo,
                usageGrandchildOne, usageGrandchildTwo))
        }}
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        val resultRoot = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findSubtreeTotal")
            .bodyValue(FindSubtreeTotalRequestDto(root.id, resource.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindSubtreeTotalResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultRoot.aggregates.size)
        Assertions.assertEquals(root.id, resultRoot.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultRoot.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultRoot.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("190",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("95",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("95",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("38",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("57",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("152",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        val resultChildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findSubtreeTotal")
            .bodyValue(FindSubtreeTotalRequestDto(childOne.id, resource.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindSubtreeTotalResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildOne.aggregates.size)
        Assertions.assertEquals(childOne.id, resultChildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultChildOne.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultChildOne.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("60",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("30",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("30",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("12",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("18",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("48",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        val resultChildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findSubtreeTotal")
            .bodyValue(FindSubtreeTotalRequestDto(childTwo.id, resource.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindSubtreeTotalResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildTwo.aggregates.size)
        Assertions.assertEquals(childTwo.id, resultChildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultChildTwo.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("120",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("60",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("60",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("24",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("36",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("96",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        val resultGrandchildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findSubtreeTotal")
            .bodyValue(FindSubtreeTotalRequestDto(grandchildOne.id, resource.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindSubtreeTotalResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildOne.aggregates.size)
        Assertions.assertEquals(grandchildOne.id, resultGrandchildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultGrandchildOne.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("30",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("15",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("15",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("6",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("9",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("24",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        val resultGrandchildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findSubtreeTotal")
            .bodyValue(FindSubtreeTotalRequestDto(grandchildTwo.id, resource.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindSubtreeTotalResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultGrandchildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultGrandchildTwo.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("60",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("30",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("30",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("12",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("18",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("48",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
    }

    @Test
    fun testRankSubtree(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne, childTwo, grandchildOne, grandchildTwo))
                .awaitSingleOrNull()
        }}
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne, folderChildTwo,
                folderGrandchildOne, folderGrandchildTwo)).awaitSingleOrNull()
        }}
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProviderRetryable(txSession, provider)
            .awaitSingleOrNull() }}
        val resourceType = resourceTypeModel(provider.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            .awaitSingleOrNull() }}
        val resource = resourceModel(provider.id, "test", resourceType.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourceRetryable(txSession, resource)
            .awaitSingleOrNull() }}
        val accountRoot = accountModel(provider.id, null, folderRoot.id, "test")
        val accountChildOne = accountModel(provider.id, null, folderChildOne.id, "test")
        val accountChildTwo = accountModel(provider.id, null, folderChildTwo.id, "test")
        val accountGrandchildOne = accountModel(provider.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwo = accountModel(provider.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRoot, accountChildOne, accountChildTwo,
                accountGrandchildOne, accountGrandchildTwo)).awaitSingleOrNull()
        }}
        val quotaRoot = quotaModel(provider.id, resource.id, folderRoot.id, 10L, 5L)
        val quotaChildOne = quotaModel(provider.id, resource.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwo = quotaModel(provider.id, resource.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOne = quotaModel(provider.id, resource.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwo = quotaModel(provider.id, resource.id, folderGrandchildTwo.id, 60L, 30L)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.upsertAllRetryable(txSession, listOf(quotaRoot, quotaChildOne, quotaChildTwo,
                quotaGrandchildOne, quotaGrandchildTwo)).awaitSingleOrNull()
        }}
        val provisionRoot = accountQuotaModel(provider.id, resource.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOne = accountQuotaModel(provider.id, resource.id, folderChildOne.id, accountChildOne.id,
            15L, 6L)
        val provisionChildTwo = accountQuotaModel(provider.id, resource.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L)
        val provisionGrandchildOne = accountQuotaModel(provider.id, resource.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L)
        val provisionGrandchildTwo = accountQuotaModel(provider.id, resource.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.upsertAllRetryable(txSession, listOf(provisionRoot, provisionChildOne, provisionChildTwo,
                provisionGrandchildOne, provisionGrandchildTwo)).awaitSingleOrNull()
        }}
        val now = Instant.now()
        val usageRoot = serviceUsageModel(root.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L))
        val usageChildOne = serviceUsageModel(childOne.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L))
        val usageChildTwo = serviceUsageModel(childTwo.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L))
        val usageGrandchildOne = serviceUsageModel(grandchildOne.id, provider.id, resource.id, now,
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null)
        val usageGrandchildTwo = serviceUsageModel(grandchildTwo.id, provider.id, resource.id, now,
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, listOf(usageRoot, usageChildOne, usageChildTwo,
                usageGrandchildOne, usageGrandchildTwo))
        }}
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        val resultRoot = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsRequestDto(root.id, resource.id, from = null, limit = 100, sortingParams = null, unitId = null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(5, resultRoot.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultRoot.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].folders.size)
        Assertions.assertEquals(folderGrandchildTwo.id, resultRoot.aggregates[0].folders[0].id)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultRoot.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultRoot.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("60",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("30",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("30",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("12",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("18",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("48",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        Assertions.assertEquals(childTwo.id, resultRoot.aggregates[1].serviceId)
        Assertions.assertEquals(1, resultRoot.aggregates[1].folders.size)
        Assertions.assertEquals(folderChildTwo.id, resultRoot.aggregates[1].folders[0].id)
        Assertions.assertEquals(1, resultRoot.aggregates[1].providers.size)
        Assertions.assertEquals(provider.id, resultRoot.aggregates[1].providers[0].providerId)
        Assertions.assertEquals(1, resultRoot.aggregates[1].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultRoot.aggregates[1].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultRoot.aggregates[1].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultRoot.aggregates[1].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("30",
            resultRoot.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("30",
            resultRoot.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("12",
            resultRoot.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("18",
            resultRoot.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("48",
            resultRoot.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultRoot.aggregates[1].providers[0].resourceTypes[0].resources[0].usage)
        Assertions.assertEquals(grandchildOne.id, resultRoot.aggregates[2].serviceId)
        Assertions.assertEquals(1, resultRoot.aggregates[2].folders.size)
        Assertions.assertEquals(folderGrandchildOne.id, resultRoot.aggregates[2].folders[0].id)
        Assertions.assertEquals(1, resultRoot.aggregates[2].providers.size)
        Assertions.assertEquals(provider.id, resultRoot.aggregates[2].providers[0].providerId)
        Assertions.assertEquals(1, resultRoot.aggregates[2].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultRoot.aggregates[2].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultRoot.aggregates[2].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultRoot.aggregates[2].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("15",
            resultRoot.aggregates[2].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("15",
            resultRoot.aggregates[2].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("6",
            resultRoot.aggregates[2].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("9",
            resultRoot.aggregates[2].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("24",
            resultRoot.aggregates[2].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultRoot.aggregates[2].providers[0].resourceTypes[0].resources[0].usage)
        Assertions.assertEquals(childOne.id, resultRoot.aggregates[3].serviceId)
        Assertions.assertEquals(1, resultRoot.aggregates[3].folders.size)
        Assertions.assertEquals(folderChildOne.id, resultRoot.aggregates[3].folders[0].id)
        Assertions.assertEquals(1, resultRoot.aggregates[3].providers.size)
        Assertions.assertEquals(provider.id, resultRoot.aggregates[3].providers[0].providerId)
        Assertions.assertEquals(1, resultRoot.aggregates[3].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultRoot.aggregates[3].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultRoot.aggregates[3].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultRoot.aggregates[3].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("15",
            resultRoot.aggregates[3].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("15",
            resultRoot.aggregates[3].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("6",
            resultRoot.aggregates[3].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("9",
            resultRoot.aggregates[3].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("24",
            resultRoot.aggregates[3].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultRoot.aggregates[3].providers[0].resourceTypes[0].resources[0].usage)
        Assertions.assertEquals(root.id, resultRoot.aggregates[4].serviceId)
        Assertions.assertEquals(1, resultRoot.aggregates[4].folders.size)
        Assertions.assertEquals(folderRoot.id, resultRoot.aggregates[4].folders[0].id)
        Assertions.assertEquals(1, resultRoot.aggregates[4].providers.size)
        Assertions.assertEquals(provider.id, resultRoot.aggregates[4].providers[0].providerId)
        Assertions.assertEquals(1, resultRoot.aggregates[4].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultRoot.aggregates[4].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultRoot.aggregates[4].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultRoot.aggregates[4].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("5",
            resultRoot.aggregates[4].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("5",
            resultRoot.aggregates[4].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("2",
            resultRoot.aggregates[4].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("3",
            resultRoot.aggregates[4].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("8",
            resultRoot.aggregates[4].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultRoot.aggregates[4].providers[0].resourceTypes[0].resources[0].usage)
        val resultChildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsRequestDto(childOne.id, resource.id, from = null, limit = 100, sortingParams = null, unitId = null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(2, resultChildOne.aggregates.size)
        Assertions.assertEquals(grandchildOne.id, resultChildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].folders.size)
        Assertions.assertEquals(folderGrandchildOne.id, resultChildOne.aggregates[0].folders[0].id)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultChildOne.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultChildOne.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("60",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("15",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("15",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("6",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("9",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("24",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        Assertions.assertEquals(childOne.id, resultChildOne.aggregates[1].serviceId)
        Assertions.assertEquals(1, resultChildOne.aggregates[1].folders.size)
        Assertions.assertEquals(folderChildOne.id, resultChildOne.aggregates[1].folders[0].id)
        Assertions.assertEquals(1, resultChildOne.aggregates[1].providers.size)
        Assertions.assertEquals(provider.id, resultChildOne.aggregates[1].providers[0].providerId)
        Assertions.assertEquals(1, resultChildOne.aggregates[1].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultChildOne.aggregates[1].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultChildOne.aggregates[1].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultChildOne.aggregates[1].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("15",
            resultChildOne.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("15",
            resultChildOne.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("6",
            resultChildOne.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("9",
            resultChildOne.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("24",
            resultChildOne.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultChildOne.aggregates[1].providers[0].resourceTypes[0].resources[0].usage)
        val resultChildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsRequestDto(childTwo.id, resource.id, from = null, limit = 100, sortingParams = null, unitId = null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(2, resultChildTwo.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultChildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].folders.size)
        Assertions.assertEquals(folderGrandchildTwo.id, resultChildTwo.aggregates[0].folders[0].id)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultChildTwo.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("60",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("30",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("30",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("12",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("18",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("48",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        Assertions.assertEquals(childTwo.id, resultChildTwo.aggregates[1].serviceId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[1].folders.size)
        Assertions.assertEquals(folderChildTwo.id, resultChildTwo.aggregates[1].folders[0].id)
        Assertions.assertEquals(1, resultChildTwo.aggregates[1].providers.size)
        Assertions.assertEquals(provider.id, resultChildTwo.aggregates[1].providers[0].providerId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[1].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultChildTwo.aggregates[1].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[1].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultChildTwo.aggregates[1].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("30",
            resultChildTwo.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("30",
            resultChildTwo.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("12",
            resultChildTwo.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("18",
            resultChildTwo.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("48",
            resultChildTwo.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultChildTwo.aggregates[1].providers[0].resourceTypes[0].resources[0].usage)
        val resultGrandchildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsRequestDto(grandchildOne.id, resource.id, from = null, limit = 100, sortingParams = null, unitId = null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildOne.aggregates.size)
        Assertions.assertEquals(grandchildOne.id, resultGrandchildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].folders.size)
        Assertions.assertEquals(folderGrandchildOne.id, resultGrandchildOne.aggregates[0].folders[0].id)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultGrandchildOne.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("60",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("15",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("15",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("6",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("9",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("24",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        val resultGrandchildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsRequestDto(grandchildTwo.id, resource.id, from = null, limit = 100, sortingParams = null, unitId = null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultGrandchildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].folders.size)
        Assertions.assertEquals(folderGrandchildTwo.id, resultGrandchildTwo.aggregates[0].folders[0].id)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultGrandchildTwo.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("60",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("30",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("30",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("12",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("18",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("48",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
    }

    @Test
    fun testRankSubtreePaging(): Unit = runBlocking {
        var serviceId = 65535L
        val services = mutableListOf<ServiceRecipeModel>()
        repeat(250) {
            services.add(serviceModel(serviceId++, null))
        }
        val root = serviceModel(serviceId++, null)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, services + listOf(root)).awaitSingleOrNull()
        }}
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProviderRetryable(txSession, provider)
            .awaitSingleOrNull() }}
        val resourceType = resourceTypeModel(provider.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            .awaitSingleOrNull() }}
        val resource = resourceModel(provider.id, "test", resourceType.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourceRetryable(txSession, resource)
            .awaitSingleOrNull() }}
        var transferable = services.size.toLong()
        val aggregates = services.map { ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root.id,
                resourceId = resource.id,
                serviceId = it.id
            ),
            lastUpdate = Instant.now(),
            epoch = 0L,
            providerId = provider.id,
            transferable = transferable--,
            deallocatable = 0L,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(60),
                    balance = BigInteger.valueOf(30),
                    provided = BigInteger.valueOf(30),
                    allocated = BigInteger.valueOf(15),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(15),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(45),
                    deallocatable = BigInteger.ZERO,
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = null
                )
            )
        ) }
        dbSessionRetryable(tableClient) { rwTxRetryable { serviceDenormalizedAggregatesDao.upsertManyRetryable(txSession, aggregates) }}
        val now = Instant.now()
        val usages = mutableListOf<ServiceUsageModel>()
        (services + listOf(root)).forEach {
            usages.add(serviceUsageModel(it.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L)))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, usages)
        }}
        val resultPageOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsRequestDto(root.id, resource.id, from = null, limit = 100, sortingParams = null, unitId = null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(100, resultPageOne.aggregates.size)
        Assertions.assertNotNull(resultPageOne.nextPageToken)
        val resultPageTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsRequestDto(root.id, resource.id, from = resultPageOne.nextPageToken, limit = 100, sortingParams = null, unitId = null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(100, resultPageTwo.aggregates.size)
        Assertions.assertNotNull(resultPageTwo.nextPageToken)
        val resultPageThree = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsRequestDto(root.id, resource.id, from = resultPageTwo.nextPageToken, limit = 100, sortingParams = null, unitId = null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(50, resultPageThree.aggregates.size)
        Assertions.assertNull(resultPageThree.nextPageToken)
    }

    @Test
    fun testServiceEmpty(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne, childTwo, grandchildOne, grandchildTwo))
                .awaitSingleOrNull()
        }}
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne, folderChildTwo,
                folderGrandchildOne, folderGrandchildTwo)).awaitSingleOrNull()
        }}
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProviderRetryable(txSession, provider)
            .awaitSingleOrNull() }}
        val resourceType = resourceTypeModel(provider.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            .awaitSingleOrNull() }}
        val resource = resourceModel(provider.id, "test", resourceType.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourceRetryable(txSession, resource)
            .awaitSingleOrNull() }}
        val resultRoot = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(root.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultRoot.aggregates.size)
    }

    @Test
    fun testServiceTotal(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne, childTwo, grandchildOne, grandchildTwo))
                .awaitSingleOrNull()
        }}
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne, folderChildTwo,
                folderGrandchildOne, folderGrandchildTwo)).awaitSingleOrNull()
        }}
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProviderRetryable(txSession, provider)
            .awaitSingleOrNull() }}
        val resourceType = resourceTypeModel(provider.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            .awaitSingleOrNull() }}
        val resource = resourceModel(provider.id, "test", resourceType.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourceRetryable(txSession, resource)
            .awaitSingleOrNull() }}
        val accountRoot = accountModel(provider.id, null, folderRoot.id, "test")
        val accountChildOne = accountModel(provider.id, null, folderChildOne.id, "test")
        val accountChildTwo = accountModel(provider.id, null, folderChildTwo.id, "test")
        val accountGrandchildOne = accountModel(provider.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwo = accountModel(provider.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRoot, accountChildOne, accountChildTwo,
                accountGrandchildOne, accountGrandchildTwo)).awaitSingleOrNull()
        }}
        val quotaRoot = quotaModel(provider.id, resource.id, folderRoot.id, 10L, 5L)
        val quotaChildOne = quotaModel(provider.id, resource.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwo = quotaModel(provider.id, resource.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOne = quotaModel(provider.id, resource.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwo = quotaModel(provider.id, resource.id, folderGrandchildTwo.id, 60L, 30L)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.upsertAllRetryable(txSession, listOf(quotaRoot, quotaChildOne, quotaChildTwo,
                quotaGrandchildOne, quotaGrandchildTwo)).awaitSingleOrNull()
        }}
        val provisionRoot = accountQuotaModel(provider.id, resource.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOne = accountQuotaModel(provider.id, resource.id, folderChildOne.id, accountChildOne.id,
            15L, 6L)
        val provisionChildTwo = accountQuotaModel(provider.id, resource.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L)
        val provisionGrandchildOne = accountQuotaModel(provider.id, resource.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L)
        val provisionGrandchildTwo = accountQuotaModel(provider.id, resource.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.upsertAllRetryable(txSession, listOf(provisionRoot, provisionChildOne, provisionChildTwo,
                provisionGrandchildOne, provisionGrandchildTwo)).awaitSingleOrNull()
        }}
        val now = Instant.now()
        val usageRoot = serviceUsageModel(root.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L))
        val usageChildOne = serviceUsageModel(childOne.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L))
        val usageChildTwo = serviceUsageModel(childTwo.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L))
        val usageGrandchildOne = serviceUsageModel(grandchildOne.id, provider.id, resource.id, now,
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null)
        val usageGrandchildTwo = serviceUsageModel(grandchildTwo.id, provider.id, resource.id, now,
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, listOf(usageRoot, usageChildOne, usageChildTwo,
                usageGrandchildOne, usageGrandchildTwo))
        }}
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        val resultRoot = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(root.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultRoot.aggregates.size)
        Assertions.assertEquals(root.id, resultRoot.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultRoot.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultRoot.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("190",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("95",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("95",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("38",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("57",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("95",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("95",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("38",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("57",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].amounts.unallocated?.rawAmount)
        Assertions.assertNotNull(resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        val resultChildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childOne.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildOne.aggregates.size)
        Assertions.assertEquals(childOne.id, resultChildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultChildOne.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultChildOne.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("190",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("30",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("30",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("12",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("18",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("30",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("30",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("12",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("18",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].amounts.unallocated?.rawAmount)
        Assertions.assertNotNull(resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        val resultChildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childTwo.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildTwo.aggregates.size)
        Assertions.assertEquals(childTwo.id, resultChildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultChildTwo.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("190",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("60",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("60",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("24",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("36",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("60",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("60",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("24",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("36",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].amounts.unallocated?.rawAmount)
        Assertions.assertNotNull(resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        val resultGrandchildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildOne.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildOne.aggregates.size)
        Assertions.assertEquals(grandchildOne.id, resultGrandchildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultGrandchildOne.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("190",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("15",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("15",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("6",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("9",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("15",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("15",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("6",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("9",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].amounts.unallocated?.rawAmount)
        Assertions.assertNotNull(resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        val resultGrandchildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildTwo.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultGrandchildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultGrandchildTwo.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("190",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("30",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("30",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("12",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("18",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("30",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("30",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("12",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("18",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].amounts.unallocated?.rawAmount)
        Assertions.assertNotNull(resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
    }

    @Test
    fun testServiceTotalManyProviders(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne, childTwo, grandchildOne, grandchildTwo))
                .awaitSingleOrNull()
        }}
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne, folderChildTwo,
                folderGrandchildOne, folderGrandchildTwo)).awaitSingleOrNull()
        }}
        val providerOne = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        val providerTwo = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProvidersRetryable(txSession,
            listOf(providerOne, providerTwo)).awaitSingleOrNull() }}
        val resourceTypeOne = resourceTypeModel(providerOne.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeTwo = resourceTypeModel(providerOne.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeThree = resourceTypeModel(providerTwo.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeFour = resourceTypeModel(providerTwo.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypesRetryable(txSession,
            listOf(resourceTypeOne, resourceTypeTwo, resourceTypeThree, resourceTypeFour)).awaitSingleOrNull() }}
        val resourceOne = resourceModel(providerOne.id, "test", resourceTypeOne.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceTwo = resourceModel(providerOne.id, "test", resourceTypeOne.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceThree = resourceModel(providerOne.id, "test", resourceTypeTwo.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceFour = resourceModel(providerOne.id, "test", resourceTypeTwo.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceFive = resourceModel(providerTwo.id, "test", resourceTypeThree.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceSix = resourceModel(providerTwo.id, "test", resourceTypeThree.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceSeven = resourceModel(providerTwo.id, "test", resourceTypeFour.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceEight = resourceModel(providerTwo.id, "test", resourceTypeFour.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourcesRetryable(txSession,
            listOf(resourceOne, resourceTwo, resourceThree, resourceFour, resourceFive, resourceSix, resourceSeven,
                resourceEight)).awaitSingleOrNull() }}
        val accountRootProviderOne = accountModel(providerOne.id, null, folderRoot.id, "test")
        val accountChildOneProviderOne = accountModel(providerOne.id, null, folderChildOne.id, "test")
        val accountChildTwoProviderOne = accountModel(providerOne.id, null, folderChildTwo.id, "test")
        val accountGrandchildOneProviderOne = accountModel(providerOne.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwoProviderOne = accountModel(providerOne.id, null, folderGrandchildTwo.id, "test")
        val accountRootProviderTwo = accountModel(providerTwo.id, null, folderRoot.id, "test")
        val accountChildOneProviderTwo = accountModel(providerTwo.id, null, folderChildOne.id, "test")
        val accountChildTwoProviderTwo = accountModel(providerTwo.id, null, folderChildTwo.id, "test")
        val accountGrandchildOneProviderTwo = accountModel(providerTwo.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwoProviderTwo = accountModel(providerTwo.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRootProviderOne, accountChildOneProviderOne, accountChildTwoProviderOne,
                accountGrandchildOneProviderOne, accountGrandchildTwoProviderOne, accountRootProviderTwo,
                accountChildOneProviderTwo, accountChildTwoProviderTwo, accountGrandchildOneProviderTwo,
                accountGrandchildTwoProviderTwo)).awaitSingleOrNull()
        }}
        val quotas = mutableListOf<QuotaModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            quotas.add(quotaModel(providerOne.id, it.id, folderRoot.id, 10L, 5L))
            quotas.add(quotaModel(providerOne.id, it.id, folderChildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerOne.id, it.id, folderChildTwo.id, 60L, 30L))
            quotas.add(quotaModel(providerOne.id, it.id, folderGrandchildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerOne.id, it.id, folderGrandchildTwo.id, 60L, 30L))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            quotas.add(quotaModel(providerTwo.id, it.id, folderRoot.id, 10L, 5L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderChildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderChildTwo.id, 60L, 30L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderGrandchildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderGrandchildTwo.id, 60L, 30L))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.upsertAllRetryable(txSession, quotas).awaitSingleOrNull()
        }}
        val provisions = mutableListOf<AccountsQuotasModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderRoot.id, accountRootProviderOne.id,
                5L, 2L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderChildOne.id, accountChildOneProviderOne.id,
                15L, 6L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderChildTwo.id, accountChildTwoProviderOne.id,
                30L, 12L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderGrandchildOne.id,
                accountGrandchildOneProviderOne.id, 15L, 6L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderGrandchildTwo.id,
                accountGrandchildTwoProviderOne.id, 30L, 12L))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderRoot.id, accountRootProviderTwo.id,
                5L, 2L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderChildOne.id, accountChildOneProviderTwo.id,
                15L, 6L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderChildTwo.id, accountChildTwoProviderTwo.id,
                30L, 12L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderGrandchildOne.id,
                accountGrandchildOneProviderTwo.id, 15L, 6L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderGrandchildTwo.id,
                accountGrandchildTwoProviderTwo.id, 30L, 12L))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.upsertAllRetryable(txSession, provisions).awaitSingleOrNull()
        }}
        val now = Instant.now()
        val usages = mutableListOf<ServiceUsageModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            usages.add(serviceUsageModel(root.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L)))
            usages.add(serviceUsageModel(childOne.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(childTwo.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(grandchildOne.id, providerOne.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
            usages.add(serviceUsageModel(grandchildTwo.id, providerOne.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            usages.add(serviceUsageModel(root.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L)))
            usages.add(serviceUsageModel(childOne.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(childTwo.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(grandchildOne.id, providerTwo.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
            usages.add(serviceUsageModel(grandchildTwo.id, providerTwo.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, usages)
        }}
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", providerOne.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", providerTwo.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        val resultRoot = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(root.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultRoot.aggregates.size)
        Assertions.assertEquals(root.id, resultRoot.aggregates[0].serviceId)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[1].resourceTypes.size)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[1].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[1].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultRoot.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultRoot.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
        Assertions.assertTrue(resultRoot.aggregates[0].providers[1].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultRoot.aggregates[0].providers[1].resourceTypes[1].resources.all { it.usage != null })
        val resultChildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childOne.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildOne.aggregates.size)
        Assertions.assertEquals(childOne.id, resultChildOne.aggregates[0].serviceId)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[1].resourceTypes.size)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[1].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[1].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultChildOne.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
        Assertions.assertTrue(resultChildOne.aggregates[0].providers[1].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultChildOne.aggregates[0].providers[1].resourceTypes[1].resources.all { it.usage != null })
        val resultChildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childTwo.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildTwo.aggregates.size)
        Assertions.assertEquals(childTwo.id, resultChildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[1].resourceTypes.size)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[1].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[1].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultChildTwo.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
        Assertions.assertTrue(resultChildTwo.aggregates[0].providers[1].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultChildTwo.aggregates[0].providers[1].resourceTypes[1].resources.all { it.usage != null })
        val resultGrandchildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildOne.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildOne.aggregates.size)
        Assertions.assertEquals(grandchildOne.id, resultGrandchildOne.aggregates[0].serviceId)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[1].resourceTypes.size)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[1].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[1].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultGrandchildOne.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
        Assertions.assertTrue(resultGrandchildOne.aggregates[0].providers[1].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultGrandchildOne.aggregates[0].providers[1].resourceTypes[1].resources.all { it.usage != null })
        val resultGrandchildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildTwo.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultGrandchildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[1].resourceTypes.size)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[1].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[1].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
        Assertions.assertTrue(resultGrandchildTwo.aggregates[0].providers[1].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultGrandchildTwo.aggregates[0].providers[1].resourceTypes[1].resources.all { it.usage != null })
    }

    @Test
    fun testServiceTotalManyProvidersSegmented(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne, childTwo, grandchildOne, grandchildTwo))
                .awaitSingleOrNull()
        }}
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne, folderChildTwo,
                folderGrandchildOne, folderGrandchildTwo)).awaitSingleOrNull()
        }}
        val providerOne = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        val providerTwo = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProvidersRetryable(txSession,
            listOf(providerOne, providerTwo)).awaitSingleOrNull() }}
        val resourceTypeOne = resourceTypeModel(providerOne.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeTwo = resourceTypeModel(providerOne.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeThree = resourceTypeModel(providerTwo.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeFour = resourceTypeModel(providerTwo.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypesRetryable(txSession,
            listOf(resourceTypeOne, resourceTypeTwo, resourceTypeThree, resourceTypeFour)).awaitSingleOrNull() }}
        val segmentationOne = resourceSegmentationModel(providerOne.id, "test")
        val segmentationTwo = resourceSegmentationModel(providerTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentationsDao.upsertResourceSegmentationsRetryable(txSession,
            listOf(segmentationOne, segmentationTwo)).awaitSingleOrNull() }}
        val segmentOne = resourceSegmentModel(segmentationOne.id, "test")
        val segmentTwo = resourceSegmentModel(segmentationOne.id, "test")
        val segmentThree = resourceSegmentModel(segmentationTwo.id, "test")
        val segmentFour = resourceSegmentModel(segmentationTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession,
            listOf(segmentOne, segmentTwo, segmentThree, segmentFour)).awaitSingleOrNull() }}
        val resourceOne = resourceModel(providerOne.id, "test", resourceTypeOne.id, mapOf(segmentationOne.id to segmentOne.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceTwo = resourceModel(providerOne.id, "test", resourceTypeOne.id, mapOf(segmentationOne.id to segmentTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceThree = resourceModel(providerOne.id, "test", resourceTypeTwo.id, mapOf(segmentationOne.id to segmentOne.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceFour = resourceModel(providerOne.id, "test", resourceTypeTwo.id, mapOf(segmentationOne.id to segmentTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceFive = resourceModel(providerTwo.id, "test", resourceTypeThree.id, mapOf(segmentationTwo.id to segmentThree.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceSix = resourceModel(providerTwo.id, "test", resourceTypeThree.id, mapOf(segmentationTwo.id to segmentFour.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceSeven = resourceModel(providerTwo.id, "test", resourceTypeFour.id, mapOf(segmentationTwo.id to segmentThree.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceEight = resourceModel(providerTwo.id, "test", resourceTypeFour.id, mapOf(segmentationTwo.id to segmentFour.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourcesRetryable(txSession,
            listOf(resourceOne, resourceTwo, resourceThree, resourceFour, resourceFive, resourceSix, resourceSeven,
                resourceEight)).awaitSingleOrNull() }}
        val accountRootProviderOne = accountModel(providerOne.id, null, folderRoot.id, "test")
        val accountChildOneProviderOne = accountModel(providerOne.id, null, folderChildOne.id, "test")
        val accountChildTwoProviderOne = accountModel(providerOne.id, null, folderChildTwo.id, "test")
        val accountGrandchildOneProviderOne = accountModel(providerOne.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwoProviderOne = accountModel(providerOne.id, null, folderGrandchildTwo.id, "test")
        val accountRootProviderTwo = accountModel(providerTwo.id, null, folderRoot.id, "test")
        val accountChildOneProviderTwo = accountModel(providerTwo.id, null, folderChildOne.id, "test")
        val accountChildTwoProviderTwo = accountModel(providerTwo.id, null, folderChildTwo.id, "test")
        val accountGrandchildOneProviderTwo = accountModel(providerTwo.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwoProviderTwo = accountModel(providerTwo.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRootProviderOne, accountChildOneProviderOne, accountChildTwoProviderOne,
                accountGrandchildOneProviderOne, accountGrandchildTwoProviderOne, accountRootProviderTwo,
                accountChildOneProviderTwo, accountChildTwoProviderTwo, accountGrandchildOneProviderTwo,
                accountGrandchildTwoProviderTwo)).awaitSingleOrNull()
        }}
        val quotas = mutableListOf<QuotaModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            quotas.add(quotaModel(providerOne.id, it.id, folderRoot.id, 10L, 5L))
            quotas.add(quotaModel(providerOne.id, it.id, folderChildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerOne.id, it.id, folderChildTwo.id, 60L, 30L))
            quotas.add(quotaModel(providerOne.id, it.id, folderGrandchildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerOne.id, it.id, folderGrandchildTwo.id, 60L, 30L))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            quotas.add(quotaModel(providerTwo.id, it.id, folderRoot.id, 10L, 5L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderChildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderChildTwo.id, 60L, 30L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderGrandchildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderGrandchildTwo.id, 60L, 30L))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.upsertAllRetryable(txSession, quotas).awaitSingleOrNull()
        }}
        val provisions = mutableListOf<AccountsQuotasModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderRoot.id, accountRootProviderOne.id,
                5L, 2L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderChildOne.id, accountChildOneProviderOne.id,
                15L, 6L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderChildTwo.id, accountChildTwoProviderOne.id,
                30L, 12L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderGrandchildOne.id,
                accountGrandchildOneProviderOne.id, 15L, 6L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderGrandchildTwo.id,
                accountGrandchildTwoProviderOne.id, 30L, 12L))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderRoot.id, accountRootProviderTwo.id,
                5L, 2L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderChildOne.id, accountChildOneProviderTwo.id,
                15L, 6L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderChildTwo.id, accountChildTwoProviderTwo.id,
                30L, 12L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderGrandchildOne.id,
                accountGrandchildOneProviderTwo.id, 15L, 6L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderGrandchildTwo.id,
                accountGrandchildTwoProviderTwo.id, 30L, 12L))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.upsertAllRetryable(txSession, provisions).awaitSingleOrNull()
        }}
        val now = Instant.now()
        val usages = mutableListOf<ServiceUsageModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            usages.add(serviceUsageModel(root.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L)))
            usages.add(serviceUsageModel(childOne.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(childTwo.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(grandchildOne.id, providerOne.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
            usages.add(serviceUsageModel(grandchildTwo.id, providerOne.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            usages.add(serviceUsageModel(root.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L)))
            usages.add(serviceUsageModel(childOne.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(childTwo.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(grandchildOne.id, providerTwo.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
            usages.add(serviceUsageModel(grandchildTwo.id, providerTwo.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, usages)
        }}
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", providerOne.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", providerTwo.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        val resultRoot = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(root.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultRoot.aggregates.size)
        Assertions.assertEquals(root.id, resultRoot.aggregates[0].serviceId)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[1].resourceTypes.size)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[1].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[1].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultRoot.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultRoot.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
        Assertions.assertTrue(resultRoot.aggregates[0].providers[1].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultRoot.aggregates[0].providers[1].resourceTypes[1].resources.all { it.usage != null })
        val resultChildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childOne.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildOne.aggregates.size)
        Assertions.assertEquals(childOne.id, resultChildOne.aggregates[0].serviceId)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[1].resourceTypes.size)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[1].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[1].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultChildOne.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
        Assertions.assertTrue(resultChildOne.aggregates[0].providers[1].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultChildOne.aggregates[0].providers[1].resourceTypes[1].resources.all { it.usage != null })
        val resultChildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childTwo.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildTwo.aggregates.size)
        Assertions.assertEquals(childTwo.id, resultChildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[1].resourceTypes.size)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[1].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[1].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultChildTwo.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
        Assertions.assertTrue(resultChildTwo.aggregates[0].providers[1].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultChildTwo.aggregates[0].providers[1].resourceTypes[1].resources.all { it.usage != null })
        val resultGrandchildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildOne.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildOne.aggregates.size)
        Assertions.assertEquals(grandchildOne.id, resultGrandchildOne.aggregates[0].serviceId)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[1].resourceTypes.size)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[1].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[1].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultGrandchildOne.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
        Assertions.assertTrue(resultGrandchildOne.aggregates[0].providers[1].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultGrandchildOne.aggregates[0].providers[1].resourceTypes[1].resources.all { it.usage != null })
        val resultGrandchildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildTwo.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultGrandchildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[1].resourceTypes.size)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[1].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[1].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
        Assertions.assertTrue(resultGrandchildTwo.aggregates[0].providers[1].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultGrandchildTwo.aggregates[0].providers[1].resourceTypes[1].resources.all { it.usage != null })
    }

    @Test
    fun testServiceTotalProviderFilter(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne, childTwo, grandchildOne, grandchildTwo))
                .awaitSingleOrNull()
        }}
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne, folderChildTwo,
                folderGrandchildOne, folderGrandchildTwo)).awaitSingleOrNull()
        }}
        val providerOne = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        val providerTwo = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProvidersRetryable(txSession,
            listOf(providerOne, providerTwo)).awaitSingleOrNull() }}
        val resourceTypeOne = resourceTypeModel(providerOne.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeTwo = resourceTypeModel(providerOne.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeThree = resourceTypeModel(providerTwo.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeFour = resourceTypeModel(providerTwo.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypesRetryable(txSession,
            listOf(resourceTypeOne, resourceTypeTwo, resourceTypeThree, resourceTypeFour)).awaitSingleOrNull() }}
        val segmentationOne = resourceSegmentationModel(providerOne.id, "test")
        val segmentationTwo = resourceSegmentationModel(providerTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentationsDao.upsertResourceSegmentationsRetryable(txSession,
            listOf(segmentationOne, segmentationTwo)).awaitSingleOrNull() }}
        val segmentOne = resourceSegmentModel(segmentationOne.id, "test")
        val segmentTwo = resourceSegmentModel(segmentationOne.id, "test")
        val segmentThree = resourceSegmentModel(segmentationTwo.id, "test")
        val segmentFour = resourceSegmentModel(segmentationTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession,
            listOf(segmentOne, segmentTwo, segmentThree, segmentFour)).awaitSingleOrNull() }}
        val resourceOne = resourceModel(providerOne.id, "test", resourceTypeOne.id, mapOf(segmentationOne.id to segmentOne.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceTwo = resourceModel(providerOne.id, "test", resourceTypeOne.id, mapOf(segmentationOne.id to segmentTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceThree = resourceModel(providerOne.id, "test", resourceTypeTwo.id, mapOf(segmentationOne.id to segmentOne.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceFour = resourceModel(providerOne.id, "test", resourceTypeTwo.id, mapOf(segmentationOne.id to segmentTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceFive = resourceModel(providerTwo.id, "test", resourceTypeThree.id, mapOf(segmentationTwo.id to segmentThree.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceSix = resourceModel(providerTwo.id, "test", resourceTypeThree.id, mapOf(segmentationTwo.id to segmentFour.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceSeven = resourceModel(providerTwo.id, "test", resourceTypeFour.id, mapOf(segmentationTwo.id to segmentThree.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceEight = resourceModel(providerTwo.id, "test", resourceTypeFour.id, mapOf(segmentationTwo.id to segmentFour.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourcesRetryable(txSession,
            listOf(resourceOne, resourceTwo, resourceThree, resourceFour, resourceFive, resourceSix, resourceSeven,
                resourceEight)).awaitSingleOrNull() }}
        val accountRootProviderOne = accountModel(providerOne.id, null, folderRoot.id, "test")
        val accountChildOneProviderOne = accountModel(providerOne.id, null, folderChildOne.id, "test")
        val accountChildTwoProviderOne = accountModel(providerOne.id, null, folderChildTwo.id, "test")
        val accountGrandchildOneProviderOne = accountModel(providerOne.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwoProviderOne = accountModel(providerOne.id, null, folderGrandchildTwo.id, "test")
        val accountRootProviderTwo = accountModel(providerTwo.id, null, folderRoot.id, "test")
        val accountChildOneProviderTwo = accountModel(providerTwo.id, null, folderChildOne.id, "test")
        val accountChildTwoProviderTwo = accountModel(providerTwo.id, null, folderChildTwo.id, "test")
        val accountGrandchildOneProviderTwo = accountModel(providerTwo.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwoProviderTwo = accountModel(providerTwo.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRootProviderOne, accountChildOneProviderOne, accountChildTwoProviderOne,
                accountGrandchildOneProviderOne, accountGrandchildTwoProviderOne, accountRootProviderTwo,
                accountChildOneProviderTwo, accountChildTwoProviderTwo, accountGrandchildOneProviderTwo,
                accountGrandchildTwoProviderTwo)).awaitSingleOrNull()
        }}
        val quotas = mutableListOf<QuotaModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            quotas.add(quotaModel(providerOne.id, it.id, folderRoot.id, 10L, 5L))
            quotas.add(quotaModel(providerOne.id, it.id, folderChildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerOne.id, it.id, folderChildTwo.id, 60L, 30L))
            quotas.add(quotaModel(providerOne.id, it.id, folderGrandchildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerOne.id, it.id, folderGrandchildTwo.id, 60L, 30L))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            quotas.add(quotaModel(providerTwo.id, it.id, folderRoot.id, 10L, 5L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderChildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderChildTwo.id, 60L, 30L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderGrandchildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderGrandchildTwo.id, 60L, 30L))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.upsertAllRetryable(txSession, quotas).awaitSingleOrNull()
        }}
        val provisions = mutableListOf<AccountsQuotasModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderRoot.id, accountRootProviderOne.id,
                5L, 2L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderChildOne.id, accountChildOneProviderOne.id,
                15L, 6L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderChildTwo.id, accountChildTwoProviderOne.id,
                30L, 12L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderGrandchildOne.id,
                accountGrandchildOneProviderOne.id, 15L, 6L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderGrandchildTwo.id,
                accountGrandchildTwoProviderOne.id, 30L, 12L))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderRoot.id, accountRootProviderTwo.id,
                5L, 2L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderChildOne.id, accountChildOneProviderTwo.id,
                15L, 6L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderChildTwo.id, accountChildTwoProviderTwo.id,
                30L, 12L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderGrandchildOne.id,
                accountGrandchildOneProviderTwo.id, 15L, 6L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderGrandchildTwo.id,
                accountGrandchildTwoProviderTwo.id, 30L, 12L))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.upsertAllRetryable(txSession, provisions).awaitSingleOrNull()
        }}
        val now = Instant.now()
        val usages = mutableListOf<ServiceUsageModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            usages.add(serviceUsageModel(root.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L)))
            usages.add(serviceUsageModel(childOne.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(childTwo.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(grandchildOne.id, providerOne.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
            usages.add(serviceUsageModel(grandchildTwo.id, providerOne.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            usages.add(serviceUsageModel(root.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L)))
            usages.add(serviceUsageModel(childOne.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(childTwo.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(grandchildOne.id, providerTwo.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
            usages.add(serviceUsageModel(grandchildTwo.id, providerTwo.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, usages)
        }}
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", providerOne.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", providerTwo.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        val resultRoot = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(root.id, FindServiceTotalsRequestFilterDto(providerOne.id, null)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultRoot.aggregates.size)
        Assertions.assertEquals(root.id, resultRoot.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultRoot.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultRoot.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
        val resultChildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childOne.id, FindServiceTotalsRequestFilterDto(providerOne.id, null)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildOne.aggregates.size)
        Assertions.assertEquals(childOne.id, resultChildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultChildOne.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
        val resultChildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childTwo.id, FindServiceTotalsRequestFilterDto(providerOne.id, null)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildTwo.aggregates.size)
        Assertions.assertEquals(childTwo.id, resultChildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultChildTwo.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
        val resultGrandchildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildOne.id, FindServiceTotalsRequestFilterDto(providerOne.id, null)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildOne.aggregates.size)
        Assertions.assertEquals(grandchildOne.id, resultGrandchildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultGrandchildOne.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
        val resultGrandchildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildTwo.id, FindServiceTotalsRequestFilterDto(providerOne.id, null)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultGrandchildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
    }

    @Test
    fun testServiceTotalEmptyProviderFilter(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne, childTwo, grandchildOne, grandchildTwo))
                .awaitSingleOrNull()
        }}
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne, folderChildTwo,
                folderGrandchildOne, folderGrandchildTwo)).awaitSingleOrNull()
        }}
        val providerOne = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        val providerTwo = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        val providerThree = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProvidersRetryable(txSession,
            listOf(providerOne, providerTwo, providerThree)).awaitSingleOrNull() }}
        val resourceTypeOne = resourceTypeModel(providerOne.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeTwo = resourceTypeModel(providerOne.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeThree = resourceTypeModel(providerTwo.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeFour = resourceTypeModel(providerTwo.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypesRetryable(txSession,
            listOf(resourceTypeOne, resourceTypeTwo, resourceTypeThree, resourceTypeFour)).awaitSingleOrNull() }}
        val segmentationOne = resourceSegmentationModel(providerOne.id, "test")
        val segmentationTwo = resourceSegmentationModel(providerTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentationsDao.upsertResourceSegmentationsRetryable(txSession,
            listOf(segmentationOne, segmentationTwo)).awaitSingleOrNull() }}
        val segmentOne = resourceSegmentModel(segmentationOne.id, "test")
        val segmentTwo = resourceSegmentModel(segmentationOne.id, "test")
        val segmentThree = resourceSegmentModel(segmentationTwo.id, "test")
        val segmentFour = resourceSegmentModel(segmentationTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession,
            listOf(segmentOne, segmentTwo, segmentThree, segmentFour)).awaitSingleOrNull() }}
        val resourceOne = resourceModel(providerOne.id, "test", resourceTypeOne.id, mapOf(segmentationOne.id to segmentOne.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceTwo = resourceModel(providerOne.id, "test", resourceTypeOne.id, mapOf(segmentationOne.id to segmentTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceThree = resourceModel(providerOne.id, "test", resourceTypeTwo.id, mapOf(segmentationOne.id to segmentOne.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceFour = resourceModel(providerOne.id, "test", resourceTypeTwo.id, mapOf(segmentationOne.id to segmentTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceFive = resourceModel(providerTwo.id, "test", resourceTypeThree.id, mapOf(segmentationTwo.id to segmentThree.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceSix = resourceModel(providerTwo.id, "test", resourceTypeThree.id, mapOf(segmentationTwo.id to segmentFour.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceSeven = resourceModel(providerTwo.id, "test", resourceTypeFour.id, mapOf(segmentationTwo.id to segmentThree.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceEight = resourceModel(providerTwo.id, "test", resourceTypeFour.id, mapOf(segmentationTwo.id to segmentFour.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourcesRetryable(txSession,
            listOf(resourceOne, resourceTwo, resourceThree, resourceFour, resourceFive, resourceSix, resourceSeven,
                resourceEight)).awaitSingleOrNull() }}
        val accountRootProviderOne = accountModel(providerOne.id, null, folderRoot.id, "test")
        val accountChildOneProviderOne = accountModel(providerOne.id, null, folderChildOne.id, "test")
        val accountChildTwoProviderOne = accountModel(providerOne.id, null, folderChildTwo.id, "test")
        val accountGrandchildOneProviderOne = accountModel(providerOne.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwoProviderOne = accountModel(providerOne.id, null, folderGrandchildTwo.id, "test")
        val accountRootProviderTwo = accountModel(providerTwo.id, null, folderRoot.id, "test")
        val accountChildOneProviderTwo = accountModel(providerTwo.id, null, folderChildOne.id, "test")
        val accountChildTwoProviderTwo = accountModel(providerTwo.id, null, folderChildTwo.id, "test")
        val accountGrandchildOneProviderTwo = accountModel(providerTwo.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwoProviderTwo = accountModel(providerTwo.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRootProviderOne, accountChildOneProviderOne, accountChildTwoProviderOne,
                accountGrandchildOneProviderOne, accountGrandchildTwoProviderOne, accountRootProviderTwo,
                accountChildOneProviderTwo, accountChildTwoProviderTwo, accountGrandchildOneProviderTwo,
                accountGrandchildTwoProviderTwo)).awaitSingleOrNull()
        }}
        val quotas = mutableListOf<QuotaModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            quotas.add(quotaModel(providerOne.id, it.id, folderRoot.id, 10L, 5L))
            quotas.add(quotaModel(providerOne.id, it.id, folderChildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerOne.id, it.id, folderChildTwo.id, 60L, 30L))
            quotas.add(quotaModel(providerOne.id, it.id, folderGrandchildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerOne.id, it.id, folderGrandchildTwo.id, 60L, 30L))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            quotas.add(quotaModel(providerTwo.id, it.id, folderRoot.id, 10L, 5L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderChildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderChildTwo.id, 60L, 30L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderGrandchildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderGrandchildTwo.id, 60L, 30L))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.upsertAllRetryable(txSession, quotas).awaitSingleOrNull()
        }}
        val provisions = mutableListOf<AccountsQuotasModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderRoot.id, accountRootProviderOne.id,
                5L, 2L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderChildOne.id, accountChildOneProviderOne.id,
                15L, 6L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderChildTwo.id, accountChildTwoProviderOne.id,
                30L, 12L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderGrandchildOne.id,
                accountGrandchildOneProviderOne.id, 15L, 6L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderGrandchildTwo.id,
                accountGrandchildTwoProviderOne.id, 30L, 12L))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderRoot.id, accountRootProviderTwo.id,
                5L, 2L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderChildOne.id, accountChildOneProviderTwo.id,
                15L, 6L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderChildTwo.id, accountChildTwoProviderTwo.id,
                30L, 12L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderGrandchildOne.id,
                accountGrandchildOneProviderTwo.id, 15L, 6L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderGrandchildTwo.id,
                accountGrandchildTwoProviderTwo.id, 30L, 12L))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.upsertAllRetryable(txSession, provisions).awaitSingleOrNull()
        }}
        val now = Instant.now()
        val usages = mutableListOf<ServiceUsageModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            usages.add(serviceUsageModel(root.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L)))
            usages.add(serviceUsageModel(childOne.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(childTwo.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(grandchildOne.id, providerOne.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
            usages.add(serviceUsageModel(grandchildTwo.id, providerOne.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            usages.add(serviceUsageModel(root.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L)))
            usages.add(serviceUsageModel(childOne.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(childTwo.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(grandchildOne.id, providerTwo.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
            usages.add(serviceUsageModel(grandchildTwo.id, providerTwo.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, usages)
        }}
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", providerOne.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", providerTwo.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        val resultRoot = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(root.id, FindServiceTotalsRequestFilterDto(providerThree.id, null)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultRoot.aggregates.size)
        Assertions.assertEquals(root.id, resultRoot.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers.size)
        Assertions.assertEquals(0, resultRoot.aggregates[0].providers[0].resourceTypes.size)
        val resultChildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childOne.id, FindServiceTotalsRequestFilterDto(providerThree.id, null)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildOne.aggregates.size)
        Assertions.assertEquals(childOne.id, resultChildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers.size)
        Assertions.assertEquals(0, resultChildOne.aggregates[0].providers[0].resourceTypes.size)
        val resultChildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childTwo.id, FindServiceTotalsRequestFilterDto(providerThree.id, null)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildTwo.aggregates.size)
        Assertions.assertEquals(childTwo.id, resultChildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(0, resultChildTwo.aggregates[0].providers[0].resourceTypes.size)
        val resultGrandchildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildOne.id, FindServiceTotalsRequestFilterDto(providerThree.id, null)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildOne.aggregates.size)
        Assertions.assertEquals(grandchildOne.id, resultGrandchildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers.size)
        Assertions.assertEquals(0, resultGrandchildOne.aggregates[0].providers[0].resourceTypes.size)
        val resultGrandchildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildTwo.id, FindServiceTotalsRequestFilterDto(providerThree.id, null)))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultGrandchildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(0, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes.size)
    }

    @Test
    fun testServiceTotalProviderSegmentsFilter(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne, childTwo, grandchildOne, grandchildTwo))
                .awaitSingleOrNull()
        }}
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne, folderChildTwo,
                folderGrandchildOne, folderGrandchildTwo)).awaitSingleOrNull()
        }}
        val providerOne = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        val providerTwo = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProvidersRetryable(txSession,
            listOf(providerOne, providerTwo)).awaitSingleOrNull() }}
        val resourceTypeOne = resourceTypeModel(providerOne.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeTwo = resourceTypeModel(providerOne.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeThree = resourceTypeModel(providerTwo.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeFour = resourceTypeModel(providerTwo.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypesRetryable(txSession,
            listOf(resourceTypeOne, resourceTypeTwo, resourceTypeThree, resourceTypeFour)).awaitSingleOrNull() }}
        val segmentationOne = resourceSegmentationModel(providerOne.id, "test")
        val segmentationTwo = resourceSegmentationModel(providerTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentationsDao.upsertResourceSegmentationsRetryable(txSession,
            listOf(segmentationOne, segmentationTwo)).awaitSingleOrNull() }}
        val segmentOne = resourceSegmentModel(segmentationOne.id, "test")
        val segmentTwo = resourceSegmentModel(segmentationOne.id, "test")
        val segmentThree = resourceSegmentModel(segmentationTwo.id, "test")
        val segmentFour = resourceSegmentModel(segmentationTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession,
            listOf(segmentOne, segmentTwo, segmentThree, segmentFour)).awaitSingleOrNull() }}
        val resourceOne = resourceModel(providerOne.id, "test", resourceTypeOne.id, mapOf(segmentationOne.id to segmentOne.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceTwo = resourceModel(providerOne.id, "test", resourceTypeOne.id, mapOf(segmentationOne.id to segmentTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceThree = resourceModel(providerOne.id, "test", resourceTypeTwo.id, mapOf(segmentationOne.id to segmentOne.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceFour = resourceModel(providerOne.id, "test", resourceTypeTwo.id, mapOf(segmentationOne.id to segmentTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceFive = resourceModel(providerTwo.id, "test", resourceTypeThree.id, mapOf(segmentationTwo.id to segmentThree.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceSix = resourceModel(providerTwo.id, "test", resourceTypeThree.id, mapOf(segmentationTwo.id to segmentFour.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceSeven = resourceModel(providerTwo.id, "test", resourceTypeFour.id, mapOf(segmentationTwo.id to segmentThree.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceEight = resourceModel(providerTwo.id, "test", resourceTypeFour.id, mapOf(segmentationTwo.id to segmentFour.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourcesRetryable(txSession,
            listOf(resourceOne, resourceTwo, resourceThree, resourceFour, resourceFive, resourceSix, resourceSeven,
                resourceEight)).awaitSingleOrNull() }}
        val accountRootProviderOne = accountModel(providerOne.id, null, folderRoot.id, "test")
        val accountChildOneProviderOne = accountModel(providerOne.id, null, folderChildOne.id, "test")
        val accountChildTwoProviderOne = accountModel(providerOne.id, null, folderChildTwo.id, "test")
        val accountGrandchildOneProviderOne = accountModel(providerOne.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwoProviderOne = accountModel(providerOne.id, null, folderGrandchildTwo.id, "test")
        val accountRootProviderTwo = accountModel(providerTwo.id, null, folderRoot.id, "test")
        val accountChildOneProviderTwo = accountModel(providerTwo.id, null, folderChildOne.id, "test")
        val accountChildTwoProviderTwo = accountModel(providerTwo.id, null, folderChildTwo.id, "test")
        val accountGrandchildOneProviderTwo = accountModel(providerTwo.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwoProviderTwo = accountModel(providerTwo.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRootProviderOne, accountChildOneProviderOne, accountChildTwoProviderOne,
                accountGrandchildOneProviderOne, accountGrandchildTwoProviderOne, accountRootProviderTwo,
                accountChildOneProviderTwo, accountChildTwoProviderTwo, accountGrandchildOneProviderTwo,
                accountGrandchildTwoProviderTwo)).awaitSingleOrNull()
        }}
        val quotas = mutableListOf<QuotaModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            quotas.add(quotaModel(providerOne.id, it.id, folderRoot.id, 10L, 5L))
            quotas.add(quotaModel(providerOne.id, it.id, folderChildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerOne.id, it.id, folderChildTwo.id, 60L, 30L))
            quotas.add(quotaModel(providerOne.id, it.id, folderGrandchildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerOne.id, it.id, folderGrandchildTwo.id, 60L, 30L))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            quotas.add(quotaModel(providerTwo.id, it.id, folderRoot.id, 10L, 5L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderChildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderChildTwo.id, 60L, 30L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderGrandchildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderGrandchildTwo.id, 60L, 30L))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.upsertAllRetryable(txSession, quotas).awaitSingleOrNull()
        }}
        val provisions = mutableListOf<AccountsQuotasModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderRoot.id, accountRootProviderOne.id,
                5L, 2L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderChildOne.id, accountChildOneProviderOne.id,
                15L, 6L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderChildTwo.id, accountChildTwoProviderOne.id,
                30L, 12L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderGrandchildOne.id,
                accountGrandchildOneProviderOne.id, 15L, 6L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderGrandchildTwo.id,
                accountGrandchildTwoProviderOne.id, 30L, 12L))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderRoot.id, accountRootProviderTwo.id,
                5L, 2L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderChildOne.id, accountChildOneProviderTwo.id,
                15L, 6L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderChildTwo.id, accountChildTwoProviderTwo.id,
                30L, 12L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderGrandchildOne.id,
                accountGrandchildOneProviderTwo.id, 15L, 6L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderGrandchildTwo.id,
                accountGrandchildTwoProviderTwo.id, 30L, 12L))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.upsertAllRetryable(txSession, provisions).awaitSingleOrNull()
        }}
        val now = Instant.now()
        val usages = mutableListOf<ServiceUsageModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            usages.add(serviceUsageModel(root.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L)))
            usages.add(serviceUsageModel(childOne.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(childTwo.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(grandchildOne.id, providerOne.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
            usages.add(serviceUsageModel(grandchildTwo.id, providerOne.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            usages.add(serviceUsageModel(root.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L)))
            usages.add(serviceUsageModel(childOne.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(childTwo.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(grandchildOne.id, providerTwo.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
            usages.add(serviceUsageModel(grandchildTwo.id, providerTwo.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, usages)
        }}
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", providerOne.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", providerTwo.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        val resultRoot = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(root.id, FindServiceTotalsRequestFilterDto(providerOne.id,
                listOf(FindServiceTotalsRequestFilterSegmentDto(segmentationOne.id, segmentOne.id)))))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultRoot.aggregates.size)
        Assertions.assertEquals(root.id, resultRoot.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultRoot.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultRoot.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
        val resultChildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childOne.id, FindServiceTotalsRequestFilterDto(providerOne.id,
                listOf(FindServiceTotalsRequestFilterSegmentDto(segmentationOne.id, segmentOne.id)))))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildOne.aggregates.size)
        Assertions.assertEquals(childOne.id, resultChildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultChildOne.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
        val resultChildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childTwo.id, FindServiceTotalsRequestFilterDto(providerOne.id,
                listOf(FindServiceTotalsRequestFilterSegmentDto(segmentationOne.id, segmentOne.id)))))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildTwo.aggregates.size)
        Assertions.assertEquals(childTwo.id, resultChildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultChildTwo.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
        val resultGrandchildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildOne.id, FindServiceTotalsRequestFilterDto(providerOne.id,
                listOf(FindServiceTotalsRequestFilterSegmentDto(segmentationOne.id, segmentOne.id)))))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildOne.aggregates.size)
        Assertions.assertEquals(grandchildOne.id, resultGrandchildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultGrandchildOne.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
        val resultGrandchildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildTwo.id, FindServiceTotalsRequestFilterDto(providerOne.id,
                listOf(FindServiceTotalsRequestFilterSegmentDto(segmentationOne.id, segmentOne.id)))))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultGrandchildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[1].resources.size)
        Assertions.assertTrue(resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources.all { it.usage != null })
        Assertions.assertTrue(resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[1].resources.all { it.usage != null })
    }

    @Test
    fun testServiceTotalEmptyProviderSegmentsFilter(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne, childTwo, grandchildOne, grandchildTwo))
                .awaitSingleOrNull()
        }}
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne, folderChildTwo,
                folderGrandchildOne, folderGrandchildTwo)).awaitSingleOrNull()
        }}
        val providerOne = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        val providerTwo = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProvidersRetryable(txSession,
            listOf(providerOne, providerTwo)).awaitSingleOrNull() }}
        val resourceTypeOne = resourceTypeModel(providerOne.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeTwo = resourceTypeModel(providerOne.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeThree = resourceTypeModel(providerTwo.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeFour = resourceTypeModel(providerTwo.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypesRetryable(txSession,
            listOf(resourceTypeOne, resourceTypeTwo, resourceTypeThree, resourceTypeFour)).awaitSingleOrNull() }}
        val segmentationOne = resourceSegmentationModel(providerOne.id, "test")
        val segmentationTwo = resourceSegmentationModel(providerTwo.id, "test")
        val segmentationThree = resourceSegmentationModel(providerOne.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentationsDao.upsertResourceSegmentationsRetryable(txSession,
            listOf(segmentationOne, segmentationTwo, segmentationThree)).awaitSingleOrNull() }}
        val segmentOne = resourceSegmentModel(segmentationOne.id, "test")
        val segmentTwo = resourceSegmentModel(segmentationOne.id, "test")
        val segmentThree = resourceSegmentModel(segmentationTwo.id, "test")
        val segmentFour = resourceSegmentModel(segmentationTwo.id, "test")
        val segmentFive = resourceSegmentModel(segmentationThree.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession,
            listOf(segmentOne, segmentTwo, segmentThree, segmentFour, segmentFive)).awaitSingleOrNull() }}
        val resourceOne = resourceModel(providerOne.id, "test", resourceTypeOne.id, mapOf(segmentationOne.id to segmentOne.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceTwo = resourceModel(providerOne.id, "test", resourceTypeOne.id, mapOf(segmentationOne.id to segmentTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceThree = resourceModel(providerOne.id, "test", resourceTypeTwo.id, mapOf(segmentationOne.id to segmentOne.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceFour = resourceModel(providerOne.id, "test", resourceTypeTwo.id, mapOf(segmentationOne.id to segmentTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceFive = resourceModel(providerTwo.id, "test", resourceTypeThree.id, mapOf(segmentationTwo.id to segmentThree.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceSix = resourceModel(providerTwo.id, "test", resourceTypeThree.id, mapOf(segmentationTwo.id to segmentFour.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceSeven = resourceModel(providerTwo.id, "test", resourceTypeFour.id, mapOf(segmentationTwo.id to segmentThree.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceEight = resourceModel(providerTwo.id, "test", resourceTypeFour.id, mapOf(segmentationTwo.id to segmentFour.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourcesRetryable(txSession,
            listOf(resourceOne, resourceTwo, resourceThree, resourceFour, resourceFive, resourceSix, resourceSeven,
                resourceEight)).awaitSingleOrNull() }}
        val accountRootProviderOne = accountModel(providerOne.id, null, folderRoot.id, "test")
        val accountChildOneProviderOne = accountModel(providerOne.id, null, folderChildOne.id, "test")
        val accountChildTwoProviderOne = accountModel(providerOne.id, null, folderChildTwo.id, "test")
        val accountGrandchildOneProviderOne = accountModel(providerOne.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwoProviderOne = accountModel(providerOne.id, null, folderGrandchildTwo.id, "test")
        val accountRootProviderTwo = accountModel(providerTwo.id, null, folderRoot.id, "test")
        val accountChildOneProviderTwo = accountModel(providerTwo.id, null, folderChildOne.id, "test")
        val accountChildTwoProviderTwo = accountModel(providerTwo.id, null, folderChildTwo.id, "test")
        val accountGrandchildOneProviderTwo = accountModel(providerTwo.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwoProviderTwo = accountModel(providerTwo.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRootProviderOne, accountChildOneProviderOne, accountChildTwoProviderOne,
                accountGrandchildOneProviderOne, accountGrandchildTwoProviderOne, accountRootProviderTwo,
                accountChildOneProviderTwo, accountChildTwoProviderTwo, accountGrandchildOneProviderTwo,
                accountGrandchildTwoProviderTwo)).awaitSingleOrNull()
        }}
        val quotas = mutableListOf<QuotaModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            quotas.add(quotaModel(providerOne.id, it.id, folderRoot.id, 10L, 5L))
            quotas.add(quotaModel(providerOne.id, it.id, folderChildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerOne.id, it.id, folderChildTwo.id, 60L, 30L))
            quotas.add(quotaModel(providerOne.id, it.id, folderGrandchildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerOne.id, it.id, folderGrandchildTwo.id, 60L, 30L))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            quotas.add(quotaModel(providerTwo.id, it.id, folderRoot.id, 10L, 5L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderChildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderChildTwo.id, 60L, 30L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderGrandchildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderGrandchildTwo.id, 60L, 30L))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.upsertAllRetryable(txSession, quotas).awaitSingleOrNull()
        }}
        val provisions = mutableListOf<AccountsQuotasModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderRoot.id, accountRootProviderOne.id,
                5L, 2L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderChildOne.id, accountChildOneProviderOne.id,
                15L, 6L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderChildTwo.id, accountChildTwoProviderOne.id,
                30L, 12L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderGrandchildOne.id,
                accountGrandchildOneProviderOne.id, 15L, 6L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderGrandchildTwo.id,
                accountGrandchildTwoProviderOne.id, 30L, 12L))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderRoot.id, accountRootProviderTwo.id,
                5L, 2L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderChildOne.id, accountChildOneProviderTwo.id,
                15L, 6L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderChildTwo.id, accountChildTwoProviderTwo.id,
                30L, 12L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderGrandchildOne.id,
                accountGrandchildOneProviderTwo.id, 15L, 6L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderGrandchildTwo.id,
                accountGrandchildTwoProviderTwo.id, 30L, 12L))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.upsertAllRetryable(txSession, provisions).awaitSingleOrNull()
        }}
        val now = Instant.now()
        val usages = mutableListOf<ServiceUsageModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            usages.add(serviceUsageModel(root.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L)))
            usages.add(serviceUsageModel(childOne.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(childTwo.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(grandchildOne.id, providerOne.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
            usages.add(serviceUsageModel(grandchildTwo.id, providerOne.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            usages.add(serviceUsageModel(root.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L)))
            usages.add(serviceUsageModel(childOne.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(childTwo.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(grandchildOne.id, providerTwo.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
            usages.add(serviceUsageModel(grandchildTwo.id, providerTwo.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, usages)
        }}
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", providerOne.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", providerTwo.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        val resultRoot = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(root.id, FindServiceTotalsRequestFilterDto(providerOne.id,
                listOf(FindServiceTotalsRequestFilterSegmentDto(segmentationThree.id, segmentFive.id)))))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultRoot.aggregates.size)
        Assertions.assertEquals(root.id, resultRoot.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers.size)
        Assertions.assertEquals(0, resultRoot.aggregates[0].providers[0].resourceTypes.size)
        val resultChildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childOne.id, FindServiceTotalsRequestFilterDto(providerOne.id,
                listOf(FindServiceTotalsRequestFilterSegmentDto(segmentationThree.id, segmentFive.id)))))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildOne.aggregates.size)
        Assertions.assertEquals(childOne.id, resultChildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers.size)
        Assertions.assertEquals(0, resultChildOne.aggregates[0].providers[0].resourceTypes.size)
        val resultChildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childTwo.id, FindServiceTotalsRequestFilterDto(providerOne.id,
                listOf(FindServiceTotalsRequestFilterSegmentDto(segmentationThree.id, segmentFive.id)))))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildTwo.aggregates.size)
        Assertions.assertEquals(childTwo.id, resultChildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(0, resultChildTwo.aggregates[0].providers[0].resourceTypes.size)
        val resultGrandchildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildOne.id, FindServiceTotalsRequestFilterDto(providerOne.id,
                listOf(FindServiceTotalsRequestFilterSegmentDto(segmentationThree.id, segmentFive.id)))))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildOne.aggregates.size)
        Assertions.assertEquals(grandchildOne.id, resultGrandchildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers.size)
        Assertions.assertEquals(0, resultGrandchildOne.aggregates[0].providers[0].resourceTypes.size)
        val resultGrandchildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildTwo.id, FindServiceTotalsRequestFilterDto(providerOne.id,
                listOf(FindServiceTotalsRequestFilterSegmentDto(segmentationThree.id, segmentFive.id)))))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultGrandchildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(0, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes.size)
    }

    @Test
    fun testServiceTotalInvalidProviderSegmentsFilter(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne, childTwo, grandchildOne, grandchildTwo))
                .awaitSingleOrNull()
        }}
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne, folderChildTwo,
                folderGrandchildOne, folderGrandchildTwo)).awaitSingleOrNull()
        }}
        val providerOne = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        val providerTwo = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProvidersRetryable(txSession,
            listOf(providerOne, providerTwo)).awaitSingleOrNull() }}
        val resourceTypeOne = resourceTypeModel(providerOne.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeTwo = resourceTypeModel(providerOne.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeThree = resourceTypeModel(providerTwo.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeFour = resourceTypeModel(providerTwo.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypesRetryable(txSession,
            listOf(resourceTypeOne, resourceTypeTwo, resourceTypeThree, resourceTypeFour)).awaitSingleOrNull() }}
        val segmentationOne = resourceSegmentationModel(providerOne.id, "test")
        val segmentationTwo = resourceSegmentationModel(providerTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentationsDao.upsertResourceSegmentationsRetryable(txSession,
            listOf(segmentationOne, segmentationTwo)).awaitSingleOrNull() }}
        val segmentOne = resourceSegmentModel(segmentationOne.id, "test")
        val segmentTwo = resourceSegmentModel(segmentationOne.id, "test")
        val segmentThree = resourceSegmentModel(segmentationTwo.id, "test")
        val segmentFour = resourceSegmentModel(segmentationTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession,
            listOf(segmentOne, segmentTwo, segmentThree, segmentFour)).awaitSingleOrNull() }}
        val resourceOne = resourceModel(providerOne.id, "test", resourceTypeOne.id, mapOf(segmentationOne.id to segmentOne.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceTwo = resourceModel(providerOne.id, "test", resourceTypeOne.id, mapOf(segmentationOne.id to segmentTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceThree = resourceModel(providerOne.id, "test", resourceTypeTwo.id, mapOf(segmentationOne.id to segmentOne.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceFour = resourceModel(providerOne.id, "test", resourceTypeTwo.id, mapOf(segmentationOne.id to segmentTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceFive = resourceModel(providerTwo.id, "test", resourceTypeThree.id, mapOf(segmentationTwo.id to segmentThree.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceSix = resourceModel(providerTwo.id, "test", resourceTypeThree.id, mapOf(segmentationTwo.id to segmentFour.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceSeven = resourceModel(providerTwo.id, "test", resourceTypeFour.id, mapOf(segmentationTwo.id to segmentThree.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceEight = resourceModel(providerTwo.id, "test", resourceTypeFour.id, mapOf(segmentationTwo.id to segmentFour.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourcesRetryable(txSession,
            listOf(resourceOne, resourceTwo, resourceThree, resourceFour, resourceFive, resourceSix, resourceSeven,
                resourceEight)).awaitSingleOrNull() }}
        val accountRootProviderOne = accountModel(providerOne.id, null, folderRoot.id, "test")
        val accountChildOneProviderOne = accountModel(providerOne.id, null, folderChildOne.id, "test")
        val accountChildTwoProviderOne = accountModel(providerOne.id, null, folderChildTwo.id, "test")
        val accountGrandchildOneProviderOne = accountModel(providerOne.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwoProviderOne = accountModel(providerOne.id, null, folderGrandchildTwo.id, "test")
        val accountRootProviderTwo = accountModel(providerTwo.id, null, folderRoot.id, "test")
        val accountChildOneProviderTwo = accountModel(providerTwo.id, null, folderChildOne.id, "test")
        val accountChildTwoProviderTwo = accountModel(providerTwo.id, null, folderChildTwo.id, "test")
        val accountGrandchildOneProviderTwo = accountModel(providerTwo.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwoProviderTwo = accountModel(providerTwo.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRootProviderOne, accountChildOneProviderOne, accountChildTwoProviderOne,
                accountGrandchildOneProviderOne, accountGrandchildTwoProviderOne, accountRootProviderTwo,
                accountChildOneProviderTwo, accountChildTwoProviderTwo, accountGrandchildOneProviderTwo,
                accountGrandchildTwoProviderTwo)).awaitSingleOrNull()
        }}
        val quotas = mutableListOf<QuotaModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            quotas.add(quotaModel(providerOne.id, it.id, folderRoot.id, 10L, 5L))
            quotas.add(quotaModel(providerOne.id, it.id, folderChildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerOne.id, it.id, folderChildTwo.id, 60L, 30L))
            quotas.add(quotaModel(providerOne.id, it.id, folderGrandchildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerOne.id, it.id, folderGrandchildTwo.id, 60L, 30L))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            quotas.add(quotaModel(providerTwo.id, it.id, folderRoot.id, 10L, 5L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderChildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderChildTwo.id, 60L, 30L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderGrandchildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderGrandchildTwo.id, 60L, 30L))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.upsertAllRetryable(txSession, quotas).awaitSingleOrNull()
        }}
        val provisions = mutableListOf<AccountsQuotasModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderRoot.id, accountRootProviderOne.id,
                5L, 2L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderChildOne.id, accountChildOneProviderOne.id,
                15L, 6L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderChildTwo.id, accountChildTwoProviderOne.id,
                30L, 12L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderGrandchildOne.id,
                accountGrandchildOneProviderOne.id, 15L, 6L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderGrandchildTwo.id,
                accountGrandchildTwoProviderOne.id, 30L, 12L))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderRoot.id, accountRootProviderTwo.id,
                5L, 2L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderChildOne.id, accountChildOneProviderTwo.id,
                15L, 6L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderChildTwo.id, accountChildTwoProviderTwo.id,
                30L, 12L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderGrandchildOne.id,
                accountGrandchildOneProviderTwo.id, 15L, 6L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderGrandchildTwo.id,
                accountGrandchildTwoProviderTwo.id, 30L, 12L))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.upsertAllRetryable(txSession, provisions).awaitSingleOrNull()
        }}
        val now = Instant.now()
        val usages = mutableListOf<ServiceUsageModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            usages.add(serviceUsageModel(root.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L)))
            usages.add(serviceUsageModel(childOne.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(childTwo.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(grandchildOne.id, providerOne.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
            usages.add(serviceUsageModel(grandchildTwo.id, providerOne.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
        }
        listOf(resourceFive, resourceSix, resourceSeven, resourceEight).forEach {
            usages.add(serviceUsageModel(root.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L)))
            usages.add(serviceUsageModel(childOne.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(childTwo.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(grandchildOne.id, providerTwo.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
            usages.add(serviceUsageModel(grandchildTwo.id, providerTwo.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, usages)
        }}
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", providerOne.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", providerTwo.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(root.id, FindServiceTotalsRequestFilterDto(UUID.randomUUID().toString(),
                listOf(FindServiceTotalsRequestFilterSegmentDto(UUID.randomUUID().toString(), UUID.randomUUID().toString())))))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
    }

    @Test
    fun testServiceTotalFlags(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne, childTwo, grandchildOne, grandchildTwo))
                .awaitSingleOrNull()
        }}
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne, folderChildTwo,
                folderGrandchildOne, folderGrandchildTwo)).awaitSingleOrNull()
        }}
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProviderRetryable(txSession, provider)
            .awaitSingleOrNull() }}
        val resourceTypeOne = resourceTypeModel(provider.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeTwo = resourceTypeModel(provider.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypesRetryable(txSession,
            listOf(resourceTypeOne, resourceTypeTwo)).awaitSingleOrNull() }}
        val resourceOne = resourceModel(provider.id, "test", resourceTypeOne.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceTwo = resourceModel(provider.id, "test", resourceTypeTwo.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceThree = resourceModel(provider.id, "test", resourceTypeTwo.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourcesRetryable(txSession, listOf(resourceOne,
            resourceTwo, resourceThree)).awaitSingleOrNull() }}
        val accountRoot = accountModel(provider.id, null, folderRoot.id, "test")
        val accountChildOne = accountModel(provider.id, null, folderChildOne.id, "test")
        val accountChildTwo = accountModel(provider.id, null, folderChildTwo.id, "test")
        val accountGrandchildOne = accountModel(provider.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwo = accountModel(provider.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRoot, accountChildOne, accountChildTwo,
                accountGrandchildOne, accountGrandchildTwo)).awaitSingleOrNull()
        }}
        val quotaOneRoot = quotaModel(provider.id, resourceOne.id, folderRoot.id, 10L, 5L)
        val quotaOneChildOne = quotaModel(provider.id, resourceOne.id, folderChildOne.id, 30L, 15L)
        val quotaOneChildTwo = quotaModel(provider.id, resourceOne.id, folderChildTwo.id, 60L, 30L)
        val quotaOneGrandchildOne = quotaModel(provider.id, resourceOne.id, folderGrandchildOne.id, 30L, 15L)
        val quotaOneGrandchildTwo = quotaModel(provider.id, resourceOne.id, folderGrandchildTwo.id, 60L, 30L)
        val quotaTwoRoot = quotaModel(provider.id, resourceTwo.id, folderRoot.id, 10L, 5L)
        val quotaTwoChildOne = quotaModel(provider.id, resourceTwo.id, folderChildOne.id, 30L, 15L)
        val quotaTwoChildTwo = quotaModel(provider.id, resourceTwo.id, folderChildTwo.id, 60L, 30L)
        val quotaTwoGrandchildOne = quotaModel(provider.id, resourceTwo.id, folderGrandchildOne.id, 30L, 15L)
        val quotaTwoGrandchildTwo = quotaModel(provider.id, resourceTwo.id, folderGrandchildTwo.id, 60L, 30L)
        val quotaThreeRoot = quotaModel(provider.id, resourceThree.id, folderRoot.id, 10L, 5L)
        val quotaThreeChildOne = quotaModel(provider.id, resourceThree.id, folderChildOne.id, 30L, 15L)
        val quotaThreeChildTwo = quotaModel(provider.id, resourceThree.id, folderChildTwo.id, 60L, 30L)
        val quotaThreeGrandchildOne = quotaModel(provider.id, resourceThree.id, folderGrandchildOne.id, 30L, 15L)
        val quotaThreeGrandchildTwo = quotaModel(provider.id, resourceThree.id, folderGrandchildTwo.id, 60L, 30L)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.upsertAllRetryable(txSession, listOf(quotaOneRoot, quotaOneChildOne, quotaOneChildTwo,
                quotaOneGrandchildOne, quotaOneGrandchildTwo, quotaTwoRoot, quotaTwoChildOne, quotaTwoChildTwo,
                quotaTwoGrandchildOne, quotaTwoGrandchildTwo, quotaThreeRoot, quotaThreeChildOne, quotaThreeChildTwo,
                quotaThreeGrandchildOne, quotaThreeGrandchildTwo)).awaitSingleOrNull()
        }}
        val provisionOneRoot = accountQuotaModel(provider.id, resourceOne.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionOneChildOne = accountQuotaModel(provider.id, resourceOne.id, folderChildOne.id, accountChildOne.id,
            15L, 6L)
        val provisionOneChildTwo = accountQuotaModel(provider.id, resourceOne.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L)
        val provisionOneGrandchildOne = accountQuotaModel(provider.id, resourceOne.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L)
        val provisionOneGrandchildTwo = accountQuotaModel(provider.id, resourceOne.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L)
        val provisionTwoRoot = accountQuotaModel(provider.id, resourceTwo.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionTwoChildOne = accountQuotaModel(provider.id, resourceTwo.id, folderChildOne.id, accountChildOne.id,
            15L, 6L)
        val provisionTwoChildTwo = accountQuotaModel(provider.id, resourceTwo.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L)
        val provisionTwoGrandchildOne = accountQuotaModel(provider.id, resourceTwo.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L)
        val provisionTwoGrandchildTwo = accountQuotaModel(provider.id, resourceTwo.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L)
        val provisionThreeRoot = accountQuotaModel(provider.id, resourceThree.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionThreeChildOne = accountQuotaModel(provider.id, resourceThree.id, folderChildOne.id, accountChildOne.id,
            15L, 6L)
        val provisionThreeChildTwo = accountQuotaModel(provider.id, resourceThree.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L)
        val provisionThreeGrandchildOne = accountQuotaModel(provider.id, resourceThree.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L)
        val provisionThreeGrandchildTwo = accountQuotaModel(provider.id, resourceThree.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.upsertAllRetryable(txSession, listOf(provisionOneRoot, provisionOneChildOne, provisionOneChildTwo,
                provisionOneGrandchildOne, provisionOneGrandchildTwo, provisionTwoRoot, provisionTwoChildOne,
                provisionTwoChildTwo, provisionTwoGrandchildOne, provisionTwoGrandchildTwo, provisionThreeRoot,
                provisionThreeChildOne, provisionThreeChildTwo, provisionThreeGrandchildOne,
                provisionThreeGrandchildTwo)).awaitSingleOrNull()
        }}
        val now = Instant.now()
        val usages = mutableListOf<ServiceUsageModel>()
        listOf(resourceOne, resourceTwo, resourceThree).forEach {
            usages.add(serviceUsageModel(root.id, provider.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L)))
            usages.add(serviceUsageModel(childOne.id, provider.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(childTwo.id, provider.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(grandchildOne.id, provider.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
            usages.add(serviceUsageModel(grandchildTwo.id, provider.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, usages)
        }}
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        val resultRoot = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(root.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultRoot.aggregates.size)
        Assertions.assertEquals(root.id, resultRoot.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultRoot.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(setOf(resourceTypeOne.id, resourceTypeTwo.id), resultRoot.aggregates[0].providers[0]
            .resourceTypes.map { it.resourceTypeId }.toSet())
        Assertions.assertEquals(setOf(false, true), resultRoot.aggregates[0].providers[0]
            .resourceTypes.map { it.expandResources }.toSet())
        val resultChildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childOne.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildOne.aggregates.size)
        Assertions.assertEquals(childOne.id, resultChildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultChildOne.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(setOf(resourceTypeOne.id, resourceTypeTwo.id), resultRoot.aggregates[0].providers[0]
            .resourceTypes.map { it.resourceTypeId }.toSet())
        Assertions.assertEquals(setOf(false, true), resultRoot.aggregates[0].providers[0]
            .resourceTypes.map { it.expandResources }.toSet())
        val resultChildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childTwo.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildTwo.aggregates.size)
        Assertions.assertEquals(childTwo.id, resultChildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultChildTwo.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(setOf(resourceTypeOne.id, resourceTypeTwo.id), resultRoot.aggregates[0].providers[0]
            .resourceTypes.map { it.resourceTypeId }.toSet())
        Assertions.assertEquals(setOf(false, true), resultRoot.aggregates[0].providers[0]
            .resourceTypes.map { it.expandResources }.toSet())
        val resultGrandchildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildOne.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildOne.aggregates.size)
        Assertions.assertEquals(grandchildOne.id, resultGrandchildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultGrandchildOne.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(setOf(resourceTypeOne.id, resourceTypeTwo.id), resultRoot.aggregates[0].providers[0]
            .resourceTypes.map { it.resourceTypeId }.toSet())
        Assertions.assertEquals(setOf(false, true), resultRoot.aggregates[0].providers[0]
            .resourceTypes.map { it.expandResources }.toSet())
        val resultGrandchildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildTwo.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultGrandchildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultGrandchildTwo.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(setOf(resourceTypeOne.id, resourceTypeTwo.id), resultRoot.aggregates[0].providers[0]
            .resourceTypes.map { it.resourceTypeId }.toSet())
        Assertions.assertEquals(setOf(false, true), resultRoot.aggregates[0].providers[0]
            .resourceTypes.map { it.expandResources }.toSet())
    }

    @Test
    fun testServiceTotalManyProvidersSegmentedSegmentationFlags(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne, childTwo, grandchildOne, grandchildTwo))
                .awaitSingleOrNull()
        }}
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne, folderChildTwo,
                folderGrandchildOne, folderGrandchildTwo)).awaitSingleOrNull()
        }}
        val providerOne = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        val providerTwo = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProvidersRetryable(txSession,
            listOf(providerOne, providerTwo)).awaitSingleOrNull() }}
        val resourceTypeOne = resourceTypeModel(providerOne.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeTwo = resourceTypeModel(providerOne.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeThree = resourceTypeModel(providerTwo.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeFour = resourceTypeModel(providerTwo.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypesRetryable(txSession,
            listOf(resourceTypeOne, resourceTypeTwo, resourceTypeThree, resourceTypeFour)).awaitSingleOrNull() }}
        val segmentationOne = resourceSegmentationModel(providerOne.id, "test")
        val segmentationTwo = resourceSegmentationModel(providerTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentationsDao.upsertResourceSegmentationsRetryable(txSession,
            listOf(segmentationOne, segmentationTwo)).awaitSingleOrNull() }}
        val segmentOne = resourceSegmentModel(segmentationOne.id, "test")
        val segmentTwo = resourceSegmentModel(segmentationOne.id, "test")
        val segmentThree = resourceSegmentModel(segmentationTwo.id, "test")
        val segmentFour = resourceSegmentModel(segmentationTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession,
            listOf(segmentOne, segmentTwo, segmentThree, segmentFour)).awaitSingleOrNull() }}
        val resourceOne = resourceModel(providerOne.id, "test", resourceTypeOne.id, mapOf(segmentationOne.id to segmentOne.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceTwo = resourceModel(providerOne.id, "test", resourceTypeOne.id, mapOf(segmentationOne.id to segmentTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceThree = resourceModel(providerOne.id, "test", resourceTypeTwo.id, mapOf(segmentationOne.id to segmentOne.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceFour = resourceModel(providerOne.id, "test", resourceTypeTwo.id, mapOf(segmentationOne.id to segmentTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceFive = resourceModel(providerTwo.id, "test", resourceTypeThree.id, mapOf(segmentationTwo.id to segmentThree.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val resourceSix = resourceModel(providerTwo.id, "test", resourceTypeFour.id, mapOf(segmentationTwo.id to segmentThree.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourcesRetryable(txSession,
            listOf(resourceOne, resourceTwo, resourceThree, resourceFour, resourceFive, resourceSix))
            .awaitSingleOrNull() }}
        val accountRootProviderOne = accountModel(providerOne.id, null, folderRoot.id, "test")
        val accountChildOneProviderOne = accountModel(providerOne.id, null, folderChildOne.id, "test")
        val accountChildTwoProviderOne = accountModel(providerOne.id, null, folderChildTwo.id, "test")
        val accountGrandchildOneProviderOne = accountModel(providerOne.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwoProviderOne = accountModel(providerOne.id, null, folderGrandchildTwo.id, "test")
        val accountRootProviderTwo = accountModel(providerTwo.id, null, folderRoot.id, "test")
        val accountChildOneProviderTwo = accountModel(providerTwo.id, null, folderChildOne.id, "test")
        val accountChildTwoProviderTwo = accountModel(providerTwo.id, null, folderChildTwo.id, "test")
        val accountGrandchildOneProviderTwo = accountModel(providerTwo.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwoProviderTwo = accountModel(providerTwo.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRootProviderOne, accountChildOneProviderOne, accountChildTwoProviderOne,
                accountGrandchildOneProviderOne, accountGrandchildTwoProviderOne, accountRootProviderTwo,
                accountChildOneProviderTwo, accountChildTwoProviderTwo, accountGrandchildOneProviderTwo,
                accountGrandchildTwoProviderTwo)).awaitSingleOrNull()
        }}
        val quotas = mutableListOf<QuotaModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            quotas.add(quotaModel(providerOne.id, it.id, folderRoot.id, 10L, 5L))
            quotas.add(quotaModel(providerOne.id, it.id, folderChildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerOne.id, it.id, folderChildTwo.id, 60L, 30L))
            quotas.add(quotaModel(providerOne.id, it.id, folderGrandchildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerOne.id, it.id, folderGrandchildTwo.id, 60L, 30L))
        }
        listOf(resourceFive, resourceSix).forEach {
            quotas.add(quotaModel(providerTwo.id, it.id, folderRoot.id, 10L, 5L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderChildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderChildTwo.id, 60L, 30L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderGrandchildOne.id, 30L, 15L))
            quotas.add(quotaModel(providerTwo.id, it.id, folderGrandchildTwo.id, 60L, 30L))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.upsertAllRetryable(txSession, quotas).awaitSingleOrNull()
        }}
        val provisions = mutableListOf<AccountsQuotasModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderRoot.id, accountRootProviderOne.id,
                5L, 2L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderChildOne.id, accountChildOneProviderOne.id,
                15L, 6L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderChildTwo.id, accountChildTwoProviderOne.id,
                30L, 12L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderGrandchildOne.id,
                accountGrandchildOneProviderOne.id, 15L, 6L))
            provisions.add(accountQuotaModel(providerOne.id, it.id, folderGrandchildTwo.id,
                accountGrandchildTwoProviderOne.id, 30L, 12L))
        }
        listOf(resourceFive, resourceSix).forEach {
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderRoot.id, accountRootProviderTwo.id,
                5L, 2L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderChildOne.id, accountChildOneProviderTwo.id,
                15L, 6L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderChildTwo.id, accountChildTwoProviderTwo.id,
                30L, 12L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderGrandchildOne.id,
                accountGrandchildOneProviderTwo.id, 15L, 6L))
            provisions.add(accountQuotaModel(providerTwo.id, it.id, folderGrandchildTwo.id,
                accountGrandchildTwoProviderTwo.id, 30L, 12L))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.upsertAllRetryable(txSession, provisions).awaitSingleOrNull()
        }}
        val now = Instant.now()
        val usages = mutableListOf<ServiceUsageModel>()
        listOf(resourceOne, resourceTwo, resourceThree, resourceFour).forEach {
            usages.add(serviceUsageModel(root.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L)))
            usages.add(serviceUsageModel(childOne.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(childTwo.id, providerOne.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(grandchildOne.id, providerOne.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
            usages.add(serviceUsageModel(grandchildTwo.id, providerOne.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
        }
        listOf(resourceFive, resourceSix).forEach {
            usages.add(serviceUsageModel(root.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L)))
            usages.add(serviceUsageModel(childOne.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(childTwo.id, providerTwo.id, it.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L)))
            usages.add(serviceUsageModel(grandchildOne.id, providerTwo.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
            usages.add(serviceUsageModel(grandchildTwo.id, providerTwo.id, it.id, now,
                mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, usages)
        }}
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", providerOne.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", providerTwo.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        val resultRoot = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(root.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultRoot.aggregates.size)
        Assertions.assertEquals(root.id, resultRoot.aggregates[0].serviceId)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[1].resourceTypes.size)
        val rootProviderOne = resultRoot.aggregates[0].providers.associateBy { it.providerId }[providerOne.id]!!
        val rootProviderTwo = resultRoot.aggregates[0].providers.associateBy { it.providerId }[providerTwo.id]!!
        Assertions.assertTrue(rootProviderOne.resourceTypes.all { type -> type.resources.all { resource -> resource
            .segmentations.all { segmentation -> segmentation.displayInExpandedView } } })
        Assertions.assertTrue(rootProviderTwo.resourceTypes.all { type -> type.resources.all { resource -> resource
            .segmentations.all { segmentation -> !segmentation.displayInExpandedView } } })
        val resultChildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childOne.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildOne.aggregates.size)
        Assertions.assertEquals(childOne.id, resultChildOne.aggregates[0].serviceId)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[1].resourceTypes.size)
        val childOneProviderOne = resultChildOne.aggregates[0].providers.associateBy { it.providerId }[providerOne.id]!!
        val childOneProviderTwo = resultChildOne.aggregates[0].providers.associateBy { it.providerId }[providerTwo.id]!!
        Assertions.assertTrue(childOneProviderOne.resourceTypes.all { type -> type.resources.all { resource -> resource
            .segmentations.all { segmentation -> segmentation.displayInExpandedView } } })
        Assertions.assertTrue(childOneProviderTwo.resourceTypes.all { type -> type.resources.all { resource -> resource
            .segmentations.all { segmentation -> !segmentation.displayInExpandedView } } })
        val resultChildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childTwo.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildTwo.aggregates.size)
        Assertions.assertEquals(childTwo.id, resultChildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[1].resourceTypes.size)
        val childTwoProviderOne = resultChildTwo.aggregates[0].providers.associateBy { it.providerId }[providerOne.id]!!
        val childTwoProviderTwo = resultChildTwo.aggregates[0].providers.associateBy { it.providerId }[providerTwo.id]!!
        Assertions.assertTrue(childTwoProviderOne.resourceTypes.all { type -> type.resources.all { resource -> resource
            .segmentations.all { segmentation -> segmentation.displayInExpandedView } } })
        Assertions.assertTrue(childTwoProviderTwo.resourceTypes.all { type -> type.resources.all { resource -> resource
            .segmentations.all { segmentation -> !segmentation.displayInExpandedView } } })
        val resultGrandchildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildOne.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildOne.aggregates.size)
        Assertions.assertEquals(grandchildOne.id, resultGrandchildOne.aggregates[0].serviceId)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[1].resourceTypes.size)
        val grandchildOneProviderOne = resultGrandchildOne.aggregates[0].providers.associateBy { it.providerId }[providerOne.id]!!
        val grandchildOneProviderTwo = resultGrandchildOne.aggregates[0].providers.associateBy { it.providerId }[providerTwo.id]!!
        Assertions.assertTrue(grandchildOneProviderOne.resourceTypes.all { type -> type.resources.all { resource -> resource
            .segmentations.all { segmentation -> segmentation.displayInExpandedView } } })
        Assertions.assertTrue(grandchildOneProviderTwo.resourceTypes.all { type -> type.resources.all { resource -> resource
            .segmentations.all { segmentation -> !segmentation.displayInExpandedView } } })
        val resultGrandchildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildTwo.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultGrandchildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[1].resourceTypes.size)
        val grandchildTwoProviderOne = resultGrandchildTwo.aggregates[0].providers.associateBy { it.providerId }[providerOne.id]!!
        val grandchildTwoProviderTwo = resultGrandchildTwo.aggregates[0].providers.associateBy { it.providerId }[providerTwo.id]!!
        Assertions.assertTrue(grandchildTwoProviderOne.resourceTypes.all { type -> type.resources.all { resource -> resource
            .segmentations.all { segmentation -> segmentation.displayInExpandedView } } })
        Assertions.assertTrue(grandchildTwoProviderTwo.resourceTypes.all { type -> type.resources.all { resource -> resource
            .segmentations.all { segmentation -> !segmentation.displayInExpandedView } } })
    }

    @Test
    fun testServiceTotalResourcesWithoutUsage(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne, childTwo, grandchildOne, grandchildTwo))
                .awaitSingleOrNull()
        }}
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne, folderChildTwo,
                folderGrandchildOne, folderGrandchildTwo)).awaitSingleOrNull()
        }}
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProviderRetryable(txSession, provider)
            .awaitSingleOrNull() }}
        val resourceType = resourceTypeModel(provider.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val otherResourceType = resourceTypeModel(provider.id, "other_test", UnitsEnsembleIds.CPU_UNITS_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypesRetryable(txSession,
            listOf(resourceType, otherResourceType)).awaitSingleOrNull() }}
        val resource = resourceModel(provider.id, "test", resourceType.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        val otherResource = resourceModel(provider.id, "other_test", otherResourceType.id, emptyMap(),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.CORES), UnitIds.CORES, UnitIds.CORES, null)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourcesRetryable(txSession,
            listOf(resource, otherResource)).awaitSingleOrNull() }}
        val accountRoot = accountModel(provider.id, null, folderRoot.id, "test")
        val accountChildOne = accountModel(provider.id, null, folderChildOne.id, "test")
        val accountChildTwo = accountModel(provider.id, null, folderChildTwo.id, "test")
        val accountGrandchildOne = accountModel(provider.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwo = accountModel(provider.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRoot, accountChildOne, accountChildTwo,
                accountGrandchildOne, accountGrandchildTwo)).awaitSingleOrNull()
        }}
        val quotaRoot = quotaModel(provider.id, resource.id, folderRoot.id, 10L, 5L)
        val quotaChildOne = quotaModel(provider.id, resource.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwo = quotaModel(provider.id, resource.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOne = quotaModel(provider.id, resource.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwo = quotaModel(provider.id, resource.id, folderGrandchildTwo.id, 60L, 30L)
        val otherQuotaRoot = quotaModel(provider.id, otherResource.id, folderRoot.id, 10L, 5L)
        val otherQuotaChildOne = quotaModel(provider.id, otherResource.id, folderChildOne.id, 30L, 15L)
        val otherQuotaChildTwo = quotaModel(provider.id, otherResource.id, folderChildTwo.id, 60L, 30L)
        val otherQuotaGrandchildOne = quotaModel(provider.id, otherResource.id, folderGrandchildOne.id, 30L, 15L)
        val otherQuotaGrandchildTwo = quotaModel(provider.id, otherResource.id, folderGrandchildTwo.id, 60L, 30L)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.upsertAllRetryable(txSession, listOf(quotaRoot, quotaChildOne, quotaChildTwo,
                quotaGrandchildOne, quotaGrandchildTwo, otherQuotaRoot, otherQuotaChildOne, otherQuotaChildTwo,
                otherQuotaGrandchildOne, otherQuotaGrandchildTwo)).awaitSingleOrNull()
        }}
        val provisionRoot = accountQuotaModel(provider.id, resource.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOne = accountQuotaModel(provider.id, resource.id, folderChildOne.id, accountChildOne.id,
            15L, 6L)
        val provisionChildTwo = accountQuotaModel(provider.id, resource.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L)
        val provisionGrandchildOne = accountQuotaModel(provider.id, resource.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L)
        val provisionGrandchildTwo = accountQuotaModel(provider.id, resource.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L)
        val otherProvisionRoot = accountQuotaModel(provider.id, otherResource.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val otherProvisionChildOne = accountQuotaModel(provider.id, otherResource.id, folderChildOne.id, accountChildOne.id,
            15L, 6L)
        val otherProvisionChildTwo = accountQuotaModel(provider.id, otherResource.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L)
        val otherProvisionGrandchildOne = accountQuotaModel(provider.id, otherResource.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L)
        val otherProvisionGrandchildTwo = accountQuotaModel(provider.id, otherResource.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.upsertAllRetryable(txSession, listOf(provisionRoot, provisionChildOne, provisionChildTwo,
                provisionGrandchildOne, provisionGrandchildTwo, otherProvisionRoot, otherProvisionChildOne, otherProvisionChildTwo,
                otherProvisionGrandchildOne, otherProvisionGrandchildTwo)).awaitSingleOrNull()
        }}
        val now = Instant.now()
        val usageRoot = serviceUsageModel(root.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L))
        val usageChildOne = serviceUsageModel(childOne.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L))
        val usageChildTwo = serviceUsageModel(childTwo.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L))
        val usageGrandchildOne = serviceUsageModel(grandchildOne.id, provider.id, resource.id, now,
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null)
        val usageGrandchildTwo = serviceUsageModel(grandchildTwo.id, provider.id, resource.id, now,
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, listOf(usageRoot, usageChildOne, usageChildTwo,
                usageGrandchildOne, usageGrandchildTwo))
        }}
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        val resultRoot = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(root.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultRoot.aggregates.size)
        Assertions.assertEquals(root.id, resultRoot.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultRoot.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(2, resultRoot.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes[1].resources.size)
        val resultChildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childOne.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildOne.aggregates.size)
        Assertions.assertEquals(childOne.id, resultChildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultChildOne.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(2, resultChildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers[0].resourceTypes[1].resources.size)
        val resultChildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(childTwo.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildTwo.aggregates.size)
        Assertions.assertEquals(childTwo.id, resultChildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultChildTwo.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(2, resultChildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers[0].resourceTypes[1].resources.size)
        val resultGrandchildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildOne.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildOne.aggregates.size)
        Assertions.assertEquals(grandchildOne.id, resultGrandchildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultGrandchildOne.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(2, resultGrandchildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[1].resources.size)
        val resultGrandchildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(FindServiceTotalsRequestDto(grandchildTwo.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultGrandchildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultGrandchildTwo.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(2, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[1].resources.size)
    }

    @Test
    fun testSubtreeTotalWithUnit(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne, childTwo, grandchildOne, grandchildTwo))
                .awaitSingleOrNull()
        }}
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne, folderChildTwo,
                folderGrandchildOne, folderGrandchildTwo)).awaitSingleOrNull()
        }}
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProviderRetryable(txSession, provider)
            .awaitSingleOrNull() }}
        val resourceType = resourceTypeModel(provider.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            .awaitSingleOrNull() }}
        val resource = resourceModel(provider.id, "test", resourceType.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourceRetryable(txSession, resource)
            .awaitSingleOrNull() }}
        val accountRoot = accountModel(provider.id, null, folderRoot.id, "test")
        val accountChildOne = accountModel(provider.id, null, folderChildOne.id, "test")
        val accountChildTwo = accountModel(provider.id, null, folderChildTwo.id, "test")
        val accountGrandchildOne = accountModel(provider.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwo = accountModel(provider.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRoot, accountChildOne, accountChildTwo,
                accountGrandchildOne, accountGrandchildTwo)).awaitSingleOrNull()
        }}
        val quotaRoot = quotaModel(provider.id, resource.id, folderRoot.id, 10L, 5L)
        val quotaChildOne = quotaModel(provider.id, resource.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwo = quotaModel(provider.id, resource.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOne = quotaModel(provider.id, resource.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwo = quotaModel(provider.id, resource.id, folderGrandchildTwo.id, 60L, 30L)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.upsertAllRetryable(txSession, listOf(quotaRoot, quotaChildOne, quotaChildTwo,
                quotaGrandchildOne, quotaGrandchildTwo)).awaitSingleOrNull()
        }}
        val provisionRoot = accountQuotaModel(provider.id, resource.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOne = accountQuotaModel(provider.id, resource.id, folderChildOne.id, accountChildOne.id,
            15L, 6L)
        val provisionChildTwo = accountQuotaModel(provider.id, resource.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L)
        val provisionGrandchildOne = accountQuotaModel(provider.id, resource.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L)
        val provisionGrandchildTwo = accountQuotaModel(provider.id, resource.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.upsertAllRetryable(txSession, listOf(provisionRoot, provisionChildOne, provisionChildTwo,
                provisionGrandchildOne, provisionGrandchildTwo)).awaitSingleOrNull()
        }}
        val now = Instant.now()
        val usageRoot = serviceUsageModel(root.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L))
        val usageChildOne = serviceUsageModel(childOne.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L))
        val usageChildTwo = serviceUsageModel(childTwo.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L))
        val usageGrandchildOne = serviceUsageModel(grandchildOne.id, provider.id, resource.id, now,
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null)
        val usageGrandchildTwo = serviceUsageModel(grandchildTwo.id, provider.id, resource.id, now,
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, listOf(usageRoot, usageChildOne, usageChildTwo,
                usageGrandchildOne, usageGrandchildTwo))
        }}
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        val resultRoot = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findSubtreeTotal")
            .bodyValue(FindSubtreeTotalRequestDto(root.id, resource.id, UnitIds.BYTES))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindSubtreeTotalResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultRoot.aggregates.size)
        Assertions.assertEquals(root.id, resultRoot.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultRoot.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultRoot.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("190",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("95",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("95",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("38",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("57",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("152",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        val resultChildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findSubtreeTotal")
            .bodyValue(FindSubtreeTotalRequestDto(childOne.id, resource.id, UnitIds.BYTES))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindSubtreeTotalResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildOne.aggregates.size)
        Assertions.assertEquals(childOne.id, resultChildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultChildOne.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultChildOne.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("60",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("30",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("30",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("12",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("18",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("48",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        val resultChildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findSubtreeTotal")
            .bodyValue(FindSubtreeTotalRequestDto(childTwo.id, resource.id, UnitIds.BYTES))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindSubtreeTotalResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultChildTwo.aggregates.size)
        Assertions.assertEquals(childTwo.id, resultChildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultChildTwo.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("120",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("60",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("60",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("24",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("36",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("96",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        val resultGrandchildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findSubtreeTotal")
            .bodyValue(FindSubtreeTotalRequestDto(grandchildOne.id, resource.id, UnitIds.BYTES))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindSubtreeTotalResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildOne.aggregates.size)
        Assertions.assertEquals(grandchildOne.id, resultGrandchildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultGrandchildOne.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("30",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("15",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("15",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("6",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("9",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("24",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        val resultGrandchildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findSubtreeTotal")
            .bodyValue(FindSubtreeTotalRequestDto(grandchildTwo.id, resource.id, UnitIds.BYTES))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindSubtreeTotalResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultGrandchildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultGrandchildTwo.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("60",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("30",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("30",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("12",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("18",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("48",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
    }

    @Test
    fun testRankSubtreeWithUnit(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne, childTwo, grandchildOne, grandchildTwo))
                .awaitSingleOrNull()
        }}
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne, folderChildTwo,
                folderGrandchildOne, folderGrandchildTwo)).awaitSingleOrNull()
        }}
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProviderRetryable(txSession, provider)
            .awaitSingleOrNull() }}
        val resourceType = resourceTypeModel(provider.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            .awaitSingleOrNull() }}
        val resource = resourceModel(provider.id, "test", resourceType.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourceRetryable(txSession, resource)
            .awaitSingleOrNull() }}
        val accountRoot = accountModel(provider.id, null, folderRoot.id, "test")
        val accountChildOne = accountModel(provider.id, null, folderChildOne.id, "test")
        val accountChildTwo = accountModel(provider.id, null, folderChildTwo.id, "test")
        val accountGrandchildOne = accountModel(provider.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwo = accountModel(provider.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRoot, accountChildOne, accountChildTwo,
                accountGrandchildOne, accountGrandchildTwo)).awaitSingleOrNull()
        }}
        val quotaRoot = quotaModel(provider.id, resource.id, folderRoot.id, 10L, 5L)
        val quotaChildOne = quotaModel(provider.id, resource.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwo = quotaModel(provider.id, resource.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOne = quotaModel(provider.id, resource.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwo = quotaModel(provider.id, resource.id, folderGrandchildTwo.id, 60L, 30L)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.upsertAllRetryable(txSession, listOf(quotaRoot, quotaChildOne, quotaChildTwo,
                quotaGrandchildOne, quotaGrandchildTwo)).awaitSingleOrNull()
        }}
        val provisionRoot = accountQuotaModel(provider.id, resource.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOne = accountQuotaModel(provider.id, resource.id, folderChildOne.id, accountChildOne.id,
            15L, 6L)
        val provisionChildTwo = accountQuotaModel(provider.id, resource.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L)
        val provisionGrandchildOne = accountQuotaModel(provider.id, resource.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L)
        val provisionGrandchildTwo = accountQuotaModel(provider.id, resource.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.upsertAllRetryable(txSession, listOf(provisionRoot, provisionChildOne, provisionChildTwo,
                provisionGrandchildOne, provisionGrandchildTwo)).awaitSingleOrNull()
        }}
        val now = Instant.now()
        val usageRoot = serviceUsageModel(root.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L))
        val usageChildOne = serviceUsageModel(childOne.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L))
        val usageChildTwo = serviceUsageModel(childTwo.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L))
        val usageGrandchildOne = serviceUsageModel(grandchildOne.id, provider.id, resource.id, now,
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null)
        val usageGrandchildTwo = serviceUsageModel(grandchildTwo.id, provider.id, resource.id, now,
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, listOf(usageRoot, usageChildOne, usageChildTwo,
                usageGrandchildOne, usageGrandchildTwo))
        }}
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
        val resultRoot = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsRequestDto(root.id, resource.id, from = null, limit = 100, sortingParams = null, unitId = UnitIds.BYTES))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(5, resultRoot.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultRoot.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].folders.size)
        Assertions.assertEquals(folderGrandchildTwo.id, resultRoot.aggregates[0].folders[0].id)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultRoot.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultRoot.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("60",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("30",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("30",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("12",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("18",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("48",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        Assertions.assertEquals(childTwo.id, resultRoot.aggregates[1].serviceId)
        Assertions.assertEquals(1, resultRoot.aggregates[1].folders.size)
        Assertions.assertEquals(folderChildTwo.id, resultRoot.aggregates[1].folders[0].id)
        Assertions.assertEquals(1, resultRoot.aggregates[1].providers.size)
        Assertions.assertEquals(provider.id, resultRoot.aggregates[1].providers[0].providerId)
        Assertions.assertEquals(1, resultRoot.aggregates[1].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultRoot.aggregates[1].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultRoot.aggregates[1].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultRoot.aggregates[1].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("30",
            resultRoot.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("30",
            resultRoot.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("12",
            resultRoot.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("18",
            resultRoot.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("48",
            resultRoot.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultRoot.aggregates[1].providers[0].resourceTypes[0].resources[0].usage)
        Assertions.assertEquals(grandchildOne.id, resultRoot.aggregates[2].serviceId)
        Assertions.assertEquals(1, resultRoot.aggregates[2].folders.size)
        Assertions.assertEquals(folderGrandchildOne.id, resultRoot.aggregates[2].folders[0].id)
        Assertions.assertEquals(1, resultRoot.aggregates[2].providers.size)
        Assertions.assertEquals(provider.id, resultRoot.aggregates[2].providers[0].providerId)
        Assertions.assertEquals(1, resultRoot.aggregates[2].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultRoot.aggregates[2].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultRoot.aggregates[2].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultRoot.aggregates[2].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("15",
            resultRoot.aggregates[2].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("15",
            resultRoot.aggregates[2].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("6",
            resultRoot.aggregates[2].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("9",
            resultRoot.aggregates[2].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("24",
            resultRoot.aggregates[2].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultRoot.aggregates[2].providers[0].resourceTypes[0].resources[0].usage)
        Assertions.assertEquals(childOne.id, resultRoot.aggregates[3].serviceId)
        Assertions.assertEquals(1, resultRoot.aggregates[3].folders.size)
        Assertions.assertEquals(folderChildOne.id, resultRoot.aggregates[3].folders[0].id)
        Assertions.assertEquals(1, resultRoot.aggregates[3].providers.size)
        Assertions.assertEquals(provider.id, resultRoot.aggregates[3].providers[0].providerId)
        Assertions.assertEquals(1, resultRoot.aggregates[3].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultRoot.aggregates[3].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultRoot.aggregates[3].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultRoot.aggregates[3].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("15",
            resultRoot.aggregates[3].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("15",
            resultRoot.aggregates[3].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("6",
            resultRoot.aggregates[3].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("9",
            resultRoot.aggregates[3].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("24",
            resultRoot.aggregates[3].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultRoot.aggregates[3].providers[0].resourceTypes[0].resources[0].usage)
        Assertions.assertEquals(root.id, resultRoot.aggregates[4].serviceId)
        Assertions.assertEquals(1, resultRoot.aggregates[4].folders.size)
        Assertions.assertEquals(folderRoot.id, resultRoot.aggregates[4].folders[0].id)
        Assertions.assertEquals(1, resultRoot.aggregates[4].providers.size)
        Assertions.assertEquals(provider.id, resultRoot.aggregates[4].providers[0].providerId)
        Assertions.assertEquals(1, resultRoot.aggregates[4].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultRoot.aggregates[4].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultRoot.aggregates[4].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultRoot.aggregates[4].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("5",
            resultRoot.aggregates[4].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("5",
            resultRoot.aggregates[4].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("2",
            resultRoot.aggregates[4].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("3",
            resultRoot.aggregates[4].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("8",
            resultRoot.aggregates[4].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultRoot.aggregates[4].providers[0].resourceTypes[0].resources[0].usage)
        val resultChildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsRequestDto(childOne.id, resource.id, from = null, limit = 100, sortingParams = null, unitId = UnitIds.BYTES))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(2, resultChildOne.aggregates.size)
        Assertions.assertEquals(grandchildOne.id, resultChildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].folders.size)
        Assertions.assertEquals(folderGrandchildOne.id, resultChildOne.aggregates[0].folders[0].id)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultChildOne.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultChildOne.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("60",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("15",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("15",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("6",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("9",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("24",
            resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultChildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        Assertions.assertEquals(childOne.id, resultChildOne.aggregates[1].serviceId)
        Assertions.assertEquals(1, resultChildOne.aggregates[1].folders.size)
        Assertions.assertEquals(folderChildOne.id, resultChildOne.aggregates[1].folders[0].id)
        Assertions.assertEquals(1, resultChildOne.aggregates[1].providers.size)
        Assertions.assertEquals(provider.id, resultChildOne.aggregates[1].providers[0].providerId)
        Assertions.assertEquals(1, resultChildOne.aggregates[1].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultChildOne.aggregates[1].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultChildOne.aggregates[1].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultChildOne.aggregates[1].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("15",
            resultChildOne.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("15",
            resultChildOne.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("6",
            resultChildOne.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("9",
            resultChildOne.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("24",
            resultChildOne.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultChildOne.aggregates[1].providers[0].resourceTypes[0].resources[0].usage)
        val resultChildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsRequestDto(childTwo.id, resource.id, from = null, limit = 100, sortingParams = null, unitId = UnitIds.BYTES))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(2, resultChildTwo.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultChildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].folders.size)
        Assertions.assertEquals(folderGrandchildTwo.id, resultChildTwo.aggregates[0].folders[0].id)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultChildTwo.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("60",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("30",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("30",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("12",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("18",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("48",
            resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultChildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        Assertions.assertEquals(childTwo.id, resultChildTwo.aggregates[1].serviceId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[1].folders.size)
        Assertions.assertEquals(folderChildTwo.id, resultChildTwo.aggregates[1].folders[0].id)
        Assertions.assertEquals(1, resultChildTwo.aggregates[1].providers.size)
        Assertions.assertEquals(provider.id, resultChildTwo.aggregates[1].providers[0].providerId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[1].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultChildTwo.aggregates[1].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultChildTwo.aggregates[1].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultChildTwo.aggregates[1].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("30",
            resultChildTwo.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("30",
            resultChildTwo.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("12",
            resultChildTwo.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("18",
            resultChildTwo.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("48",
            resultChildTwo.aggregates[1].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultChildTwo.aggregates[1].providers[0].resourceTypes[0].resources[0].usage)
        val resultGrandchildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsRequestDto(grandchildOne.id, resource.id, from = null, limit = 100, sortingParams = null, unitId = UnitIds.BYTES))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildOne.aggregates.size)
        Assertions.assertEquals(grandchildOne.id, resultGrandchildOne.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].folders.size)
        Assertions.assertEquals(folderGrandchildOne.id, resultGrandchildOne.aggregates[0].folders[0].id)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultGrandchildOne.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("60",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("15",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("15",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("6",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("9",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("24",
            resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultGrandchildOne.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
        val resultGrandchildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsRequestDto(grandchildTwo.id, resource.id, from = null, limit = 100, sortingParams = null, unitId = UnitIds.BYTES))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultGrandchildTwo.aggregates[0].serviceId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].folders.size)
        Assertions.assertEquals(folderGrandchildTwo.id, resultGrandchildTwo.aggregates[0].folders[0].id)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers.size)
        Assertions.assertEquals(provider.id, resultGrandchildTwo.aggregates[0].providers[0].providerId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes.size)
        Assertions.assertEquals(resourceType.id, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources.size)
        Assertions.assertEquals(resource.id,
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        Assertions.assertEquals("60",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.rawAmount)
        Assertions.assertEquals("30",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.rawAmount)
        Assertions.assertEquals("30",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.rawAmount)
        Assertions.assertEquals("12",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.rawAmount)
        Assertions.assertEquals("18",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.rawAmount)
        Assertions.assertEquals("48",
            resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.rawAmount)
        Assertions.assertNotNull(resultGrandchildTwo.aggregates[0].providers[0].resourceTypes[0].resources[0].usage)
    }

    @Test
    fun rankSubtreeWithCustomSortTest(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                servicesDao.upsertRecipeManyRetryable(
                    txSession,
                    listOf(root, childOne, childTwo, grandchildOne, grandchildTwo)
                )
                    .awaitSingleOrNull()
            }
        }
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                folderDao.upsertAllRetryable(
                    txSession, listOf(
                    folderRoot, folderChildOne, folderChildTwo,
                    folderGrandchildOne, folderGrandchildTwo
                )
                ).awaitSingleOrNull()
            }
        }
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                providersDao.upsertProviderRetryable(txSession, provider)
                    .awaitSingleOrNull()
            }
        }
        val resourceType = resourceTypeModel(provider.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
                    .awaitSingleOrNull()
            }
        }
        val resource = resourceModel(
            provider.id, "test", resourceType.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourcesDao.upsertResourceRetryable(txSession, resource)
                    .awaitSingleOrNull()
            }
        }
        val accountRoot = accountModel(provider.id, null, folderRoot.id, "test")
        val accountChildOne = accountModel(provider.id, null, folderChildOne.id, "test")
        val accountChildTwo = accountModel(provider.id, null, folderChildTwo.id, "test")
        val accountGrandchildOne = accountModel(provider.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwo = accountModel(provider.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsDao.upsertAllRetryable(
                    txSession, listOf(
                    accountRoot, accountChildOne, accountChildTwo,
                    accountGrandchildOne, accountGrandchildTwo
                )
                ).awaitSingleOrNull()
            }
        }
        val quotaRoot = quotaModel(provider.id, resource.id, folderRoot.id, 10L, 5L)
        val quotaChildOne = quotaModel(provider.id, resource.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwo = quotaModel(provider.id, resource.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOne = quotaModel(provider.id, resource.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwo = quotaModel(provider.id, resource.id, folderGrandchildTwo.id, 60L, 30L)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                quotasDao.upsertAllRetryable(
                    txSession, listOf(
                    quotaRoot, quotaChildOne, quotaChildTwo,
                    quotaGrandchildOne, quotaGrandchildTwo
                )
                ).awaitSingleOrNull()
            }
        }
        val provisionRoot = accountQuotaModel(provider.id, resource.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOne = accountQuotaModel(
            provider.id, resource.id, folderChildOne.id, accountChildOne.id,
            15L, 6L
        )
        val provisionChildTwo = accountQuotaModel(
            provider.id, resource.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L
        )
        val provisionGrandchildOne = accountQuotaModel(
            provider.id, resource.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L
        )
        val provisionGrandchildTwo = accountQuotaModel(
            provider.id, resource.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsQuotasDao.upsertAllRetryable(
                    txSession, listOf(
                    provisionRoot, provisionChildOne, provisionChildTwo,
                    provisionGrandchildOne, provisionGrandchildTwo
                )
                ).awaitSingleOrNull()
            }
        }
        val now = Instant.now()
        val usageRoot = serviceUsageModel(root.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L))
        val usageChildOne = serviceUsageModel(childOne.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L))
        val usageChildTwo = serviceUsageModel(childTwo.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L))
        val usageGrandchildOne = serviceUsageModel(grandchildOne.id, provider.id, resource.id, now,
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null)
        val usageGrandchildTwo = serviceUsageModel(grandchildTwo.id, provider.id, resource.id, now,
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, listOf(usageRoot, usageChildOne, usageChildTwo,
                usageGrandchildOne, usageGrandchildTwo))
        }}
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent

        val resultAllocatedDesc = helper.rankSubtreeAmounts(
            root.id, resource.id, from = null, limit = 100,
            RankSubtreeSortingParamsDto(RankSubtreeSortingField.UNDERUTILIZED_ESTIMATION, RankSubtreeSortingOrder.DESC)
        )!!
        Assertions.assertEquals(
            listOf(grandchildTwo.id, childTwo.id, grandchildOne.id, childOne.id, root.id),
            resultAllocatedDesc.aggregates.map { a -> a.serviceId }
        )

        val resultAllocatedAsc = helper.rankSubtreeAmounts(
            root.id, resource.id, from = null, limit = 100,
            RankSubtreeSortingParamsDto(RankSubtreeSortingField.UNDERUTILIZED_ESTIMATION, RankSubtreeSortingOrder.ASC)
        )!!
        Assertions.assertEquals(
            listOf(root.id, childOne.id, grandchildOne.id, childTwo.id, grandchildTwo.id),
            resultAllocatedAsc.aggregates.map { a -> a.serviceId }
        )
    }

    @Test
    fun rankSubtreeWithCustomSortPagingTest(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                servicesDao.upsertRecipeManyRetryable(
                    txSession,
                    listOf(root, childOne, childTwo, grandchildOne, grandchildTwo)
                )
                    .awaitSingleOrNull()
            }
        }
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                folderDao.upsertAllRetryable(
                    txSession, listOf(
                    folderRoot, folderChildOne, folderChildTwo,
                    folderGrandchildOne, folderGrandchildTwo
                )
                ).awaitSingleOrNull()
            }
        }
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                providersDao.upsertProviderRetryable(txSession, provider)
                    .awaitSingleOrNull()
            }
        }
        val resourceType = resourceTypeModel(provider.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
                    .awaitSingleOrNull()
            }
        }
        val resource = resourceModel(
            provider.id, "test", resourceType.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourcesDao.upsertResourceRetryable(txSession, resource)
                    .awaitSingleOrNull()
            }
        }
        val accountRoot = accountModel(provider.id, null, folderRoot.id, "test")
        val accountChildOne = accountModel(provider.id, null, folderChildOne.id, "test")
        val accountChildTwo = accountModel(provider.id, null, folderChildTwo.id, "test")
        val accountGrandchildOne = accountModel(provider.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwo = accountModel(provider.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsDao.upsertAllRetryable(
                    txSession, listOf(
                    accountRoot, accountChildOne, accountChildTwo,
                    accountGrandchildOne, accountGrandchildTwo
                )
                ).awaitSingleOrNull()
            }
        }
        val quotaRoot = quotaModel(provider.id, resource.id, folderRoot.id, 10L, 5L)
        val quotaChildOne = quotaModel(provider.id, resource.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwo = quotaModel(provider.id, resource.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOne = quotaModel(provider.id, resource.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwo = quotaModel(provider.id, resource.id, folderGrandchildTwo.id, 60L, 30L)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                quotasDao.upsertAllRetryable(
                    txSession, listOf(
                    quotaRoot, quotaChildOne, quotaChildTwo,
                    quotaGrandchildOne, quotaGrandchildTwo
                )
                ).awaitSingleOrNull()
            }
        }
        val provisionRoot = accountQuotaModel(provider.id, resource.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOne = accountQuotaModel(
            provider.id, resource.id, folderChildOne.id, accountChildOne.id,
            15L, 6L
        )
        val provisionChildTwo = accountQuotaModel(
            provider.id, resource.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L
        )
        val provisionGrandchildOne = accountQuotaModel(
            provider.id, resource.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L
        )
        val provisionGrandchildTwo = accountQuotaModel(
            provider.id, resource.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsQuotasDao.upsertAllRetryable(
                    txSession, listOf(
                    provisionRoot, provisionChildOne, provisionChildTwo,
                    provisionGrandchildOne, provisionGrandchildTwo
                )
                ).awaitSingleOrNull()
            }
        }
        val now = Instant.now()
        val usageRoot = serviceUsageModel(root.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 4L, 600L to 8L), mapOf(0L to 0L, 300L to 5L, 600L to 10L))
        val usageChildOne = serviceUsageModel(childOne.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L))
        val usageChildTwo = serviceUsageModel(childTwo.id, provider.id, resource.id, now, mapOf(0L to 0L, 300L to 1L, 600L to 2L),
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), mapOf(0L to 0L, 300L to 2L, 600L to 4L))
        val usageGrandchildOne = serviceUsageModel(grandchildOne.id, provider.id, resource.id, now,
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null)
        val usageGrandchildTwo = serviceUsageModel(grandchildTwo.id, provider.id, resource.id, now,
            mapOf(0L to 0L, 300L to 1L, 600L to 2L), null, null)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, listOf(usageRoot, usageChildOne, usageChildTwo,
                usageGrandchildOne, usageGrandchildTwo))
        }}
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent

        val resultAllocatedDescFirstPage = helper.rankSubtreeAmounts(
            root.id, resource.id, from = null, limit = 3,
            RankSubtreeSortingParamsDto(RankSubtreeSortingField.UNDERUTILIZED_ESTIMATION, RankSubtreeSortingOrder.DESC)
        )!!
        Assertions.assertEquals(
            listOf(grandchildTwo.id, childTwo.id, grandchildOne.id),
            resultAllocatedDescFirstPage.aggregates.map { a -> a.serviceId }
        )
        val resultAllocatedDescSecondPage = helper.rankSubtreeAmounts(
            root.id, resource.id, resultAllocatedDescFirstPage.nextPageToken, limit = 3,
            RankSubtreeSortingParamsDto(RankSubtreeSortingField.UNDERUTILIZED_ESTIMATION, RankSubtreeSortingOrder.DESC)
        )!!
        Assertions.assertEquals(
            listOf(childOne.id, root.id),
            resultAllocatedDescSecondPage.aggregates.map { a -> a.serviceId }
        )

        val resultAllocatedAscFirstPage = helper.rankSubtreeAmounts(
            root.id, resource.id, from = null, limit = 3,
            RankSubtreeSortingParamsDto(RankSubtreeSortingField.UNDERUTILIZED_ESTIMATION, RankSubtreeSortingOrder.ASC)
        )!!
        Assertions.assertEquals(
            listOf(root.id, childOne.id, grandchildOne.id),
            resultAllocatedAscFirstPage.aggregates.map { a -> a.serviceId }
        )

        val resultAllocatedAscSecondPage = helper.rankSubtreeAmounts(
            root.id, resource.id, resultAllocatedAscFirstPage.nextPageToken, limit = 3,
            RankSubtreeSortingParamsDto(RankSubtreeSortingField.UNDERUTILIZED_ESTIMATION, RankSubtreeSortingOrder.ASC)
        )!!
        Assertions.assertEquals(
            listOf(childTwo.id, grandchildTwo.id),
            resultAllocatedAscSecondPage.aggregates.map { a -> a.serviceId }
        )
    }

    private fun providerModel(accountsSpacesSupported: Boolean,
                              aggregationMode: FreeProvisionAggregationMode
    ): ProviderModel {
        return ProviderModel.builder()
            .id(UUID.randomUUID().toString())
            .grpcApiUri("in-process:test")
            .restApiUri(null)
            .destinationTvmId(42L)
            .tenantId(Tenants.DEFAULT_TENANT_ID)
            .version(0L)
            .nameEn("Test")
            .nameRu("Test")
            .descriptionEn("Test")
            .descriptionRu("Test")
            .sourceTvmId(42L)
            .serviceId(69L)
            .deleted(false)
            .readOnly(false)
            .multipleAccountsPerFolder(true)
            .accountTransferWithQuota(true)
            .managed(true)
            .key("test")
            .trackerComponentId(1L)
            .accountsSettings(
                AccountsSettingsModel.builder()
                    .displayNameSupported(true)
                    .keySupported(true)
                    .deleteSupported(true)
                    .softDeleteSupported(false)
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
                    .build()
            )
            .importAllowed(true)
            .accountsSpacesSupported(accountsSpacesSupported)
            .syncEnabled(true)
            .grpcTlsOn(true)
            .aggregationSettings(AggregationSettings(aggregationMode, UsageMode.TIME_SERIES, 300))
            .build()
    }

    private fun resourceTypeModel(providerId: String, key: String, unitsEnsembleId: String): ResourceTypeModel {
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
            .build()
    }

    private fun resourceModel(
        providerId: String,
        key: String,
        resourceTypeId: String,
        segments: Map<String, String>,
        unitsEnsembleId: String,
        allowedUnitIds: Set<String>,
        defaultUnitId: String,
        baseUnitId: String,
        accountsSpaceId: String?
    ): ResourceModel {
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
            .segments(segments.map { (k ,v) -> ResourceSegmentSettingsModel(k, v) }.toSet())
            .resourceUnits(ResourceUnitsModel(allowedUnitIds, defaultUnitId, null))
            .managed(true)
            .orderable(true)
            .readOnly(false)
            .baseUnitId(baseUnitId)
            .accountsSpacesId(accountsSpaceId)
            .build()
    }

    private fun folderModel(serviceId: Long): FolderModel {
        return FolderModel.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setServiceId(serviceId)
            .setVersion(0L)
            .setDisplayName("Test")
            .setDescription("Test")
            .setDeleted(false)
            .setFolderType(FolderType.COMMON)
            .setTags(emptySet())
            .setNextOpLogOrder(1L)
            .build()
    }

    fun quotaModel(providerId: String, resourceId: String, folderId: String, quota: Long, balance: Long): QuotaModel {
        return QuotaModel.builder()
            .tenantId(Tenants.DEFAULT_TENANT_ID)
            .providerId(providerId)
            .resourceId(resourceId)
            .folderId(folderId)
            .quota(quota)
            .balance(balance)
            .frozenQuota(0L)
            .build()
    }

    private fun accountQuotaModel(
        providerId: String, resourceId: String, folderId: String,
        accountId: String, provided: Long, allocated: Long
    ): AccountsQuotasModel {
        return AccountsQuotasModel.Builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setProviderId(providerId)
            .setResourceId(resourceId)
            .setFolderId(folderId)
            .setAccountId(accountId)
            .setProvidedQuota(provided)
            .setAllocatedQuota(allocated)
            .setLastProvisionUpdate(Instant.now())
            .setLastReceivedProvisionVersion(null)
            .setLatestSuccessfulProvisionOperationId(null)
            .build()
    }

    private fun accountModel(providerId: String, accountsSpaceId: String?, folderId: String,
                             displayName: String): AccountModel {
        return AccountModel.Builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setId(UUID.randomUUID().toString())
            .setVersion(0L)
            .setProviderId(providerId)
            .setAccountsSpacesId(accountsSpaceId)
            .setOuterAccountIdInProvider(UUID.randomUUID().toString())
            .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
            .setFolderId(folderId)
            .setDisplayName(displayName)
            .setDeleted(false)
            .setLastAccountUpdate(Instant.now())
            .setLastReceivedVersion(null)
            .setLatestSuccessfulAccountOperationId(null)
            .build()
    }

    private fun serviceModel(id: Long, parentId: Long?): ServiceRecipeModel {
        return ServiceRecipeModel.builder()
            .id(id)
            .name("test")
            .nameEn("test")
            .slug("test-$id")
            .state(ServiceState.DEVELOP)
            .readOnlyState(null)
            .exportable(true)
            .parentId(parentId)
            .build()
    }

    fun resourceSegmentationModel(providerId: String, key: String): ResourceSegmentationModel {
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
            .build()
    }

    fun resourceSegmentModel(segmentationId: String, key: String): ResourceSegmentModel {
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
            .build()
    }
}
