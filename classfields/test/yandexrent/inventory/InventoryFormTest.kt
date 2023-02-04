package com.yandex.mobile.realty.test.yandexrent.inventory

import androidx.test.filters.LargeTest
import com.google.gson.JsonObject
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesExternalViewUrlIntent
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.*
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.test.services.registerOwnerRentFlat
import com.yandex.mobile.realty.test.yandexrent.OWNER_REQUEST_ID
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author sorokinandrei on 4/14/22
 */
@LargeTest
class InventoryFormTest : BaseTest() {

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
    fun showManagerComment() {
        configureWebServer {
            registerOwnerRentFlat(notification = fillFormNotification())
            registerLastInventory(
                inventoryResponse = inventoryResponse(
                    managerComment = MANAGER_COMMENT,
                    rooms = sampleRooms()
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(NOTIFICATION_FILL_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<InventoryFormScreen> {
            waitUntil { proceedButton.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("screen"))
            listView.scrollTo(managerCommentItem).click()
        }

        onScreen<SimpleInfoScreen> {
            dialogView
                .waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(getTestRelatedFilePath("infoView"))
        }
    }

    @Test
    fun showEmptyInventoryForm() {
        configureWebServer {
            registerOwnerRentFlat(notification = fillFormNotification())
            repeat(2) {
                registerLastInventory(
                    inventoryResponse = inventoryResponse(
                        rooms = emptyRooms()
                    )
                )
            }
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(NOTIFICATION_FILL_TITLE)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }

        onScreen<InventoryFormScreen> {
            waitUntil { proceedButton.isCompletelyDisplayed() }
            listView.isContentStateMatches(getTestRelatedFilePath("rooms"))

            proceedButton.click()
            listView.isContentStateMatches(getTestRelatedFilePath("defects"))

            proceedButton.click()
        }
        onScreen<InventoryPreviewScreen> {
            waitUntil { saveButton.isCompletelyDisplayed() }
            listView.isContentStateMatches(getTestRelatedFilePath("preview"))
        }
    }

    @Test
    fun showFilledInventoryForm() {
        configureWebServer {
            registerOwnerRentFlat(notification = fillFormNotification())
            repeat(2) {
                registerLastInventory(
                    ownerRequestId = OWNER_REQUEST_ID,
                    inventoryResponse = inventoryResponse(
                        rooms = sampleRooms(),
                        defects = sampleDefects()
                    )
                )
            }
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(NOTIFICATION_FILL_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<InventoryFormScreen> {
            waitUntil { proceedButton.isCompletelyDisplayed() }
            listView.isContentStateMatches(getTestRelatedFilePath("rooms"))

            proceedButton.click()
            listView.isContentStateMatches(getTestRelatedFilePath("defects"))

            proceedButton.click()
        }

        onScreen<InventoryPreviewScreen> {
            waitUntil { saveButton.isCompletelyDisplayed() }
            listView.isContentStateMatches(getTestRelatedFilePath("preview"))

            listView.scrollTo(roomItem(ItemId(1)))
                .click()
        }
        onScreen<InventoryItemPreviewScreen> {
            listView.isContentStateMatches(getTestRelatedFilePath("emptyItem"))
            pressBack()
        }

        onScreen<InventoryPreviewScreen> {
            listView.scrollTo(roomItem(ItemId(2)))
                .click()
        }
        onScreen<InventoryItemPreviewScreen> {
            listView.isContentStateMatches(getTestRelatedFilePath("fullItem"))
            listView.scrollTo(imageItem(ITEM_IMAGE_URL)).click()

            onScreen<GalleryScreen> {
                waitUntil { photoView.isCompletelyDisplayed() }
                photoView.isViewStateMatches(getTestRelatedFilePath("itemImage"))
                pressBack()
            }
            listView.scrollTo(imageItem(DEFECT_IMAGE_URL)).click()
            onScreen<GalleryScreen> {
                waitUntil { photoView.isCompletelyDisplayed() }
                photoView.isViewStateMatches(getTestRelatedFilePath("defectImage"))
                pressBack()
            }
            pressBack()
        }

        onScreen<InventoryPreviewScreen> {
            listView.scrollTo(roomItem(ItemId(6)))
                .click()
        }
        onScreen<InventoryItemPreviewScreen> {
            listView.isContentStateMatches(getTestRelatedFilePath("onlyDefectItem"))
            pressBack()
        }

        onScreen<InventoryPreviewScreen> {
            listView.scrollTo(defectItem(DefectId(4))).click()
        }
        onScreen<InventoryItemPreviewScreen> {
            listView.isContentStateMatches(getTestRelatedFilePath("defect"))
            pressBack()
        }
        onScreen<InventoryPreviewScreen> {
            saveButton
                .waitUntil { isCompletelyDisplayed() }
                .click()

            successItem
                .waitUntil { listView.contains(this) }
                .also { root.isViewStateMatches(getTestRelatedFilePath("success")) }
                .invoke { okButton.click() }
        }
        onScreen<RentFlatScreen> {
            flatHeaderItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun showInventoryFormErrors() {
        configureWebServer {
            registerOwnerRentFlat(notification = fillFormNotification())
            registerLastInventoryError(OWNER_REQUEST_ID)
            registerLastInventory(
                ownerRequestId = OWNER_REQUEST_ID,
                inventoryResponse = inventoryResponse(
                    rooms = emptyRooms()
                )
            )
            registerLastInventoryError(OWNER_REQUEST_ID)
            registerLastInventory(
                ownerRequestId = OWNER_REQUEST_ID,
                inventoryResponse = inventoryResponse(
                    rooms = emptyRooms()
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(NOTIFICATION_FILL_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<InventoryFormScreen> {
            fullscreenErrorView
                .waitUntil { isCompletelyDisplayed() }
                .retryButton
                .click()

            proceedButton
                .waitUntil { isCompletelyDisplayed() }
                .click()

            proceedButton
                .click()
        }

        onScreen<InventoryPreviewScreen> {
            fullscreenErrorView
                .waitUntil { isCompletelyDisplayed() }
                .retryButton
                .click()

            waitUntil { saveButton.isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldOpenTutorialLink() {
        configureWebServer {
            registerOwnerRentFlat(notification = fillFormNotification())
            registerLastInventory(
                ownerRequestId = OWNER_REQUEST_ID,
                inventoryResponse = inventoryResponse(
                    rooms = emptyRooms()
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerResultOkIntent(matchesExternalViewUrlIntent(TUTORIAL_URL), null)

        onScreen<RentFlatScreen> {
            notificationItem(NOTIFICATION_FILL_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<InventoryFormScreen> {
            alertItem
                .waitUntil { listView.contains(this) }
                .invoke { textView.tapOnLinkText(R.string.yandex_rent_inventory_alert_link) }

            intended(matchesExternalViewUrlIntent(TUTORIAL_URL))
        }
    }

    private fun fillFormNotification(): JsonObject {
        return jsonObject {
            "ownerNeedToFillOutInventory" to jsonObject {
                "ownerRequestId" to OWNER_REQUEST_ID
            }
        }
    }

    private companion object {

        const val NOTIFICATION_FILL_TITLE = "Создайте опись имущества"
        const val TUTORIAL_URL = "https://realty.yandex.ru/export/arenda/inventory_owner_guide.pdf"
        const val MANAGER_COMMENT = "Это текст комментария менеджера"
    }
}
