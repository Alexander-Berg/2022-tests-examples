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
import ru.yandex.intranet.d.model.usage.*
import java.math.BigInteger
import java.time.Instant
import java.util.*

/**
 * Quota aggregator test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
class QuotaAggregatorTest {

    @Test
    fun testEmpty() {
        val result = prepareAggregates(
            provider = providerStub(),
            resources = emptyMap(),
            resourceTypes = emptyMap(),
            epochs = emptyMap(),
            quotas = QuotasIndex(quotas = emptyMap()),
            hierarchy = ServiceHierarchy(roots = emptyList(), childrenByParent = emptyMap(), nodesCount = 0),
            timestamp = Instant.now(),
            usage = UsageIndex(mapOf())
        )
        Assertions.assertTrue(result.aggregates.isEmpty())
        Assertions.assertTrue(result.denormalizedAggregates.isEmpty())
    }

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
        val result = prepareAggregates(
            provider = provider,
            resources = mapOf(resource.id to resource),
            resourceTypes = mapOf(resourceType.id to resourceType),
            epochs = mapOf(resource.id to epoch),
            quotas = QuotasIndex(quotas = mapOf(root to mapOf(resource.id to setOf(quota, provision)))),
            hierarchy = ServiceHierarchy(roots = listOf(root), childrenByParent = emptyMap(), nodesCount = 1),
            timestamp = now,
            usage = UsageIndex(mapOf())
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
                    unusedEst = null,
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
                    unusedEst = null,
                    underutilizedEst = null
                )
            )
        )), result.denormalizedAggregates)
        Assertions.assertTrue(result.usageAggregates.isEmpty())
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
        val folderIdOne = UUID.randomUUID().toString()
        val folderIdTwo = UUID.randomUUID().toString()
        val quotaOne = QuotaAggregationModel(
            resourceId = resource.id,
            quota = 10L,
            balance = 5L,
            provided = null,
            allocated = null,
            serviceId = rootOne,
            folderId = folderIdOne,
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
            folderId = folderIdTwo,
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
            folderId = folderIdOne,
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
            folderId = folderIdTwo,
            accountId = UUID.randomUUID().toString(),
            folderType = FolderType.COMMON_DEFAULT_FOR_SERVICE
        )
        val result = prepareAggregates(
            provider = provider,
            resources = mapOf(resource.id to resource),
            resourceTypes = mapOf(resourceType.id to resourceType),
            epochs = mapOf(resource.id to epoch),
            quotas = QuotasIndex(quotas = mapOf(rootOne to mapOf(resource.id to setOf(quotaOne, provisionOne)),
                rootTwo to mapOf(resource.id to setOf(quotaTwo, provisionTwo)))),
            hierarchy = ServiceHierarchy(roots = listOf(rootOne, rootTwo), childrenByParent = emptyMap(), nodesCount = 2),
            timestamp = now,
            usage = UsageIndex(mapOf())
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
                    underutilizedEst = null
                )
            )
        )), result.denormalizedAggregates.toSet())
        Assertions.assertTrue(result.usageAggregates.isEmpty())
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
            usage = UsageIndex(mapOf())
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
                    underutilizedEst = null
                )
            )
        )), result.denormalizedAggregates.toSet())
        Assertions.assertTrue(result.usageAggregates.isEmpty())
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
            usage = UsageIndex(mapOf())
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
                    underutilizedEst = null
                )
            )
        )), result.denormalizedAggregates.toSet())
        Assertions.assertTrue(result.usageAggregates.isEmpty())
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
            usage = UsageIndex(mapOf())
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
                    underutilizedEst = null
                )
            )
        )), result.denormalizedAggregates.toSet())
        Assertions.assertTrue(result.usageAggregates.isEmpty())
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
            usage = UsageIndex(mapOf())
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
                    underutilizedEst = null
                )
            )
        )), result.denormalizedAggregates.toSet())
        Assertions.assertTrue(result.usageAggregates.isEmpty())
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
            usage = UsageIndex(mapOf())
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
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
                    unusedEst = null,
                    underutilizedEst = null
                )
            )
        )), result.denormalizedAggregates.toSet())
        Assertions.assertTrue(result.usageAggregates.isEmpty())
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
            usage = UsageIndex(mapOf())
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
                    unusedEst = null,
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
                    unusedEst = null,
                    underutilizedEst = null
                )
            )
        )), result.denormalizedAggregates.toSet())
        Assertions.assertTrue(result.usageAggregates.isEmpty())
    }

    @Test
    fun testSingleRootWithUsage() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = usageResourceStub(provider.id, resourceType.id)
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
        val result = prepareAggregates(
            provider = provider,
            resources = mapOf(resource.id to resource),
            resourceTypes = mapOf(resourceType.id to resourceType),
            epochs = mapOf(resource.id to epoch),
            quotas = QuotasIndex(quotas = mapOf(root to mapOf(resource.id to setOf(quota, provision)))),
            hierarchy = ServiceHierarchy(roots = listOf(root), childrenByParent = emptyMap(), nodesCount = 1),
            timestamp = now,
            usage = UsageIndex(mapOf(root to mapOf(resource.id to ServiceUsageModel(
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
                        accumulated = BigInteger.valueOf(1),
                        accumulatedDuration = 1L,
                        histogram = null,
                        values = null,
                        valuesX = listOf(0L),
                        valuesY = listOf(BigInteger.valueOf(1L)),
                        unused = null
                    ),
                    subtree = null,
                    total = null
                )
            ))))
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(4)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(4)
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
                    accumulated = BigInteger.valueOf(1),
                    accumulatedDuration = 1L,
                    histogram = null,
                    values = null,
                    valuesX = listOf(0L),
                    valuesY = listOf(BigInteger.valueOf(1L)),
                    unused = null
                ),
                subtree = null,
                total = null
            )
        )), result.usageAggregates)
    }

    @Test
    fun testTwoRootsWithUsage() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = usageResourceStub(provider.id, resourceType.id)
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
        val result = prepareAggregates(
            provider = provider,
            resources = mapOf(resource.id to resource),
            resourceTypes = mapOf(resourceType.id to resourceType),
            epochs = mapOf(resource.id to epoch),
            quotas = QuotasIndex(quotas = mapOf(rootOne to mapOf(resource.id to setOf(quotaOne, provisionOne)),
                rootTwo to mapOf(resource.id to setOf(quotaTwo, provisionTwo)))),
            hierarchy = ServiceHierarchy(roots = listOf(rootOne, rootTwo), childrenByParent = emptyMap(), nodesCount = 2),
            timestamp = now,
            usage = UsageIndex(mapOf(rootOne to mapOf(resource.id to ServiceUsageModel(
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
                        accumulated = BigInteger.valueOf(1),
                        accumulatedDuration = 1L,
                        histogram = null,
                        values = null,
                        valuesX = listOf(0L),
                        valuesY = listOf(BigInteger.valueOf(1L)),
                        unused = null
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
                        accumulated = BigInteger.valueOf(1),
                        accumulatedDuration = 1L,
                        histogram = null,
                        values = null,
                        valuesX = listOf(0L),
                        valuesY = listOf(BigInteger.valueOf(1L)),
                        unused = null
                    ),
                    subtree = null,
                    total = null
                )
            ))))
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(4)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(49)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(4)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(49)
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
                    accumulated = BigInteger.valueOf(1),
                    accumulatedDuration = 1L,
                    histogram = null,
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
                    accumulated = BigInteger.valueOf(1),
                    accumulatedDuration = 1L,
                    histogram = null,
                    values = null,
                    valuesX = listOf(0L),
                    valuesY = listOf(BigInteger.valueOf(1L)),
                    unused = null
                ),
                subtree = null,
                total = null
            )
        )), result.usageAggregates.toSet())
    }

    @Test
    fun testRootAndChildWithUsage() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = usageResourceStub(provider.id, resourceType.id)
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
            usage = UsageIndex(mapOf(root to mapOf(resource.id to ServiceUsageModel(
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
                        variance = BigInteger.valueOf(0),
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
                        variance = BigInteger.valueOf(0),
                        accumulated = BigInteger.valueOf(2),
                        accumulatedDuration = 1L,
                        histogram = listOf(HistogramBin(BigInteger.valueOf(2), BigInteger.valueOf(2), 1L)),
                        values = null,
                        valuesX = listOf(0L),
                        valuesY = listOf(BigInteger.valueOf(2L)),
                        unused = null
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
            ))))
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(4)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(18)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(4)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
                    variance = BigInteger.valueOf(0),
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
                    variance = BigInteger.valueOf(0),
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
                serviceId = child,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            exactAmounts = ServiceUsageAmounts(
                own = UsageAmount(
                    value = null,
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
        )), result.usageAggregates.toSet())
    }

    @Test
    fun testRootAndTwoChildrenWithUsage() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = usageResourceStub(provider.id, resourceType.id)
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
            usage = UsageIndex(mapOf(root to mapOf(resource.id to ServiceUsageModel(
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
                        average = BigInteger.valueOf(2),
                        min = BigInteger.valueOf(2),
                        max = BigInteger.valueOf(2),
                        median = BigInteger.valueOf(2),
                        variance = BigInteger.valueOf(0),
                        accumulated = BigInteger.valueOf(2),
                        accumulatedDuration = 1L,
                        histogram = listOf(HistogramBin(BigInteger.valueOf(2), BigInteger.valueOf(2), 1L)),
                        values = null,
                        valuesX = listOf(0L),
                        valuesY = listOf(BigInteger.valueOf(2L)),
                        unused = null
                    ),
                    total = UsageAmount(
                        value = null,
                        average = BigInteger.valueOf(3),
                        min = BigInteger.valueOf(3),
                        max = BigInteger.valueOf(3),
                        median = BigInteger.valueOf(3),
                        variance = BigInteger.valueOf(0),
                        accumulated = BigInteger.valueOf(3),
                        accumulatedDuration = 1L,
                        histogram = listOf(HistogramBin(BigInteger.valueOf(3), BigInteger.valueOf(3), 1L)),
                        values = null,
                        valuesX = listOf(0L),
                        valuesY = listOf(BigInteger.valueOf(3L)),
                        unused = null
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
            ))))
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(4)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(43)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(47)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(29)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(4)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(29)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(29)
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
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
                    average = BigInteger.valueOf(2),
                    min = BigInteger.valueOf(2),
                    max = BigInteger.valueOf(2),
                    median = BigInteger.valueOf(2),
                    variance = BigInteger.valueOf(0),
                    accumulated = BigInteger.valueOf(2),
                    accumulatedDuration = 1L,
                    histogram = listOf(HistogramBin(BigInteger.valueOf(2), BigInteger.valueOf(2), 1L)),
                    values = null,
                    valuesX = listOf(0L),
                    valuesY = listOf(BigInteger.valueOf(2L)),
                    unused = null
                ),
                total = UsageAmount(
                    value = null,
                    average = BigInteger.valueOf(3),
                    min = BigInteger.valueOf(3),
                    max = BigInteger.valueOf(3),
                    median = BigInteger.valueOf(3),
                    variance = BigInteger.valueOf(0),
                    accumulated = BigInteger.valueOf(3),
                    accumulatedDuration = 1L,
                    histogram = listOf(HistogramBin(BigInteger.valueOf(3), BigInteger.valueOf(3), 1L)),
                    values = null,
                    valuesX = listOf(0L),
                    valuesY = listOf(BigInteger.valueOf(3L)),
                    unused = null
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
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
                serviceId = childTwo,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            exactAmounts = ServiceUsageAmounts(
                own = UsageAmount(
                    value = null,
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
        )), result.usageAggregates.toSet())
    }

    @Test
    fun testRootAndChildAndGrandchildWithUsage() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = usageResourceStub(provider.id, resourceType.id)
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
            usage = UsageIndex(mapOf(root to mapOf(resource.id to ServiceUsageModel(
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
                        average = BigInteger.valueOf(2),
                        min = BigInteger.valueOf(2),
                        max = BigInteger.valueOf(2),
                        median = BigInteger.valueOf(2),
                        variance = BigInteger.valueOf(0),
                        accumulated = BigInteger.valueOf(2),
                        accumulatedDuration = 1L,
                        histogram = listOf(HistogramBin(BigInteger.valueOf(2), BigInteger.valueOf(2), 1L)),
                        values = null,
                        valuesX = listOf(0L),
                        valuesY = listOf(BigInteger.valueOf(2L)),
                        unused = null
                    ),
                    total = UsageAmount(
                        value = null,
                        average = BigInteger.valueOf(3),
                        min = BigInteger.valueOf(3),
                        max = BigInteger.valueOf(3),
                        median = BigInteger.valueOf(3),
                        variance = BigInteger.valueOf(0),
                        accumulated = BigInteger.valueOf(3),
                        accumulatedDuration = 1L,
                        histogram = listOf(HistogramBin(BigInteger.valueOf(3), BigInteger.valueOf(3), 1L)),
                        values = null,
                        valuesX = listOf(0L),
                        valuesY = listOf(BigInteger.valueOf(3L)),
                        unused = null
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
                        variance = BigInteger.valueOf(0),
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
                        variance = BigInteger.valueOf(0),
                        accumulated = BigInteger.valueOf(2),
                        accumulatedDuration = 1L,
                        histogram = listOf(HistogramBin(BigInteger.valueOf(2), BigInteger.valueOf(2), 1L)),
                        values = null,
                        valuesX = listOf(0L),
                        valuesY = listOf(BigInteger.valueOf(2L)),
                        unused = null
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
            ))))
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(4)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(43)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(47)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(29)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(43)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(29)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(4)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(29)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(29)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(29)
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
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
                    average = BigInteger.valueOf(2),
                    min = BigInteger.valueOf(2),
                    max = BigInteger.valueOf(2),
                    median = BigInteger.valueOf(2),
                    variance = BigInteger.valueOf(0),
                    accumulated = BigInteger.valueOf(2),
                    accumulatedDuration = 1L,
                    histogram = listOf(HistogramBin(BigInteger.valueOf(2), BigInteger.valueOf(2), 1L)),
                    values = null,
                    valuesX = listOf(0L),
                    valuesY = listOf(BigInteger.valueOf(2L)),
                    unused = null
                ),
                total = UsageAmount(
                    value = null,
                    average = BigInteger.valueOf(3),
                    min = BigInteger.valueOf(3),
                    max = BigInteger.valueOf(3),
                    median = BigInteger.valueOf(3),
                    variance = BigInteger.valueOf(0),
                    accumulated = BigInteger.valueOf(3),
                    accumulatedDuration = 1L,
                    histogram = listOf(HistogramBin(BigInteger.valueOf(3), BigInteger.valueOf(3), 1L)),
                    values = null,
                    valuesX = listOf(0L),
                    valuesY = listOf(BigInteger.valueOf(3L)),
                    unused = null
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
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
                    variance = BigInteger.valueOf(0),
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
                    variance = BigInteger.valueOf(0),
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
                serviceId = grandchild,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            exactAmounts = ServiceUsageAmounts(
                own = UsageAmount(
                    value = null,
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
        )), result.usageAggregates.toSet())
    }

    @Test
    fun testRootAndTwoChildrenAndTwoGrandchildrenWithUsage() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = usageResourceStub(provider.id, resourceType.id)
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
            usage = UsageIndex(mapOf(root to mapOf(resource.id to ServiceUsageModel(
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
                        variance = BigInteger.valueOf(0),
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
                        variance = BigInteger.valueOf(0),
                        accumulated = BigInteger.valueOf(5),
                        accumulatedDuration = 1L,
                        histogram = listOf(HistogramBin(BigInteger.valueOf(5), BigInteger.valueOf(5), 1L)),
                        values = null,
                        valuesX = listOf(0L),
                        valuesY = listOf(BigInteger.valueOf(5L)),
                        unused = null
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
                        variance = BigInteger.valueOf(0),
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
                        variance = BigInteger.valueOf(0),
                        accumulated = BigInteger.valueOf(2),
                        accumulatedDuration = 1L,
                        histogram = listOf(HistogramBin(BigInteger.valueOf(2), BigInteger.valueOf(2), 1L)),
                        values = null,
                        valuesX = listOf(0L),
                        valuesY = listOf(BigInteger.valueOf(2L)),
                        unused = null
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
                        variance = BigInteger.valueOf(0),
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
                        variance = BigInteger.valueOf(0),
                        accumulated = BigInteger.valueOf(2),
                        accumulatedDuration = 1L,
                        histogram = listOf(HistogramBin(BigInteger.valueOf(2), BigInteger.valueOf(2), 1L)),
                        values = null,
                        valuesX = listOf(0L),
                        valuesY = listOf(BigInteger.valueOf(2L)),
                        unused = null
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
            ))))
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(4L)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(86)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(90)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(28)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(29)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(29)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(58)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(29)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(4)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(29)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(29)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(29)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(29)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(29)
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
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
                    variance = BigInteger.valueOf(0),
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
                    variance = BigInteger.valueOf(0),
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
                serviceId = childOne,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            exactAmounts = ServiceUsageAmounts(
                own = UsageAmount(
                    value = null,
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
                    variance = BigInteger.valueOf(0),
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
                    variance = BigInteger.valueOf(0),
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
                serviceId = childTwo,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            exactAmounts = ServiceUsageAmounts(
                own = UsageAmount(
                    value = null,
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
                    variance = BigInteger.valueOf(0),
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
                    variance = BigInteger.valueOf(0),
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
                serviceId = grandchildOne,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            exactAmounts = ServiceUsageAmounts(
                own = UsageAmount(
                    value = null,
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
                serviceId = grandchildTwo,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            exactAmounts = ServiceUsageAmounts(
                own = UsageAmount(
                    value = null,
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
        )), result.usageAggregates.toSet())
    }

    @Test
    fun testRootAndChildEmptyRootWithUsage() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = usageResourceStub(provider.id, resourceType.id)
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
            usage = UsageIndex(mapOf(root to mapOf(resource.id to ServiceUsageModel(
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
                        accumulated = BigInteger.valueOf(1),
                        accumulatedDuration = 1L,
                        histogram = listOf(HistogramBin(BigInteger.valueOf(1), BigInteger.valueOf(1), 1L)),
                        values = null,
                        valuesX = listOf(0L),
                        valuesY = listOf(BigInteger.valueOf(1L)),
                        unused = null
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
            ))))
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
                    accumulated = BigInteger.valueOf(1),
                    accumulatedDuration = 1L,
                    histogram = listOf(HistogramBin(BigInteger.valueOf(1), BigInteger.valueOf(1), 1L)),
                    values = null,
                    valuesX = listOf(0L),
                    valuesY = listOf(BigInteger.valueOf(1L)),
                    unused = null
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
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
        )), result.usageAggregates.toSet())
    }

    @Test
    fun testRootAndChildEmptyChildWithUsage() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = usageResourceStub(provider.id, resourceType.id)
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
            usage = UsageIndex(mapOf(root to mapOf(resource.id to ServiceUsageModel(
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
            ))))
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(4)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(4)
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
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
        )), result.usageAggregates.toSet())
    }

    @Test
    fun testRootAndChildWithUsageOnlyUsageInChild() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = usageResourceStub(provider.id, resourceType.id)
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
            usage = UsageIndex(mapOf(root to mapOf(resource.id to ServiceUsageModel(
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
                        variance = BigInteger.valueOf(0),
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
                        variance = BigInteger.valueOf(0),
                        accumulated = BigInteger.valueOf(2),
                        accumulatedDuration = 1L,
                        histogram = listOf(HistogramBin(BigInteger.valueOf(2), BigInteger.valueOf(2), 1L)),
                        values = null,
                        valuesX = listOf(0L),
                        valuesY = listOf(BigInteger.valueOf(2L)),
                        unused = null
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
            ))))
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(4)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(-1)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(3)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(-1)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(4)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(-1)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(-1)
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
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
                    variance = BigInteger.valueOf(0),
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
                    variance = BigInteger.valueOf(0),
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
                serviceId = child,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            exactAmounts = ServiceUsageAmounts(
                own = UsageAmount(
                    value = null,
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
        )), result.usageAggregates.toSet())
    }

    @Test
    fun testRootAndChildWithUsageOnlyUsageInRoot() {
        val provider = providerStub()
        val resourceType = resourceTypeStub(provider.id)
        val resource = usageResourceStub(provider.id, resourceType.id)
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
            usage = UsageIndex(mapOf(root to mapOf(resource.id to ServiceUsageModel(
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
                        variance = BigInteger.valueOf(0),
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
                        variance = BigInteger.valueOf(0),
                        accumulated = BigInteger.valueOf(2),
                        accumulatedDuration = 1L,
                        histogram = listOf(HistogramBin(BigInteger.valueOf(2), BigInteger.valueOf(2), 1L)),
                        values = null,
                        valuesX = listOf(0L),
                        valuesY = listOf(BigInteger.valueOf(2L)),
                        unused = null
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
                        average = BigInteger.valueOf(1),
                        min = BigInteger.valueOf(1),
                        max = BigInteger.valueOf(1),
                        median = BigInteger.valueOf(1),
                        variance = BigInteger.valueOf(0),
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
            ))))
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(-1L)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(13)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(-1)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    extUsage = true,
                    unusedEst = null,
                    underutilizedEst = BigInteger.valueOf(14)
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
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
                    variance = BigInteger.valueOf(0),
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
                    variance = BigInteger.valueOf(0),
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
                serviceId = child,
                providerId = provider.id,
                resourceId = resource.id
            ),
            lastUpdate = now,
            epoch = epoch.epoch,
            exactAmounts = ServiceUsageAmounts(
                own = UsageAmount(
                    value = null,
                    average = BigInteger.valueOf(1),
                    min = BigInteger.valueOf(1),
                    max = BigInteger.valueOf(1),
                    median = BigInteger.valueOf(1),
                    variance = BigInteger.valueOf(0),
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
        .aggregationSettings(AggregationSettings(FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE, UsageMode.UNDEFINED, null))
        .build()

    private fun usageResourceStub(providerId: String, resourceTypeId: String) = ResourceModel.builder()
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
        .aggregationSettings(AggregationSettings(FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE, UsageMode.TIME_SERIES, 1L))
        .build()

}
