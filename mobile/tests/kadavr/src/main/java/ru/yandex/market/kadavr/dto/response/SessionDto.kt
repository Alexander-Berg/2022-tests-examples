package ru.yandex.market.kadavr.dto.response

import com.google.gson.annotations.SerializedName

data class SessionDto(
    @SerializedName("id") val id: String
)