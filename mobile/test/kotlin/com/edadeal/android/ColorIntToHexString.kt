package com.edadeal.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ColorIntToHexString(private val hexString: String, private val colorInt: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Any> = listOf(
            arrayOf("ffdb4c", -9396), // R.color.yellow1
            arrayOf("666699", -10066279), // R.color.blue1
            arrayOf("ff0000", -65536), // R.color.red1
            arrayOf("27ae61", -14176671), // R.color.green1
            arrayOf("e68a36", -1668554) // R.color.orange1
        )
    }

    @Test
    fun `should return valid hex string`() {
        assertEquals(hexString, colorIntToHexString(colorInt))
    }
}
