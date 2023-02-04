package postgres

import auto.dealers.amoyak.model.triggers.TriggerEventType
import auto.dealers.amoyak.storage.dao.TriggerStatusesDao.ClientTriggerData
import auto.dealers.amoyak.storage.postgres.PgTriggerStatusesDao
import auto.dealers.amoyak.storage.postgres.TriggerToPgEnumMeta._
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import doobie.implicits._
import doobie.Transactor
import zio.{Task, ZIO}
import zio.interop.catz._
import zio.test.Assertion.{hasSameElements, isEmpty, isFalse, isTrue}
import zio.test.TestAspect.{after, beforeAll, sequential}
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

object PgTriggerStatusesDaoSpec extends DefaultRunnableSpec {

  private val clientId = 1L
  private val triggerType = TriggerEventType.BalanceSevenDaysLeft

  private val dataToUpsert = Seq(
    ClientTriggerData(
      clientId,
      triggerType,
      isDisabled = true
    ),
    ClientTriggerData(
      2L,
      TriggerEventType.VasVolumeDecreased,
      isDisabled = true
    )
  )

  private val dataToOverrideExisted = dataToUpsert.headOption.map(_.copy(isDisabled = false)).toSeq

  private def getDataFromTable(xa: Transactor[Task]): Task[Seq[ClientTriggerData]] =
    sql"select client_id, trigger_type, is_disabled from trigger_statuses"
      .query[ClientTriggerData]
      .to[Seq]
      .transact(xa)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgTriggerStatusesDao")(
      testM("upsert should insert new rows")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          dataBeforeUpsert <- getDataFromTable(xa)
          _ <- new PgTriggerStatusesDao(xa).upsert(dataToUpsert)
          resultAfterUpsert <- getDataFromTable(xa)
        } yield assert(dataBeforeUpsert)(isEmpty) && assert(resultAfterUpsert)(hasSameElements(dataToUpsert))
      ),
      testM("upsert should update is_disabled field for existing client trigger")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          dataBeforeUpsert <- getDataFromTable(xa)
          client = new PgTriggerStatusesDao(xa)
          _ <- client.upsert(dataToUpsert)
          resultAfterFirstUpsert <- getDataFromTable(xa)
          _ <- client.upsert(dataToOverrideExisted)
          resultAfterSecondUpsert <- getDataFromTable(xa)
        } yield assert(dataBeforeUpsert)(isEmpty) &&
          assert(resultAfterFirstUpsert)(hasSameElements(dataToUpsert)) &&
          assert(resultAfterSecondUpsert)(hasSameElements(dataToUpsert.tail ++ dataToOverrideExisted))
      ),
      testM("isDisabled should return is_disabled field")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgTriggerStatusesDao(xa)
          _ <- client.upsert(dataToUpsert)
          resultAfterFirstUpsert <- client.isDisabled(clientId, triggerType)
          _ <- client.upsert(dataToOverrideExisted)
          resultAfterSecondUpsert <- client.isDisabled(clientId, triggerType)
        } yield assert(resultAfterFirstUpsert)(isTrue) &&
          assert(resultAfterSecondUpsert)(isFalse)
      ),
      testM("isDisabled should return false for not existing client trigger row")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          triggerStatuses <- getDataFromTable(xa)
          result <- new PgTriggerStatusesDao(xa).isDisabled(clientId, triggerType)
        } yield assert(triggerStatuses)(isEmpty) && assert(result)(isFalse)
      )
    ) @@
      beforeAll(ZIO.service[Transactor[Task]].flatMap(InitSchema("/schema.sql", _)).orDie) @@
      after(ZIO.service[Transactor[Task]].flatMap(xa => sql"DELETE FROM trigger_statuses".update.run.transact(xa))) @@
      sequential).provideCustomLayerShared(TestPostgresql.managedTransactor)
  }
}
