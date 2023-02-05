package com.edadeal.moshi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonPropertyTest {
    @Test
    fun `fromJson should set correct values for properties`() {
        val name = JsonStringProperty("name")
        val long = JsonLongProperty("long")
        val double = JsonDoubleProperty("double")
        val rawArray = JsonRawProperty("raw")
        val rawObject = JsonRawProperty("route")
        val paramName = JsonStringProperty("name")
        val paramEnabled = JsonBooleanProperty("enabled")
        val paramConfigUrl = JsonStringProperty("url")
        val paramConfigCount = JsonLongProperty("count")
        val longArray = JsonLongArrayProperty("longArray")
        val doubleArray = JsonDoubleArrayProperty("doubleArray")
        val stringArray = JsonStringArrayProperty("stringArray")
        val missing = JsonLongProperty("missing")

        val json = """{
"name":"field",
"route":{
 "path": "some",
 "long": 3,
 "double": 3.0
},
"raw" :
 [{ "long": 3, "double": 3.0 }]  ,
"param":{
 "name": "parameter",
 "enabled": true,
 "config": {
  "url": "http",
  "count": 42
 }
},
"long": 3,
"double": 3.0,
"stringArray": [ "a", "b", "c" ],
"longArray": [ 0, 1.0, 2 ],
"doubleArray": [ 0, 1.0, 2 ]
}""".trimIndent()

        JsonProperty.fromJson(
            json,
            name, long, double, rawArray, rawObject, longArray, doubleArray, stringArray, missing,
            JsonObjectProperty(
                "param", paramName, paramEnabled, JsonObjectProperty("config", paramConfigUrl, paramConfigCount)
            )
        )

        assertEquals("field", name.value)
        assertEquals(3, long.value)
        assertEquals(3.0, double.value)
        assertEquals("parameter", paramName.value)
        assertEquals(true, paramEnabled.value)
        assertEquals("http", paramConfigUrl.value)
        assertEquals(42, paramConfigCount.value)
        assertEquals("[{ \"long\": 3, \"double\": 3.0 }]", rawArray.value)
        assertEquals("{\n \"path\": \"some\",\n \"long\": 3,\n \"double\": 3.0\n}", rawObject.value)
        assertEquals(longArrayOf(0, 1, 2).asList(), longArray.value?.asList().orEmpty())
        assertEquals(arrayOf("a", "b", "c").asList(), stringArray.value?.asList().orEmpty())
        assertEquals(doubleArrayOf(0.0, 1.0, 2.0).asList(), doubleArray.value?.asList().orEmpty())
        assertNull(missing.value)
    }
}
