package ru.yandex.vertis.subscriptions.scheduler.tasks

import ru.yandex.vertis.subscriptions.Model.{Delivery, State, Subscription, TimeUnit}
import ru.yandex.vertis.subscriptions.backend.confirmation.ReconfirmationService
import ru.yandex.vertis.subscriptions.backend.transport.SendAlways
import ru.yandex.vertis.subscriptions.storage.memory.SubscriptionsDao
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators._
import ru.yandex.vertis.subscriptions.{Model, SpecBase, TestExecutionContext}

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.junit.JUnitRunner

import scala.util.Success

/**
  * Tests for [[ReconfirmOldSubscriptionsTask]]
  *
  * @author zvez
  */
//@RunWith(classOf[JUnitRunner])
class ReconfirmOldSubscriptionsTaskSpec extends SpecBase with BeforeAndAfter with TestExecutionContext {

  val confirmationService = mock(classOf[ReconfirmationService[Subscription]], Answers.RETURNS_SMART_NULLS)
  val subscriptionsDao = new SubscriptionsDao
  val task = new ReconfirmOldSubscriptionsTask(subscriptionsDao, confirmationService, SendAlways)
  val now = DateTime.now()

  before {
    reset(confirmationService)
    when(confirmationService.requestReconfirmation(any(), any()))
      .thenReturn(Success(null))
  }

  "ReconfirmOldSubscriptionsTask" should {
    "not do anything if there is no old subscriptions" in {
      subscriptionsDao
        .create(makeSubscription(State.Value.ACTIVE, now))
        .futureValue
      subscriptionsDao
        .create(makeSubscription(State.Value.ACTIVE, now.minusMonths(5)))
        .futureValue
      subscriptionsDao
        .create(makeSubscription(State.Value.ACTIVE, now.minusMonths(11)))
        .futureValue
      task.run().futureValue
      verifyNoMoreInteractions(confirmationService)
    }

    "send confirmation for old subscriptions and mark them as AWAIT_CONFIRMATION" in {
      val oldSub = makeSubscription(State.Value.ACTIVE, now.minusMonths(13))
      subscriptionsDao.create(oldSub).futureValue
      task.run().futureValue
      verify(confirmationService).requestReconfirmation(oldSub.getUser, oldSub)
      verifyNoMoreInteractions(confirmationService)

      val updatedSub = subscriptionsDao.get(oldSub.getId).futureValue
      updatedSub.getState.getValue should be(State.Value.AWAIT_CONFIRMATION)
      updatedSub.getState.getTimestamp should be > now.getMillis
    }

    "not send anything for old not active subscriptions" in {
      subscriptionsDao
        .create(makeSubscription(State.Value.AWAIT_CONFIRMATION, now.minusMonths(24)))
        .futureValue
      task.run().futureValue
      verifyNoMoreInteractions(confirmationService)
    }

    "not try to send anything for non-email subscriptions" in {
      val oldSub = {
        val sub = makeSubscription(State.Value.ACTIVE, now.minusMonths(13))
        val push = Delivery.Push
          .newBuilder()
          .setToken("123")
          .setPeriod(
            Model.Duration
              .newBuilder()
              .setLength(12)
              .setTimeUnit(TimeUnit.HOURS)
          )
        sub.toBuilder
          .setDelivery(sub.getDelivery.toBuilder.clearEmail().setPush(push))
          .build()
      }
      subscriptionsDao.create(oldSub).futureValue
      task.run().futureValue
      verifyNoMoreInteractions(confirmationService)
    }
  }

  private def makeSubscription(s: State.Value, date: DateTime) = {
    val state = State.newBuilder().setValue(s).setTimestamp(date.getMillis)
    emailSubscriptions.next.toBuilder
      .setState(state)
      .build()
  }

}
