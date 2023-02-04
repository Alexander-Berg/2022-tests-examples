package ru.yandex.intranet.d.web.front.aggregation

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.kotlin.ProviderId
import ru.yandex.intranet.d.model.accounts.AccountModel
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel
import ru.yandex.intranet.d.model.folders.FolderModel
import ru.yandex.intranet.d.model.folders.FolderType
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel
import ru.yandex.intranet.d.model.providers.AggregationSettings
import ru.yandex.intranet.d.model.providers.FreeProvisionAggregationMode
import ru.yandex.intranet.d.model.providers.ProviderModel
import ru.yandex.intranet.d.model.providers.UsageMode
import ru.yandex.intranet.d.model.quotas.QuotaModel
import ru.yandex.intranet.d.model.resources.ResourceModel
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel
import ru.yandex.intranet.d.model.resources.segmentations.SegmentationUISettings
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel
import ru.yandex.intranet.d.model.services.ServiceRecipeModel
import ru.yandex.intranet.d.model.services.ServiceState
import ru.yandex.intranet.d.model.usage.ServiceUsageAmounts
import ru.yandex.intranet.d.model.usage.ServiceUsageKey
import ru.yandex.intranet.d.model.usage.ServiceUsageModel
import ru.yandex.intranet.d.model.usage.UsageAmount
import ru.yandex.intranet.d.model.usage.UsagePoint
import ru.yandex.intranet.d.services.usage.accumulate
import ru.yandex.intranet.d.services.usage.histogram
import ru.yandex.intranet.d.services.usage.mean
import ru.yandex.intranet.d.services.usage.minMedianMax
import ru.yandex.intranet.d.services.usage.roundToIntegerHalfUp
import ru.yandex.intranet.d.services.usage.variance
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.aggregation.FindServiceTotalsExpandedFilterDto
import ru.yandex.intranet.d.web.model.aggregation.FindServiceTotalsRequestDto
import ru.yandex.intranet.d.web.model.aggregation.FindServiceTotalsRequestFilterDto
import ru.yandex.intranet.d.web.model.aggregation.FindServiceTotalsResponseDto
import ru.yandex.intranet.d.web.model.aggregation.FindSubtreeTotalRequestDto
import ru.yandex.intranet.d.web.model.aggregation.FindSubtreeTotalResponseDto
import ru.yandex.intranet.d.web.model.aggregation.RankSubtreeAmountsRequestDto
import ru.yandex.intranet.d.web.model.aggregation.RankSubtreeAmountsResponseDto
import ru.yandex.intranet.d.web.model.aggregation.RankSubtreeSortingParamsDto
import java.math.BigInteger
import java.time.Instant
import java.util.*

@Component
class FrontAggregationTestHelper {
    @Autowired
    lateinit var webClient: WebTestClient

    suspend fun rankSubtreeAmounts(
        rootServiceId: Long,
        resourceId: String,
        from: String?,
        limit: Long,
        sortingParams: RankSubtreeSortingParamsDto = RankSubtreeSortingParamsDto.default(),
        unitId: String? = null
    ): RankSubtreeAmountsResponseDto? = webClient
        .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
        .post()
        .uri("/front/aggregation/_rankSubtreeAmounts")
        .bodyValue(
            RankSubtreeAmountsRequestDto(
                rootServiceId,
                resourceId,
                from,
                limit,
                sortingParams,
                unitId
            )
        )
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(RankSubtreeAmountsResponseDto::class.java)
        .responseBody
        .awaitSingle()

    suspend fun findSubtreeTotals(
        rootServiceId: Long,
        resourceId: String,
        unitId: String? = null
    ): FindSubtreeTotalResponseDto? = webClient
        .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
        .post()
        .uri("/front/aggregation/_findSubtreeTotal")
        .bodyValue(FindSubtreeTotalRequestDto(rootServiceId, resourceId, unitId))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(FindSubtreeTotalResponseDto::class.java)
        .responseBody
        .awaitSingle()

    suspend fun findServiceTotals(
        rootServiceId: Long,
        filter: FindServiceTotalsRequestFilterDto? = null,
        expandedFilter: FindServiceTotalsExpandedFilterDto? = null
    ): FindServiceTotalsResponseDto = webClient
        .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
        .post()
        .uri("/front/aggregation/_findServiceTotals")
        .bodyValue(FindServiceTotalsRequestDto(rootServiceId, filter, expandedFilter))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(FindServiceTotalsResponseDto::class.java)
        .responseBody
        .awaitSingle()

    fun aggregate(
        providerId: ProviderId
    ) {
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/admin/aggregation/$providerId/_aggregate")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent
    }

    companion object {

        fun providerModel(
            accountsSpacesSupported: Boolean,
            aggregationMode: FreeProvisionAggregationMode
        ): ProviderModel {
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
                .aggregationSettings(AggregationSettings(aggregationMode, UsageMode.TIME_SERIES, 300))
                .build()
        }

        fun resourceTypeModel(providerId: String, key: String, unitsEnsembleId: String): ResourceTypeModel {
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

        fun resourceModel(
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
                .segments(segments.map { (k, v) -> ResourceSegmentSettingsModel(k, v) }.toSet())
                .resourceUnits(ResourceUnitsModel(allowedUnitIds, defaultUnitId, null))
                .managed(true)
                .orderable(true)
                .readOnly(false)
                .baseUnitId(baseUnitId)
                .accountsSpacesId(accountsSpaceId)
                .build()
        }

        fun folderModel(serviceId: Long): FolderModel {
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

        fun quotaModel(
            providerId: String,
            resourceId: String,
            folderId: String,
            quota: Long,
            balance: Long
        ): QuotaModel {
            return QuotaModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .providerId(providerId)
                .resourceId(resourceId)
                .folderId(folderId)
                .quota(quota)
                .balance(balance)
                .frozenQuota(0L)
                .build()
        }

        fun accountQuotaModel(
            providerId: String, resourceId: String, folderId: String,
            accountId: String, provided: Long, allocated: Long
        ): AccountsQuotasModel {
            return AccountsQuotasModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setProviderId(providerId)
                .setResourceId(resourceId)
                .setFolderId(folderId)
                .setAccountId(accountId)
                .setProvidedQuota(provided)
                .setAllocatedQuota(allocated)
                .setLastProvisionUpdate(Instant.now())
                .setLastReceivedProvisionVersion(null)
                .setLatestSuccessfulProvisionOperationId(null)
                .build()
        }

        fun accountModel(
            providerId: String, accountsSpaceId: String?, folderId: String,
            displayName: String
        ): AccountModel {
            return AccountModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setId(UUID.randomUUID().toString())
                .setVersion(0L)
                .setProviderId(providerId)
                .setAccountsSpacesId(accountsSpaceId)
                .setOuterAccountIdInProvider(UUID.randomUUID().toString())
                .setOuterAccountKeyInProvider(UUID.randomUUID().toString())
                .setFolderId(folderId)
                .setDisplayName(displayName)
                .setDeleted(false)
                .setLastAccountUpdate(Instant.now())
                .setLastReceivedVersion(null)
                .setLatestSuccessfulAccountOperationId(null)
                .build()
        }

        fun serviceModel(id: Long, parentId: Long?): ServiceRecipeModel {
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

        fun resourceSegmentationModel(
            providerId: String,
            key: String,
            choiceOrder: Int = 0
        ): ResourceSegmentationModel {
            return ResourceSegmentationModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .providerId(providerId)
                .version(0L)
                .key(key)
                .groupingOrder(choiceOrder)
                .uiSettings(
                    SegmentationUISettings(
                        choiceOrder,
                        inSameBlockWithPrevious = null,
                        title = null
                    )
                )
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

        fun serviceUsageModel(serviceId: Long, providerId: String, resourceId: String,
                                      intervalStart: Instant, ownTimeSeries: Map<Long, Long>?,
                                      subtreeTimeSeries: Map<Long, Long>?, totalTimeSeries: Map<Long, Long>?
        ): ServiceUsageModel {
            return ServiceUsageModel(
                key = ServiceUsageKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = serviceId,
                    providerId = providerId,
                    resourceId = resourceId
                ),
                lastUpdate = Instant.now(),
                epoch = 0,
                usage = ServiceUsageAmounts(
                    own = if (ownTimeSeries != null) { usageAmount(ownTimeSeries, intervalStart) } else { null },
                    subtree = if (subtreeTimeSeries != null) { usageAmount(subtreeTimeSeries, intervalStart) } else { null },
                    total = if (totalTimeSeries != null) { usageAmount(totalTimeSeries, intervalStart) } else { null }
                )
            )
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
            val values = sortedEntries.map { UsagePoint(it.key, it.value, null) }
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
                values = values,
                valuesX = valuesX,
                valuesY = valuesY,
                unused = null
            )
        }
    }
}
