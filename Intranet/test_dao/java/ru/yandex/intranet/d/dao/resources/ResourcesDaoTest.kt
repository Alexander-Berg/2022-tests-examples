package ru.yandex.intranet.d.dao.resources

import com.yandex.ydb.table.transaction.TransactionMode
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import reactor.util.function.Tuple2
import reactor.util.function.Tuples
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestProviders.YP_ID
import ru.yandex.intranet.d.TestResourceTypes.YDP_HDD
import ru.yandex.intranet.d.TestResourceTypes.YP_HDD
import ru.yandex.intranet.d.TestResourceTypes.YP_SSD
import ru.yandex.intranet.d.TestResources.CLAUD1_RAM
import ru.yandex.intranet.d.TestResources.YDB_RAM_SAS
import ru.yandex.intranet.d.TestResources.YP_HDD_IVA
import ru.yandex.intranet.d.TestResources.YP_HDD_MAN
import ru.yandex.intranet.d.TestResources.YP_HDD_READ_ONLY
import ru.yandex.intranet.d.TestResources.YP_SSD_MAN
import ru.yandex.intranet.d.TestResources.YP_VIRTUAL
import ru.yandex.intranet.d.TestSegmentations.YP_LOCATION_VLA
import ru.yandex.intranet.d.dao.Tenants.DEFAULT_TENANT_ID
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.impl.YdbRetry
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.metrics.YdbMetrics
import ru.yandex.intranet.d.model.TenantId
import ru.yandex.intranet.d.model.resources.ResourceModel
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Resources DAO tests
 *
 * @author Aleksey Chizhikov <achizhikov></achizhikov>@yandex-team.ru>
 */
@IntegrationTest
class ResourcesDaoTest(
    @Autowired
    private val resourcesDao: ResourcesDao,
    @Autowired
    private val ydbTableClient: YdbTableClient,
    @Autowired
    private val ydbMetrics: YdbMetrics,
    @Value("\${ydb.transactionRetries}")
    private val transactionRetries: Long
) {
    @Test
    fun resourceWithNullVirtualFlagTest() {
        val resource = ydbTableClient
            .usingSessionMonoRetryable { ydbSession ->
                resourcesDao
                    .getById(
                        ydbSession.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        "fb2321d7-1b2b-4567-92b9-a2ded3722fc6",
                        DEFAULT_TENANT_ID)
                    .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))
            }.block()!!
            .orElseThrow()
        assertFalse(resource.isVirtual) // 'virtual' must be false if null in db
    }

    @Test
    fun allByIdsTest() {
        val requestIdsWithTenants = listOf(
            YP_HDD_MAN, YP_SSD_MAN, YDB_RAM_SAS, YP_HDD_READ_ONLY, YP_HDD_IVA, CLAUD1_RAM, YP_VIRTUAL)
            .map { resourceId: String -> Tuples.of(resourceId, DEFAULT_TENANT_ID) }
        var result = ydbTableClient
            .usingSessionMonoRetryable { ydbSession ->
                resourcesDao
                    .getAllByIds(
                        ydbSession.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        requestIdsWithTenants)
                    .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))
            }.block()!!
        assertEquals(7, result.size)
        val resultIds = result.stream().map { obj: ResourceModel -> obj.id }.collect(Collectors.toSet())
        val requestIds = requestIdsWithTenants
            .map { obj: Tuple2<String, TenantId?> -> obj.t1 }
            .toSet()
        assertEquals(requestIds, resultIds)
        val badRequestIdsWithTenants = Stream.of("not id", "not id too", "not id either")
            .map { resourceId: String -> Tuples.of(resourceId, DEFAULT_TENANT_ID) }
            .collect(Collectors.toList())
        result = ydbTableClient
            .usingSessionMonoRetryable { ydbSession ->
                resourcesDao
                    .getAllByIds(
                        ydbSession.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        badRequestIdsWithTenants)
                    .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))
            }.block()!!
        assertEquals(0, result.size)
    }

    @Test
    fun manyResourcesByIdsTest() {
        val numberOfResources = 2500
        val resourcesIds = listOf(0 until numberOfResources).flatten().map { UUID.randomUUID().toString() }
        val resources = listOf(0 until numberOfResources)
            .flatten()
            .map { i ->
                ResourceModel.builder()
                    .id(resourcesIds[i])
                    .tenantId(DEFAULT_TENANT_ID)
                    .version(0L)
                    .key("test-a-lot-$i")
                    .nameEn("Test a lot $i")
                    .nameRu("Test a lot $i")
                    .descriptionEn("Test a lot")
                    .descriptionRu("Test a lot")
                    .deleted(false)
                    .unitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                    .providerId(YP_ID)
                    .resourceUnits(ResourceUnitsModel(setOf("b15101c2-da50-4d6f-9a8e-b90160871b0a"),
                        "b15101c2-da50-4d6f-9a8e-b90160871b0a", null))
                    .managed(true)
                    .orderable(true)
                    .readOnly(false)
                    .baseUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                    .build()
            }
        ydbTableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                resourcesDao.upsertResourcesRetryable(txSession, resources)
            }
        }
            .block()
        val result = ydbTableClient.usingSessionMonoRetryable { ydbSession ->
            resourcesDao
                .getAllByIds(
                    ydbSession.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                    resourcesIds.map { id: String -> Tuples.of(id, DEFAULT_TENANT_ID) })
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))
        }.block()!!
        assertEquals(numberOfResources, result.size)
        val resultIds = result.map(ResourceModel::getId)
        assertEquals(resourcesIds.toSet(), resultIds.toSet())
    }

    @Test
    fun getByProviderTypeAndSegmentsTest() {
        val ypHdd = ydbTableClient.usingSessionMonoRetryable { ydbSession ->
            resourcesDao
                .getByProviderResourceTypeAndSegments(
                    ydbSession.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                    DEFAULT_TENANT_ID,
                    YP_ID,
                    YP_HDD,
                    setOf(YP_LOCATION_VLA),
                    null,
                    100,
                    true
                )
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))
        }.block()!!

        assertTrue(ypHdd.isNotEmpty())
        assertTrue(ypHdd.all { resource -> resource.resourceTypeId == YP_HDD})
        assertTrue(ypHdd.all { resource -> resource.segments.map { s -> s.segmentId }.contains(YP_LOCATION_VLA) })
    }

    @Test
    fun getByProviderTypeAndSegmentsWithoutSegmentsTest() {
        val ypHdd = ydbTableClient.usingSessionMonoRetryable { ydbSession ->
            resourcesDao
                .getByProviderResourceTypeAndSegments(
                    ydbSession.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                    DEFAULT_TENANT_ID,
                    YP_ID,
                    YP_HDD,
                    null,
                    null,
                    100,
                    true
                )
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))
        }.block()!!

        assertTrue(ypHdd.isNotEmpty())
        assertTrue(ypHdd.all { resource -> resource.resourceTypeId == YP_HDD})

        val ypSsd = ydbTableClient.usingSessionMonoRetryable { ydbSession ->
            resourcesDao
                .getByProviderResourceTypeAndSegments(
                    ydbSession.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                    DEFAULT_TENANT_ID,
                    YP_ID,
                    YP_SSD,
                    emptySet(),
                    null,
                    100,
                    true
                )
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))
        }.block()!!

        assertTrue(ypSsd.isNotEmpty())
        assertTrue(ypSsd.all { resource -> resource.resourceTypeId == YP_SSD})
    }

    @Test
    fun getByProviderTypeAndSegmentsNotFoundTest() {
        val ypHdd = ydbTableClient.usingSessionMonoRetryable { ydbSession ->
            resourcesDao
                .getByProviderResourceTypeAndSegments(
                    ydbSession.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                    DEFAULT_TENANT_ID,
                    YP_ID,
                    YDP_HDD,
                    null,
                    null,
                    100,
                    true
                )
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))
        }.block()!!

        assertTrue(ypHdd.isEmpty())
    }

    @Test
    fun testGetAllByProviders(): Unit = runBlocking {
        val models = mutableListOf<ResourceModel>()
        val providerIdOne = UUID.randomUUID().toString()
        val providerIdTwo = UUID.randomUUID().toString()
        repeat(1000) {
            models.add(ResourceModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(DEFAULT_TENANT_ID)
                .version(0L)
                .key("test")
                .nameEn("Test")
                .nameRu("Test")
                .descriptionEn("Test")
                .descriptionRu("Test")
                .deleted(false)
                .unitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                .providerId(providerIdOne)
                .resourceUnits(ResourceUnitsModel(setOf("b15101c2-da50-4d6f-9a8e-b90160871b0a"),
                    "b15101c2-da50-4d6f-9a8e-b90160871b0a", null))
                .managed(true)
                .orderable(true)
                .readOnly(false)
                .baseUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                .build())
        }
        repeat(1100) {
            models.add(ResourceModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(DEFAULT_TENANT_ID)
                .version(0L)
                .key("test")
                .nameEn("Test")
                .nameRu("Test")
                .descriptionEn("Test")
                .descriptionRu("Test")
                .deleted(false)
                .unitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                .providerId(providerIdTwo)
                .resourceUnits(ResourceUnitsModel(setOf("b15101c2-da50-4d6f-9a8e-b90160871b0a"),
                    "b15101c2-da50-4d6f-9a8e-b90160871b0a", null))
                .managed(true)
                .orderable(true)
                .readOnly(false)
                .baseUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                .build())
        }
        val result = dbSessionRetryable(ydbTableClient) {
            rwTxRetryable {
                resourcesDao.upsertResourcesRetryable(txSession, models).awaitSingleOrNull()
            }
            rwTxRetryable {
                resourcesDao.getAllByProviders(txSession, listOf(providerIdOne, providerIdTwo), DEFAULT_TENANT_ID, false)
                    .awaitSingle()
            }
        }!!
        assertEquals(models.map { it.id }.toSet(), result.map { it.id }.toSet())
    }

    @Test
    fun testGetAllByProviderResourceType(): Unit = runBlocking {
        val models = mutableListOf<ResourceModel>()
        val providerId = UUID.randomUUID().toString()
        val resourceTypeId = UUID.randomUUID().toString()
        repeat(2100) {
            models.add(ResourceModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(DEFAULT_TENANT_ID)
                .version(0L)
                .key("test")
                .nameEn("Test")
                .nameRu("Test")
                .descriptionEn("Test")
                .descriptionRu("Test")
                .deleted(false)
                .unitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                .providerId(providerId)
                .resourceUnits(ResourceUnitsModel(setOf("b15101c2-da50-4d6f-9a8e-b90160871b0a"),
                    "b15101c2-da50-4d6f-9a8e-b90160871b0a", null))
                .managed(true)
                .orderable(true)
                .readOnly(false)
                .baseUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                .resourceTypeId(resourceTypeId)
                .build())
        }
        val result = dbSessionRetryable(ydbTableClient) {
            rwTxRetryable {
                resourcesDao.upsertResourcesRetryable(txSession, models).awaitSingleOrNull()
            }
            rwTxRetryable {
                resourcesDao.getAllByProviderResourceType(txSession, providerId, resourceTypeId,
                    DEFAULT_TENANT_ID, false).awaitSingle()
            }
        }!!
        assertEquals(models.map { it.id }.toSet(), result.map { it.id }.toSet())
    }

}
