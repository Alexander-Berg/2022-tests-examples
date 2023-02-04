package com.yandex.maps.testapp.search

import com.yandex.mapkit.search.BusinessFilter
import com.yandex.mapkit.search.BusinessFilter.EnumValue
import com.yandex.mapkit.search.Feature
import com.yandex.mapkit.search.FilterCollection
import com.yandex.mapkit.search.FilterCollectionUtils
import java.io.Serializable

private const val RADIO_BUTTON_EMOJI = "\uD83D\uDD18"

private fun groupPrefix(f: BusinessFilter): String {
    return if (f.singleSelect == true) RADIO_BUTTON_EMOJI else ""
}

data class Range(val from: Double, val to: Double): Serializable

data class RangeFilter(
    val id: String,
    val name: String,
    val disabled: Boolean,
    val range: Range
): Serializable
{
    constructor(f: BusinessFilter): this(
        f.id,
        f.name!!,
        f.disabled == true,
        f.values.range!!.run { Range(from, to) }
    )
}

data class DateRange(val from: String, val to: String): Serializable

data class DateFilter(
    val id: String,
    val name: String,
    val disabled: Boolean,
    val range: DateRange? = null
): Serializable
{
    constructor(f: BusinessFilter): this(
        f.id,
        f.name!!,
        f.disabled == true
    )
}

data class Filter(
    val id: String,
    val name: String = "",
    val parentId: String? = null,
    val selected: Boolean = true,
    val disabled: Boolean = false,
    val singleSelect: Boolean = false
): Serializable
{
    constructor(f: BusinessFilter): this(
        f.id,
        f.name!!,
        null,
        f.values.booleans.orEmpty().any { it.selected ?: false },
        f.disabled == true
    )

    constructor(f: BusinessFilter, e: EnumValue): this(
        e.value.id,
        "${groupPrefix(f)}${f.name}: ${e.value.name} ${imageUrlString(e.value)}",
        f.id,
        e.selected == true,
        e.disabled == true || f.disabled == true,
        f.singleSelect == true
    )

    fun isBoolean() = parentId == null
    fun isEnum() = parentId != null
}

data class Filters(
    val filters: List<Filter>,
    val rangeFilters: List<RangeFilter>,
    val dateFilters: List<DateFilter>
): Serializable
{
    constructor(businessFilters: List<BusinessFilter>): this(
        businessFilters.flatMap { it.toFilter() },
        businessFilters
            .filter { it.values.range != null }
            .map { RangeFilter(it) },
        businessFilters
            .filter { it.values.date != null }
            .map { DateFilter(it) }
    )

    fun toFilterCollection(): FilterCollection {
        return FilterCollectionUtils.createFilterCollectionBuilder().run {
            filters
                .filter { it.isBoolean() }
                .forEach { addBooleanFilter(it.id) }
            filters
                .filter { it.isEnum() }
                .groupBy { it.parentId }
                .forEach { group ->
                    addEnumFilter(group.key ?: "", group.value.map { it.id })
                }
            rangeFilters
                .forEach { addRangeFilter(it.id, it.range.from, it.range.to) }
            dateFilters.forEach { filter ->
                filter.range?.let { addDateFilter(filter.id, it.from, it.to) }
            }
            build()
        }
    }
}

fun BusinessFilter.toFilter() = when {
    values.enums != null -> values.enums!!.map { Filter(this, it) }
    values.booleans != null -> listOf(Filter(this))
    else -> emptyList()
}

fun imageUrlString(e: Feature.FeatureEnumValue) =
    if (e.imageUrlTemplate != null) { "(${e.imageUrlTemplate})" } else { "" }
