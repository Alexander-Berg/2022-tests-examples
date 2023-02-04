package ru.yandex.auto.vin.decoder.raw.scrapinghub

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.raw.scrapinghub.ScrapinghubExceptions.{
  ShErrorResponseException,
  ShInvalidFormatException
}

import scala.io.Source

class AccidentsRawModelTest extends AnyFunSuite {

  private val TestVin = VinCode.apply("XTT315196C0516055")

  test("accidents exists") {
    val code = 200
    val raw = getRaw("accidents/accidents-exists.json")

    val result = AccidentsRawModel.apply(TestVin, code, raw)
    val optData = result.result

    assert(optData.nonEmpty)
    assert(optData.get.accidents.length == 2)
    assert(optData.get.accidents(0).accidentType === "Столкновение")
    assert(optData.get.accidents(0).accidentNumber === "880001227")
    assert(optData.get.accidents(0).regionName === "Республика Марий Эл")
    assert(optData.get.accidents(0).damagePoints === Seq("01", "04"))
  }

  test("accidents not found") {
    val code = 200
    val raw = getRaw("accidents/accidents-not-found.json")

    val result = AccidentsRawModel.apply(TestVin, code, raw)
    val optData = result.result

    assert(optData.nonEmpty)
    assert(optData.get.accidents.isEmpty)
  }

  test("invalid captcha should not pass") {
    val code = 200
    val raw = getRaw("failed-captcha.json")

    intercept[ShInvalidFormatException] {
      AccidentsRawModel.apply(TestVin, code, raw)
    }
  }

  test("response with error should not pass") {
    val code = 200
    val raw = getRaw("error.json")

    intercept[ShErrorResponseException] {
      AccidentsRawModel.apply(TestVin, code, raw)
    }
  }

  test("check hash accidents exists") {
    val code = 200
    val raw = getRaw("accidents/accidents-exists.json")
    val rawSecond = getRaw("accidents/for_compute_hash/accidents-exists.json")

    val result = AccidentsRawModel.apply(TestVin, code, raw)
    val resultSecond = AccidentsRawModel.apply(TestVin, code, rawSecond)

    assert(result.hash == resultSecond.hash)
  }

  test("check hash accidents not found") {
    val code = 200
    val raw = getRaw("accidents/accidents-not-found.json")
    val rawSecond = getRaw("accidents/for_compute_hash/accidents-not-found.json")

    val result = AccidentsRawModel.apply(TestVin, code, raw)
    val resultSecond = AccidentsRawModel.apply(TestVin, code, rawSecond)

    assert(result.hash == resultSecond.hash)
  }

  private def getRaw(filename: String): String = {
    val stream = getClass.getResourceAsStream(s"/scrapinghub/gibdd/$filename")
    val result = Source.fromInputStream(stream, "UTF-8").mkString
    stream.close()
    result
  }
}
