package ru.yandex.vos2.model.user

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vos2.model.{UserRef, UserRefPhone}

@RunWith(classOf[JUnitRunner])
class UserRefSpec extends FlatSpec with Matchers {

  behavior of classOf[UserRef].getName + ".get()"

  it should "accept phone (hashed) id" in {
    UserRef.get("phone_somehash") shouldBe Some(UserRefPhone("somehash"))
  }

}
