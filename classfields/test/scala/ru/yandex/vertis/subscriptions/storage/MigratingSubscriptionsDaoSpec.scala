package ru.yandex.vertis.subscriptions.storage

import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators
import ru.yandex.vertis.subscriptions.{Mocking, Model, SlowAsyncSpec, SpecBase, SubscriptionId, TestExecutionContext}
import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.vertis.subscriptions.Model.User

import org.junit.runner.RunWith
import org.scalatest.concurrent.Eventually
import org.scalatest.junit.JUnitRunner

/**
  *
  * @author zvez
  */
@RunWith(classOf[JUnitRunner])
class MigratingSubscriptionsDaoSpec
  extends SpecBase
  with Mocking
  with TestExecutionContext
  with Eventually
  with SlowAsyncSpec {

  private val sub = CoreGenerators.subscriptions.next
  private val otherSub = CoreGenerators.subscriptions.next
  private val subs = Seq(sub, otherSub)
  private val user = sub.getUser
  private val subId = sub.getId

  trait TestSubscriptionsDao extends SubscriptionsDao with TokenSubscriptions

  private trait Test {
    val main = mock[TestSubscriptionsDao]
    val slave = mock[SubscriptionsDao]

    val dao = new MigratingSubscriptionsDao(main, slave, "test")(ec, TestOperationalSupport)
  }

  "get" should {
    "migrate subscription if it doesn't exist in slave" in new Test {
      (main.get(_: User, _: SubscriptionId)).expects(user, subId).returnsF(sub)
      (slave.get(_: User, _: SubscriptionId)).expects(user, subId).failsF(new NoSuchElementException)
      (slave.create _).expects(sub).returnsF(())

      dao.get(user, subId).futureValue shouldBe sub
      waitEffectsReady()
    }

    "not do anything if subscription is already in slave" in new Test {
      (main.get(_: User, _: SubscriptionId)).expects(user, subId).returnsF(sub)
      (slave.get(_: User, _: SubscriptionId)).expects(user, subId).returnsF(sub)

      dao.get(user, subId).futureValue shouldBe sub
      waitEffectsReady()
    }

    "migrate subscription if it is different in slave " in new Test {
      (main.get(_: User, _: SubscriptionId)).expects(user, subId).returnsF(sub)
      (slave.get(_: User, _: SubscriptionId)).expects(user, subId).returnsF(otherSub)
      (slave.create _).expects(sub).returnsF(())

      dao.get(user, subId).futureValue shouldBe sub
      waitEffectsReady()
    }

    "fallback to slave if main fails" in new Test {
      (main.get(_: User, _: SubscriptionId)).expects(user, subId).failsF(new RuntimeException)
      (slave.get(_: User, _: SubscriptionId)).expects(user, subId).returnsF(sub)

      dao.get(user, subId).futureValue shouldBe sub
      waitEffectsReady()
    }

    "should not fail if slave fails" in new Test {
      (main.get(_: User, _: SubscriptionId)).expects(user, subId).returnsF(sub)
      (slave.get(_: User, _: SubscriptionId)).expects(user, subId).failsF(new RuntimeException)

      dao.get(user, subId).futureValue shouldBe sub
      waitEffectsReady()
    }
  }

  "create" should {
    "replicate to slave" in new Test {
      (main.create _).expects(sub).returnsF(())
      (slave.create _).expects(sub).returnsF(())

      dao.create(sub).futureValue
      waitEffectsReady()
    }
  }

  "listByUser" should {
    "migrate subscriptions they don't exist in slave" in new Test {
      (main.listByUser _).expects(user).returnsF(subs)
      (slave.listByUser _).expects(user).returnsF(Nil)
      subs.foreach { sub =>
        (slave.create _).expects(sub).returnsF(())
      }

      dao.listByUser(user).futureValue should contain theSameElementsAs subs
      waitEffectsReady()
    }

    "do nothing if slave is in sync" in new Test {
      (main.listByUser _).expects(user).returnsF(subs)
      (slave.listByUser _).expects(user).returnsF(subs)

      dao.listByUser(user).futureValue should contain theSameElementsAs subs
      waitEffectsReady()
    }

    "create missing subs and delete extra" in new Test {
      val subToAdd = CoreGenerators.subscriptions.next.toBuilder.setUser(user).build()
      val subToDelete = CoreGenerators.subscriptions.next.toBuilder.setUser(user).build()
      val mainSubs: Seq[Model.Subscription] = subs :+ subToAdd

      (main.listByUser _).expects(user).returnsF(mainSubs)
      (slave.listByUser _).expects(user).returnsF(subs :+ subToDelete)
      (slave.create _).expects(subToAdd).returnsF(())
      (slave.delete(_: User, _: SubscriptionId)).expects(subToDelete.getUser, subToDelete.getId).returnsF(())

      dao.listByUser(user).futureValue should contain theSameElementsAs mainSubs
      waitEffectsReady()
    }
  }

  private def waitEffectsReady(): Unit = {
    eventually {
      (1 to 10).foreach { _ =>
        ecExecutor.getActiveCount shouldBe 0
        Thread.sleep(10)
      }
    }
  }
}
