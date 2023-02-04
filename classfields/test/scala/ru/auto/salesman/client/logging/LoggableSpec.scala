package ru.auto.salesman.client.logging

import ru.auto.api.ResponseModel.VinResolutionResponse
import ru.auto.api.vin.VinResolutionEnums
import ru.auto.salesman.service.logging.Loggable
import ru.auto.salesman.test.BaseSpec

class LoggableSpec extends BaseSpec {

  "Loggable" should {
    "not break VinResolution russian text, as protobuf.Message#toString does" in {
      val b = VinResolutionResponse.newBuilder()
      val text = "Указанный VIN принадлежит другому автомобилю"
      b.getResolutionBuilder
        .setStatus(VinResolutionEnums.Status.INVALID)
        .getSummaryBuilder
        .setText(text)
      val resolution = b.build()
      implicitly[Loggable[Option[VinResolutionResponse]]]
        .toLogString(Some(resolution)) should include(text)
    }
  }
}
