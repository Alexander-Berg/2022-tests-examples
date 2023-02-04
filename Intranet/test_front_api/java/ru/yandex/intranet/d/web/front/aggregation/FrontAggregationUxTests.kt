package ru.yandex.intranet.d.web.front.aggregation

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.UnitIds
import ru.yandex.intranet.d.UnitsEnsembleIds
import ru.yandex.intranet.d.dao.accounts.AccountsDao
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao
import ru.yandex.intranet.d.dao.folders.FolderDao
import ru.yandex.intranet.d.dao.providers.ProvidersDao
import ru.yandex.intranet.d.dao.quotas.QuotasDao
import ru.yandex.intranet.d.dao.resources.ResourcesDao
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao
import ru.yandex.intranet.d.dao.services.ServicesDao
import ru.yandex.intranet.d.dao.usage.ServiceUsageDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.providers.FreeProvisionAggregationMode
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.accountModel
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.accountQuotaModel
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.folderModel
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.providerModel
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.quotaModel
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.resourceModel
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.resourceTypeModel
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.serviceModel
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.serviceUsageModel
import java.time.Instant

@IntegrationTest
class FrontAggregationUxTests @Autowired constructor(
    private val providersDao: ProvidersDao,
    private val resourceTypesDao: ResourceTypesDao,
    private val resourcesDao: ResourcesDao,
    private val servicesDao: ServicesDao,
    private val folderDao: FolderDao,
    private val quotasDao: QuotasDao,
    private val accountsDao: AccountsDao,
    private val accountsQuotasDao: AccountsQuotasDao,
    private val tableClient: YdbTableClient,
    private val serviceUsageDao: ServiceUsageDao,
    private val helper: FrontAggregationTestHelper
) {

    @Test
    fun testServiceTotalsRoundings(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                servicesDao.upsertRecipeManyRetryable(txSession, listOf(root)).awaitSingleOrNull()
            }
        }
        val folderRoot = folderModel(root.id)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                folderDao.upsertAllRetryable(txSession, listOf(folderRoot)).awaitSingleOrNull()
            }
        }
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                providersDao.upsertProviderRetryable(txSession, provider).awaitSingleOrNull()
            }
        }
        val resourceType = resourceTypeModel(provider.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType).awaitSingleOrNull()
            }
        }
        val resource = resourceModel(
            provider.id, "test", resourceType.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES, UnitIds.KILOBYTES),
            UnitIds.BYTES, UnitIds.BYTES, null
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourcesDao.upsertResourceRetryable(txSession, resource).awaitSingleOrNull()
            }
        }
        val accountRoot = accountModel(provider.id, null, folderRoot.id, "test")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsDao.upsertAllRetryable(txSession, listOf(accountRoot)).awaitSingleOrNull()
            }
        }
        val quotaRoot = quotaModel(provider.id, resource.id, folderRoot.id, 6000L, 1000L)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                quotasDao.upsertAllRetryable(txSession, listOf(quotaRoot)).awaitSingleOrNull()
            }
        }
        val provisionRoot = accountQuotaModel(provider.id, resource.id, folderRoot.id, accountRoot.id, 5000L, 700L)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsQuotasDao.upsertAllRetryable(txSession, listOf(provisionRoot)).awaitSingleOrNull()
            }
        }
        val now = Instant.now()
        val usageRoot = serviceUsageModel(root.id, provider.id, resource.id, now,
            ownTimeSeries = mapOf(0L to 0L, 300L to 1L, 600L to 2L), subtreeTimeSeries = null, totalTimeSeries = null)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, listOf(usageRoot))
        }}
        helper.aggregate(provider.id)
        val resultRoot = helper.findServiceTotals(root.id)
        assertEquals(1, resultRoot.aggregates.size)
        assertEquals(root.id, resultRoot.aggregates[0].serviceId)
        assertEquals(1, resultRoot.aggregates[0].providers.size)
        assertEquals(provider.id, resultRoot.aggregates[0].providers[0].providerId)
        assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes.size)
        assertEquals(resourceType.id, resultRoot.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes[0].resources.size)
        assertEquals(resource.id, resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId
        )

        assertEquals(resource.id,
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        assertEquals("6",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.readableUnit)
        assertEquals("1",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.readableUnit)
        assertEquals("5",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.readableUnit)
        assertEquals("700",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.readableAmount)
        assertEquals("B",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.readableUnit)
        assertEquals("4",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.readableUnit)
        assertEquals("5",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.readableUnit)

        assertEquals("1",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.average!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.average!!.readableUnit)
        assertEquals("0",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.min!!.readableAmount)
        assertEquals("B",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.min!!.readableUnit)
        assertEquals("2",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.max!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.max!!.readableUnit)
        assertEquals("1",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.median!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.median!!.readableUnit)
        assertEquals("1",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.stdev!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.stdev!!.readableUnit)
        assertEquals("4",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.underutilizedEstimation!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.underutilizedEstimation!!.readableUnit)

        assertEquals("6",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].amounts.quota?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].amounts.quota?.readableUnit)
        assertEquals("1",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].amounts.balance?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].amounts.balance?.readableUnit)
        assertEquals("5",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].amounts.provided?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].amounts.provided?.readableUnit)
        assertEquals("700",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].amounts.allocated?.readableAmount)
        assertEquals("B",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].amounts.allocated?.readableUnit)
        assertEquals("4",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].amounts.unallocated?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].amounts.unallocated?.readableUnit)
        assertEquals("5",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].amounts.transferable?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].amounts.transferable?.readableUnit)

        assertEquals("1",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].usage!!.average!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].usage!!.average!!.readableUnit)
        assertEquals("0",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].usage!!.min!!.readableAmount)
        assertEquals("B",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].usage!!.min!!.readableUnit)
        assertEquals("2",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].usage!!.max!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].usage!!.max!!.readableUnit)
        assertEquals("1",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].usage!!.median!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].usage!!.median!!.readableUnit)
        assertEquals("1",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].usage!!.stdev!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].usage!!.stdev!!.readableUnit)
        assertEquals("4",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].usage!!.underutilizedEstimation!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].usage!!.underutilizedEstimation!!.readableUnit)
    }

    @Test
    fun testSubtreeTotalRounding(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                servicesDao.upsertRecipeManyRetryable(txSession, listOf(root)).awaitSingleOrNull()
            }
        }
        val folderRoot = folderModel(root.id)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                folderDao.upsertAllRetryable(txSession, listOf(folderRoot)).awaitSingleOrNull()
            }
        }
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                providersDao.upsertProviderRetryable(txSession, provider).awaitSingleOrNull()
            }
        }
        val resourceType = resourceTypeModel(provider.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType).awaitSingleOrNull()
            }
        }
        val resource = resourceModel(
            provider.id, "test", resourceType.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES, UnitIds.KILOBYTES),
            UnitIds.BYTES, UnitIds.BYTES, null
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourcesDao.upsertResourceRetryable(txSession, resource).awaitSingleOrNull()
            }
        }
        val accountRoot = accountModel(provider.id, null, folderRoot.id, "test")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsDao.upsertAllRetryable(txSession, listOf(accountRoot)).awaitSingleOrNull()
            }
        }
        val quotaRoot = quotaModel(provider.id, resource.id, folderRoot.id, 6000L, 1000L)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                quotasDao.upsertAllRetryable(txSession, listOf(quotaRoot)).awaitSingleOrNull()
            }
        }
        val provisionRoot = accountQuotaModel(provider.id, resource.id, folderRoot.id, accountRoot.id, 5000L, 700L)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsQuotasDao.upsertAllRetryable(txSession, listOf(provisionRoot)).awaitSingleOrNull()
            }
        }
        val now = Instant.now()
        val usageRoot = serviceUsageModel(root.id, provider.id, resource.id, now,
            ownTimeSeries = mapOf(0L to 0L, 300L to 1L, 600L to 2L), subtreeTimeSeries = null, totalTimeSeries = null)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, listOf(usageRoot))
        }}
        helper.aggregate(provider.id)
        val resultRoot = helper.findSubtreeTotals(root.id, resource.id)!!
        assertEquals(1, resultRoot.aggregates.size)
        assertEquals(root.id, resultRoot.aggregates[0].serviceId)
        assertEquals(1, resultRoot.aggregates[0].providers.size)
        assertEquals(provider.id, resultRoot.aggregates[0].providers[0].providerId)
        assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes.size)
        assertEquals(resourceType.id, resultRoot.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes[0].resources.size)
        assertEquals(resource.id, resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId
        )

        assertEquals(resource.id,
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        assertEquals("6",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.readableUnit)
        assertEquals("1",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.readableUnit)
        assertEquals("5",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.readableUnit)
        assertEquals("700",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.readableAmount)
        assertEquals("B",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.readableUnit)
        assertEquals("4",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.readableUnit)
        assertEquals("5",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.readableUnit)

        assertEquals("1",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.average!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.average!!.readableUnit)
        assertEquals("0",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.min!!.readableAmount)
        assertEquals("B",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.min!!.readableUnit)
        assertEquals("2",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.max!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.max!!.readableUnit)
        assertEquals("1",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.median!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.median!!.readableUnit)
        assertEquals("1",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.stdev!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.stdev!!.readableUnit)
        assertEquals("4",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.underutilizedEstimation!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.underutilizedEstimation!!.readableUnit)
    }

    @Test
    fun testRankSubtreeRounding(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                servicesDao.upsertRecipeManyRetryable(txSession, listOf(root)).awaitSingleOrNull()
            }
        }
        val folderRoot = folderModel(root.id)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                folderDao.upsertAllRetryable(txSession, listOf(folderRoot)).awaitSingleOrNull()
            }
        }
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                providersDao.upsertProviderRetryable(txSession, provider).awaitSingleOrNull()
            }
        }
        val resourceType = resourceTypeModel(provider.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType).awaitSingleOrNull()
            }
        }
        val resource = resourceModel(
            provider.id, "test", resourceType.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES, UnitIds.KILOBYTES),
            UnitIds.BYTES, UnitIds.BYTES, null
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourcesDao.upsertResourceRetryable(txSession, resource).awaitSingleOrNull()
            }
        }
        val accountRoot = accountModel(provider.id, null, folderRoot.id, "test")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsDao.upsertAllRetryable(txSession, listOf(accountRoot)).awaitSingleOrNull()
            }
        }
        val quotaRoot = quotaModel(provider.id, resource.id, folderRoot.id, 6000L, 1000L)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                quotasDao.upsertAllRetryable(txSession, listOf(quotaRoot)).awaitSingleOrNull()
            }
        }
        val provisionRoot = accountQuotaModel(provider.id, resource.id, folderRoot.id, accountRoot.id, 5000L, 700L)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsQuotasDao.upsertAllRetryable(txSession, listOf(provisionRoot)).awaitSingleOrNull()
            }
        }
        val now = Instant.now()
        val usageRoot = serviceUsageModel(root.id, provider.id, resource.id, now,
            ownTimeSeries = mapOf(0L to 0L, 300L to 1L, 600L to 2L), subtreeTimeSeries = null, totalTimeSeries = null)
        dbSessionRetryable(tableClient) { rwTxRetryable {
            serviceUsageDao.upsertManyRetryable(txSession, listOf(usageRoot))
        }}

        helper.aggregate(provider.id)
        val resultRoot = helper.rankSubtreeAmounts(root.id, resource.id, from = null, limit = 100)!!

        assertEquals(1, resultRoot.aggregates.size)
        assertEquals(root.id, resultRoot.aggregates[0].serviceId)
        assertEquals(1, resultRoot.aggregates[0].folders.size)
        assertEquals(folderRoot.id, resultRoot.aggregates[0].folders[0].id)
        assertEquals(1, resultRoot.aggregates[0].providers.size)
        assertEquals(provider.id, resultRoot.aggregates[0].providers[0].providerId)
        assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes.size)
        assertEquals(resourceType.id, resultRoot.aggregates[0].providers[0].resourceTypes[0].resourceTypeId)
        assertEquals(1, resultRoot.aggregates[0].providers[0].resourceTypes[0].resources.size)

        assertEquals(resource.id,
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].resourceId)
        assertEquals("6",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.quota?.readableUnit)
        assertEquals("1",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.balance?.readableUnit)
        assertEquals("5",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.provided?.readableUnit)
        assertEquals("700",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.readableAmount)
        assertEquals("B",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.allocated?.readableUnit)
        assertEquals("4",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.unallocated?.readableUnit)
        assertEquals("5",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].amounts.transferable?.readableUnit)

        assertEquals("1",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.average!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.average!!.readableUnit)
        assertEquals("0",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.min!!.readableAmount)
        assertEquals("B",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.min!!.readableUnit)
        assertEquals("2",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.max!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.max!!.readableUnit)
        assertEquals("1",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.median!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.median!!.readableUnit)
        assertEquals("1",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.stdev!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.stdev!!.readableUnit)
        assertEquals("4",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.underutilizedEstimation!!.readableAmount)
        assertEquals("KB",
            resultRoot.aggregates[0].providers[0].resourceTypes[0].resources[0].usage!!.underutilizedEstimation!!.readableUnit)
    }
}
