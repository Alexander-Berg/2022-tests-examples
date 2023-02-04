package ru.yandex.auto.vin.decoder.partners.infiniti

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType.INFINITI_SERVICE_BOOK
import ru.yandex.auto.vin.decoder.model.VinCode

import java.time.{LocalDate, ZoneOffset}

class InfinitiRawModelTest extends AnyFunSuite with BeforeAndAfterAll {
  val prefix = "/infiniti"

  private val vin = VinCode("3PCMANJ55Z0552634")

  test("parse success response") {
    val raw = ResourceUtils.getStringFromResources(s"$prefix/found.json")
    val code = 200

    val parsed = InfinitiRawModel(raw, code, vin)
    assert(parsed.identifier.toString === s"""$vin""")
    assert(parsed.rawStatus === "200")
    assert(parsed.source == INFINITI_SERVICE_BOOK)

    assert(parsed.visits.nonEmpty)
    assert(
      LocalDate
        .parse(parsed.visits.head.dateOpen, InfinitiRawModel.DateFormat)
        .atStartOfDay
        .toInstant(ZoneOffset.UTC)
        .toEpochMilli === 1590364800000L
    )
    assert(parsed.visits.head.mileage === 10038)
    assert(parsed.visits.head.vin === vin.toString)
    assert(parsed.visits.head.dealerName === """ООО "Арес Авто"""")
    assert(parsed.visits.head.purchaseTags.nonEmpty)

  }

  test(" response with not found code") {
    val raw = "[]"
    val code = 404

    val parsed = InfinitiRawModel(raw, code, vin)

    assert(parsed.visits.isEmpty)

  }

  test("invalid format") {
    val raw = "{}"
    val code = 200

    intercept[InfinitiApiException] {
      InfinitiRawModel(raw, code, vin)
    }
  }

}
