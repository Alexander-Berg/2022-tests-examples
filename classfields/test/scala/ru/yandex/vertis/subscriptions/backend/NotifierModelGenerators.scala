package ru.yandex.vertis.subscriptions.backend

import org.scalacheck.Gen
import ru.yandex.vertis.push.{PushResponse, PushTargetTypes}
import ru.yandex.vertis.subscriptions.Model.Delivery
import ru.yandex.vertis.subscriptions.backend.transport.push.DirectPushRequest
import ru.yandex.vertis.subscriptions.model.owner.OwnerGenerators
import ru.yandex.vertis.subscriptions.model.{LegacyGenerators, ModelGenerators}
import ru.yandex.vertis.subscriptions.push.GenericPush
import ru.yandex.vertis.subscriptions.storage.{Notification, NotificationSummary}

/**
  *
  * @author zvez
  */
trait NotifierModelGenerators {

  val formedNotification: Gen[FormedNotification] = for {
    subscription <- LegacyGenerators.subscriptionGen
    deliveryType <- Gen.oneOf(Delivery.Type.EMAIL, Delivery.Type.PUSH)
  } yield FormedNotification(
    Notification.Key(subscription, deliveryType),
    System.currentTimeMillis(),
    NotificationSummary(
      preserveLastN = 1,
      since = System.currentTimeMillis(),
      count = 0,
      lastN = Seq.empty,
      includeAfter = System.currentTimeMillis()
    )
  )

  val pushRequest: Gen[DirectPushRequest] = for {
    owner <- OwnerGenerators.owner
    token <- LegacyGenerators.readableStringGen
    title <- LegacyGenerators.readableStringGen
  } yield DirectPushRequest(owner, token, GenericPush(Some(title), None, Map.empty))

  val pushResponse: Gen[PushResponse] = for {
    count <- Gen.choose(0, 10)
  } yield PushResponse(count, PushTargetTypes.User)

}

object NotifierModelGenerators extends NotifierModelGenerators
