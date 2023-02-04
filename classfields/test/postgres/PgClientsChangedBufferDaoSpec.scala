package postgres

import zio._
import zio.interop.catz._
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.test.TestAspect.{after, beforeAll, sequential}
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import doobie._
import doobie.implicits._
import auto.dealers.amoyak.storage.dao.ClientsChangedBufferDao.{InsertRecord, ResultRecord}
import auto.dealers.amoyak.storage.postgres.PgClientsChangedBufferDao

object PgClientsChangedBufferDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgClientsChangedBufferDao")(getAllChangesTest, getAllChangesFromVos, deleteById) @@
      beforeAll(ZIO.service[Transactor[Task]].flatMap(InitSchema("/schema.sql", _)).orDie) @@
      after(
        ZIO.service[Transactor[Task]].flatMap(xa => sql"DELETE FROM clients_changed_buffer".update.run.transact(xa))
      ) @@
      sequential)
      .provideCustomLayerShared(TestPostgresql.managedTransactor)
  }

  private val getAllChangesTest = testM("get all changes") {
    val entries = Seq(InsertRecord(0L, "vos"), InsertRecord(0L, "unknown"))
    for {
      xa <- ZIO.service[Transactor[Task]]
      state = new PgClientsChangedBufferDao(xa)
      _ <- state.add(entries)
      result <- state.get()
    } yield assert(result.map(resultToInsert))(hasSameElements(entries))
  }

  private val getAllChangesFromVos = testM("get all changes from vos") {
    val entries = Seq(InsertRecord(0L, "vos"), InsertRecord(0L, "unknown"))
    val expected = Seq(InsertRecord(0L, "vos"))
    for {
      xa <- ZIO.service[Transactor[Task]]
      state = new PgClientsChangedBufferDao(xa)
      _ <- state.add(entries)
      result <- state.getByDataSource("vos")
    } yield assert(result.map(resultToInsert))(hasSameElements(expected))
  }

  private val deleteById = testM("delete by id") {
    val entries = Seq(InsertRecord(0L, "vos"), InsertRecord(0L, "unknown"))
    val expected = Seq(InsertRecord(0L, "vos"))
    for {
      xa <- ZIO.service[Transactor[Task]]
      state = new PgClientsChangedBufferDao(xa)
      _ <- state.add(entries)
      forDeletion <- state.getByDataSource("unknown")
      _ <- state.delete(forDeletion.head.id)
      result <- state.get()
    } yield assert(result.map(resultToInsert))(hasSameElements(expected))
  }

  private def resultToInsert(res: ResultRecord): InsertRecord =
    InsertRecord(res.clientId, res.dataSource)

}
