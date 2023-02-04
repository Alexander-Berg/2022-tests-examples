package ru.yandex.vertis.subscriptions.scheduler.tasks

import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.vertis.subscriptions.core.plugin.RequestParser
import ru.yandex.vertis.subscriptions.model.{getSubscriptionId, Watch}
import ru.yandex.vertis.subscriptions.model.owner.Owner
import ru.yandex.vertis.subscriptions.storage.memory.SubscriptionsDao
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators
import ru.yandex.vertis.subscriptions.{Model, SpecBase, TestExecutionContext}

/**
  * Tests for [[ActualizeQueriesTask]]
  *
  * @author zvez
  */
@RunWith(classOf[JUnitRunner])
class ActualizeQueriesTaskSpec extends SpecBase with BeforeAndAfter with TestExecutionContext {

  private val subscriptionsDao = new SubscriptionsDao
  private val requestParser = mock(classOf[RequestParser])

  private val task = new ActualizeQueriesTask(subscriptionsDao, requestParser) {
    override protected def needActualise(sub: Model.Subscription): Boolean = true
  }
  private val subscriptionsGen = CoreGenerators.subscriptionGen.suchThat(v => !Watch.isWatch(v))

  before {
    reset(requestParser)
  }

  "ActualizeQueriesTask" should {
    "do nothing when there are nothing to do" in {
      task.run().futureValue
      verifyNoMoreInteractions(requestParser)
    }

    "actualize subscription's request" in {
      val initialTime = System.currentTimeMillis()
      val sub = subscriptionsGen.next
      subscriptionsDao.create(sub).futureValue
      val newRequest = CoreGenerators.requestGen.next
      when(requestParser.parse(any())).thenReturn(newRequest)

      val expectedNewId =
        getSubscriptionId(
          Owner.fromLegacyUser(sub.getUser),
          newRequest,
          if (sub.hasQualifier) Some(sub.getQualifier) else None
        )

      task.run().futureValue
      val updatedSub = subscriptionsDao.get(expectedNewId).futureValue
      updatedSub.getRequest.getLastUpdated should be > initialTime
      updatedSub.getRequest.getQuery should be(newRequest.getQuery)

      subscriptionsDao.get(sub.getId).failed.futureValue shouldBe an[NoSuchElementException]
    }

    "not touch subscription if query hasn't changed" in {
      val sub = subscriptionsGen.next
      subscriptionsDao.create(sub).futureValue
      when(requestParser.parse(any())).thenReturn(sub.getRequest)

      task.run().futureValue
      val loadedSubscription = subscriptionsDao.get(sub.getId).futureValue
      loadedSubscription shouldBe sub
    }

    /* "actualize only subscriptions which were not updated for 'actualDuration'" in {
      when(requestParser.parse(any())).thenReturn(CoreGenerators.requestGen.next)

      task.run().get
      verifyNoMoreInteractions(requestParser)

      val sub = {
        val lastUpdated = DateTime.now().minusDays(2).getMillis
        val s = CoreGenerators.subscriptions.next
        s.toBuilder
          .setRequest(s.getRequest.toBuilder.setLastUpdated(lastUpdated))
          .build()
      }
      subscriptionsDao.create(sub).futureValue

      task.run().get

      val clueBuilder = SubscriptionClue.newBuilder()
        .setRequestSource(sub.getRequest.getSource)
        .setDelivery(sub.getDelivery)
      if (sub.hasQualifier)
        clueBuilder.setQualifier(sub.getQualifier)

      verify(requestParser, times(1)).parse(clueBuilder.build())
      verifyNoMoreInteractions(requestParser)
    }*/
  }
}
