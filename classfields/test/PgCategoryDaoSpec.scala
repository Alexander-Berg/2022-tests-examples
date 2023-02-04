package ru.yandex.vertis.general.category_matcher.storage.test

import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.category_matcher.model.testkit.CategoryIdGen
import ru.yandex.vertis.general.category_matcher.model.{CategoryId, Namespace}
import ru.yandex.vertis.general.category_matcher.storage.CategoryDao
import ru.yandex.vertis.general.category_matcher.storage.CategoryDao.CategoryIdWithCount
import ru.yandex.vertis.general.category_matcher.storage.postgresql.PgCategoryDao
import zio.ZIO
import zio.test.Assertion._
import zio.test.TestAspect.{sequential, shrinks}
import zio.test._

object PgCategoryDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgCategoryDao")(
      testM("возвращает None, если категории нет") {
        checkNM(1)(CategoryIdGen.any) { category =>
          for {
            dao <- ZIO.service[CategoryDao.Service]
            saved <- dao.get(Namespace.Avito, category).transactIO
          } yield assert(saved)(isNone)
        }
      },
      testM("возвращает категорию со счетчиком") {
        checkNM(1)(CategoryIdGen.any) { category =>
          for {
            dao <- ZIO.service[CategoryDao.Service]
            _ <- dao.createOrUpdate(Namespace.Avito, category, 10).transactIO
            saved <- dao.get(Namespace.Avito, category).transactIO
          } yield assert(saved)(isSome(equalTo(CategoryIdWithCount(category, 10))))
        }
      },
      testM("обновляет счетчик") {
        checkNM(1)(CategoryIdGen.any) { category =>
          for {
            dao <- ZIO.service[CategoryDao.Service]
            _ <- dao.createOrUpdate(Namespace.Avito, category, 10).transactIO
            _ <- dao.createOrUpdate(Namespace.Avito, category, 20).transactIO
            saved <- dao.get(Namespace.Avito, category).transactIO
          } yield assert(saved)(isSome(equalTo(CategoryIdWithCount(category, 20))))
        }
      },
      testM("удаляет категорию") {
        checkNM(1)(CategoryIdGen.any) { category =>
          for {
            dao <- ZIO.service[CategoryDao.Service]
            _ <- dao.createOrUpdate(Namespace.Avito, category, 10).transactIO
            _ <- dao.delete(Namespace.Avito, category).transactIO
            saved <- dao.get(Namespace.Avito, category).transactIO
          } yield assert(saved)(isNone)
        }
      },
      testM("находит категории по подстроке") {
        val categories =
          (((0 until 10).map(i => s"Suggest_$i")) ++ ((0 until 10).map(i => s"other_$i"))).map(CategoryId.apply).toList

        for {
          dao <- ZIO.service[CategoryDao.Service]
          _ <- ZIO.foreach_(categories)(c => dao.createOrUpdate(Namespace.Avito, c, 1).transactIO)
          list <- dao.find(Namespace.Avito, "sug", 11).transactIO
        } yield assert(list)(hasSize(equalTo(10))) &&
          assert(list.map(_.categoryId))(forall(startsWithString("Suggest")))
      }
    ) @@ sequential @@ shrinks(0))
      .provideCustomLayerShared {
        TestPostgresql.managedTransactor >+> PgCategoryDao.live
      }
  }
}
