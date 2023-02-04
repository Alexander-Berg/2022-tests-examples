package ru.yandex.intranet.d.web.admin.settings

import kotlinx.coroutines.reactive.awaitSingle
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
import ru.yandex.intranet.d.dao.providers.ProvidersDao
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel
import ru.yandex.intranet.d.model.providers.AggregationSettings
import ru.yandex.intranet.d.model.providers.FreeProvisionAggregationMode
import ru.yandex.intranet.d.model.providers.ProviderModel
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.settings.KnownProvidersDto
import ru.yandex.intranet.d.web.model.settings.YpUsageSyncSettingsDto
import ru.yandex.intranet.d.web.model.settings.YtUsageSyncSettingsDto
import java.util.*

/**
 * Aggregation API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class RuntimeSettingsAdminControllerTest(
    @Autowired private val providersDao: ProvidersDao,
    @Autowired private val resourceTypesDao: ResourceTypesDao,
    @Autowired private val resourceSegmentationsDao: ResourceSegmentationsDao,
    @Autowired private val resourceSegmentsDao: ResourceSegmentsDao,
    @Autowired private val tableClient: YdbTableClient,
    @Autowired private val webClient: WebTestClient
) {

    @Test
    fun testYtUsageSyncSettings(): Unit = runBlocking {
        val providerOne = providerModel(false)
        val providerTwo = providerModel(false)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProvidersRetryable(txSession,
            listOf(providerOne, providerTwo)).awaitSingleOrNull() }}
        val resourceTypeOne = resourceTypeModel(providerOne.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypesRetryable(txSession,
            listOf(resourceTypeOne)).awaitSingleOrNull() }}
        val segmentationOne = resourceSegmentationModel(providerOne.id, "test")
        val segmentationTwo = resourceSegmentationModel(providerOne.id, "test")
        val segmentationThree = resourceSegmentationModel(providerOne.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentationsDao.upsertResourceSegmentationsRetryable(txSession,
            listOf(segmentationOne, segmentationTwo, segmentationThree)).awaitSingleOrNull() }}
        val segmentOne = resourceSegmentModel(segmentationOne.id, "test")
        val segmentTwo = resourceSegmentModel(segmentationTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession,
            listOf(segmentOne, segmentTwo)).awaitSingleOrNull() }}
        val resultGetEmptyKnownProviders = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/admin/settings/knownProviders")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(KnownProvidersDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(KnownProvidersDto(null, null, null), resultGetEmptyKnownProviders)
        val resultPutKnownProviders = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/admin/settings/knownProviders")
            .bodyValue(KnownProvidersDto(providerOne.id, providerTwo.id, null))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(KnownProvidersDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(KnownProvidersDto(providerOne.id, providerTwo.id, null), resultPutKnownProviders)
        val resultGetKnownProviders = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/admin/settings/knownProviders")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(KnownProvidersDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(KnownProvidersDto(providerOne.id, providerTwo.id, null), resultGetKnownProviders)
        val resultGetEmptyYtUsageSyncSettings = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/admin/settings/ytUsageSync")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(YtUsageSyncSettingsDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(YtUsageSyncSettingsDto(null, null, null,
            null, null, null, null, null, null, null),
            resultGetEmptyYtUsageSyncSettings)
        val resultPutYtUsageSyncSettings = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/admin/settings/ytUsageSync")
            .bodyValue(YtUsageSyncSettingsDto(resourceTypeOne.id, segmentationOne.id, segmentOne.id,
                segmentationThree.id, segmentationTwo.id, segmentTwo.id, true, 300L,
                UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, "b15101c2-da50-4d6f-9a8e-b90160871b0a"))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(YtUsageSyncSettingsDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(YtUsageSyncSettingsDto(resourceTypeOne.id, segmentationOne.id, segmentOne.id,
            segmentationThree.id, segmentationTwo.id, segmentTwo.id, true, 300L,
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, "b15101c2-da50-4d6f-9a8e-b90160871b0a"),
            resultPutYtUsageSyncSettings)
        val resultGetYtUsageSyncSettings = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/admin/settings/ytUsageSync")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(YtUsageSyncSettingsDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(YtUsageSyncSettingsDto(resourceTypeOne.id, segmentationOne.id, segmentOne.id,
            segmentationThree.id, segmentationTwo.id, segmentTwo.id, true, 300L,
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, "b15101c2-da50-4d6f-9a8e-b90160871b0a"),
            resultGetYtUsageSyncSettings)
    }

    @Test
    fun testYpUsageSyncSettings(): Unit = runBlocking {
        val providerOne = providerModel(false)
        val providerTwo = providerModel(false)
        val providerThree = providerModel(false)
        dbSessionRetryable(tableClient) { rwTxRetryable { providersDao.upsertProvidersRetryable(txSession,
            listOf(providerOne, providerTwo, providerThree)).awaitSingleOrNull() }}
        val resourceTypeOne = resourceTypeModel(providerOne.id, "test", UnitsEnsembleIds.CPU_UNITS_ID)
        val resourceTypeTwo = resourceTypeModel(providerTwo.id, "test", UnitsEnsembleIds.CPU_UNITS_ID)
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceTypesDao.upsertResourceTypesRetryable(txSession,
            listOf(resourceTypeOne, resourceTypeTwo)).awaitSingleOrNull() }}
        val segmentationOne = resourceSegmentationModel(providerOne.id, "test")
        val segmentationTwo = resourceSegmentationModel(providerOne.id, "test")
        val segmentationThree = resourceSegmentationModel(providerTwo.id, "test")
        val segmentationFour = resourceSegmentationModel(providerTwo.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentationsDao.upsertResourceSegmentationsRetryable(txSession,
            listOf(segmentationOne, segmentationTwo, segmentationThree, segmentationFour)).awaitSingleOrNull() }}
        val segmentOne = resourceSegmentModel(segmentationOne.id, "test")
        val segmentTwo = resourceSegmentModel(segmentationTwo.id, "test")
        val segmentThree = resourceSegmentModel(segmentationTwo.id, "test")
        val segmentFour = resourceSegmentModel(segmentationThree.id, "test")
        val segmentFive = resourceSegmentModel(segmentationFour.id, "test")
        val segmentSix = resourceSegmentModel(segmentationFour.id, "test")
        dbSessionRetryable(tableClient) { rwTxRetryable { resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession,
            listOf(segmentOne, segmentTwo, segmentThree, segmentFour, segmentFive, segmentSix)).awaitSingleOrNull() }}
        val resultGetEmptyKnownProviders = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/admin/settings/knownProviders")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(KnownProvidersDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(KnownProvidersDto(null, null, null), resultGetEmptyKnownProviders)
        val resultPutKnownProviders = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/admin/settings/knownProviders")
            .bodyValue(KnownProvidersDto(providerThree.id, providerOne.id, providerTwo.id))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(KnownProvidersDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(KnownProvidersDto(providerThree.id, providerOne.id, providerTwo.id), resultPutKnownProviders)
        val resultGetKnownProviders = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/admin/settings/knownProviders")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(KnownProvidersDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(KnownProvidersDto(providerThree.id, providerOne.id, providerTwo.id), resultGetKnownProviders)
        val resultGetEmptyYpUsageSyncSettings = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/admin/settings/ypUsageSync")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(YpUsageSyncSettingsDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(YpUsageSyncSettingsDto(null, null, null,
            null, null, null, null,
            null, null, null, null, null, null),
            resultGetEmptyYpUsageSyncSettings)
        val resultPutYpUsageSyncSettings = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .put()
            .uri("/admin/settings/ypUsageSync")
            .bodyValue(YpUsageSyncSettingsDto(resourceTypeOne.id, resourceTypeTwo.id, segmentationOne.id, segmentationTwo.id,
                segmentationThree.id, segmentationFour.id, segmentTwo.id, segmentThree.id, segmentFive.id, segmentSix.id,
                UnitsEnsembleIds.CPU_UNITS_ID, UnitIds.CORES, true))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(YpUsageSyncSettingsDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(YpUsageSyncSettingsDto(resourceTypeOne.id, resourceTypeTwo.id, segmentationOne.id,
            segmentationTwo.id, segmentationThree.id, segmentationFour.id, segmentTwo.id, segmentThree.id, segmentFive.id,
            segmentSix.id, UnitsEnsembleIds.CPU_UNITS_ID, UnitIds.CORES, true),
            resultPutYpUsageSyncSettings)
        val resultGetYpUsageSyncSettings = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/admin/settings/ypUsageSync")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(YpUsageSyncSettingsDto::class.java)
            .responseBody.awaitSingle()
        Assertions.assertEquals(YpUsageSyncSettingsDto(resourceTypeOne.id, resourceTypeTwo.id, segmentationOne.id,
            segmentationTwo.id, segmentationThree.id, segmentationFour.id, segmentTwo.id, segmentThree.id, segmentFive.id,
            segmentSix.id, UnitsEnsembleIds.CPU_UNITS_ID, UnitIds.CORES, true),
            resultGetYpUsageSyncSettings)
    }

    private fun providerModel(accountsSpacesSupported: Boolean): ProviderModel {
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
            .accountsSpacesSupported(accountsSpacesSupported)
            .syncEnabled(true)
            .grpcTlsOn(true)
            .aggregationSettings(AggregationSettings(FreeProvisionAggregationMode.NONE, null, null))
            .build()
    }

    private fun resourceTypeModel(providerId: String, key: String, unitsEnsembleId: String): ResourceTypeModel {
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
            .build()
    }

    private fun resourceSegmentationModel(providerId: String, key: String): ResourceSegmentationModel {
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

    private fun resourceSegmentModel(segmentationId: String, key: String): ResourceSegmentModel {
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
