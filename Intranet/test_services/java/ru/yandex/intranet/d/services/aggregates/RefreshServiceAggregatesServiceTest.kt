package ru.yandex.intranet.d.services.aggregates

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
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
import ru.yandex.intranet.d.dao.accounts.ProviderReserveAccountsDao
import ru.yandex.intranet.d.dao.aggregates.ServiceAggregateUsageDao
import ru.yandex.intranet.d.dao.aggregates.ServiceAggregatesDao
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
import ru.yandex.intranet.d.model.WithTenant
import ru.yandex.intranet.d.model.accounts.AccountModel
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel
import ru.yandex.intranet.d.model.accounts.ProviderReserveAccountKey
import ru.yandex.intranet.d.model.accounts.ProviderReserveAccountModel
import ru.yandex.intranet.d.model.aggregates.*
import ru.yandex.intranet.d.model.folders.FolderModel
import ru.yandex.intranet.d.model.folders.FolderType
import ru.yandex.intranet.d.model.providers.*
import ru.yandex.intranet.d.model.quotas.QuotaModel
import ru.yandex.intranet.d.model.resources.ResourceModel
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel
import ru.yandex.intranet.d.model.services.ServiceRecipeModel
import ru.yandex.intranet.d.model.services.ServiceState
import ru.yandex.intranet.d.model.usage.*
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.monlib.metrics.Metric
import ru.yandex.monlib.metrics.labels.Labels
import ru.yandex.monlib.metrics.primitives.GaugeInt64
import ru.yandex.monlib.metrics.registry.MetricId
import ru.yandex.monlib.metrics.registry.MetricRegistry
import java.math.BigInteger
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Refresh service aggregates service test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class RefreshServiceAggregatesServiceTest(
    @Autowired private val serviceAggregatesDao: ServiceAggregatesDao,
    @Autowired private val serviceAggregateUsageDao: ServiceAggregateUsageDao,
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
    @Autowired private val providerReserveAccountsDao: ProviderReserveAccountsDao,
    @Autowired private val tableClient: YdbTableClient,
    @Autowired private val refreshServiceAggregatesService: RefreshServiceAggregatesService,
    @Autowired private val webClient: WebTestClient
) {

    @Test
    fun testOneResourceOneRootTwoChildrenTwoGrandchildren() = runBlocking {
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
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE, null, null, null)
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
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
        refreshServiceAggregatesService.refreshMetrics()
        val aggregatesCountMetric = getJobMetric<GaugeInt64>("cron.jobs.aggregations_count", provider.key)!!
        val aggregatesCountMaxByResourceMetric = getJobMetric<GaugeInt64>("cron.jobs.aggregations_count.max_by_resource",
            provider.key)!!
        refreshServiceAggregatesService.refreshProvider(provider.id, Clock.fixed(now, ZoneId.systemDefault()))
        val expectedAggregates =
            expectedAggregates(root, provider, resource, now, childOne, childTwo, grandchildOne, grandchildTwo, null, false, false)
        val expectedDenormalizedAggregates = expectedDenormalizedAggregates(root, resource, now, provider,
            childOne, childTwo, grandchildOne, grandchildTwo, null, false, false)
        val aggregateIds = expectedAggregates.map { it.key }
        val denormalizedAggregateIds = expectedDenormalizedAggregates.map { it.key }
        val actualAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregatesDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualUsageAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregateUsageDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualDenormalizedAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceDenormalizedAggregatesDao.getByIds(txSession, denormalizedAggregateIds)
        }}!!
        Assertions.assertEquals(expectedAggregates, actualAggregates.toSet())
        Assertions.assertTrue(actualUsageAggregates.isEmpty())
        Assertions.assertEquals(expectedDenormalizedAggregates, actualDenormalizedAggregates.toSet())
        Assertions.assertEquals(expectedDenormalizedAggregates.size.toLong(), aggregatesCountMetric.get())
        val maxCountByResource = expectedDenormalizedAggregates.groupingBy { it.key.resourceId }
            .eachCount()
            .maxOfOrNull { it.value }
            ?.toLong()
        Assertions.assertEquals(maxCountByResource, aggregatesCountMaxByResourceMetric.get())
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.deleteQuotasRetryable(txSession, listOf(
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaRoot.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaChildOne.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaChildTwo.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaGrandchildOne.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaGrandchildTwo.toKey()))).awaitSingleOrNull()
        }}
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.removeAllRetryable(txSession, listOf(
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionRoot.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionChildOne.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionChildTwo.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionGrandchildOne.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionGrandchildTwo.identity))).awaitSingleOrNull()
        }}
        refreshServiceAggregatesService.refreshProvider(provider.id, Clock.fixed(now, ZoneId.systemDefault()))
        val actualEmptyAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregatesDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualEmptyAggregateUsage = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregateUsageDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualEmptyDenormalizedAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceDenormalizedAggregatesDao.getByIds(txSession, denormalizedAggregateIds)
        }}!!
        Assertions.assertTrue(actualEmptyAggregates.isEmpty())
        Assertions.assertTrue(actualEmptyAggregateUsage.isEmpty())
        Assertions.assertTrue(actualEmptyDenormalizedAggregates.isEmpty())
        Assertions.assertEquals(0L, aggregatesCountMetric.get())
        Assertions.assertEquals(0L, aggregatesCountMaxByResourceMetric.get())
    }

    @ParameterizedTest
    @MethodSource("aggregationAlgorithms")
    fun testParameterized(algorithm: AggregationAlgorithm) = runBlocking {
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
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE,
            algorithm, null, null)
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
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
        refreshServiceAggregatesService.refreshMetrics()
        val aggregatesCountMetric = getJobMetric<GaugeInt64>("cron.jobs.aggregations_count", provider.key)!!
        val aggregatesCountMaxByResourceMetric = getJobMetric<GaugeInt64>("cron.jobs.aggregations_count.max_by_resource",
            provider.key)!!
        refreshServiceAggregatesService.refreshProvider(provider.id, Clock.fixed(now, ZoneId.systemDefault()))
        val expectedAggregates =
            expectedAggregates(root, provider, resource, now, childOne, childTwo, grandchildOne, grandchildTwo, null, false, false)
        val expectedDenormalizedAggregates = expectedDenormalizedAggregates(root, resource, now, provider,
            childOne, childTwo, grandchildOne, grandchildTwo, null, false, false)
        val aggregateIds = expectedAggregates.map { it.key }
        val denormalizedAggregateIds = expectedDenormalizedAggregates.map { it.key }
        val actualAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregatesDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualUsageAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregateUsageDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualDenormalizedAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceDenormalizedAggregatesDao.getByIds(txSession, denormalizedAggregateIds)
        }}!!
        Assertions.assertEquals(expectedAggregates, actualAggregates.toSet())
        Assertions.assertTrue(actualUsageAggregates.isEmpty())
        Assertions.assertEquals(expectedDenormalizedAggregates, actualDenormalizedAggregates.toSet())
        Assertions.assertEquals(expectedDenormalizedAggregates.size.toLong(), aggregatesCountMetric.get())
        val maxCountByResource = expectedDenormalizedAggregates.groupingBy { it.key.resourceId }
            .eachCount()
            .maxOfOrNull { it.value }
            ?.toLong()
        Assertions.assertEquals(maxCountByResource, aggregatesCountMaxByResourceMetric.get())
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.deleteQuotasRetryable(txSession, listOf(
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaRoot.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaChildOne.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaChildTwo.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaGrandchildOne.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaGrandchildTwo.toKey()))).awaitSingleOrNull()
        }}
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.removeAllRetryable(txSession, listOf(
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionRoot.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionChildOne.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionChildTwo.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionGrandchildOne.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionGrandchildTwo.identity))).awaitSingleOrNull()
        }}
        refreshServiceAggregatesService.refreshProvider(provider.id, Clock.fixed(now, ZoneId.systemDefault()))
        val actualEmptyAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregatesDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualEmptyAggregateUsage = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregateUsageDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualEmptyDenormalizedAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceDenormalizedAggregatesDao.getByIds(txSession, denormalizedAggregateIds)
        }}!!
        Assertions.assertTrue(actualEmptyAggregates.isEmpty())
        Assertions.assertTrue(actualEmptyAggregateUsage.isEmpty())
        Assertions.assertTrue(actualEmptyDenormalizedAggregates.isEmpty())
        Assertions.assertEquals(0L, aggregatesCountMetric.get())
        Assertions.assertEquals(0L, aggregatesCountMaxByResourceMetric.get())
    }

    @Test
    fun testDeepTree() = runBlocking {
        val services = mutableListOf<ServiceRecipeModel>()
        var currentParent: Long? = null
        var currentServiceId: Long = 65535L
        repeat(200) {
            val currentService = serviceModel(currentServiceId, currentParent)
            services.add(currentService)
            currentParent = currentServiceId
            currentServiceId++
        }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, services).awaitSingleOrNull()
        }}
        val folders = services.map { folderModel(it.id) }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            folderDao.upsertAllRetryable(txSession, folders).awaitSingleOrNull()
        }}
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE, null, null, null)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProviderRetryable(txSession, provider)
            .awaitSingleOrNull() }}
        val resourceType = resourceTypeModel(provider.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            .awaitSingleOrNull() }}
        val resources = mutableListOf<ResourceModel>()
        repeat(10) {
            resources.add(resourceModel(
                provider.id, "test", resourceType.id, emptyMap(),
                UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
            ))
        }
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourcesRetryable(txSession, resources)
            .awaitSingleOrNull() }}
        val accounts = folders.map { accountModel(provider.id, null, it.id, "test") }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, accounts).awaitSingleOrNull()
        }}
        val quotas = mutableListOf<QuotaModel>()
        folders.forEach { folder -> resources.forEach { resource -> quotas
            .add(quotaModel(provider.id, resource.id, folder.id, 10L, 5L)) } }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.upsertAllRetryable(txSession, quotas).awaitSingleOrNull()
        }}
        val provisions = mutableListOf<AccountsQuotasModel>()
        accounts.forEach { account -> resources.forEach { resource -> provisions
            .add(accountQuotaModel(provider.id, resource.id, account.folderId, account.id, 5L, 2L)) } }
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.upsertAllRetryable(txSession, provisions).awaitSingleOrNull()
        }}
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
        refreshServiceAggregatesService.refreshProvider(provider.id, Clock.fixed(now, ZoneId.systemDefault()))
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.deleteQuotasRetryable(txSession, quotas.map { WithTenant(Tenants.DEFAULT_TENANT_ID, it.toKey()) })
                .awaitSingleOrNull()
        }}
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.removeAllRetryable(txSession,
                provisions.map { WithTenant(Tenants.DEFAULT_TENANT_ID, it.identity) }).awaitSingleOrNull()
        }}
        refreshServiceAggregatesService.refreshProvider(provider.id, Clock.fixed(now, ZoneId.systemDefault()))
    }

    @Test
    fun testEndpoint(): Unit = runBlocking {
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
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE, null, null, null)
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
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.deleteQuotasRetryable(txSession, listOf(
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaRoot.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaChildOne.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaChildTwo.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaGrandchildOne.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaGrandchildTwo.toKey()))).awaitSingleOrNull()
        }}
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.removeAllRetryable(txSession, listOf(
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionRoot.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionChildOne.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionChildTwo.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionGrandchildOne.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionGrandchildTwo.identity))).awaitSingleOrNull()
        }}
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
    }

    @Test
    fun testOneResourceOneRootTwoChildrenTwoGrandchildrenWithUsage() = runBlocking {
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
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE,
            null, UsageMode.TIME_SERIES, 1L)
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
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
        val usageRoot = serviceUsageModel(root.id, provider.id, resource.id, BigInteger.valueOf(1),
            BigInteger.valueOf(4), BigInteger.valueOf(5), now)
        val usageChildOne = serviceUsageModel(childOne.id, provider.id, resource.id, BigInteger.valueOf(1),
            BigInteger.valueOf(1), BigInteger.valueOf(2), now)
        val usageChildTwo = serviceUsageModel(childTwo.id, provider.id, resource.id, BigInteger.valueOf(1),
            BigInteger.valueOf(1), BigInteger.valueOf(2), now)
        val usageGrandchildOne = serviceUsageModel(grandchildOne.id, provider.id, resource.id, BigInteger.valueOf(1),
            null, null, now)
        val usageGrandchildTwo = serviceUsageModel(grandchildTwo.id, provider.id, resource.id, BigInteger.valueOf(1),
            null, null, now)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, listOf(usageRoot, usageChildOne, usageChildTwo,
                usageGrandchildOne, usageGrandchildTwo))
        }}
        refreshServiceAggregatesService.refreshProvider(provider.id, Clock.fixed(now, ZoneId.systemDefault()))
        val expectedAggregates =
            expectedAggregates(root, provider, resource, now, childOne, childTwo, grandchildOne, grandchildTwo, true, false, true)
        val expectedAggregateUsage = expectedAggregateUsage(root, provider, resource, now, childOne, childTwo,
            grandchildOne, grandchildTwo)
        val expectedDenormalizedAggregates = expectedDenormalizedAggregates(root, resource, now, provider,
            childOne, childTwo, grandchildOne, grandchildTwo, true, false, true)
        val aggregateIds = expectedAggregates.map { it.key }
        val denormalizedAggregateIds = expectedDenormalizedAggregates.map { it.key }
        val actualAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregatesDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualUsageAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregateUsageDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualDenormalizedAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceDenormalizedAggregatesDao.getByIds(txSession, denormalizedAggregateIds)
        }}!!
        Assertions.assertEquals(expectedAggregates, actualAggregates.toSet())
        Assertions.assertEquals(expectedAggregateUsage, actualUsageAggregates.toSet())
        Assertions.assertEquals(expectedDenormalizedAggregates, actualDenormalizedAggregates.toSet())
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.deleteQuotasRetryable(txSession, listOf(
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaRoot.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaChildOne.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaChildTwo.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaGrandchildOne.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaGrandchildTwo.toKey()))).awaitSingleOrNull()
        }}
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.removeAllRetryable(txSession, listOf(
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionRoot.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionChildOne.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionChildTwo.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionGrandchildOne.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionGrandchildTwo.identity))).awaitSingleOrNull()
        }}
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.deleteByIdsRetryable(txSession, listOf(usageRoot.key, usageChildOne.key, usageChildTwo.key,
                usageGrandchildOne.key, usageGrandchildTwo.key))
        }}
        refreshServiceAggregatesService.refreshProvider(provider.id, Clock.fixed(now, ZoneId.systemDefault()))
        val actualEmptyAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregatesDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualEmptyUsageAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregateUsageDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualEmptyDenormalizedAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceDenormalizedAggregatesDao.getByIds(txSession, denormalizedAggregateIds)
        }}!!
        Assertions.assertTrue(actualEmptyAggregates.isEmpty())
        Assertions.assertTrue(actualEmptyUsageAggregates.isEmpty())
        Assertions.assertTrue(actualEmptyDenormalizedAggregates.isEmpty())
    }

    @ParameterizedTest
    @MethodSource("aggregationAlgorithms")
    fun testParameterizedWithUsage(algorithm: AggregationAlgorithm) = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne, childTwo,
                grandchildOne, grandchildTwo)).awaitSingleOrNull()
        }}
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderChildOneReserve = FolderModel.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setServiceId(childOne.id)
            .setVersion(0L)
            .setDisplayName("Test")
            .setDescription("Test")
            .setDeleted(false)
            .setFolderType(FolderType.PROVIDER_RESERVE)
            .setTags(emptySet())
            .setNextOpLogOrder(1L)
            .build()
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne, folderChildTwo,
                folderChildOneReserve, folderGrandchildOne, folderGrandchildTwo)).awaitSingleOrNull()
        }}
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE,
            algorithm, UsageMode.TIME_SERIES, 1L)
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
        val accountChildReserve = accountModel(provider.id, null, folderRoot.id, "test")
        val accountChildOneReserveFolder = accountModel(provider.id, null, folderChildOneReserve.id, "test")
        val accountGrandchildOne = accountModel(provider.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwo = accountModel(provider.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRoot, accountChildOne, accountChildTwo,
                accountChildReserve, accountGrandchildOne, accountGrandchildTwo)).awaitSingleOrNull()
        }}
        val quotaRoot = quotaModel(provider.id, resource.id, folderRoot.id, 20L, 5L)
        val quotaChildOne = quotaModel(provider.id, resource.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwo = quotaModel(provider.id, resource.id, folderChildTwo.id, 60L, 30L)
        val quotaChildOneReserveFolder = quotaModel(provider.id, resource.id, folderChildOneReserve.id, 50L, 0L)
        val quotaGrandchildOne = quotaModel(provider.id, resource.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwo = quotaModel(provider.id, resource.id, folderGrandchildTwo.id, 60L, 30L)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.upsertAllRetryable(txSession, listOf(quotaRoot, quotaChildOne, quotaChildTwo,
                quotaChildOneReserveFolder, quotaGrandchildOne, quotaGrandchildTwo)).awaitSingleOrNull()
        }}
        val provisionRoot = accountQuotaModel(provider.id, resource.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOne = accountQuotaModel(provider.id, resource.id, folderChildOne.id, accountChildOne.id,
            15L, 6L)
        val provisionChildTwo = accountQuotaModel(provider.id, resource.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L)
        val provisionChildReserve = accountQuotaModel(provider.id, resource.id, folderRoot.id,
            accountChildReserve.id, 10L, 0L)
        val provisionChildOneReserveFolder = accountQuotaModel(provider.id, resource.id, folderChildOneReserve.id,
            accountChildOneReserveFolder.id, 10L, 0L)
        val provisionGrandchildOne = accountQuotaModel(provider.id, resource.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L)
        val provisionGrandchildTwo = accountQuotaModel(provider.id, resource.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.upsertAllRetryable(txSession, listOf(provisionRoot, provisionChildOne, provisionChildTwo,
                provisionChildReserve, provisionGrandchildOne, provisionGrandchildTwo)).awaitSingleOrNull()
        }}
        val providerReserveAccount = ProviderReserveAccountModel(ProviderReserveAccountKey(
                Tenants.DEFAULT_TENANT_ID, provider.id, null, accountChildReserve.id
            ))
        dbSessionRetryable(tableClient) { rwTxRetryable {
            providerReserveAccountsDao.upsertOneRetryableMono(txSession, providerReserveAccount).awaitSingleOrNull()
        }}
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
        val usageRoot = serviceUsageModel(root.id, provider.id, resource.id, BigInteger.valueOf(1),
            BigInteger.valueOf(5), BigInteger.valueOf(6), now)
        val usageChildOne = serviceUsageModel(childOne.id, provider.id, resource.id, BigInteger.valueOf(1),
            BigInteger.valueOf(1), BigInteger.valueOf(2), now)
        val usageChildTwo = serviceUsageModel(childTwo.id, provider.id, resource.id, BigInteger.valueOf(1),
            BigInteger.valueOf(1), BigInteger.valueOf(2), now)
        val usageGrandchildOne = serviceUsageModel(grandchildOne.id, provider.id, resource.id, BigInteger.valueOf(1),
            null, null, now)
        val usageGrandchildTwo = serviceUsageModel(grandchildTwo.id, provider.id, resource.id, BigInteger.valueOf(1),
            null, null, now)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, listOf(usageRoot, usageChildOne, usageChildTwo,
                usageGrandchildOne, usageGrandchildTwo))
        }}
        refreshServiceAggregatesService.refreshProvider(provider.id, Clock.fixed(now, ZoneId.systemDefault()))
        val expectedAggregates =
            expectedAggregates(root, provider, resource, now, childOne, childTwo, grandchildOne, grandchildTwo,
                true, false, true)
        val expectedAggregateUsage = expectedAggregateUsage(root, provider, resource, now, childOne, childTwo,
            grandchildOne, grandchildTwo)
        val expectedDenormalizedAggregates = expectedDenormalizedAggregates(root, resource, now, provider,
            childOne, childTwo, grandchildOne, grandchildTwo, true, false, true)
        val aggregateIds = expectedAggregates.map { it.key }
        val denormalizedAggregateIds = expectedDenormalizedAggregates.map { it.key }
        val actualAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregatesDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualUsageAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregateUsageDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualDenormalizedAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceDenormalizedAggregatesDao.getByIds(txSession, denormalizedAggregateIds)
        }}!!
        Assertions.assertEquals(expectedAggregates, actualAggregates.toSet())
        Assertions.assertEquals(expectedAggregateUsage, actualUsageAggregates.toSet())
        Assertions.assertEquals(expectedDenormalizedAggregates, actualDenormalizedAggregates.toSet())
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.deleteQuotasRetryable(txSession, listOf(
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaRoot.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaChildOne.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaChildTwo.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaGrandchildOne.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaGrandchildTwo.toKey()))).awaitSingleOrNull()
        }}
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.removeAllRetryable(txSession, listOf(
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionRoot.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionChildOne.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionChildTwo.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionGrandchildOne.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionGrandchildTwo.identity))).awaitSingleOrNull()
        }}
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.deleteByIdsRetryable(txSession, listOf(usageRoot.key, usageChildOne.key, usageChildTwo.key,
                usageGrandchildOne.key, usageGrandchildTwo.key))
        }}
        refreshServiceAggregatesService.refreshProvider(provider.id, Clock.fixed(now, ZoneId.systemDefault()))
        val actualEmptyAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregatesDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualEmptyUsageAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregateUsageDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualEmptyDenormalizedAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceDenormalizedAggregatesDao.getByIds(txSession, denormalizedAggregateIds)
        }}!!
        Assertions.assertTrue(actualEmptyAggregates.isEmpty())
        Assertions.assertTrue(actualEmptyUsageAggregates.isEmpty())
        Assertions.assertTrue(actualEmptyDenormalizedAggregates.isEmpty())
    }

    @Test
    fun testOneResourceOneRootTwoChildrenTwoGrandchildrenWithUsageUnused() = runBlocking {
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
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE,
            null, UsageMode.UNUSED_ESTIMATION_VALUE, 1L)
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
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
        val usageRoot = serviceUsageModelUnused(root.id, provider.id, resource.id, BigInteger.valueOf(1),
            BigInteger.valueOf(4), BigInteger.valueOf(5), now)
        val usageChildOne = serviceUsageModelUnused(childOne.id, provider.id, resource.id, BigInteger.valueOf(1),
            BigInteger.valueOf(1), BigInteger.valueOf(2), now)
        val usageChildTwo = serviceUsageModelUnused(childTwo.id, provider.id, resource.id, BigInteger.valueOf(1),
            BigInteger.valueOf(1), BigInteger.valueOf(2), now)
        val usageGrandchildOne = serviceUsageModelUnused(grandchildOne.id, provider.id, resource.id, BigInteger.valueOf(1),
            null, null, now)
        val usageGrandchildTwo = serviceUsageModelUnused(grandchildTwo.id, provider.id, resource.id, BigInteger.valueOf(1),
            null, null, now)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, listOf(usageRoot, usageChildOne, usageChildTwo,
                usageGrandchildOne, usageGrandchildTwo))
        }}
        refreshServiceAggregatesService.refreshProvider(provider.id, Clock.fixed(now, ZoneId.systemDefault()))
        val expectedAggregates =
            expectedAggregates(root, provider, resource, now, childOne, childTwo, grandchildOne, grandchildTwo, null, true, false)
        val expectedAggregateUsage = expectedAggregateUsageWithUnused(root, provider, resource, now, childOne,
            childTwo, grandchildOne, grandchildTwo)
        val expectedDenormalizedAggregates = expectedDenormalizedAggregates(root, resource, now, provider,
            childOne, childTwo, grandchildOne, grandchildTwo, null, true, false)
        val aggregateIds = expectedAggregates.map { it.key }
        val denormalizedAggregateIds = expectedDenormalizedAggregates.map { it.key }
        val actualAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregatesDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualAggregateUsage = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregateUsageDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualDenormalizedAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceDenormalizedAggregatesDao.getByIds(txSession, denormalizedAggregateIds)
        }}!!
        Assertions.assertEquals(expectedAggregates, actualAggregates.toSet())
        Assertions.assertEquals(expectedAggregateUsage, actualAggregateUsage.toSet())
        Assertions.assertEquals(expectedDenormalizedAggregates, actualDenormalizedAggregates.toSet())
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.deleteQuotasRetryable(txSession, listOf(
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaRoot.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaChildOne.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaChildTwo.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaGrandchildOne.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaGrandchildTwo.toKey()))).awaitSingleOrNull()
        }}
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.removeAllRetryable(txSession, listOf(
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionRoot.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionChildOne.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionChildTwo.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionGrandchildOne.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionGrandchildTwo.identity))).awaitSingleOrNull()
        }}
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.deleteByIdsRetryable(txSession, listOf(usageRoot.key, usageChildOne.key, usageChildTwo.key,
                usageGrandchildOne.key, usageGrandchildTwo.key))
        }}
        refreshServiceAggregatesService.refreshProvider(provider.id, Clock.fixed(now, ZoneId.systemDefault()))
        val actualEmptyAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregatesDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualEmptyAggregateUsage = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregateUsageDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualEmptyDenormalizedAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceDenormalizedAggregatesDao.getByIds(txSession, denormalizedAggregateIds)
        }}!!
        Assertions.assertTrue(actualEmptyAggregates.isEmpty())
        Assertions.assertTrue(actualEmptyAggregateUsage.isEmpty())
        Assertions.assertTrue(actualEmptyDenormalizedAggregates.isEmpty())
    }

    @ParameterizedTest
    @MethodSource("aggregationAlgorithms")
    fun testParameterizedWithUsageUnused(algorithm: AggregationAlgorithm) = runBlocking {
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
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE,
            algorithm, UsageMode.UNUSED_ESTIMATION_VALUE, 1L)
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
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
        val usageRoot = serviceUsageModelUnused(root.id, provider.id, resource.id, BigInteger.valueOf(1),
            BigInteger.valueOf(4), BigInteger.valueOf(5), now)
        val usageChildOne = serviceUsageModelUnused(childOne.id, provider.id, resource.id, BigInteger.valueOf(1),
            BigInteger.valueOf(1), BigInteger.valueOf(2), now)
        val usageChildTwo = serviceUsageModelUnused(childTwo.id, provider.id, resource.id, BigInteger.valueOf(1),
            BigInteger.valueOf(1), BigInteger.valueOf(2), now)
        val usageGrandchildOne = serviceUsageModelUnused(grandchildOne.id, provider.id, resource.id, BigInteger.valueOf(1),
            null, null, now)
        val usageGrandchildTwo = serviceUsageModelUnused(grandchildTwo.id, provider.id, resource.id, BigInteger.valueOf(1),
            null, null, now)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, listOf(usageRoot, usageChildOne, usageChildTwo,
                usageGrandchildOne, usageGrandchildTwo))
        }}
        refreshServiceAggregatesService.refreshProvider(provider.id, Clock.fixed(now, ZoneId.systemDefault()))
        val expectedAggregates =
            expectedAggregates(root, provider, resource, now, childOne, childTwo, grandchildOne, grandchildTwo, null, true, false)
        val expectedAggregateUsage = expectedAggregateUsageWithUnused(root, provider, resource, now, childOne,
            childTwo, grandchildOne, grandchildTwo)
        val expectedDenormalizedAggregates = expectedDenormalizedAggregates(root, resource, now, provider,
            childOne, childTwo, grandchildOne, grandchildTwo, null, true, false)
        val aggregateIds = expectedAggregates.map { it.key }
        val denormalizedAggregateIds = expectedDenormalizedAggregates.map { it.key }
        val actualAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregatesDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualAggregateUsage = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregateUsageDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualDenormalizedAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceDenormalizedAggregatesDao.getByIds(txSession, denormalizedAggregateIds)
        }}!!
        Assertions.assertEquals(expectedAggregates, actualAggregates.toSet())
        Assertions.assertEquals(expectedAggregateUsage, actualAggregateUsage.toSet())
        Assertions.assertEquals(expectedDenormalizedAggregates, actualDenormalizedAggregates.toSet())
        dbSessionRetryable(tableClient) { rwTxRetryable {
            quotasDao.deleteQuotasRetryable(txSession, listOf(
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaRoot.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaChildOne.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaChildTwo.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaGrandchildOne.toKey()),
                WithTenant(Tenants.DEFAULT_TENANT_ID, quotaGrandchildTwo.toKey()))).awaitSingleOrNull()
        }}
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsQuotasDao.removeAllRetryable(txSession, listOf(
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionRoot.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionChildOne.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionChildTwo.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionGrandchildOne.identity),
                WithTenant(Tenants.DEFAULT_TENANT_ID, provisionGrandchildTwo.identity))).awaitSingleOrNull()
        }}
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.deleteByIdsRetryable(txSession, listOf(usageRoot.key, usageChildOne.key, usageChildTwo.key,
                usageGrandchildOne.key, usageGrandchildTwo.key))
        }}
        refreshServiceAggregatesService.refreshProvider(provider.id, Clock.fixed(now, ZoneId.systemDefault()))
        val actualEmptyAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregatesDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualEmptyAggregateUsage = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceAggregateUsageDao.getByIds(txSession, aggregateIds)
        }}!!
        val actualEmptyDenormalizedAggregates = dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceDenormalizedAggregatesDao.getByIds(txSession, denormalizedAggregateIds)
        }}!!
        Assertions.assertTrue(actualEmptyAggregates.isEmpty())
        Assertions.assertTrue(actualEmptyAggregateUsage.isEmpty())
        Assertions.assertTrue(actualEmptyDenormalizedAggregates.isEmpty())
    }

    private fun expectedDenormalizedAggregates(
        root: ServiceRecipeModel,
        resource: ResourceModel,
        now: Instant,
        provider: ProviderModel,
        childOne: ServiceRecipeModel,
        childTwo: ServiceRecipeModel,
        grandchildOne: ServiceRecipeModel,
        grandchildTwo: ServiceRecipeModel,
        extUsage: Boolean?,
        unused: Boolean,
        underUtilization: Boolean
    ) = setOf(
        ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root.id,
                resourceId = resource.id,
                serviceId = root.id,
            ),
            lastUpdate = now,
            epoch = 0L,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(8), BigInteger.valueOf(152)),
            deallocatable = 0L,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(10),
                    balance = BigInteger.valueOf(5),
                    provided = BigInteger.valueOf(5),
                    allocated = BigInteger.valueOf(2),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(3),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(8),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(1) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(4) } else { null }
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = childOne.id,
                resourceId = resource.id,
                serviceId = childOne.id,
            ),
            lastUpdate = now,
            epoch = 0L,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(24), BigInteger.valueOf(48)),
            deallocatable = 0L,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(30),
                    balance = BigInteger.valueOf(15),
                    provided = BigInteger.valueOf(15),
                    allocated = BigInteger.valueOf(6),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(9),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(24),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(1) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(14) } else { null }
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root.id,
                resourceId = resource.id,
                serviceId = childOne.id,
            ),
            lastUpdate = now,
            epoch = 0L,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(24), BigInteger.valueOf(152)),
            deallocatable = 0L,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(30),
                    balance = BigInteger.valueOf(15),
                    provided = BigInteger.valueOf(15),
                    allocated = BigInteger.valueOf(6),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(9),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(24),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(1) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(14) } else { null }
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = childTwo.id,
                resourceId = resource.id,
                serviceId = childTwo.id,
            ),
            lastUpdate = now,
            epoch = 0L,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(48), BigInteger.valueOf(96)),
            deallocatable = 0L,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(60),
                    balance = BigInteger.valueOf(30),
                    provided = BigInteger.valueOf(30),
                    allocated = BigInteger.valueOf(12),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(18),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(48),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(1) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(29) } else { null }
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root.id,
                resourceId = resource.id,
                serviceId = childTwo.id,
            ),
            lastUpdate = now,
            epoch = 0L,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(48), BigInteger.valueOf(152)),
            deallocatable = 0L,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(60),
                    balance = BigInteger.valueOf(30),
                    provided = BigInteger.valueOf(30),
                    allocated = BigInteger.valueOf(12),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(18),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(48),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(1) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(29) } else { null }
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = grandchildOne.id,
                resourceId = resource.id,
                serviceId = grandchildOne.id,
            ),
            lastUpdate = now,
            epoch = 0L,
            providerId = provider.id,
            transferable = Long.MAX_VALUE,
            deallocatable = 0L,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(30),
                    balance = BigInteger.valueOf(15),
                    provided = BigInteger.valueOf(15),
                    allocated = BigInteger.valueOf(6),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(9),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(24),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(1) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(14) } else { null }
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = grandchildTwo.id,
                resourceId = resource.id,
                serviceId = grandchildTwo.id,
            ),
            lastUpdate = now,
            epoch = 0L,
            providerId = provider.id,
            transferable = Long.MAX_VALUE,
            deallocatable = 0L,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(60),
                    balance = BigInteger.valueOf(30),
                    provided = BigInteger.valueOf(30),
                    allocated = BigInteger.valueOf(12),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(18),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(48),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(1) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(29) } else { null }
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = childOne.id,
                resourceId = resource.id,
                serviceId = grandchildOne.id,
            ),
            lastUpdate = now,
            epoch = 0L,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(24), BigInteger.valueOf(48)),
            deallocatable = 0L,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(30),
                    balance = BigInteger.valueOf(15),
                    provided = BigInteger.valueOf(15),
                    allocated = BigInteger.valueOf(6),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(9),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(24),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(1) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(14) } else { null }
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = childTwo.id,
                resourceId = resource.id,
                serviceId = grandchildTwo.id,
            ),
            lastUpdate = now,
            epoch = 0L,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(48), BigInteger.valueOf(96)),
            deallocatable = 0L,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(60),
                    balance = BigInteger.valueOf(30),
                    provided = BigInteger.valueOf(30),
                    allocated = BigInteger.valueOf(12),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(18),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(48),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(1) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(29) } else { null }
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root.id,
                resourceId = resource.id,
                serviceId = grandchildOne.id,
            ),
            lastUpdate = now,
            epoch = 0L,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(24), BigInteger.valueOf(152)),
            deallocatable = 0L,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(30),
                    balance = BigInteger.valueOf(15),
                    provided = BigInteger.valueOf(15),
                    allocated = BigInteger.valueOf(6),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(9),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(24),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(1) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(14) } else { null }
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root.id,
                resourceId = resource.id,
                serviceId = grandchildTwo.id,
            ),
            lastUpdate = now,
            epoch = 0L,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(48), BigInteger.valueOf(152)),
            deallocatable = 0L,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(60),
                    balance = BigInteger.valueOf(30),
                    provided = BigInteger.valueOf(30),
                    allocated = BigInteger.valueOf(12),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(18),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(48),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(1) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(29) } else { null }
                )
            )
        )
    )

    private fun expectedAggregates(
        root: ServiceRecipeModel,
        provider: ProviderModel,
        resource: ResourceModel,
        now: Instant,
        childOne: ServiceRecipeModel,
        childTwo: ServiceRecipeModel,
        grandchildOne: ServiceRecipeModel,
        grandchildTwo: ServiceRecipeModel,
        extUsage: Boolean?,
        unused: Boolean,
        underUtilization: Boolean
    ) = setOf(
        ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root.id,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = 0L,
            exactAmounts = ServiceAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(10),
                    balance = BigInteger.valueOf(5),
                    provided = BigInteger.valueOf(5),
                    allocated = BigInteger.valueOf(2),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(3),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(8),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(1) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(4) } else { null }
                ),
                subtree = AggregateBundle(
                    quota = BigInteger.valueOf(180),
                    balance = BigInteger.valueOf(90),
                    provided = BigInteger.valueOf(90),
                    allocated = BigInteger.valueOf(36),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(54),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(144),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(4) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(86) } else { null }
                ),
                total = AggregateBundle(
                    quota = BigInteger.valueOf(190),
                    balance = BigInteger.valueOf(95),
                    provided = BigInteger.valueOf(95),
                    allocated = BigInteger.valueOf(38),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(57),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(152),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(5) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(90) } else { null }
                )
            )
        ), ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = childOne.id,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = 0L,
            exactAmounts = ServiceAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(30),
                    balance = BigInteger.valueOf(15),
                    provided = BigInteger.valueOf(15),
                    allocated = BigInteger.valueOf(6),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(9),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(24),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(1) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(14) } else { null }
                ),
                subtree = AggregateBundle(
                    quota = BigInteger.valueOf(30),
                    balance = BigInteger.valueOf(15),
                    provided = BigInteger.valueOf(15),
                    allocated = BigInteger.valueOf(6),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(9),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(24),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(1) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(14) } else { null }
                ),
                total = AggregateBundle(
                    quota = BigInteger.valueOf(60),
                    balance = BigInteger.valueOf(30),
                    provided = BigInteger.valueOf(30),
                    allocated = BigInteger.valueOf(12),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(18),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(48),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(2) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(28) } else { null }
                )
            )
        ), ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = childTwo.id,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = 0L,
            exactAmounts = ServiceAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(60),
                    balance = BigInteger.valueOf(30),
                    provided = BigInteger.valueOf(30),
                    allocated = BigInteger.valueOf(12),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(18),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(48),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(1) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(29) } else { null }
                ),
                subtree = AggregateBundle(
                    quota = BigInteger.valueOf(60),
                    balance = BigInteger.valueOf(30),
                    provided = BigInteger.valueOf(30),
                    allocated = BigInteger.valueOf(12),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(18),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(48),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(1) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(29) } else { null }
                ),
                total = AggregateBundle(
                    quota = BigInteger.valueOf(120),
                    balance = BigInteger.valueOf(60),
                    provided = BigInteger.valueOf(60),
                    allocated = BigInteger.valueOf(24),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(36),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(96),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(2) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(58) } else { null }
                )
            )
        ), ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = grandchildOne.id,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = 0L,
            exactAmounts = ServiceAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(30),
                    balance = BigInteger.valueOf(15),
                    provided = BigInteger.valueOf(15),
                    allocated = BigInteger.valueOf(6),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(9),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(24),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(1) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(14) } else { null }
                ),
                subtree = null,
                total = null
            )
        ), ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = grandchildTwo.id,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = 0L,
            exactAmounts = ServiceAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(60),
                    balance = BigInteger.valueOf(30),
                    provided = BigInteger.valueOf(30),
                    allocated = BigInteger.valueOf(12),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(18),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(48),
                    deallocatable = BigInteger.ZERO,
                    extUsage = extUsage,
                    unusedEst = if (unused) { BigInteger.valueOf(1) } else { null },
                    underutilizedEst = if (underUtilization) { BigInteger.valueOf(29) } else { null }
                ),
                subtree = null,
                total = null
            )
        )
    )

    private fun expectedAggregateUsage(
        root: ServiceRecipeModel,
        provider: ProviderModel,
        resource: ResourceModel,
        now: Instant,
        childOne: ServiceRecipeModel,
        childTwo: ServiceRecipeModel,
        grandchildOne: ServiceRecipeModel,
        grandchildTwo: ServiceRecipeModel
    ) = setOf(
        ServiceAggregateUsageModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root.id,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = 0L,
            exactAmounts = ServiceUsageAmounts(
                own = UsageAmount(
                    value = null,
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.ZERO,
                    accumulated = BigInteger.valueOf(1),
                    accumulatedDuration = 1L,
                    histogram = listOf(HistogramBin(BigInteger.valueOf(1), BigInteger.valueOf(1), 1L)),
                    values = null,
                    valuesX = listOf(0L),
                    valuesY = listOf(BigInteger.valueOf(1L)),
                    unused = null
                ),
                subtree = UsageAmount(
                    value = null,
                    average = BigInteger.valueOf(4),
                    min = BigInteger.valueOf(4),
                    max = BigInteger.valueOf(4),
                    median = BigInteger.valueOf(4),
                    variance = BigInteger.ZERO,
                    accumulated = BigInteger.valueOf(4),
                    accumulatedDuration = 1L,
                    histogram = listOf(HistogramBin(BigInteger.valueOf(4), BigInteger.valueOf(4), 1L)),
                    values = null,
                    valuesX = listOf(0L),
                    valuesY = listOf(BigInteger.valueOf(4L)),
                    unused = null
                ),
                total = UsageAmount(
                    value = null,
                    average = BigInteger.valueOf(5),
                    min = BigInteger.valueOf(5),
                    max = BigInteger.valueOf(5),
                    median = BigInteger.valueOf(5),
                    variance = BigInteger.ZERO,
                    accumulated = BigInteger.valueOf(5),
                    accumulatedDuration = 1L,
                    histogram = listOf(HistogramBin(BigInteger.valueOf(5), BigInteger.valueOf(5), 1L)),
                    values = null,
                    valuesX = listOf(0L),
                    valuesY = listOf(BigInteger.valueOf(5L)),
                    unused = null
                )
            )
        ), ServiceAggregateUsageModel(
        key = ServiceAggregateKey(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = childOne.id,
            providerId = provider.id,
            resourceId = resource.id
        ),
        lastUpdate = now,
        epoch = 0L,
        exactAmounts = ServiceUsageAmounts(
            own = UsageAmount(
                value = null,
                average = BigInteger.valueOf(1),
                min = BigInteger.valueOf(1),
                max = BigInteger.valueOf(1),
                median = BigInteger.valueOf(1),
                variance = BigInteger.ZERO,
                accumulated = BigInteger.valueOf(1),
                accumulatedDuration = 1L,
                histogram = listOf(HistogramBin(BigInteger.valueOf(1), BigInteger.valueOf(1), 1L)),
                values = null,
                valuesX = listOf(0L),
                valuesY = listOf(BigInteger.valueOf(1L)),
                unused = null
            ),
            subtree = UsageAmount(
                value = null,
                average = BigInteger.valueOf(1),
                min = BigInteger.valueOf(1),
                max = BigInteger.valueOf(1),
                median = BigInteger.valueOf(1),
                variance = BigInteger.ZERO,
                accumulated = BigInteger.valueOf(1),
                accumulatedDuration = 1L,
                histogram = listOf(HistogramBin(BigInteger.valueOf(1), BigInteger.valueOf(1), 1L)),
                values = null,
                valuesX = listOf(0L),
                valuesY = listOf(BigInteger.valueOf(1L)),
                unused = null
            ),
            total = UsageAmount(
                value = null,
                average = BigInteger.valueOf(2),
                min = BigInteger.valueOf(2),
                max = BigInteger.valueOf(2),
                median = BigInteger.valueOf(2),
                variance = BigInteger.ZERO,
                accumulated = BigInteger.valueOf(2),
                accumulatedDuration = 1L,
                histogram = listOf(HistogramBin(BigInteger.valueOf(2), BigInteger.valueOf(2), 1L)),
                values = null,
                valuesX = listOf(0L),
                valuesY = listOf(BigInteger.valueOf(2L)),
                unused = null
            )
        )
    ), ServiceAggregateUsageModel(
        key = ServiceAggregateKey(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = childTwo.id,
            providerId = provider.id,
            resourceId = resource.id
        ),
        lastUpdate = now,
        epoch = 0L,
        exactAmounts = ServiceUsageAmounts(
            own = UsageAmount(
                value = null,
                average = BigInteger.valueOf(1),
                min = BigInteger.valueOf(1),
                max = BigInteger.valueOf(1),
                median = BigInteger.valueOf(1),
                variance = BigInteger.ZERO,
                accumulated = BigInteger.valueOf(1),
                accumulatedDuration = 1L,
                histogram = listOf(HistogramBin(BigInteger.valueOf(1), BigInteger.valueOf(1), 1L)),
                values = null,
                valuesX = listOf(0L),
                valuesY = listOf(BigInteger.valueOf(1L)),
                unused = null
            ),
            subtree = UsageAmount(
                value = null,
                average = BigInteger.valueOf(1),
                min = BigInteger.valueOf(1),
                max = BigInteger.valueOf(1),
                median = BigInteger.valueOf(1),
                variance = BigInteger.ZERO,
                accumulated = BigInteger.valueOf(1),
                accumulatedDuration = 1L,
                histogram = listOf(HistogramBin(BigInteger.valueOf(1), BigInteger.valueOf(1), 1L)),
                values = null,
                valuesX = listOf(0L),
                valuesY = listOf(BigInteger.valueOf(1L)),
                unused = null
            ),
            total = UsageAmount(
                value = null,
                average = BigInteger.valueOf(2),
                min = BigInteger.valueOf(2),
                max = BigInteger.valueOf(2),
                median = BigInteger.valueOf(2),
                variance = BigInteger.ZERO,
                accumulated = BigInteger.valueOf(2),
                accumulatedDuration = 1L,
                histogram = listOf(HistogramBin(BigInteger.valueOf(2), BigInteger.valueOf(2), 1L)),
                values = null,
                valuesX = listOf(0L),
                valuesY = listOf(BigInteger.valueOf(2L)),
                unused = null
            )
        )
    ), ServiceAggregateUsageModel(
        key = ServiceAggregateKey(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = grandchildOne.id,
            providerId = provider.id,
            resourceId = resource.id
        ),
        lastUpdate = now,
        epoch = 0L,
        exactAmounts = ServiceUsageAmounts(
            own = UsageAmount(
                value = null,
                average = BigInteger.valueOf(1),
                min = BigInteger.valueOf(1),
                max = BigInteger.valueOf(1),
                median = BigInteger.valueOf(1),
                variance = BigInteger.ZERO,
                accumulated = BigInteger.valueOf(1),
                accumulatedDuration = 1L,
                histogram = listOf(HistogramBin(BigInteger.valueOf(1), BigInteger.valueOf(1), 1L)),
                values = null,
                valuesX = listOf(0L),
                valuesY = listOf(BigInteger.valueOf(1L)),
                unused = null
            ),
            subtree = null,
            total = null
        )
    ), ServiceAggregateUsageModel(
        key = ServiceAggregateKey(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = grandchildTwo.id,
            providerId = provider.id,
            resourceId = resource.id
        ),
        lastUpdate = now,
        epoch = 0L,
        exactAmounts = ServiceUsageAmounts(
            own = UsageAmount(
                value = null,
                average = BigInteger.valueOf(1),
                min = BigInteger.valueOf(1),
                max = BigInteger.valueOf(1),
                median = BigInteger.valueOf(1),
                variance = BigInteger.ZERO,
                accumulated = BigInteger.valueOf(1),
                accumulatedDuration = 1L,
                histogram = listOf(HistogramBin(BigInteger.valueOf(1), BigInteger.valueOf(1), 1L)),
                values = null,
                valuesX = listOf(0L),
                valuesY = listOf(BigInteger.valueOf(1L)),
                unused = null
            ),
            subtree = null,
            total = null
        )
    )
    )

    private fun expectedAggregateUsageWithUnused(
        root: ServiceRecipeModel,
        provider: ProviderModel,
        resource: ResourceModel,
        now: Instant,
        childOne: ServiceRecipeModel,
        childTwo: ServiceRecipeModel,
        grandchildOne: ServiceRecipeModel,
        grandchildTwo: ServiceRecipeModel
    ) = setOf(
        ServiceAggregateUsageModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root.id,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = 0L,
            exactAmounts = ServiceUsageAmounts(
                own = UsageAmount(
                    value = null,
                    average = null,
                    min = null,
                    max = null,
                    median = null,
                    variance = null,
                    accumulated = null,
                    accumulatedDuration = null,
                    histogram = null,
                    values = null,
                    valuesX = null,
                    valuesY = null,
                    unused = BigInteger.valueOf(1)
                ),
                subtree = UsageAmount(
                    value = null,
                    average = null,
                    min = null,
                    max = null,
                    median = null,
                    variance = null,
                    accumulated = null,
                    accumulatedDuration = null,
                    histogram = null,
                    values = null,
                    valuesX = null,
                    valuesY = null,
                    unused = BigInteger.valueOf(4)
                ),
                total = UsageAmount(
                    value = null,
                    average = null,
                    min = null,
                    max = null,
                    median = null,
                    variance = null,
                    accumulated = null,
                    accumulatedDuration = null,
                    histogram = null,
                    values = null,
                    valuesX = null,
                    valuesY = null,
                    unused = BigInteger.valueOf(5)
                )
            )
        ), ServiceAggregateUsageModel(
        key = ServiceAggregateKey(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = childOne.id,
            providerId = provider.id,
            resourceId = resource.id
        ),
        lastUpdate = now,
        epoch = 0L,
        exactAmounts = ServiceUsageAmounts(
            own = UsageAmount(
                value = null,
                average = null,
                min = null,
                max = null,
                median = null,
                variance = null,
                accumulated = null,
                accumulatedDuration = null,
                histogram = null,
                values = null,
                valuesX = null,
                valuesY = null,
                unused = BigInteger.valueOf(1)
            ),
            subtree = UsageAmount(
                value = null,
                average = null,
                min = null,
                max = null,
                median = null,
                variance = null,
                accumulated = null,
                accumulatedDuration = null,
                histogram = null,
                values = null,
                valuesX = null,
                valuesY = null,
                unused = BigInteger.valueOf(1)
            ),
            total = UsageAmount(
                value = null,
                average = null,
                min = null,
                max = null,
                median = null,
                variance = null,
                accumulated = null,
                accumulatedDuration = null,
                histogram = null,
                values = null,
                valuesX = null,
                valuesY = null,
                unused = BigInteger.valueOf(2)
            )
        )
    ), ServiceAggregateUsageModel(
        key = ServiceAggregateKey(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = childTwo.id,
            providerId = provider.id,
            resourceId = resource.id
        ),
        lastUpdate = now,
        epoch = 0L,
        exactAmounts = ServiceUsageAmounts(
            own = UsageAmount(
                value = null,
                average = null,
                min = null,
                max = null,
                median = null,
                variance = null,
                accumulated = null,
                accumulatedDuration = null,
                histogram = null,
                values = null,
                valuesX = null,
                valuesY = null,
                unused = BigInteger.valueOf(1)
            ),
            subtree = UsageAmount(
                value = null,
                average = null,
                min = null,
                max = null,
                median = null,
                variance = null,
                accumulated = null,
                accumulatedDuration = null,
                histogram = null,
                values = null,
                valuesX = null,
                valuesY = null,
                unused = BigInteger.valueOf(1)
            ),
            total = UsageAmount(
                value = null,
                average = null,
                min = null,
                max = null,
                median = null,
                variance = null,
                accumulated = null,
                accumulatedDuration = null,
                histogram = null,
                values = null,
                valuesX = null,
                valuesY = null,
                unused = BigInteger.valueOf(2)
            )
        )
    ), ServiceAggregateUsageModel(
        key = ServiceAggregateKey(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = grandchildOne.id,
            providerId = provider.id,
            resourceId = resource.id
        ),
        lastUpdate = now,
        epoch = 0L,
        exactAmounts = ServiceUsageAmounts(
            own = UsageAmount(
                value = null,
                average = null,
                min = null,
                max = null,
                median = null,
                variance = null,
                accumulated = null,
                accumulatedDuration = null,
                histogram = null,
                values = null,
                valuesX = null,
                valuesY = null,
                unused = BigInteger.valueOf(1)
            ),
            subtree = null,
            total = null
        )
    ), ServiceAggregateUsageModel(
        key = ServiceAggregateKey(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = grandchildTwo.id,
            providerId = provider.id,
            resourceId = resource.id
        ),
        lastUpdate = now,
        epoch = 0L,
        exactAmounts = ServiceUsageAmounts(
            own = UsageAmount(
                value = null,
                average = null,
                min = null,
                max = null,
                median = null,
                variance = null,
                accumulated = null,
                accumulatedDuration = null,
                histogram = null,
                values = null,
                valuesX = null,
                valuesY = null,
                unused = BigInteger.valueOf(1)
            ),
            subtree = null,
            total = null
        )
    )
    )

    private fun providerModel(accountsSpacesSupported: Boolean,
                              aggregationMode: FreeProvisionAggregationMode,
                              aggregationAlgorithm: AggregationAlgorithm?, usageMode: UsageMode?,
                              gridSpacing: Long?): ProviderModel {
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
            .aggregationSettings(AggregationSettings(aggregationMode, usageMode, gridSpacing))
            .aggregationAlgorithm(aggregationAlgorithm)
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
                                  ownAccumulatedUsage: BigInteger?, subtreeAccumulatedUsage: BigInteger?,
                                  totalAccumulatedUsage: BigInteger?, now: Instant): ServiceUsageModel {
        return ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = serviceId,
                providerId = providerId,
                resourceId = resourceId
            ),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = if (ownAccumulatedUsage != null) { UsageAmount(
                    value = null,
                    average = ownAccumulatedUsage,
                    min = ownAccumulatedUsage,
                    max = ownAccumulatedUsage,
                    median = ownAccumulatedUsage,
                    variance = BigInteger.ZERO,
                    accumulated = ownAccumulatedUsage,
                    accumulatedDuration = 1L,
                    histogram = listOf(HistogramBin(ownAccumulatedUsage, ownAccumulatedUsage, 1L)),
                    values = null,
                    valuesX = listOf(0L),
                    valuesY = listOf(ownAccumulatedUsage),
                    unused = null
                ) } else { null },
                subtree = if (subtreeAccumulatedUsage != null) { UsageAmount(
                    value = null,
                    average = subtreeAccumulatedUsage,
                    min = subtreeAccumulatedUsage,
                    max = subtreeAccumulatedUsage,
                    median = subtreeAccumulatedUsage,
                    variance = BigInteger.ZERO,
                    accumulated = subtreeAccumulatedUsage,
                    accumulatedDuration = 1L,
                    histogram = listOf(HistogramBin(subtreeAccumulatedUsage, subtreeAccumulatedUsage, 1L)),
                    values = null,
                    valuesX = listOf(0L),
                    valuesY = listOf(subtreeAccumulatedUsage),
                    unused = null
                ) } else { null },
                total = if (totalAccumulatedUsage != null) { UsageAmount(
                    value = null,
                    average = totalAccumulatedUsage,
                    min = totalAccumulatedUsage,
                    max = totalAccumulatedUsage,
                    median = totalAccumulatedUsage,
                    variance = BigInteger.ZERO,
                    accumulated = totalAccumulatedUsage,
                    accumulatedDuration = 1L,
                    histogram = listOf(HistogramBin(totalAccumulatedUsage, totalAccumulatedUsage, 1L)),
                    values = null,
                    valuesX = listOf(0L),
                    valuesY = listOf(totalAccumulatedUsage),
                    unused = null
                ) } else { null }
            )
        )
    }

    private fun serviceUsageModelUnused(serviceId: Long, providerId: String, resourceId: String,
                                        ownUnused: BigInteger?, subtreeUnused: BigInteger?,
                                        totalUnused: BigInteger?, now: Instant): ServiceUsageModel {
        return ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = serviceId,
                providerId = providerId,
                resourceId = resourceId
            ),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = if (ownUnused != null) { UsageAmount(
                    value = null,
                    average = null,
                    min = null,
                    max = null,
                    median = null,
                    variance = null,
                    accumulated = null,
                    accumulatedDuration = null,
                    histogram = null,
                    values = null,
                    valuesX = null,
                    valuesY = null,
                    unused = ownUnused
                ) } else { null },
                subtree = if (subtreeUnused != null) { UsageAmount(
                    value = null,
                    average = null,
                    min = null,
                    max = null,
                    median = null,
                    variance = null,
                    accumulated = null,
                    accumulatedDuration = null,
                    histogram = null,
                    values = null,
                    valuesX = null,
                    valuesY = null,
                    unused = subtreeUnused
                ) } else { null },
                total = if (totalUnused != null) { UsageAmount(
                    value = null,
                    average = null,
                    min = null,
                    max = null,
                    median = null,
                    variance = null,
                    accumulated = null,
                    accumulatedDuration = null,
                    histogram = null,
                    values = null,
                    valuesX = null,
                    valuesY = null,
                    unused = totalUnused
                ) } else { null }
            )
        )
    }

    private inline fun <reified T : Metric> getJobMetric(name: String, providerKey: String): T? {
        return MetricRegistry.root().getMetric(MetricId(name,
            Labels.of("job", "RefreshServiceAggregatesJob", "provider", providerKey))) as? T
    }

    companion object {
        @JvmStatic
        fun aggregationAlgorithms() = listOf(
            Arguments.of(AggregationAlgorithm(false, 1000, AggregationQuotaQueryType.SCAN_SNAPSHOT, false, false,
                3, false, false)),
            Arguments.of(AggregationAlgorithm(false, 1000, AggregationQuotaQueryType.PAGINATE, false, false,
                3, false, false)),
            Arguments.of(AggregationAlgorithm(false, 1000, AggregationQuotaQueryType.SCAN_SNAPSHOT, false, false,
                3, true, false)),
            Arguments.of(AggregationAlgorithm(false, 1000, AggregationQuotaQueryType.PAGINATE, false, false,
                3, true, false)),
            Arguments.of(AggregationAlgorithm(false, 1000, AggregationQuotaQueryType.SCAN_SNAPSHOT, true, true,
                3, true, true)),
            Arguments.of(AggregationAlgorithm(false, 1000, AggregationQuotaQueryType.PAGINATE, true, true,
                3, true, true)),
            Arguments.of(AggregationAlgorithm(false, 1000, AggregationQuotaQueryType.SCAN_SNAPSHOT, true, true,
                3, false, true)),
            Arguments.of(AggregationAlgorithm(false, 1000, AggregationQuotaQueryType.PAGINATE, true, true,
                3, false, true))
        )
    }

}
