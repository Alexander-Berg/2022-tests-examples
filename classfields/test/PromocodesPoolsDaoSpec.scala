package auto.carfax.promo_dispenser.storage.test

import auto.carfax.promo_dispenser.storage.dao.PromocodesPoolsDao
import auto.carfax.promo_dispenser.storage.postgresql.PromocodesPoolsDaoImpl
import auto.carfax.promo_dispenser.storage.testkit.{Generators, Schema}
import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import zio.ZIO
import zio.test.TestAspect.{after, beforeAll, sequential, shrinks}
import zio.test._

object PromocodesPoolsDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PromocodesPoolsDao")(
      testM("read inserted promocodes") {
        checkM(Gen.listOf(Generators.anyPoolPromocode)) { promocodes =>
          for {
            dao <- ZIO.service[PromocodesPoolsDao.Service]
            _ <- dao.upsertPromocodes(promocodes).transactIO
            res <- ZIO.foreach(promocodes)(p => {
              dao.getPromocode(p.poolId, p.promocode).transactIO.map(savedP => p -> savedP)
            })
          } yield assertTrue(res.forall(p => p._2.contains(p._1)))
        }
      },
      testM("update existed promocodes") {
        checkM(Gen.listOf(Generators.notUsedPoolPromocode)) { promocodes =>
          for {
            dao <- ZIO.service[PromocodesPoolsDao.Service]
            _ <- dao.upsertPromocodes(promocodes).transactIO
            updatedPromocodes = promocodes.zipWithIndex.map { case (p, idx) =>
              p.copy(userId = Some(s"user:$idx"))
            }
            _ <- dao.upsertPromocodes(updatedPromocodes).transactIO
            resultPromocodes <- ZIO
              .foreach(promocodes)(p => {
                dao.getPromocode(p.poolId, p.promocode).transactIO
              })
              .map(_.flatten)
          } yield assertTrue(updatedPromocodes == resultPromocodes)
        }
      },
      testM("read user promocodes") {
        checkM(Gen.listOf(Generators.notUsedPoolPromocode), Gen.listOf(Generators.notUsedPoolPromocode)) {
          (promocodes1, promocodes2) =>
            for {
              dao <- ZIO.service[PromocodesPoolsDao.Service]
              _ <- Schema.cleanup
              _ <- dao.upsertPromocodes(promocodes1 ++ promocodes2).transactIO
              updatedPromocodes1 = promocodes1.map(_.copy(userId = Some(s"user:1")))
              _ <- dao.upsertPromocodes(updatedPromocodes1 ++ promocodes2).transactIO
              resultPromocodes <- dao.getUserPromocodes("user:1").transactIO
            } yield {
              assertTrue(updatedPromocodes1.size == resultPromocodes.size) &&
              assertTrue(updatedPromocodes1.forall(resultPromocodes.contains))
            }
        }
      },
      testM("count not used promocodes") {
        checkM(Gen.listOf(Generators.notUsedPoolPromocode), Gen.listOf(Generators.usedPoolPromocode)) {
          (notUsedPromocodes, usedPromocodes) =>
            for {
              dao <- ZIO.service[PromocodesPoolsDao.Service]
              _ <- Schema.cleanup
              _ <- dao.upsertPromocodes(notUsedPromocodes ++ usedPromocodes).transactIO
              notUsedCount <- dao.notUsedPromocodesCount().transactIO.map(_.toMap)
              notUsedMap = notUsedPromocodes.groupBy(_.poolId).map { case (poolId, listOfPromocodes) =>
                poolId -> (listOfPromocodes.size)
              }
            } yield assertTrue(notUsedMap == notUsedCount)
        }
      },
      testM("acquire promocode") {
        checkM(Gen.listOf(Generators.notUsedPoolPromocode)) { promocodes =>
          for {
            dao <- ZIO.service[PromocodesPoolsDao.Service]
            _ <- Schema.cleanup
            _ <- dao.upsertPromocodes(promocodes).transactIO
            acquiredPromocodes <- ZIO
              .foreach(promocodes)(p => {
                dao.acquirePromocode(p.poolId, "user:1").transactIO
              })
              .map(_.flatten)
          } yield assertTrue(promocodes.size == acquiredPromocodes.size) &&
            assertTrue(promocodes.forall(p => {
              acquiredPromocodes.exists(acquired => acquired.poolId == p.poolId && acquired.promocode == p.promocode)
            })) &&
            assertTrue(acquiredPromocodes.forall(_.userId.contains("user:1")))
        }
      },
      testM("cant acquire promocode") {
        checkM(Generators.usedPoolPromocode) { promocode =>
          for {
            dao <- ZIO.service[PromocodesPoolsDao.Service]
            _ <- dao.upsertPromocodes(List(promocode)).transactIO
            res <- dao.acquirePromocode(promocode.poolId, "user:1").transactIO
          } yield assertTrue(res.isEmpty)
        }
      }
    ) @@ beforeAll(Schema.init) @@ after(Schema.cleanup) @@ sequential @@ shrinks(0)).provideCustomLayerShared {
      TestPostgresql.managedTransactor(version = "12") >+> PromocodesPoolsDaoImpl.live
    }
  }
}
