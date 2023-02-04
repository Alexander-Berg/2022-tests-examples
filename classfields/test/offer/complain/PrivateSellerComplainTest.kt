package ru.auto.ara.test.offer.complain

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.complaints.ComplaintsDispatcher
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.offer_card.GetSpecialsDispatcher
import ru.auto.ara.core.dispatchers.phones.GetPhonesDispatcher
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.ratecall.performRateCallExp
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DecreaseRateCallTimeRule
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.PRIVATE_SELLER_REASONS
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.intendingNotInternal
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.respondWithOk
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.core.utils.withIntents
import ru.auto.ara.deeplink.DeeplinkActivity
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class PrivateSellerComplainTest(private val testParameter: TestParameter) {
    private val URL_PREFIX = "https://auto.ru/cars/used/sale/"
    private val PRIVATE_SELLER_OFFER = "1082957054-8d55bf9a"
    private val complaintsWatcher = RequestWatcher()
    private val webServerRule = WebServerRule {
        delegateDispatchers(
            GetOfferDispatcher.getOffer(category = "cars", offerId = PRIVATE_SELLER_OFFER),
            GetPhonesDispatcher.onePhone(offerId = PRIVATE_SELLER_OFFER),
            ComplaintsDispatcher(offerId = PRIVATE_SELLER_OFFER, category = "cars", requestWatcher = complaintsWatcher),
            GetSpecialsDispatcher.specials()
        )
    }
    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule(),
        DecreaseRateCallTimeRule()
    )

    @Before
    fun setUp() {
        activityTestRule.launchDeepLinkActivity(URL_PREFIX + PRIVATE_SELLER_OFFER)
    }

    @Test
    fun shouldComplainFromMenu() {
        performOfferCard {
            clickOpenMenuButton()
            waitSomething(2, TimeUnit.SECONDS)
            clickMenuItemWithText(getResourceString(R.string.complain_full))
            interactions.onComplainName(testParameter.displayName).waitUntilIsCompletelyDisplayed().performClick()
        }.checkResult {
            complaintsWatcher.checkRequestBodyParameters(
                "reason" to testParameter.apiParam,
                paramComplainPlacementOfferMenu
            )
            complaintsWatcher.checkNotRequestBodyParameter("text")
        }
    }

    @Test
    fun shouldComplainFromButton() {
        performOfferCard {
            scrollToComplainPosition()
            waitSomething(1, TimeUnit.SECONDS)
            interactions.onComplainButton().waitUntilIsCompletelyDisplayed().performClick()
            interactions.onComplainName(testParameter.displayName).waitUntilIsCompletelyDisplayed().performClick()
        }.checkResult {
            interactions.onComplainButton().waitUntilIsCompletelyDisplayed()
            complaintsWatcher.checkRequestBodyParameters(
                "reason" to testParameter.apiParam,
                paramComplainPlacementOfferButton
            )
            complaintsWatcher.checkNotRequestBodyParameter("text")
        }
    }

    @Test
    fun shouldComplainFromRateCall() {
        withIntents {
            intendingNotInternal().respondWithOk(delay = 5)
            performOfferCard { interactions.onMakeCallOnCardButton().waitUntilIsCompletelyDisplayed().performClick() }
        }
        performRateCallExp { clickComplaint() }
        performOfferCard {
            interactions.onComplainName(testParameter.displayName).waitUntilIsCompletelyDisplayed().performClick()
        }.checkResult {
            isOfferCardTitle("BMW 4 серия 420d xDrive F32/F33/F36, 2016")
            complaintsWatcher.checkRequestBodyParameters(
                "reason" to testParameter.apiParam,
                paramComplainPlacementRateCall
            )
            complaintsWatcher.checkNotRequestBodyParameter("text")
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = REASONS.map { arrayOf(it) }

        private val REASONS = PRIVATE_SELLER_REASONS.map { (displayName, apiParam) ->
            TestParameter(
                displayName = displayName,
                apiParam = apiParam
            )
        }

        private val paramComplainPlacementOfferButton = "placement" to "offerCard"
        private val paramComplainPlacementOfferMenu = "placement" to "offerCard_menu"
        private val paramComplainPlacementRateCall = "placement" to "callRate"

        data class TestParameter(
            val displayName: String,
            val apiParam: String,
        ) {
            override fun toString() = apiParam
        }
    }
}
