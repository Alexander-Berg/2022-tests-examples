package com.yandex.mobile.realty.test.yandexrent

import androidx.test.filters.LargeTest
import com.google.gson.JsonObject
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesMarketIntent
import com.yandex.mobile.realty.core.registerMarketIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ConfirmationDialogScreen
import com.yandex.mobile.realty.core.screen.OfferCardScreen
import com.yandex.mobile.realty.core.screen.RentFlatScreen
import com.yandex.mobile.realty.core.screen.RentOwnerOfferPriceChangeScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SimpleInfoScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.error
import com.yandex.mobile.realty.core.webserver.jsonBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.core.webserver.success
import com.yandex.mobile.realty.data.model.proto.ErrorCode
import com.yandex.mobile.realty.feature.rent.ui.R
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.test.services.registerOwnerRentFlat
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author andrey-bgm on 20.05.2022
 */
@LargeTest
class OwnerOfferStatsTests : BaseTest() {

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
    fun showNormalActivityLevelNotification() {
        configureWebServer {
            registerOwnerRentFlat(notification = statsNotification("NORMAL"))
            registerOffer()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            offerStatsNotificationItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { viewsInfoButton.click() }

            onScreen<SimpleInfoScreen> {
                dialogView.waitUntil { isCompletelyDisplayed() }
                    .isViewStateMatches(getTestRelatedFilePath("viewsInfo"))
                pressBack()
            }

            offerStatsNotificationItem.view.openOfferButton.click()
        }

        onScreen<OfferCardScreen> {
            waitUntil { floatingCommButtons.isCompletelyDisplayed() }
        }
    }

    @Test
    fun showModerateActivityLevelNotification() {
        configureWebServer {
            registerOwnerRentFlat(notification = statsNotification("MODERATE"))
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            offerStatsNotificationItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
        }
    }

    @Test
    fun showLowActivityLevelNotification() {
        configureWebServer {
            registerOwnerRentFlat(notification = statsNotification("LOW"))
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            offerStatsNotificationItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
        }
    }

    @Test
    fun changeOwnerPrice() {
        configureWebServer {
            registerOwnerRentFlat(notification = statsNotification("MODERATE"))
            registerCalcOfferPrice()
            registerSubmitOwnerPrice()
            registerOwnerRentFlat(notification = statsNotification("NORMAL"))
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            offerStatsNotificationItem
                .waitUntil { listView.contains(this) }
                .invoke { changePriceButton.click() }
        }

        onScreen<RentOwnerOfferPriceChangeScreen> {
            waitUntil { listView.contains(priceInputItem) }

            isViewStateMatches(getTestRelatedFilePath("initial"))
            listView.scrollTo(priceInputItem)
                .invoke { inputView.replaceText("91000") }

            priceInputHintItem.waitUntil { view.containsText("будет указано в\u00A0объявлении") }
            isViewStateMatches(getTestRelatedFilePath("filled"))
            listView.scrollTo(buttonsItem)
            saveButton.click()

            successItem
                .waitUntil { listView.contains(this) }
                .also { isViewStateMatches(getTestRelatedFilePath("success")) }
                .invoke { okButton.click() }
        }

        onScreen<RentFlatScreen> {
            offerStatsNotificationItem.waitUntil {
                listView.contains(this)
                view.changePriceButton.isHidden()
            }
        }
    }

    @Test
    fun showErrors() {
        configureWebServer {
            registerOwnerRentFlat(notification = statsNotification("MODERATE"))
            registerCalcOfferPriceError()
            registerCalcOfferPrice()
            registerSubmitOwnerPriceError(validationError("/newRentalValue"))
            registerSubmitOwnerPriceError(validationError("/unknownField"))
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            offerStatsNotificationItem
                .waitUntil { listView.contains(this) }
                .invoke { changePriceButton.click() }
        }

        registerMarketIntent()

        onScreen<RentOwnerOfferPriceChangeScreen> {
            waitUntil { listView.contains(priceInputItem) }

            listView.scrollTo(priceInputItem)
                .invoke { inputView.replaceText("91000") }
            priceInputHintItem
                .waitUntil { view.containsText(getResourceString(R.string.error_try_again)) }
                .also { isViewStateMatches(getTestRelatedFilePath("priceCalcError")) }
                .invoke { tapOnLinkText(R.string.retry) }

            priceInputHintItem.waitUntil { view.containsText("будет указано в\u00A0объявлении") }
            listView.scrollTo(buttonsItem)
            saveButton.click()

            priceInputHintItem.waitUntil { view.containsText("Цена должна быть снижена") }
            isViewStateMatches(getTestRelatedFilePath("validationError"))
            saveButton.click()

            onScreen<ConfirmationDialogScreen> {
                waitUntil { titleView.isCompletelyDisplayed() }
                root.isViewStateMatches("dialog/needAppUpdateDialog")
                confirmButton.click()
                intended(matchesMarketIntent())
            }

            saveButton.click()

            waitUntil {
                toastView(getResourceString(R.string.error_try_again))
                    .isCompletelyDisplayed()
            }
        }
    }

    private fun statsNotification(activityLevel: String): JsonObject {
        return jsonObject {
            "tenantSearchStats" to jsonObject {
                "daysInExposition" to 14
                "views" to 226
                "calls" to 15
                "showings" to 6
                "applications" to 3
                "offerId" to "2588476662299073687"
                "activityLevel" to activityLevel
                "currentRentalValue" to "10000000"
                "currentAdValue" to "10850000"
            }
        }
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
                queryParam("id", OFFER_ID)
            },
            response {
                assetBody("RentFlatNotificationsTest/offerCard.json")
            }
        )
    }

    private fun DispatcherRegistry.registerCalcOfferPrice() {
        register(
            request {
                path("2.0/rent/user/me/flats/$FLAT_ID/calc-rent-price")
                method("POST")
                jsonBody {
                    "rentalValue" to 9_100_000
                }
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "adValue" to 9_250_000
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerCalcOfferPriceError() {
        register(
            request {
                path("2.0/rent/user/me/flats/$FLAT_ID/calc-rent-price")
                method("POST")
            },
            error()
        )
    }

    private fun DispatcherRegistry.registerSubmitOwnerPrice() {
        register(
            request {
                path("2.0/rent/user/me/flats/$FLAT_ID/rent-price")
                method("PUT")
                jsonBody {
                    "newRentalValue" to 9_100_000
                }
            },
            success()
        )
    }

    private fun DispatcherRegistry.registerSubmitOwnerPriceError(error: JsonObject) {
        register(
            request {
                path("2.0/rent/user/me/flats/$FLAT_ID/rent-price")
                method("PUT")
            },
            response {
                setResponseCode(400)
                jsonBody {
                    "error" to error
                }
            }
        )
    }

    private fun validationError(parameter: String): JsonObject {
        return jsonObject {
            "code" to ErrorCode.VALIDATION_ERROR.name
            "message" to "error message"
            "data" to jsonObject {
                "validationErrors" to jsonArrayOf(
                    jsonObject {
                        "parameter" to parameter
                        "code" to "code"
                        "localizedDescription" to "Цена должна быть снижена"
                    }
                )
            }
        }
    }

    private companion object {

        const val OFFER_ID = "2588476662299073687"
    }
}
