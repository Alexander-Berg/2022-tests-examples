package ru.yandex.vertis.passport.api

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{Matchers, Suite}

/**
  * Base class for http handlers' tests
  *
  * @author zvez
  */
trait HandlerSpecBase extends Matchers with ScalatestRouteTest { this: Suite =>

  def seal(route: Route): Route = Route.seal(route)(
    routingSettings = implicitly[RoutingSettings],
    exceptionHandler = DomainExceptionHandler.exceptionHandler,
    rejectionHandler = DomainExceptionHandler.rejectionHandler
  )

  override def testConfig: Config = ConfigFactory.parseResources("application.test.conf")
}
