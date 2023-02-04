package ru.yandex.realty.http

import akka.http.scaladsl.model.{MediaType, MediaTypes}
import akka.http.scaladsl.server._
import akka.http.scaladsl.settings.RoutingSettings
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.realty.SpecBase
import ru.yandex.realty.akka.http.ProtobufSupport
import ru.yandex.realty.akka.http.directives.RequestDirectives
import ru.yandex.realty.akka.http.handlers.Handler
import ru.yandex.realty.auth.Application
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.protobuf.BasicProtoFormats
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.util.akka.http.protobuf.Protobuf

import scala.concurrent.duration._
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.testkit.TestDuration

/**
  * Base class for HTTP handlers' tests
  *
  * @author dimas
  */
trait HandlerSpecBase
  extends SpecBase
  with Handler
  with ScalatestRouteTest
  with RequestAware
  with ProtobufSupport
  with BasicProtoFormats
  with ProducerProvider {

  implicit val timeout = RouteTestTimeout(10.seconds.dilated)

  def supportedMediaTypes: Iterable[MediaType] =
    Seq(Protobuf.mediaType, MediaTypes.`application/json`)

  def routeUnderTest: Route

  override def route: Route = prepareForTests(routeUnderTest)

  protected def prepareForTests(route: Route): Route = pushRequestForTests {
    Route.seal(route)(
      routingSettings = implicitly[RoutingSettings],
      exceptionHandler = exceptionHandler,
      rejectionHandler = rejectionHandler
    )
  }

  protected def exceptionHandler: ExceptionHandler

  protected def rejectionHandler: RejectionHandler

  override def testConfig: Config = ConfigFactory.empty()

  private def pushRequestForTests: Directive0 = {
    Directive { inner => ctx =>
      val updatedRequest = RequestDirectives
        .transformRequest(ctx.request) { request =>
          request.setIp("127.0.0.1")
          request.setTrace(Traced.empty)
          if (request.applicationOpt.isEmpty)
            request.setApplication(Application.UnitTests)
        }
      val newCtx = ctx.mapRequest(_ => updatedRequest)
      inner.apply(Unit)(newCtx)
    }
  }

}
