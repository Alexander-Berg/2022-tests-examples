package com.yandex.mobile.realty.test.startup

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.metrica.event
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SessionStartupRequiredRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchMapScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author pvl-zolotov on 21.07.2022
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class StartupTest : BaseTest() {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)
    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        activityTestRule,
        authorizationRule,
        MetricaEventsRule(),
        SessionStartupRequiredRule()
    )

    @Test
    fun shouldReportAppLaunchUnauthorized() {
        activityTestRule.launchActivity()

        val appLaunchMetricaEvent = event("Запуск приложения") {
            "Пользователь" to jsonObject {
                "Авторизован" to "нет"
                "Тип пользователя" to "Не пользовался ЛК"
            }
        }
        onScreen<SearchMapScreen> {
            waitUntil { appLaunchMetricaEvent.isOccurred() }
        }
    }

    @Test
    fun shouldReportAppLaunchOffersOwner() {
        configureWebServer {
            registerUserProfile()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        val appLaunchMetricaEvent = event("Запуск приложения") {
            "Пользователь" to jsonObject {
                "Авторизован" to "да"
                "Тип пользователя" to "Пользовался ЛК"
            }
        }
        onScreen<SearchMapScreen> {
            waitUntil { appLaunchMetricaEvent.isOccurred() }
        }
    }

    @Test
    fun shouldReportAppLaunchNotOffersOwner() {
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        val appLaunchMetricaEvent = event("Запуск приложения") {
            "Пользователь" to jsonObject {
                "Авторизован" to "да"
                "Тип пользователя" to "Не пользовался ЛК"
            }
        }
        onScreen<SearchMapScreen> {
            waitUntil { appLaunchMetricaEvent.isOccurred() }
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
}
