package ru.yandex.vertis.moisha.api.v1.service

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshal
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moisha.ProductPolicy
import ru.yandex.vertis.moisha.backend.marshalling.AutoruUsersMarshallingSupport
import ru.yandex.vertis.moisha.backend.Backend
import ru.yandex.vertis.moisha.backend.metering.MoishaDirectives
import ru.yandex.vertis.moisha.impl.autoru_users.model._
import ru.yandex.vertis.moisha.impl.autoru_users.AutoRuUsersPolicy
import ru.yandex.vertis.moisha.impl.autoru_users.view._
import ru.yandex.vertis.moisha.test.BaseSpec
import ru.yandex.vertis.moisha.impl.autoru_users.gens.test.TestData._

@RunWith(classOf[JUnitRunner])
class PriceRouteSpec extends BaseSpec with ScalatestRouteTest with MoishaDirectives {

  val policy = mock[AutoRuUsersPolicy]

  val marshallingSupport = new AutoruUsersMarshallingSupport()
  val backend = Backend(policy, marshallingSupport)
  val priceRoute = wrapRequest(new PriceRoute(backend).priceRoute)

  "PriceRoute" should {

    "return prolongPrice" in {
      val req = createReq(Products.Placement)
      (policy.estimate _).expects(*).returningT(createResponse(req, prolongPrice = Some(100)))
      implicit val marshaller: ToEntityMarshaller[ProductPolicy.Request] = marshallingSupport.requestMarshaller
      Post("/price", req) ~>
        priceRoute ~>
        check {
          val resp = Unmarshal(response).to[AutoRuUsersResponseView].futureValue

          resp.request.product shouldBe Products.Placement.toString
          resp.points.head.product.goods.head.price shouldBe 200

          resp.points.head.product.goods.head.prolongPrice shouldBe Some(100)
        }
    }

    "empty prolongPrice" in {
      val req = createReq(Products.Placement)
      (policy.estimate _).expects(*).returningT(createResponse(req, prolongPrice = None))
      implicit val marshaller: ToEntityMarshaller[ProductPolicy.Request] = marshallingSupport.requestMarshaller
      Post("/price", req) ~>
        priceRoute ~>
        check {
          val resp = Unmarshal(response).to[AutoRuUsersResponseView].futureValue
          resp.request.product shouldBe Products.Placement.toString
          resp.points.head.product.goods.head.price shouldBe 200

          resp.points.head.product.goods.head.prolongPrice shouldBe None

        }
    }
  }

}
