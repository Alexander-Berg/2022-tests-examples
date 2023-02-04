package ru.yandex.auto.vin.decoder.partners.checkburo.converter

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.checkburo.CheckburoReportType
import ru.yandex.auto.vin.decoder.proto.VinHistory.{Constraint, VinInfoHistory}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils

class ConstraintsToPreparedConverterTest extends AnyFunSuite {

  implicit private val t = Traced.empty
  private val vin = VinCode("XW7BF4FKX0S127312")
  private val converter = new ConstraintsToPreparedConverter

  test("Can parse empty constraint list") {
    val raw = ResourceUtils.getStringFromResources("/checkburo/constraints/empty_200.json")
    val model = CheckburoReportType.Constraints.parse(vin, 200, raw)
    val converted = converter.convert(model).await
    val expected = VinInfoHistory
      .newBuilder()
      .setVin(vin.toString)
      .setEventType(EventType.CHECKBURO_CONSTRAINTS)
      .setStatus(VinInfoHistory.Status.OK)
      .build()
    assert(expected == converted)
  }

  test("Can parse non-empty constraint list") {
    val raw = ResourceUtils.getStringFromResources("/checkburo/constraints/non_empty_200.json")
    val model = CheckburoReportType.Constraints.parse(vin, 200, raw)
    val converted = converter.convert(model).await
    val expected = VinInfoHistory
      .newBuilder()
      .setVin(vin.toString)
      .setEventType(EventType.CHECKBURO_CONSTRAINTS)
      .setStatus(VinInfoHistory.Status.OK)
      .addConstraints(
        Constraint
          .newBuilder()
          .setConType("Запрет на регистрационные действия")
          .setReason(
            "Документ: 115433655/4718 от 09.07.2019, Гребенникова Наталия Борисовна, СПИ: 41180100000082, ИП: 16728/18/47018-ИП от 06.07.2018"
          )
          .setRegion("Ленинградская область")
          .setDate(1562630400000L)
      )
      .build()
    assert(expected == converted)
  }
}
