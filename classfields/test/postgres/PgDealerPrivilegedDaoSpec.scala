package auto.dealers.dealer_pony.storage.postgres.test

import ru.auto.dealer_pony.palma.proto.palma.LoyalDealersEntry
import auto.dealers.dealer_pony.storage.dao.DealerPrivilegedDao
import auto.dealers.dealer_pony.storage.postgres.PgDealerPrivilegedDao
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import doobie._
import doobie.implicits._
import zio.interop.catz._
import zio.test.Assertion._
import zio.test.TestAspect.{after, beforeAll, failing, sequential}
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio._

object PgDealerPrivilegedDaoSpec extends DefaultRunnableSpec {

  val entries = List(
    LoyalDealersEntry(1, "first"),
    LoyalDealersEntry(2, "second")
  )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    val suit = suite("PgDealerPrivilegedDaoSpec")(
      replaceTest,
      doubleReplaceTest,
      duplicateReplaceTest
    ) @@
      beforeAll(dbInit) @@
      after(dbClean) @@
      sequential

    suit.provideCustomLayerShared(TestPostgresql.managedTransactor >+> PgDealerPrivilegedDao.live)
  }

  val replaceTest = testM("Replace test") {
    for {
      _ <- DealerPrivilegedDao.replace(entries)
      res <- selectEntries
    } yield assert(res)(hasSameElements(entries))
  }

  val doubleReplaceTest = testM("Double replace test") {
    for {
      _ <- DealerPrivilegedDao.replace(entries)
      _ <- DealerPrivilegedDao.replace(entries.tail)
      res <- selectEntries
    } yield assert(res)(hasSameElements(entries.tail))
  }

  val duplicateReplaceTest = testM("Duplicate replace test") {
    for {
      _ <- DealerPrivilegedDao.replace(entries.head :: entries)
      res <- selectEntries
    } yield assert(res)(hasSameElements(entries))
  }

  private val selectEntries = ZIO
    .service[Transactor[Task]]
    .flatMap { xa =>
      sql"SELECT dealer_id, comment FROM dealers_privileged"
        .query[(Long, String)]
        .to[List]
        .transact(xa)
        .map(_.map(entry => LoyalDealersEntry(entry._1, entry._2)))
    }

  private val dbInit: URIO[Has[doobie.Transactor[Task]], Unit] = ZIO
    .service[Transactor[Task]]
    .flatMap(InitSchema("/schema.sql", _))
    .orDie

  private val dbClean = ZIO
    .service[Transactor[Task]]
    .flatMap { xa =>
      sql"DELETE FROM dealers_privileged".update.run.transact(xa)
    }
}
