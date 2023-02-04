package ru.yandex.vertis.subscriptions.storage

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.subscriptions.Model.{AutoruUser, BillingClient, DomainYandexUser, User}
import ru.yandex.vertis.subscriptions.model.{DomainYandexUserKey, UserKey}

/**
  * Specs on [[UserKey]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class UserKeySpec extends Matchers with WordSpecLike {

  "YandexUserKey" should {
    val authenticated = User.newBuilder().setUid("foo").build()
    val nonAuthenticated = User.newBuilder().setYandexuid("bar").build()
    "round trip authenticated user" in {
      rt(authenticated)
    }
    "round trip non-authenticated user" in {
      rt(nonAuthenticated)
    }
  }

  "BillingUser" should {
    val directClient = BillingClient.newBuilder().setDomain("realty").setClientId(42).build()
    val agency = BillingClient.newBuilder().setDomain("auto").setAgencyId(42).build()
    val agencyClient = BillingClient.newBuilder().setDomain("auto").setAgencyId(42).setClientId(51).build()

    "round trip direct client" in {
      rt(directClient)
    }

    "round trip agency" in {
      rt(agency)
    }
    "round trip agency client" in {
      rt(agencyClient)
    }
  }

  "AutoruUser" should {
    "round trip authenticated user" in {
      rt(AutoruUser.newBuilder().setPassportId("foo").build())
    }
    "round trip non-authenticated user" in {
      rt(AutoruUser.newBuilder().setSessionId("bar").build())
    }
    "round trip non-authenticated user with nonalphanumeric id" in {
      rt(AutoruUser.newBuilder().setSessionId("foo.bar_bee..baz").build())
    }
  }

  "DomainYandexUser" should {
    "round trip authenticated user" in {
      rt(DomainYandexUser.newBuilder().setDomain("realty").setUid("foo").build())
    }
    "round trip non-authenticated user" in {
      rt(DomainYandexUser.newBuilder().setDomain("realty-commercial").setYandexuid("foo").build())
    }
    "not serialized in key" in {
      intercept[IllegalArgumentException] {
        DomainYandexUserKey(DomainYandexUser.newBuilder().setUid("bar").build())
      }
      intercept[IllegalArgumentException] {
        DomainYandexUserKey(DomainYandexUser.newBuilder().setDomain("realty").build())
      }
      intercept[IllegalArgumentException] {
        DomainYandexUserKey(DomainYandexUser.newBuilder().setDomain("").build())
      }
    }
  }

  private def rt(initial: AutoruUser): Unit =
    rt(User.newBuilder().setAutoruUser(initial).build())

  private def rt(initial: DomainYandexUser): Unit =
    rt(User.newBuilder().setYandexUser(initial).build())

  private def rt(initial: BillingClient): Unit =
    rt(User.newBuilder().setBillingUser(initial).build())

  private def rt(initial: User): Unit = UserKey(initial) match {
    case UserKey(extracted) => extracted should be(initial)
    case other => fail(s"Unable to extract user from $other")
  }
}
