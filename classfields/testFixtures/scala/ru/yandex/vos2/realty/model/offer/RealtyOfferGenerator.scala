package ru.yandex.vos2.realty.model.offer

import org.scalacheck.Gen
import ru.yandex.vos2.OfferModel._
import ru.yandex.vertis.vos2.model.realty.RealtyOffer._
import ru.yandex.vertis.vos2.model.realty._
import ru.yandex.vos2.UserModel.User
import ru.yandex.vos2.model.BasicGenerator._
import ru.yandex.vos2.model.CommonGen._
import ru.yandex.vos2.model.offer.OfferGenerator
import ru.yandex.vos2.model.user.UserGenerator
import ru.yandex.vos2.realty.components.TestRealtyCoreComponents

/**
  * @author Leonid Nalchadzhi (nalchadzhi@yandex-team.ru)
  */
object RealtyOfferGenerator {

  val idManager = TestRealtyCoreComponents.offerIdManager

  val FacilitiesGen = for {
    phone ← Gen.oneOf(true, false)
    television ← Gen.oneOf(true, false)
    internet ← Gen.oneOf(true, false)
    washingMachine ← Gen.oneOf(true, false)
    refrigerator ← Gen.oneOf(true, false)
    furnitureRoom ← Gen.oneOf(true, false)
    furnitureKitchen ← Gen.oneOf(true, false)
    lift ← Gen.oneOf(true, false)
    rubbishChute ← Gen.oneOf(true, false)
    parking ← Gen.oneOf(true, false)
    alarm ← Gen.oneOf(true, false)
    alarmFlat ← Gen.oneOf(true, false)
    kitchen ← Gen.oneOf(true, false)
    pool ← Gen.oneOf(true, false)
    billiard ← Gen.oneOf(true, false)
    sauna ← Gen.oneOf(true, false)
    heatingSupply ← Gen.oneOf(true, false)
    waterSupply ← Gen.oneOf(true, false)
    sewerageSupply ← Gen.oneOf(true, false)
    electricitySupply ← Gen.oneOf(true, false)
    gasSupply ← Gen.oneOf(true, false)
    airCondition ← Gen.oneOf(true, false)
    builtInTech ← Gen.oneOf(true, false)
    dishwasher ← Gen.oneOf(true, false)
    pets ← Gen.oneOf(true, false)
    children ← Gen.oneOf(true, false)
    pmg ← Gen.oneOf(true, false)
    isNew ← Gen.oneOf(true, false)
    isElite ← Gen.oneOf(true, false)
    isStudio ← Gen.oneOf(true, false)
    isApartments ← Gen.oneOf(true, false)
    openPlan ← Gen.oneOf(true, false)
    haggle ← Gen.oneOf(true, false)
    mortgage ← Gen.oneOf(true, false)
    rentPledge ← Gen.oneOf(true, false)
    passBy ← Gen.oneOf(true, false)
  } yield RealtyOfferFacilities
    .newBuilder()
    .setFlagPhone(phone)
    .setFlagTelevision(television)
    .setFlagInternet(internet)
    .setFlagWashingMachine(washingMachine)
    .setFlagRefrigerator(refrigerator)
    .setFlagFurnitureRoom(furnitureRoom)
    .setFlagFurnitureKitchen(furnitureKitchen)
    .setFlagLift(lift)
    .setFlagRubbishChute(rubbishChute)
    .setFlagParking(parking)
    .setFlagAlarm(alarm)
    .setFlagAlarmFlat(alarmFlat)
    .setFlagKitchen(kitchen)
    .setFlagPool(pool)
    .setFlagBilliard(billiard)
    .setFlagSauna(sauna)
    .setFlagHeatingSupply(heatingSupply)
    .setFlagWaterSupply(waterSupply)
    .setFlagSewerageSupply(sewerageSupply)
    .setFlagElectricitySupply(electricitySupply)
    .setFlagGasSupply(gasSupply)
    .setFlagAirCondition(airCondition)
    .setFlagBuiltInTech(builtInTech)
    .setFlagDishwasher(dishwasher)
    .setFlagPets(pets)
    .setFlagChildren(children)
    .setFlagPmg(pmg)
    .setFlagIsNew(isNew)
    .setFlagIsElite(isElite)
    .setFlagIsStudio(isStudio)
    .setFlagIsApartments(isApartments)
    .setFlagOpenPlan(openPlan)
    .setFlagHaggle(haggle)
    .setFlagMortgage(mortgage)
    .setFlagRentPledge(rentPledge)
    .setFlagPassBy(passBy)
    .build()

  val RealtyOfferGen = for {
    propertyType ← Gen.oneOf(RealtyPropertyType.values())
    categoryType ← Gen.oneOf(RealtyCategory.values())
    location ← LocationGen
    timeStampUpdate ← CreateDateGen
    price ← PriceGen
    offerType ← Gen.oneOf(OfferType.RENT, OfferType.SELL)
    facilities ← FacilitiesGen
  } yield RealtyOffer
    .newBuilder()
    .setPropertyType(propertyType)
    .setCategory(categoryType)
    .setAddress(location)
    .setPrice(price)
    .setOfferType(offerType)
    .setFacilities(facilities)
    .build()

  def offerGen(
    userGen: Gen[User] = UserGenerator.NewUserGen,
    timestampCreateGen: Gen[Long] = CreateDateGen,
    timeStampUpdateGen: Long ⇒ Gen[Long] = updateDateGen,
    ttlGen: Gen[Int] = Gen.choose(10, 1000)
  ): Gen[Offer] =
    for {
      offer ← OfferGenerator.offerBaseGen(
        userGen,
        timestampCreateGen,
        timeStampUpdateGen,
        ttlGen,
        OfferService.OFFER_REALTY,
        idManager
      )
      realtyOffer ← RealtyOfferGen
      externalId ← Gen.choose(10000, 1000000)
    } yield offer.setOfferRealty(realtyOffer).setExternalId(externalId.toString).build()
}
