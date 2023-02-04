package ru.yandex.vertis.general.search.scheduler.test

import common.zio.grpc.client.GrpcClient
import common.zio.kafka.ProducerConfig
import common.zio.kafka.scalapb.{ScalaProtobufDeserializer, ScalaProtobufSerializer}
import common.zio.kafka.testkit.TestKafka
import common.zio.kafka.testkit.TestKafka.TestKafka
import general.aglomerat.api.AglomeratServiceGrpc.AglomeratService
import general.bonsai.attribute_model.{AttributeDefinition, NumberSettings}
import general.bonsai.category_model.{Category => BonsaiCategory, CategoryAttribute}
import general.bonsai.public_api.PublicBonsaiServiceGrpc.PublicBonsaiService
import general.bonsai.public_api._
import general.common.price_model
import general.common.price_model.Price
import general.common.price_model.Price.Price.PriceInCurrency
import general.common.seller_model.SellerId
import general.common.seller_model.SellerId.SellerId.UserId
import general.globe.api.GeoServiceGrpc.GeoService
import general.gost.offer_api.OfferServiceGrpc.OfferService
import general.gost.offer_model.AttributeValue.Value.Number
import general.gost.offer_model.OfferStatusEnum.OfferStatus
import general.gost.offer_model.{
  Attribute,
  AttributeValue,
  Category => GostCategory,
  Offer,
  OfferOriginEnum,
  OfferUpdateRecord,
  OfferView
}
import general.gost.seller_model.Seller
import general.users.model.UserView
import general.vasabi.api.VasesServiceGrpc.VasesService
import org.apache.kafka.clients.producer.ProducerRecord
import ru.yandex.vertis.general.aglomerat.testkit.TestAglomeratService
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.bonsai.testkit.TestBonsaiService
import ru.yandex.vertis.general.globe.testkit.TestGeoService
import ru.yandex.vertis.general.gost.testkit.TestOfferService
import ru.yandex.vertis.general.search.logic.{DefaultOfferMapper, SearchEmbedder}
import ru.yandex.vertis.general.search.logic.OfferMapper
import ru.yandex.vertis.general.search.logic.link.OfferLinkBuilder
import ru.yandex.vertis.general.search.scheduler.VasgenExportScheduler.GostExportConfig
import ru.yandex.vertis.general.search.scheduler.{VasgenExportScheduler, VasgenIndexerConfig}
import ru.yandex.vertis.general.search.testkit.TestVasgenSender
import ru.yandex.vertis.general.users.testkit.TestUserService
import ru.yandex.vertis.general.vasabi.testkit.TestVasabiService
import common.zio.logging.Logging
import ru.yandex.vertis.general.search.logic.extractors.DefaultOfferViewFieldExtractor
import vertis.vasgen.document.{QualifiedBatch, RawDocument}
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.consumer.ConsumerSettings
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.{Deserializer, Serde, Serializer}
import zio.test.Assertion._
import zio.test._

object VasgenExportSchedulerTest extends DefaultRunnableSpec {

  private val qualifiedBatchDeserializer = Deserializer(new ScalaProtobufDeserializer[QualifiedBatch])
  private val offerUpdateSerializer = Serializer(new ScalaProtobufSerializer[OfferUpdateRecord])

  private val OfferId = "123456789"
  private val CategoryId = "category_id"
  private val CategoryVersion = 1
  private val AttributeId = "attribute_id"
  private val AttributeVersion = 2

  private val ValidOfferView = OfferView(
    sellerId = Some(SellerId(UserId(123))),
    offerId = OfferId,
    offer = Some(
      Offer(
        title = "TITLE",
        description = "DESCRIPTION",
        price = Some(Price(PriceInCurrency(price_model.PriceInCurrency(100)))),
        category = Some(GostCategory("category_id", 1)),
        attributes = Seq(Attribute("attribute_id", 2, Some(AttributeValue(Number(3))))),
        seller = Some(Seller())
      )
    ),
    status = OfferStatus.ACTIVE,
    version = 1,
    origin = OfferOriginEnum.OfferOrigin.FORM
  )

  private val ValidUsers = Seq(UserView(123))

  private val ValidBonsaiCategory = BonsaiCategory(
    id = CategoryId,
    version = CategoryVersion,
    name = "CATEGORY_NAME",
    attributes = Seq(
      CategoryAttribute(
        attributeId = AttributeId,
        version = AttributeVersion
      )
    )
  )

  private val ValidAttributeDefinition = AttributeDefinition(
    id = AttributeId,
    version = AttributeVersion,
    name = "CATEGORY_NAME",
    attributeSettings = AttributeDefinition.AttributeSettings.NumberSettings(NumberSettings())
  )

  private val ValidGetCategoryResponse = GetCategoryResponse(
    Some(
      CategoryData(
        Some(ValidBonsaiCategory),
        Map(AttributeId -> AttributeData(Some(ValidAttributeDefinition)))
      )
    )
  )

  private def createTestSpecificEnv(gostTopic: String) = {
    val kafkaProducerConfig = ZLayer.requires[Has[ProducerConfig]]
    val kafkaConsumerSettings = ZLayer.requires[Has[ConsumerSettings]]
    val testKafka = ZLayer.requires[TestKafka]
    val logging = Logging.live

    val gostProducer = ZLayer.fromServiceManaged { producerConfig: ProducerConfig =>
      val producerSettings = ProducerSettings(producerConfig.bootstrapServers)
        .withCloseTimeout(producerConfig.closeTimeout)
        .withProperties(producerConfig.properties)
      Producer.make[Any, String, OfferUpdateRecord](producerSettings, Serde.string, offerUpdateSerializer).orDie
    }

    val clock = Clock.live
    val testBonsai = TestBonsaiService.layer
    val testGost = TestOfferService.layer
    val testUsers = TestUserService.withUsers(ValidUsers)
    val testVasabi = TestVasabiService.layer
    val testGlobe = TestGeoService.layer
    val testAglomerat = TestAglomeratService.layer
    val searchEmbedder = SearchEmbedder.noop
    val offerLinkBuilder = UIO(new OfferLinkBuilder.Service {
      override def buildOfferLinks(offersView: Seq[OfferView]): Task[Seq[String]] = ZIO.succeed(Seq("o.yandex.ru"))
    }).toLayer
    val offerMapper =
      (testBonsai ++ testGlobe ++ searchEmbedder ++ logging ++ testVasabi ++ clock ++ testGost ++ testAglomerat ++ offerLinkBuilder) >>> {
        for {
          bonsai <- ZIO.service[GrpcClient.Service[PublicBonsaiService]]
          globe <- ZIO.service[GrpcClient.Service[GeoService]]
          vasabi <- ZIO.service[GrpcClient.Service[VasesService]]
          gost <- ZIO.service[GrpcClient.Service[OfferService]]
          aglomerat <- ZIO.service[GrpcClient.Service[AglomeratService]]
          bonsaiSnapshotRef <- ZRef.make(new BonsaiSnapshot(List.empty, List.empty))
          logging <- ZIO.service[Logging.Service]
          offerLinkBuilder <- ZIO.service[OfferLinkBuilder.Service]
          searchEmbedder <- ZIO.service[SearchEmbedder.Service]
          clock <- ZIO.service[Clock.Service]
          offerViewFieldExtractor = new DefaultOfferViewFieldExtractor(
            bonsaiSnapshotRef = bonsaiSnapshotRef,
            bonsai = bonsai,
            globe = globe,
            gost = gost,
            offerLinkBuilder = offerLinkBuilder,
            log = logging
          )
        } yield new DefaultOfferMapper(
          offerViewFieldExtractor = offerViewFieldExtractor,
          vasabi = vasabi,
          aglomerat = aglomerat,
          searchEmbedder = searchEmbedder,
          log = logging,
          clock = clock
        ): OfferMapper.Service
      }.toLayer

    val gostExportConfig = ZLayer.succeed(GostExportConfig(gostTopic, s"$gostTopic-group"))
    val vasgenIndexerConfig = ZLayer.succeed(VasgenIndexerConfig("general"))
    val testVasgenSender = TestVasgenSender.test

    val log = Logging.live
    val blocking = Blocking.live

    val topicCreation: ZLayer[TestKafka, Nothing, Has[Unit]] = (testKafka ++ blocking) >>>
      (TestKafka.createTopic(gostTopic, numPartitions = 2).orDie).toLayer

    val vasgenExportScheduler = topicCreation ++
      kafkaConsumerSettings ++
      gostExportConfig ++
      testVasgenSender ++
      vasgenIndexerConfig ++
      log ++
      clock ++
      blocking ++
      offerMapper ++
      testGost ++
      testUsers >>> VasgenExportScheduler.live

    val runningScheduler = vasgenExportScheduler >>> ZLayer.fromServiceManaged { scheduler =>
      scheduler.run.forkManaged.unit
    }

    (kafkaProducerConfig >>> gostProducer) ++
      runningScheduler ++
      testBonsai ++
      testGost ++
      testUsers ++
      testVasgenSender ++
      blocking ++
      clock
  }

  private def produceOfferAsGost(
      topic: String,
      offer: OfferView,
      partition: Int = 0): RIO[Blocking with Producer[Any, String, OfferUpdateRecord], Unit] =
    for {
      producer <- ZIO.service[Producer.Service[Any, String, OfferUpdateRecord]]
      _ <- producer.produce(new ProducerRecord(topic, partition, offer.offerId, OfferUpdateRecord(offer = Some(offer))))
    } yield ()

  private def consumeAsVasgen(maximumDocuments: Int): RIO[Has[TestVasgenSender.Service] with Clock, Seq[RawDocument]] =
    TestVasgenSender.get
      .repeatWhile(_.size < maximumDocuments)
      .timeoutFail(new RuntimeException("scheduler did not produce enough documents"))(60.seconds)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("VasgenExportScheduler")(
      testM("Map documents and send to vasgen") {
        for {
          _ <- TestBonsaiService.setGetCategoryResponse(request =>
            ZIO
              .fail(new RuntimeException("category not found"))
              .when(!request.entityRef.exists(ref => ref.id == CategoryId /*&& ref.version == CategoryVersion*/ ))
              .as(ValidGetCategoryResponse)
          )
          _ <- produceOfferAsGost("gost-1", ValidOfferView)
          documents <- consumeAsVasgen(1)
        } yield assert(documents)(hasSize(equalTo(1)))
      }.provideLayer(createTestSpecificEnv("gost-1")),
      testM("Fallback to gost when offerView is broken") {
        for {
          _ <- TestBonsaiService.setGetCategoryResponse(request =>
            ZIO
              .fail(new RuntimeException("category not found"))
              .when(!request.entityRef.exists(ref => ref.id == CategoryId /*&& ref.version == CategoryVersion*/ ))
              .as(ValidGetCategoryResponse)
          )
          callToGost <- zio.Promise.make[Nothing, Unit]
          _ <- TestOfferService.setGetOfferResponse(request =>
            ZIO
              .fail(new RuntimeException("category not found"))
              .when(request.offerId != OfferId || request.filters.exists(filter => !filter.includeRemoved))
              .tap(_ => callToGost.succeed(()))
              .as(ValidOfferView.copy(version = 100))
          )
          _ <- produceOfferAsGost("gost-2", ValidOfferView.copy(sellerId = None)) // must break mapper
          _ <- callToGost.await.timeoutFail(new RuntimeException("Call to gost was not performed"))(60.seconds)
          documents <- consumeAsVasgen(1)
        } yield assert(documents)(
          hasSize[RawDocument](equalTo(1)) &&
            hasFirst(hasField[RawDocument, Long]("version", _.version, equalTo(100L)))
        )
      }.provideLayer(createTestSpecificEnv("gost-2")),
      testM("Retry mapping if neither standard procedure nor gost fallback works") {
        for {
          callToBonsai <- zio.Promise.make[Nothing, Unit]
          _ <- TestBonsaiService.setGetCategoryResponse(_ =>
            callToBonsai.succeed(()) *> ZIO.fail(new RuntimeException("bonsai does not work sorry"))
          )
          callToGost <- zio.Promise.make[Nothing, Unit]
          _ <- TestOfferService.setGetOfferResponse(_ =>
            callToGost.succeed(()) *> ZIO.fail(new RuntimeException("gost does not work either"))
          )
          _ <- produceOfferAsGost("gost-3", ValidOfferView)
          _ <- callToBonsai.await.timeoutFail(new RuntimeException("Call to bonsai was not performed"))(30.seconds)
          _ <- callToGost.await.timeoutFail(new RuntimeException("Call to gost was not performed"))(30.seconds)
          _ <- TestBonsaiService.setGetCategoryResponse(request => // repairing bonsai
            ZIO
              .fail(new RuntimeException("category not found"))
              .when(!request.entityRef.exists(ref => ref.id == CategoryId /*&& ref.version == CategoryVersion*/ ))
              .as(ValidGetCategoryResponse)
          )
          documents <- consumeAsVasgen(1)
        } yield assert(documents)(hasSize(equalTo(1)))
      }.provideLayer(createTestSpecificEnv("gost-3")),
      testM("partitions are handled independently") {
        for {
          _ <- TestBonsaiService.setGetCategoryResponse(request =>
            ZIO
              .fail(new RuntimeException("category not found"))
              .when(!request.entityRef.exists(ref => ref.id == CategoryId /*&& ref.version == CategoryVersion*/ ))
              .as(ValidGetCategoryResponse)
          )
          _ <- TestOfferService.setGetOfferResponse(request => ZIO.fail(new RuntimeException("NOPE")))
          _ <- produceOfferAsGost("gost-4", ValidOfferView.copy(sellerId = None, version = 50), 1)
            .repeat(Schedule.recurs(203)) // broken offers
          expectedValidOffersNumber = 208
          _ <- produceOfferAsGost("gost-4", ValidOfferView.copy(version = 10), 0)
            .repeat(Schedule.recurs(expectedValidOffersNumber - 1)) // valid offers
          result <- consumeAsVasgen(expectedValidOffersNumber)
        } yield assert(result)(forall(hasField("version", _.version, equalTo(10L))))
      }.provideLayer(createTestSpecificEnv("gost-4"))
    ).provideCustomLayerShared {
      val kafka = TestKafka.live
      val kafkaProducerConfig = kafka >>> ZLayer.fromServiceM(_ =>
        TestKafka.bootstrapServers.map(servers => ProducerConfig(servers, 30.seconds, Map.empty))
      )
      val kafkaConsumerSettings =
        kafka >>> ZLayer.fromServiceM(_ =>
          TestKafka.bootstrapServers.map(servers =>
            ConsumerSettings(servers)
              .withOffsetRetrieval(OffsetRetrieval.Auto(AutoOffsetStrategy.Earliest))
          )
        )

      kafka ++ kafkaProducerConfig ++ kafkaConsumerSettings
    }
  }
}
