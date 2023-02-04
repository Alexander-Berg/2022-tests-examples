package ru.yandex.vertis.passport.loc

import org.scalatest.{Matchers, WordSpec}

/**
  *
  * @author zvez
  */
class AutoruStringResourcesSpec extends WordSpec with Matchers {

  "AutoruStringResources" should {

    "resolve ban reason" in {
      AutoruStringResources.userBanReason("WRONG_GENERATION") shouldBe defined
    }

    "not resolve non-existent reasons" in {
      AutoruStringResources.userBanReason("something_wrong") shouldBe empty
    }
  }

}
