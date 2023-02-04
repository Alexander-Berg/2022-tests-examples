package com.yandex.mobile.realty.test.yandexrent.contract

import android.content.Intent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.filters.LargeTest
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.*
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.viewMatchers.NamedIntentMatcher
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.data.model.proto.ErrorCode
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.CONTRACT_ID
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.test.services.registerOwnerRentFlat
import com.yandex.mobile.realty.test.yandexrent.contractNotification
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.hamcrest.CoreMatchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author sorokinandrei on 4/29/22
 */
@LargeTest
class OwnerContractSigningTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = RentFlatActivityTestRule(
        flatId = FLAT_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun shouldSignContract() {
        configureWebServer {
            registerOwnerRentFlat(notification = contractNotification("ownerSignRentContract"))
            registerContractSummary()
            registerContractSigningSmsCodeRequest()
            registerContractSigningSmsCodeSubmit()
            registerOwnerRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openContract(NOTIFICATION_SIGN_TITLE)

        onScreen<RentContractScreen> {
            actionButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        onScreen<RentSmsConfirmationScreen> {
            smsCodeItem
                .waitUntil { listView.contains(this) }
                .invoke { inputView.typeText(SMS_CODE, closeKeyboard = false) }

            successItem
                .waitUntil { listView.contains(this) }
                .also {
                    root.isViewStateMatches(getTestRelatedFilePath("successContent"))
                }
                .invoke { okButton.click() }
        }

        onScreen<RentFlatScreen> {
            listView.waitUntil {
                contains(flatHeaderItem)
                doesNotContain(notificationItem(NOTIFICATION_SIGN_TITLE))
            }
        }
    }

    @Test
    fun shouldShowContractErrors() {
        configureWebServer {
            registerOwnerRentFlat(notification = contractNotification("ownerSignRentContract"))
            registerContractSummaryError()
            registerContractSummary()
            registerContractRequestChangesConflictError()
            registerContractRequestChangesValidationError("/comment")
            registerContractRequestChangesValidationError("/unknown")
            registerContractRequestChangesError()
            registerContractRequestChanges()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openContract(NOTIFICATION_SIGN_TITLE)

        onScreen<RentContractScreen> {
            fullscreenErrorItem
                .waitUntil { listView.contains(this) }
                .invoke { retryButton.click() }

            listView.scrollTo(faqButtonItem)
                .click()
        }

        onScreen<RentContractFaqScreen> {
            suggestChangesButtonItem
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentContractSuggestChangesScreen> {
            inputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(INPUT_COMMENT, closeKeyboard = false)

            submitButton.click()
            toastView(CONFLICT_MESSAGE)
                .waitUntil { isCompletelyDisplayed() }

            submitButton.click()
            inputErrorView.waitUntil { isCompletelyDisplayed() }
            isContentStateMatches(getTestRelatedFilePath("validationError"))

            submitButton.click()
            onScreen<ConfirmationDialogScreen> {
                waitUntil { titleView.isCompletelyDisplayed() }
                root.isViewStateMatches("dialog/needAppUpdateDialog")
                cancelButton.click()
            }

            submitButton.click()
            toastView(getResourceString(R.string.error_try_again))
                .waitUntil { isCompletelyDisplayed() }

            pressBack()
            onScreen<ConfirmationDialogScreen> {
                waitUntil { titleView.isCompletelyDisplayed() }
                root.isViewStateMatches(getTestRelatedFilePath("rejectChanges"))
                cancelButton.click()
            }
            pressBack()
            onScreen<ConfirmationDialogScreen> {
                waitUntil { titleView.isCompletelyDisplayed() }
                confirmButton.click()
            }
            onScreen<RentContractFaqScreen> {
                listView.contains(suggestChangesButtonItem)
            }
        }
    }

    @Test
    fun shouldOpenCommentAndFaqInfo() {
        configureWebServer {
            registerOwnerRentFlat(notification = contractNotification("ownerSignRentContract"))
            registerContractSummary(managerAnswer = "sample manager comment")
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        registerResultOkIntent(matchesOpenAnswerLinkIntent(), null)

        openContract(NOTIFICATION_SIGN_TITLE)

        onScreen<RentContractScreen> {
            managerCommentItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("managerComment"))
                .click()

            onScreen<SimpleInfoScreen> {
                dialogView.waitUntil { isCompletelyDisplayed() }
                    .isViewStateMatches(getTestRelatedFilePath("infoView"))
                pressBack()
            }

            listView.scrollTo(faqButtonItem)
                .click()
        }

        onScreen<RentContractFaqScreen> {
            questionItem(QUESTION_TEXT)
                .waitUntil { listView.contains(this) }

            listView.doesNotContain(suggestChangesButtonItem)

            listView.isContentStateMatches(getTestRelatedFilePath("faqCollapsed"))

            listView.scrollTo(questionItem(QUESTION_TEXT))
                .click()

            listView.isContentStateMatches(getTestRelatedFilePath("faqExpanded"))

            answerItem(ANSWER_TEXT)
                .waitUntil { listView.contains(this) }
                .tapOnLinkText(ANSWER_LINK)

            intended(matchesOpenAnswerLinkIntent())

            listView.scrollTo(questionItem(QUESTION_TEXT))
                .click()

            listView.isContentStateMatches(getTestRelatedFilePath("faqCollapsed"))
        }
    }

    @Test
    fun showContractFaqAndSuggestChanges() {
        configureWebServer {
            registerOwnerRentFlat(notification = contractNotification("ownerSignRentContract"))
            registerContractSummary()
            registerContractRequestChanges()
            registerOwnerRentFlat(
                notification = contractNotification("ownerRentContractChangesRequested")
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openContract(NOTIFICATION_SIGN_TITLE, screenshot = true)

        onScreen<RentContractScreen> {
            waitUntil { listView.contains(faqButtonItem) }
            listView.isContentStateMatches(getTestRelatedFilePath("summaryWithoutComment"))

            listView.scrollTo(faqButtonItem)
                .click()
        }

        onScreen<RentContractFaqScreen> {
            suggestChangesButtonItem
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentContractSuggestChangesScreen> {
            inputView.waitUntil { isCompletelyDisplayed() }

            closeKeyboard()

            isContentStateMatches(getTestRelatedFilePath("changesForm"))

            inputView.typeText(INPUT_COMMENT)

            isContentStateMatches(getTestRelatedFilePath("filled"))

            submitButton.click()

            waitUntil { successView.isCompletelyDisplayed() }
            isContentStateMatches(getTestRelatedFilePath("success"))
            successView.invoke { okButton.click() }
        }

        onScreen<RentFlatScreen> {
            notificationItem(NOTIFICATION_CHANGES_REQUESTED_TITLE)
                .waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldShowAlreadySentChanges() {
        configureWebServer {
            registerOwnerRentFlat(notification = contractNotification("ownerSignRentContract"))
            registerContractSummary(status = "DRAFT", ownerComment = "sample owner comment")
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openContract(NOTIFICATION_SIGN_TITLE)

        onScreen<RentContractScreen> {
            successItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("alreadySentComment"))
        }
    }

    @Test
    fun shouldShowAlreadySigned() {
        configureWebServer {
            registerOwnerRentFlat(notification = contractNotification("ownerSignRentContract"))
            registerContractSummary(status = "SIGNED_BY_OWNER")
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openContract(NOTIFICATION_SIGN_TITLE)

        onScreen<RentContractScreen> {
            successItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("alreadySignedComment"))
        }
    }

    @Test
    fun shouldShowChangesRequested() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = contractNotification("ownerRentContractChangesRequested")
            )
            registerContractSummary(ownerComment = "sample comment")
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        checkNotification(NOTIFICATION_CHANGES_REQUESTED_TITLE)
    }

    @Test
    fun shouldShowContractSigned() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = contractNotification("ownerRentContractSignedByOwner")
            )
            registerContractSummary()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        checkNotification(NOTIFICATION_SIGNED_TITLE)
    }

    @Test
    fun shouldShowContractActive() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = contractNotification("ownerRentContractIsActive")
            )
            registerContractSummary()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        checkNotification(NOTIFICATION_ACTIVE_TITLE)
    }

    private fun openContract(notificationTitle: String, screenshot: Boolean = false) {
        onScreen<RentFlatScreen> {
            notificationItem(notificationTitle)
                .waitUntil { listView.contains(this) }
                .invoke {
                    if (screenshot) {
                        isViewStateMatches(getTestRelatedFilePath("notification"))
                    }
                    actionButton.click()
                }
        }
    }

    private fun checkNotification(notificationTitle: String) {
        onScreen<RentFlatScreen> {
            notificationItem(notificationTitle)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
        }
    }

    private fun DispatcherRegistry.registerContractSummary(
        status: String = "SIGNING",
        ownerComment: String? = null,
        managerAnswer: String? = null,
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
                            "conversation" to jsonObject {
                                "ownerComment" to ownerComment
                                "managerAnswer" to managerAnswer
                            }
                            "summary" to sampleSummary()
                            "faq" to sampleFaq()
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

    private fun sampleSummary(): JsonArray {
        return Gson().fromJson(
            """
            [{
                "name": "Основная информация:",
                "items": [{
                    "name": "Арендная плата",
                    "value": "10 000 000 Р/мес"
                }, {
                    "name": "Тип недвижимости",
                    "value": "Квартира"
                }, {
                    "name": "Дата платежа",
                    "value": "28"
                }, {
                    "name": "Дата сдачи",
                    "value": "28.04.2022"
                }, {
                    "name": "Тип договора",
                    "value": "От собственника"
                }, {
                    "name": "Статус арендодателя",
                    "value": "Физ. лицо"
                }, {
                    "name": "Наличие животных",
                    "value": "Без животных"
                }]
            }, {
                "name": "Страховка",
                "items": [{
                    "name": "Дата полиса",
                    "value": "01.01.1970"
                }, {
                    "name": "Номер полиса",
                    "value": ""
                }, {
                    "name": "Стоимость страховки",
                    "value": "141 100 Р"
                }]
            }, {
                "name": "Данные собственника",
                "items": [{
                    "name": "ФИО",
                    "value": "Владелец Нету АндрейС"
                }, {
                    "name": "Телефон",
                    "value": "+79217797243"
                }, {
                    "name": "Электронная почта",
                    "value": "andreyrockin@yandex.ru"
                }]
            }, {
                "name": "Данные жильца",
                "items": [{
                    "name": "ФИО",
                    "value": "Жилец ш АндрейС"
                }, {
                    "name": "Телефон",
                    "value": "+79217797243"
                }, {
                    "name": "Электронная почта",
                    "value": "asorokius@gmail.com"
                }]
            }]
            """.trimIndent(),
            JsonArray::class.java
        )
    }

    @Suppress("MaxLineLength")
    private fun sampleFaq(): JsonArray {
        return Gson().fromJson(
            """
            [{
                "id": "20200413_1",
                "question": "Вопрос на 100000",
                "answer": "Ответ с ссылкой <a href=\"https://localhost:8080/question\">https://localhost:8080/question</a>"
            }, {
                "id": "4",
                "question": "Как поживаете?",
                "answer": "Неплохо. Спасибо."
            }, {
                "id": "1",
                "question": "Вопрос-1 для всех",
                "answer": "Ответ на вопрос-1 для всех"
            }, {
                "id": "2",
                "question": "Вопрос-2 для владельца",
                "answer": "Ответ на вопрос-2 для владельца"
            }]
            """.trimIndent(),
            JsonArray::class.java
        )
    }

    private fun DispatcherRegistry.registerContractSigningSmsCodeRequest() {
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
                            "phone" to PHONE
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerContractSigningSmsCodeSubmit() {
        register(
            request {
                path("2.0/rent/user/me/contracts/$CONTRACT_ID/confirmation-code/submit")
                jsonBody {
                    "confirmSmsInfo" to jsonObject {
                        "code" to SMS_CODE
                        "requestId" to SMS_REQUEST_ID
                    }
                }
            },
            response { jsonBody { "response" to JsonObject() } }
        )
    }

    private fun DispatcherRegistry.registerContractRequestChanges() {
        register(
            request {
                path("2.0/rent/user/me/contracts/$CONTRACT_ID/request-changes")
                jsonBody {
                    "comment" to INPUT_COMMENT
                }
            },
            success()
        )
    }

    private fun DispatcherRegistry.registerContractRequestChangesError() {
        register(
            request {
                path("2.0/rent/user/me/contracts/$CONTRACT_ID/request-changes")
                jsonBody {
                    "comment" to INPUT_COMMENT
                }
            },
            error()
        )
    }

    private fun DispatcherRegistry.registerContractRequestChangesValidationError(
        parameter: String
    ) {
        register(
            request {
                path("2.0/rent/user/me/contracts/$CONTRACT_ID/request-changes")
                jsonBody {
                    "comment" to INPUT_COMMENT
                }
            },
            response {
                setResponseCode(400)
                jsonBody {
                    "error" to jsonObject {
                        "code" to ErrorCode.VALIDATION_ERROR.name
                        "message" to "error message"
                        "data" to jsonObject {
                            "validationErrors" to jsonArrayOf(
                                jsonObject {
                                    "parameter" to parameter
                                    "code" to "code"
                                    "localizedDescription" to "Валидационная ошибка"
                                }
                            )
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerContractRequestChangesConflictError() {
        register(
            request {
                path("2.0/rent/user/me/contracts/$CONTRACT_ID/request-changes")
                jsonBody {
                    "comment" to INPUT_COMMENT
                }
            },
            response {
                setResponseCode(409)
                jsonBody {
                    "error" to jsonObject {
                        "code" to "CONFLICT"
                        "message" to CONFLICT_MESSAGE
                    }
                }
            }
        )
    }

    private fun matchesOpenAnswerLinkIntent(): NamedIntentMatcher {
        return NamedIntentMatcher(
            "Открытие ссылки из ответа faq",
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(ANSWER_LINK)
            )
        )
    }

    private companion object {

        const val NOTIFICATION_SIGN_TITLE = "Договор готов"
        const val NOTIFICATION_CHANGES_REQUESTED_TITLE = "Договор отправлен менеджеру"
        const val NOTIFICATION_SIGNED_TITLE = "Вы подписали договор"
        const val NOTIFICATION_ACTIVE_TITLE = "Договор подписан"
        const val QUESTION_TEXT = "Вопрос на 100000"
        const val ANSWER_LINK = "https://localhost:8080/question"
        const val ANSWER_TEXT = "Ответ с ссылкой"
        const val SMS_CODE = "00000"
        const val SMS_REQUEST_ID = "requestId0001"
        const val PHONE = "+79998887766"
        const val INPUT_COMMENT = "Sample comment"
        const val CONFLICT_MESSAGE = "Конфликт комментария"
    }
}
