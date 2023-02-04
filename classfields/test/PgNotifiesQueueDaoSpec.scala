package ru.yandex.vertis.general.snatcher.storage.test

import java.time.Instant

import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.common.model.user.SellerId
import ru.yandex.vertis.general.snatcher.storage.NotifiesQueueDao
import ru.yandex.vertis.general.snatcher.storage.postgresql.notifies_queue.PgNotifiesQueueDao
import zio.ZIO
import zio.test._
import zio.test.Assertion._

object PgNotifiesQueueDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("PgLocksDao")(
      testM("Enqueue and poll seller") {
        for {
          dao <- ZIO.service[NotifiesQueueDao.Service]
          seller = SellerId.UserId(1L)
          _ <- dao.enqueueWithUpsert(seller, Instant.now.minusSeconds(10L)).transactIO
          readyToNotify <- dao.poll(1000, Instant.now).transactIO
        } yield assert(readyToNotify)(contains(seller))
      },
      testM("Enqueue and do not poll") {
        for {
          dao <- ZIO.service[NotifiesQueueDao.Service]
          seller = SellerId.UserId(2L)
          _ <- dao.enqueueWithUpsert(seller, Instant.now.plusSeconds(10000L)).transactIO
          readyToNotify <- dao.poll(1000, Instant.now).transactIO
        } yield assert(readyToNotify)(not(contains(seller)))
      },
      testM("Enqueue, delete and do not poll") {
        for {
          dao <- ZIO.service[NotifiesQueueDao.Service]
          seller = SellerId.UserId(3L)
          _ <- dao.enqueueWithUpsert(seller, Instant.now.minusSeconds(10L)).transactIO
          _ <- dao.deleteFromQueue(seller).transactIO
          readyToNotify <- dao.poll(1000, Instant.now).transactIO
        } yield assert(readyToNotify)(not(contains(seller)))
      },
      testM("Enqueue works like upsert") {
        for {
          dao <- ZIO.service[NotifiesQueueDao.Service]
          seller = SellerId.UserId(4L)
          _ <- dao.enqueueWithUpsert(seller, Instant.now.minusSeconds(10L)).transactIO
          _ <- dao.enqueueWithUpsert(seller, Instant.now.plusSeconds(10000L)).transactIO
          readyToNotify <- dao.poll(1000, Instant.now).transactIO
        } yield assert(readyToNotify)(not(contains(seller)))
      }
    ).provideCustomLayerShared(
      TestPostgresql.managedTransactor >+> PgNotifiesQueueDao.live
    )
}
