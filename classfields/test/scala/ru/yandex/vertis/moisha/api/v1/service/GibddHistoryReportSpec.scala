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
import ru.yandex.vertis.moisha.impl.autoru.model.{AutoRuContext, AutoRuOffer, Categories, Products, Transports}
import ru.yandex.vertis.moisha.impl.autoru.view.AutoRuOfferView.DefaultPrice
import ru.yandex.vertis.moisha.impl.autoru.view.AutoRuResponseView
import ru.yandex.vertis.moisha.impl.autoru_users.TestPolicies
import ru.yandex.vertis.moisha.model.DateTimeInterval
import ru.yandex.vertis.moisha.model.FundsConversions.FundsLong
import ru.yandex.vertis.moisha.test.BaseSpec

@RunWith(classOf[JUnitRunner])
class GibddHistoryReportSpec extends BaseSpec with TestPolicies with ScalatestRouteTest with MoishaDirectives {

  private val marshallingSupport = AutoRuMarshallingSupport
  implicit private val requestMarshaller: ToEntityMarshaller[Request] = marshallingSupport.requestMarshaller
  private val priceRoute = new PriceRoute(Backend(dealersPolicy, marshallingSupport)).priceRoute

  private val startOfPolicy = DateTime.parse("2020-10-14T00:00+03:00")

  private val priceRequest = AutoRuRequest(
    Products.GibddHistoryReport,
    AutoRuOffer(DefaultPrice, startOfPolicy, Transports.Cars, Categories.Used, mark = None, model = None),
    AutoRuContext(clientRegionId = 1, clientCityId = None, offerPlacementDay = None, productTariff = None),
    DateTimeInterval.dayIntervalFrom(startOfPolicy),
    priceRequestId = None
  )

  //for withName directive support we will wrap request
  val route: Route = wrapRequest {
    priceRoute
  }

  "Gibdd history report price calculation" should {

    "return promocode-only price" in {
      Post("/price", priceRequest) ~> route ~> check {
        responseProductInfo.futureValue.goods.toList.loneElement.price shouldBe 79.rubles
      }
    }

    "return duration = 365 days" in {
      Post("/price", priceRequest) ~> route ~> check {
        responseProductInfo.futureValue.duration shouldBe 365
      }
    }
  }

  private def responseProductInfo = {
    withClue(responseAs[String]) {
      Unmarshal(response).to[AutoRuResponseView].map(_.points.toList.loneElement.product)
    }
  }
}
