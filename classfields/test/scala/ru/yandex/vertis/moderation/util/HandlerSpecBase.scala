package ru.yandex.vertis.moderation.util

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.codahale.metrics.MetricRegistry
import com.typesafe.config.{Config, ConfigFactory}
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{any => argAny}
import ru.yandex.common.monitoring.ping.SignalSwitchingDecider
import ru.yandex.vertis.caching.base.impl.stub.StubAsyncCache
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.hobo.proto.Model.Task
import ru.yandex.vertis.moderation.api._
import ru.yandex.vertis.moderation.backend.{Environment, EnvironmentRegistry, SharedComponents}
import ru.yandex.vertis.moderation.dao.impl.empty.{EmptyInstanceArchiveDao, EmptySearchInstanceDao}
import ru.yandex.vertis.moderation.dao.impl.inmemory.{InMemoryInstanceDao, InMemoryStorageImpl}
import ru.yandex.vertis.moderation.dao.{InstanceDao, ModerationRuleArchiveDao, ModerationRuleDao}
import ru.yandex.vertis.moderation.feature.{EmptyFeatureRegistry, FeatureService, FeatureServiceImpl}
import ru.yandex.vertis.moderation.handler.ModerationRequestHandler
import ru.yandex.vertis.moderation.hobo.HoboResolutionDecider
import ru.yandex.vertis.moderation.hobo.HoboResolutionDecider.Decision
import ru.yandex.vertis.moderation.instance.{
  EssentialsPatchCalculatorImpl,
  InstancePatchCalculatorImpl,
  InstancePatcherImpl
}
import ru.yandex.vertis.moderation.kafka.KafkaProducer
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{EssentialsPatch, ExternalId, Instance, UpdateJournalRecord}
import ru.yandex.vertis.moderation.model.meta.MetadataFetchRequest
import ru.yandex.vertis.moderation.model.signal.{Signal, SignalSet, SignalSwitchOff, Tombstone}
import ru.yandex.vertis.moderation.model.{InstanceId, ModerationRequest, SignalKey}
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.service.{
  ApiInstanceServiceImpl,
  MetadataService,
  ModerationRuleStatisticsService,
  StubPhoneRedirectSignalEnrichmentService
}
import ru.yandex.vertis.moderation.{Globals, RequestContext, SpecBase, StubPrometheusRegistry}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
  * @author semkagtn
  */
trait HandlerSpecBase extends SpecBase with ScalatestRouteTest with ApiMarshalling {

  override def testConfig: Config = ConfigFactory.parseResources("application.test.conf")

  def basePath: String

  protected def url(remainPath: String): String = s"$basePath$remainPath"

  private def seal(route: Route): Route =
    Route.seal(route)(
      routingSettings = implicitly[RoutingSettings],
      exceptionHandler = CustomExceptionHandling.customExceptionHandler
    )

  protected val environmentRegistry = new EnvironmentRegistry

  private def environmentFor(service: Service): Environment = {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
    implicit val featureRegistry: FeatureRegistry = EmptyFeatureRegistry
    implicit val featureService: FeatureService = new FeatureServiceImpl(featureRegistry)
    val inMemoryStorage = new InMemoryStorageImpl
    val instanceDao = spy(new InMemoryInstanceDao(service, inMemoryStorage))
    val searchInstanceDao = spy(new EmptySearchInstanceDao)
    val opinionCalculator = Globals.opinionCalculator(service)
    val instanceArchiveDao = spy(new EmptyInstanceArchiveDao)
    val updateJournalProducer = mock[KafkaProducer[InstanceId, UpdateJournalRecord]]
    val moderationRuleDao = mock[ModerationRuleDao]
    val moderationRuleArchiveDao = mock[ModerationRuleArchiveDao]
    val ruleStatisticsService = mock[ModerationRuleStatisticsService]
    val userUpdatesCache = spy(new StubAsyncCache[ExternalId, Instance])
    val patchCalculator = new InstancePatchCalculatorImpl(EssentialsPatchCalculatorImpl)
    val patcher = InstancePatcherImpl
    val requestHandler = mock[ModerationRequestHandler]
    val hoboResolutionDecider = mock[HoboResolutionDecider]
    val metadataService = mock[MetadataService]
    val phoneRedirectEnrichmentService = StubPhoneRedirectSignalEnrichmentService

    doReturn(Future.unit).when(requestHandler).handle(argAny[ModerationRequest])
    doReturn(Future.unit).when(updateJournalProducer).append(argAny[UpdateJournalRecord])
    doReturn(Future.successful(Some(MetadataGen.next)))
      .when(metadataService)
      .fetchMetadata(argAny[MetadataFetchRequest])(argAny[RequestContext])
    doReturn(Decision(ModerationRequestGen.next(2).toSeq, ModerationRequestGen.next(2).toSeq))
      .when(hoboResolutionDecider)
      .decide(argAny[Instance], argAny[Task], argAny[DateTime], argAny[Int])
    val apiService =
      spy(
        new ApiInstanceServiceImpl(
          requestHandler = requestHandler,
          instanceDao = instanceDao,
          searchInstanceDao = searchInstanceDao,
          instancePatchCalculator = patchCalculator,
          instancePatcher = patcher,
          userUpdatesCache = userUpdatesCache,
          featureRegistry = featureRegistry,
          instanceArchiveDao = instanceArchiveDao,
          opinionCalculator = opinionCalculator,
          updateJournalProducer = updateJournalProducer,
          metadataService = metadataService,
          phoneRedirectEnrichmentService = phoneRedirectEnrichmentService
        )
      )
    Environment(
      apiService = apiService,
      instanceDao = instanceDao,
      moderationRuleDao = moderationRuleDao,
      moderationRuleArchiveDao = moderationRuleArchiveDao,
      featureRegistry = featureRegistry,
      featureService = featureService,
      moderationRuleStatisticsService = ruleStatisticsService,
      requestHandler = requestHandler,
      hoboResolutionDecider = hoboResolutionDecider,
      registry = environmentRegistry
    )
  }

  Globals.AllServices.foreach { service =>
    environmentRegistry.register(service, environmentFor(service))
  }

  val sharedComponents: SharedComponents = mock[SharedComponents]
  sharedComponents.prometheusRegistry.returns(StubPrometheusRegistry)

  protected lazy val route: Route =
    seal(
      new ApiHandlerImpl(
        environmentRegistry,
        sharedComponents,
        new MetricRegistry,
        new SignalSwitchingDecider
      ).route
    )

  protected def updateInstanceDao(newInstance: Instance, instanceDao: InstanceDao[Future])(
      oldSignals: SignalSet = SignalSet.Empty
  ): Unit = {
    val signalMap = mutable.Map.empty[SignalKey, Signal]
    val signalTombstoneMap = mutable.Map.empty[SignalKey, Tombstone]
    val switchOffMap = mutable.Map.empty[SignalKey, SignalSwitchOff]
    val switchOffTombstoneMap = mutable.Map.empty[SignalKey, Tombstone]

    newInstance.signals.signalMap.foreach {
      case (k, Right(signal))   => signalMap += ((k, signal))
      case (k, Left(tombstone)) => signalTombstoneMap += ((k, tombstone))
    }
    newInstance.signals.switchOffMap.foreach {
      case (k, Right(switchOff)) => switchOffMap += ((k, switchOff))
      case (k, Left(tombstone))  => switchOffTombstoneMap += ((k, tombstone))
    }

    instanceDao.upsert(EssentialsPatch.fromInstance(newInstance)).futureValue
    instanceDao.updateContext(newInstance.id, newInstance.context).futureValue
    instanceDao
      .changeSignalsAndSwitchOffs(
        newInstance.id,
        newInstance.signals.signalMap,
        newInstance.signals.switchOffMap,
        oldSignals
      )
      .futureValue
  }
}
