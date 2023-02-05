package com.edadeal.android.helpers

import okhttp3.ResponseBody
import retrofit2.Response

/**
 * фабрика, для генерации тестовых http-ответов
 */
object ResponseFactory {
    fun success(): Response<ResponseBody> {
        return Response.success(null)
    }

    fun error(code: Int): Response<ResponseBody> {
        return Response.error(code, ResponseBody.create(null, ""))
    }
}
