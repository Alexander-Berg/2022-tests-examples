package ru.yandex.auto.vin.decoder.partners.mazda

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils

import scala.jdk.CollectionConverters.ListHasAsScala

class MazdaRawToPreparedConverterTest extends AnyFunSuite {

  private val vinCode = VinCode("SJNFDAJ11U1084830")
  private val converter = new MazdaRawToPreparedConverter
  implicit val t: Traced = Traced.empty

  test("success") {
    val raw = ResourceUtils.getStringFromResources(s"/mazda/success.xml")

    val model = MazdaRawModel(raw, 200, vinCode)
    val converted = converter.convert(model).await

    assert(converted.getVin == vinCode.toString)
    assert(converted.getEventType == EventType.MAZDA_SERVICE_BOOK)
    assert(converted.hasServiceBook)

    val orders = converted.getServiceBook.getOrdersList.asScala

    assert(orders.length == 12)
    assert(orders.head.getMileage == 40110)
    assert(orders.head.getDescription == "")
    assert(orders.head.getOrderDate == 1272340800000L)
    assert(orders.head.getProductsCount == 0)
    assert(orders.head.getRecommendationListCount == 0)
    assert(orders.head.getServicesCount == 4)
    assert(orders.head.getStoCity == "Санкт-Петербург")
    assert(orders.head.getStoId == "")
    assert(orders.head.getStoName == "ООО \"Евросиб-Авто\"")

    val work = orders.head.getServices(0)
    assert(work.getName == "Замена тормозной жидкости")
  }

  test("not found") {
    val raw = ResourceUtils.getStringFromResources(s"/mazda/not_found.xml")

    val model = MazdaRawModel(raw, 200, vinCode)
    val converted = converter.convert(model).await

    assert(converted.getVin == vinCode.toString)
    assert(converted.getEventType == EventType.MAZDA_SERVICE_BOOK)
    assert(!converted.hasServiceBook)
  }

  test("record should contain valid mobility warranty") {
    val raw = ResourceUtils.getStringFromResources(s"/mazda/mob_warranty.xml")

    val model = MazdaRawModel(raw, 200, vinCode)
    val converted = converter.convert(model).await

    assert(converted.getVin == vinCode.toString)
    assert(converted.getProgramsList.asScala.count(_.getIsActive.getValue) == 1)
    assert(converted.getProgramsList.asScala.size == 1)
    assert(converted.getEventType == EventType.MAZDA_SERVICE_BOOK)
  }
}
