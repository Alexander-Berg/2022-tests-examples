package auto.plus_me.storage.test

import auto.plus_me.storage.OfferWatchDao
import auto.plus_me.storage.pg.PgOfferWatchDao
import common.db.migrations.doobie.DoobieMigrations
import common.zio.doobie.testkit.TestPostgresql
import zio._
import zio.magic._
import zio.test.TestAspect._
import zio.test._

object PgOfferWatchDaoSpec extends DefaultRunnableSpec {

  def spec = {
    suite("PgOfferWatchDao")(
      testM("сохранение ватча") {
        for {
          dao <- ZIO.service[OfferWatchDao.Service]
          now <- clock.instant
          _ <- dao.watchOffer("1", 2, now)
          res <- dao.getWatchesOverDeadline(now, 5)
        } yield assertTrue(res.size == 1, res.head.offerId == "1", res.head.userId == 2, res.head.deadline == now)
      },
      testM("удаление ватча") {
        for {
          dao <- ZIO.service[OfferWatchDao.Service]
          now <- clock.instant
          _ <- dao.watchOffer("1", 2, now)
          _ <- dao.unwatchOffer("1")
          res <- dao.getWatchesOverDeadline(now, 5)
        } yield assertTrue(res.isEmpty)
      },
      testM("Получене вотчей по дедлайну") {
        for {
          dao <- ZIO.service[OfferWatchDao.Service]
          now <- clock.instant
          _ <- dao.watchOffer("1", 2, now.minusSeconds(10))
          _ <- dao.watchOffer("2", 2, now.plusSeconds(10))
          res1 <- dao.getWatchesOverDeadline(now.minusSeconds(11), 5)
          res2 <- dao.getWatchesOverDeadline(now, 5)
          res3 <- dao.getWatchesOverDeadline(now.plusSeconds(10), 5)
        } yield assertTrue(res1.isEmpty, res2.size == 1, res3.size == 2, res2.head.offerId == "1")
      },
      testM("unwatch не падает, если оффер не вотчится") {
        for {
          dao <- ZIO.service[OfferWatchDao.Service]
          _ <- dao.unwatchOffer("23")
          _ <- dao.unwatchOffer("23")
        } yield assertCompletes
      }
    ) @@ before(DoobieMigrations.dropAll *> DoobieMigrations.migrate("test")) @@ sequential
  }.injectCustomShared(
    TestPostgresql.managedTransactor,
    PgOfferWatchDao.live
  )
}
