package ru.yandex.vertis.subscriptions

import java.io.IOException

import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import ru.yandex.vertis.subscriptions.ApiClient.{AlreadyExistsException, NotFoundException}
import ru.yandex.vertis.subscriptions.Model.{AutoruUser, OuterDelivery, OuterSubscriptionSource, OuterView, RequestSource, User}

import scala.util.{Failure, Success}

/**
  * Tests for [[ApiClientImpl]]
  *
  * @author zvez
  */
class ApiClientSpec extends WordSpecLike with Matchers with BeforeAndAfterAll {

  val client = new ApiClientImpl("http://csback01ht.vs.yandex.net:36134/api/1.x/subscriptions/service/auto/user")
//  val client = new ApiClientImpl("http://localhost:36134/api/1.x/subscriptions/service/auto/user")

  val user = User
    .newBuilder()
    .setAutoruUser(AutoruUser.newBuilder().setSessionId("testuser"))
    .build()

  val subscription = buildSubscriptionSource()

  "ApiClient" should {
    "successfully create subscription" in {
      client.create(user, subscription) should matchPattern {
        case Success(_) =>
      }
    }

    "fail with AlreadyExistsException on duplicate subscription" in {
      client.create(user, subscription) should matchPattern {
        case Failure(ex: AlreadyExistsException) =>
      }
    }

    "fail with IllegalArgumentException on illegal source" in {
      val source = buildSubscriptionSource().toBuilder.clearRequest().build()
      client.create(user, source) should matchPattern {
        case Failure(ex: IllegalArgumentException) =>
      }
    }

    "fail with NotFoundException on unknown subscription id" in {
      client.update(user, "some-unknown-id", subscription) should matchPattern {
        case Failure(ex: NotFoundException) =>
      }
    }

    "fail with IOException on network errors" in {
      val brokenClient = new ApiClientImpl("http://localhost:9999/api/1.x/subscriptions/service/auto/user")
      brokenClient.update(user, "some-unknown-id", subscription) should matchPattern {
        case Failure(ex: IOException) =>
      }
    }
  }

  override protected def beforeAll() = {
    val Success(subscriptions) = client.list(user)
    subscriptions.foreach(s => client.delete(user, s.getId))
  }

  private def buildSubscriptionSource() = {
    val delivery = OuterDelivery
      .newBuilder()
      .setEmail(
        OuterDelivery.Email
          .newBuilder()
          .setAddress("foo")
          .setPeriod(10)
      )

    val requestSource = RequestSource
      .newBuilder()
      .setHttpQuery("customs_state=1&image=true&is_clear=false&rid=2&state=NEW&state=USED")
    val state = ModelDSL.createState(Model.State.Value.ACTIVE)
    val view = OuterView.newBuilder().setTitle("test").setBody("test")
    OuterSubscriptionSource
      .newBuilder()
      .setDelivery(delivery)
      .setRequest(requestSource)
      .setState(state)
      .setView(view)
      .build()

  }
}
