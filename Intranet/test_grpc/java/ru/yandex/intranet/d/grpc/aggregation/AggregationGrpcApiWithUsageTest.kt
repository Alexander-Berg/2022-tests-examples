package ru.yandex.intranet.d.grpc.aggregation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.devh.boot.grpc.client.inject.GrpcClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.UnitIds
import ru.yandex.intranet.d.UnitsEnsembleIds
import ru.yandex.intranet.d.backend.service.proto.AggregatesLimit
import ru.yandex.intranet.d.backend.service.proto.AggregatesPageToken
import ru.yandex.intranet.d.backend.service.proto.AggregationQueriesServiceGrpc
import ru.yandex.intranet.d.backend.service.proto.FindSubtreeTotalRequest
import ru.yandex.intranet.d.backend.service.proto.RankSubtreeAmountsRequest
import ru.yandex.intranet.d.backend.service.proto.RankSubtreeSortingField
import ru.yandex.intranet.d.backend.service.proto.RankSubtreeSortingOrder
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
import ru.yandex.intranet.d.dao.usage.ServiceUsageDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.grpc.MockGrpcUser
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
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel
import ru.yandex.intranet.d.model.services.ServiceRecipeModel
import ru.yandex.intranet.d.model.services.ServiceState
import ru.yandex.intranet.d.model.usage.ServiceUsageAmounts
import ru.yandex.intranet.d.model.usage.ServiceUsageKey
import ru.yandex.intranet.d.model.usage.ServiceUsageModel
import ru.yandex.intranet.d.model.usage.UsageAmount
import ru.yandex.intranet.d.model.usage.UsagePoint
import ru.yandex.intranet.d.services.usage.accumulate
import ru.yandex.intranet.d.services.usage.histogram
import ru.yandex.intranet.d.services.usage.mean
import ru.yandex.intranet.d.services.usage.minMedianMax
import ru.yandex.intranet.d.services.usage.roundToIntegerHalfUp
import ru.yandex.intranet.d.services.usage.variance
import ru.yandex.intranet.d.web.MockUser
import java.math.BigInteger
import java.time.Instant
import java.util.*

/**
 * Aggregation with usage GRPC API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class AggregationGrpcApiWithUsageTest(
    @Autowired private val serviceDenormalizedAggregatesDao: ServiceDenormalizedAggregatesDao,
    @Autowired private val providersDao: ProvidersDao,
    @Autowired private val resourceTypesDao: ResourceTypesDao,
    @Autowired private val resourcesDao: ResourcesDao,
    @Autowired private val servicesDao: ServicesDao,
    @Autowired private val folderDao: FolderDao,
    @Autowired private val quotasDao: QuotasDao,
    @Autowired private val accountsDao: AccountsDao,
    @Autowired private val accountsQuotasDao: AccountsQuotasDao,
    @Autowired private val serviceUsageDao: ServiceUsageDao,
    @Autowired private val tableClient: YdbTableClient,
    @Autowired private val webClient: WebTestClient,
    @Autowired private val helper: AggregationGrpcApiTestHelper
) {

    @GrpcClient("inProcess")
    private lateinit var aggregationService: AggregationQueriesServiceGrpc.AggregationQueriesServiceBlockingStub

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
        withContext(Dispatchers.IO) {
            aggregationService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .findSubtreeTotal(
                    FindSubtreeTotalRequest.newBuilder()
                        .setRootServiceId(root.id)
                        .setResourceId(resource.id)
                        .setProviderId(provider.id)
                        .setIncludeUsage(true)
                        .build()
                )
        }
        val resultRoot = withContext(Dispatchers.IO) {
            aggregationService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .rankSubtreeAmounts(
                    RankSubtreeAmountsRequest.newBuilder()
                        .setRootServiceId(root.id)
                        .setResourceId(resource.id)
                        .setProviderId(provider.id)
                        .setIncludeUsage(true)
                        .build()
                )
        }
        Assertions.assertEquals(0, resultRoot.aggregatesList.size)
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
        val resultRoot = withContext(Dispatchers.IO) {
            aggregationService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .findSubtreeTotal(
                    FindSubtreeTotalRequest.newBuilder()
                        .setRootServiceId(root.id)
                        .setResourceId(resource.id)
                        .setProviderId(provider.id)
                        .setIncludeUsage(true)
                        .build()
                )
        }
        Assertions.assertEquals("190", resultRoot.aggregate.amounts.quota?.amount)
        Assertions.assertEquals("95", resultRoot.aggregate.amounts.balance?.amount)
        Assertions.assertEquals("95", resultRoot.aggregate.amounts.provided?.amount)
        Assertions.assertEquals("38", resultRoot.aggregate.amounts.allocated?.amount)
        Assertions.assertEquals("57", resultRoot.aggregate.amounts.unallocated?.amount)
        Assertions.assertEquals("152", resultRoot.aggregate.amounts.transferable?.amount)
        Assertions.assertTrue(resultRoot.aggregate.hasUsage())
        val resultChildOne = withContext(Dispatchers.IO) {
            aggregationService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .findSubtreeTotal(
                    FindSubtreeTotalRequest.newBuilder()
                        .setRootServiceId(childOne.id)
                        .setResourceId(resource.id)
                        .setProviderId(provider.id)
                        .setIncludeUsage(true)
                        .build()
                )
        }
        Assertions.assertEquals("60", resultChildOne.aggregate.amounts.quota?.amount)
        Assertions.assertEquals("30", resultChildOne.aggregate.amounts.balance?.amount)
        Assertions.assertEquals("30", resultChildOne.aggregate.amounts.provided?.amount)
        Assertions.assertEquals("12", resultChildOne.aggregate.amounts.allocated?.amount)
        Assertions.assertEquals("18", resultChildOne.aggregate.amounts.unallocated?.amount)
        Assertions.assertEquals("48", resultChildOne.aggregate.amounts.transferable?.amount)
        Assertions.assertTrue(resultChildOne.aggregate.hasUsage())
        val resultChildTwo = withContext(Dispatchers.IO) {
            aggregationService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .findSubtreeTotal(
                    FindSubtreeTotalRequest.newBuilder()
                        .setRootServiceId(childTwo.id)
                        .setResourceId(resource.id)
                        .setProviderId(provider.id)
                        .setIncludeUsage(true)
                        .build()
                )
        }
        Assertions.assertEquals("120", resultChildTwo.aggregate.amounts.quota?.amount)
        Assertions.assertEquals("60", resultChildTwo.aggregate.amounts.balance?.amount)
        Assertions.assertEquals("60", resultChildTwo.aggregate.amounts.provided?.amount)
        Assertions.assertEquals("24", resultChildTwo.aggregate.amounts.allocated?.amount)
        Assertions.assertEquals("36", resultChildTwo.aggregate.amounts.unallocated?.amount)
        Assertions.assertEquals("96", resultChildTwo.aggregate.amounts.transferable?.amount)
        Assertions.assertTrue(resultChildTwo.aggregate.hasUsage())
        val resultGrandchildOne = withContext(Dispatchers.IO) {
            aggregationService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .findSubtreeTotal(
                    FindSubtreeTotalRequest.newBuilder()
                        .setRootServiceId(grandchildOne.id)
                        .setResourceId(resource.id)
                        .setProviderId(provider.id)
                        .setIncludeUsage(true)
                        .build()
                )
        }
        Assertions.assertEquals("30", resultGrandchildOne.aggregate.amounts.quota?.amount)
        Assertions.assertEquals("15", resultGrandchildOne.aggregate.amounts.balance?.amount)
        Assertions.assertEquals("15", resultGrandchildOne.aggregate.amounts.provided?.amount)
        Assertions.assertEquals("6", resultGrandchildOne.aggregate.amounts.allocated?.amount)
        Assertions.assertEquals("9", resultGrandchildOne.aggregate.amounts.unallocated?.amount)
        Assertions.assertEquals("24", resultGrandchildOne.aggregate.amounts.transferable?.amount)
        Assertions.assertTrue(resultGrandchildOne.aggregate.hasUsage())
        val resultGrandchildTwo = withContext(Dispatchers.IO) {
            aggregationService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .findSubtreeTotal(
                    FindSubtreeTotalRequest.newBuilder()
                        .setRootServiceId(grandchildTwo.id)
                        .setResourceId(resource.id)
                        .setProviderId(provider.id)
                        .setIncludeUsage(true)
                        .build()
                )
        }
        Assertions.assertEquals("60", resultGrandchildTwo.aggregate.amounts.quota?.amount)
        Assertions.assertEquals("30", resultGrandchildTwo.aggregate.amounts.balance?.amount)
        Assertions.assertEquals("30", resultGrandchildTwo.aggregate.amounts.provided?.amount)
        Assertions.assertEquals("12", resultGrandchildTwo.aggregate.amounts.allocated?.amount)
        Assertions.assertEquals("18", resultGrandchildTwo.aggregate.amounts.unallocated?.amount)
        Assertions.assertEquals("48", resultGrandchildTwo.aggregate.amounts.transferable?.amount)
        Assertions.assertTrue(resultGrandchildTwo.aggregate.hasUsage())
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
        val resultRoot = withContext(Dispatchers.IO) {
            aggregationService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .rankSubtreeAmounts(
                    RankSubtreeAmountsRequest.newBuilder()
                        .setRootServiceId(root.id)
                        .setResourceId(resource.id)
                        .setProviderId(provider.id)
                        .setIncludeUsage(true)
                        .build()
                )
        }
        Assertions.assertEquals(5, resultRoot.aggregatesList.size)
        Assertions.assertEquals(grandchildTwo.id, resultRoot.aggregatesList[0].serviceId)
        Assertions.assertEquals("60", resultRoot.aggregatesList[0].amounts.quota?.amount)
        Assertions.assertEquals("30", resultRoot.aggregatesList[0].amounts.balance?.amount)
        Assertions.assertEquals("30", resultRoot.aggregatesList[0].amounts.provided?.amount)
        Assertions.assertEquals("12", resultRoot.aggregatesList[0].amounts.allocated?.amount)
        Assertions.assertEquals("18", resultRoot.aggregatesList[0].amounts.unallocated?.amount)
        Assertions.assertEquals("48", resultRoot.aggregatesList[0].amounts.transferable?.amount)
        Assertions.assertTrue(resultRoot.aggregatesList[0].hasUsage())
        Assertions.assertEquals(childTwo.id, resultRoot.aggregatesList[1].serviceId)
        Assertions.assertEquals("60", resultRoot.aggregatesList[1].amounts.quota?.amount)
        Assertions.assertEquals("30", resultRoot.aggregatesList[1].amounts.balance?.amount)
        Assertions.assertEquals("30", resultRoot.aggregatesList[1].amounts.provided?.amount)
        Assertions.assertEquals("12", resultRoot.aggregatesList[1].amounts.allocated?.amount)
        Assertions.assertEquals("18", resultRoot.aggregatesList[1].amounts.unallocated?.amount)
        Assertions.assertEquals("48", resultRoot.aggregatesList[1].amounts.transferable?.amount)
        Assertions.assertTrue(resultRoot.aggregatesList[1].hasUsage())
        Assertions.assertEquals(grandchildOne.id, resultRoot.aggregatesList[2].serviceId)
        Assertions.assertEquals("30", resultRoot.aggregatesList[2].amounts.quota?.amount)
        Assertions.assertEquals("15", resultRoot.aggregatesList[2].amounts.balance?.amount)
        Assertions.assertEquals("15", resultRoot.aggregatesList[2].amounts.provided?.amount)
        Assertions.assertEquals("6", resultRoot.aggregatesList[2].amounts.allocated?.amount)
        Assertions.assertEquals("9", resultRoot.aggregatesList[2].amounts.unallocated?.amount)
        Assertions.assertEquals("24", resultRoot.aggregatesList[2].amounts.transferable?.amount)
        Assertions.assertTrue(resultRoot.aggregatesList[2].hasUsage())
        Assertions.assertEquals(childOne.id, resultRoot.aggregatesList[3].serviceId)
        Assertions.assertEquals("30", resultRoot.aggregatesList[3].amounts.quota?.amount)
        Assertions.assertEquals("15", resultRoot.aggregatesList[3].amounts.balance?.amount)
        Assertions.assertEquals("15", resultRoot.aggregatesList[3].amounts.provided?.amount)
        Assertions.assertEquals("6", resultRoot.aggregatesList[3].amounts.allocated?.amount)
        Assertions.assertEquals("9", resultRoot.aggregatesList[3].amounts.unallocated?.amount)
        Assertions.assertEquals("24", resultRoot.aggregatesList[3].amounts.transferable?.amount)
        Assertions.assertTrue(resultRoot.aggregatesList[3].hasUsage())
        Assertions.assertEquals(root.id, resultRoot.aggregatesList[4].serviceId)
        Assertions.assertEquals("10", resultRoot.aggregatesList[4].amounts.quota?.amount)
        Assertions.assertEquals("5", resultRoot.aggregatesList[4].amounts.balance?.amount)
        Assertions.assertEquals("5", resultRoot.aggregatesList[4].amounts.provided?.amount)
        Assertions.assertEquals("2", resultRoot.aggregatesList[4].amounts.allocated?.amount)
        Assertions.assertEquals("3", resultRoot.aggregatesList[4].amounts.unallocated?.amount)
        Assertions.assertEquals("8", resultRoot.aggregatesList[4].amounts.transferable?.amount)
        Assertions.assertTrue(resultRoot.aggregatesList[4].hasUsage())
        val resultChildOne = withContext(Dispatchers.IO) {
            aggregationService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .rankSubtreeAmounts(
                    RankSubtreeAmountsRequest.newBuilder()
                        .setRootServiceId(childOne.id)
                        .setResourceId(resource.id)
                        .setProviderId(provider.id)
                        .setIncludeUsage(true)
                        .build()
                )
        }
        Assertions.assertEquals(2, resultChildOne.aggregatesList.size)
        Assertions.assertEquals(grandchildOne.id, resultChildOne.aggregatesList[0].serviceId)
        Assertions.assertEquals("30", resultChildOne.aggregatesList[0].amounts.quota?.amount)
        Assertions.assertEquals("15", resultChildOne.aggregatesList[0].amounts.balance?.amount)
        Assertions.assertEquals("15", resultChildOne.aggregatesList[0].amounts.provided?.amount)
        Assertions.assertEquals("6", resultChildOne.aggregatesList[0].amounts.allocated?.amount)
        Assertions.assertEquals("9", resultChildOne.aggregatesList[0].amounts.unallocated?.amount)
        Assertions.assertEquals("24", resultChildOne.aggregatesList[0].amounts.transferable?.amount)
        Assertions.assertTrue(resultChildOne.aggregatesList[0].hasUsage())
        Assertions.assertEquals(childOne.id, resultChildOne.aggregatesList[1].serviceId)
        Assertions.assertEquals("30", resultChildOne.aggregatesList[1].amounts.quota?.amount)
        Assertions.assertEquals("15", resultChildOne.aggregatesList[1].amounts.balance?.amount)
        Assertions.assertEquals("15", resultChildOne.aggregatesList[1].amounts.provided?.amount)
        Assertions.assertEquals("6", resultChildOne.aggregatesList[1].amounts.allocated?.amount)
        Assertions.assertEquals("9", resultChildOne.aggregatesList[1].amounts.unallocated?.amount)
        Assertions.assertEquals("24", resultChildOne.aggregatesList[1].amounts.transferable?.amount)
        Assertions.assertTrue(resultChildOne.aggregatesList[1].hasUsage())

        val resultChildTwo = withContext(Dispatchers.IO) {
            aggregationService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .rankSubtreeAmounts(
                    RankSubtreeAmountsRequest.newBuilder()
                        .setRootServiceId(childTwo.id)
                        .setResourceId(resource.id)
                        .setProviderId(provider.id)
                        .setIncludeUsage(true)
                        .build()
                )
        }
        Assertions.assertEquals(2, resultChildTwo.aggregatesList.size)
        Assertions.assertEquals(grandchildTwo.id, resultChildTwo.aggregatesList[0].serviceId)
        Assertions.assertEquals("60", resultChildTwo.aggregatesList[0].amounts.quota?.amount)
        Assertions.assertEquals("30", resultChildTwo.aggregatesList[0].amounts.balance?.amount)
        Assertions.assertEquals("30", resultChildTwo.aggregatesList[0].amounts.provided?.amount)
        Assertions.assertEquals("12", resultChildTwo.aggregatesList[0].amounts.allocated?.amount)
        Assertions.assertEquals("18", resultChildTwo.aggregatesList[0].amounts.unallocated?.amount)
        Assertions.assertEquals("48", resultChildTwo.aggregatesList[0].amounts.transferable?.amount)
        Assertions.assertTrue(resultChildTwo.aggregatesList[0].hasUsage())
        Assertions.assertEquals(childTwo.id, resultChildTwo.aggregatesList[1].serviceId)
        Assertions.assertEquals("60", resultChildTwo.aggregatesList[1].amounts.quota?.amount)
        Assertions.assertEquals("30", resultChildTwo.aggregatesList[1].amounts.balance?.amount)
        Assertions.assertEquals("30", resultChildTwo.aggregatesList[1].amounts.provided?.amount)
        Assertions.assertEquals("12", resultChildTwo.aggregatesList[1].amounts.allocated?.amount)
        Assertions.assertEquals("18", resultChildTwo.aggregatesList[1].amounts.unallocated?.amount)
        Assertions.assertEquals("48", resultChildTwo.aggregatesList[1].amounts.transferable?.amount)
        Assertions.assertTrue(resultChildTwo.aggregatesList[1].hasUsage())
        val resultGrandchildOne = withContext(Dispatchers.IO) {
            aggregationService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .rankSubtreeAmounts(
                    RankSubtreeAmountsRequest.newBuilder()
                        .setRootServiceId(grandchildOne.id)
                        .setResourceId(resource.id)
                        .setProviderId(provider.id)
                        .setIncludeUsage(true)
                        .build()
                )
        }
        Assertions.assertEquals(1, resultGrandchildOne.aggregatesList.size)
        Assertions.assertEquals(grandchildOne.id, resultGrandchildOne.aggregatesList[0].serviceId)
        Assertions.assertEquals("30", resultGrandchildOne.aggregatesList[0].amounts.quota?.amount)
        Assertions.assertEquals("15", resultGrandchildOne.aggregatesList[0].amounts.balance?.amount)
        Assertions.assertEquals("15", resultGrandchildOne.aggregatesList[0].amounts.provided?.amount)
        Assertions.assertEquals("6", resultGrandchildOne.aggregatesList[0].amounts.allocated?.amount)
        Assertions.assertEquals("9", resultGrandchildOne.aggregatesList[0].amounts.unallocated?.amount)
        Assertions.assertEquals("24", resultGrandchildOne.aggregatesList[0].amounts.transferable?.amount)
        Assertions.assertTrue(resultGrandchildOne.aggregatesList[0].hasUsage())
        val resultGrandchildTwo = withContext(Dispatchers.IO) {
            aggregationService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .rankSubtreeAmounts(
                    RankSubtreeAmountsRequest.newBuilder()
                        .setRootServiceId(grandchildTwo.id)
                        .setResourceId(resource.id)
                        .setProviderId(provider.id)
                        .setIncludeUsage(true)
                        .build()
                )
        }
        Assertions.assertEquals(1, resultGrandchildTwo.aggregatesList.size)
        Assertions.assertEquals(grandchildTwo.id, resultGrandchildTwo.aggregatesList[0].serviceId)
        Assertions.assertEquals("60", resultGrandchildTwo.aggregatesList[0].amounts.quota?.amount)
        Assertions.assertEquals("30", resultGrandchildTwo.aggregatesList[0].amounts.balance?.amount)
        Assertions.assertEquals("30", resultGrandchildTwo.aggregatesList[0].amounts.provided?.amount)
        Assertions.assertEquals("12", resultGrandchildTwo.aggregatesList[0].amounts.allocated?.amount)
        Assertions.assertEquals("18", resultGrandchildTwo.aggregatesList[0].amounts.unallocated?.amount)
        Assertions.assertEquals("48", resultGrandchildTwo.aggregatesList[0].amounts.transferable?.amount)
        Assertions.assertTrue(resultGrandchildTwo.aggregatesList[0].hasUsage())
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
        val resultPageOne = withContext(Dispatchers.IO) {
            aggregationService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .rankSubtreeAmounts(
                    RankSubtreeAmountsRequest.newBuilder()
                        .setRootServiceId(root.id)
                        .setResourceId(resource.id)
                        .setProviderId(provider.id)
                        .setLimit(AggregatesLimit.newBuilder().setLimit(100).build())
                        .setIncludeUsage(true)
                        .build()
                )
        }
        Assertions.assertEquals(100, resultPageOne.aggregatesList.size)
        Assertions.assertTrue(resultPageOne.hasNext())
        val resultPageTwo = withContext(Dispatchers.IO) {
            aggregationService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .rankSubtreeAmounts(
                    RankSubtreeAmountsRequest.newBuilder()
                        .setRootServiceId(root.id)
                        .setResourceId(resource.id)
                        .setProviderId(provider.id)
                        .setLimit(AggregatesLimit.newBuilder().setLimit(100).build())
                        .setFrom(AggregatesPageToken.newBuilder().setToken(resultPageOne.next.token))
                        .setIncludeUsage(true)
                        .build()
                )
        }
        Assertions.assertEquals(100, resultPageTwo.aggregatesList.size)
        Assertions.assertTrue(resultPageTwo.hasNext())
        val resultPageThree = withContext(Dispatchers.IO) {
            aggregationService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .rankSubtreeAmounts(
                    RankSubtreeAmountsRequest.newBuilder()
                        .setRootServiceId(root.id)
                        .setResourceId(resource.id)
                        .setProviderId(provider.id)
                        .setLimit(AggregatesLimit.newBuilder().setLimit(100).build())
                        .setFrom(AggregatesPageToken.newBuilder().setToken(resultPageTwo.next.token))
                        .setIncludeUsage(true)
                        .build()
                )
        }
        Assertions.assertEquals(50, resultPageThree.aggregatesList.size)
        Assertions.assertFalse(resultPageThree.hasNext())
    }

    @Test
    fun testRankSubtreeCustomSort(): Unit = runBlocking {
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
            .isNoContent
        val resultAllocatedDesc = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = null,
            RankSubtreeSortingField.UNDERUTILIZED_ESTIMATION, RankSubtreeSortingOrder.DESC)
        Assertions.assertEquals(
            listOf(grandchildTwo.id, childTwo.id, grandchildOne.id, childOne.id, root.id),
            resultAllocatedDesc.aggregatesList.map { a -> a.serviceId }
        )
        val resultAllocatedAsc = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = null,
            RankSubtreeSortingField.UNDERUTILIZED_ESTIMATION, RankSubtreeSortingOrder.ASC)
        Assertions.assertEquals(
            listOf(root.id, childOne.id, grandchildOne.id, childTwo.id, grandchildTwo.id),
            resultAllocatedAsc.aggregatesList.map { a -> a.serviceId }
        )
    }

    @Test
    fun testRankSubtreeCustomSortPaging(): Unit = runBlocking {
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
            .isNoContent

        val resultAllocatedDescFirstPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 3,
            RankSubtreeSortingField.UNDERUTILIZED_ESTIMATION, RankSubtreeSortingOrder.DESC)
        Assertions.assertEquals(
            listOf(grandchildTwo.id, childTwo.id, grandchildOne.id),
            resultAllocatedDescFirstPage.aggregatesList.map { a -> a.serviceId }
        )
        val resultAllocatedDescSecondPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, resultAllocatedDescFirstPage.next.token, limit = 3,
            RankSubtreeSortingField.UNDERUTILIZED_ESTIMATION, RankSubtreeSortingOrder.DESC)
        Assertions.assertEquals(
            listOf(childOne.id, root.id),
            resultAllocatedDescSecondPage.aggregatesList.map { a -> a.serviceId }
        )
        val resultAllocatedAscFirstPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, from = null, limit = 3,
            RankSubtreeSortingField.UNDERUTILIZED_ESTIMATION, RankSubtreeSortingOrder.ASC)
        Assertions.assertEquals(
            listOf(root.id, childOne.id, grandchildOne.id),
            resultAllocatedAscFirstPage.aggregatesList.map { a -> a.serviceId }
        )
        val resultAllocatedAscSecondPage = helper.rankSubtreeAmounts(
            root.id, resource.id, provider.id, resultAllocatedAscFirstPage.next.token, limit = 3,
            RankSubtreeSortingField.UNDERUTILIZED_ESTIMATION, RankSubtreeSortingOrder.ASC)
        Assertions.assertEquals(
            listOf(childTwo.id, grandchildTwo.id),
            resultAllocatedAscSecondPage.aggregatesList.map { a -> a.serviceId }
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

    private fun serviceUsageModel(serviceId: Long, providerId: String, resourceId: String,
                                  intervalStart: Instant, ownTimeSeries: Map<Long, Long>?,
                                  subtreeTimeSeries: Map<Long, Long>?, totalTimeSeries: Map<Long, Long>?
    ): ServiceUsageModel {
        return ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = serviceId,
                providerId = providerId,
                resourceId = resourceId
            ),
            lastUpdate = Instant.now(),
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = if (ownTimeSeries != null) { usageAmount(ownTimeSeries, intervalStart) } else { null },
                subtree = if (subtreeTimeSeries != null) { usageAmount(subtreeTimeSeries, intervalStart) } else { null },
                total = if (totalTimeSeries != null) { usageAmount(totalTimeSeries, intervalStart) } else { null }
            )
        )
    }

    private fun usageAmount(timeSeries: Map<Long, Long>, intervalStart: Instant): UsageAmount {
        val series = mutableMapOf<Long, BigInteger>()
        timeSeries.forEach { (k, v) ->
            series[k + intervalStart.epochSecond] = BigInteger.valueOf(v * 1000)
        }
        val average = mean(series.values)
        val minMedianMax = minMedianMax(series.values)
        val variance = variance(series.values, average)
        val accumulated = accumulate(series, 300)
        val histogram = histogram(series.values, minMedianMax.first, minMedianMax.third)
        val sortedEntries = series.entries.sortedBy { it.key }
        val values = sortedEntries.map { UsagePoint(it.key, it.value, null) }
        val valuesX = sortedEntries.map { it.key }
        val valuesY = sortedEntries.map { it.value }
        return UsageAmount(
            value = null,
            average = roundToIntegerHalfUp(average),
            min = minMedianMax.first,
            max = minMedianMax.third,
            median = roundToIntegerHalfUp(minMedianMax.second),
            variance = roundToIntegerHalfUp(variance),
            accumulated = roundToIntegerHalfUp(accumulated.first),
            accumulatedDuration = accumulated.second,
            histogram = histogram,
            values = values,
            valuesX = valuesX,
            valuesY = valuesY,
            unused = null
        )
    }

}
