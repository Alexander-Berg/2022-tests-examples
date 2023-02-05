package ru.yandex.market.ui.view.mvp.cartcounterbutton

import com.annimon.stream.Optional
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analitycs.events.orders.AddToCartAnalyticsParamsMapper
import ru.yandex.market.analytics.facades.AddToCartButtonAnalytics
import ru.yandex.market.analytics.facades.CartCounterAnalytics
import ru.yandex.market.analytics.facades.ChangeCartItemCountAnalyticsFacade
import ru.yandex.market.analytics.facades.CreditInfoAnalytics
import ru.yandex.market.analytics.facades.DeleteItemFromCartAnalyticsFacade
import ru.yandex.market.analytics.facades.MiscellaneousAnalyticsFacade
import ru.yandex.market.analytics.facades.ServicesAnalyticsFacade
import ru.yandex.market.analytics.facades.health.AddToCartHealthFacade
import ru.yandex.market.analytics.firebase.FirebaseEcommAnalyticsFacade
import ru.yandex.market.analytics.mapper.CreditInfoAnalyticMapper
import ru.yandex.market.analytics.offer.OfferAnalyticsFacade
import ru.yandex.market.clean.domain.model.FoodtechType
import ru.yandex.market.clean.domain.model.cart.AddToCartEvent
import ru.yandex.market.clean.domain.model.cartItemTestInstance
import ru.yandex.market.clean.domain.model.offerTestInstance
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import ru.yandex.market.clean.presentation.feature.cart.CartTargetScreen
import ru.yandex.market.clean.presentation.feature.cartbutton.AddToCartNotificationsDelegate
import ru.yandex.market.clean.presentation.feature.pricedrop.PriceDropOffersTargetScreen
import ru.yandex.market.clean.presentation.feature.sis.seemoredialog.ShopInShopSeeMoreDialogTargetScreen
import ru.yandex.market.clean.presentation.formatter.MoneyFormatter
import ru.yandex.market.clean.presentation.formatter.OfferPromoFormatter
import ru.yandex.market.clean.presentation.formatter.ShopInShopPageIdFormatter
import ru.yandex.market.clean.presentation.navigation.NavigationDelegate
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.common.errors.ErrorVoFormatter
import ru.yandex.market.common.featureconfigs.managers.MinimumOrderToggleManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider
import ru.yandex.market.db.addCartItemsResultTestInstance
import ru.yandex.market.domain.hyperlocal.model.HyperlocalAddress
import ru.yandex.market.feature.manager.EatsRetailFeatureManager
import ru.yandex.market.feature.timer.ui.TimersProvider
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.test.extensions.asSingle
import ru.yandex.market.ui.view.mvp.cartcounterbutton.adult.CartCounterAdultDelegate
import ru.yandex.market.ui.view.mvp.cartcounterbutton.hyperlocal.CartCounterHyperlocalDelegate
import ru.yandex.market.ui.view.mvp.cartcounterbutton.order.CartCounterOrderDelegate
import ru.yandex.market.ui.view.mvp.cartcounterbutton.realtime.CartCounterRealtimeSignalDelegate

class CartCounterPresenterTest {

    private val useCases = mock<CartCounterUseCases> {
        on { getCartItemChangesStream(any(), any(), any(), any()) } doReturn Observable.never()
        on { getCartItemBySkuId(any(), any(), any()) } doReturn Maybe.empty()
        on { isFirstOrder() } doReturn Single.just(false)
        on { getShopInShopSeeMoreAvailable(any()) } doReturn Single.just(false)
        on { getCartItemsBySkuId(any(), any()) } doReturn Single.just(emptyList())
        on { getOffer(any(), any(), any()) } doReturn productOfferTestInstance(
            offer = offerTestInstance(
                isExpressDelivery = false,
                isSample = false,
                foodtechType = FoodtechType.MARKET,
            ),
            foodtechType = FoodtechType.MARKET,
        ).asSingle()
        on { getArgumentsChangesStream() } doReturn Observable.never()
        on {
            addOfferToCart(
                offer = any(),
                price = any(),
                count = any(),
                promotionalOffers = any(),
                alternativeOfferReason = anyOrNull(),
                selectedServiceId = anyOrNull(),
                isDirectShopInShop = any(),
                shopInShopPageId = anyOrNull(),
            )
        } doReturn addCartItemsResultTestInstance().asSingle()
        on { isPriceDropGrantedByOffer(any(), any(), any()) } doReturn false.asSingle()
        on { getSelectedServiceInfoStreamForOffer(any()) } doReturn Observable.never()
        on { getSisTunnelingEnabled() } doReturn Single.just(true)
        on { getIsDirectSisBusinessId(any()) } doReturn Single.just(false)
        on { getCurrentHyperlocalAddress() } doReturn Single.just(HyperlocalAddress.Absent)
        on { getCartItems() } doReturn Single.just(emptyList())
    }
    private val router = mock<Router> {
        on { currentScreen } doReturn Screen.CART
        on { currentFlow } doReturn Optional.empty()
    }
    private val schedulers = presentationSchedulersMock()
    private val navigationDelegate = mock<NavigationDelegate> {
        on { currentFlow } doReturn Optional.empty()
    }
    private val arguments = spy(
        cartCounterArgumentsTestInstance(
            primaryOffer = cartCounterArguments_OfferTestInstance(
                isPreorder = false,
                isAdultOffer = false,
                isSample = false,
            ),
            lavkaRedirectDialogParams = null
        )
    )

    @Suppress("DEPRECATION")
    private val analyticsService = mock<ru.yandex.market.analitycs.AnalyticsService>()
    private val cartButtonFormatter = mock<CartButtonFormatter>()
    private val offerAnalyticsFacade = mock<OfferAnalyticsFacade>()
    private val flashSaleTimersProvider = mock<TimersProvider> {
        on { getTimer(any(), any()) } doReturn Observable.never()
    }
    private val view = mock<CartCounterView>()
    private val minimumOrderToggleManager = mock<MinimumOrderToggleManager> {
        on { getFromCacheOrDefault() } doReturn FeatureToggle(isEnabled = false)
    }
    private val eatsRetailToggleManager = mock<EatsRetailFeatureManager> {
        on { isEnabled() } doReturn Single.just(true)
    }
    private val featureConfigProvider = mock<FeatureConfigsProvider>().also { provider ->
        whenever(provider.minimumOrderToggleManager) doReturn minimumOrderToggleManager
    }
    private val notificationDelegate = mock<AddToCartNotificationsDelegate>()
    private val addToCartButtonAnalytics = mock<AddToCartButtonAnalytics>()
    private val cartCounterAnalyticsSender = mock<CartCounterAnalyticsSender>()
    private val appMetricaAnalytics = mock<CartCounterAnalytics>()
    private val offerPromoFormatter = mock<OfferPromoFormatter>()
    private val resourcesDataStore = mock<ResourcesManager>()
    private val creditInfoAnalytics = mock<CreditInfoAnalytics>()
    private val servicesAnalytics = mock<ServicesAnalyticsFacade>()
    private val moneyFormatter = mock<MoneyFormatter>()
    private val firebaseEcommAnalyticsFacade = mock<FirebaseEcommAnalyticsFacade>()
    private val errorVoFormatter = mock<ErrorVoFormatter>()
    private val addToCartAnalyticsParamsMapper = mock<AddToCartAnalyticsParamsMapper>()
    private val miscellaneousAnalyticsFacade = mock<MiscellaneousAnalyticsFacade>()
    private val creditInfoAnalyticMapper = mock<CreditInfoAnalyticMapper>()
    private val realtimeSignalDelegate = mock<CartCounterRealtimeSignalDelegate>()
    private val deleteItemFromCartAnalyticsFacade = mock<DeleteItemFromCartAnalyticsFacade>()
    private val changeCartItemCountAnalyticsFacade = mock<ChangeCartItemCountAnalyticsFacade>()
    private val addToCartHealthFacade = mock<AddToCartHealthFacade>()
    private val cartCounterHyperlocalDelegate = mock<CartCounterHyperlocalDelegate> {
        on { hasHyperlocalAddress() } doReturn Single.just(true)
        on {
            checkHyperlocalAddressForOffer(any(), any(), any(), any(), any())
        } doAnswer {
            it.getArgument<() -> Unit>(1).invoke()
        }
    }
    private val cartCounterAdultDelegate = mock<CartCounterAdultDelegate>()
    private val cartCounterOrderDelegate = mock<CartCounterOrderDelegate>()
    private val shopInShopPageIdFormatter = mock<ShopInShopPageIdFormatter>()
    private val cartCounterCalculator = mock<CartCounterCalculator> {
        on {
            calculateItemCountForIncrease(
                isCartEmpty = any(),
                currentCountInCart = any(),
                stepCount = any(),
                minItemCount = any(),
                availableCountInStock = any(),
            )
        } doReturn 1
    }

    val presenter = CartCounterPresenter(
        schedulers = schedulers,
        router = router,
        addToCartButtonAnalytics = addToCartButtonAnalytics,
        useCases = useCases,
        navigationDelegate = navigationDelegate,
        arguments = arguments,
        analyticsService = analyticsService,
        cartButtonFormatter = cartButtonFormatter,
        featureConfigsProvider = featureConfigProvider,
        offerAnalyticsFacade = offerAnalyticsFacade,
        timersProvider = flashSaleTimersProvider,
        notificationsDelegate = notificationDelegate,
        cartCounterAnalyticsSender = cartCounterAnalyticsSender,
        appMetricaAnalytics = appMetricaAnalytics,
        offerPromoFormatter = offerPromoFormatter,
        resourcesManager = resourcesDataStore,
        creditInfoAnalytics = creditInfoAnalytics,
        servicesAnalytics = servicesAnalytics,
        moneyFormatter = moneyFormatter,
        firebaseEcommAnalyticsFacade = firebaseEcommAnalyticsFacade,
        errorVoFormatter = errorVoFormatter,
        addToCartAnalyticsParamsMapper = addToCartAnalyticsParamsMapper,
        miscellaneousAnalyticsFacade = miscellaneousAnalyticsFacade,
        creditInfoAnalyticMapper = creditInfoAnalyticMapper,
        realtimeSignalDelegate = { realtimeSignalDelegate },
        deleteItemFromCartAnalyticsFacade = deleteItemFromCartAnalyticsFacade,
        changeCartItemCountAnalyticsFacade = changeCartItemCountAnalyticsFacade,
        addToCartHealthFacade = addToCartHealthFacade,
        eatsRetailFeatureManager = eatsRetailToggleManager,
        cartCounterHyperlocalDelegate = cartCounterHyperlocalDelegate,
        cartCounterAdultDelegate = cartCounterAdultDelegate,
        cartCounterOrderDelegate = cartCounterOrderDelegate,
        shopInShopPageIdFormatter = shopInShopPageIdFormatter,
        cartCounterCalculator = cartCounterCalculator
    )

    @Before
    fun init() {
        whenever(navigationDelegate.currentScreen) doReturn Screen.SKU
    }

    @Test
    fun `Add item to cart on button click when current state is not in cart`() {
        whenever(router.currentScreen) doReturn Screen.SKU

        presenter.attachView(view)
        presenter.onButtonClick()

        verify(useCases).addOfferToCart(
            offer = any(),
            price = any(),
            count = any(),
            promotionalOffers = any(),
            alternativeOfferReason = anyOrNull(),
            selectedServiceId = anyOrNull(),
            isDirectShopInShop = any(),
            shopInShopPageId = anyOrNull(),
        )
    }

    @Test
    fun `Go to cart on button click when current state is in cart`() {
        whenever(useCases.getCartItemBySkuId(any(), any(), any())) doReturn Maybe.just(
            cartItemTestInstance(
                offer = productOfferTestInstance(
                    offer = offerTestInstance(
                        isPreorder = false,
                        isAdult = false,
                        isSample = false,
                    )
                )
            )
        )

        presenter.attachView(view)
        presenter.onButtonClick(canGoToCartInCounterExp = true)

        verify(navigationDelegate).navigateTo(any<CartTargetScreen>())
    }

    @Test
    fun `Check alternative Offer Added To Cart Event`() {
        whenever(router.currentScreen) doReturn Screen.UNKNOWN
        presenter.attachView(view)
        presenter.itemCountSubject.onNext(CartCounterPresenter.ItemCountChangeInfo(1, true))
        verify(offerAnalyticsFacade).alternativeOfferAddedToCartEvent(
            arguments.cartCounterAnalytics.multiOfferAnalyticsParam!!,
            Screen.UNKNOWN
        )
    }

    @Test
    fun `Add Retail item to cart (first time) - Show only ShopInShopSeeMoreDialogTargetScreen`() {
        whenever(useCases.getShopInShopSeeMoreAvailable(any())) doReturn Single.just(true)
        whenever(useCases.getOffer(any(), any(), any())) doReturn productOfferTestInstance(
            offer = offerTestInstance(
                isExpressDelivery = false,
                isSample = false,
                foodtechType = FoodtechType.RETAIL,
            ),
            foodtechType = FoodtechType.RETAIL,
        ).asSingle()

        presenter.attachView(view)
        presenter.onButtonClick(canGoToCartInCounterExp = true)

        verify(router).navigateTo(any<ShopInShopSeeMoreDialogTargetScreen>())
        verify(router, times(0)).navigateTo(any<PriceDropOffersTargetScreen>())
        verify(notificationDelegate, times(0)).handleAddToCartEvent(any(), anyOrNull(), any())
    }

    @Test
    fun `Add Retail item to cart (second time) - Invoke only handleAddToCartEvent`() {
        whenever(useCases.getShopInShopSeeMoreAvailable(any())) doReturn Single.just(false)
        whenever(useCases.getOffer(any(), any(), any())) doReturn productOfferTestInstance(
            offer = offerTestInstance(
                isExpressDelivery = false,
                isSample = false,
                foodtechType = FoodtechType.RETAIL,
            ),
            foodtechType = FoodtechType.RETAIL,
        ).asSingle()

        presenter.attachView(view)
        presenter.onButtonClick(canGoToCartInCounterExp = true)

        verify(router, times(0)).navigateTo(any<ShopInShopSeeMoreDialogTargetScreen>())
        verify(router, times(0)).navigateTo(any<PriceDropOffersTargetScreen>())
        verify(notificationDelegate, times(1)).handleAddToCartEvent(any(), anyOrNull(), any())
    }

    @Test
    fun `Add Retail item to cart (first time) on SiS - Don't show PriceDropOffersTargetScreen or ShopInShopSeeMoreDialogTargetScreen`() {
        whenever(router.currentScreen) doReturn Screen.SHOP_IN_SHOP_FLOW
        whenever(useCases.getShopInShopSeeMoreAvailable(any())) doReturn Single.just(true)
        whenever(useCases.getOffer(any(), any(), any())) doReturn productOfferTestInstance(
            offer = offerTestInstance(
                isExpressDelivery = false,
                isSample = false,
                foodtechType = FoodtechType.RETAIL,
            ),
            foodtechType = FoodtechType.RETAIL,
        ).asSingle()

        presenter.attachView(view)
        presenter.onButtonClick(canGoToCartInCounterExp = true)

        verify(router, times(0)).navigateTo(any<ShopInShopSeeMoreDialogTargetScreen>())
        verify(router, times(0)).navigateTo(any<PriceDropOffersTargetScreen>())
        verify(notificationDelegate, times(0)).handleAddToCartEvent(any(), anyOrNull(), any())
    }

    @Test
    fun `Add Express item to cart (first time) - Show only ShopInShopSeeMoreDialogTargetScreen`() {
        whenever(useCases.getShopInShopSeeMoreAvailable(any())) doReturn Single.just(true)
        whenever(useCases.getOffer(any(), any(), any())) doReturn productOfferTestInstance(
            offer = offerTestInstance(
                isExpressDelivery = true,
                isSample = false,
                foodtechType = FoodtechType.MARKET,
            ),
            foodtechType = FoodtechType.MARKET,
        ).asSingle()

        presenter.attachView(view)
        presenter.onButtonClick(canGoToCartInCounterExp = true)

        verify(router).navigateTo(any<ShopInShopSeeMoreDialogTargetScreen>())
        verify(router, times(0)).navigateTo(any<PriceDropOffersTargetScreen>())
        verify(notificationDelegate, times(0)).handleAddToCartEvent(any(), anyOrNull(), any())
    }

    @Test
    fun `Add Express item to cart (second time) - Invoke only handleAddToCartEvent`() {
        whenever(useCases.getShopInShopSeeMoreAvailable(any())) doReturn Single.just(false)
        whenever(useCases.getOffer(any(), any(), any())) doReturn productOfferTestInstance(
            offer = offerTestInstance(
                isExpressDelivery = true,
                isSample = false,
                foodtechType = FoodtechType.MARKET,
            ),
            foodtechType = FoodtechType.MARKET,
        ).asSingle()

        presenter.attachView(view)
        presenter.onButtonClick(canGoToCartInCounterExp = true)

        verify(router, times(0)).navigateTo(any<ShopInShopSeeMoreDialogTargetScreen>())
        verify(router, times(0)).navigateTo(any<PriceDropOffersTargetScreen>())
        verify(notificationDelegate, times(1)).handleAddToCartEvent(any(), anyOrNull(), any())
    }

    @Test
    fun `Add Express item to cart (first time) on SiS - Don't show PriceDropOffersTargetScreen or ShopInShopSeeMoreDialogTargetScreen`() {
        whenever(router.currentScreen) doReturn Screen.SHOP_IN_SHOP_FLOW
        whenever(useCases.getShopInShopSeeMoreAvailable(any())) doReturn Single.just(true)
        whenever(useCases.getOffer(any(), any(), any())) doReturn productOfferTestInstance(
            offer = offerTestInstance(
                isExpressDelivery = true,
                isSample = false,
                foodtechType = FoodtechType.MARKET,
            ),
            foodtechType = FoodtechType.MARKET,
        ).asSingle()

        presenter.attachView(view)
        presenter.onButtonClick(canGoToCartInCounterExp = true)

        verify(router, times(0)).navigateTo(any<ShopInShopSeeMoreDialogTargetScreen>())
        verify(router, times(0)).navigateTo(any<PriceDropOffersTargetScreen>())
        verify(notificationDelegate, times(0)).handleAddToCartEvent(any(), anyOrNull(), any())
    }

    @Test
    fun `Add non-Retail item to cart - Show only PriceDropOffersFragment`() {
        whenever(useCases.getShopInShopSeeMoreAvailable(any())) doReturn Single.just(true)

        presenter.attachView(view)
        presenter.onButtonClick(canGoToCartInCounterExp = true)

        verify(notificationDelegate).handleAddToCartEvent(any<AddToCartEvent.Native>(), anyOrNull(), any())
        verify(router, times(0)).navigateTo(any<ShopInShopSeeMoreDialogTargetScreen>())
    }
}
