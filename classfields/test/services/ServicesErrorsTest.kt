package com.yandex.mobile.realty.test.services

import com.yandex.mobile.realty.activity.ServicesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.InternetRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.ServicesScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.error
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.test.BaseTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author sorokinandrei on 9/10/21.
 */
class ServicesErrorsTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val internetRule = InternetRule()
    private val activityTestRule = ServicesActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        internetRule,
        activityTestRule
    )

    @Test
    fun shouldShowErrors() {
        configureWebServer {
            registerServicesError()
            repeat(2) {
                registerServicesInfo()
            }
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            errorItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("serverError"))
                .invoke {
                    retryButton.click()
                }

            rentPromoItem.waitUntil { listView.contains(this) }

            internetRule.turnOff()

            listView.swipeDown()

            errorItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("networkError"))

            internetRule.turnOn()

            errorItem.view {
                retryButton.click()
            }

            rentPromoItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldNotShowErrorForUnauthorizedUser() {
        configureWebServer {
            registerServicesError()
        }

        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            waitUntil { listView.contains(rentPromoItem) }
            listView.doesNotContain(errorItem)
        }
    }

    @Test
    fun shouldRefreshContent() {
        configureWebServer {
            registerServicesError()
            registerServicesInfo()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            errorItem.waitUntil { listView.contains(this) }

            listView.swipeDown()

            rentPromoItem.waitUntil { listView.contains(this) }
        }
    }

    private fun DispatcherRegistry.registerServicesError() {
        register(
            request {
                path("2.0/service/info")
            },
            error()
        )
    }
}
