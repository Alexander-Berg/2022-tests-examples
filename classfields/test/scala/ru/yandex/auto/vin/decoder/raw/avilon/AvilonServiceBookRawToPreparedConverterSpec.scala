package ru.yandex.auto.vin.decoder.raw.avilon

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.clients.avilon.model.AvilonServiceBookItemModel
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory.Status

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset}
import scala.jdk.CollectionConverters.ListHasAsScala

class AvilonServiceBookRawToPreparedConverterSpec extends AnyWordSpecLike with Matchers {

  private val vinCode = VinCode("Z8T4C5FS9BM005269")
  implicit val t: Traced = Traced.empty

  "AvilonServiceBookRawToPreparedConverter" must {

    "correctly convert AvilonServiceBookRawModel to vinInfoHistory" in {

      val model = rawModel(records("guaranty"))
      val res = new AvilonServiceBookRawToPreparedConverter().convert(model).futureValue

      res.getVin shouldBe vinCode.toString
      res.getEventType shouldBe EventType.AVILON_SERVICE_BOOK
      res.getStatus shouldBe Status.OK
      res.getServiceBook.getMark shouldBe "Фольксваген"
      res.getServiceBook.getModel shouldBe "Jetta 6"
      res.getServiceBook.getYear shouldBe 2015
      res.getServiceBook.getOrdersCount shouldBe 1
      res.getServiceBook.getOrders(0).getOrderDate shouldBe ldt.toInstant(ZoneOffset.ofHours(3)).toEpochMilli
      res.getServiceBook.getOrders(0).getMileage shouldBe 100500
      res.getServiceBook.getOrders(0).getStoCity shouldBe "Москва"
      res.getServiceBook.getOrders(0).getDescription shouldBe "Ремонт по гарантии"
      (res.getServiceBook.getOrders(0).getServicesList.asScala should have).length(2)
      res.getServiceBook.getOrders(0).getServicesList.asScala.map(_.getName) should contain(
        "Аккумуляторная батарея зарядить"
      )
      res.getServiceBook.getOrders(0).getServicesList.asScala.map(_.getName) should contain(
        "Рулевой механизм EML снять и установить"
      )
      (res.getServiceBook.getOrders(0).getProductsList.asScala should have).length(5)
      res.getServiceBook.getOrders(0).getProductsList.asScala.map(_.getName) should contain("Болт (kombi)")
      res.getServiceBook.getOrders(0).getProductsList.asScala.map(_.getName) should contain(
        "Болт с шестигр. гол. (комби) м10х35"
      )
      res.getServiceBook.getOrders(0).getProductsList.asScala.map(_.getName) should contain("Болт м10х75")
      res.getServiceBook.getOrders(0).getProductsList.asScala.map(_.getName) should contain("Болт м10х76")
      res.getServiceBook.getOrders(0).getProductsList.asScala.map(_.getName) should contain("Болт м8х35х22")
    }

    "correctly convert AvilonServiceBookRawModel to vinInfoHistory then type is body_insurance" in {

      val model = rawModel(records("body_insurance"))
      val res = new AvilonServiceBookRawToPreparedConverter().convert(model).futureValue

      res.getServiceBook.getOrders(0).getDescription shouldBe "Кузовной ремонт за счет СК"
    }

    "correctly convert AvilonServiceBookRawModel to vinInfoHistory then type is ordinary_insurance" in {

      val model = rawModel(records("ordinary_insurance"))
      val res = new AvilonServiceBookRawToPreparedConverter().convert(model).futureValue

      res.getServiceBook.getOrders(0).getDescription shouldBe "Текущий ремонт за счет СК"
    }

    "correctly convert AvilonServiceBookRawModel to vinInfoHistory then type is body_paid" in {

      val model = rawModel(records("body_paid"))
      val res = new AvilonServiceBookRawToPreparedConverter().convert(model).futureValue

      res.getServiceBook.getOrders(0).getDescription shouldBe "Кузовной ремонт за свой счет"
    }

    "correctly convert AvilonServiceBookRawModel to vinInfoHistory then type is ordinary_paid" in {

      val model = rawModel(records("ordinary_paid"))
      val res = new AvilonServiceBookRawToPreparedConverter().convert(model).futureValue

      res.getServiceBook.getOrders(0).getDescription shouldBe "Текущий ремонт за свой счет"
    }

    "correctly convert AvilonServiceBookRawModel to vinInfoHistory then type is ordinary_dealer" in {

      val model = rawModel(records("ordinary_dealer"))
      val res = new AvilonServiceBookRawToPreparedConverter().convert(model).futureValue

      res.getServiceBook.getOrders(0).getDescription shouldBe "Текущий ремонт"
    }

    "correctly convert AvilonServiceBookRawModel to vinInfoHistory then type is regulation" in {

      val model = rawModel(records("regulation"))
      val res = new AvilonServiceBookRawToPreparedConverter().convert(model).futureValue

      res.getServiceBook.getOrders(0).getDescription shouldBe "Регламентное ТО"
    }

    "throw an exception then type is unknown" in {

      val model = rawModel(records("some unknown type"))
      an[IllegalArgumentException] should be thrownBy
        new AvilonServiceBookRawToPreparedConverter().convert(model).futureValue
    }

    "correctly convert AvilonServiceBookRawModel to vinInfoHistory then records is empty" in {

      val model = rawModel(Nil)
      val res = new AvilonServiceBookRawToPreparedConverter().convert(model).futureValue

      res.getVin shouldBe vinCode.toString
      res.getEventType shouldBe EventType.AVILON_SERVICE_BOOK
      res.getStatus shouldBe Status.OK
    }

  }

  val ldt = LocalDateTime.parse("2016-07-28 19:18:32", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

  def records(`type`: String) = List(
    AvilonServiceBookItemModel(
      vinCode,
      "123",
      Some(ldt),
      "Москва",
      "Москва",
      `type`,
      "Фольксваген",
      "Jetta 6",
      100500,
      List("Аккумуляторная батарея зарядить", "Рулевой механизм EML снять и установить"),
      List("Болт (kombi)", "Болт с шестигр. гол. (комби) м10х35", "Болт м10х75", "Болт м10х76", "Болт м8х35х22"),
      1,
      2015
    )
  )

  def rawModel(records: List[AvilonServiceBookItemModel]) =
    AvilonServiceBookRawModel("", "200", vinCode, records)
}
