package com.yandex.mobile.realty.test.yandexrent.inventory

import androidx.test.filters.LargeTest
import com.google.gson.JsonObject
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.InventoryFormScreen
import com.yandex.mobile.realty.core.screen.InventoryPreviewScreen
import com.yandex.mobile.realty.core.screen.InventoryRoomFormScreen
import com.yandex.mobile.realty.core.screen.RentFlatScreen
import com.yandex.mobile.realty.core.screen.RentSmsConfirmationScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SimpleInfoScreen
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.test.services.RENT_ROLE_OWNER
import com.yandex.mobile.realty.test.services.RENT_ROLE_TENANT
import com.yandex.mobile.realty.test.services.registerOwnerRentFlat
import com.yandex.mobile.realty.test.services.registerRentFlat
import com.yandex.mobile.realty.test.yandexrent.OWNER_REQUEST_ID
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author misha-kozlov on 21.04.2022
 */
@LargeTest
class InventorySigningTest : BaseTest() {

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
    fun shouldSignOwnerInventory() {
        shouldSignInventory(
            rentRole = RENT_ROLE_OWNER,
            notification = jsonObject {
                "ownerNeedToConfirmInventory" to jsonObject {
                    "ownerRequestId" to OWNER_REQUEST_ID
                }
            },
            actions = listOf(ACTION_EDIT, ACTION_CONFIRM),
            successAction = { successItem.view.invoke { okButton.click() } }
        )
    }

    @Test
    fun shouldSignTenantInventory() {
        shouldSignInventory(
            rentRole = RENT_ROLE_TENANT,
            notification = jsonObject {
                "tenantNeedToConfirmInventory" to jsonObject {
                    "ownerRequestId" to OWNER_REQUEST_ID
                }
            },
            actions = listOf(ACTION_CONFIRM),
            successAction = { pressBack() }
        )
    }

    @Test
    fun shouldShowAlreadySignedOwnerInventory() {
        shouldShowAlreadySignedInventory(
            rentRole = RENT_ROLE_OWNER,
            notification = jsonObject {
                "ownerNeedToConfirmInventory" to jsonObject {
                    "ownerRequestId" to OWNER_REQUEST_ID
                }
            },
            confirmedByOwner = true
        )
    }

    @Test
    fun shouldShowAlreadySignedTenantInventory() {
        shouldShowAlreadySignedInventory(
            rentRole = RENT_ROLE_TENANT,
            notification = jsonObject {
                "tenantNeedToConfirmInventory" to jsonObject {
                    "ownerRequestId" to OWNER_REQUEST_ID
                }
            },
            confirmedByTenant = true
        )
    }

    @Test
    fun shouldEditOwnerInventoryBeforeSigning() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "ownerNeedToConfirmInventory" to jsonObject {
                        "ownerRequestId" to OWNER_REQUEST_ID
                    }
                }
            )
            registerLastInventory(
                inventoryResponse = inventoryResponse(
                    actions = listOf(ACTION_CONFIRM, ACTION_EDIT)
                )
            )
            registerLastInventory(
                inventoryResponse = inventoryResponse(
                    actions = listOf(ACTION_CONFIRM, ACTION_EDIT)
                )
            )
            registerInventoryEdit(rooms = sampleRooms())
            registerLastInventory(
                inventoryResponse = inventoryResponse(
                    rooms = sampleRooms(),
                    actions = listOf(ACTION_CONFIRM, ACTION_EDIT)
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(NOTIFICATION_CONFIRM_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<InventoryPreviewScreen> {
            editButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

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

            listView.scrollTo(saveButton).click()
        }

        onScreen<InventoryFormScreen> {
            proceedButton
                .waitUntil { isCompletelyDisplayed() }
                .apply {
                    click()
                    click()
                }
        }

        onScreen<InventoryPreviewScreen> {
            roomHeaderItem(NEW_ROOM_ID).waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldShowManagerComment() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "ownerNeedToConfirmInventory" to jsonObject {
                        "ownerRequestId" to OWNER_REQUEST_ID
                    }
                }
            )
            registerLastInventory(
                inventoryResponse = inventoryResponse(
                    managerComment = MANAGER_COMMENT
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(NOTIFICATION_CONFIRM_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<InventoryPreviewScreen> {
            managerCommentItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("managerComment"))
                .click()

            onScreen<SimpleInfoScreen> {
                dialogView
                    .waitUntil { isCompletelyDisplayed() }
                    .isViewStateMatches(getTestRelatedFilePath("infoView"))
            }
        }
    }

    private fun shouldSignInventory(
        rentRole: String,
        notification: JsonObject,
        actions: List<String>,
        successAction: RentSmsConfirmationScreen.() -> Unit
    ) {
        configureWebServer {
            registerRentFlat(rentRole = rentRole, notifications = listOf(notification))
            registerLastInventory(
                inventoryResponse = inventoryResponse(
                    rooms = emptyRooms(),
                    actions = actions
                )
            )
            registerInventorySmsCodeRequest()
            registerInventorySmsCodeSubmit(SMS_CODE)
            registerRentFlat(rentRole = rentRole)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(NOTIFICATION_CONFIRM_TITLE)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }

        onScreen<InventoryPreviewScreen> {
            signButton.waitUntil { isCompletelyDisplayed() }

            root.isViewStateMatches(getTestRelatedFilePath("inventory"))

            signButton.click()
        }

        onScreen<RentSmsConfirmationScreen> {
            smsCodeItem
                .waitUntil { listView.contains(this) }
                .invoke { inputView.typeText(SMS_CODE, closeKeyboard = false) }

            successItem
                .waitUntil { listView.contains(this) }
                .also {
                    root.isViewStateMatches(getTestRelatedFilePath("successContent"))
                }

            successAction.invoke(this)
        }

        onScreen<RentFlatScreen> {
            listView.waitUntil {
                contains(flatHeaderItem)
                doesNotContain(notificationItem(NOTIFICATION_CONFIRM_TITLE))
            }
        }
    }

    private fun shouldShowAlreadySignedInventory(
        rentRole: String,
        notification: JsonObject,
        confirmedByOwner: Boolean = false,
        confirmedByTenant: Boolean = false,
    ) {
        configureWebServer {
            registerRentFlat(rentRole = rentRole, notifications = listOf(notification))
            registerLastInventory(
                inventoryResponse = inventoryResponse(
                    confirmedByOwner = confirmedByOwner,
                    confirmedByTenant = confirmedByTenant
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(NOTIFICATION_CONFIRM_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }
        onScreen<InventoryPreviewScreen> {
            successItem
                .waitUntil { listView.contains(this) }
                .also { root.isViewStateMatches(getTestRelatedFilePath("success")) }
                .invoke { okButton.click() }
        }
        onScreen<RentFlatScreen> {
            flatHeaderItem.waitUntil { listView.contains(this) }
        }
    }

    private fun sampleRooms(): List<JsonObject> {
        return listOf(
            room(id = NEW_ROOM_ID, name = NEW_ROOM_NAME, emptyList())
        )
    }

    private companion object {

        const val NOTIFICATION_CONFIRM_TITLE = "Опись имущества"
        const val SMS_CODE = "00000"
        const val NEW_ROOM_NAME = "New room"
        const val MANAGER_COMMENT = "Это текст комментария менеджера"
        val NEW_ROOM_ID = RoomId(1)
    }
}
