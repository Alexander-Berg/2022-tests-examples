package ru.yandex.auto.vin.decoder.partners.checkburo.converter

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.checkburo.CheckburoReportType
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils

class MileagesToPreparedConverterTest extends AnyWordSpecLike {
  implicit private val t = Traced.empty
  val converter = new MileagesToPreparedConverter
  val vin = VinCode("X7MCF41GPAA233148")

  "MileagesToPreparedConverter" should {

    "convert nonempty checkburo mileages" in {
      val raw = ResourceUtils.getStringFromResources("/checkburo/mileage/successful_response.json")
      val model = CheckburoReportType.Mileages.parse(vin, 200, raw)
      val converted = converter.convert(model).await

      assert(2 == converted.getMileageCount)
      assert(EventType.CHECKBURO_MILEAGE == converted.getEventType)

    }

    "convert empty checkburo mileages" in {
      val raw = ResourceUtils.getStringFromResources("/checkburo/mileage/empty_response.json")
      val model = CheckburoReportType.Mileages.parse(vin, 200, raw)
      val converted = converter.convert(model).await

      assert(converted.getMileageList.isEmpty)
      assert(EventType.CHECKBURO_MILEAGE == converted.getEventType)

    }
  }
}
