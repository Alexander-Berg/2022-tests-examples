package com.yandex.mobile.realty.test.userOffers

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.core.PushBroadcastCommand
import com.yandex.mobile.realty.core.pressBack
import com.yandex.mobile.realty.core.robot.performOnNotificationShade
import com.yandex.mobile.realty.core.robot.performOnUserOfferCardScreen
import com.yandex.mobile.realty.core.robot.performOnUserOffersScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author andrey-bgm on 30/10/2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class UserOfferNotificationTest {

    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
    )

    @After
    fun closeNotificationAfterTest() {
        pressBack()
    }

    @Test
    fun openUserOfferCardFromPush() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOffer()
        }
        authorizationRule.setUserAuthorized()

        sendPush("1")

        performOnNotificationShade {
            hasNotification(TITLE, BODY)
            clickOnNotification()
        }

        performOnUserOfferCardScreen {
            waitUntil { isPriceViewShown() }
            pressBack()
        }

        performOnUserOffersScreen {
            waitUntil { isRootViewShown() }
        }
    }

    @Test
    fun shouldNotShowPushNotificationWhenUserNotAuthorized() {
        sendPush("1")

        performOnNotificationShade {
            hasNoNotifications()
        }
    }

    @Test
    fun shouldNotShowPushNotificationWhenUidDoesNotMatch() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserOffer()
        }
        authorizationRule.setUserAuthorized()

        sendPush("2")

        performOnNotificationShade {
            hasNoNotifications()
        }
    }

    private fun sendPush(recipientId: String) {
        PushBroadcastCommand.sendPush(
            action = "deeplink",
            params = """
                        {
                            "push_id": "SOCIAL",
                            "title": "$TITLE",
                            "body": "$BODY",
                            "url": "yandexrealty://realty.yandex.ru/management-new/offer/1",
                            "recipient_id": "$recipientId"
                        }
            """.trimIndent(),
            name = "Напоминание \"$TITLE\""
        )
    }

    private fun DispatcherRegistry.registerUserProfile() {
        register(
            request {
                path("1.0/user")
            },
            response {
                assetBody("user/userOwner.json")
            }
        )
    }

    private fun DispatcherRegistry.registerUserOffer() {
        register(
            request {
                path("2.0/user/me/offers/1/card")
            },
            response {
                assetBody("userOffer/userOfferPublishedFree.json")
            }
        )
    }

    private companion object {

        const val TITLE = "Привяжите mos.ru"
        const val BODY = "Привяжите mos.ru и получите статус проверенного собственника"
    }
}
