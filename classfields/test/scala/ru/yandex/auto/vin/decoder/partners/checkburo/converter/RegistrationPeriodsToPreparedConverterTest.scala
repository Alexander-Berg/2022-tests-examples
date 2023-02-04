package ru.yandex.auto.vin.decoder.partners.checkburo.converter

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.checkburo.CheckburoReportType
import ru.yandex.auto.vin.decoder.proto.VinHistory
import ru.yandex.auto.vin.decoder.proto.VinHistory.{RegistrationEvent, VinInfoHistory}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils

class RegistrationPeriodsToPreparedConverterTest extends AnyFunSuite {

  implicit private val t = Traced.empty
  private val vin = VinCode("XW7BF4FKX0S127312")
  private val converter = new RegistrationPeriodsToPreparedConverter

  test("Can parse empty periods list") {
    val raw = ResourceUtils.getStringFromResources("/checkburo/owners/empty_200.json")
    val model = CheckburoReportType.RegActions.parse(vin, 200, raw)
    val converted = converter.convert(model).await
    val expected = VinInfoHistory
      .newBuilder()
      .setVin(vin.toString)
      .setEventType(EventType.CHECKBURO_OWNERS)
      .setStatus(VinInfoHistory.Status.OK)
      .setRegistration(
        VinHistory.Registration
          .newBuilder()
          .setMark("UNKNOWN_MARK")
          .setModel("UNKNOWN_MODEL")
      )
      .build()
    assert(expected == converted)
  }

  test("Can parse non-empty periods list") {
    val raw = ResourceUtils.getStringFromResources("/checkburo/owners/non_empty_200.json")
    val model = CheckburoReportType.RegActions.parse(vin, 200, raw)
    val converted = converter.convert(model).await
    val expected = VinInfoHistory
      .newBuilder()
      .setVin(vin.toString)
      .setEventType(EventType.CHECKBURO_OWNERS)
      .setStatus(VinInfoHistory.Status.OK)
      .setRegistration(
        VinHistory.Registration
          .newBuilder()
          .addPeriods(
            RegistrationEvent
              .newBuilder()
              .setFrom(1344988800000L)
              .setTo(1384732800000L)
              .setOwner("PERSON")
              .setOperationTypeId("11")
              .setOperationType("первичная регистрация")
          )
          .addPeriods(
            RegistrationEvent
              .newBuilder()
              .setFrom(1384732800000L)
              .setTo(1406851200000L)
              .setOwner("PERSON")
              .setOperationTypeId("03")
              .setOperationType(
                "Изменение собственника (владельца) в результате совершения сделки, вступления в наследство, слияние и разделение капитала у юридического лица, переход права по договору лизинга, судебные решения и др."
              )
          )
      )
      .build()
    assert(expected == converted)
  }
}
