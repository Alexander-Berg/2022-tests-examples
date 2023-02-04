package ru.auto.salesman.client.moisha

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import ru.auto.api.ApiOfferModel.Section
import ru.auto.salesman.environment.{today, IsoDateTimeFormatter}
import ru.auto.salesman.model.{OfferCategories, ProductId, RegionId}
import ru.auto.salesman.service.DealerFeatureService
import ru.auto.salesman.service.PriceEstimateService.PriceRequest
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.TestAkkaComponents._
import spray.json.{JsArray, JsBoolean, JsNumber, JsObject, JsString}

import java.util.UUID
import scala.concurrent.Future

class HttpMoishaClientSpec extends BaseSpec {

  private val testUrl = "testUrl"
  private val dealerFeatureService = mock[DealerFeatureService]

  "HTTP Moisha client" should {

    "work correctly" in {
      val marks = List("Audi", "Mercedes")
      val region = RegionId(213)
      val total = 100500
      val priceRequestId = UUID.randomUUID().toString
      val client = new HttpMoishaClient(testUrl, dealerFeatureService) {
        override def post(
            uri: Uri,
            headers: scala.Seq[HttpHeader],
            entity: RequestEntity,
            disableSsl: Boolean = false
        )(implicit system: ActorSystem): Future[HttpResponse] = {
          uri.path.toString shouldBe "testUrl/api/1.x/service/autoru_auction/price"
          headers shouldBe empty
          Unmarshal(entity).to[JsObject].futureValue shouldBe JsObject(
            "offer" -> JsObject(
              "category" -> JsString("cars"),
              "section" -> JsString("new")
            ),
            "context" -> JsObject(
              "clientRegionId" -> JsNumber(region),
              "marks" -> JsArray(JsString("Audi"), JsString("Mercedes")),
              "hasPriorityPlacement" -> JsBoolean(false)
            ),
            "product" -> JsString("call"),
            "interval" -> JsObject(
              "from" -> JsString(IsoDateTimeFormatter.print(today().from)),
              "to" -> JsString(IsoDateTimeFormatter.print(today().to))
            ),
            "priceRequestId" -> JsString(priceRequestId)
          )
          val result = JsObject(
            "points" -> JsArray(
              JsObject(
                "product" -> JsObject(
                  "product" -> JsString("call"),
                  "total" -> JsNumber(total)
                )
              )
            ),
            "request" -> JsObject(
              "priceRequestId" -> JsString(priceRequestId)
            )
          ).prettyPrint
          Future.successful(
            HttpResponse(entity = HttpEntity(`application/json`, result))
          )
        }
      }

      val request = PriceRequest(
        PriceRequest
          .PaidCallClientOffer(
            OfferCategories.Cars,
            Section.NEW,
            mark = None,
            model = None
          ),
        PriceRequest
          .PaidCallClientContext(region, marks, hasPriorityPlacement = false),
        ProductId.Call,
        today(),
        Some(priceRequestId)
      )

      val response = client.estimate(request).futureValue

      val List(point) = response.points
      point.product.product shouldBe "call"
      point.product.total shouldBe total
    }
  }
}
