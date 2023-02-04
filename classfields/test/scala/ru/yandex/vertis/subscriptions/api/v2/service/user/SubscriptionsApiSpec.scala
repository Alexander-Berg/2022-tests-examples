package ru.yandex.vertis.subscriptions.api.v2.service.user

import akka.testkit.TestProbe
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.subscriptions.{Model, SpecBase, TestExecutionContext}
import ru.yandex.vertis.subscriptions.Model.State
import ru.yandex.vertis.subscriptions.api.RouteTestWithConfig
import ru.yandex.vertis.subscriptions.backend.confirmation.impl.{EmitMailConfirmation, EmitSubscriptionsConfirmationViaActor, MailConfirmationService, MemoryUserProperties, SubscriptionConfirmationService}
import ru.yandex.vertis.subscriptions.backend.user.impl.{UserServiceImpl, UserSubscriptionsServiceImpl}
import ru.yandex.vertis.subscriptions.backend.user.logging.LoggingUserService
import ru.yandex.vertis.subscriptions.core.plugin.TrivialRequestParser
import ru.yandex.vertis.subscriptions.model.UserKey
import ru.yandex.vertis.subscriptions.service.impl.JvmDraftService
import ru.yandex.vertis.subscriptions.storage.memory.{InMemoryUserLinks, SubscriptionsDao}
import ru.yandex.vertis.subscriptions.util.AsyncService
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators._
import ru.yandex.vertis.subscriptions.view.json.{SourceView, SubscriptionUpdateView, SubscriptionView, ViewDetailsView}

import spray.http.StatusCodes

import scala.concurrent.ExecutionContext

/**
  * Tests for [[SubscriptionsApi]]
  *
  * @author zvez
  */
@RunWith(classOf[JUnitRunner])
class SubscriptionsApiSpec extends SpecBase with RouteTestWithConfig with TestExecutionContext {

  //todo probably most of these are not needed

  val subscriptionConfirmationRequester = TestProbe()
  val mailConfirmationRequester = TestProbe()

  val userSubscriptions = new MemoryUserProperties
  val userMails = new MemoryUserProperties

  val subscriptionConfirmation = new SubscriptionConfirmationService(userSubscriptions)
    with EmitSubscriptionsConfirmationViaActor {
    val confirmationRequester = subscriptionConfirmationRequester.ref
  }

  val emailConfirmation = new MailConfirmationService(userMails) with EmitMailConfirmation {
    val confirmationRequester = mailConfirmationRequester.ref
  }

  val dao = new SubscriptionsDao

  val userLinks = new InMemoryUserLinks

  val userSubscriptionsService =
    new UserSubscriptionsServiceImpl(
      dao,
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
    emailConfirmation,
    drafts,
    lightWeightEc = ec,
    blockingEc = ec
  ) with LoggingUserService

  private val route = seal {
    new SubscriptionsApi(userService)(ExecutionContext.global).route
  }

  "/user/*/subscriptions" should {

    val user = Users.next
    val userKey = UserKey(user)
    val source = SourceView(anySubscriptionsSources.next)
    var subscription: SubscriptionView = null

    "list empty subscriptions" in {
      Get(s"/$userKey/subscriptions") ~> route ~> check {
        responseAs[Seq[SubscriptionView]] should be(Nil)
      }
    }

    "create subscription" in {
      Post(s"/$userKey/subscriptions", source) ~> route ~> check {
        subscription = responseAs[SubscriptionView]
        checkCorrespond(subscription, source)
      }
    }

    "get created subscription" in {
      Get(s"/$userKey/subscriptions") ~> route ~> check {
        responseAs[Seq[SubscriptionView]] should be(Seq(subscription))
      }
      Get(s"/$userKey/subscriptions/${subscription.id}") ~> route ~> check {
        responseAs[SubscriptionView] should be(subscription)
      }
    }

    val updatedSource = {
      val builder = source.asModel.toBuilder
      val otherSource = anySubscriptionsSources.next
      builder.setView(otherSource.getView)
      builder.setDelivery(otherSource.getDelivery)
      builder.build()
    }
    var updatedSubscription: SubscriptionView = null

    "update subscription" in {
      val request = SubscriptionUpdateView(updatedSource)
      Put(s"/$userKey/subscriptions/${subscription.id}", request) ~> route ~> check {
        updatedSubscription = responseAs[SubscriptionView]
        checkCorrespond(updatedSubscription, SourceView(updatedSource))
      }

      Get(s"/$userKey/subscriptions/${updatedSubscription.id}") ~> route ~> check {
        responseAs[SubscriptionView] should be(updatedSubscription)
      }
    }

    "delete subscription" in {
      Delete(s"/$userKey/subscriptions/${updatedSubscription.id}") ~> route ~> check {
        status should be(StatusCodes.OK)
      }
      Get(s"/$userKey/subscriptions/${updatedSubscription.id}") ~> route ~> check {
        status should be(StatusCodes.NotFound)
      }
    }

    "create subscription in active state" in {
      val source = SourceView(anySubscriptionsSources.next).copy(state = Some(State.Value.ACTIVE))
      Post(s"/$userKey/subscriptions", source) ~> route ~> check {
        subscription = responseAs[SubscriptionView]
        checkCorrespond(subscription, source)
        subscription.state should be(State.Value.ACTIVE.name())
      }
    }
  }

  "/user/*/subscriptions (push subscriptions)" should {
    val user = Users.next
    val userKey = UserKey(user)
    val source = SourceView(pushSubscriptionsSources.next)
    var subscription: SubscriptionView = null

    "create subscription in ACTIVE state" in {
      Post(s"/$userKey/subscriptions", source) ~> route ~> check {
        subscription = responseAs[SubscriptionView]
        checkCorrespond(subscription, source)
        subscription.state should be(Model.State.Value.ACTIVE.name())
        subscription.disabled should be(None)
      }
    }

    "update subscription view should not change state" in {
      val updatedView =
        ViewDetailsView(Some("updated title"), body = Some("updated body"), None, None, None, None, None)
      val request = SubscriptionUpdateView(None, Some(updatedView), None)

      Put(s"/$userKey/subscriptions/${subscription.id}", request) ~> route ~> check {
        val updatedSubscription = responseAs[SubscriptionView]
        updatedSubscription.view should be(updatedView)
        updatedSubscription.state should be(Model.State.Value.ACTIVE.name())
        updatedSubscription.disabled should be(None)
      }
    }

    val updatedSource = {
      val builder = source.asModel.toBuilder
      val otherSource = pushSubscriptionsSources.next
      builder.setView(otherSource.getView)
      builder.setDelivery(otherSource.getDelivery)
      builder.build()
    }

    "update subscription should not change state" in {
      val request = SubscriptionUpdateView(updatedSource)
      var updatedSubscription: SubscriptionView = null
      Put(s"/$userKey/subscriptions/${subscription.id}", request) ~> route ~> check {
        updatedSubscription = responseAs[SubscriptionView]
        checkCorrespond(updatedSubscription, SourceView(updatedSource))
        updatedSubscription.state should be(Model.State.Value.ACTIVE.name())
        updatedSubscription.disabled should be(None)
      }

      Get(s"/$userKey/subscriptions/${updatedSubscription.id}") ~> route ~> check {
        responseAs[SubscriptionView] should be(updatedSubscription)
      }
    }

    "disable subscription" in {
      val request = SubscriptionUpdateView(disabled = Some(true))
      var updatedSubscription: SubscriptionView = null
      Put(s"/$userKey/subscriptions/${subscription.id}", request) ~> route ~> check {
        updatedSubscription = responseAs[SubscriptionView]
        checkCorrespond(updatedSubscription, SourceView(updatedSource))
        updatedSubscription.state should be(Model.State.Value.ACTIVE.name())
        updatedSubscription.disabled should be(Some(true))
      }

      Get(s"/$userKey/subscriptions/${updatedSubscription.id}") ~> route ~> check {
        responseAs[SubscriptionView] should be(updatedSubscription)
      }
    }

    "enable subscription" in {
      val request = SubscriptionUpdateView(disabled = Some(false))
      var updatedSubscription: SubscriptionView = null
      Put(s"/$userKey/subscriptions/${subscription.id}", request) ~> route ~> check {
        updatedSubscription = responseAs[SubscriptionView]
        checkCorrespond(updatedSubscription, SourceView(updatedSource))
        updatedSubscription.state should be(Model.State.Value.ACTIVE.name())
        updatedSubscription.disabled should be(Some(false))
      }

      Get(s"/$userKey/subscriptions/${updatedSubscription.id}") ~> route ~> check {
        responseAs[SubscriptionView] should be(updatedSubscription)
      }
    }

    "disable all subscriptions" in {
      Post(s"/$userKey/subscriptions/disableAll") ~> route ~> check {
        status shouldBe StatusCodes.OK
      }

      Get(s"/$userKey/subscriptions/${subscription.id}") ~> route ~> check {
        val updated = responseAs[SubscriptionView]
        updated.disabled shouldBe Some(true)
      }
    }

    "enable all subscriptions" in {
      Post(s"/$userKey/subscriptions/enableAll") ~> route ~> check {
        status shouldBe StatusCodes.OK
      }

      Get(s"/$userKey/subscriptions/${subscription.id}") ~> route ~> check {
        val updated = responseAs[SubscriptionView]
        updated.disabled shouldBe Some(false)
      }
    }
  }

  def checkCorrespond(subscription: SubscriptionView, source: SourceView): Unit = {
    subscription.delivery.email should be(source.emailDelivery)
    subscription.delivery.push should be(source.pushDelivery)
    subscription.request.httpQuery should be(source.httpQuery)
    subscription.view.title should be(source.title)
    subscription.view.body should be(source.body)
  }
}
