package ru.yandex.vertis.subscriptions.api.v1

import akka.testkit.TestActorRef
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.subscriptions.api.{RequestContextProbe, RouteTestWithConfig}

/**
  * Specs [[ru.yandex.vertis.subscriptions.api.v1.Handler]].
  */
@RunWith(classOf[JUnitRunner])
class HandlerSpec extends Matchers with WordSpecLike with RouteTestWithConfig {

  private val probe = new RequestContextProbe(system)

  /** Route of handler under test */
  private val route = seal {
    TestActorRef(new Handler {
      protected def subscriptionsHandler = probe.ref
    }).underlyingActor.route
  }

  "/subscriptions" should {
    "route to subscriptions resource" in {
      Get("/subscriptions") ~> route ~> check {
        probe.expectHttp(req => true, req => req.complete("OK"))
        responseAs[String] should be("OK")
      }
    }
  }
}
