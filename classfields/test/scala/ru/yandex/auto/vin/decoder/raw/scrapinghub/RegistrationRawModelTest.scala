package ru.yandex.auto.vin.decoder.raw.scrapinghub

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.VinReportModel.OwnerType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.raw.scrapinghub.ScrapinghubExceptions.{
  ShErrorResponseException,
  ShInvalidFormatException
}

import scala.io.Source

class RegistrationRawModelTest extends AnyFunSuite {

  private val TestVin = VinCode.apply("XTT315196C0516055")

  test("found registration") {
    val code = 200
    val raw = getRaw("registration/registration-success.json")

    val result: RegistrationRawModel = RegistrationRawModel.apply(TestVin, code, raw)
    val optData = result.result

    assert(result.identifier === TestVin)
    assert(result.raw === raw)
    assert(result.rawStatus == code.toString)
    assert(optData.nonEmpty)
    assert(optData.get.vehicle.rawPowerHp === Some("122.22"))
    assert(optData.get.vehicle.optPowerHp === Some(122))
    assert(optData.get.vehicle.optYear === Some(2010))
    assert(optData.get.vehicle.rawColor === Some("СИНИЙ"))
    assert(optData.get.periods.length === 2)

    assert(optData.get.periods(0).ownerType === OwnerType.Type.PERSON)
    assert(optData.get.periods(0).rawOwnerType === "Natural")
    assert(optData.get.periods(0).from === 1276128000000L)
    assert(optData.get.periods(0).to.contains(1436475600000L))

    assert(optData.get.periods(1).ownerType === OwnerType.Type.PERSON)
    assert(optData.get.periods(1).rawOwnerType === "Natural")
    assert(optData.get.periods(1).from === 1436475600000L)
    assert(optData.get.periods(1).to.isEmpty)

    assert(optData.get.passport.get.number.contains("39НА201433"))
    assert(optData.get.passport.get.issue.contains("ООО \"ЭЛЛАДА ИНТЕРТРЕЙД\""))

  }

  test("not found registration") {
    val code = 200
    val raw = getRaw("registration/registration-not-found.json")

    val result = RegistrationRawModel.apply(TestVin, code, raw)
    val optData = result.result

    assert(result.identifier === TestVin)
    assert(result.raw === raw)
    assert(result.rawStatus == code.toString)
    assert(optData.isEmpty)
  }

  test("invalid vin") {
    val code = 200
    val raw = getRaw("registration/registration-invalid-vin.json")

    val result = RegistrationRawModel.apply(TestVin, code, raw)
    val optData = result.result

    assert(result.identifier === TestVin)
    assert(result.raw === raw)
    assert(result.rawStatus == code.toString)
    assert(optData.isEmpty)
  }

  test("invalid captcha should not pass") {
    val code = 200
    val raw = getRaw("failed-captcha.json")

    intercept[ShInvalidFormatException] {
      RegistrationRawModel.apply(TestVin, code, raw)
    }
  }

  test("response with error should not pass") {
    val code = 200
    val raw = getRaw("error.json")

    intercept[ShErrorResponseException] {
      RegistrationRawModel.apply(TestVin, code, raw)
    }
  }

  test("check hash registration with status 200") {
    val code = 200
    val raw = getRaw("registration/registration-success.json")
    val rawSecond = getRaw("registration/for_compute_hash/registration-success.json")

    val result: RegistrationRawModel = RegistrationRawModel.apply(TestVin, code, raw)
    val resultSecond: RegistrationRawModel = RegistrationRawModel.apply(TestVin, code, rawSecond)

    assert(result.hash == resultSecond.hash)
  }

  test("check hash registration with status 201") {
    val code = 200
    val raw = getRaw("registration/registration-invalid-vin.json")
    val rawSecond = getRaw("registration/for_compute_hash/registration-invalid-vin.json")

    val result: RegistrationRawModel = RegistrationRawModel.apply(TestVin, code, raw)
    val resultSecond: RegistrationRawModel = RegistrationRawModel.apply(TestVin, code, rawSecond)

    assert(result.hash == resultSecond.hash)
  }

  test("check hash registration with status 404") {
    val code = 200
    val raw = getRaw("registration/registration-not-found.json")
    val rawSecond = getRaw("registration/for_compute_hash/registration-not-found.json")

    val result: RegistrationRawModel = RegistrationRawModel.apply(TestVin, code, raw)
    val resultSecond: RegistrationRawModel = RegistrationRawModel.apply(TestVin, code, rawSecond)

    assert(result.hash == resultSecond.hash)
  }

  test("check hash registration with empty markmodel") {
    val code = 200
    val raw = getRaw("registration/registration-with-empty-markmodel.json")
    val rawSecond = getRaw("registration/for_compute_hash/registration-with-empty-markmodel.json")

    val result: RegistrationRawModel = RegistrationRawModel.apply(TestVin, code, raw)
    val resultSecond: RegistrationRawModel = RegistrationRawModel.apply(TestVin, code, rawSecond)

    assert(result.hash == resultSecond.hash)
  }

  private def getRaw(filename: String): String = {
    val stream = getClass.getResourceAsStream(s"/scrapinghub/gibdd/$filename")
    val result = Source.fromInputStream(stream, "UTF-8").mkString
    stream.close()
    result
  }
}
