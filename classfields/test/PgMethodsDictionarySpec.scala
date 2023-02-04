package infra.profiler_collector.storage.test

import common.db.migrations.doobie.DoobieMigrations
import infra.profiler_collector.storage.{MethodsDictionary, PgMethodsDictionary}
import zio.magic._
import zio.test._

object PgMethodsDictionarySpec extends DefaultRunnableSpec {

  def spec =
    suite("PgMethodsDictionary")(
      testM("save & load") {
        for {
          _ <- DoobieMigrations.migrate()
          saveResult <- MethodsDictionary(_.saveMethods(Set("a", "b")))
          saveResult2 <- MethodsDictionary(_.saveMethods(Set("a", "c")))
          getResult <- MethodsDictionary(_.getMethods(saveResult.values.toSet))
        } yield assertTrue(saveResult("a") == saveResult2("a")) &&
          assertTrue(saveResult("a") != saveResult2("c")) &&
          assertTrue(getResult == saveResult.map(p => p._2 -> p._1))
      }
    ).injectCustom(
      PgMethodsDictionary.layer,
      common.zio.doobie.testkit.TestPostgresql.managedTransactor
    )
}
