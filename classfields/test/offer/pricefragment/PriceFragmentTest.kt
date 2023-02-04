package ru.auto.ara.test.offer.pricefragment

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.offercard.performPriceFragment
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity


class PrivateSellerClickOnCloseIconPriceTest : PriceFragmentTest("1083103750-31ad88") {
    @Test
    fun shouldCloseFragmentWhenTappingOnCloseIcon() {
        performPriceFragment {}.checkResult { isTitleDisplayed() }

        performPriceFragment { clickCloseIcon() }
        performOfferCard { }.checkResult { isOfferCard() }
    }
}

class PrivateSellerNotInFavoritePriceTest : PriceFragmentTest("1083103750-31ad88") {
    @Test
    fun shouldDisplayPriceHistoryOfPrivateSeller() {
        performPriceFragment {}.checkResult {
            isTitleDisplayed()
            isCloseIconDisplayed()
            isPriceHistoryItemsTheSame()
            isSubscribeButtonDisplayed(getResourceString(R.string.subscribe_to_change))
        }
    }
}

class PrivateSellerCheckFavBtnPriceTest : PriceFragmentTest("1083103750-31ad88") {

    @Test
    fun shouldAddToFavorites() {
        performPriceFragment {}.checkResult {
            isTitleDisplayed()
            isSubscribeButtonDisplayed(getResourceString(R.string.subscribe_to_change))
        }
        performPriceFragment { clickAddToFavButton() }
        webServerRule.routing { userSetup() }
        performLogin { loginWithPhoneAndCode() }
        performOfferCard { clickPriceArrow() }
        performPriceFragment {}.checkResult {
            isTitleDisplayed()
            isSubscribeButtonDisplayed(getResourceString(R.string.unsubscribe))
        }
    }
}

class SalonPriceTest : PriceFragmentTest("1083280948-dc2c56") {
    @Test
    fun shouldShowSalonCard() {
        performPriceFragment {}.checkResult {
            isTitleDisplayed()
            isCloseIconDisplayed()
            isDiscountBadgeDisplayed()
            isRurPriceDisplayed("от 4 185 000 \u20BD")
            isDiscountBadgeDisplayed()
            isCurrencyPriceDisplayed("4 485 000 \u20BD   •   \$ 70,330   •   € 61,785")
            isDiscountTitleDisplayed()
            isDiscountOptionDisplayed(
                getResourceString(R.string.discount_credit),
                "до 100 000 \u20BD",
                R.color.auto_success_emphasis_high
            )
            isDiscountOptionDisplayed(
                getResourceString(R.string.discount_tradein),
                "до 150 000 \u20BD",
                R.color.auto_success_emphasis_high
            )
            isDiscountOptionDisplayed(
                getResourceString(R.string.discount_casco),
                "до 130 000 \u20BD",
                R.color.auto_success_emphasis_high
            )
            isDiscountOptionDisplayed(
                getResourceString(R.string.discount_max),
                "300 000 \u20BD",
                R.color.auto_success_emphasis_high,
                getResourceString(R.string.discount_max_descr)
            )
            isCallOrChatButtonDisplayed()
        }
    }
}

@RunWith(AndroidJUnit4::class)
abstract class PriceFragmentTest(private val offerId: String) {

    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    protected val webServerRule = WebServerRule {
        delegateDispatcher(GetOfferDispatcher.getOffer("cars", offerId))
        postLoginOrRegisterSuccess()
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule()
    )

    @Before
    fun setUp() {
        activityTestRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/$offerId")
        performOfferCard {}.checkResult { isOfferCard() }
        performOfferCard { clickPriceArrow() }
    }
}
