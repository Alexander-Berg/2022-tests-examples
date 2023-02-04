package ru.yandex.vertis.general.users.storage

import common.zio.ydb.Ydb.HasTxRunner
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.general.users.model.{User, UserExport}
import ru.yandex.vertis.general.users.model.testkit.UserGen
import ru.yandex.vertis.general.users.storage.QueueDao.QueueDao
import zio.clock.Clock
import zio.random.Random
import zio.test.Assertion._
import zio.test._
import java.time.Instant
import java.time.temporal.ChronoField.MILLI_OF_SECOND
import java.time.temporal.ChronoUnit

object QueueDaoSpec {

  def spec(
      label: String): Spec[QueueDao with Clock with HasTxRunner with Random with Sized with TestConfig, TestFailure[Nothing], TestSuccess] = {
    suite(label)(
      testM("push & pull users") {
        checkNM(1)(UserGen.anyUser, UserGen.anyUserId) { (userInput, userId) =>
          val user = User.fromUserInput(userInput)
          for {
            shardId <- zio.random.nextInt
            now <- zio.clock.instant.map(_.truncatedTo(ChronoUnit.MILLIS))
            export1 = UserExport(shardId, now, userId, user)
            export2 = UserExport(shardId, now.plusSeconds(1), userId, user)
            _ <- runTx(QueueDao.push(export1))
            _ <- runTx(QueueDao.push(export2))
            saved <- runTx(QueueDao.pull(shardId, 10))
          } yield assert(saved)(equalTo(Seq(export1, export2)))
        }
      },
      testM("remove offers from queue") {
        checkNM(1)(UserGen.anyUser, UserGen.anyUserId) { (userInput, userId) =>
          val user = User.fromUserInput(userInput)
          for {
            shardId <- zio.random.nextInt
            timestamp <- zio.clock.instant
            export = UserExport(shardId, timestamp, userId, user)
            export2 = UserExport(shardId, timestamp.plusSeconds(1), userId, user)
            _ <- runTx(QueueDao.push(export :: export2 :: Nil))
            _ <- runTx(QueueDao.remove(export :: export2 :: Nil))
            saved <- runTx(QueueDao.pull(shardId, 10))
          } yield assert(saved)(isEmpty)
        }
      },
      testM("push multiple users") {
        checkNM(1)(UserGen.anyUser, UserGen.anyUserId) { (userInput, userId) =>
          val user = User.fromUserInput(userInput)
          for {
            shardId <- zio.random.nextInt
            now <- zio.clock.instant.map(_.truncatedTo(ChronoUnit.MILLIS))
            export1 = UserExport(shardId, now, userId, user)
            export2 = UserExport(shardId, now.plusSeconds(1), userId, user)
            _ <- runTx(QueueDao.push(List(export1, export2)))
            saved <- runTx(QueueDao.pull(shardId, 10))
          } yield assert(saved)(equalTo(Seq(export1, export2)))
        }
      },
      testM("get queue metrics") {
        checkNM(1)(UserGen.anyUser, UserGen.anyUserId) { (userInput, userId) =>
          val user = User.fromUserInput(userInput)
          for {
            shardId <- zio.random.nextInt
            before <- runTx(QueueDao.getQueueMetrics)
            time = Instant.ofEpochSecond(0)
            export = UserExport(shardId, time, userId, user)
            _ <- runTx(QueueDao.push(export))
            after <- runTx(QueueDao.getQueueMetrics)
          } yield assert(after.size)(equalTo(before.size + 1)) && assert(after.oldestTimestamp)(isSome(equalTo(time)))
        }
      }
    )
  }
}
