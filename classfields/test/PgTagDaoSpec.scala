package ru.yandex.vertis.general.category_matcher.storage.test

import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.category_matcher.model.Namespace
import ru.yandex.vertis.general.category_matcher.storage.TagDao
import ru.yandex.vertis.general.category_matcher.storage.postgresql.PgTagDao
import zio.ZIO
import zio.test.Assertion._
import zio.test.TestAspect.{sequential, shrinks}
import zio.test._

object PgTagDaoSpec extends DefaultRunnableSpec {

  val genTag = Gen.alphaNumericStringBounded(10, 20)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgTagDao")(
      testM("возвращает None, если тега нет") {
        checkNM(1)(genTag, genTag) { (tag1, tag2) =>
          for {
            dao <- ZIO.service[TagDao.Service]
            saved <- dao.get(Namespace.Avito, List(tag1, tag2)).transactIO
          } yield assert(saved)(isEmpty)
        }
      },
      testM("возвращает теги со счетчиками") {
        checkNM(1)(genTag, genTag) { (tag1, tag2) =>
          val counters = Map(tag1 -> 10, tag2 -> 5)
          for {
            dao <- ZIO.service[TagDao.Service]
            _ <- dao.createOrUpdate(Namespace.Avito, counters).transactIO
            saved <- dao.get(Namespace.Avito, counters.keys.toList).transactIO
          } yield assert(saved)(equalTo(counters))
        }
      },
      testM("обновляет счетчик") {
        checkNM(1)(genTag, genTag, genTag) { (tag1, tag2, tag3) =>
          val counters1 = Map(tag1 -> 10, tag2 -> 5)
          val counters2 = Map(tag2 -> 7, tag3 -> 5)
          val resultCounters = counters1 ++ counters2
          for {
            dao <- ZIO.service[TagDao.Service]
            _ <- dao.createOrUpdate(Namespace.Avito, counters1).transactIO
            _ <- dao.createOrUpdate(Namespace.Avito, counters2).transactIO
            saved <- dao.get(Namespace.Avito, resultCounters.keys.toList).transactIO
          } yield assert(saved)(equalTo(resultCounters))
        }
      },
      testM("удаляет категорию") {
        checkNM(1)(genTag, genTag) { (tag1, tag2) =>
          val counters = Map(tag1 -> 10, tag2 -> 5)
          for {
            dao <- ZIO.service[TagDao.Service]
            _ <- dao.createOrUpdate(Namespace.Avito, counters).transactIO
            _ <- dao.delete(Namespace.Avito, List(tag1)).transactIO
            saved <- dao.get(Namespace.Avito, counters.keys.toList).transactIO
          } yield assert(saved)(equalTo(Map(tag2 -> 5)))
        }
      },
      testM("находит категории по подстроке") {
        val tags =
          (((0 until 10).map(i => s"Suggest_$i")) ++ ((0 until 10).map(i => s"other_$i"))).toList
        for {
          dao <- ZIO.service[TagDao.Service]
          _ <- dao.createOrUpdate(Namespace.Avito, tags.map(_ -> 1).toMap).transactIO
          list <- dao.find(Namespace.Avito, "sug", 11).transactIO
        } yield assert(list)(hasSize(equalTo(10))) &&
          assert(list)(forall(startsWithString("Suggest")))
      }
    ) @@ sequential @@ shrinks(0))
      .provideCustomLayerShared {
        TestPostgresql.managedTransactor >+> PgTagDao.live
      }
  }
}
