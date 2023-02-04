package ru.yandex.realty.searcher.search.clause

import org.scalacheck.Gen
import ru.yandex.realty.model.building.{BuildingPriceStatistics, BuildingPriceStatisticsItem}
import ru.yandex.realty.model.gen.RealtyGenerators
import ru.yandex.realty.model.offer.{
  ApartmentInfo,
  BuildingInfo,
  Offer,
  RoomsType,
  SaleAgent,
  SalesAgentCategory,
  WindowView
}
import ru.yandex.realty.proto.site.cluster.FlatPlanStored
import ru.yandex.realty.response.GuessedFlatPlan
import ru.yandex.realty.vos.model.offer.CuratedFlatPlan

trait NRTIndexOfferGenerator extends RealtyGenerators {

  def offerGen(offerId: Long): Gen[Offer] = {
    val offer = new Offer()
    val saleAgent = offer.createAndGetSaleAgent()
    for {
      id <- Gen.const(offerId)
      apartmentInfo <- apartmentInfoGen
      _ <- saleAgentGen(saleAgent)
      buildingInfo <- buildingInfoGen
    } yield {
      offer.setId(id)
      offer.setApartmentInfo(apartmentInfo)
      offer.setBuildingInfo(buildingInfo)
      offer
    }
  }

  def apartmentInfoGen: Gen[ApartmentInfo] = {
    for {
      floorCoverageString <- readableString
      windowView <- javaEnum(WindowView.values())
      newFlat <- bool
      mobileNewFlat <- bool
      rooms <- posNum[Int]
      roomsOffered <- posNum[Int]
      openPlan <- bool
      studio <- bool
      roomsType <- javaEnum(RoomsType.values())
      phoneLines <- posNum[Int]
      electricCapacity <- posNum[Int]
      cadastralNumber <- readableString
      apartment <- readableString
      guessedFlatPlan <- Gen.const(GuessedFlatPlan.getDefaultInstance)
      curatedFlatPlan <- Gen.const(CuratedFlatPlan.getDefaultInstance)
      siteFlatPlanStored <- Gen.const(FlatPlanStored.getDefaultInstance)
    } yield {
      val apartmentInfo = new ApartmentInfo()
      apartmentInfo.setFloorCoverageString(floorCoverageString)
      apartmentInfo.setWindowView(windowView)
      apartmentInfo.setNewFlat(newFlat)
      apartmentInfo.setMobileNewFlat(mobileNewFlat)
      apartmentInfo.setRooms(rooms)
      apartmentInfo.setRoomsOffered(roomsOffered)
      apartmentInfo.setOpenPlan(openPlan)
      apartmentInfo.setStudio(studio)
      apartmentInfo.setRoomsType(roomsType)
      apartmentInfo.setPhoneLines(phoneLines)
      apartmentInfo.setElectricCapacity(electricCapacity)
      apartmentInfo.setCadastralNumber(cadastralNumber)
      apartmentInfo.setApartment(apartment)
      apartmentInfo.setGuessedFlatPlan(guessedFlatPlan)
      apartmentInfo.setCuratedFlatPlan(curatedFlatPlan)
      apartmentInfo.setSiteFlatPlanStored(siteFlatPlanStored)
      apartmentInfo
    }
  }

  def saleAgentGen(saleAgent: SaleAgent): Gen[SaleAgent] = {
    for {
      category <- javaEnum(SalesAgentCategory.values())
    } yield {
      saleAgent.setCategory(category)
      saleAgent
    }
  }

  def buildingInfoGen: Gen[BuildingInfo] = {
    for {
      buildingPhase <- readableString
      buildingSection <- readableString
      buildQuarter <- posNum[Int]
      phaseId <- posNum[Long]
      houseId <- posNum[Long]
      expectDemolition <- bool
      sectionName <- readableString
      partnerHouseId <- readableString
      reconstructionYear <- posNum[Int]
      flatsCount <- posNum[Int]
      porchesCount <- posNum[Int]
      priceStatistics <- buildingPriceStatisticsGen
    } yield {
      val buildingInfo = new BuildingInfo()
      buildingInfo.setBuildingPhase(buildingPhase)
      buildingInfo.setBuildingSection(buildingSection)
      buildingInfo.setBuildQuarter(buildQuarter)
      buildingInfo.setPhaseId(phaseId)
      buildingInfo.setHouseId(houseId)
      buildingInfo.setExpectDemolition(expectDemolition)
      buildingInfo.setSectionName(sectionName)
      buildingInfo.setPartnerHouseId(partnerHouseId)
      buildingInfo.setReconstructionYear(reconstructionYear)
      buildingInfo.setFlatsCount(flatsCount)
      buildingInfo.setPorchesCount(porchesCount)
      buildingInfo.setPriceStatistics(priceStatistics)
      buildingInfo
    }
  }

  def buildingPriceStatisticsGen: Gen[BuildingPriceStatistics] = {
    for {
      sellPrice <- Gen.const(None)
      sellPriceByRooms <- Gen.const(Map[Int, BuildingPriceStatisticsItem]())
      rentPriceByRooms <- Gen.const(Map[Int, BuildingPriceStatisticsItem]())
      profitability <- Gen.const(None)
    } yield {
      BuildingPriceStatistics(
        sellPrice = sellPrice,
        sellPriceByRooms = sellPriceByRooms,
        rentPriceByRooms = rentPriceByRooms,
        profitability = profitability
      )
    }
  }

}
