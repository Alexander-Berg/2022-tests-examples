package ru.auto.salesman.mocks

import java.util.concurrent.ConcurrentHashMap

import ru.auto.salesman.Task
import ru.auto.salesman.client.{PublicApiClient, SenderClient}
import ru.auto.salesman.dao.{ClientDao, ClientSubscriptionsDao}
import ru.auto.salesman.mocks.MatchApplicationsNotificationSenderMock.ClientNotification
import ru.auto.salesman.model.ClientId
import ru.auto.salesman.model.match_applications.MatchApplicationCreateRequest
import ru.auto.salesman.service.match_applications.MatchApplicationsNotificationSender
import ru.auto.salesman.test.{TestException, ZIOValues}
import zio.{Promise, UIO}

class MatchApplicationsNotificationSenderMock(
    publicApiClient: PublicApiClient,
    clientSubscriptionsDao: ClientSubscriptionsDao,
    sender: SenderClient,
    clientDao: ClientDao
) extends MatchApplicationsNotificationSender(
      publicApiClient,
      clientSubscriptionsDao,
      sender,
      clientDao
    )
    with ZIOValues {

  private val clientsNotificationStatuses =
    new ConcurrentHashMap[ClientId, ClientNotification]()

  def clear() = clientsNotificationStatuses.clear()

  def mockForClient(
      clientId: ClientId,
      notificationStatus: Boolean
  ): UIO[Promise[TestException, Unit]] = {
    val notificationProgressPromise = Promise.make[TestException, Unit]

    notificationProgressPromise
      .map { promise =>
        clientsNotificationStatuses.put(
          clientId,
          ClientNotification(clientId, notificationStatus, promise)
        )
        promise
      }
  }

  override def sendNotificationWithEmail(
      clientId: ClientId,
      matchApplicationId: MatchApplicationCreateRequest.MatchApplicationId
  ): Task[Unit] = {
    val clientNotification = clientsNotificationStatuses.get(clientId)

    if (clientNotification.shouldNotify)
      clientNotification.promise.succeed(()).unit
    else clientNotification.promise.fail(new TestException()).unit
  }
}

object MatchApplicationsNotificationSenderMock {

  case class ClientNotification(
      clientId: ClientId,
      shouldNotify: Boolean,
      promise: Promise[TestException, Unit]
  )
}
