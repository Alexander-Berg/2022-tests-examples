package auto.carfax.promo_dispenser.logic.test

import auto.carfax.promo_dispenser.logic.PromocodesPoolsManager
import auto.carfax.promo_dispenser.model.PoolPromocode
import auto.carfax.promo_dispenser.storage.postgresql.{PromocodesPoolsDaoImpl, UsersDaoImpl}
import auto.carfax.promo_dispenser.storage.testkit.{Generators, Schema}
import common.zio.doobie.testkit.TestPostgresql
import zio.ZIO
import zio.test.TestAspect.{after, beforeAll, sequential, shrinks}
import zio.test._

object DefaultPromocodesPoolsManagerSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = (suite("PromocodesPoolsManager")(
    testM("acquire not used promocode") {
      checkM(Generators.notUsedPoolPromocode) { promocode =>
        for {
          manager <- ZIO.service[PromocodesPoolsManager.Service]
          _ <- Schema.cleanup
          _ <- manager.upsertPromocodes(List(promocode))
          acquired <- manager.acquirePromocodes("user:1", List(promocode.poolId))
          userPromocodes <- manager.getUserPromocodes("user:1")
        } yield assertTrue(acquired.size == 1) &&
          assertTrue(acquired == userPromocodes) &&
          assertTrue(acquired.head.poolId == promocode.poolId && acquired.head.promocode == promocode.promocode)
      }
    },
    testM("cant acquire twice") {
      checkM(
        Gen.const(PoolPromocode("pool1", "promocode1", None)),
        Gen.const(PoolPromocode("pool1", "promocode2", None))
      ) { (p1, p2) =>
        for {
          manager <- ZIO.service[PromocodesPoolsManager.Service]
          _ <- manager.upsertPromocodes(List(p1, p2))
          acquired <- manager.acquirePromocodes("user:1", List("pool1"))
          acquredTwice <- manager.acquirePromocodes("user:1", List("pool1"))
          userPromocodes <- manager.getUserPromocodes("user:1")
        } yield assertTrue(acquired.size == 1) &&
          assertTrue(acquired == acquredTwice) &&
          assertTrue(userPromocodes == acquired)
      }
    },
    testM("cant acquire a promocode when the pool of promocode is over") {
      checkM(
        Gen.const(PoolPromocode("pool1", "promocode1", Some("user:3"))),
        Gen.const(PoolPromocode("pool1", "promocode2", None))
      ) { (p1, p2) =>
        for {
          manager <- ZIO.service[PromocodesPoolsManager.Service]
          _ <- manager.upsertPromocodes(List(p1, p2))
          acquired <- manager.acquirePromocodes("user:1", List("pool1"))
          acquredForUser2 <- manager.acquirePromocodes("user:2", List("pool1"))
          userPromocodesForUser1 <- manager.getUserPromocodes("user:1")
          userPromocodesForUser2 <- manager.getUserPromocodes("user:2")
          userPromocodesForUser3 <- manager.getUserPromocodes("user:3")
        } yield assertTrue(acquired.size == 1) &&
          assertTrue(acquredForUser2.size == 0) &&
          assertTrue(userPromocodesForUser1.size == 1) &&
          assertTrue(userPromocodesForUser2.size == 0) &&
          assertTrue(userPromocodesForUser3.size == 1)
      }
    },
    testM("acquire several promocodes for several users") {
      checkM(Gen.listOf(Generators.notUsedPoolPromocode), Generators.anyUserId, Generators.anyUserId) {
        (promocodes, user1, user2) =>
          for {
            manager <- ZIO.service[PromocodesPoolsManager.Service]
            _ <- Schema.cleanup
            _ <- manager.upsertPromocodes(promocodes)
            (promocodesForUser1, promocodesForUser2) = promocodes.partition(_.poolId.hashCode % 2 == 0)
            _ <- manager.acquirePromocodes(user1, promocodesForUser1.map(_.poolId))
            _ <- manager.acquirePromocodes(user2, promocodesForUser2.map(_.poolId))
            user1Promocodes <- manager.getUserPromocodes(user1)
            user2Promocodes <- manager.getUserPromocodes(user2)
          } yield assertTrue(promocodesForUser1.forall(p => user1Promocodes.map(_.poolId).contains(p.poolId))) &&
            assertTrue(promocodesForUser2.forall(p => user2Promocodes.map(_.poolId).contains(p.poolId)))
      }
    },
    testM("some promocodes already exists") {
      checkM(Gen.listOf(Generators.notUsedPoolPromocode), Generators.anyUserId) { (promocodes, user) =>
        for {
          manager <- ZIO.service[PromocodesPoolsManager.Service]
          _ <- Schema.cleanup
          (notUsedPromocodes, alreadyUsedPromocodes) = promocodes.partition(_.poolId.hashCode % 2 == 0)
          _ <- manager.upsertPromocodes(notUsedPromocodes ++ alreadyUsedPromocodes.map(_.copy(userId = Some(user))))
          acquired <- manager.acquirePromocodes(user, promocodes.map(_.poolId))
          userPromocodes <- manager.getUserPromocodes(user)
        } yield assertTrue(acquired.size == userPromocodes.size && userPromocodes.size == promocodes.size) &&
          assertTrue(
            promocodes.forall(p => userPromocodes.exists(u => p.poolId == u.poolId && p.promocode == u.promocode))
          )
      }
    }
  ) @@ beforeAll(Schema.init) @@ after(Schema.cleanup) @@ sequential @@ shrinks(0)).provideCustomLayerShared {
    val tx = TestPostgresql.managedTransactor(version = "12")
    ((tx >+> (PromocodesPoolsDaoImpl.live ++ UsersDaoImpl.live)) >>> PromocodesPoolsManager.live) ++ tx
  }
}
