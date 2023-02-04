package ru.yandex.auto.vin.decoder.partners.checkburo.converter

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.checkburo.CheckburoReportType
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils

class FinesToPreparedConverterTest extends AnyWordSpecLike {
  implicit private val t = Traced.empty
  private val vin = VinCode("XW7BF4FKX0S127312")
  private val converter = new FinesToPreparedConverter

  "FinesToPreparedConverter" should {
    "convert response" in {
      val raw = ResourceUtils.getStringFromResources("/checkburo/fines/successful_response.json")
      val model = CheckburoReportType.Fines.parse(vin, 200, raw)
      val converted = converter.convert(model).await

      assert(70 == converted.getFinesCount)
      assert(EventType.CHECKBURO_FINES == converted.getEventType)

      val sampleFine = converted.getFines(15)
      assert(
        "12.9Ч.2 - Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 км/ч" ==
          sampleFine.getArticleDescription
      )
      assert(500.0 == sampleFine.getAmountTotal)
      assert(!sampleFine.getIsPaid.getValue)
      assert(1568851200000L == sampleFine.getDateEvent)

      val sampleFineWithFloatingPointAmount = converted.getFines(57)
      assert(8556.66 == sampleFineWithFloatingPointAmount.getAmountTotal)
    }

    "convert empty response" in {
      val raw = ResourceUtils.getStringFromResources("/checkburo/fines/empty_response.json")
      val model = CheckburoReportType.Fines.parse(vin, 200, raw)
      val converted = converter.convert(model).await
      assert(0 == converted.getFinesCount)
      assert(EventType.CHECKBURO_FINES == converted.getEventType)

    }
  }
}
