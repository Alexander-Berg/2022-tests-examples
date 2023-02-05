package com.yandex.xplat.testopithecus.common

import com.yandex.xplat.common.*
import java.net.SocketTimeoutException
import java.net.URI
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer

val NetworkMethod.method: String
    get() = when (this) {
        NetworkMethod.get -> "GET"
        NetworkMethod.post -> "POST"
    }

class DefaultSyncNetwork(
    private val jsonSerializer: JSONSerializer,
    private val logger: Logger,
    networkConfig: NetworkConfig? = null
) : SyncNetwork {
    private val httpClient = OkHttpClient.Builder()
        .apply {
            networkConfig?.sslContextCreator?.createSSLConfiguredClient(this)
            networkConfig?.stethoProxy?.patch(this)
        }
        .build()

    override fun syncExecute(baseUrl: String, request: NetworkRequest, oauthToken: String?): Result<String> = runRequest(createHttpRequest(baseUrl, request, oauthToken))
    override fun syncExecuteWithRetries(
        retries: Int,
        baseUrl: String,
        request: NetworkRequest,
        oauthToken: String?
    ): Result<String> {
        var result: Result<String>? = null
        var retriesLeft = retries
        while (retriesLeft >= 0) {
            result = syncExecute(baseUrl, request, oauthToken)
            if (result!!.isValue()) {
                return result!!
            }
            retriesLeft--
        }
        return result!!
    }

    private fun runRequest(request: Request): Result<String> {
        logger.info(this.stringRepresentation(request))
        var response: Response? = null
        try {
            response = httpClient.newCall(request).execute()
        } catch (e: SocketTimeoutException) {
            return resultError(YSError("Request timeout!"))
        }
        if ((response == null) || (response.code() / 10 != 20)) {
            val responseBody = if (response == null) "" else response!!.body()?.string()
            val message = "Non 20x response code for request ${request.url()}, message: $responseBody"
            return resultError(YSError(message))
        }
        return resultValue(response.body()!!.string())
    }

    private fun stringRepresentation(request: Request): String {
        val sb = java.lang.StringBuilder()
        sb.append("curl ").append("'${request.url()}'").append(" -d ")

        val copy = request.newBuilder().build()
        val buffer = Buffer()
        copy.body()?.writeTo(buffer)
        val data = buffer.readUtf8()

        sb.append("'$data'")
        for ((key, value) in request.headers().toMultimap()) {
            sb.append(" -H '$key: ${value[0]}'")
        }
        return sb.toString()
    }

    private fun createHttpRequest(baseUrl: String, request: NetworkRequest, oauthToken: String?): Request {
        val encodedParameters = encodeRequest(this.jsonSerializer, request.encoding(), request.method(), request.params())

        val urlBuilder = HttpUrl.get(URI.create(baseUrl))!!.newBuilder()
        urlBuilder.addPathSegments(request.targetPath())

        val queryParameters: Map<String, *> = JSONItemSupport.from(request.urlExtra()) as Map<String, *> + encodedParameters.queryParameters
        queryParameters.forEach { (key, value) ->
            stringifyQueryParam(value)?.let { urlBuilder.addQueryParameter(key, it) }
        }

        val builder = Request.Builder().url(urlBuilder.build())
            .addHeader("Connection", "keep-alive")
            .addHeader("Content-Type", "application/json")
        if (oauthToken != null) {
            builder.addHeader("Authorization", "OAuth $oauthToken")
        }

        val headers = JSONItemSupport.from(request.headersExtra()) as Map<String, *>
        headers.forEach { (key, value) ->
            stringifyQueryParam(value)?.let { builder.addHeader(key, it) }
        }

        if (request.params().asMap().isEmpty()) {
            builder.method(request.method().method, null)
        } else {
            builder.method(request.method().method, encodedParameters.body)
        }
        return builder.build()
    }
}

fun stringifyQueryParam(value: Any?): String? =
    when (value) {
        is Number -> value.toString()
        is String -> value
        true -> "yes"
        false -> "no"
        null -> "null"
        is StringJSONItem -> value.value
        is IntegerJSONItem -> value.asInt64().toString()
        is BooleanJSONItem -> value.value.toString()
        else -> throw NotValidQueryTypeException()
    }

class NotValidQueryTypeException : Exception()
