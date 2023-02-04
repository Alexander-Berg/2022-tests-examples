package ru.yandex.vertis.telepony.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.{Directive, Directive0, Route}
import org.scalatest.concurrent.Eventually
import ru.yandex.common.monitoring.ping.Decider

/**
  * @author evans
  */
class RootHandlerSpec extends RouteTest with Eventually {
  private val v2Probe = UnmatchedPathRoute()

  object AlwaysOkDecider extends Decider(Seq()) {
    override def decide(): Boolean = true
  }

  private val route = RequestDirectives.wrapRequest {
    RequestDirectives.seal {
      new RootHandler {
        override def apiV2Route: Route = v2Probe.route

        override def logged: Directive0 = Directive.Empty

        override def metered: Directive0 = Directive.Empty

        override def traced: Directive0 = Directive.Empty

        override def timeLimited: Directive0 = Directive.Empty

        override def pingRoute: Route = new PingHandler(AlwaysOkDecider).route
      }.route
    }
  }

  "RootHandler" should {
    "ping" in {
      Get("/ping") ~> route ~> check {
        responseAs[String] shouldEqual "pong"
        status shouldEqual StatusCodes.OK
      }
    }
    "redirect to api/2.x" in {
      Get("/api/2.x/2") ~> route ~> check {
        eventually { v2Probe.unmatchedPath == Path./("2") }
        status shouldEqual StatusCodes.OK
      }
    }
    "provide Swagger UI" in new TestSwaggerRoute {
      Get("/swagger/") ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }
  }
}
