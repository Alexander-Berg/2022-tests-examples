package ru.yandex.auto.vin.decoder.raw.converters.scrapinghub

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.raw.scrapinghub.ConstraintsRawModel
import ru.yandex.auto.vin.decoder.raw.scrapinghub.converters.RawConstraintsToPreparedConverter

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

class RawConstraintsToPreparedConverterTest extends AnyFunSuite {

  private val TestVin: VinCode = VinCode.apply("XWEHC812AA0001038")

  val converter = new RawConstraintsToPreparedConverter
  implicit val t: Traced = Traced.empty

  test("convert constraints") {
    val code = 200
    val rawResponse = getRaw("constraints-exists.json")

    val rawModel = ConstraintsRawModel.apply(TestVin, code, rawResponse)
    val prepared = Await.result(converter.convert(rawModel), 1.second)

    assert(prepared.getVin === TestVin.toString)
    assert(prepared.getEventType == EventType.SH_GIBDD_CONSTRAINTS)
    assert(prepared.getStatus == VinInfoHistory.Status.OK)

    assert(prepared.getConstraintsCount == 1)
    assert(prepared.getConstraints(0).getDate === 1573084800000L)
    assert(prepared.getConstraints(0).getYear === 2013)
    assert(prepared.getConstraints(0).getModel === "Нет данных")
    assert(prepared.getConstraints(0).getRegion === "Республика Дагестан")
    assert(
      prepared.getConstraints(0).getReason ===
        "Документ: 253287961/0522 от 07.11.2019, Магадаев Низам Имиралиевич, СПИ: 82221008300335, ИП: 120528/19/05022-ИП от 06.11.2019"
    )
    assert(prepared.getConstraints(0).getConType === "Запрет на регистрационные действия")
  }

  test("convert empty") {
    val code = 200
    val rawResponse = getRaw("constraints-not-found.json")

    val rawModel = ConstraintsRawModel.apply(TestVin, code, rawResponse)
    val prepared = Await.result(converter.convert(rawModel), 1.second)

    assert(prepared.getVin === TestVin.toString)
    assert(prepared.getEventType == EventType.SH_GIBDD_CONSTRAINTS)
    assert(prepared.getStatus == VinInfoHistory.Status.OK)

    assert(prepared.getConstraintsCount == 0)
  }

  private def getRaw(filename: String): String = {
    val stream = getClass.getResourceAsStream(s"/scrapinghub/gibdd/constraints/$filename")
    val result = Source.fromInputStream(stream, "UTF-8").mkString
    stream.close()
    result
  }
}
