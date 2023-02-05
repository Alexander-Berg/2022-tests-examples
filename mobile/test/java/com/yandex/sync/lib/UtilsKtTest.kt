package com.yandex.sync.lib

import com.yandex.sync.lib.utils.SyncTestRunner
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(SyncTestRunner::class)
class UtilsKtTest {

    @Test
    fun generateArgsStringTest() {
        val args = listOf("a", "b", "c")
        assertEquals("?,?,?", args.generateArgsString())

        val args0 = listOf<String>()
        assertEquals("", args0.generateArgsString())
    }

    @Test
    fun chunkedFlattenTest() {
        val args = listOf("a", "b", "c")
        val transformed = args.chunkedFlatten(2) {
            return@chunkedFlatten it + "1"
        }

        assertEquals(listOf("a", "b", "1", "c", "1"), transformed)
    }
}