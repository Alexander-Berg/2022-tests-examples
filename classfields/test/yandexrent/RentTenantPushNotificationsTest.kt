package com.yandex.mobile.realty.test.yandexrent

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.core.PushBroadcastCommand
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.InventoryPreviewScreen
import com.yandex.mobile.realty.core.screen.MeterReadingsListScreen
import com.yandex.mobile.realty.core.screen.NotificationScreen
import com.yandex.mobile.realty.core.screen.RentContractScreen
import com.yandex.mobile.realty.core.screen.RentFlatScreen
import com.yandex.mobile.realty.core.screen.RentShowingsScreen
import com.yandex.mobile.realty.core.screen.RentUtilitiesBillInfoScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.ServicesScreen
import com.yandex.mobile.realty.core.screen.TenantUtilitiesImagesScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.jsonBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.CONTRACT_ID
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 30.12.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class RentTenantPushNotificationsTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule
    )

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
    fun shouldShowUtilitiesConditions() {
        authorizationRule.setUserAuthorized()

        sendRentUtilitiesConditionsPush()

        onScreen<NotificationScreen> {
            hasNotification(UTILITIES_CONDITIONS_TITLE, UTILITIES_CONDITIONS_BODY)
            clickOnNotification()
        }

        onScreen<WebViewScreen> {
            val url = "https://arenda.test.vertis.yandex.ru/" +
                "lk/tenant/flat/$FLAT_ID/house-services/settings/confirmation?only-content=true"
            webView.waitUntil { isPageUrlEquals(url) }
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
    fun shouldShowUtilitiesBillInfo() {
        configureWebServer {
            registerUtilitiesPayment()
            registerPeriodWithInfo()
        }

        authorizationRule.setUserAuthorized()

        sendUtilitiesBillInfoPush()

        onScreen<NotificationScreen> {
            hasNotification(BILL_INFO_TITLE, BILL_INFO_BODY)
            clickOnNotification()
        }

        onScreen<RentUtilitiesBillInfoScreen> {
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

    @Test
    fun shouldShowReceiptsImages() {
        configureWebServer {
            registerPeriodWithInfo()
        }

        authorizationRule.setUserAuthorized()

        sendReceiptsPush()

        onScreen<NotificationScreen> {
            hasNotification(RECEIPTS_TITLE, RECEIPTS_BODY)
            clickOnNotification()
        }

        onScreen<TenantUtilitiesImagesScreen> {
            waitUntil { toolbarTitleView.isTextEquals("Данные за\u00A0октябрь") }
            root.isViewStateMatches(getTestRelatedFilePath("screen"))
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
    fun shouldShowPaymentConfirmationImages() {
        configureWebServer {
            registerPeriodWithInfo()
        }

        authorizationRule.setUserAuthorized()

        sendPaymentConfirmationPush()

        onScreen<NotificationScreen> {
            hasNotification(PAYMENT_CONFIRMATION_TITLE, PAYMENT_CONFIRMATION_BODY)
            clickOnNotification()
        }

        onScreen<TenantUtilitiesImagesScreen> {
            waitUntil { toolbarTitleView.isTextEquals("Данные за\u00A0октябрь") }
            root.isViewStateMatches(getTestRelatedFilePath("screen"))
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
    fun showContractSigningPush() {
        authorizationRule.setUserAuthorized()

        sendTenantContractSigningPush()

        onScreen<NotificationScreen> {
            hasNotification(CONTRACT_SIGNING_TITLE, CONTRACT_SIGNING_BODY)
            clickOnNotification()
        }

        onScreen<RentContractScreen> {
            waitUntil { contentView.isCompletelyDisplayed() }
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

    private fun sendInventoryPreviewPush() {
        PushBroadcastCommand.sendPush(
            action = "tenant_inventory_preview",
            params = jsonObject {
                "push_id" to "tenant_inventory_need_to_confirm"
                "flat_id" to FLAT_ID
                "owner_request_id" to OWNER_REQUEST_ID
                "recipient_id" to CURRENT_USER_UID
                "title" to SIGN_INVENTORY_TITLE
                "body" to SIGN_INVENTORY_BODY
            }.toString(),
            name = "Подпишите опись"
        )
    }

    private fun sendRentUtilitiesConditionsPush() {
        PushBroadcastCommand.sendPush(
            action = "tenant_house_service_conditions",
            params = jsonObject {
                "push_id" to "tenant_house_service_conditions"
                "recipient_id" to CURRENT_USER_UID
                "title" to UTILITIES_CONDITIONS_TITLE
                "body" to UTILITIES_CONDITIONS_BODY
                "flat_id" to FLAT_ID
            }.toString(),
            name = "Условия ЖКХ"
        )
    }

    private fun sendMeterReadingsPush() {
        PushBroadcastCommand.sendPush(
            action = "tenant_house_service_meter_readings",
            params = jsonObject {
                "push_id" to "tenant_house_service_send_meter_readings"
                "recipient_id" to CURRENT_USER_UID
                "title" to METER_READINGS_TITLE
                "body" to METER_READINGS_BODY
                "flat_id" to FLAT_ID
                "period_id" to PERIOD_ID
            }.toString(),
            name = "Показания счётчиков"
        )
    }

    private fun sendUtilitiesBillInfoPush() {
        PushBroadcastCommand.sendPush(
            action = "tenant_house_service_bill",
            params = jsonObject {
                "push_id" to "tenant_rent_house_service_bill_received"
                "recipient_id" to CURRENT_USER_UID
                "title" to BILL_INFO_TITLE
                "body" to BILL_INFO_BODY
                "flat_id" to FLAT_ID
                "period_id" to PERIOD_ID
            }.toString(),
            name = "Счёт на оплату ЖКХ"
        )
    }

    private fun sendReceiptsPush() {
        PushBroadcastCommand.sendPush(
            action = "tenant_house_service_receipts",
            params = jsonObject {
                "push_id" to "tenant_house_service_send_receipts"
                "recipient_id" to CURRENT_USER_UID
                "title" to RECEIPTS_TITLE
                "body" to RECEIPTS_BODY
                "flat_id" to FLAT_ID
                "period_id" to PERIOD_ID
            }.toString(),
            name = "Квитанции"
        )
    }

    private fun sendPaymentConfirmationPush() {
        PushBroadcastCommand.sendPush(
            action = "tenant_house_service_payment_confirmation",
            params = jsonObject {
                "push_id" to "tenant_house_service_send_payment_confirmation"
                "recipient_id" to CURRENT_USER_UID
                "title" to PAYMENT_CONFIRMATION_TITLE
                "body" to PAYMENT_CONFIRMATION_BODY
                "flat_id" to FLAT_ID
                "period_id" to PERIOD_ID
            }.toString(),
            name = "Подтверждение оплаты"
        )
    }

    private fun sendTenantContractSigningPush() {
        PushBroadcastCommand.sendPush(
            action = "tenant_rent_contract_signing",
            params = jsonObject {
                "push_id" to "tenant_sign_rent_contract"
                "recipient_id" to CURRENT_USER_UID
                "title" to CONTRACT_SIGNING_TITLE
                "body" to CONTRACT_SIGNING_BODY
                "flat_id" to FLAT_ID
                "contract_id" to CONTRACT_ID
            }.toString(),
            name = "Подписание договора аренды жильцом"
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

    private fun DispatcherRegistry.registerPeriodWithInfo() {
        register(
            request {
                path("2.0/rent/user/me/flats/$FLAT_ID/house-services/periods/$PERIOD_ID")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "period" to "2021-10"
                        "billStatus" to "SHOULD_BE_PAID"
                        "bill" to jsonObject {
                            "amount" to 2000
                        }
                    }
                }
            }
        )
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
                            "id" to "paymentId0001"
                        }
                    }
                }
            }
        )
    }

    private companion object {
        const val CURRENT_USER_UID = "1"
        const val PERIOD_ID = "periodId00001"
        const val UTILITIES_CONDITIONS_TITLE = "Условия по ЖКХ"
        const val UTILITIES_CONDITIONS_BODY =
            "Ознакомьтесь с условиями по ЖКХ перед тем, как подписать договор"
        const val METER_READINGS_TITLE = "Этот день настал!"
        const val METER_READINGS_BODY =
            "Решили напомнить: пришло время отправлять показания счётчиков"
        const val BILL_INFO_TITLE = "Счёт готов!"
        const val BILL_INFO_BODY = "Можно оплатить"
        const val RECEIPTS_TITLE = "Отправим квитанции собственнику?"
        const val RECEIPTS_BODY = "Новый квест — отправить квитанции"
        const val PAYMENT_CONFIRMATION_TITLE = "Подтверждение оплаты"
        const val PAYMENT_CONFIRMATION_BODY =
            "Самое время отправить подтверждение оплаты собственнику"
        const val SIGN_INVENTORY_TITLE = "Опись имущества"
        const val SIGN_INVENTORY_BODY = "Она нужна, чтобы зафиксировать состояние квартиры. " +
            "Пожалуйста, проверьте её, и если всё хорошо — подпишите."
        const val CONTRACT_SIGNING_TITLE = "Договор готов"
        const val CONTRACT_SIGNING_BODY = "Ознакомьтесь с условиями и подпишите договор"
    }
}
