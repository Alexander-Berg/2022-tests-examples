package com.yandex.mobile.realty.data.model.proto.yandexrent

import com.yandex.mobile.realty.domain.model.yandexrent.Inventory

/**
 * @author sorokinandrei on 5/16/22
 */
const val ROOM_ID = "room_1"
const val ITEM_ID = "item_1"
const val DEFECT_ID = "defect_1"

fun inventory(
    rooms: List<Inventory.Room> = emptyList(),
    defects: List<Inventory.Defect> = emptyList(),
): Inventory {
    return Inventory(
        rooms = rooms,
        defects = defects,
        managerComment = null,
        editable = true,
        readyForSigning = false,
        signedByOwner = false,
        signedByTenant = false,
        version = 0,
    )
}

fun room(
    id: String,
    name: String = "Test room",
    items: List<Inventory.Item> = emptyList()
): Inventory.Room {
    return Inventory.Room(
        id = id,
        name = name,
        items = items,
    )
}

fun item(
    id: String,
    name: String,
    defectId: String? = null,
    count: Int = 1,
): Inventory.Item {
    return Inventory.Item(
        id = id,
        name = name,
        images = emptyList(),
        count = count,
        defectId = defectId,
    )
}

fun defect(
    id: String,
    comment: String,
    itemRef: Inventory.Defect.ItemRef? = null
): Inventory.Defect {
    return Inventory.Defect(
        id = id,
        comment = comment,
        images = emptyList(),
        itemRef = itemRef
    )
}
