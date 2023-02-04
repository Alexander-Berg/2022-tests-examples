package ru.yandex.vertis.subscriptions.storage

import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.subscriptions.Model.{State, User}
import ru.yandex.vertis.subscriptions.{SpecBase, TestExecutionContext}
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators

import scala.concurrent.Future

/** Specs on [[ru.yandex.vertis.subscriptions.storage.SubscriptionsDao]] behavior.
  */
trait SubscriptionsDaoSpecBase extends SpecBase with TestExecutionContext with ProducerProvider {

  protected def dao: SubscriptionsDao

  "SubscriptionsDao" should {

    "return Success(NoSuchElementException) on request to non-exist sub" in {
      val result = dao.get("foo").failed.futureValue
      result shouldBe a[NoSuchElementException]
    }

    "get created subscription" in {
      val subscription = CoreGenerators.emailSubscriptions.next

      dao.create(subscription).futureValue
      dao.create(subscription).futureValue //check for double creation
      val restored = dao.get(subscription.getId).futureValue
      restored shouldBe subscription
    }

    "set state" in {
      val subscription = CoreGenerators.emailSubscriptions.next
      dao.create(subscription).futureValue
      dao.create(subscription).futureValue

      dao.setState(subscription.getUser, subscription.getId, State.Value.DELETED).futureValue
      val deleted = dao.get(subscription.getId).futureValue
      deleted.getState.getValue shouldBe State.Value.DELETED
    }

    "list subscriptions by user" in {
      val subscription = CoreGenerators.emailSubscriptions.next
      def user(uid: String) = User.newBuilder().setUid(uid).build
      val user1 = user("uid-1")
      val user2 = user("uid-2")
      val user3 = user("uid-3")
      val user10 = user("uid-10")

      val sub1 = subscription.toBuilder.setId("1").setUser(user1).build()
      val sub2 = subscription.toBuilder.setId("2").setUser(user2).build()
      val sub3 = subscription.toBuilder.setId("3").setUser(user3).build()
      val sub4 = subscription.toBuilder.setId("4").setUser(user1).build()

      dao.create(sub1).futureValue
      dao.create(sub2).futureValue
      dao.create(sub3).futureValue
      dao.create(sub4).futureValue

      dao.listByUser(user1).futureValue.toSet shouldBe Set(sub1, sub4)
      dao.listByUser(user2).futureValue.toSet shouldBe Set(sub2)
      dao.listByUser(user10).futureValue shouldBe empty
    }

    "list many subscriptions by user" in {
      val user = CoreGenerators.userGen.next
      val subs = CoreGenerators.subscriptions.next(20).map(_.toBuilder.setUser(user).build())
      Future.traverse(subs)(dao.create).futureValue

      val loaded = dao.listByUser(user).futureValue

      loaded should contain theSameElementsAs subs
    }

    "delete subscription by ID" in {
      val subscription = CoreGenerators.emailSubscriptions.next

      dao.create(subscription).futureValue
      dao.get(subscription.getId).futureValue should be(subscription)
      dao.listByUser(subscription.getUser).futureValue should contain(subscription)

      dao.delete(subscription.getId).futureValue
      dao.get(subscription.getId).failed.futureValue should be(a[NoSuchElementException])
      dao.listByUser(subscription.getUser).futureValue should not(contain(subscription))
    }
  }

  "SubscriptionsDao.listByUser" should {
    "update touch date" in {
      val subscription = CoreGenerators.emailSubscriptions.next
      val user = subscription.getUser
      dao.create(subscription).futureValue
      val initialTouchO = dao.getLastTouch(user).futureValue
      initialTouchO shouldBe defined
      val Some(initialTouch) = initialTouchO

      Thread.sleep(10)
      dao.listByUser(user).futureValue
      val lastTouchO = dao.getLastTouch(user).futureValue
      lastTouchO shouldBe defined
      val Some(firstTouch) = lastTouchO
      firstTouch.isAfter(initialTouch) shouldBe true

      Thread.sleep(10)
      dao.listByUser(user).futureValue
      val Some(secondTouch) = dao.getLastTouch(user).futureValue

      secondTouch.isAfter(firstTouch) shouldBe true
    }
  }

  "SubscriptionsDao.all" should {
    "return all subscriptions" in {
      val someSubs = CoreGenerators.subscriptions.next(10)
      Future
        .traverse(someSubs) { sub =>
          dao.create(sub)
        }
        .futureValue

      dao.all().futureValue.toSeq should contain allElementsOf someSubs
    }
  }

  "SubscriptionsDao.getMultiple" should {
    "not fail on non-existent subscription" in {
      val subscription = CoreGenerators.emailSubscriptions.next
      dao.create(subscription).futureValue

      val subs = dao.getMultiple(Seq("foo", subscription.getId, "bar")).futureValue

      subs should contain theSameElementsAs Seq(subscription)
    }
  }

}
