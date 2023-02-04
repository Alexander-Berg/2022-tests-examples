package ru.yandex.vertis.general.feed.logic.test

import common.zio.clients.s3.S3Client
import common.zio.clients.s3.testkit.TestS3
import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.common.errors.FeedValidationErrors
import ru.yandex.vertis.general.common.model.pagination.LimitOffset
import ru.yandex.vertis.general.common.model.user.SellerId
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.feed.logic.TaskCleanupManager
import ru.yandex.vertis.general.feed.model.testkit.{FeedTaskGen, NamespaceIdGen}
import ru.yandex.vertis.general.feed.model.{ErrorLevel, FailedOffer, FeedStatistics, NamespaceId}
import ru.yandex.vertis.general.feed.storage.postgresql._
import ru.yandex.vertis.general.feed.storage.{FeedStatisticsDao, OfferBatchesDao, OfferErrorsDao, TaskDao}
import zio.ZIO
import zio.clock.Clock
import zio.random.Random
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test.TestAspect.{sequential, shrinks}
import zio.test._

object TaskCleanupManagerSpec extends DefaultRunnableSpec {

  private def feedError(externalId: String) =
    FailedOffer(FailedOffer.Key(externalId, 1), "", ErrorLevel.Error, "", "", FeedValidationErrors.addressNotFoundCode)

  val bucket = "test"

  private def filename(namespaceId: NamespaceId, taskId: Long) = s"general_one_shot/$taskId"

  private def assertHasData(sellerId: SellerId, namespaceId: NamespaceId, taskId: Long) =
    assertData(sellerId, namespaceId, taskId, true)

  private def assertHasNoData(sellerId: SellerId, namespaceId: NamespaceId, taskId: Long) =
    assertData(sellerId, namespaceId, taskId, false)

  private def assertData(sellerId: SellerId, namespaceId: NamespaceId, taskId: Long, exists: Boolean) = {
    val optionAssertion = if (exists) isSome(anything) else isNone
    val seqAssertion = if (exists) isNonEmpty else isEmpty
    for {
      feedStatisticsDao <- ZIO.service[FeedStatisticsDao.Service]
      offerBatchesDao <- ZIO.service[OfferBatchesDao.Service]
      taskDao <- ZIO.service[TaskDao.Service]
      offerErrorDao <- ZIO.service[OfferErrorsDao.Service]
      fileAssert <- S3Client.headObject(bucket, filename(namespaceId, taskId)).fold(_ => !exists, _ => exists)
      task <- taskDao.get(sellerId, namespaceId, taskId).transactIO
      taskStatistics <- feedStatisticsDao.get(sellerId, namespaceId, taskId).transactIO
      taskBatches <- offerBatchesDao.list(sellerId, namespaceId, taskId, LimitOffset(10, 0)).transactIO
      taskErrors <- offerErrorDao.list(sellerId, namespaceId, taskId, None, LimitOffset(10, 0)).transactIO
    } yield assert(task)(optionAssertion) && assert(taskStatistics)(optionAssertion) &&
      assert(taskBatches)(seqAssertion) && assert(taskErrors)(seqAssertion) &&
      assert(fileAssert)(isTrue)
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("TaskCleanupManager")(
      testM("Удаляет данные устаревших задач") {
        checkNM(1)(
          SellerGen.anySellerId.noShrink,
          NamespaceIdGen.anyNamespaceId("1").noShrink,
          FeedTaskGen.any.noShrink,
          NamespaceIdGen.anyNamespaceId("2").noShrink
        ) { (sellerId, namespaceId1, task, namespaceId2) =>
          for {
            feedStatisticsDao <- ZIO.service[FeedStatisticsDao.Service]
            offerBatchesDao <- ZIO.service[OfferBatchesDao.Service]
            taskDao <- ZIO.service[TaskDao.Service]
            offerErrorDao <- ZIO.service[OfferErrorsDao.Service]
            _ <- S3Client.createBucket(bucket)
            _ <- ZIO.foreach_(Seq(namespaceId1, namespaceId2)) { nmId =>
              ZIO.foreach_(0 until 3) { taskId =>
                val name = filename(nmId, taskId)
                val url = if (taskId == 0) {
                  "https://odna.co/test/file/talents_classified_65615672.xml"
                } else {
                  s"https://test.s3.yandex.net/$name"
                }
                val action = for {
                  _ <- taskDao.createOrUpdate(
                    task.copy(sellerId = sellerId, namespaceId = nmId, taskId = taskId, url = Some(url))
                  )
                  _ <- feedStatisticsDao.createOrUpdate(sellerId, nmId, taskId, FeedStatistics(1, 1, 0, 0))
                  _ <- offerBatchesDao.upsert(sellerId, nmId, taskId, 1)
                  _ <- offerErrorDao
                    .insertOrIgnore(sellerId, nmId, taskId, Seq(feedError("abc"), feedError("def")))
                } yield ()
                S3Client
                  .uploadContent(bucket, name, 1, "application/xml", ZStream.apply(1.toByte))
                  .when(taskId != 0)
                  .orDie *> action.transactIO
              }
            }
            _ <- TaskCleanupManager.cleanup(sellerId, namespaceId1, 1)
            testResult0 <- assertHasNoData(sellerId, namespaceId1, 0)
            testResult1 <- assertHasNoData(sellerId, namespaceId1, 1)
            testResult2 <- assertHasData(sellerId, namespaceId1, 2)
            _ <- ZIO.foreach(Seq.range(0, 3))(assertHasData(sellerId, namespaceId2, _)).map(_.reduce(_ && _))
          } yield testResult0 && testResult1 && testResult2
        }
      }
    ) @@ sequential @@ shrinks(0))
      .provideCustomLayerShared {
        val transactorV = TestPostgresql.managedTransactor
        val dao = {
          PgTaskDao.live ++ PgFeedStatisticsDao.live ++ PgOfferBatchesDao.live ++ PgOfferErrorsDao.live ++ PgTaskCleanerQueueDao.live
        }
        val s3 = TestS3.mocked
        val taskCleanupManager = (Clock.live ++ (transactorV >+> dao) ++ s3) >>> TaskCleanupManager.live
        Random.live ++ transactorV ++ (transactorV >+> dao) ++ taskCleanupManager ++ s3
      }
  }
}
