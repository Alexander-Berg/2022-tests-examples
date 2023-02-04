package com.yandex.mobile.realty.test.yandexrent

import android.content.Intent
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.filters.LargeTest
import com.google.gson.JsonObject
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.*
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.viewMatchers.NamedIntentMatcher
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.FLAT_ADDRESS
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.test.services.registerTenantRentFlat
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author misha-kozlov on 10.01.2022
 */
@LargeTest
class TenantUtilitiesBillTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = RentFlatActivityTestRule(
        flatId = FLAT_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
    )

    @Test
    fun shouldPayUtilitiesBill() {
        configureWebServer {
            registerTenantRentFlat(notification = billNotification())
            registerUtilitiesPayment()
            registerPeriodWithInfo(billStatus = "SHOULD_BE_PAID")
            registerPayment(payment = payment("NEW"))
            registerFlat()
            registerInitPayment()
            registerPayment(payment = payment("PAID_BY_TENANT"))
            registerFlat()
            registerTenantRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(BILL_NOTIFICATION_TITLE)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }

        onScreen<RentUtilitiesBillInfoScreen> {
            waitUntil { payButton.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("billInfo"))
            payButton.click()
        }

        onScreen<YandexRentPaymentScreen> {
            waitUntil {
                paymentView.isCompletelyDisplayed()
                titleView.isTextEquals(PAYMENT_INITIAL_TITLE)
            }
            root.isViewStateMatches(getTestRelatedFilePath("paymentInitial"))

            paymentMethodView(getResourceString(R.string.payment_method_new_card)).click()
            registerResultOkIntent(matchesAcquiringIntent(), null)
            payButton.click()

            val resultTitleRes = R.string.yandex_rent_utilities_payment_success_title
            waitUntil { listView.contains(resultItem(resultTitleRes)) }
            root.isViewStateMatches(getTestRelatedFilePath("paymentScreenSuccess"))
            resultActionButton(resultTitleRes).click()
        }

        checkNotificationDoesNotExits()
    }

    @Test
    fun shouldDeclineUtilitiesBill() {
        configureWebServer {
            registerTenantRentFlat(notification = billNotification())
            registerUtilitiesPayment()
            registerPeriodWithInfo(billStatus = "SHOULD_BE_PAID")
            registerDeclineBill()
            registerTenantRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openBillInfoNotification()

        onScreen<RentUtilitiesBillInfoScreen> {
            waitUntil { payButton.isCompletelyDisplayed() }
            declineButton.click()
        }

        onScreen<RentUtilitiesDeclineScreen> {
            waitUntil { messageView.isCompletelyDisplayed() }
            isViewStateMatches(getTestRelatedFilePath("empty"))

            messageView.typeText(DECLINE_REASON)
            isViewStateMatches(getTestRelatedFilePath("filled"))

            declineButton.click()

            waitUntil { successView.isCompletelyDisplayed() }
            isViewStateMatches(getTestRelatedFilePath("success"))
            successButton.click()
        }

        checkNotificationDoesNotExits()
    }

    @Test
    fun shouldShowPaidResultImmediately() {
        configureWebServer {
            registerTenantRentFlat(notification = billNotification())
            registerPeriodWithInfo(billStatus = "PAID")
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openBillInfoNotification()

        onScreen<RentUtilitiesBillInfoScreen> {
            waitUntil { billPaidView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("screen"))
        }
    }

    @Test
    fun shouldShowDeclineResultImmediately() {
        configureWebServer {
            registerTenantRentFlat(notification = billNotification())
            registerPeriodWithInfo(billStatus = "DECLINED")
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openBillInfoNotification()

        onScreen<RentUtilitiesBillInfoScreen> {
            waitUntil { billDeclinedView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("screen"))
        }
    }

    @Test
    fun shouldShowFullScreenImages() {
        configureWebServer {
            registerTenantRentFlat(notification = billNotification())
            registerUtilitiesPayment()
            registerPeriodWithInfo(billStatus = "SHOULD_BE_PAID")
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openBillInfoNotification()

        onScreen<RentUtilitiesBillInfoScreen> {
            waitUntil { payButton.isCompletelyDisplayed() }
            receiptImageView(IMAGE_URL_2).click()
        }
        onScreen<GalleryScreen> {
            waitUntil { photoView.isCompletelyDisplayed() }
            photoView.isViewStateMatches(getTestRelatedFilePath("secondImage"))
            photoView.swipeRight()
            waitUntil { photoView.isCompletelyDisplayed() }
            photoView.isViewStateMatches(getTestRelatedFilePath("firstImage"))
        }
    }

    @Test
    fun shouldShowDeclineConflictError() {
        configureWebServer {
            registerTenantRentFlat(notification = billNotification())
            registerUtilitiesPayment()
            registerPeriodWithInfo(billStatus = "SHOULD_BE_PAID")
            registerDeclineBillConflict()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openBillInfoNotification()

        onScreen<RentUtilitiesBillInfoScreen> {
            waitUntil { payButton.isCompletelyDisplayed() }
            declineButton.click()
        }

        onScreen<RentUtilitiesDeclineScreen> {
            messageView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(DECLINE_REASON)
            declineButton.click()

            toastView(CONFLICT_MESSAGE).isCompletelyDisplayed()
        }
    }

    private fun openBillInfoNotification() {
        onScreen<RentFlatScreen> {
            notificationItem(BILL_NOTIFICATION_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }
    }

    private fun checkNotificationDoesNotExits() {
        onScreen<RentFlatScreen> {
            waitUntil {
                listView.doesNotContain(notificationItem(BILL_NOTIFICATION_TITLE))
            }
        }
    }

    private fun DispatcherRegistry.registerUtilitiesPayment() {
        register(
            request {
                method("POST")
                val path = "2.0/rent/user/me/flats/$FLAT_ID/" +
                    "house-services/periods/$PERIOD_ID/bills/payments"
                path(path)
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "payment" to jsonObject {
                            "id" to PAYMENT_ID
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerPeriodWithInfo(billStatus: String) {
        register(
            request {
                path("2.0/rent/user/me/flats/$FLAT_ID/house-services/periods/$PERIOD_ID")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "period" to "2021-10"
                        "billStatus" to billStatus
                        "bill" to jsonObject {
                            "amount" to 2000
                            "comment" to "Вот столько нужно оплатить за ЖКХ"
                            "photos" to jsonArrayOf(
                                jsonObject {
                                    "namespace" to "arenda"
                                    "groupId" to "65493"
                                    "name" to "image_name"
                                    "imageUrls" to jsonArrayOf(
                                        jsonObject {
                                            "alias" to "1024x1024"
                                            "url" to IMAGE_URL_1
                                        },
                                        jsonObject {
                                            "alias" to "orig"
                                            "url" to IMAGE_URL_1
                                        }
                                    )
                                },
                                jsonObject {
                                    "namespace" to "arenda"
                                    "groupId" to "65493"
                                    "name" to "image_name_2"
                                    "imageUrls" to jsonArrayOf(
                                        jsonObject {
                                            "alias" to "1024x1024"
                                            "url" to IMAGE_URL_2
                                        },
                                        jsonObject {
                                            "alias" to "orig"
                                            "url" to IMAGE_URL_2
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerPayment(payment: JsonObject) {
        register(
            request {
                method("GET")
                path("2.0/rent/user/me/flats/$FLAT_ID/payments/$PAYMENT_ID")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "payment" to payment
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerInitPayment() {
        register(
            request {
                method("POST")
                path("2.0/rent/user/me/flats/$FLAT_ID/payments/$PAYMENT_ID/init")
                jsonBody {
                    "paymentMethod" to "CARD"
                    "shouldUpdateVersion" to true
                }
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "transactionInfo" to jsonObject {
                            "tinkoffPaymentId" to "900000026621"
                            "tinkoffOrderId" to PAYMENT_ID
                            "tinkoffCustomerKey" to "0b23491b98cf"
                            "amount" to "2000"
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerFlat() {
        register(
            request {
                method("GET")
                path("2.0/rent/user/me/flats/$FLAT_ID")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "flat" to jsonObject {
                            "flatId" to FLAT_ID
                            "address" to jsonObject {
                                "addressFromStreetToFlat" to FLAT_ADDRESS
                            }
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerDeclineBill() {
        register(
            request {
                method("PUT")
                val path = "2.0/rent/user/me/flats/$FLAT_ID/" +
                    "house-services/periods/$PERIOD_ID/bills/decline"
                path(path)
                jsonBody {
                    "reasonForDecline" to DECLINE_REASON
                }
            },
            success()
        )
    }

    private fun DispatcherRegistry.registerDeclineBillConflict() {
        register(
            request {
                method("PUT")
                val path = "2.0/rent/user/me/flats/$FLAT_ID/" +
                    "house-services/periods/$PERIOD_ID/bills/decline"
                path(path)
                jsonBody {
                    "reasonForDecline" to DECLINE_REASON
                }
            },
            response {
                setResponseCode(409)
                jsonBody {
                    "error" to jsonObject {
                        "code" to "CONFLICT"
                        "message" to CONFLICT_MESSAGE
                        "data" to jsonObject {
                            "code" to "BILL_IS_BLOCKED"
                        }
                    }
                }
            }
        )
    }

    private fun billNotification(): JsonObject {
        return jsonObject {
            "houseServiceBillsReceived" to jsonObject {
                "periodId" to PERIOD_ID
                "period" to PERIOD
            }
        }
    }

    private fun payment(status: String): JsonObject {
        return jsonObject {
            "id" to PAYMENT_ID
            "status" to status
            "tenantSpecificPaymentInfo" to jsonObject {
                "amount" to "2000"
            }
            "housingAndCommunalServices" to jsonObject {}
        }
    }

    private fun matchesAcquiringIntent(): Matcher<Intent> {
        return NamedIntentMatcher(
            "Открытие экрана оплаты через Тинькофф",
            IntentMatchers.hasComponent(
                "ru.tinkoff.acquiring.sdk.ui.activities.PaymentActivity"
            )
        )
    }

    private companion object {

        const val PAYMENT_INITIAL_TITLE = "ЖКХ и\u00A0услуги"
        const val BILL_NOTIFICATION_TITLE = "Счёт за\u00A0октябрь готов!"
        const val PERIOD = "2021-10"
        const val PERIOD_ID = "periodId00001"
        const val PAYMENT_ID = "paymentId00001"
        const val IMAGE_URL_1 = "https://localhost:8080/1/receipt.webp"
        const val IMAGE_URL_2 = "https://localhost:8080/2/meter.webp"
        const val DECLINE_REASON = "some text"
        const val CONFLICT_MESSAGE = "Отменить счёт можно через 15 минут"
    }
}
