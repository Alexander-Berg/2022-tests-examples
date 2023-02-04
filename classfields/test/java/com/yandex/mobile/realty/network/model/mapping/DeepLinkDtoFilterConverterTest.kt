package com.yandex.mobile.realty.network.model.mapping

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.yandex.mobile.realty.data.mapping.EmptyDescriptor
import com.yandex.mobile.realty.data.model.CommuteDto
import com.yandex.mobile.realty.data.model.GeoObjectDto
import com.yandex.mobile.realty.data.model.GeoPolygonDto
import com.yandex.mobile.realty.data.model.GeoRegionDto
import com.yandex.mobile.realty.data.model.deeplink.DeepLinkDto
import com.yandex.mobile.realty.data.model.search.ParamsItemDto
import com.yandex.mobile.realty.deeplink.DeepLinkParsedData
import com.yandex.mobile.realty.deeplink.OfferListDeepLinkContext
import com.yandex.mobile.realty.deeplink.SiteListDeepLinkContext
import com.yandex.mobile.realty.deeplink.VillageListDeepLinkContext
import com.yandex.mobile.realty.domain.model.Range
import com.yandex.mobile.realty.domain.model.common.CommissioningState
import com.yandex.mobile.realty.domain.model.common.Quarter
import com.yandex.mobile.realty.domain.model.common.QuarterOfYear
import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.domain.model.search.SearchTag
import com.yandex.mobile.realty.network.model.FilterParamsNames.AGENTS
import com.yandex.mobile.realty.network.model.FilterParamsNames.AIRCONDITION
import com.yandex.mobile.realty.network.model.FilterParamsNames.APARTMENT_TYPE
import com.yandex.mobile.realty.network.model.FilterParamsNames.BALCONY
import com.yandex.mobile.realty.network.model.FilterParamsNames.BATHROOM
import com.yandex.mobile.realty.network.model.FilterParamsNames.BUILDING_CLASS
import com.yandex.mobile.realty.network.model.FilterParamsNames.BUILDING_EPOCH
import com.yandex.mobile.realty.network.model.FilterParamsNames.BUILDING_SERIES_ID
import com.yandex.mobile.realty.network.model.FilterParamsNames.BUILDING_TYPE
import com.yandex.mobile.realty.network.model.FilterParamsNames.BUILD_YEAR
import com.yandex.mobile.realty.network.model.FilterParamsNames.CATEGORY
import com.yandex.mobile.realty.network.model.FilterParamsNames.CEILING_HEIGHT_MIN
import com.yandex.mobile.realty.network.model.FilterParamsNames.CHILDREN
import com.yandex.mobile.realty.network.model.FilterParamsNames.CLEANING_INCLUDED
import com.yandex.mobile.realty.network.model.FilterParamsNames.COMMERCIAL_BUILDING_TYPE
import com.yandex.mobile.realty.network.model.FilterParamsNames.COMMERCIAL_PLAN_TYPE
import com.yandex.mobile.realty.network.model.FilterParamsNames.COMMERCIAL_TYPE
import com.yandex.mobile.realty.network.model.FilterParamsNames.COMMISSION
import com.yandex.mobile.realty.network.model.FilterParamsNames.COMMISSIONING_DATE
import com.yandex.mobile.realty.network.model.FilterParamsNames.DEAL_STATUS
import com.yandex.mobile.realty.network.model.FilterParamsNames.DECORATION
import com.yandex.mobile.realty.network.model.FilterParamsNames.DEMOLITION
import com.yandex.mobile.realty.network.model.FilterParamsNames.DEVELOPER
import com.yandex.mobile.realty.network.model.FilterParamsNames.DISHWASHER
import com.yandex.mobile.realty.network.model.FilterParamsNames.ELECTRICITY_INCLUDED
import com.yandex.mobile.realty.network.model.FilterParamsNames.ELECTRICITY_SUPPLY
import com.yandex.mobile.realty.network.model.FilterParamsNames.ENTRANCE_TYPE
import com.yandex.mobile.realty.network.model.FilterParamsNames.EXCLUDE_TAG
import com.yandex.mobile.realty.network.model.FilterParamsNames.EXPECT_METRO
import com.yandex.mobile.realty.network.model.FilterParamsNames.FLOOR
import com.yandex.mobile.realty.network.model.FilterParamsNames.FLOORS
import com.yandex.mobile.realty.network.model.FilterParamsNames.FLOOR_EXCEPT_FIRST
import com.yandex.mobile.realty.network.model.FilterParamsNames.FURNITURE
import com.yandex.mobile.realty.network.model.FilterParamsNames.GARAGE_TYPE
import com.yandex.mobile.realty.network.model.FilterParamsNames.GAS_SUPPLY
import com.yandex.mobile.realty.network.model.FilterParamsNames.HAS_FEE
import com.yandex.mobile.realty.network.model.FilterParamsNames.HAS_INSTALLMENT
import com.yandex.mobile.realty.network.model.FilterParamsNames.HAS_MATERNITY_FUNDS
import com.yandex.mobile.realty.network.model.FilterParamsNames.HAS_MILITARY_MORTGAGE
import com.yandex.mobile.realty.network.model.FilterParamsNames.HAS_MORTGAGE
import com.yandex.mobile.realty.network.model.FilterParamsNames.HAS_PARKING
import com.yandex.mobile.realty.network.model.FilterParamsNames.HAS_RAILWAY_STATION
import com.yandex.mobile.realty.network.model.FilterParamsNames.HAS_SECURITY
import com.yandex.mobile.realty.network.model.FilterParamsNames.HAS_SPECIAL_PROPOSAL
import com.yandex.mobile.realty.network.model.FilterParamsNames.HAS_TWENTY_FOUR_SEVEN
import com.yandex.mobile.realty.network.model.FilterParamsNames.HAS_VENTILATION
import com.yandex.mobile.realty.network.model.FilterParamsNames.HEATING_SUPPLY
import com.yandex.mobile.realty.network.model.FilterParamsNames.HOUSE_AREA
import com.yandex.mobile.realty.network.model.FilterParamsNames.HOUSE_TYPE
import com.yandex.mobile.realty.network.model.FilterParamsNames.INCLUDE_TAG
import com.yandex.mobile.realty.network.model.FilterParamsNames.KITCHEN_AREA
import com.yandex.mobile.realty.network.model.FilterParamsNames.LAND_TYPE
import com.yandex.mobile.realty.network.model.FilterParamsNames.LAST_FLOOR
import com.yandex.mobile.realty.network.model.FilterParamsNames.LIVING_AREA
import com.yandex.mobile.realty.network.model.FilterParamsNames.LOT_AREA
import com.yandex.mobile.realty.network.model.FilterParamsNames.LOT_TYPE
import com.yandex.mobile.realty.network.model.FilterParamsNames.METRO_DISTANCE
import com.yandex.mobile.realty.network.model.FilterParamsNames.METRO_DISTANCE_TYPE
import com.yandex.mobile.realty.network.model.FilterParamsNames.NB_DEAL_TYPE
import com.yandex.mobile.realty.network.model.FilterParamsNames.NEW_FLAT
import com.yandex.mobile.realty.network.model.FilterParamsNames.OFFICE_CLASS
import com.yandex.mobile.realty.network.model.FilterParamsNames.ONLINE_SHOW
import com.yandex.mobile.realty.network.model.FilterParamsNames.PARKING
import com.yandex.mobile.realty.network.model.FilterParamsNames.PARK_TYPE
import com.yandex.mobile.realty.network.model.FilterParamsNames.PETS
import com.yandex.mobile.realty.network.model.FilterParamsNames.POND_TYPE
import com.yandex.mobile.realty.network.model.FilterParamsNames.PRICE
import com.yandex.mobile.realty.network.model.FilterParamsNames.PRICE_TYPE
import com.yandex.mobile.realty.network.model.FilterParamsNames.PRICING_PERIOD
import com.yandex.mobile.realty.network.model.FilterParamsNames.PRIMARY_SALE
import com.yandex.mobile.realty.network.model.FilterParamsNames.PROPERTY_AREA
import com.yandex.mobile.realty.network.model.FilterParamsNames.RANGE_MAX_PREFIX
import com.yandex.mobile.realty.network.model.FilterParamsNames.RANGE_MAX_SUFFIX
import com.yandex.mobile.realty.network.model.FilterParamsNames.RANGE_MIN_PREFIX
import com.yandex.mobile.realty.network.model.FilterParamsNames.RANGE_MIN_SUFFIX
import com.yandex.mobile.realty.network.model.FilterParamsNames.REFRIGERATOR
import com.yandex.mobile.realty.network.model.FilterParamsNames.RENOVATION
import com.yandex.mobile.realty.network.model.FilterParamsNames.RENT_TIME
import com.yandex.mobile.realty.network.model.FilterParamsNames.ROOMS_COUNT
import com.yandex.mobile.realty.network.model.FilterParamsNames.SEWERAGE_SUPPLY
import com.yandex.mobile.realty.network.model.FilterParamsNames.SHOW_OUTDATED
import com.yandex.mobile.realty.network.model.FilterParamsNames.TAXATION_FORM
import com.yandex.mobile.realty.network.model.FilterParamsNames.TELEVISION
import com.yandex.mobile.realty.network.model.FilterParamsNames.TYPE
import com.yandex.mobile.realty.network.model.FilterParamsNames.USER
import com.yandex.mobile.realty.network.model.FilterParamsNames.UTILITIES_INCLUDED
import com.yandex.mobile.realty.network.model.FilterParamsNames.VILLAGE_CLASS
import com.yandex.mobile.realty.network.model.FilterParamsNames.VILLAGE_OFFER_TYPE
import com.yandex.mobile.realty.network.model.FilterParamsNames.WALLS_TYPE
import com.yandex.mobile.realty.network.model.FilterParamsNames.WASHING_MACHINE
import com.yandex.mobile.realty.network.model.FilterParamsNames.WATER_SUPPLY
import com.yandex.mobile.realty.network.model.FilterParamsNames.WITH_EXCERPT_REPORT
import com.yandex.mobile.realty.network.model.FilterParamsNames.WITH_PHOTO
import com.yandex.mobile.realty.network.model.FilterParamsNames.WITH_VIDEO
import com.yandex.mobile.realty.network.model.FilterParamsNames.YANDEX_RENT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * @author rogovalex on 2019-10-03.
 */
class DeepLinkDtoFilterConverterTest {

    private val converter = DeepLinkDto.Converter(Gson())

    @Test
    fun shouldConvertSellApartmentFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("SELL")),
                ParamsItemDto(CATEGORY, listOf("APARTMENT")),
                ParamsItemDto(NEW_FLAT, listOf("NO")),
                ParamsItemDto(ROOMS_COUNT, listOf("ONE")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_METER")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("5")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_FOOT")),
                ParamsItemDto(PROPERTY_AREA + RANGE_MIN_SUFFIX, listOf("11")),
                ParamsItemDto(KITCHEN_AREA + RANGE_MAX_SUFFIX, listOf("19")),
                ParamsItemDto(CEILING_HEIGHT_MIN, listOf("2.5")),
                ParamsItemDto(RENOVATION, listOf("EURO")),
                ParamsItemDto(BALCONY, listOf("BALCONY")),
                ParamsItemDto(BATHROOM, listOf("MATCHED")),
                ParamsItemDto(FLOOR + RANGE_MIN_SUFFIX, listOf("2")),
                ParamsItemDto(LAST_FLOOR, listOf("NO")),
                ParamsItemDto(FLOOR_EXCEPT_FIRST, listOf("YES")),
                ParamsItemDto(BUILD_YEAR + RANGE_MAX_SUFFIX, listOf("2010")),
                ParamsItemDto(BUILDING_TYPE, listOf("MONOLIT")),
                ParamsItemDto(BUILDING_EPOCH, listOf("STALIN")),
                ParamsItemDto(APARTMENT_TYPE, listOf("YES")),
                ParamsItemDto(RANGE_MAX_PREFIX + FLOORS, listOf("3")),
                ParamsItemDto(DEMOLITION, listOf("YES")),
                ParamsItemDto(PARKING, listOf("UNDERGROUND")),
                ParamsItemDto(PARK_TYPE, listOf("FOREST")),
                ParamsItemDto(POND_TYPE, listOf("SEA")),
                ParamsItemDto(EXPECT_METRO, listOf("YES")),
                ParamsItemDto(AGENTS, listOf("NO")),
                ParamsItemDto(WITH_PHOTO, listOf("YES")),
                ParamsItemDto(ONLINE_SHOW, listOf("YES")),
                ParamsItemDto(WITH_VIDEO, listOf("YES")),
                ParamsItemDto(WITH_EXCERPT_REPORT, listOf("YES")),
                ParamsItemDto(BUILDING_SERIES_ID, listOf("663298")),
                ParamsItemDto(FURNITURE, listOf("YES")),
                ParamsItemDto(DEAL_STATUS, listOf("COUNTERSALE")),
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.SellApartment

        assertEquals(Filter.Market.SECONDARY, filter.market)
        assertEquals(setOf(Filter.RoomsCount.ONE), filter.roomsCount)
        assertEquals(Filter.PriceType.PER_METER, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(5, Filter.MetroRemoteness.Unit.ON_FOOT),
            filter.metroRemoteness
        )
        assertEquals(Range.valueOf(11L, null), filter.propertyArea)
        assertEquals(Range.valueOf(null, 19L), filter.kitchenArea)
        assertEquals(2.5f, filter.ceilingHeightMin)
        assertEquals(setOf(Filter.Renovation.EURO), filter.renovation)
        assertEquals(Filter.Balcony.BALCONY, filter.balcony)
        assertEquals(Filter.Bathroom.MATCHED, filter.bathroom)
        assertEquals(Range.valueOf(2L, null), filter.floor)
        assertEquals(false, filter.lastFloor)
        assertEquals(true, filter.exceptFirstFloor)
        assertEquals(Range.valueOf(null, 2010L), filter.builtYear)
        assertEquals(setOf(Filter.BuildingType.MONOLIT), filter.buildingType)
        assertEquals(setOf(Filter.BuildingEpoch.STALIN), filter.buildingEpoch)
        assertEquals(true, filter.apartments)
        assertEquals(Range.valueOf(null, 3L), filter.floors)
        assertEquals(true, filter.demolition)
        assertEquals(setOf(Filter.ParkingType.UNDERGROUND), filter.parkingType)
        assertEquals(setOf(Filter.ParkType.FOREST), filter.parkType)
        assertEquals(setOf(Filter.PondType.SEA), filter.pondType)
        assertEquals(true, filter.expectMetro)
        assertEquals(false, filter.showFromAgents)
        assertEquals(true, filter.withPhoto)
        assertEquals(true, filter.onlineShow)
        assertEquals(true, filter.withVideo)
        assertEquals(true, filter.withExcerptReport)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
        assertEquals(true, filter.withFurniture)
        assertEquals(Filter.BuildingSeries(663_298, "1-510"), filter.buildingSeries)
        assertEquals(Filter.DealStatus.COUNTERSALE, filter.dealStatus)
    }

    @Test
    fun shouldConvertSiteApartmentFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "SITE_LIST",
            params = listOf(
                ParamsItemDto(ROOMS_COUNT, listOf("TWO")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_METER")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("10")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_TRANSPORT")),
                ParamsItemDto(PROPERTY_AREA + RANGE_MIN_SUFFIX, listOf("11")),
                ParamsItemDto(KITCHEN_AREA + RANGE_MAX_SUFFIX, listOf("19")),
                ParamsItemDto(CEILING_HEIGHT_MIN, listOf("2.7")),
                ParamsItemDto(DECORATION, listOf("CLEAN")),
                ParamsItemDto(BATHROOM, listOf("SEPARATED")),
                ParamsItemDto(FLOOR + RANGE_MIN_SUFFIX, listOf("2")),
                ParamsItemDto(LAST_FLOOR, listOf("YES")),
                ParamsItemDto(COMMISSIONING_DATE, listOf("FINISHED")),
                ParamsItemDto(BUILDING_TYPE, listOf("PANEL")),
                ParamsItemDto(BUILDING_CLASS, listOf("ECONOM")),
                ParamsItemDto(APARTMENT_TYPE, listOf("YES")),
                ParamsItemDto(RANGE_MIN_PREFIX + FLOORS, listOf("5")),
                ParamsItemDto(PARKING, listOf("OPEN")),
                ParamsItemDto(PARK_TYPE, listOf("FOREST")),
                ParamsItemDto(POND_TYPE, listOf("SEA")),
                ParamsItemDto(EXPECT_METRO, listOf("YES")),
                ParamsItemDto(HAS_MORTGAGE, listOf("YES")),
                ParamsItemDto(HAS_SPECIAL_PROPOSAL, listOf("YES")),
                ParamsItemDto(HAS_INSTALLMENT, listOf("YES")),
                ParamsItemDto(NB_DEAL_TYPE, listOf("FZ_214")),
                ParamsItemDto(HAS_MATERNITY_FUNDS, listOf("YES")),
                ParamsItemDto(HAS_MILITARY_MORTGAGE, listOf("YES")),
                ParamsItemDto(SHOW_OUTDATED, listOf("YES")),
            ),
            filter = JsonObject().apply {
                add(
                    DEVELOPER,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("name", "developer")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as SiteListDeepLinkContext

        val filter = context.filter

        assertEquals(setOf(Filter.RoomsCount.TWO), filter.roomsCount)
        assertEquals(Filter.PriceType.PER_METER, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_TRANSPORT),
            filter.metroRemoteness
        )
        assertEquals(Range.valueOf(11L, null), filter.propertyArea)
        assertEquals(Range.valueOf(null, 19L), filter.kitchenArea)
        assertEquals(2.7f, filter.ceilingHeightMin)
        assertEquals(setOf(Filter.Decoration.CLEAN), filter.decoration)
        assertEquals(Filter.Bathroom.SEPARATED, filter.bathroom)
        assertEquals(Range.valueOf(2L, null), filter.floor)
        assertEquals(CommissioningState.Delivered, filter.commissioningState)
        assertEquals(setOf(Filter.BuildingType.PANEL), filter.buildingType)
        assertEquals(setOf(Filter.BuildingClass.ECONOM), filter.buildingClass)
        assertEquals(true, filter.apartments)
        assertEquals(Range.valueOf(5L, null), filter.floors)
        assertEquals(setOf(Filter.ParkingType.OPEN), filter.parkingType)
        assertEquals(Filter.Developer(1, "developer"), filter.developer)
        assertEquals(setOf(Filter.ParkType.FOREST), filter.parkType)
        assertEquals(setOf(Filter.PondType.SEA), filter.pondType)
        assertEquals(true, filter.expectMetro)
        assertEquals(true, filter.hasMortgage)
        assertEquals(true, filter.hasSpecialProposal)
        assertEquals(true, filter.hasInstallment)
        assertEquals(true, filter.law214)
        assertEquals(true, filter.hasMaternityFunds)
        assertEquals(true, filter.hasMilitaryMortgage)
        assertEquals(true, filter.showOutdated)
        assertEquals(true, filter.lastFloor)
    }

    @Test
    fun shouldConvertRentApartmentFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("RENT")),
                ParamsItemDto(CATEGORY, listOf("APARTMENT")),
                ParamsItemDto(RENT_TIME, listOf("LARGE")),
                ParamsItemDto(ROOMS_COUNT, listOf("THREE")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_OFFER")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("5")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_FOOT")),
                ParamsItemDto(PROPERTY_AREA + RANGE_MIN_SUFFIX, listOf("11")),
                ParamsItemDto(KITCHEN_AREA + RANGE_MAX_SUFFIX, listOf("19")),
                ParamsItemDto(CEILING_HEIGHT_MIN, listOf("3")),
                ParamsItemDto(RENOVATION, listOf("EURO")),
                ParamsItemDto(BALCONY, listOf("LOGGIA")),
                ParamsItemDto(FLOOR + RANGE_MIN_SUFFIX, listOf("2")),
                ParamsItemDto(LAST_FLOOR, listOf("NO")),
                ParamsItemDto(FLOOR_EXCEPT_FIRST, listOf("YES")),
                ParamsItemDto(FURNITURE, listOf("YES")),
                ParamsItemDto(REFRIGERATOR, listOf("YES")),
                ParamsItemDto(DISHWASHER, listOf("YES")),
                ParamsItemDto(AIRCONDITION, listOf("YES")),
                ParamsItemDto(TELEVISION, listOf("YES")),
                ParamsItemDto(WASHING_MACHINE, listOf("YES")),
                ParamsItemDto(CHILDREN, listOf("YES")),
                ParamsItemDto(PETS, listOf("YES")),
                ParamsItemDto(BUILD_YEAR + RANGE_MAX_SUFFIX, listOf("2010")),
                ParamsItemDto(BUILDING_TYPE, listOf("BRICK")),
                ParamsItemDto(BUILDING_EPOCH, listOf("BREZHNEV")),
                ParamsItemDto(RANGE_MIN_PREFIX + FLOORS, listOf("3")),
                ParamsItemDto(RANGE_MAX_PREFIX + FLOORS, listOf("5")),
                ParamsItemDto(PARKING, listOf("CLOSED")),
                ParamsItemDto(DEMOLITION, listOf("YES")),
                ParamsItemDto(PARK_TYPE, listOf("FOREST")),
                ParamsItemDto(POND_TYPE, listOf("SEA")),
                ParamsItemDto(EXPECT_METRO, listOf("YES")),
                ParamsItemDto(AGENTS, listOf("NO")),
                ParamsItemDto(HAS_FEE, listOf("NO")),
                ParamsItemDto(ONLINE_SHOW, listOf("NO")),
                ParamsItemDto(WITH_VIDEO, listOf("NO")),
                ParamsItemDto(YANDEX_RENT, listOf("YES")),
                ParamsItemDto(BUILDING_SERIES_ID, listOf("663298"))
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "7")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agent1")
                                addProperty("userType", "AGENT")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.RentApartment

        assertEquals(Filter.RentTime.LARGE, filter.rentTime)
        assertEquals(setOf(Filter.RoomsCount.THREE), filter.roomsCount)
        assertEquals(Filter.PriceType.PER_OFFER, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(5, Filter.MetroRemoteness.Unit.ON_FOOT),
            filter.metroRemoteness
        )
        assertEquals(Range.valueOf(11L, null), filter.propertyArea)
        assertEquals(Range.valueOf(null, 19L), filter.kitchenArea)
        assertEquals(3f, filter.ceilingHeightMin)
        assertEquals(setOf(Filter.Renovation.EURO), filter.renovation)
        assertEquals(Filter.Balcony.LOGGIA, filter.balcony)
        assertEquals(Range.valueOf(2L, null), filter.floor)
        assertEquals(false, filter.lastFloor)
        assertEquals(true, filter.exceptFirstFloor)
        assertEquals(true, filter.withFurniture)
        assertEquals(true, filter.withRefrigerator)
        assertEquals(true, filter.withDishwasher)
        assertEquals(true, filter.withAircondition)
        assertEquals(true, filter.withTV)
        assertEquals(true, filter.withWashingMachine)
        assertEquals(true, filter.withChildren)
        assertEquals(true, filter.withPets)
        assertEquals(Range.valueOf(null, 2010L), filter.builtYear)
        assertEquals(setOf(Filter.BuildingType.BRICK), filter.buildingType)
        assertEquals(setOf(Filter.BuildingEpoch.BREZHNEV), filter.buildingEpoch)
        assertEquals(Range.valueOf(3L, 5L), filter.floors)
        assertEquals(setOf(Filter.ParkingType.CLOSED), filter.parkingType)
        assertEquals(true, filter.demolition)
        assertEquals(setOf(Filter.ParkType.FOREST), filter.parkType)
        assertEquals(setOf(Filter.PondType.SEA), filter.pondType)
        assertEquals(true, filter.expectMetro)
        assertEquals(false, filter.showFromAgents)
        assertEquals(false, filter.hasFee)
        assertEquals(true, filter.yandexRent)
        assertEquals(false, filter.onlineShow)
        assertEquals(false, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("7", "agent1", Filter.Agency.Type.AGENT), filter.agency)
        assertEquals(Filter.BuildingSeries(663_298, "1-510"), filter.buildingSeries)
    }

    @Test
    fun shouldConvertSellRoomFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("SELL")),
                ParamsItemDto(CATEGORY, listOf("ROOMS")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_METER")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("15")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_FOOT")),
                ParamsItemDto(LIVING_AREA + RANGE_MIN_SUFFIX, listOf("11")),
                ParamsItemDto(ROOMS_COUNT, listOf("FOUR")),
                ParamsItemDto(CEILING_HEIGHT_MIN, listOf("4")),
                ParamsItemDto(RENOVATION, listOf("DESIGNER_RENOVATION")),
                ParamsItemDto(KITCHEN_AREA + RANGE_MAX_SUFFIX, listOf("19")),
                ParamsItemDto(BATHROOM, listOf("TWO_AND_MORE")),
                ParamsItemDto(FLOOR + RANGE_MIN_SUFFIX, listOf("2")),
                ParamsItemDto(LAST_FLOOR, listOf("NO")),
                ParamsItemDto(FLOOR_EXCEPT_FIRST, listOf("YES")),
                ParamsItemDto(BUILD_YEAR + RANGE_MAX_SUFFIX, listOf("2010")),
                ParamsItemDto(BUILDING_TYPE, listOf("MONOLIT_BRICK")),
                ParamsItemDto(BUILDING_EPOCH, listOf("KHRUSHCHEV")),
                ParamsItemDto(RANGE_MIN_PREFIX + FLOORS, listOf("13")),
                ParamsItemDto(RANGE_MAX_PREFIX + FLOORS, listOf("13")),
                ParamsItemDto(PARKING, listOf("OPEN")),
                ParamsItemDto(DEMOLITION, listOf("YES")),
                ParamsItemDto(PARK_TYPE, listOf("FOREST")),
                ParamsItemDto(POND_TYPE, listOf("SEA")),
                ParamsItemDto(EXPECT_METRO, listOf("YES")),
                ParamsItemDto(AGENTS, listOf("NO")),
                ParamsItemDto(WITH_PHOTO, listOf("YES")),
                ParamsItemDto(ONLINE_SHOW, listOf("YES")),
                ParamsItemDto(WITH_VIDEO, listOf("YES")),
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.SellRoom

        assertEquals(Filter.PriceType.PER_METER, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(15, Filter.MetroRemoteness.Unit.ON_FOOT),
            filter.metroRemoteness
        )
        assertEquals(Range.valueOf(11L, null), filter.livingArea)
        assertEquals(setOf(Filter.TotalRooms.FOUR), filter.totalRooms)
        assertEquals(4f, filter.ceilingHeightMin)
        assertEquals(setOf(Filter.Renovation.DESIGNER_RENOVATION), filter.renovation)
        assertEquals(Range.valueOf(null, 19L), filter.kitchenArea)
        assertEquals(Filter.Bathroom.TWO_AND_MORE, filter.bathroom)
        assertEquals(Range.valueOf(2L, null), filter.floor)
        assertEquals(false, filter.lastFloor)
        assertEquals(true, filter.exceptFirstFloor)
        assertEquals(Range.valueOf(null, 2010L), filter.builtYear)
        assertEquals(setOf(Filter.BuildingType.MONOLIT_BRICK), filter.buildingType)
        assertEquals(setOf(Filter.BuildingEpoch.KHRUSHCHEV), filter.buildingEpoch)
        assertEquals(Range.valueOf(13L, 13L), filter.floors)
        assertEquals(setOf(Filter.ParkingType.OPEN), filter.parkingType)
        assertEquals(true, filter.demolition)
        assertEquals(setOf(Filter.ParkType.FOREST), filter.parkType)
        assertEquals(setOf(Filter.PondType.SEA), filter.pondType)
        assertEquals(true, filter.expectMetro)
        assertEquals(false, filter.showFromAgents)
        assertEquals(true, filter.withPhoto)
        assertEquals(true, filter.onlineShow)
        assertEquals(true, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
    }

    @Test
    fun shouldConvertRentRoomFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("RENT")),
                ParamsItemDto(CATEGORY, listOf("ROOMS")),
                ParamsItemDto(RENT_TIME, listOf("SHORT")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_OFFER")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("30")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_FOOT")),
                ParamsItemDto(LIVING_AREA + RANGE_MIN_SUFFIX, listOf("11")),
                ParamsItemDto(ROOMS_COUNT, listOf("FIVE")),
                ParamsItemDto(CEILING_HEIGHT_MIN, listOf("2.7")),
                ParamsItemDto(RENOVATION, listOf("NEEDS_RENOVATION")),
                ParamsItemDto(FLOOR + RANGE_MIN_SUFFIX, listOf("2")),
                ParamsItemDto(LAST_FLOOR, listOf("YES")),
                ParamsItemDto(FLOOR_EXCEPT_FIRST, listOf("YES")),
                ParamsItemDto(FURNITURE, listOf("YES")),
                ParamsItemDto(REFRIGERATOR, listOf("YES")),
                ParamsItemDto(DISHWASHER, listOf("YES")),
                ParamsItemDto(AIRCONDITION, listOf("YES")),
                ParamsItemDto(TELEVISION, listOf("YES")),
                ParamsItemDto(WASHING_MACHINE, listOf("YES")),
                ParamsItemDto(CHILDREN, listOf("YES")),
                ParamsItemDto(PETS, listOf("YES")),
                ParamsItemDto(BUILD_YEAR + RANGE_MAX_SUFFIX, listOf("2010")),
                ParamsItemDto(BUILDING_TYPE, listOf("BLOCK")),
                ParamsItemDto(BUILDING_EPOCH, listOf("KHRUSHCHEV")),
                ParamsItemDto(RANGE_MIN_PREFIX + FLOORS, listOf("13")),
                ParamsItemDto(PARKING, listOf("OPEN")),
                ParamsItemDto(DEMOLITION, listOf("YES")),
                ParamsItemDto(PARK_TYPE, listOf("FOREST")),
                ParamsItemDto(POND_TYPE, listOf("SEA")),
                ParamsItemDto(EXPECT_METRO, listOf("YES")),
                ParamsItemDto(AGENTS, listOf("NO")),
                ParamsItemDto(HAS_FEE, listOf("NO")),
                ParamsItemDto(ONLINE_SHOW, listOf("YES")),
                ParamsItemDto(WITH_VIDEO, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.RentRoom

        assertEquals(Filter.RentTime.SHORT, filter.rentTime)
        assertEquals(Filter.PriceType.PER_OFFER, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(30, Filter.MetroRemoteness.Unit.ON_FOOT),
            filter.metroRemoteness
        )
        assertEquals(Range.valueOf(11L, null), filter.livingArea)
        assertEquals(setOf(Filter.TotalRooms.FIVE), filter.totalRooms)
        assertEquals(2.7f, filter.ceilingHeightMin)
        assertEquals(setOf(Filter.Renovation.NEEDS_RENOVATION), filter.renovation)
        assertEquals(Range.valueOf(2L, null), filter.floor)
        assertEquals(true, filter.lastFloor)
        assertEquals(true, filter.exceptFirstFloor)
        assertEquals(true, filter.withFurniture)
        assertEquals(true, filter.withRefrigerator)
        assertEquals(true, filter.withDishwasher)
        assertEquals(true, filter.withAircondition)
        assertEquals(true, filter.withTV)
        assertEquals(true, filter.withWashingMachine)
        assertEquals(true, filter.withChildren)
        assertEquals(true, filter.withPets)
        assertEquals(Range.valueOf(null, 2010L), filter.builtYear)
        assertEquals(setOf(Filter.BuildingType.BLOCK), filter.buildingType)
        assertEquals(setOf(Filter.BuildingEpoch.KHRUSHCHEV), filter.buildingEpoch)
        assertEquals(Range.valueOf(13L, null), filter.floors)
        assertEquals(setOf(Filter.ParkingType.OPEN), filter.parkingType)
        assertEquals(true, filter.demolition)
        assertEquals(setOf(Filter.ParkType.FOREST), filter.parkType)
        assertEquals(setOf(Filter.PondType.SEA), filter.pondType)
        assertEquals(true, filter.expectMetro)
        assertEquals(false, filter.showFromAgents)
        assertEquals(false, filter.hasFee)
        assertEquals(true, filter.onlineShow)
        assertEquals(true, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
    }

    @Test
    fun shouldConvertSellHouseFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("SELL")),
                ParamsItemDto(CATEGORY, listOf("HOUSE")),
                ParamsItemDto(PRIMARY_SALE, listOf("NO")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_METER")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("45")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_TRANSPORT")),

                ParamsItemDto(HOUSE_TYPE, listOf("TOWNHOUSE")),
                ParamsItemDto(RENOVATION, listOf("DESIGNER_RENOVATION")),
                ParamsItemDto(PROPERTY_AREA + RANGE_MAX_SUFFIX, listOf("19")),
                ParamsItemDto(LOT_AREA + RANGE_MIN_SUFFIX, listOf("19")),
                ParamsItemDto(BUILDING_TYPE, listOf("BRICK")),

                ParamsItemDto(ELECTRICITY_SUPPLY, listOf("YES")),
                ParamsItemDto(GAS_SUPPLY, listOf("YES")),
                ParamsItemDto(WATER_SUPPLY, listOf("YES")),
                ParamsItemDto(SEWERAGE_SUPPLY, listOf("YES")),
                ParamsItemDto(HEATING_SUPPLY, listOf("YES")),

                ParamsItemDto(PARK_TYPE, listOf("FOREST")),
                ParamsItemDto(POND_TYPE, listOf("SEA")),
                ParamsItemDto(EXPECT_METRO, listOf("YES")),

                ParamsItemDto(AGENTS, listOf("NO")),
                ParamsItemDto(WITH_PHOTO, listOf("YES")),
                ParamsItemDto(ONLINE_SHOW, listOf("YES")),
                ParamsItemDto(WITH_VIDEO, listOf("YES")),
                ParamsItemDto(RANGE_MIN_PREFIX + FLOORS, listOf("1")),
                ParamsItemDto(RANGE_MAX_PREFIX + FLOORS, listOf("7")),
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.SellHouse

        assertEquals(Filter.Market.SECONDARY, filter.market)
        assertEquals(Filter.PriceType.PER_METER, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(45, Filter.MetroRemoteness.Unit.ON_TRANSPORT),
            filter.metroRemoteness
        )
        assertEquals(setOf(Filter.HouseType.TOWNHOUSE), filter.houseType)
        assertEquals(setOf(Filter.Renovation.DESIGNER_RENOVATION), filter.renovation)
        assertEquals(setOf(Filter.HouseMaterialType.BRICK), filter.houseMaterialType)
        assertEquals(Range.valueOf(null, 19L), filter.houseArea)
        assertEquals(Range.valueOf(19L, null), filter.lotArea)
        assertEquals(true, filter.withElectricitySupply)
        assertEquals(true, filter.withGasSupply)
        assertEquals(true, filter.withWaterSupply)
        assertEquals(true, filter.withSewerageSupply)
        assertEquals(true, filter.withHeatingSupply)
        assertEquals(setOf(Filter.ParkType.FOREST), filter.parkType)
        assertEquals(setOf(Filter.PondType.SEA), filter.pondType)
        assertEquals(true, filter.expectMetro)
        assertEquals(false, filter.showFromAgents)
        assertEquals(true, filter.withPhoto)
        assertEquals(true, filter.onlineShow)
        assertEquals(true, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
        assertEquals(Range.valueOf(1L, 7L), filter.floors)
    }

    @Test
    fun shouldConvertVillageHouseNoTypesFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_LIST",
            params = listOf(
                ParamsItemDto(PRICE_TYPE, listOf("PER_OFFER")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(HOUSE_AREA + RANGE_MAX_SUFFIX, listOf("19")),
                ParamsItemDto(WALLS_TYPE, listOf("WOOD")),
                ParamsItemDto(LOT_AREA + RANGE_MIN_SUFFIX, listOf("19")),
                ParamsItemDto(ELECTRICITY_SUPPLY, listOf("YES")),
                ParamsItemDto(GAS_SUPPLY, listOf("YES")),
                ParamsItemDto(WATER_SUPPLY, listOf("YES")),
                ParamsItemDto(SEWERAGE_SUPPLY, listOf("YES")),
                ParamsItemDto(HEATING_SUPPLY, listOf("YES")),
                ParamsItemDto(COMMISSIONING_DATE, listOf("4_${QuarterOfYear.now.year}")),
                ParamsItemDto(VILLAGE_CLASS, listOf("ECONOMY")),
                ParamsItemDto(LAND_TYPE, listOf("DNP")),
                ParamsItemDto(PARK_TYPE, listOf("FOREST")),
                ParamsItemDto(POND_TYPE, listOf("SEA")),
                ParamsItemDto(HAS_RAILWAY_STATION, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    DEVELOPER,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("name", "developer")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as VillageListDeepLinkContext

        val filter = context.filter

        assertNull(filter.villageType)
        assertEquals(Filter.PriceType.PER_OFFER, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(Range.valueOf(null, 19L), filter.houseArea)
        assertEquals(setOf(Filter.WallType.WOOD), filter.wallsType)
        assertEquals(Range.valueOf(19L, null), filter.lotArea)
        assertEquals(true, filter.withElectricitySupply)
        assertEquals(true, filter.withGasSupply)
        assertEquals(true, filter.withWaterSupply)
        assertEquals(true, filter.withSewerageSupply)
        assertEquals(true, filter.withHeatingSupply)
        assertEquals(
            CommissioningState.BeingBuilt(QuarterOfYear(QuarterOfYear.now.year, Quarter.IV)),
            filter.commissioningState
        )
        assertEquals(setOf(Filter.VillageClass.ECONOMY), filter.villageClass)
        assertEquals(Filter.Developer(1, "developer"), filter.developer)
        assertEquals(setOf(Filter.LandType.DNP), filter.landType)
        assertEquals(setOf(Filter.ParkType.FOREST), filter.parkType)
        assertEquals(setOf(Filter.PondType.SEA), filter.pondType)
        assertEquals(true, filter.hasRailwayStation)
    }

    @Test
    fun shouldConvertVillageHouseLandTypeFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_LIST",
            params = listOf(
                ParamsItemDto(VILLAGE_OFFER_TYPE, listOf("LAND")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_ARE")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(HOUSE_AREA + RANGE_MAX_SUFFIX, listOf("19")),
                ParamsItemDto(WALLS_TYPE, listOf("WOOD")),
                ParamsItemDto(LOT_AREA + RANGE_MIN_SUFFIX, listOf("19")),
                ParamsItemDto(ELECTRICITY_SUPPLY, listOf("YES")),
                ParamsItemDto(GAS_SUPPLY, listOf("YES")),
                ParamsItemDto(WATER_SUPPLY, listOf("YES")),
                ParamsItemDto(SEWERAGE_SUPPLY, listOf("YES")),
                ParamsItemDto(HEATING_SUPPLY, listOf("YES")),
                ParamsItemDto(COMMISSIONING_DATE, listOf("4_${QuarterOfYear.now.year}")),
                ParamsItemDto(VILLAGE_CLASS, listOf("ECONOMY")),
                ParamsItemDto(LAND_TYPE, listOf("DNP")),
                ParamsItemDto(PARK_TYPE, listOf("FOREST")),
                ParamsItemDto(POND_TYPE, listOf("SEA")),
                ParamsItemDto(HAS_RAILWAY_STATION, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    DEVELOPER,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("name", "developer")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as VillageListDeepLinkContext

        val filter = context.filter

        assertEquals(setOf(Filter.VillageType.LAND), filter.villageType)
        assertEquals(Filter.PriceType.PER_ARE, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertNull(filter.houseArea)
        assertNull(filter.wallsType)
        assertEquals(Range.valueOf(19L, null), filter.lotArea)
        assertEquals(true, filter.withElectricitySupply)
        assertEquals(true, filter.withGasSupply)
        assertEquals(true, filter.withWaterSupply)
        assertEquals(true, filter.withSewerageSupply)
        assertEquals(true, filter.withHeatingSupply)
        assertEquals(
            CommissioningState.BeingBuilt(QuarterOfYear(QuarterOfYear.now.year, Quarter.IV)),
            filter.commissioningState
        )
        assertEquals(setOf(Filter.VillageClass.ECONOMY), filter.villageClass)
        assertEquals(Filter.Developer(1, "developer"), filter.developer)
        assertEquals(setOf(Filter.LandType.DNP), filter.landType)
        assertEquals(setOf(Filter.ParkType.FOREST), filter.parkType)
        assertEquals(setOf(Filter.PondType.SEA), filter.pondType)
        assertEquals(true, filter.hasRailwayStation)
    }

    @Test
    fun shouldConvertVillageHouseTownhouseTypeFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_LIST",
            params = listOf(
                ParamsItemDto(VILLAGE_OFFER_TYPE, listOf("TOWNHOUSE")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_METER")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(HOUSE_AREA + RANGE_MAX_SUFFIX, listOf("19")),
                ParamsItemDto(WALLS_TYPE, listOf("WOOD")),
                ParamsItemDto(LOT_AREA + RANGE_MIN_SUFFIX, listOf("19")),
                ParamsItemDto(ELECTRICITY_SUPPLY, listOf("YES")),
                ParamsItemDto(GAS_SUPPLY, listOf("YES")),
                ParamsItemDto(WATER_SUPPLY, listOf("YES")),
                ParamsItemDto(SEWERAGE_SUPPLY, listOf("YES")),
                ParamsItemDto(HEATING_SUPPLY, listOf("YES")),
                ParamsItemDto(COMMISSIONING_DATE, listOf("4_${QuarterOfYear.now.year}")),
                ParamsItemDto(VILLAGE_CLASS, listOf("ECONOMY")),
                ParamsItemDto(LAND_TYPE, listOf("DNP")),
                ParamsItemDto(PARK_TYPE, listOf("FOREST")),
                ParamsItemDto(POND_TYPE, listOf("SEA")),
                ParamsItemDto(HAS_RAILWAY_STATION, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    DEVELOPER,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("name", "developer")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as VillageListDeepLinkContext

        val filter = context.filter

        assertEquals(setOf(Filter.VillageType.TOWNHOUSE), filter.villageType)
        assertEquals(Filter.PriceType.PER_METER, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(Range.valueOf(null, 19L), filter.houseArea)
        assertEquals(setOf(Filter.WallType.WOOD), filter.wallsType)
        assertEquals(Range.valueOf(19L, null), filter.lotArea)
        assertNull(filter.withElectricitySupply)
        assertNull(filter.withGasSupply)
        assertNull(filter.withWaterSupply)
        assertNull(filter.withSewerageSupply)
        assertNull(filter.withHeatingSupply)
        assertEquals(
            CommissioningState.BeingBuilt(QuarterOfYear(QuarterOfYear.now.year, Quarter.IV)),
            filter.commissioningState
        )
        assertEquals(setOf(Filter.VillageClass.ECONOMY), filter.villageClass)
        assertEquals(Filter.Developer(1, "developer"), filter.developer)
        assertEquals(setOf(Filter.LandType.DNP), filter.landType)
        assertEquals(setOf(Filter.ParkType.FOREST), filter.parkType)
        assertEquals(setOf(Filter.PondType.SEA), filter.pondType)
        assertEquals(true, filter.hasRailwayStation)
    }

    @Test
    fun shouldConvertVillageHouseCottageTypeFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_LIST",
            params = listOf(
                ParamsItemDto(VILLAGE_OFFER_TYPE, listOf("COTTAGE")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_METER")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(HOUSE_AREA + RANGE_MAX_SUFFIX, listOf("19")),
                ParamsItemDto(WALLS_TYPE, listOf("WOOD")),
                ParamsItemDto(LOT_AREA + RANGE_MIN_SUFFIX, listOf("19")),
                ParamsItemDto(ELECTRICITY_SUPPLY, listOf("YES")),
                ParamsItemDto(GAS_SUPPLY, listOf("YES")),
                ParamsItemDto(WATER_SUPPLY, listOf("YES")),
                ParamsItemDto(SEWERAGE_SUPPLY, listOf("YES")),
                ParamsItemDto(HEATING_SUPPLY, listOf("YES")),
                ParamsItemDto(COMMISSIONING_DATE, listOf("4_${QuarterOfYear.now.year}")),
                ParamsItemDto(VILLAGE_CLASS, listOf("ECONOMY")),
                ParamsItemDto(LAND_TYPE, listOf("DNP")),
                ParamsItemDto(PARK_TYPE, listOf("FOREST")),
                ParamsItemDto(POND_TYPE, listOf("SEA")),
                ParamsItemDto(HAS_RAILWAY_STATION, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    DEVELOPER,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("name", "developer")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as VillageListDeepLinkContext

        val filter = context.filter

        assertEquals(setOf(Filter.VillageType.COTTAGE), filter.villageType)
        assertEquals(Filter.PriceType.PER_METER, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(Range.valueOf(null, 19L), filter.houseArea)
        assertEquals(setOf(Filter.WallType.WOOD), filter.wallsType)
        assertEquals(Range.valueOf(19L, null), filter.lotArea)
        assertEquals(true, filter.withElectricitySupply)
        assertEquals(true, filter.withGasSupply)
        assertEquals(true, filter.withWaterSupply)
        assertEquals(true, filter.withSewerageSupply)
        assertEquals(true, filter.withHeatingSupply)
        assertEquals(
            CommissioningState.BeingBuilt(QuarterOfYear(QuarterOfYear.now.year, Quarter.IV)),
            filter.commissioningState
        )
        assertEquals(setOf(Filter.VillageClass.ECONOMY), filter.villageClass)
        assertEquals(Filter.Developer(1, "developer"), filter.developer)
        assertEquals(setOf(Filter.LandType.DNP), filter.landType)
        assertEquals(setOf(Filter.ParkType.FOREST), filter.parkType)
        assertEquals(setOf(Filter.PondType.SEA), filter.pondType)
        assertEquals(true, filter.hasRailwayStation)
    }

    @Test
    fun shouldConvertVillageHouseAllTypesFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_LIST",
            params = listOf(
                ParamsItemDto(VILLAGE_OFFER_TYPE, listOf("COTTAGE", "TOWNHOUSE", "LAND")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_METER")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(HOUSE_AREA + RANGE_MAX_SUFFIX, listOf("19")),
                ParamsItemDto(WALLS_TYPE, listOf("WOOD")),
                ParamsItemDto(LOT_AREA + RANGE_MIN_SUFFIX, listOf("19")),
                ParamsItemDto(ELECTRICITY_SUPPLY, listOf("YES")),
                ParamsItemDto(GAS_SUPPLY, listOf("YES")),
                ParamsItemDto(WATER_SUPPLY, listOf("YES")),
                ParamsItemDto(SEWERAGE_SUPPLY, listOf("YES")),
                ParamsItemDto(HEATING_SUPPLY, listOf("YES")),
                ParamsItemDto(COMMISSIONING_DATE, listOf("4_${QuarterOfYear.now.year}")),
                ParamsItemDto(VILLAGE_CLASS, listOf("ECONOMY")),
                ParamsItemDto(LAND_TYPE, listOf("DNP")),
                ParamsItemDto(PARK_TYPE, listOf("FOREST")),
                ParamsItemDto(POND_TYPE, listOf("SEA")),
                ParamsItemDto(HAS_RAILWAY_STATION, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    DEVELOPER,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("name", "developer")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as VillageListDeepLinkContext

        val filter = context.filter

        assertEquals(
            setOf(
                Filter.VillageType.TOWNHOUSE,
                Filter.VillageType.COTTAGE,
                Filter.VillageType.LAND
            ),
            filter.villageType
        )
        assertEquals(Filter.PriceType.PER_OFFER, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(Range.valueOf(null, 19L), filter.houseArea)
        assertEquals(setOf(Filter.WallType.WOOD), filter.wallsType)
        assertEquals(Range.valueOf(19L, null), filter.lotArea)
        assertEquals(true, filter.withElectricitySupply)
        assertEquals(true, filter.withGasSupply)
        assertEquals(true, filter.withWaterSupply)
        assertEquals(true, filter.withSewerageSupply)
        assertEquals(true, filter.withHeatingSupply)
        assertEquals(
            CommissioningState.BeingBuilt(QuarterOfYear(QuarterOfYear.now.year, Quarter.IV)),
            filter.commissioningState
        )
        assertEquals(setOf(Filter.VillageClass.ECONOMY), filter.villageClass)
        assertEquals(Filter.Developer(1, "developer"), filter.developer)
        assertEquals(setOf(Filter.LandType.DNP), filter.landType)
        assertEquals(setOf(Filter.ParkType.FOREST), filter.parkType)
        assertEquals(setOf(Filter.PondType.SEA), filter.pondType)
        assertEquals(true, filter.hasRailwayStation)
    }

    @Test
    fun shouldConvertRentHouseFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("RENT")),
                ParamsItemDto(CATEGORY, listOf("HOUSE")),
                ParamsItemDto(RENT_TIME, listOf("SHORT")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_OFFER")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("60")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_TRANSPORT")),

                ParamsItemDto(HOUSE_TYPE, listOf("DUPLEX")),
                ParamsItemDto(RENOVATION, listOf("DESIGNER_RENOVATION")),
                ParamsItemDto(PROPERTY_AREA + RANGE_MAX_SUFFIX, listOf("19")),
                ParamsItemDto(LOT_AREA + RANGE_MIN_SUFFIX, listOf("19")),
                ParamsItemDto(BUILDING_TYPE, listOf("FERROCONCRETE")),

                ParamsItemDto(PARK_TYPE, listOf("FOREST")),
                ParamsItemDto(POND_TYPE, listOf("SEA")),
                ParamsItemDto(EXPECT_METRO, listOf("YES")),

                ParamsItemDto(AGENTS, listOf("NO")),
                ParamsItemDto(HAS_FEE, listOf("NO")),

                ParamsItemDto(ONLINE_SHOW, listOf("NO")),
                ParamsItemDto(WITH_VIDEO, listOf("NO")),
                ParamsItemDto(RANGE_MIN_PREFIX + FLOORS, listOf("2")),
                ParamsItemDto(RANGE_MAX_PREFIX + FLOORS, listOf("5")),
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.RentHouse

        assertEquals(Filter.RentTime.SHORT, filter.rentTime)
        assertEquals(Filter.PriceType.PER_OFFER, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(60, Filter.MetroRemoteness.Unit.ON_TRANSPORT),
            filter.metroRemoteness
        )
        assertEquals(setOf(Filter.HouseType.DUPLEX), filter.houseType)
        assertEquals(setOf(Filter.Renovation.DESIGNER_RENOVATION), filter.renovation)
        assertEquals(setOf(Filter.HouseMaterialType.FERROCONCRETE), filter.houseMaterialType)
        assertEquals(Range.valueOf(null, 19L), filter.houseArea)
        assertEquals(Range.valueOf(19L, null), filter.lotArea)
        assertEquals(setOf(Filter.ParkType.FOREST), filter.parkType)
        assertEquals(setOf(Filter.PondType.SEA), filter.pondType)
        assertEquals(true, filter.expectMetro)
        assertEquals(false, filter.showFromAgents)
        assertEquals(false, filter.hasFee)
        assertEquals(false, filter.onlineShow)
        assertEquals(false, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
        assertEquals(Range.valueOf(2L, 5L), filter.floors)
    }

    @Test
    fun shouldConvertSellLotFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("SELL")),
                ParamsItemDto(CATEGORY, listOf("LOT")),
                ParamsItemDto(PRIMARY_SALE, listOf("NO")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_ARE")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("45")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_TRANSPORT")),
                ParamsItemDto(LOT_AREA + RANGE_MIN_SUFFIX, listOf("19")),
                ParamsItemDto(LOT_TYPE, listOf("IGS", "GARDEN")),
                ParamsItemDto(PARK_TYPE, listOf("FOREST")),
                ParamsItemDto(POND_TYPE, listOf("SEA")),
                ParamsItemDto(EXPECT_METRO, listOf("YES")),
                ParamsItemDto(AGENTS, listOf("NO")),
                ParamsItemDto(WITH_PHOTO, listOf("YES")),
                ParamsItemDto(ONLINE_SHOW, listOf("YES")),
                ParamsItemDto(WITH_VIDEO, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.SellLot

        assertEquals(Filter.Market.SECONDARY, filter.market)
        assertEquals(Filter.PriceType.PER_ARE, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(45, Filter.MetroRemoteness.Unit.ON_TRANSPORT),
            filter.metroRemoteness
        )
        assertEquals(Range.valueOf(19L, null), filter.lotArea)
        assertEquals(setOf(Filter.LotType.IGS, Filter.LotType.GARDEN), filter.lotType)
        assertEquals(setOf(Filter.ParkType.FOREST), filter.parkType)
        assertEquals(setOf(Filter.PondType.SEA), filter.pondType)
        assertEquals(true, filter.expectMetro)
        assertEquals(false, filter.showFromAgents)
        assertEquals(true, filter.withPhoto)
        assertEquals(true, filter.onlineShow)
        assertEquals(true, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
    }

    @Test
    fun shouldConvertSellCommercialNoTypesFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("SELL")),
                ParamsItemDto(CATEGORY, listOf("COMMERCIAL")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_METER")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("60")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_FOOT")),
                ParamsItemDto(PROPERTY_AREA + RANGE_MIN_SUFFIX, listOf("15")),
                ParamsItemDto(LOT_AREA + RANGE_MAX_SUFFIX, listOf("30")),
                ParamsItemDto(FURNITURE, listOf("YES")),
                ParamsItemDto(AIRCONDITION, listOf("YES")),
                ParamsItemDto(COMMERCIAL_BUILDING_TYPE, listOf("BUSINESS_CENTER")),
                ParamsItemDto(OFFICE_CLASS, listOf("A_PLUS")),
                ParamsItemDto(HAS_TWENTY_FOUR_SEVEN, listOf("YES")),
                ParamsItemDto(HAS_PARKING, listOf("YES")),
                ParamsItemDto(ENTRANCE_TYPE, listOf("SEPARATE")),
                ParamsItemDto(COMMERCIAL_PLAN_TYPE, listOf("OPEN_SPACE")),
                ParamsItemDto(HAS_VENTILATION, listOf("YES")),
                ParamsItemDto(WITH_PHOTO, listOf("YES")),
                ParamsItemDto(ONLINE_SHOW, listOf("YES")),
                ParamsItemDto(WITH_VIDEO, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.SellCommercial

        assertNull(filter.commercialType)
        assertEquals(Filter.PriceType.PER_METER, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(60, Filter.MetroRemoteness.Unit.ON_FOOT),
            filter.metroRemoteness
        )
        assertEquals(Range.valueOf(15L, null), filter.propertyArea)
        assertNull(filter.lotArea)
        assertNull(filter.withFurniture)
        assertNull(filter.withAircondition)
        assertEquals(setOf(Filter.CommercialBuildingType.BUSINESS_CENTER), filter.buildingType)
        assertEquals(setOf(Filter.BusinessCenterClass.A_PLUS), filter.businessCenterClass)
        assertNull(filter.hasTwentyFourSeven)
        assertNull(filter.hasParking)
        assertEquals(Filter.EntranceType.SEPARATE, filter.entranceType)
        assertNull(filter.commercialPlanType)
        assertNull(filter.hasVentilation)
        assertEquals(true, filter.withPhoto)
        assertEquals(true, filter.onlineShow)
        assertEquals(true, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
    }

    @Test
    fun shouldConvertSellCommercialLandTypeFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("SELL")),
                ParamsItemDto(CATEGORY, listOf("COMMERCIAL")),
                ParamsItemDto(COMMERCIAL_TYPE, listOf("LAND")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_ARE")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("60")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_FOOT")),
                ParamsItemDto(PROPERTY_AREA + RANGE_MIN_SUFFIX, listOf("15")),
                ParamsItemDto(LOT_AREA + RANGE_MAX_SUFFIX, listOf("30")),
                ParamsItemDto(FURNITURE, listOf("YES")),
                ParamsItemDto(AIRCONDITION, listOf("YES")),
                ParamsItemDto(COMMERCIAL_BUILDING_TYPE, listOf("BUSINESS_CENTER")),
                ParamsItemDto(OFFICE_CLASS, listOf("A_PLUS")),
                ParamsItemDto(HAS_TWENTY_FOUR_SEVEN, listOf("YES")),
                ParamsItemDto(HAS_PARKING, listOf("YES")),
                ParamsItemDto(ENTRANCE_TYPE, listOf("SEPARATE")),
                ParamsItemDto(COMMERCIAL_PLAN_TYPE, listOf("OPEN_SPACE")),
                ParamsItemDto(HAS_VENTILATION, listOf("YES")),
                ParamsItemDto(WITH_PHOTO, listOf("YES")),
                ParamsItemDto(ONLINE_SHOW, listOf("YES")),
                ParamsItemDto(WITH_VIDEO, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.SellCommercial

        assertEquals(setOf(Filter.CommercialType.LAND), filter.commercialType)
        assertEquals(Filter.PriceType.PER_ARE, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(60, Filter.MetroRemoteness.Unit.ON_FOOT),
            filter.metroRemoteness
        )
        assertNull(filter.propertyArea)
        assertEquals(Range.valueOf(null, 30L), filter.lotArea)
        assertNull(filter.withFurniture)
        assertNull(filter.withAircondition)
        assertNull(filter.buildingType)
        assertNull(filter.businessCenterClass)
        assertNull(filter.hasTwentyFourSeven)
        assertNull(filter.hasParking)
        assertNull(filter.entranceType)
        assertNull(filter.commercialPlanType)
        assertNull(filter.hasVentilation)
        assertEquals(true, filter.withPhoto)
        assertEquals(true, filter.onlineShow)
        assertEquals(true, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
    }

    @Test
    fun shouldConvertSellCommercialOfficeLikeTypesFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("SELL")),
                ParamsItemDto(CATEGORY, listOf("COMMERCIAL")),
                ParamsItemDto(
                    COMMERCIAL_TYPE,
                    listOf(
                        "OFFICE",
                        "RETAIL",
                        "FREE_PURPOSE",
                        "PUBLIC_CATERING",
                        "HOTEL"
                    )
                ),
                ParamsItemDto(PRICE_TYPE, listOf("PER_METER")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("60")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_FOOT")),
                ParamsItemDto(PROPERTY_AREA + RANGE_MIN_SUFFIX, listOf("15")),
                ParamsItemDto(LOT_AREA + RANGE_MAX_SUFFIX, listOf("30")),
                ParamsItemDto(FURNITURE, listOf("YES")),
                ParamsItemDto(AIRCONDITION, listOf("YES")),
                ParamsItemDto(COMMERCIAL_BUILDING_TYPE, listOf("BUSINESS_CENTER")),
                ParamsItemDto(OFFICE_CLASS, listOf("A_PLUS")),
                ParamsItemDto(HAS_TWENTY_FOUR_SEVEN, listOf("YES")),
                ParamsItemDto(HAS_PARKING, listOf("YES")),
                ParamsItemDto(ENTRANCE_TYPE, listOf("SEPARATE")),
                ParamsItemDto(COMMERCIAL_PLAN_TYPE, listOf("OPEN_SPACE")),
                ParamsItemDto(HAS_VENTILATION, listOf("YES")),
                ParamsItemDto(WITH_PHOTO, listOf("YES")),
                ParamsItemDto(ONLINE_SHOW, listOf("YES")),
                ParamsItemDto(WITH_VIDEO, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.SellCommercial

        assertEquals(
            setOf(
                Filter.CommercialType.OFFICE,
                Filter.CommercialType.RETAIL,
                Filter.CommercialType.FREE_PURPOSE,
                Filter.CommercialType.PUBLIC_CATERING,
                Filter.CommercialType.HOTEL
            ),
            filter.commercialType
        )
        assertEquals(Filter.PriceType.PER_METER, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(60, Filter.MetroRemoteness.Unit.ON_FOOT),
            filter.metroRemoteness
        )
        assertEquals(Range.valueOf(15L, null), filter.propertyArea)
        assertNull(filter.lotArea)
        assertEquals(true, filter.withFurniture)
        assertEquals(true, filter.withAircondition)
        assertEquals(setOf(Filter.CommercialBuildingType.BUSINESS_CENTER), filter.buildingType)
        assertEquals(setOf(Filter.BusinessCenterClass.A_PLUS), filter.businessCenterClass)
        assertEquals(true, filter.hasTwentyFourSeven)
        assertEquals(true, filter.hasParking)
        assertEquals(Filter.EntranceType.SEPARATE, filter.entranceType)
        assertEquals(Filter.CommercialPlanType.OPEN_SPACE, filter.commercialPlanType)
        assertEquals(true, filter.hasVentilation)
        assertEquals(true, filter.withPhoto)
        assertEquals(true, filter.onlineShow)
        assertEquals(true, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
    }

    @Test
    fun shouldConvertSellCommercialIndustrialLikeTypesFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("SELL")),
                ParamsItemDto(CATEGORY, listOf("COMMERCIAL")),
                ParamsItemDto(
                    COMMERCIAL_TYPE,
                    listOf(
                        "AUTO_REPAIR",
                        "WAREHOUSE",
                        "MANUFACTURING"
                    )
                ),
                ParamsItemDto(PRICE_TYPE, listOf("PER_METER")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("60")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_FOOT")),
                ParamsItemDto(PROPERTY_AREA + RANGE_MIN_SUFFIX, listOf("15")),
                ParamsItemDto(LOT_AREA + RANGE_MAX_SUFFIX, listOf("30")),
                ParamsItemDto(FURNITURE, listOf("YES")),
                ParamsItemDto(AIRCONDITION, listOf("YES")),
                ParamsItemDto(COMMERCIAL_BUILDING_TYPE, listOf("WAREHOUSE")),
                ParamsItemDto(OFFICE_CLASS, listOf("A_PLUS")),
                ParamsItemDto(HAS_TWENTY_FOUR_SEVEN, listOf("YES")),
                ParamsItemDto(HAS_PARKING, listOf("YES")),
                ParamsItemDto(ENTRANCE_TYPE, listOf("SEPARATE")),
                ParamsItemDto(COMMERCIAL_PLAN_TYPE, listOf("OPEN_SPACE")),
                ParamsItemDto(HAS_VENTILATION, listOf("YES")),
                ParamsItemDto(WITH_PHOTO, listOf("YES")),
                ParamsItemDto(ONLINE_SHOW, listOf("YES")),
                ParamsItemDto(WITH_VIDEO, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.SellCommercial

        assertEquals(
            setOf(
                Filter.CommercialType.AUTO_REPAIR,
                Filter.CommercialType.WAREHOUSE,
                Filter.CommercialType.MANUFACTURING
            ),
            filter.commercialType
        )
        assertEquals(Filter.PriceType.PER_METER, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(60, Filter.MetroRemoteness.Unit.ON_FOOT),
            filter.metroRemoteness
        )
        assertEquals(Range.valueOf(15L, null), filter.propertyArea)
        assertNull(filter.lotArea)
        assertNull(filter.withFurniture)
        assertEquals(true, filter.withAircondition)
        assertEquals(setOf(Filter.CommercialBuildingType.WAREHOUSE), filter.buildingType)
        assertNull(filter.businessCenterClass)
        assertEquals(true, filter.hasTwentyFourSeven)
        assertEquals(true, filter.hasParking)
        assertEquals(Filter.EntranceType.SEPARATE, filter.entranceType)
        assertNull(filter.commercialPlanType)
        assertEquals(true, filter.hasVentilation)
        assertEquals(true, filter.withPhoto)
        assertEquals(true, filter.onlineShow)
        assertEquals(true, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
    }

    @Test
    fun shouldConvertSellCommercialBusinessTypeFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("SELL")),
                ParamsItemDto(CATEGORY, listOf("COMMERCIAL")),
                ParamsItemDto(COMMERCIAL_TYPE, listOf("BUSINESS")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_METER")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("60")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_FOOT")),
                ParamsItemDto(PROPERTY_AREA + RANGE_MIN_SUFFIX, listOf("15")),
                ParamsItemDto(LOT_AREA + RANGE_MAX_SUFFIX, listOf("30")),
                ParamsItemDto(FURNITURE, listOf("YES")),
                ParamsItemDto(AIRCONDITION, listOf("YES")),
                ParamsItemDto(COMMERCIAL_BUILDING_TYPE, listOf("DETACHED_BUILDING")),
                ParamsItemDto(OFFICE_CLASS, listOf("A_PLUS")),
                ParamsItemDto(HAS_TWENTY_FOUR_SEVEN, listOf("YES")),
                ParamsItemDto(HAS_PARKING, listOf("YES")),
                ParamsItemDto(FLOOR + RANGE_MIN_SUFFIX, listOf("1")),
                ParamsItemDto(FLOOR + RANGE_MAX_SUFFIX, listOf("1")),
                ParamsItemDto(ENTRANCE_TYPE, listOf("SEPARATE")),
                ParamsItemDto(COMMERCIAL_PLAN_TYPE, listOf("OPEN_SPACE")),
                ParamsItemDto(HAS_VENTILATION, listOf("YES")),
                ParamsItemDto(WITH_PHOTO, listOf("YES")),
                ParamsItemDto(ONLINE_SHOW, listOf("YES")),
                ParamsItemDto(WITH_VIDEO, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.SellCommercial

        assertEquals(setOf(Filter.CommercialType.BUSINESS), filter.commercialType)
        assertEquals(Filter.PriceType.PER_METER, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(60, Filter.MetroRemoteness.Unit.ON_FOOT),
            filter.metroRemoteness
        )
        assertEquals(Range.valueOf(15L, null), filter.propertyArea)
        assertNull(filter.lotArea)
        assertNull(filter.withFurniture)
        assertNull(filter.withAircondition)
        assertEquals(setOf(Filter.CommercialBuildingType.DETACHED_BUILDING), filter.buildingType)
        assertNull(filter.businessCenterClass)
        assertNull(filter.hasTwentyFourSeven)
        assertNull(filter.hasParking)
        assertNull(filter.aboveFirstFloor)
        assertEquals(Filter.EntranceType.SEPARATE, filter.entranceType)
        assertNull(filter.commercialPlanType)
        assertNull(filter.hasVentilation)
        assertEquals(true, filter.withPhoto)
        assertEquals(true, filter.onlineShow)
        assertEquals(true, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
    }

    @Test
    fun shouldConvertSellCommercialLegalAddressTypeFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("SELL")),
                ParamsItemDto(CATEGORY, listOf("COMMERCIAL")),
                ParamsItemDto(COMMERCIAL_TYPE, listOf("LEGAL_ADDRESS")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_METER")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("60")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_FOOT")),
                ParamsItemDto(PROPERTY_AREA + RANGE_MIN_SUFFIX, listOf("15")),
                ParamsItemDto(LOT_AREA + RANGE_MAX_SUFFIX, listOf("30")),
                ParamsItemDto(FURNITURE, listOf("YES")),
                ParamsItemDto(AIRCONDITION, listOf("YES")),
                ParamsItemDto(COMMERCIAL_BUILDING_TYPE, listOf("BUSINESS_CENTER")),
                ParamsItemDto(OFFICE_CLASS, listOf("A_PLUS")),
                ParamsItemDto(HAS_TWENTY_FOUR_SEVEN, listOf("YES")),
                ParamsItemDto(HAS_PARKING, listOf("YES")),
                ParamsItemDto(ENTRANCE_TYPE, listOf("SEPARATE")),
                ParamsItemDto(COMMERCIAL_PLAN_TYPE, listOf("OPEN_SPACE")),
                ParamsItemDto(HAS_VENTILATION, listOf("YES")),
                ParamsItemDto(WITH_PHOTO, listOf("YES")),
                ParamsItemDto(ONLINE_SHOW, listOf("YES")),
                ParamsItemDto(WITH_VIDEO, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.SellCommercial

        assertNull(filter.commercialType)
        assertEquals(Filter.PriceType.PER_METER, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(60, Filter.MetroRemoteness.Unit.ON_FOOT),
            filter.metroRemoteness
        )
        assertEquals(Range.valueOf(15L, null), filter.propertyArea)
        assertNull(filter.lotArea)
        assertNull(filter.withFurniture)
        assertNull(filter.withAircondition)
        assertEquals(setOf(Filter.CommercialBuildingType.BUSINESS_CENTER), filter.buildingType)
        assertEquals(setOf(Filter.BusinessCenterClass.A_PLUS), filter.businessCenterClass)
        assertNull(null)
        assertNull(filter.hasParking)
        assertEquals(Filter.EntranceType.SEPARATE, filter.entranceType)
        assertNull(filter.commercialPlanType)
        assertNull(null)
        assertEquals(true, filter.withPhoto)
        assertEquals(true, filter.onlineShow)
        assertEquals(true, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
    }

    @Test
    fun shouldConvertRentCommercialNoTypesFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("RENT")),
                ParamsItemDto(CATEGORY, listOf("COMMERCIAL")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_METER")),
                ParamsItemDto(PRICING_PERIOD, listOf("PER_YEAR")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("60")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_FOOT")),
                ParamsItemDto(DEAL_STATUS, listOf("DIRECT_RENT")),
                ParamsItemDto(COMMISSION + RANGE_MIN_SUFFIX, listOf("1")),
                ParamsItemDto(TAXATION_FORM, listOf("NDS", "USN")),
                ParamsItemDto(CLEANING_INCLUDED, listOf("YES")),
                ParamsItemDto(UTILITIES_INCLUDED, listOf("YES")),
                ParamsItemDto(ELECTRICITY_INCLUDED, listOf("YES")),
                ParamsItemDto(PROPERTY_AREA + RANGE_MIN_SUFFIX, listOf("15")),
                ParamsItemDto(LOT_AREA + RANGE_MAX_SUFFIX, listOf("30")),
                ParamsItemDto(FURNITURE, listOf("YES")),
                ParamsItemDto(AIRCONDITION, listOf("YES")),
                ParamsItemDto(COMMERCIAL_BUILDING_TYPE, listOf("BUSINESS_CENTER")),
                ParamsItemDto(OFFICE_CLASS, listOf("A_PLUS")),
                ParamsItemDto(HAS_TWENTY_FOUR_SEVEN, listOf("YES")),
                ParamsItemDto(HAS_PARKING, listOf("YES")),
                ParamsItemDto(FLOOR + RANGE_MIN_SUFFIX, listOf("1")),
                ParamsItemDto(FLOOR + RANGE_MAX_SUFFIX, listOf("1")),
                ParamsItemDto(ENTRANCE_TYPE, listOf("SEPARATE")),
                ParamsItemDto(COMMERCIAL_PLAN_TYPE, listOf("OPEN_SPACE")),
                ParamsItemDto(HAS_VENTILATION, listOf("YES")),
                ParamsItemDto(WITH_PHOTO, listOf("YES")),
                ParamsItemDto(ONLINE_SHOW, listOf("YES")),
                ParamsItemDto(WITH_VIDEO, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.RentCommercial

        assertNull(filter.commercialType)
        assertEquals(Filter.PriceType.PER_METER, filter.priceType)
        assertEquals(Filter.PricingPeriod.PER_YEAR, filter.pricingPeriod)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(60, Filter.MetroRemoteness.Unit.ON_FOOT),
            filter.metroRemoteness
        )
        assertEquals(Filter.TransactionType.DIRECT_RENT, filter.transactionType)
        assertEquals(true, filter.withCommission)
        assertEquals(true, filter.nds)
        assertEquals(true, filter.usn)
        assertNull(filter.cleaningIncluded)
        assertNull(filter.utilitiesIncluded)
        assertNull(filter.electricityIncluded)
        assertEquals(Range.valueOf(15L, null), filter.propertyArea)
        assertNull(filter.lotArea)
        assertNull(filter.withFurniture)
        assertNull(filter.withAircondition)
        assertEquals(setOf(Filter.CommercialBuildingType.BUSINESS_CENTER), filter.buildingType)
        assertEquals(setOf(Filter.BusinessCenterClass.A_PLUS), filter.businessCenterClass)
        assertNull(filter.hasTwentyFourSeven)
        assertNull(filter.hasParking)
        assertEquals(false, filter.aboveFirstFloor)
        assertEquals(Filter.EntranceType.SEPARATE, filter.entranceType)
        assertNull(filter.commercialPlanType)
        assertNull(filter.hasVentilation)
        assertEquals(true, filter.withPhoto)
        assertEquals(true, filter.onlineShow)
        assertEquals(true, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
    }

    @Test
    fun shouldConvertRentCommercialLandTypeFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("RENT")),
                ParamsItemDto(CATEGORY, listOf("COMMERCIAL")),
                ParamsItemDto(COMMERCIAL_TYPE, listOf("LAND")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_ARE")),
                ParamsItemDto(PRICING_PERIOD, listOf("PER_MONTH")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("60")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_FOOT")),
                ParamsItemDto(DEAL_STATUS, listOf("SUBRENT")),
                ParamsItemDto(TAXATION_FORM, listOf("NDS", "USN")),
                ParamsItemDto(COMMISSION + RANGE_MAX_SUFFIX, listOf("0")),
                ParamsItemDto(CLEANING_INCLUDED, listOf("YES")),
                ParamsItemDto(UTILITIES_INCLUDED, listOf("YES")),
                ParamsItemDto(ELECTRICITY_INCLUDED, listOf("YES")),
                ParamsItemDto(PROPERTY_AREA + RANGE_MIN_SUFFIX, listOf("15")),
                ParamsItemDto(LOT_AREA + RANGE_MAX_SUFFIX, listOf("30")),
                ParamsItemDto(FURNITURE, listOf("YES")),
                ParamsItemDto(AIRCONDITION, listOf("YES")),
                ParamsItemDto(COMMERCIAL_BUILDING_TYPE, listOf("BUSINESS_CENTER")),
                ParamsItemDto(OFFICE_CLASS, listOf("A_PLUS")),
                ParamsItemDto(HAS_TWENTY_FOUR_SEVEN, listOf("YES")),
                ParamsItemDto(HAS_PARKING, listOf("YES")),
                ParamsItemDto(FLOOR + RANGE_MIN_SUFFIX, listOf("2")),
                ParamsItemDto(ENTRANCE_TYPE, listOf("SEPARATE")),
                ParamsItemDto(COMMERCIAL_PLAN_TYPE, listOf("OPEN_SPACE")),
                ParamsItemDto(HAS_VENTILATION, listOf("YES")),
                ParamsItemDto(WITH_PHOTO, listOf("YES")),
                ParamsItemDto(ONLINE_SHOW, listOf("YES")),
                ParamsItemDto(WITH_VIDEO, listOf("YES")),
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.RentCommercial

        assertEquals(setOf(Filter.CommercialType.LAND), filter.commercialType)
        assertEquals(Filter.PriceType.PER_ARE, filter.priceType)
        assertEquals(Filter.PricingPeriod.PER_MONTH, filter.pricingPeriod)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(60, Filter.MetroRemoteness.Unit.ON_FOOT),
            filter.metroRemoteness
        )
        assertEquals(Filter.TransactionType.SUBRENT, filter.transactionType)
        assertEquals(false, filter.withCommission)
        assertEquals(true, filter.nds)
        assertEquals(true, filter.usn)
        assertNull(filter.cleaningIncluded)
        assertNull(filter.utilitiesIncluded)
        assertNull(filter.electricityIncluded)
        assertNull(filter.propertyArea)
        assertEquals(Range.valueOf(null, 30L), filter.lotArea)
        assertNull(filter.withFurniture)
        assertNull(filter.withAircondition)
        assertNull(filter.buildingType)
        assertNull(filter.businessCenterClass)
        assertNull(filter.hasTwentyFourSeven)
        assertNull(filter.hasParking)
        assertNull(filter.aboveFirstFloor)
        assertNull(filter.entranceType)
        assertNull(filter.commercialPlanType)
        assertNull(filter.hasVentilation)
        assertEquals(true, filter.withPhoto)
        assertEquals(true, filter.onlineShow)
        assertEquals(true, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
    }

    @Test
    fun shouldConvertRentCommercialOfficeLikeTypesFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("RENT")),
                ParamsItemDto(CATEGORY, listOf("COMMERCIAL")),
                ParamsItemDto(
                    COMMERCIAL_TYPE,
                    listOf(
                        "OFFICE",
                        "RETAIL",
                        "FREE_PURPOSE",
                        "PUBLIC_CATERING",
                        "HOTEL"
                    )
                ),
                ParamsItemDto(PRICE_TYPE, listOf("PER_METER")),
                ParamsItemDto(PRICING_PERIOD, listOf("PER_MONTH")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("60")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_FOOT")),
                ParamsItemDto(DEAL_STATUS, listOf("SALE_OF_LEASE_RIGHTS")),
                ParamsItemDto(TAXATION_FORM, listOf("NDS", "USN")),
                ParamsItemDto(COMMISSION + RANGE_MAX_SUFFIX, listOf("0")),
                ParamsItemDto(CLEANING_INCLUDED, listOf("YES")),
                ParamsItemDto(UTILITIES_INCLUDED, listOf("YES")),
                ParamsItemDto(ELECTRICITY_INCLUDED, listOf("YES")),
                ParamsItemDto(PROPERTY_AREA + RANGE_MIN_SUFFIX, listOf("15")),
                ParamsItemDto(LOT_AREA + RANGE_MAX_SUFFIX, listOf("30")),
                ParamsItemDto(FURNITURE, listOf("YES")),
                ParamsItemDto(AIRCONDITION, listOf("YES")),
                ParamsItemDto(COMMERCIAL_BUILDING_TYPE, listOf("BUSINESS_CENTER")),
                ParamsItemDto(OFFICE_CLASS, listOf("A_PLUS")),
                ParamsItemDto(HAS_TWENTY_FOUR_SEVEN, listOf("YES")),
                ParamsItemDto(HAS_PARKING, listOf("YES")),
                ParamsItemDto(FLOOR + RANGE_MIN_SUFFIX, listOf("3")),
                ParamsItemDto(FLOOR + RANGE_MAX_SUFFIX, listOf("10")),
                ParamsItemDto(ENTRANCE_TYPE, listOf("COMMON")),
                ParamsItemDto(
                    RENOVATION,
                    listOf(
                        "NEEDS_RENOVATION",
                        "COSMETIC_DONE",
                        "DESIGNER_RENOVATION"
                    )
                ),
                ParamsItemDto(COMMERCIAL_PLAN_TYPE, listOf("OPEN_SPACE")),
                ParamsItemDto(HAS_VENTILATION, listOf("YES")),
                ParamsItemDto(WITH_PHOTO, listOf("YES")),
                ParamsItemDto(ONLINE_SHOW, listOf("YES")),
                ParamsItemDto(WITH_VIDEO, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.RentCommercial

        assertEquals(
            setOf(
                Filter.CommercialType.OFFICE,
                Filter.CommercialType.RETAIL,
                Filter.CommercialType.FREE_PURPOSE,
                Filter.CommercialType.PUBLIC_CATERING,
                Filter.CommercialType.HOTEL
            ),
            filter.commercialType
        )
        assertEquals(Filter.PriceType.PER_METER, filter.priceType)
        assertEquals(Filter.PricingPeriod.PER_MONTH, filter.pricingPeriod)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(60, Filter.MetroRemoteness.Unit.ON_FOOT),
            filter.metroRemoteness
        )
        assertEquals(Filter.TransactionType.SALE_OF_LEASE_RIGHTS, filter.transactionType)
        assertEquals(false, filter.withCommission)
        assertEquals(true, filter.nds)
        assertEquals(true, filter.usn)
        assertEquals(true, filter.cleaningIncluded)
        assertEquals(true, filter.utilitiesIncluded)
        assertEquals(true, filter.electricityIncluded)
        assertEquals(Range.valueOf(15L, null), filter.propertyArea)
        assertNull(filter.lotArea)
        assertEquals(true, filter.withFurniture)
        assertEquals(true, filter.withAircondition)
        assertEquals(setOf(Filter.CommercialBuildingType.BUSINESS_CENTER), filter.buildingType)
        assertEquals(setOf(Filter.BusinessCenterClass.A_PLUS), filter.businessCenterClass)
        assertEquals(true, filter.hasTwentyFourSeven)
        assertEquals(true, filter.hasParking)
        assertNull(filter.aboveFirstFloor)
        assertEquals(Filter.EntranceType.COMMON, filter.entranceType)
        assertEquals(
            setOf(
                Filter.CommercialRenovation.NEEDS_RENOVATION,
                Filter.CommercialRenovation.COSMETIC_DONE,
                Filter.CommercialRenovation.DESIGNER_RENOVATION
            ),
            filter.renovation
        )
        assertEquals(Filter.CommercialPlanType.OPEN_SPACE, filter.commercialPlanType)
        assertEquals(true, filter.hasVentilation)
        assertEquals(true, filter.withPhoto)
        assertEquals(true, filter.onlineShow)
        assertEquals(true, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
    }

    @Test
    fun shouldConvertRentCommercialIndustrialLikeTypesFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("RENT")),
                ParamsItemDto(CATEGORY, listOf("COMMERCIAL")),
                ParamsItemDto(
                    COMMERCIAL_TYPE,
                    listOf(
                        "AUTO_REPAIR",
                        "WAREHOUSE",
                        "MANUFACTURING"
                    )
                ),
                ParamsItemDto(PRICE_TYPE, listOf("PER_METER")),
                ParamsItemDto(PRICING_PERIOD, listOf("PER_MONTH")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("60")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_FOOT")),
                ParamsItemDto(DEAL_STATUS, listOf("SALE_OF_LEASE_RIGHTS")),
                ParamsItemDto(COMMISSION + RANGE_MAX_SUFFIX, listOf("0")),
                ParamsItemDto(TAXATION_FORM, listOf("NDS", "USN")),
                ParamsItemDto(CLEANING_INCLUDED, listOf("YES")),
                ParamsItemDto(UTILITIES_INCLUDED, listOf("YES")),
                ParamsItemDto(ELECTRICITY_INCLUDED, listOf("YES")),
                ParamsItemDto(PROPERTY_AREA + RANGE_MIN_SUFFIX, listOf("15")),
                ParamsItemDto(LOT_AREA + RANGE_MAX_SUFFIX, listOf("30")),
                ParamsItemDto(FURNITURE, listOf("YES")),
                ParamsItemDto(AIRCONDITION, listOf("YES")),
                ParamsItemDto(COMMERCIAL_BUILDING_TYPE, listOf("SHOPPING_CENTER")),
                ParamsItemDto(OFFICE_CLASS, listOf("A_PLUS")),
                ParamsItemDto(HAS_TWENTY_FOUR_SEVEN, listOf("YES")),
                ParamsItemDto(HAS_PARKING, listOf("YES")),
                ParamsItemDto(ENTRANCE_TYPE, listOf("COMMON")),
                ParamsItemDto(
                    RENOVATION,
                    listOf(
                        "NEEDS_RENOVATION",
                        "COSMETIC_DONE",
                        "DESIGNER_RENOVATION"
                    )
                ),
                ParamsItemDto(COMMERCIAL_PLAN_TYPE, listOf("OPEN_SPACE")),
                ParamsItemDto(HAS_VENTILATION, listOf("YES")),
                ParamsItemDto(WITH_PHOTO, listOf("YES")),
                ParamsItemDto(ONLINE_SHOW, listOf("YES")),
                ParamsItemDto(WITH_VIDEO, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.RentCommercial

        assertEquals(
            setOf(
                Filter.CommercialType.AUTO_REPAIR,
                Filter.CommercialType.WAREHOUSE,
                Filter.CommercialType.MANUFACTURING
            ),
            filter.commercialType
        )
        assertEquals(Filter.PriceType.PER_METER, filter.priceType)
        assertEquals(Filter.PricingPeriod.PER_MONTH, filter.pricingPeriod)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(60, Filter.MetroRemoteness.Unit.ON_FOOT),
            filter.metroRemoteness
        )
        assertEquals(Filter.TransactionType.SALE_OF_LEASE_RIGHTS, filter.transactionType)
        assertEquals(false, filter.withCommission)
        assertEquals(true, filter.nds)
        assertEquals(true, filter.usn)
        assertEquals(true, filter.cleaningIncluded)
        assertEquals(true, filter.utilitiesIncluded)
        assertEquals(true, filter.electricityIncluded)
        assertEquals(Range.valueOf(15L, null), filter.propertyArea)
        assertNull(filter.lotArea)
        assertNull(filter.withFurniture)
        assertEquals(true, filter.withAircondition)
        assertEquals(setOf(Filter.CommercialBuildingType.SHOPPING_CENTER), filter.buildingType)
        assertEquals(true, filter.hasParking)
        assertNull(filter.businessCenterClass)
        assertEquals(true, filter.hasTwentyFourSeven)
        assertEquals(Filter.EntranceType.COMMON, filter.entranceType)
        assertNull(filter.renovation)
        assertNull(filter.commercialPlanType)
        assertEquals(true, filter.hasVentilation)
        assertEquals(true, filter.withPhoto)
        assertEquals(true, filter.onlineShow)
        assertEquals(true, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
    }

    @Test
    fun shouldConvertRentCommercialBusinessTypeFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("RENT")),
                ParamsItemDto(CATEGORY, listOf("COMMERCIAL")),
                ParamsItemDto(COMMERCIAL_TYPE, listOf("BUSINESS")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_METER")),
                ParamsItemDto(PRICING_PERIOD, listOf("PER_MONTH")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("60")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_FOOT")),
                ParamsItemDto(DEAL_STATUS, listOf("SALE_OF_LEASE_RIGHTS")),
                ParamsItemDto(COMMISSION + RANGE_MAX_SUFFIX, listOf("0")),
                ParamsItemDto(TAXATION_FORM, listOf("NDS", "USN")),
                ParamsItemDto(CLEANING_INCLUDED, listOf("YES")),
                ParamsItemDto(UTILITIES_INCLUDED, listOf("YES")),
                ParamsItemDto(ELECTRICITY_INCLUDED, listOf("YES")),
                ParamsItemDto(PROPERTY_AREA + RANGE_MIN_SUFFIX, listOf("15")),
                ParamsItemDto(LOT_AREA + RANGE_MAX_SUFFIX, listOf("30")),
                ParamsItemDto(FURNITURE, listOf("YES")),
                ParamsItemDto(AIRCONDITION, listOf("YES")),
                ParamsItemDto(COMMERCIAL_BUILDING_TYPE, listOf("RESIDENTIAL_BUILDING")),
                ParamsItemDto(OFFICE_CLASS, listOf("A_PLUS")),
                ParamsItemDto(HAS_TWENTY_FOUR_SEVEN, listOf("YES")),
                ParamsItemDto(HAS_PARKING, listOf("YES")),
                ParamsItemDto(ENTRANCE_TYPE, listOf("COMMON")),
                ParamsItemDto(COMMERCIAL_PLAN_TYPE, listOf("OPEN_SPACE")),
                ParamsItemDto(HAS_VENTILATION, listOf("YES")),
                ParamsItemDto(WITH_PHOTO, listOf("YES")),
                ParamsItemDto(ONLINE_SHOW, listOf("YES")),
                ParamsItemDto(WITH_VIDEO, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.RentCommercial

        assertEquals(setOf(Filter.CommercialType.BUSINESS), filter.commercialType)
        assertEquals(Filter.PriceType.PER_METER, filter.priceType)
        assertEquals(Filter.PricingPeriod.PER_MONTH, filter.pricingPeriod)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(60, Filter.MetroRemoteness.Unit.ON_FOOT),
            filter.metroRemoteness
        )
        assertEquals(Filter.TransactionType.SALE_OF_LEASE_RIGHTS, filter.transactionType)
        assertEquals(false, filter.withCommission)
        assertEquals(true, filter.nds)
        assertEquals(true, filter.usn)
        assertEquals(true, filter.cleaningIncluded)
        assertEquals(true, filter.utilitiesIncluded)
        assertEquals(true, filter.electricityIncluded)
        assertEquals(Range.valueOf(15L, null), filter.propertyArea)
        assertNull(filter.lotArea)
        assertNull(filter.withFurniture)
        assertNull(filter.withAircondition)
        assertEquals(
            setOf(Filter.CommercialBuildingType.RESIDENTIAL_BUILDING),
            filter.buildingType
        )
        assertNull(filter.businessCenterClass)
        assertNull(filter.hasTwentyFourSeven)
        assertNull(filter.hasParking)
        assertEquals(Filter.EntranceType.COMMON, filter.entranceType)
        assertNull(filter.commercialPlanType)
        assertNull(filter.hasVentilation)
        assertEquals(true, filter.withPhoto)
        assertEquals(true, filter.onlineShow)
        assertEquals(true, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
    }

    @Test
    fun shouldConvertRentCommercialLegalAddressTypeFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("RENT")),
                ParamsItemDto(CATEGORY, listOf("COMMERCIAL")),
                ParamsItemDto(COMMERCIAL_TYPE, listOf("LEGAL_ADDRESS")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_METER")),
                ParamsItemDto(PRICING_PERIOD, listOf("PER_MONTH")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("60")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_FOOT")),
                ParamsItemDto(DEAL_STATUS, listOf("SALE_OF_LEASE_RIGHTS")),
                ParamsItemDto(COMMISSION + RANGE_MAX_SUFFIX, listOf("0")),
                ParamsItemDto(TAXATION_FORM, listOf("NDS", "USN")),
                ParamsItemDto(CLEANING_INCLUDED, listOf("YES")),
                ParamsItemDto(UTILITIES_INCLUDED, listOf("YES")),
                ParamsItemDto(ELECTRICITY_INCLUDED, listOf("YES")),
                ParamsItemDto(PROPERTY_AREA + RANGE_MIN_SUFFIX, listOf("15")),
                ParamsItemDto(LOT_AREA + RANGE_MAX_SUFFIX, listOf("30")),
                ParamsItemDto(FURNITURE, listOf("YES")),
                ParamsItemDto(AIRCONDITION, listOf("YES")),
                ParamsItemDto(COMMERCIAL_BUILDING_TYPE, listOf("BUSINESS_CENTER")),
                ParamsItemDto(OFFICE_CLASS, listOf("B")),
                ParamsItemDto(HAS_TWENTY_FOUR_SEVEN, listOf("YES")),
                ParamsItemDto(HAS_PARKING, listOf("YES")),
                ParamsItemDto(ENTRANCE_TYPE, listOf("COMMON")),
                ParamsItemDto(COMMERCIAL_PLAN_TYPE, listOf("OPEN_SPACE")),
                ParamsItemDto(HAS_VENTILATION, listOf("YES")),
                ParamsItemDto(WITH_PHOTO, listOf("YES")),
                ParamsItemDto(ONLINE_SHOW, listOf("YES")),
                ParamsItemDto(WITH_VIDEO, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.RentCommercial

        assertEquals(setOf(Filter.CommercialType.LEGAL_ADDRESS), filter.commercialType)
        assertEquals(Filter.PriceType.PER_METER, filter.priceType)
        assertEquals(Filter.PricingPeriod.PER_MONTH, filter.pricingPeriod)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(60, Filter.MetroRemoteness.Unit.ON_FOOT),
            filter.metroRemoteness
        )
        assertEquals(Filter.TransactionType.SALE_OF_LEASE_RIGHTS, filter.transactionType)
        assertEquals(false, filter.withCommission)
        assertEquals(true, filter.nds)
        assertEquals(true, filter.usn)
        assertNull(filter.cleaningIncluded)
        assertNull(filter.utilitiesIncluded)
        assertNull(filter.electricityIncluded)
        assertEquals(Range.valueOf(15L, null), filter.propertyArea)
        assertNull(filter.lotArea)
        assertNull(filter.withFurniture)
        assertEquals(true, filter.withAircondition)
        assertEquals(setOf(Filter.CommercialBuildingType.BUSINESS_CENTER), filter.buildingType)
        assertEquals(setOf(Filter.BusinessCenterClass.B), filter.businessCenterClass)
        assertEquals(true, filter.hasTwentyFourSeven)
        assertEquals(Filter.EntranceType.COMMON, filter.entranceType)
        assertNull(filter.commercialPlanType)
        assertEquals(true, filter.hasVentilation)
        assertEquals(true, filter.withPhoto)
        assertEquals(true, filter.onlineShow)
        assertEquals(true, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
    }

    @Test
    fun shouldConvertSellGarageFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("SELL")),
                ParamsItemDto(CATEGORY, listOf("GARAGE")),
                ParamsItemDto(GARAGE_TYPE, listOf("GARAGE", "BOX", "PARKING_PLACE")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_METER")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("45")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_TRANSPORT")),
                ParamsItemDto(ELECTRICITY_SUPPLY, listOf("YES")),
                ParamsItemDto(HEATING_SUPPLY, listOf("YES")),
                ParamsItemDto(WATER_SUPPLY, listOf("YES")),
                ParamsItemDto(HAS_SECURITY, listOf("YES")),
                ParamsItemDto(AGENTS, listOf("NO")),
                ParamsItemDto(WITH_PHOTO, listOf("YES")),
                ParamsItemDto(ONLINE_SHOW, listOf("YES")),
                ParamsItemDto(WITH_VIDEO, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.SellGarage

        assertEquals(
            setOf(
                Filter.GarageType.GARAGE,
                Filter.GarageType.BOX,
                Filter.GarageType.PARKING_PLACE
            ),
            filter.garageType
        )
        assertEquals(Filter.PriceType.PER_METER, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(45, Filter.MetroRemoteness.Unit.ON_TRANSPORT),
            filter.metroRemoteness
        )
        assertEquals(true, filter.withElectricitySupply)
        assertEquals(true, filter.withHeatingSupply)
        assertEquals(true, filter.withWaterSupply)
        assertEquals(true, filter.withSecurity)
        assertEquals(false, filter.showFromAgents)
        assertEquals(true, filter.withPhoto)
        assertEquals(true, filter.onlineShow)
        assertEquals(true, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
    }

    @Test
    fun shouldConvertRentGarageFilter() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(TYPE, listOf("RENT")),
                ParamsItemDto(CATEGORY, listOf("GARAGE")),
                ParamsItemDto(GARAGE_TYPE, listOf("GARAGE", "BOX", "PARKING_PLACE")),
                ParamsItemDto(PRICE_TYPE, listOf("PER_OFFER")),
                ParamsItemDto(PRICE + RANGE_MIN_SUFFIX, listOf("10")),
                ParamsItemDto(PRICE + RANGE_MAX_SUFFIX, listOf("20")),
                ParamsItemDto(METRO_DISTANCE, listOf("45")),
                ParamsItemDto(METRO_DISTANCE_TYPE, listOf("ON_TRANSPORT")),
                ParamsItemDto(ELECTRICITY_SUPPLY, listOf("YES")),
                ParamsItemDto(HEATING_SUPPLY, listOf("YES")),
                ParamsItemDto(WATER_SUPPLY, listOf("YES")),
                ParamsItemDto(HAS_SECURITY, listOf("YES")),
                ParamsItemDto(AGENTS, listOf("NO")),
                ParamsItemDto(HAS_FEE, listOf("NO")),
                ParamsItemDto(UTILITIES_INCLUDED, listOf("YES")),
                ParamsItemDto(ELECTRICITY_INCLUDED, listOf("YES")),
                ParamsItemDto(WITH_PHOTO, listOf("YES")),
                ParamsItemDto(ONLINE_SHOW, listOf("YES")),
                ParamsItemDto(WITH_VIDEO, listOf("YES"))
            ),
            filter = JsonObject().apply {
                add(
                    INCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 1)
                                addProperty("title", "title1")
                            }
                        )
                    }
                )
                add(
                    EXCLUDE_TAG,
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("id", 2)
                                addProperty("title", "title2")
                            }
                        )
                    }
                )
                add(
                    USER,
                    JsonObject().apply {
                        addProperty("uid", "6")
                        add(
                            "agencyProfile",
                            JsonObject().apply {
                                addProperty("name", "agency1")
                                addProperty("userType", "AGENCY")
                            }
                        )
                    }
                )
            }
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val filter = context.filter as Filter.RentGarage

        assertEquals(
            setOf(
                Filter.GarageType.GARAGE,
                Filter.GarageType.BOX,
                Filter.GarageType.PARKING_PLACE
            ),
            filter.garageType
        )
        assertEquals(Filter.PriceType.PER_OFFER, filter.priceType)
        assertEquals(Range.valueOf(10L, 20L), filter.price)
        assertEquals(
            Filter.MetroRemoteness(45, Filter.MetroRemoteness.Unit.ON_TRANSPORT),
            filter.metroRemoteness
        )
        assertEquals(true, filter.withElectricitySupply)
        assertEquals(true, filter.withHeatingSupply)
        assertEquals(true, filter.withWaterSupply)
        assertEquals(true, filter.withSecurity)
        assertEquals(false, filter.showFromAgents)
        assertEquals(false, filter.hasFee)
        assertEquals(true, filter.utilitiesIncluded)
        assertEquals(true, filter.electricityIncluded)
        assertEquals(true, filter.withPhoto)
        assertEquals(true, filter.onlineShow)
        assertEquals(true, filter.withVideo)
        assertEquals(setOf(SearchTag(1, "title1")), filter.includeTags)
        assertEquals(setOf(SearchTag(2, "title2")), filter.excludeTags)
        assertEquals(Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY), filter.agency)
    }

    private fun convert(deepLinkDto: DeepLinkDto): DeepLinkParsedData {
        return converter.map(deepLinkDto, EmptyDescriptor)
    }

    private fun createDeepLinkDto(
        action: String? = null,
        region: GeoRegionDto? = null,
        geoIntents: List<GeoObjectDto>? = null,
        mapPolygons: List<GeoPolygonDto>? = null,
        commute: CommuteDto? = null,
        params: List<ParamsItemDto>? = null,
        filter: JsonObject? = null
    ): DeepLinkDto {
        return DeepLinkDto(
            action,
            region,
            geoIntents,
            mapPolygons,
            commute,
            params,
            filter
        )
    }
}
