package ru.yandex.intranet.d.dao.aggregates

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.aggregates.AggregateBundle
import ru.yandex.intranet.d.model.aggregates.ServiceAggregateAmounts
import ru.yandex.intranet.d.model.aggregates.ServiceAggregateKey
import ru.yandex.intranet.d.model.aggregates.ServiceAggregateKeyWithEpoch
import ru.yandex.intranet.d.model.aggregates.ServiceAggregateModel
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Service aggregates DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class ServiceAggregatesDaoTest(@Autowired private val dao: ServiceAggregatesDao,
                               @Autowired private val tableClient: YdbTableClient
) {
    @Test
    fun testUpsert(): Unit = runBlocking {
        val model = ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = 42,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            exactAmounts = ServiceAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(42),
                    balance = BigInteger.valueOf(10),
                    provided = BigInteger.valueOf(31),
                    allocated = BigInteger.valueOf(22),
                    usage = BigInteger.valueOf(14),
                    unallocated = BigInteger.valueOf(9),
                    unused = BigInteger.valueOf(8),
                    underutilized = BigInteger.valueOf(17),
                    transferable = BigInteger.valueOf(19),
                    deallocatable = BigInteger.valueOf(0),
                    extUsage = null,
                    unusedEst = null,
                    underutilizedEst = null
                ),
                subtree = AggregateBundle(
                    quota = BigInteger.valueOf(420),
                    balance = BigInteger.valueOf(100),
                    provided = BigInteger.valueOf(310),
                    allocated = BigInteger.valueOf(220),
                    usage = BigInteger.valueOf(140),
                    unallocated = BigInteger.valueOf(90),
                    unused = BigInteger.valueOf(80),
                    underutilized = BigInteger.valueOf(170),
                    transferable = BigInteger.valueOf(190),
                    deallocatable = BigInteger.valueOf(0),
                    extUsage = null,
                    unusedEst = null,
                    underutilizedEst = null
                ),
                total = AggregateBundle(
                    quota = BigInteger.valueOf(462),
                    balance = BigInteger.valueOf(110),
                    provided = BigInteger.valueOf(341),
                    allocated = BigInteger.valueOf(242),
                    usage = BigInteger.valueOf(154),
                    unallocated = BigInteger.valueOf(99),
                    unused = BigInteger.valueOf(88),
                    underutilized = BigInteger.valueOf(187),
                    transferable = BigInteger.valueOf(209),
                    deallocatable = BigInteger.valueOf(0),
                    extUsage = null,
                    unusedEst = null,
                    underutilizedEst = null
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
        val modelOne = ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = 42,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            exactAmounts = ServiceAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(42),
                    balance = BigInteger.valueOf(10),
                    provided = BigInteger.valueOf(31),
                    allocated = BigInteger.valueOf(22),
                    usage = BigInteger.valueOf(14),
                    unallocated = BigInteger.valueOf(9),
                    unused = BigInteger.valueOf(8),
                    underutilized = BigInteger.valueOf(17),
                    transferable = BigInteger.valueOf(19),
                    deallocatable = BigInteger.valueOf(0),
                    extUsage = null,
                    unusedEst = null,
                    underutilizedEst = null
                ),
                subtree = AggregateBundle(
                    quota = BigInteger.valueOf(420),
                    balance = BigInteger.valueOf(100),
                    provided = BigInteger.valueOf(310),
                    allocated = BigInteger.valueOf(220),
                    usage = BigInteger.valueOf(140),
                    unallocated = BigInteger.valueOf(90),
                    unused = BigInteger.valueOf(80),
                    underutilized = BigInteger.valueOf(170),
                    transferable = BigInteger.valueOf(190),
                    deallocatable = BigInteger.valueOf(0),
                    extUsage = null,
                    unusedEst = null,
                    underutilizedEst = null
                ),
                total = AggregateBundle(
                    quota = BigInteger.valueOf(462),
                    balance = BigInteger.valueOf(110),
                    provided = BigInteger.valueOf(341),
                    allocated = BigInteger.valueOf(242),
                    usage = BigInteger.valueOf(154),
                    unallocated = BigInteger.valueOf(99),
                    unused = BigInteger.valueOf(88),
                    underutilized = BigInteger.valueOf(187),
                    transferable = BigInteger.valueOf(209),
                    deallocatable = BigInteger.valueOf(0),
                    extUsage = null,
                    unusedEst = null,
                    underutilizedEst = null
                )
            )
        )
        val modelTwo = ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = 69,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            exactAmounts = ServiceAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(42),
                    balance = BigInteger.valueOf(10),
                    provided = BigInteger.valueOf(31),
                    allocated = BigInteger.valueOf(22),
                    usage = BigInteger.valueOf(14),
                    unallocated = BigInteger.valueOf(9),
                    unused = BigInteger.valueOf(8),
                    underutilized = BigInteger.valueOf(17),
                    transferable = BigInteger.valueOf(19),
                    deallocatable = BigInteger.valueOf(0),
                    extUsage = null,
                    unusedEst = null,
                    underutilizedEst = null
                ),
                subtree = AggregateBundle(
                    quota = BigInteger.valueOf(420),
                    balance = BigInteger.valueOf(100),
                    provided = BigInteger.valueOf(310),
                    allocated = BigInteger.valueOf(220),
                    usage = BigInteger.valueOf(140),
                    unallocated = BigInteger.valueOf(90),
                    unused = BigInteger.valueOf(80),
                    underutilized = BigInteger.valueOf(170),
                    transferable = BigInteger.valueOf(190),
                    deallocatable = BigInteger.valueOf(0),
                    extUsage = null,
                    unusedEst = null,
                    underutilizedEst = null
                ),
                total = AggregateBundle(
                    quota = BigInteger.valueOf(462),
                    balance = BigInteger.valueOf(110),
                    provided = BigInteger.valueOf(341),
                    allocated = BigInteger.valueOf(242),
                    usage = BigInteger.valueOf(154),
                    unallocated = BigInteger.valueOf(99),
                    unused = BigInteger.valueOf(88),
                    underutilized = BigInteger.valueOf(187),
                    transferable = BigInteger.valueOf(209),
                    deallocatable = BigInteger.valueOf(0),
                    extUsage = null,
                    unusedEst = null,
                    underutilizedEst = null
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
        val models = mutableListOf<ServiceAggregateModel>()
        repeat(2100) {
            models.add(ServiceAggregateModel(
                key = ServiceAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = 42,
                    providerId = UUID.randomUUID().toString(),
                    resourceId = UUID.randomUUID().toString()
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                exactAmounts = ServiceAggregateAmounts(
                    own = AggregateBundle(
                        quota = BigInteger.valueOf(42),
                        balance = BigInteger.valueOf(10),
                        provided = BigInteger.valueOf(31),
                        allocated = BigInteger.valueOf(22),
                        usage = BigInteger.valueOf(14),
                        unallocated = BigInteger.valueOf(9),
                        unused = BigInteger.valueOf(8),
                        underutilized = BigInteger.valueOf(17),
                        transferable = BigInteger.valueOf(19),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    subtree = AggregateBundle(
                        quota = BigInteger.valueOf(420),
                        balance = BigInteger.valueOf(100),
                        provided = BigInteger.valueOf(310),
                        allocated = BigInteger.valueOf(220),
                        usage = BigInteger.valueOf(140),
                        unallocated = BigInteger.valueOf(90),
                        unused = BigInteger.valueOf(80),
                        underutilized = BigInteger.valueOf(170),
                        transferable = BigInteger.valueOf(190),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    total = AggregateBundle(
                        quota = BigInteger.valueOf(462),
                        balance = BigInteger.valueOf(110),
                        provided = BigInteger.valueOf(341),
                        allocated = BigInteger.valueOf(242),
                        usage = BigInteger.valueOf(154),
                        unallocated = BigInteger.valueOf(99),
                        unused = BigInteger.valueOf(88),
                        underutilized = BigInteger.valueOf(187),
                        transferable = BigInteger.valueOf(209),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            ))
        }
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getByService(txSession, Tenants.DEFAULT_TENANT_ID, 42)
            }
        }
        models.sortWith(Comparator
            .comparing<ServiceAggregateModel, String> { it.key.tenantId.id }
            .thenComparing { v -> v.key.serviceId }
            .thenComparing { v -> v.key.providerId }
            .thenComparing { v -> v.key.resourceId })
        Assertions.assertEquals(models, result)
    }

    @Test
    fun testGetByServices(): Unit = runBlocking {
        val models = mutableListOf<ServiceAggregateModel>()
        repeat(1000) {
            models.add(ServiceAggregateModel(
                key = ServiceAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = 42,
                    providerId = UUID.randomUUID().toString(),
                    resourceId = UUID.randomUUID().toString()
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                exactAmounts = ServiceAggregateAmounts(
                    own = AggregateBundle(
                        quota = BigInteger.valueOf(42),
                        balance = BigInteger.valueOf(10),
                        provided = BigInteger.valueOf(31),
                        allocated = BigInteger.valueOf(22),
                        usage = BigInteger.valueOf(14),
                        unallocated = BigInteger.valueOf(9),
                        unused = BigInteger.valueOf(8),
                        underutilized = BigInteger.valueOf(17),
                        transferable = BigInteger.valueOf(19),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    subtree = AggregateBundle(
                        quota = BigInteger.valueOf(420),
                        balance = BigInteger.valueOf(100),
                        provided = BigInteger.valueOf(310),
                        allocated = BigInteger.valueOf(220),
                        usage = BigInteger.valueOf(140),
                        unallocated = BigInteger.valueOf(90),
                        unused = BigInteger.valueOf(80),
                        underutilized = BigInteger.valueOf(170),
                        transferable = BigInteger.valueOf(190),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    total = AggregateBundle(
                        quota = BigInteger.valueOf(462),
                        balance = BigInteger.valueOf(110),
                        provided = BigInteger.valueOf(341),
                        allocated = BigInteger.valueOf(242),
                        usage = BigInteger.valueOf(154),
                        unallocated = BigInteger.valueOf(99),
                        unused = BigInteger.valueOf(88),
                        underutilized = BigInteger.valueOf(187),
                        transferable = BigInteger.valueOf(209),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            ))
        }
        repeat(900) {
            models.add(ServiceAggregateModel(
                key = ServiceAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = 69,
                    providerId = UUID.randomUUID().toString(),
                    resourceId = UUID.randomUUID().toString()
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                exactAmounts = ServiceAggregateAmounts(
                    own = AggregateBundle(
                        quota = BigInteger.valueOf(42),
                        balance = BigInteger.valueOf(10),
                        provided = BigInteger.valueOf(31),
                        allocated = BigInteger.valueOf(22),
                        usage = BigInteger.valueOf(14),
                        unallocated = BigInteger.valueOf(9),
                        unused = BigInteger.valueOf(8),
                        underutilized = BigInteger.valueOf(17),
                        transferable = BigInteger.valueOf(19),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    subtree = AggregateBundle(
                        quota = BigInteger.valueOf(420),
                        balance = BigInteger.valueOf(100),
                        provided = BigInteger.valueOf(310),
                        allocated = BigInteger.valueOf(220),
                        usage = BigInteger.valueOf(140),
                        unallocated = BigInteger.valueOf(90),
                        unused = BigInteger.valueOf(80),
                        underutilized = BigInteger.valueOf(170),
                        transferable = BigInteger.valueOf(190),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    total = AggregateBundle(
                        quota = BigInteger.valueOf(462),
                        balance = BigInteger.valueOf(110),
                        provided = BigInteger.valueOf(341),
                        allocated = BigInteger.valueOf(242),
                        usage = BigInteger.valueOf(154),
                        unallocated = BigInteger.valueOf(99),
                        unused = BigInteger.valueOf(88),
                        underutilized = BigInteger.valueOf(187),
                        transferable = BigInteger.valueOf(209),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            ))
        }
        repeat(200) {
            models.add(ServiceAggregateModel(
                key = ServiceAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = 38,
                    providerId = UUID.randomUUID().toString(),
                    resourceId = UUID.randomUUID().toString()
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                exactAmounts = ServiceAggregateAmounts(
                    own = AggregateBundle(
                        quota = BigInteger.valueOf(42),
                        balance = BigInteger.valueOf(10),
                        provided = BigInteger.valueOf(31),
                        allocated = BigInteger.valueOf(22),
                        usage = BigInteger.valueOf(14),
                        unallocated = BigInteger.valueOf(9),
                        unused = BigInteger.valueOf(8),
                        underutilized = BigInteger.valueOf(17),
                        transferable = BigInteger.valueOf(19),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    subtree = AggregateBundle(
                        quota = BigInteger.valueOf(420),
                        balance = BigInteger.valueOf(100),
                        provided = BigInteger.valueOf(310),
                        allocated = BigInteger.valueOf(220),
                        usage = BigInteger.valueOf(140),
                        unallocated = BigInteger.valueOf(90),
                        unused = BigInteger.valueOf(80),
                        underutilized = BigInteger.valueOf(170),
                        transferable = BigInteger.valueOf(190),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    total = AggregateBundle(
                        quota = BigInteger.valueOf(462),
                        balance = BigInteger.valueOf(110),
                        provided = BigInteger.valueOf(341),
                        allocated = BigInteger.valueOf(242),
                        usage = BigInteger.valueOf(154),
                        unallocated = BigInteger.valueOf(99),
                        unused = BigInteger.valueOf(88),
                        underutilized = BigInteger.valueOf(187),
                        transferable = BigInteger.valueOf(209),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            ))
        }
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getByServices(txSession, Tenants.DEFAULT_TENANT_ID, listOf(42, 69, 38))
            }
        }
        models.sortWith(Comparator
            .comparing<ServiceAggregateModel, String> { it.key.tenantId.id }
            .thenComparing { v -> v.key.serviceId }
            .thenComparing { v -> v.key.providerId }
            .thenComparing { v -> v.key.resourceId })
        Assertions.assertEquals(models, result)
    }

    @Test
    fun getKeysForOlderEpochs(): Unit = runBlocking {
        val models = mutableListOf<ServiceAggregateModel>()
        val keys = mutableListOf<ServiceAggregateKeyWithEpoch>()
        val providerId = UUID.randomUUID().toString()
        val resourceId = UUID.randomUUID().toString()
        var serviceId = 0L
        repeat(100) {
            val model = ServiceAggregateModel(
                key = ServiceAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = serviceId++,
                    providerId = providerId,
                    resourceId = resourceId
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 3,
                exactAmounts = ServiceAggregateAmounts(
                    own = AggregateBundle(
                        quota = BigInteger.valueOf(42),
                        balance = BigInteger.valueOf(10),
                        provided = BigInteger.valueOf(31),
                        allocated = BigInteger.valueOf(22),
                        usage = BigInteger.valueOf(14),
                        unallocated = BigInteger.valueOf(9),
                        unused = BigInteger.valueOf(8),
                        underutilized = BigInteger.valueOf(17),
                        transferable = BigInteger.valueOf(19),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    subtree = AggregateBundle(
                        quota = BigInteger.valueOf(420),
                        balance = BigInteger.valueOf(100),
                        provided = BigInteger.valueOf(310),
                        allocated = BigInteger.valueOf(220),
                        usage = BigInteger.valueOf(140),
                        unallocated = BigInteger.valueOf(90),
                        unused = BigInteger.valueOf(80),
                        underutilized = BigInteger.valueOf(170),
                        transferable = BigInteger.valueOf(190),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    total = AggregateBundle(
                        quota = BigInteger.valueOf(462),
                        balance = BigInteger.valueOf(110),
                        provided = BigInteger.valueOf(341),
                        allocated = BigInteger.valueOf(242),
                        usage = BigInteger.valueOf(154),
                        unallocated = BigInteger.valueOf(99),
                        unused = BigInteger.valueOf(88),
                        underutilized = BigInteger.valueOf(187),
                        transferable = BigInteger.valueOf(209),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
        }
        repeat(2000) {
            val model = ServiceAggregateModel(
                key = ServiceAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = serviceId++,
                    providerId = providerId,
                    resourceId = resourceId
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                exactAmounts = ServiceAggregateAmounts(
                    own = AggregateBundle(
                        quota = BigInteger.valueOf(42),
                        balance = BigInteger.valueOf(10),
                        provided = BigInteger.valueOf(31),
                        allocated = BigInteger.valueOf(22),
                        usage = BigInteger.valueOf(14),
                        unallocated = BigInteger.valueOf(9),
                        unused = BigInteger.valueOf(8),
                        underutilized = BigInteger.valueOf(17),
                        transferable = BigInteger.valueOf(19),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    subtree = AggregateBundle(
                        quota = BigInteger.valueOf(420),
                        balance = BigInteger.valueOf(100),
                        provided = BigInteger.valueOf(310),
                        allocated = BigInteger.valueOf(220),
                        usage = BigInteger.valueOf(140),
                        unallocated = BigInteger.valueOf(90),
                        unused = BigInteger.valueOf(80),
                        underutilized = BigInteger.valueOf(170),
                        transferable = BigInteger.valueOf(190),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    total = AggregateBundle(
                        quota = BigInteger.valueOf(462),
                        balance = BigInteger.valueOf(110),
                        provided = BigInteger.valueOf(341),
                        allocated = BigInteger.valueOf(242),
                        usage = BigInteger.valueOf(154),
                        unallocated = BigInteger.valueOf(99),
                        unused = BigInteger.valueOf(88),
                        underutilized = BigInteger.valueOf(187),
                        transferable = BigInteger.valueOf(209),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
            keys.add(ServiceAggregateKeyWithEpoch(key = model.key, epoch = model.epoch))
        }
        val firstResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getKeysForOlderEpochsFirstPage(txSession, Tenants.DEFAULT_TENANT_ID, providerId, resourceId, 3)
            }
        }
        val secondResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.getKeysForOlderEpochsNextPage(txSession, firstResult!!.value.nextFrom!!)
            }
        }
        val result = mutableListOf<ServiceAggregateKeyWithEpoch>()
        result.addAll(firstResult!!.value.keys)
        result.addAll(secondResult!!.value.keys)
        Assertions.assertEquals(keys.toSet(), result.toSet())
    }

    @Test
    fun testDelete(): Unit = runBlocking {
        val model = ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = 42,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            exactAmounts = ServiceAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(42),
                    balance = BigInteger.valueOf(10),
                    provided = BigInteger.valueOf(31),
                    allocated = BigInteger.valueOf(22),
                    usage = BigInteger.valueOf(14),
                    unallocated = BigInteger.valueOf(9),
                    unused = BigInteger.valueOf(8),
                    underutilized = BigInteger.valueOf(17),
                    transferable = BigInteger.valueOf(19),
                    deallocatable = BigInteger.valueOf(0),
                    extUsage = null,
                    unusedEst = null,
                    underutilizedEst = null
                ),
                subtree = AggregateBundle(
                    quota = BigInteger.valueOf(420),
                    balance = BigInteger.valueOf(100),
                    provided = BigInteger.valueOf(310),
                    allocated = BigInteger.valueOf(220),
                    usage = BigInteger.valueOf(140),
                    unallocated = BigInteger.valueOf(90),
                    unused = BigInteger.valueOf(80),
                    underutilized = BigInteger.valueOf(170),
                    transferable = BigInteger.valueOf(190),
                    deallocatable = BigInteger.valueOf(0),
                    extUsage = null,
                    unusedEst = null,
                    underutilizedEst = null
                ),
                total = AggregateBundle(
                    quota = BigInteger.valueOf(462),
                    balance = BigInteger.valueOf(110),
                    provided = BigInteger.valueOf(341),
                    allocated = BigInteger.valueOf(242),
                    usage = BigInteger.valueOf(154),
                    unallocated = BigInteger.valueOf(99),
                    unused = BigInteger.valueOf(88),
                    underutilized = BigInteger.valueOf(187),
                    transferable = BigInteger.valueOf(209),
                    deallocatable = BigInteger.valueOf(0),
                    extUsage = null,
                    unusedEst = null,
                    underutilizedEst = null
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
        val modelOne = ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = 42,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            exactAmounts = ServiceAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(42),
                    balance = BigInteger.valueOf(10),
                    provided = BigInteger.valueOf(31),
                    allocated = BigInteger.valueOf(22),
                    usage = BigInteger.valueOf(14),
                    unallocated = BigInteger.valueOf(9),
                    unused = BigInteger.valueOf(8),
                    underutilized = BigInteger.valueOf(17),
                    transferable = BigInteger.valueOf(19),
                    deallocatable = BigInteger.valueOf(0),
                    extUsage = null,
                    unusedEst = null,
                    underutilizedEst = null
                ),
                subtree = AggregateBundle(
                    quota = BigInteger.valueOf(420),
                    balance = BigInteger.valueOf(100),
                    provided = BigInteger.valueOf(310),
                    allocated = BigInteger.valueOf(220),
                    usage = BigInteger.valueOf(140),
                    unallocated = BigInteger.valueOf(90),
                    unused = BigInteger.valueOf(80),
                    underutilized = BigInteger.valueOf(170),
                    transferable = BigInteger.valueOf(190),
                    deallocatable = BigInteger.valueOf(0),
                    extUsage = null,
                    unusedEst = null,
                    underutilizedEst = null
                ),
                total = AggregateBundle(
                    quota = BigInteger.valueOf(462),
                    balance = BigInteger.valueOf(110),
                    provided = BigInteger.valueOf(341),
                    allocated = BigInteger.valueOf(242),
                    usage = BigInteger.valueOf(154),
                    unallocated = BigInteger.valueOf(99),
                    unused = BigInteger.valueOf(88),
                    underutilized = BigInteger.valueOf(187),
                    transferable = BigInteger.valueOf(209),
                    deallocatable = BigInteger.valueOf(0),
                    extUsage = null,
                    unusedEst = null,
                    underutilizedEst = null
                )
            )
        )
        val modelTwo = ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = 69,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            exactAmounts = ServiceAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(42),
                    balance = BigInteger.valueOf(10),
                    provided = BigInteger.valueOf(31),
                    allocated = BigInteger.valueOf(22),
                    usage = BigInteger.valueOf(14),
                    unallocated = BigInteger.valueOf(9),
                    unused = BigInteger.valueOf(8),
                    underutilized = BigInteger.valueOf(17),
                    transferable = BigInteger.valueOf(19),
                    deallocatable = BigInteger.valueOf(0),
                    extUsage = null,
                    unusedEst = null,
                    underutilizedEst = null
                ),
                subtree = AggregateBundle(
                    quota = BigInteger.valueOf(420),
                    balance = BigInteger.valueOf(100),
                    provided = BigInteger.valueOf(310),
                    allocated = BigInteger.valueOf(220),
                    usage = BigInteger.valueOf(140),
                    unallocated = BigInteger.valueOf(90),
                    unused = BigInteger.valueOf(80),
                    underutilized = BigInteger.valueOf(170),
                    transferable = BigInteger.valueOf(190),
                    deallocatable = BigInteger.valueOf(0),
                    extUsage = null,
                    unusedEst = null,
                    underutilizedEst = null
                ),
                total = AggregateBundle(
                    quota = BigInteger.valueOf(462),
                    balance = BigInteger.valueOf(110),
                    provided = BigInteger.valueOf(341),
                    allocated = BigInteger.valueOf(242),
                    usage = BigInteger.valueOf(154),
                    unallocated = BigInteger.valueOf(99),
                    unused = BigInteger.valueOf(88),
                    underutilized = BigInteger.valueOf(187),
                    transferable = BigInteger.valueOf(209),
                    deallocatable = BigInteger.valueOf(0),
                    extUsage = null,
                    unusedEst = null,
                    underutilizedEst = null
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
        val models = mutableListOf<ServiceAggregateModel>()
        val keys = mutableListOf<ServiceAggregateKeyWithEpoch>()
        val providerId = UUID.randomUUID().toString()
        val resourceIdOne = UUID.randomUUID().toString()
        val resourceIdTwo = UUID.randomUUID().toString()
        var serviceId = 0L
        repeat(100) {
            val model = ServiceAggregateModel(
                key = ServiceAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = serviceId++,
                    providerId = providerId,
                    resourceId = resourceIdOne
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 3,
                exactAmounts = ServiceAggregateAmounts(
                    own = AggregateBundle(
                        quota = BigInteger.valueOf(42),
                        balance = BigInteger.valueOf(10),
                        provided = BigInteger.valueOf(31),
                        allocated = BigInteger.valueOf(22),
                        usage = BigInteger.valueOf(14),
                        unallocated = BigInteger.valueOf(9),
                        unused = BigInteger.valueOf(8),
                        underutilized = BigInteger.valueOf(17),
                        transferable = BigInteger.valueOf(19),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    subtree = AggregateBundle(
                        quota = BigInteger.valueOf(420),
                        balance = BigInteger.valueOf(100),
                        provided = BigInteger.valueOf(310),
                        allocated = BigInteger.valueOf(220),
                        usage = BigInteger.valueOf(140),
                        unallocated = BigInteger.valueOf(90),
                        unused = BigInteger.valueOf(80),
                        underutilized = BigInteger.valueOf(170),
                        transferable = BigInteger.valueOf(190),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    total = AggregateBundle(
                        quota = BigInteger.valueOf(462),
                        balance = BigInteger.valueOf(110),
                        provided = BigInteger.valueOf(341),
                        allocated = BigInteger.valueOf(242),
                        usage = BigInteger.valueOf(154),
                        unallocated = BigInteger.valueOf(99),
                        unused = BigInteger.valueOf(88),
                        underutilized = BigInteger.valueOf(187),
                        transferable = BigInteger.valueOf(209),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
        }
        repeat(1000) {
            val model = ServiceAggregateModel(
                key = ServiceAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = serviceId++,
                    providerId = providerId,
                    resourceId = resourceIdOne
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                exactAmounts = ServiceAggregateAmounts(
                    own = AggregateBundle(
                        quota = BigInteger.valueOf(42),
                        balance = BigInteger.valueOf(10),
                        provided = BigInteger.valueOf(31),
                        allocated = BigInteger.valueOf(22),
                        usage = BigInteger.valueOf(14),
                        unallocated = BigInteger.valueOf(9),
                        unused = BigInteger.valueOf(8),
                        underutilized = BigInteger.valueOf(17),
                        transferable = BigInteger.valueOf(19),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    subtree = AggregateBundle(
                        quota = BigInteger.valueOf(420),
                        balance = BigInteger.valueOf(100),
                        provided = BigInteger.valueOf(310),
                        allocated = BigInteger.valueOf(220),
                        usage = BigInteger.valueOf(140),
                        unallocated = BigInteger.valueOf(90),
                        unused = BigInteger.valueOf(80),
                        underutilized = BigInteger.valueOf(170),
                        transferable = BigInteger.valueOf(190),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    total = AggregateBundle(
                        quota = BigInteger.valueOf(462),
                        balance = BigInteger.valueOf(110),
                        provided = BigInteger.valueOf(341),
                        allocated = BigInteger.valueOf(242),
                        usage = BigInteger.valueOf(154),
                        unallocated = BigInteger.valueOf(99),
                        unused = BigInteger.valueOf(88),
                        underutilized = BigInteger.valueOf(187),
                        transferable = BigInteger.valueOf(209),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
            keys.add(ServiceAggregateKeyWithEpoch(key = model.key, epoch = model.epoch))
        }
        repeat(1200) {
            val model = ServiceAggregateModel(
                key = ServiceAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = serviceId++,
                    providerId = providerId,
                    resourceId = resourceIdTwo
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                exactAmounts = ServiceAggregateAmounts(
                    own = AggregateBundle(
                        quota = BigInteger.valueOf(42),
                        balance = BigInteger.valueOf(10),
                        provided = BigInteger.valueOf(31),
                        allocated = BigInteger.valueOf(22),
                        usage = BigInteger.valueOf(14),
                        unallocated = BigInteger.valueOf(9),
                        unused = BigInteger.valueOf(8),
                        underutilized = BigInteger.valueOf(17),
                        transferable = BigInteger.valueOf(19),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    subtree = AggregateBundle(
                        quota = BigInteger.valueOf(420),
                        balance = BigInteger.valueOf(100),
                        provided = BigInteger.valueOf(310),
                        allocated = BigInteger.valueOf(220),
                        usage = BigInteger.valueOf(140),
                        unallocated = BigInteger.valueOf(90),
                        unused = BigInteger.valueOf(80),
                        underutilized = BigInteger.valueOf(170),
                        transferable = BigInteger.valueOf(190),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    total = AggregateBundle(
                        quota = BigInteger.valueOf(462),
                        balance = BigInteger.valueOf(110),
                        provided = BigInteger.valueOf(341),
                        allocated = BigInteger.valueOf(242),
                        usage = BigInteger.valueOf(154),
                        unallocated = BigInteger.valueOf(99),
                        unused = BigInteger.valueOf(88),
                        underutilized = BigInteger.valueOf(187),
                        transferable = BigInteger.valueOf(209),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
            keys.add(ServiceAggregateKeyWithEpoch(key = model.key, epoch = model.epoch))
        }
        val firstResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getKeysForOlderEpochsMultiResourceFirstPage(txSession, Tenants.DEFAULT_TENANT_ID, providerId,
                    listOf(resourceIdOne, resourceIdTwo), 3)
            }
        }
        val secondResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.getKeysForOlderEpochsMultiResourceNextPage(txSession, firstResult!!.value.nextFrom!!,
                    listOf(resourceIdOne, resourceIdTwo), 3)
            }
        }
        val thirdResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.getKeysForOlderEpochsMultiResourceNextPage(txSession, secondResult!!.value.nextFrom!!,
                    listOf(resourceIdOne, resourceIdTwo), 3)
            }
        }
        val result = mutableListOf<ServiceAggregateKeyWithEpoch>()
        result.addAll(firstResult!!.value.keys)
        result.addAll(secondResult!!.value.keys)
        result.addAll(thirdResult!!.value.keys)
        Assertions.assertEquals(keys.toSet(), result.toSet())
    }

    @Test
    fun testGetByServiceAndProvider(): Unit = runBlocking {
        val models = mutableListOf<ServiceAggregateModel>()
        val providerId = UUID.randomUUID().toString()
        repeat(2100) {
            models.add(ServiceAggregateModel(
                key = ServiceAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = 42,
                    providerId = providerId,
                    resourceId = UUID.randomUUID().toString()
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                exactAmounts = ServiceAggregateAmounts(
                    own = AggregateBundle(
                        quota = BigInteger.valueOf(42),
                        balance = BigInteger.valueOf(10),
                        provided = BigInteger.valueOf(31),
                        allocated = BigInteger.valueOf(22),
                        usage = BigInteger.valueOf(14),
                        unallocated = BigInteger.valueOf(9),
                        unused = BigInteger.valueOf(8),
                        underutilized = BigInteger.valueOf(17),
                        transferable = BigInteger.valueOf(19),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    subtree = AggregateBundle(
                        quota = BigInteger.valueOf(420),
                        balance = BigInteger.valueOf(100),
                        provided = BigInteger.valueOf(310),
                        allocated = BigInteger.valueOf(220),
                        usage = BigInteger.valueOf(140),
                        unallocated = BigInteger.valueOf(90),
                        unused = BigInteger.valueOf(80),
                        underutilized = BigInteger.valueOf(170),
                        transferable = BigInteger.valueOf(190),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    ),
                    total = AggregateBundle(
                        quota = BigInteger.valueOf(462),
                        balance = BigInteger.valueOf(110),
                        provided = BigInteger.valueOf(341),
                        allocated = BigInteger.valueOf(242),
                        usage = BigInteger.valueOf(154),
                        unallocated = BigInteger.valueOf(99),
                        unused = BigInteger.valueOf(88),
                        underutilized = BigInteger.valueOf(187),
                        transferable = BigInteger.valueOf(209),
                        deallocatable = BigInteger.valueOf(0),
                        extUsage = null,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            ))
        }
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getByServiceAndProvider(txSession, Tenants.DEFAULT_TENANT_ID, 42, providerId)
            }
        }
        models.sortWith(Comparator
            .comparing<ServiceAggregateModel, String> { it.key.tenantId.id }
            .thenComparing { v -> v.key.serviceId }
            .thenComparing { v -> v.key.providerId }
            .thenComparing { v -> v.key.resourceId })
        Assertions.assertEquals(models, result)
    }

}
