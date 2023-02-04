package ru.yandex.vos2.watching

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vos2.OfferModel.IDXStatus.NoteType.ERROR
import ru.yandex.vertis.vos2.model.notifications.NotificationType._
import ru.yandex.vertis.vos2.model.notifications.{NotificationType, Subscription}
import ru.yandex.vos2.config.TestRealtySchedulerComponents
import ru.yandex.vos2.getNow
import ru.yandex.vertis.vos2.model.notifications.Notification
import ru.yandex.vos2.model.notifications.NotificationModelUtils._
import ru.yandex.vos2.model.notifications.NotificationTypes.aggregationFor
import ru.yandex.vos2.model.notifications.SubscriptionUpdateBuilder
import ru.yandex.vos2.realty.model.{RealtyNotificationManager, TestUtils}
import ru.yandex.vos2.realty.services.notifications.builders.RealtyNotificationBuilders
import ru.yandex.vos2.realty.services.sender.ProbeSenderClient
import ru.yandex.vos2.util.log.Logging
import ru.yandex.vos2.watching.subscriptions.NotificationEngine

@RunWith(classOf[JUnitRunner])
class NotificationEngineSpec extends WordSpec with Matchers with Logging {

  val components = new TestRealtySchedulerComponents
  val engine = new NotificationEngine(components)
  val senderClient = components.coreComponents.senderClient.asInstanceOf[ProbeSenderClient]

  val user = {
    val builder = TestUtils.createUser()
    builder.getUserContactsBuilder.addPhonesBuilder().setNumber("+79111111111")
    builder.build
  }

  val offer = {
    val builder = TestUtils
      .createOffer()
      .setUserRef(user.getUserRef)
    builder.getIDXStatusBuilder
      .addNoteBuilder()
      .setNoteType(ERROR)
      .setNoteCode(1)
    builder.build()
  }
  val initialSubscription = RealtyNotificationManager.initSubscription(user)

  "Engine" should {

    "collapse identical notifications" in {
      senderClient.reset()
      val subscription = notifications(
        getNow - aggregationFor(NT_OFFER_FORGIVEN),
        initialSubscription,
        NT_CHANGE_MIND,
        NT_CHANGE_MIND,
        NT_PUBLISHED,
        NT_PUBLISHED
      )
      engine.process(subscription, user)
      senderClient.allSent should have size 2
    }
  }

  private def notifications(time: Long, subscription: Subscription, notifyOf: NotificationType*): Subscription = {
    val builder = new SubscriptionUpdateBuilder(RealtyNotificationManager, user, subscription)
    notifyOf
      .map(notify)
      .foldLeft(builder)((b, a) ⇒ a(b))
      .build(time)
      .map(_._1)
      .getOrElse(subscription)
  }

  private def notify(`type`: NotificationType)(sub: SubscriptionUpdateBuilder): SubscriptionUpdateBuilder = {
    if (`type`.isPerUser) {
      notifyUser(`type`, sub)
    } else {
      notifyOffer(`type`, sub)
    }
  }

  private def notifyOffer(`type`: NotificationType, sub: SubscriptionUpdateBuilder): SubscriptionUpdateBuilder = {
    sub.addForOffer(
      RealtyNotificationBuilders
        .offerNotifications(`type`, offer)
        .map(
          setEventTimeForNotification(_)
        ),
      offer
    )
  }

  private def notifyUser(`type`: NotificationType, sub: SubscriptionUpdateBuilder): SubscriptionUpdateBuilder = {
    val offer = TestUtils
      .createOffer()
      .setUserRef(sub.getOriginal.getUserRef)
      .build()
    sub.addForUser(
      RealtyNotificationBuilders
        .offerNotifications(`type`, offer)
        .map(
          setEventTimeForNotification(_)
        )
    )
  }

  private def setEventTimeForNotification(
    notification: Notification,
    eventType: Long = DateTime.now().getMillis
  ): Notification =
    notification.toBuilder.setEventTime(DateTime.now().getMillis).build()

}
