package ru.yandex.vertis.subscriptions.api.v1.notifier

import akka.testkit.{TestActorRef, TestProbe}
import com.googlecode.protobuf.format.JsonFormat
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.subscriptions.MatcherInternalApi.MatchedSubscriptions
import ru.yandex.vertis.subscriptions.api.RouteTestWithConfig
import ru.yandex.vertis.subscriptions.util.Logging
import ru.yandex.vertis.subscriptions.util.test.TestData
import spray.http.{HttpEntity, StatusCodes}
import spray.routing.HttpServiceBase

/** Specs on [[ru.yandex.vertis.subscriptions.api.v1.notifier.Handler]]
  * 1.x API notification receiver handler
  */
@RunWith(classOf[JUnitRunner])
class HandlerSpec extends Matchers with WordSpecLike with RouteTestWithConfig with HttpServiceBase with Logging {

  val autoAcceptorProbe = TestProbe()
  val realtyAcceptorProbe = TestProbe()

  val registeredServices = Map(
    "auto" -> autoAcceptorProbe.ref,
    "realty" -> realtyAcceptorProbe.ref
  )

  /** Route of handler under test */
  val route = seal {
    TestActorRef(new Handler {
      protected def matchesAcceptorActor(service: String) = registeredServices.get(service)
    }).underlyingActor.route
  }

  "PUT /not-exists-service" should {
    "repsond with 404 NotFound" in {
      val service = "not-exists-service"
      Put(s"/$service") ~> route ~> check {
        response.status should be(StatusCodes.NotFound)
        responseAs[String] should be(s"Unknown service $service")
      }
    }
  }

  "PUT /auto without entity" should {
    "respond with 400 BadRequest" in {
      Put("/auto") ~> sealRoute(route) ~> check {
        response.status should be(StatusCodes.BadRequest)
        responseAs[String] should be(s"Request entity expected but not supplied")
      }
    }
  }

  "PUT /auto with malformed entity" should {
    "respond with 400 BadRequest" in {
      val entity = HttpEntity(Handler.NotificationProtobuf, "foo")
      Put("/auto", entity) ~> sealRoute(route) ~> check {
        response.status should be(StatusCodes.BadRequest)
        responseAs[String] should startWith(s"The request content was malformed")
      }
    }
  }

  "PUT /auto correct entity" should {
    val subscription = TestData.subscription("foo")
    val notification = MatchedSubscriptions
      .newBuilder()
      .setMatches(
        MatchedSubscriptions.Matches
          .newBuilder()
          .addSubscriptionIds(subscription.getId)
          .setDocument(TestData.document("bar"))
      )
      .build()

    "respond 202 Accepted on protobuf request" in {
      val entity = HttpEntity(Handler.NotificationProtobuf, notification.toByteArray)
      Put("/auto", entity) ~> sealRoute(route) ~> check {
        response.status should be(StatusCodes.Accepted)
        autoAcceptorProbe.expectMsgClass(classOf[MatchedSubscriptions])
      }
    }

    "respond 202 Accepted on JSON request" in {
      val entity = HttpEntity(Handler.NotificationJson, JsonFormat.printToString(notification))
      Put("/auto", entity) ~> sealRoute(route) ~> check {
        response.status should be(StatusCodes.Accepted)
        autoAcceptorProbe.expectMsgClass(classOf[MatchedSubscriptions])
      }
    }
  }
}
