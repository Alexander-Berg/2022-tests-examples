package ru.yandex.vertis.telepony.api.v2

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.Route
import org.scalatest.concurrent.Eventually
import ru.yandex.vertis.telepony.api.RequestDirectives.wrapRequest
import ru.yandex.vertis.telepony.api.v2.SectionsHandlerSpec.handlerSuite
import ru.yandex.vertis.telepony.api.{RouteTest, UnmatchedPathRoute}
import ru.yandex.vertis.telepony.model.{TypedDomain, TypedDomains}

/**
  * @author evans
  */
class SectionsHandlerSpec extends RouteTest with Eventually {

  "SectionsHandler" should {
    "success process for good domain" in {
      val (handler, probe, _) = handlerSuite
      val route = handler
      Get(s"/${TypedDomains.billing_realty}/redirect") ~> route ~> check {
        eventually { probe.unmatchedPath == Path./("redirect") }
        status shouldEqual StatusCodes.OK
      }
    }
    "fail for bad domain" in {
      val (handler, probe, _) = handlerSuite
      val route: Route = handler
      Get("/bad") ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
    "success for shared route" in {
      val (handler, _, probe) = handlerSuite
      val route = handler
      Post("/shared/pool") ~> route ~> check {
        eventually { probe.unmatchedPath == Path./("pool") }
        status shouldEqual StatusCodes.OK
      }
    }
  }
}

object SectionsHandlerSpec {

  def handlerSuite: (Route, UnmatchedPathRoute, UnmatchedPathRoute) = {
    val probe = new UnmatchedPathRoute
    val sharedProbe = new UnmatchedPathRoute
    val commonProbe = new UnmatchedPathRoute
    val handler = new SectionsHandler {
      override protected def serviceHandler(domain: TypedDomain): Option[Route] =
        if (domain == TypedDomains.billing_realty) {
          Some(probe.route)
        } else {
          None
        }

      override protected def sharedHandler: Route = sharedProbe.route

      override protected def commonHandler: Route = commonProbe.route
    }.route
    (wrapRequest(handler), probe, sharedProbe)
  }
}
