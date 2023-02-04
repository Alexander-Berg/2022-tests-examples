package com.yandex.mobile.realty.test.publicationForm

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.PublicationFormActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.assertion.NamedViewAssertion.Companion.matches
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.robot.performOnMosRuBindingScreen
import com.yandex.mobile.realty.core.robot.performOnMosRuStatusScreen
import com.yandex.mobile.realty.core.robot.performOnPublicationFormScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.UserOfferDraftRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.viewMatchers.NamedViewMatcher.Companion.isCompletelyDisplayed
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.domain.model.user.MosRuStatus
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
 * @author andrey-bgm on 18/09/2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MosRuDraftTest : BasePublishFormTest() {

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

    @Test
    fun showMosRuThenBindWhenStatusNotProcessed() {
        showMosRuThenBindWhenStatusInitial(MosRuStatus.NOT_PROCESSED)
    }

    @Test
    fun showMosRuThenBindWhenStatusUnlinked() {
        showMosRuThenBindWhenStatusInitial(MosRuStatus.UNLINKED)
    }

    private fun showMosRuThenBindWhenStatusInitial(status: MosRuStatus) {
        configureWebServer {
            registerUserProfile(status)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerUserProfile(MosRuStatus.REQUESTED)
        }

        prepareMosRuBinding()
        draftRule.prepareSellApartment()
        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil {
                hasSellApartmentCollapsedToolbarTitle()
                containsDraftNotLinkedSellMosRuBlock()
            }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/notProcessedStatusSellBlock")
            tapOn(lookup.matchesMosRuBlockBindButton())
        }

        performOnMosRuBindingScreen {
            waitUntil { isMosRuBindingScreenShown() }
            intendedOpenMosRu()
            waitUntil { isMosRuBindingScreenHidden() }
        }

        performOnPublicationFormScreen {
            waitUntil { containsDraftRequestedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/requestedStatusBlock")
        }
    }

    @Test
    fun showMosRuThenBindFromDialogWhenStatusNotProcessed() {
        showMosRuThenBindFromDialogWhenStatusInitial(MosRuStatus.NOT_PROCESSED)
    }

    @Test
    fun showMosRuThenBindFromDialogWhenStatusUnlinked() {
        showMosRuThenBindFromDialogWhenStatusInitial(MosRuStatus.UNLINKED)
    }

    private fun showMosRuThenBindFromDialogWhenStatusInitial(status: MosRuStatus) {
        configureWebServer {
            registerUserProfile(status)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerUserProfile(MosRuStatus.REQUESTED)
        }

        prepareMosRuBinding()
        draftRule.prepareSellApartment()
        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil {
                hasSellApartmentCollapsedToolbarTitle()
                containsDraftNotLinkedSellMosRuBlock()
            }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/notProcessedStatusSellBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/draft/notProcessedStatusSellDialog")
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
            waitUntil { containsDraftRequestedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/requestedStatusBlock")
        }
    }

    @Test
    fun showMosRuThenFillFlatNumberFromDialogWhenStatusRequested() {
        configureWebServer {
            registerUserProfile(MosRuStatus.REQUESTED)
        }

        draftRule.prepareSellApartment()
        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil {
                hasSellApartmentCollapsedToolbarTitle()
                containsDraftRequestedMosRuBlock()
            }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/requestedStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/draft/requestedStatusSellDialog")
            tapOn(lookup.matchesFillFlatNumberButton())
            waitUntil { isMosRuStatusDialogHidden() }
        }

        performOnPublicationFormScreen {
            onView(lookup.matchesApartmentNumberField()).check(matches(isCompletelyDisplayed()))
        }
    }

    @Test
    fun showMosRuThenFillFlatNumberWhenTrustedStatus() {
        configureWebServer {
            registerUserProfile(MosRuStatus.TRUSTED)
        }

        draftRule.prepareSellApartment()
        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil {
                hasSellApartmentCollapsedToolbarTitle()
                containsDraftTrustedSellMosRuBlock()
            }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/trustedStatusSellBlock")
            tapOn(lookup.matchesMosRuBlockFillFlatNumberButton())
            onView(lookup.matchesApartmentNumberField()).check(matches(isCompletelyDisplayed()))
        }
    }

    @Test
    fun showMosRuThenRepeatBindFromDialogWhenStatusNotTrusted() {
        configureWebServer {
            registerUserProfile(MosRuStatus.NOT_TRUSTED)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerUserProfile(MosRuStatus.REQUESTED)
        }

        prepareMosRuBindingWithLogout()
        draftRule.prepareSellApartment()
        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil {
                hasSellApartmentCollapsedToolbarTitle()
                containsDraftNotTrustedMosRuBlock()
            }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/notTrustedStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/draft/notTrustedStatusDialog")
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
            waitUntil { containsDraftRequestedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/requestedStatusBlock")
        }
    }

    @Test
    fun showMosRuThenRepeatBindFromDialogWhenStatusRequestingError() {
        configureWebServer {
            registerUserProfile(MosRuStatus.REQUESTING_ERROR)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerUserProfile(MosRuStatus.REQUESTED)
        }

        prepareMosRuBinding()
        draftRule.prepareSellApartment()
        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil {
                hasSellApartmentCollapsedToolbarTitle()
                containsDraftRequestingErrorMosRuBlock()
            }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/requestingErrorStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/draft/requestingErrorStatusDialog")
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
            waitUntil { containsDraftRequestedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/requestedStatusBlock")
        }
    }

    @Test
    fun retryLoadStatusAfterBindingWhenLoadUserFailed() {
        configureWebServer {
            registerUserProfile(MosRuStatus.NOT_PROCESSED)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerUserProfileError()
            registerUserProfile(MosRuStatus.REQUESTED)
        }

        prepareMosRuBinding()
        draftRule.prepareSellApartment()
        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil {
                hasSellApartmentCollapsedToolbarTitle()
                containsDraftNotLinkedSellMosRuBlock()
            }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/notProcessedStatusSellBlock")
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
            waitUntil { containsDraftRequestedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/requestedStatusBlock")
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusNotProcessed() {
        configureWebServer {
            registerUserProfile(MosRuStatus.NOT_PROCESSED)
        }

        draftRule.prepareRentLongApartment()
        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil {
                hasRentLongApartmentCollapsedToolbarTitle()
                containsDraftNotLinkedRentMosRuBlock()
            }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/notProcessedStatusRentBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/draft/notProcessedStatusRentDialog")
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusUnlinked() {
        configureWebServer {
            registerUserProfile(MosRuStatus.UNLINKED)
        }

        draftRule.prepareRentLongApartment()
        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil {
                hasRentLongApartmentCollapsedToolbarTitle()
                containsDraftNotLinkedRentMosRuBlock()
            }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/notProcessedStatusRentBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/draft/notProcessedStatusRentDialog")
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusRequested() {
        configureWebServer {
            registerUserProfile(MosRuStatus.REQUESTED)
        }

        draftRule.prepareRentLongApartment()
        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil {
                hasRentLongApartmentCollapsedToolbarTitle()
                containsDraftRequestedMosRuBlock()
            }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/requestedStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/draft/requestedStatusRentDialog")
        }
    }

    @Test
    fun showMosRuWhenRentLongAndStatusTrusted() {
        configureWebServer {
            registerUserProfile(MosRuStatus.TRUSTED)
        }

        draftRule.prepareRentLongApartment()
        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil {
                hasRentLongApartmentCollapsedToolbarTitle()
                containsDraftTrustedRentMosRuBlock()
            }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/trustedStatusRentBlock")
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusNotTrusted() {
        configureWebServer {
            registerUserProfile(MosRuStatus.NOT_TRUSTED)
        }

        draftRule.prepareRentLongApartment()
        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil {
                hasRentLongApartmentCollapsedToolbarTitle()
                containsDraftNotTrustedMosRuBlock()
            }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/notTrustedStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/draft/notTrustedStatusDialog")
        }
    }

    @Test
    fun showMosRuAndDialogWhenRentLongAndStatusRequestingError() {
        configureWebServer {
            registerUserProfile(MosRuStatus.REQUESTING_ERROR)
        }

        draftRule.prepareRentLongApartment()
        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil {
                hasRentLongApartmentCollapsedToolbarTitle()
                containsDraftRequestingErrorMosRuBlock()
            }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/requestingErrorStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/draft/requestingErrorStatusDialog")
        }
    }

    @Test
    fun shouldNotShowMosRuWhenRentShort() {
        configureWebServer {
            registerUserProfile(MosRuStatus.NOT_PROCESSED)
        }

        draftRule.prepareRentShortApartment()
        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentShortApartmentCollapsedToolbarTitle() }
            doesNotContainMosRuBlock()
        }
    }

    @Test
    fun bindByNewUserWhenProfileNotFilled() {
        configureWebServer {
            registerNewUserProfile()
            registerProfileValidationError()
            registerProfilePatchSuccess()
            registerUserProfile(MosRuStatus.NOT_PROCESSED)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerUserProfile(MosRuStatus.REQUESTED)
        }

        prepareMosRuBinding()
        draftRule.prepareSellApartment()
        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil {
                hasSellApartmentCollapsedToolbarTitle()
                containsDraftNotLinkedSellMosRuBlock()
            }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/notProcessedStatusSellBlock")
            tapOn(lookup.matchesMosRuBlockBindButton())

            waitUntil {
                onView(lookup.matchesContactEmailField()).check(matches(isCompletelyDisplayed()))
            }
            containsValidationError("Заполните email")
            containsValidationError("Выберите телефон")

            scrollToPosition(lookup.matchesContactEmailField())
            typeText(lookup.matchesContactEmailFieldValue(), "john@gmail.com")
            scrollToPosition(lookup.matchesContactPhonesField())
            tapOn(lookup.matchesContactPhoneFieldSelector("+7111*****44"))

            scrollToPosition(lookup.matchesMosRuBlock())
            tapOn(lookup.matchesMosRuBlockBindButton())
        }

        performOnMosRuBindingScreen {
            waitUntil { isMosRuBindingScreenShown() }
            intendedOpenMosRu()
            waitUntil { isMosRuBindingScreenHidden() }
        }

        performOnPublicationFormScreen {
            waitUntil { containsDraftRequestedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/requestedStatusBlock")
        }
    }

    @Test
    fun bindByNewUserFromDialogWhenProfileNotFilled() {
        configureWebServer {
            registerNewUserProfile()
            registerProfileValidationError()
            registerProfilePatchSuccess()
            registerUserProfile(MosRuStatus.NOT_PROCESSED)
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerUserProfile(MosRuStatus.REQUESTED)
        }

        prepareMosRuBinding()
        draftRule.prepareSellApartment()
        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil {
                hasSellApartmentCollapsedToolbarTitle()
                containsDraftNotLinkedSellMosRuBlock()
            }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/notProcessedStatusSellBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/draft/notProcessedStatusSellDialog")
            tapOn(lookup.matchesBindMosRuButton())
            waitUntil { isMosRuStatusDialogHidden() }
        }

        performOnPublicationFormScreen {
            waitUntil {
                onView(lookup.matchesContactEmailField()).check(matches(isCompletelyDisplayed()))
            }
            containsValidationError("Заполните email")
            containsValidationError("Выберите телефон")

            scrollToPosition(lookup.matchesContactEmailField())
            typeText(lookup.matchesContactEmailFieldValue(), "john@gmail.com")
            scrollToPosition(lookup.matchesContactPhonesField())
            tapOn(lookup.matchesContactPhoneFieldSelector("+7111*****44"))

            scrollToPosition(lookup.matchesMosRuBlock())
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/draft/notProcessedStatusSellDialog")
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
            waitUntil { containsDraftRequestedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/requestedStatusBlock")
        }
    }

    @Test
    fun bindByNewUserWhenSomePatchError() {
        configureWebServer {
            registerNewUserProfile()
            registerProfilePatchError()
        }

        draftRule.prepareSellApartment()
        authorizationRule.setUserAuthorized()

        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil {
                hasSellApartmentCollapsedToolbarTitle()
                containsDraftNotLinkedSellMosRuBlock()
            }
            isMosRuBlockMatches("$SCREENSHOT_DIR/draft/notProcessedStatusSellBlock")
            tapOn(lookup.matchesMosRuBlockBindButton())

            waitUntil { isToastShown(getResourceString(R.string.error_bind_mosru)) }
        }
    }

    private fun DispatcherRegistry.registerUserProfile(status: MosRuStatus) {
        val body = """
            {
                "response": {
                    "valid": true,
                    "user": {
                        "name": "John",
                        "status": "active",
                        "phones": [
                            {
                                "id": "1",
                                "phone": "+7111*****44",
                                "select": true,
                                "fullPhone": "+71112223344"
                            }
                        ],
                        "email" : "john@gmail.com",
                        "type": "OWNER",
                        "redirectPhones": true,
                        "paymentType": "NATURAL_PERSON",
                        "capaUser": false,
                        "mosRuAvailable": true,
                        "mosRuStatus": "$status"
                    }
                }
            }
        """.trimIndent()

        register(
            request {
                method("GET")
                path("1.0/user")
            },
            response {
                setBody(body)
            }
        )
    }

    private fun DispatcherRegistry.registerNewUserProfile() {
        val body = """
            {
                "response": {
                    "valid": false,
                    "user": {
                        "name": "John",
                        "status": "unknown",
                        "phones": [
                            {
                                "id": "1",
                                "phone": "+7111*****44",
                                "select": false,
                                "fullPhone": "+71112223344"
                            }
                        ],
                        "type": "UNKNOWN"
                    }
                }
            }
        """.trimIndent()

        register(
            request {
                path("1.0/user")
                method("GET")
            },
            response {
                setBody(body)
            }
        )
    }

    private fun DispatcherRegistry.registerUserProfileError() {
        register(
            request {
                method("GET")
                path("1.0/user")
            },
            response {
                setResponseCode(500)
            }
        )
    }

    private fun DispatcherRegistry.registerProfileValidationError() {
        val requestBody = """
            {
                "user": {
                    "name": "John",
                    "email": "",
                    "phones": [
                        {
                            "id": "1",
                            "select": false
                        }
                    ],
                    "redirectPhones":true,
                    "allowedCommunicationChannels":["COM_CALLS","COM_CHATS"]
                }
            }
        """.trimIndent()

        val responseBody = """
            {
                "error": {
                    "codename": "JSON_VALIDATION_ERROR",
                    "data": {
                        "validationErrors": [
                            {
                                "parameter": "/phones",
                                "code": "custom_select_phones",
                                "localizedDescription": "Выберите телефон"
                            },
                            {
                                "parameter": "/email",
                                "code": "custom_email",
                                "localizedDescription": "Заполните email"
                            }
                        ],
                        "valid": false
                    }
                }
            }
        """.trimIndent()

        register(
            request {
                method("PATCH")
                path("1.0/user")
                body(requestBody)
            },
            response {
                setResponseCode(400)
                setBody(responseBody)
            }
        )
    }

    private fun DispatcherRegistry.registerProfilePatchSuccess() {
        val requestBody = """
            {
                "user": {
                    "name": "John",
                    "email": "john@gmail.com",
                    "phones": [
                        {
                            "id": "1",
                            "select": true
                        }
                    ],
                    "redirectPhones":true,
                     "allowedCommunicationChannels":["COM_CALLS","COM_CHATS"]
                }
            }
        """.trimIndent()

        register(
            request {
                method("PATCH")
                path("1.0/user")
                body(requestBody)
            },
            response {
                setResponseCode(200)
            }
        )
    }

    private fun DispatcherRegistry.registerProfilePatchError() {
        register(
            request {
                method("PATCH")
                path("1.0/user")
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
