package com.yandex.mobile.realty.test.userOffers

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.UserOfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnMosRuBindingScreen
import com.yandex.mobile.realty.core.robot.performOnMosRuStatusScreen
import com.yandex.mobile.realty.core.robot.performOnPublicationFormScreen
import com.yandex.mobile.realty.core.robot.performOnUserOfferCardScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.data.model.proto.ShowStatus
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

/**
 * @author andrey-bgm on 29/09/2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MosRuUserOfferCardTest {

    private val activityTestRule = UserOfferCardActivityTestRule(
        offerId = OFFER_ID,
        launchActivity = false
    )

    private val authorizationRule = AuthorizationRule()

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
            registerUserProfile()
            registerUserProfile()
            registerUserOfferSell(UserOfferTrustedStatus.NOT_LINKED_MOSRU)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerUserOfferSell(UserOfferTrustedStatus.WAIT_FOR_CHECK_EXIST_FLAT)
        }

        prepareMosRuBinding()
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsNotLinkedSellMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/notLinkedStatusSellBlock")
            tapOn(lookup.matchesMosRuBlockBindButton())
        }

        performOnMosRuBindingScreen {
            waitUntil { isMosRuBindingScreenShown() }
            intendedOpenMosRu()
            waitUntil { isMosRuBindingScreenHidden() }
        }

        performOnUserOfferCardScreen {
            waitUntil { containsWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/waitForCheckExistFlatStatusBlock")
        }
    }

    @Test
    fun showMosRuThenBindFromDialogWhenStatusNotLinkedMosRu() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferSell(UserOfferTrustedStatus.NOT_LINKED_MOSRU)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerUserOfferSell(UserOfferTrustedStatus.WAIT_FOR_CHECK_EXIST_FLAT)
        }

        prepareMosRuBinding()
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsNotLinkedSellMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/notLinkedStatusSellBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/notLinkedStatusSellDialog")
            tapOn(lookup.matchesBindMosRuButton())
        }

        performOnMosRuBindingScreen {
            waitUntil { isMosRuBindingScreenShown() }
            intendedOpenMosRu()
            waitUntil { isMosRuBindingScreenHidden() }
        }

        performOnUserOfferCardScreen {
            waitUntil { containsWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/waitForCheckExistFlatStatusBlock")
        }
    }

    @Test
    fun showMosRuThenFillFlatNumberWhenStatusTrustedMosRuAndNoFlat() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferSell(UserOfferTrustedStatus.TRUSTED_MOSRU_AND_NO_FLAT)
            registerUserProfile()
            registerEditOfferSell()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsTrustedNoFlatSellMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/trustedMosRuAndNoFlatStatusSellBlock")
            tapOn(lookup.matchesMosRuBlockFillFlatNumberButton())
        }

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
        }
    }

    @Test
    fun showMosRuAndDialogWhenStatusTrustedMosRuAndMatchedFlat() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferSell(UserOfferTrustedStatus.TRUSTED_MOSRU_AND_MATCHED_FLAT)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsFullTrustedSellMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/trustedMosRuAndMatchedFlatStatusSellBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches(
                "$SCREENSHOT_DIR/trustedMosRuAndMatchedFlatStatusSellDialog"
            )
        }
    }

    @Test
    fun showMosRuAndDialogWhenStatusWaitForCheckExistFlat() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferSell(UserOfferTrustedStatus.WAIT_FOR_CHECK_EXIST_FLAT)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/waitForCheckExistFlatStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches(
                "$SCREENSHOT_DIR/waitForCheckExistFlatStatusSellDialog"
            )
        }
    }

    @Test
    fun showMosRuThenFillFlatNumberFromDialogWhenStatusWaitForCheckNoFlat() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferSell(UserOfferTrustedStatus.WAIT_FOR_CHECK_NO_FLAT)
            registerUserProfile()
            registerEditOfferSell()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/waitForCheckNoFlatStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches(
                "$SCREENSHOT_DIR/waitForCheckNoFlatStatusSellDialog"
            )
            tapOn(lookup.matchesFillFlatNumberButton())
            waitUntil { isMosRuStatusDialogHidden() }
        }

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
        }
    }

    @Test
    fun showMosRuThenRepeatBindFromDialogWhenStatusErrorNotTrustedMosRu() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferSell(UserOfferTrustedStatus.ERROR_NOT_TRUSTED_MOSRU)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerUserOfferSell(UserOfferTrustedStatus.WAIT_FOR_CHECK_EXIST_FLAT)
        }

        prepareMosRuBindingWithLogout()
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsNotTrustedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/errorNotTrustedMosRuStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/errorNotTrustedMosRuStatusDialog")
            tapOn(lookup.matchesRepeatBindMosRuButton())
        }

        performOnMosRuBindingScreen {
            waitUntil { isMosRuBindingScreenShown() }
            intendedOpenMosRuWithLogout()
            waitUntil { isMosRuBindingScreenHidden() }
        }

        performOnUserOfferCardScreen {
            waitUntil { containsWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/waitForCheckExistFlatStatusBlock")
        }
    }

    @Test
    fun showMosRuThenRepeatBindFromDialogWhenStatusErrorInternal() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferSell(UserOfferTrustedStatus.ERROR_INTERNAL)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerUserOfferSell(UserOfferTrustedStatus.WAIT_FOR_CHECK_EXIST_FLAT)
        }

        prepareMosRuBinding()
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsErrorInternalMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/errorInternalStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/errorInternalStatusDialog")
            tapOn(lookup.matchesRepeatBindMosRuButton())
        }

        performOnMosRuBindingScreen {
            waitUntil { isMosRuBindingScreenShown() }
            intendedOpenMosRu()
            waitUntil { isMosRuBindingScreenHidden() }
        }

        performOnUserOfferCardScreen {
            waitUntil { containsWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/waitForCheckExistFlatStatusBlock")
        }
    }

    @Test
    fun showMosRuThenBindFromDialogWhenStatusErrorNotOwner() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferSell(UserOfferTrustedStatus.ERROR_NOT_OWNER)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerUserOfferSell(UserOfferTrustedStatus.WAIT_FOR_CHECK_EXIST_FLAT)
        }

        prepareMosRuBindingWithLogout()
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsErrorNotOwnerMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/errorNotOwnerStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/errorNotOwnerStatusDialog")
            tapOn(lookup.matchesBindMosRuButton())
        }

        performOnMosRuBindingScreen {
            waitUntil { isMosRuBindingScreenShown() }
            intendedOpenMosRuWithLogout()
            waitUntil { isMosRuBindingScreenHidden() }
        }

        performOnUserOfferCardScreen {
            waitUntil { containsWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/waitForCheckExistFlatStatusBlock")
        }
    }

    @Test
    fun showMosRuAndDialogWhenStatusErrorReportNotReceived() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferSell(UserOfferTrustedStatus.ERROR_REPORT_NOT_RECEIVED)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsErrorReportNotReceivedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/errorReportNotReceivedStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/errorReportNotReceivedStatusDialog")
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusNotLinkedMosRu() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferRentLong(UserOfferTrustedStatus.NOT_LINKED_MOSRU)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsNotLinkedRentMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/notLinkedStatusRentBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/notLinkedStatusRentDialog")
        }
    }

    @Test
    fun showMosRuWhenRentLongAndStatusTrustedMosRuAndNoFlat() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferRentLong(UserOfferTrustedStatus.TRUSTED_MOSRU_AND_NO_FLAT)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsTrustedNoFlatRentMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/trustedMosRuAndNoFlatStatusRentBlock")
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusTrustedMosRuAndMatchedFlat() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferRentLong(UserOfferTrustedStatus.TRUSTED_MOSRU_AND_MATCHED_FLAT)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsFullTrustedRentMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/trustedMosRuAndMatchedFlatStatusRentBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches(
                "$SCREENSHOT_DIR/trustedMosRuAndMatchedFlatStatusRentDialog"
            )
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusWaitForCheckExistFlat() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferRentLong(UserOfferTrustedStatus.WAIT_FOR_CHECK_EXIST_FLAT)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/waitForCheckExistFlatStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/waitForCheckExistFlatStatusRentDialog")
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusWaitForCheckNoFlat() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferRentLong(UserOfferTrustedStatus.WAIT_FOR_CHECK_NO_FLAT)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/waitForCheckNoFlatStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/waitForCheckNoFlatStatusRentDialog")
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusErrorNotTrustedMosRu() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferRentLong(UserOfferTrustedStatus.ERROR_NOT_TRUSTED_MOSRU)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsNotTrustedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/errorNotTrustedMosRuStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/errorNotTrustedMosRuStatusDialog")
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusErrorInternal() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferRentLong(UserOfferTrustedStatus.ERROR_INTERNAL)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsErrorInternalMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/errorInternalStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/errorInternalStatusDialog")
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusErrorNotOwner() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferRentLong(UserOfferTrustedStatus.ERROR_NOT_OWNER)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsErrorNotOwnerMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/errorNotOwnerStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/errorNotOwnerStatusDialog")
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusErrorReportNotReceived() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferRentLong(UserOfferTrustedStatus.ERROR_REPORT_NOT_RECEIVED)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsErrorReportNotReceivedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/errorReportNotReceivedStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/errorReportNotReceivedStatusDialog")
        }
    }

    @Test
    fun shouldNotShowMosRuWhenRentShort() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferRentShort(UserOfferTrustedStatus.NOT_LINKED_MOSRU)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { isPriceViewShown() }
            doesNotContainMosRuBlock()
        }
    }

    @Test
    fun shouldUpdateStatusAfterBindingOnPublicationForm() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferSell(UserOfferTrustedStatus.NOT_LINKED_MOSRU)
            registerUserProfile()
            registerEditOfferSell()
            registerUserOfferSell(UserOfferTrustedStatus.NOT_LINKED_MOSRU)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerUserOfferSell(UserOfferTrustedStatus.WAIT_FOR_CHECK_EXIST_FLAT)
            registerUserOfferSell(UserOfferTrustedStatus.WAIT_FOR_CHECK_EXIST_FLAT)
        }

        prepareMosRuBinding()
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsNotLinkedSellMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/notLinkedStatusSellBlock")
            tapOn(lookup.matchesEditButton())
        }

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferNotLinkedSellMosRuBlock() }
            tapOn(lookup.matchesMosRuBlockBindButton())
        }

        performOnMosRuBindingScreen {
            waitUntil { isMosRuBindingScreenShown() }
            intendedOpenMosRu()
            waitUntil { isMosRuBindingScreenHidden() }
        }

        performOnPublicationFormScreen {
            pressBack()
        }

        performOnUserOfferCardScreen {
            waitUntil { containsWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/waitForCheckExistFlatStatusBlock")
        }
    }

    @Test
    fun shouldUpdateStatusAfterOpenPublicationForm() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferSell(UserOfferTrustedStatus.WAIT_FOR_CHECK_EXIST_FLAT)
            registerUserProfile()
            registerEditOfferSell()
            registerUserOfferSell(UserOfferTrustedStatus.TRUSTED_MOSRU_AND_MATCHED_FLAT)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsWaitForCheckMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/waitForCheckExistFlatStatusBlock")
            tapOn(lookup.matchesEditButton())
        }

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }

            waitUntil { containsOfferFullTrustedSellMosRuBlock() }
            pressBack()
        }

        performOnUserOfferCardScreen {
            waitUntil { containsFullTrustedSellMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/trustedMosRuAndMatchedFlatStatusSellBlock")
        }
    }

    @Test
    fun showMosRuWhenUnpublished() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferSell(UserOfferTrustedStatus.NOT_LINKED_MOSRU, ShowStatus.UNPUBLISHED)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsNotLinkedSellMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/notLinkedStatusSellBlock")
        }
    }

    @Test
    fun shouldNotShowMosRuWhenOnModeration() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferSell(UserOfferTrustedStatus.NOT_LINKED_MOSRU, ShowStatus.MODERATION)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { isPriceViewShown() }
            doesNotContainMosRuBlock()
        }
    }

    @Test
    fun shouldNotShowMosRuWhenBannedAndEditable() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerBannedUserOfferSell(UserOfferTrustedStatus.TRUSTED_MOSRU_AND_NO_FLAT, true)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { isPriceViewShown() }
            doesNotContainMosRuBlock()
        }
    }

    @Test
    fun shouldNotShowMosRuWhenBannedAndNotEditable() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerBannedUserOfferSell(UserOfferTrustedStatus.NOT_LINKED_MOSRU, false)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { isPriceViewShown() }
            doesNotContainMosRuBlock()
        }
    }

    @Test
    fun retryLoadStatusWhenErrors() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOfferSell(UserOfferTrustedStatus.NOT_LINKED_MOSRU)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerUserOfferError()
            registerUserOfferSell(UserOfferTrustedStatus.WAIT_FOR_CHECK_EXIST_FLAT)
        }

        prepareMosRuBinding()
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { containsNotLinkedSellMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/notLinkedStatusSellBlock")
            tapOn(lookup.matchesMosRuBlockBindButton())
        }

        performOnMosRuBindingScreen {
            waitUntil { isMosRuBindingScreenShown() }
            intendedOpenMosRu()
            waitUntil { isMosRuBindingScreenHidden() }
        }

        performOnUserOfferCardScreen {
            waitUntil { containsLoadErrorMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/errorBlock")
            tapOn(lookup.matchesMosRuBlockRetryButton())
            isMosRuBlockMatches("$SCREENSHOT_DIR/waitForCheckExistFlatStatusBlock")
        }
    }

    private fun DispatcherRegistry.registerUserProfile() {
        register(
            request {
                method("GET")
                path("1.0/user")
            },
            response {
                assetBody("user/userOwner.json")
            }
        )
    }

    private fun DispatcherRegistry.registerUserOfferSell(
        trustedStatus: UserOfferTrustedStatus,
        status: ShowStatus = ShowStatus.PUBLISHED
    ) {
        registerUserOffer(
            trustedStatus,
            """
                    "sell": {
                        "price": {
                          "value": 4500000,
                          "currency": "RUB",
                          "priceType": "PER_OFFER",
                          "pricingPeriod": "WHOLE_LIFE"
                        }
                    }
            """.trimIndent(),
            status
        )
    }

    private fun DispatcherRegistry.registerBannedUserOfferSell(
        trustedStatus: UserOfferTrustedStatus,
        editable: Boolean
    ) {
        registerUserOffer(
            trustedStatus,
            """
                    "sell": {
                        "price": {
                          "value": 4500000,
                          "currency": "RUB",
                          "priceType": "PER_OFFER",
                          "pricingPeriod": "WHOLE_LIFE"
                        }
                    }
            """.trimIndent(),
            ShowStatus.BANNED,
            """
                    [{
                        "editable": $editable,
                        "title": "Неверное расположение",
                        "vosMessage": "Указано неверное расположение"
                    }]
            """.trimIndent()
        )
    }

    private fun DispatcherRegistry.registerUserOfferRentLong(
        trustedStatus: UserOfferTrustedStatus
    ) {
        registerUserOffer(
            trustedStatus,
            """
                    "rent": {
                        "price": {
                          "value": 20000,
                          "currency": "RUB",
                          "priceType": "PER_OFFER",
                          "pricingPeriod": "PER_MONTH"
                        }
                    }
            """.trimIndent(),
            ShowStatus.PUBLISHED
        )
    }

    private fun DispatcherRegistry.registerUserOfferRentShort(
        status: UserOfferTrustedStatus
    ) {
        registerUserOffer(
            status,
            """
                    "rent": { 
                        "price": {
                          "value": 5000,
                          "currency": "RUB",
                          "priceType": "PER_OFFER",
                          "pricingPeriod": "PER_DAY"
                        }
                    }
            """.trimIndent(),
            ShowStatus.PUBLISHED
        )
    }

    private fun DispatcherRegistry.registerUserOffer(
        trustedStatus: UserOfferTrustedStatus,
        deal: String,
        status: ShowStatus,
        banReasons: String? = null
    ) {
        val body = """
            {
              "response": {
                "content": {
                  "id": "$OFFER_ID",
                  "uid": "1",
                  "publishingInfo": {
                    "creationDate": "2020-02-12T12:10:00.000Z",
                    "updateDate": "2020-02-17T10:35:00.000Z",
                    "status": "$status",
                    "banReasons": $banReasons
                  },
                  "placement": {
                    "free": {}
                  },
                  "vosLocation": {
                    "rgid": "417899",
                    "point": {
                      "latitude": 59.96423,
                      "longitude": 30.407164,
                      "defined": true
                    },
                    "address": {
                      "unifiedOneline": "Россия, Санкт-Петербург, Полюстровский проспект, 7"
                    }
                  },
                  $deal,
                  "apartment": {
                    "buildingInfo": {
                      "characteristics": {
                        "floorsTotal": 9
                      }
                    },
                    "livingInfo": {
                      "rooms": 1,
                      "floor": [
                        2
                      ]
                    },
                    "area": {
                      "area": {
                        "value": 50.0,
                        "unit": "SQ_M"
                      }
                    }
                  },
                  "isFromFeed": false,
                  "trustedOfferInfo": {
                    "ownerTrustedStatus": "$trustedStatus"
                  }
                }
              }
            }
        """.trimIndent()

        register(
            request {
                path("2.0/user/me/offers/$OFFER_ID/card")
            },
            response {
                setBody(body)
            }
        )
    }

    private fun DispatcherRegistry.registerUserOfferError() {
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

    private companion object {

        private const val OFFER_ID = "1234"
        private const val SCREENSHOT_DIR = "userOfferCard/mosru"
    }
}
