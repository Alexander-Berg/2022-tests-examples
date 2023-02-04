package ru.yandex.intranet.d.web.api.provisions

import com.google.protobuf.util.Timestamps
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestFolders.TEST_FOLDER_2_ID
import ru.yandex.intranet.d.TestFolders.TEST_MARKET_DEFAULT_FOLDER_ID
import ru.yandex.intranet.d.TestProviders.MDB_ID
import ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_IR
import ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_MARKET
import ru.yandex.intranet.d.UnitIds.*
import ru.yandex.intranet.d.UnitsEnsembleIds.CPU_UNITS_ID
import ru.yandex.intranet.d.UnitsEnsembleIds.STORAGE_UNITS_BINARY_ID
import ru.yandex.intranet.d.backend.service.provider_proto.*
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.Tenants.DEFAULT_TENANT_ID
import ru.yandex.intranet.d.dao.accounts.AccountsDao
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao
import ru.yandex.intranet.d.dao.providers.ProvidersDao
import ru.yandex.intranet.d.dao.quotas.QuotasDao
import ru.yandex.intranet.d.dao.resources.ResourcesDao
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService
import ru.yandex.intranet.d.loaders.providers.ProvidersLoader
import ru.yandex.intranet.d.model.accounts.AccountModel
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel
import ru.yandex.intranet.d.model.providers.ProviderModel
import ru.yandex.intranet.d.model.quotas.QuotaModel
import ru.yandex.intranet.d.model.resources.ResourceModel
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel
import ru.yandex.intranet.d.services.elements.FolderServiceElement
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.controllers.legacy.dto.DiQuota
import ru.yandex.intranet.d.web.controllers.legacy.dto.DiUnit
import ru.yandex.intranet.d.web.controllers.legacy.dto.UpdateQuotaRequestBody
import ru.yandex.intranet.d.web.model.ErrorCollectionDto
import ru.yandex.intranet.d.web.model.legacy.DispenserGetQuotasResponseDto
import java.time.Instant
import java.util.*

@IntegrationTest
class DispenserApiCompatibleProvisionControllerTest @Autowired constructor(
    private val webTestClient: WebTestClient,
    private val tableClient: YdbTableClient,
    private val providersDao: ProvidersDao,
    private val resourcesDao: ResourcesDao,
    private val resourceTypesDao: ResourceTypesDao,
    private val accountsDao: AccountsDao,
    private val accountsQuotasDao: AccountsQuotasDao,
    private val providersLoader: ProvidersLoader,
    @Value("\${hardwareOrderService.tvmSourceId}")
    private val dispenserTvmSourceId: Long,
    private val folderServiceElement: FolderServiceElement,
    private val quotasDao: QuotasDao,
    private val stubProviderService: StubProviderService,
) {

    @Test
    fun getQuotasTest(): Unit = runBlocking {
        prepareData()

        val response = webTestClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .get()
            .uri("/dispenser/db/api/v2/quotas?project=dispenser")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(DispenserGetQuotasResponseDto::class.java)
            .responseBody
            .awaitSingle()

//        assertEquals(4, response.result.size)

        val cpu = response.result.first { r -> r.key.resourceKey == "cpu" }
        assertEquals("dispenser", cpu.key.projectKey)
        assertEquals("cpu-quota", cpu.key.quotaSpecKey)
        assertEquals("mdb", cpu.key.serviceKey)
        assertTrue(cpu.key.segmentKeys.isEmpty())
        assertEquals(3000L, cpu.max)
        assertEquals(1000L, cpu.actual)
        assertEquals(0L, cpu.ownActual)
        assertEquals(0L, cpu.ownMax)
        assertNull(cpu.lastOverquotingTs)

        val hdd = response.result.first { r -> r.key.resourceKey == "hdd" }
        assertEquals("dispenser", hdd.key.projectKey)
        assertEquals("hdd-quota", hdd.key.quotaSpecKey)
        assertEquals("mdb", hdd.key.serviceKey)
        assertTrue(hdd.key.segmentKeys.isEmpty())
        assertEquals(123456789L, hdd.max)
        assertEquals(100000000L, hdd.actual)
        assertEquals(0L, hdd.ownActual)
        assertEquals(0L, hdd.ownMax)
        assertNull(hdd.lastOverquotingTs)

        val ssd = response.result.first { r -> r.key.resourceKey == "ssd" }
        assertEquals("dispenser", ssd.key.projectKey)
        assertEquals("ssd-quota", ssd.key.quotaSpecKey)
        assertEquals("mdb", ssd.key.serviceKey)
        assertTrue(ssd.key.segmentKeys.isEmpty())
        assertEquals(12345678L, ssd.max)
        assertEquals(10000000L, ssd.actual)
        assertEquals(0L, ssd.ownActual)
        assertEquals(0L, ssd.ownMax)
        assertNull(ssd.lastOverquotingTs)

//        val ram = response.result.first { r -> r.key.resourceKey == "ram" }
//        assertEquals("dispenser", ram.key.projectKey)
//        assertEquals("ram-quota", ram.key.quotaSpecKey)
//        assertEquals("mdb", ram.key.serviceKey)
//        assertTrue(ram.key.segmentKeys.isEmpty())
//        assertEquals(0L, ram.max)
//        assertEquals(0L, ram.actual)
//        assertEquals(0L, ram.ownActual)
//        assertEquals(0L, ram.ownMax)
//        assertNull(ram.lastOverquotingTs)
    }

    @Test
    fun getQuotasNoDefaultFolderTest(): Unit = runBlocking {
        val response = webTestClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .get()
            .uri("/dispenser/db/api/v2/quotas?project=d")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is4xxClientError
            .returnResult(ErrorCollectionDto::class.java)
            .responseBody
            .awaitSingle()

        assertEquals(1, response.errors.size)
        assertTrue(response.errors.contains("Folder not found."))
    }

    @Test
    fun getQuotasEmptyResultTest(): Unit = runBlocking {
        prepareProviderAndAccount()
        val response = webTestClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .get()
            .uri("/dispenser/db/api/v2/quotas?project=tracker")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(DispenserGetQuotasResponseDto::class.java)
            .responseBody
            .awaitSingle()

        assertEquals(0, response.result.size)
    }

    @Test
    fun getQuotasServiceNotFoundTest(): Unit = runBlocking {
        prepareProviderAndAccount()
        val response = webTestClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .get()
            .uri("/dispenser/db/api/v2/quotas?project=something_weird")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is4xxClientError
            .returnResult(ErrorCollectionDto::class.java)
            .responseBody
            .awaitSingle()

        assertEquals(1, response.errors.size)
        assertTrue(response.errors.contains("Service not found."))
    }

    @Test
    fun getQuotasNoProjectKeyPassedTest(): Unit = runBlocking {
        webTestClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .get()
            .uri("/dispenser/db/api/v2/quotas")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    /**
     * @see ru.yandex.intranet.d.web.controllers.legacy.DispenserApiCompatibleController.update()
     */
    @Test
    fun updateQuotasTest(): Unit = runBlocking {
        val data = prepareData()
        val marketFolder = folderServiceElement.getOrCreateDefaultFolder(
            DEFAULT_TENANT_ID, TEST_SERVICE_ID_MARKET, Locale.getDefault()
        ).awaitSingleOrNull()!!.match({ it }, { throw AssertionError("No default folder") })
        val irFolder = folderServiceElement.getOrCreateDefaultFolder(
            DEFAULT_TENANT_ID, TEST_SERVICE_ID_IR, Locale.getDefault()
        ).awaitSingleOrNull()!!.match({ it }, { throw AssertionError("No default folder") })
        dbSessionRetryable(tableClient) {
            val marketAccount = accountsDao.upsertOneRetryable(rwSingleRetryableCommit(), AccountModel.Builder()
                    .setId(UUID.randomUUID().toString())
                    .setFolderId(marketFolder.id)
                    .setDeleted(false)
                    .setDisplayName("market")
                    .setOuterAccountIdInProvider("market")
                    .setOuterAccountKeyInProvider("market")
                    .setProviderId(MDB_ID)
                    .setLastAccountUpdate(Instant.now())
                    .setTenantId(DEFAULT_TENANT_ID)
                    .build()
            ).awaitSingleOrNull()
            quotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(
                QuotaModel(
                    /* tenantId = */ DEFAULT_TENANT_ID,
                    /* folderId = */ marketFolder.id,
                    /* providerId = */ MDB_ID,
                    /* resourceId = */ data.cpu.id,
                    /* quota = */ 10000L,
                    /* balance = */ 0L,
                    /* frozenQuota = */ 0L
                ),
                QuotaModel(
                    /* tenantId = */ DEFAULT_TENANT_ID,
                    /* folderId = */ marketFolder.id,
                    /* providerId = */ MDB_ID,
                    /* resourceId = */ data.hdd.id,
                    /* quota = */ 1024L*1024L,
                    /* balance = */ 0L,
                    /* frozenQuota = */ 0L
                ),
            )).awaitSingleOrNull()
            accountsQuotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(
                AccountsQuotasModel.Builder()
                    .setAccountId(marketAccount!!.id)
                    .setFolderId(marketFolder.id)
                    .setProviderId(MDB_ID)
                    .setTenantId(DEFAULT_TENANT_ID)
                    .setResourceId(data.cpu.id)
                    .setProvidedQuota(10000L)
                    .setAllocatedQuota(0L)
                    .setFrozenProvidedQuota(0L)
                    .setLastProvisionUpdate(Instant.now())
                    .build(),
                AccountsQuotasModel.Builder()
                    .setAccountId(marketAccount.id)
                    .setFolderId(marketFolder.id)
                    .setProviderId(MDB_ID)
                    .setTenantId(DEFAULT_TENANT_ID)
                    .setResourceId(data.hdd.id)
                    .setProvidedQuota(1024L*1024L)
                    .setAllocatedQuota(0L)
                    .setFrozenProvidedQuota(0L)
                    .setLastProvisionUpdate(Instant.now())
                    .build(),
            )).awaitSingleOrNull()
        }

        setupStubProvider(millicores = listOf(10000L - 1000L, 1000L))
        val result = webTestClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .post()
            .uri(
                "/dispenser/db/api/v1/quotas/{project_key}/{service_key}/{resource_key}/{quota_spec_key}", mapOf(
                    "project_key" to "ir",
                    "service_key" to "mdb",
                    "resource_key" to "cpu",
                    "quota_spec_key" to "cpu-quota"
                )
            )
            .header("X-Yandex-UID", "1120000000000014")
            .bodyValue(UpdateQuotaRequestBody(
                maxValue = 1, unit = DiUnit.CORES
            ))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .returnResult(DiQuota::class.java)
            .responseBody
            .awaitSingle()

        assertEquals(TEST_SERVICE_ID_IR.toInt(), result.project.abcServiceId)
        assertEquals(1000L, result.max.value)
        assertEquals(DiUnit.PERMILLE_CORES, result.max.unit)

        val newQuota = webTestClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .get()
            .uri("/dispenser/db/api/v2/quotas?project=ir")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(DispenserGetQuotasResponseDto::class.java)
            .responseBody
            .awaitSingle()

        assertEquals(1, newQuota.result.size)
        assertEquals("ir", newQuota.result[0].key.projectKey)
        assertEquals("cpu", newQuota.result[0].key.resourceKey)
        assertEquals(1000L, newQuota.result[0].max)

        val resultMarketQuotas = dbSessionRetryable(tableClient) {
            quotasDao.getByFolders(
                roStaleSingleRetryableCommit(),
                listOf(TEST_MARKET_DEFAULT_FOLDER_ID),
                Tenants.DEFAULT_TENANT_ID
            ).awaitSingleOrNull()!!
        }!!
        val cpuQuota = resultMarketQuotas.first { it.resourceId == data.cpu.id }
        assertEquals( 9000L, cpuQuota.quota)
        assertEquals(0L, cpuQuota.balance)
    }

    /**
     * @see ru.yandex.intranet.d.web.controllers.legacy.DispenserApiCompatibleController.update()
     */
    @Test
    fun updateExistingQuotasTest(): Unit = runBlocking {
        val data = prepareData()
        val marketFolder = folderServiceElement.getOrCreateDefaultFolder(
            DEFAULT_TENANT_ID, TEST_SERVICE_ID_MARKET, Locale.getDefault()
        ).awaitSingleOrNull()!!.match({ it }, { throw AssertionError("No default folder") })
        val irFolder = folderServiceElement.getOrCreateDefaultFolder(
            DEFAULT_TENANT_ID, TEST_SERVICE_ID_IR, Locale.getDefault()
        ).awaitSingleOrNull()!!.match({ it }, { throw AssertionError("No default folder") })
        dbSessionRetryable(tableClient) {
            val marketAccount = accountsDao.upsertOneRetryable(rwSingleRetryableCommit(), AccountModel.Builder()
                .setId(UUID.randomUUID().toString())
                .setFolderId(marketFolder.id)
                .setDeleted(false)
                .setDisplayName("market")
                .setOuterAccountIdInProvider("market")
                .setOuterAccountKeyInProvider("market")
                .setProviderId(MDB_ID)
                .setLastAccountUpdate(Instant.now())
                .setTenantId(DEFAULT_TENANT_ID)
                .build()
            ).awaitSingleOrNull()!!
            val irAccount = accountsDao.upsertOneRetryable(rwSingleRetryableCommit(), AccountModel.Builder()
                .setId(UUID.randomUUID().toString())
                .setFolderId(irFolder.id)
                .setDeleted(false)
                .setDisplayName("ir")
                .setOuterAccountIdInProvider("ir")
                .setOuterAccountKeyInProvider("ir")
                .setProviderId(MDB_ID)
                .setLastAccountUpdate(Instant.now())
                .setTenantId(DEFAULT_TENANT_ID)
                .build()
            ).awaitSingleOrNull()!!
            quotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(
                QuotaModel(
                    /* tenantId = */ DEFAULT_TENANT_ID,
                    /* folderId = */ marketFolder.id,
                    /* providerId = */ MDB_ID,
                    /* resourceId = */ data.cpu.id,
                    /* quota = */ 10000L,
                    /* balance = */ 0L,
                    /* frozenQuota = */ 0L
                ),
                QuotaModel(
                    /* tenantId = */ DEFAULT_TENANT_ID,
                    /* folderId = */ marketFolder.id,
                    /* providerId = */ MDB_ID,
                    /* resourceId = */ data.hdd.id,
                    /* quota = */ 1024L*1024L,
                    /* balance = */ 0L,
                    /* frozenQuota = */ 0L
                ),
                QuotaModel(
                    /* tenantId = */ DEFAULT_TENANT_ID,
                    /* folderId = */ irFolder.id,
                    /* providerId = */ MDB_ID,
                    /* resourceId = */ data.cpu.id,
                    /* quota = */ 1000L,
                    /* balance = */ 0L,
                    /* frozenQuota = */ 0L
                ),
            )).awaitSingleOrNull()
            accountsQuotasDao.upsertAllRetryable(rwSingleRetryableCommit(), listOf(
                AccountsQuotasModel.Builder()
                    .setAccountId(marketAccount.id)
                    .setFolderId(marketFolder.id)
                    .setProviderId(MDB_ID)
                    .setTenantId(DEFAULT_TENANT_ID)
                    .setResourceId(data.cpu.id)
                    .setProvidedQuota(10000L)
                    .setAllocatedQuota(0L)
                    .setFrozenProvidedQuota(0L)
                    .setLastProvisionUpdate(Instant.now())
                    .build(),
                AccountsQuotasModel.Builder()
                    .setAccountId(marketAccount.id)
                    .setFolderId(marketFolder.id)
                    .setProviderId(MDB_ID)
                    .setTenantId(DEFAULT_TENANT_ID)
                    .setResourceId(data.hdd.id)
                    .setProvidedQuota(1024L*1024L)
                    .setAllocatedQuota(0L)
                    .setFrozenProvidedQuota(0L)
                    .setLastProvisionUpdate(Instant.now())
                    .build(),
                AccountsQuotasModel.Builder()
                    .setAccountId(irAccount.id)
                    .setFolderId(irFolder.id)
                    .setProviderId(MDB_ID)
                    .setTenantId(DEFAULT_TENANT_ID)
                    .setResourceId(data.cpu.id)
                    .setProvidedQuota(1000L)
                    .setAllocatedQuota(0L)
                    .setFrozenProvidedQuota(0L)
                    .setLastProvisionUpdate(Instant.now())
                    .build(),
            )).awaitSingleOrNull()
        }

        setupStubProvider(millicores = listOf(10000L - 1000L, 2000L))
        val result = webTestClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .post()
            .uri(
                "/dispenser/db/api/v1/quotas/{project_key}/{service_key}/{resource_key}/{quota_spec_key}", mapOf(
                    "project_key" to "ir",
                    "service_key" to "mdb",
                    "resource_key" to "cpu",
                    "quota_spec_key" to "cpu-quota"
                )
            )
            .header("X-Yandex-UID", "1120000000000014")
            .bodyValue(UpdateQuotaRequestBody(
                maxValue = 2, unit = DiUnit.CORES
            ))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .returnResult(DiQuota::class.java)
            .responseBody
            .awaitSingle()

        assertEquals(TEST_SERVICE_ID_IR.toInt(), result.project.abcServiceId)
        assertEquals(2000L, result.max.value)
        assertEquals(DiUnit.PERMILLE_CORES, result.max.unit)

        val newQuota = webTestClient
            .mutateWith(MockUser.tvm(dispenserTvmSourceId))
            .get()
            .uri("/dispenser/db/api/v2/quotas?project=ir")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(DispenserGetQuotasResponseDto::class.java)
            .responseBody
            .awaitSingle()

        assertEquals(1, newQuota.result.size)
        assertEquals("ir", newQuota.result[0].key.projectKey)
        assertEquals("cpu", newQuota.result[0].key.resourceKey)
        assertEquals(2000L, newQuota.result[0].max)

        val resultMarketQuotas = dbSessionRetryable(tableClient) {
            quotasDao.getByFolders(
                roStaleSingleRetryableCommit(),
                listOf(TEST_MARKET_DEFAULT_FOLDER_ID),
                Tenants.DEFAULT_TENANT_ID
            ).awaitSingleOrNull()!!
        }!!
        val cpuQuota = resultMarketQuotas.first { it.resourceId == data.cpu.id }
        assertEquals( 9000L, cpuQuota.quota)
        assertEquals(0L, cpuQuota.balance)
    }

    private suspend fun prepareData(): Data {
        val data = Data()
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                data.mdb = providersLoader.getProviderById(
                    txSession,
                    MDB_ID,
                    DEFAULT_TENANT_ID
                ).awaitSingle()!!.get()
                data.mdb = ProviderModel.builder(data.mdb)
                    .deleted(false)
                    .build()
                providersDao.updateProviderRetryable(txSession, data.mdb).awaitSingleOrNull()
                val account = AccountModel.Builder()
                    .setId(UUID.randomUUID().toString())
                    .setFolderId(TEST_FOLDER_2_ID)
                    .setDeleted(false)
                    .setDisplayName("test")
                    .setOuterAccountIdInProvider("test")
                    .setOuterAccountKeyInProvider("test")
                    .setProviderId(MDB_ID)
                    .setLastAccountUpdate(Instant.now())
                    .setTenantId(DEFAULT_TENANT_ID)
                    .build()
                data.cpuType = ResourceTypeModel.builder()
                    .id(UUID.randomUUID().toString())
                    .providerId(MDB_ID)
                    .tenantId(DEFAULT_TENANT_ID)
                    .key("cpu_cores")
                    .deleted(false)
                    .baseUnitId(MILLICORES)
                    .nameEn("CPU")
                    .nameRu("CPU")
                    .descriptionEn("CPU")
                    .descriptionRu("CPU")
                    .unitsEnsembleId(CPU_UNITS_ID)
                    .version(1)
                    .sortingOrder(0)
                    .build()
                data.cpu = ResourceModel.builder()
                    .id(UUID.randomUUID().toString())
                    .providerId(MDB_ID)
                    .tenantId(DEFAULT_TENANT_ID)
                    .resourceTypeId(data.cpuType.id)
                    .key("cpu_cores")
                    .deleted(false)
                    .version(1)
                    .nameEn("CPU")
                    .nameRu("CPU")
                    .descriptionEn("CPU")
                    .descriptionRu("CPU")
                    .unitsEnsembleId(CPU_UNITS_ID)
                    .resourceUnits(ResourceUnitsModel(setOf(MILLICORES, CORES), CORES, MILLICORES))
                    .managed(true)
                    .orderable(true)
                    .readOnly(true)
                    .baseUnitId(MILLICORES)
                    .build()
                val cpuProvision = AccountsQuotasModel.Builder()
                    .setAccountId(account.id)
                    .setFolderId(TEST_FOLDER_2_ID)
                    .setProviderId(MDB_ID)
                    .setTenantId(DEFAULT_TENANT_ID)
                    .setResourceId(data.cpu.id)
                    .setProvidedQuota(3000L)
                    .setAllocatedQuota(1000L)
                    .setFrozenProvidedQuota(0L)
                    .setLastProvisionUpdate(Instant.now())
                    .build()
                val hddType = ResourceTypeModel.builder()
                    .id(UUID.randomUUID().toString())
                    .providerId(MDB_ID)
                    .tenantId(DEFAULT_TENANT_ID)
                    .key("hdd")
                    .deleted(false)
                    .baseUnitId(BYTES)
                    .nameEn("HDD")
                    .nameRu("HDD")
                    .descriptionEn("HDD")
                    .descriptionRu("HDD")
                    .unitsEnsembleId(STORAGE_UNITS_BINARY_ID)
                    .version(1)
                    .sortingOrder(0)
                    .build()
                data.hdd = ResourceModel.builder()
                    .id(UUID.randomUUID().toString())
                    .providerId(MDB_ID)
                    .tenantId(DEFAULT_TENANT_ID)
                    .resourceTypeId(hddType.id)
                    .key("hdd")
                    .deleted(false)
                    .version(1)
                    .nameEn("HDD")
                    .nameRu("HDD")
                    .descriptionEn("HDD")
                    .descriptionRu("HDD")
                    .unitsEnsembleId(STORAGE_UNITS_BINARY_ID)
                    .resourceUnits(ResourceUnitsModel(setOf(BINARY_BYTES, KIBIBYTES), BINARY_BYTES, BINARY_BYTES))
                    .managed(true)
                    .orderable(true)
                    .readOnly(true)
                    .baseUnitId(BINARY_BYTES)
                    .build()
                val hddProvision = AccountsQuotasModel.Builder()
                    .setAccountId(account.id)
                    .setFolderId(TEST_FOLDER_2_ID)
                    .setProviderId(MDB_ID)
                    .setTenantId(DEFAULT_TENANT_ID)
                    .setResourceId(data.hdd.id)
                    .setProvidedQuota(123456789L)
                    .setAllocatedQuota(100000000L)
                    .setFrozenProvidedQuota(0L)
                    .setLastProvisionUpdate(Instant.now())
                    .build()
                val ssdType = ResourceTypeModel.builder()
                    .id(UUID.randomUUID().toString())
                    .providerId(MDB_ID)
                    .tenantId(DEFAULT_TENANT_ID)
                    .key("ssd")
                    .deleted(false)
                    .baseUnitId(BYTES)
                    .nameEn("SSD")
                    .nameRu("SSD")
                    .descriptionEn("SSD")
                    .descriptionRu("SSD")
                    .unitsEnsembleId(STORAGE_UNITS_BINARY_ID)
                    .version(1)
                    .sortingOrder(0)
                    .build()
                val ssd = ResourceModel.builder()
                    .id(UUID.randomUUID().toString())
                    .providerId(MDB_ID)
                    .tenantId(DEFAULT_TENANT_ID)
                    .resourceTypeId(ssdType.id)
                    .key("ssd")
                    .deleted(false)
                    .version(1)
                    .nameEn("SSD")
                    .nameRu("SSD")
                    .descriptionEn("SSD")
                    .descriptionRu("SSD")
                    .unitsEnsembleId(STORAGE_UNITS_BINARY_ID)
                    .resourceUnits(ResourceUnitsModel(setOf(BYTES, KILOBYTES), KILOBYTES, BYTES))
                    .managed(false)
                    .orderable(true)
                    .readOnly(true)
                    .baseUnitId(BYTES)
                    .build()
                val ssdProvision = AccountsQuotasModel.Builder()
                    .setAccountId(account.id)
                    .setFolderId(TEST_FOLDER_2_ID)
                    .setProviderId(MDB_ID)
                    .setTenantId(DEFAULT_TENANT_ID)
                    .setResourceId(ssd.id)
                    .setProvidedQuota(12345678L)
                    .setAllocatedQuota(10000000L)
                    .setFrozenProvidedQuota(0L)
                    .setLastProvisionUpdate(Instant.now())
                    .build()
                val ramType = ResourceTypeModel.builder()
                    .id(UUID.randomUUID().toString())
                    .providerId(MDB_ID)
                    .tenantId(DEFAULT_TENANT_ID)
                    .key("ram")
                    .deleted(false)
                    .baseUnitId(BYTES)
                    .nameEn("RAM")
                    .nameRu("RAM")
                    .descriptionEn("RAM")
                    .descriptionRu("RAM")
                    .unitsEnsembleId(STORAGE_UNITS_BINARY_ID)
                    .version(1)
                    .sortingOrder(0)
                    .build()
                val ram = ResourceModel.builder()
                    .id(UUID.randomUUID().toString())
                    .providerId(MDB_ID)
                    .tenantId(DEFAULT_TENANT_ID)
                    .resourceTypeId(ramType.id)
                    .key("ram")
                    .deleted(false)
                    .version(1)
                    .nameEn("RAM")
                    .nameRu("RAM")
                    .descriptionEn("RAM")
                    .descriptionRu("RAM")
                    .unitsEnsembleId(STORAGE_UNITS_BINARY_ID)
                    .resourceUnits(ResourceUnitsModel(setOf(BYTES, KILOBYTES), KILOBYTES, BYTES))
                    .managed(false)
                    .orderable(true)
                    .readOnly(true)
                    .baseUnitId(BYTES)
                    .build()
                val ramProvision = AccountsQuotasModel.Builder()
                    .setAccountId(account.id)
                    .setFolderId(TEST_FOLDER_2_ID)
                    .setProviderId(MDB_ID)
                    .setTenantId(DEFAULT_TENANT_ID)
                    .setResourceId(ram.id)
                    .setProvidedQuota(0L)
                    .setAllocatedQuota(0L)
                    .setFrozenProvidedQuota(0L)
                    .setLastProvisionUpdate(Instant.now())
                    .build()

                resourceTypesDao.upsertResourceTypesRetryable(txSession, listOf(data.cpuType, hddType, ssdType, ramType))
                    .awaitSingleOrNull()
                resourcesDao.upsertResourcesRetryable(txSession, listOf(data.cpu, data.hdd, ssd, ram)).awaitSingleOrNull()
                accountsDao.upsertOneRetryable(txSession, account).awaitSingleOrNull()
                accountsQuotasDao.upsertAllRetryable(
                    txSession,
                    listOf(cpuProvision, hddProvision, ssdProvision, ramProvision)
                )
                    .awaitSingleOrNull()
            }
        }
        return data
    }

    private suspend fun prepareProviderAndAccount() {
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                val mdb = providersLoader.getProviderById(
                    txSession,
                    MDB_ID,
                    DEFAULT_TENANT_ID
                ).awaitSingle()!!.get()
                val mdbNotDeleted = ProviderModel.builder(mdb)
                    .deleted(false)
                    .build()
                providersDao.updateProviderRetryable(txSession, mdbNotDeleted).awaitSingleOrNull()
            }
        }
    }

    private fun setupStubProvider(millicores: List<Long>) {
        stubProviderService.reset()
        stubProviderService.setUpdateProvisionResponses(millicores.map {
            GrpcResponse.success(
                UpdateProvisionResponse.newBuilder()
                    .addProvisions(Provision.newBuilder()
                        .setResourceKey(ResourceKey.newBuilder()
                            .setCompoundKey(CompoundResourceKey.newBuilder().setResourceTypeKey("cpu_cores"))
                        )
                        .setProvided(Amount.newBuilder()
                            .setValue(it)
                            .setUnitKey("millicores")
                        )
                        .setAllocated(Amount.newBuilder()
                            .setValue(0)
                            .setUnitKey("millicores")
                        )
                        .setLastUpdate(LastUpdate.newBuilder()
                            .setAuthor(UserID.newBuilder()
                                .setPassportUid(PassportUID.newBuilder().setPassportUid("1"))
                                .setStaffLogin(StaffLogin.newBuilder().setStaffLogin("test"))
                            )
                            .setOperationId(UUID.randomUUID().toString())
                            .setTimestamp(Timestamps.fromSeconds(Instant.now().epochSecond))
                        )
                    ).build()
                )
        })
        stubProviderService.setCreateAccountResponses(listOf(
            GrpcResponse.failure(StatusRuntimeException(Status.ALREADY_EXISTS))
        ))
        stubProviderService.setListAccountsByFolderResponses(listOf(
            GrpcResponse.success(ListAccountsByFolderResponse.newBuilder()
                .addAccounts(Account.newBuilder()
                    .setAccountId("123456")
                    .build())
                .build()
            )
        ))
    }
}

class Data {
    lateinit var cpu: ResourceModel
    lateinit var mdb: ProviderModel
    lateinit var cpuType: ResourceTypeModel
    lateinit var hdd: ResourceModel
}
