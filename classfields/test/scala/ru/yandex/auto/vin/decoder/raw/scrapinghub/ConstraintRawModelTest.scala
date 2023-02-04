package ru.yandex.auto.vin.decoder.raw.scrapinghub

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.raw.scrapinghub.ScrapinghubExceptions.{
  ShErrorResponseException,
  ShInvalidFormatException
}

import scala.io.Source

class ConstraintRawModelTest extends AnyFunSuite {

  private val TestVin = VinCode.apply("XTT315196C0516055")

  test("found constraints") {
    val code = 200
    val raw = getRaw("constraints/constraints-exists.json")

    val result = ConstraintsRawModel.apply(TestVin, code, raw)
    val optData = result.result

    assert(optData.nonEmpty)
    assert(optData.get.count == 1)
    assert(optData.get.error == 0)
    assert(optData.get.records.length == 1)
    assert(optData.get.records.head.place === "Республика Дагестан")
    assert(
      optData.get.records.head.reason ===
        "Документ: 253287961/0522 от 07.11.2019, Магадаев Низам Имиралиевич, СПИ: 82221008300335, ИП: 120528/19/05022-ИП от 06.11.2019"
    )
    assert(optData.get.records.head.gibddKey === "5#SP174793263")
    assert(optData.get.records.head.optYear === Some(2013))
    assert(optData.get.records.head.rawDate === "07.11.2019")
    assert(optData.get.records.head.phone === "+7(8722)67-45-87")
    assert(optData.get.records.head.dateAdd === "08.11.2019")
  }

  test("not found constraints") {
    val code = 200
    val raw = getRaw("constraints/constraints-not-found.json")

    val result = ConstraintsRawModel.apply(TestVin, code, raw)
    val optData = result.result

    assert(optData.nonEmpty)
    assert(optData.get.count == 0)
    assert(optData.get.error == 0)
    assert(optData.get.records.isEmpty)
  }

  test("invalid captcha should not pass") {
    val code = 200
    val raw = getRaw("failed-captcha.json")

    intercept[ShInvalidFormatException] {
      ConstraintsRawModel.apply(TestVin, code, raw)
    }
  }

  test("response with error should not pass") {
    val code = 200
    val raw = getRaw("error.json")

    intercept[ShErrorResponseException] {
      ConstraintsRawModel.apply(TestVin, code, raw)
    }
  }

  test("check hash constraints-exists") {
    val code = 200
    val raw = getRaw("constraints/constraints-exists.json")
    val rawSecond = getRaw("constraints/for_compute_hash/constraints-exists.json")

    val result = ConstraintsRawModel.apply(TestVin, code, raw)
    val resultSecond = ConstraintsRawModel.apply(TestVin, code, rawSecond)

    assert(result.hash == resultSecond.hash)
  }

  test("check hash constraints-not-found") {
    val code = 200
    val raw = getRaw("constraints/constraints-not-found.json")
    val rawSecond = getRaw("constraints/for_compute_hash/constraints-not-found.json")

    val result = ConstraintsRawModel.apply(TestVin, code, raw)
    val resultSecond = ConstraintsRawModel.apply(TestVin, code, rawSecond)

    assert(result.hash == resultSecond.hash)
  }

  private def getRaw(filename: String): String = {
    val stream = getClass.getResourceAsStream(s"/scrapinghub/gibdd/$filename")
    val result = Source.fromInputStream(stream, "UTF-8").mkString
    stream.close()
    result
  }

}
