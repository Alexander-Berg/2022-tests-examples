package postgres

import auto.dealers.amoyak.model.triggers.{TriggerEventCommonType, TriggerLead}
import auto.dealers.amoyak.storage.dao.TriggerLeadsDao.TriggerLeadsFilter
import auto.dealers.amoyak.storage.postgres.PgTriggerLeadsDao
import auto.dealers.amoyak.storage.postgres.TriggerToPgEnumMeta._
import cats.data.NonEmptySeq
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import doobie.implicits._
import doobie.Transactor
import zio.{Task, ZIO}
import zio.interop.catz._
import zio.test.Assertion.{hasSameElements, isEmpty}
import zio.test.TestAspect.{after, beforeAll, sequential}
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

object PgTriggerLeadsDaoSpec extends DefaultRunnableSpec {

  private val leadIdToUpdate = 2L

  private val leadsToCreate = Seq(
    TriggerLead(
      id = 1L,
      statusId = 2,
      clientId = 1L,
      responsibleUserId = 1L,
      leadType = TriggerEventCommonType.BalanceEvent,
      title = "some balance title"
    ),
    TriggerLead(
      id = leadIdToUpdate,
      statusId = 1,
      clientId = 2L,
      responsibleUserId = 1L,
      leadType = TriggerEventCommonType.ModerationEvent,
      title = "some moderation title"
    )
  )

  private val leadsToCreateOrUpdate = Seq(
    TriggerLead(
      id = leadIdToUpdate,
      statusId = 1,
      clientId = 2L,
      responsibleUserId = 1L,
      leadType = TriggerEventCommonType.ModerationEvent,
      title = "changed moderation title"
    ),
    TriggerLead(
      id = 3L,
      statusId = 1,
      clientId = 3L,
      responsibleUserId = 1L,
      leadType = TriggerEventCommonType.VasEvent,
      title = "vas title"
    )
  )

  private def getLeadsFromTable(xa: Transactor[Task]): Task[Seq[TriggerLead]] =
    sql"select id, status_id, client_id, responsible_user_id, lead_type, title from trigger_leads"
      .query[TriggerLead]
      .to[Seq]
      .transact(xa)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgTriggerLeadsDao")(
      testM("upsert should insert new rows")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          leadsBeforeInsert <- getLeadsFromTable(xa)
          _ <- new PgTriggerLeadsDao(xa).upsert(leadsToCreate)
          resultAfterUpsert <- getLeadsFromTable(xa)
        } yield assert(leadsBeforeInsert)(isEmpty) && assert(resultAfterUpsert)(hasSameElements(leadsToCreate))
      ),
      testM("upsert should update all fields for existing ids")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          leadsBeforeAnyInsert <- getLeadsFromTable(xa)
          client = new PgTriggerLeadsDao(xa)
          _ <- client.upsert(leadsToCreate)
          resultAfterFirstUpsert <- getLeadsFromTable(xa)
          _ <- client.upsert(leadsToCreateOrUpdate)
          resultAfterSecondUpsert <- getLeadsFromTable(xa)
        } yield assert(leadsBeforeAnyInsert)(isEmpty) &&
          assert(resultAfterFirstUpsert)(hasSameElements(leadsToCreate)) &&
          assert(resultAfterSecondUpsert)(
            hasSameElements(leadsToCreate.filter(_.id != leadIdToUpdate) ++ leadsToCreateOrUpdate)
          )
      ),
      testM("find should return rows filtered by client ids")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgTriggerLeadsDao(xa)
          _ <- client.upsert(leadsToCreate)
          allLeads <- getLeadsFromTable(xa)
          clientIdToSearch = 1L
          filterToSearch =
            TriggerLeadsFilter(clientIds = NonEmptySeq.of(clientIdToSearch), statusIds = Nil, leadTypes = Nil)
          filteredLeads <- client.find(filterToSearch)
        } yield assert(allLeads)(hasSameElements(leadsToCreate)) &&
          assert(filteredLeads)(hasSameElements(leadsToCreate.filter(_.clientId == clientIdToSearch)))
      ),
      testM("find should return rows filtered by client ids and statuses if they exists in filter")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgTriggerLeadsDao(xa)
          _ <- client.upsert(leadsToCreate)
          allLeads <- getLeadsFromTable(xa)
          clientIdToSearch = 1L
          statusIdToSearch = 2
          filterToSearch = TriggerLeadsFilter(
            clientIds = NonEmptySeq.of(clientIdToSearch),
            statusIds = Seq(statusIdToSearch),
            leadTypes = Nil
          )
          filteredLeads <- client.find(filterToSearch)
        } yield assert(allLeads)(hasSameElements(leadsToCreate)) &&
          assert(filteredLeads)(
            hasSameElements(
              leadsToCreate.filter(lead => lead.clientId == clientIdToSearch && lead.statusId == statusIdToSearch)
            )
          )
      ),
      testM("find should return rows filtered by client ids and lead types if they exists in filter")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgTriggerLeadsDao(xa)
          _ <- client.upsert(leadsToCreate)
          allLeads <- getLeadsFromTable(xa)
          clientIdToSearch = 1L
          leadTypeToSearch = TriggerEventCommonType.ModerationEvent
          filterToSearch = TriggerLeadsFilter(
            clientIds = NonEmptySeq.of(clientIdToSearch),
            statusIds = Nil,
            leadTypes = Seq(leadTypeToSearch)
          )
          filteredLeads <- client.find(filterToSearch)
        } yield assert(allLeads)(hasSameElements(leadsToCreate)) &&
          assert(filteredLeads)(
            hasSameElements(
              leadsToCreate.filter(lead => lead.clientId == clientIdToSearch && lead.leadType == leadTypeToSearch)
            )
          )
      ),
      testM("delete should remove leads by their ids")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgTriggerLeadsDao(xa)
          _ <- client.upsert(leadsToCreate)
          allLeads <- getLeadsFromTable(xa)
          leadIdToRemove = 1L
          _ <- client.remove(NonEmptySeq.of(leadIdToRemove))
          leadsAfterRemoving <- getLeadsFromTable(xa)
        } yield assert(allLeads)(hasSameElements(leadsToCreate)) &&
          assert(leadsAfterRemoving)(hasSameElements(leadsToCreate.filter(_.id != leadIdToRemove)))
      )
    ) @@
      beforeAll(ZIO.service[Transactor[Task]].flatMap(InitSchema("/schema.sql", _)).orDie) @@
      after(ZIO.service[Transactor[Task]].flatMap(xa => sql"DELETE FROM trigger_leads".update.run.transact(xa))) @@
      sequential).provideCustomLayerShared(TestPostgresql.managedTransactor)
  }
}
