package ru.auto.ara.test.offer.menu

import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForOffer
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForReportByOfferId
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.user.UserAssets
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.mapping.ssr.copyWithNewCarfaxResponse
import ru.auto.ara.core.mapping.ssr.getContentCell
import ru.auto.ara.core.matchers.ViewMatchers.withClearText
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.checkVinReport
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCardVin
import ru.auto.ara.core.robot.offercard.performVinReport
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class DownloadTest {
    private val PHONE = "+7 (000) 000-00-00"
    private val CODE = "0000"
    private val OFFER_ID = "1082957054-8d55bf9a"
    private val FILE_OFFER_ID = "1093024666-aa502a2a"

    private val webServerRule = WebServerRule {
        userSetup(UserAssets.TWO_PHONES)
        postLoginOrRegisterSuccess()
        delegateDispatchers(
            GetOfferDispatcher.getOffer("cars", OFFER_ID),
            RawCarfaxOfferDispatcher(
                requestOfferId = OFFER_ID,
                fileOfferId = FILE_OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT
            )
        )
        makeXmlForOffer(offerId = OFFER_ID, dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT)
        makeXmlForReportByOfferId(
            offerId = OFFER_ID,
            dirType = RawCarfaxOfferDispatcher.DirType.BOUGHT,
            mapper = { copyWithNewCarfaxResponse(content = listOf(getContentCell())) }
        )
    }

    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule()
    )

    @Before
    fun setup() {
        activityTestRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/$OFFER_ID")
    }

    @Test
    fun shouldNotSeeDownloadButtonWhenAnon() {
        performOfferCard {
            clickOpenMenuButton()
        }.checkResult {
            interactions.onMenuItemWithText("В избранное").waitUntilIsCompletelyDisplayed()
            interactions.onMenuItemWithText("Скачать объявление").checkNotExists()
        }
    }

    @Test
    fun shouldSeeDownloadPopupWhenAuthorized() {
        checkOfferCard {
            waitSomething(300, TimeUnit.MILLISECONDS)
            isOfferCard()
        }
        performOfferCardVin { clickShowFreeReport() }
        webServerRule.routing { userSetup() }
        performLogin { loginWithPhoneAndCode(PHONE, CODE) }
        checkVinReport { isVinReport() }
        waitSomething(300, TimeUnit.MILLISECONDS)
        performVinReport { close() }
        checkOfferCard { isOfferCard() }
        performOfferCard {
            clickOpenMenuButton()
            interactions.onMenuItemWithText("Скачать объявление").waitUntilIsCompletelyDisplayed().performClick()
        }.checkResult {
            interactions.onPopupCloseButton().waitUntil(isCompletelyDisplayed(), withClearText("Закрыть"))
            interactions.onPopupMessage()
                .waitUntil(isCompletelyDisplayed(), withClearText(R.string.download_offer_dialog_message))
        }
        performOfferCard {
            interactions.onPopupCloseButton().performClick()
        }.checkResult {
            isOfferCard()
        }
    }
}
