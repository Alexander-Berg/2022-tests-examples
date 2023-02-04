package ru.yandex.vertis.feedprocessor.autoru.scheduler.services.salesman

import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.app.TestApplication
import ru.yandex.vertis.feedprocessor.autoru.model.SaleCategories
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.salesman.SalesmanClient.{Goods, Products}
import ru.yandex.vertis.feedprocessor.util.DummyOpsSupport
import ru.yandex.vertis.mockito.MockitoSupport

class SalesmanDataParseTest
  extends WordSpecBase
  with MockitoSupport
  with ScalaFutures
  with TestApplication
  with DummyOpsSupport {

  "Salesman data from json" should {

    "lower case -> truck" in {

      val json =
        """
          |{
          |  "offer": 100500,
          |  "category": "truck",
          |  "product": "all_sale_activate"
          |}
        """.stripMargin

      Json.parse(json).as[Goods].category shouldEqual SaleCategories.Truck
    }

    "lower case -> trucks" in {

      val json =
        """
          |{
          |  "offer": 100500,
          |  "category": "trucks",
          |  "product": "all_sale_activate"
          |}
        """.stripMargin

      Json.parse(json).as[Goods].category shouldEqual SaleCategories.Truck
    }

    "upper case -> TRUCK" in {

      val json =
        """
          |{
          |  "offer": 100500,
          |  "category": "TRUCK",
          |  "product": "all_sale_activate"
          |}
        """.stripMargin

      Json.parse(json).as[Goods].category shouldEqual SaleCategories.Truck
    }

    "upper case -> TRUCKS" in {

      val json =
        """
          |{
          |  "offer": 100500,
          |  "category": "TRUCK",
          |  "product": "all_sale_activate"
          |}
        """.stripMargin

      Json.parse(json).as[Goods].category shouldEqual SaleCategories.Truck
    }
  }

  "Salesman data to json" should {

    "Goods" in {

      val goods = Goods(100500, SaleCategories.Truck, Products.Placement, None)

      Json.toJson(goods).toString() shouldEqual """{"offer":100500,"category":"trucks","product":"all_sale_activate"}"""
    }
  }

}
