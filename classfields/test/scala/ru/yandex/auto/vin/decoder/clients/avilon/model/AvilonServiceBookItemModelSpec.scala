package ru.yandex.auto.vin.decoder.clients.avilon.model

import org.scalatest.enablers.Emptiness.emptinessOfOption
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import ru.yandex.auto.vin.decoder.model.VinCode

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AvilonServiceBookItemModelSpec extends AnyWordSpecLike with Matchers {

  "AvilonServiceBookItemModel" must {

    "be correctly parsed from json" in {

      val ldt = LocalDateTime.parse("2016-04-12 20:34:36", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

      val str =
        """{"vin":"Z8T4C5FS9BM005269","id":"123","event_date":"2016-04-12 20:34:36","event_region":"Москва","event_city":"Москва",
           |"type":"ordinary_dealer","brand":"Фольксваген","model":"Jetta 6","mileage":58525,
           |"works":["Аккумуляторная батарея зарядить","Рулевой механизм EML снять и установить"],
           |"products":["Болт (kombi)","Болт с шестигр. гол. (комби) м10х35","Болт м10х75","Болт м10х76","Болт м8х35х22"],
           |"client_type":1,"year":2015,"recommendations":[]}""".stripMargin

      val res = Json.parse(str).as[AvilonServiceBookItemModel]

      res.vin shouldBe VinCode("Z8T4C5FS9BM005269")
      res.id shouldBe "123"
      res.eventDate shouldBe Some(ldt)
      res.eventRegion shouldBe "Москва"
      res.eventCity shouldBe "Москва"
      res.`type` shouldBe "ordinary_dealer"
      res.brand shouldBe "Фольксваген"
      res.model shouldBe "Jetta 6"
      res.mileage shouldBe 58525
      res.works shouldBe List("Аккумуляторная батарея зарядить", "Рулевой механизм EML снять и установить")
      res.products shouldBe List(
        "Болт (kombi)",
        "Болт с шестигр. гол. (комби) м10х35",
        "Болт м10х75",
        "Болт м10х76",
        "Болт м8х35х22"
      )
      res.clientType shouldBe 1
      res.year shouldBe 2015
    }

    "be correctly parsed from json when eventDate is empty" in {

      val str =
        """{"vin":"Z8T4C5FS9BM005269","id":"123","event_date":"","event_region":"Москва","event_city":"Москва",
          |"type":"ordinary_dealer","brand":"Фольксваген","model":"Jetta 6","mileage":58525,
          |"works":["Аккумуляторная батарея зарядить","Рулевой механизм EML снять и установить"],
          |"products":["Болт (kombi)","Болт с шестигр. гол. (комби) м10х35","Болт м10х75","Болт м10х76","Болт м8х35х22"],
          |"client_type":1,"year":2015,"recommendations":[]}""".stripMargin

      val res = Json.parse(str).as[AvilonServiceBookItemModel]

      res.eventDate shouldBe empty
    }
  }
}
