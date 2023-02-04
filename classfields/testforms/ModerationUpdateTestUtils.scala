package ru.yandex.vos2.autoru.utils.testforms

import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel._
import ru.auto.api.CarsModel.CarInfo
import ru.auto.api.CommonModel.{GeoPoint, Video}
import ru.auto.api.ModerationUpdateModel.ModerationUpdate
import ru.auto.api.ModerationUpdateModel.ModerationUpdate.VideoUpdate
import ru.auto.api.MotoModel.MotoInfo
import ru.auto.api.TrucksModel.{TruckCategory, TruckInfo}
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.Editor

import scala.jdk.CollectionConverters._

/**
  * Created by sievmi on 12.02.18
  */

object ModerationUpdateTestUtils {

  //scalastyle:off
  def generateModerationUpdate(
      oldForm: ApiOfferModel.Offer,
      newForm: ApiOfferModel.Offer,
      updateBuilder: ModerationUpdate.Builder = ModerationUpdate.newBuilder()
  ): ModerationUpdate.Builder = {

    generateDocumentsUpdate(updateBuilder, oldForm.getDocuments, newForm.getDocuments)
    generateStateUpdate(updateBuilder, oldForm.getState, newForm.getState)
    generateSellerUpdate(updateBuilder, oldForm.getSeller, newForm.getSeller)

    updateBuilder.setColorHex(newForm.getColorHex)
    updateBuilder.setDescription(newForm.getDescription)
    updateBuilder.setPriceInfo(newForm.getPriceInfo)
    updateBuilder.setEditor(Editor.newBuilder().setName("testEditorName"))
    updateBuilder.setPurchaseDate(newForm.getDocuments.getPurchaseDate)

    newForm.getCategory match {
      case Category.CARS => generateUpdateForCars(updateBuilder, oldForm.getCarInfo, newForm.getCarInfo)
      case Category.TRUCKS => generateUpdateForTrucks(updateBuilder, oldForm.getTruckInfo, newForm.getTruckInfo)
      case Category.MOTO => generateUpdateForMoto(updateBuilder, oldForm.getMotoInfo, newForm.getMotoInfo)
    }

    updateBuilder
  }

  def checkModerationUpdateCorrect(form: ApiOfferModel.Offer, update: ModerationUpdate): Boolean = {
    val categoryUpdateCorrect = {
      form.getCategory match {
        case Category.CARS => checkUpdateCorrectCar(form.getCarInfo, update)
        case Category.TRUCKS => checkUpdateCorrectTruck(form.getTruckInfo, update)
        case Category.MOTO => checkUpdateCorrectMoto(form.getMotoInfo, update)
      }
    }

    // Для коммерческих офферов все данные берутся из базы по poiID
    val sellerUpdateCorrect = form.getSellerType != SellerType.PRIVATE || checkUpdateCorrectSeller(
      form.getSeller,
      update
    )

    categoryUpdateCorrect && sellerUpdateCorrect &&
    checkUpdateCorrectDocuments(form.getDocuments, update) &&
    checkUpdateCorrectState(form.getState, update) &&
    compareIfExist(update.hasColorHex, update.getColorHex, form.getColorHex) &&
    compareIfExist(update.hasDescription, update.getDescription, form.getDescription)
  }

  private def generateUpdateForCars(updateBuilder: ModerationUpdate.Builder,
                                    oldCarInfo: CarInfo,
                                    newCarInfo: CarInfo): Unit = {

    val carUpdate = ModerationUpdate.Car.newBuilder()
    setIfExists(newCarInfo.hasTechParamId, newCarInfo.getTechParamId, carUpdate.setTechParamId)
    setIfExists(newCarInfo.hasComplectationId, newCarInfo.getComplectationId, carUpdate.setComplectationId)
    setIfExists(newCarInfo.hasConfigurationId, newCarInfo.getConfigurationId, carUpdate.setConfigurationId)

    updateBuilder.setCarUpdate(carUpdate.build())
  }

  private def generateUpdateForTrucks(updateBuilder: ModerationUpdate.Builder,
                                      oldTruckInfo: TruckInfo,
                                      newTruckInfo: TruckInfo): Unit = {

    val truckUpdate = ModerationUpdate.Truck.newBuilder()

    truckUpdate.setMark(newTruckInfo.getMark)
    truckUpdate.setModel(newTruckInfo.getModel)
    truckUpdate.setHorsePower(newTruckInfo.getHorsePower)
    truckUpdate.setSteeringWheel(newTruckInfo.getSteeringWheel)
    truckUpdate.setTruckType(newTruckInfo.getTruckType)
    truckUpdate.setSwapBodyType(newTruckInfo.getSwapBodyType)
    truckUpdate.setGearType(newTruckInfo.getGear)
    truckUpdate.setEngineVolume(newTruckInfo.getDisplacement)
    truckUpdate.setTransmission(newTruckInfo.getTransmission)
    truckUpdate.setCabin(newTruckInfo.getCabin)
    truckUpdate.setEngineType(newTruckInfo.getEngine)
    truckUpdate.setTruckCategory(newTruckInfo.getTruckCategory)
    newTruckInfo.getTruckCategory match {
      case TruckCategory.TRUCK => truckUpdate.setTruckType(newTruckInfo.getTruckType)
      case TruckCategory.LCV => truckUpdate.setLightTruckType(newTruckInfo.getLightTruckType)
      case TruckCategory.TRAILER => truckUpdate.setTrailerType(newTruckInfo.getTrailerType)
      case TruckCategory.SWAP_BODY => truckUpdate.setSwapBodyType(newTruckInfo.getSwapBodyType)
      case TruckCategory.BUS => truckUpdate.setBusType(newTruckInfo.getBusType)
      case TruckCategory.AGRICULTURAL => truckUpdate.setAgriculturalType(newTruckInfo.getAgriculturalType)
      case TruckCategory.CONSTRUCTION => truckUpdate.setConstructionType(newTruckInfo.getConstructionType)
      case TruckCategory.AUTOLOADER => truckUpdate.setAutoloaderType(newTruckInfo.getAutoloaderType)
      case TruckCategory.DREDGE => truckUpdate.setDredgeType(newTruckInfo.getDredgeType)
      case TruckCategory.BULLDOZERS => truckUpdate.setBulldozerType(newTruckInfo.getBulldozerType)
      case TruckCategory.MUNICIPAL => truckUpdate.setMunicipalType(newTruckInfo.getMunicipalType)
      case _ =>
    }

    updateBuilder.setTruckUpdate(truckUpdate.build())
  }

  private def generateUpdateForMoto(updateBuilder: ModerationUpdate.Builder,
                                    oldMotoInfo: MotoInfo,
                                    newMotoInfo: MotoInfo): Unit = {
    val motoUpdate = ModerationUpdate.Moto.newBuilder()

    motoUpdate.setMark(newMotoInfo.getMark)
    motoUpdate.setModel(newMotoInfo.getModel)
    motoUpdate.setHorsePower(newMotoInfo.getHorsePower)
    motoUpdate.setGearType(newMotoInfo.getGear)
    motoUpdate.setEngineVolume(newMotoInfo.getDisplacement)
    motoUpdate.setTransmission(newMotoInfo.getTransmission)
    motoUpdate.setEngineType(newMotoInfo.getEngine)
    motoUpdate.setMotoCategory(newMotoInfo.getMotoCategory)
    motoUpdate.setMotoType(newMotoInfo.getMotoType)

    updateBuilder.setMotoUpdate(motoUpdate.build())
  }

  private def generateDocumentsUpdate(updateBuilder: ModerationUpdate.Builder,
                                      oldDocuments: Documents,
                                      newDocuments: Documents): Unit = {
    updateBuilder.setOwnersNumber(newDocuments.getOwnersNumber)
    updateBuilder.setPtsOriginal(newDocuments.getPtsOriginal)
    updateBuilder.setSts(newDocuments.getSts)
    updateBuilder.setVin(oldDocuments.getVin)
    updateBuilder.setCustomCleared(newDocuments.getCustomCleared)
    updateBuilder.setYear(newDocuments.getYear)
  }

  private def generateStateUpdate(updateBuilder: ModerationUpdate.Builder, oldState: State, newState: State): Unit = {

    updateBuilder.setMileage(newState.getMileage)
    updateBuilder.setStateNotBeaten(newState.getStateNotBeaten)

    updateBuilder.addAllPhoto(newState.getImageUrlsList)
    updateBuilder.addAllModerationPhoto(newState.getModerationImageList)

    if (newState.hasVideo) {
      updateBuilder.setVideoUpdate(VideoUpdate.newBuilder().setVideo(newState.getVideo))
    } else {
      updateBuilder.setVideoUpdate(VideoUpdate.newBuilder().clearVideo())
    }
  }

  private def generateSellerUpdate(updateBuilder: ModerationUpdate.Builder,
                                   oldSeller: Seller,
                                   newSeller: Seller): Unit = {
    val newLocation: ApiOfferModel.Location = newSeller.getLocation

    updateBuilder.setAddress(newLocation.getAddress)
    updateBuilder.setGeobaseId(newLocation.getGeobaseId)
    updateBuilder.setOfferUserName(newSeller.getName)
    updateBuilder.setCoordinates(newLocation.getCoord)

    updateBuilder.addAllPhones(newSeller.getPhonesList)
  }

  private def checkUpdateCorrectCar(carInfo: CarInfo, update: ModerationUpdate): Boolean = {
    if (update.hasCarUpdate) {
      val carUpdate = update.getCarUpdate
      compareIfExist(carUpdate.hasTechParamId, carUpdate.getTechParamId, carInfo.getTechParamId) &&
      compareIfExist(carUpdate.hasComplectationId, carUpdate.getComplectationId, carInfo.getComplectationId) &&
      compareIfExist(carUpdate.hasConfigurationId, carUpdate.getConfigurationId, carInfo.getConfigurationId)
    } else true
  }

  private def checkUpdateCorrectTruck(truckInfo: TruckInfo, update: ModerationUpdate): Boolean = {
    val correctTruckUpdate = if (update.hasTruckUpdate) {
      val truckUpdate = update.getTruckUpdate
      compareIfExist(truckUpdate.hasTruckType, truckUpdate.getTruckType, truckInfo.getTruckType) &&
      compareIfExist(truckUpdate.hasSwapBodyType, truckUpdate.getSwapBodyType, truckInfo.getSwapBodyType) &&
      compareIfExist(truckUpdate.hasGearType, truckUpdate.getGearType, truckInfo.getGear) &&
      compareIfExist(truckUpdate.hasTransmission, truckUpdate.getTransmission, truckInfo.getTransmission) &&
      compareIfExist(truckUpdate.hasCabin, truckUpdate.getCabin, truckInfo.getCabin) &
        compareIfExist(truckUpdate.hasEngineType, truckUpdate.getEngineType, truckInfo.getEngine) &&
      compareIfExist(truckUpdate.hasMark, truckUpdate.getMark, truckInfo.getMark) &&
      compareIfExist(truckUpdate.hasModel, truckUpdate.getModel, truckInfo.getModel) &&
      compareIfExist(truckUpdate.hasHorsePower, truckUpdate.getHorsePower, truckInfo.getHorsePower) &&
      compareIfExist(truckUpdate.hasSteeringWheel, truckUpdate.getSteeringWheel, truckInfo.getSteeringWheel)
    } else true

    correctTruckUpdate
  }

  private def checkUpdateCorrectMoto(motoInfo: MotoInfo, update: ModerationUpdate): Boolean = {
    val correctMotoUpdate = if (update.hasMotoUpdate) {
      val motoUpdate = update.getMotoUpdate
      compareIfExist(motoUpdate.hasGearType, motoUpdate.getGearType, motoInfo.getGear) &&
      compareIfExist(motoUpdate.hasTransmission, motoUpdate.getTransmission, motoInfo.getTransmission) &&
      compareIfExist(motoUpdate.hasEngineType, motoUpdate.getEngineType, motoInfo.getEngine) &&
      compareIfExist(motoUpdate.hasMark, motoUpdate.getMark, motoInfo.getMark) &&
      compareIfExist(motoUpdate.hasModel, motoUpdate.getModel, motoInfo.getModel) &&
      compareIfExist(motoUpdate.hasHorsePower, motoUpdate.getHorsePower, motoInfo.getHorsePower)
    } else true

    correctMotoUpdate
  }

  private def checkUpdateCorrectDocuments(documents: Documents, update: ModerationUpdate): Boolean = {
    compareIfExist(update.hasOwnersNumber, update.getOwnersNumber, documents.getOwnersNumber) &&
    compareIfExist(update.hasPtsOriginal, update.getPtsOriginal, documents.getPtsOriginal) &&
    compareIfExist(update.hasSts, update.getSts, documents.getSts) &&
    compareIfExist(update.hasVin, update.getVin, documents.getVin) &&
    compareIfExist(update.hasCustomCleared, update.getCustomCleared, documents.getCustomCleared) &&
    compareIfExist(update.hasYear, update.getYear, documents.getYear)
  }

  private def checkUpdateCorrectState(state: State, update: ModerationUpdate): Boolean = {
    val correctPhoto = update.getPhotoList.asScala.forall(photo =>
      state.getImageUrlsList.asScala.exists(statePhoto => statePhoto.getName == photo.getName)
    )

    val correctModerationPhoto = update.getModerationPhotoList.asScala.forall(photo =>
      state.getModerationImageList.asScala.exists(statePhoto => statePhoto.getName == photo.getName)
    )

    compareIfExist(update.hasMileage, update.getMileage, state.getMileage) &&
    compareIfExist(update.hasStateNotBeaten, update.getStateNotBeaten, state.getStateNotBeaten) &&
    correctPhoto &&
    correctModerationPhoto

  }

  private def checkUpdateCorrectSeller(seller: Seller, update: ModerationUpdate): Boolean = {
    compareIfExist(update.hasAddress, update.getAddress, seller.getLocation.getAddress) &&
    compareIfExist(update.hasGeobaseId, update.getGeobaseId, seller.getLocation.getGeobaseId) &&
    compareIfExist(update.hasCoordinates, update.getCoordinates, seller.getLocation.getCoord) &&
    compareIfExist(update.hasOfferUserName, update.getOfferUserName, seller.getName) &&
    (update.getPhonesList.isEmpty || update.getPhonesList.asScala.map(_.getPhone).toSet ==
      seller.getPhonesList.asScala.map(_.getOriginal).toSet)

  }

  private def compareIfExist[T](exists: Boolean, expected: T, actual: T): Boolean = {
    if (exists) expected == actual
    else true
  }

  private def setIfNotEqual[T](oldValue: T, newValue: T, update: T => Unit): Unit = {
    if (oldValue != newValue) update(newValue)
  }

  private def setIfExists[T](exists: Boolean, value: T, update: T => Unit): Unit = {
    if (exists) update(value)
  }
}
