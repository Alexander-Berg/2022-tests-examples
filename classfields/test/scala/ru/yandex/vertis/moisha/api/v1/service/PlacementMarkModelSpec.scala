package ru.yandex.vertis.moisha.api.v1.service

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshal
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moisha.ProductPolicy.Request
import ru.yandex.vertis.moisha.backend.Backend
import ru.yandex.vertis.moisha.backend.marshalling.AutoRuMarshallingSupport
import ru.yandex.vertis.moisha.backend.metering.MoishaDirectives
import ru.yandex.vertis.moisha.impl.autoru.AutoRuPolicy.AutoRuRequest
import ru.yandex.vertis.moisha.impl.autoru.model._
import ru.yandex.vertis.moisha.impl.autoru.view.AutoRuOfferView.DefaultPrice
import ru.yandex.vertis.moisha.impl.autoru.view.AutoRuResponseView
import ru.yandex.vertis.moisha.impl.autoru_users.TestPolicies
import ru.yandex.vertis.moisha.model.DateTimeInterval
import ru.yandex.vertis.moisha.model.FundsConversions.FundsLong
import ru.yandex.vertis.moisha.test.BaseSpec

@RunWith(classOf[JUnitRunner])
class PlacementMarkModelSpec extends BaseSpec with TestPolicies with ScalatestRouteTest with MoishaDirectives {

  private val marshallingSupport = AutoRuMarshallingSupport
  implicit private val requestMarshaller: ToEntityMarshaller[Request] = marshallingSupport.requestMarshaller
  private val priceRoute = new PriceRoute(Backend(dealersPolicy, marshallingSupport)).priceRoute

  private val startOfPolicy = DateTime.parse("2022-07-07T00:00+03:00")

  val route: Route = wrapRequest {
    priceRoute
  }

  "price calculation for placement" should {

    "return price for model if it exists" in {
      Post("/price", request(mark = Some("AUDI"), model = Some("A3"))) ~> route ~> check {
        responseProductInfo.futureValue.goods.toList.loneElement.price shouldBe 200.rubles
      }
    }

    "return price for mark if model not exists" in {
      Post("/price", request(mark = Some("AUDI"), model = Some("A4"))) ~> route ~> check {
        responseProductInfo.futureValue.goods.toList.loneElement.price shouldBe 100.rubles
      }
    }

    "return price for mark if model not set" in {
      Post("/price", request(mark = Some("AUDI"), model = None)) ~> route ~> check {
        responseProductInfo.futureValue.goods.toList.loneElement.price shouldBe 100.rubles
      }
    }

    "return zero price from matrix rule" in {
      Post("/price", request(mark = Some("AUDI"), model = Some("A6"))) ~> route ~> check {
        responseProductInfo.futureValue.goods.toList.loneElement.price shouldBe 0.rubles
      }
    }
  }

  private def responseProductInfo = {
    withClue(responseAs[String]) {
      Unmarshal(response).to[AutoRuResponseView].map(_.points.toList.loneElement.product)
    }
  }

  private def request(mark: Option[MarkId], model: Option[ModelId]) =
    AutoRuRequest(
      Products.Placement,
      AutoRuOffer(DefaultPrice, startOfPolicy, Transports.Cars, Categories.Used, mark, model),
      AutoRuContext(clientRegionId = 1, clientCityId = None, offerPlacementDay = None, productTariff = None),
      DateTimeInterval.dayIntervalFrom(startOfPolicy),
      priceRequestId = None
    )
}
