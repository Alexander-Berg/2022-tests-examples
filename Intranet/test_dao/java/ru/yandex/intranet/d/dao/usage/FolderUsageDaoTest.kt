package ru.yandex.intranet.d.dao.usage

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.usage.FolderUsageKey
import ru.yandex.intranet.d.model.usage.FolderUsageKeyWithEpoch
import ru.yandex.intranet.d.model.usage.FolderUsageModel
import ru.yandex.intranet.d.model.usage.HistogramBin
import ru.yandex.intranet.d.model.usage.UsageAmount
import ru.yandex.intranet.d.model.usage.UsagePoint
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Folder usage DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class FolderUsageDaoTest(@Autowired private val dao: FolderUsageDao,
                         @Autowired private val tableClient: YdbTableClient) {

    @Test
    fun testUpsert(): Unit = runBlocking {
        val model = FolderUsageModel(
            key = FolderUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                folderId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            ownUsage = UsageAmount(
                value = BigInteger.valueOf(1L),
                average = BigInteger.valueOf(2L),
                min = BigInteger.valueOf(3L),
                max = BigInteger.valueOf(4L),
                median = BigInteger.valueOf(5L),
                variance = BigInteger.valueOf(6L),
                accumulated = BigInteger.valueOf(7L),
                accumulatedDuration = 42L,
                histogram = listOf(HistogramBin(BigInteger.valueOf(8L), BigInteger.valueOf(9L), 10L)),
                values = listOf(UsagePoint(1L, BigInteger.valueOf(2L), null)),
                valuesX = listOf(1L),
                valuesY = listOf(BigInteger.valueOf(2L)),
                unused = null
            )
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertOneRetryable(txSession, model)
            }
            rwTxRetryable {
                dao.getById(txSession, model.key)
            }
        }
        Assertions.assertEquals(model, result)
    }

    @Test
    fun testUpsertMany(): Unit = runBlocking {
        val modelOne = FolderUsageModel(
            key = FolderUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                folderId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            ownUsage = UsageAmount(
                value = BigInteger.valueOf(1L),
                average = BigInteger.valueOf(2L),
                min = BigInteger.valueOf(3L),
                max = BigInteger.valueOf(4L),
                median = BigInteger.valueOf(5L),
                variance = BigInteger.valueOf(6L),
                accumulated = BigInteger.valueOf(7L),
                accumulatedDuration = 42L,
                histogram = listOf(HistogramBin(BigInteger.valueOf(8L), BigInteger.valueOf(9L), 10L)),
                values = listOf(UsagePoint(1L, BigInteger.valueOf(2L), null)),
                valuesX = listOf(1L),
                valuesY = listOf(BigInteger.valueOf(2L)),
                unused = null
            )
        )
        val modelTwo = FolderUsageModel(
            key = FolderUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                folderId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            ownUsage = UsageAmount(
                value = BigInteger.valueOf(1L),
                average = BigInteger.valueOf(2L),
                min = BigInteger.valueOf(3L),
                max = BigInteger.valueOf(4L),
                median = BigInteger.valueOf(5L),
                variance = BigInteger.valueOf(6L),
                accumulated = BigInteger.valueOf(7L),
                accumulatedDuration = 42L,
                histogram = listOf(HistogramBin(BigInteger.valueOf(8L), BigInteger.valueOf(9L), 10L)),
                values = listOf(UsagePoint(1L, BigInteger.valueOf(2L), null)),
                valuesX = listOf(1L),
                valuesY = listOf(BigInteger.valueOf(2L)),
                unused = null
            )
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                dao.getByIds(txSession, listOf(modelOne.key, modelTwo.key))
            }
        }
        Assertions.assertEquals(setOf(modelOne, modelTwo), result!!.toSet())
    }

    @Test
    fun testGetByFolder(): Unit = runBlocking {
        val models = mutableListOf<FolderUsageModel>()
        val folderId = UUID.randomUUID().toString()
        repeat(2100) {
            models.add(FolderUsageModel(
                key = FolderUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    folderId = folderId,
                    resourceId = UUID.randomUUID().toString()
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                ownUsage = UsageAmount(
                    value = BigInteger.valueOf(1L),
                    average = BigInteger.valueOf(2L),
                    min = BigInteger.valueOf(3L),
                    max = BigInteger.valueOf(4L),
                    median = BigInteger.valueOf(5L),
                    variance = BigInteger.valueOf(6L),
                    accumulated = BigInteger.valueOf(7L),
                    accumulatedDuration = 42L,
                    histogram = listOf(HistogramBin(BigInteger.valueOf(8L), BigInteger.valueOf(9L), 10L)),
                    values = listOf(UsagePoint(1L, BigInteger.valueOf(2L), null)),
                    valuesX = listOf(1L),
                    valuesY = listOf(BigInteger.valueOf(2L)),
                    unused = null
                )
            ))
        }
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getByFolder(txSession, Tenants.DEFAULT_TENANT_ID, folderId, 250)
            }
        }
        models.sortWith(Comparator
            .comparing<FolderUsageModel, String> { it.key.tenantId.id }
            .thenComparing { v -> v.key.folderId }
            .thenComparing { v -> v.key.resourceId })
        Assertions.assertEquals(models, result)
    }

    @Test
    fun testGetByFolders(): Unit = runBlocking {
        val models = mutableListOf<FolderUsageModel>()
        val folderIdOne = UUID.randomUUID().toString()
        val folderIdTwo = UUID.randomUUID().toString()
        val folderIdThree = UUID.randomUUID().toString()
        repeat(1000) {
            models.add(FolderUsageModel(
                key = FolderUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    folderId = folderIdOne,
                    resourceId = UUID.randomUUID().toString()
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                ownUsage = UsageAmount(
                    value = BigInteger.valueOf(1L),
                    average = BigInteger.valueOf(2L),
                    min = BigInteger.valueOf(3L),
                    max = BigInteger.valueOf(4L),
                    median = BigInteger.valueOf(5L),
                    variance = BigInteger.valueOf(6L),
                    accumulated = BigInteger.valueOf(7L),
                    accumulatedDuration = 42L,
                    histogram = listOf(HistogramBin(BigInteger.valueOf(8L), BigInteger.valueOf(9L), 10L)),
                    values = listOf(UsagePoint(1L, BigInteger.valueOf(2L), null)),
                    valuesX = listOf(1L),
                    valuesY = listOf(BigInteger.valueOf(2L)),
                    unused = null
                )
            ))
        }
        repeat(900) {
            models.add(FolderUsageModel(
                key = FolderUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    folderId = folderIdTwo,
                    resourceId = UUID.randomUUID().toString()
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                ownUsage = UsageAmount(
                    value = BigInteger.valueOf(1L),
                    average = BigInteger.valueOf(2L),
                    min = BigInteger.valueOf(3L),
                    max = BigInteger.valueOf(4L),
                    median = BigInteger.valueOf(5L),
                    variance = BigInteger.valueOf(6L),
                    accumulated = BigInteger.valueOf(7L),
                    accumulatedDuration = 42L,
                    histogram = listOf(HistogramBin(BigInteger.valueOf(8L), BigInteger.valueOf(9L), 10L)),
                    values = listOf(UsagePoint(1L, BigInteger.valueOf(2L), null)),
                    valuesX = listOf(1L),
                    valuesY = listOf(BigInteger.valueOf(2L)),
                    unused = null
                )
            ))
        }
        repeat(200) {
            models.add(FolderUsageModel(
                key = FolderUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    folderId = folderIdThree,
                    resourceId = UUID.randomUUID().toString()
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                ownUsage = UsageAmount(
                    value = BigInteger.valueOf(1L),
                    average = BigInteger.valueOf(2L),
                    min = BigInteger.valueOf(3L),
                    max = BigInteger.valueOf(4L),
                    median = BigInteger.valueOf(5L),
                    variance = BigInteger.valueOf(6L),
                    accumulated = BigInteger.valueOf(7L),
                    accumulatedDuration = 42L,
                    histogram = listOf(HistogramBin(BigInteger.valueOf(8L), BigInteger.valueOf(9L), 10L)),
                    values = listOf(UsagePoint(1L, BigInteger.valueOf(2L), null)),
                    valuesX = listOf(1L),
                    valuesY = listOf(BigInteger.valueOf(2L)),
                    unused = null
                )
            ))
        }
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getByFolders(txSession, Tenants.DEFAULT_TENANT_ID, listOf(folderIdOne, folderIdTwo, folderIdThree), 250)
            }
        }
        models.sortWith(Comparator
            .comparing<FolderUsageModel, String> { it.key.tenantId.id }
            .thenComparing { v -> v.key.folderId }
            .thenComparing { v -> v.key.resourceId })
        Assertions.assertEquals(models, result)
    }

    @Test
    fun getKeysForOlderEpochs(): Unit = runBlocking {
        val models = mutableListOf<FolderUsageModel>()
        val keys = mutableListOf<FolderUsageKeyWithEpoch>()
        val resourceId = UUID.randomUUID().toString()
        repeat(100) {
            val model = FolderUsageModel(
                key = FolderUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    folderId = UUID.randomUUID().toString(),
                    resourceId = resourceId
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 3,
                ownUsage = UsageAmount(
                    value = BigInteger.valueOf(1L),
                    average = BigInteger.valueOf(2L),
                    min = BigInteger.valueOf(3L),
                    max = BigInteger.valueOf(4L),
                    median = BigInteger.valueOf(5L),
                    variance = BigInteger.valueOf(6L),
                    accumulated = BigInteger.valueOf(7L),
                    accumulatedDuration = 42L,
                    histogram = listOf(HistogramBin(BigInteger.valueOf(8L), BigInteger.valueOf(9L), 10L)),
                    values = listOf(UsagePoint(1L, BigInteger.valueOf(2L), null)),
                    valuesX = listOf(1L),
                    valuesY = listOf(BigInteger.valueOf(2L)),
                    unused = null
                )
            )
            models.add(model)
        }
        repeat(2000) {
            val model = FolderUsageModel(
                key = FolderUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    folderId = UUID.randomUUID().toString(),
                    resourceId = resourceId
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                ownUsage = UsageAmount(
                    value = BigInteger.valueOf(1L),
                    average = BigInteger.valueOf(2L),
                    min = BigInteger.valueOf(3L),
                    max = BigInteger.valueOf(4L),
                    median = BigInteger.valueOf(5L),
                    variance = BigInteger.valueOf(6L),
                    accumulated = BigInteger.valueOf(7L),
                    accumulatedDuration = 42L,
                    histogram = listOf(HistogramBin(BigInteger.valueOf(8L), BigInteger.valueOf(9L), 10L)),
                    values = listOf(UsagePoint(1L, BigInteger.valueOf(2L), null)),
                    valuesX = listOf(1L),
                    valuesY = listOf(BigInteger.valueOf(2L)),
                    unused = null
                )
            )
            models.add(model)
            keys.add(FolderUsageKeyWithEpoch(key = model.key, epoch = model.epoch))
        }
        val firstResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getKeysForOlderEpochsFirstPage(txSession, Tenants.DEFAULT_TENANT_ID, resourceId, 3, 1000)
            }
        }
        val secondResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.getKeysForOlderEpochsNextPage(txSession, firstResult!!.value.nextFrom!!, 1000)
            }
        }
        val result = mutableListOf<FolderUsageKeyWithEpoch>()
        result.addAll(firstResult!!.value.keys)
        result.addAll(secondResult!!.value.keys)
        Assertions.assertEquals(keys.toSet(), result.toSet())
    }

    @Test
    fun testDelete(): Unit = runBlocking {
        val model = FolderUsageModel(
            key = FolderUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                folderId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            ownUsage = UsageAmount(
                value = BigInteger.valueOf(1L),
                average = BigInteger.valueOf(2L),
                min = BigInteger.valueOf(3L),
                max = BigInteger.valueOf(4L),
                median = BigInteger.valueOf(5L),
                variance = BigInteger.valueOf(6L),
                accumulated = BigInteger.valueOf(7L),
                accumulatedDuration = 42L,
                histogram = listOf(HistogramBin(BigInteger.valueOf(8L), BigInteger.valueOf(9L), 10L)),
                values = listOf(UsagePoint(1L, BigInteger.valueOf(2L), null)),
                valuesX = listOf(1L),
                valuesY = listOf(BigInteger.valueOf(2L)),
                unused = null
            )
        )
        val resultExists = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertOneRetryable(txSession, model)
            }
            rwTxRetryable {
                dao.getById(txSession, model.key)
            }
        }
        Assertions.assertEquals(model, resultExists)
        val resultNotExists = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.deleteByIdRetryable(txSession, model.key)
            }
            rwTxRetryable {
                dao.getById(txSession, model.key)
            }
        }
        Assertions.assertNull(resultNotExists)
    }

    @Test
    fun testDeleteMany(): Unit = runBlocking {
        val modelOne = FolderUsageModel(
            key = FolderUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                folderId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            ownUsage = UsageAmount(
                value = BigInteger.valueOf(1L),
                average = BigInteger.valueOf(2L),
                min = BigInteger.valueOf(3L),
                max = BigInteger.valueOf(4L),
                median = BigInteger.valueOf(5L),
                variance = BigInteger.valueOf(6L),
                accumulated = BigInteger.valueOf(7L),
                accumulatedDuration = 42L,
                histogram = listOf(HistogramBin(BigInteger.valueOf(8L), BigInteger.valueOf(9L), 10L)),
                values = listOf(UsagePoint(1L, BigInteger.valueOf(2L), null)),
                valuesX = listOf(1L),
                valuesY = listOf(BigInteger.valueOf(2L)),
                unused = null
            )
        )
        val modelTwo = FolderUsageModel(
            key = FolderUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                folderId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            ownUsage = UsageAmount(
                value = BigInteger.valueOf(1L),
                average = BigInteger.valueOf(2L),
                min = BigInteger.valueOf(3L),
                max = BigInteger.valueOf(4L),
                median = BigInteger.valueOf(5L),
                variance = BigInteger.valueOf(6L),
                accumulated = BigInteger.valueOf(7L),
                accumulatedDuration = 42L,
                histogram = listOf(HistogramBin(BigInteger.valueOf(8L), BigInteger.valueOf(9L), 10L)),
                values = listOf(UsagePoint(1L, BigInteger.valueOf(2L), null)),
                valuesX = listOf(1L),
                valuesY = listOf(BigInteger.valueOf(2L)),
                unused = null
            )
        )
        val resultExists = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                dao.getByIds(txSession, listOf(modelOne.key, modelTwo.key))
            }
        }
        Assertions.assertEquals(setOf(modelOne, modelTwo), resultExists!!.toSet())
        val resultNotExists = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.deleteByIdsRetryable(txSession, listOf(modelOne.key, modelTwo.key))
            }
            rwTxRetryable {
                dao.getByIds(txSession, listOf(modelOne.key, modelTwo.key))
            }
        }
        Assertions.assertTrue(resultNotExists!!.isEmpty())
    }

    @Test
    fun getKeysForOlderEpochsMultiResource(): Unit = runBlocking {
        val models = mutableListOf<FolderUsageModel>()
        val keys = mutableListOf<FolderUsageKeyWithEpoch>()
        val resourceIdOne = UUID.randomUUID().toString()
        val resourceIdTwo = UUID.randomUUID().toString()
        repeat(100) {
            val model = FolderUsageModel(
                key = FolderUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    folderId = UUID.randomUUID().toString(),
                    resourceId = resourceIdOne
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 3,
                ownUsage = UsageAmount(
                    value = BigInteger.valueOf(1L),
                    average = BigInteger.valueOf(2L),
                    min = BigInteger.valueOf(3L),
                    max = BigInteger.valueOf(4L),
                    median = BigInteger.valueOf(5L),
                    variance = BigInteger.valueOf(6L),
                    accumulated = BigInteger.valueOf(7L),
                    accumulatedDuration = 42L,
                    histogram = listOf(HistogramBin(BigInteger.valueOf(8L), BigInteger.valueOf(9L), 10L)),
                    values = listOf(UsagePoint(1L, BigInteger.valueOf(2L), null)),
                    valuesX = listOf(1L),
                    valuesY = listOf(BigInteger.valueOf(2L)),
                    unused = null
                )
            )
            models.add(model)
        }
        repeat(1000) {
            val model = FolderUsageModel(
                key = FolderUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    folderId = UUID.randomUUID().toString(),
                    resourceId = resourceIdOne
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                ownUsage = UsageAmount(
                    value = BigInteger.valueOf(1L),
                    average = BigInteger.valueOf(2L),
                    min = BigInteger.valueOf(3L),
                    max = BigInteger.valueOf(4L),
                    median = BigInteger.valueOf(5L),
                    variance = BigInteger.valueOf(6L),
                    accumulated = BigInteger.valueOf(7L),
                    accumulatedDuration = 42L,
                    histogram = listOf(HistogramBin(BigInteger.valueOf(8L), BigInteger.valueOf(9L), 10L)),
                    values = listOf(UsagePoint(1L, BigInteger.valueOf(2L), null)),
                    valuesX = listOf(1L),
                    valuesY = listOf(BigInteger.valueOf(2L)),
                    unused = null
                )
            )
            models.add(model)
            keys.add(FolderUsageKeyWithEpoch(key = model.key, epoch = model.epoch))
        }
        repeat(1200) {
            val model = FolderUsageModel(
                key = FolderUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    folderId = UUID.randomUUID().toString(),
                    resourceId = resourceIdTwo
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                ownUsage = UsageAmount(
                    value = BigInteger.valueOf(1L),
                    average = BigInteger.valueOf(2L),
                    min = BigInteger.valueOf(3L),
                    max = BigInteger.valueOf(4L),
                    median = BigInteger.valueOf(5L),
                    variance = BigInteger.valueOf(6L),
                    accumulated = BigInteger.valueOf(7L),
                    accumulatedDuration = 42L,
                    histogram = listOf(HistogramBin(BigInteger.valueOf(8L), BigInteger.valueOf(9L), 10L)),
                    values = listOf(UsagePoint(1L, BigInteger.valueOf(2L), null)),
                    valuesX = listOf(1L),
                    valuesY = listOf(BigInteger.valueOf(2L)),
                    unused = null
                )
            )
            models.add(model)
            keys.add(FolderUsageKeyWithEpoch(key = model.key, epoch = model.epoch))
        }
        val firstResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getKeysForOlderEpochsMultiResourceFirstPage(txSession, Tenants.DEFAULT_TENANT_ID,
                    listOf(resourceIdOne, resourceIdTwo), 3, 1000)
            }
        }
        val secondResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.getKeysForOlderEpochsMultiResourceNextPage(txSession, firstResult!!.value.nextFrom!!,
                    listOf(resourceIdOne, resourceIdTwo), 3, 1000)
            }
        }
        val thirdResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.getKeysForOlderEpochsMultiResourceNextPage(txSession, secondResult!!.value.nextFrom!!,
                    listOf(resourceIdOne, resourceIdTwo), 3, 1000)
            }
        }
        val result = mutableListOf<FolderUsageKeyWithEpoch>()
        result.addAll(firstResult!!.value.keys)
        result.addAll(secondResult!!.value.keys)
        result.addAll(thirdResult!!.value.keys)
        Assertions.assertEquals(keys.toSet(), result.toSet())
    }

}
