package ru.yandex.auto.vin.decoder.raw.converters.scrapinghub

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.raw.scrapinghub.AccidentsRawModel
import ru.yandex.auto.vin.decoder.raw.scrapinghub.converters.RawAccidentsToPreparedConverter

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

class RawAccidentsToPreparedConverterTest extends AnyFunSuite {

  private val TestVin = VinCode.apply("XWEHC812AA0001038")

  val converter = new RawAccidentsToPreparedConverter
  implicit val t: Traced = Traced.empty

  test("convert accidents") {
    val code = 200
    val rawResponse = getRaw("accidents-exists.json")

    val rawModel = AccidentsRawModel.apply(TestVin, code, rawResponse)
    val prepared = Await.result(converter.convert(rawModel), 1.second)

    assert(prepared.getVin === TestVin.toString)
    assert(prepared.getEventType == EventType.SH_GIBDD_ACCIDENTS)
    assert(prepared.getStatus == VinInfoHistory.Status.OK)

    assert(prepared.getAccidentsCount == 2)

    assert(prepared.getAccidents(0).getNumber === "880001227")
    assert(prepared.getAccidents(0).getAccidentType === "Столкновение")
    assert(prepared.getAccidents(0).getDate === 1486408200000L)
    assert(prepared.getAccidents(0).getDamageCodesCount == 2)

    assert(prepared.getAccidents(1).getNumber === "330021921")
    assert(prepared.getAccidents(1).getAccidentType === "Столкновение")
    assert(prepared.getAccidents(1).getDate === 1480890600000L)
    assert(prepared.getAccidents(1).getDamageCodesCount == 0)
  }

  test("convert empty") {
    val code = 200
    val rawResponse = getRaw("accidents-not-found.json")

    val rawModel = AccidentsRawModel.apply(TestVin, code, rawResponse)
    val prepared = Await.result(converter.convert(rawModel), 1.second)

    assert(prepared.getVin === TestVin.toString)
    assert(prepared.getEventType == EventType.SH_GIBDD_ACCIDENTS)
    assert(prepared.getStatus == VinInfoHistory.Status.OK)
    assert(prepared.getAccidentsCount == 0)
  }

  private def getRaw(filename: String): String = {
    val stream = getClass.getResourceAsStream(s"/scrapinghub/gibdd/accidents/$filename")
    val result = Source.fromInputStream(stream, "UTF-8").mkString
    stream.close()
    result
  }

}
