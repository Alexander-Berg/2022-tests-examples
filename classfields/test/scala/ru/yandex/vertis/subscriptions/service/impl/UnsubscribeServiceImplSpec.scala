package ru.yandex.vertis.subscriptions.service.impl

import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.vertis.subscriptions.{SlowAsyncSpec, SpecBase}
import ru.yandex.vertis.subscriptions.model._
import ru.yandex.vertis.subscriptions.service.WatchService
import ru.yandex.vertis.subscriptions.storage.SubscriptionsDao
import ru.yandex.vertis.subscriptions.util.SecurityViolationException

import scala.concurrent.Future

/**
  * Runnable specs on [[UnsubscribeServiceImpl]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class UnsubscribeServiceImplSpec
  extends SpecBase
  with SlowAsyncSpec
  with PropertyChecks
  with BeforeAndAfter
  with BeforeAndAfterAll {

  val Watch: Future[Watch] = Future.successful(ModelGenerators.watch.next)

  val watchService: WatchService = mock(classOf[WatchService])
  val subscriptionDao: SubscriptionsDao = mock(classOf[SubscriptionsDao])
  val unsubscribeService = new UnsubscribeServiceImpl(watchService, subscriptionDao)

  before {
    when(watchService.deleteDeliveries(any(), any())).thenReturn(Watch)
    when(subscriptionDao.delete(any(), any())).thenReturn(Future.successful(()))
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    reset(watchService)
    reset(subscriptionDao)
  }

  "UnsubscribeServiceImpl" should {
    "unsubscribe watch with correct secret" in {
      forAll(UnsubscribeGenerators.watchUnsubscribe) { unsubscribe =>
        val correctSecret = Signing.unsubscribeSecret(unsubscribe.owner)
        val token = unsubscribeService.token(unsubscribe.copy(secret = correctSecret))
        unsubscribeService.unsubscirbe(token).futureValue
        verify(watchService)
          .deleteDeliveries(unsubscribe.owner, unsubscribe.deliveries)
      }
    }

    "not unsubscribe watch with incorrect secret" in {
      forAll(UnsubscribeGenerators.watchUnsubscribe) { unsubscribe =>
        val incorrectSecret = "some trash secret"
        val token = unsubscribeService.token(unsubscribe.copy(secret = incorrectSecret))
        intercept[SecurityViolationException] {
          cause(unsubscribeService.unsubscirbe(token).futureValue)
        }
        verifyZeroInteractions(watchService)
      }
    }

    "not unsubscribe watch with incorrect token" in {
      val token = "some trash token"
      intercept[IllegalArgumentException] {
        cause(unsubscribeService.unsubscirbe(token).futureValue)
      }
      verifyZeroInteractions(watchService)
    }

    "unsubscribe subscription with correct secret" in {
      forAll(LegacyGenerators.subscriptionGen, UnsubscribeGenerators.subscriptionUnsubscribe) { (sub, unsubscribe) =>
        val subscription = sub.toBuilder.setId(unsubscribe.id).build()
        val correctSecret = Signing.unsubscribeSecret(unsubscribe.id)

        when(subscriptionDao.get(any())).thenReturn(Future.successful(subscription))

        val token = unsubscribeService.token(unsubscribe.copy(secret = correctSecret))
        unsubscribeService.unsubscirbe(token).futureValue
        verify(subscriptionDao).get(unsubscribe.id)
        verify(subscriptionDao).delete(subscription.getUser, unsubscribe.id)
      }
    }

    "not unsubscribe subscription with incorrect secret" in {
      forAll(UnsubscribeGenerators.subscriptionUnsubscribe) { unsubscribe =>
        val incorrectSecret = "some trash secret"
        val token = unsubscribeService.token(unsubscribe.copy(secret = incorrectSecret))
        intercept[SecurityViolationException] {
          cause(unsubscribeService.unsubscirbe(token).futureValue)
        }
        verifyZeroInteractions(subscriptionDao)
      }
    }

    "not unsubscribe subscription with incorrect token" in {
      val token = "some trash token"
      intercept[IllegalArgumentException] {
        cause(unsubscribeService.unsubscirbe(token).futureValue)
      }
      verifyZeroInteractions(subscriptionDao)
    }
  }

}
