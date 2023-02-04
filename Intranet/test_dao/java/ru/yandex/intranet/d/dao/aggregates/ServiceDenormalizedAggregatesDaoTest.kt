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
import ru.yandex.intranet.d.model.aggregates.ServiceDenormalizedAggregateAmounts
import ru.yandex.intranet.d.model.aggregates.ServiceDenormalizedAggregateKey
import ru.yandex.intranet.d.model.aggregates.ServiceDenormalizedAggregateKeyWithEpoch
import ru.yandex.intranet.d.model.aggregates.ServiceDenormalizedAggregateModel
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Denormalized service aggregates DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class ServiceDenormalizedAggregatesDaoTest(@Autowired private val dao: ServiceDenormalizedAggregatesDao,
                                           @Autowired private val tableClient: YdbTableClient
) {
    @Test
    fun testUpsert(): Unit = runBlocking {
        val model = ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = 99,
                resourceId = UUID.randomUUID().toString(),
                serviceId = 42
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            providerId = UUID.randomUUID().toString(),
            transferable = 100,
            deallocatable = 50,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                    extUsage = false,
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
        val modelOne = ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = 99,
                resourceId = UUID.randomUUID().toString(),
                serviceId = 42
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            providerId = UUID.randomUUID().toString(),
            transferable = 100,
            deallocatable = 50,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                    extUsage = false,
                    unusedEst = null,
                    underutilizedEst = null
                )
            )
        )
        val modelTwo = ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = 98,
                resourceId = UUID.randomUUID().toString(),
                serviceId = 69
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            providerId = UUID.randomUUID().toString(),
            transferable = 99,
            deallocatable = 49,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                    extUsage = false,
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
    fun testDelete(): Unit = runBlocking {
        val model = ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = 99,
                resourceId = UUID.randomUUID().toString(),
                serviceId = 42
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            providerId = UUID.randomUUID().toString(),
            transferable = 100,
            deallocatable = 50,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                    extUsage = false,
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
        val modelOne = ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = 99,
                resourceId = UUID.randomUUID().toString(),
                serviceId = 42
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            providerId = UUID.randomUUID().toString(),
            transferable = 100,
            deallocatable = 50,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                    extUsage = false,
                    unusedEst = null,
                    underutilizedEst = null
                )
            )
        )
        val modelTwo = ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = 98,
                resourceId = UUID.randomUUID().toString(),
                serviceId = 69
            ),
            lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
            epoch = 1,
            providerId = UUID.randomUUID().toString(),
            transferable = 99,
            deallocatable = 49,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                    extUsage = false,
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
    fun getSubtreeDescendingTransferable(): Unit = runBlocking {
        val resourceId = UUID.randomUUID().toString()
        val providerId = UUID.randomUUID().toString()
        var serviceId = 0L
        val models = mutableListOf<ServiceDenormalizedAggregateModel>()
        repeat(1000) {
            val model = ServiceDenormalizedAggregateModel(
                key = ServiceDenormalizedAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    superTreeServiceId = 99,
                    resourceId = resourceId,
                    serviceId = serviceId++
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                providerId = providerId,
                transferable = 100,
                deallocatable = 50,
                exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                        extUsage = false,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
        }
        repeat(1000) {
            val model = ServiceDenormalizedAggregateModel(
                key = ServiceDenormalizedAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    superTreeServiceId = 99,
                    resourceId = resourceId,
                    serviceId = serviceId++
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                providerId = providerId,
                transferable = 95,
                deallocatable = 50,
                exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                        extUsage = false,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
        }
        val firstResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getSubtreeDescendingTransferableFirstPage(txSession, Tenants.DEFAULT_TENANT_ID, 99,
                    resourceId, 1000)
            }
        }
        val secondResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.getSubtreeDescendingTransferableNextPage(txSession, Tenants.DEFAULT_TENANT_ID, 99,
                    resourceId, firstResult!!.last().transferable, firstResult.last().key.serviceId, 1000)
            }
        }
        val result = mutableListOf<ServiceDenormalizedAggregateModel>()
        result.addAll(firstResult!!)
        result.addAll(secondResult!!)
        models.sortWith(Comparator
            .comparing<ServiceDenormalizedAggregateModel, Long> { it.transferable }
            .thenComparing { v -> v.key.serviceId }
            .reversed())
        Assertions.assertEquals(models, result)
    }

    @Test
    fun getSubtreeDescendingDeallocatable(): Unit = runBlocking {
        val resourceId = UUID.randomUUID().toString()
        val providerId = UUID.randomUUID().toString()
        var serviceId = 0L
        val models = mutableListOf<ServiceDenormalizedAggregateModel>()
        repeat(1000) {
            val model = ServiceDenormalizedAggregateModel(
                key = ServiceDenormalizedAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    superTreeServiceId = 99,
                    resourceId = resourceId,
                    serviceId = serviceId++
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                providerId = providerId,
                transferable = 100,
                deallocatable = 50,
                exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                        extUsage = false,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
        }
        repeat(1000) {
            val model = ServiceDenormalizedAggregateModel(
                key = ServiceDenormalizedAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    superTreeServiceId = 99,
                    resourceId = resourceId,
                    serviceId = serviceId++
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                providerId = providerId,
                transferable = 100,
                deallocatable = 45,
                exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                        extUsage = false,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
        }
        val firstResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getSubtreeDescendingDeallocatableFirstPage(txSession, Tenants.DEFAULT_TENANT_ID, 99,
                    resourceId, 1000)
            }
        }
        val secondResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.getSubtreeDescendingDeallocatableNextPage(txSession, Tenants.DEFAULT_TENANT_ID, 99,
                    resourceId, firstResult!!.last().deallocatable, firstResult.last().key.serviceId, 1000)
            }
        }
        val result = mutableListOf<ServiceDenormalizedAggregateModel>()
        result.addAll(firstResult!!)
        result.addAll(secondResult!!)
        models.sortWith(Comparator
            .comparing<ServiceDenormalizedAggregateModel, Long> { it.deallocatable }
            .thenComparing { v -> v.key.serviceId }
            .reversed())
        Assertions.assertEquals(models, result)
    }

    @Test
    fun getSubtreeDescendingTransferableMultiResource(): Unit = runBlocking {
        val resourceIdOne = UUID.randomUUID().toString()
        val resourceIdTwo = UUID.randomUUID().toString()
        val providerId = UUID.randomUUID().toString()
        var serviceId = 0L
        val models = mutableListOf<ServiceDenormalizedAggregateModel>()
        repeat(1000) {
            val model = ServiceDenormalizedAggregateModel(
                key = ServiceDenormalizedAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    superTreeServiceId = 99,
                    resourceId = resourceIdOne,
                    serviceId = serviceId++
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                providerId = providerId,
                transferable = 100,
                deallocatable = 50,
                exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                        extUsage = false,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
        }
        repeat(1000) {
            val model = ServiceDenormalizedAggregateModel(
                key = ServiceDenormalizedAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    superTreeServiceId = 99,
                    resourceId = resourceIdOne,
                    serviceId = serviceId++
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                providerId = providerId,
                transferable = 95,
                deallocatable = 50,
                exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                        extUsage = false,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
        }
        repeat(1000) {
            val model = ServiceDenormalizedAggregateModel(
                key = ServiceDenormalizedAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    superTreeServiceId = 99,
                    resourceId = resourceIdTwo,
                    serviceId = serviceId++
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                providerId = providerId,
                transferable = 90,
                deallocatable = 50,
                exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                        extUsage = false,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
        }
        repeat(1200) {
            val model = ServiceDenormalizedAggregateModel(
                key = ServiceDenormalizedAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    superTreeServiceId = 99,
                    resourceId = resourceIdTwo,
                    serviceId = serviceId++
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                providerId = providerId,
                transferable = 85,
                deallocatable = 50,
                exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                        extUsage = false,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
        }
        val firstResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getSubtreeDescendingTransferableMultiResourceFirstPage(txSession, Tenants.DEFAULT_TENANT_ID,
                    99, listOf(resourceIdOne, resourceIdTwo), 1000)
            }
        }
        val secondResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.getSubtreeDescendingTransferableMultiResourceNextPage(txSession, Tenants.DEFAULT_TENANT_ID,
                    99, listOf(resourceIdOne, resourceIdTwo), firstResult!!.last().key.resourceId,
                    firstResult.last().transferable, firstResult.last().key.serviceId, 1000)
            }
        }
        val thirdResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.getSubtreeDescendingTransferableMultiResourceNextPage(txSession, Tenants.DEFAULT_TENANT_ID,
                    99, listOf(resourceIdOne, resourceIdTwo), secondResult!!.last().key.resourceId,
                    secondResult.last().transferable, secondResult.last().key.serviceId, 1000)
            }
        }
        val fourthResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.getSubtreeDescendingTransferableMultiResourceNextPage(txSession, Tenants.DEFAULT_TENANT_ID,
                    99, listOf(resourceIdOne, resourceIdTwo), thirdResult!!.last().key.resourceId,
                    thirdResult.last().transferable, thirdResult.last().key.serviceId, 1000)
            }
        }
        val fifthResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.getSubtreeDescendingTransferableMultiResourceNextPage(txSession, Tenants.DEFAULT_TENANT_ID,
                    99, listOf(resourceIdOne, resourceIdTwo), fourthResult!!.last().key.resourceId,
                    fourthResult.last().transferable, fourthResult.last().key.serviceId, 1000)
            }
        }
        val result = mutableListOf<ServiceDenormalizedAggregateModel>()
        result.addAll(firstResult!!)
        result.addAll(secondResult!!)
        result.addAll(thirdResult!!)
        result.addAll(fourthResult!!)
        result.addAll(fifthResult!!)
        models.sortWith(Comparator
            .comparing<ServiceDenormalizedAggregateModel, Long> { it.transferable }
            .thenComparing { v -> v.key.resourceId }
            .thenComparing { v -> v.key.serviceId }
            .reversed())
        Assertions.assertEquals(models, result)
    }

    @Test
    fun getSubtreeDescendingDeallocatableMultiResource(): Unit = runBlocking {
        val resourceIdOne = UUID.randomUUID().toString()
        val resourceIdTwo = UUID.randomUUID().toString()
        val providerId = UUID.randomUUID().toString()
        var serviceId = 0L
        val models = mutableListOf<ServiceDenormalizedAggregateModel>()
        repeat(1000) {
            val model = ServiceDenormalizedAggregateModel(
                key = ServiceDenormalizedAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    superTreeServiceId = 99,
                    resourceId = resourceIdOne,
                    serviceId = serviceId++
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                providerId = providerId,
                transferable = 100,
                deallocatable = 50,
                exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                        extUsage = false,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
        }
        repeat(1000) {
            val model = ServiceDenormalizedAggregateModel(
                key = ServiceDenormalizedAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    superTreeServiceId = 99,
                    resourceId = resourceIdOne,
                    serviceId = serviceId++
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                providerId = providerId,
                transferable = 100,
                deallocatable = 45,
                exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                        extUsage = false,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
        }
        repeat(1000) {
            val model = ServiceDenormalizedAggregateModel(
                key = ServiceDenormalizedAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    superTreeServiceId = 99,
                    resourceId = resourceIdTwo,
                    serviceId = serviceId++
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                providerId = providerId,
                transferable = 100,
                deallocatable = 40,
                exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                        extUsage = false,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
        }
        repeat(1200) {
            val model = ServiceDenormalizedAggregateModel(
                key = ServiceDenormalizedAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    superTreeServiceId = 99,
                    resourceId = resourceIdTwo,
                    serviceId = serviceId++
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 1,
                providerId = providerId,
                transferable = 100,
                deallocatable = 35,
                exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                        extUsage = false,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
        }
        val firstResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getSubtreeDescendingDeallocatableMultiResourceFirstPage(txSession, Tenants.DEFAULT_TENANT_ID,
                    99, listOf(resourceIdOne, resourceIdTwo), 1000)
            }
        }
        val secondResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.getSubtreeDescendingDeallocatableMultiResourceNextPage(txSession, Tenants.DEFAULT_TENANT_ID,
                    99, listOf(resourceIdOne, resourceIdTwo), firstResult!!.last().key.resourceId,
                    firstResult.last().deallocatable, firstResult.last().key.serviceId, 1000)
            }
        }
        val thirdResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.getSubtreeDescendingDeallocatableMultiResourceNextPage(txSession, Tenants.DEFAULT_TENANT_ID,
                    99, listOf(resourceIdOne, resourceIdTwo), secondResult!!.last().key.resourceId,
                    secondResult.last().deallocatable, secondResult.last().key.serviceId, 1000)
            }
        }
        val fourthResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.getSubtreeDescendingDeallocatableMultiResourceNextPage(txSession, Tenants.DEFAULT_TENANT_ID,
                    99, listOf(resourceIdOne, resourceIdTwo), thirdResult!!.last().key.resourceId,
                    thirdResult.last().deallocatable, thirdResult.last().key.serviceId, 1000)
            }
        }
        val fifthResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.getSubtreeDescendingDeallocatableMultiResourceNextPage(txSession, Tenants.DEFAULT_TENANT_ID,
                    99, listOf(resourceIdOne, resourceIdTwo), fourthResult!!.last().key.resourceId,
                    fourthResult.last().deallocatable, fourthResult.last().key.serviceId, 1000)
            }
        }
        val result = mutableListOf<ServiceDenormalizedAggregateModel>()
        result.addAll(firstResult!!)
        result.addAll(secondResult!!)
        result.addAll(thirdResult!!)
        result.addAll(fourthResult!!)
        result.addAll(fifthResult!!)
        models.sortWith(Comparator
            .comparing<ServiceDenormalizedAggregateModel, Long> { it.deallocatable }
            .thenComparing { v -> v.key.resourceId }
            .thenComparing { v -> v.key.serviceId }
            .reversed())
        Assertions.assertEquals(models, result)
    }

    @Test
    fun getKeysForOlderEpochs(): Unit = runBlocking {
        val models = mutableListOf<ServiceDenormalizedAggregateModel>()
        val keys = mutableListOf<ServiceDenormalizedAggregateKeyWithEpoch>()
        val providerId = UUID.randomUUID().toString()
        val resourceId = UUID.randomUUID().toString()
        var serviceId = 0L
        repeat(100) {
            val model = ServiceDenormalizedAggregateModel(
                key = ServiceDenormalizedAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    superTreeServiceId = 99,
                    resourceId = resourceId,
                    serviceId = serviceId++
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 3,
                providerId = providerId,
                transferable = 100,
                deallocatable = 50,
                exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                        extUsage = false,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
        }
        repeat(2000) {
            val model = ServiceDenormalizedAggregateModel(
                key = ServiceDenormalizedAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    superTreeServiceId = 99,
                    resourceId = resourceId,
                    serviceId = serviceId++
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                providerId = providerId,
                transferable = 100,
                deallocatable = 50,
                exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                        extUsage = false,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
            keys.add(ServiceDenormalizedAggregateKeyWithEpoch(
                key = model.key,
                providerId = model.providerId,
                epoch = model.epoch
            ))
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
        val result = mutableListOf<ServiceDenormalizedAggregateKeyWithEpoch>()
        result.addAll(firstResult!!.value.keys)
        result.addAll(secondResult!!.value.keys)
        Assertions.assertEquals(keys.toSet(), result.toSet())
    }

    @Test
    fun getKeysForOlderEpochsMultiResource(): Unit = runBlocking {
        val models = mutableListOf<ServiceDenormalizedAggregateModel>()
        val keys = mutableListOf<ServiceDenormalizedAggregateKeyWithEpoch>()
        val providerId = UUID.randomUUID().toString()
        val resourceIdOne = UUID.randomUUID().toString()
        val resourceIdTwo = UUID.randomUUID().toString()
        var serviceId = 0L
        repeat(100) {
            val model = ServiceDenormalizedAggregateModel(
                key = ServiceDenormalizedAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    superTreeServiceId = 99,
                    resourceId = resourceIdOne,
                    serviceId = serviceId++
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 3,
                providerId = providerId,
                transferable = 100,
                deallocatable = 50,
                exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                        extUsage = false,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
        }
        repeat(1000) {
            val model = ServiceDenormalizedAggregateModel(
                key = ServiceDenormalizedAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    superTreeServiceId = 99,
                    resourceId = resourceIdOne,
                    serviceId = serviceId++
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                providerId = providerId,
                transferable = 100,
                deallocatable = 50,
                exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                        extUsage = false,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
            keys.add(ServiceDenormalizedAggregateKeyWithEpoch(
                key = model.key,
                providerId = model.providerId,
                epoch = model.epoch
            ))
        }
        repeat(1200) {
            val model = ServiceDenormalizedAggregateModel(
                key = ServiceDenormalizedAggregateKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    superTreeServiceId = 99,
                    resourceId = resourceIdTwo,
                    serviceId = serviceId++
                ),
                lastUpdate = Instant.now().truncatedTo(ChronoUnit.MICROS),
                epoch = 2,
                providerId = providerId,
                transferable = 100,
                deallocatable = 50,
                exactAmounts = ServiceDenormalizedAggregateAmounts(
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
                        extUsage = false,
                        unusedEst = null,
                        underutilizedEst = null
                    )
                )
            )
            models.add(model)
            keys.add(ServiceDenormalizedAggregateKeyWithEpoch(
                key = model.key,
                providerId = model.providerId,
                epoch = model.epoch
            ))
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
        val result = mutableListOf<ServiceDenormalizedAggregateKeyWithEpoch>()
        result.addAll(firstResult!!.value.keys)
        result.addAll(secondResult!!.value.keys)
        result.addAll(thirdResult!!.value.keys)
        Assertions.assertEquals(keys.toSet(), result.toSet())
    }

}
