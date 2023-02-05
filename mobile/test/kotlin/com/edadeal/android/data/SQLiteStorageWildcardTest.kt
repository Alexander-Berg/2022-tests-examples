package com.edadeal.android.data

import com.edadeal.android.util.SQLiteStorage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class SQLiteStorageWildcardTest(
    private val wildcardPattern: String,
    private val pattern: String
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<String>> = listOf(
            arrayOf("cb.*.?", "cb.%._"),
            arrayOf("cb.\\*.?", "cb.*._"),
            arrayOf("*cb.campaigns.*", "%cb.campaigns.%"),
            arrayOf("cb.\\?.campaigns.?", "cb.?.campaigns._")
        )
    }

    @Test
    fun `assert pattern value is correct`() {
        assertEquals(pattern, SQLiteStorage.fromWildcard(wildcardPattern))
    }
}
