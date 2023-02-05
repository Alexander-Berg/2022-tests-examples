package ru.yandex.market.kadavr

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import ru.yandex.market.kadavr.dto.response.SessionDto

interface KadavrApi {

    @GET("ping")
    fun ping(): Call<Void>

    @PUT("session/{id}")
    fun createSession(@Path("id") id: String): Call<SessionDto>

    @DELETE("session/{id}")
    fun deleteSession(@Path("id") id: String): Call<String>

    @HEAD("session/{id}")
    fun checkSession(@Path("id") id: String): Call<Void>

    @POST("session/{id}/state/{path}")
    fun setState(@Path("id") id: String, @Path("path") path: String, @Body state: RequestBody): Call<Void>

    @GET("session/{id}/log")
    fun getLog(@Path("id") id: String, @Query("path") path: String): Call<Void>
}