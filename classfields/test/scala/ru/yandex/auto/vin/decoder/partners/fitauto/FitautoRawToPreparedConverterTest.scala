package ru.yandex.auto.vin.decoder.partners.fitauto

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils

class FitautoRawToPreparedConverterTest extends AnyFunSuite {

  private val vinCode = VinCode("SJNFDAJ11U1084830")
  private val converter = new FitautoRawToPreparedConverter
  implicit val t: Traced = Traced.empty

  test("success") {
    val raw = ResourceUtils.getStringFromResources(s"/fitauto/success.xml")

    val model = FitautoRawModel(raw, 200, vinCode)
    val converted = converter.convert(model).await

    assert(converted.getVin == vinCode.toString)
    assert(converted.getEventType == EventType.FITAUTO_SERVICE_BOOK)

    val sb = converted.getServiceBook
    assert(sb.getMark == "MAZDA")
    assert(sb.getModel == "CX-7")
    assert(sb.getName == "MAZDA CX-72.3 MZR DISI Turbo")
    assert(sb.getYear == 2008)
    assert(sb.getOrdersCount == 1)

    val order = sb.getOrders(0)
    assert(order.getOrderDate == 1500508800000L)
    assert(order.getMileage == 121324)
    assert(order.getStoName == "Уфа, Менделеева, 21Б")
    assert(order.getStoCity == "Уфа")
    assert(order.getDescription == "****")
    assert(order.getRecommendations == "Следующая замена масла двигателя через 5000км пробега")
    assert(order.getProductsCount == 0)
    assert(order.getServicesCount == 2)
    assert(order.getServices(0).getName == "Слесарные работы")
    assert(
      order
        .getServices(1)
        .getName == "Кондиционер (легковые автомобили) - проверка работы, давления хладагента,\n                                    наличия утечек, заправка\n                                "
    )
  }

  test("not found") {
    val raw = ResourceUtils.getStringFromResources(s"/fitauto/not_found.xml")

    val model = FitautoRawModel(raw, 200, vinCode)
    val converted = converter.convert(model).await

    assert(converted.getVin == vinCode.toString)
    assert(converted.getEventType == EventType.FITAUTO_SERVICE_BOOK)
    assert(!converted.hasServiceBook)
  }
}
