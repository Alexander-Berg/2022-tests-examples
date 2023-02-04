package ru.auto.ara.test.offer.gallery

import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.matchers.ViewMatchers.withClearText
import ru.auto.ara.core.matchers.ViewMatchers.withCompoundDrawable
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity


@RunWith(AndroidJUnit4::class)
class CardGalleryTest {

    private val OFFER_CARD_PATH = "https://auto.ru/cars/used/sale/"
    private val OFFERID_CAR_WITHOUT_PHOTO = "1089249352-3dc864f3"
    private val dispatchers: List<DelegateDispatcher> = listOf(
        GetOfferDispatcher.getOffer("cars", OFFERID_CAR_WITHOUT_PHOTO)
    )

    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        SetPreferencesRule()
    )

    @Before
    fun setUp() {
        activityTestRule.launchDeepLinkActivity(OFFER_CARD_PATH + OFFERID_CAR_WITHOUT_PHOTO)
    }

    @Test
    fun shouldSeeStubInOfferCardWithoutPhoto() {
        performOfferCard {
            interactions.onGalleryRecycler().waitUntil(
                hasChildCount(1),
                hasDescendant(allOf(withClearText("Нет фото"), withId(R.id.public_offer_stub))),
                hasDescendant(withCompoundDrawable(R.drawable.ic_car_placeholder))
            )
        }
    }

    @Test
    fun shouldStayOnCardWhenTapOnStub() {
        performOfferCard {
            interactions.onGalleryRecycler().performClick()
        }.checkResult {
            isOfferCard()
        }
    }
}
