package ru.yandex.vertis.subscriptions.api.v3.service.user

import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.scalatest.junit.JUnitRunner
import org.scalatest.{fixture, Matchers}
import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.subscriptions.api.RouteTestWithConfig
import ru.yandex.vertis.subscriptions.api.v3.service.user.confirmation.ConfirmationHandler
import ru.yandex.vertis.subscriptions.backend.confirmation.Confirmation
import ru.yandex.vertis.subscriptions.backend.user.UserService
import ru.yandex.vertis.subscriptions.model.UserKey
import ru.yandex.vertis.subscriptions.model.owner.Owner
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators.Users
import spray.http.StatusCodes

import scala.concurrent.Future

/**
  *
  * @author zvez
  */
@RunWith(classOf[JUnitRunner])
class ConfirmationHandlerSpec extends Matchers with fixture.WordSpecLike with RouteTestWithConfig with MockitoSupport {

  import MockitoSupport.{eq => eqq}

  class FixtureParam {
    val userService = mock[UserService]
    val user = Users.next
    val userKey = UserKey(user)

    val route = seal(new ConfirmationHandler(userService).route(Owner.fromLegacyUser(user)))
    val email = CoreGenerators.emailAddressGen.next
    val subscriptionId = CoreGenerators.idGen.next
    val token = CoreGenerators.idGen.next
  }

  override def withFixture(test: OneArgTest) = {
    withFixture(test.toNoArgTest(new FixtureParam))
  }

  "ConfirmationHandler" should {
    pending
    "request email confirmation" in { env =>
      import env._
      when(userService.requestEmailConfirmation(?, ?, ?))
        .thenReturn(Future.successful(()))

      Post(s"/request-for-email?email=$email") ~> route ~> check {
        status shouldBe StatusCodes.OK
        verify(userService).requestEmailConfirmation(eqq(user), eqq(email), eqq(false))
      }
    }

    "request subscription confirmation" in { env =>
      import env._
      when(userService.requestSubscriptionConfirmation(?, ?))
        .thenReturn(Future.successful(()))

      Post(s"/request-for-subscription?id=$subscriptionId") ~> route ~> check {
        status shouldBe StatusCodes.OK
        verify(userService).requestSubscriptionConfirmation(eqq(user), eqq(subscriptionId))
      }
    }

    "confirm email" in { env =>
      import env._
      when(userService.confirmEmail(?)).thenReturn(Future.successful(true))

      Post(s"/confirm-email?email=$email&token=$token") ~> route ~> check {
        status shouldBe StatusCodes.OK

        verify(userService).confirmEmail(eqq(Confirmation(user, email, token)))
      }
    }

    "confirm subscription" in { env =>
      import env._
      when(userService.confirmSubscription(?))
        .thenReturn(Future.successful(true))

      Post(s"/confirm-subscription?id=$subscriptionId&token=$token") ~> route ~> check {
        status shouldBe StatusCodes.OK

        verify(userService).confirmSubscription(eqq(Confirmation(user, subscriptionId, token)))
      }
    }
  }

}
