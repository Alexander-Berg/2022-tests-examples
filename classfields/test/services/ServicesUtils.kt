package com.yandex.mobile.realty.test.services

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.jsonBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject

/**
 * @author andrey-bgm on 28/10/2021.
 */
const val FLAT_ID = "flatId00001"
const val CONTRACT_ID = "contractId0001"
const val FLAT_ADDRESS = "Ланское шоссе, 20к3, кв. 100"
const val RENT_ROLE_OWNER = "OWNER"
const val RENT_ROLE_TENANT = "TENANT"
const val RENT_ROLE_TENANT_CANDIDATE = "TENANT_CANDIDATE"
const val PAYMENT_TYPE_NATURAL_PERSON = "PT_NATURAL_PERSON"
const val PAYMENT_TYPE_JURIDICAL_PERSON = "PT_JURIDICAL_PERSON"

fun DispatcherRegistry.registerTenantRentFlats(
    flatId: String = FLAT_ID,
    flatAddress: String = FLAT_ADDRESS,
    status: String = "RENTED",
    actualPayment: JsonObject? = null,
    retouchedPhoto: JsonObject? = null,
) {
    registerRentFlats(
        rentRole = RENT_ROLE_TENANT,
        flatId = flatId,
        flatAddress = flatAddress,
        status = status,
        actualPayment = actualPayment,
        retouchedPhoto = retouchedPhoto,
    )
}

fun DispatcherRegistry.registerOwnerRentFlats(
    flatId: String = FLAT_ID,
    flatAddress: String? = FLAT_ADDRESS,
    status: String = "RENTED",
    retouchedPhoto: JsonObject? = null,
    contractInfo: JsonObject? = null
) {
    registerRentFlats(
        rentRole = RENT_ROLE_OWNER,
        flatId = flatId,
        flatAddress = flatAddress,
        status = status,
        retouchedPhoto = retouchedPhoto,
        contractInfo = contractInfo,
    )
}

fun DispatcherRegistry.registerRentFlats(
    rentRole: String,
    flatId: String = FLAT_ID,
    flatAddress: String? = FLAT_ADDRESS,
    status: String = "RENTED",
    actualPayment: JsonObject? = null,
    retouchedPhoto: JsonObject? = null,
    contractInfo: JsonObject? = null,
) {
    registerRentFlats(
        jsonArrayOf(
            rentFlat(
                rentRole = rentRole,
                flatId = flatId,
                flatAddress = flatAddress,
                status = status,
                actualPayment = actualPayment,
                retouchedPhoto = retouchedPhoto,
                contractInfo = contractInfo,
            )
        )
    )
}

fun DispatcherRegistry.registerRentFlats(flats: JsonArray = jsonArrayOf()) {
    register(
        request {
            path("2.0/rent/user/me/flats")
            queryParam("role", "tenant")
            queryParam("role", "owner")
        },
        response {
            jsonBody {
                "response" to jsonObject {
                    "flats" to flats
                }
            }
        }
    )
}

fun DispatcherRegistry.registerTenantRentFlat(
    flatId: String = FLAT_ID,
    flatAddress: String = FLAT_ADDRESS,
    status: String = "RENTED",
    actualPayment: JsonObject? = null,
    notification: JsonObject? = null,
    retouchedPhoto: JsonObject? = null,
) {
    registerRentFlat(
        rentRole = RENT_ROLE_TENANT,
        flatId = flatId,
        flatAddress = flatAddress,
        status = status,
        actualPayment = actualPayment,
        notifications = notification?.let(::listOf),
        retouchedPhoto = retouchedPhoto,
    )
}

fun DispatcherRegistry.registerOwnerRentFlat(
    flatId: String = FLAT_ID,
    flatAddress: String? = FLAT_ADDRESS,
    status: String = "RENTED",
    notification: JsonObject? = null,
    retouchedPhoto: JsonObject? = null,
    contractInfo: JsonObject? = null,
) {
    registerRentFlat(
        rentRole = RENT_ROLE_OWNER,
        flatId = flatId,
        flatAddress = flatAddress,
        status = status,
        notifications = notification?.let(::listOf),
        retouchedPhoto = retouchedPhoto,
        contractInfo = contractInfo,
    )
}

fun DispatcherRegistry.registerRentFlat(
    rentRole: String,
    flatId: String = FLAT_ID,
    flatAddress: String? = FLAT_ADDRESS,
    status: String = "RENTED",
    actualPayment: JsonObject? = null,
    notifications: List<JsonObject>? = null,
    retouchedPhoto: JsonObject? = null,
    contractInfo: JsonObject? = null,
) {
    register(
        request {
            path("2.0/rent/user/me/flats/$flatId")
            queryParam("includeFeature", "SERVICES_AND_NOTIFICATIONS")
            queryParam("includeFeature", "NOTIFICATION_FALLBACKS")
            queryParam("includeFeature", "USER_NOTIFICATIONS")
        },
        response {
            jsonBody {
                "response" to jsonObject {
                    "flat" to rentFlat(
                        rentRole = rentRole,
                        flatId = flatId,
                        flatAddress = flatAddress,
                        status = status,
                        actualPayment = actualPayment,
                        notifications = notifications,
                        retouchedPhoto = retouchedPhoto,
                        contractInfo = contractInfo,
                    )
                }
            }
        }
    )
}

fun DispatcherRegistry.registerOwnerServicesInfo(notification: JsonObject? = null) {
    registerServicesInfo(
        rentRole = RENT_ROLE_OWNER,
        notifications = notification?.let(::listOf)
    )
}

fun DispatcherRegistry.registerTenantServicesInfo(notification: JsonObject? = null) {
    registerServicesInfo(
        rentRole = RENT_ROLE_TENANT,
        notifications = notification?.let(::listOf)
    )
}

fun DispatcherRegistry.registerNaturalPersonServicesInfo() {
    registerServicesInfo(paymentType = PAYMENT_TYPE_NATURAL_PERSON)
}

fun DispatcherRegistry.registerServicesInfo(
    rentRole: String? = null,
    notifications: List<JsonObject>? = null,
    paymentType: String? = null,
    showRentFlatSearch: Boolean? = null,
) {
    register(
        request {
            path("2.0/service/info")
            queryParam("includeFeature", "USER_INFO")
        },
        response {
            jsonBody {
                "response" to jsonObject {
                    "userInfo" to jsonObject {
                        rentRole?.let { "rentRoles" to jsonArrayOf(it) }
                        paymentType?.let { "paymentType" to it }
                    }
                    notifications?.let { notifications ->
                        "unfilteredModeSettings" to jsonObject {
                            "notificationGroups" to jsonArrayOf(
                                jsonObject {
                                    "notifications" to jsonArrayOf(notifications)
                                }
                            )
                        }
                    }
                    showRentFlatSearch?.let { "showRentFlatSearch" to it }
                }
            }
        }
    )
}

fun ownerConfirmedTodoNotification(
    flatPhotosDone: Boolean = false,
    flatInfoDone: Boolean = false,
    passportDone: Boolean = false,
): JsonObject {
    return jsonObject {
        "ownerConfirmedTodo" to jsonObject {
            "items" to jsonArrayOf(
                jsonObject {
                    "addFlatPhotos" to jsonObject {}
                    "done" to flatPhotosDone
                },
                jsonObject {
                    "addFlatInfo" to jsonObject {}
                    "done" to flatInfoDone
                },
                jsonObject {
                    "addPassport" to jsonObject {}
                    "done" to passportDone
                },
            )
        }
    }
}

fun contractInfo(
    contractId: String = CONTRACT_ID,
    insuranceIsActive: Boolean = false
): JsonObject {
    return jsonObject {
        "contractId" to contractId
        "insuranceIsActive" to insuranceIsActive
    }
}

private fun rentFlat(
    rentRole: String,
    flatId: String = FLAT_ID,
    flatAddress: String? = FLAT_ADDRESS,
    status: String = "RENTED",
    actualPayment: JsonObject? = null,
    notifications: List<JsonObject>? = null,
    retouchedPhoto: JsonObject? = null,
    contractInfo: JsonObject? = null,
): JsonObject {
    return jsonObject {
        "flatId" to flatId
        "address" to jsonObject {
            flatAddress?.let { "addressFromStreetToFlat" to it }
        }
        "status" to status
        "userRole" to rentRole
        actualPayment?.let { "actualPayment" to it }
        notifications?.let { notifications ->
            "notificationGroups" to jsonArrayOf(
                jsonObject {
                    "notifications" to jsonArrayOf(notifications)
                }
            )
        }
        retouchedPhoto?.let { "retouchedPhotos" to jsonArrayOf(it) }
        contractInfo?.let { "actualContractInfo" to contractInfo }
    }
}
