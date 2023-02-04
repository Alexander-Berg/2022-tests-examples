package ru.yandex.auto.vin.decoder.raw.scrapinghub

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.raw.scrapinghub.ScrapinghubExceptions.{
  ShErrorResponseException,
  ShInvalidFormatException
}

import scala.io.Source

class WantedRawModelTest extends AnyFunSuite {

  private val TestVin = VinCode.apply("XTT315196C0516055")

  test("found wanted") {
    val code = 200
    val raw = getRaw("wanted/wanted-exists.json")

    val result = WantedRawModel.apply(TestVin, code, raw)
    val optData = result.result

    assert(optData.nonEmpty)
    assert(optData.get.count == 1)
    assert(optData.get.error == 0)
    assert(optData.get.records.length == 1)
    assert(optData.get.records.head.city === "город Москва")
    assert(optData.get.records.head.rawDate === "06.05.2010")
    assert(optData.get.records.head.model === "МАЗДА3")
    assert(optData.get.records.head.year === "2008")
  }

  test("wanted not found") {
    val code = 200
    val raw = getRaw("wanted/wanted-not-found.json")

    val result = WantedRawModel.apply(TestVin, code, raw)
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
      WantedRawModel.apply(TestVin, code, raw)
    }
  }

  test("response with error should not pass") {
    val code = 200
    val raw = getRaw("error.json")

    intercept[ShErrorResponseException] {
      WantedRawModel.apply(TestVin, code, raw)
    }
  }

  test("check hash accidents exists") {
    val code = 200
    val raw = getRaw("wanted/wanted-exists.json")
    val rawSecond = getRaw("wanted/for_compute_hash/wanted-exists.json")

    val result = WantedRawModel.apply(TestVin, code, raw)
    val resultSecond = WantedRawModel.apply(TestVin, code, rawSecond)

    assert(result.hash == resultSecond.hash)
  }

  test("check hash accidents not found") {
    val code = 200
    val raw = getRaw("wanted/wanted-not-found.json")
    val rawSecond = getRaw("wanted/for_compute_hash/wanted-not-found.json")

    val result = WantedRawModel.apply(TestVin, code, raw)
    val resultSecond = WantedRawModel.apply(TestVin, code, rawSecond)

    assert(result.hash == resultSecond.hash)
  }

  private def getRaw(filename: String): String = {
    val stream = getClass.getResourceAsStream(s"/scrapinghub/gibdd/$filename")
    val result = Source.fromInputStream(stream, "UTF-8").mkString
    stream.close()
    result
  }
}
