package com.yandex.mobile.realty.test.yandexrent.inventory

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.RentInventoryFormActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ConfirmationDialogScreen
import com.yandex.mobile.realty.core.screen.InventoryFormScreen
import com.yandex.mobile.realty.core.screen.InventoryRoomFormScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.domain.model.yandexrent.InventoryFormContext
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.registerOwnerRentFlat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author sorokinandrei on 4/14/22
 */
@LargeTest
class InventoryRoomFormTest : BaseTest() {

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
    fun fillEmptyRoomForm() {
        configureWebServer {
            registerLastInventory(
                inventoryResponse = inventoryResponse(
                    rooms = emptyRooms()
                )
            )
            registerInventoryEdit(
                rooms = listOf(
                    room(id = RoomId(1), name = PRESET_ROOM_NAME, emptyList())
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<InventoryFormScreen> {
            newRoomButtonItem
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<InventoryRoomFormScreen> {
            waitUntil { listView.contains(saveButton) }
            isContentStateMatches(getTestRelatedFilePath("emptyForm"))

            listView.scrollTo(inputItem)
                .inputView
                .typeText(NEW_ROOM_NAME)

            closeKeyboard()
            isContentStateMatches(getTestRelatedFilePath("filledForm"))

            presetItem(PRESET_ROOM_NAME)
                .click()

            isContentStateMatches(getTestRelatedFilePath("presetForm"))

            listView.scrollTo(saveButton)
                .click()
        }

        onScreen<InventoryFormScreen> {
            roomHeaderItem(RoomId(1))
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("updatedRoom"))
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
                validationErrors = listOf(PARAMETER_ROOM_NAME to VALIDATION_ERROR)
            )
            registerInventoryEditError()
            registerInventoryEditValidationErrors(
                validationErrors = listOf("rooms/3/unknown_parameter" to VALIDATION_ERROR)
            )
            registerInventoryEditValidationErrors(
                validationErrors = listOf("rooms/1/$PARAMETER_ROOM_NAME" to VALIDATION_ERROR)
            )
            registerInventoryEdit(
                rooms = listOf(
                    room(id = RoomId(1), name = PRESET_ROOM_NAME, emptyList())
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<InventoryFormScreen> {
            newRoomButtonItem
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<InventoryRoomFormScreen> {
            saveButton
                .waitUntil { listView.contains(this) }
                .click()

            fieldErrorItem(VALIDATION_ERROR)
                .waitUntil { listView.contains(this) }

            isContentStateMatches(getTestRelatedFilePath("fieldError"))

            presetItem(PRESET_ROOM_NAME)
                .click()

            listView.scrollTo(saveButton)
                .click()

            toastView(getResourceString(R.string.error_try_again))
                .waitUntil { isCompletelyDisplayed() }

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
            roomHeaderItem(RoomId(1))
                .waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun updateAndDeleteRoomForm() {
        configureWebServer {
            registerLastInventory(
                inventoryResponse = inventoryResponse(
                    rooms = listOf(
                        room(id = RoomId(1), name = "Old room name", emptyList())
                    )
                )
            )
            registerInventoryEdit(
                rooms = listOf(
                    room(id = RoomId(1), name = NEW_ROOM_NAME, emptyList())
                )
            )
            registerInventoryEdit(rooms = emptyList())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<InventoryFormScreen> {
            roomHeaderItem(RoomId(1))
                .waitUntil { listView.contains(this) }
                .invoke { editButton.click() }
        }

        onScreen<InventoryRoomFormScreen> {
            waitUntil { listView.contains(saveButton) }

            closeKeyboard()
            isContentStateMatches(getTestRelatedFilePath("filledForm"))

            listView.scrollTo(inputItem)
                .inputView
                .run {
                    clearText()
                    typeText(NEW_ROOM_NAME)
                }

            listView.scrollTo(saveButton)
                .click()
        }

        onScreen<InventoryFormScreen> {
            roomHeaderItem(RoomId(1))
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches(getTestRelatedFilePath("updatedName"))
                    editButton.click()
                }
        }

        onScreen<InventoryRoomFormScreen> {
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
            listView.doesNotContain(roomHeaderItem(RoomId(1)))
        }
    }

    @Test
    fun shouldShowRejectChangesDialog() {
        configureWebServer {
            registerLastInventory(
                inventoryResponse = inventoryResponse(
                    rooms = emptyRooms()
                )
            )

            registerOwnerRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<InventoryFormScreen> {
            newRoomButtonItem
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<InventoryRoomFormScreen> {
            waitUntil { listView.contains(saveButton) }

            listView.scrollTo(inputItem)
                .inputView
                .typeText(NEW_ROOM_NAME)

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

    private companion object {

        const val NEW_ROOM_NAME = "New room"
        const val PRESET_ROOM_NAME = "Ванная"
        const val VALIDATION_ERROR = "roomName error"
        const val PARAMETER_ROOM_NAME = "rooms/3/room_name"
    }
}
