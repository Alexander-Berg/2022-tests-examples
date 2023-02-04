package vertis.statist.api.http

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.{Config, ConfigFactory}
import common.akka.http.OpsDirectives
import common.zio.ops.tracing.Tracing
import common.zio.ops.tracing.Tracing.Tracing
import common.zio.ops.tracing.testkit.TestTracing
import ru.yandex.vertis.generators.ProducerProvider
import vertis.zio.test.ZioSpecBase
import zio.{RIO, Runtime, ZEnv}

/** @author zvez
  */
trait BaseHandlerSpec extends ZioSpecBase with ScalatestRouteTest with ProducerProvider with RequestDirectives {

  val Root = "/"

  override def testConfig: Config = ConfigFactory.empty()

  def seal(route: Route): Route =
    Route.seal(route)(
      routingSettings = implicitly[RoutingSettings],
      exceptionHandler = DomainExceptionHandler.exceptionHandler,
      rejectionHandler = DomainExceptionHandler.rejectionHandler
    )

  def seal(handler: Handler): Route =
    runSync {
      Tracing.tracer.map { tracer =>
        val wrapper = OpsDirectives.makeWrapper(
          tracer,
          NoopRequestObserver,
          System.nanoTime
        )
        wrapper {
          seal(handler.route)
        }
      }
    }

  private def runSync[A](io: => RIO[ZEnv with Tracing, A]): A =
    Runtime.default.unsafeRunTask(
      io.provideLayer(ZEnv.live ++ TestTracing.noOp)
    )
}
