package com.yandex.mobile.realty.test.cards

import com.yandex.mobile.realty.activity.PaymentCardsActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.PaymentCardsScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.registerNaturalPersonServicesInfo
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.*

/**
 * @author misha-kozlov on 2020-03-16
 */
@RunWith(Parameterized::class)
class PaymentCardTest(
    private val brand: String,
    private val titlePrefix: String
) : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = PaymentCardsActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        authorizationRule,
        activityTestRule
    )

    @Test
    fun shouldShowCard() {
        configureWebServer {
            registerNaturalPersonServicesInfo()
            registerPaymentCard(brand)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<PaymentCardsScreen> {
            cardSnippet("$titlePrefix ***4444").waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath(brand.lowercase(Locale.ROOT)))
        }
    }

    private fun DispatcherRegistry.registerPaymentCard(brand: String) {
        register(
            request {
                path("2.0/banker/user/me/cards")
            },
            response {
                setBody(
                    """{
                                "response": {
                                    "methods": [{
                                        "psId": "YANDEXKASSA_V3",
                                        "id": "bank_card",
                                        "name": "Банковская карта",
                                        "properties": {
                                            "card": {
                                                "cddPanMask": "555555|4444",
                                                "brand": "$brand",
                                                "expireYear": "2022",
                                                "expireMonth": "2"
                                            }
                                        },
                                        "needEmail": false,
                                        "preferred": true
                                    }]
                                } 
                            }"""
                )
            }
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): Iterable<Array<*>> {
            return listOf(
                arrayOf("MASTERCARD", "MasterCard"),
                arrayOf("VISA", "Visa"),
                arrayOf("MAESTRO", "Maestro"),
                arrayOf("MIR", "MIR"),
                arrayOf("UNION_PAY", "UnionPay"),
                arrayOf("JCB", "JCB"),
                arrayOf("AMERICAN_EXPRESS", "American Express"),
                arrayOf("DINERS_CLUB", "Diners Club"),
                arrayOf("UNKNOWN_CARD_BRAND", "Банковская карта")
            )
        }
    }
}
