package ru.yandex.vertis.general.feed.logic.test

import common.zio.doobie.testkit.TestPostgresql
import general.users.model.{BanReason, ModerationInfo, ModerationStatus, OffersPublishModeration, UserView}
import ru.yandex.vertis.general.common.errors.FatalFeedErrors
import ru.yandex.vertis.general.common.model.pagination.LimitOffset
import ru.yandex.vertis.general.common.model.user.SellerId
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.feed.logic.FeedLoader.DownloadedFeed
import ru.yandex.vertis.general.feed.logic._
import ru.yandex.vertis.general.feed.model.testkit.NamespaceIdGen
import ru.yandex.vertis.general.feed.model.{
  FatalErrorInfo,
  FeedLoaderTask,
  FeedSource,
  FeedTask,
  NamespaceId,
  RegularFeed,
  TaskStatus
}
import ru.yandex.vertis.general.feed.logic.test.FeedTestInitUtils._
import ru.yandex.vertis.general.feed.storage.postgresql._
import ru.yandex.vertis.general.users.testkit.TestUserService
import ru.yandex.vertis.general.users.testkit.TestUserService.UpdateUsers
import common.zio.logging.Logging
import zio.clock.Clock
import zio.random.Random
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test._
import zio.{Has, IO, URIO, ZIO, ZLayer}

import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

object FeedLoaderManagerSpec extends DefaultRunnableSpec {

  val path = Paths.get("/tmp/feed.xml")
  val url = "http://ya.ru"
  val url2 = "http://o.yandex.ru"
  val url0 = "http://ya0.ru"
  val url3 = "http://yandex.ru"
  val badUrl = "http://avito.ru"
  val badUrl2 = "http://cian.ru"
  val refreshPeriod = 1.hour

  private def createEmptyUser(sellerId: SellerId): URIO[Has[UpdateUsers], Unit] =
    sellerId match {
      case SellerId.UserId(id) => TestUserService.addUser(UserView(id = id))
      case SellerId.StoreId(_) => ZIO.unit
      case SellerId.AggregatorId(_) => ZIO.unit
    }

  private def makeFakeFeedUrl(remoteUrl: String): String = {
    s"http://s3.yandex.net/${remoteUrl.hashCode}"
  }

  private def assertTasksEqual(expected: Seq[FeedLoaderTask], actual: Seq[FeedTask]): TestResult = {
    val expectedNormalized = expected.map(task => task.copy(remoteUrl = makeFakeFeedUrl(task.remoteUrl)))

    val actualNormalized = actual.map(tasks =>
      FeedLoaderTask(
        tasks.sellerId,
        tasks.namespaceId,
        tasks.feedSource,
        tasks.feedVersion,
        tasks.url.getOrElse("remote url not defined"),
        lastFetchedAt = None // to be consistent with the original load task where no time is set
      )
    )

    assert(expectedNormalized)(hasSameElements(actualNormalized))
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("FeedLoaderManager")(
      testM("Создает задачи на каждое скачивание") {
        checkNM(1)(
          SellerGen.anySellerId.noShrink,
          NamespaceIdGen.anyNamespaceId("1").noShrink,
          NamespaceIdGen.anyNamespaceId("2").noShrink
        ) { (sellerId, namespaceId1, namespaceId2) =>
          for {
            _ <- createEmptyUser(sellerId)
            _ <- FeedManager.updateFeed(sellerId, namespaceId1, RegularFeed(url))
            _ <- FeedManager.updateFeed(sellerId, namespaceId2, RegularFeed(url0))
            tasksNm1 = Seq(
              FeedLoaderTask(sellerId, namespaceId1, FeedSource.Feed, 0, url, None),
              FeedLoaderTask(sellerId, namespaceId1, FeedSource.Feed, 0, url2, None)
            )
            tasksNm2 = Seq(
              FeedLoaderTask(sellerId, namespaceId2, FeedSource.Feed, 0, url2, None),
              FeedLoaderTask(sellerId, namespaceId2, FeedSource.Feed, 0, url3, None)
            )
            _ <- ZIO.foreach_(tasksNm1 ++ tasksNm2)(FeedLoaderManager.processTask(_, refreshPeriod))
            returnTasks1 <- TaskManager.list(sellerId, namespaceId1, LimitOffset(10, 0))
            returnTasks2 <- TaskManager.list(sellerId, namespaceId2, LimitOffset(10, 0))
          } yield assertTasksEqual(tasksNm1, returnTasks1) && assertTasksEqual(tasksNm2, returnTasks2)
        }
      },
      testM("Схлапывает дубли") {
        checkNM(1)(
          SellerGen.anySellerId.noShrink,
          NamespaceIdGen.anyNamespaceId("1").noShrink,
          NamespaceIdGen.anyNamespaceId("2").noShrink
        ) { (sellerId, namespaceId1, namespaceId2) =>
          for {
            _ <- createEmptyUser(sellerId)
            _ <- FeedManager.updateFeed(sellerId, namespaceId1, RegularFeed(url))
            _ <- FeedManager.updateFeed(sellerId, namespaceId2, RegularFeed(url))
            _ <- FeedLoaderManager
              .processTask(FeedLoaderTask(sellerId, namespaceId1, FeedSource.Feed, 0, url, None), refreshPeriod)
            _ <- FeedLoaderManager
              .processTask(FeedLoaderTask(sellerId, namespaceId1, FeedSource.Feed, 0, url, None), refreshPeriod)
            _ <- FeedLoaderManager
              .processTask(FeedLoaderTask(sellerId, namespaceId2, FeedSource.Feed, 0, url, None), refreshPeriod)
            tasks1 <- TaskManager.list(sellerId, namespaceId1, LimitOffset(10, 0))
            tasks2 <- TaskManager.list(sellerId, namespaceId2, LimitOffset(10, 0))
          } yield assert(tasks1)(hasSize(equalTo(1))) && assert(tasks2)(hasSize(equalTo(1)))
        }
      },
      testM("Сообщает об ошибках, если были проблемы со скачиванием фида") {
        checkNM(1)(
          SellerGen.anySellerId.noShrink,
          NamespaceIdGen.anyNamespaceId("1").noShrink,
          NamespaceIdGen.anyNamespaceId("2").noShrink
        ) { (sellerId, namespaceId1, namespaceId2) =>
          for {
            _ <- createEmptyUser(sellerId)
            _ <- FeedManager.updateFeed(sellerId, namespaceId1, RegularFeed(badUrl))
            _ <- FeedManager.updateFeed(sellerId, namespaceId2, RegularFeed(badUrl))
            _ <- FeedLoaderManager
              .processTask(FeedLoaderTask(sellerId, namespaceId1, FeedSource.Feed, 0, badUrl, None), refreshPeriod)
            _ <- FeedLoaderManager
              .processTask(FeedLoaderTask(sellerId, namespaceId2, FeedSource.Feed, 0, badUrl, None), refreshPeriod)
            tasks1 <- TaskManager.list(sellerId, namespaceId1, LimitOffset(10, 0))
            tasks2 <- TaskManager.list(sellerId, namespaceId2, LimitOffset(10, 0))
          } yield assert(tasks1)(hasSize(equalTo(1))) &&
            assert(tasks1 ++ tasks2)(
              forall[FeedTask](
                hasField("status", (_: FeedTask).taskStatus, equalTo(TaskStatus.Failed: TaskStatus)) &&
                  hasField("fatalErrorsInfo", (_: FeedTask).fatalErrorsInfo, isNonEmpty)
              )
            )
        }
      },
      testM("Падает, если были проблемы с сохранением фида") {
        checkNM(1)(SellerGen.anySellerId.noShrink, NamespaceIdGen.anyNamespaceId().noShrink) {
          (sellerId, namespaceId) =>
            for {
              _ <- createEmptyUser(sellerId)
              _ <- FeedManager.updateFeed(sellerId, namespaceId, RegularFeed(badUrl2))
              _ <- FeedLoaderManager
                .processTask(FeedLoaderTask(sellerId, namespaceId, FeedSource.Feed, 0, badUrl2, None), refreshPeriod)
                .flip
            } yield assertCompletes
        }
      },
      testM("Сообщает о бане пользователю") {
        checkNM(1)(SellerGen.anyUserId.noShrink, NamespaceIdGen.anyNamespaceId().noShrink) { (userId, namespaceId) =>
          for {
            _ <- TestUserService.addUser(
              UserView(
                id = userId.id,
                moderationInfo = Some(
                  ModerationInfo(
                    offersPublish = Some(
                      OffersPublishModeration(
                        status = ModerationStatus.BANNED,
                        banReasons = List(BanReason(code = "1", title = "ban", textHtml = "ban"))
                      )
                    )
                  )
                )
              )
            )
            _ <- FeedManager.updateFeed(userId, namespaceId, RegularFeed(url))
            _ <- FeedLoaderManager
              .processTask(FeedLoaderTask(userId, namespaceId, FeedSource.Feed, 0, url, None), refreshPeriod)
            _ <- FeedLoaderManager
              .processTask(FeedLoaderTask(userId, namespaceId, FeedSource.Feed, 0, url2, None), refreshPeriod)
            tasks <- TaskManager.list(userId, namespaceId, LimitOffset(10, 0))
          } yield assert(tasks)(hasSize(equalTo(2))) &&
            assert(tasks.headOption)(
              isSome(
                hasField("status", (_: FeedTask).taskStatus, equalTo(TaskStatus.Failed: TaskStatus)) &&
                  hasField(
                    "fatalErrorMessage",
                    (_: FeedTask).fatalErrorsInfo,
                    exists[FatalErrorInfo](
                      hasField("descriptionCode", _.descriptionCode, equalTo(FatalFeedErrors.sellerBannedCode))
                    )
                  )
              )
            )
        }
      }
    ) @@ sequential).provideCustomLayerShared {
      val transactor = TestPostgresql.managedTransactor

      val feedLoader = ZLayer.succeed {
        new FeedLoader.Service {
          override def downloadAndSave(
              remoteUrl: String,
              sellerId: SellerId,
              namespaceId: NamespaceId): IO[FeedLoader.Error, DownloadedFeed] = {
            remoteUrl match {
              case `badUrl` => ZIO.fail(FeedLoader.DownloadError(new RuntimeException("cannot download")))
              case `badUrl2` => ZIO.fail(FeedLoader.SaveError(new RuntimeException("cannot save")))
              case _ =>
                ZIO.succeed(DownloadedFeed(makeFakeFeedUrl(remoteUrl), remoteUrl.hashCode.toString))
            }
          }
        }
      }

      val feedLoaderManager =
        (Clock.live ++ Random.live ++ Logging.live ++ feedDaos ++ feedLoader ++ TestUserService.layer) >+> FeedLoaderManager.live

      Random.live ++ transactor ++ feedManager ++ taskManager ++ feedLoaderManager
    }
  }
}
