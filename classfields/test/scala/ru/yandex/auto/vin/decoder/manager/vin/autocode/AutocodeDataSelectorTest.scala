package ru.yandex.auto.vin.decoder.manager.vin.autocode

import cats.implicits.catsSyntaxOptionId
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.manager.offer.VosOffers
import ru.yandex.auto.vin.decoder.manager.vin.{LpData, VinData}
import ru.yandex.auto.vin.decoder.model.IdentifierGenerators.LicensePlateGen
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory.Status
import ru.yandex.auto.vin.decoder.proto.VinHistory._
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared

class AutocodeDataSelectorTest extends AnyFunSuite {

  import AutocodeDataSelectorTest._

  private val selector = new AutocodeDataSelector

  private val TestVin: VinCode = VinCode("WP1ZZZ92ZGLA80455")
  private val TestLp = LicensePlateGen.sample.get

  test("get accidents") {
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
        EventType.AUTOCODE_MAIN -> List(mainAccidents),
        EventType.AUTOCODE_MAIN_UPDATE -> List(mainUpdateAccidents, errorMainUpdateAccidents)
      ),
      offers = VosOffers.Empty
    )

    val res = selector.getAccidents(data)
    assert(res.get === mainUpdateAccidents)
  }

  test("get all accidents") {
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
        EventType.AUTOCODE_MAIN -> List(mainAccidents),
        EventType.AUTOCODE_MAIN_UPDATE -> List(mainUpdateAccidents, errorMainUpdateAccidents)
      ),
      offers = VosOffers.Empty
    )

    val res = selector.getAllAccidents(data)

    assert(res.length === 2)
    assert(res.contains(mainAccidents))
    assert(res.contains(mainUpdateAccidents))
  }

  test("get pts") {
    val res = selector.getPts(AutocodeDataSelectorTest.registrationData(TestVin))

    assert(res.get.data.getRegistration.getMark === "BMW")
    assert(res.get.data.getRegistration.getModel === "X5")
    assert(res.get.data.getRegistration.getTimestamp === 100)
    assert(res.get.data.getRegistration.getPeriodsCount === 1)
  }

  test("get periods") {
    val res = selector.getOwnershipPeriods(AutocodeDataSelectorTest.registrationData(TestVin))

    assert(res.get.data.getRegistration.getTimestamp === 400)
    assert(res.get.data.getRegistration.getPeriodsCount === 3)
  }

  test("get license plate") {
    val data = VinData(
      vinCode = TestVin,
      mysqlData = Map.empty,
      rawStorageData = Map(
        EventType.AUTOCODE_MAIN -> List(mainRegistration),
        EventType.AUTOCODE_IDENTIFIERS -> List(mainIdentifiers)
      ),
      offers = VosOffers.Empty
    )

    val res = selector.getLicensePlate(data)
    assert(res.nonEmpty)
    assert(res.get.identifier.toString == "K719CE178")
  }

  test("get taxi") {
    val vinData = VinData(
      vinCode = TestVin,
      mysqlData = Map.empty,
      rawStorageData = Map(
        EventType.AUTOCODE_TAXI -> List(getMainTaxi(100))
      ),
      offers = VosOffers.Empty
    )

    val lpData = LpData(
      lp = TestLp,
      rawStorageData = Map(
        EventType.AUTOCODE_TAXI -> List(getMainTaxi(105))
      )
    )

    val res = selector.getTaxi(vinData, lpData.some)
    assert(res.nonEmpty)
    assert(res.get.data.getTaxi(0).getFrom == 104)
  }

  test("get all sts") {
    val oldSts = "0000000"
    val freshSts = "7858441947"
    val vinData = VinData(
      vinCode = TestVin,
      mysqlData = Map.empty,
      rawStorageData = Map(
        EventType.AUTOCODE_MAIN -> List(stsAtRegistration(freshSts, age = 80)),
        EventType.AUTOCODE_TTX -> List(stsAtTtx(freshSts, age = 90)),
        EventType.AUTOCODE_ARCHIVE -> List(stsAtRegistration(oldSts, age = 0)),
        EventType.AUTOCODE_IDENTIFIERS -> List(stsAtVehicleIdentifiers(freshSts, age = 100))
      ),
      offers = VosOffers.Empty
    )

    val res = selector.getAllSts(vinData)
    assert(res.size == 2)
    assert(res.head.identifier.sts == oldSts)
    assert(res.last.identifier.sts == freshSts)
    assert(res.last.relationshipTimestampUpdate == 100)
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

object AutocodeDataSelectorTest {

  def registrationData(testVin: VinCode): VinData = {
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

    val data = VinData(
      vinCode = testVin,
      mysqlData = Map.empty,
      rawStorageData = Map(
        EventType.AUTOCODE_MAIN -> List(mainRegistration),
        EventType.AUTOCODE_MAIN_UPDATE -> List(firstMainUpdateRegistration, secondMainUpdateRegistration)
      ),
      offers = VosOffers.Empty
    )
    data

  }

  val mainRegistration = {
    val builder = VinInfoHistory.newBuilder()
    builder.getStatusesBuilder.setRegistrationStatus(VinInfoHistory.Status.OK)
    builder.getRegistrationBuilder
      .setMark("BMW")
      .setModel("X5")
      .setTimestamp(100)
      .setLicensePlate("K718CE178")
    builder.getRegistrationBuilder
      .addPeriods(RegistrationEvent.newBuilder().setFrom(100))

    Prepared(100, 100, 100, builder.build(), "")
  }

  val mainIdentifiers = {
    val builder = VinInfoHistory.newBuilder()
    builder.getStatusesBuilder.setVehicleIdentifiersStatus(Status.OK)
    builder.getVehicleIdentifiersBuilder.setLicensePlate("K719CE178")
    Prepared(101, 101, 101, builder.build(), "")
  }

  def getMainTaxi(age: Long): Prepared = {
    val builder = VinInfoHistory.newBuilder()
    builder.getStatusesBuilder.setTaxiStatus(Status.OK)
    builder.addTaxiBuilder().setFrom(age - 1)
    Prepared(age, age, age, builder.build(), "")
  }

  def stsAtRegistration(sts: String, age: Long): Prepared = {
    val builder = VinInfoHistory.newBuilder
    val regB = Registration.newBuilder
    regB.setSts(sts)
    builder.getStatusesBuilder.setRegistrationStatus(Status.OK)
    builder.setRegistration(regB.build)
    Prepared(age, age, age, builder.build(), "")
  }

  def stsAtTtx(sts: String, age: Long): Prepared = {
    val builder = VinInfoHistory.newBuilder
    val ttxB = AutocodeTtx.newBuilder
    ttxB.setSts(sts)
    builder.getStatusesBuilder.setTtxStatus(Status.OK)
    builder.setAutocodeTtx(ttxB.build)
    Prepared(age, age, age, builder.build(), "")
  }

  def stsAtVehicleIdentifiers(sts: String, age: Long): Prepared = {
    val builder = VinInfoHistory.newBuilder
    val idsB = VehicleIdentifiers.newBuilder
    idsB.setSts(sts)
    builder.getStatusesBuilder.setVehicleIdentifiersStatus(Status.OK)
    builder.setVehicleIdentifiers(idsB.build)
    Prepared(age, age, age, builder.build(), "")
  }
}
