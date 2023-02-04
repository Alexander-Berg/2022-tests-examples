package com.yandex.mobile.realty.test.services

import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.intent.Intents
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonObject
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.RealtyTestApplication
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.PushBroadcastCommand
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.metrica.event
import com.yandex.mobile.realty.core.pressBack
import com.yandex.mobile.realty.core.registerResultCanceledIntent
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.*
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.yandexrent.matchesAcquiringIntent
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.responses.GetStateResponse

/**
 * @author andrey-bgm on 15/09/2021.
 */
@LargeTest
class YandexRentTenantRentPaymentTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = RentFlatActivityTestRule(
        flatId = FLAT_ID,
        launchActivity = false
    )
    private val appStateRule = SetupDefaultAppStateRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        appStateRule,
        MetricaEventsRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun performSuccessPaymentWhenNextPaymentCreated() {
        configureWebServer {
            registerFirstPaymentNotification()

            registerPayment(newPayment())
            registerFlatWithPayment(newPayment())
            registerInitPayment()

            registerPayment(paidPayment())
            registerFlatWithPayment(futurePayment())
            registerTenantRentPaidNotification(FUTURE_PAYMENT_ID)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_first_payment_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<YandexRentPaymentScreen> {
            waitUntil {
                paymentView.isCompletelyDisplayed()
                titleView.isTextEquals(INITIAL_TITLE)
            }

            root.isViewStateMatches("YandexRentTenantRentPaymentTest/paymentScreenInitial")
            paymentMethodView(getResourceString(R.string.payment_method_new_card)).click()
            registerResultOkIntent(matchesAcquiringIntent(), null)
            payButton.click()

            val resultTitleRes = R.string.payment_passed
            waitUntil { listView.contains(resultItem(resultTitleRes)) }
            root.isViewStateMatches(getTestRelatedFilePath("paymentScreenSuccess"))
            resultActionButton(resultTitleRes).click()
        }

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_paid_title)
            waitUntil { listView.contains(notificationItem(title)) }
        }
    }

    @Test
    fun performSuccessPaymentWhenCurrentPaymentPaid() {
        configureWebServer {
            registerFirstPaymentNotification()

            registerPayment(newPayment())
            registerFlatWithPayment(newPayment())
            registerInitPayment()

            registerPayment(paidPayment())
            registerFlatWithPayment(paidPayment())
            registerTenantRentPaidNotification(PAYMENT_ID)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_first_payment_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<YandexRentPaymentScreen> {
            waitUntil {
                paymentView.isCompletelyDisplayed()
                titleView.isTextEquals(INITIAL_TITLE)
            }

            root.isViewStateMatches("YandexRentTenantRentPaymentTest/paymentScreenInitial")
            paymentMethodView(getResourceString(R.string.payment_method_new_card)).click()
            registerResultOkIntent(matchesAcquiringIntent(), null)
            payButton.click()

            val resultTitleRes = R.string.payment_passed
            waitUntil { listView.contains(resultItem(resultTitleRes)) }
            root.isViewStateMatches(getTestRelatedFilePath("paymentScreenSuccess"))
            resultActionButton(resultTitleRes).click()
        }

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_paid_title)
            waitUntil { listView.contains(notificationItem(title)) }
        }
    }

    @Test
    fun openPaymentWhenPaidOutUnderGuarantee() {
        configureWebServer {
            registerFirstPaymentNotification()

            registerPayment(paidOutUnderGuaranteePayment())
            registerFlatWithPayment(paidOutUnderGuaranteePayment())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_first_payment_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<YandexRentPaymentScreen> {
            waitUntil {
                paymentView.isCompletelyDisplayed()
                titleView.isTextEquals(INITIAL_TITLE)
            }

            root.isViewStateMatches("YandexRentTenantRentPaymentTest/paymentScreenInitial")
        }
    }

    @Test
    fun performSuccessPaymentWhenPaymentStatusTimedOut() {
        configureWebServer {
            registerFirstPaymentNotification()
            registerPayment(newPayment())
            registerFlatWithPayment(newPayment())
            registerInitPayment()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_first_payment_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<YandexRentPaymentScreen> {
            waitUntil {
                paymentView.isCompletelyDisplayed()
                titleView.isTextEquals(INITIAL_TITLE)
            }

            root.isViewStateMatches("YandexRentTenantRentPaymentTest/paymentScreenInitial")
            paymentMethodView(getResourceString(R.string.payment_method_new_card)).click()
            registerResultOkIntent(matchesAcquiringIntent(), null)
            payButton.click()

            waitUntil { listView.contains(timedOutItem) }
            root.isViewStateMatches(getTestRelatedFilePath("paymentScreenTimedOut"))
            timedOutCloseButton.click()
        }

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_first_payment_title)
            waitUntil { listView.contains(notificationItem(title)) }
        }
    }

    @Test
    fun savePaymentMethodAsPreferredAfterPayButtonPressed() {
        configureWebServer {
            registerFirstPaymentNotification()
            registerPayment(newPayment())
            registerFlatWithPayment(newPayment())
            registerInitPayment()
            registerPayment(newPayment())
            registerFlatWithPayment(newPayment())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_first_payment_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<YandexRentPaymentScreen> {
            waitUntil {
                paymentView.isCompletelyDisplayed()
                titleView.isTextEquals(INITIAL_TITLE)
            }

            root.isViewStateMatches("YandexRentTenantRentPaymentTest/paymentScreenInitial")
            paymentMethodView(getResourceString(R.string.payment_method_new_card)).click()
            registerResultCanceledIntent(matchesAcquiringIntent())
            payButton.click()

            pressBack()
        }

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_first_payment_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<YandexRentPaymentScreen> {
            waitUntil {
                paymentView.isCompletelyDisplayed()
                titleView.isTextEquals(INITIAL_TITLE)
            }

            root.isViewStateMatches("YandexRentTenantRentPaymentTest/cardSelected")
        }
    }

    @Test
    fun performPaymentWhenErrors() {
        configureWebServer {
            registerFirstPaymentNotification()

            registerPaymentError()
            registerFlatWithPayment(newPayment())

            registerPayment(newPayment())
            registerFlatError()

            registerPayment(newPayment())
            registerFlatWithPayment(newPayment())
            registerInitPaymentError()

            registerInitPaymentError()

            registerInitPayment()
            registerPaymentError()
            registerFlatWithPayment(newPayment())

            registerPayment(newPayment())
            registerFlatWithPayment(newPayment())

            registerInitPayment()

            registerPayment(newPayment())
            registerFlatWithPayment(newPayment())
            registerInitPayment()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_first_payment_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<YandexRentPaymentScreen> {
            waitUntil { paymentView.isCompletelyDisplayed() }

            errorItem
                .waitUntil { listView.contains(this) }
                .also {
                    root.isViewStateMatches(getTestRelatedFilePath("paymentScreenLoadError"))
                }
                .click()

            errorItem
                .waitUntil { listView.contains(this) }
                .also {
                    root.isViewStateMatches(getTestRelatedFilePath("paymentScreenLoadError"))
                }
                .click()

            waitUntil { titleView.isTextEquals(INITIAL_TITLE) }
            root.isViewStateMatches("YandexRentTenantRentPaymentTest/paymentScreenInitial")
            payButton.click()

            waitUntil {
                toastView(getResourceString(R.string.error_try_again))
                    .isCompletelyDisplayed()
            }
            paymentMethodView(getResourceString(R.string.payment_method_new_card)).click()
            payButton.click()

            waitUntil {
                toastView(getResourceString(R.string.error_try_again))
                    .isCompletelyDisplayed()
            }
            registerResultAcquiringErrorIntent(
                createAcquiringError(
                    errorCode = AcquiringApi.errorCodesForUserShowing.first(),
                    message = "Недостаточно средств"
                )
            )
            payButton.click()

            waitUntil { listView.contains(acquiringErrorItem) }
            root.isViewStateMatches(getTestRelatedFilePath("paymentScreenNotEnoughFundsError"))
            acquiringErrorActionButton.click()

            errorItem
                .waitUntil { listView.contains(this) }
                .also {
                    root.isViewStateMatches(getTestRelatedFilePath("paymentScreenLoadError"))
                }
                .click()

            waitUntil { titleView.isTextEquals(INITIAL_TITLE) }
            root.isViewStateMatches("YandexRentTenantRentPaymentTest/cardSelected")
            registerResultAcquiringErrorIntent(
                createAcquiringError(
                    errorCode = "999999",
                    message = "Ошибка на сервере"
                )
            )
            payButton.click()

            acquiringErrorItem.waitUntil { listView.contains(this) }
            root.isViewStateMatches(getTestRelatedFilePath("paymentScreenAcquiringCommonError"))
            acquiringErrorActionButton.click()

            waitUntil { titleView.isTextEquals(INITIAL_TITLE) }
            root.isViewStateMatches("YandexRentTenantRentPaymentTest/cardSelected")
        }
    }

    @Test
    fun openYandexRentTermsOfUse() {
        val termsUrl = "https://yandex.ru/legal/lease_termsofuse/020222/?only-content=true"
        configureWebServer {
            registerFirstPaymentNotification()
            registerPayment(newPayment(), termsUrl)
            registerFlatWithPayment(newPayment())
        }

        val showMetricaEvent = event("Аренда. Экран оплаты. Показ блока с условиями")
        val tapLinkMetricaEvent = event("Аренда. Экран оплаты. Нажатие на ссылку с условиями")

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_first_payment_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<YandexRentPaymentScreen> {
            waitUntil {
                paymentView.isCompletelyDisplayed()
                titleView.isTextEquals(INITIAL_TITLE)
            }
            waitUntil { showMetricaEvent.isOccurred() }
            root.isViewStateMatches(getTestRelatedFilePath("paymentScreenTermsOfUse"))

            termsOfUseView.tapOnLinkText("Условия оказания услуг")
        }

        onScreen<WebViewScreen> {
            waitUntil { tapLinkMetricaEvent.isOccurred() }
            waitUntil { webView.isPageUrlEquals(termsUrl) }
        }
    }

    @Test
    fun openPaymentScreenFromPushNotification() {
        configureWebServer {
            registerFirstPaymentNotification()
            registerPayment(newPayment())
            registerFlatWithPayment(newPayment())
        }

        authorizationRule.setUserAuthorized()
        sendPush(CURRENT_USER_UID)

        onScreen<NotificationScreen> {
            hasNotification(PUSH_TITLE, PUSH_BODY)
            clickOnNotification()
        }

        onScreen<YandexRentPaymentScreen> {
            waitUntil {
                paymentView.isCompletelyDisplayed()
                titleView.isTextEquals(INITIAL_TITLE)
            }

            root.isViewStateMatches("YandexRentTenantRentPaymentTest/paymentScreenInitial")
            pressBack()
        }

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_first_payment_title)
            waitUntil { listView.contains(notificationItem(title)) }
            pressBack()
        }

        onScreen<ServicesScreen> {
            servicesTitleItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun openPaymentScreenFromFirstPaymentPushNotification() {
        configureWebServer {
            registerPayment(newPayment())
            registerFlatWithPayment(newPayment())
        }

        authorizationRule.setUserAuthorized()
        sendFirstPaymentPush()

        onScreen<NotificationScreen> {
            hasNotification(PUSH_TITLE, PUSH_BODY)
            clickOnNotification()
        }

        onScreen<YandexRentPaymentScreen> {
            waitUntil {
                paymentView.isCompletelyDisplayed()
                titleView.isTextEquals(INITIAL_TITLE)
            }

            root.isViewStateMatches("YandexRentTenantRentPaymentTest/paymentScreenInitial")
            pressBack()
        }

        onScreen<RentShowingsScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<ServicesScreen> {
            servicesTitleItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun showSuccessPaymentResultFromPushNotificationWhenAlreadyPaid() {
        configureWebServer {
            registerTenantRentPaidNotification(FUTURE_PAYMENT_ID)
            registerPayment(paidPayment())
            registerFlatWithPayment(futurePayment())
        }

        authorizationRule.setUserAuthorized()
        sendPush(CURRENT_USER_UID)

        onScreen<NotificationScreen> {
            hasNotification(PUSH_TITLE, PUSH_BODY)
            clickOnNotification()
        }

        onScreen<YandexRentPaymentScreen> {
            waitUntil { paymentView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("paymentScreenSuccess"))
            pressBack()
        }

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_paid_title)
            waitUntil { listView.contains(notificationItem(title)) }
        }
    }

    @Test
    fun shouldNotShowPushNotificationWhenUidDoesNotMatch() {
        authorizationRule.setUserAuthorized()
        sendPush(ANOTHER_USER_UID)

        onScreen<NotificationScreen> {
            hasNoNotifications()
        }

        pressBack()
    }

    @Test
    fun showSbpPromo() {
        configureWebServer {
            registerFirstPaymentNotification()
            registerPayment(newPayment())
            registerFlatWithPayment(newPayment())
            registerPayment(newPayment())
            registerFlatWithPayment(newPayment())
        }

        appStateRule.setState {
            sbpPromoShown.set(false)
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_first_payment_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<SbpPromoScreen> {
            waitUntil { promoView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("sbpPromo"))
            primaryButton.click()
        }

        onScreen<YandexRentPaymentScreen> {
            waitUntil {
                paymentView.isCompletelyDisplayed()
                titleView.isTextEquals(INITIAL_TITLE)
            }
            pressBack()
        }

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_first_payment_title)
            listView.contains(notificationItem(title))
                .invoke { actionButton.click() }
        }

        onScreen<YandexRentPaymentScreen> {
            waitUntil { paymentView.isCompletelyDisplayed() }
        }
    }

    private fun registerResultAcquiringErrorIntent(error: Throwable) {
        val resultData = Intent().apply {
            putExtra(TinkoffAcquiring.EXTRA_ERROR, error)
        }
        val result = Instrumentation.ActivityResult(TinkoffAcquiring.RESULT_ERROR, resultData)

        Intents.intending(matchesAcquiringIntent())
            .respondWith(result)
    }

    private fun createAcquiringError(
        errorCode: String,
        message: String
    ): Throwable {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as RealtyTestApplication
        val gson = application.component.gson
        val json = """
                   {
                       "ErrorCode": "$errorCode",
                       "Message": "$message"
                   }
        """.trimIndent()
        val response = gson.fromJson(json, GetStateResponse::class.java)

        return AcquiringApiException(response)
    }

    private fun sendPush(recipientId: String) {
        PushBroadcastCommand.sendPush(
            action = "tenant_rent_payment",
            params = """
                         {
                             "push_id": "TENANT_RENT_PAYMENT_TODAY",
                             "recipient_id": $recipientId,
                             "title": "$PUSH_TITLE",
                             "body": "$PUSH_BODY",
                             "flat_id": "$FLAT_ID",
                             "payment_id": "$PAYMENT_ID"
                         }
            """.trimIndent(),
            name = "Пора платить за квартиру"
        )
    }

    private fun sendFirstPaymentPush() {
        PushBroadcastCommand.sendPush(
            action = "tenant_first_rent_payment",
            params = """
                         {
                             "push_id": "TENANT_PAY_TO_ACTIVATE_RENT_CONTRACT",
                             "recipient_id": $CURRENT_USER_UID,
                             "title": "$PUSH_TITLE",
                             "body": "$PUSH_BODY",
                             "flat_id": "$FLAT_ID",
                             "payment_id": "$PAYMENT_ID"
                         }
            """.trimIndent(),
            name = "Оплатите первый месяц"
        )
    }

    private fun newPayment(): JsonObject {
        return currentPayment("NEW")
    }

    private fun paidPayment(): JsonObject {
        return currentPayment("PAID_BY_TENANT")
    }

    private fun paidOutUnderGuaranteePayment(): JsonObject {
        return currentPayment("PAID_OUT_UNDER_GUARANTEE")
    }

    private fun currentPayment(status: String): JsonObject {
        return jsonObject {
            "id" to PAYMENT_ID
            "status" to status
            "tenantRentPayment" to jsonObject {
                "startDate" to "2021-09-15"
                "endDate" to "2021-10-14"
                "paymentDate" to "2021-09-15"
            }
            "tenantSpecificPaymentInfo" to jsonObject {
                "amount" to "4200000"
            }
        }
    }

    private fun futurePayment(): JsonObject {
        return jsonObject {
            "id" to FUTURE_PAYMENT_ID
            "status" to "FUTURE_PAYMENT"
            "tenantRentPayment" to jsonObject {
                "startDate" to "2021-10-15"
                "endDate" to "2021-11-14"
                "paymentDate" to "2021-10-15"
            }
            "tenantSpecificPaymentInfo" to jsonObject {
                "amount" to "4200000"
            }
        }
    }

    private fun DispatcherRegistry.registerPayment(payment: JsonObject, termsUrl: String? = null) {
        register(
            request {
                method("GET")
                path("2.0/rent/user/me/flats/$FLAT_ID/payments/$PAYMENT_ID")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "payment" to payment
                        if (termsUrl != null) {
                            "termsForAcceptance" to jsonObject {
                                "contractTermsUrl" to termsUrl
                            }
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerPaymentError() {
        register(
            request {
                method("GET")
                path("2.0/rent/user/me/flats/$FLAT_ID/payments/$PAYMENT_ID")
            },
            error()
        )
    }

    private fun DispatcherRegistry.registerFlatWithPayment(payment: JsonObject) {
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
                            "actualPayment" to payment
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerFlatError() {
        register(
            request {
                path("2.0/rent/user/me/flats/$FLAT_ID")
            },
            error()
        )
    }

    private fun DispatcherRegistry.registerFirstPaymentNotification() {
        registerTenantRentFlat(
            actualPayment = jsonObject {
                "id" to PAYMENT_ID
            },
            notification = jsonObject {
                "tenantRentFirstPayment" to jsonObject {}
            }
        )
    }

    private fun DispatcherRegistry.registerTenantRentPaidNotification(paymentId: String) {
        registerTenantRentFlat(
            actualPayment = jsonObject {
                "id" to paymentId
            },
            notification = jsonObject {
                "tenantRentPaid" to jsonObject {
                    "paidToDate" to "2021-10-14"
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
                            "amount" to "4200000"
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerInitPaymentError() {
        register(
            request {
                method("POST")
                path("2.0/rent/user/me/flats/$FLAT_ID/payments/$PAYMENT_ID/init")
            },
            error()
        )
    }

    private companion object {

        const val PAYMENT_ID = "paymentId00001"
        const val FUTURE_PAYMENT_ID = "paymentId00002"
        const val INITIAL_TITLE = "15 сент. - 14 окт."
        const val PUSH_TITLE = "Пора платить за квартиру"
        const val PUSH_BODY = "Не забудьте внести деньги до конца дня"
        const val CURRENT_USER_UID = "1"
        const val ANOTHER_USER_UID = "2"
    }
}
