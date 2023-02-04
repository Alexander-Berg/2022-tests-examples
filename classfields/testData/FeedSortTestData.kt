package ru.auto.ara.core.testdata

import ru.auto.ara.R
import ru.auto.ara.core.utils.getResourceString
import ru.auto.data.model.filter.TruckCategory

/**
 * @author themishkun on 06/02/2019.
 */
val NEW_CARS_OPTIONS = listOf(
    R.string.sort_relevance,
    R.string.sort_price_asc,
    R.string.sort_price_desc,
    R.string.sort_name
).map(::getResourceString)

val COMMON_OPTIONS = listOf(
    R.string.sort_date,
    R.string.sort_price_asc,
    R.string.sort_price_desc,
    R.string.sort_year_desc,
    R.string.sort_year_asc,
    R.string.sort_run,
    R.string.sort_name
).map(::getResourceString)

val GROUP_CARD_OPTIONS = listOf(
    R.string.sort_relevance to "",
    R.string.sort_relevance to "sort=fresh_relevance_1-desc",
    R.string.sort_price_asc to "sort=price-asc",
    R.string.sort_price_desc to "sort=price-desc",
    R.string.sort_max_discount_absolute_desc to "sort=max_discount_absolute-desc"
)

val USED_CARS_OPTIONS = listOf(R.string.sort_relevance).map(::getResourceString) +
    COMMON_OPTIONS + listOf(
    R.string.sort_exclusive,
    R.string.sort_deals,
    R.string.sort_proven_owner
).map(::getResourceString)

val TRUCKS_AVAILABLE_FROM_FILTERS = listOf(
    TruckCategory.LCV,
    TruckCategory.TRUCK,
    TruckCategory.ARTIC,
    TruckCategory.BUS,
    TruckCategory.TRAILER
)
