package ru.auto.comeback.storage.search_subscription

import cats.data.NonEmptyList
import common.zio.doobie.testkit.TestPostgresql
import ru.auto.api.comeback_model.SearchSubscription.Status
import ru.auto.comeback.model.testkit.SearchSubscriptionGen.anyNewSearchSubscription
import ru.auto.comeback.storage.Schema
import zio.ZIO
import zio.test.Assertion.{equalTo, isNone, isSome}
import zio.test.TestAspect.{after, beforeAll, samples, sequential, shrinks}
import zio.test.{assert, assertTrue, checkM, DefaultRunnableSpec}

object LiveSearchSubscriptionServiceSpec extends DefaultRunnableSpec {

  def spec = {
    suite("SearchSubscriptionService")(
      selectFromEmptyDb,
      insertAndSelectByUserId,
      insertAndSelectByUserIdAndSubscriptionId,
      updateInDb
    ) @@ after(Schema.cleanup) @@ beforeAll(Schema.init) @@ sequential @@ samples(30) @@ shrinks(0)
  }.provideCustomLayerShared(
    (SearchSubscriptionDao.live ++ TestPostgresql.managedTransactor(version = "12")) >+> SearchSubscriptionService.live
  )

  private val selectFromEmptyDb = testM("select from empty db") {
    checkM(anyNewSearchSubscription)(subs =>
      for {
        _ <- Schema.cleanup
        service <- ZIO.service[SearchSubscriptionService.Service]
        found <- service.getAllBy(subs.clientId, NonEmptyList.one(subs.status)).map(_.headOption)
      } yield assert(found)(isNone)
    )
  }

  private val insertAndSelectByUserId = testM("insert and select by user_id and status") {
    checkM(anyNewSearchSubscription)(subs =>
      for {
        _ <- Schema.cleanup
        service <- ZIO.service[SearchSubscriptionService.Service]
        inserted <- service.insert(subs)
        found <- service.getAllBy(subs.clientId, NonEmptyList.one(subs.status)).map(_.headOption)
      } yield assert(found)(isSome(equalTo(subs.withId(inserted.id))))
    )
  }

  private val insertAndSelectByUserIdAndSubscriptionId = testM("insert and select by user_id and subscription_id") {
    checkM(anyNewSearchSubscription)(subs =>
      for {
        _ <- Schema.cleanup
        service <- ZIO.service[SearchSubscriptionService.Service]
        inserted <- service.insert(subs)
        found <- service.getOneBy(subs.clientId, subs.subscriptionId)
      } yield assert(found)(isSome(equalTo(subs.withId(inserted.id))))
    )
  }

  private val updateInDb = testM("update subscription status and settings") {
    val activeSubscriptionGen = anyNewSearchSubscription.map(_.copy(status = Status.ACTIVE))
    checkM(activeSubscriptionGen)(subs =>
      for {
        _ <- Schema.cleanup
        service <- ZIO.service[SearchSubscriptionService.Service]
        inserted <- service.insert(subs)
        _ <- service.update(inserted.subscriptionId, inserted.settings, Status.DELETED)
        afterUpdate <- service
          .getAllBy(subs.clientId, NonEmptyList.one(Status.DELETED))
          .map(_.headOption)
      } yield assertTrue {
        afterUpdate.exists(ss => ss.status == Status.DELETED && ss.settings == subs.settings)
      }
    )
  }
}
