package ru.yandex.vertis.subscriptions.api.v3.service.subscription

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.subscriptions.{Model, SpecBase}
import ru.yandex.vertis.subscriptions.api.RouteTestWithConfig
import ru.yandex.vertis.subscriptions.storage.memory.SubscriptionsDao
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators.anySubscriptionsSources
import ru.yandex.vertis.subscriptions.view.json.{SourceView, SubscriptionView}
import ru.yandex.vertis.generators.ProducerProvider._
import spray.http.StatusCodes

import scala.concurrent.ExecutionContext
import scala.util.Random

/**
  * @author rs-pluss
  */
@RunWith(classOf[JUnitRunner])
class SubscriptionHandlerSpec extends SpecBase with RouteTestWithConfig {

  val source = SourceView(anySubscriptionsSources.next)
  var subscription: Model.Subscription = CoreGenerators.subscriptionGen.next
  val dao = new SubscriptionsDao
  dao.create(subscription)

  private val route = seal {
    new SubscriptionHandler(dao, "")(ExecutionContext.global).route
  }

  "get internal" should {
    "return 200 ok" in {
      Get(s"/${subscription.getId}/internal") ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }
  }

  "emails" should {
    "be correctly changed" in {
      generateTestChangeEmail("dima@yandex.ua", "dima@yandex.com", Random.nextBoolean(), true)
      generateTestChangeEmail("dima@yandex.go", "dima@yandex.go", Random.nextBoolean(), false)
      generateTestChangeEmail("dima@yandex.ua@yandex.ua", "dima@yandex.ua@yandex.com", Random.nextBoolean(), true)
      generateTestChangeEmail("dima@yandex.uayandex.ua", "dima@yandex.uayandex.ua", Random.nextBoolean(), false)
      generateTestChangeEmail("dima@gmail.com", "dima@gmail.com", Random.nextBoolean(), false)
    }
  }

  val serviceName = "auto"
  val userUid = "123"

  private def generateTestChangeEmail(
      testEmail: String,
      expectedEmail: String,
      expectedBrokerDisabled: Boolean,
      shouldBeReplaced: Boolean): Unit = {
    val subs = generateSubs(testEmail, expectedBrokerDisabled)
    val updatedSubs = SubscriptionHandler.changeUaDomainEmail(subs)

    SubscriptionHandler.shouldReplaceUaDomain(subs) shouldBe shouldBeReplaced

    updatedSubs.getDelivery.getEmail.getAddress shouldBe expectedEmail
    updatedSubs.getDelivery.getBroker.getDisabled shouldBe expectedBrokerDisabled
    updatedSubs.getId shouldBe subs.getId
    updatedSubs.getService shouldBe subs.getService
    updatedSubs.getService shouldBe serviceName
    updatedSubs.getUser.getUid shouldBe subs.getUser.getUid
    updatedSubs.getUser.getUid shouldBe userUid
  }

  private def generateSubs(email: String, expectedBrokerDisabled: Boolean): Model.Subscription = {
    Model.Subscription
      .newBuilder()
      .setId(Random.nextLong().toString)
      .setService(serviceName)
      .setUser(Model.User.newBuilder().setUid(userUid))
      .setView(Model.View.newBuilder())
      .setRequest(Model.Request.newBuilder())
      .setState(Model.State.newBuilder().setValue(Model.State.Value.ACTIVE).setTimestamp(Random.nextLong()))
      .setDelivery(
        Model.Delivery
          .newBuilder()
          .setBroker(Model.Delivery.Broker.newBuilder().setDisabled(expectedBrokerDisabled))
          .setEmail(
            Model.Delivery.Email
              .newBuilder()
              .setAddress(email)
              .setPeriod(Model.Duration.newBuilder().setLength(100).setTimeUnit(Model.TimeUnit.MINUTES))
          )
      )
      .build()
  }

}
