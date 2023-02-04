package ru.yandex.intranet.d.services.aggregates

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.model.aggregates.*
import ru.yandex.intranet.d.model.folders.FolderType
import ru.yandex.intranet.d.model.providers.*
import ru.yandex.intranet.d.model.quotas.QuotaAggregationModel
import ru.yandex.intranet.d.model.resources.ResourceModel
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel
import ru.yandex.intranet.d.model.usage.ServiceUsageAmounts
import ru.yandex.intranet.d.model.usage.ServiceUsageKey
import ru.yandex.intranet.d.model.usage.ServiceUsageModel
import ru.yandex.intranet.d.model.usage.UsageAmount
import java.math.BigInteger
import java.time.Instant
import java.util.*

/**
 * Quota aggregator unused estimation test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
class QuotaAggregatorUnusedEstimationTest {

    @Test
    fun testSingleRoot() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = resourceStub(provider.id, resourceType.id)
        val epoch = AggregateEpochModel(
            key = AggregateEpochKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = provider.id,
                resourceId = resource.id
            ),
            epoch = 0,
        )
        val now = Instant.now()
        val root = 0L
        val folderId = UUID.randomUUID().toString()
        val quota = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 10L,
            balance = 5L,
            provided = null,
            allocated = null,
            serviceId = root,
            folderId = folderId,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val provision = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 5L,
            allocated = 2L,
            serviceId = root,
            folderId = folderId,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val usage = UsageIndex(mapOf(root to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
                    unused = BigInteger.valueOf(11)
                ),
                subtree = null,
                total = null
            )
        ))))
        val result = prepareAggregates(
            provider = provider,
            resources = mapOf(resource.id to resource),
            resourceTypes = mapOf(resourceType.id to resourceType),
            epochs = mapOf(resource.id to epoch),
            quotas = QuotasIndex(quotas = mapOf(root to mapOf(resource.id to setOf(quota, provision)))),
            hierarchy = ServiceHierarchy(roots = listOf(root), childrenByParent = emptyMap(), nodesCount = 1),
            timestamp = now,
            usage = usage
        )
        Assertions.assertEquals(listOf(ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(11),
                    underutilizedEst = null
                ),
                subtree = null,
                total = null
            )
        )), result.aggregates)
        Assertions.assertEquals(listOf(ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = root,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            providerId = provider.id,
            transferable = Long.MAX_VALUE,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(11),
                    underutilizedEst = null
                )
            )
        )), result.denormalizedAggregates)
        Assertions.assertEquals(listOf(ServiceAggregateUsageModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    unused = BigInteger.valueOf(11)
                ),
                subtree = null,
                total = null
            )
        )), result.usageAggregates)
    }

    @Test
    fun testTwoRoots() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = resourceStub(provider.id, resourceType.id)
        val epoch = AggregateEpochModel(
            key = AggregateEpochKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = provider.id,
                resourceId = resource.id
            ),
            epoch = 0,
        )
        val now = Instant.now()
        val rootOne = 0L
        val rootTwo = 1L
        val folderIdRootOne = UUID.randomUUID().toString()
        val folderIdRootTwo = UUID.randomUUID().toString()
        val quotaOne = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 10L,
            balance = 5L,
            provided = null,
            allocated = null,
            serviceId = rootOne,
            folderId = folderIdRootOne,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val quotaTwo = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 100L,
            balance = 50L,
            provided = null,
            allocated = null,
            serviceId = rootTwo,
            folderId = folderIdRootTwo,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val provisionOne = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 5L,
            allocated = 2L,
            serviceId = rootOne,
            folderId = folderIdRootOne,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val provisionTwo = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 50L,
            allocated = 20L,
            serviceId = rootTwo,
            folderId = folderIdRootTwo,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val usage = UsageIndex(mapOf(rootOne to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = rootOne,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
                    unused = BigInteger.valueOf(11)
                ),
                subtree = null,
                total = null
            )
        )), rootTwo to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = rootTwo,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
                    unused = BigInteger.valueOf(12)
                ),
                subtree = null,
                total = null
            )
        ))))
        val result = prepareAggregates(
            provider = provider,
            resources = mapOf(resource.id to resource),
            resourceTypes = mapOf(resourceType.id to resourceType),
            epochs = mapOf(resource.id to epoch),
            quotas = QuotasIndex(quotas = mapOf(rootOne to mapOf(resource.id to setOf(quotaOne, provisionOne)),
                rootTwo to mapOf(resource.id to setOf(quotaTwo, provisionTwo)))),
            hierarchy = ServiceHierarchy(roots = listOf(rootOne, rootTwo), childrenByParent = emptyMap(), nodesCount = 2),
            timestamp = now,
            usage = usage
        )
        Assertions.assertEquals(setOf(ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = rootOne,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(11),
                    underutilizedEst = null
                ),
                subtree = null,
                total = null
            )
        ), ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = rootTwo,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            exactAmounts = ServiceAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(100),
                    balance = BigInteger.valueOf(50),
                    provided = BigInteger.valueOf(50),
                    allocated = BigInteger.valueOf(20),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(30),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(80),
                    deallocatable = BigInteger.ZERO,
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(12),
                    underutilizedEst = null
                ),
                subtree = null,
                total = null
            )
        )), result.aggregates.toSet())
        Assertions.assertEquals(setOf(ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = rootOne,
                resourceId = resource.id,
                serviceId = rootOne,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            providerId = provider.id,
            transferable = Long.MAX_VALUE,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(11),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = rootTwo,
                resourceId = resource.id,
                serviceId = rootTwo,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            providerId = provider.id,
            transferable = Long.MAX_VALUE,
            deallocatable = 0L,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.valueOf(100),
                    balance = BigInteger.valueOf(50),
                    provided = BigInteger.valueOf(50),
                    allocated = BigInteger.valueOf(20),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(30),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(80),
                    deallocatable = BigInteger.ZERO,
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(12),
                    underutilizedEst = null
                )
            )
        )), result.denormalizedAggregates.toSet())
        Assertions.assertEquals(setOf(ServiceAggregateUsageModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = rootOne,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    unused = BigInteger.valueOf(11)
                ),
                subtree = null,
                total = null
            )
        ), ServiceAggregateUsageModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = rootTwo,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    unused = BigInteger.valueOf(12)
                ),
                subtree = null,
                total = null
            )
        )), result.usageAggregates.toSet())
    }

    @Test
    fun testRootAndChild() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = resourceStub(provider.id, resourceType.id)
        val epoch = AggregateEpochModel(
            key = AggregateEpochKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = provider.id,
                resourceId = resource.id
            ),
            epoch = 0,
        )
        val now = Instant.now()
        val root = 0L
        val child = 1L
        val folderIdRoot = UUID.randomUUID().toString()
        val folderIdChild = UUID.randomUUID().toString()
        val rootQuota = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 10L,
            balance = 5L,
            provided = null,
            allocated = null,
            serviceId = root,
            folderId = folderIdRoot,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val childQuota = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 30L,
            balance = 15L,
            provided = null,
            allocated = null,
            serviceId = child,
            folderId = folderIdChild,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val rootProvision = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 5L,
            allocated = 2L,
            serviceId = root,
            folderId = folderIdRoot,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val childProvision = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 15L,
            allocated = 6L,
            serviceId = child,
            folderId = folderIdChild,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val usage = UsageIndex(mapOf(root to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
                    unused = BigInteger.valueOf(1L)
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
                    unused = BigInteger.valueOf(1L)
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
                    unused = BigInteger.valueOf(2L)
                )
            )
        )), child to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = child,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
                    unused = BigInteger.valueOf(1L)
                ),
                subtree = null,
                total = null
            )
        ))))
        val result = prepareAggregates(
            provider = provider,
            resources = mapOf(resource.id to resource),
            resourceTypes = mapOf(resourceType.id to resourceType),
            epochs = mapOf(resource.id to epoch),
            quotas = QuotasIndex(quotas = mapOf(root to mapOf(resource.id to setOf(rootQuota, rootProvision)),
                child to mapOf(resource.id to setOf(childQuota, childProvision)))),
            hierarchy = ServiceHierarchy(
                roots = listOf(root),
                childrenByParent = mapOf(root to setOf(child)),
                nodesCount = 2),
            timestamp = now,
            usage = usage
        )
        Assertions.assertEquals(setOf(ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1L),
                    underutilizedEst = null
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1L),
                    underutilizedEst = null
                ),
                total = AggregateBundle(
                    quota = BigInteger.valueOf(40),
                    balance = BigInteger.valueOf(20),
                    provided = BigInteger.valueOf(20),
                    allocated = BigInteger.valueOf(8),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(12),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(32),
                    deallocatable = BigInteger.ZERO,
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(2L),
                    underutilizedEst = null
                )
            )
        ), ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = child,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1L),
                    underutilizedEst = null
                ),
                subtree = null,
                total = null
            )
        )), result.aggregates.toSet())
        Assertions.assertEquals(setOf(ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = root,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(8), BigInteger.valueOf(32)),
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1L),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = child,
                resourceId = resource.id,
                serviceId = child,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1L),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = child,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(24), BigInteger.valueOf(32)),
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1L),
                    underutilizedEst = null
                )
            )
        )), result.denormalizedAggregates.toSet())
        Assertions.assertEquals(setOf(ServiceAggregateUsageModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                serviceId = child,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
        )), result.usageAggregates.toSet())
    }

    @Test
    fun testRootAndTwoChildren() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = resourceStub(provider.id, resourceType.id)
        val epoch = AggregateEpochModel(
            key = AggregateEpochKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = provider.id,
                resourceId = resource.id
            ),
            epoch = 0,
        )
        val now = Instant.now()
        val root = 0L
        val childOne = 1L
        val childTwo = 2L
        val folderIdRoot = UUID.randomUUID().toString()
        val folderIdChildOne = UUID.randomUUID().toString()
        val folderIdChildTwo = UUID.randomUUID().toString()
        val rootQuota = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 10L,
            balance = 5L,
            provided = null,
            allocated = null,
            serviceId = root,
            folderId = folderIdRoot,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val childQuotaOne = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 30L,
            balance = 15L,
            provided = null,
            allocated = null,
            serviceId = childOne,
            folderId = folderIdChildOne,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val childQuotaTwo = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 60L,
            balance = 30L,
            provided = null,
            allocated = null,
            serviceId = childTwo,
            folderId = folderIdChildTwo,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val rootProvision = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 5L,
            allocated = 2L,
            serviceId = root,
            folderId = folderIdRoot,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val childProvisionOne = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 15L,
            allocated = 6L,
            serviceId = childOne,
            folderId = folderIdChildOne,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val childProvisionTwo = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 30L,
            allocated = 12L,
            serviceId = childTwo,
            folderId = folderIdChildTwo,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val usage = UsageIndex(mapOf(root to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
                    unused = BigInteger.valueOf(2)
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
                    unused = BigInteger.valueOf(3)
                )
            )
        )), childOne to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = childOne,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
        )), childTwo to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = childTwo,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
        ))))
        val result = prepareAggregates(
            provider = provider,
            resources = mapOf(resource.id to resource),
            resourceTypes = mapOf(resourceType.id to resourceType),
            epochs = mapOf(resource.id to epoch),
            quotas = QuotasIndex(quotas = mapOf(root to mapOf(resource.id to setOf(rootQuota, rootProvision)),
                childOne to mapOf(resource.id to setOf(childQuotaOne, childProvisionOne)),
                childTwo to mapOf(resource.id to setOf(childQuotaTwo, childProvisionTwo)))),
            hierarchy = ServiceHierarchy(
                roots = listOf(root),
                childrenByParent = mapOf(root to setOf(childOne, childTwo)),
                nodesCount = 3),
            timestamp = now,
            usage = usage
        )
        Assertions.assertEquals(setOf(ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                ),
                subtree = AggregateBundle(
                    quota = BigInteger.valueOf(90),
                    balance = BigInteger.valueOf(45),
                    provided = BigInteger.valueOf(45),
                    allocated = BigInteger.valueOf(18),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(27),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(72),
                    deallocatable = BigInteger.ZERO,
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(2),
                    underutilizedEst = null
                ),
                total = AggregateBundle(
                    quota = BigInteger.valueOf(100),
                    balance = BigInteger.valueOf(50),
                    provided = BigInteger.valueOf(50),
                    allocated = BigInteger.valueOf(20),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(30),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(80),
                    deallocatable = BigInteger.ZERO,
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(3),
                    underutilizedEst = null
                )
            )
        ), ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = childOne,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                ),
                subtree = null,
                total = null
            )
        ), ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = childTwo,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                ),
                subtree = null,
                total = null
            )
        )), result.aggregates.toSet())
        Assertions.assertEquals(setOf(ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = root,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(8), BigInteger.valueOf(80)),
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = childOne,
                resourceId = resource.id,
                serviceId = childOne,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = childOne,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(24), BigInteger.valueOf(80)),
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = childTwo,
                resourceId = resource.id,
                serviceId = childTwo,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = childTwo,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(48), BigInteger.valueOf(80)),
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        )), result.denormalizedAggregates.toSet())
        Assertions.assertEquals(setOf(ServiceAggregateUsageModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    unused = BigInteger.valueOf(2)
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
                    unused = BigInteger.valueOf(3)
                )
            )
        ), ServiceAggregateUsageModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = childOne,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                serviceId = childTwo,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
        )), result.usageAggregates.toSet())
    }

    @Test
    fun testRootAndChildAndGrandchild() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = resourceStub(provider.id, resourceType.id)
        val epoch = AggregateEpochModel(
            key = AggregateEpochKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = provider.id,
                resourceId = resource.id
            ),
            epoch = 0,
        )
        val now = Instant.now()
        val root = 0L
        val child = 1L
        val grandchild = 2L
        val folderIdRoot = UUID.randomUUID().toString()
        val folderIdChild = UUID.randomUUID().toString()
        val folderIdGrandchild = UUID.randomUUID().toString()
        val rootQuota = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 10L,
            balance = 5L,
            provided = null,
            allocated = null,
            serviceId = root,
            folderId = folderIdRoot,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val childQuota = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 30L,
            balance = 15L,
            provided = null,
            allocated = null,
            serviceId = child,
            folderId = folderIdChild,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val grandchildQuota = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 60L,
            balance = 30L,
            provided = null,
            allocated = null,
            serviceId = grandchild,
            folderId = folderIdGrandchild,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val rootProvision = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 5L,
            allocated = 2L,
            serviceId = root,
            folderId = folderIdRoot,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val childProvision = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 15L,
            allocated = 6L,
            serviceId = child,
            folderId = folderIdChild,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val grandchildProvision = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 30L,
            allocated = 12L,
            serviceId = grandchild,
            folderId = folderIdGrandchild,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val usage = UsageIndex(mapOf(root to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
                    unused = BigInteger.valueOf(2)
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
                    unused = BigInteger.valueOf(3)
                )
            )
        )), child to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = child,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
        )), grandchild to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = grandchild,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
        ))))
        val result = prepareAggregates(
            provider = provider,
            resources = mapOf(resource.id to resource),
            resourceTypes = mapOf(resourceType.id to resourceType),
            epochs = mapOf(resource.id to epoch),
            quotas = QuotasIndex(quotas = mapOf(root to mapOf(resource.id to setOf(rootQuota, rootProvision)),
                child to mapOf(resource.id to setOf(childQuota, childProvision)),
                grandchild to mapOf(resource.id to setOf(grandchildQuota, grandchildProvision)))),
            hierarchy = ServiceHierarchy(
                roots = listOf(root),
                childrenByParent = mapOf(root to setOf(child), child to setOf(grandchild)),
                nodesCount = 3),
            timestamp = now,
            usage = usage
        )
        Assertions.assertEquals(setOf(ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                ),
                subtree = AggregateBundle(
                    quota = BigInteger.valueOf(90),
                    balance = BigInteger.valueOf(45),
                    provided = BigInteger.valueOf(45),
                    allocated = BigInteger.valueOf(18),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(27),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(72),
                    deallocatable = BigInteger.ZERO,
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(2),
                    underutilizedEst = null
                ),
                total = AggregateBundle(
                    quota = BigInteger.valueOf(100),
                    balance = BigInteger.valueOf(50),
                    provided = BigInteger.valueOf(50),
                    allocated = BigInteger.valueOf(20),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(30),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(80),
                    deallocatable = BigInteger.ZERO,
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(3),
                    underutilizedEst = null
                )
            )
        ), ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = child,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                ),
                total = AggregateBundle(
                    quota = BigInteger.valueOf(90),
                    balance = BigInteger.valueOf(45),
                    provided = BigInteger.valueOf(45),
                    allocated = BigInteger.valueOf(18),
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.valueOf(27),
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.valueOf(72),
                    deallocatable = BigInteger.ZERO,
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(2),
                    underutilizedEst = null
                )
            )
        ), ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = grandchild,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                ),
                subtree = null,
                total = null
            )
        )), result.aggregates.toSet())
        Assertions.assertEquals(setOf(ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = root,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(8), BigInteger.valueOf(80)),
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = child,
                resourceId = resource.id,
                serviceId = child,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(24), BigInteger.valueOf(72)),
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = child,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(24), BigInteger.valueOf(80)),
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = grandchild,
                resourceId = resource.id,
                serviceId = grandchild,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = child,
                resourceId = resource.id,
                serviceId = grandchild,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(48), BigInteger.valueOf(72)),
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = grandchild,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(48), BigInteger.valueOf(80)),
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        )), result.denormalizedAggregates.toSet())
        Assertions.assertEquals(setOf(ServiceAggregateUsageModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    unused = BigInteger.valueOf(2)
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
                    unused = BigInteger.valueOf(3)
                )
            )
        ), ServiceAggregateUsageModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = child,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                serviceId = grandchild,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
        )), result.usageAggregates.toSet())
    }

    @Test
    fun testRootAndTwoChildrenAndTwoGrandchildren() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = resourceStub(provider.id, resourceType.id)
        val epoch = AggregateEpochModel(
            key = AggregateEpochKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = provider.id,
                resourceId = resource.id
            ),
            epoch = 0,
        )
        val now = Instant.now()
        val root = 0L
        val childOne = 1L
        val childTwo = 2L
        val grandchildOne = 3L
        val grandchildTwo = 4L
        val folderIdRoot = UUID.randomUUID().toString()
        val folderIdChildOne = UUID.randomUUID().toString()
        val folderIdChildTwo = UUID.randomUUID().toString()
        val folderIdGrandchildOne = UUID.randomUUID().toString()
        val folderIdGrandchildTwo = UUID.randomUUID().toString()
        val rootQuota = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 10L,
            balance = 5L,
            provided = null,
            allocated = null,
            serviceId = root,
            folderId = folderIdRoot,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val childQuotaOne = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 30L,
            balance = 15L,
            provided = null,
            allocated = null,
            serviceId = childOne,
            folderId = folderIdChildOne,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val childQuotaTwo = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 60L,
            balance = 30L,
            provided = null,
            allocated = null,
            serviceId = childTwo,
            folderId = folderIdChildTwo,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val grandchildQuotaOne = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 30L,
            balance = 15L,
            provided = null,
            allocated = null,
            serviceId = grandchildOne,
            folderId = folderIdGrandchildOne,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val grandchildQuotaTwo = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 60L,
            balance = 30L,
            provided = null,
            allocated = null,
            serviceId = grandchildTwo,
            folderId = folderIdGrandchildTwo,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val rootProvision = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 5L,
            allocated = 2L,
            serviceId = root,
            folderId = folderIdRoot,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val childProvisionOne = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 15L,
            allocated = 6L,
            serviceId = childOne,
            folderId = folderIdChildOne,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val childProvisionTwo = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 30L,
            allocated = 12L,
            serviceId = childTwo,
            folderId = folderIdChildTwo,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val grandchildProvisionOne = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 15L,
            allocated = 6L,
            serviceId = grandchildOne,
            folderId = folderIdGrandchildOne,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val grandchildProvisionTwo = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 30L,
            allocated = 12L,
            serviceId = grandchildTwo,
            folderId = folderIdGrandchildTwo,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val usage = UsageIndex(mapOf(root to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
        )), childOne to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = childOne,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
        )), childTwo to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = childTwo,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
        )), grandchildOne to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = grandchildOne,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
        )), grandchildTwo to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = grandchildTwo,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
        ))))
        val result = prepareAggregates(
            provider = provider,
            resources = mapOf(resource.id to resource),
            resourceTypes = mapOf(resourceType.id to resourceType),
            epochs = mapOf(resource.id to epoch),
            quotas = QuotasIndex(quotas = mapOf(root to mapOf(resource.id to setOf(rootQuota, rootProvision)),
                childOne to mapOf(resource.id to setOf(childQuotaOne, childProvisionOne)),
                childTwo to mapOf(resource.id to setOf(childQuotaTwo, childProvisionTwo)),
                grandchildOne to mapOf(resource.id to setOf(grandchildQuotaOne, grandchildProvisionOne)),
                grandchildTwo to mapOf(resource.id to setOf(grandchildQuotaTwo, grandchildProvisionTwo)))),
            hierarchy = ServiceHierarchy(
                roots = listOf(root),
                childrenByParent = mapOf(root to setOf(childOne, childTwo),
                    childOne to setOf(grandchildOne), childTwo to setOf(grandchildTwo)
                ),
                nodesCount = 5),
            timestamp = now,
            usage = usage
        )
        Assertions.assertEquals(setOf(ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(4),
                    underutilizedEst = null
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(5),
                    underutilizedEst = null
                )
            )
        ), ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = childOne,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(2),
                    underutilizedEst = null
                )
            )
        ), ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = childTwo,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(2),
                    underutilizedEst = null
                )
            )
        ), ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = grandchildOne,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                ),
                subtree = null,
                total = null
            )
        ), ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = grandchildTwo,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                ),
                subtree = null,
                total = null
            )
        )), result.aggregates.toSet())
        Assertions.assertEquals(setOf(ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = root,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = childOne,
                resourceId = resource.id,
                serviceId = childOne,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = childOne,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = childTwo,
                resourceId = resource.id,
                serviceId = childTwo,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = childTwo,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = grandchildOne,
                resourceId = resource.id,
                serviceId = grandchildOne,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = grandchildTwo,
                resourceId = resource.id,
                serviceId = grandchildTwo,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = childOne,
                resourceId = resource.id,
                serviceId = grandchildOne,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = childTwo,
                resourceId = resource.id,
                serviceId = grandchildTwo,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = grandchildOne,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = grandchildTwo,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        )), result.denormalizedAggregates.toSet())
        Assertions.assertEquals(setOf(ServiceAggregateUsageModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                serviceId = childOne,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                serviceId = childTwo,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                serviceId = grandchildOne,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                serviceId = grandchildTwo,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
        )), result.usageAggregates.toSet())
    }

    @Test
    fun testRootAndChildEmptyRoot() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = resourceStub(provider.id, resourceType.id)
        val epoch = AggregateEpochModel(
            key = AggregateEpochKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = provider.id,
                resourceId = resource.id
            ),
            epoch = 0,
        )
        val now = Instant.now()
        val root = 0L
        val child = 1L
        val folderIdRoot = UUID.randomUUID().toString()
        val folderIdChild = UUID.randomUUID().toString()
        val childQuota = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 30L,
            balance = 15L,
            provided = null,
            allocated = null,
            serviceId = child,
            folderId = folderIdChild,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val childProvision = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 15L,
            allocated = 6L,
            serviceId = child,
            folderId = folderIdChild,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val usage = UsageIndex(mapOf(root to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
                own = null,
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
                    unused = BigInteger.valueOf(1)
                )
            )
        )), child to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = child,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
        ))))
        val result = prepareAggregates(
            provider = provider,
            resources = mapOf(resource.id to resource),
            resourceTypes = mapOf(resourceType.id to resourceType),
            epochs = mapOf(resource.id to epoch),
            quotas = QuotasIndex(quotas = mapOf(child to mapOf(resource.id to setOf(childQuota, childProvision)))),
            hierarchy = ServiceHierarchy(
                roots = listOf(root),
                childrenByParent = mapOf(root to setOf(child)),
                nodesCount = 2),
            timestamp = now,
            usage = usage
        )
        Assertions.assertEquals(setOf(ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            exactAmounts = ServiceAggregateAmounts(
                own = null,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                ),
                total = AggregateBundle(
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = child,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                ),
                subtree = null,
                total = null
            )
        )), result.aggregates.toSet())
        Assertions.assertEquals(setOf(ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = child,
                resourceId = resource.id,
                serviceId = child,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = child,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        )), result.denormalizedAggregates.toSet())
        Assertions.assertEquals(setOf(ServiceAggregateUsageModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            exactAmounts = ServiceUsageAmounts(
                own = null,
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
                    unused = BigInteger.valueOf(1)
                )
            )
        ), ServiceAggregateUsageModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = child,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
        )), result.usageAggregates.toSet())
    }

    @Test
    fun testRootAndChildEmptyChild() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = resourceStub(provider.id, resourceType.id)
        val epoch = AggregateEpochModel(
            key = AggregateEpochKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = provider.id,
                resourceId = resource.id
            ),
            epoch = 0,
        )
        val now = Instant.now()
        val root = 0L
        val child = 1L
        val folderIdRoot = UUID.randomUUID().toString()
        val folderIdChild = UUID.randomUUID().toString()
        val rootQuota = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 10L,
            balance = 5L,
            provided = null,
            allocated = null,
            serviceId = root,
            folderId = folderIdRoot,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val rootProvision = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 5L,
            allocated = 2L,
            serviceId = root,
            folderId = folderIdRoot,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val usage = UsageIndex(mapOf(root to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
        ))))
        val result = prepareAggregates(
            provider = provider,
            resources = mapOf(resource.id to resource),
            resourceTypes = mapOf(resourceType.id to resourceType),
            epochs = mapOf(resource.id to epoch),
            quotas = QuotasIndex(quotas = mapOf(root to mapOf(resource.id to setOf(rootQuota, rootProvision)))),
            hierarchy = ServiceHierarchy(
                roots = listOf(root),
                childrenByParent = mapOf(root to setOf(child)),
                nodesCount = 2),
            timestamp = now,
            usage = usage
        )
        Assertions.assertEquals(setOf(ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                ),
                subtree = null,
                total = null
            )
        )), result.aggregates.toSet())
        Assertions.assertEquals(setOf(ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = root,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            providerId = provider.id,
            transferable = Long.MAX_VALUE,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        )), result.denormalizedAggregates.toSet())
        Assertions.assertEquals(setOf(ServiceAggregateUsageModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
        )), result.usageAggregates.toSet())
    }

    @Test
    fun testRootAndChildOnlyUsageInChild() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = resourceStub(provider.id, resourceType.id)
        val epoch = AggregateEpochModel(
            key = AggregateEpochKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = provider.id,
                resourceId = resource.id
            ),
            epoch = 0,
        )
        val now = Instant.now()
        val root = 0L
        val child = 1L
        val folderIdRoot = UUID.randomUUID().toString()
        val folderIdChild = UUID.randomUUID().toString()
        val rootQuota = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 10L,
            balance = 5L,
            provided = null,
            allocated = null,
            serviceId = root,
            folderId = folderIdRoot,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val rootProvision = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 5L,
            allocated = 2L,
            serviceId = root,
            folderId = folderIdRoot,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val usage = UsageIndex(mapOf(root to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
        )), child to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = child,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
        ))))
        val result = prepareAggregates(
            provider = provider,
            resources = mapOf(resource.id to resource),
            resourceTypes = mapOf(resourceType.id to resourceType),
            epochs = mapOf(resource.id to epoch),
            quotas = QuotasIndex(quotas = mapOf(root to mapOf(resource.id to setOf(rootQuota, rootProvision)))),
            hierarchy = ServiceHierarchy(
                roots = listOf(root),
                childrenByParent = mapOf(root to setOf(child)),
                nodesCount = 2),
            timestamp = now,
            usage = usage
        )
        Assertions.assertEquals(setOf(ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                ),
                subtree = AggregateBundle(
                    quota = BigInteger.ZERO,
                    balance = BigInteger.ZERO,
                    provided = BigInteger.ZERO,
                    allocated = BigInteger.ZERO,
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.ZERO,
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.ZERO,
                    deallocatable = BigInteger.ZERO,
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                ),
                total = AggregateBundle(
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(2),
                    underutilizedEst = null
                )
            )
        ), ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = child,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            exactAmounts = ServiceAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.ZERO,
                    balance = BigInteger.ZERO,
                    provided = BigInteger.ZERO,
                    allocated = BigInteger.ZERO,
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.ZERO,
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.ZERO,
                    deallocatable = BigInteger.ZERO,
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                ),
                subtree = null,
                total = null
            )
        )), result.aggregates.toSet())
        Assertions.assertEquals(setOf(ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = root,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(8), BigInteger.valueOf(8)),
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = child,
                resourceId = resource.id,
                serviceId = child,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.ZERO, BigInteger.ZERO),
            deallocatable = 0L,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.ZERO,
                    balance = BigInteger.ZERO,
                    provided = BigInteger.ZERO,
                    allocated = BigInteger.ZERO,
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.ZERO,
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.ZERO,
                    deallocatable = BigInteger.ZERO,
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = child,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.ZERO, BigInteger.ZERO),
            deallocatable = 0L,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.ZERO,
                    balance = BigInteger.ZERO,
                    provided = BigInteger.ZERO,
                    allocated = BigInteger.ZERO,
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.ZERO,
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.ZERO,
                    deallocatable = BigInteger.ZERO,
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        )), result.denormalizedAggregates.toSet())
        Assertions.assertEquals(setOf(ServiceAggregateUsageModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                serviceId = child,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
        )), result.usageAggregates.toSet())
    }

    @Test
    fun testRootAndChildOnlyUsageInRoot() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = resourceStub(provider.id, resourceType.id)
        val epoch = AggregateEpochModel(
            key = AggregateEpochKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = provider.id,
                resourceId = resource.id
            ),
            epoch = 0,
        )
        val now = Instant.now()
        val root = 0L
        val child = 1L
        val folderIdRoot = UUID.randomUUID().toString()
        val folderIdChild = UUID.randomUUID().toString()
        val childQuota = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 30L,
            balance = 15L,
            provided = null,
            allocated = null,
            serviceId = child,
            folderId = folderIdChild,
            accountId = null,
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val childProvision = QuotaAggregationModel(
            resourceId = resource.id,
            quota = null,
            balance = null,
            provided = 15L,
            allocated = 6L,
            serviceId = child,
            folderId = folderIdChild,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val usage = UsageIndex(mapOf(root to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
        )), child to mapOf(resource.id to ServiceUsageModel(
            key = ServiceUsageKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = child,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            usage = ServiceUsageAmounts(
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
        ))))
        val result = prepareAggregates(
            provider = provider,
            resources = mapOf(resource.id to resource),
            resourceTypes = mapOf(resourceType.id to resourceType),
            epochs = mapOf(resource.id to epoch),
            quotas = QuotasIndex(quotas = mapOf(child to mapOf(resource.id to setOf(childQuota, childProvision)))),
            hierarchy = ServiceHierarchy(
                roots = listOf(root),
                childrenByParent = mapOf(root to setOf(child)),
                nodesCount = 2),
            timestamp = now,
            usage = usage
        )
        Assertions.assertEquals(setOf(ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            exactAmounts = ServiceAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.ZERO,
                    balance = BigInteger.ZERO,
                    provided = BigInteger.ZERO,
                    allocated = BigInteger.ZERO,
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.ZERO,
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.ZERO,
                    deallocatable = BigInteger.ZERO,
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                ),
                total = AggregateBundle(
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(2),
                    underutilizedEst = null
                )
            )
        ), ServiceAggregateModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = child,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                ),
                subtree = null,
                total = null
            )
        )), result.aggregates.toSet())
        Assertions.assertEquals(setOf(ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = root,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.ZERO, BigInteger.ZERO),
            deallocatable = 0L,
            exactAmounts = ServiceDenormalizedAggregateAmounts(
                own = AggregateBundle(
                    quota = BigInteger.ZERO,
                    balance = BigInteger.ZERO,
                    provided = BigInteger.ZERO,
                    allocated = BigInteger.ZERO,
                    usage = BigInteger.ZERO,
                    unallocated = BigInteger.ZERO,
                    unused = BigInteger.ZERO,
                    underutilized = BigInteger.ZERO,
                    transferable = BigInteger.ZERO,
                    deallocatable = BigInteger.ZERO,
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = child,
                resourceId = resource.id,
                serviceId = child,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        ), ServiceDenormalizedAggregateModel(
            key = ServiceDenormalizedAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                superTreeServiceId = root,
                resourceId = resource.id,
                serviceId = child,
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            providerId = provider.id,
            transferable = normalizeFraction(BigInteger.valueOf(24), BigInteger.valueOf(24)),
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
                    extUsage = null,
                    unusedEst = BigInteger.valueOf(1),
                    underutilizedEst = null
                )
            )
        )), result.denormalizedAggregates.toSet())
        Assertions.assertEquals(setOf(ServiceAggregateUsageModel(
            key = ServiceAggregateKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = root,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
                serviceId = child,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
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
        )), result.usageAggregates.toSet())
    }

    private fun providerStub() = ProviderModel.builder()
        .id(UUID.randomUUID().toString())
        .grpcApiUri("")
        .restApiUri("")
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
        .accountsSpacesSupported(false)
        .syncEnabled(true)
        .grpcTlsOn(true)
        .build()

    private fun resourceTypeStub(providerId: String) = ResourceTypeModel.builder()
        .id(UUID.randomUUID().toString())
        .tenantId(Tenants.DEFAULT_TENANT_ID)
        .providerId(providerId)
        .version(0L)
        .key("key")
        .nameEn("Test")
        .nameRu("Test")
        .descriptionEn("Test")
        .descriptionRu("Test")
        .deleted(false)
        .unitsEnsembleId(UUID.randomUUID().toString())
        .build()

    private fun resourceStub(providerId: String, resourceTypeId: String) = ResourceModel.builder()
        .id(UUID.randomUUID().toString())
        .tenantId(Tenants.DEFAULT_TENANT_ID)
        .version(0L)
        .key("key")
        .nameEn("Test")
        .nameRu("Test")
        .descriptionEn("Test")
        .descriptionRu("Test")
        .deleted(false)
        .unitsEnsembleId(UUID.randomUUID().toString())
        .providerId(providerId)
        .resourceTypeId(resourceTypeId)
        .segments(emptySet())
        .resourceUnits(ResourceUnitsModel(emptySet(), UUID.randomUUID().toString(), null))
        .managed(true)
        .orderable(true)
        .readOnly(false)
        .baseUnitId(UUID.randomUUID().toString())
        .accountsSpacesId(UUID.randomUUID().toString())
        .aggregationSettings(AggregationSettings(FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE, UsageMode.UNUSED_ESTIMATION_VALUE, null))
        .build()

}
