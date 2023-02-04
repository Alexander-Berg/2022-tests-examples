package com.yandex.mobile.realty.test.extra

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.core.robot.performOnExtraScreen
import com.yandex.mobile.realty.core.robot.performOnSettingsScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.BottomNavMenu
import com.yandex.mobile.realty.core.screen.ChatMessagesScreen
import com.yandex.mobile.realty.core.screen.ExtraScreen
import com.yandex.mobile.realty.core.screen.PaymentCardsScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.services.PAYMENT_TYPE_JURIDICAL_PERSON
import com.yandex.mobile.realty.test.services.RENT_ROLE_OWNER
import com.yandex.mobile.realty.test.services.registerNaturalPersonServicesInfo
import com.yandex.mobile.realty.test.services.registerServicesInfo
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Created by Alena Malchikhina on 24.12.2019
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ExtraScreenTest {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = MainActivityTestRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun shouldOpenSupportChatScreen() {
        configureWebServer {
            registerTechSupportChat()
        }
        authorizationRule.setUserAuthorized()

        onScreen<BottomNavMenu> {
            waitUntil { bottomNavView.isCompletelyDisplayed() }
            extraItemView.click(true)
        }
        onScreen<ExtraScreen> {
            listView.waitUntil { contains(supportChatItem) }
                .scrollTo(supportChatItem)
                .click()
        }
        onScreen<ChatMessagesScreen> {
            waitUntil { titleView.isTextEquals(R.string.chat_support_title) }
            pressBack()
        }
        onScreen<ExtraScreen> {
            toolbarTitle.waitUntil { isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldOpenSupportChatScreenWithAuthorization() {
        configureWebServer {
            registerTechSupportChat()
        }
        authorizationRule.registerAuthorizationIntent()

        onScreen<BottomNavMenu> {
            waitUntil { bottomNavView.isCompletelyDisplayed() }
            extraItemView.click(true)
        }
        performOnExtraScreen {
            scrollToPosition(lookup.matchesSupportChatItem()).tapOn()
        }
        onScreen<ChatMessagesScreen> {
            waitUntil { titleView.isTextEquals(R.string.chat_support_title) }
            pressBack()
        }
        performOnExtraScreen {
            waitUntil { isToolbarTitleShown() }
        }
    }

    @Test
    fun shouldOpenPaymentCardsScreen() {
        configureWebServer {
            registerUserOwnerProfile()
            registerUserOwnerProfile()
            registerNaturalPersonServicesInfo()
        }

        authorizationRule.setUserAuthorized()

        onScreen<BottomNavMenu> {
            waitUntil { bottomNavView.isCompletelyDisplayed() }
            extraItemView.click(true)
        }
        performOnExtraScreen {
            waitUntil { containsAccountItem() }
            scrollToPosition(lookup.matchesPaymentCardsItem()).tapOn()
        }
        onScreen<PaymentCardsScreen> {
            waitUntil { contentView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldShowPaymentCardsForAuthorizedUserOnly() {
        configureWebServer {
            registerUserOwnerProfile()
            registerUserOwnerProfile()
            registerNaturalPersonServicesInfo()
        }

        onScreen<BottomNavMenu> {
            waitUntil { bottomNavView.isCompletelyDisplayed() }
            extraItemView.click(true)
        }
        performOnExtraScreen {
            doesNotContainsPaymentCardsItem()

            authorizationRule.setUserAuthorized()

            waitUntil { containsPaymentCardsItem() }
        }
    }

    @Test
    fun shouldShowPaymentCardsForRentOwner() {
        configureWebServer {
            registerUserAgencyProfile()
            registerUserAgencyProfile()
            registerServicesInfo(
                rentRole = RENT_ROLE_OWNER,
                paymentType = PAYMENT_TYPE_JURIDICAL_PERSON
            )
        }

        authorizationRule.setUserAuthorized()

        onScreen<BottomNavMenu> {
            waitUntil { bottomNavView.isCompletelyDisplayed() }
            extraItemView.click(true)
        }
        performOnExtraScreen {
            waitUntil { containsAccountItem() }
            containsPaymentCardsItem()
        }
    }

    @Test
    fun shouldNotShowPaymentCardsForJuridicalPerson() {
        configureWebServer {
            registerUserAgencyProfile()
            registerUserAgencyProfile()
            registerServicesInfo(paymentType = PAYMENT_TYPE_JURIDICAL_PERSON)
        }

        authorizationRule.setUserAuthorized()

        onScreen<BottomNavMenu> {
            waitUntil { bottomNavView.isCompletelyDisplayed() }
            extraItemView.click(true)
        }

        performOnExtraScreen {
            waitUntil { containsAccountItem() }
            doesNotContainsPaymentCardsItem()
        }
    }

    @Test
    fun shouldNotShowPaymentCardsForNewUser() {
        configureWebServer {
            registerNewUserProfile()
            registerNewUserProfile()
            registerServicesInfo()
        }

        authorizationRule.setUserAuthorized()

        onScreen<BottomNavMenu> {
            waitUntil { bottomNavView.isCompletelyDisplayed() }
            extraItemView.click(true)
        }

        performOnExtraScreen {
            waitUntil { containsAccountItem() }
            doesNotContainsPaymentCardsItem()
        }
    }

    @Test
    fun shouldNotShowPaymentCardsWhenProfileLoadFailed() {
        configureWebServer {
            registerUserProfileError()
            registerUserProfileError()
        }

        authorizationRule.setUserAuthorized()

        onScreen<BottomNavMenu> {
            waitUntil { bottomNavView.isCompletelyDisplayed() }
            extraItemView.click(true)
        }

        performOnExtraScreen {
            waitUntil { containsAccountItem() }
            doesNotContainsPaymentCardsItem()
        }
    }

    @Test
    fun shouldOpenSettingsScreen() {
        onScreen<BottomNavMenu> {
            waitUntil { bottomNavView.isCompletelyDisplayed() }
            extraItemView.click(true)
        }
        performOnExtraScreen {
            scrollToPosition(lookup.matchesSettingsItem()).tapOn()
        }
        performOnSettingsScreen {
            isSettingsToolbarShown()
        }
    }

    @Test
    fun shouldShowAccountItem() {
        onScreen<BottomNavMenu> {
            waitUntil { bottomNavView.isCompletelyDisplayed() }
            extraItemView.click(true)
        }
        performOnExtraScreen {
            containsAccountItem()
        }
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

    private fun DispatcherRegistry.registerUserOwnerProfile() {
        register(
            request {
                path("1.0/user")
            },
            response {
                assetBody("user/userOwner.json")
            }
        )
    }

    private fun DispatcherRegistry.registerUserAgencyProfile() {
        register(
            request {
                path("1.0/user")
            },
            response {
                assetBody("user/userAgency.json")
            }
        )
    }

    private fun DispatcherRegistry.registerNewUserProfile() {
        register(
            request {
                path("1.0/user")
                method("GET")
            },
            response {
                assetBody("user/newUser.json")
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
}
