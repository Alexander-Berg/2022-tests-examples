package ru.yandex.vertis.subscriptions.api.v2.service.user

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{fixture, Matchers}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.subscriptions.api.RouteTestWithConfig
import ru.yandex.vertis.subscriptions.backend.confirmation.Confirmation
import ru.yandex.vertis.subscriptions.backend.user.UserService
import ru.yandex.vertis.subscriptions.model.UserKey
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators.Users
import ru.yandex.vertis.generators.ProducerProvider._

import spray.http.StatusCodes

import scala.concurrent.Future
import scala.util.Success

/**
  *
  * @author zvez
  */
@RunWith(classOf[JUnitRunner])
class ConfirmationApiSpec extends Matchers with fixture.WordSpecLike with RouteTestWithConfig with MockitoSupport {

  class FixtureParam {
    val userService = mock[UserService]
    val route = seal(new ConfirmationApi(userService).route)

    val user = Users.next
    val userKey = UserKey(user)
    val email = CoreGenerators.emailAddressGen.next
    val subscriptionId = CoreGenerators.idGen.next
    val token = CoreGenerators.idGen.next
  }

  override def withFixture(test: OneArgTest) = {
    withFixture(test.toNoArgTest(new FixtureParam))
  }

  "/user/*/confirmation" should {
    pending
    "request email confirmation" in { env =>
      import env.{eq => _, _}
      when(userService.requestEmailConfirmation(eq(user), eq(email), eq(false)))
        .thenReturn(Future.successful(()))

      Post(s"/$userKey/confirmation/request?email=$email") ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "request subscription confirmation" in { env =>
      import env.{eq => _, _}
      when(userService.requestSubscriptionConfirmation(eq(user), eq(subscriptionId)))
        .thenReturn(Future.successful(()))

      Post(s"/$userKey/confirmation/request?id=$subscriptionId") ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "confirm email" in { env =>
      import env.{eq => _, _}
      when(userService.confirmEmail(eq(Confirmation(user, email, token))))
        .thenReturn(Future.successful(true))

      Post(s"/$userKey/confirmation/confirm?email=$email&token=$token") ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "confirm subscription" in { env =>
      import env.{eq => _, _}
      when(userService.confirmSubscription(eq(Confirmation(user, subscriptionId, token))))
        .thenReturn(Future.successful(true))

      Post(s"/$userKey/confirmation/confirm?id=$subscriptionId&token=$token") ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }

}
