package ru.yandex.auto.vin.decoder.partners.checkburo.converter

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.checkburo.CheckburoReportType
import ru.yandex.auto.vin.decoder.proto.VinHistory
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils

class WantedToPreparedConverterTest extends AnyFunSuite {

  implicit private val t = Traced.empty
  private val vin = VinCode("XW7BF4FKX0S127312")
  private val converter = new WantedToPreparedConverter

  test("Can parse empty wanted list") {
    val raw = ResourceUtils.getStringFromResources("/checkburo/wanted/empty_200.json")
    val model = CheckburoReportType.Wanted.parse(vin, 200, raw)
    val converted = converter.convert(model).await
    val expected = VinInfoHistory
      .newBuilder()
      .setVin(vin.toString)
      .setEventType(EventType.CHECKBURO_WANTED)
      .setStatus(VinInfoHistory.Status.OK)
      .build()
    assert(expected == converted)
  }

  test("Can parse non-empty wanted list") {
    val raw = ResourceUtils.getStringFromResources("/checkburo/wanted/non_empty_200.json")
    val model = CheckburoReportType.Wanted.parse(vin, 200, raw)
    val converted = converter.convert(model).await
    val expected = VinInfoHistory
      .newBuilder()
      .setVin(vin.toString)
      .setEventType(EventType.CHECKBURO_WANTED)
      .setStatus(VinInfoHistory.Status.OK)
      .addWanted(
        VinHistory.Wanted
          .newBuilder()
          .setRegion("Ленинградская область")
          .setDate(1591920000000L)
      )
      .build()
    assert(expected == converted)
  }
}
