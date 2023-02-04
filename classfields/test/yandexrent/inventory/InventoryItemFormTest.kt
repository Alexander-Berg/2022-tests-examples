package com.yandex.mobile.realty.test.yandexrent.inventory

import androidx.test.filters.LargeTest
import com.google.gson.JsonObject
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
import com.yandex.mobile.realty.core.view.TRecyclerItem
import com.yandex.mobile.realty.core.view.TSelectedImageView
import com.yandex.mobile.realty.core.view.TView
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
class InventoryItemFormTest : BaseTest() {

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
    fun fillEmptyItemForm() {
        configureWebServer {
            registerLastInventory(
                inventoryResponse = inventoryResponse(
                    rooms = emptyRooms()
                )
            )
            registerUploadPhoto(ITEM_IMAGE_URL)
            registerUploadPhoto(DEFECT_IMAGE_URL)
            registerInventoryEdit(
                rooms = roomsWithItem(
                    item(
                        id = ItemId(1), name = NEW_ITEM_NAME,
                        images = listOf(rentImage(ITEM_IMAGE_URL)),
                        defectId = DefectId(1)
                    )
                ),
                defects = listOf(
                    defect(
                        id = DefectId(1),
                        description = NEW_DEFECT_COMMENT,
                        images = listOf(rentImage(DEFECT_IMAGE_URL))
                    )
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<InventoryFormScreen> {
            newRoomObjectItem(RoomId(1))
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<InventoryItemFormScreen> {
            waitUntil { listView.contains(saveButton) }
            isContentStateMatches(getTestRelatedFilePath("emptyForm"))

            updateItemName()
            selectImage(addItemImagesButton, ITEM_IMAGE_NAME).click()

            onScreen<GalleryScreen> {
                waitUntil { photoView.isCompletelyDisplayed() }
                photoView.isViewStateMatches(getTestRelatedFilePath("itemImage"))
                pressBack()
            }

            listView.scrollTo(countInputItem).invoke {
                incrementView.click()
                isViewStateMatches(getTestRelatedFilePath("countInput"))
                decrementView.click()
            }

            listView.scrollTo(hasDefectItem).click()
            listView.waitUntil { contains(addDefectImagesButton) }
            listView.scrollTo(hasDefectItem).click()
            listView.waitUntil { doesNotContain(addItemImagesButton) }
            listView.scrollTo(hasDefectItem).click()

            selectImage(addDefectImagesButton, DEFECT_IMAGE_NAME).click()

            onScreen<GalleryScreen> {
                waitUntil { photoView.isCompletelyDisplayed() }
                photoView.isViewStateMatches(getTestRelatedFilePath("defectImage"))
                pressBack()
            }

            updateComment()

            isContentStateMatches(getTestRelatedFilePath("filledForm"))

            listView.scrollTo(saveButton)
                .click()
        }

        onScreen<InventoryFormScreen> {
            roomObjectItem(ItemId(1))
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("updatedItem"))
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
                    PARAMETER_ITEM_IMAGES to VALIDATION_ERROR_ITEM_IMAGES,
                    PARAMETER_ITEM_NAME to VALIDATION_ERROR_ITEM_NAME,
                    PARAMETER_DEFECT_COMMENT to VALIDATION_ERROR_DEFECT_COMMENT,
                    PARAMETER_DEFECT_IMAGES to VALIDATION_ERROR_DEFECT_IMAGES,
                )
            )
            registerInventoryEditError()
            registerUploadPhotoError()
            registerUploadPhoto(ITEM_IMAGE_URL)
            registerInventoryEditValidationErrors(
                validationErrors = listOf(
                    "rooms/0/items/0/unknown_parameter" to VALIDATION_ERROR_DEFECT_COMMENT
                )
            )
            registerInventoryEditValidationErrors(
                validationErrors = listOf(
                    "rooms/2/items/1/$PARAMETER_ITEM_NAME" to VALIDATION_ERROR_ITEM_NAME
                )
            )
            registerInventoryEdit(
                rooms = roomsWithItem(
                    item(
                        id = ItemId(1), name = NEW_ITEM_NAME,
                        images = listOf(rentImage(ITEM_IMAGE_URL)),
                        defectId = DefectId(1)
                    )
                ),
                defects = listOf(
                    defect(
                        id = DefectId(1),
                        description = NEW_DEFECT_COMMENT,
                        images = listOf(rentImage(DEFECT_IMAGE_URL))
                    )
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<InventoryFormScreen> {
            newRoomObjectItem(RoomId(1))
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<InventoryItemFormScreen> {
            hasDefectItem
                .waitUntil { listView.contains(this) }
                .click()

            listView.scrollTo(saveButton)
                .click()

            fieldErrorItem(VALIDATION_ERROR_DEFECT_COMMENT)
                .waitUntil { listView.contains(this) }

            isContentStateMatches(getTestRelatedFilePath("fieldErrors"))

            updateItemName()
            updateComment()

            listView.scrollTo(saveButton)
                .click()

            toastView(getResourceString(R.string.error_try_again))
                .waitUntil { isCompletelyDisplayed() }

            val selectedItemImage = selectImage(
                addItemImagesButton,
                ITEM_IMAGE_NAME,
                checkLoaded = false
            )

            selectImage(
                addItemImageButton,
                ITEM_IMAGE_NAME,
                checkLoaded = false
            )

            waitUntil {
                toastView("Изображения уже были выбраны ранее").isCompletelyDisplayed()
            }

            listView.scrollTo(saveButton)
                .click()

            onScreen<ConfirmationDialogScreen> {
                waitUntil { titleView.isCompletelyDisplayed() }
                root.isViewStateMatches(getTestRelatedFilePath("imageErrorDialog"))
                cancelButton.click()
            }

            listView.scrollToTop()
            selectedItemImage.retryButton.click()

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
            roomObjectItem(ItemId(1))
                .waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun updateAndDeleteItemForm() {
        configureWebServer {
            registerLastInventory(
                inventoryResponse = inventoryResponse(
                    rooms = roomsWithItem(
                        item(
                            id = ItemId(1), name = "Old item name",
                            images = listOf(rentImage(ITEM_IMAGE_URL, OLD_IMAGE_KEY)),
                            defectId = DefectId(1)
                        )
                    ),
                    defects = listOf(
                        defect(
                            id = DefectId(1),
                            description = "Old defect description",
                            images = listOf(rentImage(DEFECT_IMAGE_URL))
                        )
                    )
                )
            )
            registerUploadPhoto(ITEM_IMAGE_URL)
            registerInventoryEdit(
                rooms = roomsWithItem(
                    item(
                        id = ItemId(1), name = NEW_ITEM_NAME,
                        images = listOf(rentImage(ITEM_IMAGE_URL))
                    )
                )
            )
            registerInventoryEdit(rooms = emptyRooms())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<InventoryFormScreen> {
            roomObjectItem(ItemId(1))
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<InventoryItemFormScreen> {
            waitUntil { listView.contains(saveButton) }
            isContentStateMatches(getTestRelatedFilePath("filledForm"))

            listView.scrollTo(imageItem(OLD_IMAGE_KEY))
                .invoke { deleteButton.click() }

            selectImage(addItemImagesButton, ITEM_IMAGE_NAME)
            listView.scrollTo(hasDefectItem)
                .click()

            listView.scrollTo(saveButton)
                .click()
        }

        onScreen<InventoryFormScreen> {
            roomObjectItem(ItemId(1))
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("removedDefect"))
                .click()
        }

        onScreen<InventoryItemFormScreen> {
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
            listView.doesNotContain(roomObjectItem(ItemId(1)))
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
            newRoomObjectItem(RoomId(1))
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<InventoryItemFormScreen> {
            waitUntil { listView.contains(saveButton) }

            updateItemName()
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

    private fun roomsWithItem(item: JsonObject): List<JsonObject> {
        return listOf(
            room(RoomId(1), "Кухня", listOf(item)),
            room(RoomId(2), "Гостиная", emptyList()),
            room(RoomId(3), "Ванная", emptyList()),
        )
    }

    private fun InventoryItemFormScreen.selectImage(
        addButton: TRecyclerItem<TView>,
        imageName: String,
        checkLoaded: Boolean = true
    ): TSelectedImageView {
        val key = registerGetImageIntent(imageName)
        addButton
            .waitUntil { listView.contains(this) }
            .click()
        onScreen<ChooseMediaDialogScreen> {
            galleryButton.click()
        }
        return imageItem(key)
            .waitUntil { listView.contains(this) }
            .apply {
                if (checkLoaded) {
                    waitUntil { isImageLoaded() }
                }
            }
    }

    private fun InventoryItemFormScreen.updateItemName() {
        listView.scrollTo(itemNameInputItem)
            .inputView
            .replaceText(NEW_ITEM_NAME)
    }

    private fun InventoryItemFormScreen.updateComment() {
        listView.scrollTo(defectCommentItem)
            .click()

        onScreen<RentCommentScreen> {
            waitUntil { messageView.isCompletelyDisplayed() }
            messageView.replaceText(NEW_DEFECT_COMMENT)
            doneButton.click()
        }
    }

    private fun registerGetImageIntent(imageName: String): String {
        return createMockImageAndGetUriString(
            mockName = imageName,
            fileName = imageName
        ).also { registerGetContentIntent(it) }
    }

    private fun DispatcherRegistry.registerUploadPhoto(imageUrl: String) {
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
                        "name" to "name"
                        "url" to imageUrl
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

        const val NEW_ITEM_NAME = "New name"
        const val NEW_DEFECT_COMMENT = "New defect"
        const val ITEM_IMAGE_NAME = "table.webp"
        const val DEFECT_IMAGE_NAME = "defect.webp"
        const val OLD_IMAGE_KEY = "old_item_image"
        const val PARAMETER_ITEM_NAME = "rooms/0/items/0/item_name"
        const val VALIDATION_ERROR_ITEM_NAME = "name validation error"
        const val PARAMETER_ITEM_IMAGES = "rooms/0/items/0/photos"
        const val VALIDATION_ERROR_ITEM_IMAGES = "item images validation error"
        const val PARAMETER_DEFECT_COMMENT = "defects/0/description"
        const val VALIDATION_ERROR_DEFECT_COMMENT = "defect comment validation error"
        const val PARAMETER_DEFECT_IMAGES = "defects/0/photos"
        const val VALIDATION_ERROR_DEFECT_IMAGES = "defect images validation error"
        private const val UPLOAD_PATH = "upload"
        private const val UPLOAD_URL = "https://localhost:8080/$UPLOAD_PATH"
    }
}
