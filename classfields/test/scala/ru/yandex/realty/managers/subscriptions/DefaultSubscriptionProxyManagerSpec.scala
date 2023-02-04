package ru.yandex.realty.managers.subscriptions

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.{JsObject, JsValue, Json}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.subscription.SubscriptionsClient
import ru.yandex.realty.model.subscriptions.ConfirmSubscriptionQuery
import ru.yandex.realty.model.user.UserRef
import ru.yandex.realty.subscriptions.proxy._
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class DefaultSubscriptionProxyManagerSpec extends AsyncSpecBase {

  private class Fixture {
    val subscriptionClient: SubscriptionsClient = mock[SubscriptionsClient]
    val subscriptionProxyManager = new DefaultSubscriptionProxyManager(subscriptionClient)

    implicit val traced: Traced = mock[Traced]

    val PassportUserRef: UserRef = UserRef.passport(1234)
    val EmailValue = "some@w.com"
    val ConfirmSubscriptionQueryValue = ConfirmSubscriptionQuery("token", EmailValue, "abc")

    def subscriptionRequest(isUserAgreed: Boolean = true): CreateSubscriptionRequest =
      CreateSubscriptionRequest
        .newBuilder()
        .setIsUserAgreed(isUserAgreed)
        .setSubscriptionRequest(
          CreateSubscriptionApiRequest
            .newBuilder()
            .setEmailDelivery(
              EmailDelivery
                .newBuilder()
                .setMail(EmailValue)
            )
        )
        .build()

    private def subscriptionResponse(isEmailConfirmed: Boolean): JsValue = {
      val subscriptionState =
        if (isEmailConfirmed) SubscriptionState.ACTIVE
        else SubscriptionState.AWAIT_CONFIRMATION
      Json.obj("id" -> "abc", "state" -> subscriptionState.name)
    }

    private def confirmResponse(isEmailConfirmed: Boolean): JsValue =
      Json.obj("message" -> isEmailConfirmed)

    def mockCalls(
      isAgreedEarlier: Boolean = true,
      checkAgreementCalls: Int = 0,
      saveAgreementCalls: Int = 0,
      ensureEmailCalls: Int = 0,
      approveEmailCalls: Int = 0,
      saveCategoriesCalls: Int = 0,
      createSubscriptionCalls: Int = 1,
      confirmSubscriptionCalls: Int = 0,
      isEmailConfirmed: Boolean = false
    ) {

      (subscriptionClient
        .createSubscription(_: String, _: JsObject)(_: Traced))
        .expects(*, *, *)
        .repeated(createSubscriptionCalls)
        .returns(Future.successful(subscriptionResponse(isEmailConfirmed)))

      (subscriptionClient
        .confirmSubscription(_: String, _: ConfirmSubscriptionQuery)(_: Traced))
        .expects(*, *, *)
        .repeated(confirmSubscriptionCalls)
        .returns(Future.successful(confirmResponse(isEmailConfirmed)))
    }
  }

  "DefaultSubscriptionProxyManager" should {

    "not fail with UserAgreementIsAbsent" in new Fixture {
      mockCalls(isAgreedEarlier = false)

      subscriptionProxyManager
        .createSubscription(PassportUserRef, subscriptionRequest(isUserAgreed = false))
        .futureValue
    }

    "save user agreement when agreed first time" in new Fixture {
      mockCalls(
        isAgreedEarlier = false,
        checkAgreementCalls = 1,
        saveAgreementCalls = 1,
        ensureEmailCalls = 1,
        saveCategoriesCalls = 1
      )

      subscriptionProxyManager.createSubscription(PassportUserRef, subscriptionRequest()).futureValue
    }

    "approve email by default when subscription response contains active state" in new Fixture {
      mockCalls(
        checkAgreementCalls = 1,
        ensureEmailCalls = 1,
        approveEmailCalls = 1,
        saveCategoriesCalls = 1,
        isEmailConfirmed = true
      )

      subscriptionProxyManager.createSubscription(PassportUserRef, subscriptionRequest()).futureValue
    }

    "approve email when it is confirmed successfully" in new Fixture {
      mockCalls(
        createSubscriptionCalls = 0,
        confirmSubscriptionCalls = 1,
        approveEmailCalls = 1,
        isEmailConfirmed = true
      )

      subscriptionProxyManager
        .confirmSubscription(PassportUserRef, PassportUserRef, ConfirmSubscriptionQueryValue)
        .futureValue
    }

    "not approve email when its confirmation fails" in new Fixture {
      mockCalls(createSubscriptionCalls = 0, confirmSubscriptionCalls = 1)

      subscriptionProxyManager
        .confirmSubscription(PassportUserRef, PassportUserRef, ConfirmSubscriptionQueryValue)
        .futureValue
    }
  }
}
