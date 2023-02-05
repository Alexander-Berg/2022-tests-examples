package com.edadeal.android.data

import android.os.Build
import com.chibatching.kotpref.Kotpref
import com.chibatching.kotpref.KotprefModel
import okio.ByteString.Companion.decodeHex
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ByteStringPrefTest {

    private lateinit var prefs: Prefs

    @BeforeTest
    fun prepare() {
        Kotpref.init(RuntimeEnvironment.application)
        prefs = Prefs()
        prefs.clear()
    }

    @Test
    fun `should successfully migrate field type from String to ByteString`() {
        prefs.preferences.edit().putString("item", "aa").apply()
        prefs.preferences.edit().putString("items", "aa;bb").apply()

        assertEquals("aa".decodeHex(), prefs.item)
        assertEquals(setOf("aa".decodeHex(), "bb".decodeHex()), prefs.items)
    }

    @Test
    fun `prefs should return value that was previously set to field (Case - single value)`() {
        val value = "aa".decodeHex()
        prefs.item = value

        assertEquals(value, prefs.item)
        assertEquals(prefs.preferences.getString("item", ""), value.hex())
    }

    @Test
    fun `prefs should return value that was previously set to field (Case - set of values)`() {
        val values = setOf("aa".decodeHex(), "bb".decodeHex())
        prefs.items = values
        assertEquals(values, prefs.items)
        assertEquals(prefs.preferences.getString("items", ""), values.joinToString(";") { it.hex() })
    }

    class Prefs : KotprefModel() {
        var items by byteStringSetPref()
        var item by byteStringPref()
    }
}
