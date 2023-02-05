package ru.yandex.market.clean.presentation.feature.product

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import ru.yandex.market.analitycs.events.health.HealthEvent
import ru.yandex.market.analitycs.events.health.additionalData.SkuLoadFailedInfo
import ru.yandex.market.analitycs.health.MetricaSender
import ru.yandex.market.analytics.facades.ProductUpperButtonAnalytics
import ru.yandex.market.analytics.firebase.FirebaseEcommAnalyticsFacade
import ru.yandex.market.analytics.health.HealthLevel
import ru.yandex.market.analytics.health.HealthName
import ru.yandex.market.analytics.health.HealthPortion
import ru.yandex.market.analytics.offer.OfferAnalyticsFacade
import ru.yandex.market.base.network.common.Response
import ru.yandex.market.base.network.common.exception.CommunicationException
import ru.yandex.market.clean.domain.model.product.ProductData
import ru.yandex.market.clean.domain.model.product.SkuProductData
import ru.yandex.market.clean.domain.model.sku.skuTestInstance
import ru.yandex.market.clean.presentation.error.CommonErrorHandler
import ru.yandex.market.clean.presentation.feature.cms.item.offer.formatter.FinancialProductFormatter
import ru.yandex.market.clean.presentation.feature.product.extension.ProductExtensionFactory
import ru.yandex.market.clean.presentation.feature.product.stationSubscription.StationSubscriptionVisibilityAnalyticsManager
import ru.yandex.market.clean.presentation.feature.question.vo.ProductQaEventFormatter
import ru.yandex.market.clean.presentation.feature.sis.mainpage.formatter.ShopActualizedDeliveryFormatter
import ru.yandex.market.clean.presentation.formatter.InstallmentPickerFormatter
import ru.yandex.market.clean.presentation.formatter.ServiceFormatter
import ru.yandex.market.clean.presentation.formatter.ShopInShopBottomBarVoFormatter
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.domain.product.model.SkuId
import ru.yandex.market.domain.product.model.skuIdTestInstance
import ru.yandex.market.feature.manager.OutOfStockProductKmFilterFeatureManager
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.utils.seconds

class ProductPresenterTest {

    private val productStream = PublishSubject.create<ProductData>()
    private val skuIdStream = PublishSubject.create<SkuId>()
    private val productId = skuIdTestInstance()
    private val arguments =
        ProductFragment.Arguments(
            productId = productId,
            offerCpc = "offerCpc",
            redirectText = "redirectText",
            showUid = "showUid",
            forcedDefaultOfferId = null,
        )
    private val productView = mock<ProductView>()

    private val schedulers = presentationSchedulersMock()
    private val useCases = mock<ProductUseCases> {
        on {
            loadProduct(
                productId = eq(productId),
                additionalOffersCount = anyOrNull(),
                showUid = eq(arguments.showUid),
                modelFilters = anyOrNull(),
                arguments = eq(arguments),
                businessId = anyOrNull(),
                shopInShopPageId = anyOrNull(),
                outOfStockSkuIds = any(),
            )
        } doReturn productStream
        on { addActiveUserAction() } doReturn Completable.complete()
        on { getSelectedSkuIdStream() } doReturn skuIdStream
        on { updateRedirectText(arguments.redirectText) } doReturn Completable.complete()
        on { observeQuestionEvents() } doReturn Observable.empty()
        on { isTinkoffCreditsEnabled() } doReturn Single.just(false)
        on { isBnplEnabled() } doReturn Single.just(false)
        on { isTinkoffInstallmentsEnabled() } doReturn Single.just(false)
        on { isNewFinancialPrioritiesEnabledUseCase() } doReturn Single.just(false)
        on { getAuthStatusStream() } doReturn Observable.just(false)
        on { startTracking(any()) } doReturn Completable.complete()
        on { saveSkuInHistory(any()) } doReturn Completable.complete()
        on { isPurchaseByListMedicineEnabled() } doReturn Single.just(false)
        on { getPartialDeliveryOnboardingWasShown() } doReturn Single.just(false)
        on { setPartialDeliveryOnboardingWasShown(any()) } doReturn Completable.complete()
        on { getSelectedServiceInfoStream(any()) } doReturn Observable.empty()
        on { hasHyperlocalAddress() } doReturn Single.just(true)
    }

    private val productData = mock<SkuProductData> {
        on { sku } doReturn skuTestInstance()
    }
    private val errorHandler = mock<CommonErrorHandler>()
    private val router = mock<Router>() {
        on { currentScreen } doReturn Screen.SKU
    }
    private val metricaSender = mock<MetricaSender>()
    private val offerAnalyticsFacade = mock<OfferAnalyticsFacade>()

    @Suppress("DEPRECATION")
    private val analyticsService = mock<ru.yandex.market.analitycs.AnalyticsService>()
    private val extentionsFactory = mock<ProductExtensionFactory> {
        on { createWishListExtension(any()) } doReturn mock()
        on { createShareExtension(any()) } doReturn mock()
        on { createBottomBarExtension(any()) } doReturn mock()
        on { createComparisonExtension(any()) } doReturn mock()
    }
    private val qaEventFormatter = mock<ProductQaEventFormatter>()
    private val productHealthFacade = mock<ProductHealthFacade>()
    private val configuration = ProductPresenter.Configuration(progressDelay = 0.seconds, showUid = "showUid")

    private val firebaseEcommAnalyticsFacade = mock<FirebaseEcommAnalyticsFacade>()

    private val productUpperButtonAnalytics = mock<ProductUpperButtonAnalytics>()

    private val financialProductFormatter = mock<FinancialProductFormatter> {
        on { formatOffer(any(), any(), any(), any(), any(), any()) } doReturn null
    }

    private val installmentPickerFormatter = mock<InstallmentPickerFormatter>()

    private val shopInShopBottomBarVoFormatter = mock<ShopInShopBottomBarVoFormatter>()

    private val serviceFormattter = mock<ServiceFormatter>()
    private val stationSubscriptionAnalyticsManager = mock<StationSubscriptionVisibilityAnalyticsManager>()
    private val shopActualizedDeliveryFormatter = mock<ShopActualizedDeliveryFormatter>()
    private val outOfStockFilterFeatureManager = mock<OutOfStockProductKmFilterFeatureManager> {
        on { isFilterEnabled() } doReturn Single.just(false)
    }

    private val presenter = ProductPresenter(
        schedulers = schedulers,
        useCases = useCases,
        initialProductId = productId,
        commonErrorHandler = errorHandler,
        router = router,
        metricaSender = metricaSender,
        configuration = configuration,
        analyticsService = analyticsService,
        arguments = arguments,
        qaEventFormatter = qaEventFormatter,
        offerAnalyticsFacade = offerAnalyticsFacade,
        productHealthFacade = productHealthFacade,
        extensionFactory = extentionsFactory,
        firebaseEcommAnalyticsFacade = firebaseEcommAnalyticsFacade,
        productUpperButtonAnalytics = productUpperButtonAnalytics,
        financialProductFormatter = financialProductFormatter,
        installmentPickerFormatter = installmentPickerFormatter,
        realtimeSignalTransport = mock(),
        serviceFormatter = serviceFormattter,
        shopInShopBottomBarVoFormatter = shopInShopBottomBarVoFormatter,
        stationSubscriptionVisibilityAnalyticsManager = stationSubscriptionAnalyticsManager,
        shopActualizedDeliveryFormatter = shopActualizedDeliveryFormatter,
        eatsRetailAnalytics = mock(),
        sisCartNavigationDelegate = mock(),
        shopInShopAnalytics = mock(),
        flexationCardFeatureManager = mock(),
        outOfStockFilterFeatureManager = outOfStockFilterFeatureManager,
    )

    @Before
    fun setup() {
        presenter.attachView(productView)
    }

    @Test
    fun `Health event send if sku not loaded with runtime error`() {
        val error = NullPointerException()
        val expectedEvent = HealthEvent.builder()
            .name(HealthName.SKU_SHOW_ERROR)
            .portion(HealthPortion.SKU_SCREEN)
            .level(HealthLevel.ERROR)
            .info(SkuLoadFailedInfo(error))
            .build()
        productStream.onError(error)
        verify(analyticsService).report(expectedEvent)
    }

    @Test
    fun `Health event send if sku not loaded with not network cause communication error`() {
        val error = CommunicationException(Response.BAD_REQUEST)
        val expectedEvent = HealthEvent.builder()
            .name(HealthName.SKU_SHOW_ERROR)
            .portion(HealthPortion.SKU_SCREEN)
            .level(HealthLevel.ERROR)
            .info(SkuLoadFailedInfo(error))
            .build()
        productStream.onError(error)
        verify(analyticsService).report(expectedEvent)
    }

    @Test
    fun `Health event not send if sku not loaded with network error`() {
        val error = CommunicationException(Response.NETWORK_ERROR)
        val unExpectedEvent = HealthEvent.builder()
            .name(HealthName.SKU_SHOW_ERROR)
            .portion(HealthPortion.SKU_SCREEN)
            .level(HealthLevel.ERROR)
            .info(SkuLoadFailedInfo(error))
            .build()
        productStream.onError(error)
        verify(analyticsService, never()).report(unExpectedEvent)
    }

    @Test
    fun `Test sku Shown Event`() {
        productStream.onNext(productData)
        verify(productView).notifyProductDataLoaded(any(), any())
    }
}
