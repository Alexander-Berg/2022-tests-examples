package ru.yandex.yandexbus.inhouse.service.taxi.uber

import okhttp3.MediaType
import okhttp3.Request
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SuccessfulCall<T>(private val response: T) : TestRetrofitCall<T>() {
    override fun enqueue(callback: Callback<T>) {
        callback.onResponse(this, Response.success(response))
    }
}

class ErrorCall<T>(private val error: Throwable) : TestRetrofitCall<T>() {
    override fun enqueue(callback: Callback<T>) {
        callback.onFailure(this, error)
    }
}

class NetworkErrorCall<T> : TestRetrofitCall<T>() {
    override fun enqueue(callback: Callback<T>) {
        val responseBody = object : ResponseBody() {
            override fun contentLength() = 0L

            override fun contentType(): MediaType? = null

            override fun source(): BufferedSource = Buffer()
        }

        callback.onResponse(this, Response.error<T>(400, responseBody))
    }
}

class EmptyCall<T> : TestRetrofitCall<T>() {
    override fun enqueue(callback: Callback<T>) {
        // do nothing
    }
}

open class TestRetrofitCall<T> : Call<T> {

    private var isCancelled = false

    override fun isCanceled() = isCancelled

    override fun cancel() {
        isCancelled = true
    }

    override fun enqueue(callback: Callback<T>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isExecuted(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clone(): Call<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun execute(): Response<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun request(): Request {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}