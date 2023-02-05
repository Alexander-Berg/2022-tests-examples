package ru.yandex.market.utils

import androidx.collection.SparseArrayCompat
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.TreeMap

class SparseArrayCompatExtensionsTest {

    @Test
    fun `Keys returns right sequence`() {
        val keysList = listOf(1, 3, 2, 4)
        val sparseArray = SparseArrayCompat<String>().apply {
            keysList.forEach { put(it, it.toString()) }
        }

        assertThat(sparseArray.keys.asIterable()).containsExactlyInAnyOrderElementsOf(keysList)
    }

    @Test
    fun `Keys return empty sequence for empty sparse array`() {
        val sparseArray = SparseArrayCompat<String>()

        assertThat(sparseArray.keys.asIterable()).isEmpty()
    }

    @Test
    fun `For each entry method is invoked for each entry`() {
        val startEntries = TreeMap<Int, String?>()
        startEntries.putAll(sequenceOf(1 to "one", 3 to "three", 2 to "two", 4 to "four"))

        val sparseArray = SparseArrayCompat<String>().apply {
            startEntries.forEach { (key, value) -> put(key, value) }
        }

        val invokedEntries = TreeMap<Int, String?>()
        sparseArray.forEachEntry { key, value -> invokedEntries[key] = value }

        assertThat(invokedEntries).isEqualTo(startEntries)
    }

    @Test
    fun `For each entry method is never invoked for empty sparse array`() {
        val sparseArray = SparseArrayCompat<String>()

        val invokedEntries = TreeMap<Int, String?>()
        sparseArray.forEachEntry { key, value -> invokedEntries[key] = value }

        assertThat(invokedEntries.entries).isEmpty()
    }
}