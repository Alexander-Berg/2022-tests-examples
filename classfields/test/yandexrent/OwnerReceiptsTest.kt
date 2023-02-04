package com.yandex.mobile.realty.test.yandexrent

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.core.webserver.RequestMatcher
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.test.services.FLAT_ID
import org.junit.Test

/**
 * @author misha-kozlov on 06.02.2022
 */
@LargeTest
class OwnerReceiptsTest : OwnerUtilitiesImagesTest() {

    @Test
    fun shouldShowImages() {
        shouldShowImages(
            notificationKey = NOTIFICATION_KEY,
            notificationTitle = NOTIFICATION_TITLE,
            registerPeriod = { registerPeriod(receipts = getImages(1)) }
        )
    }

    @Test
    fun shouldDeclineImages() {
        shouldDeclineImages(
            notificationKey = NOTIFICATION_KEY,
            notificationTitle = NOTIFICATION_TITLE,
            registerPeriod = { registerPeriod(receipts = getImages(2)) },
            declineRequest = declineRequest()
        )
    }

    @Test
    fun shouldShowDeclineNetworkError() {
        shouldShowNetworkErrorToast(
            notificationKey = NOTIFICATION_KEY,
            notificationTitle = NOTIFICATION_TITLE,
            registerPeriod = { registerPeriod(receipts = getImages(2)) },
        )
    }

    @Test
    fun shouldShowDeclineConflictError() {
        shouldShowConflictErrorToast(
            notificationKey = NOTIFICATION_KEY,
            notificationTitle = NOTIFICATION_TITLE,
            registerPeriod = { registerPeriod(receipts = getImages(2)) },
            declineRequest = declineRequest()
        )
    }

    private fun declineRequest(): RequestMatcher {
        return request {
            method("PUT")
            val path = "2.0/rent/user/me/flats/$FLAT_ID/" +
                "house-services/periods/$PERIOD_ID/receipts/decline"
            path(path)
            jsonBody {
                "reasonForDecline" to DECLINE_REASON
            }
        }
    }

    companion object {

        private const val NOTIFICATION_KEY = "houseServiceReceiptsReceived"
        private const val NOTIFICATION_TITLE =
            "Жилец отправил квитанции с\u00A0показаниями счётчиков за\u00A0октябрь"
    }
}
