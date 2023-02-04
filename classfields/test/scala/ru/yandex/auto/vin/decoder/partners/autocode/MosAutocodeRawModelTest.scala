package ru.yandex.auto.vin.decoder.partners.autocode

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.autocode.AutocodeMosResponses.ResponseStatus
import ru.yandex.auto.vin.decoder.utils.http.PartnerWarningException

class MosAutocodeRawModelTest extends AnyFunSuite {

  private val TestVin = VinCode.apply("XWEHC812AA0001038")

  test("convert success") {
    val raw = ResourceUtils.getStringFromResources("/mos.autocode/with_accidents.json")

    val model = MosAutocodeRawModel.apply(TestVin, 200, raw)
    assert(model.nonEmpty)
//    assert(model.get.hash == "f6b601d571ae6b1a968c7ec3672b0cb7")
    assert(model.get.identifier == TestVin)
    assert(model.get.groupId == "")
    assert(model.get.source == EventType.AUTOCODE_MOS)
    assert(model.get.rawStatus == "200")
    assert(model.get.response.status == ResponseStatus.Success)
    assert(model.get.response.response.nonEmpty)
  }

  test("convert not found") {
    val raw = ResourceUtils.getStringFromResources("/mos.autocode/not_found.json")

    val model = MosAutocodeRawModel.apply(TestVin, 200, raw)

    assert(model.nonEmpty)
//    assert(model.get.hash == "7a46c2a451af3c4dd493fe12aa094fc3")
    assert(model.get.identifier == TestVin)
    assert(model.get.groupId == "")
    assert(model.get.source == EventType.AUTOCODE_MOS)
    assert(model.get.rawStatus == "200")
    assert(model.get.response.status == ResponseStatus.NotFound)
    assert(model.get.response.response.isEmpty)
  }

  test("unexpected response code") {
    val raw = ResourceUtils.getStringFromResources("/mos.autocode/with_accidents.json")

    intercept[IllegalArgumentException] {
      MosAutocodeRawModel.apply(TestVin, 404, raw)
    }
  }

  test("convert error") {
    val raw = ResourceUtils.getStringFromResources("/mos.autocode/error.json")

    intercept[PartnerWarningException] {
      MosAutocodeRawModel.apply(TestVin, 200, raw)
    }
  }

  // https://st.yandex-team.ru/CARFAX-887#607f2ea2975555057da1ad27
  test("convert success by empty") {
    val raw = ResourceUtils.getStringFromResources("/mos.autocode/empty_but_not_errors.json")

    val res = MosAutocodeRawModel.apply(TestVin, 200, raw)
    assert(res.isEmpty)
  }

  test("hash equals (ignore shared link)") {
    val raw1 = ResourceUtils.getStringFromResources("/mos.autocode/hash/response1.json")
    val raw2 = ResourceUtils.getStringFromResources("/mos.autocode/hash/response2.json")

    val rawModel1 = MosAutocodeRawModel.apply(TestVin, 200, raw1)
    val rawModel2 = MosAutocodeRawModel.apply(TestVin, 200, raw2)

    assert(rawModel1.nonEmpty)
    assert(rawModel2.nonEmpty)

    assert(rawModel1.get.hash == rawModel2.get.hash)
//    assert(rawModel1.get.hash == "6744cfcaf13541229c8715be7a269734")
  }

}
