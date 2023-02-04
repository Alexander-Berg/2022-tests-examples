package ru.yandex.vertis.telepony

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Suite}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.component._
import ru.yandex.vertis.telepony.component.impl._
import ru.yandex.vertis.telepony.dummy.{DummyBrokerClient, InMemoryS3Client}
import ru.yandex.vertis.telepony.journal.kafka.KafkaJournals._
import ru.yandex.vertis.telepony.journal.{ReadJournal, WriteJournal}
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.model.mts.{IvrMenu, VoiceMenu}
import ru.yandex.vertis.telepony.properties.{DomainDynamicPropertiesReader, DynamicProperties}
import ru.yandex.vertis.telepony.service.MtsClient.MenuValue
import ru.yandex.vertis.telepony.service._
import ru.yandex.vertis.telepony.service.beeline.InMemoryBeelineClient
import ru.yandex.vertis.telepony.service.impl.beeline.BeelineOperatorClient
import ru.yandex.vertis.telepony.service.impl.mts.MtsOperatorClient
import ru.yandex.vertis.telepony.service.impl.mtt.MttOperatorClient
import ru.yandex.vertis.telepony.service.impl.vox.{VoxClient, VoxDomainClient, VoxOperatorClient}
import ru.yandex.vertis.telepony.service.impl.{OperatorAvailableServiceImpl, PhoneStatisticsEagerLoader}
import ru.yandex.vertis.telepony.service.logging._
import ru.yandex.vertis.telepony.service.metered._
import ru.yandex.vertis.telepony.service.mts.{InMemoryMtsClient, RegisteringFailureMtsClient}
import ru.yandex.vertis.telepony.service.mtt.{InMemoryMttClient, RegisteringFailureMttClient}
import ru.yandex.vertis.telepony.service.vox.InMemoryVoxClient
import ru.yandex.vertis.telepony.settings._
import ru.yandex.vertis.telepony.util.Threads._
import ru.yandex.vertis.telepony.util._
import ru.yandex.vertis.telepony.util.yql.YqlClient
import ru.yandex.vertis.util.collection.RichMap
import ru.yandex.vertis.ydb.zio.{LoggedYdbZioWrapper, YdbZioWrapper}
import ru.yandex.vertis.ydb.{QueryOptions, YdbContainer, YdbQuerySyntaxVersion}
import ru.yandex.vertis.zio.logging.SyncLogging
import ru.yandex.vertis.zio.logging.SyncLogging.toLogConsumer
import ru.yandex.vertis.zio.util.ZioRunUtils

import java.util.Collections
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source

/**
  * @author evans
  */
trait IntegrationSpecTemplate
  extends TestPrometheusComponent
  with TestComponent
  with JdbcSpecTemplate
  with SharedDbSupport
  with DbComponent
  with SharedDbComponent
  with DatabaseSpec
  with TypedDomainComponent
  with MtsComponent
  with MttComponent
  with VoxComponent
  with BeelineComponent
  with DaoComponentImpl
  with RegionTreeComponentImpl
  with PhoneAnalyzerComponentImpl
  with RegionGeneralizerComponentImpl
  with AkkaComponent
  with HydraPipelineComponentImpl
  with HydraClientComponentImpl
  with CallsStatsServiceComponentImpl
  with ApplicationComponentImpl
  with VertisSchedulerClientComponentImpl
  with BaseDomainComponent
  with HttpClientComponentImpl
  with DomainKafkaComponent
  with ComponentComponent
  with SharedDaoComponentImpl
  with SharedPoolComponentImpl
  with CuratorComponentImpl
  with DomainDynamicPropertiesComponentImpl
  with DynamicPropertiesComponentImpl
  with HoboClientComponentImpl
  with MockitoSupport
  with SpeechKitClientComponentImpl
  with BrokerComponent
  with S3Component
  with YdbComponent
  with YqlClientComponent
  with CallbackComponentImpl
  with SyncLogging
  with BeforeAndAfterAll {
  this: Suite =>

  private lazy val config = ConfigFactory.parseResources("service.conf").resolve()

  override def curatorSettings: CuratorSettings = CuratorSettings(config.getConfig("zookeeper"))

  override def resourceAvailable(): Boolean = true

  override def sharedDatabaseAvailable(): Boolean = true

  override def domain: Domain = "billing_realty"

  lazy val actorSystem = ActorSystem(component.name, config)

  lazy val materializer = ActorMaterializer()(actorSystem)

  override lazy val domainDynamicPropertiesReader: DomainDynamicPropertiesReader = {
    val mockProperties = mock[DomainDynamicPropertiesReader]
    when(mockProperties.getValue(DynamicProperties.AsyncUpdatingRedirectProperty)).thenReturn(Set())
    when(mockProperties.getValue(DynamicProperties.LastUsedRedirectModeProperty))
      .thenReturn(LastUsedRedirectModes.Always)
    when(mockProperties.getValue(DynamicProperties.EnabledGeoFallbackRulesProperty)).thenReturn(false)
    mockProperties
  }

  lazy val settings: ServiceSettings = {
    val domainConfigs = config.getConfig("telepony.domain")
    val serviceConfig = domainConfigs.getConfig("billing_realty")
    val defaultConfig = domainConfigs.getConfig("default")
    ServiceSettings(serviceConfig.withFallback(defaultConfig))
  }

  lazy val hydraClientSettings: HydraClientSettings = HydraClientSettings(config.getConfig("telepony.hydra"))

  override lazy val mttAvailableService: OperatorAvailableService =
    new OperatorAvailableServiceImpl(hydraClient, failureLimit = 0.1, operator = Operators.Mtt)

  private class NoLimitLimiter extends Limiter {
    override def available(): Boolean = true

    override def acquire(): Unit = {}

    override def release(): Unit = {}
  }

  override val mttLimiter: Limiter = new NoLimitLimiter

  override val domainBeelineLimiter: Limiter = new NoLimitLimiter

  override lazy val beelineAvailableService: OperatorAvailableService =
    new OperatorAvailableServiceImpl(hydraClient, failureLimit = 0.1, operator = Operators.Beeline)

  override lazy val beelineDomainAccountComponents: Map[OperatorAccount, DomainBeelineAccountComponent] =
    Map(OperatorAccounts.BeelineShared -> testingBeelineComponent)

  override def statusSettings: StatusSettings = settings.statusSettings

  override def hoboSettings: HoboSettings = HoboSettings(config.getConfig("telepony.hobo"))

  override def mtsDomainSettings: MtsDomainSettings = settings.mtsDomainSettings

  override def recordSettings: RecordSettings = settings.recordSettings

  override def isAntiFraudEnabled: Boolean = true

  override val defaultMtsAccount: OperatorAccount = OperatorAccounts.MtsShared

  override def hydraRequestTimeout: FiniteDuration = 10.seconds

  override def proxySettings: ProxySettings = ProxySettings(config.getConfig("telepony.http.proxy"))

  override lazy val speechKitClientSettings: SpeechKitClientSettings = settings.speechKitClientSettings

  override lazy val geoFallbackRulesSettings: GeoFallbackRulesSettings = settings.geoFallbackRulesSettings

  def vertisSchedulerClientSettings: VertisSchedulerClientSettings = settings.vertisSchedulerClientSettings

  val ydbPort = 2135
  val tablePrefix = "/local"

  val ydbContainer = {
    val c = YdbContainer.stable
    c.container.withLogConsumer(toLogConsumer(syncLogger))
    c.container.addEnv("YDB_USE_IN_MEMORY_PDISKS", "true")
    c.start()
    c
  }

  override lazy val ydb: YdbZioWrapper = {

    new LoggedYdbZioWrapper(
      YdbZioWrapper.make(
        ydbContainer.tableClient,
        tablePrefix,
        30.seconds,
        QueryOptions.Default.copy(syntaxVersion = YdbQuerySyntaxVersion.V1)
      )
    )
  }

  override lazy val yqlClient: YqlClient = ???

  override lazy val callbackSettings: Map[TypedDomain, CallbackSettings] =
    TypedDomains.values.toSeq.map((_, settings.callbackSettings)).toMap

  override lazy val domainCallbackSettings: CallbackSettings = settings.callbackSettings

  override lazy val callbackVoxClient: VoxClient = voxDomainAccountComponents(OperatorAccounts.VoxShared).client

  override lazy val mtsAvailableService: OperatorAvailableService =
    new OperatorAvailableServiceImpl(hydraClient, failureLimit = 0.1, operator = Operators.Mts)

  def defaultMtsClient: InMemoryMtsClient = inMemoryMtsClient

  val otherMtsAccount: OperatorAccount = OperatorAccounts.all.head

  override lazy val mtsDomainAccountComponents: Map[OperatorAccount, DomainMtsAccountComponent] = {
    val cmp = DomainMtsAccountComponent(inMemoryMtsClient, mtsOperatorClient)
    Map(defaultMtsAccount -> cmp, otherMtsAccount -> cmp)
  }

  override lazy val mtsClients: Map[OperatorAccount, InMemoryMtsClient] =
    mtsDomainAccountComponents.mapValuesStrict(_.client.asInstanceOf[InMemoryMtsClient])

  override val domainMtsLimiter: Limiter = new NoLimitLimiter

  private val menuXml =
    Source
      .fromInputStream(
        getClass.getClassLoader.getResourceAsStream("mts/empty.xml")
      )
      .mkString

  private val voiceMenu = VoiceMenu(MenuValue(menuXml), Seq.empty)

  override val defaultMttAccount: OperatorAccount = OperatorAccounts.MttShared

  override lazy val mttClients: Map[OperatorAccount, InMemoryMttClient] =
    mttDomainAccountComponents.mapValuesStrict(_.client.asInstanceOf[InMemoryMttClient])

  override lazy val mttDomainAccountComponents: Map[OperatorAccount, DomainMttAccountComponent] =
    Map(defaultMttAccount -> testingMttComponent)

  override lazy val voxDomainAccountComponents: Map[OperatorAccount, DomainVoxAccountComponent] =
    Map(OperatorAccounts.VoxShared -> testingVoxComponent)

  override lazy val domainVoxLimiter: Limiter = new NoLimitLimiter

  override lazy val s3Client: S3Client = new InMemoryS3Client with LoggingS3Client

  override lazy val s3CloudClient: S3Client = new InMemoryS3Client with LoggingS3Client

  def clear(): Unit =
    Defs.logTime("Clearing all data") {
      appCallDao.clear().futureValue
      recordDao.clear().databaseValue.futureValue
      callDaoV2.clear().databaseValue.futureValue
      bannedCallDaoV2.clear().databaseValue.futureValue
      redirectDaoV2.clear().databaseValue.futureValue
      operatorNumberDaoV2.clear().databaseValue.futureValue
      unmatchedRawCallDao.clear().futureValue
      mtsClients.values.foreach(_.clear())
      mttClients.values.foreach(_.clear())
      reasonBlacklistDao.clear().futureValue
      aonBlacklistDao.clear().futureValue
      complaintDao.clear().futureValue
    }

  val inMemoryMtsClient =
    new InMemoryMtsClient
      with MeteredMtsClient
      with LoggingMtsClient
      with PrometheusProvider
      with RegisteringFailureMtsClient {

      override def mtsAvailableService: OperatorAvailableService =
        IntegrationSpecTemplate.this.mtsAvailableService
    }

  val mtsOperatorClient: OperatorClient =
    new MtsOperatorClient(
      mtsClient = inMemoryMtsClient,
      crm = None,
      voiceMenu = voiceMenu,
      ivrMenu = IvrMenu(Nil),
      makeCallTime = 29.seconds,
      maxQueueTime = 30.seconds,
      channels = 1,
      masterOpt = mtsDomainSettings.masterPhone
    )

  override def readJournal[T](kafkaJournal: KafkaJournal2[T], consumer: ConsumerGroup): ReadJournal[T] = ???

  override def writeJournal[T](kafkaJournal: KafkaJournal2[T]): WriteJournal[T] = ???

  override def writeJournal[T](journal: KafkaJournal[T]): WriteJournal[T] = ???

  override lazy val operatorLabelService: OperatorLabelService = {
    val m = mock[OperatorLabelService]
    clearOperatorLabelServiceInvocations(m)
    m
  }

  def clearOperatorLabelServiceInvocations(ols: OperatorLabelService): Unit = {
    when(ols.getUnhealthy).thenReturn(Set.empty[Operator])
    when(ols.getSuspended).thenReturn(Set.empty[Operator])
  }

  private def dummyWriteJournal[T](kafkaJournal: KafkaJournal2[T], buffer: mutable.Buffer[T]): WriteJournal[T] =
    new WriteJournal[T] {

      override def send(event: T) = {
        buffer += event
        Future.successful(null)
      }
      override protected def journal = kafkaJournal
    }

  import scala.jdk.CollectionConverters._

  lazy val touchRedirectRequests: mutable.Buffer[TouchRedirectRequest] =
    Collections.synchronizedList(new java.util.ArrayList[TouchRedirectRequest]).asScala

  lazy val redirectUpdateActions: mutable.Buffer[RedirectUpdateAction] =
    Collections.synchronizedList(new java.util.ArrayList[RedirectUpdateAction]).asScala

  override lazy val touchRedirectWriter: WriteJournal[TouchRedirectRequest] =
    dummyWriteJournal(TouchRedirectKafkaJournal, touchRedirectRequests)

  override lazy val redirectUpdatesWriter: WriteJournal[RedirectUpdateAction] =
    dummyWriteJournal(RedirectUpdatesKafkaJournal, redirectUpdateActions)

  override lazy val poolStatisticsLoader = new PhoneStatisticsEagerLoader(operatorNumberServiceV2)

  lazy val testingMttComponent = {
    val mttClient: InMemoryMttClient =
      new InMemoryMttClient
        with MeteredMttClient
        with LoggingMttClient
        with PrometheusProvider
        with RegisteringFailureMttClient {

        override def mttAvailableService: OperatorAvailableService =
          IntegrationSpecTemplate.this.mttAvailableService
      }
    val mttOperatorClient: OperatorClient = new MttOperatorClient(mttClient)
    DomainMttAccountComponent(mttClient, mttOperatorClient)
  }

  private lazy val testingVoxComponent = {
    val client: VoxClient =
      new InMemoryVoxClient with MeteredVoxClient with LoggingVoxClient with PrometheusProvider
    val domainClient: VoxDomainClient =
      new VoxDomainClient(
        voxClient = client,
        ruleName = settings.voxDomainSettings.ruleName
      )
    val operatorClient: OperatorClient = new VoxOperatorClient(domainClient)
    DomainVoxAccountComponent(client, domainClient, operatorClient)
  }

  lazy val testingBeelineComponent = {
    val beelineClient: BeelineClient = new InMemoryBeelineClient
    val beelineOperatorClient: OperatorClient =
      new BeelineOperatorClient(beelineClient, settings.beelineDomainSettings.callSettings)
    DomainBeelineAccountComponent(beelineClient, beelineOperatorClient)
  }

  override lazy val brokerClient = new DummyBrokerClient

  override def beforeAll(): Unit = {
    ZioRunUtils.runSync(zio.Runtime.default)(recentCallsDao.init()).get
  }
}
