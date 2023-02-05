package ru.yandex.market.base.network.fapi.extractor

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class FapiExtractorStrategyTest {

    private val strategy = FapiExtractStrategy()

    @Test
    fun `Method 'selectByIds' selects correct items`() {
        class TestItem

        val item1 = TestItem()
        val item2 = TestItem()
        val item3 = TestItem()
        val collection = mapOf("1" to item1, "2" to item2, "3" to item3)

        val result = strategy.selectByIds(collection, listOf("1", "3"))

        assertThat(result).containsExactly(item1, item3)
    }

    @Test
    fun `Method 'selectByIds' selects items in correct order`() {
        class TestItem

        val item1 = TestItem()
        val item2 = TestItem()
        val item3 = TestItem()
        val collection = mapOf("1" to item1, "2" to item2, "3" to item3)

        val result = strategy.selectByIds(collection, listOf("3", "1"))

        assertThat(result).containsExactly(item3, item1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Method 'selectByIds' throws exception if item for id not found`() {
        class TestItem

        val item1 = TestItem()
        val item2 = TestItem()
        val item3 = TestItem()
        val collection = mapOf("1" to item1, "2" to item2, "3" to item3)

        strategy.selectByIds(collection, listOf("4"))
    }

    @Test
    fun `Method 'selectByCustomIds' without skipMissing selects correct items`() {
        class TestItem(val customId: String)

        val item1 = TestItem("one")
        val item2 = TestItem("two")
        val item3 = TestItem("three")
        val collection = mapOf("1" to item1, "2" to item2, "3" to item3)

        val result = strategy.selectByCustomIds(collection, listOf("one", "two")) { item -> item.customId }

        assertThat(result).containsExactly(item1, item2)
    }

    @Test
    fun `Method 'selectByCustomIds' without skipMissing selects items in correct order`() {
        class TestItem(val customId: String)

        val item1 = TestItem("one")
        val item2 = TestItem("two")
        val item3 = TestItem("three")
        val collection = mapOf("1" to item1, "2" to item2, "3" to item3)

        val result = strategy.selectByCustomIds(collection, listOf("two", "one")) { item -> item.customId }

        assertThat(result).containsExactly(item2, item1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Method 'selectByCustomIds' without skipMissing throws exception if item for id not found`() {
        class TestItem(val customId: String)

        val item1 = TestItem("one")
        val item2 = TestItem("two")
        val item3 = TestItem("three")
        val collection = mapOf("1" to item1, "2" to item2, "3" to item3)

        strategy.selectByCustomIds(collection, listOf("four")) { item -> item.customId }
    }

    @Test
    fun `Method 'selectByCustomIds' with skipMissing doesn't throw exception if item for id not found`() {
        class TestItem(val customId: String)

        val item1 = TestItem("one")
        val item3 = TestItem("three")
        val collection = mapOf("1" to item1, "3" to item3)

        val customIds = listOf("four", "three", "two", "one")

        val result = strategy.selectByCustomIds(
            collection = collection,
            ids = customIds,
            skipMissing = true,
            idProvider = { item -> item.customId }
        )

        assertThat(result).containsExactly(item3, item1)
    }

    @Test
    fun `Method 'selectById' returns correct item`() {
        class TestItem

        val item1 = TestItem()
        val item2 = TestItem()
        val item3 = TestItem()
        val collection = mapOf("1" to item1, "2" to item2, "3" to item3)

        val result = strategy.selectById(collection, "2")

        assertThat(result).isEqualTo(item2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Method 'selectById' throws exception if item for id not found`() {
        class TestItem

        val item1 = TestItem()
        val item2 = TestItem()
        val item3 = TestItem()
        val collection = mapOf("1" to item1, "2" to item2, "3" to item3)

        strategy.selectById(collection, "4")
    }

    @Test
    fun `Method 'selectByIdOrNull' returns correct item`() {
        class TestItem

        val item1 = TestItem()
        val item2 = TestItem()
        val item3 = TestItem()
        val collection = mapOf("1" to item1, "2" to item2, "3" to item3)

        val result = strategy.selectByIdOrNull(collection, "2")

        assertThat(result).isEqualTo(item2)
    }

    @Test
    fun `Method 'selectByIdOrNull' returns null if item for id not found`() {
        class TestItem

        val item1 = TestItem()
        val item2 = TestItem()
        val item3 = TestItem()
        val collection = mapOf("1" to item1, "2" to item2, "3" to item3)

        val result = strategy.selectByIdOrNull(collection, "4")

        assertThat(result).isEqualTo(null)
    }

    @Test
    fun `Method 'selectByCustomId' returns correct item`() {
        class TestItem(val customId: String)

        val item1 = TestItem("one")
        val item2 = TestItem("two")
        val item3 = TestItem("three")
        val collection = mapOf("1" to item1, "2" to item2, "3" to item3)

        val result = strategy.selectByCustomId(collection, "three") { item -> item.customId }

        assertThat(result).isEqualTo(item3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Method 'selectByCustomId' throws exception if item for id not found`() {
        class TestItem(val customId: String)

        val item1 = TestItem("one")
        val item2 = TestItem("two")
        val item3 = TestItem("three")
        val collection = mapOf("1" to item1, "2" to item2, "3" to item3)

        strategy.selectByCustomId(collection, "four") { item -> item.customId }
    }

    @Test
    fun `Method 'selectByCustomIdOrNull' returns correct item`() {
        class TestItem(val customId: String)

        val item1 = TestItem("one")
        val item2 = TestItem("two")
        val item3 = TestItem("three")
        val collection = mapOf("1" to item1, "2" to item2, "3" to item3)

        val result = strategy.selectByCustomIdOrNull(collection, "three") { item -> item.customId }

        assertThat(result).isEqualTo(item3)
    }

    @Test
    fun `Method 'selectByCustomIdOrNull' returns null if item for id not found`() {
        class TestItem(val customId: String)

        val item1 = TestItem("one")
        val item2 = TestItem("two")
        val item3 = TestItem("three")
        val collection = mapOf("1" to item1, "2" to item2, "3" to item3)

        val result = strategy.selectByCustomIdOrNull(collection, "four") { item -> item.customId }

        assertThat(result).isEqualTo(null)
    }
}