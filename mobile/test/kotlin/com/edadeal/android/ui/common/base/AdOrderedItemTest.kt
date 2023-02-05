package com.edadeal.android.ui.common.base

import com.edadeal.android.model.ads.AdOrderedItem
import org.junit.Test
import kotlin.test.assertEquals

class AdOrderedItemTest {

    @Test
    fun testMergeOrder() {
        val ads = listOf(
            Item(title = "a", offset = 0, orderNum = 1),
            Item(title = "b", offset = 0, orderNum = 2),
            Item(title = "c", offset = 0, orderNum = 3),
            Item(title = "d", offset = 1, orderNum = 1),
            Item(title = "e", offset = 111, orderNum = 3),
            Item(title = "f", offset = 123, orderNum = 4)
        )
        val adsShuffled = listOf(2, 3, 5, 0, 4, 1).map { ads[it] }
        val items = listOf(1, "A", "B", "C", 2, "D")
        val isString = { _: Int, item: Any? -> item is String }
        val isNullOrString = { _: Int, item: Any? -> item == null || item is String }
        val zip: List<Any>.() -> String = {
            joinToString("") { (it as? Item)?.title ?: it as? String ?: (it as? Int)?.toString() ?: "-" }
        }

        listOf(ads, adsShuffled).forEach {
            assertEquals("1AabcBdC2Def", AdOrderedItem.merge(items, it, isString, { true }).zip())
            assertEquals("1AabcBdC2D", AdOrderedItem.merge(items, it, isString, { false }).zip())
            assertEquals("abc1AdBC2Def", AdOrderedItem.merge(items, it, isNullOrString, { true }).zip())
            assertEquals("abc1AdBC2D", AdOrderedItem.merge(items, it, isNullOrString, { false }).zip())
        }
    }

    @Test
    fun testInsertAdHeaders() {
        val itemsWithAds = arrayListOf(
            Item(title = "A"), 0, Item(title = "B"), Item(title = "C"),
            Item(title = "D"), Item(title = "E"), 1, Item()
        )
        val zip: List<Any>.() -> String = { joinToString(separator = "") { (it as? String) ?: "_" } }

        assertEquals("A__B_C_D_E___", insertAdHeaders(itemsWithAds, ::getAdHeader).zip())
        assertEquals("A_", insertAdHeaders(listOf(Item(title = "A")), ::getAdHeader).zip())
        assertEquals("", insertAdHeaders(emptyList(), ::getAdHeader).zip())
    }

    private fun getAdHeader(item: AdOrderedItem): String? {
        return item.title.takeUnless { it.isEmpty() }
    }

    private class Item(
        override val offset: Int = 0,
        override val orderNum: Int = 0,
        override val title: String = ""
    ) : AdOrderedItem
}
