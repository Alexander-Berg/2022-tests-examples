package ru.yandex.intranet.d.web.api.delivery

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.accounts.AccountsDao
import ru.yandex.intranet.d.dao.accounts.AccountsSpacesDao
import ru.yandex.intranet.d.dao.folders.FolderDao
import ru.yandex.intranet.d.dao.providers.ProvidersDao
import ru.yandex.intranet.d.dao.resources.ResourcesDao
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.accounts.AccountModel
import ru.yandex.intranet.d.model.accounts.AccountSpaceModel
import ru.yandex.intranet.d.model.folders.FolderModel
import ru.yandex.intranet.d.model.folders.FolderType
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel
import ru.yandex.intranet.d.model.providers.ProviderModel
import ru.yandex.intranet.d.model.resources.ResourceModel
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.delivery.DeliveryAccountDto
import ru.yandex.intranet.d.web.model.delivery.DeliveryAccountsSpaceDto
import ru.yandex.intranet.d.web.model.delivery.DeliveryDestinationDto
import ru.yandex.intranet.d.web.model.delivery.DeliveryDestinationProvidersDto
import ru.yandex.intranet.d.web.model.delivery.DeliveryDestinationRequestDto
import ru.yandex.intranet.d.web.model.delivery.DeliveryDestinationResourceDto
import ru.yandex.intranet.d.web.model.delivery.DeliveryDestinationResponseDto
import ru.yandex.intranet.d.web.model.delivery.DeliveryProviderDto
import ru.yandex.intranet.d.web.model.delivery.DeliveryResourcesAccountsDto
import java.time.Instant
import java.util.*

/**
 * Delivery destination API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class DeliveryDestinationApiTest(
    @Autowired private val webClient: WebTestClient,
    @Autowired private val providersDao: ProvidersDao,
    @Autowired private val resourceTypesDao: ResourceTypesDao,
    @Autowired private val resourceSegmentationsDao: ResourceSegmentationsDao,
    @Autowired private val resourceSegmentsDao: ResourceSegmentsDao,
    @Autowired private val resourcesDao: ResourcesDao,
    @Autowired private val folderDao: FolderDao,
    @Autowired private val accountsSpacesDao: AccountsSpacesDao,
    @Autowired private val accountsDao: AccountsDao,
    @Autowired private val tableClient: YdbTableClient,
    @Autowired @Value("\${hardwareOrderService.tvmSourceId}")
    private val dispenserTvmSourceId: Long
) {

    @Test
    fun testNoAccountsSpaces() {
        runBlocking {
            val providerOne = providerModel("in-process:test", null, false)
            val resourceTypeOne = resourceTypeModel(providerOne.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8")
            val resourceTypeTwo = resourceTypeModel(providerOne.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8")
            val locationSegmentationOne = resourceSegmentationModel(providerOne.id, "location")
            val vlaSegmentOne = resourceSegmentModel(locationSegmentationOne.id, "VLA")
            val resourceOne = resourceModel(providerOne.id, "testOne", resourceTypeOne.id,
                setOf(Pair(locationSegmentationOne.id, vlaSegmentOne.id)), "b02344bf-96af-4cc5-937c-66a479989ce8",
                setOf("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                    "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", null)
            val resourceTwo = resourceModel(providerOne.id, "testTwo", resourceTypeTwo.id,
                setOf(Pair(locationSegmentationOne.id, vlaSegmentOne.id)), "b02344bf-96af-4cc5-937c-66a479989ce8",
                setOf("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                    "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", null)
            val providerTwo = providerModel("in-process:test", null, false)
            val resourceTypeThree = resourceTypeModel(providerTwo.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8")
            val resourceTypeFour = resourceTypeModel(providerTwo.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8")
            val locationSegmentationTwo = resourceSegmentationModel(providerTwo.id, "location")
            val vlaSegmentTwo = resourceSegmentModel(locationSegmentationTwo.id, "VLA")
            val resourceThree = resourceModel(providerTwo.id, "testThree", resourceTypeThree.id,
                setOf(Pair(locationSegmentationTwo.id, vlaSegmentTwo.id)), "b02344bf-96af-4cc5-937c-66a479989ce8",
                setOf("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                    "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", null)
            val resourceFour = resourceModel(providerTwo.id, "testFour", resourceTypeFour.id,
                setOf(Pair(locationSegmentationTwo.id, vlaSegmentTwo.id)), "b02344bf-96af-4cc5-937c-66a479989ce8",
                setOf("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                    "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", null)
            val folderOne = folderModel(12L)
            val accountOne = accountModel(folderOne.id, providerOne.id, null)
            val accountTwo = accountModel(folderOne.id, providerTwo.id, null)
            dbSessionRetryable(tableClient) {
                rwTxRetryable {
                    providersDao.upsertProvidersRetryable(txSession, listOf(providerOne, providerTwo)).awaitSingleOrNull()
                    resourceTypesDao.upsertResourceTypesRetryable(txSession, listOf(resourceTypeOne, resourceTypeTwo,
                        resourceTypeThree, resourceTypeFour)).awaitSingleOrNull()
                    resourceSegmentationsDao.upsertResourceSegmentationsRetryable(txSession, listOf(locationSegmentationOne,
                        locationSegmentationTwo)).awaitSingleOrNull()
                    resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession, listOf(vlaSegmentOne, vlaSegmentTwo))
                        .awaitSingleOrNull()
                    resourcesDao.upsertResourcesRetryable(txSession, listOf(resourceOne, resourceTwo, resourceThree,
                        resourceFour)).awaitSingleOrNull()
                    folderDao.upsertAllRetryable(txSession, listOf(folderOne)).awaitSingleOrNull()
                    accountsDao.upsertAllRetryable(txSession, listOf(accountOne, accountTwo)).awaitSingleOrNull()
                }
            }
            val result = webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_findDestinations")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(
                    DeliveryDestinationRequestDto("1120000000000010",
                        listOf(DeliveryDestinationDto(12L, 69L,
                            listOf(resourceOne.id, resourceTwo.id, resourceThree.id, resourceFour.id)))))
                .exchange()
                .expectStatus()
                .isOk
                .expectBody(DeliveryDestinationResponseDto::class.java)
                .returnResult()
                .responseBody!!
            Assertions.assertEquals(DeliveryDestinationResponseDto(
                destinations = setOf(
                    DeliveryDestinationProvidersDto(
                        serviceId = 12,
                        quotaRequestId = 69,
                        eligible = true,
                        ineligibilityReasons=null,
                        providers = setOf(
                            DeliveryProviderDto(
                                id = providerOne.id,
                                name = providerOne.nameEn,
                                key = providerOne.key,
                                eligible = true,
                                ineligibilityReasons = null,
                                accountsSpaces = null,
                                resourcesAccounts = DeliveryResourcesAccountsDto(
                                    resourceIds = setOf(
                                        resourceOne.id,
                                        resourceTwo.id
                                    ),
                                    accounts = setOf(
                                        DeliveryAccountDto(
                                            id = accountOne.id,
                                            externalId = accountOne.outerAccountIdInProvider,
                                            externalKey = accountOne.outerAccountKeyInProvider.orElse(null),
                                            displayName = accountOne.displayName.orElse(null),
                                            folderId = folderOne.id,
                                            folderName = folderOne.displayName
                                        )
                                    )
                                )
                            ),
                            DeliveryProviderDto(
                                id = providerTwo.id,
                                name = providerTwo.nameEn,
                                key = providerTwo.key,
                                eligible = true,
                                ineligibilityReasons = null,
                                accountsSpaces = null,
                                resourcesAccounts = DeliveryResourcesAccountsDto(
                                    resourceIds = setOf(
                                        resourceThree.id,
                                        resourceFour.id
                                    ),
                                    accounts = setOf(
                                        DeliveryAccountDto(
                                            id = accountTwo.id,
                                            externalId = accountTwo.outerAccountIdInProvider,
                                            externalKey = accountTwo.outerAccountKeyInProvider.orElse(null),
                                            displayName = accountTwo.displayName.orElse(null),
                                            folderId = folderOne.id,
                                            folderName = folderOne.displayName
                                        )
                                    )
                                )
                            )
                        )
                    )
                ),
                resources = setOf(
                    DeliveryDestinationResourceDto(
                        id = resourceOne.id,
                        key = resourceOne.key,
                        name = resourceOne.nameEn,
                        eligible = true,
                        ineligibilityReasons = null
                    ),
                    DeliveryDestinationResourceDto(
                        id = resourceTwo.id,
                        key = resourceTwo.key,
                        name = resourceTwo.nameEn,
                        eligible = true,
                        ineligibilityReasons = null
                    ),
                    DeliveryDestinationResourceDto(
                        id = resourceThree.id,
                        key = resourceThree.key,
                        name = resourceThree.nameEn,
                        eligible = true,
                        ineligibilityReasons = null
                    ),
                    DeliveryDestinationResourceDto(
                        id = resourceFour.id,
                        key = resourceFour.key,
                        name = resourceFour.nameEn,
                        eligible = true,
                        ineligibilityReasons = null
                    )
                )
            ), result)
        }
    }

    @Test
    fun testWithAccountsSpaces() {
        runBlocking {
            val providerOne = providerModel("in-process:test", null, false)
            val resourceTypeOne = resourceTypeModel(providerOne.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8")
            val resourceTypeTwo = resourceTypeModel(providerOne.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8")
            val locationSegmentationOne = resourceSegmentationModel(providerOne.id, "location")
            val vlaSegmentOne = resourceSegmentModel(locationSegmentationOne.id, "VLA")
            val accountsSpaceOne = accountsSpaceModel(providerOne.id,
                setOf(Pair(locationSegmentationOne.id, vlaSegmentOne.id)))
            val resourceOne = resourceModel(providerOne.id, "testOne", resourceTypeOne.id,
                setOf(Pair(locationSegmentationOne.id, vlaSegmentOne.id)), "b02344bf-96af-4cc5-937c-66a479989ce8",
                setOf("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                    "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", accountsSpaceOne.id)
            val resourceTwo = resourceModel(providerOne.id, "testTwo", resourceTypeTwo.id,
                setOf(Pair(locationSegmentationOne.id, vlaSegmentOne.id)), "b02344bf-96af-4cc5-937c-66a479989ce8",
                setOf("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                    "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", accountsSpaceOne.id)
            val providerTwo = providerModel("in-process:test", null, false)
            val resourceTypeThree = resourceTypeModel(providerTwo.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8")
            val resourceTypeFour = resourceTypeModel(providerTwo.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8")
            val locationSegmentationTwo = resourceSegmentationModel(providerTwo.id, "location")
            val vlaSegmentTwo = resourceSegmentModel(locationSegmentationTwo.id, "VLA")
            val accountsSpaceTwo = accountsSpaceModel(providerOne.id,
                setOf(Pair(locationSegmentationOne.id, vlaSegmentOne.id)))
            val resourceThree = resourceModel(providerTwo.id, "testThree", resourceTypeThree.id,
                setOf(Pair(locationSegmentationTwo.id, vlaSegmentTwo.id)), "b02344bf-96af-4cc5-937c-66a479989ce8",
                setOf("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                    "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", accountsSpaceTwo.id)
            val resourceFour = resourceModel(providerTwo.id, "testFour", resourceTypeFour.id,
                setOf(Pair(locationSegmentationTwo.id, vlaSegmentTwo.id)), "b02344bf-96af-4cc5-937c-66a479989ce8",
                setOf("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                    "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
                "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a", accountsSpaceTwo.id)
            val folderOne = folderModel(12L)
            val accountOne = accountModel(folderOne.id, providerOne.id, accountsSpaceOne.id)
            val accountTwo = accountModel(folderOne.id, providerTwo.id, accountsSpaceTwo.id)
            dbSessionRetryable(tableClient) {
                rwTxRetryable {
                    providersDao.upsertProvidersRetryable(txSession, listOf(providerOne, providerTwo)).awaitSingleOrNull()
                    resourceTypesDao.upsertResourceTypesRetryable(txSession, listOf(resourceTypeOne, resourceTypeTwo,
                        resourceTypeThree, resourceTypeFour)).awaitSingleOrNull()
                    resourceSegmentationsDao.upsertResourceSegmentationsRetryable(txSession, listOf(locationSegmentationOne,
                        locationSegmentationTwo)).awaitSingleOrNull()
                    resourceSegmentsDao.upsertResourceSegmentsRetryable(txSession, listOf(vlaSegmentOne, vlaSegmentTwo))
                        .awaitSingleOrNull()
                    accountsSpacesDao.upsertAllRetryable(txSession, listOf(accountsSpaceOne, accountsSpaceTwo))
                        .awaitSingleOrNull()
                    resourcesDao.upsertResourcesRetryable(txSession, listOf(resourceOne, resourceTwo, resourceThree,
                        resourceFour)).awaitSingleOrNull()
                    folderDao.upsertAllRetryable(txSession, listOf(folderOne)).awaitSingleOrNull()
                    accountsDao.upsertAllRetryable(txSession, listOf(accountOne, accountTwo)).awaitSingleOrNull()
                }
            }
            val result = webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_findDestinations")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(
                    DeliveryDestinationRequestDto("1120000000000010",
                        listOf(DeliveryDestinationDto(12L, 69L,
                            listOf(resourceOne.id, resourceTwo.id, resourceThree.id, resourceFour.id)))))
                .exchange()
                .expectStatus()
                .isOk
                .expectBody(DeliveryDestinationResponseDto::class.java)
                .returnResult()
                .responseBody!!
            Assertions.assertEquals(DeliveryDestinationResponseDto(
                destinations = setOf(
                    DeliveryDestinationProvidersDto(
                        serviceId = 12,
                        quotaRequestId = 69,
                        eligible = true,
                        ineligibilityReasons=null,
                        providers = setOf(
                            DeliveryProviderDto(
                                id = providerOne.id,
                                name = providerOne.nameEn,
                                key = providerOne.key,
                                eligible = true,
                                ineligibilityReasons = null,
                                accountsSpaces = setOf(
                                    DeliveryAccountsSpaceDto(
                                        id = accountsSpaceOne.id,
                                        name = accountsSpaceOne.nameEn,
                                        key = accountsSpaceOne.outerKeyInProvider,
                                        eligible = true,
                                        ineligibilityReasons = null,
                                        resourcesAccounts = DeliveryResourcesAccountsDto(
                                            resourceIds = setOf(
                                                resourceOne.id,
                                                resourceTwo.id
                                            ),
                                            accounts = setOf(
                                                DeliveryAccountDto(
                                                    id = accountOne.id,
                                                    externalId = accountOne.outerAccountIdInProvider,
                                                    externalKey = accountOne.outerAccountKeyInProvider.orElse(null),
                                                    displayName = accountOne.displayName.orElse(null),
                                                    folderId = folderOne.id,
                                                    folderName = folderOne.displayName
                                                )
                                            )
                                        )
                                    )
                                ),
                                resourcesAccounts = null
                            ),
                            DeliveryProviderDto(
                                id = providerTwo.id,
                                name = providerTwo.nameEn,
                                key = providerTwo.key,
                                eligible = true,
                                ineligibilityReasons = null,
                                accountsSpaces = setOf(
                                    DeliveryAccountsSpaceDto(
                                        id = accountsSpaceTwo.id,
                                        name = accountsSpaceTwo.nameEn,
                                        key = accountsSpaceTwo.outerKeyInProvider,
                                        eligible = true,
                                        ineligibilityReasons = null,
                                        resourcesAccounts = DeliveryResourcesAccountsDto(
                                            resourceIds = setOf(
                                                resourceThree.id,
                                                resourceFour.id
                                            ),
                                            accounts = setOf(
                                                DeliveryAccountDto(
                                                    id = accountTwo.id,
                                                    externalId = accountTwo.outerAccountIdInProvider,
                                                    externalKey = accountTwo.outerAccountKeyInProvider.orElse(null),
                                                    displayName = accountTwo.displayName.orElse(null),
                                                    folderId = folderOne.id,
                                                    folderName = folderOne.displayName
                                                )
                                            )
                                        )
                                    )
                                ),
                                resourcesAccounts = null
                            )
                        )
                    )
                ),
                resources = setOf(
                    DeliveryDestinationResourceDto(
                        id = resourceOne.id,
                        key = resourceOne.key,
                        name = resourceOne.nameEn,
                        eligible = true,
                        ineligibilityReasons = null
                    ),
                    DeliveryDestinationResourceDto(
                        id = resourceTwo.id,
                        key = resourceTwo.key,
                        name = resourceTwo.nameEn,
                        eligible = true,
                        ineligibilityReasons = null
                    ),
                    DeliveryDestinationResourceDto(
                        id = resourceThree.id,
                        key = resourceThree.key,
                        name = resourceThree.nameEn,
                        eligible = true,
                        ineligibilityReasons = null
                    ),
                    DeliveryDestinationResourceDto(
                        id = resourceFour.id,
                        key = resourceFour.key,
                        name = resourceFour.nameEn,
                        eligible = true,
                        ineligibilityReasons = null
                    )
                )
            ), result)
        }
    }

    private fun providerModel(grpcUri: String?, restUri: String?, accountsSpacesSupported: Boolean): ProviderModel {
        return providerModel(grpcUri, restUri, accountsSpacesSupported, 69L)
    }

    private fun providerModel(grpcUri: String?, restUri: String?, accountsSpacesSupported: Boolean,
                              serviceId: Long): ProviderModel {
        return ProviderModel.builder()
            .id(UUID.randomUUID().toString())
            .grpcApiUri(grpcUri)
            .restApiUri(restUri)
            .destinationTvmId(42L)
            .tenantId(Tenants.DEFAULT_TENANT_ID)
            .version(0L)
            .nameEn("Test")
            .nameRu("Test")
            .descriptionEn("Test")
            .descriptionRu("Test")
            .sourceTvmId(42L)
            .serviceId(serviceId)
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
                    .softDeleteSupported(true)
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

    private fun resourceModel(
        providerId: String, key: String, resourceTypeId: String,
        segments: Set<Pair<String, String>>, unitsEnsembleId: String,
        allowedUnitIds: Set<String>, defaultUnitId: String,
        baseUnitId: String, accountsSpaceId: String?
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
            .segments(segments.map { t -> ResourceSegmentSettingsModel(t.first, t.second) }.toSet())
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

    private fun accountModel(folderId: String, providerId: String, accountsSpaceId: String?): AccountModel {
        return AccountModel.Builder()
            .setId(UUID.randomUUID().toString())
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setProviderId(providerId)
            .setFolderId(folderId)
            .setAccountsSpacesId(accountsSpaceId)
            .setOuterAccountIdInProvider(UUID.randomUUID().toString())
            .setOuterAccountKeyInProvider("test")
            .setDisplayName("Test")
            .setDeleted(false)
            .setLastAccountUpdate(Instant.now())
            .build()
    }

    private fun accountsSpaceModel(providerId: String, segments: Set<Pair<String, String>>): AccountSpaceModel {
        return AccountSpaceModel.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setProviderId(providerId)
            .setDeleted(false)
            .setNameEn("Test")
            .setNameRu("Test")
            .setDescriptionEn("Test")
            .setDescriptionRu("Test")
            .setOuterKeyInProvider("test")
            .setReadOnly(false)
            .setVersion(0L)
            .setSegments(segments.map { t -> ResourceSegmentSettingsModel(t.first, t.second) }.toSet())
            .build()
    }

}
