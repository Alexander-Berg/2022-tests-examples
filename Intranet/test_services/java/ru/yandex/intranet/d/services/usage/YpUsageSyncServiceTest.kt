package ru.yandex.intranet.d.services.usage

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
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
import ru.yandex.intranet.d.model.usage.ServiceUsageAmounts
import ru.yandex.intranet.d.model.usage.ServiceUsageKey
import ru.yandex.intranet.d.model.usage.ServiceUsageModel
import ru.yandex.intranet.d.model.usage.UsageAmount
import ru.yandex.intranet.d.services.integration.yt.YtReader
import ru.yandex.intranet.d.services.integration.yt.YtReaderStub
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.settings.KnownProvidersDto
import ru.yandex.intranet.d.web.model.settings.YpUsageSyncSettingsDto
import java.math.BigInteger
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * YP usage sync service test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class YpUsageSyncServiceTest(
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
    @Autowired @Qualifier("ytReaderHahn") private val ytReader: YtReader,
    @Autowired private val ypUsageSyncService: YpUsageSyncService
) {

    @Test
    fun testSync(): Unit = runBlocking {
        val root = serviceModel(65535, null, "root")
        val childOne = serviceModel(65536, root.id, "child-one")
        val childTwo = serviceModel(65537, root.id, "child-two")
        val grandchildOne = serviceModel(65538, childOne.id, "grandchild-one")
        val grandchildTwo = serviceModel(65539, childTwo.id, "grandchild-two")
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
        val resourceTypeCpu = resourceTypeModel(providerYp.id, "cpu_capacity", UnitsEnsembleIds.CPU_UNITS_ID, UnitIds.MILLICORES)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceTypeCpu)
            .awaitSingleOrNull() }}
        val segmentationCluster = resourceSegmentationModel(providerYp.id, "cluster")
        val segmentationNodeSegment = resourceSegmentationModel(providerYp.id, "node_segment")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentationsDao.upsertResourceSegmentationsRetryable(txSession,
            listOf(segmentationCluster, segmentationNodeSegment)).awaitSingleOrNull() }}
        val segmentSas = resourceSegmentModel(segmentationCluster.id, "sas")
        val segmentMan = resourceSegmentModel(segmentationCluster.id, "man")
        val segmentVla = resourceSegmentModel(segmentationCluster.id, "vla")
        val segmentIva = resourceSegmentModel(segmentationCluster.id, "iva")
        val segmentMyt = resourceSegmentModel(segmentationCluster.id, "myt")
        val segmentDev = resourceSegmentModel(segmentationNodeSegment.id, "dev")
        val segmentDefault = resourceSegmentModel(segmentationNodeSegment.id, "default")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession,
            listOf(segmentSas, segmentVla, segmentMan, segmentIva, segmentMyt, segmentDev, segmentDefault)).awaitSingleOrNull() }}
        val resourceCpuSasDev = resourceModel(providerYp.id, "yp:sas:dev:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentSas.id, segmentationNodeSegment.id to segmentDev.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuSasDefault = resourceModel(providerYp.id, "yp:sas:default:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentSas.id, segmentationNodeSegment.id to segmentDefault.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuManDev = resourceModel(providerYp.id, "yp:man:dev:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentMan.id, segmentationNodeSegment.id to segmentDev.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuManDefault = resourceModel(providerYp.id, "yp:man:default:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentMan.id, segmentationNodeSegment.id to segmentDefault.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuVlaDev = resourceModel(providerYp.id, "yp:vla:dev:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentVla.id, segmentationNodeSegment.id to segmentDev.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuVlaDefault = resourceModel(providerYp.id, "yp:vla:default:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentVla.id, segmentationNodeSegment.id to segmentDefault.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuIvaDev = resourceModel(providerYp.id, "yp:iva:dev:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentIva.id, segmentationNodeSegment.id to segmentDev.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuIvaDefault = resourceModel(providerYp.id, "yp:iva:default:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentIva.id, segmentationNodeSegment.id to segmentDefault.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuMytDev = resourceModel(providerYp.id, "yp:myt:dev:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentMyt.id, segmentationNodeSegment.id to segmentDev.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuMytDefault = resourceModel(providerYp.id, "yp:myt:default:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentMyt.id, segmentationNodeSegment.id to segmentDefault.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourcesRetryable(txSession,
            listOf(resourceCpuSasDev, resourceCpuSasDefault, resourceCpuManDev, resourceCpuManDefault,
                resourceCpuVlaDev, resourceCpuVlaDefault, resourceCpuIvaDev, resourceCpuIvaDefault,
                resourceCpuMytDev, resourceCpuMytDefault)).awaitSingleOrNull() }}
        val accountRootFirst = accountModel(providerYp.id, folderRoot.id, "test", "one")
        val accountRootSecond = accountModel(providerYp.id, folderRoot.id, "test", "two")
        val accountChildOneFirst = accountModel(providerYp.id, folderChildOne.id, "test", "three")
        val accountChildOneSecond = accountModel(providerYp.id, folderChildOne.id, "test", "four")
        val accountChildTwoFirst = accountModel(providerYp.id, folderChildTwo.id, "test", "five")
        val accountChildTwoSecond = accountModel(providerYp.id, folderChildTwo.id, "test", "six")
        val accountGrandchildOneFirst = accountModel(providerYp.id, folderGrandchildOne.id, "test", "seven")
        val accountGrandchildOneSecond = accountModel(providerYp.id, folderGrandchildOne.id, "test", "eight")
        val accountGrandchildTwoFirst = accountModel(providerYp.id, folderGrandchildTwo.id, "test", "nine")
        val accountGrandchildTwoSecond = accountModel(providerYp.id, folderGrandchildTwo.id, "test", "ten")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRootFirst, accountRootSecond, accountChildOneFirst,
                accountChildOneSecond, accountChildTwoFirst, accountChildTwoSecond, accountGrandchildOneFirst,
                accountGrandchildOneSecond, accountGrandchildTwoFirst, accountGrandchildTwoSecond)).awaitSingleOrNull()
        }}
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
        (ytReader as YtReaderStub).setRows(prepareTableRows())
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
            .uri("/admin/settings/ypUsageSync")
            .bodyValue(YpUsageSyncSettingsDto(resourceTypeCpu.id, null, segmentationCluster.id, segmentationNodeSegment.id,
                null, null, segmentDefault.id, segmentDev.id, null, null,
                UnitsEnsembleIds.CPU_UNITS_ID, UnitIds.CORES, true))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
        ypUsageSyncService.doSync(Clock.fixed(now, ZoneOffset.UTC))
        val serviceUsageCpuSasDevGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuSasDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuSasDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuSasDevGrandchildOne)
        val serviceUsageCpuSasDefaultGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuSasDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuSasDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuSasDefaultGrandchildOne)
        val serviceUsageCpuManDevGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuManDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuManDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuManDevGrandchildOne)
        val serviceUsageCpuManDefaultGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuManDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuManDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuManDefaultGrandchildOne)
        val serviceUsageCpuVlaDevGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuVlaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuVlaDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuVlaDevGrandchildOne)
        val serviceUsageCpuVlaDefaultGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuVlaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuVlaDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuVlaDefaultGrandchildOne)
        val serviceUsageCpuIvaDevGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuIvaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuIvaDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuIvaDevGrandchildOne)
        val serviceUsageCpuIvaDefaultGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuIvaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuIvaDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuIvaDefaultGrandchildOne)
        val serviceUsageCpuMytDevGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuMytDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuMytDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuMytDevGrandchildOne)
        val serviceUsageCpuMytDefaultGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuMytDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuMytDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuMytDefaultGrandchildOne)
        val serviceUsageSasDevGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuSasDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuSasDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageSasDevGrandchildTwo)
        val serviceUsageSasDefaultGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuSasDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuSasDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageSasDefaultGrandchildTwo)
        val serviceUsageManDevGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuManDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuManDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageManDevGrandchildTwo)
        val serviceUsageManDefaultGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuManDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuManDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageManDefaultGrandchildTwo)
        val serviceUsageVlaDevGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuVlaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuVlaDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageVlaDevGrandchildTwo)
        val serviceUsageVlaDefaultGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuVlaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuVlaDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageVlaDefaultGrandchildTwo)
        val serviceUsageIvaDevGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuIvaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuIvaDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageIvaDevGrandchildTwo)
        val serviceUsageIvaDefaultGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuIvaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuIvaDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageIvaDefaultGrandchildTwo)
        val serviceUsageMytDevGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuMytDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuMytDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageMytDevGrandchildTwo)
        val serviceUsageMytDefaultGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuMytDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuMytDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageMytDefaultGrandchildTwo)
        val serviceUsageCpuSasDevChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuSasDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuSasDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuSasDevChildOne)
        val serviceUsageCpuSasDefaultChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuSasDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuSasDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuSasDefaultChildOne)
        val serviceUsageCpuManDevChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuManDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuManDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuManDevChildOne)
        val serviceUsageCpuManDefaultChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuManDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuManDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuManDefaultChildOne)
        val serviceUsageCpuVlaDevChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuVlaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuVlaDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuVlaDevChildOne)
        val serviceUsageCpuVlaDefaultChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuVlaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuVlaDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuVlaDefaultChildOne)
        val serviceUsageCpuIvaDevChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuIvaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuIvaDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuIvaDevChildOne)
        val serviceUsageCpuIvaDefaultChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuIvaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuIvaDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuIvaDefaultChildOne)
        val serviceUsageCpuMytDevChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuMytDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuMytDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuMytDevChildOne)
        val serviceUsageCpuMytDefaultChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuMytDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuMytDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuMytDefaultChildOne)
        val serviceUsageCpuSasDevChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuSasDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuSasDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuSasDevChildTwo)
        val serviceUsageCpuSasDefaultChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuSasDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuSasDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuSasDefaultChildTwo)
        val serviceUsageCpuManDevChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuManDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuManDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuManDevChildTwo)
        val serviceUsageCpuManDefaultChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuManDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuManDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuManDefaultChildTwo)
        val serviceUsageCpuVlaDevChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuVlaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuVlaDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuVlaDevChildTwo)
        val serviceUsageCpuVlaDefaultChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuVlaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuVlaDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuVlaDefaultChildTwo)
        val serviceUsageCpuIvaDevChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuIvaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuIvaDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuIvaDevChildTwo)
        val serviceUsageCpuIvaDefaultChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuIvaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuIvaDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuIvaDefaultChildTwo)
        val serviceUsageCpuMytDevChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuMytDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuMytDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuMytDevChildTwo)
        val serviceUsageCpuMytDefaultChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuMytDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuMytDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuMytDefaultChildTwo)
        val serviceUsageSasDevRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuSasDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuSasDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageSasDevRoot)
        val serviceUsageSasDefaultRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuSasDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuSasDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageSasDefaultRoot)
        val serviceUsageManDevRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuManDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuManDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageManDevRoot)
        val serviceUsageManDefaultRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuManDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuManDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageManDefaultRoot)
        val serviceUsageVlaDevRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuVlaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuVlaDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageVlaDevRoot)
        val serviceUsageVlaDefaultRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuVlaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuVlaDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageVlaDefaultRoot)
        val serviceUsageIvaDevRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuIvaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuIvaDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageIvaDevRoot)
        val serviceUsageIvaDefaultRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuIvaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuIvaDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageIvaDefaultRoot)
        val serviceUsageMytDevRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuMytDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuMytDev.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageMytDevRoot)
        val serviceUsageMytDefaultRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuMytDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuMytDefault.id),
            lastUpdate = now,
            epoch = 0,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageMytDefaultRoot)
    }

    @Test
    fun testSyncAgain(): Unit = runBlocking {
        val root = serviceModel(65535, null, "root")
        val childOne = serviceModel(65536, root.id, "child-one")
        val childTwo = serviceModel(65537, root.id, "child-two")
        val grandchildOne = serviceModel(65538, childOne.id, "grandchild-one")
        val grandchildTwo = serviceModel(65539, childTwo.id, "grandchild-two")
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
        val resourceTypeCpu = resourceTypeModel(providerYp.id, "cpu_capacity", UnitsEnsembleIds.CPU_UNITS_ID, UnitIds.MILLICORES)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceTypeCpu)
            .awaitSingleOrNull() }}
        val segmentationCluster = resourceSegmentationModel(providerYp.id, "cluster")
        val segmentationNodeSegment = resourceSegmentationModel(providerYp.id, "node_segment")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentationsDao.upsertResourceSegmentationsRetryable(txSession,
            listOf(segmentationCluster, segmentationNodeSegment)).awaitSingleOrNull() }}
        val segmentSas = resourceSegmentModel(segmentationCluster.id, "sas")
        val segmentMan = resourceSegmentModel(segmentationCluster.id, "man")
        val segmentVla = resourceSegmentModel(segmentationCluster.id, "vla")
        val segmentIva = resourceSegmentModel(segmentationCluster.id, "iva")
        val segmentMyt = resourceSegmentModel(segmentationCluster.id, "myt")
        val segmentDev = resourceSegmentModel(segmentationNodeSegment.id, "dev")
        val segmentDefault = resourceSegmentModel(segmentationNodeSegment.id, "default")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession,
            listOf(segmentSas, segmentVla, segmentMan, segmentIva, segmentMyt, segmentDev, segmentDefault)).awaitSingleOrNull() }}
        val resourceCpuSasDev = resourceModel(providerYp.id, "yp:sas:dev:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentSas.id, segmentationNodeSegment.id to segmentDev.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuSasDefault = resourceModel(providerYp.id, "yp:sas:default:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentSas.id, segmentationNodeSegment.id to segmentDefault.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuManDev = resourceModel(providerYp.id, "yp:man:dev:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentMan.id, segmentationNodeSegment.id to segmentDev.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuManDefault = resourceModel(providerYp.id, "yp:man:default:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentMan.id, segmentationNodeSegment.id to segmentDefault.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuVlaDev = resourceModel(providerYp.id, "yp:vla:dev:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentVla.id, segmentationNodeSegment.id to segmentDev.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuVlaDefault = resourceModel(providerYp.id, "yp:vla:default:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentVla.id, segmentationNodeSegment.id to segmentDefault.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuIvaDev = resourceModel(providerYp.id, "yp:iva:dev:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentIva.id, segmentationNodeSegment.id to segmentDev.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuIvaDefault = resourceModel(providerYp.id, "yp:iva:default:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentIva.id, segmentationNodeSegment.id to segmentDefault.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuMytDev = resourceModel(providerYp.id, "yp:myt:dev:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentMyt.id, segmentationNodeSegment.id to segmentDev.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuMytDefault = resourceModel(providerYp.id, "yp:myt:default:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentMyt.id, segmentationNodeSegment.id to segmentDefault.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourcesRetryable(txSession,
            listOf(resourceCpuSasDev, resourceCpuSasDefault, resourceCpuManDev, resourceCpuManDefault,
                resourceCpuVlaDev, resourceCpuVlaDefault, resourceCpuIvaDev, resourceCpuIvaDefault,
                resourceCpuMytDev, resourceCpuMytDefault)).awaitSingleOrNull() }}
        val accountRootFirst = accountModel(providerYp.id, folderRoot.id, "test", "one")
        val accountRootSecond = accountModel(providerYp.id, folderRoot.id, "test", "two")
        val accountChildOneFirst = accountModel(providerYp.id, folderChildOne.id, "test", "three")
        val accountChildOneSecond = accountModel(providerYp.id, folderChildOne.id, "test", "four")
        val accountChildTwoFirst = accountModel(providerYp.id, folderChildTwo.id, "test", "five")
        val accountChildTwoSecond = accountModel(providerYp.id, folderChildTwo.id, "test", "six")
        val accountGrandchildOneFirst = accountModel(providerYp.id, folderGrandchildOne.id, "test", "seven")
        val accountGrandchildOneSecond = accountModel(providerYp.id, folderGrandchildOne.id, "test", "eight")
        val accountGrandchildTwoFirst = accountModel(providerYp.id, folderGrandchildTwo.id, "test", "nine")
        val accountGrandchildTwoSecond = accountModel(providerYp.id, folderGrandchildTwo.id, "test", "ten")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRootFirst, accountRootSecond, accountChildOneFirst,
                accountChildOneSecond, accountChildTwoFirst, accountChildTwoSecond, accountGrandchildOneFirst,
                accountGrandchildOneSecond, accountGrandchildTwoFirst, accountGrandchildTwoSecond)).awaitSingleOrNull()
        }}
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
        (ytReader as YtReaderStub).setRows(prepareTableRows())
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
            .uri("/admin/settings/ypUsageSync")
            .bodyValue(YpUsageSyncSettingsDto(resourceTypeCpu.id, null, segmentationCluster.id, segmentationNodeSegment.id,
                null, null, segmentDefault.id, segmentDev.id, null, null,
                UnitsEnsembleIds.CPU_UNITS_ID, UnitIds.CORES, true))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
        ypUsageSyncService.doSync(Clock.fixed(now, ZoneOffset.UTC))
        ypUsageSyncService.doSync(Clock.fixed(now, ZoneOffset.UTC))
        val serviceUsageCpuSasDevGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuSasDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuSasDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuSasDevGrandchildOne)
        val serviceUsageCpuSasDefaultGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuSasDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuSasDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuSasDefaultGrandchildOne)
        val serviceUsageCpuManDevGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuManDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuManDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuManDevGrandchildOne)
        val serviceUsageCpuManDefaultGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuManDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuManDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuManDefaultGrandchildOne)
        val serviceUsageCpuVlaDevGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuVlaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuVlaDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuVlaDevGrandchildOne)
        val serviceUsageCpuVlaDefaultGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuVlaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuVlaDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuVlaDefaultGrandchildOne)
        val serviceUsageCpuIvaDevGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuIvaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuIvaDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuIvaDevGrandchildOne)
        val serviceUsageCpuIvaDefaultGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuIvaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuIvaDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuIvaDefaultGrandchildOne)
        val serviceUsageCpuMytDevGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuMytDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuMytDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuMytDevGrandchildOne)
        val serviceUsageCpuMytDefaultGrandchildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id,
                providerYp.id, resourceCpuMytDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildOne.id, providerYp.id, resourceCpuMytDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageCpuMytDefaultGrandchildOne)
        val serviceUsageSasDevGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuSasDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuSasDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageSasDevGrandchildTwo)
        val serviceUsageSasDefaultGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuSasDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuSasDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageSasDefaultGrandchildTwo)
        val serviceUsageManDevGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuManDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuManDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageManDevGrandchildTwo)
        val serviceUsageManDefaultGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuManDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuManDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageManDefaultGrandchildTwo)
        val serviceUsageVlaDevGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuVlaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuVlaDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageVlaDevGrandchildTwo)
        val serviceUsageVlaDefaultGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuVlaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuVlaDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageVlaDefaultGrandchildTwo)
        val serviceUsageIvaDevGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuIvaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuIvaDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageIvaDevGrandchildTwo)
        val serviceUsageIvaDefaultGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuIvaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuIvaDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageIvaDefaultGrandchildTwo)
        val serviceUsageMytDevGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuMytDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuMytDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageMytDevGrandchildTwo)
        val serviceUsageMytDefaultGrandchildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id,
                providerYp.id, resourceCpuMytDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, grandchildTwo.id, providerYp.id, resourceCpuMytDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = null,
                total = null
            )
        ), serviceUsageMytDefaultGrandchildTwo)
        val serviceUsageCpuSasDevChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuSasDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuSasDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuSasDevChildOne)
        val serviceUsageCpuSasDefaultChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuSasDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuSasDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuSasDefaultChildOne)
        val serviceUsageCpuManDevChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuManDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuManDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuManDevChildOne)
        val serviceUsageCpuManDefaultChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuManDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuManDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuManDefaultChildOne)
        val serviceUsageCpuVlaDevChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuVlaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuVlaDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuVlaDevChildOne)
        val serviceUsageCpuVlaDefaultChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuVlaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuVlaDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuVlaDefaultChildOne)
        val serviceUsageCpuIvaDevChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuIvaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuIvaDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuIvaDevChildOne)
        val serviceUsageCpuIvaDefaultChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuIvaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuIvaDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuIvaDefaultChildOne)
        val serviceUsageCpuMytDevChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuMytDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuMytDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuMytDevChildOne)
        val serviceUsageCpuMytDefaultChildOne = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id,
                providerYp.id, resourceCpuMytDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childOne.id, providerYp.id, resourceCpuMytDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuMytDefaultChildOne)
        val serviceUsageCpuSasDevChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuSasDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuSasDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuSasDevChildTwo)
        val serviceUsageCpuSasDefaultChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuSasDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuSasDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuSasDefaultChildTwo)
        val serviceUsageCpuManDevChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuManDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuManDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuManDevChildTwo)
        val serviceUsageCpuManDefaultChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuManDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuManDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuManDefaultChildTwo)
        val serviceUsageCpuVlaDevChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuVlaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuVlaDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuVlaDevChildTwo)
        val serviceUsageCpuVlaDefaultChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuVlaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuVlaDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuVlaDefaultChildTwo)
        val serviceUsageCpuIvaDevChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuIvaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuIvaDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuIvaDevChildTwo)
        val serviceUsageCpuIvaDefaultChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuIvaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuIvaDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuIvaDefaultChildTwo)
        val serviceUsageCpuMytDevChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuMytDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuMytDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuMytDevChildTwo)
        val serviceUsageCpuMytDefaultChildTwo = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id,
                providerYp.id, resourceCpuMytDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, childTwo.id, providerYp.id, resourceCpuMytDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(2L),
                total = usageAmount(4L)
            )
        ), serviceUsageCpuMytDefaultChildTwo)
        val serviceUsageSasDevRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuSasDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuSasDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageSasDevRoot)
        val serviceUsageSasDefaultRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuSasDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuSasDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageSasDefaultRoot)
        val serviceUsageManDevRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuManDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuManDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageManDevRoot)
        val serviceUsageManDefaultRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuManDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuManDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageManDefaultRoot)
        val serviceUsageVlaDevRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuVlaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuVlaDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageVlaDevRoot)
        val serviceUsageVlaDefaultRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuVlaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuVlaDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageVlaDefaultRoot)
        val serviceUsageIvaDevRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuIvaDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuIvaDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageIvaDevRoot)
        val serviceUsageIvaDefaultRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuIvaDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuIvaDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageIvaDefaultRoot)
        val serviceUsageMytDevRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuMytDev.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuMytDev.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageMytDevRoot)
        val serviceUsageMytDefaultRoot = dbSessionRetryable(tableClient) { serviceUsageDao
            .getById(roStaleSingleRetryableCommit(), ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id,
                providerYp.id, resourceCpuMytDefault.id)) }
        Assertions.assertEquals(ServiceUsageModel(
            key = ServiceUsageKey(Tenants.DEFAULT_TENANT_ID, root.id, providerYp.id, resourceCpuMytDefault.id),
            lastUpdate = now,
            epoch = 1,
            usage = ServiceUsageAmounts(
                own = usageAmount(2L),
                subtree = usageAmount(8L),
                total = usageAmount(10L)
            )
        ), serviceUsageMytDefaultRoot)
    }

    @Test
    fun testSyncEndpoint(): Unit = runBlocking {
        val root = serviceModel(65535, null, "root")
        val childOne = serviceModel(65536, root.id, "child-one")
        val childTwo = serviceModel(65537, root.id, "child-two")
        val grandchildOne = serviceModel(65538, childOne.id, "grandchild-one")
        val grandchildTwo = serviceModel(65539, childTwo.id, "grandchild-two")
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
        val resourceTypeCpu = resourceTypeModel(providerYp.id, "cpu_capacity", UnitsEnsembleIds.CPU_UNITS_ID, UnitIds.MILLICORES)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceTypeCpu)
            .awaitSingleOrNull() }}
        val segmentationCluster = resourceSegmentationModel(providerYp.id, "cluster")
        val segmentationNodeSegment = resourceSegmentationModel(providerYp.id, "node_segment")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentationsDao.upsertResourceSegmentationsRetryable(txSession,
            listOf(segmentationCluster, segmentationNodeSegment)).awaitSingleOrNull() }}
        val segmentSas = resourceSegmentModel(segmentationCluster.id, "sas")
        val segmentMan = resourceSegmentModel(segmentationCluster.id, "man")
        val segmentVla = resourceSegmentModel(segmentationCluster.id, "vla")
        val segmentIva = resourceSegmentModel(segmentationCluster.id, "iva")
        val segmentMyt = resourceSegmentModel(segmentationCluster.id, "myt")
        val segmentDev = resourceSegmentModel(segmentationNodeSegment.id, "dev")
        val segmentDefault = resourceSegmentModel(segmentationNodeSegment.id, "default")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession,
            listOf(segmentSas, segmentVla, segmentMan, segmentIva, segmentMyt, segmentDev, segmentDefault)).awaitSingleOrNull() }}
        val resourceCpuSasDev = resourceModel(providerYp.id, "yp:sas:dev:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentSas.id, segmentationNodeSegment.id to segmentDev.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuSasDefault = resourceModel(providerYp.id, "yp:sas:default:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentSas.id, segmentationNodeSegment.id to segmentDefault.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuManDev = resourceModel(providerYp.id, "yp:man:dev:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentMan.id, segmentationNodeSegment.id to segmentDev.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuManDefault = resourceModel(providerYp.id, "yp:man:default:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentMan.id, segmentationNodeSegment.id to segmentDefault.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuVlaDev = resourceModel(providerYp.id, "yp:vla:dev:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentVla.id, segmentationNodeSegment.id to segmentDev.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuVlaDefault = resourceModel(providerYp.id, "yp:vla:default:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentVla.id, segmentationNodeSegment.id to segmentDefault.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuIvaDev = resourceModel(providerYp.id, "yp:iva:dev:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentIva.id, segmentationNodeSegment.id to segmentDev.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuIvaDefault = resourceModel(providerYp.id, "yp:iva:default:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentIva.id, segmentationNodeSegment.id to segmentDefault.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuMytDev = resourceModel(providerYp.id, "yp:myt:dev:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentMyt.id, segmentationNodeSegment.id to segmentDev.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        val resourceCpuMytDefault = resourceModel(providerYp.id, "yp:myt:default:cpu_capacity", resourceTypeCpu.id,
            mapOf(segmentationCluster.id to segmentMyt.id, segmentationNodeSegment.id to segmentDefault.id),
            UnitsEnsembleIds.CPU_UNITS_ID, setOf(UnitIds.MILLICORES, UnitIds.CORES), UnitIds.CORES, UnitIds.MILLICORES)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourcesDao.upsertResourcesRetryable(txSession,
            listOf(resourceCpuSasDev, resourceCpuSasDefault, resourceCpuManDev, resourceCpuManDefault,
                resourceCpuVlaDev, resourceCpuVlaDefault, resourceCpuIvaDev, resourceCpuIvaDefault,
                resourceCpuMytDev, resourceCpuMytDefault)).awaitSingleOrNull() }}
        val accountRootFirst = accountModel(providerYp.id, folderRoot.id, "test", "one")
        val accountRootSecond = accountModel(providerYp.id, folderRoot.id, "test", "two")
        val accountChildOneFirst = accountModel(providerYp.id, folderChildOne.id, "test", "three")
        val accountChildOneSecond = accountModel(providerYp.id, folderChildOne.id, "test", "four")
        val accountChildTwoFirst = accountModel(providerYp.id, folderChildTwo.id, "test", "five")
        val accountChildTwoSecond = accountModel(providerYp.id, folderChildTwo.id, "test", "six")
        val accountGrandchildOneFirst = accountModel(providerYp.id, folderGrandchildOne.id, "test", "seven")
        val accountGrandchildOneSecond = accountModel(providerYp.id, folderGrandchildOne.id, "test", "eight")
        val accountGrandchildTwoFirst = accountModel(providerYp.id, folderGrandchildTwo.id, "test", "nine")
        val accountGrandchildTwoSecond = accountModel(providerYp.id, folderGrandchildTwo.id, "test", "ten")
        dbSessionRetryable(tableClient) { rwTxRetryable {
            accountsDao.upsertAllRetryable(txSession, listOf(accountRootFirst, accountRootSecond, accountChildOneFirst,
                accountChildOneSecond, accountChildTwoFirst, accountChildTwoSecond, accountGrandchildOneFirst,
                accountGrandchildOneSecond, accountGrandchildTwoFirst, accountGrandchildTwoSecond)).awaitSingleOrNull()
        }}
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
        (ytReader as YtReaderStub).setRows(prepareTableRows())
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
            .uri("/admin/settings/ypUsageSync")
            .bodyValue(YpUsageSyncSettingsDto(resourceTypeCpu.id, null, segmentationCluster.id, segmentationNodeSegment.id,
                null, null, segmentDefault.id, segmentDev.id, null, null,
                UnitsEnsembleIds.CPU_UNITS_ID, UnitIds.CORES, true))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/maintenance/_syncYpUsage")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
    }

    private fun prepareTableRows(): List<YpUsageSyncService.YpUsageTableRow> {
        return listOf(
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "sas", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "sas", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "sas", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "sas", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "sas", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "man", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "man", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "man", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "man", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "man", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "vla", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "vla", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "vla", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "vla", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "vla", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "iva", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "iva", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "iva", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "iva", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "iva", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "myt", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "myt", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "myt", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "myt", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "myt", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "sas", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "sas", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "sas", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "sas", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "sas", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "man", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "man", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "man", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "man", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "man", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "vla", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "vla", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "vla", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "vla", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "vla", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "iva", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "iva", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "iva", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "iva", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "iva", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "myt", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "myt", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "myt", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "myt", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "myt", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "sas", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "sas", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "sas", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "sas", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "sas", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "man", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "man", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "man", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "man", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "man", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "vla", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "vla", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "vla", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "vla", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "vla", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "iva", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "iva", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "iva", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "iva", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "iva", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "myt", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "myt", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "myt", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "myt", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "myt", "YP", "default"),
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "sas", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "sas", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "sas", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "sas", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "sas", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "man", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "man", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "man", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "man", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "man", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "vla", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "vla", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "vla", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "vla", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "vla", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "iva", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "iva", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "iva", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "iva", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "iva", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("root", 1.0, "myt", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-one", 1.0, "myt", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("child-two", 1.0, "myt", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-one", 1.0, "myt", "YP", "dev"),
            YpUsageSyncService.YpUsageTableRow("grandchild-two", 1.0, "myt", "YP", "dev")
        )
    }

    private fun usageAmount(unused: Long): UsageAmount {
        return UsageAmount(
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
            unused = BigInteger.valueOf(unused * 1000)
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
            .accountsSpacesSupported(false)
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
        baseUnitId: String
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
            .accountsSpacesId(null)
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

    private fun accountModel(providerId: String, folderId: String,
                             displayName: String, externalKey: String): AccountModel {
        return AccountModel.Builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setId(UUID.randomUUID().toString())
            .setVersion(0L)
            .setProviderId(providerId)
            .setAccountsSpacesId(null)
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

    private fun serviceModel(id: Long, parentId: Long?, slug: String): ServiceRecipeModel {
        return ServiceRecipeModel.builder()
            .id(id)
            .name("test")
            .nameEn("test")
            .slug(slug)
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

}
