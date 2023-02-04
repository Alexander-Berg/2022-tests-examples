package ru.yandex.auto.vin.decoder.raw.converters.scrapinghub

import auto.carfax.common.utils.tracing.Traced
import cats.implicits.catsSyntaxOptionId
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.vin.Event
import ru.yandex.auto.vin.decoder.proto.VinHistory.{Registration, VinInfoHistory}
import ru.yandex.auto.vin.decoder.raw.scrapinghub.RegistrationRawModel
import ru.yandex.auto.vin.decoder.raw.scrapinghub.converters.RawRegistrationToPreparedConverter
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator.MarkModelResult
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source

class RawRegistrationToPreparedConverterTest extends AnyFunSuite with MockitoSupport with BeforeAndAfter {

  private val TestVin = VinCode.apply("XWEHC812AA0001038")
  val unificator: Unificator = mock[Unificator]
  implicit val t: Traced = Traced.empty

  val converter = new RawRegistrationToPreparedConverter(unificator)

  before {
    reset(unificator)
  }

  test("convert registration") {
    when(unificator.unifyHeadOption(?)(?))
      .thenReturn(Future.successful(MarkModelResult("KIA", "CEED", "КИА ЕD (СЕЕD)", unclear = false).some))

    val code = 200
    val raw = getRaw("registration-success.json")

    val rawModel = RegistrationRawModel.apply(TestVin, code, raw)
    val result = Await.result(converter.convert(rawModel), 1.second)

    assert(result.getVin === TestVin.toString)
    assert(result.getEventType === EventType.SH_GIBDD_REGISTRATION)
    assert(result.getStatus === VinInfoHistory.Status.OK)

    assert(result.getRegistration.getMark === "KIA")
    assert(result.getRegistration.getModel === "CEED")
    assert(result.getRegistration.getRawMarkModel === "КИА ЕD (СЕЕD)")
    assert(result.getRegistration.getDisplacement === 1591)
    assert(result.getRegistration.getColor === "СИНИЙ")
    assert(result.getRegistration.getYear === 2010)
    assert(result.getRegistration.getPowerHp === 122)
    assert(result.getRegistration.getVehicleType === "Легковые автомобили универсал")

    assert(result.getRegistration.getPts === "39НА201433")

    assert(result.getRegistration.getPeriodsCount === 2)
    assert(result.getRegistration.getPeriods(0).getOwner === "PERSON")
    assert(result.getRegistration.getPeriods(0).getFrom === 1276128000000L)
    assert(result.getRegistration.getPeriods(0).getTo === 1436475600000L)
    assert(result.getRegistration.getPeriods(0).getOperationTypeId == "16")
    assert(
      result.getRegistration
        .getPeriods(0)
        .getOperationType == "регистрация ТС, прибывших из других регионов Российской Федерации"
    )
    assert(result.getRegistration.getPeriods(1).getOwner === "PERSON")
    assert(result.getRegistration.getPeriods(1).getFrom === 1436475600000L)
    assert(result.getRegistration.getPeriods(1).getTo === 0L)
    assert(result.getRegistration.getPeriods(1).getOperationTypeId == "03")
    assert(
      result.getRegistration
        .getPeriods(1)
        .getOperationType == "Изменение собственника (владельца) в результате совершения сделки, вступления в наследство, слияние и разделение капитала у юридического лица, переход права по договору лизинга, судебные решения и др."
    )
  }

  test("convert registration with empty mark model") {
    val code = 200
    val raw = getRaw("registration-with-empty-markmodel.json")

    val rawModel = RegistrationRawModel.apply(TestVin, code, raw)
    val result = Await.result(converter.convert(rawModel), 1.second)

    assert(result.getVin === TestVin.toString)
    assert(result.getEventType === EventType.SH_GIBDD_REGISTRATION)
    assert(result.getStatus === VinInfoHistory.Status.OK)

    assert(result.getRegistration.getMark === "")
    assert(result.getRegistration.getModel === "")
    assert(result.getRegistration.getRawMarkModel === "")
    assert(result.getRegistration.getDisplacement === 0)
    assert(result.getRegistration.getColor === "СИНИЙ")
    assert(result.getRegistration.getYear === 2012)
    assert(result.getRegistration.getPowerHp === 0)
    assert(result.getRegistration.getVehicleType === "Легковые автомобили прочие")

    assert(result.getRegistration.getPts === "63НМ366155")

    assert(result.getRegistration.getPeriodsCount === 1)
    assert(result.getRegistration.getPeriods(0).getOwner === "PERSON")
    assert(result.getRegistration.getPeriods(0).getFrom === 1327536000000L)
    assert(result.getRegistration.getPeriods(0).getTo === 0L)
  }

  test("convert empty") {
    val code = 200
    val raw = getRaw("registration-not-found.json")
    val rawModel = RegistrationRawModel.apply(TestVin, code, raw)

    val result = Await.result(converter.convert(rawModel), 1.second)

    assert(result.getVin === TestVin.toString)
    assert(result.getStatus === VinInfoHistory.Status.OK)
    assert(result.getEventType === EventType.SH_GIBDD_REGISTRATION)
    assert(result.getRegistration === Registration.newBuilder().setMark(Event.NoMark).setModel(Event.NoModel).build())
  }

  private def getRaw(filename: String): String = {
    val stream = getClass.getResourceAsStream(s"/scrapinghub/gibdd/registration/$filename")
    val result = Source.fromInputStream(stream, "UTF-8").mkString
    stream.close()
    result
  }
}
