package ru.yandex.auto.vin.decoder.partners.acat

import auto.carfax.common.utils.tracing.Traced
import cats.syntax.option._
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.Json
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.acat.model.AcatVehicleRawModelToPreparedConverter
import ru.yandex.auto.vin.decoder.proto.TtxSchema.{BodyType, EngineType, GearType, Transmission}
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator.MarkModelResult
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AcatRawModelToPreparedConverterTest extends AnyFunSuite with MockitoSupport {

  val unificator = mock[Unificator]
  val converter = new AcatVehicleRawModelToPreparedConverter(unificator)
  implicit val t: Traced = Traced.empty

  test("vehicle found by KL1SF08DJ8B350714") {
    val vin = VinCode("KL1SF08DJ8B350714")
    val raw = ResourceUtils.getStringFromResources(s"/acat/acat_found.json")

    when(unificator.unifyHeadOption(?)(?))
      .thenReturn(
        Future.successful(
          MarkModelResult("Chevrolet", "Kalos / Aveo / Sonic", "Kalos / Aveo / Sonic", unclear = false).some
        )
      )

    val rawModel = AcatRawModel.apply(Json.parse(raw), 200, vin)
    val prepared = converter.convert(rawModel).await

    assert(rawModel.optData.map(_.vins).nonEmpty)
    assert(prepared.getVin == vin.toString)
    assert(prepared.getEventType == EventType.ACAT_INFO)

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

  test("vehicle found by JSAFJB33V00122511") {
    val vin = VinCode("JSAFJB33V00122511")
    val raw = ResourceUtils.getStringFromResources(s"/acat/acat_found_JSAFJB33V00122511.json")

    when(unificator.unifyHeadOption(?)(?))
      .thenReturn(
        Future.successful(
          MarkModelResult("Suzuki", "Jimny", "Suzuki Jimny SN413V", unclear = false).some
        )
      )

    val rawModel = AcatRawModel.apply(Json.parse(raw), 200, vin)
    val prepared = converter.convert(rawModel).await

    assert(rawModel.optData.map(_.vins).nonEmpty)
    assert(prepared.getVin == vin.toString)
    assert(prepared.getEventType == EventType.ACAT_INFO)

    assert(prepared.getOptionsCount == 15)
    assert(prepared.getOptions(0).getCode == "ENGINE NUMBER")
    assert(prepared.getOptions(0).getRawName == "G13BB648053")
    assert(prepared.getVendorCodes.getEngineCode == "G13BB648053")

    assert(prepared.getTtx.getMark == "Suzuki")
    assert(prepared.getTtx.getModel == "Jimny")
    assert(prepared.getTtx.getRawMarkModel == "Suzuki Jimny SN413V")
    assert(prepared.getTtx.getBodyType == BodyType.UNKNOWN_BODY_TYPE)
    assert(prepared.getTtx.getTransmission == Transmission.UNKNOWN_TRANSMISSION)
    assert(prepared.getTtx.getGearType == GearType.UNKNOWN_DRIVE)
    assert(prepared.getTtx.getYear == 0)
    assert(prepared.getTtx.getEngineType == EngineType.UNKNOWN_ENGINE_TYPE)
  }

  test("not found by U5YFF24227L420768") {
    val vin = VinCode("U5YFF24227L420768")
    val raw = "{\"vins\":null}"

    when(unificator.unifyHeadOption(?)(?))
      .thenReturn(
        Future.successful(
          None
        )
      )

    val rawModel = AcatRawModel.apply(Json.parse(raw), 200, vin)
    val prepared = converter.convert(rawModel).await

    assert(prepared.getVin == vin.toString)
    assert(prepared.getEventType == EventType.ACAT_INFO)
    assert(prepared.getOptionsCount == 0)
  }

  test("process failure correctly when there is limit exceed response from acat") {
    val vin = VinCode("U5YFF24227L420768")
    val raw = "{\"code\":200,\"message\":\"Вы исчерпали лимит поиска по VIN\"}"

    when(unificator.unifyHeadOption(?)(?))
      .thenReturn(
        Future.successful(
          None
        )
      )

    intercept[RuntimeException] {
      val rawModel = AcatRawModel.apply(Json.parse(raw), 200, vin)
      converter.convert(rawModel).await
    }
  }
}
