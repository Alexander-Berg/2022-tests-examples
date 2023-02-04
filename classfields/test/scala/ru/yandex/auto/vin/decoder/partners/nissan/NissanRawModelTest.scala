package ru.yandex.auto.vin.decoder.partners.nissan

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.JsResultException
import ru.auto.api.vin.event.VinReportEventType.EventType.NISSAN_MANUFACTURER_SERVICE_BOOK
import ru.yandex.auto.vin.decoder.model.VinCode

class NissanRawModelTest extends AnyFunSuite with BeforeAndAfterAll {
  val prefix = "/nissan"

  private val vin = "SJNFDAJ11U1084830"
  private val vinCode = VinCode(vin)

  test("parse success response") {
    val raw = ResourceUtils.getStringFromResources(s"$prefix/found.json")
    val code = 200

    val parsed = NissanRawModel.apply(raw, code, vinCode)
    assert(parsed.identifier.toString === vin)
    assert(parsed.rawStatus === "200")
    assert(parsed.source == NISSAN_MANUFACTURER_SERVICE_BOOK)

    val vinInfo = parsed.vinInfoResponse.get

    assert(vinInfo.vin === vin)
    assert(vinInfo.brand === Some("NISSAN"))
    assert(vinInfo.model === Some("QASHQAI J11"))
    assert(vinInfo.wsd.nonEmpty)
    assert(vinInfo.wsd.get.toInstant.getMillis === 1410480000000L)
    assert(vinInfo.recallCampaings.isEmpty)
    assert(vinInfo.serviceContracts.isEmpty)
    assert(vinInfo.rsa.nonEmpty)

    val rsa = vinInfo.rsa.get
    assert(rsa.startDate.toInstant.getMillis === 1559779200000L)
    assert(rsa.endDate.toInstant.getMillis === 1591401600000L)

    val carVisit = vinInfo.visits.head

    assert(carVisit.id === 26184966)
    assert(carVisit.date.toInstant.getMillis === 1559779200000L)
    assert(carVisit.`type` === "Техническое обслуживание (ТО)")
    assert(carVisit.mileage === Some(56615))
    assert(carVisit.outletCity === "Москва")
    assert(carVisit.outletCode === "21200501")
    assert(carVisit.outletName === "Автоспеццентр Химки")

    val programs = vinInfo.ns3Contracts

    assert(programs.length == 3)

    assert(programs.head.subscriptionDate.toInstant.getMillis == 1583280000000L)
    assert(programs.head.startDate.toInstant.getMillis == 1615161600000L)
    assert(programs.head.endDate.toInstant.getMillis == 1741305600000L)
    assert(programs.head.name == "Контракт NS3+ 12 месяцев или 125000 км.")

    assert(programs(1).subscriptionDate.toInstant.getMillis == 1583280000000L)
    assert(programs(1).startDate.toInstant.getMillis == 1646697600000L)
    assert(programs(1).endDate.toInstant.getMillis == 1678147200000L)
    assert(programs(1).name == "Контракт NS3+ 12 месяцев или 150000 км.")

    assert(programs(2).subscriptionDate.toInstant.getMillis == 1583280000000L)
    assert(programs(2).startDate.toInstant.getMillis == 1678233600000L)
    assert(programs(2).endDate.toInstant.getMillis == 1709769600000L)
    assert(programs(2).name == "Контракт NS3+ 12 месяцев или 200000 км.")
  }

  test("not found should be processed") {
    val raw = ""
    val code = 404

    val model = NissanRawModel(raw, code, vinCode)

    assert(model.vinInfoResponse === None)

  }

  test("invalid format") {
    val raw = "{}"
    val code = 200

    intercept[JsResultException] {
      NissanRawModel.apply(raw, code, vinCode)
    }
  }

  test("wrong vin code format") {
    val raw = ResourceUtils.getStringFromResources(s"$prefix/found.json")
    val code = 200

    intercept[IllegalArgumentException] {
      NissanRawModel.apply(raw, code, VinCode("Mickey"))
    }
  }

  test("vin codes mismatch") {
    val raw = ResourceUtils.getStringFromResources(s"$prefix/found.json")
    val code = 200

    intercept[IllegalStateException] {
      NissanRawModel.apply(raw, code, VinCode("SJNFDAJ11U1084831"))
    }
  }

  test("response with error should be processed ") {
    val raw = "[{\"vin\": \"SJNFDAJ11U1084830\", \"error\": \"error message\"}]"
    val code = 200

    val model = NissanRawModel(raw, code, vinCode)

    assert(model.vinInfoResponse === None)
  }
}
