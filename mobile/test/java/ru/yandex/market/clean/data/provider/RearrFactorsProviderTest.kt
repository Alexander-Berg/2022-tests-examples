package ru.yandex.market.clean.data.provider

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.common.experiments.ExperimentConsts
import ru.yandex.market.common.experiments.config.ExperimentConfig
import ru.yandex.market.common.experiments.experiment.purchasebylist.PurchaseByListMultiMedicalParcelsSplit
import ru.yandex.market.common.experiments.manager.ExperimentManager
import ru.yandex.market.common.experiments.service.ExperimentConfigService
import ru.yandex.market.common.experiments.service.ExperimentConfigServiceHolder
import ru.yandex.market.common.featureconfigs.managers.BnplToggleManager
import ru.yandex.market.common.featureconfigs.managers.DivReviewsToggleManager
import ru.yandex.market.common.featureconfigs.managers.EatsRetailToggleManager
import ru.yandex.market.common.featureconfigs.managers.FlashSalesToggleManager
import ru.yandex.market.common.featureconfigs.managers.ForceForQAToggleManager
import ru.yandex.market.common.featureconfigs.managers.FpRearrConfigManager
import ru.yandex.market.common.featureconfigs.managers.LavkaInMarketV2ConfigManager
import ru.yandex.market.common.featureconfigs.managers.LiveStreamPreviewToggleManager
import ru.yandex.market.common.featureconfigs.managers.MultiPromoV2ToggleManager
import ru.yandex.market.common.featureconfigs.managers.PaymentSdkToggleManager
import ru.yandex.market.common.featureconfigs.managers.PharmaRearrFeatureToggleManager
import ru.yandex.market.common.featureconfigs.managers.PurchaseByListMultiMedicalParcelsFeatureToggleManager
import ru.yandex.market.common.featureconfigs.managers.ResaleGoodsToggleManager
import ru.yandex.market.common.featureconfigs.managers.StationSubscriptionToggleManager
import ru.yandex.market.common.featureconfigs.managers.StoriesPreviewToggleManager
import ru.yandex.market.common.featureconfigs.managers.TinkoffCreditsConfigManager
import ru.yandex.market.common.featureconfigs.managers.TinkoffInstallmentsToggleManager
import ru.yandex.market.common.featureconfigs.managers.TryingAvailableFilterToggleManager
import ru.yandex.market.common.featureconfigs.models.EatsRetailWebviewConfig
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.common.featureconfigs.models.ForceForQAConfig
import ru.yandex.market.common.featureconfigs.models.FpRearrConfig
import ru.yandex.market.common.featureconfigs.models.LavkaInMarketV2Config
import ru.yandex.market.common.featureconfigs.models.MultiPromoV2Config
import ru.yandex.market.common.featureconfigs.models.PharmaRearrFeatureToggleConfig
import ru.yandex.market.common.featureconfigs.models.PurchaseByListMultiMedicalParcelsConfig
import ru.yandex.market.common.featureconfigs.models.StationSubscriptionConfig
import ru.yandex.market.common.featureconfigs.models.TinkoffCreditsConfig
import ru.yandex.market.common.featureconfigs.models.tinkoffCreditsConfig_MoneyTestInstance
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider
import ru.yandex.market.common.rearrfactors.DynamicRearrFactorsProvider
import ru.yandex.market.common.rearrfactors.RearrFactorsOverrider
import ru.yandex.market.common.rearrfactors.RearrFactorsProvider

class RearrFactorsProviderTest {

    private val experimentConsts = mock<ExperimentConsts> {
        on { configExtraRearrFlags } doReturn ""
    }
    private val experimentConfigService = mock<ExperimentConfigService> {
        on { getConfigs() } doReturn emptyList()
    }
    private val experimentConfigServiceFactory = mock<ExperimentConfigServiceHolder> {
        on { experimentConfigServiceInstance } doReturn experimentConfigService
    }
    private val rearrFactorsOverrider = mock<RearrFactorsOverrider>()
    private val dynamicRearrFactorsProvider = mock<DynamicRearrFactorsProvider> {
        on { getDynamicRearrFactors() } doReturn emptySet()
    }

    private val configuration = RearrFactorsProvider.Configuration(
        rearrWhiteCpaOnBlueWithoutMsku = "WCOBWOM",
        rearrCrossdock = "CD",
        rearrCheapestAsGift = "CAG",
        rearrFlashSales = "FD",
        rearrBlueSet = "BS",
        rearrBlueSetMulti = "BS_MULTI",
        rearShowAllAlternativeOffers = "AO",
        rearrStoryPreview = "SP",
        rearrWhiteCredits = "WHITE_CREDITS",
        rearrSkillGroup = "SKLGRP",
        rearrTinkoffInstallments = "INSTLLMNTS",
        flavour = RearrFactorsProvider.Flavour.QA,
        rearrLavkaInMarket = "LAVKA",
        rearrInstallmentsFilter = "INSTALLMENTS_FILTER",
        rearrSkuInProductRedirects = "PRODUCT_REDIRECTS",
        rearrDivkit = "divkit",
        rearrFoodtechOffers = REARR_ENABLE_FOODTECH_OFFERS,
        rearrBlenderBundlesForInclid = REARR_ENABLE_BUNDLES_FOR_INCLID,
        rearrTryingAvailableFilterEnabled = "TAFE",
        rearrResaleGoodsEnabled = REARR_RESALE_GOODS,
    )

    private val flashSalesToggleManager = mock<FlashSalesToggleManager> {
        on { get() } doReturn FeatureToggle(isEnabled = false)
    }

    private val multiPromoV2ToggleManager = mock<MultiPromoV2ToggleManager> {
        on { get() } doReturn MultiPromoV2Config(false, emptyList())
    }

    private val liveStreamPreviewToggleManager = mock<LiveStreamPreviewToggleManager> {
        on { get() } doReturn FeatureToggle(isEnabled = false)
    }

    private val fpRearrToggleManager = mock<FpRearrConfigManager> {
        on { get() } doReturn FpRearrConfig(isEnabled = false, rearrList = emptyList())
    }

    private val forceForQaConfigManager = mock<ForceForQAToggleManager> {
        on { get() } doReturn ForceForQAConfig(false, FORCE_FOR_QA_REARRS)
    }

    private val storiesPreviewToggleManager = mock<StoriesPreviewToggleManager> {
        on { get() } doReturn FeatureToggle(false)
    }

    private val tinkoffCreditsConfigManager = mock<TinkoffCreditsConfigManager> {
        on { get() } doReturn TinkoffCreditsConfig(
            false,
            tinkoffCreditsConfig_MoneyTestInstance(),
            tinkoffCreditsConfig_MoneyTestInstance()
        )
    }

    private val tinkoffInstallmentsToggleManager = mock<TinkoffInstallmentsToggleManager> {
        on { get() } doReturn FeatureToggle(false)
    }

    private val stationSubscriptionToggleManager = mock<StationSubscriptionToggleManager> {
        on { get() } doReturn StationSubscriptionConfig(
            isEnabled = false,
            stationSupplierId = 0,
            rearrFactors = mutableListOf()
        )
    }

    private val lavkaInMarket2ConfigManager = mock<LavkaInMarketV2ConfigManager> {
        on { get() } doReturn LavkaInMarketV2Config(
            isEnabled = true,
            forceEnabled = true,
            searchMinimum = 0L
        )
    }

    private val bnplToggleManager = mock<BnplToggleManager> {
        on { get() } doReturn FeatureToggle(false)
    }

    private val paymentSdkToggleManager = mock<PaymentSdkToggleManager> {
        on { get() } doReturn FeatureToggle(false)
    }

    private val divReviewsToggleManager = mock<DivReviewsToggleManager> {
        on { get() } doReturn FeatureToggle(isEnabled = true)
    }

    private val multiMedicalParcelsToggleManager = mock<PurchaseByListMultiMedicalParcelsFeatureToggleManager> {
        on { get() } doReturn PurchaseByListMultiMedicalParcelsConfig(isEnabled = false, rearrList = emptyList())
    }

    private val pharmaRearrFeatureToggleManager = mock<PharmaRearrFeatureToggleManager> {
        on { get() } doReturn PharmaRearrFeatureToggleConfig(isEnabled = false, rearrList = emptyList())
    }

    private val eatsReatailToggleManager = mock<EatsRetailToggleManager> {
        on { get() } doReturn EatsRetailWebviewConfig(true, "", "")
    }

    private val tryingAvailableFilterToggleManager = mock<TryingAvailableFilterToggleManager> {
        on { get() } doReturn FeatureToggle(isEnabled = true)
    }

    private val resaleToggleManager = mock<ResaleGoodsToggleManager> {
        on { get() } doReturn FeatureToggle(isEnabled = false)
    }

    private val featureConfigsProvider = mock<FeatureConfigsProvider>().also { provider ->
        whenever(provider.flashSalesToggleManager) doReturn flashSalesToggleManager
        whenever(provider.multiPromoV2ToggleManager) doReturn multiPromoV2ToggleManager
        whenever(provider.liveStreamPreviewToggleManager) doReturn liveStreamPreviewToggleManager
        whenever(provider.fpRearrConfigManager) doReturn fpRearrToggleManager
        whenever(provider.storiesPreviewToggleManager) doReturn storiesPreviewToggleManager
        whenever(provider.tinkoffCreditsConfigManager) doReturn tinkoffCreditsConfigManager
        whenever(provider.lavkaInMarketV2ConfigManager) doReturn lavkaInMarket2ConfigManager
        whenever(provider.forceForQAToggleManager) doReturn forceForQaConfigManager
        whenever(provider.tinkoffInstallmentsToggleManager) doReturn tinkoffInstallmentsToggleManager
        whenever(provider.bnplToggleManager) doReturn bnplToggleManager
        whenever(provider.paymentSdkToggleManager) doReturn paymentSdkToggleManager
        whenever(provider.stationSubscriptionToggleManager) doReturn stationSubscriptionToggleManager
        whenever(provider.divReviewsToggleManager) doReturn divReviewsToggleManager
        whenever(provider.purchaseByListMultiMedicalParcelsFeatureToggleManager) doReturn multiMedicalParcelsToggleManager
        whenever(provider.pharmaRearrFeatureToggleManager) doReturn pharmaRearrFeatureToggleManager
        whenever(provider.eatsRetailToggleManager) doReturn eatsReatailToggleManager
        whenever(provider.tryingAvailableFilterToggleManager) doReturn tryingAvailableFilterToggleManager
        whenever(provider.resaleGoodsToggleManager) doReturn resaleToggleManager
    }

    private val experimentManager = mock<ExperimentManager>().also { manager ->
        whenever(
            manager.getExperiment(
                PurchaseByListMultiMedicalParcelsSplit::class.java
            )
        ) doReturn object : PurchaseByListMultiMedicalParcelsSplit {
            override val isMultiMedicalParcelsEnabled = false
        }
    }

    private val provider = RearrFactorsProvider(
        experimentConfigServiceFactory,
        configuration,
        experimentConsts,
        featureConfigsProvider,
        experimentManager,
        rearrFactorsOverrider,
        dynamicRearrFactorsProvider,
    )

    @Test
    fun `Check extra rearr flag with others rearrs`() {
        val extraRearr = "extra_rearr"

        whenever(experimentConsts.configExtraRearrFlags) doReturn extraRearr

        assertThat(provider.getRearrFactors()).contains(extraRearr)
    }

    @Test
    fun `Correct dsbs mode enabled by rearr`() {
        assertThat(provider.getRearrFactors()).contains(configuration.rearrWhiteCpaOnBlueWithoutMsku)
    }

    @Test
    fun `Check credits rearr exists when credits are enabled`() {
        whenever(tinkoffCreditsConfigManager.get()).thenReturn(
            TinkoffCreditsConfig(
                true,
                tinkoffCreditsConfig_MoneyTestInstance(),
                tinkoffCreditsConfig_MoneyTestInstance()
            )
        )
        assertThat(provider.getRearrFactors()).contains(configuration.rearrWhiteCredits)
    }

    @Test
    fun `Check credits rearr doesn't exist when credits are disabled`() {
        assertThat(provider.getRearrFactors()).doesNotContain(configuration.rearrWhiteCredits)
    }

    @Test
    fun `Check rearr factors when forceForQa is disabled`() {
        assertThat(provider.getRearrFactors()).doesNotContainSequence(FORCE_FOR_QA_REARRS)
    }

    @Test
    fun `Check rearr factors when forceForQa is enabled`() {
        whenever(forceForQaConfigManager.get()).thenReturn(ForceForQAConfig(true, FORCE_FOR_QA_REARRS))
        assertThat(provider.getRearrFactors()).containsSequence(FORCE_FOR_QA_REARRS)
    }

    @Test
    fun `Check rearr factors when eats retail is enabled`() {
        assertThat(provider.getRearrFactors()).containsAll(EATS_RETAIL_REARRS)
    }

    @Test
    fun `Check rearr factors when eats retail is disabled`() {
        whenever(eatsReatailToggleManager.get()).thenReturn(EatsRetailWebviewConfig(false, "", ""))
        assertThat(provider.getRearrFactors()).doesNotContainSequence(EATS_RETAIL_REARRS)
    }

    @Test
    fun `Check rearr factors when forceForQa is enabled but flavour is BASE`() {
        val config = configuration.copy(flavour = RearrFactorsProvider.Flavour.BASE)
        val provider = RearrFactorsProvider(
            experimentConfigServiceFactory,
            config,
            experimentConsts,
            featureConfigsProvider,
            experimentManager,
            rearrFactorsOverrider,
            dynamicRearrFactorsProvider,
        )
        whenever(forceForQaConfigManager.get()).thenReturn(ForceForQAConfig(true, FORCE_FOR_QA_REARRS))
        assertThat(provider.getRearrFactors()).doesNotContainSequence(FORCE_FOR_QA_REARRS)
    }

    @Test
    fun `Check rearrs are removed by disabled toggles`() {
        whenever(experimentConfigService.getConfigs()) doReturn CONFIGS_ELIMINATED_BY_TOGGLES
        assertThat(provider.getRearrFactors()).doesNotContainSequence(REARRS_ELIMINATED_BY_TOGGLES)
    }

    companion object {

        private const val REARR_ENABLE_FOODTECH_OFFERS = "REARR_ENABLE_FOODTECH_OFFERS"
        private const val REARR_ENABLE_BUNDLES_FOR_INCLID = "REARR_ENABLE_BUNDLES_FOR_INCLID"
        private const val REARR_RESALE_GOODS = "market_resale_goods_exp"
        private val FORCE_FOR_QA_REARRS = listOf("test_rearr_for_qa1", "test_rearr_for_qa2")
        private val EATS_RETAIL_REARRS = listOf(REARR_ENABLE_FOODTECH_OFFERS, REARR_ENABLE_BUNDLES_FOR_INCLID)
        private val RESALE_GOODS_EXP_CONFIG = generateExpConfigWithRearrs(REARR_RESALE_GOODS)
        private val CONFIGS_ELIMINATED_BY_TOGGLES = listOf(RESALE_GOODS_EXP_CONFIG)
        private val REARRS_ELIMINATED_BY_TOGGLES = listOf(REARR_RESALE_GOODS)

        private fun generateExpConfigWithRearrs(vararg rearrs: String): ExperimentConfig {
            val resultingRearrs = rearrs.toList()
            return ExperimentConfig(
                testId = "1",
                alias = "exp",
                bucketId = "1",
                rearrFlags = resultingRearrs,
                isOverride = false,
            )
        }
    }
}
