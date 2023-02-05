package ru.yandex.market.kadavr

import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.yandex.market.kadavr.state.KadavrState
import kotlin.random.Random

object Kadavr {

    private var kadavrApi: KadavrApi = Retrofit.Builder()
        .baseUrl(BuildConfig.KADAVR_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(KadavrApi::class.java)

    fun generateId(): String {
        return "AndroidTest_${System.currentTimeMillis()}_${Random.nextInt(99999)}"
    }

    fun createSession(sessionId: String) {
        kadavrApi.createSession(sessionId).execute()
    }

    fun deleteSession(sessionId: String) {
        kadavrApi.deleteSession(sessionId).execute()
    }

    fun setState(sessionId: String, kadavrState: KadavrState) {
        kadavrApi.setState(
            sessionId,
            kadavrState.getPath(),
            RequestBody.create(MediaType.parse("application/json; charset=utf-8"), kadavrState.getRequestDto().toString())
        )
            .execute()
    }
}