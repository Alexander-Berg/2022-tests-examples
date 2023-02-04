package com.yandex.mobile.realty.test.extra

import android.app.Activity
import android.app.Instrumentation
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ExtraActivityTestRule
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.robot.performOnExtraScreen
import com.yandex.mobile.realty.core.robot.performOnMosRuBindingScreen
import com.yandex.mobile.realty.core.robot.performOnMosRuStatusScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.domain.model.user.MosRuStatus
import com.yandex.mobile.realty.test.mosru.intendedOpenMosRu
import com.yandex.mobile.realty.test.mosru.intendedOpenMosRuWithLogout
import com.yandex.mobile.realty.test.mosru.matchesOpenInBrowserMosRuHelpIntent
import com.yandex.mobile.realty.test.mosru.matchesOpenInBrowserYandexHelpIntent
import com.yandex.mobile.realty.test.mosru.prepareMosRuBinding
import com.yandex.mobile.realty.test.mosru.prepareMosRuBindingWithLogout
import com.yandex.mobile.realty.test.mosru.registerMosRuBindingUrl
import com.yandex.mobile.realty.test.mosru.registerMosRuBindingUrlError
import com.yandex.mobile.realty.test.mosru.registerMosRuSubmitTaskId
import com.yandex.mobile.realty.test.mosru.registerMosRuSubmitTaskIdError
import com.yandex.mobile.realty.test.mosru.registerMosRuUnbind
import com.yandex.mobile.realty.test.services.registerNaturalPersonServicesInfo
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author andrey-bgm on 17/09/2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MosRuExtraTest {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = ExtraActivityTestRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun showMosRuBlockThenBindWhenStatusNotProcessed() {
        showMosRuBlockThenBindWhenStatusInitial(MosRuStatus.NOT_PROCESSED)
    }

    @Test
    fun showMosRuThenBindWhenStatusUnlinked() {
        showMosRuBlockThenBindWhenStatusInitial(MosRuStatus.UNLINKED)
    }

    private fun showMosRuBlockThenBindWhenStatusInitial(status: MosRuStatus) {
        configureWebServer {
            registerUserProfile(true, status)
            registerNaturalPersonServicesInfo()
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerUserProfile(true, MosRuStatus.REQUESTED)
            registerNaturalPersonServicesInfo()
        }

        prepareMosRuBinding()
        authorizationRule.setUserAuthorized()

        performOnExtraScreen {
            waitUntil { containsNotLinkedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/notProcessedStatusBlock")
            tapOn(lookup.matchesMosRuBlockBindButton())
        }

        performOnMosRuBindingScreen {
            waitUntil { isMosRuBindingScreenShown() }
            intendedOpenMosRu()
            waitUntil { isMosRuBindingScreenHidden() }
        }

        performOnExtraScreen {
            waitUntil { containsRequestedMosRuItem() }
            isMosRuItemMatches("$SCREENSHOT_DIR/requestedStatusItem")
        }
    }

    @Test
    fun showMosRuBlockThenBindFromDialogWhenStatusNotProcessed() {
        showMosRuBlockThenBindFromDialogWhenStatusInitial(MosRuStatus.NOT_PROCESSED)
    }

    @Test
    fun showMosRuBlockThenBindFromDialogWhenStatusUnlinked() {
        showMosRuBlockThenBindFromDialogWhenStatusInitial(MosRuStatus.UNLINKED)
    }

    private fun showMosRuBlockThenBindFromDialogWhenStatusInitial(status: MosRuStatus) {
        configureWebServer {
            registerUserProfile(true, status)
            registerNaturalPersonServicesInfo()
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerUserProfile(true, MosRuStatus.REQUESTED)
            registerNaturalPersonServicesInfo()
        }

        prepareMosRuBinding()
        authorizationRule.setUserAuthorized()

        performOnExtraScreen {
            waitUntil { containsNotLinkedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/notProcessedStatusBlock")
            tapOn(lookup.matchesMosRuBlockInfoButton())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/notProcessedStatusDialog")
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

        performOnExtraScreen {
            waitUntil { containsRequestedMosRuItem() }
            isMosRuItemMatches("$SCREENSHOT_DIR/requestedStatusItem")
        }
    }

    @Test
    fun showMosRuItemAndDialogWhenStatusRequested() {
        configureWebServer {
            registerUserProfile(true, MosRuStatus.REQUESTED)
            registerNaturalPersonServicesInfo()
        }

        authorizationRule.setUserAuthorized()

        performOnExtraScreen {
            waitUntil { containsRequestedMosRuItem() }
            isMosRuItemMatches("$SCREENSHOT_DIR/requestedStatusItem")
            tapOn(lookup.matchesMosRuItem())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/requestedStatusDialog")
        }
    }

    @Test
    fun showMosRuItemThenUnbindFromDialogWhenStatusTrusted() {
        configureWebServer {
            registerUserProfile(true, MosRuStatus.TRUSTED)
            registerNaturalPersonServicesInfo()
            registerMosRuUnbind()
            registerUserProfile(true, MosRuStatus.UNLINKED)
            registerNaturalPersonServicesInfo()
        }

        authorizationRule.setUserAuthorized()

        performOnExtraScreen {
            waitUntil { containsTrustedMosRuItem() }
            isMosRuItemMatches("$SCREENSHOT_DIR/trustedStatusItem")
            tapOn(lookup.matchesMosRuItem())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/trustedStatusDialog")
            tapOn(lookup.matchesUnbindMosRuButton())
            isMosRuStatusDialogHidden()
        }

        performOnExtraScreen {
            waitUntil { containsNotLinkedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/notProcessedStatusBlock")
        }
    }

    @Test
    fun showMosRuItemThenRepeatBindFromDialogWhenStatusNotTrusted() {
        configureWebServer {
            registerUserProfile(true, MosRuStatus.NOT_TRUSTED)
            registerNaturalPersonServicesInfo()
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerUserProfile(true, MosRuStatus.REQUESTED)
            registerNaturalPersonServicesInfo()
        }

        prepareMosRuBindingWithLogout()
        authorizationRule.setUserAuthorized()

        performOnExtraScreen {
            waitUntil { containsErrorMosRuItem() }
            isMosRuItemMatches("$SCREENSHOT_DIR/notTrustedStatusItem")
            tapOn(lookup.matchesMosRuItem())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/notTrustedStatusDialog")
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

        performOnExtraScreen {
            waitUntil { containsRequestedMosRuItem() }
            isMosRuItemMatches("$SCREENSHOT_DIR/requestedStatusItem")
        }
    }

    @Test
    fun openInBrowserMosRuHelpFromDialogWhenStatusNotTrusted() {
        configureWebServer {
            registerUserProfile(true, MosRuStatus.NOT_TRUSTED)
            registerNaturalPersonServicesInfo()
        }

        Intents.intending(matchesOpenInBrowserMosRuHelpIntent())
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        authorizationRule.setUserAuthorized()

        performOnExtraScreen {
            waitUntil { containsErrorMosRuItem() }
            isMosRuItemMatches("$SCREENSHOT_DIR/notTrustedStatusItem")
            tapOn(lookup.matchesMosRuItem())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/notTrustedStatusDialog")
            tapOnLinkText(lookup.matchesNotTrustedDescription(), "mos.ru")
            intended(matchesOpenInBrowserMosRuHelpIntent())
        }
    }

    @Test
    fun openInBrowserYandexHelpFromDialog() {
        configureWebServer {
            registerUserProfile(true, MosRuStatus.NOT_TRUSTED)
            registerNaturalPersonServicesInfo()
        }

        Intents.intending(matchesOpenInBrowserYandexHelpIntent())
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        authorizationRule.setUserAuthorized()

        performOnExtraScreen {
            waitUntil { containsErrorMosRuItem() }
            isMosRuItemMatches("$SCREENSHOT_DIR/notTrustedStatusItem")
            tapOn(lookup.matchesMosRuItem())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/notTrustedStatusDialog")
            tapOn(lookup.matchesShowHelpButton())
            intended(matchesOpenInBrowserYandexHelpIntent())
        }
    }

    @Test
    fun showMosRuItemThenRepeatBindFromDialogWhenStatusRequestingError() {
        configureWebServer {
            registerUserProfile(true, MosRuStatus.REQUESTING_ERROR)
            registerNaturalPersonServicesInfo()
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerUserProfile(true, MosRuStatus.REQUESTED)
            registerNaturalPersonServicesInfo()
        }

        prepareMosRuBinding()
        authorizationRule.setUserAuthorized()

        performOnExtraScreen {
            waitUntil { containsErrorMosRuItem() }
            isMosRuItemMatches("$SCREENSHOT_DIR/requestingErrorStatusItem")
            tapOn(lookup.matchesMosRuItem())
        }

        performOnMosRuStatusScreen {
            waitUntil { isMosRuStatusDialogShown() }
            isMosRuStatusDialogMatches("$SCREENSHOT_DIR/requestingErrorStatusDialog")
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

        performOnExtraScreen {
            waitUntil { containsRequestedMosRuItem() }
            isMosRuItemMatches("$SCREENSHOT_DIR/requestedStatusItem")
        }
    }

    @Test
    fun retryLoadStatusAfterMosRuBindingWhenLoadUserFailed() {
        configureWebServer {
            registerUserProfile(true, MosRuStatus.NOT_PROCESSED)
            registerNaturalPersonServicesInfo()
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskId()
            registerUserProfileError()
            registerNaturalPersonServicesInfo()
            registerUserProfile(true, MosRuStatus.REQUESTED)
            registerNaturalPersonServicesInfo()
        }

        prepareMosRuBinding()
        authorizationRule.setUserAuthorized()

        performOnExtraScreen {
            waitUntil { containsNotLinkedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/notProcessedStatusBlock")
            tapOn(lookup.matchesMosRuBlockBindButton())
        }

        performOnMosRuBindingScreen {
            waitUntil { isMosRuBindingScreenShown() }
            intendedOpenMosRu()
            waitUntil { isMosRuBindingScreenHidden() }
        }

        performOnExtraScreen {
            waitUntil { containsLoadErrorMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/errorBlock")

            tapOn(lookup.matchesMosRuBlockRetryButton())

            waitUntil { containsRequestedMosRuItem() }
            isMosRuItemMatches("$SCREENSHOT_DIR/requestedStatusItem")
        }
    }

    @Test
    fun retryBindingWhenErrors() {
        configureWebServer {
            registerUserProfile(true, MosRuStatus.NOT_PROCESSED)
            registerNaturalPersonServicesInfo()
            registerMosRuBindingUrlError()
            registerMosRuBindingUrl()
            registerMosRuSubmitTaskIdError()
            registerMosRuSubmitTaskId()
            registerUserProfile(true, MosRuStatus.REQUESTED)
            registerNaturalPersonServicesInfo()
        }

        prepareMosRuBinding()
        authorizationRule.setUserAuthorized()

        performOnExtraScreen {
            waitUntil { containsNotLinkedMosRuBlock() }
            isMosRuBlockMatches("$SCREENSHOT_DIR/notProcessedStatusBlock")
            tapOn(lookup.matchesMosRuBlockBindButton())
        }

        performOnMosRuBindingScreen {
            waitUntil { isMosRuBindingScreenShown() }

            isRetryButtonShown()
            tapOn(lookup.matchesRetryButton())

            isProgressShown()
            intendedOpenMosRu()

            waitUntil { isRetryButtonShown() }
            tapOn(lookup.matchesRetryButton())

            waitUntil { isMosRuBindingScreenHidden() }
        }

        performOnExtraScreen {
            waitUntil { containsRequestedMosRuItem() }
            isMosRuItemMatches("$SCREENSHOT_DIR/requestedStatusItem")
        }
    }

    @Test
    fun shouldNotShowMosRuBlockWhenMosRuNotAvailableForUser() {
        configureWebServer {
            registerUserProfile(false, MosRuStatus.NOT_PROCESSED)
            registerNaturalPersonServicesInfo()
        }

        authorizationRule.setUserAuthorized()

        performOnExtraScreen {
            waitUntil { containsAccountItem() }
            doesNotContainsMosRuBlock()
        }
    }

    @Test
    fun shouldNotShowMosRuItemWhenMosRuNotAvailableForUser() {
        configureWebServer {
            registerUserProfile(false, MosRuStatus.REQUESTED)
            registerNaturalPersonServicesInfo()
        }

        authorizationRule.setUserAuthorized()

        performOnExtraScreen {
            waitUntil { containsAccountItem() }
            doesNotContainsMosRuItem()
        }
    }

    @Test
    fun shouldNotShowMosRuWhenUserNotAuthorized() {
        configureWebServer {
            registerUserProfile(false, MosRuStatus.REQUESTED)
            registerNaturalPersonServicesInfo()
        }

        performOnExtraScreen {
            waitUntil { containsAccountItem() }
            doesNotContainsMosRuBlock()
            doesNotContainsMosRuItem()
        }
    }

    @Test
    fun shouldNotShowMosRuWhenLoadUserFailed() {
        configureWebServer {
            registerUserProfileError()
            registerNaturalPersonServicesInfo()
        }

        authorizationRule.setUserAuthorized()

        performOnExtraScreen {
            waitUntil { containsAccountItem() }
            doesNotContainsMosRuBlock()
            doesNotContainsMosRuItem()
        }
    }

    private fun DispatcherRegistry.registerUserProfile(
        mosRuAvailable: Boolean,
        status: MosRuStatus
    ) {
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
                        "mosRuAvailable": $mosRuAvailable,
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

    private companion object {

        private const val SCREENSHOT_DIR = "extra/mosru"
    }
}
