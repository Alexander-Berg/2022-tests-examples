package com.yandex.mobile.realty.test.yandexrent

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.gson.JsonObject
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.InternetRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.MeterEmptyReadingsScreen
import com.yandex.mobile.realty.core.screen.MeterReadingsListScreen
import com.yandex.mobile.realty.core.screen.OwnerMeterReadingsScreen
import com.yandex.mobile.realty.core.screen.RentFlatScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.error
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.test.services.registerOwnerRentFlat
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author sorokinandrei on 2/7/22.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class OwnerMeterReadingsListTest : BaseTest() {

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
            registerOwnerRentFlat(
                notification = metersNotification("houseServiceReceivedMeterReadings")
            )
            registerMeters("metersListPartialSent.json")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(NEW_NOTIFICATION_TITLE)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("metersReceivedNotification"))
                .invoke { actionButton.click() }
        }

        onScreen<MeterReadingsListScreen> {
            waitUntil { toolbarTitleView.containsText(TITLE) }
            listView.isContentStateMatches(getTestRelatedFilePath("metersPartialSentList"))
        }
    }

    @Test
    fun shouldOpenMeterReadingListFromReceivedAllNotification() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = metersNotification("houseServiceReceivedAllMeterReadings")
            )
            registerMeters("metersListAllSent.json")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(ALL_NOTIFICATION_TITLE)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("metersAllReceivedNotification"))
                .invoke { actionButton.click() }
        }

        onScreen<MeterReadingsListScreen> {
            waitUntil { toolbarTitleView.containsText(TITLE) }
            listView.isContentStateMatches(getTestRelatedFilePath("metersListAllReceived"))
        }
    }

    @Test
    fun shouldOpenMeterReadingListNotSent() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = metersNotification("houseServiceReceivedMeterReadings")
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
    fun shouldOpenMeterReadingListDeclined() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = metersNotification("houseServiceReceivedMeterReadings")
            )
            registerMeters("metersList.json")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openMeterList()

        onScreen<MeterReadingsListScreen> {
            waitUntil { toolbarTitleView.containsText(TITLE) }
            listView.isContentStateMatches(getTestRelatedFilePath("metersListDeclined"))
        }
    }

    @Test
    fun shouldOpenMeterReadingListExpired() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = metersNotification("houseServiceReceivedMeterReadings")
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
            registerOwnerRentFlat(
                notification = metersNotification("houseServiceReceivedMeterReadings")
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
    fun shouldShowErrors() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = metersNotification("houseServiceReceivedMeterReadings")
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

    @Test
    fun shouldOpenMeterReadingFormAndShowStatusAlerts() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = metersNotification("houseServiceReceivedMeterReadings")
            )
            registerMeters("metersList.json")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openMeterList()

        listOf(
            METER_SENDING_ID to "meterSending",
            METER_SENT_ID to "meterSent",
            METER_DECLINED_ID to "meterDeclined",
        ).forEach { (meterId, screenshotKey) ->
            onScreen<MeterReadingsListScreen> {
                waitUntil { toolbarTitleView.containsText(TITLE) }

                listView.scrollTo(meterItem(meterId))
                    .click()
            }

            onScreen<OwnerMeterReadingsScreen> {
                readingImageItem(IMAGE_URL_1)
                    .waitUntil { listView.contains(this) }
                listView.isContentStateMatches(getTestRelatedFilePath(screenshotKey))
                pressBack()
            }
        }
    }

    @Test
    fun shouldOpenMeterWithoutReadingsDialog() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = metersNotification("houseServiceReceivedMeterReadings")
            )
            registerMeters("metersList.json")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openMeterList()

        listOf(
            METER_SHOULD_BE_SENT_ID,
            METER_EXPIRED_ID,
            METER_NOT_SENT_ID
        ).forEach { meterId ->
            onScreen<MeterReadingsListScreen> {
                listView.scrollTo(meterItem(meterId))
                    .click()

                onScreen<MeterEmptyReadingsScreen> {
                    waitUntil { contentView.isCompletelyDisplayed() }
                    root.isViewStateMatches(getTestRelatedFilePath("emptyReadings"))

                    okButton.click()
                    waitUntil { contentView.doesNotExist() }
                }
            }
        }
    }

    private fun openMeterList() {
        onScreen<RentFlatScreen> {
            notificationItem(NEW_NOTIFICATION_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
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
        const val NEW_NOTIFICATION_TITLE =
            "Новые показания счётчиков за\u00A0октябрь уже у\u00A0нас"
        const val ALL_NOTIFICATION_TITLE =
            "Показания по\u00A0всем счётчикам за\u00A0октябрь уже тут"

        const val METER_SENDING_ID = "1"
        const val METER_SHOULD_BE_SENT_ID = "3"
        const val METER_SENT_ID = "2"
        const val METER_EXPIRED_ID = "5"
        const val METER_DECLINED_ID = "6"
        const val METER_NOT_SENT_ID = "7"
        const val IMAGE_URL_1 = "https://mockImages/meter1.webp"
    }
}
