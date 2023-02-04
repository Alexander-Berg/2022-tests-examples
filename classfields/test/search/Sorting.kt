package com.yandex.mobile.realty.test.search

import com.yandex.mobile.realty.core.matchers.SortingDialogLookup
import com.yandex.mobile.realty.core.viewMatchers.NamedStringMatcher
import java.util.*

/**
 * @author misha-kozlov on 2019-09-30
 */
enum class Sorting(val value: String, val matcher: () -> NamedStringMatcher, val expected: String) {
    RELEVANCE("RELEVANCE", SortingDialogLookup::matchesRelevance, "по актуальности"),
    DATE_DESC("DATE_DESC", SortingDialogLookup::matchesDateDesc, "новые предложения"),
    PRICE("PRICE", SortingDialogLookup::matchesPrice, "цена по возрастанию"),
    PRICE_DESC("PRICE_DESC", SortingDialogLookup::matchesPriceDesc, "цена по убыванию"),
    AREA("AREA", SortingDialogLookup::matchesArea, "площадь по возрастанию"),
    AREA_DESC("AREA_DESC", SortingDialogLookup::matchesAreaDesc, "площадь по убыванию"),
    HOUSE_AREA("HOUSE_AREA", SortingDialogLookup::matchesArea, "площадь по возрастанию"),
    HOUSE_AREA_DESC("HOUSE_AREA_DESC", SortingDialogLookup::matchesAreaDesc, "площадь по убыванию"),
    LOT_AREA("LOT_AREA", SortingDialogLookup::matchesLotArea, "площадь участка по возрастанию"),
    LOT_AREA_DESC(
        "LOT_AREA_DESC",
        SortingDialogLookup::matchesLotAreaDesc,
        "площадь участка по убыванию"
    ),
    LAND_AREA("LAND_AREA", SortingDialogLookup::matchesLotArea, "площадь участка по возрастанию"),
    LAND_AREA_DESC(
        "LAND_AREA_DESC",
        SortingDialogLookup::matchesLotAreaDesc,
        "площадь участка по убыванию"
    ),
    PRICE_PER_SQUARE(
        "PRICE_PER_SQUARE",
        SortingDialogLookup::matchesPricePerSquare,
        "цена за м² по возрастанию"
    ),
    PRICE_PER_SQUARE_DESC(
        "PRICE_PER_SQUARE_DESC",
        SortingDialogLookup::matchesPricePerSquareDesc,
        "цена за м² по убыванию"
    ),
    FLOOR("FLOOR", SortingDialogLookup::matchesFloor, "этаж по возрастанию"),
    FLOOR_DESC("FLOOR_DESC", SortingDialogLookup::matchesFloorDesc, "этаж по убыванию"),
    COMMISSIONING_DATE(
        "COMMISSIONING_DATE",
        SortingDialogLookup::matchesCommissioningDate,
        "срок сдачи раньше"
    ),
    COMMISSIONING_DATE_DESC(
        "COMMISSIONING_DATE_DESC",
        SortingDialogLookup::matchesCommissioningDateDesc,
        "срок сдачи позже"
    ),
    LIVING_SPACE("LIVING_SPACE", SortingDialogLookup::matchesLivingSpace, "площадь по возрастанию"),
    LIVING_SPACE_DESC(
        "LIVING_SPACE_DESC",
        SortingDialogLookup::matchesLivingSpaceDesc,
        "площадь по убыванию"
    ),
    CONFIDENCE("CONFIDENCE", SortingDialogLookup::matchesConfidence, "по достоверности");

    companion object {

        val villageValues: EnumSet<Sorting> by lazy {
            EnumSet.of(
                RELEVANCE,
                PRICE,
                PRICE_DESC,
                COMMISSIONING_DATE,
                COMMISSIONING_DATE_DESC
            )
        }

        val siteValues: EnumSet<Sorting> by lazy {
            EnumSet.of(
                RELEVANCE,
                PRICE,
                PRICE_DESC,
                PRICE_PER_SQUARE,
                PRICE_PER_SQUARE_DESC,
                COMMISSIONING_DATE,
                COMMISSIONING_DATE_DESC
            )
        }

        val houseValues: EnumSet<Sorting> by lazy {
            EnumSet.of(
                RELEVANCE,
                DATE_DESC,
                PRICE,
                PRICE_DESC,
                AREA,
                AREA_DESC,
                LOT_AREA,
                LOT_AREA_DESC,
                PRICE_PER_SQUARE,
                PRICE_PER_SQUARE_DESC
            )
        }

        val lotValues: EnumSet<Sorting> by lazy {
            EnumSet.of(
                RELEVANCE,
                DATE_DESC,
                PRICE,
                PRICE_DESC,
                LOT_AREA,
                LOT_AREA_DESC
            )
        }

        val apartmentValues: EnumSet<Sorting> by lazy {
            EnumSet.of(
                RELEVANCE,
                DATE_DESC,
                PRICE,
                PRICE_DESC,
                AREA,
                AREA_DESC,
                PRICE_PER_SQUARE,
                PRICE_PER_SQUARE_DESC,
                FLOOR,
                FLOOR_DESC
            )
        }

        val apartmentRentLongValues: EnumSet<Sorting> by lazy {
            EnumSet.of(
                RELEVANCE,
                DATE_DESC,
                PRICE,
                PRICE_DESC,
                AREA,
                AREA_DESC,
                PRICE_PER_SQUARE,
                PRICE_PER_SQUARE_DESC,
                FLOOR,
                FLOOR_DESC,
                CONFIDENCE
            )
        }

        val roomValues: EnumSet<Sorting> by lazy {
            EnumSet.of(
                RELEVANCE,
                DATE_DESC,
                PRICE,
                PRICE_DESC,
                LIVING_SPACE,
                LIVING_SPACE_DESC,
                PRICE_PER_SQUARE,
                PRICE_PER_SQUARE_DESC,
                FLOOR,
                FLOOR_DESC
            )
        }

        val commercialValues: EnumSet<Sorting> by lazy {
            EnumSet.of(
                RELEVANCE,
                DATE_DESC,
                PRICE,
                PRICE_DESC,
                AREA,
                AREA_DESC,
                PRICE_PER_SQUARE,
                PRICE_PER_SQUARE_DESC
            )
        }

        val commercialLandValues: EnumSet<Sorting> by lazy {
            EnumSet.of(
                RELEVANCE,
                DATE_DESC,
                PRICE,
                PRICE_DESC,
                AREA,
                AREA_DESC
            )
        }
    }
}
