package ru.auto.ara.test.user_offers.card

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.actions.step
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user_offers.GetUserOfferDispatcher
import ru.auto.ara.core.dispatchers.user_offers.PostActivateUserOffersDispatcher
import ru.auto.ara.core.dispatchers.user_offers.PutProductProlongDispatcher
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.useroffers.checkOffers
import ru.auto.ara.core.robot.useroffers.checkVasCatch
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.screenbundles.OfferCardBundles
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.core.utils.pressBack
import ru.auto.ara.ui.activity.OfferDetailsActivity
import ru.auto.ara.ui.fragment.offer.OfferDetailsFragment
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.network.scala.offer.converter.ServicePriceConverter.PROLONGATION_ADDITIONAL_TIME_SEC
import ru.auto.data.util.TEST_SEVEN_DAYS_TIMER

@RunWith(AndroidJUnit4::class)
class SevenDaysWithExpCardTest {
    private val activityTestRule = lazyActivityScenarioRule<OfferDetailsActivity>()

    private val userOffersDispatcherHolder = DispatcherHolder()
    private val userOfferDispatcherHolder = DispatcherHolder()
    private val putProductProlongRW = RequestWatcher()

    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            userOfferDispatcherHolder,
            userOffersDispatcherHolder,
            PostActivateUserOffersDispatcher("cars", OFFER_ID_WITHOUT_PROLONGATION),
            PutProductProlongDispatcher(
                category = "cars",
                offerId = OFFER_ID_WITHOUT_PROLONGATION,
                product = "all_sale_activate",
                requestWatcher = putProductProlongRW
            )
        )
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityTestRule,
        SetupTimeRule(date = "07.02.2020"),
        SetupAuthRule()
    )

    @Before
    fun before() {
        enableSevenDaysTimer(false)
    }

    @Test
    fun shouldSeeProlongationEnabledBlock() {
        openCard(OFFER_ID_WITH_PROLONGATION)
        performOfferCard {
            waitUntilOfferDetailsLoaded(OFFER_TITLE)
        }
        checkOffers {
            isProlongationEnabledBlockDisplayed(TIME_LEFT)
        }
    }

    @Test
    fun shouldSeeProlongationDisabledBlock() {
        openCard(OFFER_ID_WITHOUT_PROLONGATION)
        performOfferCard {
            waitUntilOfferDetailsLoaded(OFFER_TITLE)
        }
        checkOffers {
            isProlongationFailedBlockDisplayed(TIME_LEFT)
            isProlongationDisabledBlockDisplayed(TIME_LEFT)
        }
    }

    @Test
    fun shouldSeeSevenDaysInactiveBlock() {
        openCard(OFFER_ID_WITH_PROLONGATION, "${OFFER_ID_WITH_PROLONGATION}_inactive_seven_days")
        performOfferCard {
            waitUntilOfferDetailsLoaded(OFFER_TITLE)
        }
        checkOffers {
            isSevenDaysInactiveBlockDisplayed()
        }
    }

    @Test
    fun shouldNotSeeSevenDaysInactiveBlockIfNoDiscount() {
        openCard(OFFER_ID_WITH_PROLONGATION, "${OFFER_ID_WITH_PROLONGATION}_inactive_seven_days_without_discount")
        performOfferCard {
            waitUntilOfferDetailsLoaded(OFFER_TITLE)
        }
        checkOffers {
            isSevenDaysInactiveBlockNotExists()
        }
    }

    @Test
    fun shouldSeeSevenDaysWillExpireTimerBlock() {
        enableSevenDaysTimer(true)
        openCard(OFFER_ID_WITH_PROLONGATION, "${OFFER_ID_WITH_PROLONGATION}_inactive_seven_days")
        performOfferCard {
            waitUntilOfferDetailsLoaded(OFFER_TITLE)
        }
        checkOffers {
            isSevenDaysWillExpireTimerBlockDisplayed(timerSec = PROLONGATION_ADDITIONAL_TIME_SEC)
        }
    }

    @Test
    fun shouldNotEnableProlongationAfterActivation() {
        openCard(OFFER_ID_WITHOUT_PROLONGATION, "${OFFER_ID_WITHOUT_PROLONGATION}_inactive")
        performOfferCard {
            interactions.onUserOfferTitle(OFFER_TITLE).waitUntilIsCompletelyDisplayed()
            userOfferDispatcherHolder.innerDispatcher =
                GetUserOfferDispatcher(VehicleCategory.CARS, OFFER_ID_WITHOUT_PROLONGATION)
            interactions.onActivateButtonSevenDays().waitUntilIsCompletelyDisplayed().performClick()
        }
        checkVasCatch { interactions.onCarVasCatchTitle().waitUntilIsCompletelyDisplayed() }
        pressBack()
        performOfferCard { collapseAppBar() }
        checkOffers {
            isProlongationDisabledBlockDisplayed(TIME_LEFT)
            putProductProlongRW.checkRequestWasNotCalled()
        }
    }

    private fun openCard(offerId: String, mockFile: String? = offerId) = step("opening offer $offerId") {
        userOfferDispatcherHolder.innerDispatcher = GetUserOfferDispatcher(VehicleCategory.CARS, offerId, mockFile)
        activityTestRule.launchFragment<OfferDetailsFragment>(
            OfferCardBundles.userOfferBundle(
                category = VehicleCategory.CARS,
                offerId = offerId
            )
        )
    }

    private fun enableSevenDaysTimer(enable: Boolean) {
        TEST_SEVEN_DAYS_TIMER = enable
    }

    companion object {
        private var OFFER_ID_WITHOUT_PROLONGATION = "1095669442-b39897242"
        private var OFFER_ID_WITH_PROLONGATION = "1095669442-b3989724"
        private var OFFER_TITLE = "Chery Bonus (A13), 2013"
        private var TIME_LEFT = "6 дней"
    }
}
