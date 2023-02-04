package postgres

import auto.dealers.amoyak.model.ClientType
import auto.dealers.amoyak.storage.dao.CompaniesDao.CompanyRecord
import auto.dealers.amoyak.storage.dao.UsersDao.InsertUserRecord
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import doobie._
import doobie.implicits._
import auto.dealers.amoyak.storage.postgres.{PgCompaniesDao, PgUsersDao}
import zio.{Task, ZIO}
import zio.interop.catz._
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.test.Assertion._
import zio.test.TestAspect._

object PgCompaniesDaoSpec extends DefaultRunnableSpec {

  private val testAutoruId = 100L
  private val testAmoId = 234L

  private val testResponsibleUserId = 1L
  private val otherResponsibleUserId = 2L

  private val testRecord = CompanyRecord(testAutoruId, isHeadCompany = false, testAmoId, Some(testResponsibleUserId))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgCompaniesMappingDao")(
      testM("get should return companies mapping record")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          insertValues = fr"$testAutoruId, false, $testAmoId, $testResponsibleUserId"
          otherInsertValues = fr"$testAutoruId, true, ${1000 + testAmoId}, $testResponsibleUserId"
          _ <-
            sql"insert into companies(autoru_id, is_head_company, amo_id, responsible_user_id) values ($insertValues)".update.run
              .transact(xa)
          _ <-
            sql"insert into companies(autoru_id, is_head_company, amo_id, responsible_user_id) values ($otherInsertValues)".update.run
              .transact(xa)
          client = new PgCompaniesDao(xa)
          result <- client.getByAmoId(testAmoId)
        } yield assert(result)(equalTo(testRecord))
      ),
      testM("create should insert new row")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgCompaniesDao(xa)
          _ <- client.create(testRecord)
          result <- client.getByAmoId(testAmoId)
        } yield assert(result)(equalTo(testRecord))
      ),
      testM("update should replace responsible_user_id for existing mapping record")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgCompaniesDao(xa)
          _ <- client.create(testRecord)
          _ <- client.update(testAmoId, otherResponsibleUserId)
          result <- client.getByAmoId(testAmoId).map(_.responsibleUserId)
        } yield assert(result)(equalTo(Some(otherResponsibleUserId)))
      ),
      testM("getByAmoId and getByAutoruId should return same record")(for {
        xa <- ZIO.service[Transactor[Task]]
        client = new PgCompaniesDao(xa)
        _ <- client.create(testRecord)
        resultByAmoId <- client.getByAmoId(testAmoId)
        resultByAutoruId <- client.getByAutoruId(testAutoruId, ClientType.Client)
      } yield assertTrue(resultByAmoId == testRecord) && assertTrue(resultByAutoruId == testRecord))
    ) @@
      beforeAll(
        ZIO
          .service[Transactor[Task]]
          .flatMap { xa =>
            for {
              _ <- InitSchema("/schema.sql", xa)
              client = new PgUsersDao(xa)
              _ <- client.create(InsertUserRecord(111L, "test"))
              _ <- client.create(InsertUserRecord(222L, "other"))
            } yield ()
          }
      ) @@
      after(ZIO.service[Transactor[Task]].flatMap(xa => sql"DELETE FROM companies".update.run.transact(xa))) @@
      sequential).provideCustomLayerShared(TestPostgresql.managedTransactor)
  }
}
