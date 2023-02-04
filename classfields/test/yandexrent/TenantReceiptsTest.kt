package com.yandex.mobile.realty.test.yandexrent

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Test

/**
 * @author misha-kozlov on 24.10.2021
 */
@LargeTest
class TenantReceiptsTest : TenantUtilitiesImagesTest() {

    @Test
    fun shouldSendImages() {
        shouldSendImages(
            notificationKey = "houseServiceTimeToSendReceipts",
            notificationTitle = SEND_RECEIPTS_TITLE,
            registerUploadPhotoError = { registerUploadPhotoError("receipt") },
            registerUploadPhoto = { registerUploadPhoto("receipt") },
            registerSubmitRequest = { registerSendReceipts() }
        )
    }

    @Test
    fun shouldOpenFullscreenImage() {
        shouldOpenFullscreenImage(
            notificationKey = "houseServiceTimeToSendReceipts",
            notificationTitle = SEND_RECEIPTS_TITLE,
            registerUploadPhoto = { registerUploadPhoto("receipt") }
        )
    }

    @Test
    fun shouldEditDeclinedImages() {
        shouldEditDeclinedImages(
            notificationKey = "houseServiceReceiptsDeclined",
            notificationTitleRes = R.string.yandex_rent_tenant_receipts_declined_title,
            registerMeters = { registerPeriod(receipts = getDeclinedImages()) },
            registerUploadPhoto = { registerUploadPhoto("receipt") },
            registerSubmitRequest = { registerSendReceipts() }
        )
    }

    @Test
    fun shouldShowInternetErrorToast() {
        shouldShowInternetErrorToast(
            notificationKey = "houseServiceTimeToSendReceipts",
            notificationTitle = SEND_RECEIPTS_TITLE,
            imageEntityKey = "receipt"
        )
    }

    @Test
    fun shouldShowNetworkErrorToast() {
        shouldShowNetworkErrorToast(
            notificationKey = "houseServiceTimeToSendReceipts",
            notificationTitle = SEND_RECEIPTS_TITLE,
            imageEntityKey = "receipt"
        )
    }

    @Test
    fun shouldShowConflictErrorToast() {
        shouldShowSpecificErrorToast(
            notificationKey = "houseServiceTimeToSendReceipts",
            notificationTitle = SEND_RECEIPTS_TITLE,
            imageEntityKey = "receipt",
            registerError = { registerConflictError() }
        )
    }

    @Test
    fun shouldShowValidationErrorToast() {
        shouldShowSpecificErrorToast(
            notificationKey = "houseServiceTimeToSendReceipts",
            notificationTitle = SEND_RECEIPTS_TITLE,
            imageEntityKey = "receipt",
            registerError = { registerValidationError() }
        )
    }

    @Test
    fun shouldShowExitConfirmationDialog() {
        shouldShowExitConfirmationDialog(
            notificationKey = "houseServiceTimeToSendReceipts",
            notificationTitle = SEND_RECEIPTS_TITLE,
            registerUploadPhoto = { registerUploadPhoto("receipt") }
        )
    }

    private fun DispatcherRegistry.registerSendReceipts() {
        register(submitImageRequest(), success())
    }

    private fun DispatcherRegistry.registerConflictError() {
        register(submitImageRequest(), conflictError())
    }

    private fun DispatcherRegistry.registerValidationError() {
        register(submitImageRequest(), validationErrorResponse("houseServiceReceipts"))
    }

    private fun submitImageRequest(): RequestMatcher {
        return request {
            method("PUT")
            val path = "2.0/rent/user/me/flats/$FLAT_ID/" +
                "house-services/periods/$PERIOD_ID/receipts"
            path(path)
            jsonBody {
                "houseServiceReceipts" to jsonArrayOf(
                    jsonObject {
                        "namespace" to NAMESPACE
                        "groupId" to GROUP_ID
                        "name" to NEW_IMAGE_NAME
                    }
                )
            }
        }
    }

    private companion object {

        const val SEND_RECEIPTS_TITLE = "Отправим квитанции собственнику за\u00A0октябрь?"
    }
}
