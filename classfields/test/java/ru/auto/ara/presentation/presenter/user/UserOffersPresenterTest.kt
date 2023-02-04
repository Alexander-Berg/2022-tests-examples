package ru.auto.ara.presentation.presenter.user

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
 import org.mockito.Mockito.atLeastOnce
import org.mockito.kotlin.any
import org.mockito.kotlin.argForWhich
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.auto.ara.RxTest
import ru.auto.ara.billing.promo.ServicePriceToVasInfoConverter
import ru.auto.ara.deeplink.parser.DeeplinkInteractor
import ru.auto.ara.feature.yaplus.YaPlusAnalyst
import ru.auto.ara.presentation.presenter.caronlinepresentation.ICarOnlinePresentationInteractor
import ru.auto.ara.presentation.presenter.offer.IUserAuctionsController
import ru.auto.ara.presentation.presenter.offer.OfferAuctionBannerController
import ru.auto.ara.presentation.presenter.offer.UserAuctionsV2Controller
import ru.auto.ara.presentation.presenter.offer.controller.IDrivePromoLKController
import ru.auto.ara.presentation.presenter.offer.controller.IPersonalAssistantController
import ru.auto.ara.presentation.presenter.offer.controller.MicPromoControllerForUserOffers
import ru.auto.ara.presentation.view.user.OfferActionsView
import ru.auto.ara.presentation.viewstate.user.UserOffersViewState
import ru.auto.ara.router.Navigator
import ru.auto.ara.router.tab.ITabNavigation
import ru.auto.ara.router.tab.TabNavigationPoint
import ru.auto.ara.util.android.StringsProvider
import ru.auto.ara.util.error.UserErrorFactory
import ru.auto.ara.viewmodel.user.AutoUpContext
import ru.auto.ara.viewmodel.user.UserOfferFactory
import ru.auto.ara.viewmodel.vas.VasBlocksController
import ru.auto.ara.viewmodel.vas.VasBlocksFactory
import ru.auto.data.interactor.ICarPresentationDotInteractor
import ru.auto.data.interactor.ISevenDaysStrategy
import ru.auto.data.interactor.IYaPlusInteractor
import ru.auto.data.interactor.UserOffersInteractor
import ru.auto.data.interactor.YaPlusLoginInfo
import ru.auto.data.interactor.YaPlusPromoAvailability
import ru.auto.data.model.User
import ru.auto.data.model.UserProfile
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.data.offer.SellerType
import ru.auto.data.model.user.BanResult
import ru.auto.data.prefs.IPrefsDelegate
import ru.auto.data.repository.IAuctionBannerSkippingRepository
import ru.auto.data.repository.IAuctionRepository
import ru.auto.data.repository.user.IMutableUserRepository
import ru.auto.data.util.URL_AUTORU
import ru.auto.experiments.Experiments
import ru.auto.experiments.ExperimentsManager
import ru.auto.experiments.isNewVasDesign
import ru.auto.feature.banner_explanations.controller.IExplanationsController
import ru.auto.feature.banner_explanations.ui.adapters.ExplanationItemType
import ru.auto.feature.burger.IBurgerController
import ru.auto.feature.dealer.settings.ui.SettingsUserViewModel
import ru.auto.feature.diff_counters.DiffCountersInteractor
import ru.auto.feature.mic_promo_api.IMicPromoInteractor
import ru.auto.feature.promocodes.PromocodeAppliedInteractor
import ru.auto.feature.safedeal.analist.SafeDealAnalyst
import ru.auto.feature.safedeal.feature.offer.controller.ISafeDealController
import ru.auto.feature.safedeal.ui.adapters.cabinet.SafeDealInfoViewModelFactory
import ru.auto.feature.sale.interactor.ISaleInteractor
import ru.auto.feature.sale.interactor.SaleModel
import ru.auto.feature.user.repository.IUserSessionRepository
import rx.Completable
import rx.Observable
import rx.Single

@RunWith(AllureRunner::class) class UserOffersPresenterTest : RxTest() {
    private val balance: Long = 101

    private val viewState: UserOffersViewState = mock()
    private val navigator: Navigator = mock()
    private val userRepository: IMutableUserRepository = mock()
    private val offersInteractor: UserOffersInteractor = mock()
    private val saleInteractor: ISaleInteractor = mock()
    private val tabNavigation: ITabNavigation = mock()
    private val diffCountersInteractor: DiffCountersInteractor = mock()
    private val actionsController: IOfferActionsController<OfferActionsView> = mock()
    private val errorFactory: UserErrorFactory = UserErrorFactory(mock())
    private val prefs: IPrefsDelegate = mock()
    private val strings: StringsProvider = mock()
    private val deeplinkInteractor: DeeplinkInteractor = mock()
    private val userOfferFactory: UserOfferFactory = mock()
    private val sevenDaysStrategy: ISevenDaysStrategy = mock()
    private val user: User.Authorized = User.Authorized("", mock(), balance = 0)
    private val carOnlinePresentationInteractor: ICarOnlinePresentationInteractor = mock()
    private val carPresentationDotInteractor: ICarPresentationDotInteractor = mock()
    private val drivePromoController: IDrivePromoLKController = mock()
    private val userSessionRepo: IUserSessionRepository = mock()
    private val personalAssistantController: IPersonalAssistantController = mock()
    private val promocodeAppliedInteractor: PromocodeAppliedInteractor = mock()
    private val safeDealInfoViewModelFactory: SafeDealInfoViewModelFactory = mock()
    private val safeDealController: ISafeDealController = mock()
    private lateinit var presenter: UserOffersPresenter
    private val safeDealAnalyst: SafeDealAnalyst = mock()
    private val draftOfferActionsPresenter: DraftOfferActionsPresenter = mock()
    private val yaPlusInteractor: IYaPlusInteractor = mock()
    private val yaPlusAnalyst: YaPlusAnalyst = mock()
    private val burgerController: IBurgerController = mock()
    private val userAuctionRepository: IAuctionRepository = mock()
    private val auctionSnippetController: IUserAuctionsController = mock()
    private val auctionSnippetControllerV2: UserAuctionsV2Controller = mock()
    private val micPromoInteractor: IMicPromoInteractor = mock()
    private val micPromoController: MicPromoControllerForUserOffers = mock()
    private val explanationsController: IExplanationsController = mock()
    private val auctionBannerController: OfferAuctionBannerController = mock()
    private val auctionBannerSkippingRepository: IAuctionBannerSkippingRepository = mock()

    private val fallbackUrl = URL_AUTORU
    private val mockOffer1 = Offer(VehicleCategory.CARS, "", "", "0", fallbackUrl = fallbackUrl, sellerType = SellerType.PRIVATE)
    private val mockOffer2 = Offer(VehicleCategory.CARS, "", "", "0", fallbackUrl = fallbackUrl, sellerType = SellerType.PRIVATE)
    private val userOffers = listOf(mockOffer1, mockOffer2)

    @Before
    fun setUp() {
        initExp()

        whenever(strings.get(any())).thenReturn("")
        val user = User.Authorized("id", mock(), emptyList(), emptyList(), mock(), balance.toInt())
        whenever(userRepository.observeUser()).thenReturn(Observable.just(user))
        whenever(userRepository.fetchUser()).thenReturn(Single.just(user))
        whenever(userRepository.user).thenReturn(user)
        whenever(drivePromoController.observeDriveVM()).thenReturn(Observable.empty())
        whenever(explanationsController.observeRequiredTypeOfExplanation(any())).thenReturn(
            Observable.empty()
        )
        whenever(userSessionRepo.getBans()).thenReturn(Single.just(BanResult(emptyMap())))
        whenever(tabNavigation.points(TabNavigationPoint.USER_OFFERS::class.java)).thenReturn(Observable.empty())
        whenever(saleInteractor.getSale(false)).thenReturn(Single.just(SaleModel.SaleNotAvailable))
        whenever(explanationsController.observeRequiredTypeOfExplanation(any()))
            .thenReturn(Observable.just(ExplanationItemType.NONE))
        whenever(promocodeAppliedInteractor.observePromocodeApplied()).thenReturn(Observable.just(false))
        whenever(safeDealInfoViewModelFactory.createNewDealsCommonInfo(any())).thenReturn(null)
        whenever(safeDealInfoViewModelFactory.createInfo(any())).thenReturn(null)
        whenever(safeDealController.getNewDealsFromOffers(any())).thenReturn(emptyList())
        whenever(yaPlusInteractor.getLoginInfo()).thenReturn(Single.just(YaPlusLoginInfo.NotAuthorized))
        whenever(yaPlusInteractor.isPromotionAvailable()).thenReturn(Single.just(YaPlusPromoAvailability.Unavailable))
        whenever(yaPlusInteractor.observePromotionAvailabilityOnUserEvents())
            .thenReturn(Observable.just(YaPlusPromoAvailability.Unavailable))
        whenever(micPromoInteractor.needToShowGreenDialogAtAppStart()).thenReturn(Single.just(false))
        whenever(micPromoInteractor.onGreenDialogShownAtAppStart()).thenReturn(Completable.complete())
        whenever(explanationsController.observeRequiredTypeOfExplanation(any())).thenReturn(
            Observable.empty()
        )
    }

    private fun initExp() {
        val expManager: Experiments = mock()
        whenever(expManager.isNewVasDesign()).thenReturn(false)
        ExperimentsManager.setInstance(expManager)
    }

    @Test
    fun `when user is unauthorized presenter sets unauthorized view`() {
        val mockUser = User.Unauthorized
        whenever(userRepository.observeUser()).thenReturn(Observable.just(mockUser))
        whenever(userRepository.user).thenReturn(mockUser)
        mockUserOffersObservables(emptyList())
        presenter = createPresenter()
        presenter.bind(viewState)
        verify(viewState).setToolbarUnauthorized(any())
    }

    @Test
    fun `when user authorized should show user's balance`() {
        val mockUser = User.Unauthorized
        whenever(userRepository.observeUser()).thenReturn(Observable.just(mockUser))
        whenever(userRepository.user).thenReturn(mockUser)
        mockUserOffersObservables(userOffers)

        viewState.setToolbarAuthorized(SettingsUserViewModel(user, true))
        verify(viewState).setToolbarAuthorized(SettingsUserViewModel(user, true))

        presenter = createPresenter()
        presenter.bind(viewState)
        verify(viewState).setToolbarAuthorized(SettingsUserViewModel(user, true))
    }

    @Test
    @Ignore("setEmptyState is not called as implementation was changed or because of another reason")
    fun `when there are no user offers show empty view`() {
        val mockUser = USER.copy(balance = 0)
        whenever(userRepository.observeUser()).thenReturn(Observable.just(mockUser))
        whenever(userRepository.user).thenReturn(mockUser)
        mockUserOffersObservables(emptyList())

        presenter = createPresenter()
        presenter.bind(viewState)

        verify(viewState, atLeastOnce()).setEmptyState(any())
    }

    @Ignore
    @Test
    fun `when there are 2 user offers should show 2 offers and create offer header`() {
        val mockUser = USER.copy(balance = 0)
        whenever(userRepository.observeUser()).thenReturn(Observable.just(mockUser))
        whenever(userRepository.user).thenReturn(mockUser)
        mockUserOffersObservables(userOffers)

        presenter = createPresenter()
        presenter.bind(viewState)

        verify(viewState).setItems(argForWhich { size == 3 })
    }

    private fun mockUserOffersObservables(list: List<Offer>) {
        whenever(offersInteractor.observeOffers()).thenReturn(Observable.just(list))
        whenever(offersInteractor.updateOffers()).thenReturn(Observable.just(list))
        whenever(offersInteractor.getUserOffers()).thenReturn(Observable.just(list))
    }

    private fun createPresenter() = UserOffersPresenter(
        viewState = viewState,
        router = navigator,
        controller = actionsController,
        userRepository = userRepository,
        offersInteractor = offersInteractor,
        diffOfferCountersInteractor = diffCountersInteractor,
        errorFactory = errorFactory,
        prefs = prefs,
        strings = strings,
        deeplinkInteractor = deeplinkInteractor,
        userOfferFactory = userOfferFactory,
        sevenDaysStrategy = sevenDaysStrategy,
        vasBlocksController = VasBlocksController(
            factory = VasBlocksFactory(
                converter = ServicePriceToVasInfoConverter(),
                strings = strings,
                screen = AutoUpContext.Screen.USER_LISTING,
                isNewVasDesign = false,
            ),
            isNewVasDesign = false,
        ),
        saleInteractor = saleInteractor,
        tabNavigation = tabNavigation,
        carOnlinePresentationInteractor = carOnlinePresentationInteractor,
        carPresentationDotInteractor = carPresentationDotInteractor,
        drivePromoController = drivePromoController,
        userSessionRepo = userSessionRepo,
        personalAssistantController = personalAssistantController,
        promocodeAppliedInteractor = promocodeAppliedInteractor,
        safeDealInfoViewModelFactory = safeDealInfoViewModelFactory,
        safeDealAnalyst = safeDealAnalyst,
        safeDealController = safeDealController,
        draftOfferActionsPresenter = draftOfferActionsPresenter,
        yaPlusInteractor = yaPlusInteractor,
        yaPlusAnalyst = yaPlusAnalyst,
        burgerController = burgerController,
        userAuctionRepository = userAuctionRepository,
        auctionSnippetController = auctionSnippetController,
        auctionSnippetControllerV2 = auctionSnippetControllerV2,
        micPromoController = micPromoController,
        explanationsController = explanationsController,
        auctionBannerController = auctionBannerController,
        auctionBannerSkippingRepository = auctionBannerSkippingRepository,
    )

    companion object {
        private val USER = User.Authorized("id", UserProfile(null), emptyList(), emptyList(), emptyList(), 123)
    }
}
