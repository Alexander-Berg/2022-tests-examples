package com.yandex.mobile.realty.test.yandexrent.inventory

import com.google.gson.JsonObject
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.error
import com.yandex.mobile.realty.core.webserver.jsonBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.data.model.proto.ErrorCode
import com.yandex.mobile.realty.test.yandexrent.OWNER_REQUEST_ID
import com.yandex.mobile.realty.test.yandexrent.rentImage
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject

/**
 * @author sorokinandrei on 4/14/22
 */
const val INVENTORY_VERSION = 3
const val SMS_REQUEST_ID = "requestId0001"
const val PHONE = "+79998887766"
const val ACTION_EDIT = "EDIT"
const val ACTION_CONFIRM = "CONFIRM"
const val ITEM_IMAGE_URL = "https://localhost:8080/1/table.webp"
const val DEFECT_IMAGE_URL = "https://localhost:8080/2/defect.webp"

fun DispatcherRegistry.registerLastInventory(
    inventoryResponse: JsonObject,
    ownerRequestId: String = OWNER_REQUEST_ID
) {
    register(
        request {
            path("2.0/rent/user/me/inventory/$ownerRequestId/last")
        },
        response {
            jsonBody {
                "response" to inventoryResponse
            }
        }
    )
}

fun DispatcherRegistry.registerInventory(
    inventoryResponse: JsonObject,
    version: Int,
    ownerRequestId: String = OWNER_REQUEST_ID
) {
    register(
        request {
            path("2.0/rent/user/me/inventory/$ownerRequestId")
            queryParam("version", version.toString())
        },
        response {
            jsonBody {
                "response" to inventoryResponse
            }
        }
    )
}

fun DispatcherRegistry.registerLastInventoryError(ownerRequestId: String = OWNER_REQUEST_ID) {
    register(
        request {
            path("2.0/rent/user/me/inventory/$ownerRequestId/last")
        },
        error()
    )
}

fun DispatcherRegistry.registerInventorySmsCodeRequest(
    requestId: String = SMS_REQUEST_ID,
    phone: String = PHONE
) {
    register(
        request {
            path("2.0/rent/user/me/inventory/$OWNER_REQUEST_ID/confirmation-code/request")
        },
        response {
            jsonBody {
                "response" to jsonObject {
                    "smsInfo" to jsonObject {
                        "codeLength" to 5
                        "requestId" to requestId
                        "phone" to phone
                    }
                }
            }
        }
    )
}

fun DispatcherRegistry.registerInventorySmsCodeSubmit(code: String) {
    register(
        request {
            path("2.0/rent/user/me/inventory/$OWNER_REQUEST_ID/confirmation-code/submit")
            jsonBody {
                "confirmSmsInfo" to jsonObject {
                    "code" to code
                    "requestId" to SMS_REQUEST_ID
                }
            }
        },
        response {
            jsonBody {
                "response" to jsonObject {
                    "smsInfo" to jsonObject {
                        "codeLength" to 5
                        "requestId" to SMS_REQUEST_ID
                        "phone" to PHONE
                    }
                }
            }
        }
    )
}

fun DispatcherRegistry.registerInventoryEdit(
    ownerRequestId: String = OWNER_REQUEST_ID,
    rooms: List<JsonObject> = emptyList(),
    defects: List<JsonObject> = emptyList(),
    actions: List<String> = listOf("EDIT"),
) {
    register(
        request {
            path("2.0/rent/user/me/inventory/$ownerRequestId/edit")
        },
        response {
            jsonBody {
                "response" to jsonObject {
                    "inventory" to jsonObject {
                        "rooms" to jsonArrayOf(rooms)
                        "defects" to jsonArrayOf(defects)
                        "managerComment" to null
                        "confirmedByOwner" to false
                        "confirmedByTenant" to false
                        "version" to 0
                    }
                    "actions" to jsonArrayOf(actions)
                }
            }
        }
    )
}

fun DispatcherRegistry.registerInventoryEditValidationErrors(
    ownerRequestId: String = OWNER_REQUEST_ID,
    validationErrors: List<Pair<String, String>>
) {
    register(
        request {
            path("2.0/rent/user/me/inventory/$ownerRequestId/edit")
        },
        response {
            setResponseCode(400)
            jsonBody {
                "error" to jsonObject {
                    "code" to ErrorCode.VALIDATION_ERROR.name
                    "message" to "error message"
                    "data" to jsonObject {
                        "validationErrors" to jsonArrayOf(
                            validationErrors.map { (parameter, error) ->
                                jsonObject {
                                    "parameter" to parameter
                                    "code" to "code"
                                    "localizedDescription" to error
                                }
                            }
                        )
                    }
                }
            }
        }
    )
}

fun DispatcherRegistry.registerInventoryEditError(ownerRequestId: String = OWNER_REQUEST_ID) {
    register(
        request {
            path("2.0/rent/user/me/inventory/$ownerRequestId/edit")
        },
        response {
            error()
        }
    )
}

fun inventoryResponse(
    version: Int = 0,
    rooms: List<JsonObject> = emptyList(),
    defects: List<JsonObject> = emptyList(),
    managerComment: String? = null,
    actions: List<String> = listOf(ACTION_EDIT),
    confirmedByOwner: Boolean = false,
    confirmedByTenant: Boolean = false,
): JsonObject {
    return jsonObject {
        "inventory" to jsonObject {
            "rooms" to jsonArrayOf(rooms)
            "defects" to jsonArrayOf(defects)
            "managerComment" to managerComment
            "confirmedByOwner" to confirmedByOwner
            "confirmedByTenant" to confirmedByTenant
            "version" to version
        }
        "actions" to jsonArrayOf(actions)
    }
}

fun room(id: RoomId, name: String, items: List<JsonObject> = emptyList()): JsonObject {
    return jsonObject {
        "roomClientId" to id
        "roomName" to name
        "items" to jsonArrayOf(items)
    }
}

fun emptyRooms(): List<JsonObject> {
    return listOf(
        room(RoomId(1), "Кухня", emptyList()),
        room(RoomId(2), "Гостиная", emptyList()),
        room(RoomId(3), "Ванная", emptyList()),
    )
}

fun item(
    id: ItemId,
    name: String,
    defectId: DefectId? = null,
    images: List<JsonObject> = emptyList()
): JsonObject {
    return jsonObject {
        "itemClientId" to id
        "itemName" to name
        "photos" to jsonArrayOf(images)
        "count" to "1"
        "defectId" to defectId
    }
}

fun defect(id: DefectId, description: String, images: List<JsonObject> = emptyList()): JsonObject {
    return jsonObject {
        "defectClientId" to id
        "description" to description
        "photos" to jsonArrayOf(images)
    }
}

fun sampleRooms(): List<JsonObject> {
    return listOf(
        room(
            id = RoomId(1), name = "Кухня",
            items = listOf(
                item(id = ItemId(1), name = "Телевизор", defectId = DefectId(1)),
                item(
                    id = ItemId(2),
                    name = "Стол",
                    images = listOf(rentImage(ITEM_IMAGE_URL)),
                    defectId = DefectId(2)
                ),
                item(id = ItemId(3), name = "Холодильник"),
            )
        ),
        room(
            id = RoomId(2), name = "Гостиная",
            items = listOf(
                item(id = ItemId(4), name = "Телевизор"),
                item(id = ItemId(5), name = "Роутер"),
                item(
                    id = ItemId(6),
                    name = "Диван",
                    defectId = DefectId(3),
                ),
            )
        ),
        room(
            id = RoomId(3), name = "Ванная",
            items = listOf(
                item(id = ItemId(7), name = "Зеркало"),
                item(id = ItemId(8), name = "Стиральная машина")
            )
        ),
    )
}

fun sampleDefects(): List<JsonObject> {
    return listOf(
        defect(id = DefectId(1), description = "Царапина"),
        defect(
            id = DefectId(2),
            description = "Дефект телевизора",
            images = listOf(rentImage(DEFECT_IMAGE_URL))
        ),
        defect(
            id = DefectId(3),
            description = "Дефект дивана",
            images = listOf(rentImage(DEFECT_IMAGE_URL))
        ),
        defect(
            id = DefectId(4),
            description = "Дефект комнаты",
            images = listOf(rentImage(DEFECT_IMAGE_URL))
        ),
    )
}

@JvmInline
value class RoomId(private val index: Int) {

    override fun toString(): String {
        return "room_$index"
    }
}

@JvmInline
value class ItemId(private val index: Int) {

    override fun toString(): String {
        return "item_$index"
    }
}

@JvmInline
value class DefectId(private val index: Int) {

    override fun toString(): String {
        return "defect_$index"
    }
}
