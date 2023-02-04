package ru.yandex.vertis.chat.api

import akka.http.scaladsl.model.{HttpRequest, MediaType, MediaTypes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.vertis.chat.SpecBase
import ru.yandex.vertis.chat.api.util.routes.DomainExceptionHandler
import ru.yandex.vertis.chat.util.test.RequestContextAware
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.util.akka.http.protobuf.Protobuf
import ru.yandex.vertis.chat.api.util.directives.RequestDirectives.wrapRequest

import scala.language.implicitConversions

/**
  * Base class for HTTP handlers' tests
  *
  * @author zvez
  * @author dimas
  */
trait HandlerSpecBase extends SpecBase with ScalatestRouteTest with RequestContextAware with ProducerProvider {

  def seal(route: Route): Route = wrapRequest {
    Route.seal(route)(
      routingSettings = implicitly[RoutingSettings],
      exceptionHandler = DomainExceptionHandler.exceptionHandler,
      rejectionHandler = DomainExceptionHandler.rejectionHandler
    )
  }

  val root: String = "/"

  override def testConfig: Config = ConfigFactory.empty()

  implicit def requestToDomainRequest(request: HttpRequest): DomainHttpRequest =
    new DomainHttpRequest(request)

  def supportedMediaTypes: Iterable[MediaType] =
    Seq(Protobuf.mediaType, MediaTypes.`application/json`)
}
