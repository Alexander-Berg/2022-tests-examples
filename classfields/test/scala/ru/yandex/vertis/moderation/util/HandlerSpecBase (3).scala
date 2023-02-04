package ru.yandex.vertis.moderation.util

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.common.monitoring.ping.SignalSwitchingDecider
import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.hobo.client.HoboClient
import ru.yandex.vertis.hobo.proto.Model.Task
import ru.yandex.vertis.kafka.util.ConsumerSettings
import ru.yandex.vertis.moderation.api.v1.service.scheduler.SchedulerService
import ru.yandex.vertis.moderation.api.{ApiHandlerImpl, CustomExceptionHandling}
import ru.yandex.vertis.moderation.cbb.IpBlockingBaseService
import ru.yandex.vertis.moderation.client.{VosAutoruClient, YtClient}
import ru.yandex.vertis.moderation.consumer.builder.request.impl.CompositeModerationRequestBuilder
import ru.yandex.vertis.moderation.dao._
import ru.yandex.vertis.moderation.dao.factory.ExternalUpdatesConsumerSettingsFactory
import ru.yandex.vertis.moderation.feature.FeatureService
import ru.yandex.vertis.moderation.geobase.GeobaseClient
import ru.yandex.vertis.moderation.handler.ModerationRequestHandler
import ru.yandex.vertis.moderation.hobo.HoboResolutionDecider
import ru.yandex.vertis.moderation.hobo.deciderset.HoboDeciderSet
import ru.yandex.vertis.moderation.httpclient.clustering.ClusteringClient
import ru.yandex.vertis.moderation.httpclient.telepony.TeleponyPhoneUnifierClient
import ru.yandex.vertis.moderation.httpclient.vin.VinDecoderClient
import ru.yandex.vertis.moderation.instance.{DbInstancePatcher, InstancePatchCalculator, InstancePatcher}
import ru.yandex.vertis.moderation.kafka.KafkaProducer
import ru.yandex.vertis.moderation.kafka.events.rules.EmptyRulesSupervisor
import ru.yandex.vertis.moderation.kafka.ownerjournal.OwnerJournalConsumerSettings
import ru.yandex.vertis.moderation.kafka.registry.AlwaysEnabledConsumerRegistry
import ru.yandex.vertis.moderation.kafka.request.ModerationRequestKey
import ru.yandex.vertis.moderation.kafka.updatejournal.{UpdateJournalConsumerSettings, UpdateJournalProducer}
import ru.yandex.vertis.moderation.meta.AutoruPhotoLicenseDecider
import ru.yandex.vertis.moderation.model.events.{Event, EventId}
import ru.yandex.vertis.moderation.model.instance.UpdateJournalRecord
import ru.yandex.vertis.moderation.model.{InstanceId, ModerationRequest}
import ru.yandex.vertis.moderation.mosru.MosRuDecider
import ru.yandex.vertis.moderation.operational.OperationalSupport
import ru.yandex.vertis.moderation.opinion.OpinionCalculator
import ru.yandex.vertis.moderation.owners.{DeferredOwnerOffersProcessor, OwnerOffersProcessor}
import ru.yandex.vertis.moderation.photos.ImagesMetadataFetcher
import ru.yandex.vertis.moderation.photos.bad.BadPhotosProvider
import ru.yandex.vertis.moderation.photos.multiaddress.MultiAddressPhotoChecker
import ru.yandex.vertis.moderation.picapica.{PicaRequestResolver, PicaService}
import ru.yandex.vertis.moderation.price.PriceMismatchDecider
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.proven.owner.external.ExternalProvenOwnerVerdictDecider
import ru.yandex.vertis.moderation.proven.owner.post.PostProvenOwnerDecider
import ru.yandex.vertis.moderation.rule.MatchingOptions
import ru.yandex.vertis.moderation.rule.service.Matcher
import ru.yandex.vertis.moderation.scheduler.task.contactindescription.PhonesFinder
import ru.yandex.vertis.moderation.scheduler.task.images.PhotoDecider
import ru.yandex.vertis.moderation.scheduler.task.region.RegionMismatchDecider
import ru.yandex.vertis.moderation.scheduler.task.rules.ModerationRuleDecider
import ru.yandex.vertis.moderation.scheduler.task.vin._
import ru.yandex.vertis.moderation.scheduler.{Environment, EnvironmentRegistry, SharedEnvironment}
import ru.yandex.vertis.moderation.searcher.core.saas.clauses.Clause
import ru.yandex.vertis.moderation.searcher.core.saas.client.EmptySearchCleaner
import ru.yandex.vertis.moderation.searcher.core.saas.document.{Document, DocumentBuilder}
import ru.yandex.vertis.moderation.searcher.core.saas.search.SearchClauseBuilder
import ru.yandex.vertis.moderation.service._
import ru.yandex.vertis.moderation.service.geo.RegionGeneralizerService
import ru.yandex.vertis.moderation.vacuum.VacuumClusterEventDecider
import ru.yandex.vertis.moderation.vin.VinFinder
import ru.yandex.vertis.moderation.{Globals, SpecBase}
import vertis.vsquality.vacuum.Event.ClusterChangeEvent

import scala.concurrent.Future

/**
  * @author semkagtn
  */
trait HandlerSpecBase extends SpecBase with ScalatestRouteTest {

  override def testConfig: Config = ConfigFactory.parseResources("application.test.conf")

  def basePath: String

  protected def url(remainPath: String): String = s"$basePath$remainPath"

  private def seal(route: Route): Route =
    Route.seal(route)(
      routingSettings = implicitly[RoutingSettings],
      exceptionHandler = CustomExceptionHandling.customExceptionHandler
    )

  private val schedulerServices: Map[Service, SchedulerService] = Service.values.map(_ -> mock[SchedulerService]).toMap

  private def environmentFor(service: Service): Environment =
    Environment(
      instanceDao = mock[InstanceDao[Future]],
      fullScanInstanceDao = mock[InstanceDao[Future]],
      searchInstanceDao = mock[SearchInstanceDao],
      cvHashDao = mock[CvHashDao],
      hoboClient = mock[HoboClient],
      hoboResponseBaseUrl = "test-base-url",
      ownerService = mock[OwnerService],
      geobaseClient = mock[GeobaseClient],
      ownerJournalConsumerSettings = mock[OwnerJournalConsumerSettings],
      ownerJournalMdbConsumerSettings = mock[OwnerJournalConsumerSettings],
      updateJournalConsumerSettings = mock[UpdateJournalConsumerSettings],
      updateJournalMdbConsumerSettings = mock[UpdateJournalConsumerSettings],
      updateJournalProducer = mock[UpdateJournalProducer],
      hoboDeciderSet = mock[HoboDeciderSet],
      regionMismatchDecider = mock[RegionMismatchDecider],
      teleponyClient = mock[TeleponyPhoneUnifierClient],
      regionGeneralizer = mock[RegionGeneralizerService],
      instanceArchiveDao = mock[InstanceArchiveDao],
      consumerRegistry = spy(new AlwaysEnabledConsumerRegistry),
      registry = mock[EnvironmentRegistry],
      ipBlockingService = mock[IpBlockingBaseService],
      priceMismatchDecider = mock[PriceMismatchDecider],
      photoMetadataDecider = mock[PhotoDecider],
      picaService = mock[PicaService],
      expirationDao = mock[ExpirationDao],
      badPhotosProvider = mock[BadPhotosProvider],
      moderationRequestConsumerSettings = mock[ConsumerSettings[ModerationRequestKey, ModerationRequest]],
      moderationRequestMdbConsumerSettings = mock[ConsumerSettings[ModerationRequestKey, ModerationRequest]],
      vinDecoderClient = mock[VinDecoderClient],
      vinDuplicateDecider = mock[VinDuplicateDecider],
      photoDuplicateDeciders = Seq.empty,
      multiAddressPhotoChecker = mock[MultiAddressPhotoChecker],
      moderationRuleDecider = mock[ModerationRuleDecider],
      moderationRuleService = mock[ModerationRuleService],
      pushDaemonSearchSubmitter = mock[SearchSubmitter],
      searchCleaner = EmptySearchCleaner,
      vinAutocodeDecider = mock[VinAutocodeDecider],
      vinResolutionDecider = mock[VinResolutionDecider],
      vinHistoryDecider = mock[VinHistoryDecider],
      ytClient = mock[YtClient],
      featureRegistry = mock[FeatureRegistry],
      featureService = mock[FeatureService],
      operationalSupport = mock[OperationalSupport],
      externalUpdatesConsumerSettingsFactory = mock[ExternalUpdatesConsumerSettingsFactory],
      env = Environments.Development,
      kafkaOffersProcessor = mock[OwnerOffersProcessor],
      deferredOffersProcessor = mock[DeferredOwnerOffersProcessor],
      ownerLookbackDao = mock[OwnerLookbackDao],
      picaRequestResolver = mock[PicaRequestResolver],
      moderationEventsConsumerSettings = mock[ConsumerSettings[EventId, Event]],
      ruleStatisticsService = mock[ModerationRuleStatisticsService],
      requestHandler = mock[ModerationRequestHandler],
      directRequestHandler = mock[ModerationRequestHandler],
      documentBuilder = mock[DocumentBuilder],
      searchClauseBuilder = mock[SearchClauseBuilder],
      docClauseMatcher = mock[Matcher[Document, Clause, MatchingOptions]],
      instancePatchCalculator = mock[InstancePatchCalculator],
      instancePatcher = mock[InstancePatcher],
      dbInstancePatcher = mock[DbInstancePatcher],
      updatesForServiceProducer = mock[KafkaProducer[InstanceId, UpdateJournalRecord]],
      schedulerService = () => schedulerServices(service),
      phonesFinder = PhonesFinder.Empty,
      rulesSupervisor = EmptyRulesSupervisor,
      autoruPhotoLicenseDecider = mock[AutoruPhotoLicenseDecider],
      hoboTasksConsumerSettings = mock[ConsumerSettings[InstanceId, Task]],
      hoboResolutionDecider = mock[HoboResolutionDecider],
      clusteringClient = mock[ClusteringClient],
      metadataService = mock[MetadataService],
      vosAutoruClient = mock[VosAutoruClient],
      dealerMetadataService = mock[DealerMetadataService],
      moderationRequestBuilder = mock[CompositeModerationRequestBuilder],
      submitFeatures = SubmitFeatures.Empty,
      mosRuDecider = mock[MosRuDecider],
      opinionCalculator = mock[OpinionCalculator],
      stopWordsDeciders = List.empty,
      imagesMetadataFetcher = mock[ImagesMetadataFetcher],
      brokerClient = mock[BrokerClient],
      externalProvenOwnerVerdictDecider = mock[ExternalProvenOwnerVerdictDecider],
      vinFinder = mock[VinFinder],
      vacuumEventsDecider = mock[VacuumClusterEventDecider],
      vacuumEventsConsumerConfig = mock[ConsumerSettings[String, ClusterChangeEvent]],
      phoneIndexService = mock[PhoneIndexService],
      postProvenOwnerDecider = mock[PostProvenOwnerDecider]
    )

  protected lazy val environmentRegistry: EnvironmentRegistry = {
    val registry = new EnvironmentRegistry
    Service.values.foreach { service =>
      registry.register(service, environmentFor(service))
    }
    registry
  }

  protected lazy val sharedEnvironment: SharedEnvironment =
    SharedEnvironment(
      schedulerService = mock[SchedulerService]
    )

  protected lazy val route: Route =
    seal(
      new ApiHandlerImpl(
        environmentRegistry,
        sharedEnvironment,
        Globals.AllServices.toSeq,
        new SignalSwitchingDecider
      ).route
    )
}
