package com.yandex.mobile.realty.test.yandexrent.showings

import com.google.gson.JsonObject
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.jsonBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.core.webserver.success
import com.yandex.mobile.realty.data.model.proto.ErrorCode
import com.yandex.mobile.realty.test.services.FLAT_ADDRESS
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.test.yandexrent.SHOWING_ID
import com.yandex.mobile.realty.test.yandexrent.rentImage
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import com.yandex.mobile.realty.utils.toJsonArray

object Showings {

    const val SHOWING_TYPE_INFO = "INFO"
    const val SHOWING_TYPE_ACCENT = "ACCENT"
    const val SHOWING_TYPE_IMPORTANT = "IMPORTANT"
    const val SHOWING_TYPE_WARNING = "WARNING"
    const val SHOWING_TYPE_UNKNOWN = "SOME_UNKNOWN_TYPE"
    const val CHECK_IN_DATE_ERROR = "Такая дата заселения нам не нужна"
    const val ROOMMATES_LIST_URL = "https://arenda.test.vertis.yandex.ru/" +
        "app/lk/tenant/flat/$FLAT_ID/roommates?only-content=true"
    const val ROOMMATES_INVITATION_URL = "https://arenda.test.vertis.yandex.ru/" +
        "lk/roommates?only-content=true"
    const val SHARED_LINK = "https://arenda.test.vertis.yandex.ru/invite"
    const val SHARE_LINK_SCRIPT = "(function() { mobileAppInjector.onTextShared('$SHARED_LINK'); })();"
    const val UTILITIES_CONDITIONS_URL = "https://arenda.test.vertis.yandex.ru/" +
        "lk/tenant/flat/$FLAT_ID/house-services/settings/confirmation?only-content=true"
    const val SUBMIT_FORM_SCRIPT = "(function() { mobileAppInjector.onFormSubmitted(); })();"

    fun showingWidget(
        html: String,
        type: String = SHOWING_TYPE_INFO,
        action: JsonObject? = null,
        header: String? = null,
    ): JsonObject {
        return jsonObject {
            "headerText" to header
            "html" to html
            "type" to type
            if (action != null) {
                "action" to action
            }
        }
    }

    fun showing(
        showingId: String = SHOWING_ID,
        flatId: String = FLAT_ID,
        rentAmount: Int? = null,
        flatAddress: String = FLAT_ADDRESS,
        offerId: String? = null,
        imageUrl: String? = null,
        roommates: List<String> = emptyList(),
        widget: JsonObject,
    ): JsonObject {
        return jsonObject {
            "showingId" to showingId
            "flatId" to flatId
            "address" to jsonObject {
                "addressFromStreetToFlat" to flatAddress
            }
            if (rentAmount != null) {
                "rentAmount" to rentAmount
            }
            "roommates" to roommates.map { name ->
                jsonObject {
                    "person" to jsonObject { "name" to name }
                }
            }.toJsonArray()
            if (offerId != null) {
                "realtyOfferId" to offerId
            }
            if (imageUrl != null) {
                "retouchedPhotos" to jsonArrayOf(rentImage(imageUrl))
            }
            "widget" to widget
        }
    }
}

fun DispatcherRegistry.registerShowings(showings: List<JsonObject>) {
    register(
        request {
            path("2.0/rent/user/me/showings")
        },
        response {
            jsonBody {
                "response" to jsonObject {
                    "showings" to jsonArrayOf(showings)
                }
            }
        }
    )
}

fun DispatcherRegistry.registerShowing(
    showingId: String = SHOWING_ID,
    flatId: String = FLAT_ID,
    flatAddress: String = FLAT_ADDRESS,
    offerId: String? = null,
    roommates: List<String> = emptyList(),
    widget: JsonObject
) {
    registerShowings(
        showings = listOf(
            Showings.showing(
                showingId = showingId,
                flatId = flatId,
                flatAddress = flatAddress,
                offerId = offerId,
                roommates = roommates,
                widget = widget
            )
        )
    )
}

fun DispatcherRegistry.registerCheckInDateError() {
    register(
        request {
            path("2.0/rent/user/me/showings/$SHOWING_ID/tenant-check-in-date")
            method("POST")
            jsonBody { "tenantCheckInDate" to "2022-06-01" }
        },
        response {
            setResponseCode(400)
            jsonBody {
                "error" to jsonObject {
                    "code" to ErrorCode.VALIDATION_ERROR.name
                    "message" to "error message"
                    "data" to jsonObject {
                        "validationErrors" to jsonArrayOf(
                            jsonObject {
                                "parameter" to "/tenantCheckInDate"
                                "code" to "code"
                                "localizedDescription" to Showings.CHECK_IN_DATE_ERROR
                            }
                        )
                    }
                }
            }
        }
    )
}

fun DispatcherRegistry.registerCheckInDate() {
    register(
        request {
            path("2.0/rent/user/me/showings/$SHOWING_ID/tenant-check-in-date")
            method("POST")
            jsonBody { "tenantCheckInDate" to "2022-06-01" }
        },
        success()
    )
}
