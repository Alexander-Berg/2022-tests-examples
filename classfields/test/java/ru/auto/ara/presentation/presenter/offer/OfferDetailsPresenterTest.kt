package ru.auto.ara.presentation.presenter.offer

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.junit.Test
import org.junit.runner.RunWith
 import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.auto.ara.presentation.presenter.offer.controller.OfferComparisonController
import ru.auto.ara.util.android.StubStringsProvider
import ru.auto.ara.util.error.OfferDetailsErrorFactory
import ru.auto.ara.viewmodel.feed.snippet.factory.DealerDiscountType
import ru.auto.data.interactor.IWhatsAppChatInteractor
import ru.auto.data.interactor.OfferDetailsInteractor
import ru.auto.data.model.User
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.common.IComparableItem
import ru.auto.data.model.data.offer.Deal
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.data.offer.OfferRegionModel
import ru.auto.data.model.data.offer.SafeDealBuyerCancellationReason
import ru.auto.data.model.stat.SearchId
import ru.auto.data.repository.user.IUserRepository
import ru.auto.feature.appreview.ReviewCoordinationDelegate
import ru.auto.feature.safedeal.feature.offer.controller.ISafeDealController
import ru.auto.test.core.RxTestAndroid
import rx.Completable
import rx.Observable
import rx.Single

@RunWith(AllureRunner::class) class OfferDetailsPresenterTest: RxTestAndroid() {

    @Test
    fun `should trigger rate review when presenter created on public offer`() {
        val reviewDelegate: ReviewCoordinationDelegate = mock()
        createPresenter(reviewDelegate, isUserOffer = false)
        verify(reviewDelegate, times(1)).tryToShowReviewDialogFromOffer()
    }

    @Test
    fun `should not trigger rate review when presenter created on user offer`() {
        val reviewDelegate: ReviewCoordinationDelegate = mock()
        createPresenter(reviewDelegate, isUserOffer = true)
        verify(reviewDelegate, never()).tryToShowReviewDialogFromOffer()
    }

    private fun createPresenter(
        reviewCoordinationDelegate: ReviewCoordinationDelegate,
        isUserOffer: Boolean
    ): OfferDetailsPresenter {

        val model = OfferDetailsModel(
            bookingVmFactory = mock(),
            carfaxVMFactory = mock(),
            category = VehicleCategory.CARS.toString(),
            eventSource = null,
            isCurrentUserDealer = false,
            isDealerOffer = false,
            isDeeplink = false,
            isUserOffer = isUserOffer,
            offerId = "some_id",
            paidActivationViewModelFactory = mock(),
            safeDealFactory = mock(),
            serviceViewModelFactory = mock(),
            sevenDaysBlockFactory = mock(),
            strings = mock(),
            userOfferFactory = mock(),
            userRepository = mock(),
            vasConverter = mock(),
            vasBlocksController = mock(),
            region = OfferRegionModel(listOf(1), 100),
            searchId = SearchId.getEmptySearchId(),
            dealerDiscountType = DealerDiscountType.DEFAULT,
        )

        val offerDetailsErrorFactory = OfferDetailsErrorFactory(StubStringsProvider())

        val offerDetailsInteractor: OfferDetailsInteractor = mock()
        whenever(offerDetailsInteractor.offerUpdated(any(), any(), any())).thenReturn(Observable.empty())

        val comparisonsController: OfferComparisonController = mock()
        whenever(comparisonsController.observeUpdates()).thenReturn(Observable.empty())

        val userRepository: IUserRepository = mock()
        whenever(userRepository.observeUser()).thenReturn(Observable.just(User.Unauthorized))

        val whatsAppInteractor: IWhatsAppChatInteractor = mock()
        whenever(whatsAppInteractor.getWhatsAppNumber(anyOrNull(), any())).thenReturn(Single.just(null))

        return OfferDetailsPresenter(
            viewState = mock(),
            router = mock(),
            offerDetailsErrorFactory = offerDetailsErrorFactory,
            componentManager = mock(),
            offerDetailsInteractor = offerDetailsInteractor,
            diffOfferCountersInteractor = mock(),
            userOffersInteractor = mock(),
            model = model,
            stringsProvider = mock(),
            context = mock(),
            userRepository = userRepository,
            analystManager = mock(),
            manualController = mock(),
            advantagesController = mock(),
            reviewController = mock(),
            plusMinusController = mock(),
            callController = mock(),
            offerController = mock(),
            noteController = mock(),
            vasController = mock(),
            requestCallController = mock(),
            requestTradeInController = mock(),
            videoController = mock(),
            favoriteController = mock(),
            relatedOffersController = mock(),
            carfaxReportController = mock(),
            galleryController = mock(),
            promoController = mock(),
            menuController = mock(),
            editOfferController = mock(),
            socialAuthController = mock(),
            shareController = mock(),
            complainController = mock(),
            equipmentsController = mock(),
            damagesController = mock(),
            dealerVASController = mock(),
            notificationController = mock(),
            certController = mock(),
            adController = mock(),
            journalController = mock(),
            offerChangeContoller = mock(),
            oldLoanController = mock(),
            dealerController = mock(),
            loanCardDelegate = mock(),
            matchApplicationController = mock(),
            coordinator = mock(),
            inspectionController = mock(),
            sevenDaysStrategy = mock(),
            newCarsRedirectInteractor = mock(),
            bannerAdController = mock(),
            offerLoadController = mock(),
            scrollController = mock(),
            drivePromoController = mock(),
            bookingController = mock(),
            personalAssistantController = mock(),
            provenOwnerController = mock(),
            comparisonController = comparisonsController,
            sortItemFactory = mock(),
            screenHistoryRepository = mock(),
            savedSearchDelegatePresenter = mock(),
            sameButNewController = mock(),
            analyticsPanoramaOffer = mock(),
            safeDealController = createTestSafeDealController(),
            loanAnalyst = mock(),
            reviewCoordinationDelegate = reviewCoordinationDelegate,
            safeDealAnalyst = mock(),
            electricCarsAnalyst = mock(),
            goToGarageBannerController = mock(),
            whatsAppChatInteractor = whatsAppInteractor,
            resellerContactsController = mock(),
            offerAuctionBannerController = mock(),
            withDiscountExperimentAnalyst = mock(),
        )
    }

    private fun createTestSafeDealController() = object : ISafeDealController {
        override val showPromoObservable: Observable<Boolean> = Observable.just(false)

        override fun updateDealList(): Completable = Completable.complete()

        override fun observeDealByOfferId(offerId: String): Observable<Deal> = Observable.never()

        override fun observeNewDeals(): Observable<List<Deal>> = Observable.just(emptyList())

        override fun makeDeal(offerId: String, sellingPriceRub: Int): Completable = Completable.complete()

        override fun rejectDealByBuyer(
            dealId: String,
            reason: SafeDealBuyerCancellationReason,
            reasonDescription: String?,
        ): Completable = Completable.complete()

        override fun hasActiveDeals(offerId: String): Boolean = false

        override fun getNewDealsFromOffers(offers: List<Offer>): List<Deal> = emptyList()

        override fun saveAsViewedNewDealsFromOffers(offers: List<Offer>): Completable = Completable.complete()

        override fun createSafeDealStatusVM(deal: Deal): IComparableItem?  = null

        override fun createSafeDealAdsVM(offer: Offer): IComparableItem?  = null

        override fun isSafeDealMenuClicked(): Boolean  = false

        override fun setSafeDealMenuClicked(value: Boolean) {
            // Not implemented.
        }

        override fun showBuyerCancellationReasonChooser(dealId: String) {
            // Not implemented.
        }

        override fun showSellerOnboardingIfNeeded(): Completable = Completable.complete()

        override fun canShowOfferPromo(offer: Offer): Single<ISafeDealController.OfferPromoAction> =
            Single.just(ISafeDealController.OfferPromoAction.DO_NOTHING)

        override fun openLanding() {
            // Not implemented.
        }

    }

}
