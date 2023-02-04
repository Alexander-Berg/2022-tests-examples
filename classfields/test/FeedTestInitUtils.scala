package ru.yandex.vertis.general.feed.logic.test

import common.zio.doobie.testkit.TestPostgresql
import common.zio.kafka.ProducerConfig
import common.zio.kafka.testkit.TestKafka
import common.zio.kafka.testkit.TestKafka.{bootstrapServers, TestKafka}
import ru.yandex.vertis.general.feed.logic.FeedEventProducer.FeedExportConfig
import ru.yandex.vertis.general.feed.logic._
import ru.yandex.vertis.general.feed.storage.postgresql._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.{duration, Has, ZIO, ZLayer}

import java.util.concurrent.TimeUnit

object FeedTestInitUtils {

  val dao =
    PgFeedDao.live ++ PgTaskDao.live ++ PgTaskQueueDao.live ++ PgFeedLoaderTaskDao.live ++ PgFeedLockDao.live ++ PgTaskCleanerQueueDao.live

  def feedEventProducer = {
    val feedEventsConfig = ZLayer.succeed(FeedExportConfig("feed-event"))
    val kafkaFeedEventProducerConfig: ZLayer[TestKafka, Nothing, Has[ProducerConfig]] = bootstrapServers
      .flatMap(servers =>
        ZIO
          .succeed(
            ProducerConfig(
              servers,
              duration.Duration.apply(1L, TimeUnit.MINUTES),
              Map()
            )
          )
      )
      .toLayer
    Blocking.live ++ feedEventsConfig ++ (TestKafka.live >>> kafkaFeedEventProducerConfig) >>> FeedEventProducer.live
  }

  def feedDaos = {
    val transactor = TestPostgresql.managedTransactor
    transactor >+> (dao >+> TaskStore.live)
  }

  def feedManager = {
    val transactor = TestPostgresql.managedTransactor
    (Clock.live ++ feedDaos ++ transactor) >+> FeedManager.live
  }

  def taskManager = Clock.live ++ feedDaos >>> TaskManager.live
  def taskQueueManager = Clock.live ++ feedDaos >>> TaskQueueManager.live

}
