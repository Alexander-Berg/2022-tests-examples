package com.edadeal.android.util

import com.edadeal.android.metrics.JsEventCollectorHelper
import org.junit.Test
import kotlin.test.assertEquals

class JsEventCollectorHelperTest {

    @Test
    fun `enrich should return valid json`() {
        val json = "{\"WebViewVersion\":\"1.0.0\",\"ScreenName\":\"SearchScreen\",\"FromScreen\":\"OffersScreen\"," +
            "\"SearchFilter\":{\"searchType\":\"offers\",\"currentGeoId\":\"213\",\"retailerUuid\":\"54ad541b-5856" +
            "-4398-b3f0-f486f6389c29\"},\"NullValue\":null,\"OffersScreenType\":\"SearchAll\"," +
            "\"LastSearchesSuggest\":[\"eggs\",\"рыба\",\"部\"]}"
        val superArgs = mutableMapOf<String, Any>("A" to 42, "ScreenName" to "Unknown")
        val helper = JsEventCollectorHelper()

        val expected = "{\"WebViewVersion\":\"1.0.0\",\"ScreenName\":\"SearchScreen\",\"FromScreen\":\"OffersScreen\"," +
            "\"SearchFilter\":{\"searchType\":\"offers\",\"currentGeoId\":\"213\",\"retailerUuid\":\"54ad541b-5856" +
            "-4398-b3f0-f486f6389c29\"},\"NullValue\":null,\"OffersScreenType\":\"SearchAll\"," +
            "\"LastSearchesSuggest\":[\"eggs\",\"рыба\",\"部\"],\"A\":42}"
        assertEquals(expected, helper.enrich(json, superArgs))
    }

    @Test
    fun `enrich should ignore blacklisted super args`() {
        val json = "{\"object\":{\"name\":\"value\",\"bool\":false,\"int\":1,\"float\":1.0},\"name\":\"\"}"
        val superArgs = mutableMapOf<String, Any>("A" to 42, "B" to 0)
        val helper = JsEventCollectorHelper("B")

        val expected = "{\"object\":{\"name\":\"value\",\"bool\":false,\"int\":1,\"float\":1.0},\"name\":\"\",\"A\":42}"
        assertEquals(expected, helper.enrich(json, superArgs))
    }

    @Test
    fun `enrich should return input json if it have unexpected content`() {
        val json = "[{\"object\":{\"name\":\"value\",\"bool\":false,\"int\":1,\"float\":1.0},\"name\":\"\"}]"
        val superArgs = mutableMapOf<String, Any>("A" to 42, "B" to 0)
        val helper = JsEventCollectorHelper("B")

        assertEquals(json, helper.enrich(json, superArgs))
    }
}
