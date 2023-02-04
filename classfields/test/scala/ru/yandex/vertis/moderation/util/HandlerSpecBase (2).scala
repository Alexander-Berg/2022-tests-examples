package ru.yandex.vertis.moderation.util

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.codahale.metrics.MetricRegistry
import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.common.monitoring.ping.SignalSwitchingDecider
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.moderation.api.{ApiHandlerImpl, CustomExceptionHandling, PushApiMarshalling}
import ru.yandex.vertis.moderation.backend.{Environment, EnvironmentRegistry}
import ru.yandex.vertis.moderation.dao.impl.inmemory.{InMemoryInstanceDao, InMemoryStorageImpl}
import ru.yandex.vertis.moderation.feature.EmptyFeatureRegistry
import ru.yandex.vertis.moderation.handler.ModerationRequestHandler
import ru.yandex.vertis.moderation.model.ModerationRequest
import ru.yandex.vertis.moderation.model.instance.InstanceSource
import ru.yandex.vertis.moderation.opinion.DefaultOpinionCalculator
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.service.PushApiInstanceServiceImpl
import ru.yandex.vertis.moderation.service.catalog.PriceEnrichmentService
import ru.yandex.vertis.moderation.{Globals, SpecBase, StubPrometheusRegistry}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author semkagtn
  */
trait HandlerSpecBase extends SpecBase with ScalatestRouteTest with PushApiMarshalling {

  implicit private val featureRegistry: FeatureRegistry = EmptyFeatureRegistry

  override def testConfig: Config = ConfigFactory.parseResources("application.test.conf")

  def basePath: String

  protected def url(remainPath: String): String = s"$basePath$remainPath"

  private def seal(route: Route): Route =
    Route.seal(route)(
      routingSettings = implicitly[RoutingSettings],
      exceptionHandler = CustomExceptionHandling.customExceptionHandler
    )

  private def environmentFor(service: Service): Environment = {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
    val inMemoryStorage = new InMemoryStorageImpl
    val instanceDao = spy(new InMemoryInstanceDao(service, inMemoryStorage))
    val opinionCalculator = mock[DefaultOpinionCalculator]
    val requestHandler = mock[ModerationRequestHandler]
    val priceEnrichmentService: PriceEnrichmentService = (instance: InstanceSource) => Future.successful(instance)
    requestHandler.handle(any[ModerationRequest]).returns(Future.successful(()))
    val pushApiService =
      spy(
        new PushApiInstanceServiceImpl(
          service = service,
          instanceDao = instanceDao,
          requestHandler = requestHandler,
          opinionCalculator = opinionCalculator,
          priceEnrichmentService = priceEnrichmentService
        )
      )
    Environment(instanceDao, pushApiService)
  }

  protected lazy val environmentRegistry: EnvironmentRegistry = {
    val registry = new EnvironmentRegistry
    Globals.AllServices.foreach { service =>
      registry.register(service, environmentFor(service))
    }
    registry
  }

  protected lazy val route: Route =
    seal(
      new ApiHandlerImpl(
        environmentRegistry,
        new MetricRegistry(),
        StubPrometheusRegistry,
        new SignalSwitchingDecider
      ).route
    )
}
