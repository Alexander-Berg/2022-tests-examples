package com.yandex.xplat.xmail

import com.squareup.moshi.Moshi
import com.yandex.xplat.common.DefaultJSONSerializer
import com.yandex.xplat.common.MapJSONItem
import okhttp3.RequestBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RequestEncodingTests {
    private val sampleParams: NetworkParams = MapJSONItem()
            .putString("foo", "bar")
            .putString("фуу", "бар")
            .putString("foo?=&", "bar?=&")
            .putNull("nullable")
            .putBoolean("boolean", true)

    private fun parseRequestBody(body: RequestBody) = body.let {
        val buffer = Buffer()
        it.writeTo(buffer)
        buffer.readUtf8()
    }

    private fun mapRequestBody(body: RequestBody) = parseRequestBody(body).let {
        Moshi.Builder().build()!!.adapter(Map::class.java).fromJson(it)!!
    }

    @Before
    fun initRegistry() {
        Registry.registerJSONSerializer(DefaultJSONSerializer())
    }

    @Test
    fun shouldUrlEncodeRequests() {
        val result = encodeRequest(UrlRequestEncoding(), NetworkMethod.get, sampleParams)

        assertNull(result.body)
        assertEquals(result.queryParameters, sampleParams)
    }

    @Test
    fun testShouldFormEncodeRequests() {
        val result = encodeRequest(UrlRequestEncoding(), NetworkMethod.post, sampleParams)

        assertEquals(parseRequestBody(result.body!!), "boolean=yes&foo=bar&foo%3F%3D%26=bar%3F%3D%26&nullable=null&%D1%84%D1%83%D1%83=%D0%B1%D0%B0%D1%80")
        assertEquals(result.queryParameters, mapOf<String, Any?>())
    }

    @Test
    fun shouldJsonEncodeRequests() {
        val result = encodeRequest(JsonRequestEncoding(), NetworkMethod.get, sampleParams)

        assertEquals(mapRequestBody(result.body!!), sampleParams)
        assertEquals(result.queryParameters, mapOf<String, Any?>())
    }
}
