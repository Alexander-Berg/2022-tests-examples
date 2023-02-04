package ru.yandex.vertis.general.users.logic.test

import common.clients.blackbox.testkit.BlackboxClientTest
import common.clients.personality.testkit.PersonalityClientTest
import common.tvm.model.UserTicket.TicketBody
import common.zio.kafka.ProducerConfig
import common.zio.kafka.testkit.TestKafka
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import general.users.model.{User, UserView}
import org.apache.kafka.clients.consumer.ConsumerRecord
import ru.yandex.vertis.general.common.clients.clean_web.testkit.CleanWebClientTest
import ru.yandex.vertis.general.common.dictionaries.testkit.TestBansDictionaryService
import ru.yandex.vertis.general.globe.testkit.TestGeoService
import ru.yandex.vertis.general.users.logic.QueueManager.QueueExportConfig
import ru.yandex.vertis.general.users.logic.testkit.TestMiminoUserEnricher
import ru.yandex.vertis.general.users.logic.{CleanWebHelper, QueueManager, UserManager, UserStore, UserValidator}
import ru.yandex.vertis.general.users.model.User.{UserId, UserInput}
import ru.yandex.vertis.general.users.model.UserExport
import ru.yandex.vertis.general.users.resources.permissions.PermissionsSnapshot
import ru.yandex.vertis.general.users.storage.ydb.{YdbQueueDao, YdbUserDao}
import common.zio.logging.Logging
import zio.{Ref, ZLayer}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.kafka.serde.Serde
import zio.test.Assertion._
import zio.test._

object DefaultQueueManagerTest extends DefaultRunnableSpec {

  val queueExportConfig: QueueExportConfig = QueueExportConfig("topic", 10, 30)

  // ./ya tools tvmknife unittest user --default 1
  private val ticket =
    TicketBody(
      "3:user:CA0Q__________9_Gg4KAggBEAEg0oXYzAQoAQ:FUF1bTMjaFuxEyM1-TsldpgNPJ8mXrLR0fgdo-PQ_faY_gvS1wqbzRek0mLUWmhoShgwrUinCIHCfEIPhyTZcBHFhZ4FCm1vkSHPfARZKnf0Ok6aIw2FFWPvmoulU25nwDgL6UWKz-30IgAyTRReotwlOd8jZO_LKWCZT86c8nox"
    )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {

    suite("DefaultQueueManager")(
      testM("Export users to queue") {
        for {
          _ <- TestKafka.createTopic(queueExportConfig.topic)
          userId = UserId(123)
          _ <- UserManager.updateUser(userId, ticket, UserInput(Some("name"), Seq(), None, None, None, None))
          shardId = UserExport.shardId(userId)
          batchBeforeExport <- QueueManager.pull(shardId)
          _ <- QueueManager.exportShards(Set(shardId))
          batchAfterExport <- QueueManager.pull(shardId)
          records <-
            Consumer
              .subscribeAnd(Subscription.Topics(Set(queueExportConfig.topic)))
              .plainStream(Serde.long, QueueManager.userSerde)
              .take(1)
              .runCollect
          assertBeforeExport = assert(batchBeforeExport)(isNonEmpty)
          record = records.head.record
          assertQueueRecords = assert(record) {
            hasField("key", (r: ConsumerRecord[Long, UserView]) => r.key, equalTo(userId.id)) &&
            hasField(
              "value",
              (r: ConsumerRecord[Long, UserView]) => r.value(),
              hasField("id", (u: UserView) => u.id, equalTo(userId.id)) &&
                hasField(
                  "user",
                  (u: UserView) => u.user,
                  isSome(
                    hasField("name", (u: User) => u.name, isSome(equalTo("name")))
                  )
                )
            )
          }
          assertAfterExport = assert(batchAfterExport)(isEmpty) && assertQueueRecords

        } yield assertBeforeExport && assertAfterExport
      }
    ).provideCustomLayerShared {
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
      val userDao = YdbUserDao.live
      val testGLobe = TestGeoService.layer
      val userStore = (userDao ++ queueDao ++ clock) >>> UserStore.live
      val cleanWebHelper = CleanWebClientTest.live >>> CleanWebHelper.live
      val userValidator = cleanWebHelper >>> UserValidator.live
      val miminoUserEnricher = TestMiminoUserEnricher.layer
      val dictionary = TestBansDictionaryService.layer
      val userDeps =
        TestYdb.ydb >>> (userDao ++ userStore ++ Ydb.txRunner) ++ clock ++ logging ++ BlackboxClientTest.Test ++
          PersonalityClientTest.Test ++ testGLobe ++ userValidator ++ miminoUserEnricher ++ dictionary
      val draftManager = userDeps >>> UserManager.live
      val permissionsRef = Ref.make(PermissionsSnapshot(Set.empty, Set.empty)).toLayer

      val queueDeps =
        TestYdb.ydb >>> (queueDao ++ Ydb.txRunner) ++ kafkaProducerConfig ++ ZLayer.succeed(queueExportConfig) ++
          clock ++ logging ++ dictionary ++ permissionsRef
      val queueManager = queueDeps >>> QueueManager.live
      queueManager ++ draftManager ++ Blocking.live ++ kafka ++ kafkaConsumer
    }
  }
}
