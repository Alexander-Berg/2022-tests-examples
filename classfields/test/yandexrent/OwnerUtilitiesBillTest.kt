package com.yandex.mobile.realty.test.yandexrent

import androidx.test.filters.LargeTest
import com.google.gson.JsonObject
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.createMockImageAndGetUriString
import com.yandex.mobile.realty.core.interaction.NamedIntents
import com.yandex.mobile.realty.core.matchesMarketIntent
import com.yandex.mobile.realty.core.registerGetContentIntent
import com.yandex.mobile.realty.core.registerMarketIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.*
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.view.TSelectedImageView
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.test.services.registerOwnerRentFlat
import com.yandex.mobile.realty.utils.JsonObjectBuilder
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author andrey-bgm on 04.02.2022
 */
@LargeTest
class OwnerUtilitiesBillTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = RentFlatActivityTestRule(
        flatId = FLAT_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
    )

    @Test
    fun submitBill() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = billNotification("houseServiceTimeToSendBills")
            )
            registerBill(bill("SHOULD_BE_SENT"))
            registerSubmitBillError(validationError())
            registerUploadPhoto(IMAGE_NAME_1)
            registerUploadPhoto(IMAGE_NAME_2)
            registerUploadPhoto(IMAGE_NAME_3)
            registerSubmitBill { fullBillBody() }
            registerOwnerRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(SEND_BILL_NOTIFICATION_TITLE)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }

        onScreen<RentUtilitiesBillFormScreen> {
            listView.waitUntil { contains(addImagesButton) }
            isContentStateMatches(getTestRelatedFilePath("emptyBillContent"))
            listView.scrollTo(submitButton).click()

            fieldErrorItem("Укажите сумму").waitUntil { listView.contains(this) }
            isContentStateMatches(getTestRelatedFilePath("validationErrors"))
            selectImage(imageNumber = 1).click()

            onScreen<GalleryScreen> {
                waitUntil { photoView.isCompletelyDisplayed() }
                pressBack()
            }

            selectImage(imageNumber = 2)
            selectImage(imageNumber = 3)
            listView.scrollTo(amountItem)
                .invoke { inputView.typeText(AMOUNT) }
            listView.scrollTo(commentItem).click()
        }

        onScreen<RentCommentScreen> {
            waitUntil { messageView.isCompletelyDisplayed() }
            isContentStateMatches(getTestRelatedFilePath("emptyCommentContent"))

            messageView.typeText(COMMENT)
            doneButton.click()
        }

        onScreen<RentUtilitiesBillFormScreen> {
            isContentStateMatches(getTestRelatedFilePath("filledBillContent"))
            listView.scrollTo(submitButton).click()

            listView.waitUntil { contains(successItem) }
            isContentStateMatches(getTestRelatedFilePath("successContent"))
            successButton.click()
        }

        onScreen<RentFlatScreen> {
            waitUntil {
                listView.doesNotContain(notificationItem(SEND_BILL_NOTIFICATION_TITLE))
            }
        }
    }

    @Test
    fun submitDeclinedBill() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = billNotification("houseServiceBillsDeclined")
            )
            registerBill(declinedBill())
            registerUploadPhoto(IMAGE_NAME_2)
            registerUploadPhoto(IMAGE_NAME_3)
            registerSubmitBill { fullBillBody() }
            registerOwnerRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        val notificationTitle = getResourceString(R.string.yandex_rent_owner_bill_declined_title)

        onScreen<RentFlatScreen> {
            notificationItem(notificationTitle)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }

        onScreen<RentUtilitiesBillFormScreen> {
            waitUntil { listView.contains(editDeclinedButton) }
            isContentStateMatches(getTestRelatedFilePath("declinedContent"))
            declinedImageView(IMAGE_URL).click()
        }

        onScreen<GalleryScreen> {
            waitUntil { photoView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<RentUtilitiesBillFormScreen> {
            listView.scrollTo(editDeclinedButton).click()

            listView.waitUntil { contains(addImagesButton) }
            isContentStateMatches(getTestRelatedFilePath("declinedFormContent"))
            selectImage(imageNumber = 2)
            selectImage(imageNumber = 3)
            listView.scrollTo(amountItem)
                .invoke { inputView.retypeText(AMOUNT) }
            listView.scrollTo(commentItem).click()
        }

        onScreen<RentCommentScreen> {
            waitUntil { messageView.isCompletelyDisplayed() }

            messageView.replaceText(COMMENT)
            doneButton.click()
        }

        onScreen<RentUtilitiesBillFormScreen> {
            isContentStateMatches(getTestRelatedFilePath("newFormContent"))
            listView.scrollTo(submitButton).click()

            listView.waitUntil { contains(successItem) }
            isContentStateMatches(getTestRelatedFilePath("successContent"))
            successButton.click()
        }

        onScreen<RentFlatScreen> {
            waitUntil {
                listView.doesNotContain(notificationItem(notificationTitle))
            }
        }
    }

    @Test
    fun showErrors() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = billNotification("houseServiceTimeToSendBills")
            )
            registerBillError()
            registerBill(bill("SHOULD_BE_SENT"))
            registerSubmitBillError()
            registerSubmitBillError(unknownValidationError())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(SEND_BILL_NOTIFICATION_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        registerMarketIntent()

        onScreen<RentUtilitiesBillFormScreen> {
            waitUntil { listView.contains(fullscreenErrorItem) }
            isContentStateMatches(getTestRelatedFilePath("loadingError"))
            retryButton.click()

            listView.waitUntil { contains(addImagesButton) }
            listView.scrollTo(submitButton).click()

            waitUntil {
                toastView(getResourceString(R.string.error_try_again))
                    .isCompletelyDisplayed()
            }
            listView.scrollTo(submitButton).click()

            onScreen<ConfirmationDialogScreen> {
                waitUntil { titleView.isCompletelyDisplayed() }
                root.isViewStateMatches("dialog/needAppUpdateDialog")
                confirmButton.click()
                NamedIntents.intended(matchesMarketIntent())
            }
        }
    }

    @Test
    fun openBillWhenCanBeSent() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = billNotification("houseServiceTimeToSendBills")
            )
            registerBill(bill("CAN_BE_SENT"))
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(SEND_BILL_NOTIFICATION_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<RentUtilitiesBillFormScreen> {
            listView.waitUntil { contains(addImagesButton) }
        }
    }

    @Test
    fun openBillWhenAlreadySent() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = billNotification("houseServiceTimeToSendBills")
            )
            registerBill(bill("SHOULD_BE_PAID"))
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(SEND_BILL_NOTIFICATION_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<RentUtilitiesBillFormScreen> {
            listView.waitUntil { contains(successItem) }
        }
    }

    @Test
    fun openBillWhenAlreadyPaid() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = billNotification("houseServiceTimeToSendBills")
            )
            registerBill(bill("PAID"))
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(SEND_BILL_NOTIFICATION_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<RentUtilitiesBillFormScreen> {
            listView.waitUntil { contains(paidItem) }
            isContentStateMatches(getTestRelatedFilePath("paidContent"))
        }
    }

    @Test
    fun showPaidBillNotification() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = billNotification("houseServiceBillsPaid")
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(PAID_BILL_NOTIFICATION_TITLE)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
        }
    }

    private fun billNotification(notification: String): JsonObject {
        return jsonObject {
            notification to jsonObject {
                "periodId" to PERIOD_ID
                "period" to PERIOD
            }
        }
    }

    private fun bill(status: String): JsonObject {
        return jsonObject { "billStatus" to status }
    }

    private fun declinedBill(): JsonObject {
        return jsonObject {
            "billStatus" to "DECLINED"
            "bill" to jsonObject {
                "amount" to 950_010
                "comment" to "Some changes this month"
                "photos" to jsonArrayOf(
                    jsonObject {
                        "namespace" to "arenda"
                        "groupId" to "65494"
                        "name" to IMAGE_NAME_1
                        "imageUrls" to jsonArrayOf(
                            jsonObject {
                                "alias" to "1024x1024"
                                "url" to IMAGE_URL
                            },
                            jsonObject {
                                "alias" to "orig"
                                "url" to IMAGE_URL
                            }
                        )
                    }
                )
                "reasonForDecline" to "Too much"
            }
        }
    }

    private fun validationError(): JsonObject {
        return jsonObject {
            "code" to "VALIDATION_ERROR"
            "data" to jsonObject {
                "validationErrors" to jsonArrayOf(
                    jsonObject {
                        "parameter" to "/amount"
                        "localizedDescription" to "Укажите сумму"
                        "code" to "EMPTY_AMOUNT"
                    },
                    jsonObject {
                        "parameter" to "/comment"
                        "localizedDescription" to "Укажите комментарий"
                        "code" to "EMPTY_COMMENT"
                    },
                    jsonObject {
                        "parameter" to "/photos"
                        "localizedDescription" to "Добавьте фото"
                        "code" to "EMPTY_PHOTOS"
                    }
                )
            }
        }
    }

    private fun unknownValidationError(): JsonObject {
        return jsonObject {
            "code" to "VALIDATION_ERROR"
            "data" to jsonObject {
                "validationErrors" to jsonArrayOf(
                    jsonObject {
                        "parameter" to "/someField"
                        "localizedDescription" to "Укажите некоторое значение"
                        "code" to "EMPTY_FIELD"
                    }
                )
            }
        }
    }

    private fun registerGetImageIntent(imageNumber: Int) {
        val uri = createMockImageAndGetUriString(
            mockName = "receipt.webp",
            fileName = "image_$imageNumber.webp"
        )
        registerGetContentIntent(uri)
    }

    private fun RentUtilitiesBillFormScreen.selectImage(imageNumber: Int): TSelectedImageView {
        registerGetImageIntent(imageNumber)
        addImagesButton
            .waitUntil { listView.contains(this) }
            .click()
        onScreen<ChooseMediaDialogScreen> {
            galleryButton.click()
        }
        return imageItem(number = imageNumber)
            .waitUntil { listView.contains(this) }
            .waitUntil { isImageLoaded() }
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

    private fun DispatcherRegistry.registerBillError() {
        register(
            request {
                path("2.0/rent/user/me/flats/$FLAT_ID/house-services/periods/$PERIOD_ID")
            },
            error()
        )
    }

    private fun DispatcherRegistry.registerUploadPhoto(imageName: String) {
        register(
            request {
                method("POST")
                path("2.0/files/get-upload-url")
                jsonBody {
                    "entities" to jsonArrayOf(
                        jsonObject {
                            "bill" to jsonObject { }
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

    private fun DispatcherRegistry.registerSubmitBill(
        billBody: JsonObjectBuilder.() -> Unit
    ) {
        register(
            request {
                method("PUT")
                path("2.0/rent/user/me/flats/$FLAT_ID/house-services/periods/$PERIOD_ID/bills")
                jsonBody(billBody)
            },
            success()
        )
    }

    private fun DispatcherRegistry.registerSubmitBillError(error: JsonObject = jsonObject {}) {
        register(
            request {
                method("PUT")
                path("2.0/rent/user/me/flats/$FLAT_ID/house-services/periods/$PERIOD_ID/bills")
            },
            response {
                setResponseCode(400)
                jsonBody { "error" to error }
            }
        )
    }

    private fun JsonObjectBuilder.fullBillBody() {
        "amount" to AMOUNT_KOPECKS
        "comment" to COMMENT
        "photos" to jsonArrayOf(
            jsonObject {
                "namespace" to NAMESPACE
                "groupId" to GROUP_ID
                "name" to IMAGE_NAME_1
            },
            jsonObject {
                "namespace" to NAMESPACE
                "groupId" to GROUP_ID
                "name" to IMAGE_NAME_2
            },
            jsonObject {
                "namespace" to NAMESPACE
                "groupId" to GROUP_ID
                "name" to IMAGE_NAME_3
            }
        )
    }

    private companion object {

        const val PERIOD = "2022-01"
        const val PERIOD_ID = "periodId00001"
        const val SEND_BILL_NOTIFICATION_TITLE =
            "Давайте отправим счёт на\u00A0оплату за\u00A0январь"
        const val PAID_BILL_NOTIFICATION_TITLE = "Ура! Жилец оплатил счёт за\u00A0январь."
        const val AMOUNT = "5523.85"
        const val AMOUNT_KOPECKS = 552_385
        const val COMMENT = "No changes this month"

        private const val UPLOAD_PATH = "upload"
        private const val UPLOAD_URL = "https://localhost:8080/$UPLOAD_PATH"
        private const val IMAGE_URL = "https://localhost:8080/receipt.webp"
        private const val NAMESPACE = "arenda"
        private const val GROUP_ID = "65494"
        private const val IMAGE_NAME_1 = "image_1"
        private const val IMAGE_NAME_2 = "image_2"
        private const val IMAGE_NAME_3 = "image_3"
    }
}
