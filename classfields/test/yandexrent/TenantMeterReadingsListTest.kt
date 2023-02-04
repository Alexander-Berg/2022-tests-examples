package com.yandex.mobile.realty.test.yandexrent

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.gson.JsonObject
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.InternetRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.*
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.view.TRecyclerItem
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.test.services.registerTenantRentFlat
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author sorokinandrei on 10/21/21.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class TenantMeterReadingsListTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = RentFlatActivityTestRule(
        flatId = FLAT_ID,
        launchActivity = false
    )
    private val internetRule = InternetRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        internetRule
    )

    @Test
    fun shouldOpenMeterReadingListFromNotification() {
        configureWebServer {
            registerTenantRentFlat(
                notification = metersNotification("houseServiceSendMeterReadings")
            )
            registerMeters("metersList.json")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_send_meter_readings_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("metersNotification"))
                .invoke { actionButton.click() }
        }

        onScreen<MeterReadingsListScreen> {
            waitUntil { toolbarTitleView.containsText(TITLE) }
            listView.isContentStateMatches(getTestRelatedFilePath("metersListDeclined"))
        }
    }

    @Test
    fun shouldOpenMeterReadingListFromDeclinedNotification() {
        configureWebServer {
            registerTenantRentFlat(
                notification = metersNotification("houseServiceMeterReadingsDeclined")
            )
            registerMeters("metersList.json")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title =
                getResourceString(R.string.yandex_rent_tenant_meter_readings_declined_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("metersDeclinedNotification"))
                .invoke { actionButton.click() }
        }

        onScreen<MeterReadingsListScreen> {
            waitUntil { toolbarTitleView.containsText(TITLE) }
        }
    }

    @Test
    fun shouldOpenMeterReadingFormAndShowStatusAlerts() {
        configureWebServer {
            registerTenantRentFlat(
                notification = metersNotification("houseServiceSendMeterReadings")
            )
            registerMeters("metersList.json")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openMeterList()

        checkMeterFormState(
            id = METER_SHOULD_BE_SENT_ID,
            formItemProvider = { submitButtonItem },
            screenshotKey = "meterFormShouldBeSentTriple"
        )
        checkMeterFormState(
            id = METER_SENDING_ID,
            formItemProvider = { editButtonItem },
            screenshotKey = "meterFormSending"
        )
        checkMeterFormState(
            id = METER_SENT_ID,
            formItemProvider = { readingViewItem(METER_READING_TEXT) },
            screenshotKey = "meterFormSent"
        )
        checkMeterFormState(
            id = METER_EXPIRED_ID,
            formItemProvider = { submitButtonItem },
            screenshotKey = "meterFormExpired"
        )
        checkMeterFormState(
            id = METER_DECLINED_ID,
            formItemProvider = { editButtonItem },
            screenshotKey = "meterFormDeclined"
        )

        onScreen<MeterReadingsListScreen> {
            listView.scrollTo(meterItem(METER_NOT_SENT_ID))
                .click()

            onScreen<MeterDateNotDueScreen> {
                root.isViewStateMatches(getTestRelatedFilePath("dateNotDue"))
                okButton.click()
            }

            listView.scrollTo(meterItem(METER_NOT_SENT_ID))
                .click()

            onScreen<MeterDateNotDueScreen> {
                openFormButton.click()
            }
        }

        onScreen<MeterReadingsFormScreen> {
            listView.isContentStateMatches(getTestRelatedFilePath("meterNotSent"))
        }
    }

    @Test
    fun shouldOpenMeterReadingListNotSent() {
        configureWebServer {
            registerTenantRentFlat(
                notification = metersNotification("houseServiceSendMeterReadings")
            )
            registerMeters("metersListNotSent.json")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openMeterList()

        onScreen<MeterReadingsListScreen> {
            waitUntil { toolbarTitleView.containsText(TITLE) }
            listView.isContentStateMatches(getTestRelatedFilePath("metersListNotSent"))
        }
    }

    @Test
    fun shouldOpenMeterReadingListExpired() {
        configureWebServer {
            registerTenantRentFlat(
                notification = metersNotification("houseServiceSendMeterReadings")
            )
            registerMeters("metersListExpired.json")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openMeterList()

        onScreen<MeterReadingsListScreen> {
            waitUntil { toolbarTitleView.containsText(TITLE) }
            listView.isContentStateMatches(getTestRelatedFilePath("metersListExpired"))
        }
    }

    @Test
    fun shouldOpenMeterReadingListShouldBeSent() {
        configureWebServer {
            registerTenantRentFlat(
                notification = metersNotification("houseServiceSendMeterReadings")
            )
            registerMeters("metersListShouldBeSent.json")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openMeterList()

        onScreen<MeterReadingsListScreen> {
            waitUntil { toolbarTitleView.containsText(TITLE) }
            listView.isContentStateMatches(getTestRelatedFilePath("metersListShouldBeSent"))
        }
    }

    @Test
    fun shouldOpenMeterReadingListSending() {
        configureWebServer {
            registerTenantRentFlat(
                notification = metersNotification("houseServiceSendMeterReadings")
            )
            registerMeters("metersListSending.json")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openMeterList()

        onScreen<MeterReadingsListScreen> {
            waitUntil { toolbarTitleView.containsText(TITLE) }
            listView.isContentStateMatches(getTestRelatedFilePath("metersListSending"))
        }
    }

    @Test
    fun shouldShowErrors() {
        configureWebServer {
            registerTenantRentFlat(
                notification = metersNotification("houseServiceSendMeterReadings")
            )
            registerMeters("metersList.json")
            registerMetersError()
            registerMeters("metersList.json")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        internetRule.turnOff()
        openMeterList()

        onScreen<MeterReadingsListScreen> {
            fullscreenErrorView.waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(getTestRelatedFilePath("networkError"))
                .also { internetRule.turnOn() }
                .invoke { retryButton.click() }

            meterItem(METER_SENDING_ID)
                .waitUntil { listView.contains(this) }

            pressBack()
        }

        openMeterList()

        onScreen<MeterReadingsListScreen> {
            fullscreenErrorView.waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(getTestRelatedFilePath("serverError"))
                .invoke { retryButton.click() }

            meterItem(METER_SENDING_ID)
                .waitUntil { listView.contains(this) }
        }
    }

    private fun openMeterList() {
        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_send_meter_readings_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }
    }

    private fun checkMeterFormState(
        id: String,
        formItemProvider: MeterReadingsFormScreen.() -> TRecyclerItem<*>,
        screenshotKey: String,
    ) {
        onScreen<MeterReadingsListScreen> {
            waitUntil { toolbarTitleView.containsText(TITLE) }

            listView.scrollTo(meterItem(id))
                .click()
        }

        onScreen<MeterReadingsFormScreen> {
            val formItem = formItemProvider.invoke(this)
            waitUntil { listView.contains(formItem) }
            isContentStateMatches(getTestRelatedFilePath(screenshotKey))
            pressBack()
        }
    }

    private fun DispatcherRegistry.registerMeters(responseFile: String) {
        register(
            request {
                path("2.0/rent/user/me/flats/$FLAT_ID/house-services/periods/$PERIOD_ID")
            },
            response {
                assetBody("meterReadings/$responseFile")
            }
        )
    }

    private fun DispatcherRegistry.registerMetersError() {
        register(
            request {
                path("2.0/rent/user/me/flats/$FLAT_ID/house-services/periods/$PERIOD_ID")
            },
            error()
        )
    }

    private fun metersNotification(notification: String): JsonObject {
        return jsonObject {
            notification to jsonObject {
                "flatId" to FLAT_ID
                "periodId" to PERIOD_ID
                "period" to PERIOD
            }
        }
    }

    private companion object {

        const val PERIOD = "2021-10"
        const val PERIOD_ID = "periodId00001"
        const val TITLE = "Данные"

        const val METER_SENDING_ID = "1"
        const val METER_SENT_ID = "2"
        const val METER_SHOULD_BE_SENT_ID = "3"
        const val METER_EXPIRED_ID = "5"
        const val METER_DECLINED_ID = "6"
        const val METER_NOT_SENT_ID = "7"
        const val METER_READING_TEXT = "1\u00A0110"
    }
}
