package ru.auto.comeback.services.testkit

import auto.common.model.user.AutoruUser.UserRef
import cats.data.NonEmptyList
import ru.auto.api.comeback_model.SearchSubscription.{Settings, Status}
import ru.auto.comeback.model.subscriptions.SearchSubscription.{NewSearchSubscription, SearchSubscription}
import ru.auto.comeback.storage.search_subscription.SearchSubscriptionService
import ru.auto.comeback.storage.search_subscription.SearchSubscriptionService.SearchSubscriptionService
import zio.test.mock
import zio.{Has, Task, URLayer, ZLayer}
import zio.test.mock.Mock

object SearchSubscriptionServiceMock extends Mock[SearchSubscriptionService] {

  object GetAllBy extends Effect[(UserRef.DealerId, NonEmptyList[Status]), Throwable, List[SearchSubscription]]

  object Update extends Effect[(String, Settings, Status), Throwable, Long]

  object Insert extends Effect[NewSearchSubscription, Throwable, SearchSubscription]

  object GetOneBy extends Effect[(UserRef.DealerId, String), Throwable, Option[SearchSubscription]]

  override val compose: URLayer[Has[mock.Proxy], SearchSubscriptionService] = ZLayer.fromService { proxy =>
    new SearchSubscriptionService.Service {
      override def getAllBy(
          clientId: UserRef.DealerId,
          statuses: NonEmptyList[Status]): Task[List[SearchSubscription]] = proxy(GetAllBy, clientId, statuses)

      override def update(subscriptionId: String, settings: Settings, status: Status): Task[Long] =
        proxy(Update, subscriptionId, settings, status)

      override def insert(subscription: NewSearchSubscription): Task[SearchSubscription] = proxy(Insert, subscription)

      override def getOneBy(clientId: UserRef.DealerId, subscriptionId: String): Task[Option[SearchSubscription]] =
        proxy(GetOneBy, clientId, subscriptionId)
    }
  }
}
