package com.yandex.mobile.realty.test.yandexrent

import androidx.test.filters.LargeTest
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
import com.yandex.mobile.realty.core.view.TView
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.*
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author misha-kozlov on 01.12.2021
 */
@LargeTest
class OwnerFlatImagesTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = RentFlatActivityTestRule(
        flatId = FLAT_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun shouldSendNewImages() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = ownerConfirmedTodoNotification(flatPhotosDone = false)
            )
            registerFlatInfo()
            registerUploadPhoto(IMAGE_NAME_1)
            registerUploadPhoto(IMAGE_NAME_2)
            registerUploadPhoto(IMAGE_NAME_3)
            registerUploadPhoto(IMAGE_NAME_4)
            registerFailedSubmitRequest()
            registerSuccessSubmitRequest()
            registerOwnerRentFlat(
                notification = ownerConfirmedTodoNotification(flatPhotosDone = true)
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            todoNotificationActionItem(ACTION_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }
        onScreen<OwnerFlatImagesScreen> {
            waitUntil { toolbarTitleView.isTextEquals(TITLE) }
            root.isViewStateMatches(getTestRelatedFilePath("empty"))

            selectImage(addImagesButton, imageNumber = 1)

            listView.scrollTo(submitButton).click()
            toastView(getResourceString(R.string.yandex_rent_owner_flat_images_quantity_error))
                .isCompletelyDisplayed()

            selectImage(addImageButton, imageNumber = 2)
            selectImage(addImageButton, imageNumber = 3)
            selectImage(addImageButton, imageNumber = 4)

            listView.scrollTo(submitButton).click()

            toastView(getResourceString(R.string.error_send_images)).isCompletelyDisplayed()

            listView.scrollTo(submitButton).click()

            fullscreenSuccessItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("success"))
                .invoke { actionButton.click() }
        }
        onScreen<RentFlatScreen> {
            todoNotificationActionItem(ACTION_TITLE)
                .waitUntil {
                    listView.contains(this)
                    view.invoke { doneView.isCompletelyDisplayed() }
                }
        }
    }

    @Test
    fun shouldShowLoadedImages() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = ownerConfirmedTodoNotification(flatPhotosDone = false)
            )
            registerFlatInfoWithImages()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            todoNotificationActionItem(ACTION_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }
        onScreen<OwnerFlatImagesScreen> {
            waitUntil { toolbarTitleView.isTextEquals(TITLE) }
            root.isViewStateMatches(getTestRelatedFilePath("images"))
        }
    }

    @Test
    fun shouldOpenFullscreenImages() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = ownerConfirmedTodoNotification(flatPhotosDone = false)
            )
            registerFlatInfoWithImages()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            todoNotificationActionItem(ACTION_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }
        onScreen<OwnerFlatImagesScreen> {
            waitUntil { toolbarTitleView.isTextEquals(TITLE) }
            imageItem(number = 1)
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<GalleryScreen> {
            waitUntil { photoView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldShowExitConfirmationDialog() {
        configureWebServer {
            registerOwnerRentFlat(notification = ownerConfirmedTodoNotification())
            registerFlatInfo()
            registerUploadPhoto(IMAGE_NAME_1)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        val notificationTitle = getResourceString(R.string.yandex_rent_owner_confirmed_todo_title)
        onScreen<RentFlatScreen> {
            todoNotificationActionItem(ACTION_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }
        onScreen<OwnerFlatImagesScreen> {
            selectImage(addImagesButton, imageNumber = 1)
            pressBack()
        }
        onScreen<ConfirmationDialogScreen> {
            root.isViewStateMatches(getTestRelatedFilePath("dialog"))
            confirmButton.click()
        }
        onScreen<RentFlatScreen> {
            todoNotificationHeaderItem(notificationTitle).waitUntil { listView.contains(this) }
        }
    }

    private fun registerGetImageIntent(imageNumber: Int) {
        val uri = createMockImageAndGetUriString(
            mockName = "apartment-photo-small.webp",
            fileName = "image_$imageNumber.webp"
        )
        registerGetContentIntent(uri)
    }

    private fun OwnerFlatImagesScreen.selectImage(
        addImageButton: TRecyclerItem<TView>,
        imageNumber: Int
    ) {
        registerGetImageIntent(imageNumber)
        addImageButton
            .waitUntil { listView.contains(this) }
            .click()
        onScreen<ChooseMediaDialogScreen> {
            galleryButton.click()
        }
        imageItem(number = imageNumber)
            .waitUntil { listView.contains(this) }
            .waitUntil { isImageLoaded() }
    }

    private fun DispatcherRegistry.registerFlatInfo() {
        register(
            request {
                path("2.0/rent/user/me/flats/$FLAT_ID")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "flat" to jsonObject {
                            "flatId" to FLAT_ID
                            "address" to jsonObject {
                                "addressFromStreetToFlat" to FLAT_ADDRESS
                            }
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerFlatInfoWithImages() {
        register(
            request {
                path("2.0/rent/user/me/flats/$FLAT_ID")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "flat" to jsonObject {
                            "flatId" to FLAT_ID
                            "address" to jsonObject {
                                "addressFromStreetToFlat" to FLAT_ADDRESS
                            }
                            "images" to jsonArrayOf(
                                jsonObject {
                                    "namespace" to NAMESPACE
                                    "groupId" to GROUP_ID
                                    "name" to "image"
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
                        }
                    }
                }
            }
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
                            "rentFlat" to jsonObject { }
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

    private fun DispatcherRegistry.registerFailedSubmitRequest() {
        register(
            request {
                method("PUT")
                path("2.0/rent/user/me/flats/$FLAT_ID")
                jsonBody {
                    "images" to jsonArrayOf(
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
                        },
                        jsonObject {
                            "namespace" to NAMESPACE
                            "groupId" to GROUP_ID
                            "name" to IMAGE_NAME_4
                        }
                    )
                }
            },
            error()
        )
    }

    private fun DispatcherRegistry.registerSuccessSubmitRequest() {
        register(
            request {
                method("PUT")
                path("2.0/rent/user/me/flats/$FLAT_ID")
                jsonBody {
                    "images" to jsonArrayOf(
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
                        },
                        jsonObject {
                            "namespace" to NAMESPACE
                            "groupId" to GROUP_ID
                            "name" to IMAGE_NAME_4
                        }
                    )
                }
            },
            success()
        )
    }

    companion object {

        private const val UPLOAD_PATH = "upload"
        private const val UPLOAD_URL = "https://localhost:8080/$UPLOAD_PATH"
        private const val IMAGE_URL = "https://localhost:8080/apartment-photo-small.webp"
        private const val NAMESPACE = "arenda"
        private const val GROUP_ID = "65493"
        private const val IMAGE_NAME_1 = "image_1"
        private const val IMAGE_NAME_2 = "image_2"
        private const val IMAGE_NAME_3 = "image_3"
        private const val IMAGE_NAME_4 = "image_4"
        private const val TITLE = "Фотографии квартиры"
        private const val ACTION_TITLE = "Фото квартиры"
    }
}
