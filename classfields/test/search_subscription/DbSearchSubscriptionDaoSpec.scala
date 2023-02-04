package ru.auto.comeback.storage.search_subscription

import cats.data.NonEmptyList
import common.zio.doobie.testkit.TestPostgresql
import doobie.syntax.connectionio._
import doobie.util.transactor.Transactor
import ru.auto.api.comeback_model.SearchSubscription.Status
import ru.auto.comeback.model.testkit.SearchSubscriptionGen.{anyNewSearchSubscription, anyNewSearchSubscriptionList}
import ru.auto.comeback.storage.Schema
import zio.interop.catz._
import zio.test.Assertion.{equalTo, hasSameElements, isNone, isSome}
import zio.test.TestAspect._
import zio.test._
import zio.{Task, ZIO}

object DbSearchSubscriptionDaoSpec extends DefaultRunnableSpec {

  def spec = {
    suite("SearchSubscriptionDao")(
      selectFromEmptyDb,
      insertAndSelectByUserId,
      insertAndSelectByUserIdAndSubscriptionId,
      updateInDb
    ) @@ after(Schema.cleanup) @@ beforeAll(Schema.init) @@ sequential @@ samples(30) @@ shrinks(0)
  }.provideCustomLayerShared(
    TestPostgresql.managedTransactor(version = "12") >+> SearchSubscriptionDao.live
  )

  private val selectFromEmptyDb = testM("select from empty db") {
    checkM(anyNewSearchSubscription)(subs =>
      for {
        _ <- Schema.cleanup
        tx <- ZIO.service[Transactor[Task]]
        dao <- ZIO.service[SearchSubscriptionDao.Service]
        found <- dao.getAllBy(subs.clientId, NonEmptyList.one(subs.status)).transact(tx).map(_.headOption)
      } yield assert(found)(isNone)
    )
  }

  private val insertAndSelectByUserId = testM("insert and select by user_id and status") {
    checkM(anyNewSearchSubscriptionList)(subs =>
      for {
        _ <- Schema.cleanup
        tx <- ZIO.service[Transactor[Task]]
        dao <- ZIO.service[SearchSubscriptionDao.Service]
        inserted <- ZIO.foreach(subs)(sub => dao.insert(sub).transact(tx))
        anyInsertedSubs = inserted.head
        found <- dao.getAllBy(anyInsertedSubs.clientId, NonEmptyList.one(anyInsertedSubs.status)).transact(tx)
        expected = inserted.filter(ins =>
          ins.clientId == anyInsertedSubs.clientId && ins.status == anyInsertedSubs.status
        )
      } yield assert(found)(hasSameElements(expected))
    )
  }

  private val insertAndSelectByUserIdAndSubscriptionId = testM("insert and select by user_id and subscription_id") {
    checkM(anyNewSearchSubscriptionList)(subs =>
      for {
        _ <- Schema.cleanup
        tx <- ZIO.service[Transactor[Task]]
        dao <- ZIO.service[SearchSubscriptionDao.Service]
        inserted <- ZIO.foreach(subs)(sub => dao.insert(sub).transact(tx))
        expected = inserted.head
        found <- dao.getOneBy(expected.clientId, expected.subscriptionId).transact(tx)
      } yield assert(found)(isSome(equalTo(expected)))
    )
  }

  private val updateInDb = testM("update subscription status and settings") {
    val activeSubscriptionGen = anyNewSearchSubscription.map(_.copy(status = Status.ACTIVE))
    checkM(activeSubscriptionGen)(subs =>
      for {
        _ <- Schema.cleanup
        tx <- ZIO.service[Transactor[Task]]
        dao <- ZIO.service[SearchSubscriptionDao.Service]
        inserted <- dao.insert(subs).transact(tx)
        _ <- dao.update(inserted.subscriptionId, inserted.settings, Status.DELETED).transact(tx)
        afterUpdate <- dao
          .getAllBy(subs.clientId, NonEmptyList.one(Status.DELETED))
          .transact(tx)
          .map(_.headOption)
      } yield assertTrue {
        afterUpdate.exists(ss => ss.status == Status.DELETED && ss.settings == subs.settings)
      }
    )
  }
}
