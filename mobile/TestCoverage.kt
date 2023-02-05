package com.yandex.frankenstein.agent.client

import android.util.Base64
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONException
import org.json.JSONObject

private const val CLAZZ = "TestCoverage"

fun CommandImplementations.withJacocoCoverageCommands() = this
        .withCommand(CLAZZ, "collectCoverageData", ::collectCoverageData)

private fun collectCoverageData(input: CommandInput) {
    val results = JSONObject()
    val mainProcessCoverage = try {
        val waitTime = input.arguments.getLong("wait_time")
        encode(JacocoCoverage.getMainProcessCoverage(waitTime))
    } catch (e: JSONException) {
        encode(JacocoCoverage.getMainProcessCoverage())
    }

    results.put("main_process${JacocoCoverage.goodFileSuffix}", mainProcessCoverage)

    JacocoCoverage.getJacocoFiles(input.activity.application).forEach { results.put(it.name, encode(it.readBytes())) }
    val lambdas = input.client.interceptors().asSequence()
        .filterIsInstance<HttpLoggingInterceptor>()
        .map { interceptor ->
            val level = interceptor.level
            interceptor.level = HttpLoggingInterceptor.Level.HEADERS
            { interceptor.level = level }
        }
        .toList()
    input.reportResult(results)
    lambdas.forEach { it.invoke() }
}

private fun encode(data: ByteArray) = Base64.encodeToString(data, Base64.NO_WRAP)
