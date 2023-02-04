package ru.auto.api.services.counter

import akka.http.scaladsl.model.StatusCodes.NotFound
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.exceptions.OfferNotFoundException
import ru.auto.api.model.{OfferID, RequestParams}
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.{Request, RequestImpl}

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 21.03.17
  */
class OctopusCounterClientSpec extends HttpClientSpec with MockedHttpClient with ScalaCheckPropertyChecks {

  val emptyJson: String =
    """{
      |  "result": {
      |    "items": {},
      |    "total": 0
      |  },
      |  "time": 12.314
      |}""".stripMargin

  val emptyJsonWithArray: String =
    """{
      |  "result": {
      |    "items": [],
      |    "total": 0
      |  },
      |  "time": 12.314
      |}""".stripMargin

  val nonEmptyJson: String =
    """{
      |    "result": {
      |        "items": {
      |            "1041063767-2e51b": {
      |                "all": 5,
      |                "daily": 1,
      |                "phone_all": 20,
      |                "phone_daily": 2
      |            }
      |        },
      |        "total": 1
      |    },
      |    "time": 24.395
      |}""".stripMargin

  val missingValuesJson: String =
    """{
      |    "result": {
      |        "items": {
      |            "1041063767-2e51b": {}
      |        },
      |        "total": 1
      |    },
      |    "time": 24.395
      |}""".stripMargin

  val counterClient = new OctopusCounterClient(http)

  val id: OfferID = OfferID.parse("1041063767-2e51b")
  val id2: OfferID = OfferID.parse("2-2e51b")

  implicit private val request: Request = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r
  }

  "OctopusCounterClient" should {
    "return immediately on empty id list" in {
      counterClient.getCounters(Category.CARS, Seq.empty).await shouldBe empty
    }
    "parse counters from empty response" in {
      http.respondWithJson(emptyJson)
      counterClient.getCounters(Category.CARS, Seq(id)).await shouldBe empty
    }
    "parse counters from empty response with json array" in {
      http.respondWithJson(emptyJsonWithArray)
      counterClient.getCounters(Category.CARS, Seq(id)).await shouldBe empty
    }
    "parse counters from response" in {
      http.respondWithJson(nonEmptyJson)
      val result = counterClient.getCounters(Category.CARS, Seq(id)).await
      result should have size 1
      (result should contain).key(id)
      val counters = result(id)
      counters should have(
        Symbol("all")(5),
        Symbol("daily")(1),
        Symbol("phoneAll")(20),
        Symbol("phoneDaily")(2)
      )
    }
    "parse counters with missing values" in {
      http.respondWithJson(missingValuesJson)
      val result = counterClient.getCounters(Category.CARS, Seq(id)).await
      result should have size 1
      (result should contain).key(id)
      val counters = result(id)
      counters should have(
        Symbol("all")(0),
        Symbol("daily")(0),
        Symbol("phoneAll")(0),
        Symbol("phoneDaily")(0)
      )
    }
    "send offer ids with hash separated by comma in request" in {
      http.expectUrl(
        "/front/v1.0.0/sales/counters/list/?" +
          "access_key=d6ts4q53U244Fy-6n0-Kc3h5A3Z9bnwn&" +
          "remote_ip=1.1.1.1&" +
          "sale_id=1041063767-2e51b%2C2-2e51b&" +
          "category=cars"
      )
      http.respondWithJson(nonEmptyJson)
      counterClient.getCounters(Category.CARS, Seq(id, id2)).await
    }
    "get total counters" in {
      http.expectUrl(
        "/front/v1.0.0/sales/views/statistic" +
          "?access_key=d6ts4q53U244Fy-6n0-Kc3h5A3Z9bnwn&remote_ip=1.1.1.1" +
          "&sale_id=1043045004%2C1045596990&category=cars"
      )
      http.respondWithJsonFrom("/octopus/total_counters.json")
      val id1 = "1043045004-977b3"
      val id2 = "1045596990-a96e8"
      val ids = List(OfferID.parse(id1), OfferID.parse(id2))
      val List(counter1, counter2) = counterClient.getTotalCounters(Category.CARS, ids).futureValue.toList: @unchecked
      counter1.saleId shouldBe id1
      counter1.views shouldBe 182
      counter2.saleId shouldBe id2
      counter2.views shouldBe 8
    }
    "not get total counters for non-existing offer ids" in {
      http.expectUrl(
        "/front/v1.0.0/sales/views/statistic" +
          "?access_key=d6ts4q53U244Fy-6n0-Kc3h5A3Z9bnwn&remote_ip=1.1.1.1" +
          "&sale_id=1043045005%2C1045596991&category=cars"
      )
      http.respondWith(NotFound, "error")
      val id1 = "1043045005-977b3"
      val id2 = "1045596991-a96e8"
      val ids = List(OfferID.parse(id1), OfferID.parse(id2))
      val ex = counterClient.getTotalCounters(Category.CARS, ids).failed.futureValue
      ex shouldBe an[OfferNotFoundException]
    }
  }
}
