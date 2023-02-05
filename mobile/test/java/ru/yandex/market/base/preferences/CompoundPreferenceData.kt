package ru.yandex.market.base.preferences

import com.google.gson.annotations.SerializedName

@Suppress("DtoLayerRule")
data class CompoundPreferenceData(
    @SerializedName("string_value") val stringValue: String?,
    @SerializedName("int_value") val intValue: Int?,
    @SerializedName("boolean_value") val booleanValue: Boolean?
)
