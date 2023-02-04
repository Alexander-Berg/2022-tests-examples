package com.yandex.mobile.realty.test.yandexrent.contract

import android.content.Intent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.filters.LargeTest
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.ServicesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesExternalViewUrlIntent
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.metrica.event
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.*
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.viewMatchers.NamedIntentMatcher
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.*
import com.yandex.mobile.realty.test.yandexrent.PAYMENT_ID
import com.yandex.mobile.realty.test.yandexrent.SHOWING_ID
import com.yandex.mobile.realty.test.yandexrent.contractSummary
import com.yandex.mobile.realty.test.yandexrent.matchesAcquiringIntent
import com.yandex.mobile.realty.test.yandexrent.showingcard.Showing
import com.yandex.mobile.realty.test.yandexrent.showingcard.registerShowingDetails
import com.yandex.mobile.realty.test.yandexrent.showings.registerShowing
import com.yandex.mobile.realty.utils.jsonObject
import org.hamcrest.CoreMatchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author andrey-bgm on 17.06.2022
 */
@LargeTest
class TenantContractSigningTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = ServicesActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        MetricaEventsRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun signContractAndPayFirstMonth() {
        configureWebServer {
            registerServicesInfo(
                rentRole = RENT_ROLE_TENANT_CANDIDATE,
                showRentFlatSearch = true
            )
            registerRentFlats()
            registerShowing(widget = signContractWidget())
            registerContractSummary()
            registerSmsCodeRequest()
            registerSmsCodeSubmit()

            registerServicesInfo(
                rentRole = RENT_ROLE_TENANT_CANDIDATE,
                showRentFlatSearch = true
            )
            registerRentFlats()
            registerShowing(widget = payFirstMonthWidget())
            registerPayment(newPayment())
            registerFlatWithPayment(newPayment())
            registerInitPayment()
            registerPayment(paidPayment())
            registerFlatWithPayment(paidPayment())

            registerServicesInfo(
                rentRole = RENT_ROLE_TENANT,
                showRentFlatSearch = false
            )
            registerTenantRentFlats()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatSearchItem
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentShowingsScreen> {
            notificationItem(SIGN_CONTRACT_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<RentContractScreen> {
            waitUntil { listView.contains(faqButtonItem) }
            listView.isContentStateMatches(getTestRelatedFilePath("summary"))

            actionButton.click()

            registerResultOkIntent(matchesExternalViewUrlIntent(TERMS_URL), null)
            termsItem
                .waitUntil {
                    listView.contains(this)
                    view.errorView.isDisplayed()
                }
                .invoke {
                    isViewStateMatches(getTestRelatedFilePath("termsWithError"))
                    checkBox.click()
                    titleView.tapOnLinkText(R.string.yandex_rent_contract_terms_action_part)
                }

            intended(matchesExternalViewUrlIntent(TERMS_URL))
            actionButton.click()
        }

        onScreen<RentSmsConfirmationScreen> {
            smsCodeItem
                .waitUntil { listView.contains(this) }
                .invoke { inputView.typeText(SMS_CODE, closeKeyboard = false) }
        }

        onScreen<YandexRentPaymentScreen> {
            paymentMethodView(getResourceString(R.string.payment_method_new_card))
                .waitUntil { isCompletelyDisplayed() }
                .click()

            registerResultOkIntent(matchesAcquiringIntent(), null)
            payButton.click()

            val resultTitleRes = R.string.yandex_rent_contract_concluded_title
            waitUntil { listView.contains(resultItem(resultTitleRes)) }
            root.isViewStateMatches(getTestRelatedFilePath("successPayment"))
            resultActionButton(resultTitleRes).click()
        }

        onScreen<ServicesScreen> {
            waitUntil {
                listView.contains(rentFlatHeaderItem(FLAT_ADDRESS))
                listView.doesNotContain(rentFlatSearchItem)
            }
        }
    }

    @Test
    fun signContractThenPayFirstMonthFromShowingNotification() {
        configureWebServer {
            registerServicesInfo(
                rentRole = RENT_ROLE_TENANT_CANDIDATE,
                showRentFlatSearch = true
            )
            registerRentFlats()
            registerShowing(widget = signContractWidget())
            registerContractSummary()
            registerSmsCodeRequest()
            registerSmsCodeSubmit()

            registerServicesInfo(
                rentRole = RENT_ROLE_TENANT_CANDIDATE,
                showRentFlatSearch = true
            )
            registerRentFlats()
            registerShowing(widget = payFirstMonthWidget())
            repeat(2) {
                registerPayment(newPayment())
                registerFlatWithPayment(newPayment())
            }
            registerInitPayment()
            registerPayment(paidPayment())
            registerFlatWithPayment(paidPayment())

            registerServicesInfo(
                rentRole = RENT_ROLE_TENANT,
                showRentFlatSearch = false
            )
            registerTenantRentFlats()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatSearchItem
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentShowingsScreen> {
            notificationItem(SIGN_CONTRACT_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<RentContractScreen> {
            termsItem
                .waitUntil { listView.contains(this) }
                .invoke { checkBox.click() }
            actionButton.click()
        }

        onScreen<RentSmsConfirmationScreen> {
            smsCodeItem
                .waitUntil { listView.contains(this) }
                .invoke { inputView.typeText(SMS_CODE, closeKeyboard = false) }
        }

        onScreen<YandexRentPaymentScreen> {
            paymentMethodView(getResourceString(R.string.payment_method_new_card))
                .waitUntil { isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<RentShowingsScreen> {
            notificationItem(PAY_FIRST_MONTH_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<YandexRentPaymentScreen> {
            paymentMethodView(getResourceString(R.string.payment_method_new_card))
                .waitUntil { isCompletelyDisplayed() }
                .click()

            registerResultOkIntent(matchesAcquiringIntent(), null)
            payButton.click()

            val resultTitleRes = R.string.yandex_rent_contract_concluded_title
            waitUntil { listView.contains(resultItem(resultTitleRes)) }
            pressBack()
        }

        onScreen<ServicesScreen> {
            waitUntil {
                listView.contains(rentFlatHeaderItem(FLAT_ADDRESS))
                listView.doesNotContain(rentFlatSearchItem)
            }
        }
    }

    @Test
    fun signContractAndPayFirstMonthFromShowingCard() {
        configureWebServer {
            registerServicesInfo(
                rentRole = RENT_ROLE_TENANT_CANDIDATE,
                showRentFlatSearch = true
            )
            registerRentFlats()
            registerShowing(widget = signContractWidget())
            registerShowingDetails(
                body = Showing.simpleBody(
                    widget = Showing.widgetSignContract()
                )
            )
            registerContractSummary()
            registerSmsCodeRequest()
            registerSmsCodeSubmit()

            registerServicesInfo(
                rentRole = RENT_ROLE_TENANT_CANDIDATE,
                showRentFlatSearch = true
            )
            registerRentFlats()
            registerShowing(widget = payFirstMonthWidget())
            registerShowingDetails(
                body = Showing.simpleBody(
                    widget = Showing.widgetPayFirstMonth()
                )
            )
            registerPayment(newPayment())
            registerFlatWithPayment(newPayment())
            registerInitPayment()
            registerPayment(paidPayment())
            registerFlatWithPayment(paidPayment())

            registerServicesInfo(
                rentRole = RENT_ROLE_TENANT,
                showRentFlatSearch = false
            )
            registerTenantRentFlats()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatSearchItem
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentShowingsScreen> {
            waitUntil { listView.contains(notificationItem(SIGN_CONTRACT_TITLE)) }
            listView
                .scrollTo(showingHeaderItem(SHOWING_ID))
                .click()
        }

        onScreen<RentShowingCardScreen> {
            waitUntil { listView.contains(headerItem) }
            accentActionButton.click()
        }

        onScreen<RentContractScreen> {
            event("Аренда. ДА. Жилец. Переход к просмотру краткой версии ДА") {
                "Источник" to "Карточка показа"
            }.waitUntil { isOccurred() }

            termsItem
                .waitUntil { listView.contains(this) }
                .invoke { checkBox.click() }

            actionButton.click()

            event("Аренда. ДА. Жилец. Переход к подписанию ДА")
                .waitUntil { isOccurred() }
        }

        onScreen<RentSmsConfirmationScreen> {
            smsCodeItem
                .waitUntil { listView.contains(this) }
                .invoke { inputView.typeText(SMS_CODE, closeKeyboard = false) }

            event("Аренда. ДА. Жилец. Ввод кода из смс") {
                "Результат" to "Успех"
            }.waitUntil { isOccurred() }
        }

        onScreen<YandexRentPaymentScreen> {
            event("Аренда. Переход к оплате аренды") {
                "Источник" to "Подписание договора"
            }.waitUntil { isOccurred() }

            paymentMethodView(getResourceString(R.string.payment_method_new_card))
                .waitUntil { isCompletelyDisplayed() }
                .click()

            registerResultOkIntent(matchesAcquiringIntent())
            payButton.click()

            event("Аренда. Выбор способа оплаты аренды") {
                "Способ оплаты" to "Карта"
            }.waitUntil { isOccurred() }

            event("Аренда. Результат оплаты аренды") {
                "Результат" to "Успех"
                "Способ оплаты" to "Карта"
            }.waitUntil { isOccurred() }

            val resultTitleRes = R.string.yandex_rent_contract_concluded_title
            waitUntil { listView.contains(resultItem(resultTitleRes)) }
            resultActionButton(resultTitleRes).click()
        }

        onScreen<ServicesScreen> {
            waitUntil {
                listView.contains(rentFlatHeaderItem(FLAT_ADDRESS))
                listView.doesNotContain(rentFlatSearchItem)
            }
        }
    }

    @Test
    fun signContractThenPayFirstMonthFromShowingCard() {
        configureWebServer {
            registerServicesInfo(
                rentRole = RENT_ROLE_TENANT_CANDIDATE,
                showRentFlatSearch = true
            )
            registerRentFlats()
            registerShowing(widget = signContractWidget())
            registerShowingDetails(
                body = Showing.simpleBody(
                    widget = Showing.widgetSignContract()
                )
            )
            registerContractSummary()
            registerSmsCodeRequest()
            registerSmsCodeSubmit()

            registerServicesInfo(
                rentRole = RENT_ROLE_TENANT_CANDIDATE,
                showRentFlatSearch = true
            )
            registerRentFlats()
            registerShowing(widget = payFirstMonthWidget())
            registerShowingDetails(
                body = Showing.simpleBody(
                    widget = Showing.widgetPayFirstMonth()
                )
            )
            repeat(2) {
                registerPayment(newPayment())
                registerFlatWithPayment(newPayment())
            }
            registerInitPayment()
            registerPayment(paidPayment())
            registerFlatWithPayment(paidPayment())

            registerServicesInfo(
                rentRole = RENT_ROLE_TENANT,
                showRentFlatSearch = false
            )
            registerTenantRentFlats()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatSearchItem
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentShowingsScreen> {
            waitUntil { listView.contains(notificationItem(SIGN_CONTRACT_TITLE)) }
            listView
                .scrollTo(showingHeaderItem(SHOWING_ID))
                .click()
        }

        onScreen<RentShowingCardScreen> {
            waitUntil { listView.contains(headerItem) }
            accentActionButton.click()
        }

        onScreen<RentContractScreen> {
            event("Аренда. ДА. Жилец. Переход к просмотру краткой версии ДА") {
                "Источник" to "Карточка показа"
            }.waitUntil { isOccurred() }

            termsItem
                .waitUntil { listView.contains(this) }
                .invoke { checkBox.click() }

            actionButton.click()

            event("Аренда. ДА. Жилец. Переход к подписанию ДА")
                .waitUntil { isOccurred() }
        }

        onScreen<RentSmsConfirmationScreen> {
            smsCodeItem
                .waitUntil { listView.contains(this) }
                .invoke { inputView.typeText(SMS_CODE, closeKeyboard = false) }

            event("Аренда. ДА. Жилец. Ввод кода из смс") {
                "Результат" to "Успех"
            }.waitUntil { isOccurred() }
        }

        onScreen<YandexRentPaymentScreen> {
            event("Аренда. Переход к оплате аренды") {
                "Источник" to "Подписание договора"
            }.waitUntil { isOccurred() }
            pressBack()
        }

        onScreen<RentShowingCardScreen> {
            accentActionButton
                .waitUntil { isTextEquals(Showing.ACTION_PAY_TEXT) }
                .click()
        }

        onScreen<YandexRentPaymentScreen> {
            event("Аренда. Переход к оплате аренды") {
                "Источник" to "Карточка показа"
            }.waitUntil { isOccurred() }

            paymentMethodView(getResourceString(R.string.payment_method_new_card))
                .waitUntil { isCompletelyDisplayed() }
                .click()

            registerResultOkIntent(matchesAcquiringIntent())
            payButton.click()

            event("Аренда. Выбор способа оплаты аренды") {
                "Способ оплаты" to "Карта"
            }.waitUntil { isOccurred() }

            event("Аренда. Результат оплаты аренды") {
                "Результат" to "Успех"
                "Способ оплаты" to "Карта"
            }.waitUntil { isOccurred() }

            val resultTitleRes = R.string.yandex_rent_contract_concluded_title
            waitUntil { listView.contains(resultItem(resultTitleRes)) }
            resultActionButton(resultTitleRes).click()
        }

        onScreen<ServicesScreen> {
            waitUntil {
                listView.contains(rentFlatHeaderItem(FLAT_ADDRESS))
                listView.doesNotContain(rentFlatSearchItem)
            }
        }
    }

    @Test
    fun openContractWhenAlreadySigned() {
        configureWebServer {
            registerServicesInfo(
                rentRole = RENT_ROLE_TENANT_CANDIDATE,
                showRentFlatSearch = true
            )
            registerRentFlats()
            registerShowing(widget = signContractWidget())
            registerContractSummary(status = "SIGNED")
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatSearchItem
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentShowingsScreen> {
            notificationItem(SIGN_CONTRACT_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<RentContractScreen> {
            successItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("successSigned"))
        }
    }

    @Test
    fun openContractWhenAlreadyActive() {
        configureWebServer {
            registerServicesInfo(
                rentRole = RENT_ROLE_TENANT_CANDIDATE,
                showRentFlatSearch = true
            )
            registerRentFlats()
            registerShowing(widget = signContractWidget())
            registerContractSummary(status = "ACTIVE")
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatSearchItem
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentShowingsScreen> {
            notificationItem(SIGN_CONTRACT_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<RentContractScreen> {
            successItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("successSigned"))
        }
    }

    @Test
    fun openFirstMonthPaymentWhenAlreadyPaid() {
        configureWebServer {
            registerServicesInfo(
                rentRole = RENT_ROLE_TENANT_CANDIDATE,
                showRentFlatSearch = true
            )
            registerRentFlats()
            registerShowing(widget = payFirstMonthWidget())
            registerPayment(paidPayment())
            registerFlatWithPayment(paidPayment())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatSearchItem
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentShowingsScreen> {
            notificationItem(PAY_FIRST_MONTH_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<YandexRentPaymentScreen> {
            waitUntil { paymentView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("successPayment"))
        }
    }

    @Test
    fun openContractWhenError() {
        configureWebServer {
            registerServicesInfo(
                rentRole = RENT_ROLE_TENANT_CANDIDATE,
                showRentFlatSearch = true
            )
            registerRentFlats()
            registerShowing(widget = signContractWidget())
            registerContractSummaryError()
            registerContractSummary()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatSearchItem
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentShowingsScreen> {
            notificationItem(SIGN_CONTRACT_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<RentContractScreen> {
            fullscreenErrorItem
                .waitUntil { listView.contains(this) }
                .invoke { retryButton.click() }

            waitUntil { listView.contains(faqButtonItem) }
        }
    }

    @Test
    fun openFaq() {
        configureWebServer {
            registerServicesInfo(
                rentRole = RENT_ROLE_TENANT_CANDIDATE,
                showRentFlatSearch = true
            )
            registerRentFlats()
            registerShowing(widget = signContractWidget())
            registerContractSummary()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatSearchItem
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentShowingsScreen> {
            notificationItem(SIGN_CONTRACT_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<RentContractScreen> {
            faqButtonItem
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentContractFaqScreen> {
            waitUntil { listView.contains(questionItem(QUESTION_TEXT)) }

            listView.isContentStateMatches(getTestRelatedFilePath("faqCollapsed"))

            listView.scrollTo(questionItem(QUESTION_TEXT))
                .click()

            listView.isContentStateMatches(getTestRelatedFilePath("faqExpanded"))

            registerResultOkIntent(matchesOpenAnswerLinkIntent(), null)
            answerItem(ANSWER_TEXT)
                .waitUntil { listView.contains(this) }
                .tapOnLinkText(ANSWER_LINK_TEXT)

            intended(matchesOpenAnswerLinkIntent())
        }
    }

    private fun matchesOpenAnswerLinkIntent(): NamedIntentMatcher {
        return NamedIntentMatcher(
            "Открытие ссылки из ответа faq",
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(TERMS_URL)
            )
        )
    }

    private fun signContractWidget(): JsonObject {
        return jsonObject {
            "html" to SIGN_CONTRACT_TITLE
            "type" to "INFO"
            "action" to jsonObject {
                "buttonText" to "Подписать"
                "actionSignContract" to jsonObject {
                    "contractId" to CONTRACT_ID
                }
            }
        }
    }

    private fun payFirstMonthWidget(): JsonObject {
        return jsonObject {
            "html" to PAY_FIRST_MONTH_TITLE
            "type" to "INFO"
            "action" to jsonObject {
                "buttonText" to "Оплатить"
                "actionPayFirstMonth" to jsonObject {
                    "paymentId" to PAYMENT_ID
                }
            }
        }
    }

    private fun newPayment(): JsonObject {
        return payment("NEW")
    }

    private fun paidPayment(): JsonObject {
        return payment("PAID_BY_TENANT")
    }

    private fun payment(status: String): JsonObject {
        return jsonObject {
            "id" to PAYMENT_ID
            "status" to status
            "tenantRentPayment" to jsonObject {
                "startDate" to "2022-06-24"
                "endDate" to "2022-07-24"
                "paymentDate" to "2022-06-24"
            }
            "tenantSpecificPaymentInfo" to jsonObject {
                "amount" to "4200000"
            }
        }
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

    private fun DispatcherRegistry.registerContractSummary(
        status: String = "SIGNED_BY_OWNER"
    ) {
        register(
            request {
                path("2.0/rent/user/me/contracts/$CONTRACT_ID/summary")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "summary" to jsonObject {
                            "contractId" to CONTRACT_ID
                            "status" to status
                            "terms" to jsonObject {
                                "version" to TERMS_VERSION
                                "url" to TERMS_URL
                            }
                            "summary" to contractSummary()
                            "faq" to faq()
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerContractSummaryError() {
        register(
            request {
                path("2.0/rent/user/me/contracts/$CONTRACT_ID/summary")
            },
            response {
                error()
            }
        )
    }

    @Suppress("MaxLineLength")
    private fun faq(): JsonArray {
        return Gson().fromJson(
            """
                    [{
                        "id": "1",
                        "question": "Можно ли изменить дату оплаты?",
                        "answer": "$ANSWER_TEXT <a href=\"$TERMS_URL\">$ANSWER_LINK_TEXT</a>"
                    }, {
                        "id": "2",
                        "question": "Почему отличается сумма аренды в договоре и в объявлении?",
                        "answer": "В объявлении указана сумма с учётом оплаты аренды и стоимости услуг Яндекс Аренды. Сумма в договоре аренды — это та сумма, которую собственник получает каждый месяц на руки."
                    }, {
                        "id": "3",
                        "question": "Кто платит за страховку? Мне нужно что-то доплачивать?",
                        "answer": "Страховка уже включена в ту сумму, которая указана в объявлении. Она окончательная, ничего дополнительно платить не нужно."
                    }, {
                        "id": "4",
                        "question": "В договоре и в Сервисе нет показаний счётчиков. Что делать?",
                        "answer": "После заселения сфотографируйте показания счетчиков и отправьте нам: +79265159873 — Watsapp или Telegram «yandex_arenda_supbot»"
                    }]
            """.trimIndent(),
            JsonArray::class.java
        )
    }

    private fun DispatcherRegistry.registerSmsCodeRequest() {
        register(
            request {
                path("2.0/rent/user/me/contracts/$CONTRACT_ID/confirmation-code/request")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "sentSmsInfo" to jsonObject {
                            "codeLength" to 5
                            "requestId" to SMS_REQUEST_ID
                            "phone" to "+79336687742"
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerSmsCodeSubmit() {
        register(
            request {
                path("2.0/rent/user/me/contracts/$CONTRACT_ID/confirmation-code/submit")
                jsonBody {
                    "confirmSmsInfo" to jsonObject {
                        "code" to SMS_CODE
                        "requestId" to SMS_REQUEST_ID
                    }
                    "acceptedTermsVersion" to TERMS_VERSION
                }
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "firstPaymentId" to PAYMENT_ID
                    }
                }
            }
        )
    }

    private companion object {

        const val SIGN_CONTRACT_TITLE = "Подпишите договор аренды"
        const val PAY_FIRST_MONTH_TITLE = "Оплатите первый месяц аренды"
        const val TERMS_VERSION = 23
        const val TERMS_URL = "https://test.vertis.yandex.ru/legal/realty_lease_tenant/"
        const val SMS_CODE = "48122"
        const val SMS_REQUEST_ID = "8ddd3c24c66d"
        const val QUESTION_TEXT = "Можно ли изменить дату оплаты?"
        const val ANSWER_TEXT = "Нет, подробнее читайте"
        const val ANSWER_LINK_TEXT = "здесь"
    }
}
