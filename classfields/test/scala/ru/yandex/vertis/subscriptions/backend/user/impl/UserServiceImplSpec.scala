package ru.yandex.vertis.subscriptions.backend.user.impl

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.subscriptions.Model._
import ru.yandex.vertis.subscriptions.{SlowAsyncSpec, TestExecutionContext}
import ru.yandex.vertis.subscriptions.backend.actor.{MailConfirmationRequester, SubscriptionConfirmationRequesterActor}
import ru.yandex.vertis.subscriptions.backend.confirmation.Confirmation
import ru.yandex.vertis.subscriptions.backend.confirmation.impl._
import ru.yandex.vertis.subscriptions.backend.user.logging.LoggingUserService
import ru.yandex.vertis.subscriptions.core.plugin.TrivialRequestParser
import ru.yandex.vertis.subscriptions.storage.memory.{InMemoryUserLinks, SubscriptionsDao}
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators
import ru.yandex.vertis.subscriptions.backend.user.UserSubscriptionService
import ru.yandex.vertis.subscriptions.service.impl.JvmDraftService

import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.duration._

/**
  * Specs on [[UserServiceImpl]]
  */
@RunWith(classOf[JUnitRunner])
class UserServiceImplSpec
  extends TestKit(ActorSystem("unit-test", ConfigFactory.empty()))
  with Matchers
  with WordSpecLike
  with TestExecutionContext
  with ScalaFutures
  with SlowAsyncSpec {

  val noMsgTimeout: FiniteDuration = 500.millis

  val subscriptionConfirmationRequester = TestProbe()
  val mailConfirmationRequester = TestProbe()

  val userSubscriptions = new MemoryUserProperties
  val userMails = new MemoryUserProperties

  val subscriptionConfirmation = new SubscriptionConfirmationService(userSubscriptions)
    with EmitSubscriptionsConfirmationViaActor {
    val confirmationRequester = subscriptionConfirmationRequester.ref
  }

  val mailConfirmation = new MailConfirmationService(userMails) with EmitMailConfirmation {
    val confirmationRequester = mailConfirmationRequester.ref
  }

  val subscriptionsDao = new SubscriptionsDao
  val userLinks = new InMemoryUserLinks

  val userSubscriptionsService =
    new UserSubscriptionsServiceImpl(
      subscriptionsDao,
      userLinks,
      TrivialRequestParser,
      "auto",
      sandboxMode = true,
      lightWeightEc = ec,
      blockingEc = ec
    )

  val drafts = new JvmDraftService

  val userService = new UserServiceImpl(
    userSubscriptionsService,
    subscriptionConfirmation,
    mailConfirmation,
    drafts,
    lightWeightEc = ec,
    blockingEc = ec
  ) with LoggingUserService

  "UserServiceImpl for logged user" should {
    val user = User.newBuilder().setUid("1").build()
    val source = CoreGenerators.emailSubscriptionsSources.next
    val email = source.getDelivery.getEmail.getAddress
    var token: String = null
    var subscription: Subscription = null

    "create await confirmation subscription and request email confirmation" in {
      val UserSubscriptionService.Created(s) = userService.create(user, source).futureValue
      subscription = s
      mailConfirmationRequester.expectMsgPF() {
        case MailConfirmationRequester.Request(`user`, _, t) =>
          token = t
          true
      }
      subscription.getState.getValue should be(State.Value.AWAIT_CONFIRMATION)
    }

    "create subscription with state from source and without confirmation" in {
      val otherUser = User.newBuilder().setUid("2").build()
      val activeStateSource = SubscriptionSource
        .newBuilder(CoreGenerators.emailSubscriptionsSources.next)
        .setState(State.newBuilder().setValue(State.Value.ACTIVE).setTimestamp(System.currentTimeMillis))
        .build

      val UserSubscriptionService.Created(activeSubscription) =
        userService.create(otherUser, activeStateSource).futureValue
      assert(activeSubscription.hasState)
      activeSubscription.getState.getValue should be(State.Value.ACTIVE)
      mailConfirmationRequester.expectNoMessage(noMsgTimeout)
    }

    "not create the subscription twice" in {
      userService.create(user, source).futureValue should be(UserSubscriptionService.AlreadyExists(subscription))
    }

    "find subscription by user and request source" in {
      userService.find(user, source.getRequestSource).futureValue should be(Some(subscription))
    }

    "create second await confirmation subscription without request email confirmation" in {
      val anotherSource =
        source.toBuilder.setRequestSource(CoreGenerators.requestSources.next).build
      val UserSubscriptionService.Created(created) = userService.create(user, anotherSource).futureValue
      mailConfirmationRequester.expectNoMessage(noMsgTimeout)
      created.getState.getValue should be(State.Value.AWAIT_CONFIRMATION)
    }

    "get created subscription" in {
      userService.get(user, subscription.getId).futureValue should
        be(Some(subscription))
    }

    "list created subscription" in {
      val subs = userService.list(user).futureValue.toSet
      subs should have size (2)
      subs should contain(subscription)
    }

    "not confirm email and not activate subscription with invalid token" in {
      userService.confirmEmail(Confirmation(user, email, "invalid token")).futureValue should be(false)

      userService.list(user).futureValue.foreach { sub =>
        sub.getState.getValue should be(State.Value.AWAIT_CONFIRMATION)
      }

    }

    "confirm email and activate subscription with correct token" in {
      userService.confirmEmail(Confirmation(user, email, token)).futureValue should be(true)

      userService.list(user).futureValue.foreach { sub =>
        sub.getState.getValue should be(State.Value.ACTIVE)
      }
    }

    "create active subscription without email confirmation" in {
      val anotherSource =
        source.toBuilder.setRequestSource(CoreGenerators.requestSources.next).build
      val UserSubscriptionService.Created(created) = userService.create(user, anotherSource).futureValue
      mailConfirmationRequester.expectNoMessage(noMsgTimeout)
      created.getState.getValue should be(State.Value.ACTIVE)
    }

    "not update not exists subscription" in {
      val patch = SubscriptionSource.getDefaultInstance
      userService.update(user, "foo", patch).futureValue should be(UserSubscriptionService.NothingToUpdate(user, "foo"))
    }

    "update delivery for exists subscription and not skip active state (there is same email address)" in {
      val initial = userService.get(user, subscription.getId).futureValue.get
      val delivery = initial.getDelivery.toBuilder
      val email = delivery.getEmail.toBuilder
      val period = email.getPeriod.toBuilder
      val withFixedPeriod = SubscriptionSource.newBuilder
        .setDelivery(
          delivery
            .mergeEmail(email.mergePeriod(period.setLength(period.getLength + 1).buildPartial()).buildPartial())
            .buildPartial()
        )
        .build()
      val UserSubscriptionService.Updated(was, become) =
        userService.update(user, subscription.getId, withFixedPeriod).futureValue
      was should be(initial)
      become.getState.getValue should be(State.Value.ACTIVE)
      become.getDelivery should be(withFixedPeriod.getDelivery)
      mailConfirmationRequester.expectNoMessage(noMsgTimeout)
    }

    "update for exists subscription and skip active state" in {
      val id = subscription.getId
      val initial = userService.get(user, id).futureValue.get
      val delivery = CoreGenerators.EmailDeliveries.next
      val patch = SubscriptionSource.newBuilder().setDelivery(delivery).build()
      val UserSubscriptionService.Updated(was, become) = userService.update(user, subscription.getId, patch).futureValue
      was should be(initial)

      become.getState.getValue should be(State.Value.AWAIT_CONFIRMATION)
      become.getDelivery should be(delivery)

      mailConfirmationRequester.expectMsgPF() {
        case MailConfirmationRequester.Request(`user`, _, _) => true
      }
    }

    "disable subscription" in {
      val initial = userService.get(user, subscription.getId).futureValue.get
      val patch = SubscriptionSource.newBuilder().setDisabled(true).build()
      val UserSubscriptionService.Updated(was, become) =
        userService.update(user, subscription.getId, patch).futureValue
      was should be(initial)

      become.hasDisabled shouldBe true
      become.getDisabled shouldBe true
      //state shouldn't be affected
      become.getState shouldBe was.getState
    }

    "enable subscription" in {
      val initial = userService.get(user, subscription.getId).futureValue.get
      val patch = SubscriptionSource.newBuilder().setDisabled(false).build()
      val UserSubscriptionService.Updated(was, become) =
        userService.update(user, subscription.getId, patch).futureValue
      was should be(initial)

      become.hasDisabled shouldBe true
      become.getDisabled shouldBe false
      //state shouldn't be affected
      become.getState shouldBe was.getState
    }

    "delete subscription" in {
      userService.delete(user, subscription.getId).futureValue should be(true)
      userService.get(user, subscription.getId).futureValue should be(None)
    }
  }

  "UserServiceImpl for not logged user" should {
    val user = User.newBuilder().setYandexuid("1").build()
    val source = CoreGenerators.emailSubscriptionsSources.next
    var token: String = null
    var subscription: Subscription = null

    "create await confirmation subscription and request subscription confirmation" in {
      val UserSubscriptionService.Created(s) = userService.create(user, source).futureValue
      subscription = s
      subscriptionConfirmationRequester.expectMsgPF() {
        case SubscriptionConfirmationRequesterActor.Request(`user`, _, t) =>
          token = t
          true
      }
      subscription.getState.getValue should be(State.Value.AWAIT_CONFIRMATION)
    }

    "create second await confirmation subscription with request subscription confirmation" in {
      val anotherSource = source.toBuilder.setRequestSource(CoreGenerators.requestSources.next).build
      val UserSubscriptionService.Created(created) = userService.create(user, anotherSource).futureValue
      subscriptionConfirmationRequester.expectMsgPF() {
        case SubscriptionConfirmationRequesterActor.Request(`user`, _, t) => true
      }
      created.getState.getValue should be(State.Value.AWAIT_CONFIRMATION)
    }

    "get created subscription" in {
      userService.get(user, subscription.getId).futureValue should
        be(Some(subscription))
    }

    "list created subscription" in {
      val subs = userService.list(user).futureValue.toSet
      subs should have size (2)
      subs should contain(subscription)
    }

    "not confirm subscription invalid token" in {
      userService.confirmSubscription(Confirmation(user, subscription.getId, "invalid token")).futureValue should
        be(false)

      userService.list(user).futureValue.foreach { sub =>
        sub.getState.getValue should be(State.Value.AWAIT_CONFIRMATION)
      }
    }

    "confirm subscription subscription with correct token" in {
      userService.confirmSubscription(Confirmation(user, subscription.getId, token)).futureValue should be(true)

      userService.get(user, subscription.getId).futureValue.get.getState.getValue should be(State.Value.ACTIVE)
    }

    "update delivery for exists subscription and skip active state (there is same email address)" in {
      val initial = userService.get(user, subscription.getId).futureValue.get
      val delivery = initial.getDelivery.toBuilder
      val email = delivery.getEmail.toBuilder
      val period = email.getPeriod.toBuilder
      val withFixedPeriod = SubscriptionSource.newBuilder
        .setDelivery(
          delivery
            .mergeEmail(email.mergePeriod(period.setLength(period.getLength + 1).buildPartial()).buildPartial())
            .buildPartial()
        )
        .build()
      val UserSubscriptionService.Updated(was, become) =
        userService.update(user, subscription.getId, withFixedPeriod).futureValue
      was should be(initial)
      become.getState.getValue should be(State.Value.ACTIVE)
      become.getDelivery should be(withFixedPeriod.getDelivery)
      subscriptionConfirmationRequester.expectNoMessage(noMsgTimeout)
    }

    "update for exists subscription skip active state" in {
      val id = subscription.getId
      val initial = userService.get(user, id).futureValue.get
      val delivery = CoreGenerators.EmailDeliveries.next
      val patch = SubscriptionSource.newBuilder().setDelivery(delivery).build()
      val UserSubscriptionService.Updated(was, become) = userService.update(user, subscription.getId, patch).futureValue
      was should be(initial)

      become.getState.getValue should be(State.Value.AWAIT_CONFIRMATION)
      become.getDelivery should be(delivery)

      subscriptionConfirmationRequester.expectMsgPF() {
        case SubscriptionConfirmationRequesterActor.Request(`user`, _, _) => true
      }
    }

    "delete subscription" in {
      userService.delete(user, subscription.getId).futureValue should be(true)
      userService.get(user, subscription.getId).futureValue should be(None)
    }

    "update disabled flag in all user's subscriptions" in {
      val user = User.newBuilder().setUid("42").build()
      val count = 10
      val subscriptions = (1 to count).map { _ =>
        val source = CoreGenerators.emailSubscriptionsSources.next
        userService.create(user, source).futureValue
      }

      userService
        .list(user)
        .futureValue
        .map(isDisabled) should contain theSameElementsAs Seq.fill(count)(false)

      userService.updateDisabledFlag(user, disabled = true).futureValue
      userService
        .list(user)
        .futureValue
        .map(isDisabled) should contain theSameElementsAs Seq.fill(count)(true)

      userService.updateDisabledFlag(user, disabled = false).futureValue
      userService
        .list(user)
        .futureValue
        .map(isDisabled) should contain theSameElementsAs Seq.fill(count)(false)
    }
  }

  "User service for not-logged-in mobile user" should {
    val user = User
      .newBuilder()
      .setMobileAppInstance(MobileAppInstance.newBuilder().setUid("123"))
      .build()
    val source = CoreGenerators.pushSubscriptionsSources.next
    var subscription: Subscription = null

    "create push subscription" in {
      val UserSubscriptionService.Created(s) = userService.create(user, source).futureValue
      subscription = s
      subscription.getState.getValue should be(State.Value.ACTIVE)
    }

    "get created subscription" in {
      userService.get(user, subscription.getId).futureValue should be(Some(subscription))
    }

    "list created subscription" in {
      val subs = userService.list(user).futureValue.toSet
      subs should have size 1
      subs should contain(subscription)
    }
  }

  private def isDisabled(s: Subscription) = s.hasDisabled && s.getDisabled

}
