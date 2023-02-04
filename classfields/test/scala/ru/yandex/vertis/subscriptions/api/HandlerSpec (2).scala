package ru.yandex.vertis.subscriptions.api

import akka.testkit.{TestActorRef, TestProbe}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import spray.http.StatusCodes
import spray.routing.RequestContext

/**
  * Specs on [[ru.yandex.vertis.subscriptions.api.Handler]] - root API handler.
  */
@RunWith(classOf[JUnitRunner])
class HandlerSpec extends Matchers with WordSpecLike with RouteTestWithConfig {

  private val monitoringProbe = TestProbe()
  private val v1Probe = TestProbe()

  /** Route of handler under test */
  private val route = seal {
    TestActorRef(new Handler {
      protected val monitoringHandler = monitoringProbe.ref

      protected val v1Handler = v1Probe.ref
    }).underlyingActor.route
  }

  "GET /ping" should {
    "respond with 200 OK" in {
      Get("/ping") ~> route ~> check {
        response.status should be(StatusCodes.OK)
        responseAs[String] should be("0;OK")
      }

    }
  }

  "GET /api/1.x" should {
    "send request to api 1.x handler" in {
      Get("/api/1.x") ~> route ~> check {
        v1Probe.expectMsgClass(classOf[RequestContext])
      }
    }
  }
}
