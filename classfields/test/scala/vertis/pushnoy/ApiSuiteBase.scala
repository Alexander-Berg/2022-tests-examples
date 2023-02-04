package vertis.pushnoy

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server.Directives.extractRequestContext
import akka.http.scaladsl.server.RejectionHandler
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest._
import vertis.pushnoy.api.utils.FeaturesHandler
import vertis.pushnoy.api.ApiDirectives.wrapRequest
import vertis.pushnoy.api.v1.V1Handler
import vertis.pushnoy.api.v1.subscriptions.SubscriptionsHandler
import vertis.pushnoy.api.v1.user.{TestUserManager, UserHandler}
import vertis.pushnoy.api.v2.V2Handler
import vertis.pushnoy.api.v2.device.DeviceManager
import vertis.pushnoy.api.v2.user.{UserManager, V2UserHandler}
import vertis.pushnoy.api.{v1, v2, ApiExceptionHandler, RootApiHandler, RootHandler, TestFeatureRegistry}
import vertis.pushnoy.services.xiva.{XivaManager, XivaManagerSupport}
import vertis.pushnoy.services.SubscriptionsManager
import vertis.pushnoy.api.v1.device.TestDeviceManager

trait ApiSuiteBase extends SuiteMixin with ScalatestRouteTest with TestEnvironment {
  suite: Suite =>

  def xivaManager: XivaManager
  def subscriptionsManager: SubscriptionsManager

  override def testConfig: Config = ConfigFactory.load("dao.development.conf")

  lazy val v1DeviceHandler = new v1.device.DeviceHandler(xivaManager)
  lazy val userHandler = new UserHandler(xivaManager)
  lazy val subscriptionsHandler = new SubscriptionsHandler(subscriptionsManager)
  lazy val v1Handler = new V1Handler(userHandler, v1DeviceHandler, subscriptionsHandler)

  lazy val deviceManager: DeviceManager = new TestDeviceManager
  lazy val v2DeviceHandler = new v2.device.V2DeviceHandler(deviceManager)
  lazy val userManager: UserManager = new TestUserManager
  lazy val v2UserHandler = new V2UserHandler(userManager)
  lazy val featureRegistry = new TestFeatureRegistry
  lazy val featuresHandler = new FeaturesHandler(featureRegistry)
  lazy val v2Handler = new V2Handler(v2DeviceHandler, v2UserHandler)

  lazy val apiRoot = new RootApiHandler(v1Handler, v2Handler)

  lazy val root = new RootHandler {

    override def route: Route = wrapRequest {
      extractRequestContext { rc =>
        Route.seal(apiRoot.route)(rc.settings, rc.parserSettings, RejectionHandler.default, ApiExceptionHandler.handler)
      }
    }
  }
}

trait ApiSpec extends PushnoySpecBase with ApiSuiteBase with TestClients with XivaManagerSupport {
  lazy val subscriptionsManager: SubscriptionsManager = new SubscriptionsManager(dao)
}
