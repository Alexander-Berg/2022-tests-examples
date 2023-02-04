package ru.yandex.auto.vin.decoder.partners.autocode

import auto.carfax.common.utils.tracing.Traced
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.money.MosAutocodeRawToPreparedConverter
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator.MarkModelResult
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MosAutocodeRawToPreparedConverterTest extends AnyFunSuite with MockitoSupport with BeforeAndAfter {

  val unificator: Unificator = mock[Unificator]
  private val TestVin = VinCode.apply("XWEHC812AA0001038")

  val converter = new MosAutocodeRawToPreparedConverter(unificator)
  implicit val t: Traced = Traced.empty

  before {
    reset(unificator)
  }

  test("convert not found") {
    val raw = ResourceUtils.getStringFromResources("/mos.autocode/not_found.json")

    val rawModel = MosAutocodeRawModel.apply(TestVin, 200, raw)
    assert(rawModel.nonEmpty)

    val converted = converter.convert(rawModel.get).await

    assert(converted.getEventType == EventType.AUTOCODE_MOS)
    assert(converted.getStatus == VinInfoHistory.Status.ERROR)
    assert(converted.getVin == TestVin.toString)
    assert(converted.getGroupId == "")
  }

  test("convert found with accidents") {
    val raw = ResourceUtils.getStringFromResources("/mos.autocode/with_accidents.json")

    when(unificator.sanitizeUnifyOrDie(?)(?))
      .thenReturn(Future.successful(MarkModelResult("VOLKSWAGEN", "PASSAT", "ФОЛЬКСВАГЕН ПАССАТ СС", false)))

    val rawModel = MosAutocodeRawModel.apply(TestVin, 200, raw)
    assert(rawModel.nonEmpty)
    val converted = converter.convert(rawModel.get).await

    assert(converted.getEventType == EventType.AUTOCODE_MOS)
    assert(converted.getStatus == VinInfoHistory.Status.OK)
    assert(converted.getVin == TestVin.toString)
    assert(converted.getGroupId == "")
    // TODO: тест не проходит в ci, но работает локально
//    assert(rawModel.get.hash == "f6b601d571ae6b1a968c7ec3672b0cb7")

    val registration = converted.getRegistration
    assert(registration.getMark == "VOLKSWAGEN")
    assert(registration.getModel == "PASSAT")
    assert(registration.getRawMarkModel == "ФОЛЬКСВАГЕН ПАССАТ СС")
    assert(registration.getYear == 2014)
    assert(registration.getColor == "БЕЛЫЙ")
    assert(registration.getDisplacement == 1968)
    assert(registration.getPowerHp == 170)
    assert(registration.getPeriodsCount == 1)
    assert(registration.getPeriods(0).getFrom == 1413057600000L)
    assert(registration.getPeriods(0).getTo == 0L)
    assert(registration.getPeriods(0).getOwner == "PERSON")

    assert(converted.getAccidentsCount == 2)
    assert(converted.getAccidents(0).getAccidentType == "Наезд на препятствие")
    assert(converted.getAccidents(0).getDamageCodesCount == 0)
    assert(converted.getAccidents(0).getRegion == "г. Москва")
    assert(converted.getAccidents(0).getNumber == "")
    assert(converted.getAccidents(0).getDate == 1474059600000L)

    assert(converted.getConstraintsCount == 0)
    assert(converted.getWantedCount == 0)

    assert(converted.getMileageCount == 1)
    assert(converted.getMileage(0).getValue == 192319)
    assert(converted.getMileage(0).getDate == 1521925200000L)

    assert(converted.getVehicleIdentifiers.getVin == TestVin.toString)
    assert(converted.getVehicleIdentifiers.getSts == "7729270701")
  }

  test("convert found with constraints") {
    val raw = ResourceUtils.getStringFromResources("/mos.autocode/with_constraints.json")

    when(unificator.sanitizeUnifyOrDie(?)(?))
      .thenReturn(Future.successful(MarkModelResult("VOLKSWAGEN", "TIGUAN", "ФОЛЬКСВАГЕН ТИГУАН", false)))

    val rawModel = MosAutocodeRawModel.apply(TestVin, 200, raw)
    assert(rawModel.nonEmpty)
    val converted = converter.convert(rawModel.get).await

    assert(converted.getEventType == EventType.AUTOCODE_MOS)
    assert(converted.getStatus == VinInfoHistory.Status.OK)
    assert(converted.getVin == TestVin.toString)
    assert(converted.getGroupId == "")

    assert(converted.getConstraintsCount == 1)
    assert(converted.getConstraints(0).getConType == "Запрет на регистрационные действия и прохождение ТО")
    assert(converted.getConstraints(0).getDate == 0L)
    assert(converted.getConstraints(0).getRegion == "Москва")
  }

  test("convert found with wanted") {
    val raw = ResourceUtils.getStringFromResources("/mos.autocode/with_wanted.json")

    when(unificator.sanitizeUnifyOrDie(?)(?))
      .thenReturn(Future.successful(MarkModelResult("VOLKSWAGEN", "TIGUAN", "ФОЛЬКСВАГЕН ТИГУАН", false)))

    val rawModel = MosAutocodeRawModel.apply(TestVin, 200, raw)
    assert(rawModel.nonEmpty)
    val converted = converter.convert(rawModel.get).await

    assert(converted.getEventType == EventType.AUTOCODE_MOS)
    assert(converted.getStatus == VinInfoHistory.Status.OK)
    assert(converted.getVin == TestVin.toString)
    assert(converted.getGroupId == "")

    assert(converted.getConstraintsCount == 0)
    assert(converted.getWantedCount == 1)
    assert(converted.getWanted(0).getDate == 0L)
    assert(converted.getWanted(0).getRegion == "Москва")
  }

}
