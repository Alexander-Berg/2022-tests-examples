package com.yandex.mobile.realty.test.filters

import android.view.View
import com.yandex.mobile.realty.core.matchers.ApartmentTypeDialogLookup
import com.yandex.mobile.realty.core.matchers.BalconyTypeDialogLookup
import com.yandex.mobile.realty.core.matchers.BathroomDialogLookup
import com.yandex.mobile.realty.core.matchers.BuildingClassDialogLookup
import com.yandex.mobile.realty.core.matchers.BuildingTypeDialogLookup
import com.yandex.mobile.realty.core.matchers.BusinessCenterClassDialogLookup
import com.yandex.mobile.realty.core.matchers.CeilingHeightDialogLookup
import com.yandex.mobile.realty.core.matchers.CommercialBuildingTypeDialogLookup
import com.yandex.mobile.realty.core.matchers.CommercialDealStatusDialogLookup
import com.yandex.mobile.realty.core.matchers.CommercialPlanTypeDialogLookup
import com.yandex.mobile.realty.core.matchers.CommercialRenovationDialogLookup
import com.yandex.mobile.realty.core.matchers.CommercialTypeLookup
import com.yandex.mobile.realty.core.matchers.CommissionDialogLookup
import com.yandex.mobile.realty.core.matchers.CommissioningStateDialogLookup
import com.yandex.mobile.realty.core.matchers.CommunicationDialogLookup
import com.yandex.mobile.realty.core.matchers.CommuteParamsLookup
import com.yandex.mobile.realty.core.matchers.DealStatusDialogLookup
import com.yandex.mobile.realty.core.matchers.DecorationDialogLookup
import com.yandex.mobile.realty.core.matchers.DemolitionDialogLookup
import com.yandex.mobile.realty.core.matchers.EntranceTypeDialogLookup
import com.yandex.mobile.realty.core.matchers.FacilitiesDialogLookup
import com.yandex.mobile.realty.core.matchers.FiltersLookup
import com.yandex.mobile.realty.core.matchers.FirstFloorDialogLookup
import com.yandex.mobile.realty.core.matchers.FurnitureDialogLookup
import com.yandex.mobile.realty.core.matchers.HouseMaterialTypeDialogLookup
import com.yandex.mobile.realty.core.matchers.HouseTypeDialogLookup
import com.yandex.mobile.realty.core.matchers.LastFloorDialogLookup
import com.yandex.mobile.realty.core.matchers.ParkTypeDialogLookup
import com.yandex.mobile.realty.core.matchers.ParkingTypeDialogLookup
import com.yandex.mobile.realty.core.matchers.PondTypeDialogLookup
import com.yandex.mobile.realty.core.matchers.PriceDialogLookup
import com.yandex.mobile.realty.core.matchers.RenovationTypeDialogLookup
import com.yandex.mobile.realty.core.matchers.RoomsTotalDialogLookup
import com.yandex.mobile.realty.core.matchers.TimeToMetroDialogLookup
import com.yandex.mobile.realty.core.matchers.VillageClassDialogLookup
import com.yandex.mobile.realty.core.matchers.VillageLandTypeDialogLookup
import com.yandex.mobile.realty.core.matchers.WallTypeDialogLookup
import com.yandex.mobile.realty.core.viewMatchers.NamedStringMatcher
import com.yandex.mobile.realty.network.WebAPIBool
import org.hamcrest.Matcher
import java.util.*

/**
 * @author rogovalex on 17/06/2019.
 */
typealias OfferCategoryFactory = (PropertyType) -> OfferCategory

class OfferCategory(
    val params: Array<Pair<String, String>>,
    val geoSuggestParams: Array<Pair<String, String>>,
    val matcher: (FiltersLookup) -> Matcher<View>
) {

    companion object {

        val ANY: OfferCategoryFactory = ::anyOf
        val PRIMARY: OfferCategoryFactory = ::primaryOf
        val SECONDARY: OfferCategoryFactory = ::secondaryOf

        private fun anyOf(propertyType: PropertyType): OfferCategory {
            return when (propertyType) {
                PropertyType.APARTMENT -> OfferCategory(
                    params = arrayOf("objectType" to "OFFER"),
                    geoSuggestParams = arrayOf("objectType" to "OFFER")
                ) { FiltersLookup.matchesApartmentCategorySelectorAny() }
                PropertyType.HOUSE -> OfferCategory(
                    params = arrayOf("objectType" to "OFFER"),
                    geoSuggestParams = arrayOf("objectType" to "OFFER")
                ) { FiltersLookup.matchesHouseCategorySelectorAny() }
                PropertyType.LOT -> OfferCategory(
                    params = arrayOf("objectType" to "OFFER"),
                    geoSuggestParams = arrayOf("objectType" to "OFFER")
                ) { FiltersLookup.matchesLotCategorySelectorAny() }
                else -> throw IllegalArgumentException("No category for given type $propertyType")
            }
        }

        private fun primaryOf(propertyType: PropertyType): OfferCategory {
            return when (propertyType) {
                PropertyType.APARTMENT -> OfferCategory(
                    params = arrayOf("objectType" to "NEWBUILDING"),
                    geoSuggestParams = arrayOf("objectType" to "NEWBUILDING")
                ) { FiltersLookup.matchesApartmentCategorySelectorNew() }
                PropertyType.HOUSE -> OfferCategory(
                    params = arrayOf("objectType" to "VILLAGE"),
                    geoSuggestParams = arrayOf("objectType" to "VILLAGE")
                ) { FiltersLookup.matchesHouseCategorySelectorVillage() }
                PropertyType.LOT -> OfferCategory(
                    params = arrayOf("objectType" to "VILLAGE"),
                    geoSuggestParams = arrayOf("objectType" to "VILLAGE")
                ) { FiltersLookup.matchesLotCategorySelectorVillage() }
                else -> throw IllegalArgumentException("No category for given type $propertyType")
            }
        }

        private fun secondaryOf(propertyType: PropertyType): OfferCategory {
            return when (propertyType) {
                PropertyType.APARTMENT -> OfferCategory(
                    params = arrayOf("objectType" to "OFFER", "newFlat" to "NO"),
                    geoSuggestParams = arrayOf("objectType" to "OFFER")
                ) { FiltersLookup.matchesApartmentCategorySelectorSecondary() }
                PropertyType.HOUSE -> OfferCategory(
                    params = arrayOf("objectType" to "OFFER", "primarySale" to "NO"),
                    geoSuggestParams = arrayOf("objectType" to "OFFER")
                ) { FiltersLookup.matchesHouseCategorySelectorSecondary() }
                PropertyType.LOT -> OfferCategory(
                    params = arrayOf("objectType" to "OFFER", "primarySale" to "NO"),
                    geoSuggestParams = arrayOf("objectType" to "OFFER")
                ) { FiltersLookup.matchesLotCategorySelectorSecondary() }
                else -> throw IllegalArgumentException("No category for given type $propertyType")
            }
        }
    }
}

enum class CeilingHeight(
    val value: String,
    val matcher: (CeilingHeightDialogLookup) -> Matcher<View>,
    val expected: String
) {

    CENTIMETERS_250("2.5", CeilingHeightDialogLookup::matchesSelectorCentimeters250, "от 2.5 м"),
    CENTIMETERS_270("2.7", CeilingHeightDialogLookup::matchesSelectorCentimeters270, "от 2.7 м"),
    CENTIMETERS_300("3.0", CeilingHeightDialogLookup::matchesSelectorCentimeters300, "от 3 м"),
    CENTIMETERS_400("4.0", CeilingHeightDialogLookup::matchesSelectorCentimeters400, "от 4 м");

    val param: Pair<String, String>
        get() = "ceilingHeightMin" to value
}

enum class CommercialType(
    val value: String,
    val matcher: (CommercialTypeLookup) -> Matcher<View>,
    val expected: String
) {

    LAND("LAND", CommercialTypeLookup::matchesTypeSelectorLand, "Земельный участок"),
    OFFICE("OFFICE", CommercialTypeLookup::matchesOtherTypeCheckerOffice, "Офис"),
    RETAIL("RETAIL", CommercialTypeLookup::matchesOtherTypeCheckerRetail, "Торговое помещение"),
    FREE_PURPOSE(
        "FREE_PURPOSE",
        CommercialTypeLookup::matchesOtherTypeCheckerFreePurpose,
        "Помещение своб. назначения"
    ),
    WAREHOUSE(
        "WAREHOUSE",
        CommercialTypeLookup::matchesOtherTypeCheckerWarehouse,
        "Складское помещение"
    ),
    PUBLIC_CATERING(
        "PUBLIC_CATERING",
        CommercialTypeLookup::matchesOtherTypeCheckerPublicCatering,
        "Общепит"
    ),
    HOTEL("HOTEL", CommercialTypeLookup::matchesOtherTypeCheckerHotel, "Гостиница"),
    AUTO_REPAIR(
        "AUTO_REPAIR",
        CommercialTypeLookup::matchesOtherTypeCheckerAutoRepair,
        "Автосервис"
    ),
    MANUFACTURING(
        "MANUFACTURING",
        CommercialTypeLookup::matchesOtherTypeCheckerManufacturing,
        "Производ. помещение"
    ),

    LEGAL_ADDRESS(
        "LEGAL_ADDRESS",
        CommercialTypeLookup::matchesOtherTypeCheckerLegalAddress,
        "Юридический адрес"
    ),
    BUSINESS(
        "BUSINESS",
        CommercialTypeLookup::matchesOtherTypeCheckerBusiness,
        "Готовый бизнес"
    );

    val param: Pair<String, String>
        get() = "commercialType" to value

    companion object {

        val sellNonLandTypes: EnumSet<CommercialType> by lazy {
            EnumSet.of(
                OFFICE,
                WAREHOUSE,
                FREE_PURPOSE,
                AUTO_REPAIR,
                BUSINESS,
                MANUFACTURING,
                PUBLIC_CATERING,
                RETAIL,
                HOTEL
            )
        }

        val rentNonLandTypes: EnumSet<CommercialType> by lazy {
            EnumSet.of(
                OFFICE,
                WAREHOUSE,
                FREE_PURPOSE,
                AUTO_REPAIR,
                BUSINESS,
                MANUFACTURING,
                PUBLIC_CATERING,
                RETAIL,
                HOTEL,
                LEGAL_ADDRESS
            )
        }
    }
}

enum class RenovationType(
    val value: String,
    val matcher: Matcher<View>,
    val expected: String
) {

    COSMETIC_DONE(
        "COSMETIC_DONE",
        RenovationTypeDialogLookup.matchesCheckerCosmetic(),
        "Косметический"
    ),
    EURO("EURO", RenovationTypeDialogLookup.matchesCheckerEuro(), "Евро"),
    DESIGNER_RENOVATION(
        "DESIGNER_RENOVATION",
        RenovationTypeDialogLookup.matchesCheckerDesignerRenovation(),
        "Дизайнерский"
    ),
    NEEDS_RENOVATION(
        "NEEDS_RENOVATION",
        RenovationTypeDialogLookup.matchesCheckerNeedsRenovation(),
        "Требуется ремонт"
    ),
    NON_GRANDMOTHER(
        "NON_GRANDMOTHER",
        RenovationTypeDialogLookup.matchesCheckerNonGrandmother(),
        "Современный"
    );

    val param: Pair<String, String>
        get() = "renovation" to value
}

enum class BalconyType(
    val value: String,
    val matcher: (BalconyTypeDialogLookup) -> Matcher<View>,
    val expected: String
) {

    BALCONY("BALCONY", BalconyTypeDialogLookup::matchesSelectorBalcony, "Есть балкон"),
    LOGGIA("LOGGIA", BalconyTypeDialogLookup::matchesSelectorLoggia, "Есть лоджия"),
    ANY("ANY", BalconyTypeDialogLookup::matchesSelectorBalconyOrLoggia, "Есть балкон или лоджия");

    val param: Pair<String, String>
        get() = "balcony" to value
}

enum class DealType(val value: String, val matcher: (FiltersLookup) -> Matcher<View>) {

    SELL("SELL", { FiltersLookup.matchesDealTypePopupBuy() }),
    RENT("RENT", { FiltersLookup.matchesDealTypePopupRent() });

    val param: Pair<String, String>
        get() = "type" to value
}

enum class GarageType(val value: String, val matcher: (FiltersLookup) -> Matcher<View>) {

    BOX("BOX", { FiltersLookup.matchesGarageTypeCheckerBox() }),
    GARAGE("GARAGE", { FiltersLookup.matchesGarageTypeCheckerGarage() }),
    PARKING_PLACE("PARKING_PLACE", { FiltersLookup.matchesGarageTypeCheckerParkingPlace() });

    val param: Pair<String, String>
        get() = "garageType" to value
}

enum class PricingPeriod(
    val value: String,
    val matcher: (PriceDialogLookup) -> Matcher<View>
) {

    PER_MONTH("PER_MONTH", { PriceDialogLookup.matchesPeriodSelectorPerMonth() }),
    PER_YEAR("PER_YEAR", { PriceDialogLookup.matchesPeriodSelectorPerYear() });

    val param: Pair<String, String>
        get() = "pricingPeriod" to value
}

enum class PriceType(
    val value: String,
    val matcher: (PriceDialogLookup) -> Matcher<View>
) {

    PER_OFFER("PER_OFFER", { PriceDialogLookup.matchesTypeSelectorPerOffer() }),
    PER_METER("PER_METER", { PriceDialogLookup.matchesTypeSelectorPerMeter() }),
    PER_ARE("PER_ARE", { PriceDialogLookup.matchesTypeSelectorPerAre() });

    val param: Pair<String, String>
        get() = "priceType" to value
}

enum class PropertyType(val value: String, val matcher: (FiltersLookup) -> Matcher<View>) {

    APARTMENT("APARTMENT", { FiltersLookup.matchesPropertyTypePopupApartment() }),
    ROOM("ROOM", { FiltersLookup.matchesPropertyTypePopupRoom() }),
    HOUSE("HOUSE", { FiltersLookup.matchesPropertyTypePopupHouse() }),
    LOT("LOT", { FiltersLookup.matchesPropertyTypePopupLot() }),
    COMMERCIAL("COMMERCIAL", { FiltersLookup.matchesPropertyTypePopupCommercial() }),
    GARAGE("GARAGE", { FiltersLookup.matchesPropertyTypePopupGarage() });

    val param: Pair<String, String>
        get() = "category" to value
}

enum class RentTime(val value: String, val matcher: (FiltersLookup) -> Matcher<View>) {

    SHORT("SHORT", { FiltersLookup.matchesRentTimeSelectorShort() }),
    LARGE("LARGE", { FiltersLookup.matchesRentTimeSelectorLong() });

    val param: Pair<String, String>
        get() = "rentTime" to value
}

enum class RoomsTotal(val value: String, val matcher: (FiltersLookup) -> Matcher<View>) {

    STUDIO("STUDIO", { FiltersLookup.matchesRoomsCheckerStudio() }),
    ONE("1", { FiltersLookup.matchesRoomsCheckerOne() }),
    TWO("2", { FiltersLookup.matchesRoomsCheckerTwo() }),
    THREE("3", { FiltersLookup.matchesRoomsCheckerThree() }),
    FOUR_PLUS("PLUS_4", { FiltersLookup.matchesRoomsCheckerFourPlus() });

    val param: Pair<String, String>
        get() = "roomsTotal" to value
}

enum class TimeToMetro(
    val value: String,
    val matcher: (TimeToMetroDialogLookup) -> Matcher<View>,
    val expected: String
) {

    MINUTES_5("5", TimeToMetroDialogLookup::matchesListItemMinutes5, "5 минут"),
    MINUTES_10("10", TimeToMetroDialogLookup::matchesListItemMinutes10, "10 минут"),
    MINUTES_15("15", TimeToMetroDialogLookup::matchesListItemMinutes15, "15 минут"),
    MINUTES_20("20", TimeToMetroDialogLookup::matchesListItemMinutes20, "20 минут"),
    MINUTES_25("25", TimeToMetroDialogLookup::matchesListItemMinutes25, "25 минут"),
    MINUTES_30("30", TimeToMetroDialogLookup::matchesListItemMinutes30, "30 минут"),
    MINUTES_45("45", TimeToMetroDialogLookup::matchesListItemMinutes45, "45 минут"),
    MINUTES_60("60", TimeToMetroDialogLookup::matchesListItemMinutes60, "1 час");

    val param: Pair<String, String>
        get() = "timeToMetro" to value
}

enum class TimeToMetroTransport(
    val value: String,
    val matcher: (TimeToMetroDialogLookup) -> Matcher<View>,
    val expected: String
) {

    ON_FOOT("ON_FOOT", TimeToMetroDialogLookup::matchesTimeToMetroTransportOnFoot, " пешком"),
    ON_TRANSPORT(
        "ON_TRANSPORT",
        TimeToMetroDialogLookup::matchesTimeToMetroTransportOnTransport,
        " на транспорте"
    );

    val param: Pair<String, String>
        get() = "metroTransport" to value
}

enum class Bathroom(
    val value: String,
    val matcher: (BathroomDialogLookup) -> Matcher<View>,
    val expected: String
) {

    MATCHED(
        "MATCHED",
        { BathroomDialogLookup.matchesSelectorMatched() },
        "Совмещённый",
    ),
    SEPARATED(
        "SEPARATED",
        { BathroomDialogLookup.matchesSelectorSeparated() },
        "Раздельный"
    ),
    TWO_AND_MORE(
        "TWO_AND_MORE",
        { BathroomDialogLookup.matchesSelectorTwoAndMore() },
        "Два и более"
    );

    val param: Pair<String, String>
        get() = "bathroomUnit" to value
}

enum class ApartmentType(
    val value: String,
    val matcher: (ApartmentTypeDialogLookup) -> Matcher<View>,
    val expected: String
) {

    YES("YES", ApartmentTypeDialogLookup::matchesSelectorOnlyApartments, "Только апартаменты"),
    NO("NO", ApartmentTypeDialogLookup::matchesSelectorNonApartments, "Без апартаментов");

    val param: Pair<String, String>
        get() = "apartments" to value
}

enum class ParkingType(
    val value: String,
    val matcher: (ParkingTypeDialogLookup) -> Matcher<View>,
    val expected: String
) {

    CLOSED("CLOSED", ParkingTypeDialogLookup::matchesCheckerClosed, "Закрытая"),
    UNDERGROUND(
        "UNDERGROUND",
        ParkingTypeDialogLookup::matchesCheckerUnderground,
        "Подземная"
    ),
    OPEN("OPEN", ParkingTypeDialogLookup::matchesCheckerOpen, "Открытая");

    val param: Pair<String, String>
        get() = "parkingType" to value
}

enum class ParkType(
    val value: String,
    val matcher: () -> NamedStringMatcher,
    val expected: String
) {

    FOREST("FOREST", ParkTypeDialogLookup::matchesCheckerForest, "Лес"),
    PARK("PARK", ParkTypeDialogLookup::matchesCheckerPark, "Парк"),
    GARDEN("GARDEN", ParkTypeDialogLookup::matchesCheckerGarden, "Сад");

    val param: Pair<String, String>
        get() = "parkType" to value
}

enum class PondType(
    val value: String,
    val matcher: () -> NamedStringMatcher,
    val expected: String
) {

    SEA("SEA", PondTypeDialogLookup::matchesCheckerSea, "Море"),
    BAY("BAY", PondTypeDialogLookup::matchesCheckerBay, "Залив"),
    STRAIT("STRAIT", PondTypeDialogLookup::matchesCheckerStrait, "Пролив"),
    LAKE("LAKE", PondTypeDialogLookup::matchesCheckerLake, "Озеро"),
    POND("POND", PondTypeDialogLookup::matchesCheckerPond, "Пруд"),
    RIVER("RIVER", PondTypeDialogLookup::matchesCheckerRiver, "Река");

    val param: Pair<String, String>
        get() = "pondType" to value
}

enum class BuildingType(
    val param: Pair<String, String>,
    val matcher: (BuildingTypeDialogLookup) -> Matcher<String>,
    val expected: String
) {

    BRICK("buildingType" to "BRICK", BuildingTypeDialogLookup::matchesCheckerBrick, "Кирпичный"),
    MONOLIT(
        "buildingType" to "MONOLIT",
        BuildingTypeDialogLookup::matchesCheckerMono,
        "Монолитный"
    ),
    PANEL("buildingType" to "PANEL", BuildingTypeDialogLookup::matchesCheckerPanel, "Панельный"),
    MONOLIT_BRICK(
        "buildingType" to "MONOLIT_BRICK",
        BuildingTypeDialogLookup::matchesCheckerMonolitBrick,
        "Кирпично-монолитный"
    ),
    BLOCK("buildingType" to "BLOCK", BuildingTypeDialogLookup::matchesCheckerBlock, "Блочный"),
    STALIN(
        "buildingEpoch" to "STALIN",
        BuildingTypeDialogLookup::matchesCheckerEpochStalin,
        "Сталинка"
    ),
    KHRUSHCHEV(
        "buildingEpoch" to "KHRUSHCHEV",
        BuildingTypeDialogLookup::matchesCheckerEpochKhrushchev,
        "Хрущёвка"
    ),
    BREZHNEV(
        "buildingEpoch" to "BREZHNEV",
        BuildingTypeDialogLookup::matchesCheckerEpochBrezhnev,
        "Брежневка"
    );
}

enum class DemolitionType(
    val value: String,
    val matcher: (DemolitionDialogLookup) -> Matcher<View>,
    val expected: String
) {

    YES(
        "YES",
        DemolitionDialogLookup::matchesSelectorYes,
        "Показать только дома под снос"
    ),
    NO(
        "NO",
        DemolitionDialogLookup::matchesSelectorNo,
        "Не показывать дома под снос"
    );

    val param: Pair<String, String>
        get() = "expectDemolition" to value
}

enum class HasFurniture(
    val value: String,
    val matcher: (FurnitureDialogLookup) -> Matcher<View>,
    val expected: String
) {

    YES("YES", FurnitureDialogLookup::matchesSelectorYes, "Есть мебель"),
    NO("NO", FurnitureDialogLookup::matchesSelectorNO, "Нет мебели");

    val param: Pair<String, String>
        get() = "hasFurniture" to value
}

enum class Facilities(
    val param: Pair<String, String>,
    val matcher: (FacilitiesDialogLookup) -> Matcher<View>,
    val expected: String
) {

    DISHWASHER(
        "hasDishwasher" to "YES",
        FacilitiesDialogLookup::matchesCheckerDishwasher,
        "Посудомоечная машина"
    ),
    REFRIGERATOR(
        "hasRefrigerator" to "YES",
        FacilitiesDialogLookup::matchesCheckerRefrigerator,
        "Холодильник"
    ),
    AIR_CONDITION(
        "hasAircondition" to "YES",
        FacilitiesDialogLookup::matchesCheckerAircondition,
        "Кондиционер"
    ),
    TELEVISION(
        "hasTelevision" to "YES",
        FacilitiesDialogLookup::matchesCheckerTelevision,
        "Телевизор"
    ),
    WASHING_MACHINE(
        "hasWashingMachine" to "YES",
        FacilitiesDialogLookup::matchesCheckerWashingMachine,
        "Стиральная машина"
    ),
    WITH_CHILDREN(
        "withChildren" to "YES",
        FacilitiesDialogLookup::matchesCheckerChildrenAllowed,
        "Можно с детьми"
    ),
    WITH_PETS(
        "withPets" to "YES",
        FacilitiesDialogLookup::matchesCheckerPetsAllowed,
        "Можно с животными"
    );
}

enum class TotalRooms(
    val value: String,
    val matcher: (RoomsTotalDialogLookup) -> Matcher<View>,
    val expected: String
) {

    TWO("2", RoomsTotalDialogLookup::matchesCheckerTwo, "2"),
    THREE("3", RoomsTotalDialogLookup::matchesCheckerThree, "3"),
    FOUR("4", RoomsTotalDialogLookup::matchesCheckerFour, "4"),
    FIVE("5", RoomsTotalDialogLookup::matchesCheckerFive, "5"),
    SIX("6", RoomsTotalDialogLookup::matchesCheckerSix, "6"),
    SEVEN_PLUS("PLUS_7", RoomsTotalDialogLookup::matchesCheckerSevenPlus, "7+");

    val param: Pair<String, String>
        get() = "roomsTotal" to value
}

enum class HouseType(
    val value: String,
    val matcher: (HouseTypeDialogLookup) -> Matcher<View>,
    val expected: String
) {

    TOWNHOUSE("TOWNHOUSE", HouseTypeDialogLookup::matchesCheckerTownhouse, "Таунхаус"),
    DUPLEX("DUPLEX", HouseTypeDialogLookup::matchesCheckerDuplex, "Дуплекс"),
    PART_HOUSE("PARTHOUSE", HouseTypeDialogLookup::matchesCheckerPartHouse, "Часть дома"),
    HOUSE("HOUSE", HouseTypeDialogLookup::matchesCheckerHouse, "Отдельный дом");

    val param: Pair<String, String>
        get() = "houseType" to value
}

enum class LastFloor(
    val value: Boolean,
    val matcher: NamedStringMatcher,
    val expected: String
) {
    ONLY_LAST_FLOOR(true, LastFloorDialogLookup.matchesOnlyLastFloor(), "Только последний этаж"),
    EXCEPT_LAST_FLOOR(
        false,
        LastFloorDialogLookup.matchesExceptLastFloor(),
        "Кроме последнего этажа"
    );

    val param: Pair<String, String>
        get() = "lastFloor" to WebAPIBool.fromBoolean(value).toString()
}

enum class GarageCommunication(
    val param: Pair<String, String>,
    val fieldMatcher: (FiltersLookup) -> Matcher<View>,
    val valueMatcher: (FiltersLookup) -> Matcher<View>,
    val expected: String
) {

    ELECTRICITY(
        "hasElectricitySupply" to "YES",
        { FiltersLookup.matchesFieldHasElectricity() },
        { FiltersLookup.matchesHasElectricityValue() },
        "Электричество"
    ),
    WATER(
        "hasWaterSupply" to "YES",
        { FiltersLookup.matchesFieldHasWater() },
        { FiltersLookup.matchesHasWaterValue() },
        "Вода"
    ),
    HEATING(
        "hasHeatingSupply" to "YES",
        { FiltersLookup.matchesFieldHasHeat() },
        { FiltersLookup.matchesHasHeatValue() },
        "Отопление"
    );
}

enum class HouseCommunication(
    val param: Pair<String, String>,
    val matcher: (CommunicationDialogLookup) -> Matcher<View>,
    val expected: String
) {

    ELECTRICITY(
        "hasElectricitySupply" to "YES",
        CommunicationDialogLookup::matchesCheckerElecricity,
        "Электричество"
    ),
    GAS(
        "hasGasSupply" to "YES",
        CommunicationDialogLookup::matchesCheckerGas,
        "Газ"
    ),
    WATER(
        "hasWaterSupply" to "YES",
        CommunicationDialogLookup::matchesCheckerWater,
        "Вода"
    ),
    SEWERAGE(
        "hasSewerageSupply" to "YES",
        CommunicationDialogLookup::matchesCheckerSewerage,
        "Канализация"
    ),
    HEATING(
        "hasHeatingSupply" to "YES",
        CommunicationDialogLookup::matchesCheckerHeating,
        "Отопление"
    );
}

enum class TypeLot(
    val value: String,
    val fieldMatcher: (FiltersLookup) -> Matcher<View>,
    val valueMatcher: (FiltersLookup) -> Matcher<View>,
    val expected: String
) {

    IGS(
        "IGS",
        { FiltersLookup.matchesFieldLotTypeIzs() },
        { FiltersLookup.matchesLotTypeIzsValue() },
        "ИЖС"
    ),
    GARDEN(
        "GARDEN",
        { FiltersLookup.matchesFieldLotTypeGarden() },
        { FiltersLookup.matchesLotTypeGardenValue() },
        "В садоводстве"
    );

    val param: Pair<String, String>
        get() = "lotType" to value
}

enum class DealCondition(
    val param: Pair<String, String>,
    val fieldMatcher: (FiltersLookup) -> Matcher<View>,
    val valueMatcher: (FiltersLookup) -> Matcher<View>,
    val expected: String
) {

    DISCOUNT(
        "hasSpecialProposal" to "YES",
        { FiltersLookup.matchesFieldHasDiscount() },
        { FiltersLookup.matchesHasDiscountValue() },
        "Скидки"
    ),
    MORTGAGE(
        "hasSiteMortgage" to "YES",
        { FiltersLookup.matchesFieldHasMortgage() },
        { FiltersLookup.matchesHasMortgageValue() },
        "Ипотека"
    ),
    INSTALLMENT(
        "hasInstallment" to "YES",
        { FiltersLookup.matchesFieldHasInstallment() },
        { FiltersLookup.matchesHasInstallmentValue() },
        "Расрочка"
    ),
    FZ_214(
        "dealType" to "FZ_214",
        { FiltersLookup.matchesFieldHasFz214() },
        { FiltersLookup.matchesHasFz214Value() },
        "214 ФЗ"
    ),
    MATERNITY_FUNDS(
        "hasSiteMaternityFunds" to "YES",
        { FiltersLookup.matchesFieldHasMaternityFunds() },
        { FiltersLookup.matchesHasMaternityFundsValue() },
        "Материнский капитал"
    ),
    MILITARY_MORTGAGE(
        "hasMilitarySiteMortgage" to "YES",
        { FiltersLookup.matchesFieldHasMilitaryMortgage() },
        { FiltersLookup.matchesHasMilitaryMortgageValue() },
        "Военная ипотека"
    );
}

enum class BuildingClass(
    val value: String,
    val matcher: (BuildingClassDialogLookup) -> Matcher<View>,
    val expected: String
) {

    ECONOM("ECONOM", BuildingClassDialogLookup::matchesCheckerEconom, "Эконом"),
    COMFORT("COMFORT", BuildingClassDialogLookup::matchesCheckerComfort, "Комфорт"),
    COMFORT_PLUS("COMFORT_PLUS", BuildingClassDialogLookup::matchesCheckerComfortPlus, "Комфорт+"),
    BUSINESS("BUSINESS", BuildingClassDialogLookup::matchesCheckerBusiness, "Бизнес"),
    ELITE("ELITE", BuildingClassDialogLookup::matchesCheckerElite, "Элитный");

    val param: Pair<String, String>
        get() = "buildingClass" to value
}

enum class VillageClass(
    val value: String,
    val matcher: (VillageClassDialogLookup) -> Matcher<View>,
    val expected: String
) {

    ECONOM("ECONOM", VillageClassDialogLookup::matchesCheckerEconom, "Эконом"),
    COMFORT("COMFORT", VillageClassDialogLookup::matchesCheckerComfort, "Комфорт"),
    COMFORT_PLUS("COMFORT_PLUS", VillageClassDialogLookup::matchesCheckerComfortPlus, "Комфорт+"),
    BUSINESS("BUSINESS", VillageClassDialogLookup::matchesCheckerBusiness, "Бизнес"),
    ELITE("ELITE", VillageClassDialogLookup::matchesCheckerElite, "Элитный");

    val param: Pair<String, String>
        get() = "villageClass" to value
}

enum class Decoration(
    val value: String,
    val matcher: (DecorationDialogLookup) -> Matcher<View>,
    val expected: String
) {

    ROUGH("ROUGH", DecorationDialogLookup::matchesCheckerRough, "Черновая"),
    CLEAN("CLEAN", DecorationDialogLookup::matchesCheckerClean, "Чистовая"),
    TURNKEY("TURNKEY", DecorationDialogLookup::matchesCheckerTurnkey, "Под ключ");

    val param: Pair<String, String>
        get() = "decoration" to value
}

enum class CommercialBuildingType(
    val value: String,
    val matcher: (CommercialBuildingTypeDialogLookup) -> Matcher<View>,
    val expected: String
) {

    BUSINESS_CENTER(
        "BUSINESS_CENTER",
        CommercialBuildingTypeDialogLookup::matchesSelectorBusinessCenter,
        "Бизнес-центр"
    ),
    WAREHOUSE(
        "WAREHOUSE",
        CommercialBuildingTypeDialogLookup::matchesSelectorWarehouse,
        "Складской комплекс"
    ),
    SHOPPING_CENTER(
        "SHOPPING_CENTER",
        CommercialBuildingTypeDialogLookup::matchesSelectorShoppingCenter,
        "Торговый центр"
    ),
    DETACHED_BUILDING(
        "DETACHED_BUILDING",
        CommercialBuildingTypeDialogLookup::matchesSelectorDetached,
        "Отдельно стоящее здание"
    ),
    RESIDENTIAL_BUILDING(
        "RESIDENTIAL_BUILDING",
        CommercialBuildingTypeDialogLookup::matchesSelectorResidential,
        "Встроенное помещение, жилой дом"
    );

    val param: Pair<String, String>
        get() = "commercialBuildingType" to value
}

enum class BusinessCenterClass(
    val value: String,
    val matcher: (BusinessCenterClassDialogLookup) -> Matcher<View>,
    val expected: String
) {

    A("A", BusinessCenterClassDialogLookup::matchesCheckerA, "A"),
    A_PLUS("A_PLUS", BusinessCenterClassDialogLookup::matchesCheckerAPlus, "A+"),
    B("B", BusinessCenterClassDialogLookup::matchesCheckerB, "B"),
    B_PLUS("B_PLUS", BusinessCenterClassDialogLookup::matchesCheckerBPlus, "B+"),
    C("C", BusinessCenterClassDialogLookup::matchesCheckerC, "C"),
    C_PLUS("C_PLUS", BusinessCenterClassDialogLookup::matchesCheckerCPlus, "C+");

    val param: Pair<String, String>
        get() = "officeClass" to value
}

enum class EntranceType(
    val value: String,
    val matcher: (EntranceTypeDialogLookup) -> Matcher<View>,
    val expected: String
) {

    SEPARATE("SEPARATE", EntranceTypeDialogLookup::matchesSelectorSeparate, "Отдельный"),
    COMMON("COMMON", EntranceTypeDialogLookup::matchesSelectorCommon, "Общий");

    val param: Pair<String, String>
        get() = "entranceType" to value
}

enum class CommercialDealStatus(
    val value: String,
    val matcher: (CommercialDealStatusDialogLookup) -> Matcher<View>,
    val expected: String
) {

    DIRECT_RENT(
        "DIRECT_RENT",
        CommercialDealStatusDialogLookup::matchesSelectorDirect,
        "Прямая аренда"
    ),
    SUBRENT("SUBRENT", CommercialDealStatusDialogLookup::matchesSelectorSubrent, "Субаренда"),
    SALE_OF_LEASE_RIGHTS(
        "SALE_OF_LEASE_RIGHTS",
        CommercialDealStatusDialogLookup::matchesSelectorSellOfLeaseRights,
        "Продажа права аренды"
    );

    val param: Pair<String, String>
        get() = "dealStatus" to value
}

enum class DealParams(
    val param: Pair<String, String>,
    val fieldMatcher: (FiltersLookup) -> Matcher<View>,
    val valueMatcher: (FiltersLookup) -> Matcher<View>,
    val expected: String
) {

    UTILITIES(
        "hasUtilitiesIncluded" to "YES",
        { FiltersLookup.matchesFieldUtilitiesIncluded() },
        { FiltersLookup.matchesUtilitiesIncludedValue() },
        "КУ включены"
    ),
    ELECTRICITY(
        "hasElectricityIncluded" to "YES",
        { FiltersLookup.matchesFieldElectricityIncluded() },
        { FiltersLookup.matchesElectricityIncludedValue() },
        "Электроэнергия включена"
    ),
    CLEANING(
        "hasCleaningIncluded" to "YES",
        { FiltersLookup.matchesFieldCleaningIncluded() },
        { FiltersLookup.matchesCleaningIncludedValue() },
        "Клининг включен"
    ),
    NDS(
        "taxationForm" to "NDS",
        { FiltersLookup.matchesFieldNds() },
        { FiltersLookup.matchesNdsValue() },
        "НДС"
    ),
    USN(
        "taxationForm" to "USN",
        { FiltersLookup.matchesFieldUsn() },
        { FiltersLookup.matchesUsnValue() },
        "УСН"
    );
}

enum class LandType(
    val value: String,
    val matcher: (VillageLandTypeDialogLookup) -> Matcher<View>,
    val expected: String
) {

    DNP("DNP", VillageLandTypeDialogLookup::matchesCheckerDnp, "ДНП"),
    IGS("IZHS", VillageLandTypeDialogLookup::matchesCheckerIgs, "ИЖС"),
    LPH("LPH", VillageLandTypeDialogLookup::matchesCheckerLph, "ЛПХ"),
    MGS("MZHS", VillageLandTypeDialogLookup::matchesCheckerMgs, "МЖС"),
    SNT("SNT", VillageLandTypeDialogLookup::matchesCheckerSnt, "СНТ");

    val param: Pair<String, String>
        get() = "landType" to value
}

enum class WallType(
    val value: String,
    val matcher: (WallTypeDialogLookup) -> Matcher<View>,
    val expected: String
) {

    WOOD("WOOD", WallTypeDialogLookup::matchesCheckerWood, "Дерево"),
    FRAME("FRAME", WallTypeDialogLookup::matchesCheckerFrame, "Каркасно-щитовые"),
    BRICK("BRICK", WallTypeDialogLookup::matchesCheckerBrick, "Кирпич"),
    TIMBER_FRAMING("TIMBER_FRAMING", WallTypeDialogLookup::matchesCheckerTimberFraming, "Фахверк"),
    CONCRET("CONCRET", WallTypeDialogLookup::matchesCheckerConcrete, "Бетон");

    val param: Pair<String, String>
        get() = "wallsType" to value
}

private val now = com.yandex.mobile.realty.domain.model.common.QuarterOfYear.now

enum class CommissioningState(
    val value: String,
    val matcher: (CommissioningStateDialogLookup) -> Matcher<View>,
    val expected: String
) {

    FINISHED("FINISHED", CommissioningStateDialogLookup::matchesSelectorFinished, "Сдан"),
    BEING_BUILT(
        "${now.quarter.value}_${now.year}",
        { it.matchesSelectorBeingBuilt(now.quarter.value, now.year) },
        "до ${now.quarter.value} квартала ${now.year}"
    );

    val param: Pair<String, String>
        get() = "deliveryDate" to value
}

enum class CommuteTransport(
    val value: String,
    val matcher: (CommuteParamsLookup) -> Matcher<View>,
    val expected: String
) {

    BY_FOOT("BY_FOOT", CommuteParamsLookup::matchesByFootTransportButton, "пешком"),
    AUTO("AUTO", CommuteParamsLookup::matchesAutoTransportButton, "на машине"),
    PUBLIC("PUBLIC", CommuteParamsLookup::matchesPublicTransportButton, "на транспорте");

    val param: Pair<String, String>
        get() = "commuteTransport" to value
}

enum class CommuteTime(
    val value: String,
    val matcher: (CommuteParamsLookup) -> Matcher<View>,
    val expected: String
) {

    TEN_MIN("10", CommuteParamsLookup::matchesTenMinTimeButton, "10"),
    FIFTEEN_MIN("15", CommuteParamsLookup::matchesFifteenMinTimeButton, "15"),
    TWENTY_MIN("20", CommuteParamsLookup::matchesTwentyMinTimeButton, "20"),
    THIRTY_MIN("30", CommuteParamsLookup::matchesThirtyMinTimeButton, "30"),
    FORTY_FIVE_MIN("45", CommuteParamsLookup::matchesFortyFiveMinTimeButton, "45");

    val param: Pair<String, String>
        get() = "commuteTime" to value
}

enum class FirstFloorRestriction(
    val params: Array<Pair<String, String>>,
    val matcher: (FirstFloorDialogLookup) -> Matcher<View>,
    val expected: String
) {

    ONLY_FIRST(
        arrayOf("floorMin" to "1", "floorMax" to "1"),
        FirstFloorDialogLookup::matchesSelectorOnlyFirst,
        "Первый"
    ),
    ABOVE_FIRST(
        arrayOf("floorMin" to "2"),
        FirstFloorDialogLookup::matchesSelectorAboveFirst,
        "Выше первого"
    );
}

enum class CommercialRenovation(
    val value: String,
    val matcher: (CommercialRenovationDialogLookup) -> Matcher<View>,
    val expected: String
) {

    NEEDS_RENOVATION(
        "NEEDS_RENOVATION",
        CommercialRenovationDialogLookup::matchesSelectorNeedsRenovation,
        "Требуется ремонт"
    ),
    COSMETIC_DONE(
        "COSMETIC_DONE",
        CommercialRenovationDialogLookup::matchesSelectorCosmeticDonen,
        "Готовый ремонт"
    ),
    DESIGNER_RENOVATION(
        "DESIGNER_RENOVATION",
        CommercialRenovationDialogLookup::matchesSelectorDesignerRenovation,
        "Дизайнерский"
    );

    val param: Pair<String, String>
        get() = "renovation" to value

    companion object {

        val types: EnumSet<CommercialRenovation> by lazy {
            EnumSet.of(
                NEEDS_RENOVATION,
                COSMETIC_DONE,
                DESIGNER_RENOVATION
            )
        }
    }
}

enum class CommercialPlanType(
    val value: String,
    val matcher: (CommercialPlanTypeDialogLookup) -> Matcher<View>,
    val expected: String
) {

    OPEN_SPACE(
        "OPEN_SPACE",
        CommercialPlanTypeDialogLookup::matchesSelectorOpenSpace,
        "Open space"
    ),
    CABINET(
        "CABINET",
        CommercialPlanTypeDialogLookup::matchesSelectorCabinet,
        "Кабинетная"
    ),
    CORRIDOR(
        "CORRIDOR",
        CommercialPlanTypeDialogLookup::matchesSelectorCorridor,
        "Коридорная"
    );

    val param: Pair<String, String>
        get() = "commercialPlanType" to value
}

enum class Commission(
    val param: Pair<String, String>,
    val matcher: (CommissionDialogLookup) -> Matcher<View>,
    val expected: String
) {

    WITHOUT_COMMISSION(
        "commissionMax" to "0",
        CommissionDialogLookup::matchesSelectorWithoutCommission,
        "Без комиссии"
    ),
    WITH_COMMISSION(
        "commissionMin" to "1",
        CommissionDialogLookup::matchesSelectorWithCommission,
        "С комиссией"
    )
}

enum class HouseMaterialType(
    val value: String,
    val matcher: () -> NamedStringMatcher,
    val expected: String
) {

    BRICK("BRICK", HouseMaterialTypeDialogLookup::matchesBrick, "Кирпичный"),
    MONOLIT("MONOLIT", HouseMaterialTypeDialogLookup::matchesMonolit, "Монолитный"),
    MONOLIT_BRICK(
        "MONOLIT_BRICK",
        HouseMaterialTypeDialogLookup::matchesMonolitBrick,
        "Кирпично-монолитный"
    ),
    PANEL("PANEL", HouseMaterialTypeDialogLookup::matchesPanel, "Панельный"),
    WOOD("WOOD", HouseMaterialTypeDialogLookup::matchesWood, "Деревянный"),
    BLOCK("BLOCK", HouseMaterialTypeDialogLookup::matchesBlock, "Блочный"),
    FERROCONCRETE(
        "FERROCONCRETE",
        HouseMaterialTypeDialogLookup::matchesFerroConcrete,
        "Железобетонный"
    );

    val param: Pair<String, String>
        get() = "buildingType" to value
}

enum class DealStatus(
    val value: String,
    val matcher: () -> NamedStringMatcher,
    val expected: String,
) {

    COUNTERSALE("COUNTERSALE", DealStatusDialogLookup::matchesCountersale, "Обмен"),
    REASSIGNMENT("REASSIGNMENT", DealStatusDialogLookup::matchesReassignment, "Переуступка");

    val param: Pair<String, String>
        get() = "dealStatus" to value
}
