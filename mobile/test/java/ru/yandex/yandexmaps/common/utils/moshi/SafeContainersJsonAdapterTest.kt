package ru.yandex.yandexmaps.common.utils.moshi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SafeContainerAdapterTests {

    private lateinit var moshi: Moshi

    @Before
    fun setUp() {
        moshi = Moshi.Builder()
            .add(SafeContainersJsonAdapterFactory)
            .build()
    }

    @Test
    fun skipInvalidStrings() {

        val expected = Strings(listOf("1", "2.0", "3", "4"))
        val json = """{"list": [ "1", 2, "3", null, {}, [], "4" ] } """

        val adapter = moshi.adapter(Strings::class.java)
        val parsed = adapter.fromJson(json)

        assertEquals(expected, parsed)
    }

    @Test
    fun skipInvalidItemsInList() {
        val adapter = moshi.adapter(ListTestData::class.java)

        val parsed = adapter.fromJson(ListTestData.JSON)

        assertEquals(ListTestData.EXPECTED, parsed)
    }

    @Test
    fun skipInvalidItemsInMap() {
        val adapter = moshi.adapter(MapTestData::class.java)

        val parsed = adapter.fromJson(MapTestData.JSON)

        assertEquals(MapTestData.EXPECTED, parsed)
    }

    @Test
    fun skipInvalidItemsInSet() {
        val adapter = moshi.adapter(SetTestData::class.java)

        val parsed = adapter.fromJson(SetTestData.JSON)

        assertEquals(SetTestData.EXPECTED, parsed)
    }

    @Test
    fun skipInvalidItemsInNestedContainers() {
        val adapter = moshi.adapter(NestedContainersTestData::class.java)

        val parsed = adapter.fromJson(NestedContainersTestData.JSON)

        assertEquals(NestedContainersTestData.EXPECTED, parsed)
    }
}

@JsonClass(generateAdapter = true)
data class ListTestData(
    @SafeContainer val list: List<Item>,
    @SafeContainer val mutList: MutableList<Item>,
    @SafeContainer val stringList: List<String>,
    @SafeContainer val collection: Collection<Item>
) {

    companion object {

        val EXPECTED = ListTestData(
            (0..2).map { index -> Item(index, index, "$index") }.toList(),
            (0..3).map { index -> Item(index, index, "$index") }.toMutableList(),
            listOf("1.0", "str1", "str2", "2.0", "str1", "str2", "str5", "3.0", "4.0"),
            (0..2).map { index -> Item(index, index, "$index") }.toList()
        )

        const val JSON = """
            {
                "list": [
                    {"x": 0, "y": "0", "string": "0"},
                    {},
                    {"x": 1, "y": "1", "string": "1"},
                    {"x": 5, "y": "invalid type", "string":"must be skipped"},
                    {"x": 2, "y": "2", "string": "2"},
                    {}
                ],
                "collection": [
                    {"x": 0, "y": "0", "string": "0"},
                    {},
                    {"x": 1, "y": "1", "string": "1"},
                    {"x": 5, "y": "invalid type", "string":"must be skipped"},
                    {"x": 2, "y": "2", "string": "2"},
                    {}
                ],
                "mutList": [
                    {"x": 0, "y": "0", "string": "0"},
                    {"x": "invalid type", "y": 5, "string":"must be skipped"},
                    {"x": 1, "y": "1", "string": "1"},
                    {"x": 2, "y": "2", "string": "2"},
                    {"x": 5, "y": "invalid type", "string":"must be skipped"},
                    {"x": 3, "y": "3", "string": "3"}
                ],
                "stringList": [1, "str1", null, "str2", 2, null, "str1", "str2", null, "str5", 3, 4]
            }
             """
    }
}

@JsonClass(generateAdapter = true)
data class SetTestData(
    @SafeContainer val set: Set<Item>,
    @SafeContainer @Json(name = "mutset") val mutableSet: MutableSet<Item>,
    @SafeContainer val anySet: Set<Any>,
    @Json(name = "int_set") @SafeContainer val intSet: MutableSet<Int>
) {

    companion object {

        val EXPECTED = SetTestData(
            (0..2).map { index -> Item(index, index + 10, "Hi, $index") }.toSet(),
            (0..3).map { index -> Item(index, index + 20, "Hi mutable, $index") }.toMutableSet(),
            setOf(
                1.0, 2.0, "3 - string", mapOf("item1" to 1.0, "item2" to "str"), 5.0
            ),
            mutableSetOf(1, 2, 5)
        )

        const val JSON = """
            {
                "set": [
                    { "x": 0, "y":10, "string":"Hi, 0" },
                    { "x": 50, "y":"invalid type", "string":"must be skipped" },
                    { "x": 1, "y":11, "string":"Hi, 1" },
                    {},
                    { "x": 2, "y":12, "string":"Hi, 2" },
                    { "string": "must be skipped because of nullability" }
                ],
                "mutset":[
                    { "x": 0, "y":20, "string":"Hi mutable, 0" },
                    { "x": 900, "y":"invalid type", "string":"must be skipped" },
                    { "x": 1, "y":21, "string":"Hi mutable, 1" },
                    {},
                    { "x": 2, "y":22, "string":"Hi mutable, 2" },
                    { "x": "must be skipped", "y": 123, "string": "str" },
                    { "x": 3, "y":23, "string":"Hi mutable, 3" },
                    {}
                ],
                "anySet":  [ null, 1, null, 2.0, null, "3 - string", null, { "item1": 1, "item2": "str" }, 5],
                "int_set": [ null, 1, null, 2.0, null, "3 - string", null, { "item1": 1, "item2": "str" }, 5]
            }
            """
    }
}

@JsonClass(generateAdapter = true)
data class MapTestData(@SafeContainer val map: Map<String, Item>, @SafeContainer val mutmap: MutableMap<String, Item>) {

    companion object {
        val EXPECTED = MapTestData(
            (0..2).fold(HashMap()) { map, index ->
                map["item$index"] = Item(index, index + 100, "Hello, item #$index!")
                map
            },
            (0..3).fold(HashMap()) { map, index ->
                map["mutable_item$index"] = Item(index, index + 100, "Hello, mutable item #$index!")
                map
            }
        )

        const val JSON = """
            {
                "map": {
                    "item1_skip": { "x": 7, "y": "some not int" },
                    "item0" : { "x": 0, "y": 100, "string": "Hello, item #0!" },
                    "item0_skip": {},
                    "item1" : { "x": 1, "y": 101, "string": "Hello, item #1!" },
                    "item1_skip": { "x": 7, "y": "some not int" },
                    "item2" : { "x": 2, "y": 102, "string": "Hello, item #2!" },
                    "item2_skip": { "x": 5 }
                 },
                "mutmap":{
                    "mutable_item0" : { "x": 0, "y": 100, "string": "Hello, mutable item #0!" },
                    "mutable_item0_skip": {},
                    "mutable_item1" : { "x": 1, "y": 101, "string": "Hello, mutable item #1!" },
                    "mutable_item2" : { "x": 2, "y": 102, "string": "Hello, mutable item #2!" },
                    "mutable_item2_skip": { "x": 5 },
                    "mutable_item3" : { "x": 3, "y": 103, "string": "Hello, mutable item #3!" }
                 }
            }
            """
    }
}

@JsonClass(generateAdapter = true)
data class NestedContainersTestData(
    @SafeContainer val listOfSetObjs: List<SetTestData>,
    @SafeContainer val listOfListOfInt: List<List<Int>>,
    @SafeContainer val listOfSetOfMap: List<Set<Map<String, Item>>>
) {

    companion object {
        val EXPECTED = NestedContainersTestData(
            listOf(
                SetTestData.EXPECTED, SetTestData.EXPECTED, SetTestData.EXPECTED
            ),
            listOf(
                listOf(11, 12, 13, 14),
                listOf(21, 22, 23, 24),
                listOf(31, 32, 33, 34)
            ),
            (0..2).map { listIndex ->
                (0..2).map { setIndex ->
                    (0..2).fold(HashMap<String, Item>()) { map, index ->
                        map["item$index"] = Item(listIndex, setIndex, "#$index")
                        map
                    }
                }.toSet()
            }.toList()
        )

        const val JSON = """
            {
                "listOfSetObjs": [ ${SetTestData.JSON}, { "some":"invalid", "obj":"must", "be":"skipped!" }, ${SetTestData.JSON}, ${SetTestData.JSON} ],
                "listOfListOfInt": [
                    [11, "skip", 12, 13, 14],
                    {},
                    [21, 22, "skip", 23, 24],
                    {
                        "must": "be skipped!"
                    },
                    [31, 32, 33,"skip", 34],
                    "skip"
                ],
                "listOfSetOfMap": [
                    "skip_it_1",
                    [
                        {
                            "item0": { "x":0, "y":0, "string":"#0" },
                            "item1": { "x":0, "y":0, "string":"#1" },
                            "item2": { "x":0, "y":0, "string":"#2" },
                            "skip_it_2": {"x" : "invalid type", "y":5, "string":"must be skipped" }
                        },
                        {
                            "skip_it_3": {},
                            "item0": { "x":0, "y":1, "string":"#0" },
                            "item1": { "x":0, "y":1, "string":"#1" },
                            "item2": { "x":0, "y":1, "string":"#2" }
                        },
                        {
                            "item0": { "x":0, "y":2, "string":"#0" },
                            "item1": { "x":0, "y":2, "string":"#1" },
                            "skip_it_4": 1,
                            "item2": { "x":0, "y":2, "string":"#2" }
                        },

                        "skip_it"
                    ],
                    [
                        {
                            "a": [],
                            "item0": { "x":1, "y":0, "string":"#0" },
                            "b": [],
                            "item1": { "x":1, "y":0, "string":"#1" },
                            "c": [],
                            "item2": { "x":1, "y":0, "string":"#2" },
                            "d": []
                        },
                        {
                            "item0": { "x":1, "y":1, "string":"#0" },
                            "item1": { "x":1, "y":1, "string":"#1" },
                            "item2": { "x":1, "y":1, "string":"#2" },
                            "skip_it_5": {"x": 1, "z": "where is y?", "string": "skip me!" }
                        },
                        {
                            "skip_it_8": {"x" : 6, "y":"invalid type", "string":"must be skipped" },
                            "item0": { "x":1, "y":2, "string":"#0" },
                            "item1": { "x":1, "y":2, "string":"#1" },
                            "item2": { "x":1, "y":2, "string":"#2" },
                            "skip": {}
                        }
                    ],
                    [
                        {
                            "item0": { "x":2, "y":0, "string":"#0" },
                            "item1": { "x":2, "y":0, "string":"#1" },
                            "skip_it_6": {"x":{}, "y":5, "string": "must be skipped" },
                            "item2": { "x":2, "y":0, "string":"#2" }
                        },
                        {
                            "item0": { "x":2, "y":1, "string":"#0" },
                            "item1": { "x":2, "y":1, "string":"#1" },
                            "item2": { "x":2, "y":1, "string":"#2" },
                            "skip_7": "me"
                        },
                        {
                            "item0": { "x":2, "y":2, "string":"#0" },
                            "item1": { "x":2, "y":2, "string":"#1" },
                            "item2": { "x":2, "y":2, "string":"#2" },
                            "must_be_skipped": 5
                        }
                    ]
                ]
            }
            """
    }
}

@JsonClass(generateAdapter = true)
data class Item(val x: Int, val y: Int, val string: String)

@JsonClass(generateAdapter = true)
data class Strings(@SafeContainer val list: List<String>)
