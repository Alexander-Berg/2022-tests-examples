package ru.yandex.vertis.moisha.api.v1.service

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshal
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moisha.ProductPolicy
import ru.yandex.vertis.moisha.backend.Backend
import ru.yandex.vertis.moisha.backend.marshalling.{
  AutoRuUsersSubscriptionsMarshallingSupport,
  AutoruUsersMarshallingSupport
}
import ru.yandex.vertis.moisha.backend.metering.MoishaDirectives
import ru.yandex.vertis.moisha.impl.autoru_users.TestPolicies
import ru.yandex.vertis.moisha.impl.autoru_users_subscriptions.AutoRuUsersSubscriptionsPolicy.AutoRuUsersSubscriptionsRequest
import ru.yandex.vertis.moisha.impl.autoru_users_subscriptions.model._
import ru.yandex.vertis.moisha.impl.autoru_users_subscriptions.view.{
  AutoRuUsersSubscriptionsRequestView,
  AutoRuUsersSubscriptionsResponseView
}
import ru.yandex.vertis.moisha.model.DateTimeInterval
import ru.yandex.vertis.moisha.test.BaseSpec

@RunWith(classOf[JUnitRunner])
class OffersHistoryReportsDurationSpec
  extends BaseSpec
  with TestPolicies
  with ScalatestRouteTest
  with MoishaDirectives {

  private val marshallingSupport = new AutoRuUsersSubscriptionsMarshallingSupport
  private val backend = Backend(usersSubscriptionsPolicy, marshallingSupport)

  //for withName directive support we will wrap request
  private val priceRoute = wrapRequest(new PriceRoute(backend).priceRoute)

  "Price calculation" should {

    "return offers-history-reports duration" in {
      implicit val marshaller: ToEntityMarshaller[ProductPolicy.Request] = marshallingSupport.requestMarshaller
      Post("/price", priceRequest(reportsCount = 50)) ~>
        priceRoute ~> check {
        Unmarshal(response)
          .to[AutoRuUsersSubscriptionsResponseView]
          .futureValue
          .points
          .toList
          .loneElement
          .product
          .duration shouldBe 32
      }
    }
  }

  private def priceRequest(reportsCount: Int) = {
    AutoRuUsersSubscriptionsRequest(
      Products.OffersHistoryReports,
      MaybeAutoRuUsersSubscriptionsOffer(None),
      AutoRuUsersOffersHistoryReportsContext(
        reportsCount,
        geoId = 1,
        contentQuality = 0,
        contextType = ContextTypes.VinHistory,
        experiment = None
      ),
      interval = DateTimeInterval.dayIntervalFrom(DateTime.parse("2020-09-15T00:00+03:00"))
    )
  }
}
