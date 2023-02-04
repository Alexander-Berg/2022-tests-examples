package ru.yandex.auto.vin.decoder.partners.nbki

import auto.carfax.common.utils.tracing.Traced
import org.joda.time.LocalDate
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.CommonVinCode
import ru.yandex.auto.vin.decoder.partners.PartnerExceptions.PartnerInconvertibleStatusException
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils

class NbkiPledgeVinHistoryConverterTest extends AnyFunSuite {
  val Converter = new NbkiPledgeVinHistoryConverter
  val NbkiConverter = NbkiResponseConverter
  implicit val t = Traced.empty

  test("convert successful empty response") {
    val vin = CommonVinCode("WFA24E2RDADSD")
    val raw = ResourceUtils.getStringFromResources(s"/nbki/successful_empty_response.xml")
    val convertedResponse =
      Converter.convert(NbkiPledgeRawModel(vin, "200", raw)).await
    assert(convertedResponse.getPledgesCount == 0)
  }

  test("convert successful nonempty response") {
    val vin = CommonVinCode("WAUZZZ8U3HR075806")
    val expectedDate = LocalDate.parse("2026-04-27").toDateTimeAtStartOfDay.getMillis
    val raw = ResourceUtils.getStringFromResources(s"/nbki/successful_nonempty_response.xml")
    val convertedResponse =
      Converter.convert(NbkiPledgeRawModel(vin, "200", raw)).await

    assert(convertedResponse.getPledgesCount == 1)
    assert(convertedResponse.getVin == vin.toString)

    val pledge = convertedResponse.getPledges(0)
    assert(pledge.getPerformanceDate == expectedDate)
    assert(pledge.getDesc == "BMW 320D XDRIVE GT")
  }

  test("incorrect login/password internal nbki error") {
    val vin = CommonVinCode("WAUZZZ8U3HR075806")
    val raw = ResourceUtils.getStringFromResources(s"/nbki/nbki_error_response.xml")
    assertThrows[PartnerInconvertibleStatusException] {
      NbkiPledgeRawModel(vin, "200", raw)
    }
  }
}
