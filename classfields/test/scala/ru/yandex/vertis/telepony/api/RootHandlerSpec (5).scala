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

  private val v1Probe = UnmatchedPathRoute()

  object AlwaysOkDecider extends Decider(Seq()) {
    override def decide(): Boolean = true
  }

  private val route = RequestDirectives.wrapRequest {
    RequestDirectives.seal(
      new RootHandler {
        override def apiV1Route: Route = v1Probe.route

        override def logged: Directive0 = Directive.Empty

        override def metered: Directive0 = Directive.Empty

        override def pingRoute: Route = new PingHandler(AlwaysOkDecider).route
      }.route
    )
  }

  "Handler" should {
    "ping" in {
      Get("/ping") ~> route ~> check {
        responseAs[String] shouldEqual "pong"
        status shouldEqual StatusCodes.OK
      }
    }
    "redirect to api/1.x" in {
      Get("/api/1.x/2") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
      eventually { v1Probe.unmatchedPath == Path./("2") }
    }
    "provide Swagger UI" in {
      Get("/docs/docs.yaml") ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }
  }
}
