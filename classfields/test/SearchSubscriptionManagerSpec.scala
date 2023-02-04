package ru.auto.comeback.services.test

import auto.common.manager.catalog.CatalogRepository
import auto.common.manager.catalog.model.CatalogNames
import auto.common.manager.catalog.testkit.CatalogRepositoryMock
import auto.common.model.user.AutoruUser.UserRef.DealerId
import cats.data.NonEmptyList
import common.clients.subscriptions.api.src.SubscriptionsClient
import common.zio.logging.Logging
import common.zio.ops.tracing.Tracing
import common.zio.ops.tracing.testkit.TestTracing
import common.zio.sttp.model.SttpError
import ru.auto.api.comeback_model.SearchSubscription.{Settings, Status}
import ru.auto.comeback.model.subscriptions.SearchSubscription.{NewSearchSubscription, SearchSubscription}
import ru.auto.comeback.model.testkit.AutoUserGen.anyDealerId
import ru.auto.comeback.model.testkit.CommonGen.anyYandexEmail
import ru.auto.comeback.model.testkit.SearchSubscriptionGen.{
  anyNewSearchSubscription,
  anySearchSubscription,
  anySearchSubscriptionList
}
import ru.auto.comeback.services.SearchSubscriptionManagerLive
import ru.auto.comeback.services.SearchSubscriptionManagerLive.{buildTitle, DefaultTitle}
import ru.auto.comeback.services.SearchSubscriptionModelConverter.subscriptionToProto
import ru.auto.comeback.storage.search_subscription.SearchSubscriptionService
import ru.yandex.vertis.subscriptions.api.api_model
import ru.yandex.vertis.subscriptions.api.api_model.CreateSubscriptionParameters
import zio.test.Assertion._
import zio.test._
import zio.test.mock.Expectation._
import zio.{IO, Task, ZIO}

object SearchSubscriptionManagerSpec extends DefaultRunnableSpec {

  private val catalogNames = CatalogNames("BMW", Some("X5"), Some("VIII Рестайлинг"), Some("AMT 1.8 AMT (140 л.с.)"))

  private val catalogMock =
    CatalogRepositoryMock
      .GetHumanNames(
        anything,
        value(catalogNames)
      )
      .atLeast(0)
      .toLayer

  private def db(existing: List[SearchSubscription]) = new SearchSubscriptionService.Service {

    var state: List[SearchSubscription] = existing

    override def getAllBy(clientId: DealerId, statuses: NonEmptyList[Status]): Task[List[SearchSubscription]] =
      ZIO.succeed(state.filter(s => s.clientId == clientId && statuses.exists(_ == s.status)))

    override def update(subscriptionId: String, settings: Settings, status: Status): Task[Long] = {
      val updated = state.find(s => s.subscriptionId == subscriptionId).get.copy(settings = settings, status = status)
      state = state.filterNot(_.subscriptionId == subscriptionId) :+ updated
      ZIO.succeed(1L)
    }

    override def insert(subscription: NewSearchSubscription): Task[SearchSubscription] = {
      val inserted = subscription.withId(Long.MaxValue)
      state = state :+ inserted
      ZIO.succeed(inserted)
    }

    override def getOneBy(clientId: DealerId, subscriptionId: String): Task[Option[SearchSubscription]] =
      ZIO.succeed(state.find(s => s.clientId == clientId && s.subscriptionId == subscriptionId))
  }

  private def subscriptionsClient(id: String) = new SubscriptionsClient.Service {
    override def list(user: String, service: String): IO[SttpError, List[api_model.Subscription]] = ???

    override def addOrUpdate(
        user: String,
        service: String,
        parameters: CreateSubscriptionParameters): IO[SttpError, api_model.Subscription] =
      IO.succeed(api_model.Subscription.defaultInstance.withId(id))

    override def delete(user: String, service: String, subscriptionId: String): IO[SttpError, Unit] = ???
  }

  private def searchSubscriptionManager(
      subscriptionService: SearchSubscriptionService.Service,
      subscriptionId: String) = {
    for {
      logging <- ZIO.service[Logging.Service]
      tracing <- ZIO.service[Tracing.Service]
      catalog <- ZIO.service[CatalogRepository.Service]
    } yield new SearchSubscriptionManagerLive(
      subscriptionService,
      subscriptionsClient(subscriptionId),
      catalog,
      logging,
      tracing
    )
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {

    suite("SearchSubscriptionManager")(
      testM("list all active search subscriptions") {
        checkM(anySearchSubscriptionList, anyDealerId) { case (subs, dealerId) =>
          val existing = subs.map(_.copy(clientId = dealerId))
          for {
            manager <- searchSubscriptionManager(db(existing), "")
            all <- manager.getAllBy(dealerId.raw)
            expected = existing.filter(_.status == Status.ACTIVE).collect {
              case sub if sub.settings.filter.catalogFilter.isEmpty => subscriptionToProto(sub, DefaultTitle)
              case sub => subscriptionToProto(sub, buildTitle(catalogNames))
            }
          } yield assert(all.subscriptions.map(_.update()))(hasSameElements(expected))
        }
      },
      testM("insert search subscriptions as active and provide correct subscription id from api") {
        checkM(anyNewSearchSubscription) { sub =>
          for {
            manager <- searchSubscriptionManager(db(List()), sub.subscriptionId)
            inserted <- manager.insert(sub.clientId.raw, sub.settings)
          } yield assertTrue(inserted.subscription.get == subscriptionToProto(sub, buildTitle(catalogNames)))
        }
      },
      testM("reactivate previously deleted search subscription if inserting same subscription") {
        checkM(anyNewSearchSubscription) { sub =>
          val existing = sub.copy(status = Status.DELETED).withId(1)
          for {
            manager <- searchSubscriptionManager(db(List(existing)), sub.subscriptionId)
            inserted <- manager.insert(sub.clientId.raw, sub.settings)
            all <- manager.getAllBy(sub.clientId.raw)
          } yield assertTrue(inserted.subscription.get == subscriptionToProto(sub, buildTitle(catalogNames))) &&
            assert(all.subscriptions)(hasSameElements(List(subscriptionToProto(sub, buildTitle(catalogNames)))))
        }
      },
      testM("update search subscription emails") {
        checkM(anySearchSubscription, anyYandexEmail) { case (existing, newEmail) =>
          for {
            manager <- searchSubscriptionManager(db(List(existing)), existing.subscriptionId)
            updated <- manager.update(existing.clientId.raw, existing.subscriptionId, List(newEmail))
            expected = existing.copy(settings = existing.settings.clearEmails.addEmails(newEmail))
          } yield assertTrue(updated.subscription.get == subscriptionToProto(expected, buildTitle(catalogNames)))
        }
      },
      testM("delete search subscription logically") {
        checkM(anySearchSubscription) { case (existing) =>
          val service = db(List(existing))
          for {
            manager <- searchSubscriptionManager(service, existing.subscriptionId)
            _ <- manager.delete(existing.clientId.raw, existing.subscriptionId)
            all <- service.getAllBy(existing.clientId, NonEmptyList.of(Status.ACTIVE, Status.DELETED))
            expected = existing.copy(status = Status.DELETED)
          } yield assert(all)(hasSameElements(List(expected)))
        }
      }
    )
  }.provideCustomLayer(Logging.live ++ TestTracing.noOp ++ catalogMock)

}
