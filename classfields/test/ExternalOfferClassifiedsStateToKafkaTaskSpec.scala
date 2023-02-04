package auto.dealers.multiposting.scheduler.test

import auto.dealers.multiposting.clients.s3.S3MultipostingClient
import auto.dealers.multiposting.model.EventType
import auto.dealers.multiposting.scheduler.config.ClassifiedStateToKafkaConfig
import auto.dealers.multiposting.scheduler.task.ExternalOfferClassifiedsStateToKafkaTask
import auto.dealers.multiposting.scheduler.task.ExternalOfferClassifiedsStateToKafkaTask.Env
import auto.dealers.multiposting.storage.ExternalOfferEventFileDao
import auto.dealers.multiposting.storage.ExternalOfferEventFileDao.ExternalOfferEventFileDao
import auto.dealers.multiposting.storage.postgresql.PgExternalOfferEventFileDao
import com.google.protobuf.timestamp.Timestamp
import common.ops.prometheus.CollectorRegistryWrapper
import common.zio.clients.s3.S3Client
import common.zio.clients.s3.testkit.InMemoryS3
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import common.zio.kafka.ProducerConfig
import common.zio.kafka.testkit.TestKafka
import common.zio.kafka.testkit.TestKafka.TestKafka
import common.zio.ziokafka.scalapb.ScalapbSerde
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.prometheus.client.CollectorRegistry
import ru.auto.api.api_offer_model.Multiposting.Classified
import ru.auto.api.api_offer_model.Multiposting.Classified.ClassifiedName.AVITO
import ru.auto.api.api_offer_model.OfferStatus.{ACTIVE, REMOVED}
import ru.auto.multiposting.event_model.ClassifiedUpdateEvent
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._
import zio.interop.catz._
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

object ExternalOfferClassifiedsStateToKafkaTaskSpec extends DefaultRunnableSpec {
  private val classifiedUpdateEventSerde = ScalapbSerde[ClassifiedUpdateEvent]

  private val classifiedsStateEvent = EventType.ClassifiedsState
  private val carsNewClassifiedsStateEvent = EventType.CarsNewClassifiedsState
  private val carsUsedClassifiedsStateEvent = EventType.CarsUsedClassifiedsState
  private val prefix: String = "offers"
  private val prefixCarsNew: String = "cars_new_offers"
  private val prefixCarsUsed: String = "cars_used_offers"

  val jsonWithActiveStatus =
    """{"timestamp":1614846804,"autoru_client_id":40551,"source":"avito","vin":"X7LASRA1966966091","offer_id":"2053515973","url":"https:\/\/www.avito.ru\/mytischi\/avtomobili\/renault_kaptur_2021_2053515973","finish_time":"2021-04-01T08:16:30+03:00","status":"active"}"""

  val jsonWithRemovedStatus =
    """{"timestamp":1614846804,"autoru_client_id":40551,"source":"avito","vin":"X7LASRA1966966091","offer_id":"2053515973","url":"https:\/\/www.avito.ru\/mytischi\/avtomobili\/renault_kaptur_2021_2053515973","finish_time":"2021-04-01T08:16:30+03:00","status":"removed"}"""

  val expectedActive = ClassifiedUpdateEvent(
    40551,
    "X7LASRA1966966091",
    Some(
      Classified(
        name = AVITO,
        enabled = false,
        status = ACTIVE,
        detailedStatus = "",
        createDate = 0,
        expireDate = 1617254190000L,
        services = Vector(),
        servicePrices = Vector(),
        startDate = 0,
        url = "https://www.avito.ru/mytischi/avtomobili/renault_kaptur_2021_2053515973",
        id = "2053515973",
        daysOnSale = 0,
        warnings = Vector(),
        errors = Vector()
      )
    ),
    Some(Timestamp(1614846804, 0))
  )

  val expectedRemoved = expectedActive.copy(classified = expectedActive.classified.map(_.copy(status = REMOVED)))

  val task = new ExternalOfferClassifiedsStateToKafkaTask(
    new CollectorRegistryWrapper(CollectorRegistry.defaultRegistry)
  )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {

    suite("ExternalOfferClassifiedsStateToKafkaTask")(
      skipProcessingWhenS3IsEmpty(classifiedsStateEvent, prefix).provideLayer(
        createTestSpecificEnv("topic-1", "bucket-1", None, Seq.empty, classifiedsStateEvent)
      ),
      skipProcessingIfLastProcessedFileAndNewFileAreEqual(classifiedsStateEvent, prefix).provideLayer(
        createTestSpecificEnv(
          "topic-2",
          "bucket-2",
          Some(s"$prefix-1"),
          Seq(S3Content(filename = s"$prefix-1", content = "")),
          classifiedsStateEvent
        )
      ),
      processNewestFile(classifiedsStateEvent, prefix).provideLayer(
        createTestSpecificEnv(
          "topic-3",
          "bucket-3",
          None,
          Seq(S3Content(filename = s"$prefix-1", content = jsonWithActiveStatus)),
          classifiedsStateEvent
        )
      ),
      processNewFileWithoutDiff(classifiedsStateEvent, prefix).provideLayer(
        createTestSpecificEnv(
          s"topic-4",
          s"bucket-4",
          Some(s"$prefix-1"),
          Seq(
            S3Content(filename = s"$prefix-1", content = jsonWithActiveStatus),
            S3Content(filename = s"$prefix-2", content = jsonWithActiveStatus)
          ),
          classifiedsStateEvent
        )
      ),
      processNewFileWithDiff(classifiedsStateEvent, prefix).provideLayer(
        createTestSpecificEnv(
          s"topic-5",
          s"bucket-5",
          Some(s"$prefix-1"),
          Seq(
            S3Content(filename = s"$prefix-1", content = jsonWithActiveStatus),
            S3Content(filename = s"$prefix-2", content = jsonWithRemovedStatus)
          ),
          classifiedsStateEvent
        )
      ),
      skipProcessingWhenS3IsEmpty(carsNewClassifiedsStateEvent, prefixCarsNew).provideLayer(
        createTestSpecificEnv("topic-6", "bucket-6", None, Seq.empty, carsNewClassifiedsStateEvent)
      ),
      skipProcessingIfLastProcessedFileAndNewFileAreEqual(carsNewClassifiedsStateEvent, prefixCarsNew).provideLayer(
        createTestSpecificEnv(
          "topic-7",
          "bucket-7",
          Some(s"$prefixCarsNew-1"),
          Seq(S3Content(filename = s"$prefixCarsNew-1", content = "")),
          carsNewClassifiedsStateEvent
        )
      ),
      processNewestFile(carsNewClassifiedsStateEvent, prefixCarsNew).provideLayer(
        createTestSpecificEnv(
          "topic-8",
          "bucket-8",
          None,
          Seq(S3Content(filename = s"$prefixCarsNew-1", content = jsonWithActiveStatus)),
          carsNewClassifiedsStateEvent
        )
      ),
      processNewFileWithoutDiff(carsNewClassifiedsStateEvent, prefixCarsNew).provideLayer(
        createTestSpecificEnv(
          s"topic-9",
          s"bucket-9",
          Some(s"$prefixCarsNew-1"),
          Seq(
            S3Content(filename = s"$prefixCarsNew-1", content = jsonWithActiveStatus),
            S3Content(filename = s"$prefixCarsNew-2", content = jsonWithActiveStatus)
          ),
          carsNewClassifiedsStateEvent
        )
      ),
      processNewFileWithDiff(carsNewClassifiedsStateEvent, prefixCarsNew).provideLayer(
        createTestSpecificEnv(
          s"topic-10",
          s"bucket-10",
          Some(s"$prefixCarsNew-1"),
          Seq(
            S3Content(filename = s"$prefixCarsNew-1", content = jsonWithActiveStatus),
            S3Content(filename = s"$prefixCarsNew-2", content = jsonWithRemovedStatus)
          ),
          carsNewClassifiedsStateEvent
        )
      ),
      skipProcessingWhenS3IsEmpty(carsUsedClassifiedsStateEvent, prefixCarsUsed).provideLayer(
        createTestSpecificEnv("topic-11", "bucket-11", None, Seq.empty, carsUsedClassifiedsStateEvent)
      ),
      skipProcessingIfLastProcessedFileAndNewFileAreEqual(carsUsedClassifiedsStateEvent, prefixCarsUsed).provideLayer(
        createTestSpecificEnv(
          "topic-12",
          "bucket-12",
          Some(s"$prefixCarsUsed-1"),
          Seq(S3Content(filename = s"$prefixCarsUsed-1", content = "")),
          carsUsedClassifiedsStateEvent
        )
      ),
      processNewestFile(carsUsedClassifiedsStateEvent, prefixCarsUsed).provideLayer(
        createTestSpecificEnv(
          "topic-13",
          "bucket-13",
          None,
          Seq(S3Content(filename = s"$prefixCarsUsed-1", content = jsonWithActiveStatus)),
          carsUsedClassifiedsStateEvent
        )
      ),
      processNewFileWithoutDiff(carsUsedClassifiedsStateEvent, prefixCarsUsed).provideLayer(
        createTestSpecificEnv(
          s"topic-14",
          s"bucket-14",
          Some(s"$prefixCarsUsed-1"),
          Seq(
            S3Content(filename = s"$prefixCarsUsed-1", content = jsonWithActiveStatus),
            S3Content(filename = s"$prefixCarsUsed-2", content = jsonWithActiveStatus)
          ),
          carsUsedClassifiedsStateEvent
        )
      ),
      processNewFileWithDiff(carsUsedClassifiedsStateEvent, prefixCarsUsed).provideLayer(
        createTestSpecificEnv(
          s"topic-15",
          s"bucket-15",
          Some(s"$prefixCarsUsed-1"),
          Seq(
            S3Content(filename = s"$prefixCarsUsed-1", content = jsonWithActiveStatus),
            S3Content(filename = s"$prefixCarsUsed-2", content = jsonWithRemovedStatus)
          ),
          carsUsedClassifiedsStateEvent
        )
      )
    ) @@
      sequential @@
      after(
        ZIO
          .service[Transactor[Task]]
          .flatMap(xa => sql"delete from external_offer_event_files".update.run.transact(xa))
      )
  }.provideSomeLayerShared[Blocking] {
    val kafka = TestKafka.live
    val kafkaProducerConfig = kafka >>> ZLayer.fromServiceM(_ =>
      TestKafka.bootstrapServers.map(servers => ProducerConfig(servers, 30.seconds, Map.empty))
    )

    val transactor = Blocking.live >>> TestPostgresql.managedTransactor

    val initSchema = transactor >>> (
      for {
        tx <- ZIO.service[Transactor[Task]]
        _ <- InitSchema("/schema.sql", tx)
      } yield ()
    ).orDie.toLayer

    kafka ++ kafkaProducerConfig ++ transactor ++ initSchema
  }

  val skipProcessingWhenS3IsEmpty: (EventType, String) => ZSpec[Env, Throwable] =
    (eventType: EventType, filePrefix: String) =>
      testM(s"""skip processing if s3 is empty '$filePrefix'""") {
        for {
          _ <- task.program
          lastFile <- ExternalOfferEventFileDao.getLastProcessedFilename(eventType)
        } yield assert(lastFile)(isNone)
      }

  val skipProcessingIfLastProcessedFileAndNewFileAreEqual: (EventType, String) => ZSpec[Env, Throwable] =
    (eventType: EventType, filePrefix: String) =>
      testM(s"""skip processing if last processed file is equal to the last file in the bucket '$filePrefix'""") {
        for {
          _ <- task.program
          lastFile <- ExternalOfferEventFileDao.getLastProcessedFilename(eventType)
        } yield assert(lastFile)(isSome(equalTo(s"$filePrefix-1")))
      }

  val processNewestFile: (EventType, String) => ZSpec[Any with Clock with Blocking with Has[Consumer] with ExternalOfferEventFileDao with Env, Throwable] =
    (eventType: EventType, filePrefix: String) =>
      testM(s"""process newest file '$filePrefix'""") {
        for {
          cfg <- ZIO.service[ClassifiedStateToKafkaConfig]
          _ <- task.program
          lastFile <- ExternalOfferEventFileDao.getLastProcessedFilename(eventType)
          records <-
            Consumer
              .subscribeAnd(Subscription.Topics(Set(cfg.topic)))
              .plainStream(Serde.byteArray, classifiedUpdateEventSerde)
              .take(1)
              .runCollect
        } yield {
          assert(lastFile)(isSome(equalTo(s"$filePrefix-1"))) &&
          assert(records.length)(equalTo(1)) &&
          assert(records.head.record.value())(equalTo(expectedActive))
        }
      }

  val processNewFileWithoutDiff: (EventType, String) => ZSpec[Env, Throwable] =
    (eventType: EventType, filePrefix: String) =>
      testM(s"""process newest and last processed files without diff '$filePrefix'""") {
        for {
          _ <- task.program
          lastFile <- ExternalOfferEventFileDao.getLastProcessedFilename(eventType)
        } yield assert(lastFile)(isSome(equalTo(s"$filePrefix-2")))
      }

  val processNewFileWithDiff: (EventType, String) => ZSpec[Any with Clock with Blocking with Has[Consumer] with ExternalOfferEventFileDao with Env, Throwable] =
    (eventType: EventType, filePrefix: String) =>
      testM(s"""process newest and last processed files with diff '$filePrefix'""") {
        for {
          cfg <- ZIO.service[ClassifiedStateToKafkaConfig]
          _ <- task.program
          lastFile <- ExternalOfferEventFileDao.getLastProcessedFilename(eventType)
          records <-
            Consumer
              .subscribeAnd(Subscription.Topics(Set(cfg.topic)))
              .plainStream(Serde.byteArray, classifiedUpdateEventSerde)
              .take(1)
              .runCollect
        } yield assert(lastFile)(isSome(equalTo(s"$filePrefix-2"))) && assert(records.length)(equalTo(1)) && assert(
          records.head.record.value()
        )(equalTo(expectedRemoved))
      }

  private def createTestSpecificEnv(
      topic: String,
      bucket: String,
      lastFile: Option[String],
      s3Content: Seq[S3Content],
      eventType: EventType) = {
    val kafkaProducerConfig = ZLayer.requires[Has[ProducerConfig]]
    val testKafka = ZLayer.requires[TestKafka]
    val transactor = ZLayer.requires[Has[Transactor[Task]]]
    val cfg = ZLayer.succeed(ClassifiedStateToKafkaConfig(topic, bucket))
    val clock = Clock.live
    val blocking = Blocking.live

    val kafkaProducer = ZLayer.fromServiceManaged { producerConfig: ProducerConfig =>
      val producerSettings = ProducerSettings(producerConfig.bootstrapServers)
        .withCloseTimeout(producerConfig.closeTimeout)
        .withProperties(producerConfig.properties)
      Producer.make(producerSettings).orDie
    }

    val kafkaConsumer = (clock ++ blocking ++ testKafka) >>> ZLayer.fromServiceManaged(_ =>
      TestKafka.bootstrapServers.toManaged_.flatMap(servers =>
        Consumer
          .make(
            ConsumerSettings(servers)
              .withGroupId("test")
              .withOffsetRetrieval(OffsetRetrieval.Auto(AutoOffsetStrategy.Earliest))
          )
          .orDie
      )
    )

    val dao = transactor >>> PgExternalOfferEventFileDao.live
    val lastFileInsertion = lastFile match {
      case Some(value) =>
        (dao ++ blocking) >>> ExternalOfferEventFileDao
          .insertNewFilename(eventType, value)
          .toLayer
          .orDie
      case None => ZIO.unit.toLayer
    }

    val inMemoryS3: ZLayer[Any, Nothing, Has[S3Client.Service]] = InMemoryS3.make
      .tap { s3 =>
        ZIO.foreach(s3Content)(v =>
          s3.uploadContent(
            bucket = bucket,
            key = v.filename,
            contentLength = v.content.length,
            contentType = "text/plain",
            content = ZStream.fromChunk(Chunk.fromArray(v.content.getBytes))
          )
        )
      }
      .orDie
      .toLayer

    val s3 = inMemoryS3 >>> S3MultipostingClient.live

    val topicCreation: ZLayer[TestKafka, Nothing, Has[Unit]] = (testKafka ++ blocking) >>>
      (TestKafka.createTopic(topic, numPartitions = 2).orDie).toLayer

    (kafkaProducerConfig ++ Blocking.any >>> kafkaProducer) ++
      kafkaConsumer ++
      topicCreation ++
      s3 ++
      dao ++
      lastFileInsertion ++
      cfg ++
      blocking ++
      clock
  }

  final case class S3Content(filename: String, content: String)
}
