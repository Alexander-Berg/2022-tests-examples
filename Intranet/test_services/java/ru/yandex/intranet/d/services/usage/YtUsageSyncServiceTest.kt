package ru.yandex.intranet.d.services.usage

import com.google.protobuf.util.Timestamps
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.UnitIds
import ru.yandex.intranet.d.UnitsEnsembleIds
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.accounts.AccountsDao
import ru.yandex.intranet.d.dao.accounts.AccountsSpacesDao
import ru.yandex.intranet.d.dao.folders.FolderDao
import ru.yandex.intranet.d.dao.providers.ProvidersDao
import ru.yandex.intranet.d.dao.resources.ResourcesDao
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao
import ru.yandex.intranet.d.dao.services.ServicesDao
import ru.yandex.intranet.d.dao.usage.AccountUsageDao
import ru.yandex.intranet.d.dao.usage.FolderUsageDao
import ru.yandex.intranet.d.dao.usage.ServiceUsageDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.accounts.AccountModel
import ru.yandex.intranet.d.model.accounts.AccountSpaceModel
import ru.yandex.intranet.d.model.folders.FolderModel
import ru.yandex.intranet.d.model.folders.FolderType
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel
import ru.yandex.intranet.d.model.providers.AggregationSettings
import ru.yandex.intranet.d.model.providers.FreeProvisionAggregationMode
import ru.yandex.intranet.d.model.providers.ProviderModel
import ru.yandex.intranet.d.model.providers.UsageMode
import ru.yandex.intranet.d.model.resources.ResourceModel
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel
import ru.yandex.intranet.d.model.services.ServiceRecipeModel
import ru.yandex.intranet.d.model.services.ServiceState
import ru.yandex.intranet.d.model.usage.AccountUsageKey
import ru.yandex.intranet.d.model.usage.AccountUsageModel
import ru.yandex.intranet.d.model.usage.FolderUsageKey
import ru.yandex.intranet.d.model.usage.FolderUsageModel
import ru.yandex.intranet.d.model.usage.ServiceUsageAmounts
import ru.yandex.intranet.d.model.usage.ServiceUsageKey
import ru.yandex.intranet.d.model.usage.ServiceUsageModel
import ru.yandex.intranet.d.model.usage.UsageAmount
import ru.yandex.intranet.d.services.integration.solomon.SolomonClientStub
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.settings.KnownProvidersDto
import ru.yandex.intranet.d.web.model.settings.YtUsageSyncSettingsDto
import ru.yandex.monitoring.api.v3.DoubleValues
import ru.yandex.monitoring.api.v3.Timeseries
import java.math.BigInteger
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * YT usage sync service test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class YtUsageSyncServiceTest(
    @Autowired private val providersDao: ProvidersDao,
    @Autowired private val resourceTypesDao: ResourceTypesDao,
    @Autowired private val resourcesDao: ResourcesDao,
    @Autowired private val servicesDao: ServicesDao,
    @Autowired private val folderDao: FolderDao,
    @Autowired private val accountsDao: AccountsDao,
    @Autowired private val resourceSegmentationsDao: ResourceSegmentationsDao,
    @Autowired private val resourceSegmentsDao: ResourceSegmentsDao,
    @Autowired private val accountsSpacesDao: AccountsSpacesDao,
    @Autowired private val serviceUsageDao: ServiceUsageDao,
    @Autowired private val accountUsageDao: AccountUsageDao,
    @Autowired private val folderUsageDao: FolderUsageDao,
    @Autowired private val tableClient: YdbTableClient,
    @Autowired private val webClient: WebTestClient,
    @Autowired private val solomonClient: SolomonClientStub,
    @Autowired private val ytUsageSyncService: YtUsageSyncService
) {

    @Test
    fun testSync(): Unit = runBlocking {
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
        val providerYt = providerModel("yt")
        val providerYp = providerModel("yp")
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProvidersRetryable(txSession,
            listOf(providerYt, providerYp)).awaitSingleOrNull() }}
        val resourceTypeCpuStrong = resourceTypeModel(providerYt.id, "cpu_strong", UnitsEnsembleIds.CPU_UNITS_ID, UnitIds.MILLICORES)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceTypeCpuStrong)
            .awaitSingleOrNull() }}
        val segmentationCluster = resourceSegmentationModel(providerYt.id, "cluster")
        val segmentationScope = resourceSegmentationModel(providerYt.id, "scope")
        val segmentationPoolTree = resourceSegmentationModel(providerYt.id, "pool_tree")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentationsDao.upsertResourceSegmentationsRetryable(txSession,
            listOf(segmentationCluster, segmentationScope, segmentationPoolTree)).awaitSingleOrNull() }}
        val segmentHahn = resourceSegmentModel(segmentationCluster.id, "hahn")
        val segmentArnold = resourceSegmentModel(segmentationCluster.id, "arnold")
        val segmentCompute = resourceSegmentModel(segmentationScope.id, "compute")
        val segmentPhysical = resourceSegmentModel(segmentationPoolTree.id, "physical")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession,
            listOf(segmentHahn, segmentArnold, segmentCompute, segmentPhysical)).awaitSingleOrNull() }}
        val accountSpaceHahn = accountsSpaceModel(providerYt.id, "hahn_compute_physical",
            mapOf(segmentationCluster.id to segmentHahn.id, segmentationScope.id to segmentCompute.id, segmentationPoolTree.id to segmentPhysical.id))
        val accountSpaceArnold = accountsSpaceModel(providerYt.id, "arnold_compute_physical",
            mapOf(segmentationCluster.id to segmentArnold.id, segmentationScope.id to segmentCompute.id, segmentationPoolTree.id to segmentPhysical.id))
        dbSessionRetryable(tableClient) { rwTxRetryable { accountsSpacesDao.upsertAllRetryable(txSession,
            listOf(accountSpaceHahn, accountSpaceArnold)).awaitSingleOrNull() }}
        val resourceCpuStrongHahn = resourceModel(providerYt.id, "hahn_compute_physical_cpu_strong", resourceTypeCpuStrong.id,
            mapOf(segmentationCluster.id to segmentHahn.id, segmentationScope.id to segmentCompute.id, segmentationPoolTree.id to segmentPhysical.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES, accountSpaceHahn.id)
        val resourceCpuStrongArnold = resourceModel(providerYt.id, "arnold_compute_physical_cpu_strong", resourceTypeCpuStrong.id,
            mapOf(segmentationCluster.id to segmentArnold.id, segmentationScope.id to segmentCompute.id, segmentationPoolTree.id to segmentPhysical.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES, accountSpaceArnold.id)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourcesRetryable(txSession,
            listOf(resourceCpuStrongHahn, resourceCpuStrongArnold)).awaitSingleOrNull() }}
        val accountRootFirst = accountModel(providerYt.id, accountSpaceHahn.id, folderRoot.id, "test", "one")
        val accountRootSecond = accountModel(providerYt.id, accountSpaceArnold.id, folderRoot.id, "test", "two")
        val accountChildOneFirst = accountModel(providerYt.id, accountSpaceHahn.id, folderChildOne.id, "test", "three")
        val accountChildOneSecond = accountModel(providerYt.id, accountSpaceHahn.id, folderChildOne.id, "test", "four")
        val accountChildTwoFirst = accountModel(providerYt.id, accountSpaceHahn.id, folderChildTwo.id, "test", "five")
        val accountChildTwoSecond = accountModel(providerYt.id, accountSpaceArnold.id, folderChildTwo.id, "test", "six")
        val accountGrandchildOneFirst = accountModel(providerYt.id, accountSpaceHahn.id, folderGrandchildOne.id, "test", "seven")
        val accountGrandchildOneSecond = accountModel(providerYt.id, accountSpaceArnold.id, folderGrandchildOne.id, "test", "eight")
        val accountGrandchildTwoFirst = accountModel(providerYt.id, accountSpaceArnold.id, folderGrandchildTwo.id, "test", "nine")
        val accountGrandchildTwoSecond = accountModel(providerYt.id, accountSpaceArnold.id, folderGrandchildTwo.id, "test", "ten")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRootFirst, accountRootSecond, accountChildOneFirst,
                accountChildOneSecond, accountChildTwoFirst, accountChildTwoSecond, accountGrandchildOneFirst,
                accountGrandchildOneSecond, accountGrandchildTwoFirst, accountGrandchildTwoSecond)).awaitSingleOrNull()
        }}
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
        val intervalStart = now.atZone(ZoneOffset.UTC).toLocalDate().minusDays(7)
            .atStartOfDay(ZoneOffset.UTC).toInstant()
        val solomonResponse = prepareSolomonResponse(intervalStart)
        solomonClient.setStubData(solomonResponse)
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/admin/settings/knownProviders")
            .bodyValue(KnownProvidersDto(providerYt.id, providerYp.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/admin/settings/ytUsageSync")
            .bodyValue(YtUsageSyncSettingsDto(resourceTypeCpuStrong.id, segmentationPoolTree.id, segmentPhysical.id,
                segmentationCluster.id, segmentationScope.id, segmentCompute.id, true, 300L,
                UnitsEnsembleIds.CPU_UNITS_ID, UnitIds.CORES))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
        ytUsageSyncService.doSync(Clock.fixed(now, ZoneOffset.UTC))
        val serviceUsageHahnGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYt.id, resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYt.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(mapOf(0L to 19L, 300L to 20L, 600L to 21L), intervalStart),
                subtree = null,
                total = null
            )
        ), serviceUsageHahnGrandchildOne)
        val serviceUsageArnoldGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYt.id, resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYt.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(mapOf(0L to 22L, 300L to 23L, 600L to 24L), intervalStart),
                subtree = null,
                total = null
            )
        ), serviceUsageArnoldGrandchildOne)
        val serviceUsageHahnGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYt.id, resourceCpuStrongHahn.id)) }
        Assertions.assertNull(serviceUsageHahnGrandchildTwo)
        val serviceUsageArnoldGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYt.id, resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYt.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(mapOf(0L to 25L + 28L, 300L to 26L + 29L, 600L to 27L + 30L), intervalStart),
                subtree = null,
                total = null
            )
        ), serviceUsageArnoldGrandchildTwo)
        val serviceUsageHahnChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYt.id, resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYt.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(mapOf(0L to 7L + 10L, 300L to 8L + 11L, 600L to 9L + 12L), intervalStart),
                subtree = usageAmount(mapOf(0L to 19L, 300L to 20L, 600L to 21L), intervalStart),
                total = usageAmount(mapOf(0L to 7L + 10L + 19L, 300L to 8L + 11L + 20L, 600L to 9L + 12L + 21L), intervalStart)
            )
        ), serviceUsageHahnChildOne)
        val serviceUsageArnoldChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYt.id, resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYt.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = null,
                subtree = usageAmount(mapOf(0L to 22L, 300L to 23L, 600L to 24L), intervalStart),
                total = usageAmount(mapOf(0L to 22L, 300L to 23L, 600L to 24L), intervalStart)
            )
        ), serviceUsageArnoldChildOne)
        val serviceUsageHahnChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYt.id, resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYt.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(mapOf(0L to 13L, 300L to 14L, 600L to 15L), intervalStart),
                subtree = null,
                total = null
            )
        ), serviceUsageHahnChildTwo)
        val serviceUsageArnoldChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYt.id, resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYt.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(mapOf(0L to 16L, 300L to 17L, 600L to 18L), intervalStart),
                subtree = usageAmount(mapOf(0L to 25L + 28L, 300L to 26L + 29L, 600L to 27L + 30L), intervalStart),
                total = usageAmount(mapOf(0L to 16L + 25L + 28L, 300L to 17L + 26L + 29L, 600L to 18L + 27L + 30L), intervalStart)
            )
        ), serviceUsageArnoldChildTwo)
        val serviceUsageHahnRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYt.id, resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYt.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(mapOf(0L to 1L, 300L to 2L, 600L to 3L), intervalStart),
                subtree = usageAmount(mapOf(0L to 7L + 10L + 13L + 19L, 300L to 8L + 11L + 14L + 20L, 600L to 9L + 12L + 15L + 21L), intervalStart),
                total = usageAmount(mapOf(0L to 1L + 7L + 10L + 13L + 19L, 300L to 2L + 8L + 11L + 14L + 20L, 600L to 3L + 9L + 12L + 15L + 21L), intervalStart)
            )
        ), serviceUsageHahnRoot)
        val serviceUsageArnoldRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYt.id, resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYt.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(mapOf(0L to 4L, 300L to 5L, 600L to 6L), intervalStart),
                subtree = usageAmount(mapOf(0L to 16L + 22L + 25L + 28L, 300L to 17L + 23L + 26L + 29L, 600L to 18L + 24L + 27L + 30L), intervalStart),
                total = usageAmount(mapOf(0L to 4L + 16L + 22L + 25L + 28L, 300L to 5L + 17L + 23L + 26L + 29L, 600L to 6L + 18L + 24L + 27L + 30L), intervalStart)
            )
        ), serviceUsageArnoldRoot)
        val folderUsageHahnGrandchildOne = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderGrandchildOne.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(FolderUsageModel(
            key = FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderGrandchildOne.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 0,
            ownUsage = usageAmount(mapOf(0L to 19L, 300L to 20L, 600L to 21L), intervalStart)
        ), folderUsageHahnGrandchildOne)
        val folderUsageArnoldGrandchildOne = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderGrandchildOne.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(FolderUsageModel(
            key = FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderGrandchildOne.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 0,
            ownUsage = usageAmount(mapOf(0L to 22L, 300L to 23L, 600L to 24L), intervalStart)
        ), folderUsageArnoldGrandchildOne)
        val folderUsageHahnGrandchildTwo = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderGrandchildTwo.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertNull(folderUsageHahnGrandchildTwo)
        val folderUsageArnoldGrandchildTwo = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderGrandchildTwo.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(FolderUsageModel(
            key = FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderGrandchildTwo.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 0,
            ownUsage = usageAmount(mapOf(0L to 25L + 28L, 300L to 26L + 29L, 600L to 27L + 30L), intervalStart)
        ), folderUsageArnoldGrandchildTwo)
        val folderUsageHahnChildOne = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderChildOne.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(FolderUsageModel(
            key = FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderChildOne.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 0,
            ownUsage = usageAmount(mapOf(0L to 7L + 10L, 300L to 8L + 11L, 600L to 9L + 12L), intervalStart)
        ), folderUsageHahnChildOne)
        val folderUsageArnoldChildOne = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderChildOne.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertNull(folderUsageArnoldChildOne)
        val folderUsageHahnChildTwo = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderChildTwo.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(FolderUsageModel(
            key = FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderChildTwo.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 0,
            ownUsage = usageAmount(mapOf(0L to 13L, 300L to 14L, 600L to 15L), intervalStart)
        ), folderUsageHahnChildTwo)
        val folderUsageArnoldChildTwo = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderChildTwo.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(FolderUsageModel(
            key = FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderChildTwo.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 0,
            ownUsage = usageAmount(mapOf(0L to 16L, 300L to 17L, 600L to 18L), intervalStart)
        ), folderUsageArnoldChildTwo)
        val folderUsageHahnRoot = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderRoot.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(FolderUsageModel(
            key = FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderRoot.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 0,
            ownUsage = usageAmount(mapOf(0L to 1L, 300L to 2L, 600L to 3L), intervalStart)
        ), folderUsageHahnRoot)
        val folderUsageArnoldRoot = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderRoot.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(FolderUsageModel(
            key = FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderRoot.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 0,
            ownUsage = usageAmount(mapOf(0L to 4L, 300L to 5L, 600L to 6L), intervalStart)
        ), folderUsageArnoldRoot)
        val accountUsageHahnGrandchildFirst = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountGrandchildOneFirst.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountGrandchildOneFirst.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 0,
            ownUsage = usageAmount(mapOf(0L to 19L, 300L to 20L, 600L to 21L), intervalStart)
        ), accountUsageHahnGrandchildFirst)
        val accountUsageArnoldGrandchildOne = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountGrandchildOneSecond.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountGrandchildOneSecond.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 0,
            ownUsage = usageAmount(mapOf(0L to 22L, 300L to 23L, 600L to 24L), intervalStart)
        ), accountUsageArnoldGrandchildOne)
        val accountUsageArnoldGrandchildTwoFirst = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountGrandchildTwoFirst.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountGrandchildTwoFirst.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 0,
            ownUsage = usageAmount(mapOf(0L to 25L, 300L to 26L, 600L to 27L), intervalStart)
        ), accountUsageArnoldGrandchildTwoFirst)
        val accountUsageArnoldGrandchildTwoSecond = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountGrandchildTwoSecond.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountGrandchildTwoSecond.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 0,
            ownUsage = usageAmount(mapOf(0L to 28L, 300L to 29L, 600L to 30L), intervalStart)
        ), accountUsageArnoldGrandchildTwoSecond)
        val accountUsageHahnChildOneFirst = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountChildOneFirst.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountChildOneFirst.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 0,
            ownUsage = usageAmount(mapOf(0L to 7L, 300L to 8L, 600L to 9L), intervalStart)
        ), accountUsageHahnChildOneFirst)
        val accountUsageHahnChildOneSecond = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountChildOneSecond.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountChildOneSecond.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 0,
            ownUsage = usageAmount(mapOf(0L to 10L, 300L to 11L, 600L to 12L), intervalStart)
        ), accountUsageHahnChildOneSecond)
        val accountUsageHahnChildTwo = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountChildTwoFirst.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountChildTwoFirst.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 0,
            ownUsage = usageAmount(mapOf(0L to 13L, 300L to 14L, 600L to 15L), intervalStart)
        ), accountUsageHahnChildTwo)
        val accountUsageArnoldChildTwo = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountChildTwoSecond.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountChildTwoSecond.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 0,
            ownUsage = usageAmount(mapOf(0L to 16L, 300L to 17L, 600L to 18L), intervalStart)
        ), accountUsageArnoldChildTwo)
        val accountUsageHahnRoot = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountRootFirst.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountRootFirst.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 0,
            ownUsage = usageAmount(mapOf(0L to 1L, 300L to 2L, 600L to 3L), intervalStart)
        ), accountUsageHahnRoot)
        val accountUsageArnoldRoot = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountRootSecond.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountRootSecond.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 0,
            ownUsage = usageAmount(mapOf(0L to 4L, 300L to 5L, 600L to 6L), intervalStart)
        ), accountUsageArnoldRoot)
    }

    @Test
    fun testSyncAgain(): Unit = runBlocking {
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
        val providerYt = providerModel("yt")
        val providerYp = providerModel("yp")
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProvidersRetryable(txSession,
            listOf(providerYt, providerYp)).awaitSingleOrNull() }}
        val resourceTypeCpuStrong = resourceTypeModel(providerYt.id, "cpu_strong", UnitsEnsembleIds.CPU_UNITS_ID, UnitIds.MILLICORES)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceTypeCpuStrong)
            .awaitSingleOrNull() }}
        val segmentationCluster = resourceSegmentationModel(providerYt.id, "cluster")
        val segmentationScope = resourceSegmentationModel(providerYt.id, "scope")
        val segmentationPoolTree = resourceSegmentationModel(providerYt.id, "pool_tree")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentationsDao.upsertResourceSegmentationsRetryable(txSession,
            listOf(segmentationCluster, segmentationScope, segmentationPoolTree)).awaitSingleOrNull() }}
        val segmentHahn = resourceSegmentModel(segmentationCluster.id, "hahn")
        val segmentArnold = resourceSegmentModel(segmentationCluster.id, "arnold")
        val segmentCompute = resourceSegmentModel(segmentationScope.id, "compute")
        val segmentPhysical = resourceSegmentModel(segmentationPoolTree.id, "physical")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession,
            listOf(segmentHahn, segmentArnold, segmentCompute, segmentPhysical)).awaitSingleOrNull() }}
        val accountSpaceHahn = accountsSpaceModel(providerYt.id, "hahn_compute_physical",
            mapOf(segmentationCluster.id to segmentHahn.id, segmentationScope.id to segmentCompute.id, segmentationPoolTree.id to segmentPhysical.id))
        val accountSpaceArnold = accountsSpaceModel(providerYt.id, "arnold_compute_physical",
            mapOf(segmentationCluster.id to segmentArnold.id, segmentationScope.id to segmentCompute.id, segmentationPoolTree.id to segmentPhysical.id))
        dbSessionRetryable(tableClient) { rwTxRetryable { accountsSpacesDao.upsertAllRetryable(txSession,
            listOf(accountSpaceHahn, accountSpaceArnold)).awaitSingleOrNull() }}
        val resourceCpuStrongHahn = resourceModel(providerYt.id, "hahn_compute_physical_cpu_strong", resourceTypeCpuStrong.id,
            mapOf(segmentationCluster.id to segmentHahn.id, segmentationScope.id to segmentCompute.id, segmentationPoolTree.id to segmentPhysical.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES, accountSpaceHahn.id)
        val resourceCpuStrongArnold = resourceModel(providerYt.id, "arnold_compute_physical_cpu_strong", resourceTypeCpuStrong.id,
            mapOf(segmentationCluster.id to segmentArnold.id, segmentationScope.id to segmentCompute.id, segmentationPoolTree.id to segmentPhysical.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES, accountSpaceArnold.id)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourcesRetryable(txSession,
            listOf(resourceCpuStrongHahn, resourceCpuStrongArnold)).awaitSingleOrNull() }}
        val accountRootFirst = accountModel(providerYt.id, accountSpaceHahn.id, folderRoot.id, "test", "one")
        val accountRootSecond = accountModel(providerYt.id, accountSpaceArnold.id, folderRoot.id, "test", "two")
        val accountChildOneFirst = accountModel(providerYt.id, accountSpaceHahn.id, folderChildOne.id, "test", "three")
        val accountChildOneSecond = accountModel(providerYt.id, accountSpaceHahn.id, folderChildOne.id, "test", "four")
        val accountChildTwoFirst = accountModel(providerYt.id, accountSpaceHahn.id, folderChildTwo.id, "test", "five")
        val accountChildTwoSecond = accountModel(providerYt.id, accountSpaceArnold.id, folderChildTwo.id, "test", "six")
        val accountGrandchildOneFirst = accountModel(providerYt.id, accountSpaceHahn.id, folderGrandchildOne.id, "test", "seven")
        val accountGrandchildOneSecond = accountModel(providerYt.id, accountSpaceArnold.id, folderGrandchildOne.id, "test", "eight")
        val accountGrandchildTwoFirst = accountModel(providerYt.id, accountSpaceArnold.id, folderGrandchildTwo.id, "test", "nine")
        val accountGrandchildTwoSecond = accountModel(providerYt.id, accountSpaceArnold.id, folderGrandchildTwo.id, "test", "ten")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRootFirst, accountRootSecond, accountChildOneFirst,
                accountChildOneSecond, accountChildTwoFirst, accountChildTwoSecond, accountGrandchildOneFirst,
                accountGrandchildOneSecond, accountGrandchildTwoFirst, accountGrandchildTwoSecond)).awaitSingleOrNull()
        }}
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
        val intervalStart = now.atZone(ZoneOffset.UTC).toLocalDate().minusDays(7)
            .atStartOfDay(ZoneOffset.UTC).toInstant()
        val solomonResponse = prepareSolomonResponse(intervalStart)
        solomonClient.setStubData(solomonResponse)
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/admin/settings/knownProviders")
            .bodyValue(KnownProvidersDto(providerYt.id, providerYp.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/admin/settings/ytUsageSync")
            .bodyValue(YtUsageSyncSettingsDto(resourceTypeCpuStrong.id, segmentationPoolTree.id, segmentPhysical.id,
                segmentationCluster.id, segmentationScope.id, segmentCompute.id, true, 300L,
                UnitsEnsembleIds.CPU_UNITS_ID, UnitIds.CORES))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
        ytUsageSyncService.doSync(Clock.fixed(now, ZoneOffset.UTC))
        ytUsageSyncService.doSync(Clock.fixed(now, ZoneOffset.UTC))
        val serviceUsageHahnGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYt.id, resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYt.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(mapOf(0L to 19L, 300L to 20L, 600L to 21L), intervalStart),
                subtree = null,
                total = null
            )
        ), serviceUsageHahnGrandchildOne)
        val serviceUsageArnoldGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYt.id, resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYt.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(mapOf(0L to 22L, 300L to 23L, 600L to 24L), intervalStart),
                subtree = null,
                total = null
            )
        ), serviceUsageArnoldGrandchildOne)
        val serviceUsageHahnGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYt.id, resourceCpuStrongHahn.id)) }
        Assertions.assertNull(serviceUsageHahnGrandchildTwo)
        val serviceUsageArnoldGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYt.id, resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYt.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(mapOf(0L to 25L + 28L, 300L to 26L + 29L, 600L to 27L + 30L), intervalStart),
                subtree = null,
                total = null
            )
        ), serviceUsageArnoldGrandchildTwo)
        val serviceUsageHahnChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYt.id, resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYt.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(mapOf(0L to 7L + 10L, 300L to 8L + 11L, 600L to 9L + 12L), intervalStart),
                subtree = usageAmount(mapOf(0L to 19L, 300L to 20L, 600L to 21L), intervalStart),
                total = usageAmount(mapOf(0L to 7L + 10L + 19L, 300L to 8L + 11L + 20L, 600L to 9L + 12L + 21L), intervalStart)
            )
        ), serviceUsageHahnChildOne)
        val serviceUsageArnoldChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYt.id, resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYt.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = null,
                subtree = usageAmount(mapOf(0L to 22L, 300L to 23L, 600L to 24L), intervalStart),
                total = usageAmount(mapOf(0L to 22L, 300L to 23L, 600L to 24L), intervalStart)
            )
        ), serviceUsageArnoldChildOne)
        val serviceUsageHahnChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYt.id, resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYt.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(mapOf(0L to 13L, 300L to 14L, 600L to 15L), intervalStart),
                subtree = null,
                total = null
            )
        ), serviceUsageHahnChildTwo)
        val serviceUsageArnoldChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYt.id, resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYt.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(mapOf(0L to 16L, 300L to 17L, 600L to 18L), intervalStart),
                subtree = usageAmount(mapOf(0L to 25L + 28L, 300L to 26L + 29L, 600L to 27L + 30L), intervalStart),
                total = usageAmount(mapOf(0L to 16L + 25L + 28L, 300L to 17L + 26L + 29L, 600L to 18L + 27L + 30L), intervalStart)
            )
        ), serviceUsageArnoldChildTwo)
        val serviceUsageHahnRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYt.id, resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYt.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(mapOf(0L to 1L, 300L to 2L, 600L to 3L), intervalStart),
                subtree = usageAmount(mapOf(0L to 7L + 10L + 13L + 19L, 300L to 8L + 11L + 14L + 20L, 600L to 9L + 12L + 15L + 21L), intervalStart),
                total = usageAmount(mapOf(0L to 1L + 7L + 10L + 13L + 19L, 300L to 2L + 8L + 11L + 14L + 20L, 600L to 3L + 9L + 12L + 15L + 21L), intervalStart)
            )
        ), serviceUsageHahnRoot)
        val serviceUsageArnoldRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYt.id, resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYt.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(mapOf(0L to 4L, 300L to 5L, 600L to 6L), intervalStart),
                subtree = usageAmount(mapOf(0L to 16L + 22L + 25L + 28L, 300L to 17L + 23L + 26L + 29L, 600L to 18L + 24L + 27L + 30L), intervalStart),
                total = usageAmount(mapOf(0L to 4L + 16L + 22L + 25L + 28L, 300L to 5L + 17L + 23L + 26L + 29L, 600L to 6L + 18L + 24L + 27L + 30L), intervalStart)
            )
        ), serviceUsageArnoldRoot)
        val folderUsageHahnGrandchildOne = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderGrandchildOne.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(FolderUsageModel(
            key = FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderGrandchildOne.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 1,
            ownUsage = usageAmount(mapOf(0L to 19L, 300L to 20L, 600L to 21L), intervalStart)
        ), folderUsageHahnGrandchildOne)
        val folderUsageArnoldGrandchildOne = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderGrandchildOne.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(FolderUsageModel(
            key = FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderGrandchildOne.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 1,
            ownUsage = usageAmount(mapOf(0L to 22L, 300L to 23L, 600L to 24L), intervalStart)
        ), folderUsageArnoldGrandchildOne)
        val folderUsageHahnGrandchildTwo = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderGrandchildTwo.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertNull(folderUsageHahnGrandchildTwo)
        val folderUsageArnoldGrandchildTwo = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderGrandchildTwo.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(FolderUsageModel(
            key = FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderGrandchildTwo.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 1,
            ownUsage = usageAmount(mapOf(0L to 25L + 28L, 300L to 26L + 29L, 600L to 27L + 30L), intervalStart)
        ), folderUsageArnoldGrandchildTwo)
        val folderUsageHahnChildOne = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderChildOne.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(FolderUsageModel(
            key = FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderChildOne.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 1,
            ownUsage = usageAmount(mapOf(0L to 7L + 10L, 300L to 8L + 11L, 600L to 9L + 12L), intervalStart)
        ), folderUsageHahnChildOne)
        val folderUsageArnoldChildOne = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderChildOne.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertNull(folderUsageArnoldChildOne)
        val folderUsageHahnChildTwo = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderChildTwo.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(FolderUsageModel(
            key = FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderChildTwo.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 1,
            ownUsage = usageAmount(mapOf(0L to 13L, 300L to 14L, 600L to 15L), intervalStart)
        ), folderUsageHahnChildTwo)
        val folderUsageArnoldChildTwo = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderChildTwo.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(FolderUsageModel(
            key = FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderChildTwo.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 1,
            ownUsage = usageAmount(mapOf(0L to 16L, 300L to 17L, 600L to 18L), intervalStart)
        ), folderUsageArnoldChildTwo)
        val folderUsageHahnRoot = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderRoot.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(FolderUsageModel(
            key = FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderRoot.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 1,
            ownUsage = usageAmount(mapOf(0L to 1L, 300L to 2L, 600L to 3L), intervalStart)
        ), folderUsageHahnRoot)
        val folderUsageArnoldRoot = dbSessionRetryable(tableClient) { folderUsageDao
            .getById(roStaleSingleRetryableCommit(), FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderRoot.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(FolderUsageModel(
            key = FolderUsageKey(Tenants.DEFAULT_TENANT_ID, folderRoot.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 1,
            ownUsage = usageAmount(mapOf(0L to 4L, 300L to 5L, 600L to 6L), intervalStart)
        ), folderUsageArnoldRoot)
        val accountUsageHahnGrandchildFirst = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountGrandchildOneFirst.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountGrandchildOneFirst.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 1,
            ownUsage = usageAmount(mapOf(0L to 19L, 300L to 20L, 600L to 21L), intervalStart)
        ), accountUsageHahnGrandchildFirst)
        val accountUsageArnoldGrandchildOne = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountGrandchildOneSecond.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountGrandchildOneSecond.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 1,
            ownUsage = usageAmount(mapOf(0L to 22L, 300L to 23L, 600L to 24L), intervalStart)
        ), accountUsageArnoldGrandchildOne)
        val accountUsageArnoldGrandchildTwoFirst = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountGrandchildTwoFirst.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountGrandchildTwoFirst.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 1,
            ownUsage = usageAmount(mapOf(0L to 25L, 300L to 26L, 600L to 27L), intervalStart)
        ), accountUsageArnoldGrandchildTwoFirst)
        val accountUsageArnoldGrandchildTwoSecond = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountGrandchildTwoSecond.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountGrandchildTwoSecond.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 1,
            ownUsage = usageAmount(mapOf(0L to 28L, 300L to 29L, 600L to 30L), intervalStart)
        ), accountUsageArnoldGrandchildTwoSecond)
        val accountUsageHahnChildOneFirst = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountChildOneFirst.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountChildOneFirst.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 1,
            ownUsage = usageAmount(mapOf(0L to 7L, 300L to 8L, 600L to 9L), intervalStart)
        ), accountUsageHahnChildOneFirst)
        val accountUsageHahnChildOneSecond = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountChildOneSecond.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountChildOneSecond.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 1,
            ownUsage = usageAmount(mapOf(0L to 10L, 300L to 11L, 600L to 12L), intervalStart)
        ), accountUsageHahnChildOneSecond)
        val accountUsageHahnChildTwo = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountChildTwoFirst.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountChildTwoFirst.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 1,
            ownUsage = usageAmount(mapOf(0L to 13L, 300L to 14L, 600L to 15L), intervalStart)
        ), accountUsageHahnChildTwo)
        val accountUsageArnoldChildTwo = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountChildTwoSecond.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountChildTwoSecond.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 1,
            ownUsage = usageAmount(mapOf(0L to 16L, 300L to 17L, 600L to 18L), intervalStart)
        ), accountUsageArnoldChildTwo)
        val accountUsageHahnRoot = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountRootFirst.id,
                resourceCpuStrongHahn.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountRootFirst.id, resourceCpuStrongHahn.id),
            lastUpdate = now,
            epoch = 1,
            ownUsage = usageAmount(mapOf(0L to 1L, 300L to 2L, 600L to 3L), intervalStart)
        ), accountUsageHahnRoot)
        val accountUsageArnoldRoot = dbSessionRetryable(tableClient) { accountUsageDao
            .getById(roStaleSingleRetryableCommit(), AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountRootSecond.id,
                resourceCpuStrongArnold.id)) }
        Assertions.assertEquals(AccountUsageModel(
            key = AccountUsageKey(Tenants.DEFAULT_TENANT_ID, accountRootSecond.id, resourceCpuStrongArnold.id),
            lastUpdate = now,
            epoch = 1,
            ownUsage = usageAmount(mapOf(0L to 4L, 300L to 5L, 600L to 6L), intervalStart)
        ), accountUsageArnoldRoot)
    }

    @Test
    fun testSyncEndpoint(): Unit = runBlocking {
        val root = serviceModel(65535, null)
        val childOne = serviceModel(65536, root.id)
        val childTwo = serviceModel(65537, root.id)
        val grandchildOne = serviceModel(65538, childOne.id)
        val grandchildTwo = serviceModel(65539, childTwo.id)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne, childTwo, grandchildOne, grandchildTwo))
                    .awaitSingleOrNull()
            }
        }
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        val folderChildTwo = folderModel(childTwo.id)
        val folderGrandchildOne = folderModel(grandchildOne.id)
        val folderGrandchildTwo = folderModel(grandchildTwo.id)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne, folderChildTwo,
                    folderGrandchildOne, folderGrandchildTwo)).awaitSingleOrNull()
            }
        }
        val providerYt = providerModel("yt")
        val providerYp = providerModel("yp")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                providersDao.upsertProvidersRetryable(txSession,
                    listOf(providerYt, providerYp)).awaitSingleOrNull()
            }
        }
        val resourceTypeCpuStrong = resourceTypeModel(providerYt.id, "cpu_strong", UnitsEnsembleIds.CPU_UNITS_ID, UnitIds.MILLICORES)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceTypeCpuStrong)
                    .awaitSingleOrNull()
            }
        }
        val segmentationCluster = resourceSegmentationModel(providerYt.id, "cluster")
        val segmentationScope = resourceSegmentationModel(providerYt.id, "scope")
        val segmentationPoolTree = resourceSegmentationModel(providerYt.id, "pool_tree")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceSegmentationsDao.upsertResourceSegmentationsRetryable(txSession,
                    listOf(segmentationCluster, segmentationScope, segmentationPoolTree)).awaitSingleOrNull()
            }
        }
        val segmentHahn = resourceSegmentModel(segmentationCluster.id, "hahn")
        val segmentArnold = resourceSegmentModel(segmentationCluster.id, "arnold")
        val segmentCompute = resourceSegmentModel(segmentationScope.id, "compute")
        val segmentPhysical = resourceSegmentModel(segmentationPoolTree.id, "physical")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession,
                    listOf(segmentHahn, segmentArnold, segmentCompute, segmentPhysical)).awaitSingleOrNull()
            }
        }
        val accountSpaceHahn = accountsSpaceModel(providerYt.id, "hahn_compute_physical",
            mapOf(segmentationCluster.id to segmentHahn.id, segmentationScope.id to segmentCompute.id, segmentationPoolTree.id to segmentPhysical.id))
        val accountSpaceArnold = accountsSpaceModel(providerYt.id, "arnold_compute_physical",
            mapOf(segmentationCluster.id to segmentArnold.id, segmentationScope.id to segmentCompute.id, segmentationPoolTree.id to segmentPhysical.id))
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsSpacesDao.upsertAllRetryable(txSession,
                    listOf(accountSpaceHahn, accountSpaceArnold)).awaitSingleOrNull()
            }
        }
        val resourceCpuStrongHahn = resourceModel(providerYt.id, "hahn_compute_physical_cpu_strong", resourceTypeCpuStrong.id,
            mapOf(segmentationCluster.id to segmentHahn.id, segmentationScope.id to segmentCompute.id, segmentationPoolTree.id to segmentPhysical.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES, accountSpaceHahn.id)
        val resourceCpuStrongArnold = resourceModel(providerYt.id, "arnold_compute_physical_cpu_strong", resourceTypeCpuStrong.id,
            mapOf(segmentationCluster.id to segmentArnold.id, segmentationScope.id to segmentCompute.id, segmentationPoolTree.id to segmentPhysical.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES, accountSpaceArnold.id)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourcesDao.upsertResourcesRetryable(txSession,
                    listOf(resourceCpuStrongHahn, resourceCpuStrongArnold)).awaitSingleOrNull()
            }
        }
        val accountRootFirst = accountModel(providerYt.id, accountSpaceHahn.id, folderRoot.id, "test", "one")
        val accountRootSecond = accountModel(providerYt.id, accountSpaceArnold.id, folderRoot.id, "test", "two")
        val accountChildOneFirst = accountModel(providerYt.id, accountSpaceHahn.id, folderChildOne.id, "test", "three")
        val accountChildOneSecond = accountModel(providerYt.id, accountSpaceHahn.id, folderChildOne.id, "test", "four")
        val accountChildTwoFirst = accountModel(providerYt.id, accountSpaceHahn.id, folderChildTwo.id, "test", "five")
        val accountChildTwoSecond = accountModel(providerYt.id, accountSpaceArnold.id, folderChildTwo.id, "test", "six")
        val accountGrandchildOneFirst = accountModel(providerYt.id, accountSpaceHahn.id, folderGrandchildOne.id, "test", "seven")
        val accountGrandchildOneSecond = accountModel(providerYt.id, accountSpaceArnold.id, folderGrandchildOne.id, "test", "eight")
        val accountGrandchildTwoFirst = accountModel(providerYt.id, accountSpaceArnold.id, folderGrandchildTwo.id, "test", "nine")
        val accountGrandchildTwoSecond = accountModel(providerYt.id, accountSpaceArnold.id, folderGrandchildTwo.id, "test", "ten")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsDao.upsertAllRetryable(txSession, listOf(accountRootFirst, accountRootSecond, accountChildOneFirst,
                    accountChildOneSecond, accountChildTwoFirst, accountChildTwoSecond, accountGrandchildOneFirst,
                    accountGrandchildOneSecond, accountGrandchildTwoFirst, accountGrandchildTwoSecond)).awaitSingleOrNull()
            }
        }
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
        val intervalStart = now.atZone(ZoneOffset.UTC).toLocalDate().minusDays(7)
            .atStartOfDay(ZoneOffset.UTC).toInstant()
        val solomonResponse = prepareSolomonResponse(intervalStart)
        solomonClient.setStubData(solomonResponse)
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/admin/settings/knownProviders")
            .bodyValue(KnownProvidersDto(providerYt.id, providerYp.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/admin/settings/ytUsageSync")
            .bodyValue(YtUsageSyncSettingsDto(resourceTypeCpuStrong.id, segmentationPoolTree.id, segmentPhysical.id,
                segmentationCluster.id, segmentationScope.id, segmentCompute.id, true, 300L,
                UnitsEnsembleIds.CPU_UNITS_ID, UnitIds.CORES))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/maintenance/_syncYtUsage")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
    }

    private fun usageAmount(timeSeries: Map<Long, Long>, intervalStart: Instant): UsageAmount {
        val series = mutableMapOf<Long, BigInteger>()
        timeSeries.forEach { (k, v) ->
            series[k + intervalStart.epochSecond] = BigInteger.valueOf(v * 1000)
        }
        val average = mean(series.values)
        val minMedianMax = minMedianMax(series.values)
        val variance = variance(series.values, average)
        val accumulated = accumulate(series, 300)
        val histogram = histogram(series.values, minMedianMax.first, minMedianMax.third)
        val sortedEntries = series.entries.sortedBy { it.key }
        val valuesX = sortedEntries.map { it.key }
        val valuesY = sortedEntries.map { it.value }
        return UsageAmount(
            value = null,
            average = roundToIntegerHalfUp(average),
            min = minMedianMax.first,
            max = minMedianMax.third,
            median = roundToIntegerHalfUp(minMedianMax.second),
            variance = roundToIntegerHalfUp(variance),
            accumulated = roundToIntegerHalfUp(accumulated.first),
            accumulatedDuration = accumulated.second,
            histogram = histogram,
            values = null,
            valuesX = valuesX,
            valuesY = valuesY,
            unused = null
        )
    }

    private fun prepareSolomonResponse(intervalStart: Instant): Map<String, Timeseries> {
        return mutableMapOf(
            "one" to Timeseries.newBuilder()
                .setDoubleValues(DoubleValues.newBuilder()
                    .addAllValues(listOf(1.0, 2.0, 3.0))
                    .build())
                .addAllTimestamps(listOf(Timestamps.fromMillis(intervalStart.toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(300).toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(600).toEpochMilli())))
                .putLabels("pool", "one")
                .build(),
            "two" to Timeseries.newBuilder()
                .setDoubleValues(DoubleValues.newBuilder()
                    .addAllValues(listOf(4.0, 5.0, 6.0))
                    .build())
                .addAllTimestamps(listOf(Timestamps.fromMillis(intervalStart.toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(300).toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(600).toEpochMilli())))
                .putLabels("pool", "two")
                .build(),
            "three" to Timeseries.newBuilder()
                .setDoubleValues(DoubleValues.newBuilder()
                    .addAllValues(listOf(7.0, 8.0, 9.0))
                    .build())
                .addAllTimestamps(listOf(Timestamps.fromMillis(intervalStart.toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(300).toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(600).toEpochMilli())))
                .putLabels("pool", "three")
                .build(),
            "four" to Timeseries.newBuilder()
                .setDoubleValues(DoubleValues.newBuilder()
                    .addAllValues(listOf(10.0, 11.0, 12.0))
                    .build())
                .addAllTimestamps(listOf(Timestamps.fromMillis(intervalStart.toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(300).toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(600).toEpochMilli())))
                .putLabels("pool", "four")
                .build(),
            "five" to Timeseries.newBuilder()
                .setDoubleValues(DoubleValues.newBuilder()
                    .addAllValues(listOf(13.0, 14.0, 15.0))
                    .build())
                .addAllTimestamps(listOf(Timestamps.fromMillis(intervalStart.toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(300).toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(600).toEpochMilli())))
                .putLabels("pool", "five")
                .build(),
            "six" to Timeseries.newBuilder()
                .setDoubleValues(DoubleValues.newBuilder()
                    .addAllValues(listOf(16.0, 17.0, 18.0))
                    .build())
                .addAllTimestamps(listOf(Timestamps.fromMillis(intervalStart.toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(300).toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(600).toEpochMilli())))
                .putLabels("pool", "six")
                .build(),
            "seven" to Timeseries.newBuilder()
                .setDoubleValues(DoubleValues.newBuilder()
                    .addAllValues(listOf(19.0, 20.0, 21.0))
                    .build())
                .addAllTimestamps(listOf(Timestamps.fromMillis(intervalStart.toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(300).toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(600).toEpochMilli())))
                .putLabels("pool", "seven")
                .build(),
            "eight" to Timeseries.newBuilder()
                .setDoubleValues(DoubleValues.newBuilder()
                    .addAllValues(listOf(22.0, 23.0, 24.0))
                    .build())
                .addAllTimestamps(listOf(Timestamps.fromMillis(intervalStart.toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(300).toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(600).toEpochMilli())))
                .putLabels("pool", "eight")
                .build(),
            "nine" to Timeseries.newBuilder()
                .setDoubleValues(DoubleValues.newBuilder()
                    .addAllValues(listOf(25.0, 26.0, 27.0))
                    .build())
                .addAllTimestamps(listOf(Timestamps.fromMillis(intervalStart.toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(300).toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(600).toEpochMilli())))
                .putLabels("pool", "nine")
                .build(),
            "ten" to Timeseries.newBuilder()
                .setDoubleValues(DoubleValues.newBuilder()
                    .addAllValues(listOf(28.0, 29.0, 30.0))
                    .build())
                .addAllTimestamps(listOf(Timestamps.fromMillis(intervalStart.toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(300).toEpochMilli()),
                    Timestamps.fromMillis(intervalStart.plusSeconds(600).toEpochMilli())))
                .putLabels("pool", "ten")
                .build()
        )
    }

    private fun providerModel(key: String): ProviderModel {
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
            .key(key)
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
            .accountsSpacesSupported(true)
            .syncEnabled(true)
            .grpcTlsOn(true)
            .aggregationSettings(AggregationSettings(FreeProvisionAggregationMode.UNDERUTILIZED_TRANSFERABLE,
                UsageMode.TIME_SERIES, 300))
            .aggregationAlgorithm(null)
            .build()
    }

    private fun resourceTypeModel(providerId: String, key: String, unitsEnsembleId: String,
                                  baseUnitId: String): ResourceTypeModel {
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
            .baseUnitId(baseUnitId)
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

    private fun accountModel(providerId: String, accountsSpaceId: String?, folderId: String,
                             displayName: String, externalKey: String): AccountModel {
        return AccountModel.Builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setId(UUID.randomUUID().toString())
            .setVersion(0L)
            .setProviderId(providerId)
            .setAccountsSpacesId(accountsSpaceId)
            .setOuterAccountIdInProvider(UUID.randomUUID().toString())
            .setOuterAccountKeyInProvider(externalKey)
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

    fun resourceSegmentationModel(providerId: String, key: String): ResourceSegmentationModel {
        return ResourceSegmentationModel.builder()
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
            .build()
    }

    fun resourceSegmentModel(segmentationId: String, key: String): ResourceSegmentModel {
        return ResourceSegmentModel.builder()
            .id(UUID.randomUUID().toString())
            .tenantId(Tenants.DEFAULT_TENANT_ID)
            .segmentationId(segmentationId)
            .version(0L)
            .key(key)
            .nameEn("Test")
            .nameRu("Test")
            .descriptionEn("Test")
            .descriptionRu("Test")
            .deleted(false)
            .build()
    }

    fun accountsSpaceModel(providerId: String, key: String, segments: Map<String, String>): AccountSpaceModel {
        return AccountSpaceModel.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setDeleted(false)
            .setNameEn("Test")
            .setNameRu("Test")
            .setDescriptionEn("Test")
            .setDescriptionRu("Test")
            .setProviderId(providerId)
            .setOuterKeyInProvider(key)
            .setSegments(segments.map { (k ,v) -> ResourceSegmentSettingsModel(k, v) }.toSet())
            .setVersion(0L)
            .setReadOnly(false)
            .build()
    }

}
