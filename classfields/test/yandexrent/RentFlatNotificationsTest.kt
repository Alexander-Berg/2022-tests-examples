package com.yandex.mobile.realty.test.yandexrent

import androidx.test.filters.LargeTest
import com.google.gson.JsonObject
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesExternalViewUrlIntent
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.*
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.*
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author andrey-bgm on 15/09/2021.
 */
@LargeTest
class RentFlatNotificationsTest : BaseTest() {

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
    fun showNotificationHeader() {
        configureWebServer {
            registerTenantRentFlat(
                actualPayment = jsonObject {
                    "id" to "paymentId00001"
                },
                notification = jsonObject {
                    "tenantRentFirstPayment" to jsonObject {}
                    "textLinkSheet" to jsonObject {
                        "text" to NOTIFICATION_HEADER_TEXT
                        "link" to jsonObject {
                            "text" to NOTIFICATION_HEADER_LINK_TEXT
                            "url" to NOTIFICATION_HEADER_LINK_URL
                        }
                        "type" to "GUARANTEE_FOR_TENANT"
                    }
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        registerResultOkIntent(matchesExternalViewUrlIntent(NOTIFICATION_HEADER_LINK_URL), null)

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_first_payment_title)
            listView.contains(notificationItem(title))
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { headerView.tapOnLinkText(NOTIFICATION_HEADER_LINK_TEXT) }

            intended(matchesExternalViewUrlIntent(NOTIFICATION_HEADER_LINK_URL))
        }
    }

    @Test
    fun checkNotificationMinHeight() {
        configureWebServer {
            registerTenantRentFlat(
                notification = jsonObject {
                    "fallback" to jsonObject {
                        "title" to "Заполните анкету"
                    }
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            listView.contains(notificationItem("Заполните анкету"))
                .isViewStateMatches(getTestRelatedFilePath("notification"))
        }
    }

    @Test
    fun showTenantContractActiveNotification() {
        configureWebServer {
            registerTenantRentFlat(
                notification = contractNotification("tenantRentContractIsActive")
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_contract_is_active_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
        }
    }

    @Test
    fun showTenantRentFirstPaymentNotification() {
        configureWebServer {
            registerTenantRentFlat(
                actualPayment = jsonObject {
                    "id" to "paymentId00001"
                },
                notification = jsonObject {
                    "tenantRentFirstPayment" to jsonObject {}
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_first_payment_title)
            listView.contains(notificationItem(title))
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }

        onScreen<YandexRentPaymentScreen> {
            waitUntil { paymentView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun showTenantRentReadyToPayNotification() {
        configureWebServer {
            registerTenantRentFlat(
                actualPayment = jsonObject {
                    "id" to "paymentId00001"
                },
                notification = jsonObject {
                    "tenantRentReadyToPay" to jsonObject {
                        "paymentDate" to "2021-09-14"
                    }
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = "14 сентября\u00A0— день оплаты"
            listView.contains(notificationItem(title))
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }

        onScreen<YandexRentPaymentScreen> {
            waitUntil { paymentView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun showTenantRentPaymentTodayNotification() {
        configureWebServer {
            registerTenantRentFlat(
                actualPayment = jsonObject {
                    "id" to "paymentId00001"
                },
                notification = jsonObject {
                    "tenantRentPaymentToday" to jsonObject {}
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_today_payment_title)
            listView.contains(notificationItem(title))
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }

        onScreen<YandexRentPaymentScreen> {
            waitUntil { paymentView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun showTenantRentPaymentOutdatedNotification() {
        configureWebServer {
            registerTenantRentFlat(
                actualPayment = jsonObject {
                    "id" to "paymentId00001"
                    "tenantSpecificPaymentInfo" to jsonObject {
                        "amount" to "4200000"
                    }
                },
                notification = jsonObject {
                    "tenantRentPaymentOutdated" to jsonObject {}
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_outdated_payment_title)
            listView.contains(notificationItem(title))
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }

        onScreen<YandexRentPaymentScreen> {
            waitUntil { paymentView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun showTenantRentPaidNotification() {
        configureWebServer {
            registerTenantRentFlat(
                actualPayment = jsonObject {
                    "id" to "paymentId00001"
                },
                notification = jsonObject {
                    "tenantRentPaid" to jsonObject {
                        "paidToDate" to "2021-10-13"
                    }
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_rent_paid_title)
            listView.contains(notificationItem(title))
                .isViewStateMatches(getTestRelatedFilePath("notification"))
        }
    }

    @Test
    fun showTenantRentConditionsNotification() {
        configureWebServer {
            registerTenantRentFlat(
                notification = jsonObject {
                    "houseServiceSettingsAcceptanceRequired" to jsonObject {}
                }
            )
            registerTenantRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        val notificationTitle = getResourceString(
            R.string.yandex_rent_tenant_settings_acceptance_required_title
        )
        val url = "https://arenda.test.vertis.yandex.ru/" +
            "lk/tenant/flat/$FLAT_ID/house-services/settings/confirmation?only-content=true"

        onScreen<RentFlatScreen> {
            notificationItem(notificationTitle)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }
        checkRentWebViewForm(url)
        onScreen<RentFlatScreen> {
            waitUntil {
                listView.doesNotContain(notificationItem(notificationTitle))
            }
        }
    }

    @Test
    fun showOwnerPassportInfoFormFromSimpleNotification() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "needToAddPassport" to jsonObject {}
                }
            )
            registerOwnerRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        val notificationTitle = getResourceString(
            R.string.yandex_rent_owner_need_to_add_passport_title
        )
        val url = "https://arenda.test.vertis.yandex.ru/" +
            "lk/personal-data/edit?only-content=true"

        onScreen<RentFlatScreen> {
            notificationItem(notificationTitle)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }
        checkRentWebViewForm(url)
        onScreen<RentFlatScreen> {
            waitUntil {
                listView.doesNotContain(notificationItem(notificationTitle))
            }
        }
    }

    @Test
    fun showOwnerPassportInfoFormFromTodoNotification() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = ownerConfirmedTodoNotification(passportDone = false)
            )
            registerOwnerRentFlat(
                notification = ownerConfirmedTodoNotification(passportDone = true)
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        val notificationTitle = getResourceString(
            R.string.yandex_rent_owner_confirmed_todo_title
        )
        val actionTitle = "Паспортные данные"
        val url = "https://arenda.test.vertis.yandex.ru/" +
            "lk/personal-data/edit?only-content=true"

        onScreen<RentFlatScreen> {
            todoNotificationHeaderItem(notificationTitle)
                .waitUntil { listView.contains(this) }
            listView.isItemsStateMatches(
                key = getTestRelatedFilePath("notCheckedNotification"),
                fromItem = todoNotificationHeaderItem(notificationTitle),
                count = 5
            )
            todoNotificationActionItem(actionTitle)
                .view
                .invoke { actionButton.click() }
        }
        checkRentWebViewForm(url)
        onScreen<RentFlatScreen> {
            todoNotificationActionItem(actionTitle)
                .waitUntil {
                    listView.contains(this)
                    view.invoke { doneView.isCompletelyDisplayed() }
                }
            listView.isItemsStateMatches(
                key = getTestRelatedFilePath("checkedNotification"),
                fromItem = todoNotificationHeaderItem(notificationTitle),
                count = 5
            )
        }
    }

    @Test
    fun showOwnerFlatInfoForm() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = ownerConfirmedTodoNotification(flatInfoDone = false)
            )
            registerOwnerRentFlat(
                notification = ownerConfirmedTodoNotification(flatInfoDone = true)
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        val notificationTitle = getResourceString(
            R.string.yandex_rent_owner_confirmed_todo_title
        )
        val actionTitle = "Детали по\u00A0квартире"
        val url = "https://arenda.test.vertis.yandex.ru/" +
            "lk/owner/flat/$FLAT_ID/questionnaire?only-content=true"

        onScreen<RentFlatScreen> {
            todoNotificationHeaderItem(notificationTitle)
                .waitUntil { listView.contains(this) }
            listView.isItemsStateMatches(
                key = getTestRelatedFilePath("notCheckedNotification"),
                fromItem = todoNotificationHeaderItem(notificationTitle),
                count = 5
            )
            listView.scrollTo(todoNotificationActionItem(actionTitle))
                .invoke { actionButton.click() }
        }
        checkRentWebViewForm(url)
        onScreen<RentFlatScreen> {
            todoNotificationActionItem(actionTitle)
                .waitUntil {
                    listView.contains(this)
                    view.invoke { doneView.isCompletelyDisplayed() }
                }
            listView.isItemsStateMatches(
                key = getTestRelatedFilePath("checkedNotification"),
                fromItem = todoNotificationHeaderItem(notificationTitle),
                count = 5
            )
        }
    }

    @Test
    fun showOwnerRentConditionsConfigurationForm() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "houseServiceSettingsConfigurationRequired" to jsonObject {}
                }
            )
            registerOwnerRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        val notificationTitle = getResourceString(
            R.string.yandex_rent_owner_settings_configuration_required_title
        )

        onScreen<RentFlatScreen> {
            notificationItem(notificationTitle)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }
        checkRentWebViewForm(UTILITIES_CONDITIONS_URL)
        onScreen<RentFlatScreen> {
            waitUntil {
                listView.doesNotContain(notificationItem(notificationTitle))
            }
        }
    }

    @Test
    fun showOwnerRentConditionsConfigurationIncompleteForm() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlat(
                notification = jsonObject {
                    "houseServiceSettingsConfigurationIncomplete" to jsonObject {}
                }
            )
            registerOwnerServicesInfo()
            registerOwnerRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        val notificationTitle = getResourceString(
            R.string.yandex_rent_owner_settings_configuration_incomplete_title
        )

        onScreen<RentFlatScreen> {
            notificationItem(notificationTitle)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }
        checkRentWebViewForm(UTILITIES_CONDITIONS_URL)
        onScreen<RentFlatScreen> {
            waitUntil {
                listView.doesNotContain(notificationItem(notificationTitle))
            }
        }
    }

    @Test
    fun showTenantCandidates() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "checkTenantCandidates" to jsonObject {}
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        val notificationTitle = getResourceString(
            R.string.yandex_rent_owner_check_tenant_candidates_title
        )
        val url = "https://arenda.test.vertis.yandex.ru/" +
            "lk/owner/flat/$FLAT_ID/tenant-candidates?only-content=true"

        onScreen<RentFlatScreen> {
            notificationItem(notificationTitle)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }
        onScreen<WebViewScreen> {
            webView.waitUntil { isPageUrlEquals(url) }
        }
    }

    @Test
    fun showFlatNotificationFallback() {
        configureWebServer {
            registerTenantRentFlat(
                notification = jsonObject {
                    "fallback" to jsonObject {
                        "title" to "Заполните анкету жильца"
                        "subtitle" to "Чтобы собственник лучше вас узнал"
                        "action" to jsonObject {
                            "buttonText" to "Заполнить анкету"
                            "url" to "https://arenda.test.vertis.yandex.ru/profile"
                        }
                    }
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            listView.contains(notificationItem("Заполните анкету жильца"))
                .isViewStateMatches(getTestRelatedFilePath("fallback"))
                .invoke { actionButton.click() }

            val url = "https://arenda.test.vertis.yandex.ru/profile"
            intended(matchesExternalViewUrlIntent(url))
        }
    }

    @Test
    fun showOwnerWaitingForPaymentDateNotification() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "ownerRentWaitingForPaymentDate" to jsonObject {
                        "paymentDate" to "2021-10-15"
                    }
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_owner_waiting_payment_title)
            listView.contains(notificationItem(title))
                .isViewStateMatches(getTestRelatedFilePath("notification"))
        }
    }

    @Test
    fun showOwnerRentHoldingForPaymentDateNotification() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "ownerRentHoldingForPaymentDate" to jsonObject {
                        "paymentDate" to "2021-10-15"
                    }
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_owner_waiting_payment_title)
            listView.contains(notificationItem(title))
                .isViewStateMatches(getTestRelatedFilePath("notification"))
        }
    }

    @Test
    fun showOwnerRentExpectingPaymentNotification() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "ownerRentExpectingPayment" to jsonObject {
                        "paymentDate" to "2021-10-15"
                    }
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = "15 октября\u00A0— день оплаты"
            listView.contains(notificationItem(title))
                .isViewStateMatches(getTestRelatedFilePath("notification"))
        }
    }

    @Test
    fun showOwnerRentWaitingForPayoutNotification() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "ownerRentWaitingForPayout" to jsonObject {}
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_owner_waiting_for_payout_title)
            listView.contains(notificationItem(title))
                .isViewStateMatches(getTestRelatedFilePath("notification"))
        }
    }

    @Test
    fun showOwnerRentCardUnavailableNotification() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "ownerRentCardUnavailable" to jsonObject {}
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_owner_card_unavailable_title)
            listView.contains(notificationItem(title))
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }

        onScreen<PaymentCardsScreen> {
            waitUntil { contentView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun showOwnerRentPayoutBrokenNotification() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "ownerRentPayoutBroken" to jsonObject {}
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_owner_payout_broken_title)
            listView.contains(notificationItem(title))
                .isViewStateMatches(getTestRelatedFilePath("notification"))
        }
    }

    @Test
    fun showOwnerRentPaidOutToCardNotification() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "ownerRentPaidOutToCard" to jsonObject {
                        "maskedCardNumber" to "*654321"
                    }
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_owner_paid_out_to_card_title)
            listView.contains(notificationItem(title))
                .isViewStateMatches(getTestRelatedFilePath("notification"))
        }
    }

    @Test
    fun showOwnerRentPaidOutToAccountNotification() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "ownerRentPaidOutToAccount" to jsonObject {}
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_owner_paid_out_to_account_title)
            listView.contains(notificationItem(title))
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }

        onScreen<RentOwnerCardBindingScreen> {
            waitUntil { contentView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun showOwnerRentWithoutCardNotification() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "ownerWithoutCard" to jsonObject {}
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_owner_no_card_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }

        onScreen<RentOwnerCardBindingScreen> {
            waitUntil { contentView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun showOwnerRentWithoutCardWithInfoNotification() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "ownerWithoutCard" to jsonObject {
                        "paymentInfo" to jsonObject {
                            "amountKopecks" to "4200000"
                        }
                    }
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = "Готовы зачислить 42\u00A0000\u00A0₽"
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }

        onScreen<RentOwnerCardBindingScreen> {
            waitUntil { contentView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun showOwnerRentWithManyCardsNotification() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "ownerWithManyCards" to jsonObject {}
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_owner_many_cards_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }

        onScreen<PaymentCardsScreen> {
            waitUntil { contentView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun showOwnerRentPaymentInfoTodoNotification() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = ownerPaymentInfoTodoNotification(
                    addCardDone = false,
                    addInnDone = false
                )
            )
            registerOwnerRentFlat(
                notification = ownerPaymentInfoTodoNotification(
                    addCardDone = true,
                    addInnDone = true
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_owner_payment_todo_title)
            val addCardTitle = getResourceString(R.string.yandex_rent_owner_payment_todo_add_card)
            todoNotificationHeaderItem(title)
                .waitUntil { listView.contains(this) }

            listView.isItemsStateMatches(
                key = getTestRelatedFilePath("notCheckedNotification"),
                fromItem = todoNotificationHeaderItem(title),
                count = 4
            )

            todoNotificationActionItem(addCardTitle)
                .view
                .invoke { actionButton.click() }

            onScreen<RentOwnerCardBindingScreen> {
                waitUntil { contentView.isCompletelyDisplayed() }
                pressBack()
            }

            val addInnTitle = getResourceString(R.string.yandex_rent_owner_payment_todo_add_inn)
            todoNotificationActionItem(addInnTitle)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }

            onScreen<RentOwnerInnScreen> {
                waitUntil { contentView.isCompletelyDisplayed() }
                pressBack()
            }

            listView.swipeDown()

            todoNotificationHeaderItem(title)
                .waitUntil { listView.contains(this) }

            listView.isItemsStateMatches(
                key = getTestRelatedFilePath("checkedNotification"),
                fromItem = todoNotificationHeaderItem(title),
                count = 4
            )
        }
    }

    @Test
    fun showOwnerRentWithoutInnNotification() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "ownerWithoutInn" to jsonObject {}
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_owner_no_inn_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }

        onScreen<RentOwnerInnScreen> {
            waitUntil { contentView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun showOwnerRequestDeclinedNotification() {
        showSimpleNotification(
            rentRole = RENT_ROLE_OWNER,
            notificationKey = "ownerRequestDeclined",
            titleRes = R.string.yandex_rent_owner_request_declined_title
        )
    }

    @Test
    fun showOwnerKeysStillWithYouNotification() {
        showSimpleNotification(
            rentRole = RENT_ROLE_OWNER,
            notificationKey = "keysStillWithYou",
            titleRes = R.string.yandex_rent_owner_keys_still_with_you_title
        )
    }

    @Test
    fun showOwnerKeysStillWithManagerNotification() {
        showSimpleNotification(
            rentRole = RENT_ROLE_OWNER,
            notificationKey = "keysStillWithManager",
            titleRes = R.string.yandex_rent_owner_keys_still_with_manager_title
        )
    }

    @Test
    fun showOwnerKeysHandedOverToManager() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "keysHandedOverToManager" to jsonObject {
                        "handoverId" to KEYS_HANDOVER_ID
                    }
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        val title = getResourceString(R.string.yandex_rent_owner_keys_handed_over_to_manager_title)

        onScreen<RentFlatScreen> {
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
        }
    }

    @Test
    fun showOwnerLookingForTenantsNotification() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "lookingForTenants" to jsonObject {
                        "offerId" to OFFER_ID
                    }
                }
            )
            registerOffer()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_owner_looking_for_tenants_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }
        onScreen<OfferCardScreen> {
            waitUntil { floatingCommButtons.isCompletelyDisplayed() }
        }
    }

    @Test
    fun showOwnerPreparingFlatForExpositionNotification() {
        showSimpleNotification(
            rentRole = RENT_ROLE_OWNER,
            notificationKey = "preparingFlatForExposition",
            titleRes = R.string.yandex_rent_owner_preparing_for_exposition_title
        )
    }

    @Test
    fun showOwnerWaitingForArendaTeamContactNotification() {
        showSimpleNotification(
            rentRole = RENT_ROLE_OWNER,
            notificationKey = "waitingForArendaTeamContact",
            titleRes = R.string.yandex_rent_owner_waiting_for_arenda_team_contact_title
        )
    }

    @Test
    fun showOwnerFlatDraftNeedToFinish() {
        checkOwnerNotificationWithDraft(
            notification = "draftNeedToFinish",
            titleRes = R.string.yandex_rent_owner_draft_need_to_finish_title
        )
    }

    @Test
    fun showOwnerFlatDraftNeedConfirmation() {
        checkOwnerNotificationWithDraft(
            notification = "draftNeedConfirmation",
            titleRes = R.string.yandex_rent_owner_draft_need_confirmation_title
        )
    }

    @Test
    fun showOwnerRequestCanceledByOwner() {
        checkOwnerNotificationWithDraft(
            notification = "ownerRequestCanceledByOwner",
            titleRes = R.string.yandex_rent_owner_request_canceled_title
        )
    }

    @Test
    fun showOwnerRentIsFinished() {
        checkOwnerNotificationWithDraft(
            notification = "ownerRentIsFinished",
            titleRes = R.string.yandex_rent_owner_rent_over_title
        )
    }

    @Test
    fun showOwnerPrepareForMeetingNotification() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "ownerPrepareFlatForMeeting" to jsonObject {}
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        registerResultOkIntent(matchesExternalViewUrlIntent(RENT_PHOTO_TUTORIAL_URL), null)

        val title = getResourceString(R.string.yandex_rent_owner_prepare_for_meeting_title)

        onScreen<RentFlatScreen> {
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }

            intended(matchesExternalViewUrlIntent(RENT_PHOTO_TUTORIAL_URL))
        }
    }

    @Test
    fun showOwnerFlatIsRented() {
        showSimpleNotification(
            rentRole = RENT_ROLE_OWNER,
            notificationKey = "ownerFlatIsRented",
            titleRes = R.string.yandex_rent_owner_flat_is_rented_title
        )
    }

    private fun showSimpleNotification(
        rentRole: String,
        notificationKey: String,
        titleRes: Int
    ) {
        configureWebServer {
            registerRentFlat(
                rentRole = rentRole,
                notifications = listOf(
                    jsonObject {
                        notificationKey to jsonObject {}
                    }
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        val title = getResourceString(titleRes)

        onScreen<RentFlatScreen> {
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
        }
    }

    private fun checkRentWebViewForm(url: String) {
        onScreen<WebViewScreen> {
            val submitScript = "(function() { mobileAppInjector.onFormSubmitted(); })();"
            webView
                .waitUntil { isPageUrlEquals(url) }
                .invoke { evaluateJavascript(submitScript) }
        }
    }

    private fun checkOwnerNotificationWithDraft(
        notification: String,
        titleRes: Int
    ) {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    notification to jsonObject {}
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(titleRes)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }

        onScreen<RentFlatFormScreen> {
            waitUntil { contentView.isCompletelyDisplayed() }
        }
    }

    private fun ownerConfirmedTodoNotification(
        flatPhotosDone: Boolean = false,
        flatInfoDone: Boolean = false,
        passportDone: Boolean = false,
    ): JsonObject {
        return jsonObject {
            "ownerConfirmedTodo" to jsonObject {
                "items" to jsonArrayOf(
                    jsonObject {
                        "addFlatPhotos" to jsonObject {}
                        "done" to flatPhotosDone
                    },
                    jsonObject {
                        "addFlatInfo" to jsonObject {}
                        "done" to flatInfoDone
                    },
                    jsonObject {
                        "addPassport" to jsonObject {}
                        "done" to passportDone
                    },
                )
            }
        }
    }

    private fun ownerPaymentInfoTodoNotification(
        addCardDone: Boolean = false,
        addInnDone: Boolean = false,
    ): JsonObject {
        return jsonObject {
            "ownerPaymentInfoTodo" to jsonObject {
                "items" to jsonArrayOf(
                    jsonObject {
                        "addPaymentCard" to jsonObject {}
                        "done" to addCardDone
                    },
                    jsonObject {
                        "addInn" to jsonObject {}
                        "done" to addInnDone
                    },
                )
            }
        }
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
                queryParam("id", OFFER_ID)
            },
            response {
                assetBody("RentFlatNotificationsTest/offerCard.json")
            }
        )
    }

    private companion object {

        const val OFFER_ID = "0"
        const val RENT_PHOTO_TUTORIAL_URL =
            "https://realty.test.vertis.yandex.ru/export/arenda/realty_photo_tutorial.pdf"
        const val UTILITIES_CONDITIONS_URL = "https://arenda.test.vertis.yandex.ru/" +
            "lk/owner/flat/$FLAT_ID/house-services/settings?only-content=true"

        const val NOTIFICATION_HEADER_TEXT = "\uD83E\uDD11 Вы получаете арендную плату " +
            "вовремя, даже если жилец задерживает платеж."
        const val NOTIFICATION_HEADER_LINK_TEXT = "Подробнее."
        const val NOTIFICATION_HEADER_LINK_URL = "https://realty.test.vertis.yandex.ru/help"
        const val KEYS_HANDOVER_ID = "keysHandoverId0001"
    }
}
