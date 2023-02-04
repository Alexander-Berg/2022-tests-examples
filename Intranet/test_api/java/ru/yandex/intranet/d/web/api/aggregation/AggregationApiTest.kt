package ru.yandex.intranet.d.web.api.aggregation

import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
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
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao
import ru.yandex.intranet.d.dao.services.ServicesDao
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
import ru.yandex.intranet.d.model.quotas.QuotaModel
import ru.yandex.intranet.d.model.resources.ResourceModel
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel
import ru.yandex.intranet.d.model.services.ServiceRecipeModel
import ru.yandex.intranet.d.model.services.ServiceState
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.aggregation.RankSubtreeSortingField.ALLOCATED
import ru.yandex.intranet.d.web.model.aggregation.RankSubtreeSortingField.BALANCE
import ru.yandex.intranet.d.web.model.aggregation.RankSubtreeSortingField.PROVIDED
import ru.yandex.intranet.d.web.model.aggregation.RankSubtreeSortingField.QUOTA
import ru.yandex.intranet.d.web.model.aggregation.RankSubtreeSortingField.TRANSFERABLE
import ru.yandex.intranet.d.web.model.aggregation.RankSubtreeSortingField.UNALLOCATED
import ru.yandex.intranet.d.web.model.aggregation.RankSubtreeSortingOrder.ASC
import ru.yandex.intranet.d.web.model.aggregation.RankSubtreeSortingOrder.DESC
import ru.yandex.intranet.d.web.model.aggregation.RankSubtreeSortingParamsDto
import ru.yandex.intranet.d.web.model.aggregation.api.FindSubtreeTotalApiRequestDto
import ru.yandex.intranet.d.web.model.aggregation.api.FindSubtreeTotalApiResponseDto
import ru.yandex.intranet.d.web.model.aggregation.api.RankSubtreeAmountsApiRequestDto
import ru.yandex.intranet.d.web.model.aggregation.api.RankSubtreeAmountsApiResponseDto
import java.math.BigInteger
import java.time.Instant
import java.util.*

/**
 * Aggregation API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class AggregationApiTest(
    @Autowired private val serviceDenormalizedAggregatesDao: ServiceDenormalizedAggregatesDao,
    @Autowired private val providersDao: ProvidersDao,
    @Autowired private val resourceTypesDao: ResourceTypesDao,
    @Autowired private val resourcesDao: ResourcesDao,
    @Autowired private val servicesDao: ServicesDao,
    @Autowired private val folderDao: FolderDao,
    @Autowired private val quotasDao: QuotasDao,
    @Autowired private val accountsDao: AccountsDao,
    @Autowired private val accountsQuotasDao: AccountsQuotasDao,
    @Autowired private val tableClient: YdbTableClient,
    @Autowired private val webClient: WebTestClient,
    @Autowired private val helper: AggregationApiTestHelper
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
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/aggregation/_findSubtreeTotal")
            .bodyValue(FindSubtreeTotalApiRequestDto(root.id, resource.id, provider.id, null, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindSubtreeTotalApiResponseDto::class.java)
            .responseBody.awaitSingle()
        val resultRoot = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsApiRequestDto(rootServiceId = root.id, resourceId = resource.id, from = null,
                limit = 100, providerId = provider.id, sortingParams = null, includeUsage = null, includeUsageRaw = null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsApiResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(0, resultRoot.aggregates.size)
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
            .uri("/api/v1/aggregation/_findSubtreeTotal")
            .bodyValue(FindSubtreeTotalApiRequestDto(root.id, resource.id, provider.id, null, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindSubtreeTotalApiResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals("190", resultRoot.aggregate.amounts.quota?.amount)
        Assertions.assertEquals("95", resultRoot.aggregate.amounts.balance?.amount)
        Assertions.assertEquals("95", resultRoot.aggregate.amounts.provided?.amount)
        Assertions.assertEquals("38", resultRoot.aggregate.amounts.allocated?.amount)
        Assertions.assertEquals("57", resultRoot.aggregate.amounts.unallocated?.amount)
        Assertions.assertEquals("152", resultRoot.aggregate.amounts.transferable?.amount)
        val resultChildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/aggregation/_findSubtreeTotal")
            .bodyValue(FindSubtreeTotalApiRequestDto(childOne.id, resource.id, provider.id, null, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindSubtreeTotalApiResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals("60", resultChildOne.aggregate.amounts.quota?.amount)
        Assertions.assertEquals("30", resultChildOne.aggregate.amounts.balance?.amount)
        Assertions.assertEquals("30", resultChildOne.aggregate.amounts.provided?.amount)
        Assertions.assertEquals("12", resultChildOne.aggregate.amounts.allocated?.amount)
        Assertions.assertEquals("18", resultChildOne.aggregate.amounts.unallocated?.amount)
        Assertions.assertEquals("48", resultChildOne.aggregate.amounts.transferable?.amount)

        val resultChildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/aggregation/_findSubtreeTotal")
            .bodyValue(FindSubtreeTotalApiRequestDto(childTwo.id, resource.id, provider.id, null, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindSubtreeTotalApiResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals("120", resultChildTwo.aggregate.amounts.quota?.amount)
        Assertions.assertEquals("60", resultChildTwo.aggregate.amounts.balance?.amount)
        Assertions.assertEquals("60", resultChildTwo.aggregate.amounts.provided?.amount)
        Assertions.assertEquals("24", resultChildTwo.aggregate.amounts.allocated?.amount)
        Assertions.assertEquals("36", resultChildTwo.aggregate.amounts.unallocated?.amount)
        Assertions.assertEquals("96", resultChildTwo.aggregate.amounts.transferable?.amount)
        val resultGrandchildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/aggregation/_findSubtreeTotal")
            .bodyValue(FindSubtreeTotalApiRequestDto(grandchildOne.id, resource.id, provider.id, null, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindSubtreeTotalApiResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals("30", resultGrandchildOne.aggregate.amounts.quota?.amount)
        Assertions.assertEquals("15", resultGrandchildOne.aggregate.amounts.balance?.amount)
        Assertions.assertEquals("15", resultGrandchildOne.aggregate.amounts.provided?.amount)
        Assertions.assertEquals("6", resultGrandchildOne.aggregate.amounts.allocated?.amount)
        Assertions.assertEquals("9", resultGrandchildOne.aggregate.amounts.unallocated?.amount)
        Assertions.assertEquals("24", resultGrandchildOne.aggregate.amounts.transferable?.amount)
        val resultGrandchildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/aggregation/_findSubtreeTotal")
            .bodyValue(FindSubtreeTotalApiRequestDto(grandchildTwo.id, resource.id, provider.id, null, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(FindSubtreeTotalApiResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals("60", resultGrandchildTwo.aggregate.amounts.quota?.amount)
        Assertions.assertEquals("30", resultGrandchildTwo.aggregate.amounts.balance?.amount)
        Assertions.assertEquals("30", resultGrandchildTwo.aggregate.amounts.provided?.amount)
        Assertions.assertEquals("12", resultGrandchildTwo.aggregate.amounts.allocated?.amount)
        Assertions.assertEquals("18", resultGrandchildTwo.aggregate.amounts.unallocated?.amount)
        Assertions.assertEquals("48", resultGrandchildTwo.aggregate.amounts.transferable?.amount)
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
            .uri("/api/v1/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsApiRequestDto(rootServiceId = root.id, resourceId = resource.id, from = null,
                limit = 100, providerId = provider.id, sortingParams = null, includeUsage = null, includeUsageRaw = null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsApiResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(5, resultRoot.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultRoot.aggregates[0].serviceId)
        Assertions.assertEquals("60", resultRoot.aggregates[0].amounts.quota?.amount)
        Assertions.assertEquals("30", resultRoot.aggregates[0].amounts.balance?.amount)
        Assertions.assertEquals("30", resultRoot.aggregates[0].amounts.provided?.amount)
        Assertions.assertEquals("12", resultRoot.aggregates[0].amounts.allocated?.amount)
        Assertions.assertEquals("18", resultRoot.aggregates[0].amounts.unallocated?.amount)
        Assertions.assertEquals("48", resultRoot.aggregates[0].amounts.transferable?.amount)
        Assertions.assertEquals(childTwo.id, resultRoot.aggregates[1].serviceId)
        Assertions.assertEquals("60", resultRoot.aggregates[1].amounts.quota?.amount)
        Assertions.assertEquals("30", resultRoot.aggregates[1].amounts.balance?.amount)
        Assertions.assertEquals("30", resultRoot.aggregates[1].amounts.provided?.amount)
        Assertions.assertEquals("12", resultRoot.aggregates[1].amounts.allocated?.amount)
        Assertions.assertEquals("18", resultRoot.aggregates[1].amounts.unallocated?.amount)
        Assertions.assertEquals("48", resultRoot.aggregates[1].amounts.transferable?.amount)
        Assertions.assertEquals(grandchildOne.id, resultRoot.aggregates[2].serviceId)
        Assertions.assertEquals("30", resultRoot.aggregates[2].amounts.quota?.amount)
        Assertions.assertEquals("15", resultRoot.aggregates[2].amounts.balance?.amount)
        Assertions.assertEquals("15", resultRoot.aggregates[2].amounts.provided?.amount)
        Assertions.assertEquals("6", resultRoot.aggregates[2].amounts.allocated?.amount)
        Assertions.assertEquals("9", resultRoot.aggregates[2].amounts.unallocated?.amount)
        Assertions.assertEquals("24", resultRoot.aggregates[2].amounts.transferable?.amount)
        Assertions.assertEquals(childOne.id, resultRoot.aggregates[3].serviceId)
        Assertions.assertEquals("30", resultRoot.aggregates[3].amounts.quota?.amount)
        Assertions.assertEquals("15", resultRoot.aggregates[3].amounts.balance?.amount)
        Assertions.assertEquals("15", resultRoot.aggregates[3].amounts.provided?.amount)
        Assertions.assertEquals("6", resultRoot.aggregates[3].amounts.allocated?.amount)
        Assertions.assertEquals("9", resultRoot.aggregates[3].amounts.unallocated?.amount)
        Assertions.assertEquals("24", resultRoot.aggregates[3].amounts.transferable?.amount)
        Assertions.assertEquals(root.id, resultRoot.aggregates[4].serviceId)
        Assertions.assertEquals("10", resultRoot.aggregates[4].amounts.quota?.amount)
        Assertions.assertEquals("5", resultRoot.aggregates[4].amounts.balance?.amount)
        Assertions.assertEquals("5", resultRoot.aggregates[4].amounts.provided?.amount)
        Assertions.assertEquals("2", resultRoot.aggregates[4].amounts.allocated?.amount)
        Assertions.assertEquals("3", resultRoot.aggregates[4].amounts.unallocated?.amount)
        Assertions.assertEquals("8", resultRoot.aggregates[4].amounts.transferable?.amount)
        val resultChildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsApiRequestDto(rootServiceId = childOne.id, resourceId = resource.id,
                from = null, limit = 100, providerId = provider.id, sortingParams = null, includeUsage = null, includeUsageRaw = null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsApiResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(2, resultChildOne.aggregates.size)
        Assertions.assertEquals(grandchildOne.id, resultChildOne.aggregates[0].serviceId)
        Assertions.assertEquals("30", resultChildOne.aggregates[0].amounts.quota?.amount)
        Assertions.assertEquals("15", resultChildOne.aggregates[0].amounts.balance?.amount)
        Assertions.assertEquals("15", resultChildOne.aggregates[0].amounts.provided?.amount)
        Assertions.assertEquals("6", resultChildOne.aggregates[0].amounts.allocated?.amount)
        Assertions.assertEquals("9", resultChildOne.aggregates[0].amounts.unallocated?.amount)
        Assertions.assertEquals("24", resultChildOne.aggregates[0].amounts.transferable?.amount)
        Assertions.assertEquals(childOne.id, resultChildOne.aggregates[1].serviceId)
        Assertions.assertEquals("30", resultChildOne.aggregates[1].amounts.quota?.amount)
        Assertions.assertEquals("15", resultChildOne.aggregates[1].amounts.balance?.amount)
        Assertions.assertEquals("15", resultChildOne.aggregates[1].amounts.provided?.amount)
        Assertions.assertEquals("6", resultChildOne.aggregates[1].amounts.allocated?.amount)
        Assertions.assertEquals("9", resultChildOne.aggregates[1].amounts.unallocated?.amount)
        Assertions.assertEquals("24", resultChildOne.aggregates[1].amounts.transferable?.amount)
        val resultChildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsApiRequestDto(rootServiceId = childTwo.id, resourceId = resource.id,
                from = null, limit = 100, providerId = provider.id, sortingParams = null, includeUsage = null, includeUsageRaw = null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsApiResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(2, resultChildTwo.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultChildTwo.aggregates[0].serviceId)
        Assertions.assertEquals("60", resultChildTwo.aggregates[0].amounts.quota?.amount)
        Assertions.assertEquals("30", resultChildTwo.aggregates[0].amounts.balance?.amount)
        Assertions.assertEquals("30", resultChildTwo.aggregates[0].amounts.provided?.amount)
        Assertions.assertEquals("12", resultChildTwo.aggregates[0].amounts.allocated?.amount)
        Assertions.assertEquals("18", resultChildTwo.aggregates[0].amounts.unallocated?.amount)
        Assertions.assertEquals("48", resultChildTwo.aggregates[0].amounts.transferable?.amount)
        Assertions.assertEquals(childTwo.id, resultChildTwo.aggregates[1].serviceId)
        Assertions.assertEquals("60", resultChildTwo.aggregates[1].amounts.quota?.amount)
        Assertions.assertEquals("30", resultChildTwo.aggregates[1].amounts.balance?.amount)
        Assertions.assertEquals("30", resultChildTwo.aggregates[1].amounts.provided?.amount)
        Assertions.assertEquals("12", resultChildTwo.aggregates[1].amounts.allocated?.amount)
        Assertions.assertEquals("18", resultChildTwo.aggregates[1].amounts.unallocated?.amount)
        Assertions.assertEquals("48", resultChildTwo.aggregates[1].amounts.transferable?.amount)
        val resultGrandchildOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsApiRequestDto(rootServiceId = grandchildOne.id, resourceId = resource.id,
                from = null, limit = 100, providerId = provider.id, sortingParams = null, includeUsage = null, includeUsageRaw = null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsApiResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildOne.aggregates.size)
        Assertions.assertEquals(grandchildOne.id, resultGrandchildOne.aggregates[0].serviceId)
        Assertions.assertEquals("30", resultGrandchildOne.aggregates[0].amounts.quota?.amount)
        Assertions.assertEquals("15", resultGrandchildOne.aggregates[0].amounts.balance?.amount)
        Assertions.assertEquals("15", resultGrandchildOne.aggregates[0].amounts.provided?.amount)
        Assertions.assertEquals("6", resultGrandchildOne.aggregates[0].amounts.allocated?.amount)
        Assertions.assertEquals("9", resultGrandchildOne.aggregates[0].amounts.unallocated?.amount)
        Assertions.assertEquals("24", resultGrandchildOne.aggregates[0].amounts.transferable?.amount)
        val resultGrandchildTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsApiRequestDto(rootServiceId = grandchildTwo.id, resourceId = resource.id,
                from = null, limit = 100, providerId = provider.id, sortingParams = null, includeUsage = null, includeUsageRaw = null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsApiResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(1, resultGrandchildTwo.aggregates.size)
        Assertions.assertEquals(grandchildTwo.id, resultGrandchildTwo.aggregates[0].serviceId)
        Assertions.assertEquals("60", resultGrandchildTwo.aggregates[0].amounts.quota?.amount)
        Assertions.assertEquals("30", resultGrandchildTwo.aggregates[0].amounts.balance?.amount)
        Assertions.assertEquals("30", resultGrandchildTwo.aggregates[0].amounts.provided?.amount)
        Assertions.assertEquals("12", resultGrandchildTwo.aggregates[0].amounts.allocated?.amount)
        Assertions.assertEquals("18", resultGrandchildTwo.aggregates[0].amounts.unallocated?.amount)
        Assertions.assertEquals("48", resultGrandchildTwo.aggregates[0].amounts.transferable?.amount)
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
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent

        val resultAllocatedDesc = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 100, RankSubtreeSortingParamsDto(
                ALLOCATED,
                DESC
            ), null, null
        )!!
        Assertions.assertEquals(
            listOf(grandchildTwo.id, childTwo.id, grandchildOne.id, childOne.id, root.id),
            resultAllocatedDesc.aggregates.map { a -> a.serviceId }
        )

        val resultAllocatedAsc = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 100, RankSubtreeSortingParamsDto(
                ALLOCATED,
                ASC
            ), null, null
        )!!
        Assertions.assertEquals(
            listOf(root.id, childOne.id, grandchildOne.id, childTwo.id, grandchildTwo.id),
            resultAllocatedAsc.aggregates.map { a -> a.serviceId }
        )

        val resultBalanceDesc = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 100, RankSubtreeSortingParamsDto(
                BALANCE,
                DESC
            ), null, null
        )!!
        Assertions.assertEquals(
            listOf(grandchildTwo.id, childTwo.id, grandchildOne.id, childOne.id, root.id),
            resultBalanceDesc.aggregates.map { a -> a.serviceId }
        )

        val resultBalanceAsc = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 100, RankSubtreeSortingParamsDto(
                BALANCE,
                ASC
            ), null, null
        )!!
        Assertions.assertEquals(
            listOf(root.id, childOne.id, grandchildOne.id, childTwo.id, grandchildTwo.id),
            resultBalanceAsc.aggregates.map { a -> a.serviceId }
        )

        val resultProvidedDesc = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 100, RankSubtreeSortingParamsDto(
                PROVIDED,
                DESC
            ), null, null
        )!!
        Assertions.assertEquals(
            listOf(grandchildTwo.id, childTwo.id, grandchildOne.id, childOne.id, root.id),
            resultProvidedDesc.aggregates.map { a -> a.serviceId }
        )

        val resultProvidedAsc = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 100, RankSubtreeSortingParamsDto(
                PROVIDED,
                ASC
            ), null, null
        )!!
        Assertions.assertEquals(
            listOf(root.id, childOne.id, grandchildOne.id, childTwo.id, grandchildTwo.id),
            resultProvidedAsc.aggregates.map { a -> a.serviceId }
        )

        val resultQuotaDesc = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 100, RankSubtreeSortingParamsDto(
                QUOTA,
                DESC
            ), null, null
        )!!
        Assertions.assertEquals(
            listOf(grandchildTwo.id, childTwo.id, grandchildOne.id, childOne.id, root.id),
            resultQuotaDesc.aggregates.map { a -> a.serviceId }
        )

        val resultQuotaAsc = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 100, RankSubtreeSortingParamsDto(
                QUOTA,
                ASC
            ), null, null
        )!!
        Assertions.assertEquals(
            listOf(root.id, childOne.id, grandchildOne.id, childTwo.id, grandchildTwo.id),
            resultQuotaAsc.aggregates.map { a -> a.serviceId }
        )

        val resultUnallocatedDesc = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 100, RankSubtreeSortingParamsDto(
                UNALLOCATED,
                DESC
            ), null, null
        )!!
        Assertions.assertEquals(
            listOf(grandchildTwo.id, childTwo.id, grandchildOne.id, childOne.id, root.id),
            resultUnallocatedDesc.aggregates.map { a -> a.serviceId }
        )

        val resultUnallocatedAsc = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 100, RankSubtreeSortingParamsDto(
                UNALLOCATED,
                ASC
            ), null, null
        )!!
        Assertions.assertEquals(
            listOf(root.id, childOne.id, grandchildOne.id, childTwo.id, grandchildTwo.id),
            resultUnallocatedAsc.aggregates.map { a -> a.serviceId }
        )
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
                    extUsage = null,
                    unusedEst = null,
                    underutilizedEst = null
                )
            )
        ) }
        dbSessionRetryable(tableClient) { rwTxRetryable { serviceDenormalizedAggregatesDao.upsertManyRetryable(txSession, aggregates) }}
        val resultPageOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsApiRequestDto(rootServiceId = root.id, resourceId = resource.id,
                from = null, limit = 100, providerId = provider.id, sortingParams = null, includeUsage = null, includeUsageRaw = null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsApiResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(100, resultPageOne.aggregates.size)
        Assertions.assertNotNull(resultPageOne.nextPageToken)
        val resultPageTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsApiRequestDto(rootServiceId = root.id, resourceId = resource.id,
                from = resultPageOne.nextPageToken, limit = 100, providerId = provider.id, sortingParams = null,
                includeUsage = null, includeUsageRaw = null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsApiResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(100, resultPageTwo.aggregates.size)
        Assertions.assertNotNull(resultPageTwo.nextPageToken)
        val resultPageThree = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/aggregation/_rankSubtreeAmounts")
            .bodyValue(RankSubtreeAmountsApiRequestDto(rootServiceId = root.id, resourceId = resource.id,
                from = resultPageTwo.nextPageToken, limit = 100, providerId = provider.id, sortingParams = null,
                includeUsage = null, includeUsageRaw = null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(RankSubtreeAmountsApiResponseDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(50, resultPageThree.aggregates.size)
        Assertions.assertNull(resultPageThree.nextPageToken)
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
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent

        val resultAllocatedDescFirstPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 3,
            RankSubtreeSortingParamsDto(ALLOCATED, DESC), null, null
        )!!
        Assertions.assertEquals(
            listOf(grandchildTwo.id, childTwo.id, grandchildOne.id),
            resultAllocatedDescFirstPage.aggregates.map { a -> a.serviceId }
        )
        val resultAllocatedDescSecondPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, resultAllocatedDescFirstPage.nextPageToken, limit = 3,
            RankSubtreeSortingParamsDto(ALLOCATED, DESC), null, null
        )!!
        Assertions.assertEquals(
            listOf(childOne.id, root.id),
            resultAllocatedDescSecondPage.aggregates.map { a -> a.serviceId }
        )

        val resultAllocatedAscFirstPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 3,
            RankSubtreeSortingParamsDto(ALLOCATED, ASC), null, null
        )!!
        Assertions.assertEquals(
            listOf(root.id, childOne.id, grandchildOne.id),
            resultAllocatedAscFirstPage.aggregates.map { a -> a.serviceId }
        )

        val resultAllocatedAscSecondPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, resultAllocatedAscFirstPage.nextPageToken, limit = 3,
            RankSubtreeSortingParamsDto(ALLOCATED, ASC), null, null
        )!!
        Assertions.assertEquals(
            listOf(childTwo.id, grandchildTwo.id),
            resultAllocatedAscSecondPage.aggregates.map { a -> a.serviceId }
        )

        val resultBalanceDescFirstPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 3,
            RankSubtreeSortingParamsDto(BALANCE, DESC), null, null
        )!!
        Assertions.assertEquals(
            listOf(grandchildTwo.id, childTwo.id, grandchildOne.id),
            resultBalanceDescFirstPage.aggregates.map { a -> a.serviceId }
        )

        val resultBalanceDescSecondPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, resultBalanceDescFirstPage.nextPageToken, limit = 3,
            RankSubtreeSortingParamsDto(BALANCE, DESC), null, null
        )!!
        Assertions.assertEquals(
            listOf(childOne.id, root.id),
            resultBalanceDescSecondPage.aggregates.map { a -> a.serviceId }
        )

        val resultBalanceAscFirstPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 3,
            RankSubtreeSortingParamsDto(BALANCE, ASC), null, null
        )!!
        Assertions.assertEquals(
            listOf(root.id, childOne.id, grandchildOne.id),
            resultBalanceAscFirstPage.aggregates.map { a -> a.serviceId }
        )

        val resultBalanceAscSecondPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, resultBalanceAscFirstPage.nextPageToken, limit = 3,
            RankSubtreeSortingParamsDto(BALANCE, ASC), null, null
        )!!
        Assertions.assertEquals(
            listOf(childTwo.id, grandchildTwo.id),
            resultBalanceAscSecondPage.aggregates.map { a -> a.serviceId }
        )

        val resultQuotaDescFirstPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 3,
            RankSubtreeSortingParamsDto(QUOTA, DESC), null, null
        )!!
        Assertions.assertEquals(
            listOf(grandchildTwo.id, childTwo.id, grandchildOne.id),
            resultQuotaDescFirstPage.aggregates.map { a -> a.serviceId }
        )

        val resultQuotaDescSecondPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, resultQuotaDescFirstPage.nextPageToken, limit = 3,
            RankSubtreeSortingParamsDto(QUOTA, DESC), null, null
        )!!
        Assertions.assertEquals(
            listOf(childOne.id, root.id),
            resultQuotaDescSecondPage.aggregates.map { a -> a.serviceId }
        )

        val resultQuotaAscFirstPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 3,
            RankSubtreeSortingParamsDto(QUOTA, ASC), null, null
        )!!
        Assertions.assertEquals(
            listOf(root.id, childOne.id, grandchildOne.id),
            resultQuotaAscFirstPage.aggregates.map { a -> a.serviceId }
        )

        val resultQuotaAscSecondPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, resultQuotaAscFirstPage.nextPageToken, limit = 3,
            RankSubtreeSortingParamsDto(QUOTA, ASC), null, null
        )!!
        Assertions.assertEquals(
            listOf(childTwo.id, grandchildTwo.id),
            resultQuotaAscSecondPage.aggregates.map { a -> a.serviceId }
        )

        val resultProvidedDescFirstPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 3,
            RankSubtreeSortingParamsDto(PROVIDED, DESC), null, null
        )!!
        Assertions.assertEquals(
            listOf(grandchildTwo.id, childTwo.id, grandchildOne.id),
            resultProvidedDescFirstPage.aggregates.map { a -> a.serviceId }
        )

        val resultProvidedDescSecondPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, resultProvidedDescFirstPage.nextPageToken, limit = 3,
            RankSubtreeSortingParamsDto(PROVIDED, DESC), null, null
        )!!
        Assertions.assertEquals(
            listOf(childOne.id, root.id),
            resultProvidedDescSecondPage.aggregates.map { a -> a.serviceId }
        )

        val resultProvidedAscFirstPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 3,
            RankSubtreeSortingParamsDto(PROVIDED, ASC), null, null
        )!!
        Assertions.assertEquals(
            listOf(root.id, childOne.id, grandchildOne.id),
            resultProvidedAscFirstPage.aggregates.map { a -> a.serviceId }
        )

        val resultProvidedAscSecondPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, resultProvidedAscFirstPage.nextPageToken, limit = 3,
            RankSubtreeSortingParamsDto(PROVIDED, ASC), null, null
        )!!
        Assertions.assertEquals(
            listOf(childTwo.id, grandchildTwo.id),
            resultProvidedAscSecondPage.aggregates.map { a -> a.serviceId }
        )

        val resultUnallocatedDescFirstPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 3,
            RankSubtreeSortingParamsDto(UNALLOCATED, DESC), null, null
        )!!
        Assertions.assertEquals(
            listOf(grandchildTwo.id, childTwo.id, grandchildOne.id),
            resultUnallocatedDescFirstPage.aggregates.map { a -> a.serviceId }
        )

        val resultUnallocatedDescSecondPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, resultUnallocatedDescFirstPage.nextPageToken, limit = 3,
            RankSubtreeSortingParamsDto(UNALLOCATED, DESC), null, null
        )!!
        Assertions.assertEquals(
            listOf(childOne.id, root.id),
            resultUnallocatedDescSecondPage.aggregates.map { a -> a.serviceId }
        )

        val resultUnallocatedAscFirstPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 3,
            RankSubtreeSortingParamsDto(UNALLOCATED, ASC), null, null
        )!!
        Assertions.assertEquals(
            listOf(root.id, childOne.id, grandchildOne.id),
            resultUnallocatedAscFirstPage.aggregates.map { a -> a.serviceId }
        )

        val resultUnallocatedAscSecondPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, resultUnallocatedAscFirstPage.nextPageToken, limit = 3,
            RankSubtreeSortingParamsDto(UNALLOCATED, ASC), null, null
        )!!
        Assertions.assertEquals(
            listOf(childTwo.id, grandchildTwo.id),
            resultUnallocatedAscSecondPage.aggregates.map { a -> a.serviceId }
        )

        val resultTransferableDescFirstPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 3,
            RankSubtreeSortingParamsDto(TRANSFERABLE, DESC), null, null
        )!!
        Assertions.assertEquals(
            listOf(grandchildTwo.id, childTwo.id, grandchildOne.id),
            resultTransferableDescFirstPage.aggregates.map { a -> a.serviceId }
        )

        val resultTransferableDescSecondPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, resultTransferableDescFirstPage.nextPageToken, limit = 3,
            RankSubtreeSortingParamsDto(TRANSFERABLE, DESC), null, null
        )!!
        Assertions.assertEquals(
            listOf(childOne.id, root.id),
            resultTransferableDescSecondPage.aggregates.map { a -> a.serviceId }
        )

        val resultTransferableAscFirstPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 3,
            RankSubtreeSortingParamsDto(TRANSFERABLE, ASC), null, null
        )!!
        Assertions.assertEquals(
            listOf(root.id, childOne.id, grandchildOne.id),
            resultTransferableAscFirstPage.aggregates.map { a -> a.serviceId }
        )

        val resultTransferableAscSecondPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, resultTransferableAscFirstPage.nextPageToken, limit = 3,
            RankSubtreeSortingParamsDto(TRANSFERABLE, ASC), null, null
        )!!
        Assertions.assertEquals(
            listOf(childTwo.id, grandchildTwo.id),
            resultTransferableAscSecondPage.aggregates.map { a -> a.serviceId }
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
            .aggregationSettings(AggregationSettings(aggregationMode, null, null))
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

}
