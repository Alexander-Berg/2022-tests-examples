package ru.yandex.vertis.general.feed.processor.pipeline.test

import java.io.File
import java.net.URL
import common.zio.kafka.ProducerConfig
import common.zio.kafka.scalapb.ScalaProtobufDeserializer
import common.zio.kafka.testkit.TestKafka
import general.bonsai.attribute_model.{AttributeDefinition, StringSettings}
import general.bonsai.attribute_model.AttributeDefinition.AttributeSettings
import general.common.address_model.{AddressInfo, SellingAddress}
import general.feed.transformer.{FeedFormat, RawCondition, RawOffer}
import general.gost.feed_api.{FeedStreamRequest, OfferBatch}
import general.users.model.{LimitedUserView, User, UserView}
import org.apache.commons.io.FileUtils
import ru.yandex.vertis.general.common.model.user.SellerId
import ru.yandex.vertis.general.common.model.user.SellerId.toApiSellerId
import ru.yandex.vertis.general.feed.model.{FeedHash, FeedSource, NamespaceId}
import ru.yandex.vertis.general.feed.logic.FeedEventProducer.FeedExportConfig
import ru.yandex.vertis.general.feed.logic.{FeedEventProducer, FeedStatisticsManager}
import ru.yandex.vertis.general.feed.parser.FeedParser
import ru.yandex.vertis.general.feed.processor.model._
import ru.yandex.vertis.general.feed.processor.pipeline.DeduplicatorFactory.DeduplicatorFactory
import ru.yandex.vertis.general.feed.processor.pipeline.DefaultGostSender.GostSenderConfig
import ru.yandex.vertis.general.feed.processor.pipeline._
import ru.yandex.vertis.general.feed.processor.pipeline.unification.{FeedFixUtil, UnificationPipeline, UnifiedOffer}
import ru.yandex.vertis.general.feed.processor.pipeline.unification.UnificationPipeline.UnificationPipeline
import ru.yandex.vertis.general.feed.transformer.logic.{FeedDownloader, FeedTransformer}
import ru.yandex.vertis.general.users.testkit.TestUserService
import common.zio.logging.Logging
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.consumer.{CommittableRecord, Consumer, ConsumerSettings, Subscription}
import zio.kafka.serde.{Deserializer, Serde}
import zio.test.Assertion._
import zio.test._
import zio.{Chunk, IO, UIO, ULayer, ZIO, ZLayer}

object DefaultPipelineTest extends DefaultRunnableSpec {
  private val feedStreamRequestDeserializer = Deserializer(new ScalaProtobufDeserializer[FeedStreamRequest])
  private val gostSenderConfig = GostSenderConfig("feed-gost", 1000)
  private val feedExportConfig = FeedExportConfig("feed-event-sender")

  private val userId = 123

  private val downloader = ZLayer.succeed {
    new FeedDownloader.Service {
      override def download(url: URL, file: File): IO[FeedTransformer.Error, FeedFormat] = {
        IO.effectTotal {
          FileUtils.copyInputStreamToFile(url.openStream(), file)
        }.as(FeedFormat.GENERAL)
      }
    }
  }

  private def checkEof(
      record: CommittableRecord[String, FeedStreamRequest],
      sellerId: SellerId) = {
    val request = record.record.value()
    assert(request.sellerId)(isSome(equalTo(toApiSellerId(sellerId)))) &&
    assert(request.message.isEof)(isTrue)
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("DefaultPipeline")(
      testM("process feed and send result to kafka") {
        val from = getClass.getResource("/pipeline.xml")
        val taskId = 10
        val sellerId = SellerId.UserId(userId)
        val namespaceId = NamespaceId("test_197561590")
        for {
          _ <- TestKafka.createTopic(gostSenderConfig.topic)
          result <- Pipeline.processFeed(sellerId, namespaceId, taskId, from, FeedSource.Feed)
          _ <- Pipeline.processRemovedFeed(sellerId, namespaceId, 999) // to indicate EndOfStream
          records <-
            Consumer
              .subscribeAnd(Subscription.Topics(Set(gostSenderConfig.topic)))
              .plainStream(Serde.string, feedStreamRequestDeserializer)
              .takeWhile(_.record.value().taskId == taskId)
              .runCollect
        } yield {
          assert(result)(isSubtype[FeedHash.OfferHash](hasField("value", _.value, equalTo(-72404507)))) &&
          checkEof(records.last, sellerId) &&
          assert(records.init)(isNonEmpty) &&
          assert(records.init.map(_.record.value().message.isBatch))(forall(isTrue)) &&
          assert(records.init.map(_.record.value().sellerId))(forall(isSome(equalTo(toApiSellerId(sellerId))))) &&
          assert(records.init.map(_.record.value().getBatch.offers.size).sum)(equalTo(4)) &&
          assert(records.init.map(_.record.value().getBatch.batchId))(
            equalTo(Chunk.fromIterable(0L until records.size - 1))
          )
        }
      }
    ).provideCustomLayerShared {
      val clock = Clock.live
      val kafka = TestKafka.live
      val logging = Logging.live
      val kafkaProducerConfig = kafka >>> ZLayer.fromServiceM(_ =>
        TestKafka.bootstrapServers.map(servers => ProducerConfig(servers, 30.seconds, Map.empty))
      )
      val kafkaConsumer = (clock ++ Blocking.live ++ kafka) >>> ZLayer.fromServiceManaged(_ =>
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
      val gostSender = kafkaProducerConfig ++ ZLayer.succeed(gostSenderConfig) >>> DefaultGostSender.Live
      val deduplicatorFactory: ULayer[DeduplicatorFactory] = ZLayer.succeed(FakeDeduplicatorFactory)
      val unificationPipeline: ULayer[UnificationPipeline] = ZLayer.succeed(FakeUnificationPipeline)

      val feedTransformer = (Blocking.live ++ FeedParser.Live ++ downloader) >>> FeedTransformer.live

      val feedStatisticsManager = FeedStatisticsManager.stub

      val userService = TestUserService.withUsers(Seq(UserView(userId)))

      val kafkaFeedEventProducerConfig = kafka >>> ZLayer.fromServiceM(_ =>
        TestKafka.bootstrapServers.map(servers => ProducerConfig(servers, 30.seconds, Map.empty))
      )
      val feedEventProducer =
        (Blocking.live ++ kafkaFeedEventProducerConfig ++ ZLayer.succeed(feedExportConfig)) >>> FeedEventProducer.live

      val pipeline =
        (userService ++ clock ++ logging ++ feedTransformer ++ deduplicatorFactory ++
          unificationPipeline ++ feedStatisticsManager ++ gostSender ++ feedEventProducer) >+> Pipeline.live
      kafka ++ kafkaConsumer ++ pipeline
    }
  }

  object FakeDeduplicatorFactory extends DeduplicatorFactory.Service {
    override def strictDeduplicator: UIO[Deduplicator] = ZIO.succeed(_ => ZIO.unit)

    override def approximateDeduplicator: UIO[Deduplicator] = strictDeduplicator
  }

  object FakeUnificationPipeline extends UnificationPipeline.Service {

    override def unify(
        feedSource: FeedSource,
        offer: RawOffer,
        sellerId: SellerId): UIO[UnificationResult] = {
      ZIO.succeed(
        UnificationResult(
          offerId = offer.externalId,
          title = offer.title,
          category = Some("some-category-name"),
          errors = List.empty,
          offer = Some(
            UnifiedOffer(
              Category(
                id = s"category-${offer.externalId}",
                "some-category-name",
                version = 123,
                Map.empty,
                false,
                true,
                true
              ),
              Seq(
                Attribute(
                  definition = AttributeDefinition(
                    id = s"attribute-${offer.externalId}",
                    name = "name",
                    version = 456,
                    attributeSettings = AttributeSettings.StringSettings(StringSettings())
                  ),
                  StringValue(s"attribute-value-${offer.externalId}")
                )
              ),
              Seq(SellingAddress(address = Some(AddressInfo(s"address-${offer.externalId}")))),
              offer.seller.flatMap(_.contacts),
              RawCondition.Condition.USED,
              images = offer.images.map(FeedFixUtil.enrichImageWithProtocol)
            )
          ),
          rawOffer = offer
        )
      )
    }
  }
}
