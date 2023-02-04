package vertis.sraas.api

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import com.typesafe.config.{Config, ConfigFactory}
import common.akka.http.OpsDirectives
import common.akka.http.RequestObserver.CompositeObserver
import io.opentracing.noop.NoopTracerFactory
import ru.yandex.vertis.generators.ProducerProvider
import vertis.sraas.BaseSpec

import scala.concurrent.duration._

/** @author zvez
  */
trait BaseHandlerSpec extends BaseSpec with ScalatestRouteTest with ProducerProvider {

  val Root = "/"

  override def testConfig: Config = ConfigFactory.empty()

  def seal(route: Route): Route =
    Route.seal(route)(
      routingSettings = implicitly[RoutingSettings],
      exceptionHandler = DomainExceptionHandler.exceptionHandler,
      rejectionHandler = DomainExceptionHandler.rejectionHandler
    )

  def seal(handler: Handler): Route = {
    val wrapper = OpsDirectives.makeWrapper(
      NoopTracerFactory.create(),
      new CompositeObserver(),
      System.nanoTime
    )
    wrapper {
      seal(handler.route)
    }
  }

  implicit def default(implicit system: ActorSystem): RouteTestTimeout = RouteTestTimeout(5.seconds)
}
