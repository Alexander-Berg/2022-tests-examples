package com.yandex.mobile.realty.test.publicationForm

import android.content.Intent
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.PublicationFormActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.robot.performOnChatMessagesScreen
import com.yandex.mobile.realty.core.robot.performOnInfoDialog
import com.yandex.mobile.realty.core.robot.performOnPublicationFormScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.UserOfferDraftRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.viewMatchers.NamedIntentMatcher
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author solovevai on 07.06.2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class OpenFormTest : BasePublishFormTest() {

    private val authorizationRule = AuthorizationRule()
    private var activityTestRule = PublicationFormActivityTestRule(launchActivity = false)
    private val draftRule = UserOfferDraftRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        authorizationRule,
        SetupDefaultAppStateRule(),
        activityTestRule,
        draftRule
    )

    @Before
    fun setUp() {
        authorizationRule.setUserAuthorized()
    }

    @Test
    fun shouldShowUserBlocked() {
        configureWebServer {
            registerTechSupportChat()
            registerBlockedUserProfile()
            registerBlockedUserProfile()
        }

        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { isBlockedUserViewShown() }
            collapseAppBar()

            isErrorViewStateMatches("/OpenFormTest/shouldShowUserBlocked")

            tapOn(lookup.matchesContactSupportButton())
        }

        performOnChatMessagesScreen {
            waitUntil { isSupportChatTitleShown() }
        }
    }

    @Test
    fun checkAllSellApartmentFieldsExist() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
        registerResultOkIntent(matchesVideoRequirementsIntent(), null)
        registerResultOkIntent(matchesPublicationAgreementIntent(), null)
        registerResultOkIntent(matchesPublicationTermsIntent(), null)

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil {
                hasSellApartmentCollapsedToolbarTitle()
                waitUntil { doesNotContainMosRuBlock() }
            }
            isContentMatches("/OpenFormTest/checkAllSellApartmentFieldsExist/content")
            scrollToTop()

            scrollToPosition(lookup.matchesApartmentNumberWhyToFillLink()).tapOn()
            performOnInfoDialog {
                waitUntil { isDialogShown() }
                isViewSateMatches("/OpenFormTest/apartmentNumberDialog")
                onView(lookup.matchesCloseButton()).tapOn()
            }

            scrollToPosition(lookup.matchesVideoUrlFieldDescription())
                .tapOnLinkText("рекомендациями по\u00A0подготовке ролика")
            intended(matchesVideoRequirementsIntent())

            scrollToPosition(lookup.matchesTermsText()).tapOnLinkText("условиями размещения")
            intended(matchesPublicationAgreementIntent())

            scrollToPosition(lookup.matchesTermsText())
                .tapOnLinkText("правилами публикации объявлений")
            intended(matchesPublicationTermsIntent())
        }
    }

    @Test
    fun checkAllRentLongApartmentFieldsExist() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        draftRule.prepareRentLongApartment()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil {
                hasRentLongApartmentCollapsedToolbarTitle()
                waitUntil { doesNotContainMosRuBlock() }
            }
            isContentMatches("/OpenFormTest/checkAllRentLongApartmentFieldsExist/content")
        }
    }

    @Test
    fun checkAllRentShortApartmentFieldsExist() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        draftRule.prepareRentShortApartment()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentShortApartmentCollapsedToolbarTitle() }
            isContentMatches("/OpenFormTest/checkAllRentShortApartmentFieldsExist/content")
        }
    }

    @Test
    fun checkAllSellRoomFieldsExist() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        draftRule.prepareSellRoom()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellRoomExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellRoomCollapsedToolbarTitle() }
            isContentMatches("/OpenFormTest/checkAllSellRoomFieldsExist/content")
        }
    }

    @Test
    fun checkAllRentLongRoomFieldsExist() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        draftRule.prepareRentLongRoom()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongRoomExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongRoomCollapsedToolbarTitle() }
            isContentMatches("/OpenFormTest/checkAllRentLongRoomFieldsExist/content")
        }
    }

    @Test
    fun checkAllRentShortRoomFieldsExist() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        draftRule.prepareRentShortRoom()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortRoomExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentShortRoomCollapsedToolbarTitle() }
            isContentMatches("/OpenFormTest/checkAllRentShortRoomFieldsExist/content")
        }
    }

    @Test
    fun checkAllSellHouseFieldsExist() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        draftRule.prepareSellHouse()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellHouseExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellHouseCollapsedToolbarTitle() }
            isContentMatches("/OpenFormTest/checkAllSellHouseFieldsExist/content")
        }
    }

    @Test
    fun checkAllRentLongHouseFieldsExist() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        draftRule.prepareRentLongHouse()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongHouseExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongHouseCollapsedToolbarTitle() }
            isContentMatches("/OpenFormTest/checkAllRentLongHouseFieldsExist/content")
        }
    }

    @Test
    fun checkAllRentShortHouseFieldsExist() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        draftRule.prepareRentShortHouse()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortHouseExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentShortHouseCollapsedToolbarTitle() }
            isContentMatches("/OpenFormTest/checkAllRentShortHouseFieldsExist/content")
        }
    }

    @Test
    fun checkAllSellLotFieldsExist() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        draftRule.prepareSellLot()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellLotExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellLotCollapsedToolbarTitle() }
            isContentMatches("/OpenFormTest/checkAllSellLotFieldsExist/content")
        }
    }

    @Test
    fun checkAllSellGarageFieldsExist() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        draftRule.prepareSellGarage()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellGarageExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellGarageCollapsedToolbarTitle() }
            isContentMatches("/OpenFormTest/checkAllSellGarageFieldsExist/content")
        }
    }

    @Test
    fun checkAllRentLongGarageFieldsExist() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        draftRule.prepareRentLongGarage()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongGarageExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongGarageCollapsedToolbarTitle() }
            isContentMatches("/OpenFormTest/checkAllRentLongGarageFieldsExist/content")
        }
    }

    private fun matchesVideoRequirementsIntent(): Matcher<Intent> {
        return NamedIntentMatcher(
            "Открытие страницы с рекомендациями по подготовке ролика",
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData("https://yandex.ru/support/realty/rules/requirements-ads.html")
            )
        )
    }

    private fun matchesPublicationAgreementIntent(): Matcher<Intent> {
        return NamedIntentMatcher(
            "Открытие страницы c условиями размещения объявлений",
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData("https://yandex.ru/legal/realty_agreement")
            )
        )
    }

    private fun matchesPublicationTermsIntent(): Matcher<Intent> {
        return NamedIntentMatcher(
            "Открытие страницы с правилами публикации объявлений",
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData("https://yandex.ru/support/realty/advertise/good-ad.html")
            )
        )
    }

    private fun DispatcherRegistry.registerTechSupportChat() {
        register(
            request {
                path("2.0/chat/room/tech-support")
            },
            response {
                assetBody("techSupportChatCommon.json")
            }
        )
    }
}
