package com.yandex.mobile.realty.filters

import com.yandex.mobile.realty.domain.model.Range
import com.yandex.mobile.realty.domain.model.common.CommissioningState
import com.yandex.mobile.realty.domain.model.search.AirconditionFilterInfo
import com.yandex.mobile.realty.domain.model.search.BusinessCenterClassFilterInfo
import com.yandex.mobile.realty.domain.model.search.CleaningFilterInfo
import com.yandex.mobile.realty.domain.model.search.CommercialBuildingTypeFilterInfo
import com.yandex.mobile.realty.domain.model.search.CommercialFloorFilterInfo
import com.yandex.mobile.realty.domain.model.search.CommercialPlanTypeFilterInfo
import com.yandex.mobile.realty.domain.model.search.CommercialRenovationFilterInfo
import com.yandex.mobile.realty.domain.model.search.ElectricityFilterInfo
import com.yandex.mobile.realty.domain.model.search.EntranceTypeFilterInfo
import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.domain.model.search.FurnitureFilterInfo
import com.yandex.mobile.realty.domain.model.search.HasParkingFilterInfo
import com.yandex.mobile.realty.domain.model.search.HasTwentyFourSevenFilterInfo
import com.yandex.mobile.realty.domain.model.search.HasVentilationFilterInfo
import com.yandex.mobile.realty.domain.model.search.LotAreaFilterInfo
import com.yandex.mobile.realty.domain.model.search.PropertyAreaFilterInfo
import com.yandex.mobile.realty.domain.model.search.SearchTag
import com.yandex.mobile.realty.domain.model.search.UtilitiesFilterInfo

fun getPopulatedBuyApartmentAnyFilter() = Filter.SellApartment(
    roomsCount = setOf(Filter.RoomsCount.ONE),
    price = Range.valueOf(1L, 2L),
    metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
    propertyArea = Range.valueOf(null, 10L),
    kitchenArea = Range.valueOf(10L, 15L),
    ceilingHeightMin = 4f,
    renovation = setOf(Filter.Renovation.DESIGNER_RENOVATION),
    balcony = Filter.Balcony.BALCONY,
    bathroom = Filter.Bathroom.MATCHED,
    floor = Range.valueOf(1L, 10L),
    lastFloor = true,
    exceptFirstFloor = true,
    builtYear = Range.valueOf(1900L, null),
    buildingType = setOf(Filter.BuildingType.BLOCK),
    buildingEpoch = setOf(Filter.BuildingEpoch.BREZHNEV),
    apartments = true,
    floors = Range.valueOf(10, null),
    parkingType = setOf(Filter.ParkingType.CLOSED),
    demolition = true,
    parkType = setOf(Filter.ParkType.FOREST),
    pondType = setOf(Filter.PondType.RIVER),
    expectMetro = true,
    showFromAgents = false,
    withPhoto = true,
    onlineShow = true,
    withVideo = true,
    withExcerptReport = true,
    includeTags = setOf(SearchTag(1, "Test")),
    excludeTags = setOf(SearchTag(2, "Test")),
    agency = Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY),
    buildingSeries = Filter.BuildingSeries(1_569_038, "1-335"),
    withFurniture = true,
    dealStatus = Filter.DealStatus.COUNTERSALE,
)

fun getPopulatedBuyApartmentOldFilter() = Filter.SellApartment(
    market = Filter.Market.SECONDARY,
    roomsCount = setOf(Filter.RoomsCount.ONE),
    price = Range.valueOf(1L, 2L),
    metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
    propertyArea = Range.valueOf(null, 10L),
    kitchenArea = Range.valueOf(10L, 15L),
    ceilingHeightMin = 4f,
    renovation = setOf(Filter.Renovation.DESIGNER_RENOVATION),
    balcony = Filter.Balcony.BALCONY,
    bathroom = Filter.Bathroom.MATCHED,
    floor = Range.valueOf(1L, 10L),
    lastFloor = true,
    exceptFirstFloor = true,
    builtYear = Range.valueOf(1900L, null),
    buildingType = setOf(Filter.BuildingType.BLOCK),
    buildingEpoch = setOf(Filter.BuildingEpoch.BREZHNEV),
    apartments = true,
    floors = Range.valueOf(10, null),
    parkingType = setOf(Filter.ParkingType.CLOSED),
    demolition = true,
    parkType = setOf(Filter.ParkType.FOREST),
    pondType = setOf(Filter.PondType.RIVER),
    expectMetro = true,
    showFromAgents = false,
    withPhoto = true,
    onlineShow = true,
    withVideo = true,
    withExcerptReport = true,
    includeTags = setOf(SearchTag(1, "Test")),
    excludeTags = setOf(SearchTag(2, "Test")),
    agency = Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY),
    withFurniture = true,
    buildingSeries = Filter.BuildingSeries(1_569_075, "1-405"),
    dealStatus = Filter.DealStatus.COUNTERSALE,
)

fun getPopulatedBuyApartmentNewBuildingFilter() = Filter.SiteApartment(
    roomsCount = setOf(Filter.RoomsCount.ONE),
    price = Range.valueOf(1L, 2L),
    metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
    propertyArea = Range.valueOf(null, 10L),
    kitchenArea = Range.valueOf(10L, 15L),
    ceilingHeightMin = 4f,
    decoration = setOf(Filter.Decoration.CLEAN),
    bathroom = Filter.Bathroom.MATCHED,
    floor = Range.valueOf(1L, 10L),
    commissioningState = CommissioningState.Delivered,
    buildingType = setOf(Filter.BuildingType.BLOCK),
    buildingClass = setOf(Filter.BuildingClass.BUSINESS),
    apartments = true,
    floors = Range.valueOf(10, null),
    lastFloor = true,
    parkingType = setOf(Filter.ParkingType.CLOSED),
    developer = Filter.Developer(1, "any"),
    parkType = setOf(Filter.ParkType.FOREST),
    pondType = setOf(Filter.PondType.RIVER),
    expectMetro = true,
    hasMortgage = true,
    hasSpecialProposal = true,
    hasInstallment = true,
    law214 = true,
    hasMaternityFunds = true,
    hasMilitaryMortgage = true,
    showOutdated = true
)

fun getPopulatedBuyApartmentNewBuildingSamoletFilter(): Filter.SiteApartment {
    return Filter.SiteApartment(
        roomsCount = setOf(Filter.RoomsCount.ONE),
        price = Range.valueOf(1L, 2L),
        metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
        propertyArea = Range.valueOf(null, 10L),
        kitchenArea = Range.valueOf(10L, 15L),
        ceilingHeightMin = 4f,
        decoration = setOf(Filter.Decoration.CLEAN),
        bathroom = Filter.Bathroom.MATCHED,
        floor = Range.valueOf(1L, 10L),
        commissioningState = CommissioningState.Delivered,
        buildingType = setOf(Filter.BuildingType.BLOCK),
        buildingClass = setOf(Filter.BuildingClass.BUSINESS),
        apartments = true,
        floors = Range.valueOf(10, null),
        parkingType = setOf(Filter.ParkingType.CLOSED),
        developer = Filter.Developer.SAMOLET,
        parkType = setOf(Filter.ParkType.FOREST),
        pondType = setOf(Filter.PondType.RIVER),
        expectMetro = true,
        hasMortgage = true,
        hasSpecialProposal = true,
        hasInstallment = true,
        law214 = true,
        hasMaternityFunds = true,
        hasMilitaryMortgage = true,
        showOutdated = true
    )
}

fun getPopulatedRentApartmentFilter() = Filter.RentApartment(
    roomsCount = setOf(Filter.RoomsCount.ONE),
    price = Range.valueOf(1L, 2L),
    metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
    propertyArea = Range.valueOf(null, 10L),
    kitchenArea = Range.valueOf(10L, 15L),
    ceilingHeightMin = 4f,
    renovation = setOf(Filter.Renovation.DESIGNER_RENOVATION),
    balcony = Filter.Balcony.LOGGIA,
    floor = Range.valueOf(1L, 10L),
    lastFloor = true,
    exceptFirstFloor = true,
    withFurniture = true,
    withRefrigerator = true,
    withDishwasher = true,
    withAircondition = true,
    withTV = true,
    withWashingMachine = true,
    withChildren = true,
    withPets = true,
    builtYear = Range.valueOf(1900L, null),
    buildingType = setOf(Filter.BuildingType.BLOCK),
    buildingEpoch = setOf(Filter.BuildingEpoch.BREZHNEV),
    floors = Range.valueOf(10, null),
    parkingType = setOf(Filter.ParkingType.CLOSED),
    demolition = true,
    parkType = setOf(Filter.ParkType.FOREST),
    pondType = setOf(Filter.PondType.RIVER),
    expectMetro = true,
    showFromAgents = false,
    hasFee = false,
    yandexRent = true,
    onlineShow = true,
    withVideo = true,
    includeTags = setOf(SearchTag(1, "Test")),
    excludeTags = setOf(SearchTag(2, "Test")),
    agency = Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY),
    buildingSeries = Filter.BuildingSeries(156_933, "1-507"),
)

fun getPopulatedBuyRoomFilter() = Filter.SellRoom(
    price = Range.valueOf(1L, 2L),
    metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
    livingArea = Range.valueOf(null, 10L),
    totalRooms = setOf(Filter.TotalRooms.FIVE),
    ceilingHeightMin = 4f,
    renovation = setOf(Filter.Renovation.DESIGNER_RENOVATION),
    kitchenArea = Range.valueOf(10L, 15L),
    bathroom = Filter.Bathroom.MATCHED,
    floor = Range.valueOf(1L, 10L),
    lastFloor = true,
    exceptFirstFloor = true,
    builtYear = Range.valueOf(1900L, null),
    buildingType = setOf(Filter.BuildingType.BLOCK),
    buildingEpoch = setOf(Filter.BuildingEpoch.BREZHNEV),
    floors = Range.valueOf(10, null),
    parkingType = setOf(Filter.ParkingType.CLOSED),
    demolition = true,
    parkType = setOf(Filter.ParkType.FOREST),
    pondType = setOf(Filter.PondType.RIVER),
    expectMetro = true,
    showFromAgents = false,
    withPhoto = true,
    onlineShow = true,
    withVideo = true,
    includeTags = setOf(SearchTag(1, "Test")),
    excludeTags = setOf(SearchTag(2, "Test")),
    agency = Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY)
)

fun getPopulatedRentRoomFilter() = Filter.RentRoom(
    price = Range.valueOf(1L, 2L),
    metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
    livingArea = Range.valueOf(null, 10L),
    totalRooms = setOf(Filter.TotalRooms.FIVE),
    ceilingHeightMin = 4f,
    renovation = setOf(Filter.Renovation.DESIGNER_RENOVATION),
    floor = Range.valueOf(1L, 10L),
    lastFloor = true,
    exceptFirstFloor = true,
    withFurniture = true,
    withRefrigerator = true,
    withDishwasher = true,
    withAircondition = true,
    withTV = true,
    withWashingMachine = true,
    withChildren = true,
    withPets = true,
    builtYear = Range.valueOf(1900L, null),
    buildingType = setOf(Filter.BuildingType.BLOCK),
    buildingEpoch = setOf(Filter.BuildingEpoch.BREZHNEV),
    floors = Range.valueOf(10, null),
    parkingType = setOf(Filter.ParkingType.CLOSED),
    demolition = true,
    parkType = setOf(Filter.ParkType.FOREST),
    pondType = setOf(Filter.PondType.RIVER),
    expectMetro = true,
    showFromAgents = false,
    hasFee = false,
    onlineShow = true,
    withVideo = true,
    includeTags = setOf(SearchTag(1, "Test")),
    excludeTags = setOf(SearchTag(2, "Test")),
    agency = Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY)
)

fun getPopulatedBuyHouseFilter() = Filter.SellHouse(
    market = Filter.Market.SECONDARY,
    price = Range.valueOf(1L, 2L),
    metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
    houseType = setOf(Filter.HouseType.DUPLEX),
    renovation = setOf(Filter.Renovation.DESIGNER_RENOVATION),
    houseArea = Range.valueOf(null, 10L),
    lotArea = Range.valueOf(10L, null),
    houseMaterialType = setOf(Filter.HouseMaterialType.BLOCK),
    withElectricitySupply = true,
    withGasSupply = true,
    withWaterSupply = true,
    withSewerageSupply = true,
    withHeatingSupply = true,
    parkType = setOf(Filter.ParkType.FOREST),
    pondType = setOf(Filter.PondType.RIVER),
    expectMetro = true,
    showFromAgents = false,
    withPhoto = true,
    onlineShow = true,
    withVideo = true,
    includeTags = setOf(SearchTag(1, "Test")),
    excludeTags = setOf(SearchTag(2, "Test")),
    agency = Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY),
    floors = Range.valueOf(1, 10)
)

fun getPopulatedRentHouseFilter() = Filter.RentHouse(
    price = Range.valueOf(1L, 2L),
    metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
    houseType = setOf(Filter.HouseType.DUPLEX),
    renovation = setOf(Filter.Renovation.DESIGNER_RENOVATION),
    houseArea = Range.valueOf(null, 10L),
    lotArea = Range.valueOf(10L, null),
    houseMaterialType = setOf(Filter.HouseMaterialType.BLOCK),
    parkType = setOf(Filter.ParkType.FOREST),
    pondType = setOf(Filter.PondType.RIVER),
    expectMetro = true,
    showFromAgents = false,
    hasFee = false,
    onlineShow = true,
    withVideo = true,
    includeTags = setOf(SearchTag(1, "Test")),
    excludeTags = setOf(SearchTag(2, "Test")),
    agency = Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY),
    floors = Range.valueOf(2, 8)
)

fun getPopulatedBuyLotFilter() = Filter.SellLot(
    market = Filter.Market.SECONDARY,
    price = Range.valueOf(1L, 2L),
    metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
    lotArea = Range.valueOf(10L, null),
    lotType = setOf(Filter.LotType.GARDEN, Filter.LotType.IGS),
    parkType = setOf(Filter.ParkType.FOREST),
    pondType = setOf(Filter.PondType.RIVER),
    expectMetro = true,
    showFromAgents = false,
    withPhoto = true,
    onlineShow = true,
    withVideo = true,
    includeTags = setOf(SearchTag(1, "Test")),
    excludeTags = setOf(SearchTag(2, "Test")),
    agency = Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY)
)

fun getPopulatedBuyGarageFitler() = Filter.SellGarage(
    garageType = setOf(Filter.GarageType.GARAGE),
    price = Range.valueOf(1L, 2L),
    metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
    withElectricitySupply = true,
    withHeatingSupply = true,
    withWaterSupply = true,
    withSecurity = true,
    showFromAgents = false,
    withPhoto = true,
    onlineShow = true,
    withVideo = true,
    includeTags = setOf(SearchTag(1, "Test")),
    excludeTags = setOf(SearchTag(2, "Test")),
    agency = Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY)
)

fun getPopulatedRentGarageFilter() = Filter.RentGarage(
    garageType = setOf(Filter.GarageType.GARAGE),
    price = Range.valueOf(1L, 2L),
    metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
    withElectricitySupply = true,
    withHeatingSupply = true,
    withWaterSupply = true,
    withSecurity = true,
    showFromAgents = false,
    hasFee = false,
    utilitiesIncluded = true,
    electricityIncluded = true,
    withPhoto = true,
    onlineShow = true,
    withVideo = true,
    includeTags = setOf(SearchTag(1, "Test")),
    excludeTags = setOf(SearchTag(2, "Test")),
    agency = Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY)
)

fun getPopulatedBuyCommercialAnyFilter() = getPopulatedSellCommercialFilter(null)

fun getPopulatedBuyCommercialLandFilter() =
    getPopulatedSellCommercialFilter(setOf(Filter.CommercialType.LAND))

fun getPopulatedBuyCommercialOfficeFilter() =
    getPopulatedSellCommercialFilter(setOf(Filter.CommercialType.OFFICE))

fun getPopulatedBuyCommercialRetailFilter() =
    getPopulatedSellCommercialFilter(setOf(Filter.CommercialType.RETAIL))

fun getPopulatedBuyCommercialFreePurposeFilter() =
    getPopulatedSellCommercialFilter(setOf(Filter.CommercialType.FREE_PURPOSE))

fun getPopulatedBuyCommercialPublicCateringFilter() =
    getPopulatedSellCommercialFilter(setOf(Filter.CommercialType.PUBLIC_CATERING))

fun getPopulatedBuyCommercialHotelFilter() =
    getPopulatedSellCommercialFilter(setOf(Filter.CommercialType.HOTEL))

fun getPopulatedBuyCommercialAutoRepairFilter() =
    getPopulatedSellCommercialFilter(setOf(Filter.CommercialType.AUTO_REPAIR))

fun getPopulatedBuyCommercialWarehouseFilter() =
    getPopulatedSellCommercialFilter(setOf(Filter.CommercialType.WAREHOUSE))

fun getPopulatedBuyCommercialManufacturingFilter() =
    getPopulatedSellCommercialFilter(setOf(Filter.CommercialType.MANUFACTURING))

fun getPopulatedBuyCommercialBusinessFilter() =
    getPopulatedSellCommercialFilter(setOf(Filter.CommercialType.BUSINESS))

fun getPopulatedRentCommercialAnyFilter() = getPopulatedRentCommercialFilter(null)

fun getPopulatedRentCommercialLandFilter() =
    getPopulatedRentCommercialFilter(setOf(Filter.CommercialType.LAND))

fun getPopulatedRentCommercialOfficeFilter() =
    getPopulatedRentCommercialFilter(setOf(Filter.CommercialType.OFFICE))

fun getPopulatedRentCommercialRetailFilter() =
    getPopulatedRentCommercialFilter(setOf(Filter.CommercialType.RETAIL))

fun getPopulatedRentCommercialFreePurposeFilter() =
    getPopulatedRentCommercialFilter(setOf(Filter.CommercialType.FREE_PURPOSE))

fun getPopulatedRentCommercialWarehouseFilter() =
    getPopulatedRentCommercialFilter(setOf(Filter.CommercialType.WAREHOUSE))

fun getPopulatedRentCommercialPublicCateringFilter() =
    getPopulatedRentCommercialFilter(setOf(Filter.CommercialType.PUBLIC_CATERING))

fun getPopulatedRentCommercialHotelFilter() =
    getPopulatedRentCommercialFilter(setOf(Filter.CommercialType.HOTEL))

fun getPopulatedRentCommercialAutoRepairFitler() =
    getPopulatedRentCommercialFilter(setOf(Filter.CommercialType.AUTO_REPAIR))

fun getPopulatedRentCommercialManufacturingFilter() =
    getPopulatedRentCommercialFilter(setOf(Filter.CommercialType.MANUFACTURING))

fun getPopulatedRentCommercialLegalFilter() =
    getPopulatedRentCommercialFilter(setOf(Filter.CommercialType.LEGAL_ADDRESS))

fun getPopulatedRentCommercialBusinessFilter() =
    getPopulatedRentCommercialFilter(setOf(Filter.CommercialType.BUSINESS))

private fun getPopulatedSellCommercialFilter(types: Set<Filter.CommercialType>?): Filter.SellCommercial {

    val lotAreaFilterInfo = LotAreaFilterInfo.valueOf(types)
    val propertyAreaFilterInfo = PropertyAreaFilterInfo.valueOf(types)
    val buildingTypeFilterInfo = CommercialBuildingTypeFilterInfo.valueOf(types)
    val commercialFloorFilterInfo = CommercialFloorFilterInfo.valueOf(types)
    val entranceTypeFilterInfo = EntranceTypeFilterInfo.valueOf(types)
    val commercialRenovationFilterInfo = CommercialRenovationFilterInfo.valueOf(types)
    val commercialPlanTypeFilterInfo = CommercialPlanTypeFilterInfo.valueOf(types)
    val hasVentilationFilterInfo = HasVentilationFilterInfo.valueOf(types)
    val furnitureFilterInfo = FurnitureFilterInfo.valueOf(types)
    val airconditionFilterInfo = AirconditionFilterInfo.valueOf(types)
    val hasTwentyFourSevenFilterInfo = HasTwentyFourSevenFilterInfo.valueOf(types)
    val hasParkingFilterInfo = HasParkingFilterInfo.valueOf(types)

    val buildingTypes = setOf(Filter.CommercialBuildingType.BUSINESS_CENTER)
        .takeIf { buildingTypes ->
            buildingTypes.any {
                buildingTypeFilterInfo.valueRestrictions?.contains(it) != false
            }
        }

    val businessCenterClassFilterInfo = BusinessCenterClassFilterInfo.valueOf(buildingTypes)

    return Filter.SellCommercial(
        commercialType = types,
        price = Range.valueOf(1L, 2L),
        metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
        propertyArea = Range.valueOf(null, 20L)
            .takeIf { propertyAreaFilterInfo.valueRestrictions?.contains(20L) != false },
        lotArea = Range.valueOf(10L, null)
            .takeIf { lotAreaFilterInfo.valueRestrictions?.contains(10L) != false },
        withFurniture = true
            .takeIf { furnitureFilterInfo.valueRestrictions?.contains(it) != false },
        withAircondition = true
            .takeIf { airconditionFilterInfo.valueRestrictions?.contains(it) != false },
        buildingType = buildingTypes,
        businessCenterClass = setOf(Filter.BusinessCenterClass.A)
            .filter { businessCenterClassFilterInfo.valueRestrictions?.contains(it) != false }
            .takeIf { it.isNotEmpty() }
            ?.toSet(),
        hasTwentyFourSeven = true
            .takeIf {
                hasTwentyFourSevenFilterInfo.valueRestrictions?.contains(it) != false
            },
        hasParking = true
            .takeIf { hasParkingFilterInfo.valueRestrictions?.contains(it) != false },
        aboveFirstFloor = true
            .takeIf { commercialFloorFilterInfo.valueRestrictions?.contains(it) != false },
        entranceType = Filter.EntranceType.COMMON
            .takeIf { entranceTypeFilterInfo.valueRestrictions?.contains(it) != false },
        renovation = setOf(Filter.CommercialRenovation.COSMETIC_DONE)
            .filter {
                commercialRenovationFilterInfo.valueRestrictions?.contains(it) != false
            }
            .takeIf { it.isNotEmpty() }
            ?.toSet(),
        commercialPlanType = Filter.CommercialPlanType.OPEN_SPACE
            .takeIf {
                commercialPlanTypeFilterInfo.valueRestrictions?.contains(it) != false
            },
        hasVentilation = true
            .takeIf { hasVentilationFilterInfo.valueRestrictions?.contains(it) != false },
        withPhoto = true,
        onlineShow = true,
        withVideo = true,
        includeTags = setOf(SearchTag(1, "Test")),
        excludeTags = setOf(SearchTag(2, "Test")),
        agency = Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY)
    )
}

private fun getPopulatedRentCommercialFilter(types: Set<Filter.CommercialType>?): Filter.RentCommercial {

    val lotAreaFilterInfo = LotAreaFilterInfo.valueOf(types)
    val propertyAreaFilterInfo = PropertyAreaFilterInfo.valueOf(types)
    val buildingTypeFilterInfo = CommercialBuildingTypeFilterInfo.valueOf(types)
    val commercialFloorFilterInfo = CommercialFloorFilterInfo.valueOf(types)
    val entranceTypeFilterInfo = EntranceTypeFilterInfo.valueOf(types)
    val commercialRenovationFilterInfo = CommercialRenovationFilterInfo.valueOf(types)
    val commercialPlanTypeFilterInfo = CommercialPlanTypeFilterInfo.valueOf(types)
    val hasVentilationFilterInfo = HasVentilationFilterInfo.valueOf(types)
    val furnitureFilterInfo = FurnitureFilterInfo.valueOf(types)
    val airconditionFilterInfo = AirconditionFilterInfo.valueOf(types)
    val hasTwentyFourSevenFilterInfo = HasTwentyFourSevenFilterInfo.valueOf(types)
    val hasParkingFilterInfo = HasParkingFilterInfo.valueOf(types)

    val buildingTypes = setOf(Filter.CommercialBuildingType.BUSINESS_CENTER)
        .takeIf { buildingTypes ->
            buildingTypes.any {
                buildingTypeFilterInfo.valueRestrictions?.contains(it) != false
            }
        }

    val businessCenterClassFilterInfo = BusinessCenterClassFilterInfo.valueOf(buildingTypes)
    val cleaningFilterInfo = CleaningFilterInfo.valueOf(types)
    val utilitiesFilterInfo = UtilitiesFilterInfo.valueOf(types)
    val electricityFilterInfo = ElectricityFilterInfo.valueOf(types)

    return Filter.RentCommercial(
        commercialType = types,
        price = Range.valueOf(1L, 2L),
        metroRemoteness = Filter.MetroRemoteness(10, Filter.MetroRemoteness.Unit.ON_FOOT),
        transactionType = Filter.TransactionType.DIRECT_RENT,
        withCommission = true,
        nds = true,
        usn = true,
        cleaningIncluded = true
            .takeIf { cleaningFilterInfo.valueRestrictions?.contains(it) != false },
        utilitiesIncluded = true
            .takeIf { utilitiesFilterInfo.valueRestrictions?.contains(it) != false },
        electricityIncluded = true
            .takeIf { electricityFilterInfo.valueRestrictions?.contains(it) != false },
        propertyArea = Range.valueOf(null, 20L)
            .takeIf { propertyAreaFilterInfo.valueRestrictions?.contains(20L) != false },
        lotArea = Range.valueOf(10L, null)
            .takeIf { lotAreaFilterInfo.valueRestrictions?.contains(10L) != false },
        withFurniture = true
            .takeIf { furnitureFilterInfo.valueRestrictions?.contains(it) != false },
        withAircondition = true
            .takeIf { airconditionFilterInfo.valueRestrictions?.contains(it) != false },
        buildingType = buildingTypes,
        businessCenterClass = setOf(Filter.BusinessCenterClass.A)
            .filter { businessCenterClassFilterInfo.valueRestrictions?.contains(it) != false }
            .takeIf { it.isNotEmpty() }
            ?.toSet(),
        hasTwentyFourSeven = true
            .takeIf {
                hasTwentyFourSevenFilterInfo.valueRestrictions?.contains(it) != false
            },
        hasParking = true
            .takeIf { hasParkingFilterInfo.valueRestrictions?.contains(it) != false },
        aboveFirstFloor = true
            .takeIf { commercialFloorFilterInfo.valueRestrictions?.contains(it) != false },
        entranceType = Filter.EntranceType.COMMON
            .takeIf { entranceTypeFilterInfo.valueRestrictions?.contains(it) != false },
        renovation = setOf(Filter.CommercialRenovation.COSMETIC_DONE)
            .filter {
                commercialRenovationFilterInfo.valueRestrictions?.contains(it) != false
            }
            .takeIf { it.isNotEmpty() }
            ?.toSet(),
        commercialPlanType = Filter.CommercialPlanType.OPEN_SPACE
            .takeIf {
                commercialPlanTypeFilterInfo.valueRestrictions?.contains(it) != false
            },
        hasVentilation = true
            .takeIf {
                hasVentilationFilterInfo.valueRestrictions?.contains(it) != false
            },
        withPhoto = true,
        onlineShow = true,
        withVideo = true,
        includeTags = setOf(SearchTag(1, "Test")),
        excludeTags = setOf(SearchTag(2, "Test")),
        agency = Filter.Agency("6", "agency1", Filter.Agency.Type.AGENCY)
    )
}

fun getPopulatedVillageHouseFilter() = Filter.VillageHouse(
    villageType = setOf(Filter.VillageType.COTTAGE),
    price = Range.valueOf(1L, 2L),
    houseArea = Range.valueOf(null, 20L),
    wallsType = setOf(Filter.WallType.BRICK),
    lotArea = Range.valueOf(10L, null),
    withElectricitySupply = true,
    withGasSupply = true,
    withWaterSupply = true,
    withSewerageSupply = true,
    withHeatingSupply = true,
    commissioningState = CommissioningState.Delivered,
    villageClass = setOf(Filter.VillageClass.BUSINESS),
    developer = Filter.Developer(1, "name"),
    landType = setOf(Filter.LandType.DNP),
    parkType = setOf(Filter.ParkType.FOREST),
    pondType = setOf(Filter.PondType.RIVER),
    hasRailwayStation = true
)

fun getPopulatedVillageLotFilter() = Filter.VillageLot(
    price = Range.valueOf(1L, 2L),
    lotArea = Range.valueOf(10L, null),
    withElectricitySupply = true,
    withGasSupply = true,
    withWaterSupply = true,
    withSewerageSupply = true,
    withHeatingSupply = true,
    commissioningState = CommissioningState.Delivered,
    villageClass = setOf(Filter.VillageClass.BUSINESS),
    developer = Filter.Developer(1, "name"),
    landType = setOf(Filter.LandType.DNP),
    parkType = setOf(Filter.ParkType.FOREST),
    pondType = setOf(Filter.PondType.RIVER),
    hasRailwayStation = true
)
