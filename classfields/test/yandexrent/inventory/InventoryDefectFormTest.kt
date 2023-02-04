package com.yandex.mobile.realty.test.yandexrent.inventory

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.RentInventoryFormActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.createMockImageAndGetUriString
import com.yandex.mobile.realty.core.registerGetContentIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.*
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.view.TSelectedImageView
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.domain.model.yandexrent.InventoryFormContext
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.registerOwnerRentFlat
import com.yandex.mobile.realty.test.yandexrent.OWNER_REQUEST_ID
import com.yandex.mobile.realty.test.yandexrent.rentImage
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author sorokinandrei on 4/14/22
 */
@LargeTest
class InventoryDefectFormTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = RentInventoryFormActivityTestRule(
        context = InventoryFormContext.Flat.FillInventoryNotification,
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
    fun fillEmptyDefectForm() {
        configureWebServer {
            registerLastInventory(
                inventoryResponse = inventoryResponse(
                    rooms = emptyRooms()
                )
            )
            registerInventoryEdit(
                rooms = emptyRooms(),
                defects = listOf(defect(DefectId(1), NEW_DEFECT_COMMENT))
            )
            registerUploadPhoto(IMAGE_NAME)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<InventoryFormScreen> {
            proceedButton
                .waitUntil { isCompletelyDisplayed() }
                .click()

            newDefectItem
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<InventoryDefectFormScreen> {
            waitUntil { listView.contains(saveButton) }
            listView.isContentStateMatches(getTestRelatedFilePath("emptyForm"))

            updateComment()

            selectImage(1).click()

            onScreen<GalleryScreen> {
                waitUntil { photoView.isCompletelyDisplayed() }
                pressBack()
            }

            listView.isContentStateMatches(getTestRelatedFilePath("filledForm"))

            listView.scrollTo(saveButton)
                .click()
        }

        onScreen<InventoryFormScreen> {
            defectItem(DefectId(1))
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("updatedDefect"))
        }
    }

    @Test
    fun shouldShowErrors() {
        configureWebServer {
            registerLastInventory(
                inventoryResponse = inventoryResponse(
                    rooms = emptyRooms()
                )
            )
            registerInventoryEditValidationErrors(
                validationErrors = listOf(
                    PARAMETER_DEFECT_IMAGES to VALIDATION_ERROR_IMAGES,
                    PARAMETER_DEFECT_COMMENT to VALIDATION_ERROR_COMMENT
                )
            )
            registerInventoryEditError()
            registerUploadPhotoError()
            registerUploadPhoto(IMAGE_NAME)
            registerInventoryEditValidationErrors(
                validationErrors = listOf(
                    "defects/0/unknown_parameter" to VALIDATION_ERROR_COMMENT
                )
            )
            registerInventoryEditValidationErrors(
                validationErrors = listOf(
                    "defects/1/$PARAMETER_DEFECT_COMMENT" to VALIDATION_ERROR_COMMENT
                )
            )
            registerInventoryEdit(
                defects = listOf(defect(id = DefectId(1), description = NEW_DEFECT_COMMENT))
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<InventoryFormScreen> {
            proceedButton
                .waitUntil { isCompletelyDisplayed() }
                .click()

            newDefectItem
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<InventoryDefectFormScreen> {
            saveButton
                .waitUntil { listView.contains(this) }
                .click()

            fieldErrorItem(VALIDATION_ERROR_COMMENT)
                .waitUntil { listView.contains(this) }

            listView.isContentStateMatches(getTestRelatedFilePath("fieldErrors"))

            updateComment()

            listView.scrollTo(saveButton)
                .click()

            toastView(getResourceString(R.string.error_try_again))
                .waitUntil { isCompletelyDisplayed() }

            val selectedImage = selectImage(1, checkLoaded = false)

            listView.scrollTo(saveButton)
                .click()

            onScreen<ConfirmationDialogScreen> {
                waitUntil { titleView.isCompletelyDisplayed() }
                root.isViewStateMatches(getTestRelatedFilePath("imageErrorDialog"))
                cancelButton.click()
            }

            selectedImage.retryButton.click()

            listView.scrollTo(saveButton)
                .click()

            onScreen<ConfirmationDialogScreen> {
                waitUntil { titleView.isCompletelyDisplayed() }
                root.isViewStateMatches("dialog/needAppUpdateDialog")
                cancelButton.click()
            }

            listView.scrollTo(saveButton)
                .click()

            toastView(getResourceString(R.string.yandex_rent_error_contact_manager))
                .waitUntil { isCompletelyDisplayed() }

            listView.scrollTo(saveButton)
                .click()
        }

        onScreen<InventoryFormScreen> {
            defectItem(DefectId(1))
                .waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun updateAndDeleteDefectForm() {
        configureWebServer {
            registerLastInventory(
                inventoryResponse = inventoryResponse(
                    defects = listOf(
                        defect(
                            id = DefectId(1),
                            description = "old comment",
                            images = listOf(rentImage(ITEM_IMAGE_URL))
                        )
                    )
                )
            )
            registerUploadPhoto(IMAGE_NAME)
            registerInventoryEdit(
                OWNER_REQUEST_ID,
                defects = listOf(
                    defect(
                        id = DefectId(1),
                        description = NEW_DEFECT_COMMENT,
                        images = listOf(rentImage(DEFECT_IMAGE_URL))
                    )
                )
            )
            registerInventoryEdit(ownerRequestId = OWNER_REQUEST_ID, defects = emptyList())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<InventoryFormScreen> {
            proceedButton
                .waitUntil { isCompletelyDisplayed() }
                .click()

            defectItem(DefectId(1))
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<InventoryDefectFormScreen> {
            waitUntil { listView.contains(saveButton) }
            listView.isContentStateMatches(getTestRelatedFilePath("filledForm"))

            listView.scrollTo(imageItem(1))
                .invoke { deleteButton.click() }

            selectImage(1)
            updateComment()

            listView.scrollTo(saveButton)
                .click()
        }

        onScreen<InventoryFormScreen> {
            defectItem(DefectId(1))
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("updatedComment"))
                .click()
        }

        onScreen<InventoryDefectFormScreen> {
            deleteButton
                .waitUntil { listView.contains(this) }
                .click()

            onScreen<ConfirmationDialogScreen> {
                waitUntil { titleView.isCompletelyDisplayed() }
                root.isViewStateMatches(getTestRelatedFilePath("deleteConfirm"))
                confirmButton.click()
            }
        }

        onScreen<InventoryFormScreen> {
            waitUntil { proceedButton.isCompletelyDisplayed() }
            listView.doesNotContain(defectItem(DefectId(1)))
        }
    }

    @Test
    fun shouldShowRejectChangesDialog() {
        configureWebServer {
            registerLastInventory(
                ownerRequestId = OWNER_REQUEST_ID,
                inventoryResponse = inventoryResponse(
                    rooms = emptyRooms()
                )
            )
            registerOwnerRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<InventoryFormScreen> {
            proceedButton
                .waitUntil { isCompletelyDisplayed() }
                .click()

            newDefectItem
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<InventoryDefectFormScreen> {
            waitUntil { listView.contains(saveButton) }

            updateComment()
            pressBack()

            onScreen<ConfirmationDialogScreen> {
                waitUntil { titleView.isCompletelyDisplayed() }
                root.isViewStateMatches(getTestRelatedFilePath("rejectChanges"))
                cancelButton.click()
            }

            pressBack()

            onScreen<ConfirmationDialogScreen> {
                confirmButton.click()
            }
            onScreen<InventoryFormScreen> {
                waitUntil { proceedButton.isCompletelyDisplayed() }
            }
        }
    }

    private fun InventoryDefectFormScreen.selectImage(
        imageNumber: Int,
        checkLoaded: Boolean = true
    ): TSelectedImageView {
        registerGetImageIntent(imageNumber)
        addImagesButton
            .waitUntil { listView.contains(this) }
            .click()
        onScreen<ChooseMediaDialogScreen> {
            galleryButton.click()
        }
        return imageItem(number = imageNumber)
            .waitUntil { listView.contains(this) }
            .apply {
                if (checkLoaded) {
                    waitUntil { isImageLoaded() }
                }
            }
    }

    private fun InventoryDefectFormScreen.updateComment() {
        listView.scrollTo(commentItem)
            .click()

        onScreen<RentCommentScreen> {
            waitUntil { messageView.isCompletelyDisplayed() }
            messageView.replaceText(NEW_DEFECT_COMMENT)
            doneButton.click()
        }
    }

    private fun registerGetImageIntent(imageNumber: Int) {
        val uri = createMockImageAndGetUriString(
            mockName = "table.webp",
            fileName = "image_$imageNumber.webp"
        )
        registerGetContentIntent(uri)
    }

    private fun DispatcherRegistry.registerUploadPhoto(imageName: String) {
        register(
            request {
                method("POST")
                path("2.0/files/get-upload-url")
                jsonBody {
                    "entities" to jsonArrayOf(
                        jsonObject {
                            "rentInventory" to jsonObject { }
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
                        "namespace" to "arenda"
                        "groupId" to "group"
                        "name" to imageName
                        "url" to DEFECT_IMAGE_URL
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

    private companion object {

        const val NEW_DEFECT_COMMENT = "New defect"
        const val PARAMETER_DEFECT_COMMENT = "defects/0/description"
        const val VALIDATION_ERROR_COMMENT = "description validation error"
        const val PARAMETER_DEFECT_IMAGES = "defects/0/photos"
        const val VALIDATION_ERROR_IMAGES = "images validation error"
        private const val IMAGE_NAME = "name"
        private const val UPLOAD_PATH = "upload"
        private const val UPLOAD_URL = "https://localhost:8080/$UPLOAD_PATH"
    }
}
