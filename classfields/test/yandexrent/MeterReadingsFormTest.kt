package com.yandex.mobile.realty.test.yandexrent

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.gson.JsonObject
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.createMockImageAndGetUriString
import com.yandex.mobile.realty.core.registerGetContentIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.*
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.view.TRecyclerItem
import com.yandex.mobile.realty.core.view.TSelectedImageView
import com.yandex.mobile.realty.core.view.TView
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.data.model.proto.ErrorCode
import com.yandex.mobile.realty.domain.model.yandexrent.Meter
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.test.services.registerTenantRentFlat
import com.yandex.mobile.realty.utils.jsonArrayOf
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
class MeterReadingsFormTest : BaseTest() {

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
    fun shouldSendSingleMeter() {
        val meterId = METER_SINGLE_SHOULD_BE_SENT_ID
        configureWebServer {
            registerTenantRentFlat(notification = metersNotification())
            registerMeters("metersList.json")
            registerUploadPhoto(IMAGE_NAME_1)
            registerPutMeterReadingsValidationErrors(
                meterId,
                Meter.Tariff.SINGLE
            )
            registerPutMeterReadingsSuccess(
                meterId,
                listOf(READING_VALUE_1 to IMAGE_NAME_1)
            )
            registerMeters("metersListAllSending.json")
            registerTenantRentFlat()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openMeterForm(meterId)

        onScreen<MeterReadingsFormScreen> {
            submitButtonItem
                .waitUntil { listView.contains(this) }
                .click()

            isContentStateMatches(getTestRelatedFilePath("formErrors"))

            selectImage(singleAddImagesItem, IMAGE_NAME_1)
            singleInputView.typeText(READING_VALUE_1)

            isContentStateMatches(getTestRelatedFilePath("formFilled"))

            listView.scrollTo(submitButtonItem).click()
        }
        checkMeterUpdated(meterId, getTestRelatedFilePath("updatedMeter"))
    }

    @Test
    fun shouldClearAndResendDoubleMeter() {
        val meterId = METER_DOUBLE_SENDING_ID
        configureWebServer {
            registerTenantRentFlat(notification = metersNotification())
            registerMeters("metersList.json")
            registerUploadPhoto(IMAGE_NAME_1)
            registerUploadPhoto(IMAGE_NAME_2)
            registerPutMeterReadingsValidationErrors(
                meterId,
                Meter.Tariff.DOUBLE
            )
            registerPutMeterReadingsSuccess(
                meterId,
                listOf(READING_VALUE_1 to IMAGE_NAME_1, READING_VALUE_2 to IMAGE_NAME_2)
            )
            registerMeters("metersListAllSending.json")
            registerTenantRentFlat()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openMeterForm(meterId)

        onScreen<MeterReadingsFormScreen> {
            editButtonItem
                .waitUntil { listView.contains(this) }
                .click()

            listView.scrollTo(readingImageItem(IMAGE_NAME_1))
                .click()
        }
        onScreen<GalleryScreen> {
            waitUntil { photoView.isCompletelyDisplayed() }
            pressBack()
        }
        onScreen<MeterReadingsFormScreen> {
            listView.scrollTo(readingImageItem(IMAGE_NAME_1))
                .invoke { deleteButton.click() }
            listView.scrollTo(readingInputItem(T1))
            readingInputView(T1).clearText()

            listView.scrollTo(readingImageItem(IMAGE_NAME_2))
                .invoke { deleteButton.click() }
            listView.scrollTo(readingInputItem(T2))
            readingInputView(T2).clearText()

            submitButtonItem
                .waitUntil { listView.contains(this) }
                .click()

            isContentStateMatches(getTestRelatedFilePath("formErrors"))

            selectImage(readingAddImagesItem(T1), IMAGE_NAME_1)
            listView.scrollTo(readingInputItem(T1))
            readingInputView(T1).typeText(READING_VALUE_1)

            selectImage(readingAddImagesItem(T2), IMAGE_NAME_2)
            listView.scrollTo(readingInputItem(T2))
            readingInputView(T2).typeText(READING_VALUE_2)

            isContentStateMatches(getTestRelatedFilePath("formFilled"))

            listView.scrollTo(submitButtonItem).click()
        }
        checkMeterUpdated(meterId, getTestRelatedFilePath("updatedMeter"))
    }

    @Test
    fun shouldSendTripleMeter() {
        val meterId = METER_TRIPLE_SHOULD_BE_SENT_ID
        configureWebServer {
            registerTenantRentFlat(notification = metersNotification())
            registerMeters("metersList.json")
            registerUploadPhoto(IMAGE_NAME_1)
            registerUploadPhoto(IMAGE_NAME_2)
            registerUploadPhoto(IMAGE_NAME_3)
            registerPutMeterReadingsValidationErrors(
                meterId,
                Meter.Tariff.TRIPLE
            )
            registerPutMeterReadingsSuccess(
                meterId,
                listOf(
                    READING_VALUE_1 to IMAGE_NAME_1,
                    READING_VALUE_2 to IMAGE_NAME_2,
                    READING_VALUE_3 to IMAGE_NAME_3
                )
            )
            registerMeters("metersListAllSending.json")
            registerTenantRentFlat()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openMeterForm(meterId)

        onScreen<MeterReadingsFormScreen> {
            submitButtonItem
                .waitUntil { listView.contains(this) }
                .click()

            isContentStateMatches(getTestRelatedFilePath("formErrors"))

            selectImage(readingAddImagesItem(T1), IMAGE_NAME_1)
            listView.scrollTo(readingInputItem(T1))
            readingInputView(T1).typeText(READING_VALUE_1)

            selectImage(readingAddImagesItem(T2), IMAGE_NAME_2)
            listView.scrollTo(readingInputItem(T2))
            readingInputView(T2).typeText(READING_VALUE_2)

            selectImage(readingAddImagesItem(T3), IMAGE_NAME_3)
            listView.scrollTo(readingInputItem(T3))
            readingInputView(T3).typeText(READING_VALUE_3)

            isContentStateMatches(getTestRelatedFilePath("formFilled"))

            listView.scrollTo(submitButtonItem).click()
        }
        checkMeterUpdated(meterId, getTestRelatedFilePath("updatedMeter"))
    }

    @Test
    fun shouldNotAllowSubmitWithImageError() {
        val meterId = METER_SINGLE_SHOULD_BE_SENT_ID
        configureWebServer {
            registerTenantRentFlat(notification = metersNotification())
            registerMeters("metersList.json")
            registerUploadPhotoError()
            registerUploadPhoto(IMAGE_NAME_1)
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openMeterForm(meterId)

        onScreen<MeterReadingsFormScreen> {
            waitUntil { listView.contains(submitButtonItem) }
            val imageView = selectImage(singleAddImagesItem, IMAGE_NAME_1, checkLoaded = false)
            waitUntil { imageView.isImageFailed() }

            isContentStateMatches(getTestRelatedFilePath("imageError"))

            listView.scrollTo(submitButtonItem).click()

            onScreen<ConfirmationDialogScreen> {
                root.isViewStateMatches(getTestRelatedFilePath("errorDialog"))
                confirmButton.click()
            }
            imageView.retryButton.click()
            waitUntil { imageView.isImageLoaded() }
        }
    }

    @Test
    fun shouldShowUpdateDialog() {
        val meterId = METER_SINGLE_SHOULD_BE_SENT_ID
        configureWebServer {
            registerTenantRentFlat(notification = metersNotification())
            registerMeters("metersList.json")
            registerPutMeterReadingsValidationErrors(
                meterId,
                Meter.Tariff.SINGLE,
                listOf("/unknownParameter")
            )
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openMeterForm(meterId)

        onScreen<MeterReadingsFormScreen> {
            submitButtonItem
                .waitUntil { listView.contains(this) }
                .click()

            onScreen<ConfirmationDialogScreen> {
                root.isViewStateMatches("dialog/needAppUpdateDialog")
            }
        }
    }

    @Test
    fun shouldShowRejectChangesDialog() {
        val meterId = METER_SINGLE_SHOULD_BE_SENT_ID
        configureWebServer {
            registerTenantRentFlat(notification = metersNotification())
            registerMeters("metersList.json")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openMeterForm(meterId)

        onScreen<MeterReadingsFormScreen> {
            waitUntil { listView.contains(submitButtonItem) }

            singleInputView.typeText(READING_VALUE_1)

            val imageView = selectImage(singleAddImagesItem, IMAGE_NAME_1, checkLoaded = false)
            waitUntil { imageView.isImageFailed() }

            pressBack()

            onScreen<ConfirmationDialogScreen> {
                root.isViewStateMatches(getTestRelatedFilePath("rejectChangesDialog"))
                cancelButton.click()
            }
            listView.contains(submitButtonItem)

            pressBack()
            onScreen<ConfirmationDialogScreen> {
                root.isViewStateMatches(getTestRelatedFilePath("rejectChangesDialog"))
                confirmButton.click()
            }

            listView.doesNotContain(submitButtonItem)
        }
    }

    @Test
    fun shouldShowMeterStatusError() {
        val meterId = METER_SINGLE_SHOULD_BE_SENT_ID
        configureWebServer {
            registerTenantRentFlat(notification = metersNotification())
            registerMeters("metersList.json")
            registerPutMeterReadingsStatusError(meterId)
            registerMeters("metersListAllSending.json")
            registerTenantRentFlat()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openMeterForm(meterId)

        onScreen<MeterReadingsFormScreen> {
            submitButtonItem
                .waitUntil { listView.contains(this) }
                .click()

            onScreen<MeterStatusInvalidScreen> {
                root.isViewStateMatches(getTestRelatedFilePath("errorDialog"))
                okButton.click()
            }
        }
        checkMeterUpdated(meterId, getTestRelatedFilePath("updatedMeter"))
    }

    @Test
    fun shouldShowSubmitErrorToast() {
        val meterId = METER_SINGLE_SHOULD_BE_SENT_ID
        configureWebServer {
            registerTenantRentFlat(notification = metersNotification())
            registerMeters("metersList.json")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openMeterForm(meterId)

        onScreen<MeterReadingsFormScreen> {
            submitButtonItem
                .waitUntil { listView.contains(this) }
                .click()

            waitUntil {
                toastView(getResourceString(R.string.error_try_again))
                    .isCompletelyDisplayed()
            }
        }
    }

    @Test
    fun shouldShowConflictErrorToast() {
        val meterId = METER_SINGLE_SHOULD_BE_SENT_ID
        val errorMessage = "Ошибка"
        configureWebServer {
            registerTenantRentFlat(notification = metersNotification())
            registerMeters("metersList.json")
            registerPutMeterReadingsConflictError(meterId, errorMessage)
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        openMeterForm(meterId)

        onScreen<MeterReadingsFormScreen> {
            submitButtonItem
                .waitUntil { listView.contains(this) }
                .click()

            waitUntil {
                toastView(errorMessage).isCompletelyDisplayed()
            }
        }
    }

    @Test
    fun shouldShowInitialReadingsAndOpenImages() {
        val meterId = METER_SINGLE_INITIAL_READINGS_ID
        configureWebServer {
            registerTenantRentFlat(notification = metersNotification())
            registerMeters("metersListInitialReadings.json")
            registerUploadPhoto(IMAGE_NAME_1)
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        openMeterForm(meterId)

        onScreen<MeterReadingsFormScreen> {
            submitButtonItem
                .waitUntil { listView.contains(this) }

            isContentStateMatches(getTestRelatedFilePath("initialReadings"))
            toolbarImageView.click()
        }

        onScreen<GalleryScreen> {
            waitUntil { photoView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<MeterReadingsFormScreen> {
            selectImage(singleAddImagesItem, IMAGE_NAME_1).click()
        }
        onScreen<GalleryScreen> {
            waitUntil { photoView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldShowPreviousReadings() {
        val meterId = METER_SINGLE_PREVIOUS_READINGS_ID
        configureWebServer {
            registerTenantRentFlat(notification = metersNotification())
            registerMeters("metersListPreviousReadings.json")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        openMeterForm(meterId)

        onScreen<MeterReadingsFormScreen> {
            submitButtonItem
                .waitUntil { listView.contains(this) }

            isContentStateMatches(getTestRelatedFilePath("previousReadings"))
        }
    }

    private fun openMeterForm(meterId: String) {
        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_send_meter_readings_title)
            notificationItem(title)
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

    private fun MeterReadingsFormScreen.selectImage(
        addImageItem: TRecyclerItem<TView>,
        imageName: String,
        checkLoaded: Boolean = true
    ): TSelectedImageView {
        val imageUri = registerGetImageIntent(imageName)
        listView.scrollTo(addImageItem).click()
        onScreen<ChooseMediaDialogScreen> {
            galleryButton.click()
        }

        return readingImageItem(imageUri)
            .waitUntil { listView.contains(this) }
            .apply {
                if (checkLoaded) {
                    waitUntil { isImageLoaded() }
                }
            }
    }

    private fun checkMeterUpdated(meterId: String, screenshotKey: String) {
        onScreen<MeterReadingsListScreen> {
            waitUntil { toolbarTitleView.containsText(TITLE) }

            meterItem(meterId)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(screenshotKey)
            pressBack()
        }

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_tenant_send_meter_readings_title)
            listView.waitUntil {
                doesNotContain(notificationItem(title))
            }
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

    private fun DispatcherRegistry.registerPutMeterReadingsSuccess(
        meterId: String,
        readings: List<Pair<String, String>>
    ) {
        register(
            request {
                method("PUT")
                path(
                    "2.0/rent/user/me/flats/$FLAT_ID/house-services/periods/" +
                        "$PERIOD_ID/$meterId"
                )
                jsonBody {
                    "meterReadings" to jsonArrayOf(
                        readings.map { (value, image) ->
                            jsonObject {
                                "meterValue" to value.toDouble()
                                "meterPhoto" to jsonObject {
                                    "namespace" to NAMESPACE
                                    "groupId" to GROUP_ID
                                    "name" to image
                                }
                            }
                        }
                    )
                }
            },
            success()
        )
    }

    private fun DispatcherRegistry.registerPutMeterReadingsValidationErrors(
        meterId: String,
        tariff: Meter.Tariff,
        parameters: List<String> = listOf("/meterPhoto", "/meterValue")
    ) {
        register(
            request {
                method("PUT")
                path(
                    "2.0/rent/user/me/flats/$FLAT_ID/house-services/periods/" +
                        "$PERIOD_ID/$meterId"
                )
                jsonBody {
                    "meterReadings" to jsonArrayOf(
                        (0..tariff.ordinal).map {
                            jsonObject { }
                        }
                    )
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
                                (0..tariff.ordinal)
                                    .flatMap { index ->
                                        parameters.map { index.toString() + it }
                                    }
                                    .map { parameter ->
                                        jsonObject {
                                            "parameter" to parameter
                                            "code" to "code"
                                            "localizedDescription" to "$parameter error"
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerPutMeterReadingsStatusError(meterId: String) {
        register(
            request {
                method("PUT")
                path(
                    "2.0/rent/user/me/flats/$FLAT_ID/house-services/periods/" +
                        "$PERIOD_ID/$meterId"
                )
            },
            response {
                setResponseCode(409)
                jsonBody {
                    "error" to jsonObject {
                        "code" to "CONFLICT"
                        "message" to "Already sent to owner"
                        "data" to jsonObject {
                            "code" to "METER_READINGS_ARE_BLOCKED"
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerPutMeterReadingsConflictError(
        meterId: String,
        message: String
    ) {
        register(
            request {
                method("PUT")
                path(
                    "2.0/rent/user/me/flats/$FLAT_ID/house-services/periods/" +
                        "$PERIOD_ID/$meterId"
                )
            },
            response {
                setResponseCode(409)
                jsonBody {
                    "error" to jsonObject {
                        "code" to "CONFLICT"
                        "message" to message
                    }
                }
            }
        )
    }

    private fun metersNotification(): JsonObject {
        return jsonObject {
            "houseServiceSendMeterReadings" to jsonObject {
                "flatId" to FLAT_ID
                "periodId" to PERIOD_ID
                "period" to PERIOD
            }
        }
    }

    private fun DispatcherRegistry.registerUploadPhoto(imageName: String) {
        register(
            request {
                method("POST")
                path("2.0/files/get-upload-url")
                jsonBody {
                    "entities" to jsonArrayOf(
                        jsonObject {
                            IMAGE_ENTITY to jsonObject { }
                        }
                    )
                }
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "responseEntries" to jsonArrayOf(
                            jsonObject {
                                "uploadUrl" to UPLOAD_URL
                            }
                        )
                    }
                }
            }
        )
        register(
            request {
                method("POST")
                path(UPLOAD_PATH)
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "namespace" to NAMESPACE
                        "groupId" to GROUP_ID
                        "name" to imageName
                        "url" to IMAGE_URL
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerUploadPhotoError() {
        register(
            request {
                method("POST")
                path("2.0/files/get-upload-url")
            },
            response { error() }
        )
    }

    private fun registerGetImageIntent(fileName: String): String {
        return createMockImageAndGetUriString("meter.webp", fileName).also { uri ->
            registerGetContentIntent(uri)
        }
    }

    private companion object {

        const val METER_SINGLE_SHOULD_BE_SENT_ID = "4"
        const val METER_DOUBLE_SENDING_ID = "1"
        const val METER_TRIPLE_SHOULD_BE_SENT_ID = "3"
        const val METER_SINGLE_INITIAL_READINGS_ID = "10"
        const val METER_SINGLE_PREVIOUS_READINGS_ID = "11"
        const val PERIOD = "2021-10"
        const val PERIOD_ID = "periodId00001"
        const val TITLE = "Данные"
        const val UPLOAD_PATH = "upload"
        const val UPLOAD_URL = "https://localhost:8080/$UPLOAD_PATH"
        const val IMAGE_URL = "https://localhost:8080/meter.webp"
        const val NAMESPACE = "arenda"
        const val GROUP_ID = "65493"
        const val IMAGE_NAME_1 = "meter1.webp"
        const val IMAGE_NAME_2 = "meter2.webp"
        const val IMAGE_NAME_3 = "meter3.webp"
        const val IMAGE_ENTITY = "meterReadings"

        const val READING_VALUE_1 = "0.00006"
        const val READING_VALUE_2 = "222"
        const val READING_VALUE_3 = "333"

        const val T1 = "Т1"
        const val T2 = "Т2"
        const val T3 = "Т3"
    }
}
