package com.yandex.mobile.realty.test.yandexrent

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.RequestMatcher
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.success
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Test

/**
 * @author misha-kozlov on 24.10.2021
 */
@LargeTest
class TenantPaymentConfirmationTest : TenantUtilitiesImagesTest() {

    @Test
    fun shouldSendImages() {
        shouldSendImages(
            notificationKey = "houseServiceTimeToSendPaymentConfirmation",
            notificationTitle = SEND_CONFIRMATION_TITLE,
            registerUploadPhotoError = { registerUploadPhotoError("paymentConfirmation") },
            registerUploadPhoto = { registerUploadPhoto("paymentConfirmation") },
            registerSubmitRequest = { registerSendPaymentConfirmation() }
        )
    }

    @Test
    fun shouldOpenFullscreenImage() {
        shouldOpenFullscreenImage(
            notificationKey = "houseServiceTimeToSendPaymentConfirmation",
            notificationTitle = SEND_CONFIRMATION_TITLE,
            registerUploadPhoto = { registerUploadPhoto("paymentConfirmation") }
        )
    }

    @Test
    fun shouldEditDeclinedImages() {
        val notificationTitleRes = R.string.yandex_rent_tenant_payment_confirmation_declined_title
        shouldEditDeclinedImages(
            notificationKey = "houseServicePaymentConfirmationDeclined",
            notificationTitleRes = notificationTitleRes,
            registerMeters = {
                registerPeriod(paymentConfirmation = getDeclinedImages())
            },
            registerUploadPhoto = { registerUploadPhoto("paymentConfirmation") },
            registerSubmitRequest = { registerSendPaymentConfirmation() }
        )
    }

    @Test
    fun shouldShowErrorToast() {
        shouldShowNetworkErrorToast(
            notificationKey = "houseServiceTimeToSendPaymentConfirmation",
            notificationTitle = SEND_CONFIRMATION_TITLE,
            imageEntityKey = "paymentConfirmation"
        )
    }

    @Test
    fun shouldShowConflictErrorToast() {
        shouldShowSpecificErrorToast(
            notificationKey = "houseServiceTimeToSendPaymentConfirmation",
            notificationTitle = SEND_CONFIRMATION_TITLE,
            imageEntityKey = "paymentConfirmation",
            registerError = { registerConflictError() }
        )
    }

    @Test
    fun shouldShowValidationErrorToast() {
        shouldShowSpecificErrorToast(
            notificationKey = "houseServiceTimeToSendPaymentConfirmation",
            notificationTitle = SEND_CONFIRMATION_TITLE,
            imageEntityKey = "paymentConfirmation",
            registerError = { registerValidationError() }
        )
    }

    @Test
    fun shouldShowExitConfirmationDialog() {
        shouldShowExitConfirmationDialog(
            notificationKey = "houseServiceTimeToSendPaymentConfirmation",
            notificationTitle = SEND_CONFIRMATION_TITLE,
            registerUploadPhoto = { registerUploadPhoto("paymentConfirmation") }
        )
    }

    private fun DispatcherRegistry.registerSendPaymentConfirmation() {
        register(submitImageRequest(), success())
    }

    private fun DispatcherRegistry.registerConflictError() {
        register(submitImageRequest(), conflictError())
    }

    private fun DispatcherRegistry.registerValidationError() {
        register(submitImageRequest(), validationErrorResponse("paymentConfirmations"))
    }

    private fun submitImageRequest(): RequestMatcher {
        return request {
            method("PUT")
            val path = "2.0/rent/user/me/flats/$FLAT_ID/" +
                "house-services/periods/$PERIOD_ID/confirmations"
            path(path)
            jsonBody {
                "paymentConfirmations" to jsonArrayOf(
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

        const val SEND_CONFIRMATION_TITLE = "Ну\u00A0что\u00A0ж, осталось только отправить " +
            "подтверждение оплаты за\u00A0октябрь —\u00A0и\u00A0готово!"
    }
}
