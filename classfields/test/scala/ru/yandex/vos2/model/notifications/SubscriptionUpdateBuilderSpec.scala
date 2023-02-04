package ru.yandex.vos2.model.notifications

import java.time.{LocalDateTime, ZoneId}
import java.util.concurrent.ThreadLocalRandom

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vos2.OfferModel.{Offer, OfferService}
import ru.yandex.vos2.UserModel.User
import ru.yandex.vertis.vos2.model.notifications.NotificationType._
import ru.yandex.vertis.vos2.model.notifications.Subscription
import ru.yandex.vos2.getNow
import ru.yandex.vos2.model.notifications.NotificationModelUtils._
import ru.yandex.vos2.model.notifications.NotificationTypes.{aggregationFor, silenceFor}
import ru.yandex.vos2.realty.model.RealtyNotificationManager
import ru.yandex.vos2.realty.services.notifications.builders.RealtyNotificationBuilders

import scala.collection.JavaConverters._

/**
  * @author Nataila Ratskevich (reimai@yandex-team.ru)
  */
@RunWith(classOf[JUnitRunner])
class SubscriptionUpdateBuilderSpec extends WordSpec with Matchers {

  private val recent = LocalDateTime
    .of(2017, 3, 21, 11, 35, 0)
    .atZone(ZoneId.of("Europe/Moscow"))
    .toInstant
    .toEpochMilli
  private val old = LocalDateTime
    .of(2016, 12, 8, 11, 59, 0)
    .atZone(ZoneId.of("Europe/Moscow"))
    .toInstant
    .toEpochMilli

  private val recentUser = user(recent)
  private val oldUser = user(old)
  private val oldOffer = offer(old)
  private val recentOffer = offer(recent)
  private val oldUserInitial = RealtyNotificationManager.initSubscription(oldUser)
  private val recentUserInitial = RealtyNotificationManager.initSubscription(recentUser)

  "SubscriptionUpdateBuilder" should {

    "unsubscribe" in {
      withActions(getNow, recentUser)(
        _.addForUser(RealtyNotificationBuilders.SimpleEmail(NT_HELLO_OWNER).build()),
        _.subscribe(false)
      ) { subscription ⇒
        assert(subscription.isDefined)
        subscription.foreach { s ⇒
          assert(s.getDoneCount == 1)
          assert(s.getDone(0) == NT_HELLO_OWNER.toString)
          assert(s.getNotificationQueueCount == 0)
        }
      }
    }

    "don't spam an old user" in {
      withActions(getNow, oldUser, oldUserInitial)(
        _.addForUser(RealtyNotificationBuilders.SimpleEmail(NT_HELLO_AGENT).build())
      ) { subscription ⇒
        assert(subscription.isEmpty)
      }
    }

    "queue event for a new offer" in {
      withActions(getNow, recentUser)(
        _.addForOffer(RealtyNotificationBuilders.offerNotifications(NT_SOLD, recentOffer), recentOffer),
        _.addForOffer(RealtyNotificationBuilders.offerNotifications(NT_SOLD, recentOffer), recentOffer)
      ) { subscription ⇒
        assert(subscription.isDefined)
        subscription.foreach { s ⇒
          assert(s.getNotificationQueueCount == 1)
          assert(s.getNotificationQueue(0).getType == NT_SOLD)
        }
      }
    }

    "don't spam an old offer" in {
      withActions(getNow, recentUser)(
        _.addForOffer(RealtyNotificationBuilders.offerNotifications(NT_SOLD, oldOffer), oldOffer)
      ) { subscription ⇒
        assert(subscription.isEmpty)
      }
    }

    "wait aggregation delay" in {
      val now = getNow
      withActions(now, recentUser)(
        _.addForOffer(RealtyNotificationBuilders.offerNotifications(NT_OWNER_ADVISE, recentOffer), recentOffer)
      ) { subscription ⇒
        assert(subscription.isDefined)
        subscription.foreach { s ⇒
          assert(s.getCheckTime == now + aggregationFor(NT_OWNER_ADVISE))
          assert(s.getNotificationQueueCount == 1)
          assert(s.getNotificationQueue(0).getType == NT_OWNER_ADVISE)
        }
      }
    }

    "wait initial delay for a brand new user" in {
      val userCreated = getNow
      val newUser = user(userCreated)
      val later = userCreated + NotificationTypes.initialUserWideDelay / 10
      val initial = applyActions(later, newUser, RealtyNotificationManager.initSubscription(newUser, later))(
        _.addForOffer(RealtyNotificationBuilders.offerNotifications(NT_OWNER_ADVISE, recentOffer), recentOffer)
      ).get

      assert(initial.getCheckTime == userCreated + NotificationTypes.initialUserWideDelay)
      assert(initial.getNotificationQueueCount == 1)
      assert(initial.getNotificationQueue(0).getType == NT_OWNER_ADVISE)

      val stillInitialSilence = later + NotificationTypes.initialUserWideDelay / 2

      val anotherRecentOffer = offer(recent)
      val addSome = applyActions(stillInitialSilence, newUser, initial)(
        _.addForOffer(RealtyNotificationBuilders.offerNotifications(NT_CHANGE_MIND, recentOffer), recentOffer),
        _.addForOffer(
          RealtyNotificationBuilders.offerNotifications(NT_OWNER_ADVISE, anotherRecentOffer),
          anotherRecentOffer
        )
      ).get

      assert(addSome.getCheckTime == userCreated + NotificationTypes.initialUserWideDelay)
      assert(addSome.getNotificationQueueCount == 3)
      assert(addSome.getNotificationQueueList.asScala.map(_.getType).count(_ == NT_OWNER_ADVISE) == 2)
      assert(addSome.getNotificationQueueList.asScala.map(_.getType).contains(NT_CHANGE_MIND))
      assert(addSome.notificationsReady(userCreated + NotificationTypes.initialUserWideDelay).size == 3)
    }

    "wait aggregation delay for already sent" in {
      val start = getNow
      withActions(start, recentUser)(
        _.addForOffer(RealtyNotificationBuilders.offerNotifications(NT_OWNER_ADVISE, recentOffer), recentOffer)
      ) { subscription ⇒
        assert(subscription.isDefined)
        val scheduled = subscription.get.getCheckTime
        assert(scheduled == start + aggregationFor(NT_OWNER_ADVISE))
        val afterSomeDelay = scheduled + silenceFor(NT_OWNER_ADVISE) / 10
        val emptySubscription = applyActions(afterSomeDelay, recentUser, subscription.get)(
          _.sent(subscription.get.getNotificationQueueList.asScala, afterSomeDelay)
        ).get
        assert(emptySubscription.getCheckTime == 0)
        assert(emptySubscription.getNotificationQueueCount == 0)
        assert(emptySubscription.getLastNotifiedList.asScala.forall(_.getTimestamp == afterSomeDelay))

        val newOffer = offer(recent)
        val newNotification = RealtyNotificationBuilders.offerNotifications(NT_OWNER_ADVISE, newOffer);
        {
          //new notification is scheduled before silence period is over
          val updatedSubscription =
            applyActions(afterSomeDelay + silenceFor(NT_OWNER_ADVISE) / 3, recentUser, emptySubscription)(
              _.addForOffer(newNotification, newOffer)
            ).get
          assert(updatedSubscription.getCheckTime == afterSomeDelay + silenceFor(NT_OWNER_ADVISE))
          assert(updatedSubscription.getNotificationQueueCount == 1)
        }
        {
          //alternative - new notification is scheduled after silence period
          val updatedSubscription =
            applyActions(afterSomeDelay + silenceFor(NT_OWNER_ADVISE) + 1000, recentUser, emptySubscription)(
              _.addForOffer(newNotification, newOffer)
            ).get
          assert(
            updatedSubscription.getCheckTime == afterSomeDelay + silenceFor(NT_OWNER_ADVISE) + 1000 + aggregationFor(
              NT_OWNER_ADVISE
            )
          )
          assert(updatedSubscription.getNotificationQueueCount == 1)
        }
      }
    }

    "don't delay if sending is not catching up" in {
      withActions(getNow, recentUser)(
        _.addForOffer(RealtyNotificationBuilders.offerNotifications(NT_OWNER_ADVISE, recentOffer), recentOffer)
      ) { subscription ⇒
        assert(subscription.isDefined)
        val scheduled = subscription.get.getCheckTime
        val newOffer = offer(recent)
        val newNotification = RealtyNotificationBuilders.offerNotifications(NT_OWNER_ADVISE, newOffer)
        val aSilenceAfter = scheduled + silenceFor(NT_OWNER_ADVISE) + 1000
        val updatedSubscription =
          new SubscriptionUpdateBuilder(RealtyNotificationManager, recentUser, subscription.get)
            .addForOffer(newNotification, newOffer)
            .build(aSilenceAfter)
            .get
            ._1
        assert(updatedSubscription.getCheckTime == scheduled)
        assert(updatedSubscription.getNotificationQueueCount == 2)
      }
    }
  }

  private def withActions(time: Long, user: User, subscription: Subscription = recentUserInitial)(
    actions: (SubscriptionUpdateBuilder ⇒ SubscriptionUpdateBuilder)*
  )(f: Option[Subscription] ⇒ Unit): Unit = {
    f(applyActions(time, user, subscription)(actions: _*))
  }

  private def applyActions(time: Long, user: User, subscription: Subscription = recentUserInitial)(
    actions: (SubscriptionUpdateBuilder ⇒ SubscriptionUpdateBuilder)*
  ): Option[Subscription] = {
    val builder = new SubscriptionUpdateBuilder(RealtyNotificationManager, user, subscription)
    actions.foldLeft(builder)((b, a) ⇒ a(b)).build(time).map(_._1)
  }

  private def user(created: Long): User =
    User
      .newBuilder()
      .setUserRef("Some ref")
      .setTimestampCreate(created)
      .build()

  private def offer(created: Long): Offer = {
    val id = ThreadLocalRandom.current().nextLong()
    Offer
      .newBuilder()
      .setUserRef("")
      .setOfferID(s"i_$id")
      .setOfferIRef(id)
      .setTimestampCreate(created)
      .setTimestampUpdate(created)
      .setOfferService(OfferService.OFFER_REALTY)
      .build()
  }
}
