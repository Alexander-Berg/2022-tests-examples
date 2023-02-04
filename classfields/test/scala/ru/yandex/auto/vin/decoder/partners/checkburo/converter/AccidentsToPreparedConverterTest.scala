package ru.yandex.auto.vin.decoder.partners.checkburo.converter

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.checkburo.CheckburoReportType
import ru.yandex.auto.vin.decoder.proto.VinHistory.{Accident, VinInfoHistory}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils

import scala.jdk.CollectionConverters._

class AccidentsToPreparedConverterTest extends AnyFunSuite {

  implicit private val t: Traced = Traced.empty
  private val vin = VinCode("XW7BF4FKX0S127312")
  private val converter = new AccidentsToPreparedConverter

  test("Can parse empty accident list") {
    val raw = ResourceUtils.getStringFromResources("/checkburo/accidents/empty_200.json")
    val model = CheckburoReportType.Accidents.parse(vin, 200, raw)
    val converted = converter.convert(model).await
    val expected = VinInfoHistory
      .newBuilder()
      .setVin(vin.toString)
      .setEventType(EventType.CHECKBURO_ACCIDENTS)
      .setStatus(VinInfoHistory.Status.OK)
      .build()
    assert(expected == converted)
  }

  test("Can parse non-empty accident list") {
    val raw = ResourceUtils.getStringFromResources("/checkburo/accidents/non_empty_200.json")
    val model = CheckburoReportType.Accidents.parse(vin, 200, raw)
    val converted = converter.convert(model).await
    val expected = VinInfoHistory
      .newBuilder()
      .setVin(vin.toString)
      .setEventType(EventType.CHECKBURO_ACCIDENTS)
      .setStatus(VinInfoHistory.Status.OK)
      .addAccidents(
        Accident
          .newBuilder()
          .setAccidentType("Иной вид ДТП")
          .setMark("AUDI")
          .setModel("А6")
          .addAllDamageCodes(List("119").asJava)
          .setDate(1561580340000L)
          .setNumber(380005215.toString)
          .setRegion("Курская область")
          .setState("Повреждено")
          .setYear(2012)
      )
      .addAccidents(
        Accident
          .newBuilder()
          .setAccidentType("Столкновение")
          .setMark("AUDI")
          .setModel("А6")
          .addAllDamageCodes(List("07", "08").asJava)
          .setDate(1486078200000L)
          .setNumber(380001845.toString)
          .setRegion("Курская область")
          .setState("Повреждено")
          .setYear(2012)
      )
      .build()
    assert(expected == converted)
  }
}
