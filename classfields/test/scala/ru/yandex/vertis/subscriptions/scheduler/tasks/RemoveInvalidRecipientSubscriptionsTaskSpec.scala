package ru.yandex.vertis.subscriptions.scheduler.tasks

import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.vertis.subscriptions.storage.memory.{InMemoryInvalidRecipientFailuresDao, SubscriptionsDao}
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators
import ru.yandex.vertis.subscriptions.{SlowAsyncSpec, SpecBase}

import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.junit.JUnitRunner

/**
  * Tests for [[RemoveInvalidRecipientSubscriptionsTask]]
  *
  * @author zvez
  */
@RunWith(classOf[JUnitRunner])
class RemoveInvalidRecipientSubscriptionsTaskSpec extends SpecBase with SlowAsyncSpec with BeforeAndAfter {

  import scala.concurrent.ExecutionContext.Implicits.global

  val failuresDao = new InMemoryInvalidRecipientFailuresDao
  val subscriptionsDao = new SubscriptionsDao
  val subscriptionsDaoSpy = spy(subscriptionsDao)
  val task = new RemoveInvalidRecipientSubscriptionsTask(subscriptionsDaoSpy, failuresDao, 2)

  val someSub = CoreGenerators.pushSubscriptionGen.next

  before {
    reset(subscriptionsDaoSpy)
  }

  "RemoveInvalidRecipientSubscriptionsTask" should {
    "not do anything if there is nothing to do" in {
      task.run().futureValue
      verifyNoMoreInteractions(subscriptionsDaoSpy)
    }

    "not touch subscriptions if fail number doesn't reach threshold" in {
      subscriptionsDao.create(someSub)
      failuresDao.increaseFailed(someSub.getId)
      task.run().futureValue
      verifyNoMoreInteractions(subscriptionsDaoSpy)
    }

    "delete subscription which fails way too much" in {
      failuresDao.increaseFailed(someSub.getId)
      task.run().futureValue
      verify(subscriptionsDaoSpy, times(1)).delete(someSub.getUser, someSub.getId)

      subscriptionsDao.get(someSub.getId).failed.futureValue shouldBe a[NoSuchElementException]
      failuresDao.all().futureValue shouldBe empty
    }
  }
}
