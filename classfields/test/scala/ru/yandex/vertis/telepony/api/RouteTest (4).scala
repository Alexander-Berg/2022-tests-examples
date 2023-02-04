package ru.yandex.vertis.telepony.api

import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration._
import akka.testkit._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

/**
  * @author evans
  */
trait RouteTest extends Matchers with AnyWordSpecLike with ScalatestRouteTest with Directives {

  override def testConfig: Config = ConfigFactory.parseResources("spray.conf")

  implicit def default: RouteTestTimeout =
    RouteTestTimeout(300.millis.dilated(system))

  def seal(route: Route): Route =
    RequestDirectives.wrapRequest {
      RequestDirectives.seal(route)
    }

  // should only be used for swagger testing because it can be very slow
  trait TestSwaggerRoute {

    implicit def default: RouteTestTimeout =
      RouteTestTimeout(3.seconds.dilated(system))

  }

}
