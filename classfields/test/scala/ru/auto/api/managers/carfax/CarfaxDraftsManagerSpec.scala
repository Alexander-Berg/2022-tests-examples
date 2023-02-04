package ru.auto.api.managers.carfax

import cats.instances.list._
import cats.syntax.all._
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.features.FeatureManager
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.carfax.{CarfaxClient, Context}
import ru.auto.api.util.Request
import ru.auto.api.util.StringUtils._
import ru.auto.api.vin.VinReportModel.RawVinEssentialsReport
import ru.auto.api.{AsyncTasksSupport, BaseSpec}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

class CarfaxDraftsManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with BeforeAndAfter
  with AsyncTasksSupport {

  import CarfaxDraftsManagerSpec._

  implicit val request: Request = RequestGen.next

  val carfaxClient = mock[CarfaxClient]
  val featureManager = mock[FeatureManager]
  val carfaxDraftsManager = new CarfaxDraftsManager(carfaxClient, featureManager)

  before {
    reset(carfaxClient)
  }

  abstract class DraftFixture(drive: String = IdentifierGen.next, transmission: String = IdentifierGen.next) {
    val vin = VinGenerator.next
    val draft = Offer.newBuilder().build()

    val carInfo = CarInfoGen.next.toBuilder
      .setDrive(drive)
      .setTransmission(transmission)

    val report = {
      val builder = RawVinEssentialsReport.newBuilder()
      builder.getVehicleBuilder.setCarInfo(carInfo).setColorHex("000000")
      builder.getPtsInfoBuilder.getYearBuilder.setValue(2020)
      builder.build()
    }

    val enrichedDraft = CarfaxDraftsManager.enrich(draft, report)
    enrichedDraft.getColorHex shouldBe report.getVehicle.getColorHex
    enrichedDraft.getDocuments.getYear shouldBe report.getPtsInfo.getYear.getValue
  }

  "CarfaxDraftsManager" should {
    "enrich draft" in new DraftFixture {
      enrichedDraft.getCarInfo shouldBe report.getVehicle.getCarInfo
    }

    (driveMap.keySet.toList, transmissionMap.keySet.toList).tupled.foreach {
      case (drive, transmission) =>
        s"enrich draft with drive=$drive transmission=$transmission" in new DraftFixture(drive, transmission) {
          enrichedDraft.getCarInfo.getDrive shouldBe driveMap(report.getVehicle.getCarInfo.getDrive)
          enrichedDraft.getCarInfo.getTransmission shouldBe transmissionMap(
            report.getVehicle.getCarInfo.getTransmission
          )
        }
    }
  }

  "CarfaxDraftsManager.enrichAndCallReportUpdate" should {
    "call update of the report with mark" in {
      val vin = VinGenerator.next
      val draft = OfferGen.next
      when(carfaxClient.updateAll(?, ?, ?, ?)(?)).thenReturnF(())

      val rawVinEssentialsReportResponse = rawEssentialsReportResponseGen.next
      val mark = rawVinEssentialsReportResponse.getReport.getPtsInfo.getMark.getValue.toOption
      when(carfaxClient.getRawEssentialsReport(?)(?)).thenReturnF(rawVinEssentialsReportResponse)

      carfaxDraftsManager.enrichAndCallReportUpdate(vin, draft).futureValue
      verify(carfaxClient).updateAll(eq(vin), eq(Context.DraftCreation), eq(mark), eq(None))(?)
    }

    "call update of the report without mark" in {
      val vin = VinGenerator.next
      val draft = OfferGen.next
      when(carfaxClient.updateAll(?, ?, ?, ?)(?)).thenReturnF(())

      when(carfaxClient.getRawEssentialsReport(?)(?)).thenReturn(Future.failed(new IllegalArgumentException("")))

      carfaxDraftsManager
        .enrichAndCallReportUpdate(vin, draft)
        .failed
        .futureValue shouldBe an[IllegalArgumentException]

      verify(carfaxClient).updateAll(eq(vin), eq(Context.DraftCreation), eq(None), eq(None))(?)
    }

    "call update of the report with partner options" in {
      val vin = VinGenerator.next
      val draftBuilder = OfferGen.next.toBuilder
      draftBuilder.getDocumentsBuilder.setVin(vin)
      draftBuilder.getCarInfoBuilder
        .clearEquipment()
        .putEquipment("just-an-equip", true)
        .putEquipment("forced-false-equip", false)

      val reportResponseBuilder = rawEssentialsReportResponseGen.next.toBuilder
      val vehicleBuilder = reportResponseBuilder.getReportBuilder.getVehicleBuilder
      vehicleBuilder.getHeaderBuilder.setIsUpdating(false)
      vehicleBuilder.getCarInfoBuilder
        .clearEquipment()
        .putEquipment("true-equip", true)
        .putEquipment("false-equip", false)
        .putEquipment("forced-false-equip", true)

      when(carfaxClient.getRawEssentialsReport(?)(?)).thenReturnF(reportResponseBuilder.build)
      val feature = mock[Feature[Boolean]]
      when(feature.value).thenReturn(true)
      when(featureManager.enrichNewOffersWithPartnerOptions).thenReturn(feature)

      val partnerOptions = carfaxDraftsManager.enrichOptionsIfNeeded(draftBuilder.build()).futureValue
      partnerOptions.get("true-equip") shouldBe true
      partnerOptions.get("forced-false-equip") shouldBe true
    }
  }
}

object CarfaxDraftsManagerSpec {
  val IdentifierGen: Gen[String] = Gen.identifier.filter(_.nonEmpty)

  val driveMap = Map(
    "REAR" -> "REAR_DRIVE",
    "FRONT" -> "FORWARD_CONTROL",
    "ALL" -> "ALL_WHEEL_DRIVE",
    "ALL_PART" -> "ALL_WHEEL_DRIVE",
    "ALL_FULL" -> "ALL_WHEEL_DRIVE"
  )

  val transmissionMap = Map(
    "ROBOT_2CLUTCH" -> "ROBOT",
    "ROBOT_1CLUTCH" -> "ROBOT",
    "ROBOT_SEQ" -> "ROBOT",
    "PP" -> "AUTOMATIC"
  )
}
