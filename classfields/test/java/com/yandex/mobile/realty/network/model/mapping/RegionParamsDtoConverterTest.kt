package com.yandex.mobile.realty.network.model.mapping

import com.yandex.mobile.realty.data.mapping.EmptyDescriptor
import com.yandex.mobile.realty.data.model.Filter
import com.yandex.mobile.realty.data.model.FiltersInfo
import com.yandex.mobile.realty.data.model.HeatMapInfoDto
import com.yandex.mobile.realty.data.model.HeatMapLevelDto
import com.yandex.mobile.realty.data.model.ParentRegionDto
import com.yandex.mobile.realty.data.model.RegionParamsDto
import com.yandex.mobile.realty.domain.Rubric
import com.yandex.mobile.realty.domain.model.map.HeatMapInfo
import com.yandex.mobile.realty.domain.model.map.HeatMapLevel
import com.yandex.mobile.realty.domain.model.map.HeatMapType
import com.yandex.mobile.realty.network.model.SchoolInfoDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @author rogovalex on 20.04.18.
 */
class RegionParamsDtoConverterTest {

    @Test
    fun testRegionIdConversion() {
        val input = getInput(rgid = 123)
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertEquals(123, regionParams.regionId)
    }

    @Test
    fun testGeoIdConversion() {
        val input = getInput(geoId = 213)
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertEquals(213, regionParams.geoId)
    }

    @Test
    fun testHeatMapsConversion() {
        val input = getInput(
            heatmapInfos = listOf(
                HeatMapInfoDto(
                    "ecology",
                    "ecology",
                    listOf(
                        HeatMapLevelDto(1, 2, "fffffa"),
                        HeatMapLevelDto(2, 3, "00000a")
                    )
                ),
                HeatMapInfoDto(
                    null,
                    "transport",
                    listOf(
                        HeatMapLevelDto(1, 2, "fffffb"),
                        HeatMapLevelDto(2, 3, "00000b")
                    )
                ),
                HeatMapInfoDto(
                    "infrastructure",
                    "infrastructure",
                    listOf(
                        HeatMapLevelDto(1, 2, "fffffc"),
                        HeatMapLevelDto(2, 3, "00000c")
                    )
                ),
                HeatMapInfoDto(
                    "ijk",
                    null,
                    listOf(
                        HeatMapLevelDto(1, 2, "fffffd"),
                        HeatMapLevelDto(2, 3, "00000d")
                    )
                )
            )
        )
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertEquals(
            mapOf(
                HeatMapType.ECOLOGY to
                    HeatMapInfo(
                        "ecology",
                        listOf(
                            HeatMapLevel(1, 2, 0xfffffffa.toInt()),
                            HeatMapLevel(2, 3, 0xff00000a.toInt())
                        )
                    ),
                HeatMapType.INFRASTRUCTURE to
                    HeatMapInfo(
                        "infrastructure",
                        listOf(
                            HeatMapLevel(1, 2, 0xfffffffc.toInt()),
                            HeatMapLevel(2, 3, 0xff00000c.toInt())
                        )
                    )
            ),
            regionParams.heatMapTypes
        )
    }

    @Test
    fun testSchoolInfoConversion() {
        val input = getInput(
            schoolInfo = SchoolInfoDto().apply {
                lowRatingColor = "111111"
                highRatingColor = "222222"
            }
        )
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertNotNull(regionParams.schoolInfo)
        assertEquals(0xff111111.toInt(), regionParams.schoolInfo?.lowRatingColor)
        assertEquals(0xff222222.toInt(), regionParams.schoolInfo?.highRatingColor)
    }

    @Test
    fun testApartmentSellNewBuildingFiltersConversion() {
        val input = getInput(
            searchFilters = mapOf(
                "APARTMENT:SELL:NEWBUILDING" to filtersInfoOfAllRegionDependentFilters()
            )
        )
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertEquals(
            RegionParamsDto.REGION_DEPENDENT_FILTERS,
            regionParams.filters[Rubric.APARTMENT_SELL_NEWBUILDING]
        )
    }

    @Test
    fun testApartmentSellFiltersConversion() {
        val input = getInput(
            searchFilters = mapOf("APARTMENT:SELL" to filtersInfoOfAllRegionDependentFilters())
        )
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertEquals(
            RegionParamsDto.REGION_DEPENDENT_FILTERS,
            regionParams.filters[Rubric.APARTMENT_SELL]
        )
    }

    @Test
    fun testApartmentRentFiltersConversion() {
        val input = getInput(
            searchFilters = mapOf("APARTMENT:RENT" to filtersInfoOfAllRegionDependentFilters())
        )
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertEquals(
            RegionParamsDto.REGION_DEPENDENT_FILTERS,
            regionParams.filters[Rubric.APARTMENT_RENT]
        )
    }

    @Test
    fun testRoomsSellFiltersConversion() {
        val input = getInput(
            searchFilters = mapOf("ROOMS:SELL" to filtersInfoOfAllRegionDependentFilters())
        )
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertEquals(
            RegionParamsDto.REGION_DEPENDENT_FILTERS,
            regionParams.filters[Rubric.ROOMS_SELL]
        )
    }

    @Test
    fun testRoomsRentFiltersConversion() {
        val input = getInput(
            searchFilters = mapOf("ROOMS:RENT" to filtersInfoOfAllRegionDependentFilters())
        )
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertEquals(
            RegionParamsDto.REGION_DEPENDENT_FILTERS,
            regionParams.filters[Rubric.ROOMS_RENT]
        )
    }

    @Test
    fun testHouseSellFiltersConversion() {
        val input = getInput(
            searchFilters = mapOf("HOUSE:SELL" to filtersInfoOfAllRegionDependentFilters())
        )
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertEquals(
            RegionParamsDto.REGION_DEPENDENT_FILTERS,
            regionParams.filters[Rubric.HOUSE_SELL]
        )
    }

    @Test
    fun testHouseRentFiltersConversion() {
        val input = getInput(
            searchFilters = mapOf("HOUSE:RENT" to filtersInfoOfAllRegionDependentFilters())
        )
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertEquals(
            RegionParamsDto.REGION_DEPENDENT_FILTERS,
            regionParams.filters[Rubric.HOUSE_RENT]
        )
    }

    @Test
    fun testLotSellFiltersConversion() {
        val input = getInput(
            searchFilters = mapOf("LOT:SELL" to filtersInfoOfAllRegionDependentFilters())
        )
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertEquals(
            RegionParamsDto.REGION_DEPENDENT_FILTERS,
            regionParams.filters[Rubric.LOT_SELL]
        )
    }

    @Test
    fun testCommercialSellFiltersConversion() {
        val input = getInput(
            searchFilters = mapOf("COMMERCIAL:SELL" to filtersInfoOfAllRegionDependentFilters())
        )
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertEquals(
            RegionParamsDto.REGION_DEPENDENT_FILTERS,
            regionParams.filters[Rubric.COMMERCIAL_SELL]
        )
    }

    @Test
    fun testCommercialRentFiltersConversion() {
        val input = getInput(
            searchFilters = mapOf("COMMERCIAL:RENT" to filtersInfoOfAllRegionDependentFilters())
        )
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertEquals(
            RegionParamsDto.REGION_DEPENDENT_FILTERS,
            regionParams.filters[Rubric.COMMERCIAL_RENT]
        )
    }

    @Test
    fun testGarageSellFiltersConversion() {
        val input = getInput(
            searchFilters = mapOf("GARAGE:SELL" to filtersInfoOfAllRegionDependentFilters()),
        )
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertEquals(
            RegionParamsDto.REGION_DEPENDENT_FILTERS,
            regionParams.filters[Rubric.GARAGE_SELL]
        )
    }

    @Test
    fun testGarageRentFiltersConversion() {
        val input = getInput(
            searchFilters = mapOf("GARAGE:RENT" to filtersInfoOfAllRegionDependentFilters())
        )
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertEquals(
            RegionParamsDto.REGION_DEPENDENT_FILTERS,
            regionParams.filters[Rubric.GARAGE_RENT]
        )
    }

    @Test
    fun testUnknownRubricFiltersConversion() {
        val input = getInput(
            searchFilters = mapOf("AUTO:SELL" to filtersInfoOfAllRegionDependentFilters())
        )
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertTrue(regionParams.filters.isEmpty())
    }

    @Test
    fun testHasVillagesConversion() {
        val input = getInput(hasVillages = true)
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertTrue(regionParams.hasVillages)
    }

    @Test
    fun testHasNotVillagesConversion() {
        val input = getInput(hasVillages = false)
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertFalse(regionParams.hasVillages)
    }

    @Test
    fun testHasCommercialBuildingsConversion() {
        val input = getInput(hasCommercialBuildings = true)
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertTrue(regionParams.hasCommercialBuildings)
    }

    @Test
    fun testHasNotCommercialBuildingsConversion() {
        val input = getInput(hasCommercialBuildings = false)
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertFalse(regionParams.hasCommercialBuildings)
    }

    @Test
    fun testHasNotConciergeConversion() {
        val input = getInput(hasConcierge = false)
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertFalse(regionParams.hasConcierge)
    }

    @Test
    fun testHasConciergeConversion() {
        val input = getInput(hasConcierge = true)
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertTrue(regionParams.hasConcierge)
    }

    @Test
    fun testHasNotLegendaPromoConversion() {
        val input = getInput(hasDeveloperLegendaPromo = false)
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertFalse(regionParams.hasLegendaPromo)
    }

    @Test
    fun testHasLegendaPromoConversion() {
        val input = getInput(hasDeveloperLegendaPromo = true)
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertTrue(regionParams.hasLegendaPromo)
    }

    @Test
    fun testHasNotPaidSitesConversion() {
        val input = getInput(hasPaidSites = false)
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertFalse(regionParams.hasPaidSites)
    }

    @Test
    fun testHasPaidSitesConversion() {
        val input = getInput(hasPaidSites = true)
        val regionParams = RegionParamsDto.CONVERTER.map(input, EmptyDescriptor)

        assertTrue(regionParams.hasPaidSites)
    }

    private fun getInput(
        rgid: Number? = 0,
        geoId: Number? = 0,
        type: String? = "",
        locative: String? = "",
        heatmapInfos: List<HeatMapInfoDto>? = null,
        searchFilters: Map<String, FiltersInfo>? = null,
        schoolInfo: SchoolInfoDto? = null,
        hasVillages: Boolean? = false,
        hasMetro: Boolean? = false,
        sublocalities: List<Any>? = null,
        hasCommercialBuildings: Boolean? = false,
        hasConcierge: Boolean? = false,
        hasDeveloperLegendaPromo: Boolean? = false,
        hasPaidSites: Boolean? = false,
        parents: List<ParentRegionDto>? = null
    ): RegionParamsDto {
        return RegionParamsDto(
            rgid,
            geoId,
            type,
            locative,
            heatmapInfos,
            searchFilters,
            schoolInfo,
            hasVillages,
            hasMetro,
            sublocalities,
            hasCommercialBuildings,
            hasConcierge,
            hasDeveloperLegendaPromo,
            hasPaidSites,
            parents
        )
    }

    private fun filtersInfoOfAllRegionDependentFilters() = FiltersInfo(
        mapOf(
            "timeToMetro" to Filter("timeToMetro")
        ),
        mapOf(
            "metroTransport" to Filter("metroTransport")
        ),
        RegionParamsDto.REGION_DEPENDENT_FILTERS.associateWith { Filter(it) }
    )
}
