package com.yandex.mobile.realty.test.deeplink

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.PushBroadcastCommand
import com.yandex.mobile.realty.core.robot.performOnAddressSelectScreen
import com.yandex.mobile.realty.core.robot.performOnAuthWebView
import com.yandex.mobile.realty.core.robot.performOnExtraScreen
import com.yandex.mobile.realty.core.robot.performOnNotificationShade
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.BottomNavMenu
import com.yandex.mobile.realty.core.screen.NotificationScreen
import com.yandex.mobile.realty.core.screen.PaymentCardsScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.ServicesScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author andrikeev on 26/08/2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class NotificationDeepLinkTest {

    private val authorizationRule = AuthorizationRule()
    private val activityRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION),
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityRule,
    )

    @Test
    fun shouldOpenServices() {
        val title = "Пуш на раздел Сервисы"
        val body = "Сейчас откроются Сервисы"
        PushBroadcastCommand.sendPush(
            action = "open_services",
            params = jsonObject {
                "push_id" to "test"
                "title" to title
                "body" to body
            }.toString(),
            name = title
        )

        onScreen<NotificationScreen> {
            hasNotification(title, body)
            clickOnNotification()
        }

        onScreen<ServicesScreen> {
            rentPromoItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldOpenPaymentCardsScreen() {
        authorizationRule.setUserAuthorized()

        val title = "Карты оплаты"
        val body = "Добавьте карту оплаты"
        PushBroadcastCommand.sendPush(
            action = "deeplink",
            params = """
                    {
                        "push_id": "BANK_CARDS",
                        "title": "$title",
                        "body": "$body",
                        "url": "yandexrealty://realty.yandex.ru/bank-cards",
                        "recipient_id": "1"
                    }
            """.trimIndent(),
            name = "Промо \"Карты оплаты\""
        )

        performOnNotificationShade {
            hasNotification(title, body)
            clickOnNotification()
        }

        onScreen<PaymentCardsScreen> {
            waitUntil { contentView.isCompletelyDisplayed() }
            pressBack()
        }

        performOnExtraScreen {
            waitUntil { isToolbarTitleShown() }
        }
    }

    @Test
    fun shouldOpenCommuteAddressPicker() {
        configureWebServer {
            register(
                request {
                    path("1.0/addressGeocoder.json")
                    queryParam("latitude", AURORA_LATITUDE.toString())
                    queryParam("longitude", AURORA_LONGITUDE.toString())
                },
                response {
                    assetBody("geocoderAddressAurora.json")
                }
            )
        }

        val title = "Время на дорогу"
        val body = "Найдите квартиру рядом с работой"
        PushBroadcastCommand.sendPush(
            action = "commute_promo",
            params = """
                    {
                        "push_id": "COMMUTE_PROMO",
                        "title": "$title",
                        "body": "$body",
                        "payload": {
                            "type": "SELL",
                            "category": "APARTMENT",
                            "point": {
                                "latitude": $AURORA_LATITUDE,
                                "longitude": $AURORA_LONGITUDE
                            }
                        }
                    }
            """.trimIndent(),
            name = "Промо \"Время на дорогу\""
        )

        performOnNotificationShade {
            hasNotification(title, body)
            clickOnNotification()
        }

        performOnAddressSelectScreen {
            waitUntil { isAddressContainerShown() }
            isAddressEquals(EXPECTED_ADDRESS)
        }
    }

    @Test
    fun shouldOpenExcerptReportWebView() {
        authorizationRule.setUserAuthorized()

        val title = "Отчёт по квартире"
        val body = "Ура! Отчёт сформирован!"
        PushBroadcastCommand.sendPush(
            action = "deeplink",
            params = """
                    {
                        "push_id": "ANY_PUSH_ID",
                        "title": "$title",
                        "body": "$body",
                        "url": "https://m.realty.yandex.ru/egrn-report/123"
                    }
            """.trimIndent(),
            name = "Отчёт по квартире"
        )

        performOnNotificationShade {
            hasNotification(title, body)
            clickOnNotification()
        }

        performOnAuthWebView {
            waitUntil {
                isPageUrlEquals("https://m.realty.yandex.ru/egrn-report/123/?only-content=true")
            }
        }
    }

    @Test
    fun shouldOpenExcerptReportWebViewIfAppOpen() {
        authorizationRule.setUserAuthorized()

        activityRule.launchActivity()

        onScreen<BottomNavMenu> {
            bottomNavView.waitUntil { isCompletelyDisplayed() }
        }

        val title = "Отчёт по квартире"
        val body = "Ура! Отчёт сформирован!"
        PushBroadcastCommand.sendPush(
            action = "deeplink",
            params = """
                    {
                        "push_id": "ANY_PUSH_ID",
                        "title": "$title",
                        "body": "$body",
                        "url": "https://m.realty.yandex.ru/egrn-report/123"
                    }
            """.trimIndent(),
            name = "Отчёт по квартире"
        )

        performOnNotificationShade {
            hasNotification(title, body)
            clickOnNotification()
        }

        performOnAuthWebView {
            waitUntil {
                isPageUrlEquals("https://m.realty.yandex.ru/egrn-report/123/?only-content=true")
            }
        }
    }

    @Test
    fun shouldOpenRealtySubdomainWebView() {
        val title = "Промоакция"
        val body = "Тест промоакции"
        val url = "https://arenda.realty.yandex.ru/"
        PushBroadcastCommand.sendPush(
            action = "deeplink",
            params = """
                    {
                        "push_id": "ANY_PUSH_ID",
                        "title": "$title",
                        "body": "$body",
                        "url": "$url"
                    }
            """.trimIndent(),
            name = "Промо"
        )

        performOnNotificationShade {
            hasNotification(title, body)
            clickOnNotification()
        }

        onScreen<WebViewScreen> {
            waitUntil {
                webView.isPageUrlEquals("$url?only-content=true")
            }
        }
    }

    @Test
    fun shouldOpenYandexSubdomainWebView() {
        val title = "Промоакция"
        val body = "Тест промоакции"
        val url = "https://translate.yandex.ru/"
        PushBroadcastCommand.sendPush(
            action = "deeplink",
            params = """
                    {
                        "push_id": "ANY_PUSH_ID",
                        "title": "$title",
                        "body": "$body",
                        "url": "$url"
                    }
            """.trimIndent(),
            name = "Промо"
        )

        performOnNotificationShade {
            hasNotification(title, body)
            clickOnNotification()
        }

        onScreen<WebViewScreen> {
            waitUntil {
                webView.isPageUrlEquals("$url?only-content=true")
            }
        }
    }

    @Test
    fun shouldOpenYandexDomainWebView() {
        val title = "Промоакция"
        val body = "Тест промоакции"
        val url = "https://yandex.ru/favorites/"
        PushBroadcastCommand.sendPush(
            action = "deeplink",
            params = """
                    {
                        "push_id": "ANY_PUSH_ID",
                        "title": "$title",
                        "body": "$body",
                        "url": "$url"
                    }
            """.trimIndent(),
            name = "Промо"
        )

        performOnNotificationShade {
            hasNotification(title, body)
            clickOnNotification()
        }

        onScreen<WebViewScreen> {
            waitUntil {
                webView.isPageUrlEquals("$url?only-content=true")
            }
        }
    }

    companion object {

        const val AURORA_LATITUDE = 55.734655
        const val AURORA_LONGITUDE = 37.642313
        const val EXPECTED_ADDRESS = "Садовническая улица, 82с2"
    }
}
