package ru.yandex.vertis.moisha.api.v1.service

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshal
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moisha.ProductPolicy.Request
import ru.yandex.vertis.moisha.api.view.DateTimeIntervalView
import ru.yandex.vertis.moisha.backend.Backend
import ru.yandex.vertis.moisha.backend.marshalling.AutoRuMarshallingSupport
import ru.yandex.vertis.moisha.backend.metering.MoishaDirectives
import ru.yandex.vertis.moisha.impl.autoru.AutoRuPolicy.AutoRuRequest
import ru.yandex.vertis.moisha.impl.autoru.model._
import ru.yandex.vertis.moisha.impl.autoru.view.AutoRuOfferView.DefaultPrice
import ru.yandex.vertis.moisha.impl.autoru.view._
import ru.yandex.vertis.moisha.impl.autoru_users.TestPolicies
import ru.yandex.vertis.moisha.model.DateTimeInterval
import ru.yandex.vertis.moisha.model.FundsConversions.FundsLong
import ru.yandex.vertis.moisha.test.BaseSpec

import java.util.UUID

@RunWith(classOf[JUnitRunner])
class DealerProductWithTariffSpec extends BaseSpec with TestPolicies with ScalatestRouteTest with MoishaDirectives {

  private val marshallingSupport = AutoRuMarshallingSupport
  implicit private val requestMarshaller: ToEntityMarshaller[Request] = marshallingSupport.requestMarshaller
  private val priceRoute = new PriceRoute(Backend(dealersTariffPolicy, marshallingSupport)).priceRoute

  private val startOfPolicy = DateTime.parse("2020-10-14T00:00+03:00")

  //for withName directive support we will wrap request
  val route: Route = wrapRequest {
    priceRoute
  }

  "Dealer product with tariff price calculation" should {

    "return proper price for tariff application-credit:single:tariff:cars:new" in {
      Post("/price", priceRequest(Some("application-credit:single:tariff:cars:new"), priceRequestId = None)) ~> route ~> check {
        responseProductInfo.futureValue.goods.toList.loneElement.price shouldBe 15.rubles
      }
    }

    "return proper price for tariff application-credit:single:tariff:cars:used" in {
      Post("/price", priceRequest(Some("application-credit:single:tariff:cars:used"), priceRequestId = None)) ~> route ~> check {
        responseProductInfo.futureValue.goods.toList.loneElement.price shouldBe 30.rubles
      }
    }

    "return proper price without tariff" in {
      Post("/price", priceRequest(productTariff = None, priceRequestId = None)) ~> route ~> check {
        responseProductInfo.futureValue.goods.toList.loneElement.price shouldBe 50.rubles
      }
    }

    "return default price for unknown tariff" in {
      Post("/price", priceRequest(productTariff = Some("tariff"), priceRequestId = None)) ~> route ~> check {
        responseProductInfo.futureValue.goods.toList.loneElement.price shouldBe 50.rubles
      }
    }

    "return price for date in past when allowDefaults = true" in {
      val request =
        priceRequestView(DateTime.parse("2020-12-20T00:00+03:00"), allowDefaults = true, priceRequestId = None)
      Post("/price", request) ~> route ~> check {
        withClue(response) {
          status shouldBe OK
        }
      }
    }

    "return same priceRequestId as was sent in original request" in {
      val priceRequestId = Some(UUID.randomUUID().toString)
      val request = priceRequestView(DateTime.parse("2020-12-20T00:00+03:00"), allowDefaults = true, priceRequestId)
      Post("/price", request) ~> route ~> check {
        Unmarshal(response).to[AutoRuResponseView].futureValue.request.priceRequestId shouldBe priceRequestId
      }
    }
  }

  private def responseProductInfo = {
    withClue(responseAs[String]) {
      Unmarshal(response).to[AutoRuResponseView].map(_.points.toList.loneElement.product)
    }
  }

  private def priceRequest(productTariff: Option[ProductTariff], priceRequestId: Option[String]) = AutoRuRequest(
    Products.ApplicationCreditSingle,
    AutoRuOffer(DefaultPrice, startOfPolicy, Transports.Cars, Categories.Used, mark = None, model = None),
    AutoRuContext(
      clientRegionId = 1,
      clientCityId = None,
      offerPlacementDay = None,
      productTariff = productTariff
    ),
    DateTimeInterval.dayIntervalFrom(startOfPolicy),
    priceRequestId
  )

  private def priceRequestView(date: DateTime, allowDefaults: Boolean, priceRequestId: Option[String]) =
    AutoRuRequestView(
      AutoRuOfferView(
        price = None,
        creationTs = None,
        transport = None,
        category = None,
        mark = None,
        model = None,
        allowDefaults = Some(allowDefaults)
      ),
      AutoRuContextView(
        clientRegionId = 1,
        clientCityId = None,
        offerPlacementDay = None,
        productTariff = Some("application-credit:single:tariff:cars:new")
      ),
      "application-credit:single",
      DateTimeIntervalView(
        DateTimeInterval.dayIntervalFrom(date)
      ),
      priceRequestId
    )
}
