package ru.yandex.intranet.d.web.front.aggregation

import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestProviders.YP_ID
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.UnitIds
import ru.yandex.intranet.d.UnitsEnsembleIds
import ru.yandex.intranet.d.dao.accounts.AccountsDao
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao
import ru.yandex.intranet.d.dao.folders.FolderDao
import ru.yandex.intranet.d.dao.providers.ProvidersDao
import ru.yandex.intranet.d.dao.quotas.QuotasDao
import ru.yandex.intranet.d.dao.resources.ResourcesDao
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao
import ru.yandex.intranet.d.dao.services.ServicesDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.providers.FreeProvisionAggregationMode
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.accountModel
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.accountQuotaModel
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.folderModel
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.providerModel
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.quotaModel
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.resourceModel
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.resourceSegmentModel
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.resourceSegmentationModel
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.resourceTypeModel
import ru.yandex.intranet.d.web.front.aggregation.FrontAggregationTestHelper.Companion.serviceModel
import ru.yandex.intranet.d.web.model.ErrorCollectionDto
import ru.yandex.intranet.d.web.model.aggregation.FindServiceTotalsExpandedFilterDto
import ru.yandex.intranet.d.web.model.aggregation.FindServiceTotalsRequestDto
import ru.yandex.intranet.d.web.model.aggregation.FindServiceTotalsRequestFilterDto
import ru.yandex.intranet.d.web.model.aggregation.FindServiceTotalsResponseDto
import ru.yandex.intranet.d.web.model.aggregation.FindServiceTotalsSingleExpandedFilterDto
import ru.yandex.intranet.d.web.model.aggregation.SegmentationAndSegmentsIdsDto
import ru.yandex.intranet.d.web.model.resources.ResourceSelectionListResponseDto
import java.util.*

@IntegrationTest
class FrontAggregationTestWithExpandedFilter(
    @Autowired private val providersDao: ProvidersDao,
    @Autowired private val resourceTypesDao: ResourceTypesDao,
    @Autowired private val resourcesDao: ResourcesDao,
    @Autowired private val servicesDao: ServicesDao,
    @Autowired private val folderDao: FolderDao,
    @Autowired private val quotasDao: QuotasDao,
    @Autowired private val accountsDao: AccountsDao,
    @Autowired private val accountsQuotasDao: AccountsQuotasDao,
    @Autowired private val resourceSegmentationsDao: ResourceSegmentationsDao,
    @Autowired private val resourceSegmentsDao: ResourceSegmentsDao,
    @Autowired private val tableClient: YdbTableClient,
    @Autowired private val webClient: WebTestClient
) {
    @Test
    fun testServiceTotalExpandedFilter(): Unit = runBlocking {
        val root = serviceModel(id = 65535, parentId = null)
        val childOne = serviceModel(id = 65536, root.id)
        val childTwo = serviceModel(id = 65537, root.id)
        val grandchildOne = serviceModel(id = 65538, childOne.id)
        val grandchildTwo = serviceModel(id = 65539, childTwo.id)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                servicesDao.upsertRecipeManyRetryable(
                    txSession,
                    listOf(root, childOne, childTwo, grandchildOne, grandchildTwo)
                )
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
                folderDao.upsertAllRetryable(
                    txSession, listOf(
                        folderRoot, folderChildOne, folderChildTwo,
                        folderGrandchildOne, folderGrandchildTwo
                    )
                ).awaitSingleOrNull()
            }
        }
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                providersDao.upsertProviderRetryable(txSession, provider)
                    .awaitSingleOrNull()
            }
        }
        val resourceTypeOne = resourceTypeModel(provider.id, "test1", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeTwo = resourceTypeModel(provider.id, "test2", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceTypesDao.upsertResourceTypesRetryable(
                    txSession,
                    listOf(resourceTypeOne, resourceTypeTwo)
                )
                    .awaitSingleOrNull()
            }
        }

        val segmentationOne = resourceSegmentationModel(provider.id, "test1")
        val segmentationTwo = resourceSegmentationModel(provider.id, "test2")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceSegmentationsDao.upsertResourceSegmentationsRetryable(
                    txSession,
                    listOf(segmentationOne, segmentationTwo)
                )
                    .awaitSingleOrNull()
            }
        }

        val segmentOneOne = resourceSegmentModel(segmentationOne.id, "test1_1")
        val segmentOneTwo = resourceSegmentModel(segmentationOne.id, "test1_2")
        val segmentTwoOne = resourceSegmentModel(segmentationTwo.id, "test2_1")
        val segmentTwoTwo = resourceSegmentModel(segmentationTwo.id, "test2_2")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceSegmentsDao.upsertResourceSegmentsRetryable(
                    txSession,
                    listOf(segmentOneOne, segmentOneTwo, segmentTwoOne, segmentTwoTwo)
                )
                    .awaitSingleOrNull()
            }
        }

        val resourceOne = resourceModel(
            provider.id, "test1", resourceTypeOne.id,
            mapOf(segmentationOne.id to segmentOneOne.id, segmentationTwo.id to segmentTwoOne.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        val resourceTwo = resourceModel(
            provider.id, "test2", resourceTypeOne.id,
            mapOf(segmentationOne.id to segmentOneOne.id, segmentationTwo.id to segmentTwoTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        val resourceThree = resourceModel(
            provider.id, "test3", resourceTypeTwo.id,
            mapOf(segmentationOne.id to segmentOneTwo.id, segmentationTwo.id to segmentTwoTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        val resourceFour = resourceModel(
            provider.id, "test4", resourceTypeOne.id,
            mapOf(segmentationOne.id to segmentOneTwo.id, segmentationTwo.id to segmentTwoTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourcesDao.upsertResourcesRetryable(
                    txSession,
                    listOf(resourceOne, resourceTwo, resourceThree, resourceFour)
                )
                    .awaitSingleOrNull()
            }
        }

        val accountRoot = accountModel(provider.id, null, folderRoot.id, "test")
        val accountChildOne = accountModel(provider.id, null, folderChildOne.id, "test")
        val accountChildTwo = accountModel(provider.id, null, folderChildTwo.id, "test")
        val accountGrandchildOne = accountModel(provider.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwo = accountModel(provider.id, null, folderGrandchildTwo.id, "test")

        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsDao.upsertAllRetryable(
                    txSession, listOf(
                        accountRoot, accountChildOne, accountChildTwo,
                        accountGrandchildOne, accountGrandchildTwo
                    )
                ).awaitSingleOrNull()
            }
        }

        val quotaRootResourceOne = quotaModel(provider.id, resourceOne.id, folderRoot.id, 10L, 5L)
        val quotaChildOneResourceOne = quotaModel(provider.id, resourceOne.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwoResourceOne = quotaModel(provider.id, resourceOne.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOneResourceOne = quotaModel(provider.id, resourceOne.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwoResourceOne = quotaModel(provider.id, resourceOne.id, folderGrandchildTwo.id, 60L, 30L)

        val quotaRootResourceTwo = quotaModel(provider.id, resourceTwo.id, folderRoot.id, 10L, 5L)
        val quotaChildOneResourceTwo = quotaModel(provider.id, resourceTwo.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwoResourceTwo = quotaModel(provider.id, resourceTwo.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOneResourceTwo = quotaModel(provider.id, resourceTwo.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwoResourceTwo = quotaModel(provider.id, resourceTwo.id, folderGrandchildTwo.id, 60L, 30L)

        val quotaRootResourceThree = quotaModel(provider.id, resourceThree.id, folderRoot.id, 10L, 5L)
        val quotaChildOneResourceThree = quotaModel(provider.id, resourceThree.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwoResourceThree = quotaModel(provider.id, resourceThree.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOneResourceThree =
            quotaModel(provider.id, resourceThree.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwoResourceThree =
            quotaModel(provider.id, resourceThree.id, folderGrandchildTwo.id, 60L, 30L)

        val quotaRootResourceFour = quotaModel(provider.id, resourceFour.id, folderRoot.id, 10L, 5L)
        val quotaChildOneResourceFour = quotaModel(provider.id, resourceFour.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwoResourceFour = quotaModel(provider.id, resourceFour.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOneResourceFour = quotaModel(provider.id, resourceFour.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwoResourceFour = quotaModel(provider.id, resourceFour.id, folderGrandchildTwo.id, 60L, 30L)

        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                quotasDao.upsertAllRetryable(
                    txSession,
                    listOf(
                        quotaRootResourceOne, quotaChildOneResourceOne, quotaChildTwoResourceOne,
                        quotaGrandchildOneResourceOne, quotaGrandchildTwoResourceOne,
                        quotaRootResourceTwo, quotaChildOneResourceTwo, quotaChildTwoResourceTwo,
                        quotaGrandchildOneResourceTwo, quotaGrandchildTwoResourceTwo,
                        quotaRootResourceThree, quotaChildOneResourceThree, quotaChildTwoResourceThree,
                        quotaGrandchildOneResourceThree, quotaGrandchildTwoResourceThree,
                        quotaRootResourceFour, quotaChildOneResourceFour, quotaChildTwoResourceFour,
                        quotaGrandchildOneResourceFour, quotaGrandchildTwoResourceFour
                    )
                ).awaitSingleOrNull()
            }
        }

        val provisionRootResourceOne =
            accountQuotaModel(provider.id, resourceOne.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOneResourceOne = accountQuotaModel(
            provider.id, resourceOne.id, folderChildOne.id, accountChildOne.id,
            15L, 6L
        )
        val provisionChildTwoResourceOne = accountQuotaModel(
            provider.id, resourceOne.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L
        )
        val provisionGrandchildOneResourceOne = accountQuotaModel(
            provider.id, resourceOne.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L
        )
        val provisionGrandchildTwoResourceOne = accountQuotaModel(
            provider.id, resourceOne.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L
        )

        val provisionRootResourceTwo =
            accountQuotaModel(provider.id, resourceTwo.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOneResourceTwo = accountQuotaModel(
            provider.id, resourceTwo.id, folderChildOne.id, accountChildOne.id,
            15L, 6L
        )
        val provisionChildTwoResourceTwo = accountQuotaModel(
            provider.id, resourceTwo.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L
        )
        val provisionGrandchildOneResourceTwo = accountQuotaModel(
            provider.id, resourceTwo.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L
        )
        val provisionGrandchildTwoResourceTwo = accountQuotaModel(
            provider.id, resourceTwo.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L
        )

        val provisionRootResourceThree =
            accountQuotaModel(provider.id, resourceThree.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOneResourceThree = accountQuotaModel(
            provider.id, resourceThree.id, folderChildOne.id, accountChildOne.id,
            15L, 6L
        )
        val provisionChildTwoResourceThree = accountQuotaModel(
            provider.id, resourceThree.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L
        )
        val provisionGrandchildOneResourceThree = accountQuotaModel(
            provider.id, resourceThree.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L
        )
        val provisionGrandchildTwoResourceThree = accountQuotaModel(
            provider.id, resourceThree.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L
        )

        val provisionRootResourceFour =
            accountQuotaModel(provider.id, resourceFour.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOneResourceFour = accountQuotaModel(
            provider.id, resourceFour.id, folderChildOne.id, accountChildOne.id,
            15L, 6L
        )
        val provisionChildTwoResourceFour = accountQuotaModel(
            provider.id, resourceFour.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L
        )
        val provisionGrandchildOneResourceFour = accountQuotaModel(
            provider.id, resourceFour.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L
        )
        val provisionGrandchildTwoResourceFour = accountQuotaModel(
            provider.id, resourceFour.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L
        )


        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsQuotasDao.upsertAllRetryable(
                    txSession,
                    listOf(
                        provisionRootResourceOne, provisionChildOneResourceOne, provisionChildTwoResourceOne,
                        provisionGrandchildOneResourceOne, provisionGrandchildTwoResourceOne,
                        provisionRootResourceTwo, provisionChildOneResourceTwo, provisionChildTwoResourceTwo,
                        provisionGrandchildOneResourceTwo, provisionGrandchildTwoResourceTwo,
                        provisionRootResourceThree, provisionChildOneResourceThree, provisionChildTwoResourceThree,
                        provisionGrandchildOneResourceThree, provisionGrandchildTwoResourceThree,
                        provisionRootResourceFour, provisionChildOneResourceFour, provisionChildTwoResourceFour,
                        provisionGrandchildOneResourceFour, provisionGrandchildTwoResourceFour
                    )
                ).awaitSingleOrNull()
            }
        }
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent

        val resultOne = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(
                FindServiceTotalsRequestDto(
                    root.id, null,
                    FindServiceTotalsExpandedFilterDto(
                        setOf(
                            FindServiceTotalsSingleExpandedFilterDto(
                                provider.id,
                                setOf(SegmentationAndSegmentsIdsDto(segmentationOne.id, setOf(segmentOneOne.id))),
                                resourceTypeIds = null
                            ),
                            FindServiceTotalsSingleExpandedFilterDto(
                                provider.id,
                                segments = null,
                                setOf(resourceTypeTwo.id)
                            )
                        )
                    )
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()

        var resourceTypes = resultOne.aggregates[0].providers[0].resourceTypes
        assertEquals(2, resourceTypes.size)
        assertTrue(resourceTypes.any { it.resourceTypeId == resourceTypeOne.id })
        assertTrue(resourceTypes.any { it.resourceTypeId == resourceTypeTwo.id })

        var resourcesTypeOne = resourceTypes.first { it.resourceTypeId == resourceTypeOne.id }.resources
        val resourcesTypeTwo = resourceTypes.first { it.resourceTypeId == resourceTypeTwo.id }.resources

        assertEquals(2, resourcesTypeOne.size)
        assertTrue(resourcesTypeOne.any { it.resourceId == resourceOne.id })
        assertTrue(resourcesTypeOne.any { it.resourceId == resourceTwo.id })

        assertEquals(1, resourcesTypeTwo.size)
        assertTrue(resourcesTypeTwo.any { it.resourceId == resourceThree.id })

        val resultTwo = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(
                FindServiceTotalsRequestDto(
                    root.id, null,
                    FindServiceTotalsExpandedFilterDto(
                        setOf(
                            FindServiceTotalsSingleExpandedFilterDto(
                                provider.id,
                                setOf(
                                    SegmentationAndSegmentsIdsDto(
                                        segmentationOne.id,
                                        setOf(segmentOneOne.id, segmentOneTwo.id)
                                    ),
                                    SegmentationAndSegmentsIdsDto(
                                        segmentationTwo.id,
                                        setOf(segmentTwoTwo.id)
                                    )
                                ),
                                setOf(resourceTypeOne.id)
                            )
                        )
                    )
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()

        resourceTypes = resultTwo.aggregates[0].providers[0].resourceTypes
        assertEquals(1, resourceTypes.size)
        assertTrue(resourceTypes.any { it.resourceTypeId == resourceTypeOne.id })

        resourcesTypeOne = resourceTypes.first { it.resourceTypeId == resourceTypeOne.id }.resources

        assertEquals(2, resourcesTypeOne.size)
        assertTrue(resourcesTypeOne.any { it.resourceId == resourceTwo.id })
        assertTrue(resourcesTypeOne.any { it.resourceId == resourceFour.id })
    }

    @Test
    fun testServiceTotalEmptyExpandedFilter(): Unit = runBlocking {
        val root = serviceModel(id = 65535, parentId = null)
        val childOne = serviceModel(id = 65536, root.id)
        val childTwo = serviceModel(id = 65537, root.id)
        val grandchildOne = serviceModel(id = 65538, childOne.id)
        val grandchildTwo = serviceModel(id = 65539, childTwo.id)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                servicesDao.upsertRecipeManyRetryable(
                    txSession,
                    listOf(root, childOne, childTwo, grandchildOne, grandchildTwo)
                )
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
                folderDao.upsertAllRetryable(
                    txSession, listOf(
                        folderRoot, folderChildOne, folderChildTwo,
                        folderGrandchildOne, folderGrandchildTwo
                    )
                ).awaitSingleOrNull()
            }
        }
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                providersDao.upsertProviderRetryable(txSession, provider)
                    .awaitSingleOrNull()
            }
        }
        val resourceType = resourceTypeModel(provider.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
                    .awaitSingleOrNull()
            }
        }

        val segmentationOne = resourceSegmentationModel(provider.id, "test1")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceSegmentationsDao
                    .upsertResourceSegmentationRetryable(txSession, segmentationOne)
                    .awaitSingleOrNull()
            }
        }

        val segmentOneOne = resourceSegmentModel(segmentationOne.id, "test1_1")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceSegmentsDao.upsertResourceSegmentRetryable(
                    txSession,
                    segmentOneOne
                )
                    .awaitSingleOrNull()
            }
        }

        val resource = resourceModel(
            provider.id, "test", resourceType.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourcesDao.upsertResourceRetryable(txSession, resource)
                    .awaitSingleOrNull()
            }
        }
        val accountRoot = accountModel(provider.id, null, folderRoot.id, "test")
        val accountChildOne = accountModel(provider.id, null, folderChildOne.id, "test")
        val accountChildTwo = accountModel(provider.id, null, folderChildTwo.id, "test")
        val accountGrandchildOne = accountModel(provider.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwo = accountModel(provider.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsDao.upsertAllRetryable(
                    txSession, listOf(
                        accountRoot, accountChildOne, accountChildTwo,
                        accountGrandchildOne, accountGrandchildTwo
                    )
                ).awaitSingleOrNull()
            }
        }
        val quotaRoot = quotaModel(provider.id, resource.id, folderRoot.id, 10L, 5L)
        val quotaChildOne = quotaModel(provider.id, resource.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwo = quotaModel(provider.id, resource.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOne = quotaModel(provider.id, resource.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwo = quotaModel(provider.id, resource.id, folderGrandchildTwo.id, 60L, 30L)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                quotasDao.upsertAllRetryable(
                    txSession, listOf(
                        quotaRoot, quotaChildOne, quotaChildTwo,
                        quotaGrandchildOne, quotaGrandchildTwo
                    )
                ).awaitSingleOrNull()
            }
        }
        val provisionRoot = accountQuotaModel(provider.id, resource.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOne = accountQuotaModel(
            provider.id, resource.id, folderChildOne.id, accountChildOne.id,
            15L, 6L
        )
        val provisionChildTwo = accountQuotaModel(
            provider.id, resource.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L
        )
        val provisionGrandchildOne = accountQuotaModel(
            provider.id, resource.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L
        )
        val provisionGrandchildTwo = accountQuotaModel(
            provider.id, resource.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsQuotasDao.upsertAllRetryable(
                    txSession, listOf(
                        provisionRoot, provisionChildOne, provisionChildTwo,
                        provisionGrandchildOne, provisionGrandchildTwo
                    )
                ).awaitSingleOrNull()
            }
        }
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent

        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(
                FindServiceTotalsRequestDto(
                    root.id, filter = null, FindServiceTotalsExpandedFilterDto(setOf())
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is4xxClientError
            .returnResult(ErrorCollectionDto::class.java)
            .responseBody.awaitSingle()

        assertTrue(result.fieldErrors.containsKey("expandedFilter.singleFilter"))
        assertEquals(1, result.fieldErrors["expandedFilter.singleFilter"]?.size)
        assertTrue(result.fieldErrors["expandedFilter.singleFilter"]!!.contains("Field is required."))
    }

    @Test
    fun testServiceTotalExpandedFilterEmptyResult(): Unit = runBlocking {
        val root = serviceModel(id = 65535, parentId = null)
        val childOne = serviceModel(id = 65536, root.id)
        val childTwo = serviceModel(id = 65537, root.id)
        val grandchildOne = serviceModel(id = 65538, childOne.id)
        val grandchildTwo = serviceModel(id = 65539, childTwo.id)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                servicesDao.upsertRecipeManyRetryable(
                    txSession,
                    listOf(root, childOne, childTwo, grandchildOne, grandchildTwo)
                )
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
                folderDao.upsertAllRetryable(
                    txSession, listOf(
                        folderRoot, folderChildOne, folderChildTwo,
                        folderGrandchildOne, folderGrandchildTwo
                    )
                ).awaitSingleOrNull()
            }
        }
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                providersDao.upsertProviderRetryable(txSession, provider)
                    .awaitSingleOrNull()
            }
        }
        val resourceType = resourceTypeModel(provider.id, "test", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
                    .awaitSingleOrNull()
            }
        }

        val segmentationOne = resourceSegmentationModel(provider.id, "test1")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceSegmentationsDao
                    .upsertResourceSegmentationRetryable(txSession, segmentationOne)
                    .awaitSingleOrNull()
            }
        }

        val segmentOneOne = resourceSegmentModel(segmentationOne.id, "test1_1")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceSegmentsDao.upsertResourceSegmentRetryable(
                    txSession,
                    segmentOneOne
                )
                    .awaitSingleOrNull()
            }
        }

        val resource = resourceModel(
            provider.id, "test", resourceType.id, emptyMap(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourcesDao.upsertResourceRetryable(txSession, resource)
                    .awaitSingleOrNull()
            }
        }
        val accountRoot = accountModel(provider.id, null, folderRoot.id, "test")
        val accountChildOne = accountModel(provider.id, null, folderChildOne.id, "test")
        val accountChildTwo = accountModel(provider.id, null, folderChildTwo.id, "test")
        val accountGrandchildOne = accountModel(provider.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwo = accountModel(provider.id, null, folderGrandchildTwo.id, "test")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsDao.upsertAllRetryable(
                    txSession, listOf(
                        accountRoot, accountChildOne, accountChildTwo,
                        accountGrandchildOne, accountGrandchildTwo
                    )
                ).awaitSingleOrNull()
            }
        }
        val quotaRoot = quotaModel(provider.id, resource.id, folderRoot.id, 10L, 5L)
        val quotaChildOne = quotaModel(provider.id, resource.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwo = quotaModel(provider.id, resource.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOne = quotaModel(provider.id, resource.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwo = quotaModel(provider.id, resource.id, folderGrandchildTwo.id, 60L, 30L)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                quotasDao.upsertAllRetryable(
                    txSession, listOf(
                        quotaRoot, quotaChildOne, quotaChildTwo,
                        quotaGrandchildOne, quotaGrandchildTwo
                    )
                ).awaitSingleOrNull()
            }
        }
        val provisionRoot = accountQuotaModel(provider.id, resource.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOne = accountQuotaModel(
            provider.id, resource.id, folderChildOne.id, accountChildOne.id,
            15L, 6L
        )
        val provisionChildTwo = accountQuotaModel(
            provider.id, resource.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L
        )
        val provisionGrandchildOne = accountQuotaModel(
            provider.id, resource.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L
        )
        val provisionGrandchildTwo = accountQuotaModel(
            provider.id, resource.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsQuotasDao.upsertAllRetryable(
                    txSession, listOf(
                        provisionRoot, provisionChildOne, provisionChildTwo,
                        provisionGrandchildOne, provisionGrandchildTwo
                    )
                ).awaitSingleOrNull()
            }
        }
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent

        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(
                FindServiceTotalsRequestDto(
                    root.id, filter = null, expandedFilter =
                    FindServiceTotalsExpandedFilterDto(
                        setOf(
                            FindServiceTotalsSingleExpandedFilterDto(
                                provider.id,
                                setOf(
                                    SegmentationAndSegmentsIdsDto(
                                        segmentationOne.id,
                                        setOf(segmentOneOne.id)
                                    )
                                ),
                                resourceTypeIds = null
                            )
                        )
                    )
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()

        assertTrue(result.aggregates[0].providers.isEmpty())
    }

    @Test
    fun testServiceTotalExpandedFilterMultipleProviders(): Unit = runBlocking {
        val root = serviceModel(id = 65535, parentId = null)
        val childOne = serviceModel(id = 65536, root.id)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                servicesDao.upsertRecipeManyRetryable(txSession, listOf(root, childOne))
                    .awaitSingleOrNull()
            }
        }
        val folderRoot = folderModel(root.id)
        val folderChildOne = folderModel(childOne.id)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                folderDao.upsertAllRetryable(txSession, listOf(folderRoot, folderChildOne)).awaitSingleOrNull()
            }
        }
        val providerOne = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                providersDao.upsertProviderRetryable(txSession, providerOne)
                    .awaitSingleOrNull()
            }
        }
        val providerTwo = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                providersDao.upsertProviderRetryable(txSession, providerTwo)
                    .awaitSingleOrNull()
            }
        }
        val resourceTypeOne = resourceTypeModel(providerOne.id, "test1", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeTwo = resourceTypeModel(providerTwo.id, "test2", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceTypesDao.upsertResourceTypesRetryable(
                    txSession,
                    listOf(resourceTypeOne, resourceTypeTwo)
                )
                    .awaitSingleOrNull()
            }
        }

        val segmentationOne = resourceSegmentationModel(providerOne.id, "test1")
        val segmentationTwo = resourceSegmentationModel(providerTwo.id, "test2")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceSegmentationsDao.upsertResourceSegmentationsRetryable(
                    txSession,
                    listOf(segmentationOne, segmentationTwo)
                )
                    .awaitSingleOrNull()
            }
        }

        val segmentOne = resourceSegmentModel(segmentationOne.id, "test1_1")
        val segmentTwo = resourceSegmentModel(segmentationTwo.id, "test1_2")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceSegmentsDao.upsertResourceSegmentsRetryable(
                    txSession,
                    listOf(segmentOne, segmentTwo)
                )
                    .awaitSingleOrNull()
            }
        }

        val resourceOne = resourceModel(
            providerOne.id, "test1", resourceTypeOne.id,
            mapOf(segmentationOne.id to segmentOne.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        val resourceTwo = resourceModel(
            providerOne.id, "test2", resourceTypeOne.id, segments = mapOf(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        val resourceThree = resourceModel(
            providerTwo.id, "test3", resourceTypeTwo.id,
            mapOf(segmentationTwo.id to segmentTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        val resourceFour = resourceModel(
            providerTwo.id, "test4", resourceTypeTwo.id, segments = mapOf(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourcesDao.upsertResourcesRetryable(
                    txSession,
                    listOf(resourceOne, resourceTwo, resourceThree, resourceFour)
                )
                    .awaitSingleOrNull()
            }
        }


        val accountRootOfProviderOne = accountModel(providerOne.id, null, folderRoot.id, "test")
        val accountChildOneOfProviderOne = accountModel(providerOne.id, null, folderChildOne.id, "test")

        val accountRootOfProviderTwo = accountModel(providerTwo.id, null, folderRoot.id, "test")
        val accountChildOneOfProviderTwo = accountModel(providerTwo.id, null, folderChildOne.id, "test")

        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsDao.upsertAllRetryable(
                    txSession,
                    listOf(
                        accountRootOfProviderOne,
                        accountChildOneOfProviderOne,
                        accountRootOfProviderTwo,
                        accountChildOneOfProviderTwo
                    )
                ).awaitSingleOrNull()
            }
        }

        val quotaRootResourceOne = quotaModel(providerOne.id, resourceOne.id, folderRoot.id, 10L, 5L)
        val quotaChildOneResourceOne = quotaModel(providerOne.id, resourceOne.id, folderChildOne.id, 30L, 15L)

        val quotaRootResourceTwo = quotaModel(providerOne.id, resourceTwo.id, folderRoot.id, 10L, 5L)
        val quotaChildOneResourceTwo = quotaModel(providerOne.id, resourceTwo.id, folderChildOne.id, 30L, 15L)

        val quotaRootResourceThree = quotaModel(providerTwo.id, resourceThree.id, folderRoot.id, 10L, 5L)
        val quotaChildOneResourceThree = quotaModel(providerTwo.id, resourceThree.id, folderChildOne.id, 30L, 15L)

        val quotaRootResourceFour = quotaModel(providerTwo.id, resourceFour.id, folderRoot.id, 10L, 5L)
        val quotaChildOneResourceFour = quotaModel(providerTwo.id, resourceFour.id, folderChildOne.id, 30L, 15L)

        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                quotasDao.upsertAllRetryable(
                    txSession,
                    listOf(
                        quotaRootResourceOne, quotaChildOneResourceOne,
                        quotaRootResourceTwo, quotaChildOneResourceTwo,
                        quotaRootResourceThree, quotaChildOneResourceThree,
                        quotaRootResourceFour, quotaChildOneResourceFour,
                    )
                ).awaitSingleOrNull()
            }
        }

        val provisionRootResourceOne =
            accountQuotaModel(providerOne.id, resourceOne.id, folderRoot.id, accountRootOfProviderOne.id, 5L, 2L)
        val provisionChildOneResourceOne = accountQuotaModel(
            providerOne.id, resourceOne.id, folderChildOne.id, accountChildOneOfProviderOne.id,
            15L, 6L
        )
        val provisionRootResourceTwo =
            accountQuotaModel(providerOne.id, resourceTwo.id, folderRoot.id, accountRootOfProviderOne.id, 5L, 2L)
        val provisionChildOneResourceTwo = accountQuotaModel(
            providerOne.id, resourceTwo.id, folderChildOne.id, accountChildOneOfProviderOne.id,
            15L, 6L
        )


        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsQuotasDao.upsertAllRetryable(
                    txSession,
                    listOf(
                        provisionRootResourceOne, provisionChildOneResourceOne,
                        provisionRootResourceTwo, provisionChildOneResourceTwo,
                    )
                ).awaitSingleOrNull()
            }
        }

        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", providerOne.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent

        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", providerTwo.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent

        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(
                FindServiceTotalsRequestDto(
                    root.id,
                    filter = null,
                    FindServiceTotalsExpandedFilterDto(
                        setOf(
                            FindServiceTotalsSingleExpandedFilterDto(
                                providerOne.id,
                                setOf(
                                    SegmentationAndSegmentsIdsDto(
                                        segmentationOne.id,
                                        setOf(segmentOne.id)
                                    )
                                ),
                                resourceTypeIds = null
                            ),
                            FindServiceTotalsSingleExpandedFilterDto(
                                providerTwo.id,
                                setOf(
                                    SegmentationAndSegmentsIdsDto(
                                        segmentationTwo.id,
                                        setOf(segmentTwo.id)
                                    )
                                ),
                                resourceTypeIds = null
                            ),
                            FindServiceTotalsSingleExpandedFilterDto(
                                providerTwo.id,
                                setOf(
                                    SegmentationAndSegmentsIdsDto(
                                        segmentationTwo.id,
                                        setOf(segmentTwo.id)
                                    )
                                ),
                                resourceTypeIds = setOf(resourceTypeTwo.id)
                            )
                        )
                    )
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(FindServiceTotalsResponseDto::class.java)
            .responseBody.awaitSingle()

        assertEquals(2, result.aggregates[0].providers.size)
        assertTrue(result.aggregates[0].providers.any { it.providerId == providerOne.id })
        assertTrue(result.aggregates[0].providers.any { it.providerId == providerTwo.id })
        assertTrue(result.aggregates[0].providers.all { it.resourceTypes.size == 1 })
        assertEquals(
            resourceTypeOne.id,
            result.aggregates[0].providers.first { it.providerId == providerOne.id }.resourceTypes[0].resourceTypeId
        )
        assertEquals(
            1,
            result.aggregates[0].providers
                .first { it.providerId == providerOne.id }.resourceTypes[0].resources.size
        )
        assertEquals(
            resourceOne.id,
            result.aggregates[0].providers
                .first { it.providerId == providerOne.id }.resourceTypes[0].resources[0].resourceId
        )
        assertEquals(
            resourceTypeTwo.id,
            result.aggregates[0].providers.first { it.providerId == providerTwo.id }.resourceTypes[0].resourceTypeId
        )
        assertEquals(
            1,
            result.aggregates[0].providers
                .first { it.providerId == providerTwo.id }.resourceTypes[0].resources.size
        )
        assertEquals(
            resourceThree.id,
            result.aggregates[0].providers
                .first { it.providerId == providerTwo.id }.resourceTypes[0].resources[0].resourceId
        )
    }

    @Test
    fun testServiceTotalExpandedFilterWithoutProvider(): Unit = runBlocking {
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(
                FindServiceTotalsRequestDto(
                    1,
                    filter = null,
                    FindServiceTotalsExpandedFilterDto(
                        setOf(
                            FindServiceTotalsSingleExpandedFilterDto(
                                null,
                                setOf(),
                                resourceTypeIds = null
                            )
                        )
                    )
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is4xxClientError
            .returnResult(ErrorCollectionDto::class.java)
            .responseBody.awaitSingle()

        assertTrue(result.fieldErrors["expandedFilter.providerId"]?.contains("Field is required.") ?: false)
    }

    @Test
    fun testServiceTotalExpandedFilterWithNullSegments(): Unit = runBlocking {
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(
                FindServiceTotalsRequestDto(
                    1,
                    filter = null,
                    FindServiceTotalsExpandedFilterDto(
                        setOf(
                            FindServiceTotalsSingleExpandedFilterDto(
                                providerId = UUID.randomUUID().toString(),
                                setOf(
                                    SegmentationAndSegmentsIdsDto(
                                        segmentationId = null,
                                        setOf(UUID.randomUUID().toString(), null)
                                    ),
                                    null
                                ),
                                resourceTypeIds = null
                            )
                        )
                    )
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is4xxClientError
            .returnResult(ErrorCollectionDto::class.java)
            .responseBody.awaitSingle()

        assertTrue(
            result.fieldErrors["expandedFilter.segments"]?.contains("Field is required.") ?: false
        )
        assertTrue(
            result.fieldErrors["expandedFilter.segments.segmentationId"]?.contains("Field is required.") ?: false
        )
        assertTrue(
            result.fieldErrors["expandedFilter.segments.segmentId"]?.contains("Field is required.") ?: false
        )
    }

    @Test
    fun testServiceTotalWithCommonAndExpandedFilters(): Unit = runBlocking {
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/front/aggregation/_findServiceTotals")
            .bodyValue(
                FindServiceTotalsRequestDto(
                    1,
                    FindServiceTotalsRequestFilterDto(null, null),
                    FindServiceTotalsExpandedFilterDto(setOf())
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is4xxClientError
            .returnResult(ErrorCollectionDto::class.java)
            .responseBody.awaitSingle()

        assertTrue(
            result.fieldErrors["filter"]?.contains("Both expanded and common filters are not allowed in a single request.")
                ?: false
        )
    }

    @Test
    fun resourceSelectionTreeTest(): Unit = runBlocking {
        val root = serviceModel(id = 65535, parentId = null)
        val childOne = serviceModel(id = 65536, root.id)
        val childTwo = serviceModel(id = 65537, root.id)
        val grandchildOne = serviceModel(id = 65538, childOne.id)
        val grandchildTwo = serviceModel(id = 65539, childTwo.id)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                servicesDao.upsertRecipeManyRetryable(
                    txSession,
                    listOf(root, childOne, childTwo, grandchildOne, grandchildTwo)
                )
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
                folderDao.upsertAllRetryable(
                    txSession, listOf(
                        folderRoot, folderChildOne, folderChildTwo,
                        folderGrandchildOne, folderGrandchildTwo
                    )
                ).awaitSingleOrNull()
            }
        }
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                providersDao.upsertProviderRetryable(txSession, provider)
                    .awaitSingleOrNull()
            }
        }
        val resourceTypeOne = resourceTypeModel(provider.id, "test1", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeTwo = resourceTypeModel(provider.id, "test2", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceTypesDao.upsertResourceTypesRetryable(
                    txSession,
                    listOf(resourceTypeOne, resourceTypeTwo)
                )
                    .awaitSingleOrNull()
            }
        }

        val segmentationOne = resourceSegmentationModel(provider.id, key = "test1", choiceOrder = 0)
        val segmentationTwo = resourceSegmentationModel(provider.id, key = "test2", choiceOrder = 1)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceSegmentationsDao.upsertResourceSegmentationsRetryable(
                    txSession,
                    listOf(segmentationOne, segmentationTwo)
                )
                    .awaitSingleOrNull()
            }
        }

        val segmentOneOne = resourceSegmentModel(segmentationOne.id, "test1_1")
        val segmentOneTwo = resourceSegmentModel(segmentationOne.id, "test1_2")
        val segmentTwoOne = resourceSegmentModel(segmentationTwo.id, "test2_1")
        val segmentTwoTwo = resourceSegmentModel(segmentationTwo.id, "test2_2")
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceSegmentsDao.upsertResourceSegmentsRetryable(
                    txSession,
                    listOf(segmentOneOne, segmentOneTwo, segmentTwoOne, segmentTwoTwo)
                )
                    .awaitSingleOrNull()
            }
        }

        val resourceOne = resourceModel(
            provider.id, "test1", resourceTypeOne.id,
            mapOf(segmentationOne.id to segmentOneOne.id, segmentationTwo.id to segmentTwoOne.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        val resourceTwo = resourceModel(
            provider.id, "test2", resourceTypeOne.id,
            mapOf(segmentationOne.id to segmentOneOne.id, segmentationTwo.id to segmentTwoTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        val resourceThree = resourceModel(
            provider.id, "test3", resourceTypeTwo.id,
            mapOf(segmentationOne.id to segmentOneTwo.id, segmentationTwo.id to segmentTwoTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        val resourceFour = resourceModel(
            provider.id, "test4", resourceTypeOne.id,
            mapOf(segmentationOne.id to segmentOneTwo.id, segmentationTwo.id to segmentTwoTwo.id),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourcesDao.upsertResourcesRetryable(
                    txSession,
                    listOf(resourceOne, resourceTwo, resourceThree, resourceFour)
                )
                    .awaitSingleOrNull()
            }
        }

        val accountRoot = accountModel(provider.id, null, folderRoot.id, "test")
        val accountChildOne = accountModel(provider.id, null, folderChildOne.id, "test")
        val accountChildTwo = accountModel(provider.id, null, folderChildTwo.id, "test")
        val accountGrandchildOne = accountModel(provider.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwo = accountModel(provider.id, null, folderGrandchildTwo.id, "test")

        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsDao.upsertAllRetryable(
                    txSession, listOf(
                        accountRoot, accountChildOne, accountChildTwo,
                        accountGrandchildOne, accountGrandchildTwo
                    )
                ).awaitSingleOrNull()
            }
        }

        val quotaRootResourceOne = quotaModel(provider.id, resourceOne.id, folderRoot.id, 10L, 5L)
        val quotaChildOneResourceOne = quotaModel(provider.id, resourceOne.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwoResourceOne = quotaModel(provider.id, resourceOne.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOneResourceOne = quotaModel(provider.id, resourceOne.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwoResourceOne = quotaModel(provider.id, resourceOne.id, folderGrandchildTwo.id, 60L, 30L)

        val quotaRootResourceTwo = quotaModel(provider.id, resourceTwo.id, folderRoot.id, 10L, 5L)
        val quotaChildOneResourceTwo = quotaModel(provider.id, resourceTwo.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwoResourceTwo = quotaModel(provider.id, resourceTwo.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOneResourceTwo = quotaModel(provider.id, resourceTwo.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwoResourceTwo = quotaModel(provider.id, resourceTwo.id, folderGrandchildTwo.id, 60L, 30L)

        val quotaRootResourceThree = quotaModel(provider.id, resourceThree.id, folderRoot.id, 10L, 5L)
        val quotaChildOneResourceThree = quotaModel(provider.id, resourceThree.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwoResourceThree = quotaModel(provider.id, resourceThree.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOneResourceThree =
            quotaModel(provider.id, resourceThree.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwoResourceThree =
            quotaModel(provider.id, resourceThree.id, folderGrandchildTwo.id, 60L, 30L)

        val quotaRootResourceFour = quotaModel(provider.id, resourceFour.id, folderRoot.id, 10L, 5L)
        val quotaChildOneResourceFour = quotaModel(provider.id, resourceFour.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwoResourceFour = quotaModel(provider.id, resourceFour.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOneResourceFour = quotaModel(provider.id, resourceFour.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwoResourceFour = quotaModel(provider.id, resourceFour.id, folderGrandchildTwo.id, 60L, 30L)

        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                quotasDao.upsertAllRetryable(
                    txSession,
                    listOf(
                        quotaRootResourceOne, quotaChildOneResourceOne, quotaChildTwoResourceOne,
                        quotaGrandchildOneResourceOne, quotaGrandchildTwoResourceOne,
                        quotaRootResourceTwo, quotaChildOneResourceTwo, quotaChildTwoResourceTwo,
                        quotaGrandchildOneResourceTwo, quotaGrandchildTwoResourceTwo,
                        quotaRootResourceThree, quotaChildOneResourceThree, quotaChildTwoResourceThree,
                        quotaGrandchildOneResourceThree, quotaGrandchildTwoResourceThree,
                        quotaRootResourceFour, quotaChildOneResourceFour, quotaChildTwoResourceFour,
                        quotaGrandchildOneResourceFour, quotaGrandchildTwoResourceFour
                    )
                ).awaitSingleOrNull()
            }
        }

        val provisionRootResourceOne =
            accountQuotaModel(provider.id, resourceOne.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOneResourceOne = accountQuotaModel(
            provider.id, resourceOne.id, folderChildOne.id, accountChildOne.id,
            15L, 6L
        )
        val provisionChildTwoResourceOne = accountQuotaModel(
            provider.id, resourceOne.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L
        )
        val provisionGrandchildOneResourceOne = accountQuotaModel(
            provider.id, resourceOne.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L
        )
        val provisionGrandchildTwoResourceOne = accountQuotaModel(
            provider.id, resourceOne.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L
        )

        val provisionRootResourceTwo =
            accountQuotaModel(provider.id, resourceTwo.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOneResourceTwo = accountQuotaModel(
            provider.id, resourceTwo.id, folderChildOne.id, accountChildOne.id,
            15L, 6L
        )
        val provisionChildTwoResourceTwo = accountQuotaModel(
            provider.id, resourceTwo.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L
        )
        val provisionGrandchildOneResourceTwo = accountQuotaModel(
            provider.id, resourceTwo.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L
        )
        val provisionGrandchildTwoResourceTwo = accountQuotaModel(
            provider.id, resourceTwo.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L
        )

        val provisionRootResourceThree =
            accountQuotaModel(provider.id, resourceThree.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOneResourceThree = accountQuotaModel(
            provider.id, resourceThree.id, folderChildOne.id, accountChildOne.id,
            15L, 6L
        )
        val provisionChildTwoResourceThree = accountQuotaModel(
            provider.id, resourceThree.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L
        )
        val provisionGrandchildOneResourceThree = accountQuotaModel(
            provider.id, resourceThree.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L
        )
        val provisionGrandchildTwoResourceThree = accountQuotaModel(
            provider.id, resourceThree.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L
        )

        val provisionRootResourceFour =
            accountQuotaModel(provider.id, resourceFour.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOneResourceFour = accountQuotaModel(
            provider.id, resourceFour.id, folderChildOne.id, accountChildOne.id,
            15L, 6L
        )
        val provisionChildTwoResourceFour = accountQuotaModel(
            provider.id, resourceFour.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L
        )
        val provisionGrandchildOneResourceFour = accountQuotaModel(
            provider.id, resourceFour.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L
        )
        val provisionGrandchildTwoResourceFour = accountQuotaModel(
            provider.id, resourceFour.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L
        )


        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsQuotasDao.upsertAllRetryable(
                    txSession,
                    listOf(
                        provisionRootResourceOne, provisionChildOneResourceOne, provisionChildTwoResourceOne,
                        provisionGrandchildOneResourceOne, provisionGrandchildTwoResourceOne,
                        provisionRootResourceTwo, provisionChildOneResourceTwo, provisionChildTwoResourceTwo,
                        provisionGrandchildOneResourceTwo, provisionGrandchildTwoResourceTwo,
                        provisionRootResourceThree, provisionChildOneResourceThree, provisionChildTwoResourceThree,
                        provisionGrandchildOneResourceThree, provisionGrandchildTwoResourceThree,
                        provisionRootResourceFour, provisionChildOneResourceFour, provisionChildTwoResourceFour,
                        provisionGrandchildOneResourceFour, provisionGrandchildTwoResourceFour
                    )
                ).awaitSingleOrNull()
            }
        }

        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent

        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/aggregation/_resourceSelectionList?providerId=${provider.id}&rootServiceId=${root.id}")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(ResourceSelectionListResponseDto::class.java)
            .responseBody
            .awaitSingle()

        assertEquals(2, result.segmentations.size)
        assertEquals(segmentationOne.id, result.segmentations[0].id)
        assertEquals(2, result.segmentations[0].segments.size)
        assertEquals(segmentationTwo.id, result.segmentations[1].id)
        assertEquals(2, result.segmentations[1].segments.size)

        assertTrue(result.segmentations[0].segments.any { s -> s.id == segmentOneOne.id })
        assertTrue(result.segmentations[0].segments.any { s -> s.id == segmentOneTwo.id })
        val segmentOneOneResponse = result.segmentations[0].segments.first { s -> s.id == segmentOneOne.id }
        val segmentOneTwoResponse = result.segmentations[0].segments.first { s -> s.id == segmentOneTwo.id }
        assertEquals(1, segmentOneOneResponse.resourceIdsByResourceTypeIds.size)
        assertEquals(2, segmentOneOneResponse.resourceIdsByResourceTypeIds[resourceTypeOne.id]?.size ?: 0)
        assertTrue(segmentOneOneResponse.resourceIdsByResourceTypeIds[resourceTypeOne.id]!!.contains(resourceOne.id))
        assertTrue(segmentOneOneResponse.resourceIdsByResourceTypeIds[resourceTypeOne.id]!!.contains(resourceTwo.id))
        assertEquals(2, segmentOneTwoResponse.resourceIdsByResourceTypeIds.size)
        assertEquals(1, segmentOneTwoResponse.resourceIdsByResourceTypeIds[resourceTypeOne.id]?.size ?: 0)
        assertTrue(segmentOneTwoResponse.resourceIdsByResourceTypeIds[resourceTypeOne.id]!!.contains(resourceFour.id))
        assertTrue(segmentOneTwoResponse.resourceIdsByResourceTypeIds[resourceTypeTwo.id]!!.contains(resourceThree.id))


        assertTrue(result.segmentations[1].segments.any { s -> s.id == segmentTwoOne.id })
        assertTrue(result.segmentations[1].segments.any { s -> s.id == segmentTwoTwo.id })
        val segmentTwoOneResponse = result.segmentations[1].segments.first { s -> s.id == segmentTwoOne.id }
        val segmentTwoTwoResponse = result.segmentations[1].segments.first { s -> s.id == segmentTwoTwo.id }
        assertEquals(1, segmentTwoOneResponse.resourceIdsByResourceTypeIds.size)
        assertEquals(1, segmentTwoOneResponse.resourceIdsByResourceTypeIds[resourceTypeOne.id]?.size ?: 0)
        assertTrue(segmentTwoOneResponse.resourceIdsByResourceTypeIds[resourceTypeOne.id]!!.contains(resourceOne.id))
        assertEquals(2, segmentTwoTwoResponse.resourceIdsByResourceTypeIds.size)
        assertEquals(2, segmentTwoTwoResponse.resourceIdsByResourceTypeIds[resourceTypeOne.id]?.size ?: 0)
        assertTrue(segmentTwoTwoResponse.resourceIdsByResourceTypeIds[resourceTypeOne.id]!!.contains(resourceTwo.id))
        assertTrue(segmentTwoTwoResponse.resourceIdsByResourceTypeIds[resourceTypeOne.id]!!.contains(resourceFour.id))
        assertEquals(1, segmentTwoTwoResponse.resourceIdsByResourceTypeIds[resourceTypeTwo.id]?.size ?: 0)
        assertTrue(segmentTwoTwoResponse.resourceIdsByResourceTypeIds[resourceTypeTwo.id]!!.contains(resourceThree.id))

        assertEquals(4, result.resources.size)
        assertTrue(result.resources.contains(resourceOne.id))
        assertTrue(result.resources.contains(resourceTwo.id))
        assertTrue(result.resources.contains(resourceThree.id))
        assertTrue(result.resources.contains(resourceFour.id))

        assertEquals(2, result.resourceTypes.size)
        assertTrue(result.resourceTypes.contains(resourceTypeOne.id))
        assertTrue(result.resourceTypes.contains(resourceTypeTwo.id))

        assertEquals(1, result.unitsEnsemblesById.size)
    }

    @Test
    fun resourceSelectionTreeWithoutSegmentationsTest(): Unit = runBlocking {
        val root = serviceModel(id = 65535, parentId = null)
        val childOne = serviceModel(id = 65536, root.id)
        val childTwo = serviceModel(id = 65537, root.id)
        val grandchildOne = serviceModel(id = 65538, childOne.id)
        val grandchildTwo = serviceModel(id = 65539, childTwo.id)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                servicesDao.upsertRecipeManyRetryable(
                    txSession,
                    listOf(root, childOne, childTwo, grandchildOne, grandchildTwo)
                )
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
                folderDao.upsertAllRetryable(
                    txSession, listOf(
                        folderRoot, folderChildOne, folderChildTwo,
                        folderGrandchildOne, folderGrandchildTwo
                    )
                ).awaitSingleOrNull()
            }
        }
        val provider = providerModel(false, FreeProvisionAggregationMode.UNALLOCATED_TRANSFERABLE)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                providersDao.upsertProviderRetryable(txSession, provider)
                    .awaitSingleOrNull()
            }
        }
        val resourceTypeOne = resourceTypeModel(provider.id, "test1", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        val resourceTypeTwo = resourceTypeModel(provider.id, "test2", UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID)
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourceTypesDao.upsertResourceTypesRetryable(
                    txSession,
                    listOf(resourceTypeOne, resourceTypeTwo)
                )
                    .awaitSingleOrNull()
            }
        }

        val resourceOne = resourceModel(
            provider.id, "test1", resourceTypeOne.id, mapOf(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        val resourceTwo = resourceModel(
            provider.id, "test2", resourceTypeOne.id, mapOf(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        val resourceThree = resourceModel(
            provider.id, "test3", resourceTypeTwo.id, mapOf(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        val resourceFour = resourceModel(
            provider.id, "test4", resourceTypeOne.id, mapOf(),
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID, setOf(UnitIds.BYTES), UnitIds.BYTES, UnitIds.BYTES, null
        )
        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                resourcesDao.upsertResourcesRetryable(
                    txSession,
                    listOf(resourceOne, resourceTwo, resourceThree, resourceFour)
                )
                    .awaitSingleOrNull()
            }
        }

        val accountRoot = accountModel(provider.id, null, folderRoot.id, "test")
        val accountChildOne = accountModel(provider.id, null, folderChildOne.id, "test")
        val accountChildTwo = accountModel(provider.id, null, folderChildTwo.id, "test")
        val accountGrandchildOne = accountModel(provider.id, null, folderGrandchildOne.id, "test")
        val accountGrandchildTwo = accountModel(provider.id, null, folderGrandchildTwo.id, "test")

        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsDao.upsertAllRetryable(
                    txSession, listOf(
                        accountRoot, accountChildOne, accountChildTwo,
                        accountGrandchildOne, accountGrandchildTwo
                    )
                ).awaitSingleOrNull()
            }
        }

        val quotaRootResourceOne = quotaModel(provider.id, resourceOne.id, folderRoot.id, 10L, 5L)
        val quotaChildOneResourceOne = quotaModel(provider.id, resourceOne.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwoResourceOne = quotaModel(provider.id, resourceOne.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOneResourceOne = quotaModel(provider.id, resourceOne.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwoResourceOne = quotaModel(provider.id, resourceOne.id, folderGrandchildTwo.id, 60L, 30L)

        val quotaRootResourceTwo = quotaModel(provider.id, resourceTwo.id, folderRoot.id, 10L, 5L)
        val quotaChildOneResourceTwo = quotaModel(provider.id, resourceTwo.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwoResourceTwo = quotaModel(provider.id, resourceTwo.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOneResourceTwo = quotaModel(provider.id, resourceTwo.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwoResourceTwo = quotaModel(provider.id, resourceTwo.id, folderGrandchildTwo.id, 60L, 30L)

        val quotaRootResourceThree = quotaModel(provider.id, resourceThree.id, folderRoot.id, 10L, 5L)
        val quotaChildOneResourceThree = quotaModel(provider.id, resourceThree.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwoResourceThree = quotaModel(provider.id, resourceThree.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOneResourceThree =
            quotaModel(provider.id, resourceThree.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwoResourceThree =
            quotaModel(provider.id, resourceThree.id, folderGrandchildTwo.id, 60L, 30L)

        val quotaRootResourceFour = quotaModel(provider.id, resourceFour.id, folderRoot.id, 10L, 5L)
        val quotaChildOneResourceFour = quotaModel(provider.id, resourceFour.id, folderChildOne.id, 30L, 15L)
        val quotaChildTwoResourceFour = quotaModel(provider.id, resourceFour.id, folderChildTwo.id, 60L, 30L)
        val quotaGrandchildOneResourceFour = quotaModel(provider.id, resourceFour.id, folderGrandchildOne.id, 30L, 15L)
        val quotaGrandchildTwoResourceFour = quotaModel(provider.id, resourceFour.id, folderGrandchildTwo.id, 60L, 30L)

        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                quotasDao.upsertAllRetryable(
                    txSession,
                    listOf(
                        quotaRootResourceOne, quotaChildOneResourceOne, quotaChildTwoResourceOne,
                        quotaGrandchildOneResourceOne, quotaGrandchildTwoResourceOne,
                        quotaRootResourceTwo, quotaChildOneResourceTwo, quotaChildTwoResourceTwo,
                        quotaGrandchildOneResourceTwo, quotaGrandchildTwoResourceTwo,
                        quotaRootResourceThree, quotaChildOneResourceThree, quotaChildTwoResourceThree,
                        quotaGrandchildOneResourceThree, quotaGrandchildTwoResourceThree,
                        quotaRootResourceFour, quotaChildOneResourceFour, quotaChildTwoResourceFour,
                        quotaGrandchildOneResourceFour, quotaGrandchildTwoResourceFour
                    )
                ).awaitSingleOrNull()
            }
        }

        val provisionRootResourceOne =
            accountQuotaModel(provider.id, resourceOne.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOneResourceOne = accountQuotaModel(
            provider.id, resourceOne.id, folderChildOne.id, accountChildOne.id,
            15L, 6L
        )
        val provisionChildTwoResourceOne = accountQuotaModel(
            provider.id, resourceOne.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L
        )
        val provisionGrandchildOneResourceOne = accountQuotaModel(
            provider.id, resourceOne.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L
        )
        val provisionGrandchildTwoResourceOne = accountQuotaModel(
            provider.id, resourceOne.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L
        )

        val provisionRootResourceTwo =
            accountQuotaModel(provider.id, resourceTwo.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOneResourceTwo = accountQuotaModel(
            provider.id, resourceTwo.id, folderChildOne.id, accountChildOne.id,
            15L, 6L
        )
        val provisionChildTwoResourceTwo = accountQuotaModel(
            provider.id, resourceTwo.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L
        )
        val provisionGrandchildOneResourceTwo = accountQuotaModel(
            provider.id, resourceTwo.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L
        )
        val provisionGrandchildTwoResourceTwo = accountQuotaModel(
            provider.id, resourceTwo.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L
        )

        val provisionRootResourceThree =
            accountQuotaModel(provider.id, resourceThree.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOneResourceThree = accountQuotaModel(
            provider.id, resourceThree.id, folderChildOne.id, accountChildOne.id,
            15L, 6L
        )
        val provisionChildTwoResourceThree = accountQuotaModel(
            provider.id, resourceThree.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L
        )
        val provisionGrandchildOneResourceThree = accountQuotaModel(
            provider.id, resourceThree.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L
        )
        val provisionGrandchildTwoResourceThree = accountQuotaModel(
            provider.id, resourceThree.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L
        )

        val provisionRootResourceFour =
            accountQuotaModel(provider.id, resourceFour.id, folderRoot.id, accountRoot.id, 5L, 2L)
        val provisionChildOneResourceFour = accountQuotaModel(
            provider.id, resourceFour.id, folderChildOne.id, accountChildOne.id,
            15L, 6L
        )
        val provisionChildTwoResourceFour = accountQuotaModel(
            provider.id, resourceFour.id, folderChildTwo.id, accountChildTwo.id,
            30L, 12L
        )
        val provisionGrandchildOneResourceFour = accountQuotaModel(
            provider.id, resourceFour.id, folderGrandchildOne.id,
            accountGrandchildOne.id, 15L, 6L
        )
        val provisionGrandchildTwoResourceFour = accountQuotaModel(
            provider.id, resourceFour.id, folderGrandchildTwo.id,
            accountGrandchildTwo.id, 30L, 12L
        )


        dbSessionRetryable(tableClient) {
            rwTxRetryable {
                accountsQuotasDao.upsertAllRetryable(
                    txSession,
                    listOf(
                        provisionRootResourceOne, provisionChildOneResourceOne, provisionChildTwoResourceOne,
                        provisionGrandchildOneResourceOne, provisionGrandchildTwoResourceOne,
                        provisionRootResourceTwo, provisionChildOneResourceTwo, provisionChildTwoResourceTwo,
                        provisionGrandchildOneResourceTwo, provisionGrandchildTwoResourceTwo,
                        provisionRootResourceThree, provisionChildOneResourceThree, provisionChildTwoResourceThree,
                        provisionGrandchildOneResourceThree, provisionGrandchildTwoResourceThree,
                        provisionRootResourceFour, provisionChildOneResourceFour, provisionChildTwoResourceFour,
                        provisionGrandchildOneResourceFour, provisionGrandchildTwoResourceFour
                    )
                ).awaitSingleOrNull()
            }
        }

        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/{providerId}/_aggregate", provider.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent

        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/aggregation/_resourceSelectionList?providerId=${provider.id}&rootServiceId=${root.id}")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(ResourceSelectionListResponseDto::class.java)
            .responseBody
            .awaitSingle()

        assertEquals(4, result.resources.size)
        assertTrue(result.resources.contains(resourceOne.id))
        assertTrue(result.resources.contains(resourceTwo.id))
        assertTrue(result.resources.contains(resourceThree.id))
        assertTrue(result.resources.contains(resourceFour.id))

        assertEquals(2, result.resourceTypes.size)
        assertTrue(result.resourceTypes.contains(resourceTypeOne.id))
        assertTrue(result.resourceTypes.contains(resourceTypeTwo.id))

        assertEquals(1, result.unitsEnsemblesById.size)
    }

    @Test
    fun resourceSelectionListEmptyResultTest() = runBlocking {
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/aggregation/_resourceSelectionList?providerId=$YP_ID&rootServiceId=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(ResourceSelectionListResponseDto::class.java)
            .responseBody
            .awaitSingle()

        assertEquals(0, result.unitsEnsemblesById.size)
        assertEquals(0, result.segmentations.size)
        assertEquals(0, result.resourceTypes.size)
        assertEquals(0, result.resources.size)

    }
}
