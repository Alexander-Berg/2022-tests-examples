package postgres

import auto.dealers.amoyak.storage.dao.UsersDao.{InsertUserRecord, ResultUserRecord}
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import doobie._
import doobie.implicits._
import auto.dealers.amoyak.storage.postgres.PgUsersDao
import zio.{Task, ZIO}
import zio.interop.catz._
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.test.Assertion._
import zio.test.TestAspect._

object PgUsersDaoSpec extends DefaultRunnableSpec {

  private val testAmoId = 1L
  private val testEmail = "a@a.aa"
  private val testInsertRecord = InsertUserRecord(testAmoId, testEmail)
  private val testResultRecord = ResultUserRecord(1, testAmoId, testEmail)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgUsersDaoSpec")(
      testM("get should return user record by amo id")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          insertValues = fr"$testAmoId, $testEmail"
          otherInsertValues = fr"${1000 + testAmoId}, ${"mail" + testEmail}"
          _ <- sql"insert into users(amo_id, email) values ($insertValues)".update.run.transact(xa)
          _ <- sql"insert into users(amo_id, email) values ($otherInsertValues)".update.run.transact(xa)
          client = new PgUsersDao(xa)
          result <- client.getByAmoId(testAmoId)
        } yield assert(result)(equalTo(testResultRecord))
      ),
      testM("get should return user record by id")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          insertValues = fr"$testAmoId, $testEmail"
          otherInsertValues = fr"${1000 + testAmoId}, ${"mail" + testEmail}"
          _ <- sql"insert into users(amo_id, email) values ($insertValues)".update.run.transact(xa)
          _ <- sql"insert into users(amo_id, email) values ($otherInsertValues)".update.run.transact(xa)
          client = new PgUsersDao(xa)
          result <- client.getById(1L)
        } yield assert(result)(equalTo(testResultRecord))
      ),
      testM("create should insert new row")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgUsersDao(xa)
          _ <- client.create(testInsertRecord)
          result <- client.getByAmoId(testAmoId)
        } yield assert(result)(equalTo(testResultRecord))
      ),
      testM("update should replace responsible_user_id for existing mapping record")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgUsersDao(xa)
          _ <- client.create(testInsertRecord)
          newEmail = "b@b.bb"
          _ <- client.updateByAmoId(testAmoId, newEmail)
          result <- client.getByAmoId(testAmoId).map(_.email)
        } yield assert(result)(equalTo(newEmail))
      )
    ) @@
      beforeAll(ZIO.service[Transactor[Task]].flatMap(InitSchema("/schema.sql", _)).orDie) @@
      after(ZIO.service[Transactor[Task]].flatMap { xa =>
        sql"DELETE FROM users; ALTER SEQUENCE users_id_seq RESTART WITH 1".update.run.transact(xa)
      }) @@
      sequential).provideCustomLayerShared(TestPostgresql.managedTransactor)
  }
}
