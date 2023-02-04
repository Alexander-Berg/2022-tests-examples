package ru.yandex.vertis.general.gost.logic.test

import common.zio.kafka.ProducerConfig
import common.zio.kafka.testkit.TestKafka
import common.zio.logging.Logging
import common.zio.ops.tracing.testkit.TestTracing
import common.zio.ydb.Ydb
import common.zio.ydb.Ydb.HasTxRunner
import common.zio.ydb.testkit.TestYdb
import general.common.editor_model.EditorInfo
import general.gost.offer_model.{Offer, OfferUpdateRecord, OfferView}
import general.gost.storage.ydb.feed.YdbFeedIdsMappingDao
import org.apache.kafka.clients.consumer.ConsumerRecord
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.common.cache.{Cache, RequestCacher}
import ru.yandex.vertis.general.common.dictionaries.testkit.TestBansDictionaryService
import ru.yandex.vertis.general.common.model.editor.testkit.Editors
import ru.yandex.vertis.general.globe.testkit.TestGeoService
import ru.yandex.vertis.general.gost.logic.QueueManager.QueueExportConfig
import ru.yandex.vertis.general.gost.logic.StageManager.Stage
import ru.yandex.vertis.general.gost.logic._
import ru.yandex.vertis.general.gost.logic.testkit.TestValidationManager
import ru.yandex.vertis.general.gost.model.config.ActualizationConfig
import ru.yandex.vertis.general.gost.model.testkit.OfferGen
import ru.yandex.vertis.general.gost.model.validation.ValidationError
import ru.yandex.vertis.general.gost.model.{OfferExport, UpdateResult}
import ru.yandex.vertis.general.gost.storage.ydb.YdbQueueDao
import ru.yandex.vertis.general.gost.storage.ydb.counters.YdbTotalCountersDao
import ru.yandex.vertis.general.gost.storage.ydb.offer.YdbOfferDao
import ru.yandex.vertis.general.gost.storage.ydb.preset.{YdbOfferPresetDao, YdbOfferPresetsCountDao}
import ru.yandex.vertis.general.gost.storage.ydb.scheduler.{YdbStageQueueDao, YdbStageStateDao}
import ru.yandex.vertis.ydb.zio.{Tx, TxRunner}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.kafka.serde.Serde
import zio.random.Random
import zio.test.Assertion._
import zio.test._
import zio.{Ref, ZIO, ZLayer}

object DefaultQueueManagerTest extends DefaultRunnableSpec {

  val queueExportConfig: QueueExportConfig = QueueExportConfig("topic", 10)

  private def runTx[R <: Clock with HasTxRunner, E, A](action: Tx[R, E, A]): ZIO[R, E, A] =
    ZIO.service[TxRunner].flatMap(_.runTx(action).flatMapError(_.getOrDie))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {

    suite("DefaultQueueManager")(
      testM("Export offers to queue") {
        checkNM(1)(
          OfferGen.anyActiveOffer.noShrink
        ) { offer =>
          for {
            _ <- TestKafka.createTopic(queueExportConfig.topic)

            publishResult <- runTx(
              OfferStore.updateOffer(offer.offerId, Editors.seller(offer.sellerId))(o =>
                ZIO.succeed(UpdateResult(offer, List.empty[ValidationError]))
              )
            )

            offerId = publishResult.offer.offerId
            shardId = OfferExport.shardId(offerId)

            // После создания оффер должен быть в очереди
            batchBeforeExport <- QueueManager.pull(shardId)
            assertBeforeExport = assert(batchBeforeExport)(isNonEmpty)

            // прогон экспорта шардов
            _ <- QueueManager.exportShards(Set(shardId))

            // После одного прохода в очереди должно быть пусто
            batchAfterExport <- QueueManager.pull(shardId)
            assertAfterExport = assert(batchAfterExport)(isEmpty)

            records <-
              Consumer
                .subscribeAnd(Subscription.Topics(Set(queueExportConfig.topic)))
                .plainStream(Serde.string, QueueManager.offerUpdateSerde)
                .take(1)
                .runCollect

            record = records.head.record

            // проверка того, что записалось в кафку
            assertQueueRecords = assert(record) {
              hasField("key", (r: ConsumerRecord[String, OfferUpdateRecord]) => r.key, equalTo(offerId.id)) &&
              hasField(
                "value",
                (r: ConsumerRecord[String, OfferUpdateRecord]) => r.value().getOffer,
                hasField("id", (o: OfferView) => o.offerId, equalTo(offerId.id)) &&
                  hasField(
                    "offer",
                    (o: OfferView) => o.offer,
                    isSome(
                      hasField("title", (o: Offer) => o.title, equalTo(offer.title))
                    )
                  )
              ) &&
              hasField(
                "editor",
                (r: ConsumerRecord[String, OfferUpdateRecord]) => r.value().editor,
                equalTo(Editors.seller(offer.sellerId).asInstanceOf[EditorInfo])
              )
            }

          } yield assertBeforeExport && assertAfterExport && assertQueueRecords
        }
      }
      // ToDo You should provide a valid offer to publish to test that
    )
  }.provideCustomLayerShared {
    val kafka = TestKafka.live
    val kafkaProducerConfig = kafka >>> ZLayer.fromServiceM(_ =>
      TestKafka.bootstrapServers.map(servers => ProducerConfig(servers, 30.seconds, Map.empty))
    )
    val kafkaConsumer = (Clock.live ++ Blocking.live ++ kafka) >>> ZLayer.fromServiceManaged(_ =>
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

    val queueDao = YdbQueueDao.live
    val clock = Clock.live
    val logging = Logging.live
    val random = Random.live
    val cacher = Cache.noop ++ logging >>> RequestCacher.live
    val presetsDao = YdbOfferPresetDao.live
    val presetsCountDao = YdbOfferPresetsCountDao.live
    val stageQueueDao = YdbStageQueueDao.live
    val stageStateDao = YdbStageStateDao.live
    val offerDao = YdbOfferDao.live
    val presetStore = (presetsDao ++ presetsCountDao) >+> OfferPresetsStore.live
    val totalCountersDao = YdbTotalCountersDao.live
    val totalCountersStore = totalCountersDao >>> TotalCountersStore.live
    val offerStore =
      (offerDao ++ YdbFeedIdsMappingDao.live ++ queueDao ++ presetStore ++ totalCountersStore ++ clock) >+> OfferStore.live
    val dictionaryService = TestBansDictionaryService.emptyLayer
    val bonsaiSnapshot = Ref.make(BonsaiSnapshot(Seq.empty)).toLayer
    val offerDeps =
      clock ++ logging ++
        TestValidationManager.layer ++
        bonsaiSnapshot ++
        dictionaryService ++
        ChangeCategoryEventSender.noop ++
        (TestGeoService.layer ++ cacher >>> SellingAddressEnricher.live) ++
        (TestYdb.ydb >>> (offerDao ++ presetsDao ++ presetsCountDao ++ totalCountersDao ++ offerStore ++ Ydb.txRunner))

    val avatarsConfig = ZIO.succeed(AvatarsConfig("url")).toLayer
    val actualizationConfig = ZIO.succeed(ActualizationConfig()).toLayer
    val offerManager = offerDeps >>> OfferManager.live

    val stages = ZLayer.succeed(List.empty[Stage])
    val stageManager = TestYdb.ydb >+> (Ydb.txRunner ++ stageQueueDao ++ stageStateDao ++ offerStore ++ offerDao) ++
      random ++ clock ++ logging ++ dictionaryService ++ avatarsConfig ++ actualizationConfig ++ stages >+> StageManager.live

    val queueDeps =
      TestYdb.ydb >>> (queueDao ++ Ydb.txRunner) ++ kafkaProducerConfig ++ ZLayer.succeed(
        queueExportConfig
      ) ++ clock ++ avatarsConfig ++ dictionaryService ++ stageManager ++ TestTracing.noOp

    val queueManager = queueDeps >+> QueueManager.live

    queueManager ++ offerManager ++ Blocking.live ++ kafka ++ kafkaConsumer
  }
}
