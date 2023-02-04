package ru.yandex.vertis.subscriptions.api.v1

import akka.testkit.{TestActorRef, TestProbe}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.subscriptions.api.RouteTestWithConfig
import spray.routing.RequestContext

/**
  * Specs on [[ru.yandex.vertis.subscriptions.api.v1.Handler]]
  * 1.x API version handler.
  */
@RunWith(classOf[JUnitRunner])
class HandlerSpec extends Matchers with WordSpecLike with RouteTestWithConfig {

  private val notifierProbe = TestProbe()
  private val subscriptionProbe = TestProbe()

  /** Route of handler under test */
  private val route = seal {
    TestActorRef(new Handler {
      protected def notifierHandler = notifierProbe.ref

      protected def subscriptionHandler = subscriptionProbe.ref
    }).underlyingActor.route
  }

  "GET/PUT/POST /notifier" should {
    "be proxied to notifier handler" in {
      Get("/notifier") ~> route ~> check {
        notifierProbe.expectMsgClass(classOf[RequestContext])
      }
      Post("/notifier") ~> route ~> check {
        notifierProbe.expectMsgClass(classOf[RequestContext])
      }
      Put("/notifier") ~> route ~> check {
        notifierProbe.expectMsgClass(classOf[RequestContext])
      }
    }
  }

  "GET /subscription/auto/foo" should {
    "be proxied to subscription handler" in {
      Get("/subscription") ~> route ~> check {
        subscriptionProbe.expectMsgClass(classOf[RequestContext])
      }
    }
  }
}
