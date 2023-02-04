package ru.yandex.auto.vin.decoder.manager.vin.checkburo

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.manager.offer.VosOffers
import ru.yandex.auto.vin.decoder.manager.vin.VinData
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.vin.Event
import ru.yandex.auto.vin.decoder.proto.VinHistory.{RegistrationEvent, VinInfoHistory}
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared

class CheckburoDataSelectorTest extends AnyWordSpecLike with Matchers {

  private val selector = new CheckburoDataSelector

  private val TestVin: VinCode = VinCode.apply("WP1ZZZ92ZGLA80455")

  "ScrapinghubDataSelector" must {

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
          EventType.CHECKBURO_ACCIDENTS -> List(mainAccidents, mainUpdateAccidents, errorMainUpdateAccidents)
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
          EventType.CHECKBURO_ACCIDENTS -> List(mainAccidents, mainUpdateAccidents, errorMainUpdateAccidents)
        ),
        offers = VosOffers.Empty
      )

      val res = selector.getAllAccidents(data)

      res.length shouldBe 2
      res should contain(mainAccidents)
      res should contain(mainUpdateAccidents)
    }

    "get ownership periods" in {
      val res = selector.getOwnershipPeriods(registrationData)

      res.get.data.getRegistration.getTimestamp shouldBe 100
      res.get.data.getRegistration.getPeriodsCount shouldBe 3
    }

    "get pts" in {
      val res = selector.getPts(registrationData)

      res.get.data.getRegistration.getMark shouldBe "BMW"
      res.get.data.getRegistration.getModel shouldBe "X5"
      res.get.data.getRegistration.getTimestamp shouldBe 200
    }

    "get pts for card with expired registration" in {
      val res = selector.getPts(registrationDataNoMark)

      res.get.data.getRegistration.getMark shouldBe "UNKNOWN_MARK"
      res.get.data.getRegistration.getModel shouldBe "UNKNOWN_MODEL"
      res.get.data.getRegistration.getTimestamp shouldBe 200
    }
  }

  private def registrationData: VinData = {
    val main = {
      val builder = VinInfoHistory.newBuilder()
      builder.getStatusesBuilder.setRegistrationStatus(VinInfoHistory.Status.OK)
      builder.getRegistrationBuilder.setMark("BMW").setModel("X5")
      builder.getRegistrationBuilder.setTimestamp(100)

      Prepared(100, 100, 100, builder.build(), "")
    }

    val update = {
      val builder = VinInfoHistory.newBuilder()
      builder.getStatusesBuilder.setRegistrationStatus(VinInfoHistory.Status.OK)
      builder.getRegistrationBuilder.setMark("BMW").setModel("X5")
      builder.getRegistrationBuilder.setTimestamp(200)

      Prepared(200, 200, 200, builder.build(), "")
    }

    val periods = {
      val builder = VinInfoHistory.newBuilder()
      builder.getStatusesBuilder.setRegistrationStatus(VinInfoHistory.Status.OK)
      builder.getRegistrationBuilder.setTimestamp(100)
      builder.getRegistrationBuilder
        .addPeriods(RegistrationEvent.newBuilder().setFrom(100).setTo(200))
        .addPeriods(RegistrationEvent.newBuilder().setFrom(201).setTo(300))
        .addPeriods(RegistrationEvent.newBuilder().setFrom(400).setTo(800))

      Prepared(100, 100, 100, builder.build(), "")
    }

    val data = VinData(
      vinCode = TestVin,
      mysqlData = Map.empty,
      rawStorageData = Map(
        EventType.CHECKBURO_TTX -> List(main, update),
        EventType.CHECKBURO_OWNERS -> List(periods)
      ),
      offers = VosOffers.Empty
    )

    data
  }

  private def registrationDataNoMark: VinData = {
    val pts = {
      val builder = VinInfoHistory.newBuilder()
      builder.getStatusesBuilder.setRegistrationStatus(VinInfoHistory.Status.OK)
      builder.getRegistrationBuilder.setMark("BMW").setModel("X5")
      builder.getRegistrationBuilder.setTimestamp(100)

      Prepared(100, 100, 100, builder.build(), "")
    }

    val regActions = {
      val builder = VinInfoHistory.newBuilder()
      builder.getStatusesBuilder.setRegistrationStatus(VinInfoHistory.Status.OK)
      builder.getRegistrationBuilder.setMark(Event.NoMark).setModel(Event.NoModel)
      builder.getRegistrationBuilder.setTimestamp(200)

      Prepared(200, 200, 200, builder.build(), "")
    }

    val data = VinData(
      vinCode = TestVin,
      mysqlData = Map.empty,
      rawStorageData = Map(
        EventType.CHECKBURO_TTX -> List(pts),
        EventType.CHECKBURO_OWNERS -> List(regActions)
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
