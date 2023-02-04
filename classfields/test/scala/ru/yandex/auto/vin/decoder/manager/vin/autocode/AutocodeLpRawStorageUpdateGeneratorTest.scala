package ru.yandex.auto.vin.decoder.manager.vin.autocode

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.Json
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.LicensePlate
import ru.yandex.auto.vin.decoder.partners.autocode.model.AutocodeReportResponse
import ru.yandex.auto.vin.decoder.proto.VinHistory.Taxi.LicenseStatus
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory.Status
import ru.yandex.auto.vin.decoder.raw.autocode.AutocodeReportResponseRaw
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator.MarkModelResult

class AutocodeLpRawStorageUpdateGeneratorTest extends AnyFunSuite {

  test("convert non empty taxi") {
    val lp = LicensePlate("НР20677")

    val raw = ResourceUtils.getStringFromResources(
      "/autocode/taxi/autoru_taxi_history_eyJ0eXBlIjoiR1JaIiwiYm9keSI6ItCd0KAyMDY3NyIsInNjaGVtYV92ZXJzaW9uIjoiMS4wIiwic3RvcmFnZXMiOnt9fQ==.json"
    )
    val rawStatus = "200"
    val map = Map(
      (Option("ФОРД") -> Option("ФОКУС")) -> MarkModelResult("FORD", "FOCUS", "ФОРД ФОКУС", unclear = false)
    )

    val response = AutocodeReportResponseRaw(raw, rawStatus, Json.parse(raw).as[AutocodeReportResponse])

    val res = AutocodeRawStorageUpdateGenerator.updateTaxi(lp, response, map)

    assert(res.identifier === lp)
    assert(res.meta.timestampCreate === response.model.getUpdatedMillis)
    assert(res.raw.data === raw)
    assert(res.raw.status === rawStatus)

    val prepared = res.prepared.data
    assert(prepared.getStatus === Status.UNKNOWN)
    assert(prepared.getStatuses.getTaxiStatus === Status.OK)
    assert(prepared.getEventType === EventType.AUTOCODE_TAXI)
    assert(prepared.getTaxiCount === 1)

    assert(prepared.getTaxi(0).getMark === "FORD")
    assert(prepared.getTaxi(0).getModel === "FOCUS")
    assert(prepared.getTaxi(0).getLicense === "0092083")
    assert(prepared.getTaxi(0).getLicenseStatus === LicenseStatus.ANNULLED)
    assert(prepared.getTaxi(0).getFrom === 1513026000000L)
    assert(prepared.getTaxi(0).getTo === 1670706000000L)
    assert(prepared.getTaxi(0).getCancel == 1552942800000L)
  }
}
