package ru.yandex.vertis.subscriptions.api

import akka.testkit.TestActorRef
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import spray.http.StatusCodes
import spray.http.Uri.Path
import spray.routing.HttpService

/** Specs [[ru.yandex.vertis.subscriptions.api.Handler]]
  */
@RunWith(classOf[JUnitRunner])
class HandlerSpec extends Matchers with WordSpecLike with RouteTestWithConfig with HttpService {

  private val probe = new RequestContextProbe(system)

  /** Route of handler under test */
  private val route = seal {
    TestActorRef(new Handler {
      def v1handler = probe.ref

      def v2handler = probe.ref

      def v3handler = probe.ref
    }).underlyingActor.route
  }

  "/api/1.x" should {
    "route to api version 1.x" in {
      Get("/api/1.x") ~> route ~> check {
        probe.expectHttp(req => !req.unmatchedPath.startsWith(Path("/api/1.x")), req => req.complete("OK"))
        responseAs[String] should be("OK")
      }
    }
  }
  "/api/1.x/" should {
    "route to api version 1.x" in {
      Get("/api/1.x/") ~> route ~> check {
        probe.expectHttp(req => !req.unmatchedPath.startsWith(Path("/api/1.x")), req => req.complete("OK"))
        responseAs[String] should be("OK")
      }
    }
  }
  "/api/2.x/" should {
    "route to api version 2.x" in {
      Get("/api/2.x/") ~> route ~> check {
        probe.expectHttp(req => !req.unmatchedPath.startsWith(Path("/api/2.x")), req => req.complete("OK"))
        responseAs[String] should be("OK")
      }
    }
  }
  "/api/unknown" should {
    "not route to unknown resource" in {
      Get("/api/unknown/") ~> sealRoute(route) ~> check {
        status should be(StatusCodes.NotFound)
      }
    }
  }

  implicit def actorRefFactory = system
}
