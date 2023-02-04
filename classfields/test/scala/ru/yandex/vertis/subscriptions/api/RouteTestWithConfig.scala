package ru.yandex.vertis.subscriptions.api

import org.scalatest.Suite
import spray.testkit.ScalatestRouteTest
import com.typesafe.config.ConfigFactory
import ru.yandex.vertis.spray.directives.RequestDirectives.injectApiRequest
import spray.routing.Route

/**
  * Includes special test config
  */
trait RouteTestWithConfig extends ScalatestRouteTest {
  this: Suite =>

  def seal(route: Route): Route = injectApiRequest {
    route
  }

  override def testConfig = ConfigFactory.parseResources("spray.conf")

  //spray is not fully compatible with akka 2.5.x
  override def cleanUp(): Unit = {
    system.terminate()
  }
}
