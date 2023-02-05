package com.yandex.frankenstein.agent.client

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection

fun requestTestCase(client: OkHttpClient, caseRequestUrl: String, processTestCase: (JSONObject) -> Unit) {
    val request = Request.Builder()
        .url(caseRequestUrl)
        .build()

    client.newCall(request).enqueue(object : Callback {

        override fun onFailure(call: Call, e: IOException) {}

        override fun onResponse(call: Call, response: Response) {
            if (response.code() == HttpURLConnection.HTTP_OK) {
                response.body()?.let { body ->
                    processTestCase(JSONObject(body.string()))
                }
            }
        }
    })
}
