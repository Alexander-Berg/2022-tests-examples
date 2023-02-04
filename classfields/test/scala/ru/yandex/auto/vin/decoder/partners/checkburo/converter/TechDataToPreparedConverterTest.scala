package ru.yandex.auto.vin.decoder.partners.checkburo.converter

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.vin.Event
import ru.yandex.auto.vin.decoder.partners.checkburo.CheckburoReportType
import ru.yandex.auto.vin.decoder.partners.checkburo.model.CheckburoExceptions.EmptyBalance
import ru.yandex.auto.vin.decoder.proto.VinHistory
import ru.yandex.auto.vin.decoder.proto.VinHistory.{Registration, VinInfoHistory}
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator.MarkModelResult
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TechDataToPreparedConverterTest extends AnyFunSuite with MockitoSupport with BeforeAndAfter {

  implicit private val t = Traced.empty
  private val vin = VinCode("XW7BF4FKX0S127312")
  private val unificator = mock[Unificator]
  private val converter = new TechDataToPreparedConverter(unificator)

  before {
    val mm = MarkModelResult("AUDI", "A6", "AUDI A6", unclear = false)
    when(unificator.unifyHeadOption(?)(?)).thenReturn(Future.successful(Some(mm)))
  }

  test("Can parse empty vinData") {
    val raw = ResourceUtils.getStringFromResources("/checkburo/vinData/empty_200.json")
    val model = CheckburoReportType.TechData.parse(vin, 200, raw)
    val converted = converter.convert(model).await
    val expected = VinInfoHistory
      .newBuilder()
      .setVin(vin.toString)
      .setEventType(EventType.CHECKBURO_TTX)
      .setStatus(VinInfoHistory.Status.OK)
      .setRegistration(Registration.newBuilder().setMark(Event.NoMark).setModel(Event.NoModel))
      .build()
    assert(expected == converted)
  }

  test("Can parse partially filled vinData for car that is not exiting in gibdd") {
    val raw = ResourceUtils.getStringFromResources("/checkburo/vinData/gibdd_not_found.json")
    val model = CheckburoReportType.TechData.parse(vin, 200, raw)
    val converted = converter.convert(model).await
    val expected = VinInfoHistory
      .newBuilder()
      .setVin(vin.toString)
      .setEventType(EventType.CHECKBURO_TTX)
      .setStatus(VinInfoHistory.Status.OK)
      .setRegistration(Registration.newBuilder().setMark(Event.NoMark).setModel(Event.NoModel))
      .build()
    assert(expected == converted)
  }

  test("Can parse non-empty vinData") {
    val raw = ResourceUtils.getStringFromResources("/checkburo/vinData/non_empty_200.json")
    val model = CheckburoReportType.TechData.parse(vin, 200, raw)
    val converted = converter.convert(model).await
    val expected = VinInfoHistory
      .newBuilder()
      .setVin(vin.toString)
      .setEventType(EventType.CHECKBURO_TTX)
      .setStatus(VinInfoHistory.Status.OK)
      .setRegistration(
        VinHistory.Registration
          .newBuilder()
          .setMark("AUDI")
          .setModel("A6")
          .setColor("СЕРЕБРИСТЫЙ")
          .setYear(2012)
          .setPowerHp(180)
          .setDisplacement(1984)
          .setRawMarkModel("AUDI A6")
          .setVehicleType("Легковые автомобили седан")
      )
      .build()
    assert(expected == converted)
  }

  test("Cannot parse vinData on empty balance") {
    val raw = ResourceUtils.getStringFromResources("/checkburo/vinData/empty_balance.json")
    intercept[EmptyBalance](CheckburoReportType.TechData.parse(vin, 200, raw))
  }
}
