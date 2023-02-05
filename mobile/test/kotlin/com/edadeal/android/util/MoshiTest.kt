package com.edadeal.android.util

import com.edadeal.android.dto.CartItem
import com.edadeal.android.dto.Experiment
import com.edadeal.android.dto.JsonUseConstructorDefaultParametersForNulls
import com.edadeal.android.util.moshi.GsonLikeByteStringAdapter
import com.edadeal.android.util.moshi.addEnumWithDefaultOnMismatch
import com.edadeal.android.dto.Experiment.Companion.EDADEAL_API_CB
import com.google.gson.GsonBuilder
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import org.junit.Test
import java.io.EOFException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoshiTest {

    @Test
    fun `Moshi_fromJson should correctly parse CartItem json`() {
        val json = """{"id":{"data":[49,50,51]},"image":null,"description":"aaa"}"""
        val moshi = Moshi.Builder()
            .useConstructorDefaultParametersForNulls()
            .setupMoshi()
            .build()
        with(assertNotNull(moshi.fromJsonSafely<CartItem>(json))) {
            assertEquals(id, "123".encodeUtf8())
            assertEquals(image, "")
            assertEquals(description, "aaa")
        }
    }

    @Test
    fun `Moshi_fromJson should correctly parse Experiments json`() {
        val json = """
            [{"CONDITION":"","CONTEXT":{"EDADEAL":{"source":{"EDADEAL_API_CB":{"p":["1"]}}}},
            "ExperimentID":72527,"HANDLER":"EDADEAL"}]"""
        val moshi = Moshi.Builder().build()
        val tmpExperiments = (moshi.adapter<Any?>(Types.newParameterizedType(List::class.java, Experiment::class.java))
            .fromJson(json) as? List<*>).orEmpty().filterIsInstance<Experiment>()
        assertEquals(setOf("1"), tmpExperiments.first().CONTEXT.EDADEAL.source[EDADEAL_API_CB]?.get("p"))
    }

    @Test
    fun `moshi should return null on invalid json`() = with(Moshi.Builder().build()) {
        assertNull(fromJsonSafely<Player>(""))
        assertNull(fromJsonSafely<Player>("A"))
        assertNull(fromJsonSafely<Player>("42"))
        assertNull(fromJsonSafely<Player>("''"))
        assertNull(fromJsonSafely<Player>("{{}"))
    }

    @Test
    fun testMoshiEmpty() {
        val json = "{}"
        with(assertNotNull(Moshi.Builder().build().fromJsonSafely<Player>(json))) {
            assertEquals(active, Player().active)
            assertEquals(ascii, Player().ascii)
            assertEquals(name, Player().name)
            assertEquals(friend, Player().friend)
            assertEquals(hp, Player().hp)
            assertEquals(mp, Player().mp)
            assertEquals(speed, Player().speed)
            assertEquals(weight.toFloat(), Player().weight.toFloat())
            assertEquals(race, Player().race)
            assertEquals(inventory, Player().inventory)
            assertEquals(spells, Player().spells)
        }
        assertFailsWith(EOFException::class) { Moshi.Builder().build().adapter<Player>().fromJson("") }
    }

    @Test
    fun testMoshiNull() {
        val json = """{
            "active":null,"ascii":null,"name":null,"friend":null,"hp":null,"mp":null,
            "speed":null,"weight":null,"race":null,"inventory":null,"spells":null
        }"""
        val moshi = Moshi.Builder().useConstructorDefaultParametersForNulls().build()
        with(assertNotNull(moshi.fromJsonSafely<Player>(json))) {
            assertEquals(active, Player().active)
            assertEquals(ascii, Player().ascii)
            assertEquals(name, Player().name)
            assertEquals(friend, Player().friend)
            assertEquals(hp, Player().hp)
            assertEquals(mp, Player().mp)
            assertEquals(speed, Player().speed)
            assertEquals(weight.toFloat(), Player().weight.toFloat())
            assertEquals(race, Player().race)
            assertEquals(inventory, Player().inventory)
            assertEquals(spells, Player().spells)
        }
    }

    @Test
    fun testMoshiCorrect() {
        val json = """{
            "active":true,"ascii":"W","name":"Marceline","friend":"BMO","hp":50,"mp":100,
            "speed":1000.5,"weight":0.05,"race":"Vampire",
            "inventory":[{"name":"guitar","price":999999999,"effects":["unique"],"history":["a","b"]}],
            "spells":["teleport"]
        }"""
        with(assertNotNull(Moshi.Builder().build().fromJsonSafely<Player>(json))) {
            assertTrue(active)
            assertEquals(ascii, 'W')
            assertEquals(name, "Marceline")
            assertEquals(friend, "BMO")
            assertEquals(hp, 50)
            assertEquals(mp, 100)
            assertEquals(speed, 1000.5f)
            assertEquals(weight.toFloat(), 0.05f)
            assertEquals(race, Race.Vampire)
            assertEquals(inventory, mutableListOf(Item("guitar", 999999999, setOf("unique"), listOf("a", "b"))))
            assertEquals(spells, mutableListOf("teleport"))
        }
    }

    @Test
    fun testMoshiIncorrect() {
        val json = """{
            "active":true,"ascii":"A","name":123,"friend":"","hp":"50","mp":100,
            "speed":1000.5,"weight":0.05,"race":"Lemon",
            "inventory":[{"name":"guitar","price":999999999,"effects":["unique"],"history":["a","b"]}],
            "spells":["teleport"]
        }"""
        val moshi = Moshi.Builder().addEnumWithDefaultOnMismatch(Race.values(), Race.Human).build()
        with(assertNotNull(moshi.fromJsonSafely<Player>(json))) {
            assertTrue(active)
            assertEquals(ascii, 'A')
            assertEquals(name, "123")
            assertEquals(friend, "")
            assertEquals(hp, 50)
            assertEquals(mp, 100)
            assertEquals(speed, 1000.5f)
            assertEquals(weight.toFloat(), 0.05f)
            assertEquals(race, Race.Human)
            assertEquals(inventory, mutableListOf(Item("guitar", 999999999, setOf("unique"), listOf("a", "b"))))
            assertEquals(spells, mutableListOf("teleport"))
        }
    }

    @Test
    fun testByteStringGsonToMoshi() {
        val obj = CartItem(
            id = "test部text".encodeUtf8(),
            description = "description"
        )
        val moshi = Moshi.Builder().add(ByteString::class.java, GsonLikeByteStringAdapter()).build()
        with(assertNotNull(moshi.fromJsonSafely<CartItem>(GsonBuilder().create().toJson(obj)))) {
            assertEquals(id, obj.id)
            assertEquals(description, obj.description)
        }
        val json = """{
            "id":{"some":"garbage","data":[116,101,115,116,-23,-125,-88,116,101,120,116]},
            "description":"description"
        }"""
        with(assertNotNull(moshi.fromJsonSafely<CartItem>(json))) {
            assertEquals(id, "test部text".encodeUtf8())
            assertEquals(description, "description")
        }
    }

    @Test
    fun testMoshiMap() {
        val json = """{"a":"A", "b": 123}"""
        val moshi = Moshi.Builder().build()
        assertEquals(mapOf("a" to "A", "b" to 123.0), moshi.fromJsonSafely<Map<String, Any>>(json))
    }

    @JsonClass(generateAdapter = true)
    @JsonUseConstructorDefaultParametersForNulls
    class Player {
        var active = false
        var ascii = '@'
        var name = "Finn"
        var friend = "Jake"
        var hp = 100; get() = if (field in 0..100) field else 100
        var mp = 10
        var speed = 20.5f
        var weight = 66.6
        var race = Race.Human
        var inventory = mutableListOf<Item>()
        var spells = mutableListOf<String>()
    }

    @JsonClass(generateAdapter = true)
    data class Item(
        val name: String = "",
        val price: Int = 0,
        val effects: Set<String> = setOf(""),
        val history: List<String> = listOf("")
    )

    enum class Race { Human, Dog, Vampire }
}
