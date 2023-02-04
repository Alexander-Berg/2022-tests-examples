package ru.yandex.vertis.general.feed.storage.test

import cats.data.NonEmptyList
import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.feed.model.testkit.{FeedStatisticsGen, NamespaceIdGen}
import ru.yandex.vertis.general.feed.storage.FeedStatisticsDao
import ru.yandex.vertis.general.feed.storage.postgresql.PgFeedStatisticsDao
import zio.ZIO
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

object PgFeedStatisticsDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgFeedStatisticsDao")(
      testM("insert feed statistics") {
        checkNM(1)(
          SellerGen.anySellerId.noShrink,
          NamespaceIdGen.anyNamespaceId().noShrink,
          FeedStatisticsGen.any.noShrink,
          FeedStatisticsGen.any.noShrink
        ) { (sellerId, namespaceId, feedStatistics1, feedStatistics2) =>
          for {
            dao <- ZIO.service[FeedStatisticsDao.Service]
            _ <- dao.createOrUpdate(sellerId, namespaceId, 0, feedStatistics1).transactIO
            _ <- dao.createOrUpdate(sellerId, namespaceId, 1, feedStatistics2).transactIO
            saved <- dao.get(sellerId, namespaceId, NonEmptyList.of(0L, 1L, 2L)).transactIO
          } yield assert(saved)(equalTo(Map(0L -> feedStatistics1, 1L -> feedStatistics2)))
        }
      }
    ) @@ sequential)
      .provideCustomLayerShared {
        TestPostgresql.managedTransactor >+> PgFeedStatisticsDao.live
      }
  }
}
