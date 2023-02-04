package ru.yandex.auto.vin.decoder.partners.wilgood

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.wilgood.WilgoodExceptions.{WilgoodApiError, WilgoodUnreachableResponse}
import ru.yandex.auto.vin.decoder.partners.wilgood.WilgoodResponseModels.Service
import ru.yandex.vertis.mockito.MockitoSupport

class WilgoodRawModelSpec extends AnyFunSuite with MockitoSupport {

  private val prefix = "/wilgood"

  test("wilgood unreachable error") {
    val vin = VinCode("SJNFDAJ11U1084830")
    intercept[WilgoodUnreachableResponse] {
      WilgoodRawModel.apply("", 0, vin)
    }
  }

  test("wilgood api error") {
    val vin = VinCode("SJNFDAJ11U1084830")
    val raw = ResourceUtils.getStringFromResources(s"$prefix/500.json")
    intercept[WilgoodApiError] {
      WilgoodRawModel.apply(raw, 500, vin)
    }
  }

  test("wilgood unknown vin") {
    val vin = VinCode("SJNFDAJ11U1084830")
    val raw = ResourceUtils.getStringFromResources(s"$prefix/200_not_found.json")
    val rawModel = WilgoodRawModel.apply(raw, 200, vin)
    assert(rawModel.vinInfoResponse.isEmpty)
  }

  test("wilgood ok response") {
    val vin = VinCode("XWEHM812BH0003003")
    val raw = ResourceUtils.getStringFromResources(s"$prefix/200_ok.json")
    val response = WilgoodRawModel.apply(raw, 200, vin).vinInfoResponse.get
    assert(response.message.vin == Some("XWEHM812BH0003003"))
    assert(response.message.serviceBook.mark == Some("KIA"))
    assert(response.message.serviceBook.model == Some("Ceed"))
    assert(
      response.message.serviceBook.name == Some(
        "KIA Ceed 1.6 бензин (130 л.с.) серебристый № Х458ВУ178 VIN XWEHM812BH0003003 2017 г.в."
      )
    )
    assert(response.message.serviceBook.year == Some("2017"))
    val order = response.message.serviceBook.orders.get.head
    assert(order.mileage == "75161")
    assert(order.stoCity == "Балашиха")
    assert(order.orderDate == "2019-11-23")
    assert(order.description == "ДВС: Диагностика бензинового,\\nтроит ")
    val expectedServices = Seq(
      Service("Чтение ошибок сканером"),
      Service("Углы установки колес - экспресс проверка"),
      Service("Контрольный осмотр по 41 параметру")
    )
    assert(order.services == expectedServices)
  }
}
