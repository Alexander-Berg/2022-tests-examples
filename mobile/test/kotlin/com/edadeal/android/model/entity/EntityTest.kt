package com.edadeal.android.model.entity

import okio.ByteString.Companion.decodeHex
import org.junit.Test
import kotlin.test.assertEquals

class EntityTest {

    @Test
    fun `entities should be checked for equality only by id`() {
        val shop1 = Shop.EMPTY.copy(id = "01".decodeHex(), address = "a")
        val shop2 = Shop.EMPTY.copy(id = "01".decodeHex(), address = "b")
        assertEquals(shop1, shop2)
        assertEquals(1, listOf(shop1, shop2).toSet().size)
    }
}
