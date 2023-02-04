package ru.yandex.auto.vin.decoder.partners.nbki

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.PartnerExceptions.PartnerResponseConversionException

class NbkiResponseConverterTest extends AnyFunSuite {

  val Converter = NbkiResponseConverter

  test("valid response with empty pledges") {
    val rawEmpty = ResourceUtils.getStringFromResources(s"/nbki/successful_empty_response.xml")
    val unmarshalled = Converter.convert(rawEmpty)
    assert(unmarshalled.Response == Nil)
    assert(unmarshalled.Result == 0)
  }

  test("valid response with existing pledges") {
    val rawPledges = ResourceUtils.getStringFromResources(s"/nbki/successful_nonempty_response.xml")
    val unmarshalled = Converter.convert(rawPledges)
    assert(unmarshalled.Response.size == 1)
    assert(unmarshalled.Result == 1)
  }

  test("valid response with error") {
    val rawPledges = ResourceUtils.getStringFromResources(s"/nbki/error_response.b64")
    val decrypted = Converter.decrypt(rawPledges)
    val unmarshalled = Converter.convert(decrypted)
    assert(unmarshalled.Response == Nil)
    assert(unmarshalled.Result == 2)
    assert(unmarshalled.ErrorCode.get == "003")
  }

  test("broken base64") {
    assertThrows[PartnerResponseConversionException](Converter.decrypt("1234"))
  }

  test("broken xml") {
    assertThrows[PartnerResponseConversionException](
      Converter.convert(ResourceUtils.getStringFromResources(s"/nbki/broken_response.xml"))
    )
  }

  test("incorrect incoming vin xml") {
    val rawPledges = ResourceUtils.getStringFromResources(s"/nbki/nbki_clientside_error_response.xml")
    val vin = VinCode("WP1ZZZ92ZGLA80455")
    val model = NbkiPledgeRawModel(vin, "200", rawPledges)
    assert(model.pledgePeriods.isEmpty)
    assert(model.rawStatus == "200")
  }

  test("multiline garbage before xml") {
    val validBody = "<?xml /><Body></Body>"
    val withGarbage = s"\n\n\n\n\n\n\n@#\n@edsa$validBody"
    assert(Converter.replaceGarbage(withGarbage) == validBody)
  }
}
