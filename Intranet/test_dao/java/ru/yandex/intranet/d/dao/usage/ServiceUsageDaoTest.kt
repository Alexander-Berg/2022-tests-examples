package ru.yandex.intranet.d.dao.usage

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.usage.HistogramBin
import ru.yandex.intranet.d.model.usage.ServiceUsageAmounts
import ru.yandex.intranet.d.model.usage.ServiceUsageKey
import ru.yandex.intranet.d.model.usage.ServiceUsageKeyWithEpoch
import ru.yandex.intranet.d.model.usage.ServiceUsageModel
import ru.yandex.intranet.d.model.usage.UsageAmount
import ru.yandex.intranet.d.model.usage.UsagePoint
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Service usage DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class ServiceUsageDaoTest(@Autowired private val dao: ServiceUsageDao,
                          @Autowired private val tableClient: YdbTableClient) {

    @Test
    fun testUpsert(): Unit = runBlocking {
        val model = ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = 42,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = UsageAmount(
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
                ),
                subtree = UsageAmount(
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
                ),
                total = UsageAmount(
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
        val modelOne = ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = 42,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = UsageAmount(
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
                ),
                subtree = UsageAmount(
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
                ),
                total = UsageAmount(
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
        )
        val modelTwo = ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = 69,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = UsageAmount(
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
                ),
                subtree = UsageAmount(
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
                ),
                total = UsageAmount(
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
    fun testGetByService(): Unit = runBlocking {
        val models = mutableListOf<ServiceUsageModel>()
        repeat(2100) {
            models.add(ServiceUsageModel(
                key = ServiceUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = 42,
                    providerId = UUID.randomUUID().toString(),
                    resourceId = UUID.randomUUID().toString()
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                usage = ServiceUsageAmounts(
                    own = UsageAmount(
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
                    ),
                    subtree = UsageAmount(
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
                    ),
                    total = UsageAmount(
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
            ))
        }
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getByService(txSession, Tenants.DEFAULT_TENANT_ID, 42, 250)
            }
        }
        models.sortWith(Comparator
            .comparing<ServiceUsageModel, String> { it.key.tenantId.id }
            .thenComparing { v -> v.key.serviceId }
            .thenComparing { v -> v.key.providerId }
            .thenComparing { v -> v.key.resourceId })
        Assertions.assertEquals(models, result)
    }

    @Test
    fun testGetByServices(): Unit = runBlocking {
        val models = mutableListOf<ServiceUsageModel>()
        repeat(1000) {
            models.add(ServiceUsageModel(
                key = ServiceUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = 42,
                    providerId = UUID.randomUUID().toString(),
                    resourceId = UUID.randomUUID().toString()
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                usage = ServiceUsageAmounts(
                    own = UsageAmount(
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
                    ),
                    subtree = UsageAmount(
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
                    ),
                    total = UsageAmount(
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
            ))
        }
        repeat(900) {
            models.add(ServiceUsageModel(
                key = ServiceUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = 69,
                    providerId = UUID.randomUUID().toString(),
                    resourceId = UUID.randomUUID().toString()
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                usage = ServiceUsageAmounts(
                    own = UsageAmount(
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
                    ),
                    subtree = UsageAmount(
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
                    ),
                    total = UsageAmount(
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
            ))
        }
        repeat(200) {
            models.add(ServiceUsageModel(
                key = ServiceUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = 38,
                    providerId = UUID.randomUUID().toString(),
                    resourceId = UUID.randomUUID().toString()
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                usage = ServiceUsageAmounts(
                    own = UsageAmount(
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
                    ),
                    subtree = UsageAmount(
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
                    ),
                    total = UsageAmount(
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
            ))
        }
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getByServices(txSession, Tenants.DEFAULT_TENANT_ID, listOf(42, 69, 38), 250)
            }
        }
        models.sortWith(Comparator
            .comparing<ServiceUsageModel, String> { it.key.tenantId.id }
            .thenComparing { v -> v.key.serviceId }
            .thenComparing { v -> v.key.providerId }
            .thenComparing { v -> v.key.resourceId })
        Assertions.assertEquals(models, result)
    }

    @Test
    fun getKeysForOlderEpochs(): Unit = runBlocking {
        val models = mutableListOf<ServiceUsageModel>()
        val keys = mutableListOf<ServiceUsageKeyWithEpoch>()
        val providerId = UUID.randomUUID().toString()
        val resourceId = UUID.randomUUID().toString()
        var serviceId = 0L
        repeat(100) {
            val model = ServiceUsageModel(
                key = ServiceUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = serviceId++,
                    providerId = providerId,
                    resourceId = resourceId
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 3,
                usage = ServiceUsageAmounts(
                    own = UsageAmount(
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
                    ),
                    subtree = UsageAmount(
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
                    ),
                    total = UsageAmount(
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
            )
            models.add(model)
        }
        repeat(2000) {
            val model = ServiceUsageModel(
                key = ServiceUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = serviceId++,
                    providerId = providerId,
                    resourceId = resourceId
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                usage = ServiceUsageAmounts(
                    own = UsageAmount(
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
                    ),
                    subtree = UsageAmount(
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
                    ),
                    total = UsageAmount(
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
            )
            models.add(model)
            keys.add(ServiceUsageKeyWithEpoch(key = model.key, epoch = model.epoch))
        }
        val firstResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getKeysForOlderEpochsFirstPage(txSession, Tenants.DEFAULT_TENANT_ID, providerId, resourceId, 3, 1000)
            }
        }
        val secondResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.getKeysForOlderEpochsNextPage(txSession, firstResult!!.value.nextFrom!!, 1000)
            }
        }
        val result = mutableListOf<ServiceUsageKeyWithEpoch>()
        result.addAll(firstResult!!.value.keys)
        result.addAll(secondResult!!.value.keys)
        Assertions.assertEquals(keys.toSet(), result.toSet())
    }

    @Test
    fun testDelete(): Unit = runBlocking {
        val model = ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = 42,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = UsageAmount(
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
                ),
                subtree = UsageAmount(
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
                ),
                total = UsageAmount(
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
        val modelOne = ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = 42,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = UsageAmount(
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
                ),
                subtree = UsageAmount(
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
                ),
                total = UsageAmount(
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
        )
        val modelTwo = ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = 69,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = UsageAmount(
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
                ),
                subtree = UsageAmount(
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
                ),
                total = UsageAmount(
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
        val models = mutableListOf<ServiceUsageModel>()
        val keys = mutableListOf<ServiceUsageKeyWithEpoch>()
        val providerId = UUID.randomUUID().toString()
        val resourceIdOne = UUID.randomUUID().toString()
        val resourceIdTwo = UUID.randomUUID().toString()
        var serviceId = 0L
        repeat(100) {
            val model = ServiceUsageModel(
                key = ServiceUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = serviceId++,
                    providerId = providerId,
                    resourceId = resourceIdOne
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 3,
                usage = ServiceUsageAmounts(
                    own = UsageAmount(
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
                    ),
                    subtree = UsageAmount(
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
                    ),
                    total = UsageAmount(
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
            )
            models.add(model)
        }
        repeat(1000) {
            val model = ServiceUsageModel(
                key = ServiceUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = serviceId++,
                    providerId = providerId,
                    resourceId = resourceIdOne
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                usage = ServiceUsageAmounts(
                    own = UsageAmount(
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
                    ),
                    subtree = UsageAmount(
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
                    ),
                    total = UsageAmount(
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
            )
            models.add(model)
            keys.add(ServiceUsageKeyWithEpoch(key = model.key, epoch = model.epoch))
        }
        repeat(1200) {
            val model = ServiceUsageModel(
                key = ServiceUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = serviceId++,
                    providerId = providerId,
                    resourceId = resourceIdTwo
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                usage = ServiceUsageAmounts(
                    own = UsageAmount(
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
                    ),
                    subtree = UsageAmount(
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
                    ),
                    total = UsageAmount(
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
            )
            models.add(model)
            keys.add(ServiceUsageKeyWithEpoch(key = model.key, epoch = model.epoch))
        }
        val firstResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getKeysForOlderEpochsMultiResourceFirstPage(txSession, Tenants.DEFAULT_TENANT_ID, providerId,
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
        val result = mutableListOf<ServiceUsageKeyWithEpoch>()
        result.addAll(firstResult!!.value.keys)
        result.addAll(secondResult!!.value.keys)
        result.addAll(thirdResult!!.value.keys)
        Assertions.assertEquals(keys.toSet(), result.toSet())
    }

    @Test
    fun testGetByServiceAndProvider(): Unit = runBlocking {
        val models = mutableListOf<ServiceUsageModel>()
        val providerId = UUID.randomUUID().toString()
        repeat(2100) {
            models.add(ServiceUsageModel(
                key = ServiceUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = 42,
                    providerId = providerId,
                    resourceId = UUID.randomUUID().toString()
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                usage = ServiceUsageAmounts(
                    own = UsageAmount(
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
                    ),
                    subtree = UsageAmount(
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
                    ),
                    total = UsageAmount(
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
            ))
        }
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getByServiceAndProvider(txSession, Tenants.DEFAULT_TENANT_ID, 42, providerId, 250)
            }
        }
        models.sortWith(Comparator
            .comparing<ServiceUsageModel, String> { it.key.tenantId.id }
            .thenComparing { v -> v.key.serviceId }
            .thenComparing { v -> v.key.providerId }
            .thenComparing { v -> v.key.resourceId })
        Assertions.assertEquals(models, result)
    }

    @Test
    fun getAllByProviderResources(): Unit = runBlocking {
        val models = mutableListOf<ServiceUsageModel>()
        val providerId = UUID.randomUUID().toString()
        val resourceIdOne = UUID.randomUUID().toString()
        val resourceIdTwo = UUID.randomUUID().toString()
        var serviceId = 0L
        repeat(1000) {
            val model = ServiceUsageModel(
                key = ServiceUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = serviceId++,
                    providerId = providerId,
                    resourceId = resourceIdOne
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                usage = ServiceUsageAmounts(
                    own = UsageAmount(
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
                    ),
                    subtree = UsageAmount(
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
                    ),
                    total = UsageAmount(
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
            )
            models.add(model)
        }
        repeat(1200) {
            val model = ServiceUsageModel(
                key = ServiceUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = serviceId++,
                    providerId = providerId,
                    resourceId = resourceIdTwo
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                usage = ServiceUsageAmounts(
                    own = UsageAmount(
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
                    ),
                    subtree = UsageAmount(
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
                    ),
                    total = UsageAmount(
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
            )
            models.add(model)
        }
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getAllByProviderResources(txSession, Tenants.DEFAULT_TENANT_ID, providerId,
                    listOf(resourceIdOne, resourceIdTwo), 250)
            }
        }!!
        Assertions.assertEquals(models.toSet(), result.toSet())
    }

    @Test
    fun getAllByProvider(): Unit = runBlocking {
        val models = mutableListOf<ServiceUsageModel>()
        val providerId = UUID.randomUUID().toString()
        val resourceIdOne = UUID.randomUUID().toString()
        val resourceIdTwo = UUID.randomUUID().toString()
        var serviceId = 0L
        repeat(1000) {
            val model = ServiceUsageModel(
                key = ServiceUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = serviceId++,
                    providerId = providerId,
                    resourceId = resourceIdOne
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                usage = ServiceUsageAmounts(
                    own = UsageAmount(
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
                    ),
                    subtree = UsageAmount(
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
                    ),
                    total = UsageAmount(
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
            )
            models.add(model)
        }
        repeat(1200) {
            val model = ServiceUsageModel(
                key = ServiceUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = serviceId++,
                    providerId = providerId,
                    resourceId = resourceIdTwo
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                usage = ServiceUsageAmounts(
                    own = UsageAmount(
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
                    ),
                    subtree = UsageAmount(
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
                    ),
                    total = UsageAmount(
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
            )
            models.add(model)
        }
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getAllByProvider(txSession, Tenants.DEFAULT_TENANT_ID, providerId, 250)
            }
        }!!
        Assertions.assertEquals(models.toSet(), result.toSet())
    }

    @Test
    fun scanByProviderResources(): Unit = runBlocking {
        val models = mutableListOf<ServiceUsageModel>()
        val providerId = UUID.randomUUID().toString()
        val resourceIdOne = UUID.randomUUID().toString()
        val resourceIdTwo = UUID.randomUUID().toString()
        var serviceId = 0L
        repeat(1000) {
            val model = ServiceUsageModel(
                key = ServiceUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = serviceId++,
                    providerId = providerId,
                    resourceId = resourceIdOne
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                usage = ServiceUsageAmounts(
                    own = UsageAmount(
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
                    ),
                    subtree = UsageAmount(
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
                    ),
                    total = UsageAmount(
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
            )
            models.add(model)
        }
        repeat(1200) {
            val model = ServiceUsageModel(
                key = ServiceUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = serviceId++,
                    providerId = providerId,
                    resourceId = resourceIdTwo
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                usage = ServiceUsageAmounts(
                    own = UsageAmount(
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
                    ),
                    subtree = UsageAmount(
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
                    ),
                    total = UsageAmount(
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
            )
            models.add(model)
        }
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            dao.scanByProviderResources(session, Tenants.DEFAULT_TENANT_ID, providerId,
                listOf(resourceIdOne, resourceIdTwo), Duration.ofSeconds(15))
        }!!
        Assertions.assertEquals(models.toSet(), result.toSet())
    }

    @Test
    fun scanByProvider(): Unit = runBlocking {
        val models = mutableListOf<ServiceUsageModel>()
        val providerId = UUID.randomUUID().toString()
        val resourceIdOne = UUID.randomUUID().toString()
        val resourceIdTwo = UUID.randomUUID().toString()
        var serviceId = 0L
        repeat(1000) {
            val model = ServiceUsageModel(
                key = ServiceUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = serviceId++,
                    providerId = providerId,
                    resourceId = resourceIdOne
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                usage = ServiceUsageAmounts(
                    own = UsageAmount(
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
                    ),
                    subtree = UsageAmount(
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
                    ),
                    total = UsageAmount(
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
            )
            models.add(model)
        }
        repeat(1200) {
            val model = ServiceUsageModel(
                key = ServiceUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = serviceId++,
                    providerId = providerId,
                    resourceId = resourceIdTwo
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                usage = ServiceUsageAmounts(
                    own = UsageAmount(
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
                    ),
                    subtree = UsageAmount(
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
                    ),
                    total = UsageAmount(
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
            )
            models.add(model)
        }
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            dao.scanByProvider(session, Tenants.DEFAULT_TENANT_ID, providerId, Duration.ofSeconds(15))
        }!!
        Assertions.assertEquals(models.toSet(), result.toSet())
    }

}
