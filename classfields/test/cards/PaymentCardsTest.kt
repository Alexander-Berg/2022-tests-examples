package com.yandex.mobile.realty.test.cards

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.PaymentCardsActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ConfirmationDialogScreen
import com.yandex.mobile.realty.core.screen.EmailInputScreen
import com.yandex.mobile.realty.core.screen.PaymentCardsScreen
import com.yandex.mobile.realty.core.screen.RentOwnerCardBindingScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.viewMatchers.NamedIntentMatcher
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.error
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.core.webserver.success
import com.yandex.mobile.realty.fragment.RedirectWebViewFragment
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.PAYMENT_TYPE_NATURAL_PERSON
import com.yandex.mobile.realty.test.services.RENT_ROLE_OWNER
import com.yandex.mobile.realty.test.services.registerNaturalPersonServicesInfo
import com.yandex.mobile.realty.test.services.registerOwnerServicesInfo
import com.yandex.mobile.realty.test.services.registerServicesInfo
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.PaymentMethodType

/**
 * @author misha-kozlov on 2020-03-12
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class PaymentCardsTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = PaymentCardsActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        authorizationRule,
        activityTestRule
    )

    @Before
    fun setUp() {
        authorizationRule.setUserAuthorized()
    }

    @Test
    fun shouldShowEmptyScreen() {
        configureWebServer {
            registerNaturalPersonServicesInfo()
            registerEmptyPaymentCards()
        }

        activityTestRule.launchActivity()

        onScreen<PaymentCardsScreen> {
            waitUntil { listView.contains(addPaymentCardButton) }
            listView.contains(titleItem(getResourceString(R.string.payment_cards_extended)))
            editButton.doesNotExist()
        }
    }

    @Test
    fun shouldShowScreenWithCards() {
        configureWebServer {
            registerNaturalPersonServicesInfo()
            registerFirstAndSecondPaymentCards()
        }

        activityTestRule.launchActivity()

        onScreen<PaymentCardsScreen> {
            waitUntil {
                listView.contains(titleItem(getResourceString(R.string.payment_cards_extended)))
            }
            listView.contains(cardSnippet(FIRST_CARD))
                .isViewStateMatches(getTestRelatedFilePath("preferredCard"))
            listView.contains(cardSnippet(SECOND_CARD))
                .isViewStateMatches(getTestRelatedFilePath("notPreferredCard"))
            listView.contains(addPaymentCardButton)
            editButton.isCompletelyDisplayed()
        }
    }

    @Test
    fun shouldShowRentOwnerCard() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerRentOwnerCard()
        }

        activityTestRule.launchActivity()

        onScreen<PaymentCardsScreen> {
            waitUntil { listView.contains(cardSnippet(RENT_OWNER_CARD)) }
            listView.doesNotContain(addRentOwnerCardButton)
        }
    }

    @Test
    fun shouldShowRentOwnerCardSection() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerEmptyRentOwnerCards()
        }

        activityTestRule.launchActivity()

        onScreen<PaymentCardsScreen> {
            waitUntil {
                listView.contains(titleItem(getResourceString(R.string.rent_payout_card)))
            }
            listView.contains(addRentOwnerCardButton)
            listView.doesNotContain(titleItem(getResourceString(R.string.payment_cards_extended)))
            listView.doesNotContain(titleItem(getResourceString(R.string.other_payment_cards)))
            listView.doesNotContain(addPaymentCardButton)
        }
    }

    @Test
    fun shouldShowAllCardSections() {
        configureWebServer {
            registerServicesInfo(
                rentRole = RENT_ROLE_OWNER,
                paymentType = PAYMENT_TYPE_NATURAL_PERSON
            )
            registerEmptyRentOwnerCards()
            registerEmptyPaymentCards()
        }

        activityTestRule.launchActivity()

        onScreen<PaymentCardsScreen> {
            waitUntil {
                listView.contains(titleItem(getResourceString(R.string.rent_payout_card)))
            }
            listView.contains(addRentOwnerCardButton)
            listView.contains(titleItem(getResourceString(R.string.other_payment_cards)))
            listView.contains(addPaymentCardButton)
        }
    }

    @Test
    fun shouldShowEmptyScreenWhenNoAvailableCardTypes() {
        configureWebServer {
            registerServicesInfo()
        }

        activityTestRule.launchActivity()

        onScreen<PaymentCardsScreen> {
            waitUntil { contentView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("content"))
        }
    }

    @Test
    fun shouldRemoveAllCards() {
        configureWebServer {
            registerServicesInfo(
                rentRole = RENT_ROLE_OWNER,
                paymentType = PAYMENT_TYPE_NATURAL_PERSON
            )
            registerFirstAndSecondPaymentCards()
            registerRentOwnerCard()
            registerRentOwnerCardUnbind()

            registerServicesInfo(
                rentRole = RENT_ROLE_OWNER,
                paymentType = PAYMENT_TYPE_NATURAL_PERSON
            )
            registerFirstAndSecondPaymentCards()
            registerEmptyRentOwnerCards()
            registerSecondCardUnbind()

            registerServicesInfo(
                rentRole = RENT_ROLE_OWNER,
                paymentType = PAYMENT_TYPE_NATURAL_PERSON
            )
            registerFirstPaymentCard()
            registerEmptyRentOwnerCards()
            registerFirstCardUnbind()

            registerServicesInfo(
                rentRole = RENT_ROLE_OWNER,
                paymentType = PAYMENT_TYPE_NATURAL_PERSON
            )
            registerEmptyPaymentCards()
            registerEmptyRentOwnerCards()
        }

        activityTestRule.launchActivity()

        onScreen<PaymentCardsScreen> {
            waitUntil { listView.contains(cardSnippet(FIRST_CARD)) }
            listView.contains(cardSnippet(SECOND_CARD))
            listView.contains(cardSnippet(RENT_OWNER_CARD))
            listView.contains(addPaymentCardButton).isEnabled()

            editButton.click()
            waitUntil { doneButton.isCompletelyDisplayed() }
            editButton.doesNotExist()
            listView.contains(addPaymentCardButton).isNotEnabled()

            listView.scrollTo(cardSnippet(RENT_OWNER_CARD))
                .invoke { removeButton.click() }

            onScreen<ConfirmationDialogScreen> {
                root.isViewStateMatches(getTestRelatedFilePath("removeConfirmationDialog"))
                confirmButton.click()
            }

            listView.waitUntil {
                doesNotContain(cardSnippet(RENT_OWNER_CARD))
                contains(addRentOwnerCardButton).isNotEnabled()
                contains(cardSnippet(FIRST_CARD))
                contains(cardSnippet(SECOND_CARD))
                contains(addPaymentCardButton).isNotEnabled()
            }

            listView.scrollTo(cardSnippet(SECOND_CARD))
                .isViewStateMatches(getTestRelatedFilePath("cardInEditMode"))
                .invoke { removeButton.click() }

            onScreen<ConfirmationDialogScreen> {
                root.isViewStateMatches(getTestRelatedFilePath("removeConfirmationDialog"))
                confirmButton.click()
            }

            listView.waitUntil {
                doesNotContain(cardSnippet(RENT_OWNER_CARD))
                contains(addRentOwnerCardButton).isNotEnabled()
                contains(cardSnippet(FIRST_CARD))
                doesNotContain(cardSnippet(SECOND_CARD))
                contains(addPaymentCardButton).isNotEnabled()
            }
            listView.scrollTo(cardSnippet(FIRST_CARD))
                .invoke { removeButton.click() }

            onScreen<ConfirmationDialogScreen> {
                root.isViewStateMatches(getTestRelatedFilePath("removeConfirmationDialog"))
                confirmButton.click()
            }

            listView.waitUntil {
                doesNotContain(cardSnippet(RENT_OWNER_CARD))
                contains(addRentOwnerCardButton).isEnabled()
                doesNotContain(cardSnippet(FIRST_CARD))
                doesNotContain(cardSnippet(SECOND_CARD))
                contains(addPaymentCardButton).isEnabled()
            }
            editButton.doesNotExist()
            doneButton.doesNotExist()
        }
    }

    @Test
    fun shouldRemoveSinglePaymentCard() {
        configureWebServer {
            registerNaturalPersonServicesInfo()
            registerFirstPaymentCard()
            registerFirstCardUnbind()

            registerNaturalPersonServicesInfo()
            registerEmptyPaymentCards()
        }

        activityTestRule.launchActivity()

        onScreen<PaymentCardsScreen> {
            waitUntil { listView.contains(cardSnippet(FIRST_CARD)) }
            editButton.click()
            waitUntil { doneButton.isCompletelyDisplayed() }
            listView.contains(addPaymentCardButton).isNotEnabled()

            listView.scrollTo(cardSnippet(FIRST_CARD))
                .invoke { removeButton.click() }

            onScreen<ConfirmationDialogScreen> {
                confirmButton.click()
            }

            waitUntil { listView.doesNotContain(cardSnippet(FIRST_CARD)) }
            listView.contains(addPaymentCardButton).isEnabled()
            editButton.doesNotExist()
            doneButton.doesNotExist()
        }
    }

    @Test
    fun shouldRemoveSingleRentOwnerCard() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerRentOwnerCard()
            registerRentOwnerCardUnbind()

            registerOwnerServicesInfo()
            registerEmptyRentOwnerCards()
        }

        activityTestRule.launchActivity()

        onScreen<PaymentCardsScreen> {
            waitUntil { listView.contains(cardSnippet(RENT_OWNER_CARD)) }
            editButton.click()
            waitUntil { doneButton.isCompletelyDisplayed() }
            listView.doesNotContain(addRentOwnerCardButton)

            listView.scrollTo(cardSnippet(RENT_OWNER_CARD))
                .invoke { removeButton.click() }

            onScreen<ConfirmationDialogScreen> {
                confirmButton.click()
            }

            waitUntil { listView.doesNotContain(cardSnippet(RENT_OWNER_CARD)) }
            listView.contains(addRentOwnerCardButton).isEnabled()
            editButton.doesNotExist()
            doneButton.doesNotExist()
        }
    }

    @Test
    fun shouldSetPreferredCard() {
        configureWebServer {
            registerNaturalPersonServicesInfo()
            registerFirstAndSecondPaymentCards()
            registerNaturalPersonServicesInfo()
            registerSecondCardPreferred()
        }

        activityTestRule.launchActivity()

        onScreen<PaymentCardsScreen> {
            cardSnippet(FIRST_CARD).waitUntil { listView.contains(this) }
                .invoke { preferredView.isCompletelyDisplayed() }
            listView.contains(cardSnippet(SECOND_CARD))
                .invoke { preferredView.isHidden() }
            cardSnippet(SECOND_CARD).view.click()

            listView.waitUntil {
                contains(cardSnippet(SECOND_CARD))
                    .preferredView.isCompletelyDisplayed()
            }
            cardSnippet(FIRST_CARD).view.preferredView.isHidden()
        }
    }

    @Test
    fun shouldAddCardWithoutDefaultEmail() {
        configureWebServer {
            registerNaturalPersonServicesInfo()
            registerEmptyPaymentCards()
            registerEmptyDefaultEmail()
            registerBindingRequest()
            registerBindingStatus()
            registerNaturalPersonServicesInfo()
            registerFirstPaymentCard()
        }

        activityTestRule.launchActivity()
        registerTokenization()

        onScreen<PaymentCardsScreen> {
            waitUntil { listView.contains(addPaymentCardButton) }
            listView.doesNotContain(cardSnippet(FIRST_CARD))

            listView.scrollTo(addPaymentCardButton).click()

            onScreen<EmailInputScreen> {
                emailInput
                    .waitUntil { isCompletelyDisplayed() }
                    .typeText("test@test.ru")
                proceedButton.click()
            }

            waitUntil { listView.contains(cardSnippet(FIRST_CARD)) }
        }
    }

    @Test
    fun shouldAddCardWithDefaultEmail() {
        configureWebServer {
            registerNaturalPersonServicesInfo()
            registerEmptyPaymentCards()
            registerDefaultEmail()
            registerBindingRequest()
            registerBindingStatus()
            registerNaturalPersonServicesInfo()
            registerFirstPaymentCard()
        }

        activityTestRule.launchActivity()
        registerTokenization()

        onScreen<PaymentCardsScreen> {
            waitUntil { listView.contains(addPaymentCardButton) }
            listView.doesNotContain(cardSnippet(FIRST_CARD))

            listView.scrollTo(addPaymentCardButton).click()

            waitUntil { listView.contains(cardSnippet(FIRST_CARD)) }
        }
    }

    @Test
    fun shouldAddRentOwnerCard() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerEmptyRentOwnerCards()
            registerRentOwnerBindingRequest()
            registerRentOwnerUpdateCardsStatus()
            registerOwnerServicesInfo()
            registerRentOwnerCard()
        }

        activityTestRule.launchActivity()
        registerResultOkIntent(matchesRentOwnerBindingIntent(), rentOwnerSuccessResult())

        onScreen<PaymentCardsScreen> {
            addRentOwnerCardButton.waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentOwnerCardBindingScreen> {
            intended(matchesRentOwnerBindingIntent())
            waitUntil { listView.contains(resultItem) }
            root.isViewStateMatches(getTestRelatedFilePath("successContent"))
            resultActionButton.click()
        }

        onScreen<PaymentCardsScreen> {
            waitUntil { listView.contains(cardSnippet(RENT_OWNER_CARD)) }
        }
    }

    @Test
    fun shouldAddRentOwnerCardWhenErrors() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerEmptyRentOwnerCards()
            registerRentOwnerBindingRequestError()
            registerRentOwnerBindingRequest()
            registerRentOwnerBindingRequest()
            registerRentOwnerUpdateCardsStatusError()
        }

        activityTestRule.launchActivity()

        onScreen<PaymentCardsScreen> {
            addRentOwnerCardButton.waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentOwnerCardBindingScreen> {
            waitUntil { listView.contains(errorItem) }
            root.isViewStateMatches(getTestRelatedFilePath("errorContent"))
            registerResultOkIntent(matchesRentOwnerBindingIntent(), rentOwnerFailedResult())
            errorButton.click()

            intended(matchesRentOwnerBindingIntent())
            registerResultOkIntent(matchesRentOwnerBindingIntent(), rentOwnerSuccessResult())
            waitUntil { listView.contains(errorItem) }
            root.isViewStateMatches(getTestRelatedFilePath("errorContent"))
            errorButton.click()

            intended(matchesRentOwnerBindingIntent(), 2)
            waitUntil { listView.contains(timedOutItem) }
            root.isViewStateMatches(getTestRelatedFilePath("timedOutContent"))
            timedOutCloseButton.click()
        }

        onScreen<PaymentCardsScreen> {
            waitUntil { contentView.isCompletelyDisplayed() }
        }
    }

    private fun registerTokenization() {
        val resultData = Intent().apply {
            putExtra(EXTRA_PAYMENT_TOKEN, "test")
            putExtra(EXTRA_PAYMENT_METHOD_TYPE, PaymentMethodType.BANK_CARD)
        }
        val result = ActivityResult(Activity.RESULT_OK, resultData)
        intending(hasComponent("ru.yoomoney.sdk.kassa.payments.ui.CheckoutActivity"))
            .respondWith(result)
    }

    private fun matchesRentOwnerBindingIntent(): Matcher<Intent> {
        return NamedIntentMatcher(
            "Запуск веб-формы привязки карты",
            hasExtra("fragment_class", RedirectWebViewFragment::class.java)
        )
    }

    private fun rentOwnerSuccessResult(): Intent {
        return Intent().apply {
            putExtra(
                "resultRedirectUrl",
                "https://arenda.realty.yandex.ru/?tinkoffCardBindingStatus=success"
            )
        }
    }

    private fun rentOwnerFailedResult(): Intent {
        return Intent().apply {
            putExtra(
                "resultRedirectUrl",
                "https://arenda.realty.yandex.ru/?tinkoffCardBindingStatus=failed"
            )
        }
    }

    private fun DispatcherRegistry.registerEmptyRentOwnerCards() {
        register(
            request {
                path("2.0/rent/user/me/cards/owner")
            },
            response {
                setBody(
                    """{
                                "response": {
                                    "cards": []
                                }
                            }
                    """.trimIndent()
                )
            }
        )
    }

    private fun DispatcherRegistry.registerRentOwnerCard() {
        register(
            request {
                path("2.0/rent/user/me/cards/owner")
            },
            response {
                setBody(
                    """{
                                "response": {
                                    "cards": [
                                        {
                                            "cardId": "85757574",
                                            "pan": "500000******0447",
                                            "expDate": "1122",
                                            "status": "ACTIVE",
                                            "brand": "MASTERCARD"
                                        }
                                    ]
                                }
                            }
                    """.trimIndent()
                )
            }
        )
    }

    private fun DispatcherRegistry.registerRentOwnerCardUnbind() {
        register(
            request {
                path("2.0/rent/user/me/cards/owner/85757574")
                method("DELETE")
            },
            success()
        )
    }

    private fun DispatcherRegistry.registerEmptyPaymentCards() {
        register(
            request {
                path("2.0/banker/user/me/cards")
            },
            response {
                setBody(
                    """{
                                "response": {
                                    "methods": []
                                } 
                            }"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerFirstPaymentCard() {
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
                                                "brand": "MASTERCARD",
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

    private fun DispatcherRegistry.registerFirstAndSecondPaymentCards() {
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
                                                "brand": "MASTERCARD",
                                                "expireYear": "2022",
                                                "expireMonth": "2"
                                            }
                                        },
                                        "needEmail": false,
                                        "preferred": true
                                    }, {
                                        "psId": "YANDEXKASSA_V3",
                                        "id": "bank_card",
                                        "name": "Банковская карта",
                                        "properties": {
                                            "card": {
                                                "cddPanMask": "555555|4477",
                                                "brand": "MASTERCARD",
                                                "expireYear": "2022",
                                                "expireMonth": "2"
                                            }
                                        },
                                        "needEmail": false,
                                        "preferred": false
                                    }]
                                } 
                            }"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerFirstCardUnbind() {
        register(
            request {
                path("2.0/banker/user/me/card/555555%7C4444/gate/YANDEXKASSA_V3/unbind")
            },
            response {
                setBody("""{"response": {}}""")
            }
        )
    }

    private fun DispatcherRegistry.registerSecondCardUnbind() {
        register(
            request {
                path("2.0/banker/user/me/card/555555%7C4477/gate/YANDEXKASSA_V3/unbind")
            },
            response {
                setBody("""{"response": {}}""")
            }
        )
    }

    private fun DispatcherRegistry.registerSecondCardPreferred() {
        register(
            request {
                path("2.0/banker/user/me/card/555555%7C4477/gate/YANDEXKASSA_V3/preferred")
            },
            response {
                setBody("""{"response": {}}""")
            }
        )
    }

    private fun DispatcherRegistry.registerDefaultEmail() {
        register(
            request {
                path("2.0/banker/user/me/email")
            },
            response {
                setBody("""{"response": {"email": "test@test.ru"}}""")
            }
        )
    }

    private fun DispatcherRegistry.registerEmptyDefaultEmail() {
        register(
            request {
                path("2.0/banker/user/me/email")
            },
            response {
                setBody("""{"response": {}}""")
            }
        )
    }

    private fun DispatcherRegistry.registerBindingRequest() {
        register(
            request {
                path("2.0/banker/user/me/card/bind/requestV2")
            },
            response {
                setBody(
                    """{
                                "response": {
                                    "noConfirmation": {},
                                    "paymentRequestId": "111"
                                }
                            }"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerBindingStatus() {
        register(
            request {
                path("2.0/banker/user/me/card/bind/status/111")
            },
            response {
                setBody("""{"response": {"status": "BOUND"}}""")
            }
        )
    }

    private fun DispatcherRegistry.registerRentOwnerBindingRequest() {
        register(
            request {
                path("2.0/rent/user/me/cards/owner")
                method("POST")
            },
            response {
                setBody("""{"response": {"paymentUrl": "https://binding"}}""")
            }
        )
    }

    private fun DispatcherRegistry.registerRentOwnerBindingRequestError() {
        register(
            request {
                path("2.0/rent/user/me/cards/owner")
                method("POST")
            },
            response {
                error()
            }
        )
    }

    private fun DispatcherRegistry.registerRentOwnerUpdateCardsStatus() {
        register(
            request {
                path("2.0/rent/user/me/cards/owner/update-status")
                method("POST")
            },
            response {
                success()
            }
        )
    }

    private fun DispatcherRegistry.registerRentOwnerUpdateCardsStatusError() {
        register(
            request {
                path("2.0/rent/user/me/cards/owner/update-status")
                method("POST")
            },
            error()
        )
    }

    companion object {
        private const val FIRST_CARD = "MasterCard ***4444"
        private const val SECOND_CARD = "MasterCard ***4477"
        private const val RENT_OWNER_CARD = "MasterCard ***0447"

        private const val EXTRA_PAYMENT_TOKEN = "ru.yoomoney.sdk.kassa.payments.extra.PAYMENT_TOKEN"
        private const val EXTRA_PAYMENT_METHOD_TYPE =
            "ru.yoomoney.sdk.kassa.payments.extra.PAYMENT_METHOD_TYPE"
    }
}
