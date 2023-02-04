package ru.yandex.auto.vin.decoder.partners.wilgood

import auto.carfax.common.utils.tracing.Traced
import org.joda.time.format.DateTimeFormat
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.wilgood.WilgoodResponseModels.Service
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory.Status
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils

import scala.jdk.CollectionConverters._

class WilgoodRawToPreparedConverterTest extends AnyFunSuite {

  implicit val t: Traced = Traced.empty

  private val prefix = "/wilgood"
  private val dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  private val converter = new WilgoodRawToPreparedConverter

  test("can convert WilgoodRawModel with data to VinInfoHistory") {
    val raw = ResourceUtils.getStringFromResources(s"$prefix/200_ok.json")
    val vin = VinCode("XWEHM812BH0003003")
    val model = WilgoodRawModel.apply(raw, 200, vin)
    val converted = converter.convert(model).await
    assert(converted.getEventType == EventType.WILGOOD_SERVICE_BOOK)
    assert(converted.getVin == vin.toString)
    assert(converted.getStatus == Status.OK)

    assert(converted.getServiceBook.getMark == "KIA")
    assert(converted.getServiceBook.getModel == "Ceed")
    assert(
      converted.getServiceBook.getName == "KIA Ceed 1.6 бензин (130 л.с.) серебристый № Х458ВУ178 VIN XWEHM812BH0003003 2017 г.в."
    )
    assert(converted.getServiceBook.getYear == 2017)
    val order = converted.getServiceBook.getOrdersList.get(0)
    assert(order.getMileage == 75161)
    assert(order.getStoCity == "Балашиха")
    assert(order.getOrderDate == dateTimeFormat.parseDateTime("2019-11-23").getMillis)
    assert(order.getDescription == "ДВС: Диагностика бензинового,\\nтроит ")
    val expectedServices = Seq(
      Service("Чтение ошибок сканером").toMessage,
      Service("Углы установки колес - экспресс проверка").toMessage,
      Service("Контрольный осмотр по 41 параметру").toMessage
    )
    assert(order.getServicesList.asScala == expectedServices)
  }

  test("can convert WilgoodRawModel with no data to VinInfoHistory") {
    val raw = ResourceUtils.getStringFromResources(s"$prefix/200_not_found.json")
    val vin = VinCode("XWEHM812BH0003003")
    val model = WilgoodRawModel.apply(raw, 200, vin)
    val converted = converter.convert(model).await
    assert(converted.getEventType == EventType.WILGOOD_SERVICE_BOOK)
    assert(converted.getVin == vin.toString)
    assert(converted.getStatus == Status.OK)
  }
}
