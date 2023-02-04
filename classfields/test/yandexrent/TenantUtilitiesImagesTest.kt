package com.yandex.mobile.realty.test.yandexrent

import com.google.gson.JsonObject
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.createMockImageAndGetUriString
import com.yandex.mobile.realty.core.registerGetContentIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.InternetRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.*
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.test.services.registerTenantRentFlat
import com.yandex.mobile.realty.test.services.registerTenantServicesInfo
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import okhttp3.mockwebserver.MockResponse
import org.junit.Rule
import org.junit.rules.RuleChain

/**
 * @author misha-kozlov on 26.10.2021
 */
open class TenantUtilitiesImagesTest : RentUtilitiesTest() {

    private val authorizationRule = AuthorizationRule()
    private val internetRule = InternetRule()
    private val activityTestRule = RentFlatActivityTestRule(
        flatId = FLAT_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        internetRule,
        activityTestRule
    )

    protected fun shouldSendImages(
        notificationKey: String,
        notificationTitle: String,
        registerUploadPhotoError: DispatcherRegistry.() -> Unit,
        registerUploadPhoto: DispatcherRegistry.() -> Unit,
        registerSubmitRequest: DispatcherRegistry.() -> Unit
    ) {
        configureWebServer {
            registerTenantServicesInfo()
            registerTenantRentFlat(
                notification = jsonObject {
                    notificationKey to jsonObject {
                        "periodId" to PERIOD_ID
                        "period" to PERIOD
                    }
                }
            )
            registerPeriod()
            registerUploadPhotoError()
            registerUploadPhoto()
            registerSubmitRequest()
            registerTenantServicesInfo()
            registerTenantRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        registerGetImageIntent()

        onScreen<RentFlatScreen> {
            notificationItem(notificationTitle)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }
        onScreen<TenantUtilitiesImagesScreen> {
            waitUntil { toolbarTitleView.isTextEquals(TITLE) }
            root.isViewStateMatches(getTestRelatedFilePath("empty"))

            listView.scrollTo(submitButton).click()
            toastView(getResourceString(R.string.rent_utilities_no_images_selected))
                .isCompletelyDisplayed()

            selectImage()
            imageItem(number = 1)
                .waitUntil { listView.contains(this) }
                .waitUntil { isImageFailed() }
                .invoke { retryButton.click() }
            imageItem(number = 1)
                .view
                .waitUntil { isImageLoaded() }

            root.isViewStateMatches(getTestRelatedFilePath("selected"))
            listView.scrollTo(submitButton).click()

            fullscreenSuccessItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("success"))
                .invoke { actionButton.click() }
        }
        onScreen<RentFlatScreen> {
            waitUntil {
                listView.doesNotContain(notificationItem(notificationTitle))
            }
        }
    }

    protected fun shouldOpenFullscreenImage(
        notificationKey: String,
        notificationTitle: String,
        registerUploadPhoto: DispatcherRegistry.() -> Unit,
    ) {
        configureWebServer {
            registerTenantServicesInfo()
            registerTenantRentFlat(
                notification = jsonObject {
                    notificationKey to jsonObject {
                        "periodId" to PERIOD_ID
                        "period" to PERIOD
                    }
                }
            )
            registerPeriod()
            registerUploadPhoto()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        registerGetImageIntent()

        onScreen<RentFlatScreen> {
            notificationItem(notificationTitle)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }
        onScreen<TenantUtilitiesImagesScreen> {
            waitUntil { toolbarTitleView.isTextEquals(TITLE) }

            selectImage()
            imageItem(number = 1)
                .waitUntil { listView.contains(this) }
                .waitUntil { isImageLoaded() }
                .click()
        }
        onScreen<GalleryScreen> {
            waitUntil { photoView.isCompletelyDisplayed() }
        }
    }

    protected fun shouldEditDeclinedImages(
        notificationKey: String,
        notificationTitleRes: Int,
        registerMeters: DispatcherRegistry.() -> Unit,
        registerUploadPhoto: DispatcherRegistry.() -> Unit,
        registerSubmitRequest: DispatcherRegistry.() -> Unit
    ) {
        configureWebServer {
            registerTenantServicesInfo()
            registerTenantRentFlat(
                notification = jsonObject {
                    notificationKey to jsonObject {
                        "periodId" to PERIOD_ID
                    }
                }
            )
            registerMeters()
            registerUploadPhoto()
            registerSubmitRequest()
            registerTenantServicesInfo()
            registerTenantRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        registerGetImageIntent()

        val notificationTitle = getResourceString(notificationTitleRes)
        onScreen<RentFlatScreen> {
            notificationItem(notificationTitle)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }
        onScreen<TenantUtilitiesImagesScreen> {
            waitUntil { toolbarTitleView.isTextEquals(TITLE) }
            root.isViewStateMatches(getTestRelatedFilePath("empty"))

            imageItem(number = 1)
                .waitUntil { listView.contains(this) }
                .invoke { deleteButton.click() }
            imageItem(number = 1)
                .waitUntil { listView.doesNotContain(this) }

            listView.scrollTo(addImagesButton).click()
        }
        onScreen<ChooseMediaDialogScreen> {
            galleryButton.click()
        }
        onScreen<TenantUtilitiesImagesScreen> {
            imageItem(number = 1)
                .waitUntil { listView.contains(this) }
                .waitUntil { isImageLoaded() }
            listView.scrollTo(submitButton).click()

            fullscreenSuccessItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("success"))
                .invoke { actionButton.click() }
        }
        onScreen<RentFlatScreen> {
            waitUntil {
                listView.doesNotContain(notificationItem(notificationTitle))
            }
        }
    }

    protected fun shouldShowNetworkErrorToast(
        notificationKey: String,
        notificationTitle: String,
        imageEntityKey: String
    ) {
        configureWebServer {
            registerTenantServicesInfo()
            registerTenantRentFlat(
                notification = jsonObject {
                    notificationKey to jsonObject {
                        "periodId" to PERIOD_ID
                        "period" to PERIOD
                    }
                }
            )
            registerPeriod()
            registerUploadPhoto(imageEntityKey)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        registerGetImageIntent()

        onScreen<RentFlatScreen> {
            notificationItem(notificationTitle)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }
        onScreen<TenantUtilitiesImagesScreen> {
            waitUntil { toolbarTitleView.isTextEquals(TITLE) }

            selectImage()
            imageItem(number = 1)
                .waitUntil { listView.contains(this) }
                .waitUntil { isImageLoaded() }

            listView.scrollTo(submitButton).click()

            toastView(getResourceString(R.string.error_send_images)).isCompletelyDisplayed()
        }
    }

    protected fun shouldShowInternetErrorToast(
        notificationKey: String,
        notificationTitle: String,
        imageEntityKey: String
    ) {
        configureWebServer {
            registerTenantServicesInfo()
            registerTenantRentFlat(
                notification = jsonObject {
                    notificationKey to jsonObject {
                        "periodId" to PERIOD_ID
                        "period" to PERIOD
                    }
                }
            )
            registerPeriod()
            registerUploadPhoto(imageEntityKey)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        registerGetImageIntent()

        onScreen<RentFlatScreen> {
            notificationItem(notificationTitle)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }
        onScreen<TenantUtilitiesImagesScreen> {
            waitUntil { toolbarTitleView.isTextEquals(TITLE) }

            selectImage()
            imageItem(number = 1)
                .waitUntil { listView.contains(this) }
                .waitUntil { isImageLoaded() }

            internetRule.turnOff()

            listView.scrollTo(submitButton).click()

            toastView(getResourceString(R.string.error_network_message)).isCompletelyDisplayed()
        }
    }

    protected fun shouldShowExitConfirmationDialog(
        notificationKey: String,
        notificationTitle: String,
        registerUploadPhoto: DispatcherRegistry.() -> Unit
    ) {
        configureWebServer {
            registerTenantServicesInfo()
            registerTenantRentFlat(
                notification = jsonObject {
                    notificationKey to jsonObject {
                        "periodId" to PERIOD_ID
                        "period" to PERIOD
                    }
                }
            )
            registerPeriod()
            registerUploadPhoto()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        registerGetImageIntent()

        onScreen<RentFlatScreen> {
            notificationItem(notificationTitle)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }
        onScreen<TenantUtilitiesImagesScreen> {
            selectImage()
            imageItem(number = 1)
                .waitUntil { listView.contains(this) }
                .waitUntil { isImageLoaded() }
            pressBack()
        }
        onScreen<ConfirmationDialogScreen> {
            root.isViewStateMatches(getTestRelatedFilePath("dialog"))
            confirmButton.click()
        }
        onScreen<RentFlatScreen> {
            waitUntil {
                listView.contains(notificationItem(notificationTitle))
            }
        }
    }

    private fun TenantUtilitiesImagesScreen.selectImage() {
        addImagesButton
            .waitUntil { listView.contains(this) }
            .click()
        onScreen<ChooseMediaDialogScreen> {
            galleryButton.click()
        }
    }

    protected fun shouldShowSpecificErrorToast(
        notificationKey: String,
        notificationTitle: String,
        imageEntityKey: String,
        registerError: DispatcherRegistry.() -> Unit
    ) {
        configureWebServer {
            registerTenantServicesInfo()
            registerTenantRentFlat(
                notification = jsonObject {
                    notificationKey to jsonObject {
                        "periodId" to PERIOD_ID
                        "period" to PERIOD
                    }
                }
            )
            registerPeriod()
            registerUploadPhoto(imageEntityKey)
            registerError()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        registerGetImageIntent()

        onScreen<RentFlatScreen> {
            notificationItem(notificationTitle)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }
        onScreen<TenantUtilitiesImagesScreen> {
            waitUntil { toolbarTitleView.isTextEquals(TITLE) }
            listView.scrollTo(addImagesButton).click()
        }
        onScreen<ChooseMediaDialogScreen> {
            galleryButton.click()
        }
        onScreen<TenantUtilitiesImagesScreen> {
            imageItem(number = 1)
                .waitUntil { listView.contains(this) }
                .waitUntil { isImageLoaded() }
            listView.scrollTo(submitButton).click()

            toastView(ERROR_MESSAGE).isCompletelyDisplayed()
        }
    }

    protected fun getDeclinedImages(): JsonObject {
        return jsonObject {
            "photos" to jsonArrayOf(
                jsonObject {
                    "namespace" to NAMESPACE
                    "groupId" to GROUP_ID
                    "name" to OLD_IMAGE_NAME
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
            "reasonForDecline" to REASON
        }
    }

    protected fun validationErrorResponse(fieldName: String): MockResponse {
        return response {
            setResponseCode(400)
            jsonBody {
                "error" to jsonObject {
                    "code" to "VALIDATION_ERROR"
                    "data" to jsonObject {
                        "validationErrors" to jsonArrayOf(
                            jsonObject {
                                "parameter" to "/$fieldName"
                                "code" to "NO_PHOTO_CHANGES"
                                "localizedDescription" to ERROR_MESSAGE
                            }
                        )
                    }
                }
            }
        }
    }

    protected fun DispatcherRegistry.registerUploadPhoto(entity: String) {
        register(
            request {
                method("POST")
                path("2.0/files/get-upload-url")
                jsonBody {
                    "entities" to jsonArrayOf(
                        jsonObject {
                            entity to jsonObject { }
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
                        "name" to NEW_IMAGE_NAME
                        "url" to IMAGE_URL
                    }
                }
            }
        )
    }

    protected fun DispatcherRegistry.registerUploadPhotoError(entity: String) {
        register(
            request {
                method("POST")
                path("2.0/files/get-upload-url")
                jsonBody {
                    "entities" to jsonArrayOf(
                        jsonObject {
                            entity to jsonObject { }
                        }
                    )
                }
            },
            error()
        )
    }

    private fun registerGetImageIntent() {
        val uri = createMockImageAndGetUriString("receipt.webp")
        registerGetContentIntent(uri)
    }

    companion object {

        private const val UPLOAD_PATH = "upload"
        private const val UPLOAD_URL = "https://localhost:8080/$UPLOAD_PATH"
        private const val OLD_IMAGE_NAME = "111"
        const val NEW_IMAGE_NAME = "222"
        private const val REASON = "Фотографии какие-то некрасивые"
    }
}
