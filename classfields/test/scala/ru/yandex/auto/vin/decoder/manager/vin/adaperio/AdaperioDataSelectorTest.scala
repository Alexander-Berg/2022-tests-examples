package ru.yandex.auto.vin.decoder.manager.vin.adaperio

import org.scalatest.enablers.Emptiness.emptinessOfOption
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.manager.offer.VosOffers
import ru.yandex.auto.vin.decoder.manager.vin.VinData
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.VinHistory.{DiagnosticCard, Mileage, RegistrationEvent, VinInfoHistory}
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared

import scala.jdk.CollectionConverters.{IterableHasAsJava, ListHasAsScala}

class AdaperioDataSelectorTest extends AnyWordSpecLike with Matchers {

  private val selector = new AdaperioDataSelector

  private val TestVin: VinCode = VinCode.apply("WP1ZZZ92ZGLA80455")

  val mileages = List(
    Mileage.newBuilder.setDate(100).build(),
    Mileage.newBuilder.setDate(200).build()
  )

  val diagnosticCards = List(
    DiagnosticCard.newBuilder.setFrom(100).build(),
    DiagnosticCard.newBuilder.setFrom(200).build(),
    DiagnosticCard.newBuilder.setFrom(300).build()
  )

  "AdaperioDataSelector" must {

    "get accidents" in {
      val func: (VinInfoHistory.Builder, VinInfoHistory.Status) => Unit =
        (builder: VinInfoHistory.Builder, status: VinInfoHistory.Status) => {
          builder.getStatusesBuilder.setAccidentsStatus(status)
          ()
        }

      val mainAccidents = build(10, VinInfoHistory.Status.OK)(func)
      val mainUpdateAccidents = build(15, VinInfoHistory.Status.OK)(func)
      val errorMainUpdateAccidents = build(20, VinInfoHistory.Status.ERROR)(func)

      val data = VinData(
        vinCode = TestVin,
        mysqlData = Map.empty,
        rawStorageData = Map(
          EventType.ADAPERIO_MAIN -> List(mainAccidents),
          EventType.ADAPERIO_MAIN_UPDATE -> List(mainUpdateAccidents, errorMainUpdateAccidents)
        ),
        offers = VosOffers.Empty
      )

      val res = selector.getAccidents(data)

      res.get shouldBe mainUpdateAccidents
    }

    "get all accidents" in {
      val func: (VinInfoHistory.Builder, VinInfoHistory.Status) => Unit =
        (builder: VinInfoHistory.Builder, status: VinInfoHistory.Status) => {
          builder.getStatusesBuilder.setAccidentsStatus(status)
          ()
        }

      val mainAccidents = build(10, VinInfoHistory.Status.OK)(func)
      val mainUpdateAccidents = build(15, VinInfoHistory.Status.OK)(func)
      val errorMainUpdateAccidents = build(20, VinInfoHistory.Status.ERROR)(func)

      val data = VinData(
        vinCode = TestVin,
        mysqlData = Map.empty,
        rawStorageData = Map(
          EventType.ADAPERIO_MAIN -> List(mainAccidents),
          EventType.ADAPERIO_MAIN_UPDATE -> List(mainUpdateAccidents, errorMainUpdateAccidents)
        ),
        offers = VosOffers.Empty
      )

      val res = selector.getAllAccidents(data)

      res.length shouldBe 2
      res should contain(mainAccidents)
      res should contain(mainUpdateAccidents)
    }

    "get pts" in {

      val res = selector.getPts(registrationData)

      res.get.data.getRegistration.getMark shouldBe "BMW"
      res.get.data.getRegistration.getModel shouldBe "X5"
      res.get.data.getRegistration.getTimestamp shouldBe 100
      res.get.data.getRegistration.getPeriodsCount shouldBe 1
    }

    "get periods" in {

      val res = selector.getOwnershipPeriods(registrationData)

      res.get.data.getRegistration.getTimestamp shouldBe 400
      res.get.data.getRegistration.getPeriodsCount shouldBe 3
    }

    "get tech inspections" in {

      val res = selector.getTechInspections(techInspectionsData(VinInfoHistory.Status.OK, VinInfoHistory.Status.OK))

      res.get.data.getMileageList.size() shouldBe 2
      res.get.data.getMileageList.asScala shouldBe mileages
    }

    "do not get wrong tech inspections" in {

      val res = selector.getTechInspections(techInspectionsData(VinInfoHistory.Status.ERROR, VinInfoHistory.Status.OK))

      res shouldBe empty
    }

    "get diagnostic cards" in {

      val res = selector.getDiagnosticCards(techInspectionsData(VinInfoHistory.Status.OK, VinInfoHistory.Status.OK))

      res.get.data.getDiagnosticCardsList.size() shouldBe 3
      res.get.data.getDiagnosticCardsList.asScala shouldBe diagnosticCards
    }

    "do not get wrong diagnostic cards" in {

      val res = selector.getDiagnosticCards(techInspectionsData(VinInfoHistory.Status.OK, VinInfoHistory.Status.ERROR))

      res shouldBe empty
    }
  }

  private def registrationData = {
    val mainRegistration = {
      val builder = VinInfoHistory.newBuilder()
      builder.getStatusesBuilder.setRegistrationStatus(VinInfoHistory.Status.OK)
      builder.getRegistrationBuilder.setMark("BMW").setModel("X5")
      builder.getRegistrationBuilder.addPeriods(RegistrationEvent.newBuilder().setFrom(100))
      builder.getRegistrationBuilder.setTimestamp(100)

      Prepared(100, 100, 100, builder.build(), "")
    }

    val firstMainUpdateRegistration = {
      val builder = VinInfoHistory.newBuilder()
      builder.getStatusesBuilder.setRegistrationStatus(VinInfoHistory.Status.OK)
      builder.getRegistrationBuilder.setTimestamp(200)
      builder.getRegistrationBuilder
        .addPeriods(RegistrationEvent.newBuilder().setFrom(100).setTo(200))
        .addPeriods(RegistrationEvent.newBuilder().setFrom(201))

      Prepared(200, 200, 200, builder.build(), "")
    }

    val secondMainUpdateRegistration = {
      val builder = VinInfoHistory.newBuilder()
      builder.getStatusesBuilder.setRegistrationStatus(VinInfoHistory.Status.OK)
      builder.getRegistrationBuilder.setTimestamp(400)
      builder.getRegistrationBuilder
        .addPeriods(RegistrationEvent.newBuilder().setFrom(100).setTo(200))
        .addPeriods(RegistrationEvent.newBuilder().setFrom(201).setTo(300))
        .addPeriods(RegistrationEvent.newBuilder().setFrom(400).setTo(800))

      Prepared(400, 400, 400, builder.build(), "")
    }

    VinData(
      vinCode = TestVin,
      mysqlData = Map.empty,
      rawStorageData = Map(
        EventType.ADAPERIO_MAIN -> List(mainRegistration),
        EventType.ADAPERIO_MAIN_UPDATE -> List(firstMainUpdateRegistration, secondMainUpdateRegistration)
      ),
      offers = VosOffers.Empty
    )
  }

  private def techInspectionsData(mileageStatus: VinInfoHistory.Status, diagnosticCardStatus: VinInfoHistory.Status) = {

    val techInspections = {
      val builder = VinInfoHistory.newBuilder()
      builder.getStatusesBuilder.setMileagesStatus(mileageStatus).setDiagnosticCardsStatus(diagnosticCardStatus)
      builder.addAllMileage(mileages.asJava).addAllDiagnosticCards(diagnosticCards.asJava)

      Prepared(100, 100, 100, builder.build(), "")
    }

    VinData(
      vinCode = TestVin,
      mysqlData = Map.empty,
      rawStorageData = Map(EventType.ADAPERIO_MILEAGE -> List(techInspections)),
      offers = VosOffers.Empty
    )
  }

  private def build(
      timestamp: Long,
      status: VinInfoHistory.Status
    )(setStatus: (VinInfoHistory.Builder, VinInfoHistory.Status) => Unit): Prepared = {
    val builder = VinInfoHistory.newBuilder()
    setStatus(builder, status)
    Prepared(timestamp, timestamp, timestamp, builder.build(), "")
  }
}
