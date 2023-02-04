package ru.yandex.auto.vin.decoder.manager.vin

import org.scalatest.enablers.Emptiness.emptinessOfOption
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.manager.offer.VosOffers
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared

class RawStorageDataSelectorSpec extends AnyWordSpecLike with Matchers {

  private val selector = RawStorageDataSelector
  private val TestVin: VinCode = VinCode.apply("WP1ZZZ92ZGLA80455")

  "RawStorageDataSelector" should {
    "get last success: empty raw storage data" in {
      val data = VinData.empty(TestVin)

      val res =
        selector.getLastSuccessFromRawStorage(data, List(EventType.ADAPERIO_MAIN))(_.getStatuses.getRegistrationStatus)

      res shouldBe empty
    }

    "get last success: empty needed source" in {
      val data = VinData(
        vinCode = TestVin,
        mysqlData = Map.empty,
        rawStorageData = Map(
          EventType.ADAPERIO_MAIN_UPDATE -> List(build(1, VinInfoHistory.Status.OK) { (builder, status) =>
            builder.getStatusesBuilder.setRegistrationStatus(status)
          })
        ),
        offers = VosOffers.Empty
      )

      val res =
        selector.getLastSuccessFromRawStorage(data, List(EventType.ADAPERIO_MAIN))(_.getStatuses.getRegistrationStatus)

      res shouldBe empty
    }

    "get last success: filter errors" in {
      val error = build(10, VinInfoHistory.Status.ERROR) { (builder, status) =>
        builder.getStatusesBuilder.setRegistrationStatus(status)
      }
      val success = build(1, VinInfoHistory.Status.OK) { (builder, status) =>
        builder.getStatusesBuilder.setRegistrationStatus(status)
      }

      val data = VinData(
        vinCode = TestVin,
        mysqlData = Map.empty,
        rawStorageData = Map(
          EventType.ADAPERIO_MAIN -> List(success, error)
        ),
        offers = VosOffers.Empty
      )

      val res =
        selector.getLastSuccessFromRawStorage(data, List(EventType.ADAPERIO_MAIN))(_.getStatuses.getRegistrationStatus)

      res.get shouldBe success
    }

    "get last success: max by timestamp" in {
      val second = build(10, VinInfoHistory.Status.OK) { (builder, status) =>
        builder.getStatusesBuilder.setRegistrationStatus(status)
      }
      val first = build(1, VinInfoHistory.Status.OK) { (builder, status) =>
        builder.getStatusesBuilder.setRegistrationStatus(status)
      }

      val data = VinData(
        vinCode = TestVin,
        mysqlData = Map.empty,
        rawStorageData = Map(
          EventType.ADAPERIO_MAIN -> List(first, second)
        ),
        offers = VosOffers.Empty
      )

      val res =
        selector.getLastSuccessFromRawStorage(data, List(EventType.ADAPERIO_MAIN))(_.getStatuses.getRegistrationStatus)

      res.get shouldBe second
    }

    "get all success: filter errors" in {
      val success1 = build(1, VinInfoHistory.Status.OK) { (builder, status) =>
        builder.getStatusesBuilder.setRegistrationStatus(status)
      }
      val error = build(10, VinInfoHistory.Status.ERROR) { (builder, status) =>
        builder.getStatusesBuilder.setRegistrationStatus(status)
      }
      val success2 = build(20, VinInfoHistory.Status.OK) { (builder, status) =>
        builder.getStatusesBuilder.setRegistrationStatus(status)
      }

      val data = VinData(
        vinCode = TestVin,
        mysqlData = Map.empty,
        rawStorageData = Map(
          EventType.ADAPERIO_MAIN -> List(success1, error, success2)
        ),
        offers = VosOffers.Empty
      )

      val res =
        selector.getAllSuccessFromRawStorage(data, List(EventType.ADAPERIO_MAIN))(_.getStatuses.getRegistrationStatus)

      res.length shouldBe 2
      res should contain(success1)
      res should contain(success2)
    }
  }

  private def build(
      timestamp: Long,
      status: VinInfoHistory.Status
    )(setStatus: (VinInfoHistory.Builder, VinInfoHistory.Status) => Any): Prepared = {
    val builder = VinInfoHistory.newBuilder()
    setStatus(builder, status)
    Prepared(timestamp, timestamp, timestamp, builder.build(), "")
  }
}
