package ru.yandex.market.clean.presentation.feature.cart

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.processors.PublishProcessor
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analytics.facades.AddGiftToCartAnalytics
import ru.yandex.market.analytics.facades.CartNotificationsAnalytics
import ru.yandex.market.analytics.facades.CartPageAnalytics
import ru.yandex.market.analytics.facades.CartPartialPurchaseAnalytics
import ru.yandex.market.analytics.facades.CartThresholdAnalytics
import ru.yandex.market.analytics.facades.CartYandexPlusAnalytics
import ru.yandex.market.analytics.facades.CartZelenkaSupplierAnalytics
import ru.yandex.market.analytics.facades.ChangeCartItemCountAnalyticsFacade
import ru.yandex.market.analytics.facades.CreditInfoAnalytics
import ru.yandex.market.analytics.facades.DeleteItemFromCartAnalyticsFacade
import ru.yandex.market.analytics.facades.LavkaAnalytics
import ru.yandex.market.analytics.facades.ServicesAnalyticsFacade
import ru.yandex.market.analytics.facades.ShopInShopAnalyticsFacade
import ru.yandex.market.analytics.facades.health.CheckoutSpeedHealthFacade
import ru.yandex.market.analytics.facades.health.PurchaseByListHealthFacade
import ru.yandex.market.analytics.firebase.FirebaseEcommAnalyticsFacade
import ru.yandex.market.analytics.offer.OfferAnalyticsFacade
import ru.yandex.market.analytics.speed.SpeedService
import ru.yandex.market.checkout.data.CheckoutTimeMetricHelper
import ru.yandex.market.clean.domain.model.cart.CartEvent
import ru.yandex.market.clean.domain.model.cart.CartPlusCommunication
import ru.yandex.market.clean.domain.model.cart.CartPlusInfo
import ru.yandex.market.clean.domain.model.cart.CartValidationAffectingData
import ru.yandex.market.clean.domain.model.cart.cartValidationAffectingDataTestInstance
import ru.yandex.market.clean.domain.model.cart.cartValidationResultTestInstance
import ru.yandex.market.clean.domain.model.cms.CartCmsData
import ru.yandex.market.clean.presentation.error.CommonErrorHandler
import ru.yandex.market.clean.presentation.error.ErrorHealthFacade
import ru.yandex.market.clean.presentation.feature.cart.analytics.OrderButtonAnalyticsDataProvider
import ru.yandex.market.clean.presentation.feature.cart.formatter.CartCoinsFormatter
import ru.yandex.market.clean.presentation.feature.cart.formatter.CartFormatter
import ru.yandex.market.clean.presentation.feature.cart.formatter.CartItemsMergeFormatter
import ru.yandex.market.clean.presentation.feature.cart.formatter.CartNotificationFormatter
import ru.yandex.market.clean.presentation.feature.cart.formatter.CartPlusInfoFormatter
import ru.yandex.market.clean.presentation.feature.cart.formatter.CreateOrderErrorFormatter
import ru.yandex.market.clean.presentation.feature.cart.formatter.NewCheckoutButtonFormatter
import ru.yandex.market.clean.presentation.feature.cart.formatter.OutletPromoCodeFormatter
import ru.yandex.market.clean.presentation.feature.cart.vo.BottomBarVo
import ru.yandex.market.clean.presentation.feature.cart.vo.CartItemVo
import ru.yandex.market.clean.presentation.feature.cart.vo.CartVo
import ru.yandex.market.clean.presentation.feature.cart.vo.NewCheckoutButtonVo
import ru.yandex.market.clean.presentation.feature.purchaseByList.mapper.PurchaseByListOrderMapper
import ru.yandex.market.clean.presentation.formatter.InstallmentPickerFormatter
import ru.yandex.market.clean.presentation.formatter.MoneyFormatter
import ru.yandex.market.clean.presentation.formatter.error.checkout.CheckoutErrorFormatter
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.common.errors.ErrorVoFormatter
import ru.yandex.market.common.featureconfigs.models.ShopInShopPopupFlowConfig
import ru.yandex.market.domain.service.model.OfferSelectedServiceInfo
import ru.yandex.market.domain.service.model.offerSelectedServiceInfoTestInstance
import ru.yandex.market.feature.manager.CartPartialPurchaseFeatureManager
import ru.yandex.market.feature.manager.CartStickyButtonFeatureManager
import ru.yandex.market.feature.manager.CheckoutAnalogsInCartFeatureManager
import ru.yandex.market.feature.manager.MultipleCartPromoCodesFeatureManager
import ru.yandex.market.feature.manager.ShopInShopPopupFlowFeatureManager
import ru.yandex.market.optional.Optional
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.service.sync.SyncServiceMediator
import ru.yandex.market.utils.seconds

class CartPresenterTest {

    private val schedulers = presentationSchedulersMock()
    private val useCases = mock<CartUseCases> {
        on {
            getCartAffectingData(any(), any(), any())
        } doReturn Observable.just(cartValidationAffectingDataTestInstance())
        on {
            getActualizedCart(any(), any(), any(), any())
        } doReturn Single.just(cartValidationResultTestInstance())
        on { getOrderCreateEventStream() } doReturn Observable.empty()
        on { getCheckoutErrors() } doReturn Observable.empty()
        on { getCurrentAccountStream() } doReturn Observable.empty()
        on { getSupportPhoneNumberStream() } doReturn Observable.empty()
        on { getCmsWidgets() } doReturn Single.just(CartCmsData(emptyList(), null))
        on { dropLoyaltyStatus() } doReturn Completable.complete()
        on { resetCheckoutFlow() } doReturn Completable.complete()
        on { syncCartItems() } doReturn Completable.complete()
        on { getCartPlusInfoStream() } doReturn Observable.just(CartPlusInfo.NonLogin)
        on { getCartPlusInfoCommunication(any()) } doReturn Single.just(CartPlusCommunication.WITHOUT_COMMUNICATION)
        on { getCartItemsFromCache() } doReturn Single.never()
        on { getOfferSelectedServiceInfoStream() } doReturn Observable.never()
        on { observeLavkaRootAnalyticsInfo() } doReturn Observable.empty()
        on { getCartItemsStream() } doReturn Observable.just(Optional.of(emptyList()))
        on { observeSingleCartInMulticart() } doReturn Observable.empty()
        on { enableEatsRetailsCartsActualizer() } doReturn Completable.complete()
        on { actualizeAllRetailCarts() } doReturn Completable.complete()
        on { getUpsellWidgets() } doReturn Single.just(emptyList())
        on { getLavkaUpsale() } doReturn Single.just(emptyList())
    }
    private val cartFormatter = mock<CartFormatter>()
    private val router = mock<Router>()
    private val checkoutParamsMapper = mock<CheckoutParamsMapper>()
    private val commonErrorHandler = mock<CommonErrorHandler>()
    private val cartErrorHandler = mock<CartErrorHandler>()
    private val cartNotificationFormatter = mock<CartNotificationFormatter>()
    private val createOrderErrorFormatter = mock<CreateOrderErrorFormatter>()
    private val cartCoinsFormatter = mock<CartCoinsFormatter>()
    private val cartAnalyticsSender = mock<CartAnalyticsSender>()
    private val moneyFormatter = mock<MoneyFormatter>()
    private val resourcesDataStore = mock<ResourcesManager>()
    private val checkoutErrorFormatter = mock<CheckoutErrorFormatter>()
    private val newCheckoutButtonFormatter = mock<NewCheckoutButtonFormatter>()
    private val cartItemsMergeFormatter = mock<CartItemsMergeFormatter>()
    private val creditInfoAnalytics = mock<CreditInfoAnalytics>()
    private val servicesAnalytics = mock<ServicesAnalyticsFacade>()
    private val configuration = mock<CartPresenter.Configuration> {
        on { cmsRetryDelay } doReturn 10.seconds
    }

    @Suppress("DEPRECATION")
    private val analyticsService = mock<ru.yandex.market.analitycs.AnalyticsService>()
    private val checkoutTimeMetricHelper = mock<CheckoutTimeMetricHelper>()
    private val healthAnalyticsSender = mock<SpeedService>()

    private val addGiftToCartDialogArgumentsMapper = mock<AddGiftToCartDialogArgumentsMapper>()
    private val addGiftToCartAnalytics = mock<AddGiftToCartAnalytics>()
    private val cartNotificationsAnalytics = mock<CartNotificationsAnalytics>()
    private val cartPageAnalytics = mock<CartPageAnalytics>()
    private val offerAnalyticsFacade = mock<OfferAnalyticsFacade>()
    private val cartYandexPlusAnalytics = mock<CartYandexPlusAnalytics>()
    private val cartThresholdAnalytics = mock<CartThresholdAnalytics>()
    private val lavkaAnalytics = mock<LavkaAnalytics>()
    private val cartPartialPurchaseAnalytics = mock<CartPartialPurchaseAnalytics>()
    private val cartPlusInfoFormatter = mock<CartPlusInfoFormatter>()
    private val cartPartialPurchaseFeatureManager = mock<CartPartialPurchaseFeatureManager> {
        on { isEnabled() } doReturn false
        on { isDisabled() } doReturn true
    }
    private val multipleCartPromoCodesFeatureManager = mock<MultipleCartPromoCodesFeatureManager> {
        on { isMultipleCartPromoCodesEnabled() } doReturn false
    }

    private val shopInShopPopupFlowFeatureManager = mock<ShopInShopPopupFlowFeatureManager> {
        on { isEnabledSync() } doReturn ShopInShopPopupFlowConfig(false, -1)
    }

    private val cartCheckoutButtonVo = mock<NewCheckoutButtonVo> {
        on { bottomBarVo } doReturn BottomBarVo.EmptyButtonVo
    }

    private val cartVo: CartVo = mock {
        on { cartCheckoutButtonVo } doReturn cartCheckoutButtonVo
    }

    private val syncServiceMediator = mock<SyncServiceMediator>()

    private val firebaseEcommAnalyticsFacade = mock<FirebaseEcommAnalyticsFacade>()

    private val errorVoFormatter = mock<ErrorVoFormatter>()

    private val metricErrorHealth = mock<ErrorHealthFacade>()

    private val installmentPickerFormatter = mock<InstallmentPickerFormatter>()

    private val deleteItemFromCartAnalyticsFacade = mock<DeleteItemFromCartAnalyticsFacade>()

    private val changeCartItemCountAnalyticsFacade = mock<ChangeCartItemCountAnalyticsFacade>()

    private val zelenkaSupplierAnalytics = mock<CartZelenkaSupplierAnalytics>()

    private val purchaseByListOrderMapper = mock<PurchaseByListOrderMapper>()
    private val purchaseByHealthFacade = mock<PurchaseByListHealthFacade>()
    private val shopInShopAnalyticsFacade = mock<ShopInShopAnalyticsFacade>()

    private val checkoutAnalogsInCartFeatureManager = mock<CheckoutAnalogsInCartFeatureManager>()

    private val orderButtonAnalyticsDataProvider = mock<OrderButtonAnalyticsDataProvider>()

    private val cartStickyButtonFeatureManager = mock<CartStickyButtonFeatureManager>()

    private val outletPromoCodeFormatter = mock<OutletPromoCodeFormatter>()

    private val checkoutSpeedHealthFacade = mock<CheckoutSpeedHealthFacade>()

    private val presenter = CartPresenter(
        schedulers = schedulers,
        useCasesLazy = { useCases },
        cartFormatterProvider = { cartFormatter },
        router = router,
        checkoutParamsMapper = checkoutParamsMapper,
        commonErrorHandler = commonErrorHandler,
        cartErrorHandlerProvider = { cartErrorHandler },
        cartNotificationFormatter = cartNotificationFormatter,
        createOrderErrorFormatter = { createOrderErrorFormatter },
        cartPlusInfoFormatter = cartPlusInfoFormatter,
        cartCoinsFormatter = cartCoinsFormatter,
        cartAnalyticsSender = cartAnalyticsSender,
        moneyFormatter = moneyFormatter,
        resourcesManager = resourcesDataStore,
        checkoutErrorFormatter = checkoutErrorFormatter,
        newCheckoutButtonFormatter = newCheckoutButtonFormatter,
        cartItemsMergeFormatter = { cartItemsMergeFormatter },
        configuration = configuration,
        analyticsService = analyticsService,
        checkoutTimeMetricHelper = checkoutTimeMetricHelper,
        speedService = healthAnalyticsSender,
        addGiftToCartDialogArgumentsMapper = addGiftToCartDialogArgumentsMapper,
        addGiftToCartAnalytics = { addGiftToCartAnalytics },
        cartNotificationsAnalytics = { cartNotificationsAnalytics },
        cartThresholdAnalytics = { cartThresholdAnalytics },
        cartPageAnalytics = { cartPageAnalytics },
        cartPartialPurchaseAnalytics = { cartPartialPurchaseAnalytics },
        offerAnalyticsFacade = offerAnalyticsFacade,
        syncServiceMediator = syncServiceMediator,
        cartYandexPlusAnalytics = cartYandexPlusAnalytics,
        servicesAnalytics = servicesAnalytics,
        creditInfoAnalytics = creditInfoAnalytics,
        multipleCartPromoCodesFeatureManager = multipleCartPromoCodesFeatureManager,
        cartPartialPurchaseFeatureManager = cartPartialPurchaseFeatureManager,
        firebaseEcommAnalyticsFacade = firebaseEcommAnalyticsFacade,
        lavkaAnalytics = lavkaAnalytics,
        installmentPickerFormatter = installmentPickerFormatter,
        errorVoFormatter = errorVoFormatter,
        errorMetrica = metricErrorHealth,
        deleteItemFromCartAnalyticsFacade = deleteItemFromCartAnalyticsFacade,
        changeCartItemCountAnalyticsFacade = changeCartItemCountAnalyticsFacade,
        lavkaCartButtonDelegate = mock(),
        zelenkaSupplierAnalytics = { zelenkaSupplierAnalytics },
        purchaseByListOrderMapper = { purchaseByListOrderMapper },
        outletPromoCodeFormatter = { outletPromoCodeFormatter },
        purchaseByListHealthFacade = purchaseByHealthFacade,
        eatsRetailHealthFacade = mock(),
        shopInShopAnalyticsFacade = shopInShopAnalyticsFacade,
        checkoutAnalogsInCartFeatureManager = checkoutAnalogsInCartFeatureManager,
        shopInShopPopupFlowFeatureManager = shopInShopPopupFlowFeatureManager,
        orderButtonAnalyticsDataProvider = orderButtonAnalyticsDataProvider,
        cartStickyButtonFeatureManager = { cartStickyButtonFeatureManager },
        checkoutSpeedHealthFacade = checkoutSpeedHealthFacade,
        cartEventProcessor = PublishProcessor.create<CartEvent>().toSerialized(),
    )

    @Test
    fun `Don't actualize cart when cart is not in foreground`() {
        val subject = PublishSubject.create<CartValidationAffectingData>()
        whenever(useCases.getCartAffectingData(any(), any(), any())).thenReturn(subject)
        whenever(useCases.isBnplEnabled()).thenReturn(Single.fromCallable { true })
        whenever(useCases.getCartBusinessGroupsStream()).thenReturn(Observable.just(emptyList()))
        val view = mock<CartView>()

        presenter.attachView(view)
        presenter.detachView(view)
        subject.onNext(cartValidationAffectingDataTestInstance())

        verify(useCases, never()).getActualizedCart(any(), any(), any(), any())
    }

    @Test
    fun `Actualize cart when it is in foreground`() {
        val subject = PublishSubject.create<CartValidationAffectingData>()
        whenever(useCases.getCartAffectingData(any(), any(), any())).thenReturn(subject)
        whenever(useCases.isBnplEnabled()).thenReturn(Single.fromCallable { true })
        whenever(useCases.getCartBusinessGroupsStream()).thenReturn(Observable.just(emptyList()))
        val view = mock<CartView>()

        presenter.attachView(view)
        subject.onNext(cartValidationAffectingDataTestInstance())

        verify(useCases, times(1)).getActualizedCart(any(), any(), any(), any())
    }

    @Test
    fun `Actualize cart when it was got in foreground`() {
        val subject = PublishSubject.create<CartValidationAffectingData>()
        whenever(useCases.getCartAffectingData(any(), any(), any())).thenReturn(subject)
        whenever(useCases.isBnplEnabled()).thenReturn(Single.fromCallable { true })
        whenever(useCases.getCartBusinessGroupsStream()).thenReturn(Observable.just(emptyList()))
        val view = mock<CartView>()

        presenter.attachView(view)
        subject.onNext(cartValidationAffectingDataTestInstance())
        presenter.detachView(view)
        verify(useCases, times(1)).getActualizedCart(any(), any(), any(), any())

        presenter.attachView(view)

        verify(useCases, times(2)).getActualizedCart(any(), any(), any(), any())
    }

    @Test
    fun `Test Cart Item Visible Event`() {
        val vo = mock<CartItemVo>() {
            on { isInStock } doReturn true
        }
        presenter.onCartItemVisible(vo)

        verify(offerAnalyticsFacade).cartItemVisibleEvent(vo)
    }

    @Test
    fun `Test Cart Item Navigate Event`() {
        val vo = mock<CartItemVo>() {
            on { isInStock } doReturn true
            on { skuId } doReturn "id"
            on { modelId } doReturn "id"
            on { persistentOfferId } doReturn "id"
        }
        presenter.selectItem(vo)

        verify(offerAnalyticsFacade).cartItemNavigateEvent(vo)
    }

    @Test
    fun `Update cart items on service change`() {
        val subject = PublishSubject.create<OfferSelectedServiceInfo>()
        whenever(useCases.getOfferSelectedServiceInfoStream()).thenReturn(subject)
        whenever(useCases.isBnplEnabled()).thenReturn(Single.fromCallable { true })
        whenever(useCases.getCartBusinessGroupsStream()).thenReturn(Observable.just(emptyList()))
        whenever(
            cartFormatter.format(
                lastCart = anyOrNull(),
                lastCartVo = anyOrNull(),
                affectingData = any(),
                selectedByUserData = anyOrNull(),
                isActualization = any(),
                supportPhoneNumber = anyOrNull(),
                isFraudDetected = any(),
                needShowPromoCodeStatusMessage = anyOrNull(),
                appliedCoinsCount = any(),
                hiddenBundleTypes = any(),
                cartPlusInfo = any(),
                businessGroups = any(),
                lavketPageId = any(),
                upsellComplementaryConfig = anyOrNull(),
            )
        ).thenReturn(cartVo)
        whenever(
            cartVo.copy(cartCheckoutButtonVo = cartCheckoutButtonVo)
        ).thenReturn(cartVo)

        val offerIdToServiceId = offerSelectedServiceInfoTestInstance()
        val view = mock<CartView>()

        presenter.attachView(view)
        subject.onNext(offerIdToServiceId)

        verify(useCases).setSelectedService(any(), any())
    }

    @Test
    fun `Unsubscribe from service changes when in background`() {
        val subject = PublishSubject.create<OfferSelectedServiceInfo>()
        whenever(useCases.getOfferSelectedServiceInfoStream()).thenReturn(subject)
        whenever(useCases.isBnplEnabled()).thenReturn(Single.fromCallable { true })
        whenever(useCases.getCartBusinessGroupsStream()).thenReturn(Observable.just(emptyList()))

        val offerIdToServiceId = offerSelectedServiceInfoTestInstance()
        val view = mock<CartView>()

        presenter.attachView(view)
        presenter.detachView(view)
        subject.onNext(offerIdToServiceId)

        verify(useCases, never()).setSelectedService(any(), any())
    }
}
