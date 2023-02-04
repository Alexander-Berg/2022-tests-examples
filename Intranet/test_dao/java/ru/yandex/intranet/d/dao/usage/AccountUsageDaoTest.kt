package ru.yandex.intranet.d.dao.usage

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.usage.AccountUsageKey
import ru.yandex.intranet.d.model.usage.AccountUsageKeyWithEpoch
import ru.yandex.intranet.d.model.usage.AccountUsageModel
import ru.yandex.intranet.d.model.usage.HistogramBin
import ru.yandex.intranet.d.model.usage.UsageAmount
import ru.yandex.intranet.d.model.usage.UsagePoint
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Account usage DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class AccountUsageDaoTest(@Autowired private val dao: AccountUsageDao,
                          @Autowired private val tableClient: YdbTableClient) {

    @Test
    fun testUpsert(): Unit = runBlocking {
        val model = AccountUsageModel(
            key = AccountUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                accountId = UUID.randomUUID().toString(),
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
        val modelOne = AccountUsageModel(
            key = AccountUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                accountId = UUID.randomUUID().toString(),
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
        val modelTwo = AccountUsageModel(
            key = AccountUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                accountId = UUID.randomUUID().toString(),
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
    fun testGetByAccount(): Unit = runBlocking {
        val models = mutableListOf<AccountUsageModel>()
        val accountId = UUID.randomUUID().toString()
        repeat(2100) {
            models.add(AccountUsageModel(
                key = AccountUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    accountId = accountId,
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
                dao.getByAccount(txSession, Tenants.DEFAULT_TENANT_ID, accountId, 250)
            }
        }
        models.sortWith(Comparator
            .comparing<AccountUsageModel, String> { it.key.tenantId.id }
            .thenComparing { v -> v.key.accountId }
            .thenComparing { v -> v.key.resourceId })
        Assertions.assertEquals(models, result)
    }

    @Test
    fun testGetByAccounts(): Unit = runBlocking {
        val models = mutableListOf<AccountUsageModel>()
        val accountIdOne = UUID.randomUUID().toString()
        val accountIdTwo = UUID.randomUUID().toString()
        val accountIdThree = UUID.randomUUID().toString()
        repeat(1000) {
            models.add(AccountUsageModel(
                key = AccountUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    accountId = accountIdOne,
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
            models.add(AccountUsageModel(
                key = AccountUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    accountId = accountIdTwo,
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
            models.add(AccountUsageModel(
                key = AccountUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    accountId = accountIdThree,
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
                dao.getByAccounts(txSession, Tenants.DEFAULT_TENANT_ID, listOf(accountIdOne, accountIdTwo, accountIdThree), 250)
            }
        }
        models.sortWith(Comparator
            .comparing<AccountUsageModel, String> { it.key.tenantId.id }
            .thenComparing { v -> v.key.accountId }
            .thenComparing { v -> v.key.resourceId })
        Assertions.assertEquals(models, result)
    }

    @Test
    fun getKeysForOlderEpochs(): Unit = runBlocking {
        val models = mutableListOf<AccountUsageModel>()
        val keys = mutableListOf<AccountUsageKeyWithEpoch>()
        val resourceId = UUID.randomUUID().toString()
        repeat(100) {
            val model = AccountUsageModel(
                key = AccountUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    accountId = UUID.randomUUID().toString(),
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
            val model = AccountUsageModel(
                key = AccountUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    accountId = UUID.randomUUID().toString(),
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
            keys.add(AccountUsageKeyWithEpoch(key = model.key, epoch = model.epoch))
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
        val result = mutableListOf<AccountUsageKeyWithEpoch>()
        result.addAll(firstResult!!.value.keys)
        result.addAll(secondResult!!.value.keys)
        Assertions.assertEquals(keys.toSet(), result.toSet())
    }

    @Test
    fun testDelete(): Unit = runBlocking {
        val model = AccountUsageModel(
            key = AccountUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                accountId = UUID.randomUUID().toString(),
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
        val modelOne = AccountUsageModel(
            key = AccountUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                accountId = UUID.randomUUID().toString(),
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
        val modelTwo = AccountUsageModel(
            key = AccountUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                accountId = UUID.randomUUID().toString(),
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
        val models = mutableListOf<AccountUsageModel>()
        val keys = mutableListOf<AccountUsageKeyWithEpoch>()
        val resourceIdOne = UUID.randomUUID().toString()
        val resourceIdTwo = UUID.randomUUID().toString()
        repeat(100) {
            val model = AccountUsageModel(
                key = AccountUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    accountId = UUID.randomUUID().toString(),
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
            val model = AccountUsageModel(
                key = AccountUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    accountId = UUID.randomUUID().toString(),
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
            keys.add(AccountUsageKeyWithEpoch(key = model.key, epoch = model.epoch))
        }
        repeat(1200) {
            val model = AccountUsageModel(
                key = AccountUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    accountId = UUID.randomUUID().toString(),
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
            keys.add(AccountUsageKeyWithEpoch(key = model.key, epoch = model.epoch))
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
        val result = mutableListOf<AccountUsageKeyWithEpoch>()
        result.addAll(firstResult!!.value.keys)
        result.addAll(secondResult!!.value.keys)
        result.addAll(thirdResult!!.value.keys)
        Assertions.assertEquals(keys.toSet(), result.toSet())
    }

}
