package ru.yandex.auto.vin.decoder.manager.vin.megaparser

import cats.syntax.option._
import org.scalatest.enablers.Emptiness.emptinessOfOption
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.manager.offer.VosOffers
import ru.yandex.auto.vin.decoder.manager.vin.{LpData, VinData}
import ru.yandex.auto.vin.decoder.model.{LicensePlate, VinCode}
import ru.yandex.auto.vin.decoder.proto.VinHistory.{Insurance, RegistrationEvent, VinInfoHistory}
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared

import scala.jdk.CollectionConverters.IterableHasAsJava

class MegaParserDataSelectorTest extends AnyWordSpecLike with Matchers {

  private val selector = new MegaParserDataSelector

  private val TestVin: VinCode = VinCode.apply("WP1ZZZ92ZGLA80455")
  private val TestLp: LicensePlate = LicensePlate.apply("е777кх199")

  "MegaParserDataSelector" must {

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
          EventType.MEGA_PARSER_GIBDD_ACCIDENTS -> List(mainAccidents, mainUpdateAccidents, errorMainUpdateAccidents)
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
          EventType.MEGA_PARSER_GIBDD_ACCIDENTS -> List(mainAccidents, mainUpdateAccidents, errorMainUpdateAccidents)
        ),
        offers = VosOffers.Empty
      )

      val res = selector.getAllAccidents(data)

      res.length shouldBe 2
      res should contain(mainAccidents)
      res should contain(mainUpdateAccidents)
    }

    "get registration" in {
      val res = selector.getPts(registrationData)

      res.get.data.getRegistration.getMark shouldBe "BMW"
      res.get.data.getRegistration.getModel shouldBe "X5"
      res.get.data.getRegistration.getTimestamp shouldBe 400
      res.get.data.getRegistration.getPeriodsCount shouldBe 3
    }

    "get pts" in {
      val res = selector.getPts(registrationData)

      res.get.data.getRegistration.getMark shouldBe "BMW"
      res.get.data.getRegistration.getModel shouldBe "X5"
      res.get.data.getRegistration.getTimestamp shouldBe 400
    }

    "get periods" in {
      val res = selector.getPts(registrationData)

      res.get.data.getRegistration.getTimestamp shouldBe 400
      res.get.data.getRegistration.getPeriodsCount shouldBe 3
    }

    "get correct current insurances" in {

      val oldInsurances = VinInfoHistory
        .newBuilder()
        .addAllInsurances(
          List(insurance("old", "insurance1"), insurance("old", "insurance2")).asJava
        )
        .build
      val oldPrepared = Prepared(0, 0, 0, oldInsurances, "")
      val newInsurances = VinInfoHistory
        .newBuilder()
        .addAllInsurances(
          List(insurance("new", "insurance3"), insurance("new", "insurance4")).asJava
        )
        .build
      val newPrepared = Prepared(1000, 1000, 1000, newInsurances, "")

      val rawStorageData = Map(EventType.MEGA_PARSER_CURRENT_INSURANCES -> List(oldPrepared, newPrepared))

      val vinData = buildVinData(rawStorageData)

      val res = selector.getRsaInsurances(vinData, None)

      res should not be empty
      res.get.data.getInsurancesCount shouldBe 2
      res.get.data.getInsurances(0).getSerial shouldBe "new"
      res.get.data.getInsurances(0).getNumber shouldBe "insurance3"
      res.get.data.getInsurances(1).getSerial shouldBe "new"
      res.get.data.getInsurances(1).getNumber shouldBe "insurance4"
    }

    "get freshest current insurances from VinData and LpData" in {

      val vinInsurances = VinInfoHistory
        .newBuilder()
        .addAllInsurances(
          List(insurance("old", "insurance1"), insurance("old", "insurance2")).asJava
        )
        .build
      val vinPrepared = Prepared(0, 0, 0, vinInsurances, "")

      val lpInsurances = VinInfoHistory
        .newBuilder()
        .addAllInsurances(
          List(insurance("new", "insurance3"), insurance("new", "insurance4")).asJava
        )
        .build
      val lpPrepared = Prepared(1000, 1000, 1000, lpInsurances, "")

      val vinStorageData = Map(EventType.MEGA_PARSER_CURRENT_INSURANCES -> List(vinPrepared))
      val vinData = buildVinData(vinStorageData)

      val lpStorageData = Map(EventType.MEGA_PARSER_CURRENT_INSURANCES -> List(lpPrepared))
      val lpData = buildLpData(lpStorageData)

      val res = selector.getRsaInsurances(vinData, lpData.some)

      res should not be empty
      res.get.data.getInsurancesCount shouldBe 2
      res.get.data.getInsurances(0).getSerial shouldBe "new"
      res.get.data.getInsurances(0).getNumber shouldBe "insurance3"
      res.get.data.getInsurances(1).getSerial shouldBe "new"
      res.get.data.getInsurances(1).getNumber shouldBe "insurance4"
    }

    "get correct None if current insurances is empty" in {
      val prepared = Prepared(1000, 1000, 1000, VinInfoHistory.getDefaultInstance, "")
      val rawStorageData = Map(EventType.ADAPERIO_MAIN -> List(prepared))
      val vinData = buildVinData(rawStorageData)

      val res = selector.getRsaInsurances(vinData, None)

      res shouldBe empty
    }

    "get correct insurance details if it is present" in {

      val ts = System.currentTimeMillis()

      val currentInsurances = VinInfoHistory
        .newBuilder()
        .addAllInsurances(
          List(insurance("new", "insurance1"), insurance("new", "insurance2")).asJava
        )
        .build
      val currentInsurancesPrepared = Prepared(0, 0, 0, currentInsurances, "")

      val insuranceDetails = VinInfoHistory
        .newBuilder()
        .addAllInsurances(
          List(insurance("new", "insurance1", ts.some, "detailed".some)).asJava
        )
        .build
      val insuranceDetailsPrepared = Prepared(1000, 1000, 1000, insuranceDetails, "")

      val rawStorageData = Map(
        EventType.MEGA_PARSER_CURRENT_INSURANCES -> List(currentInsurancesPrepared),
        EventType.MEGA_PARSER_INSURANCE_DETAILS -> List(insuranceDetailsPrepared)
      )

      val vinData = buildVinData(rawStorageData)

      val res = selector.getRsaInsurances(vinData, None)

      res should not be empty
      res.get.data.getInsurancesCount shouldBe 2
      res.get.data.getInsurances(0).getSerial shouldBe "new"
      res.get.data.getInsurances(0).getNumber shouldBe "insurance1"
      res.get.data.getInsurances(0).getFrom shouldBe ts
      res.get.data.getInsurances(0).getPolicyStatus shouldBe "detailed"
      res.get.data.getInsurances(1).getSerial shouldBe "new"
      res.get.data.getInsurances(1).getNumber shouldBe "insurance2"
      res.get.data.getInsurances(1).getFrom shouldBe 0
      res.get.data.getInsurances(1).getPolicyStatus shouldBe empty
    }

  }

  def buildVinData(rawStorageData: Map[EventType, List[Prepared]]) =
    VinData(TestVin, Map.empty, rawStorageData, VosOffers.Empty)

  def buildLpData(rawStorageData: Map[EventType, List[Prepared]]) =
    LpData(TestLp, rawStorageData)

  def insurance(serial: String, number: String, from: Option[Long] = None, policyStatus: Option[String] = None) = {
    val b = Insurance.newBuilder().setSerial(serial).setNumber(number)
    from.foreach(b.setFrom)
    policyStatus.foreach(b.setPolicyStatus)
    b.build
  }

  private def registrationData: VinData = {
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
      builder.getRegistrationBuilder.setMark("BMW").setModel("X5")
      builder.getRegistrationBuilder.setTimestamp(200)
      builder.getRegistrationBuilder
        .addPeriods(RegistrationEvent.newBuilder().setFrom(100).setTo(200))
        .addPeriods(RegistrationEvent.newBuilder().setFrom(201))

      Prepared(200, 200, 200, builder.build(), "")
    }

    val secondMainUpdateRegistration = {
      val builder = VinInfoHistory.newBuilder()
      builder.getStatusesBuilder.setRegistrationStatus(VinInfoHistory.Status.OK)
      builder.getRegistrationBuilder.setMark("BMW").setModel("X5")
      builder.getRegistrationBuilder.setTimestamp(400)
      builder.getRegistrationBuilder
        .addPeriods(RegistrationEvent.newBuilder().setFrom(100).setTo(200))
        .addPeriods(RegistrationEvent.newBuilder().setFrom(201).setTo(300))
        .addPeriods(RegistrationEvent.newBuilder().setFrom(400).setTo(800))

      Prepared(400, 400, 400, builder.build(), "")
    }

    val data = VinData(
      vinCode = TestVin,
      mysqlData = Map.empty,
      rawStorageData = Map(
        EventType.MEGA_PARSER_GIBDD_REGISTRATION -> List(
          mainRegistration,
          firstMainUpdateRegistration,
          secondMainUpdateRegistration
        )
      ),
      offers = VosOffers.Empty
    )

    data
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
