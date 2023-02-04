package ru.yandex.auto.vin.decoder.manager.vin.autocode

import auto.carfax.common.utils.tracing.Traced
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.Json
import ru.yandex.auto.vin.decoder.manager.IdentifiersManager
import ru.yandex.auto.vin.decoder.manager.licenseplate.LpAutocodeManager
import ru.yandex.auto.vin.decoder.model.{LicensePlate, MockedFeatures, VinCode}
import ru.yandex.auto.vin.decoder.partners.autocode.AutocodeReportType
import ru.yandex.auto.vin.decoder.partners.autocode.model.AutocodeReportResponse
import ru.yandex.auto.vin.decoder.raw.autocode.AutocodeReportResponseRaw
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LpAutocodeManagerTest extends AnyFunSuite with MockitoSupport with MockedFeatures with BeforeAndAfter {
  implicit val trigger = PartnerRequestTrigger.Unknown
  implicit val t: Traced = Traced.empty

  private val unificator = mock[Unificator]
  private val rawStorageManager = mock[RawStorageManager[LicensePlate]]
  private val identifiersManager: IdentifiersManager = mock[IdentifiersManager]
  private val vinAutocodeManager = mock[VinAutocodeManager]

  private val lpAutocodeManager = new LpAutocodeManager(
    unificator,
    rawStorageManager,
    identifiersManager,
    vinAutocodeManager
  )

  before {
    reset(vinAutocodeManager)
    reset(identifiersManager)
    reset(rawStorageManager)
  }

  test("save identifiers report to lp and vin raw storages") {
    val lp = LicensePlate("A123AA77")
    val vin = VinCode("WP0ZZZ99ZDS114756")

    val identifiersResponse = {
      val identifiersRaw = ResourceUtils.getStringFromResources("/autocode/identifiers/identifiers.json")
      AutocodeReportResponseRaw(identifiersRaw, "200", Json.parse(identifiersRaw).as[AutocodeReportResponse])
    }

    when(vinAutocodeManager.update(?, ?, ?, ?)(?, ?)).thenReturn(Future.unit)
    when(identifiersManager.connect(?, ?, ?, ?)(?)).thenReturn(Future.unit)
    when(rawStorageManager.upsert(?)(?)).thenReturn(Future.unit)

    lpAutocodeManager.updateAndConnect(lp, vin, identifiersResponse, AutocodeReportType.Identifiers).await

    verify(vinAutocodeManager, times(1)).update(
      eq(vin),
      eq(AutocodeReportType.Identifiers),
      eq(identifiersResponse),
      eq(None)
    )(?, ?)
  }

  test("save taxi report only to lp raw storage") {
    val lp = LicensePlate("A123AA77")
    val vin = VinCode("WP0ZZZ99ZDS114756")

    val response = {
      val raw = ResourceUtils.getStringFromResources(
        "/autocode/taxi/autoru_taxi_history_eyJ0eXBlIjoiR1JaIiwiYm9keSI6ItCd0KAyMDY3NyIsInNjaGVtYV92ZXJzaW9uIjoiMS4wIiwic3RvcmFnZXMiOnt9fQ==.json"
      )
      AutocodeReportResponseRaw(raw, "200", Json.parse(raw).as[AutocodeReportResponse])
    }

    when(unificator.unifyHeadOption(?)(?)).thenReturn(Future.successful(None))
    when(vinAutocodeManager.update(?, ?, ?, ?)(?, ?)).thenReturn(Future.unit)
    when(identifiersManager.connect(?, ?, ?, ?)(?)).thenReturn(Future.unit)
    when(rawStorageManager.upsert(?)(?)).thenReturn(Future.unit)

    lpAutocodeManager.updateAndConnect(lp, vin, response, AutocodeReportType.Taxi).await

    verify(vinAutocodeManager, never()).update(?, ?, ?, ?)(?, ?)
  }
}
