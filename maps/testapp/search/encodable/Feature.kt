@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.Feature
import com.yandex.mapkit.search.FeatureSet
import com.yandex.mapkit.search.FeatureGroup

class FeatureEncodable(it: Feature) {
    class EnumValueEncodable(it: Feature.FeatureEnumValue) {
        val id: String = it.id
        val name: String = it.name
        val imageUrlTemplate: String? = it.imageUrlTemplate
    }

    class VariantValueEncodable(it: Feature.VariantValue) {
        val booleanValue: Boolean? = it.booleanValue
        val textValue: List<String>? = it.textValue
        val enumValue: List<EnumValueEncodable>? = it.enumValue?.map { EnumValueEncodable(it) }
    }

    val id: String = it.id
    val value: VariantValueEncodable = VariantValueEncodable(it.value)
    val name: String? = it.name
    val aref: String? = it.aref
}

class FeatureSetEncodable(it: FeatureSet) {
    val ids: List<String> = it.ids
}

class FeatureGroupEncodable(it: FeatureGroup) {
    val name: String? = it.name
    val ids: List<String> = it.ids
}
