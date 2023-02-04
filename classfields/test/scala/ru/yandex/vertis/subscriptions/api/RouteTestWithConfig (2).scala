package ru.yandex.vertis.subscriptions.api

import com.typesafe.config.ConfigFactory
import org.scalatest.Suite
import ru.yandex.vertis.spray.directives.RequestDirectives._
import spray.routing._
import spray.testkit.ScalatestRouteTest

/**
  * Includes special test config
  */
trait RouteTestWithConfig extends ScalatestRouteTest {
  this: Suite =>

  def seal(route: Route): Route = injectApiRequest {
    route
  }

  override def testConfig = ConfigFactory.parseResources("spray.conf")
}
