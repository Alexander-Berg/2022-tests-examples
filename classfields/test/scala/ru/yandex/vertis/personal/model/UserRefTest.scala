package ru.yandex.vertis.personal.model

import org.scalatest.WordSpec
import org.scalatest.Matchers._

/**
  * author: nstaroverova
  */
class UserRefTest extends WordSpec {

  "UserRef" should {

    "User id with '-' should be correct " in {
      UserRef("123-user") shouldBe SimpleUserRef("123-user")
    }

    "User id with '-' and kind should be correct " in {
      UserRef("uid:123-user") shouldBe CompositeUserRef("uid", "123-user")
    }

    "User id with unknown symbol for [A-Za-z0-9_\\-\\.]+ should be incorrect " in {
      intercept[IllegalArgumentException] {
        UserRef("123-user%")
      }
    }

    "User id with unknown symbol for [A-Za-z0-9_\\-\\.]+ and kind should be incorrect " in {
      intercept[IllegalArgumentException] {
        UserRef("uid:123-user%")
      }
    }
  }

}
