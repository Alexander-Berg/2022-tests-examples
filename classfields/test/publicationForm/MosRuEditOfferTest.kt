package com.yandex.mobile.realty.test.publicationForm

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.PublicationFormEditActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.assertion.NamedViewAssertion.Companion.matches
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.robot.performOnMosRuBindingScreen
import com.yandex.mobile.realty.core.robot.performOnMosRuStatusScreen
import com.yandex.mobile.realty.core.robot.performOnPublicationFormScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.viewMatchers.NamedViewMatcher.Companion.isCompletelyDisplayed
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.domain.model.user.UserOfferTrustedStatus
import com.yandex.mobile.realty.test.mosru.intendedOpenMosRu
import com.yandex.mobile.realty.test.mosru.intendedOpenMosRuWithLogout
import com.yandex.mobile.realty.test.mosru.prepareMosRuBinding
import com.yandex.mobile.realty.test.mosru.prepareMosRuBindingWithLogout
import com.yandex.mobile.realty.test.mosru.registerMosRuBindingUrl
import com.yandex.mobile.realty.test.mosru.registerMosRuSubmitTaskId
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.util.*

/**
 * @author andrey-bgm on 18/09/2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MosRuEditOfferTest : BasePublishFormTest() {

    private val authorizationRule = AuthorizationRule()
    private var activityTestRule =
        PublicationFormEditActivityTestRule(
            offerId = "1234",
            createTime = Date(),
            launchActivity = false
        )

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        authorizationRule,
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun showMosRuThenBindWhenStatusNotLinkedMosRu() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferSell()
            registerTrustedStatus(UserOfferTrustedStatus.NOT_LINKED_MOSRU)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerTrustedStatus(UserOfferTrustedStatus.WAIT_FOR_CHECK_EXIST_FLAT)
        }

        prepareMosRuBinding()
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferNotLinkedSellMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/notLinkedStatusSellBlock")
            tapOn(lookup.matchesMosRuBlockBindButton())
        }

        performOnMosRuBindingScreen {
            waitUntil { isMosRuBindingScreenShown() }
            intendedOpenMosRu()
            waitUntil { isMosRuBindingScreenHidden() }
        }

        performOnPublicationFormScreen {
            waitUntil { containsOfferWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/waitForCheckExistFlatStatusBlock")
        }
    }

    @Test
    fun showMosRuThenBindFromDialogWhenStatusNotLinkedMosRu() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferSell()
            registerTrustedStatus(UserOfferTrustedStatus.NOT_LINKED_MOSRU)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerTrustedStatus(UserOfferTrustedStatus.WAIT_FOR_CHECK_EXIST_FLAT)
        }

        prepareMosRuBinding()
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferNotLinkedSellMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/notLinkedStatusSellBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/edit/notLinkedStatusSellDialog")
            tapOn(lookup.matchesBindMosRuButton())
        }

        performOnMosRuBindingScreen {
            waitUntil { isMosRuBindingScreenShown() }
            intendedOpenMosRu()
            waitUntil { isMosRuBindingScreenHidden() }
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogHidden() }
        }

        performOnPublicationFormScreen {
            waitUntil { containsOfferWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/waitForCheckExistFlatStatusBlock")
        }
    }

    @Test
    fun showMosRuThenFillFlatNumberWhenStatusTrustedMosRuAndNoFlat() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferSell()
            registerTrustedStatus(UserOfferTrustedStatus.TRUSTED_MOSRU_AND_NO_FLAT)
        }

        prepareMosRuBinding()
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferTrustedNoFlatSellMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/trustedMosRuAndNoFlatStatusSellBlock")
            tapOn(lookup.matchesMosRuBlockFillFlatNumberButton())
            onView(lookup.matchesApartmentNumberField()).check(matches(isCompletelyDisplayed()))
        }
    }

    @Test
    fun showMosRuAndDialogWhenStatusTrustedMosRuAndMatchedFlat() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferSell()
            registerTrustedStatus(UserOfferTrustedStatus.TRUSTED_MOSRU_AND_MATCHED_FLAT)
        }

        prepareMosRuBinding()
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferFullTrustedSellMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/trustedMosRuAndMatchedFlatStatusSellBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches(
                "$SCREENSHOT_DIR/edit/trustedMosRuAndMatchedFlatStatusSellDialog"
            )
        }
    }

    @Test
    fun showMosRuAndDialogWhenStatusWaitForCheckExistFlat() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferSell()
            registerTrustedStatus(UserOfferTrustedStatus.WAIT_FOR_CHECK_EXIST_FLAT)
        }

        prepareMosRuBinding()
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/waitForCheckExistFlatStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/edit/waitForCheckExistFlatStatusSellDialog")
        }
    }

    @Test
    fun showMosRuThenFillFlatNumberFromDialogWhenStatusWaitForCheckNoFlat() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferSell()
            registerTrustedStatus(UserOfferTrustedStatus.WAIT_FOR_CHECK_NO_FLAT)
        }

        prepareMosRuBinding()
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/waitForCheckNoFlatStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/edit/waitForCheckNoFlatStatusSellDialog")
            tapOn(lookup.matchesFillFlatNumberButton())
            waitUntil { isMosRuStatusDialogHidden() }
        }

        performOnPublicationFormScreen {
            onView(lookup.matchesApartmentNumberField()).check(matches(isCompletelyDisplayed()))
        }
    }

    @Test
    fun showMosRuThenRepeatBindFromDialogWhenStatusErrorNotTrustedMosRu() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferSell()
            registerTrustedStatus(UserOfferTrustedStatus.ERROR_NOT_TRUSTED_MOSRU)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerTrustedStatus(UserOfferTrustedStatus.WAIT_FOR_CHECK_EXIST_FLAT)
        }

        prepareMosRuBindingWithLogout()
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferNotTrustedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/errorNotTrustedMosRuStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/edit/errorNotTrustedMosRuStatusDialog")
            tapOn(lookup.matchesRepeatBindMosRuButton())
        }

        performOnMosRuBindingScreen {
            waitUntil { isMosRuBindingScreenShown() }
            intendedOpenMosRuWithLogout()
            waitUntil { isMosRuBindingScreenHidden() }
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogHidden() }
        }

        performOnPublicationFormScreen {
            waitUntil { containsOfferWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/waitForCheckExistFlatStatusBlock")
        }
    }

    @Test
    fun showMosRuThenRepeatBindFromDialogWhenStatusErrorInternal() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferSell()
            registerTrustedStatus(UserOfferTrustedStatus.ERROR_INTERNAL)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerTrustedStatus(UserOfferTrustedStatus.WAIT_FOR_CHECK_EXIST_FLAT)
        }

        prepareMosRuBinding()
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferErrorInternalMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/errorInternalStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/edit/errorInternalStatusDialog")
            tapOn(lookup.matchesRepeatBindMosRuButton())
        }

        performOnMosRuBindingScreen {
            waitUntil { isMosRuBindingScreenShown() }
            intendedOpenMosRu()
            waitUntil { isMosRuBindingScreenHidden() }
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogHidden() }
        }

        performOnPublicationFormScreen {
            waitUntil { containsOfferWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/waitForCheckExistFlatStatusBlock")
        }
    }

    @Test
    fun showMosRuThenBindFromDialogWhenStatusErrorNotOwner() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferSell()
            registerTrustedStatus(UserOfferTrustedStatus.ERROR_NOT_OWNER)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerTrustedStatus(UserOfferTrustedStatus.WAIT_FOR_CHECK_EXIST_FLAT)
        }

        prepareMosRuBindingWithLogout()
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferErrorNotOwnerMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/errorNotOwnerStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/edit/errorNotOwnerStatusDialog")
            tapOn(lookup.matchesBindMosRuButton())
        }

        performOnMosRuBindingScreen {
            waitUntil { isMosRuBindingScreenShown() }
            intendedOpenMosRuWithLogout()
            waitUntil { isMosRuBindingScreenHidden() }
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogHidden() }
        }

        performOnPublicationFormScreen {
            waitUntil { containsOfferWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/waitForCheckExistFlatStatusBlock")
        }
    }

    @Test
    fun showMosRuAndDialogWhenStatusErrorReportNotReceived() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferSell()
            registerTrustedStatus(UserOfferTrustedStatus.ERROR_REPORT_NOT_RECEIVED)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferErrorReportNotReceivedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/errorReportNotReceivedStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/edit/errorReportNotReceivedStatusDialog")
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusNotLinkedMosRu() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferRentLong()
            registerTrustedStatus(UserOfferTrustedStatus.NOT_LINKED_MOSRU)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferNotLinkedRentMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/notLinkedStatusRentBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/edit/notLinkedStatusRentDialog")
        }
    }

    @Test
    fun showMosRuWhenRentLongAndStatusTrustedMosRuAndNoFlat() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferRentLong()
            registerTrustedStatus(UserOfferTrustedStatus.TRUSTED_MOSRU_AND_NO_FLAT)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferTrustedNoFlatRentMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/trustedMosRuAndNoFlatStatusRentBlock")
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusTrustedMosRuAndMatchedFlat() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferRentLong()
            registerTrustedStatus(UserOfferTrustedStatus.TRUSTED_MOSRU_AND_MATCHED_FLAT)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferFullTrustedRentMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/trustedMosRuAndMatchedFlatStatusRentBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches(
                "$SCREENSHOT_DIR/edit/trustedMosRuAndMatchedFlatStatusRentDialog"
            )
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusWaitForCheckExistFlat() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferRentLong()
            registerTrustedStatus(UserOfferTrustedStatus.WAIT_FOR_CHECK_EXIST_FLAT)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/waitForCheckExistFlatStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/edit/waitForCheckExistFlatStatusRentDialog")
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusWaitForCheckNoFlat() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferRentLong()
            registerTrustedStatus(UserOfferTrustedStatus.WAIT_FOR_CHECK_NO_FLAT)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/waitForCheckNoFlatStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/edit/waitForCheckNoFlatStatusRentDialog")
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusErrorNotTrustedMosRu() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferRentLong()
            registerTrustedStatus(UserOfferTrustedStatus.ERROR_NOT_TRUSTED_MOSRU)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferNotTrustedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/errorNotTrustedMosRuStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/edit/errorNotTrustedMosRuStatusDialog")
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusErrorInternal() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferRentLong()
            registerTrustedStatus(UserOfferTrustedStatus.ERROR_INTERNAL)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferErrorInternalMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/errorInternalStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/edit/errorInternalStatusDialog")
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusErrorNotOwner() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferRentLong()
            registerTrustedStatus(UserOfferTrustedStatus.ERROR_NOT_OWNER)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferErrorNotOwnerMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/errorNotOwnerStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/edit/errorNotOwnerStatusDialog")
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusErrorReportNotReceived() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferRentLong()
            registerTrustedStatus(UserOfferTrustedStatus.ERROR_REPORT_NOT_RECEIVED)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferErrorReportNotReceivedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/errorReportNotReceivedStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/edit/errorReportNotReceivedStatusDialog")
        }
    }

    @Test
    fun shouldNotShowMosRuWhenRentShort() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferRentShort()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentShortApartmentCollapsedToolbarTitle() }

            waitUntil { doesNotContainMosRuBlock() }
        }
    }

    @Test
    fun retryLoadStatusWhenErrors() {
        configureWebServer {
            registerUserProfileWithMosRu()
            registerUserProfileWithMosRu()
            registerEditOfferSell()
            registerTrustedStatusError()
            registerTrustedStatus(UserOfferTrustedStatus.NOT_LINKED_MOSRU)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerTrustedStatusError()
            registerTrustedStatus(UserOfferTrustedStatus.WAIT_FOR_CHECK_EXIST_FLAT)
        }

        prepareMosRuBinding()
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }

            waitUntil { containsLoadErrorMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/errorBlock")
            tapOn(lookup.matchesMosRuBlockRetryButton())
            waitUntil { containsOfferNotLinkedSellMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/notLinkedStatusSellBlock")

            tapOn(lookup.matchesMosRuBlockBindButton())
        }

        performOnMosRuBindingScreen {
            waitUntil { isMosRuBindingScreenShown() }
            intendedOpenMosRu()
            waitUntil { isMosRuBindingScreenHidden() }
        }

        performOnPublicationFormScreen {
            waitUntil { containsLoadErrorMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/errorBlock")
            tapOn(lookup.matchesMosRuBlockRetryButton())
            waitUntil { containsOfferWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/edit/waitForCheckExistFlatStatusBlock")
        }
    }

    private fun DispatcherRegistry.registerUserProfileWithMosRu() {
        register(
            request {
                method("GET")
                path("1.0/user")
            },
            response {
                assetBody("publishForm/userOwnerWithMosRu.json")
            }
        )
    }

    private fun DispatcherRegistry.registerEditOfferSell() {
        register(
            request {
                method("GET")
                path("1.0/user/offers/1234/edit")
            },
            response {
                assetBody("publishForm/editOfferSellApartment.json")
            }
        )
    }

    private fun DispatcherRegistry.registerEditOfferRentLong() {
        register(
            request {
                method("GET")
                path("1.0/user/offers/1234/edit")
            },
            response {
                assetBody("publishForm/editOfferRentLongApartment.json")
            }
        )
    }

    private fun DispatcherRegistry.registerEditOfferRentShort() {
        register(
            request {
                method("GET")
                path("1.0/user/offers/1234/edit")
            },
            response {
                assetBody("publishForm/editOfferRentShortApartment.json")
            }
        )
    }

    private fun DispatcherRegistry.registerTrustedStatus(status: UserOfferTrustedStatus) {
        val body = """
            {
              "response": {
                "content": {
                  "trustedOfferInfo": {
                    "ownerTrustedStatus": "$status"
                  }
                }
              }
            }
        """.trimIndent()

        register(
            request {
                method("GET")
                path("2.0/user/me/offers/1234/card")
            },
            response {
                setBody(body)
            }
        )
    }

    private fun DispatcherRegistry.registerTrustedStatusError() {
        register(
            request {
                method("GET")
                path("2.0/user/me/offers/1234/card")
            },
            response {
                setResponseCode(500)
            }
        )
    }

    private companion object {

        private const val SCREENSHOT_DIR = "publicationForm/mosru"
    }
}
