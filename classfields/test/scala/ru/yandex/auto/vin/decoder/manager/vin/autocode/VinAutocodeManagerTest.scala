package ru.yandex.auto.vin.decoder.manager.vin.autocode

import auto.carfax.common.utils.misc.ResourceUtils
import auto.carfax.common.utils.tracing.Traced
import org.mockito.Mockito._
import org.scalatest.funsuite.AsyncFunSuite
import play.api.libs.json.Json
import ru.yandex.auto.vin.decoder.geo.GeocoderManager
import ru.yandex.auto.vin.decoder.manager.IdentifiersManager
import ru.yandex.auto.vin.decoder.manager.licenseplate.LpAutocodeManager
import ru.yandex.auto.vin.decoder.manager.vin.VinHistoryManager
import ru.yandex.auto.vin.decoder.model.{LicensePlate, MockedFeatures, VinCode}
import ru.yandex.auto.vin.decoder.partners.autocode.AutocodeReportType
import ru.yandex.auto.vin.decoder.partners.autocode.model.AutocodeReportResponse
import ru.yandex.auto.vin.decoder.raw.autocode.AutocodeReportResponseRaw
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

class VinAutocodeManagerTest extends AsyncFunSuite with MockitoSupport with MockedFeatures {
  implicit val trigger = PartnerRequestTrigger.Unknown
  implicit val t: Traced = Traced.empty

  val vinHistoryManager = mock[VinHistoryManager]
  val lpAutocodeManager = mock[LpAutocodeManager]
  val identifiersManager = mock[IdentifiersManager]

  val unificator = mock[Unificator]
  val rawStorageManager = mock[RawStorageManager[VinCode]]
  val geocoderManager = mock[GeocoderManager]

  val vinAutocodeManager = new VinAutocodeManager(
    vinHistoryManager,
    unificator,
    rawStorageManager,
    identifiersManager,
    geocoderManager
  )

  val vin = VinCode("Z8NFBAJ11ES017019")
  val lp = LicensePlate("Н444ОЕ174")
  val raw = ResourceUtils.getStringFromResources("/autocode/main/autoru_main_report_Z8NFBAJ11ES017019.json")
  val response = AutocodeReportResponseRaw(raw, "200", Json.parse(raw).as[AutocodeReportResponse])

  when(rawStorageManager.upsert(?)(?)).thenReturn(Future.unit)
  when(identifiersManager.connect(eq(lp), eq(vin), ?, eq(true))(?)).thenReturn(Future.unit)

  test("update taxi and connect identifiers if lp exists") {
    vinAutocodeManager
      .update(vin, AutocodeReportType.Identifiers, response)
      .map { _ =>
        verify(identifiersManager).connect(eq(lp), eq(vin), ?, eq(true))(?)
        succeed
      }
  }
}
