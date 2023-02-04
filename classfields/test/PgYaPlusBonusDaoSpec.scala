package auto.plus_me.storage.test

import auto.plus_me.domain.YaPlusBonus
import auto.plus_me.storage.YaPlusBonusDao
import auto.plus_me.storage.pg.PgYaPlusBonusDao
import common.db.migrations.doobie.DoobieMigrations
import common.models.finance.Money.Rubles
import common.zio.doobie.testkit.TestPostgresql
import zio.{NonEmptyChunk, ZIO}
import zio.magic._
import doobie.implicits._
import doobie.postgres.implicits.JavaTimeInstantMeta
import doobie.{Read, Transactor}
import zio._
import zio.interop.catz._

import zio.test.TestAspect._
import zio.test.{assertTrue, DefaultRunnableSpec}

import java.time.Instant

object PgYaPlusBonusDaoSpec extends DefaultRunnableSpec {

  def spec = {
    suite("PgUserStateDao")(
      testM("Чтение несуществующего бонуса") {
        for {
          dao <- ZIO.service[YaPlusBonusDao.Service]
          res <- dao.getBonusById("unknown")
        } yield assertTrue(res.isEmpty)
      },
      testM("Сохранить и прочитать бонус") {
        for {
          dao <- ZIO.service[YaPlusBonusDao.Service]
          bonus = YaPlusBonus("123", 92L, 2L, Rubles(200L), "123", YaPlusBonus.Queued, Instant.EPOCH)
          _ <- dao.saveBonus(bonus)
          res <- dao.getBonusById(bonus.uniqueId)
        } yield assertTrue(res.get == bonus)
      },
      testM("Перезаписать бонус") {
        for {
          dao <- ZIO.service[YaPlusBonusDao.Service]
          bonus = YaPlusBonus("123", 92L, 2L, Rubles(200L), "123", YaPlusBonus.Queued, Instant.EPOCH)
          _ <- dao.saveBonus(bonus)
          _ <- dao.saveBonus(bonus.copy(offerId = "321"))
          res <- dao.getBonusById(bonus.uniqueId)
        } yield assertTrue(res.get != bonus, res.get.offerId == "321")
      },
      testM("Сохранить батч и прочитать бонус") {
        for {
          dao <- ZIO.service[YaPlusBonusDao.Service]
          queued = YaPlusBonus("queued", 91L, 1L, Rubles(200L), "123", YaPlusBonus.Queued, Instant.EPOCH)
          granted = YaPlusBonus("granted", 92L, 2L, Rubles(200L), "321", YaPlusBonus.Granted, Instant.EPOCH)
          _ <- dao.saveBonuses(NonEmptyChunk(queued, granted))
          res <- dao.getBonusById(queued.uniqueId)
          res2 <- dao.getBonusById(granted.uniqueId)
        } yield assertTrue(res.get == queued) && assertTrue(res2.get == granted)
      },
      testM("Чтение бонусов в статусе Queued") {
        for {
          dao <- ZIO.service[YaPlusBonusDao.Service]
          queued = YaPlusBonus("queued", 91L, 1L, Rubles(200L), "123", YaPlusBonus.Queued, Instant.EPOCH)
          granted = YaPlusBonus("granted", 92L, 2L, Rubles(200L), "321", YaPlusBonus.Granted, Instant.EPOCH)
          _ <- dao.saveBonuses(NonEmptyChunk(queued, granted))
          res <- dao.getQueuedBonuses(5)
        } yield assertTrue(res.size == 1 && res.head == queued)
      },
      testM("Лимит работает при чтении Queued бонусов") {
        for {
          dao <- ZIO.service[YaPlusBonusDao.Service]
          _ <- dao.saveBonuses(
            NonEmptyChunk(
              YaPlusBonus("queued", 91L, 1L, Rubles(200L), "123", YaPlusBonus.Queued, Instant.EPOCH),
              YaPlusBonus("queued2", 91L, 1L, Rubles(200L), "123", YaPlusBonus.Queued, Instant.EPOCH),
              YaPlusBonus("queued3", 91L, 1L, Rubles(200L), "123", YaPlusBonus.Queued, Instant.EPOCH),
              YaPlusBonus("queued4", 91L, 1L, Rubles(200L), "123", YaPlusBonus.Queued, Instant.EPOCH)
            )
          )
          res <- dao.getQueuedBonuses(2)
        } yield assertTrue(res.size == 2)
      },
      testM("Чтение старых бонусов") {
        for {
          transactor <- ZIO.service[Transactor[Task]]
          dao <- ZIO.service[YaPlusBonusDao.Service]
          _ <- sql"""insert into plus_bonuses (uniq_id, yandex_puid, amount_rubles, status, status_update)
                 |values ('1', 1, 99, 'queued', timestamp '1970-01-01 00:00')""".stripMargin.update.run
            .transact(transactor)
          bonus <- dao.getBonusById("1")
        } yield assertTrue(bonus.nonEmpty, bonus.get.offerId == "", bonus.get.userId == 0)
      }
    ) @@ before(DoobieMigrations.dropAll *> DoobieMigrations.migrate("test")) @@ sequential
  }.injectCustomShared(
    TestPostgresql.managedTransactor,
    PgYaPlusBonusDao.live
  )
}
