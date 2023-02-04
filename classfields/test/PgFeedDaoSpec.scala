package ru.yandex.vertis.general.feed.storage.test

import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.feed.model.testkit.{FeedSettingsGen, NamespaceIdGen}
import ru.yandex.vertis.general.feed.model.{Feed, FeedStatus}
import ru.yandex.vertis.general.feed.storage.FeedDao
import ru.yandex.vertis.general.feed.storage.postgresql.PgFeedDao
import zio.ZIO
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

import java.time.Instant

object PgFeedDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgFeedDao")(
      testM("insert feed") {
        checkNM(1)(
          SellerGen.anySellerId.noShrink,
          NamespaceIdGen.anyNamespaceId("1").noShrink,
          FeedSettingsGen.anyFeedSettings.noShrink,
          NamespaceIdGen.anyNamespaceId("2").noShrink,
          FeedSettingsGen.anyFeedSettings.noShrink,
          FeedSettingsGen.anyFeedSettings.noShrink
        ) { (sellerId, namespaceId, feedSettings, namespaceId2, feedSettings2, feedSettings3) =>
          val inserted = Feed(feedSettings, FeedStatus.Active, 1, Instant.now)
          val inserted2 = Feed(feedSettings2, FeedStatus.Active, 1123, Instant.now)
          val inserted3 = Feed(feedSettings3, FeedStatus.Removed, 46827, Instant.now)
          for {
            dao <- ZIO.service[FeedDao.Service]
            _ <- dao.createOrUpdate(sellerId, namespaceId, inserted).transactIO
            _ <- dao.createOrUpdate(sellerId, namespaceId2, inserted2).transactIO
            saved <- dao.get(sellerId, namespaceId).transactIO
            saved2 <- dao.get(sellerId, namespaceId2).transactIO
            _ <- dao.createOrUpdate(sellerId, namespaceId2, inserted3).transactIO
            saved3 <- dao.get(sellerId, namespaceId2).transactIO
          } yield {
            assert(saved)(isSome(equalTo(inserted))) &&
            assert(saved2)(isSome(equalTo(inserted2))) &&
            assert(saved3)(isSome(equalTo(inserted3)))
          }
        }
      }
    ) @@ sequential)
      .provideCustomLayerShared {
        TestPostgresql.managedTransactor >+> PgFeedDao.live
      }
  }
}
