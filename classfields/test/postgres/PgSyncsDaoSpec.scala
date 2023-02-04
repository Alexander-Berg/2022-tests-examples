package postgres

import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import doobie._
import doobie.implicits._
import auto.dealers.amoyak.storage.postgres.PgSyncsDao
import zio.{Task, ZIO}
import zio.interop.catz._
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.test.Assertion._
import zio.test.TestAspect._

object PgSyncsDaoSpec extends DefaultRunnableSpec {
  val clientId = 1

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgSyncsDao")(
      testM("upsert should insert new row")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          _ <- new PgSyncsDao(xa).upsert(clientId, isSent = true)
          result <- sql"SELECT COUNT(1) FROM syncs WHERE is_sent".query[Int].unique.transact(xa)
        } yield assert(result)(equalTo(1))
      ),
      testM("upsert should update is_sent for existing client_id")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgSyncsDao(xa)
          _ <- client.upsert(clientId, isSent = true)
          _ <- client.upsert(clientId)
          result <- sql"SELECT COUNT(1) FROM syncs WHERE is_sent = FALSE".query[Int].unique.transact(xa)
        } yield assert(result)(equalTo(1))
      ),
      testM("getUnsentClientIds should return seq of client_ids")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          _ <- sql"INSERT INTO syncs (client_id, is_sent) VALUES (1, TRUE), (2, FALSE)".update.run.transact(xa)
          result <- new PgSyncsDao(xa).getUnsentClientIds(1)
        } yield assert(result)(equalTo(Seq(2L)))
      )
    ) @@
      beforeAll(ZIO.service[Transactor[Task]].flatMap(InitSchema("/schema.sql", _)).orDie) @@
      after(ZIO.service[Transactor[Task]].flatMap(xa => sql"DELETE FROM syncs".update.run.transact(xa))) @@
      sequential).provideCustomLayerShared(TestPostgresql.managedTransactor)
  }
}
