package com.yandex.mobile.realty.test.promo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ServicesActivityTestRule
import com.yandex.mobile.realty.activity.UserOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.ServicesScreen
import com.yandex.mobile.realty.core.screen.SettingsScreen
import com.yandex.mobile.realty.core.screen.UserOffersScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.ExpectedRequest
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.core.webserver.success
import com.yandex.mobile.realty.test.BaseTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author merionkov on 06.10.2021.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ChatsPromoTest : BaseTest() {

    private val userOffersActivityRule = UserOffersActivityTestRule(launchActivity = false)
    private val servicesActivityRule = ServicesActivityTestRule(launchActivity = false)
    private val appStateRule = SetupDefaultAppStateRule()
    private val authRule = AuthorizationRule()

    @JvmField
    @Rule
    var ruleChain = baseChainOf(
        userOffersActivityRule,
        servicesActivityRule,
        appStateRule,
        authRule,
    )

    @Before
    fun setUp() {
        authRule.setUserAuthorized()
    }

    @Test
    fun shouldShowChatsPromo() {
        appStateRule.setState {
            shouldShowUserOffersChatsPromo.set(true)
        }
        configureWebServer {
            registerUserPersonChatsDisabledProfile()
            registerUserPersonChatsDisabledProfile()
            registerNoRequiredFeatures()
            registerUserOffers()
        }
        userOffersActivityRule.launchActivity()
        onScreen<UserOffersScreen> {
            chatsPromoItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("chatsPromo"))
        }
    }

    @Test
    fun shouldEnableChatsFromPromo() {
        appStateRule.setState {
            shouldShowUserOffersChatsPromo.set(true)
        }
        val dispatcherRegistry = DispatcherRegistry().apply {
            registerUserPersonChatsDisabledProfile()
            registerUserPersonChatsDisabledProfile()
            registerNoRequiredFeatures()
            registerUserOffers()
        }
        val enableChatsRequest = dispatcherRegistry.registerEnableChats()
        configureWebServer(dispatcherRegistry)
        userOffersActivityRule.launchActivity()
        onScreen<UserOffersScreen> {
            listView.waitUntil { contains(chatsPromoItem) }
            chatsPromoEnableButton.click()
            listView.waitUntil { doesNotContain(chatsPromoItem) }
            enableChatsRequest.waitUntil { isOccured() }
        }
    }

    @Test
    fun shouldGoToSettingsFromPromo() {
        appStateRule.setState {
            shouldShowUserOffersChatsPromo.set(true)
        }
        configureWebServer {
            registerUserPersonChatsDisabledProfile()
            registerUserPersonChatsDisabledProfile()
            registerNoRequiredFeatures()
            registerUserOffers()
        }
        userOffersActivityRule.launchActivity()
        onScreen<UserOffersScreen> {
            listView.waitUntil { contains(chatsPromoItem) }
            chatsPromoSettingsButton.click()
        }
        onScreen<SettingsScreen> {
            listView.waitUntil { contains(communicationTypeItem) }
        }
    }

    @Test
    fun shouldHideAndNotShowPromoAgain() {
        appStateRule.setState {
            shouldShowUserOffersChatsPromo.set(true)
        }
        val dispatcherRegistry = DispatcherRegistry().apply {
            registerServicesInfo()
            registerUserPersonChatsDisabledProfile()
            registerUserPersonChatsDisabledProfile()
            registerNoRequiredFeatures()
            registerUserOffers()
            registerUserPersonChatsDisabledProfile()
            registerNoRequiredFeatures()
            registerUserOffers()
        }
        configureWebServer(dispatcherRegistry)
        servicesActivityRule.launchActivity()
        onScreen<ServicesScreen> {
            userOffersHeaderItem.waitUntil { listView.contains(this) }
            userOffersTitleView.click()
        }
        onScreen<UserOffersScreen> {
            listView.waitUntil { contains(chatsPromoItem) }
            chatsPromoHideButton.click()
            listView.waitUntil { doesNotContain(chatsPromoItem) }
            pressBack()
        }
        onScreen<ServicesScreen> {
            userOffersHeaderItem.waitUntil { listView.contains(this) }
            userOffersTitleView.click()
        }
        onScreen<UserOffersScreen> {
            listView
                .waitUntil { contains(offerSnippet("1")) }
                .doesNotContain(chatsPromoItem)
        }
    }

    @Test
    fun shouldNotShowPromoIfChatsEnabled() {
        appStateRule.setState {
            shouldShowUserOffersChatsPromo.set(true)
        }
        configureWebServer {
            registerUserPersonChatsEnabledProfile()
            registerUserPersonChatsEnabledProfile()
            registerNoRequiredFeatures()
            registerUserOffers()
        }
        userOffersActivityRule.launchActivity()
        onScreen<UserOffersScreen> {
            listView
                .waitUntil { contains(offerSnippet("1")) }
                .doesNotContain(chatsPromoItem)
        }
    }

    @Test
    fun shouldNotShowPromoToJuridicalPerson() {
        appStateRule.setState {
            shouldShowUserOffersChatsPromo.set(true)
        }
        configureWebServer {
            registerUserJuridicProfile()
            registerUserJuridicProfile()
            registerNoRequiredFeatures()
            registerUserOffers()
        }
        userOffersActivityRule.launchActivity()
        onScreen<UserOffersScreen> {
            listView
                .waitUntil { contains(offerSnippet("1")) }
                .doesNotContain(chatsPromoItem)
        }
    }

    private fun DispatcherRegistry.registerNoRequiredFeatures(): ExpectedRequest {
        return register(
            request {
                method("GET")
                path("1.0/device/requiredFeature")
            },
            response {
                setBody("{\"response\": {\"userOffers\": []}}")
            },
        )
    }

    private fun DispatcherRegistry.registerUserOffers(): ExpectedRequest {
        return register(
            request {
                method("GET")
                path("2.0/user/me/offers")
            },
            response {
                assetBody("ChatsPromo/userOffers.json")
            },
        )
    }

    private fun DispatcherRegistry.registerUserPersonChatsDisabledProfile(): ExpectedRequest {
        return register(
            request {
                path("1.0/user")
                method("GET")
            },
            response {
                assetBody("ChatsPromo/userPersonChatsDisabled.json")
            },
        )
    }

    private fun DispatcherRegistry.registerUserPersonChatsEnabledProfile(): ExpectedRequest {
        return register(
            request {
                path("1.0/user")
                method("GET")
            },
            response {
                assetBody("ChatsPromo/userPersonChatsEnabled.json")
            },
        )
    }

    private fun DispatcherRegistry.registerUserJuridicProfile(): ExpectedRequest {
        return register(
            request {
                path("1.0/user")
                method("GET")
            },
            response {
                assetBody("ChatsPromo/userJuridic.json")
            },
        )
    }

    private fun DispatcherRegistry.registerEnableChats(): ExpectedRequest {
        return register(
            request {
                path("1.0/user")
                method("PATCH")
                assetBody("ChatsPromo/enableChats.json")
            },
            response {
                success()
            },
        )
    }

    private fun DispatcherRegistry.registerServicesInfo() {
        register(
            request {
                path("2.0/service/info")
            },
            response {
                assetBody("ChatsPromo/servicesInfo.json")
            }
        )
    }
}
