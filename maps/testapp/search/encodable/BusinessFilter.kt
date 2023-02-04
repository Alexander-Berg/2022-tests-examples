@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.BusinessFilter
import com.yandex.mapkit.search.FilterSet

class BusinessFilterEncodable(it: BusinessFilter) {
    class BooleanValueEncodable(it: BusinessFilter.BooleanValue) {
        val value: Boolean = it.value
        val selected: Boolean? = it.selected
    }

    class EnumValueEncodable(it: BusinessFilter.EnumValue) {
        val value: FeatureEncodable.EnumValueEncodable = FeatureEncodable.EnumValueEncodable(it.value)
        val selected: Boolean? = it.selected
        val disabled: Boolean? = it.disabled
    }

    class RangeValueEncodable(it: BusinessFilter.RangeValue) {
        val from: Double = it.from
        val to: Double = it.to
    }

    class DateValueEncodable(it: BusinessFilter.DateValue) {
        val reserved: Int = it.reserved
    }

    val id: String = it.id
    val name: String? = it.name
    val disabled: Boolean? = it.disabled
    val singleSelect: Boolean? = it.singleSelect
    class ValuesEncodable(it: BusinessFilter.Values) {
        val booleans: List<BooleanValueEncodable>? = it.booleans?.map { BooleanValueEncodable(it) }
        val enums: List<EnumValueEncodable>? = it.enums?.map { EnumValueEncodable(it) }
        val range: RangeValueEncodable? = it.range?.let { RangeValueEncodable(it) }
        val date: DateValueEncodable? = it.date?.let { DateValueEncodable(it) }
    }

    val values: ValuesEncodable = ValuesEncodable(it.values)
}

class FilterSetEncodable(it: FilterSet) {
    val ids: List<String> = it.ids
}



