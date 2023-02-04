package ru.yandex.auto.vin.decoder.partners.filter

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json._
import ru.auto.api.vin.event.VinReportEventType.EventType.FILTER_SERVICE_BOOK
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.filter.FilterResponses.HistoryResponse
import ru.yandex.auto.vin.decoder.raw.filter.FilterRawModel

class FilterRawModelTest extends AnyFunSuite {
  val prefix = "/filter"

  private val vinCode = VinCode("XTT315196C0516055")

  test("parse success response") {
    val raw = ResourceUtils.getStringFromResources(s"$prefix/found.json")
    val code = 200

    val parsed = FilterRawModel(raw, code, vinCode, Json.parse(raw).asOpt[HistoryResponse])
    assert(parsed.identifier.toString === "XTT315196C0516055")
    assert(parsed.rawStatus === "200")
    assert(parsed.source == FILTER_SERVICE_BOOK)

    val history = parsed.historyResponse.head
    val order = history.orders.head
    val work = order.works.getOrElse(Seq.empty).head
    val product = order.products.getOrElse(Seq.empty).head

    assert(history.brand === Option("Suzuki"))
    assert(history.model === Option("Grand Vitara"))
    assert(history.year === 2012)

    assert(order.mileage === "97557")
    assert(order.region === "Иркутск")
    assert(order.date === "2020-04-13")

    assert(work.name === "Снятие-установка защиты двигателя")
    assert(work.summ === 400)
    assert(work.count === 1)
    assert(work.price === 400)

    assert(product.name === "Масло моторное синтетическое Mobil Super 3000 XE 5W30, разл., ACEA C3 1 литр.")
    assert(product.summ === 2880)
    assert(product.unit === "л.")
    assert(product.count === 4.8f)
    assert(product.price === 600)
  }

  test("unexpected code") {
    val raw = ""
    val code = 404

    val parsed = FilterRawModel(raw, code, vinCode, None)

    assert(parsed.historyResponse === None)

  }

  test("invalid format") {
    val raw = "{}"
    val code = 200

    intercept[JsResultException] {
      FilterRawModel(raw, code, vinCode, Some(Json.parse(raw).as[HistoryResponse]))
    }
  }
}
