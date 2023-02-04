package com.yandex.mobile.realty.test.yandexrent

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.gson.JsonObject
import com.yandex.mobile.realty.core.PushBroadcastCommand
import com.yandex.mobile.realty.core.pressBack
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.InventoryFormScreen
import com.yandex.mobile.realty.core.screen.InventoryPreviewScreen
import com.yandex.mobile.realty.core.screen.MeterReadingsListScreen
import com.yandex.mobile.realty.core.screen.NotificationScreen
import com.yandex.mobile.realty.core.screen.OwnerUtilitiesImagesScreen
import com.yandex.mobile.realty.core.screen.PaymentCardsScreen
import com.yandex.mobile.realty.core.screen.RentContractScreen
import com.yandex.mobile.realty.core.screen.RentFlatFormScreen
import com.yandex.mobile.realty.core.screen.RentFlatScreen
import com.yandex.mobile.realty.core.screen.RentOwnerInnScreen
import com.yandex.mobile.realty.core.screen.RentUtilitiesBillFormScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.ServicesScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.error
import com.yandex.mobile.realty.core.webserver.jsonBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.services.CONTRACT_ID
import com.yandex.mobile.realty.test.services.FLAT_ADDRESS
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author andrey-bgm on 28/10/2021.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class RentOwnerPushNotificationsTest {

    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule
    )

    @Test
    fun showInventoryFormPush() {
        authorizationRule.setUserAuthorized()
        sendInventoryFormPush()

        onScreen<NotificationScreen> {
            hasNotification(FILL_INVENTORY_TITLE, FILL_INVENTORY_BODY)
            clickOnNotification()
        }

        onScreen<InventoryFormScreen> {
            waitUntil { toolbarTitleView.isTextEquals("Помещения и объекты") }
            pressBack()
        }

        onScreen<RentFlatScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<ServicesScreen> {
            servicesTitleItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun showInventoryPreviewPush() {
        authorizationRule.setUserAuthorized()
        sendInventoryPreviewPush()

        onScreen<NotificationScreen> {
            hasNotification(SIGN_INVENTORY_TITLE, SIGN_INVENTORY_BODY)
            clickOnNotification()
        }

        onScreen<InventoryPreviewScreen> {
            waitUntil { toolbarTitleView.isTextEquals("Опись имущества") }
            pressBack()
        }

        onScreen<RentFlatScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<ServicesScreen> {
            servicesTitleItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun showPayoutBroken() {
        authorizationRule.setUserAuthorized()
        sendPayoutBrokenPush()

        onScreen<NotificationScreen> {
            hasNotification(PAYOUT_BROKEN_TITLE, PAYOUT_BROKEN_BODY)
            clickOnNotification()
        }

        onScreen<RentFlatScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<ServicesScreen> {
            servicesTitleItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun showInnPushWhenEmptyInn() {
        configureWebServer {
            registerInn(null)
        }

        authorizationRule.setUserAuthorized()
        sendInnPush(CURRENT_USER_UID)

        onScreen<NotificationScreen> {
            hasNotification(INN_PUSH_TITLE, INN_PUSH_BODY)
            clickOnNotification()
        }

        onScreen<RentOwnerInnScreen> {
            waitUntil { contentView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<ServicesScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldNotShowInnScreenWhenNotEmptyInn() {
        configureWebServer {
            registerInn("500100732259")
        }

        authorizationRule.setUserAuthorized()

        sendInnPush(CURRENT_USER_UID)

        onScreen<NotificationScreen> {
            hasNotification(INN_PUSH_TITLE, INN_PUSH_BODY)
            clickOnNotification()
        }

        onScreen<ServicesScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldNotShowInnScreenWhenInnRequestError() {
        configureWebServer {
            registerInnError()
        }

        authorizationRule.setUserAuthorized()

        sendInnPush(CURRENT_USER_UID)

        onScreen<NotificationScreen> {
            hasNotification(INN_PUSH_TITLE, INN_PUSH_BODY)
            clickOnNotification()
        }

        onScreen<ServicesScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldNotShowInnPushWhenUidDoesNotMatch() {
        authorizationRule.setUserAuthorized()
        sendInnPush(ANOTHER_USER_UID)

        onScreen<NotificationScreen> {
            hasNoNotifications()
        }

        pressBack()
    }

    @Test
    fun showCardsPush() {
        authorizationRule.setUserAuthorized()
        sendCardsPush(CURRENT_USER_UID)

        onScreen<NotificationScreen> {
            hasNotification(CARDS_PUSH_TITLE, CARDS_PUSH_BODY)
            clickOnNotification()
        }

        onScreen<PaymentCardsScreen> {
            waitUntil { contentView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<ServicesScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldNotShowCardsPushWhenUidDoesNotMatch() {
        authorizationRule.setUserAuthorized()
        sendCardsPush(ANOTHER_USER_UID)

        onScreen<NotificationScreen> {
            hasNoNotifications()
        }

        pressBack()
    }

    @Test
    fun showPaymentHistoryPush() {
        authorizationRule.setUserAuthorized()
        sendPaymentHistoryPush(CURRENT_USER_UID)

        onScreen<NotificationScreen> {
            hasNotification(PAYMENT_HISTORY_PUSH_TITLE, PAYMENT_HISTORY_PUSH_BODY)
            clickOnNotification()
        }

        onScreen<RentFlatScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<ServicesScreen> {
            servicesTitleItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldNotShowPaymentHistoryWhenUidDoesNotMatch() {
        authorizationRule.setUserAuthorized()
        sendPaymentHistoryPush(ANOTHER_USER_UID)

        onScreen<NotificationScreen> {
            hasNoNotifications()
        }

        pressBack()
    }

    @Test
    fun showDraftNeedToFinishPush() {
        configureWebServer {
            registerRentFlat("DRAFT")
        }

        authorizationRule.setUserAuthorized()
        sendDraftRequestPush()

        onScreen<NotificationScreen> {
            hasNotification(DRAFT_NEED_TO_FINISH_TITLE, DRAFT_NEED_TO_FINISH_BODY)
            clickOnNotification()
        }

        onScreen<RentFlatFormScreen> {
            waitUntil { contentView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<ServicesScreen> {
            servicesTitleItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldNotShowDraftScreenWhenConfirmed() {
        configureWebServer {
            registerRentFlat("CONFIRMED")
        }

        authorizationRule.setUserAuthorized()
        sendDraftRequestPush()

        onScreen<NotificationScreen> {
            hasNotification(DRAFT_NEED_TO_FINISH_TITLE, DRAFT_NEED_TO_FINISH_BODY)
            clickOnNotification()
        }

        onScreen<RentFlatScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<ServicesScreen> {
            servicesTitleItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldNotShowDraftScreenWhenFlatRequestError() {
        configureWebServer {
            registerRentFlatError()
        }

        authorizationRule.setUserAuthorized()
        sendDraftRequestPush()

        onScreen<NotificationScreen> {
            hasNotification(DRAFT_NEED_TO_FINISH_TITLE, DRAFT_NEED_TO_FINISH_BODY)
            clickOnNotification()
        }

        onScreen<RentFlatScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<ServicesScreen> {
            servicesTitleItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun showOwnerPassportPush() {
        authorizationRule.setUserAuthorized()
        sendOwnerPassportPush()

        onScreen<NotificationScreen> {
            hasNotification(OWNER_PASSPORT_TITLE, OWNER_PASSPORT_BODY)
            clickOnNotification()
        }
        onScreen<WebViewScreen> {
            val url = "https://arenda.test.vertis.yandex.ru/" +
                "lk/personal-data/edit?only-content=true"
            webView.waitUntil { isPageUrlEquals(url) }
        }
    }

    @Test
    fun showOwnerUtilitiesConfigurationPush() {
        authorizationRule.setUserAuthorized()
        sendOwnerUtilitiesConfigurationPush()

        onScreen<NotificationScreen> {
            hasNotification(OWNER_UTILITIES_CONFIGURATION_TITLE, OWNER_UTILITIES_CONFIGURATION_BODY)
            clickOnNotification()
        }
        onScreen<WebViewScreen> {
            val url = "https://arenda.test.vertis.yandex.ru/" +
                "lk/owner/flat/$FLAT_ID/house-services/settings?only-content=true"
            webView.waitUntil { isPageUrlEquals(url) }
        }
    }

    @Test
    fun showTenantCandidatesPush() {
        authorizationRule.setUserAuthorized()
        sendTenantCandidatesPush()

        onScreen<NotificationScreen> {
            hasNotification(TENANT_CANDIDATES_TITLE, TENANT_CANDIDATES_BODY)
            clickOnNotification()
        }
        onScreen<WebViewScreen> {
            val url = "https://arenda.test.vertis.yandex.ru/" +
                "lk/owner/flat/$FLAT_ID/tenant-candidates?only-content=true"
            webView.waitUntil { isPageUrlEquals(url) }
        }
    }

    @Test
    fun shouldShowMeterReadings() {
        configureWebServer {
            registerMeters()
        }

        authorizationRule.setUserAuthorized()

        sendMeterReadingsPush()

        onScreen<NotificationScreen> {
            hasNotification(METER_READINGS_TITLE, METER_READINGS_BODY)
            clickOnNotification()
        }

        onScreen<MeterReadingsListScreen> {
            waitUntil { toolbarTitleView.containsText("Данные") }
            pressBack()
        }

        onScreen<RentFlatScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<ServicesScreen> {
            servicesTitleItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun showUtilitiesBillPush() {
        configureWebServer {
            registerBill(jsonObject { "billStatus" to "SHOULD_BE_SENT" })
        }

        authorizationRule.setUserAuthorized()
        sendUtilitiesBillPush()

        onScreen<NotificationScreen> {
            hasNotification(UTILITIES_BILL_TITLE, UTILITIES_BILL_BODY)
            clickOnNotification()
        }

        onScreen<RentUtilitiesBillFormScreen> {
            waitUntil { listView.contains(addImagesButton) }
            pressBack()
        }

        onScreen<RentFlatScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<ServicesScreen> {
            servicesTitleItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun showUtilitiesDeclinedBillPush() {
        configureWebServer {
            registerBill(
                jsonObject {
                    "billStatus" to "DECLINED"
                    "bill" to jsonObject { }
                }
            )
        }

        authorizationRule.setUserAuthorized()
        sendUtilitiesDeclineBillPush()

        onScreen<NotificationScreen> {
            hasNotification(UTILITIES_DECLINED_BILL_TITLE, UTILITIES_DECLINED_BILL_BODY)
            clickOnNotification()
        }

        onScreen<RentUtilitiesBillFormScreen> {
            waitUntil { listView.contains(editDeclinedButton) }
            pressBack()
        }

        onScreen<RentFlatScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<ServicesScreen> {
            servicesTitleItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldShowReceiptsPush() {
        configureWebServer {
            registerPeriodWithInfo()
        }

        authorizationRule.setUserAuthorized()

        sendReceiptsPush()

        onScreen<NotificationScreen> {
            hasNotification(RECEIPTS_TITLE, RECEIPTS_BODY)
            clickOnNotification()
        }

        onScreen<OwnerUtilitiesImagesScreen> {
            waitUntil { toolbarTitleView.containsText("Данные") }
            pressBack()
        }

        onScreen<RentFlatScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<ServicesScreen> {
            servicesTitleItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldShowPaymentConfirmationPush() {
        configureWebServer {
            registerPeriodWithInfo()
        }

        authorizationRule.setUserAuthorized()

        sendPaymentConfirmationPush()

        onScreen<NotificationScreen> {
            hasNotification(PAYMENT_CONFIRMATION_TITLE, PAYMENT_CONFIRMATION_BODY)
            clickOnNotification()
        }

        onScreen<OwnerUtilitiesImagesScreen> {
            waitUntil { toolbarTitleView.containsText("Данные") }
            pressBack()
        }

        onScreen<RentFlatScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<ServicesScreen> {
            servicesTitleItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun showOwnerContractSigningPush() {
        authorizationRule.setUserAuthorized()

        sendOwnerContractSigningPush()

        onScreen<NotificationScreen> {
            hasNotification(CONTRACT_SIGNING_TITLE, CONTRACT_SIGNING_BODY)
            clickOnNotification()
        }

        onScreen<RentContractScreen> {
            waitUntil { contentView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<RentFlatScreen> {
            waitUntil { listView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<ServicesScreen> {
            servicesTitleItem.waitUntil { listView.contains(this) }
        }
    }

    private fun sendReceiptsPush() {
        PushBroadcastCommand.sendPush(
            action = "owner_house_service_receipts",
            params = jsonObject {
                "push_id" to "owner_house_service_receipts"
                "recipient_id" to CURRENT_USER_UID
                "title" to RECEIPTS_TITLE
                "body" to RECEIPTS_BODY
                "flat_id" to FLAT_ID
                "period_id" to PERIOD_ID
            }.toString(),
            name = "Жилец отправил фото квитанций"
        )
    }

    private fun sendPaymentConfirmationPush() {
        PushBroadcastCommand.sendPush(
            action = "owner_house_service_payment_confirmation",
            params = jsonObject {
                "push_id" to "owner_house_service_payment_confirmation"
                "recipient_id" to CURRENT_USER_UID
                "title" to PAYMENT_CONFIRMATION_TITLE
                "body" to PAYMENT_CONFIRMATION_BODY
                "flat_id" to FLAT_ID
                "period_id" to PERIOD_ID
            }.toString(),
            name = "Жилец отправил фото подтверждения оплаты"
        )
    }

    private fun sendDraftRequestPush() {
        PushBroadcastCommand.sendPush(
            action = "owner_draft_request",
            params = jsonObject {
                "push_id" to "owner_draft_need_to_finish"
                "recipient_id" to CURRENT_USER_UID
                "title" to DRAFT_NEED_TO_FINISH_TITLE
                "body" to DRAFT_NEED_TO_FINISH_BODY
                "flat_id" to FLAT_ID
            }.toString(),
            name = "Дозаполни анкету собственника"
        )
    }

    private fun sendOwnerPassportPush() {
        PushBroadcastCommand.sendPush(
            action = "owner_add_passport",
            params = jsonObject {
                "push_id" to "owner_add_passport"
                "recipient_id" to CURRENT_USER_UID
                "title" to OWNER_PASSPORT_TITLE
                "body" to OWNER_PASSPORT_BODY
            }.toString(),
            name = "Добавь паспортные данные"
        )
    }

    private fun sendOwnerUtilitiesConfigurationPush() {
        PushBroadcastCommand.sendPush(
            action = "owner_house_service_conditions_configuration",
            params = jsonObject {
                "push_id" to "owner_house_service_conditions_configuration"
                "recipient_id" to CURRENT_USER_UID
                "title" to OWNER_UTILITIES_CONFIGURATION_TITLE
                "body" to OWNER_UTILITIES_CONFIGURATION_BODY
                "flat_id" to FLAT_ID
            }.toString(),
            name = "Настрой условия по ЖКХ"
        )
    }

    private fun sendTenantCandidatesPush() {
        PushBroadcastCommand.sendPush(
            action = "owner_check_tenant_candidates",
            params = jsonObject {
                "push_id" to "owner_check_tenant_candidates"
                "recipient_id" to CURRENT_USER_UID
                "title" to TENANT_CANDIDATES_TITLE
                "body" to TENANT_CANDIDATES_BODY
                "flat_id" to FLAT_ID
            }.toString(),
            name = "Настрой условия по ЖКХ"
        )
    }

    private fun sendPayoutBrokenPush() {
        PushBroadcastCommand.sendPush(
            action = "open_rent_flat",
            params = jsonObject {
                "push_id" to "owner_rent_payout_broken"
                "flat_id" to FLAT_ID
                "recipient_id" to CURRENT_USER_UID
                "title" to PAYOUT_BROKEN_TITLE
                "body" to PAYOUT_BROKEN_BODY
            }.toString(),
            name = "Не можем перевести деньги"
        )
    }

    private fun sendInventoryFormPush() {
        PushBroadcastCommand.sendPush(
            action = "owner_inventory_form",
            params = jsonObject {
                "push_id" to "owner_inventory_need_to_fill_out"
                "flat_id" to FLAT_ID
                "owner_request_id" to OWNER_REQUEST_ID
                "recipient_id" to CURRENT_USER_UID
                "title" to FILL_INVENTORY_TITLE
                "body" to FILL_INVENTORY_BODY
            }.toString(),
            name = "Сформируйте опись"
        )
    }

    private fun sendInventoryPreviewPush() {
        PushBroadcastCommand.sendPush(
            action = "owner_inventory_preview",
            params = jsonObject {
                "push_id" to "owner_inventory_need_to_confirm"
                "flat_id" to FLAT_ID
                "owner_request_id" to OWNER_REQUEST_ID
                "recipient_id" to CURRENT_USER_UID
                "title" to SIGN_INVENTORY_TITLE
                "body" to SIGN_INVENTORY_BODY
            }.toString(),
            name = "Подпишите опись"
        )
    }

    private fun sendInnPush(recipientId: String) {
        PushBroadcastCommand.sendPush(
            action = "owner_rent_inn",
            params = jsonObject {
                "push_id" to "OWNER_RENT_WITHOUT_INN"
                "recipient_id" to recipientId
                "title" to INN_PUSH_TITLE
                "body" to INN_PUSH_BODY
            }.toString(),
            name = "Укажите ИНН"
        )
    }

    private fun sendCardsPush(recipientId: String) {
        PushBroadcastCommand.sendPush(
            action = "owner_rent_cards",
            params = jsonObject {
                "push_id" to "OWNER_RENT_WITHOUT_CARD"
                "recipient_id" to recipientId
                "title" to CARDS_PUSH_TITLE
                "body" to CARDS_PUSH_BODY
            }.toString(),
            name = "Привяжите карту"
        )
    }

    private fun sendPaymentHistoryPush(recipientId: String) {
        PushBroadcastCommand.sendPush(
            action = "rent_payment_history",
            params = jsonObject {
                "push_id" to "OWNER_RENT_PAID_OUT_TO_CARD"
                "recipient_id" to recipientId
                "flat_id" to FLAT_ID
                "title" to PAYMENT_HISTORY_PUSH_TITLE
                "body" to PAYMENT_HISTORY_PUSH_BODY
            }.toString(),
            name = "Жилец оплатил аренду"
        )
    }

    private fun sendMeterReadingsPush() {
        PushBroadcastCommand.sendPush(
            action = "owner_house_service_meter_readings",
            params = jsonObject {
                "push_id" to "owner_house_service_meter_readings_received"
                "recipient_id" to CURRENT_USER_UID
                "title" to METER_READINGS_TITLE
                "body" to METER_READINGS_BODY
                "flat_id" to FLAT_ID
                "period_id" to PERIOD_ID
            }.toString(),
            name = "Показания счётчиков"
        )
    }

    private fun sendUtilitiesBillPush() {
        PushBroadcastCommand.sendPush(
            action = "owner_house_service_bill",
            params = jsonObject {
                "push_id" to "owner_house_service_bill"
                "recipient_id" to CURRENT_USER_UID
                "title" to UTILITIES_BILL_TITLE
                "body" to UTILITIES_BILL_BODY
                "flat_id" to FLAT_ID
                "period_id" to PERIOD_ID
            }.toString(),
            name = "Счёт за ЖКХ"
        )
    }

    private fun sendUtilitiesDeclineBillPush() {
        PushBroadcastCommand.sendPush(
            action = "owner_house_service_bill_declined",
            params = jsonObject {
                "push_id" to "owner_house_service_bill_declined"
                "recipient_id" to CURRENT_USER_UID
                "title" to UTILITIES_DECLINED_BILL_TITLE
                "body" to UTILITIES_DECLINED_BILL_BODY
                "flat_id" to FLAT_ID
                "period_id" to PERIOD_ID
            }.toString(),
            name = "Отклоненный счёт за ЖКХ"
        )
    }

    private fun sendOwnerContractSigningPush() {
        PushBroadcastCommand.sendPush(
            action = "owner_rent_contract_signing",
            params = jsonObject {
                "push_id" to "owner_sign_rent_contract"
                "recipient_id" to CURRENT_USER_UID
                "title" to CONTRACT_SIGNING_TITLE
                "body" to CONTRACT_SIGNING_BODY
                "flat_id" to FLAT_ID
                "contract_id" to CONTRACT_ID
            }.toString(),
            name = "Подписание договора аренды собом"
        )
    }

    private fun DispatcherRegistry.registerInn(inn: String?) {
        register(
            request {
                path("2.0/rent/user/me")
                queryParam("withPersonalData", "true")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "user" to jsonObject {
                            "paymentData" to jsonObject {
                                inn?.let { "inn" to it }
                            }
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerInnError() {
        register(
            request {
                path("2.0/rent/user/me")
                queryParam("withPersonalData", "true")
            },
            response {
                error()
            }
        )
    }

    private fun DispatcherRegistry.registerRentFlat(status: String) {
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
                            "status" to status
                            "address" to jsonObject {
                                "address" to FLAT_ADDRESS
                            }
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerRentFlatError() {
        register(
            request {
                method("GET")
                path("2.0/rent/user/me/flats/$FLAT_ID")
            },
            error()
        )
    }

    private fun DispatcherRegistry.registerMeters() {
        register(
            request {
                path("2.0/rent/user/me/flats/$FLAT_ID/house-services/periods/$PERIOD_ID")
            },
            response {
                assetBody("meterReadings/metersList.json")
            }
        )
    }

    private fun DispatcherRegistry.registerBill(bill: JsonObject) {
        register(
            request {
                path("2.0/rent/user/me/flats/$FLAT_ID/house-services/periods/$PERIOD_ID")
            },
            response {
                jsonBody { "response" to bill }
            }
        )
    }

    private fun DispatcherRegistry.registerPeriodWithInfo() {
        register(
            request {
                path("2.0/rent/user/me/flats/$FLAT_ID/house-services/periods/$PERIOD_ID")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "period" to "2021-10"
                    }
                }
            }
        )
    }

    private companion object {

        const val CURRENT_USER_UID = "1"
        const val ANOTHER_USER_UID = "2"
        const val PERIOD_ID = "periodId00001"
        const val PAYOUT_BROKEN_TITLE = "Не можем перевести деньги"
        const val PAYOUT_BROKEN_BODY = "Совсем не можем"
        const val INN_PUSH_TITLE = "Введите ИНН"
        const val INN_PUSH_BODY = "Это нужно, чтобы получать деньги за аренду"
        const val CARDS_PUSH_TITLE = "Привяжите карту"
        const val CARDS_PUSH_BODY = "Перевод на счёт может занять много времени"
        const val PAYMENT_HISTORY_PUSH_TITLE = "Жилец оплатил аренду"
        const val PAYMENT_HISTORY_PUSH_BODY = "Деньги скоро будут у вас"
        const val DRAFT_NEED_TO_FINISH_TITLE = "Анкета почти готова"
        const val DRAFT_NEED_TO_FINISH_BODY =
            "Осталось заполнить её до конца, чтобы мы приняли вашу заявку на сдачу квартиры"
        const val OWNER_PASSPORT_TITLE = "Укажите паспортные данные"
        const val OWNER_PASSPORT_BODY =
            "Это поможет заранее подготовить документы и сэкономить время на встрече"
        const val OWNER_UTILITIES_CONFIGURATION_TITLE = "Настройте раздел ЖКХ"
        const val OWNER_UTILITIES_CONFIGURATION_BODY = "Это нужно, чтобы подписать договор аренды"
        const val TENANT_CANDIDATES_TITLE = "Новые кандидаты"
        const val TENANT_CANDIDATES_BODY =
            "Вашей квартирой интересуются. Вы можете посмотреть анкеты и выбрать жильцов."
        const val METER_READINGS_TITLE = "Новое по коммуналке"
        const val METER_READINGS_BODY =
            "Жилец отправил новые показания счётчиков за январь"
        const val UTILITIES_BILL_TITLE = "Счёт на оплату"
        const val UTILITIES_BILL_BODY = "Пора выставлять счёт на оплату"
        const val UTILITIES_DECLINED_BILL_TITLE = "Жилец не согласен со счётом"
        const val UTILITIES_DECLINED_BILL_BODY = "Узнать причину"
        const val RECEIPTS_TITLE = "Новое по коммуналке"
        const val RECEIPTS_BODY = "Жилец отправил квитанции за январь"
        const val PAYMENT_CONFIRMATION_TITLE = "Новое по коммуналке"
        const val PAYMENT_CONFIRMATION_BODY = "Вы можете посмотреть подтверждение оплаты за январь"
        const val CONTRACT_SIGNING_TITLE = "Договор готов"
        const val CONTRACT_SIGNING_BODY = "Ознакомьтесь с условиями и подпишите договор"
        const val FILL_INVENTORY_TITLE = "Создайте опись имущества"
        const val FILL_INVENTORY_BODY = "Чтобы зафиксировать состояние квартиры. Самостоятельно " +
            "— надёжнее всего, ведь никто не знает квартиру лучше вас. " +
            "Бонус: это сэкономит время на встрече."
        const val SIGN_INVENTORY_TITLE = "Опись имущества"
        const val SIGN_INVENTORY_BODY = "Она нужна, чтобы зафиксировать состояние квартиры. " +
            "Пригодится, если в квартире будет что-то меняться. " +
            "Пожалуйста, ознакомьтесь с ней и подпишите."
    }
}
