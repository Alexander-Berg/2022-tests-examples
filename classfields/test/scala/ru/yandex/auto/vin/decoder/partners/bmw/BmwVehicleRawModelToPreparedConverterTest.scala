package ru.yandex.auto.vin.decoder.partners.bmw

import auto.carfax.common.utils.tracing.Traced
import cats.implicits._
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.Json
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.bmw.model.BmwVehicleRawModelToPreparedConverter
import ru.yandex.auto.vin.decoder.proto.TtxSchema.{BodyType, EngineType, GearType, Transmission}
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator.MarkModelResult
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BmwVehicleRawModelToPreparedConverterTest extends AnyFunSuite with MockitoSupport {

  val unificator = mock[Unificator]
  val converter = new BmwVehicleRawModelToPreparedConverter(unificator)
  implicit val t: Traced = Traced.empty

  test("vehicle found") {
    val vin = VinCode("X4XFH611700B43238")
    val raw = ResourceUtils.getStringFromResources("/bmw/vehicle_found.json")

    when(unificator.unifyHeadOption(?)(?))
      .thenReturn(Future.successful(MarkModelResult("BMW", "X6", "BMW X6", unclear = false).some))

    val rawModel = BmwVehicleRawModel.apply(Json.parse(raw), 200, vin)
    val prepared = converter.convert(rawModel).await

    assert(rawModel.optVehicle.nonEmpty)
    assert(prepared.getVin == vin.toString)
    assert(prepared.getEventType == EventType.BMW_VEHICLE_INFO)

    assert(prepared.getOptionsCount == 96)
    assert(prepared.getOptions(0).getCode == "S01CA")
    assert(prepared.getOptions(0).getRawName == "Выбор автомобиля важного для СОР")

    assert(prepared.getTtx.getMark == "BMW")
    assert(prepared.getTtx.getModel == "X6")
    assert(prepared.getTtx.getRawMarkModel == "BMW X6")
    assert(prepared.getTtx.getBodyType == BodyType.ALLROAD)
    assert(prepared.getTtx.getTransmission == Transmission.AUTO)
    assert(prepared.getTtx.getGearType == GearType.ALL_WHEEL_DRIVE)
    assert(prepared.getTtx.getYear == 2013)
    assert(prepared.getTtx.getEngineType == EngineType.DIESEL)

    assert(prepared.getVendorCodes.getComplectationCode == "xDrive30d")
  }

}
