package ru.yandex.vertis.general.feed.storage.test

import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.feed.model.testkit.NamespaceIdGen
import ru.yandex.vertis.general.feed.storage.OfferBatchesDao
import ru.yandex.vertis.general.feed.storage.postgresql.PgOfferBatchesDao
import zio.ZIO
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

object PgOfferBatchesDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgOfferBatchesDao")(
      testM("insert batches") {
        checkNM(1)(SellerGen.anySellerId.noShrink, NamespaceIdGen.anyNamespaceId().noShrink) {
          (sellerId, namespaceId) =>
            for {
              dao <- ZIO.service[OfferBatchesDao.Service]
              result1 <- dao.upsert(sellerId, namespaceId, 0, 0).transactIO
              result2 <- dao.upsert(sellerId, namespaceId, 0, 0).transactIO
            } yield assert(result1)(isTrue) && assert(result2)(isFalse)
        }
      }
    ) @@ sequential)
      .provideCustomLayerShared {
        TestPostgresql.managedTransactor >+> PgOfferBatchesDao.live
      }
  }
}
