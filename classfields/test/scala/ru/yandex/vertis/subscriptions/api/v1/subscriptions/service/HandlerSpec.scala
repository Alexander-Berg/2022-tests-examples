package ru.yandex.vertis.subscriptions.api.v1.subscriptions.service

import akka.testkit.TestActorRef
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.subscriptions.api.{RequestContextProbe, RouteTestWithConfig}

/**
  * Specs on [[ru.yandex.vertis.subscriptions.api.v1.subscriptions.service.Handler]]
  */
@RunWith(classOf[JUnitRunner])
class HandlerSpec extends Matchers with WordSpecLike with RouteTestWithConfig {

  private val subscriptionProbe = new RequestContextProbe(system)
  private val userProbe = new RequestContextProbe(system)
  private val tokenProbe = new RequestContextProbe(system)

  /** Route of handler under test */
  private val route = seal {
    TestActorRef(new Handler {
      protected def subscriptionHandler(service: String) = Some(subscriptionProbe.ref)

      protected def userHandler(service: String) = Some(userProbe.ref)

      protected def tokenHandler(service: String) = Some(tokenProbe.ref)
    }).underlyingActor.route
  }

  "/auto/subscription" should {
    "route auto subscription subresource" in {
      Get("/auto/subscription") ~> route ~> check {
        subscriptionProbe.expectHttp(req => true, req => req.complete("OK"))
        responseAs[String] should be("OK")
      }
    }
  }
  "/auto/user" should {
    "route to user subresource" in {
      Get("/auto/user") ~> route ~> check {
        userProbe.expectHttp(
          req => true,
          req => req.complete("OK")
        )
        responseAs[String] should be("OK")
      }
    }
  }

  "/auto/token/" should {
    "route to token api" in {
      Get("/auto/token/") ~> route ~> check {
        tokenProbe.expectHttp(
          req => true,
          req => req.complete("OK")
        )
        responseAs[String] should be("OK")
      }
    }
  }
}
