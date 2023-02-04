package com.yandex.mobile.realty.test.yandexrent

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.gson.JsonObject
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.GalleryScreen
import com.yandex.mobile.realty.core.screen.MeterReadingsListScreen
import com.yandex.mobile.realty.core.screen.OwnerMeterReadingsScreen
import com.yandex.mobile.realty.core.screen.RentFlatScreen
import com.yandex.mobile.realty.core.screen.RentUtilitiesDeclineScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.jsonBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.core.webserver.success
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
class OwnerMeterReadingsTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = RentFlatActivityTestRule(
        flatId = FLAT_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule
    )

    @Test
    fun shouldDeclineReadings() {
        val meterId = METER_DOUBLE_ID
        configureWebServer {
            registerOwnerRentFlat(notification = metersNotification())
            registerMeters("metersListAllSent.json")
            registerDeclineMeterReadingsError()
            registerDeclineMeterReadings()
            registerMeters("metersListAllDeclined.json")
            registerOwnerRentFlat()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openMeterForm(meterId)

        onScreen<OwnerMeterReadingsScreen> {
            declineButtonItem.waitUntil { listView.contains(this) }.click()
        }

        onScreen<RentUtilitiesDeclineScreen> {
            messageView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(DECLINE_REASON)
            declineButton.click()

            toastView(SERVER_ERROR_MESSAGE).waitUntil { isCompletelyDisplayed() }
            declineButton.click()

            waitUntil { successView.isCompletelyDisplayed() }
            isViewStateMatches(getTestRelatedFilePath("success"))

            successButton.click()
        }

        onScreen<MeterReadingsListScreen> {
            waitUntil { toolbarTitleView.containsText(TITLE) }

            meterItem(meterId)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("declinedMeter"))
        }
    }

    @Test
    fun shouldOpenImagesInFullscreen() {
        val meterId = METER_TRIPLE_ID
        configureWebServer {
            registerOwnerRentFlat(notification = metersNotification())
            registerMeters("metersListSentInitialReadings.json")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openMeterForm(meterId)

        onScreen<OwnerMeterReadingsScreen> {
            waitUntil { toolbarImageView.isCompletelyDisplayed() }
            toolbarImageView.click()
        }

        onScreen<GalleryScreen> {
            waitUntil { photoView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<OwnerMeterReadingsScreen> {
            readingImageItem(IMAGE_URL_3)
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<GalleryScreen> {
            waitUntil { photoView.isCompletelyDisplayed() }
        }
    }

    private fun openMeterForm(meterId: String) {
        onScreen<RentFlatScreen> {
            notificationItem(NOTIFICATION_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<MeterReadingsListScreen> {
            waitUntil { toolbarTitleView.containsText(TITLE) }

            meterItem(meterId)
                .waitUntil { listView.contains(this) }
                .click()
        }
    }

    private fun DispatcherRegistry.registerMeters(responseFile: String) {
        register(
            request {
                method("GET")
                path("2.0/rent/user/me/flats/$FLAT_ID/house-services/periods/$PERIOD_ID")
            },
            response {
                assetBody("meterReadings/$responseFile")
            }
        )
    }

    private fun metersNotification(): JsonObject {
        return jsonObject {
            "houseServiceReceivedMeterReadings" to jsonObject {
                "flatId" to FLAT_ID
                "periodId" to PERIOD_ID
                "period" to PERIOD
            }
        }
    }

    private fun DispatcherRegistry.registerDeclineMeterReadings() {
        register(
            request {
                method("PUT")
                val path = "2.0/rent/user/me/flats/$FLAT_ID/" +
                    "house-services/periods/$PERIOD_ID/$METER_DOUBLE_ID/decline"
                path(path)
                jsonBody {
                    "reasonForDecline" to DECLINE_REASON
                }
            },
            success()
        )
    }

    private fun DispatcherRegistry.registerDeclineMeterReadingsError() {
        register(
            request {
                method("PUT")
                val path = "2.0/rent/user/me/flats/$FLAT_ID/" +
                    "house-services/periods/$PERIOD_ID/$METER_DOUBLE_ID/decline"
                path(path)
                jsonBody {
                    "reasonForDecline" to DECLINE_REASON
                }
            },
            response {
                setResponseCode(400)
                jsonBody {
                    "error" to jsonObject {
                        "code" to "CONFLICT"
                        "message" to SERVER_ERROR_MESSAGE
                    }
                }
            }
        )
    }

    private companion object {

        const val METER_DOUBLE_ID = "1"
        const val METER_TRIPLE_ID = "3"
        const val NOTIFICATION_TITLE =
            "Новые показания счётчиков за\u00A0октябрь уже у\u00A0нас"
        const val PERIOD = "2021-10"
        const val PERIOD_ID = "periodId00001"
        const val TITLE = "Данные"
        const val IMAGE_URL_3 = "https://mockImages/meter3.webp"

        const val DECLINE_REASON = "some text"
        const val SERVER_ERROR_MESSAGE = "Произошла ошибка при отклонении"
    }
}
