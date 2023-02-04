package com.yandex.mobile.realty.network.model.mapping

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.yandex.mobile.realty.data.mapping.EmptyDescriptor
import com.yandex.mobile.realty.data.model.publication.InvalidUserOfferException
import com.yandex.mobile.realty.data.model.publication.InvalidUserOfferFieldValueException
import com.yandex.mobile.realty.data.model.publication.UserOfferDataDtoConverter
import com.yandex.mobile.realty.domain.model.publication.UserOfferData
import com.yandex.mobile.realty.domain.model.publication.UserOfferData.RoomsCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/**
 * @author andrey-bgm on 17/02/2020.
 */
class UserOfferDataDtoConverterTest {

    private val converter = UserOfferDataDtoConverter(Gson())

    @Test
    fun convertSellApartment() {
        val json = readResourceJson("userOfferSellApartment.json")

        val userOffer: UserOfferData.SellApartment = convert(json)

        assertEquals("Россия, Санкт-Петербург, Невский проспект, 7", userOffer.location?.address)
        assertEquals(59.96422958, userOffer.location?.point?.latitude)
        assertEquals(30.40716362, userOffer.location?.point?.longitude)
        assertEquals(225, userOffer.location?.country)
        assertEquals(417_899L, userOffer.location?.rgid)

        assertEquals(2, userOffer.images?.size)
        assertEquals(true, userOffer.imagesOrderChangeAllowed)
        assertEquals(4, userOffer.floor)
        assertEquals(9, userOffer.floorsCount)
        assertEquals(RoomsCount.TWO, userOffer.roomsCount)
        assertEquals(100.2, userOffer.area)
        assertEquals(10.1, userOffer.kitchenArea)
        assertEquals(70.1, userOffer.livingArea)
        assertEquals(listOf(29.4, 40.7), userOffer.roomsArea)
        assertEquals(2.8, userOffer.ceilingHeight)
        assertEquals(UserOfferData.Bathroom.SEPARATED, userOffer.bathroom)
        assertEquals(UserOfferData.Balcony.BALCONY, userOffer.balcony)
        assertEquals(
            listOf(UserOfferData.WindowView.YARD, UserOfferData.WindowView.STREET),
            userOffer.windowView
        )
        assertEquals(UserOfferData.Renovation.COSMETIC_DONE, userOffer.renovation)
        assertEquals(UserOfferData.ParkingType.OPEN, userOffer.parkingType)
        assertEquals("63", userOffer.apartmentNumber)
        assertEquals(1974, userOffer.builtYear)
        assertEquals(true, userOffer.lift)
        assertEquals(true, userOffer.rubbishChute)
        assertEquals(false, userOffer.concierge)
        assertEquals(true, userOffer.closedArea)
        assertEquals(false, userOffer.internet)
        assertEquals(true, userOffer.refrigerator)
        assertEquals(false, userOffer.roomFurniture)
        assertEquals(true, userOffer.kitchenFurniture)
        assertEquals(false, userOffer.airConditioner)
        assertEquals(UserOfferData.BuildingType.BRICK, userOffer.buildingType)
        assertEquals(true, userOffer.apartments)
        assertEquals(false, userOffer.newFlat)
        assertEquals("nice apartment", userOffer.description)

        val deal = userOffer.deal

        assertEquals(65_000L, deal.priceValue)
        assertEquals(UserOfferData.Currency.USD, deal.currency)
        assertEquals(true, deal.haggle)
        assertEquals(true, userOffer.mortgage)
        assertEquals(UserOfferData.DealStatus.SALE, userOffer.dealStatus)

        assertEquals("https://youtu.be/2Zw859-PqSM", userOffer.videoUrl)
        assertEquals(true, userOffer.onlineShow)
    }

    @Test
    fun convertRentApartment() {
        val json = readResourceJson("userOfferRentApartment.json")

        val userOffer: UserOfferData.RentApartment = convert(json)

        assertEquals("Россия, Санкт-Петербург, проспект Ветеранов, 3", userOffer.location?.address)
        assertEquals(59.96422958, userOffer.location?.point?.latitude)
        assertEquals(30.40716362, userOffer.location?.point?.longitude)
        assertEquals(225, userOffer.location?.country)
        assertEquals(417_899L, userOffer.location?.rgid)

        assertEquals(1, userOffer.images?.size)
        assertEquals(true, userOffer.imagesOrderChangeAllowed)
        assertEquals(7, userOffer.floor)
        assertEquals(9, userOffer.floorsCount)
        assertEquals(RoomsCount.TWO, userOffer.roomsCount)
        assertEquals(60.0, userOffer.area)
        assertEquals(10.0, userOffer.kitchenArea)
        assertEquals(45.0, userOffer.livingArea)
        assertEquals(listOf(15.0, 20.0), userOffer.roomsArea)
        assertEquals(3.0, userOffer.ceilingHeight)
        assertEquals(UserOfferData.Bathroom.MATCHED, userOffer.bathroom)
        assertEquals(UserOfferData.Balcony.BALCONY_LOGGIA, userOffer.balcony)
        assertEquals(listOf(UserOfferData.WindowView.YARD), userOffer.windowView)
        assertEquals(UserOfferData.Renovation.DESIGNER_RENOVATION, userOffer.renovation)
        assertEquals(UserOfferData.ParkingType.CLOSED, userOffer.parkingType)
        assertEquals("42", userOffer.apartmentNumber)
        assertEquals(1974, userOffer.builtYear)
        assertEquals(false, userOffer.lift)
        assertEquals(false, userOffer.rubbishChute)
        assertEquals(true, userOffer.concierge)
        assertEquals(false, userOffer.closedArea)
        assertEquals(true, userOffer.internet)
        assertEquals(true, userOffer.refrigerator)
        assertEquals(true, userOffer.roomFurniture)
        assertEquals(true, userOffer.kitchenFurniture)
        assertEquals(false, userOffer.airConditioner)
        assertEquals(false, userOffer.television)
        assertEquals(true, userOffer.withPets)
        assertEquals(true, userOffer.withChildren)
        assertEquals(false, userOffer.dishwasher)
        assertEquals(false, userOffer.washingMachine)
        assertEquals("nice apartment", userOffer.description)

        val deal = userOffer.deal

        assertEquals(40_000L, deal.priceValue)
        assertEquals(UserOfferData.Currency.RUR, deal.currency)
        assertEquals(false, deal.haggle)
        assertEquals(UserOfferData.Period.PER_MONTH, deal.period)
        assertEquals(true, deal.rentPledge)
        assertEquals(true, deal.utilitiesIncluded)
        assertEquals(100, deal.prepayment)
        assertEquals(50, deal.agentFee)

        assertEquals("https://youtu.be/2Zw859-PqSM", userOffer.videoUrl)
        assertEquals(true, userOffer.onlineShow)
    }

    @Test
    fun convertSellRoom() {
        val json = readResourceJson("userOfferSellRoom.json")

        val userOffer: UserOfferData.SellRoom = convert(json)

        assertEquals("Россия, Санкт-Петербург, Камышовая улица, 13", userOffer.location?.address)
        assertEquals(59.96422958, userOffer.location?.point?.latitude)
        assertEquals(30.40716362, userOffer.location?.point?.longitude)
        assertEquals(225, userOffer.location?.country)
        assertEquals(417_899L, userOffer.location?.rgid)

        assertEquals(1, userOffer.images?.size)
        assertEquals(true, userOffer.imagesOrderChangeAllowed)
        assertEquals(3, userOffer.floor)
        assertEquals(7, userOffer.floorsCount)
        assertEquals(RoomsCount.TWO, userOffer.roomsCount)
        assertEquals(RoomsCount.ONE, userOffer.roomsOffered)
        assertEquals(50.0, userOffer.apartmentArea)
        assertEquals(11.5, userOffer.kitchenArea)
        assertEquals(listOf(20.0), userOffer.roomsArea)
        assertEquals(3.1, userOffer.ceilingHeight)
        assertEquals(UserOfferData.Bathroom.SEPARATED, userOffer.bathroom)
        assertEquals(UserOfferData.Balcony.LOGGIA, userOffer.balcony)
        assertEquals(listOf(UserOfferData.WindowView.STREET), userOffer.windowView)
        assertEquals(UserOfferData.FloorCovering.GLAZED, userOffer.floorCovering)
        assertEquals(UserOfferData.Renovation.COSMETIC_DONE, userOffer.renovation)
        assertEquals(UserOfferData.ParkingType.OPEN, userOffer.parkingType)
        assertEquals("44", userOffer.apartmentNumber)
        assertEquals(1990, userOffer.builtYear)
        assertEquals(true, userOffer.lift)
        assertEquals(false, userOffer.rubbishChute)
        assertEquals(false, userOffer.concierge)
        assertEquals(true, userOffer.closedArea)
        assertEquals(true, userOffer.internet)
        assertEquals(true, userOffer.roomFurniture)
        assertEquals(false, userOffer.kitchenFurniture)
        assertEquals(UserOfferData.BuildingType.MONOLIT, userOffer.buildingType)
        assertEquals(true, userOffer.apartments)
        assertEquals("nice room", userOffer.description)

        val deal = userOffer.deal

        assertEquals(1_800_000L, deal.priceValue)
        assertEquals(UserOfferData.Currency.RUR, deal.currency)
        assertEquals(false, deal.haggle)
        assertEquals(true, userOffer.mortgage)
        assertEquals(UserOfferData.DealStatus.COUNTERSALE, userOffer.dealStatus)

        assertEquals("https://youtu.be/2Zw859-PqSM", userOffer.videoUrl)
        assertEquals(true, userOffer.onlineShow)
    }

    @Test
    fun convertRentRoom() {
        val json = readResourceJson("userOfferRentRoom.json")

        val userOffer: UserOfferData.RentRoom = convert(json)

        assertEquals("Россия, Санкт-Петербург, улица Рериха, 4", userOffer.location?.address)
        assertEquals(59.96422958, userOffer.location?.point?.latitude)
        assertEquals(30.40716362, userOffer.location?.point?.longitude)
        assertEquals(225, userOffer.location?.country)
        assertEquals(417_899L, userOffer.location?.rgid)

        assertEquals(1, userOffer.images?.size)
        assertEquals(true, userOffer.imagesOrderChangeAllowed)
        assertEquals(6, userOffer.floor)
        assertEquals(12, userOffer.floorsCount)
        assertEquals(RoomsCount.THREE, userOffer.roomsCount)
        assertEquals(RoomsCount.TWO, userOffer.roomsOffered)
        assertEquals(60.0, userOffer.apartmentArea)
        assertEquals(8.0, userOffer.kitchenArea)
        assertEquals(listOf(15.0, 20.0), userOffer.roomsArea)
        assertEquals(3.0, userOffer.ceilingHeight)
        assertEquals(UserOfferData.Bathroom.MATCHED, userOffer.bathroom)
        assertEquals(UserOfferData.Balcony.NONE, userOffer.balcony)
        assertEquals(listOf(UserOfferData.WindowView.YARD), userOffer.windowView)
        assertEquals(UserOfferData.FloorCovering.LINOLEUM, userOffer.floorCovering)
        assertEquals(UserOfferData.Renovation.NEEDS_RENOVATION, userOffer.renovation)
        assertEquals(UserOfferData.ParkingType.UNDERGROUND, userOffer.parkingType)
        assertEquals("7", userOffer.apartmentNumber)
        assertEquals(1962, userOffer.builtYear)
        assertEquals(true, userOffer.lift)
        assertEquals(false, userOffer.rubbishChute)
        assertEquals(true, userOffer.concierge)
        assertEquals(false, userOffer.closedArea)
        assertEquals(false, userOffer.internet)
        assertEquals(true, userOffer.refrigerator)
        assertEquals(true, userOffer.roomFurniture)
        assertEquals(true, userOffer.kitchenFurniture)
        assertEquals(false, userOffer.airConditioner)
        assertEquals(false, userOffer.television)
        assertEquals(true, userOffer.withPets)
        assertEquals(false, userOffer.withChildren)
        assertEquals(true, userOffer.dishwasher)
        assertEquals(false, userOffer.washingMachine)
        assertEquals("awesome room", userOffer.description)

        val deal = userOffer.deal

        assertEquals(3000L, deal.priceValue)
        assertEquals(UserOfferData.Currency.RUR, deal.currency)
        assertEquals(false, deal.haggle)
        assertEquals(UserOfferData.Period.PER_DAY, deal.period)
        assertEquals(true, deal.rentPledge)
        assertEquals(true, deal.utilitiesIncluded)
        assertEquals(50, deal.prepayment)
        assertEquals(70, deal.agentFee)

        assertEquals("https://youtu.be/2Zw859-PqSM", userOffer.videoUrl)
        assertEquals(true, userOffer.onlineShow)
    }

    @Test
    fun convertSellHouse() {
        val json = readResourceJson("userOfferSellHouse.json")

        val userOffer: UserOfferData.SellHouse = convert(json)

        assertEquals("Россия, Санкт-Петербург, Главная улица, 2", userOffer.location?.address)
        assertEquals(59.96422958, userOffer.location?.point?.latitude)
        assertEquals(30.40716362, userOffer.location?.point?.longitude)
        assertEquals(225, userOffer.location?.country)
        assertEquals(417_899L, userOffer.location?.rgid)

        assertEquals(1, userOffer.images?.size)
        assertEquals(true, userOffer.imagesOrderChangeAllowed)
        assertEquals("cozy house", userOffer.description)
        assertEquals(6.0, userOffer.lotArea)
        assertEquals(UserOfferData.AreaUnit.ARE, userOffer.lotAreaUnit)
        assertEquals(UserOfferData.LotType.GARDEN, userOffer.lotType)
        assertEquals(UserOfferData.HouseType.TOWNHOUSE, userOffer.houseType)
        assertEquals(150.0, userOffer.houseArea)
        assertEquals(2, userOffer.floorsCount)
        assertEquals(UserOfferData.HousePlace.INSIDE, userOffer.shower)
        assertEquals(UserOfferData.HousePlace.OUTSIDE, userOffer.toilet)
        assertEquals(true, userOffer.pmg)
        assertEquals(true, userOffer.kitchen)
        assertEquals(true, userOffer.waterSupply)
        assertEquals(false, userOffer.heatingSupply)
        assertEquals(true, userOffer.sewerageSupply)
        assertEquals(true, userOffer.electricitySupply)
        assertEquals(true, userOffer.gasSupply)
        assertEquals(false, userOffer.billiard)
        assertEquals(false, userOffer.sauna)
        assertEquals(true, userOffer.pool)

        val deal = userOffer.deal

        assertEquals(60_000L, deal.priceValue)
        assertEquals(UserOfferData.Currency.EUR, deal.currency)
        assertEquals(true, deal.haggle)
        assertEquals(false, userOffer.mortgage)
        assertEquals(UserOfferData.DealStatus.SALE, userOffer.dealStatus)

        assertEquals("https://youtu.be/2Zw859-PqSM", userOffer.videoUrl)
        assertEquals(true, userOffer.onlineShow)
    }

    @Test
    fun convertRentHouse() {
        val json = readResourceJson("userOfferRentHouse.json")

        val userOffer: UserOfferData.RentHouse = convert(json)

        assertEquals("Россия, Санкт-Петербург, Главная улица, 6", userOffer.location?.address)
        assertEquals(59.96422958, userOffer.location?.point?.latitude)
        assertEquals(30.40716362, userOffer.location?.point?.longitude)
        assertEquals(225, userOffer.location?.country)
        assertEquals(417_899L, userOffer.location?.rgid)

        assertEquals(1, userOffer.images?.size)
        assertEquals(true, userOffer.imagesOrderChangeAllowed)
        assertEquals("nice house", userOffer.description)
        assertEquals(3.0, userOffer.lotArea)
        assertEquals(UserOfferData.AreaUnit.HECTARE, userOffer.lotAreaUnit)
        assertEquals(UserOfferData.LotType.FARM, userOffer.lotType)
        assertEquals(UserOfferData.HouseType.HOUSE, userOffer.houseType)
        assertEquals(300.0, userOffer.houseArea)
        assertEquals(3, userOffer.floorsCount)
        assertEquals(UserOfferData.HousePlace.INSIDE, userOffer.shower)
        assertEquals(UserOfferData.HousePlace.INSIDE, userOffer.toilet)
        assertEquals(true, userOffer.pmg)
        assertEquals(true, userOffer.kitchen)
        assertEquals(true, userOffer.waterSupply)
        assertEquals(true, userOffer.heatingSupply)
        assertEquals(true, userOffer.sewerageSupply)
        assertEquals(false, userOffer.electricitySupply)
        assertEquals(false, userOffer.gasSupply)
        assertEquals(true, userOffer.billiard)
        assertEquals(true, userOffer.sauna)
        assertEquals(false, userOffer.pool)

        val deal = userOffer.deal

        assertEquals(6000L, deal.priceValue)
        assertEquals(UserOfferData.Currency.RUR, deal.currency)
        assertEquals(false, deal.haggle)
        assertEquals(UserOfferData.Period.PER_DAY, deal.period)
        assertEquals(true, deal.rentPledge)
        assertEquals(false, deal.utilitiesIncluded)
        assertEquals(80, deal.prepayment)
        assertEquals(50, deal.agentFee)

        assertEquals("https://youtu.be/2Zw859-PqSM", userOffer.videoUrl)
        assertEquals(true, userOffer.onlineShow)
    }

    @Test
    fun convertSellLot() {
        val json = readResourceJson("userOfferSellLot.json")

        val userOffer: UserOfferData.SellLot = convert(json)

        assertEquals("Россия, Санкт-Петербург, Вербная улица, 9", userOffer.location?.address)
        assertEquals(59.96422958, userOffer.location?.point?.latitude)
        assertEquals(30.40716362, userOffer.location?.point?.longitude)
        assertEquals(225, userOffer.location?.country)
        assertEquals(417_899L, userOffer.location?.rgid)

        assertEquals(1, userOffer.images?.size)
        assertEquals(true, userOffer.imagesOrderChangeAllowed)
        assertEquals("nice lot", userOffer.description)
        assertEquals(5.0, userOffer.area)
        assertEquals(UserOfferData.AreaUnit.ARE, userOffer.areaUnit)
        assertEquals(UserOfferData.LotType.IGS, userOffer.lotType)

        val deal = userOffer.deal

        assertEquals(5_000_000L, deal.priceValue)
        assertEquals(UserOfferData.Currency.RUR, deal.currency)
        assertEquals(true, deal.haggle)
        assertEquals(false, userOffer.mortgage)
        assertEquals(UserOfferData.DealStatus.COUNTERSALE, userOffer.dealStatus)

        assertEquals("https://youtu.be/2Zw859-PqSM", userOffer.videoUrl)
        assertEquals(true, userOffer.onlineShow)
    }

    @Test
    fun convertSellGarage() {
        val json = readResourceJson("userOfferSellGarage.json")

        val userOffer: UserOfferData.SellGarage = convert(json)

        assertEquals("Коломяжский проспект, 10И", userOffer.location?.address)
        assertEquals(59.99449921, userOffer.location?.point?.latitude)
        assertEquals(30.29294968, userOffer.location?.point?.longitude)
        assertEquals(225, userOffer.location?.country)
        assertEquals(417_899L, userOffer.location?.rgid)

        assertEquals(1, userOffer.images?.size)
        assertEquals(true, userOffer.imagesOrderChangeAllowed)
        assertEquals("nice garage", userOffer.description)
        assertEquals(UserOfferData.GarageType.PARKING_PLACE, userOffer.garageType)
        assertEquals(UserOfferData.GarageParkingType.MULTILEVEL, userOffer.parkingType)
        assertEquals(UserOfferData.GarageBuildingType.METAL, userOffer.buildingType)
        assertEquals(UserOfferData.GarageOwnership.BY_PROXY, userOffer.ownership)
        assertEquals(15.6, userOffer.area)
        assertEquals("ГСК Запад", userOffer.garageName)

        val deal = userOffer.deal

        assertEquals(500_000L, deal.priceValue)
        assertEquals(UserOfferData.Currency.RUR, deal.currency)
        assertEquals(true, deal.haggle)

        assertEquals("https://youtu.be/2Zw859-PqSM", userOffer.videoUrl)
        assertEquals(true, userOffer.onlineShow)

        assertEquals(true, userOffer.accessControlSystem)
        assertEquals(false, userOffer.autoRepair)
        assertEquals(true, userOffer.automaticGates)
        assertEquals(true, userOffer.carWash)
        assertEquals(false, userOffer.cctv)
        assertEquals(true, userOffer.electricitySupply)
        assertEquals(true, userOffer.fireAlarm)
        assertEquals(false, userOffer.inspectionPit)
        assertEquals(true, userOffer.heatingSupply)
        assertEquals(true, userOffer.security)
        assertEquals(true, userOffer.twentyFourSeven)
        assertEquals(true, userOffer.waterSupply)
    }

    @Test
    fun convertRentGarage() {
        val json = readResourceJson("userOfferRentGarage.json")

        val userOffer: UserOfferData.RentGarage = convert(json)

        assertEquals("Ярославский проспект, 16", userOffer.location?.address)
        assertEquals(60.01329041, userOffer.location?.point?.latitude)
        assertEquals(30.32044601, userOffer.location?.point?.longitude)
        assertEquals(225, userOffer.location?.country)
        assertEquals(417_899L, userOffer.location?.rgid)

        assertEquals(1, userOffer.images?.size)
        assertEquals(false, userOffer.imagesOrderChangeAllowed)
        assertEquals("nice garage", userOffer.description)
        assertEquals(UserOfferData.GarageType.BOX, userOffer.garageType)
        assertEquals(UserOfferData.GarageParkingType.UNDERGROUND, userOffer.parkingType)
        assertEquals(UserOfferData.GarageBuildingType.FERROCONCRETE, userOffer.buildingType)
        assertEquals(UserOfferData.GarageOwnership.COOPERATIVE, userOffer.ownership)
        assertEquals(11.4, userOffer.area)
        assertEquals("ГСК Север", userOffer.garageName)

        val deal = userOffer.deal

        assertEquals(12_000L, deal.priceValue)
        assertEquals(UserOfferData.Currency.RUR, deal.currency)
        assertEquals(false, deal.haggle)
        assertEquals(UserOfferData.Period.PER_MONTH, deal.period)
        assertEquals(true, deal.rentPledge)
        assertEquals(false, deal.utilitiesIncluded)
        assertEquals(true, userOffer.electricityIncluded)
        assertEquals(100, deal.prepayment)
        assertEquals(40, deal.agentFee)

        assertEquals("https://youtu.be/UNvPkozDJGI", userOffer.videoUrl)
        assertEquals(true, userOffer.onlineShow)

        assertEquals(true, userOffer.accessControlSystem)
        assertEquals(true, userOffer.autoRepair)
        assertEquals(false, userOffer.automaticGates)
        assertEquals(true, userOffer.carWash)
        assertEquals(true, userOffer.cctv)
        assertEquals(true, userOffer.electricitySupply)
        assertEquals(true, userOffer.fireAlarm)
        assertEquals(true, userOffer.inspectionPit)
        assertEquals(false, userOffer.heatingSupply)
        assertEquals(true, userOffer.security)
        assertEquals(false, userOffer.twentyFourSeven)
        assertEquals(true, userOffer.waterSupply)
    }

    @Test
    fun convertSellApartmentWhenEmptyFloors() {
        val json = JsonObject().apply {
            addProperty("type", "SELL")
            addProperty("category", "APARTMENT")
            add("floors", JsonArray())
        }

        val userOffer: UserOfferData.SellApartment = convert(json)

        assertNull(userOffer.floor)
    }

    @Test
    fun convertParkingTypeNoneToOpen() {
        val json = JsonObject().apply {
            addProperty("type", "SELL")
            addProperty("category", "APARTMENT")
            addProperty("parkingType", "NONE")
        }

        val userOffer: UserOfferData.SellApartment = convert(json)

        assertEquals(UserOfferData.ParkingType.OPEN, userOffer.parkingType)
    }

    @Test
    fun convertZeroRentDealValues() {
        val json = JsonObject().apply {
            addProperty("type", "RENT")
            addProperty("category", "APARTMENT")
            addProperty("period", "PER_MONTH")
            addProperty("agentFee", 0)
            addProperty("prepayment", 0)
        }

        val userOffer: UserOfferData.RentApartment = convert(json)

        assertEquals(0, userOffer.deal.agentFee)
        assertEquals(0, userOffer.deal.prepayment)
    }

    @Test(expected = InvalidUserOfferFieldValueException::class)
    fun shouldThrowWhenNoTypeAndCategory() {
        converter.map(JsonObject(), EmptyDescriptor)
    }

    @Test(expected = InvalidUserOfferFieldValueException::class)
    fun shouldThrowWhenRentLot() {
        val json = JsonObject().apply {
            addProperty("type", "RENT")
            addProperty("category", "LOT")
        }

        converter.map(json, EmptyDescriptor)
    }

    @Test(expected = InvalidUserOfferFieldValueException::class)
    fun shouldThrowWhenInvalidFieldValue() {
        val json = JsonObject().apply {
            addProperty("type", "SELL")
            addProperty("category", "APARTMENT")
            addProperty("location", 1)
        }

        converter.map(json, EmptyDescriptor)
    }

    @Test(expected = InvalidUserOfferException::class)
    fun shouldThrowWhenUnknownField() {
        val json = JsonObject().apply {
            addProperty("type", "SELL")
            addProperty("category", "APARTMENT")
            addProperty("UNKNOWN_BOOL_FIELD", false)
        }

        converter.map(json, EmptyDescriptor)
    }

    @Test
    fun shouldConvertSellApartmentWhenIgnoredFields() {
        val json = JsonObject().apply {
            addProperty("type", "SELL")
            addProperty("category", "APARTMENT")
            addProperty("cadastralNumber", "number")
            addProperty("siteId", "id")
            addProperty("disableToEdit", "value")
            addProperty("floorCovering", "value")
            addProperty("phone", "value")
            addProperty("buildInTech", "value")
            addProperty("curatedFlatPlan", "value")
            addProperty("virtualTours", "value")
            addProperty("minRentPeriod", "value")
            addProperty("prematureRentTerminationPenalty", "value")
        }
        convert<UserOfferData.SellApartment>(json)
    }

    @Test
    fun shouldConvertRentApartmentWhenIgnoredFields() {
        val json = JsonObject().apply {
            addProperty("type", "RENT")
            addProperty("category", "APARTMENT")
            addProperty("period", "PER_MONTH")
            addProperty("cadastralNumber", "number")
            addProperty("siteId", "id")
            addProperty("disableToEdit", "value")
            addProperty("flatAlarm", "value")
            addProperty("floorCovering", "value")
            addProperty("phone", "value")
            addProperty("buildInTech", "value")
            addProperty("curatedFlatPlan", "value")
            addProperty("virtualTours", "value")
            addProperty("rentDeposit", "value")
            addProperty("utilitiesFee", "value")
            addProperty("minRentPeriod", "value")
            addProperty("prematureRentTerminationPenalty", "value")
        }
        convert<UserOfferData.RentApartment>(json)
    }

    @Test
    fun shouldConvertSellRoomWhenIgnoredFields() {
        val json = JsonObject().apply {
            addProperty("type", "SELL")
            addProperty("category", "ROOMS")
            addProperty("cadastralNumber", "number")
            addProperty("siteId", "id")
            addProperty("disableToEdit", "value")
            addProperty("phone", "value")
            addProperty("buildInTech", "value")
            addProperty("curatedFlatPlan", "value")
            addProperty("virtualTours", "value")
            addProperty("minRentPeriod", "value")
            addProperty("prematureRentTerminationPenalty", "value")
        }
        convert<UserOfferData.SellRoom>(json)
    }

    @Test
    fun shouldConvertRentRoomWhenIgnoredFields() {
        val json = JsonObject().apply {
            addProperty("type", "RENT")
            addProperty("category", "ROOMS")
            addProperty("period", "PER_MONTH")
            addProperty("cadastralNumber", "number")
            addProperty("siteId", "id")
            addProperty("disableToEdit", "value")
            addProperty("phone", "value")
            addProperty("buildInTech", "value")
            addProperty("curatedFlatPlan", "value")
            addProperty("flatAlarm", "value")
            addProperty("virtualTours", "value")
            addProperty("minRentPeriod", "value")
            addProperty("prematureRentTerminationPenalty", "value")
        }
        convert<UserOfferData.RentRoom>(json)
    }

    @Test
    fun shouldConvertSellHouseWhenIgnoredFields() {
        val json = JsonObject().apply {
            addProperty("type", "SELL")
            addProperty("category", "HOUSE")
            addProperty("cadastralNumber", "number")
            addProperty("siteId", "id")
            addProperty("disableToEdit", "value")
            addProperty("virtualTours", "value")
            addProperty("minRentPeriod", "value")
            addProperty("prematureRentTerminationPenalty", "value")
        }
        convert<UserOfferData.SellHouse>(json)
    }

    @Test
    fun shouldConvertRentHouseWhenIgnoredFields() {
        val json = JsonObject().apply {
            addProperty("type", "RENT")
            addProperty("category", "HOUSE")
            addProperty("period", "PER_MONTH")
            addProperty("cadastralNumber", "number")
            addProperty("siteId", "id")
            addProperty("disableToEdit", "value")
            addProperty("virtualTours", "value")
            addProperty("minRentPeriod", "value")
            addProperty("prematureRentTerminationPenalty", "value")
        }
        convert<UserOfferData.RentHouse>(json)
    }

    @Test
    fun shouldConvertSellLotWhenIgnoredFields() {
        val json = JsonObject().apply {
            addProperty("type", "SELL")
            addProperty("category", "LOT")
            addProperty("cadastralNumber", "number")
            addProperty("siteId", "id")
            addProperty("disableToEdit", "value")
            addProperty("virtualTours", "value")
            addProperty("minRentPeriod", "value")
            addProperty("prematureRentTerminationPenalty", "value")
        }
        convert<UserOfferData.SellLot>(json)
    }

    private inline fun <reified T : UserOfferData> convert(json: JsonObject): T {
        return converter.map(json, EmptyDescriptor) as T
    }

    private fun readResourceJson(fileName: String): JsonObject {
        val path =
            javaClass.classLoader?.getResource(fileName)?.path
                ?: throw IllegalArgumentException("file not found: $fileName")
        val jsonText = File(path).readText()
        val jsonResponse = Gson().fromJson(jsonText, JsonObject::class.java)

        return jsonResponse["response"] as JsonObject
    }
}
