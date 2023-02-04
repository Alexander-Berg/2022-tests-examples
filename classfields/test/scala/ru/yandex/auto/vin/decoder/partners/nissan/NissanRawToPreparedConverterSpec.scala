package ru.yandex.auto.vin.decoder.partners.nissan

import auto.carfax.common.utils.misc.ResourceUtils
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory.Status

import scala.jdk.CollectionConverters.ListHasAsScala

class NissanRawToPreparedConverterSpec extends AnyWordSpecLike with Matchers {

  implicit val t: Traced = Traced.empty

  private val vin = "SJNFDAJ11U1084830"
  private val vinCode = VinCode(vin)
  private val raw = ResourceUtils.getStringFromResources("/nissan/found.json")
  private val code = 200
  private val rawModel = NissanRawModel.apply(raw, code, vinCode)
  val converter = new NissanRawToPreparedConverter

  "NissanRawToPreparedConverter" should {

    "convert raw model to VinINfoHistory" in {

      val res = converter.convert(rawModel).futureValue

      val orders = res.getServiceBook.getOrdersList.asScala
      val programs = res.getProgramsList.asScala

      res.getVin shouldBe vin
      res.getEventType shouldBe EventType.NISSAN_MANUFACTURER_SERVICE_BOOK
      res.getStatus shouldBe Status.OK

      res.getServiceBook.getMark shouldBe "NISSAN"
      res.getServiceBook.getModel shouldBe "QASHQAI J11"
      orders.length shouldBe 7
      programs.length shouldBe 3

      orders.head.getOrderDate shouldBe 1559779200000L
      orders.head.getMileage shouldBe 56615
      orders.head.getStoName shouldBe "Автоспеццентр Химки"
      orders.head.getStoCity shouldBe "Москва"
      orders.head.getDescription shouldBe "Техническое обслуживание (ТО)"
      orders.head.getUid shouldBe "26184966"

      orders(1).getOrderDate shouldBe 1505242490000L
      orders(1).getMileage shouldBe 45771
      orders(1).getStoName shouldBe "Автоцентр на Таганке"
      orders(1).getStoCity shouldBe "Москва"
      orders(1).getDescription shouldBe "Коммерческий ремонт"
      orders(1).getUid shouldBe "16331177"

      orders(2).getOrderDate shouldBe 1467849600000L
      orders(2).getMileage shouldBe 25177
      orders(2).getStoName shouldBe "Автомир на м. ВДНХ"
      orders(2).getStoCity shouldBe "Москва"
      orders(2).getDescription shouldBe "Техническое обслуживание (ТО)"
      orders(2).getUid shouldBe "10057901"

      orders(3).getOrderDate shouldBe 1439337600000L
      orders(3).getMileage shouldBe 6300
      orders(3).getStoName shouldBe "Автоспеццентр Химки"
      orders(3).getStoCity shouldBe "Москва"
      orders(3).getDescription shouldBe "Коммерческий ремонт"
      orders(3).getUid shouldBe "7966388"

      orders(4).getOrderDate shouldBe 1418169600000L
      orders(4).getMileage shouldBe 5
      orders(4).getStoName shouldBe "Автоспеццентр Химки"
      orders(4).getStoCity shouldBe "Москва"
      orders(4).getDescription shouldBe "Установка доп. оборудования"
      orders(4).getUid shouldBe "7950123"

      orders(5).getOrderDate shouldBe 1418083200000L
      orders(5).getMileage shouldBe 5
      orders(5).getStoName shouldBe "Автоспеццентр Химки"
      orders(5).getStoCity shouldBe "Москва"
      orders(5).getDescription shouldBe "Установка доп. оборудования"
      orders(5).getUid shouldBe "7950085"

      orders(6).getOrderDate shouldBe 1418083200000L
      orders(6).getMileage shouldBe 5
      orders(6).getStoName shouldBe "Автоспеццентр Химки"
      orders(6).getStoCity shouldBe "Москва"
      orders(6).getDescription shouldBe "Установка доп. оборудования"
      orders(6).getUid shouldBe "7950087"

      programs.head.getProgramId shouldBe "ns3_contract"
      programs.head.getIsActive.getValue shouldBe true
      programs.head.getDate shouldBe 1583280000000L
      programs.head.getProgramStartTimestamp shouldBe 1615161600000L
      programs.head.getProgramFinishTimestamp shouldBe 1741305600000L
      programs.head.getProgramName shouldBe "Контракт NS3+ 12 месяцев или 125000 км."

      programs(1).getProgramId shouldBe "ns3_contract"
      programs(1).getIsActive.getValue shouldBe true
      programs(1).getDate shouldBe 1583280000000L
      programs(1).getProgramStartTimestamp shouldBe 1646697600000L
      programs(1).getProgramFinishTimestamp shouldBe 1678147200000L
      programs(1).getProgramName shouldBe "Контракт NS3+ 12 месяцев или 150000 км."

      programs(2).getProgramId shouldBe "ns3_contract"
      programs(2).getIsActive.getValue shouldBe true
      programs(2).getDate shouldBe 1583280000000L
      programs(2).getProgramStartTimestamp shouldBe 1678233600000L
      programs(2).getProgramFinishTimestamp shouldBe 1709769600000L
      programs(2).getProgramName shouldBe "Контракт NS3+ 12 месяцев или 200000 км."
    }
  }

}
