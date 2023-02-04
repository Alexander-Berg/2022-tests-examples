package ru.auto.api.managers.lenta

import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoMoreInteractions}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.auth.Application
import ru.auto.api.model.subscriptions.AutoruLentaSubscriptionsDomain
import ru.auto.api.model.{AutoruUser, RequestParams}
import ru.auto.api.services.subscriptions.SubscriptionClient
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.lenta.ApiModel.{Subscription => OuterSubscription}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future
import scala.jdk.CollectionConverters.IterableHasAsJava

class LentaSubscriptionsManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with BeforeAndAfter {

  private val subscriptionClient = mock[SubscriptionClient]

  implicit val trace: Traced = Traced.empty

  implicit val request: Request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Some(Gen.identifier.next)))
    r.setTrace(trace)
    r.setApplication(Application.iosApp)
    r
  }

  before {
    Mockito.reset(subscriptionClient)
  }

  "LentaSubscriptionsManager" should {
    "set" in {

      val user = AutoruUser(123)

      val existingSubscription = OuterSubscription
        .newBuilder()
        .addAllIncludeTags(Iterable("tag1", "tag2", "tag3").asJava)
        .build()

      val newSubscription = OuterSubscription
        .newBuilder()
        .addAllIncludeTags(Iterable("tag3", "tag4", "tag5").asJava)
        .build()

      val innerExistingSubscription =
        LentaManagerSpec.makeSubscription(existingSubscription).toBuilder.setId("existing").build()
      val innerNewSubscription = LentaManagerSpec.makeSubscription(newSubscription).toBuilder.setId("new").build()

      val query = LentaManagerSpec.makeResultQuery(newSubscription)

      when(subscriptionClient.upsertSubscription(?, ?, ?)(?)).thenReturn(Future(innerNewSubscription))
      when(subscriptionClient.getUserSubscriptions(?, ?)(?))
        .thenReturn(Future(Seq(innerExistingSubscription, innerNewSubscription)))
      when(subscriptionClient.deleteSubscription(?, ?, ?)(?)).thenReturn(Future.unit)

      val manager = new LentaSubscriptionsManager(subscriptionClient)

      val result = manager.set(user, query).futureValue

      result shouldBe innerNewSubscription

      verify(subscriptionClient, times(1)).upsertSubscription(?, ?, ?)(?)
      verify(subscriptionClient, times(1)).getUserSubscriptions(user, AutoruLentaSubscriptionsDomain)
      verify(subscriptionClient, times(1)).deleteSubscription(user, "existing", AutoruLentaSubscriptionsDomain)
      verifyNoMoreInteractions(subscriptionClient)
    }
  }

}
