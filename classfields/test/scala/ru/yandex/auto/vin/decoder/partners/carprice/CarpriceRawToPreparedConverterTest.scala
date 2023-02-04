package ru.yandex.auto.vin.decoder.partners.carprice

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils

class CarpriceRawToPreparedConverterTest extends AnyFunSuite {

  val converter = new CarpriceRawToPreparedConverter
  implicit val t: Traced = Traced.empty

  test("success response") {
    val vin = VinCode("SJNFDAJ11U1084830")
    val raw = ResourceUtils.getStringFromResources(s"/carprice/success.json")
    val model = CarpriceRawModel.apply(200, raw, vin)
    val converted = converter.convert(model).await

    assert(converted.getStatus == VinInfoHistory.Status.OK)
    assert(converted.getVin == "SJNFDAJ11U1084830")
    assert(converted.getGroupId == "2017-09-01")
    assert(converted.getEventType == EventType.CARPRICE_MILEAGE)
    assert(converted.getMileageCount == 1)
    assert(converted.getMileage(0).getValue == 223000)
    assert(converted.getMileage(0).getCity == "Санкт-Петербург и ЛО")
    assert(converted.getMileage(0).getDate == 1504224000000L)
  }

  test("not found") {
    val vin = VinCode("SJNFDAJ11U1084830")
    val raw = ResourceUtils.getStringFromResources(s"/carprice/not_found.json")
    val model = CarpriceRawModel.apply(400, raw, vin)
    val converted = converter.convert(model).await

    assert(converted.getStatus == VinInfoHistory.Status.OK)
    assert(converted.getVin == "SJNFDAJ11U1084830")
    assert(converted.getGroupId == "")
    assert(converted.getEventType == EventType.CARPRICE_MILEAGE)
    assert(converted.getMileageCount == 0)
  }

}
