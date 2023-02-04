package ru.yandex.auto.vin.decoder.raw.converters.scrapinghub

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.raw.scrapinghub.WantedRawModel
import ru.yandex.auto.vin.decoder.raw.scrapinghub.converters.RawWantedToPreparedConverter

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

class RawWantedToPreparedConverterTest extends AnyFunSuite {

  private val TestVin = VinCode.apply("XWEHC812AA0001038")
  implicit val t: Traced = Traced.empty

  val converter = new RawWantedToPreparedConverter

  test("convert wanted") {
    val code = 200
    val rawResponse = getRaw("wanted-exists.json")

    val rawModel = WantedRawModel.apply(TestVin, code, rawResponse)
    val prepared = Await.result(converter.convert(rawModel), 1.second)

    assert(prepared.getVin === TestVin.toString)
    assert(prepared.getEventType == EventType.SH_GIBDD_WANTED)
    assert(prepared.getStatus == VinInfoHistory.Status.OK)

    assert(prepared.getWantedCount == 1)
    assert(prepared.getWanted(0).getDate === 1273104000000L)
    assert(prepared.getWanted(0).getRegion === "город Москва")
  }

  test("convert empty") {
    val code = 200
    val rawResponse = getRaw("wanted-not-found.json")

    val rawModel = WantedRawModel.apply(TestVin, code, rawResponse)
    val prepared = Await.result(converter.convert(rawModel), 1.second)

    assert(prepared.getVin === TestVin.toString)
    assert(prepared.getEventType == EventType.SH_GIBDD_WANTED)
    assert(prepared.getStatus == VinInfoHistory.Status.OK)
    assert(prepared.getWantedCount == 0)
  }

  private def getRaw(filename: String): String = {
    val stream = getClass.getResourceAsStream(s"/scrapinghub/gibdd/wanted/$filename")
    val result = Source.fromInputStream(stream, "UTF-8").mkString
    stream.close()
    result
  }

}
