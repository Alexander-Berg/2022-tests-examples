package ru.yandex.auto.garage.managers.validation

import com.google.protobuf.util.Timestamps
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.AdditionalInfo.ProvenOwnerStatus
import ru.auto.api.ApiOfferModel.{Documents, OfferStatus}
import ru.auto.api.CarsModel.CarInfo
import ru.auto.api.CommonModel.Date
import ru.auto.api.internal.Mds.MdsPhotoInfo
import ru.auto.api.vin.Common.{IdentifierType, Photo}
import ru.auto.api.vin.VinReportModel
import ru.auto.api.vin.VinReportModel.InsuranceType
import ru.auto.api.vin.garage.GarageApiModel
import ru.auto.api.vin.garage.GarageApiModel.CardTypeInfo.CardType
import ru.auto.api.vin.garage.GarageApiModel.ProvenOwnerState.DocumentsPhotos
import ru.auto.api.vin.garage.GarageApiModel.ProvenOwnerState.DocumentsPhotos.Photos
import ru.auto.api.vin.garage.GarageApiModel.Vehicle.Mileage
import ru.auto.api.vin.garage.GarageApiModel.{ProvenOwnerState, Vehicle}
import ru.yandex.auto.garage.converters.cards.ProvenOwnerHelper
import ru.yandex.auto.garage.managers.validation.proven_owner.ProvenOwnerValidationManager
import ru.yandex.auto.garage.utils.features.GarageFeaturesRegistry
import ru.yandex.auto.vin.decoder.extdata.catalog.cars.CarsCatalog
import ru.yandex.auto.vin.decoder.extdata.catalog.cars.model.CarCard
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.{GarageCard, OfferInfo, VehicleInfo}
import ru.yandex.auto.vin.decoder.model.{LicensePlate, VinCode}
import ru.yandex.auto.vin.decoder.proto.TtxSchema.{ColorInfo, CommonTtx}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import java.time.Year
import scala.jdk.CollectionConverters._
import scala.concurrent.duration._

class CardValidationManagerTest extends AnyWordSpecLike with Matchers with MockitoSupport {

  private val catalog = mock[CarsCatalog]
  private val garageFeaturesRegistry = mock[GarageFeaturesRegistry]
  private val provenOwnerValidationManager = new ProvenOwnerValidationManager

  private val validator =
    new CardValidationManager(provenOwnerValidationManager, catalog, garageFeaturesRegistry)
  private val featureTrue = mock[Feature[Boolean]]
  when(featureTrue.value).thenReturn(true)
  private val featureFalse = mock[Feature[Boolean]]
  when(featureFalse.value).thenReturn(false)

  "validate purchase date" should {
    "return error" when {
      "invalid month" in {
        val date = Date.newBuilder().setYear(2018).setMonth(15).build()
        val documents = Documents.newBuilder().setPurchaseDate(date).build()

        val res = validator.validatePurchaseDate(documents)

        res.size shouldBe 1
        res.head shouldBe PurchaseDateError
      }
      "invalid day" in {
        val date = Date.newBuilder().setYear(2018).setMonth(2).setDay(30).build()
        val documents = Documents.newBuilder().setPurchaseDate(date).build()

        val res = validator.validatePurchaseDate(documents)

        res.size shouldBe 1
        res.head shouldBe PurchaseDateError
      }
      "invalid year" in {
        val date = Date.newBuilder().setYear(1812).setMonth(3).setDay(1).build()
        val documents = Documents.newBuilder().setPurchaseDate(date).build()

        val res = validator.validatePurchaseDate(documents)

        res.size shouldBe 1
        res.head shouldBe PurchaseDateError
      }
      "production year more than purchase year" in {
        val date = Date.newBuilder().setYear(2018).setMonth(3).setDay(1).build()
        val documents = Documents.newBuilder().setPurchaseDate(date).setYear(2019).build()

        val res = validator.validatePurchaseDate(documents)

        res.size shouldBe 1
        res.head shouldBe PurchaseDateError
      }
      "purchase date more than current" in {
        val date = Date.newBuilder().setYear(2030).setMonth(3).setDay(1).build()
        val documents = Documents.newBuilder().setPurchaseDate(date).build()

        val res = validator.validatePurchaseDate(documents)

        res.size shouldBe 1
        res.head shouldBe PurchaseDateError
      }
    }
    "return success" when {
      "purchase date is empty" in {
        val documents = Documents.newBuilder().build()
        val res = validator.validatePurchaseDate(documents)
        res.size shouldBe 0
      }
      "purchase date correct" in {
        val date = Date.newBuilder().setYear(2015).setMonth(6).setDay(27).build()
        val documents = Documents.newBuilder().setPurchaseDate(date).setYear(2014).build()

        val res = validator.validatePurchaseDate(documents)

        res.size shouldBe 0
      }

    }
  }

  "validate sale date" should {
    "return error" when {
      "invalid month" in {
        val date = Date.newBuilder().setYear(2018).setMonth(15).build()
        val vehicle = Vehicle.newBuilder().setSaleDate(date).build()

        val res = validator.validateSaleDate(vehicle)

        res.size shouldBe 1
        res.head shouldBe SaleDateError
      }
      "invalid day" in {
        val date = Date.newBuilder().setYear(2018).setMonth(2).setDay(30).build()
        val vehicle = Vehicle.newBuilder().setSaleDate(date).build()

        val res = validator.validateSaleDate(vehicle)

        res.size shouldBe 1
        res.head shouldBe SaleDateError
      }
      "invalid year" in {
        val date = Date.newBuilder().setYear(1812).setMonth(3).setDay(1).build()
        val vehicle = Vehicle.newBuilder().setSaleDate(date).build()

        val res = validator.validateSaleDate(vehicle)

        res.size shouldBe 1
        res.head shouldBe SaleDateError
      }
      "production year more than sale year" in {
        val date = Date.newBuilder().setYear(2018).setMonth(3).setDay(1).build()
        val documents = Documents.newBuilder().setYear(2019).build()
        val vehicle = Vehicle.newBuilder().setSaleDate(date).setDocuments(documents).build()

        val res = validator.validateSaleDate(vehicle)

        res.size shouldBe 1
        res.head shouldBe SaleDateError
      }
      "sale date more than current" in {
        val date = Date.newBuilder().setYear(2199).setMonth(3).setDay(1).build()
        val vehicle = Vehicle.newBuilder().setSaleDate(date).build()

        val res = validator.validateSaleDate(vehicle)

        res.size shouldBe 1
        res.head shouldBe SaleDateError
      }
      "sale date less than purchase date" in {
        val saleDate = Date.newBuilder().setYear(2015).setMonth(3).setDay(1).build()
        val purchaseDate = Date.newBuilder().setYear(2016).setMonth(3).setDay(1).build()
        val documents = Documents.newBuilder().setPurchaseDate(purchaseDate)
        val vehicle = Vehicle.newBuilder().setSaleDate(saleDate).setDocuments(documents).build()

        val res = validator.validateSaleDate(vehicle)

        res.size shouldBe 1
        res.head shouldBe SaleDateError
      }
    }
    "return success" when {
      "sale date is empty" in {
        val vehicle = Vehicle.newBuilder().build()
        val res = validator.validateSaleDate(vehicle)
        res.size shouldBe 0
      }
      "sale date correct" in {
        val date = Date.newBuilder().setYear(2015).setMonth(6).setDay(27).build()
        val purchaseDate = Date.newBuilder().setYear(2013).setMonth(3).setDay(1).build()
        val documents = Documents.newBuilder().setPurchaseDate(purchaseDate)
        val vehicle = Vehicle.newBuilder().setSaleDate(date).setDocuments(documents).build()

        val res = validator.validateSaleDate(vehicle)

        res.size shouldBe 0
      }
    }
  }

  "validate VIN LP mark and model with ProvenOwner" should {
    val currentVin = "WDD2210561A232855"
    val newVin = "WDD2210561A232005"

    val currentLp = "A123AA24"
    val newLp = "A555AA77"

    val currentMark = "BMW"
    val newMark = "Opel"

    val currentModel = "X5"
    val newModel = "Matiz"

    val currentProductionYear = 2020
    val newProductionYear = 2019

    val newColor = GarageApiModel.Vehicle.Color.newBuilder().setId("C49648").setName("Бежевый").build()

    val newOldColor = GarageApiModel.Vehicle.Color.newBuilder().setId("4A2197").setName("Фиолетовый").build()

    val currentColor =
      ColorInfo.newBuilder().setRgbCode("9966cc").setName("Фиолетовый").setAutoruColorId("4A2197").build()

    val newTechParamId = 1234

    val currentTechParamId = 4321

    val OkForEditVerdict = ProvenOwnerHelper.StatusesForApproveChangeInfo

    val NotOkForEditVerdict = ProvenOwnerHelper.StatusesForForbiddenChangeInfo

    val AllVerdict = OkForEditVerdict ++ NotOkForEditVerdict

    "return data error" when {
      "newVIN != currentVIN with not ok for edit provenOwnerStatus" in {
        val result = NotOkForEditVerdict.flatMap { verdict =>
          val owner = GarageSchema.ProvenOwnerState
            .newBuilder()
            .setVerdict(verdict)
            .build()

          val currentVehicleInfo = GarageSchema.VehicleInfo
            .newBuilder()
            .setCatalog(VehicleInfo.AutoruCatalogData.newBuilder().setTechParamId(currentTechParamId).build())
            .setDocuments(
              VehicleInfo.Documents
                .newBuilder()
                .setVin(currentVin)
                .setLicensePlate(currentLp)
                .build()
            )
            .setTtx(
              CommonTtx
                .newBuilder()
                .setColor(currentColor)
                .setYear(currentProductionYear)
                .setMark(currentMark)
                .setModel(currentModel)
                .build()
            )
            .build()
          val newVehicleInfo = GarageApiModel.Vehicle
            .newBuilder()
            .setColor(newOldColor)
            .setDocuments(
              ApiOfferModel.Documents
                .newBuilder()
                .setVin(newVin)
                .setYear(currentProductionYear)
                .setYear(2020)
                .build()
            )
            .setCarInfo(
              CarInfo
                .newBuilder()
                .setTechParamId(currentTechParamId)
                .setMark(currentMark)
                .setModel(currentModel)
                .build()
            )
            .build()

          validator.validateMainInfoByProvenOwner(
            owner,
            IdentifierType.VIN,
            addedManually = false,
            newVehicleInfo,
            currentVehicleInfo,
            OfferInfo.newBuilder().build()
          )
        }
        result.nonEmpty shouldBe true
        result.size shouldBe NotOkForEditVerdict.size
        result.toSet.size shouldBe 1
        result.toSet.head shouldBe VinEditForbiddenError
      }
      "newLP != currentLP with not ok for edit provenOwnerStatus" in {
        val result = NotOkForEditVerdict.flatMap { verdict =>
          val owner = GarageSchema.ProvenOwnerState
            .newBuilder()
            .setVerdict(verdict)
            .build()

          val currentVehicleInfo = GarageSchema.VehicleInfo
            .newBuilder()
            .setCatalog(VehicleInfo.AutoruCatalogData.newBuilder().setTechParamId(currentTechParamId).build())
            .setDocuments(
              VehicleInfo.Documents
                .newBuilder()
                .setVin(currentVin)
                .setLicensePlate(currentLp)
                .build()
            )
            .setTtx(
              CommonTtx
                .newBuilder()
                .setColor(currentColor)
                .setYear(currentProductionYear)
                .setMark(currentMark)
                .setModel(currentModel)
                .build()
            )
            .build()
          val newVehicleInfo = GarageApiModel.Vehicle
            .newBuilder()
            .setColor(newOldColor)
            .setDocuments(
              ApiOfferModel.Documents
                .newBuilder()
                .setYear(currentProductionYear)
                .setLicensePlate(newLp)
                .build()
            )
            .setCarInfo(
              CarInfo
                .newBuilder()
                .setTechParamId(currentTechParamId)
                .setMark(currentMark)
                .setModel(currentModel)
                .build()
            )
            .build()

          validator.validateMainInfoByProvenOwner(
            owner,
            IdentifierType.LICENSE_PLATE,
            addedManually = false,
            newVehicleInfo,
            currentVehicleInfo,
            OfferInfo.newBuilder().build()
          )
        }
        result.nonEmpty shouldBe true
        result.size shouldBe NotOkForEditVerdict.size
        result.toSet.size shouldBe 1
        result.toSet.head shouldBe LicensePlateEditForbiddenError
      }

      "newVIN != currentVIN with not ok for edit provenOwnerStatus and addedManually is true" in {
        val result = NotOkForEditVerdict.flatMap { verdict =>
          val owner = GarageSchema.ProvenOwnerState
            .newBuilder()
            .setVerdict(verdict)
            .build()

          val currentVehicleInfo = GarageSchema.VehicleInfo
            .newBuilder()
            .setCatalog(VehicleInfo.AutoruCatalogData.newBuilder().setTechParamId(currentTechParamId).build())
            .setDocuments(
              VehicleInfo.Documents
                .newBuilder()
                .setVin(currentVin)
                .setLicensePlate(currentLp)
                .build()
            )
            .setTtx(
              CommonTtx
                .newBuilder()
                .setColor(currentColor)
                .setYear(currentProductionYear)
                .setMark(currentMark)
                .setModel(currentModel)
                .build()
            )
            .build()
          val newVehicleInfo = GarageApiModel.Vehicle
            .newBuilder()
            .setColor(newOldColor)
            .setDocuments(
              ApiOfferModel.Documents
                .newBuilder()
                .setVin(newVin)
                .setYear(currentProductionYear)
                .setYear(2020)
                .build()
            )
            .setCarInfo(
              CarInfo
                .newBuilder()
                .setTechParamId(currentTechParamId)
                .setMark(currentMark)
                .setModel(currentModel)
                .build()
            )
            .build()

          validator.validateMainInfoByProvenOwner(
            owner,
            IdentifierType.VIN,
            addedManually = true,
            newVehicleInfo,
            currentVehicleInfo,
            OfferInfo.newBuilder().build()
          )
        }
        result.nonEmpty shouldBe true
        result.size shouldBe 2 * NotOkForEditVerdict.size
        result.toSet.size shouldBe 2
        result.toSet shouldBe Set(VinEditForbiddenError, LicensePlateEditForbiddenError)
      }

      "newLP != currentLP with not ok for edit provenOwnerStatus and addedManually is true" in {
        val result = NotOkForEditVerdict.flatMap { verdict =>
          val owner = GarageSchema.ProvenOwnerState
            .newBuilder()
            .setVerdict(verdict)
            .build()

          val currentVehicleInfo = GarageSchema.VehicleInfo
            .newBuilder()
            .setCatalog(VehicleInfo.AutoruCatalogData.newBuilder().setTechParamId(currentTechParamId).build())
            .setDocuments(
              VehicleInfo.Documents
                .newBuilder()
                .setVin(currentVin)
                .setLicensePlate(currentLp)
                .build()
            )
            .setTtx(
              CommonTtx
                .newBuilder()
                .setColor(currentColor)
                .setYear(currentProductionYear)
                .setMark(currentMark)
                .setModel(currentModel)
                .build()
            )
            .build()
          val newVehicleInfo = GarageApiModel.Vehicle
            .newBuilder()
            .setColor(newOldColor)
            .setDocuments(
              ApiOfferModel.Documents
                .newBuilder()
                .setYear(currentProductionYear)
                .setLicensePlate(newLp)
                .build()
            )
            .setCarInfo(
              CarInfo
                .newBuilder()
                .setTechParamId(currentTechParamId)
                .setMark(currentMark)
                .setModel(currentModel)
                .build()
            )
            .build()

          validator.validateMainInfoByProvenOwner(
            owner,
            IdentifierType.LICENSE_PLATE,
            addedManually = true,
            newVehicleInfo,
            currentVehicleInfo,
            OfferInfo.newBuilder().build()
          )
        }
        result.nonEmpty shouldBe true
        result.size shouldBe 2 * NotOkForEditVerdict.size
        result.toSet.size shouldBe 2
        result.toSet shouldBe Set(VinEditForbiddenError, LicensePlateEditForbiddenError)
      }

      "newMark != currentMark with not ok for edit provenOwnerStatus" in {
        val result = NotOkForEditVerdict.flatMap { verdict =>
          val owner = GarageSchema.ProvenOwnerState
            .newBuilder()
            .setVerdict(verdict)
            .build()

          val currentVehicleInfo = GarageSchema.VehicleInfo
            .newBuilder()
            .setCatalog(VehicleInfo.AutoruCatalogData.newBuilder().setTechParamId(currentTechParamId).build())
            .setDocuments(
              VehicleInfo.Documents
                .newBuilder()
                .setVin(currentVin)
                .setLicensePlate(currentLp)
                .build()
            )
            .setTtx(
              CommonTtx
                .newBuilder()
                .setColor(currentColor)
                .setYear(currentProductionYear)
                .setMark(currentMark)
                .setModel(currentModel)
                .build()
            )
            .build()
          val newVehicleInfo = GarageApiModel.Vehicle
            .newBuilder()
            .setColor(newOldColor)
            .setDocuments(
              ApiOfferModel.Documents
                .newBuilder()
                .setVin(currentVin)
                .setYear(currentProductionYear)
                .build()
            )
            .setCarInfo(
              CarInfo.newBuilder().setTechParamId(currentTechParamId).setMark(newMark).setModel(currentModel).build()
            )
            .build()

          validator.validateMainInfoByProvenOwner(
            owner,
            IdentifierType.VIN,
            addedManually = false,
            newVehicleInfo,
            currentVehicleInfo,
            OfferInfo.newBuilder().build()
          )
        }
        result.nonEmpty shouldBe true
        result.size shouldBe NotOkForEditVerdict.size
        result.toSet.size shouldBe 1
        result.toSet.head shouldBe MarkEditForbiddenError
      }

      "newMark != currentMark and newModel != currentModel with not ok for edit provenOwnerStatus" in {
        val result = NotOkForEditVerdict.flatMap { verdict =>
          val owner = GarageSchema.ProvenOwnerState
            .newBuilder()
            .setVerdict(verdict)
            .build()

          val currentVehicleInfo = GarageSchema.VehicleInfo
            .newBuilder()
            .setCatalog(VehicleInfo.AutoruCatalogData.newBuilder().setTechParamId(currentTechParamId).build())
            .setDocuments(
              VehicleInfo.Documents
                .newBuilder()
                .setVin(currentVin)
                .setLicensePlate(currentLp)
                .build()
            )
            .setTtx(
              CommonTtx
                .newBuilder()
                .setColor(currentColor)
                .setYear(currentProductionYear)
                .setMark(currentMark)
                .setModel(currentModel)
                .build()
            )
            .build()
          val newVehicleInfo = GarageApiModel.Vehicle
            .newBuilder()
            .setColor(newOldColor)
            .setDocuments(
              ApiOfferModel.Documents
                .newBuilder()
                .setVin(currentVin)
                .setYear(currentProductionYear)
                .build()
            )
            .setCarInfo(
              CarInfo.newBuilder().setTechParamId(currentTechParamId).setMark(newMark).setModel(newModel).build()
            )
            .build()

          validator.validateMainInfoByProvenOwner(
            owner,
            IdentifierType.VIN,
            addedManually = false,
            newVehicleInfo,
            currentVehicleInfo,
            OfferInfo.newBuilder().build()
          )
        }
        result.nonEmpty shouldBe true
        result.size shouldBe NotOkForEditVerdict.size * 2
        result.toSet.size shouldBe 2
        result.toSet.contains(MarkEditForbiddenError) shouldBe true
        result.toSet.contains(ModelEditForbiddenError) shouldBe true
      }
      "newYear != currentYear with not ok for edit provenOwnerStatus" in {
        val result = NotOkForEditVerdict.flatMap { verdict =>
          val owner = GarageSchema.ProvenOwnerState
            .newBuilder()
            .setVerdict(verdict)
            .build()

          val currentVehicleInfo = GarageSchema.VehicleInfo
            .newBuilder()
            .setCatalog(VehicleInfo.AutoruCatalogData.newBuilder().setTechParamId(currentTechParamId).build())
            .setDocuments(
              VehicleInfo.Documents
                .newBuilder()
                .setVin(currentVin)
                .setLicensePlate(currentLp)
                .build()
            )
            .setTtx(
              CommonTtx
                .newBuilder()
                .setColor(currentColor)
                .setYear(currentProductionYear)
                .setMark(currentMark)
                .setModel(currentModel)
                .build()
            )
            .build()
          val newVehicleInfo = GarageApiModel.Vehicle
            .newBuilder()
            .setColor(newOldColor)
            .setDocuments(
              ApiOfferModel.Documents
                .newBuilder()
                .setVin(currentVin)
                .setYear(newProductionYear)
                .build()
            )
            .setCarInfo(
              CarInfo
                .newBuilder()
                .setTechParamId(currentTechParamId)
                .setMark(currentMark)
                .setModel(currentModel)
                .build()
            )
            .build()

          validator.validateMainInfoByProvenOwner(
            owner,
            IdentifierType.VIN,
            addedManually = false,
            newVehicleInfo,
            currentVehicleInfo,
            OfferInfo.newBuilder().build()
          )
        }
        result.nonEmpty shouldBe true
        result.size shouldBe NotOkForEditVerdict.size
        result.toSet.size shouldBe 1
        result.toSet.contains(ProductionYearEditForbiddenError) shouldBe true
      }
      "newColor != currentColor with not ok for edit provenOwnerStatus" in {
        val result = NotOkForEditVerdict.flatMap { verdict =>
          val owner = GarageSchema.ProvenOwnerState
            .newBuilder()
            .setVerdict(verdict)
            .build()

          val currentVehicleInfo = GarageSchema.VehicleInfo
            .newBuilder()
            .setCatalog(VehicleInfo.AutoruCatalogData.newBuilder().setTechParamId(currentTechParamId).build())
            .setDocuments(
              VehicleInfo.Documents
                .newBuilder()
                .setVin(currentVin)
                .setLicensePlate(currentLp)
                .build()
            )
            .setTtx(
              CommonTtx
                .newBuilder()
                .setColor(currentColor)
                .setYear(currentProductionYear)
                .setMark(currentMark)
                .setModel(currentModel)
                .build()
            )
            .build()
          val newVehicleInfo = GarageApiModel.Vehicle
            .newBuilder()
            .setColor(newColor)
            .setDocuments(
              ApiOfferModel.Documents
                .newBuilder()
                .setVin(currentVin)
                .setYear(currentProductionYear)
                .build()
            )
            .setCarInfo(
              CarInfo
                .newBuilder()
                .setTechParamId(currentTechParamId)
                .setMark(currentMark)
                .setModel(currentModel)
                .build()
            )
            .build()

          validator.validateMainInfoByProvenOwner(
            owner,
            IdentifierType.VIN,
            addedManually = false,
            newVehicleInfo,
            currentVehicleInfo,
            OfferInfo.newBuilder().build()
          )
        }
        result.nonEmpty shouldBe true
        result.size shouldBe NotOkForEditVerdict.size
        result.toSet.size shouldBe 1
        result.toSet.contains(ColorEditForbiddenError) shouldBe true
      }
      "newTechParamId != currentTechParamId with not ok for edit provenOwnerStatus" in {
        val result = NotOkForEditVerdict.flatMap { verdict =>
          val owner = GarageSchema.ProvenOwnerState
            .newBuilder()
            .setVerdict(verdict)
            .build()

          val currentVehicleInfo = GarageSchema.VehicleInfo
            .newBuilder()
            .setCatalog(VehicleInfo.AutoruCatalogData.newBuilder().setTechParamId(currentTechParamId).build())
            .setDocuments(
              VehicleInfo.Documents
                .newBuilder()
                .setVin(currentVin)
                .setLicensePlate(currentLp)
                .build()
            )
            .setTtx(
              CommonTtx
                .newBuilder()
                .setColor(currentColor)
                .setYear(currentProductionYear)
                .setMark(currentMark)
                .setModel(currentModel)
                .build()
            )
            .build()
          val newVehicleInfo = GarageApiModel.Vehicle
            .newBuilder()
            .setColor(newOldColor)
            .setDocuments(
              ApiOfferModel.Documents
                .newBuilder()
                .setVin(currentVin)
                .setYear(currentProductionYear)
                .build()
            )
            .setCarInfo(
              CarInfo.newBuilder().setTechParamId(newTechParamId).setMark(currentMark).setModel(currentModel).build()
            )
            .build()

          validator.validateMainInfoByProvenOwner(
            owner,
            IdentifierType.VIN,
            addedManually = false,
            newVehicleInfo,
            currentVehicleInfo,
            OfferInfo.newBuilder().build()
          )
        }
        result.nonEmpty shouldBe true
        result.size shouldBe NotOkForEditVerdict.size
        result.toSet.size shouldBe 1
        result.toSet.contains(TechParamIdEditForbiddenError) shouldBe true
      }
      "something change in vin, lp, mark or model with ok for edit provenOwnerStatus but ok status in active offer" in {
        val result = OkForEditVerdict.flatMap { verdict =>
          val owner = GarageSchema.ProvenOwnerState
            .newBuilder()
            .setVerdict(verdict)
            .build()

          val currentVehicleInfo = GarageSchema.VehicleInfo
            .newBuilder()
            .setDocuments(
              VehicleInfo.Documents
                .newBuilder()
                .setVin(currentVin)
                .setLicensePlate(currentLp)
                .build()
            )
            .setTtx(CommonTtx.newBuilder().setMark(currentMark).setModel(currentModel).build())
            .build()
          val newVehicleInfo = GarageApiModel.Vehicle
            .newBuilder()
            .setDocuments(
              ApiOfferModel.Documents
                .newBuilder()
                .setVin(newVin)
                .build()
            )
            .setCarInfo(CarInfo.newBuilder().setMark(currentMark).setModel(newModel).build())
            .build()

          validator.validateMainInfoByProvenOwner(
            owner,
            IdentifierType.VIN,
            addedManually = false,
            newVehicleInfo,
            currentVehicleInfo,
            OfferInfo.newBuilder().setStatus(OfferStatus.ACTIVE).setProvenOwnerStatus(ProvenOwnerStatus.OK).build()
          )
        }

        result.isEmpty shouldBe false
        result.toSet.size shouldBe 2
      }
    }

    "return success" when {
      "nothing change in vin, lp, mark, model with any provenOwnerStatus" in {
        val result = AllVerdict.flatMap { verdict =>
          val owner = GarageSchema.ProvenOwnerState
            .newBuilder()
            .setVerdict(verdict)
            .build()

          val currentVehicleInfo = GarageSchema.VehicleInfo
            .newBuilder()
            .setCatalog(VehicleInfo.AutoruCatalogData.newBuilder().setTechParamId(currentTechParamId).build())
            .setDocuments(
              VehicleInfo.Documents
                .newBuilder()
                .setVin(currentVin)
                .setLicensePlate(currentLp)
                .build()
            )
            .setTtx(CommonTtx.newBuilder().setColor(currentColor).setMark(currentMark).setModel(currentModel).build())
            .build()
          val newVehicleInfo = GarageApiModel.Vehicle
            .newBuilder()
            .setColor(newOldColor)
            .setDocuments(
              ApiOfferModel.Documents
                .newBuilder()
                .setVin(currentVin)
                .build()
            )
            .setCarInfo(
              CarInfo
                .newBuilder()
                .setTechParamId(currentTechParamId)
                .setMark(currentMark)
                .setModel(currentModel)
                .build()
            )
            .build()

          validator.validateMainInfoByProvenOwner(
            owner,
            IdentifierType.VIN,
            addedManually = false,
            newVehicleInfo,
            currentVehicleInfo,
            OfferInfo.newBuilder().build()
          )
        }
        result.isEmpty shouldBe true
      }

      "something change in vin, lp, mark or model with ok for edit provenOwnerStatus" in {
        val result = OkForEditVerdict.flatMap { verdict =>
          val owner = GarageSchema.ProvenOwnerState
            .newBuilder()
            .setVerdict(verdict)
            .build()

          val currentVehicleInfo = GarageSchema.VehicleInfo
            .newBuilder()
            .setDocuments(
              VehicleInfo.Documents
                .newBuilder()
                .setVin(currentVin)
                .setLicensePlate(currentLp)
                .build()
            )
            .setTtx(CommonTtx.newBuilder().setMark(currentMark).setModel(currentModel).build())
            .build()
          val newVehicleInfo = GarageApiModel.Vehicle
            .newBuilder()
            .setDocuments(
              ApiOfferModel.Documents
                .newBuilder()
                .setVin(newVin)
                .build()
            )
            .setCarInfo(CarInfo.newBuilder().setMark(currentMark).setModel(newModel).build())
            .build()

          validator.validateMainInfoByProvenOwner(
            owner,
            IdentifierType.VIN,
            addedManually = false,
            newVehicleInfo,
            currentVehicleInfo,
            OfferInfo.newBuilder().build()
          )
        }
        result.isEmpty shouldBe true
      }
    }

  }

  "validate mileage" should {
    "return date error" when {
      "mileage has not date" in {
        val mileage = Mileage.newBuilder().setValue(-1).build()

        val result = validator.validateMileage(mileage, List.empty)
        result.size shouldBe 1
        result.head shouldBe MileageDateValidationError
      }
      "mileage date less than last in history" in {
        val currentTimeMillis = System.currentTimeMillis()
        val lastMileage = GarageSchema.VehicleInfo.Mileage
          .newBuilder()
          .setValue(0)
          .setDate(Timestamps.fromMillis(currentTimeMillis))
          .build()

        val mileage = Mileage
          .newBuilder()
          .setValue(0)
          .setDate(Timestamps.fromMillis(currentTimeMillis - 1.day.toMillis))
          .build()

        val result = validator.validateMileage(mileage, List(lastMileage))
        result.size shouldBe 1
        result.head shouldBe MileageDateValidationError
      }
      "mileage date is 1 minute more or equal than last in history" in {
        val currentTimeMillis = System.currentTimeMillis()
        val lastMileage = GarageSchema.VehicleInfo.Mileage
          .newBuilder()
          .setValue(0)
          .setDate(Timestamps.fromMillis(currentTimeMillis))
          .build()

        val mileage = Mileage
          .newBuilder()
          .setValue(0)
          .setDate(Timestamps.fromMillis(currentTimeMillis - 1.minutes.toMillis))
          .build()

        val result = validator.validateMileage(mileage, List(lastMileage))
        result.size shouldBe 1
        result.head shouldBe MileageDateValidationError
      }
      "mileage date has large difference with current" ignore {
        val mileage = Mileage
          .newBuilder()
          .setValue(0)
          .setDate(Timestamps.fromMillis(System.currentTimeMillis() - 1.day.toMillis))
          .build()

        val result = validator.validateMileage(mileage, List.empty)
        result.size shouldBe 1
        result.head shouldBe MileageDateValidationError
      }
    }
    "return mileage value error" when {
      "too big mileage" in {
        val mileage = Mileage
          .newBuilder()
          .setValue(10000001)
          .setDate(Timestamps.fromMillis(System.currentTimeMillis()))
          .build()
        val result = validator.validateMileage(mileage, List.empty)

        result.size shouldBe 1
        result.head == MileageValidationError
      }
    }
    "return success" when {
      "mileage is empty" in {
        val mileage = Mileage
          .newBuilder()
          .build()

        val result = validator.validateMileage(mileage, List.empty)
        result.isEmpty shouldBe true
      }
      "mileage valid and history is empty" in {
        val mileage = Mileage
          .newBuilder()
          .setValue(78000)
          .setDate(Timestamps.fromMillis(System.currentTimeMillis()))
          .build()
        val result = validator.validateMileage(mileage, List.empty)

        result.isEmpty shouldBe true
      }
      "mileage date is 1 minute less than last in history" in {
        val currentTimeMillis = System.currentTimeMillis()
        val lastMileage = GarageSchema.VehicleInfo.Mileage
          .newBuilder()
          .setValue(0)
          .setDate(Timestamps.fromMillis(currentTimeMillis))
          .build()

        val mileage = Mileage
          .newBuilder()
          .setValue(0)
          .setDate(Timestamps.fromMillis(currentTimeMillis - 0.9.minutes.toMillis))
          .build()

        val result = validator.validateMileage(mileage, List(lastMileage))
        result.size shouldBe 0
      }
      "mileage valid and date consitent with history" in {
        val currentTimeMillis = System.currentTimeMillis()
        val lastMileage = GarageSchema.VehicleInfo.Mileage
          .newBuilder()
          .setValue(45000)
          .setDate(Timestamps.fromMillis(currentTimeMillis - 365.day.toMillis))
          .build()

        val mileage = Mileage
          .newBuilder()
          .setValue(78000)
          .setDate(Timestamps.fromMillis(currentTimeMillis))
          .build()
        val result = validator.validateMileage(mileage, List(lastMileage))

        result.isEmpty shouldBe true
      }
    }
  }

  "validate vin or lp required" should {
    "return error" when {
      "vin and lp is empty" in {
        val res = validator.validateVinOrLpRequred("", "")

        res.size shouldBe 1
        res.head shouldBe RequiredVinOrLpError
      }
      "vin and lp not valid" in {
        val res = validator.validateVinOrLpRequred("1", "A")

        res.size shouldBe 1
        res.head shouldBe RequiredVinOrLpError
      }
    }
    "return success" when {
      "vin valid" in {
        val res = validator.validateVinOrLpRequred("WMWSV31000T452990", "A")

        res.size shouldBe 0
      }
      "lp valid" in {
        val res = validator.validateVinOrLpRequred("0", "T700TT62")

        res.size shouldBe 0
      }
    }
  }

  "validate duplicated vin" should {
    "return success" when {
      "existed cards is empty" in {
        for {
          id <- List(None, Some(1L))
        } yield {
          val res = validator.validateDuplicatedVin(id, "WMWSV31000T452990", List.empty)

          res.size shouldBe 0
        }
      }
      "existed cards do not contains same vin" in {
        val history = List(
          CardEssentials(1, GarageCard.Status.ACTIVE, Some(VinCode("WDD2210561A232855")), None, CardType.CURRENT_CAR),
          CardEssentials(2, GarageCard.Status.ACTIVE, Some(VinCode("X4XFH611X0L995921")), None, CardType.EX_CAR)
        )

        val res = validator.validateDuplicatedVin(Some(1), "WDD2210561A232855", history)

        res.size shouldBe 0
      }
      "existed active cards do not contains same vin" in {
        val history = List(
          CardEssentials(2, GarageCard.Status.DELETED, Some(VinCode("WDD2210561A232855")), None, CardType.EX_CAR)
        )

        val res = validator.validateDuplicatedVin(None, "WDD2210561A232855", history)

        res.size shouldBe 0
      }
    }
    "return error" when {
      "there are active card with same vin" in {
        val vin = "WDD2210561A232855"

        val history = List(
          CardEssentials(2, GarageCard.Status.ACTIVE, Some(VinCode(vin)), None, CardType.CURRENT_CAR)
        )

        for {
          id <- List(Some(1L), None)
        } yield {
          val res = validator.validateDuplicatedVin(id, vin, history)

          res.size shouldBe 1
          res.head shouldBe VinDuplicatedError
        }
      }
    }
  }

  "validate duplicated LP" should {
    "return success" when {
      "existed cards is empty" in {
        for {
          id <- List(None, Some(1L))
        } yield {
          val res = validator.validateDuplicatedLp(id, "A123AA799", List.empty)

          res.size shouldBe 0
        }
      }
      "existed cards do not contains same LP" in {
        val history = List(
          CardEssentials(1, GarageCard.Status.ACTIVE, None, Some(LicensePlate("A123AA799")), CardType.EX_CAR),
          CardEssentials(2, GarageCard.Status.ACTIVE, None, Some(LicensePlate("A123AA798")), CardType.CURRENT_CAR)
        )

        val res = validator.validateDuplicatedLp(Some(2), "A123AA798", history)

        res.size shouldBe 0
      }
      "existed active cards do not contains same LP" in {
        val history = List(
          CardEssentials(2, GarageCard.Status.DELETED, None, Some(LicensePlate("A123AA799")), CardType.EX_CAR)
        )

        val res = validator.validateDuplicatedLp(None, "A123AA799", history)

        res.size shouldBe 0
      }
      "ignore EX_CAR with same LP" in {
        val history = List(
          CardEssentials(1, GarageCard.Status.ACTIVE, None, Some(LicensePlate("A123AA799")), CardType.EX_CAR),
          CardEssentials(2, GarageCard.Status.ACTIVE, None, Some(LicensePlate("A123AA799")), CardType.EX_CAR)
        )

        val res = validator.validateDuplicatedLp(None, "A123AA799", history)

        res.size shouldBe 0
      }
    }
    "return error" when {
      "there are active CURRENT card with same LP" in {
        val LP = "A123AA799"

        val history = List(
          CardEssentials(2, GarageCard.Status.ACTIVE, None, Some(LicensePlate("A123AA799")), CardType.CURRENT_CAR)
        )

        for {
          id <- List(Some(1L), None)
        } yield {
          val res = validator.validateDuplicatedLp(id, LP, history)

          res.size shouldBe 1
          res.head shouldBe LpDuplicatedError
        }
      }
    }
  }

  "validate vin" should {
    "return forbidden vin edit" when {
      "for not manually added card if vin changed" in {
        val optCurrentVin = Some("ZPBEA1ZL8KLA02286")
        val newVin = "ZPBEA1ZL8KLA02287"
        for {
          addedBy <- List(IdentifierType.VIN, IdentifierType.LICENSE_PLATE)
        } yield {
          val result = validator.validateVin(newVin, addedBy, false, optCurrentVin)

          result.size shouldBe 1
          result.head == VinEditForbiddenError
        }
      }
    }
    "return required vin" when {
      "vin is empty and card added by vin from standalone" in {
        val result = validator.validateVin("", IdentifierType.VIN, false, None)

        result.size shouldBe 1
        result.head shouldBe RequiredVinError
      }

    }
    "return invalid vin" when {
      "vin is invalid and card manually added by vin or grz" in {
        for {
          optCurrentVin <- List(Some("ZPBEA1ZL8KLA02286"), None)
          addedBy <- List(IdentifierType.VIN, IdentifierType.LICENSE_PLATE)
        } yield {
          val result = validator.validateVin("A1", addedBy, true, optCurrentVin)

          result.size shouldBe 1
          result.head shouldBe InvalidVinError
        }
      }
      "vin is invalid and card added by vin from standalone" in {
        for {
          addedBy <- List(IdentifierType.VIN, IdentifierType.LICENSE_PLATE)
        } yield {
          val result = validator.validateVin("A1", addedBy, false, None)

          result.size shouldBe 1
          result.head shouldBe InvalidVinError
        }
      }
    }
    "return success" when {
      "vin is masked or empty and card added by license plate" in {
        for {
          vin <- List("", "ZPB**1Z*8KLA0*286")
          optCurrentVin <- List(None, Some("ZPBEA1ZL8KLA02286"))
        } yield {
          val result = validator.validateVin(vin, IdentifierType.LICENSE_PLATE, false, optCurrentVin)

          result.size shouldBe 0
        }
      }
      "vin not changed" in {
        val vin = "ZPBEA1ZL8KLA02287"
        for {
          addedBy <- List(IdentifierType.VIN, IdentifierType.LICENSE_PLATE)
          manuallyAdded <- List(false, true)
        } yield {
          val result = validator.validateVin(vin, addedBy, manuallyAdded, Some(vin))

          result.size shouldBe 0
        }
      }
      "vin valid and card added manually" in {
        for {
          addedBy <- List(IdentifierType.VIN, IdentifierType.LICENSE_PLATE)
          optCurrentVin <- List(None, Some("ZPBEA1ZL8KLA02281"))
        } yield {
          val result = validator.validateVin("ZPBEA1ZL8KLA02287", addedBy, true, optCurrentVin)

          result.size shouldBe 0
        }
      }
    }
  }

  "validate license plate" should {
    "return forbidden license plate edit" when {
      "for not manually added card if license plate changed" in {
        val optCurrentLp = Some("T700TT62")
        val newLp = "A123AA24"
        for {
          addedBy <- List(IdentifierType.VIN, IdentifierType.LICENSE_PLATE)
        } yield {
          val result = validator.validateLicensePlate(newLp, addedBy, false, optCurrentLp)

          result.size shouldBe 1
          result.head == LicensePlateEditForbiddenError
        }
      }
    }
    "return required license plate" when {
      "lp is empty and card added by lp from standalone" in {
        val result = validator.validateLicensePlate("", IdentifierType.LICENSE_PLATE, false, None)

        result.size shouldBe 1
        result.head shouldBe RequiredLicensePlateError
      }

    }
    "return invalid license plate" when {
      "license plate is invalid and card manually added by vin or lp" in {
        for {
          optCurrentLp <- List(Some("T700TT62"), None)
          addedBy <- List(IdentifierType.VIN, IdentifierType.LICENSE_PLATE)
        } yield {
          val result = validator.validateLicensePlate("A1", addedBy, true, optCurrentLp)

          result.size shouldBe 1
          result.head shouldBe InvalidLpError
        }
      }
      "lp is invalid and card added by lp from standalone" in {
        for {
          addedBy <- List(IdentifierType.VIN, IdentifierType.LICENSE_PLATE)
        } yield {
          val result = validator.validateLicensePlate("A1", addedBy, false, None)

          result.size shouldBe 1
          result.head shouldBe InvalidLpError
        }
      }
    }
    "return success" when {
      "lp is masked or empty and card added by vin" in {
        for {
          vin <- List("", "T***AA**")
          optCurrentLp <- List(None, Some("T700TT62"))
        } yield {
          val result = validator.validateLicensePlate(vin, IdentifierType.VIN, false, optCurrentLp)

          result.size shouldBe 0
        }
      }
      "lp not changed" in {
        val lp = "T700TT62"
        for {
          addedBy <- List(IdentifierType.VIN, IdentifierType.LICENSE_PLATE)
          manuallyAdded <- List(false, true)
        } yield {
          val result = validator.validateVin(lp, addedBy, manuallyAdded, Some(lp))

          result.size shouldBe 0
        }
      }
      "lp valid and card added manually" in {
        for {
          addedBy <- List(IdentifierType.VIN, IdentifierType.LICENSE_PLATE)
          optCurrentLp <- List(None, Some("A123AA24"))
        } yield {
          val result = validator.validateLicensePlate("T700TT62", addedBy, true, optCurrentLp)

          result.size shouldBe 0
        }
      }
    }
  }

  "validate year" should {
    "return errors" when {
      "year more than current" in {
        val year = Year.now().getValue + 1
        val result = validator.validateYear(year)

        result.size shouldBe 1
        result.head.isInstanceOf[YearValidationError] shouldBe true
      }
      "year less than 1940" in {
        val year = 1939
        val result = validator.validateYear(year)

        result.size shouldBe 1
        result.head.isInstanceOf[YearValidationError] shouldBe true
      }
    }
  }

  "validate owners" should {
    "return error" when {
      "owners count more than 50" in {
        val result = validator.validateOwnerCount(51)

        result.size shouldBe 1
        result.head.isInstanceOf[OwnersCountValidationError] shouldBe true

      }
      "owners count less than zero" in {
        val result = validator.validateOwnerCount(-5)

        result.size shouldBe 1
        result.head.isInstanceOf[OwnersCountValidationError] shouldBe true

        val error = result.head.asInstanceOf[OwnersCountValidationError]
        error.max shouldBe 50
        error.min shouldBe 1
      }
    }
    "return success" when {
      "owners count between 1 and 10" in {
        val result = validator.validateOwnerCount(4)

        result.size shouldBe 0
      }
    }
  }

  private val now = System.currentTimeMillis()

  "validate insurances" should {
    "return error" when {

      "insurances have duplicate by id" in {
        val firstInsurance = buildGarageApiModelInsurance(
          serial = "XXX",
          number = "1023040506",
          fromOpt = Some(now - 200.day.toMillis),
          toOpt = Some(now + 90.day.toMillis)
        )

        val secondInsurance = buildGarageApiModelInsurance(
          serial = "XXX",
          number = "1023040506",
          fromOpt = Some(now - 300.day.toMillis),
          toOpt = Some(now - 10.day.toMillis)
        )

        val currentInsuranceInfo =
          GarageApiModel.InsuranceInfo
            .newBuilder()
            .addAllInsurances(List(firstInsurance, secondInsurance).asJava)
            .build()

        val result = validator.validateInsurances(currentInsuranceInfo)

        result.size shouldBe 1
        result.contains(ValidateInsuranceDuplicateError) shouldBe true
      }

      "insurance period is too long and type of insurance is OSAGO" in {
        val currentInsurance = buildGarageApiModelInsurance(
          serial = "XXX",
          number = "1023040506",
          fromOpt = Some(now - 300.day.toMillis),
          toOpt = Some(now + 90.day.toMillis)
        )

        val currentInsuranceInfo =
          GarageApiModel.InsuranceInfo.newBuilder().addAllInsurances(List(currentInsurance).asJava).build()

        val result = validator.validateInsurances(currentInsuranceInfo)

        result.size shouldBe 1
        result.head.isInstanceOf[InsurancePeriodValidationError] shouldBe true
      }

      "insurance period is short and type of insurance is OSAGO" in {
        val currentInsurance = buildGarageApiModelInsurance(
          serial = "XXX",
          number = "1023040506",
          fromOpt = Some(now),
          toOpt = Some(now + 3.day.toMillis)
        )
        val currentInsuranceInfo =
          GarageApiModel.InsuranceInfo.newBuilder().addAllInsurances(List(currentInsurance).asJava).build()

        val result = validator.validateInsurances(currentInsuranceInfo)

        result.size shouldBe 1
        result.head.isInstanceOf[InsurancePeriodValidationError] shouldBe true
      }

      "return error when too many insurances added" in {
        val currentInsurances =
          for (i <- 0 to 100)
            yield GarageApiModel.Insurance
              .newBuilder()
              .setNumber(i.toString)
              .setInsuranceType(InsuranceType.KASKO)
              .build()
        val currentInsuranceInfo =
          GarageApiModel.InsuranceInfo.newBuilder().addAllInsurances(currentInsurances.asJava).build()

        val existedInsurances =
          for (j <- 0 to 32)
            yield GarageSchema.Insurance
              .newBuilder()
              .setNumber((500 + j).toString)
              .setInsuranceType(InsuranceType.KASKO)
              .build()
        val existedInsuranceInfo =
          GarageSchema.InsuranceInfo.newBuilder().addAllInsurances(existedInsurances.asJava).build()

        val result = validator.validateInsurancesWithExisted(currentInsuranceInfo, existedInsuranceInfo)

        result.size shouldBe 1
        result.head shouldBe InsurancesLimitError
      }

      "return error when too many not deleted insurances" in {
        val currentInsurances =
          for (j <- 0 to 60)
            yield GarageApiModel.Insurance
              .newBuilder()
              .setNumber((500 + j).toString)
              .setInsuranceType(InsuranceType.KASKO)
              .build()

        val currentInsuranceInfo =
          GarageApiModel.InsuranceInfo.newBuilder().addAllInsurances(currentInsurances.asJava).build()

        val existedInsurances =
          for (i <- 0 to 52)
            yield GarageSchema.Insurance
              .newBuilder()
              .setNumber(i.toString)
              .setInsuranceType(InsuranceType.KASKO)
              .build()

        val existedInsuranceInfo =
          GarageSchema.InsuranceInfo.newBuilder().addAllInsurances(existedInsurances.asJava).build()

        val result = validator.validateInsurancesWithExisted(currentInsuranceInfo, existedInsuranceInfo)

        result.size shouldBe 1
        result.head shouldBe InsurancesLimitError
      }

      "insurance's fields from and to is empty" in {
        val newInsurance = buildGarageApiModelInsurance(
          serial = "XXX",
          number = "1023040506",
          fromOpt = None,
          toOpt = None
        )

        val result = validator.validateInsurances(
          GarageApiModel.InsuranceInfo
            .newBuilder()
            .addAllInsurances(
              List(newInsurance).asJava
            )
            .build()
        )
        result.size shouldBe 3
        result.contains(ValidateInsuranceFromFieldError) shouldBe true
        result.contains(ValidateInsuranceToFieldError) shouldBe true
        result.exists(el => el.isInstanceOf[InsurancePeriodValidationError]) shouldBe true
      }

      "insurance's field serial is empty and insurance type is OSAGO" in {
        import scala.concurrent.duration._
        val newInsurance = buildGarageApiModelInsurance(
          serial = " ",
          number = "1234567899",
          fromOpt = Some(now - 40.day.toMillis),
          toOpt = Some(now - 20.day.toMillis)
        )
        val result = validator.validateInsurances(
          GarageApiModel.InsuranceInfo
            .newBuilder()
            .addAllInsurances(
              List(newInsurance).asJava
            )
            .build()
        )
        result.size shouldBe 1
        result.contains(ValidateInsuranceSerialFieldError) shouldBe true
      }

      "insurance's field number is empty" in {
        import scala.concurrent.duration._
        val newInsurance = buildGarageApiModelInsurance(
          serial = "XXX",
          number = " ",
          fromOpt = Some(now - 40.day.toMillis),
          toOpt = Some(now - 20.day.toMillis)
        )

        val result = validator.validateInsurances(
          GarageApiModel.InsuranceInfo
            .newBuilder()
            .addAllInsurances(
              List(newInsurance).asJava
            )
            .build()
        )
        result.size shouldBe 1
        result.contains(ValidateInsuranceNumberFieldError) shouldBe true
      }

      "insurance's field from > to" in {
        import scala.concurrent.duration._
        val newInsurance = buildGarageApiModelInsurance(
          serial = "XXX",
          number = "1234567891",
          fromOpt = Some(now - 10.day.toMillis),
          toOpt = Some(now - 20.day.toMillis)
        )

        val result = validator.validateInsurances(
          GarageApiModel.InsuranceInfo
            .newBuilder()
            .addAllInsurances(
              List(newInsurance).asJava
            )
            .build()
        )
        result.size shouldBe 2
        result.exists(el => el.isInstanceOf[ValidateFromRelateToError]) shouldBe true
        result.exists(el => el.isInstanceOf[InsurancePeriodValidationError]) shouldBe true
      }
    }

    "return success" when {

      "insurances have not duplicate by id" in {
        val firstInsurance = buildGarageApiModelInsurance(
          serial = "XXX",
          number = "00000404030",
          fromOpt = Some(now - 200.day.toMillis),
          toOpt = Some(now + 90.day.toMillis)
        )

        val secondInsurance = buildGarageApiModelInsurance(
          serial = "XXX",
          number = "1023040506",
          fromOpt = Some(now - 300.day.toMillis),
          toOpt = Some(now - 10.day.toMillis)
        )

        val currentInsuranceInfo =
          GarageApiModel.InsuranceInfo
            .newBuilder()
            .addAllInsurances(List(firstInsurance, secondInsurance).asJava)
            .build()

        val result = validator.validateInsurances(currentInsuranceInfo)

        result.size shouldBe 0
      }

      "insurances have not duplicate by id, validation how that validation work with many types of insurance plans" in {
        val firstInsurance = buildGarageApiModelInsurance(
          serial = "XXX",
          number = "1023040506",
          fromOpt = Some(now - 200.day.toMillis),
          toOpt = Some(now + 90.day.toMillis)
        )

        val secondInsurance = buildGarageApiModelInsurance(
          serial = "XXX",
          number = "1023040506",
          fromOpt = Some(now - 300.day.toMillis),
          toOpt = Some(now - 10.day.toMillis),
          insuranceType = InsuranceType.KASKO
        )

        val currentInsuranceInfo =
          GarageApiModel.InsuranceInfo
            .newBuilder()
            .addAllInsurances(List(firstInsurance, secondInsurance).asJava)
            .build()

        val result = validator.validateInsurances(currentInsuranceInfo)

        result.size shouldBe 0
      }

      "not error when insurance period is fit or type of insurance is not OSAGO" in {
        val currentInsuranceKASKO = buildGarageApiModelInsurance(
          serial = "XXX",
          number = "1023040506",
          fromOpt = Some(now),
          toOpt = Some(now + 3.day.toMillis),
          insuranceType = InsuranceType.KASKO
        )
        val currentInsuranceOSAGO = buildGarageApiModelInsurance(
          serial = "ssws",
          number = "4333",
          fromOpt = Some(now),
          toOpt = Some(now + 5.day.toMillis)
        )

        val currentInsuranceInfo =
          GarageApiModel.InsuranceInfo
            .newBuilder()
            .addAllInsurances(List(currentInsuranceOSAGO, currentInsuranceKASKO).asJava)
            .build()

        val result = validator.validateInsurances(currentInsuranceInfo)

        result.size shouldBe 0
      }

      "no error if the limit of insurances is not exceeded" in {
        val currentInsurances =
          for (j <- 0 to 40)
            yield GarageApiModel.Insurance
              .newBuilder()
              .setNumber((500 + j).toString)
              .setSerial(s"${500 + j}-seria")
              .setInsuranceType(InsuranceType.OSAGO)
              .build()

        val currentInsuranceInfo =
          GarageApiModel.InsuranceInfo.newBuilder().addAllInsurances(currentInsurances.asJava).build()

        val existedInsurances =
          for (i <- 0 to 13)
            yield GarageSchema.Insurance
              .newBuilder()
              .setNumber(i.toString)
              .setSerial(s"$i-seria")
              .setInsuranceType(InsuranceType.OSAGO)
              .build()

        val existedInsuranceInfo =
          GarageSchema.InsuranceInfo.newBuilder().addAllInsurances(existedInsurances.asJava).build()

        val result = validator.validateInsurancesWithExisted(currentInsuranceInfo, existedInsuranceInfo)

        result.size shouldBe 0
        result shouldBe List.empty[CardValidationError]
      }

      "insurance's fields from and to non empty" in {
        import scala.concurrent.duration._
        val newInsurance = buildGarageApiModelInsurance(
          serial = "XXX",
          number = "1023040506",
          fromOpt = Some(now - 40.day.toMillis),
          toOpt = Some(now - 20.day.toMillis)
        )
        val result = validator.validateInsurances(
          GarageApiModel.InsuranceInfo
            .newBuilder()
            .addAllInsurances(
              List(newInsurance).asJava
            )
            .build()
        )
        result.size shouldBe 0
      }

      "insurance's field serial non empty or insurance type is not OSAGO" in {
        import scala.concurrent.duration._
        val newInsuranceOSAGO = buildGarageApiModelInsurance(
          serial = "ZZZ",
          number = "1234567899",
          fromOpt = Some(now - 40.day.toMillis),
          toOpt = Some(now - 20.day.toMillis)
        )

        val newInsuranceKASKO = buildGarageApiModelInsurance(
          serial = " ",
          number = "45345534",
          fromOpt = Some(now - 40.day.toMillis),
          toOpt = Some(now - 20.day.toMillis),
          insuranceType = InsuranceType.KASKO
        )
        val result = validator.validateInsurances(
          GarageApiModel.InsuranceInfo
            .newBuilder()
            .addAllInsurances(
              List(newInsuranceOSAGO, newInsuranceKASKO).asJava
            )
            .build()
        )
        result.size shouldBe 0
      }

      "insurance's field number non empty" in {
        import scala.concurrent.duration._
        val newInsurance = buildGarageApiModelInsurance(
          serial = "XXX",
          number = "1234567891",
          fromOpt = Some(now - 40.day.toMillis),
          toOpt = Some(now - 20.day.toMillis)
        )

        val result = validator.validateInsurances(
          GarageApiModel.InsuranceInfo
            .newBuilder()
            .addAllInsurances(
              List(newInsurance).asJava
            )
            .build()
        )
        result.size shouldBe 0
      }

      "insurance's field from < to" in {
        import scala.concurrent.duration._
        val newInsurance = buildGarageApiModelInsurance(
          serial = "XXX",
          number = "1234567891",
          fromOpt = Some(now - 40.day.toMillis),
          toOpt = Some(now - 20.day.toMillis)
        )

        val result = validator.validateInsurances(
          GarageApiModel.InsuranceInfo
            .newBuilder()
            .addAllInsurances(
              List(newInsurance).asJava
            )
            .build()
        )
        result.size shouldBe 0
      }
    }
  }

  "validate car info" should {
    "return error" when {
      "mark is not set" in {
        when(catalog.findFirst(?)).thenReturn(None)
        val carInfo = CarInfo.newBuilder().build()
        val result = validator.validateCarInfoConsistency(carInfo)

        result.size shouldBe 1
        result.head shouldBe RequiredMarkError
      }

      "nothing set" in {
        when(catalog.findFirst(?)).thenReturn(None)
        val carInfo = CarInfo.newBuilder().build()
        val result = validator.validateCarInfoConsistency(carInfo)

        result.size shouldBe 1
        result.head shouldBe RequiredMarkError
      }

      "mark is not set and tech param is set" in {
        when(catalog.findFirst(?)).thenReturn(None)
        val carInfo = CarInfo.newBuilder().setTechParamId(1).build()
        val result = validator.validateCarInfoConsistency(carInfo)

        result.size shouldBe 1
        result.head shouldBe TechParamFieldInconsistentError(carInfo)
      }

      "mark is not set and model is set" in {
        when(catalog.findFirst(?)).thenReturn(None)
        val carInfo = CarInfo.newBuilder().setModel("LOGAN").build()
        val result = validator.validateCarInfoConsistency(carInfo)

        result.size shouldBe 1
        result.head shouldBe ModelFieldInconsistentError(carInfo)
      }

      "tech param is not consistent with mark" in {
        when(catalog.findFirst(?)).thenReturn(None)
        val carInfo = CarInfo.newBuilder().setMark("RENAULT").setTechParamId(1).build()
        val result = validator.validateCarInfoConsistency(carInfo)

        result.size shouldBe 1
        result.head shouldBe TechParamFieldInconsistentError(carInfo)
      }

      "tech param is not consistent with mark and model" in {
        val carInfo = CarInfo.newBuilder().setMark("RENAULT").setModel("LOGAN").setTechParamId(1).build()
        when(catalog.findFirst(?)).thenReturn(None)
        val result = validator.validateCarInfoConsistency(carInfo)

        result.size shouldBe 1
        result.head shouldBe TechParamFieldInconsistentError(carInfo)
      }

      "tech param is not consistent with super gen" in {
        val superGenId = 2
        val techParamId = 1
        val carInfo = CarInfo
          .newBuilder()
          .setMark("RENAULT")
          .setModel("LOGAN")
          .setTechParamId(techParamId)
          .setSuperGenId(superGenId)
          .build()
        when(catalog.findFirst(?)).thenReturn(None)
        val result = validator.validateCarInfoConsistency(carInfo)

        result.size shouldBe 1
        result.head shouldBe TechParamFieldInconsistentError(carInfo) // техпарам не совпадет с поколением
      }
    }

    "return success" when {
      "mark is set and model is not set" in {
        val carInfo = CarInfo.newBuilder().setMark("RENAULT").build()
        val card = mock[CarCard]
        when(catalog.findFirst(?)).thenReturn(Some(card))
        val result = validator.validateCarInfoConsistency(carInfo)

        result.size shouldBe 0
      }

      "tech param is consistent with super gen and mark/model" in {
        val superGenId = 2
        val techParamId = 1
        val carInfo = CarInfo
          .newBuilder()
          .setMark("RENAULT")
          .setModel("LOGAN")
          .setTechParamId(techParamId)
          .setSuperGenId(superGenId)
          .build()
        val card = mock[CarCard]
        when(catalog.findFirst(?)).thenReturn(Some(card))
        val result = validator.validateCarInfoConsistency(carInfo)

        result.size shouldBe 0
      }
    }
  }

  "validate car info for Dream Car" should {
    "return error" when {
      "super_gen is not set" in {
        when(catalog.findFirst(?)).thenReturn(None)
        val carInfo = CarInfo.newBuilder().setMark("LADA").setModel("Vesta").build()
        val result = validator.validateDreamCarInfoConsistency(carInfo)

        result.size shouldBe 1
        result.head shouldBe RequiredSuperGenError
      }

      "model is not set" in {
        when(catalog.findFirst(?)).thenReturn(None)
        val carInfo = CarInfo.newBuilder().setSuperGenId(1).setMark("LADA").build()
        val result = validator.validateDreamCarInfoConsistency(carInfo)

        result.size shouldBe 1
        result.head shouldBe RequiredModelError
      }

      "mark is not set" in {
        when(catalog.findFirst(?)).thenReturn(None)
        val carInfo = CarInfo.newBuilder().build()
        val result = validator.validateDreamCarInfoConsistency(carInfo)

        result.size shouldBe 1
        result.head shouldBe RequiredMarkError
      }

      "nothing set" in {
        when(catalog.findFirst(?)).thenReturn(None)
        val carInfo = CarInfo.newBuilder().build()
        val result = validator.validateDreamCarInfoConsistency(carInfo)

        result.size shouldBe 1
        result.head shouldBe RequiredMarkError
      }

      "tech param is not consistent with all require field" in {
        val superGenId = 2
        val techParamId = 1
        val carInfo = CarInfo
          .newBuilder()
          .setMark("RENAULT")
          .setModel("LOGAN")
          .setTechParamId(techParamId)
          .setSuperGenId(superGenId)
          .build()
        when(catalog.findFirst(?)).thenReturn(None)
        val result = validator.validateDreamCarInfoConsistency(carInfo)

        result.size shouldBe 1
        result.head shouldBe TechParamFieldInconsistentError(carInfo) // техпарам не совпадет с поколением
      }

      "complectation param is not consistent with all require field" in {
        val superGenId = 2
        val complectationParamId = 1
        val carInfo = CarInfo
          .newBuilder()
          .setMark("RENAULT")
          .setModel("LOGAN")
          .setSuperGenId(superGenId)
          .setComplectationId(complectationParamId)
          .build()
        when(catalog.findFirst(?)).thenReturn(None)
        val result = validator.validateDreamCarInfoConsistency(carInfo)

        result.size shouldBe 1
        result.head shouldBe ComplectationFieldInconsistentError(carInfo)
      }

      "super gen param is not consistent with all require field" in {
        val superGenId = 2
        val carInfo = CarInfo
          .newBuilder()
          .setMark("RENAULT")
          .setModel("LOGAN")
          .setSuperGenId(superGenId)
          .build()
        when(catalog.findFirst(?)).thenReturn(None)
        val result = validator.validateDreamCarInfoConsistency(carInfo)

        result.size shouldBe 1
        result.head shouldBe SuperGenFieldInconsistentError(carInfo)
      }

      "configuration param is not consistent with all require field" in {
        val superGenId = 2
        val configurationParamId = 1
        val carInfo = CarInfo
          .newBuilder()
          .setMark("RENAULT")
          .setModel("LOGAN")
          .setSuperGenId(superGenId)
          .setConfigurationId(configurationParamId)
          .build()
        when(catalog.findFirst(?)).thenReturn(None)
        val result = validator.validateDreamCarInfoConsistency(carInfo)

        result.size shouldBe 1
        result.head shouldBe ConfigurationFieldInconsistentError(carInfo)
      }
    }

    "return success" when {
      "tech param is consistent with super gen and mark/model" in {
        val superGenId = 2
        val techParamId = 1
        val carInfo = CarInfo
          .newBuilder()
          .setMark("RENAULT")
          .setModel("LOGAN")
          .setTechParamId(techParamId)
          .setSuperGenId(superGenId)
          .build()
        val card = mock[CarCard]
        when(catalog.findFirst(?)).thenReturn(Some(card))
        val result = validator.validateDreamCarInfoConsistency(carInfo)

        result.size shouldBe 0
      }
    }
  }

  "validate images" should {
    val invalidName = "NotValid-name"
    val invalidNamespace = "123-namespace"
    val invalidGroupId = 0
    val validName = "1name23"
    val validGroupId = 1
    val validNamespace = "valid-namespace"

    "return error" when {
      "name is not valid" in {
        val images = List(
          Photo
            .newBuilder()
            .setMdsPhotoInfo(
              MdsPhotoInfo
                .newBuilder()
                .setName(invalidName)
                .setGroupId(validGroupId)
                .setNamespace(validNamespace)
                .build()
            )
            .build()
        )
        val result = validator.validateImages(images)

        result.size shouldBe 1
        result.head shouldBe InvalidImage(validNamespace, validGroupId, invalidName)
      }
      "groupId is not valid" in {
        val images = List(
          Photo
            .newBuilder()
            .setMdsPhotoInfo(
              MdsPhotoInfo
                .newBuilder()
                .setName(validName)
                .setGroupId(invalidGroupId)
                .setNamespace(validNamespace)
                .build()
            )
            .build()
        )
        val result = validator.validateImages(images)

        result.size shouldBe 1
        result.head shouldBe InvalidImage(validNamespace, invalidGroupId, validName)
      }
      "namespace is not valid" in {
        val images = List(
          Photo
            .newBuilder()
            .setMdsPhotoInfo(
              MdsPhotoInfo
                .newBuilder()
                .setName(validName)
                .setGroupId(validGroupId)
                .setNamespace(invalidNamespace)
                .build()
            )
            .build()
        )
        val result = validator.validateImages(images)

        result.size shouldBe 1
        result.head shouldBe InvalidImage(invalidNamespace, validGroupId, validName)
      }
    }

    "return empty error list" when {
      "name, namespace, groupId is valid" in {
        val images = List(
          Photo
            .newBuilder()
            .setMdsPhotoInfo(
              MdsPhotoInfo
                .newBuilder()
                .setName(validName)
                .setGroupId(validGroupId)
                .setNamespace(validNamespace)
                .build()
            )
            .build()
        )
        val result = validator.validateImages(images)

        result shouldBe List.empty[CardValidationError]
      }

      "no images" in {
        val images = List.empty[Photo]
        val result = validator.validateImages(images)

        result shouldBe List.empty[CardValidationError]
      }

      "return error when too many images added" in {
        val images = for (_ <- 0 to 100) yield Photo.newBuilder().build()
        val result =
          validator.validateImagesQuantity(Vehicle.newBuilder().addAllVehicleImages(images.toList.asJava).build())
        result.size shouldBe 1
        result.head shouldBe ImagesLimitError
      }

      "no error if the limit is not exceeded" in {
        val images = for (_ <- 1 to 39) yield Photo.newBuilder().build()
        val result =
          validator.validateImagesQuantity(Vehicle.newBuilder().addAllVehicleImages(images.toList.asJava).build())
        result shouldBe List.empty[CardValidationError]
      }
    }
  }

  "validate documents photos" should {
    val invalidNamespace = "vs-namespace"
    val validName = "abc123"
    val validGroupId = 1
    val validNamespace = "vs-support-pd"

    val photo = Photo
      .newBuilder()
      .setMdsPhotoInfo(
        MdsPhotoInfo
          .newBuilder()
          .setName(validName)
          .setGroupId(validGroupId)
          .setNamespace(validNamespace)
          .build()
      )
      .build()

    "return empty error list" when {
      "namespace is valid" in {

        val owner = ProvenOwnerState
          .newBuilder()
          .setDocumentsPhotos(
            DocumentsPhotos
              .newBuilder()
              .setPhotos(
                Photos
                  .newBuilder()
                  .setDrivingLicense(photo)
                  .setStsFront(photo)
                  .setStsBack(photo)
              )
          )
          .build()

        val result = validator.validateDocumentsPhotos(owner)
        result shouldBe Nil
      }
      "validation completed" in {
        val owner = ProvenOwnerState
          .newBuilder()
          .setDocumentsPhotos(
            DocumentsPhotos
              .newBuilder()
              .setUploadedAt(Timestamps.fromMillis(now))
          )
          .setStatus(ProvenOwnerState.ProvenOwnerStatus.OK)
          .build()

        val result = validator.validateDocumentsPhotos(owner)
        result shouldBe Nil
      }
    }

    "return error" when {
      "namespace is invalid" in {
        val invalidPhoto = Photo
          .newBuilder()
          .setMdsPhotoInfo(
            MdsPhotoInfo
              .newBuilder()
              .setName(validName)
              .setGroupId(validGroupId)
              .setNamespace(invalidNamespace)
              .build()
          )
          .build()

        val invalidOwner = ProvenOwnerState
          .newBuilder()
          .setDocumentsPhotos(
            DocumentsPhotos
              .newBuilder()
              .setPhotos(
                Photos
                  .newBuilder()
                  .setDrivingLicense(invalidPhoto)
                  .setStsFront(invalidPhoto)
                  .setStsBack(invalidPhoto)
              )
          )
          .setStatus(ProvenOwnerState.ProvenOwnerStatus.PENDING)
          .build()

        val result = validator.validateDocumentsPhotos(invalidOwner)
        result.nonEmpty shouldBe true
      }
      "missing driving license" in {

        val invalidOwner = ProvenOwnerState
          .newBuilder()
          .setDocumentsPhotos(
            DocumentsPhotos
              .newBuilder()
              .setPhotos(
                Photos
                  .newBuilder()
                  .setStsFront(photo)
                  .setStsBack(photo)
              )
          )
          .setStatus(ProvenOwnerState.ProvenOwnerStatus.PENDING)
          .build()

        val result = validator.validateDocumentsPhotos(invalidOwner)
        result shouldBe List(MissingDrivingLicencePhoto)
      }
      "sts front is missing" in {

        val invalidOwner = ProvenOwnerState
          .newBuilder()
          .setDocumentsPhotos(
            DocumentsPhotos
              .newBuilder()
              .setPhotos(
                Photos
                  .newBuilder()
                  .setStsBack(photo)
                  .setDrivingLicense(photo)
              )
          )
          .setStatus(ProvenOwnerState.ProvenOwnerStatus.PENDING)
          .build()

        val result = validator.validateDocumentsPhotos(invalidOwner)
        result shouldBe List(MissingStsFrontPhoto)
      }
      "sts back is missing" in {

        val invalidOwner = ProvenOwnerState
          .newBuilder()
          .setDocumentsPhotos(
            DocumentsPhotos
              .newBuilder()
              .setPhotos(
                Photos
                  .newBuilder()
                  .setStsFront(photo)
                  .setDrivingLicense(photo)
              )
          )
          .setStatus(ProvenOwnerState.ProvenOwnerStatus.PENDING)
          .build()

        val result = validator.validateDocumentsPhotos(invalidOwner)
        result shouldBe List(MissingStsBackPhoto)
      }
    }
  }

  private def buildGarageApiModelInsurance(
      serial: String,
      number: String,
      fromOpt: Option[Long] = Some(now - 240.day.toMillis),
      toOpt: Option[Long] = Some(now + 125.day.toMillis),
      insuranceType: VinReportModel.InsuranceType = VinReportModel.InsuranceType.OSAGO,
      insuranceStatus: VinReportModel.InsuranceStatus =
        VinReportModel.InsuranceStatus.ACTIVE): GarageApiModel.Insurance = {
    val insuranceBuilder = GarageApiModel.Insurance.newBuilder()
    insuranceBuilder
      .setSerial(serial)
      .setNumber(number)
      .setInsuranceType(insuranceType)
      .setStatus(insuranceStatus)
      .setCompany(
        GarageApiModel.Company
          .newBuilder()
          .setName("""АО "АльфаСтрахование"""")
          .setPhoneNumber("+1 234 000 00 00")
      )
      .setUpdateTimestamp(Timestamps.fromMillis(now))
    fromOpt.foreach(from => insuranceBuilder.setFrom(Timestamps.fromMillis(from)))
    toOpt.foreach(to => insuranceBuilder.setTo(Timestamps.fromMillis(to)))

    insuranceBuilder
      .build()
  }

}
