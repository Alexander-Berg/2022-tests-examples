package ru.yandex.market.kadavr.state

import com.google.gson.JsonElement

abstract class KadavrState {

    abstract fun getPath(): String

    abstract fun getRequestDto(): JsonElement
}