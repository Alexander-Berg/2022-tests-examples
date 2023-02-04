package ru.yandex.vertis.general.category_matcher.storage.test

import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.category_matcher.model.Namespace
import ru.yandex.vertis.general.category_matcher.model.testkit.CategoryIdGen
import ru.yandex.vertis.general.category_matcher.storage.TagValueDao
import ru.yandex.vertis.general.category_matcher.storage.postgresql.PgTagValueDao
import zio.ZIO
import zio.test.Assertion._
import zio.test.TestAspect.{sequential, shrinks}
import zio.test._

object PgTagValueDaoSpec extends DefaultRunnableSpec {

  val genTag = Gen.alphaNumericStringBounded(10, 20)
  val genTagValue = Gen.alphaNumericStringBounded(10, 20)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgTagValueDao")(
      testM("возвращает None, если тега нет") {
        checkNM(1)(genTag, genTagValue, genTagValue) { (tag, tagValue1, tagValue2) =>
          for {
            dao <- ZIO.service[TagValueDao.Service]
            saved <- dao.get(Namespace.Avito, List((tag, tagValue1), (tag, tagValue2))).transactIO
          } yield assert(saved)(isEmpty)
        }
      },
      testM("возвращает теги со счетчиками") {
        checkNM(1)(genTag, genTagValue, genTagValue) { (tag, tagValue1, tagValue2) =>
          val counters = Map((tag, tagValue1) -> 10, (tag, tagValue2) -> 5)
          for {
            dao <- ZIO.service[TagValueDao.Service]
            _ <- dao.createOrUpdate(Namespace.Avito, counters).transactIO
            saved <- dao.get(Namespace.Avito, counters.keys.toList).transactIO
          } yield assert(saved)(equalTo(counters))
        }
      },
      testM("обновляет счетчик") {
        checkNM(1)(genTag, genTagValue, genTagValue, genTagValue) { (tag, tagValue1, tagValue2, tagValue3) =>
          val counters1 = Map((tag, tagValue1) -> 10, (tag, tagValue2) -> 5)
          val counters2 = Map((tag, tagValue2) -> 7, (tag, tagValue3) -> 5)
          val resultCounters = counters1 ++ counters2
          for {
            dao <- ZIO.service[TagValueDao.Service]
            _ <- dao.createOrUpdate(Namespace.Avito, counters1).transactIO
            _ <- dao.createOrUpdate(Namespace.Avito, counters2).transactIO
            saved <- dao.get(Namespace.Avito, resultCounters.keys.toList).transactIO
          } yield assert(saved)(equalTo(resultCounters))
        }
      },
      testM("удаляет категорию") {
        checkNM(1)(genTag, genTagValue, genTagValue) { (tag, tagValue1, tagValue2) =>
          val counters = Map((tag, tagValue1) -> 10, (tag, tagValue2) -> 5)
          for {
            dao <- ZIO.service[TagValueDao.Service]
            _ <- dao.createOrUpdate(Namespace.Avito, counters).transactIO
            _ <- dao.delete(Namespace.Avito, List((tag, tagValue1))).transactIO
            saved <- dao.get(Namespace.Avito, counters.keys.toList).transactIO
          } yield assert(saved)(equalTo(Map((tag, tagValue2) -> 5)))
        }
      },
      testM("находит категории по подстроке") {
        checkNM(1)(genTag) { tag =>
          val tagValues =
            (((0 until 10).map(i => s"Suggest_$i")) ++ ((0 until 10).map(i => s"other_$i"))).toList
          for {
            dao <- ZIO.service[TagValueDao.Service]
            _ <- dao.createOrUpdate(Namespace.Avito, tagValues.map(tv => (tag, tv) -> 1).toMap).transactIO
            list <- dao.find(Namespace.Avito, tag, "sug", 11).transactIO
          } yield assert(list)(hasSize(equalTo(10))) &&
            assert(list)(forall(startsWithString("Suggest")))
        }
      }
    ) @@ sequential @@ shrinks(0))
      .provideCustomLayerShared {
        TestPostgresql.managedTransactor >+> PgTagValueDao.live
      }
  }
}
