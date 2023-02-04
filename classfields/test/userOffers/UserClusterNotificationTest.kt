package com.yandex.mobile.realty.test.userOffers

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.allure.step
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.DateRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.BottomNavMenu
import com.yandex.mobile.realty.core.screen.ClusterNotificationScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.ServicesScreen
import com.yandex.mobile.realty.core.screen.UserOffersScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author andrey-bgm on 22/06/2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class UserClusterNotificationTest {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)
    private val authorizationRule = AuthorizationRule()
    private val dateRule = DateRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule,
        dateRule
    )

    @Test
    fun showClusterNotificationOnceWhenUserAcceptedAtFirstTime() {
        configureWebServer {
            registerServicesInfo()
            registerNoRequiredFeatures()
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserProfile()
            registerUserProfile()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<BottomNavMenu> {
            waitUntil { bottomNavView.isCompletelyDisplayed() }
            servicesItemView.click(true)
        }
        onScreen<ServicesScreen> {
            userOffersHeaderItem.waitUntil { listView.contains(this) }
            userOffersTitleView.click()
        }

        onScreen<ClusterNotificationScreen> {
            waitUntil { promoView.isCompletelyDisplayed() }
            root.isViewStateMatches("UserClusterNotificationTest/promoDialog")
            allClearButton.click()
        }

        onScreen<UserOffersScreen> {
            contentView.isCompletelyDisplayed()
        }

        step("Переводим время на 24 ч вперёд") {
            dateRule.addHours(24)
        }
        closeScreenThenBackToOffers()

        onScreen<UserOffersScreen> {
            contentView.isCompletelyDisplayed()
        }
    }

    @Test
    fun showClusterNotificationTwiceWhenUserJustCloseIt() {
        configureWebServer {
            registerServicesInfo()
            registerNoRequiredFeatures()
            registerNoRequiredFeatures()
            registerNoRequiredFeatures()
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserProfile()
            registerUserProfile()
            registerUserProfile()
            registerUserProfile()
            registerUserProfile()
            registerUserProfile()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<BottomNavMenu> {
            waitUntil { bottomNavView.isCompletelyDisplayed() }
            servicesItemView.click(true)
        }
        onScreen<ServicesScreen> {
            userOffersHeaderItem.waitUntil { listView.contains(this) }
            userOffersTitleView.click()
        }

        onScreen<ClusterNotificationScreen> {
            waitUntil { promoView.isCompletelyDisplayed() }
            root.isViewStateMatches("UserClusterNotificationTest/promoDialog")
            toolbarCloseButton.click()
        }

        onScreen<UserOffersScreen> {
            contentView.isCompletelyDisplayed()
        }

        step("Переводим время на 2 ч вперёд") {
            dateRule.addHours(2)
        }
        closeScreenThenBackToOffers()

        onScreen<UserOffersScreen> {
            contentView.isCompletelyDisplayed()
        }

        step("Переводим время на 22 ч вперёд") {
            dateRule.addHours(22)
        }
        closeScreenThenBackToOffers()

        onScreen<ClusterNotificationScreen> {
            waitUntil { promoView.isCompletelyDisplayed() }
            root.isViewStateMatches("UserClusterNotificationTest/promoDialog")
            toolbarCloseButton.click()
        }

        step("Переводим время на 24 ч вперёд") {
            dateRule.addHours(24)
        }
        closeScreenThenBackToOffers()

        onScreen<UserOffersScreen> {
            contentView.isCompletelyDisplayed()
        }
    }

    @Test
    fun openPaidDurationTermsInWebView() {
        configureWebServer {
            registerServicesInfo()
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<BottomNavMenu> {
            waitUntil { bottomNavView.isCompletelyDisplayed() }
            servicesItemView.click(true)
        }
        onScreen<ServicesScreen> {
            userOffersHeaderItem.waitUntil { listView.contains(this) }
            userOffersTitleView.click()
        }

        onScreen<ClusterNotificationScreen> {
            waitUntil { promoView.isCompletelyDisplayed() }
            readTermsButton.click()
        }

        val expectedUrl = "https://yandex.ru/support/realty/paid.html?only-content=true"
        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(expectedUrl) }
        }
    }

    @Test
    fun openOfferRequirementsInWebView() {
        configureWebServer {
            registerServicesInfo()
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<BottomNavMenu> {
            waitUntil { bottomNavView.isCompletelyDisplayed() }
            servicesItemView.click(true)
        }
        onScreen<ServicesScreen> {
            userOffersHeaderItem.waitUntil { listView.contains(this) }
            userOffersTitleView.click()
        }

        onScreen<ClusterNotificationScreen> {
            waitUntil { promoView.isCompletelyDisplayed() }
            descriptionView.tapOnLinkText("нашим правилам")
        }

        val expectedUrl =
            "https://yandex.ru/support/realty/rules/requirements-ads.html?only-content=true"
        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(expectedUrl) }
        }
    }

    private fun closeScreenThenBackToOffers() {
        onScreen<UserOffersScreen> {
            pressBack()
        }
        onScreen<ServicesScreen> {
            userOffersHeaderItem.waitUntil { listView.contains(this) }
            userOffersTitleView.click()
        }
    }

    private fun DispatcherRegistry.registerServicesInfo() {
        register(
            request {
                path("2.0/service/info")
            },
            response {
                assetBody("UserClusterNotificationTest/servicesInfo.json")
            }
        )
    }

    private fun DispatcherRegistry.registerNoRequiredFeatures() {
        register(
            request {
                path("1.0/device/requiredFeature")
            },
            response {
                setBody("{\"response\": {\"userOffers\": []}}")
            }
        )
    }

    private fun DispatcherRegistry.registerUserProfile() {
        register(
            request {
                path("1.0/user")
            },
            response {
                assetBody("user/userAgentOverQuota.json")
            }
        )
    }
}
