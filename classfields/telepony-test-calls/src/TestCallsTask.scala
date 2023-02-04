package ru.yandex.vertis.etc.telepony.telepony_test_calls

import akka.actor.ActorSystem
import akka.stream.Materializer
import common.yt.Yt
import common.yt.Yt.Yt
import common.zio.akka.Akka.Akka
import common.zio.app.Application.Application
import common.zio.ops.prometheus.Prometheus
import common.zio.ops.prometheus.Prometheus.Prometheus
import common.zio.vertis_scheduler.RunnableTask
import common.zio.ydb.Ydb.Ydb
import doobie.Transactor
import io.grpc.ManagedChannelBuilder
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader
import ru.yandex.vertis.broker.client.simple.{BrokerClient, BrokerClientConfig, BrokerClientImpl}
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.scheduler.model.{TaskDescriptor, Schedule => VertisSchedule}
import ru.yandex.vertis.telepony.ExecutionContextProvider
import ru.yandex.vertis.telepony.call.{BlockedCallsSink, CallsSink, NormalCallsSink}
import ru.yandex.vertis.telepony.component.AkkaComponent
import ru.yandex.vertis.telepony.component.impl.KafkaSupportComponentImpl
import ru.yandex.vertis.telepony.dao.HistoryRedirectDaoV2
import ru.yandex.vertis.telepony.dao.jdbc._
import ru.yandex.vertis.telepony.dao.ydb.YdbRecentCallsDao
import ru.yandex.vertis.telepony.factory.CuratorFactory
import ru.yandex.vertis.telepony.journal.impl.KafkaWriteJournalImpl
import ru.yandex.vertis.telepony.journal.kafka.KafkaJournals.TeleponyCallKafkaJournal
import ru.yandex.vertis.telepony.model.TypedDomains
import ru.yandex.vertis.telepony.properties.{DomainDynamicPropertiesReader, ZkDynamicProperties}
import ru.yandex.vertis.telepony.service.dust.{DustClient, DustClientImpl, LoggingDustClient, MeteredDustClient}
import ru.yandex.vertis.telepony.service.impl.s3.S3ClientImpl
import ru.yandex.vertis.telepony.service.impl._
import ru.yandex.vertis.telepony.service.logging.{LoggingCallbackCallService, LoggingCallsStatsService, LoggingRecordService, LoggingS3Client, LoggingTranscriptionTaskService, LoggingWhitelistHistoryService}
import ru.yandex.vertis.telepony.service.logging.v2.LoggingCallServiceV2
import ru.yandex.vertis.telepony.service.metered.{MeteredActualCallService, MeteredCallbackCallService, MeteredCallsStatsService, MeteredRecordService, MeteredS3Client, MeteredTranscriptionTaskService, MeteredWhitelistHistoryService}
import ru.yandex.vertis.telepony.service.{ActualCallService, RecordService, TranscriptionTaskService}
import ru.yandex.vertis.telepony.settings._
import ru.yandex.vertis.telepony.util.Threads
import ru.yandex.vertis.telepony.util.db.{DefaultDatabaseFactory, DualDatabase}
import ru.yandex.vertis.ydb.zio.YdbZioWrapper
import zio.clock.Clock
import zio.{Has, Task, ZIO, ZLayer}

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext

object TestCallsTask {

  type TestCallsEnv = Yt
    with Prometheus
    with Akka
    with Application
    with Has[Transactor[Task]]
    with Has[DualMysqlConfig]
    with Has[TestCallsConfig]
    with Clock
    with Ydb
    with Has[StatusSettings]
    with Has[S3Settings]
    with Has[RecordSettings]
    with Has[CuratorSettings]
    with Has[BrokerSettings]
    with Has[KafkaSettings]
    with Has[DustSettings]

  type TestCallsTask = Has[TaskWrapper]

  case class TaskWrapper(task: RunnableTask[Any])

  def createTask(yt: Yt.Service,
      ytTransactor: Transactor[Task],
      db: DualDatabase,
      redirectDao: HistoryRedirectDaoV2,
      config: TestCallsConfig,
      clock: Clock,
      callService: ActualCallService,
      sharedOperatorNumberDao: SharedOperatorNumberDao,
      transcriptionTaskService: TranscriptionTaskService,
      recordService: RecordService,
      callsSink: CallsSink,
      brokerClient: BrokerClient,
      dustClient: DustClient) = {
    val live = new CallsTransferService(
      yt,
      ytTransactor,
      db,
      redirectDao,
      config,
      clock,
      callService,
      sharedOperatorNumberDao,
      transcriptionTaskService,
      recordService,
      callsSink,
      brokerClient,
      dustClient
    )
    TaskWrapper(
      RunnableTask[Any](
        TaskDescriptor(
          "load-calls-from-prod-task",
          VertisSchedule.EveryHours(1),
        ),
        live.run
      )
    )
  }

  val layer: ZLayer[TestCallsEnv, Nothing, TestCallsTask] = make.toLayer

  def make: ZIO[TestCallsEnv, Nothing, TaskWrapper] =
    for {
      yt <- ZIO.service[Yt.Service]
      prometheus <- ZIO.service[Prometheus.Service]
      as <- ZIO.accessM[Akka](_.get.actorSystem)
      registry <- prometheus.registry
      transactor <- ZIO.service[Transactor[Task]]
      mySqlConfig <- ZIO.service[DualMysqlConfig]
      testCallsConfig <- ZIO.service[TestCallsConfig]
      clock <- ZIO.environment[Clock]
      ydb <- ZIO.service[YdbZioWrapper]
      s3Settings <- ZIO.service[S3Settings]
      recordSettings <- ZIO.service[RecordSettings]
      curatorSettings <- ZIO.service[CuratorSettings]
      brokerSettings <- ZIO.service[BrokerSettings]
      kafka <- ZIO.service[KafkaSettings]
      appInfo <- ZIO.accessM[Application](_.get.info)
      dustSettings <- ZIO.service[DustSettings]
    } yield {
      val execContext = Threads.lightWeightTasksEc
      // only for env building
      val domain = TypedDomains.autoru_def
      val db = DefaultDatabaseFactory.buildDualDatabase("shared", mySqlConfig)
      val redirectNamedDb = db.named("redirect")(registry)
      val historyRedirectDao = new JdbcHistoryRedirectDaoV2(domain)

      val recentCallsDao = new YdbRecentCallsDao(domain, ydb)
      val whitelistHistoryService = new WhitelistHistoryServiceImpl(
        whitelistHistoryDao = new JdbcWhitelistHistoryDao(db.named("whitelist")(registry))
      ) with MeteredWhitelistHistoryService with LoggingWhitelistHistoryService {
        override def prometheusRegistry: PrometheusRegistry = registry
      }
      val callStatsService = new CallsStatsServiceImpl(recentCallsDao)
        with LoggingCallsStatsService with MeteredCallsStatsService {
          override def prometheusRegistry: PrometheusRegistry = registry
        }
      val callService = new CallServiceV2Impl(
        domain,
        callsStatsService = callStatsService,
        whitelistHistoryService = whitelistHistoryService,
        db = db.named("call")(registry),
        callDao = new JdbcCallDaoV2(domain)
      ) with MeteredActualCallService with LoggingCallServiceV2 {
        override def prometheusRegistry: PrometheusRegistry = registry
      }

      val sharedOperatorNumberDao = new SharedOperatorNumberDao(db.named("shared_operator_number")(registry))

      val callbackCallService = new CallbackCallServiceImpl(
        new JdbcCallbackCallDao(db.named("callback")(registry))
      )(execContext) with LoggingCallbackCallService with MeteredCallbackCallService with ExecutionContextProvider {
        override def prometheusRegistry: PrometheusRegistry = registry
        override implicit val ec: ExecutionContext = execContext
      }
      val transcriptionTaskService = new TranscriptionTaskServiceImpl(
        dao = new JdbcTranscriptionTaskDao(db.named("transcription")(registry), domain),
        callbacksService = callbackCallService
      ) with LoggingTranscriptionTaskService with MeteredTranscriptionTaskService {
        override def prometheusRegistry: PrometheusRegistry = registry
      }

      val s3Client = new S3ClientImpl(
        bucket = s3Settings.bucket,
        region = s3Settings.region,
        uri = s3Settings.uri,
        accessId = s3Settings.accessId,
        secretId = s3Settings.secretId)(execContext) with LoggingS3Client with MeteredS3Client {
        override def prometheusRegistry: PrometheusRegistry = registry
      }
      val propertiesReader = new DomainDynamicPropertiesReader(
        domain,
        new ZkDynamicProperties(
          curator = CuratorFactory.newClient(curatorSettings),
          domains = TypedDomains.values.toList
        )
      )
      val recordService = new RecordServiceImpl(
        db = db.named("record")(registry),
        recordDao = new JdbcRecordDao(domain),
        s3Client = s3Client,
        properties = propertiesReader,
        proxyHostS3Url = recordSettings.proxyHostS3Url,
        recordKeyPrefix = recordSettings.proxyHostS3Url
      )(execContext) with LoggingRecordService with MeteredRecordService {
        override def prometheusRegistry: PrometheusRegistry = registry
      }

      val kafkaComponent = new KafkaSupportComponentImpl with AkkaComponent {
        override def kafkaSettings: KafkaSettings = kafka

        override def actorSystem: ActorSystem = as

        override def materializer: Materializer = Materializer(as)
      }

      val callJournal = new KafkaWriteJournalImpl(
        producer = kafkaComponent.producer,
        journal = TeleponyCallKafkaJournal
      )
      val brokerClient = BrokerClientImpl.make(
        BrokerClientConfig(clientName = "telepony", url = brokerSettings.url, allocationId = Some(appInfo.allocationId))
      )(registry)
      val callsSink = new CallsSink(
        blockedCallsSink = new BlockedCallsSink(brokerClient = brokerClient),
        normalCallsSink = new NormalCallsSink(callJournal, brokerClient)
      )

      val dustClient = new DustClientImpl(
        ManagedChannelBuilder
          .forTarget(dustSettings.url)
          .defaultLoadBalancingPolicy("round_robin")
          .enableFullStreamDecompression()
          .enableRetry()
          .maxRetryAttempts(3)
          .keepAliveTime(10, TimeUnit.SECONDS)
          .keepAliveTimeout(5, TimeUnit.SECONDS)
          .usePlaintext()
          .build
      ) with LoggingDustClient with MeteredDustClient {
        override def prometheusRegistry: PrometheusRegistry = registry
      }

      createTask(
        yt = yt,
        ytTransactor = transactor,
        db = redirectNamedDb,
        redirectDao = historyRedirectDao,
        config = testCallsConfig,
        clock = clock,
        callService = callService,
        sharedOperatorNumberDao = sharedOperatorNumberDao,
        transcriptionTaskService = transcriptionTaskService,
        recordService = recordService,
        callsSink = callsSink,
        brokerClient = brokerClient,
        dustClient = dustClient
      )
    }



  case class TestCallsConfig(ytCalls: String, ytTranscriptions: String, ytRedirectHistory: String, ytEntries: String)

  implicit val testCallsConfigReader: ConfigReader[TestCallsConfig] = deriveReader
}
