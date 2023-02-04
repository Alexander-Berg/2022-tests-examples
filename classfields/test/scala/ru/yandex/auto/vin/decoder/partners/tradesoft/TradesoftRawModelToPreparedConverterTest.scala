package ru.yandex.auto.vin.decoder.partners.tradesoft

import auto.carfax.common.utils.tracing.Traced
import cats.implicits._
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.Json
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.tradesoft.model.TradesoftVehicleRawModelToPreparedConverter
import ru.yandex.auto.vin.decoder.proto.TtxSchema.{BodyType, EngineType, GearType, Transmission}
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator.MarkModelResult
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TradesoftRawModelToPreparedConverterTest extends AnyFunSuite with MockitoSupport {

  val unificator = mock[Unificator]
  val converter = new TradesoftVehicleRawModelToPreparedConverter(unificator)
  implicit val t: Traced = Traced.empty

  test("vehicle found") {
    val vin = VinCode("KL1SF08DJ8B350714")
    val raw = ResourceUtils.getStringFromResources("/tradesoft/vehicle_found.json")

    when(unificator.unifyHeadOption(?)(?))
      .thenReturn(
        Future.successful(
          MarkModelResult("Chevrolet", "Kalos / Aveo / Sonic", "Kalos / Aveo / Sonic", unclear = false).some
        )
      )

    val rawModel = TradesoftRawModel.apply(Json.parse(raw), 200, vin)
    val prepared = converter.convert(rawModel).await

    assert(rawModel.optVehicle.nonEmpty)
    assert(prepared.getVin == vin.toString)
    assert(prepared.getEventType == EventType.TRADESOFT_INFO)

    assert(prepared.getOptionsCount == 105)
    assert(prepared.getOptions(0).getCode == "04U")
    assert(prepared.getOptions(0).getRawName == "PRIMARY COLOR EXTERIOR, URBAN GREY (06) GMDAT")

    assert(prepared.getTtx.getMark == "Chevrolet")
    assert(prepared.getTtx.getModel == "Kalos / Aveo / Sonic")
    assert(prepared.getTtx.getRawMarkModel == "Kalos / Aveo / Sonic")
    assert(prepared.getTtx.getBodyType == BodyType.HATCHBACK)
    assert(prepared.getTtx.getTransmission == Transmission.MECHANICAL)
    assert(prepared.getTtx.getGearType == GearType.FORWARD_CONTROL)
    assert(prepared.getTtx.getYear == 2009)
    assert(prepared.getTtx.getEngineType == EngineType.UNKNOWN_ENGINE_TYPE)
  }

}
