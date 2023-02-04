package ru.yandex.auto.vin.decoder.partners.rusauto

import auto.carfax.common.utils.misc.ResourceUtils
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json._
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.rusauto.model.{RusAutoRawModel, RusAutoResponse}

import scala.concurrent.Await
import scala.concurrent.duration._

class RusAutoToPreparedConverterTest extends AnyFunSuite {

  val prefix = "/rusauto"
  val converter = new RusAutoToPreparedConverter
  val vin = VinCode.apply("XTT315196C0516055")
  implicit val t: Traced = Traced.empty

  test("convert vin with orders") {
    val code = 200
    val raw = ResourceUtils.getStringFromResources(s"$prefix/found.json")

    val rawModel = RusAutoRawModel.apply(raw, code, vin, Json.parse(raw).asOpt[RusAutoResponse])
    val converted = Await.result(converter.convert(rawModel), 1.second)
    val sb = converted.getServiceBook

    assert(converted.getEventType === EventType.RUS_AUTO_SERVICE_BOOK)
    assert(converted.getVin === "XTT315196C0516055")
    assert(sb.getMark === "УАЗ")
    assert(sb.getModel === "УАЗ-315196")
    assert(sb.getYear === 2012)
    assert(sb.getOrdersCount === 1)
    assert(sb.getOrders(0).getOrderDate === 1384300800000L)
    assert(sb.getOrders(0).getRecommendationListCount === 1)
    assert(sb.getOrders(0).getMileage === 337)
    assert(sb.getOrders(0).getStoName === "Автоцентр РУС-АВТО")
    assert(sb.getOrders(0).getStoCity === "Великий Новгород")
    assert(sb.getOrders(0).getServicesCount === 3)
    assert(sb.getOrders(0).getProductsCount === 19)
  }
}
