package ru.yandex.vertis.subscriptions.backend.confirmation

import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.subscriptions.Model.{BillingClient, User, UserProperty}
import scala.util.Success

/**
  * Specs on [[ru.yandex.vertis.subscriptions.backend.confirmation.UserPropertiesService]]
  */
trait UserPropertiesServiceSpec extends Matchers with WordSpecLike {

  protected val service: UserPropertiesService

  "UserPropertiesService" should {
    "create property once and request confirmation" in {
      val user = uid("1")
      val property = "foo"

      val confirmation = service.requestConfirmation(user, property).get
      confirmation.user should be(user)
      confirmation.id should be(property)
      assert(confirmation.token.nonEmpty, "Non empty hash")

      val retrieved = service.getProperty(user, property).get
      retrieved should be(defined)
      retrieved.get.getUser should be(user)
      retrieved.get.getProperty should be(property)
      retrieved.get.getState should be(UserProperty.State.AWAIT_CONFIRMATION)
      retrieved.get.getConfirmationHash should be(confirmation.token)

      service.requestConfirmation(user, property).get
      val retrieved2 = service.getProperty(user, property).get
      retrieved2 should be(defined)
      retrieved2.get.getUser should be(user)
      retrieved2.get.getProperty should be(property)
      retrieved2.get.getState should be(UserProperty.State.AWAIT_CONFIRMATION)
    }

    import ru.yandex.vertis.subscriptions.DSL

    "confirm property for BillingClient with pair key" in {
      val user = DSL.billingClient(4L, 3L, "auto")
      val property = "foo"

      val confirmation = service.requestConfirmation(user, property).get
      service.confirm(confirmation) should be(Success(true))

      service.getProperty(user, property).get.get.getState should be(UserProperty.State.CONFIRMED)
    }

    "confirm property for BillingClient with single key" in {
      val user = DSL.agencyBillingClient(4L, "tours")
      val property = "foo"

      val confirmation = service.requestConfirmation(user, property).get
      service.confirm(confirmation) should be(Success(true))

      service.getProperty(user, property).get.get.getState should be(UserProperty.State.CONFIRMED)
    }

    "confirm property for BillingClient with single client key" in {
      val user = DSL.billingClientFromClient(3L, "auto")
      val property = "foo"

      val confirmation = service.requestConfirmation(user, property).get
      service.confirm(confirmation) should be(Success(true))

      service.getProperty(user, property).get.get.getState should be(UserProperty.State.CONFIRMED)
    }

    "confirm property" in {
      val user = uid("2")
      val property = "foo"

      val confirmation = service.requestConfirmation(user, property).get
      service.confirm(confirmation) should be(Success(true))

      service.getProperty(user, property).get.get.getState should be(UserProperty.State.CONFIRMED)
    }

    "not confirm property" in {
      val user = yandexuid("1")
      val property = "foo"

      val confirmation = service.requestConfirmation(user, property).get
      val erroneousConfirmation = confirmation.copy(token = "abcdef")
      service.confirm(erroneousConfirmation) should be(Success(false))

      service.getProperty(user, property).get.get.getState should be(UserProperty.State.AWAIT_CONFIRMATION)
    }

    "not confirm property twice" in {
      val user = yandexuid("1")
      val property = "foo"

      val confirmation = service.requestConfirmation(user, property).get
      service.confirm(confirmation) should be(Success(true))
      service.confirm(confirmation) should be(Success(false))
      service.getProperty(user, property).get.get.getState should be(UserProperty.State.CONFIRMED)
    }
  }

  private def uid(uid: String) = User.newBuilder().setUid(uid).build()

  private def yandexuid(cuid: String) = User.newBuilder().setYandexuid(cuid).build()

}
