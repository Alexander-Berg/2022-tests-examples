package com.yandex.mobile.realty.data.model.proto.yandexrent

import com.yandex.mobile.realty.data.model.proto.yandexrent.UpdateOrCreateInventoryRequest.Companion.apply
import com.yandex.mobile.realty.data.model.yandexrent.InventoryOperation
import com.yandex.mobile.realty.domain.model.yandexrent.Inventory.Defect.ItemRef
import com.yandex.mobile.realty.domain.yandexrent.model.DefectFormData
import com.yandex.mobile.realty.domain.yandexrent.model.InventoryItemFormData
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forSingle
import io.kotest.matchers.shouldBe
import org.junit.Test

/**
 * @author sorokinandrei on 5/19/22
 */
class UpdateOrCreateInventoryRequestTest {

    private val defectIdGenerator = ::DEFECT_ID

    @Test
    fun `should add room`() {
        val inventory = inventory()

        val operation = InventoryOperation.Room.Add(
            roomId = ROOM_ID,
            name = "name"
        )
        val request = inventory.apply(operation)

        request.rooms.forSingle { actualRoom ->
            actualRoom.roomClientId shouldBe ROOM_ID
            actualRoom.roomName shouldBe "name"
            actualRoom.items shouldBe emptyList()
        }
    }

    @Test
    fun `should throw when adding duplicate room`() {
        val oldRoom = room(id = ROOM_ID, name = "old name")
        val inventory = inventory(rooms = listOf(oldRoom))

        val operation = InventoryOperation.Room.Add(
            roomId = ROOM_ID,
            name = "new name"
        )

        shouldThrow<IllegalArgumentException> {
            inventory.apply(operation)
        }
    }

    @Test
    fun `should update room`() {
        val oldRoom = room(id = ROOM_ID, name = "old name")
        val inventory = inventory(rooms = listOf(oldRoom))

        val operation = InventoryOperation.Room.Update(
            roomId = ROOM_ID,
            name = "new name"
        )
        val request = inventory.apply(operation)

        request.rooms.forSingle { actualRoom ->
            actualRoom.roomClientId shouldBe ROOM_ID
            actualRoom.roomName shouldBe "new name"
            actualRoom.items shouldBe emptyList()
        }
    }

    @Test
    fun `should delete room`() {
        val oldRoom = room(id = ROOM_ID, name = "old name")
        val inventory = inventory(rooms = listOf(oldRoom))

        val operation = InventoryOperation.Room.Delete(roomId = ROOM_ID)
        val request = inventory.apply(operation)

        request.rooms shouldBe emptyList()
    }

    @Test
    fun `should delete room with defects`() {
        val defect = defect(
            id = DEFECT_ID,
            comment = "comment",
            itemRef = ItemRef(ROOM_ID, ITEM_ID)
        )
        val item = item(id = ITEM_ID, name = "item name", defectId = DEFECT_ID)
        val oldRoom = room(id = ROOM_ID, name = "old name", items = listOf(item))
        val inventory = inventory(rooms = listOf(oldRoom), defects = listOf(defect))

        val operation = InventoryOperation.Room.Delete(roomId = ROOM_ID)
        val request = inventory.apply(operation)

        assertSoftly(request) {
            defects shouldBe emptyList()
            rooms shouldBe emptyList()
        }
    }

    @Test
    fun `should add item without defect`() {
        val room = room(id = ROOM_ID)
        val inventory = inventory(rooms = listOf(room))

        val itemData = InventoryItemFormData(
            name = "name",
            count = 2,
            images = emptyList(),
            defectData = null
        )
        val operation = InventoryOperation.Item.Add(
            roomId = ROOM_ID,
            itemId = ITEM_ID,
            defectIdGenerator = defectIdGenerator,
            itemData
        )
        val request = inventory.apply(operation)

        assertSoftly(request) {
            defects shouldBe emptyList()
            rooms.first().items.forSingle { actualItem ->
                actualItem.itemClientId shouldBe ITEM_ID
                actualItem.itemName shouldBe "name"
                actualItem.photos shouldBe emptyList()
                actualItem.count shouldBe 2
                actualItem.defectId shouldBe null
            }
        }
    }

    @Test
    fun `should add item with defect`() {
        val room = room(id = ROOM_ID)
        val inventory = inventory(rooms = listOf(room))

        val defectData = DefectFormData(
            comment = "comment",
            images = emptyList()
        )
        val itemData = InventoryItemFormData(
            name = "name",
            count = 2,
            images = emptyList(),
            defectData = defectData
        )
        val operation = InventoryOperation.Item.Add(
            roomId = ROOM_ID,
            itemId = ITEM_ID,
            defectIdGenerator = defectIdGenerator,
            itemData
        )
        val request = inventory.apply(operation)

        assertSoftly(request) {
            defects.forSingle { actualDefect ->
                actualDefect.defectClientId shouldBe DEFECT_ID
                actualDefect.description shouldBe "comment"
                actualDefect.photos shouldBe emptyList()
            }
            rooms.first().items.forSingle { actualItem ->
                actualItem.itemClientId shouldBe ITEM_ID
                actualItem.itemName shouldBe "name"
                actualItem.photos shouldBe emptyList()
                actualItem.count shouldBe 2
                actualItem.defectId shouldBe DEFECT_ID
            }
        }
    }

    @Test
    fun `should throw when adding duplicate item`() {
        val oldItem = item(id = ITEM_ID, name = "name", defectId = null)
        val room = room(id = ROOM_ID, items = listOf(oldItem))
        val inventory = inventory(rooms = listOf(room))

        val itemData = InventoryItemFormData(
            name = "name",
            count = 1,
            images = emptyList(),
            defectData = null
        )
        val operation = InventoryOperation.Item.Add(
            roomId = ROOM_ID,
            itemId = ITEM_ID,
            defectIdGenerator = defectIdGenerator,
            itemData
        )

        shouldThrow<IllegalArgumentException> {
            inventory.apply(operation)
        }
    }

    @Test
    fun `should update item with added defect`() {
        val oldItem = item(id = ITEM_ID, name = "name", defectId = null)
        val room = room(id = ROOM_ID, items = listOf(oldItem))
        val inventory = inventory(rooms = listOf(room))

        val defectData = DefectFormData(
            comment = "comment",
            images = emptyList()
        )
        val itemData = InventoryItemFormData(
            name = "name",
            count = 2,
            images = emptyList(),
            defectData = defectData
        )

        val operation = InventoryOperation.Item.Update(
            roomId = ROOM_ID,
            itemId = ITEM_ID,
            defectIdGenerator = defectIdGenerator,
            itemData
        )
        val request = inventory.apply(operation)

        assertSoftly(request) {
            defects.forSingle { actualDefect ->
                actualDefect.defectClientId shouldBe DEFECT_ID
                actualDefect.description shouldBe "comment"
                actualDefect.photos shouldBe emptyList()
            }
            rooms.first().items.forSingle { actualItem ->
                actualItem.itemClientId shouldBe ITEM_ID
                actualItem.itemName shouldBe "name"
                actualItem.photos shouldBe emptyList()
                actualItem.count shouldBe 2
                actualItem.defectId shouldBe DEFECT_ID
            }
        }
    }

    @Test
    fun `should update item with defect`() {
        val oldItem = item(id = ITEM_ID, name = "old name", defectId = DEFECT_ID)
        val oldDefect = defect(
            id = DEFECT_ID,
            comment = "old defect comment",
            itemRef = ItemRef(ROOM_ID, ITEM_ID)
        )
        val room = room(id = ROOM_ID, items = listOf(oldItem))
        val inventory = inventory(rooms = listOf(room), defects = listOf(oldDefect))

        val defectData = DefectFormData(
            comment = "new comment",
            images = emptyList()
        )
        val itemData = InventoryItemFormData(
            name = "new name",
            count = 2,
            images = emptyList(),
            defectData = defectData
        )

        val operation = InventoryOperation.Item.Update(
            roomId = ROOM_ID,
            itemId = ITEM_ID,
            defectIdGenerator = defectIdGenerator,
            itemData
        )
        val request = inventory.apply(operation)

        assertSoftly(request) {
            defects.forSingle { actualDefect ->
                actualDefect.defectClientId shouldBe DEFECT_ID
                actualDefect.description shouldBe "new comment"
                actualDefect.photos shouldBe emptyList()
            }
            rooms.first().items.forSingle { actualItem ->
                actualItem.itemClientId shouldBe ITEM_ID
                actualItem.itemName shouldBe "new name"
                actualItem.photos shouldBe emptyList()
                actualItem.count shouldBe 2
                actualItem.defectId shouldBe DEFECT_ID
            }
        }
    }

    @Test
    fun `should delete defect in item`() {
        val oldItem = item(id = ITEM_ID, name = "name", defectId = DEFECT_ID)
        val oldDefect = defect(
            id = DEFECT_ID,
            comment = "old defect comment",
            itemRef = ItemRef(ROOM_ID, ITEM_ID)
        )
        val room = room(id = ROOM_ID, items = listOf(oldItem))
        val inventory = inventory(rooms = listOf(room), defects = listOf(oldDefect))

        val itemData = InventoryItemFormData(
            name = "name",
            count = 1,
            images = emptyList(),
            defectData = null
        )

        val operation = InventoryOperation.Item.Update(
            roomId = ROOM_ID,
            itemId = ITEM_ID,
            defectIdGenerator = defectIdGenerator,
            itemData
        )
        val request = inventory.apply(operation)

        assertSoftly(request) {
            defects shouldBe emptyList()
            rooms.first().items.forSingle { actualItem ->
                actualItem.itemClientId shouldBe ITEM_ID
                actualItem.itemName shouldBe "name"
                actualItem.photos shouldBe emptyList()
                actualItem.count shouldBe 1
                actualItem.defectId shouldBe null
            }
        }
    }

    @Test
    fun `should delete item with defect`() {
        val oldItem = item(id = ITEM_ID, name = "old name", defectId = DEFECT_ID)
        val oldDefect = defect(
            id = DEFECT_ID,
            comment = "old defect comment",
            itemRef = ItemRef(ROOM_ID, ITEM_ID)
        )
        val room = room(id = ROOM_ID, items = listOf(oldItem))
        val inventory = inventory(rooms = listOf(room), defects = listOf(oldDefect))

        val operation = InventoryOperation.Item.Delete(
            roomId = ROOM_ID,
            itemId = ITEM_ID
        )
        val request = inventory.apply(operation)

        assertSoftly(request) {
            defects shouldBe emptyList()
            rooms.first().items shouldBe emptyList()
        }
    }

    @Test
    fun `should add defect`() {
        val inventory = inventory()

        val operation = InventoryOperation.Defect.Add(
            defectId = DEFECT_ID,
            comment = "comment",
            images = emptyList(),
        )
        val request = inventory.apply(operation)

        request.defects.forSingle { actualDefect ->
            actualDefect.defectClientId shouldBe DEFECT_ID
            actualDefect.description shouldBe "comment"
            actualDefect.photos shouldBe emptyList()
        }
    }

    @Test
    fun `should throw when adding duplicate defect`() {
        val oldDefect = defect(id = DEFECT_ID, comment = "old comment")
        val inventory = inventory(defects = listOf(oldDefect))

        val operation = InventoryOperation.Defect.Add(
            defectId = DEFECT_ID,
            comment = "comment",
            images = emptyList(),
        )

        shouldThrow<IllegalArgumentException> {
            inventory.apply(operation)
        }
    }

    @Test
    fun `should update defect`() {
        val oldDefect = defect(id = DEFECT_ID, comment = "old comment")
        val inventory = inventory(defects = listOf(oldDefect))

        val operation = InventoryOperation.Defect.Update(
            defectId = DEFECT_ID,
            comment = "comment",
            images = emptyList(),
        )
        val request = inventory.apply(operation)

        request.defects.forSingle { actualDefect ->
            actualDefect.defectClientId shouldBe DEFECT_ID
            actualDefect.description shouldBe "comment"
            actualDefect.photos shouldBe emptyList()
        }
    }

    @Test
    fun `should delete defect`() {
        val oldDefect = defect(
            id = DEFECT_ID,
            comment = "old comment",
            itemRef = ItemRef(ROOM_ID, ITEM_ID)
        )
        val item = item(
            id = ITEM_ID,
            name = "test item",
            defectId = DEFECT_ID
        )
        val room = room(id = ROOM_ID, items = listOf(item))
        val inventory = inventory(rooms = listOf(room), defects = listOf(oldDefect))

        val operation = InventoryOperation.Defect.Delete(defectId = DEFECT_ID)
        val request = inventory.apply(operation)

        assertSoftly(request) {
            rooms.first().items.forSingle { actualItem ->
                actualItem.defectId shouldBe null
            }
            defects shouldBe emptyList()
        }
    }
}
