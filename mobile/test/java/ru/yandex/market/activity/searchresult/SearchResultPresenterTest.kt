package ru.yandex.market.activity.searchresult

import android.os.Build
import io.reactivex.Single
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.analitycs.events.cashback.SearchResultCashbackEventParams
import ru.yandex.market.analytics.offer.OfferAnalyticsFacade
import ru.yandex.market.clean.domain.model.Category
import ru.yandex.market.clean.domain.model.ProductOffer
import ru.yandex.market.clean.domain.model.SearchProductItem
import ru.yandex.market.clean.domain.model.Supplier
import ru.yandex.market.clean.domain.model.categoryTestInstance
import ru.yandex.market.clean.presentation.feature.flashsales.FlashSalesAnalyticsHelper
import ru.yandex.market.clean.presentation.feature.lavka.badge.LavkaBadgeFormatter
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.common.experiments.experiment.sponsored.SponsoredTagNameExperiment
import ru.yandex.market.common.featureconfigs.managers.FilterSizeTableToggleManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.feature.manager.CmsSearchSnippetFeatureManager
import ru.yandex.market.feature.manager.LavkaIncutRelevanceFeatureManager
import ru.yandex.market.feature.manager.LavkaSupportedSearchFiltersFeatureManager
import ru.yandex.market.feature.manager.SponsoredTagNameFeatureManager
import ru.yandex.market.fragment.search.SearchRequestTargetScreen
import ru.yandex.market.utils.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class SearchResultPresenterTest {

    private val router = mock<Router> {
        on { currentScreen } doReturn CURRENT_SCREEN
    }
    private val offerAnalyticsFacade = mock<OfferAnalyticsFacade>()
    private val duration = mock<Duration>()
    private val searchResultCashbackEventParams = mock<SearchResultCashbackEventParams>()

    private val mockSupplier = mock<Supplier> {
        on { id } doReturn 0L
    }
    private val offer = mock<ProductOffer> {
        on { supplier } doReturn mockSupplier
    }
    private val searchProductItemOffer = mock<SearchProductItem.Offer> {
        on { productOffer } doReturn offer
    }
    private val flashSalesAnalyticsHelper = mock<FlashSalesAnalyticsHelper> {
        on { calculateFlashSalesEndTime(offer) } doReturn duration
    }

    private val searchResultCashbackEventsParamsMapper = mock<SearchResultCashbackEventsParamsMapper> {
        on { map(offer) } doReturn searchResultCashbackEventParams
    }

    private val category: Category = categoryTestInstance()

    private val searchResultArguments = mock<SearchResultArguments> {
        on { category } doReturn category
        on { supplierName } doReturn "name"
    }

    private val filterSizesTableToggleManager = mock<FilterSizeTableToggleManager> {
        on { get() } doReturn FeatureToggle(false)
    }

    private val cmsSearchSnippetFeatureManager = mock<CmsSearchSnippetFeatureManager> {
        on { isEnabled() } doReturn Single.just(true)
    }

    private val lavkaIncutRelevanceFeatureManager = mock<LavkaIncutRelevanceFeatureManager> {
        on { isEnabled() } doReturn Single.just(false)
    }

    private val sponsoredTagNameFeatureManager = mock<SponsoredTagNameFeatureManager> {
        on { getSponsoredNaming() } doReturn SponsoredTagNameExperiment.SponsoredNaming.SPONSORED
    }

    private val lavkaSupportedFiltersFeatureManager = mock<LavkaSupportedSearchFiltersFeatureManager> {
        on { getSupportedFilters() } doReturn Single.just(emptyMap())
    }

    private val lavkaBadgeFormatter = mock<LavkaBadgeFormatter> {
        on { format(any(), any()) } doReturn null
    }

    private val presenter = SearchResultPresenter(
        schedulers = mock(),
        useCases = mock(),
        arguments = searchResultArguments,
        router = router,
        resourcesManager = mock(),
        analyticsService = mock(),
        metricaSender = mock(),
        offerAnalyticsFacade = offerAnalyticsFacade,
        flashSalesAnalyticsHelper = flashSalesAnalyticsHelper,
        supplierHeaderTextFormatter = mock(),
        searchResultCashbackEventsParamsMapper = searchResultCashbackEventsParamsMapper,
        hyperlocalAddressFormatter = mock(),
        hyperlocalAnalytics = mock(),
        firebaseEcommAnalyticsFacade = mock(),
        getSearchItemsUseCase = mock(),
        getSearchShopListItemsFormatter = mock(),
        skillGroupChatFormatter = mock(),
        skillGroupChatAnalytics = mock(),
        filtersCommandUseCase = mock(),
        checkSelectorEnabledUseCase = mock(),
        shopInShopBottomBarVoFormatter = mock(),
        lavkaSearchResultVoFormatter = mock(),
        searchHeaderCategoryTitleFormatter = mock(),
        filterSizesTableToggleManager = filterSizesTableToggleManager,
        searchResultHealthFacade = mock(),
        cmsSearchSnippetFeatureManager = cmsSearchSnippetFeatureManager,
        lavkaIncutRelevanceFeatureManager = lavkaIncutRelevanceFeatureManager,
        sponsoredTagNameFeatureManager = sponsoredTagNameFeatureManager,
        lavkaSupportedFiltersFeatureManager = lavkaSupportedFiltersFeatureManager,
        lavkaAnalytics = mock(),
        lavkaBadgeFormatter = lavkaBadgeFormatter,
        shopActualizedDeliveryFormatter = mock(),
        eatsRetailAnalytics = mock(),
        sisCartNavigationDelegate = mock(),
        filtersAnalytics = mock(),
        fmcgRedesignFeatureManager = mock(),
        shopInShopAnalytics = mock(),
        experimentManager = mock(),
        visualSearchHelper = mock(),
        visualSearchAnalyticsFacade = mock(),
        commonActionHelper = mock()
    )

    @Test
    fun `Pass category and search text inside params when opening search screen`() {
        val targetScreenCaptor = argumentCaptor<SearchRequestTargetScreen>()

        val sourceScreen = mock<Screen>()
        val searchText = "some search"

        whenever(router.currentScreen) doReturn sourceScreen

        presenter.openSearchScreen(searchText)

        verify(router).navigateTo(targetScreenCaptor.capture())

        assertThat(targetScreenCaptor.firstValue.params.category).isEqualTo(category)
        assertThat(targetScreenCaptor.firstValue.params.searchText).isSameAs(searchText)
    }

    @Test
    fun `Test search item shown event 2`() {
        presenter.notifySearchItemClicked(searchProductItemOffer.productOffer, 1)
        verify(offerAnalyticsFacade).searchItemClickedEvent(
            category,
            "",
            offer,
            1,
            duration,
            true,
            searchResultCashbackEventParams,
            0L,
            "name",
            Screen.HOME
        )
    }

    companion object {
        private val CURRENT_SCREEN = Screen.HOME
    }
}
