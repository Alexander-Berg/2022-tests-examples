package ru.yandex.vertis.general.category_matcher.storage.test

import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.category_matcher.model.{CategoryId, Filters, Match, Namespace}
import ru.yandex.vertis.general.category_matcher.model.testkit.MatchGen
import ru.yandex.vertis.general.category_matcher.storage.MatchDao
import ru.yandex.vertis.general.category_matcher.storage.postgresql.PgMatchDao
import ru.yandex.vertis.general.common.model.pagination.LimitOffset
import zio.ZIO
import zio.test.Assertion._
import zio.test.TestAspect.{sequential, shrinks}
import zio.test.{assert, checkNM, suite, testM, DefaultRunnableSpec, ZSpec}

object PgMatchDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgMatchDao")(
      testM("добавляет матчинг") {
        checkNM(1)(MatchGen.any) { `match` =>
          for {
            dao <- ZIO.service[MatchDao.Service]
            _ <- dao.createOrUpdate(Namespace.Avito, `match`).transactIO
            saved <- dao.get(Namespace.Avito, `match`.key).transactIO
          } yield assert(saved)(isSome(equalTo(`match`)))
        }
      },
      testM("обновляет матчинг") {
        checkNM(1)(MatchGen.any) { `match` =>
          for {
            dao <- ZIO.service[MatchDao.Service]
            _ <- dao.createOrUpdate(Namespace.Avito, `match`).transactIO
            newMatch = `match`.copy(generalCategoryIds = List("abc", "def"))
            _ <- dao.createOrUpdate(Namespace.Avito, newMatch).transactIO
            saved <- dao.get(Namespace.Avito, `match`.key).transactIO
          } yield assert(saved)(isSome(equalTo(newMatch)))
        }
      },
      testM("удаляет матчинг") {
        checkNM(1)(MatchGen.any) { `match` =>
          for {
            dao <- ZIO.service[MatchDao.Service]
            _ <- dao.createOrUpdate(Namespace.Avito, `match`).transactIO
            _ <- dao.delete(Namespace.Avito, `match`.key).transactIO
            saved <- dao.get(Namespace.Avito, `match`.key).transactIO
          } yield assert(saved)(isNone)
        }
      },
      testM("возвращает листинг с пагинацией") {
        checkNM(1)(MatchGen.any) { `match` =>
          val matches = (0 until 30).map { i =>
            `match`.copy(key =
              `match`.key.copy(categoryId = CategoryId(s"list_${i % 2}"), tags = Map("tag" -> i.toString))
            )
          }

          for {
            dao <- ZIO.service[MatchDao.Service]
            _ <- ZIO.foreach_(matches)(m => dao.createOrUpdate(Namespace.Avito, m).transactIO)
            list1 <- dao.list(Namespace.Avito, Filters(None), LimitOffset(10, 0)).transactIO
            list2 <- dao.list(Namespace.Avito, Filters(None), LimitOffset(10, 10)).transactIO
          } yield assert(list1)(hasSize(equalTo(10))) && assert(list1)(hasSize(equalTo(10))) &&
            assert(list1.intersect(list2))(isEmpty)
        }
      },
      testM("возвращает листинг с фильтрацией и пагинацией") {
        checkNM(1)(MatchGen.any) { `match` =>
          val matches = (0 until 30).map { i =>
            `match`.copy(key =
              `match`.key.copy(categoryId = CategoryId(s"list_filter_${i % 2}"), tags = Map("tag" -> i.toString))
            )
          }

          for {
            dao <- ZIO.service[MatchDao.Service]
            _ <- ZIO.foreach_(matches)(m => dao.createOrUpdate(Namespace.Avito, m).transactIO)
            list <- dao.list(Namespace.Avito, Filters(Some(CategoryId("list_filter_1"))), LimitOffset(10, 0)).transactIO
          } yield assert(list)(hasSize(equalTo(10))) &&
            assert(list)(
              forall(hasField[Match, String]("key.categoryId", _.key.categoryId.categoryId, equalTo("list_filter_1")))
            )
        }
      },
      testM("возвращает счетчик с матчингами и фильтрацией") {
        checkNM(1)(MatchGen.any) { `match` =>
          val matches = (0 until 30).map { i =>
            `match`.copy(key =
              `match`.key.copy(categoryId = CategoryId(s"count_${i % 2}"), tags = Map("tag" -> i.toString))
            )
          }

          for {
            dao <- ZIO.service[MatchDao.Service]
            _ <- ZIO.foreach_(matches)(m => dao.createOrUpdate(Namespace.Avito, m).transactIO)
            count <- dao.count(Namespace.Avito, Filters(Some(CategoryId("count_1")))).transactIO
          } yield assert(count)(equalTo(15))
        }
      }
    ) @@ sequential @@ shrinks(0))
      .provideCustomLayerShared {
        TestPostgresql.managedTransactor >+> PgMatchDao.live
      }
  }
}
